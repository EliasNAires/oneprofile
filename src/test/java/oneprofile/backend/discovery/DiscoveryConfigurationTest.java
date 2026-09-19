package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DiscoveryConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DiscoveryConfiguration.class);

	@Test
	void offersASlugDiscoveryThatReadsFromCommonCrawl() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GreenhouseSlugDiscovery.class);
			assertThat(context).hasSingleBean(ClusterIndexReader.class);
			assertThat(context).hasSingleBean(CdxBlockReader.class);
			assertThat(context).getBean(RangeReader.class).isInstanceOf(HttpRangeReader.class);
		});
	}

}
