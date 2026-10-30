package oneprofile.backend.workers.eligibility;

import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;

/**
 * A vacancy's eligibility state for one person's country, and where in the vacancy the set that decided it was
 * read.
 *
 * @param state its eligibility state
 * @param source where the stated set that decided it was read, null where none decided it
 */
public record Eligibility(EligibilityStateEnum state, StatedSetSourceEnum source) {

	/** What a vacancy that says nothing deciding is. */
	public static final Eligibility UNKNOWN = new Eligibility(EligibilityStateEnum.UNKNOWN, null);

}
