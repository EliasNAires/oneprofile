package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorkModeRuleTest {

	private final WorkModeRule rule = new WorkModeRule(new TitleCleaningRule());

	@Test
	void readsTheModeTheLocationNames() {
		assertThat(this.rule.read("Remote - USA", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.REMOTE, WorkModeEvidenceEnum.LOCATION));
		assertThat(this.rule.read("New York, NY (Hybrid)", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.HYBRID, WorkModeEvidenceEnum.LOCATION));
		assertThat(this.rule.read("Austin, TX - On-site", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.ONSITE, WorkModeEvidenceEnum.LOCATION));
	}

	@Test
	void takesTheLocationOverAFieldThatDisagreesWithIt() {
		assertThat(this.rule.read("Remote - USA", "Hybrid", "Software Engineer"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.REMOTE, WorkModeEvidenceEnum.LOCATION));
	}

	@Test
	void readsHybridWhereTheLocationNamesItBesideAnotherMode() {
		assertThat(this.rule.read("Dallas, TX - Hybrid (3x in office/week)", null, "Engineer").mode())
			.isEqualTo(WorkModeEnum.HYBRID);
		assertThat(this.rule.read("Remote/Hybrid", null, "Engineer").mode()).isEqualTo(WorkModeEnum.HYBRID);
	}

	@Test
	void readsRemoteWhereTheLocationNamesItBesideOnsite() {
		assertThat(this.rule.read("Nashville, TN (Onsite) or Remote", null, "Engineer").mode())
			.isEqualTo(WorkModeEnum.REMOTE);
	}

	@Test
	void readsTheFieldWhereTheLocationNamesNoMode() {
		assertThat(this.rule.read("San Francisco, CA", "Hybrid", "Remote Software Engineer"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.HYBRID, WorkModeEvidenceEnum.FIELD));
	}

	@ParameterizedTest
	@CsvSource({ "OnSite, ONSITE", "Hybrid, HYBRID", "Remote, REMOTE", "onsite, ONSITE", "hybrid, HYBRID",
			"remote, REMOTE", "On Site, ONSITE", "Fully Onsite, ONSITE", "Onsite Job, ONSITE", "Office, ONSITE",
			"Office Worker (NOT Remote), ONSITE", "Hybrid Working, HYBRID", "Fully Remote, REMOTE" })
	void mapsTheFieldsValuesToAMode(String field, WorkModeEnum mode) {
		assertThat(this.rule.read("Berlin", field, "Engineer"))
			.isEqualTo(new WorkModeReading(mode, WorkModeEvidenceEnum.FIELD));
	}

	@ParameterizedTest
	@CsvSource({ "unspecified", "Field-based", "Engineering", "On-Site/Flex Friday" })
	void takesAFieldValueTheVocabularyDoesNotMapAsNoAnswer(String field) {
		assertThat(this.rule.read("Berlin", field, "Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void readsTheTitleWhereNeitherTheLocationNorTheFieldNamesAMode() {
		assertThat(this.rule.read("Berlin", "unspecified", "Senior React Developer - Remote, Latin America"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.REMOTE, WorkModeEvidenceEnum.TITLE));
		assertThat(this.rule.read(null, null, "Software Engineer (Hybrid)"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.HYBRID, WorkModeEvidenceEnum.TITLE));
		assertThat(this.rule.read(null, null, "Desarrollador Java Presencial"))
			.isEqualTo(new WorkModeReading(WorkModeEnum.ONSITE, WorkModeEvidenceEnum.TITLE));
	}

	@Test
	void doesNotReadAWorkModeWordThatNamesWhatTheWorkIsAbout() {
		assertThat(this.rule.read(null, null, "Hybrid Cloud Engineer")).isEqualTo(WorkModeReading.NONE);
		assertThat(this.rule.read(null, null, "Remote Sensing Scientist")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void isNullWhereNothingNamesAMode() {
		assertThat(this.rule.read("London, UK", null, "Backend Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void neverReadsOnsiteFromAnOfficeTheLocationNames() {
		assertThat(this.rule.read("London Office", null, "Backend Engineer")).isEqualTo(WorkModeReading.NONE);
	}

}
