package oneprofile.backend.workers.stoplist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(StoplistEndpoint.class)
class StoplistEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private StoplistRun stoplist;

	@Test
	void reportsWhatTheRunMeasured() {
		given(this.stoplist.measure()).willReturn(new StoplistRun.Report(100, 40, 12, 30, 0.3, 9,
				List.of(new StoplistRun.WordPieces("the", 20)), 300, 0.02, 5,
				List.of(new StoplistRun.WordShare("make", 0.02, 900))));

		assertThat(this.mvc.post().uri("/stoplists")).hasStatusOk().bodyJson().isEqualTo("""
				{"pieces":100,"words":40,"head":12,"closedClassPieces":30,"closedClassShare":0.3,"headOfTheRest":9,\
				"top":[{"word":"the","pieces":20}],"guardSetSize":300,"cutoff":0.02,"capitalizationLetsThrough":5,\
				"guardAtTheCutoff":[{"word":"make","share":0.02,"midSentence":900}]}""");
	}

}
