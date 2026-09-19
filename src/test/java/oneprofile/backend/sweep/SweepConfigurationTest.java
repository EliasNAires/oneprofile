package oneprofile.backend.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import oneprofile.backend.company.Companies;
import oneprofile.backend.vacancy.Vacancies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SweepConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(Companies.class, () -> mock(Companies.class))
		.withBean(Vacancies.class, () -> mock(Vacancies.class))
		.withUserConfiguration(SweepConfiguration.class);

	@Test
	void offersABoardSweepThatReadsFromGreenhouse() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BoardSweep.class);
			assertThat(context).getBean(VacancyReader.class).isInstanceOf(GreenhouseVacancyReader.class);
		});
	}

}
