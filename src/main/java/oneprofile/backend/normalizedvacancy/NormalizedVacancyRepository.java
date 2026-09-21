package oneprofile.backend.normalizedvacancy;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The derived facts that are held. */
public interface NormalizedVacancyRepository extends ListCrudRepository<NormalizedVacancy, Long> {

	/**
	 * What is held for a batch of vacancies.
	 * @param vacancyIds the vacancies to read
	 * @return the derived facts held for those of them that have any
	 */
	List<NormalizedVacancy> findByVacancyIdIn(Collection<Long> vacancyIds);

	/**
	 * The cleaned titles held after one vacancy id, in id order, so that a pass over the whole
	 * corpus can be walked in batches and resumed from the last id it read.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return their vacancy ids and cleaned titles
	 */
	@Query("select new oneprofile.backend.normalizedvacancy.NormalizedTitle(n.vacancyId, n.cleanedTitle) "
			+ "from NormalizedVacancy n where n.vacancyId > :after order by n.vacancyId")
	List<NormalizedTitle> cleanedTitlesAfter(long after, Limit limit);

}
