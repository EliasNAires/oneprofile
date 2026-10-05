package oneprofile.backend.workers.classification;

import static oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.IN;
import static oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.OUT;
import static oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum.UNKNOWN;
import static oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum.DOMAIN_AMBIGUITY;
import static oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum.SCOPE_AMBIGUITY;
import static org.assertj.core.api.Assertions.assertThat;

import oneprofile.backend.storage.normalizedvacancy.ClassificationStateEnum;
import oneprofile.backend.storage.normalizedvacancy.UnknownReasonEnum;
import org.junit.jupiter.api.Test;

class BodyClassificationRuleTest {

	private static final String DUTIES = "You will partner with product and operations leaders to define metrics, "
			+ "build the reporting that runs the business and present findings to executives every quarter. You will "
			+ "join a team of eight and report to the director of the function. ";

	/** What a description says around the work, which settles nothing. */
	private static final String OFFER = " We offer a hybrid schedule, a learning budget and private health cover.";

	private final BodyClassificationRule rule = new BodyClassificationRule();

	@Test
	void aRoleThatRequiresANamedProgrammingLanguageIsIn() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: 3+ years of experience with Python for analysis and automation."))
			.isEqualTo(IN);
	}

	@Test
	void aScopeRoleWhoseWorkIsDescribedWithNoSoftwareBackgroundIsOut() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY, DUTIES + "Requirements: a passion for numbers and Excel."))
			.isEqualTo(OUT);
	}

	@Test
	void aVacancyWithoutADescriptionOfItsWorkIsUnknown() {
		assertThat(classify("Business Analyst", SCOPE_AMBIGUITY, "")).isEqualTo(UNKNOWN);
		assertThat(classify("Business Analyst", SCOPE_AMBIGUITY, "Description here")).isEqualTo(UNKNOWN);
		assertThat(classify("Security Specialist", SCOPE_AMBIGUITY,
				"To view more information about Careers with GDBA visit follow this link back to the Join Our Team page."))
			.isEqualTo(UNKNOWN);
	}

	@Test
	void aDomainRoleWhoseDescriptionNamesAnotherDomainIsOut() {
		assertThat(classify("Project Engineer", DOMAIN_AMBIGUITY,
				"You will manage HVAC and plumbing installations on commercial construction sites, review shop "
						+ "drawings, coordinate subcontractors and keep the owner informed of schedule and budget. You will report to "
						+ "the project manager and walk every site at least once a week."))
			.isEqualTo(OUT);
	}

	@Test
	void aDomainRoleWhoseDutiesCouldBeSoftwareOrNotIsUnknown() {
		assertThat(classify("Controls Engineer", DOMAIN_AMBIGUITY,
				"You will maintain the control systems that keep our operation running, troubleshoot faults "
						+ "as they arise, document changes and work with the operations team on improvements. You will report to the "
						+ "head of operations and join the on-call rotation."))
			.isEqualTo(UNKNOWN);
		assertThat(classify("Controls Engineer", SCOPE_AMBIGUITY,
				"You will maintain the control systems that keep our operation running, troubleshoot faults "
						+ "as they arise, document changes and work with the operations team on improvements. You will report to the "
						+ "head of operations and join the on-call rotation."))
			.isEqualTo(OUT);
	}

	@Test
	void internalControlSystemsAreNotWorkThatCouldBeSoftware() {
		assertThat(classify("Compliance Engineer", DOMAIN_AMBIGUITY, DUTIES
				+ "You will maintain the internal control systems that keep our financial reporting compliant."))
			.isEqualTo(OUT);
	}

	@Test
	void aWordThatCouldBeSoftwareOrNotLeavesNoDescribedRoleUnknown() {
		assertThat(classify("Revenue Operations Engineer", DOMAIN_AMBIGUITY, "Our platform brings automation and "
				+ "simulation to embedded teams. You will own the sales forecast, run pipeline reviews with account "
				+ "executives and keep the territory plan current. You will report to the head of revenue." + OFFER))
			.isEqualTo(OUT);
	}

	@Test
	void aBackgroundListedOnlyAsAPlusDoesNotCount() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: strong Excel skills. Experience with Python is a plus."))
			.isEqualTo(OUT);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: strong Excel skills. Nice to have: familiarity with Python."))
			.isEqualTo(OUT);
	}

	@Test
	void aPreferredBackgroundCounts() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: strong Excel skills. Experience with Python preferred."))
			.isEqualTo(IN);
	}

	@Test
	void aPostThatHiresNobodyIsOutHoweverEngineeringItSounds() {
		assertThat(classify("Design Engineer", DOMAIN_AMBIGUITY,
				"This is a Prospective/Pipeline Posting — Not an Active Role. " + DUTIES + "You write Python daily."))
			.isEqualTo(OUT);
		assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
				"This is a general application track for the product management positions at Canonical. " + DUTIES
						+ "You know Python."))
			.isEqualTo(OUT);
		assertThat(classify("Project Manager", SCOPE_AMBIGUITY,
				"This is not an active opening at this time, but we anticipate hiring for this opportunity. " + DUTIES
						+ "You know Python."))
			.isEqualTo(OUT);
	}

	@Test
	void aVacancyThatOnlyMentionsATalentNetworkIsStillAVacancy() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: Python. Join our Talent Network to stay up to date on job opportunities."))
			.isEqualTo(IN);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY, DUTIES + "Requirements: Python. If you wish to be "
				+ "considered for a position where there is not an active job posted, search for our General Application."))
			.isEqualTo(IN);
	}

	@Test
	void aRoleThatCarriesAQuotaIsOutWhateverBackgroundItAsksFor() {
		assertThat(classify("Customer Engineer", DOMAIN_AMBIGUITY, DUTIES
				+ "You will operate as a quota-carrying technologist. Requirements: Python and Kubernetes experience."))
			.isEqualTo(OUT);
		assertThat(classify("Consulting Engineer", DOMAIN_AMBIGUITY,
				DUTIES + "Achieving Annual Sales Quota: create compelling presentations. Requirements: Python."))
			.isEqualTo(OUT);
	}

	@Test
	void aRoleThatOnlyTracksQuotasDoesNotSell() {
		assertThat(classify("Sales Operations Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Support territory planning and quota tracking. Requirements: advanced Python."))
			.isEqualTo(IN);
		assertThat(classify("Sales Engineer", DOMAIN_AMBIGUITY, DUTIES
				+ "Run the technical side of sales cycles in partnership with quota-carrying Account Executives. "
				+ "Requirements: Python."))
			.isEqualTo(IN);
		assertThat(classify("Technical Account Manager", SCOPE_AMBIGUITY,
				DUTIES + "You will not carry a sales quota. Requirements: Python."))
			.isEqualTo(IN);
	}

	@Test
	void aRoleThatRequiresADegreeInAnotherEngineeringIsOutEvenWhenItWritesCode() {
		assertThat(classify("Test Engineer", DOMAIN_AMBIGUITY, DUTIES
				+ "Requirements: Bachelor's degree in Electrical Engineering. Write test scripts in Python."))
			.isEqualTo(OUT);
	}

	@Test
	void aDegreeInAnotherEngineeringWithComputerScienceAsTheAlternativeIsNoSuchRequirement() {
		assertThat(classify("Test Engineer", DOMAIN_AMBIGUITY, DUTIES
				+ "Requirements: BS in Electrical Engineering, Computer Engineering or Computer Science. "
				+ "Write test scripts in Python."))
			.isEqualTo(IN);
	}

	@Test
	void aRoleThatRequiresALicenceOrAFieldDegreeIsOut() {
		assertThat(classify("Project Engineer", DOMAIN_AMBIGUITY,
				DUTIES + "Requirements: an active PE license. Automate calculations in Python."))
			.isEqualTo(OUT);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: a bachelor's degree in Finance or Accounting. Advanced SQL."))
			.isEqualTo(OUT);
	}

	@Test
	void fieldKnowledgeListedAsAPlusOrPreferredIsNoRequirement() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: Python. A degree in Finance is a plus."))
			.isEqualTo(IN);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: Python. A degree in Finance preferred."))
			.isEqualTo(IN);
	}

	@Test
	void everyFormOfAStatedSoftwareBackgroundIsIn() {
		for (String background : new String[] { "a degree in Computer Science or equivalent experience",
				"experience in Java or TypeScript", "C++ on embedded targets", "C# and .NET", "writing complex SQL queries",
				"hands-on AWS and Kubernetes experience", "familiarity with Terraform and Linux", "solid PostgreSQL skills",
				"verification in SystemVerilog" }) {
			assertThat(classify("Product Manager", SCOPE_AMBIGUITY, DUTIES + "Requirements: " + background + "."))
				.as(background)
				.isEqualTo(IN);
		}
	}

	@Test
	void aRoleThatWorksOnEngineerFacingArtifactsIsIn() {
		for (String duty : new String[] { "Read the source code to reproduce customer issues",
				"Own the REST API specifications for the platform", "Write and debug code independently",
				"Review code and AI-generated output", "Contribute to software architecture decisions",
				"Lead system design and pay down tech debt", "Run code reviews for the squad",
				"Investigate application logs to find the root cause" }) {
			assertThat(classify("Technical Account Manager", SCOPE_AMBIGUITY, DUTIES + duty + "."))
				.as(duty)
				.isEqualTo(IN);
		}
	}

	@Test
	void aTechnologyTheCompanyMentionsIsNotABackgroundTheRoleAsksFor() {
		assertThat(classify("Technical Consultant", SCOPE_AMBIGUITY, "PP Enterprise Solutions works alongside "
				+ "best-in-class partners including Adobe, AWS, Braze and Google. " + DUTIES))
			.isEqualTo(OUT);
		assertThat(classify("Analytics Associate Director", SCOPE_AMBIGUITY,
				DUTIES + "We offer training and full access to Google Cloud Platform."))
			.isEqualTo(OUT);
	}

	@Test
	void aMentionIsReadAgainstTheWordsAroundItNotAWholeUnpunctuatedList() {
		assertThat(classify("Stress Analyst", DOMAIN_AMBIGUITY, "Responsibilities Perform hand calculations and finite "
				+ "element analysis of airframe structures Prepare stress reports for certification Support the design "
				+ "team with sizing studies Requirements Five years of stress analysis experience in aerospace Ability to "
				+ "work in a fast paced team Our analysis toolchain runs on a cluster we automate with in-house Python "
				+ "tools maintained by another team" + OFFER))
			.isEqualTo(OUT);
	}

	@Test
	void sourceCodeNamedInAnExportControlNoticeIsNoArtifactOfTheRole() {
		assertThat(classify("Field Engineering Manager", DOMAIN_AMBIGUITY, DUTIES + "If access to export-controlled "
				+ "technology or source code is required for performance of job duties, the candidate must qualify."))
			.isEqualTo(OUT);
	}

	@Test
	void aToolTheCompanyRunsOnIsNotABackgroundTheRoleAsksFor() {
		assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
				DUTIES + "Our product runs production-grade infrastructure across AWS, Datadog and GitHub."))
			.isEqualTo(OUT);
		assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: hands-on experience with GitHub Actions and CI/CD."))
			.isEqualTo(IN);
	}

	@Test
	void aComputerScienceDegreeOfferedBesideNonTechnicalOnesIsNoSoftwareBackground() {
		assertThat(classify("Product Intern", SCOPE_AMBIGUITY, DUTIES
				+ "Pursuing a Bachelor's degree in Business, Engineering, Computer Science, or related field."))
			.isEqualTo(OUT);
	}

	@Test
	void aRoleWhoseWorkIsWritingSoftwareIsIn() {
		assertThat(classify("Developer", DOMAIN_AMBIGUITY, "You will design, build and ship backend services, write "
				+ "clean and tested code and review your teammates' work. You will join a team of eight and report to "
				+ "the director of engineering." + OFFER))
			.isEqualTo(IN);
	}

	@Test
	void aDomainRoleWhoseWorkIsPlainlyNotSoftwareIsOut() {
		assertThat(classify("Financial Analyst", DOMAIN_AMBIGUITY, "You will build the monthly forecast, reconcile "
				+ "accounts with the controller, prepare board materials and advise budget owners on their spend. You "
				+ "will join a team of eight and report to the head of finance." + OFFER))
			.isEqualTo(OUT);
	}

	@Test
	void aDomainRoleThatNamesAnEngineeringDisciplineOutsideSoftwareIsOut() {
		assertThat(classify("Design Engineer", DOMAIN_AMBIGUITY, "You will maintain the control systems of the plant, "
				+ "produce mechanical drawings in SolidWorks and sign off fabrication packages. You will join a team of "
				+ "eight and report to the plant manager." + OFFER))
			.isEqualTo(OUT);
	}

	@Test
	void aSkillOfferedAsOneChoiceAmongToolsThatAreNotSoftwareIsNotAskedFor() {
		for (String requirement : new String[] { "experience with SQL or BI tools", "Excel, SQL or Looker",
				"proficiency in Stata, R, Python or SAS" }) {
			assertThat(classify("Data Analyst", SCOPE_AMBIGUITY, DUTIES + "Requirements: " + requirement + "."))
				.as(requirement)
				.isEqualTo(OUT);
		}
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: strong SQL and Excel or Google Sheets."))
			.isEqualTo(IN);
	}

	@Test
	void whatTheCompanySaysOfItselfOrWhatTheRoleDoesNotNeedIsNotAskedFor() {
		assertThat(classify("Solutions Consultant", SCOPE_AMBIGUITY,
				DUTIES + "We specialise in Kubernetes and bring deep expertise to every customer."))
			.isEqualTo(OUT);
		assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
				DUTIES + "You don't need to know Python or SQL for this role."))
			.isEqualTo(OUT);
	}

	@Test
	void aToolUsedOnlyToTrackWorkIsNoSoftwareBackground() {
		assertThat(classify("Project Manager", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: experience with Jira, Confluence and Azure DevOps boards."))
			.isEqualTo(OUT);
	}

	@Test
	void fieldExpertiseRequiredInItsOwnRightIsOutEvenBesideASoftwareBackground() {
		for (String expertise : new String[] { "A subject matter expert in financial services is a must",
				"You understand how fraud attacks work", "Experience analysing biological datasets",
				"Experience in RF and satcom engineering", "Hands-on experience with avionics and flight control",
				"Deep knowledge of insurance products", "Strong understanding of trading and financial crime", "Knowledge of anti-money-laundering rules",
				"Financial-crime knowledge is required" }) {
			assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
					DUTIES + "Requirements: advanced SQL and Python. " + expertise + "."))
				.as(expertise)
				.isEqualTo(OUT);
		}
	}

	@Test
	void yearsInTheJobNamedAfterAFieldOrFieldKnowledgeTheCompanyClaimsAreNoFieldExpertise() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: five years of marketing analytics experience and advanced SQL."))
			.isEqualTo(IN);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "We bring deep knowledge of payments to our customers. Requirements: advanced SQL."))
			.isEqualTo(IN);
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY, DUTIES + "Combining data, research, technology and "
				+ "trading expertise has shaped QRT's collaborative mindset. Requirements: advanced SQL."))
			.isEqualTo(IN);
	}

	@Test
	void anArtifactPhraseThatIsNotSoftwareWorkOrNotTheRolesOwnWorkIsNotIn() {
		for (String duty : new String[] { "Lead system design for pole-line and conduit projects",
				"Partner with engineers on architecture decisions and system design discussions",
				"Ramp on an unfamiliar codebase alongside the engineering team",
				"Run log analysis and crawl audits for technical SEO",
				"Support source code review in patent litigation and disputes" }) {
			assertThat(classify("Product Designer", SCOPE_AMBIGUITY, DUTIES + duty + "."))
				.as(duty)
				.isEqualTo(OUT);
		}
	}

	@Test
	void architectureCallsTheRoleMakesItselfAreIn() {
		assertThat(classify("Product Designer", SCOPE_AMBIGUITY,
				DUTIES + "You will own architecture decisions and lead system design discussions for the platform."))
			.isEqualTo(IN);
	}

	@Test
	void aManagerWhoLeadsSoftwareEngineersIsIn() {
		assertThat(classify("Engineering Manager", DOMAIN_AMBIGUITY,
				DUTIES + "You will lead a team of eight software engineers building customer-facing products."))
			.isEqualTo(IN);
		assertThat(classify("Engineering Manager", DOMAIN_AMBIGUITY,
				DUTIES + "We hold every manager to a staff-engineer-level technical bar."))
			.isEqualTo(IN);
		assertThat(classify("Program Manager", SCOPE_AMBIGUITY,
				DUTIES + "You will manage relationships with software developers across our partners."))
			.isEqualTo(OUT);
	}

	@Test
	void aRoleThatBuildsInALowCodeToolIsIn() {
		for (String duty : new String[] { "Build apps and flows in Power Apps and Power Automate",
				"Configure and implement Dynamics 365 for our clients", "Develop case management applications on Pega",
				"Automate our sales tool stack with Clay and Zapier" }) {
			assertThat(classify("Solutions Consultant", SCOPE_AMBIGUITY, DUTIES + duty + "."))
				.as(duty)
				.isEqualTo(IN);
		}
	}

	@Test
	void aRoleThatOnlyReportsInOrAdministersALowCodeToolIsNot() {
		assertThat(classify("Business Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Build reports and dashboards in Power Apps for the sales team."))
			.isEqualTo(OUT);
		assertThat(classify("Business Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Administer Dynamics 365 users, permissions and page layouts."))
			.isEqualTo(OUT);
	}

	@Test
	void aLowCodeToolNamedInTheNextSentenceIsNotWhatTheRoleBuilds() {
		assertThat(classify("Solutions Consultant", SCOPE_AMBIGUITY, DUTIES
				+ "Customers buy our automation platform. As a consultant at Appian you will advise their executives."))
			.isEqualTo(OUT);
	}

	@Test
	void technicalExperienceInASoftwareFieldIsASoftwareBackground() {
		assertThat(classify("Solutions Consultant", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: experience with observability and developer tools."))
			.isEqualTo(IN);
	}

	@Test
	void aScientistWhoBuildsMachineLearningSystemsIsIn() {
		assertThat(classify("Applied Scientist", DOMAIN_AMBIGUITY,
				DUTIES + "You will build and deploy machine learning models for search ranking."))
			.isEqualTo(IN);
	}

	@Test
	void dutiesWrittenInAnotherLanguageAreRead() {
		assertThat(classify("Projektingenieur", DOMAIN_AMBIGUITY,
				DUTIES + "Ihre Aufgaben: Programmierung von Webanwendungen und Pflege der Schnittstellen."))
			.isEqualTo(IN);
		assertThat(classify("Ingeniero de Proyectos", DOMAIN_AMBIGUITY,
				DUTIES + "Requisitos: experiencia en programación de aplicaciones."))
			.isEqualTo(IN);
	}

	@Test
	void aDegreeListCountsOnlyWhenEveryFieldItAcceptsIsAComputingField() {
		for (String degree : new String[] { "Computer Science, Physics, Mathematics or Engineering",
				"Computer Science or a related STEM field", "Computer Science or Electrical Engineering" }) {
			assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
					DUTIES + "Requirements: a bachelor's degree in " + degree + "."))
				.as(degree)
				.isEqualTo(OUT);
		}
		for (String degree : new String[] {
				"Computer Science or Information Systems, or equivalent technical experience",
				"Information Technology" }) {
			assertThat(classify("Product Manager", SCOPE_AMBIGUITY,
					DUTIES + "Requirements: a bachelor's degree in " + degree + "."))
				.as(degree)
				.isEqualTo(IN);
		}
	}

	@Test
	void aFieldDegreeOfferedBesideAComputingOneIsNoFieldRequirement() {
		assertThat(classify("Data Analyst", SCOPE_AMBIGUITY,
				DUTIES + "Requirements: a degree in Finance or Information Systems. Advanced SQL skills."))
			.isEqualTo(IN);
	}

	private ClassificationStateEnum classify(String title, UnknownReasonEnum reason, String description) {
		return this.rule.classify(title, reason, description);
	}

}
