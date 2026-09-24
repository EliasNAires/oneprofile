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
 * <b>modifiers</b> naming the domain it does it in. Where a title names more than one function
 * head the first is the head, except where the first <b>yields</b>: {@code lead}, {@code
 * manager}, {@code support} and their kind name a rank or a department rather than the work, so
 * a head behind one of them is what the title is about — {@code Lead Analytics Engineer} is an
 * engineer. A head is what keeps the modifier lists honest: {@code security} is a software
 * qualifier, so {@code security analyst} is in while {@code security guard} is out on a head that
 * is never engineering whatever modifies it.
 * <p>
 * Four lists and three sets that qualify the head list. Heads carry the two attributes the
 * criterion gives them; the never-engineering heads are held apart because neither attribute
 * means anything for them.
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

	/**
	 * Heads that give way to a head standing behind them. Each names a rank ({@code lead},
	 * {@code director}) or a department ({@code support}, {@code operations}) rather than the work
	 * itself, so where another head follows, {@link #head(List)} reads that one instead:
	 * {@code Lead Analytics Engineer} is an engineer and {@code Network Support Engineer} is an
	 * engineer. Reading the yielding word as the head is the masking problem iterations 5 through
	 * 9 each priced and each left open; it cost 24 titles on iteration 10's sample alone.
	 */
	private static final Set<String> YIELDING_HEADS = Set.of(
			"lead",
			"leader",
			"leaders",
			"head",
			"director",
			"directors",
			"manager",
			"managers",
			"associate",
			"chief",
			"president",
			"vp",
			"avp",
			"mgr",
			"fellow",
			"expert",
			"master",
			"intern",
			"internship",
			"extern",
			"externship",
			"werkstudent",
			"praktikant",
			"praktikum",
			"stage",
			"stagiaire",
			"estagiário",
			"estágio",
			"chargé",
			"responsable",
			"leiter",
			"gerente",
			"member",
			"partner",
			"management",
			"operations",
			"tech",
			"support",
			"specialist",
			"specialists",
			// Round 13: the German team and shift leads, read the way leiter is, and the student and
			// graduate postings, read the way intern is.
			"teamleiter", "teamleitung", "schichtleiter", "objektleiter", "student", "students", "graduate",
			"graduates", "grad", "trainee", "trainees", "fellowship");

	/**
	 * Yielding heads that name no work of their own even when nothing stands behind them. Step 6
	 * read literally — "a modifier no one has classed is a market marker" — makes one of these OUT
	 * as soon as the title carries any
	 * modifier and no software qualifier argued back at step 5, which is what settles the
	 * {@code Hotel Manager} and {@code Director of Operations} families the market list was never
	 * going to reach one word at a time. The narrower {@code engineer} and {@code developer} heads
	 * are deliberately not here: a bare {@code C Engineer} is a software job and the same reading
	 * would decide it OUT. {@code consultant} joined in iteration 11, after the head was reclassed
	 * domain-bound: priced on the accumulated labels it decided 26 rows right and 9 wrong, with no
	 * new miss. {@code specialist} and {@code intern} were priced the same way and each added a miss.
	 */
	private static final Set<String> GENERIC_HEADS = Set.of(
			"advisor",
			"associate",
			"avp",
			"chief",
			"consultant",
			"consultants",
			"controller",
			"director",
			"directors",
			"fellow",
			"gerente",
			"head",
			"leader",
			"leaders",
			"leiter",
			"liaison",
			"management",
			"manager",
			"managers",
			"member",
			"mgr",
			"mitarbeiter",
			"operations",
			"partner",
			"president",
			"responsable",
			"sachbearbeiter",
			"strategist",
			"support",
			"tech",
			"vp",
			// Round 13: the German team and shift leads, read the way leiter is.
			"teamleiter", "teamleitung", "schichtleiter", "objektleiter");

	/**
	 * Words that hold a generic head open rather than letting the rule above decide it. The
	 * corpus's product roles are the family nine labellers have split hardest — 44 IN, 5 OUT and
	 * 58 UNKNOWN across the accumulated fixtures — so a product title with no software qualifier
	 * is left to steps 6 and 7 to answer. Where the market does not decide it, it is the
	 * {@code scope_ambiguity} the criterion says it is: what splits product roles is the expertise.
	 */
	private static final Set<String> GENERIC_HEAD_EXEMPTIONS = Set.of(
			"product",
			"products");

	private static final Map<String, Head> HEADS = Map.ofEntries(
			// The heads this corpus writes in a language other than English. Only the spellings the
			// corpus actually posts are here; a list of every plausible translation would be a list
			// of words no title uses.
			Map.entry("cientista", FREE_CAPABLE), Map.entry("científico", FREE_CAPABLE),
			Map.entry("エンジニア", BOUND_CAPABLE), Map.entry("개발자", BOUND_CAPABLE),
			Map.entry("엔지니어", BOUND_CAPABLE),
			Map.entry("engineer", BOUND_CAPABLE),
			Map.entry("developer", BOUND_CAPABLE), Map.entry("administrator", BOUND_CAPABLE),
			Map.entry("programmer", BOUND_CAPABLE), Map.entry("analyst", FREE_CAPABLE),
			Map.entry("architect", FREE_CAPABLE), Map.entry("scientist", FREE_CAPABLE),
			Map.entry("researcher", FREE_CAPABLE), Map.entry("specialist", BOUND_CAPABLE),
			Map.entry("lead", BOUND_CAPABLE), Map.entry("engineering", BOUND_CAPABLE),
			Map.entry("manager", BOUND_CAPABLE), Map.entry("fellow", BOUND_CAPABLE),
			Map.entry("associate", BOUND_CAPABLE),
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
			Map.entry("consultora", FREE_CAPABLE), Map.entry("desenvolvimento", BOUND_CAPABLE),
			Map.entry("développeur", BOUND_CAPABLE),
			Map.entry("développeuse", BOUND_CAPABLE),
			Map.entry("desarrollo", BOUND_CAPABLE),
			// The heads this corpus writes short. vp, avp and mgr are the vice president and the
			// manager abbreviated, and they read their modifier the same way those do.
			Map.entry("vp", BOUND_CAPABLE), Map.entry("avp", BOUND_CAPABLE), Map.entry("mgr", BOUND_CAPABLE),
			Map.entry("dba", BOUND_CAPABLE), Map.entry("engg", BOUND_CAPABLE),
			// A student posting is a vacancy and its modifier carries the domain, the way the
			// intern's does. The German spelling is on the list because this corpus repeats it.
			Map.entry("internship", BOUND_CAPABLE), Map.entry("externship", BOUND_CAPABLE),
			Map.entry("extern", BOUND_CAPABLE), Map.entry("werkstudent", BOUND_CAPABLE),
			// The heads iteration 9's unruled pile named. Half of them are the same functions in Spanish,
			// Portuguese, French and German; the rest are the nouns a title uses where it names the
			// function without naming the person doing it — an account management, a market data
			// operations, a chief of staff. Every one is capable, because a head that masks the head
			// behind it must not be the one that decides out: staff was left off for exactly that.
			Map.entry("chargé", BOUND_CAPABLE),
			Map.entry("chief", BOUND_CAPABLE), Map.entry("controller", BOUND_CAPABLE),
			Map.entry("especialista", BOUND_CAPABLE),
			Map.entry("estagiário", BOUND_CAPABLE), Map.entry("estágio", BOUND_CAPABLE),
			Map.entry("gerente", BOUND_CAPABLE),
			Map.entry("ingenieur", BOUND_CAPABLE), Map.entry("ingénieur", BOUND_CAPABLE),
			Map.entry("leiter", BOUND_CAPABLE),
			Map.entry("liaison", BOUND_CAPABLE), Map.entry("management", BOUND_CAPABLE),
			Map.entry("member", BOUND_CAPABLE), Map.entry("mitarbeiter", BOUND_CAPABLE),
			Map.entry("operations", BOUND_CAPABLE),
			Map.entry("praktikant", BOUND_CAPABLE), Map.entry("praktikum", BOUND_CAPABLE),
			Map.entry("produktmanager", BOUND_CAPABLE), Map.entry("responsable", BOUND_CAPABLE),
			Map.entry("sachbearbeiter", BOUND_CAPABLE), Map.entry("specialiste", BOUND_CAPABLE),
			Map.entry("spécialiste", BOUND_CAPABLE), Map.entry("stage", BOUND_CAPABLE),
			Map.entry("stagiaire", BOUND_CAPABLE), Map.entry("support", BOUND_CAPABLE),
			Map.entry("tech", BOUND_CAPABLE), Map.entry("techniker", BOUND_CAPABLE),
			Map.entry("tester", BOUND_CAPABLE),
			// The software engineer by its abbreviation, the engineer by its commonest misspelling, and
			// the inclusive French spelling of a developer.
			Map.entry("swe", BOUND_CAPABLE), Map.entry("enginer", BOUND_CAPABLE),
			Map.entry("développeur.euse", BOUND_CAPABLE),
			// Round 13: the German team and shift leads, and the student and graduate postings, which
			// read their modifier the way leiter and intern do.
			Map.entry("teamleiter", BOUND_CAPABLE), Map.entry("teamleitung", BOUND_CAPABLE),
			Map.entry("schichtleiter", BOUND_CAPABLE), Map.entry("objektleiter", BOUND_CAPABLE),
			Map.entry("student", BOUND_CAPABLE), Map.entry("students", BOUND_CAPABLE),
			Map.entry("graduate", BOUND_CAPABLE), Map.entry("graduates", BOUND_CAPABLE),
			Map.entry("grad", BOUND_CAPABLE), Map.entry("trainee", BOUND_CAPABLE),
			Map.entry("trainees", BOUND_CAPABLE), Map.entry("fellowship", BOUND_CAPABLE));

	/**
	 * The heads that decide out whatever modifies them, and so carry neither attribute. Each one
	 * names a function that no software domain turns into engineering work: there is no software
	 * veterinarian and no software bartender.
	 */
	private static final Set<String> NEVER_ENGINEERING = Set.of(
			"trainer", "assistant", "executive", "nurse", "representative", "coordinator",
			"teacher", "driver",
			"veterinarian", "physician", "psychologist", "psychiatrist", "pharmacist", "dentist", "surgeon",
			"therapist", "dietitian", "practitioner", "paramedic", "midwife", "caregiver", "pathologist",
			"radiographer", "sonographer", "attorney", "paralegal", "counsel", "counselor", "recruiter",
			"accountant", "bookkeeper", "auditor", "cashier", "clerk", "receptionist", "secretary", "janitor",
			"housekeeper", "chef", "cook", "bartender", "barista", "waiter", "host", "stylist", "barber",
			"esthetician", "merchandiser", "installer", "plumber", "mechanic", "welder",
			"machinist", "carpenter",
			"painter", "roofer", "landscaper", "guard", "firefighter", "instructor", "tutor", "professor",
			"librarian", "translator", "interpreter", "linguist", "editor", "artist", "animator",
			"illustrator",
			"photographer", "videographer", "copywriter", "journalist", "salesperson", "seller", "ambassador",
			"volunteer", "supervisor", "operator", "foreman", "superintendent", "dispatcher",
			"courier", "chaplain",
			"electrician", "anesthesiologist", "gastroenterologist", "histopathologist",
			"neurologist", "podiatrist",
			"psychotherapist", "chemist", "millwright", "laborer", "custodian", "estimator",
			"buyer", "producer",
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
			"porter", "dishwasher", "handler", "sourcer", "broker", "clinician", "physiotherapist",
			"dermatologist",
			"oncologist", "cardiologist", "cardiologists", "aesthetician", "reporter", "educator", "merchant",
			"advocate", "scheduler", "employee", "surveyor", "helper", "helpers", "caregivers",
			// The Spanish, Portuguese, French, German and Dutch spellings of the same functions.
			"aide", "agente", "auxiliar", "auxiliaire", "assistante", "medewerker", "berater", "vendedor",
			"vendedora", "vendeur", "vendeuse", "operador", "operadora", "enfermero", "enfermera",
			"enfermeiro",
			"enfermeira", "provider", "navigator", "banker", "attendant", "fabricator", "fitter",
			"physiologist",
			"optometrist", "endocrinologist", "sorter", "mixer", "apprentice", "firefighters", "pathologists",
			"personalberater", "verkoopmedewerker", "superviseur", "directeur", "ejecutivo", "executivo",
			"produktionsleiter",
			// The professions iteration 7's unruled pile named, and the officers a title writes as
			// three letters. A chief of finance, marketing, people or operations is never an
			// engineering role; the chiefs that are — the technology and information officers — are
			// rulings, and a ruling runs first. gm says what the general manager ruling says.
			"pediatrician", "physiatrist", "psychotherapists", "registrar", "crna", "dvm",
			"orthotist", "hygienist",
			"sommelier", "polisher", "juicer", "usher", "concierge", "interpreters", "cleaner", "negotiator",
			"trader", "evaluator", "grader", "scriptwriter", "interventionist", "rodman",
			"sonographers", "cfo",
			// The professions iteration 8's unruled pile named, one shape at a time.
			"cmo", "chro", "coo", "sdr", "gm", "endodontist", "internist", "dietician", "nanny", "scribe",
			"correspondent", "assessor", "keyholder", "runner", "shuttler", "orthodontist", "babysitter",
			// The clinical grades and licences the unruled pile was largest in: an NHS band, an American
			// nursing licence and the locum that staffs it. Sales is a function and not only a market —
			// it was on the market list alone, which left a headless sales title unruled and read a sales
			// title whose modifier named software as an engineering role. Sales Engineer is a ruling, and
			// rulings run first. Técnico and coordenador follow the technician and the coordinator.
			"actuary", "band", "cna", "coordenador", "coordinador", "locum", "lpn", "rn", "sales", "tecnico",
			"teller", "técnico", "underwriter",
			// The professions iteration 10's unruled pile named: clinical specialities, the care and
			// hospitality floor, and the warehouse line.
			"audiologist", "babysitting", "bcba", "budtender", "busser", "detailer", "doctor", "doctors",
			"doula", "emt", "geologist", "gynecologist", "hematologist", "housekeeping",
			"lawyer", "lifeguard", "loader", "mammographers", "marketer", "nurses", "obstetrician",
			"operative", "ophthalmologist", "ophthalmologists", "organiser", "organizer", "packer",
			"paediatrician", "palletizer", "phlebotomist", "physicians", "physicist", "subcontractor",
			"veterinarians"
		,
			// The heads round 13 found in the unruled pile, each one named in almost no title the rules call in.
			"investigator", "investigators", "originator", "processor", "annotator", "labeler", "scout",
			"responder", "entrepreneur", "adviser", "adviseur", "belastingadviseur", "guide", "biologist",
			"assembler", "barback", "economist", "radiologist", "adjuster", "adjusters", "pipefitter",
			"groundskeeper", "histotechnician", "urologist", "expeditor", "examiner", "drafter",
			"facilitator", "photojournalist", "psychotherapeut", "psychologe", "optician", "groomer",
			"proofreader", "retoucher", "wholesaler", "commis", "meteorologist", "archaeologist",
			"criticalist", "mascot", "biller", "cajera", "bookseller", "lokführer",
			"vertriebsmitarbeiter", "produktionsmitarbeiter", "aangiftemedewerker", "salarisprofessional",
			"assistent", "assistente", "assistants", "contributor", "anchor", "giver", "rbt",
			"lvn", "lpt", "hha");

	/**
	 * A software domain. One of these under an engineering-capable head decides in, and it decides
	 * nothing at all on its own — which is what keeps {@code security guard} off the list.
	 * <p>
	 * {@code solutions} is deliberately absent: every industry sells solutions, so the word under a
	 * head decided {@code Workplace Solutions Manager} in. The solutions roles that are engineering
	 * roles are named by ruling instead.
	 */
	private static final Set<String> SOFTWARE_QUALIFIERS = Set.of(
			"graph",
			"devex",
			"malware",
			"sharepoint",
			"sdk",
			"iam",
			"secops",
			"telemetry",
			"microservices",
			"latency",
			"runtime",
			"dados",
			"datos",
			"소프트웨어",
			"software", "backend", "back end", "frontend", "front end", "fullstack", "full stack", "data",
			"platform", "devops", "sre", "site reliability", "security", "mobile", "ios", "android", "cloud",
			"infrastructure", "automation", "machine learning", "deep learning",
			"ml", "ai", "systems", "network", "web", "api", "embedded", "application",
			"integration", "database",
			"firmware", "compiler", "robotics", "it", "information technology", "computer",
			"computer science",
			"cybersecurity", "mlops", "java", "python", "javascript", "typescript", "salesforce",
			"sap", "azure",
			"aws", "kubernetes", "linux", "blockchain", "react", "sql", "saas", "algorithm", "algorithms",
			"middleware", "ux", "ui", "edi", "identity access management", "technical", "llm", "nlp",
			"computer vision", "generative ai", "informatics", "netsuite", "servicenow", "workday",
			"gameplay",
			"unreal", "quant", "quantitative", "rendering", "graphics", "detection engineering", "exploit",
			"streaming", "c++", "gis", "outsystems", "devsecops", "observability", "threat",
			"cyber", "applications",
			"storage", "bi", "technology",
			// The software domains iteration 7's unknown pile named by their own word.
			// The software domains iteration 8's missed in rows named by their own word.
			"incident response", "forensic", "dfir", "postgresql", "datapath", "model training",
			"rust", "django",
			"rpa", "uipath", "snowflake", "power bi", "dmz", "vulnerability management", "isso",
			"agile", "helpdesk",
			"help desk", "interoperability", "ddi", "golang", "kotlin", "scala", "ruby", "php",
			"rails", "angular",
			"terraform", "ansible", "docker", "hadoop", "kafka", "tableau", "databricks",
			// The word the corpus writes for software when it is not writing English.
			"informatica", "informatique", "logiciel",
			// The software domains iteration 10's unknown pile named by their own word, the one
			// misspelling of a platform the corpus writes often enough to count, and the software
			// engineer's abbreviation, which is a head as well so that "SWE Data Ingestion" has one.
			"flutter", "genai", "agentic", "c#", "ubuntu", "vulnerability", "redes", "mac os", "macos",
			"andorid", "swe",
			// Two roles named by a phrase that holds a head. As rulings they would outrun the discipline
			// markers and decide "Tech Lead ASIC Design Engineer" in; as qualifiers step 4 still reads the
			// ASIC first. Bare "Tech Lead" was out on the generic-head rule, which read "lead" as a
			// modifier of "tech".
			"tech lead", "analytics engineer", "analytics engineering"
		);

	/**
	 * A body of training a person is hired on. It names the candidate rather than the customer, so it
	 * decides out under any head — domain-bound or domain-free — and it beats a software qualifier:
	 * {@code mechanical software engineer} and {@code nurse analyst} are both out.
	 * <p>
	 * This is what settles the hardware-adjacent code roles. An FPGA or ASIC verification engineer
	 * writes SystemVerilog, which is the criterion's Q2, and hires on a hardware credential, which
	 * fails its Q4; the discipline the title names is what decides between them.
	 * <p>
	 * The test for membership is the credential: would a person need that training to be hired? This
	 * list was split out of one undivided marker list in iteration 8, by measuring which markers ever
	 * share a title with a software qualifier and classing those — a marker that never meets a
	 * qualifier behaves identically in either class. {@code data center} is here for the facility it
	 * names: the corpus's data-center titles are cabling foremen, electricians and repair technicians,
	 * and the word {@code data} inside the phrase would otherwise read as a software qualifier.
	 */
	private static final Set<String> DISCIPLINE_MARKERS = Set.of(
			// The trainings iteration 10's unknown pile hired on: bioscience, the power grid, civil
			// site work and the process plant. Protein and actuarial were priced and left out — the
			// first named machine-learning roles in protein design, the second an actuarial software
			// engineer.
			"bioconjugation", "substation", "power generation", "land development", "traffic engineering",
			"process engineering", "cultivation",
			"radar",
			"metrology",
			"hil",
			"shop floor",
			"motor controls",
			"calibration",
			"fire protection",
			"brake",
			"medium voltage",
			"assay",
			"polymer",
			"machining",
			"tool and die",
			"post silicon",
			"occupational",
			"phlebotomy",
			"civil", "structural", "structures", "mechanical", "mechatronics", "chemical",
			"chemistry", "materials",
			"thermal", "hydraulic", "pneumatic", "cryogenic", "cryogenics", "combustion", "propulsion",
			"turbomachinery", "aerospace", "avionics", "nuclear", "petroleum", "geotechnical", "roadway",
			"preconstruction", "architectural", "mep", "low voltage", "hvac", "plumbing", "electrician",
			"electronics", "analog", "rf", "rfic", "emc", "laser", "photonics", "optomechanical", "fluidic",
			"semiconductor", "wafer", "silicon engineering", "plc", "controls", "hardware",
			"battery", "brakes",
			"steering", "fastener", "mechanism", "ew", "biomedical", "mining", "mine",
			"agriculture", "agricultural",
			"underground", "explosive", "data center", "welder", "weld", "welding", "machinist", "forklift",
			"janitor", "sanitation", "housekeeping", "nurse", "nursing", "clinical", "patient", "pharmacy",
			"pharmaceutical", "medical", "mammography", "oncology", "pathology", "ward", "anesthesia",
			"dental", "veterinary", "veterinarian", "hospice", "speech", "case manager", "behavioral",
			"personal care", "microbiology", "laboratory", "gene", "immunology", "biometrics", "attorney",
			"paralegal", "teacher", "tutor", "chef", "culinary", "graphic", "vfx", "interiors", "renovations",
			"power systems",
			// The three words the split moved out of the market list once it was priced, and the four
			// it had to write fresh: without them a pfas manager, a water systems specialist, a
			// physical security engineer and a guidance navigation and control engineer all decided in
			// on the qualifier standing next to the credential.
			"pfas", "water", "physical", "gnc", "guidance navigation", "high pressure",
			"industrial automation",
			// The credentials iteration 8's sample named. fpga, asic and rtl are the criterion's own
			// worked example for the hardware-adjacent code roles and were on no list until now; counsel
			// and dentist are here as credentials rather than only as heads, because associate and
			// director are named before them and hide them, the same repayment iteration 7 made with
			// chef and veterinarian.
			"fpga", "asic", "rtl", "dft", "serdes", "signal integrity", "pcb", "electrical",
			"airworthiness", "cfd", "cathode", "dfm", "energetics", "human factors", "formulation",
			"cqv", "aba",
			"bioinformatics", "biomarkers", "ultrasound", "neurology", "radiology", "bacteriology", "autism",
			"counsel", "dentist", "nanny", "scribe",
			// A dialysis role is certified clinical work, so the credential test the criterion writes
			// puts it here and not on the market list: it decides out under a domain-free head too.
			"dialysis"
		);

	/**
	 * Who the work is done for. Software is built for every market, so a market marker says nothing
	 * about whether this role builds it: it decides out only under a domain-bound head with no
	 * software qualifier, and under a domain-free head it settles nothing at all. {@code marketing
	 * manager} is out, {@code marketing web developer} is not, and {@code audit analyst} is undecided.
	 * <p>
	 * A marker nobody has classed belongs here, because market is the class that cannot cause a miss:
	 * the worst it does is leave a title undecided, and a vacancy decided out reaches neither the
	 * description pass that reads bodies nor a profile.
	 * <p>
	 * The commercial domains are here for the reason the physical ones were: a manager of an account,
	 * a territory or a hiring pipeline is not managing software. {@code sales} is among them because
	 * the one title it would have decided wrongly, Sales Engineer, is a ruling — and a ruling runs
	 * before step 4.
	 */
	private static final Set<String> MARKET_MARKERS = Set.of(
			"sports",
			"trading desk",
			"proposal",
			"grants",
			"retirement",
			"cruise",
			"maritime",
			"vocational",
			"claims",
			"closing",
			"conference",
			"valet",
			"babysitting",
			"shift",
			"hazardous waste",
			"dangerous goods",
			"due diligence",
			"workforce planning",
			"account", "accounting", "accounts", "apparel", "asset", "audit", "automotive",
			"aviation", "banking",
			"beauty", "benefits", "bookkeeping", "brand", "budget", "business development",
			"business operations",
			"cable", "campaign", "cashier", "channel", "channels", "charity", "client", "clients", "coach",
			"commercial", "commissioning", "community", "compensation", "compliance",
			"construction", "contracts",
			"customer care", "customer experience", "customer success", "demand generation", "door", "driver",
			"editorial", "energy", "environmental", "equipment", "escrow", "esg", "events",
			"facilities", "farm",
			"fashion", "finance", "financial", "fitness", "flight", "gas", "gear",
			"government affairs", "grid",
			"hospitality", "housing", "hr", "human resources", "industrial", "insurance", "intake",
			"inventory",
			"investment", "investor", "kids", "kitchen", "launch", "leasing", "legal", "licensing",
			"logistics",
			"lunar", "maintenance", "manufacturing", "marketing", "merchandising", "operational excellence",
			"outreach", "paid search", "partnerships", "payroll", "personal training", "print",
			"private equity",
			"procurement", "programmatic", "property", "public relations", "purchasing",
			"real estate", "records",
			"recruiting", "recruitment", "regulatory", "relationship", "renewals", "restaurant",
			"retail", "sales",
			"satellite", "seo", "shipping", "social media", "solar", "sourcing", "spacecraft",
			"space systems",
			"staffing", "store", "supply chain", "talent", "tax", "telecommunications",
			"telesales", "territory",
			"tiktok", "tour", "transformation", "transmission", "transportation", "vehicle",
			"warehouse", "wealth",
			// The markets iteration 8's domain-ambiguity pile named.
			"wellness", "yard", "advertising", "creative", "visuals", "giving", "collections",
			"people", "hrbp",
			"fp&a", "federal affairs", "policy", "media relations", "royalties", "packaging",
			"footwear", "supplier",
			"deal desk", "enablement", "alliances", "credit risk", "localization", "catalog", "pricing",
			"fulfillment", "warranty", "residential", "site acquisition", "branch", "poker", "organizing",
			"commerce", "treatment", "imagery", "inspection", "survey", "fundraising", "philanthropy",
			// The markets iteration 9's domain-ambiguity pile named. The Spanish, Portuguese, French and
			// German ones are here to decide the non-English heads on the head list: a head on its own only
			// moves a row from unruled to domain ambiguity, so those heads are worth nothing until the
			// markers land beside them.
			"atención al cliente", "atendimento", "buchhaltung", "care", "compras", "contabilidad",
			"contabilidade", "customer service", "customer support", "direct support", "einkauf",
			"event", "health", "logistica", "logística", "loja",
			"mantenimiento", "manutenção", "member experience", "production", "recursos humanos",
			"salud", "segurança", "seguridad", "social", "strategy", "sécurité", "team member",
			"vendas", "ventas", "ventes", "vertrieb"
		);

	/**
	 * The answers the procedure must reproduce. A ruling is a decision about a phrase, so it changes
	 * only by a decision and never as a side effect of a list edit.
	 * <p>
	 * An undecided ruling is always {@link UnknownReason#SCOPE_AMBIGUITY}: a ruled phrase left
	 * undecided is left undecided because its variants split across the criterion's questions, which
	 * is what that reason names.
	 */
	private static final Map<String, Classification> RULINGS = Map.ofEntries(
			Map.entry("qa engineer", Classification.in()), Map.entry("test automation", Classification.in()),
			Map.entry("automation test", Classification.in()), Map.entry("qa automation",
			Classification.in()),
			Map.entry("qa analyst", Classification.in()), Map.entry("test analyst", Classification.in()),
			
			Map.entry("product owner", Classification.in()),
			Map.entry("solutions consultant", Classification.in()),
			Map.entry("solutions engineer", Classification.in()),
			Map.entry("solutions engineering", Classification.in()),
			Map.entry("forward deployed engineer", Classification.in()),
			Map.entry("forward deployed engineering", Classification.in()),
			Map.entry("forward deployment engineer", Classification.in()),
			Map.entry("chief technology officer", Classification.in()),
			// A domain expert hired to teach a model their own domain, not to build one. The
			// computer-science and data-science variants of the same posting are the exceptions
			// these two rulings get wrong.
			Map.entry("data scientist", Classification.in()),
			Map.entry("mechanical engineer", Classification.out()),
			Map.entry("electrical engineer", Classification.out()),
			Map.entry("solutions architect", Classification.in()),
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
			Map.entry("talent communities", Classification.out()),
			// The shapes iteration 8's sample wrote for the post that hires nobody: an interest
			// register, a general interest page and the "don't see it here" banner.
			Map.entry("register your interest", Classification.out()),
			Map.entry("general interest", Classification.out()),
			Map.entry("don t see", Classification.out()),
			// The shapes iteration 10's sample wrote for it: the Portuguese and German talent pools and
			// "didn't find your vacancy" banners, careers fairs, and the study that recruits
			// participants rather than staff.
			Map.entry("banco de talentos", Classification.out()),
			Map.entry("banco de candidatos", Classification.out()),
			Map.entry("não encontrou", Classification.out()),
			Map.entry("initiativbewerbung", Classification.out()),
			Map.entry("candidature spontanée", Classification.out()),
			Map.entry("career opportunities", Classification.out()),
			Map.entry("career fair", Classification.out()),
			Map.entry("interested in applying", Classification.out()),
			Map.entry("share your contacts", Classification.out()),
			Map.entry("no open role", Classification.out()),
			Map.entry("resume drop", Classification.out()),
			Map.entry("study participant", Classification.out()),
			Map.entry("studienteilnehmer", Classification.out()),
			// A shop's key holder, where holder alone also names a PhD holder.
			Map.entry("key holder", Classification.out()));

	/**
	 * Families the criterion decides by ruling but that a discipline marker still decides out, so
	 * they are read after the credential rather than above the whole procedure the way a ruling is.
	 * {@code technical} names the subset of product and program management a software background
	 * opens; a technical project is as often cabling or construction as software, so it stays open
	 * on its domain. Every other product, program and project manager is open on its expertise,
	 * whatever software or market the title names.
	 */
	private static final Map<String, Classification> MANAGEMENT_FAMILIES = Map.ofEntries(
			Map.entry("technical product manager", Classification.in()),
			Map.entry("technical product managers", Classification.in()),
			Map.entry("technical program manager", Classification.in()),
			Map.entry("technical program managers", Classification.in()),
			Map.entry("technical project manager", Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY)),
			Map.entry("technical project managers", Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY)),
			Map.entry("product manager", scopeAmbiguity()), Map.entry("product managers", scopeAmbiguity()),
			Map.entry("program manager", scopeAmbiguity()), Map.entry("program managers", scopeAmbiguity()),
			Map.entry("project manager", scopeAmbiguity()), Map.entry("project managers", scopeAmbiguity()));

	/** The phrases that make a post an AI-training gig, decided by the expertise it names. */
	private static final Set<String> AI_TRAINING = Set.of("ai trainer", "ai trainers", "ai training", "ai tutor");

	/**
	 * The expertise that makes an AI-training post a job a software background opens. Its own list
	 * rather than the qualifiers, because a qualifier such as {@code data} names a data-entry clerk
	 * here as readily as a data scientist.
	 */
	private static final Set<String> AI_TRAINING_SOFTWARE_EXPERTISE = Set.of(
			"computer science", "computer sciences", "software", "developer", "developers", "programmer",
			"programmers", "programming", "coding", "javascript", "typescript", "python", "java", "html",
			"css", "sql", "full stack", "frontend", "front end", "backend", "back end", "ai engineer",
			"ml engineer", "machine learning", "data science", "database administrator");

	/** An AI-training post that names no expertise at all, and so is open on the expertise it needs. */
	private static final Set<String> AI_TRAINING_WITHOUT_EXPERTISE = Set.of(
			"ai training experts", "ai training expert");

	/**
	 * The rank words the criterion strips before reading what a manager manages. Under one of them,
	 * a technology noun that names no engineering function leaves the role open on its expertise:
	 * an IT manager may run a help desk as easily as a team of engineers.
	 */
	private static final Set<String> RANK_HEADS = Set.of(
			"manager", "managers", "mgr", "director", "directors", "head", "lead", "leader", "leaders", "vp",
			"avp", "chief", "president", "gerente", "leiter", "responsable");

	private static final Set<String> NON_FUNCTION_QUALIFIERS = Set.of(
			"it", "information technology", "technology", "data", "security", "cyber", "cybersecurity",
			"salesforce", "sap", "netsuite", "workday", "servicenow");

	private static final Set<String> FUNCTION_QUALIFIERS = SOFTWARE_QUALIFIERS.stream()
		.filter((qualifier) -> !NON_FUNCTION_QUALIFIERS.contains(qualifier))
		.collect(java.util.stream.Collectors.toUnmodifiableSet());

	/**
	 * Named enterprise packages, and the heads under which one names a functional role: a
	 * consultant configuring a business process on the package needs finance or supply-chain
	 * knowledge a software background does not bring. Under a developer the package is software.
	 */
	private static final Set<String> ENTERPRISE_PACKAGES = Set.of(
			"sap", "salesforce", "netsuite", "workday", "servicenow", "oracle ebs");

	private static final Set<String> FUNCTIONAL_HEADS = Set.of(
			"consultant", "consultants", "consultor", "consultora", "analyst", "analysts", "analista",
			"administrator");

	private static final int LONGEST_MANAGEMENT_FAMILY = longest(MANAGEMENT_FAMILIES.keySet());

	private static final int LONGEST_AI_TRAINING_EXPERTISE = longest(AI_TRAINING_SOFTWARE_EXPERTISE);

	private static final int LONGEST_FUNCTION_QUALIFIER = longest(FUNCTION_QUALIFIERS);

	private static final int LONGEST_ENTERPRISE_PACKAGE = longest(ENTERPRISE_PACKAGES);

	private static final int LONGEST_RULING = longest(RULINGS.keySet());

	private static final int LONGEST_QUALIFIER = longest(SOFTWARE_QUALIFIERS);

	private static final int LONGEST_DISCIPLINE_MARKER = longest(DISCIPLINE_MARKERS);

	private static final int LONGEST_MARKET_MARKER = longest(MARKET_MARKERS);

	private static final int LONGEST_GENERIC_HEAD_EXEMPTION = longest(GENERIC_HEAD_EXEMPTIONS);

	/**
	 * Decides what one cleaned title names, by the criterion's procedure in its order.
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

		// 1a — The families the criterion decides by ruling, below the credential: a discipline
		// marker still decides each of them out.
		boolean disciplined = named(words, DISCIPLINE_MARKERS, LONGEST_DISCIPLINE_MARKER) != null;
		if (named(words, AI_TRAINING, 2) != null) {
			if (disciplined) {
				return Classification.out();
			}
			if (named(words, AI_TRAINING_SOFTWARE_EXPERTISE, LONGEST_AI_TRAINING_EXPERTISE) != null) {
				return Classification.in();
			}
			return named(words, AI_TRAINING_WITHOUT_EXPERTISE, 4) != null ? scopeAmbiguity() : Classification.out();
		}
		String family = named(words, MANAGEMENT_FAMILIES.keySet(), LONGEST_MANAGEMENT_FAMILY);
		if (family != null) {
			return disciplined ? Classification.out() : MANAGEMENT_FAMILIES.get(family);
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

		// 4 — The credential beats everything the title says about the market, under any head:
		// "mechanical software engineer" and "nurse analyst" are both out.
		if (named(words, DISCIPLINE_MARKERS, LONGEST_DISCIPLINE_MARKER) != null) {
			return Classification.out();
		}

		// 5 — A software domain under a head that can carry engineering work. Two heads read it
		// narrower: a rank word over a technology noun that names no engineering function, and a
		// functional head over an enterprise package, are both open on the expertise they need.
		if (reading.engineeringCapable() && named(words, SOFTWARE_QUALIFIERS, LONGEST_QUALIFIER) != null) {
			if (FUNCTIONAL_HEADS.contains(head)
					&& named(words, ENTERPRISE_PACKAGES, LONGEST_ENTERPRISE_PACKAGE) != null) {
				return scopeAmbiguity();
			}
			if (RANK_HEADS.contains(head)
					&& named(words, FUNCTION_QUALIFIERS, LONGEST_FUNCTION_QUALIFIER) == null) {
				return scopeAmbiguity();
			}
			return Classification.in();
		}

		// 6 — With no qualifier to argue against, the market decides a bound head.
		// 6a — A generic head with a modifier and nothing software about it. The criterion's
		// own default, that an unclassed modifier is a market marker, applied where it cannot
		// cause a miss.
		boolean product = named(words, GENERIC_HEAD_EXEMPTIONS, LONGEST_GENERIC_HEAD_EXEMPTION) != null;
		if (reading.domain() == Domain.BOUND && GENERIC_HEADS.contains(head) && words.size() > 1 && !product) {
			return Classification.out();
		}

		// An engineering organisation names whom it builds for with a market word, the way a
		// marketing web developer does, so the market decides nothing under it.
		if (reading.domain() == Domain.BOUND && !head.equals("engineering")
				&& named(words, MARKET_MARKERS, LONGEST_MARKET_MARKER) != null) {
			return Classification.out();
		}

		// 7 — The head is known and nothing settled the rest. Where product held a generic head
		// open, what it leaves open is the expertise.
		if (GENERIC_HEADS.contains(head) && product) {
			return scopeAmbiguity();
		}
		return reading.engineeringCapable() ? Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY) : unruled();
	}

	/**
	 * The function head the title names, never the last word: a title ends in anything. A yielding
	 * word is skipped while a head still follows it, and a never-engineering word behind a yielding
	 * word names who the work is done for rather than what it does, so the yielding word keeps the
	 * head there. Returns null where nothing in the title is a head this class reaches.
	 */
	private static String head(List<String> words) {
		String yielding = null;
		for (String word : words) {
			if (YIELDING_HEADS.contains(word)) {
				if (yielding == null) {
					yielding = word;
				}
				continue;
			}
			if (NEVER_ENGINEERING.contains(word)) {
				return yielding != null ? yielding : word;
			}
			if (HEADS.containsKey(word)) {
				return word;
			}
		}
		return yielding != null && (HEADS.containsKey(yielding) || NEVER_ENGINEERING.contains(yielding))
				? yielding
				: null;
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
