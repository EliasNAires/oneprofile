package oneprofile.backend.workers.eligibility;

import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.Place;

/**
 * What the place rule read in one location string.
 *
 * @param places the places it names, in the order written, empty where it names none ({@code Remote})
 * @param unread the words it read as nothing, neither a place nor a word the hand table says is never one
 */
public record PlaceReading(List<Place> places, List<String> unread) {

	public PlaceReading {
		places = List.copyOf(places);
		unread = List.copyOf(unread);
	}

	/** Whether the string is residue: it names no place, and holds words that were not read. */
	public boolean residue() {
		return this.places.isEmpty() && !this.unread.isEmpty();
	}

}
