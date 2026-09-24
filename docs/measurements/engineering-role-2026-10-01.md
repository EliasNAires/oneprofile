# Iteration 11 of the engineering-role classification loop

Run on the development machine on 2026-09-23, against the raw corpus snapshot already loaded in the
development database — 179 098 vacancies, cleaned and classified — and triggered with one `POST
/classifications`. The criterion is unchanged since ADR-0010 (revision `29b1272`).

The filename carries 2026-10-01 rather than the day it ran, for the reason iteration 3 gave: the
next session finds its input by taking the newest file, and the ten earlier iterations already hold
the days before it.

This iteration labelled the 1000 rows iteration 10 drew, scored iteration 10's classifier against
those labels under the skill's three-rate definition, and grew the rules from where they disagreed.
The labels are `src/test/resources/labels/engineering-role-2026-09-30.tsv`, written to disk before
any rule of the classifier was read.

**The labelling was done by a subagent**, as in iterations 5 through 10: it was given the criterion
and the 1000 bare titles, denied the classifier's source, the iteration reports and the earlier
label files, and told to write its labels to disk before returning. Scoring, rule pricing and the
draw were done by scripts over scratchpad files, and every rule was priced on an offline `javac`
harness whose per-state counts matched the application's to the row, before and after the change.

## The numbers

The three rates score **iteration 10's** classifier, on the stratum sizes it drew: 100 `IN` / 500
`OUT` / 400 `UNKNOWN`. A mistake is any row whose state differs from the label.

| Gated number | Iteration 8 | Iteration 9 | Iteration 10 | Iteration 11 | Gate |
| --- | ---: | ---: | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 3.00% | 1.80% | 1.60% | **4.80%** (24 of 500) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 16.00% | 18.00% | 30.00% | **31.00%** (31 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 77.33% | 74.25% | 73.50% | **60.25%** (241 of 400) | ≤ 10% |
| `unruled` — of the whole corpus | 5.61% | 3.95% | 3.93% | **3.49%** (6 257) | ≤ 4% |

Reported alongside, gated by nothing:

- **`OUT`-stratum rows labelled `IN`** — the error nothing downstream recovers: **5** of 500, up
  from 1. `Agent AI Engineer` (the never-engineering `agent` read as the first head), `Staff
  Engineer MAC OS Client`, `Tech Lead Code Plane IC5` (bare `tech lead` was out on the generic-head
  rule — 11 corpus titles read plain `Tech Lead`), `IT Assistant` and `Computer Vision Specialist
  Freelance AI Trainer Project`. The second and third are fixed below; the change leaves 3 of the 5.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **43** — 24 it called `IN`, 19
  it called `OUT`. Up from 25; the `IN` side is product managers, sales and solutions engineers and
  the quant-research family, all of which this labeller ruled `scope_ambiguity`.
- **The labeller's own unknown pile**: 202 rows — 115 `domain_ambiguity`, 80 `scope_ambiguity`, 7
  `unruled`. Larger than iteration 10's 131, and the growth is all `scope_ambiguity` (13 → 80):
  this labeller read product management, sales engineering and packaged-software analysts as
  phrases that split on Q4.
- **What the `UNKNOWN` stratum turned out to be.** Of the 400 rows iteration 10 called `UNKNOWN`,
  the labeller called 38 `IN`, 203 `OUT` and 159 `UNKNOWN`. By the classifier's reason: of 226
  `domain_ambiguity` rows, 18 `IN` and 88 `OUT`; of 38 `scope_ambiguity` rows, 14 `IN` and 2
  `OUT`; of 136 `unruled` rows, 6 `IN` and 113 `OUT`. The `unruled` rows are the ones the labeller
  most often decided, and nearly always `OUT`.

**The `UNKNOWN` stratum error fell 13 points**, the first real move since iteration 8, and it fell
where iteration 10 put its change: the generic-head rule decided the management tail, so the pile
this sample was drawn from holds more titles the labeller also could not decide. **The `IN` stratum
did not move**: iteration 10's test/QA and discipline-marker changes held it at 31% against 30%.
What is left there is thirteen rows the labeller ruled `scope_ambiguity` on families it read as
splitting (quant research, solutions engineering, product management, packaged-software leads),
eleven `domain_ambiguity` rows on systems and hardware-adjacent engineers, and seven false accepts,
most of them a qualifier under a generic head (`Security Scheduling Manager`, `Manager Reward
Business Partner Data Centres`, `Construction Safety Advisor Utility Infrastructure`).

The corpus after this iteration's change:

| | Iteration 9 | Iteration 10 | Iteration 11 |
| --- | ---: | ---: | ---: |
| `IN` | 18.53% | 18.22% | **18.40%** (32 949) |
| `OUT` | 61.05% | 67.76% | **68.66%** (122 963) |
| `UNKNOWN` | 20.42% | 14.02% | **12.95%** (23 186) |
| &nbsp;&nbsp;of which `unruled` — the gated share | 3.95% | 3.93% | **3.49%** (6 257) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 14.90% | 8.52% | **7.88%** (14 119) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.57% | 1.57% | **1.57%** (2 810) |

2 017 rows changed state: 1 668 from `UNKNOWN` to `OUT`, 254 from `UNKNOWN` to `IN`, 81 from `OUT`
to `IN`, 14 from `IN` to `OUT`.

## The exit gate

**Not met.** `unruled` passes at 3.49% with half a point of margin, the widest it has had. The `OUT`
stratum error passes. `IN` stratum error is 31.00% and `UNKNOWN` stratum error is 60.25%, both
against 10%.

**Iteration 12 is the last session and the gate will not be met in it.** The skill says what that
session does: label and score as usual, write the report, record the reason split, close #10, and
hand the remaining unknown pile to #11 with the open criterion questions below as notes on #11.

## Cost

A full pass over the 179 098 cleaned titles takes **0.72s** on the offline harness against the
ten-second limit, and its per-state counts match the application's to the row
(`{"in":32949,"out":122963,"unruled":6257,"domainAmbiguity":14119,"scopeAmbiguity":2810}`).
Whole-word, case-insensitive matching over the cleaned title; no description bodies, no network.

## What changed, and why

Every change was priced on the 10 000 accumulated labels — agreement, and the count of rows decided
`OUT` that a label calls `IN`, which may not rise. Baseline: 8 570 agreeing, 25 misses. After:
**8 687 agreeing, 23 misses.** The rows the rules broke are listed with each change.

### Never-engineering heads the unruled pile named

113 of the 136 `unruled` rows in the sample were titles the labeller decided `OUT`, and their heads
were plain professions no list held. Thirty-four join the never-engineering set — clinical
specialities (`phlebotomist`, `doctor`, `obstetrician`, `gynecologist`, `ophthalmologist`,
`hematologist`, `audiologist`, `paediatrician`, `bcba`, `doula`, `emt`), the care, hospitality and
warehouse floor (`babysitting`, `lifeguard`, `busser`, `budtender`, `packer`, `palletizer`,
`loader`, `detailer`, `housekeeping`), and `lawyer`, `geologist`, `physicist`, `marketer`,
`organiser`. Each was sized on the corpus first: `server`, `creator`, `warehouse` and `dr` reach
software titles (`SQL Server`, `Creator Tools`, `Data Warehouse`) and were left out.

### Q1 in the shapes this sample wrote

Thirteen rulings for the post that hires nobody: `banco de talentos`, `banco de candidatos`, `não
encontrou` (the Portuguese "didn't find your vacancy" banner, 34 titles), `initiativbewerbung`,
`candidature spontanée`, `career opportunities`, `career fair`, `interested in applying`, `share
your contacts`, `no open role`, `resume drop`, `study participant`, `studienteilnehmer`. Two
labels broke — `Initiativbewerbung` and `Banco de Talentos de Operações Offshore`, labelled
`UNKNOWN` by the 2026-09-21 and 2026-09-22 fixtures, which predate Q1 and are evidence-free for
it.

### Software domains named by their own word, and two phrases that hold a head

`flutter`, `genai`, `agentic`, `c#`, `ubuntu`, `vulnerability`, `redes`, `mac os`, `macos`,
`andorid` and `swe` join the qualifiers; `swe`, `enginer` and `développeur.euse` join the heads.
`tech lead`, `analytics engineer` and `analytics engineering` join the qualifiers too, which is
the one unusual shape in this change: a ruling would have decided `Tech Lead ASIC Design Engineer`
and `Tech Lead PCB Layout Engineer` in, because rulings outrun the discipline markers; as a
qualifier step 4 still reads the ASIC first. One label broke: `GenAI Influencer Marketing Manager`
now `IN` — the qualifier-under-a-manager false accept this report's questions section is about.
`forward deployment engineer` is ruled `IN` beside the existing `forward deployed engineer`.

### `consultant` is a generic head

The criterion reclassed `consultant` domain-bound in iteration 6, but it was left off the generic
heads, so a consultant with an unclassed modifier (`Consultant Public Sector`, `Consultant in
Respiratory Medicine Midlands`) sat in `domain_ambiguity`. On the accumulated labels, adding it
fixed 26 rows and broke 9, with no new miss. `specialist`, `intern`, `engineer`, `developer` and
`administrator` were priced the same way: `specialist` added two misses (`Website Onboarding
Specialist`, `Calypso CATT Specialist`), `intern` one, `engineer` thirteen, `developer` six, and
`administrator` gained nothing — all left out. The existing test that pinned `consultant
gastroenterologist bristol` as `domain_ambiguity` now reads `OUT`, which is what a hospital
consultant is.

### Seven discipline markers

`bioconjugation`, `substation`, `power generation`, `land development`, `traffic engineering`,
`process engineering`, `cultivation`. Each fixed one to three labels and broke none. `protein` and
`actuarial` were priced and left out: over the corpus the first moved `ML Scientist AI for Protein
Engineering` out and the second `Actuarial Software Engineer II`, both plausibly `IN`. `fire` and
`building` were left out as well (`Fire TV`, and the verb).

### Tried and dropped

- **A qualifier under a generic head decides nothing.** Of the 282 labelled rows the classifier
  calls `IN` with no core engineering head in the title — manager, director, specialist, lead,
  consultant over a software qualifier — the labels say 114 `IN`, 65 `OUT`, 103 `UNKNOWN`. Sending
  them to `domain_ambiguity` loses 11 rows net and moves the error from the `IN` stratum to the
  `UNKNOWN` one, which is further from its gate. It is the `manager` question below.
- **A software-support qualifier (`service desk`).** This labeller called `Service Desk
  Administrator` and `Service Desk Lead` `IN`; iteration 10's called the same family
  `scope_ambiguity`. Not a rules question.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows it was grown from: 219 disagree, against 296 before — 3 misses, 7 false accepts, and 44
rows the labeller left `UNKNOWN` that are decided. By the new predictions the three rates would read
3.93%, 27.35% and 51.08%.

## What the next session should look at

Iteration 12 is the last, and it closes the loop rather than harvesting. What it hands #11 is the
unknown pile — 23 186 titles, 61% `domain_ambiguity`, 27% `unruled`, 12% `scope_ambiguity` — and
the questions below.

## Questions for the criterion, not for the rules

**Is `manager` engineering-capable?** Unchanged from iteration 10 and now the single largest source
of `IN`-stratum error: a manager, director or specialist over any software qualifier decides `IN`,
and the labels say that is right 40% of the time.

**What does `scope_ambiguity` mean to a labeller?** This labeller used it six times as often as
iteration 10's, for families the criterion never ruled — product management, sales engineering,
packaged-software analysts, quant research. The reason is defined over "a ruled phrase", and a
labeller has no ruled-phrase list, so the reason's size is a measure of the labeller's reading as
much as of the corpus.

**The AI-training gig.** Three labellers have now split `Computer Sciences Graduates AI Training`,
`Javascript Developers AI Training` and `Database Administrator Graduates AI Training` between
`IN`, `OUT` and `scope_ambiguity`. Whether it is a vacancy that a software background opens (Q4)
or annotation work is a criterion question.

**Support roles**, **designers**, **`architect` as domain-free**, **firmware, networking and
radar**, and **sales engineering** carry over from iteration 10 unchanged.

## Held out of the draw

The 10 000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, matched case-insensitively. 165 685 of the 179 098 rows were
eligible.

## The sample for iteration 12

1000 rows drawn at random from the predictions above — 100 from `IN`, 500 from `OUT`, 400 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each row
is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
22368	LPN New Grad Nurse Residency Fort Wayne
119637	Regulatory and Start Up Specialist Argentina
56024	Operations Manager
58410	CRNA HCA Southeast Houston Houston TX 290 k with 7 Weeks Off
175902	Manager Paid Social Mensch
89261	Conseiller.ère en architecture bureautique
41710	CPLB 사내 통번역사 한영 경력직
144241	Production Supervisor PCB Manufacturing
76165	Manager Transaction Management
146210	Account Executive
117069	Machine Learning Engineer
119919	CCO at Private Equity Insights
52258	Channel Account Manager Italy
123172	Environmental Research Graduates AI Training Sheffield UK
161016	CRM Manager Subscriptions
149994	Staff Site Reliability Engineer Cloud Efficiency
164406	Student Finance Advisor
150907	Personal Trainer
137536	Associate General Insurance
72059	Product Program Manager
147261	Agente Stone Consultor a Comercial Externo Campo Grande MS
53056	Energy Market Analyst
119548	Feasibility Manager
4159	Electrical Engineering Specialist Freelance AI Trainer Project
159465	Physical Occupational Therapist Consultant
149331	Manager Global Medical Affairs Operations Contract
11534	Mission Engineer Air Dominance Strike Active Clearance
122476	Canada Residents AI Trainers Laval Canada
172191	Staff Technical Program Manager Simulation Infrastructure Resource Management
17704	Marketing Manager Insurance
144858	Materials Engineer Metals Starship
89762	Center Based Registered Behavior Technician RBT
124332	Operations and PMO Leaders Subject Matter Expert Oxford UK
115904	Deployment Strategist
58605	Engineering Manager Platform
101144	Product Design Lead
123591	HTML CSS Developers AI Training Boston USA
143174	Construction Project Manager Facilities Infrastructure
82583	Travel Manager
25531	Prenatal Regional Sales Manager Northern Los Angeles
118662	International Trade Support Analyst
163000	Mission Architect Missile Defense
78065	Product Manager Evergreen Private Fund Solutions Assistant Vice President Vice President
20557	2027 Graduate Sustainability Engineer Carbon BREEAM
70125	Production Technician
124063	Linguist Translator Graduates AI Training Oaxaca Mexico
115403	金融サービス 事業開発 BizDev
71307	Purchasing Specialist
118921	Product Manager Patient Growth
4352	Khmer Language Specialist Freelance AI Trainer Project
175587	Business Analyst 12 Month FTC
151645	Personal Trainer
20398	Salesforce GTM Systems Manager
134453	Ad Partner Solutions Manager Data Measurement
75472	Warehouse Supervisor Nights
155700	Quality Engineer
106125	Actuarial Analyst
118593	AI Solutions Architect
177610	Data Analyst MedTech
56633	Vice President of Construction
160238	Lead Associate Trading Director
20367	Legal Counsel International Asst Manager Manager Manager
11083	Quality Specialist
39542	Recovery Support Specialist RSS FT Days
141860	Sous Chef Dumbo House
119962	Conference Operations Organiser
37411	Customer Engineer Vietnam
35737	Sales Manager
142653	Manager Business Development Commercialization
3949	Network Engineer Corporate Enterprise Network WAN
148545	Product Design Manager Global Payments
4218	Garment Manufacturing QC Specialist Freelance AI Trainer Project
133046	On Site Landscaper Lead
68655	AI Engineer
42809	Financial Planning and Analysis Director
160797	Robotics Field Engineer
71855	Business Analyst Workflow Associate Consultant Corporate Legal Solutions
108491	Software Engineer AI Agentic Experience Auth0
10319	Director of Advanced Design
70713	Target Analyst Reporter
124126	Mechanical Engineering Experts PhD AI Training US
109645	Civil Engineer Site Design
128622	MTS Exa
164315	UX UI Designer
24282	Field Service Technician
103298	Cloud Platform Engineer
75444	Processor Nights
31151	Analista de Processos Rio de Janeiro Onshore
150535	Personal Trainer
141083	Data Analyst Revenue Operations Analyst Bangalore Hybrid
131256	Director Supplier Industrialization Engineering
1247	Executive Long Term Care Sales Specialist Knoxville TN
66792	Sports Data Collector Football Carapeguá Paraguay
31238	Capco Associate Talent Program Dallas June 2027
161687	Bus Person
176019	Pessoa Analista de Mídia Pleno
84127	Production Supervisor NIGHT SHIFT
122931	Data Entry Clerk Graduates AI Training Nantes France
97825	Cloud Security Engineer InfoSec
107563	Product Manager of Retention
41103	AI Coach Copilot ChatGPT Claude or Gemini
101194	System Administrator
79729	Instructional Designer Contract Remote
46490	Treasury Adoption Consultant
173040	Associate Veterinarian DVM Mixed Animal Practice Small Large Animal Medicine Chewelah Veterinary Clinic
37564	Software Engineer Network Firewall
83859	Consultant Fire Protection
149481	Commercial Terrain Indépendant Freelance
23932	Gene Editing Platform Technologies Co op
11676	Reliability Engineer
107640	Associate Strategy
77599	Paid Voice Acting Improv Work Atlanta GA
80396	Health Economist final title commensurate with experience
92243	Analyst Market Development
82601	Meet us at SPLASH 2026
40324	Knowledge Engineer
157103	Collection Specialist
80073	AI Content Flexible Hours
126586	Band 6 Locum Radiotherapist Derby
58522	Geschäftsführer KFZ Betrieb
24107	Regulatory Surveillance Analyst 2
157780	Journeyman Contracts Support Specialist
32348	Registered Nurse Virtual Care Care Management Remote
77356	Chief of Staff Minneapolis
177499	Private B2B RM Private Strategy
147716	Agente Stone Consultor a Comercial Externo Rondonópolis MT
75239	Contract Advisor
14128	Technical Delivery Manager
155542	Lead All Source Intelligence Engineer
171547	Armed Security Officer Armed License Required
41820	Integrated Marketing Specialist Offline Focus
80301	Territory Manager Austin
84616	Aide à domicile
18116	Supply Chain Analyst
29324	Product Specialist
41708	Coupang Sourcer
133456	Growth Marketing Manager Lead
77014	Event Assistant volunteer
7868	Seasonal Operations Associate Part Time SanTan Village
76727	Valuations Analyst
8672	AlphaSights On Campus Interviews
16390	Tax Dealerships
105260	General Job Opening
51306	Technical Program Manager Technology Process and Supplier Development TPD
59600	Middle Technical Support Analyst L2 L3 Production Support
141053	Professional Services Business Development Director
166856	Legal Counsel Employment
35166	Events Manager
67102	CONTRACT Construction Project Manager Facility Renovation Fit Out
20727	Especialista de Produtos Car Equity Veículos
88557	Technicien.ne dessinateur.trice en structure
40372	Food Safety Specialist Overnight
171764	High Performance Computing Engineer
79382	Salesforce Administrator Austin TX ONLY
45769	Head of Global Executive Support
125294	Mining Engineer
82779	Stage Sales Assistant Full Time Curno C.C Curno
65485	Founding Clinical Director Southern California San Diego
83854	Fire Protection Engineer
74549	Assistant Store Manager Tampa
127136	Band 7 Radiotherapist Essex
100888	Software Engineer Server Security
128740	Product Manager FlashArray Core
54625	Certified Personal Trainer
117108	Account Development Representative
78851	Trade Surveillance Analyst
94977	Quality Assessment Claims Specialist
106193	Strategic Account Executive
30436	Patient Success Advocate
146467	Sales assistant Sport Zone Mozelos HG
88696	Account Manager Influencer Marketing
123778	Japan Residents Survey Participants Funabashi Japan
23478	RN Hospital at Home Overnights
178395	Software Engineer
125788	Accident Emergency A&E East Cheshire
44738	Delivery Solutions Architect Digital Native Business
108228	Core Design Engineer
62858	Director Director Regulatory Counsel
143714	Lab Architect
41576	쿠팡로지스틱스서비스 물류 차량 운영기획 담당자 신입 가능
16954	Language Data Analyst Croatian
125187	US Finance Data Contributor AI Research Remote Task Based Atlanta
83356	Vendeur CDI temps partiel
157764	Inference Server Product Software Intern Oct 2026 start
168607	Emergency Veterinarian Greenville SC
15801	Arnold Veterinary Hospital DVM VMD Extern
152072	Personal Trainer
147676	Agente Stone Consultor a Comercial Externo Franco da Rocha SP
130346	Director Product Management
120146	Experienced Event Executive
124339	Operations and PMO Leaders Subject Matter Expert Seattle US
127895	Medical Scientist Microbiology Waterford
56476	Service Associate Night
124043	Linguist Translator Graduates AI Training Lille France
125711	Associate Vice President Audience Solutions
1487	Paraprofessional
51160	FX Business Developer
64765	Production Team Lead
159661	Product Line Manager ESN
138674	Treasury Analyst
131000	SeniorSolution Architect Public Sector
70895	Project Manager Client Service
74688	Assistant Field Engineer Hourly
84386	原運用許容基準エンジニア MMEL策定担当
80063	Sales Manager Indonesia InMobi
43250	Peer Support Specialist
80520	Physical Therapist Assistant Part Time Family Medical Center of Hart Cty
122918	Data Entry Clerk Graduates AI Training Liverpool UK
32874	Critical Intervention Specialist
53171	Application Admin
41059	Staff Software Engineer File Block Storage
14392	Outside Sales Consultant 1099 Merchant Services
43844	Functional Safety Lead
9710	Support Analyst II
130335	Mechanical Engineer Machine Design
30423	Bilingual Spanish Speaking Health Coach
59576	Associate Database Administrator SQL Server Oracle
100329	Group Product Manager GenAI Creatives
109990	Data Business Analyst Alibaba Cloud Stack
95961	Product Manager Digital Risk Protection
102318	Litigation Paralegal
53097	Data Engineer
52390	Facility Technician Maintenance
56764	Implementation Manager
136724	2027 Business Analytics Intern
71523	Staff Product Manager BASE
173834	Game Designer Game Lead
152743	Personal Trainer
105895	Assistent Accountant Samenstelpraktijk
19275	Robot service technician Full Time Contract
16872	Engineer Heaters and Electrostatic Chuck ESC
108095	Praktikum Creation
94436	Part Time Floor Leader Maine Mall
26309	Technical Account Manager Cash App Pay Afterpay
171211	Project Engineer
141704	HR Systems Manager
127693	Learning Disabilities Psychologist
8995	Engineering Manager Credit and Lending
146742	Integrations Developer
66841	Sports Data Collector Football Hayes Jamaica
18796	Application Support Engineer
106227	Seasonal Product Operations Coordinator
156665	Hub Lead
4191	Financial Compliance Specialist India Freelance AI Trainer Project
157240	Producer
497	Dental Hygienist
152945	Personal Trainer
27758	Underwriter
85191	Quantamental Research Analyst Trading Team
31741	Solution Architect Financial Services
21052	Transformation Change and Delivery Consultant
148052	Production Line Lead 1st Shift
168383	Emergency Credentialed Veterinary Technician Paramus NJ
30226	Gerente de Negócios II Câmbio Comercial
156523	Technical Solutions Engineer
72649	Future Culture And Climate Opportunities
10008	Construction Project Manager Intern Summer 2027
27215	Psychiatrist MD
22044	Home Intensive Care Unit RN PT Nights
170947	Optimisation Manager Contract
86728	Training Lead Math
17047	Assessoria de Investimentos
165389	Area Safety Manager
100728	Manager Engineering
176948	QA Manual Remote
10203	Chief Engineer Fury Aircraft Development
154553	Personal Trainer
111625	Outreach Growth Representative
22591	Occupational Therapist OT Home Health
175438	Activation Lead
128075	Paediatric Specialist Speech and Language Therapist
124068	Linguist Translator Graduates AI Training Querétaro Mexico
106535	Apply to NISC s Quality Engineering Teams
136437	Security Engineer Public Sector
71223	Director Product Marketing
96395	Project Engineer Capital Improvement Projects
164678	Immigration Criminal Defense Attorney
4258	German Voice Actor Freelance AI Trainer Project
48285	Grill Cook Murray Hill
150759	Personal Trainer
31254	Consultor Especialista em Conformidade II Rio de Janeiro Híbrido
170339	Software Engineer Client Trading Infrastructure Java
51910	Business Development Coordinator
177384	Assessoria de Investimentos Início de Carreira
23239	Registered Nurse RN Homecare Overnights
73603	In Home HHD Dialysis Care Partner 1 1 Client Gulf Breeze FL 32561
117299	Sales Consultant Italy
69981	Director Product Development Edibles
92600	QA QC Manager Coatings
78887	Medical Director Medical Director Clinical Development
5958	Registered Nurse RN
88485	Staff Traffic Engineer
162763	Environmental Specialist
85649	Accountant
12449	Welder
150354	Personal Trainer
61639	M&A strategy and value creation Analyst
113076	Director Strategic Planning Office of the CEO
9974	Solution Consultant III
7438	Operations Lead Vail Village
129822	Quantitative Technologist C++ Intern
10472	Flight Test Director
64185	ETF Growth Lead
51941	Director C&E Consulting
31636	Back End .NET API AI Ready
5539	Marketing Manager Indonesia Market
153835	Personal Trainer
164858	Event frivillig
78655	Director Business Development
97446	Floor Lead Retail Full time
6643	FP&A Analyst Corporate Costs Procurement
29785	Ongoing Monitoring Analyst
125116	Traditional Chinese Cantonese Fluent Speakers AI Training Taiwan
101991	Entry Level Lead Generator
149616	Field Sales Representative
19044	Business Development Representative
83692	Associate Underwriter
10027	Project Manager Construction
88985	Wealth Manager South Korea
77566	Data Collection Contributor
32554	DevOps Engineer
102450	Seeking General Liability Claims Adjusters
52926	PMO Project Admin
102571	Fire Protection Engineer
32972	Client Relationship Analyst Client Solutions Team
46241	Health Information Specialist I
58771	Commercial Account Executive Central
73015	Data Analyst Operational Excellence
8760	Design Engineer All Levels to
162127	Trade Operations Associate
157563	Associate Director Director Financial Advisory
105892	Afstudeerstage Aangiftepraktijk
24978	SOC Analyst
132013	Manager Support
15203	Project Manager Hyperscale Construction
168196	Sales Development Representative SDR
80586	COMMERCIAL ANALYST I SHOPPING BUSINESS MKP
123359	Game Development Environment Artist Brighton UK Freelance Remote
126002	Band 5 Accident Emergency A&E Nurse Kettering
160499	国际数学教师
57982	Territory Account Manager Pump Power HVAC
33041	PLL Design Engineer
171963	Lead Noise Vibration and Harshness NVH Engineer
19214	Gesundheits und Krankenpfleger für hausärztliche Praxis
107857	Inventory Management Specialist Internal Title Stores Analyst
83615	Store Colleague JD Den Bosch Weekend Help
156548	Shop Porter Custodian
86928	Customer Care Specialist
115081	Partner Quality Specialist I
8285	Sales Director Net New Business Mid Market SaaS
6003	Associate Director Customer Success
58438	Job Share CRNA CHRISTUS St Elizabeth Beaumont TX 330 000 1099 with 26 Weeks Off
104236	Director Project Management
104928	Bilingual SAP EWM Consultant
112722	Structures Design Engineer
368	Outside Sales Representative Roofing
69986	Employment Counsel
171894	2027 Summer Intern MBA Strategic Finance
100331	Group Product Manager Supply Quality
158914	Field Account Manager CDMX
107403	VP EH&S Global
45655	Commercial Mid Market Sales Engineer
79875	Occupational Therapist OT
117092	AE Network Blitz
8427	Analyst I Directed Content Bilingual
158330	Data Solution Architect
48547	Corporate Counsel Datacenters and Infrastructure
70053	Order Fulfillment Technician
175758	GOC Ad Ops Specialist
49854	Associate Strategy and Operations Dasher Fraud
68469	Finance Systems Engineer
27066	Psychiatric Mental Health Nurse Practitioner PMHNP
76255	Purchasing Manager
70663	Production Technician
111573	HR Director
135120	Manager Finance Controlling
54870	Fitness Counselor
118166	Overnight Customer Service Representative
77949	Business Process Analyst UiPath Developer Associate Assistant Vice President
14642	Product Lead Opterra Financial Reporting
161048	Lead Product Manager Banking Payments
41583	쿠팡로지스틱스서비스 재무관리 기획
114785	Therapist
15894	Director AI Data Science Marketing Measurement Effectiveness
104165	Affiliated Distribution Intern Summer 2027
79994	Five9 Telephony Admin
32324	WIOA Program Lead
64783	DevOps Engineer
28650	Lead Product Manager Banners Content Cards
4504	Mechanical Engineering Specialist Freelance AI Trainer Project
163431	Management and Control Application
122394	Brazil Residents Survey Participants Curitiba Brazil
4427	LaTex Specialist Fluent in Russian Freelance AI Trainer Project
19369	Account Executive Consumer Tech
135271	Software Engineer Healthgrades
64687	Field Service Engineer Remote
19130	Aircraft Mechanic Program Coordinator
65107	Regional Sales Director Texas
106341	Field Marketing Manager
122664	Chemistry Graduates AI Training Glasgow UK
136215	SVP of Finance Accounting
52937	Release Engineer
5731	Product Manager Services
54690	District Fitness Manager
13230	Power Procurement
158742	FLT Driver Nights THG Manufacturing Tywyn
86058	Analytics Lead Data Measurement Impact
119242	ABM Strategist
41661	쿠팡 Business Development Q Commerce 쿠팡이츠
158364	Product Support Specialist
165523	Software Engineer Upstart Bank
8799	Integration Architect
61550	H&S Specialist
81475	Staff Architect
105612	Associate Director People Partner
123844	Javascript Developers AI Training Oklahoma City USA
72753	Contrôleur qualité Matières premières
56175	Personal Training Manager
95307	Design Engineer Co Op
1975	SAP Basis Admin Onsite in Kingstowne VA
133454	Global Developer Engagement Representative Part Time Contractor
21989	Home Health Aides Private Home Care
14356	Project Manager
107074	Household Coordinator
74322	Enterprise Account Executive DACH
109161	Warehouse Order Puller CRL
50826	Trading Intern Summer 2027 DV Commodities
163038	Identity Authorization Engineer
32027	Financial Crime and Fraud Investigator
144903	Partnerships Manager Starlink Commercial Aviation
143888	Manufacturing Specialist Starshield
176024	Pessoa Analista de Mídia Sênior
76270	Real Estate Acquisition Consultant
48777	Manager Platform Engineering
48703	Stamford Restaurant Team
164065	Resource Optimization Specialist
91866	Social Worker Safeguarding Team
14677	Autonomy Engineer
101949	Design Researcher Contract Palo Alto
167963	Recruiter Marketing
134394	Manager Product Creator Platform
165984	Mold Maker
65754	Physical Therapist
91074	Social Worker Child Protection
176847	Engineer iGaming
29555	Asistente de Banca Preferencial Banca Privada
43264	Recovery Coach Entry Level Mental Health Aide
96621	Associate Director of Mechanical Engineering West Coast Region
153922	Personal Trainer
8440	Cloud Support Engineer
48620	Staff Engineer Inference Optimizations
45732	Enterprise Sales Executive
41465	Groundskeeper at Steepleway Downs Apartments
50211	Business Intelligence Engineer
120321	Head of Event
157274	Product Manager Engagement
61451	Event Manager Freelance
74423	Director Media Strategy
112328	Office Administrator
75396	Dry Shipper Aseptic Nights
175321	Supervisor Corporate Card Administrator
177178	Paralegal Corporate and Compliance
163694	Staff Product Manager
122882	Database Administrator Graduates AI Training Tuxtla Gutiérrez Mexico
36689	General Manager PIT
25875	Regional Sales Manager
40678	Client Success Manager New York NY
50839	Operations Associate
62271	Project Lead Data Creators CDMX
68019	Director People Partnering
96116	Women s Health Provider Midwife or GP Cantonese Fluency
70261	Bauphysiker Ingenieur für Bauphysik
78391	Staff Product Manager Wallet Authentication
69700	Staff Engineering Operations Technical Program Manager
129646	General Inquiry
61763	Creative Strategist Manager
16692	Engineer I Field Process
122896	Data Entry Clerk Graduates AI Training Brighton UK
36340	Equipment Operator
22290	LPN
95763	Retail Security Officer Part Time
9047	Financial Analyst
59260	Engineer Team Lead
5974	Operations Specialist I III Germany
80310	Engineering Manager London
10871	Modeling Simulation Analysis MSA Engineer HITL Focus
178154	Engineering Manager Loyalty Product
171442	Client Success Manager
11070	Quality Control Supervisor
87805	Emergency Department Lead LVT
90005	Partner Account Manager
132812	Staff Product Manager
11698	Robotics Software Engineer Omen
128149	Psychologist Adult Inpatient
93456	Production Team Lead
174805	Intern Business Technology
24235	Sales Resident Experience Consultant South Austin TX
41342	Barback
112048	Help Desk IT Support Technician II Italian Speaking
6283	NEPA Specialist
49897	Engineering Manager Merchant Tablet
167083	Inside Sales Representative Saudi Arabia and Bahrain
22031	Home Health Visits Registered Nurse
35718	Showroom Manager
134195	Program Manager II TS SCI
25140	RALEIGH Land Site Development Internship
22446	Marketing Manager Account Executive Home Health Care
13650	Water Wastewater Operator Treatment Plant Field Services
176957	Key Account Manager
65695	Data Architect
9783	Financial Resource Management Budget Analyst
40488	Commercial HVAC Technician
77834	Director People
150628	Personal Trainer
80786	Program Manager
100615	Account Development Representative Hebrew Speaking
61067	ERP Analyst 1
170923	ESTÁGIO EM PROJETOS
145211	Structures Technician Dragon
77156	MBA Graduate
41580	쿠팡로지스틱스서비스 서브허브 물류 자동화 설비보전 엔지니어 인재풀 등록
172985	Patient Financial Advocate Full Time Wolf River
152836	Personal Trainer
144881	NDE Engineer Radiography Testing
141108	Bankkaufmann frau als Kreditspezialist
174140	Strategic Talent Acquisition Specialist
122827	Database Administrator Graduates AI Training Ciudad Juárez Mexico
7155	Retail Sales Associate
54169	Assistant General Manager
172970	Credentialing Specialist Full Time Germantown TN
134782	Frontend Engineer
43145	Manager of Enterprise AI Success
174495	מלקטים וולט מרקט מודיעין
75366	Learning and Development Lead
62239	Helix Data Creator SP
148329	EMEA Demand Generation Platforms
136353	Engagement Manager Public Sector
159137	Staff Editor Temporary
90398	Advance Practitioner
132025	User Researcher II
8913	Test Engineer
99936	Director of Product Program Management
40000	Lead Family Nurse Practitioner FNP Urgent Care
143162	Communications Security Account Manager
68796	Account Executive Private Equity
58248	Registered Nurse
125410	Freelance Pitch Form
9986	大手直販営業 Enterprise Account Executive Manufacturing
146754	Sales Manager eCommerce
161572	Software Engineer Intern Summer 2027
40106	Medical Assistant MA Urgent Care
144325	Regulatory Analyst Product Starlink
177247	Staff Product Manager Financial Systems
156761	People Partner Tech
74802	Lift Director
37400	Customer Engineer Shenzhen
85875	Platform Engineer Infrastructure USA
89422	Residency Postdoc Research Scientist Scientific Foundation Model
127653	Histology BMS Cumbria
140925	Creative Onboarding Specialist
70610	Imagery Scientist SAR Expert
163286	Flex Commercial Security Officer Downtown Chicago
126988	Band 7 Locum Mammographer Surrey
165865	Mission Engineer
80630	FINANCIAL PLANNING ANALYST I
56862	Future Opportunities Warehouse Associate
107441	Human Evaluation Researcher
122662	Chemistry Graduates AI Training Fresno US
61499	Growth Graduate Program
178735	Financial Representative Global Accounts Payable
51805	Sales Associate Optician Training Provided
124834	Remote Study Participants AI Research Saltillo
101474	Finance Analyst
91917	Social Work Referral Assessment
27434	Psychotherapist
149269	Team Lead
20070	Head of Systematic Futures
19293	Brand Designer
53592	Environmental Scientist Project Manager
102169	Class Action Associate Attorney
132720	Sales Executive Inventory Optimization
114932	NY Center Physician MD DO
13244	Staff Software Engineer Full Stack Software Products
131553	Supervisor Stage Integration First Shift
109601	Project Engineer Rail Track Terminal Intermodal
22629	On Call Runner Registered Nurse
102882	Brand Manager Contract
136696	Project Engineer
170178	Freelance Reformer Pilates Instructor
26493	Staff Security Engineer
10200	Chief Engineer EW
98791	Inventory Specialist Michels Power Inc
3635	Quality Engineering Manager
99418	Territory Manager Woodway TX
131773	Copywriter
27685	Software Engineer
28249	Microsoft Copilot Pre Sales Solutions Architect
131982	Payroll Specialist Lead Japan
154490	Personal Trainer
89128	QPIP Quality Person in Plant
101396	Performance Media Analyst
71453	Business Development Representative ENT
52357	Bilingual Clinician CBHC
134203	RF Engineer II Radar
39262	Behavioral Threat Assessment Manager Australia Remote
89156	Compiler Code Gen Engineer
79781	Product Manager Remote Contract
64276	Quality Assurance Testing Engineer
33580	Behavior Technician
95637	Estimator
50084	Product Design Manager Integrity
22729	Per Diem RN Substitute School Nurse
120416	Institutional Investor Relations Associate Dutch speaking
54884	Fitness Counselor
36749	Engineering Project Manager
126781	Band 6 Triage Physiotherapy Heywood
60571	eMarketing Specialist Paid Media
138033	Strategic Growth Partner Rhode Island
108677	Engineering Director DBA
129408	Scientific Software Engineer AMO Simulations
135607	Enterprise Core Implementation Consultant East Central
115537	Financial Solutions Guide 2
31403	Functional Testing with Banking Domain Functional Testing with Banking Domain L3 5.1 7 years 29704 1
103846	Restaurant Team
520	Office Manager
16669	Engineer Field Process Richardson TX
83765	Facade Engineer
89280	Développeur.euse ETL Talend
83023	Vendeur CDD temps partiel
75477	Join our Freelance Community
96376	Project Engineer
139921	FPGA RTL Design Engineer Signal Processing
49473	Stage Partenariats Opérations et Stratégie janvier 2027
7517	Sales Associate Full Time Old Orchard
29848	GRC Lead Crypto Capital Risk Engineer
166208	Medical Laboratory Tech MLT Nights
178006	Assistant of Humanities 2026 2027
67166	Technical Account Representative
164261	Pre Sales Engineer
116313	Marketing Manager EU
57716	Regional Heavy Equipment CDL Driver
167854	Regional Sales Manager Mid Market Atlanta
121034	Website Lead at Private Equity Insights
31039	Technical Author multiple roles and seniority levels
139753	Power Conversion Engineer
120990	Relationship Management Executive
37363	Customer Engagement Strategy Manager
81816	Turbopump Turbine Engineer
277	2K Games Technical Art Graduate Program Novato
39200	JW Marriott Doorman Sat Sun 3pm 11pm
102763	Conversion Rate Optimization Analyst
151780	Personal Trainer
25264	BIBIBOP Operations Leader Kenwood
143552	Government Affairs Manager
9550	Compliance Testing Engineer
101546	Temporary Collections Analyst
35418	Public Sector Account Executive Civilian
114953	DevOps Engineer GCP
114232	Staff Strategic Sourcing Manager Logistics
35874	Engineering Manager Verifications
168969	Emergency Veterinary Assistant Relief St Petersburg FL
19946	Manufacturing Engineer I Onsite
167096	Lead Domain Engineering Specialist
24839	Betting Hero Sales Ambassador
47567	Commercial Entry Door Technician
10279	Deployment Lead West Coast
31214	Business Analyst OpenLink Houston
89054	HR Policy Risk Controls Manager
65805	Analyst LP Secondaries
90425	Care Practitioner
122890	Data Entry Clerk Graduates AI Training Auckland New Zealand
89520	Product Development Engineer II
132885	Enterprise Sales Engineer
156495	Account Executive Team Dentsu
52359	Bilingual Family Connector Family Resource Center
35648	Director of Revenue Revenue Controller
44390	Customer Success Manager
99485	Product Manager China
22695	PCA s Personal Care Aides
109689	Safety Specialist
177775	Enterprise Account Executive
5416	Manager Strategy Operations Bangkok Based
89367	Associate Territory Manager
19577	Director Engineering Program Management
61533	Growth Strategy Analyst
20556	2027 Graduate Project Controls Planning
18668	SEO Manager Content AEO
16366	Tax Manager
41278	Service Technician Cortland Pentagon City
62960	Implementation Analyst
13300	Controller
82598	Meet us at ICFP 2026
117496	Software Engineer II
49388	Kundenservicemitarbeiter
62085	Director Strategic Sales Berlin Germany
6868	Platform Engineer
145186	Starlink Growth Lead US West
15437	Thru Put Project Manager
73905	Hiring Caregiver for a Female in Eagle Idaho 83616
117015	Lead Corporate Communications
72380	Sub Investigator NP PA
79884	Occupational Therapist OT
127246	Clinical Forensic Psychologist Adult Inpatient
104295	Utility Aide Dishwasher
124018	Linguist Translator Graduates AI Training Barcelona Spain
115860	VIP Host
103849	Restaurant Team
67010	Sports Data Collector Football Vratsa Bulgaria
149997	Staff Software Engineer
40532	Associate Director Clinical Supply Chain
132971	Site Reliability Engineer Observability
132417	Financial accountant at Retail Insights
93130	Adv Stezzano
174368	Product Manager Courier Delivery Experience
136940	Data Analyst CRM
33457	Associate Director of Clinical Services
164999	Leadership Program MBA Graduate
15515	CURITIBA Consultor a Pedagógico a Externo a
170373	Solution Consultant
88476	Staff Geotechnical Engineer
11285	Electrical Integration Systems Engineer Edge Compute and Communications Active Clearance
19708	Professional Services Engineer 911 Systems Remote
100281	Market Analyst
172270	Fleet Service Manager
122779	Customer Support Reps AI Training Charlotte US
973	Ads Growth Insights Data Analyst
157711	DFT Engineer Architecture
8935	Director Director Machine Learning for Biology
142695	Experienced Pipeline TD
155178	Personal Trainer Wilson NC
1550	Acquisition Strategist Manager Eatontown NJ
155560	Lead Mission Engineer Full Stack
83120	Vendeur CDD temps plein
176379	Group Product Manager Core
93546	Service Parts Advisor
94332	Project Manager P.E Water Wastewater
111591	In School ABA Paraprofessional RBT Lanoka Harbor NJ
149281	System Administrator
103775	Applied Mechanics Engineer
106111	Staff Product Manager Search
60758	Technology Governance Risk Compliance GRC Analyst
132134	Talent Acquisition Specialist
147431	Analista Pleno de Políticas de Crédito Crédit Business Analyst
103894	Software Development Co op Jan 27 Start
115552	Advanced BESS Technician Operations and Maintenance
6366	Consultant Financial Services Data AI
137482	Associate Director Engineering Sciences Chemist
121322	Statistical Programming Contractor
34244	Registered Behavior Technician
165492	Loan Processor
109603	Project Engineer Site Design Municipal
68085	SDE III Data Engineering
28999	Candidate Experience Coordinator
177387	Assessoria de investimentos Sênior Maceió
50495	Commodities Macro Analyst
31640	Business Analyst Data Analytics She He They
167323	Policy and Partnerships Manager
53338	Legal Working Student
29326	Regional Marketing Specialist Media
48100	Software Engineer
97961	Product Manager Onchain
167256	Virtualization Backup Engineer German Speaker
256	Staff Data Analyst
92111	Product Manager I B2B
13852	Account Executive Inbound
31211	Business Analyst Insurance Domain
3099	Team Lead of UAC Media Buyers
116907	Agency Lead Independents
119680	Product Manager CRO
84363	eVTOLのインバーター開発エンジニア
125180	Urdu Fluent Speakers AI Training Manchester UK
92288	Freelance Scientific Director Medical Education Oncology
44952	Security Field Engineer
141753	Membership Sales Manager
82847	Assistant Manager JD Málaga Larios
11907	Talent Sourcer SG&A
175432	Account Manager Search
37606	VoidZero Developer Relations Engineer
132051	Windows Engineering Manager
46680	Hackathons Team Lead FedD068
4486	Mathematics Specialist Fluent in German Freelance Trainer Project
64385	Engineering Manager Trade Management
105962	Opdrachtmanager A&A
41831	Line Haul Specialist Middle Mile 運輸規劃專員
9951	Solution Consultant
106128	Backend Engineer
154036	Personal Trainer
78343	Visual Communication Designer
103035	Media Monitoring Specialist
79730	Instructional Designer Contract Remote
53763	Executive Assistant Office Manager
90658	Occupational Therapist Community Services
12684	Corporate Finance Strategy Cash Flow Forecasting
135517	Finance Manager
87474	Funds Ratings Analyst Analyst London
176717	Security Engineer Detection Response Japan X Money
164956	Graduate Management Associate at United Media
119493	Medical Writer Promotional Medical Writing
72713	Allround Onderhoudsmonteur
14663	Analyst Trade Accounting Operations
133813	Industriemechaniker in Triebwerksmontage
79749	Instructional Designer Contract Remote
21395	Caregiver Adults and Seniors
29605	Relief Veterinarian August November 2026
72046	Microbiologist
49437	Software Engineer Fullstack Kotlin React
173522	Experienced Consultant Banking
177593	Growth Product Manager
140123	Associate Director Quality Compliance QA Client
63229	Customs Specialist
28683	People Business Partner 12 Month Fixed Term
156655	HR Business Partner
57112	Bulk Fuel Specialist CDL
6542	Strategic Intelligence Analyst Americas
122836	Database Administrator Graduates AI Training Guadalajara Mexico
73131	US DC Analyst Planning Excellence
10655	Logistics Analyst
107603	IT Project Manager
227	Gameplay Designer
101501	Specialist Media Operations
29277	Application Support Engineer
10467	Flight Sciences Engineer
172927	Wellthy Care Network Caregiver San Diego CA
110888	Retail Lead Store Advisor Gateway
61137	Manager Engineering
155813	Mr D Paid Media Lead
146886	Facility Security Officer
152280	Personal Trainer
49254	Sports Architect
63147	Director Product Compliance
158916	Inside Sales Executive Mexican Market
8175	Visual Lead Galleria Dallas
104107	Scientist R&D Design Transfer Product Development
126783	Band 7 8a Adult Community Speech and Language Therapist Croydon
156088	Collections Advisor
23332	Registered Nurse RN STAT Home Care Nights
63959	Planning Development Specialist
100660	Engineer 3 GTM Tech
137944	Staff MDR Investigator
91350	Social Worker Child Protection
145019	Site Reliability Engineer Starlink
18061	Accounting Co op
140645	Product Manager Engagement
178356	Join Our Team at Zócalo Health
151996	Personal Trainer
141914	Lending Deposits Working Student
165751	Strategic Growth Manager
166343	Licensing Project Manager
30016	Linux Systems Administrator
122601	Cardiologists Freelance Remote Denver US
84695	Auxiliaire de vie à domicile
16236	Global Billings Lead
69933	Administrative Coordinator
89270	Conseiller.ère en architecture logicielle organique
156319	Client Services Manager Growth
164596	Bulb Preconstruction Manager
98012	Systematic Trading Desk Lead
22548	Occupational Therapist
173560	Architect Tech Advisory
40777	Director Strategic Marketing
28246	Inventory Asset Operations Project Manager
111554	Credentialing Specialist
78534	Business Performance Analyst III
32725	Data Scientist Machine Learning
163619	Presales Engineer German speaker
158360	Brands Analyst
91658	Social Worker Looked After Children
54665	Certified Personal Trainer
116536	Histotechnician Lab Portsmouth NH
123121	Einwohner Deutschlands Studienteilnehmer innen Bonn Deutschland
19301	Site Infrastructure Engineer Networking Compute
42940	Instagram Channel Manager
107013	Product Manager Subscription Products
109267	Regional Financial Controller
108347	Documentation Tools Manager
128506	Business Intelligence Analyst RTB
106106	Software Engineer Backend
50042	Associate Measurement Ad Tech Marketing Technology
108508	Solutions Engineer Auth0
73175	BCBA Board Certified Behavior Analyst Northeast
146191	Account Executive
1549	Acquisition Specialist
167773	Enterprise Account Executive Osaka
85892	Consultant Business Analyst Learning Development
144400	Proposal Specialist
49652	Project Engineer
57967	Territory Account Manager
173214	Licensed Veterinary Technician
123812	Javascript Developers AI Training Charlotte USA
14453	Product Manager Human Robot Interaction
136571	Head of Channels
23552	RN Trach Care
115893	Contract Administrator
150302	Personal Trainer
141515	Barback Soho House Istanbul
22984	Program Specialist
150510	Personal Trainer
87033	Lead Product Manager
42413	Instant Commerce CRM Back end Engineer
16537	Engineer II Field Service
148809	Tech Ops Team Lead
169950	Staff Engineer AI Labs
12744	Engineering Manager Research Data Platform
164404	Patient Care Technician Program Director
122887	Database Administrator Graduates AI Training Zapopan Mexico
8804	IT Project Manager
131654	Staff Engineer
105015	Systematic Trading Technologist
62637	Sales Assistant
18629	Content Lead Copywriting Messaging
59966	Occupancy Specialist Euclid Hill Villa
84142	Sanitor 3rd shift
133831	Avionics Engineer I Design
99612	Therapeutic Account Manager Liver Los Angeles South
4798	Yucateco Dialect Specialist Freelance AI Trainer Project
39374	Controller
160918	Data Analyst Product
21808	Habilitation Technician DSP
114310	Public Funding Project Lead
5182	Total Rewards Lead
118476	Graduate Leadership Program Creative Account Coordinator
3468	LA Kings Ticket Operations Associate
123177	Finance Professionals AI Training Austin US
163942	Staff Product Manager Design and Intelligence
112178	E Scooter Delivery Driver Berkeley
119076	Creative Director CPG
163042	Platform Engineer AI
95972	House Manager
39892	Mid Market Account Executive German Speaking
108590	Staff Software Engineer Backend
42498	Program Management Selection COE Execution L6 I
48218	Entry Level Kitchen Position Madison Square Park
158083	Sales Representative
142718	Brand Strategist
137902	Enterprise Account Executive SLED
43649	Engineer Insights Reliability Engineering
109703	Stormwater Compliance Inspector Industrial Market
15939	Data Engineer
121334	Applied Scientist Supply
7245	Merchandising Sample Coordinator
65709	Business Analyst Digital Manufacturing Remote
173660	Mount Balance Warehouse Associate
169276	Veterinary Technician Student Externship Eden Prairie MN
16509	Engineer Field Service Early Careers
62552	Product Manager Poland Remote
25089	Engineer EIT Austin Public Works
161543	Research Intern Frontier Agents Summer 2027
113779	Assitante de vie
122812	Database Administrator Graduates AI Training Acapulco Mexico
56096	Operations Manager
162891	Owala Brand Designer
172955	Commercial Plumbing Technician
29608	Application Security Engineer II Contract 6 months
49504	Graduate Agronomist
27457	Psychotherapist
151753	Personal Trainer
68430	Seasonal Sales Associate Part Time Editor Seattle
72613	Veterinarian Partner Owner
131430	Launch Integration Technician
491	Dental Hygienist
43238	LVN LPT
63148	Director Product Partnerships
73074	Safety Specialist
120629	Investor Recruitment Swedish speaking
174277	Assistant Country Controller
120351	Institutional Investor Engagement Analyst
59943	Community Manager Affordable Housing
168238	Customer Experience Coordinator Part Time Charlotte NC
151303	Personal Trainer
9648	Software Engineer II Growth
10246	Controls Engineer Manufacturing Automation
130475	Director Revenue Strategy Operations Large Customer Sales
16213	Lead Product Manager Enterprise Services Management
21367	BCaBA Behavioral Health
4920	Auxiliar de Vendas e Atendimento Telêmaco Borba PR Temporário Exclusiva PCDs
69820	DevSecOps Staff Engineer
177149	Account Executive Defense
42452	Opportunistic Hire Crawling Engineer
58486	Finance
82088	Warehouse Team Lead
132136	Technical Support Representative Entry Level Fresh Graduates
89081	Scientist I Analytical Development
77899	Oracle EBS O2C G Invoicing SME
128019	Paediatric Community Speech and Language Therapist
121332	Supply Inventory Planner
53638	Software Architect
134885	Head of Digital Product
11333	Firmware Engineer Embedded Systems
157704	Project Manager ERP Implementation Contractor
41867	Biz Marketing Account Management Affiliate
72247	Machinery Service Technician Mechanic 1 Rentals
119477	People Operations Partner
56891	Application Support Engineer
15504	Consultor a Pedagógico a Maker
28187	Software Engineer Partner Integrations
144730	Enterprise Account Manager Aviation
18170	Wireless Camera Operator
71256	Garage Door Install Technician
163392	Creator Lead
165593	Associate Manager Agent Experience
```

## Predictions — do not read until step 3

What iteration 11's classifier answered for each of the rows above. Reading this before the labels
are written to disk destroys the measurement: the labeller would agree with it and the numbers would
decorate rather than measure.

```
22368	OUT
119637	OUT
56024	OUT
58410	OUT
175902	OUT
89261	UNKNOWN
41710	UNKNOWN
144241	OUT
76165	OUT
146210	OUT
117069	IN
119919	UNKNOWN
52258	OUT
123172	UNKNOWN
161016	OUT
149994	IN
164406	OUT
150907	OUT
137536	OUT
72059	UNKNOWN
147261	OUT
53056	UNKNOWN
119548	OUT
4159	OUT
159465	OUT
149331	OUT
11534	UNKNOWN
122476	UNKNOWN
172191	IN
17704	OUT
144858	OUT
89762	OUT
124332	OUT
115904	OUT
58605	IN
101144	UNKNOWN
123591	UNKNOWN
143174	IN
82583	OUT
25531	OUT
118662	UNKNOWN
163000	UNKNOWN
78065	UNKNOWN
20557	UNKNOWN
70125	OUT
124063	UNKNOWN
115403	UNKNOWN
71307	OUT
118921	OUT
4352	OUT
175587	UNKNOWN
151645	OUT
20398	IN
134453	IN
75472	OUT
155700	UNKNOWN
106125	UNKNOWN
118593	IN
177610	UNKNOWN
56633	OUT
160238	UNKNOWN
20367	OUT
11083	UNKNOWN
39542	OUT
141860	OUT
119962	OUT
37411	IN
35737	OUT
142653	OUT
3949	IN
148545	UNKNOWN
4218	OUT
133046	OUT
68655	IN
42809	OUT
160797	UNKNOWN
71855	UNKNOWN
108491	IN
10319	OUT
70713	UNKNOWN
124126	OUT
109645	OUT
128622	UNKNOWN
164315	OUT
24282	OUT
103298	IN
75444	UNKNOWN
31151	UNKNOWN
150535	OUT
141083	UNKNOWN
131256	OUT
1247	OUT
66792	OUT
31238	OUT
161687	UNKNOWN
176019	UNKNOWN
84127	OUT
122931	UNKNOWN
97825	IN
107563	UNKNOWN
41103	OUT
101194	UNKNOWN
79729	OUT
46490	OUT
173040	OUT
37564	IN
83859	OUT
149481	UNKNOWN
23932	UNKNOWN
11676	UNKNOWN
107640	OUT
77599	UNKNOWN
80396	UNKNOWN
92243	UNKNOWN
82601	UNKNOWN
40324	UNKNOWN
157103	UNKNOWN
80073	UNKNOWN
126586	OUT
58522	UNKNOWN
24107	UNKNOWN
157780	OUT
32348	OUT
77356	OUT
177499	UNKNOWN
147716	OUT
75239	OUT
14128	IN
155542	UNKNOWN
171547	OUT
41820	OUT
80301	OUT
84616	OUT
18116	UNKNOWN
29324	UNKNOWN
41708	OUT
133456	OUT
77014	OUT
7868	OUT
76727	UNKNOWN
8672	UNKNOWN
16390	UNKNOWN
105260	UNKNOWN
51306	IN
59600	IN
141053	OUT
166856	OUT
35166	OUT
67102	OUT
20727	UNKNOWN
88557	UNKNOWN
40372	UNKNOWN
171764	UNKNOWN
79382	IN
45769	OUT
125294	OUT
82779	OUT
65485	OUT
83854	OUT
74549	OUT
127136	OUT
100888	IN
128740	UNKNOWN
54625	OUT
117108	OUT
78851	UNKNOWN
94977	OUT
106193	OUT
30436	OUT
146467	OUT
88696	OUT
123778	OUT
23478	OUT
178395	IN
125788	UNKNOWN
44738	IN
108228	UNKNOWN
62858	OUT
143714	UNKNOWN
41576	UNKNOWN
16954	UNKNOWN
125187	UNKNOWN
83356	OUT
157764	IN
168607	OUT
15801	OUT
152072	OUT
147676	OUT
130346	UNKNOWN
120146	OUT
124339	OUT
127895	OUT
56476	OUT
124043	UNKNOWN
125711	OUT
1487	OUT
51160	UNKNOWN
64765	OUT
159661	UNKNOWN
138674	UNKNOWN
131000	UNKNOWN
70895	OUT
74688	UNKNOWN
84386	UNKNOWN
80063	OUT
43250	OUT
80520	OUT
122918	UNKNOWN
32874	UNKNOWN
53171	UNKNOWN
41059	IN
14392	OUT
43844	UNKNOWN
9710	UNKNOWN
130335	OUT
30423	OUT
59576	IN
100329	IN
109990	UNKNOWN
95961	UNKNOWN
102318	OUT
53097	IN
52390	OUT
56764	OUT
136724	UNKNOWN
71523	UNKNOWN
173834	OUT
152743	OUT
105895	OUT
19275	OUT
16872	UNKNOWN
108095	UNKNOWN
94436	OUT
26309	IN
171211	UNKNOWN
141704	IN
127693	OUT
8995	UNKNOWN
146742	UNKNOWN
66841	OUT
18796	UNKNOWN
106227	UNKNOWN
156665	UNKNOWN
4191	OUT
157240	OUT
497	OUT
152945	OUT
27758	OUT
85191	UNKNOWN
31741	IN
21052	OUT
148052	OUT
168383	OUT
30226	OUT
156523	IN
72649	UNKNOWN
10008	OUT
27215	OUT
22044	OUT
170947	OUT
86728	UNKNOWN
17047	UNKNOWN
165389	OUT
100728	UNKNOWN
176948	UNKNOWN
10203	UNKNOWN
154553	OUT
111625	OUT
22591	OUT
175438	UNKNOWN
128075	OUT
124068	UNKNOWN
106535	UNKNOWN
136437	IN
71223	OUT
96395	UNKNOWN
164678	OUT
4258	OUT
48285	OUT
150759	OUT
31254	UNKNOWN
170339	IN
51910	OUT
177384	UNKNOWN
23239	OUT
73603	OUT
117299	OUT
69981	UNKNOWN
92600	OUT
78887	OUT
5958	OUT
88485	UNKNOWN
162763	OUT
85649	OUT
12449	OUT
150354	OUT
61639	UNKNOWN
113076	OUT
9974	OUT
7438	OUT
129822	OUT
10472	OUT
64185	UNKNOWN
51941	OUT
31636	UNKNOWN
5539	OUT
153835	OUT
164858	UNKNOWN
78655	OUT
97446	OUT
6643	UNKNOWN
29785	UNKNOWN
125116	OUT
101991	UNKNOWN
149616	OUT
19044	OUT
83692	OUT
10027	OUT
88985	OUT
77566	UNKNOWN
32554	IN
102450	UNKNOWN
52926	UNKNOWN
102571	OUT
32972	UNKNOWN
46241	OUT
58771	OUT
73015	UNKNOWN
8760	UNKNOWN
162127	OUT
157563	OUT
105892	UNKNOWN
24978	UNKNOWN
132013	OUT
15203	OUT
168196	OUT
80586	UNKNOWN
123359	OUT
126002	OUT
160499	UNKNOWN
57982	OUT
33041	UNKNOWN
171963	UNKNOWN
19214	UNKNOWN
107857	UNKNOWN
83615	UNKNOWN
156548	OUT
86928	OUT
115081	OUT
8285	OUT
6003	OUT
58438	OUT
104236	OUT
104928	IN
112722	OUT
368	OUT
69986	OUT
171894	OUT
100331	UNKNOWN
158914	OUT
107403	OUT
45655	UNKNOWN
79875	OUT
117092	UNKNOWN
8427	UNKNOWN
158330	IN
48547	OUT
70053	OUT
175758	UNKNOWN
49854	OUT
68469	IN
27066	OUT
76255	OUT
70663	OUT
111573	OUT
135120	OUT
54870	OUT
118166	OUT
77949	IN
14642	OUT
161048	OUT
41583	UNKNOWN
114785	OUT
15894	IN
104165	UNKNOWN
79994	UNKNOWN
32324	UNKNOWN
64783	IN
28650	UNKNOWN
4504	OUT
163431	IN
122394	OUT
4427	OUT
19369	OUT
135271	IN
64687	UNKNOWN
19130	OUT
65107	OUT
106341	OUT
122664	UNKNOWN
136215	UNKNOWN
52937	UNKNOWN
5731	UNKNOWN
54690	OUT
13230	UNKNOWN
158742	OUT
86058	IN
119242	OUT
41661	UNKNOWN
158364	UNKNOWN
165523	IN
8799	IN
61550	UNKNOWN
81475	UNKNOWN
105612	OUT
123844	UNKNOWN
72753	UNKNOWN
56175	OUT
95307	UNKNOWN
1975	UNKNOWN
133454	UNKNOWN
21989	UNKNOWN
14356	OUT
107074	OUT
74322	OUT
109161	UNKNOWN
50826	UNKNOWN
163038	UNKNOWN
32027	UNKNOWN
144903	OUT
143888	OUT
176024	UNKNOWN
76270	OUT
48777	IN
48703	UNKNOWN
164065	UNKNOWN
91866	OUT
14677	UNKNOWN
101949	UNKNOWN
167963	OUT
134394	IN
165984	UNKNOWN
65754	OUT
91074	OUT
176847	UNKNOWN
29555	UNKNOWN
43264	OUT
96621	OUT
153922	OUT
8440	UNKNOWN
48620	UNKNOWN
45732	OUT
41465	UNKNOWN
50211	UNKNOWN
120321	OUT
157274	UNKNOWN
61451	OUT
74423	OUT
112328	UNKNOWN
75396	UNKNOWN
175321	OUT
177178	OUT
163694	UNKNOWN
122882	UNKNOWN
36689	OUT
25875	OUT
40678	OUT
50839	OUT
62271	IN
68019	OUT
96116	OUT
70261	UNKNOWN
78391	UNKNOWN
69700	IN
129646	UNKNOWN
61763	OUT
16692	UNKNOWN
122896	UNKNOWN
36340	OUT
22290	OUT
95763	OUT
9047	UNKNOWN
59260	UNKNOWN
5974	OUT
80310	UNKNOWN
10871	UNKNOWN
178154	UNKNOWN
171442	OUT
11070	OUT
87805	UNKNOWN
90005	OUT
132812	UNKNOWN
11698	IN
128149	OUT
93456	OUT
174805	IN
24235	OUT
41342	UNKNOWN
112048	UNKNOWN
6283	UNKNOWN
49897	UNKNOWN
167083	OUT
22031	OUT
35718	OUT
134195	OUT
25140	UNKNOWN
22446	OUT
13650	OUT
176957	OUT
65695	IN
9783	UNKNOWN
40488	OUT
77834	OUT
150628	OUT
80786	OUT
100615	OUT
61067	UNKNOWN
170923	UNKNOWN
145211	OUT
77156	UNKNOWN
41580	UNKNOWN
172985	OUT
152836	OUT
144881	UNKNOWN
141108	UNKNOWN
174140	OUT
122827	UNKNOWN
7155	OUT
54169	OUT
172970	UNKNOWN
134782	IN
43145	IN
174495	UNKNOWN
75366	UNKNOWN
62239	UNKNOWN
148329	UNKNOWN
136353	OUT
159137	OUT
90398	OUT
132025	UNKNOWN
8913	UNKNOWN
99936	UNKNOWN
40000	OUT
143162	IN
68796	OUT
58248	OUT
125410	UNKNOWN
9986	OUT
146754	OUT
161572	IN
40106	OUT
144325	UNKNOWN
177247	IN
156761	OUT
74802	OUT
37400	IN
85875	IN
89422	UNKNOWN
127653	UNKNOWN
140925	OUT
70610	UNKNOWN
163286	OUT
126988	OUT
165865	UNKNOWN
80630	UNKNOWN
56862	OUT
107441	UNKNOWN
122662	UNKNOWN
61499	UNKNOWN
178735	OUT
51805	OUT
124834	UNKNOWN
101474	UNKNOWN
91917	UNKNOWN
27434	OUT
149269	UNKNOWN
20070	OUT
19293	OUT
53592	UNKNOWN
102169	OUT
132720	OUT
114932	OUT
13244	IN
131553	OUT
109601	UNKNOWN
22629	OUT
102882	OUT
136696	UNKNOWN
170178	OUT
26493	IN
10200	OUT
98791	OUT
3635	UNKNOWN
99418	OUT
131773	OUT
27685	IN
28249	UNKNOWN
131982	OUT
154490	OUT
89128	UNKNOWN
101396	UNKNOWN
71453	OUT
52357	OUT
134203	OUT
39262	OUT
89156	IN
79781	UNKNOWN
64276	UNKNOWN
33580	OUT
95637	OUT
50084	UNKNOWN
22729	OUT
120416	OUT
54884	OUT
36749	UNKNOWN
126781	OUT
60571	UNKNOWN
138033	OUT
108677	UNKNOWN
129408	IN
135607	OUT
115537	UNKNOWN
31403	UNKNOWN
103846	UNKNOWN
520	OUT
16669	UNKNOWN
83765	UNKNOWN
89280	UNKNOWN
83023	OUT
75477	UNKNOWN
96376	UNKNOWN
139921	OUT
49473	UNKNOWN
7517	OUT
29848	UNKNOWN
166208	OUT
178006	OUT
67166	OUT
164261	UNKNOWN
116313	OUT
57716	OUT
167854	OUT
121034	OUT
31039	UNKNOWN
139753	UNKNOWN
120990	OUT
37363	OUT
81816	UNKNOWN
277	UNKNOWN
39200	UNKNOWN
102763	UNKNOWN
151780	OUT
25264	OUT
143552	OUT
9550	OUT
101546	UNKNOWN
35418	OUT
114953	IN
114232	OUT
35874	UNKNOWN
168969	OUT
19946	OUT
167096	UNKNOWN
24839	OUT
47567	OUT
10279	UNKNOWN
31214	UNKNOWN
89054	OUT
65805	UNKNOWN
90425	OUT
122890	UNKNOWN
89520	UNKNOWN
132885	UNKNOWN
156495	OUT
52359	UNKNOWN
35648	OUT
44390	OUT
99485	UNKNOWN
22695	UNKNOWN
109689	UNKNOWN
177775	OUT
5416	OUT
89367	OUT
19577	UNKNOWN
61533	UNKNOWN
20556	UNKNOWN
18668	OUT
16366	OUT
41278	OUT
62960	UNKNOWN
13300	UNKNOWN
82598	UNKNOWN
117496	IN
49388	UNKNOWN
62085	OUT
6868	IN
145186	UNKNOWN
15437	OUT
73905	OUT
117015	UNKNOWN
72380	UNKNOWN
79884	OUT
127246	OUT
104295	OUT
124018	UNKNOWN
115860	OUT
103849	UNKNOWN
67010	OUT
149997	IN
40532	OUT
132971	IN
132417	OUT
93130	UNKNOWN
174368	UNKNOWN
136940	UNKNOWN
33457	OUT
164999	UNKNOWN
15515	UNKNOWN
170373	OUT
88476	OUT
11285	OUT
19708	OUT
100281	UNKNOWN
172270	OUT
122779	OUT
973	UNKNOWN
157711	OUT
8935	IN
142695	UNKNOWN
155178	OUT
1550	OUT
155560	IN
83120	OUT
176379	UNKNOWN
93546	OUT
94332	OUT
111591	OUT
149281	UNKNOWN
103775	UNKNOWN
106111	UNKNOWN
60758	IN
132134	OUT
147431	UNKNOWN
103894	UNKNOWN
115552	OUT
6366	IN
137482	UNKNOWN
121322	UNKNOWN
34244	OUT
165492	UNKNOWN
109603	UNKNOWN
68085	IN
28999	OUT
177387	UNKNOWN
50495	UNKNOWN
31640	UNKNOWN
167323	OUT
53338	UNKNOWN
29326	OUT
48100	IN
97961	UNKNOWN
167256	UNKNOWN
256	UNKNOWN
92111	UNKNOWN
13852	OUT
31211	UNKNOWN
3099	UNKNOWN
116907	UNKNOWN
119680	UNKNOWN
84363	UNKNOWN
125180	OUT
92288	OUT
44952	UNKNOWN
141753	OUT
82847	OUT
11907	OUT
175432	OUT
37606	UNKNOWN
132051	UNKNOWN
46680	UNKNOWN
4486	UNKNOWN
64385	UNKNOWN
105962	UNKNOWN
41831	UNKNOWN
9951	OUT
106128	IN
154036	OUT
78343	OUT
103035	UNKNOWN
79730	OUT
53763	OUT
90658	OUT
12684	UNKNOWN
135517	OUT
87474	UNKNOWN
176717	IN
164956	OUT
119493	OUT
72713	UNKNOWN
14663	UNKNOWN
133813	UNKNOWN
79749	OUT
21395	OUT
29605	OUT
72046	UNKNOWN
49437	IN
173522	OUT
177593	UNKNOWN
140123	OUT
63229	UNKNOWN
28683	OUT
156655	OUT
57112	UNKNOWN
6542	UNKNOWN
122836	UNKNOWN
73131	UNKNOWN
10655	UNKNOWN
107603	IN
227	OUT
101501	UNKNOWN
29277	UNKNOWN
10467	OUT
172927	OUT
110888	OUT
61137	UNKNOWN
155813	UNKNOWN
146886	OUT
152280	OUT
49254	UNKNOWN
63147	OUT
158916	OUT
8175	UNKNOWN
104107	UNKNOWN
126783	OUT
156088	OUT
23332	OUT
63959	UNKNOWN
100660	UNKNOWN
137944	UNKNOWN
91350	OUT
145019	IN
18061	UNKNOWN
140645	UNKNOWN
178356	UNKNOWN
151996	OUT
141914	UNKNOWN
165751	OUT
166343	OUT
30016	IN
122601	OUT
84695	OUT
16236	UNKNOWN
69933	OUT
89270	UNKNOWN
156319	OUT
164596	OUT
98012	OUT
22548	OUT
173560	UNKNOWN
40777	OUT
28246	OUT
111554	UNKNOWN
78534	UNKNOWN
32725	IN
163619	UNKNOWN
158360	UNKNOWN
91658	OUT
54665	OUT
116536	UNKNOWN
123121	OUT
19301	IN
42940	OUT
107013	UNKNOWN
109267	OUT
108347	OUT
128506	UNKNOWN
106106	IN
50042	IN
108508	IN
73175	OUT
146191	OUT
1549	UNKNOWN
167773	OUT
85892	UNKNOWN
144400	OUT
49652	UNKNOWN
57967	OUT
173214	OUT
123812	UNKNOWN
14453	UNKNOWN
136571	OUT
23552	OUT
115893	UNKNOWN
150302	OUT
141515	UNKNOWN
22984	UNKNOWN
150510	OUT
87033	UNKNOWN
42413	IN
16537	UNKNOWN
148809	OUT
169950	IN
12744	IN
164404	OUT
122887	UNKNOWN
8804	IN
131654	UNKNOWN
105015	OUT
62637	OUT
18629	UNKNOWN
59966	UNKNOWN
84142	UNKNOWN
133831	OUT
99612	OUT
4798	OUT
39374	UNKNOWN
160918	UNKNOWN
21808	OUT
114310	UNKNOWN
5182	UNKNOWN
118476	OUT
3468	OUT
123177	OUT
163942	UNKNOWN
112178	OUT
119076	OUT
163042	IN
95972	OUT
39892	OUT
108590	IN
42498	OUT
48218	UNKNOWN
158083	OUT
142718	OUT
137902	OUT
43649	UNKNOWN
109703	OUT
15939	IN
121334	UNKNOWN
7245	OUT
65709	UNKNOWN
173660	OUT
169276	OUT
16509	UNKNOWN
62552	UNKNOWN
25089	UNKNOWN
161543	UNKNOWN
113779	UNKNOWN
122812	UNKNOWN
56096	OUT
162891	OUT
172955	OUT
29608	IN
49504	UNKNOWN
27457	OUT
151753	OUT
68430	OUT
72613	OUT
131430	OUT
491	OUT
43238	UNKNOWN
63148	OUT
73074	UNKNOWN
120629	UNKNOWN
174277	OUT
120351	UNKNOWN
59943	OUT
168238	OUT
151303	OUT
9648	IN
10246	OUT
130475	OUT
16213	UNKNOWN
21367	UNKNOWN
4920	OUT
69820	IN
177149	OUT
42452	UNKNOWN
58486	UNKNOWN
82088	OUT
132136	IN
89081	UNKNOWN
77899	UNKNOWN
128019	OUT
121332	OUT
53638	IN
134885	UNKNOWN
11333	IN
157704	OUT
41867	OUT
72247	OUT
119477	OUT
56891	UNKNOWN
15504	UNKNOWN
28187	IN
144730	OUT
18170	OUT
71256	OUT
163392	UNKNOWN
165593	OUT
```
