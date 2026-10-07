package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PieceRuleTest {

	@ParameterizedTest
	@ValueSource(strings = { "Kubernetes", "You", "C++", "C#", "node.js", "k8s", "5+", "2024", "Ética" })
	void takesAWordCapitalizedOrWithADigitOrASymbolAfterItsFirstLetterForANameLikeOne(String word) {
		assertThat(PieceRule.nameLike(word)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "experience", "full-time", "don't", "iPhone", "eBay", "5", "エンジニア" })
	void takesNoOtherWordForANameLikeOne(String word) {
		assertThat(PieceRule.nameLike(word)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "experience", "también", "go" })
	void takesAWordOfLowercaseLettersOnlyForALowercaseOne(String word) {
		assertThat(PieceRule.lowercase(word)).isTrue();
	}

	@Test
	void countsEveryNameLikeOccurrenceAndTheLowercaseOnesOfAWordOutsideTheKnownVocabulary() {
		assertThat(PieceRule.pieces("experience", 3, 40, 9)).isEqualTo(43);
		assertThat(PieceRule.pieces("experience", 3, 40, 10)).isEqualTo(3);
	}

	@Test
	void takesNoWordWithALetterOutsideASCIIForKnownVocabularyAsTheScriptDid() {
		assertThat(PieceRule.pieces("también", 3, 40, 10)).isEqualTo(43);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Go", "full-time", "don't", "k8s", "iPhone", "エンジニア" })
	void takesNoOtherWordForALowercaseOne(String word) {
		assertThat(PieceRule.lowercase(word)).isFalse();
	}

}
