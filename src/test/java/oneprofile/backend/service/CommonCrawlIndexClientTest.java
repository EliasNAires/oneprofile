package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class CommonCrawlIndexClientTest {

	private static final String INDEX_ID = "CC-MAIN-2026-34";

	private static final String PATTERN = "boards.greenhouse.io/";

	/** The pattern travels percent-encoded in the query string; the index decodes it. */
	private static final String ENCODED_PATTERN = "boards.greenhouse.io%2F";

	private MockRestServiceServer server;

	private CommonCrawlIndexClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		this.server = MockRestServiceServer.bindTo(builder).build();
		// No wait between retries: the point under test is the retry, not the backoff.
		this.client = new CommonCrawlIndexClient(builder, Duration.ZERO);
	}

	@Test
	void readsEveryPageTheIndexReports() {
		this.server.expect(queryParam("url", ENCODED_PATTERN))
				.andExpect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("{\"pages\":2,\"pageSize\":5,\"blocks\":9}", MediaType.APPLICATION_JSON));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withSuccess("""
						{"urlkey":"io,greenhouse,boards)/mercadolibre","url":"https://boards.greenhouse.io/mercadolibre"}
						{"urlkey":"io,greenhouse,boards)/globant","url":"https://boards.greenhouse.io/globant"}
						""", MediaType.APPLICATION_JSON));
		this.server.expect(queryParam("page", "1"))
				.andRespond(withSuccess("""
						{"urlkey":"io,greenhouse,boards)/auth0","url":"https://boards.greenhouse.io/auth0"}
						""", MediaType.APPLICATION_JSON));

		assertThat(collectUrls()).containsExactly(
				"https://boards.greenhouse.io/mercadolibre",
				"https://boards.greenhouse.io/globant",
				"https://boards.greenhouse.io/auth0");
		this.server.verify();
	}

	@Test
	void asksForNoPageWhenTheIndexHasNone() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("{\"pages\":0,\"pageSize\":5,\"blocks\":0}", MediaType.APPLICATION_JSON));

		assertThat(collectUrls()).isEmpty();
		this.server.verify();
	}

	@Test
	void skipsBlankLinesAndLinesWithoutUrl() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("{\"pages\":1}", MediaType.APPLICATION_JSON));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withSuccess("""
						{"urlkey":"io,greenhouse,boards)/globant","url":"https://boards.greenhouse.io/globant"}

						{"urlkey":"io,greenhouse,boards)/nourl"}
						""", MediaType.APPLICATION_JSON));

		assertThat(collectUrls()).containsExactly("https://boards.greenhouse.io/globant");
	}

	@Test
	void retriesWhenTheIndexIsOverloaded() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("{\"pages\":1}", MediaType.APPLICATION_JSON));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withSuccess("""
						{"url":"https://boards.greenhouse.io/globant"}
						""", MediaType.APPLICATION_JSON));

		assertThat(collectUrls()).containsExactly("https://boards.greenhouse.io/globant");
		this.server.verify();
	}

	@Test
	void givesUpAfterFourAttempts() {
		this.server.expect(ExpectedCount.times(4), queryParam("showNumPages", "true"))
				.andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

		assertThatThrownBy(this::collectUrls).isInstanceOf(HttpServerErrorException.class);
		this.server.verify();
	}

	@Test
	void doesNotRetryWhenTheIndexRejectsTheRequest() {
		this.server.expect(ExpectedCount.once(), queryParam("showNumPages", "true"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(this::collectUrls).isInstanceOf(HttpClientErrorException.class);
		this.server.verify();
	}

	private List<String> collectUrls() {
		List<String> urls = new ArrayList<>();
		this.client.forEachUrl(INDEX_ID, PATTERN, urls::add);
		return urls;
	}
}
