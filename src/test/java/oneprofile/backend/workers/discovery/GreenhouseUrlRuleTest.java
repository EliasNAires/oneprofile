package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GreenhouseUrlRuleTest {

	@Test
	void readsTheSlugFromTheFirstPathSegment() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/eikontherapeutics/jobs/5027565007"))
			.contains("eikontherapeutics");
	}

	@Test
	void readsTheSlugOfABoardThatHasNothingAfterIt() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/creditas")).contains("creditas");
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/creditas/")).contains("creditas");
	}

	@Test
	void readsTheSlugOfEachOfTheFourHosts() {
		assertThat(GreenhouseUrlRule.slugIn("https://boards.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(GreenhouseUrlRule.slugIn("https://boards.eu.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.eu.greenhouse.io/acme/jobs/1")).contains("acme");
	}

	@Test
	void readsTheSlugAnEmbeddedBoardCarriesInItsForParameter() {
		assertThat(GreenhouseUrlRule.slugIn(
				"https://job-boards.greenhouse.io/embed/job_app?for=1stdibscom&token=8049439&utm_id=13791864"))
			.contains("1stdibscom");
		assertThat(GreenhouseUrlRule.slugIn("https://boards.greenhouse.io/embed/job_board?for=acme")).contains("acme");
	}

	@Test
	void hasNoSlugForAnEmbeddedBoardThatNamesNoBoard() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/embed/job_app?token=8049439")).isEmpty();
	}

	@Test
	void lowercasesTheSlugBecauseTheSameBoardIsPublishedInEitherCase() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/FetchRewards/jobs/1")).contains("fetchrewards");
	}

	@Test
	void hasNoSlugForAHostThatIsNotAGreenhouseBoard() {
		assertThat(GreenhouseUrlRule.slugIn("https://boards-api.greenhouse.io/v1/boards/xai/jobs")).isEmpty();
		assertThat(GreenhouseUrlRule.slugIn("https://developers.greenhouse.io/acme")).isEmpty();
		assertThat(GreenhouseUrlRule.slugIn("https://boards.greenhouse.io.example.com/acme")).isEmpty();
	}

	@Test
	void hasNoSlugForACaptureThatIsNotOverHttps() {
		assertThat(GreenhouseUrlRule.slugIn("http://job-boards.greenhouse.io/acme/jobs/1")).isEmpty();
	}

	@Test
	void hasNoSlugForTheHostOnItsOwn() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/")).isEmpty();
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io")).isEmpty();
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/?error=true")).isEmpty();
	}

	@Test
	void hasNoSlugForAFirstSegmentThatIsNotSpeltLikeASlug() {
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/acme%20corp/jobs/1")).isEmpty();
		assertThat(GreenhouseUrlRule.slugIn("https://job-boards.greenhouse.io/acme+corp")).isEmpty();
	}

	@Test
	void readsTheSlugWhateverCaseTheHostWasWrittenIn() {
		assertThat(GreenhouseUrlRule.slugIn("https://Job-Boards.Greenhouse.io/acme/jobs/1")).contains("acme");
	}

}
