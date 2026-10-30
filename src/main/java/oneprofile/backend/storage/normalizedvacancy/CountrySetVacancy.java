package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;
import java.util.Set;

/**
 * One vacancy the country-set pass reads (#13): one of the engineering subset, with its work modes, its title,
 * the places of its declared location and its description's segments.
 *
 * @param vacancyId the vacancy it was derived from
 * @param workModes its work modes, every mode it offers a choice of; empty where it declares none
 * @param title its title as the company wrote it
 * @param locations each string of its declared location with its places, by list and position; empty where it
 * publishes none or the place run has not read it
 * @param segments its description's segments, empty where cleaning cut none
 */
public record CountrySetVacancy(long vacancyId, Set<WorkModeEnum> workModes, String title, List<LocationPlaces> locations,
		List<Segment> segments) {

	public CountrySetVacancy {
		workModes = Set.copyOf(workModes);
		locations = List.copyOf(locations);
		segments = List.copyOf(segments);
	}

}
