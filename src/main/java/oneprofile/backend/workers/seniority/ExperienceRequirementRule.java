package oneprofile.backend.workers.seniority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oneprofile.backend.storage.normalizedvacancy.ExperienceRequirement;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import org.springframework.stereotype.Component;

/**
 * Reads a description's experience requirements and the required years they give (PRD §6.1).
 * <p>
 * An experience requirement is a number of years ({@code 5+ years}, {@code 3-5 years}, {@code at least
 * three years}) that experience is asked for: the word after it says what the years are of ({@code of},
 * {@code in}, {@code with}, {@code building}, {@code relevant}, {@code experience} and the like), or
 * experience is named just before it ({@code Experience: 4-7 years}). Whatever the years are asked for,
 * the role or a skill, they count as professional experience. Left out are the segments that are
 * boilerplate, those marked optional by their words or their heading ({@code is a plus},
 * {@code preferred}, a "Preferred qualifications" heading), and numbers that are not experience: years
 * after a word that dates them ({@code after 2 years}, {@code spent 18 years}), years of something that
 * is not experience ({@code 18 years of age}), compounds ({@code a 4-year vesting}) and more years than
 * a vacancy asks for ({@code 40 years}, which only a company's age reaches).
 * <p>
 * Lowest wins within one requirement, highest across distinct ones: a range gives its floor, and
 * requirements that are alternatives of each other ({@code BS and 5 years, or MS and 3}) give the lowest
 * of their floors; the required years are the highest of what each requirement gives. Alternatives are
 * phrases joined by an {@code or} that offers another path ({@code , or}, {@code ; or}, an {@code OR},
 * an {@code or} before a degree), within a segment or across items that end in {@code or}; an
 * {@code or} between what the years are of ({@code Java or Python}) does not join two requirements.
 */
@Component
public class ExperienceRequirementRule {

	/** The most years a vacancy is taken to ask for; a larger number is a company's age. */
	private static final int MOST_YEARS = 25;

	private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(Map.entry("one", 1),
			Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4), Map.entry("five", 5),
			Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9),
			Map.entry("ten", 10), Map.entry("eleven", 11), Map.entry("twelve", 12), Map.entry("fifteen", 15));

	private static final String NUMBER = "\\d{1,2}|" + String.join("|", NUMBER_WORDS.keySet());

	/**
	 * A number of years, alone or as a range: {@code 5+ years}, {@code 4-7 Years}, {@code between 3 and 5 years},
	 * {@code three or more years}.
	 */
	private static final Pattern PHRASE = Pattern.compile("(?<![\\w.,$€£-])(" + NUMBER + ")\\s*\\+?(?:\\s*(?:-|–|—|to|or|and)\\s*("
			+ NUMBER + ")\\s*\\+?)?(?:\\s+or\\s+more)?\\s*(?:years?|yrs?)\\b['’]?", Pattern.CASE_INSENSITIVE);

	/** The word after the years, and whether an {@code of} or an {@code in} comes first, with its article. */
	private static final Pattern AFTER = Pattern.compile("^[\\s\"“”'’]*((?:of|in)\\s+(?:the\\s+|an?\\s+)?)?[\\s\"“”'’]*([\\p{L}-]+)",
			Pattern.CASE_INSENSITIVE);

	/** The words after the years that say they are years of experience. */
	private static final Set<String> FOLLOWERS = Set.of("in", "with", "as", "building", "developing", "working",
			"designing", "leading", "managing", "writing", "using", "delivering", "running", "operating",
			"programming", "coding", "doing", "shipping", "creating", "implementing", "supporting", "selling",
			"owning", "architecting", "engineering", "deploying", "hands-on", "hands", "professional", "industry",
			"relevant", "related", "direct", "progressive", "practical", "post", "full-time", "full", "technical",
			"commercial", "total", "overall", "prior", "previous", "proven", "combined", "applied", "software",
			"work", "experience", "experiences", "exp", "customer-facing", "minimum", "required");

	/** What years are of, or in, where they are not experience. */
	private static final Set<String> NOT_EXPERIENCE = Set.of("age", "history", "heritage", "innovation",
			"excellence", "growth", "success", "service", "trust", "tradition", "existence", "operation",
			"operations", "partnership", "employment", "tenure", "business", "market", "providing", "college",
			"university", "study", "studies", "schooling", "education", "coursework");

	/** Experience named just before the years ({@code Experience: 4-7 years}, {@code worked as … for 5 years}). */
	private static final Pattern EXPERIENCE_BEFORE = Pattern.compile("\\b(?:exp(?:erience)?|worked|working)\\b[^.;]{0,40}$",
			Pattern.CASE_INSENSITIVE);

	/** Experience named in the rest of the clause after the years. */
	private static final Pattern EXPERIENCE_AFTER = Pattern.compile("^[^.;!?]{0,60}\\bexperience\\b",
			Pattern.CASE_INSENSITIVE);

	/** A word that dates the years rather than asks for them. */
	private static final Pattern DATED = Pattern.compile(
			"\\b(?:after|within|first|next|past|last|every|each|until|spent|since|our|its|their)\\s+(?:the\\s+)?(?:over\\s+|more than\\s+|about\\s+)?$"
					+ "|\\bfor\\s+(?:over|more than|nearly|almost)\\s+$",
			Pattern.CASE_INSENSITIVE);

	/** The words that mark a segment's requirements optional, read outside its parentheses. */
	private static final Pattern OPTIONAL = Pattern.compile(
			"\\b(?:preferred|a plus|nice[- ]to[- ]have|bonus|desired|desirable|an advantage|advantageous|beneficial|optional)\\b",
			Pattern.CASE_INSENSITIVE);

	/** The headings whose segments are optional. */
	private static final Pattern OPTIONAL_HEADING = Pattern.compile(
			"\\b(?:preferred|recommended|nice[- ]to[- ]haves?|bonus|pluses|a plus|desired|desirable|optional|good to have)\\b",
			Pattern.CASE_INSENSITIVE);

	/** An {@code or} that offers another path to the requirement before it. */
	private static final Pattern ALTERNATIVE = Pattern.compile("[,;]\\s*or\\b|(?-i:\\bOR\\b)|\\bor\\s+(?:if\\s+)?(?:an?\\s+)?"
			+ "(?:" + NUMBER + "|b\\.?s\\b|b\\.?a\\b|m\\.?s\\b|m\\.?a\\b|ph\\.?d|master|bachelor|doctor|associate|high school|equivalent)",
			Pattern.CASE_INSENSITIVE);

	/** A segment in the company's voice: what the company is, not what it asks. */
	private static final Pattern COMPANY = Pattern.compile("\\b(?:we|we're|we’re|we've|we’ve|our|us|company|founded|itself)\\b",
			Pattern.CASE_INSENSITIVE);

	/** Words that turn a segment in the company's voice into what it asks of the candidate. */
	private static final Pattern CANDIDATE = Pattern.compile(
			"\\b(?:you|your|you'll|you’ll|candidates?|applicants?|requires?|required|must|minimum|at least|seeking|looking for|ideal)\\b",
			Pattern.CASE_INSENSITIVE);

	/** The headings that introduce the company rather than the vacancy. */
	private static final Pattern COMPANY_HEADING = Pattern.compile(
			"^\\W*about\\s+(?!you\\b|the role\\b|this role\\b|the job\\b|the position\\b|the opportunity\\b)|who we are|who is|why join|why work|with us\\b|our company|benefits|perks",
			Pattern.CASE_INSENSITIVE);

	/** A segment that ends by offering another path, which the next segment states. */
	private static final Pattern ENDS_IN_OR = Pattern.compile("\\bor[\\s.,;:]*$", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_OR = Pattern.compile("^\\s*or\\b", Pattern.CASE_INSENSITIVE);

	private static final Pattern PARENTHESES = Pattern.compile("\\([^)]*\\)");

	/**
	 * Reads one description.
	 * @param segments its segments, as cleaning cut them
	 * @return its required years and the experience requirements it accepted
	 */
	public ExperienceReading read(List<Segment> segments) {
		List<List<Phrase>> requirements = new ArrayList<>();
		boolean open = false;
		boolean previousRead = false;
		for (int index = 0; index < segments.size(); index++) {
			Segment segment = segments.get(index);
			String text = segment.text();
			if (segment.boilerplate() || optional(segment) || aboutTheCompany(segment)) {
				open = false;
				previousRead = false;
				continue;
			}
			open = open || (previousRead && STARTS_WITH_OR.matcher(text).find());
			int previousEnd = -1;
			boolean read = false;
			Matcher phrase = PHRASE.matcher(text);
			while (phrase.find()) {
				Phrase found = accepted(index, text, phrase);
				if (found == null) {
					continue;
				}
				boolean alternative = (previousEnd < 0) ? open
						: ALTERNATIVE.matcher(withoutParentheses(text.substring(previousEnd, phrase.end()))).find();
				if (alternative && !requirements.isEmpty()) {
					requirements.getLast().add(found);
				}
				else {
					requirements.add(new ArrayList<>(List.of(found)));
				}
				previousEnd = phrase.end();
				read = true;
			}
			open = (read || open) && ENDS_IN_OR.matcher(text).find();
			previousRead = read;
		}
		List<Integer> floors = requirements.stream()
			.map((alternatives) -> alternatives.stream().mapToInt(Phrase::lower).min().orElseThrow())
			.toList();
		Integer required = floors.stream().max(Integer::compare).orElse(null);
		List<ExperienceRequirement> accepted = new ArrayList<>();
		for (int requirement = 0; requirement < requirements.size(); requirement++) {
			int floor = floors.get(requirement);
			for (Phrase found : requirements.get(requirement)) {
				accepted.add(new ExperienceRequirement(found.segment(), found.position(), found.lower(), found.upper(),
						found.lower() == floor && floor == required));
			}
		}
		return new ExperienceReading(required, accepted);
	}

	/** The phrase a match is if it states an experience requirement, or null if it does not. */
	private static Phrase accepted(int segment, String text, Matcher phrase) {
		int lower = number(phrase.group(1));
		Integer upper = (phrase.group(2) != null) ? number(phrase.group(2)) : null;
		if (lower > MOST_YEARS || (upper != null && (upper < lower || upper > MOST_YEARS))) {
			return null;
		}
		String before = text.substring(0, phrase.start());
		if (DATED.matcher(before).find()) {
			return null;
		}
		String rest = text.substring(phrase.end());
		Matcher after = AFTER.matcher(rest);
		String next = after.find() ? after.group(2).toLowerCase(Locale.ROOT) : null;
		boolean experience;
		if (next != null && after.group(1) != null) {
			experience = !NOT_EXPERIENCE.contains(next);
		}
		else {
			experience = (next != null && FOLLOWERS.contains(next)) || EXPERIENCE_AFTER.matcher(rest).find()
					|| EXPERIENCE_BEFORE.matcher(before).find();
		}
		return experience ? new Phrase(segment, phrase.start(), lower, upper) : null;
	}

	/** Whether a segment's words or its heading mark what it requires optional. */
	private static boolean optional(Segment segment) {
		return OPTIONAL.matcher(withoutParentheses(segment.text())).find()
				|| (segment.under() != null && OPTIONAL_HEADING.matcher(segment.under()).find());
	}

	/** Whether a segment speaks of the company rather than of what the vacancy asks. */
	private static boolean aboutTheCompany(Segment segment) {
		return (segment.under() != null && COMPANY_HEADING.matcher(segment.under()).find())
				|| (COMPANY.matcher(segment.text()).find() && !CANDIDATE.matcher(segment.text()).find());
	}

	private static String withoutParentheses(String text) {
		return PARENTHESES.matcher(text).replaceAll(" ");
	}

	private static int number(String written) {
		Integer word = NUMBER_WORDS.get(written.toLowerCase(Locale.ROOT));
		return (word != null) ? word : Integer.parseInt(written);
	}

	/** One accepted phrase while the description is read. */
	private record Phrase(int segment, int position, int lower, Integer upper) {
	}

}
