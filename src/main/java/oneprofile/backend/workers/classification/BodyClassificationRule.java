package oneprofile.backend.workers.classification;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
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
 * expertise outside software are out. A role that states a software background or works on
 * engineer-facing artifacts (Q2–Q4) is in. What is left is out, since none of the questions holds,
 * except a {@code domain_ambiguity} role whose duties are work that could be software or not, in a
 * description that names no other domain: there the title's doubt is still open, and it is unknown.
 * A word that could be software or not — automation, simulation, embedded — opens no such doubt
 * when it is not the role's work.
 * <p>
 * A flattened description keeps no structure but its punctuation, so whether a mention is asked
 * for, listed as a plus or merely said of the company is read from the sentence or list item
 * around it. The whole of it is plain matching over the lowercased description, each pattern tried
 * only around the literal words it needs, which keeps a pass over the pile inside the time ADR-0012
 * allows it.
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
	 * title, and a master's degree says "degree".
	 */
	private static final String DEGREE = "\\b(degree|bachelor\\S*|b\\.?s\\.?c?|m\\.?s\\.?c?|ph\\.?d)\\b";

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

	/**
	 * The fields whose knowledge a description can ask for in its own right, apart from any degree:
	 * the subjects of finance, the sciences, and the engineering of radios, vehicles and aircraft.
	 */
	private static final String FIELD = "(financial services|finance|banking|payments|insurance|fraud|financial[- ]crimes?"
			+ "|anti-money[- ]laundering|aml|trading|capital markets|biolog\\w*|life sciences|genomics|physics|rf"
			+ "|radio frequency|satcom|satellite communications?|powertrain|avionics|flight controls?|pharmac\\w*"
			+ "|chemistry|clinical|medical|tax|legal|accounting)";

	/** The science and engineering fields in which experience is itself field expertise. */
	private static final String HARD_FIELD = "(biolog\\w*|genomics|physics\\S*|rf|radio frequency|satcom|powertrain"
			+ "|avionics|flight controls?)";

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

	/** Experience in a science or another engineering, which is field expertise in itself. */
	private static final Anchored FIELD_EXPERIENCE = new Anchored(String.join("|",
			"\\bexperience (in|with|on|analy\\w+) (\\w+ ){0,2}?" + HARD_FIELD + "\\b",
			"\\bphysics-based (modell?ing|simulations?)\\b"), "biolog", "genomics", "physics", "rf", "radio frequency",
			"satcom", "powertrain", "avionics", "flight control");

	/**
	 * A computing degree, the first form of a stated software background (Q4). Information
	 * technology and information systems also name departments, so they count only beside a degree.
	 */
	private static final Anchored SOFTWARE_DEGREE = new Anchored(String.join("|",
			"\\b(computer science|software engineering|computer engineering)\\b",
			DEGREE + ".{0,40}?\\b(information technology|information systems)\\b"), "computer science",
			"software engineering", "computer engineering", "information technology", "information systems");

	/** The computing fields a degree list can accept. */
	private static final Pattern COMPUTING_FIELD = Pattern.compile("\\b(computer science|software engineering"
			+ "|computer engineering|information technology|information systems|computing)\\b");

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
	 * cloud, container, infrastructure or networking skills, version control and delivery pipelines
	 * among them, and technical experience in a field that is software. Azure's boards, which only
	 * track work, are not among them.
	 */
	private static final Anchored SOFTWARE_SKILL = new Anchored(String.join("|",
			"\\b(python|java|javascript|typescript|golang|ruby|rust|kotlin|scala|php|perl|bash|powershell|matlab"
					+ "|verilog|systemverilog|vhdl)\\b",
			"(?<!\\w)(c\\+\\+|c#|\\.net)(?!\\w)", "\\b(sql|mysql|postgresql|postgres)\\b",
			"\\bazure\\b(?! devops| boards)",
			"\\b(aws|gcp|google cloud|kubernetes|docker|terraform|linux|tcp/ip|ci/cd|github|gitlab|git)\\b",
			"\\b(observability|developer tools|devtools)\\b"), "python", "java", "typescript", "golang", "ruby", "rust",
			"kotlin", "scala", "php", "perl", "bash", "powershell", "matlab", "verilog", "systemverilog", "vhdl", "c++",
			"c#", ".net", "sql", "mysql",
			"postgres", "aws", "azure", "gcp", "google cloud", "kubernetes", "docker", "terraform", "linux", "tcp/ip",
			"ci/cd", "git", "observability", "developer tools", "devtools");

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
	 * Code as the role's artifact (Q2) and the engineer-facing work of Q3 — source, API definitions,
	 * logs — which a role does whoever names it, in any language. Software alone is not among them: a
	 * company says it builds software.
	 */
	private static final Anchored CODE_WORK = new Anchored(String.join("|",
			"\\b(writ|develop|build|implement|ship)\\w*( [\\w,']+){0,4}? (code|microservices|backend services|firmware"
					+ "|scripts)\\b",
			"\\b(source code|review(ing)? (the )?code|code reviews?|tech(nical)? debt|application logs|system logs)\\b",
			"\\b(rest(ful)? )?apis? (specifications?|definitions?|design|documentation)\\b",
			"\\b(build|develop|train|deploy|ship|productioni[sz])\\w*( [\\w,'-]+){0,4}? (machine learning|ml|deep learning"
					+ "|ai|llm) (models?|systems|pipelines)\\b",
			"\\b(programmier\\w*|programaci[oó]n|programa[cç][aã]o|programmation|softwareentwicklung|software-entwicklung"
					+ "|desarrollo de software|d[ée]veloppement (logiciel|de logiciels)|desenvolvimento de software)\\b"),
			"code", "microservices", "backend services", "firmware", "scripts", "debt", "logs", "api",
			"machine learning model", "machine learning system", "machine learning pipeline", "ml model", "ml system",
			"ml pipeline", "deep learning model", "deep learning system", "deep learning pipeline", "ai model",
			"ai system", "ai pipeline", "llm model", "llm system", "llm pipeline", "programmier", "programaci",
			"programaç", "programac", "programmation", "softwareentwicklung", "software-entwicklung",
			"desarrollo de software", "développement", "developpement", "desenvolvimento de software");

	/**
	 * Architecture calls and a codebase, which are the role's own work only when the role makes them
	 * rather than sits beside the engineers who do.
	 */
	private static final Anchored DESIGN_WORK = new Anchored(
			"\\b(software architecture|architecture decisions|codebase)\\b", "architecture", "codebase");

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

	/**
	 * A role that builds applications, automations or integrations in a low-code builder, an ERP or a
	 * quality system. Reports and dashboards built in one are not among them.
	 */
	private static final Anchored LOW_CODE_WORK = new Anchored(
			"\\b(build|develop|design|configur|implement|automat|integrat|customi[sz])\\w*"
					+ "( (?!reports?\\b|reporting\\b|dashboards?\\b)[\\w,'/-]+){0,6}? (power apps|powerapps|power automate"
					+ "|dynamics 365|pega|outsystems|mendix|appian|servicenow|salesforce flows?|apex|netsuite|sap|erp"
					+ "|zapier|workato|n8n|clay|tray\\.io|quality management system)\\b",
			"power apps", "powerapps", "power automate", "dynamics 365", "pega", "outsystems", "mendix", "appian",
			"servicenow", "salesforce flow", "apex", "netsuite", "sap", "erp", "zapier", "workato", "n8n", "clay",
			"tray.io", "quality management system");

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
	 * Work a {@code domain_ambiguity} role is given that could be software or not — control, embedded,
	 * test or automation systems maintained, designed or programmed — so the title's doubt stays open.
	 * The word alone is no such work: a company names automation and simulation of itself.
	 */
	private static final Anchored WORK_EITHER_WAY = new Anchored(
			"\\b(maintain|troubleshoot|design|develop|program|commission|integrat|support|test)\\w*( [\\w,'-]+){0,3}? "
					+ "(control|embedded|test|automation) systems?\\b",
			"control", "embedded", "test", "automation");

	/** Control systems that are a company's financial controls, not machines or code. */
	private static final Pattern FINANCIAL_CONTROLS = Pattern.compile("\\b(internal control|financial|sox|compliance)");

	/**
	 * A domain outside software that a {@code domain_ambiguity} description names: an engineering
	 * discipline, its tools or its sites. Work that could be software or not is no doubt there.
	 */
	private static final Pattern OTHER_DOMAIN = Pattern.compile("\\b(mechanical|electrical|civil|structural|chemical"
			+ "|geotechnical|cad|solidworks|autocad|machining|welding|piping|process equipment|hvac|construction|plant"
			+ "|pcb|highways?|substation)\\b");

	private static final Pattern PLUS = Pattern.compile("\\b(a plus|nice to have|bonus|un plus|un atout|von vorteil"
			+ "|wünschenswert|deseable|valorable|diferencial)\\b");

	private static final Pattern PREFERRED = Pattern.compile("\\bpreferred\\b");

	/** How many characters either side of a mention are read with it, at most. */
	private static final int CHARACTERS_AROUND = 80;

	/**
	 * Decides one vacancy of the pile.
	 * @param cleanedTitle its cleaned title
	 * @param titleReason why its title was left unknown, which says what doubt the description settles
	 * @param cleanedDescription its description once cleaned, empty if it has none
	 * @return {@code IN} or {@code OUT} where the description settles it, {@code UNKNOWN} otherwise
	 */
	public ClassificationStateEnum classify(String cleanedTitle, UnknownReasonEnum titleReason, String cleanedDescription) {
		if (cleanedDescription.length() < SHORTEST_DESCRIPTION_OF_WORK) {
			return ClassificationStateEnum.UNKNOWN;
		}
		String description = cleanedDescription.toLowerCase(Locale.ROOT);
		if (anyMention(description, NO_VACANCY, (around) -> true)
				|| anyMention(description, CARRIES_A_QUOTA, (around) -> true)
				|| requiresExpertiseOutsideSoftware(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (statesASoftwareBackground(description)) {
			return ClassificationStateEnum.IN;
		}
		if (titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY
				&& anyMention(description, WORK_EITHER_WAY, (around) -> !FINANCIAL_CONTROLS.matcher(around).find())
				&& !OTHER_DOMAIN.matcher(description).find()) {
			return ClassificationStateEnum.UNKNOWN;
		}
		return ClassificationStateEnum.OUT;
	}

	/**
	 * Whether the description requires expertise outside software. Expertise it lists as a plus,
	 * prefers or claims for the company is no requirement, nor is a field's degree offered beside a
	 * computing one.
	 */
	private static boolean requiresExpertiseOutsideSoftware(String description) {
		return anyMention(description, FIELD_DEGREE,
				(around) -> required(around) && !COMPUTING_FIELD.matcher(around).find())
				|| anyMention(description, FIELD_KNOWLEDGE, (around) -> required(around) && !companyOnly(around))
				|| anyMention(description, FIELD_EXPERTISE, (around) -> required(around) && APPLICANT.matcher(around).find())
				|| anyMention(description, FIELD_EXPERIENCE, (around) -> required(around) && !companyOnly(around));
	}

	private static boolean required(String around) {
		return !aPlus(around) && !PREFERRED.matcher(around).find();
	}

	/** Whether a sentence lists what it names only as a plus, which asks for nothing. */
	private static boolean aPlus(String around) {
		return PLUS.matcher(around).find();
	}

	/**
	 * Whether the description requires or prefers a software background, or has the role do software
	 * work: code, engineer-facing artifacts, low-code building or leading software engineers.
	 */
	private static boolean statesASoftwareBackground(String description) {
		return anyMention(description, CODE_WORK,
				(around) -> !aPlus(around) && !NOT_SOFTWARE_WORK.matcher(around).find())
				|| anyMention(description, DESIGN_WORK,
						(around) -> !aPlus(around) && !BESIDE_ENGINEERS.matcher(around).find())
				|| anyMention(description, LOW_CODE_WORK, (around) -> !aPlus(around))
				|| anyMention(description, LEADS_ENGINEERS, (around) -> !aPlus(around))
				|| anyMention(description, SOFTWARE_SKILL,
						(around) -> askedFor(around) && !oneChoiceAmongNonSoftwareTools(around))
				|| anyMention(description, SOFTWARE_DEGREE, (around) -> askedFor(around) && !otherFieldAccepted(around));
	}

	/**
	 * Whether a sentence asks the applicant for what it names: it asks for something, does not call
	 * it a plus or say the role does not need it, and is not the company speaking of itself.
	 */
	private static boolean askedFor(String around) {
		return ASKED_FOR.matcher(around).find() && !aPlus(around) && !NOT_NEEDED.matcher(around).find()
				&& !companyOnly(around);
	}

	private static boolean companyOnly(String around) {
		return COMPANY_VOICE.matcher(around).find() && !APPLICANT.matcher(around).find();
	}

	/**
	 * Whether a skill is offered as one choice among tools that are not software skills. A list that
	 * joins it to them with "and" asks for it beside them.
	 */
	private static boolean oneChoiceAmongNonSoftwareTools(String around) {
		return NON_SOFTWARE_TOOL.matcher(around).find() && OR.matcher(around).find() && !AND.matcher(around).find();
	}

	/** Whether a degree list also accepts a field outside computing. */
	private static boolean otherFieldAccepted(String around) {
		return OTHER_FIELD.matcher(around).find();
	}

	/** Whether any mention of a pattern is one that counts, read with the words around it. */
	private static boolean anyMention(String description, Anchored mention, Predicate<String> counts) {
		for (String anchor : mention.anchors()) {
			for (int at = description.indexOf(anchor); at >= 0; at = description.indexOf(anchor, at + 1)) {
				if (at > 0 && Character.isLetterOrDigit(description.charAt(at - 1))) {
					continue;
				}
				Matcher found = mention.pattern()
					.matcher(description)
					.region(Math.max(0, at - Anchored.REACH),
							Math.min(description.length(), at + anchor.length() + Anchored.REACH))
					.useTransparentBounds(true)
					.useAnchoringBounds(false);
				while (found.find()) {
					if (counts.test(around(description, found.start(), found.end()))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * The sentence or list item around a span: a flattened description keeps no other boundary. A list
	 * flattened without punctuation runs on for hundreds of words, so no more than
	 * {@value #CHARACTERS_AROUND} characters either side of the span are read.
	 */
	private static String around(String description, int start, int end) {
		int from = start;
		while (from > 0 && start - from < CHARACTERS_AROUND && !endsASentence(description, from - 1)) {
			from--;
		}
		int to = end;
		while (to < description.length() && to - end < CHARACTERS_AROUND && !endsASentence(description, to)) {
			to++;
		}
		return description.substring(from, to);
	}

	/**
	 * A pattern searched only around its anchors: literal words, one of which every match of it
	 * contains at the start of a word. A description is scanned for the anchors and the pattern is tried only in a region
	 * around each, since trying an alternation at every position of every description is what made a
	 * pass over the pile slow.
	 *
	 * @param pattern what a mention looks like, over the lowercased description
	 * @param anchors literal words one of which every match contains at the start of a word
	 */
	private record Anchored(Pattern pattern, List<String> anchors) {

		/** How far from its anchor a match can reach, at most. */
		static final int REACH = 150;

		Anchored(String pattern, String... anchors) {
			this(Pattern.compile(pattern), List.of(anchors));
		}

	}

	private static boolean endsASentence(String description, int index) {
		char c = description.charAt(index);
		if (c == '•') {
			return true;
		}
		return (c == '.' || c == ';' || c == '!' || c == '?')
				&& (index + 1 == description.length() || description.charAt(index + 1) == ' ');
	}

}
