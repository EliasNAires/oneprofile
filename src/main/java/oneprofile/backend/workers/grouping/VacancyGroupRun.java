package oneprofile.backend.workers.grouping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.GroupMark;
import oneprofile.backend.storage.normalizedvacancy.GroupVacancy;
import oneprofile.backend.storage.normalizedvacancy.ManagementLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.workers.grouping.VacancyGroupRule.ComparedVacancy;
import oneprofile.backend.workers.grouping.VacancyGroupRule.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Groups the engineering subset's vacancies by company, title evidence and description (#27).
 * Carries stored group ids forward where possible and records duplicates in place of group membership.
 * Runs after cleaning, classification and normalization; vacancies outside the subset lose their marks.
 */
@Component
public class VacancyGroupRun {

	/** Descriptions are read by whole buckets around this size, and marks written in batches of this size. */
	private static final int CHUNK = 1000;

	/** Placeholder until the labelled sample of #27 sets the similarity threshold. */
	private static final double THRESHOLD = 0.8;

	private final DescriptionSimilarityRule similarity;

	private final VacancyGroupRule rule;

	private final NormalizedVacancyStore normalized;

	private final double threshold;

	private final int chunk;

	/**
	 * Groups vacancies at the placeholder threshold with a chunk of a thousand vacancies.
	 * @param similarity the rule that shingles descriptions
	 * @param rule the rule that groups compared vacancies and chooses duplicates
	 * @param normalized where the subset is read and the marks recorded
	 */
	@Autowired
	public VacancyGroupRun(DescriptionSimilarityRule similarity, VacancyGroupRule rule, NormalizedVacancyStore normalized) {
		this(similarity, rule, normalized, THRESHOLD, CHUNK);
	}

	/**
	 * @param similarity the rule that shingles descriptions
	 * @param rule the rule that groups compared vacancies and chooses duplicates
	 * @param normalized where the subset is read and the marks recorded
	 * @param threshold the minimum description overlap that joins compared vacancies
	 * @param chunk how many vacancies a read aims for and a write holds at most
	 */
	VacancyGroupRun(DescriptionSimilarityRule similarity, VacancyGroupRule rule, NormalizedVacancyStore normalized,
			double threshold, int chunk) {
		if (chunk < 1) {
			throw new IllegalArgumentException("A chunk has to hold at least one vacancy");
		}
		this.similarity = similarity;
		this.rule = rule;
		this.normalized = normalized;
		this.threshold = threshold;
		this.chunk = chunk;
	}

	/**
	 * Groups the subset, reuses stored ids, records every vacancy's mark, then clears marks outside it.
	 * @return how many vacancies, groups, duplicates and fresh groups the run found
	 */
	public Report groupVacancies() {
		List<GroupVacancy> scope = this.normalized.vacancyGroupScope();
		Map<Long, Long> oldIds = new HashMap<>();
		Map<Bucket, List<GroupVacancy>> buckets = new LinkedHashMap<>();
		for (GroupVacancy vacancy : scope) {
			oldIds.put(vacancy.vacancyId(), vacancy.groupId());
			buckets.computeIfAbsent(bucket(vacancy), (key) -> new ArrayList<>())
				.add(vacancy);
		}
		Map<Long, Integer> open = this.normalized.openVacanciesByCompany();
		List<Group> groups = new ArrayList<>();
		List<List<GroupVacancy>> pending = new ArrayList<>();
		int size = 0;
		for (List<GroupVacancy> bucket : buckets.values()) {
			if (bucket.size() == 1) {
				groups.addAll(this.rule.group(List.of(compared(bucket.getFirst(), new long[0])), open, this.threshold));
				continue;
			}
			if (size > 0 && size + bucket.size() > this.chunk) {
				groups.addAll(readGroups(pending, open));
				pending.clear();
				size = 0;
			}
			pending.add(bucket);
			size += bucket.size();
		}
		if (!pending.isEmpty()) {
			groups.addAll(readGroups(pending, open));
		}
		groups.sort(Comparator.comparingLong((group) -> vacancyIds(group).mapToLong(Long::longValue).min().orElseThrow()));

		Set<Long> taken = new HashSet<>();
		List<Long> assigned = new ArrayList<>();
		int fresh = 0;
		for (Group group : groups) {
			Map<Long, Integer> votes = new HashMap<>();
			vacancyIds(group).map(oldIds::get).filter((id) -> id != null && !taken.contains(id))
				.forEach((id) -> votes.merge(id, 1, Integer::sum));
			Long id = votes.entrySet().stream()
				.sorted(Map.Entry.<Long, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.map(Map.Entry::getKey).findFirst().orElse(null);
			assigned.add(id);
			if (id == null) {
				fresh++;
			}
			else {
				taken.add(id);
			}
		}
		Iterator<Long> freshIds = (fresh == 0) ? List.<Long>of().iterator()
				: this.normalized.nextGroupIds(fresh).iterator();
		Map<Long, GroupMark> marks = new LinkedHashMap<>();
		int duplicates = 0;
		for (int i = 0; i < groups.size(); i++) {
			Group group = groups.get(i);
			long id = (assigned.get(i) != null) ? assigned.get(i) : freshIds.next();
			for (long member : group.members()) {
				record(marks, member, GroupMark.inGroup(id));
			}
			for (Map.Entry<Long, Long> duplicate : group.duplicates().entrySet()) {
				record(marks, duplicate.getKey(), GroupMark.duplicateOf(duplicate.getValue()));
			}
			duplicates += group.duplicates().size();
		}
		if (!marks.isEmpty()) {
			this.normalized.recordVacancyGroups(Map.copyOf(marks));
		}
		this.normalized.clearVacancyGroupsOutsideSubset();
		return new Report(scope.size(), groups.size(), duplicates, fresh);
	}

	/** Reads descriptions for whole buckets together, then compares only within each bucket. */
	private List<Group> readGroups(List<List<GroupVacancy>> buckets, Map<Long, Integer> open) {
		List<Long> ids = buckets.stream().flatMap(List::stream).map(GroupVacancy::vacancyId).toList();
		Map<Long, List<Segment>> segments = this.normalized.segmentsOf(ids);
		List<Group> groups = new ArrayList<>();
		for (List<GroupVacancy> bucket : buckets) {
			List<String> locations = bucket.stream().flatMap((vacancy) -> vacancy.locations().stream())
				.distinct().toList();
			List<ComparedVacancy> compared = bucket.stream()
				.map((vacancy) -> compared(vacancy, this.similarity
					.shingles(segments.getOrDefault(vacancy.vacancyId(), List.of()), locations)))
				.toList();
			groups.addAll(this.rule.group(compared, open, this.threshold));
		}
		return groups;
	}

	/** Adds one mark and writes a full batch without retaining its mutable map in the store. */
	private void record(Map<Long, GroupMark> marks, long vacancyId, GroupMark mark) {
		marks.put(vacancyId, mark);
		if (marks.size() == this.chunk) {
			this.normalized.recordVacancyGroups(Map.copyOf(marks));
			marks.clear();
		}
	}

	/** Title evidence that must agree before descriptions can be compared. */
	private Bucket bucket(GroupVacancy vacancy) {
		List<String> titles = (vacancy.normalizedTitles() == null || vacancy.normalizedTitles().isEmpty())
				? List.of(vacancy.cleanedTitle().toLowerCase(Locale.ROOT)) : vacancy.normalizedTitles();
		return new Bucket(vacancy.slug(), titles, vacancy.managementLevel(), vacancy.titleSeniorities());
	}

	/** Carries the evidence the pure grouping rule needs. */
	private ComparedVacancy compared(GroupVacancy vacancy, long[] shingles) {
		return new ComparedVacancy(vacancy.vacancyId(), vacancy.companyId(), vacancy.firstPublishedAt(), vacancy.locations(),
				vacancy.workModes(), shingles);
	}

	/** All members vote for stored ids, including those now marked as duplicates. */
	private Stream<Long> vacancyIds(Group group) {
		return Stream.concat(group.members().stream(), group.duplicates().keySet().stream());
	}

	/**
	 * Boards of the same slug on any ATS share one company's title bucket.
	 * @param slug the company's slug across ATSs
	 * @param titles its normalized titles, or its lowercased cleaned title when absent
	 * @param managementLevel its title's management level, null if none
	 * @param seniorities the seniority levels its title names
	 */
	private record Bucket(String slug, List<String> titles, ManagementLevelEnum managementLevel,
			Set<SeniorityLevelEnum> seniorities) {
	}

	/**
	 * What one grouping run found.
	 * @param vacancies how many vacancies of the subset it read
	 * @param groups how many vacancy groups it formed
	 * @param duplicates how many vacancies repeat a kept member
	 * @param newGroups how many groups required fresh ids
	 */
	public record Report(int vacancies, int groups, int duplicates, int newGroups) {
	}

}
