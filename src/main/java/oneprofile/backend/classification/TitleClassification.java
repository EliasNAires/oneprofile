package oneprofile.backend.classification;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import oneprofile.backend.normalizedvacancy.Classification;
import oneprofile.backend.normalizedvacancy.UnknownReason;

/**
 * Decides whether a cleaned title names an engineering role. The criterion it answers to is
 * {@code docs/engineering-role-criterion.md}: that document is judgement and this class is
 * mechanism, so where the two disagree the document is right and these lists are wrong.
 * <p>
 * A title is read as a <b>function head</b> — the noun naming what the role does — with
 * <b>modifiers</b> naming the domain it does it in. Where a title names more than one head the
 * first is the head, and a head is what keeps the modifier lists honest: {@code security} is a
 * software qualifier, so {@code security analyst} is in while {@code security guard} is out on a
 * head that is never engineering whatever modifies it.
 * <p>
 * Four lists and nothing else. Heads carry the two attributes the criterion gives them; the
 * never-engineering heads are held apart because neither attribute means anything for them.
 * Qualifiers and markers are modifiers, read only through the head. Rulings are decisions on a
 * phrase and override the whole procedure, which is why they are a layer above it rather than
 * entries in a list.
 * <p>
 * The whole of it is whole-word, case-insensitive matching over the cleaned title, which is the
 * cost the criterion allows: no description bodies and no network, so that resolving a hard title
 * by reading its description stays out of the path every vacancy travels.
 */
public class TitleClassification {

	/** Whether a head's modifier settles what the role is, because the function lives in a domain. */
	private enum Domain {

		/** {@code engineer} names a function embedded in a domain: a civil engineer is not a software job. */
		BOUND,

		/** {@code analyst} operates on information about a domain, so the modifier settles nothing. */
		FREE

	}

	/**
	 * One function head: whether its modifier settles what the role is, and whether a software
	 * domain under it can produce an engineering role at all.
	 */
	private record Head(Domain domain, boolean engineeringCapable) {
	}

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private static final Head BOUND_CAPABLE = new Head(Domain.BOUND, true);

	private static final Head BOUND_INCAPABLE = new Head(Domain.BOUND, false);

	private static final Head FREE_CAPABLE = new Head(Domain.FREE, true);

	private static final Map<String, Head> HEADS = Map.ofEntries(Map.entry("engineer", BOUND_CAPABLE),
			Map.entry("developer", BOUND_CAPABLE), Map.entry("administrator", BOUND_CAPABLE),
			Map.entry("programmer", BOUND_CAPABLE), Map.entry("analyst", FREE_CAPABLE),
			Map.entry("architect", FREE_CAPABLE), Map.entry("scientist", FREE_CAPABLE),
			Map.entry("researcher", FREE_CAPABLE), Map.entry("specialist", BOUND_CAPABLE),
			Map.entry("lead", BOUND_CAPABLE), Map.entry("engineering", BOUND_CAPABLE),
			Map.entry("manager", BOUND_CAPABLE), Map.entry("fellow", BOUND_CAPABLE), Map.entry("associate", BOUND_CAPABLE),
			Map.entry("writer", BOUND_INCAPABLE), Map.entry("master", BOUND_INCAPABLE),
			// Heads whose modifier carries the whole of the domain: an intern, a leader, a partner,
			// an advisor and a strategist are all named by the thing they are one of.
			Map.entry("intern", BOUND_CAPABLE), Map.entry("leader", BOUND_CAPABLE),
			Map.entry("partner", BOUND_CAPABLE), Map.entry("advisor", BOUND_CAPABLE),
			Map.entry("strategist", BOUND_CAPABLE),
			// The heads a title gives someone who runs the work rather than doing it. Iterations 2,
			// 3 and 4 each left director off by the same argument — it is named before the head that
			// would have decided the title, so it masks it — and iteration 5 measured the argument
			// instead of repeating it. Over the 4000 rows four samples have labelled, the five of
			// them together decide 56 more titles out and 3 more in, and cost one false accept and no
			// misses at all; what that did to the corpus is in the iteration 5 report. The masking is
			// real and it is priced: "Head Chef" and "Director Securities Corporate Counsel" are
			// domain ambiguity now where the chef and the counsel used to decide them out.
			Map.entry("director", BOUND_CAPABLE), Map.entry("directors", BOUND_CAPABLE),
			Map.entry("head", BOUND_CAPABLE), Map.entry("president", BOUND_CAPABLE),
			Map.entry("expert", BOUND_CAPABLE),
			// The plurals a posting that hires more than one person writes. Spelled out one by one
			// rather than read off a trailing s, because the one plural this corpus needed kept off
			// the list — agents, against the never-engineering agent — is exactly what a rule that
			// strips the s would get wrong.
			Map.entry("engineers", BOUND_CAPABLE), Map.entry("developers", BOUND_CAPABLE),
			Map.entry("programmers", BOUND_CAPABLE), Map.entry("managers", BOUND_CAPABLE),
			Map.entry("specialists", BOUND_CAPABLE), Map.entry("leaders", BOUND_CAPABLE),
			Map.entry("analysts", FREE_CAPABLE), Map.entry("scientists", FREE_CAPABLE),
			Map.entry("architects", FREE_CAPABLE), Map.entry("consultants", BOUND_CAPABLE),
			// A consultant operates on information about a domain, so its modifier settles
			// nothing — and a software domain under it is an engineering role like any other.
			Map.entry("consultant", BOUND_CAPABLE),
			// The Spanish and Portuguese spellings of the same heads. The qualifier and marker
			// lists stay English: the words this corpus uses for a software domain — software,
			// data, cloud, backend — are the same in all three languages.
			Map.entry("ingeniero", BOUND_CAPABLE), Map.entry("ingeniera", BOUND_CAPABLE),
			Map.entry("engenheiro", BOUND_CAPABLE), Map.entry("engenheira", BOUND_CAPABLE),
			Map.entry("desarrollador", BOUND_CAPABLE), Map.entry("desarrolladora", BOUND_CAPABLE),
			Map.entry("desenvolvedor", BOUND_CAPABLE), Map.entry("desenvolvedora", BOUND_CAPABLE),
			Map.entry("programador", BOUND_CAPABLE), Map.entry("programadora", BOUND_CAPABLE),
			Map.entry("analista", FREE_CAPABLE), Map.entry("arquitecto", FREE_CAPABLE),
			Map.entry("arquiteto", FREE_CAPABLE), Map.entry("consultor", FREE_CAPABLE),
			Map.entry("consultora", FREE_CAPABLE), Map.entry("desenvolvimento", BOUND_CAPABLE), Map.entry("développeur", BOUND_CAPABLE),
			Map.entry("développeuse", BOUND_CAPABLE),
			Map.entry("desarrollo", BOUND_CAPABLE),
			// The heads this corpus writes short. vp, avp and mgr are the vice president and the
			// manager abbreviated, and they read their modifier the same way those do.
			Map.entry("vp", BOUND_CAPABLE), Map.entry("avp", BOUND_CAPABLE), Map.entry("mgr", BOUND_CAPABLE),
			Map.entry("dba", BOUND_CAPABLE), Map.entry("engg", BOUND_CAPABLE),
			// A student posting is a vacancy and its modifier carries the domain, the way the
			// intern's does. The German spelling is on the list because this corpus repeats it.
			Map.entry("internship", BOUND_CAPABLE), Map.entry("externship", BOUND_CAPABLE),
			Map.entry("extern", BOUND_CAPABLE), Map.entry("werkstudent", BOUND_CAPABLE));

	/**
	 * The heads that decide out whatever modifies them, and so carry neither attribute. Each one
	 * names a function that no software domain turns into engineering work: there is no software
	 * veterinarian and no software bartender.
	 */
	private static final Set<String> NEVER_ENGINEERING = Set.of("trainer", "assistant", "executive", "nurse",
			"representative", "coordinator", "teacher", "driver", "veterinarian", "physician", "psychologist",
			"psychiatrist", "pharmacist", "dentist", "surgeon", "therapist", "dietitian", "practitioner", "paramedic",
			"midwife", "caregiver", "pathologist", "radiographer", "sonographer", "attorney", "paralegal", "counsel",
			"counselor", "recruiter", "accountant", "bookkeeper", "auditor", "cashier", "clerk", "receptionist",
			"secretary", "janitor", "housekeeper", "chef", "cook", "bartender", "barista", "waiter", "host", "stylist",
			"barber", "esthetician", "merchandiser", "installer", "plumber", "mechanic", "welder", "machinist",
			"carpenter", "painter", "roofer", "landscaper", "guard", "firefighter", "instructor", "tutor", "professor",
			"librarian", "translator", "interpreter", "linguist", "editor", "artist", "animator", "illustrator",
			"photographer", "videographer", "copywriter", "journalist", "salesperson", "seller", "ambassador",
			"volunteer", "supervisor", "operator", "foreman", "superintendent", "dispatcher", "courier", "chaplain",
			"electrician", "anesthesiologist", "gastroenterologist", "histopathologist", "neurologist", "podiatrist",
			"psychotherapist", "chemist", "millwright", "laborer", "custodian", "estimator", "buyer", "producer",
			"compositor", "generalist", "inspector", "planner", "paraprofessional", "agent",
			// Four samples running have labelled every technician and every designer out and not one
			// of them in. Both were heads that carried no engineering work, which left the whole of
			// two large families unruled rather than decided; the IT technician, the one reading of
			// either that a labeller called open, is a ruling instead.
			"technician", "technicians", "technologist", "designer", "designers",
			// The rest of the functions the unruled pile held, and the plurals the corpus writes.
			// The officer needs the rulings below it: a chief information officer runs the
			// information and not the building.
			"worker", "workers", "professional", "professionals", "officer", "officers", "collector", "coach",
			"porter", "dishwasher", "handler", "sourcer", "broker", "clinician", "physiotherapist", "dermatologist",
			"oncologist", "cardiologist", "cardiologists", "aesthetician", "reporter", "educator", "merchant",
			"advocate", "scheduler", "employee", "surveyor", "helper", "helpers", "caregivers",
			// The Spanish, Portuguese, French, German and Dutch spellings of the same functions.
			"aide", "agente", "auxiliar", "auxiliaire", "assistante", "medewerker", "berater", "vendedor", "vendedora",
			"vendeur", "vendeuse", "operador", "operadora", "enfermero", "enfermera", "enfermeiro", "enfermeira",
			"provider", "navigator", "banker", "attendant", "fabricator", "fitter", "physiologist", "optometrist",
			"endocrinologist", "sorter", "mixer", "babysitter", "apprentice", "firefighters", "pathologists",
			"personalberater", "verkoopmedewerker", "superviseur", "directeur", "ejecutivo", "executivo",
			"produktionsleiter",
			// The professions iteration 7's unruled pile named, and the officers a title writes as
			// three letters. A chief of finance, marketing, people or operations is never an
			// engineering role; the chiefs that are — the technology and information officers — are
			// rulings, and a ruling runs first. gm says what the general manager ruling says.
			"pediatrician", "physiatrist", "psychotherapists", "registrar", "crna", "dvm", "orthotist",
			"hygienist", "sommelier", "polisher", "juicer", "usher", "concierge", "interpreters", "cleaner",
			"negotiator", "trader", "evaluator", "grader", "scriptwriter", "interventionist", "rodman",
			"sonographers", "cfo", "cmo", "chro", "coo", "sdr", "gm");

	/**
	 * A software domain. One of these under an engineering-capable head decides in, and it decides
	 * nothing at all on its own — which is what keeps {@code security guard} off the list.
	 * <p>
	 * {@code solutions} is deliberately absent: every industry sells solutions, so the word under a
	 * head decided {@code Workplace Solutions Manager} in. The solutions roles that are engineering
	 * roles are named by ruling instead.
	 */
	private static final Set<String> SOFTWARE_QUALIFIERS = Set.of("software", "backend", "back end", "frontend",
			"front end", "fullstack", "full stack", "data", "platform", "devops", "sre", "site reliability", "security",
			"mobile", "ios", "android", "cloud", "infrastructure", "qa", "quality assurance", "test", "automation",
			"machine learning", "deep learning", "ml", "ai", "systems", "network", "web", "api", "embedded",
			"application", "integration", "database", "firmware", "compiler", "robotics",
			"it", "information technology", "computer", "computer science", "cybersecurity", "mlops", "java",
			"python", "javascript", "typescript", "salesforce", "sap", "azure", "aws", "kubernetes", "linux",
			"blockchain", "react", "sql", "saas", "algorithm", "algorithms", "middleware", "ux", "ui", "edi",
			"identity access management", "technical", "llm", "nlp", "computer vision", "generative ai",
			"informatics", "netsuite", "servicenow", "workday", "gameplay", "unreal", "quant", "quantitative",
			"rendering", "graphics", "detection engineering", "exploit", "streaming",
			"c++", "gis", "outsystems", "devsecops", "observability", "threat", "cyber",
			"applications", "storage", "bi", "technology",
			// The software domains iteration 7's unknown pile named by their own word.
			"incident response", "forensic", "dfir", "postgresql", "datapath", "model training");

	/**
	 * A function that exists identically outside software. Under a domain-bound head one of these
	 * decides out and beats a software qualifier; under a domain-free head they do nothing, which is
	 * why {@code audit analyst} and {@code payroll analyst} are not out.
	 * <p>
	 * The commercial domains are here for the same reason the physical ones are: a manager of an
	 * account, a territory or a hiring pipeline is not managing software. {@code sales} is among them
	 * because the one title it would have decided wrongly, Sales Engineer, is a ruling — and a ruling
	 * runs before step 4.
	 */
	private static final Set<String> OFF_DOMAIN_MARKERS = Set.of("civil", "structural", "mechanical", "chemical",
			"hvac", "plumbing", "electrician", "nurse", "nursing", "clinical", "patient", "pharmacy", "pharmaceutical",
			"restaurant", "retail", "store", "cashier", "driver", "warehouse", "forklift", "construction", "teacher",
			"tutor", "attorney", "legal", "paralegal", "accounting", "payroll", "audit", "tax", "janitor", "maintenance",
			"facilities", "manufacturing", "welder", "machinist", "automotive", "aerospace", "petroleum", "mining",
			"agriculture", "agricultural", "graphic", "marketing", "biomedical", "physical", "propulsion",
			"avionics", "spacecraft", "launch", "satellite", "power systems", "data center", "controls", "electronics",
			"plc", "turbomachinery", "combustion", "thermal", "hydraulic", "pneumatic", "cryogenic", "cryogenics",
			"industrial", "materials", "sales", "account", "accounts", "customer success", "finance", "financial", "investor",
			"commercial", "real estate", "insurance", "procurement", "supply chain", "logistics", "merchandising",
			"property", "talent", "recruiting", "recruitment", "human resources", "veterinary", "dental", "fitness",
			"hospitality", "culinary", "housekeeping", "events", "social media", "brand", "editorial",
			"public relations", "hr", "behavioral", "vehicle", "solar", "sanitation", "laboratory",
			"microbiology", "equipment", "weld", "welding", "hardware", "mep", "gas", "aviation", "energy", "grid",
			"geotechnical", "telecommunications", "door", "cable", "semiconductor", "wafer", "rfic",
			"silicon engineering", "laser", "space systems", "ew", "brakes", "steering", "fastener", "mechatronics",
			"battery", "analog", "architectural", "compliance", "fashion", "wealth", "investment",
			"business development", "renewals", "territory", "purchasing", "sourcing", "contracts", "licensing",
			"community", "customer care", "customer experience",
			"apparel", "emc", "rf", "partnerships", "channel", "channels", "pfas", "asset",
			"client", "clients", "leasing", "outreach", "campaign", "banking", "escrow", "staffing",
			"gear", "fluidic", "optomechanical", "roadway", "structures", "lunar", "nuclear", "mechanism",
			"flight", "business operations", "regulatory", "inventory", "beauty", "transformation",
			"transmission", "environmental", "transportation", "intake", "records", "print", "personal training",
			"yard", "relationship", "tour", "paid search", "seo", "programmatic", "operational excellence",
			"preconstruction", "medical", "mammography", "oncology", "pathology", "dialysis", "ward", "charity",
			"coach",
			// The domains iteration 7's domain-ambiguity pile named and no marker held. chef and
			// veterinarian are here as domains rather than heads: as heads they were masked by the
			// leadership and associate heads named before them, which is the cost iterations 5 and 6
			// pinned, and as markers they decide those titles out without giving the mask its power.
			"water", "low voltage", "private equity", "compensation", "benefits", "bookkeeping", "budget",
			"government affairs", "demand generation", "chef", "kitchen", "hospice", "wellness",
			"personal care", "kids", "farm", "shipping", "mine", "explosive", "gene", "immunology",
			"chemistry", "photonics", "renovations", "interiors", "housing", "telesales", "esg", "underground",
			"commissioning", "vfx", "speech", "case manager", "tiktok", "biometrics", "veterinarian",
			"anesthesia");

	/**
	 * The answers the procedure must reproduce. A ruling is a decision about a phrase, so it changes
	 * only by a decision and never as a side effect of a list edit.
	 * <p>
	 * An undecided ruling is always {@link UnknownReason#SCOPE_AMBIGUITY}: a ruled phrase left
	 * undecided is left undecided because its variants split across the criterion's questions, which
	 * is what that reason names.
	 */
	private static final Map<String, Classification> RULINGS = Map.ofEntries(
			Map.entry("product owner", Classification.in()),
			Map.entry("solutions consultant", Classification.in()),
			Map.entry("solutions engineer", Classification.in()),
			Map.entry("solutions engineering", Classification.in()),
			Map.entry("forward deployed engineer", Classification.in()),
			Map.entry("forward deployed engineering", Classification.in()),
			Map.entry("chief technology officer", Classification.in()),
			// A domain expert hired to teach a model their own domain, not to build one. The
			// computer-science and data-science variants of the same posting are the exceptions
			// these two rulings get wrong.
			Map.entry("ai trainer", Classification.out()),
			Map.entry("ai training", Classification.out()),
			Map.entry("data scientist", Classification.in()),
			Map.entry("mechanical engineer", Classification.out()),
			Map.entry("electrical engineer", Classification.out()),
			Map.entry("solutions architect", Classification.in()),
			Map.entry("technical program manager", Classification.in()),
			Map.entry("sales engineer", scopeAmbiguity()),
			Map.entry("data analyst", scopeAmbiguity()),
			Map.entry("business analyst", scopeAmbiguity()),
			Map.entry("support engineer", scopeAmbiguity()),
			Map.entry("civil engineer", Classification.out()),
			Map.entry("network engineer", Classification.in()),
			Map.entry("field engineer", scopeAmbiguity()),
			Map.entry("research scientist", scopeAmbiguity()),
			Map.entry("systems administrator", Classification.in()),
			Map.entry("database administrator", Classification.in()),
			Map.entry("technical writer", scopeAmbiguity()),
			Map.entry("scrum master", Classification.in()),
			Map.entry("ux engineer", Classification.in()),
			Map.entry("implementation engineer", Classification.in()),
			Map.entry("game designer", Classification.out()),
			Map.entry("game programmer", Classification.in()),
			Map.entry("solution engineer", Classification.in()),
			Map.entry("customer engineer", Classification.in()),
			// The one ruling that holds a rule together rather than recording a judgement about a
			// phrase: step 4 sends it to out, and a manufacturing system is itself software.
			Map.entry("manufacturing systems engineer", scopeAmbiguity()),
			// The engineer a customer relationship is built around: the account is who the work is
			// for and not the domain it is in, so step 4's commercial markers read it wrongly.
			Map.entry("technical account manager", Classification.in()),
			Map.entry("technical account management", Classification.in()),
			Map.entry("customer success engineer", Classification.in()),
			// Code is the primary artifact however physical the machine it runs on, which is why
			// this one phrase escapes step 4 where "mechanical software engineer" does not.
			Map.entry("embedded software engineer", Classification.in()),
			Map.entry("enterprise architect", Classification.in()),
			Map.entry("solution architect", Classification.in()),
			Map.entry("member of technical staff", Classification.in()),
			Map.entry("sdet", Classification.in()),
			// After-sales support of a physical product, and the one phrase that has to outrun the
			// sales engineer ruling sitting inside it.
			Map.entry("after sales engineer", Classification.out()),
			// A laboratory instrument's pre-sales specialist, hired for the science and not the code.
			Map.entry("field application scientist", Classification.out()),
			// The annotation posting that recruits from a software profession. It has to outrun the
			// ruling on that profession, and it is left undecided because two labellers split on it:
			// iteration 4 read "Database Administrator Graduates AI Training" as the annotation gig
			// it is, and iteration 5 read it as a job a software background alone opens.
			Map.entry("graduates ai training", scopeAmbiguity()),
			Map.entry("developers ai training", scopeAmbiguity()),
			Map.entry("board test engineer", Classification.out()),
			// An associate is a seniority in engineering and a job on a shop floor. The word cannot
			// be a head without reading "associate software engineer" as the shop floor, so the
			// three phrases that name the shop floor are ruled instead.
			Map.entry("sales associate", Classification.out()),
			Map.entry("service associate", Classification.out()),
			Map.entry("operations associate", Classification.out()),
			Map.entry("store associate", Classification.out()),
			Map.entry("general manager", Classification.out()),
			// The support technician is the one reading of a technician the labels leave open, so it
			// is named where the head is not. Spelled out family by family: a technician is out.
			Map.entry("it technician", scopeAmbiguity()),
			Map.entry("it support technician", scopeAmbiguity()),
			Map.entry("help desk technician", scopeAmbiguity()),
			Map.entry("service desk technician", scopeAmbiguity()),
			Map.entry("noc technician", scopeAmbiguity()),
			// The officers who run the information rather than the building, named so that the
			// never-engineering officer does not decide them.
			Map.entry("chief information officer", Classification.in()),
			Map.entry("chief information security officer", Classification.in()),
			Map.entry("chief data officer", Classification.in()),
			Map.entry("information security officer", scopeAmbiguity()),
			Map.entry("information system security officer", scopeAmbiguity()),
			// The architecture a posting names where another would have named the architect.
			Map.entry("solutions architecture", Classification.in()),
			Map.entry("enterprise architecture", Classification.in()),
			// The designer whose head a seniority hides: "Lead Product Designer" names the lead
			// first, and a lead carries engineering work where a designer does not.
			Map.entry("product designer", Classification.out()),
			// What the behavior marker cost was a behaviour-planning engineer, which is autonomy
			// software. The two titles the marker was bought for are named instead.
			Map.entry("behavior analyst", Classification.out()),
			// A posting looking for the people a study surveys, which hires nobody at all.
			Map.entry("survey participants", Classification.out()),
			// Iteration 4 left this shape as a ruling rather than an operations marker, so that the
			// operations a software role names still reads as software.
			Map.entry("operations manager", Classification.out()),
			// The sales engineer's own family: what the title names is the customer it is sold to
			// and not the domain the work is in, which is the same split the sales engineer carries.
			Map.entry("pre sales", scopeAmbiguity()),
			// The hospital consultant, named by the speciality because the consultant head is read
			// first and cannot see it.
			Map.entry("psychiatry", Classification.out()),
			Map.entry("consultor de vendas", Classification.out()),
			Map.entry("red team", Classification.in()),
			Map.entry("marketing cloud", Classification.in()),
			// Q1, which the criterion gained during iteration 6: a post that names no role being
			// hired for is out however engineering it sounds. It is spelt as rulings rather than
			// read off the missing head, because "Software Engineering Talent Community" names a
			// head and hires nobody, and step 2 would have let it through as unruled.
			Map.entry("talent community", Classification.out()),
			Map.entry("talent network", Classification.out()),
			Map.entry("talent pool", Classification.out()),
			Map.entry("talent pipeline", Classification.out()),
			Map.entry("candidate pool", Classification.out()),
			Map.entry("general application", Classification.out()),
			Map.entry("employment application", Classification.out()),
			Map.entry("open application", Classification.out()),
			Map.entry("open applications", Classification.out()),
			Map.entry("speculative application", Classification.out()),
			Map.entry("speculative applications", Classification.out()),
			Map.entry("spontaneous application", Classification.out()),
			Map.entry("spontaneous applications", Classification.out()),
			Map.entry("expression of interest", Classification.out()),
			Map.entry("expressions of interest", Classification.out()),
			Map.entry("future opportunity", Classification.out()),
			Map.entry("future opportunities", Classification.out()),
			Map.entry("submit your resume", Classification.out()),
			Map.entry("refer a friend", Classification.out()),
			Map.entry("interested in working with us", Classification.out()),
			// A data warehouse is software however the warehouse marker reads the word, and a machine
			// learning engineer is one whatever science the title says it serves — two of iteration
			// 7's four misses. The last two are Q1: a paid research study and a talent community in
			// the plural are posts that hire nobody.
			Map.entry("data warehouse", Classification.in()),
			Map.entry("machine learning engineer", Classification.in()),
			Map.entry("research opportunity", Classification.out()),
			Map.entry("talent communities", Classification.out()));

	private static final int LONGEST_RULING = longest(RULINGS.keySet());

	private static final int LONGEST_QUALIFIER = longest(SOFTWARE_QUALIFIERS);

	private static final int LONGEST_MARKER = longest(OFF_DOMAIN_MARKERS);

	/**
	 * Decides what one cleaned title names, by the criterion's six steps in their order.
	 * @param cleanedTitle the title once cleaning has taken the noise out of it
	 * @return what the title names, with the reason where it names too little to decide
	 */
	public Classification classify(String cleanedTitle) {
		List<String> words = words(cleanedTitle);

		// 1 — Rulings override everything. They are decisions, not shortcuts.
		String ruled = named(words, RULINGS.keySet(), LONGEST_RULING);
		if (ruled != null) {
			return RULINGS.get(ruled);
		}

		String head = head(words);

		// 2 — An unknown head is our backlog, not the corpus's ambiguity.
		if (head == null) {
			return unruled();
		}

		// 3 — Some functions are never engineering, whatever they are attached to.
		if (NEVER_ENGINEERING.contains(head)) {
			return Classification.out();
		}
		Head reading = HEADS.get(head);

		// 4 — Under a domain-bound head the modifier decides, and an off-domain marker beats a
		// software qualifier: "mechanical software engineer".
		if (reading.domain() == Domain.BOUND && named(words, OFF_DOMAIN_MARKERS, LONGEST_MARKER) != null) {
			return Classification.out();
		}

		// 5 — A software domain under a head that can carry engineering work.
		if (reading.engineeringCapable() && named(words, SOFTWARE_QUALIFIERS, LONGEST_QUALIFIER) != null) {
			return Classification.in();
		}

		// 6 — The head is known and nothing settled the rest.
		return reading.engineeringCapable() ? Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY) : unruled();
	}

	/** The first function head the title names, never the last word: a title ends in anything. */
	private static String head(List<String> words) {
		for (String word : words) {
			if (HEADS.containsKey(word) || NEVER_ENGINEERING.contains(word)) {
				return word;
			}
		}
		return null;
	}

	/**
	 * The longest of the phrases the title names, taken at its earliest position where two are as
	 * long as one another. Longest rather than first, so that {@code technical program manager} is
	 * not read as the {@code product manager} sitting inside a longer decision.
	 */
	private static String named(List<String> words, Set<String> phrases, int longest) {
		for (int length = Math.min(longest, words.size()); length >= 1; length--) {
			for (int at = 0; at + length <= words.size(); at++) {
				String phrase = String.join(" ", words.subList(at, at + length));
				if (phrases.contains(phrase)) {
					return phrase;
				}
			}
		}
		return null;
	}

	private static List<String> words(String cleanedTitle) {
		String title = cleanedTitle.trim();
		return title.isEmpty() ? List.of() : List.of(WHITESPACE.split(title.toLowerCase(Locale.ROOT)));
	}

	private static Classification scopeAmbiguity() {
		return Classification.unknown(UnknownReason.SCOPE_AMBIGUITY);
	}

	private static Classification unruled() {
		return Classification.unknown(UnknownReason.UNRULED);
	}

	private static int longest(Set<String> phrases) {
		return phrases.stream().mapToInt((phrase) -> WHITESPACE.split(phrase).length).max().orElse(1);
	}

}
