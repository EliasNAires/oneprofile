package oneprofile.backend.storage.vacancy;

/**
 * One of the places beyond its own that an Ashby vacancy names for the work, as Ashby publishes it.
 *
 * @param location the place as Ashby writes it, such as "Remote (Canada)"
 * @param country the country Ashby gives the place, as it writes it, or null if it gave none
 */
public record SecondaryLocation(String location, String country) {
}
