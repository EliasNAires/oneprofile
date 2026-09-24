# Round 15 of the engineering-role loop: the review

Issue #35, round 15, reviewed on 2026-09-24. This is the last round of #35. The implementer's rules
(see the "Round 15 implemented" comment on #35) re-classified the 179 098-row corpus. This review
draws a fresh sample of that classification, has it labelled blind, scores it and groups every
disagreement.

The labels are in `src/test/resources/labels/engineering-role-round-15.tsv`. A subagent wrote them
from the criterion and the 1000 bare titles, reading nothing else. The criterion was read at
revision `5896378`, which has no uncommitted edits. That revision already holds the eleven
decisions made after the round-14 review. The rules were read as the implementer left them: round
15's changes to `TitleClassification` are in the working tree and not yet committed.

## The numbers

The sample is 100 `IN`, 500 `OUT` and 400 `UNKNOWN` rows drawn at random from 161 685 eligible
rows. Every id held by an earlier fixture and every calibration title was excluded. A mistake is any
row whose state differs from the label.

| Gated number | Closing run (2026-10-03) | Round 13 | Round 14 | Round 15 | Gate | Met |
| --- | ---: | ---: | ---: | ---: | ---: | :---: |
| `IN` stratum error: labelled `OUT` or `UNKNOWN` | 34.00% | 30.00% | 22.00% | **12.00%** (12 of 100) | ≤ 15% | yes |
| `OUT` stratum error: labelled `IN` or `UNKNOWN` | 3.80% | 2.60% | 5.40% | **3.20%** (16 of 500) | ≤ 10% | yes |
| `unruled`, as a share of the whole corpus | 3.49% (6 257) | 2.79% (4 991) | 2.60% (4 654) | **2.60%** (4 656) | ≤ 1% | no |
| `OUT`-stratum rows labelled `IN` | 3 of 500 | 1 of 500 | 0 of 500 | **2** of 500 | ≤ 5 | yes |

The two `OUT`-stratum rows labelled `IN` are `Mission Software Engineer EW` and `Engineering
Manager Ubuntu Hardware Certification Quality and Test Engineering`.

Reported but not gated:

| Number | Closing run | Round 13 | Round 14 | Round 15 |
| --- | ---: | ---: | ---: | ---: |
| `UNKNOWN` stratum error: labelled `IN` or `OUT` | 57.25% | 44.50% | 41.00% | **31.75%** (127 of 400) |
| Rows labelled `UNKNOWN` that the rules decided | 45 | 38 | 44 | **24** (10 `IN`, 14 `OUT`) |
| The labeller's own unknown pile | 216 | 260 | 280 | **297**: 120 `domain_ambiguity`, 169 `scope_ambiguity`, 8 `unruled` |
| Corpus `UNKNOWN` pile | | | 23 083 | **26 112** |
| Laundered rows (stratum error × pile), estimated | | | ≈ 9 460 | **≈ 8 290** |

The `UNKNOWN` stratum, split by the rules' reason:

| Rules' reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |
| --- | ---: | ---: | ---: |
| `domain_ambiguity` (162) | 17 | 26 | 119 |
| `scope_ambiguity` (164) | 9 | 17 | 138 |
| `unruled` (74) | 2 | 56 | 16 |

Per reason, the laundered estimate is ≈ 3 000 of 11 268 `domain_ambiguity` rows (26.5%), ≈ 1 610
of 10 188 `scope_ambiguity` rows (15.9%) and ≈ 3 650 of 4 656 `unruled` rows (78.4%).

**Gate verdict: not met.** Three of the four gated numbers pass for the first time: the `IN` stratum
error is 12%, the `OUT` stratum error 3.2%, and two `OUT`-stratum rows are labelled `IN`. The
`unruled` share is 2.60% against 1%, unchanged from round 14. Round 15 is the cap, so #35 closes
here.

How the errors split between the rules, the labels and the criterion:

| Stratum | Rules wrong | Label wrong | Criterion silent | Error net of label errors |
| --- | ---: | ---: | ---: | ---: |
| `IN` (12) | 4 | 0 | 8 | 12.00% |
| `OUT` (16) | 9 | 3 | 4 | 2.60% |
| `UNKNOWN` (127) | 117 | 2 | 8 | 31.25% |

The `unruled` share is the one gated number the round did not move, and it is almost all non-titles
and non-English titles (group 7). 57 of the 127 `UNKNOWN`-stratum errors are that backlog.

## What the rules get wrong

Titles, the state the criterion gives them, and the sentence that decides it.

### 1. A software qualifier is missed (17 rows, labelled `IN`, rules `domain_ambiguity`)

"If a software qualifier is present and head is engineering-capable: return IN." Every one of these
names a software product, language or discipline under `engineer`, `developer` or `architect`:

- `Solutions Engineer Okta Strategic Accounts Minnesota`: `Okta` ("`Sales Engineer Data Security`
  is `IN` on `data security`")
- `Compute Operations Engineer`, `Distributed Systems Engineer`, `Staff Information Systems
  Engineer`, `Ingestion Engineer UA Remote`, `Model Optimization Engineer`, `Privacy Engineer`
- `Delivery Engineer Splunk W2PE`, `Shopify Engineer`, `MS Dynamics Engineer`, `D365 Technical
  Architect`, `Guidewire Developer US Remote` (named packages: "Under `developer` or `engineer`
  they are `IN`")
- `Haskell Developer`, `.NET Developer for Sport Math Team` (`.NET` is lost to the leading dot)
- `Trading Systems Developer`, `Engineering Manager Recommendations`
- `Access Management Engineer Duo MFA Specialist Remote in the U.S`

### 2. The technical-program family and its variants (8 rows, labelled `IN`)

"`Technical Product Manager` and `Technical Program Manager` are `IN`." The ruling covers "any rank
word", and "`technical` reads as `engineering`":

- `Technical Program Director`, `Director Technical Program Management` (rules `scope_ambiguity`)
- `Engineering Program Manager`, `Engineering Program Manager Devices` (rules `scope_ambiguity`)
- `Product Manager Technical Identity Access Management` (rules `scope_ambiguity`): `technical`
  after the head, plus a software word
- `TPM Mathematical Software Algorithms` (rules `unruled`): `TPM` is the abbreviation, and
  `software` is present
- `Technical Lead Data` (rules `scope_ambiguity`): "It is `IN` with a software word."
- `Research Scientist Machine Learning PhD` (rules `scope_ambiguity`): machine learning is code as
  primary artifact (Q2), as data science is.

### 3. A discipline marker is missed (20 rows, labelled `OUT`)

"A discipline marker decides OUT under any head … and it beats a software qualifier." Rulings do
not escape it: "A discipline marker still decides all of them `OUT`."

- Chips and electronics: `Staff Design Verification Test Engineer`, `ATE Test Engineer`, `IP
  Signoff Methodology Engineer`, `Signal and Power Integrity SI PI Engineer`, `Wire Harness
  Engineer`, `Staff Optical Communications Engineer`, `Microelectronics Industrial Base Analyst`
- Test ranges, lifts and radiation: `Range Instrumentation Trajectory Tracking Engineer`,
  `Critical Lift Transport Engineer Starship`, `Radiation Effects Associate Engineer Fall 2026`
- Manufacturing and process: `Manufacturing Engineering Intern Spring 2027`, `Associate Process
  Engineer MS&T` (`msat` is a marker, `MS&T` is not read), `Factory Test Technical Specialist I 2nd
  Shift`, `Project Manager Contract Manufacturing`
- Life science: `Flow Cytometry Scientist`, `Associate Scientist Scientist I Protein Science
  Developability`
- Field and vehicle trades: `OSP Field Engineer` and `Electrical Field Engineer` (the `field
  engineer` ruling answers before the marker), `MOT Tester`
- Civil: `Project Manager Traffic Transportation Operations`

### 4. A head or noun is misread (17 rows, labelled `OUT`)

- **Design and content roles.** "UX, UI, visual and product designers are `OUT` … `product` does not
  hold them open. Content roles are `OUT` for the same reason, even under `developer`."
  `Gameplay Design Director` (rules `IN`, `IN` stratum), `Art Director Product`, `Associate Creative
  Director AI Design`, `T3 Content and Technical Writer`, `Analista de Conteúdo`.
- **Sales and hires over a technology noun.** "What is left after the rank word is stripped can also
  be a sale or a hire … these are `OUT`." `Lead Technical Recruiter`, `Renewables Origination
  Analytics Internship Program`.
- **The noun names whom the work is for, not a technology.** Strip the rank word "and read what is
  left as a title": `Finance Manager Technology FP&A` is a finance role, `Customer Success Manager
  Public Sector National Security` a customer role, `Service Center Operations Specialist Technical
  Parts Customer Experience` a parts-service role, `Data Entry Specialist` clerical (the
  non-engineering side, like `Service Desk Agent`), `Game Night Staff Technical Lead Part time
  Seasonal` event staff. `Communications Manager Analyst Relations` is a generic head with a
  modifier and no software word (step 6a).
- **Student forms.** `Praktikum Creation` is an internship ("Meaning is read in any language"):
  student form, modifier, no software word, so `OUT`. `Quantitative Intern Summer 2027` (rules `IN`,
  `IN` stratum) is the same: `quantitative` is not a software domain.
- **A plain market under `engineer`.** `Education Engineer`: step 6, "a modifier that plainly names
  a market … is a market marker".
- **Not a vacancy.** `Head of Engineering Technical Lead Paid Research` recruits paid research
  participants (Q1).

### 5. Rows the rules decide that the criterion holds open (9 rows: 7 in the `OUT` stratum, 2 in the `IN` stratum)

- `AI Trainers United Kingdom` → **`scope_ambiguity`**. "A post that names no expertise is `UNKNOWN`
  / `scope_ambiguity`." A country is not an expertise. This is round 13's and 14's `Canada Residents
  AI Trainers` row again.
- `Lead Product Operations Manager`, `Product Pricing Consultant II III`, `Product Marketing Manager
  Competitive Intelligence` → **`scope_ambiguity`**. "A product title with no software qualifier is
  `scope_ambiguity`, whatever market marker it carries."
- `Gerente de Proyecto` → **`scope_ambiguity`**. It is `Project Manager`, and "Meaning is read in
  any language."
- `Martech Solutions Manager` → **`scope_ambiguity`**. Marketing technology is a technology noun
  under a generic head, like `Salesforce Manager`.
- `ConexSmart Technician 1` → **`domain_ambiguity`**. `technician` is not generic, so an unclassed
  modifier is "no marker at all" and the title falls to step 7.
- `Network Infrastructure Manager` (rules `IN`, `IN` stratum) → **`scope_ambiguity`**: a manager
  over a technology noun, like `IT Manager`. `Computer Systems Analyst IT I Computer Systems
  Analyst` (rules `IN`, `IN` stratum) → **`scope_ambiguity`**: it is the business systems analyst
  under another name.

### 6. `IN`-labelled rows decided `OUT` (2 rows, `OUT` stratum)

- `Mission Software Engineer EW` → **`IN`**. Electronic warfare names the market the software is
  written for, not a training the software engineer is hired on (the credential test).
- `Engineering Manager Ubuntu Hardware Certification Quality and Test Engineering` → **`IN`**. It
  manages the testing of Ubuntu on hardware: `Ubuntu` is a software word, and certifying an
  operating system on hardware is not an electrical-engineering credential.

### 7. The `unruled` backlog (57 rows)

Every row the rules left `unruled` that the labeller decided. 56 are labelled `OUT`, one `IN`
(`システム開発エンジニア …`, a system development engineer on the PlayStation platform). They split
into:

- **Not vacancies** (Q1): `I want to work at OfferZen sometime in the future`, `Welcome New Grads`,
  `Create Your Own`, `Future Employment Opportunities Brokerage`, `Possibilités d Emploi Futures`,
  `Women in Trading Chicago`, `Remote Study Participants AI Research Hamilton`, `Office Dog`.
- **Non-English titles**: Japanese (`人事企画`, `CS業務企画 業務変革推進担当`), Korean (the two
  Coupang rows), Hebrew, German (`Brandschutzingenieur`, `Maler in`, `Empfangskraft …`, `Spezialist
  Zahlungsverkehr Rechnungsstellung`), French (`Commercial Terrain …`, `Concepteur trice …`,
  `Producteur rice associé e`, `Chargé.e de projet …`, `Ambassadeur Saisonnier …`), Italian.
- **English heads no list holds**: `Locksmith`, `Cinematographer`, `Medical Coder`, `Staff Writer`,
  `Fit Model`, `Grade Setter`, `FX Structurer`, `Research Interviewer`, `Store Colleague`,
  `Production II 2nd Shift`, `Business Development Rep`, `Legal Compliance`, `MSL Mid West`,
  `Registrars SPR in Emergency Medicine …`, `Social Work Looked After Children`, and the rest in the
  archive below.

The 16 `unruled` rows the labeller also left `UNKNOWN` agree and are not errors.

## Labelling errors

Five labels contradict the criterion's text. Net of them, the `OUT` stratum error is 2.6% and the
`UNKNOWN` stratum error 31.25%.

- `Solutions Manager The Orchard` and `Contracts and Legal Systems Manager`, labelled
  `scope_ambiguity`, are `OUT`. `solutions` and `systems` are function words, "no marker and no
  software qualifier", so what stays is a generic head with an unclassed modifier: "With any
  modifier and no software qualifier, a generic head is OUT."
- `Operations Specialist III`, labelled `unruled`, is `OUT`, like "`Director of Operations`".
- `Analyste Propositions Proposal Analyst`, labelled `OUT`, is `UNKNOWN`: "Under a domain-free head a
  market marker settles nothing at all: `Treasury Analyst` and `Audit Analyst` are UNKNOWN."
- `Advisor Onboarding Program Lead Enterprise`, labelled `OUT`, is `scope_ambiguity`: "Product,
  program and project roles, under any rank word … are `UNKNOWN` / `scope_ambiguity`", and a
  ruling overrides everything.

## Questions for Elias

None are left open. Following the standing rule that criterion silences found by a review are
settled from Q1–Q4 and the credential test, the eleven below are decided and written into
`docs/engineering-role-criterion.md` (uncommitted). The rows are counted as "criterion silent" in
the table above, because the labeller read the criterion before these decisions.

1. **Words as common in industry as in software**: `infrastructure`, `storage`, `technology` and
   `quality assurance`. Is `Infrastructure Engineer` a software title? **Decided:** they are
   function words. Bare, the title is `domain_ambiguity`: `Infrastructure Engineer` (×2),
   `Infrastructure Stability Architect`, `Engineering Manager Storage Execution` (×2), `Engineer II
   Inspection Technology` (rules `IN`) and `Quality Assurance Associate` (rules `OUT`) all become
   `domain_ambiguity`. `Network Infrastructure` stays under the network-roles ruling.
2. **Algorithm development.** `Systems Engineer Algorithm Development` (rules `IN`, labelled
   `domain_ambiguity`). **Decided:** `IN`. Developing algorithms is writing code (Q2) in whatever
   domain the algorithms serve. The rules are right.
3. **Security governance.** `Cybersecurity Governance Analyst` (rules `IN`, labelled
   `scope_ambiguity`). **Decided:** a governance, risk or compliance role over a security word is
   `scope_ambiguity`, like `Security Advisor`: some read configurations and logs (Q3), some write
   policy.
4. **Strategists and educators over a software word.** `Deployment Strategist`, `OxCaml Educator`
   (rules `OUT`, labelled `scope_ambiguity`). **Decided:** `scope_ambiguity`. A forward-deployed
   strategist or a teacher of a programming language may be opened by a software background alone
   (Q4) or may need a consulting or teaching record.
5. **Postdocs.** `Postdoctoral Associate` (rules `OUT`, labelled `unruled`). **Decided:**
   `postdoctoral` and `postdoc` are student forms. Bare, the title is `domain_ambiguity`. With a
   modifier and no software word it is `OUT`.
6. **Field engineers with a software word** (the implementer's question 1). `Telco Cloud Field
   Engineer` (rules `scope_ambiguity`, labelled `IN`). **Decided:** a software qualifier makes a
   field engineer `IN`, as under `engineer`. A bare field engineer stays `scope_ambiguity`, and a
   discipline marker decides `OUT` (group 3). Likewise a title whose head is `engineer` with a
   software word (`Software Engineer Data Analyst`) is `IN`: the data-analyst ruling reads analyst
   titles, not engineer titles.
7. **`solutions` against a market** (the implementer's question 2). `Event Solutions Engineer`
   (rules `domain_ambiguity`, labelled `OUT`). **Decided:** `solutions` yields to `engineer` as
   `sales` does, and a market word beside it names the customer segment. The title is
   `domain_ambiguity`, as implemented. `Recruiting Solutions Engineer` stays open for the same
   reason.
8. **Building professions.** `Project Architect Healthcare`, `Project Architect K 12 Education`
   (rules `domain_ambiguity`, labelled `OUT`); `Project Manager Estimator Sales` (rules
   `scope_ambiguity`, labelled `OUT`). **Decided:** `project architect` and `estimator` name
   building and construction credentials: discipline markers, so `OUT`.
9. **`Controller`** (rules `domain_ambiguity`, labelled `OUT`). **Decided:** `controller` is a
   never-engineering head: in this corpus it is the financial controller. The controls engineer is
   spelled `controls`.
10. **Service engineers on a shift.** `AM Service Engineer 4x10 s` (rules `domain_ambiguity`,
    labelled `OUT`). **Decided:** `domain_ambiguity`, as the rules have it. A shift pattern is not a
    credential, and `Service Engineer` is a software title as often as a field-service one.
11. **Frontier research.** `Research Scientist Frontier Benchmarks` (rules `scope_ambiguity`,
    labelled `IN`). **Decided:** `IN`. In this corpus `frontier` names frontier AI models, as it does
    in `Research Intern Frontier Agents`, and benchmarking them is code (Q2).

The implementer's question 3 (should the `unruled` gate count non-English single-token titles and
non-vacancies) is a question about the gate, which is Elias's, and stays open. It is carried to #11.

## The disagreements

`id`, title, label, label reason, rules' state, rules' reason.

```
162247	Delivery Engineer Splunk W2PE	IN		UNKNOWN	domain_ambiguity
102854	Project Manager Estimator Sales	OUT		UNKNOWN	scope_ambiguity
155564	Lead Product Operations Manager	UNKNOWN	scope_ambiguity	OUT	
41316	Haskell Developer	IN		UNKNOWN	domain_ambiguity
20838	Computer Systems Analyst IT I Computer Systems Analyst	UNKNOWN	scope_ambiguity	IN	
136095	Production II 2nd Shift	OUT		UNKNOWN	unruled
117216	Deployment Strategist	UNKNOWN	scope_ambiguity	OUT	
70972	Access Management Engineer Duo MFA Specialist Remote in the U.S	IN		UNKNOWN	domain_ambiguity
108096	Praktikum Creation	OUT		UNKNOWN	domain_ambiguity
46499	Project Manager Contract Manufacturing	OUT		UNKNOWN	scope_ambiguity
63782	Cinematographer	OUT		UNKNOWN	unruled
114172	Art Director Product	OUT		UNKNOWN	scope_ambiguity
100813	Engineering Manager Storage Execution	UNKNOWN	domain_ambiguity	IN	
83624	Store Colleague JD Rotterdam Zuidplein 38H	OUT		UNKNOWN	unruled
178237	Product Manager Technical Identity Access Management	IN		UNKNOWN	scope_ambiguity
155270	Research Scientist Machine Learning PhD	IN		UNKNOWN	scope_ambiguity
142938	Technical Program Director	IN		UNKNOWN	scope_ambiguity
177106	Operations Specialist III	UNKNOWN	unruled	OUT	
157772	Infrastructure Engineer	UNKNOWN	domain_ambiguity	IN	
131261	Factory Test Technical Specialist I 2nd Shift	OUT		UNKNOWN	scope_ambiguity
17206	ATE Test Engineer	OUT		UNKNOWN	domain_ambiguity
47681	Hollow Metal Entry Door Installers Tampa	OUT		UNKNOWN	unruled
56754	Event Solutions Engineer	OUT		UNKNOWN	domain_ambiguity
51351	Medical Coder	OUT		UNKNOWN	unruled
136140	Production 2 4pm 8pm 20 Hours Week	OUT		UNKNOWN	unruled
174503	Inventory Kfar Saba מחסנאים כפר סבא	OUT		UNKNOWN	unruled
32901	Analyste Propositions Proposal Analyst	OUT		UNKNOWN	domain_ambiguity
64536	Guidewire Developer US Remote	IN		UNKNOWN	domain_ambiguity
129190	Compute Operations Engineer	IN		UNKNOWN	domain_ambiguity
95557	Contracts and Legal Systems Manager	UNKNOWN	scope_ambiguity	OUT	
112927	Trial Master File TMF Associate	OUT		UNKNOWN	unruled
36011	Lead Technical Recruiter	OUT		UNKNOWN	scope_ambiguity
107902	Postdoctoral Associate	UNKNOWN	unruled	OUT	
146956	Technical Lead Data	IN		UNKNOWN	scope_ambiguity
12101	Staff Design Verification Test Engineer	OUT		UNKNOWN	domain_ambiguity
87740	Possibilités d Emploi Futures	OUT		UNKNOWN	unruled
101786	Game Night Staff Technical Lead Part time Seasonal	OUT		UNKNOWN	scope_ambiguity
121941	AI Trainers United Kingdom	UNKNOWN	scope_ambiguity	OUT	
178978	Staff Information Systems Engineer	IN		UNKNOWN	domain_ambiguity
11540	Mission Software Engineer EW	IN		OUT	
37228	Concepteur trice sénior e de niveaux	OUT		UNKNOWN	unruled
19797	Engineering Program Manager	IN		UNKNOWN	scope_ambiguity
112584	Quantitative Intern Summer 2027	OUT		IN	
124813	Remote Study Participants AI Research Hamilton	OUT		UNKNOWN	unruled
138191	Digital marketing Danish Speaker	OUT		UNKNOWN	unruled
53756	Future Employment Opportunities Brokerage	OUT		UNKNOWN	unruled
48697	Rittenhouse Square Restaurant Team	OUT		UNKNOWN	unruled
60728	Martech Solutions Manager	UNKNOWN	scope_ambiguity	OUT	
82500	OxCaml Educator	UNKNOWN	scope_ambiguity	OUT	
26158	Business Development Rep	OUT		UNKNOWN	unruled
8684	Placement Client Service 2027	OUT		UNKNOWN	unruled
60025	Trading Systems Developer	IN		UNKNOWN	domain_ambiguity
115331	CS業務企画 業務変革推進担当	OUT		UNKNOWN	unruled
53840	D365 Technical Architect	IN		UNKNOWN	domain_ambiguity
47230	Analista de Conteúdo	OUT		UNKNOWN	domain_ambiguity
51162	FX Structurer	OUT		UNKNOWN	unruled
68185	Product Marketing Manager Competitive Intelligence	UNKNOWN	scope_ambiguity	OUT	
12790	Finance Strategy GTM Korea	OUT		UNKNOWN	unruled
49188	Project Architect K 12 Education	OUT		UNKNOWN	domain_ambiguity
86471	Associate Creative Director AI Design	OUT		UNKNOWN	scope_ambiguity
90501	Family Court Advise	OUT		UNKNOWN	unruled
64449	Grade Setter	OUT		UNKNOWN	unruled
64653	Addetto a al Servizio Clienti Trasferimento a Budapest	OUT		UNKNOWN	unruled
35834	Ingestion Engineer UA Remote	IN		UNKNOWN	domain_ambiguity
60959	Fit Model	OUT		UNKNOWN	unruled
90069	Associate Scientist Scientist I Protein Science Developability	OUT		UNKNOWN	domain_ambiguity
41847	쿠팡 MBA 경력직 채용 Coupang Finance Development Program	OUT		UNKNOWN	unruled
88512	Chargé.e de projet en usine procédés alimentaires	OUT		UNKNOWN	unruled
89577	Engineer II Inspection Technology	UNKNOWN	domain_ambiguity	IN	
162154	ConexSmart Technician 1	UNKNOWN	domain_ambiguity	OUT	
132279	CCO at Retail Insights	OUT		UNKNOWN	unruled
57873	T3 Content and Technical Writer	OUT		UNKNOWN	scope_ambiguity
165499	Privacy Engineer	IN		UNKNOWN	domain_ambiguity
77916	Research Interviewer	OUT		UNKNOWN	unruled
19200	Empfangskraft in Düsseldorf für hausärztliche Praxis	OUT		UNKNOWN	unruled
77713	Transmission Interconnection	OUT		UNKNOWN	unruled
30830	Engineering Manager Ubuntu Hardware Certification Quality and Test Engineering	IN		OUT	
36100	Welcome New Grads	OUT		UNKNOWN	unruled
141732	Maler in	OUT		UNKNOWN	unruled
41601	쿠팡 카탈로그 품질 검수 및 운영 프로세스 개선 2년 이상	OUT		UNKNOWN	unruled
160959	Locksmith	OUT		UNKNOWN	unruled
139569	システム開発エンジニア PlayStationプラットフォームのゲームコンテンツのオーサンリング および配信システム	IN		UNKNOWN	unruled
53162	Renewables Origination Analytics Internship Program	OUT		UNKNOWN	scope_ambiguity
46769	MSL Mid West	OUT		UNKNOWN	unruled
143216	Critical Lift Transport Engineer Starship	OUT		UNKNOWN	domain_ambiguity
69380	.NET Developer for Sport Math Team	IN		UNKNOWN	domain_ambiguity
160	Gameplay Design Director	OUT		IN	
17954	Engineering Program Manager Devices	IN		UNKNOWN	scope_ambiguity
108363	Finance Manager Technology FP&A	OUT		UNKNOWN	scope_ambiguity
12697	Customer Success Manager Public Sector National Security	OUT		UNKNOWN	scope_ambiguity
159339	Solutions Manager The Orchard	UNKNOWN	scope_ambiguity	OUT	
163122	Engineering Manager Recommendations	IN		UNKNOWN	domain_ambiguity
64365	Associate Process Engineer MS&T	OUT		UNKNOWN	domain_ambiguity
64805	Shopify Engineer	IN		UNKNOWN	domain_ambiguity
70312	Brandschutzingenieur	OUT		UNKNOWN	unruled
128287	Specialty Dr for Community CAMHS role Lincolnshire	OUT		UNKNOWN	unruled
30961	Communications Manager Analyst Relations	OUT		UNKNOWN	domain_ambiguity
115425	人事企画	OUT		UNKNOWN	unruled
48954	AM Service Engineer 4x10 s	OUT		UNKNOWN	domain_ambiguity
11973	TPM Mathematical Software Algorithms	IN		UNKNOWN	unruled
8950	Advisor Onboarding Program Lead Enterprise	OUT		UNKNOWN	scope_ambiguity
17223	IP Signoff Methodology Engineer	OUT		UNKNOWN	domain_ambiguity
120604	Investor Recruitment Danish speaking	OUT		UNKNOWN	unruled
116436	Office Dog	OUT		UNKNOWN	unruled
106584	Network Infrastructure Manager	UNKNOWN	scope_ambiguity	IN	
48733	Data Entry Specialist	OUT		UNKNOWN	scope_ambiguity
92827	MOT Tester	OUT		UNKNOWN	domain_ambiguity
149464	Commercial e Terrain Indépendant e	OUT		UNKNOWN	unruled
91910	Social Work Looked After Children	OUT		UNKNOWN	unruled
123519	Head of Engineering Technical Lead Paid Research	OUT		UNKNOWN	domain_ambiguity
108054	I want to work at OfferZen sometime in the future	OUT		UNKNOWN	unruled
62305	Wire Harness Engineer	OUT		UNKNOWN	domain_ambiguity
30589	Voice Text Chat Psychic Independent Contractor	OUT		UNKNOWN	unruled
68477	Gerente de Proyecto	UNKNOWN	scope_ambiguity	OUT	
108714	Infrastructure Stability Architect	UNKNOWN	domain_ambiguity	IN	
105635	Consumer Experience Insights Clinical Trials	OUT		UNKNOWN	unruled
100814	Engineering Manager Storage Execution	UNKNOWN	domain_ambiguity	IN	
116237	Flow Cytometry Scientist	OUT		UNKNOWN	domain_ambiguity
17379	Radiation Effects Associate Engineer Fall 2026	OUT		UNKNOWN	domain_ambiguity
93533	Service Center Operations Specialist Technical Parts Customer Experience	OUT		UNKNOWN	scope_ambiguity
131532	Staff Optical Communications Engineer	OUT		UNKNOWN	domain_ambiguity
29775	Controller	OUT		UNKNOWN	domain_ambiguity
108011	Staff Writer	OUT		UNKNOWN	unruled
3434	Guest Services Staff Agora Jacobs Pavilion Globe Iron	OUT		UNKNOWN	unruled
67654	Electrical Field Engineer	OUT		UNKNOWN	scope_ambiguity
128218	Registrars SPR in Emergency Medicine at University Hospital of Birmingham	OUT		UNKNOWN	unruled
141395	Research Scientist Frontier Benchmarks	IN		UNKNOWN	scope_ambiguity
11099	Range Instrumentation Trajectory Tracking Engineer	OUT		UNKNOWN	domain_ambiguity
32422	Spezialist Zahlungsverkehr Rechnungsstellung	OUT		UNKNOWN	unruled
115662	OSP Field Engineer	OUT		UNKNOWN	scope_ambiguity
35596	Systems Engineer Algorithm Development	UNKNOWN	domain_ambiguity	IN	
149508	Commercial Terrain Indépendant Freelance	OUT		UNKNOWN	unruled
15656	Infrastructure Engineer	UNKNOWN	domain_ambiguity	IN	
174707	Project Architect Healthcare	OUT		UNKNOWN	domain_ambiguity
85344	MS Dynamics Engineer	IN		UNKNOWN	domain_ambiguity
30800	Distributed Systems Engineer	IN		UNKNOWN	domain_ambiguity
16630	Manufacturing Engineering Intern Spring 2027	OUT		UNKNOWN	domain_ambiguity
121048	Create Your Own	OUT		UNKNOWN	unruled
171672	Legal Compliance	OUT		UNKNOWN	unruled
25635	Director Technical Program Management	IN		UNKNOWN	scope_ambiguity
177817	Product Pricing Consultant II III	UNKNOWN	scope_ambiguity	OUT	
88381	Project Manager Traffic Transportation Operations	OUT		UNKNOWN	scope_ambiguity
46800	Signal and Power Integrity SI PI Engineer	OUT		UNKNOWN	domain_ambiguity
15635	Education Engineer	OUT		UNKNOWN	domain_ambiguity
31044	Telco Cloud Field Engineer	IN		UNKNOWN	scope_ambiguity
72974	Quality Assurance Associate	UNKNOWN	domain_ambiguity	OUT	
96182	Women in Trading Chicago	OUT		UNKNOWN	unruled
4869	Cybersecurity Governance Analyst	UNKNOWN	scope_ambiguity	IN	
83625	Store Colleague JD Venlo 38H	OUT		UNKNOWN	unruled
9793	Microelectronics Industrial Base Analyst	OUT		UNKNOWN	domain_ambiguity
130670	Temp Turns Help	OUT		UNKNOWN	unruled
37230	Producteur rice associé e	OUT		UNKNOWN	unruled
94383	Ambassadeur Saisonnier Fairview Pointe Claire	OUT		UNKNOWN	unruled
133479	Model Optimization Engineer	IN		UNKNOWN	domain_ambiguity
108512	Solutions Engineer Okta Strategic Accounts Minnesota	IN		UNKNOWN	domain_ambiguity
```
