package oneprofile.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BlacklistedSlug;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BlacklistedSlugRepositoryTest {

	@Autowired
	private BlacklistedSlugRepository blacklistedSlugs;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findsTheSlugsOfAnAts() {
		blacklistedSlugs.save(new BlacklistedSlug(Ats.GREENHOUSE, "mercadol"));
		blacklistedSlugs.save(new BlacklistedSlug(Ats.GREENHOUSE, "globan"));
		entityManager.flush();
		entityManager.clear();

		assertThat(blacklistedSlugs.findSlugsByAts(Ats.GREENHOUSE))
				.containsExactlyInAnyOrder("mercadol", "globan");
	}

	@Test
	void rejectsSameSlugTwiceWithinAnAts() {
		blacklistedSlugs.saveAndFlush(new BlacklistedSlug(Ats.GREENHOUSE, "mercadol"));

		assertThatThrownBy(() -> blacklistedSlugs.saveAndFlush(new BlacklistedSlug(Ats.GREENHOUSE, "mercadol")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
