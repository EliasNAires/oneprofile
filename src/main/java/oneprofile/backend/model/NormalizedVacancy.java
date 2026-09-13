package oneprofile.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * What the title of a vacancy says once cleaned, with its seniority and work mode taken
 * out to fields of their own. It is derived rather than mirrored: it can be recomputed
 * whenever a rule changes without asking the board again, and it goes away with its
 * vacancy through the cascade of the foreign key.
 */
@Entity
public class NormalizedVacancy {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(name = "vacancy_id", unique = true)
	private Vacancy vacancy;

	/** Null when nothing is left once the seniority and the work mode go out. */
	private String title;

	@Enumerated(EnumType.STRING)
	private Seniority seniority;

	@Enumerated(EnumType.STRING)
	private WorkMode workMode;

	protected NormalizedVacancy() {
	}

	public NormalizedVacancy(Vacancy vacancy) {
		this.vacancy = vacancy;
	}

	public void describe(String title, Seniority seniority, WorkMode workMode) {
		this.title = title;
		this.seniority = seniority;
		this.workMode = workMode;
	}

	public Long getId() {
		return id;
	}

	public Vacancy getVacancy() {
		return vacancy;
	}

	public String getTitle() {
		return title;
	}

	public Seniority getSeniority() {
		return seniority;
	}

	public WorkMode getWorkMode() {
		return workMode;
	}
}
