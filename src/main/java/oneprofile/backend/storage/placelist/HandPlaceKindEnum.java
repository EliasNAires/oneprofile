package oneprofile.backend.storage.placelist;

/** What a row of the place list's hand table says of the name it holds. */
public enum HandPlaceKindEnum {

	/** The name is a country's: its target is the country's two-letter code. */
	COUNTRY,

	/** The name is a subdivision's: its target is the subdivision's code, such as {@code CA.08}. */
	SUBDIVISION,

	/** The name is a city's GeoNames lists: its target is the city's GeoNames id. */
	CITY,

	/**
	 * The name is a town GeoNames' list of cities does not hold: its target is the code of its subdivision,
	 * or of its country where it has none, and the row gives its coordinates.
	 */
	TOWN,

	/** The name is a region's, a set of countries kept as named: its target is the region's label. */
	REGION,

	/** The name is never a place, though GeoNames lists a place by it ({@code Hub}, {@code Metro}). */
	IGNORED,

	/**
	 * What the name means where a string names it alone, overriding its most populous match: its target is a
	 * country's code, a subdivision's code or a city's GeoNames id.
	 */
	BARE

}
