package oneprofile.backend.workers.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.taxonomy.Skill;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.storage.vacancyskill.FoundSkill;
import org.springframework.stereotype.Component;

/**
 * Finds the skills of the taxonomy a vacancy names, in its title and in the segments of its
 * description that are not boilerplate. How it matches is ADR-0018.
 * <p>
 * Every key is read into one trie by its name form, and each segment is walked once: where a word
 * starts, the longest key that ends where a word ends is taken, and the walk resumes after it, so
 * keys never overlap and Java is not found inside JavaScript. A plain key is found in any case. A
 * context key is found only written in the casing the taxonomy gives it, and counts only where a
 * plain key confirms it: another skill found by a plain key in the same segment, or its parent or
 * one of its children found by a plain key anywhere in the vacancy. A child never adds its parent.
 * No key is found inside a URL, nor inside a few business phrases whose letters are context keys.
 */
@Component
public class SkillExtractionRule {

	/** Where a title's occurrences are kept apart from the description's segments. */
	private static final int TITLE = -1;

	/**
	 * Where no key starts: a URL up to the next whitespace, and the business phrases in which the
	 * context keys C and R are letters. The phrases are a fixed list, kept short on purpose.
	 */
	private static final Pattern SKIPPED = Pattern
		.compile("(?i)(?:https?://|www\\.)\\S*|\\b(?:C-level|C-suite|R&D)(?![\\p{L}\\p{N}])");

	private final Node keys = new Node();

	private final Map<String, Skill> skills = new HashMap<>();

	private final Map<String, Set<String>> children = new HashMap<>();

	/**
	 * @param taxonomy the skills to find, with their keys
	 */
	public SkillExtractionRule(TaxonomyStore taxonomy) {
		for (Skill skill : taxonomy.skills()) {
			this.skills.put(skill.id(), skill);
			if (skill.parent() != null) {
				this.children.computeIfAbsent(skill.parent(), (parent) -> new HashSet<>()).add(skill.id());
			}
			Set<String> contextKeys = new HashSet<>();
			skill.contextKeys().forEach((key) -> contextKeys.add(separated(key.strip())));
			add(skill.canonicalName(), skill, contextKeys);
			skill.aliases().forEach((alias) -> add(alias, skill, contextKeys));
		}
	}

	/**
	 * Finds the skills a vacancy names.
	 * @param title its cleaned title
	 * @param segments its description's segments, boilerplate among them
	 * @return one finding per skill, in the order first found
	 */
	public List<FoundSkill> extract(String title, List<Segment> segments) {
		Map<Integer, List<Occurrence>> bySegment = new LinkedHashMap<>();
		bySegment.put(TITLE, occurrences(title));
		for (int i = 0; i < segments.size(); i++) {
			if (!segments.get(i).boilerplate()) {
				bySegment.put(i, occurrences(segments.get(i).text()));
			}
		}
		Set<String> plainAnywhere = new HashSet<>();
		bySegment.values()
			.forEach((found) -> found.stream().filter((o) -> !o.context()).forEach((o) -> plainAnywhere.add(o.skill().id())));
		Map<String, Finding> findings = new LinkedHashMap<>();
		bySegment.forEach((segment, found) -> {
			Set<String> plainHere = new HashSet<>();
			found.stream().filter((o) -> !o.context()).forEach((o) -> plainHere.add(o.skill().id()));
			for (Occurrence occurrence : found) {
				String id = occurrence.skill().id();
				if (occurrence.context() && !confirmed(id, plainHere, plainAnywhere)) {
					continue;
				}
				Finding finding = findings.computeIfAbsent(id, (skill) -> new Finding());
				if (segment == TITLE) {
					finding.inTitle = true;
				}
				else {
					finding.segments.add(segment);
				}
				finding.byPlainKey |= !occurrence.context();
			}
		});
		List<FoundSkill> extracted = new ArrayList<>(findings.size());
		findings.forEach((id, finding) -> extracted
			.add(new FoundSkill(id, List.copyOf(finding.segments), finding.inTitle, !finding.byPlainKey)));
		return extracted;
	}

	private boolean confirmed(String id, Set<String> plainHere, Set<String> plainAnywhere) {
		if (plainHere.stream().anyMatch((other) -> !other.equals(id))) {
			return true;
		}
		String parent = this.skills.get(id).parent();
		if (parent != null && plainAnywhere.contains(parent)) {
			return true;
		}
		return this.children.getOrDefault(id, Set.of()).stream().anyMatch(plainAnywhere::contains);
	}

	/**
	 * Walks one text, taking at each word start outside a skipped span the longest key that ends at
	 * a word end and whose casing holds, and resuming after it.
	 */
	private List<Occurrence> occurrences(String text) {
		Folded folded = Folded.of(text);
		char[] chars = folded.chars();
		boolean[] skipped = skipped(text);
		List<Occurrence> found = new ArrayList<>();
		int i = 0;
		while (i < chars.length) {
			if (chars[i] == ' ' || (i > 0 && Character.isLetterOrDigit(chars[i - 1]))
					|| skipped[folded.origins()[i]]) {
				i++;
				continue;
			}
			Occurrence longest = null;
			int end = i;
			Node node = this.keys;
			for (int j = i; j < chars.length && node != null; j++) {
				node = node.next.get(chars[j]);
				if (node != null && node.skill != null
						&& (j + 1 == chars.length || !Character.isLetterOrDigit(chars[j + 1]))) {
					boolean context = node.contextCasings != null;
					if (!context || node.contextCasings.contains(separated(folded.original(i, j + 1)))) {
						longest = new Occurrence(node.skill, context);
						end = j + 1;
					}
				}
			}
			if (longest != null) {
				found.add(longest);
				i = end;
			}
			else {
				i++;
			}
		}
		return found;
	}

	/** Which characters of a text lie inside a URL or a skipped phrase. */
	private static boolean[] skipped(String text) {
		boolean[] skipped = new boolean[text.length()];
		Matcher matcher = SKIPPED.matcher(text);
		while (matcher.find()) {
			Arrays.fill(skipped, matcher.start(), matcher.end(), true);
		}
		return skipped;
	}

	private void add(String name, Skill skill, Set<String> contextKeys) {
		String key = Folded.of(name.strip()).folded();
		Node node = this.keys;
		for (char c : key.toCharArray()) {
			node = node.next.computeIfAbsent(c, (next) -> new Node());
		}
		node.skill = skill;
		if (contextKeys.stream().anyMatch((context) -> Folded.of(context).folded().equals(key))) {
			node.contextCasings = contextKeys;
		}
	}

	/** A name with each run of spaces, hyphens, underscores and slashes made one space, its case kept. */
	private static String separated(String name) {
		StringBuilder separated = new StringBuilder(name.length());
		for (char c : name.toCharArray()) {
			if (!Folded.separator(c)) {
				separated.append(c);
			}
			else if (separated.isEmpty() || separated.charAt(separated.length() - 1) != ' ') {
				separated.append(' ');
			}
		}
		return separated.toString();
	}

	/**
	 * A text in name form, lowercased and its separators made one space, a character at a time so
	 * that each folded character can be traced back to the text it came from. Within a name it gives
	 * what {@code TaxonomyStore.key} gives; it differs only at a text's ends, which a key never has
	 * once stripped.
	 */
	private record Folded(String text, char[] chars, int[] origins) {

		static Folded of(String text) {
			char[] chars = new char[text.length()];
			int[] origins = new int[text.length()];
			int length = 0;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (separator(c)) {
					if (length > 0 && chars[length - 1] == ' ') {
						continue;
					}
					c = ' ';
				}
				chars[length] = Character.toLowerCase(c);
				origins[length] = i;
				length++;
			}
			return new Folded(text, Arrays.copyOf(chars, length), Arrays.copyOf(origins, length));
		}

		static boolean separator(char c) {
			return Character.isWhitespace(c) || c == '-' || c == '_' || c == '/';
		}

		/** The folded text. */
		String folded() {
			return new String(this.chars);
		}

		/** The text the folded characters from {@code start} to {@code end} came from. */
		String original(int start, int end) {
			return this.text.substring(this.origins[start], this.origins[end - 1] + 1);
		}

	}

	private record Occurrence(Skill skill, boolean context) {
	}

	/** What is known of one skill while a vacancy is read. */
	private static final class Finding {

		private final Set<Integer> segments = new TreeSet<>();

		private boolean inTitle;

		private boolean byPlainKey;

	}

	/** One character of the trie; a node a key ends at holds its skill. */
	private static final class Node {

		private final Map<Character, Node> next = new HashMap<>();

		private Skill skill;

		/** The casings a context key ending here is found in, null for a plain key. */
		private Set<String> contextCasings;

	}

}
