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
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.SecondaryLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code api.ashbyhq.com}. */
class AshbyVacancyReaderAdapterTest {

	/** One opening as Ashby publishes it, cut down to the fields a sweep reads and a few it does not. */
	private static final String BOARD = """
			{"apiVersion":"1","jobs":[{"id":"34413f8d-26bf-4bbc-8ade-eb309a0e2245",
			"title":"Security Engineer, Cloud","department":"Engineering","team":"Backend",
			"employmentType":"FullTime","location":"New York, NY (HQ)",
			"secondaryLocations":[
			{"location":"Remote (Canada)","address":{"postalAddress":{"addressCountry":"Canada"}}},
			{"location":"Anywhere","address":{"postalAddress":{}}}],
			"publishedAt":"2026-04-07T17:12:35.753+00:00","isListed":true,"isRemote":true,
			"workplaceType":"Hybrid",
			"address":{"postalAddress":{"addressRegion":"NY","addressCountry":"USA"}},
			"jobUrl":"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245",
			"descriptionHtml":"<h1><strong>About Ramp</strong></h1><p>Secure&nbsp;the cloud.</p>",
			"descriptionPlain":"ABOUT RAMP\\n\\nSecure the cloud.",
			"compensation":{"compensationTierSummary":"$211.4K – $290.6K • Offers Equity",
			"summaryComponents":[
			{"compensationType":"EquityPercentage","interval":"NONE","currencyCode":null,"minValue":null,"maxValue":null},
			{"compensationType":"Salary","interval":"1 YEAR","currencyCode":"USD","minValue":211400,"maxValue":290600}]}}]}""";

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
	void readsEveryOpeningOfABoardWithItsStructuredFieldsAskingForItsCompensation() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, BOARD));

		assertThat(reader().read("ramp")).containsExactly(new PublishedVacancy("34413f8d-26bf-4bbc-8ade-eb309a0e2245",
				"Security Engineer, Cloud", "New York, NY (HQ)", "Engineering", "# About Ramp\nSecure the cloud.", null,
				"https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245", 21140000L, 29060000L, "USD", null,
				"1 YEAR", Instant.parse("2026-04-07T17:12:35.753Z"), null, "Hybrid", "USA",
				List.of(new SecondaryLocation("Remote (Canada)", "Canada"), new SecondaryLocation("Anywhere", null))));
		assertThat(this.queries).containsExactly("/posting-api/job-board/ramp?includeCompensation=true");
	}

	@Test
	void keepsAnHourlyRateInCentsAndQuotedHourlyRatherThanConvertingIt() throws IOException {
		serveOpening("""
				"compensation":{"summaryComponents":[
				{"compensationType":"Salary","interval":"1 HOUR","currencyCode":"EUR","minValue":45.5,"maxValue":60}]}""");

		PublishedVacancy opening = reader().read("acme").get(0);
		assertThat(opening.payMinCents()).isEqualTo(4550L);
		assertThat(opening.payMaxCents()).isEqualTo(6000L);
		assertThat(opening.payCurrency()).isEqualTo("EUR");
		assertThat(opening.payInterval()).isEqualTo("1 HOUR");
	}

	@Test
	void bringsNothingWhereTheBoardPublishedNoPayWorkModeAddressOrDescription() throws IOException {
		serveOpening("""
				"secondaryLocations":[],"descriptionHtml":"",
				"compensation":{"summaryComponents":[
				{"compensationType":"EquityPercentage","interval":"NONE","currencyCode":null,"minValue":null,"maxValue":null}]}""");

		PublishedVacancy opening = reader().read("acme").get(0);
		assertThat(opening.description()).isNull();
		assertThat(opening.department()).isNull();
		assertThat(opening.payMinCents()).isNull();
		assertThat(opening.payMaxCents()).isNull();
		assertThat(opening.payCurrency()).isNull();
		assertThat(opening.payInterval()).isNull();
		assertThat(opening.workplaceType()).isNull();
		assertThat(opening.addressCountry()).isNull();
		assertThat(opening.secondaryLocations()).isEmpty();
	}

	@Test
	void holdsATitleAsTheBoardWroteItEvenWhenItWroteNone() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, """
				{"jobs":[{"id":"dcae2c8b-8362-48c6-854e-d1e0ea9a3434","title":"",
				"jobUrl":"https://jobs.ashbyhq.com/acme/dcae2c8b-8362-48c6-854e-d1e0ea9a3434"}]}"""));

		assertThat(reader().read("acme").get(0).title()).isEmpty();
	}

	@Test
	void marksNothingBoilerplateSinceAshbysHtmlHasNoBoilerplateClasses() throws IOException {
		serveOpening("""
				"descriptionHtml":"<div class=\\"c-link\\"><h2>About us</h2><ul><li>Remote</li></ul></div>\"""");

		assertThat(reader().read("acme").get(0).description()).isEqualTo("""
				# About us
				- Remote""");
	}

	@Test
	void retriesWhenTheServiceAsksForABackOff() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, attempt < 3 ? 429 : 200, attempt < 3 ? "" : BOARD);
		});

		assertThat(reader().read("ramp")).hasSize(1);
		assertThat(attempts).hasValue(3);
	}

	@Test
	void givesUpOnABoardTheAtsWillNotServe() {
		serve((exchange) -> respondWith(exchange, 404, "Not Found"));

		assertThatThrownBy(() -> reader().read("ramp")).isInstanceOf(IOException.class).hasMessageContaining("404");
	}

	@Test
	void refusesAnAnswerItCannotRead() {
		serve((exchange) -> respondWith(exchange, 200, "not json"));

		assertThatThrownBy(() -> reader().read("ramp")).isInstanceOf(IOException.class).hasMessageContaining("ramp");
	}

	private void serveOpening(String fields) {
		String board = """
				{"jobs":[{"id":"0f0e0d0c-0000-4000-8000-000000000001","title":"Engineer",
				"jobUrl":"https://jobs.ashbyhq.com/acme/0f0e0d0c-0000-4000-8000-000000000001",
				"publishedAt":"2026-04-07T17:12:35.753+00:00",%s}]}""".formatted(fields);
		serve((exchange) -> respondWith(exchange, 200, board));
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

	private AshbyVacancyReaderAdapter reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new AshbyVacancyReaderAdapter(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10), Duration.ofSeconds(5));
	}

}
