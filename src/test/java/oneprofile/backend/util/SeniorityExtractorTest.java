package oneprofile.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.model.Seniority;
import oneprofile.backend.util.SeniorityExtractor.Extracted;

import org.junit.jupiter.api.Test;

/**
 * Every title here is a real one, taken from the measurements run against the 128.953
 * vacancies of the database; none was made up. They go in raw, through the cleanup
 * first, because that is the order the normalization runs in.
 */
class SeniorityExtractorTest {

	@Test
	void collapsesTheVariantsOfTheSameJob() {
		// The two of them are how one single job reaches us written by two boards.
		assertThat(normalize("Sr. Software Engineer - Backend"))
				.isEqualTo(new Extracted("software engineer backend", Seniority.SENIOR));
		assertThat(normalize("Senior Software Engineer (Backend)"))
				.isEqualTo(new Extracted("software engineer backend", Seniority.SENIOR));
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
		assertThat(normalize("Director of Engineering")).isEqualTo(new Extracted("director of engineering", null));
		assertThat(normalize("Lead Principal Data Engineer"))
				.isEqualTo(new Extracted("lead data engineer", Seniority.PRINCIPAL));
	}

	@Test
	void entryIsALevelOnlyNextToLevel() {
		assertThat(normalize("Entry Level Civil Engineer")).isEqualTo(new Extracted("civil engineer", Seniority.ENTRY));
		assertThat(normalize("Field Maintenance Technician Entry Level"))
				.isEqualTo(new Extracted("field maintenance technician", Seniority.ENTRY));
	}

	@Test
	void entryIsADoorOrADeskTheRestOfTheTime() {
		assertThat(normalize("Entry Door Technician")).isEqualTo(new Extracted("entry door technician", null));
		assertThat(normalize("Data Entry Specialist")).isEqualTo(new Extracted("data entry specialist", null));
	}

	@Test
	void midIsALevelNextToLevelOrClosingTheTitle() {
		assertThat(normalize("Mid Level Software Engineer")).isEqualTo(new Extracted("software engineer", Seniority.MID));
		assertThat(normalize("Backend Engineer Mid")).isEqualTo(new Extracted("backend engineer", Seniority.MID));
	}

	@Test
	void midIsAMarketSegmentOrARegionTheRestOfTheTime() {
		// 300 of the 566 "mid" are this one, and another 25 are the region.
		assertThat(normalize("Mid Market Account Executive"))
				.isEqualTo(new Extracted("mid market account executive", null));
		assertThat(normalize("Enterprise Account Executive Mid Atlantic"))
				.isEqualTo(new Extracted("enterprise account executive mid atlantic", null));
	}

	@Test
	void staffIsALevelInFrontOfTheJob() {
		assertThat(normalize("Staff Software Engineer")).isEqualTo(new Extracted("software engineer", Seniority.STAFF));
	}

	@Test
	void staffIsThePeopleOfAPlaceTheRestOfTheTime() {
		assertThat(normalize("Chief of Staff")).isEqualTo(new Extracted("chief of staff", null));
		assertThat(normalize("Member of Technical Staff, AI"))
				.isEqualTo(new Extracted("member of technical staff ai", null));
		assertThat(normalize("Administrative Assistant External Agency Staff"))
				.isEqualTo(new Extracted("administrative assistant external agency staff", null));
		assertThat(normalize("Staff Nurse Acute Care")).isEqualTo(new Extracted("staff nurse acute care", null));
		assertThat(normalize("Corporate Staff Accountant")).isEqualTo(new Extracted("corporate staff accountant", null));
	}

	@Test
	void readsTheSemiSeniorOfTheSpanishSpeakingMarket() {
		assertThat(normalize("Analista QA Funcional Semi Senior"))
				.isEqualTo(new Extracted("analista qa funcional", Seniority.SEMI_SENIOR));
		assertThat(normalize("Semi Senior Data Engineer")).isEqualTo(new Extracted("data engineer", Seniority.SEMI_SENIOR));
		assertThat(normalize("Backend Developer Semisenior"))
				.isEqualTo(new Extracted("backend developer", Seniority.SEMI_SENIOR));
		assertThat(normalize("SSr Data Scientist")).isEqualTo(new Extracted("data scientist", Seniority.SEMI_SENIOR));
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
		assertThat(normalize(null)).isEqualTo(new Extracted(null, null));
		assertThat(normalize("")).isEqualTo(new Extracted(null, null));
		assertThat(normalize(" --- !!! ")).isEqualTo(new Extracted(null, null));
		assertThat(normalize("Senior")).isEqualTo(new Extracted(null, Seniority.SENIOR));
	}

	private static Extracted normalize(String raw) {
		return SeniorityExtractor.extract(TitleCleaner.clean(raw));
	}

	private static Seniority seniority(String raw) {
		return normalize(raw).seniority();
	}
}
