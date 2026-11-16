package oneprofile.backend.storage.normalizedvacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class GroupMarkTest {

	/** A vacancy belongs to a group or repeats another vacancy, never both or neither. */
	@Test
	void holdsExactlyOneOfAGroupAndTheVacancyItRepeats() {
		assertThat(GroupMark.inGroup(7)).isEqualTo(new GroupMark(7L, null));
		assertThat(GroupMark.duplicateOf(9)).isEqualTo(new GroupMark(null, 9L));
		assertThatIllegalArgumentException().isThrownBy(() -> new GroupMark(null, null));
		assertThatIllegalArgumentException().isThrownBy(() -> new GroupMark(7L, 9L));
	}

}
