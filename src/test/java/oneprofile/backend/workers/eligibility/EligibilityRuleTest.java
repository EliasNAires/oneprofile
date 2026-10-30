package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.placelist.City;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import oneprofile.backend.storage.placelist.PlaceListStore;
import org.junit.jupiter.api.Test;

class EligibilityRuleTest {

	private static final PlaceListStore PLACES = PlaceListConfiguration.shipped();

	private final EligibilityRule rule = new EligibilityRule(PLACES);

	private static final Place BERLIN = Place.city("DE", "Berlin", "Berlin", 52.52437, 13.41053);

	private static final Place NEW_YORK = Place.city("US", "New York", "New York City", 40.71427, -74.00597);

	private static final Place BUENOS_AIRES = Place.city("AR", "Buenos Aires F.D.", "Buenos Aires", -34.61315,
			-58.37723);

	@Test
	void aRemoteVacancysDeclaredLocationCountsOnlyForEligibility() {
		CountrySet set = countrySet(option(WorkModeEnum.REMOTE, "Remote - US", Place.country("US")));

		assertThat(this.rule.eligibility(set, "US", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.ELIGIBLE, StatedSetSourceEnum.LOCATION));
		assertThat(this.rule.eligibility(set, "AR", null)).isEqualTo(Eligibility.UNKNOWN);
	}

	@Test
	void anUndeclaredWorkModeIsReadAsARemoteOne() {
		CountrySet set = countrySet(option(null, "Berlin, Germany", BERLIN));

		assertThat(this.rule.eligibility(set, "AR", null)).isEqualTo(Eligibility.UNKNOWN);
		assertThat(this.rule.eligibility(set, "DE", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void anOnsiteVacancysDeclaredLocationIsItsCountrySet() {
		CountrySet set = countrySet(option(WorkModeEnum.ONSITE, "Onsite, Berlin", BERLIN));

		assertThat(this.rule.eligibility(set, "AR", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.EXCLUDED, StatedSetSourceEnum.LOCATION));
		assertThat(this.rule.eligibility(set, "DE", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void anOnsiteVacancyWithACityIsJudgedByDistanceOnceThePersonsCityIsKnown() {
		CountrySet set = countrySet(option(WorkModeEnum.HYBRID, "Buenos Aires (Hybrid)", BUENOS_AIRES));

		assertThat(this.rule.eligibility(set, "AR", city("Quilmes", "AR")).state())
			.isEqualTo(EligibilityStateEnum.ELIGIBLE);
		assertThat(this.rule.eligibility(set, "AR", city("Córdoba", "AR")).state())
			.isEqualTo(EligibilityStateEnum.EXCLUDED);
		assertThat(this.rule.eligibility(set, "AR", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void anOnsiteOptionWithoutACityIsJudgedByCountryWhateverThePersonsCity() {
		CountrySet set = countrySet(option(WorkModeEnum.ONSITE, "Argentina (Onsite)", Place.country("AR")));

		assertThat(this.rule.eligibility(set, "AR", city("Córdoba", "AR")).state())
			.isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void aChoiceOfWorkModesTakesTheBestStateOfItsOptions() {
		CountrySet set = countrySet(option(WorkModeEnum.HYBRID, "NYC (Hybrid)", NEW_YORK),
				option(WorkModeEnum.REMOTE, "United States (Remote)", Place.country("US")));

		assertThat(this.rule.eligibility(set, "AR", null)).isEqualTo(Eligibility.UNKNOWN);
		assertThat(this.rule.eligibility(set, "US", city("Los Angeles", "US")).state())
			.isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void workAuthorizationCountsAsASetOfItsCountry() {
		CountrySet set = countrySet(option(WorkModeEnum.REMOTE, "Remote", new Place[0]),
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.AUTHORIZATION,
						"You must be authorized to work in the US.", Place.country("US")));

		assertThat(this.rule.eligibility(set, "AR", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.EXCLUDED, StatedSetSourceEnum.DESCRIPTION));
		assertThat(this.rule.eligibility(set, "US", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.ELIGIBLE, StatedSetSourceEnum.DESCRIPTION));
	}

	@Test
	void theDescriptionExcludesEvenWhereTheLocationNamesTheCountry() {
		CountrySet set = countrySet(option(WorkModeEnum.REMOTE, "Remote - Argentina", Place.country("AR")),
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.CITIZENSHIP, "US citizens only.",
						Place.country("US")));

		assertThat(this.rule.eligibility(set, "AR", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.EXCLUDED, StatedSetSourceEnum.DESCRIPTION));
	}

	@Test
	void aRegionIncludesItsCountriesAndExcludesTheRest() {
		CountrySet set = countrySet(stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.REGION,
				"This role is open to candidates in EMEA.", Place.region("EMEA")));

		assertThat(this.rule.eligibility(set, "DE", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
		assertThat(this.rule.eligibility(set, "AR", null).state()).isEqualTo(EligibilityStateEnum.EXCLUDED);
	}

	@Test
	void theDescriptionsLocationsAreOneSet() {
		CountrySet set = countrySet(
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.LOCATION, "Remote within the US.",
						Place.country("US")),
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.LOCATION, "We also hire in Canada.",
						Place.country("CA")));

		assertThat(this.rule.eligibility(set, "CA", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
		assertThat(this.rule.eligibility(set, "AR", null).state()).isEqualTo(EligibilityStateEnum.EXCLUDED);
	}

	@Test
	void eachRequirementMustHoldBesideTheDescriptionsLocations() {
		CountrySet set = countrySet(
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.LOCATION, "We hire in Canada.",
						Place.country("CA")),
				stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.AUTHORIZATION,
						"You must be authorized to work in the US.", Place.country("US")));

		assertThat(this.rule.eligibility(set, "CA", null).state()).isEqualTo(EligibilityStateEnum.EXCLUDED);
	}

	@Test
	void aGlobalSetIncludesEveryCountry() {
		CountrySet set = countrySet(stated(StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.REGION,
				"We hire anywhere in the world.", Place.region("Worldwide")));

		assertThat(this.rule.eligibility(set, "AR", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
	}

	@Test
	void aRegionInTheTitleCountsOnlyForEligibility() {
		CountrySet set = countrySet(option(WorkModeEnum.REMOTE, "Remote", new Place[0]),
				stated(StatedSetSourceEnum.TITLE, StatedSetKindEnum.REGION, "Developer - Remote, Latin America",
						Place.region("Latin America")));

		assertThat(this.rule.eligibility(set, "AR", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.ELIGIBLE, StatedSetSourceEnum.TITLE));
		assertThat(this.rule.eligibility(set, "US", null)).isEqualTo(Eligibility.UNKNOWN);
	}

	@Test
	void aClearanceInTheTitleExcludesOtherCountries() {
		CountrySet set = countrySet(stated(StatedSetSourceEnum.TITLE, StatedSetKindEnum.CLEARANCE,
				"Cloud Engineer - TS/SCI w/ Poly", Place.country("US")));

		assertThat(this.rule.eligibility(set, "AR", null))
			.isEqualTo(new Eligibility(EligibilityStateEnum.EXCLUDED, StatedSetSourceEnum.TITLE));
	}

	@Test
	void aTimezoneRequirementIsJudgedByThePersonsCityThenByTheCountry() {
		CountrySet set = countrySet(timezone("Candidates in UTC-5 to UTC+1.", -300, 60));

		assertThat(this.rule.eligibility(set, "AR", null).state()).isEqualTo(EligibilityStateEnum.ELIGIBLE);
		assertThat(this.rule.eligibility(set, "JP", null).state()).isEqualTo(EligibilityStateEnum.EXCLUDED);
		assertThat(this.rule.eligibility(set, "US", null)).isEqualTo(Eligibility.UNKNOWN);
		assertThat(this.rule.eligibility(set, "US", city("New York City", "US")).state())
			.isEqualTo(EligibilityStateEnum.ELIGIBLE);
		assertThat(this.rule.eligibility(set, "US", city("Los Angeles", "US")).state())
			.isEqualTo(EligibilityStateEnum.EXCLUDED);
	}

	@Test
	void aVacancyThatStatesNothingIsUnknown() {
		assertThat(this.rule.eligibility(countrySet(), "AR", null)).isEqualTo(Eligibility.UNKNOWN);
		assertThat(this.rule.eligibility(countrySet(option(WorkModeEnum.ONSITE, "Onsite", new Place[0])), "AR", null))
			.isEqualTo(Eligibility.UNKNOWN);
	}

	private static City city(String name, String country) {
		return PLACES.cities().stream()
			.filter((city) -> city.name().equals(name) && city.country().equals(country))
			.max(Comparator.comparingLong(City::population))
			.orElseThrow();
	}

	private static StatedSet option(WorkModeEnum workMode, String text, Place... places) {
		StatedSetKindEnum kind = List.of(places).stream().anyMatch((place) -> place.region() != null)
				? StatedSetKindEnum.REGION : StatedSetKindEnum.LOCATION;
		return new StatedSet(0, StatedSetSourceEnum.LOCATION, kind, workMode, null, text, List.of(places), null, null);
	}

	private static StatedSet stated(StatedSetSourceEnum source, StatedSetKindEnum kind, String text, Place... places) {
		return new StatedSet(0, source, kind, null, (source == StatedSetSourceEnum.DESCRIPTION) ? 0 : null, text,
				List.of(places), null, null);
	}

	private static StatedSet timezone(String text, int from, int to) {
		return new StatedSet(0, StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.TIMEZONE, null, 0, text, List.of(),
				from, to);
	}

	private static CountrySet countrySet(StatedSet... sets) {
		List<StatedSet> positioned = new ArrayList<>();
		for (int i = 0; i < sets.length; i++) {
			StatedSet set = sets[i];
			positioned.add(new StatedSet(i, set.source(), set.kind(), set.workMode(), set.segment(), set.text(),
					set.places(), set.utcFrom(), set.utcTo()));
		}
		return new CountrySet(positioned, false, false);
	}

}
