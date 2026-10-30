package oneprofile.backend.workers.normalization;

/** Which step of the order of evidence decided a vacancy's work mode, if any did (#86, #98). */
public enum WorkModeEvidenceEnum {

	/** The location text names a mode. */
	LOCATION,

	/** The work-mode field its ATS publishes: Ashby's and Lever's workplace type, Workday's remote type. */
	FIELD,

	/** The title names a mode, by the work-mode words the title breakdown takes out of it. */
	TITLE,

	/** The description states this vacancy's mode, by a sentence about the role, a label or a tag (#98). */
	DESCRIPTION,

	/** Nothing it reads declares a mode, so it has none. */
	NONE

}
