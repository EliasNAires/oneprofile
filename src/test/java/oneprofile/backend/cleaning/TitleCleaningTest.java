package oneprofile.backend.cleaning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import oneprofile.backend.normalizedvacancy.CleanedTitle;
import oneprofile.backend.normalizedvacancy.SeniorityLevel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TitleCleaningTest {

	private final TitleCleaning cleaning = new TitleCleaning();

	@Test
	void takesEveryKindOfNoiseOutOfOneTitle() {
		assertThat(this.cleaning.clean("Senior Software Engineer (m/w/d), Payments"))
			.isEqualTo(new CleanedTitle("Software Engineer Payments", Set.of(SeniorityLevel.SENIOR)));
	}

	@Test
	void leavesNothingOfACharacterWhoseWordIsGone() {
		assertThat(this.cleaning.clean("Senior+ Applied Scientist"))
			.isEqualTo(new CleanedTitle("Applied Scientist", Set.of(SeniorityLevel.SENIOR)));
		assertThat(this.cleaning.clean("Principal/ Sr.Principal Digital Design Engineer"))
			.isEqualTo(new CleanedTitle("Digital Design Engineer",
					Set.of(SeniorityLevel.SENIOR, SeniorityLevel.PRINCIPAL)));
	}

	@Test
	void leavesATitleThatIsAllRoleAlone() {
		assertThat(this.cleaning.clean("Backend Engineer"))
			.isEqualTo(new CleanedTitle("Backend Engineer", Set.of()));
	}

	@Nested
	class GenderMarkers {

		@Test
		void removesABracketedMarkerWhole() {
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Electrical Engineer (m/f/d)"))
				.isEqualTo("Electrical Engineer");
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Ingénieur Logiciel (H/F)")).isEqualTo("Ingénieur Logiciel");
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("FSQA Manager - Processed Category (f/m/x)"))
				.isEqualTo("FSQA Manager - Processed Category");
		}

		@Test
		void removesAMarkerThatWasWrittenWithoutBrackets() {
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("IT-Consultant m/f/d")).isEqualTo("IT-Consultant");
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Stage Software Engineer - Paris - H/F/X"))
				.isEqualTo("Stage Software Engineer - Paris -");
		}

		@Test
		void removesAMarkerFromInsideATitle() {
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Automation Engineer (m/f/d) PLC & Commissioning"))
				.isEqualTo("Automation Engineer PLC & Commissioning");
		}

		@Test
		void leavesAloneTheSlashedLettersThatNameSomethingElse() {
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Flight Software Engineer, Embedded C/C++"))
				.isEqualTo("Flight Software Engineer, Embedded C/C++");
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Patient Care Technician - 4 Medical - FT - D/N"))
				.isEqualTo("Patient Care Technician - 4 Medical - FT - D/N");
			assertThat(TitleCleaningTest.this.cleaning.withoutGenderMarkers("Engineer I/II")).isEqualTo("Engineer I/II");
		}

	}

	@Nested
	class CharacterWhitelist {

		@Test
		void keepsOnlyLettersDigitsAndWhatCarriesAMeaning() {
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Backend Engineer, Payments — Remote!"))
				.isEqualTo("Backend Engineer Payments Remote");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Front-End Engineer")).isEqualTo("Front End Engineer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("  Engineer   II  ")).isEqualTo("Engineer II");
		}

		@Test
		void keepsTheCharacterThatCarriesATechnology() {
			assertThat(TitleCleaningTest.this.cleaning.whitelisted(".NET Developer")).isEqualTo(".NET Developer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Node.js Engineer")).isEqualTo("Node.js Engineer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("C# Engineer")).isEqualTo("C# Engineer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("C++ Engineer")).isEqualTo("C++ Engineer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("R&D Engineer")).isEqualTo("R&D Engineer");
		}

		@Test
		void dropsThoseSameCharactersWhereTheyCarryNothing() {
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Sales & Marketing Lead")).isEqualTo("Sales Marketing Lead");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Engineer.")).isEqualTo("Engineer");
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("Engineer (#1 team)")).isEqualTo("Engineer 1 team");
		}

		@Test
		void leavesATitleWrittenInAnotherScriptReadable() {
			assertThat(TitleCleaningTest.this.cleaning.whitelisted("백엔드 엔지니어 (신입)")).isEqualTo("백엔드 엔지니어 신입");
		}

	}

	@Nested
	class SeniorityWords {

		@Test
		void extractsAndRemovesTheWordsThatNameALevelWhereverTheyAppear() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Senior Backend Engineer"))
				.isEqualTo(new CleanedTitle("Backend Engineer", Set.of(SeniorityLevel.SENIOR)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Sr. Data Analyst"))
				.isEqualTo(new CleanedTitle("Data Analyst", Set.of(SeniorityLevel.SENIOR)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Sr.Data Analyst"))
				.isEqualTo(new CleanedTitle("Data Analyst", Set.of(SeniorityLevel.SENIOR)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Junior Developer"))
				.isEqualTo(new CleanedTitle("Developer", Set.of(SeniorityLevel.JUNIOR)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Jr Developer"))
				.isEqualTo(new CleanedTitle("Developer", Set.of(SeniorityLevel.JUNIOR)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Principal Engineer"))
				.isEqualTo(new CleanedTitle("Engineer", Set.of(SeniorityLevel.PRINCIPAL)));
		}

		@Test
		void readsSemiSeniorAsMid() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Semi Senior QA Engineer"))
				.isEqualTo(new CleanedTitle("QA Engineer", Set.of(SeniorityLevel.MID)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Semi-Senior QA Engineer"))
				.isEqualTo(new CleanedTitle("QA Engineer", Set.of(SeniorityLevel.MID)));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("SSR Frontend Developer"))
				.isEqualTo(new CleanedTitle("Frontend Developer", Set.of(SeniorityLevel.MID)));
		}

		@Test
		void keepsEveryLevelATitleNames() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Senior Principal Engineer"))
				.isEqualTo(new CleanedTitle("Engineer", Set.of(SeniorityLevel.SENIOR, SeniorityLevel.PRINCIPAL)));
		}

		@Test
		void namesALevelOnceHoweverOftenTheTitleSaysIt() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Senior Engineer, Senior Platform"))
				.isEqualTo(new CleanedTitle("Engineer, Platform", Set.of(SeniorityLevel.SENIOR)));
		}

		@Test
		void leavesInTheWordsThatNeedAReadingOfTheJob() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Staff Engineer II"))
				.isEqualTo(new CleanedTitle("Staff Engineer II", Set.of()));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Mid Level Entry Engineer"))
				.isEqualTo(new CleanedTitle("Mid Level Entry Engineer", Set.of()));
		}

		@Test
		void leavesAloneAWordThatMerelyStartsWithOne() {
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("SRE Manager"))
				.isEqualTo(new CleanedTitle("SRE Manager", Set.of()));
			assertThat(TitleCleaningTest.this.cleaning.withoutSeniorityWords("Seniority Researcher"))
				.isEqualTo(new CleanedTitle("Seniority Researcher", Set.of()));
		}

	}

}
