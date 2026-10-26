package oneprofile.backend.storage.normalizedvacancy;

/**
 * The experience level a vacancy asks for, on one ordinal scale. Declared from least to most
 * experienced, which is the order the scale is read in.
 * <p>
 * Only the levels some step can name are here. The words that need a reading of the job — and with
 * them the levels below junior and above senior that only those words carry — belong to the steps
 * that read them, and their levels arrive with those steps.
 */
public enum SeniorityLevelEnum {

	/** Named by {@code entry} and {@code entry level}, read by the title breakdown. */
	ENTRY,

	/** Named by {@code junior} and {@code jr}. */
	JUNIOR,

	/**
	 * Named by {@code semi senior} and {@code ssr}, which ask for less than a senior, and by
	 * {@code mid}, {@code intermediate}, {@code medior} and {@code middle}, which the title
	 * breakdown reads.
	 */
	MID,

	/** Named by {@code senior} and {@code sr}. */
	SENIOR,

	/** Named by {@code staff}, read by the title breakdown. */
	STAFF,

	/** Named by {@code principal}. */
	PRINCIPAL,

	// The numbered levels, kept as the title names them. They have no place on the scale: whether
	// an Engineer II is junior or mid is not in the title, and placing them is the seniority step's
	// work (#8). They are declared last only because the scale above is read in order.

	/** Named by {@code II}. */
	II,

	/** Named by {@code III}. */
	III,

	/** Named by {@code IV}. */
	IV

}
