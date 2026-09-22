# Iteration 9 of the engineering-role classification loop

Run on the development machine on 2026-09-22, against the raw corpus snapshot already loaded in the
development database — 179 098 vacancies, cleaned and classified — and triggered with one `POST
/classifications`. The criterion is unchanged since ADR-0010: this is the second iteration whose
labels and rules answer to the rewritten document, and the first whose rules were grown under it
from a sample drawn under it.

The filename carries 2026-09-29 rather than the day it ran, for the reason iteration 3 gave: the
eight earlier iterations already hold the eight days before it, and the next session finds its input
by taking the newest file.

This iteration labelled the 1000 rows iteration 8 drew, scored iteration 8's classifier against
those labels under the skill's three-rate definition, and grew the rules from where the labels and
the answers disagreed. The labels are `src/test/resources/labels/engineering-role-2026-09-28.tsv`;
they were written to disk before any rule of the classifier was read.

**The labelling was done by a subagent**, as in iterations 5 through 8: the agent was given the
criterion and the 1000 bare titles, denied the classifier's source, the iteration reports and the
earlier label files, and told to write its labels to disk before anything else. Blinding is
ordering, and the ordering was enforced by what the agent could reach. Scoring, the rule pricing and
the draw were done by scripts over files in a scratchpad, and the rules were priced on an offline
`javac` harness whose answers were checked against the application's own counts — the 1000 rows, the
8000 accumulated labels and the 179 098 corpus titles never entered the session's context.

## The numbers

The three rates score **iteration 8's** classifier, because those are the predictions the labelled
sample carries. A mistake is any row whose state differs from the label, so an `IN` labelled
`UNKNOWN` is as wrong as an `IN` labelled `OUT`. They are scored on the stratum sizes iteration 8
drew, 100 `IN` / 500 `OUT` / 400 `UNKNOWN` — the split the skill asks for, and the first sample
drawn under it. Iterations 1–7 were scored under a narrower definition and are not comparable;
iteration 8 is.

| Gated number | Iteration 8 | Iteration 9 | Gate |
| --- | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 3.00% | **1.80%** (9 of 500) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 16.00% | **18.00%** (18 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 77.33% | **74.25%** (297 of 400) | ≤ 10% |
| `unruled` — of the whole corpus | 5.61% | **3.95%** (7 076) | ≤ 4% |

Reported alongside, gated by nothing:

- **`OUT`-stratum rows labelled `IN`** — the error nothing downstream recovers: **1** of 500, down
  from 5 of 600.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **13** — 5 it called `IN`,
  8 it called `OUT`.
- **The labeller's own unknown pile**: 116 rows, split 96 `domain_ambiguity`, 11 `scope_ambiguity`,
  9 `unruled`. This labeller used `scope_ambiguity` eleven times where iteration 8's used it once,
  which is the one reason-code shift worth noting; the pile itself is the same size as
  iteration 8's.
- **What the `UNKNOWN` stratum turned out to be.** Of the 400 rows iteration 8 called `UNKNOWN`, the
  labeller called 36 `IN`, 261 `OUT` and 103 `UNKNOWN`. The errors split 169 `domain_ambiguity` rows
  the labeller called `OUT`, 87 `unruled` rows it called `OUT`, and 26 `domain_ambiguity` rows it
  called `IN`. One unknown title in eleven is an engineering role, as in iteration 8.

The corpus after this iteration's change:

| | Iteration 7 | Iteration 8 | Iteration 9 |
| --- | ---: | ---: | ---: |
| `IN` | 17.12% | 18.46% | **18.53%** (33 179) |
| `OUT` | 58.50% | 59.06% | **61.05%** (109 339) |
| `UNKNOWN` | 24.38% | 22.49% | **20.42%** (36 580) |
| &nbsp;&nbsp;of which `unruled` — the gated share | 5.82% | 5.61% | **3.95%** (7 076) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 17.00% | 15.31% | **14.90%** (26 692) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.57% | 1.57% | **1.57%** (2 812) |

## The exit gate

**Not met**, but for the first time only two numbers fail rather than three. `unruled` is **3.95%**
against its 4% gate and passes, and the `OUT` stratum error passes with room, as it has for six
iterations. `IN` stratum error is 18.00% against 10% and `UNKNOWN` stratum error is 74.25% against
the same gate.

`unruled` passing is the one gate this loop could ever have closed by adding rules, and it closed on
the iteration that went after it directly. The other two are not that shape. The `UNKNOWN` gate asks
that the pile the classifier cannot decide be a pile a labeller could not decide either, and three
quarters of it is still rows a labeller decides on sight; three iterations remain — 10, 11 and 12 —
and nothing in iterations 5 through 9 suggests a 64-point fall is available in three. The cap, not
the gate, remains the likely end of this loop, and the skill says what to do then: write the report,
record the reason split, close #10, and hand the remaining unknown pile to #11.

## Cost

A full pass over the 179 098 cleaned titles takes **0.56s** against the ten-second limit, measured
warm on the offline harness, whose per-state counts match the application's to the row. Whole-word,
case-insensitive matching over the cleaned title; no description bodies, no network.

## What changed, and why

Four list changes and no new rulings. Priced over the 7 602 rows of the 8 000 accumulated labels
that are evidence under the current criterion — the 398 rows the superseded-family notes in the
pre-ADR-0010 fixtures exclude are not costs — the whole change moves **1 192 mistakes to 1 080**,
with **no new misses** (a row decided `OUT` that the label calls `IN` stays at 15) and six more
false accepts (137 to 143).

### The markets this sample's domain-ambiguity pile named

`strategy`, `event`, `social`, `production`, `health`, `care`, `team member`, `member experience`,
`customer service`, `customer support` and `direct support` joined the market markers, and
`dialysis` joined the discipline markers — the criterion's test for that list is the credential, and
a dialysis role is certified clinical work, so it decides out under a domain-free head too. 169 of
the 297 `UNKNOWN`-stratum errors were a known head whose modifier named who the work was for, and no
marker held it. `strategy` alone is worth 29 mistakes over the accumulated labels.

### The clinical grades the unruled pile was made of

`band`, `lpn`, `rn`, `cna`, `locum`, `underwriter`, `actuary` and `teller` became never-engineering
heads. The NHS band was the single largest family in the corpus's unruled pile — 232 titles of the
shape `band 6 echocardiographer leeds`, with no head on any list and no engineering role anywhere in
the corpus that names the word.

### Sales is a function, not only a market

`sales` was a market marker alone, which left a headless sales title (`institutional equity sales`)
unruled and read a sales title whose modifier named software (`sales manager b2b saas`) as an
engineering role. It is now also a never-engineering head. Sales Engineer is a ruling and rulings
run first, so the one title the change would have decided wrongly is untouched. It is the single
largest change of the four: 9 mistakes on the accumulated labels, and 125 corpus rows out of `IN`.

### The heads the corpus writes in other languages, and the markers beside them

Iteration 8 left this as the one change with corpus-wide reach that nobody had measured.
Twenty-eight heads landed: the Spanish, Portuguese, French and German spellings of functions the
list already held in English (`gerente`, `especialista`, `técnico`, `ingénieur`, `estágio`,
`praktikum`, `sachbearbeiter`, …), and the nouns an English title uses where it names the function
without naming the person doing it (`management`, `operations`, `controller`, `liaison`, `chief`,
`member`, `tech`, `tester`, `support`). Beside them went 21 non-English market markers (`ventas`,
`vendas`, `vertrieb`, `salud`, `recursos humanos`, …) and 3 non-English software qualifiers
(`logiciel`, `informatique`, `informatica`), because iteration 8's warning was right: a head on its
own only moves a row from `unruled` to `domain_ambiguity`, and `especialista en ventas` is only
decided once `ventas` is a market.

Every one of the twenty-eight is engineering-capable and domain-bound. That is deliberate: a head
that masks the head behind it must never be the one that decides `OUT`.

### Tried and dropped, with what they cost

- **`staff` as a head** — buys 0.07 points of `unruled` and costs 8 mistakes: `Staff Accountant` and
  `Staff Pharmacist Supervisor` become `domain_ambiguity`, because `staff` masks the accountant and
  the pharmacist. It is the masking problem in its purest form, and it was left off.
- **`planning` and `content` as market markers** — each raises the count of rows decided `OUT` that
  the label calls `IN` by one. That change is forbidden outright, whatever else it buys, so neither
  went in despite `planning` being worth 14 mistakes.
- **`member` as a never-engineering head** — 112 corpus titles are `Member of Technical Staff` and
  its variants, every one of them an engineering role. `member` went in as a capable head instead.
- **`implementation` as a market marker** — costs 3 mistakes and gains none.
- **Seventeen words that reach nothing.** The first draft of the batch carried `entwickler`, `jefe`,
  `tienda`, `verkauf`, `gesundheit`, `programmierung` and eleven more like them — the obvious next
  spelling in each language. Not one of them occurs in the 179 098 titles, so each was taken back
  out: a rule that decides no row in the corpus is a rule nobody has measured. `vendedor`, `berater`
  and `auxiliar` came out for a different reason — they were already never-engineering heads, and a
  word on both lists is read off the never-engineering one anyway.
- **`support` without `direct support`, `care` and `dialysis` beside it** — 5 mistakes, all of them
  the `Direct Support Professional` family, where `support` masked the coordinator that used to
  decide them. Priced together the four are worth 3 mistakes and the 0.09 points of `unruled` that
  put the gate within reach.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows the change was grown from. 276 of the 1000 disagree, against 324 before: one miss, 19 false
accepts, and 244 rows still left `UNKNOWN` that the labeller decided. The change moved 57 rows from
`UNKNOWN` to `OUT`, 2 from `UNKNOWN` to `IN`, and 1 from `OUT` to `UNKNOWN`. Most of what it bought
is outside this sample: the unruled families it went after are corpus-wide and this sample holds few
of them.

## What the next session should look at

**The `IN` stratum is the gate nobody has moved.** It was 35% under iteration 7, 16% under iteration
8 and 18% now, and neither the criterion's revision nor 77 new words has touched its shape: these
are titles the classifier calls `IN` and a labeller calls `OUT` or `UNKNOWN`. The 18 rows of this
sample are the list to start from; they are false accepts, which is the error class no iteration has
ever attacked directly, because every iteration has been harvesting the unknown pile.

**The `UNKNOWN` stratum is now 169 market words and 87 unreachable titles.** The market harvest
still pays — `strategy` was worth 29 mistakes on its own — but the words are getting narrower each
iteration and the tail is long. The 244 surviving rows are the next session's list.

**`unruled` passed, and it can fail again.** It passed at 3.95%, with 0.05 points of margin, on a
corpus snapshot that does not grow. Any change that adds a head without the markers beside it moves
rows into `domain_ambiguity` and any change that removes one moves them back; a session that reaches
for a head list should re-read this number before it writes its report.

## Questions for the criterion, not for the rules

**The head that masks the head behind it.** Unchanged and still the oldest open question — `staff`
is the fifth head this loop has had to leave off the list for it, after `assistant`, `associate`,
`director` and `head`. This iteration paid for it again and priced it again rather than solving it.

**Is a support role an engineering role?** Iteration 8 raised it; this labeller answered the same
way on Q4 — `Technical Support Engineer` and `IT Support Engineer` in — and this iteration then made
`support` a head, which means the question now has rules resting on it.

**Designers.** The criterion names no designer head. This labeller ruled `Product Designer`, `User
Experience Designer`, `Gameplay Designer Combat AI` and `Technical Designer` `OUT` on Q2–Q4, on the
argument that a software background alone does not make a credible designer. The corpus writes a lot
of them and the rules currently reach them through a ruling on `game designer` alone.

**`architect` is domain-free, so a building architect is undecided.** `Hospitality Architect` and
`Architect Justice+Civic` are `domain_ambiguity` by the criterion's own reading, and the labeller
followed the document rather than overriding it. If that is not the intent, the fix is a discipline
marker on the building trades, which is a criterion decision and not a rule this loop may invent.

**Firmware and networking.** `Firmware Engineer Manufacturing Test`, `SSD Firmware Development
Engineer`, `Network Engineer Starlink` and `NOC Engineer` were labelled `IN` on Q2 and Q3, while the
FPGA/ASIC argument in the criterion would put the first two `OUT`. It is the family most likely to
flip if the criterion is read again.

## Held out of the draw

The 8000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 167 685 of the 179
098 rows were eligible.

## The sample for iteration 10

1000 rows drawn at random from the predictions above — 100 from `IN`, 500 from `OUT`, 400 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each row
is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
167606	Client Experience Representative
156808	SEO Specialist Dutch speaking
84917	Associate Director Clinical Client Services
143576	Hardware Reliability Specialist Starlink Aviation
162539	Business Development Manager Partnerships Bókun
46129	Director of Solutions Consulting
68	Customer Service Sales Team Member
174509	Inventory Qiryat Bialik Kiryon מחסנאים קריון קרית ביאליק
23372	Residency Program New Nurse Graduates
46847	High School Science 2026 2027 School Year
53791	Ops Coordinator Asia
156475	Finance Manager
79405	Customer Associate
123858	Javascript Developers AI Training Tucson US
17349	Hardware Test Software Engineer
39561	Registered Nurse RN FT Front End Days
129654	Grupo QuintoAndar Analista de Inside Sales Sênior Consórcio
176712	Receiving Specialist Supercomputer Operations Memphis
88989	Customer Success Manager Enterprise
151895	Personal Trainer
37345	Response Engineer CMDC
49817	Assistant Site Manager DashMart
33719	Behavior Technician
83705	Underwriting Assistant Personal Lines
112067	Sales Development Representative Swiss German Speaking
9964	Solution Implementation Manager
119865	Crypto Growth manager
80422	Remote Child Adolescent Psychiatrist NY Licensed
101881	Backend Engineer II
60912	Trading Associate
107968	Private Client Solicitor
140841	Field Service Engineer Medium Voltage Central Dallas TX
90007	Product Engineer
156257	Security Engineer Networking
175014	Seasonal Overnight Student Chaperone Washington DC Metro Area Fall 2026 and Spring 2027
37762	Parts Associate
138293	Graduate Partnership Manager at SetSales
35848	Recruiter Recruiter GTM
124614	Portuguese Fluent Speakers AI Training BR
92996	Loonshot Games Story Artist Project Camp 5년 이상
52564	Dental Hygienist
143159	Commercial Sales Manager Starlink Mobile
50241	Strategy Operations Manager
74241	Candidature spontanée CIET
132455	Head of Event
92110	Financial Analyst Payments
31221	Business Analyst Business Analyst Banking
141129	Vertriebsmitarbeiter im Homeoffice deutschlandweit
48705	Theater District Restaurant Team
2823	Graph Engineer
116353	Commercial Product Line Manager
154762	Personal Trainer Enumclaw WA
151603	Personal Trainer
15541	Gerente de Planejamento e Performance I
177233	Staff Machine Learning Engineer Generative AI
58762	Forward Deployed Marketing Analyst
143210	Counsel Global Trade Compliance
127	Computational Theoretical Chemist I
91544	Social Worker Learning Disabilities
84238	Barista Team Member
105723	Project Manager IPP
41284	Valet Trash Tech
152557	Personal Trainer
31656	Consultant für BSI Projekte
38205	Director of Business Development
102837	Assistant Project Manager Estimator
145549	OneStream Associate
84339	Store Manager New Store Opening
16318	Administrative Associate
86301	Project Manager Civil Construction
135181	Analyst Retargeting Remarketing
12728	Director Revenue Accounting
98171	Revenue Economics Analyst
135993	Forward Deployed Engineer DRC
110677	Engagement Manager
13936	Tech Lead Java Expert
122364	Biology Graduates AI Training Liverpool UK
100857	Product Security Engineer Server
126566	Band 6 Locum Physiotherapist London
136423	Program Manager Growth Operations Contract
156188	Director Product Management Endpoint Policy and Configuration Management
4200	French Audio Evaluations Specialist Freelance AI Trainer Project
107009	Growth Lead Subscription Products
81133	Advisor for Healthcare Delivery
27724	Care Manager Disability Behavioral Support
148327	Design Program Manager Research Operations
25324	BIBIBOP Team Leader Rookwood
65180	Business Process Analyst 752
22588	Occupational Therapist OT Home Health
13314	Events and Ministry Coordinator Offsite Part Time
126141	Band 5 Paediatric Nurse Mansfield
6416	Lead Mechanical Engineer
70066	Personal Care Specialist Full Time
89768	Center Based Registered Behavior Technician RBT
5881	Performance Marketing Manager
108896	Staff Engineer R&D Efficiency Platform
151080	Personal Trainer
50758	Site Safety Manager
175580	AV Business Director 12 month FTC Betfred
137066	Staff Engineer Server
42472	FP&A Rocket Growth
18816	Business Analyst SharePoint Implementation
22596	Occupational Therapist OT Home Health Visits
107007	Ecommerce Executive
8843	Software Integration and Test Engineer
109074	Material Handler 1 2nd shift
13862	Channel Manager
118049	Customer Service Representative
145163	Starlink Customer Success Specialist
51819	Surgical Coordinator
136566	Devops Team Lead
55051	Fitness Manager
141744	Member Relations
87316	Substance Use Disorder Therapist Outpatient Hybrid Virtual
60925	Assistant Buyer Women s
106928	MES Controls 2
176724	IT Systems Engineer
16656	Director Corporate Development
23912	Part Time Tele Hospitalist KS LA TX or Active IMLC
139311	Manager Operations Continuous Improvement
99937	OHS Specialist I
157790	Associate Attorney
100371	Head of People
139332	Driver
165362	Customer Service Advisor Card Payment Start Date 10 5 26
59723	Formation Process Equipment Technician
113539	Aide à domicile
79490	Client Success Representative Remote within APAC region Contract
134969	Sales Development Manager
25267	BIBIBOP Operations Leader Oak Brook
29819	Chief Product Officer
125489	Licensed Telehealth Therapist Mental Health Counselor New Hampshire
148984	Retirement Plan Recordkeeping Associate RSG
177569	Sales Coordinator Bilingual English German
172326	Radar Lead
153693	Personal Trainer
113939	Auxiliaire de vie
166010	Director Company Strategy Operations
156847	Technischer Facility Manager
128457	Technician Catheter Engineering
95652	Material Manager Bilingual
110129	Family Nurse Practitioner or Physician Assistant Sign on Bonus available
37068	Manager HRBP NORAM
50595	Service Reliability Engineer
25314	BIBIBOP Team Leader Liberty Center
166864	Mechanical Design Engineer Avionics
60880	Property Manager Dallas
26626	Director Environment Permitting
35801	Labor Employment practice
158414	Regional Activation Manager West
29987	Part time Physical Therapist
147812	Executivo a Comercial Externo Eusebio CE
174047	Trial Attorney
7822	Seasonal Operations Associate Part Time Kierland Commons
50156	Shift Lead North Valley
95346	Specialist Cruise Line Program
178450	Guidance Navigation Controls GNC Engineer V VI
28543	Account Executive Enterprise Retail
17558	MGSE Engineer
106556	Project Manager Support
42873	Baby Imaging Specialist
39682	Deskside Support On site in Waltham MA
21165	Event Sales Representative Part Time
30214	Cientista de Dados Sênior
64889	Research Associate Product Development
73869	Urgent Hiring In Home Support Needed Deer Park IL 60010
102077	Sales Consultant Residential
170133	VP Client Services
136803	Account Manager Material Science Japan
32199	Associate Director Study Feasibility Clinical Partnerships
41987	Executive Assistant
108926	Specialist Customer Due Diligence Operations KYB
20526	2027 Graduate Architecture Part II
36245	Cision Software Developer Software Engineer AMER Canada
46753	Software Engineer C#
69728	Tech Director Post Silicon Validation
167260	Inside Sales Associate
2760	Medical Scribing
138240	Event Operations Organiser
153700	Personal Trainer
138020	Strategic Growth Manager Rhode Island
136089	Production 2 11am 7 30pm
48992	NX Administrator
9317	Component Affairs Internship 2026 fall term
129454	FPGA Engineer
57559	Heavy Equipment Shop Technician Mechanic
37031	Data Engineer
143419	Facilities Supervisor Starlink
18312	Director Land Development
49818	Associate Dasher Logistics Dasher Financial Strategy Operations
73947	Hiring Caregivers for Seniors in University Place Washington 98465
4152	Dutch Suriname Caribbean Language Specialist Freelance AI Trainer Project
3059	Manufacturing Maintenance Supervisor
169857	Field Manager
76693	Valuations Analyst
52596	Expanded Functions Dental Assistant EFDA
81717	PhD Quantitative Research Internship
87830	Licensed Veterinary Technician LVT All Pets Animal Hospital
6006	Associate Manager Transactions
51377	Front Office Python Developer
48583	Software Engineer Identity Access Management
178461	Mechanical Engineer III IV
75376	Join the Honor Talent Community
83792	Fire Protection Design Engineer
139053	Manager of Strategic Operations
111678	RBT Registered Behavioral Therapist Brooklyn NY
150930	Personal Trainer
171332	Fire Alarm Low Voltage Technician Trainee
14907	Commissioning Manager Data Centers
26080	Pharmacy Partner Coordinator
101213	AI Artist
53546	Virtual Supervising Physician Telehealth Remote
174359	Merchant Services Associate
116375	Campbell Barista
4988	Software Tester Engineer Campinas SP
84693	Auxiliaire de vie
64460	Project Engineer I
139637	Software Engineer New Grad Program
165663	SDR
85012	VP Health Plan Health System TPA Client Services
158320	Director Digital and Engagement Strategy Pharma
55609	Kids Club Associate
178871	Partner Business Manager
86102	Director Customer Marketing Commercial
110965	Seasonal Store Advisor Woodbury
115079	OFAC Sanctions Screening Analyst II
149472	Commercial Terrain Indépendant Freelance
139389	Business Development Representative
1688	Data Conversion Developer
22030	Home Health Visits Registered Nurse
150798	Personal Trainer
23917	Part Time Tele RN Case Manager Multi State
28035	FPA Manager
61481	Executive Editor
45574	Strategic Enterprise AE Hunter Telco
76956	Conference Coordinator
139528	PlayStationオンラインサービス クライアントSDK ソフトウェア開発エンジニア
175480	Associate Direction Activation
161056	Product Sales Executive Banking Payments
34246	Registered Behavior Technician
67307	Proposal Specialist
18879	Governance Risk and Compliance Specialist
152193	Personal Trainer
1408	Director of Academics
122287	Australia Residents Survey Participants Brisbane
131881	Sales Development Representative France
118664	IS Administrative Assistant
8075	Seasonal Sales Associate Part Time The Grove at Shrewsbury
80036	Manager Deal desk Global Revenue Services
70908	Analyst EMEA Medtech Healthcare Services Private Markets
176706	Production Coordinator Logistics
6892	Agile Engineer Cloud Expert 904
178816	Sales Account Executive Enterprise
4346	Kansai Dialect Specialist Freelance AI Trainer Project
164876	Event Operations Manager
51447	Doctor s Assistant Paid Training
85004	Technical QA Analyst II
115367	労務担当
29115	Underwriting Analyst
107083	Lifestyle Manager
161110	Business Manager Portugal Responsável Administrativo e Financeiro PART TIME
23700	Substitute Group Fitness Instructor for Seniors
161066	Security Engineer 3 Vulnerability Management
982	Global Manager Category Management
10315	Director Manufacturing Engineering
165838	Quality Specialist Energetics
76584	SEO Strategist Consultant
149466	Commercial e Terrain Indépendant e
88489	Survey Technician
169228	Veterinary Sonographer San Jose CA
140333	Director of Operations
136406	ML Research Engineer ML Systems
1387	Daily Substitute Teacher
178848	Employee Relations Manager
51088	Business Developer
160260	Business Development Director HoldCo
107283	Program Manager Onsite Data Center Deployment
41816	쿠팡풀필먼트서비스 HVAC 냉난방공조 설계 엔지니어
42562	Global Mobility Specialist Global Business Travel
130119	Art Director
30989	Site Reliability Gitops Engineer
162240	Cloud Transformation
93235	Learning Operations Analyst
84615	Aide à domicile
97375	Associate Paid Search and Social
93330	Associate Service Technician Coolidge
99431	Partner Manager Central
95342	Director Chapter Revenue Advisor
70710	Systems Engineer
127631	Haematology Blood Tranfusion BMS Liverpool
73420	Dialysis Technician Thousand Oaks CA 91359
156428	Data Center Deployment Technician I
90265	GTM Engineer
107238	Immigration Mobility Manager
140565	Physician
61121	Financial Analyst 12 month FTC
34592	Registered Behavior Technician RBT
65211	General Submission with Freedom 76
14898	Business Development Manager
83124	Vendeur CDD temps plein
64646	3D Print Optimization Engineer
13865	Chargé e de projet adjoint e
166471	L3 Support Analyst
5417	Marketing Analyst Rocket Travel by Agoda
50359	Threat Detection Engineer
69559	Sales Executive
152322	Personal Trainer
124631	Product UX Professionals AI Training Cambridge UK
66552	Director Analytical Development
104765	Technical Project Manager Hardware Automation
104299	Director Digital Insights
6384	Scientist Assay Development China
51505	Glaucoma Surgeon Wichita KS
25626	Associate Listings Ecosystem Development
56548	Service Associate Night
66885	Sports Data Collector Football Lyon France
70791	Marketing AI Builder
60578	Product Manager Product Creation
100968	Staff Engineer Observability
70442	Customer Support Quality Specialist
131807	Lifecycle Specialist Contract Management LATAM
71669	Enterprise Account Executive Northeast
115045	Administrative Assistant II
157278	Programming Asst
90123	Scientist II Melt Polymer Scientist
157223	Photojournalist
107156	Commercial Manager Commercial Manager
63197	Account Executive Enterprise
77448	Registered Financial Advisor San Francisco
157907	Contract Administrator
155990	Connecticut Waitlist Contract Psychiatric Nurse Practitioner
49267	Total Rewards Leader
32225	Manager Accounting
11874	System Safety Engineer
102632	Bartender
41904	Program Management 로켓그로스 정책 전략 총괄
33298	Talent Acquisition Partner
81160	Subject Matter Expert
128354	Specialty Dr Old age inpatients North West England
25023	Project Manager
113040	Care Coordinator
163523	Business Development Air Force
149624	Fullstack Engineer Bank
106994	Manager Pension Accounting Contract Review On Site
80713	TECH COORDINATOR
107254	Mechanical Project Manager Glomfjord Norway
130998	Solution Architect Enterprise
49263	Structural Engineer Intermediate
117596	Manager Partnership Services Shocker Sports Properties
83054	Vendeur CDD temps partiel
35546	Manufacturing Engineer Radar Systems
32903	Analyst Institutional Portfolio Operations
74483	Vice President Integrated Marketing
49284	CRO Executive
2786	Assistant Psychedelic Dosing Session Monitor
116967	Product Manager II Search Experience
27380	Psychiatrist MD
149413	Agente di commercio Procacciatore P IVA Settore pagamenti
94789	Home Infusion Nurse
50012	Regional Merchant Lead
4524	Nahuatl Language Expert Freelance AI Trainer Project
59672	Director of Product Delivery and Fulfillment Intelligence Remote
87652	Manager GMS Games Services Apps
41595	쿠팡풀필먼트서비스 물류센터 운영 및 공정 관리자 용인 1센터
14275	Manager Business Development
77560	Data Annotation Contributor
177723	Technical SME
146106	Veterinary Nurse Manager
10506	GNC Engineer Space
57479	Heavy Equipment Field Technician Mechanic
16403	Closing Specialist
82579	Trading Desk Operations Engineer
124147	Mental Health Professionals AI Training Dublin Ireland
69566	Component Engineer
73889	Urgently Hiring Helpers for Seniors in Wilmette Illinois 60091 Apply Today
174713	Software Engineer
139168	Service Center Processor
1238	Director GCP Quality Assurance
76155	Manager Transaction Management
13149	Startup Partnerships France Southern Europe
61727	Scenic Head of Department Freelance
157206	News Director
4254	German Language Specialist Freelance AI Trainer Project
145575	Dental Assistant
24959	Enterprise Account Manager East Region Saudi National
108205	Biotech Program Manager
151713	Personal Trainer
155910	Product Lead Source to Pay
142090	Spatial Design Lead
175450	Analista de Mídia Sênior
148891	Territory Sales Manager San Antonio TX
51715	Optometric Technician
56444	Service Associate Night
23298	Registered Nurse RN Hospice Visits
115043	Account Executive Structured Finance
30181	Analista de Gestão de Acessos Sênior IAM
34099	Child Autism Specialist We Train You
28284	Area Manager
161015	Commercial Services UK Country Commercial Lead
32437	Legal Assistant
78309	Analyst Financial Advisory Entry Level New York and San Francisco
176780	AI Scientist BioMedical AI
55043	Fitness Manager
102722	Virtual Test Engineer
73103	Staplerfahrer
61366	Biz Ops
86217	Supply Chain Manager
27854	Vice President Distribution
92571	Project Engineer
94716	Project Manager
90506	Family Court Adviser
166466	Ingénieur Lead en Déploiement Avancé
127009	Band 7 Locum Obstetric Sonographer London
70460	Payment Operations Manager
116730	C Engineer
159822	Assistant General Manager
174711	Proposal Manager
149172	Growth Marketing Campaign Manager
177509	Sales Trader Mesa BMF
24007	Director Clinical Supplies Logistics
25248	BIBIBOP Operations Leader Athens OH
106616	Software Engineer PHP
86508	Delivery Management Coordinator
135914	Workplace Project Manager Project Management Office Workplace Solutions Group
70209	Art Director
137146	CAE Engineer Energy Consumption
110622	Service Desk Analyst II
111382	ABA Paraprofessional RBT Queens NY
67453	Health Insurance Sales Representative
143222	Dangerous Goods Lead
115059	Director Portfolio Alternative Energy Structured Finance
70101	Personal Care Specialist Part Time
54206	Assistant General Manager
141082	Solutions Consultant West
110123	Family Nurse Practitioner or Physician Assistant Domain Northside
160425	Head of Regulatory Compliance Quality
61248	Nursery Room Leader
135092	Hotel Manager
137966	Staff Technical Account Manager
203	Office Admin
62657	Early Childhood Teacher Build Your Career in ABA
124106	Material Sciences Graduates AI Training Edinburgh UK
28572	Account Manager Emerging Enterprise
13777	Language Coordinator Open Application
18777	Charter Program Manager
55692	Kids Club Manager
40766	Targeting Analyst
11911	Technical Program Manager ArsenalOS
68156	HR Business Partner
93875	On Call Registered Nurse
42611	Specialist Workforce Planning Optimization Hub Operations
40352	Customer Experience Supervisor
160698	Associate Intern Bahasa Vietnamese Speaker
159664	Produktentwickler gn Value Improvement Projects
1845	Logistics Coordinator
162566	Marketing Analyst Intern
39910	Audio Visual Supervisor Live Events
122433	Canada Residents AI Trainers Canada
144969	RFIC Design Engineer RFIC Engineering
15201	Project Manager Construction
47227	Social Media Manager
87054	Channel Sales Engineer Position located in Leeds United Kingdom
7432	Operations Lead The Grove
31199	Business Analyst Custody Trade Processing
121313	Manufacturing Cell Processing Specialist 1
100309	Strategic Partnerships Manager Travel
44533	Executive Assistant
109934	Full Stack Engineer Web3
95170	Manager Itemized Bill Review Healthcare
176003	People Operations Analytics
5072	Mechatronics Test Engineer
13256	DevOps Engineer
108149	Consumer PR Business Director
109937	Security Engineer Product Security
18535	Experienced Trader Hong Kong or New York
79833	Team Lead Learning and Development Contract Remote
97614	Software Engineer Regulatory Products
125515	Remote Clinical Psychologist Michigan
177383	Assessoria de Investimentos Início de Carreira
65229	Lead Test Engineer 85
110604	Urologist New York Health
5087	Sales Engineer Industrial Automation
162798	Hazardous Waste Operations Supervisor 2nd Shift 1 30PM 10 00
175459	Analytics Manager GOC
161936	Product Manager Agentic AI
32055	Data Engineer
30552	Behavior Technician RBT
146036	IC1 Software Engineer Backend
154717	Personal Trainer Cumming GA
18498	Associate Private Debt Finance
89701	Director of Product Subscriber Value
115200	Producer
173073	Associate Veterinarian Pine Castle Animal Care Center
117980	Customer Service Representative
168826	Emergency Veterinary Assistant Dallas TX
57191	District Project Manager
82957	Stage assistant juridique droit social
119235	Reporting Analyst CPG
39515	Nurse Practitioner NP Pool PRN
92145	Project Manager Zeal
132351	Entrepreneur In Residence MBA Graduate
18252	Material Scientist
115427	データサイエンティスト リーダー候補 ビジネスグロース領域
118931	Registered Dietitian RD TX License
2952	Customer Success Manager
89531	Sales Operations Manager Smart Factory Solutions Sales
82809	Stock Associate Full Time Arese C.C Il Centro
64026	Software Engineer Motor Controls
22897	Physical Therapy Assistant Home Health
53622	Electrical Engineer Board Designer
121095	Data Engineer Data Warehouse
135518	Media Director
10293	Development Test Engineer
86795	Creative Strategist
172356	Shift Lead
24952	Business Development Representative
168070	Director Customer Success
147515	Executivo a Comercial Hunter para Grandes Contas Feira de Santana BA
88480	Staff Scientist Natural Resources
39715	AIT Technician
27344	Psychiatrist MD
80017	Associate Product Manager MSD Financial Systems and Transformation
70154	HR Generalist
90254	Test multiple select checkbox questions
123748	IT Technology Decision Makers Paid ITSM ESM Research Study Oklahoma City US
51147	FX Sales Netting Solutions
114652	Solution Partners Manager
41888	Financial Planning and Analysis
53267	End User Services Engineer
20942	Reliability Engineer
69206	Cell Line Production Associate 2nd Shift
118693	Network Support Engineer
96903	Optometric Technician
32952	Analyst Institutional Portfolio Operations
124277	New Zealand Residents Survey Participants Nelson New Zealand
39979	EMT B Urgent Care
136287	Design Build Project Manager
129005	Account Management Sales Leader
90219	Easy Apply Test
147495	Executivo a Comercial Externo Uberaba MG
88521	Ingénieur CPI en structure
88671	Mold Setter I
124694	Psychology Graduates AI Training Cambridge UK
116801	Software Engineer
105592	Software Engineer Pathology and Digital Imaging
77791	Production Operator
73390	Compassionate Caregiver Needed West Knoxville TN 37932
159642	Produktionsmitarbeiter gn
80904	Full Time Floor Leader Mayfair Shopping Centre
148840	Nurse Practitioner
160634	Solution Consultant Bilingual Spanish
9726	Associate Director Pricing and Access Analytics
42309	CFS Automation Maintenance Specialist L4 SAN3
47083	Group Controller
96375	NCDOT Roadway Design Lead
2102	Service Level Specialist
91956	Support Worker or Support Worker
56937	Account Executive Bailiwick Req#1206
172025	Business Intelligence Analyst Growth
94557	Seasonal Ambassador Menlo Park Mall
165868	Program Manager In Space
110580	Palliative Care Physician Astera Cancer Care
85917	Sports Operations Engineer
169971	Transit Ambassador
106496	Manager Market and Industry Solutions
45969	Security Sales Engineer
33175	Head of Value Engineering Nordics
20262	Temporary Facilities Assistant Immediate Need
2267	Production Support Engineer
145425	Welder Starship Launch Pad
86658	Creative Director Design
11496	Material Flow Engineer General
171581	Court Security Officer Hot Springs AR
65158	Verification Engineer
141750	Membership Manager Soho House Los Cabos
74954	Quality Coordinator
5071	Mechatronics Technician
36973	Regulatory Affairs Associate Consultant PLM
77465	Venture Executive St Louis
83602	Part Time Verkoopmedewerker JD Tilburg
40346	Culinary Operations Supervisor
176042	Planner Print Audio Cinema Mensch
23898	Director of Market Development Tucson Arizona
118355	Shift Supervisor
53302	Mechanical Engineer Building Systems
76774	Early Career HVAC Plumbing
60516	Retail Lead Nashville Soccer Club Team Store
148832	Lead Analyst Risk Adjustment Analytics
123806	Javascript Developers AI Training Birmingham UK
56585	Stretch Manager
8745	Brake Validation Engineer
92655	Skilled Laborer
40212	Radiologic Technologist Rad Tech
146180	Account Executive
54873	Fitness Counselor
129056	AI Forward Deployed Engineer
67328	Segment Product Marketing Manager Field Service
113541	Aide à domicile
75966	Dir Solution Development
30884	Linux Desktop Support Engineer London UK office
86956	Quality Control Specialist
16385	Tax
114926	Product Support Engineer 1 Vigo Spain
19817	Incident Response Manager Public Safety
131226	Client Services Manager
158542	CRNA Bountiful UT
99594	Vice President Drug Safety and Pharmacovigilance
62982	Point of Care Growth and Partnerships Associate
129634	Staff Data Analyst Marketing
31549	PM PMO Banking Financial services Domain
132205	Maintenance Technician West Loop
103620	Account Executive
138877	Manager Strategic Programs
109661	Electrical Engineer Data Centers
59658	Regional Sales Manager Financial Services
37303	National Sales Director Major and Federal Accounts
109121	Production Supervisor Night Shift
106632	Staff Product Manager
38133	Lead Claims Auditor QC
40585	Region Manager Mississippi Region
72792	Editorial Content Manager CDD
110277	Phlebotomist Administrative Assistant Trainee
83856	Fire Protection Engineer Professional Engineer
76417	Real Estate Acquisition Consultant
23533	RN Registered Nurse
55517	Kids Club Associate
169719	ER and Critical Care LVT
38946	Neuroscience Therapeutic Sales Specialist Charlotte NC
30811	Embedded Linux Field Engineer Mandarin speaking
100193	Dental Anesthesiologist
9973	GTM Analytics Manager
48359	Line Cook Suburban Square
43312	Customer Care Specialist
27170	Psychiatrist MD
109393	Civil Engineering Internship Site Design
104953	Delivery Portfolio Manager
171750	AI Sales Director East
55535	Kids Club Associate
78436	Analista de Negócios Pleno BizDev Vaga Afirmativa para Pessoas Negras
144591	Sourcing Specialist Supply Chain Launch Site
2038	SAP Warehouse Management Manager
153442	Personal Trainer
44326	Retail Sales Associate Part Time
38520	Colonial Animal Hospital Veterinary Technician
60278	Guillotine Operator
57076	Platform Engineer
5221	Analyst Pricing Bangkok Based relocation provided
56214	Service Associate
46495	Debut Internship Talent Pool
25124	HOUSTON Survey Internship
159371	Physical Occupational Therapist Consultant
13881	Hyperscaller Product Sales Specialist Cloud Ecosystem Solutions
23393	Residency Program New Nurse Graduates
21320	Administrative Coordinator
89846	Account Executive UK
2224	IT Administrator
45442	Staff Designated Support Engineer
113835	Auxiliaire de vie
35576	Radar Software Engineer
165745	Strategic Growth Manager
52443	Program Manager YMCI
28771	Site Reliability Engineer I
151140	Personal Trainer
48761	Research Analyst 12 Month Fixed Term Employee
104528	Electrical Design Engineer Data Centers
12705	Data Center Energy Lead EMEA
22444	LPN Weekly Pay Home Care Nurse
143321	Electrician Facilities Multiple Shifts
3928	Information System Security Engineer ISSE
145700	Software Engineer Platform Almaty Kazakhstan
33389	IT
120691	Investor Relations Strategy Analyst Polish speaking
40699	Collection Operations Manager
130468	Community Manager French Speaking Contract
108080	Operations Manager Office Manager
95912	Real Estate Agent
88535	Ingénieur.e en sécurité des machines
45221	Executive Assistant
165077	SDR at United Media
86239	BAS Install Lead
113000	Associate Process Optimization
37503	Revenue Operations Support Manager Singapore
138998	Ingénieur Logiciel en Autonomie Autonomy Software Engineer
163924	Test Engineer
135579	Associate Specialist Sales Engineer
163153	Integrated Media Strategist
128478	Account Executive Emerging SocialInfra accounts Japan
167116	Technical Architect Product Management AWS Azure
24633	Licensed Clinical Mental Health Counselor Remote
98548	Billing Coordinator
5185	Analista Fiscal I Telefone
80467	Lead Business Analyst Customer Integrations
77780	Food Program Manager
60775	Workspace Assistant
80091	Applied Research Scientist LLM Evaluation Post Training
5191	2026 Support Associate Korean&English Seoul
88180	On Call Archaeological Cultural Resource Technician
49507	Account Executive Attribute North America
1859	MS Dynamics Developer
160853	AMS USA DAMO SystemsSupportEngineer Lead RtH
70016	Leader in Training
13580	Landscape Technician Stormwater
627	Server 550 Madison
121344	Human Data Quality Engineer Founding Team
87186	Staff Applied Scientist Metalab
144784	GNC Engineer Starship Controls
83622	Store Colleague JD Rotterdam Lijnbaan 8 12H
32610	Retail Designer
148005	User Experience Researcher
66585	Data Engineering Manager
107549	Vice President Vice President Global Development Leader
40552	Clinical Sales Specialist CS Seattle WA
106742	Quality Assurance Inspection Specialist
170164	Duty Manager
134298	Supplier Quality Engineer I II I II PCBA
158598	Gastroenterology Physician Flowood MS
62571	Peer Recovery Supporter CORE Guide
76608	SEO Strategist Consultant
153679	Personal Trainer
29550	Credit Risk Role
141248	Account Executive
105084	Embedded Linux Engineer
170201	Kitchen Team Member
119729	Conference Operations Coordinator
150447	Personal Trainer
93069	Mechanical Commissioning Field Trainer
10607	Lead IT Hardware Procurement
78989	Field Applications Engineer
132689	Sales Account Executive Queens NY
49805	Account Manager SMB
115263	Business Development Representative Mandarin Speaker
175938	Manager Talent Acquisition Emerging Talent
82602	Meet us at SRECon EMEA 2026
172789	Babysitting Opportunities in Liberty Township OH
131454	Mechanical Engineer Mechanisms
54309	Assistant General Manager
175836	Manager Analytics Global Clients
147070	Analyst Research Investments Private Equity
172185	Staff Tech Lead Manager Machine Learning Simulator Evaluation
112096	Manager Revenue Strategy Operations Hybrid
113533	Aide à domicile
125124	UK Residents Survey Participants Cardiff
33346	Distributed Systems Engineer Data Platform
64043	Software Engineer Hardware Test
170282	SI Swim Instructor
97876	Director Communications
142530	Associate Director Label Analytics Global Digital Business
120737	Investor Relations Swedish speaking
73963	Hiring Task Based Helpers for Seniors in Felton CA 95018
88623	Détentrice de clé
209	Producer
114872	Lead Agent Architect
67716	Validation Engineer
108455	Fullstack Engineer Node.js Heavy React
3604	Technician 3D Printing
127238	CBT Therapist Dorset
25546	Computational Biologist Immune Cell Repolarization
42812	Greater Savannah Residential Installation Technician
122898	Data Entry Clerk Graduates AI Training Cambridge UK
5216	Account Specialist Turkish Speaking MEA Cairo based
57757	Rental Coordinator
12692	Customer Success Manager DACH
80047	SDE III Devops
149850	Product Manager Email SMS
9947	Sales Analyst
17071	Assessoria de Investimentos XP Future
34356	Registered Behavior Technician RBT
11093	Quality Specialist Maritime
5956	Registered Nurse RN
56579	Stretch Manager
108836	Growth Manager
125103	Sweden Residents Survey Participants Umeå Sweden
57209	Field Technician Mechanic
78384	SOC Analyst
131164	Call Centre Manager
50697	DevOps Engineer
60034	Design Assistant
122558	Canada Residents Survey Participants Varennes Canada
69829	Director Growth Operations
163189	Customer Relations Specialist
35710	Service Advisor
87531	Operator
87544	Grants Specialist
129956	Product Manager II Ads
56808	QSC Q Sys Operations Remote Support Engineer APAC
141371	Independent Sales Representative
169494	Veterinarian
29494	Director of Field Operations Midwest
137480	Associate Director Construction Delay
174930	Physical Therapist
79213	Onsite Supervisor
134701	Business Development Representative French Speaking
140028	Clinical Provider CLC EVENING SCHEDULE
95299	Backend Engineer
142276	Medical Assistant
6867	Office Admin
29961	User Operations Expert
153467	Personal Trainer
111303	ABA Paraprofessional RBT Forest Hills NY
85408	Account Executive In Territory Los Angeles CA
44001	Staff Backend Engineer
105035	Engineer Sensors
168123	Project Coordinator Asset Management
99189	Services Coordinator I Summer Oaks
70311	Architekt in Technische Detailentwicklung
126965	Band 7 Locum First Contact Physiotherapist Practitioner Sutton
91998	Team Manager Referral and Assessment
119398	Substation Wireman
160364	Affiliate Marketing Manager
57431	Heavy Equipment Field Technician Mechanic
125463	Access Manager Pharmacy Technician CPhT License Req d VOB PAs
83585	Full Time Sales Assistant JD Tilburg
152596	Personal Trainer
152079	Personal Trainer
19930	Software Technical Account Manager Albuquerque
108366	Global Paid Search Marketing Manager
158592	Gastroenterology Physician Cedar Park TX
58409	CRNA HCA Gramercy Outpatient Surgery Center Houston TX Non Call No Nights Wknds
14171	Sales Business Development Manager Advertising Solutions
138283	Global Internship Program Scandinavian speaker SetSales
25599	Operations Partner Edmonton Contract
160798	Staff Safety Engineer
114268	Licensing Specialist
136797	Tax Manager
122888	Data Entry Clerk Graduates AI Training Acapulco Mexico
102136	Case Manager
7852	Seasonal Operations Associate Part Time Penn Square Mall
17643	Structures Mechanic
24550	Engineering Tech Lead Player Management
163374	Security Supervisor
39129	Safety Specialist Construction
142518	A&R Manager
89113	Office Support Assistant
84736	Auxiliaire de vie
29005	Manager Sales Development
153477	Personal Trainer
142607	Manager Song Virality Marketing Epic
114722	Strategic Support Specialist
77867	Oracle EBS Projects Functional Lead Federal Financials G Invoicing
19226	Praxismanager für Allgemeinmedizin
51855	Client Service Associate
89074	QA Shop Floor Specialist II
130132	Director Resource Management
134832	Product Designer
16693	Engineer I Field Process
109160	Warehouse Order Puller CRL
43194	Staff Software Engineer Backend Iasi
156287	Fraud Analyst
39577	RISE Recovery Support Specialist M F 12p 9p
162524	Greenmark Waste Solutions Business Development Representative Account Executive
146296	Registered Nurse RN
110725	Staff Solution Architect
91331	Social Worker Child Protection
82801	Stage Sales Assistant Roma C.C Roma Est
115087	Software Engineer AWS Group
74738	Dynamics 365 F&O Functional Admin
176032	Pessoa Estagiária de People
109571	NDT Level II Radiographer
89192	Radar Software Engineer
162613	Software Engineer II
81629	Learning Development Specialist
19386	Full Stack Engineer
149529	Engineering Manager Verification
7625	Sales Associate Part Time Regent Street
128364	Specialty Dr Required Eating Disorders North East England
157119	Evening Anchor
58214	Primary Therapist Adult ERC
13843	Program Manager
173349	Veterinarian Saint Francis Animal Hospital
54402	Assistant Kids Club Manager
104534	Field CTO Media Entertainment AI Infrastructure
146693	Account Executive Mid Market Sales Public Sector
42045	Product Manager Rocket Growth
47570	Commercial Entry Door Technician Metro Atlanta
44250	Retail Sales Associate Full Time
179014	Transformation Architect Enterprise
122946	Data Entry Clerk Graduates AI Training Rotorua New Zealand
47313	Inside Partner Manager US Market
154132	Personal Trainer
121366	Accountants AI Training Albuquerque US
134384	Cheat Software Engineer
170117	Solutions Architect AI Systems
53663	Manager Partner Relations Tours Sun Sand
146667	Retail Sales Lead Water Tower Place
49964	Manager B2B Retention DoorDash for Business
98756	HSE Coordinator Micon Group Inc
65632	Product Manager Core Products Platform
58658	Global Workforce Management WFM Specialist
78285	Electrical Engineer I
105221	Product Operations Specialist Singapore
131235	Vocational Employment Specialist Next Chapter
45914	Creative Operations Production Manager
162797	Hazardous Waste Operations Supervisor 1st Shift 5AM 1 30PM
56427	Service Associate Night
14514	Python Developer Python Developer
171994	Product Manager Fleet Management Tools
161823	Server
117224	Product Manager Mission Solutions
24913	Bilingual Client Retention Specialist
36361	Associate GP UC Alexandria King Street
162186	Production Assembler II
99987	District Sales Manager
150004	Strategic People Partner
68445	HIL Test Engineer System Test
122346	Biology Graduates AI Training Charlotte US
90316	Adults Occupational Therapist Home First
43149	Program Director
240	Manager International GTM NBA 2K
175995	Paid Social Director FTC
119563	Non Registered Histotechnician
2798	Enrollment Clinician
143558	Government Project Engineer
170260	Personal Trainer
18855	Demo Engineer
158068	Branch Manager
27054	Psychiatric Mental Health Nurse Practitioner PMHNP
117383	Facilities Assistant
145196	Starlink Specialist Tool and Die Machining 2nd Shift
177151	Account Executive Enterprise
73313	Caregiver Needed Support for a Client Lancaster Pennsylvania 17603
100290	FP&A Manager
78271	Mechanical Engineer II Defense R&D Programs
45721	Enterprise Sales Engineer
28744	Partner Account Director Technology Partnerships AWS
121309	Vice President of Engineering
31875	frog part of Capgemini Invent Strategy Intern
113723	Assistant de vie
158413	Regional Activation Manager Southeast
179078	Analytics Engineer
39935	L1 Lighting Engineer Live Events part time
31292	Data Architecture Governance Consultant GCP BigQuery Polish is mandatory
112940	Associate Actuarial
35408	Enterprise Sales Engineer Singapore
24067	Machine Learning Engineer
118542	Customer Success Manager
109672	Mechanical Engineer Building Design
36324	Organizational Development Specialist
155330	Associate Managing Director II
167507	Sales Manager Korea
148980	Retirement Plan Administrator
159344	YouTube Channel Strategy Manager
50238	Strategic Partner Manager II
81424	AI Sales Strategy Manager
24730	Licensed Professional Counselor Remote
49450	Stage Analyste performance commerciale janvier 2027
58561	CX Product Operations Analyst
48642	Technical Program Manager Physical Infrastructure
98291	Director of Lifecycle Marketing
36599	Program Manager
62279	Security Engineer Application Security
172012	Research Scientist Map Scalability
73426	Dialysis Technician Thousand Oaks CA 91359
155645	Software Reverse Engineer
167552	Product Management Director Top Level Domains
52639	Onboarding Manager
140399	Malware Analyst Media Malware and Analysis MMA MD TS SCI CI POLY
37801	Project Operations Coordinator
150010	Technical Compliance Analyst
41586	사내 통번역사 한영 계약직
78460	Executivo a de Contas Estratégicas Farmer Vaga Afirmativa para Pessoas com Deficiência PCD
31457	Java Developer medior FinTech Spring
50736	Shopper Insights Client Lead
89035	Network Security Engineer
164148	AI driven QA Engineer
89681	Lead DevEx Engineer
21694	Direct Care Worker
169034	Hospital Manager Fort Myers FL
117718	Assistant Store Manager
46179	Area Health Information Specialist I
53658	Environmental Health Safety EHS Specialist
107310	Content Editor
82035	IT Technician I Remote
21299	1 1 RN Home Care
141713	Kitchen Porter Little House Balham
145412	Turbomachinery Engineer Raptor
138266	Executive Producer at SetSales
149077	Store Supervisor Part Time
121096	Developer 2nd Level Support
83741	Associate Fire Engineering
58172	Clinical Dietitian
159082	News Assistant Data
457	Dental Hygienist
81518	Capture and Proposal Specialist
158044	Mechanical Project Manager Quality Assurance
157342	Visual Journalist&Storyteller
19845	Mechanical Design Engineer Simulations
117807	Assistant Store Manager
62539	Site Manager
119572	Project Director
66523	Summer 2027 Internship Software Engineering
33843	Behavior Technician
32040	Software Engineer
70277	Freelance Frontend Engineer all genders
68063	Chief of Staff Dan Brown
153710	Personal Trainer
10499	Global Sourcing Manager Connectors Wire Harness
66690	Sports Data Collector American Football Fresno California USA
73675	In Home Staff Assist Thousand Oaks CA 91360
84556	Electrical Technician
85694	HR Business Partner
```

## Predictions — do not read until step 3

What iteration 9's classifier answered for each of the rows above. Reading this before the labels
are written to disk destroys the measurement: the labeller would agree with it and the numbers would
decorate rather than measure.

```
167606	OUT
156808	OUT
84917	OUT
143576	OUT
162539	OUT
46129	UNKNOWN
68	OUT
174509	UNKNOWN
23372	OUT
46847	UNKNOWN
53791	OUT
156475	OUT
79405	UNKNOWN
123858	UNKNOWN
17349	OUT
39561	OUT
129654	UNKNOWN
176712	UNKNOWN
88989	OUT
151895	OUT
37345	UNKNOWN
49817	OUT
33719	OUT
83705	OUT
112067	OUT
9964	UNKNOWN
119865	UNKNOWN
80422	OUT
101881	IN
60912	UNKNOWN
107968	UNKNOWN
140841	UNKNOWN
90007	UNKNOWN
156257	IN
175014	UNKNOWN
37762	UNKNOWN
138293	UNKNOWN
35848	OUT
124614	OUT
92996	OUT
52564	OUT
143159	OUT
50241	OUT
74241	UNKNOWN
132455	OUT
92110	UNKNOWN
31221	UNKNOWN
141129	UNKNOWN
48705	UNKNOWN
2823	UNKNOWN
116353	OUT
154762	OUT
151603	OUT
15541	UNKNOWN
177233	IN
58762	UNKNOWN
143210	OUT
127	OUT
91544	OUT
84238	OUT
105723	UNKNOWN
41284	UNKNOWN
152557	OUT
31656	UNKNOWN
38205	OUT
102837	OUT
145549	UNKNOWN
84339	OUT
16318	UNKNOWN
86301	OUT
135181	UNKNOWN
12728	OUT
98171	UNKNOWN
135993	IN
110677	UNKNOWN
13936	IN
122364	UNKNOWN
100857	IN
126566	OUT
136423	UNKNOWN
156188	OUT
4200	OUT
107009	UNKNOWN
81133	UNKNOWN
27724	OUT
148327	UNKNOWN
25324	UNKNOWN
65180	UNKNOWN
22588	OUT
13314	OUT
126141	OUT
6416	OUT
70066	OUT
89768	OUT
5881	OUT
108896	IN
151080	OUT
50758	UNKNOWN
175580	UNKNOWN
137066	UNKNOWN
42472	UNKNOWN
18816	UNKNOWN
22596	OUT
107007	OUT
8843	IN
109074	OUT
13862	OUT
118049	OUT
145163	OUT
51819	OUT
136566	IN
55051	OUT
141744	UNKNOWN
87316	OUT
60925	OUT
106928	UNKNOWN
176724	IN
16656	UNKNOWN
23912	UNKNOWN
139311	UNKNOWN
99937	UNKNOWN
157790	OUT
100371	OUT
139332	OUT
165362	OUT
59723	OUT
113539	OUT
79490	OUT
134969	OUT
25267	UNKNOWN
29819	UNKNOWN
125489	OUT
148984	UNKNOWN
177569	OUT
172326	UNKNOWN
153693	OUT
113939	OUT
166010	OUT
156847	UNKNOWN
128457	OUT
95652	UNKNOWN
110129	OUT
37068	OUT
50595	UNKNOWN
25314	UNKNOWN
166864	OUT
60880	OUT
26626	UNKNOWN
35801	UNKNOWN
158414	UNKNOWN
29987	OUT
147812	OUT
174047	OUT
7822	OUT
50156	UNKNOWN
95346	UNKNOWN
178450	OUT
28543	OUT
17558	UNKNOWN
106556	UNKNOWN
42873	UNKNOWN
39682	UNKNOWN
21165	OUT
30214	UNKNOWN
64889	UNKNOWN
73869	UNKNOWN
102077	OUT
170133	OUT
136803	OUT
32199	OUT
41987	OUT
108926	UNKNOWN
20526	UNKNOWN
36245	IN
46753	IN
69728	UNKNOWN
167260	OUT
2760	UNKNOWN
138240	OUT
153700	OUT
138020	UNKNOWN
136089	UNKNOWN
48992	UNKNOWN
9317	UNKNOWN
129454	OUT
57559	OUT
37031	IN
143419	OUT
18312	UNKNOWN
49818	OUT
73947	OUT
4152	OUT
3059	OUT
169857	UNKNOWN
76693	UNKNOWN
52596	OUT
81717	IN
87830	OUT
6006	UNKNOWN
51377	IN
48583	IN
178461	OUT
75376	OUT
83792	UNKNOWN
139053	UNKNOWN
111678	OUT
150930	OUT
171332	OUT
14907	IN
26080	OUT
101213	OUT
53546	OUT
174359	OUT
116375	OUT
4988	IN
84693	OUT
64460	UNKNOWN
139637	IN
165663	OUT
85012	OUT
158320	OUT
55609	OUT
178871	UNKNOWN
86102	OUT
110965	OUT
115079	UNKNOWN
149472	UNKNOWN
139389	OUT
1688	IN
22030	OUT
150798	OUT
23917	OUT
28035	UNKNOWN
61481	OUT
45574	UNKNOWN
76956	OUT
139528	UNKNOWN
175480	UNKNOWN
161056	OUT
34246	OUT
67307	UNKNOWN
18879	OUT
152193	OUT
1408	UNKNOWN
122287	OUT
131881	OUT
118664	OUT
8075	OUT
80036	OUT
70908	UNKNOWN
176706	OUT
6892	IN
178816	OUT
4346	OUT
164876	OUT
51447	OUT
85004	IN
115367	UNKNOWN
29115	UNKNOWN
107083	UNKNOWN
161110	UNKNOWN
23700	OUT
161066	IN
982	UNKNOWN
10315	OUT
165838	OUT
76584	OUT
149466	UNKNOWN
88489	OUT
169228	OUT
140333	UNKNOWN
136406	IN
1387	OUT
178848	OUT
51088	UNKNOWN
160260	OUT
107283	OUT
41816	UNKNOWN
42562	UNKNOWN
130119	UNKNOWN
30989	IN
162240	UNKNOWN
93235	UNKNOWN
84615	OUT
97375	OUT
93330	UNKNOWN
99431	UNKNOWN
95342	UNKNOWN
70710	IN
127631	UNKNOWN
73420	OUT
156428	OUT
90265	UNKNOWN
107238	UNKNOWN
140565	OUT
61121	UNKNOWN
34592	OUT
65211	UNKNOWN
14898	OUT
83124	OUT
64646	OUT
13865	UNKNOWN
166471	UNKNOWN
5417	UNKNOWN
50359	IN
69559	OUT
152322	OUT
124631	OUT
66552	UNKNOWN
104765	OUT
104299	UNKNOWN
6384	UNKNOWN
51505	OUT
25626	UNKNOWN
56548	OUT
66885	OUT
70791	UNKNOWN
60578	UNKNOWN
100968	IN
70442	OUT
131807	UNKNOWN
71669	OUT
115045	OUT
157278	UNKNOWN
90123	UNKNOWN
157223	UNKNOWN
107156	OUT
63197	OUT
77448	OUT
157907	UNKNOWN
155990	OUT
49267	UNKNOWN
32225	OUT
11874	UNKNOWN
102632	OUT
41904	UNKNOWN
33298	OUT
81160	UNKNOWN
128354	UNKNOWN
25023	UNKNOWN
113040	OUT
163523	UNKNOWN
149624	IN
106994	OUT
80713	UNKNOWN
107254	OUT
130998	IN
49263	OUT
117596	UNKNOWN
83054	OUT
35546	IN
32903	UNKNOWN
74483	OUT
49284	OUT
2786	OUT
116967	UNKNOWN
27380	OUT
149413	OUT
94789	OUT
50012	OUT
4524	OUT
59672	OUT
87652	UNKNOWN
41595	UNKNOWN
14275	OUT
77560	UNKNOWN
177723	UNKNOWN
146106	OUT
10506	OUT
57479	OUT
16403	UNKNOWN
82579	UNKNOWN
124147	OUT
69566	UNKNOWN
73889	OUT
174713	IN
139168	UNKNOWN
1238	IN
76155	UNKNOWN
13149	UNKNOWN
61727	UNKNOWN
157206	UNKNOWN
4254	OUT
145575	OUT
24959	OUT
108205	UNKNOWN
151713	OUT
155910	UNKNOWN
142090	UNKNOWN
175450	UNKNOWN
148891	OUT
51715	OUT
56444	OUT
23298	OUT
115043	OUT
30181	UNKNOWN
34099	OUT
28284	UNKNOWN
161015	OUT
32437	OUT
78309	UNKNOWN
176780	OUT
55043	OUT
102722	IN
73103	UNKNOWN
61366	UNKNOWN
86217	OUT
27854	UNKNOWN
92571	UNKNOWN
94716	UNKNOWN
90506	UNKNOWN
166466	UNKNOWN
127009	OUT
70460	OUT
116730	UNKNOWN
159822	OUT
174711	UNKNOWN
149172	OUT
177509	OUT
24007	OUT
25248	UNKNOWN
106616	IN
86508	UNKNOWN
135914	UNKNOWN
70209	UNKNOWN
137146	OUT
110622	UNKNOWN
111382	OUT
67453	OUT
143222	UNKNOWN
115059	OUT
70101	OUT
54206	OUT
141082	IN
110123	OUT
160425	OUT
61248	UNKNOWN
135092	UNKNOWN
137966	IN
203	UNKNOWN
62657	OUT
124106	UNKNOWN
28572	OUT
13777	OUT
18777	UNKNOWN
55692	OUT
40766	UNKNOWN
11911	IN
68156	OUT
93875	OUT
42611	UNKNOWN
40352	OUT
160698	UNKNOWN
159664	UNKNOWN
1845	OUT
162566	UNKNOWN
39910	OUT
122433	UNKNOWN
144969	OUT
15201	OUT
47227	OUT
87054	UNKNOWN
7432	UNKNOWN
31199	UNKNOWN
121313	OUT
100309	OUT
44533	OUT
109934	IN
95170	UNKNOWN
176003	OUT
5072	OUT
13256	IN
108149	UNKNOWN
109937	IN
18535	OUT
79833	UNKNOWN
97614	IN
125515	OUT
177383	UNKNOWN
65229	IN
110604	UNKNOWN
5087	UNKNOWN
162798	UNKNOWN
175459	UNKNOWN
161936	IN
32055	IN
30552	OUT
146036	IN
154717	OUT
18498	OUT
89701	UNKNOWN
115200	OUT
173073	OUT
117980	OUT
168826	OUT
57191	UNKNOWN
82957	OUT
119235	UNKNOWN
39515	OUT
92145	UNKNOWN
132351	UNKNOWN
18252	UNKNOWN
115427	UNKNOWN
118931	OUT
2952	OUT
89531	OUT
82809	UNKNOWN
64026	OUT
22897	OUT
53622	OUT
121095	IN
135518	UNKNOWN
10293	IN
86795	OUT
172356	UNKNOWN
24952	OUT
168070	OUT
147515	OUT
88480	UNKNOWN
39715	OUT
27344	OUT
80017	IN
70154	OUT
90254	UNKNOWN
123748	UNKNOWN
51147	OUT
114652	UNKNOWN
41888	UNKNOWN
53267	UNKNOWN
20942	UNKNOWN
69206	OUT
118693	UNKNOWN
96903	OUT
32952	UNKNOWN
124277	OUT
39979	UNKNOWN
136287	UNKNOWN
129005	OUT
90219	UNKNOWN
147495	OUT
88521	UNKNOWN
88671	UNKNOWN
124694	UNKNOWN
116801	IN
105592	OUT
77791	OUT
73390	OUT
159642	UNKNOWN
80904	UNKNOWN
148840	OUT
160634	UNKNOWN
9726	OUT
42309	IN
47083	UNKNOWN
96375	OUT
2102	UNKNOWN
91956	UNKNOWN
56937	OUT
172025	UNKNOWN
94557	OUT
165868	UNKNOWN
110580	OUT
85917	UNKNOWN
169971	OUT
106496	UNKNOWN
45969	UNKNOWN
33175	UNKNOWN
20262	OUT
2267	UNKNOWN
145425	OUT
86658	OUT
11496	UNKNOWN
171581	OUT
65158	UNKNOWN
141750	UNKNOWN
74954	OUT
5071	OUT
36973	OUT
77465	OUT
83602	OUT
40346	OUT
176042	OUT
23898	UNKNOWN
118355	OUT
53302	OUT
76774	UNKNOWN
60516	OUT
148832	UNKNOWN
123806	UNKNOWN
56585	UNKNOWN
8745	UNKNOWN
92655	OUT
40212	OUT
146180	OUT
54873	OUT
129056	IN
67328	OUT
113541	OUT
75966	UNKNOWN
30884	UNKNOWN
86956	UNKNOWN
16385	UNKNOWN
114926	UNKNOWN
19817	IN
131226	OUT
158542	OUT
99594	UNKNOWN
62982	OUT
129634	UNKNOWN
31549	UNKNOWN
132205	OUT
103620	OUT
138877	UNKNOWN
109661	OUT
59658	OUT
37303	OUT
109121	OUT
106632	UNKNOWN
38133	UNKNOWN
40585	UNKNOWN
72792	OUT
110277	OUT
83856	UNKNOWN
76417	OUT
23533	OUT
55517	OUT
169719	UNKNOWN
38946	OUT
30811	UNKNOWN
100193	OUT
9973	UNKNOWN
48359	OUT
43312	OUT
27170	OUT
109393	OUT
104953	UNKNOWN
171750	OUT
55535	OUT
78436	UNKNOWN
144591	OUT
2038	IN
153442	OUT
44326	OUT
38520	OUT
60278	OUT
57076	IN
5221	UNKNOWN
56214	OUT
46495	OUT
25124	OUT
159371	OUT
13881	OUT
23393	OUT
21320	OUT
89846	OUT
2224	IN
45442	UNKNOWN
113835	OUT
35576	IN
165745	UNKNOWN
52443	UNKNOWN
28771	IN
151140	OUT
48761	UNKNOWN
104528	OUT
12705	OUT
22444	OUT
143321	OUT
3928	IN
145700	IN
33389	UNKNOWN
120691	UNKNOWN
40699	OUT
130468	OUT
108080	OUT
95912	OUT
88535	UNKNOWN
45221	OUT
165077	OUT
86239	UNKNOWN
113000	UNKNOWN
37503	UNKNOWN
138998	IN
163924	IN
135579	UNKNOWN
163153	UNKNOWN
128478	OUT
167116	IN
24633	OUT
98548	OUT
5185	UNKNOWN
80467	UNKNOWN
77780	UNKNOWN
60775	OUT
80091	UNKNOWN
5191	UNKNOWN
88180	OUT
49507	OUT
1859	UNKNOWN
160853	UNKNOWN
70016	UNKNOWN
13580	OUT
627	UNKNOWN
121344	IN
87186	UNKNOWN
144784	OUT
83622	UNKNOWN
32610	OUT
148005	UNKNOWN
66585	IN
107549	UNKNOWN
40552	OUT
106742	IN
170164	UNKNOWN
134298	OUT
158598	OUT
62571	UNKNOWN
76608	OUT
153679	OUT
29550	UNKNOWN
141248	OUT
105084	IN
170201	OUT
119729	UNKNOWN
150447	OUT
93069	OUT
10607	OUT
78989	IN
132689	OUT
49805	OUT
115263	OUT
175938	OUT
82602	UNKNOWN
172789	UNKNOWN
131454	OUT
54309	OUT
175836	OUT
147070	UNKNOWN
172185	IN
112096	OUT
113533	OUT
125124	OUT
33346	IN
64043	OUT
170282	OUT
97876	UNKNOWN
142530	UNKNOWN
120737	UNKNOWN
73963	OUT
88623	UNKNOWN
209	OUT
114872	UNKNOWN
67716	UNKNOWN
108455	IN
3604	OUT
127238	OUT
25546	UNKNOWN
42812	OUT
122898	UNKNOWN
5216	OUT
57757	OUT
12692	OUT
80047	UNKNOWN
149850	UNKNOWN
9947	OUT
17071	UNKNOWN
34356	OUT
11093	UNKNOWN
5956	OUT
56579	UNKNOWN
108836	UNKNOWN
125103	OUT
57209	OUT
78384	UNKNOWN
131164	UNKNOWN
50697	IN
60034	OUT
122558	OUT
69829	UNKNOWN
163189	UNKNOWN
35710	UNKNOWN
87531	OUT
87544	UNKNOWN
129956	UNKNOWN
56808	UNKNOWN
141371	OUT
169494	OUT
29494	UNKNOWN
137480	OUT
174930	OUT
79213	OUT
134701	OUT
140028	OUT
95299	IN
142276	OUT
6867	UNKNOWN
29961	UNKNOWN
153467	OUT
111303	OUT
85408	OUT
44001	IN
105035	UNKNOWN
168123	OUT
99189	OUT
70311	UNKNOWN
126965	OUT
91998	UNKNOWN
119398	UNKNOWN
160364	OUT
57431	OUT
125463	OUT
83585	OUT
152596	OUT
152079	OUT
19930	IN
108366	OUT
158592	OUT
58409	OUT
14171	OUT
138283	UNKNOWN
25599	UNKNOWN
160798	UNKNOWN
114268	OUT
136797	OUT
122888	UNKNOWN
102136	OUT
7852	OUT
17643	OUT
24550	UNKNOWN
163374	OUT
39129	OUT
142518	UNKNOWN
89113	UNKNOWN
84736	OUT
29005	OUT
153477	OUT
142607	OUT
114722	UNKNOWN
77867	UNKNOWN
19226	UNKNOWN
51855	OUT
89074	IN
130132	UNKNOWN
134832	OUT
16693	UNKNOWN
109160	UNKNOWN
43194	IN
156287	UNKNOWN
39577	UNKNOWN
162524	OUT
146296	OUT
110725	IN
91331	OUT
82801	OUT
115087	IN
74738	UNKNOWN
176032	UNKNOWN
109571	OUT
89192	IN
162613	IN
81629	UNKNOWN
19386	IN
149529	UNKNOWN
7625	OUT
128364	UNKNOWN
157119	UNKNOWN
58214	OUT
13843	UNKNOWN
173349	OUT
54402	OUT
104534	UNKNOWN
146693	OUT
42045	UNKNOWN
47570	OUT
44250	OUT
179014	UNKNOWN
122946	UNKNOWN
47313	UNKNOWN
154132	OUT
121366	OUT
134384	IN
170117	IN
53663	UNKNOWN
146667	OUT
49964	UNKNOWN
98756	OUT
65632	IN
58658	UNKNOWN
78285	OUT
105221	UNKNOWN
131235	UNKNOWN
45914	OUT
162797	UNKNOWN
56427	OUT
14514	IN
171994	UNKNOWN
161823	UNKNOWN
117224	UNKNOWN
24913	OUT
36361	UNKNOWN
162186	UNKNOWN
99987	OUT
150004	OUT
68445	IN
122346	UNKNOWN
90316	OUT
43149	UNKNOWN
240	UNKNOWN
175995	OUT
119563	UNKNOWN
2798	OUT
143558	UNKNOWN
170260	OUT
18855	UNKNOWN
158068	OUT
27054	OUT
117383	OUT
145196	UNKNOWN
177151	OUT
73313	OUT
100290	OUT
78271	OUT
45721	UNKNOWN
28744	IN
121309	UNKNOWN
31875	OUT
113723	OUT
158413	UNKNOWN
179078	UNKNOWN
39935	OUT
31292	IN
112940	UNKNOWN
35408	UNKNOWN
24067	IN
118542	OUT
109672	OUT
36324	UNKNOWN
155330	UNKNOWN
167507	OUT
148980	UNKNOWN
159344	OUT
50238	UNKNOWN
81424	OUT
24730	OUT
49450	UNKNOWN
58561	UNKNOWN
48642	IN
98291	OUT
36599	UNKNOWN
62279	IN
172012	UNKNOWN
73426	OUT
155645	IN
167552	UNKNOWN
52639	UNKNOWN
140399	UNKNOWN
37801	UNKNOWN
150010	IN
41586	UNKNOWN
78460	OUT
31457	IN
50736	OUT
89035	IN
164148	IN
89681	UNKNOWN
21694	OUT
169034	UNKNOWN
117718	OUT
46179	OUT
53658	OUT
107310	OUT
82035	UNKNOWN
21299	OUT
141713	OUT
145412	OUT
138266	OUT
149077	OUT
121096	UNKNOWN
83741	UNKNOWN
58172	OUT
159082	OUT
457	OUT
81518	UNKNOWN
158044	OUT
157342	UNKNOWN
19845	OUT
117807	OUT
62539	UNKNOWN
119572	UNKNOWN
66523	IN
33843	OUT
32040	IN
70277	IN
68063	UNKNOWN
153710	OUT
10499	OUT
66690	OUT
73675	UNKNOWN
84556	OUT
85694	OUT
```
