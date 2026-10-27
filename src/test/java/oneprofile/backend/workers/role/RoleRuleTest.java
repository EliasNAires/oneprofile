package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.rolelist.RoleListConfiguration;
import oneprofile.backend.storage.rolelist.RoleListStore;
import org.junit.jupiter.api.Test;

/** The worked examples of {@code docs/role-criterion.md}, read by the list the application ships. */
class RoleRuleTest {

	private final RoleRule rule = new RoleRule(RoleListConfiguration.shipped());

	@Test
	void givesTheRoleTheKindInFrontOfTheHeadNames() {
		assertRoles(Map.of("data engineer", "data-engineer", "data architect", "data-engineer", "security researcher",
				"security-engineer", "penetration tester", "security-engineer", "web developer", "frontend-engineer",
				"mobile application developer", "mobile-engineer", "network administrator", "network-engineer",
				"customer success engineer", "sales-engineer", "applied scientist", "machine-learning-engineer"));
	}

	@Test
	void givesAKindTheRoleItHasBesideItsHead() {
		assertRoles(Map.of("data analyst", "data-analyst", "data scientist", "data-scientist", "security analyst",
				"security-analyst", "machine learning scientist", "research-scientist", "cloud developer",
				"software-engineer", "cloud architect", "platform-engineer", "database developer", "data-engineer",
				"database administrator", "systems-administrator", "it analyst", "support-engineer"));
	}

	@Test
	void readsTheLongestPhraseOfKindWords() {
		assertRoles(Map.of("network security engineer", "network-engineer", "systems software engineer",
				"embedded-engineer", "it support engineer", "support-engineer", "business intelligence engineer",
				"data-analyst", "business systems analyst", "business-analyst", "design verification engineer",
				RoleListStore.NONE));
	}

	@Test
	void readsADevelopmentEngineerAsADeveloper() {
		assertRoles(Map.of("development engineer", "software-engineer", "systems software development engineer",
				"embedded-engineer", "cloud software development engineer", "software-engineer"));
	}

	@Test
	void letsPlatformInfrastructureAndSoftwareYieldToAKindInFrontOfThem() {
		assertRoles(Map.of("data infrastructure engineer", "data-engineer", "machine learning platform engineer",
				"machine-learning-engineer", "security platform engineer", "security-engineer",
				"backend platform engineer", "backend-engineer", "robotics software engineer", "robotics-engineer",
				"platform engineer", "platform-engineer", "software architect", "software-engineer"));
	}

	@Test
	void sendsAnEngineerOrArchitectWithASaleWordToSalesEngineering() {
		assertRoles(Map.of("presales systems engineer", "sales-engineer", "partner solutions architect",
				"sales-engineer", "network sales engineer", "sales-engineer", "solutions architect",
				"solutions-architect", "ai solutions architect", "solutions-architect"));
	}

	@Test
	void letsAPlatformWordDecideWhateverTheHead() {
		assertRoles(Map.of("salesforce business analyst", "enterprise-platform-developer", "sap consultant",
				"enterprise-platform-developer", "forward deployed software engineer", "forward-deployed-engineer"));
	}

	@Test
	void readsAJobWhole() {
		assertRoles(Map.of("data product manager", "product-manager", "salesforce product owner", "product-manager",
				"technical program manager", "program-manager", "scrum master", "program-manager"));
	}

	@Test
	void readsAStageOfACareerPast() {
		assertRoles(Map.of("data science intern", "data-scientist", "graduate software engineer",
				"software-engineer"));
		assertThat(this.rule.read("intern").role()).isEqualTo(RoleListStore.NONE);
	}

	@Test
	void givesABareHeadThatIsNoRankItsRole() {
		assertRoles(Map.of("engineer", "software-engineer", "developer", "software-engineer", "engineering",
				"software-engineer", "technology", "software-engineer", "ingeniero", "software-engineer",
				"administrator", "systems-administrator", "architect", "solutions-architect"));
	}

	@Test
	void givesAHeadOfManyKindsOfWorkNoRole() {
		assertRoles(Map.of("analyst", RoleListStore.NONE, "consultant", RoleListStore.NONE, "scientist", RoleListStore.NONE,
				"systems engineer", RoleListStore.NONE, "automation engineer", RoleListStore.NONE, "design engineer",
				RoleListStore.NONE, "technical consultant", RoleListStore.NONE, "game developer", RoleListStore.NONE));
	}

	@Test
	void letsAKindInFrontOfAHeadOfManyKindsDecide() {
		assertRoles(Map.of("web design engineer", "frontend-engineer", "network automation engineer",
				"network-engineer", "it systems engineer", "systems-administrator", "test automation engineer",
				"qa-engineer", "sales development engineer", "sales-engineer"));
	}

	@Test
	void namesWhatAYieldingWordGivesWhereAKindInFrontOfItGivesNoRole() {
		RoleReading game = this.rule.read("game platform engineer");
		assertThat(game.role()).isEqualTo(RoleListStore.NONE);
		assertThat(game.competing()).containsExactly("platform-engineer");
	}

	@Test
	void readsAnAreaLedAsItsKind() {
		assertRoles(Map.of("data science", "data-scientist", "information security", "security-engineer", "it",
				"systems-administrator", "salesforce", "enterprise-platform-developer"));
	}

	@Test
	void givesATitleTheFileListsTheRoleTheFileGives() {
		RoleReading listed = this.rule.read("ai infrastructure engineer");
		assertThat(listed.role()).isEqualTo("platform-engineer");
		assertThat(listed.reading()).isEqualTo(RoleReadingEnum.LISTED);
		assertRoles(Map.of("manager", RoleListStore.NONE, "data", RoleListStore.NONE, "technical account manager",
				"sales-engineer"));
	}

	@Test
	void reachesNoRoleWhereNoWordOfTheTitleSaysOne() {
		RoleReading unreached = this.rule.read("performance engineer");
		assertThat(unreached.reading()).isEqualTo(RoleReadingEnum.UNREACHED);
		assertThat(unreached.role()).isEqualTo(RoleListStore.NONE);
		assertThat(this.rule.read("release manager").reading()).isEqualTo(RoleReadingEnum.UNREACHED);
	}

	@Test
	void namesTheRolesItsOtherKindsGiveWhereTwoKindsDisagree() {
		RoleReading contested = this.rule.read("machine learning data engineer");
		assertThat(contested.role()).isEqualTo("data-engineer");
		assertThat(contested.competing()).containsExactly("machine-learning-engineer");
		assertThat(this.rule.read("platform data engineer").competing()).isEmpty();
	}

	@Test
	void givesAVacancyTheRoleOfEachHeadItsTitleNames() {
		assertThat(this.rule.roles(List.of("data scientist", "machine learning engineer")))
			.containsExactly("data-scientist", "machine-learning-engineer");
		assertThat(this.rule.roles(List.of("software engineer", "software developer"))).containsExactly("software-engineer");
	}

	@Test
	void givesABareRankNoRole() {
		assertThat(this.rule.roles(List.of())).isEmpty();
		assertThat(this.rule.roles(List.of("manager"))).isEmpty();
	}

	private void assertRoles(Map<String, String> roles) {
		assertThat(roles).allSatisfy((title, role) -> assertThat(this.rule.read(title).role()).as(title).isEqualTo(role));
	}

}
