package oneprofile.backend.storage.vacancy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyEntity;
import oneprofile.backend.storage.company.CompanyRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the vacancies a sweep reads off boards. What is held for a company is a mirror of its
 * board: an opening the board still publishes is overwritten with what it says today, and one the
 * board has stopped publishing is dropped, because a vacancy that has left the board is closed.
 */
@Component
public class VacancyStore {

	private final VacancyRepository repository;

	private final CompanyRepository companies;

	public VacancyStore(VacancyRepository repository, CompanyRepository companies) {
		this.repository = repository;
		this.companies = companies;
	}

	/**
	 * Makes what is held for one company match what its board publishes. One board is read in
	 * seconds, so the whole reconciliation is one transaction.
	 * @param ats the ATS the board belongs to
	 * @param slug the slug whose board was read
	 * @param published every opening the board publishes
	 * @return what the reconciliation changed
	 * @throws IllegalStateException if no company is held under that slug
	 */
	@Transactional
	public Reconciliation mirror(AtsEnum ats, String slug, Collection<PublishedVacancy> published) {
		CompanyEntity company = this.companies.findByAtsAndSlug(ats, slug)
			.orElseThrow(() -> new IllegalStateException("No %s company is held under %s".formatted(ats, slug)));
		// Whatever is left once every published opening has been taken out of it is exactly what the
		// board has stopped publishing.
		Map<Long, VacancyEntity> held = new HashMap<>();
		for (VacancyEntity vacancy : this.repository.findByCompany(company)) {
			held.put(vacancy.externalId(), vacancy);
		}
		int added = 0;
		List<VacancyEntity> mirrored = new ArrayList<>(published.size());
		for (PublishedVacancy opening : published) {
			VacancyEntity vacancy = held.remove(opening.externalId());
			if (vacancy == null) {
				vacancy = new VacancyEntity(company, opening.externalId());
				added++;
			}
			vacancy.publishedAs(opening);
			mirrored.add(vacancy);
		}
		this.repository.saveAll(mirrored);
		this.repository.deleteAll(held.values());
		return new Reconciliation(added, mirrored.size() - added, held.size());
	}

	/**
	 * The titles of the vacancies held after one id, in id order. A pass that derives facts from
	 * titles walks the corpus with this, batch by batch, resuming from the last id it read.
	 * @param after the id to read past, 0 to start at the first vacancy
	 * @param batch how many to read at most
	 * @return their ids and titles, empty once there are none left
	 */
	@Transactional(readOnly = true)
	public List<VacancyTitle> titlesAfter(long after, int batch) {
		return this.repository.titlesAfter(after, Limit.of(batch));
	}

	/**
	 * What reconciling one board against what was held changed.
	 *
	 * @param added how many openings the board publishes that were not held
	 * @param updated how many held vacancies the board still publishes
	 * @param deleted how many held vacancies the board no longer publishes
	 */
	public record Reconciliation(int added, int updated, int deleted) {
	}

}
