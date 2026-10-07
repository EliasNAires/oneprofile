package oneprofile.backend.workers.stoplist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import oneprofile.backend.workers.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks Jev (ADR-0007) through TypeSafe's API, as {@code scripts/jev.py} does: one request per word,
 * retrying when Jev is rate limited (429) or overloaded (529). The question is about technology
 * broadly, never the skill criterion, so its answers outlive every revision of it.
 * <p>
 * The key is read from the repository's {@code .env} at the first question, so the application
 * starts without one.
 */
@Component
public class JevAdapter implements JevPort {

	private static final URI SYSTEM_ONE = URI.create("https://api.typesafe.ai/v1/systemone");

	private static final Path ENV = Path.of(".env");

	private static final int ATTEMPTS = 8;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	private static final Duration TIMEOUT = Duration.ofSeconds(120);

	private static final String INSTRUCTIONS = "Could `word` name a technology, product or software tool in any "
			+ "context?";

	private static final Map<String, String> CRITERIA = Map.of("yes",
			"Yes: in some context it names a technology, product or software tool.", "no",
			"No: in no context does it name a technology, product or software tool.");

	private final HttpClient http = HttpClient.newHttpClient();

	private final ObjectMapper json = JsonMapper.builder().build();

	private final URI url;

	private final Supplier<String> key;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	/** Asks TypeSafe's API with the key in the repository's {@code .env}. */
	@Autowired
	public JevAdapter() {
		this(SYSTEM_ONE, JevAdapter::envKey, UserAgent.VALUE, ATTEMPTS, BACK_OFF);
	}

	/**
	 * @param url where questions are posted
	 * @param key the API key, asked for at each question
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a question is posted before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one, unless
	 * Jev says how long
	 */
	JevAdapter(URI url, Supplier<String> key, String userAgent, int attempts, Duration backOff) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A question has to be posted at least once");
		}
		this.url = url;
		this.key = key;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
	}

	@Override
	public JevAnswer ask(String word) throws IOException {
		String body = this.json.writeValueAsString(Map.of("model", MODEL, "state", Map.of("word", word), "questions",
				Map.of("technology", Map.of("type", "choice", "instructions", INSTRUCTIONS, "criteria", CRITERIA))));
		HttpRequest request = HttpRequest.newBuilder(this.url)
			.header("Authorization", "Bearer " + this.key.get())
			.header("Content-Type", "application/json")
			.header("User-Agent", this.userAgent)
			.timeout(TIMEOUT)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return answer(send(request, word), word);
	}

	private JevAnswer answer(String body, String word) throws IOException {
		JsonNode response;
		try {
			response = this.json.readTree(body);
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable answer for " + word, ex);
		}
		String model = response.path("model").asString("");
		if (!model.equals(MODEL)) {
			throw new IOException("Jev answered as %s, not the pinned %s".formatted(model, MODEL));
		}
		JsonNode technology = response.path("answers").path("technology");
		JsonNode probabilities = technology.path("probabilities");
		return new JevAnswer(technology.path("choice").asString(), probabilities.path("no").asDouble(),
				probabilities.path("yes").asDouble(), model, response.path("usage").path("input_tokens").asLong());
	}

	private String send(HttpRequest request, String word) throws IOException {
		for (int attempt = 0;; attempt++) {
			HttpResponse<String> response;
			try {
				response = this.http.send(request, BodyHandlers.ofString());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while asking about " + word, ex);
			}
			int status = response.statusCode();
			if (status == 200) {
				return response.body();
			}
			if ((status != 429 && status != 529) || attempt == this.attempts - 1) {
				throw new IOException("Jev answered %d about %s: %s".formatted(status, word, response.body()));
			}
			waitBefore(attempt, response.headers().firstValue("Retry-After").orElse(null));
		}
	}

	private void waitBefore(int attempt, String retryAfter) throws IOException {
		Duration wait = (retryAfter != null && retryAfter.matches("\\d+(\\.\\d+)?"))
				? Duration.ofMillis((long) (Double.parseDouble(retryAfter) * 1000))
				: this.backOff.multipliedBy(1L << attempt);
		try {
			Thread.sleep(wait);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while backing off", ex);
		}
	}

	/** TYPESAFE_API_KEY from the repository's {@code .env}. Never logged. */
	private static String envKey() {
		try {
			return Files.readAllLines(ENV)
				.stream()
				.map((line) -> line.split("=", 2))
				.filter((pair) -> pair.length == 2 && pair[0].strip().equals("TYPESAFE_API_KEY"))
				.map((pair) -> pair[1].strip().replaceAll("^[\"']|[\"']$", ""))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("TYPESAFE_API_KEY is not in .env"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
