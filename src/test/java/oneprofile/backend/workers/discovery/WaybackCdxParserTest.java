package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Chunks shaped as the CDX server answered them on 2026-10-21. */
class WaybackCdxParserTest {

	private static final String PREFIX = "boards.greenhouse.io/";

	private final List<String> urls = new ArrayList<>();

	@Test
	void readsEveryCaptureUnderThePrefixFollowingResumeKeysUntilNoneRemains() throws IOException {
		Map<String, String> chunks = Map.of("", """
				[["original"],
				["https://boards.greenhouse.io/acme"],
				["https://boards.greenhouse.io/acme/jobs/1"],
				[],
				["eJxFzEsK"]]
				""", "eJxFzEsK", """
				[["original"],
				["https://boards.greenhouse.io/zeta"],
				[],
				["eJxLKkzN"]]
				""", "eJxLKkzN", """
				[["https://boards.greenhouse.io/zzz"]]
				""");

		new WaybackCdxParser(cdxOf(chunks)).capturedUrls(PREFIX, this.urls::add);

		assertThat(this.urls).containsExactly("https://boards.greenhouse.io/acme",
				"https://boards.greenhouse.io/acme/jobs/1", "https://boards.greenhouse.io/zeta",
				"https://boards.greenhouse.io/zzz");
	}

	@Test
	void followsAKeyFromAChunkThatHoldsNoCaptures() throws IOException {
		Map<String, String> chunks = Map.of("", """
				[["original"],
				[],
				["eJxFzEsK"]]
				""", "eJxFzEsK", """
				[["original"],
				["https://boards.greenhouse.io/acme"]]
				""");

		assertThat(new WaybackCdxParser(cdxOf(chunks)).capturedUrls(PREFIX, this.urls::add)).isEqualTo(1);
		assertThat(this.urls).containsExactly("https://boards.greenhouse.io/acme");
	}

	@Test
	void refusesAChunkThatClosesWithTheKeyItWasAskedWith() {
		WaybackCdxParser parser = new WaybackCdxParser(cdxOf(Map.of("", """
				[["original"],
				[],
				["eJxFzEsK"]]
				""", "eJxFzEsK", """
				[["original"],
				[],
				["eJxFzEsK"]]
				""")));

		assertThatIOException().isThrownBy(() -> parser.capturedUrls(PREFIX, this.urls::add));
	}

	@Test
	void refusesAChunkWithRowsAfterItsResumeKey() {
		WaybackCdxParser parser = new WaybackCdxParser(cdxOf(Map.of("", """
				[["original"],
				[],
				["eJxFzEsK"],
				["https://boards.greenhouse.io/acme"]]
				""")));

		assertThatIOException().isThrownBy(() -> parser.capturedUrls(PREFIX, this.urls::add));
	}

	@Test
	void readsNothingWhereNothingWasCaptured() throws IOException {
		new WaybackCdxParser(cdxOf(Map.of("", "[]\n"))).capturedUrls(PREFIX, this.urls::add);
		new WaybackCdxParser(cdxOf(Map.of("", ""))).capturedUrls(PREFIX, this.urls::add);

		assertThat(this.urls).isEmpty();
	}

	@Test
	void refusesARowThatIsNotACapturedUrl() {
		WaybackCdxParser parser = new WaybackCdxParser(cdxOf(Map.of("", """
				[["original"],
				["https://boards.greenhouse.io/acme", "200"]]
				""")));

		assertThatIOException().isThrownBy(() -> parser.capturedUrls(PREFIX, this.urls::add));
	}

	/** An index that answers each resume key, the first chunk under the empty one, with its chunk. */
	private static WaybackCdxPort cdxOf(Map<String, String> chunks) {
		return (urlPrefix, resumeKey) -> {
			assertThat(urlPrefix).isEqualTo(PREFIX);
			return Optional.ofNullable(chunks.get(resumeKey.orElse("")))
				.orElseThrow(() -> new IOException("No chunk for " + resumeKey));
		};
	}

}
