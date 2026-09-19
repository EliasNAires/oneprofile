package oneprofile.backend.sweep;

import java.time.Duration;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.Companies;
import oneprofile.backend.vacancy.Vacancies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the sweeping of Greenhouse boards. The sweep itself knows nothing about Spring, so that a
 * test can hand it boards of its own rather than the live ATS.
 */
@Configuration(proxyBeanMethods = false)
public class SweepConfiguration {

	/** How this client identifies itself to the ATS it asks. */
	private static final String USER_AGENT = "oneprofile (+https://github.com/EliasNAires/oneprofile)";

	/** Slower than a probe's pace, because every answer here carries a whole board's descriptions. */
	private static final Duration PACE = Duration.ofMillis(500);

	@Bean
	VacancyReader greenhouseVacancyReader() {
		return new GreenhouseVacancyReader(USER_AGENT);
	}

	@Bean
	BoardSweep greenhouseBoardSweep(VacancyReader vacancyReader, Companies companies, Vacancies vacancies) {
		return new BoardSweep(Ats.GREENHOUSE, vacancyReader, companies, vacancies, PACE);
	}

}
