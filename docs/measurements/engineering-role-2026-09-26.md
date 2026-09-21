# Iteration 6 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. **The criterion changed while this iteration was running**: it gained a
Q1 gate — a post that is not a vacancy is `OUT` — and the rules were changed to obey it before the
handoff was drawn. The labels below were written against the revision before that edit, `33113b2`.

The filename carries 2026-09-26 rather than the day it ran, for the reason iteration 3 gave: the
five earlier iterations already hold the five days before it, and the next session finds its input
by taking the newest file.

This iteration labelled the 1000 rows iteration 5 drew, scored iteration 5's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-25.tsv`; they were written to disk before any
rule of the classifier was read.

**The labelling was done by a subagent again**, the way iteration 5 did it: the agent was given the
criterion and the 1000 bare titles, denied the classifier's source, the iteration reports and the
earlier label files, and told to write its labels to disk before anything else. Blinding is
ordering, and the ordering was enforced by what the agent could reach.

## The numbers

The two rates score **iteration 5's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change.

| Gated number | Iteration 5 | Iteration 6 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | 1.50% | **1.17%** (7 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | 5.00% | **9.00%** (9 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 31.35% | **26.11%** | fell < 1 pp |

The corpus after the change:

| | Iteration 4 | Iteration 5 | Iteration 6 |
| --- | ---: | ---: | ---: |
| `IN` | 16.47% | 16.92% | **17.07%** (30 567) |
| `OUT` | 41.98% | 51.73% | **56.82%** (101 763) |
| **`UNKNOWN` — the gated share** | 41.56% | 31.35% | **26.11%** (46 768) |
| &nbsp;&nbsp;of which `unruled` | 22.52% | 9.91% | **7.00%** (12 541) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 17.60% | 19.57% | **17.54%** (31 415) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.44% | 1.87% | **1.57%** (2 812) |

This is the first iteration where `domain_ambiguity` fell — two points of it — and it fell for one
reason: the consultant stopped being domain-free, so the titles that named a head with no readable
domain now read their modifier instead of waiting for one. Every reason moved the same way for
once.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 5 called `UNKNOWN`, the
labeller called 24 `IN`, 186 `OUT` and 90 `UNKNOWN`. One unknown title in twelve is an engineering
role, against one in thirty last iteration — so the pile got *richer* in `IN` as it shrank, which
reverses the reading iteration 5 took from its own sample and says recall is worse than the 1.17%
miss rate does. Sixteen of those 24 were one of eleven software words no qualifier list held
(`c++`, `cyber`, `threat`, `observability`, `storage`, `bi`, `applications`, `technology`, `gis`,
`outsystems`, `devsecops`); they are on the list now. The 186 `OUT` rows say the harvest iteration
5 thought was reaching the bottom is not: they paid for most of this iteration's change.

## The exit gate

Not met. Both rates pass for the fourth iteration running, but the unknown share fell 5.24 points
where the gate asks for less than one, so the two-consecutive count starts again from here.

Worth saying plainly, because it is now the shape of the whole loop: **the unknown-share condition
is the only thing between this classifier and the exit, and it is a condition about the loop's own
rate of change rather than about the classifier's quality.** It can be met either by converging or
by declining to make changes the sample has already shown are right. This iteration had 186 rows
of evidence in the `UNKNOWN` stratum alone and took them, which costs the gate. The alternative —
banking a small change to hold the share still — would be measuring the loop instead of the
corpus, so it was not taken.

## Cost

Whole-word, case-insensitive matching over the cleaned title and nothing else. A full pass over all
179 098 cleaned titles takes **0.499s** in memory, against the criterion's ten-second limit. The
endpoint's wall clock is the database.

## What changed, and why

Every change below was priced before it was kept: applied to the classifier, re-scored over this
sample's 1000 fresh rows and over all 5000 rows the five fixtures now hold, kept only where the
fresh rows showed no new miss. Where a change bought its rows at the price of a miss it was
dropped, and the drops are recorded with the keeps, because what a rule costs is the part of this
that is hard to see later.

**The leadership heads survived their first honest test.** Iteration 5 added `director`, `head`,
`president` and `expert` on a count taken over rows earlier iterations had already grown rules
from, and said one bad row in this sample's false accepts would be enough to want `director`
narrowed. There were two — `Data Solutions GTM Director` and `Vice President Client Partnerships
Publisher Cloud` — and one miss, `Head of Engineering Sales Marketing Tools`. Taking the five heads
back out was measured on the fresh 1000: it saves those two false accepts and costs **20 correct
`OUT` rows and 2 correct `IN` rows**. They stay, and both false accepts are fixed by markers
instead.

**Eight markers from the false accepts.** `rf`, `emc`, `apparel`, `asset`, `channel`, `channels`,
`partnerships` and `pfas`, each from one of the nine rows iteration 5's classifier called `IN` and
the labeller called `OUT`. Together they take this sample's false accepts from nine to two without
touching a single miss. `gtm` was tried in the same batch for `Data Solutions GTM Director` and
**dropped**: it breaks `GTM Engineer Manager of Revenue Operations`, and a miss is the expensive
error at a 2% gate. That false accept is the one the batch leaves standing.

**The `advertising` marker is gone.** Iteration 5 bought it for `Fashion Paid Advertising
Strategist`, which the `fashion` marker next to it already decides; what it cost was the miss
`Software Engineer Advertising Engineering`. Dropping it costs nothing measurable and buys a miss
back.

**Two rulings for products the marker list was reading as domains.** `marketing cloud` → `IN`,
because Salesforce Marketing Cloud is a software product and the `marketing` marker was getting
both `Developer Salesforce Marketing Cloud` and `Salesforce Marketing Cloud Consultant Specialist`
wrong. And `red team` → `IN`, for `Red Team Operator`, where the never-engineering `operator` head
decides a security role the criterion's Q2 and Q4 both reach.

**The consultant is domain-bound now.** Iteration 4 made it domain-free on the criterion's own
argument — a consultant operates on information *about* a domain, the way an analyst does. Five
samples say the consultants this corpus posts are leasing agents, beauty counters, recruiters and
estate agents, and a domain-free head cannot see the marker that would decide them. Bound, it wins
**6 more correct `OUT` rows on the fresh sample and 11 over all 5000** and keeps `SAP Consultant` and `Salesforce Marketing
Cloud Consultant Specialist` `IN`. What it costs is pinned in the tests: `audit consultant` reads
`OUT` where `audit analyst` beside it stays open, which is exactly the trade the criterion's
domain-free reading was protecting.

**`associate` is a head.** Iteration 5 tried it, won nine rows, broke `Associate Veterinarian DVM`
and dropped it. The marker list has grown since, and measured again on fresh rows it is now a pure
win — **seven more correct `OUT` on this sample and fourteen over all 5000, with no new miss and no
new false accept** — because the associates this corpus posts name their domain (`Associate
Business Development`, `Warehouse Associate`, `Associate Payroll`). The break is real and unchanged
and is pinned: `Associate Veterinarian` is `domain_ambiguity`, the seniority hiding the profession
the way `Head Chef` does.

**Twenty-two never-engineering heads**, every one from a row in this sample's `UNKNOWN` pile:
`provider`, `navigator`, `banker`, `attendant`, `fabricator`, `fitter`, `physiologist`,
`optometrist`, `endocrinologist`, `sorter`, `mixer`, `babysitter`, `apprentice`, `firefighters`,
`pathologists`, and the German, Dutch, French, Spanish and Portuguese `personalberater`,
`verkoopmedewerker`, `superviseur`, `directeur`, `ejecutivo`, `executivo` and `produktionsleiter`.
Twenty-four correct `OUT` rows on the fresh sample, nothing broken. `server` was **not** added for
`Server`, the restaurant job: the word is named first in `SQL Server Developer`.

**Eight commercial markers**: `client`, `clients`, `leasing`, `outreach`, `campaign`, `banking`,
`escrow` and `staffing`. Six more were tried in the same batch and **dropped for buying eleven
misses between them** — `strategy`, `strategic`, `growth`, `media`, `revenue` and `relations` are
words software roles use as often as sales roles do: `Software Engineer Growth`, `Staff iOS
Engineer Media Foundation`, `Developer Relations Manager`, `Product Manager Revenue Tools`,
`Manager Data Strategy`. That is the clearest thing this iteration learned about commercial
markers: the ones that pay name a transaction, and the ones that do not name an ambition.

**Thirty-seven domain markers from the `UNKNOWN` pile**, physical and organisational:
`gear`, `fluidic`, `optomechanical`, `roadway`, `structures`, `lunar`, `nuclear`, `mechanism`,
`flight`, `business operations`, `regulatory`, `inventory`, `beauty`, `transformation`,
`transmission`, `environmental`, `transportation`, `intake`, `records`, `print`,
`personal training`, `yard`, `relationship`, `tour`, `paid search`, `seo`, `programmatic`,
`operational excellence`, `preconstruction`, `medical`, `mammography`, `oncology`, `pathology`,
`dialysis`, `ward`, `charity` and `coach` — the last so that `Head Varsity Boys Basketball Coach`
is decided by the coach the leadership head hides. `creative` was tried and **dropped**: it breaks
`Product Manager Creative Strategy Applications`, and the two creative directors it would have
decided are not worth a miss.

**Q1: the post that hires nobody.** The criterion gained this gate mid-iteration and the rules
follow it — 954 titles in the corpus, spelt as twenty rulings rather than read off a missing head,
because `Software Engineering Talent Community` names a head and still hires nobody: `talent
community`, `talent network`, `talent pool`, `talent pipeline`, `candidate pool`,
`general application`, `employment application`, `open application(s)`,
`speculative application(s)`, `spontaneous application(s)`, `expression(s) of interest`,
`future opportunity`/`future opportunities`, `submit your resume`, `refer a friend` and
`interested in working with us`. 42 of those titles used to read `IN` and 890 sat in `UNKNOWN`;
they are `OUT` now. Two rows in the older fixtures — `Future Opportunities Software Engineering`
and `YOW Expression of Interest Software Engineer` — were labelled `IN` by labellers reading the
criterion before Q1 existed, so they now score as misses against labels the criterion has
superseded. They are not regressions and the fixtures are not rewritten: labels record what a
session read, and Q1 is what the next session will read.

**`electrical engineer` is `OUT`.** It has been `scope_ambiguity` since iteration 2. Five samples
have now labelled nine electrical engineers and all nine are `OUT`; no labeller has ever called one
open. This is the same shape as the technician and the designer: a ruled split that the labels
never split on.

**Eleven software qualifiers and two French heads**, all from the `UNKNOWN` rows the labeller called
`IN`: `c++`, `gis`, `outsystems`, `devsecops`, `observability`, `threat`, `cyber`, `applications`,
`storage`, `bi`, `technology`, and `développeur` and `développeuse`. They take the sample's
unknown-but-really-`IN` rows from 24 to 8. `monitoring` and `analytics` were tried and **dropped**:
they buy four false accepts (`Alarm Monitoring Advisor`, `Manager Social Analytics EMEA`) and the
rows they were bought for are already decided by `observability` and `bi`.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows the change was grown from. This classifier's honest numbers are iteration 7's to take.

Four misses survive. Three are the customer-vertical family iteration 5 described and one is new:

- `Software Engineer Manufacturing Test EagleEye` — the `manufacturing` trade, recorded since
  iteration 4.
- `AI Software Engineer Self driving Laboratory` — `laboratory` beats the software qualifier.
- `Lead Engineer Vehicle SW Package SW Builds` — `vehicle` beats it, and `SW` is not a word the
  qualifier list holds.
- `Head of Engineering Sales Marketing Tools` — the tools an engineering organisation builds for
  its sales people, read as a sales role. This one is the leadership heads' own cost.

Two false accepts survive: `Data Solutions GTM Director`, described above, and `Fire Protection
Consultant Sprinkler Systems`, which the consultant head cannot fix — a marker would have decided
it, and `fire protection` is a phrase, not a domain word this corpus repeats.

## What the next session should look at

**The unknown share is 26.60% and 17.55 of those points are `domain_ambiguity`** — two thirds of
what is left, and the reason the criterion calls the corpus's rather than ours. `unruled` is 7.48%
and still falling, but it is now small enough that a session which only harvests `unruled` cannot
move the gated number by much. The question iteration 5 raised is closer: whether
`domain_ambiguity` is genuinely the world, or whether it is a head-list problem wearing the
corpus's name.

**The `UNKNOWN` stratum is getting richer in `IN`, not poorer.** One in twelve this iteration
against one in thirty last. If iteration 7 sees the same, the recall story is worse than the miss
rate says and the next round of work is qualifiers, not markers.

**Watch the consultant.** It is the change with the widest reach this iteration made — it re-reads
every consultant in the corpus, not the handful the sample showed — and, unlike the leadership heads, it contradicts an argument the criterion itself makes
about domain-free heads. One bad row in iteration 7's false accepts is worth taking seriously.

## Questions for the criterion, not for the rules

The labeller raised four. **One of them was answered while the iteration ran**: the posting that
hires nobody — `Interested in Working With Us`, `Join Our Talent Network`, `Employment
Application`, refer-a-friend posts — had no reading the criterion could give it, and it now has
Q1. That is the change described above, and it takes about twelve rows in every thousand out of
the unknown pile they were sitting in permanently. The remaining three are open.

**The customer vertical against the work domain** — unchanged from iteration 5, and still three of
four surviving misses. Step 4 cannot tell who the work is *for* from what it is *in*.

**Q2 against Q4 on hardware-adjacent code roles.** `FPGA Engineer`, `GNC Engineer`, `Signal
Processing Engineer Space`, `Scientist Computational Chemistry`: code is the primary artifact
(Q2 says `IN`) and the credential is not software (Q4 says `OUT`). This labeller let Q4 win and
called them `OUT`; iteration 5's labeller split the same family the other way. The criterion does
not rank its own questions when they conflict, and the rules leave the whole family
`domain_ambiguity`, which is why a run of these rows reads as disagreement in both directions.

**Bare management heads.** `Director`, `Program Manager`, `Delivery Manager`, `Engineering Lead`
with no domain: the labeller called them `domain_ambiguity` and resolved them only when a qualifier
or a clear non-software domain was present. That is what the rules do too, so the two agree — but
they agree by the labeller inventing a convention the criterion does not state, and the next
labeller may invent a different one.

## Held out of the draw

The 5000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 170 685 of the
179 098 rows were eligible.

## The sample for iteration 7

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
87498	Functional Safety Engineer
148554	Product Manager Global Payouts
167309	Bookkeeping Specialist
135673	New Grad Account Development Representative II Phoenix
176018	Pessoa Analista de Mídia Pleno
148242	Android BSP Engineer
161639	GNSS Payload Engineer
48214	Entry Level Kitchen Position Jefferson
58115	Yard Technician
9690	Quality Assurance Engineer II
115725	Director of ABA Services Tahlequah
36865	Title Land Diligence Manager
98478	Solution Architect Pre Sales NetSuite
25839	Web Service Protection Engineer
18723	Sales Development Representative
107863	Maintenance Engineer FC04
92311	Scientific Director Team Lead Oncology
7702	Sales Service Lead Multrees Walk Edinburgh
146520	Staff Software Engineer Platform Java
25699	Child and Adolescent Therapist
161668	Assistant Manager
2681	Garage Door Sales Consultant
38674	Maintenance Supervisor
131811	Lifecycle Specialist Time and Attendance EMEA
90315	Adults Occupational Therapist
53426	Hospice RN Case Manager Union Essex County NJ
81098	Micro Intern Web Development IAP
17096	Regional Manager Affordable Housing
168521	Emergency Credentialed Veterinary Technician Shift Lead Falls Church VA
144236	Production Shift Supervisor Starlink Akoustis
154850	Personal Trainer Imperial Beach CA
84463	Market Assistant Property Manager I
96187	CNC Machinist 2nd Shift
75095	Bathroom Remodel Apprentice
21729	Direct Support Professional DSP
4268	Haitian Creole Language Expert Freelance AI Trainer Project
112007	Account Executive Madrid
81274	Server Assistant
111442	ABA Therapist RBT Flushing NY
139971	Lead Data Analyst
102166	Case Manager Spanish Speaking
129088	Consumer Insights Consultant FMCG
113542	Aide à domicile
51726	Optometric Technician
139414	Product Builder Creator
67382	Enterprise Account Executive Lender
177719	Project Director Dallas
103380	Project Manager Remote
99262	Registered Nurse Behavioral Health
126840	Band 7 CT Radiographer Locum Milton Keynes
45965	Sales Engineer NorthCentral
15503	Consultor a Comercial Soluções de Finanças e Gestão Escolar
95738	Midtown
152476	Personal Trainer
54497	Certified Personal Trainer
13727	Oracle HCM Consultant Core HR
132843	Partnership Development Manager
155535	Facility Security Officer FSO
39710	Project Procurement Manager
166255	Registered Nurse Operating Room
108912	Strategy PMO Project Manager
81118	Receptionist L 68 99
86875	Director Social Media Influencer Marketing
116296	Operations Specialist
59350	Représentant.e PUM Maroc
42457	Brand Management
73127	US DC Labor Planning Associate
177429	Banker PME Campinas e Ribeirão Preto
29138	Director Regulatory CMC
67693	Technical Project Manager Data Center Infrastructure
154334	Personal Trainer
4006	Arabic Levantine Language Specialist Freelance AI Trainer Project
148094	Director Corporate Investment
125227	US Residents Survey Participants New York
97486	Floor Lead Retail Part time
31068	Web Developer
118923	Registered Dietitian RD Any State License
127169	Band7 Specialist Speech and Language Therapist Woolwich
95287	Regional East Market Lead
176566	Construction Superintendent Memphis
69988	Flower Technician Indoor
107694	Research Development Manager R&D
61268	Pet Insurance Sales Associate Castle Rock CO
34454	Registered Behavior Technician RBT
164676	Immigration Attorney USCIS
52409	Licensed Social Worker Early Intervention
110245	Per Diem Primary Care Nurse Practitioner or Physician Assistant Casual Employee
68875	Sales Manager Industry Expansion
86457	International Destination Expert
79549	Instructional Designer Contract Remote
23084	Registered Nurse HICU Pediatrics
60359	Private Sales Representative
7893	Seasonal Operations Associate Part Time Suburban Square
164717	Settlement Negotiator
150634	Personal Trainer
59183	01 Marketing Template
177985	Product Specialist SaaS Implementation and Onboarding
37286	Incident Response Analyst React
31201	Business Analyst Data Management
75560	Insurance Agent Birmingham AL
56130	Operations Manager New Gym Opening
137663	Talent Manager
28864	F T Veterinarian Medical Director
36009	Lead Analyst Financial Crimes
17783	Attendant Adult Care Partner Hallsville TX
75379	Warehouse Manager
48644	Head of Delivery Operations all genders
148895	Machine Learning Engineer Active Secret Clearance
135301	Chief of Staff COO
65247	Data Scientist 643
94543	Seasonal Ambassador Lynnhaven Mall
139307	Customer Service Representative
143580	Hardware Test Engineer Starlink
75437	Operator III C Crew
58746	Member of Technical Staff
145344	Test Engineer Avionics Starship
160046	Porter Full Time
33787	Behavior Technician
4176	Environment Specialist Fluent in French Freelance AI Trainer Project
38461	Motion Designer
141057	Regional Director Enterprise FedGov
126931	Band 7 Locum Echocardiographer Dumfries
83726	Sales Associate Part Time Palo Alto
66549	Director Clinical Scientist Respiratory indications
126278	Band 6 7 Locum Physiotherapist London
108224	Product Engineer
39764	Install Master
99440	Account Executive
104411	Revenue Operations Manager
129396	Quantum Scientist
30274	Gerente de Relacionamento C6 Empresas Pessoa Jurídica Rio de Janeiro Rio de Janeiro
37721	Sales Associate Stylist Yorkdale Shopping Centre
95297	Revenue Operations Analyst
6200	National Channel Manager
64250	Cruise Operations Manager
161184	Bilingual Hybrid Development Representative Mandarin
128524	Datapath Engineer Portworx
97925	Business Systems Analyst Project Management Platforms
6957	Intelligence Sharing Specialist Mid 870
4761	Urdu Language Specialist Freelance AI Trainer Project
126867	Band 7 Emergency Nurse Practitioner ENP Chard
122123	AI Training Fluent Bengali Speakers IN
14051	Technical Consultant
84637	Aide à domicile véhiculé
114194	Flex DFM Engineer
29205	2027 Investment Associate Intern
138449	Data Infrastructure Engineer Intern
104465	Benefits Specialist
107849	Engineering Team Manager
35179	Finance Event Volunteer
168316	Emergency Credentialed Veterinary Technician Full Time Nanuet NY
98379	Data Center Construction to Mid Project Manager
86356	B2B Customer Support Representative
32495	Sales Development Representative Legal Services
18123	Client Services Lead Enterprise Partnerships
159444	Physical Occupational Therapist Consultant
142028	Consulting Psychiatrist
38441	Technical Program Manager Knowledge Systems
87449	2027 Summer Intern Structured Finance Ratings
83022	Vendeur CDD temps partiel
131469	Propulsion Test Engineer
137742	Farm Associate Temporary to Hire
138695	Coordonnateur trice opérations du milieu de travail et soutien à la direction
116646	Strategy Programs Manager
14737	Design Engineer SJ2026AP
34484	Registered Behavior Technician RBT
100493	Assistant Branch Manager
78995	Staff Product Marketing Manager
50358	Technical Account Manager UK
117645	PLOS One Associate Editor Life Sciences
160627	Full Stack Developer AI Team
153967	Personal Trainer
21347	Adult Private Duty Weekend Baylor Registered Nurse RN
115207	Service Assistant
169095	New ER Doctor NERD Program Starts January 2027 Practicing Veterinarians H Street Washington D.C
55800	Member Experience Manager
174006	National Litigation Claims Management Attorney
122620	Cardiologists Freelance Remote Mesa US
2347	Analytics Engineer
97970	Associate Mechanical Engineer Intern Co Op
60113	Pediatric Occupational Therapist Schoolage 1K Sign on Bonus
104840	Dermatologist Bedford NH
34468	Registered Behavior Technician RBT
169323	Veterinary Technician Student Externship Oklahoma City OK
141910	Executive and Team Assistant
154778	Personal Trainer Fiskdale MA
110471	Advanced Practice Provider Chesapeake Urology
47104	Marketing and Communications Manager eCommerce 12 month FTC
18227	Assistant Controller
156697	Facility Manager
150006	Strategy Analyst Intern
15717	735 Counterintelligence Analyst
59992	Regional Manager Affordable Housing
82367	ꓟachine ꓡearning ꓣesearcher
47192	UX Designer Focus on Research
42899	Orthotist Los Feliz
104798	Staff Applied AI Researcher Agentic Search
159840	Experience Team Member
51704	Optometric Technician
150990	Personal Trainer
136133	Maintenance Technician III
89468	Field Service Technician Appliance Repair Albuquerque NM
80420	Remote Child Adolescent Psychiatrist MA Licensed
107192	Director of Operations Maintenance
1931	Product Platform Engineering Analyst
47453	Automatic Door Installer
27109	Psychiatrist MD
113180	Manager Insurance Product and Benefit Design
41552	쿠팡 리테일 의료기기 및 공산품 컴플라이언스 담당자
110533	Hematologist Oncologist Los Angeles Cancer Network
76052	Account Manager Full Time John Lewis Stratford
62253	Manufacturing Electrical Test Engineer
81238	Marketing Specialist
69794	Transaction Advisory Manager
169060	Medical Director Charlotte NC
50337	Capabilities Hunter
6750	Hybrid Behavior Analyst
30634	Head of Internal Communications
92637	Mechanical Estimator
122654	Chemistry Graduates AI Training Colchester UK
33409	Vice President Economic Security Technology
13687	Landscape Technician Stormwater
140827	Staff Accountant
111282	ABA Paraprofessional RBT Flushing
74001	Hiring Task Based Helpers for Seniors in Richardson Texas
157032	Team Lead
144527	Software Engineer Thermal Fluid Analysis
148989	Account Executive Mexico
118816	Staff Accountant Custodial Accounting
132966	Ripple Trading and Markets Analyst
8086	Seasonal Sales Associate Part Time UTC Sarasota
9182	General Counsel
8978	Application Security Engineer Blue Team
83426	Vendeur CDI temps plein
28200	Team Lead Full Stack
21814	HHA
114243	Coach Business Development at FirstMind
151415	Personal Trainer
164963	Graduate sales executive at United Media
111754	Registered Behavioral Therapist RBT
169076	Medical Director Virginia Beach VA
6235	Civil Engineer In Training EIT
76467	Real Estate Acquisition Consultant
21382	BWL BCBA Behavioral Health
73120	US DC Area Manager II Sous Chef
127946	MSK General Sonographer 12 Month FTC
110511	Gynecologic Oncologist Los Angeles Cancer Network
163895	Product Manager Cleared
152228	Personal Trainer
50063	Manager DashMart Growth Pricing Strategy Operations
58707	Registered Pharmacist Manila UK Support
164595	Telesales Team Leader
49424	Medical Content Manager
87842	Licensed Veterinary Technician LVT Marina Bay Animal Hospital
138907	Director Social Partnerships
44053	Software Engineer Remote
35399	Enterprise Account Executive South TOLA
81975	Clinical Research Systems Specialist
44081	Customer Success Manager French speaking
56981	Sales Support Rep Req# 1414
160135	Head Chef
142237	Cosmetic Surgery Sales Consultant Training Provided
157866	Director Supply Chain Operations
96267	Autonomy Engineer Integration
151284	Personal Trainer
122570	Cantonese Fluent Speakers AI Training Auckland New Zealand
97346	Associate Planning
28004	VP Technology Transformation
59186	1L Summer Associate
24617	Sportsbook Frontend Engineering Team Lead
20970	Consultant Power Market Expert Poland
13072	Staff+ Security Engineer Risk Engineering
156531	Game Services Engineering Lead
99686	Delivery Driver
173572	Consultant Mergers Acquisitions
173374	Veterinary Assistant
174518	אחראי ת משמרת הרצליה Shift Supervisor Herzliya
48469	Product Manager Financial Apps
99581	Regional Account Manager Gateway GEM
173334	Veterinarian Main Street Animal Clinic
102546	Lead Teacher Morgan s
128040	Paediatric Nurse
32892	Complex Case Manager Pediatric RN Temporary
45066	Solutions Architect EDW Enterprise Data Warehouse Migrations
8123	Site Director DC
45296	Manager AI Forward Deployed Engineering AI FDE
43286	Weekend Licensed Clinician
174600	Digital Creative Design for dm drogerie markt
61517	Growth Strategy Manager Hindi Speaker
149591	Field Sales Executive
164697	Personal Injury Case Manager
133672	Staff Manufacturing Engineer DFM
4000	Arabic Gulf Language Expert Freelance AI Trainer Project
156414	Dermatologist Cincinnati OH
167868	Sales Strategy and Operations Division Lead
46392	Teletherapy Therapist LSW or LPC Remote Free Supervision
111125	ABA Behavior Therapist Yonkers
103128	Project Manager Onsite HVAC Plumbing Mechanical St Louis
89599	Specialist II Procurement Construction
95600	Chief HR Officer
23888	Associate ESG
56379	Service Associate
129507	3PL Fulfillment Operations Manager Dallas TX
81337	Executive Assistant
132754	Don t See a Fit Join Our Talent Community
93325	Accounting Lead
34943	Customer Experience Representative
100277	Growth Associate Content Marketing
23022	Registered Nurse
51020	Staff Software Engineer Full Stack Engagement team
40089	Medical Assistant MA Urgent Care
8900	Supplier Quality Engineer
134310	Systems Engineering Intern Summer 2027
99507	SEO SEM Specialist
152958	Personal Trainer
74296	ServiceNow Software Engineer TSM OMT
56717	Data Center Operations Manager
24908	Director Design Product Experience
102954	VP of Strategic Accounts Financial Services
39005	Neuroscience Therapeutic Sales Specialist Raleigh NC
114748	Director Sales
154510	Personal Trainer
136751	Director of Human Capital Management EMEA
29829	Data Warehouse Engineer
128861	Join our Talent Pool for Future Opportunities with Resi
152059	Personal Trainer
50615	Software Engineer Cumberland FICCO Tools Engineering
29993	RN Director of Home Health
100860	Recruiter
17187	Lead Emulation Engineer
143551	Government Affairs Manager
71597	Our South LA team is always hiring
44873	Lakebase Sales Specialist
168796	Emergency Veterinarian Snellville GA
12801	Global Real Estate Construction Manager
22901	Physical Therapy Assistant Home Health Visits
46882	HGV Technician
147494	Executivo a Comercial Externo Teresina PI
4145	Detective Police Officers Freelance AI Trainer Project
133950	Inventory Coordinator II
50143	Shift Lead Huntsville
130957	Account Executive Public Sector
9058	Manager Medical Congress Events Management
159711	Head of TikTok Shop gn
137724	Quality Inspector
97689	VP Growth
7015	Budget Reporting Manager
139260	Day Shift Seasonal Warehouse Associate
70622	Mid Level Test Engineer
98149	Director Business Development Minneapolis Milwaukee
65983	Program Manager
128175	Registered General Nurse needed in West Midlands
131046	Materials R&D Coordinator
172988	Patient Scheduling Coordinator II Full Time Wolf River
99796	Home Health Therapy Supervisor Internal Only
107746	Head of Clinical Strategy and Program Operations
14310	Customer Success Manager
50924	Thought Leader Liaison Northeast
61439	Event Coordinator Freelance Hong Kong
155778	Catalogue Intern
117225	Product Manager Mission Solutions
94497	Seasonal Ambassador Columbia Mall
75091	Solutions Engineer II
134326	Technician I III Ops Support Facilities
16647	Prinicpal Engineer Data BIA AI Architect
101663	Operations Coordinator Multiple Shifts
17084	Onsite Project Manager
4337	Japanese Voice Actor Freelance AI Trainer Project
58384	Anesthesia Office Manager
9991	Construction Project Manager Co op Spring Summer 2027
146969	事業開発 プロジェクトマネージャー オンチェーン金融
33844	Behavior Technician
2945	Associate Sales Development Representative
109257	Head of Growth
119623	Financial Analyst Project Finance
116357	Head of Service Management
20828	CETS Logistics Support
19718	Professional Services Manager Vehicle Intelligence Product ALPR
8607	Staff Product Designer Mobile
16101	Talent Acquisition Specialist Conseil Data Paris
55961	Operations Manager
16913	Specialist Talent Leadership
177518	Talent Pool XP Empresas Banker Middle Agro
35844	SOC Supervisor
12376	Technical Recruiter Production Contract
64538	Salesforce Account Executive Financial Services
119981	Digital Marketing Consultant
83522	Vendeur CDI temps plein
116803	アカウントマネージャー Account Manager ジュニア ミッドレベル
17532	Launch Operations Mechanical Engineer Lead
52888	Ingeniero QA Automation
177893	PRN Medical Assistant MA
61335	Account Development Representative Spanish Speaker Chicago Based
52805	Lead DFT Engineer
87018	Hacker
160872	Data Engineer
31104	Aduaneiro Híbrido Macaé RJ
152535	Personal Trainer
122379	Biology Graduates AI Training San Diego US
61190	Early Years Educator Unqualified
6418	Manager Corporate Accounting
9385	On Site Project Manager Mission Critical Construction
17992	Lead Product Designer Bangalore India
52442	Program Manager YMCI
55677	Kids Club Manager
44571	Customer Success Manager
29478	Copywriter
103545	2nd Shift Press and Binding Operator Irving
96194	Field Service Technician
9598	Staff Power Electronics Control Engineer
129377	Japan Internship
3762	Software Engineer Backend Lake Analytics Platform
10904	Optical Assembly Technician
72250	Operations Coordinator
151135	Personal Trainer
50068	Manager New Verticals Search Strategy Operations
106254	Cloud FinOps Business Partner
168795	Emergency Veterinarian Silverdale WA
142195	Frontend Engineer React and TypeScript experience is required
153586	Personal Trainer
63100	Sales Consultant
19232	Join Our Talent Network
89082	Scientist II CMC Pipeline Innovation
89048	Director Regulatory CMC
94152	Physician Assistant
36277	Cloud AI Security Architect
112794	Executive Assistant
76505	Recruiter Contract Role
69116	Stylist Mall at Millenia
60565	Branch Merchandiser
8350	Customer Services Analyst
58849	Manager Finance
158904	Account Executive Core
53241	Product Analyst Omni
141977	Medical Assistant Hamptons 20 30 hr
149453	Commercial e terrain
97762	Attorney Estate Planning Strategist Atlanta Chicago New York Dallas
101955	Software Engineer Compute Platform
4350	Khaleeji Dialect Specialist Freelance AI Trainer Project
41762	쿠팡 Director II Compliance Privacy Compliance Monitoring
128572	Hardware Debug and Validation Engineer
175257	Management Accountant
71912	Hardware Test Engineer
125352	Paid Social Manager
36371	Associate Veterinarian Mansfield
1482	Long Term Substitute Teacher
158993	Risk Management Rotational Intern
74005	Hiring Task Based Helpers for Seniors in San Jose CA
118977	Media Sales Executive
34760	Director Accounting Advisory Healthcare
130521	Creative Strategist App Dev
68536	Manager Compensation
53423	Hospice RN Case Manager Middlesex Somerset County NJ
137943	Staff Infrastructure Engineer Data Streaming
94885	Backend Software Engineer Airports
148494	Operations Manager Stripe Delivery Centre
45931	Partner Manager Channels
134443	Solution Architect Finance Systems
78194	Business Development Consultant East
12676	Compute Country Lead Japan
39573	Registered Nurse RN PT .6 Night
146922	Staff Software Engineer
112238	Bilingual English and Spanish Member Loyalty Representative R14251
41755	Director Data Security Engineering Digital Trust
37803	Sales Advisor
89292	Proposal Writer
42238	전기통신사업법 TBA 모니터링 담당자
83469	Vendeur CDI temps plein
97209	Associate Associate Media Billing
71111	Lead Toddler Teacher
73815	Urgent Hiring Helpers for In Home Support Kennewick WA 99336
128200	Registrar in Paediatrics London
34472	Registered Behavior Technician RBT
152057	Personal Trainer
50483	AML Compliance Operations Quality Control Manager
177492	Officer PME Norte
96836	Medical Director Attending Physician
59934	Community Manager
64207	Alternative Investment Reporting Associate
163068	Program Manager
68716	People Business Partner
147868	Vendedor a Externo 6 horas Franco da Rocha SP
39678	HRIS Administrator II
155909	Manager Workplace Services
5987	Product Designer 8+ YOE
111714	Registered Behavioral Technician RBT Brooklyn
174465	Support Associate French speaking
31518	Managing Strategy Digital Assets
1327	3rd Grade Teacher
56635	Wellness Recovery Specialist
165223	Vascular or General Surgeon Up to 50K Sign on Bonus
57478	Heavy Equipment Field Technician Mechanic
9968	Customer Success Strategy Operations Manager
68908	Machine Learning Engineer
125513	Remote Clinical Psychologist Maryland
88191	Practice Leader Civil Engineering
136125	Group Lead
153437	Personal Trainer
122958	Data Entry Clerk Graduates AI Training Tuxtla Gutiérrez Mexico
20492	Software Engineer Application Security
132934	Privacy Manager
161278	Mid Market Account Executive Toast Retail
118160	Overnight Customer Service Representative
120437	Institutional Investor Relations Manager
62009	Smart Vending Business Development Operations Manager
95684	Truck Driver Warehouse Associate
111321	ABA Paraprofessional RBT Jamaica NY
60645	Software Engineer Full Stack FES
113068	Director Internal Audit
39905	Regional Vice President
70073	Personal Care Specialist Full Time
29823	Compliance Officer
129531	Director Specialty Materials
77389	Cyber Security Entrepreneur in Residence
14595	Federal Account Executive
94793	Home Infusion Nurse
9392	Project Developer Renovations Interiors
129612	Sourcing Director Apparel
157739	Staff Program Manager Chief of Staff Operations
172472	Infrastructure Engineer
141060	Sales Development Representative East Coast
20877	Intelligence Operations Integrator
155203	Yoga Instructor Austin TX
167386	Associate Director of Commercial Operations
26860	Psychiatric Mental Health Nurse Practitioner PMHNP
134996	Product Manager IT GTM CPQ
54021	Assistant Fitness Manager
153254	Personal Trainer
72793	Editorial Content Manager Maternity Cover All Genders
13439	Events and Ministry Coordinator Onsite Volunteer
1801	Incident Response Engineer
107385	Stakeholder Community Engagement Manager
131926	Tax Analyst Direct Tax
4440	Legal Counsel Specialist Freelance AI Trainer Project
65470	Sales Operations Coordinator
46063	Strategic Account Executive Vietnam Market
127276	Community Occupational Therapist Sussex
168071	Director Customer Success
51227	Payroll Specialist
47840	Systems Integration Technician Experienced Service Install
65842	Associate Accounting Mumbai
68827	Executive Assistant
143854	Manufacturing Engineer Blades and Vanes
39350	Brand Ambassador part time San Jose
96146	European Options Market Maker
175770	Group Experience Director SEO GEO
79330	Manufacturing Engineering Technician
96951	Physiatrist
128106	Permanent Paediatric Speech and Language Therapist Dublin
176349	WPP Media Associate Social Media Egypt
136773	Quantitative Trading Associate
156438	Data Center Deployment Technician II
36912	FT Shipping Associate
135214	Director Market Intelligence and Partnerships
141759	Night Cleaner High Road House West London
110446	Commissioning Engineer
54140	Assistant Fitness Manager NEW Gym Opening
106718	Project Manager Design and Construction
27597	Psychotherapist
19218	Medizinische Fachangestellte für Allgemeinmedizin
130465	Client Account Manager Large Customer Sales Tech
60509	Retail Lead Golden State Warriors Valkyries Team Store
22073	Hospice Volunteer Unpaid
73394	Compassionate Helper Needed Kirkland WA 98034
68056	Program Officer
176596	Executive Chef Memphis
123266	Fluent Norwegian Speakers AI Trainer Freelance Remote Bergen Norway
16516	Engineer Field Software
54638	Certified Personal Trainer
158042	Industrial Owner Sales HVAC
103303	Application Security Engineer
55802	Member Experience Manager
108841	Java Backend Engineer Asset Earn
175158	Occupational Therapist Early Career
177324	AI Research Intern Predictive World Model
57551	Heavy Equipment Shop Technician Mechanic
88737	Automotive Service Technician Contract
57488	Heavy Equipment Field Technician Mechanic
49025	Staff Structural Analyst Aerospace Defense
97823	Associate Venture Ecosystem Sales
87188	Vice President of Business Development
26813	Psychiatric Mental Health Nurse Practitioner PMHNP
32332	Product Manager Admissions
160696	Associate Global Client Services US Hours JAN 2027 Start
30872	Linux Kernel Engineer Ubuntu
140486	Camera Systems Software Engineer India
69748	Account Manager
7430	Operations Lead Shops at Clearfork
162065	Machine Learning Engineer
53917	Field Service Report Reviewer
109027	Inventory Specialist
38521	Colonial Animal Hospital Veterinary Technician
9730	Director Inventory and Cost Accounting
148870	Data Entry Pharmacy Technician
170577	Physical Therapist PT Home Health Full Time
60095	Occupational Therapist Dutchess County
151023	Personal Trainer
24430	Float Pool Certified Nursing Assistant CNA All Shifts MO IL Certification Required
1304	Enterprise Account Executive New Business Development
41578	쿠팡로지스틱스서비스 장애인 특별 채용 스포츠직
11950	Technical Recruiter Space Contract
177368	Analista Pleno Produtos Seguros PF
52231	Programmer SW Dev II
31946	Manager Digital Product Passports Data Spaces
69293	Sales Director
5149	Robotics Mechanical Engineer
151471	Personal Trainer
41234	Maintenance Technician Cortland Duluth
79678	Mid Level Instructional Designer Contract Remote
23740	Weekend Home Health Visits Registered Nurse
50932	Research Associate Gene Therapy
100078	Staff Design Engineer
73903	Hiring Caregiver for a Female in Brighton New York 14620
58432	CV Anesthesiologist Comanche County Memorial Hospital Lawton OK 26 weeks off
18302	Civil Project Manager Land Development
166781	Manager Procurement Machining
112443	Nurse Practitioner New Grads Welcome
101484	Producer Temp
106915	DevSecOps Engineer TS SCI Clearance Required
109166	Associate Director Global Regulatory Affairs Compliance
81722	Sales Associate Cyprus
60018	Execution Trader
4872	Major Account Manager
96185	CNC Lathe Machinist 2nd Shift
35743	2027 Bachelor s Master s graduates Economics Consulting Analyst Associate
17110	Temporary Regional Manager Affordable Housing
141056	Regional Customer Marketing Manager EMEA
171776	Agent Operations Territory Lead
21511	Certified Nursing Assistant CNA Pediatrics
144539	Sourcing Manager Capital Equipment Construction Starlink
83148	Vendeur CDD temps plein
37568	Software Engineer Realtime
177684	SpendHound Channel Sales Manager Finance Partnerships
124925	Spain Residents Survey Participants Madrid Spain
103471	Vendor Management Internship
72473	Speech Therapist ST Home Health Per diem
51048	Accounting Analyst
160952	HVAC Mechanic Top Secret Clearance
147504	Executivo a Comercial Hunter para Grandes Contas Brusque SC
96635	ICT Infrastructure Design Engineer I Low Voltage
129433	Control System Engineer Site Reliability Engineer SRE
177868	Manager Corporate Development
148547	Product Lead Support Experience
63633	Accounts Receivable Analyst Collections
61068	ICAM and Cyber Security
39846	Research Analyst Intern
86006	Manager Financial Planning Analysis
93428	NPI Project Engineer II
16400	Tax Supervisor Hybrid
73525	In Home Dialysis Care Partner 1 1 Client San Tan Valley Arizona 85143
55553	Kids Club Associate
33977	Center Based Board Certified Assistant Behavior Analyst BCaBA
27605	Psychotherapist
79226	MOBILE SECURITY PLENO
176854	Fullstack Engineer Java Typescript
97129	Travel Medical Assistant
87998	Strategy Director
70827	Associate Client Service
67337	Solutions Engineer
16082	Consultant Strategy Data AI Transformation
43321	Sales Development Representative Intelligence Los Angeles
16523	Engineer II Equipment Engineering
156872	Customer Success Operations Analyst
153116	Personal Trainer
20450	Early Year Practitioner
49889	Electrical Engineer DoorDash Air
162718	AI Marketing Strategist
65235	Mission Outcome Lead
30340	Retail Key Holder Woodburn
122655	Chemistry Graduates AI Training Colorado Springs US
30223	Especialista Social ESG
46443	Customer Success Manager
146267	Learning Producer Course General Manager Agentic AI Course
79787	Product Manager Remote Contract
169763	Paradocs Animal Hospital DVM VMD Student Extern
33316	Value Engineer SC Cleared
7929	Seasonal Operations Associate Part Time Willowbrook Mall
45767	Head of Enterprise Sales Japan
77238	Global Service Associate Legal Entity Management
138565	Site Reliability Engineer
163305	Sales Support Administrator
63484	Government Affairs Strategic Partnership Lead
66796	Sports Data Collector Football Chaozhou China
32247	Marketing Director H&N Cancer
114498	Director Demand Generation
145518	Anaplan Manager
34792	Associate Accounting Advisory
172271	Full Stack Software Engineer Evaluation Tools
51230	Product Designer Mobile
19107	Aircraft Maintenance Instructor
173013	Mobile Patient Care Coordinator
52529	Dental Assistant
65135	Modelling Engineer
61787	Market Strategy and Partnerships Manager
110343	Primary Care Physician Sign On Bonus Available
66517	Summer 2027 Internship Electrical I&C Engineering
64452	IT Field Engineer
39027	Neuroscience Therapeutic Sales Specialist Washington DC
94528	Seasonal Ambassador Hillsdale Shopping Center
149458	Commercial e terrain
1321	2nd Grade Teacher
90148	Research Associate Automated Chemistry
79448	Research Engineer Model Training Post Training
148313	Data Analyst Payments
29849	GRC Lead Crypto Capital Risk Engineer
9798	Mission Integration Analyst
3278	Strategy Business Ops Manager
131254	COPV Engineer II
150130	Consultant Digital Forensic and Incident Response DFIR Remote
157476	VoIP Engineer Remote
126033	Band 5 Accident Emergency Nurse Chester
158221	Digital Marketing Graduate Associate
156186	Director of Strategic Accounts SLED Northeast
58954	Strategic Partner Account Manager Public Sector
61915	Accountant
99781	Home Health Occupational Therapist OT 15K Tuition Reimbursement
25650	Product Marketing Manager
50029	Analyst Protective Services
1335	4th Grade Teacher
147928	Accounting Finance AI Automation Manager
103805	Chef Operator GM
174193	Accountant II
42870	Baby Imaging Specialist
88185	On Call Staff Biologist
27264	Psychiatrist MD
155968	Health Educator Contract
7820	Seasonal Operations Associate Part Time Jordan Creek
118065	Customer Service Representative
22449	Medical Assistant outpatient
11316	Explosive Operations Engineer
95954	IP Specialist Overseas Filling
6128	Sonographer Per Diem
2428	Manager Corporate Relations
75609	Insurance Agent Glen Burnie MD
167867	Sales Strategy and Operations Associate EMEA
22643	Outpatient Physical Therapist PT Living Visits
71450	Business Development Executive I
124109	Material Sciences Graduates AI Training Las Vegas US
132516	Venture Associate at Retail Insights
150894	Personal Trainer
42723	資深客服經理 Customer Service Manager TW Counie Operations
160792	Visual Designer
32766	Head of TikTok Shop
57707	Real Estate Counsel
134578	Product Manager Data Collaboration Measurement
136367	Field Engineer Data Engine
28922	Engineering Manager Travel
86296	Milling Machine Operator
154556	Personal Trainer
57167	Diesel Mechanic
166150	Anesthesiologist
29349	Project Coordinator
163959	Acquisitions M&A Analyst
25993	Product Designer
17816	Attendant Adult Care Partner Tyler Tx
71440	Director of Partner Success
83820	Lead Fire Protection Engineer Consultant
81458	DevOps Working Student for Portal Operations team hybrid
155870	Python Software Engineer
178887	Sales Engineer Commercial
81984	Forward Deployed Product Manager AI
55749	Member Experience Manager
164626	Criminal Defense Attorney
36351	Water Systems Specialist
160402	CX Apprentice Onboarding
146052	PostgreSQL DBA
38508	CNS Neuroscience ADHD Territory Sales Representative Chicago IL
127608	General Gynae or Obs Sonographers Cambridgeshire Weekends
45307	Manager Field Engineering Digital Native Business
119325	Field Service Technician NETA 2
21850	High Acuity Weekend Nurse LPN or RN
52507	Volunteer DD ABI Human Rights Committee Member
7547	Sales Associate Part Time Battersea
11917	Technical Program Manager Intelligence Systems
90140	Machine Learning Engineer Physical Sciences
46052	Strategic Account Executive Public Sector Japan FED
18833	Channel Solution Engineer Bilingual Spanish and Portuguese Night Shift
170036	Part Time Office Manager
72818	FSQA Manager Processed Category
30257	Gerente de Negócios Veículos São Luís Maranhão
8765	Design Release Engineer BIW Upper body Structures
168902	Emergency Veterinary Assistant Per Diem Austin TX Cedar Park
89989	Data Engineer
27929	Relief Veterinary Technician New Jersey
35826	Account Executive Chartbeat Global Media
95667	Project Manager
122023	AI Training Experts Connecticut US
63425	Account Executive CPG and Retail Media
40274	Customer Support Associate
173366	Veterinary Assistant
132443	Graduate sales specialist at Retail Insights
89934	Photonics Characterization Intern New Grad
119769	Event Organiser
142768	Managing Director China
91807	Social Worker Referral Assessment
12558	Scientist Scientist Translational Research and Clinical Biomarkers Immunology
113241	Medical Assessment and Content Editors
41399	Entry Level Sales Executive Trucking
7895	Seasonal Operations Associate Part Time The Americana at Brand
96039	Sous Chef
21091	Strategist
127943	Mortuary Technician East Midlands
148803	Technical Solutions Engineer
46520	Expert Data Catalog
169498	Veterinarian
56212	Service Associate
120330	Head of Graphics at Private Equity Insights
145754	Software Engineer Platform Cupertino CA USA
173155	DVM Student Externship Oregon
68484	MEAL Manager
177307	Sales Leader DACH
12922	Product Manager Cybersecurity
102771	Marketing Data Science Manager
44396	Product Specialist Redpin
53894	Business Immigration Professional
23637	Speech Language Pathologist Home Health
85424	Customer Success Manager
153061	Personal Trainer
66462	Mechanical Engineer Optomechanical Systems
64440	Excavation Laborer
30066	Director Director Strategic Solutions
134956	Mid Market Account Executive Germany
37886	Analista de Onboarding
137970	Staff Windows Low Level C++ Engineer Endpoint security
33476	Behavior Technician
98918	Project Manager Mission Critical Michels Power Inc
169314	Veterinary Technician Student Externship Nanuet NY
53353	Care Coordinator Embedded Cleveland OH
166290	Change Management Consultant Strategic Supplier Program
156570	Category Manager
80655	OPERATIONS ANALYST III Global Account Operations
153093	Personal Trainer
149670	Outside Sales Account Executive City of San Francisco CA
84153	Assistant Store Manager 12 Stall Street Bath
90872	Practitioner Medway Children in Care
131371	Propulsion Test Technician II
6364	Associate Partner Financial Services Data AI
4752	TypeScript Coding Specialist Freelance AI Trainer Project
87126	Data Scientist Media Consultant
153958	Personal Trainer
47748	Project Coordinator Construction and Building Materials
131796	HR Specialist Contracts Management APAC
123529	Hindi Fluent Speakers AI Training Chicago USA
134254	Systems Engineer I Secret Clearance
123706	IT Directors Short Term Paid Research Opportunity ITSM ESM Platforms Canada
19206	Facharzt für Allgemeinmedizin Innere Medizin
67165	Technical Account Manager Taiwan
50065	Manager Finance Strategy Digital Ordering SaaS
129244	Market Microstructure Researcher
26078	Pharmacy Benefits Insurance Verification Specialist Onsite Pittsburgh
34615	Registered Behavior Technician RBT
136882	Sales Development Representative
9923	Regional Vice President Sales UK
134293	Supervisor Composites Production Evening Shift
111944	Head of Marketing
152901	Personal Trainer
151051	Personal Trainer
125760	Security Engineer
140423	Compensation Analyst
58028	Work Site Stabilization Technician Foreman
32987	CPO in Training Samlino Group
115385	機械学習エンジニア 金融事業領域
10009	Construction Project Manager Intern Summer 2027
90924	Social Worker Fostering Recruitment Assessment
15259	Traveling MEP Superintendent Data Center Construction
40662	Summer Associate Falls Church VA
48976	Full Stack Software Engineer II
73539	In Home Dialysis Care Partner 1 1 Client Thousand Oaks CA
39886	Enterprise Account Executive AU
19131	AMT Instructor
23538	RN Registered Nurse Home Care 1 1
67113	Intern Biotechnologist Gene
102044	Residential Sales
148889	Systems Administrator Alachua Onsite
47506	Commercial Door Service Technicians Experienced or Trainee
132461	Head of Finance at Retail Insights
41077	Technical Deployment Lead Central
178449	Guidance Navigation Controls GNC Engineer V VI
89152	Co Leitende r Psychotherapeut in 60 100
32784	Creative Copywriter
72406	Scriptwriter Dex
51091	Business Developer FX Financial Solutions
77147	Management Graduate Program Intake Spanish speaker HRtechX
12687	Customer Success Manager
175153	Clinic Manager
1874	Oracle Cloud Enterprise Performance Management EPM Lead
74874	MEP Field Coordinator
58540	Embedded Software Engg
7887	Seasonal Operations Associate Part Time Stanford Shipping Center
176146	Director Learning and Development
73320	Caregiver Needed Support for a Client Nashville Tennessee 37212
115863	Retail Associate
158770	Production Operative Nights THG Manufacturing Tywyn
8275	Trading Operations Associate West Coast
178482	System Safety Engineer IV V
87182	Underground Mine Engineer
147736	Agente Stone Consultor a Comercial Externo Sinop MT
62608	Sales Development Representative
75974	Mgr Financial Business Consulting
167014	Veterinary Practice Manager 5 000 Sign On Bonus
49884	Director Strategy Operations Local Markets
118151	Overnight Customer Service Representative
58236	Program Coordinator Mid shift
161768	Dishwasher
167992	Technical Trainer VCE
140165	MSAT Tech Services Representative Weekend Day Shift Friday Monday
72375	Clinical Research Coordinator
27967	Veterinary Technician Externship Chicago
130992	Software Engineer Storage Engine Elasticsearch
7274	Operations Associate Full Time Rockefeller
163803	Director Head of Creative
74211	Project Manager Industrial Facilities
47999	Practice Manager Full time
102966	Analyste au soutien à la production Graduate Production Support Analyst Canada Montreal
105851	Contractor Quality Engineer
67311	Revenue Enablement Specialist EMEA 12 Month Contract
50192	Software Engineer Data and AI Platform
68832	Head of Compensation
37543	Territory Account Executive Digital Native Hong Kong
27122	Psychiatrist MD
44821	Enterprise Core Account Executive Public Sector
40520	Defence Engineers Consultants Consultants
122519	Canada Residents Survey Participants Revelstoke Canada
157373	Sales Development Representative Payments
171231	Project Manager
74913	Operations Field Coordinator
156946	Sales Associate
81080	Facilities Manager
169516	Veterinary Assistant
119845	Director Biometrics
147567	Software Engineer III POS
28381	Account Executive
29322	Full stack Software Developer
116086	Sales Associate part time Perry Ellis
65763	Speech Language Pathologist
47970	Mental Health Therapist LCSW LPC Full Time Remote Tennessee License
168563	Emergency Veterinarian Bridgeville PA
75475	Yard Jockey Nights
154197	Personal Trainer
93644	Staff Project Manager Retail Development
30521	PrePress and Print Production Contractor Fall 2026 issue
57128	CDL Delivery Driver
93924	Case Manager RN
32186	Sales Development Representative
80809	Machine Learning Scientist
149866	Business Process Analyst
45809	Manager I Engineering OrgStore Blueprint
163452	Business Development Representative
54332	Assistant General Manager NEW Gym Opening
99134	Physician Clinical Advisor Women s Health Expert
106600	Account Executive
69752	Audit Financial Services
85985	Director Demand Supply Planning
125056	Subject Matter Expert VFX Artists Remote Toulouse
177370	Analista Sales Ops
171648	Security Officer Wylie TX
53951	Solutions Architect East Coast
97330	Regional Commerce Assistant LATAM
137127	Program Manager
49953	Kitchen Shift Lead DashMart
138542	Sales Director Government Payments
112057	Product Manager Diner Engagement
80715	TECHNOLOGY COORDINATOR Customer Intelligence
98037	Front Desk Concierge ROOST White House
90386	Advanced Practitioner Support and Protect
1555	AI Data Analyst
77809	Project Engineer
43625	GM Europe
117855	Assistant Store Manager
22484	New Grad Licensed Practical Nurse LPN
142166	Construction Coordinator
165995	Tooling Technician 3rd Shift
23177	Registered Nurse RN
80209	Flow Production Tracking Administrator CONTRACT
79546	Instructional Designer Contract Remote
152854	Personal Trainer
119344	Field Service Technician NETA 3
124387	Pathologists Freelance Remote Birmingham UK
157896	Nuclear Licensing Engineer
15572	Geospatial Trainer Outreach Coordinator
128268	Specialist Paediatric Speech Language Therapist Epsom
110587	Radiation Oncologist Arizona Blood Cancer Specialists
157499	GCP Data Engineer I Vietnam 12 mths contract
160033	Porter
34211	In Home Hybrid Board Certified Behavior Analyst BCBA
49218	Commissioning Authority
87062	Customer Success Manager SMB Position located in Leeds England
117816	Assistant Store Manager
4466	Manufacturing Specialist Fluent in French Freelance AI Trainer Project
170663	Registered Nurse RN Home Health Lake Charles
115692	Tower Technician Top Hand
154723	Personal Trainer Deland FL
72267	Tech 3
152542	Personal Trainer
106151	Security Engineering Tech Lead
178018	Elementary School Literacy Curriculum Writer Consultant
105665	Project Coordinator
105044	FPV Specialist
40970	Product Manager Supply Chain
56415	Service Associate Night
```

## Predictions — do not read until step 3

```
87498	UNKNOWN
148554	UNKNOWN
167309	UNKNOWN
135673	OUT
176018	UNKNOWN
148242	IN
161639	UNKNOWN
48214	UNKNOWN
58115	OUT
9690	IN
115725	UNKNOWN
36865	UNKNOWN
98478	IN
25839	IN
18723	OUT
107863	OUT
92311	OUT
7702	OUT
146520	IN
25699	OUT
161668	OUT
2681	OUT
38674	OUT
131811	UNKNOWN
90315	OUT
53426	UNKNOWN
81098	IN
17096	UNKNOWN
168521	OUT
144236	OUT
154850	OUT
84463	OUT
96187	OUT
75095	OUT
21729	OUT
4268	OUT
112007	OUT
81274	OUT
111442	OUT
139971	UNKNOWN
102166	UNKNOWN
129088	UNKNOWN
113542	OUT
51726	OUT
139414	UNKNOWN
67382	OUT
177719	UNKNOWN
103380	UNKNOWN
99262	OUT
126840	OUT
45965	UNKNOWN
15503	UNKNOWN
95738	UNKNOWN
152476	OUT
54497	OUT
13727	OUT
132843	UNKNOWN
155535	OUT
39710	OUT
166255	OUT
108912	UNKNOWN
81118	OUT
86875	OUT
116296	UNKNOWN
59350	UNKNOWN
42457	UNKNOWN
73127	UNKNOWN
177429	OUT
29138	OUT
67693	OUT
154334	OUT
4006	OUT
148094	OUT
125227	OUT
97486	OUT
31068	IN
118923	OUT
127169	UNKNOWN
95287	UNKNOWN
176566	OUT
69988	OUT
107694	UNKNOWN
61268	OUT
34454	OUT
164676	OUT
52409	OUT
110245	OUT
68875	OUT
86457	UNKNOWN
79549	OUT
23084	OUT
60359	OUT
7893	OUT
164717	UNKNOWN
150634	OUT
59183	UNKNOWN
177985	IN
37286	IN
31201	UNKNOWN
75560	OUT
56130	OUT
137663	OUT
28864	OUT
36009	OUT
17783	OUT
75379	OUT
48644	UNKNOWN
148895	IN
135301	UNKNOWN
65247	IN
94543	OUT
139307	OUT
143580	OUT
75437	OUT
58746	IN
145344	OUT
160046	OUT
33787	OUT
4176	OUT
38461	OUT
141057	UNKNOWN
126931	UNKNOWN
83726	OUT
66549	OUT
126278	OUT
108224	UNKNOWN
39764	UNKNOWN
99440	OUT
104411	OUT
129396	UNKNOWN
30274	UNKNOWN
37721	OUT
95297	UNKNOWN
6200	OUT
64250	OUT
161184	OUT
128524	UNKNOWN
97925	IN
6957	UNKNOWN
4761	OUT
126867	OUT
122123	OUT
14051	IN
84637	OUT
114194	UNKNOWN
29205	OUT
138449	IN
104465	UNKNOWN
107849	UNKNOWN
35179	OUT
168316	OUT
98379	OUT
86356	OUT
32495	OUT
18123	OUT
159444	OUT
142028	OUT
38441	IN
87449	OUT
83022	OUT
131469	OUT
137742	UNKNOWN
138695	UNKNOWN
116646	UNKNOWN
14737	UNKNOWN
34484	OUT
100493	OUT
78995	OUT
50358	IN
117645	UNKNOWN
160627	IN
153967	OUT
21347	OUT
115207	OUT
169095	UNKNOWN
55800	UNKNOWN
174006	OUT
122620	OUT
2347	UNKNOWN
97970	OUT
60113	OUT
104840	OUT
34468	OUT
169323	OUT
141910	OUT
154778	OUT
110471	OUT
47104	OUT
18227	OUT
156697	UNKNOWN
150006	UNKNOWN
15717	UNKNOWN
59992	UNKNOWN
82367	UNKNOWN
47192	OUT
42899	UNKNOWN
104798	IN
159840	UNKNOWN
51704	OUT
150990	OUT
136133	OUT
89468	OUT
80420	OUT
107192	OUT
1931	IN
47453	OUT
27109	OUT
113180	OUT
41552	UNKNOWN
110533	OUT
76052	OUT
62253	OUT
81238	OUT
69794	UNKNOWN
169060	OUT
50337	UNKNOWN
6750	OUT
30634	UNKNOWN
92637	OUT
122654	UNKNOWN
33409	IN
13687	OUT
140827	OUT
111282	OUT
74001	OUT
157032	UNKNOWN
144527	OUT
148989	OUT
118816	OUT
132966	UNKNOWN
8086	OUT
9182	OUT
8978	IN
83426	OUT
28200	IN
21814	UNKNOWN
114243	OUT
151415	OUT
164963	OUT
111754	OUT
169076	OUT
6235	OUT
76467	OUT
21382	UNKNOWN
73120	UNKNOWN
127946	OUT
110511	OUT
163895	UNKNOWN
152228	OUT
50063	UNKNOWN
58707	OUT
164595	UNKNOWN
49424	OUT
87842	OUT
138907	OUT
44053	IN
35399	OUT
81975	OUT
44081	OUT
56981	UNKNOWN
160135	UNKNOWN
142237	OUT
157866	OUT
96267	IN
151284	OUT
122570	OUT
97346	UNKNOWN
28004	UNKNOWN
59186	UNKNOWN
24617	IN
20970	UNKNOWN
13072	IN
156531	UNKNOWN
99686	OUT
173572	UNKNOWN
173374	OUT
174518	OUT
48469	OUT
99581	OUT
173334	OUT
102546	OUT
128040	OUT
32892	UNKNOWN
45066	IN
8123	UNKNOWN
45296	IN
43286	OUT
174600	UNKNOWN
61517	UNKNOWN
149591	OUT
164697	UNKNOWN
133672	OUT
4000	OUT
156414	OUT
167868	OUT
46392	OUT
111125	OUT
103128	OUT
89599	OUT
95600	OUT
23888	UNKNOWN
56379	OUT
129507	OUT
81337	OUT
132754	UNKNOWN
93325	OUT
34943	OUT
100277	OUT
23022	OUT
51020	IN
40089	OUT
8900	UNKNOWN
134310	IN
99507	OUT
152958	OUT
74296	IN
56717	OUT
24908	UNKNOWN
102954	UNKNOWN
39005	OUT
114748	OUT
154510	OUT
136751	UNKNOWN
29829	OUT
128861	UNKNOWN
152059	OUT
50615	IN
29993	UNKNOWN
100860	OUT
17187	UNKNOWN
143551	UNKNOWN
71597	UNKNOWN
44873	OUT
168796	OUT
12801	OUT
22901	OUT
46882	OUT
147494	OUT
4145	OUT
133950	OUT
50143	UNKNOWN
130957	OUT
9058	OUT
159711	UNKNOWN
137724	OUT
97689	UNKNOWN
7015	UNKNOWN
139260	OUT
70622	IN
98149	OUT
65983	UNKNOWN
128175	OUT
131046	OUT
172988	OUT
99796	OUT
107746	OUT
14310	OUT
50924	UNKNOWN
61439	OUT
155778	UNKNOWN
117225	UNKNOWN
94497	OUT
75091	IN
134326	OUT
16647	IN
101663	OUT
17084	UNKNOWN
4337	OUT
58384	UNKNOWN
9991	OUT
146969	UNKNOWN
33844	OUT
2945	OUT
109257	UNKNOWN
119623	UNKNOWN
116357	UNKNOWN
20828	UNKNOWN
19718	OUT
8607	OUT
16101	OUT
55961	OUT
16913	OUT
177518	OUT
35844	OUT
12376	OUT
64538	OUT
119981	OUT
83522	OUT
116803	OUT
17532	OUT
52888	IN
177893	OUT
61335	OUT
52805	UNKNOWN
87018	UNKNOWN
160872	IN
31104	UNKNOWN
152535	OUT
122379	UNKNOWN
61190	OUT
6418	OUT
9385	OUT
17992	OUT
52442	UNKNOWN
55677	UNKNOWN
44571	OUT
29478	OUT
103545	OUT
96194	OUT
9598	OUT
129377	UNKNOWN
3762	IN
10904	OUT
72250	OUT
151135	OUT
50068	UNKNOWN
106254	IN
168795	OUT
142195	IN
153586	OUT
63100	OUT
19232	UNKNOWN
89082	UNKNOWN
89048	OUT
94152	OUT
36277	IN
112794	OUT
76505	OUT
69116	OUT
60565	OUT
8350	UNKNOWN
58849	OUT
158904	OUT
53241	UNKNOWN
141977	OUT
149453	UNKNOWN
97762	OUT
101955	IN
4350	OUT
41762	OUT
128572	OUT
175257	OUT
71912	OUT
125352	UNKNOWN
36371	UNKNOWN
1482	OUT
158993	UNKNOWN
74005	OUT
118977	OUT
34760	OUT
130521	UNKNOWN
68536	UNKNOWN
53423	UNKNOWN
137943	IN
94885	IN
148494	OUT
45931	OUT
134443	IN
78194	OUT
12676	UNKNOWN
39573	OUT
146922	IN
112238	OUT
41755	IN
37803	OUT
89292	UNKNOWN
42238	UNKNOWN
83469	OUT
97209	UNKNOWN
71111	OUT
73815	OUT
128200	UNKNOWN
34472	OUT
152057	OUT
50483	OUT
177492	OUT
96836	OUT
59934	OUT
64207	OUT
163068	UNKNOWN
68716	UNKNOWN
147868	OUT
39678	UNKNOWN
155909	UNKNOWN
5987	OUT
111714	OUT
174465	UNKNOWN
31518	UNKNOWN
1327	OUT
56635	UNKNOWN
165223	OUT
57478	OUT
9968	OUT
68908	IN
125513	OUT
88191	OUT
136125	UNKNOWN
153437	OUT
122958	UNKNOWN
20492	IN
132934	UNKNOWN
161278	OUT
118160	OUT
120437	OUT
62009	OUT
95684	OUT
111321	OUT
60645	IN
113068	OUT
39905	UNKNOWN
70073	UNKNOWN
29823	OUT
129531	OUT
77389	UNKNOWN
14595	OUT
94793	OUT
9392	UNKNOWN
129612	OUT
157739	UNKNOWN
172472	IN
141060	OUT
20877	UNKNOWN
155203	OUT
167386	OUT
26860	OUT
134996	IN
54021	OUT
153254	OUT
72793	OUT
13439	OUT
1801	UNKNOWN
107385	OUT
131926	UNKNOWN
4440	OUT
65470	OUT
46063	OUT
127276	OUT
168071	OUT
51227	OUT
47840	OUT
65842	OUT
68827	OUT
143854	OUT
39350	OUT
96146	UNKNOWN
175770	OUT
79330	OUT
96951	UNKNOWN
128106	OUT
176349	OUT
136773	IN
156438	OUT
36912	UNKNOWN
135214	OUT
141759	UNKNOWN
110446	UNKNOWN
54140	OUT
106718	OUT
27597	OUT
19218	UNKNOWN
130465	OUT
60509	OUT
22073	OUT
73394	OUT
68056	OUT
176596	OUT
123266	OUT
16516	IN
54638	OUT
158042	UNKNOWN
103303	IN
55802	UNKNOWN
108841	OUT
175158	OUT
177324	IN
57551	OUT
88737	OUT
57488	OUT
49025	UNKNOWN
97823	OUT
87188	OUT
26813	OUT
32332	UNKNOWN
160696	OUT
30872	IN
140486	IN
69748	OUT
7430	UNKNOWN
162065	IN
53917	UNKNOWN
109027	OUT
38521	OUT
9730	OUT
148870	OUT
170577	OUT
60095	OUT
151023	OUT
24430	OUT
1304	OUT
41578	UNKNOWN
11950	OUT
177368	UNKNOWN
52231	UNKNOWN
31946	IN
69293	OUT
5149	OUT
151471	OUT
41234	OUT
79678	OUT
23740	OUT
50932	UNKNOWN
100078	UNKNOWN
73903	OUT
58432	OUT
18302	OUT
166781	OUT
112443	OUT
101484	OUT
106915	IN
109166	OUT
81722	OUT
60018	UNKNOWN
4872	OUT
96185	OUT
35743	UNKNOWN
17110	UNKNOWN
141056	OUT
171776	OUT
21511	OUT
144539	OUT
83148	OUT
37568	IN
177684	OUT
124925	OUT
103471	UNKNOWN
72473	OUT
51048	UNKNOWN
160952	OUT
147504	OUT
96635	IN
129433	IN
177868	UNKNOWN
148547	UNKNOWN
63633	UNKNOWN
61068	UNKNOWN
39846	UNKNOWN
86006	OUT
93428	UNKNOWN
16400	OUT
73525	OUT
55553	UNKNOWN
33977	OUT
27605	OUT
79226	UNKNOWN
176854	IN
97129	OUT
87998	UNKNOWN
70827	OUT
67337	IN
16082	OUT
43321	OUT
16523	OUT
156872	UNKNOWN
153116	OUT
20450	OUT
49889	OUT
162718	OUT
65235	UNKNOWN
30340	UNKNOWN
122655	UNKNOWN
30223	UNKNOWN
46443	OUT
146267	OUT
79787	UNKNOWN
169763	UNKNOWN
33316	UNKNOWN
7929	OUT
45767	OUT
77238	OUT
138565	IN
163305	OUT
63484	UNKNOWN
66796	OUT
32247	OUT
114498	UNKNOWN
145518	UNKNOWN
34792	OUT
172271	IN
51230	OUT
19107	OUT
173013	OUT
52529	OUT
65135	UNKNOWN
61787	OUT
110343	OUT
66517	UNKNOWN
64452	UNKNOWN
39027	OUT
94528	OUT
149458	UNKNOWN
1321	OUT
90148	UNKNOWN
79448	UNKNOWN
148313	UNKNOWN
29849	UNKNOWN
9798	IN
3278	UNKNOWN
131254	UNKNOWN
150130	UNKNOWN
157476	UNKNOWN
126033	OUT
158221	OUT
156186	OUT
58954	OUT
61915	OUT
99781	OUT
25650	OUT
50029	UNKNOWN
1335	OUT
147928	OUT
103805	OUT
174193	OUT
42870	UNKNOWN
88185	UNKNOWN
27264	OUT
155968	OUT
7820	OUT
118065	OUT
22449	OUT
11316	UNKNOWN
95954	UNKNOWN
6128	OUT
2428	UNKNOWN
75609	OUT
167867	OUT
22643	OUT
71450	OUT
124109	UNKNOWN
132516	OUT
150894	OUT
42723	UNKNOWN
160792	OUT
32766	UNKNOWN
57707	OUT
134578	IN
136367	UNKNOWN
28922	UNKNOWN
86296	OUT
154556	OUT
57167	OUT
166150	OUT
29349	OUT
163959	UNKNOWN
25993	OUT
17816	OUT
71440	UNKNOWN
83820	UNKNOWN
81458	UNKNOWN
155870	IN
178887	UNKNOWN
81984	IN
55749	UNKNOWN
164626	OUT
36351	IN
160402	OUT
146052	UNKNOWN
38508	OUT
127608	UNKNOWN
45307	UNKNOWN
119325	OUT
21850	OUT
52507	OUT
7547	OUT
11917	IN
90140	OUT
46052	OUT
18833	IN
170036	UNKNOWN
72818	UNKNOWN
30257	UNKNOWN
8765	OUT
168902	OUT
89989	IN
27929	OUT
35826	OUT
95667	UNKNOWN
122023	OUT
63425	OUT
40274	UNKNOWN
173366	OUT
132443	OUT
89934	UNKNOWN
119769	UNKNOWN
142768	UNKNOWN
91807	OUT
12558	UNKNOWN
113241	UNKNOWN
41399	OUT
7895	OUT
96039	OUT
21091	UNKNOWN
127943	OUT
148803	IN
46520	IN
169498	OUT
56212	OUT
120330	IN
145754	IN
173155	UNKNOWN
68484	UNKNOWN
177307	OUT
12922	IN
102771	OUT
44396	UNKNOWN
53894	OUT
23637	OUT
85424	OUT
153061	OUT
66462	OUT
64440	OUT
30066	UNKNOWN
134956	OUT
37886	UNKNOWN
137970	IN
33476	OUT
98918	UNKNOWN
169314	OUT
53353	OUT
166290	UNKNOWN
156570	UNKNOWN
80655	UNKNOWN
153093	OUT
149670	OUT
84153	OUT
90872	OUT
131371	OUT
6364	OUT
4752	OUT
87126	IN
153958	OUT
47748	OUT
131796	OUT
123529	OUT
134254	IN
123706	IN
19206	UNKNOWN
67165	IN
50065	OUT
129244	UNKNOWN
26078	OUT
34615	OUT
136882	OUT
9923	OUT
134293	OUT
111944	OUT
152901	OUT
151051	OUT
125760	IN
140423	UNKNOWN
58028	OUT
32987	UNKNOWN
115385	UNKNOWN
10009	OUT
90924	OUT
15259	OUT
40662	UNKNOWN
48976	IN
73539	OUT
39886	OUT
19131	OUT
23538	OUT
67113	UNKNOWN
102044	UNKNOWN
148889	IN
47506	OUT
132461	OUT
41077	IN
178449	OUT
89152	UNKNOWN
32784	OUT
72406	UNKNOWN
51091	OUT
77147	UNKNOWN
12687	OUT
175153	UNKNOWN
1874	IN
74874	OUT
58540	UNKNOWN
7887	OUT
176146	UNKNOWN
73320	OUT
115863	OUT
158770	UNKNOWN
8275	OUT
178482	UNKNOWN
87182	UNKNOWN
147736	OUT
62608	OUT
75974	UNKNOWN
167014	OUT
49884	UNKNOWN
118151	OUT
58236	OUT
161768	OUT
167992	OUT
140165	OUT
72375	OUT
27967	OUT
130992	IN
7274	OUT
163803	UNKNOWN
74211	OUT
47999	UNKNOWN
102966	UNKNOWN
105851	UNKNOWN
67311	UNKNOWN
50192	IN
68832	UNKNOWN
37543	OUT
27122	OUT
44821	OUT
40520	UNKNOWN
122519	OUT
157373	OUT
171231	UNKNOWN
74913	OUT
156946	OUT
81080	OUT
169516	OUT
119845	UNKNOWN
147567	IN
28381	OUT
29322	IN
116086	OUT
65763	OUT
47970	OUT
168563	OUT
75475	UNKNOWN
154197	OUT
93644	OUT
30521	UNKNOWN
57128	OUT
93924	UNKNOWN
32186	OUT
80809	IN
149866	UNKNOWN
45809	UNKNOWN
163452	OUT
54332	OUT
99134	OUT
106600	OUT
69752	UNKNOWN
85985	UNKNOWN
125056	UNKNOWN
177370	UNKNOWN
171648	OUT
53951	IN
97330	OUT
137127	UNKNOWN
49953	UNKNOWN
138542	OUT
112057	UNKNOWN
80715	OUT
98037	UNKNOWN
90386	OUT
1555	UNKNOWN
77809	UNKNOWN
43625	UNKNOWN
117855	OUT
22484	OUT
142166	OUT
165995	OUT
23177	OUT
80209	UNKNOWN
79546	OUT
152854	OUT
119344	OUT
124387	OUT
157896	OUT
15572	OUT
128268	UNKNOWN
110587	OUT
157499	IN
160033	OUT
34211	OUT
49218	UNKNOWN
87062	OUT
117816	OUT
4466	OUT
170663	OUT
115692	OUT
154723	OUT
72267	UNKNOWN
152542	OUT
106151	IN
178018	UNKNOWN
105665	OUT
105044	UNKNOWN
40970	OUT
56415	OUT
```
