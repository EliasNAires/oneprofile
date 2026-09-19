package oneprofile.backend.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
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

	private static final Instant PROBED_AT = Instant.parse("2026-09-19T10:15:30Z");

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
	void recordsWhatAProbeFoundOnTheCompanyItProbed() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe"));

		this.companies.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe", PROBED_AT);

		Company stripe = this.repository.findByAtsAndSlug(Ats.GREENHOUSE, "stripe").orElseThrow();
		assertThat(stripe.boardStatus()).isEqualTo(BoardStatus.ACTIVE);
		assertThat(stripe.name()).isEqualTo("Stripe");
		assertThat(stripe.probedAt()).isEqualTo(PROBED_AT);
	}

	@Test
	void keepsTheNameItAlreadyHasWhenALaterProbeBringsNone() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe"));
		this.companies.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe", PROBED_AT);

		Instant later = PROBED_AT.plusSeconds(3600);
		this.companies.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.EMPTY, null, later);

		Company stripe = this.repository.findByAtsAndSlug(Ats.GREENHOUSE, "stripe").orElseThrow();
		assertThat(stripe.name()).isEqualTo("Stripe");
		assertThat(stripe.boardStatus()).isEqualTo(BoardStatus.EMPTY);
		assertThat(stripe.probedAt()).isEqualTo(later);
		assertThat(this.repository.count()).isEqualTo(1);
	}

	@Test
	void refusesToRecordAProbeOfASlugItDoesNotHold() {
		assertThatThrownBy(() -> this.companies.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe",
				PROBED_AT))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("stripe");
	}

	@Test
	void readsTheSlugsItHoldsInSlugOrder() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe", "notion"));

		assertThat(this.companies.slugsOf(Ats.GREENHOUSE)).containsExactly("notion", "stripe");
	}

	@Test
	void readsOnlyTheSlugsWhoseBoardWasLastFoundActive() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe", "notion", "gone", "unprobed"));
		this.companies.recordProbe(Ats.GREENHOUSE, "stripe", BoardStatus.ACTIVE, "Stripe", PROBED_AT);
		this.companies.recordProbe(Ats.GREENHOUSE, "notion", BoardStatus.EMPTY, null, PROBED_AT);
		this.companies.recordProbe(Ats.GREENHOUSE, "gone", BoardStatus.NOT_FOUND, null, PROBED_AT);

		assertThat(this.companies.slugsOf(Ats.GREENHOUSE, BoardStatus.ACTIVE)).containsExactly("stripe");
	}

	@Test
	void recordsOnlyTheSlugsItDoesNotHoldYet() {
		this.companies.record(Ats.GREENHOUSE, List.of("stripe"));

		assertThat(this.companies.record(Ats.GREENHOUSE, List.of("stripe", "notion"))).isEqualTo(1);
		assertThat(this.repository.slugsOf(Ats.GREENHOUSE)).containsExactlyInAnyOrder("stripe", "notion");
	}

}
