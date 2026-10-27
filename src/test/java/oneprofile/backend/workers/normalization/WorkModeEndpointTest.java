package oneprofile.backend.workers.normalization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(WorkModeEndpoint.class)
class WorkModeEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private WorkModeRun run;

	@Test
	void reportsByAtsWhichStepDecidedTheWorkModes() {
		given(this.run.readWorkModes()).willReturn(new WorkModeRun.Report(3,
				Map.of(AtsEnum.LEVER, Map.of(WorkModeEvidenceEnum.FIELD, 2, WorkModeEvidenceEnum.NONE, 1))));

		assertThat(this.mvc.post().uri("/work-modes")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":3,"decidedBy":{"LEVER":{"FIELD":2,"NONE":1}}}""");
	}

}
