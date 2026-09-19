package oneprofile.backend.vacancy;

import oneprofile.backend.company.CompanyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the vacancies a sweep holds. */
@Configuration(proxyBeanMethods = false)
public class VacancyConfiguration {

	@Bean
	Vacancies vacancies(VacancyRepository repository, CompanyRepository companies) {
		return new Vacancies(repository, companies);
	}

}
