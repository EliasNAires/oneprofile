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
import oneprofile.backend.storage.company.RegionEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code api.ashbyhq.com}. */
class AshbyBoardReaderAdapterTest {

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
	void readsABoardWithOpeningsAsActiveAndBringsNoNameSinceAshbyGivesNone() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, """
				{"apiVersion":"1","jobs":[{"id":"34413f8d-26bf-4bbc-8ade-eb309a0e2245","title":"Engineer"}]}"""));

		assertThat(reader().read("ramp", RegionEnum.US)).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, null));
		assertThat(this.queries).containsExactly("/posting-api/job-board/ramp?includeCompensation=true");
	}

	@Test
	void readsABoardWithoutOpeningsAsEmpty() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, """
				{"jobs":[],"apiVersion":"1"}"""));

		assertThat(reader().read("deel", RegionEnum.US)).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
	}

	@Test
	void readsASlugWithNoBoardAsNotFoundWithoutRetrying() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			attempts.incrementAndGet();
			respondWith(exchange, 404, "Not Found");
		});

		assertThat(reader().read("gone", RegionEnum.US)).isEqualTo(new BoardReading(BoardStatusEnum.NOT_FOUND, null));
		assertThat(attempts).hasValue(1);
	}

	@Test
	void retriesWhenTheServiceAsksForABackOff() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, attempt < 3 ? 429 : 200, attempt < 3 ? "" : "{\"jobs\":[]}");
		});

		assertThat(reader().read("deel", RegionEnum.US)).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
		assertThat(attempts).hasValue(3);
	}

	@Test
	void refusesAnAnswerItCannotRead() {
		serve((exchange) -> respondWith(exchange, 200, "not json"));

		assertThatThrownBy(() -> reader().read("ramp", RegionEnum.US)).isInstanceOf(IOException.class)
			.hasMessageContaining("ramp");
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

	private AshbyBoardReaderAdapter reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new AshbyBoardReaderAdapter(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10), Duration.ofSeconds(5));
	}

}
