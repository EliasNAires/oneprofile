package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.ManagementLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.TitleBreakdown;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.junit.jupiter.api.Test;

class TitleBreakdownRuleTest {

	private final TitleBreakdownRule rule = new TitleBreakdownRule(new TitleCleaningRule(),
			TaxonomyStore.read(new StringReader("""
					java\tJava\tlanguage\t\t\t
					android\tAndroid\tplatform\t\t\t
					react-native\tReact Native\tframework\t\t\t
					react\tReact\tframework\t\t\t
					""")));

	@Test
	void movesAKindOfWorkAfterTheHeadInFrontOfIt() {
		assertThat(normalized("Software Engineer, Backend")).containsExactly("backend engineer");
	}

	@Test
	void dropsWhatFollowsTheHeadWhenItIsATeam() {
		assertThat(normalized("Platform Engineer, Payments")).containsExactly("platform engineer");
	}

	@Test
	void keepsTheKindOfWorkInFrontWhenTheFrontAlreadyNamesOne() {
		assertThat(normalized("Data Engineer, Platform")).containsExactly("data engineer");
	}

	@Test
	void movesOnlyTheFirstRunOfKindsFromBehindTheHead() {
		assertThat(normalized("Team Lead, Android Core Product")).containsExactly("android team lead");
	}

	@Test
	void readsAnInvertedTitleByMovingItsKindInFront() {
		assertThat(normalized("Engineer, Software")).containsExactly("software engineer");
		assertThat(normalized("Engineer II, Software Development")).containsExactly("software development engineer");
	}

	@Test
	void movesTheTestKindInFrontOfTheHead() {
		assertThat(normalized("Software Engineer in Test")).containsExactly("test engineer");
	}

	@Test
	void takesTheGuardedSeniorityWordsOutAndRecordsThem() {
		assertThat(breakDown("Staff Software Engineer II")).isEqualTo(new TitleBreakdown(List.of("software engineer"),
				Set.of(SeniorityLevelEnum.STAFF, SeniorityLevelEnum.II), null));
		assertThat(breakDown("Mid-Level Data Analyst").titleSeniorities()).containsExactly(SeniorityLevelEnum.MID);
		assertThat(breakDown("Entry Level QA Engineer").titleSeniorities()).containsExactly(SeniorityLevelEnum.ENTRY);
		assertThat(breakDown("Network Engineer III").titleSeniorities()).containsExactly(SeniorityLevelEnum.III);
		assertThat(breakDown("Systems Engineer IV").titleSeniorities()).containsExactly(SeniorityLevelEnum.IV);
	}

	@Test
	void keepsTheGuardOfChiefOfStaff() {
		assertThat(breakDown("Chief of Staff, Engineering"))
			.isEqualTo(new TitleBreakdown(List.of("chief of staff"), Set.of(), null));
	}

	@Test
	void letsAYieldingHeadGiveWayToTheHeadBehindIt() {
		assertThat(breakDown("Lead Analytics Engineer"))
			.isEqualTo(new TitleBreakdown(List.of("analytics engineer"), Set.of(), ManagementLevelEnum.LEAD));
		assertThat(normalized("Software Engineering Intern")).containsExactly("software engineering");
	}

	@Test
	void keepsAHeadOfMoreThanOneWordWhole() {
		assertThat(normalized("Member of Technical Staff, Security")).containsExactly("security member of technical staff");
		assertThat(breakDown("Tech Lead"))
			.isEqualTo(new TitleBreakdown(List.of("tech lead"), Set.of(), ManagementLevelEnum.LEAD));
	}

	@Test
	void takesALeadershipWordOutAsTheManagementLevelAndKeepsTheAreaItLeads() {
		assertThat(breakDown("Engineering Manager, Backend"))
			.isEqualTo(new TitleBreakdown(List.of("backend engineering"), Set.of(), ManagementLevelEnum.MANAGER));
		assertThat(breakDown("Chief Technology Officer"))
			.isEqualTo(new TitleBreakdown(List.of("technology"), Set.of(), ManagementLevelEnum.EXECUTIVE));
		assertThat(breakDown("VP Engineering"))
			.isEqualTo(new TitleBreakdown(List.of("engineering"), Set.of(), ManagementLevelEnum.EXECUTIVE));
	}

	@Test
	void readsALeadersAreaAfterTheLeadershipWord() {
		assertThat(breakDown("Director of Cybersecurity"))
			.isEqualTo(new TitleBreakdown(List.of("cybersecurity"), Set.of(), ManagementLevelEnum.DIRECTOR));
		assertThat(breakDown("Head of Security"))
			.isEqualTo(new TitleBreakdown(List.of("security"), Set.of(), ManagementLevelEnum.DIRECTOR));
		assertThat(breakDown("Senior Manager, Data Applications"))
			.isEqualTo(new TitleBreakdown(List.of("data applications"), Set.of(), ManagementLevelEnum.MANAGER));
		assertThat(breakDown("Digital Transformation - Senior Manager"))
			.isEqualTo(new TitleBreakdown(List.of("digital transformation"), Set.of(), ManagementLevelEnum.MANAGER));
	}

	@Test
	void leavesABareLeaderAManagementLevelAndNoNormalizedTitle() {
		assertThat(breakDown("Manager")).isEqualTo(new TitleBreakdown(List.of(), Set.of(), ManagementLevelEnum.MANAGER));
	}

	@Test
	void keepsTheRankWordOfATitleThatNamesAJob() {
		assertThat(breakDown("Senior Product Manager"))
			.isEqualTo(new TitleBreakdown(List.of("product manager"), Set.of(), null));
		assertThat(normalized("Technical Program Manager")).containsExactly("technical program manager");
		assertThat(normalized("Account Manager")).containsExactly("account manager");
	}

	@Test
	void takesACorporateGradeOutWithoutReadingItAsAManagementLevel() {
		assertThat(breakDown("VP System Analyst")).isEqualTo(new TitleBreakdown(List.of("system analyst"), Set.of(), null));
		assertThat(breakDown("Analyst, Vice President")).isEqualTo(new TitleBreakdown(List.of("analyst"), Set.of(), null));
		assertThat(breakDown("Associate Software Engineer"))
			.isEqualTo(new TitleBreakdown(List.of("software engineer"), Set.of(), null));
	}

	@Test
	void readsTheWorkABareAssociateDoesFromTheRestOfTheTitle() {
		assertThat(breakDown("IT Security, Associate")).isEqualTo(new TitleBreakdown(List.of("it security"), Set.of(), null));
	}

	@Test
	void takesASkillOutOfTheFrontOfTheHead() {
		assertThat(normalized("Java Developer", "java")).containsExactly("developer");
	}

	@Test
	void keepsASkillThatIsTheKindOfWork() {
		assertThat(normalized("Senior Android Engineer", "android")).containsExactly("android engineer");
		assertThat(normalized("React Native Engineer", "react-native")).containsExactly("react native engineer");
	}

	@Test
	void keepsBothHeadsOfATitleThatNamesTwo() {
		assertThat(normalized("Data Scientist / Machine Learning Engineer"))
			.containsExactly("data scientist", "machine learning engineer");
		assertThat(normalized("Data Scientist/Engineer")).containsExactly("data scientist", "data engineer");
	}

	@Test
	void readsTwoAdjacentHeadsAsOneCompoundHead() {
		assertThat(normalized("Programmer Analyst II")).containsExactly("programmer");
	}

	@Test
	void readsAKindOfWorkWrittenAcrossASlashAsOne() {
		assertThat(normalized("AI/ML Engineer")).containsExactly("ai ml engineer");
	}

	@Test
	void takesOutWorkModesAndPlacesTheLocationFieldNamesOrNot() {
		assertThat(this.rule.breakDown("Remote Backend Developer - Remote, Latin America", "Bogotá", Set.of())
			.normalizedTitles()).containsExactly("backend developer");
		assertThat(this.rule.breakDown("Bogota Data Engineer", "Bogota, Colombia", Set.of()).normalizedTitles())
			.containsExactly("data engineer");
	}

	@Test
	void takesOutRequisitionCodesCohortYearsAndYearsOfExperience() {
		assertThat(normalized("Product Owner - R01572426")).containsExactly("product owner");
		assertThat(normalized("[Job-31906] Senior Data Developer")).containsExactly("data developer");
		assertThat(normalized("Summer 2027 Security Engineer")).containsExactly("security engineer");
		assertThat(normalized("10+ years Embedded Engineer")).containsExactly("embedded engineer");
		assertThat(normalized("Founding Engineer")).containsExactly("engineer");
		assertThat(normalized("Night Shift Network Engineer")).containsExactly("network engineer");
	}

	private List<String> normalized(String title, String... titleSkillIds) {
		return this.rule.breakDown(title, "", Set.of(titleSkillIds)).normalizedTitles();
	}

	private TitleBreakdown breakDown(String title) {
		return this.rule.breakDown(title, "", Set.of());
	}

}
