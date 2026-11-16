package oneprofile.backend.storage.normalizedvacancy;

/**
 * The vacancy group a vacancy belongs to, or the vacancy it repeats.
 * @param groupId its group's id, null for a duplicate
 * @param duplicateOfVacancyId the vacancy it repeats, null for a group member
 */
public record GroupMark(Long groupId, Long duplicateOfVacancyId) {

	/**
	 * Requires exactly one of a group and a vacancy repeated.
	 * @param groupId its group's id, null for a duplicate
	 * @param duplicateOfVacancyId the vacancy it repeats, null for a group member
	 */
	public GroupMark {
		if ((groupId == null) == (duplicateOfVacancyId == null)) {
			throw new IllegalArgumentException("A vacancy must belong to a group or repeat another vacancy");
		}
	}

	/**
	 * The mark of a vacancy that belongs to this group.
	 * @param groupId the group it belongs to
	 * @return the group membership mark
	 */
	public static GroupMark inGroup(long groupId) {
		return new GroupMark(groupId, null);
	}

	/**
	 * The mark of a duplicate that repeats this vacancy.
	 * @param vacancyId the kept vacancy it repeats
	 * @return the duplicate mark
	 */
	public static GroupMark duplicateOf(long vacancyId) {
		return new GroupMark(null, vacancyId);
	}

}
