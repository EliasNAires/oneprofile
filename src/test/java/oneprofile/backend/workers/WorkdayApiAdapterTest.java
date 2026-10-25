package oneprofile.backend.workers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercised against a stub server rather than {@code *.myworkdayjobs.com}: each board is a path on it,
 * named by its host, tenant and site as Workday's hostname and path name them.
 */
class WorkdayApiAdapterTest {

	private static final String PAGE = """
			{"total":1,"jobPostings":[{"title":"Software Engineer",
			"externalPath":"/job/US-CA-Santa-Clara/Software-Engineer_JR1","locationsText":"US, CA, Santa Clara",
			"postedOn":"Posted Today","bulletFields":["JR1"]}],"facets":[]}""";

	private static final String CHALLENGE = "<!DOCTYPE html><html><title>Just a moment...</title></html>";

	private static final String NOT_FOUND = """
			{"errorCode":"HTTP_422","httpStatus":422,"message":"The tenant or site does not exist"}""";

	private HttpServer server;

	private final List<String> requests = new CopyOnWriteArrayList<>();

	private final AtomicInteger refusals = new AtomicInteger();

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
	void postsForAPageOfTheBoardUnderTheFacetsAppliedAndIdentifiesItself() throws IOException {
		serve((exchange) -> respond(exchange, 200, PAGE));

		WorkdayApiPort.Answer answer = api(new Throttle("The test", 20, 100)).jobs("nvidia.wd5/nvidiaexternalcareersite",
				Map.of("jobFamilyGroup", List.of("0c40f6")), 40, 20, this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(200);
		assertThat(answer.body().path("jobPostings").get(0).path("title").asString()).isEqualTo("Software Engineer");
		assertThat(this.requests).containsExactly("POST /wd5/nvidia/wday/cxs/nvidia/nvidiaexternalcareersite/jobs"
				+ " application/json oneprofile-test {\"appliedFacets\":{\"jobFamilyGroup\":[\"0c40f6\"]},"
				+ "\"limit\":20,\"offset\":40,\"searchText\":\"\"}");
	}

	@Test
	void getsOneVacancyByItsExternalPath() throws IOException {
		serve((exchange) -> respond(exchange, 200, "{\"jobPostingInfo\":{\"title\":\"Software Engineer\"}}"));

		WorkdayApiPort.Answer answer = api(new Throttle("The test", 20, 100)).job("nvidia.wd5/nvidiaexternalcareersite",
				"/job/US-CA-Santa-Clara/Software-Engineer_JR1", this.refusals::incrementAndGet);

		assertThat(answer.body().path("jobPostingInfo").path("title").asString()).isEqualTo("Software Engineer");
		assertThat(this.requests).containsExactly("GET /wd5/nvidia/wday/cxs/nvidia/nvidiaexternalcareersite"
				+ "/job/US-CA-Santa-Clara/Software-Engineer_JR1 null oneprofile-test ");
	}

	@Test
	void answersWithWhatWorkdaySaysOfABoardThatCannotBeRead() throws IOException {
		serve((exchange) -> respond(exchange, 422, NOT_FOUND));

		WorkdayApiPort.Answer answer = api(new Throttle("The test", 20, 100)).jobs("nvidia.wd5/nosuchsite", Map.of(), 0,
				1, this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(422);
		assertThat(this.requests).hasSize(1);
		assertThat(this.refusals).hasValue(0);
	}

	@Test
	void slowsDownAndAsksAgainAfterCloudflaresChallenge() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			if (attempts.incrementAndGet() == 1) {
				exchange.getResponseHeaders().add("cf-mitigated", "challenge");
				respond(exchange, 429, CHALLENGE);
				return;
			}
			respond(exchange, 200, PAGE);
		});
		Throttle throttle = new Throttle("The test", 20, 100);

		WorkdayApiPort.Answer answer = api(throttle).jobs("nvidia.wd5/nvidiaexternalcareersite", Map.of(), 0, 1,
				this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(200);
		assertThat(this.refusals).hasValue(1);
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);
	}

	@Test
	void slowsDownOnceForAChallengeEveryCallerMeetsAndWaitsItOutTogether() throws Exception {
		AtomicLong challengedUntil = new AtomicLong(Long.MIN_VALUE);
		serve((exchange) -> {
			long now = System.nanoTime();
			if (challengedUntil.compareAndSet(Long.MIN_VALUE, now + Duration.ofMillis(300).toNanos())
					|| now - challengedUntil.get() < 0) {
				exchange.getResponseHeaders().add("cf-mitigated", "challenge");
				respond(exchange, 429, CHALLENGE);
				return;
			}
			respond(exchange, 200, PAGE);
		});
		Throttle throttle = new Throttle("The test", 20, 100);
		String base = "http://localhost:%d/%%2$s/%%1$s/wday/cxs/%%1$s/%%3$s/".formatted(this.server.getAddress().getPort());
		WorkdayApiAdapter api = new WorkdayApiAdapter(base, "oneprofile-test", throttle, 4, Duration.ofMillis(400),
				Duration.ofMillis(1), Duration.ofSeconds(5));

		try (ExecutorService callers = Executors.newFixedThreadPool(6)) {
			List<Future<?>> asked = new CopyOnWriteArrayList<>();
			for (int i = 0; i < 6; i++) {
				asked.add(callers.submit(() -> {
					for (int page = 0; page < 10; page++) {
						assertThat(api.jobs("nvidia.wd5/careers", Map.of(), page * 20, 20, this.refusals::incrementAndGet)
							.status()).isEqualTo(200);
					}
					return null;
				}));
			}
			for (Future<?> caller : asked) {
				caller.get();
			}
		}

		assertThat(this.refusals).hasValue(1);
		assertThat(throttle.requestsAtOnce()).isEqualTo(10);
	}

	@Test
	void asksAgainWithoutSlowingDownAfterATenantsOwnLimitOrAGatewayError() throws IOException {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			switch (attempts.incrementAndGet()) {
				case 1 -> respond(exchange, 429, "{\"errorCode\":\"HTTP_429\",\"httpStatus\":429}");
				case 2 -> respond(exchange, 520, "error code: 520");
				case 3 -> respond(exchange, 503, "no healthy upstream");
				default -> respond(exchange, 200, PAGE);
			}
		});
		Throttle throttle = new Throttle("The test", 20, 100);

		WorkdayApiPort.Answer answer = api(throttle).jobs("accenture.wd103/accenturecareers", Map.of(), 0, 1,
				this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(200);
		assertThat(this.refusals).hasValue(0);
		assertThat(throttle.requestsAtOnce()).isEqualTo(20);
	}

	@Test
	void givesUpAfterItsAttempts() {
		serve((exchange) -> respond(exchange, 502, "Bad gateway"));

		assertThatIOException().isThrownBy(() -> api(new Throttle("The test", 20, 100))
			.jobs("amb.wd1/careers", Map.of(), 0, 1, this.refusals::incrementAndGet));
		assertThat(this.requests).hasSize(4);
	}

	@Test
	void failsOnAnAnswerThatIsNotJson() {
		serve((exchange) -> respond(exchange, 200, "<html>maintenance</html>"));

		assertThatIOException().isThrownBy(() -> api(new Throttle("The test", 20, 100))
			.jobs("amb.wd1/careers", Map.of(), 0, 1, this.refusals::incrementAndGet));
	}

	@Test
	void keepsNoMoreRequestsOpenThanItsThrottleAllowsWhoeverAsks() throws Exception {
		AtomicInteger open = new AtomicInteger();
		AtomicInteger mostOpen = new AtomicInteger();
		serve((exchange) -> {
			mostOpen.accumulateAndGet(open.incrementAndGet(), Math::max);
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			open.decrementAndGet();
			respond(exchange, 200, PAGE);
		});
		WorkdayApiAdapter api = api(new Throttle("The test", 2, 100));

		try (ExecutorService callers = Executors.newFixedThreadPool(6)) {
			List<Future<WorkdayApiPort.Answer>> answers = new CopyOnWriteArrayList<>();
			for (int i = 0; i < 6; i++) {
				answers.add(callers.submit(() -> api.jobs("nvidia.wd5/careers", Map.of(), 0, 1, () -> {
				})));
			}
			for (Future<WorkdayApiPort.Answer> answer : answers) {
				assertThat(answer.get().status()).isEqualTo(200);
			}
		}

		assertThat(mostOpen).hasValue(2);
	}

	private WorkdayApiAdapter api(Throttle throttle) {
		String base = "http://localhost:%d/%%2$s/%%1$s/wday/cxs/%%1$s/%%3$s/".formatted(this.server.getAddress().getPort());
		return new WorkdayApiAdapter(base, "oneprofile-test", throttle, 4, Duration.ofMillis(1), Duration.ofMillis(1),
				Duration.ofSeconds(5));
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/", (exchange) -> {
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			this.requests.add("%s %s %s %s %s".formatted(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
					exchange.getRequestHeaders().getFirst("Content-Type"),
					exchange.getRequestHeaders().getFirst("User-Agent"), body));
			handler.handle(exchange);
		});
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

}
