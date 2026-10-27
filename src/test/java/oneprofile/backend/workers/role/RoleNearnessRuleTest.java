package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import oneprofile.backend.storage.rolenearness.RoleNearness;
import oneprofile.backend.storage.vacancyskill.SkillCounts;
import org.junit.jupiter.api.Test;

class RoleNearnessRuleTest {

	private final RoleNearnessRule rule = new RoleNearnessRule();

	@Test
	void isTheCosineOfTheSharesNamingEachSkillWeightedByHowFewOfTheSubsetNameIt() {
		// Of 8 vacancies, 4 name Spark (weight ln 2) and 2 Kafka (weight ln 4). Data engineering's shares
		// are 1 and 1/2, so its vector is ln 2 (1, 1); streaming's are 1/2 and 1, so ln 2 (1/2, 2). Their
		// cosine is 2.5 / (√2 · √4.25).
		SkillCounts subset = new SkillCounts(8, Map.of("spark", 4, "kafka", 2));

		assertThat(this.rule.nearness(subset, Map.of("data-engineer", new SkillCounts(2, Map.of("spark", 2, "kafka", 1)),
				"streaming-engineer", new SkillCounts(2, Map.of("spark", 1, "kafka", 2)))))
			.singleElement()
			.satisfies((near) -> assertThat(near.nearness()).isCloseTo(0.8575, within(0.0001)));
	}

	@Test
	void givesNoWeightToASkillEveryVacancyOfTheSubsetNames() {
		SkillCounts subset = new SkillCounts(4, Map.of("python", 4, "spark", 2, "react", 2));

		assertThat(this.rule.nearness(subset,
				Map.of("data-engineer", new SkillCounts(2, Map.of("python", 2, "spark", 2)), "frontend-engineer",
						new SkillCounts(2, Map.of("python", 2, "react", 2)))))
			.singleElement()
			.satisfies((near) -> assertThat(near.nearness()).isZero());
	}

	@Test
	void givesEachPairOnceTheRoleFirstInOrderOfSlug() {
		SkillCounts subset = new SkillCounts(4, Map.of("spark", 2, "react", 2));

		assertThat(this.rule.nearness(subset,
				Map.of("data-engineer", new SkillCounts(2, Map.of("spark", 2)), "frontend-engineer",
						new SkillCounts(2, Map.of("react", 2)), "full-stack-engineer",
						new SkillCounts(2, Map.of("react", 2)))))
			.extracting(RoleNearness::role, RoleNearness::nearRole)
			.containsExactlyInAnyOrder(tuple("data-engineer", "frontend-engineer"),
					tuple("data-engineer", "full-stack-engineer"),
					tuple("frontend-engineer", "full-stack-engineer"));
	}

	@Test
	void leavesOutARoleWhoseVacanciesNameNoSkillThatWeighs() {
		SkillCounts subset = new SkillCounts(4, Map.of("python", 4, "spark", 2));

		assertThat(this.rule.nearness(subset, Map.of("data-engineer", new SkillCounts(2, Map.of("spark", 2)),
				"support-engineer", new SkillCounts(2, Map.of("python", 2)))))
			.isEmpty();
	}

}
