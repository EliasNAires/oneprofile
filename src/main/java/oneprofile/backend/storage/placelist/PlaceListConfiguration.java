package oneprofile.backend.storage.placelist;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Wires the place list the application ships. The list itself knows nothing about Spring or about where
 * its files live, so a test hands it files of its own.
 */
@Configuration(proxyBeanMethods = false)
public class PlaceListConfiguration {

	private static final String COUNTRIES = "places/countries.tsv";

	private static final String SUBDIVISIONS = "places/subdivisions.tsv";

	private static final String CITIES = "places/cities.tsv";

	private static final String HAND = "places/hand.tsv";

	private static final String TIME_ZONES = "places/timezones.tsv";

	private static final String REGIONS = "places/regions.tsv";

	@Bean
	PlaceListStore placeList() {
		return shipped();
	}

	/**
	 * Reads the place list from the classpath, for the bean and for whatever reads it outside Spring.
	 * @return the place list the application ships
	 */
	public static PlaceListStore shipped() {
		try (Reader countries = open(COUNTRIES); Reader subdivisions = open(SUBDIVISIONS); Reader cities = open(CITIES);
				Reader hand = open(HAND); Reader timeZones = open(TIME_ZONES); Reader regions = open(REGIONS)) {
			return PlaceListStore.read(countries, subdivisions, cities, hand, timeZones, regions);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Reader open(String file) throws IOException {
		return new InputStreamReader(new ClassPathResource(file).getInputStream(), StandardCharsets.UTF_8);
	}

}
