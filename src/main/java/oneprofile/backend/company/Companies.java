package oneprofile.backend.company;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the companies discovery finds. A slug is recorded once per ATS, so a crawl that names a
 * board already held adds nothing.
 */
public class Companies {

	private final CompanyRepository repository;

	public Companies(CompanyRepository repository) {
		this.repository = repository;
	}

	/**
	 * Records the slugs of one ATS as companies, skipping those already held.
	 * @param ats the ATS the slugs belong to
	 * @param slugs the slugs discovery found
	 * @return how many companies this added
	 */
	@Transactional
	public int record(Ats ats, Collection<String> slugs) {
		Set<String> held = this.repository.slugsOf(ats);
		List<Company> added = slugs.stream()
			.distinct()
			.filter((slug) -> !held.contains(slug))
			.map((slug) -> new Company(ats, slug))
			.toList();
		this.repository.saveAll(added);
		return added.size();
	}

}
