package oneprofile.backend.model;

/**
 * The experience level a vacancy asks for, taken out of its title.
 *
 * <p>The declaration order is meaningful: values go from the lowest level to the
 * highest, and that order is what {@code SeniorityExtractor} uses when a title names more
 * than one level —a vacancy publishes the floor it accepts, so the lowest one wins. The
 * {@code STAFF} / {@code PRINCIPAL} stretch is an adopted convention rather than a
 * measured fact: both are individual contributor track and their relative order varies
 * by company, but taking a minimum needs some order.
 */
public enum Seniority {

	/** Written as "entry level"; on its own, "entry" is usually a door or data entry. */
	ENTRY,

	/** "junior" or "jr". */
	JUNIOR,

	/**
	 * The step between junior and senior of the Spanish speaking market: "semi senior",
	 * "semisenior" or "ssr". Kept apart from {@link #MID} because they come from
	 * different ladders and equating them would invent a match the data does not state.
	 */
	SEMI_SENIOR,

	/** Written as "mid level", or "mid" closing the title; "mid market" is a segment. */
	MID,

	/** "senior" or "sr". */
	SENIOR,

	/** "staff" as a level, not as the people of a place ("chief of staff", "staff nurse"). */
	STAFF,

	/** "principal". */
	PRINCIPAL,

	/**
	 * The "ii" of "Engineer II", left untranslated on purpose: the titles do not say
	 * whether it means mid or junior, so naming it would invent an equivalence.
	 */
	LEVEL_2,

	/** The "iii" of "Engineer III", untranslated for the same reason as {@link #LEVEL_2}. */
	LEVEL_3
}
