package oneprofile.backend.workers.eligibility;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.CountrySetVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.PlaceKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reads the country set of every vacancy of the engineering subset (#13, PRD §5.4), from its work mode, its title,
 * the places of its declared location and its description. It reads the work mode and the places, so it runs
 * after the work-mode run and the place run. It clears what the last run recorded first, so a vacancy that has
 * left the subset keeps no country set.
 * <p>
 * The eligibility state is not stored: it is derived at query time for whichever country and city are asked.
 * The report derives it for Argentina and the United States with the person's city unknown, so every vacancy is
 * judged by country.
 */
@Component
public class CountrySetRun {

	/** A vacancy's segments weigh as much as the seniority pass reads, so a batch is as large as its. */
	private static final int BATCH = 1000;

	/** The countries the report derives the states for: the profile's, and the largest market's. */
	private static final List<String> COUNTRIES = List.of("AR", "US");

	/** What the report calls the work mode of a vacancy that declares none. */
	private static final String UNDECLARED = "UNDECLARED";

	/** What the report calls the source of a state no stated set decided. */
	private static final String NONE = "NONE";

	private final CountrySetRule rule;

	private final EligibilityRule eligibility;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Reads country sets in batches of a thousand vacancies.
	 * @param rule the rule that reads a vacancy's country set
	 * @param eligibility the rule that derives the state a country set gives a country
	 * @param normalized where the subset is read and the country sets recorded
	 */
	@Autowired
	public CountrySetRun(CountrySetRule rule, EligibilityRule eligibility, NormalizedVacancyStore normalized) {
		this(rule, eligibility, normalized, BATCH);
	}

	/**
	 * @param rule the rule that reads a vacancy's country set
	 * @param eligibility the rule that derives the state a country set gives a country
	 * @param normalized where the subset is read and the country sets recorded
	 * @param batch how many vacancies are read and recorded at a time
	 */
	CountrySetRun(CountrySetRule rule, EligibilityRule eligibility, NormalizedVacancyStore normalized, int batch) {
		if (batch < 1) {
			throw new IllegalArgumentException("A batch has to hold at least one vacancy");
		}
		this.rule = rule;
		this.eligibility = eligibility;
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Clears what the last run recorded, then reads the country set of every vacancy of the subset.
	 * @return what the run read, and the states it gives Argentina and the United States
	 */
	public Report readCountrySets() {
		this.normalized.clearCountrySets();
		int vacancies = 0;
		Map<String, Tally> countries = new LinkedHashMap<>();
		COUNTRIES.forEach((country) -> countries.put(country, new Tally()));
		int onsite = 0;
		Map<Precision, Integer> precisions = new EnumMap<>(Precision.class);
		long after = 0;
		while (true) {
			List<CountrySetVacancy> read = this.normalized.countrySetScopeAfter(after, this.batch);
			if (read.isEmpty()) {
				break;
			}
			Map<Long, CountrySet> stated = new HashMap<>();
			for (CountrySetVacancy vacancy : read) {
				CountrySet countrySet = this.rule.read(vacancy);
				stated.put(vacancy.vacancyId(), countrySet);
				String workMode = (vacancy.workMode() != null) ? vacancy.workMode().name() : UNDECLARED;
				countries.forEach((country, tally) -> tally.count(workMode,
						this.eligibility.eligibility(countrySet, country, null)));
				if (vacancy.workMode() == WorkModeEnum.ONSITE || vacancy.workMode() == WorkModeEnum.HYBRID) {
					onsite++;
					precisions.merge(precisionOf(countrySet), 1, Integer::sum);
				}
			}
			this.normalized.recordCountrySets(stated);
			vacancies += read.size();
			after = read.getLast().vacancyId();
		}
		Map<String, Country> byCountry = new LinkedHashMap<>();
		countries.forEach((country, tally) -> byCountry.put(country, tally.country()));
		return new Report(vacancies, byCountry, new OnsitePlaces(onsite, precisions.getOrDefault(Precision.CITY, 0),
				precisions.getOrDefault(Precision.COUNTRY, 0), precisions.getOrDefault(Precision.NEITHER, 0)));
	}

	/** How precisely an onsite or hybrid vacancy's onsite and hybrid options name where it is. */
	private static Precision precisionOf(CountrySet countrySet) {
		boolean country = false;
		for (StatedSet set : countrySet.statedSets()) {
			if (set.source() != StatedSetSourceEnum.LOCATION
					|| (set.workMode() != WorkModeEnum.ONSITE && set.workMode() != WorkModeEnum.HYBRID)) {
				continue;
			}
			for (Place place : set.places()) {
				if (place.kind() == PlaceKindEnum.CITY) {
					return Precision.CITY;
				}
				country = true;
			}
		}
		return country ? Precision.COUNTRY : Precision.NEITHER;
	}

	/** How precisely an onsite or hybrid vacancy names where it is, as the report counts it. */
	private enum Precision {

		/** One of its options names a city. */
		CITY,

		/** They name a country, a subdivision or a region, and no city. */
		COUNTRY,

		/** They name no place. */
		NEITHER

	}

	/** The states a run has derived for one country so far. */
	private static final class Tally {

		private final int[] states = new int[3];

		private final Map<String, int[]> byWorkMode = new HashMap<>();

		private final Map<String, int[]> bySource = new HashMap<>();

		void count(String workMode, Eligibility eligibility) {
			String source = (eligibility.source() != null) ? eligibility.source().name() : NONE;
			int state = eligibility.state().ordinal();
			this.states[state]++;
			this.byWorkMode.computeIfAbsent(workMode, (mode) -> new int[3])[state]++;
			this.bySource.computeIfAbsent(source, (named) -> new int[3])[state]++;
		}

		Country country() {
			Map<String, States> byWorkMode = new HashMap<>();
			this.byWorkMode.forEach((mode, counts) -> byWorkMode.put(mode, states(counts)));
			Map<String, States> bySource = new HashMap<>();
			this.bySource.forEach((source, counts) -> bySource.put(source, states(counts)));
			return new Country(states(this.states), byWorkMode, bySource);
		}

		private static States states(int[] counts) {
			return new States(counts[EligibilityStateEnum.ELIGIBLE.ordinal()],
					counts[EligibilityStateEnum.EXCLUDED.ordinal()], counts[EligibilityStateEnum.UNKNOWN.ordinal()]);
		}

	}

	/**
	 * What one run read.
	 *
	 * @param vacancies how many vacancies of the subset it read
	 * @param countries the states it gives each country it was asked for, by its two-letter code, with the person's
	 * city unknown
	 * @param onsitePlaces how precisely the onsite and hybrid vacancies name where they are
	 */
	public record Report(int vacancies, Map<String, Country> countries, OnsitePlaces onsitePlaces) {

		public Report {
			countries = Map.copyOf(countries);
		}

	}

	/**
	 * The states the subset's vacancies take for one country.
	 *
	 * @param states how many take each state
	 * @param byWorkMode the same, by the vacancy's work mode, {@code UNDECLARED} where it declares none
	 * @param bySource the same, by where the stated set that decided the state was read, {@code NONE} where none
	 * decided it
	 */
	public record Country(States states, Map<String, States> byWorkMode, Map<String, States> bySource) {

		public Country {
			byWorkMode = Map.copyOf(byWorkMode);
			bySource = Map.copyOf(bySource);
		}

	}

	/**
	 * How many vacancies take each eligibility state.
	 *
	 * @param eligible how many are eligible
	 * @param excluded how many are excluded
	 * @param unknown how many are unknown
	 */
	public record States(int eligible, int excluded, int unknown) {
	}

	/**
	 * How precisely the onsite and hybrid vacancies' onsite and hybrid options name where they are: those with a
	 * city are judged by distance once the person's city is known (ADR-0028), the others by country.
	 *
	 * @param vacancies how many vacancies of the subset are onsite or hybrid
	 * @param withCity how many of them name a city
	 * @param countryOnly how many name a country, a subdivision or a region, and no city
	 * @param neither how many name no place
	 */
	public record OnsitePlaces(int vacancies, int withCity, int countryOnly, int neither) {
	}

}
