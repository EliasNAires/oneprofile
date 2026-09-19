package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DiscoveryConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DiscoveryConfiguration.class);

	@Test
	void offersAClusterIndexReaderThatReadsFromCommonCrawl() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ClusterIndexReader.class);
			assertThat(context).getBean(RangeReader.class).isInstanceOf(HttpRangeReader.class);
		});
	}

}
