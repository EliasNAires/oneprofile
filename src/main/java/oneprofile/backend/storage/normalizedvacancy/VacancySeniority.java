package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * The seniority the seniority pass derived for one vacancy (PRD §6.1).
 *
 * @param seniorityLevel its one seniority level, null where neither its title nor its required years say one
 * @param requiredYears its required years, null where its description states no experience requirement
 * @param requirements every experience requirement the pass accepted in its description
 */
public record VacancySeniority(SeniorityLevelEnum seniorityLevel, Integer requiredYears,
		List<ExperienceRequirement> requirements) {

	public VacancySeniority {
		requirements = List.copyOf(requirements);
	}

}
