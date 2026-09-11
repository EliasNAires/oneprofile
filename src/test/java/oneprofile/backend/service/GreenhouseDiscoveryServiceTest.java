package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
import oneprofile.backend.util.GreenhouseBoardUrl;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class GreenhouseDiscoveryServiceTest {

	private static final String INDEX_ID = "CC-MAIN-2026-34";

	private static final String BOARDS = "boards.greenhouse.io/";

	private static final String JOB_BOARDS = "job-boards.greenhouse.io/";

	@Autowired
	private CompanyRepository companies;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesOneCompanyPerSlugNoMatterHowManyCapturesItHas() {
		DiscoveryResult result = discover(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/mercadolibre",
				"https://boards.greenhouse.io/mercadolibre/jobs/4001",
				"https://boards.greenhouse.io/mercadolibre?token=abc",
				"https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(2, 2));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("mercadolibre", "globant");
	}

	@Test
	void treatsTheTwoBoardDomainsAsTheSameCompany() {
		DiscoveryResult result = discover(Map.of(
				BOARDS, List.of("https://boards.greenhouse.io/globant"),
				JOB_BOARDS, List.of("https://job-boards.greenhouse.io/globant")));

		assertThat(result).isEqualTo(new DiscoveryResult(1, 1));
		assertThat(slugsInDatabase()).containsExactly("globant");
	}

	@Test
	void doesNotSaveCompaniesThatAreAlreadyStored() {
		this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		this.entityManager.flush();
		this.entityManager.clear();

		DiscoveryResult result = discover(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/globant",
				"https://boards.greenhouse.io/auth0"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(2, 1));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	@Test
	void ignoresIndexedUrlsThatDoNotIdentifyACompany() {
		DiscoveryResult result = discover(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/robots.txt",
				"https://boards.greenhouse.io/",
				"https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(1, 1));
		assertThat(slugsInDatabase()).containsExactly("globant");
	}

	private DiscoveryResult discover(Map<String, List<String>> urlsByPattern) {
		DiscoveryResult result = new GreenhouseDiscoveryService(new FakeIndexClient(urlsByPattern), this.companies)
				.discover(INDEX_ID);
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private List<String> slugsInDatabase() {
		return this.companies.findSlugsByAts(Ats.GREENHOUSE);
	}

	/** Hands back canned URLs per pattern; never touches the network. */
	private static final class FakeIndexClient extends CommonCrawlIndexClient {

		private final Map<String, List<String>> urlsByPattern;

		private FakeIndexClient(Map<String, List<String>> urlsByPattern) {
			assertThat(urlsByPattern.keySet()).containsExactlyInAnyOrderElementsOf(GreenhouseBoardUrl.indexPatterns());
			this.urlsByPattern = urlsByPattern;
		}

		@Override
		public void forEachUrl(String indexId, String pattern, Consumer<String> onUrl) {
			assertThat(indexId).isEqualTo(INDEX_ID);
			this.urlsByPattern.get(pattern).forEach(onUrl);
		}
	}
}
