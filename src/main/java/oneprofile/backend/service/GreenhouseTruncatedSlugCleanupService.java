package oneprofile.backend.service;

import java.util.List;
import java.util.TreeSet;

import oneprofile.backend.model.Ats;
import oneprofile.backend.model.BlacklistedSlug;
import oneprofile.backend.model.BoardStatus;
import oneprofile.backend.model.Company;
import oneprofile.backend.repository.BlacklistedSlugRepository;
import oneprofile.backend.repository.CompanyRepository;
import oneprofile.backend.repository.VacancyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the Greenhouse companies whose board answered {@code NOT_FOUND} with a slug that
 * is a proper prefix of an {@code ACTIVE} company's slug: a line cut short by a truncated
 * Wayback or CommonCrawl index. Each one is removed for good — its vacancies and its
 * company row — and its slug is blacklisted so discovery never saves it again.
 */
@Service
public class GreenhouseTruncatedSlugCleanupService {

	private final CompanyRepository companyRepository;

	private final VacancyRepository vacancyRepository;

	private final BlacklistedSlugRepository blacklistedSlugRepository;

	public GreenhouseTruncatedSlugCleanupService(CompanyRepository companyRepository,
			VacancyRepository vacancyRepository, BlacklistedSlugRepository blacklistedSlugRepository) {
		this.companyRepository = companyRepository;
		this.vacancyRepository = vacancyRepository;
		this.blacklistedSlugRepository = blacklistedSlugRepository;
	}

	/**
	 * One short transaction for the whole cleanup: the counts it reads and the rows it
	 * writes must see a consistent snapshot, and a run touches at most a handful of
	 * companies.
	 */
	@Transactional
	public int cleanup() {
		TreeSet<String> activeSlugs = new TreeSet<>(
				this.companyRepository.findSlugsByAtsAndBoardStatus(Ats.GREENHOUSE, BoardStatus.ACTIVE));
		List<String> notFoundSlugs = this.companyRepository.findSlugsByAtsAndBoardStatus(Ats.GREENHOUSE,
				BoardStatus.NOT_FOUND);

		int removed = 0;
		for (String slug : notFoundSlugs) {
			if (isTruncated(slug, activeSlugs)) {
				remove(slug);
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Whether {@code slug} is a proper prefix of an active slug. Done on the sorted set
	 * with {@code ceiling} and {@code startsWith}, not with SQL {@code LIKE}: {@code _} is
	 * a valid slug character but also a {@code LIKE} wildcard.
	 */
	private boolean isTruncated(String slug, TreeSet<String> activeSlugs) {
		String next = activeSlugs.ceiling(slug);
		return next != null && next.startsWith(slug);
	}

	private void remove(String slug) {
		Company company = this.companyRepository.findByAtsAndSlug(Ats.GREENHOUSE, slug).orElseThrow();
		this.vacancyRepository.deleteAll(this.vacancyRepository.findByCompany(company));
		this.companyRepository.delete(company);
		this.blacklistedSlugRepository.save(new BlacklistedSlug(Ats.GREENHOUSE, slug));
	}
}
