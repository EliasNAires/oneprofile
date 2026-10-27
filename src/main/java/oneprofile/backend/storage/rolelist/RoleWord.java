package oneprofile.backend.storage.rolelist;

/**
 * One line of the role list's words: a word or phrase of a normalized title and the role it gives.
 * @param phrase the word or phrase, as the title breakdown writes it
 * @param use what it does in the rule
 * @param head for a kind, the head it gives this role beside, empty for any head; for an override, the
 * heads it decides beside, separated by {@code |}, empty for any head; empty otherwise
 * @param role the slug of the role it gives, {@link RoleListStore#NONE} for no role, null where the
 * criterion does not say (a head with nothing in front of it) or where nothing is given (a stage)
 */
public record RoleWord(String phrase, RoleWordUseEnum use, String head, String role) {
}
