package oneprofile.backend.workers.stoplist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentedVacancy;
import oneprofile.backend.storage.taxonomy.TaxonomyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
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
 * It asks Jev, out of context, whether each word of the head and of the guard set could name a
 * technology, and ledgers every answer, so a rerun asks only what the pinned model has not answered.
 * Jev's account is shared with every Jev script, so it stops between chunks of questions once their
 * ledgers together have cost the spend limit {@code scripts/jev.py} stops at.
 * <p>
 * It sets Jev's bar at the strictest hundredth from 0.80 up that no guard word reaches, and writes the
 * stoplist: the closed-class words, and the words of the head of the rest that pass both guards.
 * <p>
 * The decision record, Jev's recall labels and the ledgers are read from the repository, and the
 * stoplist written into it, so it runs from the repository's root.
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

	/** Where Jev's answers on whether a word could name a technology are ledgered. */
	private static final Path ANSWERS = Path.of("docs/measurements/stoplist-labels-jev.jsonl");

	/** The ledgers of the other Jev scripts, which share its account; jev.py's LEDGERS. */
	private static final List<Path> LEDGERS = List.of(Path.of("docs/measurements/body-labels-jev.jsonl"),
			Path.of("docs/measurements/skill-labels-jev.jsonl"), RECALL_LABELS,
			Path.of("docs/measurements/skill-key-labels-jev.jsonl"));

	private static final double DOLLARS_PER_INPUT_TOKEN = 0.042 / 1_000_000;

	private static final double SPEND_LIMIT = 4.00;

	/** Questions in flight at once, as in jev.py. */
	private static final int WORKERS = 8;

	/** How many questions are asked before the spend is checked again, as in jev.py. */
	private static final int CHUNK = 200;

	/** Where Jev's bar starts, raised a hundredth at a time until no guard word clears it. */
	private static final int FIRST_BAR = 80;

	/** Where the stoplist is written. */
	private static final Path STOPLIST = Path.of("src/main/resources/taxonomy/stopwords.tsv");

	private static final ObjectMapper JSON = JsonMapper.builder().build();

	private final NormalizedVacancyStore normalized;

	private final TaxonomyStore taxonomy;

	private final JevPort jev;

	private final Path decisions;

	private final Path recallLabels;

	private final Path answers;

	private final List<Path> ledgers;

	private final Path stoplist;

	private final double spendLimit;

	private final int batch;

	private final int chunk;

	/**
	 * Reads the corpus in batches of a thousand vacancies, and the repository's decision record and
	 * recall labels.
	 * @param normalized where the segments are read
	 * @param taxonomy the skills whose keys join the guard set
	 * @param jev who is asked whether a word could name a technology
	 */
	@Autowired
	public StoplistRun(NormalizedVacancyStore normalized, TaxonomyStore taxonomy, JevPort jev) {
		this(normalized, taxonomy, jev, DECISIONS, RECALL_LABELS, ANSWERS, LEDGERS, STOPLIST, SPEND_LIMIT, BATCH,
				CHUNK);
	}

	/**
	 * @param normalized where the segments are read
	 * @param taxonomy the skills whose keys join the guard set
	 * @param jev who is asked whether a word could name a technology
	 * @param decisions the decision record, whose keeps join the guard set
	 * @param recallLabels Jev's verdicts on pieces, whose keeps join the guard set
	 * @param answers the ledger Jev's answers are appended to and read back from
	 * @param ledgers the other ledgers of Jev's account, whose costs count toward the spend limit
	 * @param stoplist where the stoplist is written
	 * @param spendLimit the dollars every ledger together may cost before no more is asked
	 * @param batch how many vacancies are read at a time
	 * @param chunk how many questions are asked between two checks of the spend
	 */
	StoplistRun(NormalizedVacancyStore normalized, TaxonomyStore taxonomy, JevPort jev, Path decisions,
			Path recallLabels, Path answers, List<Path> ledgers, Path stoplist, double spendLimit, int batch,
			int chunk) {
		this.normalized = normalized;
		this.taxonomy = taxonomy;
		this.jev = jev;
		this.decisions = decisions;
		this.recallLabels = recallLabels;
		this.answers = answers;
		this.ledgers = ledgers;
		this.stoplist = stoplist;
		this.spendLimit = spendLimit;
		this.batch = batch;
		this.chunk = chunk;
	}

	/**
	 * Counts every word's piece occurrences over the corpus, and asks Jev about the head and the guard
	 * set.
	 * @return the counts, the head, what the closed class removes, the cutoff and what Jev cost
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
		Map<String, Double> nos = nos();
		Set<String> asked = new LinkedHashSet<>();
		rest.subList(0, headOfTheRest).forEach((word) -> asked.add(word.word()));
		asked.addAll(new TreeSet<>(guardSet));
		Asking asking = ask(asked, nos);
		double bar = bar(guardSet, nos);
		Set<String> head = rest.subList(0, headOfTheRest)
			.stream()
			.map(WordPieces::word)
			.collect(Collectors.toSet());
		List<Stoplisted> stoplisted = new ArrayList<>();
		List<HeadWord> blocked = new ArrayList<>();
		for (WordPieces word : ranked) {
			Double share = share(word.word(), counts).map(WordShare::share).orElse(null);
			Double no = nos.get(word.word());
			if (ClosedClassRule.closedClass(word.word())) {
				stoplisted.add(new Stoplisted(word.word(), word.pieces(), share, no, "closed-class"));
			}
			else if (head.contains(word.word()) && share != null && share < cutoff && no != null && no >= bar) {
				stoplisted.add(new Stoplisted(word.word(), word.pieces(), share, no, "head"));
			}
			else if (head.contains(word.word()) && blocked.size() < TOP) {
				blocked.add(new HeadWord(word.word(), word.pieces(), share, no));
			}
		}
		// A guard word Jev was not asked about could have cleared the bar, so the bar is only set once
		// Jev has answered them all.
		if (!asking.stopped()) {
			write(stoplisted);
		}
		long stoplistedPieces = stoplisted.stream().mapToLong(Stoplisted::pieces).sum();
		return new Report(pieces, ranked.size(), head(ranked, HEAD_SHARE * pieces), closedClassPieces,
				(pieces == 0) ? 0 : (double) closedClassPieces / pieces, headOfTheRest,
				ranked.subList(0, Math.min(TOP, ranked.size())), guardSet.size(), cutoff, capitalizationLetsThrough,
				guardShares.stream().filter((word) -> word.share() == cutoff).toList(), asked.size(), asking.calls(),
				asking.tokens() * DOLLARS_PER_INPUT_TOKEN, asking.spent(), asking.stopped(), bar, stoplisted.size(),
				(pieces == 0) ? 0 : (double) stoplistedPieces / pieces,
				stoplisted.stream().map(Stoplisted::word).filter(guardSet::contains).toList(), blocked);
	}

	/**
	 * Jev's bar: 0.80, raised a hundredth at a time until no guard word's probability of no reaches
	 * it.
	 */
	private static double bar(Set<String> guardSet, Map<String, Double> nos) {
		int hundredths = FIRST_BAR;
		while (true) {
			double bar = hundredths / 100.0;
			if (guardSet.stream().map(nos::get).noneMatch((no) -> no != null && no >= bar)) {
				return bar;
			}
			hundredths++;
		}
	}

	/**
	 * Writes the stoplist, one row per word, the most frequent first, with its capitalized share to
	 * four places and Jev's probability of no to two, as Jev gives it.
	 */
	private void write(List<Stoplisted> stoplisted) {
		List<String> rows = new ArrayList<>();
		rows.add("word\tpieces\tcapitalized_share\tjev_no\tlayer");
		for (Stoplisted word : stoplisted) {
			rows.add(String.join("\t", word.word(), String.valueOf(word.pieces()),
					decimals(word.share(), 4), decimals(word.no(), 2), word.layer()));
		}
		try {
			Files.write(this.stoplist, rows);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String decimals(Double value, int places) {
		return (value == null) ? "" : String.format(Locale.ROOT, "%." + places + "f", value);
	}

	/** Jev's probability of no for every word the pinned model has answered in the ledger. */
	private Map<String, Double> nos() {
		Map<String, Double> nos = new HashMap<>();
		if (Files.exists(this.answers)) {
			for (String line : lines(this.answers)) {
				if (!line.isBlank()) {
					JsonNode row = JSON.readTree(line);
					if (row.path("model").asString("").equals(JevPort.MODEL)) {
						nos.put(row.get("word").asString(), row.get("probabilities").get("no").asDouble());
					}
				}
			}
		}
		return nos;
	}

	/**
	 * Asks Jev about every word the pinned model has not answered in the ledger, a chunk at a time,
	 * appending each answer to the ledger and to Jev's probabilities of no as it arrives, until the
	 * spend limit is reached.
	 */
	private Asking ask(Set<String> words, Map<String, Double> nos) {
		long ledgered = 0;
		for (Path ledger : Stream.concat(Stream.of(this.answers), this.ledgers.stream()).toList()) {
			if (!Files.exists(ledger)) {
				continue;
			}
			for (String line : lines(ledger)) {
				if (!line.isBlank()) {
					JsonNode row = JSON.readTree(line);
					ledgered += row.required("input_tokens").asLong();
				}
			}
		}
		List<String> wanted = words.stream().filter((word) -> !nos.containsKey(word)).toList();
		int calls = 0;
		long tokens = 0;
		try (ExecutorService pool = Executors.newFixedThreadPool(WORKERS)) {
			for (int start = 0; start < wanted.size(); start += this.chunk) {
				if ((ledgered + tokens) * DOLLARS_PER_INPUT_TOKEN >= this.spendLimit) {
					return new Asking(calls, tokens, (ledgered + tokens) * DOLLARS_PER_INPUT_TOKEN, true);
				}
				List<String> chunked = wanted.subList(start, Math.min(start + this.chunk, wanted.size()));
				List<Future<JevAnswer>> asked = chunked.stream()
					.map((word) -> pool.submit(() -> ledger(word, this.jev.ask(word))))
					.toList();
				for (int i = 0; i < asked.size(); i++) {
					JevAnswer answer = answered(asked.get(i));
					nos.put(chunked.get(i), answer.no());
					tokens += answer.inputTokens();
					calls++;
				}
			}
		}
		return new Asking(calls, tokens, (ledgered + tokens) * DOLLARS_PER_INPUT_TOKEN, false);
	}

	private synchronized JevAnswer ledger(String word, JevAnswer answer) throws IOException {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("word", word);
		row.put("answer", answer.choice());
		Map<String, Double> probabilities = new LinkedHashMap<>();
		probabilities.put("no", answer.no());
		probabilities.put("yes", answer.yes());
		row.put("probabilities", probabilities);
		row.put("model", answer.model());
		row.put("input_tokens", answer.inputTokens());
		Files.writeString(this.answers, JSON.writeValueAsString(row) + "\n", StandardOpenOption.CREATE,
				StandardOpenOption.APPEND);
		return answer;
	}

	private static JevAnswer answered(Future<JevAnswer> answer) {
		try {
			return answer.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while asking Jev", ex);
		}
		catch (ExecutionException ex) {
			throw (ex.getCause() instanceof IOException io) ? new UncheckedIOException(io)
					: new IllegalStateException(ex.getCause());
		}
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
	 * What asking Jev made and cost in one run.
	 *
	 * @param calls the questions asked
	 * @param tokens the input tokens they cost
	 * @param spent what every ledger of Jev's account has cost, in dollars, once they were asked
	 * @param stopped whether the spend limit left words unasked
	 */
	private record Asking(int calls, long tokens, double spent, boolean stopped) {
	}

	/**
	 * A word on the stoplist, and the layer that put it there.
	 *
	 * @param word the word, lowercased
	 * @param pieces how many times it is a piece across the corpus
	 * @param share the share of its mid-sentence mentions written capitalized, unless it never is
	 * written mid-sentence
	 * @param no Jev's probability that it could name no technology, unless Jev was not asked
	 * @param layer {@code closed-class} or {@code head}
	 */
	private record Stoplisted(String word, long pieces, Double share, Double no, String layer) {
	}

	/**
	 * A word of the head of the rest that a guard keeps off the stoplist.
	 *
	 * @param word the word, lowercased
	 * @param pieces how many times it is a piece across the corpus
	 * @param share the share of its mid-sentence mentions written capitalized, unless it never is
	 * written mid-sentence
	 * @param no Jev's probability that it could name no technology, unless Jev has not answered
	 */
	public record HeadWord(String word, long pieces, Double share, Double no) {
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
	 * @param jevWords how many words of the head of the rest and of the guard set Jev is asked about
	 * @param jevCalls the questions this run asked, the others answered by an earlier run
	 * @param jevCost what they cost, in dollars
	 * @param jevSpent what Jev's account has cost across every ledger, in dollars
	 * @param jevStopped whether the spend limit left words unasked
	 * @param bar Jev's bar: the lowest hundredth from 0.80 up that no guard word's probability of no
	 * reaches. A word whose probability reaches it passes the Jev guard
	 * @param stoplisted how many words the stoplist holds, written unless the spend limit left words
	 * unasked
	 * @param coverage the share of every piece that a stoplisted word is
	 * @param guardCheck the stoplisted words that are in the guard set, which should be none
	 * @param blocked the fifty most frequent words of the head of the rest a guard keeps off the stoplist
	 */
	public record Report(long pieces, int words, int head, long closedClassPieces, double closedClassShare,
			int headOfTheRest, List<WordPieces> top, int guardSetSize, double cutoff, int capitalizationLetsThrough,
			List<WordShare> guardAtTheCutoff, int jevWords, int jevCalls, double jevCost, double jevSpent,
			boolean jevStopped, double bar, int stoplisted, double coverage, List<String> guardCheck,
			List<HeadWord> blocked) {
	}

}
