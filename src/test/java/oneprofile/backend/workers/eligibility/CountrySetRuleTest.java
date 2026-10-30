package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import oneprofile.backend.storage.normalizedvacancy.CountrySet;
import oneprofile.backend.storage.normalizedvacancy.CountrySetVacancy;
import oneprofile.backend.storage.normalizedvacancy.LocationListEnum;
import oneprofile.backend.storage.normalizedvacancy.LocationPlaces;
import oneprofile.backend.storage.normalizedvacancy.Place;
import oneprofile.backend.storage.normalizedvacancy.Segment;
import oneprofile.backend.storage.normalizedvacancy.SegmentKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSet;
import oneprofile.backend.storage.normalizedvacancy.StatedSetKindEnum;
import oneprofile.backend.storage.normalizedvacancy.StatedSetSourceEnum;
import oneprofile.backend.storage.normalizedvacancy.WorkModeEnum;
import oneprofile.backend.storage.placelist.PlaceListConfiguration;
import oneprofile.backend.workers.cleaning.TitleCleaningRule;
import oneprofile.backend.workers.normalization.WorkModeRule;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

class CountrySetRuleTest {

	private static final PlaceRule PLACES = new PlaceRule(PlaceListConfiguration.shipped());

	private final CountrySetRule rule = new CountrySetRule(PLACES, new WorkModeRule(new TitleCleaningRule()));

	@Test
	void aLocationStringIsAnOptionOfTheVacancysWorkMode() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of("Remote - US")));

		assertThat(set.statedSets()).containsExactly(new StatedSet(0, StatedSetSourceEnum.LOCATION,
				StatedSetKindEnum.LOCATION, WorkModeEnum.REMOTE, null, "Remote - US", List.of(Place.country("US")),
				null, null));
	}

	@Test
	void aLocationStringWithoutAWorkModeKeepsTheVacancyUndeclared() {
		CountrySet set = this.rule.read(vacancy(null, "Software Engineer", List.of("Berlin, Germany")));

		assertThat(set.statedSets()).singleElement().satisfies((option) -> {
			assertThat(option.workMode()).isNull();
			assertThat(option.places()).extracting(Place::city).containsExactly("Berlin");
		});
	}

	@Test
	void aLocationStringNamingARegionIsOfKindRegion() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of("Remote - EMEA")));

		assertThat(set.statedSets()).extracting(StatedSet::kind).containsExactly(StatedSetKindEnum.REGION);
	}

	@Test
	void aLocationStringOfferingSeveralWorkModesIsReadOptionByOption() {
		CountrySet set = this.rule
			.read(vacancy(WorkModeEnum.HYBRID, "Software Engineer", List.of("NYC (Hybrid); United States (Remote)")));

		assertThat(set.statedSets()).extracting(StatedSet::text, StatedSet::workMode)
			.containsExactly(Tuple.tuple("NYC (Hybrid)", WorkModeEnum.HYBRID),
					Tuple.tuple("United States (Remote)", WorkModeEnum.REMOTE));
		assertThat(set.statedSets().get(0).places()).extracting(Place::city).containsExactly("New York City");
		assertThat(set.statedSets().get(1).places()).containsExactly(Place.country("US"));
		assertThat(set.statedSets()).extracting(StatedSet::position).containsExactly(0, 1);
	}

	@Test
	void aRegionTheTitleNamesIsAStatedSet() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE,
				"Full Stack Laravel + React Developer - Remote, Latin America", List.of("Remote")));

		assertThat(set.statedSets()).filteredOn((stated) -> stated.source() == StatedSetSourceEnum.TITLE)
			.singleElement()
			.satisfies((stated) -> {
				assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.REGION);
				assertThat(stated.places()).containsExactly(Place.region("Latin America"));
				assertThat(stated.text()).isEqualTo("Full Stack Laravel + React Developer - Remote, Latin America");
			});
	}

	@Test
	void aCountryTheTitleNamesIsAStatedSet() {
		CountrySet set = this.rule.read(vacancy(null, "SDET - API/Middleware (India)", List.of()));

		assertThat(set.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.source()).isEqualTo(StatedSetSourceEnum.TITLE);
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.LOCATION);
			assertThat(stated.places()).containsExactly(Place.country("IN"));
		});
	}

	@Test
	void aCityOrAWordOfTheTitleIsNoStatedSet() {
		assertThat(this.rule.read(vacancy(null, "Software Engineer, Mobile", List.of())).statedSets()).isEmpty();
		assertThat(this.rule.read(vacancy(null, "Cleared Penetration Tester - Seattle", List.of())).statedSets())
			.extracting(StatedSet::kind)
			.containsExactly(StatedSetKindEnum.CLEARANCE);
	}

	@Test
	void aCodeInTheTitleIsAPlaceOnlyWhereTitlesUseItForOne() {
		assertThat(this.rule.read(vacancy(null, "Staff Software Engineer - Reference Data - AI/ML (UK)", List.of()))
			.statedSets()).singleElement().satisfies((stated) -> assertThat(stated.places())
				.containsExactly(Place.country("GB")));
		assertThat(this.rule.read(vacancy(null, "Applied AI Software Engineer, GTM", List.of())).statedSets()).isEmpty();
		assertThat(this.rule.read(vacancy(null, "Lead Cloud DevOps Engineer (Oakland, CA Office)", List.of()))
			.statedSets()).isEmpty();
		assertThat(this.rule.read(vacancy(null, "Senior Software Engineer (Hybrid - US)", List.of())).statedSets())
			.singleElement().satisfies((stated) -> assertThat(stated.places()).containsExactly(Place.country("US")));
	}

	@Test
	void aClearanceInTheTitleIsASetOfTheUnitedStates() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.ONSITE, "Cloud Engineer - TS/SCI w/ Poly",
				List.of("Herndon, VA")));

		assertThat(set.statedSets()).filteredOn((stated) -> stated.source() == StatedSetSourceEnum.TITLE)
			.singleElement()
			.satisfies((stated) -> {
				assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.CLEARANCE);
				assertThat(stated.places()).containsExactly(Place.country("US"));
			});
		assertThat(this.rule.read(vacancy(null, "Platform Engineer, Secrets Management", List.of())).statedSets())
			.isEmpty();
	}

	@Test
	void aDescriptionStatesWhereItsCandidatesMustBe() {
		assertThat(described("This role is remote, but candidates must be based in Poland."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("PL")));
		assertThat(described("This remote role is open only to candidates residing in Canada."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("CA")));
		assertThat(described("Remote (US only)")).containsExactly(stated(StatedSetKindEnum.LOCATION,
				Place.country("US")));
		assertThat(described("Candidates must be located in EMEA.")).containsExactly(stated(StatedSetKindEnum.REGION,
				Place.region("EMEA")));
		assertThat(described("Employees may live anywhere in Mexico, with travel to Mexico City."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("MX")));
		assertThat(described("We are remote-first, so you may work from anywhere you like in the U.S."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
	}

	@Test
	void contractorAndEmployerOfRecordHiringStatesWhere() {
		assertThat(described("We hire contractors in Latin America.")).containsExactly(stated(StatedSetKindEnum.REGION,
				Place.region("Latin America")));
		assertThat(described("We employ through an employer of record in Brazil."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("BR")));
	}

	@Test
	void aDescriptionMayHireAnywhere() {
		assertThat(described("Fully remote work: work remotely from anywhere in the world."))
			.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
	}

	@Test
	void aPlaceTheDescriptionMentionsIsNoStatedSet() {
		assertThat(described("Our vision is to reimagine the way people come together, from anywhere in the world."))
			.isEmpty();
		assertThat(described("For candidates based in the United States, the pay range for this position is below."))
			.isEmpty();
		assertThat(described("We are not currently able to hire in Quebec.")).isEmpty();
		assertThat(described("Employees must live within a reasonable commuting distance of the office.")).isEmpty();
		assertThat(described("We have offices in London, Berlin and New York.")).isEmpty();
		assertThat(described("Hire only people who can raise the average.")).isEmpty();
	}

	@Test
	void workingFromAnywhereForSomeDaysAYearIsABenefitAndNoStatedSet() {
		assertThat(described("Work from anywhere in the world for up to 60 days per year.")).isEmpty();
		assertThat(described("4 weeks Work From Anywhere per year")).isEmpty();
		assertThat(described("Hybrid roles have the flexibility to work from anywhere for up to 4 weeks.")).isEmpty();
		assertThat(described("Flexible PTO and the freedom to work from anywhere in the world for up to a month."))
			.isEmpty();
	}

	@Test
	void aStatementsPlaceEndsAtAnException() {
		assertThat(described("This role is open to candidates residing in the US except the NYC Metro Area."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
	}

	@Test
	void workAuthorizationIsASetOfItsCountry() {
		assertThat(described("Applicants must be authorized to work in the U.S. without the need for sponsorship."))
			.containsExactly(stated(StatedSetKindEnum.AUTHORIZATION, Place.country("US")));
		assertThat(described("To be eligible for this role you are required to have the appropriate right to work in the UK."))
			.containsExactly(stated(StatedSetKindEnum.AUTHORIZATION, Place.country("GB")));
		assertThat(described("You must currently possess valid and unrestricted U.S. work authorization."))
			.containsExactly(stated(StatedSetKindEnum.AUTHORIZATION, Place.country("US")));
		assertThat(described("Applicants must be authorized to work in the country in which they apply.")).isEmpty();
		assertThat(described("We use E-Verify to confirm that you are authorized to work in the U.S.")).isEmpty();
	}

	@Test
	void workAuthorizationForSomeOfTheCompanysRolesIsNoStatedSet() {
		assertThat(described("Applicants for employment in the US must have work authorization that does not now or "
				+ "in the future require sponsorship of a visa for employment authorization in the United States."))
			.isEmpty();
		assertThat(described("For US based roles only, please note the Company may not be able to employ candidates "
				+ "for this role who have United States work authorization related to certain U.S. visa categories."))
			.isEmpty();
	}

	@Test
	void aSponsorshipPolicyIsNoStatedSet() {
		assertThat(described("U.S. work authorization sponsorship (H-1B, TN, J, etc.) is offered exclusively for "
				+ "Principal-level roles and above, where specialized expertise aligns with long-term business needs."))
			.isEmpty();
	}

	@Test
	void citizenshipIsASetOfItsCountry() {
		assertThat(described("US Citizenship Required")).containsExactly(stated(StatedSetKindEnum.CITIZENSHIP,
				Place.country("US")));
		assertThat(described("Under ITAR, the applicant must be a U.S. citizen or lawful permanent resident."))
			.containsExactly(stated(StatedSetKindEnum.CITIZENSHIP, Place.country("US")));
		assertThat(described("We hire without regard to race, national origin, citizenship status or age.")).isEmpty();
		assertThat(described("Employment is verified with the U.S. Citizenship and Immigration Services.")).isEmpty();
	}

	@Test
	void aClearanceTheDescriptionRequiresIsASetOfItsCountry() {
		assertThat(described("Must have an active TS/SCI clearance with polygraph."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("US")));
		assertThat(described("This position does not require a security clearance.")).isEmpty();
		assertThat(described("U.S. Citizenship is required for all positions with a government clearance and certain other restricted positions."))
			.isEmpty();
	}

	@Test
	void aClearanceLabelIsReadWithItsAnswer() {
		assertThat(clearances("Government Clearance Required?", "No")).isEmpty();
		assertThat(clearances("Government Clearance Required?", "Job Posting End Date")).isEmpty();
		assertThat(clearances("Government Clearance Required?", "Yes")).containsExactly(0);
		assertThat(clearances("Clearance Required", "None")).isEmpty();
		assertThat(clearances("Minimum Clearance Required to Start:", "TS/SCI with Poly")).containsExactly(0);
		assertThat(clearances("CLEARANCE REQUIRED FOR START: No")).isEmpty();
		assertThat(clearances("Minimum Clearance Required to Start: None")).isEmpty();
		assertThat(clearances("Minimum Clearance Required to Start: TS/SCI")).containsExactly(0);
	}

	private List<Integer> clearances(String... description) {
		return this.rule.read(vacancy(null, "Software Engineer", List.of(), description))
			.statedSets()
			.stream()
			.filter((stated) -> stated.kind() == StatedSetKindEnum.CLEARANCE)
			.map(StatedSet::segment)
			.toList();
	}

	@Test
	void aTimezoneRequirementIsAWindowOfOffsets() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(),
				"You live in a timezone between UTC -6 to UTC+2 (North American Central Time through Central European)."));

		assertThat(set.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.TIMEZONE);
			assertThat(stated.utcFrom()).isEqualTo(-360);
			assertThat(stated.utcTo()).isEqualTo(120);
			assertThat(stated.segment()).isZero();
			assertThat(stated.places()).isEmpty();
		});
		assertThat(timezones("We welcome remote developers working from UTC–1 to UTC+4.")).containsExactly(-60, 240);
		assertThat(timezones("Flexible work remotely within time zones from GMT-2 to GMT+5:30.")).containsExactly(-120, 330);
		assertThat(timezones("You must live between UTC-5 and UTC+1; core hours are 10 to 4.")).containsExactly(-300, 60);
	}

	@Test
	void aScheduleIsNoTimezoneRequirement() {
		assertThat(timezones("Daily collaboration overlap with colleagues in European time zones ranging from UTC to UTC+3."))
			.isEmpty();
		assertThat(timezones("Able to attend regular meetings between 2 and 8 pm UTC.")).isEmpty();
		assertThat(timezones("Preferred time zone between UTC-8 and UTC+2")).isEmpty();
		assertThat(timezones("Team time zone: The team works within the GMT+1 to GMT+5 time zone")).isEmpty();
	}

	@Test
	void aDescriptionMayOfferVisaSponsorshipOrRelocation() {
		assertThat(offers("Visa sponsorship is available for this position.")).containsExactly(true, false);
		assertThat(offers("We offer a relocation package to Berlin.")).containsExactly(false, true);
		assertThat(offers("For certain roles, we can help with relocation from anywhere in the world."))
			.containsExactly(false, true);
		assertThat(offers("We are unable to sponsor visas for this role.")).containsExactly(false, false);
		assertThat(offers("Visa sponsorship is not available. No relocation assistance is provided."))
			.containsExactly(false, false);
	}

	@Test
	void aStatedSetKeepsTheSegmentThatStatedIt() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(),
				"We build tools.", "Candidates must be based in Spain."));

		assertThat(set.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.segment()).isEqualTo(1);
			assertThat(stated.text()).isEqualTo("Candidates must be based in Spain.");
			assertThat(stated.source()).isEqualTo(StatedSetSourceEnum.DESCRIPTION);
		});
	}

	private List<Tuple> described(String text) {
		return this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(), text))
			.statedSets()
			.stream()
			.map((stated) -> stated(stated.kind(), stated.places().toArray(Place[]::new)))
			.toList();
	}

	private static Tuple stated(StatedSetKindEnum kind, Place... places) {
		return Tuple.tuple(kind, List.of(places));
	}

	private List<Integer> timezones(String text) {
		return this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(), text))
			.statedSets()
			.stream()
			.filter((stated) -> stated.kind() == StatedSetKindEnum.TIMEZONE)
			.flatMap((stated) -> Stream.of(stated.utcFrom(), stated.utcTo()))
			.toList();
	}

	private List<Boolean> offers(String text) {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(), text));
		return List.of(set.offersVisaSponsorship(), set.offersRelocation());
	}

	private static CountrySetVacancy vacancy(WorkModeEnum workMode, String title, List<String> locations,
			String... description) {
		List<LocationPlaces> located = new ArrayList<>();
		for (int i = 0; i < locations.size(); i++) {
			located.add(new LocationPlaces(LocationListEnum.LOCATION, i, locations.get(i),
					PLACES.read(locations.get(i), null).places()));
		}
		List<Segment> segments = new ArrayList<>();
		for (String text : description) {
			segments.add(new Segment(SegmentKindEnum.SENTENCE, null, text, false));
		}
		return new CountrySetVacancy(1, workMode, title, located, segments);
	}

}
