package oneprofile.backend.workers.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(RoleNearnessEndpoint.class)
class RoleNearnessEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RoleNearnessRun nearness;

	@Test
	void reportsHowManyPairsOfRolesItComputed() {
		given(this.nearness.computeNearness()).willReturn(new RoleNearnessRun.Report(3, 3));

		assertThat(this.mvc.post().uri("/role-nearness")).hasStatusOk().bodyJson().isEqualTo("""
				{"roles":3,"pairs":3}""");
	}

}
