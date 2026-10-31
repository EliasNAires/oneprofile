package oneprofile.backend.workers.normalization;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.springframework.stereotype.Component;

/**
 * Reads the work mode of an engineering vacancy from what it declares, in the order of evidence of
 * #86 and #98: the location text where it names a mode; otherwise the work-mode field its ATS publishes;
 * otherwise the title; otherwise the description; otherwise none.
 * <p>
 * The location and the title are read by the work-mode words the title breakdown takes out of a title
 * (docs/title-breakdown-criterion.md, rule 7), one list for both. A field is read by its whole value,
 * because a tenant's own text can name a mode it is not ({@code Office Worker (NOT Remote)}); a value the
 * vocabulary does not hold is no answer.
 * <p>
 * A text offering a choice of modes keeps every mode it offers, so a filter on any of them finds the vacancy
 * (#98): its options are cut at an {@code or} or a slash with a mode beside it, and at semicolons
 * ({@code Nashville (Onsite) or Remote}, {@code Remote/Hybrid}). A mode named only to rule it out ({@code no remote
 * or hybrid option}) is not offered, nor are the parts a hybrid mode names ({@code Hybrid (onsite/remote)}), and a
 * bare mode before a semicolon gives way to what explains it. A text naming several modes with a schedule is
 * one mode, hybrid ({@code Hybrid (3x in office/week)}, {@code 3 days onsite; 2 days remote}), and so is a mode
 * with up to four days a week ({@code ONSITE 2X Week}) or in the office ({@code 4 days in office}) and an onsite
 * text naming a stretch of weekdays short of the week ({@code onsite Tuesday through Thursday}). Any other text
 * naming several modes names one beside its mode ({@code remote, with occasional travel onsite}), and is read by
 * the first it names.
 * <p>
 * The description never gives a mode by a bare word, which often names the company's culture, a benefit, a
 * technology or a duty (#98). It is read by what states this vacancy's own mode, strongest first: a sentence
 * whose subject is this role or the team it joins ({@code This role has been designed as 'Hybrid'}, {@code This
 * is a fully remote position}, {@code Hybrid Role (Minimum of 3 days a week in the office)}, {@code The teams are
 * all home based}), with the mode within its first words and no negation, condition or {@code may} before it;
 * then a short segment offering a choice of modes ({@code United States (Hybrid or Remote)}); then a label of the
 * work mode with its value ({@code Workplace type: Remote}, {@code Telecommuting Options} followed by
 * {@code Onsite}, or a mode under a {@code Job Details} heading); then a LinkedIn tag ({@code #LI-Hybrid}). A
 * list of modes alone ({@code Hybrid / Onsite / Remote}) is read by the schedule after it, and a list of every
 * mode with none after it declares nothing. Where the statements of the strongest kind found disagree, it gives
 * none.
 */
@Component
public class WorkModeRule {

	/** The words that name a work mode in a title or a location, with the mode each names. */
	static final Map<String, WorkModeEnum> WORDS = Map.ofEntries(Map.entry("remote", WorkModeEnum.REMOTE),
			Map.entry("fully remote", WorkModeEnum.REMOTE), Map.entry("remote first", WorkModeEnum.REMOTE),
			Map.entry("work from home", WorkModeEnum.REMOTE), Map.entry("wfh", WorkModeEnum.REMOTE),
			Map.entry("telecommute", WorkModeEnum.REMOTE), Map.entry("telework", WorkModeEnum.REMOTE),
			Map.entry("remoto", WorkModeEnum.REMOTE), Map.entry("remota", WorkModeEnum.REMOTE),
			Map.entry("home office", WorkModeEnum.REMOTE), Map.entry("home based", WorkModeEnum.REMOTE),
			Map.entry("hybrid", WorkModeEnum.HYBRID), Map.entry("híbrido", WorkModeEnum.HYBRID),
			Map.entry("hibrido", WorkModeEnum.HYBRID), Map.entry("onsite", WorkModeEnum.ONSITE),
			Map.entry("on site", WorkModeEnum.ONSITE), Map.entry("in office", WorkModeEnum.ONSITE),
			Map.entry("presencial", WorkModeEnum.ONSITE), Map.entry("office based", WorkModeEnum.ONSITE));

	/** Where a work-mode word names what the work is about rather than where it is done. */
	private static final Set<String> SUBJECTS = Set.of("hybrid cloud", "hybrid infrastructure", "hybrid it",
			"hybrid deployment", "hybrid deployments", "remote sensing", "remote access", "remote assist");

	/** Up to four days a week, as an option qualifies its mode by them: {@code 3 days a week}, {@code 2X Week}. */
	private static final Pattern DAYS_A_WEEK = Pattern.compile(
			"\\b(?:[1-4]|one|two|three|four)\\s*\\+?\\s*(?:x|days?|times)\\b[^.;]{0,30}?\\bweeks?\\b",
			Pattern.CASE_INSENSITIVE);

	/** Up to four days in the office, with no week named: {@code 4 days in office}. */
	private static final Pattern OFFICE_DAYS = Pattern.compile(
			"\\b(?:[1-4]|one|two|three|four)\\s+days?\\s+(?:in|at)\\s+(?:the\\s+|our\\s+|an?\\s+)?office\\b",
			Pattern.CASE_INSENSITIVE);

	/**
	 * The words that name a work mode in a statement of the description: those of a title or a location, and
	 * the forms a sentence uses.
	 */
	private static final Map<String, WorkModeEnum> DESCRIPTION_WORDS = described();

	/** Where a work-mode word of a description names what the work is about, who it is with, or a benefit. */
	private static final Set<String> DESCRIPTION_SUBJECTS = describedSubjects();

	/** A mode as a description writes it, for where a choice is cut and what a list of modes holds. */
	private static final String MODE = "(?:remote|hybrid|onsite|on-site|on site|in-office|in office|office-based|"
			+ "office based|home-based|home based)";

	/**
	 * Where a text naming several modes is cut into the options of a choice: at {@code or} ({@code (or remote in the
	 * US)}, {@code and/or}), at a comma in a list of modes that ends in one ({@code onsite, hybrid, or remote}), and
	 * at a slash between two modes ({@code Remote/Hybrid}, {@code Office-Based (Leeds) / Home-Based}, never
	 * {@code 3x/week} or {@code a hybrid of office/remote}).
	 */
	private static final Pattern CHOICE = Pattern.compile("(?<plain>\\s+or\\s+)|,\\s*or\\s+|\\s*\\(\\s*or\\s+"
			+ "|\\s+and\\s*/\\s*or\\s+"
			+ "|(?<=\\b" + MODE + ")\\s*,\\s*(?=" + MODE + "\\b\\s*,?\\s*(?:and\\s*/\\s*)?or\\s)"
			+ "|(?<=\\b" + MODE + "|\\))\\s*/\\s*(?=(?:" + MODE + "|office)\\b)", Pattern.CASE_INSENSITIVE);

	/** Only modes, between slashes, bars or commas: a list ({@code Hybrid / Onsite / Remote}), not a sentence. */
	private static final Pattern LIST = Pattern.compile("^\\W*" + MODE + "(?:\\s*[/|,]\\s*" + MODE + ")+\\W*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * A mode named only to rule it out, with the modes listed after it: {@code no remote or hybrid option},
	 * {@code not remote}.
	 */
	private static final Pattern RULED_OUT = Pattern.compile("\\b(?:no|not|non|never|without)[\\s-]+"
			+ "(?:remote(?:ly)?|hybrid|on[- ]?site|in[- ]office)(?:\\s*(?:,|/|or|and|and/or|nor)\\s*"
			+ "(?:remote(?:ly)?|hybrid|on[- ]?site|in[- ]office))*\\b", Pattern.CASE_INSENSITIVE);

	/** What a hybrid mode says it is made of: {@code Hybrid (onsite/remote)}, {@code Hybrid (3 days in office)}. */
	private static final Pattern HYBRID_PARTS = Pattern.compile("(\\bhybrid)\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE);

	/** A schedule: days a week, days of the week, or a count of days ({@code 3 days onsite}, {@code 2x}). */
	private static final Pattern SCHEDULE = Pattern.compile("\\b(?:[1-4]|one|two|three|four)\\s*\\+?\\s*(?:x|days?)\\b"
			+ "|\\b(?:mon|tues|wednes|thurs|fri)days?\\b", Pattern.CASE_INSENSITIVE);

	/** A stretch of weekdays: {@code Tuesday through Thursday}, {@code Mon-Thu}. */
	private static final Pattern WEEKDAYS = Pattern.compile("\\b(mon|tue|wed|thu|fri)[a-z]*\\s*(?:-|–|through|thru|to)\\s*"
			+ "(mon|tue|wed|thu|fri)[a-z]*\\b", Pattern.CASE_INSENSITIVE);

	/** The most words a work-mode word takes, as many as are read beside the {@code or} of a choice. */
	private static final int EDGE_WORDS = 3;

	/** A LinkedIn tag of the work mode: {@code #LI-Hybrid}, {@code LI-Remote}. */
	private static final Pattern TAG = Pattern.compile("(?<![\\p{L}\\p{N}])#?LI[-_](remote|hybrid|on-?site)\\b",
			Pattern.CASE_INSENSITIVE);

	/** What a description labels a vacancy's work mode by. */
	private static final String LABEL = "(?:work(?:place|site|ing)?\\s*(?:type|model|arrangement|style|location|mode|"
			+ "environment|setting|flexibility|options?|pattern|schedule)|workstyle|(?:job\\s+)?location(?:\\s+type)?|remote\\s+type|"
			+ "telecommut\\w*\\s+options?|position\\s+(?:role\\s+)?type|type\\s+of\\s+work(?:\\s+model)?|"
			+ "worksite\\s+flexibility|flexible\\s+work(?:ing)?\\s+arrangements?|remote\\s+option\\s+availability)";

	/** What a label's value starts with where it names a mode. */
	private static final String MODE_FIRST = "(?=(?:fully\\s+|100\\s*%\\s*)?(?:remote|hybrid|on[- ]?site|in[- ]office|"
			+ "office[- ]based|work(?:ing)?\\s+from\\s+home|telecommut\\w*|telework\\w*|wfh)\\b)";

	/** A label with its value in one segment: {@code Workplace type: Remote}. */
	private static final Pattern LABELLED = Pattern.compile("^\\W*" + LABEL + "\\s*[:\\-–—]\\s*" + MODE_FIRST + "(.+)$",
			Pattern.CASE_INSENSITIVE);

	/** A label alone, whose value is the next segment: {@code Telecommuting Options}. */
	private static final Pattern LABEL_ALONE = Pattern.compile("^\\W*" + LABEL + "\\s*[:\\-–—]?\\s*$",
			Pattern.CASE_INSENSITIVE);

	/** A segment that is a label's value, starting with a mode. */
	private static final Pattern VALUE = Pattern.compile("^\\W*" + MODE_FIRST, Pattern.CASE_INSENSITIVE);

	/** The most words a label's value in a segment of its own holds. */
	private static final int VALUE_WORDS = 6;

	/**
	 * This role as the subject of a sentence: {@code This position}, {@code The role}, {@code This is a}; or the team
	 * it joins ({@code The teams are all home based}).
	 */
	private static final Pattern THIS_ROLE = Pattern.compile("\\b(?:this|the)\\s+(?:[\\p{L}-]+\\s+){0,2}?(?:role|position|"
			+ "job|vacancy|requisition)\\b|\\bthis\\s+(?:[\\p{L}-]+\\s+){0,2}?opportunity\\b|\\bthis\\s+is\\s+an?\\b"
			+ "|(?<team>\\b(?:this|the|our)\\s+(?:[\\p{L}-]+\\s+){0,2}?teams?\\b)", Pattern.CASE_INSENSITIVE);

	/** This role joining a team of a mode: {@code as part of a remote, globally distributed team}. */
	private static final Pattern JOINS_A_TEAM = Pattern.compile("\\b(?:part\\s+of|join(?:ing)?)\\s+(?:a|an|our|the)\\s+"
			+ "(?:fully\\s+|100\\s*%\\s*)?(remote|hybrid)\\b,?\\s+(?:[\\p{L}-]+,?\\s+){0,3}?teams?\\b",
			Pattern.CASE_INSENSITIVE);

	/** A choice left to the role or the team, not offered to the candidate: {@code depending on team needs}. */
	private static final Pattern DEPENDING = Pattern.compile("\\bdepending\\s+on\\s+(?:the\\s+|your\\s+)?(?:role|team|"
			+ "position|business|needs)", Pattern.CASE_INSENSITIVE);

	/** The heading a vacancy's own labels sit under: {@code Job Details}. */
	private static final Pattern DETAILS = Pattern.compile("^\\W*(?:job|position|role)\\s+(?:details|information|summary)\\W*$",
			Pattern.CASE_INSENSITIVE);

	/** The most words a segment offering a choice holds. */
	private static final int OFFER_WORDS = 30;

	/** The most words before the first mode of a segment offering a choice. */
	private static final int OFFER_OPENING_WORDS = 8;

	/**
	 * What makes a segment offering modes a company's policy for some roles or a benefit, not this one's offer:
	 * {@code for most roles}, {@code remote or hybrid roles enjoy}, {@code a stipend for eligible staff}.
	 */
	private static final Set<String> HEDGES = Set.of("most", "some", "many", "certain", "select", "roles",
			"employees", "staff", "eligible", "benefit", "benefits", "stipend", "allowance", "perk", "perks");

	/** A sentence that starts with its mode and this role: {@code Hybrid Role (…)}. */
	private static final Pattern MODE_ROLE = Pattern.compile("^\\W*(?:fully\\s+|100\\s*%\\s*)?(?:remote|hybrid|"
			+ "on[- ]?site|in[- ]office)\\s+(?:role|position|opportunity|job)\\b", Pattern.CASE_INSENSITIVE);

	/** The most words from this role to the mode a sentence states for it. */
	private static final int STATEMENT_WORDS = 10;

	/** What before this role makes a sentence a condition: {@code If this position is listed as remote}. */
	private static final Set<String> CONDITIONS = Set.of("if", "whether", "unless", "when");

	/** A negation, which after a sentence's mode rules out the others it names ({@code remote work not considered}). */
	private static final Set<String> NEGATIONS = Set.of("not", "no", "non", "never", "cannot", "isn", "won", "neither",
			"nor");

	/** What before a sentence's mode keeps it from stating it: a negation, a condition, a possibility. */
	private static final Set<String> NOT_STATED = notStated();

	/** What before this role makes it the frame of a duty: {@code In this role, you will lead …}. */
	private static final Pattern FRAME = Pattern.compile("^\\W*in\\s*$", Pattern.CASE_INSENSITIVE);

	/** Where one sentence of a segment ends. */
	private static final Pattern SENTENCE_END = Pattern.compile("[.!?](?=\\s|$)|&#xa;");

	/**
	 * The values of the ATSs' work-mode fields that name a mode, read as words: Ashby's and Lever's
	 * workplace types, and what Workday's tenants write in their remote type.
	 */
	private static final Map<String, WorkModeEnum> FIELD_VALUES = Map.ofEntries(
			Map.entry("remote", WorkModeEnum.REMOTE), Map.entry("fully remote", WorkModeEnum.REMOTE),
			Map.entry("hybrid", WorkModeEnum.HYBRID), Map.entry("hybrid working", WorkModeEnum.HYBRID),
			Map.entry("onsite", WorkModeEnum.ONSITE), Map.entry("on site", WorkModeEnum.ONSITE),
			Map.entry("fully onsite", WorkModeEnum.ONSITE), Map.entry("onsite job", WorkModeEnum.ONSITE),
			Map.entry("office", WorkModeEnum.ONSITE), Map.entry("in office", WorkModeEnum.ONSITE),
			Map.entry("office worker not remote", WorkModeEnum.ONSITE));

	private final TitleCleaningRule cleaning;

	/**
	 * @param cleaning the rule whose reading of a text's words the location, the field and the title get
	 */
	public WorkModeRule(TitleCleaningRule cleaning) {
		this.cleaning = cleaning;
	}

	/**
	 * Reads one vacancy's work modes.
	 * @param location its location text, or null if it has none
	 * @param field the work-mode field its ATS publishes, as the ATS writes it, or null if it has none
	 * @param title its title as the company wrote it
	 * @param segments its description's segments, empty where it has none
	 * @return its work modes and the step that decided them, or {@link WorkModeReading#NONE}
	 */
	public WorkModeReading read(String location, String field, String title, List<Segment> segments) {
		Set<WorkModeEnum> named = modesNamedIn(location);
		if (!named.isEmpty()) {
			return new WorkModeReading(named, WorkModeEvidenceEnum.LOCATION);
		}
		WorkModeEnum published = (field != null) ? FIELD_VALUES.get(words(field)) : null;
		if (published != null) {
			return new WorkModeReading(Set.of(published), WorkModeEvidenceEnum.FIELD);
		}
		named = modesNamedIn(title);
		if (!named.isEmpty()) {
			return new WorkModeReading(named, WorkModeEvidenceEnum.TITLE);
		}
		named = stated(segments);
		return !named.isEmpty() ? new WorkModeReading(named, WorkModeEvidenceEnum.DESCRIPTION) : WorkModeReading.NONE;
	}

	/**
	 * Every mode a text offers by its work-mode words: one, or each of a choice it offers.
	 * @param text a location string or a title, or null
	 * @return the modes it offers, empty where it names none
	 */
	public Set<WorkModeEnum> modesNamedIn(String text) {
		return (text != null) ? offered(text, WORDS, SUBJECTS) : EnumSet.noneOf(WorkModeEnum.class);
	}

	/** The modes a text offers, read by these words with these subjects left out. */
	private Set<WorkModeEnum> offered(String text, Map<String, WorkModeEnum> vocabulary, Set<String> subjects) {
		text = HYBRID_PARTS.matcher(RULED_OUT.matcher(text).replaceAll(" ")).replaceAll("$1");
		Set<WorkModeEnum> named = wordsNamedIn(text, vocabulary, subjects);
		if (named.size() == 1) {
			boolean scheduled = DAYS_A_WEEK.matcher(text).find()
					|| named.contains(WorkModeEnum.ONSITE) && (partOfTheWeek(text) || OFFICE_DAYS.matcher(text).find());
			return scheduled ? EnumSet.of(WorkModeEnum.HYBRID) : named;
		}
		if (named.isEmpty()) {
			return named;
		}
		List<String> options = options(text, vocabulary, subjects);
		if (options.size() > 1) {
			return offeredByOption(options, vocabulary, subjects);
		}
		if (SCHEDULE.matcher(text).find()) {
			return EnumSet.of(WorkModeEnum.HYBRID);
		}
		if (text.contains(";")) {
			String[] parts = text.split(";", 2);
			if (vocabulary.containsKey(words(parts[0])) && !wordsNamedIn(parts[1], vocabulary, subjects).isEmpty()) {
				return offered(parts[1], vocabulary, subjects);
			}
			return offeredByOption(List.of(text.split(";")), vocabulary, subjects);
		}
		return EnumSet.of(first(spaced(text, subjects), vocabulary));
	}

	/**
	 * The options of a choice a text offers: it is cut where a mode stands beside the {@code or} or slash
	 * ({@code Nashville (Onsite) or Remote}), never at one between places ({@code onsite in our Austin or Dallas
	 * office}), nor at a plain {@code or} after days a week, which says where the other days are spent
	 * ({@code 3 days a week in the office, and remotely or in the office the rest}).
	 */
	private List<String> options(String text, Map<String, WorkModeEnum> vocabulary, Set<String> subjects) {
		List<String> options = new ArrayList<>();
		Matcher choice = CHOICE.matcher(text);
		int from = 0;
		while (choice.find()) {
			if (choice.group("plain") != null && DAYS_A_WEEK.matcher(text.substring(0, choice.start())).find()) {
				continue;
			}
			String before = text.substring(from, choice.start());
			String after = text.substring(choice.end());
			if (!wordsNamedIn(edge(before, true), vocabulary, subjects).isEmpty()
					|| !wordsNamedIn(edge(after, false), vocabulary, subjects).isEmpty()) {
				options.add(before);
				from = choice.end();
			}
		}
		options.add(text.substring(from));
		return options;
	}

	/** The last words of a text, or its first ones: as many as a work-mode word may take. */
	private String edge(String text, boolean last) {
		String[] words = words(text).split(" ");
		int count = Math.min(EDGE_WORDS, words.length);
		int start = last ? words.length - count : 0;
		return String.join(" ", List.of(words).subList(start, start + count));
	}

	/** The modes a text's work-mode words name, every one of them. */
	private Set<WorkModeEnum> wordsNamedIn(String text, Map<String, WorkModeEnum> vocabulary, Set<String> subjects) {
		String spaced = spaced(text, subjects);
		Set<WorkModeEnum> named = EnumSet.noneOf(WorkModeEnum.class);
		for (Map.Entry<String, WorkModeEnum> word : vocabulary.entrySet()) {
			if (spaced.contains(" " + word.getKey() + " ")) {
				named.add(word.getValue());
			}
		}
		return named;
	}

	/** Whether a text names a stretch of weekdays short of the whole week: {@code Tuesday through Thursday}. */
	private static boolean partOfTheWeek(String text) {
		Matcher days = WEEKDAYS.matcher(text);
		while (days.find()) {
			if (!days.group(1).equalsIgnoreCase("mon") || !days.group(2).equalsIgnoreCase("fri")) {
				return true;
			}
		}
		return false;
	}

	/** The modes each option of a text offers, together. */
	private Set<WorkModeEnum> offeredByOption(List<String> options, Map<String, WorkModeEnum> vocabulary,
			Set<String> subjects) {
		Set<WorkModeEnum> modes = EnumSet.noneOf(WorkModeEnum.class);
		for (String option : options) {
			modes.addAll(offered(option, vocabulary, subjects));
		}
		return modes;
	}

	/** The mode of the first work-mode word of a text, as {@link #spaced} gives it, or null where it names none. */
	private static WorkModeEnum first(String spaced, Map<String, WorkModeEnum> vocabulary) {
		int earliest = earliest(spaced, vocabulary);
		for (Map.Entry<String, WorkModeEnum> word : vocabulary.entrySet()) {
			if (spaced.indexOf(" " + word.getKey() + " ") == earliest) {
				return word.getValue();
			}
		}
		return null;
	}

	/** Where the first work-mode word of a text, as {@link #spaced} gives it, starts: its length where none does. */
	private static int earliest(String spaced, Map<String, WorkModeEnum> vocabulary) {
		int earliest = spaced.length();
		for (String word : vocabulary.keySet()) {
			int at = spaced.indexOf(" " + word + " ");
			if (at >= 0 && at < earliest) {
				earliest = at;
			}
		}
		return earliest;
	}

	/**
	 * The modes a description states for this vacancy: those of its sentences about this role, else of the
	 * choices it offers, else of its labels, else of its tags; none where the statements of that kind disagree.
	 */
	private Set<WorkModeEnum> stated(List<Segment> segments) {
		List<Set<WorkModeEnum>> sentences = new ArrayList<>();
		List<Set<WorkModeEnum>> offers = new ArrayList<>();
		List<Set<WorkModeEnum>> labels = new ArrayList<>();
		List<Set<WorkModeEnum>> tags = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++) {
			String text = segments.get(i).text();
			if (text == null || text.isBlank()) {
				continue;
			}
			Set<WorkModeEnum> said = sentence(text);
			add(sentences, said);
			if (said.isEmpty()) {
				add(offers, offer(text, segments, i));
			}
			Matcher labelled = LABELLED.matcher(text);
			if (labelled.find()) {
				add(labels, listed(labelled.group(1), segments, i));
			}
			else if (i + 1 < segments.size() && LABEL_ALONE.matcher(text).find()) {
				String value = segments.get(i + 1).text();
				if (value != null && VALUE.matcher(value).find() && value.strip().split("\\s+").length <= VALUE_WORDS) {
					add(labels, listed(value, segments, i + 1));
				}
			}
			else if (segments.get(i).under() != null && DETAILS.matcher(segments.get(i).under()).find()
					&& text.strip().split("\\s+").length <= VALUE_WORDS) {
				add(labels, listed(text, segments, i));
			}
			Matcher tag = TAG.matcher(text);
			while (tag.find()) {
				add(tags, offered(tag.group(1), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS));
			}
		}
		for (List<Set<WorkModeEnum>> kind : List.of(sentences, offers, labels, tags)) {
			if (!kind.isEmpty()) {
				return (new HashSet<>(kind).size() == 1) ? kind.getFirst() : EnumSet.noneOf(WorkModeEnum.class);
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	private static void add(List<Set<WorkModeEnum>> statements, Set<WorkModeEnum> modes) {
		if (!modes.isEmpty()) {
			statements.add(modes);
		}
	}

	/**
	 * The modes a segment offers a choice of, though no sentence of it is about this role ({@code United States
	 * (Hybrid or Remote)}, {@code Office-Based (Leeds) / Home-Based}): empty where it offers one mode, is long
	 * prose, names its first mode late, or hedges or conditions it ({@code for most roles}, {@code if}).
	 */
	private Set<WorkModeEnum> offer(String text, List<Segment> segments, int at) {
		String spaced = spaced(text, DESCRIPTION_SUBJECTS);
		List<String> words = List.of(spaced.strip().split(" "));
		int earliest = earliest(spaced, DESCRIPTION_WORDS);
		if (words.size() > OFFER_WORDS || earliest == spaced.length() || LABELLED.matcher(text).find()) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		List<String> opening = List.of(spaced.substring(0, earliest).strip().split(" "));
		if (opening.size() > OFFER_OPENING_WORDS || opening.stream().anyMatch(CONDITIONS::contains)
				|| words.stream().anyMatch(HEDGES::contains) || DEPENDING.matcher(text).find()) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		if (LIST.matcher(text).find()) {
			return listed(text, segments, at);
		}
		String unruled = RULED_OUT.matcher(text).replaceAll(" ");
		Set<WorkModeEnum> offered = offered(text, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
		return (offered.size() > 1 && options(unruled, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).size() > 1) ? offered
				: EnumSet.noneOf(WorkModeEnum.class);
	}

	/**
	 * The modes a label's value gives. A list of modes alone ({@code Hybrid / Onsite / Remote}) is narrowed by the
	 * first segment after it naming a mode with a schedule ({@code 3 days in the office}); where none does, a list
	 * of every mode is a template and gives none.
	 */
	private Set<WorkModeEnum> listed(String value, List<Segment> segments, int at) {
		if (!LIST.matcher(value).find()) {
			return offered(value, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
		}
		for (Segment after : segments.subList(at + 1, segments.size())) {
			String text = after.text();
			if (text != null && SCHEDULE.matcher(text).find()
					&& !wordsNamedIn(text, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty()) {
				return offered(text, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
			}
		}
		Set<WorkModeEnum> listed = offered(value, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
		return (listed.size() < WorkModeEnum.values().length) ? listed : EnumSet.noneOf(WorkModeEnum.class);
	}

	/** The modes a sentence states for this role, empty where it states none. */
	private Set<WorkModeEnum> sentence(String text) {
		if (MODE_ROLE.matcher(text).find()) {
			return offered(sentenceFrom(text, 0), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
		}
		Matcher team = JOINS_A_TEAM.matcher(text);
		if (team.find()) {
			return offered(team.group(1), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
		}
		Matcher subject = THIS_ROLE.matcher(text);
		while (subject.find()) {
			Set<WorkModeEnum> stated = statement(text, subject);
			if (!stated.isEmpty()) {
				return stated;
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	/** The modes the sentence of a text whose subject this role is states, empty where it states none. */
	private Set<WorkModeEnum> statement(String text, MatchResult subject) {
		String opening = text.substring(0, subject.start());
		Matcher end = SENTENCE_END.matcher(opening);
		while (end.find()) {
			opening = opening.substring(end.end());
			end = SENTENCE_END.matcher(opening);
		}
		boolean framed = FRAME.matcher(opening).find()
				&& offered(subject.group(), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty();
		boolean aside = subject.group("team") != null && (!opening.isBlank()
				|| !offered(subject.group(), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty());
		if (framed || aside || List.of(words(opening).split(" ")).stream().anyMatch(CONDITIONS::contains)) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		String statement = sentenceFrom(text, subject.start());
		String spaced = spaced(statement, DESCRIPTION_SUBJECTS);
		int earliest = earliest(spaced, DESCRIPTION_WORDS);
		if (earliest == spaced.length()) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		String[] before = spaced.substring(0, earliest).strip().split(" ");
		if (before.length > STATEMENT_WORDS) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		for (String word : before) {
			if (NOT_STATED.contains(word)) {
				return EnumSet.noneOf(WorkModeEnum.class);
			}
		}
		for (String word : spaced.substring(earliest).strip().split(" ")) {
			if (NEGATIONS.contains(word)) {
				return EnumSet.of(first(spaced, DESCRIPTION_WORDS));
			}
		}
		return offered(statement, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
	}

	/** A text from where a sentence of it starts to where that sentence ends. */
	private static String sentenceFrom(String text, int start) {
		Matcher end = SENTENCE_END.matcher(text);
		return end.find(start) ? text.substring(start, end.start()) : text.substring(start);
	}

	/** A text's words, lowercased, one space apart and between spaces, with these subjects left out. */
	private String spaced(String text, Set<String> subjects) {
		String spaced = " " + words(text) + " ";
		for (String subject : subjects) {
			spaced = spaced.replace(" " + subject + " ", " ");
		}
		return spaced;
	}

	/** A text's words, lowercased and one space apart, as cleaning keeps them. */
	private String words(String text) {
		return this.cleaning.whitelisted(text).toLowerCase(Locale.ROOT);
	}

	private static Map<String, WorkModeEnum> described() {
		Map<String, WorkModeEnum> words = new HashMap<>(WORDS);
		words.putAll(Map.of("remotely", WorkModeEnum.REMOTE, "teleworker", WorkModeEnum.REMOTE, "telecommuting",
				WorkModeEnum.REMOTE, "from home", WorkModeEnum.REMOTE, "in the office", WorkModeEnum.ONSITE));
		return Map.copyOf(words);
	}

	private static Set<String> notStated() {
		Set<String> words = new HashSet<>(NEGATIONS);
		words.addAll(CONDITIONS);
		words.addAll(Set.of("may", "might", "could"));
		return Set.copyOf(words);
	}

	private static Set<String> describedSubjects() {
		Set<String> subjects = new HashSet<>(SUBJECTS);
		subjects.addAll(Set.of("remote team", "remote teams", "remote users", "remote end users", "remote support",
				"remote location", "remote locations", "remote site", "remote sites", "remote monitoring",
				"remote debugging", "hybrid search", "hybrid environment", "hybrid environments", "onsite support",
				"on site support", "support onsite", "support on site", "onsite parking", "on site parking", "onsite gym",
				"on site gym", "onsite cafeteria", "on site cafeteria", "onsite and remote", "on site and remote"));
		return Set.copyOf(subjects);
	}

}
