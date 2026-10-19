package oneprofile.backend.workers.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SkillExtractionEndpoint.class)
class SkillExtractionEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private SkillExtractionRun extraction;

	@Test
	void reportsTheSkillsFoundPerVacancy() {
		given(this.extraction.extractSkills())
			.willReturn(new SkillExtractionRun.Report(3, 4, 1, new TreeMap<>(Map.of(0, 1, 2, 2))));

		assertThat(this.mvc.post().uri("/skill-extractions")).hasStatusOk().bodyJson().isEqualTo("""
				{"vacancies":3,"rows":4,"rowsFromContextKeys":1,"skillsPerVacancy":{"0":1,"2":2}}""");
	}

}
