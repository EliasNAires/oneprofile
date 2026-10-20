package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import oneprofile.backend.storage.company.AtsEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(DiscoveryEndpoint.class)
class DiscoveryEndpointTest {

	private static final String CRAWL = "CC-MAIN-2026-34";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private BoardDiscoveryRun discovery;

	@Test
	void reportsWhatTheRunFoundAndAddedForTheAtsItNames() throws Exception {
		given(this.discovery.discover(AtsEnum.ASHBY, CRAWL))
			.willReturn(new BoardDiscoveryRun.Report(AtsEnum.ASHBY, CRAWL, 2, 1));

		assertThat(this.mvc.post().uri("/discoveries/ashby/{crawl}", CRAWL)).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"ats":"ASHBY","crawl":"%s","slugs":2,"companiesAdded":1}""".formatted(CRAWL));
	}

}
