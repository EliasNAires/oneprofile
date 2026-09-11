package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlToTextTest {

	@Test
	void unescapesTwiceAndDropsTheTags() {
		// Exactly as Greenhouse answers it: the markup is escaped, so a &nbsp; of the
		// original text arrives as &amp;nbsp; and needs a second pass.
		String content = "&lt;div class=&quot;content-intro&quot;&gt;&lt;p&gt;&lt;strong&gt;WHO WE ARE:"
				+ "&amp;nbsp;&lt;/strong&gt;Sales &amp;amp; Marketing&lt;/p&gt;&lt;/div&gt;";

		assertThat(HtmlToText.plainText(content)).isEqualTo("WHO WE ARE: Sales & Marketing");
	}

	@Test
	void readsAnEntityThatOnlySurvivesTheSecondUnescape() {
		assertThat(HtmlToText.plainText("&lt;p&gt;3&amp;nbsp;&amp;ndash;&amp;nbsp;5 years&lt;/p&gt;"))
				.isEqualTo("3 – 5 years");
	}

	@Test
	void givesNullWhenThereIsNothingToRead() {
		assertThat(HtmlToText.plainText(null)).isNull();
		assertThat(HtmlToText.plainText("")).isNull();
		assertThat(HtmlToText.plainText("&lt;p&gt;&lt;/p&gt;")).isNull();
	}
}
