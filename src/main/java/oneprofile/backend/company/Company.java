package oneprofile.backend.company;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

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

	private String name;

	@Enumerated(EnumType.STRING)
	private BoardStatus boardStatus;

	private Instant probedAt;

	protected Company() {
	}

	public Company(Ats ats, String slug) {
		this.ats = ats;
		this.slug = slug;
	}

	/**
	 * Records what a probe found. A probe that brings no name leaves the name the company already
	 * has, because the name is read from the openings a board publishes and an empty board publishes
	 * none.
	 * @param boardStatus what the probe found the board to be
	 * @param name the readable name the board gave, or null if it gave none
	 * @param probedAt when the probe was made
	 */
	public void probed(BoardStatus boardStatus, String name, Instant probedAt) {
		this.boardStatus = boardStatus;
		this.probedAt = probedAt;
		if (name != null) {
			this.name = name;
		}
	}

	/** The readable name its board gave, or null if no probe has read one yet. */
	public String name() {
		return this.name;
	}

	/** What the last probe found its board to be, or null if it has never been probed. */
	public BoardStatus boardStatus() {
		return this.boardStatus;
	}

	/** When it was last probed, or null if it has never been probed. */
	public Instant probedAt() {
		return this.probedAt;
	}

}
