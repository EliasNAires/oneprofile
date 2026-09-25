package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GreenhouseDiscoveryEndpoint.class)
class GreenhouseDiscoveryEndpointTest {

	private static final String CRAWL = "CC-MAIN-2026-34";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private GreenhouseDiscoveryRun discovery;

	@Test
	void reportsWhatTheRunFoundAndAdded() throws Exception {
		given(this.discovery.discover(CRAWL)).willReturn(new GreenhouseDiscoveryRun.Report(CRAWL, 2, 1));

		assertThat(this.mvc.post().uri("/discoveries/{crawl}", CRAWL)).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"crawl":"%s","slugs":2,"companiesAdded":1}""".formatted(CRAWL));
	}

}
