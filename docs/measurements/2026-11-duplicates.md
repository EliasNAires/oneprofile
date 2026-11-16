# The vacancy-group threshold (#27)

2026-11-16, on the dev snapshot, with the vacancy-group pass as built in `a5bfa51`. The pass groups the
engineering subset's vacancies (155,767) by company, title and description, and marks duplicates
(`CONTEXT.md`: **Duplicate**, **Vacancy Group**). Its one free number is the threshold of description
overlap at which two vacancies join. This file sets it from a blind labelled sample of candidate pairs,
as #27's *Measured once* asks: the gate is **merge precision ≥ 95%**, recall reported beside it.

**Result.** Only a threshold of **1.0** passes: descriptions identical once places, pay, dates and
numbers are stripped. Held-out precision 95.0% (19 of 20, 95% interval 76% to 99%), recall 41%. Every
lower threshold falls far short (74% at 0.9, 67% at 0.8). `VacancyGroupRun.THRESHOLD` is now 1.0.

## How the pass was read

Interpretations made while building the pass (`a5bfa51`), stated here as the plan asked:

1. **Title key.** The pass compares vacancies within a bucket: the same slug, the same normalized titles,
   and also the same management level and title seniorities. #27's candidates are the wider frame (same
   slug and normalized titles); the pairs only the frame holds are sampled to measure what the narrower
   bucket loses (*Recall*).
2. **Location.** The set of declared location strings, compared as written, not the places read.
3. **Which duplicate is kept.** The board with the oldest posting, then that board's newest posting: the
   issue's "source over the copy" across ATSs, and "the newest on one board". The tie-break by open
   vacancies holds as built: closed vacancies are deleted from `vacancy` (`VacancyStore`), so
   `openVacanciesByCompany` counts only open ones.
4. **A duplicate's own description** must clear the threshold against the kept one, so a chain of
   single linkage can join a group but cannot hide a vacancy.
5. **Stripping.** Every location word of the bucket, numbers, dates and currencies are removed from each
   description before shingling (3-token shingles). A description under `MIN_SHINGLES` (20) never merges.

## The candidate pairs

`scripts/VacancyGroupPairs.java` lists every candidate pair: two vacancies of one slug with the same
normalized titles, 819,343 on the snapshot. For each it gives the **merge level**: the highest threshold
at which single linkage puts the two in one group, through other vacancies of the bucket or directly. At
any positive threshold t the pass merges a pair exactly when its level is at least t, so scoring levels
scores the merges the pass makes, transitive ones included. Its levels agree with every group the pass
stored at 0.8. The 298,441 pairs the bucket never compares (another management level or title seniority)
sit at level 0; it also gives each pair's own overlap.

## The sample

`scripts/draw-duplicate-sample` into `2026-11-duplicates-sample/`: the draw (`sample.tsv`), each
stratum's size (`strata.tsv`), each drawn vacancy's merge count at every threshold (`degrees.tsv`), what
the labeller saw (`batch-1.txt` to `batch-15.txt`) and its labels (`labels-1.tsv` to `labels-15.tsv`).
Seeded (27).

| stratum | pairs | vacancies | drawn |
|---|---:|---:|---:|
| frame (not in one bucket) | 298,441 | 52,652 | 20 |
| level 0 (one bucket, never joined) | 801 | 521 | 10 |
| level < 0.2 | 72,232 | 23,213 | 20 |
| 0.2 – 0.4 | 61,649 | 20,724 | 24 |
| 0.4 – 0.6 | 27,105 | 12,179 | 30 |
| 0.6 – 0.7 | 16,499 | 5,320 | 36 |
| 0.7 – 0.8 | 28,238 | 5,533 | 40 |
| 0.8 – 0.9 | 51,052 | 7,706 | 40 |
| 0.9 – < 1 | 123,139 | 16,756 | 40 |
| 1.0 | 140,187 | 19,157 | 40 |

**Drawn by vacancy, not by pair.** A bucket of n vacancies holds n(n−1)/2 pairs, so four companies
(`speechify`, `bjakcareer`, `bluelightconsulting`, Capital One) hold 75% of the pairs at level 0.7 or
more. A uniform draw of pairs would have measured those four. Each stratum is drawn instead by picking a
vacancy at random, then one of its pairs in the stratum. A pair (a, b) is drawn with probability about
n (1/d_a + 1/d_b) / V (n drawn, V the stratum's vacancies, d a vacancy's pairs in it), and weighted by its
inverse. The approximation is close where n is small against the stratum, as in every stratum here (the
nearest is level 0, 10 of 801).

**Calibration and held-out.** Each stratum's pairs are split at random in halves (all even): the
threshold is chosen on calibration and gated on held-out, so the reported precision is not the one the
threshold was chosen on.

**Blind.** Each pair is shown as A and B in a random order, each with its company, title, declared
locations, work modes and **whole** description, segment by segment. Not its ATS, date, overlap or level.
Two leaks the code review found after the labelling had started, left as drawn so the script still
reproduces the sample: pair numbers follow the strata's order (low numbers are frame pairs, high ones
level 1.0), though nothing tells the labeller so; and a company without a name is shown by its slug
(`CONTEXT.md` keeps a company's readable name from the ATS only).

## Who labelled

One labeller, Sol (`gpt-6.1-sol`) through `codex exec`, following `.claude/agents/duplicate-labeller.md`
against `docs/duplicate-criterion.md` (revision 1), one run per batch of 20 pairs, reading only the
criterion and its batch. About 1.05M Codex tokens for the 15 batches. No second labeller and no
adjudication: #27 asks for one blind labeller. Its answers were read for the high bands (below) and hold
up. Because it read whole descriptions, false merges the stripping causes show up as precision misses.

## Answers

| stratum | duplicate | same group | different position | unclear |
|---|---:|---:|---:|---:|
| frame | 0 | 1 | 19 | 0 |
| level 0 | 1 | 0 | 6 | 3 |
| < 0.2 | 0 | 0 | 20 | 0 |
| 0.2 – 0.4 | 0 | 1 | 22 | 1 |
| 0.4 – 0.6 | 0 | 4 | 26 | 0 |
| 0.6 – 0.7 | 1 | 10 | 24 | 1 |
| 0.7 – 0.8 | 4 | 13 | 23 | 0 |
| 0.8 – 0.9 | 2 | 15 | 23 | 0 |
| 0.9 – < 1 | 5 | 25 | 9 | 1 |
| 1.0 | 9 | 30 | 1 | 0 |

A pair is right to merge where it is a duplicate or the same group. An unclear pair (6) is left out.

## Precision and recall

`scripts/score-duplicate-sample`. **Precision by vacancy** is the gate. Each merged pair's weight is
its inverse inclusion probability times the mean, over its two vacancies, of 1/d, d being how many pairs
of its bucket the vacancy is merged in at t. So each merged vacancy counts once, as the share of its
merges that are right, whatever its bucket's size. Precision by pair (inverse inclusion alone) is given
beside it. Intervals are 95% Wilson on the Kish effective n of the weights. Recall is by pair over both
halves: of the pairs right to merge, the share merged, frame pairs included.

| t | calibration, by vacancy | held-out, by vacancy | held-out, by pair | recall |
|---|---|---|---|---:|
| 0.5 | 73.3% (61–83%, n 58) | 55.7% (43–67%, n 62) | 74.1% | 96.2% |
| 0.6 | 80.7% (69–89%, n 60) | 63.3% (50–75%, n 56) | 77.2% | 95.9% |
| 0.7 | 85.9% (74–93%, n 52) | 64.5% (50–76%, n 49) | 79.2% | 94.3% |
| 0.8 | 88.9% (76–95%, n 44) | 66.9% (52–79%, n 42) | 84.2% | 90.8% |
| 0.85 | 91.1% (78–97%, n 39) | 73.4% (57–85%, n 37) | 94.2% | 82.4% |
| 0.9 | 94.2% (81–98%, n 33) | 74.4% (58–86%, n 34) | 95.2% | 81.8% |
| 0.95 | 94.1% (79–99%, n 29) | 76.0% (59–88%, n 31) | 94.4% | 73.1% |
| **1.0** | **100% (84–100%, n 20)** | **95.0% (76–99%, n 20)** | 99.5% | 41.3% |

The lowest threshold whose calibration precision reaches 95% is 1.0. Its held-out precision is 95.0%:
**it passes, at the bar**. One held-out pair of 20 is wrong: two Anthropic *Staff* postings, one asking
for 7+ years and the other for 10+, identical once their numbers are stripped. By pair, which large
groups dominate, precision reads higher at every threshold (95% from 0.9).

**Why lower thresholds fail.** Of the 32 pairs at level 0.8 or more labelled different positions, 16
differ in level: `Engineer 4` against `Engineer 5`, `Level 4` against `Level 5`, `TCP_03` against
`TCP_04`, `1-3 years` against `5+ years`. Stripping numbers erases exactly what tells them apart. The
other 16 name another team, product, programme or specialty in a sentence or two of an otherwise shared
text (`Stockbroking app` against `Neobank app`, `Pretraining` against `reinforcement learning`).
Shingle overlap cannot weigh one such sentence above a page of shared template.

**The frame.** Of the 20 pairs the bucket keeps apart (another title seniority or management level),
19 are different positions: the narrower key loses little. The one that is a single position weighs
heavily in recall, since the frame holds 298,441 pairs.

## The pass at 1.0

`POST /vacancy-groups` at 1.0 over the subset: **155,767 vacancies, 142,335 groups, 4,088
duplicates**. 3,685 groups hold more than one vacancy (13,029 vacancies in all). At the 0.8 placeholder
there were 133,525 groups and 7,884 duplicates. The rerun split 8,810 of those groups, which took fresh
ids. A second run gave 0 new ids. Every vacancy of the subset has a group or a duplicate mark.

## Open

- **Recall.** At 1.0 the pass merges only bodies identical but for places, pay, dates and numbers. More
  than half the pairs labelled one position (59%, by pair) are left apart and are shown twice, as before the pass. Keeping
  level numbers and years in the comparison would address half the errors below 1.0. That would be a
  change of rule, measured again, not this measurement's.
- **The margin.** The gate rests on 20 held-out merged pairs at level 1.0, 19 right: the point estimate
  is at the bar and the interval reaches 76%.
- **The duplicate mark** (same location and modes) is not measured apart. The gate counts a pair the
  labeller called the same group but the pass marked duplicate as right, though that pair hides a
  vacancy.
- **Atomic publication.** Marks are written in batches, so a reader during a run can see a partial
  grouping. That is outside this measurement's scope; it matters once groups are published (#28).
