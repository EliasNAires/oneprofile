package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * The country set the country-set pass read for one vacancy (#13): every set of places it states, and the
 * offers that move a person rather than add a country (ADR-0027).
 *
 * @param statedSets its stated sets, in the order of their positions
 * @param offersVisaSponsorship whether its description offers visa sponsorship
 * @param offersRelocation whether its description offers relocation
 */
public record CountrySet(List<StatedSet> statedSets, boolean offersVisaSponsorship, boolean offersRelocation) {

	public CountrySet {
		statedSets = List.copyOf(statedSets);
	}

}
