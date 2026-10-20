package oneprofile.backend.storage.company;

import java.util.Locale;

/** An applicant tracking system that hosts company boards and exposes them publicly. */
public enum AtsEnum {

	GREENHOUSE,

	ASHBY;

	/**
	 * The ATS a path names, written in any case, as the endpoints take it.
	 * @param name the ATS's name, such as {@code ashby}
	 * @return the ATS
	 * @throws IllegalArgumentException if no ATS goes by that name
	 */
	public static AtsEnum named(String name) {
		return valueOf(name.toUpperCase(Locale.ROOT));
	}

}
