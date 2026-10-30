package oneprofile.backend.workers.eligibility;

/** The three answers eligibility can take (ADR-0008, ADR-0019), best first. */
public enum EligibilityStateEnum {

	/** The vacancy says it engages someone located there. */
	ELIGIBLE,

	/** The vacancy says nothing that decides it, which is expected to be the most common answer. */
	UNKNOWN,

	/** The vacancy says where it engages someone, and the place asked is not one of them. */
	EXCLUDED

}
