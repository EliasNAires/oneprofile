package oneprofile.backend.workers.stoplist;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentedVacancy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds the stoplist of skill discovery's recall (#60): the single words noise is made of, dropped
 * from what recall asks Jev about. Reads every segment of what the title kept in and of the pile, in
 * every language, and counts each word's piece occurrences.
 * <p>
 * It measures how many pieces there are, the words that cover 80% of them (the head), and what the
 * closed-class words of English and Spanish remove before the head is taken.
 */
@Component
public class StoplistRun {

	private static final int BATCH = 1000;

	/** How many of the most frequent words a report lists. */
	private static final int TOP = 50;

	/** The share of piece occurrences the head covers. */
	private static final double HEAD_SHARE = 0.8;

	private final NormalizedVacancyStore normalized;

	private final int batch;

	/**
	 * Reads the corpus in batches of a thousand vacancies.
	 * @param normalized where the segments are read
	 */
	@Autowired
	public StoplistRun(NormalizedVacancyStore normalized) {
		this(normalized, BATCH);
	}

	/**
	 * @param normalized where the segments are read
	 * @param batch how many vacancies are read at a time
	 */
	StoplistRun(NormalizedVacancyStore normalized, int batch) {
		this.normalized = normalized;
		this.batch = batch;
	}

	/**
	 * Counts every word's piece occurrences over the corpus.
	 * @return the counts, the head, and what the closed class removes
	 */
	public Report measure() {
		List<WordPieces> ranked = count().entrySet()
			.stream()
			.map((counted) -> new WordPieces(counted.getKey(), counted.getValue().pieces(counted.getKey())))
			.filter((word) -> word.pieces() > 0)
			.sorted(Comparator.comparingLong(WordPieces::pieces).reversed().thenComparing(WordPieces::word))
			.toList();
		long pieces = ranked.stream().mapToLong(WordPieces::pieces).sum();
		List<WordPieces> rest = ranked.stream().filter((word) -> !ClosedClassRule.closedClass(word.word())).toList();
		long restPieces = rest.stream().mapToLong(WordPieces::pieces).sum();
		long closedClassPieces = pieces - restPieces;
		return new Report(pieces, ranked.size(), head(ranked, HEAD_SHARE * pieces), closedClassPieces,
				(pieces == 0) ? 0 : (double) closedClassPieces / pieces, head(rest, HEAD_SHARE * restPieces),
				ranked.subList(0, Math.min(TOP, ranked.size())));
	}

	private Map<String, Count> count() {
		Map<String, Count> counts = new HashMap<>();
		long after = 0;
		while (true) {
			List<SegmentedVacancy> read = this.normalized.inAndPileAfter(after, this.batch);
			if (read.isEmpty()) {
				return counts;
			}
			for (SegmentedVacancy vacancy : read) {
				after = vacancy.vacancyId();
				for (Segment segment : vacancy.segments()) {
					for (String word : segment.words()) {
						boolean nameLike = PieceRule.nameLike(word);
						if (nameLike || PieceRule.lowercase(word)) {
							counts.computeIfAbsent(word.toLowerCase(Locale.ROOT), (form) -> new Count())
								.add(nameLike, vacancy.vacancyId());
						}
					}
				}
			}
		}
	}

	/**
	 * The fewest of the ranked words whose pieces reach a number.
	 * @param ranked words, the most frequent first
	 * @param reach how many pieces they have to hold between them
	 * @return how many words it takes
	 */
	private static int head(List<WordPieces> ranked, double reach) {
		long covered = 0;
		int words = 0;
		for (WordPieces word : ranked) {
			if (covered >= reach) {
				break;
			}
			covered += word.pieces();
			words++;
		}
		return words;
	}

	/**
	 * How one word is written across the corpus. The corpus is walked in vacancy id order, so the
	 * vacancies a word is written lowercase in are counted by noticing a new id.
	 */
	private static final class Count {

		private long nameLike;

		private long lowercase;

		private int lowercaseVacancies;

		private long lastLowercaseVacancy;

		void add(boolean nameLike, long vacancyId) {
			if (nameLike) {
				this.nameLike++;
				return;
			}
			this.lowercase++;
			if (vacancyId != this.lastLowercaseVacancy) {
				this.lowercaseVacancies++;
				this.lastLowercaseVacancy = vacancyId;
			}
		}

		long pieces(String word) {
			return PieceRule.pieces(word, this.nameLike, this.lowercase, this.lowercaseVacancies);
		}

	}

	/**
	 * One word and its piece occurrences.
	 *
	 * @param word the word, lowercased
	 * @param pieces how many times it is a piece across the corpus
	 */
	public record WordPieces(String word, long pieces) {
	}

	/**
	 * What one run measured.
	 *
	 * @param pieces the piece occurrences of single words across the corpus
	 * @param words how many distinct words are a piece at least once
	 * @param head how many of the most frequent words cover 80% of the pieces
	 * @param closedClassPieces the pieces that are closed-class words of English or Spanish
	 * @param closedClassShare their share of every piece
	 * @param headOfTheRest how many of the most frequent other words cover 80% of the pieces the closed
	 * class leaves
	 * @param top the fifty most frequent words
	 */
	public record Report(long pieces, int words, int head, long closedClassPieces, double closedClassShare,
			int headOfTheRest, List<WordPieces> top) {
	}

}
