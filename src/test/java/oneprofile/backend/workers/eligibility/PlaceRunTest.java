package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import oneprofile.backend.storage.normalizedvacancy.LocationListEnum;
import oneprofile.backend.storage.normalizedvacancy.LocationPlaces;
import oneprofile.backend.storage.normalizedvacancy.LocationString;
import oneprofile.backend.storage.normalizedvacancy.NormalizedVacancyStore;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.PlaceVacancy;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class PlaceRunTest {

	private static final PlaceRule RULE = new PlaceRule(PlaceListConfiguration.shipped());

	private final NormalizedVacancyStore normalized = mock(NormalizedVacancyStore.class);

	@Test
	void clearsWhatTheLastRunRecordedBeforeReadingTheSubset() {
		holds(vacancy(1, AtsEnum.GREENHOUSE, "Remote"));

		run(10).readPlaces();

		InOrder order = inOrder(this.normalized);
		order.verify(this.normalized).clearPlaces();
		order.verify(this.normalized).placeScopeAfter(0, 10);
	}

	@Test
	void recordsThePlacesOfEachLocationString() {
		holds(new PlaceVacancy(1, AtsEnum.ASHBY,
				List.of(new LocationString(LocationListEnum.LOCATION, 0, "London", "United States"),
						new LocationString(LocationListEnum.SECONDARY_LOCATIONS, 0, "Remote - EMEA", null))),
				vacancy(2, AtsEnum.GREENHOUSE, "Remote"), new PlaceVacancy(3, AtsEnum.GREENHOUSE, List.of()));

		run(10).readPlaces();

		assertThat(recorded()).isEqualTo(Map.of(1L,
				List.of(new LocationPlaces(LocationListEnum.LOCATION, 0, "London", List.of(Place.country("US"))),
						new LocationPlaces(LocationListEnum.SECONDARY_LOCATIONS, 0, "Remote - EMEA",
								List.of(Place.region("EMEA")))),
				2L, List.of(new LocationPlaces(LocationListEnum.LOCATION, 0, "Remote", List.of()))));
	}

	@Test
	void reportsByAtsHowManyStringsNamePlacesAndHowManyAreResidue() {
		holds(vacancy(1, AtsEnum.GREENHOUSE, "Paris, TX"), vacancy(2, AtsEnum.GREENHOUSE, "Remote"),
				vacancy(3, AtsEnum.GREENHOUSE, "PICKLE RESEARCH CAMPUS"), vacancy(4, AtsEnum.LEVER, "Toronto"),
				new PlaceVacancy(5, AtsEnum.LEVER, List.of()));

		PlaceRun.Report report = run(10).readPlaces();

		assertThat(report.vacancies()).isEqualTo(5);
		assertThat(report.withPlaces()).isEqualTo(2);
		assertThat(report.strings()).isEqualTo(Map.of(AtsEnum.GREENHOUSE, new PlaceRun.Strings(3, 1, 1),
				AtsEnum.LEVER, new PlaceRun.Strings(1, 1, 0)));
	}

	@Test
	void reportsTheResidueByHowManyStringsHoldIt() {
		holds(vacancy(1, AtsEnum.GREENHOUSE, "PICKLE RESEARCH CAMPUS"), vacancy(2, AtsEnum.GREENHOUSE, "Xanadu"),
				vacancy(3, AtsEnum.WORKDAY, "Xanadu"), vacancy(4, AtsEnum.GREENHOUSE, "Paris"));

		assertThat(run(10).readPlaces().residue()).containsExactly(new PlaceRun.Residue("Xanadu", 2),
				new PlaceRun.Residue("PICKLE RESEARCH CAMPUS", 1));
	}

	@Test
	void walksTheSubsetABatchAtATime() {
		holds(vacancy(1, AtsEnum.GREENHOUSE, "Paris"), vacancy(2, AtsEnum.GREENHOUSE, "Paris"),
				vacancy(3, AtsEnum.GREENHOUSE, "Paris"));

		assertThat(run(2).readPlaces().vacancies()).isEqualTo(3);

		then(this.normalized).should().placeScopeAfter(0, 2);
		then(this.normalized).should().placeScopeAfter(2, 2);
		then(this.normalized).should().placeScopeAfter(3, 2);
	}

	@Test
	void refusesABatchThatHoldsNothing() {
		assertThatThrownBy(() -> run(0)).isInstanceOf(IllegalArgumentException.class);
	}

	private PlaceRun run(int batch) {
		return new PlaceRun(RULE, this.normalized, batch);
	}

	private static PlaceVacancy vacancy(long vacancyId, AtsEnum ats, String location) {
		return new PlaceVacancy(vacancyId, ats, List.of(new LocationString(LocationListEnum.LOCATION, 0, location, null)));
	}

	private void holds(PlaceVacancy... scope) {
		given(this.normalized.placeScopeAfter(anyLong(), anyInt())).willAnswer((invocation) -> {
			long after = invocation.getArgument(0);
			int batch = invocation.getArgument(1);
			return List.of(scope).stream().filter((vacancy) -> vacancy.vacancyId() > after).limit(batch).toList();
		});
	}

	private Map<Long, List<LocationPlaces>> recorded() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, List<LocationPlaces>>> captor = ArgumentCaptor.forClass(Map.class);
		then(this.normalized).should(Mockito.atLeastOnce()).recordPlaces(captor.capture());
		Map<Long, List<LocationPlaces>> all = new HashMap<>();
		captor.getAllValues().forEach(all::putAll);
		return all;
	}

}
