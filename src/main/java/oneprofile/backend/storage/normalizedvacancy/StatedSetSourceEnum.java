package oneprofile.backend.storage.normalizedvacancy;

/** Where in a vacancy a stated set was read (#13). */
public enum StatedSetSourceEnum {

	/** A string of its declared location, or one option of it where the string offers several work modes. */
	LOCATION,

	/** Its title: a region the location misses, a clearance. */
	TITLE,

	/** A segment of its description. */
	DESCRIPTION

}
