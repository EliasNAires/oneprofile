package oneprofile.backend.storage.rolelist;

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
 * The role list (ADR-0024): the curated roles, the words of a normalized title the role rule reads
 * them from, and the normalized titles the rule gets wrong. What each holds is written in
 * {@code docs/role-criterion.md}, and the files answer to it.
 * <p>
 * It is read from three tab-separated files, each with a header line: the roles, as
 * {@code slug<TAB>label}; the words, as {@code phrase<TAB>use<TAB>head<TAB>role}; and the titles, as
 * {@code title<TAB>role}. A role is a slug of the roles file or {@code none}.
 * <p>
 * A list is refused if a word or a title names a role it does not hold, or if a word is read twice
 * the same way: as a head or a job twice, as an override twice, or as a kind twice beside one head.
 */
public class RoleListStore {

	/** What the files write for a title or a word that gives no role. */
	public static final String NONE = "none";

	private final List<RoleWord> words;

	private final Map<String, String> titles;

	private RoleListStore(List<RoleWord> words, Map<String, String> titles) {
		this.words = List.copyOf(words);
		this.titles = Map.copyOf(titles);
	}

	/**
	 * Reads a role list. The readers are left open for whoever opened them to close.
	 * @param roles the roles file
	 * @param words the words file
	 * @param titles the titles file
	 * @return the role list they hold
	 * @throws IllegalStateException if a role is not on the list, or a word is read twice the same way
	 */
	public static RoleListStore read(Reader roles, Reader words, Reader titles) {
		Set<String> slugs = lines(roles).stream().map((fields) -> fields[0]).collect(Collectors.toSet());
		Set<String> read = new HashSet<>();
		List<RoleWord> wordList = lines(words).stream().map((fields) -> {
			RoleWord word = new RoleWord(fields[0], RoleWordUseEnum.valueOf(fields[1].toUpperCase(Locale.ROOT)),
					field(fields, 2), field(fields, 3).isEmpty() ? null : field(fields, 3));
			if (!read.add(word.phrase() + "\t" + readAs(word))) {
				throw new IllegalStateException("'" + word.phrase() + "' is read twice as a " + word.use());
			}
			if (word.role() != null) {
				known(slugs, word.role(), word.phrase());
			}
			return word;
		}).toList();
		Map<String, String> titleMap = new HashMap<>();
		lines(titles).forEach((fields) -> {
			known(slugs, fields[1], fields[0]);
			if (titleMap.put(fields[0], fields[1]) != null) {
				throw new IllegalStateException("The title '" + fields[0] + "' is listed twice");
			}
		});
		return new RoleListStore(wordList, titleMap);
	}

	/**
	 * The words the rule reads a title by, in the order of their file.
	 * @return its words
	 */
	public List<RoleWord> words() {
		return this.words;
	}

	/**
	 * The normalized titles whose role the file gives rather than the rule, each with its role's slug or
	 * {@link #NONE}.
	 * @return its titles
	 */
	public Map<String, String> titles() {
		return this.titles;
	}

	/** How a word is read, of the ways only one of which a phrase may have. */
	private static String readAs(RoleWord word) {
		return switch (word.use()) {
			case HEAD, JOB -> "head";
			case KIND, YIELDING -> "kind\t" + word.head();
			case STAGE, OVERRIDE -> word.use().name();
		};
	}

	private static List<String[]> lines(Reader tsv) {
		return new BufferedReader(tsv).lines().skip(1).filter((line) -> !line.isBlank())
			.map((line) -> line.split("\t", -1)).toList();
	}

	private static String field(String[] fields, int index) {
		return (index < fields.length) ? fields[index] : "";
	}

	private static void known(Set<String> slugs, String role, String named) {
		if (!role.equals(NONE) && !slugs.contains(role)) {
			throw new IllegalStateException("'" + named + "' names the role '" + role + "', which is not on the list");
		}
	}

}
