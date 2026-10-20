package oneprofile.backend.storage.vacancy;

/**
 * One of the places beyond its own that a vacancy names for the work, as an ATS that structures
 * them publishes it.
 *
 * @param location the place as the ATS writes it, such as "Remote (Canada)"
 * @param country the country the ATS gives the place, as it writes it, or null if it gave none
 */
public record SecondaryLocation(String location, String country) {
}
