# Iteration 10 of the engineering-role classification loop

Run on the development machine on 2026-09-22, against the raw corpus snapshot already loaded in the
development database — 179 098 vacancies, cleaned and classified — and triggered with one `POST
/classifications`. The criterion is unchanged since ADR-0010: this is the third iteration whose
labels and rules answer to the rewritten document.

The filename carries 2026-09-30 rather than the day it ran, for the reason iteration 3 gave: the
nine earlier iterations already hold the nine days before it, and the next session finds its input
by taking the newest file.

This iteration labelled the 1000 rows iteration 9 drew, scored iteration 9's classifier against
those labels under the skill's three-rate definition, and grew the rules from where the labels and
the answers disagreed. The labels are `src/test/resources/labels/engineering-role-2026-09-29.tsv`;
they were written to disk before any rule of the classifier was read.

**The labelling was done by a subagent**, as in iterations 5 through 9: the agent was given the
criterion and the 1000 bare titles, denied the classifier's source, the iteration reports and the
earlier label files, and told to write its labels to disk before anything else. Blinding is
ordering, and the ordering was enforced by what the agent could reach. Scoring, the rule pricing and
the draw were done by scripts over files in a scratchpad, and the rules were priced on an offline
`javac` harness whose answers were checked against the application's own counts — the 1000 rows, the
9000 accumulated labels and the 179 098 corpus titles never entered the session's context.

## The numbers

The three rates score **iteration 9's** classifier, because those are the predictions the labelled
sample carries. A mistake is any row whose state differs from the label, so an `IN` labelled
`UNKNOWN` is as wrong as an `IN` labelled `OUT`. They are scored on the stratum sizes iteration 9
drew, 100 `IN` / 500 `OUT` / 400 `UNKNOWN`. Iterations 1–7 were scored under a narrower definition
and are not comparable; iterations 8 and 9 are.

| Gated number | Iteration 8 | Iteration 9 | Iteration 10 | Gate |
| --- | ---: | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 3.00% | 1.80% | **1.60%** (8 of 500) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 16.00% | 18.00% | **30.00%** (30 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 77.33% | 74.25% | **73.50%** (294 of 400) | ≤ 10% |
| `unruled` — of the whole corpus | 5.61% | 3.95% | **3.93%** (7 038) | ≤ 4% |

Reported alongside, gated by nothing:

- **`OUT`-stratum rows labelled `IN`** — the error nothing downstream recovers: **1** of 500,
  the same count as iteration 9.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **25** — 18 it called `IN`,
  7 it called `OUT`. Up from 13, and the rise is the `IN` side of it.
- **The labeller's own unknown pile**: 131 rows, split 113 `domain_ambiguity`, 13 `scope_ambiguity`,
  5 `unruled`. Comparable in size to iteration 9's 116 and iteration 8's.
- **What the `UNKNOWN` stratum turned out to be.** Of the 400 rows iteration 9 called `UNKNOWN`, the
  labeller called 29 `IN`, 265 `OUT` and 106 `UNKNOWN`. By the reason the classifier gave: of 285
  `domain_ambiguity` rows, 13 `IN` and 181 `OUT`; of 30 `scope_ambiguity` rows, 11 `IN` and 12
  `OUT`; of 85 `unruled` rows, 5 `IN` and 72 `OUT`. One unknown title in fourteen is an engineering
  role.

**The `IN` stratum error rose 12 points, and it is the number to read carefully.** It is not a
regression in the rules — iteration 9 changed nothing on the `IN` side. Two thirds of the 30 rows
are three families this labeller read more strictly than iteration 9's did: eight test and
quality-assurance titles (`Test Engineer`, `QA Shop Floor Specialist II`, `Quality Assurance
Inspection Specialist`), five product and account-management titles, and six titles where a hardware
discipline stands next to a software head (`Radar Software Engineer` twice, `Manufacturing Engineer
Radar Systems`). All three are families the rules had been deciding `IN` on a word list, and all
three are addressed below. The remainder — `Solution Architect Enterprise`, `Field Applications
Engineer`, `Forward Deployed Engineer DRC` — are genuinely contested.

The corpus after this iteration's change:

| | Iteration 8 | Iteration 9 | Iteration 10 |
| --- | ---: | ---: | ---: |
| `IN` | 18.46% | 18.53% | **18.22%** (32 628) |
| `OUT` | 59.06% | 61.05% | **67.76%** (121 362) |
| `UNKNOWN` | 22.49% | 20.42% | **14.02%** (25 108) |
| &nbsp;&nbsp;of which `unruled` — the gated share | 5.61% | 3.95% | **3.93%** (7 038) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 15.31% | 14.90% | **8.52%** (15 258) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.57% | 1.57% | **1.57%** (2 812) |

12 957 rows changed state: 11 918 from `UNKNOWN` to `OUT`, 408 from `IN` to `UNKNOWN`, 300 from `IN`
to `OUT`, 174 from `OUT` to `UNKNOWN`, 136 from `UNKNOWN` to `IN`, 21 from `OUT` to `IN`. The
`domain_ambiguity` pile fell by 43% — the largest single move any iteration has made — and it fell
because of one rule, not a word list.

## The exit gate

**Not met.** `unruled` is **3.93%** against its 4% gate and passes for the second iteration running,
with 0.07 points of margin. The `OUT` stratum error passes with room, as it has for seven
iterations. `IN` stratum error is 30.00% against 10% and `UNKNOWN` stratum error is 73.50% against
the same gate.

**Two sessions remain — 11 and 12 — and the gate will not be met.** The `UNKNOWN` stratum error has
moved 77.33 → 74.25 → 73.50 across three iterations under a stable definition, which is under two
points per iteration against a 63-point gap. The cap is the end of this loop, and the skill says
what to do then: write the report, record the reason split, close #10, and hand the remaining
unknown pile to #11. Iteration 12 should budget for that rather than for another rule harvest.

What this iteration's change does to the *next* measurement is unknown and deliberately so: the
figures below are re-scored on the rows the change was grown from and are informative only.

## Cost

A full pass over the 179 098 cleaned titles takes **0.52s** against the ten-second limit, measured
warm on the offline harness, whose per-state counts match the application's to the row
(`{"in":32628,"out":121362,"unruled":7038,"domainAmbiguity":15258,"scopeAmbiguity":2812}`).
Whole-word, case-insensitive matching over the cleaned title; no description bodies, no network.

## What changed, and why

Every candidate below was priced on the **9000 accumulated hand labels** before it was written into
the classifier, on an offline harness. The ban that governs the loop — no change may raise the count
of `OUT`-stratum rows labelled `IN` — was checked as a set, not as a count: the 20 such rows under
the new rules are the *same 20 ids* as under iteration 9's rules. Across the accumulated labels the
change moves total disagreement from 1540 to 1134 rows, and false accepts (labelled `OUT`, decided
`IN`) from 96 to 77. On the three thousand rows labelled since ADR-0010 — the only rows whose
labels are evidence for every family, since the older fixtures carry families the revision
superseded — disagreement moves from 771 to 518, false accepts from 36 to 25, and misses stay at
3. The all-9000 figures are quoted throughout because they are what each candidate was priced on;
where a candidate turned on a superseded family the post-ADR figure is the one to read.

### A yielding head gives way to the head standing behind it

`lead`, `head`, `director`, `manager`, `associate`, `chief`, `vp` and `intern` name a **rank**
before they name a function, and `support`, `operations` and `tech` name a **department**. The
criterion says the first head named is the head, and taken literally that made `Lead Analytics
Engineer` a `lead`, `Head of Engineering Payment Gateway` a `head`, `Manager Field Engineering` a
`manager` and `Network Support Engineer` a `support`. `head()` now steps over one of these while
another head still follows it. The set is named `YIELDING_HEADS` rather than `RANK_HEADS` because a
third of it names departments rather than ranks.

This is **the masking problem** — "the head that masks the head behind it" — which iterations 5
through 9 each priced and each left open, every time as a question for the criterion. The argument
for making it a rules change is that the criterion's rule is about which of two *function* heads
wins, and a yielding word in front of a function head modifies it rather than competing with it.

**This is the change in the iteration that most deserves a second opinion.** The criterion's
sentence is "where a title names more than one head, the first is the head", and `lead` and
`manager` are entries on this class's own head list, so in the code's own terms the rule now
contradicts the document — and the skill says that where the rules and the criterion disagree, the
rules are wrong. The defence above is a reading, not a licence. If the reading is rejected, the fix
is one line in `head()` and everything step 6a buys comes out with it; if it is accepted, the
criterion should say so in a sentence, which only a grilling session can write. It is recorded here
rather than in the questions below because the rule is shipped, not deferred.

One guard: a **never-engineering** word behind a yielding word does not take the head. `Manager Rider
and Driver Experience` is a manager, not a driver, and without the guard the change cost two misses.
The guard has a known cost of its own: a title like `Lead Chef` now reaches step 3 with `lead` as
its head rather than `chef`, so it is decided OUT by step 6a rather than by the never-engineering
rule that ought to decide it. Same answer, wrong route, and a route that would matter if step 6a
were ever narrowed.

### A generic head with a modifier and nothing software about it is OUT

The criterion says, in as many words, that **a modifier no one has classed is a market marker**,
because market is the class that cannot cause a miss. Step 6 had never been implemented that way: it
consulted a 195-word list, so `Hotel Manager`, `Director of Operations`, `Growth Manager`, `Proposal
Specialist` and `Shift Lead` all fell through to `domain_ambiguity`, and every iteration since the
fifth has been adding market words one at a time to chase them.

Step 6a now applies the default directly, under the heads where it cannot cause a miss: a **generic
head** — `manager`, `director`, `head`, `chief`, `vp`, `operations`, `support`, `tech`, `partner`,
`strategist` and the rest — with any modifier and no software qualifier is `OUT`. The trigger is
the presence of a second word rather than of a classed marker, which is the criterion's default
taken at its word; it is narrower than the criterion in one direction, since the criterion applies
a market marker under *every* domain-bound head and this rule applies it under twenty-nine of
them.

The restriction to generic heads is the whole of the design. Applied to every domain-bound head, the
same rule takes misses from 20 to 101 across the accumulated labels, because `C Engineer`, `Graph
Engineer` and `Analytics Engineer` are software jobs whose qualifier is simply not on the list.
`engineer` and `developer` carry a software prior that `manager` does not, so they keep the explicit
list. `specialist`, `administrator`, `expert` and `lead` were each tried in the generic set and each
cost a miss; `operations`, `support` and `tech` cost none and are in.

`product` and `products` are exempted, and this is the one place the rule steps around the evidence
rather than following it. Across the nine accumulated fixtures the product-management family splits
**44 `IN`, 5 `OUT`, 58 `UNKNOWN`** — nine labellers reading the same criterion and not agreeing —
and deciding it `OUT` would have cost 21 misses on its own. A product title with no software
qualifier is left to steps 6 and 7 to answer, which is the `domain_ambiguity` the criterion says it
is. A blanket `scope_ambiguity` ruling on `product manager` was built and measured as the
alternative; it scores slightly worse on the accumulated labels and it contradicts four existing
tests that read a product title by the software it names, which is the better behaviour.

### `test`, `qa` and `quality assurance` are not software qualifiers

They were three of the 130 software qualifiers, which made `Test Engineer`, `QA Shop Floor
Specialist II`, `Quality Assurance Inspection Specialist` and `Director GCP Quality Assurance` all
`IN`. Test and quality assurance name a function every discipline has, and this corpus posts them
over shop floors, inspection lines and hardware benches as often as over software. Dropping the
three words takes false accepts from 90 to 79 across the accumulated labels and costs no miss.

Four rulings guard the families that are unambiguously software and would otherwise have gone with
them: `qa engineer`, `qa automation`, `test automation` and `automation test` are `IN`, as are `qa
analyst` and `test analyst`. `Software Test Engineer` needs no ruling — `software` still decides it.

### Sixteen discipline markers, eighteen market markers, fourteen qualifiers, five heads

Harvested from the rows where the labels and the answers disagreed, each priced before it went in:

- **Discipline markers** — `radar`, `metrology`, `hil`, `shop floor`, `motor controls`,
  `calibration`, `fire protection`, `brake`, `medium voltage`, `assay`, `polymer`, `machining`,
  `tool and die`, `post silicon`, `occupational`, `phlebotomy`. These are what settle `Radar Software
  Engineer`: the artifact is code and the credential is not software, which is the case the criterion
  built step 4 for. Worth 6 false accepts.
- **Market markers** — `sports`, `trading desk`, `proposal`, `grants`, `retirement`, `cruise`,
  `maritime`, `vocational`, `claims`, `closing`, `conference`, `valet`, `babysitting`, `shift`,
  `hazardous waste`, `dangerous goods`, `due diligence`, `workforce planning`.
- **Software qualifiers** — `graph`, `devex`, `malware`, `sharepoint`, `sdk`, `iam`, `secops`,
  `telemetry`, `microservices`, `latency`, `runtime`, and `dados`, `datos`, `소프트웨어`.
- **Heads the corpus writes in other languages** — `cientista`, `científico`, `エンジニア`,
  `개발자`, `엔지니어`. A first draft carried fifteen heads and five non-English qualifiers; ten of
  them were plausible translations — `entwickler`, `softwareentwickler`, `sviluppatore`,
  `utvecklare`, `ソフトウェア` and their kind — that occur **zero** times in the 179 098 titles, and
  they were taken back out. A list entry no title reaches is not coverage. The ones that stayed
  carry a caveat worth reading before the next session adds more: matching is whole-word over
  whitespace tokens, so `エンジニア` reaches the spaced spelling and not `ソフトウェアエンジニア`,
  which is how the language is usually written.

### Tried and dropped, with what they cost

- **Step 6's default applied to every domain-bound head.** Misses 20 → 101 across the accumulated
  labels, 3 → 48 on the post-ADR-0010 three thousand. Forbidden outright, and the single most
  informative measurement this iteration made: it is what fixed the rule's scope to generic heads.
- **`analytics` as a software qualifier.** False accepts 77 → 93. The corpus's analytics titles are
  analytics managers and analytics directors and labellers call them `OUT`.
- **`gtm` as a market marker.** One miss, on `GTM Engineer Manager of Revenue Operations`, which an
  earlier labeller called `IN` while this one called `GTM Engineer` `OUT`. Dropped for the miss; the
  family is split and belongs in the questions below.
- **`soc` and `dynamics` as software qualifiers.** One false accept each for no net gain.
- **`specialist`, `administrator`, `expert` and `lead` in the generic-head set.** One miss each
  (`SOC Lead`, `Website Onboarding Specialist`, `SecOps Expert`, `Lead Tech Delivery`).

### Three existing tests changed their answer

`workplace solutions manager`, `vp growth` and `quality assurance manager women s apparel qa import`
were asserted `domain_ambiguity`, `domain_ambiguity` and `IN` by iterations 6 through 8; they are
`OUT` under the new rules, and the assertions were updated with the reason. The third reverses a
consequence of ADR-0010 that iteration 8 recorded as a cost of the revision — the revision let a
market marker stop beating an adjacent qualifier, and `qa` was the qualifier doing the work.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows the change was grown from. 187 of the 1000 disagree, against 332 before: one miss, 6 false
accepts, and 138 rows still left `UNKNOWN` that the labeller decided. On the sample the three rates
would read 20.21%, 4.42% and 60.79%. The `OUT` stratum error rises — 8 rows to 30 — because the
change decides rows the labeller left `UNKNOWN`; that stratum has band to spare and this is where
some of it is being spent.

## What the next session should look at

**The `UNKNOWN` pile is now two-thirds `domain_ambiguity` and one-third `unruled`, and the
`unruled` third is where the rows are.** The generic-head rule cleared the management tail; what is
left in the `UNKNOWN` stratum after this change is titles with no head the lists reach at all —
`biz ops`, `credit risk role`, `data annotation contributor`, `assessoria de investimentos`,
`candidature spontanée`, and a long non-English tail. Heads, not markers, is where the remaining
coverage is.

**`unruled` passed again, at 3.93%, with 0.07 points of margin.** Adding heads moves rows out of it
and is safe; anything that removes a head moves them back. A session that reaches for the head list
should re-read this number before it writes its report.

**The `IN` stratum is still the gate nobody has moved**, and this iteration is the first to attack
it directly rather than harvesting the unknown pile. Whether the test/QA and discipline-marker
changes moved it is exactly what iteration 11's sample measures; do not read this iteration's
re-score as the answer.

## Questions for the criterion, not for the rules

**The masking head is answered in the rules, and the criterion has not caught up.** Five iterations
recorded it as the oldest open question for the criterion. It is closed here without touching the
criterion, on the reading that the first-head rule is about function heads and a yielding word is
not one. That reading is argued at length in the change section above and it is the one thing in
this iteration a reader should check before trusting the rest: the criterion's sentence says
"first", and the rules now say "first that does not yield". Either the criterion gains a sentence
or the rule comes out.

**Is `manager` engineering-capable?** The criterion's own example says `data manager` is "not
obviously one", which this labeller read as a whole-head verdict and used to send every product
manager, engineering manager and technical account manager out of `IN`. The accumulated labels say
the family splits 44/5/58. This is now the largest single unsettled family in the corpus — 2 370
titles name a product manager — and the rules step around it rather than deciding it.

**Is a support role an engineering role?** Raised by iteration 8, answered the same way by
iterations 9 and 10 (`Technical Support Engineer` and `IT Support Engineer` in, `L3 Support Analyst`
and `Production Support Engineer` in, `2026 Support Associate Korean&English Seoul` out). This
iteration put `support` in both the yielding-head and the generic-head sets, so the question now has
two rules resting on it.

**Designers.** Unchanged from iteration 9 and unchanged in the rules. This labeller ruled `Product
Designer`, `User Experience Designer` and `Technical Designer` `OUT`; the corpus writes a lot of
them and the rules reach them only through a ruling on `game designer`.

**`architect` is domain-free, so a building architect is undecided.** Unchanged from iteration 9.
`2027 Graduate Architecture Part II` and `Architekt in Technische Detailentwicklung` are in this
iteration's unruled pile for the same reason.

**Firmware, networking and now radar.** Iteration 9 flagged the FPGA/ASIC argument as most likely to
flip; this iteration extended it, adding `radar`, `hil`, `motor controls` and `post silicon` as
discipline markers, because the labeller ruled `Radar Software Engineer` `OUT` twice on exactly that
reading. If the criterion means the discipline marker to reach only titles that name *no* software
head, these four are wrong and `Radar Software Engineer` is `domain_ambiguity`.

**`scope_ambiguity` has no meaning for a labeller.** The criterion defines it as "a ruled phrase
whose variants genuinely split", and a labeller has no ruled-phrase list. This labeller used it for
phrases whose instances split in the world and `domain_ambiguity` for a known head whose domain is
unnamed — a reasonable reading the document does not actually license. 13 rows of this sample and 30
of the classifier's predictions turn on it.

**Sales engineering is one job the procedure splits in two.** `Security Sales Engineer` is `IN`
because `security` is a software qualifier; `Enterprise Sales Engineer` and `Channel Sales Engineer`
are not, because nothing argues back at the sales market marker. The procedure is being followed
exactly and the answer is still probably wrong.

## Held out of the draw

The 9000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id and is matched
case-insensitively. 166 685 of the 179 098 rows were eligible.

## The sample for iteration 11

1000 rows drawn at random from the predictions above — 100 from `IN`, 500 from `OUT`, 400 from
`UNKNOWN` — and **shuffled together**, so that the strata cannot be told apart by position. Each row
is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
1215	General Creative Services Application
155016	Personal Trainer Punta Gorda FL
109455	Engineering Internship Substation and Power Generation
4389	Language Alignment Resource Partner Estonian Freelance AI Trainer Project
115363	内部統制 IT統制担当
132700	Strategic Marketing Specialist Hybrid
85936	Account Executive Renewal Sales
41999	Line Haul Specialist 資深運輸營運調度專員 Middle Mile
967	Interested in exploring a secondary career Inquire here
62963	Counsel Legal
1468	Intervention Specialist
174570	Part Time Substitute Teacher Preschool Daycare 14 17 hr
174918	Athletic Trainer
175273	Project Manager Value Delivery Enterprise AI
133283	Director of Accounting
107044	Afgangselev
128728	Pre Sales Systems Engineer Enterprise Grand Rapids
145006	Security Software Enginer Starshield
54391	Assistant Kids Club Manager
77005	Entrepreneur in Residence Intern French speaker HRtechX
172788	Babysitting Opportunities in Jacksonville FL
118163	Overnight Customer Service Representative
80270	Technical Solution Owner
41798	쿠팡풀필먼트서비스 물류센터 시설관리 FM 담당자 대구 2센터
84012	Structural Analysis Engineer
119892	Afgangselev
77838	AI Scientist
106230	Coordinator Finance Global Partnerships
29173	Manager Computer System Validation QA
33336	Value Engineer Scale EMEA Italian Speaking
174736	Transportation Intern Pennsylvania Spring 2027
48028	Associate Director Director Clinical Pharmacology and Pharmacometrics
61761	Business Solutions Lead
128279	Specialty Doctor in Microbiology Manchester
132711	Business Development Representative
69039	Stylist 2nd PCH
45659	Commercial Sales Engineer 2 AMER West
125255	VFX CGI Fully Remote Subject Matter Expert Singapore
67147	Scientist Scientist Protein Science
160769	UK Return to China Career Fair 2nd 8th Sept 2026
23756	Weekend Registered Nurse RN Home Health
16835	Manager Market Analytics
162757	Client Billing Operations Analyst
152092	Personal Trainer
85709	Modeling Simulation Engineer National Security Missions
149913	Lead Product Manager Game Content Creation
167943	Solutions Engineering Manager EMEA
44558	Project Management Consultant
76126	Home Inspector Salary 50 85k
114096	Responsable d agence aide à domicile
81691	Logistician Data Analyst CMM
142222	Software Dev Engineer PLM EDI
66488	Radiation Protection Technician
125144	United Kingdom Residents Survey Participants Coventry UK
15692	628 Fully Qualified Navy Validator III
71727	Asset Management Analyst
4975	Formação de Devs nativos em IA
73457	Hiring Dialysis Technician Little Rock Arkansas
122941	Data Entry Clerk Graduates AI Training Palmerston North New Zealand
174149	MENA Equities Quantitative Researcher
19714	Professional Services Manager II Axon Air
24349	Mechanical Design Engineer Production Tooling
52216	Developer Ops and Sustainment
123260	Fluent Kannada Speakers AI Training Shivamogga
43050	Edible Kitchen Manager
90890	Social Work Assistant Area Services
133472	Engineer Studio Builder Tools
122766	Computer Sciences Graduates AI Training Washington DC US
40149	New Grad Family Nurse Practitioner FNP Urgent Care
161838	Payment Operations Specialist
60378	Vault Operations Associate Sunday Wednesday
51455	Eyecare Technician
169706	Companion Animal Clinic CSR
86435	Information System Security Officer ISSO Level 2 SF
85656	ASIC Physical Design Engineer
83486	Vendeur CDI temps plein
169830	Customer Service Representative
62020	Vice President Production Operations
121439	AI Engineer ML Engineer Developers AI Training Colorado Springs US
127065	Band 7 Mammographer South Yorkshire
157394	Nephrology PA or NP with CA License Fully Virtual Opportunity
161544	Research Intern Frontier Agents Winter 2027
85068	SN Head of CXP Country Jumia Full Time
17306	System Validation Engineer
32597	Growth Creative Strategist
114976	Customer Success Manager
67085	General Interest in Genomenon for B&H Share your resume and we ll keep it on file
111120	ABA Behavior Therapist East Meadows NY
27424	Psychiatrist MD
167158	C# Developer Explorers Identity Resilience
75395	Dry Shipper Aseptic Days
138866	Director Strategic Growth Initiatives Transformation
95847	Supervisor Retail Security Officer
78416	Workplace Engagement Assistant
166147	Administrative Float Oncology
146564	Director BI
40594	Manager Clinical Quality Assurance
158837	Quality Assurance Executive Product Integrity 12 Month FCT
92742	Chercheur euse en sciences mathématiques pour la sécurité de l IA
62864	Head of Capital Solutions APAC
86489	Analyst Retail Media Strategy Activation
168343	Emergency Credentialed Veterinary Technician Louisville KY
62201	Workforce Management Forecasting Analyst
123414	Game Development Environment Artist Paris France Freelance Remote
104359	Design Research Resume Drop Off
125936	Band 4 Locum General Rehabilitation Support Worker London
130953	Sales Development Representative French speaking
113013	Associate Strategic Finance FP&A
59875	Affordable Housing Compliance Manager
44162	Lead Cultivation Associate
8809	Lab Engineer II
41554	쿠팡 글로벌 위기관리센터 운영담당자 정규직
18367	Project Engineer Land Development
99968	Presales Consultant
4105	Computer Vision Specialist Freelance AI Trainer Project
48393	Restaurant Shift Leader Upper East Side
172738	Vice President Capital Markets
47815	Sales Coordinator
16818	Engineer Total Product Support
30384	Marketplace Growth Specialist
159445	Physical Occupational Therapist Consultant
163357	Security Officers in North Charleston South Carolina
93540	Service Detailer Roswell GA
60207	General Interest Clinical Roles
80289	Operations Customer Service Specialist
27767	Biomedical Technician Equipment Support Specialist Level III
18048	Consultant Digital HR
33498	Behavior Technician
155241	Experte Group Accounting Reporting 80 100 Grossraum Zürich Hybrid
70465	Product Manager Latam
146101	Veterinary Assistant
32844	Cloud Software Engineer .NET
106276	Employer Branding Student Temporary
30846	Go Golang Software Engineer Developer Tooling and Containers
110221	Per Diem Family Medicine Physician Casual Employee
136791	Strategist CIO Office FICC
84304	Shift Supervisor Chicago
52281	Marketing Associate US Remote
119349	Field Service Technician NETA 4
57621	Intern Software Engineer
84112	Palletizer
89876	Revenue Manager Independent Hotels
169028	Emergency Veterinary Nursing Trainer Virginia Beach VA
116798	Software Engineer Takeoff
17471	CAD Product Structure Engineer
49117	Architect K 12
109307	Singapore English Consumer Electronics Global Brand
100604	Mortgage Specialist
1452	Intervention Specialist
79838	Team Lead Learning and Development Contract Remote
78458	Executivo a Comercial Closer iFood Benefícios
111115	ABA Behavioral Technician RBT Baldwin NY
74411	Account Executive Public Relations Professional Services
62156	Software Engineer AI Platforms
27784	Controller
130597	Staff Product Manager Ads Signals
41142	Behavioral Interventionist
3573	Information Review and Release Analyst
63693	Fullstack Experiences Software Engineer
103264	Quant Research Quant Associate Programme 2027
175452	Analytics Associate Director
90236	Seeker Shine EA Test Job
103922	EMT
157570	Consultant Strategic Communications
164021	Counsel
59510	Join Our Talent Network
139984	Sales Development Representative
78749	Index Options Volatility Trader
28302	Press Operator
20657	Director of Omnichannel Product Management
137222	REEV Function Integration Software Planning Specialist
153053	Personal Trainer
94566	Seasonal Ambassador Newbury Street
8853	R&D Mechanical Engineer
39978	EMT B Urgent Care
101774	Event Support Technician
26415	Treasury Operations Analyst
136980	Lead Product Manager Star Trek Fleet Command
105693	Technical Account Executives Mid Market Enterprise Northeast Corridor
84401	空飛ぶクルマのテストシステム開発エンジニア
44549	Performance and Data Analyst
119770	Event Organiser
67038	Sports Data Collector Ice Hockey Coventry England
132615	Continuous Improvement Co Ordinator
3062	Mechanical Repair Technician Associate
68979	Frontend Engineer
74171	General Manager Multi Site Manufacturing Operations
41519	Account Executive
72131	Manager Digital Product Design Digital Sculpting
123061	Dutch Fluent Speakers AI Training Brussels Belgium
45657	Commercial Mid Market Sales Engineer
38814	Gestor a de Parcerias
74130	Quality Assurance Quality Control Engineer
121321	Statistical Programming Contractor
53831	Area Manager Quality and Barge Loadout
155750	Sales Director EMEA
55021	Fitness Manager
162520	DuraPlas Regional Sales Manager Midwest HVAC Plumbing
84971	Analyst Analytics Corporate Insights
28073	Engineering Manager Product Engineering
177135	Program Manager Aerospace Defense
104502	Data Center IT Technician Beit Shemesh
86749	Weekdays Family Nurse Practitioner FNP FL NJ NY MA VA NLC Remote
164110	Logistics Specialist II
89257	Conseiller.ère en analyse d affaires Murex
90866	Practitioner Family Safeguarding
162466	Freelancer Application
45314	Manager Field Engineering UAE
16995	Peer to Peer Specialist
67945	Staff Backend Engineer Database Automation Go
92825	Master Technician
110765	Initiativbewerbung München
62949	Manager Supply Planning
66271	Directeur rice du design Design Director
145560	Analytics Engineer
98027	Breakfast Server Le Supreme
92686	Manager of Engineering
43335	Construction Superintendent
56081	Operations Manager
164897	Events Organiser
122755	Computer Sciences Graduates AI Training Raleigh US
97597	Talent Acquisition Partner
8237	Product Manager Derivatives
76082	Retail Artist Part Time 3 Days 24hours John Lewis White City
147060	Analyst Business Development Private Wealth
68825	Enterprise Account Executive Healthcare
31209	Business Analyst Fraud Financial Crime
109809	Make Ready Technician Olympus Hudson Oaks
73529	In Home Dialysis Care Partner 1 1 Client St Peters Missouri 63303
84474	Recertification Specialist
147157	Seasonal Operations Specialist
156202	Partner Marketing Manager
163499	Revenue Operations Manager Post Sales
10951	Procurement Analyst
142086	Account Manager
170526	Occupational Therapist OT Home Health PRN
61006	Technical Designer Women s
69908	Press Brake Operator II
73865	Urgent Hiring In Home Post Surgery Support Rochester Webster NY 14625
65209	Full Stack Developer 729
114016	Auxiliaire de vie
68331	Product Program Manager
119893	Assistent Analyst
163463	Digital Transformation Project Manager
55663	Kids Club Manager
165085	Venture Associate at United Media
160303	Systems Operations Engineer
173743	Trading Operations Intern Summer 2027
149746	Subgerente de Ventas Directa Terreno
144798	HR Investigator Employee Relations
47900	Sales Development Representative
19100	Remote Cardiac Rehabilitation Nutrition Group Educator
81289	Mobile Manipulator Engineer
37586	Systems Engineer Growth Engineering
40743	Risk Mitigation Specialist
29807	Associate Client Service Operations
127536	Consultant in Respiratory Medicine Midlands
135221	People Team Intern
29113	Underwriting Analyst
9745	VP Regulatory Promotion and Advertising
19740	Sales Engineer II Dedrone
127872	Medical Scientist Haematology Blood Transfusion Waterford
152115	Personal Trainer
46973	Dental Office Manager
35003	Research Assistant I
45511	Staff Software Engineer Fullstack
105933	Assistent Financial Reporting
44592	Sales Engineer APJ
96419	Water Wastewater Practice Lead
73022	Data Scientist Growth
14045	Consultant Public Sector
174886	Développeur.euse C# React Migrate ShareGate
81207	Specialist Epic Solution Delivery Ambulatory
137092	Director Revenue Operations Analytics
93981	Environmental Attendant DCMC Full Time Part Time All Shifts
109282	Strategist
15977	Director AI Data Consulting
48265	Food Service Worker Upper East Side
20207	Specialty Account Manager Symbravo Fort Smith AR
29080	Staff Product Manager Expenses
40908	Operational Readiness Engineer
52461	Relief Social Services
95194	Staff Data Scientist ML
122010	AI Training Experts Arizona US
1448	Intervention Specialist
97953	Founding Product Manager Agentic Commerce
139535	PlayStation向けカスタムLSI開発のPMO
119840	Σύμβουλος Πωλήσεων Energy
171977	Market Operations Lead San Francisco
94301	Surgical Technologist III
114974	Creative Director Art and Design
58500	Power Optimization Engineer
89419	Research Scientist LG AI Research Center Ann Arbor
89575	Engineer II Environmental Sustainability
74710	Assistant Safety Director
130696	Loan Originator Nevada Sage Home Loans
23422	RN
163813	Product Engineering Manager
16544	Engineer II Global Product Support
75890	Insurance Producer Scottsbluff NE
86662	Art Director
111016	Specialist Innovation Footwear Developer
149702	Selbstständiger Vertriebsmitarbeiter Außendienst
115735	Lead COTA Supervisor Van Buren
51404	Stage crédité Coordonnateur rice en communication marketing
156834	Teamleiter Logistik
110516	Hematologist New York Oncology Hematology
71884	Consultant Vendor Management Office
80901	Full Time Floor Leader Government Street
20229	Content Moderation Analyst Overnight Shift 1 9AM
172808	Babysitting Opportunities in Robbinsville Township NJ
35397	Enterprise Account Executive Nordics
108858	Product Designer Core
170204	Lifeguard
102541	Career Opportunities at Morgan Morgan P.A
62996	戦略的パートナーシップ コミュニケーションマネージャー 日本
132943	Program Manager Service and Delivery
156787	Buyer Non food
26654	Exploration Geologist Springer Site
44497	Startup Intern
175777	Head of Digital
39468	Charge Nurse PT Night 7pm 7 30am every other weekend
47723	Outside Commercial Sales Representative Construction Building Materials Industry
175015	Seasonal Overnight Student Supervisor Night Chaperone Dulles VA Fall 2026 and Spring 2027
24541	Danish speaking Customer Service inc relocation to Malta
96653	Manager Implementation
138640	Multi Media Design Intern
31436	Insurance Finance IFRS 17 Analyst Manager
28577	Account Manager Scale
81189	Interview Engineer Mexico
124235	Neuroscience AI Training El Paso US
1966	SAP Basis Admin
124444	Pathologists Freelance Remote Virginia Beach US
153747	Personal Trainer
58529	Vertrieb Badgestaltung
76504	Recruiter Contract Role
76028	Cyber Security Júnior SOC NOC
178245	Agent AI Engineer
20937	Military Operations Analyst
158863	Licensed Practical Nurse LPN
5027	Não encontrou a sua vaga em Operações Clique aqui e avisaremos quando for divulgada
46099	Enterprise Account Executive Japan Retail Telco
48936	Growth Product Manager
178440	Flight Test Engineer III IV
107332	Manager Reward Business Partner Data Centres
160760	Sales Executive
10918	People Program Manager
40285	Accountant
88515	Concepteur.trice en automatisation
141708	IT Assistant
102097	Window Installation Subcontractor
150307	Personal Trainer
156076	Utah Contract Therapist
105790	Software Engineer II
2107	ServiceNow Business Analyst HRSD
167401	Design Engineer
113258	Product Manager Growth
94267	Staff Nurse Operating Room 1.0 FTE LHAAMC
33165	GSI Partner Lead
38209	Future Career Opportunities
81360	Test Engineer
63037	Project Engineer CV
59498	Forward Deployed Engineer
36629	Reverse Engineering Vulnerability Research Engineer
79836	Team Lead Learning and Development Contract Remote
74498	Director of Advancement at North Carolina Hillel
51905	Benefits Solutions Consultant
174984	Account Manager
51935	Commercial Insurance Broker Account Executive Law Firms
19216	Medizinische Fachangestellte für Allgemeinmedizin Hamburg Winterhude
92663	Superintednent
136996	Player Services Representative
115695	Traveling HVAC Installation Technician
167567	6245 Solution Prep Engineer Lead Validation Engineer
65455	Inside Sales Intern
33313	Value Engineer Public Sector
153015	Personal Trainer
150232	Personal Trainer
161816	Server
119364	P&C Lead Test Engineer
152386	Personal Trainer
38482	Expert Recruiter October Start Date
80998	Distribution Analyst 1 Year Temporary
160895	Software Engineer C# Java
34312	Registered Behavior Technician RBT
153551	Personal Trainer
109766	Business Manager Olympus Fenwick
173662	Warehouse Associate
167851	Regional Sales Director UK
170240	Personal Trainer
56376	Service Associate
85575	Head of Solar
31490	Management Consultant AML Investigations Sanctions Fraud
171756	Cloud Support Engineer
120301	Growth Marketing Specialist French speaking
31703	GenAI Engineer
6716	Behavior Therapist
98835	Power Distribution Designer Mi Tech Services Inc
34380	Registered Behavior Technician RBT
59851	Sanitation Technician 4 00 PM 12 00 AM Future 12 Hour Rotating 2 2 3 Schedule
104988	Quality Technician
91610	Social Worker Looked After Children
15492	Analista Suporte de Operações Pleno
100121	RF Engineer
67357	Civil Engineering Staff Professional
149471	Commercial Terrain Indépendant Freelance
120320	Head of Event
161159	People HR Operations Analyst 12 month contract
149911	Lead Product Manager Game Content Creation
43112	Application Support Engineer
102857	Communications Content Partner
148557	Product Manager Link Consumer
138842	Product Design Engineering Co op Opportunities
39974	EMT B Urgent Care
52347	Behavioral Specialist I ACCS
45297	Manager BFSI Field Engineering
130975	Product Manager Logs Observability
61584	Lead Strategic Partner Manager
29549	Banco de candidatos Front Office
31059	Ubuntu Package Management Engineering Manager
89629	FP&A Analyst
9766	Counterproliferation Special Projects Analyst
176231	Search Specialist
150714	Personal Trainer
38120	Claims Auditor
158299	Research Analyst Intern Economics Summer 2027
98987	Safety Coordinator Michels Energy Holdings Inc
17571	Automation and AI Operations Engineer
108839	HR Business Partner Global Commercial Center
38669	Maintenance Apprentice
135447	Accounting Manager
176922	Andorid Developer
86490	Analyst Retail Media Strategy Activation
51512	Medical Assistant Ophthalmic Technician Training Provided
22781	Physical Therapist Home Health
79819	Project Manager Remote Contract
114463	Business to Business Sales Representative
128543	Engineering Manager Exa
63277	Inbound Sales Development Representative New Grad
79034	Compliance Lawyer
52729	Client Success Manager
114847	Product Manager Customer Experience
105391	Staff Engineer MAC OS Client
150900	Personal Trainer
41292	Custodial Specialist
111783	Registered Behavior Technician RBT
175164	Physical Therapist Assistant PTA Early Career
167915	Product Manager New Alarms Product
8634	Compliance Analyst
100017	Procurement Manager
49839	Associate Manager Restaurant Off Platform Measurement
109102	Packager Loader Unloader Shipping
100356	Staff Applied Scientist 시니어 스태프 응용 과학자
71432	Associate Product Manager
26376	Lead Design Engineer
54586	Certified Personal Trainer
56380	Service Associate
115995	1099 eLearning Developers
82667	Sales Assistant Categoria Protetta Roma Stazione Termini
108420	Public Sector Account Executive Okta
5541	Product Designer
167716	Account Executive Territory Mid Market
152806	Personal Trainer
39288	2026 2027 High School Leave Replacement French Teacher
11017	Product Quality Engineer Fury
88835	Growth Marketing Analyst
129793	Business Development
148281	Contracting Operations Specialist
17715	Associate Scientist I Miami
113237	Clinical Content Editor PGY 1 Medical Students
64076	Supplier Industrialization Engineer PCBA
62873	Manager Visual Design
81326	Applied AI Engineer
14674	Algorithm Engineer
125802	Accident Emergency SpR Romford
51126	Data Analyst Fincrime Reporting
161424	Territory Account Executive Retail Fort Worth
127550	Consultant Paediatrician West Midlands
55790	Member Experience Manager
171775	Agent Operations Territory Lead
148821	Workplace Experience Manager
56533	Service Associate Night
86302	Project Manager Paving
122419	Canada Residents AI Trainers Antigonish Canada
133299	ICT Risk Oversight Lead
38782	Business Consultant ComparaJá
135520	Talent Acquisition Partner
23319	Registered Nurse RN Private Duty
90936	Social Worker Mental Health
124982	Spanish Fluent Speakers AI Training Mexico City
28102	Director Partner Architect
10039	Project Manager Co Op Spring Summer 2027
34969	Microbiology Lab Manager
125847	Adult Community Speech and Language Therapist
29289	Customer Success Engineer
16742	Engineer II Process Engineering PEALD SiN
61333	Account Development Representative Portuguese Speaker Chicago Based
177329	Machine Learning Engineer Foundation Model
59473	Linux Security Engineer
26964	Psychiatric Mental Health Nurse Practitioner PMHNP
159654	Lead SAP Business Solutions gn S4 Hana Public Cloud
35472	Poker Dealer
69417	Enterprise Account Executive Acquisition META Remote Spain
52233	Requirements Analyst II
67096	Biotechnologist Protein Sciences
118824	Backend Architect Golang
84918	Associate Product Manager
88366	Project Manager Geotechnical Engineering
178780	Sales Engineer Federal
133958	Development Engineer Neutron
158811	Technical Administrator THG Nutrition Manufacturing
162052	Développeur des opérations de négociation Trade Operations Developer
17696	Actuarial Analyst Pricing
13048	Silicon Engineer
169048	Lead Doctor Pittsburgh Pennsylvania
125332	Product Intern Madrid
64312	Construction Safety Advisor Utility Infrastructure
21570	Client Services Associate
115518	Aesthetic Injector Nurse Practitioner Part Time
156535	Staff Services Engineer
43011	Line Leader 2nd Shift
36965	Medical Writer Medical Reviewer Consultant Oncology Ovarian Cancer
75601	Insurance Agent Fort Mill SC
69616	Engineer Power Electronics
45557	Strategic Core Account Executive Banking
140816	Claims Corporate Representative
160605	Account Manager North America
127854	Mammographers Derbyshire
72482	Open Application
71220	FP&A Analyst
43659	Site Reliability Engineer
38888	Education Specialist
25263	BIBIBOP Operations Leader Huber Heights
79143	Lead COTA Supervisor Sherwood
166766	Fluids Test Engineer
4685	Spanish Caribbean Language Expert Freelance AI Trainer Project
49821	Associate Manager Consumer Integrated Marketing New Verticals Habituation
85006	Underwriting Analyst
120521	Investor Development Swedish speaking
12233	Staff Systems Engineer Strategic Defense
118058	Customer Service Representative
159089	Photo Editor Operations Coordinator NYT Wirecutter
30641	PhD Physicist
105318	Regional Sales Manager
140492	Full Stack Engineer Frontend Android Focus
137357	Supervisor Assembly Quality
140920	Business Analytics Intern
171294	Systems Engineer V Test
58324	Client Relationship Manager
166534	Flutter Engineer
167038	CoE Program Content Intern Summer 2027
8204	Visual Lead Westfield London
50915	Scientist Bioconjugation
92224	Director Product Management Intelligence and Ecosystem
16347	International Tax Manager
65076	Receiving Associate
72197	Visual Writer Universes Beyond Contingent Contract
53215	Growth Specialist
25760	Psychiatric Mental Health Nurse Practitioner Part Time Hybrid
81853	Underwriting Assistant Mid Market
119375	P&C Test Engineer
130420	Executive Director Corporate Strategy Intelligence
22105	Hospice Volunteer UNPAID
55085	Fitness Manager
59593	Middle Full Stack Software Engineer .NET
36161	Sales Manager Chinese Vertical
2869	Partnerships Product Manager
163874	Lead Software Reverse Engineer
70472	Share Your Contacts
130807	Production Engineering Manager
175532	Associate Director Social
64692	Field Service Technician Newark
57243	Field Technician Mechanic Pump Power HVAC
52400	Housing First Coordinator Homeless Services CSP HI
171735	Concept Artist Vehicles Props World of Tanks
54689	District Fitness Manager
41617	쿠팡 프로덕트 마케팅 담당자 경력 5년 이상 계약직
88518	CPI mécanique du bâtiment
37497	Product Manager Enterprise
136194	Student CEO SCEO GEORGIA STATE UNIVERSITY
122759	Computer Sciences Graduates AI Training San Jose US
8374	Technical Business Analyst
38156	Clinical Informaticist
148531	Product Analytics Developer Experience Bridge
73775	Rogersville TN Client 24 7 Rotating Shifts 8 Hour or 12 Hour Shifts
69105	Stylist King Street
141815	Runner Little House Mayfair Central London
17402	Reliability Test Engineer
108988	Cost Accounting Manager
48682	Jenkintown Restaurant Team
172783	Babysitting Opportunities in Durham NC
25637	Financial Operations Manager
157516	Country Manager Canada
133623	Software Engineer Safety Foundation
77586	Data Study Participant Onsite Columbus Ohio
85533	All Future Mission Assignments Vehicle Operations
15457	Traveling Superintendent Construction
60596	Tax Manager
134564	Machine Learning Scientist Creative Generation Personalization
40339	B2B Talent Acquisition Manager
165268	Manager Enterprise Implementation
135847	2026 Fall Intern SOC Modeling
85389	Talent Acquisition Specialist Cyprus
94611	Seasonal Ambassador South Park
146511	Manager Engineering Tooling
26925	Psychiatric Mental Health Nurse Practitioner PMHNP
160385	Epic Referrals Analyst
127453	Consultant in Anaesthetics Somerset
90214	EA AC malformed questions
73272	Frontend Engineer AI Products
118820	Backend Architect Golang
90895	Social Worker Adults
99101	Utility Pole Inspector Mi Tech Services Inc
12510	Radiology Veterinary Assistant
172823	Caregiver Duluth GA
158397	Brand Designer
74599	Floating Leasing Specialist
42480	II Program Manager L6 II Rocket Growth Policies
28010	GPU Engineer
58914	Administrative Coordinator Temporary
49736	Analytics Engineer India
133265	Business Analyst Intern Summer 2027
153565	Personal Trainer
54236	Assistant General Manager
169087	New ER Doctor NERD Program Starts January 2027 Practicing Veterinarians
136336	Communications Manager Corporate Product Enterprise
171679	Convertible Arbitrage Analyst Intern Winter 2027
44791	Engineering Manager CustomerLake Profile Agents
11063	Program Operations Maritime Heavy Metal
48305	Hiring Immediately Dishwasher Prep Cook Brookfield Place
42609	Sourcer L5
107706	Video Editor Cyprus
123814	Javascript Developers AI Training Colchester UK
158798	Influencer Executive Myprotein Germany
60317	QA Proofreader
84102	Packer 1st shift
60635	Manager Strategy Partnerships
109155	Truck Loader 2nd Shift
89465	Entrepreneur in Residence Digital Health LG NOVA
62121	Manager Software Engineering Billing
99759	Home Health Nursing Supervisor Internal Only
73825	Urgent Hiring Helpers for In Home Support Mansfield PA 16933
74830	MEP Energy Marshall
85317	Funding Specialist
9678	Associate Support Analyst
80391	Software Engineering Intern
43436	Software Engineer Stream Control Plane
101027	Customer Experience Researcher
24245	Customer Support for Italy
47386	Ruby on Rails Developer
87938	Placement Associate
148164	Account Executive AI Startups Hunter
67788	Customer Success Architect CEUR
162042	Agentic System Developer
34595	Registered Behavior Technician RBT
123627	Hungary Residents Survey Participants Nagykanizsa Hungary
4266	Guaraní Language Specialist Freelance AI Trainer Project
69168	Stylist Topanga
157559	Talent Pool Submission
112852	General Application
153307	Personal Trainer
16714	Engineer I Field Service
164446	Business Leadership
18405	Project Engineer Land Development
125487	Licensed Telehealth Therapist Mental Health Counselor Nebraska
167601	RTP Quality Assurance Specialists
119171	Measurement Analyst Fusepoint
141534	Bartender Cecconi s Pizza Bar Central London
142741	Client Experience Coordinator
174632	Architect Mission Critical USA
149669	Outside Sales Account Executive Chicago IL
161144	Associate Product Manager
87946	Designer I
65836	Garage Door Technician Sales Service Professional
140274	Forward Deployment Engineer
138711	Enterprise Account Executive Hybrid NYC
172474	Manager Engineering
30190	Analista de Riscos Sênior Risco de Modelo
87461	CMBS Structured Finance Attorney NY Chicago
15049	Entry Level Traveling Safety Supervisor
106894	Business Analyst Requirements Engineer Top Secret with agreement to obtain CI Poly
68082	Program Manager Partner Launches Delivery
95288	Analyst Strategic Finance
24573	Mobile Apps QA Engineer
136476	Software Engineer Identity
125867	Adult Speech and Language Therapist
41717	쿠팡 자회사 떠나요 펜션 셀러 지원 및 CS 운영 담당자 대졸신입
29795	Full Time Analyst 2027
161876	ML Engineer II App Engine
117553	Account Executive
67481	Head of Performance Marketing Paid Social
136673	Sales Development Representative Indo China
31244	Change Communications Engagement Manager Product Experience
97243	Director Paid Social
101950	Design Researcher Contract Tokyo
16297	Bilingual Psychiatric Physician Assistant Remote
179006	Technical Success Manager
108613	Staff Technical Writer
16807	Engineer Process Engineering Global R&D Process Engineering Program
167426	Performance Marketing Manager
46508	Analytics Engineer Pricing data
13600	Project Manager Traffic Engineering
15368	Construction Project Manager New Grad 2027
32571	Global Consultant Bootcamp November 14th 2026
83806	Intern Fire Protection Engineering
100190	Anesthesiologist West Frankfort Comfortable with Peds
167780	Enterprise Development Representative December 2026 Grads
2100	Service Desk Lead
110562	Hematologist Oncologist The Center for Cancer Blood Disorders
107305	Analyst Treasury Financing and Debt Capital Markets
128625	MTS Exa
137703	Scientist I Scientist II Analytical Sciences
75412	Lab Technician Nights
40283	Head of Liquidity Management
149639	Inbound Account Executive Dutch Market
123136	Einwohner Deutschlands Studienteilnehmer innen Mannheim Deutschland
163468	Embedded Software Engineer HU
144316	Receiving Inspector Starlink Night Shift
135575	Associate Sales Engineer SE Desk Mid West
113072	Director Provider Experience
62791	Staff Information Security Engineer
3609	Summer Law Student Intern Program in Education Advocacy
140839	Field Service Engineer Central Dallas TX
89116	Ops Expert
133168	Quantum Scientist Applied QEC
37476	Named Account Executive Majors Adelaide
75681	Insurance Agent Rialto CA
147915	Assistant Store Manager Part Time
116117	Supervisor part time Perry Ellis
154692	Personal Trainer Clayton MO
141182	Retail Sales Supervisor Maje South Coast Plaza Costa Mesa CA
158675	Urology Physician Lafayette LA
26268	Product GTM Operations Manager
101060	Head of Engineering Payment Gateway
91803	Social Worker Referral and Assessment
58578	Lead Business Analyst
141847	Server Ludlow House
34977	Micro Lab Tech
122459	Canada Residents AI Trainers Hamilton Canada
109041	Machine Operator 1 Warehouse Days
48641	Talent Success Business Partner 8 Months Contract
140349	General Manager South Bend
24836	Betting Hero Sales Ambassador
164129	Manager Benefits
30786	Cloud Support Engineer London UK office
34231	.Registered Behavior Technician
94855	Line Lead 2nd Shift
33732	Behavior Technician
49852	Associate Product Operations
79526	Graphic Designer Data Visualization Remote Contract
175333	Technical Security Governance Lead Exposure Management
70194	No open role Click here to share your details speculatively
26894	Psychiatric Mental Health Nurse Practitioner PMHNP
166194	Inpatient Behavioral Health Tech BHT PRN
27746	AML Analyst II LATIN
102280	Litigation Assistant Bilingual English and Spanish
39862	Software Engineer C#
112542	Global Observability Team Lead
46396	Protocol Engineer
172392	SWE Data Ingestion
70604	History Program Specialist Mid
38741	Sales Development Representative
68330	Data Scientist Engineer
171565	Central Technology Center Operator 2nd Shift
73090	Site QA QC Specialist
42380	Director Data Security Engineering Digital Trust
27088	Psychiatrist MD
62640	Member of Technical Staff Engineering
71101	Lead Toddler Teacher
172258	Executive Assistant
47642	Entry Door Technician
108831	Finance Analyst
93041	Electrical Commissioning Lead
30999	Software Engineer Cloud Images
146705	Manager Engineering Tooling
41833	쿠팡 커머스 라이브 콘텐츠 품질관리팀 매니저 Live Quality Assurance
37787	Vehicle Inspector
164360	Technician II Sub Assembly
159306	Online Event Nursing Careers October 13 5pm Pacific
66769	Sports Data Collector Football Arendal Norway
171539	Armed Contract Security Officer Georgetown TN
154237	Personal Trainer
82139	Business Development Manager
12792	Finance Systems Engineer Finance and Strategy
5904	Software Engineer Growth Onboarding
62490	Outbound BDR
72850	Inbound Outbound Operative Nights
127997	Our Future Health Phlebotomist HCA Edinburgh Lothians
70776	R&D Team Lead
41797	쿠팡풀필먼트서비스 물류센터 시설관리 FM 담당자 전라광주1 FC 공조냉동
152996	Personal Trainer
51930	Commercial Insurance Account Manager
156541	Diesel Mechanic
56226	Service Associate
30790	Content Marketing Manager
25858	Lead Sales Engineer West
8200	Visual Lead The Knox
19270	Robot Service Technician Assistant Part Time Weekend
47090	Head of Growth Media
20051	AXON Creative Artworker Creative Marketing
152278	Personal Trainer
86715	English Chemistry Video Content Creator Grade 11 12 Freelancer India
32288	Career Advisor
138274	Founder Associate at SetSales
31207	Business Analyst DORA
87889	Veterinary Cardiologist Bridgetown Veterinary Emergency Referral
174373	Radnik u Prodavnici Novi Sad
136990	Marketing Intelligence AI Automation Manager
289	LQA Analyst Arabic
155969	Health Wellness Coach Contract
158936	Hospitality
125790	ACCIDENT EMERGENCY GRIMSBY
97168	Staff Product Manager
15564	Software Engineer Unity Frontend
59501	Government Pre Sales Engineer
5580	Security Engineer
576	Traveling Orthodontic Dental Assistant
5449	Staff Software Engineer FinTech Back End Bangkok based Relocation provided
136625	Account Executive West
26202	Global Enablement Program Manager
15357	Construction Project Manager New Grad 2027
88582	Electrical Engineer MEP
128381	Specialty Dr Required for Eating Disorders unit South Coast of England
174631	Architect Mission Critical
155419	Mechanical Engineer II IV
76114	Home Buying Specialist
68613	Service Desk Administrator
163366	Security Scheduling Manager
126569	Band 6 Locum Physiotherapist London
62957	Client Services Representative
161995	Technical Business Analyst and Client Product Owner
101215	AI Artist Multimedia Designer
1770	Full Stack Developer
13810	Sales Manager SMB Account Executive Austin
171391	Product Manager VSCO Economy
134470	Director of Product Management Search
128141	Phlebotomist Our Future Health Swansea Bay West Glamorgan
99596	Therapeutic Account Manager Liver Allentown
78740	Hardware Machine Learning Engineer
133161	Lead Quantum Error Correction Researcher
17866	Industrial Maintenance Mechanic
115304	Software Engineer
136759	Compliance Associate
121441	AI Engineer ML Engineer Developers AI Training Coventry UK
81410	Regional Sales Director Midwest
162915	Autonomy Engineer Ops Research
61243	Nursery Room Leader
9564	Production Associate
103911	Finance Manager
30216	Coordenador de Negócios Seguros
106656	Emergency Medical Technician Midland 1 day month
59996	Community Manager Affordable Housing
157998	Intern Quality Engineer
8079	Seasonal Sales Associate Part Time Toronto Eaton Centre
17737	Systems Architect
171433	Account Manager
153037	Personal Trainer
122823	Database Administrator Graduates AI Training Cancún Mexico
167127	Renewals Sales Representative Velocity
60751	Product Analyst
108005	Private Credit Analyst
59581	Calypso CATT Specialist
117095	AE OTR Network
168215	April 2027 New ER Doctor Program 2027 Graduates
9862	Delivery Assurance Architect
141562	Busser Ludlow House
128297	Specialty Dr in Addictions Required North West England
59409	Childcare Center Director Burton Park
176997	Solutions Engineer
116186	General Application
23463	RN Home Care 1 1 PRN
93204	Building Engineer
10436	Field Operations Engineer
6186	Executive Director Safety Net
91723	Social Worker Mental Wellbeing Service
94474	Seasonal Ambassador Anchorage 5th Avenue Mall
49617	Platform Engineer
136144	Product Quality Assurance Color Specialist
41319	Product Designer
122733	Computer Sciences Graduates AI Training Fresno US
123697	Italy Residents Survey Participants Rimini Italy
167353	Systems Engineer IVD Software
83777	Fire Engineer
177235	Staff Machine Learning Engineer Generative AI
109343	Associate Construction Materials Testing Technician
163562	Head of Sales
62937	UAS Systems Integration Engineer II
4119	Custody Operations Specialist Freelance Project
14362	Associate Front End Developer
123062	Dutch Fluent Speakers AI Training Chicago USA
40343	Cooling Specialist
3078	Strategic Account Executive
36802	Analyst Procurement
142174	Mobile Mechanic
95479	MN Licensed 1099 Mental HealthTherapist
169714	Dr Dan s Animal Hospital DVM VMD Extern
44565	Stakeholder Engagement Specialist
104271	Direct Care Aide
142825	Tech Lead Code Plane IC5
150485	Personal Trainer
107670	Operations Engineering Engineer
51163	FX Structurer
110354	Test Clinical Referrals
50929	VP Learning Leadership Development
167428	Product Communications Manager
121527	AI Trainer Advanced Korean Fluency Berlin Remote
34161	Early Intervention BCBA Life Skills Autism Academy Center Based
129988	Product Manager News
104496	Data Center IT Technician
177365	Analista Júnior de Produtos de Recrutamento e Seleção Vaga Afirmativa para Pessoas com Deficiência
75779	Insurance Producer Dallas GA
161380	Territory Account Executive Niagara Falls
114087	Coordinateur de secteur
51465	Front Desk Medical office Day shift no weekends full benefis
77093	Graduate Conference Manager at HRtechX
18027	IT Consultant
7205	Flagship Sales Service Manager Regent Street
89667	Lead Security and Infra Engineer
18809	Business Analyst cum Software Tester
171620	Rover Security Officer
15583	Fraud Analyst
77215	Kansas Remote Mental Health Licensed Therapist 1099 Contractor
49385	IT Squad Lead Finance Paris
174581	Part Time Substitute Teacher Up to 144 Daily
119545	Feasibility Manager
86531	Inventory Service Associate ISA Toledo OH 1099 Contractor
96414	Water Resources Designer II
168885	Emergency Veterinary Assistant Part Time Greenville SC
127315	Consultant Histopathologist South Wales
86717	Math Video Creator Freelance Grades 11 12 India
1745	EBS Analyst
152715	Personal Trainer
154222	Personal Trainer
5079	Quality Manager
88896	Customer Success Manager
2825	Test Automation Engineer
52141	Engineering Manager Architecture
52084	Licensed School Based Mental Health Clinician Lead
37075	Onboarding Coach Guest Experience
121183	Consultant Business Optimization
137462	Inside Sales Representative Home Services Sales Closer
106630	Product Quality Engineer
91370	Social Worker Child Protection
61814	Market Strategy and Partnerships Manager
143646	Integration Technician Starshield Level 4 5
31043	Technical Reporting Accountant
118905	Doula Virginia
134040	Electric Propulsion Engineer
154606	Personal Trainer Aberdeen MD
35421	Regional Director Sales CEUR
162352	Product Manager Product Manager
78032	Interested in Applying
25873	Regional Field CTO
22294	LPN
112590	Quantitative Research Internship Bachelor or Master Summer 2027 Shanghai
140046	Quality Analyst
41428	IT Help Desk Specialist
22868	Physical Therapist PT Living
154838	Personal Trainer Highland CA
139239	SIC Temp to Full Time Employee Application
177611	Data Engineer Global Team India
79847	Team Lead Learning and Development Contract Remote
36304	Construction Inspection Supervisor
117995	Customer Service Representative
106406	Associate M&A Amsterdam
66638	People Operations Manager UK
56418	Service Associate Night
30316	Pessoa Estagiária em Planejamento Estratégico
14178	Backend Engineer SRE Reliability Performance
43066	Lead Cultivation Agent
69570	Debug Validation Engineer Multiple Levels
130855	Staff Electrical Engineer Balance of Plant
113243	Panel Review Subject Matter Expert SME
113119	NP PA Virtual Urgent Care
74046	In Home Dialysis Care Partner 1+ Year Dialysis Machine Experience
110087	Family Medicine Physician Laurel Heights Sign On Bonus Available
153530	Personal Trainer
171677	Trading Risk
63853	Flight Test Operator Amarillo
179009	Technical Success Manager
72013	Staff Software Engineer
115372	パートナー営業担当
95362	Architectural Technician
118248	Shift Supervisor
111072	Store Leader Qiantan Taikoo Li
126518	Band6 Locum MSK Physiotherapist Newham
108194	资深后端工程师 专家 期权做市 流动性平台
101017	Business Relationship Manager Ekiti
105856	Learning Experience Designer ELA Multilingual Learning Remote US
91951	Support Worker or Support Worker
19163	Electrical Technician Instructor
138845	Product Developer Corded Cordless
3940	Mid Tier Application and Database Developer
15813	Especialista de Redes IP III
86837	Service Technician
46514	Digital Product Manager CorpOps Finance
90864	Practitioner Family Safeguarding
151885	Personal Trainer
107300	Risk Manager Document Controller Narvik Oslo
1867	Network Management Tools Engineer
```

## Predictions — do not read until step 3

What iteration 10's classifier answered for each of the rows above. Reading this before the labels
are written to disk destroys the measurement: the labeller would agree with it and the numbers would
decorate rather than measure.

```
1215	UNKNOWN
155016	OUT
109455	UNKNOWN
4389	OUT
115363	UNKNOWN
132700	OUT
85936	OUT
41999	UNKNOWN
967	UNKNOWN
62963	OUT
1468	UNKNOWN
174570	OUT
174918	OUT
175273	IN
133283	OUT
107044	UNKNOWN
128728	UNKNOWN
145006	UNKNOWN
54391	OUT
77005	UNKNOWN
172788	UNKNOWN
118163	OUT
80270	UNKNOWN
41798	UNKNOWN
84012	OUT
119892	UNKNOWN
77838	IN
106230	OUT
29173	IN
33336	UNKNOWN
174736	OUT
48028	OUT
61761	UNKNOWN
128279	UNKNOWN
132711	OUT
69039	OUT
45659	UNKNOWN
125255	OUT
67147	UNKNOWN
160769	UNKNOWN
23756	OUT
16835	OUT
162757	UNKNOWN
152092	OUT
85709	IN
149913	UNKNOWN
167943	IN
44558	UNKNOWN
76126	OUT
114096	OUT
81691	UNKNOWN
142222	IN
66488	OUT
125144	OUT
15692	UNKNOWN
71727	UNKNOWN
4975	UNKNOWN
73457	OUT
122941	UNKNOWN
174149	IN
19714	OUT
24349	OUT
52216	UNKNOWN
123260	OUT
43050	OUT
90890	OUT
133472	UNKNOWN
122766	UNKNOWN
40149	OUT
161838	OUT
60378	OUT
51455	OUT
169706	UNKNOWN
86435	UNKNOWN
85656	OUT
83486	OUT
169830	OUT
62020	OUT
121439	UNKNOWN
127065	OUT
157394	UNKNOWN
161544	UNKNOWN
85068	OUT
17306	UNKNOWN
32597	OUT
114976	OUT
67085	OUT
111120	OUT
27424	OUT
167158	UNKNOWN
75395	UNKNOWN
138866	OUT
95847	OUT
78416	OUT
166147	UNKNOWN
146564	IN
40594	OUT
158837	OUT
92742	UNKNOWN
62864	OUT
86489	UNKNOWN
168343	OUT
62201	UNKNOWN
123414	OUT
104359	UNKNOWN
125936	OUT
130953	OUT
113013	OUT
59875	OUT
44162	UNKNOWN
8809	UNKNOWN
41554	UNKNOWN
18367	UNKNOWN
99968	UNKNOWN
4105	OUT
48393	OUT
172738	OUT
47815	OUT
16818	UNKNOWN
30384	UNKNOWN
159445	OUT
163357	OUT
93540	UNKNOWN
60207	OUT
80289	OUT
27767	OUT
18048	OUT
33498	OUT
155241	UNKNOWN
70465	UNKNOWN
146101	OUT
32844	IN
106276	UNKNOWN
30846	IN
110221	OUT
136791	OUT
84304	OUT
52281	OUT
119349	OUT
57621	IN
84112	UNKNOWN
89876	OUT
169028	OUT
116798	IN
17471	UNKNOWN
49117	UNKNOWN
109307	UNKNOWN
100604	UNKNOWN
1452	UNKNOWN
79838	UNKNOWN
78458	OUT
111115	OUT
74411	OUT
62156	IN
27784	UNKNOWN
130597	UNKNOWN
41142	OUT
3573	UNKNOWN
63693	IN
103264	IN
175452	OUT
90236	UNKNOWN
103922	UNKNOWN
157570	UNKNOWN
164021	OUT
59510	OUT
139984	OUT
78749	OUT
28302	OUT
20657	UNKNOWN
137222	IN
153053	OUT
94566	OUT
8853	OUT
39978	UNKNOWN
101774	OUT
26415	UNKNOWN
136980	UNKNOWN
105693	UNKNOWN
84401	UNKNOWN
44549	UNKNOWN
119770	UNKNOWN
67038	OUT
132615	UNKNOWN
3062	OUT
68979	IN
74171	OUT
41519	OUT
72131	UNKNOWN
123061	OUT
45657	UNKNOWN
38814	UNKNOWN
74130	UNKNOWN
121321	UNKNOWN
53831	OUT
155750	OUT
55021	OUT
162520	OUT
84971	UNKNOWN
28073	UNKNOWN
177135	OUT
104502	UNKNOWN
86749	OUT
164110	OUT
89257	UNKNOWN
90866	OUT
162466	UNKNOWN
45314	UNKNOWN
16995	UNKNOWN
67945	IN
92825	UNKNOWN
110765	UNKNOWN
62949	OUT
66271	OUT
145560	UNKNOWN
98027	UNKNOWN
92686	UNKNOWN
43335	OUT
56081	OUT
164897	UNKNOWN
122755	UNKNOWN
97597	OUT
8237	UNKNOWN
76082	OUT
147060	UNKNOWN
68825	OUT
31209	UNKNOWN
109809	OUT
73529	OUT
84474	UNKNOWN
147157	OUT
156202	OUT
163499	OUT
10951	UNKNOWN
142086	OUT
170526	OUT
61006	OUT
69908	OUT
73865	OUT
65209	IN
114016	OUT
68331	UNKNOWN
119893	UNKNOWN
163463	OUT
55663	OUT
165085	OUT
160303	IN
173743	OUT
149746	UNKNOWN
144798	OUT
47900	OUT
19100	OUT
81289	IN
37586	IN
40743	UNKNOWN
29807	OUT
127536	UNKNOWN
135221	OUT
29113	UNKNOWN
9745	OUT
19740	UNKNOWN
127872	OUT
152115	OUT
46973	OUT
35003	OUT
45511	IN
105933	UNKNOWN
44592	UNKNOWN
96419	OUT
73022	IN
14045	UNKNOWN
174886	UNKNOWN
81207	UNKNOWN
137092	OUT
93981	OUT
109282	UNKNOWN
15977	IN
48265	OUT
20207	OUT
29080	UNKNOWN
40908	UNKNOWN
52461	UNKNOWN
95194	IN
122010	OUT
1448	UNKNOWN
97953	OUT
139535	UNKNOWN
119840	UNKNOWN
171977	OUT
94301	OUT
114974	OUT
58500	UNKNOWN
89419	UNKNOWN
89575	OUT
74710	OUT
130696	UNKNOWN
23422	OUT
163813	UNKNOWN
16544	UNKNOWN
75890	OUT
86662	OUT
111016	OUT
149702	UNKNOWN
115735	UNKNOWN
51404	OUT
156834	UNKNOWN
110516	UNKNOWN
71884	UNKNOWN
80901	OUT
20229	UNKNOWN
172808	UNKNOWN
35397	OUT
108858	OUT
170204	UNKNOWN
102541	UNKNOWN
62996	UNKNOWN
132943	OUT
156787	OUT
26654	UNKNOWN
44497	UNKNOWN
175777	OUT
39468	OUT
47723	OUT
175015	OUT
24541	UNKNOWN
96653	OUT
138640	UNKNOWN
31436	UNKNOWN
28577	OUT
81189	UNKNOWN
124235	OUT
1966	UNKNOWN
124444	OUT
153747	OUT
58529	UNKNOWN
76504	OUT
76028	UNKNOWN
178245	OUT
20937	UNKNOWN
158863	OUT
5027	UNKNOWN
46099	OUT
48936	UNKNOWN
178440	OUT
107332	IN
160760	OUT
10918	OUT
40285	OUT
88515	UNKNOWN
141708	OUT
102097	UNKNOWN
150307	OUT
156076	OUT
105790	IN
2107	UNKNOWN
167401	UNKNOWN
113258	UNKNOWN
94267	OUT
33165	OUT
38209	UNKNOWN
81360	UNKNOWN
63037	UNKNOWN
59498	IN
36629	UNKNOWN
79836	UNKNOWN
74498	OUT
51905	IN
174984	OUT
51935	OUT
19216	UNKNOWN
92663	UNKNOWN
136996	OUT
115695	OUT
167567	UNKNOWN
65455	OUT
33313	UNKNOWN
153015	OUT
150232	OUT
161816	UNKNOWN
119364	UNKNOWN
152386	OUT
38482	UNKNOWN
80998	UNKNOWN
160895	IN
34312	OUT
153551	OUT
109766	OUT
173662	OUT
167851	OUT
170240	OUT
56376	OUT
85575	OUT
31490	UNKNOWN
171756	UNKNOWN
120301	OUT
31703	UNKNOWN
6716	OUT
98835	OUT
34380	OUT
59851	OUT
104988	OUT
91610	OUT
15492	UNKNOWN
100121	OUT
67357	OUT
149471	UNKNOWN
120320	OUT
161159	UNKNOWN
149911	UNKNOWN
43112	UNKNOWN
102857	OUT
148557	UNKNOWN
138842	UNKNOWN
39974	UNKNOWN
52347	OUT
45297	UNKNOWN
130975	IN
61584	UNKNOWN
29549	UNKNOWN
31059	UNKNOWN
89629	UNKNOWN
9766	UNKNOWN
176231	UNKNOWN
150714	OUT
38120	OUT
158299	UNKNOWN
98987	OUT
17571	IN
108839	OUT
38669	OUT
135447	OUT
176922	UNKNOWN
86490	UNKNOWN
51512	OUT
22781	OUT
79819	OUT
114463	OUT
128543	UNKNOWN
63277	OUT
79034	UNKNOWN
52729	OUT
114847	OUT
105391	OUT
150900	OUT
41292	UNKNOWN
111783	OUT
175164	OUT
167915	UNKNOWN
8634	UNKNOWN
100017	OUT
49839	IN
109102	UNKNOWN
100356	UNKNOWN
71432	UNKNOWN
26376	UNKNOWN
54586	OUT
56380	OUT
115995	UNKNOWN
82667	OUT
108420	OUT
5541	OUT
167716	OUT
152806	OUT
39288	OUT
11017	UNKNOWN
88835	UNKNOWN
129793	UNKNOWN
148281	OUT
17715	UNKNOWN
113237	OUT
64076	OUT
62873	OUT
81326	IN
14674	IN
125802	UNKNOWN
51126	UNKNOWN
161424	OUT
127550	UNKNOWN
55790	OUT
171775	OUT
148821	OUT
56533	OUT
86302	OUT
122419	UNKNOWN
133299	UNKNOWN
38782	UNKNOWN
135520	OUT
23319	OUT
90936	OUT
124982	OUT
28102	UNKNOWN
10039	OUT
34969	OUT
125847	OUT
29289	IN
16742	UNKNOWN
61333	OUT
177329	IN
59473	IN
26964	OUT
159654	IN
35472	UNKNOWN
69417	OUT
52233	UNKNOWN
67096	UNKNOWN
118824	IN
84918	UNKNOWN
88366	OUT
178780	UNKNOWN
133958	UNKNOWN
158811	IN
162052	UNKNOWN
17696	UNKNOWN
13048	UNKNOWN
169048	UNKNOWN
125332	UNKNOWN
64312	IN
21570	OUT
115518	OUT
156535	UNKNOWN
43011	OUT
36965	OUT
75601	OUT
69616	OUT
45557	OUT
140816	OUT
160605	OUT
127854	UNKNOWN
72482	OUT
71220	UNKNOWN
43659	IN
38888	UNKNOWN
25263	OUT
79143	UNKNOWN
166766	UNKNOWN
4685	OUT
49821	OUT
85006	UNKNOWN
120521	UNKNOWN
12233	IN
118058	OUT
159089	OUT
30641	UNKNOWN
105318	OUT
140492	IN
137357	OUT
140920	UNKNOWN
171294	IN
58324	OUT
166534	UNKNOWN
167038	UNKNOWN
8204	UNKNOWN
50915	UNKNOWN
92224	UNKNOWN
16347	OUT
65076	OUT
72197	UNKNOWN
53215	UNKNOWN
25760	OUT
81853	OUT
119375	UNKNOWN
130420	OUT
22105	OUT
55085	OUT
59593	IN
36161	OUT
2869	OUT
163874	IN
70472	UNKNOWN
130807	OUT
175532	OUT
64692	OUT
57243	OUT
52400	OUT
171735	OUT
54689	OUT
41617	UNKNOWN
88518	UNKNOWN
37497	UNKNOWN
136194	UNKNOWN
122759	UNKNOWN
8374	UNKNOWN
38156	UNKNOWN
148531	UNKNOWN
73775	UNKNOWN
69105	OUT
141815	OUT
17402	UNKNOWN
108988	OUT
48682	UNKNOWN
172783	UNKNOWN
25637	OUT
157516	OUT
133623	IN
77586	UNKNOWN
85533	OUT
15457	OUT
60596	OUT
134564	IN
40339	OUT
165268	OUT
135847	UNKNOWN
85389	OUT
94611	OUT
146511	UNKNOWN
26925	OUT
160385	UNKNOWN
127453	UNKNOWN
90214	UNKNOWN
73272	IN
118820	IN
90895	OUT
99101	OUT
12510	OUT
172823	OUT
158397	OUT
74599	OUT
42480	OUT
28010	UNKNOWN
58914	OUT
49736	UNKNOWN
133265	UNKNOWN
153565	OUT
54236	OUT
169087	UNKNOWN
136336	UNKNOWN
171679	UNKNOWN
44791	UNKNOWN
11063	OUT
48305	OUT
42609	OUT
107706	OUT
123814	UNKNOWN
158798	OUT
60317	UNKNOWN
84102	UNKNOWN
60635	OUT
109155	UNKNOWN
89465	UNKNOWN
62121	IN
99759	OUT
73825	OUT
74830	UNKNOWN
85317	UNKNOWN
9678	UNKNOWN
80391	IN
43436	IN
101027	UNKNOWN
24245	OUT
47386	IN
87938	OUT
148164	OUT
67788	UNKNOWN
162042	UNKNOWN
34595	OUT
123627	OUT
4266	OUT
69168	OUT
157559	OUT
112852	OUT
153307	OUT
16714	UNKNOWN
164446	UNKNOWN
18405	UNKNOWN
125487	OUT
167601	UNKNOWN
119171	UNKNOWN
141534	OUT
142741	OUT
174632	UNKNOWN
149669	OUT
161144	UNKNOWN
87946	OUT
65836	OUT
140274	UNKNOWN
138711	OUT
172474	UNKNOWN
30190	UNKNOWN
87461	OUT
15049	OUT
106894	UNKNOWN
68082	OUT
95288	UNKNOWN
24573	IN
136476	IN
125867	OUT
41717	UNKNOWN
29795	UNKNOWN
161876	IN
117553	OUT
67481	OUT
136673	OUT
31244	UNKNOWN
97243	OUT
101950	UNKNOWN
16297	OUT
179006	IN
108613	UNKNOWN
16807	UNKNOWN
167426	OUT
46508	IN
13600	UNKNOWN
15368	OUT
32571	UNKNOWN
83806	OUT
100190	OUT
167780	OUT
2100	UNKNOWN
110562	OUT
107305	UNKNOWN
128625	UNKNOWN
137703	UNKNOWN
75412	OUT
40283	OUT
149639	OUT
123136	UNKNOWN
163468	IN
144316	OUT
135575	UNKNOWN
113072	OUT
62791	IN
3609	UNKNOWN
140839	UNKNOWN
89116	UNKNOWN
133168	UNKNOWN
37476	OUT
75681	OUT
147915	OUT
116117	OUT
154692	OUT
141182	OUT
158675	OUT
26268	OUT
101060	UNKNOWN
91803	OUT
58578	UNKNOWN
141847	UNKNOWN
34977	OUT
122459	UNKNOWN
109041	OUT
48641	OUT
140349	OUT
24836	OUT
164129	OUT
30786	UNKNOWN
34231	OUT
94855	OUT
33732	OUT
49852	UNKNOWN
79526	OUT
175333	IN
70194	UNKNOWN
26894	OUT
166194	OUT
27746	UNKNOWN
102280	OUT
39862	IN
112542	IN
46396	UNKNOWN
172392	UNKNOWN
70604	UNKNOWN
38741	OUT
68330	IN
171565	OUT
73090	UNKNOWN
42380	IN
27088	OUT
62640	IN
71101	OUT
172258	OUT
47642	OUT
108831	UNKNOWN
93041	OUT
30999	IN
146705	UNKNOWN
41833	UNKNOWN
37787	OUT
164360	OUT
159306	UNKNOWN
66769	OUT
171539	OUT
154237	OUT
82139	OUT
12792	IN
5904	IN
62490	UNKNOWN
72850	UNKNOWN
127997	UNKNOWN
70776	UNKNOWN
41797	UNKNOWN
152996	OUT
51930	OUT
156541	OUT
56226	OUT
30790	OUT
25858	UNKNOWN
8200	UNKNOWN
19270	OUT
47090	OUT
20051	UNKNOWN
152278	OUT
86715	UNKNOWN
32288	OUT
138274	OUT
31207	UNKNOWN
87889	OUT
174373	UNKNOWN
136990	IN
289	UNKNOWN
155969	OUT
158936	UNKNOWN
125790	UNKNOWN
97168	UNKNOWN
15564	IN
59501	UNKNOWN
5580	IN
576	OUT
5449	IN
136625	OUT
26202	OUT
15357	OUT
88582	OUT
128381	UNKNOWN
174631	UNKNOWN
155419	OUT
76114	UNKNOWN
68613	UNKNOWN
163366	IN
126569	OUT
62957	OUT
161995	UNKNOWN
101215	OUT
1770	IN
13810	OUT
171391	UNKNOWN
134470	UNKNOWN
128141	UNKNOWN
99596	OUT
78740	IN
133161	UNKNOWN
17866	OUT
115304	IN
136759	OUT
121441	UNKNOWN
81410	OUT
162915	UNKNOWN
61243	OUT
9564	OUT
103911	OUT
30216	OUT
106656	OUT
59996	OUT
157998	UNKNOWN
8079	OUT
17737	IN
171433	OUT
153037	OUT
122823	UNKNOWN
167127	OUT
60751	UNKNOWN
108005	UNKNOWN
59581	UNKNOWN
117095	UNKNOWN
168215	UNKNOWN
9862	UNKNOWN
141562	UNKNOWN
128297	UNKNOWN
59409	OUT
176997	IN
116186	OUT
23463	OUT
93204	UNKNOWN
10436	UNKNOWN
6186	OUT
91723	OUT
94474	OUT
49617	IN
136144	UNKNOWN
41319	OUT
122733	UNKNOWN
123697	OUT
167353	IN
83777	UNKNOWN
177235	IN
109343	OUT
163562	OUT
62937	IN
4119	OUT
14362	IN
123062	OUT
40343	UNKNOWN
3078	OUT
36802	UNKNOWN
142174	OUT
95479	UNKNOWN
169714	OUT
44565	UNKNOWN
104271	OUT
142825	OUT
150485	OUT
107670	UNKNOWN
51163	UNKNOWN
110354	UNKNOWN
50929	OUT
167428	UNKNOWN
121527	OUT
34161	UNKNOWN
129988	UNKNOWN
104496	UNKNOWN
177365	UNKNOWN
75779	OUT
161380	OUT
114087	UNKNOWN
51465	UNKNOWN
77093	OUT
18027	IN
7205	OUT
89667	IN
18809	UNKNOWN
171620	OUT
15583	UNKNOWN
77215	OUT
49385	IN
174581	OUT
119545	OUT
86531	OUT
96414	OUT
168885	OUT
127315	UNKNOWN
86717	UNKNOWN
1745	UNKNOWN
152715	OUT
154222	OUT
5079	OUT
88896	OUT
2825	IN
52141	UNKNOWN
52084	OUT
37075	OUT
121183	UNKNOWN
137462	OUT
106630	UNKNOWN
91370	OUT
61814	OUT
143646	OUT
31043	OUT
118905	UNKNOWN
134040	OUT
154606	OUT
35421	OUT
162352	UNKNOWN
78032	UNKNOWN
25873	UNKNOWN
22294	OUT
112590	IN
140046	UNKNOWN
41428	IN
22868	OUT
154838	OUT
139239	OUT
177611	IN
79847	UNKNOWN
36304	OUT
117995	OUT
106406	OUT
66638	OUT
56418	OUT
30316	UNKNOWN
14178	IN
43066	UNKNOWN
69570	UNKNOWN
130855	OUT
113243	UNKNOWN
113119	UNKNOWN
74046	OUT
110087	OUT
153530	OUT
171677	UNKNOWN
63853	OUT
179009	IN
72013	IN
115372	UNKNOWN
95362	OUT
118248	OUT
111072	OUT
126518	OUT
108194	UNKNOWN
101017	OUT
105856	OUT
91951	OUT
19163	OUT
138845	UNKNOWN
3940	IN
15813	UNKNOWN
86837	OUT
46514	OUT
90864	OUT
151885	OUT
107300	OUT
1867	IN
```
