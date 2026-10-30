package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CountrySetEndpoint.class)
class CountrySetEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CountrySetRun run;

	@Test
	void reportsTheStatesByCountryAndHowPreciselyOnsiteVacanciesNameTheirPlace() {
		CountrySetRun.States states = new CountrySetRun.States(1, 2, 3);
		given(this.run.readCountrySets()).willReturn(new CountrySetRun.Report(6,
				Map.of("AR", new CountrySetRun.Country(states, Map.of("REMOTE", states), Map.of("NONE", states))),
				new CountrySetRun.OnsitePlaces(4, 2, 1, 1)));

		assertThat(this.mvc.post().uri("/country-sets")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":6,"countries":{"AR":{"states":{"eligible":1,"excluded":2,"unknown":3},
				"byWorkMode":{"REMOTE":{"eligible":1,"excluded":2,"unknown":3}},
				"bySource":{"NONE":{"eligible":1,"excluded":2,"unknown":3}}}},
				"onsitePlaces":{"vacancies":4,"withCity":2,"countryOnly":1,"neither":1}}""");
	}

}
