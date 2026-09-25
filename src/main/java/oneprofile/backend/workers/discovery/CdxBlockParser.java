package oneprofile.backend.workers.discovery;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads one CDX block out of a crawl's shard and returns what it captured.
 * <p>
 * A crawl index mixes three subsets: the pages themselves under {@code warc}, the robots.txt files
 * under {@code robotstxt}, and the responses that never became a page — redirects above all — under
 * {@code crawldiagnostics}. Only the {@code warc} subset is a capture of a page, so only its URLs
 * are returned.
 */
@Component
public class CdxBlockParser {

	private final RangeReaderPort reader;

	private final ObjectMapper json = JsonMapper.builder().build();

	public CdxBlockParser(RangeReaderPort reader) {
		this.reader = reader;
	}

	/**
	 * Reads a block and returns the URL of every page it captured, in index order.
	 * @param crawl the crawl the block belongs to, such as {@code CC-MAIN-2026-34}
	 * @param block the block to read
	 * @return the URLs captured in the {@code warc} subset
	 * @throws IOException if the block could not be read or does not hold CDX captures
	 */
	public List<String> capturedUrls(String crawl, CdxBlock block) throws IOException {
		String path = "cc-index/collections/%s/indexes/%s".formatted(crawl, block.shard());
		byte[] compressed = this.reader.read(path, block.offset(), block.length());
		List<String> urls = new ArrayList<>();
		// A block is one gzip member, but a range that was widened to cover several blocks arrives
		// as their members back to back, and only a concatenating reader sees past the first.
		try (BufferedReader lines = new BufferedReader(new InputStreamReader(
				new GzipCompressorInputStream(new ByteArrayInputStream(compressed), true), StandardCharsets.UTF_8))) {
			String line;
			while ((line = lines.readLine()) != null) {
				capturedUrl(line).ifPresent(urls::add);
			}
		}
		return urls;
	}

	/** The URL a CDX line captured, unless the line is not a capture of a page. */
	private Optional<String> capturedUrl(String line) throws IOException {
		// A line is its SURT key, the timestamp and then the capture itself, as JSON.
		int json = line.indexOf(' ', line.indexOf(' ') + 1);
		if (json < 0) {
			throw new IOException("Malformed CDX line: " + line);
		}
		JsonNode capture = parse(line.substring(json + 1), line);
		String filename = capture.path("filename").asString("");
		String url = capture.path("url").asString("");
		if (!filename.contains("/warc/") || url.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(url);
	}

	private JsonNode parse(String capture, String line) throws IOException {
		try {
			return this.json.readTree(capture);
		}
		catch (JacksonException ex) {
			throw new IOException("Malformed CDX line: " + line, ex);
		}
	}

}
