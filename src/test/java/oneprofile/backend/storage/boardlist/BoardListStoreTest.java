package oneprofile.backend.storage.boardlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.StringReader;
import oneprofile.backend.storage.company.AtsEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoardListStoreTest {

	/** The header is skipped and each ATS has its own slugs. */
	@Test
	void readsSlugsPerAts() {
		BoardListStore list = read("lever\tdemo\tDemo board\nlever\ttest\tTest board\nashby\tdemo\tDemo board\n");

		assertThat(list.unreadSlugsOf(AtsEnum.LEVER)).containsExactlyInAnyOrder("demo", "test");
		assertThat(list.unreadSlugsOf(AtsEnum.ASHBY)).containsExactly("demo");
		assertThat(list.unreadSlugsOf(AtsEnum.GREENHOUSE)).isEmpty();
	}

	/** Every row must name an ATS, a slug and a reason exactly once. */
	@ParameterizedTest
	@ValueSource(strings = { "lever\tdemo", "lever\tdemo\treason\textra", "unknown\tdemo\treason",
			"lever\t \treason", "lever\tdemo\t ", "lever\tdemo\t", "", "   ",
			"lever\tdemo\treason\nlever\tdemo\tother reason" })
	void refusesMalformedOrDuplicateRows(String rows) {
		assertThatIllegalStateException().isThrownBy(() -> read(rows + "\n"));
	}

	/** The application ships the demo and test boards kept out of the corpus. */
	@Test
	void readsTheShippedList() {
		BoardListStore list = BoardListConfiguration.shipped();

		assertThat(list.unreadSlugsOf(AtsEnum.LEVER)).contains("levertest");
		assertThat(list.unreadSlugsOf(AtsEnum.WORKDAY)).contains("accenture.wd103/accenturecareers");
	}

	/** Reads rows under the file's header. */
	private BoardListStore read(String rows) {
		return BoardListStore.read(new StringReader("ats\tslug\treason\n" + rows));
	}

}
