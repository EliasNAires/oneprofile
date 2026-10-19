package oneprofile.backend.storage.taxonomy;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The curated set of skills, and the resolution of a name to the skill it names.
 * <p>
 * The taxonomy is read from a file with one skill per line, as
 * {@code id<TAB>canonical_name<TAB>category<TAB>aliases<TAB>context_keys<TAB>parent}, its aliases and
 * its context keys separated by {@code |}, and its parent the id of the skill it belongs to, if any
 * (Spring Security to the Spring Framework). The parent is not read here yet. A name is looked up by a key, its name form: the name
 * lowercased, each run of spaces, hyphens, underscores and slashes between two other characters one
 * space, the ends trimmed. Discovery's {@code skillloop.name_form} gives the same key, and both are
 * tested on the cases of {@code taxonomy/name-forms.tsv} (#63). The canonical name is a key like any
 * alias. The context keys are not told apart here yet (#15).
 * <p>
 * A file is refused if two of its skills share an id, or if one key would name two skills.
 */
public class TaxonomyStore {

	private static final Pattern ENDS = Pattern.compile("^\\s+|\\s+$", Pattern.UNICODE_CHARACTER_CLASS);

	private static final Pattern SEPARATED = Pattern.compile("(?<=[^\\s_/-])[\\s_/-]+(?=[^\\s_/-])",
			Pattern.UNICODE_CHARACTER_CLASS);

	private final Map<String, Skill> byKey;

	private TaxonomyStore(Map<String, Skill> byKey) {
		this.byKey = byKey;
	}

	/**
	 * Reads a taxonomy file. The reader is left open for whoever opened it to close.
	 * @param tsv the file's text, one skill per line
	 * @return the taxonomy it holds
	 * @throws IllegalStateException if two skills share an id, or a key names two skills
	 */
	public static TaxonomyStore read(Reader tsv) {
		Set<String> ids = new HashSet<>();
		Map<String, Skill> byKey = new HashMap<>();
		new BufferedReader(tsv).lines().map(TaxonomyStore::parse).forEach((skill) -> {
			if (!ids.add(skill.id())) {
				throw new IllegalStateException("Two skills have the id '" + skill.id() + "'");
			}
			index(byKey, skill.canonicalName(), skill);
			skill.aliases().forEach((alias) -> index(byKey, alias, skill));
		});
		return new TaxonomyStore(byKey);
	}

	/**
	 * Resolves a name to the skill it names.
	 * @param name a canonical name or an alias, in any case and spacing, its words parted by any separator
	 * @return the skill, or empty if the taxonomy has no skill by that name
	 */
	public Optional<Skill> resolve(String name) {
		return Optional.ofNullable(this.byKey.get(key(name)));
	}

	private static Skill parse(String line) {
		String[] fields = line.split("\t", -1);
		List<String> aliases = fields[3].isEmpty() ? List.of() : List.of(fields[3].split("\\|"));
		return new Skill(fields[0], fields[1], fields[2], aliases);
	}

	private static void index(Map<String, Skill> byKey, String name, Skill skill) {
		Skill named = byKey.putIfAbsent(key(name), skill);
		if (named != null && named != skill) {
			throw new IllegalStateException(
					"'" + key(name) + "' names both '" + named.id() + "' and '" + skill.id() + "'");
		}
	}

	static String key(String name) {
		String lowered = ENDS.matcher(name).replaceAll("").toLowerCase(Locale.ROOT);
		return SEPARATED.matcher(lowered).replaceAll(" ");
	}

}
