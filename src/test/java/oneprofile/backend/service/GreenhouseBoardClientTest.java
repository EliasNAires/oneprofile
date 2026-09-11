package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.service.GreenhouseBoardClient.BoardProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class GreenhouseBoardClientTest {

	private static final String SLUG = "globant";

	private static final String BOARD_URL = "https://boards-api.greenhouse.io/v1/boards/globant/jobs";

	private MockRestServiceServer server;

	private GreenhouseBoardClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		this.server = MockRestServiceServer.bindTo(builder).build();
		// No wait between retries: the point under test is the retry, not the backoff.
		this.client = new GreenhouseBoardClient(builder, Duration.ZERO);
	}

	@Test
	void readsTheOpeningsAndTheCompanyNameOfALiveBoard() {
		this.server.expect(requestTo(BOARD_URL)).andRespond(withSuccess("""
				{"jobs":[
				  {"id":4001,"title":"Backend Engineer","company_name":"Globant"},
				  {"id":4002,"title":"Data Analyst","company_name":"Globant"}
				],"meta":{"total":2}}
				""", MediaType.APPLICATION_JSON));

		assertThat(this.client.probe(SLUG)).isEqualTo(new BoardProbe(BoardStatus.ACTIVE, 2, "Globant"));
		this.server.verify();
	}

	@Test
	void reportsABoardWithoutOpeningsAsEmptyAndWithoutAName() {
		this.server.expect(requestTo(BOARD_URL))
				.andRespond(withSuccess("{\"jobs\":[],\"meta\":{\"total\":0}}", MediaType.APPLICATION_JSON));

		assertThat(this.client.probe(SLUG)).isEqualTo(new BoardProbe(BoardStatus.EMPTY, 0, null));
	}

	@Test
	void reportsAnUnknownSlugAsNotFound() {
		this.server.expect(requestTo(BOARD_URL))
				.andRespond(withStatus(HttpStatus.NOT_FOUND)
						.body("{\"status\":404,\"error\":\"Job not found\"}")
						.contentType(MediaType.APPLICATION_JSON));

		assertThat(this.client.probe(SLUG)).isEqualTo(new BoardProbe(BoardStatus.NOT_FOUND, 0, null));
	}

	@Test
	void doesNotRetryAnUnknownSlug() {
		this.server.expect(ExpectedCount.once(), requestTo(BOARD_URL))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		this.client.probe(SLUG);
		this.server.verify();
	}

	@Test
	void retriesWhenGreenhouseIsOverloaded() {
		this.server.expect(requestTo(BOARD_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		this.server.expect(requestTo(BOARD_URL))
				.andRespond(withSuccess("{\"jobs\":[],\"meta\":{\"total\":0}}", MediaType.APPLICATION_JSON));

		assertThat(this.client.probe(SLUG).status()).isEqualTo(BoardStatus.EMPTY);
		this.server.verify();
	}

	@Test
	void givesUpAfterThreeAttempts() {
		this.server.expect(ExpectedCount.times(3), requestTo(BOARD_URL))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThatThrownBy(() -> this.client.probe(SLUG)).isInstanceOf(HttpServerErrorException.class);
		this.server.verify();
	}
}
