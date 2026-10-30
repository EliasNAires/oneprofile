package oneprofile.backend.storage.placelist;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The place list (#26): what a location string's places are read against. GeoNames gives the countries,
 * their first-level subdivisions and the cities of more than 15,000 people (cut by
 * {@code scripts/fetch-geonames}); the hand table gives what GeoNames does not: other names for them
 * ({@code USA}, {@code Bangalore}), the towns the residue shows often, regions, names that are never a
 * place, and what a name means where a string names it alone.
 * <p>
 * Each is read from a tab-separated file with a header line: the countries as
 * {@code code<TAB>iso3<TAB>name}, the subdivisions as {@code code<TAB>name<TAB>ascii}, the cities as
 * {@code id<TAB>name<TAB>ascii<TAB>country<TAB>subdivision<TAB>latitude<TAB>longitude<TAB>population}, and
 * the hand table as {@code name<TAB>kind<TAB>target<TAB>latitude<TAB>longitude}.
 * <p>
 * A list is refused if a hand row names a country, a subdivision or a city it does not hold, gives a town
 * no coordinates, or is listed twice; a name may mean several things (a town in two states), but has one
 * meaning alone.
 */
public class PlaceListStore {

	private final List<Country> countries;

	private final List<Subdivision> subdivisions;

	private final List<City> cities;

	private final List<HandPlace> hand;

	private PlaceListStore(List<Country> countries, List<Subdivision> subdivisions, List<City> cities,
			List<HandPlace> hand) {
		this.countries = List.copyOf(countries);
		this.subdivisions = List.copyOf(subdivisions);
		this.cities = List.copyOf(cities);
		this.hand = List.copyOf(hand);
	}

	/**
	 * Reads a place list. The readers are left open for whoever opened them to close.
	 * @param countries the countries file
	 * @param subdivisions the subdivisions file
	 * @param cities the cities file
	 * @param hand the hand table
	 * @return the place list they hold
	 * @throws IllegalStateException if a hand row names what the list does not hold, gives a town no
	 * coordinates, or is listed twice
	 */
	public static PlaceListStore read(Reader countries, Reader subdivisions, Reader cities, Reader hand) {
		List<Country> countryList = lines(countries).stream()
			.map((fields) -> new Country(fields[0], fields[1], fields[2]))
			.toList();
		List<Subdivision> subdivisionList = lines(subdivisions).stream()
			.map((fields) -> new Subdivision(fields[0], fields[1], fields[2]))
			.toList();
		List<City> cityList = lines(cities).stream()
			.map((fields) -> new City(Long.parseLong(fields[0]), fields[1], fields[2], fields[3], fields[4],
					Double.parseDouble(fields[5]), Double.parseDouble(fields[6]), Long.parseLong(fields[7])))
			.toList();
		Set<String> countryCodes = countryList.stream().map(Country::code).collect(Collectors.toSet());
		Set<String> subdivisionCodes = subdivisionList.stream().map(Subdivision::code).collect(Collectors.toSet());
		Set<String> cityIds = cityList.stream().map((city) -> Long.toString(city.id())).collect(Collectors.toSet());
		Set<String> listed = new HashSet<>();
		List<HandPlace> handList = lines(hand).stream().map((fields) -> {
			HandPlace place = new HandPlace(fields[0], HandPlaceKindEnum.valueOf(fields[1].toUpperCase(Locale.ROOT)),
					field(fields, 2), coordinate(field(fields, 3)), coordinate(field(fields, 4)));
			String row = place.kind() + "\t" + place.name().toLowerCase(Locale.ROOT)
					+ ((place.kind() == HandPlaceKindEnum.BARE) ? "" : "\t" + place.target());
			if (!listed.add(row)) {
				throw new IllegalStateException("'" + place.name() + "' is listed twice as a " + place.kind());
			}
			boolean known = switch (place.kind()) {
				case COUNTRY -> countryCodes.contains(place.target());
				case SUBDIVISION -> subdivisionCodes.contains(place.target());
				case CITY -> cityIds.contains(place.target());
				case TOWN -> subdivisionCodes.contains(place.target()) || countryCodes.contains(place.target());
				case BARE -> countryCodes.contains(place.target()) || subdivisionCodes.contains(place.target())
						|| cityIds.contains(place.target());
				case REGION -> !place.target().isEmpty();
				case IGNORED -> true;
			};
			if (!known) {
				throw new IllegalStateException(
						"'" + place.name() + "' names '" + place.target() + "', which the list does not hold");
			}
			if (place.kind() == HandPlaceKindEnum.TOWN && (place.latitude() == null || place.longitude() == null)) {
				throw new IllegalStateException("The town '" + place.name() + "' has no coordinates");
			}
			return place;
		}).toList();
		return new PlaceListStore(countryList, subdivisionList, cityList, handList);
	}

	/** Every country GeoNames lists. */
	public List<Country> countries() {
		return this.countries;
	}

	/** Every first-level subdivision GeoNames lists. */
	public List<Subdivision> subdivisions() {
		return this.subdivisions;
	}

	/** Every city of more than 15,000 people GeoNames lists. */
	public List<City> cities() {
		return this.cities;
	}

	/** The hand table's rows, in the order of its file. */
	public List<HandPlace> hand() {
		return this.hand;
	}

	private static List<String[]> lines(Reader tsv) {
		return new BufferedReader(tsv).lines().skip(1).filter((line) -> !line.isBlank())
			.map((line) -> line.split("\t", -1)).toList();
	}

	private static String field(String[] fields, int index) {
		return (index < fields.length) ? fields[index] : "";
	}

	private static Double coordinate(String field) {
		return field.isEmpty() ? null : Double.valueOf(field);
	}

}
