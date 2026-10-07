package oneprofile.backend.workers.stoplist;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exercised against a stub server rather than {@code api.typesafe.ai}. */
class JevAdapterTest {

	private static final String ANSWER = """
			{"model":"jev-1.13.0","answers":{"technology":{"choice":"no","probabilities":{"yes":0.1,"no":0.9}}},\
			"usage":{"input_tokens":180}}""";

	private HttpServer server;

	private final List<JsonNode> bodies = new ArrayList<>();

	private final List<String> authorizations = new ArrayList<>();

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
	void asksThePinnedModelWhetherTheWordCouldNameATechnologyAndReadsItsProbabilityOfNo() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, ANSWER));

		assertThat(jev().ask("experience")).isEqualTo(new JevAnswer("no", 0.9, 0.1, "jev-1.13.0", 180));
		assertThat(this.authorizations).containsExactly("Bearer secret");
		assertThat(this.userAgents).containsExactly("oneprofile-test");
		JsonNode body = this.bodies.getFirst();
		assertThat(body.get("model").asString()).isEqualTo("jev-1.13.0");
		assertThat(body.get("state").get("word").asString()).isEqualTo("experience");
		JsonNode question = body.get("questions").get("technology");
		assertThat(question.get("type").asString()).isEqualTo("choice");
		assertThat(question.get("criteria").propertyNames()).containsExactlyInAnyOrder("yes", "no");
	}

	@Test
	void retriesWhenJevIsRateLimitedOrOverloaded() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			int attempt = attempts.incrementAndGet();
			respondWith(exchange, (attempt == 1) ? 429 : (attempt == 2) ? 529 : 200, ANSWER);
		});

		assertThat(jev().ask("experience").no()).isEqualTo(0.9);
		assertThat(attempts).hasValue(3);
	}

	@Test
	void failsOnAnyOtherRefusalWithoutRetrying() {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			attempts.incrementAndGet();
			respondWith(exchange, 402, "{\"error\":\"out of credit\"}");
		});

		assertThatThrownBy(() -> jev().ask("experience")).isInstanceOf(IOException.class).hasMessageContaining("402");
		assertThat(attempts).hasValue(1);
	}

	@Test
	void refusesAnAnswerFromAModelOtherThanThePinnedOne() {
		serve((exchange) -> respondWith(exchange, 200, ANSWER.replace("jev-1.13.0", "jev-2.0.0")));

		assertThatThrownBy(() -> jev().ask("experience")).isInstanceOf(IOException.class)
			.hasMessageContaining("jev-2.0.0");
	}

	private JevAdapter jev() {
		return new JevAdapter(URI.create("http://localhost:" + this.server.getAddress().getPort() + "/v1/systemone"),
				() -> "secret", "oneprofile-test", 3, Duration.ofMillis(1));
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/v1/systemone", (exchange) -> {
			this.authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
			this.userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
			this.bodies.add(JsonMapper.builder().build().readTree(exchange.getRequestBody()));
			handler.handle(exchange);
		});
	}

	private static void respondWith(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

}
