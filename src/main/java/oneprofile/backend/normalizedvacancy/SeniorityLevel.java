package oneprofile.backend.normalizedvacancy;

/**
 * The experience level a vacancy asks for, on one ordinal scale. Declared from least to most
 * experienced, which is the order the scale is read in.
 * <p>
 * Only the levels some step can name are here. The words that need a reading of the job — and with
 * them the levels below junior and above senior that only those words carry — belong to the steps
 * that read them, and their levels arrive with those steps.
 */
public enum SeniorityLevel {

	/** Named by {@code junior} and {@code jr}. */
	JUNIOR,

	/** Named by {@code semi senior} and {@code ssr}, which ask for less than a senior. */
	MID,

	/** Named by {@code senior} and {@code sr}. */
	SENIOR,

	/** Named by {@code principal}. */
	PRINCIPAL

}
