package oneprofile.backend.storage.normalizedvacancy;

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Entity(name = "NormalizedVacancy")
public class NormalizedVacancyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private long vacancyId;

	private String cleanedTitle;

	@Enumerated(EnumType.STRING)
	private ClassificationStateEnum classificationState;

	@Enumerated(EnumType.STRING)
	private UnknownReasonEnum classificationReason;

	@Enumerated(EnumType.STRING)
	private ClassificationSignalEnum classificationSignal;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<Segment> descriptionSegments;

	private String detectedLanguage;

	@ElementCollection
	@CollectionTable(name = "normalized_vacancy_title_seniority",
			joinColumns = @JoinColumn(name = "normalized_vacancy_id"))
	@Column(name = "title_seniority")
	@Enumerated(EnumType.STRING)
	private Set<SeniorityLevelEnum> titleSeniorities = new HashSet<>();

	private String[] normalizedTitles;

	@Enumerated(EnumType.STRING)
	private ManagementLevelEnum managementLevel;

	@ElementCollection
	@CollectionTable(name = "normalized_vacancy_guarded_title_seniority",
			joinColumns = @JoinColumn(name = "normalized_vacancy_id"))
	@Column(name = "title_seniority")
	@Enumerated(EnumType.STRING)
	private Set<SeniorityLevelEnum> guardedTitleSeniorities = new HashSet<>();

	private String[] roles;

	@Enumerated(EnumType.STRING)
	private WorkModeEnum workMode;

	@Enumerated(EnumType.STRING)
	private SeniorityLevelEnum seniorityLevel;

	private Integer requiredYears;

	@ElementCollection
	@CollectionTable(name = "normalized_vacancy_experience_requirement",
			joinColumns = @JoinColumn(name = "normalized_vacancy_id"))
	private Set<ExperienceRequirement> experienceRequirements = new HashSet<>();

	protected NormalizedVacancyEntity() {
	}

	public NormalizedVacancyEntity(long vacancyId) {
		this.vacancyId = vacancyId;
	}

	/**
	 * Takes on what cleaning made of the vacancy. Cleaning the same vacancy again replaces what the
	 * last run recorded rather than adding to it.
	 * @param cleaned the vacancy's title once cleaned with the levels it named, its description's
	 * segments, and the language detected where its board declared none
	 */
	public void cleanedAs(CleanedVacancy cleaned) {
		CleanedTitle title = cleaned.cleanedTitle();
		this.cleanedTitle = title.title();
		this.titleSeniorities.clear();
		this.titleSeniorities.addAll(title.titleSeniorities());
		this.descriptionSegments = cleaned.descriptionSegments();
		this.detectedLanguage = cleaned.detectedLanguage();
	}

	/**
	 * Takes on what classification made of the vacancy. Classifying it again replaces what the last
	 * run decided, because a rule change is measured by running the pass again over the same corpus.
	 * @param classification what the rules decided, why the title left it undecided where it did,
	 * and which signal decided
	 */
	public void classifiedAs(Classification classification) {
		this.classificationState = classification.state();
		this.classificationReason = classification.reason();
		this.classificationSignal = classification.signal();
	}

	/**
	 * Takes on what the body decided of a vacancy its title left unknown, keeping the title's reason.
	 * @param state what the body decided, {@code IN} or {@code OUT}
	 */
	public void decidedByBody(ClassificationStateEnum state) {
		classifiedAs(Classification.byBody(state, this.classificationReason));
	}

	/**
	 * Takes on what the title breakdown made of the vacancy, replacing what the last run recorded. The
	 * levels cleaning read are kept: the breakdown reads only the guarded words cleaning leaves.
	 * @param breakdown its normalized titles, the levels its guarded seniority words name and its
	 * management level
	 */
	public void brokenDownAs(TitleBreakdown breakdown) {
		this.normalizedTitles = breakdown.normalizedTitles().toArray(String[]::new);
		this.managementLevel = breakdown.managementLevel();
		this.guardedTitleSeniorities.clear();
		this.guardedTitleSeniorities.addAll(breakdown.titleSeniorities());
	}

	/**
	 * Takes on the roles the role rule gave the vacancy, replacing what the last run recorded.
	 * @param roles the slugs of its roles, empty if none of its normalized titles has one
	 */
	public void assignedRoles(List<String> roles) {
		this.roles = roles.toArray(String[]::new);
	}

	/**
	 * Takes on the work mode the work-mode rule read for the vacancy, replacing what the last run recorded.
	 * @param workMode its work mode
	 */
	public void workingAs(WorkModeEnum workMode) {
		this.workMode = workMode;
	}

	/**
	 * Takes on the seniority the seniority pass derived for the vacancy, replacing what the last run recorded.
	 * @param seniority its seniority level, its required years and the experience requirements they come from
	 */
	public void placedAt(VacancySeniority seniority) {
		this.seniorityLevel = seniority.seniorityLevel();
		this.requiredYears = seniority.requiredYears();
		this.experienceRequirements.clear();
		this.experienceRequirements.addAll(seniority.requirements());
	}

	/** The vacancy these facts were derived from. */
	public long vacancyId() {
		return this.vacancyId;
	}

	/** Its title with what is noise in any title taken out. */
	public String cleanedTitle() {
		return this.cleanedTitle;
	}

	/** What classification made of it, null until classification has run. */
	public Classification classification() {
		return (this.classificationState == null) ? null
				: new Classification(this.classificationState, this.classificationReason, this.classificationSignal);
	}

	/** Its cleaned description cut into segments, null until cleaning has cut it. */
	public List<Segment> descriptionSegments() {
		return this.descriptionSegments;
	}

	/**
	 * The language cleaning detected its description to be in, null where its board declared one or
	 * the text did not say.
	 */
	public String detectedLanguage() {
		return this.detectedLanguage;
	}

	/**
	 * Every seniority level its title names, empty if it names none: those cleaning read, and those
	 * the title breakdown read from the guarded words.
	 */
	public Set<SeniorityLevelEnum> titleSeniorities() {
		Set<SeniorityLevelEnum> named = new HashSet<>(this.titleSeniorities);
		named.addAll(this.guardedTitleSeniorities);
		return Set.copyOf(named);
	}

	/** Its normalized titles, one per head its title names, null until the title breakdown has run. */
	public List<String> normalizedTitles() {
		return (this.normalizedTitles == null) ? null : List.of(this.normalizedTitles);
	}

	/** The rank of the leadership word its title names over an area, null if it names none. */
	public ManagementLevelEnum managementLevel() {
		return this.managementLevel;
	}

	/** The slugs of its roles, one per normalized title that has one, null until the role assignment has run. */
	public List<String> roles() {
		return (this.roles == null) ? null : List.of(this.roles);
	}

	/** Where it says its work is done, null where it declares none or the work-mode run has not reached it. */
	public WorkModeEnum workMode() {
		return this.workMode;
	}

	/**
	 * What the seniority pass derived for it, its experience requirements in the order written; nothing
	 * where the pass has not reached it.
	 */
	public VacancySeniority seniority() {
		return new VacancySeniority(this.seniorityLevel, this.requiredYears,
				this.experienceRequirements.stream()
					.sorted(Comparator.comparingInt(ExperienceRequirement::segment)
						.thenComparingInt(ExperienceRequirement::position))
					.toList());
	}

}
