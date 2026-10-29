package oneprofile.backend.workers.seniority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.ExperienceRequirement;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.junit.jupiter.api.Test;

class ExperienceRequirementRuleTest {

	/** The slug of a board none of the examples names. */
	private static final String ACME = "acme";

	private final ExperienceRequirementRule rule = new ExperienceRequirementRule();

	@Test
	void readsTheYearsAnItemRequires() {
		ExperienceReading reading = this.rule.read(ACME, List.of(item("Qualifications", "5+ years of software engineering experience")));

		assertThat(reading.requiredYears()).isEqualTo(5);
		assertThat(reading.requirements()).containsExactly(new ExperienceRequirement(0, 0, 5, null, true));
	}

	@Test
	void readsARangeAsItsFloorAndItsTop() {
		ExperienceReading reading = this.rule.read(ACME, List.of(item("Requirements", "Experience Range : 4-7 Years"),
				item("Requirements", "5 to 10 years of applicable Security Consulting experience"),
				item("Requirements", "Experience: 2 -10 years")));

		assertThat(reading.requirements()).extracting(ExperienceRequirement::lowerYears, ExperienceRequirement::upperYears)
			.containsExactly(tuple(4, 7), tuple(5, 10), tuple(2, 10));
	}

	@Test
	void takesTheHighestAcrossDistinctRequirementsWithoutSummingThem() {
		ExperienceReading reading = this.rule.read(ACME, List.of(item("Requirements", "3+ years of Java"),
				item("Requirements", "2+ years with AWS")));

		assertThat(reading.requiredYears()).isEqualTo(3);
		assertThat(reading.requirements()).extracting(ExperienceRequirement::setsRequiredYears)
			.containsExactly(true, false);
	}

	@Test
	void takesTheHighestOfARequirementAndOneItIncludes() {
		assertThat(this.rule.read(ACME, List.of(item("Who you are",
				"8+ years of software development experience, including 3+ years leading engineering teams.")))
			.requiredYears()).isEqualTo(8);
	}

	@Test
	void takesTheLowestOfAlternativesInOneSegment() {
		ExperienceReading reading = this.rule.read(ACME, List.of(item("Job Requirements",
				"B.S. in Computer Science (or related field) and 5+ years related experience or M.S. Degree and 3+yrs of experience.")));

		assertThat(reading.requiredYears()).isEqualTo(3);
		assertThat(reading.requirements()).extracting(ExperienceRequirement::setsRequiredYears)
			.containsExactly(false, true);
	}

	@Test
	void takesTheLowestOfAlternativesListedAsItemsJoinedByOr() {
		assertThat(this.rule.read(ACME, List.of(
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
		assertThat(this.rule.read(ACME, List.of(item("Requirements",
				"5+ years of Java or Python and 3+ years of AWS")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void readsTheYearsWrittenInWords() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "Three or more years of experience with Kotlin")))
			.requiredYears()).isEqualTo(3);
	}

	@Test
	void readsANumberWrittenOutBeforeItsDigits() {
		assertThat(this.rule.read(ACME, List.of(item("Qualifications",
				"Twelve (12) years of relevant professional experience providing Network Engineering support is required."))))
			.isEqualTo(new ExperienceReading(12, List.of(new ExperienceRequirement(0, 0, 12, null, true))));
		assertThat(this.rule.read(ACME, List.of(item("Experience", "Ten (10)+ or more years of software engineering experience"))))
			.extracting(ExperienceReading::requiredYears).isEqualTo(10);
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "three (3) to five (5) years of experience with Java"))))
			.extracting(ExperienceReading::requiredYears).isEqualTo(3);
	}

	@Test
	void leavesOutTheYearsThatSubstituteForSomethingElse() {
		assertThat(this.rule.read(ACME, List.of(item("Required Qualifications",
				"A bachelor's degree and twelve years of experience in software development. Sixteen (16) years of experience in software development may be substituted in lieu of a degree"),
				item("Qualifications", "A Master's degree may be substituted for two (2) years of experience.")))
			.requirements()).extracting(ExperienceRequirement::lowerYears).containsExactly(12);
	}

	@Test
	void leavesOutTheYearsOfferedInLieuOfSomethingNamedFirst() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements",
				"A bachelor’s degree and fifteen years of experience as a system administrator is required. In lieu of a bachelor’s degree twenty (20) years of experience.")))
			.requiredYears()).isEqualTo(15);
	}

	@Test
	void takesTheLowestOfAlternativesThatAreAlsoAcceptable() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements",
				"A B.S. degree in Computer Science and four years of programming experience. Also acceptable is no degree and eight (8) years of programming experience.")))
			.requiredYears()).isEqualTo(4);
	}

	@Test
	void keepsTheRequirementsBesideASubstitution() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements",
				"Bachelors degree with 8 years related software or systems experience or MS degree with 6 years of related experience, or 4 additional years of experience may be considered in lieu of a completed degree."),
				item("Requirements",
						"Bachelor's Degree and a minimum of 5 years of experience (additional years of experience will be considered in lieu of degree")))
			.requirements()).extracting(ExperienceRequirement::lowerYears).containsExactly(8, 6, 5);
	}

	@Test
	void readsARangeFromZero() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "Typically zero to four (4) years’ experience as a programmer analyst."))))
			.extracting(ExperienceReading::requiredYears).isEqualTo(0);
	}

	@Test
	void readsTheYearsWhereExperienceIsNamedFirst() {
		assertThat(this.rule.read(ACME, List.of(new Segment(SegmentKindEnum.HEADING, null, "Exp: 8-14 Years", false)))
			.requiredYears()).isEqualTo(8);
		assertThat(this.rule.read(ACME, List.of(item("Expected Skills & Experience",
				"Industry experience 4+ years particularly with Power BI"))).requiredYears()).isEqualTo(4);
	}

	@Test
	void leavesOutARequirementMarkedOptional() {
		ExperienceReading reading = this.rule.read(ACME, List.of(item("Requirements", "3+ years of Go"),
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
		assertThat(this.rule.read(ACME, List.of(item("What you'll bring",
				"5+ years in a software engineering role using one or more mainstream languages (.NET / C# preferred; Java, Go, Python)."),
				item("Requirements", "3+ years of experience in DV preferably in CPU verification")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void leavesOutNumbersThatAreNotExperience() {
		ExperienceReading reading = this.rule.read(ACME, List.of(
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
		assertThat(this.rule.read(ACME, List.of(item("Required Skills",
				"5 years of relevant experience with a Bachelor’s Degree or 10 years of relevant experience without a degree.")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void leavesOutTheYearsOfExperienceACompanyHas() {
		ExperienceReading reading = this.rule.read(ACME, List.of(
				sentence("", "With over 25 years of experience and a team of 5,000 professionals, we help organizations build products."),
				sentence("About OpenTable", "With 70,000+ restaurant partners and 25+ years of experience, OpenTable is an industry leader."),
				sentence("Who is Forcepoint?", "20+ years in business."),
				sentence("", "Trading for over 20 years, it’s experience that makes us leaders in the field."),
				sentence("About Propellerhead", "Over our 20+ years in the market we have worked with many organisations."),
				item("Why You’ll Love Working With Us", "Join a leader backed by over 23 years of Canadian Solar experience.")));

		assertThat(reading.requirements()).isEmpty();
	}

	@Test
	void leavesOutTheYearsOfAnAwardAndOfASegmentAboutIts() {
		assertThat(this.rule.read(ACME, List.of(
				item("What we offer at Avanade", "Work with the Microsoft Global Alliance Partner of the Year (14 years in a row)"),
				sentence("What We Offer",
						"Its track record is built on more than 20 years of industry experience and leadership.")))
			.requirements()).isEmpty();
	}

	@Test
	void leavesOutTheYearsOfTheCompanysTeamAndItsFacts() {
		assertThat(this.rule.read(ACME, List.of(item("Our Team", "25+ years in Finance, Strategy, M&A"),
				sentence("Meet the Team",
						"The team is comprised of talented engineers with diverse experience levels, ranging from 3 to over 20 years."),
				item("OUR FACTS & FIGURES", "23+ years of expertise Gradion builds digital platforms & deep-tech solutions.")))
			.requirements()).isEmpty();
	}

	@Test
	void keepsWhatACandidateBringsToTheTeam() {
		assertThat(this.rule.read(ACME, List.of(item("What you bring to the team", "5+ years of experience with Go"))).requiredYears())
			.isEqualTo(5);
	}

	@Test
	void keepsARequirementOnSomethingAndIts() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "4+ years of experience with Java and its ecosystem"))).requiredYears())
			.isEqualTo(4);
	}

	@Test
	void leavesOutTheYearsOfTheCompanyNamedByItsName() {
		assertThat(this.rule.read("opentable", List.of(sentence("",
				"With millions of diners, 70,000+ restaurant partners and 25+ years of experience, OpenTable, part of Booking Holdings, is an industry leader."))).requirements()).isEmpty();
		assertThat(this.rule.read("rocketlab", List.of(sentence("Principal Algorithm Engineer",
				"Building on more than 20 years of electro-optical and infrared systems innovation from Geost, Rocket Lab Optical Systems delivers solutions."))).requirements()).isEmpty();
		assertThat(this.rule.read("dxctechnology.wd1/dxcjobs", List.of(sentence("DXC ServiceNow Strategic Business Group",
				"As ServiceNow Global Elite Partner, DXC has been a leader in the ecosystem for more than 12 years and has over 20 years of Enterprise Service Management Experience."))).requirements()).isEmpty();
	}

	@Test
	void keepsTheYearsOfWhatTheCompanyNamesAsTheJobsWork() {
		assertThat(this.rule.read("salesforce.wd12/external_career_site", List.of(
				item("Required", "10+ years architecting or implementing enterprise Salesforce solutions"),
				item("Must Have", "10+ years of total experience in the IT industry, with 5+ years delivering Salesforce projects"))).requiredYears())
			.isEqualTo(10);
		assertThat(this.rule.read("gopuff", List.of(sentence("",
				"Gopuff is hiring an Oracle Business Analyst with 5+ years of experience and strong SQL skills."))).requiredYears())
			.isEqualTo(5);
		assertThat(this.rule.read("simcorp.wd3/simcorp", List.of(sentence("Why this role is important",
				"With typically 12+ years of experience in Testing, Quality Assurance or a related field, this role leads a team."))).requiredYears())
			.isEqualTo(12);
	}

	@Test
	void keepsTheYearsACompanyAsksOfTheCandidate() {
		assertThat(this.rule.read(ACME, List.of(sentence("Job Summary",
				"We are seeking a Software Engineer with more than 6+ year of experience in .NET."),
				item("What we're looking for", "4+ years of experience with Kafka")))
			.requiredYears()).isEqualTo(6);
	}

	@Test
	void readsARangeJoinedByWordsAsItsFloor() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "Between 3 and 5 years of experience in Go"))).requiredYears())
			.isEqualTo(3);
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "2 or 3 years of experience with React"))).requiredYears())
			.isEqualTo(2);
	}

	@Test
	void leavesOutYearsOfSchoolingAndYearsInTheBusiness() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "Bachelor's degree or 4 years of college coursework"),
				sentence("Overview", "Acme has 20+ years in the business of payments.")))
			.requirements()).isEmpty();
	}

	@Test
	void keepsTheYearsSomeoneHasWorkedFor() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "You have worked as an engineer for 5+ years")))
			.requiredYears()).isEqualTo(5);
	}

	@Test
	void keepsARequirementThatTakesAdvantageOfSomething() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "4+ years of experience taking advantage of AWS")))
			.requiredYears()).isEqualTo(4);
	}

	@Test
	void joinsASegmentStartingWithOrOnlyToTheSegmentJustBefore() {
		assertThat(this.rule.read(ACME, List.of(item("Requirements", "8+ years of experience with C++"),
				item("Requirements", "Strong communication skills"),
				item("Requirements", "Or 3 years of experience with a PhD")))
			.requiredYears()).isEqualTo(8);
	}

	@Test
	void readsNothingInADescriptionWithoutSegments() {
		assertThat(this.rule.read(ACME, List.of())).isEqualTo(new ExperienceReading(null, List.of()));
	}

	private static Segment sentence(String under, String text) {
		return new Segment(SegmentKindEnum.SENTENCE, under.isEmpty() ? null : under, text, false);
	}

	private static Segment item(String under, String text) {
		return new Segment(SegmentKindEnum.ITEM, under, text, false);
	}

}
