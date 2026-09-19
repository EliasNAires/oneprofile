package oneprofile.backend.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import oneprofile.backend.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, CompanyConfiguration.class })
class CompaniesTest {

	@Autowired
	private Companies companies;

	@Autowired
	private CompanyRepository repository;

	@Test
	void holdsOneCompanyPerSlugItIsGiven() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe", "notion"));

		assertThat(this.repository.slugsOf(Ats.GREENHOUSE)).containsExactlyInAnyOrder("stripe", "notion");
	}

	@Test
	void recordsNothingTheSecondTimeItIsGivenTheSameSlugs() {
		List<String> discovered = List.of("stripe", "notion");
		this.companies.record(Ats.GREENHOUSE, discovered);

		assertThat(this.companies.record(Ats.GREENHOUSE, discovered)).isZero();
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void recordsOnlyTheSlugsItDoesNotHoldYet() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe"));

		assertThat(this.companies.record(Ats.GREENHOUSE, List.of("stripe", "notion"))).isEqualTo(1);
		assertThat(this.repository.slugsOf(Ats.GREENHOUSE)).containsExactlyInAnyOrder("stripe", "notion");
	}

}
