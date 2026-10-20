package oneprofile.backend.storage.vacancyskill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import oneprofile.backend.storage.normalizedvacancy.CleanedTitle;
import oneprofile.backend.storage.normalizedvacancy.CleanedVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.vacancy.PublishedVacancy;
import oneprofile.backend.storage.vacancy.VacancyStore;
import oneprofile.backend.storage.vacancy.VacancyText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, CompanyStore.class, VacancyStore.class, NormalizedVacancyStore.class,
		VacancySkillStore.class })
class VacancySkillStoreTest {

	@Autowired
	private VacancySkillStore skills;

	@Autowired
	private VacancySkillRepository repository;

	@Autowired
	private VacancyStore vacancies;

	@Autowired
	private TestEntityManager entityManager;

	private long first;

	private long second;

	@BeforeEach
	void holdTheCorpus(@Autowired CompanyStore companies, @Autowired NormalizedVacancyStore normalized) {
		companies.record(AtsEnum.GREENHOUSE, List.of("stripe"));
		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of(published(4001), published(4002)));
		List<Long> ids = this.vacancies.textsAfter(0, 10).stream().map(VacancyText::id).toList();
		normalized.recordCleanedVacancies(Map.of(ids.get(0), cleaned(), ids.get(1), cleaned()));
		this.entityManager.flush();
		this.first = normalizedId(ids.get(0));
		this.second = normalizedId(ids.get(1));
	}

	@Test
	void holdsOneRowPerSkillWithTheSegmentsItWasFoundInAndWhetherTheTitleNamesIt() {
		assertThat(this.skills.record(Map.of(this.first, List.of(new FoundSkill("go", List.of(1, 3), true, true),
				new FoundSkill("postgresql", List.of(), true, false))))).isEqualTo(2);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findAll())
			.extracting(VacancySkillEntity::normalizedVacancyId, VacancySkillEntity::skillId, VacancySkillEntity::segments,
					VacancySkillEntity::inTitle)
			.containsExactlyInAnyOrder(tuple(this.first, "go", List.of(1, 3), true),
					tuple(this.first, "postgresql", List.of(), true));
	}

	@Test
	void letsGoOfEveryRowARunRecordedBefore() {
		this.skills.record(Map.of(this.first, List.of(new FoundSkill("go", List.of(1), false, false)), this.second,
				List.of(new FoundSkill("java", List.of(0), false, false))));
		this.entityManager.flush();

		this.skills.clear();
		this.skills.record(Map.of(this.first, List.of(new FoundSkill("rust", List.of(2), false, false))));
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findAll()).extracting(VacancySkillEntity::normalizedVacancyId, VacancySkillEntity::skillId)
			.containsExactly(tuple(this.first, "rust"));
	}

	@Test
	void readsTheSegmentsEachSkillWasFoundInByVacancy() {
		this.skills.record(Map.of(this.first, List.of(new FoundSkill("go", List.of(1, 3), true, true),
				new FoundSkill("postgresql", List.of(), true, false)), this.second,
				List.of(new FoundSkill("java", List.of(0), false, false))));
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.skills.segmentsFoundIn(List.of(this.first, this.second + 1000)))
			.isEqualTo(Map.of(this.first, Map.of("go", List.of(1, 3), "postgresql", List.of())));
	}

	@Test
	void letsGoOfWhatWasFoundInAVacancyThatHasLeftItsBoard() {
		this.skills.record(Map.of(this.first, List.of(new FoundSkill("go", List.of(1), false, false))));
		this.entityManager.flush();

		this.vacancies.mirror(AtsEnum.GREENHOUSE, "stripe", List.of());
		this.entityManager.flush();

		assertThat(this.repository.count()).isZero();
	}

	private long normalizedId(long vacancyId) {
		return ((Number) this.entityManager.getEntityManager()
			.createNativeQuery("select id from normalized_vacancy where vacancy_id = :vacancyId")
			.setParameter("vacancyId", vacancyId)
			.getSingleResult()).longValue();
	}

	private static CleanedVacancy cleaned() {
		return new CleanedVacancy(new CleanedTitle("Engineer", Set.of()), List.of(), null);
	}

	private PublishedVacancy published(long externalId) {
		return new PublishedVacancy(String.valueOf(externalId), "Senior Backend Engineer", "Remote - Americas", "Engineering",
				"Ship payments.", "en", "https://job-boards.greenhouse.io/stripe/jobs/" + externalId, null, null, null, null,
				null, Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-18T16:30:00Z"), null, null, null);
	}

}
