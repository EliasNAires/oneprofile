package oneprofile.backend.storage.normalizedvacancy;

/**
 * Where a vacancy's work is performed, as the vacancy declares it (PRD §5.3, #86). A vacancy that
 * declares none has no work mode, which never means onsite.
 */
public enum WorkModeEnum {

	/** Done away from any office. */
	REMOTE,

	/** Done partly at an office and partly away from it. */
	HYBRID,

	/** Done at an office or a site. */
	ONSITE

}
