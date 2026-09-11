package oneprofile.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class VacancyRepositoryTest {

	@Autowired
	private VacancyRepository vacancies;

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesAndReadsBackAnOpening() {
		Company company = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		Vacancy vacancy = new Vacancy(company, 8172510L);
		vacancy.describe("Backend Engineer", "Buenos Aires, Argentina", "Engineering", "Java and Spring",
				"https://job-boards.greenhouse.io/globant/jobs/8172510", "en", 12000000L, 18000000L, "USD",
				"Annual base salary range:", Instant.parse("2026-09-09T14:50:29Z"),
				Instant.parse("2026-09-10T17:11:58Z"));
		Long id = this.vacancies.save(vacancy).getId();
		this.entityManager.flush();
		this.entityManager.clear();

		Vacancy found = this.vacancies.findById(id).orElseThrow();

		assertThat(found.getCompany().getSlug()).isEqualTo("globant");
		assertThat(found.getExternalId()).isEqualTo(8172510L);
		assertThat(found.getTitle()).isEqualTo("Backend Engineer");
		assertThat(found.getLocation()).isEqualTo("Buenos Aires, Argentina");
		assertThat(found.getDepartment()).isEqualTo("Engineering");
		assertThat(found.getLanguage()).isEqualTo("en");
		assertThat(found.getPayMinCents()).isEqualTo(12000000L);
		assertThat(found.getPayMaxCents()).isEqualTo(18000000L);
		assertThat(found.getPayCurrency()).isEqualTo("USD");
		assertThat(found.getPayTitle()).isEqualTo("Annual base salary range:");
		assertThat(found.getFirstPublished()).isEqualTo(Instant.parse("2026-09-09T14:50:29Z"));
		assertThat(found.getUpdatedAt()).isEqualTo(Instant.parse("2026-09-10T17:11:58Z"));
	}

	@Test
	void keepsADescriptionLongerThanAVarchar() {
		Company company = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		// The measured median is around 5.000 characters of plain text.
		String description = "requisitos ".repeat(600);
		Vacancy vacancy = new Vacancy(company, 4001L);
		vacancy.describe("Data Analyst", null, null, description, null, null, null, null, null, null, null, null);
		Long id = this.vacancies.save(vacancy).getId();
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.vacancies.findById(id).orElseThrow().getDescription()).isEqualTo(description);
	}

	@Test
	void findsTheOpeningsOfOneCompanyOnly() {
		Company globant = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		Company splice = this.companies.save(new Company(Ats.GREENHOUSE, "splice"));
		this.vacancies.save(new Vacancy(globant, 4001L));
		this.vacancies.save(new Vacancy(splice, 4002L));
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.vacancies.findByCompany(globant)).extracting(Vacancy::getExternalId)
				.containsExactly(4001L);
	}

	@Test
	void rejectsTheSameExternalIdTwiceWithinACompany() {
		Company company = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		this.vacancies.saveAndFlush(new Vacancy(company, 4001L));

		assertThatThrownBy(() -> this.vacancies.saveAndFlush(new Vacancy(company, 4001L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void acceptsTheSameExternalIdInTwoCompanies() {
		// The id of Greenhouse is unique within a board, not across boards.
		Company globant = this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		Company splice = this.companies.save(new Company(Ats.GREENHOUSE, "splice"));

		this.vacancies.saveAndFlush(new Vacancy(globant, 4001L));
		this.vacancies.saveAndFlush(new Vacancy(splice, 4001L));

		assertThat(this.vacancies.count()).isEqualTo(2);
	}
}
