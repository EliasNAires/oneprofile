package oneprofile.backend.model;

/**
 * Where the job is done, when the vacancy says so.
 *
 * <p>A null work mode means the vacancy does not declare one, not that the job is on
 * site: 112.508 of the 128.953 vacancies name no mode at all, and reading them as
 * presence would invent the data. It is the same meaning a null {@code boardStatus} has
 * on {@link Company}.
 *
 * <p>The values are the ones the data actually spells. Measuring first is what kept
 * "telecommute", "telework", "teletrabajo" and "a distancia" out of the extractor: all
 * four are zero across the whole dataset.
 */
public enum WorkMode {

	/** "remote", "remoto", "home based", "work from home", "wfh". */
	REMOTE,

	/**
	 * Remote and naming no place at all, so anyone can apply. It is a case of
	 * {@link #REMOTE} rather than a mode of its own: most remote vacancies do name a
	 * country or a region ("Remote - US"), and this is the 19% that does not.
	 */
	FULLY_REMOTE,

	/** "hybrid" or "hibrido". */
	HYBRID,

	/** "onsite", "on site", "in office", "in person", "presencial", "field based". */
	ONSITE
}
