package oneprofile.backend.storage.normalizedvacancy;

/** How precisely a place is named: the most precise level its string gives (#26). */
public enum PlaceKindEnum {

	/** A set of countries kept as named, such as {@code EMEA}. */
	REGION,

	COUNTRY,

	/** A country's first-level subdivision: a state or a province. */
	SUBDIVISION,

	CITY

}
