package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(RoleAssignmentEndpoint.class)
class RoleAssignmentEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RoleAssignmentRun assignment;

	@Test
	void reportsHowManyVacanciesItGaveRolesTo() {
		given(this.assignment.assignRoles()).willReturn(new RoleAssignmentRun.Report(3, 1));

		assertThat(this.mvc.post().uri("/roles")).hasStatusOk().bodyJson().isEqualTo("""
				{"vacancies":3,"withoutRole":1}""");
	}

}
