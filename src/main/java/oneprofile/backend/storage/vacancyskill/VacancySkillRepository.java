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
	 * The skills found in the titles of some vacancies.
	 * @param normalizedVacancyIds the normalized vacancies
	 * @return their rows whose skill the title names
	 */
	List<VacancySkillEntity> findByNormalizedVacancyIdInAndInTitleTrue(Collection<Long> normalizedVacancyIds);

	/**
	 * How many vacancies the engineering subset holds. This and the counts below read the classification
	 * and roles of the derived facts the rows belong to, in SQL, since the roles are an array to unnest.
	 * @param subset the classification state of the subset, by name
	 * @return the count
	 */
	@Query(value = "select count(*) from normalized_vacancy where classification_state = :subset", nativeQuery = true)
	int countSubset(String subset);

	/**
	 * How many vacancies of the engineering subset name each skill.
	 * @param subset the classification state of the subset, by name
	 * @return rows of skill id and count
	 */
	@Query(value = "select s.skill_id, count(*) from vacancy_skill s join normalized_vacancy n on n.id = s.normalized_vacancy_id "
			+ "where n.classification_state = :subset group by s.skill_id", nativeQuery = true)
	List<Object[]> countSubsetNaming(String subset);

	/**
	 * How many vacancies of the engineering subset hold each role.
	 * @param subset the classification state of the subset, by name
	 * @return rows of role slug and count
	 */
	@Query(value = "select r.role, count(*) from normalized_vacancy n cross join unnest(n.roles) r(role) "
			+ "where n.classification_state = :subset group by r.role", nativeQuery = true)
	List<Object[]> countRoles(String subset);

	/**
	 * How many vacancies of the engineering subset holding each role name each skill.
	 * @param subset the classification state of the subset, by name
	 * @return rows of role slug, skill id and count
	 */
	@Query(value = "select r.role, s.skill_id, count(*) from normalized_vacancy n cross join unnest(n.roles) r(role) "
			+ "join vacancy_skill s on s.normalized_vacancy_id = n.id where n.classification_state = :subset "
			+ "group by r.role, s.skill_id", nativeQuery = true)
	List<Object[]> countRolesNaming(String subset);

	/**
	 * Deletes every row in one statement, rather than one per row.
	 * @return how many rows it deleted
	 */
	@Modifying
	@Query("delete from VacancySkill")
	int deleteEveryRow();

}
