package oneprofile.backend.workers.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
	void aLocationStringWithoutAWorkModeIsAnOptionOfEachModeAVacancyOfferingAChoiceOffers() {
		CountrySet set = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.HYBRID, WorkModeEnum.REMOTE),
				"Software Engineer", List.of(new LocationPlaces(LocationListEnum.LOCATION, 0, "Berlin, Germany",
						PLACES.read("Berlin, Germany", null).places())), List.of()));

		assertThat(set.statedSets()).extracting(StatedSet::text, StatedSet::workMode, StatedSet::position)
			.containsExactly(Tuple.tuple("Berlin, Germany", WorkModeEnum.REMOTE, 0),
					Tuple.tuple("Berlin, Germany", WorkModeEnum.HYBRID, 1));
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

	/** The person's stated location is required without must. */
	@Test
	void aDescriptionStatesWhereYouAreBased() {
		for (String text : List.of("You are based in the US", "You're currently located in the US",
				"You’re primarily based within the US", "You will be located in the US")) {
			assertThat(described(text)).as(text)
				.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
		}
		for (String text : List.of("you'll be based within the EMEA region", "you’ll be located in EMEA")) {
			assertThat(described(text)).as(text)
				.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("EMEA")));
		}
		assertThat(described("If you are based in the US, the pay range is below.")).isEmpty();
		assertThat(described("For candidates located in the US, the pay range is below.")).isEmpty();
		assertThat(described("You will not be based in the US.")).isEmpty();
	}

	/** A statement of where this role is done needs no must. */
	@Test
	void aDescriptionStatesWhereTheRoleIsDone() {
		assertCityStatement("This role is based at our Oxford, England (UK) office", "GB", "Oxford");
		assertCityStatement("This role is based in our San Francisco office", "US", "San Francisco");
		assertCityStatement("This position is located in Vilnius", "LT", "Vilnius");
		assertCityStatement("THIS POSITION IS BASED IN BANGKOK", "TH", "Bangkok");
		assertCityStatement("This hybrid position will be located in Latina, Italy", "IT", "Latina");
		assertCityStatement("This role is based in either our Nottingham or London offices", "GB", "Nottingham");
		assertCityStatement("This job will be primarily located out of London", "GB", "London");
		assertThat(described("a remote-first role based in the U.S."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
		assertThat(described("This opportunity is based within Canada"))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("CA")));
		assertThat(described("This role is not based in the US.")).isEmpty();
		assertThat(described("This role is preferably based in the US.")).isEmpty();
	}

	/** Where the person will work is a statement, including work without be. */
	@Test
	void aDescriptionStatesWhereYouWillWork() {
		assertThat(described("You'll work in Hyvinkää, Finland"))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("FI")));
		assertCityStatement("You’ll be working from London", "GB", "London");
		assertCityStatement("You will work out of Vilnius", "LT", "Vilnius");
		assertCityStatement("You'll work  in Vilnius", "LT", "Vilnius");
		assertThat(described("You'll work in a fast-paced team")).isEmpty();
		assertThat(described("You'll work from anywhere")).isEmpty();
		assertThat(described("You won't work in the US")).isEmpty();
		assertThat(described("You'll be working in a complex enterprise domain where maintainability matters"))
			.isEmpty();
		assertThat(described("You'll work in Agile teams serving customers in Berlin")).isEmpty();
		assertCityStatement("You'll work in our Vilnius office", "LT", "Vilnius");
	}

	/** A name of places in several countries is read in the one its list, or the vacancy's options, share. */
	@Test
	void aPlaceIsReadInTheCountryItsListOrTheOptionsShare() {
		assertThat(described("This position is open only to candidates residing in Alberta, British Columbia, Ontario"))
			.singleElement()
			.satisfies((stated) -> assertThat(((List<?>) stated.toList().get(1))).extracting("country")
				.containsOnly("CA"));
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of("Remote - Canada"),
				"Candidates must reside in Ontario."));
		assertThat(set.statedSets()).filteredOn((stated) -> stated.source() == StatedSetSourceEnum.DESCRIPTION)
			.singleElement()
			.satisfies((stated) -> assertThat(stated.places()).extracting(Place::country).containsExactly("CA"));
	}

	/** Looking for a person based somewhere states that place, beside a separate perk too. */
	@Test
	void lookingForAPersonBasedSomewhereStatesWhere() {
		assertThat(described("We are looking for a Senior Engineering Manager, Dev Tools based in Portugal."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("PT")));
		assertThat(described("Our partner is looking for a Senior Backend Engineer based in Netherlands"))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("NL")));
		CountrySet set = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
				List.of(), List.of(new Segment(SegmentKindEnum.SENTENCE, null,
						"Our partner is looking for a Data Engineer based in Sweden.", false),
						new Segment(SegmentKindEnum.SENTENCE, "Benefits", "Work from anywhere globally", false))));
		assertThat(set.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.LOCATION);
			assertThat(stated.places()).containsExactly(Place.country("SE"));
		});
	}

	/** Perk headings state no places without hiring or the person's location, but still record offers. */
	@Test
	void aSegmentUnderAPerkHeadingStatesNothingUnlessItNamesHiring() {
		for (String[] example : List.of(
				new String[] { "Perks", "401k retirement plan + company match (US only)" },
				new String[] { "Benefits", "Opportunity to work remotely from Europe" },
				new String[] { "Why join us?", "Work from anywhere in the Americas." },
				new String[] { "What Makes Acme Different?", "Fully Remote: Anywhere in the U.S" })) {
			CountrySet set = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
					List.of(), List.of(new Segment(SegmentKindEnum.SENTENCE, example[0], example[1], false))));
			assertThat(set.statedSets()).as(example[0]).isEmpty();
		}
		CountrySet hiring = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
				List.of(), List.of(new Segment(SegmentKindEnum.SENTENCE, "Benefits",
						"We hire from anywhere in the world.", false))));
		assertThat(hiring.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.REGION);
			assertThat(stated.places()).containsExactly(Place.region("Worldwide"));
		});
		CountrySet separate = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
				List.of(), List.of(new Segment(SegmentKindEnum.SENTENCE, null, "This role is based in Italy.", false),
						new Segment(SegmentKindEnum.SENTENCE, "Benefits", "Opportunity to work remotely from Europe", false))));
		assertThat(separate.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.LOCATION);
			assertThat(stated.places()).containsExactly(Place.country("IT"));
		});
		CountrySet offers = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
				List.of(), List.of(new Segment(SegmentKindEnum.SENTENCE, "Benefits",
						"Visa sponsorship is available. Relocation assistance is available.", false))));
		assertThat(offers.statedSets()).isEmpty();
		assertThat(offers.offersVisaSponsorship()).isTrue();
		assertThat(offers.offersRelocation()).isTrue();
	}

	/** A team's location states nothing, while the person's location still states a place. */
	@Test
	void aDescriptionOfTheTeamStatesNoPlaceForThePerson() {
		for (String text : List.of("Team members working remotely across North America and Europe.",
				"We are looking for an engineer to join our fully remote team based in Europe.",
				"This role joins our team based in Europe.",
				"Our colleagues work anywhere in Brazil.", "Our candidates join a team working from anywhere.")) {
			assertThat(described(text)).as(text).isEmpty();
		}
		assertThat(described("We are looking for an engineer based in Spain to join our team."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("ES")));
		assertThat(described("As a member of our team, you will work remotely from anywhere in the US."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
		assertThat(described("Candidates must be based in the US."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
	}

	/** Location labels state places unless they offer a choice including remote. */
	@Test
	void aDescriptionLocationLabelStatesWhere() {
		assertCityStatement("Location: Bucharest, Romania", "RO", "Bucharest");
		assertThat(described("Location: Remote, US or Canada"))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US"), Place.country("CA")));
		for (String text : List.of("Location: Remote, US", "Job location: US", "Work locations: US", "Role location: US")) {
			assertThat(described(text)).as(text)
				.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US")));
		}
		assertThat(described("Location: San Francisco or Remote")).isEmpty();
		assertThat(described("Location: Remote / NYC")).isEmpty();
		assertThat(described("Our office location: Bucharest, Romania")).isEmpty();
	}

	/** A label keeps its bracketed and semicolon-separated places, but no remote choice or headquarters. */
	@Test
	void aDescriptionLocationLabelReadsItsWholeValue() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(),
				"Location: Prague (Hybrid); Czech Republic (Remote); UK (Remote)"));
		assertThat(set.statedSets()).singleElement().satisfies((stated) -> {
			assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.LOCATION);
			assertThat(stated.places()).extracting(Place::country).containsExactly("CZ", "CZ", "GB");
			assertThat(stated.places().getFirst().city()).isEqualTo("Prague");
		});
		for (String text : List.of("Location: Remote - Headquarters located in Burlington, MA",
				"Location: Remote; NYC", "Location: NYC; Remote", "Location: Remote - HQ in Burlington, MA",
				"Location: Remote - head office in Burlington, MA", "Location: London (HQ) or Remote")) {
			assertThat(described(text)).as(text).isEmpty();
		}
		assertCityStatement("Location: Washington, D.C. Hybrid three days a week.", "US", "Washington");
		assertCityStatement("Location: London\nParis", "GB", "London");
		assertCityStatement("Location: London. Paris", "GB", "London");
	}

	/** Anywhere within named countries also permits other verbs and punctuation after remote. */
	@Test
	void anywhereWithinAPlaceStatesWhere() {
		for (String text : List.of("You may be anywhere in Mexico", "You may reside anywhere in Mexico",
				"Candidates residing anywhere in Mexico", "Work may be performed anywhere in Mexico",
				"Work can be done anywhere in Mexico", "Remote from anywhere in Mexico")) {
			assertThat(described(text)).as(text)
				.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("MX")));
		}
		for (String punctuation : List.of(" ", ", ", ": ", "– ", "— ", "- ")) {
			assertThat(described("Remote" + punctuation + "anywhere in the United States or Canada"))
				.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("US"), Place.country("CA")));
		}
		assertThat(described("Australia-based candidates only"))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("AU")));
	}

	/** Asserts one location statement's country and cities without coupling to coordinates. */
	private void assertCityStatement(String text, String country, String... cities) {
		assertThat(this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of(), text)).statedSets())
			.as(text).singleElement().satisfies((stated) -> {
				assertThat(stated.kind()).isEqualTo(StatedSetKindEnum.LOCATION);
				assertThat(stated.places()).allSatisfy((place) -> assertThat(place.country()).isEqualTo(country));
				assertThat(stated.places()).extracting(Place::city).containsExactly(cities);
			});
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
		assertThat(described("We hire from anywhere in the world."))
			.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
		assertThat(described("Candidates may be located anywhere in the world."))
			.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
		assertThat(described("You will be based anywhere in the world."))
			.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
	}

	/** Welcoming applications worldwide states no hiring place, while a named country still counts. */
	@Test
	void aDescriptionWelcomingApplicationsDropsWorldwide() {
		for (String verb : List.of("welcome", "accept", "encourage", "invite")) {
			for (String object : List.of("applications", "applicants")) {
				assertThat(described("We " + verb + " " + object + " from candidates based anywhere in the world."))
					.as(verb + " " + object).isEmpty();
				assertThat(described("We " + verb + " " + object + " from candidates based anywhere in Brazil."))
					.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("BR")));
			}
		}
		assertThat(described("We welcome qualified, experienced applicants based anywhere in the world.")).isEmpty();
		assertThat(described("We welcome applicants to join us from any country.")).isEmpty();
	}

	/** Working from anywhere as a perk states no hiring place. */
	@Test
	void workingFromAnywhereAsAPerkIsNoStatedSet() {
		for (String text : List.of("Work from anywhere – flexibility to fit your life",
				"Remote-first – Work from anywhere",
				"Fully Remote Environment: Work from anywhere, with normal business hours.",
				"Work From Anywhere: Fully remote.", "Remote first, you can work from anywhere you choose.",
				"Our team works remotely across the globe.",
				"Fully remote work: work remotely from anywhere in the world.",
				"Work from anywhere. Candidates must be based in a suitable time zone.",
				"Our employees work from anywhere.")) {
			assertThat(described(text)).as(text).isEmpty();
		}
		assertThat(described("We hire in Sweden. Work from anywhere."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("SE")));
	}

	/** Hiring managers, teams, processes and partners describe hiring work rather than worldwide hiring. */
	@Test
	void aHiringManagersLocationStatesNothing() {
		for (String subject : List.of("Manager", "Managers", "team", "process", "partner", "partners")) {
			assertThat(described("Some of our Hiring " + subject + " are globally distributed.")).as(subject).isEmpty();
			assertThat(described("Our hiring " + subject + " work from anywhere in the world.")).as(subject).isEmpty();
		}
	}

	/** Global hiring and hiring through an employer of record elsewhere state Worldwide. */
	@Test
	void globalHiringStatesWorldwide() {
		assertThat(described("Join our remote-first global workforce and work from anywhere in the world."))
			.isEmpty();
		assertThat(described("Join us and work from anywhere in the world.")).isEmpty();
		for (String text : List.of("We hire globally", "We are hiring worldwide", "Our company hires talent globally",
				"We recruit skilled engineers worldwide", "We are recruiting globally",
				"We hire from any country", "We are hiring talented engineers from virtually any country",
				"join our workforce from almost any country", "Join us from nearly any country",
				"We hire people from practically any country",
				"you will be hired via an employer of record if based elsewhere",
				"You may be located elsewhere and hired through an EOR",
				"If based elsewhere, we employ you through an EOR",
				"We use an employer of record in other countries")) {
			assertThat(described(text)).as(text)
				.containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
		}
		assertThat(described("We do not hire globally.")).isEmpty();
		assertThat(described("Our customers are worldwide.")).isEmpty();
		assertThat(described("We cannot hire from any country.")).isEmpty();
		assertThat(described("We do not use an employer of record if based elsewhere.")).isEmpty();
		assertThat(described("No EOR is offered in other countries.")).isEmpty();
		assertThat(described("We use an employer of record in Canada; candidates based elsewhere are not eligible."))
			.containsExactly(stated(StatedSetKindEnum.LOCATION, Place.country("CA")));
	}

	/** Vague country counts name no place; an employer of record reaches them only when the sentence hires. */
	@Test
	void anEmployerOfRecordInMostCountriesNeedsHiring() {
		for (String amount : List.of("most", "many", "several", "numerous", "the most", "the many", "the several",
				"the numerous")) {
			assertThat(described("We can hire remotely in " + amount + " countries.")).as(amount).isEmpty();
		}
		for (String elsewhere : List.of("in most countries", "outside our country", "outside of the home country",
				"outside its core countries", "outside of our main countries")) {
			assertThat(described("We can hire remotely " + elsewhere + " through our employer of record."))
				.as(elsewhere).containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
			assertThat(described("You will be employed through an EOR " + elsewhere + "."))
				.as(elsewhere).containsExactly(stated(StatedSetKindEnum.REGION, Place.region("Worldwide")));
			assertThat(described("Deel is an employer of record " + elsewhere + ".")).as(elsewhere).isEmpty();
			assertThat(described("Our hiring manager discusses an employer of record " + elsewhere + "."))
				.as(elsewhere).isEmpty();
		}
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

	/** An export alternative admits noncitizens; a separate licence statement does not. */
	@Test
	void anExportAlternativeStatesNoCitizenship() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Software Engineer", List.of("London, UK"),
				"Applicant must either be (A) a U.S. person, defined as a U.S. citizen or permanent resident, "
						+ "or (B) eligible for an export license or other authorization from the U.S. government."));
		assertThat(set.statedSets()).noneMatch((stated) -> stated.kind() == StatedSetKindEnum.CITIZENSHIP);
		assertThat(described("US citizens only."))
			.contains(stated(StatedSetKindEnum.CITIZENSHIP, Place.country("US")));
		assertThat(described("Must be a US citizen. We may also need an export license for some customers."))
			.containsExactly(stated(StatedSetKindEnum.CITIZENSHIP, Place.country("US")));
		assertThat(described("US citizenship required. For shipments, obtain an export license or licence exemption."))
			.containsExactly(stated(StatedSetKindEnum.CITIZENSHIP, Place.country("US")));
	}

	/** A role scope states no authorization for this vacancy. */
	@Test
	void authorizationForRegionalRolesStatesNothing() {
		assertThat(described("Applicants for North American based positions with Acme must be legally authorized "
				+ "to work in the United States or Canada.")).isEmpty();
		assertThat(described("Applicants for employment in the US must have work authorization.")).isEmpty();
		assertThat(described("You must be authorized to work in the US."))
			.containsExactly(stated(StatedSetKindEnum.AUTHORIZATION, Place.country("US")));
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

	/** A clearance's own country and a citizen's demonym outweigh the declared location. */
	@Test
	void aClearanceAndCitizenshipNameTheirOwnCountry() {
		String text = "Ability to obtain or maintain an Australian Defence Security Clearance – "
				+ "must be an Australian citizen!";
		for (String location : List.of("Sydney, Australia", "Remote")) {
			List<StatedSet> requirements = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Engineer",
					List.of(location), text)).statedSets().stream()
				.filter((set) -> set.source() == StatedSetSourceEnum.DESCRIPTION).toList();
			assertThat(requirements).extracting(StatedSet::kind)
				.containsExactlyInAnyOrder(StatedSetKindEnum.CLEARANCE, StatedSetKindEnum.CITIZENSHIP);
			assertThat(requirements).allSatisfy((set) ->
				assertThat(set.places()).containsExactly(Place.country("AU")));
		}
		assertThat(described("Based in Australia; UK SC clearance required."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("GB")));
		assertThat(described("TS/SCI clearance required."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("US")));
	}

	/** National schemes and country words belong to the clearance phrase, not elsewhere in the segment. */
	@Test
	void clearanceSchemesAndDemonymsNameTheirCountries() {
		for (String scheme : List.of("SC", "DV", "BPSS", "SC security", "DV security", "BPSS security")) {
			assertThat(described("Based in Australia; " + scheme + " clearance required."))
				.as(scheme).containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("GB")));
		}
		for (String scheme : List.of("Baseline", "NV1", "NV2", "Baseline security", "NV1 security", "NV2 security")) {
			assertThat(described(scheme + " clearance required."))
				.as(scheme).containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("AU")));
		}
		for (List<String> country : List.of(List.of("American", "US"), List.of("Australian", "AU"),
				List.of("British", "GB"), List.of("Canadian", "CA"), List.of("Irish", "IE"))) {
			assertThat(described("Must be a " + country.get(0) + " citizen."))
				.containsExactly(stated(StatedSetKindEnum.CITIZENSHIP, Place.country(country.get(1))));
			assertThat(described(country.get(0) + " security clearance required."))
				.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country(country.get(1))));
		}
		assertThat(described("Canadian TS/SCI clearance required."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("US")));
		assertThat(described("Australian security clearance required. UK SC clearance required."))
			.containsExactlyInAnyOrder(stated(StatedSetKindEnum.CLEARANCE, Place.country("AU")),
					stated(StatedSetKindEnum.CLEARANCE, Place.country("GB")));
	}

	/** A clearance preferred in its own sentence states nothing beside a required clearance. */
	@Test
	void eachClearancesOwnSentenceDecidesItsPreference() {
		assertThat(clearances("Active Secret clearance strongly preferred.")).isEmpty();
		assertThat(clearances("Active Secret clearance required.")).containsExactly(0);
		assertThat(clearances("Active Secret clearance required. Top Secret clearance preferred."))
			.containsExactly(0);
		assertThat(clearances("Top Secret clearance preferred. Active Secret clearance required."))
			.containsExactly(0);
		assertThat(described("Australian security clearance required. UK SC clearance preferred."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("AU")));
		assertThat(described("Australian security clearance preferred. UK SC clearance required."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("GB")));
	}

	@Test
	void aClearanceTheDescriptionRequiresIsASetOfItsCountry() {
		assertThat(described("Must have an active TS/SCI clearance with polygraph."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("US")));
		assertThat(described("This position does not require a security clearance.")).isEmpty();
		assertThat(described("U.S. Citizenship is required for all positions with a government clearance and certain other restricted positions."))
			.isEmpty();
		assertThat(described("Under Maryland law, an employer may not require or demand, as a condition of employment, "
				+ "that an individual submit to a polygraph examination.")).isEmpty();
		assertThat(described("Under federal law, you must hold an active Secret clearance."))
			.containsExactly(stated(StatedSetKindEnum.CLEARANCE, Place.country("US")));
	}

	/** What a segment under a preferred or nice-to-have heading says is no requirement. */
	@Test
	void aSegmentUnderAPreferredHeadingStatesNothing() {
		for (String heading : List.of("Preferred Qualifications", "Nice to have", "Bonus points")) {
			CountrySet set = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
					List.of(), List.of(new Segment(SegmentKindEnum.ITEM, heading, "Secret Clearance eligible", false),
							new Segment(SegmentKindEnum.ITEM, heading, "Must be based in the US", false))));
			assertThat(set.statedSets()).as(heading).isEmpty();
		}
		CountrySet required = this.rule.read(new CountrySetVacancy(1, Set.of(WorkModeEnum.REMOTE), "Software Engineer",
				List.of(), List.of(new Segment(SegmentKindEnum.ITEM, "Requirements", "Active Secret clearance required",
						false))));
		assertThat(required.statedSets()).extracting(StatedSet::kind).containsExactly(StatedSetKindEnum.CLEARANCE);
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

	/** A label describing a timezone requirement is no location; a label naming London still is. */
	@Test
	void aLocationLabelWithAVerbStatesNoPlace() {
		assertThat(this.rule.read(vacancy(WorkModeEnum.REMOTE, "Engineer", List.of(),
				"Location: This role requires a location within APAC time zones")).statedSets())
			.singleElement().satisfies((set) -> {
				assertThat(set.kind()).isEqualTo(StatedSetKindEnum.TIMEZONE);
				assertThat(set.utcFrom()).isEqualTo(300);
				assertThat(set.utcTo()).isEqualTo(780);
			});
		assertCityStatement("Location: London, European time zone", "GB", "London");
		for (String verb : List.of("required", "must", "will", "should")) {
			assertThat(described("Location: London " + verb + " be considered.")).as(verb).isEmpty();
		}
	}

	/** Required names state windows, each alternative keeping its sentence and country. */
	@Test
	void namedTimezoneRequirementsStateWindows() {
		assertThat(timezones("This role requires a location within APAC time zones.")).containsExactly(300, 780);
		assertThat(timezones("You must be located in European time zones.")).containsExactly(-60, 180);
		assertThat(timezones("You must be based in a US time zone.")).containsExactly(-600, -300);
		assertThat(timezones("Must reside in US Eastern or Central time."))
			.containsExactly(-300, -300, -360, -360);
		assertThat(timezones("Applicants must be patient and give us time to review.")).isEmpty();
		String sentence = "Based in the EU or New York timezone.";
		List<StatedSet> zones = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Engineer", List.of(),
				"We build tools. " + sentence)).statedSets();
		assertThat(zones).hasSize(2).allSatisfy((zone) -> {
			assertThat(zone.kind()).isEqualTo(StatedSetKindEnum.TIMEZONE);
			assertThat(zone.text()).isEqualTo(sentence);
		});
		assertThat(zones.get(0).utcFrom()).isEqualTo(-60);
		assertThat(zones.get(0).utcTo()).isEqualTo(180);
		assertThat(zones.get(0).places()).isEmpty();
		assertThat(zones.get(1).utcFrom()).isEqualTo(-300);
		assertThat(zones.get(1).utcTo()).isEqualTo(-300);
		assertThat(zones.get(1).places()).containsExactly(Place.country("US"));
		for (String text : List.of("Daily collaboration overlap with colleagues in European time zones.",
				"Work US Eastern hours.", "Preferably based in European time zones.",
				"You must not be based in European time zones.")) {
			assertThat(timezones(text)).as(text).isEmpty();
		}
	}

	/** A window repeated in a separate segment must keep that separate requirement. */
	@Test
	void repeatedTimezoneWindowsKeepTheirStatements() {
		CountrySet set = this.rule.read(vacancy(WorkModeEnum.REMOTE, "Engineer", List.of(),
				"Based in the EU or New York timezone.", "Based in the EU timezone."));
		assertThat(set.statedSets()).extracting(StatedSet::segment).containsExactly(0, 0, 1);
		assertThat(new EligibilityRule(PlaceListConfiguration.shipped()).eligibility(set, "US", null).state())
			.isEqualTo(EligibilityStateEnum.EXCLUDED);
	}

	/** Every supported alias keeps its full name and the offsets of that zone. */
	@Test
	void timezoneAliasesKeepTheirOffsetsAndCountries() {
		assertZoneAliases(300, 780, false, "APAC", "Asia-Pacific", "Asia Pacific");
		assertZoneAliases(-60, 180, false, "European", "Europe", "EU");
		assertZoneAliases(60, 60, false, "Central European", "CET");
		assertZoneAliases(-600, -300, true, "US", "U.S.", "United States");
		assertZoneAliases(-300, -300, true, "US Eastern", "Eastern", "ET", "EST", "New York");
		assertZoneAliases(-360, -360, true, "US Central", "Central", "CT", "CST", "Chicago");
		assertZoneAliases(-420, -420, true, "US Mountain", "Mountain", "MT", "MST", "Denver");
		assertZoneAliases(-480, -480, true, "US Pacific", "Pacific", "PT", "PST", "Los Angeles", "San Francisco");
		for (String separator : List.of(" or ", " and ", ", ", "/", " & ")) {
			assertThat(timezones("Must reside in Eastern" + separator + "Central time."))
				.as(separator).containsExactly(-300, -300, -360, -360);
		}
		assertThat(timezones("We require European time zones.")).containsExactly(-60, 180);
		assertThat(timezones("Required: European timezones.")).containsExactly(-60, 180);
	}

	/** Asserts the window and country restriction of each alias without reading unrelated location sets. */
	private void assertZoneAliases(int from, int to, boolean us, String... aliases) {
		for (String alias : aliases) {
			String text = "You must be based in " + alias + " time zones.";
			assertThat(this.rule.read(vacancy(WorkModeEnum.REMOTE, "Engineer", List.of(), text)).statedSets()
				.stream().filter((set) -> set.kind() == StatedSetKindEnum.TIMEZONE).toList())
				.as(alias).singleElement().satisfies((zone) -> {
					assertThat(zone.utcFrom()).isEqualTo(from);
					assertThat(zone.utcTo()).isEqualTo(to);
					assertThat(zone.places()).isEqualTo(us ? List.of(Place.country("US")) : List.of());
					assertThat(zone.text()).isEqualTo(text);
				});
		}
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
		return new CountrySetVacancy(1, (workMode != null) ? Set.of(workMode) : Set.of(), title, located, segments);
	}

}
