package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * What cleaning made of one vacancy.
 *
 * @param cleanedTitle its cleaned title, and the levels the title named
 * @param descriptionSegments its cleaned description cut into segments, empty if it has none
 */
public record CleanedVacancy(CleanedTitle cleanedTitle, List<Segment> descriptionSegments) {

	public CleanedVacancy {
		descriptionSegments = List.copyOf(descriptionSegments);
	}

}
