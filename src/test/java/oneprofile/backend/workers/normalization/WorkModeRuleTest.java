package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorkModeRuleTest {

	private final WorkModeRule rule = new WorkModeRule(new TitleCleaningRule());

	@Test
	void readsTheModeTheLocationNames() {
		assertThat(read("Remote - USA", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.LOCATION));
		assertThat(read("New York, NY (Hybrid)", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.HYBRID), WorkModeEvidenceEnum.LOCATION));
		assertThat(read("Austin, TX - On-site", null, "Software Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.ONSITE), WorkModeEvidenceEnum.LOCATION));
	}

	@Test
	void takesTheLocationOverAFieldThatDisagreesWithIt() {
		assertThat(read("Remote - USA", "Hybrid", "Software Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.LOCATION));
	}

	@Test
	void keepsEveryModeALocationOfferingAChoiceNames() {
		assertThat(read("Nashville, TN (Onsite) or Remote", null, "Engineer").modes())
			.containsExactlyInAnyOrder(WorkModeEnum.ONSITE, WorkModeEnum.REMOTE);
		assertThat(read("Remote/Hybrid", null, "Engineer").modes())
			.containsExactlyInAnyOrder(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID);
		assertThat(read("San Francisco, CA (Hybrid) or US-Remote", null, "Engineer").modes())
			.containsExactlyInAnyOrder(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE);
	}

	@Test
	void readsAModeWordWithDaysAWeekInTheOfficeAsHybridAlone() {
		assertThat(read("Dallas, TX - Hybrid (3x in office/week)", null, "Engineer").modes())
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(read("Remote, with 2 days a week in office", null, "Engineer").modes())
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(read(null, null, "Software Engineer - ONSITE 2X Week"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.HYBRID), WorkModeEvidenceEnum.TITLE));
	}

	@Test
	void readsAChoiceWhoseHybridOptionNamesItsDaysAsAChoice() {
		assertThat(read("Remote or Hybrid (2 days a week in office)", null, "Engineer").modes())
			.containsExactlyInAnyOrder(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID);
	}

	@Test
	void readsFiveDaysAWeekInTheOfficeAsOnsite() {
		assertThat(read("Austin, TX - Onsite 5 days a week", null, "Engineer").modes())
			.containsExactly(WorkModeEnum.ONSITE);
	}

	@Test
	void readsTheFieldWhereTheLocationNamesNoMode() {
		assertThat(read("San Francisco, CA", "Hybrid", "Remote Software Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.HYBRID), WorkModeEvidenceEnum.FIELD));
	}

	@ParameterizedTest
	@CsvSource({ "OnSite, ONSITE", "Hybrid, HYBRID", "Remote, REMOTE", "onsite, ONSITE", "hybrid, HYBRID",
			"remote, REMOTE", "On Site, ONSITE", "Fully Onsite, ONSITE", "Onsite Job, ONSITE", "Office, ONSITE",
			"Office Worker (NOT Remote), ONSITE", "Hybrid Working, HYBRID", "Fully Remote, REMOTE" })
	void mapsTheFieldsValuesToAMode(String field, WorkModeEnum mode) {
		assertThat(read("Berlin", field, "Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(mode), WorkModeEvidenceEnum.FIELD));
	}

	@ParameterizedTest
	@CsvSource({ "unspecified", "Field-based", "Engineering", "On-Site/Flex Friday" })
	void takesAFieldValueTheVocabularyDoesNotMapAsNoAnswer(String field) {
		assertThat(read("Berlin", field, "Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void readsTheTitleWhereNeitherTheLocationNorTheFieldNamesAMode() {
		assertThat(read("Berlin", "unspecified", "Senior React Developer - Remote, Latin America"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.TITLE));
		assertThat(read(null, null, "Software Engineer (Hybrid)"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.HYBRID), WorkModeEvidenceEnum.TITLE));
		assertThat(read(null, null, "Desarrollador Java Presencial"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.ONSITE), WorkModeEvidenceEnum.TITLE));
	}

	@Test
	void doesNotReadAWorkModeWordThatNamesWhatTheWorkIsAbout() {
		assertThat(read(null, null, "Hybrid Cloud Engineer")).isEqualTo(WorkModeReading.NONE);
		assertThat(read(null, null, "Remote Sensing Scientist")).isEqualTo(WorkModeReading.NONE);
		assertThat(read(null, null, "Tech Lead, Remote Assist - Robotaxi")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void isNullWhereNothingNamesAMode() {
		assertThat(read("London, UK", null, "Backend Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void readsTheDescriptionOnlyWhereTheLocationTheFieldAndTheTitleAreSilent() {
		assertThat(this.rule.read("Remote - USA", null, "Engineer", described("This position is 100% onsite.")))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.LOCATION));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", described("This position is 100% onsite.")))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.ONSITE), WorkModeEvidenceEnum.DESCRIPTION));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "LI-Hybrid | HYBRID", "#LI-Remote | REMOTE", "LI-ONSITE | ONSITE",
			"Li-remote #full-time | REMOTE" })
	void readsTheWorkModeTagOfTheDescription(String tag, WorkModeEnum mode) {
		assertThat(modesIn(tag)).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Workplace type: Remote | REMOTE", "Location: Remote (Europe Preferred) | REMOTE",
			"Work Flexibility: Hybrid | HYBRID", "Location: On-site | ONSITE",
			"Work Location: Hybrid: 3 days onsite in Quantico VA; 2 days remote. | HYBRID" })
	void readsAWorkModeLabelWithItsValue(String label, WorkModeEnum mode) {
		assertThat(modesIn(label)).containsExactly(mode);
	}

	@Test
	void readsAWorkModeLabelWhoseValueIsTheNextSegment() {
		assertThat(modesIn("Travel: Less than 10%", "Telecommuting Options", "Onsite")).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn("Remote Type -", "Hybrid")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("Workplace type", "On-site Working")).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"This role has been designed as 'Hybrid' with a requirement that you will work on average 2 days per week from an HPE office. | HYBRID",
			"This role has been designated as ‘Remote/Teleworker’, which means you will primarily work from home. | REMOTE",
			"This position is expected to be 100% onsite. | ONSITE",
			"This position is 100% remote anywhere in the US | REMOTE",
			"Hybrid Role (Minimum of 3 days a week in the office) | HYBRID",
			"This is a hybrid position based in our NYC Financial District office, requiring on-site work Tuesday through Thursday and remote work on Monday and Friday. | HYBRID",
			"This position is located in Arlington, VA and is in office Monday through Thursday with work from home on Friday. | HYBRID",
			"This position is remote, with occasional travel to our onsite team meetings. | REMOTE",
			"This is a fully remote role. | REMOTE",
			"In this hybrid role, you'll have a defined work location that includes working from home and a minimum of three days in the office. | HYBRID",
			"This role requires you to be onsite. Remote and/or hybrid work will not be considered | ONSITE",
			"This position is a hybrid of office/remote working | HYBRID",
			"Location: Hybrid attendance (3x/week in-office) at one of our office locations | HYBRID",
			"This role reports to the Manager II.&#xa;&#xa;This is a remote position which may require occasional in-person attendance. | REMOTE",
			"Employees in this role will work in the office Monday-Thursday, with the flexibility to work remotely on Friday. | HYBRID",
			"Job Location: Remote, however, strong preference for candidates who live in the Washington DC Metro Area. | REMOTE",
			"Under the Company's current hybrid-work expectations, employees in this position work onsite Tuesday through Thursday each week. | HYBRID",
			"This position is onsite Monday through Friday. | ONSITE",
			"This role is onsite in our Austin or Dallas office, with one day a week from home. | HYBRID",
			"This role requires you to be onsite in Hawthorne, CA; remote or hybrid work will not be considered. | ONSITE" })
	void readsASentenceStatingTheModeOfThisRole(String sentence, WorkModeEnum mode) {
		assertThat(modesIn(sentence)).containsExactly(mode);
	}

	@Test
	void keepsEveryModeASentenceOffersAChoiceOf() {
		assertThat(modesIn("This position can be hybrid or remote depending on your location."))
			.containsExactlyInAnyOrder(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Our culture pioneered hybrid work trends.", "Free on-site parking",
			"Hybrid/flexible working options", "Experience building hybrid search over vector stores.",
			"Support onsite and remote end users.", "We're a remote-first company.",
			"Remote: If this position is listed as remote, there may still be occasions when you are required to work in person at a customer facility.",
			"Location: This role is not remote.",
			"Due to customer requirements, hybrid and remote working are not available for this role.",
			"This role supports our remote sensing payloads.", "Remote", "Hybrid working",
			"In this role, you will lead major customer engagements, both remotely and on-site.",
			"Applicants should expect that the role will require the ability to convene with colleagues in person.",
			"The opportunity to collaborate in the office and work from home part-time has promoted team-building.",
			"This role offers free on-site parking.", "This role will support onsite and remote teams.",
			"This role provides onsite support to our clinics." })
	void neverReadsAModeFromAWordThatIsNotAboutThisRole(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void takesASentenceAboutThisRoleOverALabelAndALabelOverATag() {
		assertThat(modesIn("LI-Remote", "This role is onsite in our Austin office.")).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn("LI-Remote", "Workplace type: Hybrid")).containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoModeWhereSentencesAboutThisRoleContradictEachOther() {
		assertThat(modesIn("This role is fully remote.", "This position is onsite five days a week.")).isEmpty();
	}

	@Test
	void neverReadsOnsiteFromAnOfficeTheLocationNames() {
		assertThat(read("London Office", null, "Backend Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	private WorkModeReading read(String location, String field, String title) {
		return this.rule.read(location, field, title, List.of());
	}

	private Set<WorkModeEnum> modesIn(String... description) {
		return this.rule.read("Austin, TX", null, "Engineer", described(description)).modes();
	}

	private static List<Segment> described(String... texts) {
		List<Segment> segments = new ArrayList<>();
		for (String text : texts) {
			segments.add(new Segment(SegmentKindEnum.SENTENCE, null, text, false));
		}
		return segments;
	}

}
