package oneprofile.backend.workers.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import oneprofile.backend.storage.taxonomy.Skill;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
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
 * company's text, not the role's, and is not read. The skills skill extraction found are read the same
 * way, by the segment they were found in: a software skill asked of the applicant is the plainest
 * software background a description states (ADR-0017), and a platform the role configures or automates
 * is low-code work. The whole of it is plain matching over the lowercased segments, each pattern tried
 * only on a segment holding the literal words it needs, which keeps a pass over the pile inside the
 * time ADR-0012 allows it.
 */
@Component
public class BodyClassificationRule {

	/**
	 * The kinds of skill in the taxonomy that are a software background when asked for: languages, their
	 * frameworks, databases, cloud, delivery, networks, operating systems, testing, security, machine
	 * learning and data. A tool is not among them: the kind holds trackers, CRMs and ERPs beside editors.
	 */
	private static final Set<String> SOFTWARE_KINDS = Set.of("language", "framework", "database", "cloud", "devops",
			"networking", "os", "testing", "security", "ml", "data");

	/**
	 * The kinds of skill a role can build in without code: tools, ERPs, CRMs and automation builders
	 * among them, and the platforms that build agents.
	 */
	private static final Set<String> PLATFORM_KINDS = Set.of("tool", "ml");

	/**
	 * Reporting and analytics products, built for non-engineers. A skill offered as one choice among them
	 * can be met without the software skill ("Snowflake, ThoughtSpot or Excel").
	 */
	private static final Set<String> REPORTING_PRODUCTS = Set.of("adobe-analytics", "alteryx", "amazon-quicksight",
			"amplitude", "apache-superset", "crystal-reports", "domo", "google-analytics", "google-looker-studio",
			"ibm-cognos", "jmp", "knime", "lightdash", "looker", "metabase", "microstrategy", "mixpanel", "obiee",
			"oracle-analytics-cloud", "power-bi", "qlik", "qlikview", "redash", "sap-analytics-cloud",
			"sap-businessobjects", "sas", "sisense", "spotfire", "tableau", "tableau-prep", "thoughtspot");

	/**
	 * Chat assistants, the models a person talks to, the assistants that write code for them and the company
	 * named for its assistants (OpenAI), which using is no software background: a recruiter or a marketer is
	 * asked for them as tools of the trade.
	 */
	private static final Set<String> ASSISTANTS = Set.of("chatgpt", "claude", "claude-haiku", "claude-opus",
			"claude-sonnet", "deepseek", "elevenlabs", "google-gemini", "gpt", "kimi", "kling", "minimax", "qwen", "sora",
			"claude-code", "openai-codex", "cursor", "github-copilot", "amazon-codewhisperer", "windsurf", "tabnine",
			"replit", "openai");

	/**
	 * Skills of a software kind that ask for no software background all the same: reporting products,
	 * assistants, the assistants built into business suites, data catalogues, the operating systems
	 * of consumer devices, governance, risk and compliance products, the terminologies, file formats,
	 * protocols and tools of a field — imaging, maps, content, drones, vehicle buses, building automation,
	 * marketing suites, animation — and the boards that track work and its test cases.
	 */
	private static final Set<String> NOT_A_SOFTWARE_BACKGROUND = union(REPORTING_PRODUCTS, ASSISTANTS,
			Set.of("adobe-experience-platform", "bi-publisher", "birt", "directquery", "gameanalytics", "heap", "hex",
					"hotjar", "jaspersoft", "oracle-essbase", "otbi", "power-query", "powerpivot", "salesforce-crm-analytics",
					"sql-server-reporting-services", "suiteanalytics", "tm1", "workday-prism", "amazon-q", "atlassian-rovo",
					"microsoft-copilot-studio", "salesforce-agentforce", "salesforce-einstein", "sap-joule", "agentspace",
					"collibra", "alation", "atlan", "microsoft-purview", "microsoft-windows", "macos", "ios", "ipados",
					"android", "chromeos", "tvos", "watchos", "android-tv", "android-auto", "webos", "tizen",
					"windows-mobile", "drata", "vanta", "hyperproof", "rsa-archer", "xacta", "emass", "snomed-ct", "loinc",
					"rxnorm", "sdtm", "define-xml", "dicom", "kml", "kmz", "contentful", "optimizely", "ardupilot", "px4",
					"arcsde", "canbus", "can-fd", "canopen", "dnp3", "bacnet", "modbus", "modbus-tcp",
					"adobe-experience-manager", "aem-as-a-cloud-service", "aem-edge-delivery-services", "lottie",
					"azure-devops", "testrail", "qase", "qmetry"));

	/**
	 * Skills of the platform kinds that track work, hold documents or content or draw designs, which
	 * configuring is administration rather than building, and the assistants.
	 */
	private static final Set<String> NOT_A_PLATFORM = union(ASSISTANTS, Set.of("optimizely","jira", "jira-product-discovery",
			"jira-service-management", "confluence", "figma", "adobe-xd", "github-issues", "github-projects", "bugzilla",
			"redmine", "youtrack", "mantisbt", "versionone", "codebeamer", "jama", "polarion", "ibm-doors", "hp-alm",
			"markdown", "latex", "mediawiki", "microsoft-sharepoint", "nextcloud", "mintlify", "mkdocs",
			"gitlab-service-desk"));

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
	 * What a description says when the role sells: it carries a quota, owns a book of business, or is
	 * measured on meeting a quota or a sales target. A role that plans or tracks quotas for others, or
	 * works beside those who carry them, does not sell.
	 */
	private static final Anchored CARRIES_A_QUOTA = new Anchored(String.join("|",
			"\\b(as|be|is|are) an? quota[- ]carrying\\b",
			"(?<!\\bnot )\\bcarr(y|ies|ying) (a|an|your|the)( \\w+){0,2} quota",
			"\\b(achiev|attain|meet|exceed|hit)\\w* (your |the |an? )?(\\w+ ){0,2}quota",
			"\\b(own|owns|owning|grow|grows|growing|build|builds|building) (a |an |your |the )?(\\w+ ){0,2}?"
					+ "book of business\\b",
			"\\b(achiev|attain|meet|exceed|hit)\\w* (your |the |an? )?(\\w+ ){0,2}?(sales|revenue|bookings?|arr) "
					+ "(targets?|goals?)\\b"),
			"quota", "book of business", "sales", "revenue", "booking", "arr");

	/** A heading over what the company pays and offers, where a revenue target is how commission is earned. */
	private static final Pattern BENEFITS_HEADING = Pattern.compile("\\b(benefits|perks|compensation|pay|rewards"
			+ "|what we offer|we offer)\\b");

	/**
	 * What a sentence says when a quota is copy about the product rather than the role's measure: the deals
	 * don't just hit quota, they prove something. One that goes on to exceed it is still selling.
	 */
	private static final Pattern NOT_JUST_A_QUOTA = Pattern.compile("\\b(don['’]t|doesn['’]t|do not|does not|not) just "
			+ "(hit|meet|exceed)\\w* (the |your )?quota\\b(?!,? (you|but)\\b)");

	/** What a sentence says when the role helps others sell or meet their numbers, rather than selling. */
	private static final Pattern HELPS_OTHERS_SELL = Pattern.compile("\\b(help\\w*|support\\w*|enabl\\w*|empower\\w*"
			+ "|coach\\w*|equip\\w*)\\b");

	/**
	 * Where a description names a degree. "Master" alone is left out: it is also a Scrum Master's
	 * title, so a master's counts only as "master's in" or "master's of".
	 */
	private static final String DEGREE = "\\b(degree|bachelor\\S*|b\\.?s\\.?c?|b\\.a|m\\.?s\\.?c?|ph\\.?d"
			+ "|master['’]?s (in|of))\\b";

	/** The engineering disciplines other than software. */
	private static final String OTHER_DISCIPLINE = "(mechanical|civil|electrical|chemical|structural|aerospace|industrial"
			+ "|manufacturing|biomedical|nuclear|petroleum|materials|environmental)";

	/**
	 * Expertise outside software a software applicant would lack: a degree in another engineering
	 * discipline, before or after the word degree, or a licence or qualification in a field.
	 */
	private static final Anchored FIELD_DEGREE = new Anchored(String.join("|",
			DEGREE + ".{0,60}?\\b" + OTHER_DISCIPLINE + " engineering\\b",
			"\\b" + OTHER_DISCIPLINE + " engineering (degree|bachelor\\S*|b\\.?s\\.?c?)\\b", "\\b(bsee|msee|bsme|msme)\\b",
			"\\bp\\.?e\\.? (license|licensure|registration)", "\\bprofessional engineer(ing)? (\\(pe\\) )?licen",
			"\\bregistered nurse\\b", "\\blicensed (attorney|pharmacist|physician|nurse|clinician)",
			"\\bcpa (license|certification)", "\\bactuarial (exams?|credentials?|designation)"), "mechanical", "civil",
			"electrical", "chemical", "structural", "aerospace", "industrial", "manufacturing", "biomedical", "nuclear",
			"petroleum", "materials", "environmental", "bsee", "msee", "bsme", "msme", "actuarial", "licen",
			"registration", "registered nurse", "cpa");

	/** A degree in a subject the role is about, outside engineering. */
	private static final Anchored SUBJECT_DEGREE = new Anchored(DEGREE + ".{0,60}?\\b(finance|accounting|marketing|law"
			+ "|medicine|nursing|pharmacy|actuarial science)\\b", "finance", "accounting", "marketing", "law", "medicine",
			"nursing", "pharmacy", "actuarial");

	/**
	 * What a degree list says when it is open to business, analytical or quantitative fields, or to any degree,
	 * so the subject it also names is one way in among others when it offers them as a choice.
	 */
	private static final Pattern BROAD_DEGREE_LIST = Pattern.compile("\\b(any|business|economics|math\\w*|statistics"
			+ "|quantitative|stem|engineering|analytics|supply chain)\\b");

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
	 * What follows a field's name when it names software in the field instead: its protocols, stacks,
	 * codecs, pipelines or code ("RF protocols", "media codecs", "imaging pipelines").
	 */
	private static final String SOFTWARE_IN_THE_FIELD = "protocols?|stacks?|firmware|software|codecs?|pipelines?|apis?"
			+ "|sdks?|frameworks?|librar(y|ies)|streaming";

	/** Software in a field, after the name of its hardware: its systems are still the hardware. */
	private static final String NOT_ITS_SOFTWARE = "(?! (" + SOFTWARE_IN_THE_FIELD + "))";

	/**
	 * Software in a field, or its systems, platforms, data, technology or applications, after the name
	 * of a field whose systems are software ("healthcare data", "media platforms").
	 */
	private static final String NOT_THE_FIELD_ITSELF = "(?! (" + SOFTWARE_IN_THE_FIELD + "|systems?|platforms?|data|it"
			+ "|technolog\\w*|interoperability|apps?|applications))";

	/**
	 * Radio hardware, which is electrical engineering. Wireless protocols, their stacks and the code
	 * that runs them are networking, so "RF" before them is not the hardware.
	 */
	private static final String RADIO = "(rf|radio frequency)" + NOT_ITS_SOFTWARE;

	/** The standards of a quality system that is procedures and controlled documents, not software. */
	private static final String QUALITY_STANDARD = "(iso 9001|as ?9100|iso 13485|iatf 16949|iso/iec 17025|iso 17025)";

	/**
	 * The fields whose knowledge a description can ask for in its own right, apart from any degree:
	 * the subjects of finance, healthcare, energy and media, the sciences, and the engineering of radios,
	 * vehicles and aircraft.
	 */
	private static final String FIELD = "(financial services|finance|banking|payments|insurance|fraud|financial[- ]crimes?"
			+ "|anti-money[- ]laundering|aml|trading|capital markets|fixed income|investment management|gaap"
			+ "|biolog\\w*|life sciences|genomics|physics|" + RADIO + "|satcom|satellite communications?|powertrain"
			+ "|avionics|flight controls?|pharmac\\w*|pbms?|pharmacy benefits?|healthcare" + NOT_THE_FIELD_ITSELF
			+ "|chemistry|clinical|medical|imaging" + NOT_THE_FIELD_ITSELF + "|tax|legal|accounting|power markets"
			+ "|energy markets|renewable energy|media" + NOT_THE_FIELD_ITSELF + "|quantum (hardware|physics|mechanics)"
			+ "|financial markets|mortgages?|(rlc |analog |electronic |electrical )?circuits?|circuitry|schematics"
			+ "|electronics)";

	/**
	 * The science, engineering, medical and financial fields in which experience is itself field
	 * expertise, and the instruments of electronics.
	 */
	private static final String HARD_FIELD = "(biolog\\w*|genomics|physics\\S*|" + RADIO + "|satcom|powertrain"
			+ "|avionics|flight controls?|medical (terminology|coding)|medicare|medicaid|icd-?\\d+"
			+ "|(equity |interest rate |credit |fx )?derivatives?|mortgages?|oscilloscopes?|multimeters?"
			+ "|(electronic |electrical )?test equipment|circuits?|schematics|soldering)";

	/** The subjects of compliance, whose experience is field expertise for an analyst. */
	private static final String COMPLIANCE = "((?<!(security|soc ?2|cloud|data|it|pci|pci-dss|iso 27001|fedramp) )compliance|aml|kyc"
			+ "|anti-money[- ]laundering|financial[- ]crimes?|sanctions)";

	/**
	 * Knowledge of a field asked for in its own right, apart from the role's experience: a subject
	 * matter expert, knowledge or understanding of the field, or a physician. Years in the job named
	 * after a field ("five years of marketing analytics") are not among them.
	 */
	private static final Anchored FIELD_KNOWLEDGE = new Anchored(String.join("|",
			"\\b(knowledge|understanding|expertise|background)( and [\\w-]+)? (of|in) (the |how )?(\\w+ ){0,2}?" + FIELD
					+ "\\b",
			"\\b(subject[- ]matter|domain) expert(ise)? (in|on|for) (the )?(\\w+ ){0,2}?" + FIELD + "\\b",
			"\\bunderstands? how (\\w+ ){0,2}?" + FIELD + "\\b",
			"\\b(be|is|are) an? (licensed |board[- ]certified |practicing |practising )?physician\\b"),
			"knowledge", "understand", "expert", "background", "physician");

	/**
	 * Knowledge of a subject asked for by its shape rather than by a list of fields: knowledge,
	 * understanding or expertise of the products, markets, industry or regulation of something
	 * ("deep knowledge of fixed income products", "an understanding of power markets"). What it is of
	 * is the qualifier, which counts as a field unless it is software or says nothing of a field.
	 */
	private static final Anchored SUBJECT_KNOWLEDGE = new Anchored(
			"\\b(knowledge|understanding|expertise) (of|in) (the )?(?<subject>([\\w&/-]+ ){1,4}?)"
					+ "(industry|industries|markets?|products|regulations?|regulatory (requirements|frameworks?|environment)"
					+ "|ecosystems?|sector)\\b",
			"knowledge", "understanding", "expertise");

	/**
	 * The qualifiers of a subject that are software or office suites, or that name the company, its customers
	 * or a kind of business rather than a field, and an article left alone ("knowledge of the industry").
	 */
	private static final Pattern NOT_A_FIELD = Pattern.compile("\\b(the|a|an|software|saas|paas|iaas|cloud|data"
			+ "|(fin|ed|mar|ad|dev|sec)?tech\\w*|it|ai|ml|apis?|developer\\w*|devops|network\\w*|security|cyber\\w*|digital|web|mobile|internet|online|computer"
			+ "|computing|engineering|systems?|platforms?|open[- ]source|our|your|their|its|company\\S*|customers?"
			+ "|clients?|business|target|relevant|current|local|global|competitive|b2b|b2c|enterprise|smb|startup|key"
			+ "|new|emerging|related|respective|core|existing|these|this|such|other|various|different|internal|external"
			+ "|user|end-user|product|microsoft|office|google|adobe)\\b");

	/**
	 * A field's expertise named as a noun ("payments domain expert", "a finance background", "finance P&L
	 * knowledge"), which a company also says of itself ("combining technology and trading expertise"), so it
	 * counts only where the sentence speaks to or of the applicant. A software word between the field and the
	 * noun makes it a software background beside the field ("media and technology background").
	 */
	private static final Anchored FIELD_EXPERTISE = new Anchored("\\b" + FIELD
			+ "( (?!(software|tech\\w*|it|data|engineering|systems?|platforms?|products?|computer\\w*|cloud|apis?|sdks?"
			+ "|infrastructure|developer\\w*|tools?|stacks?|protocols?|code|observability)\\b)[\\w&/-]+)"
			+ "{0,3}? (expertise|knowledge|expert|background)\\b", "expert", "knowledge",
			"background");

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
			"icd", "derivative", "mortgage", "oscilloscope", "multimeter", "test equipment", "circuit", "schematic",
			"solder", "compliance", "aml", "kyc", "anti-money", "financial crime", "financial-crime", "sanctions");

	/** The controls of financial reporting, whose compliance is where an auditor's job is done. */
	private static final Pattern SOX_CONTROLS = Pattern.compile("\\b(sox|sarbanes[- ]oxley|itgcs?|it general controls)\\b");

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
					+ "|statistics|stem|quantitative|policy|life sciences|natural sciences|biolog\\w*|biochemistry"
					+ "|chemistry|(program|portfolio|project) management)\\b",
			"(?<!software |computer )\\bengineering\\b(?! (teams?|managers?|leaders?|leadership|org\\w*|department"
					+ "|culture|practices|experience|background|roles?|partners?))"));

	/**
	 * The forms of a stated software background (Q4) the taxonomy holds no skill for: delivery pipelines,
	 * the protocols, topologies and security of networks, the security of applications, APIs and clouds,
	 * data loss prevention and code scanning, disaster recovery and mainframes among infrastructure, and
	 * technical experience in a field that is software, distributed systems among them. Languages, SQL,
	 * cloud, containers and the rest are the skills extraction finds.
	 */
	private static final Anchored SOFTWARE_SKILL = new Anchored(String.join("|", "\\bci/cd\\b",
			"\\b(pki|vpns?|firewalls?|proxies|proxy servers?)\\b", "\\b(disaster recovery|mainframes?)\\b",
			"\\b(observability|developer tools|devtools|distributed systems)\\b",
			"\\b(application|api|cloud|network) security\\b",
			"\\b(data loss prevention|dlp|sast|dast)\\b", "\\bnetwork (topolog\\w*|protocols?|fundamentals)\\b"),
			"ci/cd", "pki", "vpn", "firewall", "prox", "disaster recovery", "mainframe", "observability",
			"developer tools", "devtools", "distributed systems", "security", "data loss", "dlp", "sast", "dast",
			"network");

	/**
	 * Business continuity and emergency planning, whose disaster recovery is a plan for the business
	 * rather than its systems.
	 */
	private static final Pattern BUSINESS_CONTINUITY = Pattern.compile("\\b(business continuity|bcp|crisis"
			+ "|emergency (management|planning|preparedness))\\b");

	/**
	 * A software background asked for in those words: experience as a software engineer or in software
	 * development, a backend developer's background, coding experience. Experience beside software
	 * teams, of how they work, or recruiting or selling to them, is not one.
	 */
	private static final Anchored SOFTWARE_BACKGROUND = new Anchored(String.join("|",
			"\\b(experience|background|years|career)( [\\w-]+){0,3}? (as|in) (an? )?(software|backend|back-end|frontend"
					+ "|front-end|full[- ]stack|web|mobile|application)( \\w+)? (engineer|developer|programmer|engineering"
					+ "|development)s?\\b(?! (teams?|managers?|organi\\w+|partners?|lifecycle|life cycle|process\\w*"
					+ "|methodolog\\w*|practices|recruit\\w*|hiring|talent|sales|marketing))",
			"\\b(software|backend|back-end|full[- ]stack) (engineering|development) (experience|background)\\b",
			"\\b(hands-on|professional|prior|previous) (software|coding|programming) (engineering |development )?"
					+ "experience\\b",
			"\\b(coding|programming) (experience|background)\\b"),
			"software", "backend", "back-end", "frontend", "front-end", "full", "web", "mobile", "application", "coding",
			"programming");

	/**
	 * Version control, a software skill unless it only tracks work or stores documents. GitHub's or GitLab's
	 * own releases are the company's.
	 */
	private static final Anchored VERSION_CONTROL = new Anchored("\\b(github|gitlab|git)\\b(?!['’]s)", "git");

	/**
	 * What a sentence names when a tool tracks work or publishes documents in it, or holds the profile an
	 * application links to.
	 */
	private static final Pattern TRACKS_OR_PUBLISHES = Pattern.compile("\\b(jira|confluence|asana|trello|track(s|ed|ing)?(?! record)"
			+ "|content|documentation|docs|markdown|publish\\w*|cms|wiki|links?|profiles?|portfolios?|urls?)\\b");

	/**
	 * What a sentence says when it asks the applicant for what follows: experience with it, years, proficiency,
	 * knowledge or a degree in it, or the role writing or using it every day. Experience said of anything else
	 * ("a seamless trading experience"), an ability or a duty is not asking for a skill.
	 */
	private static final Pattern ASKED_FOR = Pattern.compile("\\b(experience\\w* (with|in|as|using|of|on|across|working"
			+ "|writing|building|developing)|years|proficien\\w*|knowledge|familiar\\w*|fluen\\w*|expertise|understanding"
			+ "|background|hands-on|skill(s|ed)? (in|with)|exposure|comfortable|competen\\w*|command of|mastery|degree"
			+ "|bachelor\\S*|master['’]?s|qualifications|you have|you bring|you['’]ve|must|requir\\w*|preferred|preferably"
			+ "|ideally|certif\\w*|(you will|you['’]ll|you) (\\w+ ){0,3}?(write|writing|use|using|query|querying|script"
			+ "|scripting|code|coding|program|programming|build|building|develop|developing|design|designing|implement"
			+ "|implementing|configure|configuring|administer|deploy|deploying|automate|automating))\\b"
			+ "|^(write|writing|use|using|query|querying|script|code|program|develop|developing|build|building|deploy"
			+ "|automate)\\b");

	/** What a sentence says after a skill when it asks for it ("SQL is required", "Python preferred"). */
	private static final Pattern ASKED_AFTER = Pattern.compile("\\b(required|preferred|a must|essential|mandatory"
			+ "|necessary)\\b");

	/**
	 * A heading over what the applicant must bring: requirements, qualifications, skills, experience, the
	 * ideal candidate, what is preferred or desired.
	 */
	private static final Pattern REQUIREMENT_HEADING = Pattern.compile("\\b(qualifications?|requirements?|required"
			+ "|requisitos|must[- ]haves?|you (have|bring|need|will need)|you['’]ll (bring|need|have)|who you are|about you"
			+ "|skills|experience|expertise|competenc\\w*|knowledge|looking for|ideal candidate|preferred|desired|desirable"
			+ "|essential|background|profile|perfil|profil|anforderungen|qualifikation\\w*)\\b");

	/**
	 * The skills the criterion names as a software background in themselves, which an acronym says as
	 * plainly as the name.
	 */
	private static final Set<String> NAMED_BY_THE_CRITERION = Set.of("sql");

	/** A short acronym in capitals, as a segment writes it. */
	private static final Pattern ACRONYM = Pattern.compile("\\p{Lu}[\\p{Lu}\\d]{1,3}");

	/** What a sentence says when the role does not need what it names. */
	private static final Pattern NOT_NEEDED = Pattern.compile("\\b(don['’]t|do not|doesn['’]t|does not|won['’]t|will not"
			+ "|no) (need|require|have) to\\b|\\bnot required\\b|\\bno (coding|programming|code)\\b"
			+ "|\\bwithout (writing )?code\\b");

	/** What a sentence says when the role does not write code itself. */
	private static final Pattern DOES_NOT_CODE = Pattern.compile("\\b(don['’]t|do not|doesn['’]t|does not|won['’]t"
			+ "|will not|never) (be )?(write|writing|code|coding|program|programming|build|building)\\b");

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
			+ "|power bi|stata|sas|spss|alteryx|qlik|google sheets|(reconciliation|reporting|analytics|spreadsheet) tools?)"
			+ "\\b");

	/**
	 * The tools of a physical product's engineering: CAD, test and simulation environments and the statistics
	 * packages of quality engineering. A software skill listed beside them is a word in a list of the product's
	 * tools, not a software background.
	 */
	private static final Pattern ENGINEERING_TOOLS = Pattern.compile("\\b(labview|simulink|jmp|minitab|solidworks|catia"
			+ "|creo|autocad|cad|ansys|comsol|mathcad)\\b");

	/**
	 * What a segment says when the code it asks for analyses, models or simulates, which for a
	 * {@code domain_ambiguity} role is the product's engineering rather than software.
	 */
	private static final Pattern MODELS_THE_PRODUCT = Pattern.compile("\\b(simulat\\w*|modell?ing|scripting environments?"
			+ "|analysis (pipelines?|tools?))\\b");

	/**
	 * What a sentence says before a skill when it is the company's product the applicant is to champion, or a
	 * skill the applicant need only be curious about.
	 */
	private static final Pattern NOT_ASKED_BEFORE = Pattern.compile("\\b((proponents?|advocates?|champions?|evangelists?"
			+ "|ambassadors?) (of|for)|interest(ed)? in|curiosity (about|in)|curious about|eager(ness)? to learn"
			+ "|willing(ness)? to learn)\\b");

	/** A choice whose other option is a field rather than a software skill ("distributed systems or fintech"). */
	private static final Pattern OR_A_FIELD = Pattern.compile("\\bor (financial|finance|fintech|banking|payments"
			+ "|economics)\\b");

	private static final Pattern OR = Pattern.compile("\\bor\\b");

	private static final Pattern AND = Pattern.compile("\\band\\b");

	/**
	 * Code as the role's artifact (Q2) and the engineer-facing work of Q3 — source, API definitions
	 * and calls, SDKs, logs, test suites and the gates of a delivery pipeline, and the testbenches that
	 * verify a chip's design. Software alone is not among them: a company says it builds software.
	 */
	private static final Anchored CODE_WORK = new Anchored(String.join("|",
			"\\b(writ|develop|build|implement|ship)\\w*( [\\w,'+#]+){0,4}? (code|microservices|backend services|firmware"
					+ "|scripts)\\b",
			"\\b(source code|review(ing)? (the )?code|code reviews?|application logs|system logs)\\b",
			"\\b(rest(ful)? )?apis? (specifications?|definitions?|design|documentation|calls|requests)\\b",
			"\\b(webhooks?|sdks?|code samples?|uvm|testbench\\w*|verification code)\\b",
			"\\b(build|develop|writ|implement|creat|maintain|automat)\\w*( [\\w,'/-]+){0,4}? (test suites?"
					+ "|automated tests?|test automation|quality gates?|ci/cd pipelines?)\\b",
			"\\b(build|develop|train|deploy|ship|productioni[sz])\\w*( [\\w,'-]+){0,4}? (machine learning|ml|deep learning"
					+ "|ai|llm) (models?|systems|pipelines)\\b"),
			"code", "microservices", "backend services", "firmware", "scripts", "logs", "api", "webhook", "sdk", "uvm",
			"testbench", "verification code",
			"test suite", "automated test", "test automation", "quality gate", "ci/cd", "machine learning model",
			"machine learning system", "machine learning pipeline", "ml model", "ml system", "ml pipeline",
			"deep learning model", "deep learning system", "deep learning pipeline", "ai model", "ai system",
			"ai pipeline", "llm model", "llm system", "llm pipeline");

	/**
	 * Code as the role's own work said in other words: building and shipping, features coded, sample and
	 * demo apps, web, mobile and internal applications and tools, software systems, software built or
	 * implemented. Features built or implemented, and applications shipped, are a product manager's words as
	 * much as an engineer's, and a demo alone is not code: one given to support a sale is decided by Q4.
	 */
	private static final Anchored BUILDS_THE_PRODUCT = new Anchored(String.join("|",
			"\\bbuild(s|ing)? and ship(s|ping)?\\b", "\\b(code|coding)( [\\w,'+#-]+){0,4}? features\\b",
			"\\b(build|builds|building|develop|develops|developing):?( [\\w,'+#-]+){0,4}? ((sample"
					+ "|demo) (apps?|applications|code)|(web|mobile|software|full[- ]stack|internal|ai|llm|agentic) (applications"
					+ "|apps|tools)|software systems)\\b",
			"\\b(build|builds|building|implement|implements|implementing|writ\\w*)( [\\w-]+){0,2}? software\\b(?! (teams?"
					+ "|engineers|developers|compan\\w*|vendors?|sales|products?|solutions|platforms?|tools|engineering|development"
					+ "|requirements|upgrades|releases|licen\\w*|testing|quality|li[fv]e ?cycles?|process\\w*|projects?"
					+ "|implementations?|delivery))"),
			"build", "develop", "code", "coding", "implement", "writ");

	/**
	 * What a sentence says when the role helps others build, or decides what is built: it supports or
	 * enables a team, works with one, or owns a roadmap and its requirements.
	 */
	private static final Pattern HELPS_OTHERS_BUILD = Pattern.compile("\\b(help\\w*|enabl\\w*|support\\w*|empower\\w*"
			+ "|guid(e|es|ing)|roadmaps?|priorit\\w*|defin(e|es|ing)|requirements|specs?|specifications)\\b"
			+ "|\\b(with|alongside|partner\\w*|collaborat\\w*) (the |our |your )?([\\w-]+ ){0,2}?(teams?|engineers"
			+ "|engineering|developers)\\b");

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
	 * Architecture calls, technical debt paid down, RFCs, a codebase, and the architecture, schemas and
	 * models of data (Q3), which are the role's own work only when the role makes them rather than sits
	 * beside the engineers who do.
	 */
	private static final Anchored DESIGN_WORK = new Anchored(String.join("|",
			"\\b(software architecture|architecture decisions|architectural decisions|architecture reviews?|code ?base"
					+ "|rfcs|(technical|design) rfcs?)\\b",
			"\\b(writ|author|review)\\w* (\\w+ ){0,2}?rfcs?\\b",
			"\\b(pay|pays|paying|reduc\\w*|address\\w*|tackl\\w*|own\\w*|refactor\\w*)( down)?( [\\w-]+){0,2}? "
					+ "tech(nical)? debt\\b",
			"\\b(data architecture|schema design|database design|(database|data) schemas?"
					+ "|(logical|physical|conceptual|canonical|dimensional) data models?|data modell?ing)\\b"),
			"architecture", "architectural", "codebase", "code base", "rfc", "debt", "schema", "database design", "database schema",
			"data schema", "data model", "logical", "physical", "conceptual", "canonical", "dimensional");

	/**
	 * A prototype made with AI tooling, the metadata, lineage and stewardship of data, the models of
	 * spreadsheets and BI tools, and fluency in or an understanding of design, where a codebase or a data model
	 * is what the role points tools at, catalogues, builds in a tool made for non-engineers or knows of, rather
	 * than designs.
	 */
	private static final Pattern NOT_ITS_OWN_DESIGN = Pattern.compile("\\b(prototyp\\w*|tooling|metadata|data lineage"
			+ "|data governance|data stewardship|data catalog\\w*|master data|excel|spreadsheets?|pivot tables?"
			+ "|power query|power pivot|powerpivot|power bi|tableau|looker|tabular models?|fluen\\w*|familiar\\w*"
			+ "|understanding|awareness)\\b");

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
	 * What follows a platform's name when what is built in it is its reports, its own or those of the systems
	 * named beside it ("Salesforce and LMS reports").
	 */
	private static final String NOT_ITS_REPORTS = "(?!(( and| &| or|/) ?[\\w-]+)? (reports?|reporting|dashboards?)\\b)";

	/** The verbs of designing or configuring something, as verbs. */
	private static final String DESIGNS_OR_CONFIGURES = "design|designs|designed|designing|configure|configures"
			+ "|configured|configuring";

	/**
	 * A role that builds applications, automations or integrations in a low-code builder or an ERP, or
	 * integrates a CRM or marketing-automation platform. Reports and dashboards built in one are not
	 * among them.
	 */
	private static final Anchored LOW_CODE_WORK = new Anchored(String.join("|",
			"\\b(" + BUILDS + "|" + DESIGNS_OR_CONFIGURES + "|customi[sz](e|es|ed|ing))"
					+ "( (?!reports?\\b|reporting\\b|dashboards?\\b)[\\w,'/-]+){0,6}? (power apps|powerapps|power automate"
					+ "|dynamics 365|pega|outsystems|mendix|appian|servicenow|salesforce flows?|apex|netsuite|sap|erp"
					+ "|zapier|workato|n8n|clay|tray\\.io)\\b" + NOT_ITS_REPORTS,
			"\\b(" + BUILDS + ")( (?!reports?\\b|reporting\\b|dashboards?\\b|records?\\b)[\\w,'/-]+){0,6}? (salesforce"
					+ "|marketo|hubspot|dynamics (365|crm)|eloqua|pardot)\\b" + NOT_ITS_REPORTS),
			"power apps", "powerapps", "power automate", "dynamics", "pega", "outsystems", "mendix", "appian",
			"servicenow", "salesforce", "apex", "netsuite", "sap", "erp", "zapier", "workato", "n8n", "clay", "tray.io",
			"marketo", "hubspot", "eloqua", "pardot");

	/**
	 * A quality management system the role builds or configures, which is software only when it is not
	 * the procedures and controlled documents of a quality standard.
	 */
	private static final Anchored QUALITY_SYSTEM_WORK = new Anchored("\\b(" + BUILDS + "|" + DESIGNS_OR_CONFIGURES
			+ ")( [\\w,'/-]+){0,6}? (quality management systems?|qms)\\b", "quality management system", "qms");

	/** A quality standard named, whose quality system is procedures and controlled documents. */
	private static final Anchored NAMES_A_QUALITY_STANDARD = new Anchored("\\b" + QUALITY_STANDARD + "\\b", "iso 9001",
			"as9100", "as 9100", "iso 13485", "iatf", "iso/iec", "iso 17025");

	/**
	 * What a role does when it builds in a platform: configures or customizes it, or builds, automates or
	 * integrates workflows, apps, agents or integrations. Implementing or developing one alone are a
	 * project's words, and building alone may be a pipeline of deals.
	 */
	private static final Pattern CONFIGURES = Pattern.compile("\\b(configur(e|es|ed|ing)|customi[sz](e|es|ed|ing))\\b"
			+ "|\\b(build|builds|building|automat\\w*|integrat\\w*)( [\\w,'/-]+){0,4}? (workflows?|automations?"
			+ "|integrations?|apps|applications|agents?|flows?|processes|solutions|modules?)\\b");

	/** What the company says of its product when it is built on a platform, which is no work of the role's. */
	private static final Pattern BUILT_ON = Pattern.compile("\\b(built|developed) (\\w+ )?(on|in|with)\\b");

	/** What a platform holds that is reporting or record-keeping rather than built. */
	private static final Pattern REPORTS = Pattern.compile("\\b(reports?|reporting|dashboards?|records?)\\b");

	/**
	 * A low-code system the role only names: it runs a project or a rollout of it, works with IT on
	 * it, or automates in it now and then.
	 */
	private static final Pattern NAMES_A_SYSTEM = Pattern.compile("\\b(rollouts?|roll-outs?|programme|program manag\\w*"
			+ "|project (team|manag\\w*)|sometimes|occasionally|from time to time)\\b"
			+ "|\\b(with|alongside|partner\\w* with|collaborat\\w* with|support\\w*) (the |our )?it\\b");

	/**
	 * A manager who leads software engineers or software teams, which is equivalent technical
	 * experience under Q4. Working with engineers, or managing relationships with them, is not leading
	 * them, and a data or machine-learning team is software only when it is made of engineers.
	 */
	private static final Anchored LEADS_ENGINEERS = new Anchored(String.join("|",
			"\\b(lead|leads|leading|manage|manages|managing|mentor\\w*|hire|hiring|grow|growing)( (?!with\\b)[\\w,'-]+){0,4}? "
					+ "(software|backend|back-end|frontend|front-end|full[- ]stack|mobile|platform|data|ml|machine learning"
					+ "|devops|infrastructure|site reliability) (engineers|developers|engineering teams?)\\b",
			"\\b(lead|leads|leading|manage|manages|managing|grow|growing)( (?!with\\b)[\\w,'-]+){0,4}? "
					+ "(software|backend|back-end|frontend|front-end|full[- ]stack|mobile|platform|devops|infrastructure"
					+ "|site reliability)( engineering| development)? (teams?|organi[sz]ations?|orgs?|groups?)\\b",
			"\\bstaff[- ]engineer[- ]level\\b"), "engineers", "developers", "team", "organi", "org", "group",
			"engineer-level", "engineer level");

	/**
	 * A physical product a {@code domain_ambiguity} role designs, integrates, tests or certifies:
	 * aircraft, rockets, satellites, weapons, vehicles, robots, chips, building systems. It is another
	 * domain unless the role's own work on it is software.
	 */
	private static final Anchored PRODUCT_WORK = new Anchored(String.join("|",
			"\\b(design|integrat|test|qualif|certif|validat|verif|assembl|build|manufactur|calibrat|inspect|evaluat"
					+ "|commission|install|maintain|troubleshoot|servic|repair|model|simulat|analy[sz])\\w*( [\\w,'/-]+){0,5}? "
					+ "(aircraft|airframes?|drones?|uavs?|unmanned (aerial|aircraft) (systems?|vehicles?)|flight controllers?"
					+ "|rockets?"
					+ "|launch vehicles?|spacecraft|satellites?|missiles?|munitions|weapons?|vehicles?|powertrains?"
					+ "|emissions|robots?|chips?|semiconductors?|asics?|wafers?|building automation|building systems"
					+ "|hvac systems?|manufacturing execution systems?|plant execution systems?|rf (test )?systems?"
					+ "|rf hardware|socs?|systems?[- ]on[- ]chips?|memory subsystems?|silicon|photomasks?|scan tools?"
					+ "|consumer products?|consumer electronics|appliances?|hardware (systems?|products?)"
					+ "|defen[cs]e systems?|navigation (systems?|products?)|medical devices?)\\b",
			"\\b(aircraft|drone|rocket|launch vehicle|spacecraft|satellite|missile|weapons?|vehicle|powertrain|emissions"
					+ "|robot|chip|semiconductor|asic|wafer)s? (design|integration|test(ing)?|qualification|certification"
					+ "|validation|verification|assembly|calibration|inspection|diagnostics?)\\b",
			"\\bnon-?destructive (evaluation|testing|inspection)\\b",
			"\\b(pcbas?|wire harness\\w*|harnessing(?! (the|ai|data|our|your|its|their|technology|generative|machine|llms?"
					+ "|insights|power))|mechanical structures|radars?|electro-?mechanical|robotic systems"
					+ "|missile defen[cs]e)\\b",
			"\\b(lithography|optical proximity correction|mbse|model[- ]based systems engineering|sysml"
					+ "|hardware systems engineering)\\b"),
			"aircraft", "airframe", "drone", "uav", "unmanned", "flight controller", "rocket", "launch vehicle",
			"spacecraft", "satellite", "missile", "munitions",
			"weapon", "vehicle", "powertrain", "emissions", "robot", "chip", "semiconductor", "asic", "wafer",
			"building automation", "building system", "hvac", "manufacturing execution", "plant execution",
			"non-destructive", "nondestructive", "rf", "soc", "system-on", "system on", "systems-on", "systems on",
			"memory", "silicon", "photomask", "scan tool", "consumer", "appliance", "hardware", "defence", "defense",
			"navigation", "medical device", "lithography", "optical proximity", "mbse", "model-based", "model based",
			"sysml", "pcba", "wire harness", "harnessing", "mechanical structures", "radar", "electro", "robotic systems");

	/**
	 * Another domain named by its own work: a process plant and its control system, its piping and
	 * electrical diagrams, the protection and control of a power grid, welding, the build-out of a
	 * telecom network's physical plant, a building's HVAC and electrical plant, and a vehicle's systems and
	 * control units.
	 */
	private static final Anchored OTHER_DOMAIN_WORK = new Anchored(String.join("|",
			"\\b(deltav|batch process\\w*|p&ids?|piping and instrumentation|single[- ]line diagrams?)\\b",
			"\\b(protection and control|protective relay\\w*|relay (testing|settings)|power grid|substations?)\\b",
			"\\b(welding|weld inspect\\w*|outside plant|cell (towers?|sites?))\\b",
			"\\b(telecom|fiber|fibre|wireless) network build[- ]?outs?\\b",
			"\\b(hvac|ups systems?|electrical plant|vehicle systems|ecus)\\b"),
			"deltav", "batch process", "p&id", "piping", "single", "protection", "protective", "relay", "power grid",
			"substation", "weld", "outside plant", "cell", "telecom", "fiber", "fibre", "wireless", "hvac", "ups",
			"electrical plant", "vehicle systems", "ecus");

	/** Scripts, which test or automate a physical product without being its code. */
	private static final Pattern SCRIPTS = Pattern.compile("\\bscripts\\b");

	/**
	 * A product's own software: its code, its software tests, its software's architecture, or the
	 * testbenches that verify its design. Test software written for a product tests the product, not its
	 * software.
	 */
	private static final Anchored PRODUCT_SOFTWARE = new Anchored(String.join("|",
			"\\b(writ|develop|build|design|implement|architect)\\w*( [\\w,'+#/-]+){0,3}? (?<!test )software\\b",
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

	/**
	 * What an item says of itself when it is only a plus. "Bonus" is also pay, so it counts alone only
	 * where it starts the item and is not followed by pay's words.
	 */
	private static final Pattern PLUS = Pattern.compile("\\b(an? (\\w+ )?plus|an? (\\w+ )?advantage|advantageous"
			+ "|(is|are|be|considered) an? (\\w+ )?asset"
			+ "|nice[- ]to[- ]haves?|bonus points|an? bonus"
			+ "|any history of|even better if|extra credit|it['’]?s great if|un plus|un atout|von vorteil|wünschenswert"
			+ "|deseable|valorable|diferencial)\\b|^bonus\\b(?! (pay|plan|eligib\\w*|structure|potential|program\\w*))");

	/** A heading that makes every item under it a plus. */
	private static final Pattern PLUS_HEADING = Pattern.compile("\\s*(nice[- ]to[- ]haves?|bonus points|pluses"
			+ "|even better if|extra credit)\\b");

	private static final Pattern PREFERRED = Pattern.compile("\\bpreferred\\b");

	/**
	 * A heading that makes every item under it preferred: what is preferred, desired or desirable, which
	 * the criterion counts as preferred rather than a plus.
	 */
	private static final Pattern PREFERRED_HEADING = Pattern.compile("\\s*((preferred|desired) (qualifications|skills"
			+ "|experience|requirements)|desirable)\\b");

	private final TaxonomyStore taxonomy;

	/** The skills that are a software background when asked for. */
	private final Set<String> softwareSkills = new HashSet<>();

	/** The skills that are platforms a role can build in without code. */
	private final Set<String> platforms = new HashSet<>();

	/** Every skill's names, lowercased, by which the rule finds where in a segment extraction found it. */
	private final Map<String, List<String>> names = new HashMap<>();

	/**
	 * @param taxonomy the skills extraction finds, which say what kind each found skill is
	 */
	public BodyClassificationRule(TaxonomyStore taxonomy) {
		this.taxonomy = taxonomy;
		for (Skill skill : taxonomy.skills()) {
			if (SOFTWARE_KINDS.contains(skill.category()) && !NOT_A_SOFTWARE_BACKGROUND.contains(skill.id())) {
				this.softwareSkills.add(skill.id());
			}
			if (PLATFORM_KINDS.contains(skill.category()) && !NOT_A_PLATFORM.contains(skill.id())) {
				this.platforms.add(skill.id());
			}
			List<String> named = new ArrayList<>();
			named.add(skill.canonicalName().strip().toLowerCase(Locale.ROOT));
			skill.aliases().forEach((alias) -> named.add(alias.strip().toLowerCase(Locale.ROOT)));
			this.names.put(skill.id(), List.copyOf(named));
		}
	}

	/**
	 * Decides one vacancy of the pile.
	 * @param cleanedTitle its cleaned title
	 * @param titleReason why its title was left unknown, which says what doubt the description settles
	 * @param segments its description as cleaning cut it, none if it has none
	 * @param skills the skills extraction found in it, by id, each with the indexes of the segments it was
	 * found in
	 * @return {@code IN} or {@code OUT} where the description settles it, {@code UNKNOWN} otherwise
	 */
	public ClassificationStateEnum classify(String cleanedTitle, UnknownReasonEnum titleReason,
			List<Segment> segments, Map<String, List<Integer>> skills) {
		Map<Integer, Set<String>> skillsIn = new HashMap<>();
		skills.forEach((skill, found) -> found
			.forEach((index) -> skillsIn.computeIfAbsent(index, (segment) -> new HashSet<>()).add(skill)));
		List<Around> description = new ArrayList<>(segments.size());
		int length = 0;
		for (int i = 0; i < segments.size(); i++) {
			Segment segment = segments.get(i);
			if (!segment.boilerplate()) {
				description.add(around(segment, skillsIn.getOrDefault(i, Set.of())));
				length += segment.text().length() + 1;
			}
		}
		if (length < SHORTEST_DESCRIPTION_OF_WORK) {
			return ClassificationStateEnum.UNKNOWN;
		}
		if (anyMention(description, NO_VACANCY, (around) -> true)
				|| anyMention(description, CARRIES_A_QUOTA, (around) -> !HELPS_OTHERS_SELL.matcher(around.item()).find()
						&& !BENEFITS_HEADING.matcher(around.heading()).find()
						&& !NOT_JUST_A_QUOTA.matcher(around.item()).find())
				|| requiresExpertiseOutsideSoftware(description) || requiresKnowledgeOfASubject(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY
				&& (anyMention(description, PRODUCT_WORK, (around) -> !companyOnly(around.item()))
						|| anyMention(description, OTHER_DOMAIN_WORK, (around) -> !companyOnly(around.item())))
				&& !writesTheProductsSoftware(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (statesASoftwareBackground(description, titleReason)) {
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
	 * computing one, nor a subject's degree in a list open to business or quantitative fields, nor a
	 * science offered among the fields of a quantitative list, nor compliance done under SOX controls.
	 */
	private static boolean requiresExpertiseOutsideSoftware(List<Around> description) {
		return anyMention(description, FIELD_DEGREE,
				(around) -> required(around) && !COMPUTING_FIELD.matcher(around.item()).find())
				|| anyMention(description, SUBJECT_DEGREE,
						(around) -> required(around) && !COMPUTING_FIELD.matcher(around.item()).find()
								&& !(BROAD_DEGREE_LIST.matcher(around.item()).find() && OR.matcher(around.item()).find()))
				|| anyMention(description, SCIENCE_DEGREE,
						(around) -> required(around) && !COMPUTING_FIELD.matcher(around.item()).find()
								&& !QUANTITATIVE_FIELD.matcher(around.item()).find())
				|| anyMention(description, FIELD_KNOWLEDGE, (around) -> required(around) && !companyOnly(around.item()))
				|| anyMention(description, FIELD_EXPERTISE,
						(around) -> required(around) && APPLICANT.matcher(around.item()).find())
				|| anyMention(description, FIELD_EXPERIENCE, (around) -> required(around) && !companyOnly(around.item())
						&& !SOX_CONTROLS.matcher(around.item()).find());
	}

	/**
	 * Whether the description requires knowledge of a subject by its shape: of a field's products,
	 * markets, industry or regulation, where what qualifies them is neither software, nor a skill the
	 * taxonomy holds, nor the company or a kind of business.
	 */
	private boolean requiresKnowledgeOfASubject(List<Around> description) {
		for (Around around : description) {
			if (!SUBJECT_KNOWLEDGE.anchoredIn(around.item()) || !required(around) || companyOnly(around.item())) {
				continue;
			}
			Matcher mention = SUBJECT_KNOWLEDGE.pattern().matcher(around.item());
			while (mention.find()) {
				String subject = mention.group("subject").strip();
				if (!NOT_A_FIELD.matcher(subject).find() && !namesASkill(subject)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Whether a subject, or one of its words, is the name of a skill. */
	private boolean namesASkill(String subject) {
		if (this.taxonomy.resolve(subject).isPresent()) {
			return true;
		}
		for (String word : subject.split(" ")) {
			if (this.taxonomy.resolve(word).isPresent()) {
				return true;
			}
		}
		return false;
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
		return aPlus(around, -1);
	}

	/**
	 * Whether what an item names at a place in it is only a plus: the clause it is in says so, the item opens
	 * with a plus before a colon ("Nice to have: Go; Rust"), or it sits under a heading that does. A plus said of another clause of the item ("Python; Rust would be a plus") is
	 * not said of this one.
	 * @param at where in the item the thing is named, -1 if anywhere, when the whole item is read
	 */
	private static boolean aPlus(Around around, int at) {
		if (PLUS_HEADING.matcher(around.heading()).lookingAt()) {
			return true;
		}
		String item = around.item();
		if (at < 0) {
			return PLUS.matcher(item).find();
		}
		int colon = item.indexOf(':');
		if (colon >= 0 && colon < at && PLUS.matcher(item.substring(0, colon)).find()) {
			return true;
		}
		int from = Math.max(item.lastIndexOf(';', at), item.lastIndexOf(" but ", at)) + 1;
		int next = item.indexOf(';', at);
		int to = (next < 0) ? item.length() : next;
		int but = item.indexOf(" but ", at);
		if (but >= 0 && but < to) {
			to = but;
		}
		return PLUS.matcher(item.substring(from, to)).find();
	}

	/**
	 * Whether the role's own work on the physical product it works on is software: code that is not
	 * written to analyse, model or simulate the product, nor scripts or test software that test or drive it,
	 * its software tests or its software's architecture.
	 */
	private static boolean writesTheProductsSoftware(List<Around> description) {
		return anyMention(description, CODE_WORK,
				(around) -> ownCodeWork(around) && !ANALYSIS.matcher(around.item()).find()
						&& !SCRIPTS.matcher(around.item()).find())
				|| anyMention(description, PRODUCT_SOFTWARE, (around) -> !aPlus(around) && !companyOnly(around.item())
						&& !ANALYSIS.matcher(around.item()).find() && !SCRIPTS.matcher(around.item()).find());
	}

	/**
	 * Whether the description requires or prefers a software background, or has the role do software
	 * work: code, engineer-facing artifacts, low-code building or leading software engineers. A software
	 * skill extraction found counts where the segment it was found in asks for it.
	 */
	private boolean statesASoftwareBackground(List<Around> description, UnknownReasonEnum titleReason) {
		Set<String> found = new HashSet<>();
		description.forEach((around) -> around.skills().stream().filter(this.softwareSkills::contains).forEach(found::add));
		return description.stream().anyMatch((around) -> asksForASoftwareSkill(around, found, titleReason))
				|| anyMention(description, SOFTWARE_BACKGROUND, BodyClassificationRule::askedFor)
				|| anyMention(description, CODE_WORK, BodyClassificationRule::ownCodeWork)
				|| anyMention(description, BUILDS_THE_PRODUCT,
						(around) -> ownCodeWork(around) && !HELPS_OTHERS_BUILD.matcher(around.item()).find())
				|| description.stream().anyMatch(this::configuresAPlatform)
				|| anyMention(description, QUALITY_SYSTEM_WORK,
						(around) -> !aPlus(around) && !NAMES_A_SYSTEM.matcher(around.item()).find())
						&& !anyMention(description, NAMES_A_QUALITY_STANDARD, (around) -> true)
				|| anyMention(description, FOREIGN_CODE_WORK,
						(around) -> ownCodeWork(around) && !FOREIGN_DEGREE.matcher(around.item()).find())
				|| anyMention(description, DESIGN_WORK,
						(around) -> !aPlus(around) && !companyOnly(around.item())
								&& !BESIDE_ENGINEERS.matcher(around.item()).find()
								&& !BUSINESS_SYSTEMS.matcher(around.item()).find()
								&& !NOT_ITS_OWN_DESIGN.matcher(around.item()).find())
				|| anyMention(description, LOW_CODE_WORK, (around) -> !aPlus(around)
						&& !NAMES_A_SYSTEM.matcher(around.item()).find()
						&& !(companyOnly(around.item()) && BUILT_ON.matcher(around.item()).find()))
				|| anyMention(description, LEADS_ENGINEERS, (around) -> !aPlus(around))
				|| anyMention(description, SOFTWARE_SKILL,
						(around) -> askedFor(around) && !oneChoiceAmongNonSoftwareTools(around, -1)
								&& !BUSINESS_CONTINUITY.matcher(around.item()).find())
				|| anyMention(description, VERSION_CONTROL,
						(around) -> askedFor(around) && !TRACKS_OR_PUBLISHES.matcher(around.item()).find())
				|| anyMention(description, SOFTWARE_DEGREE,
						(around) -> askedFor(around) && !otherFieldAccepted(around.item()));
	}

	/**
	 * Whether a segment asks for a software skill extraction found in it: the skill has to be what the
	 * requirement asks for, not merely share a sentence with an asking word. A skill found only as a short
	 * acronym is read in its software sense only beside another software skill of the description, since
	 * the same letters name welding societies, companies, certifications and grades; SQL, which the
	 * criterion names, is read on its own. A skill listed beside a physical product's engineering tools is one
	 * of its tools, and so, for a {@code domain_ambiguity} role, is a language asked for to model or simulate;
	 * SQL, which the criterion names, stays asked for beside them.
	 * @param found the software skills found anywhere in the description
	 * @param titleReason why the title was left unknown
	 */
	private boolean asksForASoftwareSkill(Around around, Set<String> found, UnknownReasonEnum titleReason) {
		boolean theProductsTools = ENGINEERING_TOOLS.matcher(around.item()).find()
				|| titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY && MODELS_THE_PRODUCT.matcher(around.item()).find();
		for (String skill : around.skills()) {
			if (!this.softwareSkills.contains(skill) || (theProductsTools && !NAMED_BY_THE_CRITERION.contains(skill))) {
				continue;
			}
			List<Span> spans = where(around.item(), skill);
			if (!NAMED_BY_THE_CRITERION.contains(skill) && onlyAnAcronym(around, spans)
					&& found.stream().allMatch(skill::equals)) {
				continue;
			}
			int at = spans.isEmpty() ? -1 : spans.get(0).start();
			if (askedFor(around, at) && !oneChoiceAmongNonSoftwareTools(around, at)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Where in a lowercased segment a skill's names are written, each a span that begins and ends at a
	 * word's edge. None when extraction found it written in a form of its name that
	 * differs from the taxonomy's, by its separators.
	 */
	private List<Span> where(String item, String skill) {
		List<Span> spans = new ArrayList<>();
		for (String name : this.names.getOrDefault(skill, List.of())) {
			for (int at = item.indexOf(name); at >= 0; at = item.indexOf(name, at + 1)) {
				int end = at + name.length();
				if ((at == 0 || !Character.isLetterOrDigit(item.charAt(at - 1)))
						&& (end == item.length() || !Character.isLetterOrDigit(item.charAt(end)))) {
					spans.add(new Span(at, end));
				}
			}
		}
		spans.sort((one, other) -> Integer.compare(one.start(), other.start()));
		return spans;
	}

	/** Whether every place a skill is written in a segment is a short acronym in capitals, such as AWS. */
	private static boolean onlyAnAcronym(Around around, List<Span> spans) {
		if (spans.isEmpty() || around.written().length() != around.item().length()) {
			return false;
		}
		return spans.stream()
			.allMatch((span) -> ACRONYM.matcher(around.written().substring(span.start(), span.end())).matches());
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

	private static boolean askedFor(Around around) {
		return askedFor(around, -1);
	}

	/**
	 * Whether an item asks the applicant for what it names at a place in it: it sits under a heading
	 * over requirements, says after the thing that it is required or preferred, or asks for it before
	 * the place. It must not call it a plus or say the role does not need it, and must not be the
	 * company speaking of itself.
	 * @param at where in the item the thing is named, -1 if anywhere
	 */
	private static boolean askedFor(Around around, int at) {
		String item = around.item();
		if (aPlus(around, at) || NOT_NEEDED.matcher(item).find() || companyOnly(item) || notAskedBefore(item, at)) {
			return false;
		}
		if (REQUIREMENT_HEADING.matcher(around.heading()).find() || ASKED_AFTER.matcher(item).find()) {
			return true;
		}
		Matcher asks = ASKED_FOR.matcher(item);
		return asks.find() && (at < 0 || asks.start() < at);
	}

	/**
	 * Whether what an item names at a place in it follows words that champion it as the company's product or
	 * ask only for curiosity about it, with no asking between them and the place.
	 * @param at where in the item the thing is named, -1 if anywhere, when nothing is read before it
	 */
	private static boolean notAskedBefore(String item, int at) {
		if (at < 0) {
			return false;
		}
		Matcher before = NOT_ASKED_BEFORE.matcher(item.substring(0, at));
		int end = -1;
		while (before.find()) {
			end = before.end();
		}
		return end >= 0 && !ASKED_FOR.matcher(item.substring(end, at)).find();
	}

	private static boolean companyOnly(String item) {
		return COMPANY_VOICE.matcher(item).find() && !APPLICANT.matcher(item).find();
	}

	/**
	 * Whether the role configures, customizes or automates a platform extraction found, in the segment it
	 * was found in, which is low-code work unless what it builds there is reports or records, or it only
	 * names the system.
	 */
	private boolean configuresAPlatform(Around around) {
		String item = around.item();
		return around.skills().stream().anyMatch(this.platforms::contains) && CONFIGURES.matcher(item).find()
				&& !REPORTS.matcher(item).find() && !aPlus(around) && !NAMES_A_SYSTEM.matcher(item).find()
				&& !companyOnly(item);
	}

	/**
	 * Whether a skill is offered as one choice among tools that are not software skills, named or found
	 * as a reporting product, or beside a field offered in its place. Only the stretch of the item between the
	 * "and"s around the skill is read: an "and" that joins the skill to the rest asks for it beside them, and
	 * one elsewhere in the item leaves its choice standing.
	 * @param at where in the item the skill is named, -1 if not known, when the whole item is read and any
	 * "and" in it asks for the skill
	 */
	private boolean oneChoiceAmongNonSoftwareTools(Around around, int at) {
		String item = around.item();
		if (at < 0) {
			return OR_A_FIELD.matcher(item).find()
					|| (NON_SOFTWARE_TOOL.matcher(item).find() || around.skills().stream().anyMatch(REPORTING_PRODUCTS::contains))
							&& OR.matcher(item).find() && !AND.matcher(item).find();
		}
		int before = item.lastIndexOf(" and ", at);
		int from = (before < 0) ? 0 : before + " and ".length();
		int after = item.indexOf(" and ", at);
		int to = (after < 0) ? item.length() : after;
		String stretch = item.substring(from, to);
		boolean reportingProduct = around.skills()
			.stream()
			.filter(REPORTING_PRODUCTS::contains)
			.anyMatch((product) -> where(item, product).stream().anyMatch((span) -> span.start() >= from && span.start() < to));
		return OR_A_FIELD.matcher(stretch).find()
				|| (NON_SOFTWARE_TOOL.matcher(stretch).find() || reportingProduct) && OR.matcher(stretch).find();
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

	@SafeVarargs
	private static Set<String> union(Set<String>... sets) {
		Set<String> union = new HashSet<>();
		for (Set<String> set : sets) {
			union.addAll(set);
		}
		return Set.copyOf(union);
	}

	/** A segment read with the skills extraction found in it. */
	private static Around around(Segment segment, Set<String> skills) {
		return new Around(segment.text().toLowerCase(Locale.ROOT), segment.text(),
				(segment.under() != null) ? segment.under().toLowerCase(Locale.ROOT) : "", skills);
	}

	/**
	 * What a mention is read with.
	 *
	 * @param item the segment it is in, lowercased: a sentence, a list item or a heading
	 * @param written the segment as it is written, whose capitals tell an acronym
	 * @param heading the heading that segment sits under, lowercased, empty if there is none
	 * @param skills the skills extraction found in the segment, by id
	 */
	private record Around(String item, String written, String heading, Set<String> skills) {
	}

	/**
	 * Where a name is written in a segment.
	 *
	 * @param start the index it starts at
	 * @param end the index after its last character
	 */
	private record Span(int start, int end) {
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
