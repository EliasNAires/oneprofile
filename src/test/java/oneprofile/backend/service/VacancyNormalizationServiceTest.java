package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.NormalizedVacancy;
import oneprofile.backend.model.Seniority;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.model.WorkMode;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.NormalizedVacancyRepository;
import oneprofile.backend.repository.VacancyRepository;
import oneprofile.backend.service.VacancyNormalizationService.NormalizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class VacancyNormalizationServiceTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private VacancyRepository vacancies;

	@Autowired
	private NormalizedVacancyRepository normalized;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private TestEntityManager entityManager;

	private Company company;

	@BeforeEach
	void storeCompany() {
		this.company = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
	}

	@Test
	void keepsTheTitleWithItsSeniorityAndWorkModeTakenOut() {
		store(4001L, "Sr. Software Engineer (Remote)", "Remote");

		NormalizationResult result = service().normalizeAll();

		assertThat(result).isEqualTo(new NormalizationResult(1, 0));
		assertThat(this.normalized.findAll())
			.extracting(NormalizedVacancy::getTitle, NormalizedVacancy::getSeniority, NormalizedVacancy::getWorkMode)
			.containsExactly(tuple("software engineer", Seniority.SENIOR, WorkMode.FULLY_REMOTE));
	}

	@Test
	void walksEveryPage() {
		for (long id = 4001L; id <= 4005L; id++) {
			store(id, "Backend Engineer", "Buenos Aires, Argentina");
		}

		NormalizationResult result = service().normalizeAll();

		assertThat(result).isEqualTo(new NormalizationResult(5, 0));
		assertThat(this.normalized.count()).isEqualTo(5);
	}

	@Test
	void normalizingAllAgainUpdatesInsteadOfDuplicating() {
		Vacancy vacancy = store(4001L, "Backend Engineer", "Buenos Aires, Argentina");
		store(4002L, "Data Analyst", "Hybrid - London");
		service().normalizeAll();

		retitle(vacancy, "Junior Backend Engineer");
		NormalizationResult result = service().normalizeAll();

		assertThat(result).isEqualTo(new NormalizationResult(0, 2));
		assertThat(this.normalized.findAll()).extracting(NormalizedVacancy::getTitle, NormalizedVacancy::getSeniority)
			.containsExactlyInAnyOrder(tuple("backend engineer", Seniority.JUNIOR), tuple("data analyst", null));
	}

	@Test
	void normalizingTheMissingOnesLeavesTheRowsAlreadyThere() {
		Vacancy vacancy = store(4001L, "Backend Engineer", "Buenos Aires, Argentina");
		service().normalizeAll();

		retitle(vacancy, "Senior Backend Engineer");
		store(4002L, "Staff Data Engineer", "Remote - US");
		NormalizationResult result = service().normalizeMissing();

		assertThat(result).isEqualTo(new NormalizationResult(1, 0));
		assertThat(this.normalized.findAll())
			.extracting(NormalizedVacancy::getTitle, NormalizedVacancy::getSeniority, NormalizedVacancy::getWorkMode)
			.containsExactlyInAnyOrder(tuple("backend engineer", null, null),
					tuple("data engineer", Seniority.STAFF, WorkMode.REMOTE));
	}

	private Vacancy store(long externalId, String title, String location) {
		Vacancy vacancy = new Vacancy(this.company, externalId);
		vacancy.describe(title, location, null, null, null, null, null, null, null, null, null, null);
		this.vacancies.save(vacancy);
		this.entityManager.flush();
		return vacancy;
	}

	private void retitle(Vacancy vacancy, String title) {
		Vacancy stored = this.vacancies.findById(vacancy.getId()).orElseThrow();
		stored.describe(title, stored.getLocation(), null, null, null, null, null, null, null, null, null, null);
		this.entityManager.flush();
		this.entityManager.clear();
	}

	/** Pages of two, so that a handful of vacancies already spans more than one. */
	private VacancyNormalizationService service() {
		return new VacancyNormalizationService(this.vacancies, this.normalized, this.transactionManager, 2);
	}
}
