package oneprofile.backend.workers.deduplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.DuplicateVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.workers.deduplication.DuplicateRule.Candidate;
import oneprofile.backend.workers.deduplication.DuplicateRule.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Marks exact duplicates across the engineering subset, replacing the previous run's marks.
 * Runs after cleaning, classification, normalization and work mode.
 */
@Component
public class DuplicateRun {

	/** How many vacancies' descriptions to read together, keeping whole keys in one chunk. */
	private static final int CHUNK = 1000;

	private final DuplicateRule rule;

	private final NormalizedVacancyStore normalized;

	private final int chunk;

	/**
	 * Wires the pass with the usual description read chunk.
	 * @param rule the rule that compares raw evidence and chooses the kept vacancy
	 * @param normalized the derived facts held for the corpus
	 */
	@Autowired
	public DuplicateRun(DuplicateRule rule, NormalizedVacancyStore normalized) {
		this(rule, normalized, CHUNK);
	}

	/**
	 * Wires the pass with a controllable description read chunk.
	 * @param rule the rule that compares raw evidence and chooses the kept vacancy
	 * @param normalized the derived facts held for the corpus
	 * @param chunk how many vacancies to read together, allowing a larger whole key
	 */
	DuplicateRun(DuplicateRule rule, NormalizedVacancyStore normalized, int chunk) {
		if (chunk < 1) {
			throw new IllegalArgumentException("The chunk must hold at least one vacancy");
		}
		this.rule = rule;
		this.normalized = normalized;
		this.chunk = chunk;
	}

	/**
	 * Reads descriptions only for repeated keys and records all duplicate marks in one call.
	 * @return how many vacancies the subset holds and how many repeat a kept vacancy
	 */
	public Report markDuplicates() {
		List<DuplicateVacancy> scope = this.normalized.duplicateScope();
		Map<Key, List<DuplicateVacancy>> candidatesByKey = new LinkedHashMap<>();
		for (DuplicateVacancy vacancy : scope) {
			Key key = this.rule.key(vacancy.slug(), vacancy.title(), vacancy.locations(), vacancy.workModes());
			candidatesByKey.computeIfAbsent(key, (ignored) -> new ArrayList<>()).add(vacancy);
		}
		Map<Long, Long> duplicates = new HashMap<>();
		List<List<DuplicateVacancy>> candidateSets = new ArrayList<>();
		int size = 0;
		for (List<DuplicateVacancy> candidates : candidatesByKey.values()) {
			if (candidates.size() < 2) {
				continue;
			}
			if (size > 0 && size + candidates.size() > this.chunk) {
				duplicates.putAll(readDuplicates(candidateSets));
				candidateSets.clear();
				size = 0;
			}
			candidateSets.add(candidates);
			size += candidates.size();
		}
		if (!candidateSets.isEmpty()) {
			duplicates.putAll(readDuplicates(candidateSets));
		}
		this.normalized.recordDuplicates(Map.copyOf(duplicates));
		return new Report(scope.size(), duplicates.size());
	}

	/** Reads descriptions for whole keys together, then compares candidates only within each key. */
	private Map<Long, Long> readDuplicates(List<List<DuplicateVacancy>> candidateSets) {
		List<Long> ids = candidateSets.stream().flatMap(List::stream).map(DuplicateVacancy::vacancyId).toList();
		Map<Long, String> descriptions = this.normalized.descriptionsOf(ids);
		Map<Long, Long> duplicates = new HashMap<>();
		for (List<DuplicateVacancy> candidates : candidateSets) {
			List<Candidate> described = candidates.stream()
				.map((vacancy) -> new Candidate(vacancy.vacancyId(), vacancy.companyId(), vacancy.firstPublishedAt(),
						descriptions.get(vacancy.vacancyId())))
				.toList();
			duplicates.putAll(this.rule.duplicates(described));
		}
		return duplicates;
	}

	/**
	 * What one duplicate run found.
	 * @param vacancies how many vacancies of the subset it read
	 * @param duplicates how many vacancies repeat a kept vacancy
	 */
	public record Report(int vacancies, int duplicates) {
	}

}
