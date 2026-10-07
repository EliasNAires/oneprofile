package oneprofile.backend.workers.stoplist;

import java.io.IOException;

/** Asks Jev, out of context, whether a word could name a technology, so the stoplist's head is checked. */
public interface JevPort {

	/** The model every answer is asked of, as jev.py pins it. */
	String MODEL = "jev-1.13.0";

	/**
	 * Asks whether a word could name a technology, product or software tool in any context.
	 * @param word the word, lowercased
	 * @return Jev's answer and what it cost
	 * @throws IOException if Jev could not be asked or answered as another model
	 */
	JevAnswer ask(String word) throws IOException;

}
