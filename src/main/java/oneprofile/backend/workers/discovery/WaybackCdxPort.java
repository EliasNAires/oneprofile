package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.Optional;

/**
 * Asks the Wayback Machine's CDX index for the captures under one URL prefix, one chunk at a time.
 * A chunk is the index's whole answer to one query, as JSON: the captured URLs and, while more
 * remain, the resume key that asks for the next chunk.
 */
public interface WaybackCdxPort {

	/**
	 * Asks for one chunk of the captures under a prefix.
	 * @param urlPrefix the URL prefix without its scheme, such as {@code boards.greenhouse.io/}, or
	 * {@code *.myworkdayjobs.com/} for every capture on a domain and its subdomains
	 * @param resumeKey the key the previous chunk closed with, or empty for the first chunk
	 * @return the chunk as the index answered it: a closed JSON array, or an empty body if nothing
	 * was captured under the prefix
	 * @throws IOException if the index gave no whole answer
	 */
	String chunk(String urlPrefix, Optional<String> resumeKey) throws IOException;

}
