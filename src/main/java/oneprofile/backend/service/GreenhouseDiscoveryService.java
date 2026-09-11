package oneprofile.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.util.GreenhouseBoardUrl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Discovers the companies that use Greenhouse by asking the CommonCrawl index for
 * every URL under the Greenhouse board domains and saving the slug of each one.
 */
@Service
public class GreenhouseDiscoveryService {

	private final CommonCrawlIndexClient indexClient;

	private final CompanyRepository companyRepository;

	public GreenhouseDiscoveryService(CommonCrawlIndexClient indexClient, CompanyRepository companyRepository) {
		this.indexClient = indexClient;
		this.companyRepository = companyRepository;
	}

	/**
	 * One transaction for the whole run: without it every company would be inserted in
	 * its own, which is thousands of round trips, and a failure halfway would leave the
	 * table half loaded.
	 */
	@Transactional
	public DiscoveryResult discover(String indexId) {
		// One set for both domains: it collapses repeated captures and the same company
		// reached through either domain.
		Set<String> slugs = new HashSet<>();
		for (String pattern : GreenhouseBoardUrl.indexPatterns()) {
			this.indexClient.forEachUrl(indexId, pattern,
					url -> GreenhouseBoardUrl.slugFrom(url).ifPresent(slugs::add));
		}

		Set<String> known = new HashSet<>(this.companyRepository.findSlugsByAts(Ats.GREENHOUSE));
		List<Company> newCompanies = slugs.stream()
				.filter(slug -> !known.contains(slug))
				.map(slug -> new Company(Ats.GREENHOUSE, slug))
				.toList();
		this.companyRepository.saveAll(newCompanies);

		return new DiscoveryResult(slugs.size(), newCompanies.size());
	}

	public record DiscoveryResult(int slugsFound, int newCompanies) {
	}
}