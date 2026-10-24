package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

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
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code web.archive.org}. */
class HttpWaybackCdxAdapterTest {

	private static final String CHUNK = """
			[["original"],
			["https://boards.greenhouse.io/acme"],
			[],
			["eJxFzEsKgCAQ"]]
			""";

	private static final String REFUSAL = "<html><head><title>Internet Archive: Temporarily Offline</title></head></html>";

	private HttpServer server;

	private final List<String> queries = new ArrayList<>();

	private final List<String> userAgents = new ArrayList<>();

	@BeforeEach
	void startServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress(0), 0);
		this.server.setExecutor(Executors.newCachedThreadPool());
		this.server.start();
	}

	@AfterEach
	void stopServer() {
		this.server.stop(0);
	}

	@Test
	void asksForEveryCaptureUnderThePrefixAsJsonAndIdentifiesItself() throws IOException {
		serve((exchange) -> respond(exchange, 200, CHUNK));

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.empty())).isEqualTo(CHUNK);
		assertThat(this.queries).containsExactly(
				"url=boards.greenhouse.io%2F&matchType=prefix&limit=25000&showResumeKey=true&output=json&fl=original");
		assertThat(this.userAgents).containsExactly("oneprofile-test");
	}

	@Test
	void asksForEveryCaptureOnTheDomainForAWildcardHost() throws IOException {
		serve((exchange) -> respond(exchange, 200, CHUNK));

		cdx().chunk("*.myworkdayjobs.com/", Optional.empty());

		assertThat(this.queries).containsExactly(
				"url=myworkdayjobs.com&matchType=domain&limit=25000&showResumeKey=true&output=json&fl=original");
	}

	@Test
	void continuesFromTheResumeKeyThePreviousChunkClosedWith() throws IOException {
		serve((exchange) -> respond(exchange, 200, CHUNK));

		cdx().chunk("jobs.ashbyhq.com/", Optional.of("eJxF+zEs/KgCAQ=="));

		assertThat(this.queries).singleElement().asString().endsWith("&resumeKey=eJxF%2BzEs%2FKgCAQ%3D%3D");
	}

	@Test
	void asksAgainAfterTheTemporarilyOfflinePage() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			if (attempts.incrementAndGet() == 1) {
				respond(exchange, 503, REFUSAL);
				return;
			}
			respond(exchange, 200, CHUNK);
		});

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.of("eJxFzEsKgCAQ"))).isEqualTo(CHUNK);
		assertThat(this.queries).hasSize(2).containsOnly(this.queries.getFirst());
	}

	@Test
	void asksAgainAfterTheGatewayTimedOut() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> respond(exchange, attempts.incrementAndGet() == 1 ? 504 : 200, CHUNK));

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.empty())).isEqualTo(CHUNK);
		assertThat(attempts).hasValue(2);
	}

	@Test
	void asksAgainWhenTheAnswerWasCutShort() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> respond(exchange, 200,
				attempts.incrementAndGet() == 1 ? "[[\"original\"],\n[\"https://boards.greenhouse.io/acm" : CHUNK));

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.empty())).isEqualTo(CHUNK);
		assertThat(attempts).hasValue(2);
	}

	@Test
	void asksAgainWhenTheAnswerWasCutInsideAUrlEndingAsAChunkCloses() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> respond(exchange, 200,
				attempts.incrementAndGet() == 1 ? "[[\"original\"],\n[\"https://boards.greenhouse.io/a?x=[[1]]" : CHUNK));

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.empty())).isEqualTo(CHUNK);
		assertThat(attempts).hasValue(2);
	}

	@Test
	void asksAgainWhenAnEmptyAnswerFollowsAResumeKey() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> respond(exchange, 200, attempts.incrementAndGet() == 1 ? "" : CHUNK));

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.of("eJxFzEsKgCAQ"))).isEqualTo(CHUNK);
		assertThat(attempts).hasValue(2);
	}

	@Test
	void asksAgainWhenTheAnswerStallsBeforeItCloses() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			if (attempts.incrementAndGet() == 1) {
				exchange.sendResponseHeaders(200, 0);
				exchange.getResponseBody().write("[[\"original\"],".getBytes(StandardCharsets.UTF_8));
				exchange.getResponseBody().flush();
				try {
					Thread.sleep(2000);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				exchange.close();
				return;
			}
			respond(exchange, 200, CHUNK);
		});

		assertThat(cdx().chunk("boards.greenhouse.io/", Optional.empty())).isEqualTo(CHUNK);
		assertThat(attempts).hasValue(2);
	}

	@Test
	void takesAnEmptyAnswerAsNothingCaptured() throws IOException {
		serve((exchange) -> respond(exchange, 200, "[]\n"));

		assertThat(cdx().chunk("boards.eu.greenhouse.io/", Optional.empty())).isEqualTo("[]\n");
		assertThat(this.queries).hasSize(1);
	}

	@Test
	void givesTheReadUpOnceTheAttemptsAreSpent() {
		serve((exchange) -> respond(exchange, 503, REFUSAL));

		assertThatIOException().isThrownBy(() -> cdx().chunk("boards.greenhouse.io/", Optional.empty()))
			.withMessageContaining("503");
		assertThat(this.queries).hasSize(3);
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		if (bytes.length > 0) {
			exchange.getResponseBody().write(bytes);
		}
		exchange.close();
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/cdx/search/cdx", (exchange) -> {
			this.queries.add(exchange.getRequestURI().getRawQuery());
			this.userAgents.add(String.valueOf(exchange.getRequestHeaders().getFirst("User-Agent")));
			handler.handle(exchange);
		});
	}

	private HttpWaybackCdxAdapter cdx() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/cdx/search/cdx");
		return new HttpWaybackCdxAdapter(base, "oneprofile-test", 3, Duration.ofMillis(10), Duration.ofMillis(25),
				Duration.ofMillis(500));
	}

}
