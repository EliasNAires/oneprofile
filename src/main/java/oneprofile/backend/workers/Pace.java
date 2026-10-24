package oneprofile.backend.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import oneprofile.backend.storage.company.RegionEnum;

/**
 * How fast a run asks one ATS: how many requests it keeps open at once, and how many it starts a
 * second at most, measured for each ATS as ADR-0025 says (docs/measurements/2026-10-pace.md).
 * <p>
 * A run that is refused anyway slows down: each refusal halves both, down to one at a time and one a
 * second, for the rest of the run. The next run starts from the pace again.
 *
 * @param requestsAtOnce how many requests are open at once at most
 * @param startsASecond how many requests start each second at most
 * @param perRegion whether each region gets the pace of its own, as on an ATS with one API per
 * region, rather than every company sharing it
 */
public record Pace(int requestsAtOnce, int startsASecond, boolean perRegion) {

	public Pace {
		if (requestsAtOnce < 1 || startsASecond < 1) {
			throw new IllegalArgumentException("A run has to ask at least one board at a time, once a second");
		}
	}

	/**
	 * Reads every company at this pace, and hands each one's reading on as soon as it is read. The
	 * companies of each region are started in slug order; readings are handed on in whatever
	 * order they finish, from several threads at once.
	 * @param <T> what a reading is
	 * @param run what is reading, named in the log when it is refused
	 * @param companies the slug and region of every company to read
	 * @param read reads one company, telling the run each time the ATS refuses it
	 * @param then what is done with each reading, on the thread that read it, before its slot is given
	 * to the next request, so that no more are written at once than are read
	 */
	public <T> void readEach(String run, SortedMap<String, RegionEnum> companies, Read<T> read,
			BiConsumer<String, T> then) {
		Queue<Future<?>> readings = new ConcurrentLinkedQueue<>();
		try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<?>> starters = new ArrayList<>();
			for (Map.Entry<String, SortedMap<String, RegionEnum>> region : regions(companies).entrySet()) {
				Throttle throttle = new Throttle(run + region.getKey(), this.requestsAtOnce, this.startsASecond);
				starters.add(threads.submit(() -> {
					for (Map.Entry<String, RegionEnum> company : region.getValue().entrySet()) {
						throttle.awaitStart();
						readings.add(threads.submit(() -> {
							try {
								then.accept(company.getKey(),
										read.read(company.getKey(), company.getValue(), throttle::refused));
							}
							finally {
								throttle.finished();
							}
						}));
					}
					return null;
				}));
			}
			starters.forEach(Pace::await);
			readings.forEach(Pace::await);
		}
	}

	/** The companies of each region, or all of them together unless the ATS has one API per region. */
	private Map<String, SortedMap<String, RegionEnum>> regions(SortedMap<String, RegionEnum> companies) {
		if (!this.perRegion) {
			return Map.of("", companies);
		}
		Map<String, SortedMap<String, RegionEnum>> regions = new TreeMap<>();
		companies.forEach((slug, region) -> regions.computeIfAbsent(", " + region, (key) -> new TreeMap<>())
			.put(slug, region));
		return regions;
	}

	private static void await(Future<?> task) {
		try {
			task.get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting on a run's requests", ex);
		}
		catch (ExecutionException ex) {
			if (ex.getCause() instanceof RuntimeException cause) {
				throw cause;
			}
			if (ex.getCause() instanceof Error cause) {
				throw cause;
			}
			throw new IllegalStateException(ex.getCause());
		}
	}

	/** Reads one company. */
	@FunctionalInterface
	public interface Read<T> {

		/**
		 * @param slug the company's slug
		 * @param region the region its company holds
		 * @param onRefusal to be run each time the ATS refuses a request, so the run slows down
		 * @return what was read
		 */
		T read(String slug, RegionEnum region, Runnable onRefusal);

	}

}
