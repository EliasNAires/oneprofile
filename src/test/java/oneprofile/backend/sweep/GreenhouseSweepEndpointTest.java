package oneprofile.backend.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GreenhouseSweepEndpoint.class)
class GreenhouseSweepEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private BoardSweep sweep;

	@Test
	void reportsWhatTheRunFound() {
		given(this.sweep.sweepAll()).willReturn(new BoardSweep.Report(4, 120, 12, 108, 3, 1));

		assertThat(this.mvc.post().uri("/sweeps/greenhouse")).hasStatusOk().bodyJson().isEqualTo("""
				{"boards":4,"published":120,"added":12,"updated":108,"deleted":3,"unreadable":1}""");
	}

}
