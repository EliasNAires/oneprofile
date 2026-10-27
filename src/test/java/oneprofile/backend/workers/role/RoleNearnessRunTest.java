package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.rolenearness.RoleNearness;
import oneprofile.backend.storage.rolenearness.RoleNearnessStore;
import oneprofile.backend.storage.vacancyskill.SkillCounts;
import oneprofile.backend.storage.vacancyskill.VacancySkillStore;
import org.junit.jupiter.api.Test;

class RoleNearnessRunTest {

	private final VacancySkillStore skills = mock(VacancySkillStore.class);

	private final RoleNearnessStore nearness = mock(RoleNearnessStore.class);

	private final RoleNearnessRun run = new RoleNearnessRun(new RoleNearnessRule(), this.skills, this.nearness);

	@Test
	void replacesTheNearnessHeldWithThatOfTheRolesTheSubsetHoldsNow() {
		given(this.skills.subsetSkillCounts()).willReturn(new SkillCounts(4, Map.of("spark", 2, "react", 2)));
		given(this.skills.roleSkillCounts()).willReturn(Map.of("data-engineer", new SkillCounts(2, Map.of("spark", 2)),
				"frontend-engineer", new SkillCounts(2, Map.of("react", 2))));
		given(this.nearness.replace(anyList())).willAnswer((invocation) -> invocation.<List<?>>getArgument(0).size());

		assertThat(this.run.computeNearness()).isEqualTo(new RoleNearnessRun.Report(2, 1));

		then(this.nearness).should()
			.replace(List.of(new RoleNearness("data-engineer", "frontend-engineer", 0.0)));
	}

}
