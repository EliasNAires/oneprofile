package oneprofile.backend.storage.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import oneprofile.backend.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, CompanyStore.class })
class CompanyStoreTest {

	private static final Instant PROBED_AT = Instant.parse("2026-09-19T10:15:30Z");

	@Autowired
	private CompanyStore companies;

	@Autowired
	private CompanyRepository repository;

	@Test
	void holdsOneCompanyPerSlugItIsGiven() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US));

		assertThat(this.repository.slugsOf(AtsEnum.GREENHOUSE)).containsExactlyInAnyOrder("stripe", "notion");
	}

	@Test
	void recordsNothingTheSecondTimeItIsGivenTheSameSlugs() {
		Map<String, RegionEnum> discovered = Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US);
		this.companies.record(AtsEnum.GREENHOUSE, discovered);

		assertThat(this.companies.record(AtsEnum.GREENHOUSE, discovered)).isZero();
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	void recordsWhatAProbeFoundOnTheCompanyItProbed() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US));

		this.companies.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);

		CompanyEntity stripe = this.repository.findByAtsAndSlug(AtsEnum.GREENHOUSE, "stripe").orElseThrow();
		assertThat(stripe.boardStatus()).isEqualTo(BoardStatusEnum.ACTIVE);
		assertThat(stripe.name()).isEqualTo("Stripe");
		assertThat(stripe.probedAt()).isEqualTo(PROBED_AT);
	}

	@Test
	void keepsTheNameItAlreadyHasWhenALaterProbeBringsNone() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US));
		this.companies.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);

		Instant later = PROBED_AT.plusSeconds(3600);
		this.companies.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.EMPTY, null, later);

		CompanyEntity stripe = this.repository.findByAtsAndSlug(AtsEnum.GREENHOUSE, "stripe").orElseThrow();
		assertThat(stripe.name()).isEqualTo("Stripe");
		assertThat(stripe.boardStatus()).isEqualTo(BoardStatusEnum.EMPTY);
		assertThat(stripe.probedAt()).isEqualTo(later);
		assertThat(this.repository.count()).isEqualTo(1);
	}

	@Test
	void refusesToRecordAProbeOfASlugItDoesNotHold() {
		assertThatThrownBy(() -> this.companies.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe",
				PROBED_AT))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("stripe");
	}

	@Test
	void readsTheSlugsItHoldsInSlugOrder() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US));

		assertThat(this.companies.regionsOf(AtsEnum.GREENHOUSE).keySet()).containsExactly("notion", "stripe");
	}

	@Test
	void readsOnlyTheSlugsWhoseBoardWasLastFoundActive() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US, "gone",
				RegionEnum.US, "unprobed", RegionEnum.US));
		this.companies.recordProbe(AtsEnum.GREENHOUSE, "stripe", BoardStatusEnum.ACTIVE, "Stripe", PROBED_AT);
		this.companies.recordProbe(AtsEnum.GREENHOUSE, "notion", BoardStatusEnum.EMPTY, null, PROBED_AT);
		this.companies.recordProbe(AtsEnum.GREENHOUSE, "gone", BoardStatusEnum.NOT_FOUND, null, PROBED_AT);

		assertThat(this.companies.regionsOf(AtsEnum.GREENHOUSE, BoardStatusEnum.ACTIVE))
			.containsExactly(entry("stripe", RegionEnum.US));
	}

	@Test
	void recordsOnlyTheSlugsItDoesNotHoldYet() {
		this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US));

		assertThat(this.companies.record(AtsEnum.GREENHOUSE, Map.of("stripe", RegionEnum.US, "notion", RegionEnum.US)))
			.isEqualTo(1);
		assertThat(this.repository.slugsOf(AtsEnum.GREENHOUSE)).containsExactlyInAnyOrder("stripe", "notion");
	}

	@Test
	void holdsEachCompanyInTheRegionItsBoardWasDiscoveredIn() {
		this.companies.record(AtsEnum.LEVER, Map.of("finix", RegionEnum.US, "kwalee", RegionEnum.EU));

		assertThat(this.companies.regionsOf(AtsEnum.LEVER)).containsExactly(entry("finix", RegionEnum.US),
				entry("kwalee", RegionEnum.EU));
	}

	@Test
	void keepsTheRegionFirstRecordedWhenTheSlugIsDiscoveredInTheOther() {
		this.companies.record(AtsEnum.LEVER, Map.of("leverdemo", RegionEnum.US));

		assertThat(this.companies.record(AtsEnum.LEVER, Map.of("leverdemo", RegionEnum.EU))).isZero();
		assertThat(this.companies.regionsOf(AtsEnum.LEVER)).containsExactly(entry("leverdemo", RegionEnum.US));
	}

}
