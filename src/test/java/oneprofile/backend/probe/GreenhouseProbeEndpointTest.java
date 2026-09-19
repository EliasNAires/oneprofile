package oneprofile.backend.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GreenhouseProbeEndpoint.class)
class GreenhouseProbeEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private BoardProbe probe;

	@Test
	void reportsWhatTheRunFound() {
		given(this.probe.probeAll()).willReturn(new BoardProbe.Report(3, 1, 2, 1));

		assertThat(this.mvc.post().uri("/probes/greenhouse")).hasStatusOk().bodyJson().isEqualTo("""
				{"active":3,"empty":1,"notFound":2,"unreadable":1}""");
	}

}
