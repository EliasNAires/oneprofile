# Round 14 of the engineering-role loop: the review

Issue #35, round 14, reviewed on 2026-09-24. The implementer's rules (see the "Round 14
implemented" comment on #35) re-classified the 179 098-row corpus. This review draws a fresh sample
of that classification, has it labelled blind, scores it and groups every disagreement.

The labels are in `src/test/resources/labels/engineering-role-round-14.tsv`. A subagent wrote them
from the criterion and the 1000 bare titles, reading nothing else. The criterion was read at
revision `a450853`, which has no uncommitted edits. The rules were read as the implementer left
them: round 14's changes to `TitleClassification` are in the working tree and not yet committed.

## The numbers

The sample is 100 `IN`, 500 `OUT` and 400 `UNKNOWN` rows drawn at random from 162 685 eligible
rows. Every id held by an earlier fixture and every calibration title was excluded. A mistake is any
row whose state differs from the label.

| Gated number | Closing run (2026-10-03) | Round 13 | Round 14 | Gate | Met |
| --- | ---: | ---: | ---: | ---: | :---: |
| `IN` stratum error: labelled `OUT` or `UNKNOWN` | 34.00% | 30.00% | **22.00%** (22 of 100) | ≤ 15% | no |
| `OUT` stratum error: labelled `IN` or `UNKNOWN` | 3.80% | 2.60% | **5.40%** (27 of 500) | ≤ 10% | yes |
| `unruled`, as a share of the whole corpus | 3.49% (6 257) | 2.79% (4 991) | **2.60%** (4 654) | ≤ 1% | no |
| `OUT`-stratum rows labelled `IN` | 3 of 500 | 1 of 500 | **0** of 500 | ≤ 5 | yes |

Reported but not gated:

| Number | Closing run | Round 13 | Round 14 |
| --- | ---: | ---: | ---: |
| `UNKNOWN` stratum error: labelled `IN` or `OUT` | 57.25% | 44.50% | **41.00%** (164 of 400) |
| Rows labelled `UNKNOWN` that the rules decided | 45 | 38 | **44** (17 `IN`, 27 `OUT`) |
| The labeller's own unknown pile | 216 | 260 | **280**: 144 `domain_ambiguity`, 130 `scope_ambiguity`, 6 `unruled` |

The `UNKNOWN` stratum, split by the rules' reason:

| Rules' reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |
| --- | ---: | ---: | ---: |
| `domain_ambiguity` (182) | 24 | 39 | 119 |
| `scope_ambiguity` (140) | 9 | 26 | 105 |
| `unruled` (78) | 6 | 60 | 12 |

**Gate verdict: not met.** The `IN` stratum error and the `unruled` share fail. Round 15 is the last
round.

How the errors split between the rules, the labels and the criterion:

| Stratum | Rules wrong | Label wrong | Criterion silent | Error net of label errors |
| --- | ---: | ---: | ---: | ---: |
| `IN` (22) | 5 | 0 | 17 | 22.00% |
| `OUT` (27) | 3 | 1 | 23 | 5.20% |
| `UNKNOWN` (164) | 118 | 6 | 40 | 39.50% |

As in round 13, the `IN` stratum error is mostly not the rules'. Fixing the five rows the rules get
wrong brings it to 17%, still above the gate. The other 17 rows are in families the criterion does
not decide, most of them the solutions, systems and support engineers of round 13's question 2,
which is still open. The `OUT` stratum error rose from 2.6% to 5.4%. Round 14 decided more titles
`OUT`, and the labeller calls 23 of the new ones undecidable (questions 2, 6 and 7 below).

## What the rules get wrong

Titles, the state the criterion gives them, and the sentence that decides it.

### 1. A discipline marker is missed (28 rows, labelled `OUT`)

- **Electrical, electronic and computer hardware:** `Technical Lead High Voltage and Fault Handling`
  (rules `IN`), `Digital IC Design Engineer`, `Power and Board Design Engineer`.
- **Mechanical, CAD and manufacturing:** `Mechanisms Engineer`, `Mechanical Field Engineer Travel
  Required` (rules `scope_ambiguity`), `NX CAD Support Engineer`, `Cabinet Engineer CAD Engineer`,
  `CNC Programmer Mill Lathe Swiss`, `Composites R&D Manufacturing Engineering Lead`.
- **Aerospace, naval and nuclear:** `Aerodynamics Analyst`, `Propellant Development Engineer II`,
  `Staff Mission Design Engineer`, `Program Manager Payload and Space Systems` (rules
  `scope_ambiguity`), `Naval Architect`, `Engineer Reactor Vessel`.
- **Civil, building and environment:** `Fire Engineer` (×2), `VDC Engineer` (virtual design and
  construction), `Director Engineering Sciences Environmental Engineering`, `Environmental Technical
  Studies Manager FT Hybrid` (rules `scope_ambiguity`), `Professional Land Surveyor Project Manager`
  (rules `scope_ambiguity`).
- **Life sciences and pharma:** `Scientist Antibody Discovery`, `Scientist I In Vitro
  Pharmacology`, `Staff Engineer Drug Substance MSAT`, `Associate Director Drug Product` (rules
  `scope_ambiguity`), `Safeguards Enforcement Analyst Bio Harms` (biosecurity).
- **Accounting:** `Manager External Reporting Technical Accounting` (rules `scope_ambiguity`).
- **A plain market under `engineer`:** `Material Flow Engineer`.

→ **`OUT`.** "A **discipline marker decides OUT under any head**, domain-bound or domain-free, and
it beats a software qualifier." Each of these names a training a person is hired on: power
electronics, IC design, CAD, CNC machining, aerodynamics, propulsion, naval architecture, nuclear,
fire protection, environmental engineering, land surveying (a licence), pharmacology, accounting.
For the program, project and product managers the rulings add: "A discipline marker still decides
all of them `OUT`." For `Associate Director Drug Product`, step 4 comes before `product` holds
anything open.

`Material Flow Engineer` has no discipline, but `material flow` is logistics. The criterion names
logistics as a market, and under `engineer` "only a modifier that plainly names a market … is a
market marker". So step 6 decides it `OUT`. The rules added `material handling` in round 14, and
`material flow` is the same thing.

`Digital IC Design Engineer` was probably missed because round 14's new marker is the phrase
`digital design`, and `IC` stands between the two words.

### 2. A software qualifier is missed (21 rows, labelled `IN`)

The rules call these `domain_ambiguity`, except three they call `scope_ambiguity`: the AEM field
engineer, the embedded Linux field engineer and `Software Engineer Data Analyst`.

- **Security:** `Detection Engineer`, `SOC Engineer Detection Engineer`, `Escalation Engineer DLP`,
  `Cryptography Engineer`, `Staff CorpSec Engineer`.
- **Named packages and platforms:** `Appian Developer`, `Oracle Fusion Techno Functional Developer
  Remote US`, `M365 Architecture Engineering Lead`, `Director WMS Product Design and Engineering`,
  `KDB Developer`, `Windows Engineer`, `Ultimate Success Field Engineer US Shift AEM` (Adobe
  Experience Manager).
- **Developer tooling:** `Dev Tooling Engineer Must CLI GO`, `Staff Developer Experience
  Engineer`, `DevRel Engineer`.
- **Other software domains:** `Game Development Engineer`, `VoIP Engineer IV`, `Embedded Linux
  Field Engineer for Devices IoT`.
- **AI spelled another way:** `A.I Engineering Intern`, `IA Engineer Specialist` (`IA` is AI in
  Spanish and French).
- **The head is `engineer`:** `Software Engineer Data Analyst`. "The first head the title names" is
  `engineer`, and `software` is present. The `data analyst` phrase further on does not change the
  head.

→ **`IN`.** Step 5: "a software domain under a head that can carry engineering work". Under the
named-packages ruling: "Under `developer` or `engineer` they are `IN`."

### 3. The head is misread (6 rows, labelled `OUT`)

- `Specialist Account Executive SecOps Commercial IL OH CO` (rules `IN`). `specialist` yields, and
  the head behind it is `account executive`, a sales head. `SecOps` names what is sold.
- `Strategic Partnerships Manager AI API` (rules `IN`) and `ISV Technical Alliance Manager` (rules
  `scope_ambiguity`). "Strip the rank word … and read what is left as a title." What is left is
  partnerships and alliances, which are business development. This is round 13's group 10.
  Round 14's sale-or-hire words do not include `partnerships` or `alliance`.
- `Lead Security Officer` (rules `scope_ambiguity`). `lead` yields to `officer`. A security officer
  is a guard, not a security technology function.
- `Student CEO SCEO NORTHEASTERN UNIVERSITY` and `… ROWAN UNIVERSITY` (rules `domain_ambiguity`).
  The head behind `student` is `CEO`, an executive head that does no engineering. These are
  campus brand-ambassador posts.

→ **`OUT`**, by step 3 once the head is read correctly.

### 4. A technology noun under a rank head is decided (3 rows, labelled `scope_ambiguity`)

- `Director Enterprise Applications` (rules `IN`). Strip the rank word and `Enterprise
  Applications` is a technology noun that names no engineering function, like `IT` in `IT
  Manager`.
- `HRIS Manager` (rules `OUT`). The same applies: HRIS is a software system run by HR.
- `Data Product Analyst Corporate` (rules `IN`). The criterion gives `data analyst` as its example
  of a scope phrase: "some `data analyst` roles are engineering roles and some are not".

→ **`UNKNOWN` / `scope_ambiguity`**, by the managers ruling: "A manager over a technology noun that
names no engineering function … is `UNKNOWN` / `scope_ambiguity`." `Data Product Analyst` is also
decided by the product rule: "A product title with no software qualifier is `scope_ambiguity`."

### 5. A title the criterion leaves open is decided `OUT` (2 rows)

- `Sales Operations Analyst Commissions` (labelled `domain_ambiguity`). "Under a domain-free head a
  market marker settles nothing at all: `Treasury Analyst` and `Audit Analyst` are UNKNOWN."
- `Canada Residents AI Trainers Hinton Canada` (labelled `scope_ambiguity`). "A post that names no
  expertise is `UNKNOWN` / `scope_ambiguity`." This is round 13's group 8 again. The implementer
  declined it because it needs a list of place names. The criterion still decides it.

### 6. The `unruled` backlog (66 rows)

Six are labelled `IN`:

- `Multinational Digital Infrastructure Full Stack SW Eng US` (`SW Eng` is software engineer)
- `グローバルITサポートエンジニア` (global IT support engineer)
- `Full Stack Analytics Digital Unit Sports`
- `XStore Testing`
- `Implementation SME DAAS API GW`
- `Conseiller.ère en architecture de sécurité` (security architecture advisor)

Sixty are labelled `OUT`. The full list is in the archive below. Among them:

- **Not vacancies (Q1):** `Template Job`, `Test Job With Sensitive Questions` (×2), `Register your
  C.V for upcoming positions`, `Substitute and On Call Opportunities Inquire Here`, `Call for
  members SME Implementation Group SMEIG`, `AlphaSights Networking Dinner with ESADE University`,
  `IT Technology Decision Makers Paid ITSM ESM Research Study Liverpool UK`, `550 Madison`,
  `Gloucester Go Outdoors`, and the insight programmes `LDN SEE 2027 Trading Research` and `NYC
  Winter INSIGHT 2027 Trading Research`.
- **Never-engineering heads:** `Server` (×2), `Personal Shopper`, `Case Publicist`, `Lex
  Columnist`, `Nutritionist …`, `Veterinary Criticialist`, `Medical Sewer I II`, `Grade Setter`,
  `Patient Account Coder`, `Specialty Dr for adult inpatient ward …`, `Haematology Blood
  Transfusion BMS` (biomedical scientist, ×2), `Bioengineer Preclinical Development In Vivo`,
  `Actors Performers …`, `Educational Videos Actor …`, `Payment Center Customer Service Rep`,
  `Home Care Providers`, `Tax`, `Deal Pricing`, `Trust and Safety Compliance`.
- **Staff and team posts with no function:** `Community Events Staff`, `Guest Services Staff`,
  `Pine Street Restaurant Team`, `Entry Level Kitchen Position`, `Store Colleague JD Enschede`,
  `1st Shift Unarmed`.
- **Non-English:** `Technischer Vertrieb` (technical sales), `Leitender Konstrukteur für
  fluidtechnische Komponenten` (lead mechanical designer), `Technicien.ne en automatisation`,
  `Motorista de Distribuição` (driver), `Asociado de Almacén` (warehouse associate), `Commercial
  Terrain`, `Coordinateur trice des Partenariats`, `Stagiair e Digital Video Audio Advertising`,
  `Vil du forme fremtidens datadrevne markedsføring` (marketing), `シニアリクルーター` (senior
  recruiter), the Japanese `事業企画 リーダー候補` posts (business planning), and the Korean character
  modelling artist, brand manager and content planning posts.

`Server` and `Tax` are back from round 13. The implementer explained why neither can be a
never-engineering head: a never-engineering word ahead of a head takes that head. Both are bare
titles here, with no head behind them, so they are `OUT` by the criterion either way.

The corpus holds 4 654 `unruled` rows against a gate of 1 791.

## Labelling errors

Seven rows. One is in the `OUT` stratum, so that stratum's error net of it is 5.2%. The other six
are in the `UNKNOWN` stratum.

- `Functional Consultant EMEA`, labelled `scope_ambiguity` (rules `OUT`). No package is named, so
  the named-packages ruling does not apply. "With any modifier and no software qualifier, a
  generic head is OUT."
- `Immigration Program Manager`, `D&I Program Manager Professional Services` and `Project Manager
  Qualitative Healthcare Market Research`, labelled `OUT` (rules `scope_ambiguity`). "Product,
  program and project managers are `UNKNOWN` / `scope_ambiguity`." Only a discipline marker
  decides them `OUT`. Immigration, D&I and market research are markets, not credentials.
- `Lead Product Design`, `Technical Product Marketing Manager` and `Manager Product Sourcing
  Engineering`, labelled `OUT`. "One word holds a generic head open: `product`. A product title
  with no software qualifier is `scope_ambiguity`, whatever market marker it carries." The rules
  give `scope_ambiguity` to the first two, which is right. They give `domain_ambiguity` to the
  third, which is also not `OUT`.

## Questions for Elias

Each group below is one the criterion does not decide. The rules and the labeller each chose, and
they chose differently. Questions 1, 2, 4 and 13 were asked in earlier rounds and are still open.
They come back with new titles.

1. **Interns, graduates and students with a non-software modifier** (11 rows; rules
   `domain_ambiguity`, labelled `OUT`). This is the implementer's question 1.
   - `Intern Damages and Valuations`, `Global Consulting Internship at United Media`, `Voice of
     the Customer Intern Summer 2027`, `2026 Fall Education Programs Intern onsite`, `Design
     Intern Winter Spring 2027`, `Total Rewards Intern …`, `Summer 2027 Human Resource
     Internship`
   - `Intern Fleet Coordinator`, `Stage Assistant Satisfaction janvier 2027`, `Student Ministry
     Coordinator Development`
   - `Raleigh Land Site Development New Grad`

   The criterion names `intern` as a yielding head but not as a generic one. Is an intern with no
   software word `OUT` the way a generic head is? If so, `Research Intern Frontier Agents` is lost
   unless `research` holds the head open. In this sample, 11 interns with no software word are
   labelled `OUT`, and the one intern labelled `IN` (`A.I Engineering Intern`) has a software
   word.

2. **Construction, energy and environmental project managers** (12 rows; rules
   `scope_ambiguity`, labelled `OUT`). This is round 13's question 1 again, with more titles.
   - `Project Manager Construction` (×3), `Construction Project Manager`, `Construction Project
     Manager New Grad 2027` (×2), `Construction Project Manager Intern Summer 2027`, `Entry Level
     Project Manager Construction`, `Project Manager Foundations Michels Construction Inc`
   - `Project Manager Natural Gas Transmission Projects`, `Project Manager Engineering Midstream
     Oil and Gas`
   - `Environmental Program Manager`

   Are `construction`, `oil and gas`, `gas transmission` and `environmental` discipline markers
   for project managers? Construction management is a degree and a licence in many places. Or are
   they markets, which leaves every project manager `scope_ambiguity`?

3. **`Project Lead` and `Project Director`** (2 rows; rules `OUT`, labelled `scope_ambiguity`).
   Does the product, program and project managers ruling cover `lead` and `director`? The managers
   ruling strips both as rank words. Round 13's question 10 asked the same about `Program
   Director`.

4. **Solutions, systems, support, sales and customer engineers** (13 rows). This is round 13's
   question 2, still open. The criterion revision says only that `Systems Engineer` and `Autonomy
   Engineer` are not `OUT`.
   - Rules `IN`, labelled `domain_ambiguity`: `Technical Support Engineer`, `Application Engineer
     Customer Experience`, `Sustainment Systems Engineer`, `International Expat Staff Systems
     Engineering Integration Engineer`, `Solutions Engineer East Coast Remote`.
   - Rules `IN`, labelled `OUT`: `Enterprise Solutions Consultant`.
   - Rules `domain_ambiguity`, labelled `IN`: `Integrations Engineer`, `Digital Experience
     Engineer`, `Technical Project Manager Digital Experience`, `Presales Engineer ISV HLS`,
     `Staff Autonomy Engineer MHE Vision`, `Director of Engineering iCasino FBG`.
   - Rules `scope_ambiguity`, labelled `IN`: `Specialist Sales Engineer Data Security`.

   Which of `solutions`, `systems`, `application`, `technical support`, `integrations`, `digital
   experience`, `presales`, `autonomy` and `iCasino` are software words? Are sales, presales and
   solutions engineers `IN` under Q4? This family is 13 of the 22 `IN` stratum errors this round.

5. **`technical` outside the product and program rulings** (3 rows).
   - `Technical Lead Lending Savings`: rules `IN`, labelled `domain_ambiguity`.
   - `Technical Account Manager Mandarin Speaking Singapore`: rules `IN`, labelled
     `scope_ambiguity`.
   - `Technical Services Manager`: rules `scope_ambiguity`, labelled `OUT`.

   Is `technical lead` a software role on its own? Is a technical account manager `IN` under Q4,
   `scope_ambiguity`, or an account manager and so `OUT`?

6. **`production`, `service`, `launch`, `commissioning` and `QA/QC` under `engineer` and
   `technician`** (10 rows; rules `OUT`, labelled `domain_ambiguity`).
   - `Production Technician` (×2), `Operations Engineer Starlink Production`
   - `Service Technician`, `Service Technician Frankfurt`
   - `Launch Reliability Engineer`, `Specialist Diagnostic and Commissioning Engineer`, `QA QC
     Technician`, `TPS Technician II First Shift`
   - `Community Engineer multiple roles and seniority levels`

   The criterion revision says that under `engineer` "only a modifier that plainly names a market —
   `building`, `hotel`, `retail` — is a market marker". Do `production`, `service`, `launch` and
   `commissioning` plainly name a market? The labeller says no, so they fall to step 7. The rules
   say yes. Round 14 made `production` a market marker. Most of these posts are plant and
   field work, so `OUT` is probably what they are, but by the criterion that would need them to be
   discipline markers, not markets.

7. **Bare heads with only a rank word in front** (5 rows; rules `OUT`, labelled `domain_ambiguity`
   except the third, labelled `unruled`).
   - `Assistant General Manager`, `Co Manager Assistant Manager`
   - `Associate Implementational Planning UK`
   - `Technologist Req#1230`, `Systems Operations Associate`

   This is the implementer's question 3, about `Team Lead`. Are `assistant`, `general`, `co`,
   `team` and `group` modifiers, which decide a generic head `OUT` at step 6a? Or are they part of
   the rank, which leaves the head bare and at step 7? Is `technologist`, which this corpus mostly
   uses for medical technologists, a never-engineering head?

8. **Technology nouns under non-rank heads, and business systems** (11 rows). This is round 13's
   question 4, still open.
   - Rules `IN`, labelled `scope_ambiguity`: `Supply Chain Systems Analyst WMS`, `Business Systems
     Analyst New Product Introduction`, `Analyst Business Systems`, `Manager Supply Chain Planning
     Systems`, `Technology Strategy Consultant 2026 start`, `Strategic Security Advisor New England
     Northeast region`, `IT Support II`.
   - Rules `OUT`, labelled `scope_ambiguity`: `IT Service Desk Agent`, `IT Internal Auditor`,
     `Manager Digital Finance Supply Chain Transformation`, `Head of Sport CRM`.

   Is a business systems analyst `IN` or `scope_ambiguity`, as `data analyst` is? Does the
   technology-noun ruling reach `consultant`, `advisor`, `support`, `agent` and `auditor`? Is
   `CRM` in `Head of Sport CRM` the package or customer-relationship marketing?

9. **`AI` under a rank word** (1 row): `AI Search Innovation Lead Madrid based`, rules `IN`,
   labelled `scope_ambiguity`. This is the implementer's question 2.

10. **`analytics` under a rank word** (2 rows): `Associate Director Business Analytics` and
    `Manager Insights and Analytics`, rules `OUT`, labelled `scope_ambiguity`. Is `analytics` a
    technology noun, like `data` (`scope_ambiguity`), or a market (`OUT`)? The implementer priced
    `analytics` as a software qualifier and declined it.

11. **Data analysts and data science** (4 rows; rules `scope_ambiguity`, labelled `IN`).
    - `Technical Data Analyst Data Warehouse Services`, `BUSINESS DATA ANALYST III DAAS API GW`
    - `Data Science Lead Defense Accounts`, `Manager Data Science`

    Does a second software word (`data warehouse`, `API`) settle a data analyst as `IN`? Is data
    science an `IN` role, so that its managers are `IN` by the managers ruling?

12. **Network operations** (1 row): `Missile Defense Agency MDA NOC Technician`, rules
    `scope_ambiguity`, labelled `IN`. Round 13's question 3, on network engineering, covers this.

13. **Designers** (1 row): `Lead UI UX Designer Unannounced Project`, rules `IN`, labelled `OUT`.
    This is round 13's question 5. The rules and the labeller have now swapped sides from round
    13.

14. **Hardware architecture and non-software work under `developer`** (4 rows; rules
    `domain_ambiguity`, labelled `OUT`).
    - `Performance Architect CPU Cluster`, `NPU Architect`
    - `Geospatial Analysis All Purpose Subject Matter Expert Mid 848`
    - `Training Content Developer Instructor TechELINT`

    Are `CPU` and `NPU` discipline markers, like `FPGA`? Is `geospatial` a discipline? Under the
    letter of the criterion, `Training Content Developer` is a `developer` with no marker, so it
    falls to step 7. Should `content`, like `writer`, make the head something else?

## Decided after the review

The questions above were settled the same day from Q1–Q4 and the credential test, and written
into `docs/engineering-role-criterion.md`. Round 15 implements against that revision. The
round-14 labels were written before it, so where a label here and the revised criterion
disagree, the criterion wins.

1. **Student forms** (`intern`, `graduate`, `trainee`, `student`) are generic heads. With a
   modifier and no software word they are `OUT`. `Research Intern Frontier Agents` is recovered by
   its software words, not by holding `research` open.
2. **Construction, oil and gas, and environmental permitting** are discipline markers. So is
   `commissioning`. `Construction Project Manager` is `OUT`.
3. The product, program and project ruling covers **any rank word**: `Project Lead` and `Project
   Director` are `scope_ambiguity`.
4. **Function words** (`solutions`, `systems`, `sales`, `presales`, `application`, `technical
   support`, `customer`, `integration`, `production`) are no marker and no software qualifier.
   Bare, the title falls to step 7 as `domain_ambiguity`. With a software word it is `IN`. `sales`
   and `presales` yield to `engineer`.
5. **`technical`** reads as `engineering`. A bare `Technical Lead` is `domain_ambiguity`. `Technical
   Account Manager` and `Technical Services Manager` are `scope_ambiguity`.
6. Under **`technician`**, `production`, `service` and `QA/QC` are trade credentials, so `OUT`.
   Under `engineer`, `production` is a function word (`Production Engineer` is also an SRE title).
7. **Rank words are not modifiers** (`assistant`, `associate`, `deputy`, `co`, `senior`, `team`,
   `group`). `Team Lead` is a bare head. `general` is a modifier, so `Assistant General Manager` is
   `OUT`.
8. The **technology-noun ruling** reaches every generic head. `AI`, `analytics`, `enterprise
   applications`, `HRIS` and `digital transformation` are technology nouns: `scope_ambiguity`. An
   engineering function word makes them `IN` (`Director Applied AI`). A head naming the help-desk
   work itself (`Service Desk Agent`) is `OUT`. A partnership, alliance, sales or sourcing role
   over a technology noun is `OUT`.
9. **Data science is `IN`**, and so is its management. A data analyst or business systems analyst
   is `scope_ambiguity`, and `IN` when the title names a warehouse, an API or ETL.
10. **Network engineers are `IN`**. Network and NOC technicians are `scope_ambiguity`.
11. **Designers** (UX, UI, visual, product) are `OUT`, and so are content roles under `developer`.
    `CPU` and `NPU` architecture are hardware credentials, like `FPGA`.

## The disagreements

All 213, sorted by the predicted state, then the label.

```
id	title	label	label_reason	predicted	predicted_reason
2855	Enterprise Solutions Consultant	OUT		IN	
106598	Lead UI UX Designer Unannounced Project	OUT		IN	
178940	Specialist Account Executive SecOps Commercial IL OH CO	OUT		IN	
97863	Strategic Partnerships Manager AI API	OUT		IN	
167285	Technical Lead High Voltage and Fault Handling	OUT		IN	
14431	Application Engineer Customer Experience	UNKNOWN	domain_ambiguity	IN	
150052	International Expat Staff Systems Engineering Integration Engineer	UNKNOWN	domain_ambiguity	IN	
69471	Solutions Engineer East Coast Remote	UNKNOWN	domain_ambiguity	IN	
53641	Sustainment Systems Engineer	UNKNOWN	domain_ambiguity	IN	
26414	Technical Lead Lending Savings	UNKNOWN	domain_ambiguity	IN	
114410	Technical Support Engineer	UNKNOWN	domain_ambiguity	IN	
61346	AI Search Innovation Lead Madrid based	UNKNOWN	scope_ambiguity	IN	
167947	Analyst Business Systems	UNKNOWN	scope_ambiguity	IN	
12665	Business Systems Analyst New Product Introduction	UNKNOWN	scope_ambiguity	IN	
177586	Data Product Analyst Corporate	UNKNOWN	scope_ambiguity	IN	
19767	Director Enterprise Applications	UNKNOWN	scope_ambiguity	IN	
95562	IT Support II	UNKNOWN	scope_ambiguity	IN	
105060	Manager Supply Chain Planning Systems	UNKNOWN	scope_ambiguity	IN	
70986	Strategic Security Advisor New England Northeast region	UNKNOWN	scope_ambiguity	IN	
12292	Supply Chain Systems Analyst WMS	UNKNOWN	scope_ambiguity	IN	
62184	Technical Account Manager Mandarin Speaking Singapore	UNKNOWN	scope_ambiguity	IN	
30666	Technology Strategy Consultant 2026 start	UNKNOWN	scope_ambiguity	IN	
54318	Assistant General Manager	UNKNOWN	domain_ambiguity	OUT	
88617	Co Manager Assistant Manager	UNKNOWN	domain_ambiguity	OUT	
30787	Community Engineer multiple roles and seniority levels	UNKNOWN	domain_ambiguity	OUT	
143730	Launch Reliability Engineer	UNKNOWN	domain_ambiguity	OUT	
144083	Operations Engineer Starlink Production	UNKNOWN	domain_ambiguity	OUT	
44229	Production Technician	UNKNOWN	domain_ambiguity	OUT	
70127	Production Technician	UNKNOWN	domain_ambiguity	OUT	
92604	QA QC Technician	UNKNOWN	domain_ambiguity	OUT	
28699	Sales Operations Analyst Commissions	UNKNOWN	domain_ambiguity	OUT	
78180	Service Technician	UNKNOWN	domain_ambiguity	OUT	
93560	Service Technician Frankfurt	UNKNOWN	domain_ambiguity	OUT	
137264	Specialist Diagnostic and Commissioning Engineer	UNKNOWN	domain_ambiguity	OUT	
134687	Systems Operations Associate	UNKNOWN	domain_ambiguity	OUT	
56979	Technologist Req#1230	UNKNOWN	domain_ambiguity	OUT	
131562	TPS Technician II First Shift	UNKNOWN	domain_ambiguity	OUT	
97177	Associate Director Business Analytics	UNKNOWN	scope_ambiguity	OUT	
122460	Canada Residents AI Trainers Hinton Canada	UNKNOWN	scope_ambiguity	OUT	
178509	Functional Consultant EMEA	UNKNOWN	scope_ambiguity	OUT	
97393	Head of Sport CRM	UNKNOWN	scope_ambiguity	OUT	
115475	HRIS Manager	UNKNOWN	scope_ambiguity	OUT	
149726	IT Internal Auditor	UNKNOWN	scope_ambiguity	OUT	
102238	IT Service Desk Agent	UNKNOWN	scope_ambiguity	OUT	
35653	Manager Digital Finance Supply Chain Transformation	UNKNOWN	scope_ambiguity	OUT	
178158	Manager Insights and Analytics	UNKNOWN	scope_ambiguity	OUT	
32318	Project Director	UNKNOWN	scope_ambiguity	OUT	
178271	Project Lead II	UNKNOWN	scope_ambiguity	OUT	
176100	Associate Implementational Planning UK	UNKNOWN	unruled	OUT	
138432	A.I Engineering Intern	IN		UNKNOWN	domain_ambiguity
1572	Appian Developer	IN		UNKNOWN	domain_ambiguity
31272	Cryptography Engineer	IN		UNKNOWN	domain_ambiguity
178843	Detection Engineer	IN		UNKNOWN	domain_ambiguity
6305	DevRel Engineer	IN		UNKNOWN	domain_ambiguity
136160	Dev Tooling Engineer Must CLI GO	IN		UNKNOWN	domain_ambiguity
1741	Digital Experience Engineer	IN		UNKNOWN	domain_ambiguity
60615	Director of Engineering iCasino FBG	IN		UNKNOWN	domain_ambiguity
72789	Director WMS Product Design and Engineering	IN		UNKNOWN	domain_ambiguity
178729	Escalation Engineer DLP	IN		UNKNOWN	domain_ambiguity
10496	Game Development Engineer	IN		UNKNOWN	domain_ambiguity
78490	IA Engineer Specialist	IN		UNKNOWN	domain_ambiguity
140304	Integrations Engineer	IN		UNKNOWN	domain_ambiguity
31470	KDB Developer	IN		UNKNOWN	domain_ambiguity
175253	M365 Architecture Engineering Lead	IN		UNKNOWN	domain_ambiguity
26258	Oracle Fusion Techno Functional Developer Remote US	IN		UNKNOWN	domain_ambiguity
163626	Presales Engineer ISV HLS	IN		UNKNOWN	domain_ambiguity
155957	SOC Engineer Detection Engineer	IN		UNKNOWN	domain_ambiguity
66125	Staff Autonomy Engineer MHE Vision	IN		UNKNOWN	domain_ambiguity
163544	Staff CorpSec Engineer	IN		UNKNOWN	domain_ambiguity
44004	Staff Developer Experience Engineer	IN		UNKNOWN	domain_ambiguity
65081	Technical Project Manager Digital Experience	IN		UNKNOWN	domain_ambiguity
166313	VoIP Engineer IV	IN		UNKNOWN	domain_ambiguity
82587	Windows Engineer	IN		UNKNOWN	domain_ambiguity
75988	BUSINESS DATA ANALYST III DAAS API GW	IN		UNKNOWN	scope_ambiguity
140322	Data Science Lead Defense Accounts	IN		UNKNOWN	scope_ambiguity
30810	Embedded Linux Field Engineer for Devices IoT	IN		UNKNOWN	scope_ambiguity
49969	Manager Data Science	IN		UNKNOWN	scope_ambiguity
30017	Missile Defense Agency MDA NOC Technician	IN		UNKNOWN	scope_ambiguity
92116	Software Engineer Data Analyst	IN		UNKNOWN	scope_ambiguity
178919	Specialist Sales Engineer Data Security	IN		UNKNOWN	scope_ambiguity
20261	Technical Data Analyst Data Warehouse Services	IN		UNKNOWN	scope_ambiguity
171007	Ultimate Success Field Engineer US Shift AEM	IN		UNKNOWN	scope_ambiguity
89264	Conseiller.ère en architecture de sécurité	IN		UNKNOWN	unruled
46524	Full Stack Analytics Digital Unit Sports	IN		UNKNOWN	unruled
75995	Implementation SME DAAS API GW	IN		UNKNOWN	unruled
10884	Multinational Digital Infrastructure Full Stack SW Eng US	IN		UNKNOWN	unruled
166570	XStore Testing	IN		UNKNOWN	unruled
139521	グローバルITサポートエンジニア	IN		UNKNOWN	unruled
37006	2026 Fall Education Programs Intern onsite	OUT		UNKNOWN	domain_ambiguity
134088	Aerodynamics Analyst	OUT		UNKNOWN	domain_ambiguity
24142	Cabinet Engineer CAD Engineer	OUT		UNKNOWN	domain_ambiguity
66414	CNC Programmer Mill Lathe Swiss	OUT		UNKNOWN	domain_ambiguity
24311	Composites R&D Manufacturing Engineering Lead	OUT		UNKNOWN	domain_ambiguity
46662	Design Intern Winter Spring 2027	OUT		UNKNOWN	domain_ambiguity
105529	Digital IC Design Engineer	OUT		UNKNOWN	domain_ambiguity
137492	Director Engineering Sciences Environmental Engineering	OUT		UNKNOWN	domain_ambiguity
85805	Engineer Reactor Vessel	OUT		UNKNOWN	domain_ambiguity
83773	Fire Engineer	OUT		UNKNOWN	domain_ambiguity
83840	Fire Engineer	OUT		UNKNOWN	domain_ambiguity
6939	Geospatial Analysis All Purpose Subject Matter Expert Mid 848	OUT		UNKNOWN	domain_ambiguity
164942	Global Consulting Internship at United Media	OUT		UNKNOWN	domain_ambiguity
137502	Intern Damages and Valuations	OUT		UNKNOWN	domain_ambiguity
57616	Intern Fleet Coordinator	OUT		UNKNOWN	domain_ambiguity
10699	Manager Product Sourcing Engineering	OUT		UNKNOWN	domain_ambiguity
93407	Material Flow Engineer	OUT		UNKNOWN	domain_ambiguity
17550	Mechanisms Engineer	OUT		UNKNOWN	domain_ambiguity
114700	Naval Architect	OUT		UNKNOWN	domain_ambiguity
52814	NPU Architect	OUT		UNKNOWN	domain_ambiguity
10893	NX CAD Support Engineer	OUT		UNKNOWN	domain_ambiguity
157693	Performance Architect CPU Cluster	OUT		UNKNOWN	domain_ambiguity
17233	Power and Board Design Engineer	OUT		UNKNOWN	domain_ambiguity
165833	Propellant Development Engineer II	OUT		UNKNOWN	domain_ambiguity
25226	Raleigh Land Site Development New Grad	OUT		UNKNOWN	domain_ambiguity
13001	Safeguards Enforcement Analyst Bio Harms	OUT		UNKNOWN	domain_ambiguity
163792	Scientist Antibody Discovery	OUT		UNKNOWN	domain_ambiguity
62894	Scientist I In Vitro Pharmacology	OUT		UNKNOWN	domain_ambiguity
166978	Staff Engineer Drug Substance MSAT	OUT		UNKNOWN	domain_ambiguity
81373	Staff Mission Design Engineer	OUT		UNKNOWN	domain_ambiguity
49455	Stage Assistant Satisfaction janvier 2027	OUT		UNKNOWN	domain_ambiguity
136200	Student CEO SCEO NORTHEASTERN UNIVERSITY	OUT		UNKNOWN	domain_ambiguity
136204	Student CEO SCEO ROWAN UNIVERSITY	OUT		UNKNOWN	domain_ambiguity
36206	Student Ministry Coordinator Development	OUT		UNKNOWN	domain_ambiguity
86275	Summer 2027 Human Resource Internship	OUT		UNKNOWN	domain_ambiguity
81035	Total Rewards Intern Winter January 2027 8+ Months	OUT		UNKNOWN	domain_ambiguity
70726	Training Content Developer Instructor TechELINT	OUT		UNKNOWN	domain_ambiguity
1164	VDC Engineer	OUT		UNKNOWN	domain_ambiguity
167257	Voice of the Customer Intern Summer 2027	OUT		UNKNOWN	domain_ambiguity
131615	Associate Director Drug Product	OUT		UNKNOWN	scope_ambiguity
6011	Construction Project Manager	OUT		UNKNOWN	scope_ambiguity
9356	Construction Project Manager Intern Summer 2027	OUT		UNKNOWN	scope_ambiguity
15356	Construction Project Manager New Grad 2027	OUT		UNKNOWN	scope_ambiguity
15360	Construction Project Manager New Grad 2027	OUT		UNKNOWN	scope_ambiguity
117178	D&I Program Manager Professional Services	OUT		UNKNOWN	scope_ambiguity
10019	Entry Level Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
139718	Environmental Program Manager	OUT		UNKNOWN	scope_ambiguity
43705	Environmental Technical Studies Manager FT Hybrid	OUT		UNKNOWN	scope_ambiguity
62101	Immigration Program Manager	OUT		UNKNOWN	scope_ambiguity
30868	ISV Technical Alliance Manager	OUT		UNKNOWN	scope_ambiguity
100484	Lead Product Design	OUT		UNKNOWN	scope_ambiguity
143760	Lead Security Officer	OUT		UNKNOWN	scope_ambiguity
136819	Manager External Reporting Technical Accounting	OUT		UNKNOWN	scope_ambiguity
109566	Mechanical Field Engineer Travel Required	OUT		UNKNOWN	scope_ambiguity
95235	Professional Land Surveyor Project Manager	OUT		UNKNOWN	scope_ambiguity
25920	Program Manager Payload and Space Systems	OUT		UNKNOWN	scope_ambiguity
15109	Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
15132	Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
76198	Project Manager Construction	OUT		UNKNOWN	scope_ambiguity
18370	Project Manager Engineering Midstream Oil and Gas	OUT		UNKNOWN	scope_ambiguity
99030	Project Manager Foundations Michels Construction Inc	OUT		UNKNOWN	scope_ambiguity
53804	Project Manager Natural Gas Transmission Projects	OUT		UNKNOWN	scope_ambiguity
87358	Project Manager Qualitative Healthcare Market Research	OUT		UNKNOWN	scope_ambiguity
148911	Technical Product Marketing Manager	OUT		UNKNOWN	scope_ambiguity
6076	Technical Services Manager	OUT		UNKNOWN	scope_ambiguity
95690	1st Shift Unarmed	OUT		UNKNOWN	unruled
106199	2027 NFL Rotational Program	OUT		UNKNOWN	unruled
604	550 Madison	OUT		UNKNOWN	unruled
30669	Actors Performers for a Bluey x CAMP Production	OUT		UNKNOWN	unruled
8671	AlphaSights Networking Dinner with ESADE University	OUT		UNKNOWN	unruled
107558	Asociado de Almacén Nivel 1 Cranford NJ	OUT		UNKNOWN	unruled
128452	Bioengineer Preclinical Development In Vivo	OUT		UNKNOWN	unruled
78519	Call for members SME Implementation Group SMEIG	OUT		UNKNOWN	unruled
114450	Case Publicist	OUT		UNKNOWN	unruled
149487	Commercial Terrain Indépendant Freelance	OUT		UNKNOWN	unruled
158438	Community Events Staff	OUT		UNKNOWN	unruled
7067	Contract Opportunity Home Care Providers 1099	OUT		UNKNOWN	unruled
63806	Coordinateur trice des Partenariats Côte d Ivoire	OUT		UNKNOWN	unruled
148321	Deal Pricing	OUT		UNKNOWN	unruled
82281	Educational Videos Actor Australia New Zealand Freelance	OUT		UNKNOWN	unruled
48200	Entry Level Kitchen Position 40th Madison	OUT		UNKNOWN	unruled
123288	Fluent Russian Speakers US Task Based Remote Flexible	OUT		UNKNOWN	unruled
69008	Gloucester Go Outdoors	OUT		UNKNOWN	unruled
64448	Grade Setter	OUT		UNKNOWN	unruled
3441	Guest Services Staff PromoWest PA	OUT		UNKNOWN	unruled
127632	Haematology Blood Transfusion BMS	OUT		UNKNOWN	unruled
127636	Haematology or Transfusion BMS Required in Yorkshire	OUT		UNKNOWN	unruled
123743	IT Technology Decision Makers Paid ITSM ESM Research Study Liverpool UK	OUT		UNKNOWN	unruled
84889	LDN SEE 2027 Trading Research	OUT		UNKNOWN	unruled
133815	Leitender Konstrukteur für fluidtechnische Komponenten	OUT		UNKNOWN	unruled
62363	Lex Columnist	OUT		UNKNOWN	unruled
92977	Loonshot Games 콘텐츠 기획 외주 관리 3년 5년	OUT		UNKNOWN	unruled
41852	쿠팡 로켓배송 직매입MD 브랜드매니저 채용 Baby Convenience	OUT		UNKNOWN	unruled
12585	Medical Sewer I II	OUT		UNKNOWN	unruled
147554	Motorista de Distribuição CNH B Carro Barueri SP	OUT		UNKNOWN	unruled
27700	Nourish the Service NTS Event Contractor New England	OUT		UNKNOWN	unruled
157398	Nutritionist CA Licensed Fully Virtual FTE	OUT		UNKNOWN	unruled
84895	NYC Winter INSIGHT 2027 Trading Research	OUT		UNKNOWN	unruled
109862	OmniCraft Labs 시니어 캐릭터 모델링 아티스트 10년 이상	OUT		UNKNOWN	unruled
89327	Patient Account Coder	OUT		UNKNOWN	unruled
104221	Payment Center Customer Service Rep	OUT		UNKNOWN	unruled
139399	Personal Shopper	OUT		UNKNOWN	unruled
48691	Pine Street Restaurant Team	OUT		UNKNOWN	unruled
92847	Polestar Product Genie	OUT		UNKNOWN	unruled
68408	Register your C.V for upcoming positions	OUT		UNKNOWN	unruled
161814	Server	OUT		UNKNOWN	unruled
161817	Server	OUT		UNKNOWN	unruled
102946	ShortForm Creative On Site in Greenville NC	OUT		UNKNOWN	unruled
128286	Specialty Dr for adult inpatient ward in East Anglia required	OUT		UNKNOWN	unruled
93675	Staff SIE Exterior Trim	OUT		UNKNOWN	unruled
176279	Stagiair e Digital Video Audio Advertising	OUT		UNKNOWN	unruled
83616	Store Colleague JD Enschede 8H	OUT		UNKNOWN	unruled
970	Substitute and On Call Opportunities Inquire Here	OUT		UNKNOWN	unruled
69783	Tax	OUT		UNKNOWN	unruled
88558	Technicien.ne en automatisation	OUT		UNKNOWN	unruled
130429	Technischer Vertrieb b für DACH Region	OUT		UNKNOWN	unruled
17320	Template Job	OUT		UNKNOWN	unruled
90251	Test Job With Sensitive Questions	OUT		UNKNOWN	unruled
90252	Test Job With Sensitive Questions	OUT		UNKNOWN	unruled
41920	Trust and Safety Compliance	OUT		UNKNOWN	unruled
58985	Veterinary Criticialist	OUT		UNKNOWN	unruled
176322	Vil du forme fremtidens datadrevne markedsføring	OUT		UNKNOWN	unruled
84349	シニアリクルーター	OUT		UNKNOWN	unruled
115456	事業企画 リーダー候補	OUT		UNKNOWN	unruled
115377	新規事業企画 戦略 リーダー候補 チャージ関連業務 金融機関連携	OUT		UNKNOWN	unruled
```
