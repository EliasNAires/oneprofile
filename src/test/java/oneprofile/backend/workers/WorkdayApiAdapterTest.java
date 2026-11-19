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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Exercised against a stub server rather than {@code *.myworkdayjobs.com}: each board is a path on it,
 * named by its host, tenant and site as Workday's hostname and path name them.
 */
@ExtendWith(OutputCaptureExtension.class)
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

	private final List<WorkdayApiAdapter> adapters = new CopyOnWriteArrayList<>();

	@BeforeEach
	void startServer() throws IOException {
		this.server = HttpServer.create(new InetSocketAddress(0), 0);
		this.server.setExecutor(Executors.newCachedThreadPool());
		this.server.start();
	}

	@AfterEach
	void stopServer() {
		this.adapters.forEach(WorkdayApiAdapter::stopTraffic);
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
	void answersWithWhatWorkdaySaysOfABoardThatCannotBeRead(CapturedOutput output) throws Exception {
		serve((exchange) -> respond(exchange, 422, NOT_FOUND));

		WorkdayApiPort.Answer answer = api(new Throttle("The test", 20, 100), Duration.ofMillis(50)).jobs("nvidia.wd5/nosuchsite", Map.of(), 0,
				1, this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(422);
		assertThat(this.requests).hasSize(1);
		assertThat(this.refusals).hasValue(0);
		await(() -> total(output, "status_other") == 1);
		assertThat(total(output, "io_error")).isZero();
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
				Duration.ofMillis(1), Duration.ofSeconds(5), Duration.ofMinutes(1));
		this.adapters.add(api);

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
	void asksAgainWithoutSlowingDownAfterATenantsOwnLimitOrAGatewayError(CapturedOutput output) throws Exception {
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

		WorkdayApiPort.Answer answer = api(throttle, Duration.ofMillis(50)).jobs("accenture.wd103/accenturecareers", Map.of(), 0, 1,
				this.refusals::incrementAndGet);

		assertThat(answer.status()).isEqualTo(200);
		assertThat(this.refusals).hasValue(0);
		assertThat(throttle.requestsAtOnce()).isEqualTo(20);
		await(() -> total(output, "status_200") == 1);
		assertThat(total(output, "tenant_429")).isEqualTo(1);
		assertThat(total(output, "gateway")).isEqualTo(2);
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

	/** Every HTTP attempt has one outcome, including attempts retried after a refusal or gateway error. */
	@Test
	void reportsTrafficByKindAndOutcome(CapturedOutput output) throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		serve((exchange) -> {
			switch (attempts.incrementAndGet()) {
				case 2 -> respond(exchange, 404, "{}");
				case 3 -> {
					exchange.getResponseHeaders().add("cf-mitigated", "challenge");
					respond(exchange, 429, CHALLENGE);
				}
				case 5 -> respond(exchange, 503, "no healthy upstream");
				default -> respond(exchange, 200, PAGE);
			}
		});
		WorkdayApiAdapter api = api(new Throttle("The test", 20, 100), Duration.ofMillis(50));
		api.jobs("nvidia.wd5/careers", Map.of(), 0, 1, this.refusals::incrementAndGet);
		api.job("nvidia.wd5/careers", "/job/closed", this.refusals::incrementAndGet);
		api.jobs("nvidia.wd5/careers", Map.of(), 0, 1, this.refusals::incrementAndGet);
		api.jobs("nvidia.wd5/careers", Map.of(), 0, 1, this.refusals::incrementAndGet);

		await(() -> total(output, "answered_list") == 5 && total(output, "answered_description") == 1
				&& total(output, "status_200") == 3 && total(output, "gateway") == 1);
		assertThat(total(output, "started_list")).isEqualTo(5);
		assertThat(total(output, "started_description")).isEqualTo(1);
		assertThat(total(output, "status_200")).isEqualTo(3);
		assertThat(total(output, "status_404")).isEqualTo(1);
		assertThat(total(output, "challenge")).isEqualTo(1);
		assertThat(total(output, "gateway")).isEqualTo(1);
		assertThat(total(output, "status_other")).isZero();
		assertThat(total(output, "tenant_429")).isZero();
		assertThat(total(output, "io_error")).isZero();
		assertThat(output.getOut()).containsPattern("WORKDAY_TRAFFIC started_list=\\d+ started_description=\\d+"
				+ " answered_list=\\d+ list_ms=\\d+ answered_description=\\d+ description_ms=\\d+"
				+ " status_200=\\d+ status_404=\\d+ status_other=\\d+ tenant_429=\\d+ challenge=\\d+ gateway=\\d+"
				+ " io_error=\\d+ throttle_wait_ms=\\d+ backoff_ms=\\d+ requests_at_once=10 starts_a_second=50"
				+ " open=0 oldest_open_s=0");
	}

	/** A connection closed without an HTTP answer is counted once as an I/O failure. */
	@Test
	void reportsAnIoFailureWithoutAnHttpAnswer(CapturedOutput output) throws Exception {
		serve(HttpExchange::close);
		WorkdayApiAdapter api = api(new Throttle("The test", 20, 100), Duration.ofMillis(50));

		assertThatIOException().isThrownBy(() -> api.jobs("nvidia.wd5/careers", Map.of(), 0, 1,
				this.refusals::incrementAndGet));

		await(() -> total(output, "io_error") == 1);
		assertThat(total(output, "started_list")).isEqualTo(1);
		assertThat(total(output, "answered_list")).isZero();
		assertThat(total(output, "status_other")).isZero();
	}

	/** Headers alone do not finish a request: the heartbeat still sees its unfinished body. */
	@Test
	void reportsAnOpenRequestWhileItsBodyIsBlocked(CapturedOutput output) throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		serve((exchange) -> {
			exchange.sendResponseHeaders(200, 2);
			exchange.getResponseBody().write('{');
			exchange.getResponseBody().flush();
			try {
				if (!release.await(4, TimeUnit.SECONDS)) {
					throw new IOException("The test did not release the response body");
				}
				exchange.getResponseBody().write('}');
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException(ex);
			}
			finally {
				exchange.close();
			}
		});
		WorkdayApiAdapter api = api(new Throttle("The test", 20, 100), Duration.ofMillis(50));
		try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
			Future<WorkdayApiPort.Answer> answer = caller.submit(() -> api.jobs("nvidia.wd5/careers", Map.of(), 0, 1,
					this.refusals::incrementAndGet));
			try {
				await(() -> output.getOut().contains("open=1 oldest_open_s=0"));
				await(() -> output.getOut().contains("open=1 oldest_open_s=1"));
				assertThat(total(output, "answered_list")).isZero();
			}
			finally {
				release.countDown();
			}
			assertThat(answer.get(2, TimeUnit.SECONDS).status()).isEqualTo(200);
		}
	}

	/** Adds one metric over all intervals without assuming which tick a request finishes in. */
	private static long total(CapturedOutput output, String key) {
		return output.getOut().lines().filter((line) -> line.contains("WORKDAY_TRAFFIC ")).mapToLong((line) -> {
			var matcher = Pattern.compile("(?:^| )" + key + "=(\\d+)(?: |$)").matcher(line);
			return matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
		}).sum();
	}

	/** Gives the scheduled logger a bounded opportunity to publish the expected observation. */
	private static void await(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
			Thread.sleep(10);
		}
		assertThat(condition.getAsBoolean()).isTrue();
	}

	/** Uses the production interval for tests that do not inspect traffic. */
	private WorkdayApiAdapter api(Throttle throttle) {
		return api(throttle, Duration.ofMinutes(1));
	}

	/** Shares the local HTTP server and shuts down each adapter after the test. */
	private WorkdayApiAdapter api(Throttle throttle, Duration heartbeat) {
		String base = "http://localhost:%d/%%2$s/%%1$s/wday/cxs/%%1$s/%%3$s/".formatted(this.server.getAddress().getPort());
		WorkdayApiAdapter api = new WorkdayApiAdapter(base, "oneprofile-test", throttle, 4, Duration.ofMillis(1),
				Duration.ofMillis(1), Duration.ofSeconds(5), heartbeat);
		this.adapters.add(api);
		return api;
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
