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
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
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
 * vocabulary does not hold is no answer. A location that opens with its mode, as Workday writes one, is a
 * label of it: {@code Home - UK- England} is remote, {@code Office - United Kingdom - Edinburgh} onsite, and so is a
 * location named virtual ({@code US Virtual Location}), remote.
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
 * all home based}, {@code This role is based in our Edinburgh office}), with the mode within its first words and
 * no negation, condition or {@code may} before it, or that names this role or its team by a mode ({@code a fully
 * remote software team}, {@code looking for a remote Back End Engineer}); then a short segment offering a choice
 * of modes ({@code United States (Hybrid or Remote)}, {@code Open to remote locations}); then office days short of
 * the week, whoever their subject, unhedged, as a floor for every role covers this one ({@code at least 3 days in
 * office}); then onsite stated as what the job requires, every working day or primarily, whoever its subject
 * ({@code required to work on-site}, {@code 5 days/week in-office}, {@code Majority of tasking is performed on-site});
 * then a label of the work mode with its value ({@code Workplace type: Remote}, {@code Location: London (Hybrid)},
 * {@code London / Hybrid}, {@code Telecommuting Options} followed by {@code Onsite}, a mode under a {@code Job Details}
 * or {@code Working Conditions} heading, or a heading naming only the mode, or the mode with its schedule or place);
 * then the office the work is in, naming no mode, as onsite ({@code work out of our Chicago office}); then a LinkedIn
 * tag ({@code #LI-Hybrid}); then a company with no office, which is remote; then a job whose work is travelling to
 * sites, which is onsite. Modes listed under a heading of what the company offers are a perk, never a choice. A list
 * of modes alone ({@code Hybrid / Onsite / Remote}) is read by the schedule after it, and a list of
 * every mode with none after it declares nothing. Where the statements of the strongest kind found disagree, it
 * gives none.
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

	/**
	 * A location that opens with its mode, as Workday writes one: {@code Home - UK- England}, {@code Working at Home -
	 * Pennsylvania}, {@code Office - …}.
	 */
	private static final Pattern LOCATION_LABEL = Pattern.compile("^\\W*(?:(?<home>(?:work(?:ing)?\\s+(?:at|from)\\s+)?home)"
			+ "|office)\\s*-", Pattern.CASE_INSENSITIVE);

	/** A location named virtual, which is remote: {@code US Virtual Location}. */
	private static final Pattern VIRTUAL_LOCATION = Pattern.compile("\\bvirtual\\s+location\\b", Pattern.CASE_INSENSITIVE);

	/** Up to four days a week, as an option qualifies its mode by them: {@code 3 days a week}, {@code 2X Week}. */
	private static final Pattern DAYS_A_WEEK = Pattern.compile(
			"\\b(?:[1-4]|one|two|three|four)\\s*\\+?\\s*(?:x|days?|times)\\b[^.;]{0,30}?\\bweeks?\\b",
			Pattern.CASE_INSENSITIVE);

	/** Up to four days in the office, with no week named: {@code 4 days in office}, {@code 3 days onsite}. */
	private static final Pattern OFFICE_DAYS = Pattern.compile("\\b(?:[1-4]|one|two|three|four)\\s+days?\\s+(?:(?:in|at)\\s+"
			+ "(?:the\\s+|our\\s+|an?\\s+)?office|on[- ]?site|in[- ]person)\\b", Pattern.CASE_INSENSITIVE);

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
	 * {@code not remote}, {@code not a remote role}.
	 */
	private static final Pattern RULED_OUT = Pattern.compile("\\b(?:no|not|non|never|without)[\\s-]+(?:an?\\s+)?"
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
			+ "environment|setting|flexibility|options?|pattern|schedule|conditions)|workstyle|"
			+ "(?:job\\s+)?location(?:\\s+(?:type|details))?|remote\\s+type|employment\\s+type|"
			+ "telecommut\\w*\\s+options?|position\\s+(?:role\\s+)?type|type\\s+of\\s+work(?:\\s+model)?|"
			+ "worksite\\s+flexibility|flexible\\s+work(?:ing)?\\s+arrangements?|remote\\s+option\\s+availability)";

	/** What a label's value starts with where it names a mode. */
	private static final String MODE_FIRST = "(?=(?:full[- ]time\\s*[/,|]\\s*)?(?:fully\\s+|100\\s*%\\s*)?(?:remote|hybrid|"
			+ "on[- ]?site|in[- ]office|office[- ]based|work(?:ing)?\\s+from\\s+home|telecommut\\w*|telework\\w*|wfh)\\b"
			+ "|office\\W*$)";

	/** A label with its value in one segment: {@code Workplace type: Remote}. */
	private static final Pattern LABELLED = Pattern.compile("^\\W*" + LABEL + "(?:\\s*/\\s*" + LABEL + ")?\\s*[:\\-–—]\\s*" + MODE_FIRST + "(.+)$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * A label whose short value names its mode after a place, up to the next label on its line: {@code Location: London
	 * (Hybrid) | Practice Area: …}, {@code Logistics / Location: Dayton, OH, In Office}.
	 */
	private static final Pattern PLACED_LABEL = Pattern.compile("^\\W*(?:\\p{L}+\\s*/\\s*)?" + LABEL + "(?:\\s*/\\s*" + LABEL
			+ ")?\\s*[:\\-–—]\\s*([^|]+)", Pattern.CASE_INSENSITIVE);

	/** The most words a label's value naming a place before its mode holds. */
	private static final int PLACED_VALUE_WORDS = 8;

	/**
	 * A short segment that is a place or a label, then its mode: {@code London / Hybrid}, {@code Job Location (Hybrid)},
	 * {@code London, United Kingdom - Hybrid}.
	 */
	private static final Pattern PLACE_MODE = Pattern.compile("^\\W*(?<place>(?:[\\p{L}.'’-]+,?\\s+){0,4}?[\\p{L}.'’-]+)"
			+ "\\s*(?:[/(|]|\\s[-–—])\\s*(?:fully\\s+|100\\s*%\\s*)?(?<mode>remote|hybrid|on[- ]?site|in[- ]office)\\s*\\)?\\W*$",
			Pattern.CASE_INSENSITIVE);

	/** What a mode's value is where it answers whether the mode applies, not where: {@code Hybrid: No}. */
	private static final Set<String> NOT_A_PLACE = Set.of("no", "not", "yes", "n", "a", "required", "optional", "available",
			"none", "possible", "considered");

	/** A short segment that is a mode, then its place: {@code Onsite: Eglin AFB}. */
	private static final Pattern MODE_PLACE = Pattern.compile("^\\W*(?:fully\\s+|100\\s*%\\s*)?(?<mode>remote|hybrid|"
			+ "on[- ]?site)\\s*:\\s*(?<place>(?:[\\p{L}.'’-]+,?\\s*){1,4})$", Pattern.CASE_INSENSITIVE);

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
			+ "job|vacancy|requisition)\\b|\\bthis\\s+(?:[\\p{L}-]+\\s+){0,2}?opportunity\\b|\\bthis\\s+is\\s+(?:\\p{L}+\\s+)?an?\\b"
			+ "|(?<team>\\b(?:this|the|our)\\s+(?:[\\p{L}-]+\\s+){0,2}?teams?\\b)", Pattern.CASE_INSENSITIVE);

	/**
	 * A team of a mode that this role joins, or that a sentence opens with: {@code as part of a remote, globally
	 * distributed team}, {@code <Unit> is a fully remote software team}, {@code This globally distributed, fully remote
	 * team …}; never a company ({@code a remote first company with teams in …}).
	 */
	private static final Pattern TEAM_OF_A_MODE = Pattern.compile("(?:^\\W*(?:this|our)\\s+(?:[\\p{L}-]+,?\\s+){0,3}?"
			+ "|\\b(?:part\\s+of|join(?:ing)?|is|are)\\s+(?:a|an|our|the)\\s+)(?:fully\\s+|100\\s*%\\s*)?(remote|hybrid)\\b"
			+ "(?!\\s+first\\b),?\\s+(?:(?!(?:company|organi[sz]ation|business|culture)\\b)[\\p{L}-]+,?\\s+){0,3}?teams?\\b",
			Pattern.CASE_INSENSITIVE);

	/** This role as the one a company is hiring, of a mode: {@code Acme is looking for a remote Back End Engineer}. */
	private static final Pattern HIRING = Pattern.compile("\\b(?:looking\\s+for|seeking|hiring|searching\\s+for)\\s+"
			+ "(?:an?\\s+)?(?:fully\\s+|100\\s*%\\s*)?(remote|hybrid|on-?site)\\b(?![\\s-]+first\\b)", Pattern.CASE_INSENSITIVE);

	/** Remote offered with no subject: {@code Open to remote locations}, {@code Open to remote work for candidates …}. */
	private static final Pattern OPEN_TO_REMOTE = Pattern.compile("^\\W*open\\s+to\\s+(?:fully\\s+)?remote\\b",
			Pattern.CASE_INSENSITIVE);

	/** A company with no office, which covers every role as remote: {@code Speechify has no office}. */
	private static final Pattern NO_OFFICE = Pattern.compile("\\b(?:has|have)\\s+no\\s+(?:physical\\s+)?offices?\\b"
			+ "(?!\\s+(?:in|near|at)\\b)", Pattern.CASE_INSENSITIVE);

	/** A choice left to the role or the team, not offered to the candidate: {@code depending on team needs}. */
	private static final Pattern DEPENDING = Pattern.compile("\\bdepending\\s+on\\s+(?:the\\s+|your\\s+)?(?:role|team|"
			+ "position|business|needs)", Pattern.CASE_INSENSITIVE);

	/** A heading of what the company offers, under which modes listed are a perk: {@code We offer}, {@code Benefits}. */
	private static final Pattern PERKS = Pattern.compile("^\\W*(?:(?:what\\s+)?we\\s+offer|benefits?|perks?)\\b",
			Pattern.CASE_INSENSITIVE);

	/** A placeholder of a template, listing modes between brackets: {@code [office/remote/hybrid/field]}. */
	private static final Pattern PLACEHOLDER = Pattern.compile("\\[[^\\]]*/[^\\]]*\\]");

	/** The heading a vacancy's own labels sit under: {@code Job Details}. */
	private static final Pattern DETAILS = Pattern.compile("^\\W*(?:(?:job|position|role)\\s+(?:details|information|summary)"
			+ "|work(?:ing)?\\s+conditions)\\W*$", Pattern.CASE_INSENSITIVE);

	/** A pay heading, under which a mode alone names the vacancy's own: {@code Pay Range} over {@code Remote}. */
	private static final Pattern PAY = Pattern.compile("^\\W*(?:pay|salary|compensation)(?:\\s+(?:range|details|information))?\\W*$",
			Pattern.CASE_INSENSITIVE);

	/** A mode alone: {@code Remote}, {@code Fully remote}. */
	private static final Pattern BARE = Pattern.compile("^\\W*(?:fully\\s+|100\\s*%\\s*)?" + "(?:remote|hybrid|on[- ]?site)\\W*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * A heading that is the vacancy's own label: the mode alone, or the job family and the mode
	 * ({@code # Remote}, {@code # Consultant | Remote}), never a heading with a theme in it.
	 */
	private static final Pattern MODE_HEADING = Pattern.compile("^\\W*(?:[^|]+\\|\\s*)?(?:fully\\s+|100\\s*%\\s*)?"
			+ "(?:remote|hybrid|on[- ]?site|in[- ]office)\\W*$", Pattern.CASE_INSENSITIVE);

	/**
	 * A heading that opens with the mode and goes on with its schedule or its place: {@code # Hybrid 3 days (office
	 * locations are …)}, {@code # Hybrid — 3 days onsite}, {@code # Remote - United States}; never a theme
	 * ({@code # Remote-first culture}, {@code # Hybrid (and why we love it)}).
	 */
	private static final Pattern MODE_HEADING_WITH_DETAILS = Pattern.compile("^\\W*(?:fully\\s+|100\\s*%\\s*)?(?:remote|hybrid|"
			+ "on[- ]?site|in[- ]office)(?:\\s+[-–—]\\s+|\\s*[:(]\\s*|\\s+)(?:[1-5]\\b|(?:one|two|three|four|five)\\b|(?-i:\\p{Lu}))"
			+ "(?!.*\\b(?:why|how|what|culture|love)\\b)", Pattern.CASE_INSENSITIVE);

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

	/** The office this role is based in, which a sentence naming no mode states as onsite: {@code based in our Leeds office}. */
	private static final Pattern BASED_IN_AN_OFFICE = Pattern.compile("\\b(?:based|located)\\s+(?:in|at|out\\s+of)\\s+"
			+ "(?:one\\s+of\\s+)?(?:our|the|an?)\\s+(?:[\\p{L}.,-]+\\s+){0,4}?offices?\\b", Pattern.CASE_INSENSITIVE);

	/** What before this role makes it the frame of a duty: {@code In this role, you will lead …}. */
	private static final Pattern FRAME = Pattern.compile("^\\W*in\\s*$", Pattern.CASE_INSENSITIVE);

	/** Where a sentence with days a week names the office it means: {@code from our offices}, {@code in an office}. */
	private static final Pattern OFFICE = Pattern.compile("\\boffices?\\b|\\bon[- ]?site\\b|\\bin[- ]person\\b",
			Pattern.CASE_INSENSITIVE);

	/**
	 * What keeps office days from covering this role: a policy for some roles, a recommendation, a negation, a
	 * condition, a possibility ({@code most roles}, {@code recommended office days}, {@code you will not need}); never
	 * {@code unless}, an exception granted case by case ({@code unless there is an approved accommodation}).
	 */
	private static final Set<String> SCHEDULE_HEDGES = scheduleHedges();

	/**
	 * Onsite stated as what the job requires, whoever its subject: {@code required to work on-site}, {@code Ability to
	 * work onsite}, {@code must work onsite}, {@code will be an on-site role}, {@code requires 100% onsite work}.
	 */
	private static final Pattern REQUIRED_ONSITE = Pattern.compile("\\b(?:required|requires?|requirement|must|"
			+ "need(?:s|ed)?\\s+to|ability|able|willing(?:ness)?|expected|will\\s+be(?:\\s+an?)?)\\s+(?:to\\s+)?(?:be\\s+)?"
			+ "(?:work(?:ing)?\\s+|report(?:ing)?\\s+)?(?:100\\s*%\\s*|fully\\s+|full[- ]time\\s+)?"
			+ "(?:on[- ]?site|in[- ]office|in\\s+the\\s+office)\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * Every working day at the office or a site, whoever its subject: {@code 5 days/week in-office}, {@code full-time
	 * on-site}, {@code work fully from the office}.
	 */
	private static final Pattern EVERY_DAY = Pattern.compile("\\b(?:5|five)\\s*(?:x|days?)\\b(?=[^.;]*?(?:\\boffices?\\b"
			+ "|\\bon[- ]?site\\b|\\bin[- ]person\\b))|\\b(?:full[- ]time|fully|100\\s*%|entirely)\\s*(?:(?:from|in|at|out\\s+of)"
			+ "\\s+(?:the|our|an?)\\s+offices?|on[- ]?site|in[- ]office|in[- ]person)\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * The work primarily at the office or a site, whatever it allows away from it: {@code Primarily based in the
	 * Innovation Hub … with flexibility to work remotely as required}, {@code Majority of tasking is performed on-site};
	 * never primarily based in a place, which is where the person lives ({@code primarily based in the US}).
	 */
	private static final Pattern PRIMARILY_ONSITE = Pattern.compile("\\b(?:primarily|mainly|mostly|predominantly)\\s+"
			+ "(?:(?:an?\\s+)?(?:on[- ]?site|in[- ]office|in\\s+the\\s+office|office[- ]based)|based\\s+(?:in|at|out\\s+of)\\s+"
			+ "(?:the|our|an?)\\s+(?:[\\p{L}.,-]+\\s+){0,3}?(?:office|hub|facility|site|campus|lab|plant|headquarters|hq)\\b)\\b"
			+ "|\\bmajority\\s+of\\s+(?:the\\s+)?(?:\\p{L}+\\s+){0,2}?(?:is|are|will\\s+be)\\s+(?:performed|done|spent|"
			+ "conducted|completed)\\s+(?:on[- ]?site|in\\s+the\\s+office|in[- ]office)\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * A job whose work is travelling to sites, which is onsite: {@code travel on site/test bed for commissioning},
	 * {@code travels to customer sites for on-site implementations}, {@code to computer sites throughout the County};
	 * never a visit to a customer ({@code travel to customer sites}), nor travel now and then or for a share of the
	 * time ({@code some travel}, {@code up to 25%}).
	 */
	private static final Pattern SITE_WORK = Pattern.compile("^(?!.*(?:\\d\\s*%|\\bup\\s+to\\b|\\b(?:may|might|could|not|no|"
			+ "some|limited|minimal|light)\\b)).*?\\btravel(?:s|ling|ing|led)?\\b[^.;]{0,40}?\\b(?:on[- ]?site|(?:customer|client|"
			+ "computer|field|job|project|work)\\s+sites?\\s+(?:for|throughout|across)\\b)", Pattern.CASE_INSENSITIVE);

	/**
	 * The office the work is in, which a sentence naming no mode states as onsite: {@code work in our New York office},
	 * {@code work out of our Chicago office}, {@code join our team at Acme's Sunnyvale office}, {@code hiring a … in our
	 * Beavercreek Ohio Office}; never an office as the work's surroundings ({@code work in an office environment}), a
	 * unit of the organisation ({@code the Program Management Office}), or the company's offices ({@code our offices
	 * worldwide}).
	 */
	private static final Pattern WORKS_IN_AN_OFFICE = Pattern.compile("\\b(?:work(?:s|ing)?|join(?:ing)?\\s+(?:our|the)"
			+ "\\s+(?:[\\p{L}-]+\\s+){0,3}?team|hiring\\b[^.;]{0,120}?)\\s+(?:in|at|out\\s+of|from)\\s+(?:one\\s+of\\s+)?"
			+ "(?:our|the|[\\p{L}&.-]+['’]s)\\s+(?:[\\p{L}.,-]+\\s+){0,4}?"
			+ "(?<!\\b(?:front|back|management|program|project|executive|post|home)\\s)office\\b"
			+ "|\\bwork(?:s|ing)?\\s+(?:in|at|out\\s+of|from)\\s+one\\s+of\\s+our\\s+offices\\b"
			+ "(?!\\s+(?:environment|setting|hours|equipment|culture|supplies|space)\\b)", Pattern.CASE_INSENSITIVE);

	/**
	 * What makes an onsite statement a visit, not the job's mode ({@code as needed}, {@code occasionally}), or regular
	 * days in the office, which are hybrid ({@code in the office regularly}).
	 */
	private static final Pattern NOW_AND_THEN = Pattern.compile("\\bas\\s+(?:needed|required|necessary)\\b"
			+ "|\\bregular(?:ly)?\\b"
			+ "|\\bwhen\\s+(?:needed|required|necessary)\\b|\\boccasional(?:ly)?\\b|\\bperiodic(?:al(?:ly)?)?\\b"
			+ "|\\bat\\s+times\\b|\\bsometimes\\b|\\bfrom\\s+time\\s+to\\s+time\\b", Pattern.CASE_INSENSITIVE);

	/**
	 * An event the person attends, whose days are not the job's schedule or mode: {@code onboarding training},
	 * {@code team events}, {@code during kickoff}.
	 */
	private static final Pattern EVENT = Pattern.compile("\\b(?:events?|training|onboarding|orientation|kick-?off)\\b",
			Pattern.CASE_INSENSITIVE);

	/** What before an onsite statement keeps it from stating it: a negation, a possibility, a policy for some roles. */
	private static final Set<String> NOT_REQUIRED = notRequired();

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
		if (text == null) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
		Matcher label = LOCATION_LABEL.matcher(text);
		if (label.find()) {
			return EnumSet.of((label.group("home") != null) ? WorkModeEnum.REMOTE : WorkModeEnum.ONSITE);
		}
		if (VIRTUAL_LOCATION.matcher(text).find()) {
			return EnumSet.of(WorkModeEnum.REMOTE);
		}
		return offered(text, WORDS, SUBJECTS);
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
	 * The modes a description states for this vacancy, by the strongest kind of statement it holds: its sentences about
	 * this role, the choices it offers, its office days, onsite stated as required, its labels, the office the work is in,
	 * its tags, a company with no office, a job travelling to sites; none where the statements of that kind disagree.
	 * Onsite stated as required, or as the office the work is in, gives way to a hybrid label or tag, which names the
	 * office days it leaves out.
	 */
	private Set<WorkModeEnum> stated(List<Segment> segments) {
		List<Set<WorkModeEnum>> sentences = new ArrayList<>();
		List<Set<WorkModeEnum>> offers = new ArrayList<>();
		List<Set<WorkModeEnum>> schedules = new ArrayList<>();
		List<Set<WorkModeEnum>> requirements = new ArrayList<>();
		List<Set<WorkModeEnum>> labels = new ArrayList<>();
		List<Set<WorkModeEnum>> offices = new ArrayList<>();
		List<Set<WorkModeEnum>> tags = new ArrayList<>();
		List<Set<WorkModeEnum>> policies = new ArrayList<>();
		List<Set<WorkModeEnum>> sites = new ArrayList<>();
		for (int i = 0; i < segments.size(); i++) {
			Segment segment = segments.get(i);
			String text = segment.text();
			if (text == null || text.isBlank()) {
				continue;
			}
			Set<WorkModeEnum> said = sentence(text);
			add(sentences, said);
			if (said.isEmpty() && (segment.under() == null || !PERKS.matcher(segment.under()).find())) {
				add(offers, offer(text, segments, i));
			}
			add(schedules, scheduled(text));
			add(requirements, onsite(text, REQUIRED_ONSITE, EnumSet.of(WorkModeEnum.ONSITE)));
			add(requirements, onsite(text, EVERY_DAY, EnumSet.of(WorkModeEnum.ONSITE)));
			add(requirements, primarilyOnsite(text));
			add(sites, onsite(text, SITE_WORK, EnumSet.of(WorkModeEnum.ONSITE)));
			add(offices, onsite(text, WORKS_IN_AN_OFFICE, EnumSet.noneOf(WorkModeEnum.class)));
			Matcher labelled = LABELLED.matcher(text);
			Matcher placed = PLACED_LABEL.matcher(text);
			String placedMode = placedMode(text);
			if (segment.kind() == SegmentKindEnum.HEADING && MODE_HEADING.matcher(text).find()) {
				add(labels, listed(text.replaceFirst("^[^|]*\\|", ""), segments, i));
			}
			else if (segment.kind() == SegmentKindEnum.HEADING && MODE_HEADING_WITH_DETAILS.matcher(text).find()) {
				add(labels, offered(text, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS));
			}
			else if (labelled.find()) {
				add(labels, listed(labelled.group(1), segments, i));
			}
			else if (placed.find() && placed.group(1).strip().split("\\s+").length <= PLACED_VALUE_WORDS
					&& !wordsNamedIn(placed.group(1), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty()) {
				add(labels, listed(placed.group(1), segments, i));
			}
			else if (placedMode != null) {
				add(labels, offered(placedMode, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS));
			}
			else if (i + 1 < segments.size() && LABEL_ALONE.matcher(text).find()) {
				String value = segments.get(i + 1).text();
				if (value != null && VALUE.matcher(value).find() && value.strip().split("\\s+").length <= VALUE_WORDS) {
					add(labels, listed(value, segments, i + 1));
				}
			}
			else if (segment.under() != null && (DETAILS.matcher(segment.under()).find()
					&& text.strip().split("\\s+").length <= VALUE_WORDS
					|| PAY.matcher(segment.under()).find() && BARE.matcher(text).find())) {
				add(labels, listed(text, segments, i));
			}
			Matcher tag = TAG.matcher(text);
			while (tag.find()) {
				add(tags, offered(tag.group(1), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS));
			}
			if (NO_OFFICE.matcher(text).find()) {
				add(policies, EnumSet.of(WorkModeEnum.REMOTE));
			}
		}
		boolean labelledHybrid = labels.contains(EnumSet.of(WorkModeEnum.HYBRID)) || tags.contains(EnumSet.of(WorkModeEnum.HYBRID));
		for (List<Set<WorkModeEnum>> kind : List.of(sentences, offers, schedules, requirements, labels, offices, tags,
				policies, sites)) {
			if ((kind == requirements || kind == offices) && !kind.isEmpty() && labelledHybrid) {
				return EnumSet.of(WorkModeEnum.HYBRID);
			}
			if (!kind.isEmpty()) {
				return (new HashSet<>(kind).size() == 1) ? kind.getFirst() : EnumSet.noneOf(WorkModeEnum.class);
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	/**
	 * The mode a short segment names beside a place or a label ({@code London / Hybrid}, {@code Onsite: Eglin AFB}), or
	 * null where it is no such segment, or its place is itself a mode ({@code Onsite / Hybrid}) or an answer
	 * ({@code Hybrid: No}, {@code Onsite: Not required}).
	 */
	private String placedMode(String text) {
		for (Pattern placed : List.of(PLACE_MODE, MODE_PLACE)) {
			Matcher matcher = placed.matcher(text);
			if (matcher.find() && wordsNamedIn(matcher.group("place"), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty()
					&& List.of(words(matcher.group("place")).split(" ")).stream().noneMatch(NOT_A_PLACE::contains)) {
				return matcher.group("mode");
			}
		}
		return null;
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
		if (OPEN_TO_REMOTE.matcher(text).find()) {
			return EnumSet.of(WorkModeEnum.REMOTE);
		}
		if (PLACEHOLDER.matcher(text).find()) {
			return EnumSet.noneOf(WorkModeEnum.class);
		}
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
	 * Hybrid where a sentence of a segment names office days short of the week, whoever its subject
	 * ({@code Visa requires at least 3 days in office}, {@code Monday to Thursday, work onsite}): a floor for every
	 * role covers this one. Empty where none does, where a hedge, a negation or a condition qualifies it, or where the
	 * days are an event's ({@code 3 days in-person onboarding training}).
	 */
	private Set<WorkModeEnum> scheduled(String text) {
		for (String sentence : SENTENCE_END.split(text)) {
			boolean days = DAYS_A_WEEK.matcher(sentence).find() || OFFICE_DAYS.matcher(sentence).find()
					|| partOfTheWeek(sentence);
			boolean office = OFFICE.matcher(spaced(sentence, DESCRIPTION_SUBJECTS)).find()
					|| !wordsNamedIn(sentence, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty();
			if (days && office && !EVENT.matcher(sentence).find() && List.of(words(sentence).split(" ")).stream().noneMatch(SCHEDULE_HEDGES::contains)) {
				return EnumSet.of(WorkModeEnum.HYBRID);
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	/**
	 * Onsite where a sentence of a segment states it by this pattern, whoever its subject: as what the job requires
	 * ({@code The selected candidate will be required to work on-site}) or as the office the work is in. The sentence
	 * names no mode but those it may name and no schedule, which the steps before read, and no negation or possibility
	 * comes before the statement, nor a visit now and then anywhere in it ({@code as needed}).
	 * @param named the modes the sentence's words may name
	 */
	private Set<WorkModeEnum> onsite(String text, Pattern statement, Set<WorkModeEnum> named) {
		for (String sentence : SENTENCE_END.split(text)) {
			sentence = RULED_OUT.matcher(sentence).replaceAll(" ");
			Matcher stated = statement.matcher(sentence);
			if (!stated.find() || SCHEDULE.matcher(sentence).find() || DAYS_A_WEEK.matcher(sentence).find()
					|| NOW_AND_THEN.matcher(sentence).find() || EVENT.matcher(sentence).find()
					|| !named.containsAll(wordsNamedIn(sentence, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS))) {
				continue;
			}
			if (unqualified(sentence, stated)) {
				return EnumSet.of(WorkModeEnum.ONSITE);
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	/**
	 * Onsite where a sentence of a segment states the work as primarily at the office or a site, whatever it allows away
	 * from it, with no negation or possibility before it.
	 */
	private Set<WorkModeEnum> primarilyOnsite(String text) {
		for (String sentence : SENTENCE_END.split(text)) {
			Matcher stated = PRIMARILY_ONSITE.matcher(sentence);
			if (stated.find() && !SCHEDULE.matcher(sentence).find() && !DAYS_A_WEEK.matcher(sentence).find()
					&& unqualified(sentence, stated)) {
				return EnumSet.of(WorkModeEnum.ONSITE);
			}
		}
		return EnumSet.noneOf(WorkModeEnum.class);
	}

	/** Whether no negation, possibility or policy for some roles comes before a statement in its sentence. */
	private boolean unqualified(String sentence, MatchResult statement) {
		return List.of(words(sentence.substring(0, statement.start())).split(" ")).stream().noneMatch(NOT_REQUIRED::contains);
	}

	/**
	 * The modes a label's value gives. {@code Office} alone is onsite ({@code Remote Type: Office}). A list of modes
	 * alone ({@code Hybrid / Onsite / Remote}) is narrowed by the first segment after it naming a mode with a schedule
	 * ({@code 3 days in the office}); where none does, a list of every mode is a template and gives none.
	 */
	private Set<WorkModeEnum> listed(String value, List<Segment> segments, int at) {
		if (words(value).equals("office")) {
			return EnumSet.of(WorkModeEnum.ONSITE);
		}
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
		for (Pattern ofAMode : List.of(TEAM_OF_A_MODE, HIRING)) {
			Matcher stated = ofAMode.matcher(text);
			if (stated.find()) {
				return offered(stated.group(1), DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS);
			}
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
		if (wordsNamedIn(statement, DESCRIPTION_WORDS, DESCRIPTION_SUBJECTS).isEmpty()) {
			statement = BASED_IN_AN_OFFICE.matcher(statement).replaceFirst("onsite");
		}
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

	private static Set<String> notRequired() {
		Set<String> words = new HashSet<>(NEGATIONS);
		words.addAll(Set.of("may", "might", "could", "most", "some", "many", "certain", "select"));
		return Set.copyOf(words);
	}

	private static Set<String> scheduleHedges() {
		Set<String> words = new HashSet<>(NOT_STATED);
		words.addAll(Set.of("most", "some", "many", "certain", "select", "recommended", "encouraged", "optional",
				"depending"));
		words.remove("unless");
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
