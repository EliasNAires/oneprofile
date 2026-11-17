package oneprofile.backend.workers.sweep;

import java.io.IOException;

/**
 * The ATS answered that no board is published under the slug, the answer a probe records as
 * {@code NOT_FOUND}, as opposed to failing to answer.
 */
public class BoardNotFoundException extends IOException {

	/** Describes the ATS's answer that no board is published under the slug. */
	public BoardNotFoundException(String message) {
		super(message);
	}

}
