package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

	@MockitoBean
	private WaybackDiscoveryRun waybackDiscovery;

	@Test
	void reportsWhatTheRunFoundAndAddedForTheAtsItNames() throws Exception {
		given(this.discovery.discover(AtsEnum.ASHBY, CRAWL))
			.willReturn(new BoardDiscoveryRun.Report(AtsEnum.ASHBY, CRAWL, 2, 1));

		assertThat(this.mvc.post().uri("/discoveries/ashby/{crawl}", CRAWL)).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"ats":"ASHBY","crawl":"%s","slugs":2,"companiesAdded":1}""".formatted(CRAWL));
	}

	@Test
	void runsAWaybackDiscoveryRatherThanTakingWaybackForACrawl() throws Exception {
		given(this.waybackDiscovery.discover(AtsEnum.GREENHOUSE))
			.willReturn(new WaybackDiscoveryRun.Report(AtsEnum.GREENHOUSE, 3100000L, 17730, 10840));

		assertThat(this.mvc.post().uri("/discoveries/greenhouse/wayback")).hasStatusOk()
			.bodyJson()
			.isEqualTo("""
					{"ats":"GREENHOUSE","captures":3100000,"slugs":17730,"companiesAdded":10840}""");
		then(this.discovery).shouldHaveNoInteractions();
	}

}
