# The title breakdown over the engineering subset

The first run of `TitleBreakdownRule` (#92), on 2026-10-26, against the dev database restored from
`~/oneprofile-snapshots/classified-2026-10-25.dump`. It covered the 155,767 vacancies classified `IN`
(41,727 Greenhouse, 22,575 Ashby, 12,552 Lever, 78,913 Workday), through `POST /title-breakdowns`,
in 2 min 50 s. The figures below were read back from the database. The probe this replaces is
`2026-10-title-breakdown-probe-workday.md`.

## How far the titles collapse

A vacancy whose title names two heads counts toward a top N if either of its normalized titles is in
it. **Head alone** is the share left with a normalized title that is a bare head (`engineer`,
`developer`, `analyst`, `architect`, `engineering`, `intern`, `specialist`, `consultant` and the
like), or with no normalized title at all, as a bare `Manager` is left.

| ATS | Vacancies | Distinct | Top 100 cover | Top 300 cover | Top 1,000 cover | Titles for 70% | Floor there | Head alone |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Greenhouse | 41,727 | 7,725 | 55.5% | 67.8% | 78.9% | 377 | 10 | 4.4% |
| Ashby | 22,575 | 3,410 | 67.4% | 79.3% | 88.9% | 127 | 20 | 4.7% |
| Lever | 12,552 | 2,794 | 57.4% | 72.5% | 86.0% | 247 | 7 | 6.7% |
| Workday | 78,913 | 20,046 | 41.6% | 52.4% | 63.5% | 2,093 | 4 | 5.5% |
| **All** | **155,767** | **28,362** | **48.3%** | **59.5%** | **69.8%** | **1,055** | **12** | **5.2%** |

370 vacancies (0.2%) have no normalized title, and 1,126 have two.

**The share left with a head alone fell from 12.9% to 5.2%**, against the probe's cut. The probe's
faults are fixed:

- Heads of more than one word stay whole: `member of technical staff`, `tech lead`, `chief of staff`.
- A leader's area is kept and its rank is the management level: `Director of Cybersecurity` →
  `cybersecurity`, `Head of Security` → `security`, `Engineering Manager, Backend` → `backend engineering`.
- A skill that names the kind of work stays: `Senior Android Engineer` → `android engineer`.
- Requisition codes, places, and test kinds after the head are handled: `Product Owner - R01572426` → `product owner`,
  `Remote, Latin America` goes, `Software Engineer in Test` → `test engineer`.

The top 300 cover less than the probe's 62.8%, and there are more distinct titles than its 27,427.
That is the price of the fixes. A kind of work moved in front, or a leader's area kept, splits
titles the probe's cut had merged: `backend engineer` against `software engineer`, `data
engineering` against `director`. Merging them is the role list's work (#25).

The 20 most common: `software engineer` 12,255, `data engineer` 3,309, `data scientist` 2,729,
`full stack engineer` 2,390, `ai engineer` 2,365, `backend engineer` 2,334, `engineer` 2,285,
`platform engineer` 1,657, `developer` 1,608, `engineering` 1,597, `machine learning engineer`
1,524, `devops engineer` 1,463, `forward deployed engineer` 1,378, `security engineer` 1,352,
`systems engineer` 1,289, `product manager` 1,259, `software engineering` 1,164, `site reliability
engineer` 1,097, `network engineer` 1,059, `data analyst` 1,032.

What is still bare:

- `engineer` and `developer` are mostly titles whose only word in front of the head was a skill
  (`Senior Python Engineer`, `Java Developer`), as ADR-0024 says they should be, or a team after it
  (`Principal Engineer, DSP`).
- `engineering` and `software engineering` are mostly managers' areas (`Engineering Manager`).

Workday's tail is long because its titles are specific, not because the breakdown fails on them:
`NASA Commercial Spacecraft Flight Software Systems Engineer`, `Guidance Nav & Control Engineer`.

## The guarded levels

The levels the breakdown reads from the words cleaning leaves. 13,703 vacancies (8.8%) name at
least one.

| ATS | `staff` | `mid` | `entry` | `II` | `III` | `IV` | Any |
|---|---:|---:|---:|---:|---:|---:|---:|
| Greenhouse | 3,573 | 208 | 37 | 1,034 | 274 | 34 | 5,135 (12.3%) |
| Ashby | 1,779 | 129 | 5 | 212 | 44 | 4 | 2,167 (9.6%) |
| Lever | 576 | 94 | 27 | 183 | 89 | 15 | 979 (7.8%) |
| Workday | 2,027 | 320 | 52 | 1,995 | 930 | 150 | 5,422 (6.9%) |
| **All** | **7,955** | **751** | **121** | **3,424** | **1,337** | **203** | **13,703 (8.8%)** |

`Chief of Staff` keeps its guard: it is a head, and records no level.

## The management levels

| ATS | Lead | Manager | Director | Executive | None |
|---|---:|---:|---:|---:|---:|
| Greenhouse | 2,479 | 2,226 | 982 | 215 | 35,825 |
| Ashby | 1,367 | 1,048 | 442 | 100 | 19,618 |
| Lever | 729 | 526 | 225 | 63 | 11,009 |
| Workday | 6,235 | 5,450 | 2,430 | 890 | 63,908 |
| **All** | **10,810** | **9,250** | **4,079** | **1,268** | **130,360** |

`head` is read as a director, and a chief or a vice president as an executive. A vice president, an
assistant vice president or a managing director beside a function head is a bank's corporate grade
(`VP System Analyst`, `Analyst, Vice President`). It is taken out and names no level.

## Choices the issue left open

How the rule read what #92 did not spell out:

- **Parts.** The title is read as published, cut at commas, brackets and spaced dashes, and each part
  is cleaned on its own. The cleaned title has lost those marks, and they are what says whether a word
  stands in front of the head or after it. A slash or an `&` joins two heads. Two heads side by side
  with nothing between (`Programmer Analyst`) are one compound head.
- **`software` gives way.** A kind of work moved from behind the head replaces a bare `software` in
  front of it: `Software Engineer, Backend` → `backend engineer`, as the issue's example reads.
  - It does not move where the front already names a kind (`Data Engineer, Platform` → `data
    engineer`).
  - Only its first run of kind words moves (`Team Lead, Android Core Product` → `android team lead`).
- **Skills that are the kind of work** are the platforms a title is a job of: Android, iOS, React
  Native, Flutter, Salesforce, ServiceNow, SAP and Workday. Languages and other tools go.
- **Ranks.** `lead` in front of a function head is the lead's level, and the head is kept (`Lead
  Analytics Engineer` → `analytics engineer`). `tech lead`, `technical lead` and `team lead` keep their
  words and record a lead. A rank with no function head leads an area. When nothing stands beside the
  rank, the area is the next part, or else the first (`Digital Transformation - Senior Manager`).
- **`associate`** is taken out as a corporate grade, as the probe classed it.
- **Spellings.** A plural head reads as its singular. `fullstack`, `back end`, `front end`, `sw`,
  `swe` and `mts` are written one way.
- **Storage.** The guarded levels are kept in their own table beside cleaning's, so that each pass
  replaces only its own. The numbered levels are in `SeniorityLevelEnum`, declared after the scale
  and given no place on it (#8).

## Run again: a leader of a kind of engineering

Decided with Elias the same day: a leader whose area is a kind of engineering is read as an
engineer of that kind. `Software Engineering Manager` → `software engineer` and `Head of Data
Engineering` → `data engineer`, each with its management level. A bare `Engineering Manager` keeps
`engineering`. The engineer it would read as names no kind at all, and which role it is belongs to
the role list (#25). In the same run, a spaced `&` stopped leaving `and` in a title: `Automation & AI
Engineer` is an `automation ai engineer` again. The levels are unchanged.

| ATS | Distinct | Top 100 cover | Top 300 cover | Top 1,000 cover | Titles for 70% | Floor there | Head alone |
|---|---:|---:|---:|---:|---:|---:|---:|
| Greenhouse | 7,569 | 56.7% | 68.9% | 79.6% | 341 | 11 | 4.4% |
| Ashby | 3,318 | 68.6% | 80.3% | 89.5% | 115 | 22 | 4.7% |
| Lever | 2,738 | 58.3% | 73.2% | 86.4% | 234 | 7 | 6.7% |
| Workday | 19,834 | 42.3% | 53.0% | 63.9% | 2,021 | 4 | 5.5% |
| **All** | **27,963** | **49.1%** | **60.4%** | **70.3%** | **986** | **12** | **5.2%** |

The tail is the normalized titles that fewer than 5 vacancies carry: 25,327 titles over 34,613
vacancies (22.2%). In a random 60 of them, about three in four are real titles that are simply
specific: `cyber resiliency analyst`, `mainframe zos systems administrator`, `multimodal data
engineer`. The rest are faults:

- **Company and programme names:** `Website Lead at Private Equity Insights`, `College to
  Corporate IT Internship`, and `Atlas Network`, whose `network` was moved in front as a kind of work.
- **`middle` as a level:** `Middle Full-Stack Engineer`, written for mid.
- **Words out of order:** `systems marketing operations`.
- **Titles not in English:** `Estágio em Engenharia de Análise de Dados`.

## A blind check of 200 vacancies, and its fixes

Run the same day. 200 vacancies were drawn uniformly from the subset, so the common titles weigh as
they do in the subset. Two agents labelled 100 each, blind to the rule. They read a criterion written
from #92, ADR-0024 and the decisions above, and wrote each vacancy's normalized titles and management
level. Every disagreement was then read and laid either to the rule or to the labeller.

| Output | Labeller agreed | The rule's faults | Debatable |
|---|---:|---:|---:|
| Normalized title | 171 of 200 | 7 (3.5%) | 3 |
| Management level | 198 of 200 | 2 (1%) | 0 |

The other 19 title disagreements were the labeller's:

- **Translations:** it translated `ingénieur` and `architekt`.
- **Spellings:** it kept `fullstack` and `sw` as written.
- **Abbreviations:** it missed that `FDE` is a forward deployed engineer.
- **Kinds of work:** it read a shorter list of them than the rule (`computer vision`).

The debatable three are kinds of work two parts behind the head, such as `Product Owner,
Technical - AI Finance`. They are left as they are, because reaching that far would also move a
company's name, as the `network` of `Atlas Network`.

Fixed:

- **A head after a leader's area.** A head in a later part no longer overrides the area a leader
  leads: `Manager, Software Engineering - Developer Support` → `software engineer`, MANAGER.
- **A company after `at`.** What a title names after `at` goes: `AI Engineering Lead at AIOS` → `ai
  engineer`, LEAD.
- **A chief's office.** `Chief Data Office` is a department, not an executive.
- **Grades beside a person.** A corporate grade beside a head that names a person (`specialist`,
  `quant`, `intern`) is a grade too: `Vice President - Inflation Quant` has no level. Such a head is
  also read before `support` or `operations`: `IT Support Specialist` → `it support specialist`.
- **More words for mid.** `intermediate`, `medior` and `middle` name mid (decided with Elias), except
  in `Middle East` and `Middle Office`.

One fault the labeller saw is left by choice. In `Software Engineer, AI & Data` only the first kind
moves (`ai engineer`). Carrying the whole run across a slash or an `&` was tried, and it split titles
that are one kind of work (`infra data engineer`, `simulation robotics engineer`), raising the
distinct titles from 27,948 to 28,223.

After the fixes:

| ATS | Distinct | Top 100 cover | Top 300 cover | Top 1,000 cover | Titles for 70% | Floor there | Head alone |
|---|---:|---:|---:|---:|---:|---:|---:|
| Greenhouse | 7,537 | 56.9% | 68.9% | 79.7% | 338 | 11 | 4.4% |
| Ashby | 3,326 | 68.6% | 80.2% | 89.5% | 115 | 21 | 4.7% |
| Lever | 2,723 | 58.5% | 73.3% | 86.5% | 231 | 7 | 6.8% |
| Workday | 19,844 | 42.3% | 52.9% | 63.8% | 2,029 | 4 | 5.5% |
| **All** | **27,948** | **49.2%** | **60.4%** | **70.4%** | **983** | **12** | **5.2%** |

`mid` is now named by 1,022 vacancies (751 before), and 13,972 (9.0%) name a guarded level. The
executives fell from 1,268 to 1,206, as grades beside a person stopped counting; the other levels are
unchanged.
