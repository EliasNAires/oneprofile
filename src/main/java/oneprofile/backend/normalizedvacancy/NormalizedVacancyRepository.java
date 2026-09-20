package oneprofile.backend.normalizedvacancy;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/** The derived facts that are held. */
public interface NormalizedVacancyRepository extends ListCrudRepository<NormalizedVacancy, Long> {

	/**
	 * What is held for a batch of vacancies.
	 * @param vacancyIds the vacancies to read
	 * @return the derived facts held for those of them that have any
	 */
	List<NormalizedVacancy> findByVacancyIdIn(Collection<Long> vacancyIds);

}
