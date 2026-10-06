package oneprofile.backend.storage.normalizedvacancy;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The derived facts that are held. */
public interface NormalizedVacancyRepository extends ListCrudRepository<NormalizedVacancyEntity, Long> {

	/**
	 * What is held for a batch of vacancies.
	 * @param vacancyIds the vacancies to read
	 * @return the derived facts held for those of them that have any
	 */
	List<NormalizedVacancyEntity> findByVacancyIdIn(Collection<Long> vacancyIds);

	/**
	 * The cleaned titles held after one vacancy id, in id order, so that a pass over the whole
	 * corpus can be walked in batches and resumed from the last id it read.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return their vacancy ids and cleaned titles
	 */
	@Query("select new oneprofile.backend.storage.normalizedvacancy.NormalizedTitle(n.vacancyId, n.cleanedTitle) "
			+ "from NormalizedVacancy n where n.vacancyId > :after order by n.vacancyId")
	List<NormalizedTitle> cleanedTitlesAfter(long after, Limit limit);

	/**
	 * The pile held after one vacancy id, in id order, with each vacancy's description: every vacancy
	 * whose title was left unknown for a corpus reason, whatever the body has decided of it since.
	 * @param reasons the title reasons that make the pile
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return the pile's vacancies, their cleaned titles, title reasons, descriptions and segments
	 */
	@Query("select new oneprofile.backend.storage.normalizedvacancy.PileVacancy(n.vacancyId, n.cleanedTitle, "
			+ "n.classificationReason, v.description, n.descriptionSegments) "
			+ "from NormalizedVacancy n join Vacancy v on v.id = n.vacancyId "
			+ "where n.vacancyId > :after and n.classificationReason in :reasons order by n.vacancyId")
	List<PileVacancy> pileAfter(Collection<UnknownReasonEnum> reasons, long after, Limit limit);

	/**
	 * Puts every vacancy of the pile back to what its title left it: unknown, decided by the title.
	 * @param reasons the title reasons that make the pile
	 * @return how many vacancies the pile holds
	 */
	@Modifying
	@Query("update NormalizedVacancy n set "
			+ "n.classificationState = oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.UNKNOWN, "
			+ "n.classificationSignal = oneprofile.backend.storage.normalizedvacancy.ClassificationSignalEnum.TITLE "
			+ "where n.classificationReason in :reasons")
	int resetPile(Collection<UnknownReasonEnum> reasons);

}
