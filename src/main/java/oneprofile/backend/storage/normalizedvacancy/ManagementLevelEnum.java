package oneprofile.backend.storage.normalizedvacancy;

/**
 * The rank of a leadership word a vacancy's title names over an area, from the least to the most
 * senior (ADR-0024, amended 2026-10-26). Not the seniority level: a vacancy can have both.
 */
public enum ManagementLevelEnum {

	/** Named by {@code lead} and {@code leader}, and by a tech or team lead: the title does not say whether they manage anyone. */
	LEAD,

	/** Named by {@code manager}. */
	MANAGER,

	/** Named by {@code director} and {@code head}. */
	DIRECTOR,

	/** Named by a vice president, a president or a chief. */
	EXECUTIVE

}
