package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GreenhouseBoardUrlTest {

	@ParameterizedTest
	@CsvSource({
			"https://boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/mercadolibre/jobs/4012345, mercadolibre",
			"https://boards.greenhouse.io/mercadolibre?gh_src=abc, mercadolibre",
			"https://job-boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://www.boards.greenhouse.io/mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/embed/job_board?for=mercadolibre, mercadolibre",
			"https://boards.greenhouse.io/embed/job_app?for=mercadolibre&token=4012345, mercadolibre" })
	void extractsSlug(String url, String slug) {
		assertThat(GreenhouseBoardUrl.slugFrom(url)).contains(slug);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://boards.greenhouse.io/embed/job_board",
			"https://boards.greenhouse.io/robots.txt",
			"https://boards.greenhouse.io/",
			"https://example.com/mercadolibre",
			"http://boards.greenhouse.io/mercadolibre" })
	void findsNoSlug(String url) {
		assertThat(GreenhouseBoardUrl.slugFrom(url)).isEmpty();
	}
}
