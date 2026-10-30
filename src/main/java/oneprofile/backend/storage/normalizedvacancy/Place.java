package oneprofile.backend.storage.normalizedvacancy;

/**
 * What one location string names, as precisely as the string gives it (#26): a country, then a state or
 * province where one is named, then a city where one is named, with the city's coordinates; or a region
 * kept as named.
 *
 * @param kind how precisely it is named
 * @param region the region's label, null for any other kind
 * @param country the two-letter code of its country, null for a region
 * @param subdivision the name of its state or province, null where none is known
 * @param city the city's name, null for a less precise place
 * @param latitude the city's latitude, null for a less precise place
 * @param longitude the city's longitude, null for a less precise place
 */
public record Place(PlaceKindEnum kind, String region, String country, String subdivision, String city,
		Double latitude, Double longitude) {

	/**
	 * @param label the region's label, such as {@code EMEA}
	 * @return the region
	 */
	public static Place region(String label) {
		return new Place(PlaceKindEnum.REGION, label, null, null, null, null, null);
	}

	/**
	 * @param country its two-letter code
	 * @return the country
	 */
	public static Place country(String country) {
		return new Place(PlaceKindEnum.COUNTRY, null, country, null, null, null, null);
	}

	/**
	 * @param country the two-letter code of its country
	 * @param subdivision its name
	 * @return the state or province
	 */
	public static Place subdivision(String country, String subdivision) {
		return new Place(PlaceKindEnum.SUBDIVISION, null, country, subdivision, null, null, null);
	}

	/**
	 * @param country the two-letter code of its country
	 * @param subdivision the name of its state or province, null where none is known
	 * @param city its name
	 * @param latitude its latitude
	 * @param longitude its longitude
	 * @return the city
	 */
	public static Place city(String country, String subdivision, String city, double latitude, double longitude) {
		return new Place(PlaceKindEnum.CITY, null, country, subdivision, city, latitude, longitude);
	}

}
