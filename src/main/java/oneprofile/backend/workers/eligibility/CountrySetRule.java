package oneprofile.backend.workers.eligibility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.CountrySetVacancy;
import oneprofile.backend.storage.normalizedvacancy.LocationPlaces;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.PlaceKindEnum;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.normalization.WorkModeRule;
import org.springframework.stereotype.Component;

/**
 * Reads the country set of one vacancy of the engineering subset (#13, PRD §5.4): every set of places it states,
 * each with the text that stated it and its kind. Rules only (ADR-0005). Its eligibility state is derived from
 * it at query time, by the eligibility rule.
 * <p>
 * Each string of the declared location is an option, with the places the place run read in it (#26) and its own
 * work mode: the mode its words name, otherwise the vacancy's, one option for each mode a vacancy offering a
 * choice offers. A string offering remote beside another mode
 * ({@code NYC (Hybrid); United States (Remote)}) is a choice, and is cut at its semicolons and its {@code or}s
 * into options, each read again for its places and its own mode; one naming remote among others is remote, and
 * one naming none is undeclared.
 * <p>
 * The title states a set where a part of it, between its dashes, commas and brackets, names only regions and
 * countries, beside work-mode words at most ({@code Developer - Remote, Latin America}); a city in a title is
 * left to the location. It states a clearance where it names one ({@code TS/SCI w/ Poly}).
 * <p>
 * The description states a set where a segment requires where the person is: that candidates must be based,
 * located or living somewhere ({@code candidates must be based in Poland}), that the vacancy is open only to those
 * in a place, that it hires or is remote within one, that it is for a place only ({@code US only}), or that the
 * company hires or the person may be located anywhere ({@code we hire from anywhere}, a global set). Worldwide is
 * kept only when the statement's own sentence names hiring or the person's location; joining alone supplies no
 * such context, while joining from any country is itself global hiring. A sentence welcoming, accepting,
 * encouraging or inviting applications or applicants drops Worldwide while keeping other places it names. Working
 * from anywhere as a perk or culture states nothing. Hiring managers, teams, processes and partners are not hiring
 * words. A statement of where this role is done requires no must: {@code you are based in},
 * {@code this role is based at}, {@code this role is located in}, {@code you'll work in} where its object is a
 * place, or {@code looking for an engineer based in}, including a title with a comma before {@code based}. A
 * location label at a segment's start also states its places, across brackets and semicolons up to a newline or a
 * sentence's end, stopping before headquarters, HQ or head office. A label whose value holds requires,
 * required, must, will or should names no place. A bare remote option, including one beside a
 * semicolon, makes it a choice that states nothing. A place it names in passing ({@code our offices in London},
 * {@code for candidates based in the United States, the pay range is}) states nothing, and neither does one after
 * a negation ({@code we are not able to hire in Quebec}). A team's location states nothing: a team before a remote
 * or anywhere statement in its own clause, or inside the words naming the role or person being sought, is not the
 * person's location. It states work authorization where it requires authorization or the right to work in a
 * country, citizenship where it requires a country's citizens ({@code US Citizenship Required}, a
 * {@code U.S. person}), and a clearance where it requires one; never where it is conditional ({@code for all
 * positions with a government clearance}), for some roles only, or a list of what the company does not
 * discriminate by. A citizenship statement offering an export licence, authorization or licence exemption
 * as an alternative states no citizenship. A label is read with its answer: {@code Government Clearance
 * Required?} followed by {@code No}, or
 * {@code Clearance Required to Start: None}, requires nothing, and so does a legal notice of what an employer may
 * not require. A segment under a heading of what is preferred ({@code Preferred Qualifications}) states nothing. A
 * perk or culture heading also states nothing unless its segment names hiring or the person's location; visa
 * sponsorship and relocation offers are still recorded. It states a timezone requirement where it says the person
 * lives or works within a window of UTC offsets ({@code working from UTC-1 to UTC+4}), or requires a named
 * zone ({@code APAC time zones}, {@code US Eastern or Central time}). Each named alternative keeps its
 * sentence and offsets; a US zone also names the United States alone. A window of hours to
 * overlap or attend is a schedule and states nothing. The place a statement names is read from the words after it,
 * up to the end of its clause, by the place rule, a name of several countries' places in a country the declared
 * options name. Most, many, several or numerous countries name no place. An employer of record in most countries
 * or outside its home countries states Worldwide only where its sentence also names hiring or employment;
 * elsewhere and other countries keep their reading.
 * <p>
 * A clearance is a set of the country it is of: the United States for one only the United States grants
 * ({@code TS/SCI}, a polygraph, public trust), then the country named within three words before clearance
 * or by its scheme ({@code SC}, {@code DV}, {@code BPSS} for the United Kingdom; {@code Baseline}, {@code NV1},
 * {@code NV2} for Australia), otherwise the one country the declared location names, and the United States
 * where it names none or several. American, Australian, British, Canadian and Irish name their countries in
 * a clearance phrase or before citizenship. Each clearance in a segment is read on its own; one preferred
 * in its own sentence states nothing.
 * <p>
 * Visa sponsorship and relocation add no country (ADR-0027): a description offering either, in a sentence
 * that does not negate it, is recorded as offering it.
 */
@Component
public class CountrySetRule {

	private static final int IGNORING_CASE = Pattern.CASE_INSENSITIVE;

	/** What a title is cut into parts at: dashes between spaces, commas, brackets, bars, slashes and colons. */
	private static final Pattern TITLE_PARTS = Pattern.compile("\\s[-–—]\\s|[,()\\[\\]|/:]");

	/** The words a part of a title may hold beside its places and still name only them. */
	private static final Set<String> BESIDE_PLACES = Set.of("remote", "hybrid", "onsite", "on", "site", "based",
			"only", "in", "the", "and", "or", "fully", "anywhere");

	/**
	 * The codes a title or a {@code US only} names a place by. Any other code of two or three capitals names
	 * something else there ({@code AI/ML}, {@code GTM}, the state in {@code Oakland, CA Office}).
	 */
	private static final Set<String> PLACE_CODES = Set.of("US", "USA", "UK", "EU", "UAE", "KSA", "EMEA", "APAC", "APJ",
			"LATAM", "LAC", "AMER", "NAMER", "NORAM", "ANZ", "DACH", "MENA", "CEE", "UKI", "SEA", "CIS");

	/** A code of two or three capitals, as the place rule reads one. */
	private static final Pattern CODE = Pattern.compile("\\b[A-Z]{2,3}\\b");

	/** Where a location string offering several modes is cut into its options. */
	private static final Pattern OPTIONS = Pattern.compile(";|\\s+or\\s+|,\\s*or\\s+", IGNORING_CASE);

	/** A clearance only the United States grants. */
	private static final Pattern US_CLEARANCE = Pattern.compile(
			"\\bTS\\s*/\\s*SCI\\b|\\bTS-SCI\\b|\\bpolygraph\\b|\\b(?:w/|with|ci|fs|full[- ]scope)\\s*poly\\b|\\bpublic trust\\b",
			IGNORING_CASE);

	/** A clearance of any country, as a title names one. */
	private static final Pattern TITLE_CLEARANCE = Pattern.compile(
			"\\bclearance\\b|\\bcleared\\b|\\b(?:top )?secret\\b(?!s|\\s+(?:management|manager|sauce|store|storage))",
			IGNORING_CASE);

	private static final String UNITED_STATES = "US";

	private static final String WORLDWIDE = "Worldwide";

	/** Where the person must be: {@code candidates must be based in}, {@code must reside within}. */
	private static final Pattern MUST_BE_IN = Pattern.compile("\\b(?:must|required to|need to|needs to|have to|has to|"
			+ "should)\\s+(?:currently\\s+|already\\s+|also\\s+)?(?:be\\s+)?(?:physically\\s+)?(?:based|located|residing|"
			+ "living|resident|reside|live)\\s+(?:in|within)\\s+", IGNORING_CASE);

	/** Where the person is or will be based, without an explicit must. */
	private static final Pattern YOU_ARE_IN = Pattern.compile("\\byou(?:\\s+(?:are|will\\s+be)|['’]re|['’]ll\\s+be)\\s+"
			+ "(?:(?:currently|primarily)\\s+)?(?:based|located)\\s+(?:in|within)\\s+", IGNORING_CASE);

	/** Where this role is done, required by the nature of the statement. */
	private static final Pattern ROLE_IN = Pattern.compile("\\b(?:role|position|job|opportunity)\\s+"
			+ "(?:(?:is|will\\s+be)\\s+)?(?:\\w+\\s+){0,3}?(?:based|located)\\s+(?:at|in|within|out\\s+of)\\s+",
			IGNORING_CASE);

	/** Where the person will work, with or without be. */
	private static final Pattern YOU_WORK_IN = Pattern.compile("\\byou(?:['’]ll|\\s+will)\\s+(?:be\\s+)?work(?:ing)?\\s+"
			+ "(?:in|from|out\\s+of)\\s+", IGNORING_CASE);

	/**
	 * The words that may stand beside the places {@code you'll work in} names, which names nothing else:
	 * {@code You'll work in our Vilnius office}.
	 */
	private static final Set<String> BESIDE_WORK_PLACES = Set.of("the", "our", "and", "or", "office", "offices", "hub",
			"hubs", "hq", "headquarters", "studio", "campus");

	/** Looking for a person based somewhere states where that person must be. */
	private static final Pattern LOOKING_FOR_IN = Pattern.compile("\\blooking\\s+for\\s+(?:(?:a|an)\\s+)?"
			+ "(?:\\w+,?\\s+){0,10}?based\\s+(?:in|within)\\s+", IGNORING_CASE);

	/** A location label at the start of a description segment. */
	private static final Pattern LOCATION_LABEL = Pattern.compile("^\\s*(?:(?:job|work|role)\\s+)?locations?\\s*:\\s*",
			IGNORING_CASE);

	/** A label value describing a requirement rather than directly naming a place. */
	private static final Pattern LOCATION_VERB = Pattern.compile("\\b(?:requires|required|must|will|should)\\b",
			IGNORING_CASE);

	/**
	 * A location value offering remote as an alternative to its places ({@code San Francisco or Remote}), which
	 * requires none of them; {@code Remote, US or Canada} offers a choice of places only.
	 */
	private static final Pattern REMOTE_CHOICE = Pattern.compile("\\b(?:or|/)\\s*remote\\b|\\bremote\\s*(?:or\\b|/)"
			+ "|(?:^|;)\\s*remote\\s*(?=;|$)",
			IGNORING_CASE);

	/** The company location after which a location label no longer names the person's places. */
	private static final Pattern HEADQUARTERS = Pattern.compile("\\b(?:headquarters|HQ|head\\s+office)\\b", IGNORING_CASE);

	/** A preference, which states nothing: {@code This role is preferably based in the US}. */
	private static final Pattern PREFERRED = Pattern.compile("\\b(?:prefer\\w*|ideally|typically|usually)\\b", IGNORING_CASE);

	/** Who the vacancy is open to: {@code open only to candidates residing in}. */
	private static final Pattern OPEN_TO = Pattern.compile("\\b(?:open|available|limited|restricted)\\s+(?:only\\s+)?to\\s+"
			+ "(?:candidates|applicants|residents|individuals|people|those|talent)\\s+(?:who\\s+(?:are|live|reside)\\s+)?"
			+ "(?:currently\\s+)?(?:(?:based|located|residing|living)\\s+)?(?:in|within|of|from)\\s+", IGNORING_CASE);

	/** What may stand between anywhere and the place it is in: {@code anywhere you like in}. */
	private static final String YOU_LIKE = "(?:you\\s+(?:like|want|choose|wish)\\s+)?";

	/** Where the person may be anywhere in: {@code employees may live anywhere in}. */
	private static final Pattern ANYWHERE_IN = Pattern.compile("\\b(?:live|work|working|based|located|remote|remotely|"
			+ "hire|hiring|be|reside|residing|performed|done)\\s+(?:from\\s+)?anywhere\\s+" + YOU_LIKE
			+ "(?:in|within|across)\\s+|\\bremote[\\s,:–—-]+anywhere\\s+" + YOU_LIKE + "(?:in|within|across)\\s+",
			IGNORING_CASE);

	/** That the person may work from anywhere at all. */
	private static final Pattern FROM_ANYWHERE = Pattern.compile("\\b(?:live|work|working|remote|remotely|hire|hiring)\\s+"
			+ "(?:remotely\\s+)?from\\s+anywhere\\b(?!\\s+" + YOU_LIKE + "(?:in|within|across)\\b)", IGNORING_CASE);

	/** Hiring rather than the people or process doing it. */
	private static final String HIRING = "(?:hire|hires|hired|hiring(?!\\s+(?:managers?|team|process|partners?)\\b))";

	/** Hiring or the person's location, which a worldwide statement needs in its own sentence. */
	private static final Pattern WORLDWIDE_CONTEXT = Pattern.compile("\\b(?:" + HIRING + "|employ|employs|employed|employing|"
			+ "recruit\\w*|employer of record|candidates|applicants|(?:can|may|will|'ll|’ll)\\s+(?:be\\s+)?"
			+ "(?:located|based)|(?:can|may)\\s+(?:live|reside))\\b", IGNORING_CASE);

	/** Welcoming applications anywhere does not state worldwide hiring. */
	private static final Pattern WELCOMING_APPLICATIONS = Pattern.compile("\\b(?:welcom\\w*|accept\\w*|encourag\\w*|"
			+ "invit\\w*)\\s+(?:\\w+,?\\s+){0,4}?(?:applications|applicants)\\b", IGNORING_CASE);

	/** Hiring globally or joining from any country states a global set. */
	private static final Pattern GLOBAL_HIRING = Pattern.compile("\\b(?:(?:" + HIRING + "|recruit|recruiting)\\s+"
			+ "(?:\\w+\\s+){0,3}?(?:globally|worldwide)|(?:" + HIRING + "|join)\\s+(?:\\w+\\s+){0,4}?from\\s+"
			+ "(?:(?:almost|nearly|virtually|practically)\\s+)?any\\s+country)\\b", IGNORING_CASE);

	/** Hiring through an employer of record may reach countries outside those already named. */
	private static final Pattern EMPLOYER_OF_RECORD = Pattern.compile("\\b(?:employer\\s+of\\s+record|EOR)\\b", IGNORING_CASE);

	/** Where the employer of record reaches; the captured phrasings also need a hiring word in the sentence. */
	private static final Pattern ELSEWHERE = Pattern.compile("\\b(?:(?:based|located)\\s+elsewhere|in\\s+other\\s+countries|"
			+ "(in\\s+most\\s+countries|outside\\s+(?:of\\s+)?(?:our|the|its)\\s+(?:(?:home|core|main)\\s+)?countr(?:y|ies)))\\b",
			IGNORING_CASE);

	/** A vague number of countries, which names no place even where Most is a town's name. */
	private static final Pattern VAGUE_COUNTRIES = Pattern.compile("^\\s*(?:the\\s+)?(?:most|many|several|numerous)\\s+countries\\b",
			IGNORING_CASE);

	/**
	 * Where the company hires, as employees or contractors, or the vacancy is remote: {@code we only hire in},
	 * {@code we hire contractors in}, {@code through an employer of record in}, {@code remote within}.
	 */
	private static final Pattern HIRES_IN = Pattern.compile("\\b(?:(?:only\\s+|currently\\s+|able\\s+to\\s+)?(?:hire|hiring|employ|"
			+ "employing)\\s+(?:candidates\\s+|people\\s+|talent\\s+|contractors\\s+|freelancers\\s+)?(?:only\\s+)?(?:in|from|"
			+ "within|across)|remote(?:ly)?\\s+(?:only\\s+)?(?:within|in|from|across)|employer\\s+of\\s+record\\s+(?:in|across))\\s+",
			IGNORING_CASE);

	/** A team or company location, rather than the person whose location the vacancy states. */
	private static final Pattern TEAM = Pattern.compile("\\b(?:teams?|members|colleagues|workforce|managers|headquarters|HQ)\\b",
			IGNORING_CASE);

	/** A place a vacancy is for only: {@code US only}, {@code UK-based candidates only}. */
	private static final Pattern PLACE_ONLY = Pattern.compile("((?:U\\.S\\.|[A-Z][\\w.]*)(?:[ -][A-Z][\\w.]*){0,2})"
			+ "(?:[ -]based)?\\s+(?:candidates\\s+|applicants\\s+|residents\\s+)?only\\b");

	/** Work authorization a country is named after: {@code authorized to work in}, {@code right to work in}. */
	private static final Pattern AUTHORIZED_IN = Pattern.compile("\\b(?:(?:authori[sz]ed|eligible|permitted|allowed|entitled|"
			+ "able)\\s+to\\s+(?:legally\\s+)?work|right\\s+to\\s+work|work\\s+(?:authori[sz]ation|permit|visa|rights))"
			+ "\\s+(?:full[- ]time\\s+|permanently\\s+)?(?:for\\s+any\\s+employer\\s+)?(?:in|within|for)\\s+", IGNORING_CASE);

	/**
	 * Work authorization a country is named before: {@code U.S. work authorization}. Not where sponsorship follows,
	 * which is a policy on visas: {@code U.S. work authorization sponsorship (H-1B, TN, J) is offered}.
	 */
	private static final Pattern PLACE_AUTHORIZATION = Pattern.compile(
			"((?:U\\.S\\.|[A-Z][\\w.]*)(?: [A-Z][\\w.]*){0,2})\\s+(?i:work\\s+(?:authori[sz]ation|permit|rights))\\b(?!\\s+sponsorship)");

	/** Citizenship of a country named before it: {@code US citizenship}, {@code U.S. person}. */
	private static final Pattern PLACE_CITIZENSHIP = Pattern.compile("((?:U\\.S\\.|[A-Z][\\w.]*)(?: [A-Z][\\w.]*){0,2})\\s+"
			+ "(?i:citizens?|citizenship|persons?)\\b(?!\\s+(?i:and\\s+immigration))");

	/** Citizenship of a country named after it: {@code citizens of}. */
	private static final Pattern CITIZENS_OF = Pattern.compile("\\bcitizens?(?:hip)?\\s+of\\s+", IGNORING_CASE);

	/** Demonyms that name a country before citizenship or within a clearance phrase. */
	private static final Map<String, String> DEMONYMS = Map.of("american", "US", "australian", "AU", "british", "GB",
			"canadian", "CA", "irish", "IE");

	/** A demonym inside a clearance's own phrase. */
	private static final Pattern DEMONYM = Pattern.compile("\\b(?:American|Australian|British|Canadian|Irish)\\b",
			IGNORING_CASE);

	/** The clearance noun and up to three words naming whose clearance it is. */
	private static final Pattern CLEARANCE_PHRASE = Pattern.compile("\\b(?:[\\p{L}\\d./-]+\\s+){0,3}clearance\\b",
			IGNORING_CASE);

	/** Clearance schemes of the United Kingdom. */
	private static final Pattern GB_CLEARANCE = Pattern.compile("\\b(?:SC|DV|BPSS)\\b", IGNORING_CASE);

	/** Clearance schemes of Australia. */
	private static final Pattern AU_CLEARANCE = Pattern.compile("\\b(?:Baseline|NV1|NV2)\\b", IGNORING_CASE);

	/** An alternative after citizenship that admits people through export permission instead. */
	private static final Pattern EXPORT_ALTERNATIVE = Pattern.compile("\\bor\\b[^;!?]*?\\b(?:export\\s+"
			+ "(?:licen[cs]e|authori[sz]ation)|licen[cs]e\\s+exemption)\\b", IGNORING_CASE);

	/** A clearance. */
	private static final Pattern CLEARANCE = Pattern.compile("\\b(?:TS\\s*/\\s*SCI|TS-SCI|top\\s+secret)\\b(?:\\s+clearance\\b)?|"
			+ "\\bpolygraph\\b|\\b(?:security|secret|government|federal|dod|active|interim|public\\s+trust)\\s+clearance\\b|"
			+ "\\b(?:SC|DV|NV1|NV2|BPSS|Baseline)\\s+(?:security\\s+)?clearance\\b|"
			+ "\\bclearance\\s+(?:is\\s+)?required\\b", IGNORING_CASE);

	/**
	 * A legal notice of what an employer may not require, in whose sentence a clearance or polygraph is required
	 * of no one: {@code Under Maryland law, an employer may not require … a polygraph examination}.
	 */
	private static final Pattern LEGAL_NOTICE = Pattern.compile("\\b(?:(?:may|shall|"
			+ "must|can|will|do|does)\\s+not\\s+(?:\\w+\\s+){0,2}?(?:require|demand)|cannot\\s+(?:require|demand)|"
			+ "prohibit\\w*)\\b", IGNORING_CASE);

	/**
	 * A heading over what is preferred rather than required ({@code Preferred Qualifications}, {@code Nice to
	 * have}, {@code Bonus points}), under which a segment requires nothing.
	 */
	private static final Pattern PREFERRED_HEADING = Pattern.compile("\\b(?:prefer\\w*|nice[- ]to[- ]haves?|desired|"
			+ "desirable|pluses|a plus|bonus\\s+(?:points?|skills?|qualifications?|if))\\b|^\\s*bonus\\W*$", IGNORING_CASE);

	/** A heading over perks or culture, under which places need hiring or the person's location. */
	private static final Pattern PERK_HEADING = Pattern.compile("\\b(?:benefits|perks|why join|why work|what we offer|"
			+ "our culture|culture|life at|what makes\\b.*\\bdifferent)\\b", IGNORING_CASE);

	/** Hiring or employment, excluding the employer whose presence alone does not say it hires. */
	private static final Pattern HIRING_WORD = Pattern.compile("\\b(?:" + HIRING + "|employ(?!ers?\\b)\\w*)\\b", IGNORING_CASE);

	/** Hiring or the person's location permits a statement under a perk heading. */
	private static final Pattern PERK_CONTEXT = Pattern.compile(HIRING_WORD.pattern()
			+ "|\\b(?:employer of record|recruit\\w*|candidates|applicants|(?:can|may|will|'ll|’ll)\\s+(?:be\\s+)?(?:located|based)|"
			+ "(?:can|may)\\s+(?:live|reside))\\b", IGNORING_CASE);

	/** A requirement, which a statement of authorization, citizenship or clearance needs beside it. */
	private static final Pattern REQUIRED = Pattern.compile("\\b(?:must|required|requires|require|need|needs|only|should|"
			+ "have to|has to|who are|eligible|possess|hold|active|ability to obtain|able to obtain)\\b", IGNORING_CASE);

	/** A negation, after which a place states nothing. */
	private static final Pattern NEGATION = Pattern.compile("\\b(?:not|no|unable|cannot|can't|won't|except|excluding|"
			+ "nor|without)\\b|n't\\b", IGNORING_CASE);

	/** A condition, under which a requirement holds for some vacancies only: {@code for US based roles}. */
	private static final Pattern CONDITION = Pattern.compile("\\b(?:if|where applicable|when required|for all positions|"
			+ "for\\s+(?:[\\w.]+[- ]){1,3}based\\s+(?:roles|positions|jobs)|"
			+ "applicants\\s+for\\s+(?:employment|positions|roles)\\s+(?:in|within)|"
			+ "certain (?:other )?(?:positions|roles)|some positions|may be required|may require)\\b", IGNORING_CASE);

	/** What lists the grounds a company does not discriminate by, where citizenship is one of them. */
	private static final Pattern NON_DISCRIMINATION = Pattern.compile("\\b(?:regardless|without regard|national origin|"
			+ "discriminat\\w*|protected (?:class|characteristic|status|veteran))\\b", IGNORING_CASE);

	/** The words a clause ends at, after which a statement's place is no longer named. */
	private static final Pattern CLAUSE_END = Pattern.compile("\\b(?:with|without|who|whose|for|to|at|on|as|because|due|"
			+ "where|which|that|while|but|unless|must|prior|during|by|so|are|is|will|may|can|should|have|has|please|if|"
			+ "time|from|except|excluding|and\\s+(?:must|will|be|have|are|reside|live))\\b", IGNORING_CASE);

	/** A time working from anywhere is allowed for, which makes it a benefit rather than where the vacancy hires. */
	private static final Pattern BENEFIT = Pattern.compile("\\b(?:\\d+|a few|few|several|a|one|two|three|four|six)\\s+"
			+ "(?:days?|weeks?|months?)\\b|\\b(?:per|a|each|every)\\s+year\\b|\\b(?:policy|scheme|program|programme)\\b", IGNORING_CASE);

	/** A UTC offset: {@code UTC}, {@code GMT+5:30}, {@code UTC -6}. */
	private static final String OFFSET = "(?:UTC|GMT)\\s*(?:([+\\-−–])\\s*(\\d{1,2})(?::?(\\d{2}))?)?";

	/** A window of UTC offsets. */
	private static final Pattern WINDOW = Pattern.compile(OFFSET + "\\s*(?:to|-|–|—|and|through|until)\\s*" + OFFSET);

	/**
	 * Zone names, longer names before their parts so Asia-Pacific never reads as Pacific. Codes are matched in
	 * capitals only, so that {@code give us time} names no zone.
	 */
	private static final String ZONE_NAME = "(?:Central European|Asia-Pacific|Asia Pacific|United States|"
			+ "(?-i:US) Eastern|(?-i:US) Central|(?-i:US) Mountain|(?-i:US) Pacific|Los Angeles|San Francisco|New York|"
			+ "European|Eastern|Central|Mountain|Pacific|Chicago|Denver|Europe|U\\.S\\.|"
			+ "(?-i:APAC|CET|EST|CST|MST|PST|US|EU|ET|CT|MT|PT))";

	/** One named zone in a list of alternatives. */
	private static final Pattern ZONE = Pattern.compile(ZONE_NAME, IGNORING_CASE);

	/** A list of names followed by time, time zone or timezone, singular or plural. */
	private static final Pattern NAMED_ZONES = Pattern.compile("\\b(" + ZONE_NAME
			+ "(?:\\s*(?:or\\b|and\\b|,|/|&)\\s*" + ZONE_NAME + ")*)\\s+time(?:\\s*zones?)?\\b", IGNORING_CASE);

	/** That the person lives or works within a window, as a timezone requirement says. */
	private static final Pattern LOCATED = Pattern.compile("\\b(?:live|lives|living|located|based|reside|residing|"
			+ "working from|work from|within|be in|require|requires|required|must|in a time ?zone|"
			+ "time ?zones? (?:between|from|within|of))\\b", IGNORING_CASE);

	/**
	 * That a window is a schedule, hours to overlap, attend or be available in, or no requirement at all: a
	 * preference, or where the team works.
	 */
	private static final Pattern SCHEDULE = Pattern.compile("\\b(?:overlap\\w*|hours?|shifts?|meetings?|on-call|"
			+ "availability|available|collaborat\\w*|attend|prefer\\w*|team)\\b", IGNORING_CASE);

	/** An offer of visa sponsorship. */
	private static final Pattern VISA_SPONSORSHIP = Pattern.compile("\\b(?:visa|immigration|h-?1b)\\s+sponsorship\\s+"
			+ "(?:is\\s+|will\\s+be\\s+|may\\s+be\\s+|can\\s+be\\s+)?(?:available|offered|provided|possible|supported)\\b|"
			+ "\\b(?:we|will|can|do)\\s+(?:also\\s+)?(?:offer\\s+|provide\\s+|support\\s+)?(?:visa\\s+)?sponsor(?:ship)?\\s+"
			+ "(?:for\\s+)?(?:visas?|work\\s+permits?|h-?1b)\\b|\\boffer(?:s|ing)?\\s+(?:\\w+\\s+){0,2}?visa\\s+sponsorship\\b|"
			+ "\\bsponsorship\\s+available\\b", IGNORING_CASE);

	/** An offer of relocation. */
	private static final Pattern RELOCATION = Pattern.compile("\\brelocation\\s+(?:(?:assistance|support|package|bonus|"
			+ "help|benefits?|stipend|allowance)\\s+)?(?:is\\s+|will\\s+be\\s+|may\\s+be\\s+|can\\s+be\\s+)?(?:available|"
			+ "offered|provided|included|supported)\\b|\\b(?:offer|offers|offering|provide|provides|support|supports|"
			+ "help\\s+with|cover|covers|assist\\s+with)\\s+(?:\\w+\\s+){0,2}?relocation\\b", IGNORING_CASE);

	/** Where a segment is cut into sentences, each of which an offer is negated in or not. */
	private static final Pattern SENTENCES = Pattern.compile("(?<=[.!?])\\s+");

	/** Where a segment is cut into sentences and semicolon clauses, keeping the dots of U.S. inside its sentence. */
	private static final Pattern CLAUSES = Pattern.compile("(?<=[.!?;])(?<!\\b[A-Za-z]\\.)\\s+");

	/**
	 * The words one of the statements or offers needs, lowercased: a segment holding none of them states
	 * nothing.
	 */
	private static final List<String> STATING_WORDS = List.of("based", "located", "resid", "live", "living", "open",
			"available", "limited", "restricted", "anywhere", "hire", "hiring", "employ", "remote", "only", "to work",
			"legally work", "work authori", "work permit", "work visa", "work rights", "citizen", "person", "clearance",
			"secret", "sci", "polygraph", "utc", "gmt", "sponsor", "relocation", "recruit", "join", "eor", "location",
			"ll work", "will work", "ll be work", "will be work", "time");

	/** A label's answer of no, after its colon: {@code CLEARANCE REQUIRED FOR START: No}. */
	private static final Pattern ANSWERED_NO = Pattern.compile(":\\s*(?:no|none|n/?a|not required)\\W*$", IGNORING_CASE);

	/** A segment that answers the label before it with no. */
	private static final Pattern NO = Pattern.compile("\\s*(?:no|none|n/?a|not required)\\W*", IGNORING_CASE);

	/** A segment that answers the label before it with yes. */
	private static final Pattern YES = Pattern.compile("^\\s*yes\\b", IGNORING_CASE);

	/** How far before a statement a negation or a condition is looked for, in characters. */
	private static final int BEFORE = 50;

	/** How long the words naming a statement's place may be, in characters. */
	private static final int LONGEST_PLACE = 100;

	/** The work modes, the one that excludes the fewest people first. */
	private static final List<WorkModeEnum> MOST_OPEN_FIRST = List.of(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID,
			WorkModeEnum.ONSITE);

	private final PlaceRule places;

	private final WorkModeRule workModes;

	/**
	 * @param places the rule that reads the places a text names
	 * @param workModes the rule whose work-mode words a location string's own mode is read by
	 */
	public CountrySetRule(PlaceRule places, WorkModeRule workModes) {
		this.places = places;
		this.workModes = workModes;
	}

	/**
	 * Reads one vacancy's country set.
	 * @param vacancy the vacancy, with its title, declared location and description
	 * @return every set of places it states, and whether it offers visa sponsorship or relocation
	 */
	public CountrySet read(CountrySetVacancy vacancy) {
		List<StatedSet> stated = new ArrayList<>();
		for (LocationPlaces location : vacancy.locations()) {
			options(location, vacancy.workModes(), stated);
		}
		title(vacancy, stated);
		boolean visaSponsorship = false;
		boolean relocation = false;
		Set<List<Object>> read = new HashSet<>();
		List<Segment> segments = vacancy.segments();
		Set<String> near = optionCountries(vacancy);
		for (int i = 0; i < segments.size(); i++) {
			Segment segment = segments.get(i);
			if (!mayState(segment.text())) {
				continue;
			}
			String next = (i + 1 < segments.size()) ? segments.get(i + 1).text() : null;
			// Preferences and perks state nothing unless the perk's text names hiring or the person's location.
			List<StatedSet> statements = (segment.under() != null && (PREFERRED_HEADING.matcher(segment.under()).find()
					|| PERK_HEADING.matcher(segment.under()).find() && !PERK_CONTEXT.matcher(segment.text()).find()))
					? List.of() : statements(segment.text(), next, i, vacancy, near);
			for (StatedSet set : statements) {
				List<Object> key = new ArrayList<>(List.of(set.kind(), set.places(), String.valueOf(set.utcFrom()),
						String.valueOf(set.utcTo())));
				// Equal windows in different statements still belong to separate requirements.
				if (set.kind() == StatedSetKindEnum.TIMEZONE) {
					key.add(set.segment());
					key.add(set.text());
				}
				if (read.add(key)) {
					stated.add(set);
				}
			}
			visaSponsorship |= offers(segment.text(), VISA_SPONSORSHIP);
			relocation |= offers(segment.text(), RELOCATION);
		}
		List<StatedSet> positioned = new ArrayList<>(stated.size());
		for (int i = 0; i < stated.size(); i++) {
			StatedSet set = stated.get(i);
			positioned.add(new StatedSet(i, set.source(), set.kind(), set.workMode(), set.segment(), set.text(),
					set.places(), set.utcFrom(), set.utcTo()));
		}
		return new CountrySet(positioned, visaSponsorship, relocation);
	}

	/**
	 * The options one location string offers, each with its own work mode. A string naming no mode is an option
	 * of each mode the vacancy offers, so that its eligibility is the best of them (ADR-0027).
	 */
	private void options(LocationPlaces location, Set<WorkModeEnum> declared, List<StatedSet> stated) {
		Set<WorkModeEnum> named = this.workModes.modesNamedIn(location.location());
		if (named.contains(WorkModeEnum.REMOTE) && named.size() > 1) {
			for (String option : OPTIONS.split(location.location())) {
				if (!option.isBlank()) {
					stated.add(option(option.strip(), modeOf(this.workModes.modesNamedIn(option), null),
							this.places.read(option, null).places()));
				}
			}
			return;
		}
		if (!named.isEmpty() || declared.isEmpty()) {
			stated.add(option(location.location(), modeOf(named, null), location.places()));
			return;
		}
		for (WorkModeEnum mode : MOST_OPEN_FIRST) {
			if (declared.contains(mode)) {
				stated.add(option(location.location(), mode, location.places()));
			}
		}
	}

	private static StatedSet option(String text, WorkModeEnum workMode, List<Place> places) {
		return new StatedSet(0, StatedSetSourceEnum.LOCATION, kindOf(places), workMode, null, text, places, null, null);
	}

	/** The mode of an option naming these modes: remote where it is among them, then hybrid, then onsite. */
	private static WorkModeEnum modeOf(Set<WorkModeEnum> named, WorkModeEnum otherwise) {
		for (WorkModeEnum mode : MOST_OPEN_FIRST) {
			if (named.contains(mode)) {
				return mode;
			}
		}
		return otherwise;
	}

	/** The sets the title states: the regions and countries a part of it names, and a clearance. */
	private void title(CountrySetVacancy vacancy, List<StatedSet> stated) {
		String title = vacancy.title();
		if (title == null || title.isBlank()) {
			return;
		}
		Set<Place> named = new LinkedHashSet<>();
		for (String part : TITLE_PARTS.split(title)) {
			if (namesOtherCodes(part)) {
				continue;
			}
			List<Place> places = placesNamedOnly(part);
			if (!places.isEmpty() && places.stream()
				.allMatch((place) -> place.kind() == PlaceKindEnum.REGION || place.kind() == PlaceKindEnum.COUNTRY)) {
				named.addAll(places);
			}
		}
		if (!named.isEmpty()) {
			List<Place> places = List.copyOf(named);
			stated.add(new StatedSet(0, StatedSetSourceEnum.TITLE, kindOf(places), null, null, title, places, null,
					null));
		}
		if (US_CLEARANCE.matcher(title).find() || TITLE_CLEARANCE.matcher(title).find()) {
			stated.add(new StatedSet(0, StatedSetSourceEnum.TITLE, StatedSetKindEnum.CLEARANCE, null, null, title,
					List.of(Place.country(clearanceCountry(title, vacancy))), null, null));
		}
	}

	/**
	 * Whether a segment holds a word some statement or offer needs, so that the rest, most of any description,
	 * is not read pattern by pattern.
	 */
	private static boolean mayState(String text) {
		String lowered = text.toLowerCase(Locale.ROOT);
		for (String word : STATING_WORDS) {
			if (lowered.contains(word)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The sets one segment of the description states.
	 * @param text the segment's text
	 * @param next the next segment's text, which answers a label ({@code Government Clearance Required?}), null
	 * for the last
	 * @param segment the segment's index
	 * @param vacancy the vacancy it is of
	 * @param near the countries its options name, in which a name of several countries' places is read first
	 */
	private List<StatedSet> statements(String text, String next, int segment, CountrySetVacancy vacancy,
			Set<String> near) {
		List<StatedSet> stated = new ArrayList<>();
		for (Pattern statement : List.of(MUST_BE_IN, YOU_ARE_IN, ROLE_IN, YOU_WORK_IN, LOOKING_FOR_IN,
				LOCATION_LABEL, OPEN_TO, ANYWHERE_IN, HIRES_IN)) {
			if (statement == ANYWHERE_IN && BENEFIT.matcher(text).find()) {
				continue;
			}
			Matcher matcher = statement.matcher(text);
			while (matcher.find()) {
				if (statement == LOCATION_LABEL && (REMOTE_CHOICE.matcher(labelValue(text, matcher.end())).find()
						|| LOCATION_VERB.matcher(labelValue(text, matcher.end())).find())
						|| statement == ROLE_IN && NEGATION.matcher(matcher.group()).find()
						|| (statement == ROLE_IN || statement == LOOKING_FOR_IN) && TEAM.matcher(matcher.group()).find()
						|| (statement == HIRES_IN || statement == ANYWHERE_IN) && teamBefore(text, matcher.start())
						|| PREFERRED.matcher(matcher.group()).find()) {
					continue;
				}
				if (!negatedBefore(text, matcher.start())) {
					// Where you'll work names a place only where its object is one: not a complex enterprise domain.
					List<Place> places = hiringPlaces(text, matcher.start(), (statement == LOCATION_LABEL)
							? this.places.read(locationValue(text, matcher.end()), null, near).places()
							: (statement == YOU_WORK_IN) ? workPlaces(clauseAfter(text, matcher.end()), near)
									: placesAfter(text, matcher.end(), near));
					if (!places.isEmpty()) {
						stated.add(described(kindOf(places), segment, text, places));
					}
				}
			}
		}
		Matcher anywhere = FROM_ANYWHERE.matcher(text);
		while (anywhere.find()) {
			if (!negatedBefore(text, anywhere.start()) && !BENEFIT.matcher(text).find()
					&& !teamBefore(text, anywhere.start())) {
				worldwide(text, anywhere.start(), segment, stated);
			}
		}
		Matcher global = GLOBAL_HIRING.matcher(text);
		while (global.find()) {
			if (!negatedBefore(text, global.start())
					&& !WELCOMING_APPLICATIONS.matcher(sentenceAt(text, global.start())).find()) {
				stated.add(described(StatedSetKindEnum.REGION, segment, text, List.of(Place.region(WORLDWIDE))));
			}
		}
		Matcher employer = EMPLOYER_OF_RECORD.matcher(text);
		while (employer.find()) {
			// Those based elsewhere are hired only where the employer of record's own sentence says so, unnegated.
			String sentence = sentenceAt(text, employer.start());
			Matcher elsewhere = ELSEWHERE.matcher(sentence);
			while (elsewhere.find()) {
				if ((elsewhere.group(1) == null || HIRING_WORD.matcher(sentence).find())
						&& !NEGATION.matcher(sentence).find()) {
					worldwide(text, employer.start(), segment, stated);
					break;
				}
			}
		}
		Matcher only = PLACE_ONLY.matcher(text);
		while (only.find()) {
			// A city a vacancy is for only is an office it names, which the declared location gives.
			List<Place> places = namesOtherCodes(only.group(1)) ? List.of()
					: placesNamedOnly(only.group(1)).stream().filter((place) -> place.kind() != PlaceKindEnum.CITY).toList();
			places = hiringPlaces(text, only.start(), places);
			if (!places.isEmpty()) {
				stated.add(described(kindOf(places), segment, text, places));
			}
		}
		boolean required = REQUIRED.matcher(text).find() && !CONDITION.matcher(text).find();
		if (required) {
			authorization(text, segment, near, stated);
			if (!NON_DISCRIMINATION.matcher(text).find()) {
				citizenship(text, segment, near, stated);
			}
			Matcher clearance = CLEARANCE.matcher(text);
			while (clearance.find()) {
				String sentence = sentenceAt(text, clearance.start());
				if (!negatedBefore(text, clearance.start())
						&& !LEGAL_NOTICE.matcher(sentence).find() && !PREFERRED.matcher(sentence).find()
						&& answeredYes(text, next)) {
					stated.add(described(StatedSetKindEnum.CLEARANCE, segment, text,
							List.of(Place.country(clearanceCountry(clearancePhrase(text, clearance), vacancy)))));
				}
			}
		}
		timezone(text, segment, stated);
		return stated;
	}

	/** States the worldwide set at a statement, where its sentence says the company hires or the person is there. */
	private static void worldwide(String text, int at, int segment, List<StatedSet> stated) {
		List<Place> places = hiringPlaces(text, at, List.of(Place.region(WORLDWIDE)));
		if (!places.isEmpty()) {
			stated.add(described(kindOf(places), segment, text, places));
		}
	}

	/** Removes Worldwide from application welcomes, or without hiring or the person's location in its sentence. */
	private static List<Place> hiringPlaces(String text, int at, List<Place> places) {
		String sentence = sentenceAt(text, at);
		if (WORLDWIDE_CONTEXT.matcher(sentence).find() && !WELCOMING_APPLICATIONS.matcher(sentence).find()) {
			return places;
		}
		return places.stream().filter((place) -> !Place.region(WORLDWIDE).equals(place)).toList();
	}

	/** The rest of the sentence, or of the clause after a semicolon, from a position on. */
	private static String restOfSentence(String text, int from) {
		Matcher boundary = CLAUSES.matcher(text);
		return boundary.find(from) ? text.substring(from, boundary.start()) : text.substring(from);
	}

	/** The sentence of a segment, or the clause after a semicolon, that a position lies in. */
	private static String sentenceAt(String text, int at) {
		int start = 0;
		int end = text.length();
		Matcher boundary = CLAUSES.matcher(text);
		while (boundary.find()) {
			if (boundary.start() >= at) {
				end = boundary.start();
				break;
			}
			start = boundary.end();
		}
		return text.substring(start, end);
	}

	private void authorization(String text, int segment, Set<String> near, List<StatedSet> stated) {
		Matcher after = AUTHORIZED_IN.matcher(text);
		while (after.find()) {
			if (!negatedBefore(text, after.start())) {
				List<Place> places = countries(placesAfter(text, after.end(), near));
				if (!places.isEmpty()) {
					stated.add(described(StatedSetKindEnum.AUTHORIZATION, segment, text, places));
				}
			}
		}
		Matcher before = PLACE_AUTHORIZATION.matcher(text);
		while (before.find()) {
			List<Place> places = countries(placesNamedOnly(before.group(1)));
			if (!places.isEmpty() && !negatedBefore(text, before.start())) {
				stated.add(described(StatedSetKindEnum.AUTHORIZATION, segment, text, places));
			}
		}
	}

	/** Reads required citizenship, except where an export alternative admits noncitizens. */
	private void citizenship(String text, int segment, Set<String> near, List<StatedSet> stated) {
		Matcher before = PLACE_CITIZENSHIP.matcher(text);
		while (before.find()) {
			String demonym = DEMONYMS.get(before.group(1).toLowerCase(Locale.ROOT));
			List<Place> places = (demonym != null) ? List.of(Place.country(demonym))
					: countries(placesNamedOnly(before.group(1)));
			if (!places.isEmpty() && !negatedBefore(text, before.start())
					&& !EXPORT_ALTERNATIVE.matcher(restOfSentence(text, before.end())).find()) {
				stated.add(described(StatedSetKindEnum.CITIZENSHIP, segment, text, places));
			}
		}
		Matcher after = CITIZENS_OF.matcher(text);
		while (after.find()) {
			List<Place> places = countries(placesAfter(text, after.end(), near));
			if (!places.isEmpty() && !negatedBefore(text, after.start())
					&& !EXPORT_ALTERNATIVE.matcher(restOfSentence(text, after.end())).find()) {
				stated.add(described(StatedSetKindEnum.CITIZENSHIP, segment, text, places));
			}
		}
	}

	/**
	 * Whether a requirement is answered yes where it is a label: one ending in a question mark or a colon is
	 * answered by the next segment, with a yes or what it requires; one answered {@code No} or {@code None},
	 * after its colon or in the next segment, requires nothing.
	 */
	private static boolean answeredYes(String text, String next) {
		if (ANSWERED_NO.matcher(text).find()) {
			return false;
		}
		String label = text.strip();
		if (label.endsWith("?") || label.endsWith(":")) {
			return next != null && (YES.matcher(next).find() || CLEARANCE.matcher(next).find());
		}
		return next == null || !NO.matcher(next).matches();
	}

	/** The noun phrase holding one clearance match, or the match itself where no clearance noun follows it. */
	private static String clearancePhrase(String text, Matcher clearance) {
		Matcher phrase = CLEARANCE_PHRASE.matcher(text);
		while (phrase.find()) {
			if (phrase.start() <= clearance.start() && phrase.end() >= clearance.end()) {
				return phrase.group();
			}
		}
		return clearance.group();
	}

	/** Reads numeric windows and required named zones; a numeric window already supplies its sentence's offsets. */
	private static void timezone(String text, int segment, List<StatedSet> stated) {
		Matcher window = WINDOW.matcher(text);
		while (window.find()) {
			String before = text.substring(0, window.start());
			// What the window is about is said in its own sentence, up to the window itself.
			int sentence = Math.max(before.lastIndexOf(". "), before.lastIndexOf("; ")) + 1;
			if (LOCATED.matcher(before).find() && !SCHEDULE.matcher(text.substring(sentence, window.end())).find()) {
				stated.add(new StatedSet(0, StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.TIMEZONE, null, segment,
						text, List.of(), offset(window, 1), offset(window, 4)));
			}
		}
		Matcher named = NAMED_ZONES.matcher(text);
		while (named.find()) {
			String sentence = sentenceAt(text, named.start());
			String before = sentenceAt(text.substring(0, named.start()), named.start());
			if (!LOCATED.matcher(before).find() || SCHEDULE.matcher(before + named.group()).find()
					|| negatedBefore(text, named.start()) || WINDOW.matcher(sentence).find()) {
				continue;
			}
			Matcher zone = ZONE.matcher(named.group(1));
			while (zone.find()) {
				List<Integer> offsets = switch (zone.group().toLowerCase(Locale.ROOT)) {
					case "apac", "asia-pacific", "asia pacific" -> List.of(300, 780);
					case "european", "europe", "eu" -> List.of(-60, 180);
					case "central european", "cet" -> List.of(60, 60);
					case "us", "u.s.", "united states" -> List.of(-600, -300);
					case "us eastern", "eastern", "et", "est", "new york" -> List.of(-300, -300);
					case "us central", "central", "ct", "cst", "chicago" -> List.of(-360, -360);
					case "us mountain", "mountain", "mt", "mst", "denver" -> List.of(-420, -420);
					default -> List.of(-480, -480);
				};
				List<Place> places = offsets.get(1) < 0 ? List.of(Place.country(UNITED_STATES)) : List.of();
				stated.add(new StatedSet(0, StatedSetSourceEnum.DESCRIPTION, StatedSetKindEnum.TIMEZONE, null, segment,
						sentence, places, offsets.get(0), offsets.get(1)));
			}
		}
	}

	/** The offset in minutes a window's matcher read from the groups starting at one, 0 for a bare UTC. */
	private static int offset(Matcher window, int group) {
		if (window.group(group) == null) {
			return 0;
		}
		int minutes = Integer.parseInt(window.group(group + 1)) * 60
				+ ((window.group(group + 2) != null) ? Integer.parseInt(window.group(group + 2)) : 0);
		return window.group(group).equals("+") ? minutes : -minutes;
	}

	/** Whether a team stands before a statement in its own clause, bounded by a dot, semicolon, comma or colon. */
	private static boolean teamBefore(String text, int at) {
		int start = at;
		while (start > 0 && ".;,:".indexOf(text.charAt(start - 1)) < 0) {
			start--;
		}
		return TEAM.matcher(text.substring(start, at)).find();
	}

	/** Whether a negation or a condition stands in the same sentence shortly before a statement. */
	private static boolean negatedBefore(String text, int at) {
		String before = text.substring(Math.max(0, at - BEFORE), at);
		int sentence = Math.max(before.lastIndexOf(". "), Math.max(before.lastIndexOf("; "), before.lastIndexOf(": ")));
		String clause = (sentence >= 0) ? before.substring(sentence + 2) : before;
		return NEGATION.matcher(clause).find() || CONDITION.matcher(clause).find();
	}

	/**
	 * The places the words after a statement name, up to the end of its clause, a name of several countries'
	 * places read in one of those near first.
	 */
	private List<Place> placesAfter(String text, int from, Set<String> near) {
		String clause = clauseAfter(text, from);
		if (VAGUE_COUNTRIES.matcher(clause).find()) {
			return List.of();
		}
		if (clause.strip().toLowerCase(Locale.ROOT).matches("(?:the\\s+)?world\\b.*")) {
			return List.of(Place.region(WORLDWIDE));
		}
		return this.places.read(clause, null, near).places();
	}

	/** The places of a label's value: its words up to headquarters, which name the company's place. */
	private static String locationValue(String text, int from) {
		String value = labelValue(text, from);
		Matcher headquarters = HEADQUARTERS.matcher(value);
		return headquarters.find() ? value.substring(0, headquarters.start()) : value;
	}

	/** A label's value across brackets and semicolons, up to a newline or a sentence's end. */
	private static String labelValue(String text, int from) {
		StringBuilder words = new StringBuilder();
		for (int i = from; i < text.length() && words.length() < LONGEST_PLACE * 2; i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '.' && !(i >= 1 && Character.isLetter(text.charAt(i - 1))
					&& (i < 2 || !Character.isLetter(text.charAt(i - 2))))) {
				break;
			}
			words.append(c);
		}
		return words.toString();
	}

	/** The words after a statement, up to the end of its clause. */
	private static String clauseAfter(String text, int from) {
		StringBuilder words = new StringBuilder();
		for (int i = from; i < text.length() && words.length() < LONGEST_PLACE; i++) {
			char c = text.charAt(i);
			if (c == ';' || c == ':' || c == '!' || c == '?' || c == '(' || c == ')' || c == '\n') {
				break;
			}
			// The dot of an abbreviation (U.S., D.C.) does not end the clause.
			if (c == '.' && !(i >= 1 && Character.isLetter(text.charAt(i - 1))
					&& (i < 2 || !Character.isLetter(text.charAt(i - 2))))) {
				break;
			}
			words.append(c);
		}
		Matcher end = CLAUSE_END.matcher(words);
		return end.find() ? words.substring(0, end.start()) : words.toString();
	}

	/**
	 * The places a clause names where every word of it is a place, a town the list does not hold, capitalized, or
	 * an office's: {@code Hyvinkää, Finland}, {@code our Vilnius office}.
	 */
	private List<Place> workPlaces(String clause, Set<String> near) {
		PlaceReading reading = this.places.read(clause, null, near);
		boolean onlyPlaces = reading.unread().stream()
			.allMatch((word) -> Character.isUpperCase(word.charAt(0))
					|| BESIDE_WORK_PLACES.contains(word.toLowerCase(Locale.ROOT)));
		return onlyPlaces ? reading.places() : List.of();
	}

	/** The countries the vacancy's declared options name. */
	private static Set<String> optionCountries(CountrySetVacancy vacancy) {
		Set<String> countries = new HashSet<>();
		for (LocationPlaces location : vacancy.locations()) {
			for (Place place : location.places()) {
				if (place.country() != null) {
					countries.add(place.country());
				}
			}
		}
		return countries;
	}

	/** The places some words name, where every word of them is read as a place or a word beside one. */
	private List<Place> placesNamedOnly(String words) {
		PlaceReading reading = this.places.read(words, null);
		boolean onlyPlaces = reading.unread().stream()
			.allMatch((word) -> BESIDE_PLACES.contains(word.toLowerCase(Locale.ROOT)));
		return onlyPlaces ? reading.places() : List.of();
	}

	/** The countries of some places: a requirement of authorization or citizenship is a country's. */
	private static List<Place> countries(List<Place> places) {
		Set<Place> countries = new LinkedHashSet<>();
		for (Place place : places) {
			if (place.country() != null) {
				countries.add(Place.country(place.country()));
			}
		}
		return List.copyOf(countries);
	}

	/** Whether a segment offers what a pattern names, in a sentence that does not negate it. */
	private static boolean offers(String text, Pattern offer) {
		for (String sentence : SENTENCES.split(text)) {
			if (offer.matcher(sentence).find() && !NEGATION.matcher(sentence).find()) {
				return true;
			}
		}
		return false;
	}

	private static StatedSet described(StatedSetKindEnum kind, int segment, String text, List<Place> places) {
		return new StatedSet(0, StatedSetSourceEnum.DESCRIPTION, kind, null, segment, text, places, null, null);
	}

	/** Whether a text holds a code that names no place in a title. */
	private static boolean namesOtherCodes(String text) {
		Matcher code = CODE.matcher(text);
		while (code.find()) {
			if (!PLACE_CODES.contains(code.group())) {
				return true;
			}
		}
		return false;
	}

	/** The country a clearance the text names is of. */
	private String clearanceCountry(String text, CountrySetVacancy vacancy) {
		if (US_CLEARANCE.matcher(text).find()) {
			return UNITED_STATES;
		}
		Matcher phrase = CLEARANCE_PHRASE.matcher(text);
		if (phrase.find()) {
			String words = phrase.group();
			Matcher demonym = DEMONYM.matcher(words);
			if (demonym.find()) {
				return DEMONYMS.get(demonym.group().toLowerCase(Locale.ROOT));
			}
			// A scheme such as SC names a clearance here, rather than a country with the same code.
			String countryWords = GB_CLEARANCE.matcher(AU_CLEARANCE.matcher(words).replaceAll("")).replaceAll("");
			for (Place place : this.places.read(countryWords, null).places()) {
				if (place.kind() == PlaceKindEnum.COUNTRY) {
					return place.country();
				}
			}
			if (GB_CLEARANCE.matcher(words).find()) {
				return "GB";
			}
			if (AU_CLEARANCE.matcher(words).find()) {
				return "AU";
			}
		}
		Set<String> countries = optionCountries(vacancy);
		return (countries.size() == 1) ? countries.iterator().next() : UNITED_STATES;
	}

	private static StatedSetKindEnum kindOf(List<Place> places) {
		return places.stream().anyMatch((place) -> place.kind() == PlaceKindEnum.REGION) ? StatedSetKindEnum.REGION
				: StatedSetKindEnum.LOCATION;
	}

}
