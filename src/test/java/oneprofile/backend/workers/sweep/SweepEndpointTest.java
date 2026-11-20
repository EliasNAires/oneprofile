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
		given(this.sweep.sweep(AtsEnum.ASHBY, null, null)).willReturn(new BoardSweepRun.Report(4, 120, 12, 108, 3, 1, 1, 0));

		assertThat(this.mvc.post().uri("/sweeps/ashby")).hasStatusOk().bodyJson().isEqualTo("""
				{"boards":4,"published":120,"added":12,"updated":108,"deleted":3,"notFound":1,"unreadable":1,"wallSeconds":0}""");
	}

	/** Optional slice parameters are passed unchanged to the run. */
	@Test
	void passesTheSliceToTheRun() {
		given(this.sweep.sweep(AtsEnum.ASHBY, "ramp", 3))
			.willReturn(new BoardSweepRun.Report(3, 0, 0, 0, 0, 0, 0, 7));
		assertThat(this.mvc.post().uri("/sweeps/ashby").param("after", "ramp").param("limit", "3"))
			.hasStatusOk().bodyJson().isEqualTo("""
				{"boards":3,"published":0,"added":0,"updated":0,"deleted":0,"notFound":0,"unreadable":0,"wallSeconds":7}""");
	}

	/** Workday's short sweep has an endpoint of its own, sliced as the long one is. */
	@Test
	void runsWorkdaysShortSweepOnTheSlice() {
		given(this.sweep.shortSweep("acme.wd1/careers", 20))
			.willReturn(new BoardSweepRun.Report(20, 900, 4, 896, 2, 0, 0, 60));
		assertThat(this.mvc.post().uri("/sweeps/workday/short").param("after", "acme.wd1/careers").param("limit", "20"))
			.hasStatusOk().bodyJson().isEqualTo("""
				{"boards":20,"published":900,"added":4,"updated":896,"deleted":2,"notFound":0,"unreadable":0,"wallSeconds":60}""");
	}

}
