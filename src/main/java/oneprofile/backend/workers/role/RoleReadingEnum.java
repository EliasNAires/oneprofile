package oneprofile.backend.workers.role;

/** Which reading of {@code docs/role-criterion.md} gave a normalized title its role, or none. */
public enum RoleReadingEnum {

	/** The role list's titles file gives it, where the rule would be wrong. */
	LISTED,

	/** A job read whole (rule 3). */
	JOB,

	/** A word that decides wherever it stands in front of the head: a sale or a platform (rules 4, 6). */
	OVERRIDE,

	/** The kind of work named nearest in front of the head, past platform, infrastructure and software (rules 4, 5). */
	KIND,

	/** The head alone (rules 8, 9). */
	BARE,

	/** Nothing but stages of a career, which give no role (rule 7). */
	STAGE,

	/** A title with no head that names a kind of work, as the area a leader leads (rule 2). */
	AREA,

	/** No reading reaches it: its head, or the words in front of it, are not on the list. */
	UNREACHED

}
