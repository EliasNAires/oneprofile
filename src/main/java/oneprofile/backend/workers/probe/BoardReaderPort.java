package oneprofile.backend.workers.probe;

import java.io.IOException;
import oneprofile.backend.storage.company.RegionEnum;

/** Reads one board from its ATS, so that a probe can record what is there. */
public interface BoardReaderPort {

	/**
	 * Reads the board a slug names.
	 * @param slug the slug to read the board of
	 * @param region the region its company holds, which picks the API host where an ATS has one per
	 * region
	 * @param onRefusal run each time the ATS refuses a request (429 or 503), retried or not, so that
	 * the run asking slows down
	 * @return what the board is, including that no board is published under the slug
	 * @throws IOException if the ATS could not be read, which is not an answer about the board
	 */
	BoardReading read(String slug, RegionEnum region, Runnable onRefusal) throws IOException;

	/**
	 * Asks the ATS for the company's readable name where {@link #read} brings none.
	 * @param slug the slug whose company to name
	 * @param region the region whose host to ask
	 * @param onRefusal run each time the ATS refuses a request, so that the run slows down
	 * @return the readable name, or null when the ATS publishes none
	 * @throws IOException if the ATS could not be read
	 */
	default String name(String slug, RegionEnum region, Runnable onRefusal) throws IOException {
		return null;
	}

}
