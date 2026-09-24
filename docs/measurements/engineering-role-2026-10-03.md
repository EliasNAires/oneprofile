# A closing measurement of the engineering-role classifier

Run on the development machine on 2026-09-24, after #10 closed at iteration 12. The corpus is the raw
snapshot already loaded in the development database — 179 098 vacancies, classified by iteration 11's
rules. Its counts match iteration 12's report exactly.

Iteration 12 labelled and scored iteration 11's sample. It changed no rule and drew no sample. This
run adds one more fresh sample of the classifier the loop leaves, labelled under the criterion **as
it stands after the close**. The close added four sentences: engineering managers are `IN`, product
managers are `UNKNOWN` / `scope_ambiguity`, program and project managers are `UNKNOWN` /
`scope_ambiguity`, and `scope_ambiguity` is defined. Iteration 12's labels predate those sentences.
This run measures the rules against them. **No rule changed**, and #10 stays closed.

The labels are `src/test/resources/labels/engineering-role-2026-10-03.tsv`. As in iterations 5
through 12, a subagent wrote them from the criterion and the 1000 bare titles. It had no access to the
classifier's source, the iteration reports or the earlier label files. The criterion was read at
revision `763ca05`.

## The numbers

These are 100 `IN`, 500 `OUT` and 400 `UNKNOWN` rows drawn at random, excluding the 11 000 ids held
by earlier fixtures and the 100 calibration titles; 164 685 rows were eligible. A mistake is any row
whose state differs from the label.

| Gated number | Iteration 12 | This run | Gate |
| --- | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 3.80% | **3.80%** (19 of 500) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 27.00% | **34.00%** (34 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 65.25% | **57.25%** (229 of 400) | ≤ 10% |
| `unruled` — of the whole corpus | 3.49% | **3.49%** (6 257) | ≤ 4% |

Reported alongside:

- **`OUT`-stratum rows labelled `IN`**: **3** of 500, the same as iteration 12. They are
  `Head of Engineering Sales Marketing Tools`, `Battery Software Integration Engineer Energy
  Storage` and `Helpdesk Technician`.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **45**. The classifier called
  29 of them `IN` and 16 `OUT`.
- **The labeller's own unknown pile** is 216 rows: 127 `domain_ambiguity`, 80 `scope_ambiguity`
  and 9 `unruled`.
- **What the `UNKNOWN` stratum turned out to be**, split by the classifier's reason:

  | Classifier's reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |
  | --- | ---: | ---: | ---: |
  | `domain_ambiguity` (245) | 32 | 72 | 141 |
  | `scope_ambiguity` (34) | 10 | 8 | 16 |
  | `unruled` (121) | 9 | 98 | 14 |

## Reading the numbers

**The `IN` stratum error rose because the rules lag the criterion.** Of its 34 errors, 13 are
`scope_ambiguity` labels. Most follow sentences 2 and 3 of the close directly: `Technical Project
Manager` (twice), `Technical Program Manager QA Developer Experience`, `Product Manager Platform
Engineering`, `Project Manager Data Centers`. The rest are functional consultants on a vendor
package (`NetSuite Consultant`, `Lead SAP PP DS Consultant`). The classifier still decides these by
their qualifier. Sixteen more are `domain_ambiguity`, mostly systems, integration and robotics
engineers (`Control Systems Engineer`, `Vehicle Integration Engineer`, `Systems Test Engineer`). Five
are `OUT`, such as `Application Engineer CNC Sheet` and `Optical Integration Engineer`.

**The `UNKNOWN` stratum fell 8 points**, but its shape is unchanged. The labeller decides `unruled`
rows nearly always (107 of 121), mostly `OUT`. It decides `domain_ambiguity` rows about two times
in five.

**A two-sample reading of the closing classifier:** its `OUT` decisions are reliable at 3.80% twice.
Its `IN` decisions are wrong about a third of the time against the current criterion. Its
`UNKNOWN` pile is roughly 40% undecidable from the title, 60% titles the rules could still decide.

## The sample

The 1000 rows as id and cleaned title, in the drawn (shuffled) order.

```
38444	User Research Intern
873	Anesthesiologist Lake Oswego
80628	FINANCIAL OPERATIONS ANALYST II
90306	Associate Scientist Innovation Scientific Affairs
176607	Facilities Specialist Data Center Memphis
41647	Associate Catalog Operation 電商營運審核專員
87573	Board Certified Behavior Analyst BCBA
8030	Seasonal Sales Associate Part Time Plaza at Frotenac
171087	Développeur d agents Équipe produits
25459	Compliance Officer
144264	Propulsion Engineer Merlin Hardware Development
78650	Bilingual Patient Engagement Associate
15046	Entry Level Project Manager Construction
131781	EDD Analyst
79308	Field Service Technician Level III
130718	User Experience Researcher The Points Guy
156343	Product Manager Reporting and Measurement
112726	Brokerage Team Leader
34137	Child Behavior Interventionalist
29935	Derivatives Risk Control Specialist
58806	Automotive Detailer
129359	TechOps Engineer
97752	Relationship Manager Allentown PA
53507	Part Time Nurse Practitioner Physician Assistant Greensboro Winston Salem NC
122952	Data Entry Clerk Graduates AI Training Tauranga New Zealand
48989	Material Handler
80581	BUSINESS ANALYST II
3624	Business Development Representative
17574	NTN Radio Access and Mobility Engineer
57389	Heavy Equipment Field Technician Mechanic
80585	COMMERCIAL ANALYST III SHOPPING BUSINESS MKP
161949	Sales Engineer New York City
151403	Personal Trainer
85439	Group Product Manager Product Foundations
124033	Linguist Translator Graduates AI Training Dunedin New Zealand
54397	Assistant Kids Club Manager
52946	Business Analyst Automation Core Banking
89371	Laser Welding Operator 1st Shift
174865	Business Development Representative
98141	Director Applied AI
156338	Product Manager Buying Optimization
135125	Manager People Culture Operations
147789	Consultor a Comercial Externo Orlândia SP
144821	IT Wireless Network Engineer
16850	Engineer Global Product Support
133257	Android Engineer Government Products
132579	Content Creator and Social Media Manager Contract
108838	Growth Operations Manager
19704	Product Specialist Productivity
91707	Social Worker Mental Health
49654	Project Executive
162204	Asset Based Finance Asset Management Analyst
149988	Staff Market Intelligence Researcher
93619	Mechanical Engineer Stator Rotor
51765	Pediatric GVG Wichita
128231	Service Delivery Lead Our Future Health Reading
98641	Equipment Traveling Mechanic
20585	HV Cables Design Engineer
147246	Agente Stone Consultor a Comercial Externo Afenas MG
143938	Mechanical Engineer Flight Termination System Starship
31725	Service Designer Product Experience
83636	Store Collega JD Rotterdam Alexandrium 8 16H
14589	Director Product
45144	Solutions Architect EMEA Startups France
119806	Sales Account Executive
109372	Civil Drafting Internship Water Wastewater
160762	Sales Manager
140769	Mid Level Digital Forensic Analyst
88552	Technicien.ne concepteur.trice en structure
124431	Pathologists Freelance Remote Portland US
9358	Construction Project Manager Intern Summer 2027
20374	Product Supply Planning Manager
86990	Internal Communications Employer Brand Co op Spring 2027
175545	Associate Media Planning Pakistan
29670	Bridge Design Engineer
128281	Specialty Doctor in Respiratory Medicine London
22272	Licensed Practical Nurse Private Duty Pediatrics
101368	Media Coordinator
157741	Staff Compensation Analyst
166917	Test Specialist
136121	Equipment Operator NP 3rd Shift
73224	Behavior Technician RBT Shelton
12184	Staff Reliability Engineer
163548	Strategic Project Lead Code
53717	Mainframe Developer
167696	Veterinary Technician Swing
151428	Personal Trainer
37739	Car Detail Team Lead
96029	Host
115837	Database Engineer
169645	Veterinary Technician
136493	Director Applied AI Forward Deployed Engineering
96422	Food Service Manager
89841	Account Executive Business Intelligence based in Spain German speaker
164820	Digital marketing Danish Speaker
139851	Part Time Caregiver Multiple Shifts
16986	IOP Clinician at South Shore Hospital
80311	Go To Market Engineer
155406	ICT Summer Intern
66369	Account Director
131927	Tax Analyst VAT
55806	Member Experience Manager
43747	Plan Check Engineer FT Hybrid
110366	Virtual Family Nurse Practitioner NH Licensed
135917	Scientist Analytical Development
84150	Utility Relief
69585	Engineering Manager Frameworks
103776	Project Engineer
38703	Multi Site Maintenance Supervisor
133247	Product Lead
278	2K Games Technical Art Graduate Program Vancouver
152599	Personal Trainer
31170	BA Insurance
73425	Dialysis Technician Thousand Oaks CA 91359
7679	Sales Service Lead Bellevue Square
4938	Consultor de Vendas Fortaleza CE Major Facundo
155886	Cloud Systems Engineer II
21378	Bilingual Spanish Registered Nurse RN
125991	Band 5 Accident Emergency A&E Harlow
31356	Engenheiro de Projetos Sênior Híbrido Santos SP e Rio de Janeiro RJ
147456	Consultor a Comercial Externo Vaga Afirmativa para Pessoas com Deficiência PcD Nova Iguaçu RJ
25093	FDOT Project Surveyor
125957	Band 5 6 Locum General Radiographer Milton Keynes
64079	Supply Chain Business Operations Intern Summer 2027
73437	Helper Needed Light Home Support Transportation Fort Lauderdale FL
99971	Product Support Engineer
116510	Product Master Data Contractor
64338	Project Administrator Utility Construction
86376	Inside Sales Representative MM Bilingual German DACH
121276	GTM Engineer
48369	Restaurant Shift Leader Back Bay
85148	Campus Quantitative Trader Intern
159396	Physical Occupational Therapist Consultant
157091	Broadcast Engineer Lead
53355	Care Coordinator Embedded Partnered Dialysis Clinics
137095	Training Specialist
47937	VP Financial Services Insurance Strategy
137195	Lead Engineering Fleet Operations
105057	Logistics Manager
152392	Personal Trainer
62538	Treasury Specialist
120712	Investor Relations Strategy Dutch speaking
47853	Warehouse Associate
63161	Staff Product Designer Consumer
112845	Associate Low Carbon
25217	HOUSTON Site Development New Grad
3098	Team Lead of PPC Media Buyers
89747	Center Based Behavior Technician
58514	Badplaner
169068	Medical Director Orange CT
53848	Engineering and Operations Intern
142699	Expression of Interest Production
27425	Psychiatrist MD
136379	Frontier Agents Engineer
56395	Service Associate New Gym Opening
107158	Communications Specialist
69281	Project Manager Industrial Construction Facilities
76653	Transaction Coordinator
97692	Intern
11536	Mission Operations Engineer Connected Warfare Active Clearance
11102	R&D Engineering Technician
48252	Food Service Worker Madison Square Park
156253	PhD Residency Utility Scale Power Electronics Winter 2026 Summer 2027
43363	Family Advocacy Program Assistant FAPA
25986	Marketing Intern NYC
29923	AML Specialist KYB Malaysia
42693	Staff Mobile Engineer L6 1 Marketing Product Engineering
59550	People Generalist
137464	2027 Athletic Training Student Intern Educational Internship Seattle Reign FC
59087	Research Scientist Autonomous Systems
174851	Staff Product Manager Developer tools
95343	Lead Manager IT Security Engineer
55503	Juice Bar Manager
165711	Stock Administrator
175791	Influencer Director EMEA
7662	Sales Associate Part Time Village at Rochester Hills
38646	Leasing Specialist
110391	Virtual Seasonal Family Nurse Practitioner CA Licensed
40606	Thought Leader Liaison New England
123308	France Residents Survey Participants Metz France
63597	Exchange Director Multifamily Single Family
130926	Product Manager XDR Exposure Management Security Solutions
12550	Scientist Molecular Biology
101273	Content Creator
56409	Service Associate Night
39122	Environmental Health and Safety Technician
66888	Sports Data Collector Football Marrakech Morocco
147162	Accountant Revenue Platform Accounting
53157	Unsolicited Application all genders
57917	Territory Account Manager
170088	Associate Director New Business Strategy Influencer Marketing
129726	Grupo QuintoAndar Vaga afirmativa para PcD Analista de Operações Vistoria e Laudos
176586	Electrical Engineer Memphis
38518	Paralegal
119558	Manager Client Finance
64528	Guidewire Engage Developer Hybrid US
12110	Staff Electrical Engineer Propulsion
77284	Executive Assistant Workplace Specialist
98168	Revenue Economics Analyst
129850	Bank Relationship and Analytics Manager
162234	Scientist Immunology Targeted LNP Delivery
72632	Veterinary Receptionist
175981	Online Marketeer
114557	HR Business Partner
166051	Provider Partnerships Associate Territory Sales
125557	Head of Product Marketing
163236	Software Engineer Backend Services
144519	Software Engineer Starship
79315	Field Service Technician Montreal CAN
93200	Building Engineer
123148	Environmental Research Graduates AI Training Charlotte US
117271	Customer Support Engineer
10201	Chief Engineer Fury Advanced Concepts
78834	Software Engineer Intern
50674	Analyst Investment Banking Los Angeles
66410	Campus Engagement Early Talent Recruiter
155490	Product
87282	Health Law Counsel Regulatory Transactional Practice Remote
12616	Anthropic Fellows Program AI Safety Security
41018	Treasury Analyst
162747	Class A CDL Driver Monday Friday
84017	Director of Accounting Operations
24554	Full Stack Software Engineer Zecure Gaming
137108	Full Stack Software Engineer
151954	Personal Trainer
34020	Center Based Child Autism Specialist
143545	GNC Engineer Phased Array Devices Starshield
110958	Seasonal Store Advisor Miami
64387	Fund Administration Operations Analyst
105834	PM Core Experience
133006	Staff Software Engineer Cash Forecasting
40161	New Grad Physician Assistant PA Urgent Care
133338	Quality Engineer
32675	Project Engineer Light Industrial
159658	Product Developer gn PCT
165259	Vein Specialist Physician Boca Raton Florida Up to 50K Sign on Bonus
71459	Channel Partners Director
153799	Personal Trainer
124620	Portuguese Fluent Speakers AI Training Switzerland
76337	Real Estate Acquisition Consultant
130737	Battery Software Integration Engineer Energy Storage
177927	Office Coordinator
71947	System Test Validation Engineer
77657	Accounts Payable
21112	Dental Assistant Barrie
119394	P&C Test Engineer II
47054	Creative Analyst Creative US Remote
45548	Strategic Account Executive
143839	Manager Starlink Enterprise Sales
400	Associate Dentist
116366	Ballgirl
53167	Account Executive Chicago
31189	Business Analyst Banking
58120	Yard Technician
114363	OVERDARE UGC Game Engineer
139953	Business Development Representative
126311	Band 6 7 Paediatric Audiologist East London
73284	Sales Director Multi Industry SME London
53982	Assistant Fitness Manager
171891	2027 Summer Intern BS SysEng Software Engineer
136128	Interim Low Bay Expeditor 9 30pm 6am
31157	Analista de sistemas Rio de Janeiro RJ Hibrido
122454	Canada Residents AI Trainers Estevan Canada
38228	Accelerations Programs Intern
117115	Facilities Specialist
115247	Hardware Engineering Program Manager
64901	Alarm and Security Installation Technician
78317	Analyst Associate Family Office Advisory New York and San Francisco
178058	Analyst Deal Desk Contract Analytics
81581	Staff Harness Engineer
132974	Software Engineer Banking Connectivity
120381	Institutional Investor Engagement German speaking
22981	PRN Private Duty Nurse LPN
169489	Veterinarian
38903	Over 18 Family Therapist
41731	쿠팡이츠서비스 CX 운영 및 기획 개선 담당자
109318	Digital Content Designer
28	Cyber Investigator
150644	Personal Trainer
123542	Hindi Fluent Speakers AI Training Philadelphia USA
14366	Staff Technical Architect
93191	Building Engineer
128581	IT Functional Analyst Finance Systems SAP Accounts Receivable
161183	Bilingual Hybrid Development Representative Mandarin
108406	Software Engineer
58558	Annuity Sales Representative
145118	Terrestrial Services Sales Manager Starlink Aviation
117537	NetSuite Consultant Non Profit
138431	A.I Engineering Intern
126591	Band 6 Locum Respiratory Physiologist Lincoln
123738	IT Technology Decision Makers Paid ITSM ESM Research Study El Paso US
131868	Sales Development Representative AMER
117528	Mohs Surgeon with Shareholder Track
137779	Engineering Director Developer Experience
138770	Director of Product Integrity Home Environment
38320	Risk Monitoring Analyst III
107901	Mission Operations AI Enablement Lead
171403	Technical Project Manager
127076	Band 7 MRI Radiographer Hertfordshire
129331	Technology Buyer
71658	Financial Operations Analyst
20030	Technical Support Engineer Axon 911
66194	Garage Door Technician Apprentice
62742	Backend Engineer Cortex IND
123031	Doctors AI Training Los Angeles US
147476	Exclusiva PcD Consultor a Comercial Interno Híbrido Rio de Janeiro RJ
8227	International Accounting Manager
117405	Product Analyst Middle+ Customer Care
97534	Seasonal Stylist Retail Part time
154815	Personal Trainer Grimsley TN
135096	Hotel Operations Manager New Opening
46687	Mission Director Cyber
58725	Talent Acquisition Partner Manila Call Center Leadership High volume Operations
103307	FP&A Analyst
91142	Social Worker Child Protection
19420	Reporter Axios Dallas
53694	IAM Analyst
137707	Associate Trainer
176151	D OOH Specialist
35815	Associate European Competition practice
21770	Early Intervention Pediatric Therapists OT PT SLP
43989	Vehicle Integration Engineer
138759	Director Engineering Advanced Development
85952	Deployment Engineer United States
91913	Social Work Looked After Children
157965	Account Director United States
15396	Project Manager
18655	Product Manager BSG
71888	Staff Accountant
144065	Operations Development Engineer Starlink
67198	Deep Foundation Driller
104987	Product Development Engineer II
41187	Bilingual Leasing Hub Specialist Sandy Springs GA
148334	Engineering Manager APAC EMEA Cards
143840	Manager Starlink Enterprise Sales
2289	Calibration Technician
101026	Credit Risk Analyst
89849	Area Director North Asia
69595	Infrastructure and MLOps Engineer
2777	Customer Solutions Consultant II
103265	Software Infrastructure Graduate 2027 Hong Kong
47256	Strategist
47423	Research Analyst Equities
161894	Software Engineer AutoTagging
98013	Brand Director
161306	Risk Analyst Strategy Capabilities
140096	Software Engineer II Device Cloud
131777	CX Operations Manager Tooling
104055	Organ Health Specialist Gen Neph
113655	Aide aux personnes âgées
40236	Radiologic Technologist Urgent Care
104242	Web Production Intern Summer 2027
49051	DevSecOps
123283	Fluent Russian Speakers Latvia Task Based Remote Flexible
4924	Cientista de Dados Sênior Campinas SP
20720	Analista de Produtos Seguros
45218	Engineering Manager Pipelines Engine
26331	Staff Data Analyst Block Compliance
159347	Battery Pack Specialist
13992	Enterprise Account Executive 4th Estate
63485	Government Affairs Strategic Partnership Lead
4323	Investment Operations Specialist Freelance Project
126287	Band 6 7 Locum Remote Adult Speech and Language Therapist Shipley
161032	Engineering Manager Manage Money
104467	Business Analyst Revenue Unit Economics
50774	C++ Software Engineer DV Commodities London
65123	Developer Experience DevEx
2089	Performance Reporting Architect
29682	Transportation Engineer EI EIT
114023	Auxiliaire de vie
114225	Staff Engineering Program Manager Core Tech
1260	Administrative Assistant
39826	Head of Information Security
38435	Strategic Finance Intern
61870	SFMC Engineer
101386	P2P Team Lead
29884	Legal Compliance AI Data Expert
93673	Staff Machine Learning Engineer ADAS Autonomous Driving
18230	Controller
176072	Search Executive GOC
77674	Cybersecurity Partner
127749	Locum Band 8C Aseptic Pharmacist Salisbury
64827	Supplier Industrialization Engineer
31328	Data Validation Liquidity
118772	Service Desk Engineer Technology
13247	Manager Project Development Hybrid
75035	VDC Engineer
167933	Test Engineer Alarms
144908	PDK Engineer Starlink Akoustis
44479	Finance Intern
141509	Barback Babington House
78971	Software engineer
50334	SDE UIUX
73994	Hiring Task Based Helpers for Seniors in Phoenix Arizona 85009
40474	Steward Toronto
75860	Insurance Producer Oceanside CA
162012	Edge Platform Support Engineer
39517	Nurse Practitioner Physician Assistant Pool PRN
18494	Investor Relations CO OP
137207	Manager Project Steering
77715	HVAC Installation Technician
142108	Staff DevOps Engineer
169725	Fairfax Animal Hospital Veterinary Surgical Assistant
124196	Mexico Residents Survey Participants Ecatepec Mexico
17085	Portfolio Performance Specialist
88555	Technicien.ne dessinateur.trice en structure
9514	Intercedent STEM
49528	Data Engineer Cloud SaaS Integrations
8673	AlphaSights x AUB From Brief to Close Workshop September 15 2026
11057	Program Manager Open Architecture
90062	AI Residency Program Material Science 2026 Cohort
30371	Global Tax Trainee
121628	AI Trainer Aeronautical Engineers CAD Expertise Remote Advisory Japon
123506	Greece Residents Survey Participants Komotini Greece
78491	iFood Pago Analista de Growth CRM Crédito
93193	Building Engineer
19891	Site Reliability Engineer I
157922	Simulator Engineer Training Simulation Engineering
89611	Specialist Safety and Heath
48737	Delivery Analyst Risk Audit Americas
73928	Hiring Caregivers for Seniors in Fort Wayne Indiana 46809
52064	Outside Sales Engineer
38107	Software Engineer Backend
160633	Solution Consultant Bilingual Spanish
177390	Assessoria de Investimentos XP Future
141134	Vertriebsprofi Kreditberatung
126248	Band 5 to 8 Neuro Physiotherapist Haslemere
129528	Director of Product Search
160749	Lancaster University Virtual Asia Careers Fair 8th 9th September 2026
107394	Talent Acquisition Coordinator
101366	Lead Product Manager
87982	Program Director
127529	Consultant in Orthopaedic Surgery Upper Limb Perm or FTC 12 month West Norfolk
171084	Coordonnateur.trice événements marketing
51038	Product Manager Risk Fraud
4568	Payroll Specialist Freelance Project
140456	Venture Investment Analyst
73357	Compassionate Caregiver Needed Crossville TN
98395	Data Center Construction Mid to Project Manager Cost
93232	Lead Building Engineer
1067	Enterprise Account Executive Toronto
14455	Product Manager Simulation
123843	Javascript Developers AI Training Nottingham UK
136137	Production 2 10am 6 30pm
93230	Lead Building Engineer
24976	SOC Analyst
72670	Quality Governance Lead
141832	Chef de Partie High Road House West London
54783	Fitness Counselor
109090	Material Handler 2nd Shift 2pm 10pm
65469	Sales Executive Support Coordinator
146626	Workplace Experience Ambassador
120405	Institutional Investor Relations Analyst
174952	Head of Brand Product Marketing
68768	Staff Product Manager IoT Connected Devices
51695	Optician Day Shift Full Benefits Training Provided
61795	Market Strategy and Partnerships Manager
126937	Band 7 Locum Echocardiographer Harlow
170937	Jovem Aprendiz
83575	FLEX Sales Assistant JD Leiden Haarlemmerstraat
152461	Personal Trainer
136259	General Interest
54339	Assistant Kids Club Manager
30984	Silicon Alliances Ecosystem Development Manager
62599	Product Internship
64148	Field Service Technician Maintenance Portsmouth UK Fixed Term Minimum 30 Hours Week
59508	Investigative Analyst Due Diligence Global Risk
170969	Revisor
61196	Early Years Educator Unqualified
81548	Quantum Scientist
73616	In Home HHD Dialysis Care Partner 1 1 Client Miami AZ 85539
160490	Full Time Offline Teacher Kuala Lumpur Branch
19788	Electrical Engineer Communication and Radio Frequency Remote Eligible Relocation Assistance Available
23400	Residency Program New Nurse Graduates
166163	Care Technician Patient Care Unit
170268	Personal Training Manager
29862	Intern Test Development Engineer Intern
63066	Staff Product Manager Connected Ecosystem
88170	Intern Site Civil Engineering
53058	Energy Market Analyst Support
16496	Engineer Field Process Taiwan Hsinchu Taichung Tainan Kaoshiung
41446	Employee Benefits Client Consultant
89722	Rotor Wing Relief Pilot
53878	Receiving Operator bulk materials
162870	Associate Digital Marketing Director Owala
84353	CEO補佐
16935	デモラボ 設備保全 工事管理
158374	Group Business Accountant
128346	Specialty Dr in Psychiatry required for Forensic unit in Yorkshire
145008	Signal Integrity Engineer Serdes Satellites Starlink
142053	Psychiatric Clinician
48029	Associate Director Director Drug Product CMC
134838	Sales Manager
111509	Board Certified Behavior Analyst BCBA
75744	Insurance Producer Amarillo TX
159759	Product Developer gn Value Improvement Projects
110600	Urologist Beacon Clinic
134383	Build Release Engineer
172561	Groundskeeper
74617	SVP Revenue Asset Management
14	Manager Consumer Insights
116450	LLM Agentic Evaluation Rig Engineer
174900	Midwives
131531	Staff Mechanisms Engineer
132891	FP&A Analyst
145618	Endodontist Opening
176409	Professional Services Consultant German
154636	Personal Trainer Baltimore MD
125191	US Finance Data Contributor AI Research Remote Task Based Charlotte
35889	Engineering Manager Mortgage
78946	Publisher Business Development Manager Spot Commerce Media
72824	Fulfillment Associate
150070	Class A Commercial Driver
43569	Quality Control Specialist Onsite Creative
46244	Health Information Specialist I
50350	Product Manager Neighborhood Keeper
47312	GTM Recruiter Contract
83407	Vendeur CDI temps partiel Job Template
129842	Staff Engineer
63819	Electrical Engineer Intern Spring 2027
114074	Auxiliaire de vie Job étudiant
64804	Robotic Systems Integration Engineer SLA SLS
124806	Remote Study Participants AI Research Chihuahua
123840	Javascript Developers AI Training Nashville USA
99524	Legal Ops
137787	Enterprise Account Executive Southeast
122688	Chemistry Graduates AI Training San Jose US
118576	Technical Support Engineer
142342	Director Resource Planning
111012	Specialist Footwear Tooling Engineer
121266	Founding Growth Marketer
80096	Data AI Annotator Flexible Hours
52826	58 SAS Developer
54008	Assistant Fitness Manager
110617	Project Manager Remote
971	Substitute or On Call Opportunities Inquire here
117563	Video Producer
113768	Assistante de vie
96490	Retail Store Associates
122752	Computer Sciences Graduates AI Training Omaha US
39559	Registered Nurse RN FT Back End Nights
137508	Intern Engineering Sciences Summer 2027
81429	Director Product Management Domains SSL Products
85521	All Future Mission Assignments Avionics
91766	Social Worker Referral and Assessment
128428	Speech and language therapy Preston
146060	Mortgage Loan Officer
928	Robotics Research Engineer
127143	Band 7 Sonographer
133282	Deputy CCO
11072	Quality Engineer or Dive XL
146183	Account Executive
46143	Cloud Engineer ACWS
140483	Test Automation Software Engineer Intern
142447	Software Eng II
59389	Technical Support Engineer
107722	Inspector Electrical IT
121889	AI Trainer Kazakh Korea
120380	Institutional Investor Engagement French speaking
141538	Bartender Mews House Mayfair Central London
122830	Database Administrator Graduates AI Training Culiacán Mexico
42197	Staff Machine Learning Engineer Search Discovery
52609	Patient Coordinator
123150	Environmental Research Graduates AI Training Columbus US
172691	People Assistant
255	Applied Scientist
122114	AI Training Experts Wyoming US
5220	Analyst Marketing Analytics and Strategy Bangkok Based relocation provided
156596	Continuous Improvement Operations Manager Open Application
135881	Manager Memory Sales
35707	Service Advisor
80370	Manager Product Operations
48837	Site Reliability Engineer
20062	GT 2027 R&D Program
57018	Corrosion Operations Engineer I II III
129670	Grupo QuintoAndar Especialista Comercial Hunter Goiânia
8615	Staff Software Engineer
84212	Team Member Juicer U115 Bluewater Shopping Centre Dartford
100736	Manager Site Reliability Engineering Storage Layer Service
50719	Lead Engineer Workplace Technology Automation
146131	BR Payments Ops Specialist
114777	Overnight Clinical Assistant
53299	Machine Safety Engineer
128213	Clinical Psychologist ASD Assessments
27481	Psychotherapist
17979	AI Engineer Enterprise Search
4654	Self Service Implementation Specialist Freelance Project
20550	2027 Graduate Engineer HV Power
146419	Responsável de Secção Sport Zone Algarve C.C Mar Shopping
96928	Orthopedic Nurse Practitioner Physician Assistant
33983	Center Based Board Certified Assistant Behavior Analyst BCaBA
88277	Project Geotechnical Engineer Project Manager
120281	Graphic Designer at Private Equity Insights
133556	Product Manager App Performance Architecture
176747	Software Engineer X Data Engineering
56761	Helpdesk Technician
98781	HSE Safety Coordinator Mission Critical Michels Energy Holdings Inc
3896	Manager Creator Content Video Editor EU
14918	Construction Project Manager Co op Spring Summer 2027
86528	Inventory Service Associate ISA Monroe LA 1099 Contractor
133066	Implementation Consultant
12743	Engineering Manager Labs
16969	Cloud DevOps Engineer GCP and Kubernetes
170086	Analyst Business Intelligence
14941	Construction Project Manager Intern or Co Op Summer 2027
140959	Business Analytics Analyst
108593	Staff Software Engineer Delegated Administration Auth0
75036	VDC Engineer
52835	Analista de Aplicaciones
90623	Occupational Therapist Adult Social Care
167044	Customer Care AI Analyst Intern Summer 2027
155665	Medical Device Sales Associate Territory Account Manager
146431	Sales assistant Sport Zone Beja
173056	Associate Veterinarian Kyle Animal Hospital Kyle TX
161373	Strategic Finance Manager Go To Market
96737	Endocrinology Nurse Practitioner Physician Assistant
20776	CEO Simunix
166789	Manufacturing Coordinator Structures Manufacturing
153796	Personal Trainer
164168	Cloud QA UniFi Cloud Service
117647	Product Manager 12 Month FTC
99618	Therapeutic Account Manager Liver Minneapolis
121242	Business Development Intern
63639	Engineering Manager CAPEX Projects
171655	Utility Officer Atlanta GA
47987	Obstetrician Gynecologist PRN
38293	Manager Corporate Programs
42319	CFS Automation Maintenance Specialist L5 Automation Engineering Physical Plant Mech HVAC R DON1
15479	Analista de Operações Pleno Gestão de Integrações
176881	AI ML Engineer
45064	Solutions Architect EDW Enterprise Data Warehouse Migrations
133275	Customer Experience Representative Advanced Services
127341	Consultant in Adult Psychiatry Crawley
109989	Business Developer Mercado Brasil
152312	Personal Trainer
75193	RTH FSQA Technician 1st Shift Days Off Saturday Sunday
62376	Project Portfolio Governance Manager
114593	Counsel
19150	A&P Mechanic Instructor
123725	IT Technology Decision Makers Paid ITSM ESM Research Study Austin US
13020	Sales Strategy Operational Excellence
165106	Webpage Lead Wordpress at United Media
175222	Director Thought Leadership
35583	Communications Systems Engineer
1884	Oracle EBS Sub Ledger Accounting SLA SME
72329	Clinical Research Medical Receptionist
152009	Personal Trainer
9119	Clinical Care Coordinator
163080	Staff Propulsion Analyst Engine Performance
48205	Entry Level Kitchen Position Boston
52836	Ansible automation engineer
16394	Tax Staff
146331	Lead Clinical Care Navigator Global
49359	Commercial.e B2B Lille
139674	Industrial Trainee Finance Accounting
117543	NetSuite Consultant FSM
960	Staff Engineer Pharmaceutical Delivery
97265	Integrated Media Planner
139979	Product Data Scientist Data Labs
148552	Product Manager E Invoicing
35274	Leadership Program MBA Graduate
60510	Retail Lead Indianapolis Colts Team Store
30142	Manager of IT Consultants
145635	Oral Surgeon Opening
5801	Web Software Engineer Data Science Prototyping
115429	バックエンドエンジニア 審査 債権システム
31545	PMO on site Kraków
21484	Certified Nursing Assistant CNA All Shifts
78663	Pediatric In Home Acute Care Registered Nurse Float
134465	Content Specialist Associate
148337	Engineering Manager Billing Products
121005	Sponsorship Sales Associate at Private Equity Insights
17724	Health IT Data Engineer
80249	Applied AI Solution Owner
58314	Client Relationship Manager
60015	Custody Operations Associate
49272	Sales Director Construction Saudi Cluster
22387	LPN Nurse Residency Program for Private Duty Nursing
37991	Delivery Director
120524	Investor Engagement Analyst Danish speaking
70637	Guidance Navigation Controls Engineer
140807	Systems Test Engineer
115351	UX Writer
110638	Primary Care Functional Medicine Nurse Practitioner
54670	Certified Personal Trainer
170738	Selling Area Sales Director Home Health Sales
2324	Chief of Staff to Chief Executive Officer CEO
177380	Assessoria de Investimentos Início de Carreira
164050	Clinical Documentation Integrity Specialist
51965	Account Manager P&C
90502	Family Court Adviser
81826	Working Student Talent Acquisition
95966	Associate Consultant NY Area
125148	United Kingdom Residents Survey Participants Liverpool UK
3420	Fall Intern Sustainability
59337	Project Manager Data Reporting and Visualization Team III
137697	Scientist I
51865	Director Financial Planning
27017	Psychiatric Mental Health Nurse Practitioner PMHNP
123368	Game Development Environment Artist Colorado US Freelance Remote
64438	Estimator
3149	Engineering Manager Debit Card Payments
36777	Consulting Project Team Lead Launch Commercialization Multiple offices
72719	Area Manager I Warehouse
17069	Assessoria de Investimentos XP Future
77002	Email Marketing Associate at HRtechX
137669	Staff Data Analyst
147230	Structures Fabricator Sheet Metal
49838	Associate Manager Product Operations Autonomy Commercialization DoorDash Dot
159506	Clinical Lead Supervisor BCBA Pilsen 10 000 Sign On Bonus
143775	Lead Welder Starship Launch Hardware Multiple Shifts
18914	Low Code Engineer SFMC MCAE
126780	Band 6 Trauma and Orthopaedics Physiotherapist Barts NHS Trust
78980	Technical Project Manager
101087	Head of Engineering Sales Marketing Tools
113060	Director Accounting
177063	Application Engineer CNC Sheet
156094	Data Analyst II Credit
44913	Manager Field Engineering Strategic Digital Native Business
167504	Product Manager Unattended Payment Environments
155970	Military Coordinator Payor Success Associate
37472	Named Account Executive High Tech NY Metro
156552	Adjoint chef d équipe
130669	Temp Turns Help
111022	Specialist Regional Sales Representative Pacific Northwest
74167	Director of Operations
143441	Fleet Maintenance Technician Starship Level 4 5
94988	Director Global B2B Marketing
93192	Building Engineer
85484	Program Manager Product
152184	Personal Trainer
89015	Staff Software Engineer
127152	Band 7 Sonographer Grimsby
60422	Lead Accountant Supply Chain
15891	Director AI and Machine Learning
95464	Associate Scientist Associate Scientist In Vivo Pharmacology Study Coordinator
41030	Specialist Field Engineer Kubernetes
16768	Engineer I NPI Field Process
11446	Manager Mechanical Engineering
65280	System Engineer Skill Level 2 763
95901	Real Estate Agent
152690	Personal Trainer
57913	Territory Account Manager
135502	Media Specialist
177156	Account Executive Enterprise
25127	Jacksonville FL Land Site Development New Grad
31815	Trade Transaction Reporting Business Analyst Project Manager
149044	Apprentice Piercer Future Opportunity
122903	Data Entry Clerk Graduates AI Training Ciudad Juárez Mexico
1012	Product Insights Manager
16086	Data Consultant US
15150	Project Manager Co op Summer Fall 2027
71389	Product Design Manager Banking
48128	Social Media Manager
177041	Transactions Monitoring Officer
67802	Engineering Manager Agent Foundations Agent Execution
153042	Personal Trainer
131639	Customer Architect
62842	Associate Scientist Scientist I In Vivo Pharmacology
27857	Head of Credit Management
12572	Paid Media Analyst
63881	Head of Flight Test Engineering
169981	Werkstudent in Recht
121123	Billing Operations Specialist
156630	Fulfilment Site Lead
163436	Licensed Practical Nurse Chelsea Massachusetts
61503	Growth Operations Manager
65681	Product Manager Lab
28544	Account Executive Enterprise Retail
175873	Manager Global Commerce Strategy
94069	Nurse Practitioner Physician Assistant Cardiac Surgery
68239	Commodities Trading Insight Networking Evening London
92596	Purchasing Manager South Dallas Area
109331	Innovation Manager
28226	AI Response Labeler Annotator Italian Specialty
10191	Camera Test Engineer
33263	Applied Value Engineer High Tech
8645	Engineer II Information Engineer
90778	Person in Charge
96335	Construction Observer
156467	Campaign Manager
127785	Locum Consultant in Oncology Lung and CNS cancers 12 month Fixed Term Contract or Substantive option Norfolk
81232	Business Developer
163641	Business Intelligence Engineer
115436	システムリスク管理担当
121825	AI Trainer Fluent Serbian Speaker
103194	Optical Engineer
64229	Front of House Receptionist
109144	Shipping Loader Unloader
33081	Computational Biologist
73124	US DC FSQ Area Manager
71298	General Application
135052	Bartender New Opening
72921	Operations Manager
166504	Salesforce Commerce Cloud PWA SFRA Developer
97033	Primary Care Nurse Practitioner Physician Assistant
137490	Billing Analyst
122511	Canada Residents Survey Participants Port Stanley Canada
55276	General Manager
124079	Linguist Translator Graduates AI Training Toluca Mexico
58515	Badverkäufer Badplaner
137511	Intern Engineering Sciences Summer 2027
129833	Product Manager Army
132656	B2C Product Manager Hybrid
134655	Software Engineer Video Platform
113895	Auxiliaire de vie
136711	Risk Advisory Staff
68423	Sales Associate Part Time Editor Las Vegas
48942	Associate Investments
171380	Integrated Systems Service Technician
94747	Ambulatory Infusion Clinic Nurse
77831	Supplier Quality Engineer
137692	Technology Reporter Business of Tech AI Power
95397	Analyst Fund Expense
91480	Social Worker Duty and Assessment
166102	Business Development Client Services
148449	Lifecycle Marketing Manager Capital
152276	Personal Trainer
18752	Business Development Manager
47222	Field Marketing Manager
38050	Research Investigator Computational Chemistry
3411	Event Guest Services Staff Texas Trust CU Theatre Grand Prairie
81609	Lead Endpoint Engineering Architect
176012	Pessoa Analista de Dados Pleno App
25609	Operations Specialist Driver Providence RI
93388	Maintenance Automation Engineer
157546	Research Engineer
33118	Associate Deployment Engineer Galaxy Graduate Program
92544	Pipefitter Journeyman Shop Fabrication
97881	Director Pharma Media
121174	Associate Product Management Consultant
141242	Account Executive
36350	Victim Assistance Specialist
34163	Early Intervention BCBA Life Skills Autism Academy Center Based
89265	Conseiller.ère en architecture de sécurité de l intelligence artificielle
68342	Software Engineer Data Platform
41631	쿠팡 이츠 Account Management팀 매니저 Food AM
105168	OSS BA
139269	Operations Manager
46302	Commercial Life Sciences Solutions
48460	Events Manager
9940	Manager Product Management
159645	Quality Manager gn Product Development
42290	쿠팡 프라이싱운영 담당자 Bundle L3
95320	Ecommerce Conversion Rate Optimization Specialist
67036	Sports Data Collector Ice Hockey Cardiff Wales
96104	Staff Software Engineer Identity Platform
147075	Associate Private Equity Product Manager Data Technology Solutions
175495	Associate Director Client Services India
146541	Client Service Manager
98257	Directory Services Engineer I 6228
137389	Knowledge Manager
100643	Consulting Engineer
96858	Neurology Nurse Practitioner Physician Assistant
131314	Manager High Power Engineering
129300	Reference Data Business Analyst
39853	Product Designer
76806	Technical Program Manager QA Developer Experience
147069	Analyst Research Investments Private Equity
160867	Lead Program Manager
72912	Marketing Analyst Growth Analytics
33152	Director Value Engineering Financial Services
2407	ACLU NBLSA Southern Legal Internship Program 2027
74120	Project Manager Data Centers
99278	Lead SAP PP DS Consultant Präge die Planungslösungen unserer Kunden
37290	Intelligence Production Lead
86942	Clinical Sales Specialist Las Vegas
155633	Software Engineer
53428	Hospice Social Worker
141236	Account Executive
70177	Account Director
172638	Director MOE
94233	Staff Nurse Emergency Department
111338	ABA Paraprofessional RBT Ozone Park Queens
62408	Market Analyst
17227	Package Signal Power Integrity
75964	Compliance Examiner Broker Dealer
17700	Actuarial Analyst Pricing Remote
38524	Data Center Operations Engineer
40401	Join our Talent Community
58209	PMHNP
173008	Mental Health Care Coordinator for High Needs Youth
120961	Event Coordinator
39232	PT Supervisor de Equipo de Limpieza Oficinas Lunes a Viernes 5 30pm 10 30pm
112365	Medical Assistant
81506	Optical Integration Engineer
165974	Quality Control Technician Monday to Thursday 8pm 6am
176687	Network Engineer Battery Storage Memphis
78817	Research Analyst Indian Equities
101059	Head of Engineering Field Verification
81701	SE Mission Systems RF Payloads
49650	Project Accountant
93660	Test Engineer Inverter
127596	Echocardiographer Salisbury
115358	キャンペーン企画担当
67204	Fleet Services Operations Technician
124665	Product UX Professionals AI Training Nashville USA
94042	Medical Lab Scientist I PRN Evening AAMC
140774	Software Packaging Engineer
20610	Process Engineer
63578	FP&A Analyst
85200	Quantitative Trader Trading Team
85320	Global PR Specialist
93365	Facilities Maintenance Supervisor
164127	Applied Scientist Shipper Pricing
130214	Product Operations Specialist Bogotá
50938	Service Desk Technician
174286	Bolt munkatárs Wolt Market Keleti
121944	AI Trainer Travel Agents Dallas US
105422	Client Relationship Manager
68911	Research Lead Training
106108	Software Engineer Fullstack
156455	Future Coach Facilitator Opportunity
25801	Platform Ops Lead
20548	2027 Graduate Engineer High Voltage Protection Control
138919	TikTok Shop Manager Greece
94936	Group Product Manager Verticals
109577	OSP Construction Technician 100 Travel Required
173501	Wolf Run Veterinary Clinic Urgent Care DVM
25309	BIBIBOP Team Leader Huber Heights
40501	Mechanical Engineer New Product Development
83915	Product Designer AIR
151042	Personal Trainer
37680	Manager Medicare Product Market Intelligence
97253	Future Talents Mediabrands Prospect
84577	General Applications
89784	Center Based Registered Behavior Technician RBT
12170	Staff Product Quality Engineer Actuators
65822	Product Manager Payments
156979	Seasonal Sales Associate
141593	Club Reception Ludlow House
130374	Manager of Web Experience
84502	Customer Onboarding Specialist PracticePanther
58347	Environmental Superintendent
64612	GTM Engineer
170138	Associate Director Quality Systems Temporary
59217	Calling all qualified drivers
40371	Food Safety Specialist
154251	Personal Trainer
106838	Administrative Assistant Part Time
81792	Design Engineer
135610	Enterprise Core Sales Engineer Midwest
103866	Sous Chef New Restaurant Opening
74008	Hiring Task Based Helpers for Seniors in San Mateo CA
14101	Product Manager Platform Engineering
130408	Tech Support Manager
146609	PPM Business Analyst with Workfront JIRA Experience
11220	Controls Engineer Manufacturing Automation
171555	Armed Security Officer Mobile AL
30911	Partner Sales Director IHV Alliances
128290	Specialty Dr for Forensic Psychiatry role based in East Anglia
63393	Staff Product Manager SmugMug
54890	Fitness Counselor
75116	Sales Manager
34904	Clinical Account Manager Southeast Pennsylvania
10495	Functional Safety Engineer
29915	Product Manager User Notification Control
22227	Licensed Practical Nurse LPN Home Health
7817	Seasonal Operations Associate Part Time Houston Galleria
13234	Projects Control Engineer
15758	Site Superintendent Data Center Startup Commissioning
139514	リードソフトウェアリサーチャー 3Dディスプレイ描画アルゴリズム開発
86831	Outside Sales
149914	Lead TPM
143878	Manufacturing Specialist 2nd Shift
120435	Institutional Investor Relations German speaking
97919	Accounting Manager
6500	GNSS Navigation and Estimation Engineer
131058	QA Auditor
30907	OpenStack Engineering Manager
57154	Control Systems Engineer
130018	SOX Compliance Analyst
177408	Assessoria de Investimentos XP Future
118703	PM Administrative Assistant
73192	BCBA Fellowship Field Work Springfield PA
25909	Director Business Development Europe
134626	Software Engineer Kubernetes ServiceMesh
155654	Social and Content Manager
91277	Social Worker Child Protection
119521	Clinical Project Manager Clinical Project Manager
108471	Product Manager Auth0
59157	Analista de Compras Pleno
90242	Test EA AC Webhook failure
174961	Revenue Operations Analyst
122673	Chemistry Graduates AI Training Louisville US
23299	Registered Nurse RN INFANT 1 1 Home Care
54878	Fitness Counselor
5603	Machine Learning Engineer Lead Vulcan
122703	Chile Residents Survey Participants Los Ángeles Chile
43534	Paralegal
96766	Gastroenterology Nurse Practitioner Physician Assistant
116822	Executive Assistant
85515	Territory Manager Lexington KY
115780	QC Lab Administrator
54375	Assistant Kids Club Manager
113109	Manager Strategic Planning
62841	Associate Scientist Analytical Development and Quality Control
95898	Real Estate Agent
33890	Behavior Technician Work with Kids Training Provided
160733	Graduate Associate Client Services German Speaker
```

## Predictions

What iteration 11's classifier answered for each row above, with its reason for `UNKNOWN`.

```
38444	UNKNOWN	domain_ambiguity
873	OUT
80628	UNKNOWN	domain_ambiguity
90306	UNKNOWN	domain_ambiguity
176607	OUT
41647	OUT
87573	OUT
8030	OUT
171087	UNKNOWN	domain_ambiguity
25459	OUT
144264	OUT
78650	OUT
15046	OUT
131781	UNKNOWN	domain_ambiguity
79308	OUT
130718	UNKNOWN	domain_ambiguity
156343	UNKNOWN	domain_ambiguity
112726	OUT
34137	UNKNOWN	unruled
29935	UNKNOWN	domain_ambiguity
58806	OUT
129359	UNKNOWN	domain_ambiguity
97752	OUT
53507	OUT
122952	UNKNOWN	scope_ambiguity
48989	OUT
80581	UNKNOWN	scope_ambiguity
3624	OUT
17574	UNKNOWN	domain_ambiguity
57389	OUT
80585	UNKNOWN	domain_ambiguity
161949	UNKNOWN	scope_ambiguity
151403	OUT
85439	UNKNOWN	domain_ambiguity
124033	UNKNOWN	scope_ambiguity
54397	OUT
52946	UNKNOWN	scope_ambiguity
89371	OUT
174865	OUT
98141	IN
156338	UNKNOWN	domain_ambiguity
135125	OUT
147789	UNKNOWN	domain_ambiguity
144821	IN
16850	UNKNOWN	domain_ambiguity
133257	IN
132579	OUT
108838	OUT
19704	UNKNOWN	domain_ambiguity
91707	OUT
49654	OUT
162204	UNKNOWN	domain_ambiguity
149988	UNKNOWN	domain_ambiguity
93619	OUT
51765	UNKNOWN	unruled
128231	OUT
98641	OUT
20585	UNKNOWN	domain_ambiguity
147246	OUT
143938	OUT
31725	OUT
83636	UNKNOWN	unruled
14589	UNKNOWN	domain_ambiguity
45144	IN
119806	OUT
109372	OUT
160762	OUT
140769	IN
88552	UNKNOWN	unruled
124431	OUT
9358	OUT
20374	UNKNOWN	domain_ambiguity
86990	UNKNOWN	unruled
175545	OUT
29670	UNKNOWN	domain_ambiguity
128281	OUT
22272	OUT
101368	OUT
157741	UNKNOWN	domain_ambiguity
166917	UNKNOWN	domain_ambiguity
136121	OUT
73224	OUT
12184	UNKNOWN	domain_ambiguity
163548	UNKNOWN	domain_ambiguity
53717	UNKNOWN	domain_ambiguity
167696	OUT
151428	OUT
37739	UNKNOWN	domain_ambiguity
96029	OUT
115837	IN
169645	OUT
136493	IN
96422	OUT
89841	OUT
164820	UNKNOWN	unruled
139851	OUT
16986	OUT
80311	UNKNOWN	domain_ambiguity
155406	UNKNOWN	domain_ambiguity
66369	OUT
131927	UNKNOWN	domain_ambiguity
55806	OUT
43747	UNKNOWN	domain_ambiguity
110366	OUT
135917	UNKNOWN	domain_ambiguity
84150	UNKNOWN	unruled
69585	UNKNOWN	domain_ambiguity
103776	UNKNOWN	domain_ambiguity
38703	OUT
133247	UNKNOWN	domain_ambiguity
278	UNKNOWN	unruled
152599	OUT
31170	UNKNOWN	unruled
73425	OUT
7679	OUT
4938	OUT
155886	IN
21378	OUT
125991	OUT
31356	UNKNOWN	domain_ambiguity
147456	UNKNOWN	domain_ambiguity
25093	OUT
125957	OUT
64079	OUT
73437	OUT
99971	UNKNOWN	scope_ambiguity
116510	UNKNOWN	unruled
64338	OUT
86376	OUT
121276	UNKNOWN	domain_ambiguity
48369	OUT
85148	OUT
159396	OUT
157091	UNKNOWN	domain_ambiguity
53355	OUT
137095	UNKNOWN	domain_ambiguity
47937	OUT
137195	UNKNOWN	domain_ambiguity
105057	OUT
152392	OUT
62538	UNKNOWN	domain_ambiguity
120712	UNKNOWN	unruled
47853	OUT
63161	OUT
112845	OUT
25217	UNKNOWN	unruled
3098	UNKNOWN	domain_ambiguity
89747	OUT
58514	UNKNOWN	unruled
169068	OUT
53848	UNKNOWN	domain_ambiguity
142699	OUT
27425	OUT
136379	UNKNOWN	domain_ambiguity
56395	OUT
107158	UNKNOWN	domain_ambiguity
69281	OUT
76653	OUT
97692	UNKNOWN	domain_ambiguity
11536	UNKNOWN	domain_ambiguity
11102	UNKNOWN	domain_ambiguity
48252	OUT
156253	UNKNOWN	unruled
43363	OUT
25986	OUT
29923	UNKNOWN	domain_ambiguity
42693	IN
59550	OUT
137464	UNKNOWN	domain_ambiguity
59087	UNKNOWN	scope_ambiguity
174851	UNKNOWN	domain_ambiguity
95343	IN
55503	OUT
165711	UNKNOWN	domain_ambiguity
175791	OUT
7662	OUT
38646	OUT
110391	OUT
40606	OUT
123308	OUT
63597	OUT
130926	IN
12550	UNKNOWN	domain_ambiguity
101273	UNKNOWN	unruled
56409	OUT
39122	OUT
66888	OUT
147162	OUT
53157	UNKNOWN	unruled
57917	OUT
170088	OUT
129726	UNKNOWN	domain_ambiguity
176586	OUT
38518	OUT
119558	OUT
64528	UNKNOWN	domain_ambiguity
12110	OUT
77284	OUT
98168	UNKNOWN	domain_ambiguity
129850	OUT
162234	OUT
72632	OUT
175981	UNKNOWN	unruled
114557	OUT
166051	OUT
125557	OUT
163236	IN
144519	IN
79315	OUT
93200	UNKNOWN	domain_ambiguity
123148	UNKNOWN	scope_ambiguity
117271	UNKNOWN	scope_ambiguity
10201	UNKNOWN	domain_ambiguity
78834	IN
50674	UNKNOWN	domain_ambiguity
66410	OUT
155490	UNKNOWN	unruled
87282	OUT
12616	UNKNOWN	unruled
41018	UNKNOWN	domain_ambiguity
162747	OUT
84017	OUT
24554	IN
137108	IN
151954	OUT
34020	OUT
143545	OUT
110958	OUT
64387	UNKNOWN	domain_ambiguity
105834	UNKNOWN	unruled
133006	IN
40161	OUT
133338	UNKNOWN	domain_ambiguity
32675	OUT
159658	UNKNOWN	domain_ambiguity
165259	UNKNOWN	domain_ambiguity
71459	OUT
153799	OUT
124620	OUT
76337	OUT
130737	OUT
177927	OUT
71947	UNKNOWN	domain_ambiguity
77657	UNKNOWN	unruled
21112	OUT
119394	UNKNOWN	domain_ambiguity
47054	UNKNOWN	domain_ambiguity
45548	OUT
143839	OUT
400	OUT
116366	UNKNOWN	unruled
53167	OUT
31189	UNKNOWN	scope_ambiguity
58120	OUT
114363	UNKNOWN	domain_ambiguity
139953	OUT
126311	OUT
73284	OUT
53982	OUT
171891	IN
136128	UNKNOWN	unruled
31157	UNKNOWN	domain_ambiguity
122454	UNKNOWN	unruled
38228	UNKNOWN	domain_ambiguity
117115	OUT
115247	OUT
64901	OUT
78317	UNKNOWN	domain_ambiguity
178058	UNKNOWN	domain_ambiguity
81581	UNKNOWN	domain_ambiguity
132974	IN
120381	UNKNOWN	unruled
22981	OUT
169489	OUT
38903	OUT
41731	UNKNOWN	unruled
109318	OUT
28	UNKNOWN	unruled
150644	OUT
123542	OUT
14366	IN
93191	UNKNOWN	domain_ambiguity
128581	IN
161183	OUT
108406	IN
58558	OUT
145118	OUT
117537	IN
138431	UNKNOWN	domain_ambiguity
126591	OUT
123738	UNKNOWN	unruled
131868	OUT
117528	OUT
137779	UNKNOWN	domain_ambiguity
138770	UNKNOWN	domain_ambiguity
38320	UNKNOWN	domain_ambiguity
107901	IN
171403	IN
127076	OUT
129331	OUT
71658	UNKNOWN	domain_ambiguity
20030	UNKNOWN	scope_ambiguity
66194	OUT
62742	IN
123031	OUT
147476	UNKNOWN	domain_ambiguity
8227	OUT
117405	UNKNOWN	domain_ambiguity
97534	OUT
154815	OUT
135096	OUT
46687	IN
58725	OUT
103307	UNKNOWN	domain_ambiguity
91142	OUT
19420	OUT
53694	IN
137707	OUT
176151	UNKNOWN	domain_ambiguity
35815	OUT
21770	UNKNOWN	unruled
43989	IN
138759	UNKNOWN	domain_ambiguity
85952	UNKNOWN	domain_ambiguity
91913	UNKNOWN	unruled
157965	OUT
15396	OUT
18655	UNKNOWN	domain_ambiguity
71888	OUT
144065	UNKNOWN	domain_ambiguity
67198	UNKNOWN	unruled
104987	UNKNOWN	domain_ambiguity
41187	OUT
148334	UNKNOWN	domain_ambiguity
143840	OUT
2289	OUT
101026	UNKNOWN	domain_ambiguity
89849	OUT
69595	IN
2777	IN
103265	UNKNOWN	unruled
47256	UNKNOWN	domain_ambiguity
47423	UNKNOWN	domain_ambiguity
161894	IN
98013	OUT
161306	UNKNOWN	domain_ambiguity
140096	IN
131777	OUT
104055	OUT
113655	OUT
40236	OUT
104242	IN
49051	UNKNOWN	unruled
123283	UNKNOWN	unruled
4924	IN
20720	UNKNOWN	domain_ambiguity
45218	UNKNOWN	domain_ambiguity
26331	UNKNOWN	scope_ambiguity
159347	OUT
13992	OUT
63485	OUT
4323	OUT
126287	OUT
161032	UNKNOWN	domain_ambiguity
104467	UNKNOWN	scope_ambiguity
50774	IN
65123	IN
2089	UNKNOWN	domain_ambiguity
29682	OUT
114023	OUT
114225	UNKNOWN	domain_ambiguity
1260	OUT
39826	IN
38435	OUT
61870	UNKNOWN	domain_ambiguity
101386	UNKNOWN	domain_ambiguity
29884	IN
93673	IN
18230	UNKNOWN	domain_ambiguity
176072	OUT
77674	IN
127749	OUT
64827	OUT
31328	UNKNOWN	unruled
118772	IN
13247	OUT
75035	UNKNOWN	domain_ambiguity
167933	UNKNOWN	domain_ambiguity
144908	UNKNOWN	domain_ambiguity
44479	OUT
141509	UNKNOWN	unruled
78971	IN
50334	UNKNOWN	unruled
73994	OUT
40474	UNKNOWN	unruled
75860	OUT
162012	UNKNOWN	scope_ambiguity
39517	OUT
18494	UNKNOWN	unruled
137207	OUT
77715	OUT
142108	IN
169725	OUT
124196	OUT
17085	UNKNOWN	domain_ambiguity
88555	UNKNOWN	unruled
9514	UNKNOWN	unruled
49528	IN
8673	UNKNOWN	unruled
11057	OUT
90062	UNKNOWN	unruled
30371	UNKNOWN	unruled
121628	OUT
123506	OUT
78491	UNKNOWN	domain_ambiguity
93193	UNKNOWN	domain_ambiguity
19891	IN
157922	UNKNOWN	domain_ambiguity
89611	UNKNOWN	domain_ambiguity
48737	UNKNOWN	domain_ambiguity
73928	OUT
52064	UNKNOWN	scope_ambiguity
38107	IN
160633	OUT
177390	UNKNOWN	unruled
141134	UNKNOWN	unruled
126248	OUT
129528	UNKNOWN	domain_ambiguity
160749	UNKNOWN	unruled
107394	OUT
101366	UNKNOWN	domain_ambiguity
87982	OUT
127529	OUT
171084	UNKNOWN	unruled
51038	UNKNOWN	domain_ambiguity
4568	OUT
140456	UNKNOWN	domain_ambiguity
73357	OUT
98395	OUT
93232	UNKNOWN	domain_ambiguity
1067	OUT
14455	UNKNOWN	domain_ambiguity
123843	UNKNOWN	scope_ambiguity
136137	UNKNOWN	unruled
93230	UNKNOWN	domain_ambiguity
24976	UNKNOWN	domain_ambiguity
72670	UNKNOWN	domain_ambiguity
141832	OUT
54783	OUT
109090	OUT
65469	OUT
146626	OUT
120405	UNKNOWN	domain_ambiguity
174952	OUT
68768	UNKNOWN	domain_ambiguity
51695	UNKNOWN	unruled
61795	OUT
126937	OUT
170937	UNKNOWN	unruled
83575	OUT
152461	OUT
136259	OUT
54339	OUT
30984	OUT
62599	UNKNOWN	domain_ambiguity
64148	OUT
59508	UNKNOWN	domain_ambiguity
170969	UNKNOWN	unruled
61196	OUT
81548	UNKNOWN	domain_ambiguity
73616	OUT
160490	OUT
19788	OUT
23400	OUT
166163	OUT
170268	OUT
29862	UNKNOWN	domain_ambiguity
63066	UNKNOWN	domain_ambiguity
88170	OUT
53058	UNKNOWN	domain_ambiguity
16496	UNKNOWN	domain_ambiguity
41446	OUT
89722	UNKNOWN	unruled
53878	OUT
162870	OUT
84353	UNKNOWN	unruled
16935	UNKNOWN	unruled
158374	OUT
128346	OUT
145008	OUT
142053	OUT
48029	UNKNOWN	domain_ambiguity
134838	OUT
111509	OUT
75744	OUT
159759	UNKNOWN	domain_ambiguity
110600	UNKNOWN	unruled
134383	UNKNOWN	domain_ambiguity
172561	UNKNOWN	unruled
74617	OUT
14	OUT
116450	IN
174900	UNKNOWN	unruled
131531	UNKNOWN	domain_ambiguity
132891	UNKNOWN	domain_ambiguity
145618	OUT
176409	OUT
154636	OUT
125191	UNKNOWN	unruled
35889	UNKNOWN	domain_ambiguity
78946	OUT
72824	OUT
150070	OUT
43569	OUT
46244	OUT
50350	UNKNOWN	domain_ambiguity
47312	OUT
83407	OUT
129842	UNKNOWN	domain_ambiguity
63819	OUT
114074	OUT
64804	IN
124806	UNKNOWN	unruled
123840	UNKNOWN	scope_ambiguity
99524	UNKNOWN	unruled
137787	OUT
122688	UNKNOWN	scope_ambiguity
118576	UNKNOWN	scope_ambiguity
142342	OUT
111012	OUT
121266	OUT
80096	UNKNOWN	unruled
52826	UNKNOWN	domain_ambiguity
54008	OUT
110617	OUT
971	UNKNOWN	unruled
117563	OUT
113768	OUT
96490	UNKNOWN	unruled
122752	UNKNOWN	scope_ambiguity
39559	OUT
137508	UNKNOWN	domain_ambiguity
81429	UNKNOWN	domain_ambiguity
85521	UNKNOWN	unruled
91766	OUT
128428	UNKNOWN	unruled
146060	OUT
928	IN
127143	OUT
133282	UNKNOWN	unruled
11072	UNKNOWN	domain_ambiguity
146183	OUT
46143	IN
140483	IN
142447	UNKNOWN	unruled
59389	UNKNOWN	scope_ambiguity
107722	OUT
121889	OUT
120380	UNKNOWN	unruled
141538	OUT
122830	UNKNOWN	scope_ambiguity
42197	IN
52609	OUT
123150	UNKNOWN	scope_ambiguity
172691	OUT
255	UNKNOWN	domain_ambiguity
122114	OUT
5220	UNKNOWN	domain_ambiguity
156596	OUT
135881	OUT
35707	OUT
80370	UNKNOWN	domain_ambiguity
48837	IN
20062	UNKNOWN	unruled
57018	UNKNOWN	domain_ambiguity
129670	UNKNOWN	domain_ambiguity
8615	IN
84212	OUT
100736	IN
50719	IN
146131	UNKNOWN	domain_ambiguity
114777	OUT
53299	UNKNOWN	domain_ambiguity
128213	OUT
27481	OUT
17979	IN
4654	UNKNOWN	domain_ambiguity
20550	UNKNOWN	domain_ambiguity
146419	UNKNOWN	unruled
96928	OUT
33983	OUT
88277	OUT
120281	OUT
133556	UNKNOWN	domain_ambiguity
176747	IN
56761	OUT
98781	OUT
3896	OUT
14918	OUT
86528	OUT
133066	OUT
12743	UNKNOWN	domain_ambiguity
16969	IN
170086	UNKNOWN	domain_ambiguity
14941	OUT
140959	UNKNOWN	domain_ambiguity
108593	IN
75036	UNKNOWN	domain_ambiguity
52835	UNKNOWN	domain_ambiguity
90623	OUT
167044	IN
155665	OUT
146431	OUT
173056	OUT
161373	OUT
96737	OUT
20776	UNKNOWN	unruled
166789	OUT
153796	OUT
164168	UNKNOWN	unruled
117647	UNKNOWN	domain_ambiguity
99618	OUT
121242	OUT
63639	UNKNOWN	domain_ambiguity
171655	OUT
47987	OUT
38293	OUT
42319	OUT
15479	UNKNOWN	domain_ambiguity
176881	IN
45064	IN
133275	OUT
127341	OUT
109989	UNKNOWN	domain_ambiguity
152312	OUT
75193	OUT
62376	OUT
114593	OUT
19150	OUT
123725	UNKNOWN	unruled
13020	OUT
165106	UNKNOWN	domain_ambiguity
175222	OUT
35583	IN
1884	UNKNOWN	unruled
72329	OUT
152009	OUT
9119	OUT
163080	OUT
48205	UNKNOWN	unruled
52836	IN
16394	UNKNOWN	unruled
146331	OUT
49359	UNKNOWN	unruled
139674	UNKNOWN	unruled
117543	IN
960	OUT
97265	OUT
139979	IN
148552	UNKNOWN	domain_ambiguity
35274	UNKNOWN	unruled
60510	OUT
30142	IN
145635	OUT
5801	IN
115429	UNKNOWN	unruled
31545	UNKNOWN	unruled
21484	OUT
78663	OUT
134465	UNKNOWN	domain_ambiguity
148337	UNKNOWN	domain_ambiguity
121005	OUT
17724	IN
80249	UNKNOWN	unruled
58314	OUT
60015	OUT
49272	OUT
22387	OUT
37991	OUT
120524	UNKNOWN	domain_ambiguity
70637	OUT
140807	IN
115351	UNKNOWN	unruled
110638	OUT
54670	OUT
170738	OUT
2324	OUT
177380	UNKNOWN	unruled
164050	OUT
51965	OUT
90502	UNKNOWN	unruled
81826	UNKNOWN	unruled
95966	OUT
125148	OUT
3420	UNKNOWN	domain_ambiguity
59337	IN
137697	UNKNOWN	domain_ambiguity
51865	OUT
27017	OUT
123368	OUT
64438	OUT
3149	UNKNOWN	domain_ambiguity
36777	OUT
72719	OUT
17069	UNKNOWN	unruled
77002	OUT
137669	UNKNOWN	scope_ambiguity
147230	OUT
49838	UNKNOWN	domain_ambiguity
159506	OUT
143775	OUT
18914	UNKNOWN	domain_ambiguity
126780	OUT
78980	IN
101087	OUT
113060	OUT
177063	IN
156094	UNKNOWN	scope_ambiguity
44913	UNKNOWN	domain_ambiguity
167504	UNKNOWN	domain_ambiguity
155970	OUT
37472	OUT
156552	OUT
130669	UNKNOWN	unruled
111022	OUT
74167	OUT
143441	OUT
94988	OUT
93192	UNKNOWN	domain_ambiguity
85484	UNKNOWN	domain_ambiguity
152184	OUT
89015	IN
127152	OUT
60422	OUT
15891	IN
95464	UNKNOWN	domain_ambiguity
41030	UNKNOWN	scope_ambiguity
16768	UNKNOWN	domain_ambiguity
11446	OUT
65280	UNKNOWN	domain_ambiguity
95901	OUT
152690	OUT
57913	OUT
135502	UNKNOWN	domain_ambiguity
177156	OUT
25127	UNKNOWN	unruled
31815	UNKNOWN	scope_ambiguity
149044	OUT
122903	UNKNOWN	scope_ambiguity
1012	UNKNOWN	domain_ambiguity
16086	IN
15150	OUT
71389	OUT
48128	OUT
177041	OUT
67802	UNKNOWN	domain_ambiguity
153042	OUT
131639	UNKNOWN	domain_ambiguity
62842	UNKNOWN	domain_ambiguity
27857	OUT
12572	UNKNOWN	domain_ambiguity
63881	OUT
169981	UNKNOWN	domain_ambiguity
121123	OUT
156630	UNKNOWN	domain_ambiguity
163436	OUT
61503	OUT
65681	UNKNOWN	domain_ambiguity
28544	OUT
175873	OUT
94069	OUT
68239	UNKNOWN	unruled
92596	OUT
109331	OUT
28226	UNKNOWN	unruled
10191	UNKNOWN	domain_ambiguity
33263	UNKNOWN	domain_ambiguity
8645	UNKNOWN	domain_ambiguity
90778	UNKNOWN	unruled
96335	UNKNOWN	unruled
156467	OUT
127785	OUT
81232	UNKNOWN	domain_ambiguity
163641	UNKNOWN	domain_ambiguity
115436	UNKNOWN	unruled
121825	OUT
103194	UNKNOWN	domain_ambiguity
64229	OUT
109144	OUT
33081	UNKNOWN	unruled
73124	OUT
71298	OUT
135052	OUT
72921	OUT
166504	IN
97033	OUT
137490	UNKNOWN	domain_ambiguity
122511	OUT
55276	OUT
124079	UNKNOWN	scope_ambiguity
58515	UNKNOWN	unruled
137511	UNKNOWN	domain_ambiguity
129833	UNKNOWN	domain_ambiguity
132656	UNKNOWN	domain_ambiguity
134655	IN
113895	OUT
136711	UNKNOWN	unruled
68423	OUT
48942	OUT
171380	OUT
94747	OUT
77831	OUT
137692	OUT
95397	UNKNOWN	domain_ambiguity
91480	OUT
166102	UNKNOWN	unruled
148449	OUT
152276	OUT
18752	OUT
47222	OUT
38050	UNKNOWN	unruled
3411	UNKNOWN	unruled
81609	UNKNOWN	domain_ambiguity
176012	IN
25609	OUT
93388	IN
157546	UNKNOWN	domain_ambiguity
33118	UNKNOWN	domain_ambiguity
92544	UNKNOWN	unruled
97881	OUT
121174	UNKNOWN	domain_ambiguity
141242	OUT
36350	UNKNOWN	domain_ambiguity
34163	OUT
89265	UNKNOWN	unruled
68342	IN
41631	UNKNOWN	unruled
105168	UNKNOWN	unruled
139269	OUT
46302	UNKNOWN	unruled
48460	OUT
9940	UNKNOWN	domain_ambiguity
159645	UNKNOWN	domain_ambiguity
42290	UNKNOWN	unruled
95320	UNKNOWN	domain_ambiguity
67036	OUT
96104	IN
147075	IN
175495	OUT
146541	OUT
98257	UNKNOWN	domain_ambiguity
137389	OUT
100643	UNKNOWN	domain_ambiguity
96858	OUT
131314	UNKNOWN	domain_ambiguity
129300	UNKNOWN	scope_ambiguity
39853	OUT
76806	IN
147069	UNKNOWN	domain_ambiguity
160867	UNKNOWN	domain_ambiguity
72912	UNKNOWN	domain_ambiguity
33152	OUT
2407	OUT
74120	IN
99278	IN
37290	OUT
86942	OUT
155633	IN
53428	OUT
141236	OUT
70177	OUT
172638	OUT
94233	OUT
111338	OUT
62408	UNKNOWN	domain_ambiguity
17227	UNKNOWN	unruled
75964	OUT
17700	UNKNOWN	domain_ambiguity
38524	OUT
40401	OUT
58209	UNKNOWN	unruled
173008	OUT
120961	OUT
39232	OUT
112365	OUT
81506	IN
165974	OUT
176687	IN
78817	UNKNOWN	domain_ambiguity
101059	UNKNOWN	domain_ambiguity
81701	UNKNOWN	unruled
49650	OUT
93660	UNKNOWN	domain_ambiguity
127596	UNKNOWN	unruled
115358	UNKNOWN	unruled
67204	OUT
124665	OUT
94042	OUT
140774	IN
20610	UNKNOWN	domain_ambiguity
63578	UNKNOWN	domain_ambiguity
85200	OUT
85320	UNKNOWN	domain_ambiguity
93365	OUT
164127	UNKNOWN	domain_ambiguity
130214	UNKNOWN	domain_ambiguity
50938	UNKNOWN	scope_ambiguity
174286	UNKNOWN	unruled
121944	OUT
105422	OUT
68911	UNKNOWN	domain_ambiguity
106108	IN
156455	OUT
25801	IN
20548	UNKNOWN	domain_ambiguity
138919	OUT
94936	UNKNOWN	domain_ambiguity
109577	OUT
173501	OUT
25309	OUT
40501	OUT
83915	OUT
151042	OUT
37680	UNKNOWN	domain_ambiguity
97253	UNKNOWN	unruled
84577	UNKNOWN	unruled
89784	OUT
12170	UNKNOWN	domain_ambiguity
65822	UNKNOWN	domain_ambiguity
156979	OUT
141593	UNKNOWN	unruled
130374	IN
84502	UNKNOWN	domain_ambiguity
58347	OUT
64612	UNKNOWN	domain_ambiguity
170138	IN
59217	UNKNOWN	unruled
40371	UNKNOWN	domain_ambiguity
154251	OUT
106838	OUT
81792	UNKNOWN	domain_ambiguity
135610	UNKNOWN	scope_ambiguity
103866	OUT
74008	OUT
14101	IN
130408	OUT
146609	UNKNOWN	scope_ambiguity
11220	OUT
171555	OUT
30911	OUT
128290	OUT
63393	UNKNOWN	domain_ambiguity
54890	OUT
75116	OUT
34904	OUT
10495	UNKNOWN	domain_ambiguity
29915	UNKNOWN	domain_ambiguity
22227	OUT
7817	OUT
13234	UNKNOWN	domain_ambiguity
15758	OUT
139514	UNKNOWN	unruled
86831	OUT
149914	UNKNOWN	domain_ambiguity
143878	OUT
120435	UNKNOWN	unruled
97919	OUT
6500	UNKNOWN	domain_ambiguity
131058	OUT
30907	UNKNOWN	domain_ambiguity
57154	IN
130018	UNKNOWN	domain_ambiguity
177408	UNKNOWN	unruled
118703	OUT
73192	OUT
25909	OUT
134626	IN
155654	OUT
91277	OUT
119521	OUT
108471	UNKNOWN	domain_ambiguity
59157	UNKNOWN	domain_ambiguity
90242	UNKNOWN	unruled
174961	UNKNOWN	domain_ambiguity
122673	UNKNOWN	scope_ambiguity
23299	OUT
54878	OUT
5603	IN
122703	OUT
43534	OUT
96766	OUT
116822	OUT
85515	OUT
115780	UNKNOWN	domain_ambiguity
54375	OUT
113109	OUT
62841	UNKNOWN	domain_ambiguity
95898	OUT
33890	OUT
160733	OUT
```
