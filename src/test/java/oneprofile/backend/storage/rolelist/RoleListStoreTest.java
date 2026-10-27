package oneprofile.backend.storage.rolelist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class RoleListStoreTest {

	private static final String ROLES = """
			slug\tlabel
			data-engineer\tData engineer
			""";

	@Test
	void readsTheWordsAndTheTitlesTheRuleGetsWrong() {
		RoleListStore list = RoleListStore.read(new StringReader(ROLES), new StringReader("""
				phrase\tuse\thead\trole
				data\tkind\t\tdata-engineer
				engineer\thead\t\t
				"""), new StringReader("""
				title\trole
				data\tnone
				"""));
		assertThat(list.words()).containsExactly(new RoleWord("data", RoleWordUseEnum.KIND, "", "data-engineer"),
				new RoleWord("engineer", RoleWordUseEnum.HEAD, "", null));
		assertThat(list.titles()).containsEntry("data", "none");
	}

	@Test
	void refusesARoleTheListDoesNotHold() {
		assertThatIllegalStateException()
			.isThrownBy(() -> RoleListStore.read(new StringReader(ROLES),
					new StringReader("phrase\tuse\thead\trole\nweb\tkind\t\tfrontend-engineer\n"),
					new StringReader("title\trole\n")))
			.withMessageContaining("frontend-engineer");
		assertThatIllegalStateException()
			.isThrownBy(() -> RoleListStore.read(new StringReader(ROLES),
					new StringReader("phrase\tuse\thead\trole\n"),
					new StringReader("title\trole\nweb developer\tfrontend-engineer\n")))
			.withMessageContaining("frontend-engineer");
	}

	@Test
	void refusesAWordReadTwiceTheSameWay() {
		assertThatIllegalStateException()
			.isThrownBy(() -> RoleListStore.read(new StringReader(ROLES), new StringReader("""
					phrase\tuse\thead\trole
					data\tkind\t\tdata-engineer
					data\tkind\t\tnone
					"""), new StringReader("title\trole\n")))
			.withMessageContaining("data");
		assertThatIllegalStateException()
			.isThrownBy(() -> RoleListStore.read(new StringReader(ROLES), new StringReader("""
					phrase\tuse\thead\trole
					engineer\thead\t\tdata-engineer
					engineer\tjob\t\tnone
					"""), new StringReader("title\trole\n")))
			.withMessageContaining("engineer");
	}

	@Test
	void readsTheListTheApplicationShips() {
		assertThat(RoleListConfiguration.shipped().words()).hasSizeGreaterThan(100);
	}

}
