# The seniority pass's first run (#8)

2026-10-29, on `classified-2026-10-25`, with the rules as first built (`ExperienceRequirementRule`,
`SeniorityRule`): `POST /seniorities` over the engineering subset, 155,767 vacancies, in 4 min 35 s
(11 min 47 s on a cold database). Whether the levels are right is measured in #99; this file reports
what the pass reached.

## Coverage

| | vacancies | share of the subset |
|---|---:|---:|
| a level word in the title (before) | 58,354 | 37.5% |
| required years | 103,517 | 66.5% |
| a seniority level (after) | 118,612 | 76.1% |
| neither | 37,155 | 23.9% |

By level: entry 3,506, junior 8,453, mid 17,365, senior 65,168, staff 18,810, principal 5,310.
Principal comes only from titles, since the years stop at staff (8+).

## Numbered levels

4,182 titles name a numbered level (`II`, `III`, `IV`) and no level on the ladder. The required years
place 3,313 of them, **79.2%**; the other 869 state no experience requirement and keep no level.

## The title's level against the required years

43,259 vacancies have both. The title's level is the one kept; the years agree with it on 21,355
(49.4%), place it higher on 8,438 (19.5%) and lower on 13,466 (31.1%). By the title's level (rows)
and the level the years alone would give (columns):

| title | entry | junior | mid | senior | staff |
|---|---:|---:|---:|---:|---:|
| entry | 27 | 15 | 3 | 2 | 1 |
| junior | 228 | 124 | 81 | 20 | 5 |
| mid | 22 | 125 | 327 | 175 | 41 |
| senior | 246 | 1,078 | 5,604 | 18,109 | 8,095 |
| staff | 17 | 50 | 232 | 1,762 | 2,768 |
| principal | 55 | 43 | 222 | 842 | 2,940 |

Two readings this run cannot tell apart, which #99's sample can: a senior title asking for 3–4 years
(5,604) is either a company calling mid-level work senior or a requirement read too low, and a senior
title at 8+ (8,095) is either a demanding vacancy or one stray large number read under highest-wins.

## What the rule reads

The experience requirements are read from the segments that are not boilerplate. A number of years
counts where what follows says what it is of (`of`, `in`, `with`, `building`, `relevant`,
`experience`, …) or where experience is named just before (`Experience: 4-7 years`). Left out:

- **Optional**: a segment whose words mark it so outside its parentheses (`preferred`, `is a plus`,
  `nice to have`, `bonus`, `desired`), or one under such a heading (`Preferred qualifications`,
  `Recommended qualifications`, `Nice to have`). `preferably` and `ideally` are not read as optional:
  in the corpus they qualify a part (`3+ years in DV, preferably in CPU verification`).
- **The company's own years**: a segment under a heading that introduces the company (`About
  <company>`, `Who we are`, `Why join`, `Benefits`), or one in the company's voice (`we`, `our`,
  `company`) with no word for the candidate (`you`, `candidate`, `required`, `seeking`, …). Without
  it, most requirements read at 20 years or more were company ages (`With over 25 years of
  experience, we help organizations…`, 20 of a read of 30); with it, 284 vacancies have 20 or more.
- **Numbers that are not experience**: years dated by the word before them (`after 2 years`, `spent
  18 years`, `for over 20 years`, `our 20+ years`), years of something else (`18 years of age`,
  `20+ years in business`), compounds (`a 4-year vesting`) and more than 25 years.

Alternatives give their lowest: phrases joined by an `or` that offers another path (`, or`, `; or`,
`OR`, an `or` before a degree or before the years), within a segment or across items ending in `or`
(`Bachelor's and 8 years; or` / `Master's and 6 years; or` gives 6). An `or` between what the years
are of (`5+ years of Java or Python`) joins nothing.

Reads of the requirements that set the required years (40 below 10 years, 40 at 10 or more) found
them requirements but for one alternative the first run joined wrongly (`5 years … with a Bachelor's
or 10 years … without a degree`, fixed: an `or` before the years now joins) and one it cannot tell
from a requirement: a substitution (`Eight years of relevant work experience may be substituted for
degree requirement`).

A review of the rule against the spec then fixed ranges joined by words (`between 3 and 5 years`,
`2 or 3 years`, which read their top), years in the business or of schooling (`4 years of college`),
`for 5+ years` after the work it is of, and an `Or` item joining a requirement further back than the
item before it; the figures above are after those fixes. Left as they are, for #99 to weigh:

- Optional is decided per segment, so `5+ years of Java; Kotlin preferred` drops the Java years (too
  low).
- An `, or` inside a list joins the requirements around it: `3+ years with AWS, GCP, or Azure and 6+
  years of Java` gives 3 (too low).
- The company's voice is read by its words, so an item such as `technologies we use: 5+ years Java`
  is left out (too low).

## Storage

`normalized_vacancy.seniority_level` and `required_years`, and one row per accepted requirement in
`normalized_vacancy_experience_requirement`: the segment's index, where the phrase starts in its
text, its lower and upper years (upper null outside a range), and whether it set the required years.
