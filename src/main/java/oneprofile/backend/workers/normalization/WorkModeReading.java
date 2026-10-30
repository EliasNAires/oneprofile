package oneprofile.backend.workers.normalization;

import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;

/**
 * The work modes the work-mode rule read for one vacancy, and which step of the order of evidence
 * decided them.
 * @param modes its work modes: one, or every mode it offers a choice of; empty where nothing declares one
 * @param evidence the step that decided them, or {@link WorkModeEvidenceEnum#NONE}
 */
public record WorkModeReading(Set<WorkModeEnum> modes, WorkModeEvidenceEnum evidence) {

	/** What is read where nothing declares a mode. */
	public static final WorkModeReading NONE = new WorkModeReading(Set.of(), WorkModeEvidenceEnum.NONE);

	public WorkModeReading {
		modes = Set.copyOf(modes);
	}

}
