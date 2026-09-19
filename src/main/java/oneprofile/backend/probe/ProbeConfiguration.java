package oneprofile.backend.probe;

import java.time.Clock;
import java.time.Duration;
import oneprofile.backend.company.Ats;
import oneprofile.backend.company.Companies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the probing of Greenhouse boards. The probe itself knows nothing about Spring, so that a
 * test can hand it boards of its own rather than the live ATS.
 */
@Configuration(proxyBeanMethods = false)
public class ProbeConfiguration {

	/** How this client identifies itself to the ATS it asks. */
	private static final String USER_AGENT = "oneprofile (+https://github.com/EliasNAires/oneprofile)";

	/** The 130 boards a minute Greenhouse served without complaint before the reset. */
	private static final Duration PACE = Duration.ofMinutes(1).dividedBy(130);

	@Bean
	BoardReader greenhouseBoardReader() {
		return new GreenhouseBoardReader(USER_AGENT);
	}

	@Bean
	BoardProbe greenhouseBoardProbe(BoardReader boardReader, Companies companies) {
		return new BoardProbe(Ats.GREENHOUSE, boardReader, companies, Clock.systemUTC(), PACE);
	}

}
