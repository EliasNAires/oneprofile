package oneprofile.backend.workers.eligibility;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.PlaceKindEnum;
import oneprofile.backend.storage.placelist.City;
import oneprofile.backend.storage.placelist.Country;
import oneprofile.backend.storage.placelist.HandPlace;
import oneprofile.backend.storage.placelist.PlaceListStore;
import oneprofile.backend.storage.placelist.Subdivision;
import org.springframework.stereotype.Component;

/**
 * Reads the places a location string names, against the place list (#26). Rules only (ADR-0005).
 * <p>
 * The string is read word by word for the longest run of words the list names: a country, a subdivision,
 * a city, a region, or a word the hand table says is never a place. A code of two or three capitals
 * ({@code TX}, {@code USA}) is read only as written; any other name in any case and without its accents.
 * A run never crosses a comma, a bracket or a spaced dash, and a word joined by a hyphen
 * ({@code Winston-Salem}, {@code United States-Colorado}) may be read with its neighbour.
 * <p>
 * The names read are then put together into places, in the order written: a name joins the place before it
 * where some reading of them all is one place, at most one country, one subdivision and one city, the city
 * in the subdivision and both in the country ({@code Paris, TX}, {@code US, CA, Santa Clara}). Otherwise it
 * starts a place of its own ({@code Chicago, IL, Cleveland, OH}), as it does after a semicolon, a slash, an
 * ampersand, {@code and} or {@code or}. A region is always a place of its own, kept as named.
 * <p>
 * Of the readings of one place, the most precise wins, then the most populous city. A name alone that the
 * hand table gives a meaning has that one ({@code Singapore}, the city); otherwise a name alone that is a
 * country's is the country ({@code Mexico}), and any other is its most populous match.
 * <p>
 * The region {@code Worldwide} ({@code Anywhere}, {@code Global}) is kept only where the string names no
 * other place: {@code Anywhere in the U.S.} is the United States.
 * <p>
 * Where the ATS names the string's country, that country is taken and not read: every name is read within
 * it, and the country is the place where the string names nothing more precise in it.
 */
@Component
public class PlaceRule {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private static final Pattern SPACES = Pattern.compile("\\s+");

	private static final Pattern CODE = Pattern.compile("[A-Z]{2,3}");

	/** The hand table's label of the region that is every country, kept only where nothing narrows it. */
	private static final String WORLDWIDE = "Worldwide";

	/** How many people a city holds for its name to outweigh an outdated subdivision written beside it. */
	private static final long CITY_OVER_OUTDATED_STATE = 1_000_000;

	/** How many people a city holds for its name alone to outweigh a subdivision's of the same name. */
	private static final long CITY_OVER_SAME_NAME = 100_000;

	/** The short forms a name's first word is also written in, both ways: {@code St Louis}, {@code Ft Worth}. */
	private static final Map<String, String> SHORT_FORMS = Map.of("saint", "st", "fort", "ft", "mount", "mt");

	/** A name of one word shorter than this is read only as a code. */
	private static final int SHORTEST_NAME = 3;

	private final Map<String, Set<Candidate>> names = new HashMap<>();

	private final Map<String, Set<Candidate>> codes = new HashMap<>();

	private final Set<String> ignored = new HashSet<>();

	private final Map<String, Candidate> bare = new HashMap<>();

	private final Map<String, String> subdivisionNames = new HashMap<>();

	private final Map<String, Candidate> countries = new HashMap<>();

	private int longestName = 1;

	public PlaceRule(PlaceListStore placeList) {
		Map<String, Candidate> cities = new HashMap<>();
		Map<String, Candidate> subdivisions = new HashMap<>();
		for (Country country : placeList.countries()) {
			Candidate candidate = Candidate.country(country.code());
			this.countries.put(country.code(), candidate);
			add(country.code(), candidate);
			add(country.iso3(), candidate);
			add(country.name(), candidate);
		}
		for (Subdivision subdivision : placeList.subdivisions()) {
			Candidate candidate = Candidate.subdivision(subdivision.country(), subdivision.code());
			this.subdivisionNames.put(subdivision.code(), subdivision.name());
			subdivisions.put(subdivision.code(), candidate);
			add(subdivision.name(), candidate);
			add(subdivision.ascii(), candidate);
			if (subdivision.country().equals("US")) {
				add(subdivision.code().substring(3), candidate);
			}
		}
		for (City city : placeList.cities()) {
			Candidate candidate = Candidate.city(city);
			cities.put(Long.toString(city.id()), candidate);
			add(city.name(), candidate);
			add(city.ascii(), candidate);
		}
		for (HandPlace hand : placeList.hand()) {
			switch (hand.kind()) {
				case COUNTRY -> add(hand.name(), this.countries.get(hand.target()));
				case SUBDIVISION -> add(hand.name(), subdivisions.get(hand.target()));
				case CITY -> add(hand.name(), cities.get(hand.target()));
				case TOWN -> {
					String country = hand.target().contains(".") ? hand.target().substring(0, 2) : hand.target();
					String subdivision = hand.target().contains(".") ? hand.target().substring(3) : "";
					add(hand.name(), Candidate.city(new City(0, hand.name(), hand.name(), country, subdivision,
							hand.latitude(), hand.longitude(), 0, "")));
				}
				case REGION -> add(hand.name(), Candidate.region(hand.target()));
				case IGNORED -> this.ignored.add(lookup(hand.name()));
				case BARE -> this.bare.put(key(hand.name()), Objects.requireNonNullElseGet(
						this.countries.get(hand.target()), () -> Objects.requireNonNullElseGet(
								subdivisions.get(hand.target()), () -> cities.get(hand.target()))));
			}
		}
	}

	/**
	 * Reads the places one location string names.
	 * @param location the string as its ATS publishes it
	 * @param country the country the ATS names for it, as the ATS writes it, or null where it names none
	 * @return the places it names, and the words it read as nothing
	 */
	public PlaceReading read(String location, String country) {
		if (location == null || location.isBlank()) {
			return new PlaceReading(List.of(), List.of());
		}
		String own = ownCountry(country);
		List<String> unread = new ArrayList<>();
		List<Mention> mentions = mentions(words(location), own, unread);
		Set<Place> places = new LinkedHashSet<>();
		List<Mention> group = new ArrayList<>();
		for (Mention mention : mentions) {
			Candidate region = mention.region();
			if (region != null) {
				if (!mention.beside()) {
					resolve(group, own).ifPresent(places::add);
					group.clear();
					places.add(Place.region(region.region()));
				}
				continue;
			}
			if (!group.isEmpty() && !mention.separated()) {
				if (best(append(group, mention)) != null) {
					group.add(mention);
					continue;
				}
				if (mendedAsUnknownTown(group, mention)) {
					continue;
				}
			}
			if (mention.beside() && !(group.isEmpty() && places.isEmpty())) {
				continue;
			}
			resolve(group, own).ifPresent(places::add);
			group.clear();
			group.add(mention);
		}
		resolve(group, own).ifPresent(places::add);
		if (places.size() > 1) {
			// Anywhere followed by a place is anywhere in it (Anywhere in the U.S., Anywhere in Latin America).
			places.remove(Place.region(WORLDWIDE));
		}
		if (own != null && places.stream().allMatch((place) -> place.kind() == PlaceKindEnum.REGION)) {
			places.add(Place.country(own));
		}
		return new PlaceReading(List.copyOf(places), unread);
	}

	/**
	 * Whether a name that does not fit the place before it reads as a town the list does not hold, beside the
	 * subdivision or country it lies in, and the group is mended to that. A subdivision or country written
	 * after a city it does not hold ({@code Malvern, PA}, not Malvern, Ontario) replaces the city, unless the
	 * city is one of more than a million people in that country, written beside an outdated subdivision
	 * ({@code Hyderabad, Andhra Pradesh}), which is kept instead. A city written after a subdivision or country
	 * alone that does not hold it ({@code IL-Rosemont}) is passed over, unless it is one of more than a million
	 * people. Either way the subdivision or country
	 * reads as a town's qualifier.
	 * @param group the names of the place so far, changed where the name reads as a town's
	 * @param mention the name that does not fit them
	 * @return whether the name was read so
	 */
	private static boolean mendedAsUnknownTown(List<Mention> group, Mention mention) {
		Mention last = group.getLast();
		if (mention.qualifier()) {
			Set<String> countries = new HashSet<>();
			mention.candidates().forEach((candidate) -> countries.add(candidate.country()));
			if (last.candidates().stream().anyMatch((candidate) -> candidate.population() >= CITY_OVER_OUTDATED_STATE
					&& countries.contains(candidate.country()))) {
				return true;
			}
		}
		if (last.onlyCities() && mention.qualifier()) {
			List<Mention> mended = new ArrayList<>(group.subList(0, group.size() - 1));
			mended.add(mention.asQualifier());
			if (best(mended) != null) {
				group.clear();
				group.addAll(mended);
				return true;
			}
			return false;
		}
		if (group.size() == 1 && last.qualifier() && mention.onlyCities()
				&& mention.candidates().stream().allMatch((candidate) -> candidate.population() < CITY_OVER_OUTDATED_STATE)) {
			group.set(0, last.asQualifier());
			return true;
		}
		return false;
	}

	/** The code of the country the ATS names, null where it names none or one the list does not know. */
	private String ownCountry(String country) {
		if (country == null || country.isBlank()) {
			return null;
		}
		Set<Candidate> candidates = this.names.getOrDefault(key(country), this.codes.get(country.strip()));
		if (candidates == null) {
			return null;
		}
		return candidates.stream().filter((candidate) -> candidate.kind() == PlaceKindEnum.COUNTRY).findFirst()
			.map(Candidate::country).orElse(null);
	}

	/** The names a string's words hold, each with the readings it may have. */
	private List<Mention> mentions(List<Word> words, String own, List<String> unread) {
		List<Mention> mentions = new ArrayList<>();
		List<Integer> mentionSegments = new ArrayList<>();
		Set<Integer> unreadSegments = new HashSet<>();
		boolean separated = false;
		boolean afterUnread = false;
		int segment = 0;
		int at = 0;
		while (at < words.size()) {
			Boundary before = words.get(at).before();
			if (before == Boundary.SOFT || before == Boundary.STRONG) {
				segment++;
			}
			if (before == Boundary.STRONG) {
				separated = true;
				afterUnread = false;
			}
			int length = Math.min(this.longestName, words.size() - at);
			Set<Candidate> found = null;
			boolean ignore = false;
			for (; length > 0; length--) {
				if (crossesBoundary(words, at, length)) {
					continue;
				}
				String lookup = lookup(words, at, length);
				if (this.ignored.contains(lookup)) {
					ignore = true;
					break;
				}
				found = isCode(lookup) ? this.codes.get(lookup) : this.names.get(lookup);
				if (found != null) {
					break;
				}
			}
			if (ignore) {
				at += length;
				continue;
			}
			if (found == null) {
				String word = words.get(at).text();
				if (word.equalsIgnoreCase("and") || word.equalsIgnoreCase("or")) {
					separated = true;
					afterUnread = false;
					segment++;
				}
				else {
					unread.add(word);
					unreadSegments.add(segment);
					afterUnread = true;
				}
				at++;
				continue;
			}
			List<Candidate> candidates = found.stream()
				.filter((candidate) -> own == null || candidate.kind() == PlaceKindEnum.REGION
						|| own.equals(candidate.country()))
				.sorted(Comparator.comparingLong(Candidate::population).reversed())
				.toList();
			if (candidates.isEmpty()) {
				// A name the ATS's country does not hold (London, with the country United States) is not read.
				for (int i = at; i < at + length; i++) {
					unread.add(words.get(i).text());
				}
			}
			else {
				mentions.add(new Mention(key(words, at, length), candidates, separated, afterUnread, false));
				mentionSegments.add(segment);
				separated = false;
				afterUnread = false;
			}
			at += length;
		}
		List<Mention> marked = new ArrayList<>(mentions.size());
		for (int i = 0; i < mentions.size(); i++) {
			Mention mention = mentions.get(i);
			marked.add(new Mention(mention.key(), mention.candidates(), mention.separated(), mention.afterUnread(),
					unreadSegments.contains(mentionSegments.get(i))));
		}
		return marked;
	}

	/** The place a group of names makes, nothing for an empty group. */
	private Optional<Place> resolve(List<Mention> group, String own) {
		if (group.isEmpty()) {
			return Optional.empty();
		}
		Candidate alone = (group.size() == 1 && own == null) ? this.bare.get(group.getFirst().key()) : null;
		Place place = place((alone != null) ? List.of(alone) : best(group));
		if (place.kind() == PlaceKindEnum.COUNTRY) {
			// A city-state named twice (Singapore, Singapore) is the city its name means alone.
			for (Mention mention : group) {
				Candidate city = this.bare.get(mention.key());
				if (city != null && city.kind() == PlaceKindEnum.CITY && city.country().equals(place.country())) {
					return Optional.of(place(List.of(city)));
				}
			}
		}
		return Optional.of(place);
	}

	private Place place(List<Candidate> reading) {
		Candidate city = null;
		Candidate subdivision = null;
		String country = null;
		for (Candidate candidate : reading) {
			country = candidate.country();
			switch (candidate.kind()) {
				case CITY -> city = candidate;
				case SUBDIVISION -> subdivision = candidate;
				default -> {
				}
			}
		}
		if (city != null) {
			City at = city.city();
			String subdivisionName = this.subdivisionNames.get(at.country() + "." + at.subdivision());
			return Place.city(at.country(), subdivisionName, at.name(), at.latitude(), at.longitude());
		}
		if (subdivision != null) {
			return Place.subdivision(country, this.subdivisionNames.get(subdivision.subdivision()));
		}
		return Place.country(country);
	}

	/**
	 * The best reading of a group of names as one place, null where no reading makes them one. A name that is
	 * a country's is read as the country wherever that fits, unless it follows words that were not read
	 * ({@code Riverwoods, IL}); then the most precise reading wins, then the most populous city. A name alone
	 * that is also a subdivision's is read as a city of fewer than a hundred thousand people only where the
	 * city lies in it ({@code Florida} is the state, not the town in Cuba).
	 */
	private static List<Candidate> best(List<Mention> group) {
		List<List<Candidate>> readings = new ArrayList<>();
		readings(group, 0, new ArrayList<>(), readings);
		return readings.stream()
			.max(Comparator.<List<Candidate>>comparingInt((reading) -> countriesRead(group, reading))
				.thenComparingInt(PlaceRule::precision)
				.thenComparingLong(PlaceRule::population))
			.orElse(null);
	}

	private static void readings(List<Mention> group, int at, List<Candidate> chosen, List<List<Candidate>> readings) {
		if (at == group.size()) {
			readings.add(List.copyOf(chosen));
			return;
		}
		Mention mention = group.get(at);
		Set<String> subdivisions = new HashSet<>();
		if (group.size() == 1) {
			mention.candidates().stream().filter((candidate) -> candidate.kind() == PlaceKindEnum.SUBDIVISION)
				.forEach((candidate) -> subdivisions.add(candidate.subdivision()));
		}
		for (Candidate candidate : mention.candidates()) {
			if (candidate.kind() == PlaceKindEnum.CITY && !subdivisions.isEmpty() && candidate.population() < CITY_OVER_SAME_NAME
					&& !subdivisions.contains(candidate.country() + "." + candidate.city().subdivision())) {
				continue;
			}
			if (fits(chosen, candidate)) {
				chosen.add(candidate);
				readings(group, at + 1, chosen, readings);
				chosen.removeLast();
			}
		}
	}

	/** How many names of a group that may be a country's, and do not follow unread words, a reading reads so. */
	private static int countriesRead(List<Mention> group, List<Candidate> reading) {
		int read = 0;
		for (int i = 0; i < group.size(); i++) {
			if (!group.get(i).afterUnread() && reading.get(i).kind() == PlaceKindEnum.COUNTRY) {
				read++;
			}
		}
		return read;
	}

	/**
	 * Whether a reading of one name is one place with those chosen before it: the same country, at most one
	 * subdivision and one city, and the city in the subdivision.
	 */
	private static boolean fits(List<Candidate> chosen, Candidate candidate) {
		for (Candidate other : chosen) {
			if (!other.country().equals(candidate.country())) {
				return false;
			}
			if (other.kind() == candidate.kind() && !other.equals(candidate)) {
				return false;
			}
			Candidate city = (other.kind() == PlaceKindEnum.CITY) ? other : candidate;
			Candidate subdivision = (other.kind() == PlaceKindEnum.SUBDIVISION) ? other : candidate;
			if (city.kind() == PlaceKindEnum.CITY && subdivision.kind() == PlaceKindEnum.SUBDIVISION
					&& !subdivision.subdivision().equals(city.country() + "." + city.city().subdivision())) {
				return false;
			}
		}
		return true;
	}

	private static int precision(List<Candidate> reading) {
		return reading.stream().mapToInt((candidate) -> candidate.kind().ordinal()).max().orElse(0);
	}

	private static long population(List<Candidate> reading) {
		return reading.stream().mapToLong(Candidate::population).max().orElse(0);
	}

	private static List<Mention> append(List<Mention> group, Mention mention) {
		List<Mention> appended = new ArrayList<>(group);
		appended.add(mention);
		return appended;
	}

	private void add(String name, Candidate candidate) {
		String lookup = lookup(name);
		if (lookup.isEmpty()) {
			return;
		}
		if (!isCode(lookup) && lookup.indexOf(' ') < 0 && lookup.length() < SHORTEST_NAME) {
			return;
		}
		(isCode(lookup) ? this.codes : this.names).computeIfAbsent(lookup, (named) -> new LinkedHashSet<>())
			.add(candidate);
		this.longestName = Math.max(this.longestName, lookup.split(" ").length);
		int space = lookup.indexOf(' ');
		if (space > 0) {
			String first = lookup.substring(0, space);
			SHORT_FORMS.forEach((full, shortForm) -> {
				String other = first.equals(full) ? shortForm : first.equals(shortForm) ? full : null;
				if (other != null) {
					this.names.computeIfAbsent(other + lookup.substring(space), (named) -> new LinkedHashSet<>())
						.add(candidate);
				}
			});
		}
	}

	/** What a name is looked up by: a code as written, any other name by its key. */
	private static String lookup(String name) {
		String stripped = strip(name).replace(".", "").strip();
		return isCode(stripped) ? stripped : key(name);
	}

	private static String lookup(List<Word> words, int at, int length) {
		return (length == 1 && isCode(words.get(at).text())) ? words.get(at).text() : key(words, at, length);
	}

	private static boolean isCode(String text) {
		return CODE.matcher(text).matches();
	}

	private static String key(List<Word> words, int at, int length) {
		StringBuilder joined = new StringBuilder();
		for (int i = at; i < at + length; i++) {
			joined.append(' ').append(words.get(i).text());
		}
		return key(joined.toString());
	}

	/** A name without its accents, its case, its dots and apostrophes, its hyphens read as spaces. */
	private static String key(String name) {
		String stripped = strip(name).replace(".", "").toLowerCase(Locale.ROOT).replace('-', ' ');
		return SPACES.matcher(stripped).replaceAll(" ").strip();
	}

	/** A text without its accents and apostrophes. */
	private static String strip(String text) {
		String decomposed = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		StringBuilder stripped = new StringBuilder(decomposed.length());
		for (char c : decomposed.toCharArray()) {
			switch (c) {
				case '\'', '’' -> {
				}
				case 'ł' -> stripped.append('l');
				case 'Ł' -> stripped.append('L');
				case 'ø' -> stripped.append('o');
				case 'Ø' -> stripped.append('O');
				case 'đ' -> stripped.append('d');
				case 'ı' -> stripped.append('i');
				case 'ß' -> stripped.append("ss");
				case 'æ' -> stripped.append("ae");
				default -> stripped.append(c);
			}
		}
		return stripped.toString();
	}

	/** Whether a run of words holds a boundary a name cannot cross. */
	private static boolean crossesBoundary(List<Word> words, int at, int length) {
		for (int i = at + 1; i < at + length; i++) {
			if (words.get(i).before() == Boundary.SOFT || words.get(i).before() == Boundary.STRONG) {
				return true;
			}
		}
		return false;
	}

	/** A string's words, each with the strongest boundary between it and the word before. */
	private static List<Word> words(String location) {
		String text = strip(location);
		List<Word> words = new ArrayList<>();
		StringBuilder word = new StringBuilder();
		Boundary before = Boundary.SPACE;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				word.append(c);
				continue;
			}
			if (c == '.' && (word.length() < 2 || i + 1 == text.length() || !Character.isLetter(text.charAt(i + 1)))) {
				// The dot of an abbreviation (U.S.A., St. Louis) is no boundary; one between two words (USA.VA) is.
				continue;
			}
			if (!word.isEmpty()) {
				words.add(new Word(word.toString(), before));
				word.setLength(0);
				before = Boundary.SPACE;
			}
			Boundary boundary = switch (c) {
				case ';', '|', '/', '\\', '&', '+', '•', '\n' -> Boundary.STRONG;
				case '-' -> (i > 0 && i + 1 < text.length() && Character.isLetterOrDigit(text.charAt(i - 1))
						&& Character.isLetterOrDigit(text.charAt(i + 1))) ? Boundary.JOIN : Boundary.SOFT;
				default -> Character.isWhitespace(c) ? Boundary.SPACE : Boundary.SOFT;
			};
			if (boundary.compareTo(before) > 0) {
				before = boundary;
			}
		}
		if (!word.isEmpty()) {
			words.add(new Word(word.toString(), before));
		}
		return words;
	}

	/** What stands between two words, weakest first. */
	private enum Boundary {

		/** A space: a name may run across it. */
		SPACE,

		/** A hyphen between two words: a name may run across it. */
		JOIN,

		/** A comma, a bracket, a spaced dash: a name stops at it, a place may not. */
		SOFT,

		/** A semicolon, a slash, an ampersand: a place stops at it. */
		STRONG

	}

	private record Word(String text, Boundary before) {
	}

	/**
	 * One name a string holds.
	 * @param key the name's key, by which the hand table says what it means alone
	 * @param candidates what it may be, the most populous city first
	 * @param separated whether a semicolon, a slash, {@code and} or the like stands before it
	 * @param afterUnread whether words that were not read stand right before it, as a town's do before its
	 * state ({@code Riverwoods, IL})
	 * @param beside whether words that were not read stand beside it, between the same commas or brackets
	 * ({@code Gera Commerzone SEZ}): a name there is read only as part of the place before it, or as the
	 * string's first place, and never as a region
	 */
	private record Mention(String key, List<Candidate> candidates, boolean separated, boolean afterUnread,
			boolean beside) {

		/** Whether all it may be is a city. */
		boolean onlyCities() {
			return this.candidates.stream().allMatch((candidate) -> candidate.kind() == PlaceKindEnum.CITY);
		}

		/** Whether it may be a subdivision or a country, as a town's state is. */
		boolean qualifier() {
			return this.candidates.stream()
				.anyMatch((candidate) -> candidate.kind() == PlaceKindEnum.SUBDIVISION
						|| candidate.kind() == PlaceKindEnum.COUNTRY);
		}

		/** The same name, read as a town's qualifier: a code is a subdivision before it is a country. */
		Mention asQualifier() {
			return new Mention(this.key, this.candidates, this.separated, true, this.beside);
		}

		Candidate region() {
			return this.candidates.stream().filter((candidate) -> candidate.kind() == PlaceKindEnum.REGION)
				.findFirst().orElse(null);
		}

	}

	/**
	 * One thing a name may be.
	 * @param kind which list it comes from
	 * @param region a region's label
	 * @param country its country's code, null for a region
	 * @param subdivision a subdivision's code, such as {@code US.TX}
	 * @param city a city
	 */
	private record Candidate(PlaceKindEnum kind, String region, String country, String subdivision, City city) {

		static Candidate region(String label) {
			return new Candidate(PlaceKindEnum.REGION, label, null, null, null);
		}

		static Candidate country(String code) {
			return new Candidate(PlaceKindEnum.COUNTRY, null, code, null, null);
		}

		static Candidate subdivision(String country, String code) {
			return new Candidate(PlaceKindEnum.SUBDIVISION, null, country, code, null);
		}

		static Candidate city(City city) {
			return new Candidate(PlaceKindEnum.CITY, null, city.country(), null, city);
		}

		long population() {
			return (this.city == null) ? 0 : this.city.population();
		}

	}

}
