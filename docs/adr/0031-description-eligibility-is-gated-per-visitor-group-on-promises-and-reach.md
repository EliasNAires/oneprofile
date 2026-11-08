# Description eligibility is gated per visitor group, on promises and reach

Status: accepted

ADR-0029 gated the description's eligibility rules (#103) for Argentina alone, and its second gate
asked how many of the unknowns are closed to a reader. Both missed the product. The Finder's visitors
are not only in Argentina. And the unknown gate only matters if the Finder shows the unknown. The
rules cannot reach it either: a silent city abroad stays unknown by decision (ADR-0004, ADR-0029), and
it alone keeps the gate above its line (46 of 50 unknowns closed at revision 2). Decided by Elias on
2026-11-08, after the first calibration froze revision 2.

This supersedes ADR-0029's gates, its cap, and its Argentina-only labelling and gold set. The rest of
ADR-0029 holds: the cue segments, batches of 50, labels kept and reused, Sonnet, no second labeller,
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

X is set by Elias once the recalibration has measured reach at the rules as they stand.

Reach is estimated from two strata weighted by their sizes: the vacancies eligible for the group (the
promises' precision) and a draw of the rest, unknown and excluded, asked whether each is open. Open
vacancies are rare in the rest, about 2 in 50 for Argentina at revision 2, so that draw is larger than
the promises', or drawn first from rows with a region or `anywhere` cue and weighted back.

## The Finder

The Finder shows the eligible alone for good. The unknown is no longer meant to follow them once it
means unknown (ADR-0019's amendment of 2026-10-30): what a visitor loses is the open jobs they are not
shown, which reach measures.

## The labelling

**The cap** is **250k tokens a round**, labelling and adjudication together, for the three groups.
About 250 rows a round once rows shared by the groups' draws are labelled once.

## Consequences

- #103 is recalibrated before round 1: the criterion asks per group, the scripts draw and score per
  group with reach, and a new gold set is drawn across the groups. The first gold set
  (`eligibility-gold-set.tsv`, Argentina) is kept as it was.
- Round 0's Feedback stands for round 1; it is mostly about the perk's `anywhere` and the place stated
  for the role, which hold for every group.
