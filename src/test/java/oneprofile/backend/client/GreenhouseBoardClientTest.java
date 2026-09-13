package oneprofile.backend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;

import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.client.GreenhouseBoardClient.BoardJob;
import oneprofile.backend.client.GreenhouseBoardClient.BoardProbe;
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

	private static final String JOBS_URL = BOARD_URL + "?content=true&pay_transparency=true";

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
	@Test
	void mapsAnOpeningWithEverythingTheBoardPublishes() {
		this.server.expect(requestTo(JOBS_URL)).andRespond(withSuccess("""
				{"jobs":[{
				  "id": 8172510,
				  "title": "Backend Engineer",
				  "location": {"name": "Buenos Aires, Argentina"},
				  "departments": [{"id": 4095160002, "name": "Engineering"}],
				  "content": "&lt;p&gt;Java &amp;amp; Spring&lt;/p&gt;",
				  "absolute_url": "https://job-boards.greenhouse.io/globant/jobs/8172510",
				  "language": "en",
				  "pay_input_ranges": [
				    {"min_cents": 12000000, "max_cents": 18000000, "currency_type": "USD",
				     "title": "Annual base salary range:"}
				  ],
				  "first_published": "2026-09-09T10:50:29-04:00",
				  "updated_at": "2026-09-10T13:11:58-04:00"
				}],"meta":{"total":1}}
				""", MediaType.APPLICATION_JSON));

		assertThat(this.client.jobs(SLUG)).containsExactly(new BoardJob(8172510L, "Backend Engineer",
				"Buenos Aires, Argentina", "Engineering", "Java & Spring",
				"https://job-boards.greenhouse.io/globant/jobs/8172510", "en", 12000000L, 18000000L, "USD",
				"Annual base salary range:", Instant.parse("2026-09-09T14:50:29Z"),
				Instant.parse("2026-09-10T17:11:58Z")));
		this.server.verify();
	}

	@Test
	void leavesThePayFieldsNullWhenTheBoardPublishesNoRange() {
		this.server.expect(requestTo(JOBS_URL)).andRespond(withSuccess("""
				{"jobs":[{
				  "id": 4001,
				  "title": "Data Analyst",
				  "location": {"name": "Remote - LATAM"},
				  "departments": [{"name": "Data"}],
				  "content": "&lt;p&gt;SQL&lt;/p&gt;",
				  "absolute_url": "https://job-boards.greenhouse.io/globant/jobs/4001",
				  "language": "en",
				  "pay_input_ranges": [],
				  "first_published": "2026-09-09T10:50:29-04:00",
				  "updated_at": "2026-09-09T10:50:29-04:00"
				}],"meta":{"total":1}}
				""", MediaType.APPLICATION_JSON));

		BoardJob job = this.client.jobs(SLUG).getFirst();

		assertThat(job.payMinCents()).isNull();
		assertThat(job.payMaxCents()).isNull();
		assertThat(job.payCurrency()).isNull();
		assertThat(job.payTitle()).isNull();
		assertThat(job.title()).isEqualTo("Data Analyst");
	}

	@Test
	void readsAnEmptyBoardAsNoOpenings() {
		this.server.expect(requestTo(JOBS_URL))
				.andRespond(withSuccess("{\"jobs\":[],\"meta\":{\"total\":0}}", MediaType.APPLICATION_JSON));

		assertThat(this.client.jobs(SLUG)).isEmpty();
	}
}
