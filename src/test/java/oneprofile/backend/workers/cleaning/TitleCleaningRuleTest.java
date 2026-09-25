package oneprofile.backend.workers.cleaning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import oneprofile.backend.storage.normalizedvacancy.CleanedTitle;
import oneprofile.backend.storage.normalizedvacancy.SeniorityLevelEnum;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TitleCleaningRuleTest {

	private final TitleCleaningRule cleaning = new TitleCleaningRule();

	@Test
	void takesEveryKindOfNoiseOutOfOneTitle() {
		assertThat(this.cleaning.clean("Senior Software Engineer (m/w/d), Payments"))
			.isEqualTo(new CleanedTitle("Software Engineer Payments", Set.of(SeniorityLevelEnum.SENIOR)));
	}

	@Test
	void leavesNothingOfACharacterWhoseWordIsGone() {
		assertThat(this.cleaning.clean("Senior+ Applied Scientist"))
			.isEqualTo(new CleanedTitle("Applied Scientist", Set.of(SeniorityLevelEnum.SENIOR)));
		assertThat(this.cleaning.clean("Principal/ Sr.Principal Digital Design Engineer"))
			.isEqualTo(new CleanedTitle("Digital Design Engineer",
					Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL)));
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
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Electrical Engineer (m/f/d)"))
				.isEqualTo("Electrical Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Ingénieur Logiciel (H/F)")).isEqualTo("Ingénieur Logiciel");
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("FSQA Manager - Processed Category (f/m/x)"))
				.isEqualTo("FSQA Manager - Processed Category");
		}

		@Test
		void removesAMarkerThatWasWrittenWithoutBrackets() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("IT-Consultant m/f/d")).isEqualTo("IT-Consultant");
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Stage Software Engineer - Paris - H/F/X"))
				.isEqualTo("Stage Software Engineer - Paris -");
		}

		@Test
		void removesAMarkerFromInsideATitle() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Automation Engineer (m/f/d) PLC & Commissioning"))
				.isEqualTo("Automation Engineer PLC & Commissioning");
		}

		@Test
		void leavesAloneTheSlashedLettersThatNameSomethingElse() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Flight Software Engineer, Embedded C/C++"))
				.isEqualTo("Flight Software Engineer, Embedded C/C++");
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Patient Care Technician - 4 Medical - FT - D/N"))
				.isEqualTo("Patient Care Technician - 4 Medical - FT - D/N");
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutGenderMarkers("Engineer I/II")).isEqualTo("Engineer I/II");
		}

	}

	@Nested
	class CharacterWhitelist {

		@Test
		void keepsOnlyLettersDigitsAndWhatCarriesAMeaning() {
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Backend Engineer, Payments — Remote!"))
				.isEqualTo("Backend Engineer Payments Remote");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Front-End Engineer")).isEqualTo("Front End Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("  Engineer   II  ")).isEqualTo("Engineer II");
		}

		@Test
		void keepsTheCharacterThatCarriesATechnology() {
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted(".NET Developer")).isEqualTo(".NET Developer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Node.js Engineer")).isEqualTo("Node.js Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("C# Engineer")).isEqualTo("C# Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("C++ Engineer")).isEqualTo("C++ Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("R&D Engineer")).isEqualTo("R&D Engineer");
		}

		@Test
		void dropsThoseSameCharactersWhereTheyCarryNothing() {
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Sales & Marketing Lead")).isEqualTo("Sales Marketing Lead");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Engineer.")).isEqualTo("Engineer");
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("Engineer (#1 team)")).isEqualTo("Engineer 1 team");
		}

		@Test
		void leavesATitleWrittenInAnotherScriptReadable() {
			assertThat(TitleCleaningRuleTest.this.cleaning.whitelisted("백엔드 엔지니어 (신입)")).isEqualTo("백엔드 엔지니어 신입");
		}

	}

	@Nested
	class SeniorityWords {

		@Test
		void extractsAndRemovesTheWordsThatNameALevelWhereverTheyAppear() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Senior Backend Engineer"))
				.isEqualTo(new CleanedTitle("Backend Engineer", Set.of(SeniorityLevelEnum.SENIOR)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Sr. Data Analyst"))
				.isEqualTo(new CleanedTitle("Data Analyst", Set.of(SeniorityLevelEnum.SENIOR)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Sr.Data Analyst"))
				.isEqualTo(new CleanedTitle("Data Analyst", Set.of(SeniorityLevelEnum.SENIOR)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Junior Developer"))
				.isEqualTo(new CleanedTitle("Developer", Set.of(SeniorityLevelEnum.JUNIOR)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Jr Developer"))
				.isEqualTo(new CleanedTitle("Developer", Set.of(SeniorityLevelEnum.JUNIOR)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Principal Engineer"))
				.isEqualTo(new CleanedTitle("Engineer", Set.of(SeniorityLevelEnum.PRINCIPAL)));
		}

		@Test
		void readsSemiSeniorAsMid() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Semi Senior QA Engineer"))
				.isEqualTo(new CleanedTitle("QA Engineer", Set.of(SeniorityLevelEnum.MID)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Semi-Senior QA Engineer"))
				.isEqualTo(new CleanedTitle("QA Engineer", Set.of(SeniorityLevelEnum.MID)));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("SSR Frontend Developer"))
				.isEqualTo(new CleanedTitle("Frontend Developer", Set.of(SeniorityLevelEnum.MID)));
		}

		@Test
		void keepsEveryLevelATitleNames() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Senior Principal Engineer"))
				.isEqualTo(new CleanedTitle("Engineer", Set.of(SeniorityLevelEnum.SENIOR, SeniorityLevelEnum.PRINCIPAL)));
		}

		@Test
		void namesALevelOnceHoweverOftenTheTitleSaysIt() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Senior Engineer, Senior Platform"))
				.isEqualTo(new CleanedTitle("Engineer, Platform", Set.of(SeniorityLevelEnum.SENIOR)));
		}

		@Test
		void leavesInTheWordsThatNeedAReadingOfTheJob() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Staff Engineer II"))
				.isEqualTo(new CleanedTitle("Staff Engineer II", Set.of()));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Mid Level Entry Engineer"))
				.isEqualTo(new CleanedTitle("Mid Level Entry Engineer", Set.of()));
		}

		@Test
		void leavesAloneAWordThatMerelyStartsWithOne() {
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("SRE Manager"))
				.isEqualTo(new CleanedTitle("SRE Manager", Set.of()));
			assertThat(TitleCleaningRuleTest.this.cleaning.withoutSeniorityWords("Seniority Researcher"))
				.isEqualTo(new CleanedTitle("Seniority Researcher", Set.of()));
		}

	}

}
