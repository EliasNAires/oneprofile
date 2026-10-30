package oneprofile.backend.storage.normalizedvacancy;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where a vacancy's work is performed, as the vacancy declares it (PRD §5.3, #86). A vacancy that
 * declares none has no work mode, which never means onsite; one offering a choice has every mode it
 * offers (#98).
 */
public enum WorkModeEnum {

	/** Done away from any office. */
	REMOTE,

	/** Done partly at an office and partly away from it. */
	HYBRID,

	/** Done at an office or a site. */
	ONSITE;

	/**
	 * The modes a vacancy's work modes are stored as.
	 * @param names the names of its modes, as stored, or null where it declares none
	 * @return its modes, empty where it declares none
	 */
	public static Set<WorkModeEnum> setOf(String[] names) {
		Set<WorkModeEnum> modes = EnumSet.noneOf(WorkModeEnum.class);
		if (names != null) {
			for (String name : names) {
				modes.add(valueOf(name));
			}
		}
		return modes;
	}

}
