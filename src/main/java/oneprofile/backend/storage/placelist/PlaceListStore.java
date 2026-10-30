package oneprofile.backend.storage.placelist;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The place list (#26): what a location string's places are read against. GeoNames gives the countries,
 * their first-level subdivisions and the cities of more than 15,000 people (cut by
 * {@code scripts/fetch-geonames}); the hand table gives what GeoNames does not: other names for them
 * ({@code USA}, {@code Bangalore}), the towns the residue shows often, regions, names that are never a
 * place, and what a name means where a string names it alone. GeoNames also gives each city's time zone
 * and each time zone's UTC offset, by which a timezone requirement is judged (#13); the regions, kept by
 * hand, expand each region the hand table names into its countries.
 * <p>
 * Each is read from a tab-separated file with a header line: the countries as
 * {@code code<TAB>iso3<TAB>name}, the subdivisions as {@code code<TAB>name<TAB>ascii}, the cities as
 * {@code id<TAB>name<TAB>ascii<TAB>country<TAB>subdivision<TAB>latitude<TAB>longitude<TAB>population<TAB>timezone},
 * the hand table as {@code name<TAB>kind<TAB>target<TAB>latitude<TAB>longitude}, the time zones as
 * {@code timezone<TAB>country<TAB>offset}, the offset in hours outside daylight saving time, and the regions
 * as {@code region<TAB>country}, one row per country, {@code *} for every country.
 * <p>
 * A list is refused if a hand row names a country, a subdivision or a city it does not hold, gives a town
 * no coordinates, or is listed twice; a name may mean several things (a town in two states), but has one
 * meaning alone. It is refused too if a region the hand table names has no countries, or a region names a
 * country the list does not hold.
 */
public class PlaceListStore {

	private final List<Country> countries;

	private final List<Subdivision> subdivisions;

	private final List<City> cities;

	private final List<HandPlace> hand;

	private final Map<String, Integer> offsets;

	private final Map<String, Set<Integer>> countryOffsets;

	private final Map<String, Set<String>> regions;

	private PlaceListStore(List<Country> countries, List<Subdivision> subdivisions, List<City> cities,
			List<HandPlace> hand, Map<String, Integer> offsets, Map<String, Set<Integer>> countryOffsets,
			Map<String, Set<String>> regions) {
		this.countries = List.copyOf(countries);
		this.subdivisions = List.copyOf(subdivisions);
		this.cities = List.copyOf(cities);
		this.hand = List.copyOf(hand);
		this.offsets = Map.copyOf(offsets);
		this.countryOffsets = Map.copyOf(countryOffsets);
		this.regions = Map.copyOf(regions);
	}

	/**
	 * Reads a place list. The readers are left open for whoever opened them to close.
	 * @param countries the countries file
	 * @param subdivisions the subdivisions file
	 * @param cities the cities file
	 * @param hand the hand table
	 * @param timeZones the time zones file
	 * @param regions the regions file
	 * @return the place list they hold
	 * @throws IllegalStateException if a hand row names what the list does not hold, gives a town no
	 * coordinates, or is listed twice, or if a region is not expanded or names a country the list does not hold
	 */
	public static PlaceListStore read(Reader countries, Reader subdivisions, Reader cities, Reader hand,
			Reader timeZones, Reader regions) {
		List<Country> countryList = lines(countries).stream()
			.map((fields) -> new Country(fields[0], fields[1], fields[2]))
			.toList();
		List<Subdivision> subdivisionList = lines(subdivisions).stream()
			.map((fields) -> new Subdivision(fields[0], fields[1], fields[2]))
			.toList();
		List<City> cityList = lines(cities).stream()
			.map((fields) -> new City(Long.parseLong(fields[0]), fields[1], fields[2], fields[3], fields[4],
					Double.parseDouble(fields[5]), Double.parseDouble(fields[6]), Long.parseLong(fields[7]),
					field(fields, 8)))
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
		Map<String, Integer> offsets = new HashMap<>();
		Map<String, Set<Integer>> countryOffsets = new HashMap<>();
		for (String[] fields : lines(timeZones)) {
			int minutes = (int) Math.round(Double.parseDouble(fields[2]) * 60);
			offsets.put(fields[0], minutes);
			countryOffsets.computeIfAbsent(fields[1], (country) -> new HashSet<>()).add(minutes);
		}
		Map<String, Set<String>> regionMap = new HashMap<>();
		for (String[] fields : lines(regions)) {
			if (!fields[1].equals("*") && !countryCodes.contains(fields[1])) {
				throw new IllegalStateException(
						"The region '" + fields[0] + "' names '" + fields[1] + "', which the list does not hold");
			}
			regionMap.computeIfAbsent(fields[0], (region) -> new HashSet<>())
				.addAll(fields[1].equals("*") ? countryCodes : Set.of(fields[1]));
		}
		for (HandPlace place : handList) {
			if (place.kind() == HandPlaceKindEnum.REGION && !regionMap.containsKey(place.target())) {
				throw new IllegalStateException("The region '" + place.target() + "' has no countries");
			}
		}
		Map<String, Set<String>> frozen = new HashMap<>();
		regionMap.forEach((region, members) -> frozen.put(region, Set.copyOf(members)));
		Map<String, Set<Integer>> frozenOffsets = new HashMap<>();
		countryOffsets.forEach((country, members) -> frozenOffsets.put(country, Set.copyOf(members)));
		return new PlaceListStore(countryList, subdivisionList, cityList, handList, offsets, frozenOffsets, frozen);
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

	/**
	 * @param timeZone a time zone's IANA id, such as {@code America/New_York}
	 * @return its UTC offset in minutes outside daylight saving time, null where the list does not hold it
	 */
	public Integer offsetOf(String timeZone) {
		return this.offsets.get(timeZone);
	}

	/**
	 * @param country a country's two-letter code
	 * @return the UTC offsets in minutes of its time zones, outside daylight saving time; empty where the list
	 * holds none
	 */
	public Set<Integer> offsetsOf(String country) {
		return this.countryOffsets.getOrDefault(country, Set.of());
	}

	/**
	 * @param region a region's label, as the hand table names it ({@code EMEA})
	 * @return the two-letter codes of its countries, empty for a label the regions do not hold
	 */
	public Set<String> countriesOf(String region) {
		return this.regions.getOrDefault(region, Set.of());
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
