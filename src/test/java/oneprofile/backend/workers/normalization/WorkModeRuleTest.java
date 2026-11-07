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

	/** A negated mode in an earlier independent clause does not negate a later required office schedule. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"This is not a fully remote role; you will be in the office 3 days a week.",
			"We are not remote, and you will work in the office three days per week.",
			"The position is not remote; colleagues work onsite two days a week.",
			"Our employees are not fully remote, and engineers work in the office four days per week." })
	void keepsSchedulesAfterAnEarlierNegatedMode(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Denying the schedule itself still leaves no required count, including after a denied mode. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You will not be required to be in the office 3 days a week.",
			"Engineers are not expected to work onsite two days per week.",
			"We are not remote, and you will not work in the office three days per week.",
			"This is not a fully remote role; you will not be in the office 3 days a week." })
	void readsNoScheduleFromADeniedOfficeCount(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** People working or spending a partial share, a count floor or named days in an office have a schedule. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Employees work in one of our offices at least 25% of the time.",
			"Our engineers work at the office 40% of their working time.",
			"You will spend 50% of your working time in the office each month.",
			"Colleagues spend 60% of their time in our offices.",
			"Teams choose which 10+ days a month they come in to the office.",
			"Engineers pick which 8+ days per month they work onsite.",
			"You will work in the office three (3) or more days per week.",
			"Colleagues work onsite two (2) or more days a week.",
			"You will work from the office on Monday, Wednesday, and Thursday, with remote work on Tuesday and Friday.",
			"Engineers work in the office on Tuesday and Thursday, with remote work on Monday, Wednesday and Friday." })
	void readsPartialOfficeSharesAndFurtherScheduleShapes(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Office weekdays followed by remote actions or home locations on weekdays state a split schedule. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"In the office on Tuesdays and Thursdays, remote on Mondays and Fridays.",
			"Office on Monday through Wednesday, from home on Thursday and Friday.",
			"Engineers are in the office on Wednesdays, WFH on Tuesdays.",
			"Colleagues work in the office on Mondays, remote work on Thursdays.",
			"In the office on Thursdays, working remotely on Fridays.",
			"Office on Tuesdays, work remotely on Mondays.",
			"Office on Wednesdays, remotely on Thursdays.",
			"In the office on Fridays, from home on Tuesdays." })
	void readsSplitWeekdaysAcrossRemoteActionsAndHomeLocations(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Tools, technical subjects, optional days and incidental events do not declare a split work schedule. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"In the office on Tuesdays and Thursdays, remote access on Mondays and Fridays.",
			"Office on Mondays, from home networks on Fridays.",
			"Office on Wednesdays, WFH equipment on Tuesdays.",
			"Office on Mondays, remote work research on Thursdays.",
			"Some colleagues are in the office on Thursdays, working remotely on Fridays.",
			"Office on Tuesdays, work remotely on Mondays during onboarding.",
			"Office on Wednesdays, remotely on Thursdays for training.",
			"Optional office on Fridays, from home on Tuesdays." })
	void readsNoSplitWeekdaysFromSubjectsOrQualifiedDays(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Shares of unrelated work and optional or denied office schedules do not require office days. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Up to 25% travel.",
			"Employees spend 50% of their working time coding near the office.",
			"Engineers spend 60% of their time reviewing office equipment.",
			"Some employees work in one of our offices 25% of the time.",
			"Many engineers work at the office 40% of their working time.",
			"10+ days a month in the office are optional.",
			"8+ days per month onsite are recommended.",
			"You will not work in the office three (3) or more days per week.",
			"Colleagues will not work onsite two (2) or more days a week.",
			"Office work on Monday and Wednesday is optional.",
			"Office work on Tuesday and Thursday is recommended.",
			"Employees do not spend 100% of their working time in our offices.",
			"100% of the time coding near the office.",
			"Not 100% on-site.",
			"3 days per week in the office are optional." })
	void readsNoScheduleFromNearbyPartialShareOrDayForms(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A full office share is a requirement below schedules and explicit role statements. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "100% of the time in the office",
			"Employees spend 100% of their working time in our offices.", "100% on-site" })
	void readsFullOfficeTimeAsARequirement(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "2 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** A full weekday range leaves no recurring days away. */
	@Test
	void neverReadsAFullWeekdayRangeAsHybrid() {
		assertThat(modesIn("Monday through Friday in the office")).doesNotContain(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Working in office Monday through Thursday and remotely on Fridays.",
			"You will work from home on Fridays and from our office the rest of the week.",
			"Work from home on Fridays." })
	void readsNamedOfficeOrRemoteDaysAsHybrid(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "a minimum of three coordinated days in the office per week",
			"in-office work four (4) days every week", "Hybrid (3 days of work from the office)",
			"(hybrid, 3 days in Palo Alto office)" })
	void readsOfficeDaysWithInterveningWordsOrRepeatedCounts(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Hybrid in-office Tuesday-Thursday but could require occasional in-office support on other days.",
			"An expected in-office presence of 2 days per week, depending on business needs.",
			"2 days required in the office per week, more if you like.",
			"3 days in the office are required; extra days are optional." })
	void keepsARequiredScheduleWhenLaterDaysAreOptional(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Hybrid working arrangement", "Job Schedule: Full-time Hybrid" })
	void readsAShortArrangementOrJobScheduleLabel(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "2 days in the office are recommended but not enforced.",
			"3 days per week in the office are optional." })
	void readsNoModeFromAnOptionalCountOfOfficeDays(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Experience working remotely with distributed teams.",
			"Experience working from home is preferred.", "Our tools let customers work remotely.",
			"You are experienced working remotely with distributed teams." })
	void readsNoModeFromExperienceOrSomeoneElsesRemoteWork(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void takesACompanyHybridStatementOverABasedOffice() {
		assertThat(modesIn("This role is based in our Bristol office.", "We operate as a hybrid workplace."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void keepsEveryModeOfACompanyPolicyByLocationWhenNothingMoreSpecificSpeaks() {
		String remote = "Remote work in locations without our offices";
		String hybrid = "Hybrid work in locations with our offices (2 days in-office, 3 days remote)";
		assertThat(modesIn(remote, hybrid)).containsExactlyInAnyOrder(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID);
		assertThat(modesIn(remote, hybrid, "This role is onsite.")).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(remote, hybrid, "#LI-Hybrid")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(remote + "; this position is onsite in Boston.")).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(remote + " is optional.")).isEmpty();
	}

	@Test
	void readsAJobHeadingWithItsModeInParentheses() {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null,
				"Presales – Lenovo (Hybrid Role)", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes())
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void keepsAHiringSentencesChoiceOfAHybridBasisOrRemote() {
		assertThat(modesIn("Acme is looking for a Designer to join our team in our Culver City studio "
				+ "on a hybrid basis, or remotely within the US."))
			.containsExactlyInAnyOrder(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "We are hiring candidates based in Dublin or Cork for our hybrid working model.",
			"You will be working on a flexible hybrid schedule." })
	void readsAHiringSentencesHybridArrangement(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	/** Permissions and longer role subjects state this vacancy's arrangement above labels and tags. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"This role is a full-time role based in Sunnyvale, CA (hybrid). | HYBRID",
			"We are hiring candidates based in Tokyo for our hybrid working model. | HYBRID",
			"You're welcome to work remotely from Canada. | REMOTE",
			"This Senior Electrical Controls and Automation Engineer position can work out remotely. | REMOTE",
			"Telecommuting is permitted. | REMOTE" })
	void readsRolePermissionsAndLongerSubjects(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(mode);
	}

	/** Invitations without work, denied permissions and experience do not declare an arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This role is a full-time role based in Sunnyvale, CA.",
			"We are hiring candidates based in Tokyo for our flexible working model.",
			"You're welcome to apply.", "You're not welcome to work remotely from Canada.",
			"This Senior Electrical Controls and Automation Engineer position cannot work out remotely.",
			"Telecommuting is not permitted.", "Telecommuting is permitted for some roles.",
			"You're welcome to work remotely if approved.", "Experience working remotely with distributed teams.",
			"Remote work may be permitted for some roles." })
	void readsNoArrangementFromQualifiedPermissionsOrLongerSubjects(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Permissions name the person's remote work, with subjects and verbs independent of place wording. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You are free to work from home anywhere in Germany.",
			"Remote work is allowed for this position.", "Candidates are eligible to telecommute from Austria.",
			"This role is permitted to work remotely from France.", "The position can work remote from Spain.",
			"You may work out remotely from Portugal.", "You're able to work remotely from Belgium.",
			"Are welcome to work from home in Sweden.", "May telecommute from Finland.",
			"Working from home is an option in Denmark.", "Telecommuting is available in Norway.",
			"You are allowed to work remotely from Italy." })
	void readsRemotePermissionsAcrossSubjectsAndVerbs(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Denials, subsets, tools and conditional permissions do not grant this vacancy remote work. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Remote work is not allowed.", "Some roles may work remotely.",
			"Our software lets you work remotely.", "Candidates are not eligible to telecommute from Austria.",
			"Candidates are permitted to work remotely if approved.", "The position cannot work remote from Spain.",
			"You may work out remotely for some projects.", "You're unable to work remotely from Belgium.",
			"Are not welcome to work from home in Sweden.", "May not telecommute from Finland.",
			"Remote work is not an option in Denmark.", "Telecommuting is unavailable in Norway.",
			"You are allowed to work remotely depending on the role." })
	void readsNoRemotePermissionFromNearbyQualifiedForms(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Permissions limited to a subset or requiring approval do not grant this vacancy remote work. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "May work remotely for eligible employees only.",
			"Remote work is available subject to manager approval.",
			"Telecommuting is allowed for approved staff.", "You can work from home for certain candidates.",
			"Remote work is permitted for select roles.", "You may telecommute subject to approval.",
			"Working from home is an option with manager approval.",
			"Candidates are allowed to work remotely for approved employees.",
			"Remote work is available for eligible staff.", "May telecommute for certain roles.",
			"You can work remotely for select candidates." })
	void readsNoRemotePermissionFromSubsetOrApprovalRestrictions(String text) {
		assertThat(modesIn(text)).isEmpty();
		assertThat(modesIn(text, "This role is onsite.")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Eligibility of this person and approval unrelated to their permission leave the grant unhedged. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You are eligible to work remotely.",
			"Candidates are eligible to work from home.", "Remote work is available for this role.",
			"You may telecommute; equipment purchases are subject to approval." })
	void keepsRemotePermissionsWithoutSubsetOrApprovalRestrictions(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "Workplace type: Onsite")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** A permission restricted to recurring remote days keeps the schedule's hybrid reading. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You may work from home on Fridays.",
			"You are free to work from home on Mondays." })
	void readsRecurringRemotePermissionsAsHybrid(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "#LI-Remote")).containsExactly(WorkModeEnum.HYBRID);
	}

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

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Onsite 4 days per week", "4-5 days per week in the office",
			"4 to 5 days a week onsite", "4+ days per week in office" })
	void readsOfficeDaysOfFourOrMoreOutOfFiveAsHybrid(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsFiveDaysAWeekInTheOfficeAsOnsiteInADescription() {
		assertThat(modesIn("5 days a week in the office")).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "70/30 work model", "a 60/40 hybrid model", "60% in office",
			"70% in-office", "50% onsite", "8 days a month in the office", "at least 4 days per month onsite" })
	void readsAShareSplitOrDaysAMonthAsHybrid(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "20 days of paid leave per year", "70% of the time coding",
			"We run a 24/7 support model for our customers" })
	void readsNoModeFromPayOrAShareWithNoOfficeWord(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void keepsAFullShareAtOnsiteRatherThanHybrid() {
		assertThat(modesIn("Our employees are 100% onsite.")).containsExactly(WorkModeEnum.ONSITE);
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
	@CsvSource(delimiter = '|', value = { "Hybrid in Towson, MD or Columbia, MD, or Remote | HYBRID,REMOTE",
			"United States (Hybrid or Remote) | HYBRID,REMOTE",
			"Hybrid work in London, with a desk in our Shoreditch office, or remote in Germany or Spain | HYBRID,REMOTE",
			"Office-Based (Leeds) / Home-Based | ONSITE,REMOTE",
			"Hybrid or Remote depending on your location | HYBRID,REMOTE",
			"Work location is variable with onsite, hybrid, or remote options | ONSITE,HYBRID,REMOTE",
			"This role is on-site in our Fort Worth (or remote in the US) location. | ONSITE,REMOTE",
			"This position will be onsite at the Sunnyvale plant and/or remote work. | ONSITE,REMOTE" })
	void keepsEveryModeAChoiceOffersInTheShapesADescriptionWritesIt(String text, String modes) {
		assertThat(modesIn(text)).containsExactlyInAnyOrderElementsOf(modes(modes));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "On-Site position, no remote or hybrid option | ONSITE",
			"This is a hybrid role, with at least three days per week in the office or onsite with clients. | HYBRID",
			"This role is in the office 3 days a week, choosing to work remotely or in the office the remaining days. | HYBRID",
			"Location: Hybrid (onsite/remote) | HYBRID",
			"Workplace type: Hybrid; Primarily remote with flexibility to come on site as needed | REMOTE",
			"This position is 4 days in office. | HYBRID" })
	void readsOneModeWhereATextNamingSeveralOffersNoChoice(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Ability to work remotely full-time.",
			"Working remotely, while collaborating with global teams.",
			"You can effectively perform all work activities remotely, and will only be required to be on-site "
					+ "on an occasional basis." })
	void keepsASingleModeAnOffersSegmentResolvesToOnceOffersOtherGuardsPass(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
	}

	@Test
	void readsNoModeWhereAnOffersFirstModeIsQualifiedRightAfterIt() {
		assertThat(modesIn("Hybrid as needed, and we are open to exceptional remote talent.")).isEmpty();
	}

	@Test
	void readsNoModeFromAnOffersSingleModeANegationQualifies() {
		assertThat(modesIn("Not able to work remotely full-time.")).isEmpty();
	}

	@Test
	void readsATemplateListingEveryModeByWhatFollowsIt() {
		assertThat(modesIn("Type of work model: Hybrid / Onsite / Remote", "Our Hybrid work model: 3 days in the office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("Type of work model: Hybrid / Onsite / Remote", "We build payments software.")).isEmpty();
		assertThat(modesIn("Workplace type", "Remote / Onsite", "You will be onsite 3-4 days per week."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "The teams are all home based in different countries.",
			"You will work as part of a remote, globally distributed team." })
	void readsTheModeOfTheTeamThisRoleJoins(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
	}

	@Test
	void readsAModeLabelUnderTheJobDetailsHeading() {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, "Job Details", false),
				new Segment(SegmentKindEnum.ITEM, "Job Details", "Full-time / Remote", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(WorkModeEnum.REMOTE);
		List<Segment> benefits = List.of(new Segment(SegmentKindEnum.HEADING, null, "Benefits", false),
				new Segment(SegmentKindEnum.ITEM, "Benefits", "Remote work", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", benefits).modes()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "We offer hybrid or remote working for most roles.",
			"You will collaborate with the onsite team in Austin.", "Remote / Hybrid / Onsite",
			"Remote or hybrid roles enjoy a generous travel budget for team meetups.",
			"We offer flexible work: hybrid or remote, depending on team needs.",
			"Benefits include a home office stipend or hybrid commuter pass for eligible staff." })
	void readsNoChoiceTheVacancyDoesNotOffer(String text) {
		assertThat(modesIn(text)).isEmpty();
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
			"Acme is a remote first company with teams in 12 countries.", "We have no office in Argentina.",
			"We are seeking remote-first leaders to grow our culture." })
	void neverReadsAModeFromAWordThatIsNotAboutThisRole(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You are required to be in the office 2 to 3 times a week.",
			"Visa requires at least 3 days in office.", "Monday to Thursday, work onsite with your colleagues.",
			"There is an on-site requirement of three days per week.", "Onsite 4 days per week",
			"Our employees work from our offices 4 days a week with flexibility to work remotely 1 day.",
			"All colleagues are expected to spend at least three days per week in the office.",
			"We expect team members to work in an office 4+ days per week.",
			"We support a hybrid model with 3 days in office." })
	void readsOfficeDaysAsHybridThoughNoSentenceIsAboutThisRole(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Team members work onsite Tuesdays through Fridays, with the flexibility to work remotely on Mondays, unless there is an approved workplace accommodation.",
			"We expect 3 days onsite." })
	void readsASchedulesExceptionAndDaysWithNoWeekAsHybrid(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoChoiceFromModesListedAsAPerkOrAPlaceholder() {
		List<Segment> perks = List.of(new Segment(SegmentKindEnum.HEADING, null, "We offer", false),
				new Segment(SegmentKindEnum.ITEM, "We offer", "Flexible working format remote, office-based or flexible", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", perks).modes()).isEmpty();
		assertThat(modesIn("Work arrangement [office/remote/hybrid/field]")).isEmpty();
	}

	@Test
	void readsNoChoiceFromModesListedAsAFlexibility() {
		assertThat(modesIn("Flexibility: remote, hybrid, or in-office", "#LI-Hybrid")).containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoChoiceFromModesListedUnderAWhatYouGetHeading() {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, "What you get", false),
				new Segment(SegmentKindEnum.ITEM, "What you get", "Thrive in a Hybrid or Remote Model", false),
				new Segment(SegmentKindEnum.SENTENCE, null, "#LI-Hybrid", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoModeFromAChannelOfTheWork() {
		assertThat(modesIn("Deliver remote support and on-site client engagement")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Most roles spend 3 days a week in the office.",
			"We have recommended office days, 2 days per week.",
			"You will not need to be in the office 3 days a week.",
			"You will provide onsite support to our clinics 3 days a week." })
	void readsNoModeFromOfficeDaysAHedgeOrANegationQualifies(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This role is based in our Edinburgh office. | ONSITE",
			"This role is located at our Schaumburg, IL office. | ONSITE",
			"This position will be based out of the Dallas office. | ONSITE",
			"This is primarily an office-based position. | ONSITE",
			"This role is based in our London office, with two days a week from home. | HYBRID" })
	void readsTheOfficeThisRoleIsBasedInAsItsMode(String sentence, WorkModeEnum mode) {
		assertThat(modesIn(sentence)).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"you will be based in either our Bristol or London office | ONSITE",
			"This internship role is based in either Acme's San Francisco or New York hub offices | ONSITE" })
	void readsAnEitherOfficeOrAPossessiveAsTheOfficeThisRoleIsBasedIn(String sentence, WorkModeEnum mode) {
		assertThat(modesIn(sentence)).containsExactly(mode);
	}

	/** An office base without a full role subject still names where this vacancy works. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Role based in Singapore office, with occasional travel.",
			"US based from our Austin office.", "Position based in our Leeds office.",
			"Job located at our Leeds office.", "Germany based in our Berlin office.",
			"New York based in our Manhattan office.", "London located at our central office.",
			"Based out of our office in Gangnam 5 days/week.",
			"UK based from either our Bristol, Cambridge or London office.",
			"As an engineer in our Tuzla office, you will build tools.",
			"You can be based in our Plano, Reston or Washington DC office.",
			"You may be based in either our Bristol or London office." })
	void readsAnOfficeBaseWithoutAFullRoleSubject(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** A city, company office list, or qualified base does not state this role's office. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Based in London.", "We have offices in 12 cities.",
			"US based from Austin.", "Position based in Leeds.", "Job located at Leeds.",
			"Germany based in Berlin.", "New York based in Manhattan.", "London located centrally.",
			"locally based in our Austin office.", "Vacancies advertised as based in our Austin office.",
			"Some roles may be based in our London office.", "Role based in Singapore, with occasional travel.",
			"Occasionally based out of our office in Gangnam.",
			"UK based from either Bristol, Cambridge or London.",
			"As an engineer in Tuzla, you will build tools.",
			"You can be based in Plano, Reston or Washington DC.",
			"You may occasionally be based in our London office.",
			"You may be based in our London office.", "You can be based in our London office." })
	void readsNoOfficeBaseFromAResidenceOrAQualifiedOffice(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A proper office name needs no determiner and still gives way to a specific arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Role based in Singapore office.",
			"This role is located in New York office." })
	void readsAProperOfficeNameWithoutADeterminer(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** An unrelated office after a city does not name the role's base. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This role is located in New York, with offices worldwide.",
			"Based in London and close to our partners' offices.",
			"Role based in singapore office.", "This role is located in London With Offices worldwide.",
			"Based in London And Paris offices." })
	void readsNoOfficeBaseAcrossUnrelatedWordsWithoutADeterminer(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A bare role base gives way to office days in another segment. */
	@Test
	void takesAScheduleOverABareRoleBase() {
		assertThat(modesIn("Role based in our Singapore office.", "3 days a week in the office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("Role based in our Singapore office.", "We follow a hybrid work model."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoModeFromBeingBasedInACityWithNoOffice() {
		assertThat(modesIn("This role is based in London.")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"This position is located onsite at the customer's facility in Dayton, OH.",
			"Daily onsite presence at our San Jose, CA office is required.",
			"Required ability to work onsite in Irving, TX." })
	void readsOnsiteStatedAsLocatedOrADailyPresence(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Provide on-site technical support to clinicians.",
			"a team that provides onsite support to users and customers", "This role provides onsite support to our clinics." })
	void readsAJobWhoseOwnWorkIsSupportAtASiteAsOnsite(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Longer support objects and an onsite technical owner state the candidate's own work. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Provide on-site cybersecurity and RMF sustainment support.",
			"Provides on-site network and server administration support.",
			"Deliver onsite hardware troubleshooting support.", "Perform onsite Level 2 support.",
			"Serve as the on-site technical owner for our Austin office." })
	void readsLongerOnsiteSupportDuties(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Serving in an onsite role names the work arrangement even without a following office. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Serve as an on-site IT lead for the plant.",
			"Serve as the onsite owner.", "Serve as an onsite senior systems engineer.",
			"Serve as the onsite IT operations team manager.", "Serve as an onsite technician.",
			"Serve as the onsite representative.", "Serve as an onsite point of contact.",
			"Serve as the onsite technical point of contact for customers." })
	void readsServingInAnOnsiteRole(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** An onsite role for events or occasional visits does not state the vacancy's arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Serve as the on-site lead for quarterly events.",
			"Serve as the onsite owner during training.", "Serve as an onsite engineer as needed.",
			"Serve as the onsite IT operations team manager occasionally.",
			"Serve as an onsite technician for onboarding.", "Serve as the onsite representative for events.",
			"Serve as an onsite point of contact when needed.",
			"Serve as the onsite technical point of contact for orientation." })
	void readsNoArrangementFromServingInAnIncidentalOnsiteRole(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Channels, incidental support and ownership without a work arrangement name no mode. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Provide onsite or remote support to users.",
			"Provide onsite network and server administration escalation support.",
			"Deliver onsite phone support.", "Perform onsite email support.",
			"Provide onsite virtual support.", "Provide onsite remotely delivered support.",
			"Provide onsite support as needed.", "Provide onsite occasional support.",
			"Provide onsite as requested support.", "Provide onsite needed support.",
			"Support onsite events as needed.",
			"Provide on-site cybersecurity and RMF sustainment support occasionally.",
			"Provide onsite cybersecurity or remote support to users.",
			"Serve as the on-site technical owner for our Austin office as needed.",
			"Serve as the technical owner for our Austin office." })
	void readsNoModeFromLongerSupportDutiesThatDoNotStateAnArrangement(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "after-hours support onsite or remote",
			"support onsite events as needed" })
	void readsNoModeFromSupportThatIsNotThisJobsOwnWork(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This is an in-person opportunity in London. | ONSITE",
			"All full-time roles are in person in New York. | ONSITE" })
	void readsInPersonAsAnOnsiteWord(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
	}

	@Test
	void readsNoModeFromInPersonEvents() {
		assertThat(modesIn("We host in-person events quarterly.")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This is a 100% remote role.", "This role is fully remote." })
	void readsARoleStatedAsFullyOrAPercentRemoteAsRemote(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This role is primarily remote, with occasional onsite visits.",
			"Fully remote with quarterly visits to our Austin office." })
	void readsRemoteWithVisitsNowAndThenAsRemote(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"You can be based in our Austin office or work remotely.",
			"This role can be based in our Paris office or fully remote.",
			"can be based out of our San Francisco office or remotely in the USA region",
			"You can cooperate 100% remotely or choose one of our office spaces in Warsaw or Cracow.",
			"Remote or office, you choose.", "You will work out of our Chicago office or remotely." })
	void keepsAChoiceOfAnOfficeOrRemoteAsBoth(String text) {
		assertThat(modesIn(text)).containsExactlyInAnyOrder(WorkModeEnum.REMOTE, WorkModeEnum.ONSITE);
	}

	@Test
	void readsOnsiteInNamedOfficesAsOnsiteAlone() {
		assertThat(modesIn("onsite in our Austin or Dallas office")).containsExactly(WorkModeEnum.ONSITE);
	}

	@Test
	void givesWayToAScheduleFromAnOfficeThisRoleIsBasedInAlone() {
		assertThat(modesIn("This role is based out of our San Francisco office.",
				"Due to our hybrid work model, we expect you to come to the office at least three days per week."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void takesAHybridLabelOrTagOverAnOfficeThisRoleIsBasedInAlone() {
		assertThat(modesIn("Workplace type: Hybrid", "This role is based in our Austin office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("#LI-Hybrid", "This role is based in our Austin office."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Remote Type: Office | ONSITE", "Working Conditions: 100% on-site | ONSITE",
			"LOCATION/WORK MODEL: Full-time/On-site | ONSITE", "LOCATION DETAILS: Onsite Austin | ONSITE" })
	void readsTheJobDetailsLabelsADescriptionWritesOnsiteIn(String label, WorkModeEnum mode) {
		assertThat(modesIn(label)).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"The selected candidate will be required to work on-site, full-time at our Melbourne, FL location.",
			"Ability to work onsite due to the nature of the classified systems.",
			"Applicants will be considered only if they are willing to work on-site.",
			"Engineers must work onsite at the facility.", "This will be an on-site role.",
			"The work requires 100% onsite work." })
	void readsOnsiteStatedAsARequirement(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Intervening requirement words and punctuation still state onsite below schedules and role arrangements. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "This role requires that you work on-site daily.",
			"You will be required to work full-time, on-site at our facility.",
			"Work is exclusively onsite at a client location." })
	void readsOnsiteRequirementsWithInterveningWords(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Work stated as done onsite is a requirement below schedules and explicit role arrangements. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "All work is performed entirely onsite.",
			"Work is done onsite.", "Work is performed on-site.", "Work is onsite.",
			"Work is done exclusively on-site.", "Work is performed fully on-site.",
			"Work is done 100% onsite.", "This role's work is done onsite." })
	void readsWorkStatedAsDoneOnsite(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "3 days a week in the office.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** A subset of work, a negation or an incidental occasion does not require onsite work. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Some work is performed onsite.",
			"Some work is done onsite.", "Some work is onsite.", "Work is not performed onsite.",
			"Work is done onsite as needed.", "Work is performed onsite occasionally.",
			"Work is done 100% onsite during training.", "This role's work is not done onsite." })
	void readsNoRequirementFromQualifiedWorkDoneOnsite(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Nearby negations, possibilities and incidental visits do not become requirements. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You are not required to work on-site.",
			"This role requires that you work on-site occasionally.",
			"You will not be required to work full-time, on-site at our facility.",
			"Work is not exclusively onsite at a client location." })
	void readsNoOnsiteRequirementFromInterveningWordsWithAQualifier(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You may be required to work onsite occasionally.",
			"You will not be required to work onsite.", "Ability to work onsite as needed.",
			"You are expected to be in the office regularly.",
			"Applicants should expect that the role will require the ability to attend in-person meetings." })
	void readsNoModeFromAnOnsiteRequirementAHedgeQualifies(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"We are transitioning to a full-time onsite working model through a phased rollout.",
			"Some roles and locations require 100% onsite presence, while others require less.",
			"This role requires working with both onsite and offshore team members." })
	void readsNoModeFromAChangeUnderwayOrColleaguesLocation(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You are excited to work in our New York office.",
			"You will work out of our Chicago office.", "Join our team at Acme's Sunnyvale office.",
			"Acme is hiring a Software Engineer to join the platform group in our Beavercreek Ohio Office." })
	void readsTheOfficeTheWorkIsInAsOnsite(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "You will work in an office environment.",
			"You may occasionally work in our New York office." })
	void readsNoModeFromAnOfficeTheWorkIsInThatIsNotItsPlace(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void takesALabelOverTheOfficeTheWorkIsInAndThatOfficeOverATag() {
		assertThat(modesIn("Workplace type: Hybrid", "You will work out of our Chicago office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("#LI-Remote", "You will work out of our Chicago office.")).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Requires 5 days/week in-office collaboration.",
			"You will be onsite at the client location five days per week.", "We work 5 days a week in the office for all roles.",
			"All positions at Acme are full-time on-site.", "Our employees work fully from the office.",
			"Primarily based in the Innovation Hub, with flexibility to work remotely as required.",
			"Majority of tasking is performed on-site." })
	void readsEveryWorkingDayInTheOfficeAsOnsite(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Willing to travel on site/test bed for commissioning.",
			"The engineer travels to customer sites for on-site implementations.",
			"Travels in personal car to computer sites throughout the County." })
	void readsAJobWhoseWorkIsAtSitesAsOnsite(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Occasional travel to customer sites.",
			"Travel up to 25% to customer sites.", "Most roles are in the office 5 days a week.",
			"You will not be onsite 5 days a week." })
	void readsNoModeFromSiteTravelOrAFullWeekAHedgeQualifies(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void takesAHybridScheduleOverAFullWeekFloor() {
		assertThat(modesIn("Our employees work fully from the office.", "This team is in the office 3 days a week."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void takesARemoteTagOverSiteTravel() {
		assertThat(modesIn("#LI-Remote", "Occasionally travels to customer sites for on-site implementations."))
			.containsExactly(WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Mostly remote, primarily based in the US.", "Primarily based in Austin, TX.",
			"Some travel to customer sites.", "Limited travel to client sites.", "Willing to travel to customer sites.",
			"You are expected to be in the office for team events.", "Willing to work in-office during training.",
			"Attend 3 days in-person onboarding training.", "Hybrid: No", "Onsite: Not required",
			"Our employees working in our offices worldwide enjoy free lunch.", "You will work in the front office.",
			"You will work in the Program Management Office.", "Location: New York; this is not a remote role" })
	void readsNoModeFromTextThatOnlyResemblesAnOnsiteOrALabel(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({ "Remote-first culture", "Remote: why we work this way", "Hybrid (and why we love it)" })
	void readsNoLabelFromAHeadingOpeningWithAModeAndATheme(String heading) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.SENTENCE, heading, "We build payments software.", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).isEmpty();
	}

	@Test
	void takesAHybridLabelOrTagOverAnOnsiteRequirementOrFloor() {
		assertThat(modesIn("Workplace type: Hybrid", "Must be able to work onsite.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("#LI-Hybrid", "Must be able to work onsite.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("Workplace type: Hybrid", "Our employees work fully from the office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("Workplace type: Remote", "Must work onsite.")).containsExactly(WorkModeEnum.ONSITE);
	}

	@Test
	void takesOfficeDaysOverAnOnsiteRequirement() {
		assertThat(modesIn("Must be able to work onsite.", "Team members are in the office 3 days a week."))
			.containsExactly(WorkModeEnum.HYBRID);
	}

	@Test
	void readsNoModeFromRemoteRuledOutAlone() {
		assertThat(modesIn("This position does not offer any virtual or telecommute working options.")).isEmpty();
		assertThat(modesIn("Must work onsite.", "This position does not offer any virtual or telecommute working options."))
			.containsExactly(WorkModeEnum.ONSITE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = {
			"Location: London (Hybrid) | Practice Area: Corporate | Type: Permanent; HYBRID",
			"Location: Philomath, Oregon (Onsite); ONSITE", "Location: London, United Kingdom - Hybrid; HYBRID",
			"London / Hybrid; HYBRID", "Job Location (Hybrid); HYBRID", "Onsite: Eglin AFB; ONSITE",
			"Employment Type: Remote, 6 months contract with extension; REMOTE",
			"Employment Type: Full-time, hybrid work arrangement; HYBRID",
			"Work Environment: Collaborative, hybrid work arrangement; HYBRID",
			"Logistics / Location: Dayton, OH, In Office; ONSITE" })
	void readsALabelWhoseValueNamesAPlaceBeforeTheMode(String label, WorkModeEnum mode) {
		assertThat(modesIn(label)).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Remote: If this position is listed as remote, there may be occasions in person.",
			"Customer support across remote and onsite channels", "Employment Type: Full-time" })
	void readsNoLabelFromASegmentThatOnlyMentionsAMode(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = {
			"Hybrid 3 days (office locations are New York City, Denver, and Charlotte); HYBRID",
			"Hybrid — 3 days onsite; HYBRID", "Remote - United States; REMOTE" })
	void readsAHeadingNamingTheModeWithAScheduleOrAPlaceAsALabel(String heading, WorkModeEnum mode) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.SENTENCE, heading, "We build payments software.", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = { "Remote Type -; Hybrid; HYBRID", "Remote Type -; Office; ONSITE",
			"Remote Work; Onsite; ONSITE", "Hybrid Working; Onsite; ONSITE" })
	void readsAHeadingThatIsALabelAloneByItsNextSegment(String heading, String value, WorkModeEnum mode) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.ITEM, heading, value, false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource({ "US Virtual Location", "Working at Home - Pennsylvania" })
	void readsTheLocationsWordsForWorkingFromHomeAsRemote(String location) {
		assertThat(read(location, null, "Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.LOCATION));
	}

	@Test
	void readsNoModeFromVirtualInATitle() {
		assertThat(read(null, null, "Virtual Reality Engineer")).isEqualTo(WorkModeReading.NONE);
	}

	@Test
	void readsNoModeFromThePhysicalSurroundingsOfTheWork() {
		assertThat(modesIn("Environmental Conditions: Office")).isEmpty();
		assertThat(modesIn("Location: Office in a busy downtown building")).isEmpty();
	}

	@Test
	void readsAWorkdayLocationOpeningWithItsModeAsALabel() {
		assertThat(read("Office - United Kingdom - Edinburgh", null, "Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.ONSITE), WorkModeEvidenceEnum.LOCATION));
		assertThat(read("Home - UK- England", null, "Engineer"))
			.isEqualTo(new WorkModeReading(Set.of(WorkModeEnum.REMOTE), WorkModeEvidenceEnum.LOCATION));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"This globally distributed, fully remote team of experienced engineers develops our payments platform.",
			"Payments is a fully remote software team within Acme.", "Our team is fully remote.",
			"This role and its team are fully remote.", "Acme is looking for a remote Back End Software Engineer.",
			"Open to remote locations.",
			"Open to remote work for candidates outside a reasonable commuting distance.", "Speechify has no office." })
	void readsRemoteStatedForThisRolesTeamOrOfferedWithNoSubject(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Invenergy follows a hybrid schedule. | HYBRID",
			"We operate as a hybrid workplace. | HYBRID", "We follow a hybrid work model. | HYBRID",
			"we have a hybrid working model | HYBRID", "Acme embraces a hybrid model. | HYBRID",
			"All positions are onsite. | ONSITE", "All of our roles are in-office. | ONSITE",
			"We are an in-office company. | ONSITE", "We are a fully remote company. | REMOTE",
			"All our roles are remote. | REMOTE", "Acme is a remote company. | REMOTE" })
	void readsACompanysUnhedgedStatementOfItsModelWithNoDaysAsALowTier(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
	}

	/** Company arrangements stay below every statement and label about this role. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Acme practices a hybrid model. | HYBRID",
			"A hybrid work model with a mix of in-office and remote days. | HYBRID",
			"We're a team of 800+ employees, working remotely across 28 countries. | REMOTE",
			"All of our roles are remote, however some roles may carry location-based eligibility requirements. | REMOTE" })
	void readsFurtherCompanyArrangementsBelowRoleStatements(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is fully remote.")).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** A mode directly qualifies a work model noun; its parts need not use a fixed description. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Our hybrid working approach combines office days with time at home. | HYBRID",
			"An onsite arrangement with everyone at headquarters. | ONSITE",
			"A remote setup combining home working and annual gatherings. | REMOTE",
			"Our hybrid work model that combines collaboration at headquarters and home working. | HYBRID",
			"A hybrid model of office attendance and home working. | HYBRID",
			"Our remote working arrangement. | REMOTE",
			"An in-office work approach with desks for everyone. | ONSITE" })
	void readsCompanyModelsAcrossNounsAndParts(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is hybrid.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** A model's trailing description must name working places or time rather than a technical subject. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "A hybrid approach with legacy mainframe systems.",
			"Our remote model of data collection.", "An onsite setup combining database replicas and caches.",
			"Our hybrid arrangement that combines batch processing and streaming." })
	void readsNoCompanyModelFromUnrelatedParts(String text) {
		assertThat(modesIn(text)).isEmpty();
		assertThat(modesIn(text, "This role is remote.")).containsExactly(WorkModeEnum.REMOTE);
	}

	/** Working places and time identify the parts of an arrangement across model nouns and clause verbs. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "A hybrid setup with in-person days and remote days. | HYBRID",
			"Our remote approach of home working. | REMOTE",
			"An onsite model combining on-site work and office time. | ONSITE" })
	void readsCompanyModelsWithWorkingArrangementParts(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is hybrid.")).containsExactly(WorkModeEnum.HYBRID);
	}

	/** Technology modifiers, hedges and perks do not state the company's work model. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "A hybrid cloud model with on-prem and remote nodes.",
			"An onsite arrangement with some teams at headquarters.",
			"A remote setup combining home working and optional office attendance.",
			"Our hybrid work model that combines office and home work depending on the role.",
			"A hybrid deployment model of local and remote nodes.", "Our flexible remote working arrangement.",
			"An in-office work approach with phased rollout." })
	void readsNoCompanyModelFromNearbyQualifiedForms(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A work model listed as a benefit remains a perk under the benefits heading. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Perks: a hybrid work model.",
			"Our hybrid working approach combines office days with time at home.",
			"A remote setup with home working." })
	void readsNoCompanyModelFromBenefits(String text) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, "Benefits", false),
				new Segment(SegmentKindEnum.ITEM, "Benefits", text, false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).isEmpty();
	}

	/** Company and team subjects state their people's remote work without fixing headcounts or geography. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Our 300 people work remotely from 20 countries.",
			"We work remotely across Europe.", "We're fully remote in Canada.",
			"Our team work remotely from Ireland.", "Our people are distributed and remote across Asia.",
			"Our employees are remote.", "The team is fully remote in Australia.",
			"We are distributed and remote.", "Our 40 employees working remotely in Brazil.",
			"We're a team of 120 people, working remotely from six continents." })
	void readsCompanyRemotePeopleAcrossSubjectsAndPlaces(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "This role is hybrid.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Subsets, customers, negations and preferences do not declare how all company people work. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Some of our team work remotely.", "We help teams work remotely.",
			"Our 300 people may work remotely from 20 countries.", "We sometimes work remotely across Europe.",
			"We're remote-first in Canada.", "Our team do not work remotely from Ireland.",
			"Our people are distributed and remote for some roles.", "Our employees are not remote.",
			"The team may be fully remote in Australia.", "Our 40 employees working remotely in Brazil occasionally.",
			"We're a team of 120 people, most working remotely from six continents." })
	void readsNoCompanyRemotePeopleFromNearbyQualifiedForms(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A hedge in a different clause qualifies eligibility, place or extra days rather than the mode. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"All of our roles are remote; some roles may carry location-based eligibility requirements.",
			"All of our roles are remote, but some roles may carry location-based eligibility requirements.",
			"All of our roles are remote, although some roles may carry location-based eligibility requirements.",
			"All of our roles are remote, while some roles may carry location-based eligibility requirements.",
			"All of our roles are remote, though some roles may carry location-based eligibility requirements.",
			"All of our roles are remote. Some roles may carry location-based eligibility requirements.",
			"Most roles have location requirements; all of our roles are remote.",
			"All of our roles are remote, however most employees must live in Canada.",
			"All of our roles are remote, but employees may visit the office for extra days." })
	void keepsCompanyModesWhenAnotherClauseHasAHedge(String text) {
		assertThat(modesIn(text)).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "This role is hybrid.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn(text, "#LI-Onsite")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Subsets, preferences, culture and perks do not state a company arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Most of our roles are hybrid.", "We're a remote-first company.",
			"Our culture pioneered hybrid work.", "We offer a flexible hybrid model.",
			"Some of our roles are remote, but this one may differ.", "Acme may practice a hybrid model.",
			"Acme practices a hybrid model for some roles.",
			"A flexible working model with a mix of in-office and remote days.",
			"We're a team of 800+ employees, most working remotely across 28 countries.",
			"All of our roles may be remote, however some roles carry location-based eligibility requirements.",
			"All of our roles are remote for some locations, however eligibility requirements apply." })
	void readsNoCompanyArrangementFromNearbyHedgedForms(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@Test
	void takesATagOverACompanysUnhedgedStatementOfItsModel() {
		assertThat(modesIn("We follow a hybrid work model.")).containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("We follow a hybrid work model.", "#LI-Remote")).containsExactly(WorkModeEnum.REMOTE);
	}

	@ParameterizedTest
	@CsvSource({ "Most of our teams follow a hybrid model.", "We are a remote-first company.",
			"We're a remote-first company." })
	void readsNoCompanyStatementAHedgeQualifies(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = { "Remote; REMOTE", "Consultant | Remote; REMOTE", "Hybrid; HYBRID" })
	void readsAHeadingNamingOnlyTheModeAsALabel(String heading, WorkModeEnum mode) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.SENTENCE, heading, "We build payments software.", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(mode);
	}

	@ParameterizedTest
	@CsvSource({ "Innovate Remotely", "Why remote work at Acme" })
	void readsNoLabelFromAHeadingWithAThemeInIt(String heading) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.SENTENCE, heading, "We build payments software.", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Working Conditions | Remote professional work environment.",
			"Pay Range | Remote" })
	void readsAModeUnderAWorkingConditionsOrAPayHeading(String heading, String text) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.ITEM, heading, text, false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(WorkModeEnum.REMOTE);
	}

	@Test
	void takesOfficeDaysOverALabelOrATag() {
		assertThat(modesIn("Workplace type: Onsite", "Team members work three to four days per week in the office."))
			.containsExactly(WorkModeEnum.HYBRID);
		assertThat(modesIn("#LI-Remote", "We are in the office 3 days a week.")).containsExactly(WorkModeEnum.HYBRID);
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

	/** A work-mode field after other bar-separated fields remains a label of this role. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"'Location: McLean, Virginia, USA | Work Model: Onsite at HQ' | ONSITE",
			"'Location: London | Type: Permanent | Work Arrangement: Hybrid' | HYBRID",
			"'Department: Engineering | Work Style: Remote in Canada' | REMOTE",
			"'Contract: Permanent | Location: York (Hybrid)' | HYBRID" })
	void readsLabelsAfterOtherFields(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is onsite.")).containsExactly(WorkModeEnum.ONSITE);
		assertThat(modesIn(text, "We work in the office 2 days a week.")).containsExactly(WorkModeEnum.HYBRID);
	}

	/** Setup, assignment and mode heads label arrangements rather than equipment or communication. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Work Setup: Onsite (Mandatory) | ONSITE",
			"Work Location Assignment: Hybrid | HYBRID", "Mode: Remote | REMOTE",
			"Working Setup: Hybrid in France | HYBRID", "Work Location Assignment: Remote in Spain | REMOTE",
			"Mode: On-site at headquarters | ONSITE" })
	void readsSetupAssignmentAndModeLabels(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is remote.")).containsExactly(WorkModeEnum.REMOTE);
		assertThat(modesIn(text, "We work in the office 2 days a week.")).containsExactly(WorkModeEnum.HYBRID);
	}

	/** Field heads and values must both name a work arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Mode of communication: email",
			"Work Setup: laptop and monitor provided", "Working Setup: remote access tools provided",
			"Work Location Assignment: Hybrid Cloud Lab", "Mode: remote sensing research",
			"Work Location Assignment: Paris" })
	void readsNoModeFromSetupEquipmentOrOtherAssignments(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** A terminal mode clause labels even a location whose full value exceeds the short-value limit. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Location: London, United Kingdom, Greater London area near Kings Cross station - Hybrid | HYBRID",
			"Job Location: Manchester, United Kingdom, central district beside the main railway station (Hybrid) | HYBRID",
			"Location: Lyon, France, metropolitan area near the northern regional transport hub / Remote | REMOTE" })
	void readsAModeClauseAfterALongPlace(String text, WorkModeEnum mode) {
		assertThat(modesIn(text)).containsExactly(mode);
		assertThat(modesIn(text, "This role is onsite.")).containsExactly(WorkModeEnum.ONSITE);
	}

	/** A long location's technical subject is never a mode clause. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Location: Dallas, Texas, United States, working on our hybrid cloud platform team",
			"Job Location: Manchester, United Kingdom, central district beside the main railway station (Hybrid Cloud Lab)",
			"Location: Lyon, France, metropolitan area near the northern regional transport hub / Remote Sensing Lab" })
	void readsNoModeFromSubjectsAfterALongPlace(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	/** Work and working headings name their mode unless the next segment supplies its value. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "REMOTE WORKING | We hire across Europe. | REMOTE",
			"Remote Working | We hire across Europe. | REMOTE", "Hybrid Working | We develop medical software. | HYBRID",
			"Remote Work | We build financial tools. | REMOTE", "Hybrid Work | Contact our recruitment team. | HYBRID",
			"Remote Working | Onsite | ONSITE", "Hybrid Working | Remote | REMOTE",
			"REMOTE WORKING | Onsite 2 days a week | HYBRID" })
	void readsWorkHeadingsByTheirValueOrTheirOwnMode(String heading, String value, WorkModeEnum mode) {
		List<Segment> segments = new ArrayList<>(List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.ITEM, heading, value, false)));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(mode);
		segments.addAll(described("This role is onsite."));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).containsExactly(WorkModeEnum.ONSITE);
	}

	/** Themes, type labels without a value and non-heading fragments declare no arrangement. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "Why remote work at Acme", "Why hybrid working matters",
			"Remote Type", "Remote Type -", "Remote working tools", "Hybrid work culture",
			"Remote working benefits", "Hybrid working technology" })
	void readsNoModeFromWorkThemesOrUnansweredTypeHeadings(String heading) {
		List<Segment> segments = List.of(new Segment(SegmentKindEnum.HEADING, null, heading, false),
				new Segment(SegmentKindEnum.ITEM, heading, "See below", false));
		assertThat(this.rule.read("Austin, TX", null, "Engineer", segments).modes()).isEmpty();
		assertThat(modesIn(heading, "See below")).isEmpty();
	}

	/** Other fields and subject words in a location do not declare a work mode. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "'Location: Remote Sensing Lab | Type: Permanent'",
			"'Department: Engineering | Location: Hybrid Cloud Lab | Type: Permanent'",
			"'Contract: Permanent | Work Style: hybrid cloud research'",
			"'Location: York | Team: Remote Sensing'" })
	void readsNoModeFromOtherFieldsOrLocationSubjects(String text) {
		assertThat(modesIn(text)).isEmpty();
	}

	private WorkModeReading read(String location, String field, String title) {
		return this.rule.read(location, field, title, List.of());
	}

	private Set<WorkModeEnum> modesIn(String... description) {
		return this.rule.read("Austin, TX", null, "Engineer", described(description)).modes();
	}

	private static List<WorkModeEnum> modes(String names) {
		return List.of(names.split(",")).stream().map(WorkModeEnum::valueOf).toList();
	}

	private static List<Segment> described(String... texts) {
		List<Segment> segments = new ArrayList<>();
		for (String text : texts) {
			segments.add(new Segment(SegmentKindEnum.SENTENCE, null, text, false));
		}
		return segments;
	}

}
