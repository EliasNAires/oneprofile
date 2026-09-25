package oneprofile.backend.workers.probe;

import java.io.IOException;

/** Reads one board from its ATS, so that a probe can record what is there. */
public interface BoardReaderPort {

	/**
	 * Reads the board a slug names.
	 * @param slug the slug to read the board of
	 * @return what the board is, including that no board is published under the slug
	 * @throws IOException if the ATS could not be read, which is not an answer about the board
	 */
	BoardReading read(String slug) throws IOException;

}
