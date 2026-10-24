package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.company.RegionEnum;
import org.junit.jupiter.api.Test;

class WorkdayUrlRuleTest {

	@Test
	void readsTheTenantAndHostFromTheHostnameAndTheSiteFromThePath() {
		assertThat(WorkdayUrlRule.slugIn(
				"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Engineer_JR2019101"))
			.contains("nvidia.wd5/nvidiaexternalcareersite");
		assertThat(WorkdayUrlRule.slugIn("https://3m.wd1.myworkdayjobs.com/Search?q=engineer")).contains("3m.wd1/search");
	}

	@Test
	void skipsALocaleBeforeTheSite() {
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite/details/x"))
			.contains("nvidia.wd5/nvidiaexternalcareersite");
		assertThat(WorkdayUrlRule.slugIn("https://siemens.wd3.myworkdayjobs.com/de-de/Careers")).contains("siemens.wd3/careers");
	}

	@Test
	void readsTheSameBoardFromItsAddressOnTheOtherDomain() {
		assertThat(WorkdayUrlRule.slugIn(
				"https://wd5.myworkdaysite.com/en-US/recruiting/nvidia/NVIDIAExternalCareerSite/job/Engineer_JR2019101"))
			.contains("nvidia.wd5/nvidiaexternalcareersite");
		assertThat(WorkdayUrlRule.slugIn("https://wd1.myworkdaysite.com/recruiting/3m/Search")).contains("3m.wd1/search");
	}

	@Test
	void readsTheBoardOfAnApiUrl() {
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com/wday/cxs/nvidia/NVIDIAExternalCareerSite/jobs"))
			.contains("nvidia.wd5/nvidiaexternalcareersite");
	}

	@Test
	void placesEveryBoardInTheUsSinceWorkdaysHostsAreNotRegions() {
		assertThat(WorkdayUrlRule.regionIn("https://siemens.wd3.myworkdayjobs.com/Careers")).isEqualTo(RegionEnum.US);
	}

	@Test
	void hasNoSlugForAnotherHostOrAPathThatIsNoBoard() {
		assertThat(WorkdayUrlRule.slugIn("https://www.myworkdayjobs.com/")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://myworkdayjobs.com/Careers")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com/")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com/robots.txt")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com/en-US")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("http://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://nvidia.wd5.myworkdayjobs.com.example.com/Careers")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://wd5.myworkdaysite.com/en-US/recruiting/nvidia")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://wd5.myworkdaysite.com/Careers")).isEmpty();
		assertThat(WorkdayUrlRule.slugIn("https://workday.wd5.myworkdayjobs.com/%E2%80%8BCareers")).isEmpty();
	}

}
