package oneprofile.backend.workers.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.junit.jupiter.api.Test;

class DescriptionSimilarityRuleTest {

	private final DescriptionSimilarityRule rule = new DescriptionSimilarityRule();

	/** A substantial description whose changing details are supplied by each test. */
	private static final String DESCRIPTION = "Design reliable distributed systems and build scalable services with our engineering team "
			+ "while improving production performance through careful testing monitoring collaboration architecture reviews "
			+ "and thoughtful technical decisions for customers around the world";

	@Test
	void stripsLocationsPayDatesAndRequisitionNumbers() {
		long[] a = shingles(DESCRIPTION + " London USD 100000 January 12 2025 Monday req123", "London");
		long[] b = shingles(DESCRIPTION + " Paris euros 90000 Feb 23 2026 Tuesday req456", "Paris");

		assertThat(this.rule.overlap(a, b)).isEqualTo(1.0);
		assertThat(this.rule.comparable(a)).isTrue();
	}

	@Test
	void descriptionsNamingEachOthersCitiesOverlapFullyWithBothLocations() {
		List<String> locations = List.of("London", "Paris");
		long[] a = this.rule.shingles(List.of(segment(DESCRIPTION + " Paris", false)), locations);
		long[] b = this.rule.shingles(List.of(segment(DESCRIPTION + " London", false)), locations);

		assertThat(this.rule.overlap(a, b)).isEqualTo(1.0);
	}

	@Test
	void leavesOutBoilerplateButFallsBackWhenEverySegmentIsBoilerplate() {
		long[] expected = shingles(DESCRIPTION, "");
		assertThat(this.rule.shingles(List.of(segment(DESCRIPTION, false), segment("unrelated boilerplate", true)), List.of()))
			.isEqualTo(expected);
		assertThat(this.rule.shingles(List.of(segment(DESCRIPTION, true)), List.of())).isEqualTo(expected);
	}

	@Test
	void thinDescriptionsNeverOverlap() {
		long[] thin = shingles("short description of work", "");
		assertThat(this.rule.comparable(thin)).isFalse();
		assertThat(this.rule.overlap(thin, thin)).isZero();
	}

	@Test
	void differentDescriptionsOverlapLittleAndOverlapIsSymmetric() {
		long[] a = shingles(DESCRIPTION, "");
		long[] b = shingles("Prepare financial statements audit annual accounts reconcile payments track invoices "
				+ "manage budgets forecast revenue calculate taxes report expenses coordinate payroll "
				+ "maintain records review transactions ensure compliance support accountants", "");
		assertThat(this.rule.overlap(a, b)).isLessThan(0.2);
		assertThat(this.rule.overlap(a, b)).isEqualTo(this.rule.overlap(b, a));
	}

	/** Reads one description with the vacancy's own location. */
	private long[] shingles(String text, String location) {
		return this.rule.shingles(List.of(segment(text, false)), List.of(location));
	}

	/** One cleaned sentence with its boilerplate mark. */
	private Segment segment(String text, boolean boilerplate) {
		return new Segment(SegmentKindEnum.SENTENCE, null, text, boilerplate);
	}

}
