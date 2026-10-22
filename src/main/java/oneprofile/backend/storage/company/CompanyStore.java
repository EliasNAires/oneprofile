package oneprofile.backend.storage.company;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the companies discovery finds. A slug is recorded once per ATS, so a crawl that names a
 * board already held adds nothing.
 */
@Component
public class CompanyStore {

	private final CompanyRepository repository;

	public CompanyStore(CompanyRepository repository) {
		this.repository = repository;
	}

	/**
	 * Records the slugs of one ATS as companies, each in the region its board was discovered in,
	 * skipping those already held. A company held already keeps the region it was first recorded in.
	 * @param ats the ATS the slugs belong to
	 * @param regionsBySlug the slugs discovery found, each with its region
	 * @return how many companies this added
	 */
	@Transactional
	public int record(AtsEnum ats, Map<String, RegionEnum> regionsBySlug) {
		Set<String> held = this.repository.slugsOf(ats);
		List<CompanyEntity> added = regionsBySlug.entrySet()
			.stream()
			.filter((discovered) -> !held.contains(discovered.getKey()))
			.map((discovered) -> new CompanyEntity(ats, discovered.getKey(), discovered.getValue()))
			.toList();
		this.repository.saveAll(added);
		return added.size();
	}

	/**
	 * The slugs held for one ATS, each with its region, in slug order, so that a run over them is
	 * repeatable.
	 * @param ats the ATS to read
	 * @return every slug held for it, with the region its board is asked for in
	 */
	@Transactional(readOnly = true)
	public SortedMap<String, RegionEnum> regionsOf(AtsEnum ats) {
		return regionsBySlug(this.repository.findByAts(ats));
	}

	/**
	 * The slugs held for one ATS whose board was last found to be of one kind, each with its region,
	 * in slug order, so that a run over them is repeatable.
	 * @param ats the ATS to read
	 * @param boardStatus what their boards were last found to be
	 * @return every slug held for it with that board status, with the region its board is asked for in
	 */
	@Transactional(readOnly = true)
	public SortedMap<String, RegionEnum> regionsOf(AtsEnum ats, BoardStatusEnum boardStatus) {
		return regionsBySlug(this.repository.findByAtsAndBoardStatus(ats, boardStatus));
	}

	private static SortedMap<String, RegionEnum> regionsBySlug(List<CompanyEntity> companies) {
		SortedMap<String, RegionEnum> regions = new TreeMap<>();
		companies.forEach((company) -> regions.put(company.slug(), company.region()));
		return regions;
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
	public void recordProbe(AtsEnum ats, String slug, BoardStatusEnum boardStatus, String name, Instant probedAt) {
		CompanyEntity company = this.repository.findByAtsAndSlug(ats, slug)
			.orElseThrow(() -> new IllegalStateException("No %s company is held under %s".formatted(ats, slug)));
		company.probed(boardStatus, name, probedAt);
		this.repository.save(company);
	}

}
