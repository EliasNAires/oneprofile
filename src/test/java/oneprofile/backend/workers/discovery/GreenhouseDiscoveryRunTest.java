package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.company.CompanyStore;
import org.junit.jupiter.api.Test;

/**
 * The crawl the discovery is pointed at is a miniature of a real one: a {@code cluster.idx} of six
 * sampled keys over one shard of six blocks, one per Greenhouse host and one on either side.
 */
class GreenhouseDiscoveryRunTest {

	private static final String CRAWL = "CC-MAIN-2026-34";

	private static final String SHARD = "cdx-00199.gz";

	private final MiniatureCrawl crawl = new MiniatureCrawl();

	private final CompanyStore companies = mock(CompanyStore.class);

	private final GreenhouseDiscoveryRun discovery = new GreenhouseDiscoveryRun(new ClusterIndexParser(this.crawl),
			new CdxBlockParser(this.crawl), this.companies);

	@Test
	void recordsTheSlugOfEveryBoardCapturedOnAnyOfTheHostsAndReportsWhatItAdded() throws IOException {
		TreeSet<String> slugs = new TreeSet<>(List.of("acme", "decathlontechnology", "eikontherapeutics", "rightship"));
		given(this.companies.record(eq(AtsEnum.GREENHOUSE), eq(slugs))).willReturn(3);

		assertThat(this.discovery.discover(CRAWL)).isEqualTo(new GreenhouseDiscoveryRun.Report(CRAWL, 4, 3));
	}

	@Test
	void doesNotRecordAnythingWhenTheCrawlCouldNotBeRead() {
		GreenhouseDiscoveryRun discovery = new GreenhouseDiscoveryRun(new ClusterIndexParser(new UnreachableCrawl()),
				new CdxBlockParser(new UnreachableCrawl()), this.companies);

		assertThatIOException().isThrownBy(() -> discovery.discover(CRAWL));
		then(this.companies).shouldHaveNoInteractions();
	}

	@Test
	void readsEachBlockOnceEvenWhereSeveralHostsNameIt() throws IOException {
		this.discovery.discover(CRAWL);

		assertThat(this.crawl.blockReads()).hasSize(6).doesNotHaveDuplicates();
	}

	/**
	 * A crawl index of one shard, served as ranges the way {@code data.commoncrawl.org} serves it.
	 * Its {@code cluster.idx} samples one key before the Greenhouse hosts, one per host, and one
	 * after them, so that every host's search also reaches its neighbours' blocks.
	 */
	private static final class UnreachableCrawl implements RangeReaderPort {

		@Override
		public long size(String path) throws IOException {
			throw new IOException("data.commoncrawl.org answered 503");
		}

		@Override
		public byte[] read(String path, long offset, int length) throws IOException {
			throw new IOException("data.commoncrawl.org answered 503");
		}

	}

	private static final class MiniatureCrawl implements RangeReaderPort {

		private static final String CLUSTER_INDEX = "cc-index/collections/%s/indexes/cluster.idx".formatted(CRAWL);

		private static final String SHARD_PATH = "cc-index/collections/%s/indexes/%s".formatted(CRAWL, SHARD);

		private final Map<String, byte[]> files = new LinkedHashMap<>();

		private final List<String> blockReads = new ArrayList<>();

		private MiniatureCrawl() {
			ByteArrayOutputStream shard = new ByteArrayOutputStream();
			StringBuilder clusterIndex = new StringBuilder();
			appendBlock(shard, clusterIndex, "io,aaa)/", List.of(capture("https://aaa.io/")));
			appendBlock(shard, clusterIndex, "io,greenhouse,boards)/acme",
					List.of(capture("https://boards.greenhouse.io/acme/jobs/1"),
							capture("https://boards.greenhouse.io/Acme")));
			appendBlock(shard, clusterIndex, "io,greenhouse,eu,boards)/abbyy",
					List.of(capture("https://boards.eu.greenhouse.io/abbyy/jobs/4344118101")));
			appendBlock(shard, clusterIndex, "io,greenhouse,eu,job-boards)/decathlontechnology",
					List.of(capture("https://job-boards.eu.greenhouse.io/decathlontechnology/jobs/4917685101"),
							capture("https://job-boards.eu.greenhouse.io/rightship")));
			appendBlock(shard, clusterIndex, "io,greenhouse,job-boards)/eikontherapeutics",
					List.of(capture("https://job-boards.greenhouse.io/eikontherapeutics/jobs/5027565007"),
							capture("https://job-boards.greenhouse.io/embed/job_app?for=Acme&token=1")));
			appendBlock(shard, clusterIndex, "io,zzz)/", List.of(capture("https://zzz.io/")));
			this.files.put(CLUSTER_INDEX, clusterIndex.toString().getBytes(StandardCharsets.UTF_8));
			this.files.put(SHARD_PATH, shard.toByteArray());
		}

		/** Gzips one block onto the shard and samples its first key into {@code cluster.idx}. */
		private void appendBlock(ByteArrayOutputStream shard, StringBuilder clusterIndex, String key,
				List<String> captures) {
			int offset = shard.size();
			byte[] block = gzip(captures);
			shard.writeBytes(block);
			clusterIndex.append("%s 20260816194955\t%s\t%d\t%d\t1\n".formatted(key, SHARD, offset, block.length));
		}

		private static String capture(String url) {
			String subset = url.startsWith("https://boards.") ? "crawldiagnostics" : "warc";
			return "key 20260816194955 {\"url\": \"%s\", \"filename\": \"crawl-data/%s/segments/1/%s/x.warc.gz\"}"
				.formatted(url, CRAWL, subset);
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

		@Override
		public long size(String path) {
			return this.files.get(path).length;
		}

		@Override
		public byte[] read(String path, long offset, int length) {
			byte[] file = this.files.get(path);
			if (path.equals(SHARD_PATH)) {
				this.blockReads.add("%d+%d".formatted(offset, length));
			}
			int from = Math.toIntExact(Math.min(offset, file.length));
			int to = Math.toIntExact(Math.min(offset + (long) length, file.length));
			byte[] range = new byte[to - from];
			System.arraycopy(file, from, range, 0, to - from);
			return range;
		}

		List<String> blockReads() {
			return this.blockReads;
		}

	}

}
