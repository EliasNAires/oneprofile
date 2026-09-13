package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.model.WorkMode;
import oneprofile.backend.util.WorkModeExtractor.Extracted;

import org.junit.jupiter.api.Test;

/**
 * Every title and every location here is a real one, taken from the measurements run
 * against the 128.953 vacancies of the database; none was made up.
 */
class WorkModeExtractorTest {

	@Test
	void readsTheModeFromTheLocation() {
		// Where 14.624 of the 16.444 remote vacancies say it, and the title says nothing.
		assertThat(extract("Software Engineer", "Remote - US"))
				.isEqualTo(new Extracted("software engineer", WorkMode.REMOTE));
		assertThat(extract("Account Executive", "United States - Remote"))
				.isEqualTo(new Extracted("account executive", WorkMode.REMOTE));
	}

	@Test
	void readsTheModeFromTheTitleWhenTheLocationNamesNone() {
		assertThat(extract("Remote Therapist", "Chicago, IL"))
				.isEqualTo(new Extracted("therapist", WorkMode.REMOTE));
		assertThat(extract("Field Sales Manager - Hybrid", "Austin, TX"))
				.isEqualTo(new Extracted("field sales manager", WorkMode.HYBRID));
	}

	@Test
	void callsItFullyRemoteWhenNoPlaceIsNamedAtAll() {
		// 2.736 vacancies have exactly this location, and anyone can apply to them.
		assertThat(extract("Senior Product Designer", "Remote"))
				.isEqualTo(new Extracted("senior product designer", WorkMode.FULLY_REMOTE));
		assertThat(extract("Remote Data Entry Specialist", null))
				.isEqualTo(new Extracted("data entry specialist", WorkMode.FULLY_REMOTE));
	}

	@Test
	void staysRemoteWhenThePlaceNarrowsIt() {
		assertThat(extract("Software Engineer", "Remote, United States").mode()).isEqualTo(WorkMode.REMOTE);
		assertThat(extract("Software Engineer", "US Remote").mode()).isEqualTo(WorkMode.REMOTE);
	}

	@Test
	void takesTheMostSpecificWhenOneSourceNamesMoreThanOne() {
		assertThat(extract("Sales Representative", "Hybrid Remote - London").mode()).isEqualTo(WorkMode.HYBRID);
	}

	@Test
	void takesThePhraseOutWhole() {
		// Dropping only the "remote" of "fully remote" would leave a dangling "fully".
		assertThat(extract("Fully Remote Behavior Technician", null).title()).isEqualTo("behavior technician");
		assertThat(extract("Work From Home Customer Support", "United States").title())
				.isEqualTo("customer support");
	}

	@Test
	void readsTheSpellingsThatAreNotEnglish() {
		assertThat(extract("Desarrollador Backend Remoto", null).mode()).isEqualTo(WorkMode.FULLY_REMOTE);
		assertThat(extract("Comercial Presencial", "Madrid").mode()).isEqualTo(WorkMode.ONSITE);
	}

	@Test
	void givesNullWhenNeitherSourceSaysIt() {
		assertThat(extract("Behavior Technician", "New York, NY"))
				.isEqualTo(new Extracted("behavior technician", null));
	}

	@Test
	void givesNullTitleWhenTheModeWasTheWholeTitle() {
		assertThat(extract("Remote", "London, United Kingdom")).isEqualTo(new Extracted(null, WorkMode.REMOTE));
	}

	private static Extracted extract(String rawTitle, String location) {
		return WorkModeExtractor.extract(TitleCleaner.clean(rawTitle), location);
	}
}
