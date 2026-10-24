package oneprofile.backend.storage.vacancy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import oneprofile.backend.storage.company.CompanyEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A vacancy published on a Workday board, with the fields Workday structures kept as it writes them.
 * Its country, a two-letter code, and every location it names share the columns Lever's take
 * (ADR-0021); its time type has a column of its own.
 */
@Entity(name = "WorkdayVacancy")
@DiscriminatorValue("WORKDAY")
public class WorkdayVacancyEntity extends VacancyEntity {

	private String timeType;

	private String country;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> allLocations;

	protected WorkdayVacancyEntity() {
	}

	WorkdayVacancyEntity(CompanyEntity company, String externalId) {
		super(company, externalId);
	}

	@Override
	public void publishedAs(PublishedVacancy published) {
		if (!(published instanceof PublishedWorkdayVacancy workday)) {
			throw new IllegalArgumentException("A Workday vacancy cannot take on " + published);
		}
		publishedAsAnyAts(workday);
		this.timeType = workday.timeType();
		this.country = workday.country();
		this.allLocations = workday.allLocations();
	}

	/** How the work is held, such as {@code Full time}, or null if Workday gave none or it was not read. */
	public String timeType() {
		return this.timeType;
	}

	/** The country of its requisition, as a two-letter code, or null if Workday gave none or it was not read. */
	public String country() {
		return this.country;
	}

	/** Every location it names, the main one first, or null if its page was not read. */
	public List<String> allLocations() {
		return this.allLocations;
	}

}
