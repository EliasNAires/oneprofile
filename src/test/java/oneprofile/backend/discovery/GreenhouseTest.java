package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GreenhouseTest {

	@Test
	void readsTheSlugFromTheFirstPathSegment() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/eikontherapeutics/jobs/5027565007"))
			.contains("eikontherapeutics");
	}

	@Test
	void readsTheSlugOfABoardThatHasNothingAfterIt() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/creditas")).contains("creditas");
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/creditas/")).contains("creditas");
	}

	@Test
	void readsTheSlugOfEachOfTheFourHosts() {
		assertThat(Greenhouse.slugIn("https://boards.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(Greenhouse.slugIn("https://boards.eu.greenhouse.io/acme/jobs/1")).contains("acme");
		assertThat(Greenhouse.slugIn("https://job-boards.eu.greenhouse.io/acme/jobs/1")).contains("acme");
	}

	@Test
	void readsTheSlugAnEmbeddedBoardCarriesInItsForParameter() {
		assertThat(Greenhouse.slugIn(
				"https://job-boards.greenhouse.io/embed/job_app?for=1stdibscom&token=8049439&utm_id=13791864"))
			.contains("1stdibscom");
		assertThat(Greenhouse.slugIn("https://boards.greenhouse.io/embed/job_board?for=acme")).contains("acme");
	}

	@Test
	void hasNoSlugForAnEmbeddedBoardThatNamesNoBoard() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/embed/job_app?token=8049439")).isEmpty();
	}

	@Test
	void lowercasesTheSlugBecauseTheSameBoardIsPublishedInEitherCase() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/FetchRewards/jobs/1")).contains("fetchrewards");
	}

	@Test
	void hasNoSlugForAHostThatIsNotAGreenhouseBoard() {
		assertThat(Greenhouse.slugIn("https://boards-api.greenhouse.io/v1/boards/xai/jobs")).isEmpty();
		assertThat(Greenhouse.slugIn("https://developers.greenhouse.io/acme")).isEmpty();
		assertThat(Greenhouse.slugIn("https://boards.greenhouse.io.example.com/acme")).isEmpty();
	}

	@Test
	void hasNoSlugForACaptureThatIsNotOverHttps() {
		assertThat(Greenhouse.slugIn("http://job-boards.greenhouse.io/acme/jobs/1")).isEmpty();
	}

	@Test
	void hasNoSlugForTheHostOnItsOwn() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/")).isEmpty();
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io")).isEmpty();
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/?error=true")).isEmpty();
	}

	@Test
	void hasNoSlugForAFirstSegmentThatIsNotSpeltLikeASlug() {
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/acme%20corp/jobs/1")).isEmpty();
		assertThat(Greenhouse.slugIn("https://job-boards.greenhouse.io/acme+corp")).isEmpty();
	}

	@Test
	void readsTheSlugWhateverCaseTheHostWasWrittenIn() {
		assertThat(Greenhouse.slugIn("https://Job-Boards.Greenhouse.io/acme/jobs/1")).contains("acme");
	}

}
