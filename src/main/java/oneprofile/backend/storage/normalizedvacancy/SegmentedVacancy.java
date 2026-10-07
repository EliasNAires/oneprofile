package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * One vacancy's description as cleaning cut it, for a pass that reads only the segments.
 *
 * @param vacancyId the vacancy it was derived from
 * @param descriptionSegments its cleaned description cut into segments, null until cleaning has cut it
 */
public record SegmentedVacancy(long vacancyId, List<Segment> descriptionSegments) {

	/**
	 * Its segments, none if cleaning has not cut it.
	 * @return the segments of its description
	 */
	public List<Segment> segments() {
		return (this.descriptionSegments != null) ? this.descriptionSegments : List.of();
	}

}
