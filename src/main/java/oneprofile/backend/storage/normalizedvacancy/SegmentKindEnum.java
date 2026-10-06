package oneprofile.backend.storage.normalizedvacancy;

/** What a segment of a description is. */
public enum SegmentKindEnum {

	/** The title of a section: what the items and sentences after it sit under. */
	HEADING,

	/** One entry of a list. */
	ITEM,

	/** One sentence of prose, or a line that is neither a heading nor an item. */
	SENTENCE

}
