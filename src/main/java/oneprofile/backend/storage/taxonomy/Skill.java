package oneprofile.backend.storage.taxonomy;

import java.util.List;

/**
 * One concept of the taxonomy: a named capability, and the other names it goes by.
 * @param id a stable readable slug derived from the canonical name, as in {@code c-sharp}
 * @param canonicalName the name the skill is shown by
 * @param category the kind of skill it is
 * @param aliases the other spellings and names that resolve to it
 */
public record Skill(String id, String canonicalName, String category, List<String> aliases) {

	public Skill {
		aliases = List.copyOf(aliases);
	}

}
