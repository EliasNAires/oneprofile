package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import oneprofile.backend.storage.company.AtsEnum;

/**
 * One vacancy the place run reads: one of the engineering subset, with every string of its declared location.
 *
 * @param vacancyId the vacancy it was derived from
 * @param ats the ATS that publishes it
 * @param locations its location strings, the location field first, empty where it publishes none
 */
public record PlaceVacancy(long vacancyId, AtsEnum ats, List<LocationString> locations) {

	public PlaceVacancy {
		locations = List.copyOf(locations);
	}

}
