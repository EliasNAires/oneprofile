package oneprofile.backend.storage.vacancy;

import java.util.List;
import oneprofile.backend.storage.company.CompanyEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The vacancies that are held. */
public interface VacancyRepository extends ListCrudRepository<VacancyEntity, Long> {

	/**
	 * The vacancies held for one company.
	 * @param company the company to read
	 * @return every vacancy held for it
	 */
	List<VacancyEntity> findByCompany(CompanyEntity company);

	/**
	 * The titles of the vacancies held after one id, in id order, so that a pass over the whole
	 * corpus can be walked in batches and resumed from the last id it read.
	 * @param after the id to read past, 0 to start at the first vacancy
	 * @param limit how many to read at most
	 * @return their ids and titles
	 */
	@Query("select new oneprofile.backend.storage.vacancy.VacancyTitle(v.id, v.title) from Vacancy v "
			+ "where v.id > :after order by v.id")
	List<VacancyTitle> titlesAfter(long after, Limit limit);

}
