package oneprofile.backend.sweep;

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
import oneprofile.backend.vacancy.PublishedVacancy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code boards-api.greenhouse.io}. */
class GreenhouseVacancyReaderTest {

	/** The description is escaped twice, as Greenhouse publishes it. */
	private static final String BOARD = """
			{"jobs":[{"id":4001,"title":"Backend Engineer",
			"absolute_url":"https://job-boards.greenhouse.io/stripe/jobs/4001",
			"location":{"name":"Remote - Americas"},
			"departments":[{"id":7,"name":"Engineering"}],
			"content":"&lt;div&gt;Ship&amp;nbsp;payments.&lt;/div&gt;",
			"first_published":"2026-09-01T10:00:00-04:00",
			"updated_at":"2026-09-18T12:30:00-04:00",
			"pay_input_ranges":[{"min_cents":15000000,"max_cents":20000000,
			"currency_type":"USD","title":"Annual Salary"}],
			"company_name":"Stripe"}],"meta":{"total":1}}""";

	private static final String WITHOUT_PAY_OR_DESCRIPTION = """
			{"jobs":[{"id":4002,"title":"Designer",
			"absolute_url":"https://job-boards.greenhouse.io/stripe/jobs/4002",
			"location":{"name":"Remote"},"departments":[],"content":"",
			"first_published":"2026-09-01T10:00:00-04:00",
			"updated_at":"2026-09-18T12:30:00-04:00",
			"pay_input_ranges":[],"company_name":"Stripe"}],"meta":{"total":1}}""";

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
	void readsEveryOpeningOfABoardAskingForItsContentAndPay() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, BOARD));

		assertThat(reader().read("stripe")).containsExactly(new PublishedVacancy(4001L, "Backend Engineer",
				"Remote - Americas", "Engineering", "Ship payments.",
				"https://job-boards.greenhouse.io/stripe/jobs/4001", 15000000L, 20000000L, "USD", "Annual Salary",
				Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z")));
		assertThat(this.queries).containsExactly("/v1/boards/stripe/jobs?content=true&pay_transparency=true");
	}

	@Test
	void bringsNothingWhereTheBoardPublishedNoDescriptionPayOrDepartment() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, WITHOUT_PAY_OR_DESCRIPTION));

		PublishedVacancy designer = reader().read("stripe").get(0);
		assertThat(designer.description()).isNull();
		assertThat(designer.department()).isNull();
		assertThat(designer.payMinCents()).isNull();
		assertThat(designer.payMaxCents()).isNull();
		assertThat(designer.payCurrency()).isNull();
		assertThat(designer.payTitle()).isNull();
	}

	@Test
	void retriesWhenTheServiceAsksForABackOff() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, attempt < 3 ? 503 : 200, attempt < 3 ? "" : BOARD);
		});

		assertThat(reader().read("stripe")).hasSize(1);
		assertThat(attempts).hasValue(3);
	}

	@Test
	void givesUpOnABoardTheAtsWillNotServe() {
		serve((exchange) -> respondWith(exchange, 404, ""));

		assertThatThrownBy(() -> reader().read("stripe")).isInstanceOf(IOException.class).hasMessageContaining("404");
	}

	@Test
	void refusesAnAnswerItCannotRead() {
		serve((exchange) -> respondWith(exchange, 200, "not json"));

		assertThatThrownBy(() -> reader().read("stripe")).isInstanceOf(IOException.class)
			.hasMessageContaining("stripe");
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

	private GreenhouseVacancyReader reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new GreenhouseVacancyReader(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10), Duration.ofSeconds(5));
	}

}
