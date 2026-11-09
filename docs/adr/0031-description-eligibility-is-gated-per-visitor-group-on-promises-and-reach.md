# Description eligibility is gated per visitor group, on promises and reach

Status: accepted

ADR-0029 gated the description's eligibility rules (#103) for Argentina alone, and its second gate
asked how many of the unknowns are closed to a reader. Both missed the product. The Finder's visitors
are not only in Argentina. And the unknown gate only matters if the Finder shows the unknown. The
rules cannot reach it either: a silent city abroad stays unknown by decision (ADR-0004, ADR-0029), and
it alone keeps the gate above its line (46 of 50 unknowns closed at revision 2). Decided by Elias on
2026-11-08, after the first calibration froze revision 2.

This supersedes ADR-0029's gates, its cap, and its Argentina-only labelling and gold set. The rest of
ADR-0029 holds: the cue segments, batches of 50, labels kept and reused, no second labeller,
five rounds, and the silent-city count's outcome.

## The visitor groups

- **LATAM**: Argentina, Brazil, Mexico, Colombia, Chile.
- **US**: the United States.
- **English-speaking**: the United Kingdom, Canada, Australia, New Zealand, Ireland.

A gate is held per group, not per country. The labeller reads a row once and answers for every
group. How a group's answer is given over its countries is the recalibration's to write into the
criterion.

## The gates

A round holds when, adjudicated, for each group:

1. **Promises.** Of the vacancies the description makes eligible for the group, at least **90%** are
   eligible by the criterion's rule verdict. Unchanged from ADR-0029 but for the group.
2. **Reach.** Of the vacancies really open to someone located in the group, by the criterion's reader
   verdict, at least **X%** are shown as eligible. If 1,000 jobs would pay someone in Argentina and
   the Finder shows 650 of them, reach is 65%. It counts the eligible from every source, the location
   and the title included, since that is what a visitor is shown.

X is not a gate for now: see *Reach guards, it does not gate* below.

Reach is estimated from two strata weighted by their sizes: the vacancies eligible for the group (the
promises' precision) and a draw of the rest, unknown and excluded, asked whether each is open. Open
vacancies are rare in the rest, about 2 in 50 for Argentina at revision 2, so that draw is larger than
the promises', or drawn first from rows with a region or `anywhere` cue and weighted back.

## The Finder

The Finder shows the eligible alone for good. The unknown is no longer meant to follow them once it
means unknown (ADR-0019's amendment of 2026-10-30): what a visitor loses is the open jobs they are not
shown, which reach measures.

## The labelling

**The labeller and the adjudicator run on Codex** (Sol, `docs/agents/delegation.md`), following
`.claude/agents/eligibility-labeller.md` and `.claude/agents/adjudicator.md`, in place of ADR-0029's
Sonnet agents. Decided by Elias on 2026-11-08, during the recalibration.

**The cap** is **250k tokens a round**, labelling and adjudication together, for the three groups.
About 250 rows a round once rows shared by the groups' draws are labelled once.

## Consequences

- #103 is recalibrated before round 1: the criterion asks per group, the scripts draw and score per
  group with reach, and a new gold set is drawn across the groups. The first gold set
  (`eligibility-gold-set.tsv`, Argentina) is kept as it was.
- Round 0's Feedback stands for round 1; it is mostly about the perk's `anywhere` and the place stated
  for the role, which hold for every group.

## The recalibration (2026-11-08)

The criterion was revised to revision 4: every vacancy asked both verdicts for each group, a group's
verdict the best of its countries'. A gold set of 207 vacancies was drawn across the groups
(`eligibility-gold-set-groups.tsv`), 35 promises, 25 rest rows with a cue of the group and 10 without
per group, half held out, labelled and reviewed on Codex (one batch reviewed by Sonnet before the switch).

**Agreement on the held-out half, revision 4**: the labeller agrees with the gold set on 102 of 103
scored answers, **99.0%** (94.7–99.8%), and on all six verdicts of 89 of 101 rows (88.1%); the
adjudicator sided with the gold set on the one disagreement. The labeller and the gold set's reviewer
are the same model, so the figure is an upper bound on agreement with an independent reader.

**At the rules as they stand** (the gold set's own labels):

| group | promises eligible | reach |
|---|---|---|
| latam | 12/35, 34% (21–51%) | 67.5% (34–100%) |
| us | 21/35, 60% (44–74%) | 95.6% (86–100%) |
| english | 20/35, 57% (41–72%) | 82.2% (64–100%) |

Reach's intervals are wide because open rows among the rest are few and the uncued strata are large;
X is Elias's to set from them.

## Reach guards, it does not gate (2026-11-08)

Decided by Elias on 2026-11-08, from the recalibration's figures, with Astra's advice. Reach as measured
cannot carry a fixed line: its intervals run from 34% to 100% for latam, and it borrowed the description's
promises precision for every eligible vacancy. So, until the Finder launches:

- **Gate 2 is non-regression.** A round holds reach for a group when its reach is no more than **5
  points** below the group's **baseline**: reach scored as a round scores it, on the rules as they stood
  before round 1 (`eligibility-round-0`).
- **The targets** are **70%** for latam, **90%** for us and **80%** for english. Each round reports them;
  none decides whether it holds. Gate 1, promises at 90%, is unchanged.
- **Reach weighs precision by source.** Each round also draws 10 vacancies per group eligible from the
  location or the title, and reach uses their precision for those, the promises' for the description's.

After launch, reach becomes a fixed line at the targets, measured on a larger draw of the uncued strata
and a fixed sample, so that rounds compare. What this risks: the Finder may launch hiding open vacancies,
latam's most; non-regression keeps reach from falling, not from being too low.

**The baseline** (`eligibility-round-0.txt`, commit fae00fb, revision 4, seed 10300): reach **85.5%**
for latam (66–100%), **100%** for us (no open vacancy among the rest drawn), **84.2%** for english
(73–100%). Promises at the same rules: 31%, 51% and 54%.

## The third calibration (2026-11-09)

Round 2's review read rule 11 two ways against rule 12 for a partial timezone beside a place holding the
country (`remote within the United States; must reside in Eastern or Central time`), and us's gate 1
turned on it. Revision 5 rules that a window holding only some of a country's offsets neither holds nor
excludes it, as a city inside it would not, and that a zone named by a country is that country's alone.

**Agreement on the held-out half, revision 5**: the labeller agrees with the gold set on 102 of 103
scored answers, **99.0%** (94.7–99.8%), and on all six verdicts of 89 of 101 rows (88.1%), as under
revision 4; the adjudicator sided with the gold set on the one disagreement. The open half has none.

**Round 2 re-scored under revision 5** (`eligibility-round-2-revision-5.txt`, seed 10302): promises
91.4% for latam, 91.4% for us and 94.3% for english, reach at or within a point of each baseline. Every
group holds both gates, so the loop exits on round 2.
