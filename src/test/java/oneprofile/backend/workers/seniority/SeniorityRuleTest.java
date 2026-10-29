package oneprofile.backend.workers.seniority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import org.junit.jupiter.api.Test;

class SeniorityRuleTest {

	private final SeniorityRule rule = new SeniorityRule();

	@Test
	void takesTheLevelATitleNamesOverTheRequiredYears() {
		assertThat(this.rule.seniorityLevel(Set.of(SeniorityLevelEnum.SENIOR), 2)).isEqualTo(SeniorityLevelEnum.SENIOR);
	}

	@Test
	void takesTheLowestOfTheLevelsATitleNames() {
		assertThat(this.rule.seniorityLevel(Set.of(SeniorityLevelEnum.STAFF, SeniorityLevelEnum.SENIOR), null))
			.isEqualTo(SeniorityLevelEnum.SENIOR);
	}

	@Test
	void placesTheRequiredYearsOnTheLadderWhereTheTitleNamesNoLevel() {
		assertThat(this.rule.seniorityLevel(Set.of(), 0)).isEqualTo(SeniorityLevelEnum.ENTRY);
		assertThat(this.rule.seniorityLevel(Set.of(), 1)).isEqualTo(SeniorityLevelEnum.ENTRY);
		assertThat(this.rule.seniorityLevel(Set.of(), 2)).isEqualTo(SeniorityLevelEnum.JUNIOR);
		assertThat(this.rule.seniorityLevel(Set.of(), 3)).isEqualTo(SeniorityLevelEnum.MID);
		assertThat(this.rule.seniorityLevel(Set.of(), 4)).isEqualTo(SeniorityLevelEnum.MID);
		assertThat(this.rule.seniorityLevel(Set.of(), 5)).isEqualTo(SeniorityLevelEnum.SENIOR);
		assertThat(this.rule.seniorityLevel(Set.of(), 7)).isEqualTo(SeniorityLevelEnum.SENIOR);
		assertThat(this.rule.seniorityLevel(Set.of(), 8)).isEqualTo(SeniorityLevelEnum.STAFF);
		assertThat(this.rule.seniorityLevel(Set.of(), 15)).isEqualTo(SeniorityLevelEnum.STAFF);
	}

	@Test
	void placesANumberedLevelByTheRequiredYears() {
		assertThat(this.rule.seniorityLevel(Set.of(SeniorityLevelEnum.II), 3)).isEqualTo(SeniorityLevelEnum.MID);
		assertThat(this.rule.seniorityLevel(Set.of(SeniorityLevelEnum.II), null)).isNull();
	}

	@Test
	void tellsATitleNamingOnlyNumberedLevels() {
		assertThat(this.rule.namesOnlyNumberedLevels(Set.of(SeniorityLevelEnum.II))).isTrue();
		assertThat(this.rule.namesOnlyNumberedLevels(Set.of(SeniorityLevelEnum.II, SeniorityLevelEnum.SENIOR))).isFalse();
		assertThat(this.rule.namesOnlyNumberedLevels(Set.of())).isFalse();
	}

	@Test
	void givesNoLevelWhereNeitherTheTitleNorTheYearsSayOne() {
		assertThat(this.rule.seniorityLevel(Set.of(), null)).isNull();
	}

}
