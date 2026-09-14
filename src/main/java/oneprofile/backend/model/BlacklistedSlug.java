package oneprofile.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A slug that discovery must never save as a company again, such as one truncated by a
 * cut-off Wayback or CommonCrawl line.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "ats", "slug" }))
public class BlacklistedSlug {

	@Id
	@GeneratedValue
	private Long id;

	@Enumerated(EnumType.STRING)
	private Ats ats;

	private String slug;

	protected BlacklistedSlug() {
	}

	public BlacklistedSlug(Ats ats, String slug) {
		this.ats = ats;
		this.slug = slug;
	}

	public Long getId() {
		return id;
	}

	public Ats getAts() {
		return ats;
	}

	public String getSlug() {
		return slug;
	}
}
