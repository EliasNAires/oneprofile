# The work mode read from the description (#98)

2026-10-30, on the dev snapshot, with the rule of `0aa2e61` (`WorkModeRule`): `POST /work-modes` over the
engineering subset, 155,767 vacancies, in about 3 minutes. The description is read last, only where the
location, the field and the title are all silent, and a choice keeps every mode it offers.

**Outcome.** Precision passes (97.7%, 96–99%). Remote recall on undeclared vacancies fails (56.6%,
44–73%, against 90%): most of the remote vacancies the rule misses are offered as a choice whose remote
option it does not read. By the issue's decision 3, #98 becomes a rule loop.

## Coverage

By the step of the order of evidence that decided it:

| ATS | vacancies | with a mode | share | location | field | title | description |
|---|---:|---:|---:|---:|---:|---:|---:|
| Workday | 78,913 | 19,914 | 25.2% | 4,251 | — | 1,619 | 14,044 |
| Greenhouse | 41,727 | 16,265 | 39.0% | 8,628 | — | 353 | 7,284 |
| Ashby | 22,575 | 20,679 | 91.6% | 3,080 | 17,370 | 23 | 206 |
| Lever | 12,552 | 12,354 | 98.4% | 779 | 11,573 | 2 | — |
| all | 155,767 | 69,212 | 44.4% | 16,738 | 28,943 | 1,997 | 21,534 |

**The share of the subset given a mode** rose from 30.6% (#86, `2026-10-work-mode.md`) to 44.4%. Workday's
`remoteType` is still absent from the snapshot. 86,555 vacancies have no mode.

## ADR-0029's count

`scripts/draw-work-mode-description-sample DIR --count`. A vacancy is **undeclared** where its location, field
and title name no mode. **Placed** is an undeclared vacancy whose location options name only cities,
subdivisions or countries and whose description states a mode; the share is how many of those the rule reads
as remote.

| ATS | undeclared | the description gives a mode | placed | placed, remote | share |
|---|---:|---:|---:|---:|---:|
| Workday | 73,043 | 14,044 | 13,761 | 1,450 | 10.5% |
| Greenhouse | 32,746 | 7,284 | 7,117 | 1,386 | 19.5% |
| Ashby | 2,102 | 206 | 186 | 55 | 29.6% |
| Lever | 198 | 0 | 0 | 0 | — |

These shares are the rule's, and its remote recall is below the gate, so they understate remote. #103 should
not use them until the loop passes. Workday's 10.5% sits on the 10% line.

## The measurement

A blind sample: `scripts/draw-work-mode-description-sample` and `scripts/score-work-mode-description-sample`
into `2026-10-work-mode-description-sample/`, against `docs/work-mode-criterion.md`. Its files: the draw
(`sample.tsv`), each stratum's size (`strata.tsv`), what the labeller saw (`batch-*.txt`), the labels
(`labels-*.tsv`), and the disagreements with their verdicts (`disagreements/`).

**How it was drawn.** Undeclared vacancies only, seeded (98), in five strata:

| stratum | vacancies | drawn |
|---|---:|---:|
| the description gives remote | 2,833 | 40 |
| the description gives hybrid | 11,540 | 40 |
| the description gives onsite | 6,863 | 40 |
| the description gives a choice | 298 | 15 |
| silent remote: no mode read, the description names a remote word | 14,322 | 65 |

**Who labelled.** Labelling followed ADR-0029. A labeller saw the company, title, location and field, and
only the description's segments with a mode cue, under their headings. It gave one answer per row, in
batches of 50, with one labeller per batch. Each batch was labelled by a Sonnet agent with Read and Write
only (`.claude/agents/work-mode-labeller.md`). Batch 1, the pilot, ran as a general-purpose agent instead.

The rule and the labeller split on 26 rows. A row split where the label did not hold every mode the rule
gave, or where it held remote and the rule did not. Those 26 went to the adjudicator, with the two answers
bare and in random order. It sided with the labeller on 22 and the rule on 4, and chose neither on none.

3 labels were unclear and are left out.

**Cost.** Labelling took 192k subagent tokens: 78k for the pilot and 37–39k for each of the other three.
Adjudication took 52k, for 244k in all against the ~150k ADR-0029 aims for. The pilot's general-purpose
agent cost twice what the Read/Write agent did. A loop round on the Read/Write agent alone would cost about
200k.

## Precision: passes

A row is right where its settled label holds every mode the rule gave.

| stratum | right | share | 95% |
|---|---:|---:|---|
| remote | 36 of 38 | 94.7% | 83–99% |
| hybrid | 40 of 40 | 100.0% | 91–100% |
| onsite | 39 of 40 | 97.5% | 87–100% |
| choice | 6 of 15 | 40.0% | 20–64% |
| every row | 121 of 133 | 91.0% | 85–95% |
| **weighted by stratum size** | | **97.7%** | 96–99% (bootstrap) |

The choice stratum is weak, but it is 298 vacancies, 1.4% of those the description decides. Its 9 misses:

- **A hybrid schedule read as hybrid or onsite** (4). Two Parametric postings say `office 3 days a week and
  choosing to work remotely or in the office the remaining days`. Two more give hybrid schedules that name
  onsite days at a client. All four are one mode, hybrid.
- **A template listing every mode** (3). Coupang's `Type of work model: Hybrid / Onsite / Remote` is
  followed by a hybrid schedule, and another posting lists `on-site/hybrid/remote` and then asks for
  `onsite 3-4 days per week`. These are the known miss of template labels, more common than judged.
- **A negation** (1): `On-Site position, no remote or hybrid option` read as a choice.
- **Primarily remote, on site as needed** (1), read as remote and hybrid. It is remote.

The single-mode strata miss 3:

- `4 days in office` with `Hybrid work environment` read as onsite. It is hybrid.
- Office days plus a flexible day read as remote. It is hybrid.
- A stray `LI-Remote` tag against an office statement. It is onsite.

## Remote recall on undeclared vacancies: fails

Of the undeclared vacancies whose settled label holds remote, the share the rule reads as remote, weighted
by stratum size. Undeclared vacancies whose description names no remote word are **assumed not remote**: the
sample does not read them.

| stratum | judged | remote | the rule gives remote |
|---|---:|---:|---:|
| remote | 38 | 36 | 36 |
| hybrid | 40 | 0 | 0 |
| onsite | 40 | 2 | 0 |
| choice | 15 | 5 | 5 |
| silent remote | 64 | 8 | 0 |
| **weighted** | | | **56.6%** (44–73%, bootstrap) |

The silent stratum carries the result. 8 of 64 are remote, which weighs ~1,790 missed vacancies against
~2,780 found. Its other 56 are 32 none, 18 hybrid and 6 onsite: the rule also misses hybrid and onsite there,
outside the gates.

### The misses, by pattern

- **A choice that offers remote, unread or read as one mode** (7 of 10):
  - `Hybrid in Towson, MD or New Britain, CT, or Remote`
  - `Work location is variable with onsite, hybrid, or remote options`
  - `Portugal (Hybrid or Remote)` under `Location options`
  - `Hybrid work in Prague … or remote in CZ/SK` under `Work Perks & Flexibility`
  - `Office-Based (Bengaluru) / Home-Based`
  - `onsite at the NIH-NCBI … and/or remote work`, read as onsite
  - `This role is on-site in our Fort Worth, Texas (or remote in the US) location`, read as onsite

  Most of these name a remote option in a different sentence shape from the one the rule reads as a role
  statement.
- **A bare label in a job-details list** (1): `Full-time / Remote` under `Job Details`.
- **`Home` as a location** (1): `Home - UK- England`. This is the location step's miss, not the
  description's: `Home` names remote there.
- **A statement about the whole team** (1): `The teams are all home based in different countries`. It counts
  by the criterion's rule 1, as it plainly covers every role.

### The 3 unclear

- An `on-site role` that is later `eligible for a remote work arrangement`. The rule read it as remote.
- `Remote workdays` with flexibility possible, naming no arrangement. The rule read it as remote.
- A company-wide onsite rollout with carve-outs. This was a silent-stratum row.

## Known misses accepted in the rule

These were seen before the run and judged rare. The sample met one of them, the template label, 3 times
among 15 choices:

- a statement whose mode comes more than 10 words after `this role`;
- weekday schedules (`100% ON-SITE (Thursday to Monday)`) read as hybrid;
- template labels listing every mode, read as a choice of all three;
- `in person`, which is not a mode word.
