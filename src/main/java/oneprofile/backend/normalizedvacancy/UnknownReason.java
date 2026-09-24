package oneprofile.backend.normalizedvacancy;

/**
 * Why a title was left unknown. The reason is what tells the next reader whether the title landed
 * there because of the rules or because of the world: only {@link #UNRULED} is ours to fix, and it
 * is the only share expected to fall as the rules grow.
 */
public enum UnknownReason {

	/** No rule reaches this title. Ours — a line added to a list fixes it. */
	UNRULED,

	/** The title means different work in different domains, and does not say which. The corpus's. */
	DOMAIN_AMBIGUITY,

	/** The domain is clear, the expertise the role needs is not. The corpus's. */
	SCOPE_AMBIGUITY

}
