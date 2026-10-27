package oneprofile.backend.storage.rolenearness;

/**
 * How near two roles are by the skills their vacancies ask for (ADR-0024). It is the same both ways, so
 * a pair is held once, its roles in order of slug.
 *
 * @param role the slug of the pair's first role
 * @param nearRole the slug of its second role
 * @param nearness from 0, no weighed skill in common, to 1, the same shares of every skill
 */
public record RoleNearness(String role, String nearRole, double nearness) {
}
