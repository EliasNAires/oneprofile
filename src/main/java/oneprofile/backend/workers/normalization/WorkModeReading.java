package oneprofile.backend.workers.normalization;

import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;

/**
 * The work mode the work-mode rule read for one vacancy, and which step of the order of evidence
 * decided it.
 * @param mode its work mode, or null where nothing declares one
 * @param evidence the step that decided it, or {@link WorkModeEvidenceEnum#NONE}
 */
public record WorkModeReading(WorkModeEnum mode, WorkModeEvidenceEnum evidence) {

	/** What is read where nothing declares a mode. */
	public static final WorkModeReading NONE = new WorkModeReading(null, WorkModeEvidenceEnum.NONE);

}
