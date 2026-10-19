package oneprofile.backend.storage.vacancyskill;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The skills found in vacancies that are held. */
public interface VacancySkillRepository extends ListCrudRepository<VacancySkillEntity, VacancySkillEntity.Key> {

	/**
	 * The skills found in some vacancies.
	 * @param normalizedVacancyIds the normalized vacancies
	 * @return their rows
	 */
	List<VacancySkillEntity> findByNormalizedVacancyIdIn(Collection<Long> normalizedVacancyIds);

	/**
	 * Deletes every row in one statement, rather than one per row.
	 * @return how many rows it deleted
	 */
	@Modifying
	@Query("delete from VacancySkill")
	int deleteEveryRow();

}
