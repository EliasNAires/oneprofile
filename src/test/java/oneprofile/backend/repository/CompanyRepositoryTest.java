package oneprofile.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CompanyRepositoryTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesAndReadsBackAts() {
		Long id = companies.save(new Company(Ats.GREENHOUSE, "mercadolibre")).getId();
		entityManager.flush();
		entityManager.clear();

		Company found = companies.findById(id).orElseThrow();

		assertThat(found.getAts()).isEqualTo(Ats.GREENHOUSE);
		assertThat(found.getSlug()).isEqualTo("mercadolibre");
	}

	@Test
	void findsTheSlugsOfAnAts() {
		companies.save(new Company(Ats.GREENHOUSE, "mercadolibre"));
		companies.save(new Company(Ats.GREENHOUSE, "globant"));
		entityManager.flush();
		entityManager.clear();

		assertThat(companies.findSlugsByAts(Ats.GREENHOUSE))
				.containsExactlyInAnyOrder("mercadolibre", "globant");
	}

	@Test
	void findsTheSlugsWhoseBoardIsInAGivenState() {
		companies.save(probed("mercadolibre", BoardStatus.ACTIVE));
		companies.save(probed("globant", BoardStatus.ACTIVE));
		companies.save(probed("auth0", BoardStatus.EMPTY));
		companies.save(new Company(Ats.GREENHOUSE, "splice"));
		entityManager.flush();
		entityManager.clear();

		assertThat(companies.findSlugsByAtsAndBoardStatus(Ats.GREENHOUSE, BoardStatus.ACTIVE))
				.containsExactlyInAnyOrder("mercadolibre", "globant");
	}

	private static Company probed(String slug, BoardStatus status) {
		Company company = new Company(Ats.GREENHOUSE, slug);
		company.recordProbe(status, null, Instant.parse("2026-09-10T12:00:00Z"));
		return company;
	}

	@Test
	void rejectsSameSlugTwiceWithinAnAts() {
		companies.saveAndFlush(new Company(Ats.GREENHOUSE, "mercadolibre"));

		assertThatThrownBy(() -> companies.saveAndFlush(new Company(Ats.GREENHOUSE, "mercadolibre")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
