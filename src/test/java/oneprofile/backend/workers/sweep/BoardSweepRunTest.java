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
import java.util.TreeSet;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.BoardStatusEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

class BoardSweepRunTest {

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

		assertThat(sweep.sweepAll()).isEqualTo(new BoardSweepRun.Report(2, 4, 2, 2, 3, 0));
		then(this.vacancies).should().mirror(AtsEnum.GREENHOUSE, "notion", List.of(published(5001)));
	}

	@Test
	void leavesAloneTheCompaniesWhoseBoardItCouldNotRead() {
		activeSlugs("stripe", "unreachable");
		given(this.vacancies.mirror(eq(AtsEnum.GREENHOUSE), eq("stripe"), any()))
			.willReturn(new VacancyStore.Reconciliation(1, 0, 0));
		Map<String, List<PublishedVacancy>> boards = new LinkedHashMap<>();
		boards.put("stripe", List.of(published(4001)));
		boards.put("unreachable", null);

		assertThat(sweepOf(boards).sweepAll()).isEqualTo(new BoardSweepRun.Report(2, 1, 1, 0, 0, 1));

		then(this.vacancies).should(BDDMockito.never()).mirror(eq(AtsEnum.GREENHOUSE), eq("unreachable"), any());
	}

	@Test
	void waitsBetweenBoardsSoThatTheAtsIsNotAskedTooOften() {
		activeSlugs("stripe", "notion", "figma");
		given(this.vacancies.mirror(any(), any(), any())).willReturn(new VacancyStore.Reconciliation(0, 0, 0));
		BoardSweepRun sweep = new BoardSweepRun(AtsEnum.GREENHOUSE,
				reader(Map.of("stripe", List.of(), "notion", List.of(), "figma", List.of())), this.companies,
				this.vacancies, Duration.ofMillis(50));

		Instant before = Instant.now();
		sweep.sweepAll();

		assertThat(Duration.between(before, Instant.now())).isGreaterThanOrEqualTo(Duration.ofMillis(100));
	}

	private void activeSlugs(String... slugs) {
		given(this.companies.slugsOf(AtsEnum.GREENHOUSE, BoardStatusEnum.ACTIVE)).willReturn(new TreeSet<>(List.of(slugs)));
	}

	private BoardSweepRun sweepOf(Map<String, List<PublishedVacancy>> boards) {
		return new BoardSweepRun(AtsEnum.GREENHOUSE, reader(boards), this.companies, this.vacancies, Duration.ZERO);
	}

	/** Answers what the map holds, and fails to read the slugs it maps to null. */
	private VacancyReaderPort reader(Map<String, List<PublishedVacancy>> boards) {
		return (slug) -> {
			List<PublishedVacancy> published = boards.get(slug);
			if (published == null) {
				throw new IOException("boards-api.greenhouse.io answered 503");
			}
			return published;
		};
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(externalId, "Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null, null, null,
				Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z"));
	}

}
