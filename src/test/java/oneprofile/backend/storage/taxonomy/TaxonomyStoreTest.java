package oneprofile.backend.storage.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxonomyStoreTest {

	private final TaxonomyStore taxonomy = TaxonomyStore.read(new StringReader("""
			postgresql\tPostgreSQL\tdatabase\tpostgres|psql
			c-sharp\tC#\tlanguage\tcsharp|c sharp
			terraform\tTerraform\tdevops\t
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
	void resolvesASkillThatHasNoAliases() {
		assertThat(this.taxonomy.resolve("terraform")).hasValue(new Skill("terraform", "Terraform", "devops", List.of()));
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
