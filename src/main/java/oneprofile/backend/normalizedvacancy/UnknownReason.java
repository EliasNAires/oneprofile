package oneprofile.backend.normalizedvacancy;

/**
 * Why a title was left unknown. The reason is what tells the next reader whether the title landed
 * there because of the rules or because of the world: only {@link #UNRULED} is ours to fix, and it
 * is the only share expected to fall as the rules grow.
 */
public enum UnknownReason {

	/** No rule reaches this title. Ours — a line added to a list fixes it. */
	UNRULED,

	/** The head is known, the domain is not. The corpus's. */
	DOMAIN_AMBIGUITY,

	/** A ruled phrase whose variants genuinely split. The corpus's. */
	SCOPE_AMBIGUITY

}
