# Role list: review of the draft (#25, step 1.3)

Reviewed on 2026-10-26 against ADR-0024 and its four amendments, #25's body, and
`docs/engineering-role-criterion.md`, which decides what the subset holds. The reviewer did not
write `draft.md`.

## What was recounted, and how

- `titles.tsv` and `top-200.tsv` were re-sorted by an independent keyword sketch. Over the titles on
  at least 5 vacancies it lands within about 10% of the draft's "with tail" column for every role,
  so the draft's top-200 and tail figures are not in dispute as such.
- Where a share decides a fold, it was also counted over **all 155,767 IN vacancies** in the dev
  database (`normalized_vacancy.normalized_titles`), including the titles on 1 to 4 vacancies
  (34,282 vacancies, 22% of the subset). Those figures are marked "all". They come from regexes over
  normalized titles and are rough to about ±10%.
- Raw titles and employers were read from the same database for the titles whose role is in doubt.
- 1% of the subset is 1,558 vacancies.

The subset is every vacancy IN by `docs/engineering-role-criterion.md`: a role that writes code
(Q2), works on engineer-facing artifacts (Q3), or that a software background alone opens (Q4).
Product managers, program managers and scrum masters, analysts, sales, solutions and support
engineers, technical account managers, and AI trainers with software expertise are in it on equal
footing. Each kind of work is judged here by the same two tests: a distinct day-to-day work, and
about 1% of the subset.

## Findings

### 1. The 1% floor was measured over titles on at least 5 vacancies only (required)

**Wrong.** The draft's shares stop at titles on 5 or more vacancies. The floor is about the subset,
and the rule of step 6 reaches the titles on 1 to 4 vacancies too, so a kind of work with a long
tail is understated. Four kinds the draft rejected or folded for being under 1% clear about 1% over
all vacancies:

| Kind of work | Draft's estimate | All IN vacancies |
|---|---|---|
| Business analysis (`business analyst`, `business systems analyst`, `systems analyst`, `functional analyst`, `process analyst`…) | ~1,300 (0.84%) | 1,656 (1.06%) |
| Security analysis and operations (`security analyst`, `cybersecurity analyst`, `soc analyst`, `threat … analyst`, `incident response analyst`, `exploitation analyst`…) | ~800 (0.5%) | 1,503 (0.96%), about 2,000 with GRC and compliance analysts |
| Robotics, autonomy, perception, simulation | ~800 (0.5%) | 1,430 (0.92%) |
| SRE apart from platform | 0.8% (top 200) | 1,692 (1.09%) |

The draft's security-operations figure also left out the top-200 analyst titles it had already
placed (`security analyst` 209, `cybersecurity analyst` 158, `information security analyst` 100).

**Change.** Count every fold and every rejection over all IN vacancies, by the normalized titles
in the database, not over `titles.tsv` alone. Findings 2 to 4 follow from the recount; the SRE merge
stands on other grounds (finding 15).

### 2. Business analysts are not product managers (required)

**Wrong.** `product-manager` "decides and specifies what gets built … sets priorities and the
roadmap". A business analyst elicits and documents requirements and processes for decisions others
make; a candidate reading `Business Analyst` does not expect to own a roadmap, and one reading
`Product Manager` does not expect to write process maps for an IT department. The draft covers the
fold by one clause, "writes requirements and user stories", which is half of the description. The
skills agree that the work differs: `business analyst` is nearest `product owner` at only 0.53, and
then UX designer titles. `systems analyst` (184) has raw titles such as "SQL DBA / Systems Analyst",
which no product manager is. At 1,656 vacancies (1.06%) the kind clears the floor.

**Change.** A role of its own:

- **`business-analyst`: Business analyst.** *Day to day:* works out what a business needs from its
  systems: interviews stakeholders, maps processes, writes requirements and specifications, and
  checks that what is delivered meets them.
- *Titles:* `business analyst` 630, `business systems analyst` 186, `systems analyst` 184, `it
  business analyst` 59, `it systems analyst` 32, `technical business analyst` 29, `computer systems
  analyst` 26, `functional analyst` 22, `business process analyst` 19, `business systems engineer`
  27.
- *Edges:* `product owner` → `product-manager` (owns the backlog and decides what goes in it).
  `data business analyst` and `analytics business analyst` → `data-analyst`, as the draft has it.
  `salesforce business analyst` → `enterprise-platform-developer` by the platform word, as the
  draft has it. `it analyst` stays with `support-engineer`.

The Stack Overflow survey joins "data or business analyst"; the two stay apart here because
querying data and writing requirements are different days, and both clear 1%.

### 3. Security analysts are a kind of work of their own (required)

**Wrong.** The fold rests on being under 1%, which the recount does not bear out (finding 1). On the
day-to-day test the two differ: a SOC analyst watches alerts in shifts, triages, investigates and
escalates; a security engineer builds and runs the defences. The skills show the cluster:
`security analyst` is nearest `soc analyst` 0.86 and `security operations analyst` 0.84, and
asks for mitre-att-ck, sentinel, defender, crowdstrike, splunk.

**Change.** A role of its own:

- **`security-analyst`: Security analyst.** *Day to day:* watches systems for attacks, investigates
  and responds to alerts and incidents, tracks threats, and assesses vulnerabilities and controls.
- *Titles:* `security analyst` 209, `cybersecurity analyst` 158, `information security analyst` 100,
  `soc analyst` 68, `digital network exploitation analyst` 67, `security operations analyst` 48,
  `it security analyst` 37, `target digital network analyst` 37, `industrial security analyst` 32,
  `incident response analyst` 26, `exploitation analyst` 22, the threat-intelligence analysts, and
  the GRC and compliance analysts over a security word.
- *Edges:* `detection engineer` (52), `security operations engineer` (60) → `security-engineer`:
  they build the detections and tooling the analysts run. `penetration tester` (48),
  `vulnerability researcher` (50), `security researcher` (72) → `security-engineer`, whose "finds
  and fixes vulnerabilities" covers them. `cybersecurity analyst` stays here although it is 0.79 to
  `cybersecurity engineer`: its raw titles are analysts and its skills (acas, nessus) are
  assessment.
- `security-engineer`'s description drops "and monitors for and responds to attacks", so that the
  two descriptions do not overlap.

### 4. Robotics is folded into embedded by widening the description (required)

**Wrong.** The fourth amendment lets a kind of work under 1% join a role only where that role's
description already covers it. The draft wrote robotics into `embedded-engineer`'s description and
label ("and the control and autonomy software of robots and vehicles") so that it would. The work
differs: robotics software engineers do motion planning, perception and control in C++ and Python
on Linux with ROS; embedded engineers write firmware and drivers in C for microcontrollers and
boards. And the kind is not under 1%: 1,430 (0.92%) over all vacancies.

**Change.** A role of its own:

- **`robotics-engineer`: Robotics engineer.** *Day to day:* writes the software that lets robots
  and autonomous vehicles sense, plan and move: perception, motion planning, control and simulation,
  tested on the machine.
- *Titles:* `robotics engineer` 160, `robotics software engineer` 131, `autonomy engineer` 91,
  `simulation engineer` 89, `perception engineer` 57, `autonomy software engineer`, `robotics
  automation engineer` 21, `simulation software engineer` 20.
- *Edges:* `computer vision engineer` → `machine-learning-engineer`, as the draft has it.
  `flight software engineer` (95) → `embedded-engineer`: flight computers' software is embedded work.
  `mech and robotics tech` → no role.
- `embedded-engineer` returns to "Embedded engineer": firmware, embedded Linux, drivers and kernels.
  It stays at about 1.8% (2,802 all).

### 5. `systems engineer` mixes four kinds of work and should have no role (required)

**Wrong.** It is 1,585 vacancies, 43% of `systems-administrator`'s top-200 share, and its raw titles
do not say one kind of work, which is the reason the draft gives no role to `automation engineer`
and `design engineer`. The raw titles behind it:

- 265 (17%) carry "software" in the raw title: "Software Engineer, Engine Systems", "Software
  Engineer II - Core Systems & Libraries", "Principal Software Engineer – File System". The
  breakdown moved "Systems" in front of the head. These are software engineers.
- Defence systems engineering (requirements, integration, MBSE): Northrop Grumman 98, RTX 74, Boeing,
  Anduril, KBR, AeroVironment, Helsing. "Principal Systems Engineer – Chief Engineer", "Systems
  Engineer II - Rotary Wing Displays". The draft knew of this and left it to the blind sample.
- Presales: "Presales, Systems Engineer - SLED", "HPE Networking Federal Presales, System Engineer".
- IT systems work: GDIT, Leidos, CACI, Esri, "Linux Systems Engineer", "CommVault Systems Engineer".
  About half of the vacancies outside the "software" ones (646 of 1,320) name an IT skill (vmware,
  windows, active-directory, powershell, intune, linux), and `linux` is in that list.

The criterion itself reads a bare `Systems Engineer` as domain-ambiguous.

**Change.** `systems engineer` → no role. Reported as a normalization note for #92: "Software
Engineer, X Systems" should not read as `systems engineer`. `systems-administrator` keeps
`systems administrator`, `it systems engineer`, `it engineer`, `database administrator` and the
rest: about 2,100 (1.3%) in the top 200 and about 3,300 (2.1%) with the tail, still over the floor.
Coverage loses about one point.

### 6. Titles that are not the work they seem to name (required)

**Wrong.** Some titles the draft places, or that a rule built on the draft's kind words would
place, are not the work their words suggest:

- `mobile expert` (207) → `mobile-engineer` in the draft. All 207 are T-Mobile USA on Workday, with
  no skills recorded: T-Mobile's retail store sales job. It is a classification leak, not mobile
  engineering.
- `mobile mortgage advisor` (62) and `mobile mortgage specialist` (46) are bank mortgage sales. A rule
  reading `mobile` as a kind word would send them to `mobile-engineer`.
- `ai machine learning quality control specialist` (49) is "Quality control specialist for AI/ML
  with Dutch language": a language job, which the criterion decides OUT. A rule reading `ai` and
  `machine learning` would send it to an AI role.

**Change.** All three → no role, written into the curated file as exceptions, and reported to the
classification (#11) as leaks. `mobile-engineer` loses 207 and stays at about 1.5% (2,300 all
without them).

### 7. AI-training posts hide inside other normalized titles (required)

**Missing.** The draft does not mention AI training, which the criterion admits when the expertise
is software. About 500 vacancies (0.32%; Prolific Academic, Meridial and others) are titled like
"HTML/CSS - Senior Developers - AI Training", "Database Administrator - Graduates - AI Training" or
"Coding Specialist (Fluent in Hindi) - Freelance AI Trainer Project". The breakdown drops "AI
Training" and leaves the expertise, so they land in other roles' titles:

| Normalized title | AI-training posts in it | Title's vacancies |
|---|---|---|
| `ai developer` | 118 | 265 (44%) |
| `database administrator` | 76 | 376 (20%) |
| `ai engineer` | 64 | 2,572 |
| `machine learning engineer` | 53 | 1,846 |
| `computer sciences` | 52 | 52 |
| `data science analysis` | 39 | 39 |

At 0.32% the kind is under 1% and no role's description covers it (an AI trainer writes and grades
code to train a model, not software for users), so it should have no role. But the sorting cannot
keep it out, because the normalized titles are shared with real developers and DBAs.

**Change.** A normalization note for #92: "AI Training" and "AI Trainer" name the kind of work and
should survive the breakdown (as `ai trainer`), so the role list can give it none. Until then the
draft records that `ai developer` and `database administrator` are partly AI training.
`ai coding specialist` (35) and `coding specialist` (27) → no role.

### 8. The worked examples for `platform` and `infrastructure` contradict each other (required)

**Wrong.** The draft reads `data platform engineer` → data, `machine learning platform engineer` →
ML, and `ai platform engineer` → AI: a kind word before `platform` names the work. But it reads `ai
infrastructure engineer` → `platform-engineer`, although `platform-engineer`'s own description treats
platform and infrastructure as one word for the same work. It leaves `machine learning
infrastructure engineer` (55; ray, vllm, sglang, kubeflow) and `data infrastructure engineer` (76;
trino, iceberg, dagster, nearest data platform engineer 0.83) unplaced. A rule written from this
sorting would get one of the two readings wrong.

**Change.** State the precedence once, in the draft and for the rule: `platform`, `infrastructure`
and `software` before the head defer to a kind word in front of them that has a role (data, machine
learning, AI, security, backend, network). So `data infrastructure engineer` → `data-engineer`,
`machine learning infrastructure engineer` → `machine-learning-engineer`, `security platform
engineer` (38) and `security infrastructure engineer` (19) → `security-engineer`, `backend platform
engineer` (19) → `backend-engineer`. `ai infrastructure engineer` (65; nccl, infiniband, slurm, GPU
clusters) is then the one exception, kept in `platform-engineer` by the curated file with its
reason, since `ai-engineer`'s description (applications on foundation models) does not cover
running GPU clusters.

### 9. The edge between `solutions-architect` and `sales-engineer` is not drawn for presales architects (required)

**Wrong.** `solutions architect` is posted most by vendors (Databricks 111, MongoDB 27, NVIDIA 23,
GitLab 14, Stripe 12), where the job is pre-sale technical design with the customer; it is 0.65 to
`solutions engineer` and asks for the vendor's own product (databricks, spark). The draft keeps it
in `solutions-architect` because "the candidate expects to design", which is fair for the bare
title. But it also reads `ai solutions engineer` → `sales-engineer` and `ai solutions architect` →
`solutions-architect`, and puts `delivery solutions architect` with the architects "because the head
is architect", while for every other architect it lets the kind word decide (`data architect`,
`salesforce architect`). The titles that name the sale say which side they are on, and the draft
does not place them: `partner solutions architect` 44, `presales solutions architect` 22, `presales
solution architect` 22, `presales systems engineer` 64, `presales engineer` 44.

**Change.** A kind word naming the sale (`presales`, `pre sales`, `partner`, `sales`) sends an
architect or engineer title to `sales-engineer`; the bare `solutions architect` stays. Keep
`solutions architect` vs `sales-engineer` as the pair the blind sample reports first.

### 10. Game development has to be left without a role, not read as software engineer (required)

**Wrong.** The draft lists game development among the kinds under 1% (about 380 vacancies all,
0.25%) and then says `game developer` and `gameplay engineer` "read as `software-engineer` by
their head under the draft", leaving the choice to the reviewer. `software-engineer` covers titles
"where the title names no more specific area"; `game` names one. Folding it would be the guess the
fourth amendment forbids.

**Change.** `game developer`, `game engineer`, `gameplay engineer`, `gameplay programmer`,
`graphics engineer`, `graphics programmer`, `rendering engineer` → no role, reported. They stay
visible as their normalized titles.

### 11. Intern titles with a kind of work should take that kind's role (required)

**Missing.** The draft leaves bare `intern`, `internship` and `stage` without a role, which is right:
they name no work. It says nothing about interns with a kind word, which are 1,880 vacancies (1.2%)
over all: `data science intern` 55, `it intern` 56, `frontend trainee` 51, `graduate software
engineer` 36, `software development intern` 33, `machine learning intern` 24, `quantitative research
intern` 24, `cybersecurity intern` 19… The criterion reads `intern`, `graduate`, `trainee` and
`student` as a stage of a career, not as work, which is how the ADR reads `senior`.

**Change.** An edge in the draft and a case for the rule: `intern`, `internship`, `trainee`,
`graduate`, `stage`, `stagiaire`, `apprentice` are taken out and the role is read from what is left
(`data science intern` → `data-scientist`); the bare forms keep no role. Reported to #92, where the
breakdown could take them out as a level.

### 12. Wording that treats admitted work as outside the subset (required, small)

The draft gives some no-role titles a reason that says the work is not the subset's, when the
criterion admitted every one of them:

- "`financial analyst` → no role: budgets and forecasts in spreadsheets, not data work in the
  subset's sense", and in the table "Finance, not engineering".
- "`account manager` (not in the top 200) → no role: sales without technical work."
- "`mech and robotics tech` → no role: a technician who builds and repairs machines, not software."

**Change.** The reason for a title without a role is the fourth amendment's: its kind of work is
under about 1% and no role's description covers it, or its title does not say one kind. Where a
title looks wrongly admitted (`mobile expert`, `mobile mortgage advisor`), say that it is a leak for
#11, not that it is "not engineering". Issue #25's "Open" also still speaks of "work in the subset
that is not engineering"; product, analyst and account work are admitted by Q3 and Q4 and are not a
separate question.

### 13. Some shares lean on one employer's repeated postings (optional)

Several top-200 titles are mostly one employer, posting the same job in many places:

| Title | Vacancies | Top employer |
|---|---|---|
| `android team lead`, `android tech lead` | 248, 247 | Speechify, 246 each |
| `ai data engineer` | 333 | Speechify, 246 |
| `mobile expert` | 207 | T-Mobile, 207 (finding 6) |
| `technical product`, `ai technical product manager` | 178, 200 | bjakcareer, 172 and 174 |
| `mobile application developer` | 80 | bjakcareer, 71 |
| `flask+react full stack engineer` | 101 | bluelightconsulting, 101 |
| `ai developer` | 265 | Prolific, 117 (finding 7) |

No role's 1% rests on these today: `mobile-engineer` without Speechify and T-Mobile is about 2,300
(1.5%) over all vacancies. But Speechify's "Data & AI Engineer" (skills vercel, ios, android) makes
`ai data engineer` 74% one posting. **Change:** the measurement report gives each role's share with
the share of its largest employer beside it, so a role held up by one employer is visible.

### 14. `support-engineer` joins product support and IT help desk (optional)

IT and help-desk support is about 1,440 vacancies (0.92%) over all, and product and technical
support about 2,290 (1.47%). A candidate reading "Technical Support Engineer" at a software vendor
expects to read logs and reproduce bugs in the product; one reading "IT Support Technician" expects
employees' laptops and accounts. The draft's "same queue of tickets" is a fair reading of the
title words, and the IT side is under 1% by a little. **Keep merged**, and let the blind sample
report `support-engineer` vs `systems-administrator` by those two halves; if they split often,
the IT half joins `systems-administrator` (whose description "keeps … devices running" covers it)
rather than becoming a role.

### 15. The SRE merge is right, for a different reason (optional)

SRE is 1,692 (1.09%) over all, not 0.8%, so "SRE alone is 0.8%" is not a reason. The merge stands
on the other one the draft gives: the titles are used interchangeably and a candidate expects the
same pipelines, clusters and on-call. **Change:** drop the size argument from the draft.

### 16. Smaller edges (optional)

- **`integration engineer` → `software-engineer` but `integrations engineer` → `backend-engineer`.**
  A rule should not split on a plural. Put both in one role (`software-engineer`, since neither says
  an area reliably) or hold the exception in the curated file.
- **`network security engineer` → `network-engineer`** while every other `X security engineer` goes
  to security. Defensible by the work (firewalls, VPNs), but it is a curated exception, and the draft
  should say so for the rule.
- **`applied scientist` → `research-scientist`.** Its skills (sagemaker, athena, ecs, spark) and
  nearest titles (ML engineer 0.72) are product modelling, and `research-scientist`'s description
  is "advances methods … publishes". `machine-learning-engineer` fits it better.
- **`technology` (103; CTO 39% executive).** By the ADR's reading of a bare `Engineering Manager`
  as `engineering` → `software-engineer`, a bare `technology` with its management level could follow
  it; its skills are spring-boot, java, react, nearest `tech lead` 0.69. The draft's "none" is also
  defensible; whichever is chosen, it should be argued from the `engineering` precedent.
- **Other-language bare heads.** The draft sends `ingénieur` and `développeur` to `software-engineer`
  as bare heads. `ingeniero` 49, `engenheiro` 23, `desarrollador` 30, `desenvolvedor` 17, `concepteur
  développeur` 13 should be listed with them, and `analista` 71 and `cientista` 24 with bare
  `analyst` and `scientist` (no role). Note that `ingénieur` is 70% Thales, mostly embedded and
  industrial work.
- **`developer experience engineer` (71), `developer productivity engineer` (27), `developer
  platform engineer` (29)** are unplaced; they build internal tooling and CI for engineers, which
  `platform-engineer` covers.
- **`implementation engineer` (71), `deployment engineer` (33)**: `forward-deployed-engineer`'s
  description ("build and deploy the product in their systems … integrations for them") covers them.
  Placing them there is better than leaving the draft's "may join at sorting".
- **Labels.** `enterprise-platform-developer` holds administrators, consultants, analysts and
  architects as well as developers; a label such as "Enterprise platform engineer" or "Business
  platform specialist" fits its members better (the slug can stay). `sales-engineer` holds the
  post-sale technical account managers and customer success engineers (670, 0.43% all, a fold the
  description honestly covers); "Sales and customer engineer" says so.
- **`systems-administrator`'s label** does not mention databases, though DBAs (971, 0.62% all) are
  folded into it; the fold is honest by the description, and "Systems and database administrator"
  would let a DBA find it.

## The checks the brief asked for

**Distinct work.** Pairs whose edge was not drawn or whose examples contradict each other: 
`product-manager` / business analysts (finding 2); `security-engineer` / security analysts (3);
`embedded-engineer` / robotics (4); `platform-engineer` / `ai-engineer`, `data-engineer`,
`machine-learning-engineer` on platform and infrastructure (8); `solutions-architect` /
`sales-engineer` on presales architects (9); `systems-administrator` / software, defence systems and
presales work inside `systems engineer` (5). The other edges are drawn and consistent.

**About 1% each.** Every role clears about 1% over all IN vacancies, including the three new ones
(business analysis 1.06%, security analysis 0.96% before GRC, robotics 0.92%). The draft's three
"near the floor" roles are comfortably over it once titles on 1 to 4 vacancies are counted:
`enterprise-platform-developer` about 2.0%, `frontend-engineer` about 1.3%, `support-engineer` about
2.3%. `forward-deployed-engineer` is about 1.3%, `research-scientist` about 1.5%,
`program-manager` about 1.7%.

**None inside another.**
- `ai-engineer` is not inside `machine-learning-engineer`: one builds on models others trained
  (langchain, langgraph, mcp, openai), the other trains and serves them (pytorch, mlflow). A
  candidate reading the two titles expects different work, and a visitor moving from backend into
  AI picks the first. Keep both.
- `full-stack-engineer` is not inside `software-engineer`: `software-engineer` is defined as the
  titles that name no area, and full stack names one. The same holds for backend, frontend and
  mobile.
- `forward-deployed-engineer` is a kind of work, not a rank of solutions work: the day is writing
  production code inside the customer's systems after the sale, which neither the sales engineer
  (demos before the sale) nor the solutions architect (design) does. It clears the floor on its own
  title, mostly at Databricks, Salesforce, Palantir and the AI labs. Keep, with implementation
  engineers joining it (finding 16).

**Over 25%.** None. `software-engineer` is about 16%, and finding 5 takes a little software work back
into it once #92 stops reading "Software Engineer, X Systems" as `systems engineer`. Keep.

**Folds under 1%.**

| Fold | Over all vacancies | Verdict |
|---|---|---|
| Business analysts into `product-manager` | 1.06% | Does not hold: own role (finding 2) |
| Robotics into `embedded-engineer` | 0.92% | Does not hold: the description was widened to cover it; own role (finding 4) |
| Security analysts into `security-engineer` | 0.96% (about 1.3% with GRC) | Does not hold: own role (finding 3) |
| DBAs into `systems-administrator` | 0.62% | Holds: "keeps … databases running" is in the description; label optional (finding 16) |
| Scrum masters and agile delivery into `program-manager` | 0.23% | Holds: "the process the team runs" |
| TAMs and customer success engineers into `sales-engineer` | 0.43% | Holds: "looks after accounts technically after the sale"; label optional |
| Quant researchers into `data-scientist`, quant developers into `software-engineer` | 0.73% | Holds |

**Titles left without a role.**

| Title | Draft | Review |
|---|---|---|
| `analyst` (844) | none | Agree. 80% Workday; CrowdStrike Falcon Complete (security), Genpact, DBS, Home Depot, MUFG; the title says no kind. |
| `architect` (402) | `solutions-architect` | Agree, unsure. Genpact, Bank of America, PwC: enterprise design, which the description covers. |
| `administrator` (152) | `systems-administrator` | Agree. Oracle DBA, SharePoint, SAP Basis: running a system, which the description covers. |
| `consultant` (340) | none | Agree. ERP, data, strategy; the criterion found most consultants elsewhere are not this work. |
| `automation engineer` (715) | none | Agree. JLL and Cushman & Wakefield (building automation), Eli Lilly, Jabil, GEA (industrial), Roboyo (RPA). |
| `systems engineer` (1,585) | `systems-administrator` | Disagree: none (finding 5). |
| `technology` (103) | none | Either; see finding 16. |
| `data` (80) | none | Agree. A head of data leads engineering, analytics or science, and the title does not say which. |
| `specialist` (121), `intern` (106), `stage` (86), `scientist` (90) | none | Agree. |

## Cross-check against the Stack Overflow developer types

| Survey type | Draft | Note |
|---|---|---|
| Full-stack, back-end, front-end, mobile | own roles | |
| Desktop or enterprise applications | inside `software-engineer` | No title word marks it. |
| Embedded applications or devices | `embedded-engineer` | Robotics apart (finding 4). |
| Game or graphics | none | 0.25%; no role (finding 10). |
| QA or test | `qa-engineer` | |
| DevOps, cloud infrastructure, SRE | `platform-engineer` | The survey splits them; the titles are interchangeable here (finding 15). |
| Data engineer, data scientist | own roles | |
| AI/ML engineer | `ai-engineer` and `machine-learning-engineer` | Finer than the survey, on a real difference. |
| Data or business analyst | `data-analyst`; business analysts folded | Business analyst becomes a role (finding 2). |
| Database administrator | folded into `systems-administrator` | 0.62%, covered. |
| System administrator | `systems-administrator` | |
| Security professional | `security-engineer` | Split by analysts (finding 3). |
| Engineering manager, senior executive | management level | By the ADR. |
| Product manager, project manager | `product-manager`, `program-manager` | |
| Research and development | `research-scientist` | |
| Hardware engineer | none | 0.35%, and mostly OUT by the criterion's discipline markers. |
| Designer | none | OUT by the criterion; `ux researcher` (74) and `ux designer` (33) are leaks. |
| Developer advocate, developer experience | none / unplaced | DevRel 0.21%, none. DevEx engineers fit `platform-engineer` (finding 16). |
| Blockchain | none | 0.06%. |
| Educator, student | none | Students with a kind word take the kind (finding 11). |
| Marketing or sales | `sales-engineer` for technical sales | Account managers without a technical word: none. |

No common kind the survey names is missing at about 1%, once business analysts are a role. Two the
survey does not name are in the data: technical support (about 2.3%, kept) and AI training (0.32%,
no role, finding 7).

## Verdict per role

| Slug | Verdict | What changes |
|---|---|---|
| `software-engineer` | keep | Game titles out (10); interns with a kind word read by the kind (11); other-language bare heads listed (16) |
| `platform-engineer` | change | Precedence of `platform` and `infrastructure` stated; ML and data infrastructure out; `ai infrastructure engineer` a curated exception (8); DevEx engineers in (16) |
| `security-engineer` | change | Analysts out to `security-analyst`; "monitors for and responds to attacks" out of the description (3) |
| `data-engineer` | keep | `data infrastructure engineer` in (8) |
| `ai-engineer` | change | Edges with platform and infrastructure (8); AI-training posts recorded as a leak (7) |
| `product-manager` | change | Business analysts out (2) |
| `full-stack-engineer` | keep | |
| `systems-administrator` | change | `systems engineer` out (5); label may name databases (16) |
| `data-scientist` | keep | |
| `backend-engineer` | keep | `backend platform engineer` in (8); `integrations engineer` with `integration engineer` (16) |
| `solutions-architect` | change | Presales and partner architects to `sales-engineer` (9) |
| `machine-learning-engineer` | keep | `machine learning infrastructure engineer` in (8); `applied scientist` in (16, optional) |
| `qa-engineer` | keep | |
| `embedded-engineer` | change | Robotics out; description and label back to embedded (4) |
| `mobile-engineer` | change | `mobile expert` out (6) |
| `data-analyst` | keep | |
| `network-engineer` | keep | `network security engineer` recorded as an exception (16) |
| `sales-engineer` | keep | Presales architects in (9); label optional (16) |
| `support-engineer` | keep | Blind sample reports its IT and product halves (14) |
| `program-manager` | keep | |
| `research-scientist` | keep | `applied scientist` out (16, optional) |
| `forward-deployed-engineer` | keep | Implementation and deployment engineers in (16) |
| `frontend-engineer` | keep | |
| `enterprise-platform-developer` | keep | Label optional (16) |
| `business-analyst` | **new** | (2) |
| `security-analyst` | **new** | (3) |
| `robotics-engineer` | **new** | (4) |

27 roles.

## Required changes

1. Recount every fold and rejection over all IN vacancies, titles on 1 to 4 vacancies included (1).
2. Add `business-analyst` and take business analysts out of `product-manager` (2).
3. Add `security-analyst` and take the analysts and the monitoring clause out of `security-engineer`
   (3).
4. Add `robotics-engineer` and return `embedded-engineer` to embedded work (4).
5. `systems engineer` → no role; report "Software Engineer, X Systems" to #92 (5).
6. `mobile expert`, `mobile mortgage advisor`, `mobile mortgage specialist` and `ai machine learning
   quality control specialist` → no role, as curated exceptions, reported to #11 (6).
7. Record the AI-training leak into `ai developer`, `database administrator`, `ai engineer` and
   `machine learning engineer`, and ask #92 to keep "AI Training" as a kind word (7).
8. State the precedence of `platform`, `infrastructure` and `software` before the head, and place the
   ML, data, security and backend platform and infrastructure titles by it (8).
9. Send presales and partner architects to `sales-engineer` (9).
10. Game and graphics titles → no role, not `software-engineer` (10).
11. Interns, trainees and graduates with a kind word take that kind's role (11).
12. Replace "not engineering" reasons with the fourth amendment's reasons (12).

## Optional changes

1. Report each role's share with its largest employer's share (13).
2. Let the blind sample report `support-engineer`'s IT and product halves (14).
3. Drop the size argument from the SRE merge (15).
4. One role for `integration engineer` and `integrations engineer`; `network security engineer`
   recorded as an exception; `applied scientist` → `machine-learning-engineer`; `technology` argued
   from the `engineering` precedent; other-language bare heads listed; DevEx engineers →
   `platform-engineer`; implementation and deployment engineers → `forward-deployed-engineer` (16).
5. Labels: `enterprise-platform-developer`, `sales-engineer`, `systems-administrator` (16).
