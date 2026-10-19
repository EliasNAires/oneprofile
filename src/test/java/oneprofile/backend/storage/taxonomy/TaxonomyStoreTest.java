package oneprofile.backend.storage.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TaxonomyStoreTest {

	private final TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader("""
			postgresql\tPostgreSQL\tdatabase\tpostgres|psql\t\t
			c-sharp\tC#\tlanguage\tcsharp|c sharp\t\t
			terraform\tTerraform\tdevops\t\t\t
			"""));

	@Test
	void resolvesAliasesAndCanonicalNamesToTheSameSkill() {
		assertThat(List.of("postgres", "psql", "PostgreSQL"))
			.allSatisfy((name) -> assertThat(this.taxonomy.resolve(name)).map(Skill::id).hasValue("postgresql"));
	}

	@Test
	void resolvesANameWhateverItsCaseAndSpacing() {
		assertThat(this.taxonomy.resolve("  C \t SHARP ")).map(Skill::id).hasValue("c-sharp");
	}

	@Test
	void resolvesANameWhateverSeparatorPartsItsWords() {
		TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader("spring-boot\tSpring Boot\tframework\t\n"));
		assertThat(List.of("Spring-Boot", "spring_boot", "Spring/Boot"))
			.allSatisfy((name) -> assertThat(taxonomy.resolve(name)).map(Skill::id).hasValue("spring-boot"));
	}

	@Test
	void givesTheKeyDiscoveryGivesForEveryCaseTheyShare() throws IOException {
		try (BufferedReader cases = new BufferedReader(new InputStreamReader(
				new ClassPathResource("taxonomy/name-forms.tsv").getInputStream(), StandardCharsets.UTF_8))) {
			assertThat(cases.lines().filter((line) -> !line.startsWith("#")).toList()).hasSizeGreaterThan(10)
				.allSatisfy((line) -> {
					int tab = line.lastIndexOf('\t');
					assertThat(TaxonomyStore.key(line.substring(0, tab))).as(line).isEqualTo(line.substring(tab + 1));
				});
		}
	}

	@Test
	void resolvesASkillThatHasNoAliases() {
		assertThat(this.taxonomy.resolve("terraform")).hasValue(new Skill("terraform", "Terraform", "devops", List.of(), Set.of(), null));
	}

	@Test
	void readsWhichOfASkillsKeysAreContextKeysAndTheSkillItBelongsTo() {
		TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader("""
				amazon-aurora\tAmazon Aurora\tdatabase\tAurora|AWS Aurora\tAurora\taws
				"""));
		assertThat(taxonomy.resolve("aurora")).hasValue(new Skill("amazon-aurora", "Amazon Aurora", "database",
				List.of("Aurora", "AWS Aurora"), Set.of("Aurora"), "aws"));
	}

	@Test
	void holdsEverySkillOnce() {
		assertThat(this.taxonomy.skills()).extracting(Skill::id)
			.containsExactlyInAnyOrder("postgresql", "c-sharp", "terraform");
	}

	@Test
	void resolvesNothingForANameItDoesNotHold() {
		assertThat(this.taxonomy.resolve("cobol")).isEmpty();
	}

	@Test
	void refusesTwoSkillsWithTheSameId() {
		assertThatIllegalStateException().isThrownBy(() -> TaxonomyStore.read(new StringReader("""
				go\tGo\tlanguage\tgolang
				go\tGo Template\ttool\t
				"""))).withMessageContaining("go");
	}

	@Test
	void refusesAKeyThatNamesTwoSkills() {
		assertThatIllegalStateException().isThrownBy(() -> TaxonomyStore.read(new StringReader("""
				javascript\tJavaScript\tlanguage\tjs
				java\tJava\tlanguage\tJS
				"""))).withMessageContaining("js");
	}

	@Test
	void allowsASkillToRepeatAKeyOfItsOwn() {
		TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader("kubernetes\tKubernetes\tdevops\tkubernetes|k8s|K8S\n"));
		assertThat(taxonomy.resolve("k8s")).map(Skill::id).hasValue("kubernetes");
	}

}
