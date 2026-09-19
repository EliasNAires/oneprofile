package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The index the reader is given is a verbatim slice of {@code cluster.idx} from CC-MAIN-2026-34,
 * so the reader is exercised against the real format and the real ordering, never the live service.
 */
class ClusterIndexReaderTest {

	private static final String CRAWL = "CC-MAIN-2026-34";

	private static final String GREENHOUSE = "https://boards.greenhouse.io/";

	private final RecordingRangeReader reader = new RecordingRangeReader(new RecordedIndex());

	private final ClusterIndexReader clusterIndex = new ClusterIndexReader(this.reader);

	@Test
	void readsTheClusterIndexOfTheNamedCrawl() throws IOException {
		this.clusterIndex.locate(CRAWL, GREENHOUSE);

		assertThat(this.reader.paths())
				.containsOnly("cc-index/collections/CC-MAIN-2026-34/indexes/cluster.idx");
	}

	@Test
	void returnsEveryBlockWhoseKeyCarriesThePrefix() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, GREENHOUSE);

		assertThat(blocks).contains(new CdxBlock("cdx-00199.gz", 389540715, 196281),
				new CdxBlock("cdx-00199.gz", 389736996, 207125),
				new CdxBlock("cdx-00199.gz", 389944121, 211950),
				new CdxBlock("cdx-00199.gz", 390156071, 208118),
				new CdxBlock("cdx-00199.gz", 390364189, 207351));
	}

	@Test
	void startsOneBlockBeforeTheFirstMatchAndEndsOneBlockPastTheLast() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, GREENHOUSE);

		assertThat(blocks).hasSize(7);
		assertThat(blocks.getFirst()).isEqualTo(new CdxBlock("cdx-00199.gz", 389267940, 272775));
		assertThat(blocks.getLast()).isEqualTo(new CdxBlock("cdx-00199.gz", 390571540, 227048));
	}

	@Test
	void canonicalizesTheUrlIntoASurtPrefix() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, "HTTPS://BOARDS.Greenhouse.io");

		assertThat(blocks).isEqualTo(this.clusterIndex.locate(CRAWL, GREENHOUSE));
	}

	@Test
	void keepsHostsApartThatShareASuffix() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, "https://job-boards.eu.greenhouse.io/");

		assertThat(blocks).containsExactly(new CdxBlock("cdx-00199.gz", 390364189, 207351),
				new CdxBlock("cdx-00199.gz", 390571540, 227048),
				new CdxBlock("cdx-00199.gz", 390798588, 231185),
				new CdxBlock("cdx-00199.gz", 391029773, 246298));
	}

	@Test
	void returnsTheStraddlingBlocksWhenNoSampledKeyCarriesThePrefix() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, "https://gpu.greenhouse.io/");

		assertThat(blocks).containsExactly(new CdxBlock("cdx-00199.gz", 390798588, 231185),
				new CdxBlock("cdx-00199.gz", 391029773, 246298));
	}

	@Test
	void hasNoPrecedingBlockWhenThePrefixSortsBeforeTheWholeIndex() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, "https://aardvark.io/");

		assertThat(blocks).containsExactly(new CdxBlock("cdx-00199.gz", 383503847, 275885));
	}

	@Test
	void hasNoFollowingBlockWhenThePrefixSortsPastTheWholeIndex() throws IOException {
		List<CdxBlock> blocks = this.clusterIndex.locate(CRAWL, "https://zzz.io/");

		assertThat(blocks).containsExactly(new CdxBlock("cdx-00199.gz", 435696828, 292900));
	}

	@Test
	void rejectsAUrlItCannotCanonicalize() {
		assertThatThrownBy(() -> this.clusterIndex.locate(CRAWL, "http://"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void searchesAFullSizedIndexWithinTheMeasuredBudget() throws IOException {
		RecordingRangeReader fullSized = new RecordingRangeReader(new PaddedIndex());

		List<CdxBlock> blocks = new ClusterIndexReader(fullSized).locate(CRAWL, GREENHOUSE);

		assertThat(blocks).hasSize(7);
		assertThat(fullSized.requests()).isLessThanOrEqualTo(17);
		assertThat(fullSized.bytesRead()).isLessThanOrEqualTo(72 * 1024);
	}

	/**
	 * An index the reader can be pointed at, standing in for one key under
	 * {@code https://data.commoncrawl.org/}.
	 */
	private interface IndexFile {

		long size();

		byte[] slice(long offset, int length);

	}

	/** The recorded slice of a real {@code cluster.idx}, served as if it were the whole file. */
	private static final class RecordedIndex implements IndexFile {

		private final byte[] bytes = read();

		private static byte[] read() {
			try (InputStream fixture = ClusterIndexReaderTest.class
					.getResourceAsStream("/commoncrawl/cluster-idx-greenhouse-region.txt")) {
				return fixture.readAllBytes();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		public long size() {
			return this.bytes.length;
		}

		@Override
		public byte[] slice(long offset, int length) {
			int from = Math.toIntExact(Math.min(offset, this.bytes.length));
			int to = Math.toIntExact(Math.min(offset + (long) length, this.bytes.length));
			byte[] range = new byte[to - from];
			System.arraycopy(this.bytes, from, range, 0, to - from);
			return range;
		}

	}

	/**
	 * The recorded slice padded with sorted filler keys to the size of a real {@code cluster.idx} —
	 * 103,401,192 bytes for CC-MAIN-2026-34 — so that the number of range requests a search costs
	 * can be held to the budget the crawl was measured at. The filler is generated on read rather
	 * than materialized, since only the bytes the reader actually asks for are ever looked at.
	 */
	private static final class PaddedIndex implements IndexFile {

		private static final byte[] FILLER_LINE = ("io,%07d)/a/padded/path/of/the/length/these/keys/run/to"
				+ " 20260816194955\tcdx-99999.gz\t1\t1\t1\n").formatted(0).getBytes(StandardCharsets.UTF_8);

		private static final int LEADING_LINES = 561_838;

		private static final int TRAILING_LINES = 561_838;

		private final RecordedIndex recorded = new RecordedIndex();

		private final long recordedStart = (long) LEADING_LINES * FILLER_LINE.length;

		@Override
		public long size() {
			return this.recordedStart + this.recorded.size() + (long) TRAILING_LINES * FILLER_LINE.length;
		}

		@Override
		public byte[] slice(long offset, int length) {
			int size = Math.toIntExact(Math.min(length, Math.max(0, size() - offset)));
			byte[] range = new byte[size];
			for (int i = 0; i < size; i++) {
				range[i] = byteAt(offset + i);
			}
			return range;
		}

		private byte byteAt(long position) {
			long recordedEnd = this.recordedStart + this.recorded.size();
			if (position >= this.recordedStart && position < recordedEnd) {
				return this.recorded.slice(position - this.recordedStart, 1)[0];
			}
			long inFiller = (position < this.recordedStart) ? position : position - recordedEnd;
			long line = inFiller / FILLER_LINE.length;
			int column = Math.toIntExact(inFiller % FILLER_LINE.length);
			byte[] key = ("io,%s%07d)".formatted((position < this.recordedStart) ? "a" : "z", line))
					.getBytes(StandardCharsets.UTF_8);
			return (column < key.length) ? key[column] : FILLER_LINE[column];
		}

	}

	/**
	 * Serves ranges out of an index and records what was asked for, so that a test can hold the
	 * reader to a request budget.
	 */
	private static final class RecordingRangeReader implements RangeReader {

		private final IndexFile index;

		private final List<String> paths = new ArrayList<>();

		private int requests;

		private long bytesRead;

		private RecordingRangeReader(IndexFile index) {
			this.index = index;
		}

		@Override
		public long size(String path) {
			this.paths.add(path);
			this.requests++;
			return this.index.size();
		}

		@Override
		public byte[] read(String path, long offset, int length) {
			this.paths.add(path);
			this.requests++;
			byte[] range = this.index.slice(offset, length);
			this.bytesRead += range.length;
			return range;
		}

		List<String> paths() {
			return this.paths;
		}

		int requests() {
			return this.requests;
		}

		long bytesRead() {
			return this.bytesRead;
		}

	}

}
