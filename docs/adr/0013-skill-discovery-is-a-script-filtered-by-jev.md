# Skill discovery is a script whose candidates Jev filters, against a decision record

Status: accepted

The taxonomy was built in one-off steps (#37, #38, #40), and #40's pass cannot be repeated: its
miner passed 8,290 names, about 70% noise, and two rounds of subagents reading them all used
most of a five-hour usage window. Technology moves, so finding skills has to be a loop that can
be run again on each new snapshot (#41). This records its design, settled on 2026-10-06.

## A run

1. A **script** reads a snapshot and proposes **candidates**: names that are neither a key of
   `skills.tsv` nor in the decision record. Each comes with its spellings grouped, its
   document frequency, a category suggested from the known skills it sits beside, and three
   example segments.
2. **Jev** (ADR-0012) reads `docs/skill-criterion.md`, a candidate and its segments, and decides
   keep or drop; for a keep, it picks one of the criterion's twelve categories.
3. A **Haiku** agent drafts the `skills.tsv` row of each kept name: its id, canonical name and
   aliases, the canonical name and every alias a safe key.
4. A **session** reviews the drafted rows for names that are an existing skill under another
   spelling, unsafe keys, and junk Jev let through, appends them to `skills.tsv`, and appends
   every decision, drops included, to the decision record.

The script is developed as a rule loop (#54) until it passes the gates below. After that,
re-running it on a new snapshot *is* the loop, and the script stays in `scripts/` for good,
unlike #37–#40's, which were deleted once the taxonomy was built.

## Jev filters the output, so recall is the gate

ADR-0007 keeps a language model off the pipeline, meaning the path every vacancy travels at
runtime. Curating the taxonomy is offline work done once per snapshot, which subagents already
did in #38 and #40; Jev doing it does not put a model on any vacancy's path. Jev is the paid API
ADR-0007 allows, and this shares #11's account and budget.

Since Jev reads everything the script proposes, the script's precision bounds cost and noise,
not the quality of the taxonomy. A name the script never proposes, though, is lost for good. So
**recall is the gate that matters**, and precision only has to be good enough to keep Jev's
input from being mostly noise.

## The gates

Scored each round by `scripts/score-skill-round` (#53), with Jev as the labeller:

- **Recall at or above 90%**: the share of skill-bearing segments in which every software skill
  named is either a key of `skills.tsv` or a candidate. The main gate. The segments are
  requirement segments drawn from the IN vacancies and the pile in proportion to their size,
  boilerplate skipped.
- **Precision at or above 50%**: the share of candidates kept, by the decision record where it
  has the name and by Jev otherwise.
- **At most 1,000 candidates a run**, so the Haiku drafter and the reviewing session read about
  500 rows at the precision gate.
- **Six rounds** at most, whatever the numbers say; the loop then stops and reports where it
  stopped.

## What is mined

The IN vacancies and the pile (ADR-0012), in English and Spanish only. The pile is mined too
because the criterion decides what a skill is by whether a software person is hired for it,
wherever the ad comes from: AutoCAD or Epic EHR found in the pile are drops, not misses, and a
software technology named in an OUT-leaning ad is still a skill. The run reads
`classified-2026-10-06.dump`, the snapshot ADR-0012 fixed.

## The decision record

`src/main/resources/taxonomy/decisions.tsv`, append-only.

- `skills.tsv` stays the truth for **what the skills are**, and is curated by hand; the decision
  record is the truth for **what has been seen**. Every key of `skills.tsv` counts as seen
  without a row.
- **One row per name form**: the name lowercased, each run of spaces and hyphens between two
  other characters collapsed into one space, leading and trailing spaces trimmed, every other
  character kept. `C`, `C++`, `C#` and `C--` stay apart; `Spring-Boot` and `spring boot` are one.
- Each row carries the **decision**, the **criterion revision** and the **snapshot** it was decided
  under, and its **source**: `#38`, `#40`, `jev` or `session`.
- **A drop is final**, unless the criterion changes and the reviewer of the new revision names
  that drop's reason as one it reopens. A keep means the name is a skill, under its own name or
  as another spelling of one.

The decision record is **seeded once** from `docs/measurements/taxonomy-review-2026-09-24.tsv`
(#38) and `taxonomy-mined-review-2026-09-24.tsv` (#40), under revision `0`, the rules those reviews
applied before the criterion was written, and the snapshot `raw-2026-09-20`, whose corpus they
read. Its columns are `name` (the name form), `decision`, `revision`, `snapshot` and `source`.
The seed leaves out:

- **The 334 names #40 left unreviewed.** The archive writes them as drops, but no one decided
  them; they join the first run's candidates (#55).
- **The 24 keeps whose skill never reached `skills.tsv`**, being named in fewer than three
  descriptions (Apache Ant, Concourse CI, Knockout.js): a keep in the decision record has to
  mean a skill the taxonomy holds, so these are proposed again.
- **The 9 drops whose name form is a key of `skills.tsv`** (`Curl`, `Microsoft Word`, `Redux
  Toolkit`): the name was kept elsewhere in the reviews, or added since, and `skills.tsv` wins.

Name forms the archives repeat (Genie, Delphi) are seeded once; none of them disagree. `screened
out` drops of #40 are seeded as drops: screened on the name alone, they rarely dropped a real
skill (three of 5,702 are keys of `skills.tsv` from another reviewer), and recall shows if they
did. The seed holds 9,140 rows, 2,419 keeps and 6,721 drops. The file has a header row and no
comments, since a name form may begin with `#` (`#kubernetes`).

## Skills that fade are never removed

A skill whose document frequency falls is kept, and nothing is recorded about it: profiles may
still name it, and zero demand in today's ads is already visible from the ads. A dormant flag
waits until the profile picker needs one.

## Budget

Jev shares #11's account and its **$5 hard limit**; #11 projects under $1. A candidate is about
2.5k input tokens with the criterion, so 1,000 candidates cost about $0.10. Every script that
calls Jev records each response's `usage`, keeps a running total across both issues, and stops
at **$4**.

## Considered options

- **Subagents reviewing every mined name**, as #40 did. Rejected on cost: it used most of a
  usage window for one pass.
- **A script precise enough to need no reviewer**, gated on precision. Rejected: tightening
  precision costs recall, and a name the script misses is never seen again, while a noisy name
  Jev drops costs a hundredth of a cent.
- **`skills.tsv` regenerated from the decision record**, as `taxonomy_select.py` regenerated it
  from the reviews. Rejected: `skills.tsv` is curated by hand since #38, and a loop that
  rewrites it would undo the curation. The decision record only says what has been seen.
- **Removing skills whose demand fades.** Rejected, see above.

## Consequences

A run of this loop changes the input of #11's body rules (ADR-0012), which read `skills.tsv`;
the next #11 round names the `skills.tsv` commit it ran against.

The criterion now drops office software and technologies no software person is hired for, which
`skills.tsv` already holds some of (AutoCAD, Microsoft Word, Microsoft Excel, Google Workspace).
They are left as they are: `skills.tsv` is curated by hand, and removing them is a curation
decision, not one a run makes; #56 makes it.

This amends ADR-0007's scope for skill discovery only, as ADR-0012 does for #11.
