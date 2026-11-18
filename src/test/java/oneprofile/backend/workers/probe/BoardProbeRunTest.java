package oneprofile.backend.workers.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.boardlist.BoardListStore;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyRepository;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedGreenhouseVacancy;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyRepository;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.workers.Pace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class BoardProbeRunTest {

	private static final Instant PROBED_AT = Instant.parse("2026-09-19T10:15:30Z");

	/** Fast enough that no test waits on it. */
	private static final Pace UNPACED = new Pace(1, 1000, false);

	private final CompanyStore companies = mock(CompanyStore.class);

	private final VacancyStore vacancies = mock(VacancyStore.class);

	/** Only an active company still holding no name is asked separately. */
	@Test
	void asksForAndRecordsOnlyTheMissingNameOfAnActiveCompany() throws IOException {
		heldSlugs("unnamed", "named", "empty", "gone", "unreadable", "listed", "provided");
		BDDMockito.given(this.companies.unnamedSlugsOf(AtsEnum.GREENHOUSE))
			.willReturn(Set.of("unnamed", "empty", "gone", "unreadable", "listed", "provided"));
		BoardReaderPort reader = mock(BoardReaderPort.class);
		BDDMockito.given(reader.read(any(), any(), any())).willAnswer((call) -> switch ((String) call.getArgument(0)) {
			case "unreadable" -> throw new IOException("Unavailable");
			case "empty" -> new BoardReading(BoardStatusEnum.EMPTY, null);
			case "gone" -> new BoardReading(BoardStatusEnum.NOT_FOUND, null);
			case "provided" -> new BoardReading(BoardStatusEnum.ACTIVE, "Provided");
			default -> new BoardReading(BoardStatusEnum.ACTIVE, null);
		});
		BDDMockito.given(reader.name(eq("unnamed"), eq(RegionEnum.US), any())).willReturn("Readable");
		BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader),
				Map.of(AtsEnum.GREENHOUSE, UNPACED), this.companies, this.vacancies, unread("listed"), clock());

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(3, 1, 1, 1, 1));

		then(reader).should().name(eq("unnamed"), eq(RegionEnum.US), any());
		for (String slug : List.of("named", "empty", "gone", "unreadable", "listed", "provided")) {
			then(reader).should(BDDMockito.never()).name(eq(slug), any(), any());
		}
		then(this.companies).should().unnamedSlugsOf(AtsEnum.GREENHOUSE);
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "unnamed", BoardStatusEnum.ACTIVE,
				"Readable", PROBED_AT);
	}

	/** A failed name request keeps the board active and is tried again until the company is named. */
	@Test
	void retriesAFailedNameOnTheNextRunAndStopsOnceNamed() throws IOException {
		heldSlugs("unnamed");
		BDDMockito.given(this.companies.unnamedSlugsOf(AtsEnum.GREENHOUSE))
			.willReturn(Set.of("unnamed"), Set.of("unnamed"), Set.of());
		BoardReaderPort reader = mock(BoardReaderPort.class);
		BDDMockito.given(reader.read(any(), any(), any())).willReturn(new BoardReading(BoardStatusEnum.ACTIVE, null));
		BDDMockito.given(reader.name(eq("unnamed"), eq(RegionEnum.US), any()))
			.willThrow(new IOException("Unavailable")).willReturn("Readable");
		BoardProbeRun probe = probeOf(reader, UNPACED);

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0, 0));
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "unnamed", BoardStatusEnum.ACTIVE, null, PROBED_AT);
		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0, 0));
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "unnamed", BoardStatusEnum.ACTIVE,
				"Readable", PROBED_AT);
		probe.probeAll(AtsEnum.GREENHOUSE);
		then(reader).should(BDDMockito.times(2)).name(eq("unnamed"), eq(RegionEnum.US), any());
	}

	/** An ATS publishing no name leaves the company eligible for the next probe. */
	@Test
	void retriesAnEmptyNameOnTheNextRun() throws IOException {
		heldSlugs("unnamed");
		BDDMockito.given(this.companies.unnamedSlugsOf(AtsEnum.GREENHOUSE)).willReturn(Set.of("unnamed"));
		BoardReaderPort reader = mock(BoardReaderPort.class);
		BDDMockito.given(reader.read(any(), any(), any())).willReturn(new BoardReading(BoardStatusEnum.ACTIVE, null));
		BDDMockito.given(reader.name(any(), any(), any())).willReturn(null, "Readable");
		BoardProbeRun probe = probeOf(reader, UNPACED);
		probe.probeAll(AtsEnum.GREENHOUSE);
		probe.probeAll(AtsEnum.GREENHOUSE);
		then(reader).should(BDDMockito.times(2)).name(eq("unnamed"), eq(RegionEnum.US), any());
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "unnamed", BoardStatusEnum.ACTIVE,
				"Readable", PROBED_AT);
	}

	/** Listed boards are cleared without requesting them; all other outcomes are counted. */
	@Test
	void recordsListedBoardsAsUnreadWithoutReadingThem() {
		heldSlugs("demo", "active", "empty", "gone", "unreadable");
		Set<String> asked = ConcurrentHashMap.newKeySet();
		BoardReaderPort answers = reader(Map.of("demo", new BoardReading(BoardStatusEnum.ACTIVE, "Demo"),
				"active", new BoardReading(BoardStatusEnum.ACTIVE, "Active"), "empty",
				new BoardReading(BoardStatusEnum.EMPTY, null), "gone", new BoardReading(BoardStatusEnum.NOT_FOUND, null)));
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			asked.add(slug);
			return answers.read(slug, region, onRefusal);
		};
		BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader),
				Map.of(AtsEnum.GREENHOUSE, UNPACED), this.companies, this.vacancies, unread("demo"), clock());

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 1, 1));

		assertThat(asked).containsExactlyInAnyOrder("active", "empty", "gone", "unreadable");
		var order = inOrder(this.vacancies, this.companies);
		order.verify(this.vacancies).mirror(AtsEnum.GREENHOUSE, "demo", List.of());
		order.verify(this.companies).recordProbe(AtsEnum.GREENHOUSE, "demo", BoardStatusEnum.UNREAD, null, PROBED_AT);
	}

	/** UNREAD is assigned by the list, never returned by a reader. */
	@Test
	void refusesAnUnreadReaderResult() {
		heldSlugs("bug");

		assertThatIllegalStateException().isThrownBy(() -> probeOf(Map.of("bug",
				new BoardReading(BoardStatusEnum.UNREAD, null))).probeAll(AtsEnum.GREENHOUSE));
		then(this.companies).should(BDDMockito.never()).recordProbe(any(), any(), any(), any(), any());
		then(this.vacancies).shouldHaveNoInteractions();
	}

	@Test
	void recordsAgainstEveryCompanyWhatItsBoardIs() {
		heldSlugs("stripe", "notion", "gone");
		BoardProbeRun probe = probeOf(Map.of("stripe", new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"), "notion",
				new BoardReading(BoardStatusEnum.EMPTY, null), "gone", new BoardReading(BoardStatusEnum.NOT_FOUND, null)));

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 0, 0));

		then(this.companies).should()
			.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "notion", BoardStatusEnum.EMPTY, null, PROBED_AT);
		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "gone", BoardStatusEnum.NOT_FOUND, null, PROBED_AT);
		var order = inOrder(this.vacancies, this.companies);
		order.verify(this.vacancies).mirror(AtsEnum.GREENHOUSE, "gone", List.of());
		order.verify(this.companies).recordProbe(AtsEnum.GREENHOUSE, "gone", BoardStatusEnum.NOT_FOUND, null, PROBED_AT);
		order.verify(this.vacancies).mirror(AtsEnum.GREENHOUSE, "notion", List.of());
		order.verify(this.companies).recordProbe(AtsEnum.GREENHOUSE, "notion", BoardStatusEnum.EMPTY, null, PROBED_AT);
		then(this.vacancies).should(BDDMockito.never()).mirror(eq(AtsEnum.GREENHOUSE), eq("stripe"), any());
	}

	@Test
	void leavesUnprobedTheCompaniesWhoseBoardItCouldNotRead() {
		heldSlugs("stripe", "unreachable");
		Map<String, BoardReading> boards = new LinkedHashMap<>();
		boards.put("stripe", new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"));
		boards.put("unreachable", null);

		assertThat(probeOf(boards).probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0, 1));

		then(this.companies).should().recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);
		then(this.companies).should(BDDMockito.never())
			.recordProbe(AtsEnum.GREENHOUSE, "unreachable", null, null, PROBED_AT);
		then(this.vacancies).shouldHaveNoInteractions();
	}

	@Test
	void keepsNoMoreRequestsOpenAtOnceThanItsPaceAllows() {
		heldSlugs("a", "b", "c", "d", "e", "f", "g", "h", "i");
		AtomicInteger open = new AtomicInteger();
		AtomicInteger mostOpen = new AtomicInteger();
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			mostOpen.accumulateAndGet(open.incrementAndGet(), Math::max);
			pause(Duration.ofMillis(30));
			open.decrementAndGet();
			return new BoardReading(BoardStatusEnum.EMPTY, null);
		};

		probeOf(reader, new Pace(3, 1000, false)).probeAll(AtsEnum.GREENHOUSE);

		assertThat(mostOpen).hasValue(3);
	}

	@Test
	void startsNoMoreRequestsASecondThanItsPaceAllows() {
		heldSlugs("a", "b", "c", "d", "e");
		BoardReaderPort reader = (slug, region, onRefusal) -> new BoardReading(BoardStatusEnum.EMPTY, null);

		Instant before = Instant.now();
		probeOf(reader, new Pace(10, 20, false)).probeAll(AtsEnum.GREENHOUSE);

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(200));
	}

	@Test
	void halvesTheRequestsItKeepsOpenAtOnceEachTimeItIsRefused() {
		heldSlugs("a", "b", "c", "d", "e", "f", "g", "h", "i");
		AtomicInteger open = new AtomicInteger();
		AtomicInteger mostOpenOnceRefused = new AtomicInteger();
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			int nowOpen = open.incrementAndGet();
			if (slug.equals("a")) {
				onRefusal.run();
			}
			// e starts four milliseconds after a at the soonest, long after a was refused.
			if (slug.compareTo("e") >= 0) {
				mostOpenOnceRefused.accumulateAndGet(nowOpen, Math::max);
			}
			pause(Duration.ofMillis(30));
			open.decrementAndGet();
			return new BoardReading(BoardStatusEnum.EMPTY, null);
		};

		probeOf(reader, new Pace(4, 1000, false)).probeAll(AtsEnum.GREENHOUSE);

		assertThat(mostOpenOnceRefused).hasValue(2);
	}

	@Test
	void halvesTheRequestsItStartsASecondEachTimeItIsRefused() {
		heldSlugs("a", "b", "c", "d");
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			if (slug.equals("a")) {
				onRefusal.run();
			}
			return new BoardReading(BoardStatusEnum.EMPTY, null);
		};

		Instant before = Instant.now();
		probeOf(reader, new Pace(1, 20, false)).probeAll(AtsEnum.GREENHOUSE);

		// 50 ms before b, whose start was set before the refusal, then 100 ms before each of c and d.
		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(250));
	}

	@Test
	void countsABoardStillRefusedAfterItsRetriesAsUnreadable() {
		heldSlugs("stripe", "refused");
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			if (slug.equals("refused")) {
				onRefusal.run();
				throw new IOException("Asked for the board of refused and got 429");
			}
			return new BoardReading(BoardStatusEnum.ACTIVE, "Stripe");
		};

		assertThat(probeOf(reader, new Pace(2, 1000, false)).probeAll(AtsEnum.GREENHOUSE))
			.isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0, 1));
		then(this.companies).should(BDDMockito.never())
			.recordProbe(eq(AtsEnum.GREENHOUSE), eq("refused"), any(), any(), any());
	}

	@Test
	void givesEachRegionThePaceOfItsOwnWhereTheAtsHasOneApiPerRegion() throws InterruptedException {
		BDDMockito.given(this.companies.regionsOf(AtsEnum.LEVER))
			.willReturn(new TreeMap<>(Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU)));
		CountDownLatch bothOpen = new CountDownLatch(2);
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			bothOpen.countDown();
			try {
				bothOpen.await(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new BoardReading(BoardStatusEnum.ACTIVE, null);
		};
		BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.LEVER, reader),
				Map.of(AtsEnum.LEVER, new Pace(1, 1000, true)), this.companies, this.vacancies, unread(), clock());

		probe.probeAll(AtsEnum.LEVER);

		assertThat(bothOpen.getCount()).isZero();
	}

	@Test
	void probesTheBoardsOfTheAtsItIsAskedForWithThatAtssReader() {
		BDDMockito.given(this.companies.regionsOf(AtsEnum.ASHBY))
			.willReturn(new TreeMap<>(Map.of("ramp", RegionEnum.US)));
		BoardProbeRun probe = new BoardProbeRun(
				Map.of(AtsEnum.GREENHOUSE, reader(Map.of()), AtsEnum.ASHBY,
						reader(Map.of("ramp", new BoardReading(BoardStatusEnum.ACTIVE, null)))),
				Map.of(AtsEnum.GREENHOUSE, UNPACED, AtsEnum.ASHBY, UNPACED), this.companies, this.vacancies, unread(), clock());

		assertThat(probe.probeAll(AtsEnum.ASHBY)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0, 0));
		then(this.companies).should().recordProbe(AtsEnum.ASHBY, "ramp", BoardStatusEnum.ACTIVE, null, PROBED_AT);
	}

	@Test
	void asksForEachBoardInTheRegionItsCompanyHolds() {
		BDDMockito.given(this.companies.regionsOf(AtsEnum.LEVER))
			.willReturn(new TreeMap<>(Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU)));
		Map<String, RegionEnum> asked = new ConcurrentHashMap<>();
		BoardReaderPort reader = (slug, region, onRefusal) -> {
			asked.put(slug, region);
			return new BoardReading(BoardStatusEnum.ACTIVE, null);
		};
		BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.LEVER, reader), Map.of(AtsEnum.LEVER, UNPACED),
				this.companies, this.vacancies, unread(), clock());

		assertThat(probe.probeAll(AtsEnum.LEVER)).isEqualTo(new BoardProbeRun.Report(2, 0, 0, 0, 0));
		assertThat(asked).isEqualTo(Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU));
	}

	private void heldSlugs(String... slugs) {
		BDDMockito.given(this.companies.regionsOf(AtsEnum.GREENHOUSE))
			.willReturn(Stream.of(slugs)
				.collect(Collectors.toMap((slug) -> slug, (slug) -> RegionEnum.US, (a, b) -> a, TreeMap::new)));
	}

	private BoardProbeRun probeOf(Map<String, BoardReading> boards) {
		return probeOf(reader(boards), UNPACED);
	}

	private BoardProbeRun probeOf(BoardReaderPort reader, Pace pace) {
		return new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader), Map.of(AtsEnum.GREENHOUSE, pace), this.companies,
				this.vacancies, unread(), clock());
	}

	/** A list containing only the named Greenhouse boards. */
	private BoardListStore unread(String... slugs) {
		return BoardListStore.read(new StringReader("ats\tslug\treason\n" + Stream.of(slugs)
			.map((slug) -> "greenhouse\t" + slug + "\tTest board\n").collect(Collectors.joining())));
	}

	private static void pause(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/** Answers what the map holds, and fails to read the slugs it maps to null. */
	private BoardReaderPort reader(Map<String, BoardReading> boards) {
		return (slug, region, onRefusal) -> {
			BoardReading reading = boards.get(slug);
			if (reading == null) {
				throw new IOException("boards-api.greenhouse.io answered 503");
			}
			return reading;
		};
	}

	/** Verifies the vacancies retained or removed by each probe result. */
	@Nested
	@DataJpaTest
	@AutoConfigureTestDatabase(replace = Replace.NONE)
	@Import({ TestcontainersConfiguration.class, CompanyStore.class, VacancyStore.class })
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	class WithStores {

		@Autowired
		private CompanyStore companies;

		@Autowired
		private VacancyStore vacancies;

		@Autowired
		private CompanyRepository companyRepository;

		@Autowired
		private VacancyRepository vacancyRepository;

		/** Removes committed fixtures, since paced callbacks run on other threads. */
		@AfterEach
		void removeCompanies() {
			for (String slug : List.of("probe-gone", "probe-empty", "probe-active", "probe-unreadable",
					"probe-unread", "probe-relisted")) {
				this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, slug).ifPresent((company) -> {
					this.vacancies.mirror(AtsEnum.GREENHOUSE, slug, List.of());
					this.companyRepository.delete(company);
				});
			}
		}

		/** Listed boards lose their vacancies and keep their held name without being read. */
		@Test
		void clearsAListedActiveBoardAndProbesAnUnlistedUnreadBoard() {
			this.companies.record(AtsEnum.GREENHOUSE,
					Map.of("probe-unread", RegionEnum.US, "probe-relisted", RegionEnum.US));
			this.companies.recordProbe(AtsEnum.GREENHOUSE, "probe-unread", BoardStatusEnum.ACTIVE,
					"Held name", Instant.EPOCH);
			this.companies.recordProbe(AtsEnum.GREENHOUSE, "probe-relisted", BoardStatusEnum.UNREAD,
					"Old name", Instant.EPOCH);
			this.vacancies.mirror(AtsEnum.GREENHOUSE, "probe-unread", List.of(published("held")));
			BoardReaderPort reader = (slug, region, onRefusal) -> {
				assertThat(slug).isEqualTo("probe-relisted");
				return new BoardReading(BoardStatusEnum.ACTIVE, "New name");
			};
			BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader),
					Map.of(AtsEnum.GREENHOUSE, UNPACED), this.companies, this.vacancies, unread("probe-unread"), clock());

			assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 1, 0));

			var unread = this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, "probe-unread").orElseThrow();
			assertThat(unread.boardStatus()).isEqualTo(BoardStatusEnum.UNREAD);
			assertThat(unread.name()).isEqualTo("Held name");
			assertThat(unread.probedAt()).isEqualTo(PROBED_AT);
			assertThat(this.vacancyRepository.findByCompany(unread)).isEmpty();
			var relisted = this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, "probe-relisted").orElseThrow();
			assertThat(relisted.boardStatus()).isEqualTo(BoardStatusEnum.ACTIVE);
			assertThat(relisted.name()).isEqualTo("New name");
			assertThat(relisted.probedAt()).isEqualTo(PROBED_AT);
		}

		/** Only boards confirmed to publish nothing lose their vacancies. */
		@Test
		void removesVacanciesOnlyForNotFoundAndEmptyBoards() {
			this.companies.record(AtsEnum.GREENHOUSE, Map.of("probe-gone", RegionEnum.US, "probe-empty",
					RegionEnum.US, "probe-active", RegionEnum.US, "probe-unreadable", RegionEnum.US));
			for (String slug : List.of("probe-gone", "probe-empty", "probe-active", "probe-unreadable")) {
				this.companies.recordProbe(AtsEnum.GREENHOUSE, slug, BoardStatusEnum.ACTIVE, slug, Instant.EPOCH);
				this.vacancies.mirror(AtsEnum.GREENHOUSE, slug, List.of(published(slug)));
			}
			BoardReaderPort reader = reader(Map.of("probe-gone", new BoardReading(BoardStatusEnum.NOT_FOUND, null),
					"probe-empty", new BoardReading(BoardStatusEnum.EMPTY, null), "probe-active",
					new BoardReading(BoardStatusEnum.ACTIVE, "Active")));
			BoardProbeRun probe = new BoardProbeRun(Map.of(AtsEnum.GREENHOUSE, reader),
					Map.of(AtsEnum.GREENHOUSE, UNPACED), this.companies, this.vacancies, unread(), clock());

			assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 0, 1));

			for (String slug : List.of("probe-gone", "probe-empty")) {
				var company = this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, slug).orElseThrow();
				assertThat(company.boardStatus()).isEqualTo(slug.equals("probe-gone")
						? BoardStatusEnum.NOT_FOUND : BoardStatusEnum.EMPTY);
				assertThat(company.probedAt()).isEqualTo(PROBED_AT);
				assertThat(this.vacancyRepository.findByCompany(company)).isEmpty();
			}
			for (String slug : List.of("probe-active", "probe-unreadable")) {
				var company = this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, slug).orElseThrow();
				assertThat(company.boardStatus()).isEqualTo(BoardStatusEnum.ACTIVE);
				assertThat(this.vacancyRepository.findByCompany(company)).singleElement()
					.satisfies((vacancy) -> assertThat(vacancy.externalId()).isEqualTo(slug));
			}
			var unreadable = this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, "probe-unreadable").orElseThrow();
			assertThat(unreadable.probedAt()).isEqualTo(Instant.EPOCH);
			assertThat(unreadable.name()).isEqualTo("probe-unreadable");
		}

		/** A vacancy held before the probe. */
		private PublishedVacancy published(String externalId) {
			return new PublishedGreenhouseVacancy(externalId, "Backend Engineer", "Remote - Americas",
					"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId,
					null, null, null, PROBED_AT, null, PROBED_AT, "en");
		}
	}

	private Clock clock() {
		return Clock.fixed(PROBED_AT, ZoneOffset.UTC);
	}

}
