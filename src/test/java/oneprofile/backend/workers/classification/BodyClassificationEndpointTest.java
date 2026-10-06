package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BodyClassificationEndpoint.class)
class BodyClassificationEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private BodyClassificationRun body;

	@Test
	void reportsWhatTheBodyDecidedOfThePile() {
		given(this.body.classifyPile()).willReturn(new BodyClassificationRun.Report(21246, 9000, 7000, 4519, 727));

		assertThat(this.mvc.post().uri("/body-classifications")).hasStatusOk().bodyJson().isEqualTo("""
				{"pile":21246,"in":9000,"out":7000,"unknown":4519,"unsupportedLanguage":727}""");
	}

}
