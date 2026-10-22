package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.company.RegionEnum;
import org.junit.jupiter.api.Test;

class AshbyUrlRuleTest {

	@Test
	void readsTheSlugFromTheFirstPathSegment() {
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/ramp/34413f8d-26bf-4bbc-8ade-eb309a0e2245"))
			.contains("ramp");
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/ramp")).contains("ramp");
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/ramp/")).contains("ramp");
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/ramp?utm_source=x")).contains("ramp");
	}

	@Test
	void placesEveryBoardInTheUsSinceAshbyHasOneHost() {
		assertThat(AshbyUrlRule.regionIn("https://jobs.ashbyhq.com/ramp")).isEqualTo(RegionEnum.US);
	}

	@Test
	void holdsTheSlugLowercasedBecauseAshbyServesTheSameBoardInEitherCase() {
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/Ramp/34413f8d/application")).contains("ramp");
	}

	@Test
	void hasNoSlugForAnotherHostOrAPathThatIsNoBoard() {
		assertThat(AshbyUrlRule.slugIn("https://www.ashbyhq.com/ramp")).isEmpty();
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/")).isEmpty();
		assertThat(AshbyUrlRule.slugIn("http://jobs.ashbyhq.com/ramp")).isEmpty();
		assertThat(AshbyUrlRule.slugIn("https://jobs.ashbyhq.com/%E2%80%8Bramp")).isEmpty();
	}

}
