package oneprofile.backend.storage.normalizedvacancy;

/** What a stated set says, by which a wrong eligibility state is traced and work rights are read later (#13). */
public enum StatedSetKindEnum {

	/** Countries, subdivisions or cities a vacancy names as where it engages someone. */
	LOCATION,

	/** A region a vacancy names as where it engages someone, kept as named ({@code EMEA}). */
	REGION,

	/** Work authorization in a country: {@code must be authorized to work in the US}. */
	AUTHORIZATION,

	/** Citizenship of a country: {@code US citizens only}. */
	CITIZENSHIP,

	/** A security clearance, which is a country's: {@code TS/SCI}. */
	CLEARANCE,

	/** A window of UTC offsets the person must be located in: {@code candidates in UTC-5 to UTC+1}. */
	TIMEZONE

}
