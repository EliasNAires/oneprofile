package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Every title here is a real one, taken from the measurements run against the 128.953
 * vacancies of the database; none was made up.
 */
class TitleCleanerTest {

	@Test
	void keepsTheCharactersThatAreLanguages() {
		assertThat(TitleCleaner.clean("Senior Software Engineer, C++")).isEqualTo("senior software engineer c++");
		assertThat(TitleCleaner.clean("Software Engineer (C#)")).isEqualTo("software engineer c#");
		assertThat(TitleCleaner.clean("1535 Staff Backend Engineer .NET")).isEqualTo("1535 staff backend engineer .net");
		assertThat(TitleCleaner.clean("Node.js Developer")).isEqualTo("node.js developer");
		assertThat(TitleCleaner.clean("Avionics R&D Technician")).isEqualTo("avionics r&d technician");
	}

	@Test
	void dropsThoseSameCharactersWhereTheyAreJustPunctuation() {
		assertThat(TitleCleaner.clean("Software Engineer.")).isEqualTo("software engineer");
		assertThat(TitleCleaner.clean("#hiring Product Designer")).isEqualTo("hiring product designer");
		assertThat(TitleCleaner.clean("Sales & Marketing Coordinator")).isEqualTo("sales marketing coordinator");
	}

	@Test
	void dropsTheApostropheWithoutLeavingAHole() {
		assertThat(TitleCleaner.clean("Women's Health Nurse")).isEqualTo("womens health nurse");
	}

	@Test
	void dropsTheDiacriticsSoTheAccentedAndThePlainMeet() {
		assertThat(TitleCleaner.clean("Ingeniería de Datos")).isEqualTo("ingenieria de datos");
		assertThat(TitleCleaner.clean("Aide a domicile")).isEqualTo("aide a domicile");
	}

	@Test
	void keepsTitlesThatAreNotWrittenInLatinLetters() {
		assertThat(TitleCleaner.clean("쿠팡 Sr Employee Relation Specialist"))
				.isEqualTo("쿠팡 sr employee relation specialist");
	}

	@Test
	void dropsTheAcronymThatOnlyRepeatsTheTitle() {
		// 1.393 vacancies were published both ways, with the acronym and without it.
		assertThat(TitleCleaner.clean("Registered Behavior Technician (RBT)"))
				.isEqualTo("registered behavior technician");
		assertThat(TitleCleaner.clean("Board Certified Behavior Analyst (BCBA)"))
				.isEqualTo("board certified behavior analyst");
		assertThat(TitleCleaner.clean("Licensed Practical Nurse (LPN)")).isEqualTo("licensed practical nurse");
		assertThat(TitleCleaner.clean("Chief Information Security Officer (CISO)"))
				.isEqualTo("chief information security officer");
	}

	@Test
	void letsTheAcronymSkipTheShortestWords() {
		assertThat(TitleCleaner.clean("Senior Software Development Engineer in Test (SDET)"))
				.isEqualTo("senior software development engineer in test");
	}

	@Test
	void keepsWhatClosesTheTitleWhenItSaysSomethingNew() {
		// All four sit exactly where a redundant acronym would, and none of them is one:
		// this is why the rule asks for the initials instead of dropping the last word.
		assertThat(TitleCleaner.clean("Senior Software Architect (.NET)")).isEqualTo("senior software architect .net");
		assertThat(TitleCleaner.clean("Dialysis Technician - Thousand Oaks, CA 91359"))
				.isEqualTo("dialysis technician thousand oaks ca 91359");
		assertThat(TitleCleaner.clean("Account Executive (US)")).isEqualTo("account executive us");
		assertThat(TitleCleaner.clean("Certified Nurse Midwife (CNM) - PRN")).isEqualTo("certified nurse midwife cnm prn");
	}

	@Test
	void dropsTheGenderMarkWhereverItSits() {
		assertThat(TitleCleaner.clean("Aide à domicile (H/F)")).isEqualTo("aide a domicile");
		assertThat(TitleCleaner.clean("Psychologe (m/w/d)")).isEqualTo("psychologe");
		assertThat(TitleCleaner.clean("Solution Architect (m/w/d) Financial Services"))
				.isEqualTo("solution architect financial services");
	}

	@Test
	void leavesALoneLetterAloneWhenItIsNotTheGenderMark() {
		// The "e" is what is left of "Assistant(e)", and only the "h f" run is a mark.
		assertThat(TitleCleaner.clean("Assistant(e) de vie H/F")).isEqualTo("assistant e de vie");
	}

	@Test
	void givesAnEmptyStringWhenThereIsNothingToRead() {
		assertThat(TitleCleaner.clean(null)).isEmpty();
		assertThat(TitleCleaner.clean("")).isEmpty();
		assertThat(TitleCleaner.clean(" --- !!! ")).isEmpty();
	}
}
