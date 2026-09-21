# Iteration 3 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. Criterion revision `33113b2`, unchanged.

The filename carries 2026-09-23 rather than the day it ran, because iterations 1 and 2 already
hold `engineering-role-2026-09-21.md` and `engineering-role-2026-09-22.md` and the next session
finds its input by taking the newest file.

This iteration labelled the 1000 rows iteration 2 drew, scored iteration 2's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-22.tsv`; they were written to disk before any
rule of the classifier was read.

## The numbers

The two rates score **iteration 2's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change. That split
is what the loop's design makes available: labels can only score what has already run.

| Gated number | Iteration 2 | Iteration 3 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | 5.00% | **1.00%** (6 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | 8.00% | **8.00%** (8 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 63.25% | **48.68%** | fell < 1 pp |

The corpus after the change:

| | Iteration 1 | Iteration 2 | Iteration 3 |
| --- | ---: | ---: | ---: |
| `IN` | 12.62% | 15.58% | **15.27%** (27 347) |
| `OUT` | 20.99% | 21.17% | **36.05%** (64 571) |
| **`UNKNOWN` — the gated share** | 66.39% | 63.25% | **48.68%** (87 180) |
| &nbsp;&nbsp;of which `unruled` | 57.35% | 39.99% | **29.64%** (53 088) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 7.06% | 21.28% | **17.60%** (31 517) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.98% | 1.98% | **1.44%** (2 575) |

This move is a different shape from iteration 2's. There the `unruled` fall was largely an
accounting change into `domain_ambiguity`; here almost all of it lands in `OUT`, which grew 14.9
points. That is what a batch of never-engineering heads does: a title that no rule reached now
names a function — veterinarian, paralegal, bartender, supervisor — that no domain turns into
engineering work, and the criterion's step 3 decides it outright.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 2 called `UNKNOWN`, I
labelled 17 `IN`, 239 `OUT` and 44 `UNKNOWN`. One unknown title in eighteen is an engineering
role, against one in ten last iteration, so the pile is thinning of `IN` as well as shrinking —
but it still hides nearly three times as many `IN` titles as the `OUT` stratum does, and recall is
still worse than the miss rate says.

## The exit gate

Not met. Both rates pass this iteration — the miss rate fell from 5.00% to 1.00% and the false
accept rate held at 8.00% — but the unknown share fell 14.57 points, and the gate asks for a fall
of less than one. Iteration 2 failed the miss gate outright, so this is also the first of the two
consecutive iterations the exit needs rather than the second.

## Cost

Whole-word, case-insensitive matching over the cleaned title and nothing else. A full pass over all
179 098 cleaned titles takes **0.234s** in memory, against the criterion's ten-second limit,
unchanged by the change in list sizes. The endpoint's 42s wall clock is the database.

## What changed, and why

Every change below is grown from a row where my label and iteration 2's answer disagreed.

**The largest change is a batch of never-engineering heads**, and it is where almost all the
coverage came from. The `UNKNOWN` stratum was 239 rows I labelled `OUT`, and the heads they named
repeat: `veterinarian`, `physician`, `psychologist`, `pharmacist`, `therapist`, `practitioner`,
`paralegal`, `attorney`, `accountant`, `clerk`, `receptionist`, `chef`, `bartender`, `stylist`,
`mechanic`, `welder`, `electrician`, `guard`, `instructor`, `librarian`, `translator`, `editor`,
`artist`, `photographer`, `journalist`, `seller`, `volunteer`, `supervisor`, `operator`,
`superintendent`, `dispatcher` and two dozen more. None of them is a function a software domain
turns into engineering work, which is exactly what the criterion's third class is for.

Two of these are judgement calls worth naming. `supervisor` is on the list because every
supervisor in the sample — clinic, shift, manufacturing, operations, quality — was `OUT`, and a
software organisation calls the same job a lead or a manager. `operator` is on it for the same
reason; a computer operator is not an engineering role either. Both are first-position heads, so
`Supervisor Software Engineering` would now read `OUT` — no such title is in the corpus sample,
but it is the shape of title these two would get wrong.

**A batch of commercial off-domain markers**, read only under a domain-bound head the way the
physical markers are: `sales`, `account`, `customer success`, `finance`, `financial`, `investor`,
`commercial`, `real estate`, `insurance`, `procurement`, `supply chain`, `logistics`,
`merchandising`, `property`, `talent`, `recruiting`, `recruitment`, `human resources`,
`veterinary`, `dental`, `fitness`, `hospitality`, `culinary`, `housekeeping`, `events`,
`social media`, `brand`, `editorial`, `public relations`. These decide the manager titles the head
list left open — `Customer Success Manager`, `National Channel Sales Manager`, `Talent Acquisition
Specialist`, `Investor Relations Strategy Manager`.

`sales` was deliberately absent from the marker list until now, on the grounds that Sales Engineer
names the solutions-engineer role in some companies. It is safe to add because `sales engineer` is
a ruling, and rulings run before step 4 — so the one title the marker would decide wrongly never
reaches it. `events` is plural on purpose: `event` would have caught event-driven architecture.
`social media` rather than `media`, for streaming media engineers.

**Twelve new physical markers, from the false accepts**: `controls`, `electronics`, `plc`,
`turbomachinery`, `combustion`, `thermal`, `hydraulic`, `pneumatic`, `cryogenic`, `cryogenics`,
`industrial`, `materials`. Five of the eight false accepts were physical engineering reading as
software on the `automation`, `test`, `integration` and `quality assurance` qualifiers —
`Quality Assurance Engineer PLC Automation`, `Automation Controls Engineer Asset Engineering`,
`Electronics Integration and Development Engineer`, `Turbomachinery Test Responsible Engineer`.

**`solutions` and `solution` are no longer software qualifiers.** Every industry sells solutions,
and the word under a head decided `Workplace Solutions Manager` in. The solutions roles that are
engineering roles are named by ruling instead: `solutions architect`, `solutions consultant` and
`solution engineer` were already there, and **`solutions engineer` and `solutions engineering` are
new**.

**The `engineering manager` ruling is gone.** It resolved to `scope_ambiguity` and, because
rulings override everything, it overrode the modifier that would have decided the title:
`Engineering Manager Data Delivery Platform`, `Engineering Manager SRE AV Fleet Japan` and
`Engineering Manager Resolutions Platform Brazil` were all held open by it. Removing it lets the
`engineering` head read them the ordinary way — `IN` on their software qualifier, `OUT` under
`Manufacturing Engineering Manager`, and `domain_ambiguity` for a bare `Engineering Manager`,
which is the honest answer.

**A ruling `ai training` → `OUT`**, alongside the `ai trainer` ruling it matches. The corpus holds
the same family of postings spelt both ways, and the `ai` qualifier was deciding the `training`
spelling `IN` — `AI Training Education Analyst` was a false accept for exactly that reason.

**Four smaller additions**, each from a row the sample showed:

- New software qualifiers `algorithm`, `algorithms`, `middleware`, `ux`, `ui`, `edi` and
  `identity access management`, for `Algorithm Developer`, `Middleware Engineer`, `Product
  Engineer UX UI`, `EDI Mapping Specialist` and the two identity-management titles.
- A ruling `forward deployed engineering` → `IN`, because `forward deployed engineer` did not
  reach `Forward Deployed Engineering Manager`.
- A ruling `chief technology officer` → `IN`. The title names no head at all, so it was `unruled`.

## What this did not fix

Six misses and one false accept survive the change.

- `Software Engineer with Java Warehouse Automation` and `Director Software Engineering Retail
  Platform Delivery ELERA` — `warehouse` and `retail` are off-domain markers, and step 4 says a
  marker beats a software qualifier. A ruling on `software engineer` would fix both and would
  contradict the criterion on its own worked example, `mechanical software engineer`, so it was
  not written. These stay wrong.
- `Staff Systems Engineer Oracle EBS Accounting` — the same trade on `accounting`, and
  `Manager Salesforce Marketing Cloud Development` the same on `marketing`.
- `Product Manager Rider and Driver Mobile Experience` — `driver` as a marker, catching the
  rideshare sense of the word.
- `Computer Information Systems Specialist Freelance AI Trainer Project` — the known exception to
  the `ai trainer` ruling, recorded by iteration 2 and still wrong.
- `AI Resilience Fund Manager` — the `ai` qualifier under `manager`, on a fund.

Three rows the change newly gets wrong:

- `Java Engineer OKX Pay Smart Account Team` — the new `account` marker, reading a team name as a
  commercial domain. This is the clearest cost of that marker, and it is kept because
  `Account Manager` is a large and plainly commercial family.
- `Staff Controls Software Engineer` — the new `controls` marker. It buys two false accepts and
  costs this one.
- `Data Science Analysis AI Training Boston US` and the San Francisco posting of the same — the
  new `ai training` ruling, on the two variants of that family where a data-science background
  alone is the qualification.

## What the next session should look at

The two rates now pass and the unknown share is the number that fails, so the question is what is
left in the 29.64% that is still `unruled`. The four `UNKNOWN` rows I labelled `IN` that no rule
reaches point at Portuguese and Spanish titles — `Analista de Desenvolvimento Sênior` — and at
heads such as `roboticist`. Multilingual heads are the larger of the two and were left alone
deliberately: half a head list in one language is worse than none.

`director` was considered for a second time and left off again, and this sample says why more
precisely than iteration 2 could. Adding it as a domain-bound engineering-capable head would
decide `Director of Product AI Underwriting` and `Director of Engineering Infrastructure` `IN`,
and would convert `Director Customer Success`, `Director Strategic Finance`, `Creative Director`
and `Medical Director` from `OUT` to `domain_ambiguity` — it would raise the gated number to buy
two rows. The same argument covers `vp`, `head` and `president`.

Watch the two markers this iteration is least sure of. `account` already has one recorded miss,
and `controls` one. If either shows up again in the next sample's misses, it is cheaper to drop it
than to keep paying.

The loose end iteration 1 created is still open: `docs/engineering-role-criterion.md` points at
`docs/engineering-role-seed-lists.md`, which no longer exists. The criterion is the developer's
document and changes only in a grilling session, so the dangling reference is left alone.

## Held out of the draw

The 2000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`. 173 685 of the 179 098 rows were eligible; the loss is almost
entirely duplicate titles matching the calibration set, which is held out by title rather than by
id.

## The sample for iteration 4

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
56101	Operations Manager
109836	Director of Marketing
73205	Behavior Technician RBT Harrisburg PA
108099	Social Creative Art
159086	Night Editor Politics
166261	Registered Nurse PreOp PACU
51161	FX Customer Relations Executive
31210	Business Analyst Insurance
11711	SCADA Controls Engineer Rocket Motor Systems
168275	Emergency Credentialed Veterinary Technician Ardmore PA
153401	Personal Trainer
5105	Technical Project Manager Industrial Automation
153998	Personal Trainer
70054	Personal Care Specialist
109388	Civil Engineering Internship Roadway
42159	Staff Back end Engineer Rocket Pay
69257	Mechanical Engineer EV
74705	Assistant Quality Superintendent
89567	Crew Leader Production
75807	Insurance Producer Grand Junction CO
69195	After sales engineer
95585	QA Release Engineer
141117	Head of Banking
101356	INTERCOMPANY ACCOUNTANT
173916	Litigation Paralegal
59732	Clinical Data Manager
39029	Neuroscience Therapeutic Sales Specialist Wichita KS
33156	Enterprise Architect Director
10805	Mechanical Engineer Rocket Motor Systems
97542	Seasonal Stylist Retail Part time
120529	Investor Engagement Analyst Norwegian speaking
10235	Construction Systems Specialist
110292	Primary Care Nurse Practitioner or Physician Assistant Adults 14+ Sign on Bonus Available
130501	Android Engineer Design System
125223	US Residents Survey Participants Jacksonville
138315	Head of Events
145216	Summer 2027 Business Operations Internship Co op
54275	Assistant General Manager
31893	Consultant Manager Collaborative Data Ecosystems Data Sharing
111652	Paraprofessional RBT Queens NY
103554	Sales Development Representative
51957	Private Client Insurance Relationship Manager
66149	IT IAM SaaS Integration Engineer
87776	Associate Veterinarian Mt Scott Animal Clinic
125667	Lead Install Plumber
169326	Veterinary Technician Student Externship Overland Park KS
141260	Account Executive
72469	Account Manager Contra Costa County
115171	Commercial Lines Account Manager Assistant
153174	Personal Trainer
86527	Inventory Service Associate ISA Milwaukee WI 1099 Contractor
139549	Manager Talent Acquisition team 採用シニアマネジャー
163598	Fraud Specialist 1
94221	Speech Language Pathologist Inpatient PRN DCMC
132166	Environmental Field Equipment Operator Ecological Restoration
15987	Consultant SEO GEO CDI PARIS
146229	Field Sales Representative
3489	Ontario Reign Camera Operator Part Time
167035	C# Developer VB for Cloud Infrastructure
15024	Director of Business Development Construction
991	Intermediate Backend Java Developer
69804	Mechanisms Engineer
101046	Hardware Engineer
150576	Personal Trainer
168518	Emergency Credentialed Veterinary Technician Shift Lead Boulder CO
146756	Account Executive Enterprise Commerce CPG
159007	Program Assistant In Gallery Learning
76493	Recruiter Contract Role
64760	Print Optimization Intern Winter Spring 2027
109868	OmniCraft Labs Project GW 리드급 캐릭터 컨셉 7년 이상
95207	Civil Engineering Intern Summer 2027
154421	Personal Trainer
50936	Director Cybersecurity Operations and Platform Delivery
94963	Operations Associate
137036	Motion Designer
69526	Staff Software Engineer Databases SRE Germany Remote
114283	Executive Vice President Marketing
165526	Staff Analyst
150554	Personal Trainer
131970	Managing Counsel Employment Legal US
17545	Manufacturing Engineer Satellite Integration
31280	Data Analyst Data Analyst L3 5.1 7 years
142325	COUPA Technical Functional Lead
34983	Micro Lab Tech
142462	Software Engineer Quality Engineering
134444	Account Coordinator
51392	Business Development Director Channel Partnerships
43598	Don t see what you are seeking
178323	Enterprise Sales Development Representative
76186	Project Manager Construction
97163	Software Engineer
122429	Canada Residents AI Trainers Burnaby Canada
7623	Sales Associate Part Time Promenade at Westlake
27595	Psychotherapist
65809	Associate Core Operations
50230	Staff Software Engineer Event Streaming Systems
132228	Solar Design Manager
10688	Manager Manufacturing Test Engineering Electromagnetic Warfare
170130	Talent Agent Influencer
779	Mission Network Engineer
129972	Account Executive
161443	Territory Account Executive Retail Minneapolis MN
112436	Medical Assistant
15097	Project Manager Commercial Construction
142690	Experienced FX Artist
142959	AMS Verification Engineer RFIC Engineering
163541	Research Engineer
128953	Mechanical CAD Designer
43546	Strategic Sales Manager
47455	Automatic Door Technician
26353	Territory Account Executive Atlanta
90505	Family Court Adviser
74454	Designer Brand Motion
92090	Sales Associate
136920	Accounting Associate
158530	Anesthesiologist Norwood OH
120887	Multimedia designer at Private Equity Insights
149767	Retail Assistant Store Manager
69099	Stylist Heights Mercantile
53474	Nurse Practitioner or Physician Assistant Rankin County Mississippi
128586	Machine Learning Engineer Digital Experience
15115	Project Manager Construction
151100	Personal Trainer
133947	Integration Test Intern Spring 2027
30129	Software Engineer Platform Data AI Back End
59464	Business Development Representative
24533	CRM Manager Reactivation
150215	Software Developer II C# .NET
24824	Fraud Analyst
82826	Store Manager Siracusa Melilli C.C Belvedere
154879	Personal Trainer Lakeland FL
71269	Garage Door Sales Technician
169541	Veterinary Assistant
108036	Territory Sales Associate Mid Atlantic Southeast Region
99329	Territory Manager Albany GA
39736	Design Center Coordinator
141560	Breakfast Line Cook II
66353	Regional Account Executive General Pediatrics Long Island Queens
28430	Copywriter
83684	Comunicação Executivo a de Contas Júnior Turismo
157571	Consultant Strategy Communications
49605	Growth Manager Remote Romania
148277	Company Strategy Operations
89039	Associate Director Global Scientific Communications Medical Affairs
109437	Electrical Engineer Data Centers
94467	Part Time Seasonal Ambassador Streets at Southpoint
99915	Manager Digital Marketing
99575	Key Account Manager Hepatology South Germany
102379	Personal Injury Litigation Attorney
122929	Data Entry Clerk Graduates AI Training Monterrey Mexico
3908	Cable Technician
116384	Del Mar Barista
78575	Program Management Analyst III
23129	Registered Nurse Home Health Visits
107151	Accountant Transfer Pricing
178239	Specialist Business finops FTE
68361	Regional HR Service Delivery Manager APAC
85820	Design Manager Owners Representative Phoenix AZ Hybrid
156137	Motor Finance Account Manager Dealer South Yorkshire
1120	Software Engineer II Insider Risk
145340	Telecommunications Engineer BICSI RCDD Certified
128677	RRO Program Manager
57851	Shop Technician Mechanic Pump Power HVAC
141174	Retail Sales Associate Sandro Maje The Shops at Riverside Hackensack
153162	Personal Trainer
137324	Specialist Diagnostics Integration
172951	Commercial Boiler Technician
123347	Game Development Environment Artist Alabama US Freelance Remote
158730	Document Control Specialist Bentley Labs
69424	Future Sales Openings Account Executive US Remote
106573	Accountant
83245	Vendeur CDD temps plein
133505	Software Engineer WebRTC
136445	Frontier Agents Engineer Applied AI
94779	Contract 1099 Home Infusion Nurse
52394	Family Partner CSA
149431	Agente di commercio Procacciatore P IVA Settore pagamenti
71260	Garage Door Master Technician
19658	Manager Solutions Architect Deployment
11652	Propulsion Engineer Air Vehicles Air Breathing Engine Integration
156580	Commercial Analytics Projects Mid Level
38628	Assistant Community Manager
163180	Manufacturing Engineering Technician Level 1 3
128636	NAND Supply Planning Operations Manager
132276	Business Development Executive at Retail Insights
166167	Certified Hand Therapist PRN
42365	Data Analyst TW Logistics 物流資料分析師
79929	Physical Therapist PT
53962	Assistant Fitness Manager
12838	International Indirect Tax VAT GST
142540	Catalog Marketing Manager
139035	Clinical Pharmacist
60715	Executive Assistant
83695	Claims Assistant P&C
116429	Tarzana Barista
140826	Quality Analyst Salesforce Digital Technology
90620	Occupational Therapist Adult Social Care
104732	Software Engineer Agentic Search Web Rendering Engineer
93544	Service Operations Trainer Automotive Aftersales
146370	Venedors ores de Botiga Sprinter Andorra
71634	Sales Operations Analyst
27045	Psychiatric Mental Health Nurse Practitioner PMHNP
174118	Staff Software Engineer Operations Research
75198	RTH Sanitation Associate 1st Shift Days Off Saturday Sunday
51265	Fire Alarm Inspector
67587	Office Services Clerk Temporary Position
97244	Director Paid Social
160594	SV Part Time Lower Elementary Math Teacher and Teaching Assistant
132951	Business Recruiter 12 Month Contract
14910	Concrete Project Manager Construction
25963	Barista Washington D.C
34519	Registered Behavior Technician RBT
126615	Band 6 Midwife Chesterfield
135800	Staff Product Design Engineer
93759	Customer Success Executive
121030	Volunteer Team Member
151217	Personal Trainer
63540	Regional Operations Manager
58470	PRN CRNA The Physicians Centre Hospital Bryan TX Award Winning Facility Ortho Heavy
55188	General Manager
152407	Personal Trainer
114128	Responsable ouverture d agence d aide à domicile
22337	LPN Home Care Nurse
121754	AI Trainer Fluent Macedonian Speakers Houston US
56911	Account Manager
115840	Site Reliability Engineer
60033	Design Assistant
34851	Community Sales Manager
114945	Data Science Engineering Lead
97034	Primary Care Nurse Practitioner Physician Assistant
172575	Maintenance Technician EPA Certified
2687	Controls Engineer
174072	Account Executive InfuseTrack
170275	Regional Sales Manager London
145221	Summer 2027 Software Engineering Internship Co op
146377	Assistant Manager Sprinter Vilafranca del penedès
5466	Support Associate Japanese Speaker 24 7 Yokohama requires relocation to Yokohama
144577	Sourcing Specialist Construction
77799	QC Chemist 3rd Shift
111806	Registered Behavior Technician RBT Bronx NY Spanish Speaking
56341	Service Associate
158112	HVAC Account Executive
169027	Emergency Veterinary Nursing Trainer Viera FL
177988	Technical Account Specialist Customer Support
153980	Personal Trainer
28776	Software Engineer II Full Stack
98314	Patient Service Representative
155845	Database Administrator MySQL PostgreSQL Takealot
38360	Recruiter
169823	Associate General Manager
24285	Fulfillment Coordinator
151088	Personal Trainer
65121	Board Test Engineer
127301	Consultant Gastroenterologist Bristol
167752	Embedded Automated Test Test System Engineer
44690	Security Engineer
85600	ASIC Physical Design Engineer
126300	Band 6 7 Locum CT Radiographer West Sussex
13812	Application Security Engineer
146960	Growth Marketing Manager Japan Asia Startale App のプロダクトグロースを牽引
57198	Field Service Technician Mechanic
13751	Medical Assistant Avon Office
96957	Podiatrist
14292	Full Stack Engineer Nigeria Remote
20793	Growth Marketing Lead Teamup
105519	GTM Engineer Manager of Revenue Operations
105522	Telehealth Nurse Practitioner or Physician Assistant Sleep Specialist
154457	Personal Trainer
98959	Quality Representative Michels Power Inc
115672	Solar Field Service Technician II
161804	Line Cook
166874	Photo Editor Fixed Term
1585	Azure Infrastructure Engineer
101458	Copywriter TEMP
161750	Delivery Driver Restaurant Catering
141461	Lead Cloud Ops Platform Engineer
4621	Retail Trade Specialist Freelance AI Trainer Project
63336	Associate Trucking Warehousing Associate
109278	Project Manager Marketing
12859	Manager Applied AI Architecture Healthcare Life Sciences
127966	Nottinghamshire Recovery Nurse
123291	Fluent Thai Speakers AI Training US
87084	Regional Channel Account Manager Northeast Remote
37100	Accounting Specialist LATAM
140676	Aesthetic Nurse Practitioner
74876	MEP Manager Systems Integration
68918	Strategic Events Lead
133194	R&D Co op
72798	Engineering Manager Consumer Alliance
153672	Personal Trainer
83716	Sales Associate Full Time Woodbury Common
90248	Test job 123
152822	Personal Trainer
121207	Enterprise Account Executive
14517	Customer Service Representative
169302	Veterinary Technician Student Externship Leesburg VA
164922	Finance Industry Event Volunteer
42397	Director Program Management L7 I II Catalog AI Quality Taiwan based
129450	Embedded Software Engineer Multicore Platforms Avionics Networking
93647	Structural Technician
143071	Build Specialist Automation Controls Starship Launch Pad
32655	Assistant Superintendent Travel within Florida
164696	Personal Injury Case Manager
173888	Legal Support Assistant
138978	Sales Account Executive
122867	Database Administrator Graduates AI Training Querétaro Mexico
144203	Product Compliance Engineer Starlink
40990	Software Engineer II Applied Training
643	Vice President Influencer Creator Strategy
127779	Locum Consultant Histopathologist Milton Keynes
92813	Driver
152896	Personal Trainer
76009	Alternance Assistant e Office Manager
137187	Lead Engineer L1 Integration
155798	Inventory Shift Manager
4482	Mathematics Specialist Fluent in Arabic Freelance AI Trainer Project
154549	Personal Trainer
98431	Maintenance Technician Carillon
90656	Occupational Therapist Community Moving and Handling Service
130715	Social Media Manager Lonely Planet
159332	College Marketing Representative Seattle The Orchard
108403	Recruiter APJ
11189	Avionics Maintenance Engineer Advanced Effects
65166	Facilities Engineer Full Time AM Shift
7328	Operations Associate Part Time Lenox Square
175408	Account Manager AV
158072	Manager In Training
16183	Customer Success Manager Scaled Accounts
136918	2D Artist
154303	Personal Trainer
51222	Internal Audit Manager IT Information Security
165169	Physician Liaison
7495	Sales Associate Full Time Berlin Mitte
150712	Personal Trainer
64629	Robotics Field Service Technician
25948	Barista Leeds
58783	Machine Learning Engineer
96892	Nursing Care Coordinator LPN RN
62701	Commercial Sales Account Director San Francisco
26365	Territory Account Executive Pittsburgh
150022	Customer Success Manager Italian and Spanish Market
21629	CNA Certified Nursing Assistant
144097	OS Platform Software Engineer Starlink
12227	Staff Systems Engineer C2 Integration Active Clearance
173288	Registered Veterinary Technician
81249	Team Lead
149679	Outside Sales Account Executive Raleigh NC
23470	RN Home Care Nurse
68347	Compensation Partner
8352	Finance Business Partner Alternatives
27034	Psychiatric Mental Health Nurse Practitioner PMHNP
22133	Kupuna Caregiver In Home
142902	Facilities Technician
160674	Associate Client Services January 2027 Start Date
164562	Business Development Executive US Caller
110576	Nurse Practitioner The START Center for Cancer Care
23704	Trach and Vent Registered Nurse RN Home Care
105586	Software Engineer Implant Manufacturing
106765	1534 Fullstack Software Engineer React Node.js Python
126063	Band 5 Chemotherapy Nurse Stoke on Trent
118297	Shift Supervisor
17650	Technical Project Manager
173088	Associate Veterinarian Urgent Care Animal Care Clinic
104796	Staff Applied AI Researcher Agentic Search
22884	Physical Therapist Weekend Baylor
167019	Sales Development Representative SDR
102458	Seeking Paralegal New Grads
62934	Research Systems Analyst
122937	Data Entry Clerk Graduates AI Training Nice France
151127	Personal Trainer
24124	Sales Account Executive Enterprise
4076	Coding Specialist Fluent in Dutch Freelance AI Trainer Project
23497	RN Nurse Manager Home Health
173809	Social Media Project Manager Contractor
80426	Manager of Data Engineer
84466	Market Maintenance Technician
22033	Home Health Visits Registered Nurse
96775	Licensed Clinical Social Worker Psychologist
155841	Seller Support Specialist FTC
64996	Clinical Risk Management Manager
73783	Urgent Hiring Helpers for In Home Support Berkeley Springs WV 25411
120894	Operations Specialist
53832	Assistant Supervisor
131269	Friction Stir Welding Technician II Second Shift
8545	Associate Account Executive Financial Services
130365	APJ Accounting Manager
95992	Accountant
23620	Special Needs Caregiver DSP
164099	Freight Pricing Manager
110326	Primary Care Physician Mission Valencia Sign On Bonus Available
38397	Software Engineer Blockchain Platform Nodes
132253	Respiratory Therapist Home Care
11719	Site Reliability Engineer Space
44724	Business Development Representative Italian speaking
167447	Security Software Engineer v0
13855	Account Manager Associate Energy Industry
105202	Staff Software Engineer Next Gen
116156	Manager Recruitment Admissions
70276	Fachkraft Verladung Ladungssicherung
13424	Events and Ministry Coordinator Onsite
108942	Staff Staff Software Engineer Mobile
138821	Manager Social Analytics EMEA
54331	Assistant General Manager NEW Gym Opening
97671	Director of Aviation Flying G700 Bay Area CA
5427	Regional Manager Customer Partner Operations Based in Seoul
15422	Project Manager Construction
63916	Maps Intern Summer 2027
100753	Partner Specialist
160960	Machinery Maintenance Mechanic
62761	NetSuite Applications Administrator
43871	Logistics and Inventory Specialist
71802	Production Planning Material Control Coordinator Rework Operations
14257	Backend Engineering Intern 2027 Summer Internship
69219	Distribution Sales Manager
115128	Implementation Manager
105445	AI Strategy Transformation Architect
96056	Executive Producer Experiential Production
24832	Staff Engineer
122220	AI Training Personal Assistant Raleigh US
177454	Engenheiro a de Software Assistente e Júnior
141077	Software Engineer I Automation
166623	Biologics Formulation Research Internship Spring 2027
17326	CAD Engineer Associate Spring 2027
122726	Computer Sciences Graduates AI Training Colorado Springs US
58216	Primary Therapist Full Time
14142	Account Manager South Korea Advertising Solutions
177625	SpendHound Finance Partnerships Manager
31602	Project Programme Manager Financial Services
151513	Personal Trainer
149015	Tax Accountant Indirect Tax
93082	Project Manager Mechanical Plumbing
21255	1 1 Nurse LPN
34476	Registered Behavior Technician RBT
119913	Business Development Manager Norwegian speaking
94844	Territory Account Executive
89713	Flight Paramedic
130843	System Modeling Engineer Energy Storage
15114	Project Manager Construction
150417	Personal Trainer
164014	Counsel
154199	Personal Trainer
30902	NetSuite Administrator
116972	SDET II tvScientific
114678	General Application
153916	Personal Trainer
77530	Regional Director Partner Account Manager Atlantic
24243	Accounts Payable Accountant
155152	Personal Trainer Virginia Beach VA
40419	Maintenance Team Member
122844	Database Administrator Graduates AI Training Lyon France
60125	Pediatric Physical Therapist Albany County
53785	Account Executive
113653	Aide aux personnes âgées
129985	Lead Product Marketing Manager Client Experience Delivery
80874	Account Executive New Business
38757	SLED Account Executive
46434	Nurse Practitioner Home Virtual Care
31113	AI Business Consultant Consultant Wealth Asset Management
118828	Customer Marketing Manager
131057	Production Sample Coordinator Women s Apparel
48253	Food Service Worker Murray Hill
25979	General Manager Philadelphia
177812	Lead Software Engineer Java
42398	Director Program Management SMB Seller Growth Success
153215	Personal Trainer
21899	Home Care Operations Director
154215	Personal Trainer
4559	Pavement Condition Index PCI Survey Annotation Specialist Freelance AI Trainer Project
167418	Marketing Operations Manager
131679	Co Op Opportunity Automation Engineering
157553	Territory Account Manager
137801	Manager Detection Engineering Rapid Response Team
15337	Construction Project Manager Intern or Co Op 2027
89952	Staff Physical Design Engineer
151247	Personal Trainer
30093	Industry Solution Leader Oil Gas Chemical Refining Chemical Asset Performance
159211	Associate Project Finance
148471	Manager Technical Account Management
177297	Copywriter Paid Social Media Ad Creative
116048	Retail Coordinator Las Vegas Part Time 20 hours week
269	Rendering Engineer
141887	Waiter Waitress 5th Floor Shoreditch House East London
863	Anesthesiologist
93013	BIM Coordinator
158096	Sales Representative
45757	Field Marketing Manager ASEAN
55317	General Manager NEW Gym Opening
64728	Manager Paid Media
48288	Grill Cook Rittenhouse Square
114916	Director Business Development Genomics
74153	Value based Primary Care Physician Daytona Beach Palm Coast FL
147854	Vaga Temporária Auxiliar de Atendimento Logístico CNH B Carro Novo Hamburgo RS
130494	Manager Large Customer Sales UK
161205	Bilingual Strategic Cuisines Account Executive Spanish
109685	Power Systems Studies Engineer SCCAF Data Centers
127559	Consultant Psychiatrist Eating Disorders
154681	Personal Trainer Center Point AL
75144	RTH FSQA Superintendent
19890	Site Reliability Engineer I
120052	Event Helper Volunteer
54867	Fitness Counselor
83876	Campus Ambassador
141694	Host Receptionist Mandolin Aegean Bistro
91873	Social Worker Practitioner Children s Integrated Assessment Service
177608	Data Analyst Data Feeds
111630	Paralegal Brooklyn NY
48010	Sonographer Full time
60420	Product Developer
112389	Home Health Physical Therapist 10 000 Sign On Bonus
168827	Emergency Veterinary Assistant Eden Prairie
169960	Strategy Operations Manager Citymapper
154263	Personal Trainer
153151	Personal Trainer
89348	Comfort Advisor
123891	Korean Fluent Speakers AI Training Jeju South Korea
139364	Associate Counsel
127436	Consultant in Adult Psychiatry Sussex
53008	Engagement Ambassador University of Delaware
127746	Locum Band 8A Pharmacist Central London Private role
68431	Seasonal Sales Associate Part Time Editor Soho
19612	Field Service Representative
113033	Bilingual Spanish English Physician Virtual Health Assessment
103261	Motion Pleno Freelancer II Banco Talentos
139417	Product Marketing Manager
160541	Math Teacher Business Operations Specialist
104025	Japan Customer Support Representative
80380	Trainer Intern
18344	Land Solutions Project Manager Electric Transmission Remote Wisconsin
46289	Lead Area Health Information Specialist
10483	Flight Test Operator
15289	Traveling Superintendent Construction
115590	HVAC Entry Level Technician
112434	Medical Assistant
173152	DVM Student Externship North Dakota
173314	Veterinarian Bay Breeze Animal Clinic Rockport TX
140010	Infrastructure Automation Engineer
162633	Community Manager
26606	Bluehole Studio Technical Animator 12년 이상 Project V
113394	Physical Therapy Assistant PRN Home Health
40351	Customer Experience Supervisor
101211	Account Manager Digital Analytics
52176	Informatics Engineer
21890	Home Care Nurse RN
68610	Manager Retail Marketing
176073	Search Executive GOC
92830	MOT Vehicle Technician
18104	Sales Contract and Pricing Specialist
118184	Overnight Customer Service Representative
56399	Service Associate Night
103340	Field Marketing Manager East Remote
27114	Psychiatrist MD
56638	Wellness Recovery Specialist
155305	Talent Acquisition Specialist
118007	Customer Service Representative
165651	Recruiter Contract
142689	Experienced Compositor
156928	Assistant Store Manager
71550	Seasonal Sales Associate Regent Street London
68815	Director Enterprise Sales West Coast
81283	Customer Success Engineer Robotics Software and Automation
104669	Applied Scientist Efficient LLM Inference Model Optimization
18959	Program and Project Manager
58817	Lead Automotive Detailer
29317	مسؤولي المبيعات و مشرفي حسابات العملاء الرياض
10999	Production Test Specialist Barracuda
168817	Emergency Veterinary Assistant Bridgeville PA
176171	Executive Social GOC
169382	Kennel Assistant
23910	Part Time Tele Cardiology Arkansas
35781	Consulting Associate European Competition practice
82949	In Store Merchandiser
75442	Predictive Maintenance Technician Days M F
95774	Security D Mobile Patrol Deerfield Margate
156605	Digital Workspace Solutions Engineer
14944	Construction Project Manager Intern or Co Op Summer 2027
165078	Content Editor at United Media
176667	Member of Technical Staff Grok Product
100255	Tax Manager
152404	Personal Trainer
138802	Key Account Manager Sephora
98363	Sales Enablement Manager Sales Product Training
87235	Systems Engineer Fault Management
168468	Emergency Credentialed Veterinary Technician Relief Louisville KY
90464	Children s Social Workers and Social Workers Brief Intervention Service Permanent
125404	Spanish Contract Interpreter
123540	Hindi Fluent Speakers AI Training New York USA
96604	Retail Assistant Manager
173098	Canyons Veterinary Clinic Cottonwood Heights UT Associate Veterinarian
23542	RN Registered Nurse Pediatrics
30472	Full Time Grocery Merchandiser Sunnyvale CA
153069	Personal Trainer
98132	Associate Account Executive New England
45025	Software Engineer Backend
174517	אחראי ת משמרת בן יהודה Shift Supervisor Ben Yehuda
42642	쿠팡 HRM Specialist L5 Eats Business Management
33501	Behavior Technician
126804	Band 7 and Band 8a MRI Radiographer London
131849	Onboarding Operations Specialist
98554	Buyer
58871	Assistant Store Leader Product Operations Everlane Seaport
22749	Physical Therapist
134524	Device Sales Marketing Manager
56015	Operations Manager
55052	Fitness Manager
138989	Sales Account Executive
38036	Associate Director Trade Operations
52128	PER DIEM School Based Therapist 2026 27
93072	Mechanical Quality and Commissioning Engineer
126785	Band 7 8a Clinical Pharmacist Calderdale and Huddersfiled West Yorkshire
81636	Assistant Dispensary Manager
33439	Customer Success Lead
127844	Locum Pharmacy Technician Guildford
62606	Sales Associate at FirstMind
33186	Lead AI Consultant Field
121325	Data Architect Clinical
52323	Target Digital Network Analyst Level 2
124013	Legal Professionals AI Training Virginia Beach US
44200	Lead Store Associate
84137	Sanitation Tech
114888	Partner Solution Engineer SAP
122900	Data Entry Clerk Graduates AI Training Canterbury UK
144178	Software Engineer CDN Starlink
112149	Bicycle Scooter Repair Technician
53995	Assistant Fitness Manager
8831	Mechanical Engineer Ref L 2617
118246	Shift Supervisor
30837	Enterprise Customer Success Manager Japanese speaker
127269	Clinical Research Nurse Yorkshire
24962	Enterprise Account Manager SLED
13154	Strategic Account Executive Investment Banking Capital Markets
170576	Physical Therapist PT Home Health Full Time
72342	Part time or Full time Physician Clinical Research
126167	Band 5 Paediatrics Nurse RNC Shrewsbury
157727	Engineer SoC Design Verification
143444	Fluids Engineer Launch Hardware Engineering
119465	Associate Project Manager Healthcare Advertising
29199	Management Coordinator
155510	SeniorAssociate
86339	Tax Manager
145168	Starlink Enterprise Account Lead Aviation
158803	National Account Manager Convenience Myprotein
47995	Patient Representative Full Time
62441	Sales Director Banking
130586	Staff iOS Engineer Media Foundation
32489	Paralegal Funds
139981	Receptionist Maternity Leave Replacement
149889	Fullstack Engineer
72890	Machine Operator
86846	Microbiology Technician III Quality Control
41836	Manager Back end Engineering Rocket Pay
110828	Lead Commercial Training Design
54772	Fitness Counselor
5544	Product Marketing Manager
84798	Program Analyst
150238	Personal Trainer
98401	Data Center Construction Mid to Project Manager MEP
134029	People Culture Intern Spring 2027
166511	Salesforce Marketing Cloud Consultant Specialist
124418	Pathologists Freelance Remote Memphis US
129139	Facilities Manager Advanced Manufacturing
174080	Assistant Chief Pilot
43048	Custodian
170553	Physical Therapist
47264	Customer Maintenance Trainer Conveyor Systems
77623	Channel Account Manager II Service Provider
68899	Executive Assistant
137311	Manager Brand Creative
74792	Laborer Foreman Hourly
110806	Fit Model Flexible Part Time Opportunity
129932	Project Executive CRM
27347	Psychiatrist MD
139603	Enterprise Account Executive
171208	Automation Software Engineer
157880	IT Systems Engineer
116150	Major Gifts Officer Regional Northeast
42607	Security Engineer Penetration Tester
112763	Account Manager
114366	OVERDARE Unreal Gameplay Engineer
163274	Tower Ambassador
11972	Thermal Engineer Space
160894	Software Engineer C# Java
10634	Lead Security Engineer GRC
132860	Enterprise BD Manager
170643	Registered Nurse Hospice PRN
1237	Director EU Supply Chain
136399	Machine Learning Fellow Human Frontier Collective Canada
169354	Veterinary Technician Student Externship Torrance CA
102557	Civil Engineer Project Manager
141177	Retail Sales Supervisor Temp Sandro Maje Bloomingdale s 59th Street
119970	Conference Producer at Private Equity Insights
60358	MarTech Integration Engineer
120135	Experienced Business Development Executive
63512	Manager Infrastrukturentwicklung
103263	Quant Developer Quant Associate Programme 2027
76807	Wireless Systems Software Engineer
112693	Surgical Assistant
118426	Store Manager
127084	Band 7 MRI Radiographer Sheffield
162179	Mechanical Application Engineer Harnessing
105264	Network Evaluator
12773	Enterprise Account Executive Retail CPG Trading
18988	Account Executive Belgium
109849	Finance Project Coordinator Trainee
55443	Group Fitness Instructor
103448	Data Engineer Enablement
92597	QA QC
147892	Vendedor a Externo 6 horas Pontal SP
52333	Assistant Program Manager ACCS
139766	Full Time Activities Assistant AM
110409	Virtual Seasonal Physician Assistant NY Licensed
107250	Manager Investor Relations Legal
51953	Lead Generation Specialist
33422	Manager Product Quality
41442	Salesforce Administrator
35774	Client Relations Coordinator Forensic Services practice
96080	Director Marketing Operations
39842	Physics Subject Matter Expert Virtual 3D Science Labs
150345	Personal Trainer
143874	Manufacturing Engineer Supplier Development Starlink Gateway
32702	Manager Finance Operations
130111	Account Director Healthcare Communications
32624	Product Photographer
79318	Field Service Technician Sterling VA
140200	Specialist QA Document Training
143990	Millwright Launch Hardware
158557	CRNA Lubbock TX
52585	Dentist
165224	Vascular or General Surgeon Up to 50K Sign on Bonus
116412	Redwood City Shift Supervisor
155476	Supervising Mechanical Engineer
100330	Group Product Manager Signals Identity
119625	Manager Clinical Operations Clinical Process Owner
63135	Director Account Management
98584	Construction Project Manager Michels Infrastructure Solutions Inc
168958	Emergency Veterinary Assistant Relief Overnights Clearwater FL
149082	Store Supervisor Part Time
26274	Retail Partner Manager
140627	Wound Care Physician
91279	Social Worker Child Protection
6163	Grid Connnection Engineer
124651	Product UX Professionals AI Training Jacksonville USA
60919	Activation Experiential Manager
68411	Brand Experience Lead London
3024	Industrial Maintenance Technician
168775	Emergency Veterinarian Relief Virginia Beach VA
95344	IT Security Specialist
45553	Strategic Account Executive Transportation and Logistics
108721	Lead Expert RWA Ecosystem Growth
170497	Licensed Practical Nurse LPN Home Health PRN
25753	Psychiatric Mental Health Nurse Practitioner MD License PMHNP
99502	Sales Manager Director
100797	Customer Success Manager
42311	CFS Automation Maintenance Specialist L4 YAN5 Automation
104541	Field Technical Lead Data Center Deployments
118193	Overnight Customer Service Representative Bilingual Preferred
58322	Client Relationship Manager
37085	Sales Manager
166861	Manufacturing Engineer
15179	Project Manager Tenant Improvement Construction
54129	Assistant Fitness Manager
97076	Pulmonary Nurse Practitioner Physician Assistant
103073	Superintendent Public Works
124993	Spanish Fluent Speakers AI Training Santander
39458	Case Manager SUDP FT Days
109478	Entry Level Construction Material Testing Technician
174340	Grocery Shift Lead
65240	Procurement Finance Engineering Lead 637
137397	F&B Host
141656	Gerente de Piso Soho House Los Cabos
103103	Plumbing General Superintendent Mechanical Pipe HVAC Plumbing Traveling
103252	Associate Director Retail Activation Marketing
80009	Account Manager Microsoft Advertising
17359	Mechanical Associate Engineer Winter 2027
118606	Central Funding Application Delivery Manager
152018	Personal Trainer
142349	General Manager
42853	Lead Teacher
145580	Dental Assistant
71439	AI Deployment Specialist
125346	Design Intern
108255	Legal Executive Assistant
132921	Lead Digital Designer
39038	Site of Care Account Lead Great Lakes
177296	Copywriter Paid Social Media Ad Creative
121635	AI Trainer Aerospace Engineers CAD Expertise Remote Advisory Italy
57373	Heavy Equipment CDL Driver
97109	Travel Certified Nursing Assistant
115168	Commercial Lines Account Manager
21609	Clinical Program Manager Living
15004	Construction Superintendent Intern Summer 2027
108681	Engineering Director Mobile Infrastructure
35593	Software Engineer
135884	Physical Design Engineer
158076	Manager In Training
153494	Personal Trainer
148657	Customer Success Manager
168978	Emergency Veterinary Assistant Shift Lead Alexandria VA
146302	Business Development Representative
6854	Manufacturing Engineer
45743	Enterprise Sales Executive Sweden
92923	Vehicle Technician Mechanic
113635	Aide aux personnes âgées
95357	Part Time Sales Associate
81449	AI Developer with Python for Customer Care AI Platform team hybrid
109017	Estimator Phoenix
159141	Systems Analyst Procurement
9248	Manufacturing Engineer
28621	Director Global Programs
14908	Commissioning Project Manager
21485	Certified Nursing Assistant CNA Home Care
144608	Spring 2027 Silicon Engineering Internship Co op
75452	Regional Sales Manager
97635	Mental Health Therapist II Licensed Preferred Wednesday Friday and Thursday Saturday from 7a 730p Walk in Crisis Center
12245	Staff Thermal Engineer Hypersonic Air Vehicles
78751	Institutional Sales Lead
97115	Travel Certified Nursing Assistant
173418	Veterinary Assistant
154147	Personal Trainer
136852	Per Diem Clinical Research Nurse Home Visits
86782	Director Securities Corporate Counsel
60286	Lead Affiliate Partner Programs
133801	Development Engineer Umbilicals
172923	Wellthy Care Network Caregiver Boulder CO
104813	Team Assistant Infrastructure
119518	Clinical Project Manager Clinical Project Manager
153296	Personal Trainer
34250	Registered Behavior Technician
147844	Vaga Temporária Auxiliar de Atendimento Logístico CNH A Moto Aracajú SE
98416	Lead Maintenance Technician Sonoran Apartments
115669	Solar Field Service Technician I
60542	Sales Manager
9625	Enterprise Account Executive
54436	Assistant Kids Club Manager
18926	Operator
114644	Veterinary Technician
21118	Business Development Representative
54545	Certified Personal Trainer
55421	Group Fitness Instructor
126623	Band 6 Midwife Scunthorpe
74196	General Laborer
118802	Tax Specialist Private Credit
72643	Operations Associate Planning Supply Chain
111634	Paraprofessional RBT
96472	Retail Shift Manager
90279	Payroll Specialist
96038	Cook
4646	Science Specialist Fluent in Spanish Spain Freelance AI Trainer Project
111950	Legal Counsel U.S
4697	Sports Specialist Freelance AI Trainer Project
617	Line Cook COTE 550
58382	Regional Vice President of Strategic Alliances East Coast or Central Preferred
147266	Agente Stone Consultor a Comercial Externo Cascavel PR
639	Account Executive Tech
163569	Online Elementary ELA Tutor
30965	HR Generalist EMEA
9257	Supply Chain Engineer
108847	Manager Financial Planning Analysis
45348	Software Engineer Infrastructure
11984	Service Desk Analyst
21477	Certified Nursing Assistant CNA
166267	Respiratory Therapist
82145	Front Desk Coordinator
158628	Gastroenterology Physician Springdale AR
99407	Territory Manager Steubenville OH
94466	Part Time Floor Leader Westfield Culver City
131657	Solution Architect
99453	Director of Analytics
166738	Avionics Test Technician
101719	Inventory Control Coordinator APAC
14546	Test Engineer High Pressure Gas Systems
60508	Retail Lead Detroit Lions Team Store
20735	Operador a de Veículos Região Sul de Santa Catarina
81114	Project Manager
20414	Planner Furniture Monitors
112720	Technical Program Manager TPM
169297	Veterinary Technician Student Externship H Street Washington D.C
73897	Clinical Dialysis Nurse RN Madera California
98354	Director Workplace Benefits
20618	CAD Technician
75152	Warehouse Supervisor Night Shift
113694	Assistant de vie
34014	Center Based Board Certified Behavior Analyst BCBA
92866	Sales Executive
43636	Technical Program Manager Enterprise Technology
160357	Retail Sales Manager Chicago
141303	Independent Sales Representative
14308	Customer Success Manager
126227	Band 5 Registered NICU Nurse Bristol
47578	Commercial Installer
101870	Staff Backend Engineer
60512	Retail Lead Memphis Grizzlies
56462	Service Associate Night
100368	Battery Sourcing Lead
137296	Specialist Supplier Management
54180	Assistant General Manager
20209	Specialty Account Manager Symbravo Green Bay WI
94717	Project Manager
77497	Partner Account Manager Cincinnati
105223	Product Operations Specialist
175076	Quantitative Developer
92317	Onsite Support Engineer Robotics
65603	Contract Web Engineer
101978	Appointment Setter Brand Ambassador
98447	Maintenance Technician Texcoco
160515	Elementary School Math Teacher Remote
94635	Seasonal Ambassador Upper Canada Mall
131572	Weld Technical Specialist II
67180	Bilingual Branch Manager
18454	Database Engineer PostgreSQL MSSQL
151605	Personal Trainer
149569	Field Sales Executive
84203	Store Manager Kensington Chelsea
18755	Guest Services Associate
177666	Sales Development Representative Enterprise Strategic
70890	HR Operations Specialist
24269	Strategy Consultant Transformation und Organizational Development
37160	Sales Manager
27981	Enterprise Account Executive SLED
32068	Enterprise Software Engineer
28570	Account Executive Scale Thai Speaking
151834	Personal Trainer
89214	Lifecycle Marketing Manager
23233	Registered Nurse RN Home Care Clinical Designee
709	Founding Business Development Representative Mid Market
87903	Veterinary Neurologist
107986	Client Solutions Specialist
45858	CRO and Web Analyst
121546	AI Trainer Advanced Mandarin Fluency Birmingham
63330	Account Manager
72577	Territory Sales Manager Atlanta
75390	Conventional Route Driver
173121	Credentialed Veterinary Technician CVT
1817	Inventory Configuration Specialist
102289	Litigation Paralegal
88315	Geotechnical Engineer
155432	Plumbing Engineer I
94243	Staff Nurse General Surgical Unit
92779	Assistant Store Manager Utah
151744	Personal Trainer
29814	Blockchain Risk Control Lead
148773	Strategic Account Executive Platforms
102356	Nursing Home Attorney
12909	Policy Design Manager Conventional Weapons
161754	Delivery Driver Restaurant Catering
79600	Marketing Campaign Coordinator Specialist
86724	Project Lead Uttar Pradesh
43762	Commercial HVAC R Technician
20238	Growth Marketing Specialist
25205	FRISCO Land Development New Grad
14710	Lead Supply Chain Commercial Manager
57429	Heavy Equipment Field Technician Mechanic
99119	Experienced Emergency Veterinary Assistant
57561	Heavy Equipment Shop Technician Mechanic
128078	Paediatric Special School Speech and Language Therapist
112253	Staff Software Engineer R14082
12949	Recruiting Solutions Engineer
87074	Manager US Facilities
57741	Rental Coordinator
131952	Director Strategic Partner Sales
45291	Industry Marketing Manager Manufacturing Supply Chain
36530	Talent Attraction Specialist Especialista em Atração de Talentos São Paulo Hybrid
52574	Dental Treatment Coordinator
25456	Brand Manager
59900	Apartment Maintenance Technician
154609	Personal Trainer Albany GA
177385	Assessoria de Investimentos Início de Carreira
160229	General Manager Partner Growth
169244	Veterinary Technician Student Externship Beaverton OR
153211	Personal Trainer
2164	ISSO
63	Customer Service and Labor
72796	Elektroniker für Betriebstechnik all genders
151106	Personal Trainer
135292	Admin and Workspace Experience Manager
114273	Talent Pool for Growth Product Manager
160719	Commercial Enablement Manager Corporates
142603	Manager Frontline Business Media PR
4732	Swedish Language Specialist Freelance AI Trainer Project
139716	Director R&D Equipment Engineering
79908	Physical Therapist PT
80858	Account Executive SEO
163764	Field Application Scientist SynBio and NGS Panel Design CHINA
34684	Spanish Speaking Behavior Technician
60964	Infrastructure Systems Engineer
26741	Psychiatric Mental Health Nurse Practitioner PMHNP
173492	Veterinary Technician
152163	Personal Trainer
109423	Construction Services Lead
54014	Assistant Fitness Manager
145107	Technical Project Lead Silicon Engineering
173010	Staff Accountant
163708	Strategic Account Executive
72757	Copywriter Global Creative All genders
118228	Shift Supervisor
88398	Staff Civil Engineer
130861	Staff Software Engineer Factory Automation
14838	Technical Account Manager Azure
117962	Customer Service Representative
337	Customer Care Resolution Associate
84608	Aide à domicile
```

## Predictions — do not read until step 3

This is what the classifier answered for each of the rows above. Reading it before the labels are
written to disk destroys the measurement: the labeller would agree with the classifier and the
numbers would be decorative.

```
63	UNKNOWN
269	UNKNOWN
337	UNKNOWN
617	OUT
639	OUT
643	UNKNOWN
709	OUT
779	IN
863	UNKNOWN
991	IN
1120	IN
1237	UNKNOWN
1585	IN
1817	UNKNOWN
2164	UNKNOWN
2687	OUT
3024	OUT
3489	OUT
3908	UNKNOWN
4076	OUT
4482	OUT
4559	OUT
4621	OUT
4646	OUT
4697	OUT
4732	OUT
5105	OUT
5427	UNKNOWN
5466	UNKNOWN
5544	OUT
6163	UNKNOWN
6854	OUT
7328	UNKNOWN
7495	UNKNOWN
7623	UNKNOWN
8352	UNKNOWN
8545	OUT
8831	OUT
9248	OUT
9257	OUT
9625	OUT
10235	OUT
10483	OUT
10634	IN
10688	OUT
10805	OUT
10999	IN
11189	OUT
11652	OUT
11711	OUT
11719	IN
11972	OUT
11984	UNKNOWN
12227	IN
12245	OUT
12773	OUT
12838	UNKNOWN
12859	IN
12909	UNKNOWN
12949	IN
13154	OUT
13424	OUT
13751	OUT
13812	IN
13855	OUT
14142	OUT
14257	IN
14292	IN
14308	OUT
14517	OUT
14546	IN
14710	OUT
14838	OUT
14908	UNKNOWN
14910	OUT
14944	OUT
15004	OUT
15024	UNKNOWN
15097	OUT
15114	OUT
15115	OUT
15179	OUT
15289	OUT
15337	OUT
15422	OUT
15987	UNKNOWN
16183	OUT
17326	UNKNOWN
17359	OUT
17545	OUT
17650	UNKNOWN
18104	OUT
18344	UNKNOWN
18454	IN
18755	UNKNOWN
18926	OUT
18959	UNKNOWN
18988	OUT
19612	OUT
19658	IN
19890	IN
20209	OUT
20238	OUT
20414	UNKNOWN
20618	UNKNOWN
20735	UNKNOWN
20793	OUT
21118	OUT
21255	OUT
21477	OUT
21485	OUT
21609	OUT
21629	OUT
21890	OUT
21899	UNKNOWN
22033	OUT
22133	OUT
22337	OUT
22749	OUT
22884	OUT
23129	OUT
23233	OUT
23470	OUT
23497	OUT
23542	OUT
23620	OUT
23704	OUT
23910	UNKNOWN
24124	OUT
24243	OUT
24269	UNKNOWN
24285	OUT
24533	UNKNOWN
24824	UNKNOWN
24832	UNKNOWN
24962	OUT
25205	UNKNOWN
25456	OUT
25753	OUT
25948	OUT
25963	OUT
25979	UNKNOWN
26274	OUT
26353	OUT
26365	OUT
26606	OUT
26741	OUT
27034	OUT
27045	OUT
27114	OUT
27347	OUT
27595	UNKNOWN
27981	OUT
28430	OUT
28570	OUT
28621	UNKNOWN
28776	IN
29199	OUT
29317	UNKNOWN
29814	IN
30093	UNKNOWN
30129	IN
30472	OUT
30837	OUT
30902	UNKNOWN
30965	UNKNOWN
31113	UNKNOWN
31210	UNKNOWN
31280	UNKNOWN
31602	OUT
31893	UNKNOWN
32068	IN
32489	OUT
32624	OUT
32655	OUT
32702	OUT
33156	UNKNOWN
33186	IN
33422	UNKNOWN
33439	OUT
33501	UNKNOWN
34014	UNKNOWN
34250	UNKNOWN
34476	UNKNOWN
34519	UNKNOWN
34684	UNKNOWN
34851	OUT
34983	UNKNOWN
35593	IN
35774	OUT
35781	UNKNOWN
36530	OUT
37085	OUT
37100	OUT
37160	OUT
38036	UNKNOWN
38360	OUT
38397	IN
38628	OUT
38757	OUT
39029	OUT
39038	OUT
39458	UNKNOWN
39736	OUT
39842	UNKNOWN
40351	OUT
40419	UNKNOWN
40990	IN
41442	IN
41836	OUT
42159	OUT
42311	OUT
42365	UNKNOWN
42397	UNKNOWN
42398	OUT
42607	IN
42642	UNKNOWN
42853	OUT
43048	UNKNOWN
43546	OUT
43598	UNKNOWN
43636	IN
43762	OUT
43871	OUT
44200	OUT
44690	IN
44724	OUT
45025	IN
45291	OUT
45348	IN
45553	OUT
45743	OUT
45757	OUT
45858	IN
46289	UNKNOWN
46434	OUT
47264	OUT
47455	UNKNOWN
47578	OUT
47995	OUT
48010	OUT
48253	UNKNOWN
48288	OUT
49605	UNKNOWN
50230	IN
50936	UNKNOWN
51161	OUT
51222	OUT
51265	UNKNOWN
51392	UNKNOWN
51953	UNKNOWN
51957	OUT
52128	OUT
52176	UNKNOWN
52323	IN
52333	OUT
52394	UNKNOWN
52574	OUT
52585	OUT
53008	OUT
53474	OUT
53785	OUT
53832	OUT
53962	OUT
53995	OUT
54014	OUT
54129	OUT
54180	OUT
54275	OUT
54331	OUT
54436	OUT
54545	OUT
54772	OUT
54867	OUT
55052	OUT
55188	UNKNOWN
55317	UNKNOWN
55421	OUT
55443	OUT
56015	UNKNOWN
56101	UNKNOWN
56341	UNKNOWN
56399	UNKNOWN
56462	UNKNOWN
56638	UNKNOWN
56911	OUT
57198	UNKNOWN
57373	OUT
57429	UNKNOWN
57561	UNKNOWN
57741	OUT
57851	OUT
58216	OUT
58322	UNKNOWN
58382	UNKNOWN
58470	UNKNOWN
58783	IN
58817	OUT
58871	OUT
59464	OUT
59732	OUT
59900	OUT
60033	OUT
60125	OUT
60286	UNKNOWN
60358	IN
60420	UNKNOWN
60508	OUT
60512	OUT
60542	OUT
60715	OUT
60919	UNKNOWN
60964	IN
62441	UNKNOWN
62606	UNKNOWN
62701	UNKNOWN
62761	UNKNOWN
62934	IN
63135	UNKNOWN
63330	OUT
63336	UNKNOWN
63512	UNKNOWN
63540	UNKNOWN
63916	UNKNOWN
64629	UNKNOWN
64728	UNKNOWN
64760	UNKNOWN
64996	OUT
65121	IN
65166	OUT
65240	OUT
65603	IN
65809	UNKNOWN
66149	IN
66353	OUT
67180	UNKNOWN
67587	OUT
68347	UNKNOWN
68361	UNKNOWN
68411	OUT
68431	OUT
68610	OUT
68815	UNKNOWN
68899	OUT
68918	OUT
69099	OUT
69195	UNKNOWN
69219	OUT
69257	OUT
69424	OUT
69526	IN
69804	UNKNOWN
70054	UNKNOWN
70276	UNKNOWN
70890	UNKNOWN
71260	UNKNOWN
71269	OUT
71439	IN
71550	UNKNOWN
71634	UNKNOWN
71802	OUT
72342	OUT
72469	OUT
72577	OUT
72643	UNKNOWN
72757	OUT
72796	UNKNOWN
72798	UNKNOWN
72890	OUT
73205	UNKNOWN
73783	UNKNOWN
73897	OUT
74153	OUT
74196	UNKNOWN
74454	OUT
74705	OUT
74792	OUT
74876	IN
75144	OUT
75152	OUT
75198	UNKNOWN
75390	OUT
75442	OUT
75452	OUT
75807	UNKNOWN
76009	OUT
76186	OUT
76493	OUT
76807	IN
77497	OUT
77530	OUT
77623	OUT
77799	UNKNOWN
78575	UNKNOWN
78751	OUT
79318	UNKNOWN
79600	OUT
79908	OUT
79929	OUT
80009	OUT
80380	OUT
80426	IN
80858	OUT
80874	OUT
81114	UNKNOWN
81249	UNKNOWN
81283	OUT
81449	IN
81636	OUT
82145	OUT
82826	OUT
82949	OUT
83245	UNKNOWN
83684	UNKNOWN
83695	OUT
83716	UNKNOWN
83876	OUT
84137	UNKNOWN
84203	OUT
84466	OUT
84608	UNKNOWN
84798	UNKNOWN
85600	OUT
85820	UNKNOWN
86339	OUT
86527	UNKNOWN
86724	UNKNOWN
86782	OUT
86846	UNKNOWN
87074	OUT
87084	OUT
87235	IN
87776	OUT
87903	UNKNOWN
88315	UNKNOWN
88398	OUT
89039	UNKNOWN
89214	OUT
89348	UNKNOWN
89567	UNKNOWN
89713	OUT
89952	OUT
90248	UNKNOWN
90279	OUT
90464	UNKNOWN
90505	UNKNOWN
90620	OUT
90656	OUT
91279	UNKNOWN
91873	OUT
92090	UNKNOWN
92317	UNKNOWN
92597	UNKNOWN
92779	OUT
92813	OUT
92830	UNKNOWN
92866	OUT
92923	UNKNOWN
93013	OUT
93072	OUT
93082	OUT
93544	OUT
93647	OUT
93759	OUT
94221	OUT
94243	OUT
94466	UNKNOWN
94467	OUT
94635	OUT
94717	UNKNOWN
94779	OUT
94844	OUT
94963	UNKNOWN
95207	OUT
95344	IN
95357	UNKNOWN
95585	IN
95774	UNKNOWN
95992	OUT
96038	OUT
96056	OUT
96080	UNKNOWN
96472	OUT
96604	OUT
96775	OUT
96892	OUT
96957	UNKNOWN
97034	OUT
97076	OUT
97109	OUT
97115	OUT
97163	IN
97244	UNKNOWN
97542	OUT
97635	OUT
97671	UNKNOWN
98132	OUT
98314	OUT
98354	UNKNOWN
98363	OUT
98401	OUT
98416	OUT
98431	OUT
98447	OUT
98554	UNKNOWN
98584	OUT
98959	OUT
99119	OUT
99329	UNKNOWN
99407	UNKNOWN
99453	UNKNOWN
99502	OUT
99575	OUT
99915	OUT
100255	OUT
100330	UNKNOWN
100368	UNKNOWN
100753	UNKNOWN
100797	OUT
101046	UNKNOWN
101211	OUT
101356	OUT
101458	OUT
101719	OUT
101870	IN
101978	OUT
102289	OUT
102356	OUT
102379	OUT
102458	OUT
102557	OUT
103073	OUT
103103	OUT
103252	UNKNOWN
103261	UNKNOWN
103263	UNKNOWN
103340	OUT
103448	IN
103554	OUT
104025	OUT
104541	OUT
104669	UNKNOWN
104732	IN
104796	UNKNOWN
104813	OUT
105202	IN
105223	UNKNOWN
105264	UNKNOWN
105445	IN
105519	UNKNOWN
105522	OUT
105586	OUT
106573	OUT
106765	IN
107151	OUT
107250	OUT
107986	UNKNOWN
108036	UNKNOWN
108099	UNKNOWN
108255	OUT
108403	OUT
108681	IN
108721	UNKNOWN
108847	OUT
108942	IN
109017	UNKNOWN
109278	OUT
109388	OUT
109423	OUT
109437	UNKNOWN
109478	OUT
109685	OUT
109836	UNKNOWN
109849	OUT
109868	UNKNOWN
110292	OUT
110326	OUT
110409	OUT
110576	OUT
110806	UNKNOWN
110828	OUT
111630	OUT
111634	UNKNOWN
111652	UNKNOWN
111806	UNKNOWN
111950	OUT
112149	UNKNOWN
112253	IN
112389	OUT
112434	OUT
112436	OUT
112693	OUT
112720	IN
112763	OUT
113033	OUT
113394	OUT
113635	UNKNOWN
113653	UNKNOWN
113694	OUT
114128	UNKNOWN
114273	OUT
114283	OUT
114366	UNKNOWN
114644	OUT
114678	UNKNOWN
114888	IN
114916	UNKNOWN
114945	IN
115128	UNKNOWN
115168	OUT
115171	OUT
115590	OUT
115669	UNKNOWN
115672	UNKNOWN
115840	IN
116048	OUT
116150	UNKNOWN
116156	OUT
116384	OUT
116412	OUT
116429	OUT
116972	UNKNOWN
117962	OUT
118007	OUT
118184	OUT
118193	OUT
118228	OUT
118246	OUT
118297	OUT
118426	OUT
118606	IN
118802	OUT
118828	OUT
119465	UNKNOWN
119518	OUT
119625	OUT
119913	UNKNOWN
119970	UNKNOWN
120052	OUT
120135	OUT
120529	UNKNOWN
120887	UNKNOWN
120894	UNKNOWN
121030	OUT
121207	OUT
121325	IN
121546	OUT
121635	OUT
121754	OUT
122220	OUT
122429	UNKNOWN
122726	OUT
122844	IN
122867	IN
122900	OUT
122929	OUT
122937	OUT
123291	OUT
123347	OUT
123540	OUT
123891	OUT
124013	OUT
124418	UNKNOWN
124651	OUT
124993	OUT
125223	UNKNOWN
125346	UNKNOWN
125404	OUT
125667	UNKNOWN
126063	OUT
126167	OUT
126227	OUT
126300	OUT
126615	OUT
126623	OUT
126785	OUT
126804	OUT
127084	OUT
127269	OUT
127301	UNKNOWN
127436	UNKNOWN
127559	UNKNOWN
127746	OUT
127779	UNKNOWN
127844	OUT
127966	OUT
128078	OUT
128586	IN
128636	UNKNOWN
128677	UNKNOWN
128953	OUT
129139	OUT
129450	OUT
129932	OUT
129972	OUT
129985	OUT
130111	UNKNOWN
130365	OUT
130494	OUT
130501	IN
130586	IN
130715	OUT
130843	UNKNOWN
130861	IN
131057	OUT
131269	UNKNOWN
131572	UNKNOWN
131657	UNKNOWN
131679	IN
131849	UNKNOWN
131952	UNKNOWN
131970	OUT
132166	OUT
132228	UNKNOWN
132253	OUT
132276	OUT
132860	UNKNOWN
132921	UNKNOWN
132951	OUT
133194	UNKNOWN
133505	IN
133801	UNKNOWN
133947	UNKNOWN
134029	UNKNOWN
134444	OUT
134524	OUT
135292	UNKNOWN
135800	UNKNOWN
135884	OUT
136399	UNKNOWN
136445	IN
136852	OUT
136918	OUT
136920	UNKNOWN
137036	UNKNOWN
137187	IN
137296	UNKNOWN
137311	OUT
137324	IN
137397	OUT
137801	UNKNOWN
138315	UNKNOWN
138802	OUT
138821	UNKNOWN
138978	OUT
138989	OUT
139035	OUT
139364	OUT
139417	OUT
139549	OUT
139603	OUT
139716	UNKNOWN
139766	OUT
139981	OUT
140010	IN
140200	IN
140627	OUT
140676	OUT
140826	IN
141077	IN
141117	UNKNOWN
141174	UNKNOWN
141177	OUT
141260	OUT
141303	OUT
141461	IN
141560	OUT
141656	UNKNOWN
141694	OUT
141887	OUT
142325	UNKNOWN
142349	UNKNOWN
142462	IN
142540	OUT
142603	UNKNOWN
142689	UNKNOWN
142690	OUT
142902	OUT
142959	UNKNOWN
143071	OUT
143444	OUT
143874	OUT
143990	UNKNOWN
144097	IN
144178	IN
144203	UNKNOWN
144577	OUT
144608	UNKNOWN
145107	UNKNOWN
145168	OUT
145216	UNKNOWN
145221	IN
145340	UNKNOWN
145580	OUT
146229	OUT
146302	OUT
146370	UNKNOWN
146377	OUT
146756	OUT
146960	OUT
147266	UNKNOWN
147844	UNKNOWN
147854	UNKNOWN
147892	UNKNOWN
148277	UNKNOWN
148471	OUT
148657	OUT
148773	OUT
149015	OUT
149082	OUT
149431	UNKNOWN
149569	OUT
149679	OUT
149767	OUT
149889	IN
150022	OUT
150215	IN
150238	OUT
150345	OUT
150417	OUT
150554	OUT
150576	OUT
150712	OUT
151088	OUT
151100	OUT
151106	OUT
151127	OUT
151217	OUT
151247	OUT
151513	OUT
151605	OUT
151744	OUT
151834	OUT
152018	OUT
152163	OUT
152404	OUT
152407	OUT
152822	OUT
152896	OUT
153069	OUT
153151	OUT
153162	OUT
153174	OUT
153211	OUT
153215	OUT
153296	OUT
153401	OUT
153494	OUT
153672	OUT
153916	OUT
153980	OUT
153998	OUT
154147	OUT
154199	OUT
154215	OUT
154263	OUT
154303	OUT
154421	OUT
154457	OUT
154549	OUT
154609	OUT
154681	OUT
154879	OUT
155152	OUT
155305	OUT
155432	OUT
155476	OUT
155510	UNKNOWN
155798	UNKNOWN
155841	OUT
155845	IN
156137	OUT
156580	UNKNOWN
156605	IN
156928	OUT
157553	OUT
157571	UNKNOWN
157727	UNKNOWN
157880	IN
158072	UNKNOWN
158076	UNKNOWN
158096	OUT
158112	OUT
158530	UNKNOWN
158557	UNKNOWN
158628	OUT
158730	UNKNOWN
158803	OUT
159007	OUT
159086	OUT
159141	IN
159211	UNKNOWN
159332	OUT
160229	UNKNOWN
160357	OUT
160515	OUT
160541	OUT
160594	OUT
160674	UNKNOWN
160719	OUT
160894	IN
160960	OUT
161205	OUT
161443	OUT
161750	OUT
161754	OUT
161804	OUT
162179	OUT
162633	UNKNOWN
163180	OUT
163274	OUT
163541	UNKNOWN
163569	OUT
163598	UNKNOWN
163708	OUT
163764	IN
164014	OUT
164099	UNKNOWN
164562	OUT
164696	UNKNOWN
164922	OUT
165078	OUT
165169	OUT
165224	OUT
165526	UNKNOWN
165651	OUT
166167	OUT
166261	OUT
166267	OUT
166511	UNKNOWN
166623	UNKNOWN
166738	OUT
166861	OUT
166874	OUT
167019	OUT
167035	IN
167418	OUT
167447	IN
167752	IN
168275	OUT
168468	OUT
168518	OUT
168775	OUT
168817	OUT
168827	OUT
168958	OUT
168978	OUT
169027	OUT
169244	OUT
169297	OUT
169302	OUT
169326	OUT
169354	OUT
169382	OUT
169541	OUT
169823	UNKNOWN
169960	UNKNOWN
170130	UNKNOWN
170275	OUT
170497	OUT
170553	OUT
170576	OUT
170643	OUT
171208	IN
172575	OUT
172923	OUT
172951	OUT
173010	OUT
173088	OUT
173098	OUT
173121	OUT
173152	UNKNOWN
173288	OUT
173314	OUT
173418	OUT
173492	OUT
173809	OUT
173888	OUT
173916	OUT
174072	OUT
174080	OUT
174118	IN
174340	UNKNOWN
174517	OUT
175076	UNKNOWN
175408	OUT
176073	OUT
176171	OUT
176667	UNKNOWN
177296	OUT
177297	OUT
177385	UNKNOWN
177454	UNKNOWN
177608	UNKNOWN
177625	OUT
177666	OUT
177812	IN
177988	OUT
178239	UNKNOWN
178323	OUT
```
