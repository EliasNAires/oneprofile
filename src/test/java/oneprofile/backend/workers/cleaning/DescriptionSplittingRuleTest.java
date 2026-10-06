package oneprofile.backend.workers.cleaning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import org.junit.jupiter.api.Test;

class DescriptionSplittingRuleTest {

	private final DescriptionSplittingRule splitting = new DescriptionSplittingRule();

	@Test
	void takesTheLinesTheSweepMarkedForWhatTheyAre() {
		assertThat(this.splitting.split("""
				We move money.
				# Requirements:
				- 5+ years of Java
				- Kafka""")).containsExactly(sentence(null, "We move money."), heading("Requirements"),
						item("Requirements", "5+ years of Java"), item("Requirements", "Kafka"));
	}

	@Test
	void marksBoilerplateAndEndsAHeadingWhereBoilerplateStartsOrStops() {
		assertThat(this.splitting.split("""
				> # About Nova
				> Nova builds retirement plans.
				Own the payroll integration.
				# Benefits
				> - 401(k) match""")).containsExactly(
						new Segment(SegmentKindEnum.HEADING, null, "About Nova", true),
						new Segment(SegmentKindEnum.SENTENCE, "About Nova", "Nova builds retirement plans.", true),
						sentence(null, "Own the payroll integration."), heading("Benefits"),
						new Segment(SegmentKindEnum.ITEM, null, "401(k) match", true));
	}

	@Test
	void cutsAParagraphIntoItsSentencesButLeavesAnItemWhole() {
		assertThat(this.splitting.split("""
				We build APIs, e.g. Payments. Do you love Go? ¡Únete! We ship 2.5 times a day. 10 people work here.
				- Own the API. Mentor others.""")).containsExactly(sentence(null, "We build APIs, e.g. Payments."),
						sentence(null, "Do you love Go?"), sentence(null, "¡Únete!"),
						sentence(null, "We ship 2.5 times a day."), sentence(null, "10 people work here."),
						item(null, "Own the API. Mentor others."));
	}

	@Test
	void cutsOnTheBulletsACompanyTyped() {
		assertThat(this.splitting.split("""
				# Duties
				• Lead PCB design.
				· Run SI reviews
				* Mentor engineers
				- • Own the 6-8 person roadmap
				We offer: • Equity • Full-time remote · A laptop""")).containsExactly(heading("Duties"),
						item("Duties", "Lead PCB design."), item("Duties", "Run SI reviews"),
						item("Duties", "Mentor engineers"), item("Duties", "Own the 6-8 person roadmap"),
						heading("We offer"), item("We offer", "Equity"), item("We offer", "Full-time remote"),
						item("We offer", "A laptop"));
	}

	@Test
	void takesANumberedLineForAnItem() {
		assertThat(this.splitting.split("""
				1. Lead the product support teams.
				2) Grow parts sales
				(3) Visit customers
				2026 was our best year.
				2.5 years of Java is enough.""")).containsExactly(item(null, "Lead the product support teams."),
						item(null, "Grow parts sales"), item(null, "Visit customers"),
						sentence(null, "2026 was our best year."), sentence(null, "2.5 years of Java is enough."));
	}

	@Test
	void takesAShortLineThatReadsLikeAHeadingForOne() {
		assertThat(this.splitting.split("""
				REQUISITOS
				Requisitos deseables
				- Inglés avanzado
				Nice to have
				What you’ll bring:
				Location: Remote
				Employment type: Full-time
				We are looking for someone who loves building great products and teams:""")).containsExactly(
						heading("REQUISITOS"), heading("Requisitos deseables"),
						item("Requisitos deseables", "Inglés avanzado"), heading("Nice to have"),
						heading("What you’ll bring"), sentence("What you’ll bring", "Location: Remote"),
						sentence("What you’ll bring", "Employment type: Full-time"),
						sentence("What you’ll bring",
								"We are looking for someone who loves building great products and teams:"));
	}

	@Test
	void cutsAHeadingOutOfTheSentenceItOpens() {
		assertThat(this.splitting.split(
				"We move money. Location: Remote. Requirements: 5+ years of Java. Nice to have: Go. Requisitos: inglés."))
			.containsExactly(sentence(null, "We move money."), sentence(null, "Location: Remote."),
					heading("Requirements"), sentence("Requirements", "5+ years of Java."), heading("Nice to have"),
					sentence("Nice to have", "Go."), heading("Requisitos"), sentence("Requisitos", "inglés."));
	}

	@Test
	void guessesWhereAnItemStartsOnlyInADescriptionOfOneLine() {
		assertThat(this.splitting.split(
				"We hire. Requirements 5+ years of Java Strong communication skills Experience with Kafka"))
			.containsExactly(sentence(null, "We hire."), heading("Requirements"),
					item("Requirements", "5+ years of Java"), item("Requirements", "Strong communication skills"),
					item("Requirements", "Experience with Kafka"));
		assertThat(this.splitting.split("""
				# Team
				You work with Java Strong typing matters here""")).containsExactly(heading("Team"),
						sentence("Team", "You work with Java Strong typing matters here"));
	}

	@Test
	void readsEachSegmentAsLowercasedWords() {
		assertThat(this.splitting.split("Build C++ & C# APIs in Node.js; 5+ years, full-time. Inglés: Don’t wait!"))
			.extracting(Segment::tokens)
			.containsExactly(List.of("build", "c++", "c#", "apis", "in", "node.js", "5+", "years", "full-time"),
					List.of("inglés", "don’t", "wait"));
	}

	@Test
	void cutsNothingOutOfALineLeftWithoutWords() {
		assertThat(this.splitting.split(">\n#\n-\n• ...\n> # :\nWe hire.")).containsExactly(sentence(null, "We hire."));
		assertThat(this.splitting.split("")).isEmpty();
	}

	private static Segment heading(String text) {
		return new Segment(SegmentKindEnum.HEADING, null, text, false);
	}

	private static Segment item(String under, String text) {
		return new Segment(SegmentKindEnum.ITEM, under, text, false);
	}

	private static Segment sentence(String under, String text) {
		return new Segment(SegmentKindEnum.SENTENCE, under, text, false);
	}

}
