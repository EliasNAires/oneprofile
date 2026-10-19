package oneprofile.backend.workers.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import oneprofile.backend.storage.vacancyskill.FoundSkill;
import org.junit.jupiter.api.Test;

class SkillExtractionRuleTest {

	private final SkillExtractionRule rule = new SkillExtractionRule(TaxonomyStore.read(new StringReader("""
			go\tGo\tlanguage\tGolang\tGo\t
			postgresql\tPostgreSQL\tdatabase\tPostgres\t\t
			aws\tAWS\tcloud\tAmazon Web Services\t\t
			aws-lambda\tAWS Lambda\tcloud\tLambda\tLambda\taws
			kubernetes\tKubernetes\tdevops\tK8s\t\t
			spring-framework\tSpring Framework\tframework\tSpring\tSpring\t
			spring-boot\tSpring Boot\tframework\t\t\tspring-framework
			java\tJava\tlanguage\t\t\t
			javascript\tJavaScript\tlanguage\tJS\t\t
			c-sharp\tC#\tlanguage\t\t\t
			node-js\tNode.js\truntime\tNode\tNode\t
			""")));

	@Test
	void findsTheSkillsOfTheWorkedExampleOfAdr0018() {
		List<FoundSkill> found = this.rule.extract("Senior Go Engineer",
				List.of(boilerplate("Our platform runs on Kubernetes."),
						item("Experience with Go, PostgreSQL and AWS Lambda."),
						sentence("You will go on call once a month.")));

		assertThat(found).containsExactlyInAnyOrder(new FoundSkill("go", List.of(1), false, true),
				new FoundSkill("postgresql", List.of(1), false, false),
				new FoundSkill("aws-lambda", List.of(1), false, false));
	}

	@Test
	void findsAPlainKeyInAnyCaseAndSpacing() {
		assertThat(ids(this.rule.extract("Engineer", List.of(item("postgres, KUBERNETES and spring_boot")))))
			.containsExactlyInAnyOrder("postgresql", "kubernetes", "spring-boot");
	}

	@Test
	void letsTheLongestKeyWinWithNoOverlaps() {
		assertThat(ids(this.rule.extract("Engineer", List.of(item("Spring Boot, JavaScript and AWS Lambda.")))))
			.containsExactlyInAnyOrder("spring-boot", "javascript", "aws-lambda");
	}

	@Test
	void findsNoKeyInsideAWord() {
		assertThat(this.rule.extract("Engineer", List.of(sentence("Javanese, Gopher and Awsome Postgresql.")))).extracting(FoundSkill::skillId)
			.containsExactly("postgresql");
	}

	@Test
	void findsAKeyThatEndsInASymbolOrHoldsAFullStop() {
		assertThat(ids(this.rule.extract("Engineer", List.of(item("C#, Node.js and Java.")))))
			.containsExactlyInAnyOrder("c-sharp", "node-js", "java");
	}

	@Test
	void findsAContextKeyOnlyInTheCasingTheTaxonomyGivesIt() {
		assertThat(ids(this.rule.extract("Engineer",
				List.of(sentence("Our GO-TO-MARKET runs on PostgreSQL."), sentence("We go fast on Postgres.")))))
			.containsExactly("postgresql");
	}

	@Test
	void letsNoContextKeyConfirmAnother() {
		assertThat(this.rule.extract("Engineer", List.of(item("Go and Spring.")))).isEmpty();
	}

	@Test
	void letsAPlainKeyOfTheSkillsParentOrChildAnywhereInTheVacancyConfirmIt() {
		List<FoundSkill> found = this.rule.extract("Engineer", List.of(item("Spring Boot services."),
				sentence("We love Spring."), sentence("We run on Lambda."), item("Most of it on AWS.")));

		assertThat(found).contains(new FoundSkill("spring-framework", List.of(1), false, true),
				new FoundSkill("aws-lambda", List.of(2), false, true));
	}

	@Test
	void doesNotLetAChildAddItsParent() {
		assertThat(ids(this.rule.extract("Engineer", List.of(item("AWS Lambda functions."))))).containsExactly("aws-lambda");
	}

	@Test
	void readsTheTitleAsOneMoreSegment() {
		assertThat(this.rule.extract("Go and Kubernetes Engineer", List.of(item("Kubernetes daily."))))
			.containsExactlyInAnyOrder(new FoundSkill("go", List.of(), true, true),
					new FoundSkill("kubernetes", List.of(0), true, false));
	}

	@Test
	void givesOneRowPerSkillWithEverySegmentItWasFoundIn() {
		assertThat(this.rule.extract("Engineer",
				List.of(item("Golang."), heading("Stack"), item("Go with Postgres."), item("More Golang."))))
			.contains(new FoundSkill("go", List.of(0, 2, 3), false, false));
	}

	@Test
	void doesNotReadBoilerplate() {
		assertThat(this.rule.extract("Engineer", List.of(boilerplate("We run Kubernetes.")))).isEmpty();
	}

	private static List<String> ids(List<FoundSkill> found) {
		return found.stream().map(FoundSkill::skillId).toList();
	}

	private static Segment item(String text) {
		return new Segment(SegmentKindEnum.ITEM, null, text, false);
	}

	private static Segment heading(String text) {
		return new Segment(SegmentKindEnum.HEADING, null, text, false);
	}

	private static Segment sentence(String text) {
		return new Segment(SegmentKindEnum.SENTENCE, null, text, false);
	}

	private static Segment boilerplate(String text) {
		return new Segment(SegmentKindEnum.SENTENCE, null, text, true);
	}

}
