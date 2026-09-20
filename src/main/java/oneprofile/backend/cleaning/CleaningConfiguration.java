package oneprofile.backend.cleaning;

import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import oneprofile.backend.vacancy.Vacancies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the cleaning of the corpus. The rules themselves know nothing about Spring or about the
 * corpus, so a test constructs them and hands them a title.
 */
@Configuration(proxyBeanMethods = false)
public class CleaningConfiguration {

	/** Large enough that the corpus is walked in a couple of hundred transactions. */
	private static final int BATCH = 1000;

	@Bean
	TitleCleaning titleCleaning() {
		return new TitleCleaning();
	}

	@Bean
	CorpusCleaning corpusCleaning(TitleCleaning titleCleaning, Vacancies vacancies, NormalizedVacancies normalized) {
		return new CorpusCleaning(titleCleaning, vacancies, normalized, BATCH);
	}

}
