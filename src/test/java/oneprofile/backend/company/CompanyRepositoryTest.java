package oneprofile.backend.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
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
	void rejectsSameSlugTwiceWithinAnAts() {
		companies.saveAndFlush(new Company(Ats.GREENHOUSE, "mercadolibre"));

		assertThatThrownBy(() -> companies.saveAndFlush(new Company(Ats.GREENHOUSE, "mercadolibre")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
