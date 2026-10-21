package oneprofile.backend.storage.vacancy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Instant;
import oneprofile.backend.storage.company.CompanyEntity;

/** A vacancy published on a Greenhouse board, with what Greenhouse publishes of its own. */
@Entity(name = "GreenhouseVacancy")
@DiscriminatorValue("GREENHOUSE")
public class GreenhouseVacancyEntity extends VacancyEntity {

	private String payTitle;

	private Instant updatedAt;

	private String language;

	protected GreenhouseVacancyEntity() {
	}

	GreenhouseVacancyEntity(CompanyEntity company, String externalId) {
		super(company, externalId);
	}

	@Override
	public void publishedAs(PublishedVacancy published) {
		if (!(published instanceof PublishedGreenhouseVacancy greenhouse)) {
			throw new IllegalArgumentException("A Greenhouse vacancy cannot take on " + published);
		}
		publishedAsAnyAts(greenhouse);
		this.payTitle = greenhouse.payTitle();
		this.updatedAt = greenhouse.updatedAt();
		this.language = greenhouse.language();
	}

	/** The heading the company put above its pay range, or null if it publishes none. */
	public String payTitle() {
		return this.payTitle;
	}

	/** When the board last changed it, or null if the board did not say. */
	public Instant updatedAt() {
		return this.updatedAt;
	}

	/** The language its board says it is written in, or null if the board said none. */
	public String language() {
		return this.language;
	}

}
