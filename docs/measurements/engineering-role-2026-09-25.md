# Iteration 5 of the engineering-role classification loop

Run on the development machine on 2026-09-21, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. Criterion revision `33113b2`, unchanged.

The filename carries 2026-09-25 rather than the day it ran, for the reason iteration 3 gave: the
four earlier iterations already hold the four days before it, and the next session finds its input
by taking the newest file.

This iteration labelled the 1000 rows iteration 4 drew, scored iteration 4's classifier against
those labels, and changed the rules. The labels are
`src/test/resources/labels/engineering-role-2026-09-24.tsv`; they were written to disk before any
rule of the classifier was read.

**The labelling was done by a subagent this time**, which is a change in who ran the step and not
in what the step is. The agent was given the criterion, the 1000 bare titles and the order to write
its labels to disk before it was handed the predictions; it was forbidden the classifier's source,
the iteration reports and the earlier label files. Blinding is still ordering, and the ordering was
enforced by giving the agent the predictions only as a separate file it read after it had written
its own. What it bought is context: the session that changed the rules never had to read 1000
titles to do it.

## The numbers

The two rates score **iteration 4's** classifier, because those are the predictions the labelled
sample carries. The unknown share is the corpus **after** this iteration's rule change.

| Gated number | Iteration 4 | Iteration 5 | Gate |
| --- | ---: | ---: | ---: |
| Miss rate — of the `OUT` stratum, labelled `IN` | 1.17% | **1.50%** (9 of 600) | ≤ 2% |
| False accept rate — of the `IN` stratum, labelled `OUT` | 7.00% | **5.00%** (5 of 100) | ≤ 10% |
| Unknown share — of the whole corpus | 41.56% | **31.35%** | fell < 1 pp |

The corpus after the change:

| | Iteration 3 | Iteration 4 | Iteration 5 |
| --- | ---: | ---: | ---: |
| `IN` | 15.27% | 16.47% | **16.92%** (30 304) |
| `OUT` | 36.05% | 41.98% | **51.73%** (92 640) |
| **`UNKNOWN` — the gated share** | 48.68% | 41.56% | **31.35%** (56 154) |
| &nbsp;&nbsp;of which `unruled` | 29.64% | 22.52% | **9.91%** (17 743) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 17.60% | 17.60% | **19.57%** (35 055) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.44% | 1.44% | **1.87%** (3 356) |

This is the first iteration where the other two reasons moved, and they moved the wrong way:
`unruled` fell 12.61 points and gave 1.97 of them back as `domain_ambiguity` and 0.43 as
`scope_ambiguity`, for a net fall of 10.21. Both costs are bought deliberately and are named below
— the leadership heads turn a title no rule reached into a head with no domain, and the new
scope rulings say out loud that a phrase splits. The shape iteration 4 described still holds
directionally: `unruled` is ours and it is the only reason that falls on its own.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 4 called `UNKNOWN`, the
labeller called 10 `IN`, 234 `OUT` and 56 `UNKNOWN`. Split by reason, the 147 of the 176 `unruled`
rows that were really `OUT` are the whole argument for this iteration's change: five iterations in,
the titles no rule reaches are overwhelmingly titles that are not engineering roles. One unknown
title in thirty is an engineering role against one in thirteen last iteration, so the pile got
*poorer* in `IN` as it shrank — the opposite of iteration 4, and the first sign that harvesting
`OUT` out of `UNKNOWN` is reaching the bottom of it.

## The exit gate

Not met. Both rates pass for the third iteration running — the miss rate at 1.50% and the false
accept rate at 5.00% — but the unknown share fell 10.21 points where the gate asks for less than
one, so the two-consecutive count starts again from here on that condition.

## Cost

Whole-word, case-insensitive matching over the cleaned title and nothing else. A full pass over all
179 098 cleaned titles takes **0.685s** in memory, against the criterion's ten-second limit. The
endpoint's 63s wall clock is the database.

## What changed, and why

Every change below is grown from a row where the labels and iteration 4's answer disagreed, or
from the `unruled` rows the sample showed.

**The technician and the designer are never-engineering heads.** This is the largest single change
and the best evidenced: across the 4000 rows four samples have now labelled, `technician` appears
183 times and `designer` 24, and not one of either was ever labelled `IN`. Both were heads that
carried no engineering work, which under step 6 means every title naming one landed `unruled` and
stayed there — 2326 titles in the corpus on `technician` alone. The one reading of a technician a
labeller has ever called open is the IT support desk, and that is five rulings — `it technician`,
`it support technician`, `help desk technician`, `service desk technician`, `noc technician` — each
to `scope_ambiguity`, rather than a reason to leave the whole family undecided. What it costs is
the field service technician, which two labellers called `domain_ambiguity` and which now reads
`OUT`.

**The heads of the people who run the work: `director`, `head`, `president` and `expert`.**
Iterations 2, 3 and 4 each considered `director` and each rejected it by the same argument — a
director is named before the head that would have decided the title, so it masks it, and
`Director Securities Corporate Counsel` would stop being decided by its counsel. Iteration 5
measured the argument instead of restating it. Over the 4000 labelled rows the four heads together
decide **56 more titles `OUT` and 3 more `IN`, cost one false accept and not one new miss**, and
they take `unruled` from 14.55% of the corpus to 9.91%. The masking is real, it is exactly the
shape the earlier iterations predicted, and it is now priced: `Head Chef` and `Director Securities
Corporate Counsel` are `domain_ambiguity` where the chef and the counsel used to decide them, and
there is a test pinning both so that the cost stays visible.

`associate` was tried in the same batch and dropped. It won nine rows and broke
`Associate Veterinarian DVM`, a decision an earlier iteration had already pinned; iteration 4's
four rulings on the shop floor's `associate` remain the right shape for that word.

**Plural heads.** `engineers`, `developers`, `programmers`, `managers`, `specialists`, `leaders`,
`analysts`, `scientists`, `architects`, `consultants` and `directors`, because a posting hiring
more than one person writes the plural and no rule reached it. They are spelled out one by one
rather than read off a trailing `s`: the one plural this corpus needed kept off the list is
`agents`, which iteration 4 excluded on purpose so that `Frontier Agents Engineer Applied AI` would
not read as a talent agent, and a rule that strips the `s` gets exactly that wrong.

**Five more heads whose modifier carries the whole domain** — `intern`, `leader`, `partner`,
`advisor` and `strategist` — from `Product Software Intern`, `Healthcare Architectural Team
Leader`, `Regional Sales Partner`, `PT Sales Advisor` and `Fashion Paid Advertising Strategist`.

**Thirty-nine never-engineering heads** from the `unruled` pile, each a function no software domain
turns into engineering work: `technologist`, `worker`, `professional`, `officer`, `collector`,
`coach`, `porter`, `dishwasher`, `handler`, `sourcer`, `broker`, `clinician`, `physiotherapist`,
`dermatologist`, `oncologist`, `cardiologist`, `aesthetician`, `reporter`, `educator`, `merchant`,
`advocate`, `scheduler`, `employee`, `surveyor`, `helper`, their plurals where the corpus writes
one, and the French, German and Dutch `auxiliaire`, `assistante`, `berater` and `medewerker`.
`officer` needed three rulings to protect the officers who run the information rather than the
building — `chief information officer`, `chief information security officer` and `chief data
officer` to `IN`, joining the `chief technology officer` that was already there — and two more,
`information security officer` and `information system security officer`, to `scope_ambiguity`,
because the labels split on the ISSO.

**The annotation posting that recruits from a software profession is now `scope_ambiguity`.** Three
of this sample's nine misses are one family — `Javascript Developers AI Training`,
`Computer Sciences Graduates AI Training`, `Database Administrator Graduates AI Training` — and it
is the family iteration 4 called `OUT` with the ruling `graduates ai training`. The two labellers
disagree on the same title: iteration 4 read the Lyon posting as the annotation gig it is and
iteration 5 read its Wellington twin as a job a software background alone opens. That is the
criterion's own definition of `scope_ambiguity`, so `graduates ai training` changes verdict and
`developers ai training` joins it. The postings that recruit from a profession no software
background reaches are untouched: `Registered Nurses AI Training` is still `OUT` on the plain
`ai training` ruling.

**A ruling `pre sales` → `scope_ambiguity`**, for the miss `Pre Sales Systems Engineer Higher
Education Mid Atlantic`. The `sales engineer` sitting next to it has been `scope_ambiguity` since
iteration 2 for precisely this reason; the pre-sales spelling was landing on the `sales` marker
instead and deciding `OUT`.

**The `behavior` marker is gone**, replaced by the ruling `behavior analyst` → `OUT`. The marker
was bought in iteration 4 for the Registered Behavior Technician family, and that family is now
decided by the technician head; what the marker still cost was the miss `Behavior Planning
Engineer`, which is autonomy software. Dropping the word does not make that title `IN` — it names
no software domain — but it stops it being decided wrongly.

**Twenty-three new off-domain markers.** From the false accepts: `laser` (`Laser Test Engineer`),
`space systems` (`Guidance Navigation and Control Engineer Space Systems`), `ew`
(`Staff Systems Engineer EW`) and `compliance` (`CPLB Quality Assurance Specialist L4 Compliance
Program Ops`). From the `domain_ambiguity` rows the sample showed to be `OUT`: `brakes`, `steering`,
`fastener`, `mechatronics`, `battery`, `analog`, `architectural`, `advertising`, `fashion`,
`wealth`, `investment`, and the commercial functions `business development`, `renewals`,
`territory`, `purchasing`, `sourcing`, `contracts`, `licensing`, `community`, `customer care`,
`customer experience` and `accounts` — the last of which is only the plural of a marker that was
already there, and which `Head of Accounts Payable` needed.

`space` was tried as a marker and narrowed to `space systems`: the labels hold
`Site Reliability Engineer Space` as `IN`, where `Space` is a product and not a domain. `packaging`
was tried and dropped for the same reason — `Software Engineer Python Linux Packaging` is software
packaging. Both would have bought a miss, which is the expensive kind of error at a 2% gate.

**Six smaller rulings and two qualifiers**, each from a row the sample showed:

- `survey participants` → `OUT`, for the postings that recruit the people a study surveys and
  hire nobody at all. About 700 titles in the corpus.
- `operations manager` → `OUT`. Iteration 4 rejected `operations` as a marker because it would
  decide `Cloud Operations Engineer` wrongly, and said the shape should be a ruling on the exact
  phrase. This is that ruling, and there is a test pinning `cloud operations engineer` to `IN`.
- `product designer` → `OUT`, for the false accept `Lead Product Designer UI Design Systems`,
  where the `lead` is named first and hides the designer.
- `psychiatry` → `OUT`, for the three hospital consultants this sample held. The consultant head
  is domain-free and cannot see a marker, so the speciality has to be named as a ruling — which
  is the cost iteration 4 recorded when it made `consultant` engineering-capable, now partly paid.
- `solutions architecture` and `enterprise architecture` → `IN`, for
  `Head of Enterprise Solutions Architecture Platforms`, which names the architecture where
  another title would name the architect.
- `consultor de vendas` → `OUT`, with the Spanish and Portuguese `consultor`, `consultora`,
  `desenvolvimento` and `desarrollo` heads.
- New software qualifiers `exploit` and `streaming`, from `Exploit Developer US` and
  `Engineering Manager Streaming`.

## What this did not fix

Three misses survive, and all three are the same shape: a modifier that names the industry the
software is sold into rather than the discipline the work is in.

- `Software Engineer Satellite Command and Control` — `satellite` is a marker and step 4 says a
  marker beats a software qualifier. Iteration 3 recorded this trade on `warehouse` and `retail`
  and iteration 4 on `manufacturing`; the reasoning has not changed.
- `Engineering Manager Finance` — `finance` is a commercial marker, and a fintech engineering
  manager is an engineering manager.
- `Marketing Web Developer` — `marketing` beats the `web` qualifier under `developer`.

These are not list errors, and patching them one phrase at a time is what iteration 4 did with
`technical account manager` and `customer success engineer`. See the criterion question below.

The false accept `Specialist QA Document Training` that iteration 4 left open is untouched, and it
stays for the reason iteration 4 gave: the only fix is to drop `specialist` as a head, which would
cost `IT Security Specialist` and `AI Deployment Specialist`.

Rows this change newly gets wrong, as far as the 4000 labelled rows show: one false accept,
`Cheil Agency Network Director of People Operations Analytics`, which the new `director` head
reads through `analytics`. The soft costs are the two masking cases pinned in the tests and the
field service technician described above.

## A question for the criterion, not for the rules

The labeller raised this and three of this iteration's nine misses are it, so it is recorded here
rather than solved in code. **The criterion's step 4 cannot tell a customer vertical from a work
domain.** `Engineering Manager Finance`, `Marketing Web Developer`, `Pre Sales Systems Engineer
Higher Education` and `Software Engineer Satellite Command and Control` all name a noun that is who
the work is *for*, and step 4 reads every one of them as the domain the work is *in*. The rules
have been paying for this one ruling at a time since iteration 3 — `technical account manager`,
`customer success engineer`, `embedded software engineer`, now `pre sales`. A second class of
marker would fix the family, and it would change the procedure the criterion spells out, which is
the developer's document and not the loop's. It is left alone.

The labeller raised three more, none of which changed a rule:

- **Q2 and Q4 disagree on hardware-adjacent roles.** FPGA and ASIC verification writes
  SystemVerilog as its primary artifact, which is Q2, while the credential it hires on is hardware,
  which is not Q4. The labels split the family accordingly and another labeller would reasonably
  put all of it on one side.
- **A title with no head at all has no unknown reason that fits.** `Submit your resume here`,
  `Talent Community`, `APAD II` and their like are `unruled` to the classifier, which is right, but
  a hand labeller is forbidden `unruled` and has to file them under `domain_ambiguity`, which
  inflates that reason in the hand labels.
- **`analyst` is under-specified in practice.** The criterion makes it domain-free, so the modifier
  settles nothing and every `X Analyst` should be `UNKNOWN`. The labeller did not do that — it
  called `Treasury Analyst` and `Management Analyst` `OUT` on judgement while leaving
  `Data Analyst` open. The rules follow the criterion here and leave all of them `UNKNOWN`, which
  is why a handful of this sample's `domain_ambiguity` rows read `OUT` in the labels.

## What the next session should look at

`unruled` is 9.91% and for the first time it is no longer most of the unknown share:
`domain_ambiguity` is now twice its size at 19.57%, and it is the reason the criterion says is the
corpus's and not ours. If the next iteration's `unruled` harvest is as thin as this sample's
`UNKNOWN` stratum suggests, the gate stops being reachable by adding rules and the question becomes
whether `domain_ambiguity` is genuinely the world — which is a criterion question and a grilling
session, not a list edit.

Watch the four new leadership heads. They are the widest change this iteration made, they were
rejected three times before on an argument that turned out to be worth one row in 4000, and the
measurement that overturned it was taken partly on samples earlier iterations had already grown
rules from — so "56 out and 3 in for one false accept" is a count on rows that are no longer
fresh, and not a rate. Iteration 6's sample is the first honest test of it, and one bad row in its
false accepts is enough to want `director` narrowed to `director of <qualifier>` phrases.

`technical`, which iteration 4 flagged as the widest qualifier it had added, cost nothing in this
sample: none of the five false accepts turned on it.

The loose end iteration 1 created is still open: `docs/engineering-role-criterion.md` points at
`docs/engineering-role-seed-lists.md`, which no longer exists. The criterion is the developer's
document and changes only in a grilling session, so the dangling reference is left alone.

## Held out of the draw

The 4000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 171 685 of the
179 098 rows were eligible.

## The sample for iteration 6

1000 rows drawn at random from the predictions above — 100 from `IN`, 600 from `OUT`, 300 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each
row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
98608	Document Controls Engineer Michels Infrastructure Solutions Inc
119675	Internal Medicine Physician Leesburg Clinic Value Based Care
94501	Seasonal Ambassador Crocker Park
77430	Managing Director of Technology Chicago
45292	Industry Marketing Manager Startups
1494	Program Director
52680	Marketing Intern
111949	Legal Counsel Netherlands or U.K
178856	Manager Sales Engineering
169399	Licensed Veterinary Technician
175022	Tour Guide Course Leader
126754	Band 6 Respiratory physiologist Bath
50982	Interested in Working With Us
81991	Research Assistant Wichita KS
66423	Document Control and Records Manager
16757	Engineer I Mechanical Engineering
48759	Product Compliance Requirements Analyst
92727	1664 Manager Strategic Sourcing Corporate Services
119760	Event Operations Coordinator
71820	Product Integration Engineer Mechanical Automotive
134600	Software Engineer Advertising Engineering
154684	Personal Trainer Charlottesville VA
117866	Assistant Store Manager Bilingual Spanish Preferred
1897	Palantir Data Scientist Onsite Vandenberg CA
172932	Call Centre Representative
76359	Real Estate Acquisition Consultant
50243	Support Specialist DoorDash for Business Live Operations
2510	Behavior Technician
137190	Lead Engineer Vehicle SW Package SW Builds
102019	Outside Sales Consultant
150758	Personal Trainer
26612	Associate Director Program Management
120693	Investor Relations Strategy Associate
99636	Vice President Market Access
80774	Events Producer
111439	ABA Therapist RBT Flushing NY
96964	Podiatrist
98586	Construction Technology Specialist Michels Road Stone Inc
40064	Licensed Practical Nurse LPN Urgent Care
160703	Associate Payroll MUM
100629	Associate Recruiter
19793	Embedded Application Software Engineer II
119821	Freelance Session Support Fully Remote
132427	Global Internship Program Scandinavian speaker Retail Insights
110908	Retail Lead Store Advisor Selfridges
56080	Operations Manager
69222	Energy Storage Systems ESS Project Manager
116655	Mechanical Engineering Machinist
153729	Personal Trainer
153956	Personal Trainer
167994	Enterprise Account Executive Vancouver
107932	Mental Health Therapist 1099 California San Francisco Clinic
37116	Director of Customer Support
132609	Beauty Consultant Boots Swords Dublin
86733	Endocrinologist Virtual Full Time and Flexible PRN
122094	AI Training Experts San Jose US
145298	Supply Chain Engineer PCB Starlink
32544	Intern Strategy Account Management
178671	Account Executive Enterprise Mid Atlantic
74715	Carpenter Hourly
20268	Analyst Structured Finance
151044	Personal Trainer
174355	Merchant Compliance Administrator
91814	Social Worker Referral Assessment
54183	Assistant General Manager
95715	Firefighters
117709	Assistant Store Manager
26756	Psychiatric Mental Health Nurse Practitioner PMHNP
133803	Fluidic Component Design Engineer
64970	Graduate Technical Account Management Austin TX
38899	Manager of Young Child
54086	Assistant Fitness Manager
42746	Editor Impact Content Paid Media
80561	PRN Physical Therapist OrthoSouth
117369	Ejecutivo de ventas en campo CDMX Sur
85259	Director Media Fixed Term
60047	CFO Prism Media LLC
175074	Quantitative Developer
159529	Registered Behavior Technician RBT Portage Park
106816	Employment Application
129268	QRT Travel Grant to NeurIPS 2026 Apply Here
87522	Director Marketing Peer to Peer Strategy
112783	Account Executive Tech Lifestyle
177487	Officer Middle São Luís
174232	Field Services Technician San Francsico
132188	Director
101086	Head of Engineering Sales Marketing Tools
27293	Psychiatrist MD
59004	Veterinary Technician Neurology
129736	Marketplace Operations Manager
159125	Producer Vertical Video Opinion Shows
124283	Norway Residents Survey Participants Ålesund Norway
66932	Sports Data Collector Football Ponta Grossa Paraná Brazil
12893	Partnerships Business Operations Manager
9197	Software Architect
1782	GIS Developer
19384	Delivery Manager
48157	Chef in Training SoHo
145527	CLM Engagement Lead
97728	Financial Planner McLean VA
114663	Director Field Engineering
116491	Application Engineer I
141652	Furniture Lighting Product Technologist Soho Home
52537	Dental Assistant
90207	Talent Community at LINK Stay Connected
112966	Associate Manager CG&A and MPCT
124797	Registered Nurses AI Training San Jose USA
78018	Fund Origination Vice President
59704	Engagement Consultant
104538	Field Network Engineer
152271	Personal Trainer
267	Gameplay and Animation Engineer
159857	Leasing Consultant
6130	Urgent Care Emergency Medicine Physician Adult Pediatric Care Per Diem
142153	Sonder Responder Queensland
150514	Personal Trainer
76904	Maintenance Technician II
158431	Tax Manager
99279	Managing Consultant SAP SuccessFactors Employee Central Payroll Werde der fachliche Kopf für das Fundament jeder SF Landschaft
78904	Associate Customer Success Manager
33927	Center Based Behavior Technician
13989	Director Real Estate Facilities
108362	Finance Manager Products Technology
22477	MIL Home Health Aide Training ACSP
41942	Rocket Now Regional Merchant Operations Manager Multiple Openings Across Levels Regions
51318	Software Engineer IoT Platform
137567	Creative Director Copy
44625	DevOps Engineer
42631	South Korea 한국 Refer Your Friend for Future Job Opportunities 쿠팡 인재 네트워크로 추천하기
153052	Personal Trainer
91360	Social Worker Child Protection
42181	Staff Engineer Cloud Backend Engineering
119230	Recruiter Contract Position
177751	Partner Marketing Manager
68528	Field Sales Executive
152848	Personal Trainer
45556	Strategic Core Account Executive Automotive OEM
117587	Director Playfly Max University of Maryland
26264	People Intelligence Architect
102273	Litigation Assistant
107988	Credit Analyst Distressed Debt
127952	Neonatal Intensive Care Nurse NICU Barts
132353	Event Assistant volunteer
165146	Medical Assistant
122580	Cantonese Fluent Speakers AI Training USA
164496	Director Director Strategy
130061	Commercial Account Manager
113303	Certified Nursing Assistant CNA STNA
106477	Software Engineer Infrastructure SRE Security Focused
155884	Business Development Manager
56138	Personal Training Manager
32983	Business Development Manager Samlino Group
71681	AI Designer
29771	Media Operations Specialist Project Based
142924	Mechanism Simulation Engineer
64994	Administrative Clerk Mail Receivables
149929	Project Portfolio Manager Finance
79186	Director Joplin MO
172865	Part Time Babysitter Pearland TX
156406	Tax Resolution Specialist
129596	Manager Global Procurement
100671	Enterprise Account Executive
148102	Flight Termination System Engineer
117448	SRE Antifraud
2581	Registered Behavior Technician
16840	Manager Sales
25457	MLRO Compliance Manager
102040	Part Time Brand Ambassador
101324	Freelancer MX Estrategas
170960	Product Manager Commerce Contract
40676	Client Success Manager
28840	Account Executive DACH
137694	Associate Project Manager Project Manager
128069	Paediatric Occupational Therapist Yorkshire
17503	Environmental Test Technician Supervisor Night Shift
2343	Licensed Insurance Pre Licensing Instructor Contract New York
7506	Sales Associate Full Time Galleria Edina
35534	Forward Deployed Engineer Software
90421	Assistant Team Manager Fostering Kent
83165	Vendeur CDD temps plein
99289	Salesforce Technical Architect Team mit 400 Umsatzwachstum
14870	Assistant Superintendent
84741	Auxiliaire de vie
89644	Field Service Technician Austin TX
130127	AVP VP Client Services RadScience
26172	Channel Sales Support Agent
4948	Consultor de Vendas Linhares ES
47279	Director of Payroll and Retirement Plan Compliance
48947	Additive Manufacturing Technician multiple shifts
79108	People Ops Analyst
3553	Air Vehicle Operator 2 LEA
144707	Electrical Construction Project Engineer
57795	Accountant
53757	Future Employment Opportunities Engineering Product
174246	Assistant Community Manager
176553	Client Account Manager
148626	Sales Manager Enterprise Platforms Hunter Team
10589	Inventory Specialist
121177	Associate Director Strategy Development
160177	Restaurant Manager
80231	LSAT Tutor
49526	Data Engineer Cloud SaaS Integrations
152740	Personal Trainer
122277	Arabic Fluent Speakers AI Training Sweden
152834	Personal Trainer
74355	Sales Development Representative EMEA
43749	Emergency Veterinarian
138025	Strategic Growth Partner
37501	Response Engineer Cloudflare Managed Defense Center CMDC
49900	Engineering Manager People Applications
65783	Sales Account Executive
123558	HR Talent Professionals AI Training Brighton UK
163524	Business Development Marine Corps
146278	Surgical Technician Program Learning Mentor
31568	Consultant Manager Cyber
160630	Software Trainer
133441	Engineering Director Storage
81064	Clinical Informatics Specialist
17163	Director of Business Acceleration
18144	Analytics Consultant Higher Education
45029	Software Engineer Database Engine Internals
57301	General Manager Pump Power HVAC
56394	Service Associate New Gym Opening
104260	Dining Cook
171719	Game Design Director World of Warships PC
115137	Outside Sales Executive Healthcare
61164	Early Years Educator Qualified
139434	Enterprise Account Manager
37460	Named Account Executive
7634	Sales Associate Part Time Somerset Collection
127777	Locum Consultant Histopathologist Manchester
3972	Afrikaans Language Specialist Freelance AI Trainer Project
37415	Data Engineer
173031	Associate Veterinarian Branson Veterinary Hospital
1242	Director R&D AI Insights Analytics
53705	Manager Monitoring Observability
75693	Insurance Agent San Antonio TX
167712	Account Executive Territory Mid Market
56120	Operations Manager in Training
61973	Intern HR
168988	Emergency Veterinary Assistant Shift Lead Schaumburg IL
4479	Materials Science Engineering Specialist Freelance AI Trainer Project
170295	Operations Director
38789	Business Transformation Manager ComparaJa
91472	Social Worker Dementia and Care Quality
97120	Travel Medical Assistant
65623	Account Based Marketing Manager
54676	Certified Personal Trainer New Gym Opening
37003	Vice President Sales
57456	Heavy Equipment Field Technician Mechanic
39415	Behavioral Health Specialist Full Time FED
81342	GNC Engineer
43874	Marketing
51824	Surgical Coordinator
66937	Sports Data Collector Football Prostejov Czech Republic
176230	Programmatic Specialist
169138	Partner Support Specialist
143699	IT Support Technician VIP Support
70502	Technology Specialist
108008	Research Associate
32765	Head of Retail Expansion
64983	Head Varsity Boys Basketball Coach High School
123298	France Residents Survey Participants Clermont Ferrand France
103219	Technical Program Manager Software
78289	Industrial Designer I
108225	Social Growth Marketing Manager
96761	Gastroenterology Nurse Practitioner Physician Assistant
174165	Procurement Category Lead Technology
23144	Registered Nurse Home Health Visits
141606	Demi Chef de Partie
28584	Applied AI Architect GTM
16453	Medical Writer
126546	Band6 Locum Physiotherapist
77782	Maintenance Mechanic 1st Shift
166583	Warranty Representative
90707	Occupational Therapist Manchester City Wide Team
117572	Assistant General Counsel
41839	Manager Global Security Operations Center Taiwan
127397	Consultant in Adult Psychiatry London
95005	Software Engineer Lyft Business
166202	Mammography Tech PRN
141893	Waiter Waitress High Road House West London
44030	Vice President of Finance Technology
92515	Mechanical Estimator eg
135836	Industrial Designer
42538	Creator Affiliate Marketing Specialist 資深創作者暨聯盟行銷專員
1871	Operations Lead
126401	Band 6 CT Radiographer Taunton
21986	Home Health Aide Private Duty
31836	Développeur C++
19629	Key Account Leader
11716	Signal Processing Engineer Space
58190	Licensed Practical Nurse
113597	Aide aux personnes âgées
170346	Trading Operations Analyst
156436	Data Center Deployment Technician II
76963	Conference Operations
178750	Practice Architect Data Security
6663	Sales Internship Program
79720	Project Coordinator
85818	CSA QAQC Manager General Contractor Rep Culpeper VA
48825	Lead Engineer Issue Management Triage
6856	Manufacturing Engineer Weekend Shift
129844	Product Lead Trading Platform
53005	Engagement Ambassador New York University
129102	HR Business Partner
131747	Community Manager
82983	Superviseur de rayon
37614	Customer Threat Advisor Offensive
177708	Design Manager AMER
164097	Driver
92008	Ward Manager Charity Organisation
124687	Produktionsleiter Plant Manager Bezahlte Studie in Deutschland
18421	Substation Engineer MC I Oil and Gas
52173	Clinical Trial Manager Oncology APAC
111770	Registered Behavior Technician RBT
148544	Product Designer Terminal
36586	Director Program Management
805	Sheet Metal Fabricator
57171	Dispatcher
169152	Traveling Emergency Veterinarian D.C Maryland Virginia
14843	FP&A Manager
6544	VP Customer Sucess
36950	Universal Banker St Petersburg
73459	Hiring Dialysis Technician Woodstock Connecticut 6281
123066	Dutch Fluent Speakers AI Training Fully Remote Almere
9360	Construction Project Manager New Grad 2027
60046	Strategic Account Executive
137149	Corporate Commercial Paralegal
57639	Maintenance Manager
101041	Field Credit Officer South West
70130	Production Technician
16155	Systems Engineer
70704	Systems Administrator Level 1
106220	Seasonal Part Time Broadcast Audio 1
163634	Program Manager
65307	Electrical Engineer New Grad December 2026
25087	Electric Transmission Environmental Siting Specialist Texas Oklahoma
126813	Band 7 Cardiac Physiologist Warwick
91067	Social Worker Care Assessment and Triage
168302	Emergency Credentialed Veterinary Technician Eden Prairie MN
131059	Quality Assurance Manager Women s Apparel QA Import
151974	Personal Trainer
100183	Anesthesiologist Johnston City Comfortable with Peds
47278	Community Impact Associate
119149	Fashion Paid Advertising Strategist
55572	Kids Club Associate
147558	RC Analista de Governança Dados
175183	Speech Language Pathologist SLP Experienced Clinician
103636	Account Executive
81126	Sales Engineer Intern
1091	Sales Development Representative German Speaking
119745	Event Coordinator
44158	Inventory Supervisor
122870	Database Administrator Graduates AI Training Saltillo Mexico
62211	Electrical Engineering Intern Fall 2026
37741	Chief of Staff
46788	EMC Test Engineer
32088	DevOps Engineer Hybrid Kubernetes Terraform AWS Python Jenkins
21964	Home Health Aide HHA Pediatric Home Care
102231	Intake Attorney
33684	Behavior Technician
48880	Software Engineer Growth
4941	Consultor de Vendas Içara SC
111179	ABA Paraprofessional RBT Baldwin NY
122615	Cardiologists Freelance Remote Los Angeles US
104521	Detection Engineering Response Lead
83651	Verkoopmedewerker JD Groningen Vismarkt 32 38H
141006	Account Manager
19465	School Social Worker Remote
115395	経営戦略担当
176735	Software Engineer Media
25818	Linux System Administrator
172898	Part Time Nanny Housekeeper Austin TX
85011	UX UI Designer
145535	Data Solutions GTM Director
102706	Technical Program Manager Autonomy
137027	Director of Product Performance Unannounced Title
77694	Project Accountant
168110	Staffing Specialist Bilingual
65583	Operations Associate Patterson Flynn Fort Mill South Carolina
57054	Transportation and Capacity Analyst I II III IV
13744	Clinical Research Assistant Phlebotomist
56228	Service Associate
31794	Técnico em segurança do trabalho Presencial
79520	Graphic Designer Data Visualization Remote Contract
127630	Gynaecology Gynaecology Surgeon Consultant GLASGOW
146802	Account Manager Programmatic Media Strategy
19783	Director Strategic Sourcing
114289	Events Manager
154517	Personal Trainer
141636	Floor Manager Babington House
132289	Client Success Lead at Retail Insights
143970	Mechanical Engineer Structures Starship Infrastructure
44885	Lakebase Sales Specialist Enterprise UKI
69758	Audit Non for Profit
148162	Account Executive AI Sales
123771	Japanese Fluent Speakers AI Training Paris France
105030	Cyber Security Engineer
56677	Account Executive West Bay Area
115673	Solar Field Service Technician III
123870	Korean Fluent Speakers AI Training Bucheon South Korea
53970	Assistant Fitness Manager
37566	Software Engineer Platforms Productivity
41252	Revenue Recovery Specialist Atlanta GA
67491	People Operations Specialist APAC
30055	Business Applications Manager
58559	Customer Operations Associate
70348	Full Time Delivery Route Driver CU
97838	HRIS Analyst
6481	Fullstack Developer Python
23369	Residency Program New Nurse Graduates
9506	Entry Level Recruitment Consultant
11477	Manufacturing Engineer PCBA Barracuda
155740	Infrastructure Engineer
41130	ABA Supervisor
116760	Claims Adjuster Workers Compensation
63466	Datenschutzbeauftragter
23269	Registered Nurse RN Home Health Visits
42640	Executive Assistant
43815	Delivery Driver
19426	Reporter Gulf Region Middle East Remote
54239	Assistant General Manager
78452	Content Designer Sênior
4852	Social Editor CO
154518	Personal Trainer
83468	Vendeur CDI temps plein
98272	Systems Engineer I 6227
176342	WPP Media Associate Media Operations Egypt
174881	Sales Development Representative ShareGate
175348	VML Ogilvy Business Director Japan
94157	Physician Plastic Surgeon Microvascular
161214	Bilingual Strategic Cuisines Account Executive Spanish South Austin TX
102075	Sales Agent Residential
87716	Robotics Product Manager
149009	Product Manager Internal Tools AI Workflows Customer Success
2606	Registered Behavior Technician
140347	General Manager Restoration Services
35850	Data Engineer Brazil
162793	Field Services Technician
171475	Payroll Specialist
49177	National Mission Critical Client Leader
19234	Frontend Developer React Typescript
21330	Adult Caregiver CNA HHA
53111	Director of Solar Outreach and Market Strategy Remote Illinois US
170184	Head of Ü
13318	Events and Ministry Coordinator Offsite Part Time
15339	Construction Project Manager Intern or Co Op 2027 GSC
38152	RN Supervisor
38501	Academic Advisor
130464	Client Account Manager Large Customer Sales CPG
3110	Intake Specialist Remote
8574	Product Marketing Manager API Cloud Security
24914	Bilingual Customer Service Representative
28040	Payer Sales Executive
150884	Personal Trainer
51624	Ophthalmic Technician
143629	Instrumentation Fitter 2nd Shift
53888	Vessel Operator loading bulk materials
146681	Escrow and Payment Solutions Director
157509	Outsystems Developer
108509	Solutions Engineer Auth0 Canada
145278	Supplier Development Engineer Materials Special Processes Falcon Dragon
67954	Staff Corporate Security Engineer
72356	Physician Clinical Trials MD DO
50206	Software Engineer Traffic
43557	Marketing Science Analyst
59613	Data Engineer
78494	Product Manager Ads
151512	Personal Trainer
60821	Robotics Solutions Engineer
167392	Account Executive Commercial
116585	Dermatologist Gloucester MA
132115	Key Account Manager Germany
7604	Sales Associate Part Time Merrick Park
62331	Experienced Analysts Investment Banking
32407	Sales Manager Außendienst Großraum Hamm
47306	Forward Deployed Engineer
102470	Social Security Disability Attorney
61824	Market Strategy Partnerships Manager
105759	Creative Director Visual Design
143546	GNC Engineer Satellite Power Systems Starlink
105876	Sales Executive Hospitals
66675	Sports Data Collector American Football Boise Idaho USA
159657	Product Developer gn Bars
167924	Software Engineer Computer Vision
16391	Tax Hybrid
57520	Heavy Equipment Shop Technician Mechanic
91804	Social Worker Referral and Assessment
104052	Oncology Pathology Assist Temp Position
68909	Product Engineer
64226	Associate Associate Planning
172714	Primary Care Provider Nurse Practitioner Physician Assistant
7243	Merchandise Planner
157047	Account Executive
20330	Manager Regulatory Affairs
36632	Software Engineer
165051	Private Equity Event Volunteer
43002	Estimator
177398	Assessoria de Investimentos XP Future
162091	Quantitative Trader Researcher APAC
41506	Lead Software Engineer
38614	Executive Assistant
62	Customer Service and Labor
104336	Optometrist Sierra Vista Arizona
85301	Customer Support Specialist Vietnamese Speaker
30091	Industry Solution Leader High Tech Manufacturing Semiconductor and Fuel Cell Fabrication
20872	Intelligence Operations Integrator
24271	January 2027 BES Fellowship for Founding School Leaders Workforce Development Schools in Atlanta
47302	Enterprise Sales Engineer East
175162	Occupational Therapist New Grad
120520	Investor Development Specialist Swedish speaking
108713	Independent Non Executive Director Resident Seychelles
136904	C++ Developer Remote
2258	Consultant Data Platform
158223	Marketing Account Manager
80026	Lead Account Management Microsoft Ads
137931	Staff Backend Software Engineer Agent Platform Python Go
16601	Manager Account Management Advanced Packaging
79736	Instructional Designer Contract Remote
6942	Helpdesk Access Manager 875
38711	Account Executive
74976	Design Coordinator
61339	Account Executive Portuguese Speaker
149298	Associate Director Risk Based Quality Management Lead
121744	AI Trainer Fluent Lithuanian Speaker Peterborough UK
23748	Weekend Physical Therapist PT Living
88269	Project Geotechnical Engineer
54939	Fitness Manager
145297	Supplier Quality Specialist Starlink Aviation
72198	Workplace Technology Support Engineer
82131	Manager InfoSec Governance Risk and Compliance GRC
107452	Account Manager
60287	Lead Laminas
120109	Event Producer
44035	Yard Specialist
95138	DevSecOps Engineer II
119590	Safety Physician
154860	Personal Trainer Jonesboro GA
87440	Program Manager
56019	Operations Manager
156283	Account Specialist I
18130	Staff Backend Engineer Platform Consumer Apps Klover
78659	Paramedic Certified Licensed
175508	Associate Director Influencer Marketing
118023	Customer Service Representative
47260	Strategy Partner CPG
133163	Lead Quantum Scientist Government Programs QEC
174742	Customer Success Director
176942	Java Integration Engineer 1 month contract
103962	Associate Laboratory Director
83791	Fire Protection Consultant Sprinkler Systems
39831	Lead Executive Assistant Private Office Operations Manager CEO Office
137654	Strategy and Operations Consultant
85273	Manager Accounting
66028	Investment Analyst Inflation
62832	Store Manager all genders
147733	Agente Stone Consultor a Comercial Externo São Roque SP
21910	Home Care RN Registered Nurse Maple Grove
169234	Veterinary Sonographer White Plains NY
46339	Graduate Quant
171796	Head of Compliance and Risk
148077	Tax Staff II
98896	Project Manager Michels Energy Solutions Inc
17461	Associate Manufacturing Engineer Systems Tulip
153674	Personal Trainer
51834	Surgical Technician PRN
50760	2027 Quantitative Research Intern DV Equities
53614	PFAS Technical Manager
16581	Intern Process Engineering Advanced Packaging
172403	Technical Product Manager Development Vehicles
95383	Product Program Manager
10113	Acceptance Test Procedure Technician Roadrunner
177319	2027 Campus Recruiting General Intelligence Center Full Time Position
173432	Veterinary Assistant Client Service Representative
58503	Firmware Engineer
61876	Account Development Representative Spanish Speaker
160392	Assistant Restaurant Manager
43510	Intake Coordinator
27975	Account Executive Commercial France French speaking
4225	Garment Manufacturing QC Specialist Freelance AI Trainer Project
43405	Regional Sales Director Bay Enterprise
112401	Hospice Client Support Executive
150920	Personal Trainer
13870	Développeur Développeuse Logiciel
67263	Warehouse Yard Technician
24395	Ultrasonic Nondestructive Inspection NDI Technician
85813	Assistant Construction Manager Owner s Representative Data Center Construction
39277	CA Licensed Bilingual Spanish English Clinician LCSW or LPCC Behavioral Health
33337	Value Engineer Scale NAM
164375	시니어 데이터 엔지니어 데이터브릭스
99072	Talent Acquisition Specialist
126114	Band 5 Mental Health Nurse Mile End Hospital Barts Health NHS Trust
125776	Vice President Product Management CTV
151438	Personal Trainer
28366	Media Investment Director
125999	Band 5 Accident Emergency A&E Nurse Dorset
22706	Pediatric Licensed Practical Nurse Home Care
86099	Channel Development Manager
113857	Auxiliaire de vie
115925	Manager BI Analytics Engineer
103614	Account Executive
17059	Assessoria de Investimentos XP Future
162216	PubCo Fund Accounting Policy and Strategy Vice President
55009	Fitness Manager
151311	Personal Trainer
38733	Partner Business Manager
125306	Software Engineer Networking
111984	Marketing Coordinator Events
50025	Sales Development Representative Chicago
159144	Video Journalist Cinematography Temp
7894	Seasonal Operations Associate Part Time Summit at Fritz Farm
128498	Application Engineer AI Automation
158908	Account Executive Primary
149828	Community Manager Delaware Pines
113677	AIDE DE VIE
63724	Software Engineer C++
57262	Future Construction Project Manager Opportunities
102355	Nursing Home Attorney
177531	Datacentre Operations Engineer
73305	Caregiver Needed Support for an Adult Client Wilkes Barre PA
4399	Language Alignment Resource Partner Lao Freelance AI Trainer Project
49755	Workplace Facilities Coordinator
23387	Residency Program New Nurse Graduates
11780	Software Engineer Manufacturing Test EagleEye
117099	Mid Market Customer Success Manager
45863	Partner Manager Channels UKI Security
165162	Nurse Practitioner Physician Assistant Gilbert AZ United Vein and Vascular Centers
41709	Coupang Talent Network
82529	Quantitative Trader
130976	Product Manager Logs Observability
95538	Developer Salesforce Marketing Cloud
178390	Manager Integrated Marketing Provider
137741	Sushi Cook Sensei by Nobu
73592	In Home HHD Dialysis Care Partner 1 1 Client Chesterland OH 44026
124244	Neuroscience AI Training Leicester UK
945	Clinical Project Manager
56068	Operations Manager
169376	DVM Student Externship Preceptorship Program
169331	Veterinary Technician Student Externship Phoenix AZ
32988	Entrepreneur in Residence Samlino Group
66828	Sports Data Collector Football Gangneung South Korea
57546	Heavy Equipment Shop Technician Mechanic
12248	Staff Threat Attack Research Engineer
133400	Staff Software Engineer DevX Developer Infrastructure
52021	Software Engineer
170963	Programme Manager
118434	Store Manager
63990	Electrical Test Engineer Hardware Validation
37378	Customer Engineer LATAM MCR Santiago Chile
15921	Paid Media Account Director Maternity cover
55304	General Manager in Training
27885	Credentialed Veterinary Technician Brooklyn Park Slope
140917	Agency Customer Success Manager
24354	Part Time Fixed Wing Certified Flight Instructor CFI
10898	Operational Support Engineer Maritime
99470	Account Manager China
90655	Occupational Therapist Community Equipment Service
129578	QC Manager Cookware Kitchenware
113463	Aide à domicile
66142	DZ Driver Local Routes No Overnight Shifts
23321	Registered Nurse RN Private Duty
45651	Commercial Account Executive LATAM Spanish Speaking Boston
62076	Director Business Operations
110522	Hematologist Oncologist Cancer Specialists of North Florida
50171	Shift Lead Wellswood
93903	Registered Nurse Case Manager
118266	Shift Supervisor
118550	Product Manager GTM
91079	Social Worker Child Protection
97528	Seasonal Stylist Retail Part time
40599	Manager Thought Leader Programming
86933	Assoc Director Data Management
28619	Director Global Programs
59066	Machine Learning ML Engineer AI Insurtech
60790	Embodied AI Tooling Engineer
37734	Brand Manager
82654	Part Time Sales Assistant 20 uur
30306	Pessoa Desenvolvedora Especialista Back end Java e Kotlin Cadastro
82821	Store Manager Ferrara C.C Il Castello
75612	Insurance Agent Great Falls MT
166882	Structures Engineer Lunar
127919	MENTAL HEALTH NURSE RMN WHITECHAPEL
48	University Account Executive
35880	Process Design Specialist
70326	Preconstruction Manager Technische Akquise
93968	Customer Information Assistant
105513	Salesforce Technical Architect
128495	AI HPC NeoCloud Pre Sales System Engineer UAE
42688	Staff Insider Threat Analyst
122260	AI Training Sales Phoenix US
163435	Licensed Practical Nurse Boston
29821	Client Service Analyst Livechat Thai required
136846	Per Diem Clinical Research Nurse Home Visits
109297	French Post Editor Technology Company
99961	Global Sales Performance Trainer
124225	Neuroscience AI Training Brisbane Australia
124392	Pathologists Freelance Remote Cambridge UK
142735	Audience Growth Analyst
79535	Graphic Designer Data Visualization Remote Contract
29873	L&C Lead Licensing Counsel
99972	Recruiter
119476	Medical Writer Medical Communications
158126	Site Reliability Engineer
149497	Commercial Terrain Indépendant Freelance
120897	Private Equity Conference Volunteer
19519	Account Executive Corrections East
109932	Counsel Product Commercial
170933	FireFly FDE AI
56923	Sales Manager
103164	Product Manager II Software Communications
128893	Forward Deployed Engineer
33994	Center Based Board Certified Behavior Analyst BCBA
108569	Staff Site Reliability Engineer
166374	Nuclear Supplier Quality Engineer
115469	Financial Accounting Accounts Receivable Specialist
94613	Seasonal Ambassador South Shore Plaza
133887	Electrical Engineer I
7405	Operations Lead Broadway Plaza
159533	AP Instructor Calculus BC Remote
126171	Band 5 Pharmacy Technician Aseptic Services West Yorkshire
124531	Pharmacists Fluent Speakers AI Training Newcastle upon Tyne UK
24091	Delivery Manager 3
121076	Real Estate Agent Loan Originator
147838	Executivo a Comercial Hunter para Grandes Contas Piracicaba SP
93491	Automation Engineer EV Access Controls
25690	Care Navigator
100254	Planning Associate
87764	AAHA Accredited Hospital Seeking FT Associate Veterinarian Rita Ranch Pet Hospital
119527	Clinical Research Associate II
40455	Product Designer OpsUnity Freelance
3421	Fall Intern Talent Acquisition
52085	Licensed School Based Mental Health Clinician Lead
120633	Investor Relations Analyst Dutch speaking
168122	Asset Management Leasing Specialist
63978	RF Test Engineer Staff
174391	Retail Sales Manager Remote Region München
93497	Engineer Charging Software Testing
170439	Clinical Care Manager RN Hospice
168656	Emergency Veterinarian Part Time Dallas TX
162931	Data Scientist II Operations Reliability
140001	HR Business Partner APJ
85937	Assistant General Counsel
64934	Security Alarm Technician
82903	Directeur adjoint de magasin
64106	Kitchen Porter Seasonal Full Time Part Time On Call
60502	Retail General Manager University of Wisconsin Team Store
92449	Equipment Operator eg
11978	User Experience Researcher
172751	Account Manager
129376	Internship Research Development
78649	Digital Account Manager
66543	Scientist Scientist Computational Chemistry
123687	Italy Residents Survey Participants Milan Italy
119863	Country Manager Filipino
96237	Technical Product Marketing Manager
93410	Mechanical Design Engineer Automotive Service Tooling
36746	Derivatives Sales
135509	Sales Representative
106057	Software Engineer Party Cell
151531	Personal Trainer
7696	Sales Service Lead Los Angeles
101391	Paid Search Delivery Lead Media Operations
25235	SOUTH AUSTIN Transportation Internship
41849	쿠팡 로켓배송 직매입MD 브랜드매니저 채용 키친 주방용품 경력
57525	Heavy Equipment Shop Technician Mechanic
82532	Quantitative Trader
33564	Behavior Technician
100666	Enterprise Account Executive
80450	Lead Data Implementation Specialist
156468	Campaign Manager
20739	Business Development Representative
47845	Talent Community Olathe
130453	Technical Director AI Decision Systems
69831	Director of Strategic Operations
112042	Engineering Manager Private Dining
123039	Doctors AI Training Philadelphia US
13604	Roadway Engineer
56684	AV Lead Technician
9520	Personalberater
65261	Technical Writer 704
126154	Band 5 Paediatric Nurse RCN Preston
145337	Technical Trainer Electrical Starship
29934	Data Analyst Spot
44694	Exchange Relations Manager
147719	Agente Stone Consultor a Comercial Externo Santana de Parnaíba SP
148421	Head of Startup Sales Grower AMER
33707	Behavior Technician
119780	Events Manager
99190	Vice President Controller
63436	Solutions Lead Print to Digital
151855	Personal Trainer
79003	Business Intelligence Insights Lead
18368	Project Manager Civil Engineering Residential Land Development
89397	AI Software Engineer Self driving Laboratory
27617	Psychotherapist
165752	Strategic Growth Manager
15232	Superintendent Intern COOP
3282	Technical Product Manager Internal Developer Platform
37291	IT Asset Manager
7758	Seasonal Associate Sydney Westfield The Galeries
67006	Sports Data Collector Football Varnsdorf Czech Republic
146002	Technician Electronics Maintenance
131308	Lead Propulsion Analysis
84483	Illinois Psychiatric Mental Health Nurse Practitioner PMHNP Telehealth Addiction Mental Health Care
142006	Clinical Therapist
74819	MEP Coordinator
29722	Brand Ambassador Events
165744	Strategic Growth Associate
44637	Google Customer Engineer
175955	Media Consultant Mensch Google
72662	Executive Assistant
68336	Regional Sales Manager
110293	Primary Care Nurse Practitioner or Physician Assistant DC Adams Morgan Office
79432	Power Electronics Test Engineer
114320	Area Sales Manager Minneapolis
14920	Construction Project Manager Co op Spring Summer 2027
23865	Warehouse Associate
38309	Product Designer II Design Systems
73211	Behavior Technician RBT Leesburg VA
47685	Install Coordinator EDS
37451	Manager Customer Engineering India
12093	Staff Automation Engineer Manufacturing Automation
108729	P2P Partner Operations Lead
39291	2026 2027 Middle School Physical Education and Health Teacher
18275	Civil CAD Designer Land Development
94612	Seasonal Ambassador Southport Avenue
79054	Blockchain Analyst US
45569	Strategic Enterprise Account Executive Life Sciences
48551	Corporate Counsel Datacenters and Infrastructure
94785	Home Infusion Nurse
43576	Area Sales Manager
98221	Forward Deployed Engineer III 6706
33538	Behavior Technician
137522	Manager Mechanical Engineer
176323	VP Clients FTC
103996	Clinical Oncology Specialist Breast Oncology SoCal
2906	Software Engineer Reference Data
26620	Survey Scientist
73085	UX Product Designer Engagement
115728	Early Childhood Behavior and Classroom Coach Conway
63396	Digital Marketing Specialist Spécialiste du marketing numérique
125838	Adult Acute Speech and Language Therapist
150880	Personal Trainer
90310	Regional Sales Manager Specialty Military Foodservice
55279	General Manager
1320	2nd Grade Teacher
85414	Account Executive In Territory Tampa Miami Orlando Sarasota
156762	Performance Marketing Specialist
111930	Assistant Manager Global AP Financial Accounting
155228	Lead 3D Tracking Artist
156086	Associate Data Analyst
164973	Head of Digital Marketing at United Media
52327	Administrative Assistant Juvenile Justice
89711	SEO ASO Manager
108405	Software Engineer
70951	GPSU Military Federal Fellowship Hybrid
178196	Vice President Client Partnerships Publisher Cloud
118172	Overnight Customer Service Representative
115093	Underwriter II USDA Generalist
164383	Databricks Resident Solutions Architect
142984	Apprentice Controls Technician 2nd Shift
172615	Caregiver Home Care Assistant
40917	Accountant Accounts Payable
133486	Security Software Engineer IAM
77684	Engineering Lead
60104	Pediatric Occupational Therapist 500 Bonus
94684	Temporary Store Manager Liberty Center
164075	Carrier Manager II Transportation Management
141356	Independent Sales Representative
160501	1 on 1 High School Math Tutor Remote in US
43491	Director of Operations
133847	Business Development Director Satellites TS SCI Clearance
151619	Personal Trainer
90076	Controls Engineer II Sustaining Engineering
168726	Emergency Veterinarian Redmond WA
6046	Commercial Manager
92292	Production and Editorial Manager
17792	Attendant Adult Care Partner Laneville TX
17475	Composite Manufacturing Engineer
169250	Veterinary Technician Student Externship Brookhaven GA
41218	Leasing Sales Consultant Cortland Hollywood
76522	Residential Construction Field Manager
11448	Manager Product Sourcing Engineer
110487	Advanced Practice Provider Pacific Cancer Care
85049	Travel Occupational Therapist Home Modifications
178872	Partner Sales Specialist
160565	Part Time PreK Lower Elementary Math Teacher
89613	Supervisor II Cell Production
34727	Home Health Nurse LPN RN
97479	Floor Lead Retail Part time
114619	Relationship Specialist Southeast
62284	Gear Design Engineer
141450	Program Assistant Africa Peacebuilding and Developmental Dynamics
19147	A&P Mechanic Instructor
44655	Red Team Operator
53943	Legal Counsel NY
124997	Spanish Fluent Speakers AI Training Seville
66840	Sports Data Collector Football Hawtah Bani Tamim Saudi Arabia
17346	FPGA Engineer
57529	Heavy Equipment Shop Technician Mechanic
135362	Retail Sales Associate
158146	Account Executive Small and Medium Business SMB
103195	Optomechanical Engineer
76443	Real Estate Acquisition Consultant
19442	Mechanical Engineer Level 3
168304	Emergency Credentialed Veterinary Technician Falls Church VA
99322	Regional Sales Director Midwest
98453	Porter Texcoco
161812	Server
19497	IT Operations Technical Lead
149797	Retail Sales Associate Part Time
33310	Value Engineer Manufacturing Production
141155	Retail Assistant Store Manager Sandro Paris Westfield Valley Fair
102646	Spa Attendant Morning Shift Part Time
148702	Software Engineer Online Database Infrastructure
85780	Medical Science Liaison Field Medical Affairs Regional Medical Liaison Greater Texas
6276	Project Manager Public Works PE
175440	Activation TV Audio
22021	Home Health Visits Licensed Practical Nurse
98255	Director IC Strategy Growth
100074	Strategic Finance Associate G&A
92290	HR Intern
119327	Field Service Technician NETA 2
8392	Performance Video Editor
118475	Graduate Leadership Program Copywriter
128952	Future Internship Opportunities
115474	Director of Financial Planning Analysis
72970	PT Sorter
62334	FinTech Strategy and Business Development First Year Analyst
92396	Electrical Apprentice
35939	Special Education Teacher
175069	Low Latency C++ Developer
70824	Associate Business Development
83260	Vendeur CDI temps partiel
11647	Program Finance Manager AD&S
56549	Service Associate Night New Gym Opening
37154	Sales Manager
64762	Print Production Specialist
174358	Merchant Onboarding Specialist Launcher
67329	Segment Product Marketing Manager Field Service
166777	Join Our Talent Network
164441	Warehouse Associate
142595	Legal Counsel
101619	Consumer Engagement Ambassador Strategic Brands
177773	Director of Engineering Core Database
100481	FP&A Manager
136708	IT Risk Advisory
15522	Editor a de Conteúdo Bilíngue Inglês ou Espanhol
95407	Client Relations Associate Italian Speaker 12m FTC
160675	Associate Client Services Japanese Speaker
6598	Assistant Financial Controller
101714	Customer Service Specialist APAC
63579	Stagiair Operations Benelux
169593	Veterinary Receptionist
27567	Psychotherapist
4684	Spanish Audio Specialist Freelance AI Trainer Project
12107	Staff Electrical Engineer Drones
84626	Aide à domicile
137057	UI Artist MONOPOLY GO
143860	Manufacturing Engineer Machining
62252	Logistics Dispatch Manager
107692	Procurement Manager
79524	Graphic Designer Data Visualization Remote Contract
138501	Mobile Engineer Colombia All Levels
48335	Line Cook Bethesda
100513	Branch Manager
38029	Member Outreach Specialist
84097	Mixer 3rd Shift
18785	Account Executive
17968	Systems Engineer Mechanical
171248	Electrical Engineer Power Systems Design
164378	Databricks Resident Solutions Architect
30388	Operational Excellence Specialist
113188	Manager Project Management
165236	Vascular Technologist Sonographer
17472	Carbon Fiber Reinforced Polymer CFRP Manufacturing Technician
38647	Leasing Specialist
53103	Manager Revenue Operations
94801	Home Infusion Nurse
87926	Veterinary Technician LVT Compassion Animal Hospital
161744	Delivery Driver Restaurant Catering
6161	study skills tutor
123789	Japan Residents Survey Participants Kyoto Japan
37170	Sales Manager
82249	Associate Product Manager Dictionary Media Group
90515	Family Court Advisor
39296	Account Executive Commercial
107796	Associate Veterinarian
3495	Operations Staff Jacobs Pavilion
100438	Business Process Transformation Specialist
121705	AI Trainer Fluent Icelandic Speakers Iceland
110191	Nurse Practitioner or Physician Assistant Pacific Heights 100 000 Sign On Bonus Available
33513	Behavior Technician
144997	Satellite Policy Manager Starlink Mobile
54304	Assistant General Manager
22029	Home Health Visits Registered Nurse
14615	Supervisor Production
163262	GTM Strategy Growth Associate
83074	Vendeur CDD temps partiel
```

## Predictions — do not read until step 3

```
98608	OUT
119675	OUT
94501	OUT
77430	UNKNOWN
45292	OUT
1494	UNKNOWN
52680	OUT
111949	OUT
178856	OUT
169399	OUT
175022	UNKNOWN
126754	UNKNOWN
50982	UNKNOWN
81991	OUT
66423	UNKNOWN
16757	OUT
48759	UNKNOWN
92727	OUT
119760	OUT
71820	OUT
134600	OUT
154684	OUT
117866	OUT
1897	IN
172932	OUT
76359	UNKNOWN
50243	UNKNOWN
2510	OUT
137190	OUT
102019	UNKNOWN
150758	OUT
26612	UNKNOWN
120693	UNKNOWN
99636	UNKNOWN
80774	OUT
111439	OUT
96964	OUT
98586	OUT
40064	OUT
160703	UNKNOWN
100629	OUT
19793	IN
119821	UNKNOWN
132427	UNKNOWN
110908	OUT
56080	OUT
69222	OUT
116655	OUT
153729	OUT
153956	OUT
167994	OUT
107932	OUT
37116	UNKNOWN
132609	UNKNOWN
86733	UNKNOWN
122094	OUT
145298	OUT
32544	OUT
178671	OUT
74715	OUT
20268	UNKNOWN
151044	OUT
174355	OUT
91814	OUT
54183	OUT
95715	UNKNOWN
117709	OUT
26756	OUT
133803	UNKNOWN
64970	IN
38899	UNKNOWN
54086	OUT
42746	OUT
80561	OUT
117369	UNKNOWN
85259	UNKNOWN
60047	UNKNOWN
175074	IN
159529	OUT
106816	UNKNOWN
129268	UNKNOWN
87522	OUT
112783	OUT
177487	OUT
174232	OUT
132188	UNKNOWN
101086	OUT
27293	OUT
59004	OUT
129736	OUT
159125	OUT
124283	OUT
66932	OUT
12893	OUT
9197	IN
1782	UNKNOWN
19384	UNKNOWN
48157	OUT
145527	UNKNOWN
97728	OUT
114663	UNKNOWN
116491	IN
141652	OUT
52537	OUT
90207	UNKNOWN
112966	UNKNOWN
124797	OUT
78018	UNKNOWN
59704	UNKNOWN
104538	IN
152271	OUT
267	IN
159857	UNKNOWN
6130	OUT
142153	UNKNOWN
150514	OUT
76904	OUT
158431	OUT
99279	IN
78904	OUT
33927	OUT
13989	OUT
108362	OUT
22477	OUT
41942	OUT
51318	IN
137567	UNKNOWN
44625	IN
42631	UNKNOWN
153052	OUT
91360	OUT
42181	IN
119230	OUT
177751	OUT
68528	OUT
152848	OUT
45556	OUT
117587	UNKNOWN
26264	UNKNOWN
102273	OUT
107988	UNKNOWN
127952	OUT
132353	OUT
165146	OUT
122580	OUT
164496	UNKNOWN
130061	OUT
113303	OUT
106477	IN
155884	OUT
56138	UNKNOWN
32983	OUT
71681	OUT
29771	UNKNOWN
142924	UNKNOWN
64994	OUT
149929	OUT
79186	UNKNOWN
172865	UNKNOWN
156406	OUT
129596	OUT
100671	OUT
148102	UNKNOWN
117448	UNKNOWN
2581	OUT
16840	OUT
25457	OUT
102040	OUT
101324	UNKNOWN
170960	UNKNOWN
40676	UNKNOWN
28840	OUT
137694	UNKNOWN
128069	OUT
17503	OUT
2343	OUT
7506	OUT
35534	IN
90421	OUT
83165	OUT
99289	IN
14870	OUT
84741	OUT
89644	OUT
130127	UNKNOWN
26172	OUT
4948	OUT
47279	OUT
48947	OUT
79108	UNKNOWN
3553	OUT
144707	OUT
57795	OUT
53757	UNKNOWN
174246	OUT
176553	OUT
148626	OUT
10589	UNKNOWN
121177	UNKNOWN
160177	OUT
80231	OUT
49526	IN
152740	OUT
122277	OUT
152834	OUT
74355	OUT
43749	OUT
138025	UNKNOWN
37501	UNKNOWN
49900	UNKNOWN
65783	OUT
123558	OUT
163524	UNKNOWN
146278	OUT
31568	UNKNOWN
160630	OUT
133441	UNKNOWN
81064	OUT
17163	UNKNOWN
18144	UNKNOWN
45029	IN
57301	OUT
56394	OUT
104260	OUT
171719	UNKNOWN
115137	OUT
61164	OUT
139434	OUT
37460	OUT
7634	OUT
127777	UNKNOWN
3972	OUT
37415	IN
173031	OUT
1242	IN
53705	UNKNOWN
75693	OUT
167712	OUT
56120	OUT
61973	OUT
168988	OUT
4479	OUT
170295	UNKNOWN
38789	UNKNOWN
91472	OUT
97120	OUT
65623	OUT
54676	OUT
37003	OUT
57456	OUT
39415	OUT
81342	UNKNOWN
43874	UNKNOWN
51824	OUT
66937	OUT
176230	UNKNOWN
169138	UNKNOWN
143699	UNKNOWN
70502	UNKNOWN
108008	UNKNOWN
32765	OUT
64983	UNKNOWN
123298	OUT
103219	IN
78289	OUT
108225	OUT
96761	OUT
174165	OUT
23144	OUT
141606	OUT
28584	IN
16453	UNKNOWN
126546	OUT
77782	OUT
166583	OUT
90707	OUT
117572	OUT
41839	IN
127397	OUT
95005	IN
166202	UNKNOWN
141893	OUT
44030	OUT
92515	OUT
135836	OUT
42538	OUT
1871	UNKNOWN
126401	OUT
21986	OUT
31836	UNKNOWN
19629	OUT
11716	UNKNOWN
58190	OUT
113597	OUT
170346	UNKNOWN
156436	OUT
76963	UNKNOWN
178750	IN
6663	UNKNOWN
79720	OUT
85818	UNKNOWN
48825	UNKNOWN
6856	OUT
129844	IN
53005	OUT
129102	OUT
131747	OUT
82983	UNKNOWN
37614	UNKNOWN
177708	UNKNOWN
164097	OUT
92008	UNKNOWN
124687	UNKNOWN
18421	OUT
52173	OUT
111770	OUT
148544	OUT
36586	UNKNOWN
805	UNKNOWN
57171	OUT
169152	OUT
14843	UNKNOWN
6544	UNKNOWN
36950	UNKNOWN
73459	OUT
123066	OUT
9360	OUT
60046	OUT
137149	OUT
57639	OUT
101041	OUT
70130	OUT
16155	IN
70704	IN
106220	UNKNOWN
163634	UNKNOWN
65307	UNKNOWN
25087	UNKNOWN
126813	UNKNOWN
91067	OUT
168302	OUT
131059	IN
151974	OUT
100183	OUT
47278	UNKNOWN
119149	OUT
55572	UNKNOWN
147558	UNKNOWN
175183	OUT
103636	OUT
81126	UNKNOWN
1091	OUT
119745	OUT
44158	OUT
122870	UNKNOWN
62211	UNKNOWN
37741	UNKNOWN
46788	IN
32088	IN
21964	OUT
102231	OUT
33684	OUT
48880	IN
4941	OUT
111179	OUT
122615	OUT
104521	IN
83651	UNKNOWN
141006	OUT
19465	OUT
115395	UNKNOWN
176735	IN
25818	IN
172898	OUT
85011	OUT
145535	IN
102706	IN
137027	UNKNOWN
77694	OUT
168110	UNKNOWN
65583	OUT
57054	UNKNOWN
13744	OUT
56228	OUT
31794	UNKNOWN
79520	OUT
127630	OUT
146802	OUT
19783	OUT
114289	OUT
154517	OUT
141636	UNKNOWN
132289	OUT
143970	OUT
44885	OUT
69758	UNKNOWN
148162	OUT
123771	OUT
105030	IN
56677	OUT
115673	OUT
123870	OUT
53970	OUT
37566	IN
41252	UNKNOWN
67491	UNKNOWN
30055	UNKNOWN
58559	OUT
70348	OUT
97838	UNKNOWN
6481	IN
23369	OUT
9506	UNKNOWN
11477	OUT
155740	IN
41130	OUT
116760	OUT
63466	UNKNOWN
23269	OUT
42640	OUT
43815	OUT
19426	OUT
54239	OUT
78452	OUT
4852	OUT
154518	OUT
83468	OUT
98272	IN
176342	UNKNOWN
174881	OUT
175348	UNKNOWN
94157	OUT
161214	OUT
102075	OUT
87716	IN
149009	OUT
2606	OUT
140347	OUT
35850	IN
162793	OUT
171475	OUT
49177	UNKNOWN
19234	IN
21330	OUT
53111	OUT
170184	UNKNOWN
13318	OUT
15339	OUT
38152	OUT
38501	UNKNOWN
130464	OUT
3110	UNKNOWN
8574	OUT
24914	OUT
28040	OUT
150884	OUT
51624	OUT
143629	UNKNOWN
53888	OUT
146681	UNKNOWN
157509	UNKNOWN
108509	IN
145278	OUT
67954	IN
72356	OUT
50206	IN
43557	UNKNOWN
59613	IN
78494	UNKNOWN
151512	OUT
60821	IN
167392	OUT
116585	OUT
132115	OUT
7604	OUT
62331	UNKNOWN
32407	OUT
47306	IN
102470	OUT
61824	UNKNOWN
105759	UNKNOWN
143546	OUT
105876	OUT
66675	OUT
159657	UNKNOWN
167924	IN
16391	UNKNOWN
57520	OUT
91804	OUT
104052	UNKNOWN
68909	UNKNOWN
64226	UNKNOWN
172714	OUT
7243	OUT
157047	OUT
20330	UNKNOWN
36632	IN
165051	OUT
43002	OUT
177398	UNKNOWN
162091	IN
41506	IN
38614	OUT
62	UNKNOWN
104336	UNKNOWN
85301	UNKNOWN
30091	OUT
20872	UNKNOWN
24271	UNKNOWN
47302	UNKNOWN
175162	OUT
120520	OUT
108713	OUT
136904	UNKNOWN
2258	IN
158223	OUT
80026	OUT
137931	IN
16601	OUT
79736	OUT
6942	UNKNOWN
38711	OUT
74976	OUT
61339	OUT
149298	UNKNOWN
121744	OUT
23748	OUT
88269	OUT
54939	OUT
145297	OUT
72198	UNKNOWN
82131	OUT
107452	OUT
60287	UNKNOWN
120109	OUT
44035	UNKNOWN
95138	UNKNOWN
119590	OUT
154860	OUT
87440	UNKNOWN
56019	OUT
156283	OUT
18130	IN
78659	OUT
175508	OUT
118023	OUT
47260	UNKNOWN
133163	UNKNOWN
174742	OUT
176942	IN
103962	OUT
83791	IN
39831	OUT
137654	UNKNOWN
85273	OUT
66028	UNKNOWN
62832	OUT
147733	OUT
21910	OUT
169234	OUT
46339	UNKNOWN
171796	OUT
148077	UNKNOWN
98896	OUT
17461	OUT
153674	OUT
51834	OUT
50760	IN
53614	IN
16581	UNKNOWN
172403	IN
95383	UNKNOWN
10113	OUT
177319	UNKNOWN
173432	OUT
58503	IN
61876	OUT
160392	OUT
43510	OUT
27975	OUT
4225	OUT
43405	OUT
112401	OUT
150920	OUT
13870	UNKNOWN
67263	OUT
24395	OUT
85813	OUT
39277	OUT
33337	UNKNOWN
164375	UNKNOWN
99072	OUT
126114	OUT
125776	UNKNOWN
151438	OUT
28366	OUT
125999	OUT
22706	OUT
86099	UNKNOWN
113857	OUT
115925	UNKNOWN
103614	OUT
17059	UNKNOWN
162216	OUT
55009	OUT
151311	OUT
38733	UNKNOWN
125306	IN
111984	OUT
50025	OUT
159144	OUT
7894	OUT
128498	IN
158908	OUT
149828	OUT
113677	OUT
63724	IN
57262	OUT
102355	OUT
177531	UNKNOWN
73305	OUT
4399	OUT
49755	OUT
23387	OUT
11780	OUT
117099	OUT
45863	IN
165162	OUT
41709	UNKNOWN
82529	UNKNOWN
130976	UNKNOWN
95538	OUT
178390	OUT
137741	OUT
73592	UNKNOWN
124244	OUT
945	OUT
56068	OUT
169376	UNKNOWN
169331	OUT
32988	UNKNOWN
66828	OUT
57546	OUT
12248	UNKNOWN
133400	IN
52021	IN
170963	UNKNOWN
118434	OUT
63990	OUT
37378	IN
15921	OUT
55304	OUT
27885	OUT
140917	OUT
24354	OUT
10898	UNKNOWN
99470	OUT
90655	OUT
129578	UNKNOWN
113463	OUT
66142	OUT
23321	OUT
45651	OUT
62076	UNKNOWN
110522	OUT
50171	UNKNOWN
93903	OUT
118266	OUT
118550	UNKNOWN
91079	OUT
97528	OUT
40599	UNKNOWN
86933	IN
28619	UNKNOWN
59066	IN
60790	IN
37734	OUT
82654	OUT
30306	IN
82821	OUT
75612	OUT
166882	UNKNOWN
127919	OUT
48	OUT
35880	UNKNOWN
70326	UNKNOWN
93968	OUT
105513	IN
128495	UNKNOWN
42688	UNKNOWN
122260	OUT
163435	OUT
29821	UNKNOWN
136846	OUT
109297	OUT
99961	OUT
124225	OUT
124392	UNKNOWN
142735	UNKNOWN
79535	OUT
29873	OUT
99972	OUT
119476	UNKNOWN
158126	IN
149497	UNKNOWN
120897	OUT
19519	OUT
109932	OUT
170933	UNKNOWN
56923	OUT
103164	IN
128893	IN
33994	OUT
108569	IN
166374	UNKNOWN
115469	OUT
94613	OUT
133887	UNKNOWN
7405	UNKNOWN
159533	OUT
126171	OUT
124531	OUT
24091	UNKNOWN
121076	OUT
147838	UNKNOWN
93491	OUT
25690	UNKNOWN
100254	UNKNOWN
87764	OUT
119527	UNKNOWN
40455	OUT
3421	OUT
52085	OUT
120633	UNKNOWN
168122	UNKNOWN
63978	IN
174391	OUT
93497	IN
170439	OUT
168656	OUT
162931	IN
140001	OUT
85937	OUT
64934	OUT
82903	UNKNOWN
64106	OUT
60502	OUT
92449	OUT
11978	UNKNOWN
172751	OUT
129376	UNKNOWN
78649	OUT
66543	UNKNOWN
123687	OUT
119863	UNKNOWN
96237	OUT
93410	OUT
36746	UNKNOWN
135509	OUT
106057	IN
151531	OUT
7696	OUT
101391	UNKNOWN
25235	UNKNOWN
41849	UNKNOWN
57525	OUT
82532	UNKNOWN
33564	OUT
100666	OUT
80450	IN
156468	UNKNOWN
20739	OUT
47845	UNKNOWN
130453	IN
69831	UNKNOWN
112042	UNKNOWN
123039	OUT
13604	UNKNOWN
56684	UNKNOWN
9520	UNKNOWN
65261	UNKNOWN
126154	OUT
145337	OUT
29934	UNKNOWN
44694	UNKNOWN
147719	OUT
148421	OUT
33707	OUT
119780	OUT
99190	UNKNOWN
63436	UNKNOWN
151855	OUT
79003	UNKNOWN
18368	OUT
89397	OUT
27617	OUT
165752	UNKNOWN
15232	OUT
3282	IN
37291	IN
7758	UNKNOWN
67006	OUT
146002	OUT
131308	OUT
84483	OUT
142006	OUT
74819	OUT
29722	OUT
165744	UNKNOWN
44637	IN
175955	UNKNOWN
72662	OUT
68336	OUT
110293	OUT
79432	OUT
114320	OUT
14920	OUT
23865	UNKNOWN
38309	OUT
73211	OUT
47685	OUT
37451	UNKNOWN
12093	OUT
108729	UNKNOWN
39291	OUT
18275	OUT
94612	OUT
79054	IN
45569	OUT
48551	OUT
94785	OUT
43576	OUT
98221	IN
33538	OUT
137522	OUT
176323	UNKNOWN
103996	OUT
2906	IN
26620	UNKNOWN
73085	OUT
115728	OUT
63396	OUT
125838	OUT
150880	OUT
90310	OUT
55279	OUT
1320	OUT
85414	OUT
156762	OUT
111930	OUT
155228	UNKNOWN
156086	UNKNOWN
164973	OUT
52327	OUT
89711	UNKNOWN
108405	IN
70951	UNKNOWN
178196	IN
118172	OUT
115093	OUT
164383	IN
142984	OUT
172615	OUT
40917	OUT
133486	IN
77684	UNKNOWN
60104	OUT
94684	OUT
164075	UNKNOWN
141356	OUT
160501	OUT
43491	UNKNOWN
133847	OUT
151619	OUT
90076	OUT
168726	OUT
6046	OUT
92292	OUT
17792	UNKNOWN
17475	OUT
169250	OUT
41218	UNKNOWN
76522	OUT
11448	OUT
110487	UNKNOWN
85049	OUT
178872	OUT
160565	OUT
89613	OUT
34727	OUT
97479	OUT
114619	UNKNOWN
62284	UNKNOWN
141450	OUT
19147	OUT
44655	OUT
53943	OUT
124997	OUT
66840	OUT
17346	UNKNOWN
57529	OUT
135362	OUT
158146	OUT
103195	UNKNOWN
76443	UNKNOWN
19442	OUT
168304	OUT
99322	OUT
98453	OUT
161812	UNKNOWN
19497	IN
149797	OUT
33310	OUT
141155	OUT
102646	UNKNOWN
148702	IN
85780	UNKNOWN
6276	UNKNOWN
175440	UNKNOWN
22021	OUT
98255	UNKNOWN
100074	UNKNOWN
92290	OUT
119327	OUT
8392	OUT
118475	OUT
128952	UNKNOWN
115474	OUT
72970	UNKNOWN
62334	UNKNOWN
92396	UNKNOWN
35939	OUT
175069	UNKNOWN
70824	UNKNOWN
83260	OUT
11647	OUT
56549	OUT
37154	OUT
64762	UNKNOWN
174358	OUT
67329	OUT
166777	UNKNOWN
164441	UNKNOWN
142595	OUT
101619	OUT
177773	IN
100481	UNKNOWN
136708	UNKNOWN
15522	OUT
95407	UNKNOWN
160675	UNKNOWN
6598	OUT
101714	UNKNOWN
63579	UNKNOWN
169593	OUT
27567	OUT
4684	OUT
12107	UNKNOWN
84626	OUT
137057	OUT
143860	OUT
62252	OUT
107692	OUT
79524	OUT
138501	IN
48335	OUT
100513	UNKNOWN
38029	UNKNOWN
84097	UNKNOWN
18785	OUT
17968	OUT
171248	UNKNOWN
164378	IN
30388	UNKNOWN
113188	UNKNOWN
165236	OUT
17472	OUT
38647	UNKNOWN
53103	UNKNOWN
94801	OUT
87926	OUT
161744	OUT
6161	OUT
123789	OUT
37170	OUT
82249	UNKNOWN
90515	UNKNOWN
39296	OUT
107796	OUT
3495	UNKNOWN
100438	UNKNOWN
121705	OUT
110191	OUT
33513	OUT
144997	OUT
54304	OUT
22029	OUT
14615	OUT
163262	UNKNOWN
83074	OUT
```
