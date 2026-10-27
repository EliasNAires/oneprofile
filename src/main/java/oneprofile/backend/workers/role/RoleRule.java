package oneprofile.backend.workers.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import oneprofile.backend.storage.rolelist.RoleListStore;
import oneprofile.backend.storage.rolelist.RoleWord;
import oneprofile.backend.storage.rolelist.RoleWordUseEnum;
import org.springframework.stereotype.Component;

/**
 * Gives a normalized title its role from the role list, as {@code docs/role-criterion.md} reads it
 * (ADR-0024, amended a fourth time 2026-10-26). The criterion is right where the two disagree; the
 * list's titles file holds the titles the rule gets wrong.
 * <p>
 * A title the titles file lists has the role the file gives. Otherwise its stages of a career are read
 * past, and its head is the longest run of its last words that the list names as one:
 * <ul>
 * <li>A job is read whole, whatever stands in front of it.</li>
 * <li>Beside any other head, a word the list names as deciding wherever it stands decides: a sale word
 * in front of an engineer or an architect, or a vendor's platform. Otherwise the kind of work named
 * nearest the head decides, its longest phrase read first; {@code platform}, {@code infrastructure}
 * and {@code software} yield to any other kind. A kind gives the role the list gives it beside that
 * head, or else beside any head, and a kind the list gives nothing beside that head is passed over.</li>
 * <li>A head with nothing in front of it gives the role the list gives it alone.</li>
 * <li>A title with no head on the list is read as an area, whole, as a kind or a deciding word.</li>
 * </ul>
 * What none of these reaches has no role. Where the words in front of a head give different roles, the
 * one that decided is kept and the others are reported, for an agent to settle.
 */
@Component
public class RoleRule {

	private static final Pattern SPACE = Pattern.compile(" ");

	/** The key of a kind's role beside any head. */
	private static final String ANY_HEAD = "";

	private final Map<String, String> titles;

	private final Map<String, RoleWord> heads = new HashMap<>();

	private final Set<String> stages = new HashSet<>();

	/** Each kind's role by its head, {@link #ANY_HEAD} for its role beside any. */
	private final Map<String, Map<String, String>> kinds = new HashMap<>();

	private final Set<String> yielding = new HashSet<>();

	private final Map<String, RoleWord> overrides = new HashMap<>();

	private final int longestPhrase;

	public RoleRule(RoleListStore roleList) {
		this.titles = roleList.titles();
		int longest = 1;
		for (RoleWord word : roleList.words()) {
			longest = Math.max(longest, SPACE.split(word.phrase()).length);
			switch (word.use()) {
				case HEAD, JOB -> this.heads.put(word.phrase(), word);
				case STAGE -> this.stages.add(word.phrase());
				case KIND -> this.kinds.computeIfAbsent(word.phrase(), (phrase) -> new HashMap<>())
					.put(word.head(), word.role());
				case YIELDING -> {
					this.kinds.computeIfAbsent(word.phrase(), (phrase) -> new HashMap<>()).put(word.head(), word.role());
					this.yielding.add(word.phrase());
				}
				case OVERRIDE -> this.overrides.put(word.phrase(), word);
			}
		}
		this.longestPhrase = longest;
	}

	/**
	 * The roles of a vacancy: the role of each of its normalized titles that has one, once each.
	 * @param normalizedTitles its normalized titles, one per head its title names
	 * @return the slugs of its roles, in the order its titles name them
	 */
	public List<String> roles(List<String> normalizedTitles) {
		Set<String> roles = new LinkedHashSet<>();
		normalizedTitles.stream().map((title) -> read(title).role())
			.filter((role) -> !role.equals(RoleListStore.NONE)).forEach(roles::add);
		return List.copyOf(roles);
	}

	/**
	 * Reads one normalized title's role.
	 * @param normalizedTitle a normalized title, as the title breakdown writes it
	 * @return its role and how it was read
	 */
	public RoleReading read(String normalizedTitle) {
		String listed = this.titles.get(normalizedTitle);
		if (listed != null) {
			return reading(listed, RoleReadingEnum.LISTED);
		}
		List<String> words = Arrays.stream(SPACE.split(normalizedTitle))
			.filter((word) -> !word.isEmpty() && !this.stages.contains(word)).toList();
		if (words.isEmpty()) {
			return reading(RoleListStore.NONE, RoleReadingEnum.STAGE);
		}
		String unstaged = String.join(" ", words);
		listed = this.titles.get(unstaged);
		if (listed != null) {
			return reading(listed, RoleReadingEnum.LISTED);
		}
		String head = longest(words, words.size(), this.heads::containsKey);
		if (head != null) {
			return headed(this.heads.get(head), words.subList(0, words.size() - length(head)));
		}
		RoleWord override = this.overrides.get(unstaged);
		if (override != null && override.head().isEmpty()) {
			return reading(override.role(), RoleReadingEnum.OVERRIDE);
		}
		String area = role(unstaged, ANY_HEAD);
		return (area != null) ? reading(area, RoleReadingEnum.AREA)
				: reading(RoleListStore.NONE, RoleReadingEnum.UNREACHED);
	}

	private RoleReading headed(RoleWord head, List<String> front) {
		if (head.use() == RoleWordUseEnum.JOB) {
			return reading(head.role(), RoleReadingEnum.JOB);
		}
		if (front.isEmpty()) {
			return (head.role() != null) ? reading(head.role(), RoleReadingEnum.BARE)
					: reading(RoleListStore.NONE, RoleReadingEnum.UNREACHED);
		}
		List<String> kinds = new ArrayList<>();
		List<String> yieldingKinds = new ArrayList<>();
		List<String> overriding = new ArrayList<>();
		int end = front.size();
		while (end > 0) {
			String phrase = longest(front, end,
					(words) -> this.kinds.containsKey(words) || this.overrides.containsKey(words));
			if (phrase == null) {
				end--;
				continue;
			}
			end -= length(phrase);
			RoleWord override = this.overrides.get(phrase);
			if (override != null && decidesBeside(override, head.phrase())) {
				overriding.add(override.role());
			}
			String role = role(phrase, head.phrase());
			if (role != null) {
				(this.yielding.contains(phrase) ? yieldingKinds : kinds).add(role);
			}
		}
		if (!overriding.isEmpty()) {
			return decided(overriding, List.of(), RoleReadingEnum.OVERRIDE);
		}
		if (!kinds.isEmpty()) {
			// A yielding word gives way only to a kind with a role; where the kind gives none, an agent settles it.
			return decided(kinds, kinds.get(0).equals(RoleListStore.NONE) ? yieldingKinds : List.of(),
					RoleReadingEnum.KIND);
		}
		if (!yieldingKinds.isEmpty()) {
			return reading(yieldingKinds.get(0), RoleReadingEnum.KIND);
		}
		return reading(RoleListStore.NONE, RoleReadingEnum.UNREACHED);
	}

	/**
	 * The longest phrase the list knows that ends at a place in a title's words.
	 * @return the phrase, or null if no phrase ending there is known
	 */
	private String longest(List<String> words, int end, Predicate<String> known) {
		for (int length = Math.min(this.longestPhrase, end); length > 0; length--) {
			String phrase = String.join(" ", words.subList(end - length, end));
			if (known.test(phrase)) {
				return phrase;
			}
		}
		return null;
	}

	/** The role the nearest gives, and those the others and the passed-over give that differ from it. */
	private static RoleReading decided(List<String> nearestFirst, List<String> passedOver, RoleReadingEnum reading) {
		String role = nearestFirst.get(0);
		Set<String> competing = new LinkedHashSet<>(nearestFirst);
		competing.addAll(passedOver);
		competing.remove(role);
		return new RoleReading(role, reading, competing);
	}

	/** Whether an override decides beside a head: one it names, or one ending in it ({@code systems engineer}). */
	private static boolean decidesBeside(RoleWord override, String head) {
		return override.head().isEmpty() || Arrays.stream(override.head().split("\\|"))
			.anyMatch((named) -> head.equals(named) || head.endsWith(" " + named));
	}

	/** The role a kind gives beside a head, null where the list gives it none there. */
	private String role(String kind, String head) {
		Map<String, String> byHead = this.kinds.get(kind);
		if (byHead == null) {
			return null;
		}
		return byHead.containsKey(head) ? byHead.get(head) : byHead.get(ANY_HEAD);
	}

	private static int length(String phrase) {
		return SPACE.split(phrase).length;
	}

	private static RoleReading reading(String role, RoleReadingEnum reading) {
		return new RoleReading(role, reading, Set.of());
	}

}
