package oneprofile.backend.discovery;

import java.io.IOException;

/**
 * Reads byte ranges out of a remote file. The path is relative to whatever the implementation reads
 * from, such as {@code cc-index/collections/CC-MAIN-2026-34/indexes/cluster.idx}.
 */
public interface RangeReader {

	/**
	 * The size of the whole file in bytes.
	 * @param path the file to measure
	 * @return its length in bytes
	 * @throws IOException if the file could not be read
	 */
	long size(String path) throws IOException;

	/**
	 * Reads up to {@code length} bytes starting at {@code offset}. Fewer bytes come back when the
	 * range runs past the end of the file.
	 * @param path the file to read from
	 * @param offset the byte offset to start at
	 * @param length how many bytes to ask for
	 * @return the bytes read
	 * @throws IOException if the range could not be read
	 */
	byte[] read(String path, long offset, int length) throws IOException;

}
