package oneprofile.backend.workers.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ClassificationEndpoint.class)
class ClassificationEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CorpusClassificationRun classification;

	@Test
	void reportsWhatTheRunDecidedAndWhyItLeftTheRestUndecided() {
		given(this.classification.classifyAll())
			.willReturn(new CorpusClassificationRun.Report(179098, 12044, 48310, 118744, 79201, 33128, 6415));

		assertThat(this.mvc.post().uri("/classifications")).hasStatusOk().bodyJson().isEqualTo("""
				{"vacancies":179098,"in":12044,"out":48310,"unknown":118744,\
				"unruled":79201,"domainAmbiguity":33128,"scopeAmbiguity":6415}""");
	}

}
