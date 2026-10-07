package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuardSetRuleTest {

	@Test
	void takesTheNamesThatAreOneWordAsSegmentCutsThemLowercased() {
		assertThat(GuardSetRule.words(List.of("Go", "Node.js", "C#", ".NET", "Apache Spark", "S/4HANA", "datadog's")))
			.containsExactlyInAnyOrder("go", "node.js", "c#", "net", "datadog's");
	}

	@Test
	void holdsTheBareFormsReopenedFromTheDecisionRecord() {
		assertThat(GuardSetRule.REOPENED).hasSize(47).contains("make", "go", "sonnet", "joule");
	}

}
