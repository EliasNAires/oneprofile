package oneprofile.backend.storage.normalizedvacancy;

/**
 * Which signal decided a vacancy's classification state. Downstream reads the one state and never
 * this; it is what tells a later pass which rules a state answers to.
 */
public enum ClassificationSignalEnum {

	/** The title decided, or left it unknown. Every vacancy classification reaches starts here. */
	TITLE,

	/** The description decided a vacancy its title left unknown for a corpus reason (ADR-0012). */
	BODY

}
