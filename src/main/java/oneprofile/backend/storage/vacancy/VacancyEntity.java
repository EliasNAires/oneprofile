package oneprofile.backend.storage.vacancy;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import oneprofile.backend.storage.company.CompanyEntity;

/**
 * A single open position at a company, held as a mirror of the board it was published on. It is
 * identified by its company and the id the ATS gives it, which is all that survives a board
 * republishing everything else about it.
 * <p>
 * Each ATS's vacancy is a class of its own over the one table, told apart by its {@code ats}, which
 * is always its company's. This class holds what any ATS would be expected to publish, so null here
 * means the vacancy did not publish it; what one ATS publishes its own way is held by its class, as
 * that ATS publishes it (ADR-0021).
 * <p>
 * The link to the company is one way: a company has thousands of openings, so a collection on the
 * other side would be a burden rather than a convenience.
 */
@Entity(name = "Vacancy")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ats")
public abstract class VacancyEntity {

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

	private String url;

	private Long payMinCents;

	private Long payMaxCents;

	private String payCurrency;

	private Instant firstPublishedAt;

	protected VacancyEntity() {
	}

	protected VacancyEntity(CompanyEntity company, String externalId) {
		this.company = company;
		this.externalId = externalId;
	}

	/**
	 * A vacancy of a company, of the class of the ATS that published it, holding nothing yet but its
	 * identity.
	 * @param company the company whose board publishes it
	 * @param published the opening as its board publishes it
	 * @return the vacancy to hold it as
	 */
	static VacancyEntity of(CompanyEntity company, PublishedVacancy published) {
		return switch (published) {
			case PublishedGreenhouseVacancy greenhouse -> new GreenhouseVacancyEntity(company, greenhouse.externalId());
			case PublishedAshbyVacancy ashby -> new AshbyVacancyEntity(company, ashby.externalId());
		};
	}

	/**
	 * Takes on everything the board publishes about it, other than the identity it is held under.
	 * @param published the opening as the board publishes it today
	 * @throws IllegalArgumentException if another ATS published it
	 */
	public abstract void publishedAs(PublishedVacancy published);

	/** Takes on what any ATS publishes. */
	protected void publishedAsAnyAts(PublishedVacancy published) {
		this.title = published.title();
		this.location = published.location();
		this.department = published.department();
		this.description = published.description();
		this.url = published.url();
		this.payMinCents = published.payMinCents();
		this.payMaxCents = published.payMaxCents();
		this.payCurrency = published.payCurrency();
		this.firstPublishedAt = published.firstPublishedAt();
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

	/** When it was first published. */
	public Instant firstPublishedAt() {
		return this.firstPublishedAt;
	}

}
