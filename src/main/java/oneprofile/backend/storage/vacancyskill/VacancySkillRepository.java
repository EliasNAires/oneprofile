package oneprofile.backend.storage.vacancyskill;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

/** The skills found in vacancies that are held. */
public interface VacancySkillRepository extends ListCrudRepository<VacancySkillEntity, VacancySkillEntity.Key> {

	/**
	 * Deletes every row in one statement, rather than one per row.
	 * @return how many rows it deleted
	 */
	@Modifying
	@Query("delete from VacancySkill")
	int deleteEveryRow();

}
