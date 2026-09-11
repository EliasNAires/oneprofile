package oneprofile.backend.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "ats", "slug" }))
public class Company {

	@Id
	@GeneratedValue
	private Long id;

	@Enumerated(EnumType.STRING)
	private Ats ats;

	private String slug;

	/** Only the ATS knows the readable name; it stays null until the board is probed. */
	private String name;

	/** Null until the board has been probed for the first time. */
	@Enumerated(EnumType.STRING)
	private BoardStatus boardStatus;

	private Instant lastProbedAt;

	protected Company() {
	}

	public Company(Ats ats, String slug) {
		this.ats = ats;
		this.slug = slug;
	}

	/**
	 * Takes down the outcome of a probe. A probe that brought no name —every board
	 * without openings— leaves the name already known untouched instead of erasing it.
	 */
	public void recordProbe(BoardStatus status, String name, Instant probedAt) {
		this.boardStatus = status;
		if (name != null) {
			this.name = name;
		}
		this.lastProbedAt = probedAt;
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

	public String getName() {
		return name;
	}

	public BoardStatus getBoardStatus() {
		return boardStatus;
	}

	public Instant getLastProbedAt() {
		return lastProbedAt;
	}
}
