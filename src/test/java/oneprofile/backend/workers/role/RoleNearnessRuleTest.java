package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import oneprofile.backend.storage.rolenearness.RoleNearness;
import oneprofile.backend.storage.vacancyskill.SkillProfile;
import org.junit.jupiter.api.Test;

class RoleNearnessRuleTest {

	private final RoleNearnessRule rule = new RoleNearnessRule();

	@Test
	void isTheCosineOfTheSharesNamingEachSkillWeightedByHowFewOfTheSubsetNameIt() {
		// Of 8 vacancies, 4 name Spark (weight ln 2) and 2 Kafka (weight ln 4). Data engineering's shares
		// are 1 and 1/2, so its vector is ln 2 (1, 1); streaming's are 1/2 and 1, so ln 2 (1/2, 2). Their
		// cosine is 2.5 / (√2 · √4.25).
		SkillProfile subset = new SkillProfile(8, Map.of("spark", 4, "kafka", 2));

		assertThat(this.rule.nearness(subset, Map.of("data-engineer", new SkillProfile(2, Map.of("spark", 2, "kafka", 1)),
				"streaming-engineer", new SkillProfile(2, Map.of("spark", 1, "kafka", 2)))))
			.singleElement()
			.satisfies((near) -> assertThat(near.nearness()).isCloseTo(0.8575, within(0.0001)));
	}

	@Test
	void givesNoWeightToASkillEveryVacancyOfTheSubsetNames() {
		SkillProfile subset = new SkillProfile(4, Map.of("python", 4, "spark", 2, "react", 2));

		assertThat(this.rule.nearness(subset,
				Map.of("data-engineer", new SkillProfile(2, Map.of("python", 2, "spark", 2)), "frontend-engineer",
						new SkillProfile(2, Map.of("python", 2, "react", 2)))))
			.singleElement()
			.satisfies((near) -> assertThat(near.nearness()).isZero());
	}

	@Test
	void givesEachPairOnceTheRoleFirstInOrderOfSlug() {
		SkillProfile subset = new SkillProfile(4, Map.of("spark", 2, "react", 2));

		assertThat(this.rule.nearness(subset,
				Map.of("data-engineer", new SkillProfile(2, Map.of("spark", 2)), "frontend-engineer",
						new SkillProfile(2, Map.of("react", 2)), "full-stack-engineer",
						new SkillProfile(2, Map.of("react", 2)))))
			.extracting(RoleNearness::role, RoleNearness::nearRole)
			.containsExactlyInAnyOrder(tuple("data-engineer", "frontend-engineer"),
					tuple("data-engineer", "full-stack-engineer"),
					tuple("frontend-engineer", "full-stack-engineer"));
	}

	@Test
	void leavesOutARoleWhoseVacanciesNameNoSkillThatWeighs() {
		SkillProfile subset = new SkillProfile(4, Map.of("python", 4, "spark", 2));

		assertThat(this.rule.nearness(subset, Map.of("data-engineer", new SkillProfile(2, Map.of("spark", 2)),
				"support-engineer", new SkillProfile(2, Map.of("python", 2)))))
			.isEmpty();
	}

}
