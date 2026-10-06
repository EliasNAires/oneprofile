package oneprofile.backend.workers.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import org.springframework.stereotype.Component;

/**
 * Decides from its description whether a vacancy of the pile, one its title left unknown for a
 * corpus reason, names an engineering role. The criterion it answers to is
 * {@code docs/engineering-role-body-criterion.md}: that document is judgement and this class is
 * mechanism, so where the two disagree the document is right and these patterns are wrong. How it
 * is scored is ADR-0012.
 * <p>
 * The questions are asked in the criterion's order. A description too short to describe any work
 * is unknown. A post that hires nobody (Q1), a role that carries a quota and a role that requires
 * expertise outside software are out, and so is a {@code domain_ambiguity} role that works on a
 * physical product unless its own work on it is software. A role that states a software background
 * or works on engineer-facing artifacts (Q2–Q4) is in. What is left is out, since none of the
 * questions holds, except a {@code domain_ambiguity} role whose duties are work that could be
 * software or not, in a description that names no other domain: there the title's doubt is still
 * open, and it is unknown. A word that could be software or not — automation, simulation, embedded
 * — opens no such doubt when it is not the role's work.
 * <p>
 * It reads the description as cleaning cut it into segments, so whether a mention is asked for,
 * listed as a plus or merely said of the company is read from the segment it is in, the sentence,
 * list item or heading, and from the heading that segment sits under. The board's boilerplate is the
 * company's text, not the role's, and is not read. The whole of it is plain matching over the
 * lowercased segments, each pattern tried only on a segment holding the literal words it needs,
 * which keeps a pass over the pile inside the time ADR-0012 allows it.
 */
@Component
public class BodyClassificationRule {

	/** The longest descriptions in the pile that describe no work are placeholders and a link. */
	private static final int SHORTEST_DESCRIPTION_OF_WORK = 200;

	/**
	 * What a post says when it hires nobody (Q1): a pipeline or general application, said of the
	 * post itself. A talent network named in a vacancy's footer is not one.
	 */
	private static final Anchored NO_VACANCY = new Anchored(String.join("|",
			"\\bthis is an? (prospective|pipeline|general application)",
			"\\b(this|it) is not an? (active|specific|current) (role|opening|job|position|vacancy)",
			"\\bnot an active role\\b", "\\bthis is not for a specific role", "\\bpipeline candidates",
			"\\bto grow our pipeline"), "this is a", "is not a", "not an active", "is not for", "pipeline");

	/**
	 * What a description says when the role sells: it carries a quota, or is measured on meeting
	 * one. A role that plans or tracks quotas for others, or works beside those who carry them, does
	 * not sell.
	 */
	private static final Anchored CARRIES_A_QUOTA = new Anchored(String.join("|",
			"\\b(as|be|is|are) an? quota[- ]carrying\\b",
			"(?<!\\bnot )\\bcarr(y|ies|ying) (a|an|your|the)( \\w+){0,2} quota",
			"\\b(achiev|attain|meet|exceed|hit)\\w* (your |the |an? )?(\\w+ ){0,2}quota"), "quota");

	/**
	 * Where a description names a degree. "Master" alone is left out: it is also a Scrum Master's
	 * title, so a master's counts only as "master's in" or "master's of".
	 */
	private static final String DEGREE = "\\b(degree|bachelor\\S*|b\\.?s\\.?c?|b\\.a|m\\.?s\\.?c?|ph\\.?d"
			+ "|master['’]?s (in|of))\\b";

	/**
	 * Expertise outside software a software applicant would lack: a degree in a field the role is
	 * about, another engineering discipline among them, or a licence or qualification in one.
	 */
	private static final Anchored FIELD_DEGREE = new Anchored(String.join("|",
			DEGREE + ".{0,60}?\\b(mechanical|civil|electrical"
					+ "|chemical|structural|aerospace|industrial|manufacturing|biomedical|nuclear|petroleum|materials"
					+ "|environmental) engineering\\b",
			DEGREE + ".{0,60}?\\b(finance|accounting|marketing|law"
					+ "|medicine|nursing|pharmacy|actuarial science)\\b",
			"\\bp\\.?e\\.? (license|licensure|registration)", "\\bprofessional engineer(ing)? (\\(pe\\) )?licen",
			"\\bregistered nurse\\b", "\\blicensed (attorney|pharmacist|physician|nurse|clinician)",
			"\\bcpa (license|certification)", "\\bactuarial (exams?|credentials?|designation)"), "mechanical", "civil",
			"electrical", "chemical", "structural", "aerospace", "industrial", "manufacturing", "biomedical", "nuclear",
			"petroleum", "materials", "environmental", "finance", "accounting", "marketing", "law", "medicine", "nursing",
			"pharmacy", "actuarial", "licen", "registration", "registered nurse", "cpa");

	/** A degree in a natural science, which is field expertise unless the list is a quantitative one. */
	private static final Anchored SCIENCE_DEGREE = new Anchored(DEGREE + ".{0,60}?\\b(physics|neuroscience|biology"
			+ "|biochemistry|chemistry|life sciences|geology|astronomy|astrophysics)\\b", "physics", "neuroscience",
			"biology", "biochemistry", "chemistry", "life sciences", "geology", "astronomy", "astrophysics");

	/**
	 * The fields of a quantitative degree list, which a science sits in as one way into analysis
	 * rather than as the subject of the role.
	 */
	private static final Pattern QUANTITATIVE_FIELD = Pattern.compile("\\b(math\\w*|statistics|quantitative|economics"
			+ "|econometrics)\\b");

	/**
	 * The fields whose knowledge a description can ask for in its own right, apart from any degree:
	 * the subjects of finance, the sciences, and the engineering of radios, vehicles and aircraft.
	 */
	private static final String FIELD = "(financial services|finance|banking|payments|insurance|fraud|financial[- ]crimes?"
			+ "|anti-money[- ]laundering|aml|trading|capital markets|biolog\\w*|life sciences|genomics|physics|rf"
			+ "|radio frequency|satcom|satellite communications?|powertrain|avionics|flight controls?|pharmac\\w*"
			+ "|chemistry|clinical|medical|tax|legal|accounting|quantum (hardware|physics|mechanics))";

	/**
	 * The science, engineering, medical and financial fields in which experience is itself field
	 * expertise.
	 */
	private static final String HARD_FIELD = "(biolog\\w*|genomics|physics\\S*|rf|radio frequency|satcom|powertrain"
			+ "|avionics|flight controls?|medical (terminology|coding)|medicare|medicaid|icd-?\\d+"
			+ "|(equity |interest rate |credit |fx )?derivatives?)";

	/** The subjects of compliance, whose experience is field expertise for an analyst. */
	private static final String COMPLIANCE = "((?<!(security|soc ?2|cloud|data|it|pci|pci-dss|iso 27001|fedramp) )compliance|aml|kyc"
			+ "|anti-money[- ]laundering|financial[- ]crimes?|sanctions)";

	/**
	 * Knowledge of a field asked for in its own right, apart from the role's experience: a subject
	 * matter expert, knowledge or understanding of the field, or a physician. Years in the job named
	 * after a field ("five years of marketing analytics") are not among them.
	 */
	private static final Anchored FIELD_KNOWLEDGE = new Anchored(String.join("|",
			"\\b(knowledge|understanding|expertise) (of|in) (the |how )?(\\w+ ){0,2}?" + FIELD + "\\b",
			"\\b(subject[- ]matter|domain) expert(ise)? (in|on|for) (the )?(\\w+ ){0,2}?" + FIELD + "\\b",
			"\\bunderstands? how (\\w+ ){0,2}?" + FIELD + "\\b",
			"\\b(be|is|are) an? (licensed |board[- ]certified |practicing |practising )?physician\\b"),
			"knowledge", "understand", "expert", "physician");

	/**
	 * A field's expertise named as a noun ("payments domain expert", "insurance knowledge"), which a
	 * company also says of itself ("combining technology and trading expertise"), so it counts only
	 * where the sentence speaks to or of the applicant.
	 */
	private static final Anchored FIELD_EXPERTISE = new Anchored(
			"\\b" + FIELD + " (domain |subject[- ]matter )?(expertise|knowledge|expert)\\b", "expert", "knowledge");

	/**
	 * Experience in a science, another engineering, medicine, a financial instrument or compliance,
	 * which is field expertise in itself.
	 */
	private static final Anchored FIELD_EXPERIENCE = new Anchored(String.join("|",
			"\\bexperience (in|with|on|analy\\w+|building|developing|pricing) (\\w+ ){0,2}?" + HARD_FIELD + "\\b",
			"\\bphysics-based (modell?ing|simulations?)\\b",
			"\\b" + COMPLIANCE + "( (or|and) " + COMPLIANCE + "|/" + COMPLIANCE + ")? experience\\b",
			"\\bexperience in (\\w+ ){0,1}?" + COMPLIANCE + "\\b"), "biolog", "genomics", "physics", "rf",
			"radio frequency", "satcom", "powertrain", "avionics", "flight control", "medical", "medicare", "medicaid",
			"icd", "derivative", "compliance", "aml", "kyc", "anti-money", "financial crime", "financial-crime",
			"sanctions");

	/**
	 * A computing degree, the first form of a stated software background (Q4). The other computing
	 * fields also name departments and skills, so they count only beside a degree.
	 */
	private static final Anchored SOFTWARE_DEGREE = new Anchored(String.join("|",
			"\\b(computer science|software engineering|computer engineering)\\b",
			DEGREE + ".{0,120}?\\b(information technology|information systems|data science|cyber ?security"
					+ "|networking and telecommunications|telecommunications|network (engineering|administration)"
					+ "|systems administration)\\b"),
			"computer science", "software engineering", "computer engineering", "information technology",
			"information systems", "data science", "cybersecurity", "cyber security", "networking", "telecommunications",
			"network engineering", "network administration", "systems administration");

	/** The computing fields a degree list can accept. */
	private static final Pattern COMPUTING_FIELD = Pattern.compile("\\b(computer science|software engineering"
			+ "|computer engineering|information technology|information systems|computing|data science|cyber ?security"
			+ "|networking|telecommunications|systems administration)\\b");

	/**
	 * The fields outside computing a degree list can also accept, which make a software background
	 * one way into the role among others. Engineering without a discipline is one, read apart from
	 * the engineering teams and roles a sentence names beside a degree.
	 */
	private static final Pattern OTHER_FIELD = Pattern.compile(String.join("|",
			"\\b(business|finance|economics|marketing|communications?|accounting|humanities|psychology|physics|math\\w*"
					+ "|statistics|stem|quantitative|policy)\\b",
			"(?<!software |computer )\\bengineering\\b(?! (teams?|managers?|leaders?|leadership|org\\w*|department"
					+ "|culture|practices|experience|background|roles?|partners?))"));

	/**
	 * The other forms of a stated software background (Q4): a programming language by name, SQL,
	 * cloud, container, infrastructure or networking skills, delivery pipelines and the protocols of
	 * networks and their security among them, and technical experience in a field that is software,
	 * distributed systems among them.
	 * Azure's boards, which only track work, are not among them, nor is a web address.
	 */
	private static final Anchored SOFTWARE_SKILL = new Anchored(String.join("|",
			"\\b(python|java|javascript|typescript|golang|ruby|rust|kotlin|scala|php|perl|bash|powershell|matlab"
					+ "|verilog|systemverilog|vhdl)\\b",
			"(?<!\\w)(c\\+\\+|c#|\\.net)(?!\\w)", "\\b(sql|mysql|postgresql|postgres)\\b",
			"\\bazure\\b(?! devops| boards)",
			"\\b(aws|gcp|google cloud|kubernetes|docker|terraform|linux|tcp/ip|ci/cd)\\b",
			"\\bhttps?\\b(?!:)", "\\b(tls|ssl|pki|dns|bgp|vlans?|vpns?|firewalls?|ssh|proxies|proxy servers?)\\b",
			"\\b(observability|developer tools|devtools|distributed systems)\\b"), "python", "java", "typescript",
			"golang", "ruby", "rust", "kotlin", "scala", "php", "perl", "bash", "powershell", "matlab", "verilog",
			"systemverilog", "vhdl", "c++", "c#", ".net", "sql", "mysql", "postgres", "aws", "azure", "gcp",
			"google cloud", "kubernetes", "docker", "terraform", "linux", "tcp/ip", "ci/cd", "http", "tls", "ssl", "pki",
			"dns", "bgp", "vlan", "vpn", "firewall", "ssh", "prox", "observability", "developer tools", "devtools",
			"distributed systems");

	/** Version control, a software skill unless it only tracks work or stores documents. */
	private static final Anchored VERSION_CONTROL = new Anchored("\\b(github|gitlab|git)\\b", "git");

	/** What a sentence names when a tool tracks work or publishes documents in it. */
	private static final Pattern TRACKS_OR_PUBLISHES = Pattern.compile("\\b(jira|confluence|asana|trello|track(s|ed|ing)?(?! record)"
			+ "|content|documentation|docs|markdown|publish\\w*|cms|wiki)\\b");

	/**
	 * What a sentence says when it asks the applicant for something, rather than describing the
	 * company or what it offers.
	 */
	private static final Pattern ASKED_FOR = Pattern.compile("\\b(experience\\w*|proficien\\w*|knowledge|familiar\\w*"
			+ "|degree|bachelor\\S*|master\\S*|skill\\w*|background|understanding|expertise|years|requir\\w*|must"
			+ "|hands-on|ability|fluen\\w*|comfortable|exposure|preferred|qualifications|you will|you'll|you have)\\b");

	/** What a sentence says when the role does not need what it names. */
	private static final Pattern NOT_NEEDED = Pattern.compile("\\b(don['’]t|do not|doesn['’]t|does not|won['’]t|will not"
			+ "|no) (need|require|have) to\\b|\\bnot required\\b|\\bno (coding|programming|code)\\b"
			+ "|\\bwithout (writing )?code\\b");

	/** What a sentence says when the role does not write code itself. */
	private static final Pattern DOES_NOT_CODE = Pattern.compile("\\b(don['’]t|do not|doesn['’]t|does not|won['’]t"
			+ "|will not|never) (write|code|program|build)\\b");

	/** The company speaking of itself. */
	private static final Pattern COMPANY_VOICE = Pattern.compile("\\b(we|we're|we’re|our|us)\\b");

	/** What a sentence says when it speaks to, or of, the applicant. */
	private static final Pattern APPLICANT = Pattern.compile("\\b(you|your|you'll|you’ll|candidates?|applicants?"
			+ "|requirements?|qualifications|must|required|preferred|years|someone|ideal)\\b");

	/**
	 * Tools that are not software skills. A software skill offered as one choice among them ("Excel,
	 * SQL or Looker") can be met without it.
	 */
	private static final Pattern NON_SOFTWARE_TOOL = Pattern.compile("\\b(excel|spreadsheets?|bi tools?|looker|tableau"
			+ "|power bi|stata|sas|spss|alteryx|qlik|google sheets)\\b");

	private static final Pattern OR = Pattern.compile("\\bor\\b");

	private static final Pattern AND = Pattern.compile("\\band\\b");

	/**
	 * Code as the role's artifact (Q2) and the engineer-facing work of Q3 — source, API definitions
	 * and calls, SDKs, logs, test suites and the gates of a delivery pipeline. Software alone is not
	 * among them: a company says it builds software.
	 */
	private static final Anchored CODE_WORK = new Anchored(String.join("|",
			"\\b(writ|develop|build|implement|ship)\\w*( [\\w,'+#]+){0,4}? (code|microservices|backend services|firmware"
					+ "|scripts)\\b",
			"\\b(source code|review(ing)? (the )?code|code reviews?|application logs|system logs)\\b",
			"\\b(rest(ful)? )?apis? (specifications?|definitions?|design|documentation|calls|requests)\\b",
			"\\b(webhooks?|sdks?|code samples?)\\b",
			"\\b(build|develop|writ|implement|creat|maintain|automat)\\w*( [\\w,'/-]+){0,4}? (test suites?"
					+ "|automated tests?|test automation|quality gates?|ci/cd pipelines?)\\b",
			"\\b(build|develop|train|deploy|ship|productioni[sz])\\w*( [\\w,'-]+){0,4}? (machine learning|ml|deep learning"
					+ "|ai|llm) (models?|systems|pipelines)\\b"),
			"code", "microservices", "backend services", "firmware", "scripts", "logs", "api", "webhook", "sdk",
			"test suite", "automated test", "test automation", "quality gate", "ci/cd", "machine learning model",
			"machine learning system", "machine learning pipeline", "ml model", "ml system", "ml pipeline",
			"deep learning model", "deep learning system", "deep learning pipeline", "ai model", "ai system",
			"ai pipeline", "llm model", "llm system", "llm pipeline");

	/** Code work named in another language: programming, software development, code and coding. */
	private static final Anchored FOREIGN_CODE_WORK = new Anchored(
			"\\b(programmier\\w*|programaci[oó]n|programa[cç][aã]o|programmation|softwareentwicklung|software-entwicklung"
					+ "|desarrollo de software|d[ée]veloppement (logiciel|de logiciels)|desenvolvimento de software)\\b"
					+ "|(코드|코딩|프로그래밍)",
			"programmier", "programaci", "programaç", "programac", "programmation", "softwareentwicklung",
			"software-entwicklung", "desarrollo de software", "développement", "developpement",
			"desenvolvimento de software", "코드", "코딩", "프로그래밍");

	/** Where a description names a degree, in another language or in English: a field of study, not the role's work. */
	private static final Pattern FOREIGN_DEGREE = Pattern.compile("\\b(carreras?|licenciatura|t[ií]tulo|grado en"
			+ "|graduad[oa]|studium|abschluss|dipl[ôo]me|forma[cç][aã]o|gradua[cç][aã]o|degree)\\b");

	/**
	 * Code someone other than the role writes: a team or its engineers, or the work that comes before
	 * any code.
	 */
	private static final Pattern NOT_THE_ROLES_CODE = Pattern.compile("\\bbefore (any |writing |a line of )?code"
			+ "|\\bbefore (writing|coding)\\b|\\b(team|engineers|developers)( [\\w-]+){0,3}? (who|that) (will )?(write"
			+ "|build|develop)");

	/**
	 * Architecture calls, technical debt paid down, RFCs and a codebase, which are the role's own work
	 * only when the role makes them rather than sits beside the engineers who do.
	 */
	private static final Anchored DESIGN_WORK = new Anchored(String.join("|",
			"\\b(software architecture|architecture decisions|architectural decisions|architecture reviews?|codebase"
					+ "|rfcs|(technical|design) rfcs?)\\b",
			"\\b(writ|author|review)\\w* (\\w+ ){0,2}?rfcs?\\b",
			"\\b(pay|pays|paying|reduc\\w*|address\\w*|tackl\\w*|own\\w*|refactor\\w*)( down)?( [\\w-]+){0,2}? "
					+ "tech(nical)? debt\\b"),
			"architecture", "architectural", "codebase", "rfc", "debt");

	/** Business systems, whose architecture is a matter of finance and tax rather than software. */
	private static final Pattern BUSINESS_SYSTEMS = Pattern.compile("\\b(business systems|erp|tax|finance systems"
			+ "|financial systems|accounting systems)\\b");

	/**
	 * A sentence that has the role beside the engineers who do the work, taking part in their
	 * discussions, or ramping on it.
	 */
	private static final Pattern BESIDE_ENGINEERS = Pattern.compile(String.join("|",
			"\\b(with|alongside|partner\\w*|collaborat\\w*) (the |our |your )?([\\w-]+ ){0,2}?(engineers|engineering"
					+ "|developers)\\b",
			"\\b(participat|join|attend|contribut)\\w* (in |to )?([\\w-]+ ){0,3}?discussions?\\b", "\\bramp\\w* (up )?on\\b"));

	/**
	 * Where code and its review are a matter of law or buildings: an export-control notice names
	 * source code among what the law restricts, a dispute reviews it, a building is held to one.
	 */
	private static final Pattern NOT_SOFTWARE_WORK = Pattern.compile("\\b(export\\w*|litigation|disputes?|legal"
			+ "|expert witness|building codes?|code compliance)\\b");

	/** The verbs of building something, as verbs: an implementation or a rollout is a project, not work. */
	private static final String BUILDS = "build|builds|building|built|develop|develops|developed|developing|implement"
			+ "|implements|implemented|implementing|automate|automates|automated|automating|integrate|integrates"
			+ "|integrated|integrating";

	/**
	 * A role that builds applications, automations or integrations in a low-code builder, an ERP or a
	 * quality system, or integrates a CRM or marketing-automation platform. Reports and dashboards
	 * built in one are not among them.
	 */
	private static final Anchored LOW_CODE_WORK = new Anchored(String.join("|",
			"\\b(" + BUILDS + "|design|designs|designed|designing|configure|configures"
					+ "|configured|configuring|customi[sz](e|es|ed|ing))"
					+ "( (?!reports?\\b|reporting\\b|dashboards?\\b)[\\w,'/-]+){0,6}? (power apps|powerapps|power automate"
					+ "|dynamics 365|pega|outsystems|mendix|appian|servicenow|salesforce flows?|apex|netsuite|sap|erp"
					+ "|zapier|workato|n8n|clay|tray\\.io|quality management system)\\b",
			"\\b(" + BUILDS + ")( (?!reports?\\b|reporting\\b|dashboards?\\b|records?\\b)[\\w,'/-]+){0,6}? (salesforce"
					+ "|marketo|hubspot|dynamics (365|crm)|eloqua|pardot)\\b"),
			"power apps", "powerapps", "power automate", "dynamics", "pega", "outsystems", "mendix", "appian",
			"servicenow", "salesforce", "apex", "netsuite", "sap", "erp", "zapier", "workato", "n8n", "clay", "tray.io",
			"quality management system", "marketo", "hubspot", "eloqua", "pardot");

	/**
	 * A low-code system the role only names: it runs a project or a rollout of it, works with IT on
	 * it, or automates in it now and then.
	 */
	private static final Pattern NAMES_A_SYSTEM = Pattern.compile("\\b(rollouts?|roll-outs?|programme|program manag\\w*"
			+ "|project (team|manag\\w*)|sometimes|occasionally|from time to time)\\b"
			+ "|\\b(with|alongside|partner\\w* with|collaborat\\w* with|support\\w*) (the |our )?it\\b");

	/**
	 * A manager who leads software engineers, which is equivalent technical experience under Q4.
	 * Working with engineers, or managing relationships with them, is not leading them.
	 */
	private static final Anchored LEADS_ENGINEERS = new Anchored(String.join("|",
			"\\b(lead|leads|leading|manage|manages|managing|mentor\\w*|hire|hiring|grow|growing)( (?!with\\b)[\\w,'-]+){0,4}? "
					+ "(software|backend|back-end|frontend|front-end|full[- ]stack|mobile|platform|data|ml|machine learning"
					+ "|devops|infrastructure|site reliability) (engineers|developers|engineering teams?)\\b",
			"\\bstaff[- ]engineer[- ]level\\b"), "engineers", "developers", "engineering team", "engineer-level",
			"engineer level");

	/**
	 * A physical product a {@code domain_ambiguity} role designs, integrates, tests or certifies:
	 * aircraft, rockets, satellites, weapons, vehicles, robots, chips, building systems. It is another
	 * domain unless the role's own work on it is software.
	 */
	private static final Anchored PRODUCT_WORK = new Anchored(String.join("|",
			"\\b(design|integrat|test|qualif|certif|validat|verif|assembl|build|manufactur|calibrat|inspect|evaluat"
					+ "|commission|install|maintain|troubleshoot)\\w*( [\\w,'/-]+){0,5}? (aircraft|airframes?|rockets?"
					+ "|launch vehicles?|spacecraft|satellites?|missiles?|munitions|weapons?|vehicles?|powertrains?"
					+ "|emissions|robots?|chips?|semiconductors?|asics?|wafers?|building automation|building systems"
					+ "|hvac systems?|manufacturing execution systems?|plant execution systems?)\\b",
			"\\b(aircraft|rocket|launch vehicle|spacecraft|satellite|missile|weapons?|vehicle|powertrain|emissions|robot"
					+ "|chip|semiconductor|asic|wafer)s? (design|integration|test(ing)?|qualification|certification"
					+ "|validation|verification|assembly|calibration|inspection)\\b",
			"\\bnon-?destructive (evaluation|testing|inspection)\\b"),
			"aircraft", "airframe", "rocket", "launch vehicle", "spacecraft", "satellite", "missile", "munitions",
			"weapon", "vehicle", "powertrain", "emissions", "robot", "chip", "semiconductor", "asic", "wafer",
			"building automation", "building system", "hvac", "manufacturing execution", "plant execution",
			"non-destructive", "nondestructive");

	/**
	 * A product's own software: its code, its software tests, its software's architecture, or the
	 * testbenches that verify its design.
	 */
	private static final Anchored PRODUCT_SOFTWARE = new Anchored(String.join("|",
			"\\b(writ|develop|build|design|implement|architect)\\w*( [\\w,'+#/-]+){0,3}? software\\b",
			"\\bsoftware (tests?|testing|architecture|verification)\\b", "\\b(uvm|testbench\\w*|verification code)\\b"),
			"software", "uvm", "testbench", "verification code");

	/** Code written to analyse, model or simulate a product, which does not make its work software. */
	private static final Pattern ANALYSIS = Pattern.compile("\\b(analy\\w*|simulat\\w*|modell?ing|post-process\\w*"
			+ "|data reduction)\\b");

	/**
	 * Work a {@code domain_ambiguity} role is given that could be software or not — control, embedded,
	 * test or automation systems maintained, designed or programmed — so the title's doubt stays open.
	 * The word alone is no such work: a company names automation and simulation of itself. Version
	 * control is software, and a requirement is not a duty.
	 */
	private static final Anchored WORK_EITHER_WAY = new Anchored(
			"\\b(maintain|troubleshoot|design|develop|program|commission|integrat|support|test)\\w*( [\\w,'-]+){0,3}? "
					+ "(?<!version )(control|embedded|test|automation) systems?\\b",
			"control", "embedded", "test", "automation");

	/** Control systems that are a company's financial controls, not machines or code. */
	private static final Pattern FINANCIAL_CONTROLS = Pattern.compile("\\b(internal control|financial|sox|compliance)");

	/** What a requirement says, as opposed to a duty. */
	private static final Pattern REQUIREMENT = Pattern.compile("\\b(experience|degree|knowledge|familiar\\w*"
			+ "|proficien\\w*|background|years)\\b");

	/**
	 * A domain outside software that a {@code domain_ambiguity} description names: an engineering
	 * discipline, its tools or its sites. Work that could be software or not is no doubt there.
	 */
	private static final Pattern OTHER_DOMAIN = Pattern.compile("\\b(mechanical|electrical|civil|structural|chemical"
			+ "|geotechnical|cad|solidworks|autocad|machining|welding|piping|process equipment|hvac|construction|plant"
			+ "|pcb|highways?|substation)\\b");

	/** What an item says of itself when it is only a plus. "Bonus" alone is also pay. */
	private static final Pattern PLUS = Pattern.compile("\\b(an? (\\w+ )?plus|nice[- ]to[- ]haves?|bonus points|an? bonus"
			+ "|any history of|un plus|un atout|von vorteil|wünschenswert|deseable|valorable|diferencial)\\b");

	/** A heading that makes every item under it a plus. */
	private static final Pattern PLUS_HEADING = Pattern.compile("\\s*(nice[- ]to[- ]haves?|bonus points|pluses"
			+ "|desirable|desired (skills|qualifications|experience))\\b");

	private static final Pattern PREFERRED = Pattern.compile("\\bpreferred\\b");

	/** A heading that makes every item under it preferred. */
	private static final Pattern PREFERRED_HEADING = Pattern.compile("\\s*preferred (qualifications|skills|experience"
			+ "|requirements)\\b");

	/**
	 * Decides one vacancy of the pile.
	 * @param cleanedTitle its cleaned title
	 * @param titleReason why its title was left unknown, which says what doubt the description settles
	 * @param segments its description as cleaning cut it, none if it has none
	 * @return {@code IN} or {@code OUT} where the description settles it, {@code UNKNOWN} otherwise
	 */
	public ClassificationStateEnum classify(String cleanedTitle, UnknownReasonEnum titleReason,
			List<Segment> segments) {
		List<Around> description = new ArrayList<>(segments.size());
		int length = 0;
		for (Segment segment : segments) {
			if (!segment.boilerplate()) {
				description.add(Around.of(segment));
				length += segment.text().length() + 1;
			}
		}
		if (length < SHORTEST_DESCRIPTION_OF_WORK) {
			return ClassificationStateEnum.UNKNOWN;
		}
		if (anyMention(description, NO_VACANCY, (around) -> true)
				|| anyMention(description, CARRIES_A_QUOTA, (around) -> true)
				|| requiresExpertiseOutsideSoftware(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY
				&& anyMention(description, PRODUCT_WORK, (around) -> !companyOnly(around.item()))
				&& !writesTheProductsSoftware(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (statesASoftwareBackground(description)) {
			return ClassificationStateEnum.IN;
		}
		if (titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY
				&& anyMention(description, WORK_EITHER_WAY,
						(around) -> !FINANCIAL_CONTROLS.matcher(around.item()).find()
								&& !REQUIREMENT.matcher(around.item()).find())
				&& description.stream().noneMatch((around) -> OTHER_DOMAIN.matcher(around.item()).find())) {
			return ClassificationStateEnum.UNKNOWN;
		}
		return ClassificationStateEnum.OUT;
	}

	/**
	 * Whether the description requires expertise outside software. Expertise it lists as a plus,
	 * prefers or claims for the company is no requirement, nor is a field's degree offered beside a
	 * computing one, nor a science offered among the fields of a quantitative list.
	 */
	private static boolean requiresExpertiseOutsideSoftware(List<Around> description) {
		return anyMention(description, FIELD_DEGREE,
				(around) -> required(around) && !COMPUTING_FIELD.matcher(around.item()).find())
				|| anyMention(description, SCIENCE_DEGREE,
						(around) -> required(around) && !COMPUTING_FIELD.matcher(around.item()).find()
								&& !QUANTITATIVE_FIELD.matcher(around.item()).find())
				|| anyMention(description, FIELD_KNOWLEDGE, (around) -> required(around) && !companyOnly(around.item()))
				|| anyMention(description, FIELD_EXPERTISE,
						(around) -> required(around) && APPLICANT.matcher(around.item()).find())
				|| anyMention(description, FIELD_EXPERIENCE, (around) -> required(around) && !companyOnly(around.item()));
	}

	private static boolean required(Around around) {
		return !aPlus(around) && !PREFERRED.matcher(around.item()).find()
				&& !PREFERRED_HEADING.matcher(around.heading()).lookingAt();
	}

	/**
	 * Whether what an item names is only a plus: the item says so, or it sits under a heading that
	 * does. A plus said of the item before it is not said of this one.
	 */
	private static boolean aPlus(Around around) {
		return PLUS.matcher(around.item()).find() || PLUS_HEADING.matcher(around.heading()).lookingAt();
	}

	/**
	 * Whether the role's own work on the physical product it works on is software: code that is not
	 * written to analyse, model or simulate the product, its software tests or its software's
	 * architecture.
	 */
	private static boolean writesTheProductsSoftware(List<Around> description) {
		return anyMention(description, CODE_WORK,
				(around) -> ownCodeWork(around) && !ANALYSIS.matcher(around.item()).find())
				|| anyMention(description, PRODUCT_SOFTWARE, (around) -> !aPlus(around) && !companyOnly(around.item())
						&& !ANALYSIS.matcher(around.item()).find());
	}

	/**
	 * Whether the description requires or prefers a software background, or has the role do software
	 * work: code, engineer-facing artifacts, low-code building or leading software engineers.
	 */
	private static boolean statesASoftwareBackground(List<Around> description) {
		return anyMention(description, CODE_WORK, BodyClassificationRule::ownCodeWork)
				|| anyMention(description, FOREIGN_CODE_WORK,
						(around) -> ownCodeWork(around) && !FOREIGN_DEGREE.matcher(around.item()).find())
				|| anyMention(description, DESIGN_WORK,
						(around) -> !aPlus(around) && !companyOnly(around.item())
								&& !BESIDE_ENGINEERS.matcher(around.item()).find()
								&& !BUSINESS_SYSTEMS.matcher(around.item()).find())
				|| anyMention(description, LOW_CODE_WORK,
						(around) -> !aPlus(around) && !NAMES_A_SYSTEM.matcher(around.item()).find())
				|| anyMention(description, LEADS_ENGINEERS, (around) -> !aPlus(around))
				|| anyMention(description, SOFTWARE_SKILL,
						(around) -> askedFor(around) && !oneChoiceAmongNonSoftwareTools(around.item()))
				|| anyMention(description, VERSION_CONTROL,
						(around) -> askedFor(around) && !TRACKS_OR_PUBLISHES.matcher(around.item()).find())
				|| anyMention(description, SOFTWARE_DEGREE,
						(around) -> askedFor(around) && !otherFieldAccepted(around.item()));
	}

	/**
	 * Whether code work is the role's own: not a plus, not something the role is said not to do, not
	 * a matter of law or buildings, not the company's mission and not a team's code the role sits
	 * before or beside.
	 */
	private static boolean ownCodeWork(Around around) {
		String item = around.item();
		return !aPlus(around) && !NOT_NEEDED.matcher(item).find() && !DOES_NOT_CODE.matcher(item).find()
				&& !NOT_SOFTWARE_WORK.matcher(item).find()
				&& !companyOnly(item) && !NOT_THE_ROLES_CODE.matcher(item).find();
	}

	/**
	 * Whether an item asks the applicant for what it names: it asks for something, or sits under a
	 * heading that does, does not call it a plus or say the role does not need it, and is not the
	 * company speaking of itself.
	 */
	private static boolean askedFor(Around around) {
		String item = around.item();
		return (ASKED_FOR.matcher(item).find() || ASKED_FOR.matcher(around.heading()).find()) && !aPlus(around)
				&& !NOT_NEEDED.matcher(item).find()
				&& !companyOnly(item);
	}

	private static boolean companyOnly(String item) {
		return COMPANY_VOICE.matcher(item).find() && !APPLICANT.matcher(item).find();
	}

	/**
	 * Whether a skill is offered as one choice among tools that are not software skills. A list that
	 * joins it to them with "and" asks for it beside them.
	 */
	private static boolean oneChoiceAmongNonSoftwareTools(String item) {
		return NON_SOFTWARE_TOOL.matcher(item).find() && OR.matcher(item).find() && !AND.matcher(item).find();
	}

	/** Whether a degree list also accepts a field outside computing. */
	private static boolean otherFieldAccepted(String item) {
		return OTHER_FIELD.matcher(item).find();
	}

	/**
	 * Whether any mention of a pattern is one that counts, read with the segment it is in. A segment is
	 * searched only when it holds one of the pattern's anchors at the start of a word.
	 */
	private static boolean anyMention(List<Around> description, Anchored mention, Predicate<Around> counts) {
		for (Around around : description) {
			if (mention.anchoredIn(around.item()) && mention.pattern().matcher(around.item()).find()
					&& counts.test(around)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What a mention is read with.
	 *
	 * @param item the segment it is in, lowercased: a sentence, a list item or a heading
	 * @param heading the heading that segment sits under, lowercased, empty if there is none
	 */
	private record Around(String item, String heading) {

		static Around of(Segment segment) {
			return new Around(segment.text().toLowerCase(Locale.ROOT),
					(segment.under() != null) ? segment.under().toLowerCase(Locale.ROOT) : "");
		}

	}

	/**
	 * A pattern searched only in a segment holding one of its anchors: literal words, one of which every
	 * match of it contains at the start of a word. Segments are scanned for the anchors and the pattern
	 * is tried only on those that hold one, since trying an alternation at every position of every
	 * description is what made a pass over the pile slow.
	 *
	 * @param pattern what a mention looks like, over a lowercased segment
	 * @param anchors literal words one of which every match contains at the start of a word
	 */
	private record Anchored(Pattern pattern, List<String> anchors) {

		Anchored(String pattern, String... anchors) {
			this(Pattern.compile(pattern), List.of(anchors));
		}

		/** Whether a segment holds one of the anchors at the start of a word. */
		boolean anchoredIn(String item) {
			for (String anchor : this.anchors) {
				for (int at = item.indexOf(anchor); at >= 0; at = item.indexOf(anchor, at + 1)) {
					if (at == 0 || !Character.isLetterOrDigit(item.charAt(at - 1))) {
						return true;
					}
				}
			}
			return false;
		}

	}

}
