package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PileExportEndpoint.class)
class PileExportEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PileExportRun export;

	@Test
	void writesThePileToTheFileItIsGiven() {
		given(this.export.exportTo(Path.of("/tmp/pile.jsonl")))
			.willReturn(new PileExportRun.Report("/tmp/pile.jsonl", 20519, 727, 6000, 14000, 1500, 3500));

		assertThat(this.mvc.post().uri("/pile-exports").param("file", "/tmp/pile.jsonl")).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"file":"/tmp/pile.jsonl","vacancies":20519,"unsupportedLanguage":727,"meanDescriptionCharacters":6000,\
					"p95DescriptionCharacters":14000,"meanDescriptionTokens":1500,"p95DescriptionTokens":3500}""");
	}

}
