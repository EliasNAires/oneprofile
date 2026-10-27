package oneprofile.backend.storage.rolelist;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Wires the role list the application ships. The list itself knows nothing about Spring or about
 * where its files live, so a test hands it files of its own.
 */
@Configuration(proxyBeanMethods = false)
public class RoleListConfiguration {

	private static final String ROLES = "rolelist/roles.tsv";

	private static final String WORDS = "rolelist/words.tsv";

	private static final String TITLES = "rolelist/titles.tsv";

	@Bean
	RoleListStore roleList() {
		return shipped();
	}

	/**
	 * Reads the role list from the classpath, for the bean and for whatever reads it outside Spring.
	 * @return the role list the application ships
	 */
	public static RoleListStore shipped() {
		try (Reader roles = open(ROLES); Reader words = open(WORDS); Reader titles = open(TITLES)) {
			return RoleListStore.read(roles, words, titles);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Reader open(String file) throws IOException {
		return new InputStreamReader(new ClassPathResource(file).getInputStream(), StandardCharsets.UTF_8);
	}

}
