package oneprofile.backend.workers.deduplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(DuplicateEndpoint.class)
class DuplicateEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private DuplicateRun run;

	@Test
	void reportsTheVacanciesAndDuplicates() {
		given(this.run.markDuplicates()).willReturn(new DuplicateRun.Report(8, 2));

		assertThat(this.mvc.post().uri("/duplicates")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":8,"duplicates":2}""");
	}

}
