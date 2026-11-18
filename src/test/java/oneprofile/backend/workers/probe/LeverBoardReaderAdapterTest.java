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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.RegionEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercised against a stub server rather than {@code api.lever.co} and {@code api.eu.lever.co}: each
 * region's API is a path of its own on it.
 */
class LeverBoardReaderAdapterTest {

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
	void asksTheApiOfTheCompanysRegionForOneOpeningAndReadsABoardWithOpeningsAsActive() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, """
				[{"id":"5f1c2a4e-0d4f-4c1e-9d0a-1b2c3d4e5f60","text":"Game Developer"}]"""));

		assertThat(reader().read("kwalee", RegionEnum.EU, () -> {})).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, null));
		assertThat(reader().read("finix", RegionEnum.US, () -> {})).isEqualTo(new BoardReading(BoardStatusEnum.ACTIVE, null));
		assertThat(this.queries).containsExactly("/eu/v0/postings/kwalee?mode=json&limit=1",
				"/us/v0/postings/finix?mode=json&limit=1");
	}

	@Test
	void readsABoardWithoutOpeningsAsEmpty() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, "[]"));

		assertThat(reader().read("nrwl", RegionEnum.US, () -> {})).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
	}

	@Test
	void readsASlugWithNoBoardInItsRegionAsNotFoundWithoutRetryingOrAskingTheOtherRegion() throws IOException {
		serve((exchange) -> respondWith(exchange, 404, """
				{"ok":false,"error":"Document not found"}"""));

		assertThat(reader().read("kwalee", RegionEnum.US, () -> {})).isEqualTo(new BoardReading(BoardStatusEnum.NOT_FOUND, null));
		assertThat(this.queries).containsExactly("/us/v0/postings/kwalee?mode=json&limit=1");
	}

	@Test
	void retriesAGatewayErrorWithoutTellingTheRunOfARefusal() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, (attempt == 1) ? 500 : (attempt == 2) ? 504 : 200, (attempt < 3) ? "" : "[]");
		});

		AtomicInteger refusals = new AtomicInteger();
		assertThat(reader().read("finix", RegionEnum.US, refusals::incrementAndGet)).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
		assertThat(attempts).hasValue(3);
		assertThat(refusals).hasValue(0);
	}

	@Test
	void retriesARequestToBackOffAndTellsTheRunOfIt() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, (attempt == 1) ? 502 : (attempt == 2) ? 429 : 200, (attempt < 3) ? "" : "[]");
		});

		AtomicInteger refusals = new AtomicInteger();
		assertThat(reader().read("finix", RegionEnum.US, refusals::incrementAndGet)).isEqualTo(new BoardReading(BoardStatusEnum.EMPTY, null));
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
			respondWith(exchange, 200, "[]");
		});

		Instant before = Instant.now();
		reader().read("finix", RegionEnum.US, () -> {});

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
	}

	@Test
	void givesUpWhenTheApiKeepsFailing() {
		serve((exchange) -> respondWith(exchange, 502, ""));

		assertThatThrownBy(() -> reader().read("finix", RegionEnum.US, () -> {})).isInstanceOf(IOException.class)
			.hasMessageContaining("502");
	}

	@Test
	void refusesAnAnswerItCannotRead() {
		serve((exchange) -> respondWith(exchange, 200, "{\"ok\":true}"));

		assertThatThrownBy(() -> reader().read("finix", RegionEnum.US, () -> {})).isInstanceOf(IOException.class)
			.hasMessageContaining("finix");
	}

	/** Names come from the hosted board page in the company's own region. */
	@Test
	void readsTheNameFromEachRegionsBoardPage() throws IOException {
		serve((exchange) -> {
			assertThat(exchange.getRequestMethod()).isEqualTo("GET");
			assertThat(exchange.getRequestHeaders().getFirst("User-Agent"))
				.isEqualTo("oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)");
			String name = exchange.getRequestURI().getPath().contains("/eu/") ? "Kwalee" : "Palantir Technologies";
			respondWith(exchange, 200, "<title> " + name + " </title><meta property=\"og:title\" content=\"" + name + " jobs\">");
		});
		assertThat(reader().name("palantir", RegionEnum.US, () -> {})).isEqualTo("Palantir Technologies");
		assertThat(reader().name("kwalee", RegionEnum.EU, () -> {})).isEqualTo("Kwalee");
		assertThat(this.queries).containsExactly("/pages/us/palantir?null", "/pages/eu/kwalee?null");
	}

	/** Named and numeric entities are decoded once in both title and metadata. */
	@Test
	void unescapesTheNameAndItsMatchingMetadata() throws IOException {
		serve((exchange) -> respondWith(exchange, 200,
				"<title>A &amp; B &#39;C&#39; &quot;D&quot; &lt;E&gt; &#65;&#x1F680;</title>"
				+ "<meta content='A &amp; B &#39;C&#39; &quot;D&quot; &lt;E&gt; &#65;&#x1F680; jobs' property='og:title'>"));
		assertThat(reader().name("company", RegionEnum.US, () -> {})).isEqualTo("A & B 'C' \"D\" <E> A🚀");
	}

	/** An empty board's hosted page answers 404. */
	@Test
	void readsNoNameFromAMissingPageWithoutRetrying() throws IOException {
		serve((exchange) -> respondWith(exchange, 404, "Not Found"));
		assertThat(reader().name("empty", RegionEnum.US, () -> {})).isNull();
		assertThat(this.queries).hasSize(1);
	}

	/** A title alone cannot establish that a page names this company's board. */
	@ParameterizedTest
	@ValueSource(strings = {
			"<title>Not found – 404 error</title>",
			"<title>Company</title><meta property=\"og:title\" content=\"Other jobs\">",
			"<title> </title><meta property=\"og:title\" content=\" jobs\">" })
	void readsNoNameWithoutAMatchingNonblankTitle(String body) throws IOException {
		serve((exchange) -> respondWith(exchange, 200, body));
		assertThat(reader().name("company", RegionEnum.US, () -> {})).isNull();
	}

	/** Gateway failures are retried without slowing the run; refusals do slow it. */
	@Test
	void retriesNameRequestsWithTheSameRefusalCallbackAsBoards() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		AtomicInteger refusals = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, attempt == 1 ? 502 : attempt == 3 ? 429 : 200,
					"<title>Kwalee</title><meta property=\"og:title\" content=\"Kwalee jobs\">");
		});
		assertThat(reader().name("kwalee", RegionEnum.EU, refusals::incrementAndGet)).isEqualTo("Kwalee");
		assertThat(attempts).hasValue(2);
		assertThat(refusals).hasValue(0);
		assertThat(reader().name("kwalee", RegionEnum.EU, refusals::incrementAndGet)).isEqualTo("Kwalee");
		assertThat(attempts).hasValue(4);
		assertThat(refusals).hasValue(1);
	}

	/** An HTTP failure cannot supply a name. */
	@ParameterizedTest
	@ValueSource(ints = { 403, 502 })
	void throwsWhenTheNamePageKeepsFailing(int status) {
		serve((exchange) -> respondWith(exchange, status, ""));
		assertThatThrownBy(() -> reader().name("kwalee", RegionEnum.EU, () -> {})).isInstanceOf(IOException.class);
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

	private LeverBoardReaderAdapter reader() {
		String server = "http://localhost:" + this.server.getAddress().getPort();
		return new LeverBoardReaderAdapter(
				Map.of(RegionEnum.US, URI.create(server + "/us/"), RegionEnum.EU, URI.create(server + "/eu/")),
				Map.of(RegionEnum.US, URI.create(server + "/pages/us/"), RegionEnum.EU, URI.create(server + "/pages/eu/")),
				"oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3, Duration.ofMillis(10),
				Duration.ofSeconds(5));
	}

}
