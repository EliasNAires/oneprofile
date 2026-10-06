package oneprofile.backend.workers.sweep;

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
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercised against a stub server rather than {@code boards-api.greenhouse.io}. */
class GreenhouseVacancyReaderAdapterTest {

	/** The description is escaped twice, as Greenhouse publishes it. */
	private static final String BOARD = """
			{"jobs":[{"id":4001,"title":"Backend Engineer",
			"absolute_url":"https://job-boards.greenhouse.io/stripe/jobs/4001",
			"location":{"name":"Remote - Americas"},
			"departments":[{"id":7,"name":"Engineering"}],
			"content":"&lt;div&gt;Ship&amp;nbsp;payments.&lt;/div&gt;","language":"en",
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
				"Remote - Americas", "Engineering", "Ship payments.", "en",
				"https://job-boards.greenhouse.io/stripe/jobs/4001", 15000000L, 20000000L, "USD", "Annual Salary",
				Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z")));
		assertThat(this.queries).containsExactly("/v1/boards/stripe/jobs?content=true&pay_transparency=true");
	}

	@Test
	void bringsNothingWhereTheBoardPublishedNoDescriptionLanguagePayOrDepartment() throws IOException {
		serve((exchange) -> respondWith(exchange, 200, WITHOUT_PAY_OR_DESCRIPTION));

		PublishedVacancy designer = reader().read("stripe").get(0);
		assertThat(designer.description()).isNull();
		assertThat(designer.language()).isNull();
		assertThat(designer.department()).isNull();
		assertThat(designer.payMinCents()).isNull();
		assertThat(designer.payMaxCents()).isNull();
		assertThat(designer.payCurrency()).isNull();
		assertThat(designer.payTitle()).isNull();
	}

	@Test
	void writesOneLinePerBlockMarkingHeadingsAndListItems() throws IOException {
		serveDescription("""
				<p>We move money.</p>
				<h3>What you'll do</h3>
				<ul><li>Build&nbsp;APIs</li><li>Review code</li></ul>""");

		assertThat(description()).isEqualTo("""
				We move money.
				# What you'll do
				- Build APIs
				- Review code""");
	}

	@Test
	void takesAShortLineThatIsAllBoldForAHeading() throws IOException {
		serveDescription("""
				<p><strong>Requirements</strong></p>
				<ul><li>5+ years of <b>Java</b></li></ul>
				<p><strong>POSITION SUMMARY</strong>: The supervisor drives services growth.</p>
				<p><strong>%s</strong></p>""".formatted("We are an equal opportunity employer and value diversity at every one of our offices."));

		assertThat(description()).isEqualTo("""
				# Requirements
				- 5+ years of Java
				POSITION SUMMARY: The supervisor drives services growth.
				We are an equal opportunity employer and value diversity at every one of our offices.""");
	}

	@Test
	void takesAHeadingTagForAHeadingOnlyWhenItIsShortAndNotEmpty() throws IOException {
		serveDescription("""
				<h3>&nbsp;</h3>
				<h3>Über uns</h3>
				<h5><strong>Innovid is an independent software platform that powers the creation, delivery and measurement of TV ads.</strong></h5>""");

		assertThat(description()).isEqualTo("""
				# Über uns
				Innovid is an independent software platform that powers the creation, delivery and measurement of TV ads.""");
	}

	@Test
	void marksEveryLineOfGreenhouseBoilerplate() throws IOException {
		serveDescription("""
				<div class="content-intro"><p><strong>About Nova</strong></p><p>Nova builds retirement plans.</p></div>
				<div><p>Own the payroll integration.</p></div>
				<div class="content-pay-transparency"><div class="pay-input"><div class="title">Pay range</div></div></div>
				<div class="content-conclusion"><h3>Benefits</h3><ul><li>401(k) match</li></ul></div>""");

		assertThat(description()).isEqualTo("""
				> # About Nova
				> Nova builds retirement plans.
				Own the payroll integration.
				> Pay range
				> # Benefits
				> - 401(k) match""");
	}

	@Test
	void endsALineAtEveryBreak() throws IOException {
		serveDescription("""
				<p><strong>Job Responsibilities:</strong><br>• &nbsp; &nbsp;Manage a team of 6-8<br>• &nbsp; &nbsp;Champion AMP’s values</p>""");

		assertThat(description()).isEqualTo("""
				# Job Responsibilities:
				• Manage a team of 6-8
				• Champion AMP’s values""");
	}

	@Test
	void takesABoldLineForAHeadingWhateverSpacesFollowIt() throws IOException {
		serveDescription("""
				<p><strong>Requirements</strong>&nbsp;</p>
				<div><strong>&nbsp;</strong></div>
				<p><strong>Duties and Responsibilities</strong></p>
				<p>· &nbsp; &nbsp; &nbsp; &nbsp; Identify the technical characteristics of signals.</p>""");

		assertThat(description()).isEqualTo("""
				# Requirements
				# Duties and Responsibilities
				· Identify the technical characteristics of signals.""");
	}

	@Test
	void keepsTheItemsAndNumbersACompanyTypedAsTheyAre() throws IOException {
		serveDescription("""
				<h3>Key Responsibilities</h3>
				<p>• Lead PCB design.<br>• Run SI reviews.</p>
				<p><strong>ESSENTIAL FUNCTIONS</strong></p>
				<p>1. Manage the PSSRs.</p>
				<p>Location: Austin<br>Employment type: Full-time</p>""");

		assertThat(description()).isEqualTo("""
				# Key Responsibilities
				• Lead PCB design.
				• Run SI reviews.
				# ESSENTIAL FUNCTIONS
				1. Manage the PSSRs.
				Location: Austin
				Employment type: Full-time""");
	}

	@Test
	void neverLetsTheCompanysTextPassForAMark() throws IOException {
		serveDescription("""
				<p>> 5 years of Go</p>
				<p># Note: remote only</p>""");

		assertThat(description()).isEqualTo("""
				5 years of Go
				Note: remote only""");
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

	/** Serves a board of one opening whose content is this HTML, escaped twice as Greenhouse does. */
	private void serveDescription(String html) {
		serveOpening("\"content\":\"%s\"".formatted(escapedTwice(html)));
	}

	private void serveOpening(String fields) {
		String board = """
				{"jobs":[{"id":4003,"title":"Engineer","absolute_url":"https://job-boards.greenhouse.io/acme/jobs/4003",
				%s}]}""".formatted(fields);
		serve((exchange) -> respondWith(exchange, 200, board));
	}

	private static String escapedTwice(String html) {
		String once = html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return once.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private String description() throws IOException {
		return reader().read("acme").get(0).description();
	}

	private void serve(HttpHandler handler) {
		this.server.createContext("/", (exchange) -> {
			URI uri = exchange.getRequestURI();
			this.queries.add(uri.getPath() + "?" + uri.getQuery());
			handler.handle(exchange);
		});
	}

	private GreenhouseVacancyReaderAdapter reader() {
		URI base = URI.create("http://localhost:" + this.server.getAddress().getPort() + "/");
		return new GreenhouseVacancyReaderAdapter(base, "oneprofile/0.1 (+https://github.com/EliasNAires/oneprofile)", 3,
				Duration.ofMillis(10), Duration.ofSeconds(5));
	}

}
