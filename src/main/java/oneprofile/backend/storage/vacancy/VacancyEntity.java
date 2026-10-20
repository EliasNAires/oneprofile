package oneprofile.backend.storage.vacancy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.List;
import oneprofile.backend.storage.company.CompanyEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A single open position at a company, held as a mirror of the board it was published on. It is
 * identified by its company and the id the ATS gives it, which is all that survives a board
 * republishing everything else about it.
 * <p>
 * The link to the company is one way: a company has thousands of openings, so a collection on the
 * other side would be a burden rather than a convenience.
 */
@Entity(name = "Vacancy")
public class VacancyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "company_id")
	private CompanyEntity company;

	private String externalId;

	private String title;

	private String location;

	private String department;

	private String description;

	private String language;

	private String url;

	private Long payMinCents;

	private Long payMaxCents;

	private String payCurrency;

	private String payTitle;

	private String payInterval;

	private Instant firstPublishedAt;

	private Instant updatedAt;

	private String workplaceType;

	private String addressCountry;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<SecondaryLocation> secondaryLocations;

	protected VacancyEntity() {
	}

	public VacancyEntity(CompanyEntity company, String externalId) {
		this.company = company;
		this.externalId = externalId;
	}

	/**
	 * Takes on everything the board publishes about it, other than the identity it is held under.
	 * @param published the opening as the board publishes it today
	 */
	public void publishedAs(PublishedVacancy published) {
		this.title = published.title();
		this.location = published.location();
		this.department = published.department();
		this.description = published.description();
		this.language = published.language();
		this.url = published.url();
		this.payMinCents = published.payMinCents();
		this.payMaxCents = published.payMaxCents();
		this.payCurrency = published.payCurrency();
		this.payTitle = published.payTitle();
		this.payInterval = published.payInterval();
		this.firstPublishedAt = published.firstPublishedAt();
		this.updatedAt = published.updatedAt();
		this.workplaceType = published.workplaceType();
		this.addressCountry = published.addressCountry();
		this.secondaryLocations = published.secondaryLocations();
	}

	/** The id the ATS gives it, unique within its board. */
	public String externalId() {
		return this.externalId;
	}

	/** Its title as the company wrote it. */
	public String title() {
		return this.title;
	}

	/** Where it says the work is, as free text. */
	public String location() {
		return this.location;
	}

	/** The department it belongs to, or null if the board named none. */
	public String department() {
		return this.department;
	}

	/** Its description as marked lines of plain text, or null if the board published none. */
	public String description() {
		return this.description;
	}

	/** The language its board says it is written in, or null if the board said none. */
	public String language() {
		return this.language;
	}

	/** Where it is published. */
	public String url() {
		return this.url;
	}

	/** The bottom of its published pay range, or null if it publishes none. */
	public Long payMinCents() {
		return this.payMinCents;
	}

	/** The top of its published pay range, or null if it publishes none. */
	public Long payMaxCents() {
		return this.payMaxCents;
	}

	/** The currency its pay range is in, or null if it publishes none. */
	public String payCurrency() {
		return this.payCurrency;
	}

	/** The heading the company put above its pay range, or null if it publishes none. */
	public String payTitle() {
		return this.payTitle;
	}

	/** The interval its pay range is quoted in, as its ATS writes it, or null if the ATS gave none. */
	public String payInterval() {
		return this.payInterval;
	}

	/** When it was first published. */
	public Instant firstPublishedAt() {
		return this.firstPublishedAt;
	}

	/** When the board last changed it, or null if its ATS does not say. */
	public Instant updatedAt() {
		return this.updatedAt;
	}

	/** Its work mode as its ATS names it, or null if the ATS gave none. */
	public String workplaceType() {
		return this.workplaceType;
	}

	/** The country of its own address as its ATS writes it, or null if the ATS gave none. */
	public String addressCountry() {
		return this.addressCountry;
	}

	/** The other places it names for the work, or null if its ATS does not provide them. */
	public List<SecondaryLocation> secondaryLocations() {
		return this.secondaryLocations;
	}

}
