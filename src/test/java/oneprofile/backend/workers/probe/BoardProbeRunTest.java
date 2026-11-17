package oneprofile.backend.workers.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oneprofile.backend.TestcontainersConfiguration;
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

	@Test
	void recordsAgainstEveryCompanyWhatItsBoardIs() {
		heldSlugs("stripe", "notion", "gone");
		BoardProbeRun probe = probeOf(Map.of("stripe", new BoardReading(BoardStatusEnum.ACTIVE, "Stripe"), "notion",
				new BoardReading(BoardStatusEnum.EMPTY, null), "gone", new BoardReading(BoardStatusEnum.NOT_FOUND, null)));

		assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 0));

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

		assertThat(probeOf(boards).probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 1));

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
			.isEqualTo(new BoardProbeRun.Report(1, 0, 0, 1));
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
				Map.of(AtsEnum.LEVER, new Pace(1, 1000, true)), this.companies, this.vacancies, clock());

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
				Map.of(AtsEnum.GREENHOUSE, UNPACED, AtsEnum.ASHBY, UNPACED), this.companies, this.vacancies, clock());

		assertThat(probe.probeAll(AtsEnum.ASHBY)).isEqualTo(new BoardProbeRun.Report(1, 0, 0, 0));
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
				this.companies, this.vacancies, clock());

		assertThat(probe.probeAll(AtsEnum.LEVER)).isEqualTo(new BoardProbeRun.Report(2, 0, 0, 0));
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
				this.vacancies, clock());
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
			for (String slug : List.of("probe-gone", "probe-empty", "probe-active", "probe-unreadable")) {
				this.companyRepository.findByAtsAndSlug(AtsEnum.GREENHOUSE, slug).ifPresent((company) -> {
					this.vacancies.mirror(AtsEnum.GREENHOUSE, slug, List.of());
					this.companyRepository.delete(company);
				});
			}
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
					Map.of(AtsEnum.GREENHOUSE, UNPACED), this.companies, this.vacancies, clock());

			assertThat(probe.probeAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardProbeRun.Report(1, 1, 1, 1));

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
