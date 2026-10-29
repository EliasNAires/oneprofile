package oneprofile.backend.workers.seniority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SeniorityEndpoint.class)
class SeniorityEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private SeniorityRun run;

	@Test
	void reportsTheCoverageTheRunReached() {
		given(this.run.readSeniorities()).willReturn(new SeniorityRun.Report(3, 1, 2, 2, 1, 1,
				new SeniorityRun.Disagreement(1, 0, 1), Map.of(SeniorityLevelEnum.MID, 2)));

		assertThat(this.mvc.post().uri("/seniorities")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":3,"namedByTitle":1,"withRequiredYears":2,"withSeniorityLevel":2,"numberedOnly":1,
				"numberedResolved":1,"disagreement":{"both":1,"yearsAbove":0,"yearsBelow":1},"bySeniorityLevel":{"MID":2}}""");
	}

}
