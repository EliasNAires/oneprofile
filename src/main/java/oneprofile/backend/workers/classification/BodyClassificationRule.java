package oneprofile.backend.workers.classification;

import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import org.springframework.stereotype.Component;

/**
 * Decides from its description whether a vacancy of the pile, one its title left unknown for a
 * corpus reason, names an engineering role. The criterion it answers to is
 * {@code docs/engineering-role-criterion.md}; how it is scored is ADR-0012.
 * <p>
 * No rule has been written yet, so it decides nothing: every vacancy stays as the title left it.
 * The loop of #11 writes the rules.
 */
@Component
public class BodyClassificationRule {

	/**
	 * Decides one vacancy of the pile.
	 * @param cleanedTitle its cleaned title
	 * @param cleanedDescription its description once cleaned, empty if it has none
	 * @return {@code IN} or {@code OUT} where the description settles it, {@code UNKNOWN} otherwise
	 */
	public ClassificationStateEnum classify(String cleanedTitle, String cleanedDescription) {
		return ClassificationStateEnum.UNKNOWN;
	}

}
