package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.model.Seniority;
import oneprofile.backend.util.TitleNormalizer.NormalizedTitle;

import org.junit.jupiter.api.Test;

/**
 * Every title here is a real one, taken from the measurements run against the 128.953
 * vacancies of the database; none was made up.
 */
class TitleNormalizerTest {

	@Test
	void keepsTheCharactersThatAreLanguages() {
		assertThat(title("Senior Software Engineer, C++")).isEqualTo("software engineer c++");
		assertThat(title("Software Engineer (C#)")).isEqualTo("software engineer c#");
		assertThat(title("1535 Staff Backend Engineer .NET")).isEqualTo("1535 backend engineer .net");
		assertThat(title("Node.js Developer")).isEqualTo("node.js developer");
		assertThat(title("Avionics R&D Technician")).isEqualTo("avionics r&d technician");
	}

	@Test
	void dropsThoseSameCharactersWhereTheyAreJustPunctuation() {
		assertThat(title("Software Engineer.")).isEqualTo("software engineer");
		assertThat(title("#hiring Product Designer")).isEqualTo("hiring product designer");
		assertThat(title("Sales & Marketing Coordinator")).isEqualTo("sales marketing coordinator");
	}

	@Test
	void dropsTheApostropheWithoutLeavingAHole() {
		assertThat(title("Women's Health Nurse")).isEqualTo("womens health nurse");
	}

	@Test
	void collapsesTheVariantsOfTheSameJob() {
		// The three of them are how one single job reaches us written by three boards.
		assertThat(TitleNormalizer.normalize("Sr. Software Engineer - Backend"))
				.isEqualTo(new NormalizedTitle("software engineer backend", Seniority.SENIOR));
		assertThat(TitleNormalizer.normalize("Senior Software Engineer (Backend)"))
				.isEqualTo(new NormalizedTitle("software engineer backend", Seniority.SENIOR));
	}

	@Test
	void dropsTheDiacriticsSoTheAccentedAndThePlainMeet() {
		assertThat(title("Aide à domicile (H/F)")).isEqualTo("aide a domicile h f");
		assertThat(title("Ingeniería de Datos")).isEqualTo("ingenieria de datos");
	}

	@Test
	void keepsTitlesThatAreNotWrittenInLatinLetters() {
		assertThat(title("쿠팡 Sr Employee Relation Specialist")).isEqualTo("쿠팡 employee relation specialist");
	}

	@Test
	void takesTheLevelsThatNeedNoGuard() {
		assertThat(seniority("Sr Data Engineer")).isEqualTo(Seniority.SENIOR);
		assertThat(seniority("Junior Software Engineer")).isEqualTo(Seniority.JUNIOR);
		assertThat(seniority("Jr Sous Chef")).isEqualTo(Seniority.JUNIOR);
		assertThat(seniority("Principal Solutions Architect")).isEqualTo(Seniority.PRINCIPAL);
		assertThat(seniority("Software Engineer II")).isEqualTo(Seniority.LEVEL_2);
		assertThat(seniority("Maintenance Technician III")).isEqualTo(Seniority.LEVEL_3);
	}

	@Test
	void leavesTheHierarchicalRolesInsideTheTitle() {
		// The role is the job: an engineering director is not an engineer.
		assertThat(TitleNormalizer.normalize("Director of Engineering"))
				.isEqualTo(new NormalizedTitle("director of engineering", null));
		assertThat(TitleNormalizer.normalize("Lead Principal Data Engineer"))
				.isEqualTo(new NormalizedTitle("lead data engineer", Seniority.PRINCIPAL));
	}

	@Test
	void entryIsALevelOnlyNextToLevel() {
		assertThat(TitleNormalizer.normalize("Entry Level Civil Engineer"))
				.isEqualTo(new NormalizedTitle("civil engineer", Seniority.ENTRY));
		assertThat(TitleNormalizer.normalize("Field Maintenance Technician Entry Level"))
				.isEqualTo(new NormalizedTitle("field maintenance technician", Seniority.ENTRY));
	}

	@Test
	void entryIsADoorOrADeskTheRestOfTheTime() {
		assertThat(TitleNormalizer.normalize("Entry Door Technician"))
				.isEqualTo(new NormalizedTitle("entry door technician", null));
		assertThat(TitleNormalizer.normalize("Data Entry Specialist"))
				.isEqualTo(new NormalizedTitle("data entry specialist", null));
	}

	@Test
	void midIsALevelNextToLevelOrClosingTheTitle() {
		assertThat(TitleNormalizer.normalize("Mid Level Software Engineer"))
				.isEqualTo(new NormalizedTitle("software engineer", Seniority.MID));
		assertThat(TitleNormalizer.normalize("Backend Engineer Mid"))
				.isEqualTo(new NormalizedTitle("backend engineer", Seniority.MID));
	}

	@Test
	void midIsAMarketSegmentOrARegionTheRestOfTheTime() {
		// 300 of the 566 "mid" are this one, and another 25 are the region.
		assertThat(TitleNormalizer.normalize("Mid Market Account Executive"))
				.isEqualTo(new NormalizedTitle("mid market account executive", null));
		assertThat(TitleNormalizer.normalize("Enterprise Account Executive Mid Atlantic"))
				.isEqualTo(new NormalizedTitle("enterprise account executive mid atlantic", null));
	}

	@Test
	void staffIsALevelInFrontOfTheJob() {
		assertThat(TitleNormalizer.normalize("Staff Software Engineer"))
				.isEqualTo(new NormalizedTitle("software engineer", Seniority.STAFF));
	}

	@Test
	void staffIsThePeopleOfAPlaceTheRestOfTheTime() {
		assertThat(TitleNormalizer.normalize("Chief of Staff"))
				.isEqualTo(new NormalizedTitle("chief of staff", null));
		assertThat(TitleNormalizer.normalize("Member of Technical Staff, AI"))
				.isEqualTo(new NormalizedTitle("member of technical staff ai", null));
		assertThat(TitleNormalizer.normalize("Administrative Assistant External Agency Staff"))
				.isEqualTo(new NormalizedTitle("administrative assistant external agency staff", null));
		assertThat(TitleNormalizer.normalize("Staff Nurse Acute Care"))
				.isEqualTo(new NormalizedTitle("staff nurse acute care", null));
		assertThat(TitleNormalizer.normalize("Corporate Staff Accountant"))
				.isEqualTo(new NormalizedTitle("corporate staff accountant", null));
	}

	@Test
	void readsTheSemiSeniorOfTheSpanishSpeakingMarket() {
		assertThat(TitleNormalizer.normalize("Analista QA Funcional Semi Senior"))
				.isEqualTo(new NormalizedTitle("analista qa funcional", Seniority.SEMI_SENIOR));
		assertThat(TitleNormalizer.normalize("Semi Senior Data Engineer"))
				.isEqualTo(new NormalizedTitle("data engineer", Seniority.SEMI_SENIOR));
		assertThat(TitleNormalizer.normalize("Backend Developer Semisenior"))
				.isEqualTo(new NormalizedTitle("backend developer", Seniority.SEMI_SENIOR));
		assertThat(TitleNormalizer.normalize("SSr Data Scientist"))
				.isEqualTo(new NormalizedTitle("data scientist", Seniority.SEMI_SENIOR));
	}

	@Test
	void takesTheLowestWhenTheTitleNamesMoreThanOne() {
		// A vacancy naming several levels is publishing the floor it accepts.
		assertThat(seniority("Senior Staff Engineer")).isEqualTo(Seniority.SENIOR);
		assertThat(seniority("Controls Engineer All Levels Junior to Senior")).isEqualTo(Seniority.JUNIOR);
		assertThat(seniority("Backend Software Engineer Mid Senior")).isEqualTo(Seniority.MID);
		assertThat(seniority("Machine Learning Engineer Staff Principal")).isEqualTo(Seniority.STAFF);
		assertThat(seniority("Automation Engineer I II III")).isEqualTo(Seniority.LEVEL_2);
	}

	@Test
	void aWordBeatsANumberBecauseTheNumberSaysNothingAboutTheScale() {
		assertThat(seniority("Senior Account Executive II")).isEqualTo(Seniority.SENIOR);
	}

	@Test
	void givesNullWhenThereIsNoTitleLeft() {
		assertThat(TitleNormalizer.normalize(null)).isEqualTo(new NormalizedTitle(null, null));
		assertThat(TitleNormalizer.normalize("")).isEqualTo(new NormalizedTitle(null, null));
		assertThat(TitleNormalizer.normalize(" --- !!! ")).isEqualTo(new NormalizedTitle(null, null));
		assertThat(TitleNormalizer.normalize("Senior")).isEqualTo(new NormalizedTitle(null, Seniority.SENIOR));
	}

	private static String title(String raw) {
		return TitleNormalizer.normalize(raw).title();
	}

	private static Seniority seniority(String raw) {
		return TitleNormalizer.normalize(raw).seniority();
	}
}
