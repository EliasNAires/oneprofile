# How far engineering titles collapse when their facts are taken out

A probe run on 2026-10-22, before the role was decided (ADR-0024). It ran against the classified
snapshot `~/oneprofile-snapshots/classified-2026-10-22-lever.dump`, over its 76,854 vacancies
classified `IN`: 22,575 Ashby, 41,727 Greenhouse and 12,552 Lever. It was a throwaway Python
script over the cleaned titles, not a rule. Its regexes are rough, so read its numbers as
estimates.

## Distinct titles, one step at a time

Each step works on what the step before it left.

| Step | Distinct | Seen once | Top 20 cover | Top 100 cover | Top 300 cover |
|---|---:|---:|---:|---:|---:|
| Cleaned titles, lowercased | 40,622 | 33,452 | 10.4% | 19.1% | 26.8% |
| − `staff`, `mid`, `entry`, `level`, numbered levels; `remote`, `hybrid`, `onsite` | 38,396 | 31,277 | 12.4% | 22.1% | 29.7% |
| − words that also appear in its own location field | 37,012 | 29,784 | 12.8% | 23.0% | 30.8% |
| − every key of the skills extraction found in its title | 35,149 | 28,145 | 15.2% | 26.3% | 34.1% |
| − everything after the first head word | 12,309 | 8,915 | 43.1% | 63.6% | 72.6% |

The 1,000 most common cleaned titles cover 35.8%.

Taking facts out barely collapses titles. Most of the collapse comes from dropping the words
after the head, which mostly name a team, a product, a market or a place:
`software engineer authorization india`, `security engineer defi`.

## What the last step leaves

The 20 most common results: `software engineer` 12,401, `engineer` 1,819, `lead` 1,748,
`engineering manager` 1,449, `data engineer` 1,327, `developer` 1,324, `product manager` 1,303,
`data scientist` 1,241, `backend engineer` 1,121, `forward deployed engineer` 1,097, `manager`
1,074, `machine learning engineer` 1,041, `director` 995, `security engineer` 854, `devops` 804,
`technical program manager` 754, `ai engineer` 739, `full stack engineer` 713, `product engineer`
646, `solutions architect` 643.

Three faults of the probe that the real breakdown has to avoid:

- **The cut stops at a yielding head.** Bare `lead`, `manager` and `director` are mostly titles
  cut before the head behind them. The title classifier's head reading (ADR-0009) already handles
  these.
- **Some titles put the kind of work after the head.** About 5,781 cleaned titles match
  `engineer|developer` followed by a word such as `backend`, `frontend`, `platform`, `security`
  or `full stack`: `software engineer backend` 236, `software engineer full stack` 146,
  `software engineer frontend` 86, `software engineer infrastructure` 71. The pattern also
  catches some team names, so the count is high.
- **Synonyms are left apart**: `full stack engineer`, `full stack developer` and `full stack
  software engineer`; `solutions architect` and `solution architect`; `software developer` and
  `software development engineer`.

## Work mode in the subset

The work-mode field Ashby and Lever publish, against what their location text says:

| ATS | Field | Vacancies | Location says remote | Location says hybrid |
|---|---|---:|---:|---:|
| Ashby | OnSite | 6,777 | 7 | 2 |
| Ashby | Hybrid | 6,714 | 95 | 199 |
| Ashby | Remote | 6,603 | 2,370 | 10 |
| Ashby | (none) | 2,481 | 304 | 58 |
| Lever | remote | 5,049 | 661 | 2 |
| Lever | hybrid | 4,104 | 55 | 51 |
| Lever | onsite | 3,196 | 5 | 0 |
| Lever | unspecified | 203 | 1 | 2 |
| Greenhouse | (no field) | 41,727 | 7,459 | 906 |

The field and the location text disagree in about 170 vacancies. In a sample of 15 of them, the
location text was usually the right one: field `Hybrid` with location `Remote - USA`, `hybrid`
with `Remote`.
