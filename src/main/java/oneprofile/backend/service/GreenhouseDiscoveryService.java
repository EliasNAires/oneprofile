package oneprofile.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oneprofile.backend.client.CommonCrawlIndexClient;
import oneprofile.backend.client.WaybackCdxClient;
import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.BlacklistedSlugRepository;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.util.GreenhouseBoardUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Discovers the companies that use Greenhouse by asking web archives for every URL
 * under the Greenhouse board domains and saving the slug of each one.
 */
@Service
public class GreenhouseDiscoveryService {

	private static final Logger logger = LoggerFactory.getLogger(GreenhouseDiscoveryService.class);

	/**
	 * Each index sees only part of the companies, so one is not enough; ten already
	 * gather most of what CommonCrawl has, and older ones add less and less.
	 */
	private static final int RECENT_INDEXES = 10;

	/**
	 * CommonCrawl degradation is transient: an index that fails often answers on a later
	 * try. Three passes, no wait between them (not asked for).
	 */
	private static final int PASSES = 3;

	/**
	 * Reading Wayback takes about forty minutes, so new companies are saved as they come
	 * instead of at the end; a failure then loses at most one batch. Ten JDBC batches of 50.
	 */
	private static final int WAYBACK_BATCH = 500;

	private final CommonCrawlIndexClient indexClient;

	private final WaybackCdxClient waybackClient;

	private final CompanyRepository companyRepository;

	private final BlacklistedSlugRepository blacklistedSlugRepository;

	private final int waybackBatch;

	/** Marked so Spring picks this one: the other is there only for the test. */
	@Autowired
	public GreenhouseDiscoveryService(CommonCrawlIndexClient indexClient, WaybackCdxClient waybackClient,
			CompanyRepository companyRepository, BlacklistedSlugRepository blacklistedSlugRepository) {
		this(indexClient, waybackClient, companyRepository, blacklistedSlugRepository, WAYBACK_BATCH);
	}

	GreenhouseDiscoveryService(CommonCrawlIndexClient indexClient, WaybackCdxClient waybackClient,
			CompanyRepository companyRepository, BlacklistedSlugRepository blacklistedSlugRepository,
			int waybackBatch) {
		this.indexClient = indexClient;
		this.waybackClient = waybackClient;
		this.companyRepository = companyRepository;
		this.blacklistedSlugRepository = blacklistedSlugRepository;
		this.waybackBatch = waybackBatch;
	}

	/**
	 * Goes through the most recent CommonCrawl indexes one by one, saving what each
	 * one brings before asking the next. An index that fails is retried in the next
	 * pass, up to {@link #PASSES} passes; what the others found is kept meanwhile, and
	 * only an index that fails every pass ends up in {@code failedIndexes}.
	 */
	public CommonCrawlResult discoverOnRecentCommonCrawl() {
		List<IndexResult> indexes = new ArrayList<>();
		List<String> pending = this.indexClient.latestIndexIds(RECENT_INDEXES);
		List<String> failedIndexes = List.of();
		for (int pass = 1; pass <= PASSES && !pending.isEmpty(); pass++) {
			logger.info("CommonCrawl pass {} of {}: {} indexes {}", pass, PASSES, pending.size(), pending);
			List<String> failedThisPass = new ArrayList<>();
			for (String indexId : pending) {
				try {
					DiscoveryResult result = discoverOnIndex(indexId);
					indexes.add(new IndexResult(indexId, result));
					logger.info("CommonCrawl index {} pass {}: {} slugs found, {} new companies saved", indexId, pass,
							result.slugsFound(), result.newCompanies());
				}
				catch (RuntimeException ex) {
					logger.warn("CommonCrawl index {} failed on pass {}: {}", indexId, pass, ex.getMessage());
					failedThisPass.add(indexId);
				}
			}
			pending = failedThisPass;
			failedIndexes = failedThisPass;
		}
		return new CommonCrawlResult(indexes, failedIndexes);
	}

	/**
	 * Reads everything Wayback holds for both board domains, saving new companies in
	 * batches along the way. If the reading fails, the batches already saved stay, and a
	 * later run skips them as known.
	 */
	public DiscoveryResult discoverOnWayback() {
		Set<String> known = knownSlugs();
		int knownBefore = known.size();
		Set<String> found = new HashSet<>();
		List<Company> pending = new ArrayList<>();
		for (String pattern : GreenhouseBoardUrl.indexPatterns()) {
			this.waybackClient.forEachUrl(pattern, url -> GreenhouseBoardUrl.slugFrom(url).ifPresent(slug -> {
				found.add(slug);
				if (known.add(slug)) {
					pending.add(new Company(Ats.GREENHOUSE, slug));
					if (pending.size() == this.waybackBatch) {
						this.companyRepository.saveAll(pending);
						pending.clear();
					}
				}
			}));
		}
		this.companyRepository.saveAll(pending);
		return new DiscoveryResult(found.size(), known.size() - knownBefore);
	}

	private DiscoveryResult discoverOnIndex(String indexId) {
		// One set for both domains: it collapses repeated captures and the same company
		// reached through either domain.
		Set<String> slugs = new HashSet<>();
		for (String pattern : GreenhouseBoardUrl.indexPatterns()) {
			this.indexClient.forEachUrl(indexId, pattern,
					url -> GreenhouseBoardUrl.slugFrom(url).ifPresent(slugs::add));
		}
		return saveNew(slugs);
	}

	/**
	 * Deliberately not transactional around the reading: that is network time, and a
	 * transaction would hold a connection through all of it. {@code saveAll} opens its
	 * own short transaction, which still sends the inserts in batches.
	 */
	private DiscoveryResult saveNew(Set<String> slugs) {
		Set<String> known = knownSlugs();
		List<Company> newCompanies = slugs.stream()
				.filter(slug -> !known.contains(slug))
				.map(slug -> new Company(Ats.GREENHOUSE, slug))
				.toList();
		this.companyRepository.saveAll(newCompanies);
		return new DiscoveryResult(slugs.size(), newCompanies.size());
	}

	/** Slugs already known, whether as a saved company or as one discovery must not save again. */
	private Set<String> knownSlugs() {
		Set<String> known = new HashSet<>(this.companyRepository.findSlugsByAts(Ats.GREENHOUSE));
		known.addAll(this.blacklistedSlugRepository.findSlugsByAts(Ats.GREENHOUSE));
		return known;
	}

	public record DiscoveryResult(int slugsFound, int newCompanies) {
	}

	public record IndexResult(String indexId, DiscoveryResult result) {
	}

	public record CommonCrawlResult(List<IndexResult> indexes, List<String> failedIndexes) {
	}
}
