package oneprofile.backend.workers.deduplication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import oneprofile.backend.storage.normalizedvacancy.DuplicateVacancy.Location;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.springframework.stereotype.Component;

/** Identifies exact duplicates and chooses the kept vacancy from their publication dates. */
@Component
public class DuplicateRule {

	/**
	 * The evidence that must agree before descriptions are read.
	 * @param slug the company's slug shared across ATSs
	 * @param title the raw title
	 * @param locations the declared location strings and their countries
	 * @param workModes every mode the vacancy offers
	 * @return the key with folded title and location-country set
	 */
	public Key key(String slug, String title, List<Location> locations, Set<WorkModeEnum> workModes) {
		return new Key(slug, normalize(title), locations.stream()
				.map((location) -> new Location(normalize(location.location()),
						(location.country() != null) ? normalize(location.country()) : null))
				.collect(Collectors.toSet()),
				workModes);
	}

	/**
	 * Compares the raw descriptions of candidates sharing one key, ignoring descriptions with no text.
	 * @param candidates the vacancies whose keys agree
	 * @return each duplicate vacancy id mapped to the kept vacancy id
	 */
	public Map<Long, Long> duplicates(List<Candidate> candidates) {
		Map<String, List<Candidate>> descriptions = new HashMap<>();
		for (Candidate candidate : candidates) {
			String description = normalize(candidate.description());
			if (!description.isEmpty()) {
				descriptions.computeIfAbsent(description, (key) -> new ArrayList<>()).add(candidate);
			}
		}
		Map<Long, Long> duplicates = new HashMap<>();
		for (List<Candidate> same : descriptions.values()) {
			Candidate kept = kept(same);
			for (Candidate candidate : same) {
				if (candidate.vacancyId() != kept.vacancyId()) {
					duplicates.put(candidate.vacancyId(), kept.vacancyId());
				}
			}
		}
		return Map.copyOf(duplicates);
	}

	/** Collapses Unicode whitespace, trims it and folds case without removing any words. */
	private String normalize(String text) {
		return (text != null) ? text.replaceAll("(?U)\\s+", " ").trim().toLowerCase(Locale.ROOT) : "";
	}

	/** Chooses the oldest board, then its newest vacancy, breaking ties by their ids. */
	private Candidate kept(List<Candidate> same) {
		Map<Long, List<Candidate>> boards = new HashMap<>();
		same.forEach((candidate) -> boards.computeIfAbsent(candidate.companyId(), (key) -> new ArrayList<>())
			.add(candidate));
		Comparator<Long> boardOrder = Comparator
			.comparing((Long companyId) -> boards.get(companyId).stream().map(Candidate::firstPublishedAt)
				.filter((date) -> date != null).min(Comparator.naturalOrder()).orElse(null),
					Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
			.thenComparingLong(Long::longValue);
		long board = boards.keySet().stream().min(boardOrder).orElseThrow();
		return boards.get(board).stream()
			.max(Comparator.comparing(Candidate::firstPublishedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
				.thenComparingLong(Candidate::vacancyId))
			.orElseThrow();
	}

	/**
	 * Evidence shared before descriptions are compared.
	 * @param slug the company's slug
	 * @param title the folded raw title
	 * @param locations the folded location-country set, with null countries kept distinct
	 * @param workModes the offered work-mode set
	 */
	public record Key(String slug, String title, Set<Location> locations, Set<WorkModeEnum> workModes) {

		/** Keeps the sets independent of their source collections. */
		public Key {
			locations = Set.copyOf(locations);
			workModes = Set.copyOf(workModes);
		}

	}

	/**
	 * One vacancy whose key agrees with the other candidates.
	 * @param vacancyId the vacancy being compared
	 * @param companyId the company whose board holds it
	 * @param firstPublishedAt its first publication, null where unknown
	 * @param description its raw stored description, null where absent
	 */
	public record Candidate(long vacancyId, long companyId, Instant firstPublishedAt, String description) {
	}

}
