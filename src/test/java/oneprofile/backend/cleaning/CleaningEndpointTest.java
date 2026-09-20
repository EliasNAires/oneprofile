package oneprofile.backend.cleaning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CleaningEndpoint.class)
class CleaningEndpointTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CorpusCleaning cleaning;

	@Test
	void reportsWhatTheRunCleanedAndWhatEachRuleCollapsed() {
		given(this.cleaning.cleanAll()).willReturn(new CorpusCleaning.Report(179098, 115286, 104906, 3412, 426, 1921, 4282));

		assertThat(this.mvc.post().uri("/cleanings")).hasStatusOk().bodyJson().isEqualTo("""
				{"vacancies":179098,"distinctTitles":115286,"distinctCleanedTitles":104906,\
				"collapsedBySpacing":3412,"collapsedByGenderMarkers":426,"collapsedByWhitelist":1921,\
				"collapsedBySeniorityWords":4282}""");
	}

}
