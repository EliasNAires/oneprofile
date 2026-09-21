package oneprofile.backend.normalizedvacancy;

/**
 * What classification answers about a vacancy. {@code UNKNOWN} is the default and not a softer
 * {@code OUT}: it says nothing earned a decision, where {@code IN} and {@code OUT} are claims a
 * rule has to have earned.
 */
public enum ClassificationState {

	/** The vacancy is an engineering role. */
	IN,

	/** It is not, and the title says so. */
	OUT,

	/** The title does not carry enough to decide, and the reason says why. */
	UNKNOWN

}
