package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class GreenhouseBoardUrlTest {

	@ParameterizedTest
	@CsvSource({
			"https://boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/mercadolibre/jobs/4012345, mercadolibre",
			"https://boards.greenhouse.io/mercadolibre?gh_src=abc, mercadolibre",
			"https://job-boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://www.boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/embed/job_board?for=mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/embed/job_app?for=mercadolibre&token=4012345, mercadolibre",
			"https://job-boards.eu.greenhouse.io/proton/jobs/123, proton",
			"https://boards.eu.greenhouse.io/embed/job_board?for=nice, nice" })
	void extractsSlug(String url, String slug) {
		assertThat(GreenhouseBoardUrl.slugFrom(url)).contains(slug);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://boards.greenhouse.io/embed/job_board",
			"https://boards.greenhouse.io/robots.txt",
			"https://boards.greenhouse.io/",
			"https://example.com/mercadolibre",
			"http://boards.greenhouse.io/mercadolibre",
			"http://job-boards.eu.greenhouse.io/proton",
			"https://job-boards.us.greenhouse.io/stripe" })
	void findsNoSlug(String url) {
		assertThat(GreenhouseBoardUrl.slugFrom(url)).isEmpty();
	}

	@Test
	void indexPatternsBringsTheFourDomains() {
		assertThat(GreenhouseBoardUrl.indexPatterns()).containsExactlyInAnyOrder("boards.greenhouse.io/",
				"job-boards.greenhouse.io/", "boards.eu.greenhouse.io/", "job-boards.eu.greenhouse.io/");
	}
}
