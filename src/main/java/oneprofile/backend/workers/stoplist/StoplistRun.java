package oneprofile.backend.workers.stoplist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentedVacancy;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the stoplist of skill discovery's recall (#60): the single words noise is made of, dropped
 * from what recall asks Jev about. Reads every segment of what the title kept in and of the pile, in
 * every language, and counts each word's piece occurrences.
 * <p>
 * It measures how many pieces there are, the words that cover 80% of them (the head), and what the
 * closed-class words of English and Spanish remove before the head is taken. It assembles the guard
 * set, and sets the capitalization cutoff at the lowest mid-sentence capitalized share of a guard word,
 * so that a word is let through only when it is written capitalized less often than every guard word.
 * <p>
 * The decision record and Jev's recall labels are read from the repository, so it runs from the
 * repository's root.
 */
@Component
public class StoplistRun {

	private static final int BATCH = 1000;

	/** How many of the most frequent words a report lists. */
	private static final int TOP = 50;

	/** The share of piece occurrences the head covers. */
	private static final double HEAD_SHARE = 0.8;

	private static final Path DECISIONS = Path.of("src/main/resources/taxonomy/decisions.tsv");

	private static final Path RECALL_LABELS = Path.of("docs/measurements/skill-recall-labels-jev.jsonl");

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	private final NormalizedVacancyStore normalized;

	private final TaxonomyStore taxonomy;

	private final Path decisions;

	private final Path recallLabels;

	private final int batch;

	/**
	 * Reads the corpus in batches of a thousand vacancies, and the repository's decision record and
	 * recall labels.
	 * @param normalized where the segments are read
	 * @param taxonomy the skills whose keys join the guard set
	 */
	@Autowired
	public StoplistRun(NormalizedVacancyStore normalized, TaxonomyStore taxonomy) {
		this(normalized, taxonomy, DECISIONS, RECALL_LABELS, BATCH);
	}

	/**
	 * @param normalized where the segments are read
	 * @param taxonomy the skills whose keys join the guard set
	 * @param decisions the decision record, whose keeps join the guard set
	 * @param recallLabels Jev's verdicts on pieces, whose keeps join the guard set
	 * @param batch how many vacancies are read at a time
	 */
	StoplistRun(NormalizedVacancyStore normalized, TaxonomyStore taxonomy, Path decisions, Path recallLabels,
			int batch) {
		this.normalized = normalized;
		this.taxonomy = taxonomy;
		this.decisions = decisions;
		this.recallLabels = recallLabels;
		this.batch = batch;
	}

	/**
	 * Counts every word's piece occurrences over the corpus.
	 * @return the counts, the head, and what the closed class removes
	 */
	public Report measure() {
		Map<String, Count> counts = count();
		List<WordPieces> ranked = counts.entrySet()
			.stream()
			.map((counted) -> new WordPieces(counted.getKey(), counted.getValue().pieces(counted.getKey())))
			.filter((word) -> word.pieces() > 0)
			.sorted(Comparator.comparingLong(WordPieces::pieces).reversed().thenComparing(WordPieces::word))
			.toList();
		long pieces = ranked.stream().mapToLong(WordPieces::pieces).sum();
		List<WordPieces> rest = ranked.stream().filter((word) -> !ClosedClassRule.closedClass(word.word())).toList();
		long restPieces = rest.stream().mapToLong(WordPieces::pieces).sum();
		long closedClassPieces = pieces - restPieces;
		int headOfTheRest = head(rest, HEAD_SHARE * restPieces);
		Set<String> guardSet = guardSet();
		List<WordShare> guardShares = guardSet.stream()
			.flatMap((word) -> share(word, counts).stream())
			.sorted(Comparator.comparingDouble(WordShare::share).thenComparing(WordShare::word))
			.toList();
		double cutoff = guardShares.isEmpty() ? 0 : guardShares.getFirst().share();
		int capitalizationLetsThrough = (int) rest.subList(0, headOfTheRest)
			.stream()
			.flatMap((word) -> share(word.word(), counts).stream())
			.filter((word) -> word.share() < cutoff)
			.count();
		return new Report(pieces, ranked.size(), head(ranked, HEAD_SHARE * pieces), closedClassPieces,
				(pieces == 0) ? 0 : (double) closedClassPieces / pieces, headOfTheRest,
				ranked.subList(0, Math.min(TOP, ranked.size())), guardSet.size(), cutoff, capitalizationLetsThrough,
				guardShares.stream().filter((word) -> word.share() == cutoff).toList());
	}

	/**
	 * The guard set: the single words among the keys of the taxonomy, the bare forms reopened, the
	 * names Jev found or kept in context, and the decision record's keeps.
	 */
	private Set<String> guardSet() {
		Set<String> names = new HashSet<>(this.taxonomy.keys());
		names.addAll(GuardSetRule.REOPENED);
		lines(this.decisions).stream()
			.skip(1)
			.map((row) -> row.split("\t"))
			.filter((row) -> row[1].equals("keep"))
			.forEach((row) -> names.add(row[0]));
		// Revisions before 4 answered for the whole segment, listing the names it found in it; later
		// ones keep or drop one name.
		lines(this.recallLabels).stream().filter((row) -> !row.isBlank()).map(JSON::readTree).forEach((row) -> {
			row.path("found").forEach((found) -> names.add(found.asString()));
			if (row.path("decision").asString("").equals("keep")) {
				names.add(row.get("name_form").asString());
			}
		});
		return GuardSetRule.words(names);
	}

	private static List<String> lines(Path file) {
		try {
			return Files.readAllLines(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * How often a word is written capitalized mid-sentence, unless it never is written mid-sentence.
	 */
	private static Optional<WordShare> share(String word, Map<String, Count> counts) {
		Count count = counts.get(word);
		if (count == null || count.midSentence == 0) {
			return Optional.empty();
		}
		return Optional.of(new WordShare(word, (double) count.capitalized / count.midSentence, count.midSentence));
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
					for (String word : CapitalizationRule.midSentence(segment)) {
						counts.computeIfAbsent(word.toLowerCase(Locale.ROOT), (form) -> new Count())
							.writtenMidSentence(CapitalizationRule.capitalized(word));
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

		private long midSentence;

		private long capitalized;

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

		void writtenMidSentence(boolean capitalized) {
			this.midSentence++;
			if (capitalized) {
				this.capitalized++;
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
	 * How often one word is written capitalized mid-sentence.
	 *
	 * @param word the word, lowercased
	 * @param share the share of its mid-sentence mentions written capitalized
	 * @param midSentence how many times it is written mid-sentence
	 */
	public record WordShare(String word, double share, long midSentence) {
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
	 * @param guardSetSize how many single words are known skills
	 * @param cutoff the lowest capitalized share of a guard word written mid-sentence: a word written
	 * capitalized less often passes the capitalization guard
	 * @param capitalizationLetsThrough how many words of the head of the rest pass the capitalization
	 * guard
	 * @param guardAtTheCutoff the guard words that set the cutoff, written capitalized least often
	 */
	public record Report(long pieces, int words, int head, long closedClassPieces, double closedClassShare,
			int headOfTheRest, List<WordPieces> top, int guardSetSize, double cutoff, int capitalizationLetsThrough,
			List<WordShare> guardAtTheCutoff) {
	}

}
