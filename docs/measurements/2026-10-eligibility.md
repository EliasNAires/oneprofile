# Eligibility on a blind sample (#12)

2026-10-30, on `classified-2026-10-25`, with `CountrySetRule` after the audit's five fixes (below) and
`EligibilityRule` as #13 built it. The question is PRD §5.4's: is a vacancy's state for a country right, and is
what a visitor in Argentina is shown worth reading. Argentina is the reference country, the person's city
unknown; the United States gets one line.

The scripts are `scripts/draw-eligibility-sample` (audit, concentration, draw; it runs the application's own
`EligibilityRule` through `scripts/EligibilityStates.java`) and `scripts/score-eligibility-sample`, into
`2026-10-eligibility-sample/`, against `docs/eligibility-criterion.md`. Everything there: the audit
(`audit.tsv`), the draw (`sample.tsv`) and each stratum's size (`strata.tsv`), the strata checked with no
labeller (`checks.tsv`), what the first labeller saw (`batch-*.txt`) and its labels (`labels-a-*.tsv`), what the
second saw (`second-*.txt`) and its labels (`labels-b-*.tsv`), and the disagreements with their verdicts
(`disagreements/`).

## The results against their gates

| result | gate (Elias, 2026-10-30) | measured | then |
|---|---|---|---|
| eligible precision for Argentina | under 90% → a rule loop on its worst source | **78.4%**; the description **22%** | a rule loop on the description |
| the feed's share really open to Argentina | under 30% → change the default view or how unknowns rank | **6%** (2–16%) | change it |
| the unknowns really onsite or hybrid | over 50% → take up #98 | **48%** (31–66%) at random, 11 of 27 unclear | under, by a margin the sample cannot tell |
| the five largest companies' share of the eligible set | over 40% → widen the sources | **37.3%** | passes; one company holds 25% |

## 1. The sentence audit

The 100 texts that decided a state for Argentina for the most vacancies cover 19,417 of the 48,054 a stated set
decides: 12 eligible, 88 excluded (`audit.tsv`). The most frequent description-eligible texts were read too,
since none reached the top 100. Five were wrong, and were fixed in `CountrySetRule` test-first before the draw:

| text | vacancies | was | now |
|---|---:|---|---|
| `Applicants for employment in the US must have work authorization that does not … require sponsorship` | 500 | US authorization | nothing (some roles only) |
| `For US based roles only, please note the Company may not be able to employ … United States work authorization …` | 192 | US authorization | nothing (a condition) |
| `U.S. work authorization sponsorship (H-1B, TN, J) is offered exclusively for Principal-level roles` | 69 | US authorization | nothing (sponsorship) |
| `work from anywhere you like in the U.S, excluding U.S. territories` | 8 | Worldwide | the United States |
| `the freedom to work from anywhere in the world for up to a month` | 11 | Worldwide | nothing (a benefit) |

`Government Clearance Required? Yes` (101 vacancies, Jakarta) takes the options' country, Indonesia, as #13
chose; the criterion's rule 10 says so. After the fixes `POST /country-sets` (7 min 21 s) gives Argentina
**2,866 eligible, 44,833 excluded and 108,068 unknown** of 155,767, against 2,885, ~45,170 and ~107,790 before.

## 2. Concentration

The 2,866 eligible vacancies come from **535 companies**; the largest five hold **1,070 (37.3%)**:
bluelightconsulting 713, jobgether 163, Canonical 102, supabase 50, Sezzle 42. 237 companies offer one. **942
repeat another's company and title** (37 also its options), leaving 1,924 distinct postings: bluelightconsulting
posts each title once per Latin American city.

## 3. The sample

**How it was drawn.** Seeded (12), 350 vacancies in the strata of `strata.tsv`. A vacancy may sit in two
strata (the feed overlaps the others). The labellers read whole descriptions for the two recall strata (the
unknowns), and elsewhere the title, the options and the segments holding a broad cue, each with the segment
before and after it.

**Shrunk on the budget.** A whole description ran ~2,000 tokens, not the 1,400 planned, so four of the ten
recall batches were not labelled, as the method allows: **27 of the 50 random unknowns and 33 of the 50 cue
unknowns** were. Every other stratum was labelled in full.

**Who labelled.** Sonnet agents with Read and Write only (`.claude/agents/eligibility-labeller.md`), blind to the
rules. The first labelled all 300 labelled rows; the second, the same agent run again, labelled the 71 where the
first split from the rule for Argentina and 30 of the eligible strata where it agreed. They agree on **84 of 101**,
and on **29 of the 30** agreeing eligible rows (97%, 83–99%). A Sonnet adjudicator (`.claude/agents/adjudicator.md`)
settled the 17 splits: 14 for the first, 3 for the second. About 1.44M tokens of labelling: 1.04M the first labeller
(50–96k a batch), 338k the second, 62k the adjudicator; the audit, the criterion and the scripts besides.

**Checked with no labeller** (`checks.tsv`):

- **Excluded by an onsite or hybrid option: 10 of 10 parsed right.**
- **Timezone requirements: 31 of 34 right.** 250588 and 250596, one posting, list alternatives (`Europe &
  Africa: UTC-1 to UTC+3` / `The Americas: Eastern Time or UTC-3 (e.g., Brazil, Argentina)`); the rule read the
  first as a requirement and excluded Argentina, **two gems lost**. 1376036 (`Stakeholders are located across
  multiple time zones (GMT-8 to GMT+3), which may require … meetings`) is a schedule read as a requirement; it
  stays eligible from `work from anywhere`.

## 4. The rule against the settled label, for Argentina

With a 95% Wilson interval, each error by where the vacancy should have gone:

| stratum | vacancies | labelled | rule right | errors |
|---|---:|---:|---:|---|
| eligible, from the location | 1,547 | 50 | 44 (88%, 76–94%) | promise broken 6 |
| eligible, from the title | 763 | 50 | **50** (100%, 93–100%) | |
| eligible, from the description | 556 | 50 | **11** (22%, 13–35%) | promise broken 39 |
| unknown, at random | 108,068 | 27 | 24 (89%, 72–96%) | unknown, really excluded 3 |
| unknown, with a cue | 7,050 | 33 | 25 (76%, 59–87%) | unknown, really excluded 8 |
| the feed (eligible and unknown) | 110,934 | 50 | 37 (74%, 60–84%) | gem lost 3, unknown, really excluded 10 |
| excluded, a description location | 5,158 | 10 | 9 | excluded, really unknown 1 |
| excluded, authorization | 5,378 | 10 | 10 | |
| excluded, citizenship | 8,060 | 10 | 10 | |
| excluded, clearance | 11,336 | 10 | 10 | |

**Eligible precision for Argentina, weighted by source: 78.4%** ((44/50 · 1,547 + 763 + 11/50 · 556) / 2,866).
Two of the location stratum's six are the labeller's, not the rule's (below), which would make it 80.5%.

**The reader's verdict.** By the first labeller, whether someone in Argentina could get paid for the job:

| stratum | open | closed | onsite or hybrid by the text |
|---|---:|---:|---:|
| the feed | **3 of 50 (6%, 2–16%)** | 36 (72%) | 9 (18%) |
| unknown, at random | **0 of 27** (0–12%) | 25 (93%) | 13 (48%, 31–66%) |
| unknown, with a cue | 1 of 33 | 31 (94%) | 19 (58%) |

Over every labelled vacancy: of the 150 the rule makes eligible, the reader says yes to 113, no to 31; of the
110 unknown, yes to 4, no to 92. **No gem was found among the 60 unknowns**: the gems lost are the timezone
alternatives above and three feed rows.

**The United States**, by the first label only: the rule agrees on 250 of 300 (83%, 79–87%): promise broken 30,
gem lost 7, unknown but really excluded 12, excluded but really unknown 1. The broken promises are the
description's errors below, which hold every country. One gem lost is a clearance read for the wrong country (289237:
`Buckley SFB` placed in Great Britain, and the clearance then given the options' country although the sentence
names the United States).

## 5. What the rule misses: the work queue

**Promises broken by the description (39 of 50).** Two causes cover nearly all:

1. **A perk's `work from anywhere` read as Worldwide** (to unknown): a line under *Benefits* or *Why join us*,
   not a statement of where the company hires. `Remote-first culture – work from anywhere!`, `WFA (Work From
   Anywhere)`, `Work from anywhere: Remote-first Culture`, `Work from anywhere (Singapore office or Remote)`,
   `Workation Wonderland … 30 extra days`.
2. **A place stated beside the perk** (to excluded). The description's places are one set, so the perk's
   Worldwide holds every country even where the text says `based in Sweden`:
   - jobgether's template, `Our partner is looking for a … based in <country>` with `Work from anywhere
     globally` under Benefits: 13 of the broken promises, from Germany to Turkey. Where its benefits name a region (`within your
     home country in Canada, the United States, or Latin America`, `eligible locations across Europe and Latin
     America`), the union holds Argentina, and the adjudicator kept it eligible (397025, 398004).
   - `you must be based in the US`, `must reside in the Eastern Time Zone`, `Remote - United States or Canada`,
     `remote-first role based in the U.S.`, `only applies to the continental U.S.`, `This role is based in San
     Francisco`.

**Promises broken by the location (6 of 50).**

- **US state codes read as countries** (476374, 855458): `Remote, AR`, `AR - Work from home` is Arkansas in a
  list of states, and so are `MT`, `MA`, `CA`, `IN`, … beside them; `Remote, LA` is read as Los Angeles. 34
  vacancies are eligible for Argentina from such a string alone (`AR - Work from home` 11, `Telecommuter AR` 6,
  `Remote, AR` 4, …). The place rule's (#26).
- **A timezone requirement beside a listed Argentina** (242263): the options name 19 countries, Argentina among
  them, and the description `Time Zone: US Time Zones (EST–PST)`, which the adjudicator read as rule 11's
  requirement. The rule did not read it.
- **`AMER - Canada - Ontario`** (506456) reads AMER as the Americas while the place is Canada; 98 vacancies have
  an `AMER…` string read as the Americas. The criterion lists `Americas` as a region and says nothing of a
  prefix; the labellers took AMER for an org tag. A question for the criterion.
- **The labeller's, not the rule's:** `Global [remote]` (275520) is Worldwide by the place rule, but the
  criterion's list of regions does not name `Global`, so the labeller and the adjudicator kept it unknown; and
  318951 shows `Remote` five times, while the stored places come from Ashby's structured address (Brazil, the
  United States, Spain, Argentina, Uruguay), which the batch did not show.

**Unknown, really excluded: the feed's noise.** Where the role is, said without `must`:

- `This role is based in our San Francisco office`, `This position is located in Vilnius`, `This hybrid position
  will be located in Latina, Italy`, `THIS POSITION IS BASED IN BANGKOK`, `You'll work in Hyvinkää, Finland`,
  `Location: Bucharest, Romania`, `based in either our Nottingham or London offices`, `internship in Montreal`,
  `commuting distance of our hubs`, `On-Site position, no remote or hybrid option`.
- Required, and missed: `US and Puerto Rico Residents Only`, `This role can be filled anywhere in the United
  States`, `applicants from Canada in EST time zones only`, `open to applicants who can work in (or relocate to)
  … Detroit`, `100% Remote within Türkiye` (in the title), `Government of Canada Secret Security Clearance`, `A
  valid authorization to work in the U.S. is required`, `U.S. Citizen`, `US Citizenship`, and `TS/SCI with
  Polygraph` in a segment the clearance pattern skipped.

**Gems lost:** `We hire globally` under a *Location* heading (bjakcareer 224829, 226172, 226235), the timezone
alternatives (250588, 250596), and `Remote.com if based elsewhere` (camunda 227768, a reader yes the rule
verdict leaves unknown).

## 6. What the criterion leaves open

Where the labellers split or hesitated, for the next calibration:

- **Where the role is, without `must`.** Rule 7 asks for required wording; the labellers and the adjudicator took
  `This role is based in X`, `This position is located in X` and `Location: X` as required. It decides most of
  the feed's excluded rows.
- **A perk's `anywhere`.** Labellers read it as saying nothing; the criterion names only a time-bound benefit
  (`up to 30 days a year`).
- **Region names beyond the list** (`Global`, `AMER`).
- **The reader's `no` against `unclear`** for a lone undeclared city abroad: batches 2 to 4 said no, 13 and 16
  unclear.
