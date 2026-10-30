package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import org.junit.jupiter.api.Test;

/** Location strings of the corpus, read by the place list the application ships. */
class PlaceRuleTest {

	private final PlaceRule rule = new PlaceRule(PlaceListConfiguration.shipped());

	@Test
	void givesACityItsCoordinates() {
		assertThat(this.rule.read("Paris, TX", null).places())
			.containsExactly(Place.city("US", "Texas", "Paris", 33.66094, -95.55551));
	}

	@Test
	void readsABareCityAsItsMostPopulousMatch() {
		assertPlaces("Paris", "Paris, Île-de-France, FR");
		assertPlaces("London", "London, England, GB");
	}

	@Test
	void readsACityByTheStateOrCountryTheStringNames() {
		assertPlaces("Paris, Texas", "Paris, Texas, US");
		assertPlaces("London, Ontario", "London, Ontario, CA");
		assertPlaces("Toronto, CA", "Toronto, Ontario, CA");
		assertPlaces("Santa Clara, CA", "Santa Clara, California, US");
		assertPlaces("US, CA, Santa Clara", "Santa Clara, California, US");
		assertPlaces("Vancouver, WA", "Vancouver, Washington, US");
	}

	@Test
	void readsAsPreciseAPlaceAsTheStringGives() {
		assertPlaces("United States", "US");
		assertPlaces("Remote TX", "Texas, US");
		assertPlaces("Remote - United States", "US");
		assertPlaces("Bengaluru, Karnātaka, India", "Bengaluru, Karnataka, IN");
	}

	@Test
	void readsEachPlaceAStringNames() {
		assertPlaces("Chicago, IL, Cleveland, OH", "Chicago, Illinois, US", "Cleveland, Ohio, US");
		assertPlaces("Denver, CO; New York City, NY", "Denver, Colorado, US", "New York City, New York, US");
		assertPlaces("Atlanta, GA; Tempe, AZ", "Atlanta, Georgia, US", "Tempe, Arizona, US");
		assertPlaces("Berlin or Munich", "Berlin, State of Berlin, DE", "Munich, Bavaria, DE");
	}

	@Test
	void keepsARegionAsNamed() {
		assertPlaces("Remote - EMEA", "EMEA");
		assertPlaces("LATAM", "Latin America");
		assertPlaces("Remote (North America)", "North America");
	}

	@Test
	void readsAnywhereAsWorldwideOnlyWhereTheStringNamesNoOtherPlace() {
		assertPlaces("Home based - Worldwide", "Worldwide");
		assertPlaces("Anywhere", "Worldwide");
		assertPlaces("Anywhere in the U.S.", "US");
		assertPlaces("Anywhere in France, Belgium, Spain", "FR", "BE", "ES");
		assertPlaces("Anywhere in Latin America", "Latin America");
	}

	@Test
	void readsNothingInAStringThatNamesNoPlace() {
		PlaceReading remote = this.rule.read("Remote", null);
		assertThat(remote.places()).isEmpty();
		assertThat(remote.residue()).isFalse();
	}

	@Test
	void reportsAStringItCannotReadAsResidue() {
		PlaceReading unknown = this.rule.read("PICKLE RESEARCH CAMPUS", null);
		assertThat(unknown.places()).isEmpty();
		assertThat(unknown.unread()).containsExactly("PICKLE", "RESEARCH");
		assertThat(unknown.residue()).isTrue();
	}

	@Test
	void readsANameThatIsACountrysAsTheCountry() {
		assertPlaces("Mexico", "MX");
		assertPlaces("Georgia, USA", "Georgia, US");
		assertPlaces("Atlanta, Georgia", "Atlanta, Georgia, US");
	}

	@Test
	void readsANameAloneAsTheHandTableSays() {
		assertPlaces("Singapore", "Singapore, SG");
		assertPlaces("Singapore, Singapore", "Singapore, SG");
		assertPlaces("Florida", "Florida, US");
		assertPlaces("New Orleans, LA", "New Orleans, Louisiana, US");
		assertPlaces("LA", "Los Angeles, California, US");
	}

	@Test
	void readsOtherNamesFromTheHandTable() {
		assertPlaces("Bangalore", "Bengaluru, Karnataka, IN");
		assertPlaces("New York, NY", "New York City, New York, US");
		assertPlaces("Fort Meade, MD", "Fort Meade, Maryland, US");
		assertPlaces("Ft. Collins, Colorado, United States of America", "Fort Collins, Colorado, US");
	}

	@Test
	void readsAStateAfterATownTheListDoesNotHold() {
		assertPlaces("Malvern, PA", "Pennsylvania, US");
		assertPlaces("Westlake, TX", "Texas, US");
		assertPlaces("USA - Berkeley, MO", "Missouri, US");
		assertPlaces("IL-Rosemont", "Illinois, US");
	}

	@Test
	void keepsALargeCityBesideAnOutdatedState() {
		assertPlaces("Hyderabad, Andhra Pradesh, India", "Hyderabad, Telangana, IN");
		assertPlaces("Kuala Lumpur, Selangor, Malaysia", "Kuala Lumpur, Kuala Lumpur, MY");
	}

	@Test
	void readsANameBesideUnreadWordsOnlyAsPartOfAPlace() {
		assertPlaces("Pune, Gera Commerzone SEZ", "Pune, Maharashtra, IN");
		assertPlaces("Hyderabad - Phoenix Aquila, India", "Hyderabad, Telangana, IN");
		assertPlaces("MUFG Global Service Private Ltd. - Bengaluru (BCIT)", "Bengaluru, Karnataka, IN");
		assertPlaces("Bengaluru Millenia", "Bengaluru, Karnataka, IN");
	}

	@Test
	void readsTheStringsOfWorkdayAndItsDots() {
		assertPlaces("USA.VA.Reston", "Reston, Virginia, US");
		assertPlaces("United States-Colorado-Schriever AFB", "Schriever AFB, Colorado, US");
		assertPlaces("NY-New York", "New York City, New York, US");
		assertPlaces("Washington, D.C.", "Washington, District of Columbia, US");
		assertPlaces("St. Louis, MO", "St. Louis, Missouri, US");
		assertPlaces("Winston-Salem, NC", "Winston-Salem, North Carolina, US");
	}

	@Test
	void readsWithinTheCountryTheAtsNames() {
		assertThat(describe(this.rule.read("Remote", "United States"))).containsExactly("US");
		assertThat(describe(this.rule.read("London", "USA"))).containsExactly("US");
		assertThat(describe(this.rule.read("Cambridge", "United States"))).containsExactly("Cambridge, Massachusetts, US");
		assertThat(describe(this.rule.read("Georgia", "United States"))).containsExactly("Georgia, US");
		assertThat(describe(this.rule.read("Remote - EMEA", "Germany"))).containsExactly("EMEA", "DE");
	}

	@Test
	void leavesUnreadANameTheAtsCountryDoesNotHold() {
		assertThat(this.rule.read("London", "USA").unread()).containsExactly("London");
	}

	private void assertPlaces(String location, String... places) {
		assertThat(describe(this.rule.read(location, null))).as(location).containsExactly(places);
	}

	/** Each place as a person writes it: the city, the subdivision and the country, or the region. */
	private static List<String> describe(PlaceReading reading) {
		return reading.places().stream()
			.map((place) -> (place.region() != null) ? place.region()
					: String.join(", ", Stream.of(place.city(), place.subdivision(), place.country())
						.filter((part) -> part != null).toList()))
			.toList();
	}

}
