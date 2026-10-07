package oneprofile.backend.workers.stoplist;

import java.util.ArrayList;
import java.util.List;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;

/**
 * Where a word is written mid-sentence, the only place its case says whether it is written as a
 * name: the first word of a sentence is capitalized whatever it is, and so is every word of a
 * heading written in title case.
 */
final class CapitalizationRule {

	private CapitalizationRule() {
	}

	/**
	 * The words of a sentence or an item written mid-sentence: every word but the first, and but one
	 * right after a full stop, question mark, exclamation mark or colon. A word with no letter that has
	 * a case, such as {@code 802.11}, says nothing by its case and is left out. A heading has none.
	 * @param segment a segment
	 * @return its mid-sentence words as they are written, in order
	 */
	static List<String> midSentence(Segment segment) {
		if (segment.kind() == SegmentKindEnum.HEADING) {
			return List.of();
		}
		String text = segment.text();
		List<String> words = new ArrayList<>();
		int end = -1;
		// Nothing between two words is a letter or a digit, and a word starts with one, so the next
		// occurrence of a word past the end of the last one is where it was cut.
		for (String word : segment.words()) {
			int start = text.indexOf(word, Math.max(end, 0));
			if (end >= 0 && !startsSentence(text.substring(end, start)) && firstCased(word) >= 0) {
				words.add(word);
			}
			end = start + word.length();
		}
		return words;
	}

	/**
	 * Whether a word's first letter that has a case is upper case, past any digits before it, so
	 * {@code 10G} is capitalized and {@code iSCSI} is not.
	 * @param word a word as it is written, with a letter that has a case
	 * @return whether it is written capitalized
	 */
	static boolean capitalized(String word) {
		int first = firstCased(word);
		return Character.isUpperCase(first) || Character.isTitleCase(first);
	}

	private static int firstCased(String word) {
		return word.codePoints()
			.filter((c) -> Character.isUpperCase(c) || Character.isLowerCase(c) || Character.isTitleCase(c))
			.findFirst()
			.orElse(-1);
	}

	private static boolean startsSentence(String between) {
		return between.chars().anyMatch((c) -> ".?!:".indexOf(c) >= 0);
	}

}
