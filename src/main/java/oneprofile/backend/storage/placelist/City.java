package oneprofile.backend.storage.placelist;

/**
 * A populated place of more than 15,000 people, as GeoNames lists it.
 *
 * @param id its GeoNames id
 * @param name its name, such as {@code Zürich}
 * @param ascii its name in ASCII, such as {@code Zuerich}
 * @param country the two-letter code of its country
 * @param subdivision the code of its first-level subdivision within the country, such as {@code TX}, empty
 * where it has none
 * @param latitude its latitude
 * @param longitude its longitude
 * @param population how many people live in it
 * @param timeZone the IANA id of its time zone, such as {@code America/New_York}, empty where none is known
 */
public record City(long id, String name, String ascii, String country, String subdivision, double latitude,
		double longitude, long population, String timeZone) {
}
