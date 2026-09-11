package oneprofile.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One opening of a board, as the ATS answers it today. The link to the company is
 * one-way on purpose: a company has thousands of openings, so a collection on the
 * other side would be a problem rather than a convenience.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "company_id", "external_id" }))
public class Vacancy {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "company_id")
	private Company company;

	/** The id of the ATS, unique within the board but not across boards. */
	private Long externalId;

	private String title;

	/** Free text as the company wrote it: "Remote - U.S.", "Bengaluru, India", "AMER". */
	@Column(columnDefinition = "text")
	private String location;

	private String department;

	@Column(columnDefinition = "text")
	private String description;

	@Column(columnDefinition = "text")
	private String url;

	private String language;

	private Long payMinCents;

	private Long payMaxCents;

	private String payCurrency;

	/**
	 * The heading the company put above the range, kept as it came. It is the only
	 * thing telling an hourly rate from a yearly salary.
	 */
	private String payTitle;

	private Instant firstPublished;

	private Instant updatedAt;

	protected Vacancy() {
	}

	public Vacancy(Company company, Long externalId) {
		this.company = company;
		this.externalId = externalId;
	}

	/** Everything but the company and the external id, which are the identity. */
	public void describe(String title, String location, String department, String description, String url,
			String language, Long payMinCents, Long payMaxCents, String payCurrency, String payTitle,
			Instant firstPublished, Instant updatedAt) {
		this.title = title;
		this.location = location;
		this.department = department;
		this.description = description;
		this.url = url;
		this.language = language;
		this.payMinCents = payMinCents;
		this.payMaxCents = payMaxCents;
		this.payCurrency = payCurrency;
		this.payTitle = payTitle;
		this.firstPublished = firstPublished;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public Company getCompany() {
		return company;
	}

	public Long getExternalId() {
		return externalId;
	}

	public String getTitle() {
		return title;
	}

	public String getLocation() {
		return location;
	}

	public String getDepartment() {
		return department;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public String getLanguage() {
		return language;
	}

	public Long getPayMinCents() {
		return payMinCents;
	}

	public Long getPayMaxCents() {
		return payMaxCents;
	}

	public String getPayCurrency() {
		return payCurrency;
	}

	public String getPayTitle() {
		return payTitle;
	}

	public Instant getFirstPublished() {
		return firstPublished;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
