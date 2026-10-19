package oneprofile.backend.storage.taxonomy;

import java.util.List;
import java.util.Set;

/**
 * One concept of the taxonomy: a named capability, and the other names it goes by.
 * @param id a stable readable slug derived from the canonical name, as in {@code c-sharp}
 * @param canonicalName the name the skill is shown by
 * @param category the kind of skill it is
 * @param aliases the other spellings and names that resolve to it
 * @param contextKeys those of its names that are context keys, in the casing they are found in
 * @param parent the id of the skill it belongs to, null if it belongs to none
 */
public record Skill(String id, String canonicalName, String category, List<String> aliases, Set<String> contextKeys,
		String parent) {

	public Skill {
		aliases = List.copyOf(aliases);
		contextKeys = Set.copyOf(contextKeys);
	}

}
