package oneprofile.backend.workers.role;

import java.util.Set;
import oneprofile.backend.storage.rolelist.RoleListStore;

/**
 * The role the role rule read off one normalized title, and how.
 * @param role the slug of its role, or {@link RoleListStore#NONE}
 * @param reading which reading gave it
 * @param competing the roles, or {@link RoleListStore#NONE}, that other words in front of the head give,
 * which the one that decided overrode; empty where the title's words agree
 */
public record RoleReading(String role, RoleReadingEnum reading, Set<String> competing) {

	public RoleReading {
		competing = Set.copyOf(competing);
	}

}
