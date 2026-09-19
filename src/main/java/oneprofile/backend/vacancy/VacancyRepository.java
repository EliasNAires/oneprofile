package oneprofile.backend.vacancy;

import java.util.List;
import oneprofile.backend.company.Company;
import org.springframework.data.repository.ListCrudRepository;

/** The vacancies that are held. */
public interface VacancyRepository extends ListCrudRepository<Vacancy, Long> {

	/**
	 * The vacancies held for one company.
	 * @param company the company to read
	 * @return every vacancy held for it
	 */
	List<Vacancy> findByCompany(Company company);

}
