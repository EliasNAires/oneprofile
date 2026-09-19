package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.Companies;
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
	private GreenhouseSlugDiscovery discovery;

	@MockitoBean
	private Companies companies;

	@Test
	void recordsTheSlugsTheCrawlNamesAndReportsWhatItAdded() throws Exception {
		given(this.discovery.discover(CRAWL)).willReturn(new TreeSet<>(List.of("notion", "stripe")));
		given(this.companies.record(eq(Ats.GREENHOUSE), eq(new TreeSet<>(List.of("notion", "stripe"))))).willReturn(1);

		assertThat(this.mvc.post().uri("/discoveries/{crawl}", CRAWL)).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"crawl":"%s","slugs":2,"companiesAdded":1}""".formatted(CRAWL));
	}

	@Test
	void doesNotRecordAnythingWhenTheCrawlCouldNotBeRead() throws Exception {
		given(this.discovery.discover(CRAWL)).willThrow(new IOException("data.commoncrawl.org answered 503"));

		assertThat(this.mvc.post().uri("/discoveries/{crawl}", CRAWL)).hasFailed()
			.failure()
			.isInstanceOf(IOException.class);
		then(this.companies).shouldHaveNoInteractions();
	}

}
