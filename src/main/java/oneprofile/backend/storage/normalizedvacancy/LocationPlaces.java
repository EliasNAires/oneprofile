package oneprofile.backend.storage.normalizedvacancy;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One string of a vacancy's declared location with the places it was read as (#26).
 *
 * @param list the list it came from
 * @param position where in that list it stands, from 0
 * @param location the string as its ATS publishes it
 * @param places the places it names, in the order written, empty where it names none
 */
@Embeddable
public record LocationPlaces(@Enumerated(EnumType.STRING) @Column(name = "list") LocationListEnum list, int position,
		String location, @JdbcTypeCode(SqlTypes.JSON) List<Place> places) {

	public LocationPlaces {
		places = List.copyOf(places);
	}

}
