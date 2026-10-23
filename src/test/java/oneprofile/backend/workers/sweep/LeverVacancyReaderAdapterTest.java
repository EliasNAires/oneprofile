package oneprofile.backend.workers.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedLeverVacancy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercised against a stub server rather than {@code api.lever.co} and {@code api.eu.lever.co}: each
 * region's API is a path of its own on it.
 */
class LeverVacancyReaderAdapterTest {

	/**
	 * One opening as Lever publishes it, cut down from a real one: the plain-text copies of each HTML
	 * field, and the halves {@code description} is made of, are left out.
	 */
	private static final String BOARD = """
			[{"id":"d2907c20-a739-4c0b-a10e-94aaf1f72c78","text":"Account Executive, Direct Merchant",
			"categories":{"commitment":"Full-time","department":"Revenue","location":"San Francisco",
			"team":"Sales","allLocations":["San Francisco","Toronto","Remote"]},
			"createdAt":1787348790172,"country":"US","workplaceType":"remote",
			"description":"<p><strong>About Finix</strong></p><div>Finix is building payments.</div>",
			"lists":[{"text":"You Will: ","content":"<li>Own the sales cycle.</li><li>Close deals.</li>"},
			{"text":"You Have:","content":"<li>Five years of sales.</li>"}],
			"salaryDescription":"<div>The base salary is set by location.</div>",
			"additional":"<div>Finix is an equal opportunity employer.</div>",
			"salaryRange":{"currency":"USD","interval":"per-year-salary","min":140000,"max":180000},
			"hostedUrl":"https://jobs.lever.co/finix/d2907c20-a739-4c0b-a10e-94aaf1f72c78",
			"applyUrl":"https://jobs.lever.co/finix/d2907c20-a739-4c0b-a10e-94aaf1f72c78/apply"}]""";

	private HttpServer server;

	private final List<String> queries = new ArrayList<>();

	@BeforeEach
	void startServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress(0), 0);
		this.server.start();
	}

	@AfterEach
	void stopServer() {
		this.server.stop(0);
	}

	@Test
	void readsEveryOpeningOfABoardFromTheApiOfItsRegionWithWhatLeverStructures() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, BOARD));

		assertThat(reader().read("finix", RegionEnum.US, () -> {})).containsExactly(new PublishedLeverVacancy(
				"d2907c20-a739-4c0b-a10e-94aaf1f72c78", "Account Executive, Direct Merchant", "San Francisco",
				"Revenue",
				"""
						# About Finix
						Finix is building payments.
						# You Will:
						- Own the sales cycle.
						- Close deals.
						# You Have:
						- Five years of sales.
						The base salary is set by location.
						Finix is an equal opportunity employer.""",
				"https://jobs.lever.co/finix/d2907c20-a739-4c0b-a10e-94aaf1f72c78", 14000000L, 18000000L, "USD",
				Instant.ofEpochMilli(1787348790172L), "remote", "US", "Full-time", "Sales",
				List.of("San Francisco", "Toronto", "Remote"), "per-year-salary"));
		assertThat(this.queries).containsExactly("/us/v0/postings/finix?mode=json");
	}

	@Test
	void asksTheEuApiForABoardOfTheEu() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, "[]"));

		assertThat(reader().read("kwalee", RegionEnum.EU, () -> {})).isEmpty();
		assertThat(this.queries).containsExactly("/eu/v0/postings/kwalee?mode=json");
	}

	@Test
	void bringsNothingWhereTheBoardPublishedNoPayDepartmentListsOrDescription() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, """
				[{"id":"0f0e0d0c-0000-4000-8000-000000000001","text":"Engineer",
				"categories":{"location":"London","allLocations":["London"]},
				"createdAt":1787348790172,"workplaceType":"unspecified","description":"","lists":[],
				"additional":"",
				"hostedUrl":"https://jobs.eu.lever.co/kwalee/0f0e0d0c-0000-4000-8000-000000000001"}]"""));

		PublishedLeverVacancy opening = (PublishedLeverVacancy) reader().read("kwalee", RegionEnum.EU, () -> {}).get(0);
		assertThat(opening.description()).isNull();
		assertThat(opening.department()).isNull();
		assertThat(opening.payMinCents()).isNull();
		assertThat(opening.payMaxCents()).isNull();
		assertThat(opening.payCurrency()).isNull();
		assertThat(opening.payInterval()).isNull();
		assertThat(opening.country()).isNull();
		assertThat(opening.commitment()).isNull();
		assertThat(opening.team()).isNull();
		assertThat(opening.workplaceType()).isEqualTo("unspecified");
		assertThat(opening.allLocations()).containsExactly("London");
	}

	@Test
	void retriesAGatewayErrorWithoutTellingTheRunOfARefusal() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, (attempt == 1) ? 500 : (attempt == 2) ? 504 : 200, (attempt < 3) ? "" : BOARD);
		});

		AtomicInteger refusals = new AtomicInteger();
		assertThat(reader().read("finix", RegionEnum.US, refusals::incrementAndGet)).hasSize(1);
		assertThat(attempts).hasValue(3);
		assertThat(refusals).hasValue(0);
	}

	@Test
	void retriesARequestToBackOffAndTellsTheRunOfIt() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, (attempt == 1) ? 502 : (attempt == 2) ? 429 : 200, (attempt < 3) ? "" : BOARD);
		});

		AtomicInteger refusals = new AtomicInteger();
		assertThat(reader().read("finix", RegionEnum.US, refusals::incrementAndGet)).hasSize(1);
		assertThat(attempts).hasValue(3);
		assertThat(refusals).hasValue(1);
	}

	@Test
	void waitsAsLongAsARefusalAsksWhenItSays() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			if (attempts.incrementAndGet() == 1) {
				exchange.getResponseHeaders().add("Retry-After", "1");
				respondWith(exchange, 503, "");
				return;
			}
			respondWith(exchange, 200, BOARD);
		});

		Instant before = Instant.now();
		reader().read("finix", RegionEnum.US, () -> {});

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
	}

	@Test
	void givesUpOnABoardTheAtsWillNotServe() {
		serve((exchange) -> respondWith(exchange, 404, """
				{"ok":false,"error":"Document not found"}"""));

		assertThatThrownBy(() -> reader().read("finix", RegionEnum.US, () -> {})).isInstanceOf(IOException.class)
			.hasMessageContaining("404");
	}

	@Test
	void refusesAnAnswerItCannotRead() {
		serve((exchange) -> respondWith(exchange, 200, "not json"));

		assertThatThrownBy(() -> reader().read("finix", RegionEnum.US, () -> {})).isInstanceOf(IOException.class)
			.hasMessageContaining("finix");
	}

	private void respondWith(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/", (exchange) -> {
			URI uri = exchange.getRequestURI();
			this.queries.add(uri.getPath() + "?" + uri.getQuery());
			handler.handle(exchange);
		});
	}

	private LeverVacancyReaderAdapter reader() {
		String server = "http://localhost:" + this.server.getAddress().getPort();
		return new LeverVacancyReaderAdapter(
				Map.of(RegionEnum.US, URI.create(server + "/us/"), RegionEnum.EU, URI.create(server + "/eu/")),
				"oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3, Duration.ofMillis(10),
				Duration.ofSeconds(5));
	}

}
