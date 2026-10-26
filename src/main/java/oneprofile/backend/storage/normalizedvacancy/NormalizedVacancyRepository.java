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
	@Query("select new oneprofile.backend.storage.normalizedvacancy.ClassificationVacancy(n.vacancyId, n.cleanedTitle) "
			+ "from NormalizedVacancy n where n.vacancyId > :after order by n.vacancyId")
	List<ClassificationVacancy> cleanedTitlesAfter(long after, Limit limit);

	/**
	 * The pile held after one vacancy id, in id order, with each vacancy's language and segments: every
	 * vacancy whose title was left unknown for a corpus reason, whatever the body has decided of it since.
	 * Its language is the one its board declared, or else the one cleaning detected.
	 * @param reasons the reasons a vacancy of the pile can hold
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return the pile's vacancies, their cleaned titles, reasons, languages and segments
	 */
	@Query("select new oneprofile.backend.storage.normalizedvacancy.PileVacancy(n.id, n.vacancyId, "
			+ "n.cleanedTitle, n.classificationReason, coalesce(treat(v as GreenhouseVacancy).language, n.detectedLanguage), n.descriptionSegments) "
			+ "from NormalizedVacancy n join Vacancy v on v.id = n.vacancyId "
			+ "where n.vacancyId > :after and n.classificationReason in :reasons order by n.vacancyId")
	List<PileVacancy> pileAfter(Collection<UnknownReasonEnum> reasons, long after, Limit limit);

	/**
	 * What skill extraction reads after one vacancy id, in id order: every vacancy the title decided in,
	 * and every one of the pile, whatever the body has decided of it since, in a language the body pass
	 * reads.
	 * @param pileReasons the title reasons that make the pile
	 * @param languages the languages the body pass reads, against the language its board declared or else
	 * the one cleaning detected; a vacancy with neither is read
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return the vacancies, their cleaned titles and segments
	 */
	@Query("select new oneprofile.backend.storage.normalizedvacancy.ExtractionVacancy(n.id, n.vacancyId, "
			+ "n.cleanedTitle, n.descriptionSegments) "
			+ "from NormalizedVacancy n join Vacancy v on v.id = n.vacancyId where n.vacancyId > :after and ("
			+ "(n.classificationState = oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.IN "
			+ "and n.classificationSignal = oneprofile.backend.storage.normalizedvacancy.ClassificationSignalEnum.TITLE) "
			+ "or (n.classificationReason in :pileReasons and (coalesce(treat(v as GreenhouseVacancy).language, n.detectedLanguage) is null "
			+ "or coalesce(treat(v as GreenhouseVacancy).language, n.detectedLanguage) in :languages))) "
			+ "order by n.vacancyId")
	List<ExtractionVacancy> extractionScopeAfter(Collection<UnknownReasonEnum> pileReasons,
			Collection<String> languages, long after, Limit limit);

	/**
	 * The engineering subset held after one vacancy id, in id order, with each vacancy's title as
	 * published and its location field.
	 * @param after the vacancy id to read past, 0 to start at the first
	 * @param limit how many to read at most
	 * @return the vacancies, their titles and locations
	 */
	@Query("select new oneprofile.backend.storage.normalizedvacancy.BreakdownVacancy(n.id, n.vacancyId, v.title, v.location) "
			+ "from NormalizedVacancy n join Vacancy v on v.id = n.vacancyId where n.vacancyId > :after "
			+ "and n.classificationState = oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.IN "
			+ "order by n.vacancyId")
	List<BreakdownVacancy> breakdownScopeAfter(long after, Limit limit);

	/**
	 * Lets go of every vacancy's normalized titles and management level.
	 * @return how many vacancies had been broken down
	 */
	@Modifying
	@Query("update NormalizedVacancy n set n.normalizedTitles = null, n.managementLevel = null "
			+ "where n.normalizedTitles is not null")
	int clearTitleBreakdowns();

	/**
	 * Lets go of every level the title breakdown read, in one statement rather than one per vacancy.
	 * @return how many levels it let go of
	 */
	@Modifying
	@Query(value = "delete from normalized_vacancy_guarded_title_seniority", nativeQuery = true)
	int clearGuardedTitleSeniorities();

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
