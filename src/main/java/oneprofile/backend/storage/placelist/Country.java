package oneprofile.backend.storage.placelist;

/**
 * A country as GeoNames lists it.
 *
 * @param code its two-letter ISO code, such as {@code US}
 * @param iso3 its three-letter ISO code, such as {@code USA}
 * @param name its English name
 */
public record Country(String code, String iso3, String name) {
}
