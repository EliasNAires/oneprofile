package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * What cleaning made of one vacancy.
 *
 * @param cleanedTitle its cleaned title, and the levels the title named
 * @param descriptionSegments its cleaned description cut into segments, empty if it has none
 * @param detectedLanguage the language its description was detected to be in, as an ISO 639-1 code,
 * or null where its board declared one or the text did not say
 */
public record CleanedVacancy(CleanedTitle cleanedTitle, List<Segment> descriptionSegments, String detectedLanguage) {

	public CleanedVacancy {
		descriptionSegments = List.copyOf(descriptionSegments);
	}

}
