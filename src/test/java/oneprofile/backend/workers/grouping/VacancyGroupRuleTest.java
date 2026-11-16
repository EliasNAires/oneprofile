package oneprofile.backend.workers.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import org.junit.jupiter.api.Test;

class VacancyGroupRuleTest {

	private final VacancyGroupRule rule = new VacancyGroupRule(new DescriptionSimilarityRule());

	@Test
	void differentCitiesOrModesKeepTheirOwnMembers() {
		var a = compared(1, 10, "2020-01-01", "London", WorkModeEnum.ONSITE, 0);
		var b = compared(2, 10, "2021-01-01", "Paris", WorkModeEnum.ONSITE, 0);
		var c = compared(3, 10, "2022-01-01", "London", WorkModeEnum.REMOTE, 0);

		assertThat(this.rule.group(List.of(a, b, c), Map.of(), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(1L, 2L, 3L), Map.of()));
	}

	@Test
	void newestVacancyOnOneBoardIsKeptAndLocationsAreNormalized() {
		var a = compared(1, 10, "2020-01-01", "  LONDON  ", WorkModeEnum.ONSITE, 0);
		var b = compared(2, 10, "2021-01-01", "london", WorkModeEnum.ONSITE, 0);

		assertThat(this.rule.group(List.of(a, b), Map.of(), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(2L), Map.of(1L, 2L)));
	}

	@Test
	void oldestBoardWinsAcrossAtsBoardsButItsNewestVacancyIsKept() {
		var a = compared(1, 10, "2020-01-01", "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 10, "2023-01-01", "London", WorkModeEnum.REMOTE, 0);
		var c = compared(3, 20, "2021-01-01", "London", WorkModeEnum.REMOTE, 0);

		assertThat(this.rule.group(List.of(c, b, a), Map.of(20L, 100), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(2L), Map.of(1L, 2L, 3L, 2L)));
	}

	@Test
	void equalDatesPreferMoreOpenVacanciesThenLowestCompanyId() {
		var a = compared(1, 10, "2020-01-01", "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 20, "2020-01-01", "London", WorkModeEnum.REMOTE, 0);
		assertThat(this.rule.group(List.of(a, b), Map.of(20L, 5), 0.8).getFirst().duplicates())
			.isEqualTo(Map.of(1L, 2L));
		assertThat(this.rule.group(List.of(b, a), Map.of(), 0.8).getFirst().duplicates())
			.isEqualTo(Map.of(2L, 1L));
	}

	@Test
	void nullDatesLoseAndEqualDatesOnOneBoardPreferHighestVacancyId() {
		var a = compared(1, 10, null, "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 20, "2020-01-01", "London", WorkModeEnum.REMOTE, 0);
		var c = compared(3, 20, "2020-01-01", "London", WorkModeEnum.REMOTE, 0);
		assertThat(this.rule.group(List.of(a, b, c), Map.of(10L, 100), 0.8).getFirst().duplicates())
			.isEqualTo(Map.of(1L, 3L, 2L, 3L));
	}

	@Test
	void singleLinkageJoinsTransitively() {
		var a = compared(1, 10, null, "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 10, null, "Paris", WorkModeEnum.REMOTE, 2);
		var c = compared(3, 10, null, "Berlin", WorkModeEnum.REMOTE, 4);
		assertThat(this.rule.group(List.of(a, b, c), Map.of(), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(1L, 2L, 3L), Map.of()));
	}

	@Test
	void singleLinkageDoesNotHideAnIndirectMatchInTheSamePlace() {
		var a = compared(1, 10, null, "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 10, null, "Paris", WorkModeEnum.REMOTE, 2);
		var c = compared(3, 10, null, "London", WorkModeEnum.REMOTE, 4);
		assertThat(this.rule.group(List.of(a, b, c), Map.of(), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(1L, 2L, 3L), Map.of()));
	}

	@Test
	void belowThresholdAndThinDescriptionsStayApart() {
		var a = compared(1, 10, null, "London", WorkModeEnum.REMOTE, 0);
		var b = compared(2, 10, null, "Paris", WorkModeEnum.REMOTE, 50);
		assertThat(this.rule.group(List.of(a, b), Map.of(), 0.8)).hasSize(2);
		var thin = new VacancyGroupRule.ComparedVacancy(3, 10, null, List.of(), Set.of(), new long[0]);
		assertThat(this.rule.group(List.of(thin), Map.of(), 0.8))
			.containsExactly(new VacancyGroupRule.Group(List.of(3L), Map.of()));
		assertThat(this.rule.group(List.of(a, thin), Map.of(), 0.0)).hasSize(2);
	}

	/** Twenty consecutive hashes give a known overlap as their starting point moves. */
	private VacancyGroupRule.ComparedVacancy compared(long id, long companyId, String date, String location,
			WorkModeEnum mode, int start) {
		return new VacancyGroupRule.ComparedVacancy(id, companyId,
				(date == null) ? null : Instant.parse(date + "T00:00:00Z"), List.of(location), Set.of(mode),
				LongStream.range(start, start + 20).toArray());
	}

}
