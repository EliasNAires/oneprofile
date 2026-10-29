package oneprofile.backend.workers.seniority;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import org.springframework.stereotype.Component;

/**
 * Gives a vacancy its one seniority level (PRD §6.1). A level word in the title sets it, the lowest
 * where the title names several ({@code Senior/Staff}); otherwise the required years do: 0–1 entry,
 * 2 junior, 3–4 mid, 5–7 senior, 8+ staff. A numbered level ({@code Engineer II}) has no place on the
 * ladder of its own, so a title naming only one is placed by its required years.
 */
@Component
public class SeniorityRule {

	/** The levels on the ladder, which a title naming them places a vacancy at. */
	private static final Set<SeniorityLevelEnum> LADDER = EnumSet.range(SeniorityLevelEnum.ENTRY, SeniorityLevelEnum.PRINCIPAL);

	/**
	 * The level a title places a vacancy at.
	 * @param titleSeniorities every level its title names
	 * @return the lowest of them on the ladder, or null where it names none there
	 */
	public SeniorityLevelEnum namedSeniorityLevel(Set<SeniorityLevelEnum> titleSeniorities) {
		return titleSeniorities.stream().filter(LADDER::contains).min(Comparator.naturalOrder()).orElse(null);
	}

	/**
	 * Whether a title names a numbered level and no level on the ladder: one only its required years can place.
	 * @param titleSeniorities every level its title names
	 * @return whether it names only numbered levels, and at least one
	 */
	public boolean namesOnlyNumberedLevels(Set<SeniorityLevelEnum> titleSeniorities) {
		return !titleSeniorities.isEmpty() && titleSeniorities.stream().noneMatch(LADDER::contains);
	}

	/**
	 * The level some required years place a vacancy at.
	 * @param requiredYears its required years
	 * @return their level on the ladder
	 */
	public SeniorityLevelEnum seniorityLevelOf(int requiredYears) {
		if (requiredYears <= 1) {
			return SeniorityLevelEnum.ENTRY;
		}
		if (requiredYears == 2) {
			return SeniorityLevelEnum.JUNIOR;
		}
		if (requiredYears <= 4) {
			return SeniorityLevelEnum.MID;
		}
		return (requiredYears <= 7) ? SeniorityLevelEnum.SENIOR : SeniorityLevelEnum.STAFF;
	}

	/**
	 * Gives one vacancy its level.
	 * @param titleSeniorities every level its title names
	 * @param requiredYears its required years, null where its description states none
	 * @return its seniority level, or null where neither says one
	 */
	public SeniorityLevelEnum seniorityLevel(Set<SeniorityLevelEnum> titleSeniorities, Integer requiredYears) {
		SeniorityLevelEnum named = namedSeniorityLevel(titleSeniorities);
		if (named != null) {
			return named;
		}
		return (requiredYears != null) ? seniorityLevelOf(requiredYears) : null;
	}

}
