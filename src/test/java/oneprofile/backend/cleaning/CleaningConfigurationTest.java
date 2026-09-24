package oneprofile.backend.cleaning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import oneprofile.backend.vacancy.Vacancies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CleaningConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(Vacancies.class, () -> mock(Vacancies.class))
		.withBean(NormalizedVacancies.class, () -> mock(NormalizedVacancies.class))
		.withUserConfiguration(CleaningConfiguration.class);

	@Test
	void offersACleaningOfTheWholeCorpus() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CorpusCleaning.class);
			assertThat(context).hasSingleBean(TitleCleaning.class);
		});
	}

	@Test
	void offersACleaningOfDescriptions() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DescriptionCleaning.class));
	}

}
