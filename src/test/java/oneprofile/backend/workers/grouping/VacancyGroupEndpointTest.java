package oneprofile.backend.workers.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(VacancyGroupEndpoint.class)
class VacancyGroupEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private VacancyGroupRun run;

	@Test
	void reportsTheVacanciesGroupsDuplicatesAndFreshIds() {
		given(this.run.groupVacancies()).willReturn(new VacancyGroupRun.Report(8, 3, 2, 1));

		assertThat(this.mvc.post().uri("/vacancy-groups")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":8,"groups":3,"duplicates":2,"newGroups":1}""");
	}

}
