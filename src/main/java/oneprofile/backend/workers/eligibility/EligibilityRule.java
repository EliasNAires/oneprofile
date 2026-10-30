package oneprofile.backend.workers.eligibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.PlaceKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.placelist.City;
import oneprofile.backend.storage.placelist.PlaceListStore;
import org.springframework.stereotype.Component;

/**
 * Derives a vacancy's eligibility state for one person from its country set, at query time (PRD §5.4, #13): for
 * the country a Finder visitor picks or the profile's, and the person's city where it is known.
 * <p>
 * Each option of the declared location is judged by its own work mode. An onsite or hybrid option's places are
 * its country set (ADR-0027): it is eligible where one of them holds the country, or, for a place with a city
 * and a person whose city is known, where the two cities lie within 50 km (ADR-0028); otherwise excluded. A
 * remote option, or one whose work mode is undeclared, counts only for eligibility (ADR-0004): eligible where
 * one of its places holds the country, otherwise unknown. An option that names no place is unknown.
 * <p>
 * What the title and the description state is then read against every option. The places the description
 * says the vacancy hires in are one set, however many sentences name them ({@code remote within the US}, {@code
 * we also hire in Canada}); each authorization, citizenship, clearance and timezone requirement is a requirement
 * of its own. A set or requirement that leaves the person out excludes, whatever the option says: the country
 * is not among the places, or the person is outside the window. One that holds them makes an unknown option
 * eligible. A region or place the title names counts only for eligibility, as the location does for a
 * remote option. A timezone requirement is judged by the person's city where its time zone is known, otherwise
 * by the country: inside where all of its offsets are, outside where none is, and saying nothing where only
 * some are.
 * <p>
 * A vacancy offered in several options takes the best state of them: eligible over unknown over excluded.
 */
@Component
public class EligibilityRule {

	/** How far apart an onsite or hybrid vacancy's city and the person's may be, in kilometres (ADR-0028). */
	static final double RADIUS = 50;

	private static final double EARTH_RADIUS = 6371.0088;

	private final PlaceListStore placeList;

	public EligibilityRule(PlaceListStore placeList) {
		this.placeList = placeList;
	}

	/**
	 * Derives a vacancy's eligibility state for one person.
	 * @param countrySet the vacancy's country set
	 * @param country the two-letter code of the person's country
	 * @param city the person's city, null where it is not known
	 * @return its state, and where the set that decided it was read
	 */
	public Eligibility eligibility(CountrySet countrySet, String country, City city) {
		List<StatedSet> options = new ArrayList<>();
		List<Place> hiresIn = new ArrayList<>();
		Eligibility excluded = null;
		Eligibility required = null;
		Eligibility named = null;
		for (StatedSet set : countrySet.statedSets()) {
			if (set.source() == StatedSetSourceEnum.LOCATION) {
				options.add(set);
				continue;
			}
			boolean places = set.kind() == StatedSetKindEnum.LOCATION || set.kind() == StatedSetKindEnum.REGION;
			if (places && set.source() == StatedSetSourceEnum.DESCRIPTION) {
				hiresIn.addAll(set.places());
				continue;
			}
			EligibilityStateEnum state = requirement(set, country, city);
			if (places) {
				// A place the title names counts only for eligibility, as the location of a remote option does.
				if (state == EligibilityStateEnum.ELIGIBLE && named == null) {
					named = new Eligibility(state, set.source());
				}
			}
			else if (state == EligibilityStateEnum.EXCLUDED && excluded == null) {
				excluded = new Eligibility(state, set.source());
			}
			else if (state == EligibilityStateEnum.ELIGIBLE && required == null) {
				required = new Eligibility(state, set.source());
			}
		}
		if (!hiresIn.isEmpty()) {
			boolean holds = hiresIn.stream().anyMatch((place) -> holds(place, country));
			Eligibility described = new Eligibility(
					holds ? EligibilityStateEnum.ELIGIBLE : EligibilityStateEnum.EXCLUDED, StatedSetSourceEnum.DESCRIPTION);
			if (!holds && excluded == null) {
				excluded = described;
			}
			else if (holds && required == null) {
				required = described;
			}
		}
		if (excluded != null) {
			return excluded;
		}
		Eligibility best = null;
		for (StatedSet option : options) {
			EligibilityStateEnum state = option(option, country, city);
			Eligibility judged = switch (state) {
				case ELIGIBLE, EXCLUDED -> new Eligibility(state, StatedSetSourceEnum.LOCATION);
				case UNKNOWN -> (named != null) ? named : (required != null) ? required : Eligibility.UNKNOWN;
			};
			if (best == null || judged.state().compareTo(best.state()) < 0) {
				best = judged;
			}
		}
		if (best != null) {
			return best;
		}
		return (named != null) ? named : (required != null) ? required : Eligibility.UNKNOWN;
	}

	/** What one option of the declared location says for the person, by its own work mode. */
	private EligibilityStateEnum option(StatedSet option, String country, City city) {
		if (option.places().isEmpty()) {
			return EligibilityStateEnum.UNKNOWN;
		}
		boolean onsite = option.workMode() == WorkModeEnum.ONSITE || option.workMode() == WorkModeEnum.HYBRID;
		for (Place place : option.places()) {
			boolean holds = (onsite && place.kind() == PlaceKindEnum.CITY && city != null)
					? distance(place, city) <= RADIUS : holds(place, country);
			if (holds) {
				return EligibilityStateEnum.ELIGIBLE;
			}
		}
		return onsite ? EligibilityStateEnum.EXCLUDED : EligibilityStateEnum.UNKNOWN;
	}

	/** What a set the title or the description states says for the person. */
	private EligibilityStateEnum requirement(StatedSet set, String country, City city) {
		if (set.kind() == StatedSetKindEnum.TIMEZONE) {
			return timezone(set, country, city);
		}
		return set.places().stream().anyMatch((place) -> holds(place, country)) ? EligibilityStateEnum.ELIGIBLE
				: EligibilityStateEnum.EXCLUDED;
	}

	private EligibilityStateEnum timezone(StatedSet set, String country, City city) {
		int from = Math.min(set.utcFrom(), set.utcTo());
		int to = Math.max(set.utcFrom(), set.utcTo());
		Integer offset = (city != null) ? this.placeList.offsetOf(city.timeZone()) : null;
		if (offset != null) {
			return (from <= offset && offset <= to) ? EligibilityStateEnum.ELIGIBLE : EligibilityStateEnum.EXCLUDED;
		}
		Set<Integer> offsets = this.placeList.offsetsOf(country);
		if (offsets.isEmpty()) {
			return EligibilityStateEnum.UNKNOWN;
		}
		long inside = offsets.stream().filter((minutes) -> from <= minutes && minutes <= to).count();
		if (inside == offsets.size()) {
			return EligibilityStateEnum.ELIGIBLE;
		}
		return (inside == 0) ? EligibilityStateEnum.EXCLUDED : EligibilityStateEnum.UNKNOWN;
	}

	/** Whether a place holds the country: names it, or is a region that holds it. */
	private boolean holds(Place place, String country) {
		if (place.kind() == PlaceKindEnum.REGION) {
			return this.placeList.countriesOf(place.region()).contains(country);
		}
		return country.equals(place.country());
	}

	/** The great-circle distance between a place's city and the person's, in kilometres. */
	private static double distance(Place place, City city) {
		double fromLatitude = Math.toRadians(place.latitude());
		double toLatitude = Math.toRadians(city.latitude());
		double latitudes = toLatitude - fromLatitude;
		double longitudes = Math.toRadians(city.longitude() - place.longitude());
		double a = Math.pow(Math.sin(latitudes / 2), 2)
				+ Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(longitudes / 2), 2);
		return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(a));
	}

}
