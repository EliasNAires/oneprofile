# Eligibility from the description is a rule loop, labelled cheaply

Status: accepted

#12 measured eligibility once (`docs/measurements/2026-10-eligibility.md`) and two of its gates
failed. Eligible precision for Argentina was 78.4%, under 90%: the title held 100% and the location
88%, but the description 22%, mostly a perk's `work from anywhere` read as hiring worldwide. And 6%
of what a visitor in Argentina is shown was really open to them: most unknowns are closed by their own
text. So the description's eligibility rules become a rule loop (`docs/agents/rule-loop.md`, ADR-0014),
and the Finder shows the eligible alone until the unknown means unknown (ADR-0019, amended). Decided by
Elias on 2026-10-30.

The loop is blocked by #98: with the work mode read from the description, an onsite or hybrid
vacancy's declared location is its country set (ADR-0027), so the rules can assume where the job is
and read only where the company hires. The location's own misses (a US state code read as a country)
are #102's, not this loop's.

## The gates

A round holds when, adjudicated:

1. **Promises.** Of the vacancies the description makes eligible for Argentina, at least **90%** are
   eligible by the criterion's rule verdict.
2. **Truly unknown.** Of the vacancies unknown for Argentina, at most **30%** are closed to someone
   in Argentina by the criterion's reader verdict.

Gate 2 is judged as a reader sees it, so it counts what the rules cannot reach by decision. A remote
option in another country with a silent description (`Remote - US`) stays unknown by ADR-0004 and
reads as closed; it was about 2 of #12's 27 random unknowns. Gems lost into unknown are reported, not
gated: #12 found none among 60 unknowns.

**The cap** is **five rounds**, whatever the numbers.

## A silent city abroad

About 10 of #12's 27 random unknowns name a city or country in an undeclared option, and their
description says nothing of the mode or the place (`Chennai [undeclared]`). A reader takes them for an
office; no rule can read that from the text, and by ADR-0004 an undeclared location never counts
against a country. They alone hold gate 2 above 30%.

They are settled by one count, once #98 has passed its gates, including its recall of remote on
undeclared vacancies: among the undeclared vacancies whose options name only a city or country and
whose description states a mode, the share that is remote, **per ATS**. Where an ATS's share is
**10% or less**, a silent city of that ATS counts as an office: its declared location is its country
set, as an onsite one's is (ADR-0027). Elsewhere it stays unknown. The next calibration after #98
closes runs the count and writes the outcome here.

The count leans safe: a remote job usually says so, an office job often does not, so the silent
vacancies are likely remote less often than the ones that state a mode. What would make it unsafe is
#98 missing remote statements, which is why its remote recall is part of the condition.

## The labelling

#12's labelling cost about 1.44M tokens, 3.5k a row: whole descriptions, five answers a row and a second
labeller. A round here spends at most **150k tokens**, labelling and adjudication together, so:

- **The labeller reads little.** The title, the options with their work modes, and only the
  description segments with a cue (a place or a region, `remote`, `office`, `onsite`, `hybrid`,
  `based`, `located`, `relocat`, `authoriz`, `citizen`, `clearance`, `time zone`, `UTC`, `GMT`,
  `anywhere`, `hire`), each under the heading it sits in. No neighbouring segments. The draw prints each
  batch's size before it is launched; a row averages under 700 tokens.
- **One answer a row.** A promise row gets its rule verdict for Argentina (`eligible`, `excluded`,
  `unknown`); an unknown row its reader verdict for Argentina (`open`, `closed`, `unclear`). No United
  States, no mode, no reason beyond the phrase quoted.
- **Batches of 50**, so the criterion and the agent's own instructions are read once per 50 rows.
- **Each text once.** A row whose shown text matches one already labelled under the same revision
  takes that label (one company's template repeats across countries).
- **Labels are kept.** Each is stored with the revision it was made under and reused by any later
  round that draws the vacancy again with the same shown text. Only new rows cost.
- **No second labeller.** The adjudicator settles where the labeller and the rules split, as in every
  loop.
- **Sonnet**, with Read and Write only.

A round draws **50 promises and 50 unknowns**, seeded, from the rules' current output: about 100k for
labelling and 30k for adjudication. Each round's Record reports what it spent.

**The final measurement.** When a round's adjudicated figures hold both gates, the round is widened
before the loop exits, since near the gate a tighter interval is worth more: more rows are drawn into
both strata, past the 150k cap, as many as the round comment states and sizes beforehand.

## The gold set

Drawn as a round draws, at the rules as they stand when calibration runs: 50 promises and 50 unknowns,
half of each held out. Labelled by a Sonnet labeller with its close calls noted and corrected by an agent
review (the project's practice since #26), not by Elias.

The calibration that freezes the first revision takes up what #12 left open in
`docs/eligibility-criterion.md`: whether a place stated for the role without `must` (`This role is based
in our London office`, `Location: Bucharest`) is required; that a perk's `anywhere` says nothing; region
names beyond the list (`Global`, `AMER`); and the reader verdict of a silent city abroad, which for
gate 2 is closed.

## Considered options

- **#12's method each round**: about 1.4M tokens, half a session's budget. Rejected on cost.
- **Haiku labellers**: cheaper per token, but the project labels with Sonnet only, for its reading of
  long criteria.
- **Excluding a silent city now**: it would hide a remote job whose location field is a city, the worst
  error for a Finder whose pitch is jobs one can get, on a guess. Rejected for the count above.
- **Gating only what the text can fix**: reachable, but it would pass while the feed still shows silent
  cities as unknown.

## Consequences

- The Finder's default stays the eligible alone until gate 2 holds.
- #98 is worked before this loop, and its measurement reports remote recall on undeclared vacancies.
- A loop's cost grows with the new rows it labels, not with its rounds; the same shape is meant for
  #98's loop.
