package oneprofile.backend.workers.discovery;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.archive.url.WaybackURLKeyMaker;
import org.springframework.stereotype.Component;

/**
 * Finds the CDX blocks of a crawl that can hold the records of a URL prefix, by binary-searching
 * the crawl's {@code cluster.idx} with range requests.
 * <p>
 * {@code cluster.idx} samples only every 3000th CDX record, so a sampled key that carries the
 * prefix says nothing about where the run of matching records actually begins or ends. The block
 * before the first sampled match and the block after the last one are therefore always returned
 * with the matches: the records the search is after can be sitting in either of them.
 */
@Component
public class ClusterIndexParser {

	/**
	 * How much of the index to read per request. Lines run to about 120 bytes, so a chunk this size
	 * always holds a complete line, and a full-sized index is bisected in around fifteen requests.
	 */
	private static final int CHUNK = 4096;

	private final RangeReaderPort reader;

	private final WaybackURLKeyMaker keyMaker = new WaybackURLKeyMaker();

	public ClusterIndexParser(RangeReaderPort reader) {
		this.reader = reader;
	}

	/**
	 * Locates the blocks of one crawl that can hold records for a URL prefix.
	 * @param crawl the crawl to search, such as {@code CC-MAIN-2026-34}
	 * @param urlPrefix the URL prefix to look for, such as {@code https://boards.greenhouse.io/}
	 * @return the blocks to read, in index order, from the one preceding the first match to the one
	 * following the last
	 * @throws IOException if the index could not be read
	 * @throws IllegalArgumentException if the URL prefix cannot be canonicalized
	 */
	public List<CdxBlock> locate(String crawl, String urlPrefix) throws IOException {
		String prefix = surt(urlPrefix);
		String path = "cc-index/collections/%s/indexes/cluster.idx".formatted(crawl);
		long size = this.reader.size(path);
		return blocksFrom(path, size, lastLineStartBelow(path, size, prefix), prefix);
	}

	private String surt(String urlPrefix) {
		try {
			return this.keyMaker.makeKey(urlPrefix);
		}
		catch (URISyntaxException | RuntimeException ex) {
			// A URL the key maker cannot parse reaches us as anything from a URISyntaxException to
			// an index out of bounds, so every failure to canonicalize is reported the same way.
			throw new IllegalArgumentException("Not a URL prefix that can be canonicalized: " + urlPrefix, ex);
		}
	}

	/**
	 * Bisects the index down to the start of a line whose key sorts below the prefix and which is
	 * within one chunk of the first line whose key does not.
	 */
	private long lastLineStartBelow(String path, long size, String prefix) throws IOException {
		long low = 0;
		long high = size;
		while (high - low > CHUNK) {
			long middle = low + (high - low) / 2;
			Line line = lineAfter(path, middle, size);
			if (line == null || line.start() >= high) {
				high = middle;
			}
			else if (line.key().compareTo(prefix) < 0) {
				low = line.start();
			}
			else {
				high = line.start();
			}
		}
		return low;
	}

	/**
	 * Walks the index from a line start, collecting the block before the first match, every block
	 * whose key carries the prefix, and the block after the last one.
	 */
	private List<CdxBlock> blocksFrom(String path, long size, long start, String prefix) throws IOException {
		List<CdxBlock> blocks = new ArrayList<>();
		CdxBlock preceding = null;
		boolean matching = false;
		long position = start;
		while (position < size) {
			List<Line> lines = linesFrom(path, position, size);
			if (lines.isEmpty()) {
				break;
			}
			for (Line line : lines) {
				if (!matching && line.key().compareTo(prefix) < 0) {
					preceding = line.block();
					continue;
				}
				if (!matching && preceding != null) {
					blocks.add(preceding);
				}
				matching = true;
				blocks.add(line.block());
				if (!line.key().startsWith(prefix)) {
					return blocks;
				}
			}
			position = lines.getLast().end();
		}
		if (!matching && preceding != null) {
			blocks.add(preceding);
		}
		return blocks;
	}

	/** Reads the complete lines that start at {@code start}, which must be the start of a line. */
	private List<Line> linesFrom(String path, long start, long size) throws IOException {
		byte[] chunk = chunkWithNewline(path, start, size);
		String text = new String(chunk, StandardCharsets.UTF_8);
		boolean endOfIndex = start + chunk.length >= size;
		List<Line> lines = new ArrayList<>();
		long position = start;
		for (String candidate : text.split("\n", -1)) {
			long end = position + candidate.getBytes(StandardCharsets.UTF_8).length + 1;
			boolean complete = end <= start + chunk.length;
			if (!complete && !(endOfIndex && !candidate.isEmpty())) {
				break;
			}
			lines.add(parse(position, Math.min(end, size), candidate));
			position = end;
		}
		return lines;
	}

	/** Reads the first complete line that starts after {@code offset}, or null if there is none. */
	private Line lineAfter(String path, long offset, long size) throws IOException {
		byte[] chunk = chunkWithNewline(path, offset, size);
		String text = new String(chunk, StandardCharsets.UTF_8);
		int firstBreak = text.indexOf('\n');
		int secondBreak = text.indexOf('\n', firstBreak + 1);
		if (firstBreak < 0 || secondBreak < 0) {
			return null;
		}
		long start = offset + text.substring(0, firstBreak + 1).getBytes(StandardCharsets.UTF_8).length;
		String line = text.substring(firstBreak + 1, secondBreak);
		return parse(start, start + line.getBytes(StandardCharsets.UTF_8).length + 1, line);
	}

	/**
	 * Reads a chunk, growing the request until it holds a line break or reaches the end of the
	 * index, so that a line longer than a chunk is read rather than silently skipped.
	 */
	private byte[] chunkWithNewline(String path, long offset, long size) throws IOException {
		for (int length = CHUNK;; length *= 2) {
			byte[] chunk = this.reader.read(path, offset, length);
			if (new String(chunk, StandardCharsets.UTF_8).indexOf('\n') >= 0 || offset + length >= size) {
				return chunk;
			}
		}
	}

	private static Line parse(long start, long end, String line) throws IOException {
		String[] fields = line.split("\t");
		if (fields.length < 4) {
			throw new IOException("Malformed cluster.idx line at byte " + start + ": " + line);
		}
		String searchKey = fields[0];
		int timestamp = searchKey.lastIndexOf(' ');
		String key = (timestamp < 0) ? searchKey : searchKey.substring(0, timestamp);
		try {
			CdxBlock block = new CdxBlock(fields[1], Long.parseLong(fields[2]), Integer.parseInt(fields[3]));
			return new Line(start, end, key, block);
		}
		catch (NumberFormatException ex) {
			throw new IOException("Malformed cluster.idx line at byte " + start + ": " + line, ex);
		}
	}

	/** One line of {@code cluster.idx}: a sampled key and the block it was sampled from. */
	private record Line(long start, long end, String key, CdxBlock block) {
	}

}
