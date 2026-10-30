package oneprofile.backend.storage.placelist;

/**
 * One row of the place list's hand table.
 *
 * @param name the name as a location string writes it
 * @param kind what the row says of it
 * @param target what it names, by its kind; empty for an ignored name
 * @param latitude a town's latitude, null for any other kind
 * @param longitude a town's longitude, null for any other kind
 */
public record HandPlace(String name, HandPlaceKindEnum kind, String target, Double latitude, Double longitude) {
}
