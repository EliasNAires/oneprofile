package oneprofile.backend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParamCount;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
import org.springframework.web.client.RestClient;

class WaybackCdxClientTest {

	private static final String PATTERN = "boards.greenhouse.io/";

	private MockRestServiceServer server;

	private WaybackCdxClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		this.server = MockRestServiceServer.bindTo(builder).build();
		// No wait between pages or retries: neither is what these tests are about.
		this.client = new WaybackCdxClient(builder, Duration.ZERO, Duration.ZERO);
	}

	@Test
	void readsEveryPageTheArchiveReports() {
		this.server.expect(requestTo(
				"https://web.archive.org/cdx/search/cdx?url=boards.greenhouse.io%2F&matchType=prefix&showNumPages=true"))
				.andRespond(withSuccess("2\n", MediaType.TEXT_PLAIN));
		this.server.expect(requestTo(
				"https://web.archive.org/cdx/search/cdx?url=boards.greenhouse.io%2F&matchType=prefix&fl=original&page=0"))
				.andRespond(withSuccess("""
						https://boards.greenhouse.io/mercadolibre
						http://boards.greenhouse.io/globant
						""", MediaType.TEXT_PLAIN));
		this.server.expect(queryParam("page", "1"))
				.andRespond(withSuccess("https://boards.greenhouse.io/auth0\n", MediaType.TEXT_PLAIN));

		assertThat(collectUrls()).containsExactly(
				"https://boards.greenhouse.io/mercadolibre",
				"http://boards.greenhouse.io/globant",
				"https://boards.greenhouse.io/auth0");
		this.server.verify();
	}

	@Test
	void asksForNoPageWhenTheArchiveHasNone() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("0\n", MediaType.TEXT_PLAIN));

		assertThat(collectUrls()).isEmpty();
		this.server.verify();
	}

	@Test
	void asksForTheCountWithoutFieldsAndForThePagesWithTheOriginalUrlOnly() {
		this.server.expect(queryParam("showNumPages", "true"))
				// url, matchType and showNumPages: no fl.
				.andExpect(queryParamCount(3))
				.andRespond(withSuccess("1\n", MediaType.TEXT_PLAIN));
		this.server.expect(queryParam("page", "0"))
				.andExpect(queryParam("fl", "original"))
				.andRespond(withSuccess("https://boards.greenhouse.io/globant\n", MediaType.TEXT_PLAIN));

		assertThat(collectUrls()).containsExactly("https://boards.greenhouse.io/globant");
		this.server.verify();
	}

	@Test
	void skipsBlankLines() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("1\n", MediaType.TEXT_PLAIN));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withSuccess("""
						https://boards.greenhouse.io/globant

						https://boards.greenhouse.io/auth0
						""", MediaType.TEXT_PLAIN));

		assertThat(collectUrls()).containsExactly("https://boards.greenhouse.io/globant",
				"https://boards.greenhouse.io/auth0");
	}

	@Test
	void retriesWhenTheArchiveIsUnavailableOrRateLimited() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("1\n", MediaType.TEXT_PLAIN));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		this.server.expect(queryParam("page", "0"))
				.andRespond(withSuccess("https://boards.greenhouse.io/globant\n", MediaType.TEXT_PLAIN));

		assertThat(collectUrls()).containsExactly("https://boards.greenhouse.io/globant");
		this.server.verify();
	}

	@Test
	void doesNotRetryWhenThePageIsOutOfRange() {
		this.server.expect(queryParam("showNumPages", "true"))
				.andRespond(withSuccess("1\n", MediaType.TEXT_PLAIN));
		this.server.expect(ExpectedCount.once(), queryParam("page", "0"))
				.andRespond(withStatus(HttpStatus.BAD_REQUEST));

		assertThatThrownBy(this::collectUrls).isInstanceOf(HttpClientErrorException.class);
		this.server.verify();
	}

	private List<String> collectUrls() {
		List<String> urls = new ArrayList<>();
		this.client.forEachUrl(PATTERN, urls::add);
		return urls;
	}
}
