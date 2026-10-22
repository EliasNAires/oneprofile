package oneprofile.backend.workers.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.company.RegionEnum;
import org.junit.jupiter.api.Test;

class LeverUrlRuleTest {

	@Test
	void readsTheSlugFromTheFirstPathSegmentOfEitherHost() {
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co/finix/d2907c20-a739-4c0b-a10e-94aaf1f72c78/apply"))
			.contains("finix");
		assertThat(LeverUrlRule.slugIn("https://jobs.eu.lever.co/kwalee")).contains("kwalee");
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co/binance?department=finance%20%26%20administration"))
			.contains("binance");
	}

	@Test
	void readsTheRegionFromTheHost() {
		assertThat(LeverUrlRule.regionIn("https://jobs.lever.co/finix")).isEqualTo(RegionEnum.US);
		assertThat(LeverUrlRule.regionIn("https://jobs.eu.lever.co/kwalee/5f1c")).isEqualTo(RegionEnum.EU);
	}

	@Test
	void lowercasesTheSlugBecauseLeverPublishesEveryBoardUnderALowercaseOne() {
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co/Finix")).contains("finix");
	}

	@Test
	void hasNoSlugForAnotherHostOrAPathThatIsNoBoard() {
		assertThat(LeverUrlRule.slugIn("https://api.lever.co/v0/postings/finix")).isEmpty();
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co/")).isEmpty();
		assertThat(LeverUrlRule.slugIn("http://jobs.lever.co/finix")).isEmpty();
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co.example.com/finix")).isEmpty();
		assertThat(LeverUrlRule.slugIn("https://jobs.lever.co/%E2%80%8Bfinix")).isEmpty();
	}

}
