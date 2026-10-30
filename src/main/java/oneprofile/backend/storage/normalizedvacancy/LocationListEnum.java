package oneprofile.backend.storage.normalizedvacancy;

/** Which of the lists an ATS publishes a location string came from (#26). */
public enum LocationListEnum {

	/** The location field every ATS publishes. */
	LOCATION,

	/** Ashby's secondary locations, each with the country Ashby gives it. */
	SECONDARY_LOCATIONS,

	/** Lever's and Workday's every location, past the first, which is the location field. */
	ALL_LOCATIONS

}
