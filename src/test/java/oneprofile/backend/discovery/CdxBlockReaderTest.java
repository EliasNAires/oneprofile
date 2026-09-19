package oneprofile.backend.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/**
 * The captures the reader is given are verbatim CDX lines from CC-MAIN-2026-34, gzipped the way a
 * shard holds them, so the reader is exercised against the real format without the live service.
 */
class CdxBlockReaderTest {

	private static final String CRAWL = "CC-MAIN-2026-34";

	private static final List<String> CAPTURES = recordedCaptures();

	private static final String EMBEDDED = "https://job-boards.greenhouse.io/embed/job_app?for=1stdibscom"
			+ "&token=8049439&utm_id=13791864&utm_source=DataAnalystJobs.io";

	private static final String EU = "https://job-boards.eu.greenhouse.io/abbyy/jobs/4823909101";

	private static final String JOBS = "https://job-boards.greenhouse.io/eikontherapeutics/jobs/5027565007";

	private static final String TAGGED_JOBS = "https://job-boards.greenhouse.io/eikontherapeutics/jobs/5085374007"
			+ "?utm_source=General+Catalyst+job+board&utm_medium=getro.com&gh_src=General+Catalyst+job+board";

	@Test
	void rangeReadsTheShardOfTheNamedCrawlAtTheBlock() throws IOException {
		ShardRangeReader reader = new ShardRangeReader(gzip(CAPTURES));

		new CdxBlockReader(reader).capturedUrls(CRAWL, new CdxBlock("cdx-00199.gz", 0, reader.shardLength()));

		assertThat(reader.reads())
			.containsExactly("cc-index/collections/CC-MAIN-2026-34/indexes/cdx-00199.gz@0+" + reader.shardLength());
	}

	@Test
	void returnsTheUrlOfEveryWarcCaptureInTheBlockAndOfNothingElse() throws IOException {
		List<String> urls = readAll(gzip(CAPTURES));

		// The other three captures are a robots.txt and two redirects, which the crawl filed under
		// robotstxt and crawldiagnostics.
		assertThat(urls).containsExactly(EU, JOBS, TAGGED_JOBS, EMBEDDED);
	}

	@Test
	void readsABlockWhoseRangeSpansSeveralGzipMembers() throws IOException {
		byte[] members = concatenate(gzip(CAPTURES.subList(0, 3)), gzip(CAPTURES.subList(3, CAPTURES.size())));

		assertThat(readAll(members)).isEqualTo(readAll(gzip(CAPTURES)));
	}

	@Test
	void rejectsALineThatIsNotACdxCapture() {
		byte[] block = gzip(List.of("io,greenhouse,job-boards)/acme 20260816194955 not-json"));

		assertThatThrownBy(() -> readAll(block)).isInstanceOf(IOException.class);
	}

	private static List<String> readAll(byte[] shard) throws IOException {
		ShardRangeReader reader = new ShardRangeReader(shard);
		return new CdxBlockReader(reader).capturedUrls(CRAWL, new CdxBlock("cdx-00199.gz", 0, shard.length));
	}

	private static List<String> recordedCaptures() {
		try (InputStream fixture = CdxBlockReaderTest.class
			.getResourceAsStream("/commoncrawl/cdx-greenhouse-captures.txt")) {
			return List.of(new String(fixture.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static byte[] gzip(List<String> captures) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
			gzip.write(String.join("\n", captures).concat("\n").getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return bytes.toByteArray();
	}

	private static byte[] concatenate(byte[] first, byte[] second) {
		byte[] both = new byte[first.length + second.length];
		System.arraycopy(first, 0, both, 0, first.length);
		System.arraycopy(second, 0, both, first.length, second.length);
		return both;
	}

	/** Serves one shard and records what was asked of it. */
	private static final class ShardRangeReader implements RangeReader {

		private final byte[] shard;

		private final List<String> reads = new ArrayList<>();

		private ShardRangeReader(byte[] shard) {
			this.shard = shard;
		}

		@Override
		public long size(String path) {
			return this.shard.length;
		}

		@Override
		public byte[] read(String path, long offset, int length) {
			this.reads.add("%s@%d+%d".formatted(path, offset, length));
			int from = Math.toIntExact(Math.min(offset, this.shard.length));
			int to = Math.toIntExact(Math.min(offset + (long) length, this.shard.length));
			byte[] range = new byte[to - from];
			System.arraycopy(this.shard, from, range, 0, to - from);
			return range;
		}

		int shardLength() {
			return this.shard.length;
		}

		List<String> reads() {
			return this.reads;
		}

	}

}
