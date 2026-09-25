package oneprofile.backend.workers.probe;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.company.BoardStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code boards-api.greenhouse.io}. */
class GreenhouseBoardReaderAdapterTest {

	private static final String WITH_OPENINGS = """
			{"jobs":[{"id":1,"title":"Abuse Investigator","company_name":"Stripe"},
			{"id":2,"title":"Backend Engineer","company_name":"Stripe"}],"meta":{"total":2}}""";

	private static final String WITHOUT_OPENINGS = """
			{"jobs":[],"meta":{"total":0}}""";

	private HttpServer server;

	private final List<String> paths = new ArrayList<>();

	private final List<String> userAgents = new ArrayList<>();

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
	void readsABoardWithOpeningsAsActiveAndTakesItsNameFromTheResponse() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, WITH_OPENINGS));

		assertThat(reader().read("stripe")).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"));
		assertThat(this.paths).containsExactly("/v1/boards/stripe/jobs");
		assertThat(this.userAgents).allMatch((agent) -> agent.contains("oneprofile"));
	}

	@Test
	void readsABoardWithoutOpeningsAsEmptyAndBringsNoName() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, WITHOUT_OPENINGS));

		assertThat(reader().read("stripe")).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
	}

	@Test
	void readsASlugWithNoBoardAsNotFoundWithoutRetrying() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			attempts.incrementAndGet();
			respondWith(exchange, 404, "");
		});

		assertThat(reader().read("stripe")).isEqualTo(new BoardReading(BoardStatusEnum.NOT_FOUND, null));
		assertThat(attempts).hasValue(1);
	}

	@Test
	void retriesWhenTheServiceAsksForABackOff() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, switch (attempt) {
				case 1 -> 503;
				case 2 -> 429;
				default -> 200;
			}, attempt < 3 ? "" : WITH_OPENINGS);
		});

		assertThat(reader().read("stripe")).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"));
		assertThat(attempts).hasValue(3);
	}

	@Test
	void givesUpOnceTheAttemptsAreSpent() {
		serve((exchange) -> respondWith(exchange, 503, ""));

		assertThatThrownBy(() -> reader().read("stripe")).isInstanceOf(IOException.class).hasMessageContaining("503");
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
			this.paths.add(exchange.getRequestURI().getPath());
			this.userAgents.add(String.valueOf(exchange.getRequestHeaders().getFirst("User-Agent")));
			handler.handle(exchange);
		});
	}

	private GreenhouseBoardReaderAdapter reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new GreenhouseBoardReaderAdapter(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10));
	}

}
