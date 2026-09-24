package oneprofile.backend.cleaning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DescriptionCleaningTest {

	private final DescriptionCleaning cleaning = new DescriptionCleaning();

	@Test
	void makesEverySpaceASingleOrdinaryOne() {
		assertThat(this.cleaning.clean("  Build\u202fAPIs \u3000 in\u2002Go.  ")).isEqualTo("Build APIs in Go.");
	}

	@Test
	void takesOutWhatIsDecorationRatherThanText() {
		assertThat(this.cleaning.clean("🚀 Join us!\u2764\ufe0f Perks:\uf0b7Equity ● Remote\u200dfirst ☐ Apply®"))
			.isEqualTo("Join us! Perks: Equity Remote first Apply");
	}

	@Test
	void keepsWhatTheTextIsWrittenWith() {
		String text = "C#, C++ & .NET (R&D) — $120k–$150k/yr, 100% remote; e-mail jobs@acme.io? “Yes” · • Ingénieur 「エンジニア」、हिन्दी <5 years> = 5× don´t";
		assertThat(this.cleaning.clean(text)).isEqualTo(text);
	}

}
