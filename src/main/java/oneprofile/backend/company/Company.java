package oneprofile.backend.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * An employer that has a board on an ATS. A slug is unique only within its ATS, so a company is
 * identified by the pair.
 */
@Entity
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private Ats ats;

	private String slug;

	protected Company() {
	}

	public Company(Ats ats, String slug) {
		this.ats = ats;
		this.slug = slug;
	}

}
