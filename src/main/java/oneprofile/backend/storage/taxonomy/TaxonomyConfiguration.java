package oneprofile.backend.storage.taxonomy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Wires the taxonomy the application ships. The taxonomy itself knows nothing about Spring or
 * about where its file lives, so a test hands it a file of its own.
 */
@Configuration(proxyBeanMethods = false)
public class TaxonomyConfiguration {

	private static final String FILE = "taxonomy/skills.tsv";

	@Bean
	TaxonomyStore taxonomy() {
		try (Reader tsv = new InputStreamReader(new ClassPathResource(FILE).getInputStream(),
				StandardCharsets.UTF_8)) {
			return TaxonomyStore.read(tsv);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
