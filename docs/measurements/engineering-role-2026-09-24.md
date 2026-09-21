# Iteration 4 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. Criterion revision `33113b2`, unchanged.

The filename carries 2026-09-24 rather than the day it ran, for the reason iteration 3 gave: the
three earlier iterations already hold the three days before it, and the next session finds its
input by taking the newest file.

This iteration labelled the 1000 rows iteration 3 drew, scored iteration 3's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-23.tsv`; they were written to disk before any
rule of the classifier was read.

## The numbers

The two rates score **iteration 3's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change.

| Gated number | Iteration 3 | Iteration 4 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | 1.00% | **1.17%** (7 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | 8.00% | **7.00%** (7 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 48.68% | **41.56%** | fell < 1 pp |

The corpus after the change:

| | Iteration 2 | Iteration 3 | Iteration 4 |
| --- | ---: | ---: | ---: |
| `IN` | 15.58% | 15.27% | **16.47%** (29 490) |
| `OUT` | 21.17% | 36.05% | **41.98%** (75 179) |
| **`UNKNOWN` — the gated share** | 63.25% | 48.68% | **41.56%** (74 429) |
| &nbsp;&nbsp;of which `unruled` | 39.99% | 29.64% | **22.52%** (40 332) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 21.28% | 17.60% | **17.60%** (31 523) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.98% | 1.44% | **1.44%** (2 574) |

The whole of the fall is `unruled`, and the other two reasons did not move at all — to four
significant figures `domain_ambiguity` is the same share it was, and `scope_ambiguity` moved by
one vacancy. That is the shape the criterion predicts: `unruled` is ours and falls when we add
rules, and the other two are the world's.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 3 called `UNKNOWN`, I
labelled 23 `IN`, 236 `OUT` and 41 `UNKNOWN`. One unknown title in thirteen is an engineering
role, against one in eighteen last iteration — the pile got *richer* in `IN` as it shrank, which
is what harvesting `OUT` out of it does. It still hides three times the density of `IN` that the
`OUT` stratum does (23 of 300 against 7 of 600), so recall remains worse than the miss rate says.

## The exit gate

Not met. Both rates pass for the second iteration running — the miss rate at 1.17% and the false
accept rate at 7.00% — but the unknown share fell 7.12 points where the gate asks for less than
one, so the two-consecutive count starts again from here on that condition.

## Cost

Whole-word, case-insensitive matching over the cleaned title and nothing else. A full pass over all
179 098 cleaned titles takes **0.241s** in memory, against the criterion's ten-second limit. The
endpoint's 41s wall clock is the database.

## What changed, and why

Every change below is grown from a row where my label and iteration 3's answer disagreed.

**`technical` is now a software qualifier**, and it is the single largest source of the `IN` growth.
Under an engineering-capable head the word names a software domain in this corpus — `Technical
Project Manager`, `COUPA Technical Functional Lead` — and where it does not, the head or a marker
already decides: `Technical Recruiter` on a never-engineering head, `Technical Sales Manager` on
the `sales` marker, `Technical Project Manager Industrial Automation` on `industrial`. The one row
it read wrongly, `Weld Technical Specialist II`, is what bought the new `weld` marker.

**A ruling for the engineer a customer relationship is built around.** `technical account manager`,
`technical account management` and `customer success engineer` are all `IN`. All three were misses
in the same way: the commercial markers iteration 3 added — `account`, `customer success` — read
the customer as the domain, when the customer is who the work is *for*. A ruling is the right shape
here precisely because it runs before step 4.

**A ruling `embedded software engineer` → `IN`.** `Embedded Software Engineer Multicore Platforms
Avionics Networking` was a miss on the `avionics` marker. The criterion's Q2 settles it — code is
the primary artifact however physical the machine it runs on — and a ruling is the only shape that
does not contradict the criterion's own worked example, `mechanical software engineer`, which stays
`OUT`.

**`rocket` is no longer an off-domain marker.** It cost two misses, `Staff Back end Engineer Rocket
Pay` and `Manager Back end Engineering Rocket Pay`, where it read a payments product's name as a
motor. Nothing was lost: every rocket-engineering title in the sample is decided by `propulsion`,
`launch`, `mechanical` or `controls` anyway.

**Consultant is now engineering-capable.** It was a domain-free head that could carry no
engineering work, which left every `Salesforce`, `SAP`, `cloud` and `AI` consultant `unruled`. The
head is domain-free, so no title moved from `OUT`; what it costs is the hospital consultant —
`Consultant Gastroenterologist` now reads `domain_ambiguity` rather than `unruled`, because the
first head named is the head and `consultant` comes first. That is a move inside `UNKNOWN`, with
the wrong reason attached to it, and it is recorded in the tests.

**Two new heads, `researcher` and `fellow`**, for `Staff Applied AI Researcher Agentic Search` and
`Machine Learning Fellow`, which named no head at all.

**Spanish and Portuguese heads.** `ingeniero`, `engenheiro`, `desarrollador`, `desenvolvedor`,
`programador`, `analista`, `arquitecto`, `arquiteto` and their feminine spellings, plus the
never-engineering `vendedor`, `operador`, `enfermero`, `auxiliar`, `agente` and the French `aide`
and `vendeur`. Iteration 3 left multilingual heads alone on the grounds that half a head list in
one language is worse than none; this is the head half of two languages and nothing else, which is
the half that works — the words this corpus uses for a software domain (`software`, `data`,
`cloud`, `backend`) are the same in all three languages, so the English qualifier list already
covers them. `Engenheiro a de Software Assistente e Júnior` was the row that forced it.

**Sixteen never-engineering heads** from the `UNKNOWN` pile, each one a function no software domain
turns into engineering work: `anesthesiologist`, `gastroenterologist`, `histopathologist`,
`neurologist`, `podiatrist`, `psychotherapist`, `chemist`, `millwright`, `laborer`, `custodian`,
`estimator`, `buyer`, `producer`, `compositor`, `generalist`, `inspector`, `planner`,
`paraprofessional` and `agent`. `agent` is singular on purpose: `agents` would have caught
`Frontier Agents Engineer Applied AI`.

**Twenty-four new off-domain markers**: `hardware`, `equipment`, `mep`, `gas`, `weld`, `welding`,
`solar`, `grid`, `energy`, `geotechnical`, `telecommunications`, `vehicle`, `door`, `cable`,
`sanitation`, `laboratory`, `microbiology`, `behavior`, `behavioral`, `hr`, `aviation`,
`semiconductor`, `wafer`, `rfic` and the phrase `silicon engineering`. Three of them came straight
from false accepts — `Board Test Engineer`, `MEP Manager Systems Integration` and `Test Engineer
High Pressure Gas Systems` — and the rest from the technician and engineer titles the `UNKNOWN`
pile still held. `behavior` matters more than it looks: the Registered Behavior Technician family
is large and it was landing `unruled` every time.

**Four rulings on the shop floor's `associate`.** `sales associate`, `service associate`,
`operations associate` and `store associate` are `OUT`. The word cannot be a head: `associate` is a
seniority in engineering and a job title in retail, and any head reading of it turns `Associate
Software Engineer` into a shop assistant. This is the one place in this iteration where a ruling is
right and a head is wrong, and there is a test pinning `associate software engineer` to `IN` so
that a later session does not undo it.

**Six smaller rulings and a batch of qualifiers**, each from a row the sample showed:

- Rulings `enterprise architect`, `solution architect`, `member of technical staff` and `sdet` →
  `IN`, for four titles that named no head the lists reached.
- Rulings `after sales engineer` → `OUT` and `field application scientist` → `OUT`. The first has
  to be three words long to outrun the `sales engineer` ruling sitting inside it.
- A ruling `graduates ai training` → `OUT`, for the two false accepts `Database Administrator
  Graduates AI Training Lyon France` and its Querétaro twin. The annotation posting names the
  profession it recruits from first, so it has to outrun the ruling on that profession.
- A ruling `board test engineer` → `OUT` and `general manager` → `OUT`.
- New software qualifiers `llm`, `nlp`, `computer vision`, `generative ai`, `informatics`,
  `netsuite`, `servicenow`, `workday`, `gameplay`, `unreal`, `quant`, `quantitative`, `rendering`,
  `graphics` and `detection engineering`.

## What this did not fix

One miss and one false accept survive on purpose.

- `Software Engineer Implant Manufacturing` — `manufacturing` is a marker and step 4 says a marker
  beats a software qualifier. Iteration 3 recorded the same trade on `warehouse` and `retail`, and
  the reasoning has not changed: a ruling on `software engineer` would fix the family and would
  contradict the criterion on its own worked example. Note that `embedded software engineer` is
  exactly this shape and *was* ruled — the difference is that `embedded` names how the software is
  built and `implant` names what it is built for, which is what step 4 is about.
- `Specialist QA Document Training` — `qa` under the `specialist` head. The only fix is to drop
  `specialist` as a head, which would cost `IT Security Specialist` and `AI Deployment Specialist`.

Rows this change newly gets wrong, as far as this sample shows: none in the `IN` or `OUT` strata.
The two soft costs are both inside `UNKNOWN` — the hospital consultant above, and the eleven rows
this sample's classifier called `IN` that I labelled `UNKNOWN`, nine of which are the `systems`
and `integration` qualifiers on a bare engineer (`Lead Engineer L1 Integration`, `Systems Engineer
Fault Management`, `Staff Systems Engineer C2 Integration Active Clearance`). Those two words are
the widest qualifiers on the list and are worth a look if the false accept rate ever rises.

## What the next session should look at

`unruled` is still 22.52% of the corpus and it is still the whole of the gated number's movement,
so the question stays what is left in it.

`director` was considered for a third time and left off again. The argument has not changed and
this sample sharpens it: adding it as a domain-bound engineering-capable head would win `Director
Cybersecurity Operations and Platform Delivery` and `Director Program Management L7 Catalog AI
Quality`, and would convert `Director Global Programs`, `Director of Aviation`, `Director Paid
Social`, `Director Workplace Benefits`, `Director Business Development Genomics` and — worst —
`Director Securities Corporate Counsel` from `OUT` to `domain_ambiguity`, because `director` is
named before the `counsel` that decides it. Six lost for two won. The same argument covers `vp`,
`head` and `president`.

`product manager` is the other phrase left alone, and for the opposite reason: my own labels split
on it. I called `Group Product Manager Signals Identity` `IN` and `Talent Pool for Growth Product
Manager` `UNKNOWN`, which is `scope_ambiguity` by definition. Today the modifier decides it, which
is the honest reading; ruling the phrase would override the markers that make `Product Manager
Retail Operations` `OUT`. Leave it unless a later sample shows the split is not real.

`operations` was considered as a marker and rejected: it would decide `Cloud Operations Engineer`
and `Staff Software Engineer Operations Research` `OUT`, and both are `IN`. The `Operations
Manager` family is large and still `unruled`, so if a later session wants it, the shape is a ruling
on the exact phrase and not a marker.

Watch `technical`. It is the widest qualifier added this iteration and it decides on its own under
every engineering-capable head; one bad row in the next sample's false accepts is enough to want it
narrowed to `technical <head>` phrases. `account` and `controls`, the two markers iteration 3 was
least sure of, each recorded one more cost this iteration and both were paid by the new rulings
rather than by dropping the marker.

The loose end iteration 1 created is still open: `docs/engineering-role-criterion.md` points at
`docs/engineering-role-seed-lists.md`, which no longer exists. The criterion is the developer's
document and changes only in a grilling session, so the dangling reference is left alone.

## Held out of the draw

The 3000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 172 679 of the
179 098 rows were eligible.

## The sample for iteration 5

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
152937	Personal Trainer
137676	Digital Social Platform Editor
75370	Manager Market Growth Operations
160728	Graduate Associate Client Services 2027 Start
52329	Assistant Clinical Director CCS
30411	Treasury Analyst
107632	Operations Associate Teleport Campus
85910	Recruiting Sourcer
104245	Bartender
36674	Software Engineer Observability
133040	Business Development Representative San Antonio
106829	Investment Sales Broker Seattle
36546	Territory Sales Expert Expert de Vendas Locais Sao Luis MA Hybrid Remote
83814	Prevenzione Incendi Categoria Protetta L 68 99
78618	Desenvolvimento Full Stack Python e Typescript
114134	Lead Certified Nurse Midwife CNM
82445	IT Logistics and Warehouse Specialist
81517	Account Executive NW SLED
28129	Java Developer
146853	Ecommerce Performance Marketing Manager Korea
132646	Manager Finance
98490	Alternative Delivery Manager Heavy Civil Construction Michels Preconstruction Services Inc
120022	Event Assistant Volunteer
17370	Power Electronics Team Lead
90276	Technical Account Manager Lead AMER
40724	Experimentation Analyst
174668	Healthcare Architectural Team Leader
41577	쿠팡로지스틱스서비스 물류 수요예측 및 운영기획 담당자
126643	Band 6 Neurological Physiotherapist Trauma Orthopaedic London
37974	Manager CS AI Curriculum
13635	Stormwater Construction Foreman
102909	Head of Accounts Payable
148907	Machine Learning Engineer Active TS SCI Clearance
9285	Federal Government Relations Director
138044	Data Engineer fixed term contract
59106	Patient Care Coordinator CARDIO4Cities Brunei Contract role
4664	Slovak Language Specialist Freelance AI Trainer Project
92607	Quality Document Control
85762	Welder and Fabrication Technician
168648	Emergency Veterinarian Part Time Brentwood CA
129455	Hardware Verification Validation Engineer
68509	Data Scientist Payment Intelligence
174148	Data Engineer Quantitative Platform
155991	D.C Waitlist Contract Psychiatric Nurse Practitioner
62824	Affiliate Partners Marketing Manager Parental Leave Cover all genders
118750	Quant Library Developer Macro Technology
154397	Personal Trainer
76236	Purchasing Manager
168639	Emergency Veterinarian Orange Village OH
125215	US Residents Survey Participants Austin
56693	AV Technician Events
64352	Project Manager Utility Construction
107398	US Events Manager
122930	Data Entry Clerk Graduates AI Training Morelia Mexico
56137	Personal Training Manager
14459	Security Engineer Data Platform
38984	Neuroscience Therapeutic Sales Specialist Manhattan NY
128731	Pre Sales Systems Engineer Higher Education Mid Atlantic
120900	Private Equity Conference Volunteer
154596	Personal Trainer
84454	Cheil Agency Network Director of People Operations Analytics
130239	Director Forward Deployed Engineering
23625	Special Needs Job Coach
114304	Engineer Embedded Sensors and Safety Software
177203	Technical Program Manager Pricing and Manufacturability
41844	Manager Security Engineering DART Taiwan
136362	Enterprise Account Executive
103943	Account Sales Representative
110332	Primary Care Physician Preston Center
95259	Customer Success Operations
123018	Doctors AI Training Dallas US
117192	Global Deal Desk Manager
106936	Mission Trainer Lead
77103	Graduate sales executive at HRtechX
63479	FP&A Manager
151495	Personal Trainer
76834	Community Manager Lease Up
37166	Sales Manager
171509	Exploit Developer US
59347	Registered Nurse Home Health
111370	ABA Paraprofessional RBT Queens
162769	Environmental Specialist
24780	Psychologische Berater in
171224	Program Manager V Aerospace
25723	Clinical Operations Specialist
146899	Systems Engineer
44138	Delivery Driver
23683	Speech Language Pathologist SLP ST Home Health
162631	3D Artist
58374	Digital Court Reporter Contractor
173655	Sales Development Representative
165269	Sales Operations Manager
102179	Employment Law Attorney
148317	Data Scientist Core Infrastructure
93118	Adv Modena MO
79164	Registered Behavior Technician Alma
10490	Frontend Software Engineer
85457	Account Executive In Territory SF Bay Area LA Orange County San Diego
121633	AI Trainer Aerospace Engineers CAD Expertise Remote Advisory Francia
101592	Consumer Engagement Ambassador Monster Energy
9938	Data Engineer
93832	Sales Representative Telehealth Patient Conversion
126835	Band 7 CT Radiographer Glasgow
71181	Substitute Teacher
92001	Team Manager Safegaurding
21551	CHHA Certified Home Health Aide ACSP
100382	STRONG Pilates Fitness Instructor
3704	Manager Software Engineering App Experiences
149429	Agente di commercio Procacciatore P IVA Settore pagamenti
87901	Veterinary Medical Oncologist
151910	Personal Trainer
26509	Strategic Account Executive UK
101482	Producer
87242	Product Manager Assisted Autonomy
62916	Research Associate Molecular Biology and Sequencing
7284	Operations Associate Part Time Americana at Brand
138701	Financial Operations Associate
126809	Band 7 Cardiac MRI Radiographer Central London
57866	SkillBridge Program
66962	Sports Data Collector Football Santa Cruz Costa Rica
139314	Training Coordinator
98911	Project Manager Mission Critical Data Center Michels Underground Cable Inc
73669	In Home Staff Assist Thousand Oaks CA 91359
172585	Maintenance Technician EPA Certified
123845	Javascript Developers AI Training Omaha US
151050	Personal Trainer
156870	Associate Talent Acqusition
68669	Campaign Operations Manager Individual Contributor
34685	Spanish Speaking Behavior Technician
20525	2027 Graduate Architectural Technician
14242	Electrical Design Engineer Power Electronics Thermal Systems
106614	Software Engineer
90641	Occupational Therapist Adults Social Care Independence Access
80754	Account Executive MidMarket LATAM
155145	Personal Trainer Tyngsborough MA
178051	School Operations Associate or Manager 2026 2027
150973	Personal Trainer
99175	Maintenance Technician Moon Gate Plaza
9566	Product Software Intern
99423	Enterprise Account Executive Chicago
175581	AV Implementation Director
67516	Client Accounting Services Internship
26454	Finance Operations Transformation Manager
175506	Associate Director Implementation Planning UK
87963	Client Manager
154877	Personal Trainer Lake Catherine LA
113865	Auxiliaire de vie
154465	Personal Trainer
114681	Lead Mechanical Engineer Structures
152563	Personal Trainer
109609	Project Manager II
150072	Class A Commercial Driver
59883	Apartment Maintenance Manager
63306	Sales Development Representative
136614	Digital Media Manager
14598	Marine Mechanical Engineer
21253	1 1 Nurse LPN
143933	Mechanical Engineer Data Center MEP Systems
16493	Engineer Field Process New Graduate Early Career
61490	Global Events Logistics Manager
41671	CFN Staff Back end Engineer Lending Engineering
35201	Finance Industry Event Volunteer
158755	Marketplace Manager THG Nutrition Myprotein
57332	Heavy Equipment CDL Driver
13738	Project Manager
45843	Partner Marketing Manager LATAM
116045	Retail Coordinator Baltimore Part Time 20 hours week
10524	Hardware Test Engineering Manager
11360	FPGA Verification Engineer
55358	Group Fitness Instructor
38934	Neuroscience Therapeutic Sales Specialist Albuquerque NM
3464	LA Kings Graphic Designer
71691	Marketing Events Programs Specialist
77993	Engineering Manager Assistant Vice President Vice President
3127	Commercial Counsel Deal Desk
4748	Training and Development Specialist Freelance AI Trainer Project
47871	Account Executive Mid Market
105681	Systems Engineer
103071	Site Work Foreman
60855	Insurance Producer
11804	Software Engineer Satellite Command and Control
75042	Cannabis Facility Custodian
20175	Specialty Account Manager Auvelity Springfield MA
42784	Alarm Monitoring Advisor
61883	Strategic Finance Graduate Program
103814	Dishwasher Porter
141302	Independent Sales Representative
55458	Group Fitness Instructor
113019	Behavioral Health Physician Reviewer Part Time
42370	Director Affiliate Marketing
50955	Future Opportunities at EarnIn
93094	Dermatologist Baton Rouge LA
5139	Director AI
54051	Assistant Fitness Manager
99532	AI Engineer
59166	Director of People
150702	Personal Trainer
175448	Advanced Analytics Executive GOC
11219	Contracts Manager
137346	Specialist Aftersales Cost Monitoring
59000	Veterinary Technician Emergency
11503	Materials Process Engineer Rocket Motor Systems Polymeric Ablatives
164552	Assistant Project Manager
129764	Cloud Infrastructure Support Engineer TS SCI Eligible
88693	Growth Product Marketing Manager
4284	Hindi Language Specialist Freelance AI Trainer Project
4760	Urdu Language Specialist Freelance AI Trainer Project
63837	Executive Assistant Chief Hardware Officer Head of Autonomy
164893	Events Executive
38608	Search Backend Engineer
44371	Wealth Advisor
1142	Interested in Working With Us
94679	Temporary Assistant Manager Polo Park
173352	Veterinarian South Texas Veterinary Clinic Beeville TX
81725	Trading Solutions London
112003	Account Executive Hotels
134972	Sales Development Representative German speaking
166282	Surgical Tech
30015	Lead Information System Security Officer ISSO
139804	Full Time Driver AM
122149	AI Training Fluent Japanese Speakers Kumamoto Japan
127296	Consultant Child and Adolescent Psychiatry CAMHS Greater London
63723	Software Engineer C++
161779	Host
140719	Aesthetician PT
44086	Regional Marketing Manager DACH
68916	Counsel
151136	Personal Trainer
9548	Reliability Leader
132944	Renewals Manager EMEA
166661	Propulsion Engineering Internship Spring 2027
94290	Surgical Assistant
55999	Operations Manager
84665	Assistante de vie
79082	Site Superintendent
76187	Project Manager Construction
176744	Software Engineer Search Ranking
34573	Registered Behavior Technician RBT
62335	Macro Research Content Strategy Associate
154933	Personal Trainer Memphis TN
151188	Personal Trainer
101590	Consumer Engagement Ambassador Monster Energy
103914	Backend Software Engineer Nas.com
88568	Intermediate Controls Engineer
168553	Emergency Veterinarian Ann Arbor MI
177298	Customs Operations Specialist
87193	Field Operations Supervisor I DRC
59918	Assistant Community Manager Affordable Housing
115538	Financial Solutions Guide 2
21715	Direct Support Professional
60056	Team Executive Assistant
124337	Operations and PMO Leaders Subject Matter Expert San Francisco US
114493	Display Account Manager
45461	Staff Product Manager Security
54554	Certified Personal Trainer
47018	Account Supervisor
59560	General Accountant US CPA US Certified
93575	Brakes and Steering Engineer
4733	Swedish Voice Actor Freelance AI Trainer Project
113516	Aide à domicile
88749	Mission Operator Palo Alto CA Third Shift Contractor
30023	OSP ISP Engineering Support
149395	Agente di commercio Procacciatore P IVA Settore pagamenti
72690	Full Time Licensed Therapist Maryland
167794	Federal Enterprise Account Executive
90801	Reablement Occupational Therapist Hospital Discharge
168141	Corporate Accounting Consolidations Manager SEC
130179	Regional Sales Partner Mideast
24757	Mental Health Counselor New Comp Benefits
82684	Sales Assistant Part Time Benevento C.C Buonvento
33381	Digital Workspace Product Manager
102570	Entry Level Civil Engineer Transportation Highway DOT
54059	Assistant Fitness Manager
9711	Support Analyst II
3618	Product Manager Technical
37145	Sales Director
174490	Wolt Market Budaörs Warehouse Shift Lead
122139	AI Training Fluent Japanese Speakers Himeji Japan
140509	Robotics Engineer Manipulation
154459	Personal Trainer
166105	Business Development Manager
153242	Personal Trainer
55744	Manager of MarTech
163552	Clinical Specialist Region Atlanta GA
94264	Staff Nurse NICU PT D E
142314	Brand Manager Mobile Marketing Contract
89493	Field Training Specialist Commercial Laundry
62575	Strive Guide
27413	Psychiatrist MD
83576	Flex Sales Assistant JD Nijmegen
41623	쿠팡 로켓그로스 신규 채널 마케팅 담당자 6개월 계약직
122720	Computer Sciences Graduates AI Training Brighton UK
36210	Government Travel Consultant
40378	Food Safety Supervisor
142624	Release Planning Coordinator
92983	Loonshot Games 일본 사업 BD PM 5년 이상
63422	Your next opportunity at Flipdish Pakistan Talent Community
117894	Customer Service Representative
147187	Design Reliability Engineer System Safety
110258	Per Diem Primary Care Physician Casual Employee DC Farragut Square Office
63576	Shop Employee
109740	Surveyor in Training
117850	Assistant Store Manager
135000	Project Manager Professional Services
19266	Robot Service Technician Assistant Part Time Weekend
76507	Recruiter Contract Role
174585	Product Manager Growth
56314	Service Associate
109555	Mechanical Engineer Data Centers
44187	Lead Store Associate
3475	Manager Maintenance United Heritage Hall
88209	Practice Leader Geotechnical Engineering
40836	Data Center Technician Caledonia MI
103359	Account Executive Las Vegas NV Remote
36779	Consulting Project Team Lead New Product Planning Multiple offices
47524	Commercial Door Technician
4788	Welsh Language Specialist Freelance AI Trainer Project
176045	Planning Lead
165564	Account Executive
144863	Mechanical Design Engineer Gateways Starlink
171360	Fire Sprinkler Foreman
156215	Security Engineer Customer Transparency
131969	Manager Payroll Operations Sub Saharan
153972	Personal Trainer
171297	Thermal Engineer II
94459	Part Time Floor Leader South Shore Plaza
151332	Personal Trainer
32095	Network Engineer 2 Cisco Arista
35703	Sales Executive CUPRA SEAT
28054	Certified Medical Assistant
58473	PRN CV Anesthesiologist Texoma Medical Center Denison TX Guaranteed Hours
135356	Assistant Retail Store Manager
151252	Personal Trainer
96738	Endocrinology Nurse Practitioner Physician Assistant
45681	Customer Success Manager Denver
25597	Operations Partner Detroit MI
116445	Analog Mixed Signal IC Design Lead Engineer
1233	Associate Director Vendor Relationship Management
51899	Assistant Commercial Insurance Client Manager
124896	Slovenian Fluent Speakers AI Training Novo Mesto
166190	Housekeeper
133756	Head Of Product Marketing Europe Or Brazil
171261	Proposal Writer Proposal Architect Aerospace
84162	Assistant Store Manager Cobham 23 High Street
41795	쿠팡 Financial Planning Analysis Central Functions
136717	Tax Manager Core
125814	Adult Acute Speech and Language Therapist
128998	Account Executive New Logo Dallas
57736	Rental Coordinator
153157	Personal Trainer
13343	Events and Ministry Coordinator Onsite
20878	Intelligence Operations Integrator OCONUS
55612	Kids Club Associate
141251	Account Executive
57705	Purchasing Coordinator
173111	Chalco Hills Animal Hospital Omaha NE Associate Veterinarian
32926	Head of Platform Engineering Reliability Control
21348	Adult Trach and Vent Registered Nurse RN
100719	Inside Account Executive
90538	Healthcare Assistant Disabilities
89984	Data Center Site Operations Manager
112396	Home Health Registered Nurse Grand Haven 10 000 Sign On Bonus
74541	Assistant Merchandise Planner
151779	Personal Trainer
144035	New Graduate Engineer Mechanical Design Starshield
28349	Data Analyst Operations
108234	Director of Project Development
62569	Peer Recovery Specialist CORE Guide
83511	Vendeur CDI temps plein
75268	Maintenance Mechanic
152773	Personal Trainer
153970	Personal Trainer
126417	Band 6 General Radiographer East Yorkshire
161749	Delivery Driver Restaurant Catering
89078	R&D Applications Specialist
113538	Aide à domicile
156959	Sales Associate
141306	Independent Sales Representative
126157	Band 5 Paediatric Nurse RCN St Helens
113175	Executive Assistant
20581	Expression of Interest Power Transmission Distribution Professionals Design Dublin
79987	Healthcare Contact Centre Supervisor Remote
17302	Staff Physical Design CAD Engineer
71544	Paid Social Executive
15220	Superintendent
69138	Stylist Plaza Frontenac
153371	Personal Trainer
24365	Receiving Inspector Quality
151716	Personal Trainer
114514	Director Office of the President
135429	Commercial Counsel
2229	Front End Software Engineer
67631	Strategic Account Manager
122943	Data Entry Clerk Graduates AI Training Puebla Mexico
35979	Assistant Teacher
97030	Primary Care Nurse Practitioner Physician Assistant
138129	Account Executive at SetSales
24086	Submit your resume here
82846	Assistant Manager JD Fuengirola
162547	Customer Care Specialist Italian Market
133380	Software Engineering Intern iOS Summer 2027
85035	General Contractor Home Modifications
88459	Staff Civil Engineer
47955	Certified Nurse Midwife CNM PRN
22050	Hospice Chaplain Spiritual Counselor Per diem
136212	Student CEO SCEO UNIVERSITY OF TEXAS ARLINGTON
115231	Sales Development Representative
60930	Assistant Stylist
68575	Contract Design Engineer II Automotive Plastics CATIA
127456	Consultant in CAMHS Learning Disabilities Psychiatry Workington
61742	Account Executive Korean Speaker
83269	Vendeur CDI temps partiel
36112	Bilingual Digital Marketing Manager Mandarin English
56347	Service Associate
61887	Strategic Project Manager
165775	Transaction Coordinator
86534	Patient Specialist Arlington Heights IL 1099 Contractor Per Diem On Call
60714	Data Scientist Market Making
113350	IT Generalist Field Technician
113399	Registered Nurse RN
85420	Continuous Improvement Coach
91318	Social Worker Child Protection
35317	Private Equity Conference Volunteer
121851	AI Trainer Graphical Abstract Physics Remote London
132960	Director Security Engineering
42448	Operations Excellence Specialist Hub Operations Management
172484	Staff DevOps Engineer Delivery Loop
102502	Trial Attorney
126310	Band 6 7 Outpatient Dispensary Pharmacist Newcastle
27374	Psychiatrist MD
112302	OEM Sales Specialist
63420	Territory Sales Executive
28174	Organizational Development Manager
145574	Dental Assistant
156268	Customer Support Advocate Spanish
13277	DevOps Architect
75363	Home Care Sales Representative
122391	Brazil Residents Survey Participants Campinas Brazil
4985	Backend Software Engineer Campinas SP
146342	Application Security Engineer II
32986	CMO in Training Samlino Group
83474	Vendeur CDI temps plein
111198	ABA Paraprofessional RBT Brooklyn
82106	Implementation Lead Innovation Success
125567	Android Software Engineer London
142526	Associate Director Creative Alamo
156984	Seasonal Sales Associate Atherton Mill
154988	Personal Trainer Palestine TX
41208	Interior Designer Atlanta GA
36167	Sales Manager Chinese Vertical
142044	Psychiatric Clinician
147123	Legal Practice Assistant
150087	IT Support Technician Ohio
108845	Manager Customer Complaints
93234	Lead Building Engineer
70357	Operations Manager FedEx Pickup and Delivery
14848	Postdoctoral Researcher Biological Design Hsu Lab
6129	Urgent Care Emergency Medicine Physician Adult Pediatric Care Full Time
19157	Career Services Coordinator
151617	Personal Trainer
7945	Seasonal Sales Associate Part Time Bethesda Row
45322	Platform Monitoring Engineer
166083	Lead Recruiter
4449	Litigation Specialist Freelance AI Trainer Project
150038	Manager Growth Marketing Retail Media SEO
44211	Manager Retail Workforce Planning
31016	Software Engineer Python Linux Packaging
38193	Strategic Medical Director Inpatient
133005	Staff Software Engineer Authentication Authorization
97732	Manager Client Service Morristown NJ
96782	LPN Care Coordinator BHI
144613	Accountant AI Operations
12521	PT Sales Advisor Brooklyn
47640	Entry Door Service Technician
98612	Electrical Estimator Industrial Michels Power Inc
57672	Outside Sales Representative
170928	Experimentation Manager
150311	Personal Trainer
173905	Paralegal
48881	Software Engineer Machine Learning Safety
20514	Low Power SoC Hardware Architect
41221	Leasing Sales Consultant Cortland Valley Ranch
60179	SEIT Special Education Teacher Dutchess County
148412	Head of Enterprise Solutions Architecture Platforms
114308	Mechatronics Engineer
16646	Technologist
88405	Staff Civil Engineer
92716	Strategic Finance Manager Bellevue WA or Chicago IL
43527	Local Government Paralegal
56239	Service Associate
4445	Lithuanian Voice Actor Freelance AI Trainer Project
111577	In School ABA Paraprofessional RBT Lanoka Harbor NJ
50737	Strategic Solutions Architect
94847	Territory Account Executive
133825	Aerospace Production Technician II III
46377	Licensed Therapist LCSW LPCC LMFT 75 95 Session Remote
6964	Lifecycle Acquisition Analyst Mid 809
86032	Account Manager Paid Social
149292	Associate Director Clinical Study Start Up
4851	Creative Lead Freelancer
66159	Behavior Planning Engineer
34274	Registered Behavior Technician
153088	Personal Trainer
42352	CPLB Quality Assurance Specialist L4 Compliance Program Ops
147869	Vendedor a Externo 6 horas Garuva SC
59434	Group Lead Driver Excel Early Learning
141647	Front Desk Agent Part Time Soho Beach House Miami
76871	Maintenance Supervisor
31798	Técnico em segurança do trabalho Presencial
64269	Engineering Manager Finance
52693	Software Engineer Infrastructure
40692	Sales Engineer SLED Central
67319	Customer Delivery Manager
57151	Construction Equipment Mechanic
17564	OSS Cloud DevOps Engineer
7347	Operations Associate Part Time Perimeter Mall
59325	Software Engineer
83095	Vendeur CDD temps partiel
152963	Personal Trainer
99087	Trenchless Mechanic
106743	R&D Pilot Lab Technician
114910	Working Student Finance Accounting
160376	Product Analyst
116103	Supervisor part time
86044	Mobile SDK Developer
101738	Endorser Manager Asia
72498	Payer Relations Manager Central
131730	Customer Success Representative
77772	Engineering Co Op Fall 2027
136975	Lead Finance Analyst
9319	Get Ready Campaign Internship 2026 fall term
106459	Maintenance Supervisor Automation Robotics Fulfillment Burlington NJ
47898	Sales Account Executive I SB
111162	ABA Paraprofessional Position BT Port Chester NY
89840	Account Executive Business Intelligence based in Spain German speaker
84425	Implementation Consultant
81740	Controls Automation Engineer
168740	Emergency Veterinarian Relief Charlotte NC
95304	Associate Manager Brand Marketing Being Frenshe
59756	Revenue Operations Associate
18015	Business Development Representative DACH
31241	Capco Summer Internship Program Dallas Summer 2027
123793	Japan Residents Survey Participants Oita Japan
6627	Technical Project Manager Data Center Infrastructure Cabling
152821	Personal Trainer
20719	Analista de Planejamento Comercial Mercado de Crédito Varejo Veículos
4949	Consultor de Vendas Marabá AP Prazo Determinado
132981	Staff DevOps Engineer
61040	Associate Manager Brand Communications
42512	Public Relations Specialist Global Communications L4
83144	Vendeur CDD temps plein
109813	Customer Experience Enablement Manager
26207	Head of Growth Partnerships Square
110763	SEO Manager Innsbruck
29229	Certified Payroll Compliance Specialist
27821	Business Development Manager
178723	Director Technical Program Management
99411	Territory Manager Warner Robins GA
4239	German Audio Evaluations Specialist Freelance AI Trainer Project
38100	Strategic Account Manager
83113	Vendeur CDD temps plein
123530	Hindi Fluent Speakers AI Training Dallas USA
71479	Frontend Manager Software Engineer Hands On Partners Acquisition
118984	Shift Lead
13489	Construction Technician
69375	Operations Account Manager
64939	Suppression Manager
21965	Home Health Aide HHA Pediatrics
6421	Partner Account Manager
160954	Janitor
8061	Seasonal Sales Associate Part Time Stanford Shopping Center
86887	Material Supply Planner Raw Materials
83458	Vendeur CDI temps plein
80458	DevOps Release Engineer
106603	Data Analyst Revenue Operations
106611	Product Manager Market Expansion Merchant Central
103212	Software Engineering Manager Software Platform
65441	Commercial Insurance Specialist
101830	Lead Machine Learning Scientist FinCrime
3884	Web Marketing Manager
147762	Agente Stone Executivo a de Contas Externo 6 horas Pires do Rio GO
28120	Full Stack Engineer
62197	Manager of SEC Reporting Technical Accounting
145666	Surgical Assistant
166117	Director Finance Accounting Business Development
62357	Deputy Editor Ignites Asia
97427	Assistant Store Manager
116433	Whittier Shift Supervisor
140934	Customer Success Manager
115204	Select Business Account Manager
124171	Mental Health Professionals AI Training Nottingham UK
13672	Landscape Technician Stormwater
100799	Customer Success Manager FSI
9952	Solution Consultant
157421	Account Executive APAC ANZ
145236	Supervisor Facilities Starlink Split Shift
33819	Behavior Technician
3020	Industrial Maintenance Reliability Engineer
31511	Managing Director Financial Crime Transformation AML Investigations Sanctions Fraud
173640	Talent Manager Emerging Talent
112206	Telehealth Nurse Practitioner or Physician Assistant Remote Delaware License
173648	Talent Manager
158303	Administrator
129630	Sourcing Manager Sweaters
88165	Intern Landscape Architecture Planning
77634	Software Engineer SOC Experience Ruby Rails
98072	Sales Coordinator ROOST Detroit
12229	Staff Systems Engineer EW
98437	Maintenance Technician Garden Grove
98039	Front Desk Supervisor Roost Cleveland
69779	Tax Manager Financial Services
119946	Conference Executive
93670	Staff Global Supply Chain Manager Battery Cell Enclosure
20554	2027 Graduate Engineer Mechanical Building Services
76030	Data Analyst Pleno
143835	Manager Production Solar Cells
119752	Event Manager frivillig
31595	Project Manager Data Management Transformation
2973	Join our Talent Community Analytics Services Team
148909	Software Engineer
9430	Superintendent
173915	Application Security Engineer
83586	Full Time Verkoopmedewerker JD Arnhem
119059	CPG Marketing Associate Account Director
101062	Head of Engineering Payment Gateway
168406	Emergency Credentialed Veterinary Technician Part Time Overnights Clearwater FL
159204	Procurement Contract Administrator
44249	Retail Operations Manager
141854	Social Media Manager New York
35402	Enterprise Business Development Representative LATAM
127405	Consultant in Adult Psychiatry Middlesbrough
101243	Associate Director Media Strategy
159153	Staff Photo Editor Digital News Desk
125572	Community Manager
31479	Lead Product Designer UI Design Systems Product Experience
74488	Vice President Public Relations Business of Health
42307	CFS Automation Maintenance Specialist L4 INC28 Automation
166805	Mechanical Engineer II Life Support Systems
176589	Electrician Construction
30018	Missile Defense Agency MDA NOC Technician
70396	Administrative Assistant Construction
54046	Assistant Fitness Manager
148913	Staff Machine Learning Engineer Active Secret Clearance
127046	Band 7 Locum Sonographer Slough
169033	Hospital Manager Ann Arbor Michigan
41483	Maintenance Technician at The Retreat at Peachtree City Apartments
99066	Superintendent Michels Canada
132715	IT Service Delivery Tech Lead
104675	Backend Engineer
79931	Physical Therapist PT
114282	Executive Assistant
98768	HSE Coordinator Micon Group Inc
49999	Premium Onboarding Partner DDfB
151745	Personal Trainer
72633	Veterinary Student Externship
47370	CNC Turning Specialist
67269	Warehouse Yard Technician
71828	Software Test Engineer ADAS
158625	Gastroenterology Physician San Marcos TX
84388	整備士訓練開発エンジニア FSB 整備士担当
37525	Solutions Architect APJC Developer Platform India Market
103364	AWS Cloud Architect Managed Services
24352	MRB Liaison Engineer Production Engineering
134217	Spacecraft GNC Engineer I
145656	Patient Care Coordinator
75838	Insurance Producer Little Rock AR
10596	Laser Test Engineer
122621	Cardiologists Freelance Remote Milwaukee US
83654	Visual Town Merchandiser
117452	Supervisor de Cobranza Campo
64455	Mechanic
94640	Seasonal Ambassador Vaughan Mills
169052	Locum Emergency Veterinarian Etobicoke Ontario
120432	Institutional Investor Relations Danish speaking
168459	Emergency Credentialed Veterinary Technician Relief Green Bay WI
29421	Structural Engineer
114902	Partner Agent Architect UK
6224	Business Development Lead
90748	Occupational Therapist Social Care
2819	Psychiatric Nurse Practitioner Physician Assistant
152912	Personal Trainer
134533	Engineering Manager Streaming
119731	Conference Operations Executive
51902	Associate Producer Insurance
47017	Account Supervisor
42822	North Augusta Commercial Sales Representative
107648	Vice President Government and Community Partnerships
17591	Quality Assurance Inspector II different shifts available
151627	Personal Trainer
146860	Product Operations Specialist Temporary Assignment
9636	Customer Success Architect
111441	ABA Therapist RBT Flushing NY
153427	Personal Trainer
4806	Content Creator Freelancer
112374	Mohs Medical Assistant
152823	Personal Trainer
41117	職種 データセンター技術ティーチングアシスタント TA
94122	Patient Care Technician Flex AAMC FT Nights
173977	Insurance Coverage Insurance Litigation Attorney
69719	Staff Software Engineer
95827	Security Officers
89	Junk Removal and Sales Training
28063	Nurse Practitioner Addiction Medicine Telehealth
101999	In Home Sales Consultant
150811	Personal Trainer
108518	ServiceNow Developer SAM
134960	National Partner Manager ePlus
133728	Lead AI Engineer
15299	Traveling Superintendent Mission Critical Construction
98627	Electrical Superintendent Heavy Industrial Michels Power Inc
130747	Chemical Operator
6905	Business Analyst Mid 771
167136	Sales Development Representative with Italian
2472	Digital Designer
171001	Supervisor Commerce Operations Multi Retailer
157848	Structural Analysis Engineer I II
4269	Haitian Creole Language Specialist Freelance AI Trainer Project
152800	Personal Trainer
27503	Psychotherapist
33510	Behavior Technician
56482	Service Associate Night
71765	Fastener Engineer
47436	Manager Development
73554	In Home Dialysis Care Partner Little Rock AR
165213	Vascular or General Surgeon Traveler
39556	Registered Nurse RN FT .9 Day
72624	Veterinary Assistant
168830	Emergency Veterinary Assistant Fort Myers FL
159044	Deputy Director Photo
139731	Manager Battery Engineering
9890	Enterprise Account Executive Financial Services
98998	Scheduler Michels Underground Cable Inc
42858	Rich Media Designer
121736	AI Trainer Fluent Lithuanian Speaker Edinburgh UK
21859	Home Care Licensed Practical Nurse
57835	Shop Foreman Mechanic Pump Power HVAC
123929	Korea Residents Survey Participants Cheonan South Korea
36662	Director Revenue Operations CLEAR1 B2B
82678	Sales Assistant Marcon C.C Valecenter
70874	Client Success Manager EMEA
109427	Corporate Recruiter
33772	Behavior Technician
83780	Fire Engineer Litigation
130777	Global Supply Manager Mechanicals
170954	PMO Value AI and Measurement
162190	QC Inspector
96478	Retail Shift Manager Gas City area
36080	Care Without the Chaos DSP Direct Support Professional
127753	Locum Biochemistry BMS London
111648	Paraprofessional RBT New Hartford CT
170280	SI Swim Instructor
54163	Assistant General Manager
35567	Mechanical Engineer Platform Integration
13348	Events and Ministry Coordinator Onsite
36113	Bilingual Marketing Campaigns Manager Mandarin English
83109	Vendeur CDD Temps partiel
24608	Platform Core DevOps Engineer Infra
19760	Data Engineer II Hybrid
27679	Bluestaq Graduate Research Scholar NDSU Partnership
131354	Turbomachinery Engineer Turbine Development
125472	Licensed Telehealth Therapist Mental Health Counselor
27850	Manager Wholesale Account Operations
81658	Talent Communities Account Executives
22513	Night Shift LPN Home Care Nurse 1 1
119228	Recruiter Contract Position
3685	Director Information Technology Security
128620	MTS Exa
7173	Assistant Store Manager Visual London Pipeline
92415	Electrical Mining Superintendent
140919	Associate Customer Success Manager for GSA
161166	Cyber Security Engineer
128310	Specialty Dr in Community CAMHS required London
4264	Greek Voice Acting Specialist Freelance AI Trainer Project
75596	Insurance Agent Elmhurst IL
74011	Hiring Task Based Helpers for Seniors in Santa Clara CA
141183	Retail Sales Supervisor Sandro Men s Bloomingdale s Aventura Mall
58607	Growth Marketing Manager Lifecycle
150662	Personal Trainer
32521	Workday Systems Payroll HCM Manager
102640	Nail Technician
54034	Assistant Fitness Manager
98651	Estimator Mission Critical Data Center Michels Underground Cable Inc
10221	Compliance Project Manager
98008	Market Data Engineer
89721	Rotor Wing Pilot Base Lead
110068	Early Career Primary Care Physician Internal Medicine Adults Sign On Bonus Available
147562	SITE RELIABILITY ENGINEER II
151119	Personal Trainer
109174	Director Safety Sciences
33713	Behavior Technician
73954	Hiring Caregivers for Seniors in Yuca Valley California 92284
111462	ABA Therapist RBT Ridge NY
156091	Customer Care Advisor Non voice
8069	Seasonal Sales Associate Part Time Template
67793	Director of Engineering Organizations Cells
60225	Women s Psychiatric Mental Health Nurse Practitioner Pennsylvania
19675	Mission Engineer Overland Park KS
124738	Python Developers AI Training USA
82795	Stage Sales Assistant Napoli C.C La Birreria
142079	Creative Director Experiential
90816	Registered Manager Children s Residential Home
34657	Spanish Speaking Behavior Technician
154752	Personal Trainer Elizabeth CO
139254	General Interest Application
103589	Account Executive
57319	Heavy Equipment CDL Driver
139062	Patient Support Supervisor Certified Pharmacy Technician Prior Authorizations
119148	Fashion Paid Advertising Strategist
140151	Manufacturing Supervisor 3rd Shift
27722	Care Manager Disability Behavioral Support
111890	Spanish Speaking RBT Behavior Technician BT Tri State Area
44379	Wealth Management Intern
105067	Mission Integration Specialist
144302	Quality Inspector Supply Chain Avionics
104028	Laboratory Director
129513	Associate Merchant Bath
130808	Reliability Engineer Energy Storage
117	Truck Team and Customer Service
154140	Personal Trainer
128490	Account Executive SLED TOLA
58577	Revenue Operations Associate
141980	Membership Sales Associate Inside Sales Tuesday Saturday
41603	쿠팡풀필먼트서비스 물류센터 운영 및 공정 관리자 이천2물류센터
139245	IT Technician First Shift
7311	Operations Associate Part Time Fashion Island
52673	Customer Success Manager CSM
95316	Executive Consultant Account Lead
123164	Environmental Research Graduates AI Training Nottingham UK
174094	Lab Support Engineer
142869	South Star Enterprise Care Agent
50038	Associate Growth Marketing Canada Consumer
46614	Procurement Contracts Lead PMC
62525	Customer Success Manager
154824	Personal Trainer Harker Heights TX
126057	Band 5 Chemotherapy Nurse Blackburn
62160	Software Engineer C++
21830	HHA Home Health Aide
69950	Assistant General Manager
86209	Guidance Navigation and Control Engineer Space Systems
36321	Management Analyst II
71024	Assistant Teacher
116157	Manager Recruitment Admissions
123450	German Fluent Speakers AI Training Nuremberg Germany
27348	Psychiatrist MD
161331	Software Engineer Data Integration
93905	Registered Nurse Case Manager
111822	Registered Behavior Technician RBT Richmond VA
94786	Home Infusion Nurse
43928	Sales Account Executive
35283	Management Consulting Conference Volunteer
133799	Propulsion Test Technician
178231	Sales Director Carrier Solutions
140433	HBM Architect
169548	Veterinary Assistant
138519	Software Engineer Identity Access Management
96576	Tax Consultant CPA
57473	Heavy Equipment Field Technician Mechanic
175329	Systems Data Integration lead CRM
17630	SoC ASIC Architect
46119	Full Stack Software Engineer
35036	Chief of Staff at CFO Insights
103653	Account Executive
120220	Financial accountant at Private Equity Insights
21007	Manager Financial Services Risk Advisory
135270	Software Engineer Golang
101598	Consumer Engagement Ambassador Monster Energy
26121	Staff Pharmacist Supervisor
74088	Assistant Project Manager
111171	ABA Paraprofessional Position BT Yorktown Heights NY
176335	Werkstudent Mensch Mediaberatung Marketing Werbung
52273	Account Executive Home Health Hospice US Remote
34298	Registered Behavior Technician
143385	Environmental Health Safety Technician AI Supercomputer Memphis
2211	Mobile Primary Care Nurse Practitioner Physician Assistant
119185	Paid Advertising Account Manager
160457	Business Development Manager
102248	Legal Assistant
74492	Advocacy Engagement Coordinator at Hillel Waterloo Laurier
160281	Account Manager DTC
150296	Personal Trainer
112928	Business Development Representative
121412	Accountants AI Training Omaha US
118320	Shift Supervisor
22558	Occupational Therapist Full Time Home Health
162434	Software Engineer Applications Unmanned Aircraft Systems
154769	Personal Trainer Fairfax VA
154963	Personal Trainer New Braunfels TX
118295	Shift Supervisor
17015	Residential Rehab Educator PT hrs 20
71483	Global Accounting Specialist
114508	Head of PL POS Growth
2532	Behavior Technician Part Time
112678	Sterilization Technician
175220	Director of Operations Account Management
177242	Staff Product Manager Design for Manufacturability DFM
80500	Product Specialist Biotech
81223	Strategic Account Executive Data and Analytics
66208	Executive Protection Specialist Veterans Preferred
153007	Personal Trainer
92959	Vehicle Technicians Opportunities Nationwide
60279	Human Resources Recruiting Operations Coordinator
58242	Psychiatric PA C
161062	Sales Representative
172020	Analyst Deals Finance
43944	Counsel
43069	Lead Packaging Agent 2nd Shift
23262	Registered Nurse RN Home Health Full Time
141047	Product Marketing Manager Vertical Industries
143785	Machine Maintenance Technician
11357	FPGA Test Engineer Intelligence Systems
42565	Line Haul Specialist 資深運輸營運調度專員 Middle Mile
80404	Child Adolescent Therapist Remote NJ
1347	6th 8th Grade Math Teacher
142180	Systems Engineering Manager
120611	Investor Recruitment Manager Dutch speaking
164882	Event Organiser
63049	Marketing Web Developer
31977	Commercial Relationship Manager NOVA Market
22746	Physical Therapist
122371	Biology Graduates AI Training Newcastle upon Tyne UK
170544	Office Coordinator Home Health Webster
136866	Per Diem Clinical Research Nurse Home Visits
98866	Project Engineer Michels Construction Inc
112560	Lead Quantitative Engineer
34802	Manager Accounting Advisory
9111	Learning Scientist
126117	Band 5 Mental Health Nurse RMN Berkshire
124986	Spanish Fluent Speakers AI Training Oviedo
64563	Endocrinology Physician Remote
46927	Dental Assistant
38581	Demand Generation Manager Global
50889	Dyne Care Partner Northern California
75533	Welcome Coordinator
150551	Personal Trainer
158848	Certified Medical Assistant Pediatrics
80699	SOFTWARE DEVELOPER SPECIALIST II
165925	Assembly Product Handler Off Shift 5 30pm 3 30am
155325	APAD II
45975	Software Engineer Backend
76053	Assistant Manager North America Brand Marketing
22463	Medical Social Worker Hospice Full time
144347	Ropes Access Technician Starship
124774	Registered Nurses AI Training London UK
141407	Technical Recruiter Engineering
125826	Adult Acute Speech and Language Therapist
9860	Customer Success Specialist
158204	Psychiatrist Regional Medical Director
102239	Legal Assistant
172546	Logistics Coordinator
64078	Supply Chain Business Operations Intern Spring 2027
34048	Center Based Registered Behavior Technician RBT
69050	Stylist Aventura
165115	Clinical Technologist I
58123	Yard Worker
40890	Manager DC Construction Project Accounting
113837	Auxiliaire de vie
55249	General Manager
100807	Director Product Management Growth
95153	AI Engineer Agentic Systems
68894	Brand Growth Lead
53319	Security Engineer AI Cloud
84321	Shift Supervisor San Ramon
36493	Partnerships Expert TravelPay São Paulo Hybrid
157768	Software Engineer Intern AI Compiler Serbia
144129	Porter Temporary
404	Associate Dentist
113630	Aide aux personnes âgées
77437	Product CEO Venture Studio Director Austin
109480	Entry Level Construction Technician
96825	Medical Director
88712	Manager Talent Partnerships
111176	ABA Paraprofessional RBT Baldwin NY
144965	RF Front End Module Design Engineer RFIC Engineering
7345	Operations Associate Part Time Park City
173046	Associate Veterinarian Established Practice Excellent Location Edmonds Westgate Veterinary Hospital
145742	Software Engineer Platform Chapel Hill NC USA
129495	Future Opportunities
7461	Operations Visual Manager 12 South
9654	Strategic Account Executive
21669	Companion Direct Support Professional
54449	Assistant Stretch Manager
12128	Staff Global Sourcing Manager Critical Minerals Mining Rare Earth Magnets
75565	Insurance Agent Butte MT
113007	Associate Risk Compliance
122886	Database Administrator Graduates AI Training Wellington New Zealand
17530	Tulip Platform Engineer
156635	Gehaltsbuchhalter
71807	Quality Service Technician Chassis GA Contract
160133	GL Accountant
157884	Licensing Manager
4775	Vietnamese Language Specialist Freelance AI Trainer Project
79372	San Antonio Talent Community
148836	Medical Assistant
130211	Customer Service Analytics Analyst
156991	Seasonal Sales Associate City Creek
38892	General Opportunities Psychiatric Nurse Practitioner
81094	Manager Platform Engineering
55439	Group Fitness Instructor
105595	Strategic Finance Analyst
39937	Line Cook
```

## Predictions — do not read until step 3

This is what the classifier answered for each of the rows above. Reading it before the labels are
written to disk destroys the measurement: the labeller would agree with the classifier and the
numbers would be decorative.

```
89	UNKNOWN
117	UNKNOWN
404	OUT
1142	UNKNOWN
1233	UNKNOWN
1347	OUT
2211	OUT
2229	IN
2472	UNKNOWN
2532	OUT
2819	OUT
2973	UNKNOWN
3020	OUT
3127	OUT
3464	OUT
3475	OUT
3618	IN
3685	UNKNOWN
3704	IN
3884	OUT
4239	OUT
4264	OUT
4269	OUT
4284	OUT
4445	OUT
4449	OUT
4664	OUT
4733	OUT
4748	OUT
4760	OUT
4775	OUT
4788	OUT
4806	UNKNOWN
4851	UNKNOWN
4949	UNKNOWN
4985	IN
5139	UNKNOWN
6129	OUT
6224	UNKNOWN
6421	OUT
6627	OUT
6905	UNKNOWN
6964	UNKNOWN
7173	OUT
7284	OUT
7311	OUT
7345	OUT
7347	OUT
7461	UNKNOWN
7945	OUT
8061	OUT
8069	OUT
9111	UNKNOWN
9285	UNKNOWN
9319	UNKNOWN
9430	OUT
9548	UNKNOWN
9566	UNKNOWN
9636	UNKNOWN
9654	OUT
9711	UNKNOWN
9860	OUT
9890	OUT
9938	IN
9952	UNKNOWN
10221	UNKNOWN
10490	IN
10524	OUT
10596	IN
11219	UNKNOWN
11357	IN
11360	UNKNOWN
11503	OUT
11804	OUT
12128	OUT
12229	IN
12521	UNKNOWN
13277	IN
13343	OUT
13348	OUT
13489	OUT
13635	OUT
13672	UNKNOWN
13738	UNKNOWN
14242	OUT
14459	IN
14598	OUT
14848	UNKNOWN
15220	OUT
15299	OUT
16493	UNKNOWN
16646	UNKNOWN
17015	UNKNOWN
17302	OUT
17370	OUT
17530	IN
17564	IN
17591	OUT
17630	UNKNOWN
18015	OUT
19157	OUT
19266	UNKNOWN
19675	UNKNOWN
19760	IN
20175	OUT
20514	UNKNOWN
20525	UNKNOWN
20554	OUT
20581	UNKNOWN
20719	UNKNOWN
20878	UNKNOWN
21007	OUT
21253	OUT
21348	OUT
21551	OUT
21669	UNKNOWN
21715	UNKNOWN
21830	OUT
21859	OUT
21965	OUT
22050	OUT
22463	UNKNOWN
22513	OUT
22558	OUT
22746	OUT
23262	OUT
23625	UNKNOWN
23683	OUT
24086	UNKNOWN
24352	UNKNOWN
24365	OUT
24608	IN
24757	OUT
24780	UNKNOWN
25597	UNKNOWN
25723	OUT
26121	OUT
26207	UNKNOWN
26454	OUT
26509	OUT
27348	OUT
27374	OUT
27413	OUT
27503	OUT
27679	UNKNOWN
27722	OUT
27821	UNKNOWN
27850	OUT
28054	OUT
28063	OUT
28120	IN
28129	IN
28174	UNKNOWN
28349	UNKNOWN
29229	OUT
29421	OUT
30015	IN
30018	UNKNOWN
30023	UNKNOWN
30411	UNKNOWN
31016	IN
31241	UNKNOWN
31479	IN
31511	UNKNOWN
31595	IN
31798	UNKNOWN
31977	OUT
32095	IN
32521	OUT
32926	IN
32986	UNKNOWN
33381	UNKNOWN
33510	OUT
33713	OUT
33772	OUT
33819	OUT
34048	OUT
34274	OUT
34298	OUT
34573	OUT
34657	OUT
34685	OUT
34802	OUT
35036	UNKNOWN
35201	OUT
35283	OUT
35317	OUT
35402	OUT
35567	OUT
35703	OUT
35979	OUT
36080	UNKNOWN
36112	OUT
36113	OUT
36167	OUT
36210	UNKNOWN
36321	UNKNOWN
36493	UNKNOWN
36546	UNKNOWN
36662	UNKNOWN
36674	IN
36779	UNKNOWN
37145	UNKNOWN
37166	OUT
37525	IN
37974	IN
38100	OUT
38193	UNKNOWN
38581	UNKNOWN
38608	IN
38892	OUT
38934	OUT
38984	OUT
39556	OUT
39937	OUT
40378	OUT
40692	UNKNOWN
40724	UNKNOWN
40836	OUT
40890	OUT
41117	UNKNOWN
41208	UNKNOWN
41221	UNKNOWN
41483	OUT
41577	UNKNOWN
41603	UNKNOWN
41623	UNKNOWN
41671	IN
41795	UNKNOWN
41844	IN
42307	OUT
42352	IN
42370	UNKNOWN
42448	UNKNOWN
42512	OUT
42565	UNKNOWN
42784	UNKNOWN
42822	OUT
42858	UNKNOWN
43069	UNKNOWN
43527	OUT
43928	OUT
43944	OUT
44086	OUT
44138	OUT
44187	OUT
44211	OUT
44249	OUT
44371	UNKNOWN
44379	UNKNOWN
45322	IN
45461	IN
45681	OUT
45843	OUT
45975	IN
46119	IN
46377	OUT
46614	OUT
46927	OUT
47017	OUT
47018	OUT
47370	UNKNOWN
47436	UNKNOWN
47524	OUT
47640	OUT
47871	OUT
47898	OUT
47955	OUT
48881	IN
49999	UNKNOWN
50038	UNKNOWN
50737	IN
50889	UNKNOWN
50955	UNKNOWN
51899	OUT
51902	OUT
52273	OUT
52329	OUT
52673	OUT
52693	IN
53319	IN
54034	OUT
54046	OUT
54051	OUT
54059	OUT
54163	OUT
54449	OUT
54554	OUT
55249	OUT
55358	OUT
55439	OUT
55458	OUT
55612	UNKNOWN
55744	UNKNOWN
55999	UNKNOWN
56137	UNKNOWN
56239	OUT
56314	OUT
56347	OUT
56482	OUT
56693	OUT
57151	OUT
57319	OUT
57332	OUT
57473	OUT
57672	OUT
57705	OUT
57736	OUT
57835	OUT
57866	UNKNOWN
58123	UNKNOWN
58242	UNKNOWN
58374	UNKNOWN
58473	OUT
58577	OUT
58607	OUT
59000	OUT
59106	OUT
59166	UNKNOWN
59325	IN
59347	OUT
59434	OUT
59560	OUT
59756	OUT
59883	OUT
59918	OUT
60056	OUT
60179	OUT
60225	OUT
60279	OUT
60714	IN
60855	OUT
60930	OUT
61040	OUT
61490	OUT
61742	OUT
61883	UNKNOWN
61887	UNKNOWN
62160	IN
62197	OUT
62335	UNKNOWN
62357	OUT
62525	OUT
62569	UNKNOWN
62575	UNKNOWN
62824	OUT
62916	UNKNOWN
63049	OUT
63306	OUT
63420	OUT
63422	UNKNOWN
63479	UNKNOWN
63576	UNKNOWN
63723	IN
63837	OUT
64078	UNKNOWN
64269	OUT
64352	OUT
64455	OUT
64563	OUT
64939	UNKNOWN
65441	OUT
66159	OUT
66208	OUT
66962	UNKNOWN
67269	OUT
67319	UNKNOWN
67516	UNKNOWN
67631	OUT
67793	UNKNOWN
68509	IN
68575	OUT
68669	UNKNOWN
68894	OUT
68916	OUT
69050	OUT
69138	OUT
69375	OUT
69719	IN
69779	OUT
69950	OUT
70357	UNKNOWN
70396	OUT
70874	UNKNOWN
71024	OUT
71181	OUT
71479	IN
71483	OUT
71544	OUT
71691	OUT
71765	UNKNOWN
71807	UNKNOWN
71828	IN
72498	UNKNOWN
72624	OUT
72633	UNKNOWN
72690	OUT
73554	UNKNOWN
73669	UNKNOWN
73954	UNKNOWN
74011	UNKNOWN
74088	OUT
74488	UNKNOWN
74492	OUT
74541	OUT
75042	OUT
75268	OUT
75363	OUT
75370	UNKNOWN
75533	OUT
75565	OUT
75596	OUT
75838	OUT
76030	UNKNOWN
76053	OUT
76187	OUT
76236	UNKNOWN
76507	OUT
76834	UNKNOWN
76871	OUT
77103	OUT
77437	UNKNOWN
77634	IN
77772	UNKNOWN
77993	UNKNOWN
78618	UNKNOWN
79082	OUT
79164	OUT
79372	UNKNOWN
79931	OUT
79987	OUT
80404	OUT
80458	IN
80500	UNKNOWN
80699	IN
80754	OUT
81094	IN
81223	OUT
81517	OUT
81658	UNKNOWN
81725	UNKNOWN
81740	OUT
82106	UNKNOWN
82445	OUT
82678	OUT
82684	OUT
82795	OUT
82846	OUT
83095	OUT
83109	OUT
83113	OUT
83144	OUT
83269	OUT
83458	OUT
83474	OUT
83511	OUT
83576	OUT
83586	UNKNOWN
83654	OUT
83780	UNKNOWN
83814	UNKNOWN
84162	OUT
84321	OUT
84388	UNKNOWN
84425	UNKNOWN
84454	UNKNOWN
84665	UNKNOWN
85035	UNKNOWN
85420	UNKNOWN
85457	OUT
85762	OUT
85910	UNKNOWN
86032	OUT
86044	IN
86209	IN
86534	OUT
86887	OUT
87193	OUT
87242	UNKNOWN
87901	UNKNOWN
87963	UNKNOWN
88165	UNKNOWN
88209	OUT
88405	OUT
88459	OUT
88568	OUT
88693	OUT
88712	OUT
88749	OUT
89078	UNKNOWN
89493	OUT
89721	UNKNOWN
89840	OUT
89984	OUT
90276	IN
90538	OUT
90641	OUT
90748	OUT
90801	OUT
90816	UNKNOWN
91318	UNKNOWN
92001	UNKNOWN
92415	OUT
92607	UNKNOWN
92716	OUT
92959	UNKNOWN
92983	UNKNOWN
93094	UNKNOWN
93118	UNKNOWN
93234	UNKNOWN
93575	UNKNOWN
93670	OUT
93832	OUT
93905	OUT
94122	OUT
94264	OUT
94290	OUT
94459	UNKNOWN
94640	OUT
94679	OUT
94786	OUT
94847	OUT
95153	IN
95259	UNKNOWN
95304	OUT
95316	OUT
95827	UNKNOWN
96478	OUT
96576	UNKNOWN
96738	OUT
96782	OUT
96825	UNKNOWN
97030	OUT
97427	OUT
97732	UNKNOWN
98008	IN
98039	OUT
98072	OUT
98437	OUT
98490	OUT
98612	OUT
98627	OUT
98651	OUT
98768	OUT
98866	OUT
98911	OUT
98998	UNKNOWN
99066	OUT
99087	OUT
99175	OUT
99411	UNKNOWN
99423	OUT
99532	IN
100382	OUT
100719	OUT
100799	OUT
100807	UNKNOWN
101062	UNKNOWN
101243	UNKNOWN
101482	OUT
101590	OUT
101592	OUT
101598	OUT
101738	UNKNOWN
101830	IN
101999	UNKNOWN
102179	OUT
102239	OUT
102248	OUT
102502	OUT
102570	OUT
102640	UNKNOWN
102909	UNKNOWN
103071	OUT
103212	IN
103359	OUT
103364	IN
103589	OUT
103653	OUT
103814	UNKNOWN
103914	IN
103943	OUT
104028	UNKNOWN
104245	OUT
104675	IN
105067	IN
105595	UNKNOWN
105681	IN
106459	OUT
106603	UNKNOWN
106611	UNKNOWN
106614	IN
106743	UNKNOWN
106829	UNKNOWN
106936	OUT
107398	OUT
107632	OUT
107648	UNKNOWN
108234	UNKNOWN
108518	IN
108845	UNKNOWN
109174	UNKNOWN
109427	OUT
109480	OUT
109555	OUT
109609	UNKNOWN
109740	UNKNOWN
109813	UNKNOWN
110068	OUT
110258	OUT
110332	OUT
110763	UNKNOWN
111162	OUT
111171	OUT
111176	OUT
111198	OUT
111370	OUT
111441	OUT
111462	OUT
111577	OUT
111648	OUT
111822	OUT
111890	OUT
112003	OUT
112206	OUT
112302	OUT
112374	OUT
112396	OUT
112560	IN
112678	UNKNOWN
112928	OUT
113007	UNKNOWN
113019	OUT
113175	OUT
113350	OUT
113399	OUT
113516	OUT
113538	OUT
113630	OUT
113837	UNKNOWN
113865	UNKNOWN
114134	OUT
114282	OUT
114304	IN
114308	UNKNOWN
114493	OUT
114508	UNKNOWN
114514	UNKNOWN
114681	OUT
114902	OUT
114910	UNKNOWN
115204	OUT
115231	OUT
115538	UNKNOWN
116045	OUT
116103	OUT
116157	OUT
116433	OUT
116445	UNKNOWN
117192	UNKNOWN
117452	OUT
117850	OUT
117894	OUT
118295	OUT
118320	OUT
118750	IN
118984	UNKNOWN
119059	UNKNOWN
119148	UNKNOWN
119185	OUT
119228	OUT
119731	OUT
119752	UNKNOWN
119946	OUT
120022	OUT
120220	OUT
120432	UNKNOWN
120611	OUT
120900	OUT
121412	OUT
121633	OUT
121736	OUT
121851	OUT
122139	OUT
122149	OUT
122371	OUT
122391	UNKNOWN
122621	UNKNOWN
122720	OUT
122886	OUT
122930	OUT
122943	OUT
123018	OUT
123164	OUT
123450	OUT
123530	OUT
123793	UNKNOWN
123845	OUT
123929	UNKNOWN
124171	OUT
124337	UNKNOWN
124738	OUT
124774	OUT
124896	OUT
124986	OUT
125215	UNKNOWN
125472	OUT
125567	IN
125572	UNKNOWN
125814	OUT
125826	OUT
126057	OUT
126117	OUT
126157	OUT
126310	OUT
126417	OUT
126643	UNKNOWN
126809	OUT
126835	OUT
127046	OUT
127296	UNKNOWN
127405	UNKNOWN
127456	UNKNOWN
127753	UNKNOWN
128310	UNKNOWN
128490	OUT
128620	UNKNOWN
128731	OUT
128998	OUT
129455	OUT
129495	UNKNOWN
129513	UNKNOWN
129630	UNKNOWN
129764	UNKNOWN
130179	UNKNOWN
130211	UNKNOWN
130239	IN
130747	OUT
130777	UNKNOWN
130808	OUT
131354	OUT
131730	OUT
131969	OUT
132646	OUT
132715	IN
132944	UNKNOWN
132960	IN
132981	IN
133005	IN
133040	OUT
133380	IN
133728	IN
133756	UNKNOWN
133799	OUT
133825	OUT
134217	OUT
134533	UNKNOWN
134960	UNKNOWN
134972	OUT
135000	UNKNOWN
135270	IN
135356	OUT
135429	OUT
136212	UNKNOWN
136362	OUT
136614	UNKNOWN
136717	OUT
136866	OUT
136975	OUT
137346	UNKNOWN
137676	OUT
138044	IN
138129	OUT
138519	IN
138701	OUT
139062	OUT
139245	UNKNOWN
139254	UNKNOWN
139314	OUT
139731	UNKNOWN
139804	OUT
140151	OUT
140433	UNKNOWN
140509	IN
140719	UNKNOWN
140919	OUT
140934	OUT
141047	OUT
141183	OUT
141251	OUT
141302	OUT
141306	OUT
141407	OUT
141647	OUT
141854	OUT
141980	OUT
142044	UNKNOWN
142079	UNKNOWN
142180	IN
142314	OUT
142526	UNKNOWN
142624	OUT
142869	OUT
143385	UNKNOWN
143785	OUT
143835	OUT
143933	OUT
144035	OUT
144129	UNKNOWN
144302	OUT
144347	UNKNOWN
144613	OUT
144863	OUT
144965	OUT
145236	OUT
145574	OUT
145656	OUT
145666	OUT
145742	IN
146342	IN
146853	OUT
146860	UNKNOWN
146899	IN
147123	OUT
147187	UNKNOWN
147562	IN
147762	OUT
147869	OUT
148317	IN
148412	UNKNOWN
148836	OUT
148907	IN
148909	IN
148913	IN
149292	UNKNOWN
149395	OUT
149429	OUT
150038	OUT
150072	OUT
150087	UNKNOWN
150296	OUT
150311	OUT
150551	OUT
150662	OUT
150702	OUT
150811	OUT
150973	OUT
151050	OUT
151119	OUT
151136	OUT
151188	OUT
151252	OUT
151332	OUT
151495	OUT
151617	OUT
151627	OUT
151716	OUT
151745	OUT
151779	OUT
151910	OUT
152563	OUT
152773	OUT
152800	OUT
152821	OUT
152823	OUT
152912	OUT
152937	OUT
152963	OUT
153007	OUT
153088	OUT
153157	OUT
153242	OUT
153371	OUT
153427	OUT
153970	OUT
153972	OUT
154140	OUT
154397	OUT
154459	OUT
154465	OUT
154596	OUT
154752	OUT
154769	OUT
154824	OUT
154877	OUT
154933	OUT
154963	OUT
154988	OUT
155145	OUT
155325	UNKNOWN
155991	OUT
156091	UNKNOWN
156215	IN
156268	UNKNOWN
156635	UNKNOWN
156870	UNKNOWN
156959	OUT
156984	OUT
156991	OUT
157421	OUT
157768	IN
157848	OUT
157884	UNKNOWN
158204	OUT
158303	UNKNOWN
158625	OUT
158755	UNKNOWN
158848	OUT
159044	UNKNOWN
159153	OUT
159204	OUT
160133	OUT
160281	OUT
160376	UNKNOWN
160457	UNKNOWN
160728	UNKNOWN
160954	OUT
161062	OUT
161166	IN
161331	IN
161749	OUT
161779	OUT
162190	OUT
162434	IN
162547	UNKNOWN
162631	OUT
162769	UNKNOWN
163552	OUT
164552	OUT
164882	UNKNOWN
164893	OUT
165115	UNKNOWN
165213	OUT
165269	OUT
165564	OUT
165775	OUT
165925	UNKNOWN
166083	UNKNOWN
166105	UNKNOWN
166117	UNKNOWN
166190	OUT
166282	UNKNOWN
166661	OUT
166805	OUT
167136	OUT
167794	OUT
168141	OUT
168406	OUT
168459	OUT
168553	OUT
168639	OUT
168648	OUT
168740	OUT
168830	OUT
169033	UNKNOWN
169052	OUT
169548	OUT
170280	OUT
170544	OUT
170928	UNKNOWN
170954	UNKNOWN
171001	OUT
171224	OUT
171261	OUT
171297	OUT
171360	OUT
171509	UNKNOWN
172020	UNKNOWN
172484	IN
172546	OUT
172585	OUT
173046	OUT
173111	OUT
173352	OUT
173640	OUT
173648	OUT
173655	OUT
173905	OUT
173915	IN
173977	OUT
174094	UNKNOWN
174148	IN
174490	OUT
174585	UNKNOWN
174668	UNKNOWN
175220	UNKNOWN
175329	IN
175448	OUT
175506	UNKNOWN
175581	UNKNOWN
176045	UNKNOWN
176335	UNKNOWN
176589	OUT
176744	IN
177203	IN
177242	UNKNOWN
177298	UNKNOWN
178051	OUT
178231	UNKNOWN
178723	UNKNOWN
```
