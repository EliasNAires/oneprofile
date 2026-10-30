package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import oneprofile.backend.storage.company.AtsEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PlaceEndpoint.class)
class PlaceEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PlaceRun run;

	@Test
	void reportsHowManyStringsNamePlacesAndTheResidue() {
		given(this.run.readPlaces()).willReturn(new PlaceRun.Report(3, 2,
				Map.of(AtsEnum.LEVER, new PlaceRun.Strings(4, 2, 1)), List.of(new PlaceRun.Residue("Xanadu", 1))));

		assertThat(this.mvc.post().uri("/places")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"vacancies":3,"withPlaces":2,"strings":{"LEVER":{"read":4,"withPlaces":2,"residue":1}},
				"residue":[{"location":"Xanadu","strings":1}]}""");
	}

}
