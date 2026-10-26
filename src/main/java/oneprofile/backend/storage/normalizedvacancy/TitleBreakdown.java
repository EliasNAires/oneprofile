package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import java.util.Set;

/**
 * What the title breakdown made of one vacancy's title (ADR-0024).
 *
 * @param normalizedTitles its normalized title, one per head the title names, in the order named;
 * empty where nothing is left once its facts are out, as of a bare {@code Manager}
 * @param titleSeniorities the levels named by the guarded seniority words, which cleaning leaves in
 * the title, empty if it names none
 * @param managementLevel the rank of the leadership word that leads an area, null if it names none
 */
public record TitleBreakdown(List<String> normalizedTitles, Set<SeniorityLevelEnum> titleSeniorities,
		ManagementLevelEnum managementLevel) {

	public TitleBreakdown {
		normalizedTitles = List.copyOf(normalizedTitles);
		titleSeniorities = Set.copyOf(titleSeniorities);
	}

}
