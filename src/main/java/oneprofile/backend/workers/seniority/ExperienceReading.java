package oneprofile.backend.workers.seniority;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.ExperienceRequirement;

/**
 * What the experience requirement rule read in one description.
 *
 * @param requiredYears the vacancy's required years, null where it states no experience requirement
 * @param requirements every experience requirement it accepted, in the order written
 */
public record ExperienceReading(Integer requiredYears, List<ExperienceRequirement> requirements) {

	public ExperienceReading {
		requirements = List.copyOf(requirements);
	}

}
