package oneprofile.backend.storage.vacancy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import oneprofile.backend.storage.company.CompanyEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A vacancy published on an Ashby board, with the fields Ashby structures kept as it writes them.
 * Its workplace type is Ashby's field, not the work mode a pass derives from it (ADR-0021).
 */
@Entity(name = "AshbyVacancy")
@DiscriminatorValue("ASHBY")
public class AshbyVacancyEntity extends VacancyEntity {

	private String workplaceType;

	private String addressCountry;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<SecondaryLocation> secondaryLocations;

	private String payInterval;

	protected AshbyVacancyEntity() {
	}

	AshbyVacancyEntity(CompanyEntity company, String externalId) {
		super(company, externalId);
	}

	@Override
	public void publishedAs(PublishedVacancy published) {
		if (!(published instanceof PublishedAshbyVacancy ashby)) {
			throw new IllegalArgumentException("An Ashby vacancy cannot take on " + published);
		}
		publishedAsAnyAts(ashby);
		this.workplaceType = ashby.workplaceType();
		this.addressCountry = ashby.addressCountry();
		this.secondaryLocations = ashby.secondaryLocations();
		this.payInterval = ashby.payInterval();
	}

	/** Its workplace type as Ashby names it, such as {@code Remote}, or null if Ashby gave none. */
	public String workplaceType() {
		return this.workplaceType;
	}

	/** The country of its own address as Ashby writes it, or null if Ashby gave none. */
	public String addressCountry() {
		return this.addressCountry;
	}

	/** The other places it names for the work, empty if it names none. */
	public List<SecondaryLocation> secondaryLocations() {
		return this.secondaryLocations;
	}

	/** The interval its pay range is quoted in, as Ashby writes it, or null if Ashby gave none. */
	public String payInterval() {
		return this.payInterval;
	}

}
