package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TitleBreakdownEndpoint.class)
class TitleBreakdownEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TitleBreakdownRun breakdown;

	@Test
	void reportsHowManyTitlesItBrokeDown() {
		given(this.breakdown.breakDownTitles()).willReturn(new TitleBreakdownRun.Report(3, 1));

		assertThat(this.mvc.post().uri("/title-breakdowns")).hasStatusOk().bodyJson().isEqualTo("""
				{"vacancies":3,"withoutNormalizedTitle":1}""");
	}

}
