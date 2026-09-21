package oneprofile.backend.classification;

import oneprofile.backend.normalizedvacancy.NormalizedVacancies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the classification of the corpus. The rules themselves know nothing about Spring or about
 * the corpus, so a test constructs them and hands them a title.
 */
@Configuration(proxyBeanMethods = false)
public class ClassificationConfiguration {

	/** Large enough that the corpus is walked in a couple of hundred transactions. */
	private static final int BATCH = 1000;

	@Bean
	TitleClassification titleClassification() {
		return new TitleClassification();
	}

	@Bean
	CorpusClassification corpusClassification(TitleClassification titleClassification,
			NormalizedVacancies normalized) {
		return new CorpusClassification(titleClassification, normalized, BATCH);
	}

}
