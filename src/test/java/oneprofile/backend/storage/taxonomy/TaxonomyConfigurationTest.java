package oneprofile.backend.storage.taxonomy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

/**
 * Starting the context reads the taxonomy file the application ships, so a file with two skills
 * under one id, or a key naming two skills, fails here.
 */
class TaxonomyConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TaxonomyConfiguration.class);

	@Test
	void offersTheTaxonomyTheApplicationShips() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(TaxonomyStore.class));
	}

	@Test
	void resolvesEveryNameOfPostgresqlToOneSkill() {
		this.contextRunner.run((context) -> {
			TaxonomyStore taxonomy = context.getBean(TaxonomyStore.class);
			assertThat(List.of("postgres", "psql", "PostgreSQL"))
				.allSatisfy((name) -> assertThat(taxonomy.resolve(name)).map(Skill::id).hasValue("postgresql"));
		});
	}

	@Test
	void holdsTheSkillsItWasSeededWith() {
		this.contextRunner.run((context) -> {
			TaxonomyStore taxonomy = context.getBean(TaxonomyStore.class);
			assertThat(List.of("Terraform", "Angular", "Ansible"))
				.allSatisfy((name) -> assertThat(taxonomy.resolve(name)).isPresent());
		});
	}

	@Test
	void holdsTheSkillsTheCorpusSupports() throws IOException {
		try (Reader tsv = new InputStreamReader(new ClassPathResource("taxonomy/skills.tsv").getInputStream(),
				StandardCharsets.UTF_8)) {
			assertThat(new BufferedReader(tsv).lines().count()).isBetween(1800L, 2200L);
		}
	}

}
