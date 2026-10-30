package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.CountrySetVacancy;
import oneprofile.backend.storage.normalizedvacancy.LocationListEnum;
import oneprofile.backend.storage.normalizedvacancy.LocationPlaces;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import oneprofile.backend.storage.placelist.PlaceListStore;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import oneprofile.backend.workers.normalization.WorkModeRule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class CountrySetRunTest {

	private static final PlaceListStore PLACE_LIST = PlaceListConfiguration.shipped();

	private static final PlaceRule PLACES = new PlaceRule(PLACE_LIST);

	private static final CountrySetRule RULE = new CountrySetRule(PLACES, new WorkModeRule(new TitleCleaningRule()));

	private static final EligibilityRule ELIGIBILITY = new EligibilityRule(PLACE_LIST);

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(vacancy(1, WorkModeEnum.REMOTE, "Remote"));

		run(10).readCountrySets();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearCountrySets();
		order.verify(this.normalized).countrySetScopeAfter(0, 10);
	}

	@Test
	void recordsTheCountrySetOfEveryVacancy() {
		holds(vacancy(1, WorkModeEnum.REMOTE, "Remote - US", "You must be authorized to work in the US."),
				vacancy(2, null, null));

		run(10).readCountrySets();

		Map<Long, CountrySet> recorded = recorded();
		assertThat(recorded).containsOnlyKeys(1L, 2L);
		assertThat(recorded.get(1L).statedSets()).extracting((set) -> set.source() + " " + set.kind())
			.containsExactly("LOCATION LOCATION", "DESCRIPTION AUTHORIZATION");
		assertThat(recorded.get(2L).statedSets()).isEmpty();
	}

	@Test
	void reportsTheStatesOfArgentinaAndTheUnitedStatesByWorkModeAndBySource() {
		holds(vacancy(1, WorkModeEnum.REMOTE, "Remote - US"), vacancy(2, WorkModeEnum.ONSITE, "Berlin, Germany"),
				vacancy(3, null, "Remote", "This role is open to candidates in Argentina."),
				vacancy(4, WorkModeEnum.REMOTE, "Remote - US", "US citizenship required."));

		CountrySetRun.Report report = run(10).readCountrySets();

		assertThat(report.vacancies()).isEqualTo(4);
		CountrySetRun.Country argentina = report.countries().get("AR");
		assertThat(argentina.states()).isEqualTo(new CountrySetRun.States(1, 2, 1));
		assertThat(argentina.byWorkMode()).isEqualTo(Map.of("REMOTE", new CountrySetRun.States(0, 1, 1),
				"ONSITE", new CountrySetRun.States(0, 1, 0), "UNDECLARED", new CountrySetRun.States(1, 0, 0)));
		assertThat(argentina.bySource()).isEqualTo(Map.of("NONE", new CountrySetRun.States(0, 0, 1),
				"LOCATION", new CountrySetRun.States(0, 1, 0), "DESCRIPTION", new CountrySetRun.States(1, 1, 0)));
		CountrySetRun.Country unitedStates = report.countries().get("US");
		assertThat(unitedStates.states()).isEqualTo(new CountrySetRun.States(2, 2, 0));
	}

	@Test
	void reportsHowManyOnsiteAndHybridVacanciesHaveACityACountryOrNeither() {
		holds(vacancy(1, WorkModeEnum.ONSITE, "Berlin, Germany"), vacancy(2, WorkModeEnum.HYBRID, "Germany"),
				vacancy(3, WorkModeEnum.HYBRID, "Remote"), vacancy(4, WorkModeEnum.ONSITE, null),
				vacancy(5, WorkModeEnum.REMOTE, "Berlin, Germany"));

		assertThat(run(10).readCountrySets().onsitePlaces()).isEqualTo(new CountrySetRun.OnsitePlaces(4, 1, 1, 2));
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(vacancy(1, null, "Paris"), vacancy(2, null, "Paris"), vacancy(3, null, "Paris"));

		assertThat(run(2).readCountrySets().vacancies()).isEqualTo(3);

		then(this.normalized).should().countrySetScopeAfter(0, 2);
		then(this.normalized).should().countrySetScopeAfter(2, 2);
		then(this.normalized).should().countrySetScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private CountrySetRun run(int batch) {
		return new CountrySetRun(RULE, ELIGIBILITY, this.normalized, batch);
	}

	private static CountrySetVacancy vacancy(long vacancyId, WorkModeEnum workMode, String location,
			String... description) {
		List<LocationPlaces> locations = (location == null) ? List.of()
				: List.of(new LocationPlaces(LocationListEnum.LOCATION, 0, location, PLACES.read(location, null).places()));
		List<Segment> segments = new ArrayList<>();
		for (String text : description) {
			segments.add(new Segment(SegmentKindEnum.SENTENCE, null, text, false));
		}
		return new CountrySetVacancy(vacancyId, (workMode != null) ? Set.of(workMode) : Set.of(), "Software Engineer",
				locations, segments);
	}

	private void holds(CountrySetVacancy... scope) {
		given(this.normalized.countrySetScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, CountrySet> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, CountrySet>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordCountrySets(captor.capture());
		Map<Long, CountrySet> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
