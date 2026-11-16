package oneprofile.backend.workers.grouping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.springframework.stereotype.Component;

/** Joins similar descriptions within one company's title bucket, then identifies its duplicates. */
@Component
public class VacancyGroupRule {

	private final DescriptionSimilarityRule similarity;

	/**
	 * Uses the description's shingle overlap to join compared vacancies.
	 * @param similarity the rule that compares description shingles
	 */
	public VacancyGroupRule(DescriptionSimilarityRule similarity) {
		this.similarity = similarity;
	}

	/**
	 * Forms single-linkage groups and chooses duplicates within each location and work-mode set.
	 * A single-linkage chain may join a group, but hiding a vacancy needs its own description to
	 * repeat the one shown.
	 * @param compared the vacancies in one company's title bucket
	 * @param openVacanciesByCompany each board's open vacancy count for choosing the source
	 * @param threshold the minimum description overlap for grouping and identifying duplicates
	 * @return the groups with their kept members and duplicates
	 */
	public List<Group> group(List<ComparedVacancy> compared, Map<Long, Integer> openVacanciesByCompany, double threshold) {
		int[] parents = new int[compared.size()];
		for (int i = 0; i < parents.length; i++) {
			parents[i] = i;
		}
		for (int i = 0; i < parents.length; i++) {
			for (int j = i + 1; j < parents.length; j++) {
				long[] a = compared.get(i).shingles();
				long[] b = compared.get(j).shingles();
				if (this.similarity.comparable(a) && this.similarity.comparable(b)
						&& this.similarity.overlap(a, b) >= threshold) {
					parents[root(parents, j)] = root(parents, i);
				}
			}
		}
		Map<Integer, List<ComparedVacancy>> joined = new LinkedHashMap<>();
		for (int i = 0; i < parents.length; i++) {
			joined.computeIfAbsent(root(parents, i), (key) -> new ArrayList<>()).add(compared.get(i));
		}
		List<Group> groups = new ArrayList<>();
		for (List<ComparedVacancy> members : joined.values()) {
			Map<LocationModes, List<ComparedVacancy>> repeated = new LinkedHashMap<>();
			for (ComparedVacancy member : members) {
				Set<String> locations = new TreeSet<>();
				member.locations().forEach((location) -> locations
					.add(location.toLowerCase(Locale.ROOT).replaceAll("(?U)\\s+", " ").trim()));
				repeated.computeIfAbsent(new LocationModes(locations, member.workModes()), (key) -> new ArrayList<>())
					.add(member);
			}
			List<Long> kept = new ArrayList<>();
			Map<Long, Long> duplicates = new HashMap<>();
			for (List<ComparedVacancy> same : repeated.values()) {
				ComparedVacancy keep = kept(same, openVacanciesByCompany);
				kept.add(keep.vacancyId());
				for (ComparedVacancy vacancy : same) {
					if (vacancy.vacancyId() == keep.vacancyId()) {
						continue;
					}
					if (this.similarity.overlap(vacancy.shingles(), keep.shingles()) >= threshold) {
						duplicates.put(vacancy.vacancyId(), keep.vacancyId());
					}
					else {
						kept.add(vacancy.vacancyId());
					}
				}
			}
			groups.add(new Group(kept.stream().sorted().toList(), Map.copyOf(duplicates)));
		}
		return groups;
	}

	/** Finds a component's root, shortening the path for later comparisons. */
	private int root(int[] parents, int index) {
		while (parents[index] != index) {
			parents[index] = parents[parents[index]];
			index = parents[index];
		}
		return index;
	}

	/**
	 * Across ATSs the oldest board is the source over the copy. On one board a repost is the company
	 * refreshing it, so the newest carries the current date and apply link.
	 */
	private ComparedVacancy kept(List<ComparedVacancy> same, Map<Long, Integer> openVacanciesByCompany) {
		Map<Long, List<ComparedVacancy>> boards = new HashMap<>();
		same.forEach((compared) -> boards.computeIfAbsent(compared.companyId(), (key) -> new ArrayList<>())
			.add(compared));
		Comparator<Long> boardOrder = Comparator
			.comparing((Long companyId) -> boards.get(companyId).stream().map(ComparedVacancy::firstPublishedAt)
				.filter((date) -> date != null).min(Comparator.naturalOrder()).orElse(null),
					Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
			.thenComparing(Comparator.comparingInt((Long companyId) -> openVacanciesByCompany.getOrDefault(companyId, 0))
				.reversed())
			.thenComparingLong(Long::longValue);
		long board = boards.keySet().stream().min(boardOrder).orElseThrow();
		return boards.get(board).stream()
			.max(Comparator.comparing(ComparedVacancy::firstPublishedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
				.thenComparingLong(ComparedVacancy::vacancyId))
			.orElseThrow();
	}

	/**
	 * A vacancy's evidence within a company's title bucket.
	 * @param vacancyId the vacancy being compared
	 * @param companyId its board's company id
	 * @param firstPublishedAt its publication date, null where unknown
	 * @param locations its declared locations
	 * @param workModes the modes it offers
	 * @param shingles its sorted description hashes, empty for a bucket of one
	 */
	public record ComparedVacancy(long vacancyId, long companyId, Instant firstPublishedAt, List<String> locations,
			Set<WorkModeEnum> workModes, long[] shingles) {
	}

	/**
	 * One position and the vacancies it repeats.
	 * @param members sorted ids of vacancies that keep the group id
	 * @param duplicates duplicate vacancy ids mapped to the vacancy kept
	 */
	public record Group(List<Long> members, Map<Long, Long> duplicates) {
	}

	/**
	 * Equal declared locations and modes allow checking for duplicates within a group.
	 * @param locations the normalized declared locations
	 * @param modes the work modes offered
	 */
	private record LocationModes(Set<String> locations, Set<WorkModeEnum> modes) {
	}

}
