package oneprofile.backend.workers.seniority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.ExperienceRequirement;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.junit.jupiter.api.Test;

class ExperienceRequirementRuleTest {

	private final ExperienceRequirementRule rule = new ExperienceRequirementRule();

	@Test
	void readsTheYearsAnItemRequires() {
		ExperienceReading reading = this.rule.read(List.of(item("Qualifications", "5+ years of software engineering experience")));

		assertThat(reading.requiredYears()).isEqualTo(5);
		assertThat(reading.requirements()).containsExactly(new ExperienceRequirement(0, 0, 5, null, true));
	}

	@Test
	void readsARangeAsItsFloorAndItsTop() {
		ExperienceReading reading = this.rule.read(List.of(item("Requirements", "Experience Range : 4-7 Years"),
				item("Requirements", "5 to 10 years of applicable Security Consulting experience"),
				item("Requirements", "Experience: 2 -10 years")));

		assertThat(reading.requirements()).extracting(ExperienceRequirement::lowerYears, ExperienceRequirement::upperYears)
			.containsExactly(tuple(4, 7), tuple(5, 10), tuple(2, 10));
	}

	@Test
	void takesTheHighestAcrossDistinctRequirementsWithoutSummingThem() {
		ExperienceReading reading = this.rule.read(List.of(item("Requirements", "3+ years of Java"),
				item("Requirements", "2+ years with AWS")));

		assertThat(reading.requiredYears()).isEqualTo(3);
		assertThat(reading.requirements()).extracting(ExperienceRequirement::setsRequiredYears)
			.containsExactly(true, false);
	}

	@Test
	void takesTheHighestOfARequirementAndOneItIncludes() {
		assertThat(this.rule.read(List.of(item("Who you are",
				"8+ years of software development experience, including 3+ years leading engineering teams.")))
			.requiredYears()).isEqualTo(8);
	}

	@Test
	void takesTheLowestOfAlternativesInOneSegment() {
		ExperienceReading reading = this.rule.read(List.of(item("Job Requirements",
				"B.S. in Computer Science (or related field) and 5+ years related experience or M.S. Degree and 3+yrs of experience.")));

		assertThat(reading.requiredYears()).isEqualTo(3);
		assertThat(reading.requirements()).extracting(ExperienceRequirement::setsRequiredYears)
			.containsExactly(false, true);
	}

	@Test
	void takesTheLowestOfAlternativesListedAsItemsJoinedByOr() {
		assertThat(this.rule.read(List.of(
				item("Required Qualifications",
						"Bachelor's degree in a STEM-related field and 8 years of relevant experience; or"),
				item("Required Qualifications",
						"Master's degree in a STEM-related field and 6 years of relevant experience; or"),
				item("Required Qualifications", "High school diploma and 12 years of relevant experience"),
				item("Required Qualifications", "2+ years of Python programming")))
			.requiredYears()).isEqualTo(6);
	}

	@Test
	void keepsApartRequirementsJoinedByAnOrInsideOne() {
		assertThat(this.rule.read(List.of(item("Requirements",
				"5+ years of Java or Python and 3+ years of AWS")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void readsTheYearsWrittenInWords() {
		assertThat(this.rule.read(List.of(item("Requirements", "Three or more years of experience with Kotlin")))
			.requiredYears()).isEqualTo(3);
	}

	@Test
	void readsTheYearsWhereExperienceIsNamedFirst() {
		assertThat(this.rule.read(List.of(new Segment(SegmentKindEnum.HEADING, null, "Exp: 8-14 Years", false)))
			.requiredYears()).isEqualTo(8);
		assertThat(this.rule.read(List.of(item("Expected Skills & Experience",
				"Industry experience 4+ years particularly with Power BI"))).requiredYears()).isEqualTo(4);
	}

	@Test
	void leavesOutARequirementMarkedOptional() {
		ExperienceReading reading = this.rule.read(List.of(item("Requirements", "3+ years of Go"),
				item("Requirements", "Minimum 5 years of experience in Red Team activities is highly preferred"),
				item("Requirements", "8+ years with Kubernetes is a plus"),
				item("Requirements", "Preferred: MS and 2 or more years experience in C"),
				item("Preferred Qualifications", "9+ years of experience coding in Go"),
				item("Nice to have", "6+ years of Rust"),
				item("Bonus points", "7 years building compilers"),
				item("Recommended Qualifications", "7-10 years experience in an IT Quality role.")));

		assertThat(reading.requiredYears()).isEqualTo(3);
		assertThat(reading.requirements()).hasSize(1);
	}

	@Test
	void keepsARequirementWhoseOptionalWordQualifiesSomethingElse() {
		assertThat(this.rule.read(List.of(item("What you'll bring",
				"5+ years in a software engineering role using one or more mainstream languages (.NET / C# preferred; Java, Go, Python)."),
				item("Requirements", "3+ years of experience in DV preferably in CPU verification")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void leavesOutNumbersThatAreNotExperience() {
		ExperienceReading reading = this.rule.read(List.of(
				sentence("Why Cisco?",
						"We’ve been innovating fearlessly for 40 years to create solutions that power how humans work."),
				sentence("About us", "Before founding Sierra, Clay spent 18 years at Google, where he led Google Labs."),
				sentence("", "Cadence is a pivotal leader, building upon more than 30 years of computational software expertise."),
				sentence("Benefits", "The base salary range is $115,000 - $160,000 per year, with bonuses."),
				item("Benefits", "28 vacation days, increasing by 2 days after 2 years and 3 days after 3 years with Solaris."),
				item("Perks", "4 weeks of vacation, increasing to 5 weeks after 2 years of employment"),
				item("Duties", "Co-own the 1-year and 3-year roadmap with the VP of AI."),
				sentence("The Opportunity", "Join a global company with a 130-year legacy and a 4-year vesting schedule."),
				sentence("Who we are", "For over 20 years, we have worked closely with asset managers."),
				item("Requirements", "Must be 18 years of age or older"),
				new Segment(SegmentKindEnum.SENTENCE, null, "We have 10 years of experience in fintech.", true)));

		assertThat(reading.requiredYears()).isNull();
		assertThat(reading.requirements()).isEmpty();
	}

	@Test
	void takesTheLowestOfAlternativesJoinedByAnOrBeforeTheYears() {
		assertThat(this.rule.read(List.of(item("Required Skills",
				"5 years of relevant experience with a Bachelor’s Degree or 10 years of relevant experience without a degree.")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void leavesOutTheYearsOfExperienceACompanyHas() {
		ExperienceReading reading = this.rule.read(List.of(
				sentence("", "With over 25 years of experience and a team of 5,000 professionals, we help organizations build products."),
				sentence("About OpenTable", "With 70,000+ restaurant partners and 25+ years of experience, OpenTable is an industry leader."),
				sentence("Who is Forcepoint?", "20+ years in business."),
				sentence("", "Trading for over 20 years, it’s experience that makes us leaders in the field."),
				sentence("About Propellerhead", "Over our 20+ years in the market we have worked with many organisations."),
				item("Why You’ll Love Working With Us", "Join a leader backed by over 23 years of Canadian Solar experience.")));

		assertThat(reading.requirements()).isEmpty();
	}

	@Test
	void keepsTheYearsACompanyAsksOfTheCandidate() {
		assertThat(this.rule.read(List.of(sentence("Job Summary",
				"We are seeking a Software Engineer with more than 6+ year of experience in .NET."),
				item("What we're looking for", "4+ years of experience with Kafka")))
			.requiredYears()).isEqualTo(6);
	}

	@Test
	void readsARangeJoinedByWordsAsItsFloor() {
		assertThat(this.rule.read(List.of(item("Requirements", "Between 3 and 5 years of experience in Go"))).requiredYears())
			.isEqualTo(3);
		assertThat(this.rule.read(List.of(item("Requirements", "2 or 3 years of experience with React"))).requiredYears())
			.isEqualTo(2);
	}

	@Test
	void leavesOutYearsOfSchoolingAndYearsInTheBusiness() {
		assertThat(this.rule.read(List.of(item("Requirements", "Bachelor's degree or 4 years of college coursework"),
				sentence("Overview", "Acme has 20+ years in the business of payments.")))
			.requirements()).isEmpty();
	}

	@Test
	void keepsTheYearsSomeoneHasWorkedFor() {
		assertThat(this.rule.read(List.of(item("Requirements", "You have worked as an engineer for 5+ years")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void keepsARequirementThatTakesAdvantageOfSomething() {
		assertThat(this.rule.read(List.of(item("Requirements", "4+ years of experience taking advantage of AWS")))
			.requiredYears()).isEqualTo(4);
	}

	@Test
	void joinsASegmentStartingWithOrOnlyToTheSegmentJustBefore() {
		assertThat(this.rule.read(List.of(item("Requirements", "8+ years of experience with C++"),
				item("Requirements", "Strong communication skills"),
				item("Requirements", "Or 3 years of experience with a PhD")))
			.requiredYears()).isEqualTo(8);
	}

	@Test
	void readsNothingInADescriptionWithoutSegments() {
		assertThat(this.rule.read(List.of())).isEqualTo(new ExperienceReading(null, List.of()));
	}

	private static Segment sentence(String under, String text) {
		return new Segment(SegmentKindEnum.SENTENCE, under.isEmpty() ? null : under, text, false);
	}

	private static Segment item(String under, String text) {
		return new Segment(SegmentKindEnum.ITEM, under, text, false);
	}

}
