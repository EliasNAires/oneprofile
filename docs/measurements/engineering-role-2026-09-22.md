# Iteration 2 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. Criterion revision `33113b2`, unchanged.

The filename carries 2026-09-22 rather than the day it ran, because iteration 1 already holds
`engineering-role-2026-09-21.md` and the next session finds its input by taking the newest file.

This iteration labelled the 1000 rows iteration 1 drew, scored iteration 1's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-21.tsv`; they were written to disk before any
rule of the classifier was read.

## The numbers

The two rates score **iteration 1's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change. That split
is what the loop's design makes available: labels can only score what has already run.

| Gated number | Iteration 1 | Iteration 2 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | — | **5.00%** (30 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | — | **8.00%** (8 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 66.39% | **63.25%** | fell < 1 pp |

The corpus after the change:

| | | Iteration 1 | Iteration 2 |
| --- | ---: | ---: | ---: |
| Vacancies classified | 179 098 | | |
| `IN` | 27 905 | 12.62% | **15.58%** |
| `OUT` | 37 916 | 20.99% | **21.17%** |
| **`UNKNOWN` — the gated share** | 113 277 | 66.39% | **63.25%** |
| &nbsp;&nbsp;of which `unruled` | 71 623 | 57.35% | **39.99%** |
| &nbsp;&nbsp;of which `domain_ambiguity` | 38 107 | 7.06% | **21.28%** |
| &nbsp;&nbsp;of which `scope_ambiguity` | 3 547 | 1.98% | **1.98%** |

`unruled` fell 17.4 points and `domain_ambiguity` rose 14.2, which is the shape a head-list edit
makes: a title that no rule reached now has a known head and an unsettled domain. That is a real
move — `unruled` is ours to fix and `domain_ambiguity` is the corpus's — but it is worth reading as
the accounting change it partly is rather than as 17 points of new decisions.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 1 called `UNKNOWN`, I
labelled 30 `IN`, 244 `OUT` and 26 `UNKNOWN`. One unknown title in ten is an engineering role, so
recall is worse than the 5.00% miss rate says: the pile hides roughly as many `IN` titles again as
the `OUT` stratum does.

## The exit gate

Not met, and not close on the first number: the miss rate is 5.00% against a gate of 2%. This is
the first iteration with a measurement at all, so there is no previous iteration for it to be
consecutive with either.

## Cost

Whole-word, case-insensitive matching over the cleaned title and nothing else. A full pass over all
179 098 cleaned titles takes **0.23s** in memory, against the criterion's ten-second limit. The
endpoint's 53s wall clock is the database.

## What changed, and why

Every change below is grown from a row where my label and iteration 1's answer disagreed.

**26 of the 30 misses were product managers.** `product manager` was ruled a flat `OUT`, inherited
from the seed lists, and it sent `Product Manager Search`, `Payment Product Manager`, `Staff Product
Manager SAP` and twenty-three more to `OUT`. The criterion decides this the other way: a product
manager on a software product reads specs and API definitions (Q3), and a software background alone
makes you a credible candidate (Q4) — which is the reasoning that already puts Scrum Master in. The
criterion is right and the ruling was wrong, so:

- **The `product manager` ruling is gone**, and **`manager` is now engineering-capable**. A product
  manager is decided by its modifier like any other domain-bound head: `product manager mobile` is
  `IN`, `product manager construction` is `OUT`, and a bare `product manager` is
  `domain_ambiguity` — which is the honest answer, because the title alone does not say what is
  being managed. This also picks up `Manager Site Reliability Engineering`, `Manager Information
  Technology` and `Software Development Manager`, which step 6 had left `unruled`.

**Five of the eight false accepts were physical-domain engineering reading as software**, because
`systems`, `test`, `integration` and `security` are qualifiers that exist identically outside
software: `Power Systems Engineer Starship`, `Space Avionics Systems Engineer`, `Propulsion
Integration Test Engineer`, `Test and Launch Engineer`, `Physical Security Engineer`. Rather than
weaken the qualifiers, the domains were named:

- **New off-domain markers** `physical`, `rocket`, `propulsion`, `avionics`, `spacecraft`, `launch`,
  `satellite`, `power systems`, `data center`. The last two are two words, so **markers are now read
  with the same phrase matcher as qualifiers** instead of word-by-word. `data center` is there to
  protect the `manager` change: without it `Data Center Site Manager` becomes `IN` on the `data`
  qualifier.

**Three new heads**, because a head generalises across every title that names it and the handoff
asked for exactly this: `specialist` (iteration 1 flagged it as the largest head deliberately left
off, 4.1% of the corpus), `lead` and `engineering`. All three are domain-bound and
engineering-capable, so they behave like `engineer`.

**A list of technologies by name as software qualifiers**: `it`, `information technology`,
`computer`, `computer science`, `cybersecurity`, `mlops`, `java`, `python`, `javascript`,
`typescript`, `salesforce`, `sap`, `azure`, `aws`, `kubernetes`, `linux`, `blockchain`, `react`,
`sql`, `saas`. Naming a technology is naming a software domain, and this is where `Salesforce
Developer`, `Python Developer` and `IT Systems Engineer` were falling through.

**Four new rulings**, each a decision the lists cannot reach:

- `product owner` → `IN`, read the same way `scrum master` is, and for the same Q4 reason.
- `solutions consultant` → `IN`, matching the `solutions architect` and `solution engineer` rulings.
- `forward deployed engineer` → `IN`; the title names no domain, so step 6 was leaving it open.
- `ai trainer` → `OUT`. This one is the price of adding `specialist`: the corpus holds a family of
  `<domain> Specialist … Freelance AI Trainer Project` postings whose first head is now
  `specialist`, and they used to land on the never-engineering head `trainer`. They are domain
  experts hired to teach a model their own domain, so `OUT` is right for nearly all of them — and
  wrong for `AI Trainer Computer Science Expert`, which is recorded below as a known miss.

## What this did not fix

Four of the thirty misses survive the change, deliberately:

- `Fullstack Engineer Marketing` and `Marketing Data Science Manager` — `marketing` is an off-domain
  marker, and the criterion's step 4 says a marker beats a software qualifier. Dropping `marketing`
  would send every `Marketing Manager` from `OUT` to `domain_ambiguity`, which costs far more than
  these two. The marker stays and these two stay wrong.
- `Machine Vision Engineer Manufacturing Automation` — the same trade, on `manufacturing`.
- `AI Trainer Computer Science Expert …` — the one posting in that family where a software
  background alone is the qualification, against roughly ten where it is not.

Two rows the change newly gets wrong, both single items no rule generalises from: `Product Manager
Virtual Physical Appliances` (the new `physical` marker, reading network appliances as a physical
domain) and `Specialist L2 Body Integration` (the new `specialist` head over the `integration`
qualifier). `Document Control Quality Systems Administrator` and `Research Communications Analyst
Critical Infrastructure` were false accepts before the change and remain so.

## What the next session should look at

The miss rate is the number that fails, and after this change the product-manager family should
stop dominating it — so the next sample's misses are the first honest look at what else is wrong.
Watch for whether `manager` being engineering-capable overshoots: `Manager Quality Assurance` is now
`IN` on the `quality assurance` qualifier, and that is the kind of title the criterion would call
`scope_ambiguity`.

On coverage, `unruled` is still 39.99% and the thirty `UNKNOWN` rows I labelled `IN` point at heads
and phrases nothing reaches: `director`, `president`, `owner`, `roboticist`, and rulings such as
`developer relations`, `technical support engineer` and `quantitative developer`. `director` was
considered and left off this round: it would convert a large number of plainly commercial titles
from `unruled` to `domain_ambiguity` without deciding any of them.

One loose end iteration 1 created is still open: `docs/engineering-role-criterion.md` points at
`docs/engineering-role-seed-lists.md`, which no longer exists. The criterion is the developer's
document and changes only in a grilling session, so the dangling reference is left alone.

## Held out of the draw

The 1000 ids in `src/test/resources/labels/engineering-role-2026-09-21.tsv` and the 100 distinct
titles in `src/test/resources/calibration/`. 174 685 of the 179 098 rows were eligible; the loss is
almost entirely duplicate titles matching the calibration set, which is held out by title rather
than by id.

## The sample for iteration 3

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
44534	Executive Assistant
143535	GNC Engineer ADCS Starlink
30468	Full Time Grocery Merchandiser San Mateo CA
123229	Fluent Danish Speakers AI Trainer Freelance Remote Aarhus Denmark
137682	Executive Relations Manager Events Live Journalism Contract
47159	Copywriter FTC
111013	Specialist Footwear Tooling Engineer LightSpray
121910	AI Trainer Materials Science Specialists Remote Vancouver
24111	Account Executive Enterprise
121235	Staff Software Engineer
77588	Delivery Lead
178869	Marketing Manager Benelux
150996	Personal Trainer
75226	Warehouse Associate Day Shift Days Off Monday Tuesday Wednesday
94250	Staff Nurse ICU FT Nights LHAAMC
124382	Pathologists Freelance Remote Austin US
131358	Production Manager Avionics Manufacturing
157791	Clerical Assistant
71476	ENT Business Development Representative Talent Pool
118043	Customer Service Representative
93445	Part Time Brand Ambassador
39101	Solutions Analyst Wholesale Finance
24739	Licensed Psychologist Remote
13285	Business Development Executive Dutch Speaking
26402	Performance Marketing Specialist Mobile
173381	Veterinary Assistant
366	Outside Sales Representative Roofing
167830	Mid Enterprise Corporate Account Executive Quebec City
171119	Don t see your dream job here Apply to Vox Media for future openings
146589	IT Support
54883	Fitness Counselor
156813	Software Engineer with Java Warehouse Automation
173787	Engineering Professional Site Civil Engineering
54655	Certified Personal Trainer
145864	Software Engineer Platform Oxford United Kingdom
153221	Personal Trainer
76190	Project Manager Construction
54399	Assistant Kids Club Manager
10591	IT Operations Manager M&A
124674	Product UX Professionals AI Training Portland USA
159794	Medication Nursing Assistant MNA
21752	Direct Support Professional DSP Special Needs
14929	Construction Project Manager Co op Summer Fall 2027
55724	Kids Club Manager
146830	Growth Performance Marketing Manager
16739	Engineer II Mechanical Engineering
152195	Personal Trainer
147523	Executivo a Comercial Hunter para Grandes Contas Itajaí SC
77717	HVAC Shop Technician
48931	HR Executive Specialist
143426	Facilities Technician Temporary
2085	Mission Analyst
116875	Strategic Account Executive
1713	Data Scientist Application Developer Secret cleared
13891	Manager Revenue Accounting
74676	HVAC Install Lead
3591	Payload Specialist
38028	Marketing Events Manager Part time
168398	Emergency Credentialed Veterinary Technician Part time Las Vegas NV
29606	Veterinarian
82760	Sales Assistant Stage Guidonia C.C Tiburtino
114131	Certified Nurse Midwife CNM
163518	Field Engineering Manager
40065	Licensed Practical Nurse LPN Urgent Care
49242	Mechanical Engineer Gas Energy
30975	Site Reliability Gitops Engineer
9440	Traveling Assistant Superintendent Data Center Construction
94075	Nursing Graduate Heart and Vascular Unit October 2026 Cohort
34774	Manager Accounting Advisory
113720	Assistant de vie
6359	Sales Representative
115271	Business Development Representative BDR NL
77724	Master HVAC Mechanic
93135	Adv Verona
4564	Pavement Condition Index PCI Survey Annotation Specialist Freelance AI Trainer Project
86264	Mission Critical Master HVAC Technician
85942	Partner Support Associate I
26779	Psychiatric Mental Health Nurse Practitioner PMHNP
166579	Maintenance Technician Landings at Hard Scrabble
143804	Manager Automation Controls Engineering Power Generation Utilities
23031	Registered Nurse
28531	Account Executive Enterprise
4234	Geospatial Reasoning Specialist Freelance AI Trainer Project
107478	Customer Service Representative Hybrid Rotating English European Languages
25210	HOUSTON Electrical Engineer New Grad
164285	Android Developer UniFi Design App
19628	Key Account Executive
112674	Patient Coordinator
173041	Associate Veterinarian DVM Reidsville Animal Hospital AAHA accredited
54413	Assistant Kids Club Manager
127189	Band 8a b Psychologist Prison
152838	Personal Trainer
55300	General Manager in Training
85430	Director Payments and Fraud Operations
143411	Facilities Engineer Fluids Cryogenics
89121	Ops Process Operator Obelisc
115480	Chemical Batching Technician 2nd Shift
138297	Graduate Sales Development Representative at SetSales
46193	Client Coding Project Manger CCPM
158484	Inside Sales Representative
164386	Data Architect Databricks Lakehouse
68134	Enterprise Account Executive Chicago
18088	Group Manager Product Marketing
120944	Sales Executive Spanish speaking
176306	Unsolicited Application WPP Media Copenhagen Office
40424	Meal Plating Associate
150063	Software Engineer Backend
4102	Computer Information Systems Specialist Freelance AI Trainer Project
12580	Clinical Specialist TAVR EU
69464	Enterprise Account Executive Growth Central Remote
22376	LPN Nurse New Grad Residency
14711	Lead Supply Chain Manager
97134	Travel Nurse Practitioner Physician Assistant
23388	Residency Program New Nurse Graduates
4007	Arabic Voice Actor Freelance AI Trainer Project
110646	Primary Care Functional Medicine Physician Assistant
95764	Retail Security Officer Verrado Marketplace
27745	Psychiatric Mental Health Nurse Practitioner
3521	Manager Marketing United Heritage Hall
167650	Veterinary Coordinator Internal Medicine
67882	Commercial Account Executive MENA
59361	SDR
49880	Director Grocery Retail Services
143032	Avionics Manufacturing Engineer
41329	Sommelier
140459	Embedded Software Engineer Intern
13484	Civil Engineer Transportation
50998	Operations Project Lead
161262	Inside Account Executive Brampton
20063	Render ATL Leadership Development Internship 2027
94256	Staff Nurse Medical Oncology
81254	Golf Services
71774	Infrastructure Engineer Manufacturing Datacenter
50248	Technician Lead Labs Bay Area
146464	Sales assistant Sport Zone Marco de Canaveses
11411	Industrial Engineer Simulation
121508	AI Trainer Advanced Indonesian Fluency Netherlands
32631	名古屋 Part Time Brand Representative 週5日勤務
34441	Registered Behavior Technician RBT
109576	Onsite Construction Administration Project Manager Data Centers
154380	Personal Trainer
146479	Sales assistant Sport Zone Porto Mar Shopping
88330	Project Manager Civil Engineering
14469	Staff Electrical Engineer
79979	Accountant
176247	Video Marketing Manager Media Planning Mensch
95515	Manager Salesforce Marketing Cloud Development
26243	Mid Market Account Executive
160134	Gym Host
111000	Lead Staff Software Engineer
177355	Analista de Planejamento Comercial
33123	Business Development Representative SaaS Sales
111728	Registered Behavioral Technician RBT Queens NY
60376	Trade Marketing Events Manager
57241	Field Technician Mechanic Pump Power HVAC
71395	Staff Product Designer Finance
139079	Pharmacy Technician Pharmacotherapy Support
44241	Retail Activation Manager
154296	Personal Trainer
47592	Commercial Service Technician Trainee
109646	Civil Engineer Site Design
85814	Construction Manager Owner s Representative Data Center Construction
15404	Project Manager Construction
47414	Dalio Family Office General Interest
42589	Product Designer Rocket Growth
13437	Events and Ministry Coordinator Onsite
2388	Assistant Director Writer
67750	Mechanical Engineering Intern Co op Satellite Bus Winter Spring 2027
154706	Personal Trainer Columbus OH
151847	Personal Trainer
174525	אחראי ת משמרת לב הארץ Shift Supervisor Lev Ha arets
153573	Personal Trainer
4942	Consultor de Vendas Interlagos SP
81492	Facilities Technician III
51838	Technical Support Specialist I
15353	Construction Project Manager New Grad 2027
71040	Assistant Teacher
152014	Personal Trainer
23242	Registered Nurse RN Home Care Weekend Nights
139089	Remote Clinical Pharmacy Specialist
25426	Team Leader Arlington Heights
2177	Systems Engineer mid level
151297	Personal Trainer
4039	Bengali Language Specialist India Freelance AI Trainer Project
114175	Revenue Accounting Manager
2743	Enterprise Account Executive
85363	Pre Payroll Specialist
151584	Personal Trainer
51405	EDI Mapping Specialist with e invoicing
97005	Primary Care Nurse Practitioner Physician Assistant
79773	Product Manager Remote Contract
37130	Mid Market Onboarding Coach
95958	Account Executive Korea
117685	Assistant Store Manager
14879	Assistant Superintendent New Grad 2027
153486	Personal Trainer
119722	Conference Executive
140080	Manager Digital Marketing Analytics
93008	Engineering Project Manager Facilities Capital Projects
10582	International Legal Counsel
154088	Personal Trainer
146935	Director Customer Success
108137	Client Finance Traffic Executive
136745	AI Training Education Analyst
130109	21GRAMS Technical Delivery Manager
5274	Commercial Strategy Manager
117412	Product Manager Lead Communications Platform
85086	Account Executive LATAM
13350	Events and Ministry Coordinator Onsite
88340	Project Manager Civil Engineering
13815	Direct Marketing Manager II
141320	Independent Sales Representative
154626	Personal Trainer Ashburn VA
15593	VP Financial Planning Analysis
64416	Patient Financial Services Representative
87882	Veterinary Assistant Heritage Pet Hospital
25852	Implementation Consultant
14271	Growth Analyst
1153	Office Assistant
135775	Specialist Seller Connected Maintenance Public Sector
16278	Software Engineer
108402	Product Manager Okta Identity Threat Detection and Response Products
145820	Software Engineer Platform Leeds United Kingdom
100504	Branch Manager
177424	Banco de Talentos de Operações Offshore
129093	Data Scientist wiqRetail
75229	Warehouse Lead Night Shift Days Off Monday Tuesday and Wednesday
8216	Customer Success Manager LATAM
33342	Country Manager South Korea
131355	Turbomachinery Test Responsible Engineer
2731	Solar Maintenance Technician Traveling
76649	Transaction Coordinator
46978	Dental Office Manager
54141	Assistant General Manager
66438	Executive Assistant
150689	Personal Trainer
128617	Member of Technical Staff SSD Firmware Development Engineer
86341	Site Operations Coordinator Moses Lake
113679	Assistant d agence Gestionnaire de planning Aide à domicile
139038	Clinical Pharmacy Specialist
101795	Director Strategic Finance
4500	Mathematics Specialist Fluent in Spanish Latin America Freelance AI Trainer Project
27980	Enterprise Account Executive Northeast
34831	Campus Executive Director
67602	Research Technology Librarian
166251	Registered Nurse Level II NICU
96550	Manager Reliability Platform Engineering
55389	Group Fitness Instructor
64515	Director or Vice President Product
105617	Manager Media Strategy
18698	Account Executive CRM
31340	EBA 4.4 Changes Resource
154907	Personal Trainer Longmont CO
62590	Graduates 2021 Summer intake
145711	Software Engineer Platform Bangkok Thailand
159713	Industriemechaniker gn
150556	Personal Trainer
97104	Travel Certified Nursing Assistant
108375	Large Enterprise Account Executive Okta
105254	Digital Network Exploitation Analyst Mid
93500	Finance Business Partner Transportation
140152	Manufacturing Supervisor 3rd Shift
132828	Account Manager North East Edinburgh
36873	EDI Technical Solutions Manager
30227	Gerente de Negócios Pleno Comercial Alta Renda
173338	Veterinarian Oconomowoc Animal Hospital
95393	HVAC Service Technician
18625	Staff Accountant
34836	Esthetician Instructor Future Opportunities
95051	AI Engineer India
156163	Manager Total Rewards
75944	Supplemental Sales Agent Jacksonville FL
16762	Engineer I Metrology
97017	Primary Care Nurse Practitioner Physician Assistant
12752	Enterprise Account Executive Digital Native Business
10081	Medical Assistant
62094	Executive Assistant Sales São Paulo Brazil
152648	Personal Trainer
115937	Software Engineer
87728	Forward Deployed Engineering Manager
152268	Personal Trainer
46420	Manager Clinical Operations
127263	Clinical Research Nurse London
117056	Staff Software Engineer Big Data tvScientific
65798	Full Stack Growth Engineer
150929	Personal Trainer
139494	Territory Representative
104403	Lead Machine Learning Engineer Geometric Spatial AI
117060	Staff Systems Engineer Oracle EBS Accounting
68370	Commercial Customer Success Manager
167288	Manager Data Strategy
62817	Recent Graduate Trading Assistant NYC
153707	Personal Trainer
85212	Software Engineer
42064	Staff Backend Engineer
165039	Private Equity Conference Volunteer
164865	Event marketing internship paid at United Media
137725	Security Front Desk Coordinator
121719	AI Trainer Fluent Kazakh Speaker Leipzig Germany
110739	Staff Software Engineer
39186	Director Pre Construction Data Centers
40063	Licensed Practical Nurse LPN Urgent Care
135947	Commercial Real Estate Advisor
80857	Account Executive Professional Services
173620	Legal Assistant Personal Injury
134314	Systems Engineer Mechanical
54158	Assistant General Manager
177888	Pediatrician
82089	Account Executive
98130	Associate Account Executive Houston
101665	Pricing Representative Temp to Hire
133603	Software Engineer Fraud
72301	Account Manager Health and Wellness
124035	Linguist Translator Graduates AI Training Ecatepec Mexico
117722	Assistant Store Manager
96975	Primary Care Nurse Practitioner
118465	Convergent Video Audio Lead
154146	Personal Trainer
86015	Agentic AI Engineer
165043	Private Equity Conference Volunteer
13786	Account Executive SMB
152906	Personal Trainer
121502	AI Trainer Advanced French Fluency Remote Montreal
86371	Event Marketing Specialist EMEA
52065	Outside Sales Engineer
18465	Software Engineer Frontend
163245	Delivery Driver
121094	Customer Care Manager
127262	Clinical Research Nurse Leicester
122142	AI Training Fluent Japanese Speakers Irvine US
62676	Mortgage Loan Originator Pinedale
117675	Assistant Store Manager
54145	Assistant General Manager
106306	Partner Executive SLED
113744	Assistant e de vie
35163	Events Executive
8161	Store Manager Westport
102202	General Liability Pre Suit Attorney
27677	Psychotherapists
46091	Product Marketing Manager
148084	Aircraft Maintenance Systems Technician
122165	AI Training Fluent Japanese Speakers Sakai Japan
127758	Locum Biomedical Scientist Histology Chichester
70671	Software Engineer
163457	Customer Success Manager General Manufacturing
117982	Customer Service Representative
158453	Housekeeper
73414	Dialysis Technician Little Rock AR
32351	AI Engineer
23255	Registered Nurse RN Home Health
151799	Personal Trainer
94790	Home Infusion Nurse
136207	Student CEO SCEO SUNY PLATTSBURGH
151062	Personal Trainer
150350	Personal Trainer
114470	Customer Service Representative Onsite
164686	Litigation Paralegal
73234	Board Certified Behavior Analyst BCBA
20292	Hospital Specialty Sales Representative West Chicago
176598	Executive Sous Chef Memphis
57044	Safety Coordinator I II Midstream Operations
154958	Personal Trainer Naperville IL
7455	Operations Supervisor DC
120298	Growth Marketing Specialist
83567	Assistant Store Manager JD Venlo
112361	Medical Assistant
117683	Assistant Store Manager
134680	Staff Software Engineer Demand Bidder Ad Serving Platform
152631	Personal Trainer
33396	Banquet Coordinator Conferencing Office
66077	Account Executive Southwest
67372	Project Manager Civil Geo
41644	Assistant Manager Catalog Operation Management 商品資訊營運助理經理
178049	Resident Assistant 2026 2027
47730	Outside Commercial Sales Representative Integrated Security Systems Experience Needed
153225	Personal Trainer
172142	Staff Machine Learning Engineer Data Flywheel
150230	Personal Trainer
72979	Quality Supervisor
126396	Band 6 CT Radiographer Shetland Islands
4533	Norwegian Language Specialist Freelance AI Trainer Project
118088	Customer Service Representative Bilingual Spanish Preferred
38900	Medical Director
153818	Personal Trainer
103939	Account Nephrology Representative
167835	National Channel Sales Manager Convergint Eastern US
41322	COTE Miami Bar Back
164492	Tax Manager
171385	Service Coordinator
79331	Manufacturing Engineering Technician Automation
82158	Front Desk Coordinator
152537	Personal Trainer
156924	Assistant Store Manager
64171	Software Engineer
57316	Heavy Equipment CDL Driver
161474	Territory Account Executive Retail South Texas Spanish Bilingual
95281	Yard Coordinator
13272	Account Support Representative Bilingual Preferred
16452	Account Executive
4223	Garment Manufacturing QC Specialist Freelance AI Trainer Project
36561	Sales Enablement Representative
165093	Talent Acquisition Specialist at United Media
151228	Personal Trainer
156011	Nebraska Waitlist Contract Psychiatric Nurse Practitioner
174130	Marketing Associate 2+ Yrs Experience 100 Remote
177850	Geotechnical Engineer
109218	Producer
67862	Backend Engineer AMER
150240	Personal Trainer
176050	Programmatic Account Manager CDI
54118	Assistant Fitness Manager
77630	Manager Security Operations Center EMEA
6392	Recruiter
154476	Personal Trainer
171196	Mechanical Engineer II Structural Analysis
137181	Lead Data Architect
56051	Operations Manager
89925	Foundry Engineer
140154	Manufacturing Supervisor Finishing 2nd Shift
167953	Field Marketing Manager Mid Market West
54616	Certified Personal Trainer
170603	Physical Therapy Assistant PTA
82825	Store Manager Ravenna C.C ESP
9657	Strategic Account Executive
82893	Assistant RH juridique
53029	Enterprise Account Executive Southwest
133771	Group Lead UK Test Site
27127	Psychiatrist MD
47948	Certified Nurse Midwife CNM Laborist Full Time
146272	Career Coach Part time Contract US Remote
145304	Supply Chain Planner xAI Starlink
77173	Sales Executive at HRtechX
81432	Linux System Administrator Defense Products
107867	Maintenance Technician Maintenance Technician
72206	Logistics Coordinator Contractor
56610	Stretch Trainer
116004	Sales Director Brand Direct NYC
148288	Creative Director Event Design
105337	Solutions Engineer
120867	Management Consulting Event Volunteer
169820	Algorithm Developer
121126	Director Customer Success
24571	Manual QA Engineer Sportsbook
10004	Construction Project Manager Intern Summer 2027
527	Oral Surgery Dental Assistant
17654	Technician I Manufacturing Production Assembly El Paso to Midland
169838	Director of Launch Operations
44806	Enterprise Account Executive Healthcare
117166	Customer Success Manager Civil Government
110444	Quality Assurance Engineer PLC Automation
8087	Seasonal Sales Associate Part Time Utica Square
153178	Personal Trainer
9985	大手直販営業 Enterprise Account Executive Finance
172048	Materials Program Manager
39739	Glass Installer
143796	Maintenance Engineer PCB Starlink
150526	Personal Trainer
9635	Sales Development Representative DACH
16721	Engineer I Global Product Support
89360	Warranty Technician
16797	Engineer Manufacturing Quality
65294	Additive Manufacturing Development Engineer
151843	Personal Trainer
63117	Automotive Technician
96882	Nursing Care Coordinator LPN RN
162730	Assistant Disposal Operations Manager
43955	Frontend Engineer
144623	AI Engineer Special Programs Top Secret Clearance
135005	Renewal Sales Specialist I Night Shift 5 30 PM to 2 30 AM
116226	Executive Director Site Head
1157	Quality Control Manager Federal Construction
120719	Investor Relations Strategy Manager French speaking
63325	Sales Manager Chinese Speaking
152473	Personal Trainer
95244	Water Wastewater Design Engineer
8852	R D Embedded Software Engineer
9124	Licensed Psychologist
69955	Assistant General Manager
5301	FX Controller Controller Bangkok based relocation provided
31302	Data Engineer DEPS MTCR
59343	Regional Sales Manager
132879	Account Executive
10972	Production Operations Manager Maritime
35051	Conference Coordinator Volunteer
58889	Part Time Ambassador
61875	Account Development Representative Portuguese Speaker
123233	Fluent Danish Speakers AI Trainer Freelance Remote Horsens Denmark
25184	WEST PALM BEACH Land Site Development New Grad
94949	Legal Operations Specialist
140142	Maintenance Planning Manager
16959	Mid Level AI Platform Engineer SAIQ
154526	Personal Trainer
105797	Account Executive E commerce
42015	Manager Trust and Safety Compliance Audit
89812	GCP Cloud DevOps Engineer
136583	Product Marketing Manager
138621	Talent Acquisition Intern Remote
122968	Data Science Analysis AI Training Boston US
146993	Business Development Representative BDR
46870	Field Service Engineer
11145	Robotics Software Engineer Sensor Integration
45938	Partner Marketing Manager Cloud Channel
109860	Trainee Account Executive
106857	Medical Advanced Practice Provider PRN
150889	Personal Trainer
166388	Staff Controls Software Engineer
168687	Emergency Veterinarian Part Time Whitby ON
129403	Quantum Scientist Time to Solution Benchmarking
164153	Assistant Operations Manager
148301	Customer Success Manager
117454	Supervisor de Embajadores Ciudad Juárez
95333	Warehouse Operations Manager
150983	Personal Trainer
18773	Maintenance Technician 500 SIGN ON BONUS
118845	Licensing Coordinator
166262	Registered Nurse Preop PACU PRN
124050	Linguist Translator Graduates AI Training Marseille France
121503	AI Trainer Advanced French Fluency Remote Nice
102972	Daft US Software Engineer Interest Form
150330	Personal Trainer
30714	Polisher
177079	DFM Engineer LATAM
143755	Lead Mechanical Engineer Starship Avionics
23316	Registered Nurse RN Preceptor Home Health
10025	Project Manager Construction
3148	Engineering Manager Data Delivery Platform
34074	Center Based Registered Behavior Technician RBT
36417	Account Executive Bill Pay Ejecutivo de Ventas Cuentas por Pagar Mexico City Hybrid
48723	Business Development Representative
132522	Sponsorship Analyst at Retail Insights
125704	Associate Director Demand Facilitation EMEA
93006	Manager Process Safety
38106	Software Engineer Backend
161967	Director Software Engineering Retail Platform Delivery ELERA
39284	Patient Connection Manager
160255	Account Executive
108354	Enterprise Account Executive Auth0
39693	Sales Development Representative
154151	Personal Trainer
136620	Strategy Analyst
139088	Remote Clinical Pharmacist Specialist
155450	Mechanical Engineer Critical Facilities
19023	Software Engineer
122997	Data Science Analysis AI Training San Francisco US
15607	Manager Project Execution Operations Technology
15171	Project Manager Mission Critical Construction
127743	Locum Band 8A Mental Health Pharmacist Staffordshire
12604	Scientist I II Analytical Development
146630	Workplace Solutions Manager
26192	Enterprise Account Executive Commerce
151308	Personal Trainer
123110	Dutch Fluent Speakers AI Training Tilburg Netherlands
152160	Personal Trainer
120033	Event Coordinator Polish speaking
102861	Pastoral Care Coordinator
92356	CDL Driver
86454	Integrated Marketing Manager
4636	Science Specialist Fluent in German Freelance AI Trainer Project
88169	Intern Site Civil Engineering
90402	Approved Mental Health Professional
115868	Clinical Trial Manager
21531	Certified Occupational Therapy Assistant Home Health
50446	Partner Marketing Manager AI ISV Ecosystem
176067	Retail Media Manager
84946	Legal Operations Specialist
93646	Structural Repair Technician Automotive
4617	Radiological Health Freelance AI Trainer Project
103187	Mechanical Engineer Spacecraft Instrument Mechanisms
90373	Advanced Practitioner Independent Futures
4756	Ukrainian Language Specialist Freelance AI Trainer Project
4791	Wetlab Protocol Specialist Freelance AI Trainer Project
4456	Malayalam Language Specialist Freelance AI Trainer Project
103068	Business Development Manager Stormwater Maintenance Construction
154645	Personal Trainer Birmingham AL
7765	Seasonal Operations Associate Part Time Associé aux opérations saisonnier temps partiel Carrefour Laval
24237	Property Manager West San Antonio TX
151075	Personal Trainer
116900	Occupational Therapist Industrial Rehab
126197	Band 5 Registered General Nurse RGN Harrogate
131147	Store Manager Full Time Corte Madera
173092	Associate Veterinarian Wildwood Animal Hospital Gresham OR
135906	Staff Engineer SSD Storage and Systems Architecture
97535	Seasonal Stylist Retail Part time
177057	Account Executive III Defense
15807	Analista de Desenvolvimento Sênior
138385	Telesales team Lead at SetSales
25364	BIBIBOP Team Member Kenwood
13032	Data Center Capacity Delivery Manager AUS
104002	Clinical Oncology Specialist Head Neck
57850	Shop Technician Mechanic
89677	Customer Operations Executive Portuguese or Spanish Speaking
18111	Statistician
177053	Account Executive II
103520	Account Executive
145280	Supplier Development Engineer Mechanical Falcon Dragon
10051	Project Manager Hyperscale Construction
169943	Product Manager Rider and Driver Mobile Experience
94223	Staff Nurse 4 Medical Med Surg Tele
109960	Tanzania Internal Investigation Supervisor and Coordinator Fixed Term
49577	Product Analyst
32548	Product Marketing Manager
164784	Conference Coordinator Volunteer
57981	Territory Account Manager Pump Power HVAC
45298	Manager Compensation Analytics Intelligence
28258	Executive Assistant
124169	Mental Health Professionals AI Training Newcastle upon Tyne UK
162227	Sales Representative Institutional Test Prep
136587	Sales Development Representative
130415	Director Clinical Pharmacology
171204	Mechanical Properties Technician
123365	Game Development Environment Artist Chicago US Freelance Remote
57235	Field Technician Mechanic Pump Power HVAC
141344	Independent Sales Representative
164312	Thermal Engineer
154407	Personal Trainer
153121	Personal Trainer
155781	Content Marketing Manager
153653	Personal Trainer
154960	Personal Trainer Natalia TX
44019	Structural Engineer
42843	Winston Salem Commercial Sales Representative
176904	Data Architect
82058	Customer Service Representative Haslet
58554	Software Validation Engineer Python
61354	APAC Tax Manager
40563	Director National Field Access Central
23873	Marketing Manager Diagnostics
130792	Manufacturing Equipment Engineer
77218	Accounts Payable Clerk
145976	IT Service Desk Technician
67272	Automotive Technician Remote Contractor
34732	Home Health Nurse LPN RN
150437	Personal Trainer
13355	Events and Ministry Coordinator Onsite
58057	Yard Technician
37584	Systems Engineer Data Intelligence Analytics Team
49513	Account Executive DoiT Cloud Intelligence US West
40121	Medical Receptionist
63079	Automotive Technician
52724	Customer Success Manager France Southern Europe
112173	E Scooter Delivery Driver
69614	Embedded SW FW Engineer Bringup Bengaluru multiple vacancies
154290	Personal Trainer
121308	Talent Project Executive
153091	Personal Trainer
41235	Maintenance Technician Cortland Harbour Cove
147169	Software Development Engineer in Test
93181	Assistant Property Manager
45645	Commercial Account Executive Boston
158105	Sales Representative bilingual
117331	Back Office Support Coordinator
93930	Clinical Escort Assistant
2780	Enterprise Account Executive
89533	AI FDE Forward Deployed Engineer Manager
153544	Personal Trainer
54321	Assistant General Manager New Gym Opening
129291	Quantitative Researcher Metals
39054	Closing Coordinator
152110	Personal Trainer
27011	Psychiatric Mental Health Nurse Practitioner PMHNP
36290	Recruiter
62812	Experienced Lateral Quantitative Researcher
10611	Lead Manufacturing Engineer Analytics Digital Tools
117698	Assistant Store Manager
14657	Product Consultant
4971	Cyber Security Manager Campinas SP
120479	Investor Development Associate Danish speaking
58475	Customer Success Manager 9 month contract
4310	Indonesian Language Specialist Freelance AI Trainer Project
152831	Personal Trainer
39222	Maintenance Technician High Rise Lease Up Sign on Bonus 1500
52407	Licensed Practical Nurse RN Respite
39659	Associate Consultant Employee Benefits
8145	Store Manager Fashion Island
32384	Sales Representative
106200	Administrative Assistant External Agency Staff
73799	Urgent Hiring Helpers for In Home Support Eugene OR 97402
152729	Personal Trainer
57734	Rental Coordinator
145593	Dental Assistant
109581	Power System Studies Project Manager Data Center
46398	Account Executive
21325	Admissions Registered Nurse Home Health
127591	Dietitian Band 6 East Midlands
132330	Digital Marketing Coordinator at Retail Insights
34751	Private Duty Home Health Nurse Registered Nurse
138983	Branch Account Executive
71155	Substitute Teacher
151602	Personal Trainer
84337	Store Manager Las Olas FL
67331	Segment Product Marketing Manager Public Sector
155428	Plumbing Engineer I
160358	Retail Sales Manager Minneapolis
79589	Marketing Campaign Coordinator Specialist
33900	Behavior Technician Work With Kids Training Provided
125424	Account Executive Abu Dhabi
144877	Mechanical Engineer Starship Avionics
132878	Account Executive
134167	Manufacturing Engineer I Assembly Integration and Test
19506	Data Scientist Agentic AI Systems
55089	Fitness Manager
70747	Service Technician
135577	Associate Sales Engineer SE Desk Southeast
160967	Project Manager Facilities Maintenance Operations
94246	Staff Nurse Heart and Vascular Unit
150255	Personal Trainer
4492	Mathematics Specialist Fluent in Japanese Freelance AI Trainer Project
113707	Assistant de vie
150322	Personal Trainer
132905	Client Support Analyst
28547	Account Executive Open Application
116605	General Dermatology Physician Assistant or Nurse Practitioner
121759	AI Trainer Fluent Macedonian Speakers US
74209	Technical Project Manager
25879	DevOps Engineer Cloud Operations
6122	Medical Assistant Full Time Part Time
137509	Intern Engineering Sciences Summer 2027
32364	Terminal Operations Manager Drayage Port Logistics
151419	Personal Trainer
102081	Canvassing Manager
21864	Home Care Licensed Practical Nurse 1 1
139918	Partner Marketing Manager
67681	Quality Engineer
102606	C++ Software Engineer
104288	Registered Nurse RN Manager
162878	Customer Logistics Coordinator
168421	Emergency Credentialed Veterinary Technician Port St Lucie FL
134000	Manufacturing Engineer Nightshift
6261	Engineering Geologist
117902	Customer Service Representative
138830	Marketing Specialist Amazon US
143702	IT Systems Administrator Manufacturing
147555	Novos Negócios Analista de Produtos Sênior Presencial RJ
107677	System Operator Trainer
173582	Manager CFO Advisory Close Consolidation
95036	Product Marketing Manager Measurement Data Audiences
143402	Executive IT Support Specialist
65513	Demand Generation Lead
176636	Journeyman Electrician Construction
126129	Band 5 Neonatal NICU Nurse Nottingham
59887	Apartment Maintenance Manager
32942	Architect Cloud Identity Infrastructure
119345	Field Service Technician NETA 3
96606	Retail Assistant Manager
118554	Regional Customer Success Manager Automotive
88930	Clinical Lead Health Solutions
102729	Business Development Representative
112174	E Scooter Delivery Driver
147864	Vendedor a Externo 6 horas Carpina PE
90408	Approved Mental Health Professional AMHP Hub
154683	Personal Trainer Charlotte NC
112118	Founding Account Executive BNLX Belgium market
77322	AI Resilience Fund Manager
24333	IT Help Desk Lead Information Technology
65316	Manufacturing Engineering Intern Summer 2027
60124	Pediatric Physical Therapist 500 Bonus EI Preschool
48676	Executive Chef
106232	Manager Global Event Production
128801	Subscription Services Representative
171883	Pharmacy Student Clinical Support Specialist Ohio Temporary Part Time
82780	Stage Sales Assistant Full Time Modena C.C Grandemilia
42083	Staff Machine Learning Engineer Coupang AI Foundations
15622	Director of Product AI Underwriting
47919	Project Manager
97435	Floor Lead Retail
176156	Executive HR
156983	Seasonal Sales Associate Alamo Heights
142152	Sonder Responder Queensland
115123	Director Yield Management
46909	Packaging Mechanical Engineer
4472	Māori Language Specialist Freelance AI Trainer Project
22167	Licensed Practical Nurse Home Care
13944	Marketing Operations Manager
6807	Speech Language Pathologist Full Time
49767	Engineering Manager Resolutions Platform Brazil
117687	Assistant Store Manager
82753	Sales Assistant Part Time Verona C.C Adigeo
158481	Field Sales Representative Paris
94807	Infusion Nurse
95080	Help Desk Technician Tier 1 2
125394	Onsite Contract Spanish Interpreters
123072	Dutch Fluent Speakers AI Training Fully Remote Breda
11532	Mechatronics Engineer Electro Mechanical PCB design Motors Actuation
70092	Personal Care Specialist Part Time
108842	Java Engineer OKX Pay Smart Account Team
248	Technical Animator
125921	Band 3 Locum Therapy Assistant Epsom
117736	Assistant Store Manager
13315	Events and Ministry Coordinator Offsite Part Time
108260	Mechanical Design Engineer Fuel Recycling
98903	Project Manager Michels Pipeline Inc
61317	HVAC Service Technician
46128	Account Executive
76211	Project Manager Construction
41228	Maintenance Technician Austin TX
121962	AI Trainer Vietnamese Kanazawa Japan
153787	Personal Trainer
152701	Personal Trainer
87943	Designer II Civil Engineering
52752	AI Native Marketer Digital Lifecycle
128187	Registered Mental Health Nurse Queen s Hospital
138024	Strategic Growth Partner
88993	Mid Market Account Executive
8779	Electrical Field Representative
159327	College Marketing Representative Atlanta The Orchard
92860	Sales Executive
118070	Customer Service Representative Bilingual Preferred
124282	New Zealand Residents Survey Participants Wellington New Zealand
113785	Auxiliaire de vie Contrat Etudiant
176389	Lead Identity Access Management Specialist
141135	Accounts Receivable Coordinator SMCP
131833	Mobility Specialist AMER
12358	Technical Program Manager Advanced Effects
174928	Athletic Trainer SIGN ON BONUS
113702	Assistant de vie
165567	Agent Experience Coordinator
8948	Director Project Team Leader Executive Committee Business Manager
45772	Indirect Tax Manager
58737	AI Engineer Intern
76291	Real Estate Acquisition Consultant
152283	Personal Trainer
140626	Wound Care Nurse Practitioner
174916	Athletic Trainer
97480	Floor Lead Retail Part time
47321	Partner Solutions Engineer LATAM
23363	Registered Nurse Weekends Home Health Living
77353	Chief of Staff Bay Area
51752	Patient Coordinator
34584	Registered Behavior Technician RBT
171127	Creative Director Temporary
126095	Band 5 ITU Nurse Macclesfield
178236	Product Manager Technical Identity Access Management
67565	Legal Executive Assistant
5328	Manager Data Analytics Bangkok Based Relocation Provided
111066	Store Advisor University Town Center
46704	FDE Data Engineer FedD140 FedD148
93908	Registered Nurse Case Manager 5k Sign on Bonus
142998	Automation Controls Engineer Asset Engineering
50490	Blockchain Strategy Analyst
107704	Video Editor Brazil
113277	Admissions and Customer Service Coordinator
168999	Emergency Veterinary Assistant Woodbury MN
123360	Game Development Environment Artist Bristol UK Freelance Remote
67059	Sports Data Collector Ice Hockey Visby Sweden
42162	Staff Backend Engineer Streaming AI Infrastructure
30562	Compliance Assurance and Training Specialist
88427	Staff Geotechnical Engineer
45583	Workplace Specialist
38103	Office Coordinator Part Time
157458	Founding Account Executive Nordics
142774	Manager Facilities
14540	Combustion Engineer Syngas Oxyfuel
80700	SOFTWARE DEVELOPER SPECIALIST II JAVA CARDS Lavras
132912	Director of Engineering Infrastructure
169533	Veterinary Assistant
102244	Legal Assistant
112440	Medical Coding Specialist Remote
40002	Lead Family Nurse Practitioner FNP Urgent Care
140681	Aesthetic Nurse Practitioner
60222	Women s Psychiatric Mental Health Nurse Practitioner Kentucky
121067	Loan Originator Assistant Unlicensed
125976	Band 5 6 Registered ITU Nurse Whipps Cross Hospital Barts Health NHS Trust
107270	Physical Security Policy Contracts Manager
122772	Customer Support Reps AI Training Belfast UK
25648	Product Marketing Manager
150521	Personal Trainer
152430	Personal Trainer
71393	Revenue Accountant
2496	Video Production Coordinator Freelancer 6 months
154969	Personal Trainer Newton MA
17887	Warehouse Manager
29151	Field Reimbursement Manager Southwest Skeletal Dysplasia
119849	Technical Delivery Manager
123117	Einwohner Deutschlands Studienteilnehmer innen Aachen Deutschland
43665	Strategic Sourcing Manager Marketing
151280	Personal Trainer
75399	Entry Operator Nights
113320	Culinary Aide Wait Staff
146041	IC3 Software Engineer Mobile
61245	Nursery Room Leader
104087	Area Marketing Manager Women s Health Southeast
95270	Purchase Order Manager
167466	SOX Manager
135658	Manager Mid Market East
53354	Care Coordinator Embedded Leesburg VA
143728	Launch Pad Technician Starship Night Shift
147747	Agente Stone Consultor a Comercial Externo Volta Redonda RJ
171779	Chief Technology Officer
144278	Propulsion Technician Raptor Chamber and Nozzle Assembly
35934	Math Teacher
136492	Solutions Engineering Lead Healthcare Life Sciences
124192	Mexico Residents Survey Participants Chihuahua Mexico
85540	Avionics Test Engineer Optical
176260	Software Engineer Web Platform
171419	Sales Leader System Integrators
97982	Mechanical Integration Test Engineer
57361	Heavy Equipment CDL Driver
27265	Psychiatrist MD
86370	Enterprise Sales Executive UK
81120	Risk Compliance Project Manager
124802	Registered Nurses AI Training Washington DC USA
38359	Product Marketing Manager Trading
157461	Founding Account Executive Southern Europe
36298	Assistant City Attorney II General Counsel
41677	CLS Operation Research Scientist First Middlemile
153058	Personal Trainer
66148	Inventory Specialist
92791	Apprentice Technician
142798	Temporary Property Handler
65407	Partnership Manager Autonomous Vehicle
64465	Rock Truck Driver
117633	Business Development Representative
17614	Manager Accounting Defense
53461	Nurse Practitioner or Physician Assistant Marion County FL
2377	NJ Certified High School Math Teacher 2026 2027
96314	Account Executive OR Account Supervisor Lifestyle
171418	Sales Executive VTEX Ads
143037	Avionics Technician Falcon Dragon
128866	Sales Development Representative
51544	Medical Assistant Willing to Train
37462	Named Account Executive Ankara Public Sector
81366	Electronics Integration and Development Engineer
153214	Personal Trainer
92855	Sales Executive
69392	Product Engineer UX UI
99881	Speech Language Therapist SLP Home Health 5K Sign on
53357	Care Coordinator Embedded Partnered Dialysis Clinics
51428	Clinic Supervisor Ophthalmology Certified Ophthalmic Assistant preferred no weekends full benefits
143851	Manufacturing Automation Engineer Chamber and Nozzle Raptor
4766	Video Collection LATAM Freelance AI Trainer Project
26998	Psychiatric Mental Health Nurse Practitioner PMHNP
67711	Parts Sales Representative
58981	Veterinary Cardiologist
22970	Private Duty Registered Nurse RN
108182	安全合规与治理工程师
130858	Staff Mechanical Design Engineer EPC
135207	Backend Engineer Golang
80843	Staff Machine Learning Scientist
175377	WPP Media Activation Executive Japan
15106	Project Manager Construction
107813	Enterprise Account Executive East New York
103338	Field Marketing Manager DC
94780	Contract Infusion Nurse
113276	Activities Assistant
54544	Certified Personal Trainer
49631	Technical Product Owner Core Services
51523	Medical Assistant Ophthalmology Training Provided Full Time Blue Ash
58254	Registered Nurse Part Time Nights
71082	Lead Preschool Teacher
38232	Accounting Manager GL Operations Intercompany
173409	Veterinary Assistant
97123	Travel Medical Assistant
34153	Clinical Quality Assurance Manager
72499	Device Quality Engineer
7192	Director of Real Estate Legal
113293	Certified Nursing Assistant CNA STNA
172256	Engineering Manager SRE AV Fleet Japan
81741	Controls Automation Technician
65737	Clinic Coordinator Physical Therapist
42835	Residential Security Sales Representative
100590	Part Time Financial Service Representative
168947	Emergency Veterinary Assistant Relief Katy TX
66292	Service Agreement Coordinator
82179	Infusion Nurse Practitioner NP
152695	Personal Trainer
33297	Strategic Solutions Consultant AI
77968	Cloud Infrastructure Engineer Vice President
174326	Grocery Associate Košice
46357	Facilities Services Manager
121579	AI Trainer Advanced Tamil Fluency Calgary
119531	Clinical Trial Manager Clinical Trial Manager
132669	Sales Account Executive Cerritos CA
154381	Personal Trainer
13309	Staff Software Engineer
96763	Gastroenterology Nurse Practitioner Physician Assistant
14536	Head of Legal
43324	Sales Development Representative Pro Miami
90533	Front Door Children s Social Work Team
150988	Personal Trainer
158709	Assistant Garment Technologist MP Activewear
85934	Account Executive Renewal Sales
25483	Oncology Account Executive
89911	SUD Nurse LPN RN
138001	Data Engineer
66117	Customer Care Representative Bilingual Spanish Preferred
155324	Administrative Coordinator I II
22396	LPN Pediatric Home Care Nurse
127178	Band 7 Urology Sonographer Worthing
50116	Shift Lead 11 Mile Gratiot
94761	Ambulatory Infusion Clinic Nurse
113686	Assistant de vie
109006	Driver Class A
22245	Licensed Practical Nurse LPN Pediatric Homecare Overnights
81079	Executive Assistant
80425	Manager of Clinical Product
7068	Contract Opportunity Shared Living Provider 1099
165793	Resident Coordinator
53726	Middleware Engineer
167027	Associate Account Executive Commercial Inside Sales Hebrew speaker
25729	Diagnostic Evaluator PsyD Contract 1099
```

## Predictions — do not read until step 3

This is what the classifier answered for each of the rows above. Reading it before the labels are
written to disk destroys the measurement: the labeller would agree with the classifier and the
numbers would be decorative.

```
248	UNKNOWN
366	OUT
527	OUT
1153	OUT
1157	OUT
1713	IN
2085	UNKNOWN
2177	IN
2377	OUT
2388	OUT
2496	OUT
2731	OUT
2743	OUT
2780	OUT
3148	UNKNOWN
3521	OUT
3591	UNKNOWN
4007	OUT
4039	OUT
4102	OUT
4223	OUT
4234	OUT
4310	OUT
4456	OUT
4472	OUT
4492	OUT
4500	OUT
4533	OUT
4564	OUT
4617	OUT
4636	OUT
4756	OUT
4766	OUT
4791	OUT
4942	UNKNOWN
4971	IN
5274	UNKNOWN
5301	UNKNOWN
5328	IN
6122	OUT
6261	UNKNOWN
6359	OUT
6392	UNKNOWN
6807	UNKNOWN
7068	UNKNOWN
7192	UNKNOWN
7455	UNKNOWN
7765	UNKNOWN
8087	UNKNOWN
8145	OUT
8161	OUT
8216	UNKNOWN
8779	OUT
8852	IN
8948	OUT
9124	UNKNOWN
9440	OUT
9635	OUT
9657	OUT
9985	OUT
10004	OUT
10025	OUT
10051	OUT
10081	OUT
10582	UNKNOWN
10591	IN
10611	OUT
10972	UNKNOWN
11145	IN
11411	UNKNOWN
11532	OUT
12358	IN
12580	OUT
12604	UNKNOWN
12752	OUT
13032	OUT
13272	OUT
13285	OUT
13309	IN
13315	OUT
13350	OUT
13355	OUT
13437	OUT
13484	OUT
13786	OUT
13815	OUT
13891	OUT
13944	OUT
14271	UNKNOWN
14469	UNKNOWN
14536	UNKNOWN
14540	UNKNOWN
14657	UNKNOWN
14711	UNKNOWN
14879	OUT
14929	OUT
15106	OUT
15171	OUT
15353	OUT
15404	OUT
15593	UNKNOWN
15607	UNKNOWN
15622	UNKNOWN
15807	UNKNOWN
16278	IN
16452	OUT
16721	UNKNOWN
16739	OUT
16762	UNKNOWN
16797	OUT
16959	IN
17614	OUT
17654	OUT
17887	OUT
18088	OUT
18111	UNKNOWN
18465	IN
18625	UNKNOWN
18698	OUT
18773	OUT
19023	IN
19506	IN
19628	OUT
20063	UNKNOWN
20292	OUT
21325	OUT
21531	OUT
21752	UNKNOWN
21864	OUT
22167	OUT
22245	OUT
22376	OUT
22396	OUT
22970	OUT
23031	OUT
23242	OUT
23255	OUT
23316	OUT
23363	OUT
23388	OUT
23873	OUT
24111	OUT
24237	UNKNOWN
24333	IN
24571	IN
24739	UNKNOWN
25184	UNKNOWN
25210	UNKNOWN
25364	UNKNOWN
25426	UNKNOWN
25483	OUT
25648	OUT
25729	UNKNOWN
25852	UNKNOWN
25879	IN
26192	OUT
26243	OUT
26402	OUT
26779	OUT
26998	OUT
27011	OUT
27127	UNKNOWN
27265	UNKNOWN
27677	UNKNOWN
27745	OUT
27980	OUT
28258	OUT
28531	OUT
28547	OUT
29151	UNKNOWN
29606	UNKNOWN
30227	UNKNOWN
30468	UNKNOWN
30562	UNKNOWN
30714	UNKNOWN
30975	IN
31302	IN
31340	UNKNOWN
32351	IN
32364	UNKNOWN
32384	OUT
32548	OUT
32631	OUT
32942	IN
33123	OUT
33297	IN
33342	UNKNOWN
33396	OUT
33900	UNKNOWN
34074	UNKNOWN
34153	OUT
34441	UNKNOWN
34584	UNKNOWN
34732	OUT
34751	OUT
34774	OUT
34831	OUT
34836	UNKNOWN
35051	OUT
35163	OUT
35934	OUT
36290	UNKNOWN
36298	OUT
36417	OUT
36561	OUT
36873	IN
37130	UNKNOWN
37462	OUT
37584	IN
38028	OUT
38103	OUT
38106	IN
38232	OUT
38359	OUT
38900	UNKNOWN
39054	OUT
39101	IN
39186	UNKNOWN
39222	OUT
39284	OUT
39659	UNKNOWN
39693	OUT
39739	UNKNOWN
40002	OUT
40063	OUT
40065	OUT
40121	UNKNOWN
40424	UNKNOWN
40563	UNKNOWN
41228	OUT
41235	OUT
41322	UNKNOWN
41329	UNKNOWN
41644	OUT
41677	UNKNOWN
42015	OUT
42064	IN
42083	IN
42162	IN
42589	OUT
42835	OUT
42843	OUT
43324	OUT
43665	OUT
43955	IN
44019	OUT
44241	OUT
44534	OUT
44806	OUT
45298	UNKNOWN
45583	UNKNOWN
45645	OUT
45772	OUT
45938	OUT
46091	OUT
46128	OUT
46193	UNKNOWN
46357	OUT
46398	OUT
46420	OUT
46704	IN
46870	UNKNOWN
46909	OUT
46978	UNKNOWN
47159	UNKNOWN
47321	IN
47414	UNKNOWN
47592	UNKNOWN
47730	OUT
47919	UNKNOWN
47948	OUT
48676	OUT
48723	OUT
48931	OUT
49242	OUT
49513	OUT
49577	UNKNOWN
49631	IN
49767	UNKNOWN
49880	UNKNOWN
50116	UNKNOWN
50248	UNKNOWN
50446	OUT
50490	IN
50998	UNKNOWN
51405	UNKNOWN
51428	OUT
51523	OUT
51544	OUT
51752	OUT
51838	UNKNOWN
52065	UNKNOWN
52407	OUT
52724	UNKNOWN
52752	UNKNOWN
53029	OUT
53354	OUT
53357	OUT
53461	OUT
53726	UNKNOWN
54118	OUT
54141	OUT
54145	OUT
54158	OUT
54321	OUT
54399	OUT
54413	OUT
54544	OUT
54616	OUT
54655	OUT
54883	UNKNOWN
55089	UNKNOWN
55300	UNKNOWN
55389	UNKNOWN
55724	UNKNOWN
56051	UNKNOWN
56610	OUT
57044	OUT
57235	OUT
57241	OUT
57316	OUT
57361	OUT
57734	OUT
57850	UNKNOWN
57981	OUT
58057	UNKNOWN
58254	OUT
58475	UNKNOWN
58554	IN
58737	IN
58889	UNKNOWN
58981	UNKNOWN
59343	UNKNOWN
59361	UNKNOWN
59887	OUT
60124	UNKNOWN
60222	OUT
60376	OUT
61245	UNKNOWN
61317	OUT
61354	OUT
61875	OUT
62094	OUT
62590	UNKNOWN
62676	UNKNOWN
62812	UNKNOWN
62817	OUT
63079	OUT
63117	OUT
63325	UNKNOWN
64171	IN
64416	OUT
64465	OUT
64515	UNKNOWN
65294	OUT
65316	OUT
65407	UNKNOWN
65513	UNKNOWN
65737	OUT
65798	IN
66077	OUT
66117	OUT
66148	UNKNOWN
66292	OUT
66438	OUT
67059	UNKNOWN
67272	OUT
67331	OUT
67372	OUT
67565	OUT
67602	UNKNOWN
67681	UNKNOWN
67711	OUT
67750	OUT
67862	IN
67882	OUT
68134	OUT
68370	UNKNOWN
69392	UNKNOWN
69464	OUT
69614	IN
69955	OUT
70092	UNKNOWN
70671	IN
70747	UNKNOWN
71040	OUT
71082	OUT
71155	OUT
71393	UNKNOWN
71395	UNKNOWN
71476	OUT
71774	OUT
72206	OUT
72301	UNKNOWN
72499	UNKNOWN
72979	UNKNOWN
73234	UNKNOWN
73414	UNKNOWN
73799	UNKNOWN
74209	UNKNOWN
74676	OUT
75226	UNKNOWN
75229	OUT
75399	UNKNOWN
75944	UNKNOWN
76190	OUT
76211	OUT
76291	UNKNOWN
76649	OUT
77173	OUT
77218	UNKNOWN
77322	IN
77353	UNKNOWN
77588	UNKNOWN
77630	IN
77717	OUT
77724	OUT
77968	IN
79331	OUT
79589	OUT
79773	UNKNOWN
79979	UNKNOWN
80425	OUT
80700	IN
80843	IN
80857	OUT
81079	OUT
81120	UNKNOWN
81254	UNKNOWN
81366	IN
81432	IN
81492	OUT
81741	UNKNOWN
82058	OUT
82089	OUT
82158	OUT
82179	OUT
82753	OUT
82760	OUT
82780	OUT
82825	OUT
82893	OUT
83567	OUT
84337	OUT
84946	OUT
85086	OUT
85212	IN
85363	OUT
85430	UNKNOWN
85540	OUT
85814	OUT
85934	OUT
85942	UNKNOWN
86015	IN
86264	OUT
86341	OUT
86370	OUT
86371	OUT
86454	OUT
87728	UNKNOWN
87882	OUT
87943	OUT
88169	OUT
88330	OUT
88340	OUT
88427	UNKNOWN
88930	OUT
88993	OUT
89121	UNKNOWN
89360	UNKNOWN
89533	IN
89677	OUT
89812	IN
89911	OUT
89925	UNKNOWN
90373	UNKNOWN
90402	UNKNOWN
90408	UNKNOWN
90533	UNKNOWN
92356	OUT
92791	UNKNOWN
92855	OUT
92860	OUT
93006	UNKNOWN
93008	OUT
93135	UNKNOWN
93181	OUT
93445	UNKNOWN
93500	UNKNOWN
93646	OUT
93908	OUT
93930	OUT
94075	UNKNOWN
94223	OUT
94246	OUT
94250	OUT
94256	OUT
94761	OUT
94780	OUT
94790	OUT
94807	OUT
94949	OUT
95036	OUT
95051	IN
95080	UNKNOWN
95244	UNKNOWN
95270	UNKNOWN
95281	OUT
95333	OUT
95393	OUT
95515	OUT
95764	UNKNOWN
95958	OUT
96314	OUT
96550	IN
96606	OUT
96763	OUT
96882	OUT
96975	OUT
97005	OUT
97017	OUT
97104	OUT
97123	OUT
97134	OUT
97435	OUT
97480	OUT
97535	UNKNOWN
97982	OUT
98130	OUT
98903	UNKNOWN
99881	UNKNOWN
100504	UNKNOWN
100590	OUT
101665	OUT
101795	UNKNOWN
102081	UNKNOWN
102202	UNKNOWN
102244	OUT
102606	IN
102729	OUT
102861	OUT
102972	IN
103068	OUT
103187	OUT
103338	OUT
103520	OUT
103939	OUT
104002	OUT
104087	OUT
104288	OUT
104403	IN
105254	IN
105337	IN
105617	UNKNOWN
105797	OUT
106200	OUT
106232	UNKNOWN
106306	OUT
106857	UNKNOWN
107270	OUT
107478	OUT
107677	OUT
107704	UNKNOWN
107813	OUT
107867	OUT
108137	OUT
108182	UNKNOWN
108260	OUT
108354	OUT
108375	OUT
108402	UNKNOWN
108842	IN
109006	OUT
109218	UNKNOWN
109576	OUT
109581	OUT
109646	OUT
109860	OUT
109960	OUT
110444	IN
110646	OUT
110739	IN
111000	IN
111013	UNKNOWN
111066	UNKNOWN
111728	UNKNOWN
112118	OUT
112173	OUT
112174	OUT
112361	OUT
112440	UNKNOWN
112674	OUT
113276	OUT
113277	OUT
113293	OUT
113320	UNKNOWN
113679	OUT
113686	OUT
113702	OUT
113707	OUT
113720	OUT
113744	OUT
113785	UNKNOWN
114131	OUT
114175	OUT
114470	OUT
115123	UNKNOWN
115271	OUT
115480	OUT
115868	OUT
115937	IN
116004	UNKNOWN
116226	OUT
116605	OUT
116875	OUT
116900	UNKNOWN
117056	IN
117060	OUT
117166	OUT
117331	OUT
117412	IN
117454	UNKNOWN
117633	OUT
117675	OUT
117683	OUT
117685	OUT
117687	OUT
117698	OUT
117722	OUT
117736	OUT
117902	OUT
117982	OUT
118043	OUT
118070	OUT
118088	OUT
118465	UNKNOWN
118554	OUT
118845	OUT
119345	UNKNOWN
119531	OUT
119722	OUT
119849	UNKNOWN
120033	OUT
120298	OUT
120479	UNKNOWN
120719	UNKNOWN
120867	UNKNOWN
120944	OUT
121067	OUT
121094	UNKNOWN
121126	UNKNOWN
121235	IN
121308	OUT
121502	OUT
121503	OUT
121508	OUT
121579	OUT
121719	OUT
121759	OUT
121910	OUT
121962	OUT
122142	UNKNOWN
122165	UNKNOWN
122772	UNKNOWN
122968	UNKNOWN
122997	UNKNOWN
123072	UNKNOWN
123110	UNKNOWN
123117	UNKNOWN
123229	OUT
123233	OUT
123360	UNKNOWN
123365	UNKNOWN
124035	UNKNOWN
124050	UNKNOWN
124169	UNKNOWN
124192	UNKNOWN
124282	UNKNOWN
124382	UNKNOWN
124674	UNKNOWN
124802	UNKNOWN
125394	UNKNOWN
125424	OUT
125704	UNKNOWN
125921	OUT
125976	OUT
126095	OUT
126129	OUT
126197	OUT
126396	UNKNOWN
127178	UNKNOWN
127189	UNKNOWN
127262	OUT
127263	OUT
127591	UNKNOWN
127743	UNKNOWN
127758	UNKNOWN
128187	OUT
128617	IN
128801	OUT
128866	OUT
129093	IN
129291	UNKNOWN
129403	IN
130109	UNKNOWN
130415	UNKNOWN
130792	OUT
130858	OUT
131147	OUT
131355	IN
131358	OUT
131833	UNKNOWN
132330	OUT
132522	UNKNOWN
132669	OUT
132828	UNKNOWN
132878	OUT
132879	OUT
132905	UNKNOWN
132912	IN
133603	IN
133771	IN
134000	OUT
134167	OUT
134314	OUT
134680	IN
135005	UNKNOWN
135207	IN
135577	UNKNOWN
135658	UNKNOWN
135775	OUT
135906	IN
135947	UNKNOWN
136207	UNKNOWN
136492	IN
136583	OUT
136587	OUT
136620	UNKNOWN
136745	IN
137181	IN
137509	UNKNOWN
137682	OUT
137725	OUT
138001	IN
138024	UNKNOWN
138297	OUT
138385	UNKNOWN
138621	UNKNOWN
138830	OUT
138983	OUT
139038	OUT
139079	OUT
139088	OUT
139089	OUT
139494	OUT
139918	OUT
140080	OUT
140142	OUT
140152	UNKNOWN
140154	UNKNOWN
140459	IN
140626	OUT
140681	OUT
141135	OUT
141320	OUT
141344	OUT
142152	UNKNOWN
142774	OUT
142798	UNKNOWN
142998	IN
143032	OUT
143037	OUT
143402	OUT
143411	OUT
143426	OUT
143535	UNKNOWN
143702	IN
143728	OUT
143755	OUT
143796	OUT
143804	IN
143851	OUT
144278	OUT
144623	IN
144877	OUT
145280	OUT
145304	UNKNOWN
145593	OUT
145711	IN
145820	IN
145864	IN
145976	UNKNOWN
146041	IN
146272	UNKNOWN
146464	OUT
146479	OUT
146589	UNKNOWN
146630	IN
146830	OUT
146935	UNKNOWN
146993	OUT
147169	IN
147523	UNKNOWN
147555	UNKNOWN
147747	UNKNOWN
147864	UNKNOWN
148084	OUT
148288	UNKNOWN
148301	UNKNOWN
150063	IN
150230	OUT
150240	OUT
150255	OUT
150322	OUT
150330	OUT
150350	OUT
150437	OUT
150521	OUT
150526	OUT
150556	OUT
150689	OUT
150889	OUT
150929	OUT
150983	OUT
150988	OUT
150996	OUT
151062	OUT
151075	OUT
151228	OUT
151280	OUT
151297	OUT
151308	OUT
151419	OUT
151584	OUT
151602	OUT
151799	OUT
151843	OUT
151847	OUT
152014	OUT
152110	OUT
152160	OUT
152195	OUT
152268	OUT
152283	OUT
152430	OUT
152473	OUT
152537	OUT
152631	OUT
152648	OUT
152695	OUT
152701	OUT
152729	OUT
152831	OUT
152838	OUT
152906	OUT
153058	OUT
153091	OUT
153121	OUT
153178	OUT
153214	OUT
153221	OUT
153225	OUT
153486	OUT
153544	OUT
153573	OUT
153653	OUT
153707	OUT
153787	OUT
153818	OUT
154088	OUT
154146	OUT
154151	OUT
154290	OUT
154296	OUT
154380	OUT
154381	OUT
154407	OUT
154476	OUT
154526	OUT
154626	OUT
154645	OUT
154683	OUT
154706	OUT
154907	OUT
154958	OUT
154960	OUT
154969	OUT
155324	OUT
155428	OUT
155450	OUT
155781	OUT
156011	OUT
156163	UNKNOWN
156813	OUT
156924	OUT
156983	UNKNOWN
157458	OUT
157461	OUT
157791	OUT
158105	OUT
158453	UNKNOWN
158481	OUT
158484	OUT
158709	OUT
159327	OUT
159713	UNKNOWN
159794	OUT
160134	UNKNOWN
160255	OUT
160358	OUT
160967	OUT
161262	OUT
161474	OUT
161967	OUT
162227	OUT
162730	OUT
162878	OUT
163245	OUT
163457	OUT
163518	UNKNOWN
164153	OUT
164285	IN
164312	UNKNOWN
164386	IN
164492	OUT
164686	UNKNOWN
164784	OUT
164865	UNKNOWN
165039	UNKNOWN
165043	UNKNOWN
165093	UNKNOWN
165567	OUT
165793	OUT
166251	OUT
166262	OUT
166388	IN
166579	OUT
167027	OUT
167288	IN
167466	UNKNOWN
167650	OUT
167830	OUT
167835	UNKNOWN
167953	OUT
168398	UNKNOWN
168421	UNKNOWN
168687	UNKNOWN
168947	OUT
168999	OUT
169533	OUT
169820	UNKNOWN
169838	UNKNOWN
169943	OUT
170603	OUT
171119	UNKNOWN
171127	UNKNOWN
171196	OUT
171204	OUT
171385	OUT
171418	OUT
171419	UNKNOWN
171779	UNKNOWN
171883	OUT
172048	UNKNOWN
172142	IN
172256	UNKNOWN
173041	UNKNOWN
173092	UNKNOWN
173338	UNKNOWN
173381	OUT
173409	OUT
173582	UNKNOWN
173620	OUT
173787	OUT
174130	UNKNOWN
174326	UNKNOWN
174525	UNKNOWN
174916	OUT
174928	OUT
175377	OUT
176050	UNKNOWN
176067	OUT
176156	OUT
176247	OUT
176260	IN
176306	UNKNOWN
176389	UNKNOWN
176598	OUT
176636	UNKNOWN
176904	IN
177053	OUT
177057	OUT
177079	UNKNOWN
177355	UNKNOWN
177424	UNKNOWN
177850	UNKNOWN
177888	UNKNOWN
178049	OUT
178236	UNKNOWN
178869	OUT
```
