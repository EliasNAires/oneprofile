# The seniority pass's first run (#8)

2026-10-29, on `classified-2026-10-25`, with the rules as first built (`ExperienceRequirementRule`,
`SeniorityRule`): `POST /seniorities` over the engineering subset, 155,767 vacancies, in 4 min 35 s
(11 min 47 s on a cold database). What the pass reached comes first; its
precision on a blind sample (#99) is under *Precision*.

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

## Precision

Measured on a blind sample (#99): `scripts/draw-seniority-sample` and `scripts/score-seniority-sample`
into `2026-10-seniority-sample/`, against `docs/seniority-criterion.md`. The draw (`sample.tsv`), each
cell's size (`cells.tsv`), what the labellers saw (`batch-1.tsv` to `batch-3.tsv`), their labels
(`labels-a-*.tsv`, `labels-b-*.tsv`), the disagreements and their verdicts (`disagreements/`).

**How it was drawn.** 20 vacancies of each cell, by where the level came from: the title, one cell per
level the required years give (entry, junior, mid, senior, staff), and no level; 140 in all. Then two
strata of 20 drawn first, where a level comes out too high: required years of 20 or more, and several
accepted requirements whose highest floor is not the lowest. 180 vacancies, seeded (99). The query
that drew them had no order, which the script now gives (`order by v.id`), so a re-run need not draw
the same 180.

Then a third stratum, drawn after the first figures to measure the experience requirements the rule
misses: 20 more vacancies with no required years from each of the two groups that hold them, no level
(37,155) and a level from the title (15,095), none drawn before (`--no-years`, into `batch-3.tsv`). 220
vacancies in all.

**Who labelled.** Both labellers were Sonnet, as one agent (`.claude/agents/seniority-labeller.md`)
run twice, blind, on every batch, with only the Read and Write tools. Per vacancy they read the
company, the title and the description's segments that name years or a level, never the rule's answer.
A Sonnet adjudicator settled the 2 they split on (both as a). About 445k tokens in all: 326k the four
labelling runs of the first 180, 24k the adjudication, 95k the two runs of the third stratum.

**Agreement: 218 of 220 (99.1%)**, between two runs of one model, so weaker evidence than the work
mode's two models. They agreed on all 40 of the third stratum. The splits: `Pleno` (Portuguese for mid) is not a level word of the criterion, and
`1 year … plus 2 years` is one path of two requirements, not two alternatives.

**Precision.** The share whose settled level is the rule's:

| | right | precision | 95% interval | too high | too low | years right |
|---|---:|---:|---|---:|---:|---:|
| every cell with a level, 20 each | 118 of 120 | 98.3% | 94% to 100% | 2 | 0 | 115 of 120 |
| weighted by cell size, with a level | | 98.9% | | | | |
| weighted by cell size, all | | 96.8% | | | | |
| title | 20 of 20 | 100% | 84% to 100% | 0 | 0 | 17 of 20 |
| years entry | 20 of 20 | 100% | 84% to 100% | 0 | 0 | 20 of 20 |
| years junior | 20 of 20 | 100% | 84% to 100% | 0 | 0 | 20 of 20 |
| years mid | 20 of 20 | 100% | 84% to 100% | 0 | 0 | 20 of 20 |
| years senior | 20 of 20 | 100% | 84% to 100% | 0 | 0 | 20 of 20 |
| years staff | 18 of 20 | 90.0% | 70% to 97% | 2 | 0 | 18 of 20 |
| no level | 18 of 20 | 90.0% | 70% to 97% | 0 | 2 | |
| 20 years or more | 15 of 20 | 75.0% | 53% to 89% | 5 | 0 | 6 of 20 |
| several requirements | 17 of 20 | 85.0% | 64% to 95% | 3 | 0 | 16 of 20 |
| no years | 39 of 40 | 97.5% | 87% to 100% | 1 | 0 | 17 of 20 |

The strata are drawn from inside the cells, mostly years staff and title, so the weighted figures
already hold them at their size: 284 vacancies have 20 years or more.

**No level.** Of the 20 with none, 16 state nothing, 2 ask for years the rule missed, and 2 state a
level only in body words. The body's level words (`88e1037`) would place about one in ten of the
37,155 with no level, before the false positives they would bring.

**Missed years.** 52,250 vacancies of the subset have no required years. Of the 65 drawn (the third
stratum and the 25 of the cells), the share asking for years the rule did not read:

| group | vacancies | ask for years | share | 95% interval | weighted |
|---|---:|---:|---:|---|---:|
| no level | 37,155 | 2 of 40 | 5.0% | 1% to 17% | about 1,900 |
| a level from the title | 15,095 | 4 of 25 | 16.0% | 6% to 35% | about 2,400 |

About 4,300 in all, 8% of those with no years. In the no-level group each would gain a level, a level
too low today; in the title group the level stands and only the stored years are missing. The labellers
see only the segments that name `year` or `yr`, so a requirement written without either (`Experience:
3+`) is not counted here.

**Recall.** Of the vacancies that ask for years, the rule reads them in about 96%: 103,517 of about
107,800, between 90% and 99% from the two intervals. Of the vacancies that should have a level, it
gives one to about 98%: only the no-level group's misses count, since a title's level stands without
years, so 118,612 of about 120,500, between 95% and 99.7%. Both are upper bounds, by the segments the
labellers do not see. A level stated only in body words is not counted as missed, since the criterion
does not let it set the level; counted, it would bring the level's recall to about 95%.

## Where the rule and the labels split

29 vacancies, 13 by level (11 too high, 2 too low) and 16 by their years alone, the level being right.

**Too high (11).**

- **The company's own years (6).** Not left out because neither the heading nor the voice marks them
  as the company's: `With millions of diners … and 25+ years of experience, OpenTable … is an industry
  leader` (the company named, not `we`); `Its track record is built on more than 20 years of industry
  experience` under `What We Offer`; `DXC … has over 20 years of Enterprise Service Management
  Experience`; `Microsoft Global Alliance Partner of the Year (14 years in a row)`; two team bios
  (`25+ years in Finance, Strategy, M&A`). Five of the six are the 20-years stratum's misses, which
  makes this the cause of most of what reads at 20 years or more.
- **An alternative the rule does not join (3).** `BS + 5 years of exp …; MS + 3 years of exp.` (a `;`
  with no `or`); `10+ years … with a Bachelor's …, 7 years … with an Advanced degree …, 4 years … with a
  PhD` (commas); and two tracks of one posting, `Level 3 or 4`, each with its own qualifications.
- **A range without its dash (1).** `5 8 years of experience` reads as 8.
- **A level word before `Manager` (1).** `Sr. Manager, Software Engineering` is senior to the rule;
  both labellers read no level, since `Manager` is not on the ladder. Earlier in the sample they read
  `Sr. Solutions Director` as senior, as the rule does. The criterion does not say which holds: whether
  `Lead`, `Manager`, `Director` and `Head` being no levels takes the level word before them away.

**Too low (2).**

- **A heading read as the company's (1).** `About this roll* (Responsibilities)` matches the company
  headings' `About …`, so `2–4 years of professional backend software development` is left out.
- **The years after their words (1).** `Year of experience required- 4+`.

**Years wrong, level right (16).** The company's own years in 8 (`20 years of electro-optical and
infrared systems innovation`, `23+ years of expertise Gradion builds…`), alternatives not joined in 4
(`Also acceptable are a Doctoral degree (PH.D.) and seven (7) years`, `or an Advanced Degree … and
minimum 5 years`, an `or no degree + 21 years` path), a number written out before its digits not read
in 3 (`five (5) years`, `Twelve (12) years`, `Ten (10)+ or more years`), and `1-2 years in business
analysis` left out in 1, taken for years in the business. A title's
word decides these, so only the stored years are wrong.

**The three cases left by the first run.** None was found in the 220 (at most 1.7% of the sample, by
the interval): no level or years came out too low by an optional part decided per segment, an `, or`
inside a list, or a requirement in the company's voice. The two too-low misses are other causes.

## Is it enough

No pass mark was set ahead. The cells read 98.9% weighted where a level is given and 96.8% over the
subset; every cell is at 90% or more, and too high outweighs too low 11 to 2. Where the error was
expected, it is: the 20-years stratum reads 75% and several requirements 85%, both too high. Most of
that is the company's own years, which a wider company voice (the company's name, `its`, award and
team-bio lines) would remove; the rest are alternatives joined by `;` or commas without an `or`. About 8% of the vacancies with
no required years ask for some, half of them in a number written out before its digits (`Twelve (12) years`). Whether
this is enough, or seniority becomes a rule loop (PRD §12.6), is not decided here.
