package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import oneprofile.backend.storage.company.AtsEnum;

/**
 * One vacancy the work-mode rule reads: one of the engineering subset, with what it declares of where
 * the work is done, its description included.
 *
 * @param vacancyId the vacancy it was derived from
 * @param ats the ATS that publishes it
 * @param title its title as published
 * @param location its location field, null if it has none
 * @param workModeField the work-mode field its ATS publishes, as the ATS writes it: Ashby's or Lever's
 * workplace type, or Workday's remote type; null if it has none, as no Greenhouse vacancy does
 * @param segments its description's segments, empty where cleaning cut none
 */
public record WorkModeVacancy(long vacancyId, AtsEnum ats, String title, String location, String workModeField,
		List<Segment> segments) {

	public WorkModeVacancy {
		segments = (segments != null) ? List.copyOf(segments) : List.of();
	}

}
