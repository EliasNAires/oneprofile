package oneprofile.backend.storage.normalizedvacancy;

import jakarta.persistence.Embeddable;

/**
 * One experience requirement the seniority pass accepted in a vacancy's description: a phrase requiring
 * some years of professional experience, for the role or for a skill (PRD §6.1).
 *
 * @param segment the index of the description segment it was read in
 * @param position where the phrase starts in that segment's text
 * @param lowerYears the years it requires at the least: the floor of a range, the number of {@code 5+}
 * @param upperYears the top of its range, null where it names none ({@code 5+ years}, {@code 5 years})
 * @param setsRequiredYears whether its floor is the vacancy's required years: the lowest of its
 * alternatives, and the highest of the floors the vacancy's requirements give
 */
@Embeddable
public record ExperienceRequirement(int segment, int position, int lowerYears, Integer upperYears,
		boolean setsRequiredYears) {
}
