package oneprofile.backend.workers.discovery;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code data.commoncrawl.org}. */
class HttpRangeReaderAdapterTest {

	private static final byte[] BODY = "the-block-bytes".getBytes(StandardCharsets.UTF_8);

	private HttpServer server;

	private final List<String> ranges = new ArrayList<>();

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
	void asksForTheRangeAndIdentifiesItself() throws IOException {
		serve((exchange) -> respondWithRange(exchange, 206));

		byte[] block = reader().read("cc-index/cluster.idx", 0, BODY.length);

		assertThat(block).isEqualTo(BODY);
		assertThat(this.ranges).containsExactly("bytes=0-%d".formatted(BODY.length - 1));
		assertThat(this.userAgents).allMatch((agent) -> agent.contains("oneprofile"));
	}

	@Test
	void reportsTheSizeOfTheWholeFile() throws IOException {
		serve((exchange) -> {
			exchange.getResponseHeaders().add("Content-Length", "103401192");
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});

		assertThat(reader().size("cc-index/cluster.idx")).isEqualTo(103401192L);
	}

	@Test
	void retriesWhenTheServiceAsksForABackOff() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int status = switch (attempts.incrementAndGet()) {
				case 1 -> 503;
				case 2 -> 429;
				default -> 206;
			};
			if (status == 206) {
				respondWithRange(exchange, 206);
				return;
			}
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
		});

		byte[] block = reader().read("cc-index/cluster.idx", 0, BODY.length);

		assertThat(block).isEqualTo(BODY);
		assertThat(attempts).hasValue(3);
	}

	@Test
	void givesUpOnceTheAttemptsAreSpent() {
		serve((exchange) -> {
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});

		assertThatThrownBy(() -> reader().read("cc-index/cluster.idx", 0, BODY.length))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("503");
	}

	@Test
	void refusesABodyShorterThanTheRangeItAskedFor() {
		serve((exchange) -> respondWith(exchange, 206, "the-block".getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> reader().read("cc-index/cluster.idx", 0, BODY.length))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("got 9");
	}

	@Test
	void refusesABodyThatIsNotTheRangeItAskedFor() {
		serve((exchange) -> respondWithRange(exchange, 200));

		assertThatThrownBy(() -> reader().read("cc-index/cluster.idx", 0, BODY.length))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("200");
	}

	private void respondWithRange(HttpExchange exchange, int status) throws IOException {
		respondWith(exchange, status, BODY);
	}

	private void respondWith(HttpExchange exchange, int status, byte[] body) throws IOException {
		exchange.getResponseHeaders().add("Content-Range", "bytes 0-%d/%d".formatted(BODY.length - 1, BODY.length));
		exchange.sendResponseHeaders(status, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/", (exchange) -> {
			this.ranges.add(String.valueOf(exchange.getRequestHeaders().getFirst("Range")));
			this.userAgents.add(String.valueOf(exchange.getRequestHeaders().getFirst("User-Agent")));
			handler.handle(exchange);
		});
	}

	private HttpRangeReaderAdapter reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new HttpRangeReaderAdapter(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10));
	}

}
