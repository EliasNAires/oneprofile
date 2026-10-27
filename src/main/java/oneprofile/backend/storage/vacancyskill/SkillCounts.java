package oneprofile.backend.storage.vacancyskill;

import java.util.Map;

/**
 * The skills a group of vacancies asks for: the engineering subset, or the vacancies of one role.
 *
 * @param vacancies how many vacancies the group holds, those naming no skill included
 * @param vacanciesNaming how many of them name each skill, by skill id; a skill none names is left out
 */
public record SkillCounts(int vacancies, Map<String, Integer> vacanciesNaming) {

	public SkillCounts {
		vacanciesNaming = Map.copyOf(vacanciesNaming);
	}

}
