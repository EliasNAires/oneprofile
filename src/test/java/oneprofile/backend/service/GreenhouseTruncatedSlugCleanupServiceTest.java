package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import oneprofile.backend.model.Vacancy;
import oneprofile.backend.repository.BlacklistedSlugRepository;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.VacancyRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class GreenhouseTruncatedSlugCleanupServiceTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private VacancyRepository vacancies;

	@Autowired
	private BlacklistedSlugRepository blacklistedSlugs;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void removesANotFoundCompanyThatIsAProperPrefixOfAnActiveOne() {
		company("mercadol", BoardStatus.NOT_FOUND);
		company("mercadolibre", BoardStatus.ACTIVE);

		int removed = service().cleanup();

		assertThat(removed).isEqualTo(1);
		assertThat(this.companies.findByAtsAndSlug(Ats.GREENHOUSE, "mercadol")).isEmpty();
		assertThat(this.companies.findByAtsAndSlug(Ats.GREENHOUSE, "mercadolibre")).isPresent();
		assertThat(this.blacklistedSlugs.findSlugsByAts(Ats.GREENHOUSE)).containsExactly("mercadol");
	}

	@Test
	void keepsAnUnderscoreLiteralInsteadOfTreatingItAsAWildcard() {
		company("mercado_", BoardStatus.NOT_FOUND);
		company("mercadoX", BoardStatus.ACTIVE);

		int removed = service().cleanup();

		assertThat(removed).isEqualTo(0);
		assertThat(this.companies.findByAtsAndSlug(Ats.GREENHOUSE, "mercado_")).isPresent();
		assertThat(this.blacklistedSlugs.findSlugsByAts(Ats.GREENHOUSE)).isEmpty();
	}

	@Test
	void doesNotRemoveAPrefixOfAnEmptyOrOfAnotherNotFoundCompany() {
		company("acme", BoardStatus.NOT_FOUND);
		company("acmecorp", BoardStatus.EMPTY);
		company("acmeinc", BoardStatus.NOT_FOUND);

		int removed = service().cleanup();

		assertThat(removed).isEqualTo(0);
		assertThat(this.companies.findByAtsAndSlug(Ats.GREENHOUSE, "acme")).isPresent();
		assertThat(this.blacklistedSlugs.findSlugsByAts(Ats.GREENHOUSE)).isEmpty();
	}

	@Test
	void removesTheVacanciesOfARemovedCompanyTogetherWithIt() {
		Company truncated = company("mercadol", BoardStatus.NOT_FOUND);
		company("mercadolibre", BoardStatus.ACTIVE);
		Vacancy vacancy = new Vacancy(truncated, 1L);
		this.vacancies.save(vacancy);
		this.entityManager.flush();

		int removed = service().cleanup();

		assertThat(removed).isEqualTo(1);
		assertThat(this.vacancies.findById(vacancy.getId())).isEmpty();
	}

	private Company company(String slug, BoardStatus status) {
		Company company = new Company(Ats.GREENHOUSE, slug);
		company.recordProbe(status, null, Instant.now());
		this.companies.save(company);
		this.entityManager.flush();
		return company;
	}

	private GreenhouseTruncatedSlugCleanupService service() {
		return new GreenhouseTruncatedSlugCleanupService(this.companies, this.vacancies, this.blacklistedSlugs);
	}
}
