package oneprofile.backend.storage.normalizedvacancy;

/**
 * Why a vacancy was left unknown: why its title was, or, for one of the pile, why the body pass did
 * not read it. The reason is what tells the next reader whether the title landed
 * there because of the rules or because of the world: only {@link #UNRULED} is ours to fix, and it
 * is the only share expected to fall as the rules grow.
 */
public enum UnknownReasonEnum {

	/** No rule reaches this title. Ours — a line added to a list fixes it. */
	UNRULED,

	/** The title means different work in different domains, and does not say which. The corpus's. */
	DOMAIN_AMBIGUITY,

	/** The domain is clear, the expertise the role needs is not. The corpus's. */
	SCOPE_AMBIGUITY,

	/**
	 * The vacancy is in the pile but written in a language other than English or Spanish, so the body
	 * pass does not read it. Given by the body pass in place of the title's reason.
	 */
	UNSUPPORTED_LANGUAGE

}
