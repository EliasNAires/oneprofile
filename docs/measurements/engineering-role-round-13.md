# Round 13 of the engineering-role loop: the review

Issue #35, round 13, reviewed on 2026-09-24. The implementer's rules (see the "Round 13
implemented" comment on #35) re-classified the 179 098-row corpus. This review draws a fresh sample
of that classification, has it labelled blind, scores it and groups every disagreement.

The labels are in `src/test/resources/labels/engineering-role-round-13.tsv`. A subagent wrote them
from the criterion and the 1000 bare titles, reading nothing else. The criterion was read at
revision `d26763b`, which has no uncommitted edits.

## The numbers

The sample is 100 `IN`, 500 `OUT` and 400 `UNKNOWN` rows drawn at random from 163 685 eligible
rows. Every id held by an earlier fixture and every calibration title was excluded. A mistake is any
row whose state differs from the label.

| Gated number | Closing run (2026-10-03) | Round 13 | Gate | Met |
| --- | ---: | ---: | ---: | :---: |
| `IN` stratum error: labelled `OUT` or `UNKNOWN` | 34.00% | **30.00%** (30 of 100) | ≤ 15% | no |
| `OUT` stratum error: labelled `IN` or `UNKNOWN` | 3.80% | **2.60%** (13 of 500) | ≤ 10% | yes |
| `unruled`, as a share of the whole corpus | 3.49% (6 257) | **2.79%** (4 991) | ≤ 1% | no |
| `OUT`-stratum rows labelled `IN` | 3 of 500 | **1** of 500 | ≤ 5 | yes |

Reported but not gated:

| Number | Closing run | Round 13 |
| --- | ---: | ---: |
| `UNKNOWN` stratum error: labelled `IN` or `OUT` | 57.25% | **44.50%** (178 of 400) |
| Rows labelled `UNKNOWN` that the rules decided | 45 | **38** (26 `IN`, 12 `OUT`) |
| The labeller's own unknown pile | 216 | **260**: 114 `domain_ambiguity`, 129 `scope_ambiguity`, 17 `unruled` |

The one `OUT`-stratum row labelled `IN` is `AI Engineer AI Tutor`.

The `UNKNOWN` stratum, split by the rules' reason:

| Rules' reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |
| --- | ---: | ---: | ---: |
| `domain_ambiguity` (182) | 15 | 70 | 97 |
| `scope_ambiguity` (131) | 4 | 19 | 108 |
| `unruled` (87) | 4 | 66 | 17 |

**Gate verdict: not met.** The `IN` stratum error and the `unruled` share fail.

The `IN` stratum error does not come mainly from the rules. Its 30 rows split this way:

- 6 are rules wrong.
- 2 are labelling errors, so the error net of them is 28%.
- 22 fall in families the criterion does not decide: network engineers, solutions and customer
  engineers, systems engineers, and technology nouns under consultant and specialist.

Round 14 can take at most 6 points off this number by fixing rules. The rest waits on answers to
the questions below.

## What the rules get wrong

Titles, the state the criterion gives them, and the sentence that decides it.

### 1. Generic and yielding heads with a non-software modifier are left `domain_ambiguity` (46 rows, all labelled `OUT`)

`Cleaning Specialist`, `Customs Specialist`, `Credentialing Specialist`, `Education Specialist`,
`Intervention Specialist` (×2), `Renewal Specialist`, `Seasonal Loss Prevention Specialist`,
`Specialist Employee Relations EMEA` (×2), `Specialist Distribution Operations`, `Lifecycle
Specialist Time and Attendance AMER`, `Curriculum Design Specialist`, `Freelance Reddit
Specialist`, `Baby Imaging Specialist Mesa`, `Build Specialist Starship`, `CLS Line Haul Specialist
L4 East MXD`, `HSPD 12 Government Badging Credentialing Specialist`, `Stabilization Specialist CBHC
MCI`, `Learning Specialist 2nd Grade`, `LP Relations Specialist French speaking`, `Partnership
Delivery and Communication Specialist`. Then leads: `Animation Lead`, `External Affairs Lead`,
`Influencer Lead`, `Visual Lead Georgetown`, `Visual Lead Berkeley 4th St`, `OTC Oil Desk Lead`,
`Strategic Engagement Lead Life Sciences`, `Lead Affiliate Manager`, `Lead Lighting Artist`.
Then the student and graduate forms: `Graduate Physicist`, `Graduate recruiter at CFO Insights`,
`Graduate Partnership Manager at United Media`, `Graduate Babyzone`, `Fast Track to Leadership
Trainee`, `Synthesis Slurry Intern`, `Stage assistant ressources humaines`, `Working Student Visual
Design`, `Product Development Design Consumer Insights Intern Opportunities`. And the rest: `North
Grand Rapids Center Administrator`, `Consultor a Comercial Externo …`, `Consultor Especialista em
Gestão e Liderança …`, `Customer Happiness Associate … Analista de Experiência do Cliente …`.

→ **`OUT`.** The criterion defines generic heads as "domain-bound heads that name no work of their
own" and says: "With any modifier and no software qualifier, a generic head is OUT." `specialist`,
`lead`, `intern`, `graduate`, `trainee`, `administrator` and `consultor` name no work of their own.
None of these titles carries a software word. Where a function stands behind the yielding word
(`artist`, `recruiter`, `physicist`, `manager` with a market marker), that head is never-engineering
or market-decided, and the answer is the same.

The rules call the whole family `domain_ambiguity`. This is the largest single cause of `UNKNOWN`
stratum error.

### 2. Domain-bound heads with a market or discipline modifier are left `domain_ambiguity` (23 rows, all labelled `OUT`)

- **Engineers, building and plant:** `Building Engineer` (×2), `BMS Smart Building Engineer
  Supervising`, `Stationary Engineer`, `Material Handling Engineer`, `Utility Engineer Wastewater`,
  `Operations Engineer Blades and Vanes Foundry`, `Plastics Engineer Tooling Process`.
- **Engineers, aerospace, defence and electronics:** `Staff Project Engineer Rotors and Blades`,
  `Stage Fluids Engineer`, `Warhead Design Engineer`, `Remote Sensing OPIR Engineer 897`, `Harness
  Design Engineer`, `PLL Design Engineer`, `Digital Design Engineer`, `Manager Supplier
  Industrialization Engineering`, `Graduate Engineer Engineer in Training`.
- **Scientists and analysts:** `Scientist Drug Metabolism and Pharmacokinetics DMPK`, `Scientist
  II Degrader Antibody Conjugate Characterization`, `Lightning Scientist`, `Actuarial Analyst
  Pricing`, `Property Actuarial Analyst`.
- **Technician:** `Service Techniker Frankfurt`.

→ **`OUT`.** Two sentences decide these:

- "A **discipline marker decides OUT under any head**." Pharmacokinetics, actuarial,
  wastewater, aerospace propulsion, analog IC (PLL) and digital logic design are credentials.
- "A **market marker decides OUT only under a domain-bound head with no software qualifier**."
  `engineer` and `technician` are domain-bound. "A modifier no one has classed is a market
  marker", so `building`, `material handling` and `service` decide `OUT` even where no one has
  classed them as a discipline.

The rules stop at step 7 on these titles.

### 3. The head is misread (2 rows)

- `Experienced Engineering Recruiter` (rules `domain_ambiguity`) → **`OUT`**. The head is
  `recruiter`, which is never-engineering. `Engineering` here names whom the recruiter hires.
- `Educational Content Author Cloud Solution Architect Nebius Academy` (rules `IN`) → **not
  `IN`**. "The first head the title names": that head is `author`, which does not yield, so
  `architect` never becomes the head. The label is `scope_ambiguity`. Under the criterion the row
  is `OUT` if `author` is never-engineering and `unruled` if it is on no list.

### 4. A software qualifier is missed (8 rows, labelled `IN`)

`07.Data Engineer`, `Architect Server Side`, `Engineering Team Lead ETL`, `Expert ETL Developer`,
`Mainframe Engineer`, `PLM Developer`, `Backup and Recovery Engineer`, `Engineering Manager
Analytics House`. The rules call all eight `domain_ambiguity`.

→ **`IN`.** "A software domain under a head that can carry engineering work" (step 5). `data`,
`server side`, `ETL`, `mainframe`, `PLM` (the software package), `backup and recovery` and
`analytics` name software.

`07.Data Engineer` looks like a tokenising miss: the `07.` prefix is glued to `Data`.

### 5. A technology noun under an `engineer` head becomes `scope_ambiguity` (3 rows, labelled `IN`)

`IT Support Engineer`, `IT Support Engineer Application Administrator`, `Application Support
Engineer`.

→ **`IN`.** `support` yields to `engineer` ("so is `Network Support Engineer`"). The managers
ruling that makes a technology noun `scope_ambiguity` is written for rank heads: "A manager over a
technology noun…". Under `engineer`, `IT` and `application` are software qualifiers, so step 5
applies. The rank-head rule of round 13 reaches too far.

### 6. A qualifier is read out of a compound that names something else (2 rows, rules `IN`, labelled `OUT`)

- `Commissioning Engineer Energy Storage` → **`OUT`**. `energy storage` is a market (batteries and
  the grid), not data storage. It is an unclassed modifier under a domain-bound head, so it is a
  market marker (step 6).
- `Mgr Quality Management Systems` → **`OUT`**. Strip the rank word, as the managers ruling says,
  and `Quality Management Systems` is the ISO-quality function, not a software system.

### 7. `technical` is read as a software qualifier outside its ruling (2 rows, rules `IN`, labelled `scope_ambiguity`)

- `Technical Product Marketing Manager TPMM`
- `Technical Success Manager Strategic West`

→ **not `IN`.** The criterion gives `technical` its meaning only inside "`Technical Product
Manager` and `Technical Program Manager` are `IN`". Neither title is one of those phrases:

- The first is a product marketing manager. Under "One word holds a generic head open:
  `product`" it is `UNKNOWN` / `scope_ambiguity`.
- The second is customer success. Strip the rank word and `Technical Success` names no
  engineering function.

### 8. AI-training posts (2 rows)

- `AI Engineer AI Tutor` (rules `OUT`) → **`IN`**. "Software or computer-science expertise is
  `IN`". The expertise named is AI engineering. This is the round's only `OUT`-stratum row
  labelled `IN`.
- `Canada Residents AI Trainers East Zorra Tavistock Canada` (rules `OUT`) → **`UNKNOWN` /
  `scope_ambiguity`**. "A post that names no expertise is `UNKNOWN` / `scope_ambiguity`." `Residents`
  here means people living in Canada, not medical residents.

### 9. A rank head over a technology noun is decided `IN` (1 row)

`Production AI Innovation Lead Contract Remote` (rules `IN`) → **`UNKNOWN` / `scope_ambiguity`**.
"A manager over a technology noun that names no engineering function … is `UNKNOWN` /
`scope_ambiguity`." `AI innovation` names no engineering function.

### 10. A sales or recruiting manager over an IT market becomes `scope_ambiguity` (4 rows, labelled `OUT`)

`Manager Business Development IT Agency Staffing Recruitment`, `Account Manager IT Staffing and
Solutions`, `Client Director Frontier Data US`, `Head of Talent Sourcing Data as a Service`.

→ **`OUT`.** The managers ruling says "strip the rank word … and read what is left as a title".
What is left is business development, account management, client management and talent sourcing.
`IT` and `data` name the market these roles sell to or hire for, not what the manager runs.

### 11. Q1 outside English (1 row)

`인재풀 Research Scientist Engineer Language Lab` (rules `scope_ambiguity`) → **`OUT`**. `인재풀`
means "talent pool", a talent-community sign-up. "Q1 is read first" and "Meaning is read in any
language."

### 12. The `unruled` backlog (70 rows)

Four are labelled `IN`: `AI Builder`, `Cobalt Core Pentester UK Germany Nordics`, `IT Security
Architekt gn IAM OnPrem`, `Data Modelling Dataverse COM INGLÊS`. Sixty-six are labelled `OUT`, and
the full list is in the archive below. Among the 66:

- **Not vacancies (Q1):** `General Applications` (a phrase the criterion names), `General Comics
  Graphic Novels Application`, `2026 Female in Finance Event`, `Mission Healthcare Job Fair Redding`,
  `Tufts x Marshall Wace Technical Workshop`, `Test Email Confirmation Success`.
- **Never-engineering heads:** `HRBP` (×2), `Biostatistician`, `Tax`, `Podiatry`, `Groundman`, `Bus
  Person`, `Community Outreach Canvasser`, `Content Writer`, `Content Creator`, `Paraprofessionals`,
  `Home Care Aides`, `Mental HealthTherapist`, `Telehealth Providers MD DO NP`, `Six Sigma Black
  Belt`.
- **Executive and sales abbreviations:** `SVP …`, `RVP …`, `BD …`, `Business Development …`,
  `Fund Development`, `CS FP&A`.
- **Non-English:** `Geschäftsführer`, `Bauleiter in`, `Bankkaufmann frau als Kreditspezialist`,
  `Referendar`, `Team Leder for Salg`, `Event Koordinator`, `Commercial Terrain`, `Assessoria de
  Investimentos`, the Korean `매니저`, `담당자` and `기획자`, and the Japanese `リーダー候補` and
  `ALM領域`.

The corpus holds 4 991 `unruled` rows against a gate of 1 791.

## Labelling errors

Two rows, both in the `IN` stratum. Net of them, the `IN` stratum error is 28%.

- `Machine Learning Engineer Applied AI LLMs`, labelled `OUT`. Machine learning is a software
  domain under an engineering-capable head, so step 5 gives `IN`. No discipline marker is named.
- `Technical Product Manager Cable Systems`, labelled `UNKNOWN` / `domain_ambiguity`. The ruling
  reads: "`Technical Product Manager` and `Technical Program Manager` are `IN` … A discipline
  marker still decides all of them `OUT`." `Cable systems` is not a credential, so it is `IN`.

## Questions for Elias

Each group below is one the criterion does not decide. The rules and the labeller each chose, and
they chose differently.

1. **Construction, site and environmental project managers** (13 rows; rules `scope_ambiguity`,
   labelled `OUT`). This is the implementer's question 1 again, with more titles:
   - `Project Manager Construction` (×2), `Construction Project Manager Facilities`,
     `Capital Improvements Construction Project Manager`, `Project Manager Construction Management`
   - four construction PM intern and new-grad posts
   - `Associate Project Manager Site Design`, `Land Solutions Project Manager Electric
     Transmission`, `Project Manager Environmental Permitting Compliance`, `Legal Analyst
     Litigation Project Manager`

   Are `construction`, `site design`, `electric transmission`, `environmental permitting` and
   `litigation` discipline markers, which the credential test would suggest? Or do they stay
   unclassed market markers, which leaves every project manager `scope_ambiguity`?

2. **Solutions, customer, systems, QA and integration engineers, and robotics code roles**
   (15 rows).
   - Rules `IN`, labelled `domain_ambiguity`: `Solutions Engineer` (×2), `Solutions Consultant
     Retail SC3`, `Customer Engineer ANZ`, `Customer Engineer Singapore`, `QA Engineer`, `Systems
     Engineer`, `Lead Engineer L1 Integration`, `Staff Infrastructure Design Engineer`, `Director
     of Engineering infrastructure Operations`.
   - Rules `domain_ambiguity`, labelled `IN`: `Programmer`, `Developer Educator`, `Staff Autonomy
     Engineer Drone`, `Soft and Virtual ECU Engineer`, `Payments Engineer Acquiring Payment
     Processing`.

   Read to the letter, step 6 makes an unclassed modifier under `engineer` a market marker and
   decides `OUT`. That would put `Systems Engineer`, `Solutions Engineer` and `Payments Engineer`
   `OUT`, which neither side chose. Which of `solutions`, `customer`, `systems`, `QA`,
   `integration`, `infrastructure` and `autonomy` are software words, which are ambiguous, and are
   bare `Programmer` and `Developer` `IN`?

   The closing run's declined `domain_ambiguity` misses belong to this family.

3. **Network engineering** (6 rows): `Network Engineer Amsterdam`, `Network Engineer Mobile Core
   Network Services`, `Systems and Network Engineer`, `Network Specialist gn` and `Target Digital
   Network Analyst 1`, all rules `IN`, and `Transport IP TRIP Network Technician`, rules `OUT`. The
   labeller called all six `scope_ambiguity`. Does a software background alone open network
   engineering (Q4), or is it a CCNA-type credential?

4. **Technology nouns under non-rank heads** (7 rows).
   - Rules `IN`: `Data Inventory Specialist`, `COTS Integration Specialist 734`, `Information
     Technology Consultant Level 5 …`, `Lead Management Technology Consultant Manufacturing
     Production`.
   - Rules `OUT`: `Data Center Administrator`, `Project Coordinator Data Quality`, `Security
     Control Assessor Representative Task Lead`.

   The labeller called all seven `scope_ambiguity`. Does the managers ruling ("a manager over a
   technology noun … is `scope_ambiguity`") extend to `specialist`, `consultant`, `administrator`,
   `coordinator` and `assessor`?

5. **Designers** (3 rows): `Interaction UX Designer`, `Visual UI Designer 64` and `Product Designer
   AI Native Core`. The rules call them `OUT` and the labeller `scope_ambiguity`. Are UX, UI and
   product designers `OUT` (no code, and a software background does not open them) or
   `scope_ambiguity`?

6. **Researchers** (3 rows):
   - `Quantitative Researcher Trading Team`: rules `IN`, labelled `scope_ambiguity`.
   - `Staff Research Scientist AI Safety`: rules `scope_ambiguity`, labelled `IN`.
   - `Research Intern Model Shaping Winter 2027`: rules `domain_ambiguity`, labelled `IN`.

   Are AI research scientists `IN`, and is quantitative research `IN` or `scope_ambiguity`?

7. **`product` with a market marker** (2 rows): `Director Growth Product Strategy`, rules `OUT`,
   and `Product Support CVA Supervisor`, rules `scope_ambiguity`. The labeller went the other way on
   both. The criterion's text says "A product title with no software qualifier is
   `scope_ambiguity`". Its procedure lets step 6, the market marker, fire after step 6a has spared
   `product`. The implementer followed the procedure. Which one wins?

8. **Automation** (2 rows): `Marketing Automation Specialist` (labelled `OUT`) and `Manager Service
   Desk IT Automation` (labelled `scope_ambiguity`). The rules call both `IN`. Is `automation` a
   software qualifier, or does marketing automation fail Q3, as work on "authoring surfaces built
   for non-engineers"?

9. **Implementation consultants** (2 rows): `Enterprise Implementation Consultant` and `Enterprise
   Core Implementation Consultant West`. The rules call them `OUT` by step 6a and the labeller
   `scope_ambiguity`. Should they follow the named-packages ruling, where a functional consultant is
   `scope_ambiguity`?

10. **`Program Director`** (1 row; rules `OUT`, labelled `scope_ambiguity`). Does the
    program-manager ruling cover `program director`? In this corpus the title is often education or
    healthcare work.

11. **Architects on a named package** (1 row): `CRM Architect B2B`, rules `domain_ambiguity`,
    labelled `IN`. The packages ruling names developer and engineer (`IN`) and consultant, analyst
    and administrator (`scope_ambiguity`). It does not name architect.

## The disagreements

All 221, sorted by the predicted state, then the label.

```
id	title	label	label_reason	predicted	predicted_reason
69210	Commissioning Engineer Energy Storage	OUT		IN	
49395	Machine Learning Engineer Applied AI LLMs	OUT		IN	
53239	Marketing Automation Specialist	OUT		IN	
29186	Mgr Quality Management Systems	OUT		IN	
65188	COTS Integration Specialist 734	UNKNOWN	scope_ambiguity	IN	
100045	Customer Engineer ANZ	UNKNOWN	domain_ambiguity	IN	
37401	Customer Engineer Singapore	UNKNOWN	domain_ambiguity	IN	
30032	Data Inventory Specialist	UNKNOWN	scope_ambiguity	IN	
68684	Director of Engineering infrastructure Operations	UNKNOWN	domain_ambiguity	IN	
104526	Educational Content Author Cloud Solution Architect Nebius Academy	UNKNOWN	scope_ambiguity	IN	
141420	Information Technology Consultant Level 5 Columbia MD TS SCI Full Scope Polygraph	UNKNOWN	scope_ambiguity	IN	
137191	Lead Engineer L1 Integration	UNKNOWN	domain_ambiguity	IN	
33208	Lead Management Technology Consultant Manufacturing Production	UNKNOWN	scope_ambiguity	IN	
141090	Manager Service Desk IT Automation Hybrid Bangalore	UNKNOWN	scope_ambiguity	IN	
161555	Network Engineer Amsterdam	UNKNOWN	scope_ambiguity	IN	
166304	Network Engineer Mobile Core Network Services	UNKNOWN	scope_ambiguity	IN	
159632	Network Specialist gn	UNKNOWN	scope_ambiguity	IN	
79695	Production AI Innovation Lead Contract Remote	UNKNOWN	scope_ambiguity	IN	
117155	QA Engineer	UNKNOWN	domain_ambiguity	IN	
85197	Quantitative Researcher Trading Team	UNKNOWN	scope_ambiguity	IN	
28784	Solutions Consultant Retail SC3	UNKNOWN	scope_ambiguity	IN	
97966	Solutions Engineer	UNKNOWN	domain_ambiguity	IN	
46477	Solutions Engineer EMEA	UNKNOWN	domain_ambiguity	IN	
48626	Staff Infrastructure Design Engineer	UNKNOWN	domain_ambiguity	IN	
81568	Systems and Network Engineer	UNKNOWN	scope_ambiguity	IN	
163916	Systems Engineer	UNKNOWN	domain_ambiguity	IN	
70716	Target Digital Network Analyst 1	UNKNOWN	scope_ambiguity	IN	
167282	Technical Product Manager Cable Systems	UNKNOWN	domain_ambiguity	IN	
68919	Technical Product Marketing Manager TPMM	UNKNOWN	scope_ambiguity	IN	
9647	Technical Success Manager Strategic West	UNKNOWN	scope_ambiguity	IN	
45611	AI Engineer AI Tutor	IN		OUT	
122451	Canada Residents AI Trainers East Zorra Tavistock Canada	UNKNOWN	scope_ambiguity	OUT	
143228	Data Center Administrator	UNKNOWN	scope_ambiguity	OUT	
86103	Director Growth Product Strategy	UNKNOWN	scope_ambiguity	OUT	
135609	Enterprise Core Implementation Consultant West	UNKNOWN	scope_ambiguity	OUT	
135623	Enterprise Implementation Consultant	UNKNOWN	scope_ambiguity	OUT	
18881	Interaction UX Designer	UNKNOWN	scope_ambiguity	OUT	
116963	Product Designer AI Native Core	UNKNOWN	scope_ambiguity	OUT	
32313	Program Director	UNKNOWN	scope_ambiguity	OUT	
62270	Project Coordinator Data Quality	UNKNOWN	scope_ambiguity	OUT	
52237	Security Control Assessor Representative Task Lead	UNKNOWN	scope_ambiguity	OUT	
30050	Transport IP TRIP Network Technician	UNKNOWN	scope_ambiguity	OUT	
19014	Visual UI Designer 64	UNKNOWN	scope_ambiguity	OUT	
115256	07.Data Engineer	IN		UNKNOWN	domain_ambiguity
59175	Architect Server Side	IN		UNKNOWN	domain_ambiguity
1587	Backup and Recovery Engineer	IN		UNKNOWN	domain_ambiguity
129517	CRM Architect B2B	IN		UNKNOWN	domain_ambiguity
172470	Developer Educator	IN		UNKNOWN	domain_ambiguity
49543	Engineering Manager Analytics House	IN		UNKNOWN	domain_ambiguity
174749	Engineering Team Lead ETL	IN		UNKNOWN	domain_ambiguity
837	Expert ETL Developer	IN		UNKNOWN	domain_ambiguity
18915	Mainframe Engineer	IN		UNKNOWN	domain_ambiguity
138561	Payments Engineer Acquiring Payment Processing	IN		UNKNOWN	domain_ambiguity
128650	PLM Developer	IN		UNKNOWN	domain_ambiguity
87026	Programmer	IN		UNKNOWN	domain_ambiguity
161548	Research Intern Model Shaping Winter 2027	IN		UNKNOWN	domain_ambiguity
8886	Soft and Virtual ECU Engineer	IN		UNKNOWN	domain_ambiguity
66124	Staff Autonomy Engineer Drone	IN		UNKNOWN	domain_ambiguity
56893	Application Support Engineer	IN		UNKNOWN	scope_ambiguity
85468	IT Support Engineer	IN		UNKNOWN	scope_ambiguity
12844	IT Support Engineer Application Administrator	IN		UNKNOWN	scope_ambiguity
25562	Staff Research Scientist AI Safety	IN		UNKNOWN	scope_ambiguity
50958	AI Builder	IN		UNKNOWN	unruled
37864	Cobalt Core Pentester UK Germany Nordics	IN		UNKNOWN	unruled
31695	Data Modelling Dataverse COM INGLÊS	IN		UNKNOWN	unruled
159721	IT Security Architekt gn IAM OnPrem	IN		UNKNOWN	unruled
17698	Actuarial Analyst Pricing	OUT		UNKNOWN	domain_ambiguity
136927	Animation Lead	OUT		UNKNOWN	domain_ambiguity
42874	Baby Imaging Specialist Mesa	OUT		UNKNOWN	domain_ambiguity
155349	BMS Smart Building Engineer Supervising	OUT		UNKNOWN	domain_ambiguity
93197	Building Engineer	OUT		UNKNOWN	domain_ambiguity
93203	Building Engineer	OUT		UNKNOWN	domain_ambiguity
143073	Build Specialist Starship	OUT		UNKNOWN	domain_ambiguity
24306	Cleaning Specialist	OUT		UNKNOWN	domain_ambiguity
42328	CLS Line Haul Specialist L4 East MXD	OUT		UNKNOWN	domain_ambiguity
147459	Consultor a Comercial Externo Vaga Afirmativa para Pessoas com Deficiência PcD Rio de Janeiro RJ	OUT		UNKNOWN	domain_ambiguity
31255	Consultor Especialista em Gestão e Liderança Híbrido Rio de Janeiro	OUT		UNKNOWN	domain_ambiguity
113051	Credentialing Specialist	OUT		UNKNOWN	domain_ambiguity
50850	Curriculum Design Specialist	OUT		UNKNOWN	domain_ambiguity
36454	Customer Happiness Associate Exclusive for People with Disabilities PWD Analista de Experiência do Cliente Exclusiva para PCD São Paulo Hybrid	OUT		UNKNOWN	domain_ambiguity
63230	Customs Specialist	OUT		UNKNOWN	domain_ambiguity
17211	Digital Design Engineer	OUT		UNKNOWN	domain_ambiguity
58184	Education Specialist	OUT		UNKNOWN	domain_ambiguity
8395	Education Specialist Inclusive Pathways 26 27	OUT		UNKNOWN	domain_ambiguity
8788	Experienced Engineering Recruiter	OUT		UNKNOWN	domain_ambiguity
5586	External Affairs Lead	OUT		UNKNOWN	domain_ambiguity
156623	Fast Track to Leadership Trainee	OUT		UNKNOWN	domain_ambiguity
112776	Freelance Reddit Specialist	OUT		UNKNOWN	domain_ambiguity
26035	Graduate Babyzone	OUT		UNKNOWN	domain_ambiguity
36317	Graduate Engineer Engineer in Training	OUT		UNKNOWN	domain_ambiguity
164958	Graduate Partnership Manager at United Media	OUT		UNKNOWN	domain_ambiguity
30633	Graduate Physicist	OUT		UNKNOWN	domain_ambiguity
35236	Graduate recruiter at CFO Insights	OUT		UNKNOWN	domain_ambiguity
56872	Harness Design Engineer	OUT		UNKNOWN	domain_ambiguity
52222	HSPD 12 Government Badging Credentialing Specialist	OUT		UNKNOWN	domain_ambiguity
47091	Influencer Lead	OUT		UNKNOWN	domain_ambiguity
1460	Intervention Specialist	OUT		UNKNOWN	domain_ambiguity
36893	Intervention Specialist	OUT		UNKNOWN	domain_ambiguity
88783	Lead Affiliate Manager	OUT		UNKNOWN	domain_ambiguity
173	Lead Lighting Artist	OUT		UNKNOWN	domain_ambiguity
72655	Learning Specialist 2nd Grade	OUT		UNKNOWN	domain_ambiguity
131808	Lifecycle Specialist Employee Relations Transitions Canada	OUT		UNKNOWN	domain_ambiguity
131964	Lifecycle Specialist Time and Attendance AMER	OUT		UNKNOWN	domain_ambiguity
2337	Lightning Scientist	OUT		UNKNOWN	domain_ambiguity
120844	LP Relations Specialist French speaking	OUT		UNKNOWN	domain_ambiguity
9560	Manager Supplier Industrialization Engineering	OUT		UNKNOWN	domain_ambiguity
17547	Material Handling Engineer	OUT		UNKNOWN	domain_ambiguity
34219	North Grand Rapids Center Administrator	OUT		UNKNOWN	domain_ambiguity
144894	Operations Engineer Blades and Vanes Foundry	OUT		UNKNOWN	domain_ambiguity
66591	OTC Oil Desk Lead	OUT		UNKNOWN	domain_ambiguity
52052	Partnership Delivery and Communication Specialist	OUT		UNKNOWN	domain_ambiguity
93625	Plastics Engineer Tooling Process	OUT		UNKNOWN	domain_ambiguity
58551	PLL Design Engineer	OUT		UNKNOWN	domain_ambiguity
138849	Product Development Design Consumer Insights Intern Opportunities	OUT		UNKNOWN	domain_ambiguity
51962	Property Actuarial Analyst	OUT		UNKNOWN	domain_ambiguity
6989	Remote Sensing OPIR Engineer 897	OUT		UNKNOWN	domain_ambiguity
178777	Renewal Specialist	OUT		UNKNOWN	domain_ambiguity
85999	Scientist Drug Metabolism and Pharmacokinetics DMPK	OUT		UNKNOWN	domain_ambiguity
107535	Scientist II Degrader Antibody Conjugate Characterization	OUT		UNKNOWN	domain_ambiguity
60550	Seasonal Loss Prevention Specialist	OUT		UNKNOWN	domain_ambiguity
93570	Service Techniker Frankfurt	OUT		UNKNOWN	domain_ambiguity
43664	Specialist Distribution Operations	OUT		UNKNOWN	domain_ambiguity
131917	Specialist Employee Relations EMEA	OUT		UNKNOWN	domain_ambiguity
131918	Specialist Employee Relations EMEA	OUT		UNKNOWN	domain_ambiguity
52488	Stabilization Specialist CBHC MCI Temporary Position	OUT		UNKNOWN	domain_ambiguity
14808	Staff Project Engineer Rotors and Blades	OUT		UNKNOWN	domain_ambiguity
82958	Stage assistant ressources humaines	OUT		UNKNOWN	domain_ambiguity
134237	Stage Fluids Engineer	OUT		UNKNOWN	domain_ambiguity
3076	Stationary Engineer	OUT		UNKNOWN	domain_ambiguity
46316	Strategic Engagement Lead Life Sciences	OUT		UNKNOWN	domain_ambiguity
141960	Synthesis Slurry Intern	OUT		UNKNOWN	domain_ambiguity
6287	Utility Engineer Wastewater	OUT		UNKNOWN	domain_ambiguity
8166	Visual Lead Berkeley 4th St	OUT		UNKNOWN	domain_ambiguity
8176	Visual Lead Georgetown	OUT		UNKNOWN	domain_ambiguity
11980	Warhead Design Engineer	OUT		UNKNOWN	domain_ambiguity
156867	Working Student Visual Design	OUT		UNKNOWN	domain_ambiguity
59038	Account Manager IT Staffing and Solutions	OUT		UNKNOWN	scope_ambiguity
109350	Associate Project Manager Site Design	OUT		UNKNOWN	scope_ambiguity
14902	Capital Improvements Construction Project Manager	OUT		UNKNOWN	scope_ambiguity
163532	Client Director Frontier Data US	OUT		UNKNOWN	scope_ambiguity
24371	Construction Project Manager Facilities	OUT		UNKNOWN	scope_ambiguity
14934	Construction Project Manager Intern or Co Op 2027	OUT		UNKNOWN	scope_ambiguity
10007	Construction Project Manager Intern Summer 2027	OUT		UNKNOWN	scope_ambiguity
14959	Construction Project Manager Intern Summer 2027	OUT		UNKNOWN	scope_ambiguity
9361	Construction Project Manager New Grad 2027	OUT		UNKNOWN	scope_ambiguity
141390	Head of Talent Sourcing Data as a Service	OUT		UNKNOWN	scope_ambiguity
18343	Land Solutions Project Manager Electric Transmission Remote St Louis MO	OUT		UNKNOWN	scope_ambiguity
135725	Legal Analyst Litigation Project Manager	OUT		UNKNOWN	scope_ambiguity
166127	Manager Business Development IT Agency Staffing Recruitment	OUT		UNKNOWN	scope_ambiguity
72254	Product Support CVA Supervisor	OUT		UNKNOWN	scope_ambiguity
15400	Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
15409	Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
25136	Project Manager Construction Management	OUT		UNKNOWN	scope_ambiguity
88359	Project Manager Environmental Permitting Compliance	OUT		UNKNOWN	scope_ambiguity
89415	인재풀 Research Scientist Engineer Language Lab	OUT		UNKNOWN	scope_ambiguity
41579	쿠팡로지스틱스서비스 밀크런 운송 관리 담당자	OUT		UNKNOWN	unruled
92242	2026 Female in Finance Event	OUT		UNKNOWN	unruled
95691	2nd shift Unarmed	OUT		UNKNOWN	unruled
111416	ABA Paraprofessionals New York NY	OUT		UNKNOWN	unruled
146371	Admin Sprinter	OUT		UNKNOWN	unruled
146117	Advisory Curriculum Writer	OUT		UNKNOWN	unruled
17053	Assessoria de Investimentos Início de Carreira	OUT		UNKNOWN	unruled
141109	Bankkaufmann frau als Kreditspezialist	OUT		UNKNOWN	unruled
70259	Bauleiter in	OUT		UNKNOWN	unruled
48118	BD Nuclear Fission	OUT		UNKNOWN	unruled
32336	Bilingual Spanish Speaking Telehealth Providers MD DO NP Remote 1099	OUT		UNKNOWN	unruled
134437	Biostatistician	OUT		UNKNOWN	unruled
127225	Blood Sciences BMS Carlisle	OUT		UNKNOWN	unruled
48655	Boston Restaurant Teams	OUT		UNKNOWN	unruled
64425	Business Development Texas Water	OUT		UNKNOWN	unruled
29902	Business Risk Control	OUT		UNKNOWN	unruled
161690	Bus Person	OUT		UNKNOWN	unruled
149470	Commercial Terrain Nantes	OUT		UNKNOWN	unruled
79401	Community Outreach Canvasser	OUT		UNKNOWN	unruled
42465	Compliance L6 I Compliance Risk Assessment and Monitoring	OUT		UNKNOWN	unruled
42466	CS FP&A	OUT		UNKNOWN	unruled
3136	Customer Engagement Industry Advocacy Europe	OUT		UNKNOWN	unruled
63472	E learning Creator Global Design Projects	OUT		UNKNOWN	unruled
35110	Entry Level Content Writer at CFO Insights	OUT		UNKNOWN	unruled
132368	Event Koordinator frivillig	OUT		UNKNOWN	unruled
3414	Event Security Guest Services Staff Rocky Mountains	OUT		UNKNOWN	unruled
157969	Freelancer Get Paid to Visit and Test Airports Across the US	OUT		UNKNOWN	unruled
33388	Fund Development	OUT		UNKNOWN	unruled
44609	General Applications	OUT		UNKNOWN	unruled
1213	General Comics Graphic Novels Application	OUT		UNKNOWN	unruled
58519	Geschäftsführer	OUT		UNKNOWN	unruled
86291	Groundman	OUT		UNKNOWN	unruled
3440	Guest Services Staff PromoWest MegaCorp	OUT		UNKNOWN	unruled
58905	HRBP	OUT		UNKNOWN	unruled
167441	HRBP G&A	OUT		UNKNOWN	unruled
160145	Kitchen Mamma	OUT		UNKNOWN	unruled
174623	한국 시장 KOL Affiliate BD 매니저	OUT		UNKNOWN	unruled
164684	Legal Marketing	OUT		UNKNOWN	unruled
99847	Mission Healthcare Job Fair Redding	OUT		UNKNOWN	unruled
127963	NHS Sonography role General Gynae Weekends Leicester	OUT		UNKNOWN	unruled
113116	NP PA Virtual Urgent Care	OUT		UNKNOWN	unruled
109866	OmniCraft Labs 콘텐츠 기획자 5년 이상	OUT		UNKNOWN	unruled
22692	PCA s Home Care Aides Weekends	OUT		UNKNOWN	unruled
128144	Podiatry	OUT		UNKNOWN	unruled
171616	Prior Law Enforcement Washington DC CSO SSO	OUT		UNKNOWN	unruled
113395	Property Maintenance PT	OUT		UNKNOWN	unruled
159647	Referendar Wahlstation Legal gn	OUT		UNKNOWN	unruled
124819	Remote Study Participants AI Research Merida	OUT		UNKNOWN	unruled
73776	Rogersville TN Client 24 7 Rotating Shifts 8 Hour or 12 Hour Shifts	OUT		UNKNOWN	unruled
116837	RVP Channels Alliances Americas	OUT		UNKNOWN	unruled
163368	Security Shift Supervisors	OUT		UNKNOWN	unruled
103387	Six Sigma Black Belt Business Process Excellence	OUT		UNKNOWN	unruled
38030	Social Media Content Creator Freelance	OUT		UNKNOWN	unruled
128394	Specialty Dr Required for PICU role Yorkshire	OUT		UNKNOWN	unruled
83614	Store Colleague JD Breda 38H	OUT		UNKNOWN	unruled
40760	Surveillance Role Player	OUT		UNKNOWN	unruled
107390	SVP Global OFCI Procurement	OUT		UNKNOWN	unruled
135287	SVP Pharmacy Relations	OUT		UNKNOWN	unruled
16388	Tax	OUT		UNKNOWN	unruled
44500	Team Leder for Salg at CVX Ventures Denmark	OUT		UNKNOWN	unruled
90247	Test Email Confirmation Success	OUT		UNKNOWN	unruled
61256	Third in Charge	OUT		UNKNOWN	unruled
103278	Tufts x Marshall Wace Technical Workshop	OUT		UNKNOWN	unruled
95488	WI Licensed 1099 Mental HealthTherapist	OUT		UNKNOWN	unruled
115354	事業戦略 事業企画 リーダー候補	OUT		UNKNOWN	unruled
115361	保険事業開発 資産運用 ALM領域	OUT		UNKNOWN	unruled
```
