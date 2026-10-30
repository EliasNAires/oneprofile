package oneprofile.backend.storage.placelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class PlaceListStoreTest {

	private static final String COUNTRIES = """
			code\tiso3\tname
			AR\tARG\tArgentina
			US\tUSA\tUnited States
			""";

	private static final String SUBDIVISIONS = """
			code\tname\tascii
			US.MD\tMaryland\tMaryland
			""";

	private static final String CITIES = """
			id\tname\tascii\tcountry\tsubdivision\tlatitude\tlongitude\tpopulation\ttimezone
			4347778\tBaltimore\tBaltimore\tUS\tMD\t39.29038\t-76.61219\t576498\tAmerica/New_York
			""";

	private static final String TIME_ZONES = """
			timezone\tcountry\toffset
			America/Argentina/Buenos_Aires\tAR\t-3.0
			America/Los_Angeles\tUS\t-8.0
			America/New_York\tUS\t-5.0
			Asia/Kolkata\tIN\t5.5
			""";

	private static final String REGIONS = """
			region\tcountry
			Americas\tAR
			Americas\tUS
			Worldwide\t*
			""";

	@Test
	void readsGeoNamesFilesAndTheHandTable() {
		PlaceListStore list = read("""
				name\tkind\ttarget\tlatitude\tlongitude
				USA\tcountry\tUS\t\t
				Charm City\tcity\t4347778\t\t
				Fort Meade\ttown\tUS.MD\t39.10838\t-76.74343
				AMER\tregion\tAmericas\t\t
				HQ\tignored\t\t\t
				""");

		assertThat(list.countries()).containsExactly(new Country("AR", "ARG", "Argentina"),
				new Country("US", "USA", "United States"));
		assertThat(list.subdivisions()).containsExactly(new Subdivision("US.MD", "Maryland", "Maryland"));
		assertThat(list.cities()).containsExactly(
				new City(4347778, "Baltimore", "Baltimore", "US", "MD", 39.29038, -76.61219, 576498, "America/New_York"));
		assertThat(list.hand()).containsExactly(new HandPlace("USA", HandPlaceKindEnum.COUNTRY, "US", null, null),
				new HandPlace("Charm City", HandPlaceKindEnum.CITY, "4347778", null, null),
				new HandPlace("Fort Meade", HandPlaceKindEnum.TOWN, "US.MD", 39.10838, -76.74343),
				new HandPlace("AMER", HandPlaceKindEnum.REGION, "Americas", null, null),
				new HandPlace("HQ", HandPlaceKindEnum.IGNORED, "", null, null));
	}

	@Test
	void givesTheUtcOffsetsOfATimeZoneAndOfACountry() {
		PlaceListStore list = read("name\tkind\ttarget\tlatitude\tlongitude\n");

		assertThat(list.offsetOf("America/New_York")).isEqualTo(-300);
		assertThat(list.offsetOf("Asia/Kolkata")).isEqualTo(330);
		assertThat(list.offsetOf("Europe/Nowhere")).isNull();
		assertThat(list.offsetsOf("US")).containsExactlyInAnyOrder(-480, -300);
		assertThat(list.offsetsOf("GB")).isEmpty();
	}

	@Test
	void expandsARegionIntoItsCountriesAndWorldwideIntoEveryCountry() {
		PlaceListStore list = read("name\tkind\ttarget\tlatitude\tlongitude\nAMER\tregion\tAmericas\t\t\n");

		assertThat(list.countriesOf("Americas")).containsExactlyInAnyOrder("AR", "US");
		assertThat(list.countriesOf("Worldwide")).containsExactlyInAnyOrder("AR", "US");
	}

	@Test
	void refusesARegionTheRegionsDoNotExpand() {
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nEMEA\tregion\tEMEA\t\t\n"))
			.withMessageContaining("EMEA");
		assertThatIllegalStateException()
			.isThrownBy(() -> PlaceListStore.read(new StringReader(COUNTRIES), new StringReader(SUBDIVISIONS),
					new StringReader(CITIES), new StringReader("name\tkind\ttarget\tlatitude\tlongitude\n"),
					new StringReader(TIME_ZONES), new StringReader("region\tcountry\nDACH\tDE\n")))
			.withMessageContaining("DE");
	}

	@Test
	void refusesAHandRowNamingWhatTheListDoesNotHold() {
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nUK\tcountry\tGB\t\t\n"))
			.withMessageContaining("GB");
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nON\tsubdivision\tCA.08\t\t\n"))
			.withMessageContaining("CA.08");
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nBangalore\tcity\t1277333\t\t\n"))
			.withMessageContaining("1277333");
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nSingapore\tbare\tSG\t\t\n"))
			.withMessageContaining("SG");
	}

	@Test
	void refusesATownWithoutItsCoordinates() {
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nFort Meade\ttown\tUS.MD\t\t\n"))
			.withMessageContaining("Fort Meade");
	}

	@Test
	void refusesARowListedTwice() {
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nUSA\tcountry\tUS\t\t\nusa\tcountry\tUS\t\t\n"))
			.withMessageContaining("usa");
	}

	@Test
	void refusesTwoMeaningsOfANameAlone() {
		assertThatIllegalStateException()
			.isThrownBy(() -> read("name\tkind\ttarget\tlatitude\tlongitude\nMD\tbare\tUS\t\t\nMD\tbare\tUS.MD\t\t\n"))
			.withMessageContaining("MD");
	}

	@Test
	void holdsANameThatMeansSeveralThings() {
		assertThat(read("""
				name\tkind\ttarget\tlatitude\tlongitude
				Bastrop\ttown\tUS.MD\t30.11049\t-97.31527
				Bastrop\ttown\tUS\t32.77828\t-91.91144
				""").hand()).hasSize(2);
	}

	private static PlaceListStore read(String hand) {
		return PlaceListStore.read(new StringReader(COUNTRIES), new StringReader(SUBDIVISIONS),
				new StringReader(CITIES), new StringReader(hand), new StringReader(TIME_ZONES), new StringReader(REGIONS));
	}

}
