import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.BackendApplication;
import oneprofile.backend.storage.normalizedvacancy.GroupVacancy;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.workers.grouping.DescriptionSimilarityRule;
import oneprofile.backend.workers.grouping.VacancyGroupRun;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Writes every candidate pair of the vacancy-group pass (#27) with the threshold at which the pass would put
 * the two in one group, read from the development database with the application's own store and rules.
 *
 *     java -cp CLASSPATH scripts/VacancyGroupPairs.java PAIRS.tsv
 *
 * A candidate pair is two vacancies of one company (one slug across ATSs) with the same normalized titles,
 * as #27 defines it. The pass compares a narrower bucket, which also needs the same management level and
 * title seniorities (VacancyGroupRun.bucket); a pair outside one is never merged.
 *
 * Writes one line a pair, as slug, first and second vacancy id, their ATSs, whether they share the pass's
 * bucket (1 or 0), the merge level and the overlap, tab-separated. The merge level is the highest threshold
 * at which single linkage joins the two, through other vacancies of the bucket or directly: the pass merges
 * them at threshold t exactly when the level is at least t; it is 0 outside a bucket. The overlap is their
 * own descriptions' shingle overlap, which decides whether one can hide the other as a duplicate. Both strip
 * the locations the pass strips, those of the bucket, or of the candidate pairs' titles outside one.
 */
class VacancyGroupPairs {

	/** Titles are read for about this many vacancies at once. */
	private static final int CHUNK = 1000;

	record Frame(String slug, List<String> titles) {
	}

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
			.web(WebApplicationType.NONE)
			.properties("spring.main.banner-mode=off", "logging.level.root=WARN",
					"spring.docker.compose.lifecycle-management=start-only")
			.run();
		NormalizedVacancyStore store = context.getBean(NormalizedVacancyStore.class);
		DescriptionSimilarityRule similarity = context.getBean(DescriptionSimilarityRule.class);
		Map<Frame, List<GroupVacancy>> frames = new LinkedHashMap<>();
		for (GroupVacancy vacancy : store.vacancyGroupScope()) {
			VacancyGroupRun.Bucket bucket = VacancyGroupRun.bucket(vacancy);
			frames.computeIfAbsent(new Frame(bucket.slug(), bucket.titles()), (key) -> new ArrayList<>()).add(vacancy);
		}
		long pairs = 0;
		try (PrintStream out = new PrintStream(Files.newOutputStream(Path.of(args[0])), false, StandardCharsets.UTF_8)) {
			List<List<GroupVacancy>> pending = new ArrayList<>();
			int size = 0;
			for (List<GroupVacancy> frame : frames.values()) {
				if (frame.size() < 2) {
					continue;
				}
				pending.add(frame);
				size += frame.size();
				if (size >= CHUNK) {
					pairs += write(pending, store, similarity, out);
					pending.clear();
					size = 0;
				}
			}
			pairs += write(pending, store, similarity, out);
		}
		System.err.println(pairs + " pairs");
		context.close();
	}

	/** Reads the frames' descriptions together and writes each frame's pairs. */
	private static long write(List<List<GroupVacancy>> frames, NormalizedVacancyStore store,
			DescriptionSimilarityRule similarity, PrintStream out) {
		if (frames.isEmpty()) {
			return 0;
		}
		Map<Long, List<Segment>> segments = store
			.segmentsOf(frames.stream().flatMap(List::stream).map(GroupVacancy::vacancyId).toList());
		long pairs = 0;
		for (List<GroupVacancy> frame : frames) {
			pairs += write(frame, segments, similarity, out);
		}
		return pairs;
	}

	/** Writes one frame's pairs: a bucket's with their merge levels, the rest at level 0. */
	private static long write(List<GroupVacancy> frame, Map<Long, List<Segment>> segments,
			DescriptionSimilarityRule similarity, PrintStream out) {
		Map<VacancyGroupRun.Bucket, List<Integer>> buckets = new LinkedHashMap<>();
		for (int i = 0; i < frame.size(); i++) {
			buckets.computeIfAbsent(VacancyGroupRun.bucket(frame.get(i)), (key) -> new ArrayList<>()).add(i);
		}
		int n = frame.size();
		int[] bucketOf = new int[n];
		long[][] shingles = new long[n][];
		double[][] level = new double[n][n];
		int b = 0;
		for (List<Integer> bucket : buckets.values()) {
			List<String> locations = bucket.stream().flatMap((i) -> frame.get(i).locations().stream()).distinct().toList();
			for (int i : bucket) {
				bucketOf[i] = b;
				shingles[i] = similarity.shingles(segments.getOrDefault(frame.get(i).vacancyId(), List.of()), locations);
			}
			levels(bucket, shingles, similarity, level);
			b++;
		}
		long[][] frameShingles = null;
		if (buckets.size() > 1) {
			List<String> locations = frame.stream().flatMap((vacancy) -> vacancy.locations().stream()).distinct().toList();
			frameShingles = new long[n][];
			for (int i = 0; i < n; i++) {
				frameShingles[i] = similarity.shingles(segments.getOrDefault(frame.get(i).vacancyId(), List.of()), locations);
			}
		}
		long pairs = 0;
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				boolean same = bucketOf[i] == bucketOf[j];
				double overlap = same ? similarity.overlap(shingles[i], shingles[j])
						: similarity.overlap(frameShingles[i], frameShingles[j]);
				GroupVacancy first = frame.get(i);
				GroupVacancy second = frame.get(j);
				out.println(String.join("\t", first.slug(), Long.toString(first.vacancyId()),
						Long.toString(second.vacancyId()), first.ats().name(), second.ats().name(), same ? "1" : "0",
						Double.toString(same ? level[i][j] : 0), Double.toString(overlap)));
				pairs++;
			}
		}
		return pairs;
	}

	/**
	 * Fills the merge level of every pair of one bucket: joining its vacancies by overlap, highest first, the
	 * edge that first joins two vacancies' components is the level at which single linkage merges them.
	 */
	private static void levels(List<Integer> bucket, long[][] shingles, DescriptionSimilarityRule similarity,
			double[][] level) {
		record Edge(int a, int b, double overlap) {
		}
		List<Edge> edges = new ArrayList<>();
		for (int x = 0; x < bucket.size(); x++) {
			for (int y = x + 1; y < bucket.size(); y++) {
				double overlap = similarity.overlap(shingles[bucket.get(x)], shingles[bucket.get(y)]);
				if (overlap > 0) {
					edges.add(new Edge(bucket.get(x), bucket.get(y), overlap));
				}
			}
		}
		edges.sort(Comparator.comparingDouble(Edge::overlap).reversed());
		Map<Integer, List<Integer>> components = new LinkedHashMap<>();
		Map<Integer, Integer> componentOf = new LinkedHashMap<>();
		for (int i : bucket) {
			components.put(i, new ArrayList<>(List.of(i)));
			componentOf.put(i, i);
		}
		for (Edge edge : edges) {
			int a = componentOf.get(edge.a());
			int c = componentOf.get(edge.b());
			if (a == c) {
				continue;
			}
			List<Integer> left = components.get(a);
			List<Integer> right = components.remove(c);
			for (int x : left) {
				for (int y : right) {
					level[x][y] = edge.overlap();
					level[y][x] = edge.overlap();
				}
			}
			for (int y : right) {
				componentOf.put(y, a);
			}
			left.addAll(right);
		}
	}

}
