package oneprofile.backend.workers.grouping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import org.springframework.stereotype.Component;

/** Compares descriptions after removing the changing details of one position's vacancies. */
@Component
public class DescriptionSimilarityRule {

	/** How many consecutive kept tokens name one shingle. */
	public static final int SHINGLE = 3;

	/** A description needs this many distinct shingles before it can join another. */
	public static final int MIN_SHINGLES = 20;

	/** Splits text without breaking a token that contains a digit. */
	private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

	/** Tokens containing any Unicode digit are changing details. */
	private static final Pattern NUMBER = Pattern.compile(".*\\p{N}.*");

	/** Calendar and currency tokens do not describe the position. */
	private static final Set<String> OMIT = Set.of("january", "jan", "february", "feb", "march", "mar", "april",
			"apr", "may", "june", "jun", "july", "jul", "august", "aug", "september", "sep", "october", "oct",
			"november", "nov", "december", "dec", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
			"sunday", "usd", "eur", "gbp", "cad", "aud", "dollars", "dollar", "euros", "euro", "pounds");

	/**
	 * Returns the sorted, distinct hashes of consecutive kept tokens in the description.
	 * @param segments the cleaned description's segments
	 * @param locations the location strings to strip from the description
	 * @return the sorted, distinct description hashes
	 */
	public long[] shingles(List<Segment> segments, List<String> locations) {
		Set<String> omitted = new HashSet<>(OMIT);
		locations.forEach((location) -> omitted.addAll(tokens(location)));
		boolean allBoilerplate = segments.stream().allMatch(Segment::boilerplate);
		String text = String.join(" ", segments.stream().filter((segment) -> allBoilerplate || !segment.boilerplate())
			.map(Segment::text).toList());
		List<String> kept = tokens(text).stream().filter((token) -> !omitted.contains(token)).toList();
		List<Long> hashes = new ArrayList<>();
		for (int i = 0; i + SHINGLE <= kept.size(); i++) {
			hashes.add(hash(String.join(" ", kept.subList(i, i + SHINGLE))));
		}
		return hashes.stream().mapToLong(Long::longValue).sorted().distinct().toArray();
	}

	/**
	 * Whether the description is substantial enough to merge on.
	 * @param shingles the description's distinct hashes
	 * @return whether there are enough shingles to compare the description
	 */
	public boolean comparable(long[] shingles) {
		return shingles.length >= MIN_SHINGLES;
	}

	/**
	 * Jaccard overlap of sorted sets, or zero when either description is too thin.
	 * @param a the first description's sorted, distinct hashes
	 * @param b the second description's sorted, distinct hashes
	 * @return their Jaccard overlap, or zero when either description is too thin
	 */
	public double overlap(long[] a, long[] b) {
		if (!comparable(a) || !comparable(b)) {
			return 0;
		}
		int i = 0;
		int j = 0;
		int intersection = 0;
		while (i < a.length && j < b.length) {
			if (a[i] == b[j]) {
				intersection++;
				i++;
				j++;
			}
			else if (a[i] < b[j]) {
				i++;
			}
			else {
				j++;
			}
		}
		return (double) intersection / (a.length + b.length - intersection);
	}

	/** Reads lowercase letter tokens, discarding the whole token when it contains a digit. */
	private List<String> tokens(String text) {
		return SEPARATOR.splitAsStream(text.toLowerCase(Locale.ROOT))
			.filter((token) -> !token.isEmpty() && !NUMBER.matcher(token).matches()).toList();
	}

	/** FNV-1a over UTF-8 gives each shingle a stable 64-bit hash. */
	private long hash(String text) {
		long hash = 0xcbf29ce484222325L;
		for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
			hash ^= value & 0xff;
			hash *= 0x100000001b3L;
		}
		return hash;
	}

}
