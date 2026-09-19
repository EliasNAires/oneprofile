package oneprofile.backend.discovery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

/**
 * Reads byte ranges over anonymous HTTPS, by default from {@code https://data.commoncrawl.org/}.
 * <p>
 * Common Crawl names range requests as the traffic class hardest for them to serve, so this client
 * identifies itself and backs off when the service answers 429 or 503.
 */
public class HttpRangeReader implements RangeReader {

	private static final URI DATA_COMMONCRAWL = URI.create("https://data.commoncrawl.org/");

	private static final int ATTEMPTS = 5;

	private static final Duration BACK_OFF = Duration.ofSeconds(1);

	private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	private final URI base;

	private final String userAgent;

	private final int attempts;

	private final Duration backOff;

	/**
	 * Reads from Common Crawl.
	 * @param userAgent how this client identifies itself, which Common Crawl asks for
	 */
	public HttpRangeReader(String userAgent) {
		this(DATA_COMMONCRAWL, userAgent, ATTEMPTS, BACK_OFF);
	}

	/**
	 * @param base what paths are resolved against
	 * @param userAgent how this client identifies itself
	 * @param attempts how many times a request is made before it is given up on
	 * @param backOff how long to wait after the first refusal, doubling with each further one
	 */
	public HttpRangeReader(URI base, String userAgent, int attempts, Duration backOff) {
		if (attempts < 1) {
			throw new IllegalArgumentException("A request has to be attempted at least once");
		}
		this.base = base;
		this.userAgent = userAgent;
		this.attempts = attempts;
		this.backOff = backOff;
	}

	@Override
	public long size(String path) throws IOException {
		HttpResponse<Void> response = send(request(path).method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
				BodyHandlers.discarding());
		if (response.statusCode() != 200) {
			throw new IOException("Asked for the size of %s and got %d".formatted(path, response.statusCode()));
		}
		return response.headers()
			.firstValueAsLong("Content-Length")
			.orElseThrow(() -> new IOException("No Content-Length for " + path));
	}

	@Override
	public byte[] read(String path, long offset, int length) throws IOException {
		HttpRequest request = request(path).header("Range", "bytes=%d-%d".formatted(offset, offset + length - 1))
			.GET()
			.build();
		HttpResponse<byte[]> response = send(request, BodyHandlers.ofByteArray());
		if (response.statusCode() != 206) {
			// A 200 carries the whole file rather than the range, which would be read as if it were
			// the range that was asked for.
			throw new IOException("Asked for bytes %d-%d of %s and got %d".formatted(offset, offset + length - 1, path,
					response.statusCode()));
		}
		byte[] block = response.body();
		long expected = Math.min(length, sizeFrom(response, path) - offset);
		if (block.length != expected) {
			// A body shorter than the range is a truncated response, which would otherwise be read
			// as a complete one.
			throw new IOException("Asked for %d bytes at %d of %s and got %d".formatted(length, offset, path,
					block.length));
		}
		return block;
	}

	/** The total length of the file, as the {@code Content-Range} of a partial response states it. */
	private static long sizeFrom(HttpResponse<byte[]> response, String path) throws IOException {
		String contentRange = response.headers()
			.firstValue("Content-Range")
			.orElseThrow(() -> new IOException("No Content-Range on the partial response for " + path));
		try {
			return Long.parseLong(contentRange.substring(contentRange.indexOf('/') + 1));
		}
		catch (NumberFormatException ex) {
			throw new IOException("Unreadable Content-Range for %s: %s".formatted(path, contentRange), ex);
		}
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(this.base.resolve(path)).header("User-Agent", this.userAgent);
	}

	private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> body) throws IOException {
		IOException lastFailure = null;
		for (int attempt = 0; attempt < this.attempts; attempt++) {
			if (attempt > 0) {
				waitBefore(attempt);
			}
			try {
				HttpResponse<T> response = this.http.send(request, body);
				if (response.statusCode() != 429 && response.statusCode() != 503) {
					return response;
				}
				lastFailure = new IOException(
						"%s answered %d".formatted(request.uri(), response.statusCode()));
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while reading " + request.uri(), ex);
			}
		}
		throw lastFailure;
	}

	private void waitBefore(int attempt) throws IOException {
		try {
			Thread.sleep(this.backOff.multipliedBy(1L << (attempt - 1)));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while backing off", ex);
		}
	}

}
