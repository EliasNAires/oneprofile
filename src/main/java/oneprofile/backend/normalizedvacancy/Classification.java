package oneprofile.backend.normalizedvacancy;

/**
 * What classification made of one title: the state, and where the state is unknown, the reason it
 * is. A decided state carries no reason, because there is nothing left unanswered to give one for.
 *
 * @param state what classification answers about the vacancy
 * @param reason why the title was left unknown, null unless it was
 */
public record Classification(ClassificationState state, UnknownReason reason) {

	public Classification {
		if ((state == ClassificationState.UNKNOWN) != (reason != null)) {
			throw new IllegalArgumentException("An unknown state carries a reason and a decided one does not");
		}
	}

	/** The vacancy is an engineering role. */
	public static Classification in() {
		return new Classification(ClassificationState.IN, null);
	}

	/** It is not, and the title says so. */
	public static Classification out() {
		return new Classification(ClassificationState.OUT, null);
	}

	/**
	 * The title does not carry enough to decide.
	 * @param reason why it does not
	 * @return the undecided classification
	 */
	public static Classification unknown(UnknownReason reason) {
		return new Classification(ClassificationState.UNKNOWN, reason);
	}

}
