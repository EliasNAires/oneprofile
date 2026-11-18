package oneprofile.backend.storage.company;

/**
 * Whether a board was found and whether it had openings when it was last probed, or that it is on the
 * list of boards the sweep does not read. A company that has never been probed has no board status at
 * all, which is what unknown means.
 */
public enum BoardStatusEnum {

	/** The board is published and has openings. */
	ACTIVE,

	/** The board is published but has no openings. */
	EMPTY,

	/** No board is published under the slug. */
	NOT_FOUND,

	/** The board is on the list of boards the sweep does not read. */
	UNREAD

}
