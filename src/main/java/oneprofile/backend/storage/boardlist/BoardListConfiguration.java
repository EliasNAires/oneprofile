package oneprofile.backend.storage.boardlist;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/** Wires the board list the application ships, leaving the list itself independent of Spring. */
@Configuration(proxyBeanMethods = false)
public class BoardListConfiguration {

	/** The hand-kept file on the classpath. */
	private static final String UNREAD = "boards/unread.tsv";

	/** The list used by probes. */
	@Bean
	BoardListStore boardList() {
		return shipped();
	}

	/**
	 * Reads the board list from the classpath, for the bean and for callers outside Spring.
	 * @return the board list the application ships
	 */
	public static BoardListStore shipped() {
		try (Reader unread = new InputStreamReader(new ClassPathResource(UNREAD).getInputStream(),
				StandardCharsets.UTF_8)) {
			return BoardListStore.read(unread);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
