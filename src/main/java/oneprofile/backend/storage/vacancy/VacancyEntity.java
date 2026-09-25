package oneprofile.backend.storage.vacancy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import oneprofile.backend.storage.company.CompanyEntity;

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

	private long externalId;

	private String title;

	private String location;

	private String department;

	private String description;

	private String url;

	private Long payMinCents;

	private Long payMaxCents;

	private String payCurrency;

	private String payTitle;

	private Instant firstPublishedAt;

	private Instant updatedAt;

	protected VacancyEntity() {
	}

	public VacancyEntity(CompanyEntity company, long externalId) {
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
		this.url = published.url();
		this.payMinCents = published.payMinCents();
		this.payMaxCents = published.payMaxCents();
		this.payCurrency = published.payCurrency();
		this.payTitle = published.payTitle();
		this.firstPublishedAt = published.firstPublishedAt();
		this.updatedAt = published.updatedAt();
	}

	/** The id the ATS gives it, unique within its board. */
	public long externalId() {
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

	/** Its body as plain text, or null if the board published none. */
	public String description() {
		return this.description;
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

	/** When it was first published. */
	public Instant firstPublishedAt() {
		return this.firstPublishedAt;
	}

	/** When the board last changed it. */
	public Instant updatedAt() {
		return this.updatedAt;
	}

}
