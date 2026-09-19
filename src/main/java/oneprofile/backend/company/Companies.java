package oneprofile.backend.company;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

	/**
	 * The slugs held for one ATS, in slug order, so that a run over them is repeatable.
	 * @param ats the ATS to read
	 * @return every slug held for it
	 */
	@Transactional(readOnly = true)
	public SortedSet<String> slugsOf(Ats ats) {
		return new TreeSet<>(this.repository.slugsOf(ats));
	}

	/**
	 * Records on a company what a probe found on its board. Probing the same slug again replaces
	 * what the last probe recorded rather than adding anything.
	 * @param ats the ATS the slug belongs to
	 * @param slug the slug that was probed
	 * @param boardStatus what the probe found the board to be
	 * @param name the readable name the board gave, or null if it gave none
	 * @param probedAt when the probe was made
	 * @throws IllegalStateException if no company is held under that slug
	 */
	@Transactional
	public void recordProbe(Ats ats, String slug, BoardStatus boardStatus, String name, Instant probedAt) {
		Company company = this.repository.findByAtsAndSlug(ats, slug)
			.orElseThrow(() -> new IllegalStateException("No %s company is held under %s".formatted(ats, slug)));
		company.probed(boardStatus, name, probedAt);
		this.repository.save(company);
	}

}
