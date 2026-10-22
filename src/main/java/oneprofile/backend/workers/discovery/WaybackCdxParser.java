package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads every capture the Wayback Machine holds under one URL prefix, chunk after chunk, and hands
 * on the URL of each.
 * <p>
 * A chunk is a JSON array of one-field rows, {@code ["original"]} first as its header. While more
 * remains, it closes with an empty row and a row holding the resume key that asks for the next
 * chunk; the last chunk closes without them. A Greenhouse read is millions of captures, so the URLs
 * are handed on as they are read rather than collected.
 */
@Component
public class WaybackCdxParser {

	private static final Log logger = LogFactory.getLog(WaybackCdxParser.class);

	private static final String HEADER = "original";

	private final WaybackCdxPort cdx;

	private final ObjectMapper json = JsonMapper.builder().build();

	public WaybackCdxParser(WaybackCdxPort cdx) {
		this.cdx = cdx;
	}

	/**
	 * Reads every capture under a prefix.
	 * @param urlPrefix the URL prefix without its scheme, such as {@code boards.greenhouse.io/}
	 * @param url what each captured URL is handed to, in index order
	 * @return how many captures were read
	 * @throws IOException if a chunk could not be read or is not shaped as a chunk
	 */
	public long capturedUrls(String urlPrefix, Consumer<String> url) throws IOException {
		long captures = 0;
		Optional<String> resumeKey = Optional.empty();
		do {
			Optional<String> askedWith = resumeKey;
			JsonNode rows = parse(this.cdx.chunk(urlPrefix, askedWith), urlPrefix);
			resumeKey = Optional.empty();
			for (int i = 0; i < rows.size(); i++) {
				JsonNode row = rows.get(i);
				if (row.isArray() && row.isEmpty()) {
					if (i != rows.size() - 2) {
						throw new IOException("A Wayback chunk under %s does not close with its resume key"
							.formatted(urlPrefix));
					}
					resumeKey = Optional.of(onlyField(rows.get(i + 1), urlPrefix));
					if (resumeKey.equals(askedWith)) {
						// Asked again, the same key would be answered the same way, without end.
						throw new IOException("A Wayback chunk under %s closes with the key it was asked with: %s"
							.formatted(urlPrefix, askedWith.get()));
					}
					break;
				}
				String field = onlyField(row, urlPrefix);
				if (i == 0 && HEADER.equals(field)) {
					continue;
				}
				url.accept(field);
				captures++;
			}
			logger.info("Read %d Wayback captures under %s%s".formatted(captures, urlPrefix,
					resumeKey.isPresent() ? ", more remain" : ", all of them"));
		}
		while (resumeKey.isPresent());
		return captures;
	}

	/** A chunk as an array of rows, an empty body being a chunk with none. */
	private JsonNode parse(String chunk, String urlPrefix) throws IOException {
		try {
			JsonNode rows = this.json.readTree(chunk.isBlank() ? "[]" : chunk);
			if (!rows.isArray()) {
				throw new IOException("A Wayback chunk under %s is not an array".formatted(urlPrefix));
			}
			return rows;
		}
		catch (JacksonException ex) {
			throw new IOException("Unreadable Wayback chunk under " + urlPrefix, ex);
		}
	}

	/** The one string a row holds: a captured URL, the header, or a resume key. */
	private static String onlyField(JsonNode row, String urlPrefix) throws IOException {
		if (row == null || !row.isArray() || row.size() != 1 || !row.get(0).isString()) {
			throw new IOException("Not a row of one field in a Wayback chunk under %s: %s".formatted(urlPrefix, row));
		}
		return row.get(0).asString();
	}

}
