package oneprofile.backend.storage.placelist;

/**
 * A country's first-level subdivision, a state or a province, as GeoNames lists it.
 *
 * @param code GeoNames' code for it, its country's code and its own joined by a dot, such as {@code US.TX}
 * @param name its name, such as {@code Texas}
 * @param ascii its name in ASCII
 */
public record Subdivision(String code, String name, String ascii) {

	/** The two-letter code of its country. */
	public String country() {
		return this.code.substring(0, this.code.indexOf('.'));
	}

}
