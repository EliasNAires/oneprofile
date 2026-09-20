package oneprofile.backend.normalizedvacancy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the derived facts the rule passes hold. */
@Configuration(proxyBeanMethods = false)
public class NormalizedVacancyConfiguration {

	@Bean
	NormalizedVacancies normalizedVacancies(NormalizedVacancyRepository repository) {
		return new NormalizedVacancies(repository);
	}

}
