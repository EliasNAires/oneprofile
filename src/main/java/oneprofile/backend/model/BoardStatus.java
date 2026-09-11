package oneprofile.backend.model;

/**
 * What the job board of a company answered the last time it was probed.
 */
public enum BoardStatus {

	/** The ATS does not know the slug: the company does not use this ATS anymore. */
	NOT_FOUND,

	/** The board is there, with no job openings published. */
	EMPTY,

	/** The board is there and has job openings. */
	ACTIVE
}
