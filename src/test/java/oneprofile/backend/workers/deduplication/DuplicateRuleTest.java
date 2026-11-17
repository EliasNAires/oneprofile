package oneprofile.backend.workers.deduplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.DuplicateVacancy.Location;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.workers.deduplication.DuplicateRule.Candidate;
import org.junit.jupiter.api.Test;

class DuplicateRuleTest {

	private final DuplicateRule rule = new DuplicateRule();

	/** Equal location strings with different declared countries stay apart. */
	@Test
	void differentDeclaredCountriesKeepTheSameLocationApart() {
		var key = this.rule.key("acme", "Engineer", List.of(new Location("London", "United Kingdom")), Set.of());
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Engineer",
				List.of(new Location("London", "Canada")), Set.of()));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Engineer",
				List.of(new Location("London", null)), Set.of()));
		assertThat(this.rule.key("acme", "Engineer", List.of(new Location("London", null)), Set.of()))
			.isEqualTo(this.rule.key("acme", "Engineer", List.of(new Location(" london ", null)), Set.of()));
	}

	/** Country whitespace and case do not split otherwise equal candidates. */
	@Test
	void foldsWhitespaceAndCaseInDeclaredCountries() {
		assertThat(this.rule.key("acme", "Engineer",
				List.of(new Location(" London ", " UNITED\u00a0Kingdom\n")), Set.of()))
			.isEqualTo(this.rule.key("acme", "Engineer",
					List.of(new Location("london", "united kingdom")), Set.of()));
	}

	@Test
	void identicalDescriptionsMergeAndEveryWordMustMatch() {
		assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, "Build reliable systems."),
				candidate(2, 10, null, "Build reliable systems.")))).containsExactlyEntriesOf(Map.of(1L, 2L));
		assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, "Build reliable systems."),
				candidate(2, 10, null, "Build scalable systems.")))).isEmpty();
	}

	@Test
	void normalizesWhitespaceAndCaseInTitlesDescriptionsAndLocations() {
		assertThat(this.rule.key("acme", "  SOFTWARE\tEngineer\n", List.of(new Location(" New\u00a0York ", null)), Set.of()))
			.isEqualTo(this.rule.key("acme", "software engineer", List.of(new Location("new york", null)), Set.of()));
		assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, "  BUILD\u00a0reliable\n systems. "),
				candidate(2, 10, null, "build reliable systems.")))).containsExactlyEntriesOf(Map.of(1L, 2L));
	}

	@Test
	void usesRootCaseFoldingRegardlessOfTheDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr"));
			assertThat(this.rule.key("acme", "ENGINEER", List.of(new Location("ITALY", null)), Set.of()))
				.isEqualTo(this.rule.key("acme", "engineer", List.of(new Location("italy", null)), Set.of()));
			assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, "SHIP IT"),
					candidate(2, 10, null, "ship it")))).containsEntry(1L, 2L);
		}
		finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void everyPartOfTheKeyMustMatch() {
		var key = this.rule.key("acme", "Software Engineer", List.of(new Location("London", null)), Set.of(WorkModeEnum.REMOTE));
		assertThat(key).isNotEqualTo(this.rule.key("other", "Software Engineer", List.of(new Location("London", null)), Set.of(WorkModeEnum.REMOTE)));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Software Developer", List.of(new Location("London", null)), Set.of(WorkModeEnum.REMOTE)));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Software Engineer", List.of(new Location("Paris", null)), Set.of(WorkModeEnum.REMOTE)));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Software Engineer", List.of(new Location("London", null), new Location("Paris", null)), Set.of(WorkModeEnum.REMOTE)));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Software Engineer", List.of(new Location("London", null)), Set.of(WorkModeEnum.HYBRID)));
		assertThat(key).isNotEqualTo(this.rule.key("acme", "Software Engineer", List.of(new Location("London", null)), Set.of(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID)));
	}

	@Test
	void locationOrderAndRepetitionsDoNotMatter() {
		assertThat(this.rule.key("acme", "Engineer", List.of(new Location("London", null), new Location("PARIS", null), new Location(" london ", null)),
				Set.of(WorkModeEnum.REMOTE, WorkModeEnum.HYBRID)))
			.isEqualTo(this.rule.key("acme", "Engineer", List.of(new Location("Paris", null), new Location("London", null)),
					Set.of(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE)));
	}

	@Test
	void nullAndBlankDescriptionsNeitherRepeatNorHaveDuplicates() {
		assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, null), candidate(2, 10, null, null),
				candidate(3, 10, null, ""), candidate(4, 10, null, " \t\n\u00a0"),
				candidate(5, 10, null, "Build")))).isEmpty();
	}

	@Test
	void keepsTheOldestBoardAndItsNewestVacancy() {
		assertThat(this.rule.duplicates(List.of(candidate(1, 20, "2020-01-01T00:00:00Z", "Build"),
				candidate(2, 20, "2024-01-01T00:00:00Z", "Build"),
				candidate(3, 10, "2021-01-01T00:00:00Z", "Build"))))
			.containsExactlyInAnyOrderEntriesOf(Map.of(1L, 2L, 3L, 2L));
	}

	@Test
	void tiedBoardsKeepTheLowestCompanyId() {
		assertThat(this.rule.duplicates(List.of(candidate(1, 20, "2020-01-01T00:00:00Z", "Build"),
				candidate(2, 10, "2020-01-01T00:00:00Z", "Build"))))
			.containsExactlyEntriesOf(Map.of(1L, 2L));
		assertThat(this.rule.duplicates(List.of(candidate(1, 20, null, "Build"), candidate(2, 10, null, "Build"))))
			.containsExactlyEntriesOf(Map.of(1L, 2L));
	}

	@Test
	void undatedBoardsComeAfterEveryDatedBoard() {
		assertThat(this.rule.duplicates(List.of(candidate(1, 10, null, "Build"),
				candidate(2, 20, "2020-01-01T00:00:00Z", "Build"))))
			.containsExactlyEntriesOf(Map.of(1L, 2L));
	}

	@Test
	void onOneBoardDatedBeatsNullAndDateTiesKeepTheHighestVacancyId() {
		assertThat(this.rule.duplicates(List.of(candidate(3, 10, null, "Build"),
				candidate(1, 10, "2020-01-01T00:00:00Z", "Build"),
				candidate(2, 10, "2020-01-01T00:00:00Z", "Build"))))
			.containsExactlyInAnyOrderEntriesOf(Map.of(1L, 2L, 3L, 2L));
	}

	/** One candidate with a raw description and an optional publication date. */
	private Candidate candidate(long vacancyId, long companyId, String date, String description) {
		return new Candidate(vacancyId, companyId, (date != null) ? Instant.parse(date) : null, description);
	}

}
