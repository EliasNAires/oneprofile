package oneprofile.backend.storage.rolenearness;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import oneprofile.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, RoleNearnessStore.class })
class RoleNearnessStoreTest {

	@Autowired
	private RoleNearnessStore nearness;

	@Autowired
	private RoleNearnessRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void holdsWhatOneRunComputedAndNothingTheLastRunDid() {
		this.nearness.replace(List.of(new RoleNearness("data-engineer", "data-scientist", 0.8),
				new RoleNearness("data-engineer", "frontend-engineer", 0.1)));
		this.entityManager.flush();

		assertThat(this.nearness.replace(List.of(new RoleNearness("data-engineer", "data-scientist", 0.7))))
			.isEqualTo(1);
		this.entityManager.flush();
		this.entityManager.clear();

		assertThat(this.repository.findAll()).extracting(RoleNearnessEntity::nearness)
			.containsExactly(new RoleNearness("data-engineer", "data-scientist", 0.7));
	}

}
