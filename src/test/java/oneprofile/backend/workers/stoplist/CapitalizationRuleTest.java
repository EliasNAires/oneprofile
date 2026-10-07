package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.junit.jupiter.api.Test;

class CapitalizationRuleTest {

	@Test
	void takesEveryWordButTheFirstOfASentenceOrAnItem() {
		assertThat(CapitalizationRule.midSentence(segment(SegmentKindEnum.SENTENCE, "Build APIs in Go")))
			.containsExactly("APIs", "in", "Go");
		assertThat(CapitalizationRule.midSentence(segment(SegmentKindEnum.ITEM, "Strong Python skills")))
			.containsExactly("Python", "skills");
	}

	@Test
	void leavesOutAWordThatStartsASentenceInsideTheSegment() {
		assertThat(CapitalizationRule.midSentence(segment(SegmentKindEnum.ITEM,
				"Location: Remote. We use Node.js! Really? Yes (Go). Next")))
			.containsExactly("use", "Node.js", "Go");
	}

	@Test
	void leavesOutAWordWithNoLetterThatHasACase() {
		assertThat(CapitalizationRule.midSentence(segment(SegmentKindEnum.SENTENCE, "Supports 802.11 and 10G")))
			.containsExactly("and", "10G");
	}

	@Test
	void takesNothingFromAHeading() {
		assertThat(CapitalizationRule.midSentence(segment(SegmentKindEnum.HEADING, "What You Will Do"))).isEmpty();
	}

	@Test
	void callsAWordCapitalizedWhenItsFirstLetterIsUpperCase() {
		assertThat(CapitalizationRule.capitalized("10G")).isTrue();
		assertThat(CapitalizationRule.capitalized("iSCSI")).isFalse();
		assertThat(CapitalizationRule.capitalized("Go")).isTrue();
		assertThat(CapitalizationRule.capitalized("AWS")).isTrue();
		assertThat(CapitalizationRule.capitalized("Élan")).isTrue();
		assertThat(CapitalizationRule.capitalized("k8s")).isFalse();
		assertThat(CapitalizationRule.capitalized("5g")).isFalse();
	}

	private static Segment segment(SegmentKindEnum kind, String text) {
		return new Segment(kind, null, text, false);
	}

}
