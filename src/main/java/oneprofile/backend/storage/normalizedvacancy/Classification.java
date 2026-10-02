package oneprofile.backend.storage.normalizedvacancy;

import java.util.Set;

/**
 * What classification made of one vacancy: the state, the reason its title left it unknown, and the
 * signal that decided. The title decides first, and a state it decides carries no reason, because
 * there is nothing left unanswered to give one for. The body decides only what the title left
 * unknown for a corpus reason, and keeps that reason, so a vacancy it decided stays recognisable as
 * one of the pile.
 *
 * @param state what classification answers about the vacancy
 * @param reason why the title was left unknown, null unless it was
 * @param signal what decided the state
 */
public record Classification(ClassificationStateEnum state, UnknownReasonEnum reason,
		ClassificationSignalEnum signal) {

	/** The reasons that put a vacancy its title left unknown in the pile the body pass reads. */
	public static final Set<UnknownReasonEnum> PILE_REASONS = Set.of(UnknownReasonEnum.DOMAIN_AMBIGUITY,
			UnknownReasonEnum.SCOPE_AMBIGUITY);

	public Classification {
		if (signal == ClassificationSignalEnum.TITLE && (state == ClassificationStateEnum.UNKNOWN) != (reason != null)) {
			throw new IllegalArgumentException("An unknown title carries a reason and a decided one does not");
		}
		if (signal == ClassificationSignalEnum.BODY
				&& (state == ClassificationStateEnum.UNKNOWN || !PILE_REASONS.contains(reason))) {
			throw new IllegalArgumentException("The body decides only what the title left unknown for a corpus reason");
		}
	}

	/** The vacancy is an engineering role. */
	public static Classification in() {
		return new Classification(ClassificationStateEnum.IN, null, ClassificationSignalEnum.TITLE);
	}

	/** It is not, and the title says so. */
	public static Classification out() {
		return new Classification(ClassificationStateEnum.OUT, null, ClassificationSignalEnum.TITLE);
	}

	/**
	 * The title does not carry enough to decide.
	 * @param reason why it does not
	 * @return the undecided classification
	 */
	public static Classification unknown(UnknownReasonEnum reason) {
		return new Classification(ClassificationStateEnum.UNKNOWN, reason, ClassificationSignalEnum.TITLE);
	}

	/**
	 * The description decided a vacancy its title left unknown.
	 * @param state what the body decided, {@code IN} or {@code OUT}
	 * @param titleReason why the title left it unknown
	 * @return the classification the body decided
	 */
	public static Classification byBody(ClassificationStateEnum state, UnknownReasonEnum titleReason) {
		return new Classification(state, titleReason, ClassificationSignalEnum.BODY);
	}
}
