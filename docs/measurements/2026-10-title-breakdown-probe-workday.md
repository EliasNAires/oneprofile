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
