package oneprofile.backend.storage.vacancyskill;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Persistable;

/**
 * One skill extraction found in one vacancy, with where it found it. Held by the derived facts of
 * the vacancy rather than the vacancy, and deleted with them; the skill by its id in the taxonomy
 * file, which the database cannot reference.
 * <p>
 * A run clears the table before writing, so a row is always new: saying so spares a read of each
 * row before it is inserted.
 */
@Entity(name = "VacancySkill")
@IdClass(VacancySkillEntity.Key.class)
public class VacancySkillEntity implements Persistable<VacancySkillEntity.Key> {

	@Id
	private long normalizedVacancyId;

	@Id
	private String skillId;

	private Integer[] segments;

	private boolean inTitle;

	protected VacancySkillEntity() {
	}

	/**
	 * @param normalizedVacancyId the derived facts of the vacancy it was found in
	 * @param found the skill and where it was found
	 */
	public VacancySkillEntity(long normalizedVacancyId, FoundSkill found) {
		this.normalizedVacancyId = normalizedVacancyId;
		this.skillId = found.skillId();
		this.segments = found.segments().toArray(Integer[]::new);
		this.inTitle = found.inTitle();
	}

	/** The derived facts of the vacancy it was found in. */
	public long normalizedVacancyId() {
		return this.normalizedVacancyId;
	}

	/** The skill, by its id in the taxonomy. */
	public String skillId() {
		return this.skillId;
	}

	/** The indexes of the segments it was found in, empty when only the title names it. */
	public List<Integer> segments() {
		return List.of(this.segments);
	}

	/** Whether the title names it. */
	public boolean inTitle() {
		return this.inTitle;
	}

	@Override
	public Key getId() {
		return new Key(this.normalizedVacancyId, this.skillId);
	}

	@Override
	@Transient
	public boolean isNew() {
		return true;
	}

	/**
	 * What a row is identified by: the pair.
	 *
	 * @param normalizedVacancyId the derived facts of the vacancy
	 * @param skillId the skill
	 */
	public record Key(long normalizedVacancyId, String skillId) implements Serializable {
	}

}
