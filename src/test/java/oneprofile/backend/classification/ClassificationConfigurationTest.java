package oneprofile.backend.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClassificationConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(NormalizedVacancies.class, () -> mock(NormalizedVacancies.class))
		.withUserConfiguration(ClassificationConfiguration.class);

	@Test
	void offersAClassificationOfTheWholeCorpus() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CorpusClassification.class);
			assertThat(context).hasSingleBean(TitleClassification.class);
		});
	}

}
