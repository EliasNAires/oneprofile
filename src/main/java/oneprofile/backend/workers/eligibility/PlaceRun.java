package oneprofile.backend.workers.eligibility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.normalizedvacancy.LocationPlaces;
import oneprofile.backend.storage.normalizedvacancy.LocationString;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.PlaceVacancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reads the places every string of each declared location of the engineering subset names, by the place rule
 * (#26). It reads what the ATS published and nothing the other passes derive, so it can run any time after
 * classification. It clears what the last run recorded first, so a vacancy that has left the subset keeps no
 * places.
 */
@Component
public class PlaceRun {

	/** A vacancy's location strings weigh little, so a batch is as large as the work mode's. */
	private static final int BATCH = 5000;

	/** How many of the residue's strings the report lists, the most frequent first. */
	private static final int RESIDUE = 200;

	private final PlaceRule rule;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Reads places in batches of five thousand vacancies.
	 * @param rule the rule that reads a location string's places
	 * @param normalized where the subset is read and the places recorded
	 */
	@Autowired
	public PlaceRun(PlaceRule rule, NormalizedVacancyStore normalized) {
		this(rule, normalized, BATCH);
	}

	/**
	 * @param rule the rule that reads a location string's places
	 * @param normalized where the subset is read and the places recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	PlaceRun(PlaceRule rule, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.rule = rule;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then reads the places of every location string of the subset.
	 * @return what the run read
	 */
	public Report readPlaces() {
		this.normalized.clearPlaces();
		int vacancies = 0;
		int withPlaces = 0;
		Map<AtsEnum, Tally> strings = new EnumMap<>(AtsEnum.class);
		Map<String, Integer> residue = new HashMap<>();
		long after = 0;
		while (true) {
			List<PlaceVacancy> read = this.normalized.placeScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, List<LocationPlaces>> located = new HashMap<>();
			for (PlaceVacancy vacancy : read) {
				List<LocationPlaces> locations = new ArrayList<>(vacancy.locations().size());
				Tally tally = strings.computeIfAbsent(vacancy.ats(), (ats) -> new Tally());
				for (LocationString location : vacancy.locations()) {
					PlaceReading reading = this.rule.read(location.location(), location.country());
					locations.add(new LocationPlaces(location.list(), location.position(), location.location(),
							reading.places()));
					tally.read++;
					if (!reading.places().isEmpty()) {
						tally.withPlaces++;
					}
					if (reading.residue()) {
						tally.residue++;
						residue.merge(location.location(), 1, Integer::sum);
					}
				}
				if (!locations.isEmpty()) {
					located.put(vacancy.vacancyId(), locations);
				}
				if (locations.stream().anyMatch((location) -> !location.places().isEmpty())) {
					withPlaces++;
				}
			}
			this.normalized.recordPlaces(located);
			vacancies += read.size();
			after = read.getLast().vacancyId();
		}
		Map<AtsEnum, Strings> byAts = new EnumMap<>(AtsEnum.class);
		strings.forEach((ats, tally) -> byAts.put(ats, new Strings(tally.read, tally.withPlaces, tally.residue)));
		return new Report(vacancies, withPlaces, byAts, residue.entrySet().stream()
			.map((entry) -> new Residue(entry.getKey(), entry.getValue()))
			.sorted(Comparator.comparingInt(Residue::strings).reversed().thenComparing(Residue::location))
			.limit(RESIDUE)
			.toList());
	}

	/** How many location strings of one ATS a run has read so far. */
	private static final class Tally {

		private int read;

		private int withPlaces;

		private int residue;

	}

	/**
	 * What one run read.
	 *
	 * @param vacancies how many vacancies of the subset it read
	 * @param withPlaces how many of them have a location string that names a place
	 * @param strings how many location strings it read, by ATS
	 * @param residue the most frequent strings that name no place it could read, each with how many strings of
	 * the subset hold it: the input for growing the hand table
	 */
	public record Report(int vacancies, int withPlaces, Map<AtsEnum, Strings> strings, List<Residue> residue) {

		public Report {
			strings = Map.copyOf(strings);
			residue = List.copyOf(residue);
		}

	}

	/**
	 * How many location strings of one ATS a run read.
	 *
	 * @param read how many it read
	 * @param withPlaces how many of them name a place
	 * @param residue how many of them are residue: they name no place, and hold words that were not read
	 */
	public record Strings(int read, int withPlaces, int residue) {
	}

	/**
	 * One string of the residue.
	 *
	 * @param location the string
	 * @param strings how many location strings of the subset are it
	 */
	public record Residue(String location, int strings) {
	}

}
