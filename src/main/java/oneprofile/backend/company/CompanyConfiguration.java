package oneprofile.backend.company;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the companies discovery finds. */
@Configuration(proxyBeanMethods = false)
public class CompanyConfiguration {

	@Bean
	Companies companies(CompanyRepository repository) {
		return new Companies(repository);
	}

}
