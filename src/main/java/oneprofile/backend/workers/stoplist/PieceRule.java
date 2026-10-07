package oneprofile.backend.workers.stoplist;

import java.util.regex.Pattern;

/**
 * Which single words of a segment are pieces, the names recall would ask Jev about: a word written
 * name-like, and a lowercase word outside the known vocabulary. Ported from {@code pieces()} in
 * {@code scripts/skillloop.py} onto the words {@link oneprofile.backend.storage.normalizedvacancy.Segment}
 * cuts, for single words only, since only single words are stoplisted. Those words hold no slash, so
 * the script's cut at slashes has nothing to cut, and a word never starts with the dot of
 * {@code .NET}, which leaves {@code NET} capitalized.
 * <p>
 * As in the script, only a word of the ASCII letters {@code a} to {@code z} can be known vocabulary,
 * so a lowercase word with an accent is always a piece.
 */
final class PieceRule {

	/** A word written in lower case in at least this many vacancies is known vocabulary. */
	private static final int KNOWN_VACANCIES = 10;

	private static final Pattern ASCII_LOWERCASE = Pattern.compile("[a-z]+");

	private PieceRule() {
	}

	/**
	 * How many times a word is a piece, from how it is written across the corpus.
	 * @param word the word, lowercased
	 * @param nameLike how many times it is written name-like
	 * @param lowercase how many times it is written as a lowercase word
	 * @param lowercaseVacancies how many vacancies write it as a lowercase word
	 * @return every name-like occurrence, and the lowercase ones unless it is known vocabulary
	 */
	static long pieces(String word, long nameLike, long lowercase, int lowercaseVacancies) {
		boolean known = lowercaseVacancies >= KNOWN_VACANCIES && ASCII_LOWERCASE.matcher(word).matches();
		return nameLike + (known ? 0 : lowercase);
	}

	/**
	 * Whether a word is written the way names are: capitalized, or with a digit, {@code +}, {@code #}
	 * or {@code .} after its first character.
	 * @param word a word as it is written
	 * @return whether it is a piece wherever it is written
	 */
	static boolean nameLike(String word) {
		if (Character.isUpperCase(word.codePointAt(0))) {
			return true;
		}
		return word.substring(Character.charCount(word.codePointAt(0)))
			.codePoints()
			.anyMatch((c) -> Character.isDigit(c) || c == '+' || c == '#' || c == '.');
	}

	/**
	 * Whether a word is all letters and all of them lowercase: a piece unless it is known vocabulary.
	 * A word of a script without case is neither this
	 * nor name-like, and never a piece.
	 * @param word a word as it is written
	 * @return whether it is a lowercase word
	 */
	static boolean lowercase(String word) {
		return word.codePoints().allMatch(Character::isLetter) && word.codePoints().anyMatch(Character::isLowerCase)
				&& word.codePoints().noneMatch((c) -> Character.isUpperCase(c) || Character.isTitleCase(c));
	}

}
