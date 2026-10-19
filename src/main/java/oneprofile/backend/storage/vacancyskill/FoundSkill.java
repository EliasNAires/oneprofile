package oneprofile.backend.storage.vacancyskill;

import java.util.List;

/**
 * A skill extraction found in one vacancy, and where it found it.
 *
 * @param skillId the skill, by its id in the taxonomy
 * @param segments the indexes of the description's segments it was found in, in order, empty when it
 * was found only in the title
 * @param inTitle whether the title names it
 * @param fromContextKeys whether only context keys found it, no plain key of its own
 */
public record FoundSkill(String skillId, List<Integer> segments, boolean inTitle, boolean fromContextKeys) {

	public FoundSkill {
		segments = List.copyOf(segments);
	}

}
