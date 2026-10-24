package oneprofile.backend.workers.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.company.RegionEnum;
import oneprofile.backend.storage.vacancy.PublishedGreenhouseVacancy;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.PublishedWorkdayVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.workers.Pace;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

class BoardSweepRunTest {

	/** Fast enough that no test waits on it. */
	private static final Pace UNPACED = new Pace(1, 1000, false);

	private final CompanyStore companies = mock(CompanyStore.class);

	private final VacancyStore vacancies = mock(VacancyStore.class);

	@Test
	void mirrorsTheBoardOfEveryCompanyWhoseBoardHasOpenings() {
		activeSlugs("stripe", "notion");
		given(this.vacancies.mirror(eq(AtsEnum.GREENHOUSE), eq("stripe"), any()))
			.willReturn(new VacancyStore.Reconciliation(2, 1, 0));
		given(this.vacancies.mirror(eq(AtsEnum.GREENHOUSE), eq("notion"), any()))
			.willReturn(new VacancyStore.Reconciliation(0, 1, 3));

		BoardSweepRun sweep = sweepOf(Map.of("stripe", List.of(published(4001), published(4002), published(4003)),
				"notion", List.of(published(5001))));

		assertThat(sweep.sweepAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardSweepRun.Report(2, 4, 2, 2, 3, 0));
		then(this.vacancies).should().mirror(AtsEnum.GREENHOUSE, "notion", List.of(published(5001)));
	}

	@Test
	void recordsTheNameTheVacanciesOfABoardGiveMostOftenAndNoneWhereTheyGiveNone() {
		given(this.companies.regionsOf(AtsEnum.WORKDAY, BoardStatusEnum.ACTIVE))
			.willReturn(new TreeMap<>(Map.of("nvidia.wd5/careers", RegionEnum.US, "acme.wd1/careers", RegionEnum.US)));
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));
		Map<String, List<PublishedVacancy>> boards = Map.of("nvidia.wd5/careers",
				List.of(workday("JR1", "2100 NVIDIA USA"), workday("JR2", null), workday("JR3", "NVIDIA Israel"),
						workday("JR4", "2100 NVIDIA USA")),
				"acme.wd1/careers", List.of(workday("JR9", null)));

		new BoardSweepRun(Map.of(AtsEnum.WORKDAY, reader(boards)), Map.of(AtsEnum.WORKDAY, UNPACED), this.companies,
				this.vacancies)
			.sweepAll(AtsEnum.WORKDAY);

		then(this.companies).should().recordName(AtsEnum.WORKDAY, "nvidia.wd5/careers", "2100 NVIDIA USA");
		then(this.companies).should(BDDMockito.never()).recordName(eq(AtsEnum.WORKDAY), eq("acme.wd1/careers"), any());
	}

	@Test
	void leavesAloneTheCompaniesWhoseBoardItCouldNotRead() {
		activeSlugs("stripe", "unreachable");
		given(this.vacancies.mirror(eq(AtsEnum.GREENHOUSE), eq("stripe"), any()))
			.willReturn(new VacancyStore.Reconciliation(1, 0, 0));
		Map<String, List<PublishedVacancy>> boards = new LinkedHashMap<>();
		boards.put("stripe", List.of(published(4001)));
		boards.put("unreachable", null);

		assertThat(sweepOf(boards).sweepAll(AtsEnum.GREENHOUSE)).isEqualTo(new BoardSweepRun.Report(2, 1, 1, 0, 0, 1));

		then(this.vacancies).should(BDDMockito.never()).mirror(eq(AtsEnum.GREENHOUSE), eq("unreachable"), any());
	}

	@Test
	void keepsNoMoreRequestsOpenAtOnceThanItsPaceAllows() {
		activeSlugs("a", "b", "c", "d", "e", "f", "g", "h", "i");
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));
		AtomicInteger open = new AtomicInteger();
		AtomicInteger mostOpen = new AtomicInteger();
		VacancyReaderPort reader = (slug, region, onRefusal) -> {
			mostOpen.accumulateAndGet(open.incrementAndGet(), Math::max);
			pause(Duration.ofMillis(30));
			open.decrementAndGet();
			return List.of();
		};

		sweepOf(reader, new Pace(3, 1000, false)).sweepAll(AtsEnum.GREENHOUSE);

		assertThat(mostOpen).hasValue(3);
	}

	@Test
	void startsNoMoreRequestsASecondThanItsPaceAllows() {
		activeSlugs("a", "b", "c", "d", "e");
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));

		Instant before = Instant.now();
		sweepOf((slug, region, onRefusal) -> List.of(), new Pace(10, 20, false)).sweepAll(AtsEnum.GREENHOUSE);

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(200));
	}

	@Test
	void slowsDownEachTimeItIsRefused() {
		activeSlugs("a", "b", "c", "d");
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));
		VacancyReaderPort reader = (slug, region, onRefusal) -> {
			if (slug.equals("a")) {
				onRefusal.run();
			}
			return List.of();
		};

		Instant before = Instant.now();
		sweepOf(reader, new Pace(1, 20, false)).sweepAll(AtsEnum.GREENHOUSE);

		// 50 ms before b, whose start was set before the refusal, then 100 ms before each of c and d.
		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(250));
	}

	@Test
	void leavesAloneABoardStillRefusedAfterItsRetries() {
		activeSlugs("stripe", "refused");
		given(this.vacancies.mirror(eq(AtsEnum.GREENHOUSE), eq("stripe"), any()))
			.willReturn(new VacancyStore.Reconciliation(1, 0, 0));
		VacancyReaderPort reader = (slug, region, onRefusal) -> {
			if (slug.equals("refused")) {
				onRefusal.run();
				throw new IOException("Asked for the openings of refused and got 429");
			}
			return List.of(published(4001));
		};

		assertThat(sweepOf(reader, new Pace(2, 1000, false)).sweepAll(AtsEnum.GREENHOUSE))
			.isEqualTo(new BoardSweepRun.Report(2, 1, 1, 0, 0, 1));
		then(this.vacancies).should(BDDMockito.never()).mirror(eq(AtsEnum.GREENHOUSE), eq("refused"), any());
	}

	@Test
	void sweepsTheBoardsOfTheAtsItIsAskedForWithThatAtssReader() {
		given(this.companies.regionsOf(AtsEnum.ASHBY, BoardStatusEnum.ACTIVE))
			.willReturn(new TreeMap<>(Map.of("ramp", RegionEnum.US)));
		given(this.vacancies.mirror(eq(AtsEnum.ASHBY), eq("ramp"), any()))
			.willReturn(new VacancyStore.Reconciliation(1, 0, 0));
		BoardSweepRun sweep = new BoardSweepRun(
				Map.of(AtsEnum.GREENHOUSE, reader(Map.of()), AtsEnum.ASHBY, reader(Map.of("ramp", List.of(published(1))))),
				Map.of(AtsEnum.GREENHOUSE, UNPACED, AtsEnum.ASHBY, UNPACED), this.companies, this.vacancies);

		assertThat(sweep.sweepAll(AtsEnum.ASHBY)).isEqualTo(new BoardSweepRun.Report(1, 1, 1, 0, 0, 0));
		then(this.vacancies).should().mirror(AtsEnum.ASHBY, "ramp", List.of(published(1)));
	}

	@Test
	void readsEachBoardInTheRegionItsCompanyHolds() {
		given(this.companies.regionsOf(AtsEnum.LEVER, BoardStatusEnum.ACTIVE))
			.willReturn(new TreeMap<>(Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU)));
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));
		Map<String, RegionEnum> asked = new ConcurrentHashMap<>();
		VacancyReaderPort reader = (slug, region, onRefusal) -> {
			asked.put(slug, region);
			return List.of();
		};
		BoardSweepRun sweep = new BoardSweepRun(Map.of(AtsEnum.LEVER, reader), Map.of(AtsEnum.LEVER, UNPACED),
				this.companies, this.vacancies);

		sweep.sweepAll(AtsEnum.LEVER);

		assertThat(asked).isEqualTo(Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU));
	}

	private static PublishedVacancy workday(String externalId, String companyName) {
		return new PublishedWorkdayVacancy(externalId, "Software Engineer", "Santa Clara", null,
				"https://nvidia.wd5.myworkdayjobs.com/careers/job/" + externalId, null, null, null, null, companyName);
	}

	private void activeSlugs(String... slugs) {
		given(this.companies.regionsOf(AtsEnum.GREENHOUSE, BoardStatusEnum.ACTIVE))
			.willReturn(Stream.of(slugs)
				.collect(Collectors.toMap((slug) -> slug, (slug) -> RegionEnum.US, (a, b) -> a, TreeMap::new)));
	}

	private BoardSweepRun sweepOf(Map<String, List<PublishedVacancy>> boards) {
		return sweepOf(reader(boards), UNPACED);
	}

	private BoardSweepRun sweepOf(VacancyReaderPort reader, Pace pace) {
		return new BoardSweepRun(Map.of(AtsEnum.GREENHOUSE, reader), Map.of(AtsEnum.GREENHOUSE, pace), this.companies,
				this.vacancies);
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
	private VacancyReaderPort reader(Map<String, List<PublishedVacancy>> boards) {
		return (slug, region, onRefusal) -> {
			List<PublishedVacancy> published = boards.get(slug);
			if (published == null) {
				throw new IOException("boards-api.greenhouse.io answered 503");
			}
			return published;
		};
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedGreenhouseVacancy(String.valueOf(externalId), "Backend Engineer", "Remote - Americas",
				"Engineering", "Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null,
				null, Instant.parse("2026-09-01T14:00:00Z"), null, Instant.parse("2026-09-18T16:30:00Z"), "en");
	}

}
