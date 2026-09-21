# Iteration 1 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot
`~/oneprofile-snapshots/raw-2026-09-20.dump` — 6890 companies and 179 098 vacancies, already
cleaned — and triggered with one `POST /classifications`. Criterion revision `33113b2`.

**This iteration labelled nothing.** Iteration 1 has no sample to label, because no previous
iteration left one, so there is no miss rate and no false accept rate to report. The first two
numbers arrive with iteration 2, which labels the 1000 rows at the bottom of this file.

## The numbers

| | | |
| --- | ---: | ---: |
| Vacancies classified | 179 098 | |
| `IN` | 22 606 | 12.62% |
| `OUT` | 37 586 | 20.99% |
| **`UNKNOWN` — the gated share** | **118 906** | **66.39%** |
| &nbsp;&nbsp;of which `unruled` | 102 714 | 57.35% |
| &nbsp;&nbsp;of which `domain_ambiguity` | 12 644 | 7.06% |
| &nbsp;&nbsp;of which `scope_ambiguity` | 3 548 | 1.98% |

| Gated number | This iteration |
| --- | ---: |
| Miss rate | — (nothing to label) |
| False accept rate | — (nothing to label) |
| Unknown share | 66.39% |

Two thirds of the corpus is unknown, and 57.35 points of that are `unruled` — no rule reaches
the title at all. That is the shape ADR-0009 predicted when it flipped the default: the unknown
share starts above half the corpus, and almost all of it is the rules' backlog rather than the
world's ambiguity. It is also what makes iteration 2 worth running, because `unruled` is the one
share a list edit moves.

The exit gate is not met and could not have been: it wants two consecutive iterations, and this
is the first.

## Cost

The rules are whole-word, case-insensitive matching over the cleaned title, and nothing else — no
description bodies, no network, no backtracking regexes. A full pass over all 179 098 cleaned
titles takes **0.30s** in memory, against the criterion's ten-second limit. The endpoint's 42s
wall clock is the database: 179 batches read and written, one transaction each.

## What changed

Iteration 1 has no disagreements to grow from, so the whole of the change is the port the loop
was told to make.

**`docs/engineering-role-seed-lists.md` is gone, and its four lists are now code.**
`src/main/java/oneprofile/backend/classification/TitleClassification.java` holds them: thirteen
function heads with their two attributes, eight never-engineering heads, thirty-eight software
qualifiers, forty-five off-domain markers, and twenty-six rulings. The criterion's six steps are
that class's `classify` method, in their order and with the step numbers in the comments, so the
document and the code can be read side by side.

Three things the seed lists left open had to be decided to make them executable, and each is
decided the way the criterion's own prose decides it:

- **An undecided ruling carries `scope_ambiguity`.** The seed table gives verdicts and no
  reasons. The criterion says a phrase is unknown with `scope_ambiguity` when its variants split
  across Q2–Q4, which is exactly why every undecided ruling in the table is undecided, so all
  nine of them carry it.
- **Where a title names two rulings, the earliest wins**, and a longer ruling beats a shorter
  one it contains. Earliest is the same rule the criterion already states for heads — "where a
  title names more than one head, the first is the head" — and longest-first keeps a future
  three-word ruling from being read as the two-word one inside it. No two of today's rulings
  nest, so only the earliest half of this is exercised by the corpus.
- **The head is the first head the title names, not its last word.** ADR-0009 proposed reaching
  this by adding a cleaning rule that strips trailing location, date and level junk, and storing
  the head on `normalized_vacancy`. Scanning left to right for the first known head makes that
  unnecessary: `Backend Engineer II Remote US` finds `engineer` without anything being stripped.
  No cleaning rule was added and no head column was stored.

**The classification state is held on `normalized_vacancy`**, in two nullable columns added by
`V5__normalized_vacancy_classification.sql`, alongside the cleaned title cleaning already writes
there. Nullable because cleaning writes the row first: a vacancy swept since the last
classification run has a title and no state, which is not the same as being unknown. The pass
itself is `CorpusClassification`, built the same way `CorpusCleaning` is — batched, one
transaction each, re-runnable — and `POST /classifications` runs it.

## What the next session should look at

`unruled` at 57.35% is where the work is, and the head list is where the coverage comes from: a
head generalises across every title that names it. The seed lists flagged `specialist` as the
largest head deliberately left off, at 4.1% of the corpus, and the boundary calibration labelled
`it specialist` IN and `sales specialist` OUT — which is a modifier question under one head, not
a reason to leave the head off the list. That is one candidate among many the sample will turn
up.

One loose end this iteration created and did not fix: `docs/engineering-role-criterion.md` still
points at `docs/engineering-role-seed-lists.md`, in the sentence that anticipated the file being
absorbed. The criterion is the developer's document and is changed only in a grilling session,
so the dangling reference was left alone rather than quietly edited.

## Held out of the draw

The 100 distinct titles in `src/test/resources/calibration/` were excluded from the sample below.
Issue #10 keeps those two fixtures as history read by nothing, but
`engineering-role-2026-09-20.tsv` states on its own face that a round never draws a title listed
in it, and honouring that costs 100 titles out of 104 891.

## The sample for iteration 2

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
148210	Account Executive Product Sales Currency Management
31772	Técnico de Segurança do Trabalho Offshore Santos SP
41487	Part Time Customer Experience Specialist at 5 Row Apartments
135990	Forward Deployed Engineer
113519	Aide à domicile
134647	Software Engineer Roku UI
51511	Medical Assistant No weekends Full benefits
22201	Licensed Practical Nurse LPN For High Acuity
57600	Industrial Tooling CDL Delivery Driver
37639	IHC Certified Medical Assistant Union NJ
107740	Tupande Procurement Coordinator BU Support
38583	Demand Generation Manager Industrials Manufacturing
87146	Inside Sales Representative
160326	Procedural Nurse Darien
127915	MENTAL HEALTH NURSE RMN EUSTON
108383	Manager Site Reliability Engineering Auth0
150196	Internal Auditor Insurance Fronting Carrier
175402	Account Director SEO CDI
154984	Personal Trainer Omaha NE
28498	Accounting Manager
57683	Parts Assistant
94888	Business Systems Engineer
173892	Legal Support Assistant
113972	Auxiliaire de vie
21478	Certified Nursing Assistant CNA
14357	Revenue Accountant
53342	3000 Sign on Bonus Nurse Practitioner or Physician Assistant Baltimore County
25764	Psychiatric Mental Health Nurse Practitioner VA License PMHNP Contract 1099
55683	Kids Club Manager
4494	Mathematics Specialist Fluent in Portuguese Brazil Freelance AI Trainer Project
159165	Video Journalist News
24416	Certified Nursing Assistant CNA Memory Care Full Time 10 45pm 7 15am
25693	Child Adolescent Therapist Resident in Counseling LPC R
79307	Field Service Technician Level II
175594	Business Director Investment
175648	Data Management and BI Executive GOC
13750	Medical Assistant Ashtabula Office
97605	Customer Service Representative
102773	Marketing Data Science Manager
149010	Product Manager Seller Experience
117665	Assistant Store Manager
44907	Manager Field Engineering Financial Services Industry
9397	Project Manager Construction
147740	Agente Stone Consultor a Comercial Externo Ubatuba SP
129986	Lead Product Marketing Manager Solo Agents Small Teams
162458	Tri County Veterinary Clinic Kennel Attendant
68670	Commercial Account Executive Outbound Sales
92354	Carpenter
41250	Regional Marketing Manager Atlanta GA
97102	Travel Certified Nursing Assistant
97826	Compliance Risk Manager Prudential
126351	Band 6 Biomedical Science Blood Transfusion and Haematology Portsmouth
27881	Credentialed Veterinary Technician Boston Multiple Locations
64897	Technical SEO
24127	Strategic Account Manager
45143	Solutions Architect Digital Native Business Named Accounts
150748	Personal Trainer
85675	Electro Mechanical Manufacturing Engineer
92568	Project Coordinator Electrical
169124	Nursing Manager Portland Maine
41232	Maintenance Technician Cortland Belmar
53220	Manager Information Technology
53301	Mechanical Engineer Building Systems
157868	EHS Coordinator
137140	Analyst Material Chemical Conformity
89754	Center Based Behavior Technician
44055	Software Team Lead
21622	CNA Certified Nursing Assistant
16377	Tax Manager Estate Trust
29460	Executive
19745	Account Executive Vehicle Intelligence
114464	Business to Business Sales Representative
149104	Health Sciences Assignment Grader Contract
159894	Leasing Consultant
93894	PRN Registered Nurse Case Manager
72647	Culture Assistant 2026 2027
162705	Business Development Executive London
154976	Personal Trainer North Sea NY
45637	Commercial Account Executive
10974	Production Optical Assembly Technician
125894	Anaesthetics Theatres Specialty Registrar Birmingham
54429	Assistant Kids Club Manager
125863	Adult Neuro Rehab Speech and Language Therapist
151259	Personal Trainer
154188	Personal Trainer
15141	Project Manager Construction
6865	Mechanical Technician
39890	Mid Market Account Executive France
29019	Product Manager Travel
148671	Short form Video Social Community Comms
154904	Personal Trainer Long Island NY
165180	Registered Nurse PRN
167066	Director Product Marketing Veeam Intelligent ResOps
108871	Product Manager DEX Institution Trading
117970	Customer Service Representative
137518	Manager Engineering Sciences Electrical Engineering
116549	Medical Assistant II Dermatology Sign On Brookhaven Snellville GA
56442	Service Associate Night
93149	Store Manager Euroma 2
4018	Attorney Specialist Freelance AI Trainer Project
51192	Sales Executive Partnerships FX Sales
28931	Enterprise Client Sales Executive
4778	Visual Digital Arts Specialist Freelance AI Trainer Project
68812	DACH Sales Manager
94238	Staff Nurse Emergency Department
25930	Assistant General Manager Manchester
40813	Account Solution Architect
148395	Full Stack Software Engineer Brazil
132378	Event Operations Coordinator
161135	Strategic Account Executive PacNW
170764	Speech Language Pathologist SLP Home Health PRN
82661	Sales Assistant Categoria Protetta Firenze C.C I Gigli
61144	Bank Early Years Educator
133827	Aircraft Structural Technician all levels
159023	Associate Manager Supplier Operations NYT Wirecutter Temporary
65884	Business Development Representative Commercial Doors
147244	Account Executive Farming Operação Híbrido SP
95752	Retail Security Dispatch Officer
151372	Personal Trainer
57182	District Field Technician Mechanic Pump Power HVAC
91072	Social Worker CHC
66085	IT Systems Engineer
153759	Personal Trainer
68180	Product Manager Enterprise Intelligence
177705	Commercial Manager
109060	Maintenance Technician 2nd Shift
96655	Software Engineer
34628	Spanish Speaking Behavior Technician
167434	Scaled Commercial Account Executive Install base
135357	Assistant Retail Store Manager
57854	Shop Technician Mechanic Pump Power HVAC
145764	Software Engineer Platform Dubai United Arab Emirates
136847	Per Diem Clinical Research Nurse Home Visits
92849	Premium Divison Assistant Accountant
154230	Personal Trainer
156894	Contact Center Solutions Architect
14009	Head of Revenue Enablement
131364	Propulsion Integration Test Engineer II First Shift
18392	Proposal Marketing Associate Remote West Coast Land Development Public Works
145576	Dental Assistant
38231	Accounting Manager GL Operations Intercompany
74518	Executive Director Interim Opportunities Submit Your Resume
61057	Cybersecurity Systems Analyst Intermediate
163383	Product Manager Creative Strategy Applications
23227	Registered Nurse RN Home Care
117074	Full Stack Engineer Product
23219	Registered Nurse RN Home Care
23057	Registered Nurse 1 1 Pediatric Home Care
125257	Assistant Property Manager Leasing Experience Manager III Mansion Grove
129098	Executive Manager Commercial Finance 12m FTC
137334	Specialist L2 Body Integration
135682	Product Manager Revenue Tools
66032	Software Engineer Intern
142966	Application Software Engineer
116127	Remote Nurse Practitioner Contractor Multi State
1565	AI ML Strategist
11289	Electrical Test Technician
3483	Marketing Coordinator Pacific Northwest
89684	Sales Representative Finnish speaking
129722	Grupo QuintoAndar Staff Software Engineer Women Applicants Only
176649	Manager Data Center Facilities Memphis
54242	Assistant General Manager
26729	Psychiatric Mental Health Nurse Practitioner PMHNP
1044	Developer Relations Manager
67299	Order Specialist
153566	Personal Trainer
153092	Personal Trainer
88449	Health Safety Coordinator
45459	Staff Product Manager SAP
9813	Executive Support Analyst
65079	Product Manager Digital Experience
18117	Supply Chain Coordinator
3069	Industrial Maintenance Technician
4409	Language Alignment Resource Partner Polish Freelance AI Trainer Project
57725	Rental Coordinator
19698	Privacy AI Counsel
13420	Events and Ministry Coordinator Onsite
82718	Sales Assistant Part Time Civitavecchia C.C La Scaglia
65318	Manufacturing Engineer New Grad Summer 2027
98765	HSE Coordinator Micon Group Inc
109677	Mechanical Engineer Healthcare Buildings
112165	E Scooter Delivery Driver
172331	Roboticist Robot Foundation Model
154282	Personal Trainer
126868	Band 7 Emergency Nurse Practitioner ENP Chippenham
12505	MSU Medical Surgical Unit OvernightVeterinary Assistant
2479	Specialist Marketing Key Account Manager Media
3171	Field Marketing Manager
159602	Head of Laboratory gn
30480	Grocery Merchandiser Rockford IL
104499	Data Center IT Technician
106086	Research Communications Analyst Critical Infrastructure
23095	Registered Nurse Home Health
60519	Retail Lead Orlando City Soccer Club Pride Team Store
55529	Kids Club Associate
37656	Medical Assistant Somerset Middlesex NJ
82423	FIX Onboarding and Connectivity Engineer
129887	Software Engineer
102558	Civil Engineer Project Manager
123201	Finance Professionals AI Training Liverpool UK
6940	Geospatial Analyst Middle Job#823
51000	Paid Social Media Coordinator
25967	Cafe Manager Beverly Hills
13956	Account Executive
14874	Assistant Superintendent New Grad 2027
74581	Store Manager Boston
54459	Assistant Stretch Manager
144799	Human Resources Business Partner
101933	Psychiatric Mental Health Nurse Practitioner PMHNP
153193	Personal Trainer
112319	Sales Development Representative Vietnam
40704	Data Scientist Technical Targeter
84751	Auxiliaire de vie
38920	Registered Nurse
19551	Content Marketing Manager
2558	Board Certified Behavior Analyst
137815	AI Platform Engineer Lead Architect
106945	SAP Security Specialist
68166	Product Marketing Manager Glean Intelligence
97039	Primary Care Nurse Practitioner Physician Assistant
139629	Software Engineer Frontend
117600	Sales Executive
22890	Physical Therapy Assistant
61542	Head of Business Strategy
102098	Window Installation Subcontractor
153356	Personal Trainer
67471	Account Manager
25162	Civil Land Site Project Manager
110733	Solutions Engineer Solutions Consultant Internship
116456	Analog Mixed Signal IC Designer
63657	Production Training Coordinator
4616	Radiochemistry Nuclear Chemistry Specialist Freelance AI Trainer Project
25512	Prenatal Account Executive
155137	Personal Trainer Tracy CA
62799	Vice President Engineering and AI Innovations
45770	HRIS Manager
94600	Seasonal Ambassador Santa Anita
177045	Health Product Manager Claims
157842	Structural Analysis Engineer
154233	Personal Trainer
24010	Executive Medical Director Clinical Development
374	Outside Sales Representative Roofing
99761	Home Health Nursing Supervisor Internal Only
154640	Personal Trainer Beaver PA
154195	Personal Trainer
133394	Staff Product Manager Integrations Advisor Platform
124074	Linguist Translator Graduates AI Training Sheffield UK
76074	Retail Artist Full Time 5 Days 40 Hours Per Week Arnotts Dublin
50348	Product Manager Delivery
161201	Bilingual Strategic Cuisines Account Executive Mandarin Toronto
29020	Product Manager Travel
154366	Personal Trainer
40800	Sales Development Representative UK
3443	Guest Services Staff Usher Ticket Taker AEG Presents Great Lakes Royal Oak Music Theater
51791	Registered Nurse Pre Op Post Op PACU PRN
146469	Sales assistant Sport Zone Ovar Dolce Vita
53617	Assistant Controller
134284	Spacecraft Structural Analysis Engineer I II
100210	Lifecycle Marketing Manager
42710	Staff Visual Designer L6 1 Coupang Eats
84138	Sanitation Weekends 2nd Shift
62769	Software Engineer
112490	YOW Expression of Interest Software Engineer
150113	Manager Remote Accounting Services
2583	Registered Behavior Technician
73044	Manager Long Term Planning
152248	Personal Trainer
26971	Psychiatric Mental Health Nurse Practitioner PMHNP
100182	Anesthesiologist Herrin Comfortable with Peds
2946	Associate Sales Development Representative
63587	Trainer Ausbilder für Lokführer Triebfahrzeugführer
14065	Advisory Solutions Consultant Broad Markets
92731	1685 Director Indirect Procurement IT
112062	Product Marketing Manager Hybrid
136330	AI Product Manager Coding Multimodal
139047	Dispensing Pharmacy Technician
110494	Advanced Practice Provider Tennessee Oncology
150370	Personal Trainer
29938	Growth Product Manager User Activation
128372	Specialty Dr Required for Community CAMHS role in the North East
33135	Client Value Partner CVP Utilities Energy Industry
28337	Werkstudent in Influencer Marketing
76042	Staff Engineer I Back end
63042	Account Executive Mid Market
140735	Clinic Manager Brea
104259	Connected Living Activities Coordinator
145602	Dental Hygienist
16638	Engineer Mechanical Engineering Chemical Materials Based
133370	Underwriting Associate
113015	Associate Strategic Finance FP&A
167518	Product Manager Hardware
70461	Payment Product Manager
19068	Technical Support Engineer
166207	Medical Assistant OB Gyn
5186	Assistente Fiscal II
12704	Data Center Energy Lead Australia
153822	Personal Trainer
161150	Customer Success Manager
128656	Product Manager Commercial Systems
129906	Channel Partner Marketing Manager
108010	Sales Director
118857	Product Support Lead eCommerce
57704	Purchasing Coordinator
96686	Attending Physician
97119	Travel Medical Assistant
142843	Medical Assistant Columbus GA
78947	Publisher Development Manager
77456	Full Stack Engineer
177145	Account Executive Defense
57682	Parts Assistant
24405	Certified Nursing Assistant CNA Full Time 2 45pm 11 15pm
69598	Lab Technician
39594	Unit Coordinator PRN Pool
70192	Group Creative Director Health
145665	Surgical Assistant
131564	Valve Manufacturing Engineer 2nd Shift
39492	Licensed Practical Nurse LPN Pool PRN
129127	VP Legal
152934	Personal Trainer
17793	Attendant Adult Care Partner Lindale TX
132519	Social Media Coordinator at Retail Insights
97061	Psychiatric Nurse Practitioner
153295	Personal Trainer
26065	Growth Lifecycle Marketing Manager
87	Junk Removal and Sales In Training
47558	Commercial Entry Door Installers Hollow Metal Wood Frames Hardware
168926	Emergency Veterinary Assistant Relief Clifton NJ
77485	Manager Client Relationship Management
78219	Payroll Executive
23716	Updated HH HOS Nurse Job Post Questions Q12026
154565	Personal Trainer
106170	Product Manager Omnichannel CX
37705	Assistant Store Manager TEC
148082	Product Owner Office Cloud Storage
125680	PUBG STUDIOS Lead Engine Engineer 10년 이상
48542	Business Operations Analyst Infrastructure Planning and Operational Excellence
82291	IXL Associate Product Manager
103629	Account Executive
64842	Technical Sales Representative Spanish Speaking
71060	Lead Infant Teacher
18022	Head of Product Security Engineer
161717	Cashier Deli Bakery Coordinator
130273	Materials Coordinator
103104	Plumbing Superintendent Traveling
145094	Supplier Development Engineer Mechanical Metrology Starlink
70571	DevOps Engineer
174716	Survey Crew Chief
72009	Staff Product Manager Continuous Deployment
46733	Account Executive Agency Partnerships
20563	BI Integration Innovation Lead FTC
106501	Data Engineer
150692	Personal Trainer
96983	Primary Care Nurse Practitioner
121692	AI Trainer Fluent Azerbaijani Speakers Nuremberg Germany
20588	Initiativbewerbung
22187	Licensed Practical Nurse LPN 1 1
153550	Personal Trainer
152548	Personal Trainer
108351	Enterprise Account Executive
132547	Associate Director Supply Chain Planning
11474	Manufacturing Engineer Mechanical
95671	QAQC Lead
72213	AI Game Designer
126074	Band 5 Intensive Care Unit Nurse ITU Surrey
123237	Fluent Danish Speakers AI Trainer Freelance Remote Odense Denmark
59877	Apartment Maintenance Manager
132022	Regional Field Marketing Manager
88309	Civil Project Manager Power Generation Transmission
89621	Technician III Maintenance
14649	Sales Executive
57384	Heavy Equipment CDL Driver
66408	Automation and Controls Engineer Manufacturing
161124	Enterprise Account Executive Bay Area
176412	Security Infrastructure Engineer
132682	Sales Account Executive Minneapolis MN
37929	Associate Project Manager
38561	Business Development Representative
16907	Specialist II Technical Training Night Shift
99557	Business Development Manager Architecture Engineering Construction
156931	Assistant Store Manager
117782	Assistant Store Manager
105792	Software Engineer Manager Database Observability
159701	Field Sales Representative gn LEH Deutschlandweit
92150	Software Engineer Android
111502	Board Certified Behavior Analyst BCBA
155121	Personal Trainer Swedesboro NJ
4066	Chinese Language Specialist Freelance AI Trainer Project
132686	Sales Account Executive Orange County CA
148399	Growth Marketing Manager Greater China
43884	Middleware Solutions Engineer I
138327	Leadership Program MBA Graduate
175081	Quantitative Developer
40097	Medical Assistant MA Urgent Care
174020	Cyber Incident Response Attorney
40852	Director Compensation
83440	Vendeur CDI temps plein
19231	Werkstudent in Finance all genders
46436	Nurse Practitioner Home Virtual Care
2752	Strategic Account Executive
166973	Manager Quality Assurance
90375	Advanced Practitioner Long Term Team
126202	Band 5 Registered General Nurse RGN Liverpool Merseyside
151672	Personal Trainer
175762	GOC Paid Social Media Specialist
14872	Assistant Superintendent Data Center Construction
153260	Personal Trainer
155078	Personal Trainer Scurry TX
172100	Vehicle Dynamics Simulation Engineer
163013	Embedded Systems Security Engineer
23213	Registered Nurse RN Homecare
151775	Personal Trainer
135590	Data Engineer
11152	Safety Equipment Engineer Rocket Motor Systems
83105	Vendeur CDD Temps partiel
82709	Sales Assistant Part Time Categorie Protette Modena C.C Grandemilia
590	Procurement Operations Manager Factor Europe all genders
99992	Vehicle Logistics Specialist
89982	Backend Engineer
136786	Strategist
89361	Account Care Representative
135192	Data Center Site Manager Pittsburgh
63250	Enterprise Account Executive
4467	Manufacturing Specialist Fluent in French Freelance AI Trainer Project
132259	Customer Service Representative
88019	Entry Level Civil Engineer
121860	AI Trainer Graphical Abstract Physics Remote Sydney
85490	Software Engineer Applied AI Toronto
132917	Head of Customer Success Support
169069	Medical Director Orange Village Ohio
151674	Personal Trainer
118310	Shift Supervisor
72705	Psychiatric Mental Health Nurse Practitioner Oklahoma
103663	Account Executive
99124	Nurse Practitioner California
78093	Relationship Manager Business Development RIA Enterprise Vice President
155410	Mechanical Engineer I
154896	Personal Trainer Leonard TX
149230	Theater Teacher
147909	Assistant Store Manager
157173	Multimedia Consumer Reporter
26711	Psychiatric Mental Health Nurse Practitioner PMHNP
49005	AM Equipment Engineer
79511	Graphic Designer Data Visualization Remote Contract
61500	Growth Manager Hindi Speaker
100567	Financial Service Representative
58959	Operations Coordinator
77022	Event Executive
150963	Personal Trainer
172498	Member Transportation Specialist North Seattle
150344	Personal Trainer
121582	AI Trainer Advanced Tamil Fluency Freelance Basel
52631	Customer Success Manager Tier 2 East
162258	Inside Sales Representative
160772	Assistant Store Manager
161735	Deli Cook
166680	Space Avionics Systems Engineer
25528	Prenatal Regional Sales Manager Great Plains
1342	5th Grade Teacher
79452	Inside Sales Representative Bilingual French Spanish
134802	Account Executive
4303	Hungarian Voice Acting Specialist Freelance AI Trainer Project
80067	Staff Engineer 1 Salesforce AI
39555	Registered Nurse RN Back End Days
100669	Enterprise Account Executive
163430	Low Latency Quantitative Researcher
83933	Technical Product Marketing Manager .NET Developer Tools
24039	Engineering Coordinator
144538	Sourcing Manager Capital Equipment Construction Starlink
156399	Remote Sales Representative FL
23212	Registered Nurse RN Homecare
26958	Psychiatric Mental Health Nurse Practitioner PMHNP
173052	Associate Veterinarian Hilliard Veterinary Hospital
81004	Cafe Shift Assistant
61790	Market Strategy and Partnerships Manager
99684	Delivery Driver
15573	GIS Project Manager
104289	Registered Nurse RN MDS Coordinator
149156	Entry Level to Experienced Teacher
165539	Manufacturing Engineer
85751	Supplier Development Engineer Mechanical
166265	Registered Nurse Radiation Oncology
79472	Client Success Representative Remote Contract
19603	Facilities Manager
18328	Electrical Project Engineer Oil and Gas Pipelines and Facilities
111451	ABA Therapist RBT Forest Hills NY
151067	Personal Trainer
164022	Counsel
167707	Account Executive Territory Mid Market
109471	Entry Level Civil Engineer Airports
176552	Civil Engineer SpaceXAI Battery Storage Memphis
161741	Delivery Driver Restaurant Catering
40093	Medical Assistant MA Urgent Care
75515	Medical Assistant
178560	Presales Solutions Architect
9292	Dental Assistant Instructor
178357	Account Executive Enterprise
57678	Parts Assistant
158498	Join Our Talent Pipeline LA
130449	Healthcare Sales Executive
24917	Customer Service Representative
164631	Criminal Defense Legal Assistant
36265	Sales Development Representative
51749	Patient Coordinator
163637	Sales Development Representative 2
109430	Design Technician Site Design
121164	Applied AI Engineer
47143	Resource Manager
123123	Einwohner Deutschlands Studienteilnehmer innen Bremen Deutschland
96779	LPN Care Coordinator BHI
177606	Account Executive Enterprise Brands Retail Corporate
119214	Project Manager Digital Marketing
123670	Italian Fluent Speakers AI Training Switzerland
156494	Sales Marketing Intern
48719	Advisory Consulting Manager Risk Audit
134582	Product Manager Recommendations
120993	Sales Representative
89404	MLOps Engineer
4741	Tajik Language Specialist Freelance AI Trainer Project
151072	Personal Trainer
43822	Delivery Driver
121930	AI Trainer Neuroscience Fully Remote New York
12452	Winter 2027 Manufacturing Engineer Co op
170629	Physical Therapy Assistant PTA Home Health PRN CORE
117493	Fullstack Software Engineer React Node.js
61329	Account Development Representative French Speaker
143097	Business Operations Manager Starlink Aviation
60966	Manager Merchandise Planning
80375	Software Engineer Instawork Robotics
34997	Part Time Microbiology Lab Tech
86304	Safety Coordinator
918	Assistant Teacher
156719	Maintenance Engineer Automation Instandhaltung
72567	Territory Account Manager Syracuse NY Utica NY
151921	Personal Trainer
23243	Registered Nurse RN Home Health
74186	UAS Operator
65338	Additive Manufacturing Development Engineer
178054	Soccer Coach Specialty Program Teacher
170687	Registered Nurse RN Home Health PRN
128263	Specialist Chemo Nurse Bristol Somerset Permanent Opportunity
63219	Assistant Manager Air Operations Gurugram
17835	Special Attendant Adult Travel Care Partner Community Care Mesquite Tx
157378	People Partner
151595	Personal Trainer
24593	Backend .NET Software Engineer
114278	Accounting Manager
164828	Email Channel Manager at United Media
92345	Assistant Project Manager Electrical Construction
39671	Technical Assistant Surety
41149	Behavioral Interventionist
86752	Weekends Family Nurse Practitioner FNP FL NJ NY MA VA NLC Remote
84173	Juicer Manchester Airport
79096	Business Development Representative NAM
161512	Développeur euse en IA systèmes agentiques AI Agentic Systems Developer
102707	Technical Program Manager Release Integration
173400	Veterinary Assistant
104622	Network Security Engineer
51264	Federal Contracts Manager
24478	Physical Therapist Assistant As Needed 8am 4pm
152679	Personal Trainer
170710	RN Registered Nurse Home Health
1705	Data Platform Engineer Palantir
134424	Physical Therapist
98098	Mechanical Engineer Cryogenics
97588	Account Executive UHV Payors
32405	Salesforce Developer
40891	Manager Energy Market Development
149654	Inbound Sales Representative Nordics
130548	Software Engineer Ads
154694	Personal Trainer Cleburne TX
1527	Virtual General Education Teacher
109979	Site Reliability Engineer
100424	Associate Cybersecurity Analyst Early Career
170168	Duty Manager VA
90345	Adult Social Worker Specialist Placement Team
164144	Transportation Logistics Coordinator New Grads Welcome
48999	Recruiting Coordinator
15260	Traveling Project Safety Manager Construction
89643	Field Service Technician Atlanta GA
81794	Engine Cycle Engineer
151762	Personal Trainer
23705	Transitional Care Adult Pediatric Nurse RN
13156	Strategic Account Executive Retail Commercial Banking
145887	Software Engineer Platform San Jose CA USA
130649	Leasing Specialist Part Time
27019	Psychiatric Mental Health Nurse Practitioner PMHNP
48838	Strategic Account Executive
48086	Site Reliability Engineer
102556	Civil Engineer Project Manager
90308	Director Head of Social Influencer
88829	EMEA Marketing Manager
100675	Enterprise Account Executive
167805	Global Solutions Engineer
147580	Vaga Temporária Auxiliar de Atendimento Logístico CNH B Carro Gramado RS
124574	Polish Fluent Speakers AI Training Norway
23589	Living Program Manager Nurse Therapist
4644	Science Specialist Fluent in Portuguese Portugal Freelance AI Trainer Project
96275	Systems Engineer II
76957	Conference Coordinator Volunteer
142833	Product Manager Mobile
102885	Channel Manager Line Producer
125508	Remote Clinical Psychologist Florida
53729	Product Manager AI Led Mainframe Managed Services
132290	Client Success Manager at Retail Insights
53991	Assistant Fitness Manager
34316	Registered Behavior Technician RBT
99183	Services Coordinator I 636 El Camino A Willow Greenridge
89618	Supervisor I Production
130000	Data Scientist CRM Personalization
57102	Assistant General Manager
168825	Emergency Veterinary Assistant Costa Mesa CA
24188	Construction Manager
132595	Logistics Coordinator
96712	Certified Nursing Assistant
112806	Infrastructure Systems Engineer
88689	Account Executive DACH
151167	Personal Trainer
112235	Bilingual English and Spanish Member Loyalty Representative R14248
109533	Licensed Civil Engineer Site Design Municipal
169518	Veterinary Assistant
7467	Operations Visual Manager Chicago IL
54015	Assistant Fitness Manager
74252	Business Development Representative
110667	Account Executive Enterprise Denver CO Remote
88809	Enterprise Acquisition Account Executive
99256	Director of Operations
90420	Assistant Team Manager Families First Non Case Holding
60854	Crop Insurance Trainer
87752	Automotive Technician Express
53993	Assistant Fitness Manager
127148	Band 7 Sonographer Bradford
131409	EHS Program Manager
24215	Maintenance Technician Round Rock TX
175633	CX Designer
156388	Enterprise Account Executive Singapore
162508	Sales Executive KC
135244	Landscape Designer
146644	Retail General Manager Fashion Valley
142163	Support Coordinator
24951	Business Development Representative
29878	Lead Blockchain Development Engineer Java
134522	Device Sales Marketing Manager
26827	Psychiatric Mental Health Nurse Practitioner PMHNP
169434	Surgical Veterinary Assistant
79329	Manufacturing Engineer
59417	Childcare Lead Teacher
47323	Product Designer Design Systems
161469	Territory Account Executive Retail Scranton
20840	Computer Systems Engineer I Computer Network Architect
139624	Revenue Accounting Manager
117750	Assistant Store Manager
139483	Manufacturing Engineer II
130691	Group AI Product Manager Growth
111912	Software QA Specialist
18594	Strategic Relationship Executive Account Manager
122533	Canada Residents Survey Participants Saint Zotique Canada
126000	Band 5 Accident Emergency A&E Nurse Hereford
66288	HVAC R Service Technician Mid Shift 15 SHIFT DIFFERENTIAL NO ON CALL
44103	Assistant Store Manager
160296	Staff Product Manager Generalist
146687	Onboarding Associate
23072	Registered Nurse Flexible Schedule
119746	Event Coordinator
78991	HR Operations Specialist
167508	Sales Representative
16192	Enterprise Solutions Engineer Service Management
153721	Personal Trainer
75351	Associate Regional Sales Manager Southeast
176905	Fullstack Architect AI Agentic Systems
16633	Engineer Data BIA AI
60300	Marketing Director LATAM
19247	Facilities Technician
37447	Majors Account Executive San Francisco Bay Area
173395	Veterinary Assistant
89423	Software Engineer Backend
148184	Account Executive Enterprise Platforms Tier 2 Grower
150503	Personal Trainer
57680	Parts Assistant
79779	Product Manager Remote Contract
123847	Javascript Developers AI Training Philadelphia USA
138782	Engineering Technology Intern Opportunities
133843	Business Development Director Optical Systems TS SCI Clearance
86139	Staff Product Manager Customer Insight Strategy
45735	Enterprise Sales Executive
74700	Assistant Quality Superintendent
134450	Ad Operations Associate
123016	Doctors AI Training Columbus US
36274	Staff Software Engineer
99807	Hospice Certified Nursing Assistant CNA
131245	Asset Maintenance Technician II First Shift
108322	Business Development Representative East
100533	Financial Service Representative
32470	Funds Paralegal
79026	Executive Assistant
112654	Clinical Project Manager
87969	Design Director Packaging
110174	Nurse Practitioner or Physician Assistant
80192	Staff Staff Wi Fi Firmware Engineer
142229	Test Staff Engineer Network+ Automation Exp is a must
43826	Delivery Driver Scooter Car
124389	Pathologists Freelance Remote Boston US
117858	Assistant Store Manager
9368	Construction Project Manager New Grad 2027
174460	Store Manager Wolt Market Qormi
97500	Future Opportunities New York
56858	Future Opportunities Software Engineering
125701	Account Executive Commerce Media
85020	Contracted In Home Occupational Therapist
10712	Manufacturing Engineer
130414	Clinical Supply Chain Manager
76903	Maintenance Technician II
169173	Veterinary Nursing Manager Annapolis MD
63803	Construction Manager South West Region
118657	Head of Compliance Hong Kong
152450	Personal Trainer
156205	Product Manager Virtual Physical Appliances
162107	Software Engineer Development Tools
93773	Field Service Technician
8049	Seasonal Sales Associate Part Time Shops at Crystals
151387	Personal Trainer
152663	Personal Trainer
107586	Pharmacy Manager Bilingual Miami Florida
149993	Staff Site Reliability Engineer Cloud Efficiency
107341	Physical Security Engineer
61993	Product Manager Chicago IL
26818	Psychiatric Mental Health Nurse Practitioner PMHNP
87061	Customer Success Manager SMB Hybrid
96852	Neurology Nurse Practitioner Physician Assistant
151711	Personal Trainer
21882	Home Care Nurse LPN
2782	Security and Infrastructure Engineer
148912	Staff Machine Learning Engineer
6246	Construction Inspector
69290	Recruiting Manager Bilingual Mandarin Required
142291	Registered Nurse
174722	Water Resources Engineer Water Business
50966	Platform Engineer AI Focused
3203	Manager Global Mobility
154231	Personal Trainer
66487	Quality Engineer Manufacturing
134328	Test and Launch Engineer II Launch Pad
70936	Account Executive Southern CA
104770	U.S Securities and Transactional Counsel
15772	AI Engineer
36392	CityVet Veterinarian Externship Houston TX
121934	AI Trainer Neuroscience Fully Remote St Louis
4312	Indonesian Language Specialist Freelance AI Trainer Project
54225	Assistant General Manager
152301	Personal Trainer
13334	Events and Ministry Coordinator Offsite Part Time
42026	PR Manager
23531	RN Private Duty Wabash
30777	Channel Partner Sales Executive Japan
139082	Pharmacy Technician Refill Calls
161567	Software Engineer Together Cloud Infrastructure
97488	Floor Lead Retail Part time
104699	ML Engineer AI Research Portability
103638	Account Executive
44780	Director Web Marketing
13488	Construction Engineer
167740	Dec 2026 Grads Sales Development Representative AAE Salt Lake City
158145	Account Executive Mid Market East Coast
134351	Join Our Talent Community Brazil
100535	Financial Service Representative
58680	Medical Support Associate Manila Registered Nurse
152521	Personal Trainer
110440	Project Engineer BESS
146224	Field Sales Representative
24556	Gaming Coordinator Greece
102187	Facilities Coordinator
32836	Sales Development Representative
54832	Fitness Counselor
12661	BDR Enablement Lead
174370	Product Manager Homepage
163418	Product Manager Search
162312	VP Security
51293	Manufacturing Test Development Manager
4796	Xiang Dialect Specialist Freelance AI Trainer Project
47286	Entry level New graduates Computer Science CIS
55542	Kids Club Associate
7843	Seasonal Operations Associate Part Time Oakridge Centre
99966	Inside Sales Executive US
43064	Kitchen Agent 2nd Shift
126795	Band 7 8 Locum Invasive Cardiac Physiologist EP Physiologist Cardiff
10668	Machine Vision Engineer Manufacturing Automation
170620	Physical Therapy Assistant PTA Home Health PRN
42684	Staff Engineer Cloud Backend Engineering
33842	Behavior Technician
40101	Medical Assistant MA Urgent Care
175311	Solution Architect Sales Marketing
6974	Mission Trainer 866
149397	Agente di commercio Procacciatore P IVA Settore pagamenti
8874	HIL Validation Integration Engineer Automotive
151639	Personal Trainer
32273	Product Operations Specialist
15351	Construction Project Manager Intern or Co Op Summer 2027
118960	Mid Market Account Executive
118063	Customer Service Representative
25596	Operations Partner Denver CO
88767	Software Engineer Vehicle Communication C++
140624	Wound Care Nurse Practitioner
9656	Strategic Account Executive
86464	Travel Product Content Specialist
12764	Enterprise Account Executive Industries Generalist
39601	B2B SaaS Sales Development Representative
68150	Founding Forward Deployed Engineer
69707	Staff Manufacturing Product Engineer
54942	Fitness Manager
143973	Mechanical Engineer Tower Launch and Test
168086	Staff Software Engineer
110030	Administrative Assistant Castro Monday Friday
90368	Advanced Practitioner First Response Team MASH
83242	Vendeur CDD temps plein
75823	Insurance Producer Huntsville AL
53532	Project Office Coordinator Tucson AZ
170627	Physical Therapy Assistant PTA Home Health PRN
96996	Primary Care Nurse Practitioner
161968	Director Treasury Credit Collections
156012	Nevada Waitlist Contract Psychiatric Nurse Practitioner
13278	Microsoft AI and Data Architect
151898	Personal Trainer
150114	Manager Remote Accounting Services PST
28079	Sales Development Representative
178728	Escalation Engineer DLP
72341	Nurse Practitioner or Physician Assistant Plymouth MA Clinical Research
15028	Director of Business Development Construction
42055	Software Development Manager ECommerce Engineering
167917	Software Engineer Alarms
112228	Bilingual English and Spanish Member Loyalty Representative R14210
60020	Head of Technical Accounting Financial Reporting
51751	Patient Coordinator
4491	Mathematics Specialist Fluent in Italian Freelance AI Trainer Project
79485	Client Success Representative Remote Contract
107358	Technical Product Manager GPU Infrastructure
21447	Certified Home Health Aide CHHA Work Close to Home
113681	Assistant de vie
66330	Staff Security Engineer Threat Detection Response
73051	Mobile Product Developer Growth
167639	Veterinary Assistant Internal Medicine
143200	Contractor Service Administrator Facilities
134476	Executive Assistant
21884	Home Care Nurse LPN
91959	Team Leader Children s Residential
54473	Assistant Stretch Manager
68068	Consumer Insights
25732	Facilities Clinic Operations Coordinator
153829	Personal Trainer
170109	Paralegal
103166	Satellite Operator
125374	Onsite Contract Interpreters
1236	Director Content Development
91595	Social Worker Looked After Children
66202	Executive Protection Specialist
35876	Enterprise Account Executive Upper Mid Market
91134	Social Worker Child Protection
67898	Fullstack Engineer Marketing
38040	Executive Director Precision Medicine Executives
26879	Psychiatric Mental Health Nurse Practitioner PMHNP
100227	Medical Assistant I
98184	Manager Growth Marketing
133337	Product Manager Money Movement
85859	VP of Energy Solutions Owner s Representative Data Center Construction
60384	Apparel Graphic Designer I
91434	Social Worker Children s Support and Safeguarding
72578	Territory Sales Manager Charleston SC Columbia SC Greenville SC
57300	General Manager Pump Power HVAC
145014	Site Reliability Engineer AI Infrastructure Starshield
158102	Sales Representative
128099	Permanent Adult Neuro Rehab Speech and Language Therapist Glasgow
120643	Investor Relations Associate French speaking
121857	AI Trainer Graphical Abstract Physics Remote New York
11590	Product Designer Space
104842	Dermatologist Cape Cod MA
125076	Survey Participants Pregnancy Care Research Mesa US
30506	Cost Accounting Manager
124501	Pharmacists AI Training Long Beach US
153101	Personal Trainer
12920	Product Manager Claude Science
153230	Personal Trainer
105473	Salesforce Engagement Manager
121893	AI Trainer Materials Science Specialists Remote Boston
49252	Structural Engineer
50749	Reverse Engineer
125569	Android Software Engineer Vilnius
155989	Colorado Waitlist Contract Psychiatric Nurse Practitioner
40355	Dishwasher
99700	Human Resources Coordinator
154373	Personal Trainer
39357	Inventory Control Specialist
154481	Personal Trainer
101834	Lead Product Manager Payments
44046	Director of US Payroll and Tax Compliance US Experience
62092	Executive Assistant Design
17596	RAN Deployment Engineer
35620	Systems Engineer Integration Test
131154	Store Manager Full Time Passy
78466	Executivo a de Contas Estratégicas III Farmer
101847	Backend Engineer
165572	Agent Experience Coordinator
133166	Quantum Error Correction Researcher qLDPC
32303	Customer Service Representative
130920	Java Engineer Distributed Systems Serverless Elasticsearch
18280	Civil Designer Land Development
140417	AI Memory Solution Architect
117560	Field Demand Generation Marketer
78756	Java Software Engineer
51236	Product Manager Financial Crime Name Payment Screening
151628	Personal Trainer
113336	Executive Director
30537	Assistant Curator Schlinger Chair of Arachnology
147056	2027 Venture Capital Summer Analyst
151945	Personal Trainer
42581	Manager Trust and Safety Compliance Audit
82907	Directeur adjoint de magasin
23362	Registered Nurse Weekends Home Health Living
14845	In Vivo Research Associate II Konermann Lab
35002	Research Assistant I
82166	FT Front Desk Coordinator
120987	Partner Development Executive
41392	Entry Level Sales Executive Employee Benefits
136860	Per Diem Clinical Research Nurse Home Visits
16663	Engineer Field Process Boise ID
131107	Sales Lead Full Time Broadway Plaza
31607	Python Developer HR Strat Engineer
113232	GI Nurse Practitioner Arizona California New Mexico Nevada or Utah
38234	Accounting Manager Tokenized Equities
115523	Front Desk Coordinator
110110	Family Medicine Physician South Austin
84931	Data Engineer II
3473	Maintenance Specialist The Pinnacle
163501	Site Reliability Engineer
153983	Personal Trainer
17546	Manufacturing IT Technician
139923	R&D Engineer
24927	Inside Sales Representative Remote
42081	Staff ll Machine Learning Engineer Search Relevance
140558	Clinical Sales Manager
153379	Personal Trainer
152700	Personal Trainer
77776	Environmental Compliance Manager
49301	Marketing Executive Marketing Executive Digital Subscription Growth
154033	Personal Trainer
14867	Assistant General Counsel EPC Transactions
15338	Construction Project Manager Intern or Co Op 2027
106672	Document Control Quality Systems Administrator
77527	Partner Account Manager Washington DC
173457	Veterinary Receptionist Client Service Representative
101935	Psychiatric Mental Health Nurse Practitioner PMHNP
155053	Personal Trainer Salem MA
163706	Strategic Account Executive
175179	ABA Clinical Assistant
51885	Manager Tax Advisory Planning Remote USA
1423	Instructional Aide
15787	Capacity Sales Representative
146012	U.S Liaison Officer to the U.S Congress
121655	AI Trainer Computer Science Expert Graphical Abstract Fully Remote Birmingham
16108	Stage Communication Stratégie création de contenus Adopt AI
74926	Preconstruction Engineer
176163	Executive Non Biddable
133894	Electrical Technician All levels
77555	AI Trainer Automotive Expertise Required
82599	Meet us at NeurIPS 2026
57985	Territory Account Manager Pump Power HVAC
139059	Patient Outreach Specialist Pharmacy Technician
169178	Veterinary Nursing Manager Full Time Ralph Ave Brooklyn NYC Relocation Assistance Available
114585	Bilingual Field Care Coordinator Field Case Management
153537	Personal Trainer
153522	Personal Trainer
48079	Sales Development Representative
21650	CNA Personal Aide for People with Disabilities
106181	Strategic Assistant to the CEO Co Founder
126003	Band 5 Accident Emergency A&E Nurse Lewisham
117136	Director Digital Product
110811	Head of Commercial Strategy
34772	Human Resources Business Partner
96979	Primary Care Nurse Practitioner
97562	Seasonal Stylist Retail Part time
103676	Account Executive
61854	Software Engineer High Performance Scale
153994	Personal Trainer
13365	Events and Ministry Coordinator Onsite
9664	Maintenance Mechanic Technician
79521	Graphic Designer Data Visualization Remote Contract
134448	Account Manager
86303	Rodman
153847	Personal Trainer
81415	Strategic Account Manager Los Angeles
170828	Assistant Project Manager Construction
41699	Coupang Pay Back end Engineer Pay Test Operations
73078	Staff Machine Learning Engineer Menu Personalisation
1326	3rd Grade Teacher
152615	Personal Trainer
77636	Sales Development Representative
113428	Resident Assistant Resident Aide RA
1119	Software Engineer II Full Stack
10021	Entry Level Project Manager Construction
48538	Revenue Enablement Manager
176941	Java Developer with Italian Integration Azure
144918	Power Systems Engineer Starship
152227	Personal Trainer
```

---

## Predictions — do not read until step 3

This is what the classifier answered for each of the rows above. Reading it before the labels are
written to disk destroys the measurement: the labeller would agree with the classifier and the
numbers would be decorative.

```
87	UNKNOWN
374	OUT
590	UNKNOWN
918	OUT
1044	UNKNOWN
1119	IN
1236	UNKNOWN
1326	OUT
1342	OUT
1423	UNKNOWN
1527	OUT
1565	UNKNOWN
1705	IN
2479	OUT
2558	UNKNOWN
2583	UNKNOWN
2752	OUT
2782	IN
2946	OUT
3069	OUT
3171	OUT
3203	UNKNOWN
3443	UNKNOWN
3473	UNKNOWN
3483	OUT
4018	OUT
4066	OUT
4303	OUT
4312	OUT
4409	OUT
4467	OUT
4491	OUT
4494	OUT
4616	OUT
4644	OUT
4741	OUT
4778	OUT
4796	OUT
5186	UNKNOWN
6246	UNKNOWN
6865	OUT
6940	UNKNOWN
6974	OUT
7467	UNKNOWN
7843	UNKNOWN
8049	UNKNOWN
8874	OUT
9292	OUT
9368	OUT
9397	OUT
9656	OUT
9664	OUT
9813	OUT
10021	OUT
10668	OUT
10712	OUT
10974	UNKNOWN
11152	IN
11289	UNKNOWN
11474	OUT
11590	UNKNOWN
12452	OUT
12505	OUT
12661	UNKNOWN
12704	UNKNOWN
12764	OUT
12920	OUT
13156	OUT
13278	IN
13334	OUT
13365	OUT
13420	OUT
13488	OUT
13750	OUT
13956	OUT
14009	UNKNOWN
14065	UNKNOWN
14357	UNKNOWN
14649	OUT
14845	UNKNOWN
14867	OUT
14872	OUT
14874	OUT
15028	UNKNOWN
15141	OUT
15260	OUT
15338	OUT
15351	OUT
15573	UNKNOWN
15772	IN
15787	OUT
16108	UNKNOWN
16192	IN
16377	OUT
16633	IN
16638	OUT
16663	UNKNOWN
16907	UNKNOWN
17546	OUT
17596	UNKNOWN
17793	UNKNOWN
17835	UNKNOWN
18022	IN
18117	OUT
18280	OUT
18328	OUT
18392	UNKNOWN
18594	OUT
19068	UNKNOWN
19231	UNKNOWN
19247	OUT
19551	OUT
19603	OUT
19698	UNKNOWN
19745	OUT
20563	UNKNOWN
20588	UNKNOWN
20840	IN
21447	UNKNOWN
21478	OUT
21622	OUT
21650	UNKNOWN
21882	OUT
21884	OUT
22187	OUT
22201	OUT
22890	OUT
23057	OUT
23072	OUT
23095	OUT
23212	OUT
23213	OUT
23219	OUT
23227	OUT
23243	OUT
23362	OUT
23531	UNKNOWN
23589	OUT
23705	OUT
23716	OUT
24010	OUT
24039	OUT
24127	UNKNOWN
24188	OUT
24215	OUT
24405	OUT
24416	OUT
24478	OUT
24556	OUT
24593	IN
24917	OUT
24927	OUT
24951	OUT
25162	OUT
25512	OUT
25528	UNKNOWN
25596	UNKNOWN
25693	UNKNOWN
25732	OUT
25764	OUT
25930	OUT
25967	UNKNOWN
26065	OUT
26711	OUT
26729	OUT
26818	OUT
26827	OUT
26879	OUT
26958	OUT
26971	OUT
27019	OUT
27881	UNKNOWN
28079	OUT
28337	UNKNOWN
28498	OUT
28931	OUT
29019	OUT
29020	OUT
29460	OUT
29878	UNKNOWN
29938	OUT
30480	UNKNOWN
30506	OUT
30537	OUT
30777	OUT
31607	UNKNOWN
31772	UNKNOWN
32273	UNKNOWN
32303	OUT
32405	UNKNOWN
32470	UNKNOWN
32836	OUT
33135	UNKNOWN
33842	UNKNOWN
34316	UNKNOWN
34628	UNKNOWN
34772	UNKNOWN
34997	UNKNOWN
35002	OUT
35620	IN
35876	OUT
36265	OUT
36274	IN
36392	UNKNOWN
37447	OUT
37639	OUT
37656	OUT
37705	OUT
37929	UNKNOWN
38040	OUT
38231	OUT
38234	OUT
38561	OUT
38583	OUT
38920	OUT
39357	UNKNOWN
39492	OUT
39555	OUT
39594	OUT
39601	OUT
39671	OUT
39890	OUT
40093	OUT
40097	OUT
40101	OUT
40355	UNKNOWN
40704	IN
40800	OUT
40813	IN
40852	UNKNOWN
40891	UNKNOWN
41149	UNKNOWN
41232	OUT
41250	OUT
41392	OUT
41487	UNKNOWN
41699	IN
42026	UNKNOWN
42055	UNKNOWN
42081	IN
42581	OUT
42684	IN
42710	UNKNOWN
43064	UNKNOWN
43822	OUT
43826	OUT
43884	IN
44046	UNKNOWN
44055	UNKNOWN
44103	OUT
44780	UNKNOWN
44907	UNKNOWN
45143	IN
45459	OUT
45637	OUT
45735	OUT
45770	UNKNOWN
46436	OUT
46733	OUT
47143	UNKNOWN
47286	UNKNOWN
47323	UNKNOWN
47558	UNKNOWN
48079	OUT
48086	IN
48538	UNKNOWN
48542	IN
48719	OUT
48838	OUT
48999	OUT
49005	UNKNOWN
49252	OUT
49301	OUT
50348	OUT
50749	UNKNOWN
50966	IN
51000	OUT
51192	OUT
51236	OUT
51264	UNKNOWN
51293	OUT
51511	OUT
51749	OUT
51751	OUT
51791	OUT
51885	OUT
52631	UNKNOWN
53220	UNKNOWN
53301	OUT
53342	OUT
53532	OUT
53617	OUT
53729	OUT
53991	OUT
53993	OUT
54015	OUT
54225	OUT
54242	OUT
54429	OUT
54459	OUT
54473	OUT
54832	UNKNOWN
54942	UNKNOWN
55529	UNKNOWN
55542	UNKNOWN
55683	UNKNOWN
56442	UNKNOWN
56858	UNKNOWN
57102	OUT
57182	OUT
57300	OUT
57384	OUT
57600	OUT
57678	OUT
57680	OUT
57682	OUT
57683	OUT
57704	OUT
57725	OUT
57854	OUT
57985	OUT
58680	OUT
58959	OUT
59417	OUT
59877	OUT
60020	UNKNOWN
60300	UNKNOWN
60384	OUT
60519	UNKNOWN
60854	OUT
60966	UNKNOWN
61057	IN
61144	UNKNOWN
61329	OUT
61500	UNKNOWN
61542	UNKNOWN
61790	UNKNOWN
61854	IN
61993	OUT
62092	OUT
62769	IN
62799	UNKNOWN
63042	OUT
63219	OUT
63250	OUT
63587	OUT
63657	OUT
63803	OUT
64842	OUT
64897	UNKNOWN
65079	OUT
65318	OUT
65338	OUT
65884	OUT
66032	IN
66085	IN
66202	OUT
66288	OUT
66330	IN
66408	OUT
66487	OUT
67299	UNKNOWN
67471	UNKNOWN
67898	OUT
68068	UNKNOWN
68150	UNKNOWN
68166	OUT
68180	OUT
68670	OUT
68812	UNKNOWN
69290	UNKNOWN
69598	UNKNOWN
69707	OUT
70192	UNKNOWN
70461	OUT
70571	IN
70936	OUT
71060	OUT
72009	OUT
72213	OUT
72341	OUT
72567	UNKNOWN
72578	UNKNOWN
72647	OUT
72705	OUT
73044	UNKNOWN
73051	IN
73078	IN
74186	UNKNOWN
74252	OUT
74518	OUT
74581	OUT
74700	OUT
74926	UNKNOWN
75351	UNKNOWN
75515	OUT
75823	UNKNOWN
76042	IN
76074	UNKNOWN
76903	OUT
76957	OUT
77022	OUT
77456	IN
77485	UNKNOWN
77527	UNKNOWN
77555	OUT
77636	OUT
77776	UNKNOWN
78093	UNKNOWN
78219	OUT
78466	UNKNOWN
78756	IN
78947	UNKNOWN
78991	UNKNOWN
79026	OUT
79096	OUT
79307	UNKNOWN
79329	OUT
79452	OUT
79472	OUT
79485	OUT
79511	OUT
79521	OUT
79779	OUT
80067	IN
80192	IN
80375	IN
81004	OUT
81415	UNKNOWN
81794	UNKNOWN
82166	OUT
82291	OUT
82423	UNKNOWN
82599	UNKNOWN
82661	OUT
82709	OUT
82718	OUT
82907	UNKNOWN
83105	UNKNOWN
83242	UNKNOWN
83440	UNKNOWN
83933	OUT
84138	UNKNOWN
84173	UNKNOWN
84751	UNKNOWN
84931	IN
85020	UNKNOWN
85490	IN
85675	OUT
85751	OUT
85859	OUT
86139	OUT
86303	UNKNOWN
86304	OUT
86464	UNKNOWN
86752	OUT
87061	UNKNOWN
87146	OUT
87752	OUT
87969	UNKNOWN
88019	OUT
88309	OUT
88449	OUT
88689	OUT
88767	IN
88809	OUT
88829	OUT
89361	OUT
89404	UNKNOWN
89423	IN
89618	UNKNOWN
89621	OUT
89643	UNKNOWN
89684	OUT
89754	UNKNOWN
89982	IN
90308	UNKNOWN
90345	UNKNOWN
90368	UNKNOWN
90375	UNKNOWN
90420	OUT
91072	UNKNOWN
91134	UNKNOWN
91434	UNKNOWN
91595	UNKNOWN
91959	UNKNOWN
92150	IN
92345	OUT
92354	UNKNOWN
92568	OUT
92731	UNKNOWN
92849	OUT
93149	OUT
93773	UNKNOWN
93894	OUT
94238	OUT
94600	UNKNOWN
94888	IN
95671	UNKNOWN
95752	UNKNOWN
96275	IN
96655	IN
96686	UNKNOWN
96712	OUT
96779	OUT
96852	OUT
96979	OUT
96983	OUT
96996	OUT
97039	OUT
97061	OUT
97102	OUT
97119	OUT
97488	UNKNOWN
97500	UNKNOWN
97562	UNKNOWN
97588	OUT
97605	OUT
97826	UNKNOWN
98098	OUT
98184	OUT
98765	OUT
99124	OUT
99183	OUT
99256	UNKNOWN
99557	OUT
99684	OUT
99700	OUT
99761	UNKNOWN
99807	OUT
99966	OUT
99992	UNKNOWN
100182	UNKNOWN
100210	OUT
100227	OUT
100424	UNKNOWN
100533	OUT
100535	OUT
100567	OUT
100669	OUT
100675	OUT
101834	OUT
101847	IN
101933	OUT
101935	OUT
102098	UNKNOWN
102187	OUT
102556	OUT
102558	OUT
102707	IN
102773	OUT
102885	UNKNOWN
103104	UNKNOWN
103166	UNKNOWN
103629	OUT
103638	OUT
103663	OUT
103676	OUT
104259	OUT
104289	OUT
104499	UNKNOWN
104622	IN
104699	IN
104770	UNKNOWN
104842	UNKNOWN
105473	UNKNOWN
105792	IN
106086	IN
106170	OUT
106181	OUT
106501	IN
106672	IN
106945	UNKNOWN
107341	IN
107358	OUT
107586	OUT
107740	OUT
108010	UNKNOWN
108322	OUT
108351	OUT
108383	UNKNOWN
108871	OUT
109060	OUT
109430	UNKNOWN
109471	OUT
109533	OUT
109677	OUT
109979	IN
110030	OUT
110110	UNKNOWN
110174	OUT
110440	UNKNOWN
110494	UNKNOWN
110667	OUT
110733	IN
110811	UNKNOWN
111451	UNKNOWN
111502	UNKNOWN
111912	UNKNOWN
112062	OUT
112165	OUT
112228	OUT
112235	OUT
112319	OUT
112490	IN
112654	OUT
112806	IN
113015	UNKNOWN
113232	OUT
113336	OUT
113428	OUT
113519	UNKNOWN
113681	OUT
113972	UNKNOWN
114278	OUT
114464	OUT
114585	OUT
115523	OUT
116127	OUT
116456	UNKNOWN
116549	OUT
117074	IN
117136	UNKNOWN
117493	IN
117560	UNKNOWN
117600	OUT
117665	OUT
117750	OUT
117782	OUT
117858	OUT
117970	OUT
118063	OUT
118310	UNKNOWN
118657	UNKNOWN
118857	UNKNOWN
118960	OUT
119214	OUT
119746	OUT
120643	UNKNOWN
120987	OUT
120993	OUT
121164	IN
121582	OUT
121655	OUT
121692	OUT
121857	OUT
121860	OUT
121893	OUT
121930	OUT
121934	OUT
122533	UNKNOWN
123016	UNKNOWN
123123	UNKNOWN
123201	UNKNOWN
123237	OUT
123670	UNKNOWN
123847	UNKNOWN
124074	UNKNOWN
124389	UNKNOWN
124501	UNKNOWN
124574	UNKNOWN
125076	UNKNOWN
125257	OUT
125374	UNKNOWN
125508	UNKNOWN
125569	IN
125680	UNKNOWN
125701	OUT
125863	UNKNOWN
125894	UNKNOWN
126000	OUT
126003	OUT
126074	OUT
126202	OUT
126351	UNKNOWN
126795	UNKNOWN
126868	OUT
127148	UNKNOWN
127915	OUT
128099	UNKNOWN
128263	OUT
128372	UNKNOWN
128656	OUT
129098	OUT
129127	UNKNOWN
129722	IN
129887	IN
129906	OUT
129986	OUT
130000	IN
130273	OUT
130414	OUT
130449	OUT
130548	IN
130649	UNKNOWN
130691	OUT
130920	IN
131107	UNKNOWN
131154	OUT
131245	OUT
131364	IN
131409	UNKNOWN
131564	OUT
132022	OUT
132259	OUT
132290	OUT
132378	OUT
132519	OUT
132547	UNKNOWN
132595	OUT
132682	OUT
132686	OUT
132917	UNKNOWN
133166	UNKNOWN
133337	OUT
133370	UNKNOWN
133394	OUT
133827	OUT
133843	UNKNOWN
133894	UNKNOWN
134284	OUT
134328	IN
134351	UNKNOWN
134424	UNKNOWN
134448	UNKNOWN
134450	UNKNOWN
134476	OUT
134522	OUT
134582	OUT
134647	IN
134802	OUT
135192	UNKNOWN
135244	UNKNOWN
135357	OUT
135590	IN
135682	OUT
135990	UNKNOWN
136330	OUT
136786	UNKNOWN
136847	OUT
136860	OUT
137140	UNKNOWN
137334	UNKNOWN
137518	UNKNOWN
137815	IN
138327	UNKNOWN
138782	UNKNOWN
139047	OUT
139059	OUT
139082	OUT
139483	OUT
139624	OUT
139629	IN
139923	UNKNOWN
140417	IN
140558	OUT
140624	OUT
140735	UNKNOWN
142163	OUT
142229	IN
142291	OUT
142833	OUT
142843	OUT
142966	IN
143097	UNKNOWN
143200	OUT
143973	OUT
144538	OUT
144799	UNKNOWN
144918	IN
145014	IN
145094	OUT
145576	OUT
145602	UNKNOWN
145665	OUT
145764	IN
145887	IN
146012	UNKNOWN
146224	OUT
146469	OUT
146644	OUT
146687	UNKNOWN
147056	UNKNOWN
147244	OUT
147580	UNKNOWN
147740	UNKNOWN
147909	OUT
148082	UNKNOWN
148184	OUT
148210	OUT
148395	IN
148399	OUT
148671	UNKNOWN
148912	IN
149010	OUT
149104	UNKNOWN
149156	OUT
149230	OUT
149397	UNKNOWN
149654	OUT
149993	IN
150113	OUT
150114	OUT
150196	UNKNOWN
150344	OUT
150370	OUT
150503	OUT
150692	OUT
150748	OUT
150963	OUT
151067	OUT
151072	OUT
151167	OUT
151259	OUT
151372	OUT
151387	OUT
151595	OUT
151628	OUT
151639	OUT
151672	OUT
151674	OUT
151711	OUT
151762	OUT
151775	OUT
151898	OUT
151921	OUT
151945	OUT
152227	OUT
152248	OUT
152301	OUT
152450	OUT
152521	OUT
152548	OUT
152615	OUT
152663	OUT
152679	OUT
152700	OUT
152934	OUT
153092	OUT
153101	OUT
153193	OUT
153230	OUT
153260	OUT
153295	OUT
153356	OUT
153379	OUT
153522	OUT
153537	OUT
153550	OUT
153566	OUT
153721	OUT
153759	OUT
153822	OUT
153829	OUT
153847	OUT
153983	OUT
153994	OUT
154033	OUT
154188	OUT
154195	OUT
154230	OUT
154231	OUT
154233	OUT
154282	OUT
154366	OUT
154373	OUT
154481	OUT
154565	OUT
154640	OUT
154694	OUT
154896	OUT
154904	OUT
154976	OUT
154984	OUT
155053	OUT
155078	OUT
155121	OUT
155137	OUT
155410	OUT
155989	OUT
156012	OUT
156205	OUT
156388	OUT
156399	OUT
156494	UNKNOWN
156719	OUT
156894	IN
156931	OUT
157173	UNKNOWN
157378	UNKNOWN
157842	OUT
157868	OUT
158102	OUT
158145	OUT
158498	UNKNOWN
159023	UNKNOWN
159165	UNKNOWN
159602	UNKNOWN
159701	OUT
159894	UNKNOWN
160296	OUT
160326	OUT
160772	OUT
161124	OUT
161135	OUT
161150	UNKNOWN
161201	OUT
161469	OUT
161512	IN
161567	IN
161717	OUT
161735	UNKNOWN
161741	OUT
161968	UNKNOWN
162107	IN
162258	OUT
162312	UNKNOWN
162458	UNKNOWN
162508	OUT
162705	OUT
163013	IN
163383	OUT
163418	OUT
163430	UNKNOWN
163501	IN
163637	OUT
163706	OUT
164022	UNKNOWN
164144	OUT
164631	OUT
164828	UNKNOWN
165180	OUT
165539	OUT
165572	OUT
166207	OUT
166265	OUT
166680	IN
166973	UNKNOWN
167066	UNKNOWN
167434	OUT
167508	OUT
167518	OUT
167639	OUT
167707	OUT
167740	OUT
167805	IN
167917	IN
168086	IN
168825	OUT
168926	OUT
169069	UNKNOWN
169124	OUT
169173	OUT
169178	OUT
169434	OUT
169518	OUT
170109	UNKNOWN
170168	UNKNOWN
170620	OUT
170627	OUT
170629	OUT
170687	OUT
170710	OUT
170764	UNKNOWN
170828	OUT
172100	UNKNOWN
172331	UNKNOWN
172498	UNKNOWN
173052	UNKNOWN
173395	OUT
173400	OUT
173457	OUT
173892	OUT
174020	UNKNOWN
174370	OUT
174460	OUT
174716	UNKNOWN
174722	UNKNOWN
175081	UNKNOWN
175179	OUT
175311	IN
175402	UNKNOWN
175594	UNKNOWN
175633	UNKNOWN
175648	OUT
175762	UNKNOWN
176163	OUT
176412	IN
176552	OUT
176649	OUT
176905	IN
176941	IN
177045	OUT
177145	OUT
177606	OUT
177705	UNKNOWN
178054	OUT
178357	OUT
178560	IN
178728	UNKNOWN
```
