package oneprofile.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import oneprofile.backend.TestcontainersConfiguration;
import oneprofile.backend.client.CommonCrawlIndexClient;
import oneprofile.backend.client.WaybackCdxClient;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.service.GreenhouseDiscoveryService.CommonCrawlResult;
import oneprofile.backend.service.GreenhouseDiscoveryService.DiscoveryResult;
import oneprofile.backend.service.GreenhouseDiscoveryService.IndexResult;
import oneprofile.backend.util.GreenhouseBoardUrl;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClientException;

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
		DiscoveryResult result = discoverOnOneIndex(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/mercadolibre",
				"https://boards.greenhouse.io/mercadolibre/jobs/4001",
				"https://boards.greenhouse.io/mercadolibre?token=abc",
				"https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(2, 2));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("mercadolibre", "globant");
	}

	@Test
	void treatsTheTwoBoardDomainsAsTheSameCompany() {
		DiscoveryResult result = discoverOnOneIndex(Map.of(
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

		DiscoveryResult result = discoverOnOneIndex(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/globant",
				"https://boards.greenhouse.io/auth0"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(2, 1));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	@Test
	void ignoresIndexedUrlsThatDoNotIdentifyACompany() {
		DiscoveryResult result = discoverOnOneIndex(Map.of(BOARDS, List.of(
				"https://boards.greenhouse.io/robots.txt",
				"https://boards.greenhouse.io/",
				"https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()));

		assertThat(result).isEqualTo(new DiscoveryResult(1, 1));
		assertThat(slugsInDatabase()).containsExactly("globant");
	}

	@Test
	void goesThroughEveryRecentIndexCountingOnlyWhatEachOneAdds() {
		CommonCrawlResult result = discover(List.of("CC-MAIN-2026-34", "CC-MAIN-2026-30"), Map.of(
				"CC-MAIN-2026-34", Map.of(BOARDS, List.of("https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()),
				"CC-MAIN-2026-30", Map.of(BOARDS, List.of("https://boards.greenhouse.io/globant"),
						JOB_BOARDS, List.of("https://job-boards.greenhouse.io/auth0"))));

		assertThat(result).isEqualTo(new CommonCrawlResult(List.of(
				new IndexResult("CC-MAIN-2026-34", new DiscoveryResult(1, 1)),
				new IndexResult("CC-MAIN-2026-30", new DiscoveryResult(2, 1))), List.of()));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	@Test
	void keepsWhatTheOtherIndexesFoundWhenOneFails() {
		CommonCrawlResult result = discover(List.of("CC-MAIN-2026-34", "CC-MAIN-2026-30", "CC-MAIN-2026-25"), Map.of(
				"CC-MAIN-2026-34", Map.of(BOARDS, List.of("https://boards.greenhouse.io/globant"), JOB_BOARDS, List.of()),
				"CC-MAIN-2026-25", Map.of(BOARDS, List.of("https://boards.greenhouse.io/auth0"), JOB_BOARDS, List.of())));

		assertThat(result.failedIndexes()).containsExactly("CC-MAIN-2026-30");
		assertThat(result.indexes()).extracting(IndexResult::indexId).containsExactly("CC-MAIN-2026-34", "CC-MAIN-2026-25");
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	@Test
	void savesWhatWaybackBringsThroughBothDomainsExceptTheKnownCompanies() {
		this.companies.save(new Company(Ats.GREENHOUSE, "globant"));
		this.entityManager.flush();
		this.entityManager.clear();

		DiscoveryResult result = discoverOnWayback(500, new FakeWaybackClient(Map.of(
				BOARDS, List.of("https://boards.greenhouse.io/globant", "http://boards.greenhouse.io/ignored"),
				JOB_BOARDS, List.of("https://job-boards.greenhouse.io/auth0", "https://job-boards.greenhouse.io/globant")),
				Integer.MAX_VALUE));

		assertThat(result).isEqualTo(new DiscoveryResult(2, 1));
		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	@Test
	void keepsTheBatchesAlreadySavedWhenWaybackFailsHalfway() {
		FakeWaybackClient failsAfterThreeUrls = new FakeWaybackClient(Map.of(
				BOARDS, List.of("https://boards.greenhouse.io/globant", "https://boards.greenhouse.io/auth0",
						"https://boards.greenhouse.io/stripe", "https://boards.greenhouse.io/never-read"),
				JOB_BOARDS, List.of()), 3);

		assertThatThrownBy(() -> discoverOnWayback(2, failsAfterThreeUrls)).isInstanceOf(RestClientException.class);

		assertThat(slugsInDatabase()).containsExactlyInAnyOrder("globant", "auth0");
	}

	private DiscoveryResult discoverOnOneIndex(Map<String, List<String>> urlsByPattern) {
		return discover(List.of(INDEX_ID), Map.of(INDEX_ID, urlsByPattern)).indexes().getFirst().result();
	}

	private CommonCrawlResult discover(List<String> indexIds, Map<String, Map<String, List<String>>> urlsByIndex) {
		CommonCrawlResult result = service(new FakeIndexClient(indexIds, urlsByIndex), noWayback(), 500)
				.discoverOnRecentCommonCrawl();
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private DiscoveryResult discoverOnWayback(int batch, FakeWaybackClient waybackClient) {
		DiscoveryResult result = service(new FakeIndexClient(List.of(), Map.of()), waybackClient, batch)
				.discoverOnWayback();
		this.entityManager.flush();
		this.entityManager.clear();
		return result;
	}

	private GreenhouseDiscoveryService service(FakeIndexClient indexClient, FakeWaybackClient waybackClient,
			int batch) {
		return new GreenhouseDiscoveryService(indexClient, waybackClient, this.companies, batch);
	}

	private static FakeWaybackClient noWayback() {
		return new FakeWaybackClient(Map.of(BOARDS, List.of(), JOB_BOARDS, List.of()), Integer.MAX_VALUE);
	}

	private List<String> slugsInDatabase() {
		return this.companies.findSlugsByAts(Ats.GREENHOUSE);
	}

	/**
	 * Hands back canned indexes and URLs; never touches the network. An index in the
	 * list but absent from the map stands for one that cannot be read.
	 */
	private static final class FakeIndexClient extends CommonCrawlIndexClient {

		private final List<String> indexIds;

		private final Map<String, Map<String, List<String>>> urlsByIndex;

		private FakeIndexClient(List<String> indexIds, Map<String, Map<String, List<String>>> urlsByIndex) {
			urlsByIndex.values().forEach(urlsByPattern -> assertThat(urlsByPattern.keySet())
					.containsExactlyInAnyOrderElementsOf(GreenhouseBoardUrl.indexPatterns()));
			this.indexIds = indexIds;
			this.urlsByIndex = urlsByIndex;
		}

		@Override
		public List<String> latestIndexIds(int count) {
			return this.indexIds;
		}

		@Override
		public void forEachUrl(String indexId, String pattern, Consumer<String> onUrl) {
			Map<String, List<String>> urlsByPattern = this.urlsByIndex.get(indexId);
			if (urlsByPattern == null) {
				throw new RestClientException("Index " + indexId + " is unreachable");
			}
			urlsByPattern.get(pattern).forEach(onUrl);
		}
	}

	/**
	 * Hands back canned URLs per domain; never touches the network. It stops working after
	 * {@code urlsBeforeFailing} URLs, standing for an archive that fails halfway.
	 */
	private static final class FakeWaybackClient extends WaybackCdxClient {

		private final Map<String, List<String>> urlsByPattern;

		private int urlsLeft;

		private FakeWaybackClient(Map<String, List<String>> urlsByPattern, int urlsBeforeFailing) {
			assertThat(urlsByPattern.keySet()).containsExactlyInAnyOrderElementsOf(GreenhouseBoardUrl.indexPatterns());
			this.urlsByPattern = urlsByPattern;
			this.urlsLeft = urlsBeforeFailing;
		}

		@Override
		public void forEachUrl(String pattern, Consumer<String> onUrl) {
			for (String url : this.urlsByPattern.get(pattern)) {
				if (this.urlsLeft-- == 0) {
					throw new RestClientException("Wayback is unreachable");
				}
				onUrl.accept(url);
			}
		}
	}
}
