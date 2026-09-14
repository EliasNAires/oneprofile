package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.client.GreenhouseBoardClient;
import oneprofile.backend.client.GreenhouseBoardClient.BoardProbe;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.service.GreenhouseBoardProbeService.ProbeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClientException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class GreenhouseBoardProbeServiceTest {

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void takesDownTheStatusTheNameAndTheDateOfEveryCompany() {
		store("globant", "auth0", "mercadolibre");

		ProbeResult result = probe(Map.of(
				"globant", new BoardProbe(BoardStatus.ACTIVE, 3, "Globant"),
				"auth0", new BoardProbe(BoardStatus.EMPTY, 0, null),
				"mercadolibre", new BoardProbe(BoardStatus.NOT_FOUND, 0, null)));

		assertThat(result).isEqualTo(new ProbeResult(3, 1, 1, 1, 0));
		assertThat(reload("globant"))
				.extracting(Company::getBoardStatus, Company::getName)
				.containsExactly(BoardStatus.ACTIVE, "Globant");
		assertThat(reload("auth0"))
				.extracting(Company::getBoardStatus, Company::getName)
				.containsExactly(BoardStatus.EMPTY, null);
		assertThat(reload("globant").getLastProbedAt()).isNotNull();
	}

	@Test
	void keepsTheStoredNameWhenTheBoardNoLongerBringsOne() {
		store("globant");
		probe(Map.of("globant", new BoardProbe(BoardStatus.ACTIVE, 1, "Globant")));

		probe(Map.of("globant", new BoardProbe(BoardStatus.EMPTY, 0, null)));

		assertThat(reload("globant"))
				.extracting(Company::getBoardStatus, Company::getName)
				.containsExactly(BoardStatus.EMPTY, "Globant");
	}

	@Test
	void keepsGoingWhenOneBoardCannotBeReached() {
		store("globant", "auth0");

		// auth0 is missing from the canned answers, so the fake client fails on it.
		ProbeResult result = probe(Map.of("globant", new BoardProbe(BoardStatus.ACTIVE, 1, "Globant")));

		assertThat(result).isEqualTo(new ProbeResult(2, 0, 0, 1, 1));
		assertThat(reload("globant").getBoardStatus()).isEqualTo(BoardStatus.ACTIVE);
		assertThat(reload("auth0").getBoardStatus()).isNull();
	}

	@Test
	void logsProgressWithTheFailuresAccumulatedSoFar(CapturedOutput output) {
		store("globant", "auth0");

		// Neither slug is in the canned answers, so the fake client fails on both.
		new GreenhouseBoardProbeService(new FakeBoardClient(Map.of()), this.companies, Duration.ZERO, 1,
				Clock.systemUTC()).probeAll();

		assertThat(output.getOut())
				.contains("Greenhouse board probe progress: 1 of 2 companies (50%), 1 failed");
	}

	private void store(String... slugs) {
		for (String slug : slugs) {
			this.companies.save(new Company(Ats.GREENHOUSE, slug));
		}
		this.entityManager.flush();
		this.entityManager.clear();
	}

	private ProbeResult probe(Map<String, BoardProbe> probesBySlug) {
		ProbeResult result = new GreenhouseBoardProbeService(new FakeBoardClient(probesBySlug), this.companies,
				Duration.ZERO).probeAll();
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private Company reload(String slug) {
		return this.companies.findByAts(Ats.GREENHOUSE).stream()
				.filter(company -> company.getSlug().equals(slug))
				.findFirst()
				.orElseThrow();
	}

	/** Hands back a canned answer per slug; a slug with none stands for an unreachable board. */
	private static final class FakeBoardClient extends GreenhouseBoardClient {

		private final Map<String, BoardProbe> probesBySlug;

		private FakeBoardClient(Map<String, BoardProbe> probesBySlug) {
			this.probesBySlug = probesBySlug;
		}

		@Override
		public BoardProbe probe(String slug) {
			BoardProbe probe = this.probesBySlug.get(slug);
			if (probe == null) {
				throw new RestClientException("no route to the board of " + slug);
			}
			return probe;
		}
	}
}
