# Iteration 7 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. The criterion did not change during this iteration: it stands at
revision `33113b2` plus the uncommitted working-tree edit that added the Q1 vacancy gate, which is
the revision iteration 6's rules were already written against.

The filename carries 2026-09-27 rather than the day it ran, for the reason iteration 3 gave: the
six earlier iterations already hold the six days before it, and the next session finds its input
by taking the newest file.

This iteration labelled the 1000 rows iteration 6 drew, scored iteration 6's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-26.tsv`; they were written to disk before any
rule of the classifier was read.

**The labelling was done by a subagent**, the way iterations 5 and 6 did it: the agent was given
the criterion and the 1000 bare titles, denied the classifier's source, the iteration reports and
the earlier label files, and told to write its labels to disk before anything else. Blinding is
ordering, and the ordering was enforced by what the agent could reach.

**This iteration was also run to a context budget**, which is a change in how the session works
rather than in what it decides. Scoring, the rule pricing and the draw were all done by scripts
over files — the 1000 rows, the 6000 accumulated labels and the corpus never entered the session's
context — and the rules were priced offline with a four-class `javac` harness rather than by
reading the classifier's lists. Nothing about the method requires this, but it is what let one
session do the whole iteration.

## The numbers

The two rates score **iteration 6's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change.

| Gated number | Iteration 6 | Iteration 7 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | 1.17% | **0.67%** (4 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | 9.00% | **5.00%** (5 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 26.11% | **24.38%** | fell < 1 pp |

The corpus after the change:

| | Iteration 5 | Iteration 6 | Iteration 7 |
| --- | ---: | ---: | ---: |
| `IN` | 16.92% | 17.07% | **17.12%** (30 661) |
| `OUT` | 51.73% | 56.82% | **58.50%** (104 769) |
| **`UNKNOWN` — the gated share** | 31.35% | 26.11% | **24.38%** (43 668) |
| &nbsp;&nbsp;of which `unruled` | 9.91% | 7.00% | **5.82%** (10 416) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 19.57% | 17.54% | **17.00%** (30 440) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.87% | 1.57% | **1.57%** (2 812) |

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 6 called `UNKNOWN`, the
labeller called 15 `IN`, 173 `OUT` and 112 `UNKNOWN`. One unknown title in twenty is an
engineering role, against one in twelve last iteration, so the pile stopped getting richer in `IN`
— but 173 `OUT` rows in a 300-row stratum say the harvest is still where it was: the corpus keeps
handing back titles that are plainly not engineering roles and that no rule reached. They paid for
most of this iteration's change.

The labeller's own unknown pile split 65 `scope_ambiguity`, 61 `unruled`, 56 `domain_ambiguity`.

## The exit gate

Both rates pass, for the fifth iteration running, and by the widest margin the loop has recorded —
0.67% against a 2% gate and 5.00% against a 10% one. The unknown share fell 1.73 points where the
gate asks for less than one, so the two-consecutive-iterations count restarts again.

This is the fourth iteration in a row where the two quality gates hold and the rate-of-change gate
does not. The note iteration 6 left about this stands unchanged and is repeated under the
questions below.

## Cost

A full pass over the 179 098 cleaned titles takes **0.377s** against the ten-second limit, measured
over the corpus titles read from the database with the classifier warm. Whole-word,
case-insensitive matching over the cleaned title; no description bodies, no network.

## What changed, and why

Everything below was priced against the 6000 accumulated labels before it was kept, and the whole
change was then re-measured over those 6000 rows: it moves 153 of them, **137 into the state its label
gives it, 16 into or between states the label left undecided, and no new error of either kind**.

- **Twenty-three never-engineering heads**, all of them heads the `unruled` pile named and no
  software domain turns into engineering work: `pediatrician`, `physiatrist`, `psychotherapists`,
  `registrar`, `crna`, `dvm`, `orthotist`, `hygienist`, `sonographers`, `sommelier`, `polisher`,
  `juicer`, `usher`, `concierge`, `interpreters`, `cleaner`, `negotiator`, `trader`, `evaluator`,
  `grader`, `scriptwriter`, `interventionist`, `rodman`.
- **The officers a title names by their abbreviation** — `cfo`, `cmo`, `chro`, `coo`, `sdr`, `gm` —
  are never-engineering heads. The chiefs that are engineering roles, the technology and
  information officers, are rulings and a ruling runs first, so the two do not collide. `gm` says
  what the `general manager` ruling already says.
- **`vp`, `avp` and `mgr` are heads**, domain-bound and engineering-capable, because they are
  `director` and `manager` written short and the corpus writes them short constantly. `VP Legal`
  decides out on the marker it names; `VP Growth` stays `domain_ambiguity`, which is what a bare
  management head does.
- **`internship`, `externship`, `extern` and `werkstudent` are heads.** An internship is a vacancy
  and its modifier carries the domain, exactly as `intern` does; fourteen of the 22 rows they
  reach in the fixtures decide out on a marker that was already there, and none of them moves a row
  away from its label.
- **Thirty-seven off-domain markers**, all from titles the `domain_ambiguity` pile held and the
  labeller read as plainly non-software: `water`, `low voltage`, `private equity`, `compensation`,
  `benefits`, `bookkeeping`, `budget`, `government affairs`, `demand generation`, `chef`,
  `kitchen`, `hospice`, `wellness`, `personal care`, `kids`, `farm`, `shipping`, `mine`,
  `explosive`, `gene`, `immunology`, `chemistry`, `photonics`, `renovations`, `interiors`,
  `housing`, `telesales`, `esg`, `underground`, `commissioning`, `vfx`, `speech`, `case manager`,
  `tiktok`, `biometrics`, `veterinarian`, `anesthesia`.
- **Two of those markers pay back a cost two earlier iterations pinned.** `chef` and `veterinarian`
  are professions the leadership and associate heads were masking — `Head Chef` and `Associate
  Veterinarian` were the priced cost of adding `head` in iteration 5 and `associate` in iteration
  6. As markers rather than heads they decide those titles out without giving the masked head back
  its power to swallow the title. The tests that pinned the cost now pin the repayment.
- **Six software qualifiers**: `incident response`, `forensic`, `dfir`, `postgresql`, `datapath`,
  `model training`. With them, `dba` and `engg` are heads — a PostgreSQL DBA and an embedded
  software engg are both engineering roles that no head reached.
- **Four rulings.** `data warehouse` is `IN`, because a data warehouse is software however the
  `warehouse` marker reads the word, and `machine learning engineer` is `IN` whatever science the
  title says it serves — those were two of the four misses. `research opportunity` and `talent
  communities` are `OUT` under Q1, the two shapes of the posting-that-hires-nobody this sample
  wrote that iteration 6's twenty rulings did not already hold.

### Tried and dropped, with what they cost

- **`engineering` as a software qualifier** — the widest-reaching candidate this sample offered,
  and an even trade: across the 6000 labelled rows it decides 8 `domain_ambiguity` rows correctly
  `IN` and 8 others wrongly `IN`. `Engineering Manager Travel` and `Engineering Team Manager` are
  engineering roles; `Summer 2027 Internship Electrical I&C Engineering` and
  `Director Learning and Development` are not, and the word cannot tell them apart.
- **`shop`** — 209 corpus titles name it and most are retail, but 6 are full-stack engineers on a
  product called Shop. `tiktok` takes the two rows this sample actually wanted.
- **`server` as a never-engineering head** — 132 corpus titles name it and they split down the
  middle between restaurant servers and server engineers. A head that reads `Server Engineer` as a
  waiter is not worth one restaurant row.
- **`sw` as a software qualifier** — gains one row and costs one: 38 corpus titles name it, most
  of them software but some a street address or a clinic.
- **`defense` as a marker** — 2 misses against 1 gain over the fixtures.
- **`analytics` as a qualifier** — dropped again, on the same measurement iteration 6 made: 1 gain
  against 2 costs.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows the change was grown from. Two misses and one false accept survive:

- `Java Backend Engineer Asset Earn` — the `asset` marker beats the backend qualifier. This is the
  customer-vertical family again: the asset is what the software is *for*.
- `TypeScript Coding Specialist Freelance AI Trainer Project` — the `ai trainer` ruling decides it
  out, which is right for the family and wrong for this row.
- `Control System Engineer Site Reliability Engineer SRE` — two heads that disagree, and the
  criterion's first-head rule picks the control system one. The labeller flagged this row itself as
  undecidable.

## What the next session should look at

**The unknown share is 24.38% and 17.00 of those points are `domain_ambiguity`** — seven tenths of
what is left. `unruled` is 5.82% and still falling, and it has now fallen far enough that the
profession harvest which paid for this iteration will not pay for another one: what is left in it
is mostly foreign-language titles, one shape at a time.

**The `UNKNOWN` stratum is still 58% `OUT`.** Three iterations have harvested it and it keeps
refilling at roughly the same rate, which says the domain vocabulary of this corpus is long-tailed
rather than nearly covered. Expect markers to keep paying, in smaller pieces.

**Watch the new markers with the widest reach** — `speech`, `chemistry`, `kids` and `housing` each
touch more than a hundred corpus rows, and `chemistry` is pinned costing one real software title
(`Staff Software Engineer Machine Learning and Computational Chemistry`) because step 4 lets the
marker beat the qualifier. One bad row in iteration 8's false accepts is worth taking seriously.

## Questions for the criterion, not for the rules

**The customer vertical against the work domain** — unchanged since iteration 5, and still the
shape of one of the two surviving misses. Step 4 cannot tell who the work is *for* from what it is
*in*.

**Q2 against Q4 on hardware-adjacent code roles** — unchanged since iteration 6. This iteration's
labeller called `Functional Safety Engineer`, `Lead Emulation Engineer` and
`Software Engineer Thermal Fluid Analysis` out on Q4 while flagging all three as undecidable, which
is the same split iterations 5 and 6 recorded from opposite sides.

**A title in a language the lists do not hold.** This labeller called every non-English title
`unruled` on the ground that the criterion's matching is whole-word over English lists — about 25
rows of the thousand — even where the meaning was plain (`整備士訓練開発エンジニア`,
`Técnico em segurança do trabalho`). That is a convention about the labeller's job that the
criterion does not state, and a different labeller could reasonably read those titles and decide
them. It matters because the classifier is accumulating Spanish, Portuguese and French heads: the
rules are being asked to read languages the labelling convention refuses to.

**The exit gate's third condition** — unchanged from iteration 6, and now four iterations old. Both
quality gates have held since iteration 4. The unknown-share condition measures the loop's rate of
change rather than the classifier's quality, so it is met either by converging or by declining to
make changes the sample has already shown are right. This iteration had 173 rows of evidence in the
`UNKNOWN` stratum alone and took them, which costs the gate, and the same trade will be on the
table in iteration 8.

## Held out of the draw

The 6000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 169 685 of the
179 098 rows were eligible.

## The sample for iteration 8

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
40838	Data Center Technician Chester VA
65648	Product Manager Peer to Peer Events
115085	Software Engineer II Appian Group
46328	Platform Engineer
171637	Security Officer Level 3 Utility Von Ormy TX
7115	Financial Advisor
6818	Account Executive
101914	Licensed Clinical Social Worker LCSW Remote
139972	Market Intelligence Representative MIR French Speaker
111915	Software QA Specialist II
21110	Program Associate Climate
51564	Medical Receptionist PRN
64732	Manufacturing Maintenance Technician 2nd Shift
63132	Associate General Counsel Product Regulatory
22501	New Grad Registered Nurse RN
78172	Service Technician
172909	Part Time Nanny Wheaton IL
68552	Staff Data Engineer Accounting
105138	Quality Assurance Engineer
141431	Strategic Customer Success Manager
34723	Home Health Nurse LPN RN
86550	Patient Specialist Freeport Illinois 1099 Contractor Per Diem On Call
123933	Korea Residents Survey Participants Gimhae South Korea
152538	Personal Trainer
44399	Business Development Executive
76172	Manager Transaction Management
86769	Lead Teacher Kid s Country
56800	Architect Perimeter DMZ
114701	Pilot Production Technician Structures
132601	Your next role with Revivn
107312	Director Operational Procurement
138174	Conference Operations Organiser
74444	Account Executive Public Relations Cybersecurity
82704	Sales Assistant Part Time Categorie Protette L.68 Reggio Emilia C.C I Petali
25121	HOUSTON Public Works Internship
94545	Seasonal Ambassador Maine Mall
45262	Forward Deployed Engineer FDE Financial Services
163724	Director FP&A
86473	Associate Vice President Strategy
70317	FP&A Manager
6177	Clinical Pharmacist Ambulatory Care COPD Population Health Program
21504	Certified Nursing Assistant CNA Pediatrics
28508	Plumbing and Fire Protection Engineer
20917	Pre Award MXM System Administrator IV
101126	Offline Customer Support Officer Bayelsa
114885	Enterprise Engagement Manager CEE
29228	Associate Counsel
53874	Quality Technician
170796	Program Manager Women Leaders India
103171	Director Business Development USG Federal
127541	Consultant in Stroke Medicine Norwich
35462	Submitting for a General Application
41763	Director II Compliance Privacy Compliance Monitoring
85327	Head of Performance
86098	Business Analyst Power BI with Snowflake
50209	SPRINT Specialist
130500	Product Manager Developer Ecosystem
109359	CAD Technician Airport Design
177115	Partner Manager RFQ
3663	Analytics Lead Full Stack
165433	FP&A Analyst
94687	CEI Inspector Aid Inspector I
5205	Account Manager MEA Turkish Speaker Cairo Based
116579	Apply Here for Future Physician Openings
133873	Cybersecurity Engineer II Secret Clearance
116761	Data Engineer
109131	Quality Control Specialist
109015	Estimator Chicago
69301	Sales Operations Manager
126280	Band 6 7 Locum Physiotherapist MSK Maidenhead
153991	Personal Trainer
126598	Band 6 Locum Sleep Physiologist Leigh
105143	Software Architect SaaS Platform
15424	Project Manager Food and Beverage Construction
7897	Seasonal Operations Associate Part Time The Forum Carlsbad
13822	Software Engineer Growth
141986	Radiology Technologist II CT Technologist X Ray
7851	Seasonal Operations Associate Part Time Park Meadows Mall
44746	Deployment Strategist
28013	Local Driver CDL
90231	Medical devices sales specialist
131020	Head of Engineering
78156	Route Sales Tech Denver
32570	Financial Planning Analysis FP&A Analyst 1 Year Engagement
140850	Field Service Engineer Midwest Minneapolis MN
64031	Staff Field Systems Engineer RF Systems
154537	Personal Trainer
159910	Leasing Consultant
104461	AI Full Stack Developer
15804	Arnold Veterinary Hospital Veterinary Assistant
72245	Machinery Service Technician Mechanic 1 Lakeside Shop
81719	Quantitative Research PhD Graduate
109654	Construction Materials Testing Technician
112715	Treatment Specialist Student New Grad Part time
59681	Analyst Product Remote
173168	Experienced Veterinary Technician
85467	IT Engineer Enterprise Systems
164575	Project Manager
406	Associate Dentist Full Time
131010	Manufacturing Technician 6 months contract
173004	Ultrasound Tech PRN Wolf River
12062	Software Engineer Sensor Fusion
81623	Specialist I Quality Control Compliance Contract to Hire
141947	Manager Packaging Artwork
105782	Revenue Operations Manager GTM Systems and Automation
31351	Engenheiro de Planejamento Híbrido Rio de Janeiro RJ
18286	Civil Engineer EIT Residential Land Development
171148	Commissioning Engineer
38548	Sample Operations Manager
172734	Traveling Occupational Therapist
90488	DoLS Authorised Signatory
35473	Poker Shift Manager
96272	Manager Supply Chain
67390	Implementation Consultant
164413	Branch Manager
173711	HR People Partner
23915	Part Time Tele Neurology Clinic Michigan or Active IMLC
130163	Director Advertising Analytics
27897	Credentialed Veterinary Technician New Jersey Paramus
133254	Sales Recruiter
92499	Journeyman Electrician
42256	部分遠端 正職排班 Associate Catalog Operation 商品資料營運專員
123323	France Residents Survey Participants Villeurbanne France
10575	Integration and Test Engineer
26333	Staff Machine Learning Engineer Modeling Support
45786	Manager Customer Success Boston
124496	Pharmacists AI Training Kansas City US
101995	HR Payroll Operations Support
77932	Alternatives Distribution Outbound Specialist Analyst Associate
55359	Group Fitness Instructor
33686	Behavior Technician
87281	Estate Business Planning Attorney 3 7 Years
57660	Non CDL Delivery Driver
64131	Field Service Technician Installations Edinburgh UK Fixed Term Minimum 20 Hours Week
109488	Entry Level Rail Water Resources Engineer
43605	Media Buyer Integrated Media Investment Temporary
54731	Equipment Technician
84783	Responsable de secteur Ouverture d agence Services à la personne
105900	Assistent Accountant Samenstelpraktijk
45034	Software Engineer Distributed Data Systems
166784	Manufacturing Coordinator
49588	Sales Development Representative Attribute Remote
19391	Product Designer
25581	Open Application
54362	Assistant Kids Club Manager
10998	Production Test Engineering Manager
46058	Strategic Account Executive SLED
71605	Registered Dietician
35170	Events Producer
31477	Lead PMO
147364	Agente Stone Consultor a Comercial Externo Uberlândia MG
63458	Customer Journey Manager
175639	Data Engineer
148785	Strategy Ops Enterprise
131435	Manager Corporate Finance
4638	Science Specialist Fluent in Hindi Roman Script Freelance AI Trainer Project
114525	Account Manager
90745	Occupational Therapist Sensory Service
174910	Technical Logisticians
107473	Business Development Director US Remote
68731	Sales Development Representative Nashville TN
44795	Engineering Manager Ingestion
89200	Site Acquisition Lead
149522	Deputy Money Laundering Reporting Officer
57189	District Operations Manager
109255	GenAI Influencer Marketing Manager
149596	Field Sales Executive
80050	SDE IV Data Engineer
57550	Heavy Equipment Shop Technician Mechanic
135714	Analyst GTM Business Operations Public Sector
86991	Marketing Brand Creative Events Co op Spring 2027
46271	Health Information Specialist II
71503	AI Platform Engineer
2277	Vice President
35099	Email Marketing Analyst at CFO Insights
148063	Project Finance Coordinator
145953	Vice President Investor Operations
64016	Production Test Engineer
55480	Group Fitness Instructor
10416	Environmental Safety and Health Associate Manager Training
44517	Administrative Assistant II DHS Suitability
3104	Manager Business and People Experience Platforms
53175	Director Business Analytics
39354	Cultivation Technician I
176934	Delivery Manager
115099	Capital Markets Analyst Loan Trading
38843	Strategy Analyst ComparaJa
10694	Manager Product Management Finance Solutions
39758	Installer Helper
113737	Assistante de vie
51538	Medical Assistant We will train Full benefits no weekends PTO
90674	Occupational Therapist Equipment Adaptations
69661	Software Engineer
131373	Quality Engineer II Test Launch
91667	Social Worker Looked After Children
139780	Full Time Caregiver NOC
85258	Associate Director Analytics
61591	Legal Counsel Live Productions US UK Qualified
93482	Sales Specialist Beverly Hills
64302	Temporary Helpdesk Engineer
50339	Cloud Infrastructure Engineer
54647	Certified Personal Trainer
90039	Project Manager
134353	Join Our Talent Community United States
46862	PreK Teacher s Assistant
105536	Electrical Engineer Intern Robotics and Surgery Engineering
105689	Solution Executive Digital
44322	Retail Sales Associate Part Time
160896	Software Developer AIO Typescript
25233	SARASOTA Land Site Development New Grad
96699	Cardiology Nurse Practitioner Physician Assistant
30268	Gerente de Relacionamento C6 Empresas Pessoa Jurídica Itajaí Santa Catarina
143740	Lead Environmental Health Safety Engineer Construction Safety
117184	Director of Sales Europe
175392	WPP SSC MY Thai Speaker Finance Analyst PTP Account Payable Invoice Payment
142109	Staff Machine Learning Engineer
19644	Manager Equity Compensation
24854	Sports Betting Analyst
129655	Grupo QuintoAndar Analista de Planejamento e Performance Sênior Vaga Afirmativa PCD
41784	Director Sales Strategy Planning Rocket Growth
144303	Quality Inspector Valves
29727	Brand Ambassador NL Telefonisch Remote
101733	Raw Material Planner
94330	Materials Technician I Internship
39623	Regional Sales Manager German Speaking
31610	Python with Django Python with Django L3 5.1 7 years
151686	Personal Trainer
164218	Hardware Engineer Manager Embedded System
22642	Outpatient Physical Therapist PT Living
138451	Director of Regulatory Reporting
59076	Workers Comp Lost Time Claims Subject Matter Expert User Research AI SaaS
18552	Director Executive Communications
145112	Technical Recruiter Bastrop
172851	Nanny Golden CO
78839	Software Engineer Research
35887	Director Product Solution Marketing
109817	Director Clinical Quality
70997	Assistant Director
175651	Data Management and BI Executive GOC
119569	Procurement Manager
90392	Advanced Social Worker Fostering Families and Friends Assessment
106431	Travel Clinical Nurse Instructor Iowa Area
17345	Flight Software Engineer
37464	Named Account Executive Bengaluru
39881	Software Engineer Product
89231	Account Executive Utilities
77597	Operations Specialist
101594	Consumer Engagement Ambassador Monster Energy
97899	Client Financial Analyst
140359	Forward Deployed Strategist Public Sector
48717	Accounts Receivable Specialist
144525	Software Engineer Test Infrastructure Application Software
141699	Housekeeping Manager Pre apertura Soho House Los Cabos
168511	Emergency Credentialed Veterinary Technician San Jose CA
173587	Manager Data Program Management
110960	Seasonal Store Advisor Palo Alto
167660	Veterinary Internist
131053	Operations Associate Part Time Cabazon Outlet
165447	Applied Scientist
76422	Real Estate Acquisition Consultant
4219	Garment Manufacturing QC Specialist Freelance AI Trainer Project
163229	Software Engineer Backend Services
123399	Game Development Environment Artist Manchester UK Freelance Remote
121352	Product Engineer
11321	Fellow Energetics Safety
105942	Manager Audit
84944	Lead Analyst Analysis Efficiencies
173007	Mental Health Care Coordinator
135152	Supervisor Hotel Operations
80747	Account Executive Commercial
138841	Product Design Engineer II Motorized
90802	Referral and Assessment Advanced
160704	Associate Sub Editor
170715	RN Registered Nurse PRN Weekends
144639	ASIC DFT Engineer Silicon
1582	AWS Database Engineer
2270	Staff Software Engineer
54504	Certified Personal Trainer
177220	Staff Cyber Resilience Engineer
65858	Manager Environmental Health Safety Data Centers
29211	Home Health Physical Therapist PRN Per Diem
22004	Home Health Physical Therapist
63385	Staff Program Manager Strategic Initiatives Automation
6937	Foundations and Collections Collections Subject Matter Expert Mid 850
72116	Global Sourcing Manager Marketing
130287	Process Mechanical Engineer Co op Intern
9453	Traveling Superintendent Healthcare Construction
2163	ISSO
20868	Intelligence Operations Integrator
133164	QEC Researcher Frontier
111853	Spanish Speaking ABA Paraprofessional RBT Brentwood NY
56560	Stretch Manager
110353	Health Physician Sign On Bonus Available
23265	Registered Nurse RN Home Health Full Time Baylor
139444	Enablement Advisor
64976	Strategic Customer Success Engineer Central US
159296	Medical Assistant Outpatient Oncology Bilingual Spanish or Chinese
115777	PRN Pharmacal Sales Representative III Central Valley CA
8315	Lead Tech Delivery
91014	Social Worker Adult Generic Team
117105	SDR OTR Netwrok
82722	Sales Assistant Part Time Firenze C.C I Gigli
73346	Compassionate Caregiver Needed Como NC 27818
154226	Personal Trainer
72365	PRN Per Diem Research Nurse LVN
119957	Conference Operations Executive
164094	Director of Collections
79528	Graphic Designer Data Visualization Remote Contract
104686	Employment Counsel
129863	Product Designer
176038	Planificateur.rice Activation Numérique
96107	VP Communications
117430	Repartidor de Tarjetas Medio tiempo BONO GARANTIZADO
88984	Wealth Advisor Sweden
9619	Spontaneous Application Infrastructure Engineer
98252	DevSecOps Engineer III 6830
128293	Specialty Dr for Old Age inpatient role in Norfolk
123451	German Fluent Speakers AI Training Stuttgart Germany
74865	MEP Preconstruction Manager
97530	Seasonal Stylist Retail Part time
58330	Client Relationship Manager
88804	Corporate Account Executive EMEA
8476	Enterprise Account Executive Corporate
160023	Maintenance Technician Temporary
147845	Vaga Temporária Auxiliar de Atendimento Logístico CNH A Moto Brasília DF
58846	Regional Sales Director
151951	Personal Trainer
94338	Survey Project Manager
28003	Residential Warranty Project Manager
151850	Personal Trainer
101128	Offline Customer Support Officer Borno
52029	Virtual Teacher CTE Technology PA TX
54149	Assistant General Manager
171782	Customer Support Representative
94031	Medical Assistant Ctr Fetal Maternal Medicine
139248	Manager Customer Success
150836	Personal Trainer
153563	Personal Trainer
79986	Health Care Contact Center Supervisor Remote
29708	Chief Luxury Correspondent
177083	DFM Project Engineer
170145	Executive Assistant Temporary
38355	Onchain Investigator
41843	Manager Quality Assurance IPO
65384	Staff Machine Learning Scientist
108250	Human Factors Engineer
52570	Dental Hygienist
171674	Quantitative Research Development
69652	Project Manager
32978	Shield Consulting Accounting Manager
92240	Success Program Manager
70201	Greycroft Public Opportunities Fund Fall 2026 MBA Intern
76698	Valuations Analyst
128824	Worldwide MSP Enablement Manager
117185	Director of Sales Middle East
30677	Costumed Characters Mascot Guest Experience Host
136938	Creative Marketing Manager
104990	Manager of Clinical Affairs
33122	Associate Value Engineer AI Driven Data Science Analytics Orbit Program
37578	Strategy M&A Lead
66919	Sports Data Collector Football Parnaíba Piauí Brazil
145000	Screen Printing Equipment Engineer Solar Cells Starlink
167535	Retail Sales Associate Part Time
152087	Personal Trainer
136359	Engineering Manager Global Public Sector
96977	Primary Care Nurse Practitioner
128629	MTS Exa
38911	Psychiatric Nurse Practitioner
163082	Staff Software Engineer Elixir
92602	QA QC Technician
152992	Personal Trainer
129109	AI Partnerships
4705	STEM Specialist Fluent in French Freelance AI Trainer Project
177990	Head of Organic Growth
52131	School Based Mental Health Technician K 5
72636	Veterinary Technician 5 000 Sign On Bonus for CVTS
55148	General Manager
155966	General Application for Clinical and Corporate
73962	Hiring Compassionate Female Caregiver Sevierville TN 37876
34382	Registered Behavior Technician RBT
21977	Home Health Aide Pediatrics
122345	Biology Graduates AI Training Canterbury UK
79641	Middle+ Project Manager Remote Contract
66003	Front End Engineer
49296	Head of Visuals
8966	Manager Trading Credit Risk Margin Options
40250	Registered Nurse RN Urgent Care
25353	BIBIBOP Team Member Dublin Green
171044	XPU Architect
63917	Marketing Intern Spring 2027
166230	Physician OB Gyn
47697	Material Manager
167595	CQV Engineer New Grad
113734	Assistant e de vie CANNES
23540	RN Registered Nurse Home Health
21089	Specialist Direct Investments
119072	CPG Paid Advertising Strategist Client Experience
116611	CFD Multiphase DEM Engineer
34613	Registered Behavior Technician RBT
118719	Product Designer Knowledge Graph Intelligence
51678	Optical Technician Optician
83955	HVAC Installer
53233	Account Executive Hotels
103156	Mission Operations Engineer
13998	Enterprise Account Executive Intelligence Community
124113	Material Sciences Graduates AI Training Newcastle upon Tyne UK
121681	AI Trainer Electrical Engineers CAD Python Expertise Remote Advisory UK
86491	Analytics Implementation Specialist
166351	Nuclear Instrumentation Controls Engineer Hardware
110982	Lead Footwear Costing
30573	IT Certification Bootcamp Instructor AWS Certified Cloud Practitioner
173962	General Liability Associate Attorney
91630	Social Worker Looked After Children
64477	Incentive Analytics Manager
101902	Clinical Supervisor Therapist Remote
105747	Scientist Translational Biomarkers
168831	Emergency Veterinary Assistant Fort Worth TX
165740	Staff Software Engineer Front End
115741	Occupational Therapist Oklahoma
47447	Administrative Support
52756	Supply Chain and Cost Manager
116793	Manager Business Operations
148079	Warehouse Associate
84055	3rd Shift Sanitation Tech
173722	Accounting Intern Summer 2027
21180	Accounting Clerk Remote
81088	Sales Engineer Healthcare Interoperability AI Solutions
83170	Vendeur CDD temps plein
148619	Sales Manager Enterprise
37201	Website Onboarding Specialist
170371	Software Developer Co op January to August 2027
167099	Manager Veeam Cloud Service Provider Sales ANZ
5278	Culture Engagement and Organizational Performance Consultant
38559	Wholesale Merchandising Operations Coordinator
119775	Events Coordinator
80813	Manager Relationship Managers MidMarket
160662	Associate Client Services French Speaker
121540	AI Trainer Advanced Korean Fluency Seoul Remote
162903	Supply Chain Finance Analyst
76938	CCO at HRTechX
176433	Growth Marketing Manager
108637	Affiliate Business Development Manager
36251	DevOps Engineer
118230	Shift Supervisor
137205	Manager Product Safety and Conformity
74531	Program Associate at Sonoma Hillel
127115	Band 7 Paediatric Physiotherapist London
82856	Sales Assistant JD Girona
87422	Studio Support Div Korean Localization Specialist 1년 이상 계약직
137726	Harnessing Technician
40747	Experimentation Analyst
83190	Vendeur CDD temps plein
144053	New Graduate Engineer Software Security 26 27 Starlink
20804	Product Optimisation Specialist Atamis
134452	Ad Operations Associate
145287	Supplier Development Engineer PCB Starlink
2879	Sandbox and Platform Services Product Manager
47574	Commercial Fire Door Inspector
4255	German Language Specialist Freelance AI Trainer Project
27477	Psychotherapist
147505	Executivo a Comercial Hunter para Grandes Contas Caldas Novas GO
110427	Enterprise Application Support Specialist
9975	Strategic Alliances Director France
49318	Service Operations Manager
85026	Contracted In Home Occupational Therapist
116955	Managing Director Sales FinServ
116108	Supervisor part time Original Penguin
166416	Commissioning Qualification Lead
49803	Account Manager Platform Innovation
71844	Technical Recruiter Contract to Hire
77886	RPA UiPath Developer
15056	Field Quality Manager
94660	Seasonal Sales Ambassador Garden State Plaza
21885	Home Care Nurse LPN RN
47191	UI Designer
77296	Technology Lead
96092	Director HRBP
69341	Consultant Product Management
159817	Assistant Community Manager
81483	Associate General Counsel Transactions
168980	Emergency Veterinary Assistant Shift Lead Chantilly VA
116301	Start Up Regulatory Specialist
167057	Customer Success Renewals Representative with German
27887	Credentialed Veterinary Technician Chicago Wicker Park
142614	Optimization Manager
21954	Home Health Aide HHA Elderly Care
133651	Technical Director AI Roblox Studio
137767	DFIR Analyst
106644	Anesthesiologist Winter Park
139499	Territory Representative
165367	Développeur euse en automatisation AQ Plateforme
9971	FP&A Manager Customer Success
163099	UI Designer
146290	Patient Navigator
168017	Processing Specialist I
26	Red Teaming Fellowship
64097	Visual Observer
28723	Director Creative
121713	AI Trainer Fluent Kazakh Speaker Canada
95692	3rd Shift unarmed
60284	Key Account Manager Hobby Retail
65640	Product Manager In Person Giving
74192	1st Shift Warehouse Associate
21919	Home Health Aide
36828	Commissioning Manager
136236	Team Member TEMPLE UNIVERSITY
163995	Legal Assistant
104609	Manager ML Solutions Architecture Token Factory
167870	Sales Strategy and Operations Manager
49537	Don t see what you re looking for Technology
86646	Enterprise Account Executive
77610	Platform Engineer
75180	Production Machine Operator Day Shift Days Off Saturday Sunday
95410	Head of Hong Kong Compliance
72436	ADHD Assessor
24666	Licensed Independent Clinical Social Worker Remote
9488	Laneige Social Media Manager
125902	Bacteriology BMS Wexham Park Hospital
39305	Conga General Interest
150800	Personal Trainer
107214	Electrical Project Manager Glomfjord Norway
78721	Graduate Hardware Engineer
77569	Data Collection Contributor
150752	Personal Trainer
72959	Production Shift Manager all genders
109294	French Canada Apparel Brand Linguist Clothing Brand
77118	Head of Event Operations
152762	Personal Trainer
150316	Personal Trainer
158227	Lead Security Engineer
157945	Director Head of Program Management
38114	Associate Facility Administrator
73873	Urgent Hiring In Home Support Needed Mesquite TX 75150
6804	SLP OT Music Therapy Clinical Supervisor
115573	Generator Technician
155192	Personal Trainer Yucaipa CA
172795	Babysitting Opportunities in New York NY
4639	Science Specialist Fluent in Indonesian Freelance AI Trainer Project
1235	Director Clinical Development
114285	Lead Engineer Integration and Test
166961	Associate Scientist Contract Formulation Development
89877	Software Engineer
82724	Sales Assistant Part Time Fiumicino Parco Commerciale Da Vinci
47276	Utility Data Analyst
14799	Staff Manufacturing Design Engineer Structures
103671	Account Executive
110350	Health Physician
168402	Emergency Credentialed Veterinary Technician Part Time Midlothian VA
109682	Mechanical Engineer HVAC Design
136312	Estimator
164006	Receptionist
161303	Restaurant Accounting Lead
159014	Software Engineer Immersive and Interactive Media
97940	M365 AI Infrastructure Engineer
156318	Client Services Manager Growth
65576	Continuous Improvement Specialist
172643	Driver
143578	Hardware Reliability Technician Root Cause Starlink
61381	Business Growth Analyst Madrid based
56085	Operations Manager
162270	Network Administrator I CCNA Cleared Huntsville AL
55555	Kids Club Associate
55391	Group Fitness Instructor
56680	Audio Visual Commissioning Engineer
60105	Pediatric Occupational Therapist 500 Bonus EI Preschool
166889	SMT Test Technician
128883	Manager Research Analytics
88626	Key Account Manager Canada Wholesale
20660	Merchant Trees
33523	Behavior Technician
131785	Global Payroll Implementation Country Lead Germany
52254	Director of Growth Giving
109676	Mechanical Engineer Data Centers
8467	Director of Product Search and Curated Experiences
152505	Personal Trainer
104552	Forward Deployed Engineer Physical AI
27270	Psychiatrist MD
157322	Story Desk Editor
111313	ABA Paraprofessional RBT Forest Hills NY
34984	Micro Lab Tech
130380	Dark Web Collection Analyst
153633	Personal Trainer
170019	Account Director
31770	Técnico de Segurança do Trabalho Híbrido 3x Rio de Janeiro RJ
177824	QA Automation Engineer
141642	Food Drink Runner Soho House Amsterdam
57223	Field Technician Mechanic Pump Power HVAC
138086	Field Application Engineer Battery Energy Storage Systems
36085	Flex Schedules Great Pay Even Better Culture LPN RN
44272	Retail Sales Associate Part Time
26185	Director Federal Affairs
137094	Training Specialist
176076	Search Specialist
176102	Associate Media
3766	Software Engineer Backend Merchant Partner Lifecycle
125298	Robotics Support Engineer
86706	UAS Operator Maintainer
64661	Campus Recruiting Intern Fall 2026
58797	Strategic Account Executive
103563	Area Sales Director
66667	VP Sales Brands Direct
24164	Fund Migrations Associate Philippines
7102	VP Program Management Business Operations
172234	Control Engineer
134975	Sales Engineer Netherlands
60590	Software Engineering Manager Financial SCM Systems
172501	Substance Use Disorder Professional SUDP North Seattle
17836	Special Attendant Adult Travel Care Partner Community Care Tyler Tx
86060	Community Coordinator Work Management
123473	Germany Residents Survey Participants Essen Germany
47525	Commercial Door Technician
11176	Airworthiness Engineer Advanced Effects
141718	Line Cook II Dumbo House
131502	Linux Administrator
132495	Private Equity Insights Paris Volunteer Programme
143616	Industrial Security Analyst PERSEC
9703	Site Reliability Engineer III
14684	Corporate Development Director
172709	Primary Care Physician Pod Leader
49277	Delivery Manager
132255	Manager Pediatric Clinical Program
152983	Personal Trainer
96568	Manager Cloud Services Infrastructure
96310	Media Relations Strategist Media
6943	Imagery Analysis and Sensors Imagery Analysis Subject Matter Expert Mid 849
11740	Software Engineer Backend SIG
48607	Technical Product Marketing Manager
109721	Student Internship Construction Materials Testing
65676	Payroll Equity Specialist
145605	Endodontist Opening
76272	Real Estate Acquisition Consultant
172004	Program Manager Risk Insurance
16032	Data Engineer Mexico
152712	Personal Trainer
86062	Customer Success Lead
103804	Chef Operator General Manager
168797	Emergency Veterinarian St Peters MO
11944	Technical Recruiter Manufacturing Operations Contract
85488	Software Engineer Applied AI Netherlands
43997	Software Engineer Pleno
24905	GTM Strategy Analytics Manager
94817	Patient Care Coordinator
147385	Agente Stone Executivo a de Contas Externo 6 horas Jaraguá SP
76315	Real Estate Acquisition Consultant
110398	Virtual Seasonal Family Nurse Practitioner NY Licensed
146019	DevOps Engineer
43022	Maintenance Technician 1st Shift
39908	Area Director of Operations Live Events
100463	Staff Mechanical Design Engineer
148441	Internal Audit Regulatory Lead EMEA
74148	Future Opportunity Corporate
57508	Heavy Equipment Shop Technician Mechanic
113648	Aide aux personnes âgées
29206	Advanced Practice Provider Geriatrics
40749	Information System Security Officer
2391	Deputy Director National Organizing
146013	Vulnerability Management Engineer
55579	Kids Club Associate
94699	Contracts and Claims Specialist
138517	Infrastructure Engineer
95576	Temporary Manufacturing Associate I Oligo
104659	Security Architect Manager Corporate Cloud Security
123987	Legal Professionals AI Training Long Beach US
113909	Auxiliaire de vie
156301	Banking Transfer Specialist
11627	Product Sourcing Engineer
106702	Staff Accountant
85619	Mission Payload Engineer
147373	Agente Stone Consultor a Comercial Externo ZONA OESTE do Rio de Janeiro RJ
156923	Social Media Content Manager
101986	Contract Auditor
5979	Recruiting and HR Coordinator
91386	Social Worker Child Protection Court North West London
18931	Partner Account Manager
83388	Vendeur CDI temps partiel
138644	Product Intern
112579	Procurement Operations Manager
76754	Implementation Manager
135388	Project Safety Coordinator OSHA 500 TX
37589	Systems Engineer Network Automation
46093	Anaplan Systems Architect
4972	Engenheiro de Dados Sênior Campinas SP
61102	Customer Success Manager
50249	UAS Flight Test Operator I&II DoorDash Air
173323	Veterinarian Foothill Farms Veterinary Hospital
99646	Operations Lead
30970	Partner Sales Manager Global System Integrator GSI
113298	Certified Nursing Assistant CNA STNA
13483	Civil Engineers Project Managers
135924	Community Engagement Manager
85023	Contracted In Home Occupational Therapist
135770	Specialist Seller Connected Maintenance Public Sector
125087	Survey Participants Pregnancy Care Research Seattle US
139181	2nd Shift HUB Inbound Lead Fulfillment Associate
66299	Head of Consumer and Lending Compliance
62995	アブストラクター 契約社員
34162	Early Intervention BCBA Life Skills Autism Academy Center Based
129775	Training Communications Specialist Active Secret Clearance
22122	In Person Certified Home Health Aide Class
43414	Regional Sales Manager Minneapolis Enterprise
29769	Community Editor Strategist Project Based
40015	Lead Family Nurse Practitioner FNP Urgent Care
61033	FC Cincinnati Talent Pool
27942	Veterinarian Philadelphia Rittenhouse
151115	Personal Trainer
20315	Medical Science Liaison CNS Neuro Radio Northeast
110701	Customer Success Manager
81107	Security Engineer
78036	Investment Product Specialist Model Portfolios Assistant Vice President Vice President
102863	Communications Content Partner
144409	Signal Integrity Engineer Serdes Satellites Starlink
18101	Regional Sales Manager Hybrid Therapies
142146	Sonder Responder Expression of Interest
30488	National Account Manager Albertsons
41929	Public Relations Specialist Global Communications
7355	Operations Associate Part Time Santa Monica
20032	Technical Support Engineer Axon 911
51777	PT Registered Nurse Pre Admission Testing Strong phone skills needed no direct patient contact
56639	Wellness Recovery Specialist
20902	Pre Award CPE ST3 Agile Program Manager Orlando FL
158195	Nurse Manager
173688	Backend Engineer Marketplace
47630	Entry Automatics Door Service Technician Experienced or Trainee Dalton
123377	Game Development Environment Artist Florida US Freelance Remote
130084	Project Engineer
96350	Designer II Landscape Architecture
164948	Global Internship Program Scandinavian speaker United Media
114864	Deal Desk Manager
39190	Full Time Concierge Mon Fri 3pm 11pm JW Residences
146309	Clinical Research Scientist II
171372	Inspection Manager
121689	AI Trainer Fluent Azerbaijani Speakers Germany
121046	Associate Research Director Complex Care
153990	Personal Trainer
97047	Primary Care Nurse Practitioner Physician Assistant
127096	Band 7 Nuclear Medicine Tech DEXA Radiographer London
80029	Lead Executive Assistant
138250	Events Associate
54294	Assistant General Manager
58801	Sales Development Representative
160136	Head Chef New opening
47739	Outside Residential Sales Representative Selling to Homeowners and Builders
4257	German Translator Freelance AI Trainer Project
101666	Pricing Representative Temp to Hire
94513	Seasonal Ambassador Fashion Centre
171622	SC Security Officer
82516	Quantitative Researcher
5809	Staff Advanced Analytics Community Blueprint Quality
17976	Delivery Lead Consultant
109764	Business Manager Chevy Chase
46200	CPC Processor I Customer Support
103329	Account Executive St Louis Remote
54597	Certified Personal Trainer
7548	Sales Associate Part Time Bellevue Square
164985	Head of marketing at United Media
12527	Sales Advisor Chelsea
166481	Manhattan OMS Lead Developer
46379	Licensed Therapist LCSW LPCC LMFT 75 95 Session Work with Youth and Adults
101838	Operations Strategy Commercial Lead
122908	Data Entry Clerk Graduates AI Training Durango Mexico
37796	Vehicle Shuttler
165331	Warehouse Associate
83623	Store Colleague JD Rotterdam Zuidplein 16H
58648	Designer London
106815	Debt Equity Producer
15612	Rust Engineer
62646	ABA Training Manager ABA Experience Required
27048	Psychiatric Mental Health Nurse Practitioner PMHNP
154913	Personal Trainer Loxahatchee FL
33960	Center Based Behavior Technician
92338	Area Manager Water Solutions
6412	FP&A Analyst
161760	Dishwasher
67554	Intellectual Property Paralegal
167554	DDI Engineer
94732	Document Control Specialist
71495	Partner Success Supervisor
62802	WEM Application Consultant
170330	Options Quantitative Strategist
124400	Pathologists Freelance Remote Denver US
17984	Customer Success Manager Bangalore India
80636	GROWTH ANALYST I User Acquisition SEO
135848	Don t see internships you are looking for
163124	Cloud Security Engineer
124432	Pathologists Freelance Remote Raleigh US
122113	AI Training Experts Wisconsin US
140273	Enterprise Account Executive Seattle USA
162235	Account Executive
155097	Personal Trainer Spotsylvania VA
27571	Psychotherapist
74293	ServiceNow Software Engineer CSM Custom Application
49607	Growth Marketing Manager
113169	Director Network Contracting
132405	Events Producer
164318	Wireless Switching Test Engineer
57440	Heavy Equipment Field Technician Mechanic
852	Experience Practice Talent Network Application
32969	Trade Support Analyst
174400	Sales Manager
130361	Account Director Indonesia
88063	Environmental Engineer Scientist or Geologist Entry Level
102628	Virtual EL Teacher
54205	Assistant General Manager
65962	Product Designer Mobile
16595	Lead Logistics
61454	Event Manager Freelance
42268	Assistant Manager Pricing Operation Management 電商價格審核專案
8281	Field Sales Poland Teleroute Remote
94322	Womens Health Practice Specialist
96752	Endocrinology Nurse Practitioner Physician Assistant
90868	Practitioner Front Door Service
49369	Engineering Manager Site Reliability Observability
95053	Fullstack Developer India
129043	DevOps Engineer
114867	Enterprise Engagement Manager
15904	Head of People North America
154997	Personal Trainer Pensacola Pensacola FL
7858	Seasonal Operations Associate Part Time Prudential Center
59728	Staff Cell Stacking Equipment Engineer
125423	Account Executive
115117	Associate Outside Sales Executive Healthcare
156938	Assistant Store Manager Tecovas Domain Austin TX
154449	Personal Trainer
141180	Temporary Seasonal Retail Sales Associate Maje Meatpacking
75141	Production Lead 2nd Shift Days Off Mondays Tuesdays
52434	Overnight Awake Counselor Developmental Disabilities Services
72212	Design Director
168818	Emergency Veterinary Assistant Brookhaven GA
165178	Production Design Specialist
34462	Registered Behavior Technician RBT
165620	Enterprise Account Executive
37601	Territory Account Executive Qatar
154530	Personal Trainer
105570	Neurosurgical Robot Operator Part Time
59573	AI Builder Property Management
69555	Director Brand Creative Operations
23337	Registered Nurse RN Weekend BAYLOR
50070	Manager Protective Services
142761	Head of Private Sales Watches
88635	Key Holder
60491	Retail Associate University of Oklahoma Team Store
161139	Fiber Sales Representative W2 base Commission
146436	Sales assistant Sport Zone Cancela
65578	Especialista de Impresión
62046	Account Executive Mid Market
70391	Meta Ads Specialist
50049	Associate Strategic Growth Initiatives GTM
28979	Outbound Sales Development Representative Pipeline
51570	Medical Scribe Full benefits no weekends paid holidays
151769	Personal Trainer
99361	Territory Manager Humble TX
169282	Veterinary Technician Student Externship Federal Way WA
5906	Strategic Partner Account Manager DACH
10018	Entry Level Project Manager Construction
70028	Leader in Training
141169	Retail Sales Associate Maje Royalmount Montreal
5222	Analyst Analyst Associate Manager Corporate Strategy Bangkok Based
131285	Integration Technician II Stage Integration Second Shift
12917	Product Management Research
121589	AI Trainer Advanced Tamil Fluency Freelance Edinburgh
45730	Enterprise Sales Executive
28479	Progression Operator
108804	Agent Customer Service French Speaker
71955	Content Engineer Developer Relations
94302	Surgical Technologist Regional Supplemental Staffing I
10673	Maintenance Lead
93107	Adv Crema
103301	Head of Content
86616	Territory Manager Austin Central
80431	Machine Learning Engineer
93806	Content Creator Growth LATAM
76689	Transaction Coordinator
20341	Territory Manager GI Knoxville Nashville
158873	Pediatrician
72546	Territory Account Manager Nashville Tennessee
6944	Imagery Analyst Mid Job#471
127520	Consultant in Old Age Psychiatry London
98711	Health Safety and Environmental Coordinator Michels Energy Group Inc
24330	Industrial Engineer Manufacturing
51798	Registered Nurse RN Pre Operative PACU OR Full Time 4x10s Monday Thursday with one Friday per month
134682	Staff Software Engineer Demand Bidder Ad Serving Platform
148351	Enterprise Account Executive Hunter
69086	Stylist Fillmore Street
73931	Hiring Caregivers for Seniors in Haymarket Virginia 20169
140606	Physician Nashville TN
18841	CStack Security Engineer GovTech
71020	Assistant Teacher
172860	Part Time After School Nanny Philadelphia PA
66150	IT Support Engineer
152297	Personal Trainer
176486	Studio Coach Norman OK
55767	Member Experience Manager
128035	Paediatric Mainstream Speech and Language Therapist
32609	Product Graphic Design Intern
122241	AI Training Sales Dallas US
70678	Software Developer Systems Software
130744	Cathode Design Engineer
111117	ABA Behavioral Technician RBT Bronx
174151	Register Your Interest Business Management
93143	Partner Commerciale Savignano sul Rubicone
113061	Director Accounting
60532	Retail Supervisor
168165	software Engineer contract
15415	Project Manager Construction
173167	Experienced Veterinary Technician
78385	SOC Lead
56747	Data Centre Technicians Expression of Interest
115756	PRN Registered Nurse Arkansas
155908	Lead ServiceNow ITOM
632	Software Engineer III Full Stack Vue .NET
96046	Licensed Practical Nurse LPN
159821	Assistant Community Manager
163720	Bilingual Member Services Representative English Spanish
139669	Engagement Manager Life Sciences
65374	Bioinformatics Research Engineer
72530	Territory Account Manager Bakersfield CA
74320	Engineering Manager Destinations
151210	Personal Trainer
51615	Ophthalmic Photographer Part Time
13094	Staff+ Software Engineer Cybersecurity Products
101506	Copywriter TEMP
8686	Summer Associate Client Service 2027 Campus Resume Drop
103496	Account Executive
15312	Assistant Superintendent
14535	Design Engineer Combustion Devices
50064	Manager DashPass Strategy Operations Benefits and Merchant GTM
18288	Civil Engineer Land Development
114994	Photographer Videographer
158950	Associate Director of Accessibility
142873	Branch Manager
29476	Account Manager
81994	Clinical Research Coordinator
63818	Electrical Engineer Avionics
94598	Seasonal Ambassador Ross Park
12825	Head of Policy Design Societal Harms
143709	Sous Chef Temporary
139198	Lead Fulfillment Associate
82237	Account Executive Developed Markets Asia Pacific Rosetta Stone
106064	Summer 2027 Software Engineer Intern Game Development Creative Party
101779	Game Night Staff Event Engineer Part Time Seasonal
102241	Legal Assistant
32453	Account Executive Legal Services
130155	Account Associate Medical
10212	Chief Engineer Tactical Recon and Strike
120873	Management Graduate Program Intake German speaker Private Equity Insights
48792	Software Engineer Ruby on Rails
155720	Billing Engineer
75602	Insurance Agent Fort Wayne IN
61443	Event Manager Balikpapan
43652	IT Engineer SaaS Applications
76099	Home Buying Specialist
118684	Market Data Operations
86894	Category Analyst Kroger
158429	Veterinary Academic and Key Relations Manager
35856	Staff Data Scientist
69275	Production Supervisor Cell Line
63199	Accounting Analyst
59814	Talent Brand Manager
96027	Building Engineer
52567	Dental Hygienist
23786	Accountant
105956	Meewerkstage Aangiftepraktijk
172757	Client Engagement Manager
112595	Quantitative Trading Internship Taiwan 2027
157118	ENG Video Editor Photographer
17672	Royalties Manager
103417	Head of Product Core Banking Systems
121178	Associate Director Telecommunication West
42253	產品價格專案專員 Associate Pricing Operation
143956	Mechanical Engineer Satellite Payload Starlink
83982	Marketing Lifecycle Manager
96923	Optometrist
41529	Commercial Partnerships Operations Director
144078	Operations Engineer Production
154818	Personal Trainer Gulfport FL
32586	Assistant Technology Solution Manager Shenzhen
92031	Product Delivery Specialist
65988	Director of Experiential Production
124943	Spanish Fluent Speakers AI Training A Coruña
146849	Director eCommerce
104873	Enterprise Account Executive Financial Services
60602	Warehouse Operations Supervisor
107686	Co Op IT Windows Systems Administration
54892	Fitness Counselor
175006	Operations Manager
88173	Land Management Professional
95373	Associate Product Design Engineer
173837	Animator
64753	Operation Program Manager
106545	Intern Operations Analytics Implementations
74553	Keyholder Boston
127221	Biomedical Scientist in Haematology Yorkshire
40847	Data Center Technician Quincy WA
10105	2027 Flight Software Engineer Intern
107011	Head of People India
1212	General Audio Fiction Application
132180	Environmental Equipment Field Operator Ecological Restoration
77384	Chief Revenue Officer Bay Area
147628	Vendedor a Externo 6 horas Montes Claros MG
21573	Client Services Manager
```

## Predictions — do not read until step 3

What iteration 7's classifier answered for each of the rows above. Reading this before the labels
are written to disk destroys the measurement: the labeller would agree with it and the numbers
would decorate rather than measure.

```
26	UNKNOWN
406	UNKNOWN
632	IN
852	OUT
1212	UNKNOWN
1235	OUT
1582	IN
2163	UNKNOWN
2270	IN
2277	UNKNOWN
2391	UNKNOWN
2879	IN
3104	UNKNOWN
3663	IN
3766	IN
4219	OUT
4255	OUT
4257	OUT
4638	OUT
4639	OUT
4705	OUT
4972	UNKNOWN
5205	OUT
5222	UNKNOWN
5278	UNKNOWN
5809	UNKNOWN
5906	OUT
5979	OUT
6177	OUT
6412	UNKNOWN
6804	OUT
6818	OUT
6937	UNKNOWN
6943	UNKNOWN
6944	UNKNOWN
7102	OUT
7115	OUT
7355	OUT
7548	OUT
7851	OUT
7858	OUT
7897	OUT
8281	UNKNOWN
8315	UNKNOWN
8467	UNKNOWN
8476	OUT
8686	OUT
8966	UNKNOWN
9453	OUT
9488	OUT
9619	OUT
9703	IN
9971	OUT
9975	UNKNOWN
10018	OUT
10105	OUT
10212	UNKNOWN
10416	OUT
10575	IN
10673	OUT
10694	OUT
10998	IN
11176	UNKNOWN
11321	UNKNOWN
11627	OUT
11740	IN
11944	OUT
12062	IN
12527	OUT
12825	UNKNOWN
12917	UNKNOWN
13094	IN
13483	OUT
13822	IN
13998	OUT
14535	OUT
14684	UNKNOWN
14799	OUT
15056	UNKNOWN
15312	OUT
15415	OUT
15424	OUT
15612	UNKNOWN
15804	OUT
15904	UNKNOWN
16032	IN
16595	OUT
17345	OUT
17672	UNKNOWN
17836	OUT
17976	UNKNOWN
17984	OUT
18101	OUT
18286	OUT
18288	OUT
18552	UNKNOWN
18841	IN
18931	OUT
19391	OUT
19644	OUT
20032	UNKNOWN
20315	UNKNOWN
20341	OUT
20660	OUT
20804	UNKNOWN
20868	UNKNOWN
20902	UNKNOWN
20917	UNKNOWN
21089	UNKNOWN
21110	UNKNOWN
21180	OUT
21504	OUT
21573	OUT
21885	OUT
21919	OUT
21954	OUT
21977	OUT
22004	OUT
22122	OUT
22501	OUT
22642	OUT
23265	OUT
23337	OUT
23540	OUT
23786	OUT
23915	UNKNOWN
24164	UNKNOWN
24330	OUT
24666	OUT
24854	UNKNOWN
24905	UNKNOWN
25121	UNKNOWN
25233	UNKNOWN
25353	UNKNOWN
25581	OUT
26185	UNKNOWN
26333	IN
27048	OUT
27270	OUT
27477	OUT
27571	OUT
27887	OUT
27897	OUT
27942	OUT
28003	UNKNOWN
28013	OUT
28479	OUT
28508	OUT
28723	UNKNOWN
28979	OUT
29206	OUT
29211	OUT
29228	UNKNOWN
29476	OUT
29708	UNKNOWN
29727	OUT
29769	OUT
30268	UNKNOWN
30488	OUT
30573	OUT
30677	OUT
30970	OUT
31351	UNKNOWN
31477	UNKNOWN
31610	UNKNOWN
31770	UNKNOWN
32453	OUT
32570	UNKNOWN
32586	OUT
32609	OUT
32969	UNKNOWN
32978	OUT
33122	IN
33523	OUT
33686	OUT
33960	OUT
34162	UNKNOWN
34382	OUT
34462	OUT
34613	OUT
34723	OUT
34984	UNKNOWN
35099	UNKNOWN
35170	OUT
35462	OUT
35473	UNKNOWN
35856	IN
35887	OUT
36085	UNKNOWN
36251	IN
36828	OUT
37201	UNKNOWN
37464	OUT
37578	UNKNOWN
37589	IN
37601	OUT
37796	UNKNOWN
38114	UNKNOWN
38355	UNKNOWN
38548	OUT
38559	OUT
38843	UNKNOWN
38911	OUT
39190	OUT
39305	UNKNOWN
39354	OUT
39623	OUT
39758	OUT
39881	IN
39908	OUT
40015	OUT
40250	OUT
40747	UNKNOWN
40749	UNKNOWN
40838	OUT
40847	OUT
41529	OUT
41763	OUT
41784	OUT
41843	IN
41929	OUT
42253	UNKNOWN
42256	UNKNOWN
42268	OUT
43022	OUT
43414	OUT
43605	OUT
43652	IN
43997	IN
44272	OUT
44322	OUT
44399	OUT
44517	OUT
44746	UNKNOWN
44795	UNKNOWN
45034	IN
45262	IN
45730	OUT
45786	OUT
46058	OUT
46093	IN
46200	UNKNOWN
46271	UNKNOWN
46328	IN
46379	OUT
46862	OUT
47191	OUT
47276	UNKNOWN
47447	UNKNOWN
47525	OUT
47574	OUT
47630	OUT
47697	UNKNOWN
47739	OUT
48607	OUT
48717	OUT
48792	IN
49277	UNKNOWN
49296	UNKNOWN
49318	OUT
49369	IN
49537	UNKNOWN
49588	OUT
49607	OUT
49803	OUT
50049	UNKNOWN
50064	OUT
50070	UNKNOWN
50209	UNKNOWN
50249	OUT
50339	IN
51538	OUT
51564	OUT
51570	UNKNOWN
51615	OUT
51678	OUT
51777	OUT
51798	OUT
52029	OUT
52131	OUT
52254	UNKNOWN
52434	OUT
52567	OUT
52570	OUT
52756	OUT
53175	UNKNOWN
53233	OUT
53874	OUT
54149	OUT
54205	OUT
54294	OUT
54362	OUT
54504	OUT
54597	OUT
54647	OUT
54731	OUT
54892	OUT
55148	OUT
55359	OUT
55391	OUT
55480	OUT
55555	OUT
55579	OUT
55767	UNKNOWN
56085	OUT
56560	UNKNOWN
56639	OUT
56680	OUT
56747	OUT
56800	UNKNOWN
57189	OUT
57223	OUT
57440	OUT
57508	OUT
57550	OUT
57660	OUT
58330	OUT
58648	OUT
58797	OUT
58801	OUT
58846	OUT
59076	OUT
59573	UNKNOWN
59681	UNKNOWN
59728	OUT
59814	OUT
60105	OUT
60284	OUT
60491	OUT
60532	OUT
60590	OUT
60602	OUT
61033	OUT
61102	OUT
61381	UNKNOWN
61443	UNKNOWN
61454	UNKNOWN
61591	OUT
62046	OUT
62646	UNKNOWN
62802	IN
62995	UNKNOWN
63132	OUT
63199	UNKNOWN
63385	IN
63458	UNKNOWN
63818	OUT
63917	OUT
64016	IN
64031	OUT
64097	UNKNOWN
64131	OUT
64302	UNKNOWN
64477	UNKNOWN
64661	OUT
64732	OUT
64753	UNKNOWN
64976	IN
65374	UNKNOWN
65384	IN
65576	UNKNOWN
65578	UNKNOWN
65640	UNKNOWN
65648	OUT
65676	OUT
65858	OUT
65962	OUT
65988	UNKNOWN
66003	IN
66150	UNKNOWN
66299	OUT
66667	OUT
66919	OUT
67390	UNKNOWN
67554	OUT
68552	OUT
68731	OUT
69086	OUT
69275	OUT
69301	OUT
69341	UNKNOWN
69555	OUT
69652	UNKNOWN
69661	IN
70028	UNKNOWN
70201	UNKNOWN
70317	UNKNOWN
70391	UNKNOWN
70678	IN
70997	OUT
71020	OUT
71495	UNKNOWN
71503	IN
71605	UNKNOWN
71844	OUT
71955	UNKNOWN
72116	OUT
72212	UNKNOWN
72245	OUT
72365	OUT
72436	UNKNOWN
72530	OUT
72546	OUT
72636	OUT
72959	UNKNOWN
73346	OUT
73873	UNKNOWN
73931	OUT
73962	OUT
74148	OUT
74192	OUT
74293	IN
74320	UNKNOWN
74444	OUT
74531	UNKNOWN
74553	UNKNOWN
74865	OUT
75141	UNKNOWN
75180	OUT
75602	OUT
76099	UNKNOWN
76172	UNKNOWN
76272	OUT
76315	OUT
76422	OUT
76689	OUT
76698	UNKNOWN
76754	UNKNOWN
76938	UNKNOWN
77118	UNKNOWN
77296	IN
77384	OUT
77569	UNKNOWN
77597	UNKNOWN
77610	IN
77886	UNKNOWN
77932	UNKNOWN
78036	OUT
78156	UNKNOWN
78172	OUT
78385	UNKNOWN
78721	OUT
78839	IN
79528	OUT
79641	UNKNOWN
79986	OUT
80029	UNKNOWN
80050	IN
80431	IN
80636	UNKNOWN
80747	OUT
80813	OUT
81088	UNKNOWN
81107	IN
81483	UNKNOWN
81623	OUT
81719	UNKNOWN
81994	OUT
82237	OUT
82516	IN
82704	OUT
82722	OUT
82724	OUT
82856	OUT
83170	OUT
83190	OUT
83388	OUT
83623	UNKNOWN
83955	OUT
83982	OUT
84055	UNKNOWN
84783	UNKNOWN
84944	UNKNOWN
85023	OUT
85026	OUT
85258	UNKNOWN
85327	UNKNOWN
85467	IN
85488	IN
85619	UNKNOWN
86060	OUT
86062	OUT
86098	UNKNOWN
86473	UNKNOWN
86491	UNKNOWN
86550	OUT
86616	OUT
86646	OUT
86706	OUT
86769	OUT
86894	UNKNOWN
86991	UNKNOWN
87281	OUT
87422	UNKNOWN
88063	OUT
88173	OUT
88626	OUT
88635	UNKNOWN
88804	OUT
88984	OUT
89200	UNKNOWN
89231	OUT
89877	IN
90039	UNKNOWN
90231	OUT
90392	OUT
90488	UNKNOWN
90674	OUT
90745	OUT
90802	UNKNOWN
90868	OUT
91014	OUT
91386	OUT
91630	OUT
91667	OUT
92031	UNKNOWN
92240	UNKNOWN
92338	OUT
92499	OUT
92602	OUT
93107	UNKNOWN
93143	UNKNOWN
93482	OUT
93806	UNKNOWN
94031	OUT
94302	OUT
94322	UNKNOWN
94330	OUT
94338	UNKNOWN
94513	OUT
94545	OUT
94598	OUT
94660	OUT
94687	OUT
94699	OUT
94732	UNKNOWN
94817	OUT
95053	IN
95373	UNKNOWN
95410	OUT
95576	OUT
95692	UNKNOWN
96027	UNKNOWN
96046	OUT
96092	UNKNOWN
96107	UNKNOWN
96272	OUT
96310	UNKNOWN
96350	OUT
96568	IN
96699	OUT
96752	OUT
96923	OUT
96977	OUT
97047	OUT
97530	OUT
97899	UNKNOWN
97940	IN
98252	IN
98711	OUT
99361	OUT
99646	UNKNOWN
100463	OUT
101126	OUT
101128	OUT
101506	OUT
101594	OUT
101666	OUT
101733	OUT
101779	UNKNOWN
101838	OUT
101902	OUT
101914	OUT
101986	OUT
101995	UNKNOWN
102241	OUT
102628	OUT
102863	UNKNOWN
103156	UNKNOWN
103171	OUT
103301	UNKNOWN
103329	OUT
103417	OUT
103496	OUT
103563	OUT
103671	OUT
103804	OUT
104461	IN
104552	IN
104609	IN
104659	IN
104686	OUT
104873	OUT
104990	OUT
105138	IN
105143	IN
105536	OUT
105570	OUT
105689	OUT
105747	UNKNOWN
105782	OUT
105900	OUT
105942	OUT
105956	UNKNOWN
106064	IN
106431	OUT
106545	UNKNOWN
106644	OUT
106702	OUT
106815	OUT
107011	UNKNOWN
107214	UNKNOWN
107312	OUT
107473	OUT
107686	UNKNOWN
108250	UNKNOWN
108637	OUT
108804	OUT
109015	OUT
109131	UNKNOWN
109255	OUT
109294	OUT
109359	OUT
109488	OUT
109654	OUT
109676	OUT
109682	OUT
109721	OUT
109764	UNKNOWN
109817	OUT
110350	OUT
110353	OUT
110398	OUT
110427	IN
110701	OUT
110960	OUT
110982	UNKNOWN
111117	OUT
111313	OUT
111853	OUT
111915	IN
112579	OUT
112595	IN
112715	UNKNOWN
113061	OUT
113169	IN
113298	OUT
113648	OUT
113734	OUT
113737	OUT
113909	OUT
114285	IN
114525	OUT
114701	OUT
114864	UNKNOWN
114867	UNKNOWN
114885	UNKNOWN
114994	OUT
115085	IN
115099	UNKNOWN
115117	OUT
115573	OUT
115741	OUT
115756	OUT
115777	OUT
116108	OUT
116301	OUT
116579	OUT
116611	UNKNOWN
116761	IN
116793	OUT
116955	OUT
117105	OUT
117184	OUT
117185	OUT
117430	UNKNOWN
118230	OUT
118684	UNKNOWN
118719	OUT
119072	OUT
119569	OUT
119775	OUT
119957	OUT
120873	UNKNOWN
121046	UNKNOWN
121178	UNKNOWN
121352	UNKNOWN
121540	OUT
121589	OUT
121681	OUT
121689	OUT
121713	OUT
122113	OUT
122241	OUT
122345	UNKNOWN
122908	UNKNOWN
123323	OUT
123377	OUT
123399	OUT
123451	OUT
123473	OUT
123933	OUT
123987	OUT
124113	UNKNOWN
124400	OUT
124432	OUT
124496	OUT
124943	OUT
125087	OUT
125298	UNKNOWN
125423	OUT
125902	UNKNOWN
126280	OUT
126598	OUT
127096	OUT
127115	OUT
127221	UNKNOWN
127520	OUT
127541	UNKNOWN
128035	OUT
128293	UNKNOWN
128629	UNKNOWN
128824	UNKNOWN
128883	UNKNOWN
129043	IN
129109	UNKNOWN
129655	UNKNOWN
129775	UNKNOWN
129863	OUT
130084	UNKNOWN
130155	OUT
130163	UNKNOWN
130287	OUT
130361	OUT
130380	IN
130500	UNKNOWN
130744	UNKNOWN
131010	OUT
131020	UNKNOWN
131053	OUT
131285	OUT
131373	OUT
131435	OUT
131502	IN
131785	OUT
132180	OUT
132255	OUT
132405	OUT
132495	OUT
132601	UNKNOWN
133164	UNKNOWN
133254	OUT
133651	IN
133873	IN
134353	OUT
134452	OUT
134682	IN
134975	UNKNOWN
135152	OUT
135388	OUT
135714	UNKNOWN
135770	OUT
135848	UNKNOWN
135924	OUT
136236	UNKNOWN
136312	OUT
136359	UNKNOWN
136938	OUT
137094	UNKNOWN
137205	UNKNOWN
137726	OUT
137767	IN
138086	OUT
138174	UNKNOWN
138250	OUT
138451	OUT
138517	IN
138644	UNKNOWN
138841	UNKNOWN
139181	UNKNOWN
139198	UNKNOWN
139248	OUT
139444	UNKNOWN
139499	OUT
139669	UNKNOWN
139780	OUT
139972	OUT
140273	OUT
140359	UNKNOWN
140606	OUT
140850	UNKNOWN
141169	OUT
141180	OUT
141431	OUT
141642	UNKNOWN
141699	OUT
141718	OUT
141947	UNKNOWN
141986	OUT
142109	IN
142146	OUT
142614	UNKNOWN
142761	OUT
142873	UNKNOWN
143578	OUT
143616	IN
143709	OUT
143740	OUT
143956	OUT
144053	IN
144078	UNKNOWN
144303	OUT
144409	UNKNOWN
144525	IN
144639	UNKNOWN
145000	OUT
145112	OUT
145287	UNKNOWN
145605	UNKNOWN
145953	OUT
146013	UNKNOWN
146019	IN
146290	OUT
146309	UNKNOWN
146436	OUT
146849	UNKNOWN
147364	OUT
147373	OUT
147385	OUT
147505	OUT
147628	OUT
147845	OUT
148063	OUT
148079	OUT
148351	OUT
148441	OUT
148619	OUT
148785	UNKNOWN
149522	OUT
149596	OUT
150316	OUT
150752	OUT
150800	OUT
150836	OUT
151115	OUT
151210	OUT
151686	OUT
151769	OUT
151850	OUT
151951	OUT
152087	OUT
152297	OUT
152505	OUT
152538	OUT
152712	OUT
152762	OUT
152983	OUT
152992	OUT
153563	OUT
153633	OUT
153990	OUT
153991	OUT
154226	OUT
154449	OUT
154530	OUT
154537	OUT
154818	OUT
154913	OUT
154997	OUT
155097	OUT
155192	OUT
155720	UNKNOWN
155908	IN
155966	OUT
156301	OUT
156318	OUT
156923	OUT
156938	OUT
157118	OUT
157322	OUT
157945	UNKNOWN
158195	OUT
158227	IN
158429	OUT
158873	OUT
158950	UNKNOWN
159014	IN
159296	OUT
159817	OUT
159821	OUT
159910	OUT
160023	OUT
160136	OUT
160662	OUT
160704	UNKNOWN
160896	IN
161139	OUT
161303	OUT
161760	OUT
162235	OUT
162270	IN
162903	UNKNOWN
163082	IN
163099	OUT
163124	IN
163229	IN
163720	OUT
163724	UNKNOWN
163995	OUT
164006	OUT
164094	UNKNOWN
164218	OUT
164318	IN
164413	UNKNOWN
164575	UNKNOWN
164948	UNKNOWN
164985	OUT
165178	UNKNOWN
165331	OUT
165367	UNKNOWN
165433	UNKNOWN
165447	UNKNOWN
165620	OUT
165740	IN
166230	OUT
166351	OUT
166416	OUT
166481	UNKNOWN
166784	OUT
166889	OUT
166961	UNKNOWN
167057	OUT
167099	OUT
167535	OUT
167554	UNKNOWN
167595	UNKNOWN
167660	UNKNOWN
167870	OUT
168017	UNKNOWN
168165	IN
168402	OUT
168511	OUT
168797	OUT
168818	OUT
168831	OUT
168980	OUT
169282	OUT
170019	OUT
170145	OUT
170330	IN
170371	IN
170715	OUT
170796	UNKNOWN
171044	UNKNOWN
171148	OUT
171372	UNKNOWN
171622	OUT
171637	OUT
171674	UNKNOWN
171782	OUT
172004	OUT
172234	UNKNOWN
172501	OUT
172643	OUT
172709	OUT
172734	OUT
172757	OUT
172795	UNKNOWN
172851	UNKNOWN
172860	UNKNOWN
172909	UNKNOWN
173004	UNKNOWN
173007	OUT
173167	OUT
173168	OUT
173323	OUT
173587	IN
173688	IN
173711	OUT
173722	OUT
173837	OUT
173962	OUT
174151	UNKNOWN
174400	OUT
174910	UNKNOWN
175006	OUT
175392	UNKNOWN
175639	IN
175651	OUT
176038	UNKNOWN
176076	UNKNOWN
176102	UNKNOWN
176433	OUT
176486	OUT
176934	UNKNOWN
177083	UNKNOWN
177115	UNKNOWN
177220	IN
177824	IN
177990	UNKNOWN
```
