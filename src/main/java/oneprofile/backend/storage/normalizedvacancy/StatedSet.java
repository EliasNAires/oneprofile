package oneprofile.backend.storage.normalizedvacancy;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One set of places a vacancy states, with the text that stated it (#13): one option of its declared location,
 * or a requirement its title or description states. A vacancy's country set is all of them, and its
 * eligibility state for a country is derived from them at query time.
 *
 * @param position where it stands among the vacancy's stated sets, from 0
 * @param source where it was read
 * @param kind what it says
 * @param workMode the work mode of a location option, null where the option names none and the vacancy
 * declares none, and for any other source
 * @param segment the index of the description segment it was read in, null for any other source
 * @param text what stated it: the location string or the option of it, the title, or the segment's text
 * @param places the places it names, regions kept as named; empty for a timezone requirement and for a
 * location option that names none ({@code Remote})
 * @param utcFrom the earliest UTC offset of a timezone requirement, in minutes, null for any other kind
 * @param utcTo the latest UTC offset of a timezone requirement, in minutes, null for any other kind
 */
@Embeddable
public record StatedSet(int position, @Enumerated(EnumType.STRING) StatedSetSourceEnum source,
		@Enumerated(EnumType.STRING) StatedSetKindEnum kind, @Enumerated(EnumType.STRING) WorkModeEnum workMode,
		Integer segment, String text, @JdbcTypeCode(SqlTypes.JSON) @Column(name = "places") List<Place> places,
		Integer utcFrom, Integer utcTo) {

	public StatedSet {
		places = List.copyOf(places);
	}

}
