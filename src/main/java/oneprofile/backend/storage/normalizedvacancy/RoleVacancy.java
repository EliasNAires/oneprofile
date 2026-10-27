package oneprofile.backend.storage.normalizedvacancy;

import java.util.List;

/**
 * One vacancy the role assignment reads: one of the engineering subset, with the normalized titles the
 * title breakdown wrote for it.
 *
 * @param vacancyId the vacancy it was derived from
 * @param normalizedTitles its normalized titles, one per head its title names; empty where nothing was
 * left once its facts were out, as of a bare {@code Manager}
 */
public record RoleVacancy(long vacancyId, List<String> normalizedTitles) {

	public RoleVacancy {
		normalizedTitles = List.copyOf(normalizedTitles);
	}

	/** As the database holds the titles, for a query to build. */
	public RoleVacancy(long vacancyId, String[] normalizedTitles) {
		this(vacancyId, List.of(normalizedTitles));
	}

}
