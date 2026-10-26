# The title breakdown re-measured with Workday, and what the dropped parts carry

Two probes run on 2026-10-26 for #25's triage, against `~/oneprofile-snapshots/classified-2026-10-25.dump`,
over its 155,767 vacancies classified `IN` (41,727 Greenhouse, 22,575 Ashby, 12,552 Lever, 78,913
Workday; 155,280 of them live in one sweep, `2026-10-workday-run.md`). Both were throwaway Python
scripts over the cleaned and raw titles, not rules. Read their numbers as estimates.

## 1. How far the titles collapse

The steps of `2026-10-title-breakdown-probe.md`, with one change: the last step cuts after the head
as the title classifier reads it (ADR-0009, `TitleClassificationRule.head`), yielding heads
included, rather than after the first head word. Nothing moves a kind of work in front of the head
and nothing merges synonyms, so a real breakdown and list would cover more.

| ATS | Distinct after the cut | Top 300 cover | Titles needed for 70% | Floor there |
|---|---:|---:|---:|---:|
| Greenhouse | 7,212 | 71.5% | 253 | 14 |
| Ashby | 3,262 | 81.4% | 86 | 26 |
| Lever | 2,753 | 73.9% | 215 | 7 |
| Workday | 19,155 | 55.8% | 1,636 | 4 |
| **All** | **27,427** | **62.8%** | **786** | **14** |

Greenhouse alone reproduces the earlier probe's 72.6%. Workday is what moves the figure: its
cleaned titles are 47,174 distinct over 78,913 vacancies, and its top 300 after the cut cover 55.8%.

Over all four ATSs, by floor:

| Floor | Titles at or over it | Vacancies they cover |
|---:|---:|---:|
| 5 | 2,439 | 78.1% |
| 10 | 1,178 | 72.9% |
| 20 | 587 | 67.9% |
| 30 | 396 | 64.9% |
| 50 | 259 | 61.6% |
| 100 | 150 | 56.8% |

The most common results: `software engineer` 18,434, `engineer` 4,048, `developer` 2,519,
`data engineer` 2,518, `data scientist` 2,358, `engineering` 2,244, `manager` 1,992, `product manager`
1,738, `director` 1,568, `ai engineer` 1,424.

**12.9% of the subset lands on a bare head** (`engineer`, `engineering`, `manager`, `director`,
`associate`, `member`, `analyst`, `tech`, `developer`, `architect`, `consultant`, `lead`, `head`,
`vp` and a few more), 13.7% of Workday's. Those count toward the 70% while naming no role. Some are
the cut's faults, which a breakdown reading multi-word heads would avoid:

- `Member of Technical Staff, Security` → `member`; `Tech Lead, Android Core Product` → `tech`;
  `Engineering Manager, Build` → `engineering`; `Chief Technology Officer` → `chief`.
- A leader's function stands after the head: `Director of Cybersecurity` → `director`,
  `Head of Security` → `head`, `Senior Manager, Data Applications` → `manager`.
- A skill taken out as the only word in front of the head leaves it bare where the skill is the
  kind of work: `Senior Android Engineer` → `engineer`, `React Native Engineer` → `engineer`.

Other faults seen in a sample of 400:

- Requisition codes reach the normalized title: `Product Owner - R01572426` → `product owner
  r01572426`, `[Job-31906] Senior Data Developer` → `job 31906 data developer` (2.3% of Lever).
- Workday's banks put a corporate grade in front of or after the title: `VP System Analyst
  Corporate Internet Banking`, `Senior Network Engineer - VP`, `AVP - Senior Network Engineer`.
  Read as leadership, it keeps `vp system analyst` apart from `system analyst`.
- `Software Engineer in Test` → `software engineer`: the test kind is after the head.
- Places the location field does not name stay in: `Remote, Latin America`, `DACH`, `IND Staff
  Software Engineer`.

## 2. What the dropped parts carry

The kinds below were read off 100 titles drawn at random per ATS (400), each set beside its
breakdown. Each was then counted with a regex over all 155,767 raw titles; the regexes are rough
both ways (`native` is not always a language, `campus` not always a graduate post).

| Kind | Greenhouse | Ashby | Lever | Workday | All |
|---|---:|---:|---:|---:|---:|
| Employment: intern, co-op, apprentice, working student | 2.8% | 1.8% | 2.1% | 4.2% | 5,171 (3.3%) |
| Employment: new grad, early career, graduate | 1.2% | 0.6% | 0.7% | 0.7% | 1,216 (0.8%) |
| Employment: contract, fixed term, freelance | 1.0% | 0.4% | 0.9% | 0.6% | 1,051 (0.7%) |
| Employment: part time, full time, a percentage | 0.1% | 0.1% | 0.3% | 0.5% | 506 (0.3%) |
| Cohort year or season (`Summer 2027`) | 1.9% | 0.9% | 0.9% | 1.9% | 2,555 (1.6%) |
| Kind of work after the head (`, Backend`) | 7.2% | 11.0% | 4.7% | 3.1% | 8,474 (5.4%) |
| Test kind (`in Test`, `SDET`) | 0.6% | 0.3% | 0.6% | 0.5% | 782 (0.5%) |
| Region or country the location field misses | 4.0% | 2.9% | 8.5% | 1.0% | 4,151 (2.7%) |
| Work mode word | 1.7% | 1.5% | 7.8% | 2.6% | 4,026 (2.6%) |
| Corporate grade (`VP`, `AVP`, `, Associate`) | 0.5% | 0.3% | 0.3% | 1.7% | 1,660 (1.1%) |
| Clearance | 0.9% | 0.1% | 1.5% | 1.0% | 1,345 (0.9%) |
| Language asked for | 0.7% | 1.1% | 0.9% | 0.7% | 1,217 (0.8%) |
| AI training gig | 1.0% | 0.2% | 0.0% | 0.0% | 496 (0.3%) |
| Startup stage (`Founding`) | 0.1% | 1.7% | 0.2% | 0.0% | 435 (0.3%) |
| Requisition or job code | 0.1% | 0.0% | 2.3% | 0.1% | 399 (0.3%) |
| Shift, hours, time zone | 0.3% | 0.1% | 0.1% | 0.3% | 365 (0.2%) |
| Years of experience | 0.0% | 0.0% | 0.2% | 0.2% | 220 (0.1%) |

Examples:

- Employment: `Software Engineering Intern (Summer 2027)`, `DevOps Engineer (New Grad)`,
  `Support Engineer (Contract)`, `Salesforce Developer (UI/UX) 12 Month FTC`, `Software Engineer
  (Part-time)`, `AI Operations Specialist – FinOps & AI Cost Management 80-100% (f/m/d)`.
- Clearance: `Cloud Engineer - TS/SCI w/ Poly`, `Senior APEX Developer (Top Secret Clearance
  Required)`, `Journeyman Software Developer - SECRET`.
- Language: `Solutions Engineer - UK (German Speaking)`, `Forward Deployed Engineer French/Arabic
  speakers`, `Sr Mobile Expert, Bilingual Spanish Preferred`.
- Region: `Full Stack Laravel + React Developer - Remote, Latin America`, `Senior Sales Engineer,
  DACH`, `SDET - API/Middleware (India)`.
- Corporate grade: `Data Scientist, AI Lab (AVP/VP)`, `EASM / Persistent Perimeter Assurance
  Analyst, Vice President`, `Senior Lead Software Engineer Securities Finance SVP`.
- AI training: `Python - Senior Developers - AI Training - Liverpool, UK`, `Kotlin Coding
  Specialist - Freelance AI Trainer Project`.
- Years: `Embedded Software Engineer | C, Device Driver, Linux ,Multithreading - 10+ years - Bangalore`.
- Shift and hours: `Technical Support Engineer (PT time zone)`, `Application Support Analyst
  (Middle East Hours)`.

**Most of what the cut drops is a team, a product or a domain.** 53.0% of the subset has words
after the head, and in the sample most of them are of this kind: `Software Engineer II, Growth`,
`Senior Software Engineer, Secrets Management`, `Staff Forward Deployed Engineer, Life Sciences`,
`Platform Engineer, Payments`. The kinds in the table above are a small part of the tail.

The sample also shows titles the classifier let into the subset that no engineering role list
holds: `Technical Writing Intern`, `KIP Spring 2027 - Events Intern`, `Fluid Mechanics and Lab –
Adjunct Faculty`, `Warehouse Specialist - Transportation - 2nd Shift`.

## 3. Read for the role list (#25), later the same day

Throwaway queries over the same snapshot, behind the decisions in ADR-0024's amendments.

**How alike the skills of two normalized titles' vacancies are.** Cosine similarity of the share
of each title's vacancies that name each skill, with each skill weighted by how few vacancies of
the subset name it (inverse document frequency), so that Python and SQL count less. Titles are the
probe's cut of §1.

| Pair | Similarity |
|---|---:|
| `devops engineer` ~ `platform engineer` | 0.93 |
| `engineering` (mostly `engineering manager`) ~ `software engineer` | 0.93 |
| `devops engineer` ~ `site reliability engineer` | 0.91 |
| `engineer` ~ `software engineer` | 0.88 |
| `manager` ~ `director` | 0.87 |
| `software engineer` ~ `backend engineer` | 0.86 |
| `forward deployed engineer` ~ `software engineer` | 0.71 |
| `data analyst` ~ `data scientist` | 0.69 |
| `ai engineer` ~ `machine learning engineer` | 0.67 |
| `developer` ~ `software engineer` | 0.65 |
| `site reliability engineer` ~ `software engineer` | 0.63 |
| `solutions architect` ~ `software engineer` | 0.62 |
| `sales engineer` ~ `solutions architect` | 0.61 |
| `data engineer` ~ `data scientist` | 0.60 |
| `frontend engineer` ~ `full stack engineer` | 0.59 |
| `security engineer` ~ `devops engineer` | 0.57 |
| `data analyst` ~ `data engineer` | 0.57 |
| `software engineer` ~ `ai engineer` | 0.53 |
| `product manager` ~ `technical program manager` | 0.49 |
| `product manager` ~ `software engineer` | 0.47 |
| `director` ~ `software engineer` | 0.45 |
| `qa engineer` ~ `software engineer` | 0.43 |
| `data engineer` ~ `backend engineer` | 0.40 |
| `security engineer` ~ `network engineer` | 0.17 |
| `frontend engineer` ~ `data engineer` | 0.08 |

The most distinctive skills: `ai engineer` LangChain, LangGraph, PyTorch, Pinecone, MCP; `data
engineer` Spark, Airflow, Databricks, dbt, Snowflake; `frontend engineer` React, TypeScript, CSS,
Next.js; `software engineer` Java, React, TypeScript, Go, Kubernetes. So the skills tell the kinds
of work apart, but not who does what in it: a manager's vacancies ask for an engineer's skills.

**Skills per vacancy.** 12.5% of the subset names no skill, 7.0% one and 7.1% two; 30.8% name ten
or more. For about a quarter, the skills say little about the work.

**The department field beside a bare leader.** Of the vacancies whose probe cut left a rank alone
(`manager`, `director`, `head`, `lead` and the like), every one on Greenhouse (2,435), Ashby (1,871)
and Lever (540) has a department or a team, and none of Workday's 4,712 does. The values are
coarse: `Engineering`, `Tech`, `Global`, `Shared Services Corporate`. The title's own area is the
better evidence (`Manager, Application Security`).

**Himalayas**, read on 2026-10-26 as the one other job site seen modelling roles. A posting carries
7 to 15 overlapping role tags that look generated: one Canonical posting has `Software Engineer`,
`Linux Engineer`, `Linux Software Engineer`, `Linux Software Engineering`, `Open Source Engineer`,
`DevOps`, `Backend Development`, `Developer` and more, and an Askable one mixes in employment type
(`Contract Software Engineer`, `Freelance AI Software Engineer`). Each tag is a browse page
(`/jobs/<tag>`). The browse page of a role splits it by level: Entry, Mid, Senior, Manager,
Director, Executive.

## 4. The normalized titles after #92's run

Read from the dev database after `2026-10-title-breakdown.md`'s last run, for the role list's
floor. A vacancy counts if any of its normalized titles is in the set.

| Normalized titles | Vacancies they cover | Fewest vacancies on one |
|---|---:|---:|
| Top 25 | 51,179 (32.9%) | 826 |
| Top 50 | 64,594 (41.5%) | 354 |
| Top 100 | 76,467 (49.1%) | 176 |
| Top 200 | 87,840 (56.4%) | 79 |
| Top 500 | 100,518 (64.5%) | 27 |

| Floor | Titles at or over it | Vacancies they cover |
|---:|---:|---:|
| 20 | 658 | 104,053 (66.8%) |
| 12 | 1,039 | 109,709 (70.4%) |
| 10 | 1,276 | 112,163 (72.0%) |
| 8 | 1,601 | 114,867 (73.7%) |
| 5 | 2,633 | 120,794 (77.5%) |

At floor 12 the titles reach 70.4%, so the 70% holds only if nearly every one of them fits a role.
