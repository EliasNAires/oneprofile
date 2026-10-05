package oneprofile.backend.workers.classification;

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
 * except a {@code domain_ambiguity} role whose work could be software or not and whose description
 * names no other domain: there the title's doubt is still open, and it is unknown.
 * <p>
 * A flattened description keeps no structure but its punctuation, so whether a mention is asked
 * for, listed as a plus or merely said of the company is read from the sentence or list item
 * around it. The whole of it is plain matching over the lowercased description, which keeps a pass
 * over the pile inside the time ADR-0012 allows it.
 */
@Component
public class BodyClassificationRule {

	/** The longest descriptions in the pile that describe no work are placeholders and a link. */
	private static final int SHORTEST_DESCRIPTION_OF_WORK = 200;

	/**
	 * What a post says when it hires nobody (Q1): a pipeline or general application, said of the
	 * post itself. A talent network named in a vacancy's footer is not one.
	 */
	private static final Pattern NO_VACANCY = Pattern.compile(String.join("|",
			"\\bthis is an? (prospective|pipeline|general application)",
			"\\b(this|it) is not an? (active|specific|current) (role|opening|job|position|vacancy)",
			"\\bnot an active role\\b", "\\bthis is not for a specific role", "\\bpipeline candidates",
			"\\bto grow our pipeline"));

	/**
	 * What a description says when the role sells: it carries a quota, or is measured on meeting
	 * one. A role that plans or tracks quotas for others, or works beside those who carry them, does
	 * not sell.
	 */
	private static final Pattern CARRIES_A_QUOTA = Pattern.compile(String.join("|",
			"\\b(as|be|is|are) an? quota[- ]carrying\\b",
			"(?<!\\bnot )\\bcarr(y|ies|ying) (a|an|your|the)( \\w+){0,2} quota",
			"\\b(achiev|attain|meet|exceed|hit)\\w* (your |the |an? )?(\\w+ ){0,2}quota"));

	/**
	 * Where a description names a degree. "Master" alone is left out: it is also a Scrum Master's
	 * title, and a master's degree says "degree".
	 */
	private static final String DEGREE = "\\b(degree|bachelor\\S*|b\\.?s\\.?c?|m\\.?s\\.?c?|ph\\.?d)\\b";

	/**
	 * Expertise outside software a software applicant would lack: a degree in a field the role is
	 * about, another engineering discipline among them, or a licence or qualification in one.
	 */
	private static final Pattern FIELD_EXPERTISE = Pattern.compile(String.join("|",
			DEGREE + ".{0,60}?\\b(mechanical|civil|electrical"
					+ "|chemical|structural|aerospace|industrial|manufacturing|biomedical|nuclear|petroleum|materials"
					+ "|environmental) engineering\\b",
			DEGREE + ".{0,60}?\\b(finance|accounting|marketing|law"
					+ "|medicine|nursing|pharmacy|actuarial science)\\b",
			"\\bp\\.?e\\.? (license|licensure|registration)", "\\bprofessional engineer(ing)? (\\(pe\\) )?licen",
			"\\bregistered nurse\\b", "\\blicensed (attorney|pharmacist|physician|nurse|clinician)",
			"\\bcpa (license|certification)", "\\bactuarial (exams?|credentials?|designation)"));

	/** A software degree, the first form of a stated software background (Q4). */
	private static final Pattern SOFTWARE_DEGREE = Pattern
		.compile("\\b(computer science|software engineering|computer engineering)\\b");

	/**
	 * The fields a degree list names when a software degree is one way into the role among others
	 * that are not technical, so the list asks for no software background.
	 */
	private static final Pattern NON_TECHNICAL_DEGREE = Pattern
		.compile("\\b(business|finance|economics|marketing|communications?|accounting|humanities|psychology)\\b");

	/**
	 * The other forms of a stated software background (Q4): a programming language by name, SQL,
	 * cloud, container, infrastructure or networking skills, version control and delivery pipelines
	 * among them.
	 */
	private static final Pattern SOFTWARE_SKILL = Pattern.compile(String.join("|",
			"\\b(python|java|javascript|typescript|golang|ruby|rust|kotlin|scala|php|perl|bash|powershell|matlab"
					+ "|verilog|systemverilog|vhdl)\\b",
			"(?<!\\w)(c\\+\\+|c#|\\.net)(?!\\w)", "\\b(sql|mysql|postgresql|postgres)\\b",
			"\\b(aws|azure|gcp|google cloud|kubernetes|docker|terraform|linux|tcp/ip|ci/cd|github|gitlab|git)\\b"));

	/**
	 * What a sentence says when it asks the applicant for something, rather than describing the
	 * company or what it offers.
	 */
	private static final Pattern ASKED_FOR = Pattern.compile("\\b(experience\\w*|proficien\\w*|knowledge|familiar\\w*"
			+ "|degree|bachelor\\S*|master\\S*|skill\\w*|background|understanding|expertise|years|requir\\w*|must"
			+ "|hands-on|ability|fluen\\w*|comfortable|exposure|preferred|qualifications|you will|you'll|you have)\\b");

	/**
	 * Code as the role's artifact (Q2) and the engineer-facing work of Q3 — source, API definitions,
	 * logs, architecture calls — which a role does whoever names it. Software alone is not among them:
	 * a company says it builds software.
	 */
	private static final Pattern ENGINEER_ARTIFACT = Pattern.compile(String.join("|",
			"\\b(writ|develop|build|implement|ship)\\w*( [\\w,']+){0,4}? (code|microservices|backend services|firmware"
					+ "|scripts)\\b",
			"\\b(source code|codebase|review(ing)? (the )?code|code reviews?|software architecture|architecture decisions"
					+ "|system design|tech(nical)? debt|application logs|system logs|log analysis)\\b",
			"\\b(rest(ful)? )?apis? (specifications?|definitions?|design|documentation)\\b"));

	/**
	 * An export-control notice, which names source code among what the law restricts, not the
	 * role's work.
	 */
	private static final Pattern EXPORT_CONTROL = Pattern.compile("\\bexport\\b");

	private static final Pattern PLUS = Pattern.compile("\\b(a plus|nice to have|bonus)\\b");

	private static final Pattern PREFERRED = Pattern.compile("\\bpreferred\\b");

	/** How many characters either side of a mention are read with it, at most. */
	private static final int CHARACTERS_AROUND = 80;

	/**
	 * A domain outside software that a {@code domain_ambiguity} description names: an engineering
	 * discipline, its tools or its sites.
	 */
	private static final Pattern OTHER_DOMAIN = Pattern.compile("\\b(mechanical|electrical|civil|structural|chemical"
			+ "|geotechnical|cad|solidworks|autocad|machining|welding|piping|process equipment|hvac|construction|plant"
			+ "|pcb|highways?|substation)\\b");

	/**
	 * Work a {@code domain_ambiguity} description gives that could be software or not, so the title's
	 * doubt stays open.
	 */
	private static final Pattern WORK_EITHER_WAY = Pattern
		.compile("\\b(control systems?|automation|embedded|simulation|test systems?|systems integration)\\b");

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
		if (NO_VACANCY.matcher(description).find() || CARRIES_A_QUOTA.matcher(description).find()
				|| requiresExpertiseOutsideSoftware(description)) {
			return ClassificationStateEnum.OUT;
		}
		if (statesASoftwareBackground(description)) {
			return ClassificationStateEnum.IN;
		}
		if (titleReason == UnknownReasonEnum.DOMAIN_AMBIGUITY && !OTHER_DOMAIN.matcher(description).find()
				&& WORK_EITHER_WAY.matcher(description).find()) {
			return ClassificationStateEnum.UNKNOWN;
		}
		return ClassificationStateEnum.OUT;
	}

	/**
	 * Whether the description requires expertise outside software. Expertise it lists as a plus or
	 * prefers is no requirement, nor is a field's degree offered beside a software one.
	 */
	private static boolean requiresExpertiseOutsideSoftware(String description) {
		return anyMention(description, FIELD_EXPERTISE, (around) -> !PLUS.matcher(around).find()
				&& !PREFERRED.matcher(around).find() && !SOFTWARE_DEGREE.matcher(around).find());
	}

	/**
	 * Whether the description requires or prefers a software background, or has the role work on
	 * engineer-facing artifacts. A background counts only in a sentence that asks the applicant for
	 * something and does not call it a plus; a software degree, only where the list it is in names no
	 * field outside technology.
	 */
	private static boolean statesASoftwareBackground(String description) {
		return anyMention(description, ENGINEER_ARTIFACT,
				(around) -> !PLUS.matcher(around).find() && !EXPORT_CONTROL.matcher(around).find())
				|| anyMention(description, SOFTWARE_SKILL, BodyClassificationRule::askedFor)
				|| anyMention(description, SOFTWARE_DEGREE,
						(around) -> askedFor(around) && !NON_TECHNICAL_DEGREE.matcher(around).find());
	}

	private static boolean askedFor(String around) {
		return ASKED_FOR.matcher(around).find() && !PLUS.matcher(around).find();
	}

	/** Whether any mention of a pattern is one that counts, read with the words around it. */
	private static boolean anyMention(String description, Pattern mention, Predicate<String> counts) {
		Matcher found = mention.matcher(description);
		while (found.find()) {
			if (counts.test(around(description, found.start(), found.end()))) {
				return true;
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

	private static boolean endsASentence(String description, int index) {
		char c = description.charAt(index);
		if (c == '•') {
			return true;
		}
		return (c == '.' || c == ';' || c == '!' || c == '?')
				&& (index + 1 == description.length() || description.charAt(index + 1) == ' ');
	}

}
