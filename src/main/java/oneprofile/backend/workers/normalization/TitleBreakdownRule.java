package oneprofile.backend.workers.normalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.ManagementLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import oneprofile.backend.storage.normalizedvacancy.TitleBreakdown;
import oneprofile.backend.storage.taxonomy.Skill;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import org.springframework.stereotype.Component;

/**
 * Breaks an engineering vacancy's title down into its normalized title, the levels its guarded
 * seniority words name, and its management level (ADR-0024, amended 2026-10-26). It reads only
 * the engineering subset, so its lists may assume the vocabulary of engineering work.
 * <p>
 * The title is read as cleaning reads it, but in parts: the commas, brackets and spaced dashes a
 * company writes between the role and its team, place or code are kept apart, because what a part
 * says depends on which side of the head it stands. Each part is cut into words and phrases, the
 * longest phrase winning, and its facts go: the guarded seniority words (recorded), work-mode words,
 * places, the skills the title names, requisition codes, cohort years, years of experience and
 * {@code founding}.
 * <p>
 * The function head is then read as the title classifier reads it (ADR-0009): a word naming a rank
 * or a stage yields to a head behind it. What becomes of the title depends on what the head is.
 * <ul>
 * <li>A function head ({@code engineer}, {@code product manager}, {@code member of technical staff})
 * keeps the words in front of it and drops what follows, unless what follows names the kind of
 * work: then it moves in front, in place of a bare {@code software} ({@code Software Engineer,
 * Backend} is a {@code backend engineer}). A leadership word in front is taken out as the
 * management level; a vice president or a managing director beside a function head is a corporate
 * grade and is only taken out.</li>
 * <li>A leadership word with no function head leads an area: the words around it, or the part after
 * it when it stands alone ({@code Senior Manager, Data Applications}), are what is left. A kind
 * of engineering reads as its engineer: {@code Head of Data Engineering} is a {@code data
 * engineer}, while a bare {@code Engineering Manager} keeps {@code engineering}.</li>
 * <li>Two function heads in one part give two normalized titles.</li>
 * </ul>
 */
@Component
public class TitleBreakdownRule {

	/** What a word or phrase of a title does in it, as the breakdown reads it. */
	private enum WordUse {

		/** Any other word, kept or dropped by where it stands. */
		WORD,

		/** A word naming the kind of work, which moves in front of the head from behind it. */
		KIND,

		/** A function head: what the role does. */
		HEAD,

		/** A department taken as the head when no function head is named: {@code engineering}. */
		AREA,

		/** A stage or a standing that yields to any head behind it: {@code intern}, {@code associate}. */
		WEAK,

		/** A leadership word, taken out as the management level. */
		RANK,

		/** A guarded seniority word, taken out and recorded. */
		LEVEL,

		/** A fact the title names that the normalized title leaves out. */
		NOISE

	}

	/** One word or phrase of a part, with what it is and, for a rank or a lead, the level it names. */
	private record Item(String text, WordUse use, ManagementLevelEnum rank, SeniorityLevelEnum level) {

		static Item of(String text, WordUse use) {
			return new Item(text, use, null, null);
		}

		boolean is(WordUse other) {
			return this.use == other;
		}

	}

	/** One reading of the vocabulary: what a phrase is, and what it is written as once read. */
	private record Reading(WordUse use, String canonical, ManagementLevelEnum rank, SeniorityLevelEnum level) {
	}

	/**
	 * Where a title is cut into parts: brackets, the punctuation that separates clauses, and a dash with
	 * a space on either side. A hyphen inside a word stays, and so does a slash, which joins two heads
	 * or two kinds of work rather than separating a team from the role.
	 */
	private static final Pattern PART_SEPARATOR = Pattern.compile("[,;|()\\[\\]{}:]|\\s[-–—]+\\s|^[-–—]|[-–—]$");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	/** A slash, or an ampersand with a space on either side; {@code R&D} is one word. */
	private static final Pattern JOINING = Pattern.compile("/|\\s&\\s");

	/** A slash or a spaced ampersand, kept as a word: what joins two heads. */
	private static final String JOIN = "/";

	/** The words that join two heads into two titles, where adjacent heads are one compound head. */
	private static final Set<String> JOINS = Set.of(JOIN, "and", "or", "&");

	private static final Pattern DIGIT = Pattern.compile("\\p{N}");

	private static final Pattern YEAR = Pattern.compile("(?:19|20)\\d\\d");

	/**
	 * The spellings that say one thing, written one way, and the abbreviations of a whole title. An
	 * abbreviation and its long form are written the way most of the subset's titles write them (#94).
	 */
	private static final Map<String, String> SPELLINGS = Map.ofEntries(Map.entry("fullstack", "full stack"),
			Map.entry("ml", "machine learning"), Map.entry("ml ops", "mlops"), Map.entry("sre", "site reliability"),
			Map.entry("bi", "business intelligence"), Map.entry("dba", "database administrator"),
			Map.entry("quality assurance", "qa"), Map.entry("generative ai", "genai"), Map.entry("gen ai", "genai"),
			Map.entry("artificial intelligence", "ai"), Map.entry("information technology", "it"),
			Map.entry("user experience", "ux"), Map.entry("user interface", "ui"),
			Map.entry("cyber security", "cybersecurity"), Map.entry("system", "systems"),
			Map.entry("pre sales", "presales"),
			Map.entry("back end", "backend"), Map.entry("front end", "frontend"), Map.entry("sw", "software"),
			Map.entry("swe", "software engineer"), Map.entry("sde", "software development engineer"),
			Map.entry("fde", "forward deployed engineer"), Map.entry("mle", "machine learning engineer"),
			Map.entry("a.i", "ai"), Map.entry("dev ops", "devops"), Map.entry("mgr", "manager"),
			Map.entry("engg", "engineer"), Map.entry("enginer", "engineer"), Map.entry("mts", "member of technical staff"),
			Map.entry("cto", "chief technology"), Map.entry("cio", "chief information"),
			Map.entry("ciso", "chief information security"), Map.entry("cdo", "chief data"),
			Map.entry("cpo", "chief product"), Map.entry("caio", "chief ai"));

	/** The abbreviations that name a whole title where they stand alone, and a kind of work beside a head. */
	private static final Map<String, String> WHOLE_TITLES = Map.of("sre", "site reliability engineer");

	/** A chief's office is a department, not a chief: {@code Chief Data Office} reads as a data office. */
	private static final Pattern CHIEF_OFFICE = Pattern.compile("\\bchief ((?:\\S+ ){1,3})office\\b");

	/** A chief officer is read as a chief over the area named between the two words. */
	private static final Pattern CHIEF_OFFICER = Pattern.compile("\\bchief ((?:\\S+ ){1,3})officer\\b");

	/** The heads that also name the lead's level: whether a tech lead manages anyone is not in the title. */
	private static final Set<String> LEADING_HEADS = Set.of("tech lead", "technical lead", "team lead", "team leader",
			"tech leader", "technical team lead");

	/**
	 * The function heads, each with the form it is written in once read: a plural reads as its
	 * singular. Several words are one head where the first alone would be cut short, and a manager
	 * that names a job rather than a rank is a head of its own (ADR-0024).
	 */
	private static final Map<String, String> HEADS = heads();

	private static final Set<String> AREAS = Set.of("engineering", "development");

	private static final Set<String> WEAK = Set.of("intern", "interns", "internship", "specialist",
			"specialists", "expert", "experts", "member", "fellow", "graduate", "trainee", "apprentice", "student",
			"werkstudent", "praktikant", "stagiaire", "support", "operations", "tech", "partner", "co op", "quant",
			"quants");

	/**
	 * The stages and standings that name a person rather than a department, and so are read as the head
	 * before {@code support} or {@code operations} is, and are what a corporate grade stands beside.
	 */
	private static final Set<String> PEOPLE = Set.of("intern", "interns", "internship", "specialist", "specialists",
			"expert", "experts", "member", "fellow", "graduate", "trainee", "apprentice", "student", "werkstudent",
			"praktikant", "stagiaire", "partner", "quant", "quants");

	/** The leadership words and the level each names. */
	private static final Map<String, ManagementLevelEnum> RANKS = Map.ofEntries(
			Map.entry("lead", ManagementLevelEnum.LEAD), Map.entry("leader", ManagementLevelEnum.LEAD),
			Map.entry("leaders", ManagementLevelEnum.LEAD), Map.entry("manager", ManagementLevelEnum.MANAGER),
			Map.entry("managers", ManagementLevelEnum.MANAGER), Map.entry("director", ManagementLevelEnum.DIRECTOR),
			Map.entry("directors", ManagementLevelEnum.DIRECTOR), Map.entry("head", ManagementLevelEnum.DIRECTOR),
			Map.entry("chief", ManagementLevelEnum.EXECUTIVE), Map.entry("president", ManagementLevelEnum.EXECUTIVE),
			Map.entry("vp", ManagementLevelEnum.EXECUTIVE), Map.entry("svp", ManagementLevelEnum.EXECUTIVE),
			Map.entry("evp", ManagementLevelEnum.EXECUTIVE), Map.entry("avp", ManagementLevelEnum.EXECUTIVE),
			Map.entry("rvp", ManagementLevelEnum.EXECUTIVE), Map.entry("vice president", ManagementLevelEnum.EXECUTIVE),
			Map.entry("assistant vice president", ManagementLevelEnum.EXECUTIVE),
			Map.entry("executive vice president", ManagementLevelEnum.EXECUTIVE),
			Map.entry("managing director", ManagementLevelEnum.EXECUTIVE),
			Map.entry("executive director", ManagementLevelEnum.EXECUTIVE));

	/**
	 * The ranks a bank gives as a corporate grade. Beside a function head they name where the post
	 * sits in the bank, not someone leading an area: {@code VP System Analyst} is an analyst.
	 * {@code associate} is only ever a grade, and names no management level even on its own.
	 */
	private static final Set<String> GRADES = Set.of("vp", "svp", "evp", "avp", "rvp", "vice president",
			"assistant vice president", "executive vice president", "managing director", "executive director",
			"associate");

	private static final Set<String> RANK_QUALIFIERS = Set.of("group", "assistant", "deputy", "associate");

	/** The guarded seniority words and the levels they name. {@code chief of staff} is a head, so it is read first. */
	private static final Map<String, SeniorityLevelEnum> LEVELS = Map.ofEntries(
			Map.entry("staff", SeniorityLevelEnum.STAFF), Map.entry("staff+", SeniorityLevelEnum.STAFF),
			Map.entry("mid", SeniorityLevelEnum.MID), Map.entry("mid level", SeniorityLevelEnum.MID),
			Map.entry("midlevel", SeniorityLevelEnum.MID), Map.entry("intermediate", SeniorityLevelEnum.MID),
			Map.entry("medior", SeniorityLevelEnum.MID), Map.entry("middle", SeniorityLevelEnum.MID),
			Map.entry("entry", SeniorityLevelEnum.ENTRY),
			Map.entry("entry level", SeniorityLevelEnum.ENTRY), Map.entry("ii", SeniorityLevelEnum.II),
			Map.entry("iii", SeniorityLevelEnum.III), Map.entry("iv", SeniorityLevelEnum.IV),
			Map.entry("level ii", SeniorityLevelEnum.II), Map.entry("level iii", SeniorityLevelEnum.III),
			Map.entry("level iv", SeniorityLevelEnum.IV));

	/**
	 * The words that name the kind of work, read off the words the subset puts in front of a head and
	 * also after one. A skill that names one ({@code Android}) stays as the kind of work rather than
	 * going as a skill.
	 */
	private static final Set<String> KINDS = Set.of("software", "software development", "backend", "frontend",
			"full stack", "data", "data platform", "data infrastructure", "ai", "ai machine learning", "machine learning",
			"deep learning", "applied ai", "genai", "agentic ai", "llm", "nlp", "computer vision",
			"mlops", "platform", "infrastructure", "infra", "cloud", "cloud infrastructure", "devops", "devsecops",
			"site reliability", "reliability", "security", "application security", "product security",
			"cloud security", "network security", "cybersecurity", "offensive security", "detection",
			"identity", "iam", "network", "networking", "mobile", "android", "ios", "react native", "flutter", "web",
			"embedded", "embedded software", "embedded systems", "firmware", "systems", "distributed systems",
			"database", "test", "test automation", "qa", "qa automation", "automation",
			"verification", "design verification", "validation", "robotics", "perception", "autonomy", "simulation",
			"compiler", "gpu", "graphics", "game", "gameplay", "observability", "performance", "storage", "build",
			"release", "tooling", "developer experience", "developer productivity", "developer tools",
			"developer platform", "developer relations", "integration", "integrations", "analytics", "research",
			"salesforce", "servicenow", "sap", "workday", "blockchain", "quant", "quantitative", "inference", "it",
			"ux", "ui", "forward deployed", "product");

	/**
	 * The fronts the subset writes in more than one order for the same work, each in the order most titles
	 * use (#94). A front whose order changes the work is not here: an {@code application security
	 * engineer} secures applications, while a {@code security application engineer} builds them for
	 * security.
	 */
	private static final Map<String, String> ORDERS = Stream
		.of("ai machine learning", "ai data", "qa automation", "software test", "ai software", "cloud infrastructure",
				"test automation", "agentic ai", "forward deployed ai", "platform software", "full stack ai", "software qa",
				"platform infrastructure", "cloud devops", "devops platform", "application software", "applied ai",
				"software infrastructure", "manufacturing automation", "r&d software", "network systems",
				"software development engineering", "mission software", "software development test",
				"devops software", "quality automation", "data software", "process automation", "database platform",
				"enterprise ai", "devops infrastructure", "applied ai software", "platform reliability",
				"infrastructure systems", "backend ai", "embedded software", "robotics automation",
				"site reliability devops", "business intelligence data", "full stack web", "ai engineering")
		.collect(Collectors.toUnmodifiableMap(TitleBreakdownRule::orderKey, (front) -> front));

	/** The kinds a more particular kind from behind the head replaces. */
	private static final Set<String> GENERIC_KINDS = Set.of("software", "software development");

	/**
	 * The kinds that are only ever software work, so that a {@code software} beside them adds nothing:
	 * a {@code Backend Software Engineer} is a backend engineer, as a {@code Software Engineer, Backend} is.
	 */
	private static final Set<String> SOFTWARE_WORK = Set.of("backend", "frontend", "full stack", "web", "mobile",
			"android", "ios", "react native", "flutter", "ui");

	/** The places a title names that its location field may not: regions, countries and the commonest cities. */
	private static final Set<String> PLACES = Set.of("us", "usa", "u.s", "united states", "america", "americas",
			"north america", "latin america", "latam", "south america", "central america", "emea", "apac", "amer",
			"amers", "apj", "japac", "anz", "mena", "dach", "nordics", "nordic", "benelux", "europe", "eu", "uk",
			"united kingdom", "ireland", "germany", "france", "spain", "portugal", "italy", "netherlands", "poland",
			"romania", "ukraine", "india", "ind", "china", "japan", "korea", "south korea", "singapore", "australia",
			"new zealand", "canada", "mexico", "brazil", "brasil", "argentina", "colombia", "chile", "peru",
			"philippines", "vietnam", "indonesia", "malaysia", "thailand", "israel", "south africa", "nigeria",
			"kenya", "egypt", "uae", "saudi arabia", "ksa", "turkey", "switzerland", "austria", "belgium", "sweden",
			"norway", "denmark", "finland", "czech republic", "czechia", "hungary", "greece", "costa rica",
			"puerto rico", "hong kong", "taiwan", "pakistan", "bangladesh", "sri lanka", "london", "new york", "nyc",
			"san francisco", "sf", "bay area", "seattle", "austin", "boston", "chicago", "denver", "atlanta",
			"toronto", "vancouver", "montreal", "berlin", "munich", "paris", "amsterdam", "dublin", "madrid",
			"barcelona", "lisbon", "warsaw", "krakow", "bangalore", "bengaluru", "hyderabad", "pune", "chennai",
			"mumbai", "delhi", "gurgaon", "gurugram", "noida", "tokyo", "seoul", "sydney", "melbourne", "tel aviv",
			"são paulo", "sao paulo", "mexico city", "buenos aires", "bogota", "bogotá", "manila", "kuala lumpur",
			"dubai", "zurich", "stockholm", "copenhagen", "oslo", "helsinki", "prague", "budapest", "bucharest",
			"sofia", "belgrade", "kyiv", "washington dc", "washington");

	/** The shift and the hours a post is worked in, which say nothing of the work (#92). */
	private static final Set<String> SHIFTS = Set.of("shift", "night shift", "day shift", "evening shift",
			"weekend shift", "hours", "time zone");

	/** Words a startup puts in a title that say nothing of the work (#92). */
	private static final Set<String> DROPPED = Set.of("founding", "i");

	/** The words with a digit that name the work rather than a code or a year. */
	private static final Set<String> NAMED_WITH_DIGITS = Set.of("3d", "2d", "5g", "4g", "web3", "b2b", "b2c", "m365",
			"o365");

	private static final Set<String> CODE_WORDS = Set.of("job", "req", "requisition", "ref", "id");

	private static final Set<String> SEASONS = Set.of("summer", "fall", "autumn", "winter", "spring");

	private static final Set<String> YEARS_WORDS = Set.of("years", "year", "yrs", "yr", "experience");

	/** The words that join the words of an area or a front, and so are not left at either end of one. */
	private static final Set<String> CONNECTORS = Set.of(JOIN, "of", "the", "for", "and", "&", "in", "de", "del", "da",
			"do", "at", "to", "with", "on", "or", "a", "an", "y", "e", "und", "et");

	/** How a fact the normalized title leaves out is read. */
	private static final Reading NOISE = new Reading(WordUse.NOISE, "", null, null);

	private static final Map<String, Reading> VOCABULARY = vocabulary();

	private static final int LONGEST = VOCABULARY.keySet().stream().mapToInt(TitleBreakdownRule::length).max().orElse(1);

	private final TitleCleaningRule cleaning;

	private final Map<String, Set<String>> skillKeys = new HashMap<>();

	/**
	 * @param cleaning the rule whose reading of a title each part gets
	 * @param taxonomy the skills a title can name, with their keys
	 */
	public TitleBreakdownRule(TitleCleaningRule cleaning, TaxonomyStore taxonomy) {
		this.cleaning = cleaning;
		for (Skill skill : taxonomy.skills()) {
			Set<String> keys = new HashSet<>();
			keys.add(nameForm(skill.canonicalName()));
			skill.aliases().forEach((alias) -> keys.add(nameForm(alias)));
			// A title's words are respelled before its skills are found in them, so the keys are too.
			Set.copyOf(keys).forEach((key) -> keys.add(String.join(" ", respelled(List.of(WHITESPACE.split(key))))));
			this.skillKeys.put(skill.id(), keys);
		}
	}

	/**
	 * Breaks one title down.
	 * @param title the title as the company wrote it
	 * @param location the vacancy's location field, whose places the title may repeat; empty if it has none
	 * @param titleSkillIds the skills extraction found the title to name
	 * @return its normalized titles, the levels its guarded seniority words name and its management level
	 */
	public TitleBreakdown breakDown(String title, String location, Set<String> titleSkillIds) {
		Map<String, Reading> named = namedHere(location, titleSkillIds);
		Set<SeniorityLevelEnum> levels = new LinkedHashSet<>();
		List<List<Item>> parts = new ArrayList<>();
		for (String part : PART_SEPARATOR.split(this.cleaning.withoutGenderMarkers(title))) {
			List<Item> items = withoutFacts(items(words(part), named), levels);
			if (!items.isEmpty()) {
				parts.add(items);
			}
		}
		Head head = head(parts);
		return new TitleBreakdown(normalizedTitles(parts, head), levels, managementLevel(parts));
	}

	/** Where the head was read: its part and its place there, and what it is. */
	private record Head(int part, int at, WordUse use) {
	}

	/**
	 * The head the title is read by: the first function head; else the first leadership word, which
	 * then leads an area; else the first department or stage. Null if the title names none of them.
	 */
	private static Head head(List<List<Item>> parts) {
		Head function = first(parts, (item) -> item.is(WordUse.HEAD));
		boolean graded = gradeYields(parts);
		Head leader = first(parts, (item) -> item.is(WordUse.RANK) && !(graded && GRADES.contains(item.text())));
		if (function != null) {
			// A leader standing in an earlier part leads the area named between it and the head, as in
			// "Manager, Software Engineering - Developer Support"; with nothing between, the head is read.
			boolean areaBetween = leader != null && leader.part() < function.part()
					&& (!front(parts.get(leader.part())).isEmpty() || leader.part() + 1 < function.part());
			return areaBetween ? leader : function;
		}
		if (leader != null) {
			return leader;
		}
		Head area = first(parts, (item) -> item.is(WordUse.AREA));
		if (area != null) {
			return area;
		}
		Head person = first(parts, (item) -> item.is(WordUse.WEAK) && PEOPLE.contains(item.text()));
		return (person != null) ? person : first(parts, (item) -> item.is(WordUse.WEAK));
	}

	/** Where the first word or phrase that passes a test stands, null if none does. */
	private static Head first(List<List<Item>> parts, java.util.function.Predicate<Item> test) {
		for (int part = 0; part < parts.size(); part++) {
			List<Item> items = parts.get(part);
			for (int at = 0; at < items.size(); at++) {
				if (test.test(items.get(at))) {
					return new Head(part, at, items.get(at).use());
				}
			}
		}
		return null;
	}

	/**
	 * Whether a corporate grade in the title is only a grade: where the title names a function head,
	 * or a stage or standing that names a person ({@code specialist}, {@code quant}), the grade says
	 * where the post sits in the bank, and leads nothing.
	 */
	private static boolean gradeYields(List<List<Item>> parts) {
		return first(parts, (item) -> item.is(WordUse.HEAD) || (item.is(WordUse.WEAK) && PEOPLE.contains(item.text())))
				!= null;
	}

	private static List<String> normalizedTitles(List<List<Item>> parts, Head head) {
		if (parts.isEmpty()) {
			return List.of();
		}
		if (head == null) {
			return nonEmpty(text(trimmed(parts.getFirst())));
		}
		if (head.use() == WordUse.RANK) {
			return area(parts, head);
		}
		List<Item> items = parts.get(head.part());
		List<Item> front = front(items.subList(0, head.at()));
		int second = -1;
		boolean joined = false;
		for (int at = head.at() + 1; at < items.size() && head.use() == WordUse.HEAD; at++) {
			joined |= JOINS.contains(items.get(at).text());
			if (items.get(at).is(WordUse.HEAD)) {
				second = joined ? at : -1;
				break;
			}
		}
		if (second < 0) {
			List<Item> after = after(parts, head.part(), head.at());
			return nonEmpty(written(withKindsFrom(after, front), items.get(head.at()).text()));
		}
		List<Item> secondFront = front(items.subList(head.at() + 1, second));
		if (secondFront.isEmpty()) {
			secondFront = front;
		}
		List<Item> after = after(parts, head.part(), second);
		List<String> titles = new ArrayList<>();
		titles.addAll(nonEmpty(written(front, items.get(head.at()).text())));
		titles.addAll(nonEmpty(written(withKindsFrom(after, secondFront), items.get(second).text())));
		return titles.stream().distinct().toList();
	}

	/** What stands behind a head: the rest of its part, and the part after it. */
	private static List<Item> after(List<List<Item>> parts, int part, int head) {
		List<Item> after = new ArrayList<>(parts.get(part).subList(head + 1, parts.get(part).size()));
		if (part + 1 < parts.size()) {
			after.addAll(parts.get(part + 1));
		}
		return after;
	}

	/**
	 * The area a leadership word leads: the words around it, or the next part where it stands alone,
	 * with a kind of work from the part after moved in front.
	 */
	private static List<String> area(List<List<Item>> parts, Head head) {
		List<Item> area = front(parts.get(head.part()));
		int next = head.part() + 1;
		if (area.isEmpty() && next < parts.size()) {
			area = front(parts.get(next));
			next++;
		}
		else if (area.isEmpty()) {
			// A rank at the end of the title, as in "Digital Transformation - Senior Manager", leads what
			// the title began with.
			area = front(parts.getFirst());
		}
		if (area.isEmpty()) {
			return List.of();
		}
		List<Item> after = (next < parts.size()) ? parts.get(next) : List.of();
		List<Item> led = new ArrayList<>(withKindsFrom(after, area));
		// Whoever leads a kind of engineering leads engineers of that kind, and that is the work the
		// area names: a Head of Data Engineering reads as a data engineer. A bare engineering is left
		// as it is, because the engineer it would read as names no kind at all; which role it is, is
		// the role list's to say (#25).
		if (led.size() > 1 && led.getLast().text().equals("engineering")) {
			led.set(led.size() - 1, Item.of("engineer", WordUse.HEAD));
		}
		return nonEmpty(written(led, ""));
	}

	/**
	 * A head and the words in front of it, written one way: no word the head names or the word before
	 * it repeats, no {@code software} beside a kind that is only software work, and the same kinds of
	 * work in the order most titles write them.
	 */
	private static String written(List<Item> front, String head) {
		Set<String> named = Set.copyOf(List.of(WHITESPACE.split(head)));
		boolean softwareWork = front.stream().anyMatch((item) -> SOFTWARE_WORK.contains(item.text()));
		List<Item> kept = front.stream()
			.filter((item) -> !item.text().equals(JOIN) && !(softwareWork && GENERIC_KINDS.contains(item.text())))
			.toList();
		List<String> words = new ArrayList<>();
		for (Item item : trimmed(kept)) {
			for (String word : WHITESPACE.split(item.text())) {
				if (!named.contains(word) && (words.isEmpty() || !words.getLast().equals(word))) {
					words.add(word);
				}
			}
		}
		String written = String.join(" ", words);
		written = ORDERS.getOrDefault(orderKey(written), written);
		return written + " " + head;
	}

	/** What two fronts written in another order share: their words, in alphabetical order. */
	private static String orderKey(String front) {
		return Stream.of(WHITESPACE.split(front)).sorted().collect(Collectors.joining(" "));
	}

	/** The words kept in front of a head: all of them but the leadership words, and no connector at either end. */
	private static List<Item> front(List<Item> items) {
		return trimmed(items.stream().filter((item) -> !item.is(WordUse.RANK)).toList());
	}

	/**
	 * A front with the kinds of work named behind its head moved in front of it, where the front names
	 * no kind of its own but a bare {@code software}, which the more particular kind replaces.
	 */
	private static List<Item> withKindsFrom(List<Item> after, List<Item> front) {
		// Only the first run of kinds: a word that is not one ends it, as "Core" does in "Team Lead,
		// Android Core Product", and so does a slash or an ampersand, which joins two kinds of which
		// the first is kept ("AI & Data"): carrying both would split titles that are one kind of work.
		List<Item> kinds = after.stream()
			.dropWhile((item) -> !item.is(WordUse.KIND))
			.takeWhile((item) -> item.is(WordUse.KIND))
			.toList();
		boolean named = front.stream()
			.anyMatch((item) -> item.is(WordUse.KIND) && !GENERIC_KINDS.contains(item.text()));
		if (kinds.isEmpty() || named) {
			return front;
		}
		List<Item> moved = new ArrayList<>(kinds);
		front.stream().filter((item) -> !GENERIC_KINDS.contains(item.text())).forEach(moved::add);
		return moved;
	}

	/**
	 * The highest rank the title names. A corporate grade beside a head that names a person names
	 * none; a tech or team lead names a lead.
	 */
	private static ManagementLevelEnum managementLevel(List<List<Item>> parts) {
		boolean graded = gradeYields(parts);
		ManagementLevelEnum highest = null;
		for (List<Item> items : parts) {
			for (Item item : items) {
				if (item.rank() == null || (graded && GRADES.contains(item.text()))) {
					continue;
				}
				if (highest == null || item.rank().compareTo(highest) > 0) {
					highest = item.rank();
				}
			}
		}
		return highest;
	}

	/**
	 * A part cut into the longest phrases the vocabulary and this vacancy's skills and places hold,
	 * each read for what it is, and every other word as a word.
	 */
	private static List<Item> items(List<String> words, Map<String, Reading> named) {
		List<Item> items = new ArrayList<>();
		int longest = Math.max(LONGEST, named.keySet().stream().mapToInt(TitleBreakdownRule::length).max().orElse(1));
		int at = 0;
		while (at < words.size()) {
			int taken = 1;
			Item item = Item.of(words.get(at), WordUse.WORD);
			// A phrase may span a slash, as "AI/ML" does, but never start or end on one.
			for (int length = Math.min(longest, words.size() - at); length >= 1 && !words.get(at).equals(JOIN);
					length--) {
				List<String> window = words.subList(at, at + length);
				if (window.getLast().equals(JOIN)) {
					continue;
				}
				String phrase = window.stream().filter((word) -> !word.equals(JOIN)).collect(Collectors.joining(" "));
				Reading reading = VOCABULARY.get(phrase);
				if (reading == null) {
					reading = named.get(phrase);
				}
				if (reading != null) {
					item = new Item(reading.canonical(), reading.use(), reading.rank(), reading.level());
					taken = length;
					break;
				}
			}
			items.add(item);
			at += taken;
		}
		return items;
	}

	/**
	 * A part with its facts taken out: its guarded levels, recorded as they go, and its codes, years and
	 * the other noise. No connector is left at either end of what remains.
	 */
	private static List<Item> withoutFacts(List<Item> items, Set<SeniorityLevelEnum> levels) {
		List<Item> kept = new ArrayList<>();
		for (int at = 0; at < items.size(); at++) {
			Item item = items.get(at);
			if (item.is(WordUse.LEVEL)) {
				levels.add(item.level());
				continue;
			}
			if (item.text().equals("at") && !kept.isEmpty()) {
				// What a title names after "at" is the company or the team it is at.
				break;
			}
			if (item.is(WordUse.NOISE) || (item.is(WordUse.WORD) && isNoise(items, at))) {
				continue;
			}
			kept.add(item);
		}
		return trimmed(kept);
	}

	/**
	 * Whether a word says nothing of the work: {@code founding}, a lone {@code I}, or a requisition code,
	 * a cohort year or a number of years, or the word that introduces one: {@code R01572426}, {@code Job 31906}, {@code Summer 2027}, {@code 10+ years}.
	 */
	private static boolean isNoise(List<Item> items, int at) {
		String word = items.get(at).text();
		if (DROPPED.contains(word)) {
			return true;
		}
		if (DIGIT.matcher(word).find()) {
			return !NAMED_WITH_DIGITS.contains(word);
		}
		boolean beforeNumber = at + 1 < items.size() && DIGIT.matcher(items.get(at + 1).text()).find();
		boolean afterNumber = at > 0 && DIGIT.matcher(items.get(at - 1).text()).find();
		if (CODE_WORDS.contains(word) && beforeNumber) {
			return true;
		}
		if (SEASONS.contains(word) && ((beforeNumber && YEAR.matcher(items.get(at + 1).text()).matches())
				|| (afterNumber && YEAR.matcher(items.get(at - 1).text()).matches()))) {
			return true;
		}
		if (YEARS_WORDS.contains(word)) {
			return afterNumber || (at > 1 && YEARS_WORDS.contains(items.get(at - 1).text())
					&& DIGIT.matcher(items.get(at - 2).text()).find());
		}
		return false;
	}

	/** The words of one part as cleaning leaves them, lowercased and spelled one way. */
	private List<String> words(String part) {
		// Cleaning takes a slash and a spaced ampersand out, and they are what tells two heads from one
		// compound head, so either is kept as a word of its own.
		List<String> words = new ArrayList<>();
		for (String piece : JOINING.split(part)) {
			String cleaned = this.cleaning.clean(piece).title().toLowerCase(Locale.ROOT);
			if (!cleaned.isEmpty()) {
				if (!words.isEmpty()) {
					words.add(JOIN);
				}
				words.addAll(List.of(WHITESPACE.split(cleaned)));
			}
		}
		if (words.isEmpty()) {
			return List.of();
		}
		String spelled = (words.size() == 1 && WHOLE_TITLES.containsKey(words.getFirst()))
				? WHOLE_TITLES.get(words.getFirst()) : String.join(" ", respelled(words));
		spelled = CHIEF_OFFICER.matcher(spelled).replaceAll("chief $1").trim();
		spelled = CHIEF_OFFICE.matcher(spelled).replaceAll("$1office");
		return List.of(WHITESPACE.split(spelled));
	}

	private static List<String> respelled(List<String> words) {
		List<String> spelled = new ArrayList<>();
		int at = 0;
		while (at < words.size()) {
			String pair = (at + 1 < words.size()) ? words.get(at) + " " + words.get(at + 1) : null;
			if (pair != null && SPELLINGS.containsKey(pair)) {
				spelled.add(SPELLINGS.get(pair));
				at += 2;
			}
			else {
				spelled.add(SPELLINGS.getOrDefault(words.get(at), words.get(at)));
				at++;
			}
		}
		return spelled;
	}

	/**
	 * What this vacancy's title may name beyond the vocabulary: the places of its location field, and
	 * the keys of the skills extraction found in it that do not name the kind of work.
	 */
	private Map<String, Reading> namedHere(String location, Set<String> titleSkillIds) {
		Map<String, Reading> named = new HashMap<>();
		for (String id : titleSkillIds) {
			this.skillKeys.getOrDefault(id, Set.of())
				.stream()
				.filter((key) -> !KINDS.contains(key))
				.forEach((key) -> named.put(key, NOISE));
		}
		for (String place : PART_SEPARATOR.split(location)) {
			String phrase = this.cleaning.whitelisted(place).toLowerCase(Locale.ROOT);
			if (!phrase.isEmpty() && Stream.of(WHITESPACE.split(phrase)).noneMatch(TitleBreakdownRule::namesWork)) {
				named.put(phrase, NOISE);
			}
		}
		return named;
	}

	/** Whether a word of a location names work rather than a place, and so is never taken out of a title as one. */
	private static boolean namesWork(String word) {
		Reading reading = VOCABULARY.get(word);
		return reading != null && reading.use() != WordUse.NOISE;
	}

	private static List<Item> trimmed(List<Item> items) {
		int from = 0;
		int to = items.size();
		while (from < to && CONNECTORS.contains(items.get(from).text())) {
			from++;
		}
		while (to > from && CONNECTORS.contains(items.get(to - 1).text())) {
			to--;
		}
		return items.subList(from, to);
	}

	private static String text(List<Item> items) {
		return items.stream().map(Item::text).filter((text) -> !text.equals(JOIN)).collect(Collectors.joining(" "));
	}

	private static List<String> nonEmpty(String title) {
		String trimmed = WHITESPACE.matcher(title).replaceAll(" ").trim();
		return trimmed.isEmpty() ? List.of() : List.of(trimmed);
	}

	/** A name as a title's words would spell it: lowercased, its separators one space. */
	private static String nameForm(String name) {
		return WHITESPACE.matcher(name.toLowerCase(Locale.ROOT).replaceAll("[-_/]", " ")).replaceAll(" ").trim();
	}

	private static int length(String phrase) {
		return WHITESPACE.split(phrase).length;
	}

	private static Map<String, String> heads() {
		Map<String, String> heads = new HashMap<>();
		for (String head : List.of("engineer", "developer", "programmer", "architect", "scientist", "analyst",
				"administrator", "researcher", "designer", "tester", "consultant", "technician", "technologist",
				"officer", "advocate", "evangelist", "writer", "coach")) {
			heads.put(head, head);
			heads.put(head + "s", head);
		}
		for (String manager : List.of("product", "program", "programme", "project", "account", "success",
				"customer success", "delivery", "release", "marketing", "product marketing", "relations", "partner",
				"community", "category", "incident", "change", "engagement")) {
			heads.put(manager + " manager", manager + " manager");
			heads.put(manager + " managers", manager + " manager");
		}
		for (String head : List.of("sdet", "product owner", "scrum master", "chief of staff",
				"member of technical staff", "developer advocate", "ingeniero", "ingeniera", "engenheiro",
				"engenheira", "desarrollador", "desarrolladora", "desenvolvedor", "desenvolvedora", "programador",
				"programadora", "analista", "arquitecto", "arquiteto", "consultor", "consultora", "cientista",
				"científico", "ingenieur", "ingénieur", "développeur", "développeuse", "entwickler", "informatiker",
				"エンジニア", "개발자", "엔지니어")) {
			heads.put(head, head);
		}
		heads.put("member of the technical staff", "member of technical staff");
		heads.put("member technical staff", "member of technical staff");
		LEADING_HEADS.forEach((head) -> heads.put(head, head));
		return Map.copyOf(heads);
	}

	private static Map<String, Reading> vocabulary() {
		Map<String, Reading> vocabulary = new HashMap<>();
		PLACES.forEach((place) -> vocabulary.put(place, NOISE));
		WorkModeRule.WORDS.keySet().forEach((mode) -> vocabulary.put(mode, NOISE));
		SHIFTS.forEach((shift) -> vocabulary.put(shift, NOISE));
		// "middle" is mid, except in the region and the bank's middle office.
		vocabulary.put("middle east", NOISE);
		vocabulary.put("middle office", new Reading(WordUse.WORD, "middle office", null, null));
		LEVELS.forEach((word, level) -> vocabulary.put(word, new Reading(WordUse.LEVEL, word, null, level)));
		KINDS.forEach((kind) -> vocabulary.put(kind, new Reading(WordUse.KIND, kind, null, null)));
		// A plural stage or standing reads as its singular, as a plural head does.
		WEAK.forEach((word) -> vocabulary.put(word, new Reading(WordUse.WEAK,
				(word.endsWith("s") && WEAK.contains(word.substring(0, word.length() - 1)))
						? word.substring(0, word.length() - 1) : word,
				null, null)));
		AREAS.forEach((word) -> vocabulary.put(word, new Reading(WordUse.AREA, word, null, null)));
		RANKS.forEach((word, rank) -> vocabulary.put(word, new Reading(WordUse.RANK, word, rank, null)));
		// A rank qualified as a group's, an assistant's or a deputy's is the same rank, not an area.
		RANKS.forEach((word, rank) -> RANK_QUALIFIERS.forEach((qualifier) -> vocabulary.put(qualifier + " " + word,
				new Reading(WordUse.RANK, word, rank, null))));
		vocabulary.put("associate", new Reading(WordUse.RANK, "associate", null, null));
		vocabulary.put("associates", new Reading(WordUse.RANK, "associate", null, null));
		HEADS.forEach((word, head) -> vocabulary.put(word, new Reading(WordUse.HEAD, head,
				LEADING_HEADS.contains(head) ? ManagementLevelEnum.LEAD : null, null)));
		return Map.copyOf(vocabulary);
	}

}
