package oneprofile.backend.workers.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import oneprofile.backend.storage.company.AtsEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SweepEndpoint.class)
class SweepEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private BoardSweepRun sweep;

	@Test
	void reportsWhatTheRunFoundForTheAtsItNames() {
		given(this.sweep.sweepAll(AtsEnum.ASHBY)).willReturn(new BoardSweepRun.Report(4, 120, 12, 108, 3, 1));

		assertThat(this.mvc.post().uri("/sweeps/ashby")).hasStatusOk().bodyJson().isEqualTo("""
				{"boards":4,"published":120,"added":12,"updated":108,"deleted":3,"unreadable":1}""");
	}

}
