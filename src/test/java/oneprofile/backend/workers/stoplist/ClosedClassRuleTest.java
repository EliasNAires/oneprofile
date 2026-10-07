package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClosedClassRuleTest {

	@ParameterizedTest
	@ValueSource(strings = { "the", "you", "we", "if", "this", "with", "will", "you'll", "you’ll", "el", "para", "y",
			"está" })
	void takesTheFunctionWordsOfEnglishAndSpanish(String word) {
		assertThat(ClosedClassRule.closedClass(word)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "experience", "strong", "go", "c", "r", "can", "es", "os", "ha", "se", "excel", "der" })
	void takesNoOtherWordAndNoneASkillCouldBe(String word) {
		assertThat(ClosedClassRule.closedClass(word)).isFalse();
	}

}
