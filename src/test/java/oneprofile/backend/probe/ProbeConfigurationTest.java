package oneprofile.backend.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import oneprofile.backend.company.Companies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProbeConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(Companies.class, () -> mock(Companies.class))
			.withUserConfiguration(ProbeConfiguration.class);

	@Test
	void offersABoardProbeThatReadsFromGreenhouse() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BoardProbe.class);
			assertThat(context).getBean(BoardReader.class).isInstanceOf(GreenhouseBoardReader.class);
		});
	}

}
