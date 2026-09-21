package oneprofile.backend.classification;

import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.normalizedvacancy.Classification;
import oneprofile.backend.normalizedvacancy.UnknownReason;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TitleClassificationTest {

	private final TitleClassification classification = new TitleClassification();

	@Nested
	class Rulings {

		@Test
		void overrideTheVerdictTheProcedureWouldHaveReached() {
			// Step 4 sends this to OUT on the manufacturing marker; the ruling holds it open.
			assertThat(classify("manufacturing systems engineer"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
			// Step 6 leaves this unruled, because designer carries no engineering work.
			assertThat(classify("game designer")).isEqualTo(Classification.out());
		}

		@Test
		void areReadOffAPhraseInsideALongerTitle() {
			assertThat(classify("scrum master agile delivery")).isEqualTo(Classification.in());
		}

		@Test
		void takeTheEarliestPhraseWhereATitleNamesTwo() {
			assertThat(classify("civil engineer data scientist")).isEqualTo(Classification.out());
			assertThat(classify("data scientist civil engineer")).isEqualTo(Classification.in());
		}

	}

	@Nested
	class Heads {

		@Test
		void aHeadNoListReachesIsOurBacklogRatherThanTheCorpusAmbiguity() {
			assertThat(classify("security roboticist")).isEqualTo(Classification.unknown(UnknownReason.UNRULED));
		}

		@Test
		void aNeverEngineeringHeadDecidesOutWhateverModifiesIt() {
			assertThat(classify("software trainer")).isEqualTo(Classification.out());
		}

		@Test
		void theFirstHeadNamedIsTheHead() {
			assertThat(classify("writer software engineer")).isEqualTo(Classification.unknown(UnknownReason.UNRULED));
			assertThat(classify("software engineer writer")).isEqualTo(Classification.in());
		}

		@Test
		void aKnownHeadThatCarriesEngineeringAndNoDomainIsTheCorpusAmbiguity() {
			assertThat(classify("engineer")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void aKnownHeadThatCarriesNoEngineeringIsStillOurBacklog() {
			assertThat(classify("content writer")).isEqualTo(Classification.unknown(UnknownReason.UNRULED));
		}

		@Test
		void aKnownHeadThatCarriesNoEngineeringIsStillReadForItsDomain() {
			// What holding writer and master as heads buys over leaving them off the list: the head
			// is domain-bound, so an off-domain marker under it still decides, where an unknown head
			// would have returned at step 2 without reading the modifier at all.
			assertThat(classify("legal writer")).isEqualTo(Classification.out());
			assertThat(classify("master electrician")).isEqualTo(Classification.out());
		}

	}

	@Nested
	class Modifiers {

		@Test
		void aSoftwareQualifierUnderAnEngineeringCapableHeadDecidesIn() {
			assertThat(classify("backend developer")).isEqualTo(Classification.in());
			assertThat(classify("site reliability engineer")).isEqualTo(Classification.in());
		}

		@Test
		void anOffDomainMarkerBeatsASoftwareQualifierUnderADomainBoundHead() {
			assertThat(classify("mechanical software engineer")).isEqualTo(Classification.out());
		}

		@Test
		void readAnOffDomainMarkerThatIsSpeltAcrossMoreThanOneWord() {
			assertThat(classify("power systems engineer")).isEqualTo(Classification.out());
			assertThat(classify("data center site manager")).isEqualTo(Classification.out());
		}

		@Test
		void anOffDomainMarkerDecidesNothingUnderADomainFreeHead() {
			assertThat(classify("audit analyst")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void aSoftwareQualifierUnderAHeadThatCarriesNoEngineeringDecidesNothing() {
			assertThat(classify("software writer")).isEqualTo(Classification.unknown(UnknownReason.UNRULED));
		}

	}

	@Nested
	class ProductManagement {

		@Test
		void isInWhereTheTitleNamesTheSoftwareTheRoleManages() {
			assertThat(classify("product manager mobile")).isEqualTo(Classification.in());
			assertThat(classify("technical product manager gpu infrastructure")).isEqualTo(Classification.in());
		}

		@Test
		void isTheCorpusAmbiguityWhereTheTitleNamesNoDomain() {
			assertThat(classify("product manager")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void isOutWhereTheTitleNamesADomainThatIsNotSoftware() {
			assertThat(classify("product manager construction")).isEqualTo(Classification.out());
		}

		@Test
		void readsProductOwnerTheSameWayItReadsScrumMaster() {
			assertThat(classify("product owner office cloud storage")).isEqualTo(Classification.in());
		}

	}

	@Nested
	class Coverage {

		@Test
		void readsALeadAndASpecialistAndAnEngineeringOrganisationAsHeads() {
			assertThat(classify("software team lead")).isEqualTo(Classification.in());
			assertThat(classify("software qa specialist")).isEqualTo(Classification.in());
			// This read "future opportunities software engineering" until iteration 6, when Q1 made
			// the talent-pool posting out however engineering it sounds; the head it was here to
			// demonstrate is the same one.
			assertThat(classify("platform software engineering")).isEqualTo(Classification.in());
		}

		@Test
		void readsATechnologyByNameAsTheSoftwareDomainItNames() {
			assertThat(classify("salesforce developer")).isEqualTo(Classification.in());
			assertThat(classify("it systems engineer")).isEqualTo(Classification.in());
			assertThat(classify("associate cybersecurity analyst")).isEqualTo(Classification.in());
		}

		@Test
		void readsAnEngineerDeployedIntoACustomerAsAnEngineer() {
			assertThat(classify("founding forward deployed engineer")).isEqualTo(Classification.in());
			assertThat(classify("forward deployed engineering manager")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheFunctionsThatAreNeverEngineeringHoweverTheirTitleIsSpelt() {
			// The veterinarian, spelt without the associate that iteration 6 made a head: what the
			// seniority costs when it is named first is pinned in CoverageFromIterationSix.
			assertThat(classify("veterinarian dvm")).isEqualTo(Classification.out());
			assertThat(classify("litigation paralegal")).isEqualTo(Classification.out());
			assertThat(classify("manufacturing supervisor 3rd shift")).isEqualTo(Classification.out());
			assertThat(classify("game development environment artist")).isEqualTo(Classification.out());
		}

		@Test
		void readsACommercialDomainAsOffDomainTheSameWayItReadsAPhysicalOne() {
			assertThat(classify("customer success manager")).isEqualTo(Classification.out());
			assertThat(classify("national channel sales manager")).isEqualTo(Classification.out());
			assertThat(classify("talent acquisition specialist")).isEqualTo(Classification.out());
			assertThat(classify("investor relations strategy manager")).isEqualTo(Classification.out());
		}

		@Test
		void stillReadsSalesEngineerAsTheRulingAndNotAsASalesDomain() {
			assertThat(classify("associate sales engineer se desk southeast"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
		}

		@Test
		void readsTheTechnicalLayersATitleNamesAsSoftwareDomains() {
			assertThat(classify("middleware engineer")).isEqualTo(Classification.in());
			assertThat(classify("algorithm developer")).isEqualTo(Classification.in());
			assertThat(classify("product engineer ux ui")).isEqualTo(Classification.in());
			assertThat(classify("edi mapping specialist with e invoicing")).isEqualTo(Classification.in());
			assertThat(classify("lead identity access management specialist")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheOfficerWhoRunsTheEngineeringAsAnEngineeringRole() {
			assertThat(classify("chief technology officer")).isEqualTo(Classification.in());
		}

		@Test
		void readsAnAiTrainerAsTheDomainExpertItHiresRatherThanTheAiItNames() {
			assertThat(classify("chinese language specialist freelance ai trainer project"))
				.isEqualTo(Classification.out());
		}

		@Test
		void readsAPhysicalDomainAsOffDomainHoweverSoftwareTheQualifierSounds() {
			assertThat(classify("physical security engineer")).isEqualTo(Classification.out());
			assertThat(classify("propulsion integration test engineer")).isEqualTo(Classification.out());
			assertThat(classify("space avionics systems engineer")).isEqualTo(Classification.out());
			assertThat(classify("quality assurance engineer plc automation")).isEqualTo(Classification.out());
			assertThat(classify("automation controls engineer asset engineering")).isEqualTo(Classification.out());
			assertThat(classify("electronics integration and development engineer")).isEqualTo(Classification.out());
			assertThat(classify("turbomachinery test responsible engineer")).isEqualTo(Classification.out());
		}

		@Test
		void readsAiTrainingTheSameWayItReadsAiTrainer() {
			assertThat(classify("ai training education analyst")).isEqualTo(Classification.out());
			assertThat(classify("customer support reps ai training belfast uk")).isEqualTo(Classification.out());
		}

		@Test
		void readsSolutionsAsASoftwareDomainOnlyWhereARulingSaysSo() {
			assertThat(classify("partner solutions engineer latam")).isEqualTo(Classification.in());
			assertThat(classify("solutions engineering lead healthcare life sciences")).isEqualTo(Classification.in());
			assertThat(classify("workplace solutions manager"))
				.isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void letsTheModifierDecideAnEngineeringManagerRatherThanRulingThePhrase() {
			assertThat(classify("engineering manager data delivery platform")).isEqualTo(Classification.in());
			assertThat(classify("engineering manager"))
				.isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
			assertThat(classify("manufacturing engineering manager")).isEqualTo(Classification.out());
		}

	}

	@Nested
	class CoverageFromIterationFour {

		@Test
		void readsTheEngineerWhoCarriesACustomerRelationshipAsAnEngineer() {
			assertThat(classify("technical account manager azure")).isEqualTo(Classification.in());
			assertThat(classify("manager technical account management")).isEqualTo(Classification.in());
			assertThat(classify("customer success engineer robotics software and automation"))
				.isEqualTo(Classification.in());
		}

		@Test
		void readsEmbeddedSoftwareAsSoftwareHoweverPhysicalTheMachineItRunsOn() {
			assertThat(classify("embedded software engineer multicore platforms avionics networking"))
				.isEqualTo(Classification.in());
		}

		@Test
		void readsAProductNameThatCarriesAnOffDomainWordAsTheSoftwareItNames() {
			assertThat(classify("staff back end engineer rocket pay")).isEqualTo(Classification.in());
			assertThat(classify("manager back end engineering rocket pay")).isEqualTo(Classification.in());
		}

		@Test
		void readsTechnicalUnderAnEngineeringCapableHeadAsASoftwareDomain() {
			assertThat(classify("technical project manager")).isEqualTo(Classification.in());
			assertThat(classify("coupa technical functional lead")).isEqualTo(Classification.in());
			assertThat(classify("technical project manager industrial automation")).isEqualTo(Classification.out());
			assertThat(classify("technical project lead silicon engineering")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheHeadsThatNameResearchAndConsultingWork() {
			assertThat(classify("staff applied ai researcher agentic search")).isEqualTo(Classification.in());
			assertThat(classify("machine learning fellow human frontier collective")).isEqualTo(Classification.in());
			assertThat(classify("salesforce marketing cloud consultant specialist")).isEqualTo(Classification.in());
			// Iteration 6 made the consultant domain-bound and transformation an off-domain marker,
			// so the strategy consultant this once left open is decided.
			assertThat(classify("strategy consultant transformation")).isEqualTo(Classification.out());
			// The cost of reading consultant as engineering-capable: the physician a hospital calls a
			// consultant names its own profession second, and the first head named is the head.
			assertThat(classify("consultant gastroenterologist bristol"))
				.isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsTheSoftwareDomainsThisCorpusNamesByTheirOwnWord() {
			assertThat(classify("applied scientist efficient llm inference model optimization"))
				.isEqualTo(Classification.in());
			assertThat(classify("informatics engineer")).isEqualTo(Classification.in());
			assertThat(classify("netsuite administrator")).isEqualTo(Classification.in());
			assertThat(classify("overdare unreal gameplay engineer")).isEqualTo(Classification.in());
			assertThat(classify("quantitative developer")).isEqualTo(Classification.in());
			assertThat(classify("rendering engineer")).isEqualTo(Classification.in());
			assertThat(classify("manager detection engineering rapid response team")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheTitlesThatNameTheirRoleWithoutNamingAHead() {
			assertThat(classify("member of technical staff grok product")).isEqualTo(Classification.in());
			assertThat(classify("sdet ii tvscientific")).isEqualTo(Classification.in());
			assertThat(classify("enterprise architect director")).isEqualTo(Classification.in());
			assertThat(classify("solution architect")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheRemainingPhysicalDomainsAsOffDomain() {
			assertThat(classify("hardware engineer")).isEqualTo(Classification.out());
			assertThat(classify("mep manager systems integration")).isEqualTo(Classification.out());
			assertThat(classify("test engineer high pressure gas systems")).isEqualTo(Classification.out());
			assertThat(classify("board test engineer")).isEqualTo(Classification.out());
			assertThat(classify("solar field service technician")).isEqualTo(Classification.out());
			assertThat(classify("registered behavior technician")).isEqualTo(Classification.out());
			assertThat(classify("weld technical specialist ii")).isEqualTo(Classification.out());
			assertThat(classify("grid connection engineer")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheCommercialTitlesThatNoHeadReached() {
			assertThat(classify("sales associate at firstmind")).isEqualTo(Classification.out());
			assertThat(classify("service associate night")).isEqualTo(Classification.out());
			assertThat(classify("operations associate planning supply chain")).isEqualTo(Classification.out());
			assertThat(classify("general manager philadelphia")).isEqualTo(Classification.out());
			assertThat(classify("after sales engineer")).isEqualTo(Classification.out());
			assertThat(classify("field application scientist synbio and ngs panel design"))
				.isEqualTo(Classification.out());
			assertThat(classify("hr operations specialist")).isEqualTo(Classification.out());
			assertThat(classify("associate software engineer")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheAnnotationPostingAheadOfTheProfessionItRecruitsFrom() {
			// Iteration 4 read this out and iteration 5 read its Wellington twin in, so the phrase
			// is left undecided rather than decided one of the two ways two labellers split on.
			assertThat(classify("database administrator graduates ai training lyon france"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
		}

		@Test
		void readsTheSpanishAndPortugueseHeadsThisCorpusCarries() {
			assertThat(classify("engenheiro a de software assistente e junior")).isEqualTo(Classification.in());
			assertThat(classify("desarrollador backend")).isEqualTo(Classification.in());
			assertThat(classify("vendedor a externo 6 horas")).isEqualTo(Classification.out());
			assertThat(classify("aide a domicile")).isEqualTo(Classification.out());
			assertThat(classify("auxiliar de atendimento logistico")).isEqualTo(Classification.out());
			assertThat(classify("talent agent influencer")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheProfessionsTheUnknownPileStillHeld() {
			assertThat(classify("anesthesiologist norwood oh")).isEqualTo(Classification.out());
			assertThat(classify("qc chemist 3rd shift")).isEqualTo(Classification.out());
			assertThat(classify("general laborer")).isEqualTo(Classification.out());
			assertThat(classify("estimator phoenix")).isEqualTo(Classification.out());
			assertThat(classify("hr generalist emea")).isEqualTo(Classification.out());
			assertThat(classify("fire alarm inspector")).isEqualTo(Classification.out());
			assertThat(classify("planner furniture monitors")).isEqualTo(Classification.out());
			assertThat(classify("conference producer at private equity insights")).isEqualTo(Classification.out());
			assertThat(classify("custodian")).isEqualTo(Classification.out());
		}

	}

	@Nested
	class CoverageFromIterationFive {

		@Test
		void readsTheFunctionsFourSamplesRunningHaveNeverCalledEngineering() {
			assertThat(classify("nail technician")).isEqualTo(Classification.out());
			assertThat(classify("vehicle technicians opportunities nationwide")).isEqualTo(Classification.out());
			assertThat(classify("clinical technologist i")).isEqualTo(Classification.out());
			assertThat(classify("rich media designer")).isEqualTo(Classification.out());
			assertThat(classify("social worker child protection")).isEqualTo(Classification.out());
			assertThat(classify("companion direct support professional")).isEqualTo(Classification.out());
			assertThat(classify("security officers")).isEqualTo(Classification.out());
			assertThat(classify("sports data collector football santa cruz costa rica"))
				.isEqualTo(Classification.out());
			assertThat(classify("continuous improvement coach")).isEqualTo(Classification.out());
			assertThat(classify("recruiting sourcer")).isEqualTo(Classification.out());
			assertThat(classify("psychiatric clinician")).isEqualTo(Classification.out());
			assertThat(classify("auxiliaire de vie")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheItSupportTechnicianAsTheSplitTheLabelsSayItIs() {
			assertThat(classify("it technician first shift"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
			assertThat(classify("data center it technician"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
			assertThat(classify("help desk technician tier 1 2"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
		}

		@Test
		void readsTheOfficerWhoRunsTheInformationAsAnEngineeringRole() {
			assertThat(classify("chief information officer")).isEqualTo(Classification.in());
			assertThat(classify("chief information security officer")).isEqualTo(Classification.in());
			assertThat(classify("lead information system security officer isso"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
		}

		@Test
		void readsTheHeadsThatCarryTheirDomainInTheModifier() {
			assertThat(classify("product software intern")).isEqualTo(Classification.in());
			assertThat(classify("wealth management intern")).isEqualTo(Classification.out());
			assertThat(classify("healthcare architectural team leader")).isEqualTo(Classification.out());
			assertThat(classify("regional sales partner mideast")).isEqualTo(Classification.out());
			assertThat(classify("pt sales advisor brooklyn")).isEqualTo(Classification.out());
			assertThat(classify("fashion paid advertising strategist")).isEqualTo(Classification.out());
			assertThat(classify("desenvolvimento full stack python e typescript")).isEqualTo(Classification.in());
			assertThat(classify("consultor de vendas maraba ap prazo determinado")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheHeadsThatRunTheWorkRatherThanDoIt() {
			assertThat(classify("sales director")).isEqualTo(Classification.out());
			assertThat(classify("director of aviation")).isEqualTo(Classification.out());
			assertThat(classify("head of accounts payable")).isEqualTo(Classification.out());
			assertThat(classify("vice president public relations business of health")).isEqualTo(Classification.out());
			assertThat(classify("director software engineering retail platform delivery elera"))
				.isEqualTo(Classification.out());
			// The seniority still cannot swallow the engineer underneath it.
			assertThat(classify("director of data engineering")).isEqualTo(Classification.in());
			// What these heads cost, and what three iterations kept them off the list for: they are
			// named before the head that would have decided the title, so they hide it. Iteration 7
			// paid half of it back with a marker — the chef is a domain as well as a head — and the
			// counsel is still hidden.
			assertThat(classify("head chef")).isEqualTo(Classification.out());
			assertThat(classify("director securities corporate counsel"))
				.isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsAPluralHeadAsTheHeadItNames() {
			assertThat(classify("javascript developers")).isEqualTo(Classification.in());
			assertThat(classify("cloud engineers wanted")).isEqualTo(Classification.in());
			// The one plural iteration 4 kept off the list on purpose stays off it: an agentic
			// engineer is not the talent agent the singular names.
			assertThat(classify("frontier agents engineer applied ai")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheAnnotationPostingAsTheSplitTwoLabellersMadeOfIt() {
			assertThat(classify("javascript developers ai training omaha us"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
			assertThat(classify("computer sciences graduates ai training brighton uk"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
			// The posting that names a profession no software background reaches is untouched.
			assertThat(classify("registered nurses ai training belfast uk")).isEqualTo(Classification.out());
		}

		@Test
		void readsThePreSalesEngineerTheWayItAlreadyReadsTheSalesEngineer() {
			assertThat(classify("pre sales systems engineer higher education mid atlantic"))
				.isEqualTo(Classification.unknown(UnknownReason.SCOPE_AMBIGUITY));
		}

		@Test
		void readsTheBehaviourFamilyByItsOwnTitlesRatherThanByTheWordBehavior() {
			assertThat(classify("registered behavior technician rbt")).isEqualTo(Classification.out());
			assertThat(classify("board certified behavior analyst")).isEqualTo(Classification.out());
			// What the marker cost was this: behaviour planning is autonomy software, and dropping
			// the word stops it being decided out. It names no software domain either, so what the
			// change buys is an honest unknown rather than a right answer.
			assertThat(classify("behavior planning engineer"))
				.isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsTheRemainingPhysicalDomainsTheSampleNamed() {
			assertThat(classify("laser test engineer")).isEqualTo(Classification.out());
			assertThat(classify("guidance navigation and control engineer space systems"))
				.isEqualTo(Classification.out());
			assertThat(classify("staff systems engineer ew")).isEqualTo(Classification.out());
			assertThat(classify("brakes and steering engineer")).isEqualTo(Classification.out());
			assertThat(classify("fastener engineer")).isEqualTo(Classification.out());
			assertThat(classify("mechatronics engineer")).isEqualTo(Classification.out());
			assertThat(classify("manager battery engineering")).isEqualTo(Classification.out());
			assertThat(classify("landscape technician stormwater")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheCommercialFunctionsTheUnknownPileStillHeld() {
			assertThat(classify("business development manager")).isEqualTo(Classification.out());
			assertThat(classify("renewals manager emea")).isEqualTo(Classification.out());
			assertThat(classify("territory manager warner robins ga")).isEqualTo(Classification.out());
			assertThat(classify("purchasing manager")).isEqualTo(Classification.out());
			assertThat(classify("sourcing manager sweaters")).isEqualTo(Classification.out());
			assertThat(classify("contracts manager")).isEqualTo(Classification.out());
			assertThat(classify("licensing manager")).isEqualTo(Classification.out());
			assertThat(classify("community manager lease up")).isEqualTo(Classification.out());
			assertThat(classify("customer care advisor non voice")).isEqualTo(Classification.out());
			assertThat(classify("customer experience enablement manager")).isEqualTo(Classification.out());
			assertThat(classify("operations manager fedex pickup and delivery")).isEqualTo(Classification.out());
			// The phrase is ruled rather than the word, so the operations a software role names
			// still reads as software.
			assertThat(classify("cloud operations engineer")).isEqualTo(Classification.in());
		}

		@Test
		void readsThePostingThatRecruitsNobodyAsNobody() {
			assertThat(classify("us residents survey participants austin")).isEqualTo(Classification.out());
			assertThat(classify("japan residents survey participants oita japan")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheDesignerWhoseHeadIsMaskedByASeniority() {
			assertThat(classify("lead product designer ui design systems product experience"))
				.isEqualTo(Classification.out());
		}

		@Test
		void readsTheArchitectureAPostingNamesTheWayItReadsTheArchitect() {
			assertThat(classify("head of enterprise solutions architecture platforms"))
				.isEqualTo(Classification.in());
		}

		@Test
		void readsTheSoftwareDomainsThisSampleNamedByTheirOwnWord() {
			assertThat(classify("exploit developer us")).isEqualTo(Classification.in());
			assertThat(classify("engineering manager streaming")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheHospitalConsultantByTheSpecialityRatherThanByTheHead() {
			assertThat(classify("consultant in adult psychiatry middlesbrough")).isEqualTo(Classification.out());
			assertThat(classify("consultant child and adolescent psychiatry camhs greater london"))
				.isEqualTo(Classification.out());
		}

	}

	@Nested
	class CoverageFromIterationSix {

		@Test
		void readsTheDomainsTheFalseAcceptsNamed() {
			// The nine false accepts iteration 5's classifier made on this sample were all one
			// shape: a software qualifier under a head, with the domain named by a word no marker
			// list held yet.
			assertThat(classify("rf test engineer staff")).isEqualTo(Classification.out());
			assertThat(classify("emc test engineer")).isEqualTo(Classification.out());
			assertThat(classify("quality assurance manager women s apparel qa import"))
				.isEqualTo(Classification.out());
			assertThat(classify("it asset manager")).isEqualTo(Classification.out());
			assertThat(classify("partner manager channels uki security")).isEqualTo(Classification.out());
			assertThat(classify("vice president client partnerships publisher cloud"))
				.isEqualTo(Classification.out());
			assertThat(classify("pfas technical manager")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheProductNameAdvertisingAndMarketingHid() {
			// The advertising marker was bought in iteration 5 and cost a miss here: advertising
			// engineering is software, and the strategist the marker was bought for is decided by
			// the fashion marker sitting next to it.
			assertThat(classify("software engineer advertising engineering")).isEqualTo(Classification.in());
			assertThat(classify("fashion paid advertising strategist")).isEqualTo(Classification.out());
			// Salesforce Marketing Cloud is a product and not the marketing domain, which is the
			// one shape the marketing marker was getting wrong in both directions.
			assertThat(classify("developer salesforce marketing cloud")).isEqualTo(Classification.in());
			assertThat(classify("salesforce marketing cloud consultant specialist")).isEqualTo(Classification.in());
			assertThat(classify("marketing manager emea")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheRedTeamAsTheSecurityWorkItIs() {
			// The operator head is never engineering, so the phrase has to be ruled above it.
			assertThat(classify("red team operator")).isEqualTo(Classification.in());
		}

		@Test
		void readsAnAssociateAsTheHeadWhoseModifierCarriesTheDomain() {
			assertThat(classify("associate business development")).isEqualTo(Classification.out());
			assertThat(classify("warehouse associate")).isEqualTo(Classification.out());
			assertThat(classify("associate payroll mum")).isEqualTo(Classification.out());
			// The seniority cannot swallow the engineer underneath it.
			assertThat(classify("associate software engineer")).isEqualTo(Classification.in());
			// What the head cost, and what iteration 5 kept it off the list for: it is named before
			// the profession that would have decided the title, so it hid it. Iteration 7 made the
			// veterinarian a domain as well as a head, which is what pays that cost back.
			assertThat(classify("associate veterinarian")).isEqualTo(Classification.out());
		}

		@Test
		void readsAConsultantsModifierAsTheDomainTheConsultantWorksIn() {
			// Iteration 4 made the consultant domain-free on the argument that it operates on
			// information about a domain. Five samples say otherwise: the consultants this corpus
			// posts are retail, property and recruitment, and the marker list now reaches them.
			assertThat(classify("leasing consultant")).isEqualTo(Classification.out());
			assertThat(classify("beauty consultant boots swords dublin")).isEqualTo(Classification.out());
			assertThat(classify("entry level recruitment consultant")).isEqualTo(Classification.out());
			assertThat(classify("real estate acquisition consultant")).isEqualTo(Classification.out());
			// The software consultant is still a software consultant.
			assertThat(classify("sap consultant")).isEqualTo(Classification.in());
			// What the change costs: the audit consultant the domain-free reading held open reads
			// out now, where the audit analyst next to it does not.
			assertThat(classify("audit consultant")).isEqualTo(Classification.out());
			assertThat(classify("audit analyst")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsTheElectricalEngineerTheWayFiveSamplesHaveLabelledIt() {
			// Nine electrical engineers across five samples, all nine labelled out. The phrase was
			// ruled a split in iteration 2 on an argument no sample has ever borne out.
			assertThat(classify("electrical engineer i")).isEqualTo(Classification.out());
			assertThat(classify("electrical engineer power systems design")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheProfessionsThisSamplesUnknownPileHeld() {
			assertThat(classify("advanced practice provider pacific cancer care")).isEqualTo(Classification.out());
			assertThat(classify("universal banker st petersburg")).isEqualTo(Classification.out());
			assertThat(classify("spa attendant morning shift part time")).isEqualTo(Classification.out());
			assertThat(classify("sheet metal fabricator")).isEqualTo(Classification.out());
			assertThat(classify("instrumentation fitter 2nd shift")).isEqualTo(Classification.out());
			assertThat(classify("band 7 cardiac physiologist warwick")).isEqualTo(Classification.out());
			assertThat(classify("electrical apprentice")).isEqualTo(Classification.out());
			assertThat(classify("part time babysitter pearland tx")).isEqualTo(Classification.out());
			assertThat(classify("pt sorter")).isEqualTo(Classification.out());
			assertThat(classify("personalberater")).isEqualTo(Classification.out());
			assertThat(classify("superviseur de rayon")).isEqualTo(Classification.out());
			assertThat(classify("ejecutivo de ventas en campo cdmx sur")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheDomainsTheUnknownPileNamedWithoutAMarker() {
			assertThat(classify("gear design engineer")).isEqualTo(Classification.out());
			assertThat(classify("optomechanical engineer")).isEqualTo(Classification.out());
			assertThat(classify("roadway engineer")).isEqualTo(Classification.out());
			assertThat(classify("structures engineer lunar")).isEqualTo(Classification.out());
			assertThat(classify("nuclear supplier quality engineer")).isEqualTo(Classification.out());
			assertThat(classify("flight termination system engineer")).isEqualTo(Classification.out());
			assertThat(classify("manager regulatory affairs")).isEqualTo(Classification.out());
			assertThat(classify("inventory specialist")).isEqualTo(Classification.out());
			assertThat(classify("business process transformation specialist")).isEqualTo(Classification.out());
			assertThat(classify("print production specialist")).isEqualTo(Classification.out());
			assertThat(classify("seo aso manager")).isEqualTo(Classification.out());
			assertThat(classify("programmatic specialist")).isEqualTo(Classification.out());
			// A head coach is a coach, and the leadership heads iteration 5 added read it first.
			assertThat(classify("head varsity boys basketball coach high school")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheSoftwareDomainsThisSamplesUnknownPileNamedAsSoftware() {
			// Sixteen of the twenty-four titles this sample left unknown and labelled in were one
			// of these words, which is where the recall the miss rate cannot see was going.
			assertThat(classify("low latency c++ developer")).isEqualTo(Classification.in());
			assertThat(classify("gis developer")).isEqualTo(Classification.in());
			assertThat(classify("outsystems developer")).isEqualTo(Classification.in());
			assertThat(classify("devsecops engineer ii")).isEqualTo(Classification.in());
			assertThat(classify("manager monitoring observability")).isEqualTo(Classification.in());
			assertThat(classify("staff threat attack research engineer")).isEqualTo(Classification.in());
			assertThat(classify("consultant manager cyber")).isEqualTo(Classification.in());
			assertThat(classify("business applications manager")).isEqualTo(Classification.in());
			assertThat(classify("engineering director storage")).isEqualTo(Classification.in());
			assertThat(classify("manager bi analytics engineer")).isEqualTo(Classification.in());
			assertThat(classify("managing director of technology chicago")).isEqualTo(Classification.in());
			assertThat(classify("d\u00e9veloppeur c++")).isEqualTo(Classification.in());
		}

	}

	@Nested
	class CoverageFromIterationSeven {

		@Test
		void readsTheProfessionsAndOfficerTitlesTheUnruledPileHeld() {
			// The heads a title names where no rule reached it at all. None of them is a function a
			// software domain turns into engineering work, so each decides out wherever it leads.
			assertThat(classify("pediatrician")).isEqualTo(Classification.out());
			assertThat(classify("physiatrist")).isEqualTo(Classification.out());
			assertThat(classify("registrar in paediatrics london")).isEqualTo(Classification.out());
			assertThat(classify("prn crna the physicians centre hospital bryan tx")).isEqualTo(Classification.out());
			assertThat(classify("dental hygienist")).isEqualTo(Classification.out());
			assertThat(classify("sommelier")).isEqualTo(Classification.out());
			assertThat(classify("onsite contract interpreters")).isEqualTo(Classification.out());
			assertThat(classify("night cleaner high road house west london")).isEqualTo(Classification.out());
			assertThat(classify("settlement negotiator")).isEqualTo(Classification.out());
			assertThat(classify("execution trader")).isEqualTo(Classification.out());
			assertThat(classify("health sciences assignment grader contract")).isEqualTo(Classification.out());
			assertThat(classify("scriptwriter dex")).isEqualTo(Classification.out());
			assertThat(classify("behavioral interventionist")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheOfficerTheTitleNamesByItsAbbreviation() {
			// A chief of finance, marketing, people or operations is never an engineering role, and
			// the chiefs that are — the technology and information officers — are rulings already.
			assertThat(classify("cfo prism media llc")).isEqualTo(Classification.out());
			assertThat(classify("cmo in training samlino group")).isEqualTo(Classification.out());
			assertThat(classify("sdr")).isEqualTo(Classification.out());
			assertThat(classify("gm europe")).isEqualTo(Classification.out());
			assertThat(classify("chief technology officer")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheLeadershipAbbreviationsAsTheHeadsTheySpellOut() {
			// vp, avp and mgr are director and manager written short, so they read their modifier
			// the same way: the domain decides, and a bare one stays undecided.
			assertThat(classify("vp legal")).isEqualTo(Classification.out());
			assertThat(classify("vp of strategic accounts financial services")).isEqualTo(Classification.out());
			assertThat(classify("mgr financial business consulting")).isEqualTo(Classification.out());
			assertThat(classify("vp growth")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsTheStudentPostingByTheDomainItNames() {
			// An internship is a vacancy and its modifier carries the domain, the way the intern's
			// does. The German and the veterinary ones are the two spellings this corpus repeats.
			assertThat(classify("veterinary student externship")).isEqualTo(Classification.out());
			assertThat(classify("werkstudent in influencer marketing")).isEqualTo(Classification.out());
			assertThat(classify("event marketing internship paid at united media")).isEqualTo(Classification.out());
			assertThat(classify("software engineering internship summer 2027")).isEqualTo(Classification.in());
		}

		@Test
		void readsTheDomainsTheUnknownPileNamedWithoutAMarker() {
			assertThat(classify("head chef")).isEqualTo(Classification.out());
			assertThat(classify("associate veterinarian mansfield")).isEqualTo(Classification.out());
			assertThat(classify("kids club manager")).isEqualTo(Classification.out());
			assertThat(classify("temporary regional manager affordable housing")).isEqualTo(Classification.out());
			assertThat(classify("hospice rn case manager union essex county nj")).isEqualTo(Classification.out());
			assertThat(classify("head of compensation")).isEqualTo(Classification.out());
			assertThat(classify("benefits specialist")).isEqualTo(Classification.out());
			assertThat(classify("bookkeeping specialist")).isEqualTo(Classification.out());
			assertThat(classify("government affairs manager")).isEqualTo(Classification.out());
			assertThat(classify("director demand generation")).isEqualTo(Classification.out());
			assertThat(classify("head of tiktok shop")).isEqualTo(Classification.out());
			assertThat(classify("band7 specialist speech and language therapist woolwich")).isEqualTo(Classification.out());
			assertThat(classify("underground mine engineer")).isEqualTo(Classification.out());
			assertThat(classify("photonics characterization intern new grad")).isEqualTo(Classification.out());
			assertThat(classify("research associate gene therapy")).isEqualTo(Classification.out());
			assertThat(classify("project developer renovations interiors")).isEqualTo(Classification.out());
			assertThat(classify("telesales team leader")).isEqualTo(Classification.out());
			assertThat(classify("associate esg")).isEqualTo(Classification.out());
			assertThat(classify("commissioning engineer")).isEqualTo(Classification.out());
			assertThat(classify("subject matter expert vfx artists remote toulouse")).isEqualTo(Classification.out());
			assertThat(classify("anesthesia office manager")).isEqualTo(Classification.out());
			assertThat(classify("director biometrics")).isEqualTo(Classification.out());
			assertThat(classify("personal care specialist full time")).isEqualTo(Classification.out());
			assertThat(classify("farm associate temporary to hire")).isEqualTo(Classification.out());
			assertThat(classify("ft shipping associate")).isEqualTo(Classification.out());
			assertThat(classify("explosive operations engineer")).isEqualTo(Classification.out());
			assertThat(classify("wellness recovery specialist")).isEqualTo(Classification.out());
			assertThat(classify("kitchen shift lead dashmart")).isEqualTo(Classification.out());
			assertThat(classify("budget reporting manager")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheDomainsTheFalseAcceptsNamed() {
			// Four of this sample's five false accepts were one word away from decided: the domain
			// the title named was physical or financial and no marker held it.
			assertThat(classify("water systems specialist")).isEqualTo(Classification.out());
			assertThat(classify("ict infrastructure design engineer i low voltage")).isEqualTo(Classification.out());
			assertThat(classify("head of graphics at private equity insights")).isEqualTo(Classification.out());
			// The post that hires nobody, in the shape this sample wrote it: a paid research study
			// and a talent community in the plural.
			assertThat(classify("it directors short term paid research opportunity itsm esm platforms canada"))
				.isEqualTo(Classification.out());
			assertThat(classify("talent communities account executives")).isEqualTo(Classification.out());
		}

		@Test
		void readsTheSoftwareTheMissesAndTheUnknownPileNamed() {
			// A data warehouse is software however the warehouse marker reads the word, and a
			// machine learning engineer is one whatever science the title says it serves.
			assertThat(classify("data warehouse engineer")).isEqualTo(Classification.in());
			assertThat(classify("machine learning engineer physical sciences")).isEqualTo(Classification.in());
			assertThat(classify("incident response engineer")).isEqualTo(Classification.in());
			assertThat(classify("consultant digital forensic and incident response dfir remote")).isEqualTo(Classification.in());
			assertThat(classify("postgresql dba")).isEqualTo(Classification.in());
			assertThat(classify("embedded software engg")).isEqualTo(Classification.in());
			assertThat(classify("datapath engineer portworx")).isEqualTo(Classification.in());
			assertThat(classify("research engineer model training post training")).isEqualTo(Classification.in());
		}

		@Test
		void pinsWhatTheNewMarkersCost() {
			// The chemistry marker beats the software qualifier the way step 4 says it must, so a
			// computational chemistry role written as software engineering decides out.
			assertThat(classify("staff software engineer machine learning and computational chemistry"))
				.isEqualTo(Classification.out());
			// The restaurant server and the server engineer are one word, so the word is not a head:
			// 132 titles in this corpus name it and they split down the middle.
			assertThat(classify("server engineer")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

	}

	@Nested
	class Matching {

		@Test
		void readsAWordOnlyWhereTheTitleNamesItWhole() {
			// "container" carries the letters of the "ai" qualifier and names nothing of the kind.
			assertThat(classify("container engineer")).isEqualTo(Classification.unknown(UnknownReason.DOMAIN_AMBIGUITY));
		}

		@Test
		void readsATitleWhateverCaseItWasWrittenIn() {
			assertThat(classify("SOFTWARE Engineer")).isEqualTo(Classification.in());
		}

		@Test
		void saysNothingAboutATitleThatIsEmpty() {
			assertThat(classify("")).isEqualTo(Classification.unknown(UnknownReason.UNRULED));
		}

	}

	private Classification classify(String cleanedTitle) {
		return this.classification.classify(cleanedTitle);
	}

}
