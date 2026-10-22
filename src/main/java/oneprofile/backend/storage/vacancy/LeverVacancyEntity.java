package oneprofile.backend.storage.vacancy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import oneprofile.backend.storage.company.CompanyEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A vacancy published on a Lever board, with the fields Lever structures kept as it writes them. Its
 * workplace type is Lever's field, not the work mode a pass derives from it (ADR-0021); it shares
 * the column Ashby's workplace type is held in, as its pay interval shares Ashby's.
 */
@Entity(name = "LeverVacancy")
@DiscriminatorValue("LEVER")
public class LeverVacancyEntity extends VacancyEntity {

	private String workplaceType;

	private String country;

	private String commitment;

	private String team;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> allLocations;

	private String payInterval;

	protected LeverVacancyEntity() {
	}

	LeverVacancyEntity(CompanyEntity company, String externalId) {
		super(company, externalId);
	}

	@Override
	public void publishedAs(PublishedVacancy published) {
		if (!(published instanceof PublishedLeverVacancy lever)) {
			throw new IllegalArgumentException("A Lever vacancy cannot take on " + published);
		}
		publishedAsAnyAts(lever);
		this.workplaceType = lever.workplaceType();
		this.country = lever.country();
		this.commitment = lever.commitment();
		this.team = lever.team();
		this.allLocations = lever.allLocations();
		this.payInterval = lever.payInterval();
	}

	/** Its workplace type as Lever names it, such as {@code remote}, or null if Lever gave none. */
	public String workplaceType() {
		return this.workplaceType;
	}

	/** The country Lever places it in, as a two-letter code, or null if Lever gave none. */
	public String country() {
		return this.country;
	}

	/** How the work is held, such as {@code Full-time}, or null if the board named none. */
	public String commitment() {
		return this.commitment;
	}

	/** The team within its department, or null if the board named none. */
	public String team() {
		return this.team;
	}

	/** Every location it names, the first one included, empty if it names none. */
	public List<String> allLocations() {
		return this.allLocations;
	}

	/** The interval its pay range is quoted in, as Lever writes it, or null if Lever gave none. */
	public String payInterval() {
		return this.payInterval;
	}

}
