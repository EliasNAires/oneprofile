package oneprofile.backend.storage.rolelist;

/** What a word or phrase of a normalized title does in the role rule, as {@code docs/role-criterion.md} reads it. */
public enum RoleWordUseEnum {

	/** A head the title ends in, whose role the words in front of it can decide (rules 8, 9). */
	HEAD,

	/** A head that names a job and is read whole, whatever stands in front of it (rule 3). */
	JOB,

	/** A stage of a career, read past (rule 7). */
	STAGE,

	/** A word in front of the head that names the kind of work (rule 4). */
	KIND,

	/** A kind that yields to any other kind in front of the head: {@code platform}, {@code software} (rule 5). */
	YIELDING,

	/** A word that decides wherever it stands in front of the heads it names: a sale, a platform (rules 4, 6). */
	OVERRIDE

}
