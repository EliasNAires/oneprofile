package oneprofile.backend.storage.normalizedvacancy;

/**
 * One string of a vacancy's declared location, as its ATS publishes it.
 *
 * @param list the list it came from
 * @param position where in that list it stands, from 0
 * @param location the string
 * @param country the country the ATS names for it, as the ATS writes it, null where it names none: Ashby's
 * address country for the location field, and the country of each secondary location
 */
public record LocationString(LocationListEnum list, int position, String location, String country) {
}
