package oneprofile.backend.normalizedvacancy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;

/**
 * The facts derived from one vacancy by rule, rather than published by its ATS. Derived data is
 * never mixed into the vacancy itself, so a rule can be re-run over the corpus at any time without
 * what the board said being touched.
 * <p>
 * The vacancy is held by its id rather than as an association: every pass over the corpus reads the
 * few fields it derives from and never the vacancy's body, which is most of what a vacancy weighs.
 * The database has the foreign key, and takes these rows with the vacancy when a sweep finds it has
 * left its board.
 */
@Entity
public class NormalizedVacancy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private long vacancyId;

	private String cleanedTitle;

	@Enumerated(EnumType.STRING)
	private ClassificationState classificationState;

	@Enumerated(EnumType.STRING)
	private UnknownReason classificationReason;

	@ElementCollection
	@CollectionTable(name = "normalized_vacancy_title_seniority",
			joinColumns = @JoinColumn(name = "normalized_vacancy_id"))
	@Column(name = "title_seniority")
	@Enumerated(EnumType.STRING)
	private Set<SeniorityLevel> titleSeniorities = new HashSet<>();

	protected NormalizedVacancy() {
	}

	public NormalizedVacancy(long vacancyId) {
		this.vacancyId = vacancyId;
	}

	/**
	 * Takes on what cleaning made of the vacancy's title. Cleaning the same vacancy again replaces
	 * what the last run recorded rather than adding to it.
	 * @param cleaned the vacancy's title once cleaned, and the levels it named
	 */
	public void cleanedAs(CleanedTitle cleaned) {
		this.cleanedTitle = cleaned.title();
		this.titleSeniorities.clear();
		this.titleSeniorities.addAll(cleaned.titleSeniorities());
	}

	/**
	 * Takes on what classification made of the vacancy's cleaned title. Classifying it again
	 * replaces what the last run decided, because a rule change is measured by running the pass
	 * again over the same corpus.
	 * @param classification what the rules decided, and why they left it undecided where they did
	 */
	public void classifiedAs(Classification classification) {
		this.classificationState = classification.state();
		this.classificationReason = classification.reason();
	}

	/** The vacancy these facts were derived from. */
	public long vacancyId() {
		return this.vacancyId;
	}

	/** Its title with what is noise in any title taken out. */
	public String cleanedTitle() {
		return this.cleanedTitle;
	}

	/** What classification made of its cleaned title, null until classification has run. */
	public Classification classification() {
		return (this.classificationState == null) ? null
				: new Classification(this.classificationState, this.classificationReason);
	}

	/** Every seniority level its title names, empty if it names none. */
	public Set<SeniorityLevel> titleSeniorities() {
		return Set.copyOf(this.titleSeniorities);
	}

}
