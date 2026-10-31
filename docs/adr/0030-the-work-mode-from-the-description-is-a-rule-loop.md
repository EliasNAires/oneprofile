# The work mode from the description is a rule loop

Status: accepted

#98 built the reading of the work mode from the description and measured it once
(`docs/measurements/2026-10-work-mode-description.md`). Precision passed at 97.7%. Remote recall on
undeclared vacancies failed at 56.6% (44–73%) against 90%: 7 of the 10 remote vacancies missed offer
remote as one option of a choice the rule does not read, or reads as one mode (`This role is on-site in
our Fort Worth, Texas (or remote in the US) location`). By #98's decision 3, a failed gate makes it a rule
loop (`docs/agents/rule-loop.md`, ADR-0014). Its gates were set by Elias on 2026-10-30. The rest he left to
the agent's judgment on 2026-10-31.

**Amended 2026-10-31, after round 1 (Elias).** The work mode is worth having in every form for the product, not
only for Argentina's eligibility. So hybrid and onsite recall become gates beside remote recall, and the draw
grows strata where hybrid and onsite misses can be seen. Round 1 stays scored under the gates it ran under. The
amendment takes effect from the recalibration after it.

## The gates

A round holds when, adjudicated, against `docs/work-mode-criterion.md`:

1. **Precision.** At least **90%** of the modes the rule reads from the description are right, weighted
   by stratum size. A row is right where its label holds every mode the rule gave.
2. **Recall on undeclared vacancies, for each mode.** Of the undeclared vacancies whose description says
   the role is remote, at least **90%** are read as remote. The same holds for hybrid and for onsite,
   each its own gate. Each is weighted by stratum size, and a choice counts for every mode it offers.
   An undeclared vacancy whose description names no word of a mode is assumed not of that mode, and is
   not drawn. The words are a remote word for remote; `hybrid`, or an office, onsite or in-person word,
   or days a week, for hybrid and onsite (see the draw).

The loop is about recall. Precision already passes and stays a gate so that recall is not bought with
it: a template label (`Hybrid / Onsite / Remote`) read as a choice raises one and lowers the other.

**Hybrid and onsite recall are gated (amended 2026-10-31).** Until round 1 they were reported and not
gated. A missed onsite or hybrid vacancy stays undeclared, so its eligibility stays unknown, which is the
safe side for #103. But the work mode is also a filter of the public explorer and of matching. A hybrid
or onsite vacancy left without a mode is missing from a filter on its mode. Round 1 left 31 hybrid and 12
onsite of the 100 silent vacancies it drew without a mode. Hybrid recall came to 72.0% and onsite to
77.4%, and both are overstated, since no silent vacancy without a remote word was drawn. Remote stays
the miss #103 rests on (ADR-0029).

**The rules are the whole of `WorkModeRule`**, not only its description step. A location-step miss
that counts against a gate (`Home - UK- England`, where `Home` names remote) is this loop's work.

**The cap** is **five rounds**, whatever the numbers, as ADR-0029's.

## The draw

From the recalibration after round 1, a round draws **525 undeclared vacancies**, seeded, from the rule's
current output, in seven strata. The four strata the description gives a mode carry precision and the
found part of each recall. The three silent strata carry the misses of each recall, split by the first
cue the description names: a remote word, then `hybrid`, then an office, onsite or in-person word or days
a week. A silent vacancy with none of these names no mode and is not drawn. Its sizes on the snapshot
after round 1 are in brackets:

| stratum | drawn | round 1 |
|---|---:|---:|
| the description gives remote (2,864) | 30 | 30 |
| the description gives hybrid (11,686) | 25 | 25 |
| the description gives onsite (6,843) | 25 | 25 |
| the description gives a choice (962) | 20 | 20 |
| silent remote: no mode read, a remote word (13,691) | 200 | 100 |
| silent hybrid: no mode read, `hybrid` and no remote word (11,207) | 75 | — |
| silent office: no mode read, an office, onsite or in-person word or days a week, neither of the above (17,539) | 150 | — |

43,297 silent vacancies name none of these cues. **How the silent strata are sized:** each recall's
95% interval, at 92% recall, should clear 85%, a standard error of about 3.6 points. Near the gate, each
recall's misses are a few percent of its silent strata, and the found part, which the described strata
estimate, varies little. So the standard error rests on the silent rows:
- **Remote:** about 290 misses at 92%, all among the 13,691 with a remote word, need about 200 rows.
- **Onsite:** about 610 misses, mostly among the 17,539 with only an office word, need about 120 rows there.
  Those spread into the other silent strata add variance, so the stratum is drawn at 150.
- **Hybrid:** found in more than 12,000 vacancies, so its interval is the narrowest. About 1,080 misses
  need about 50 rows wherever they sit, and the silent hybrid stratum is drawn at 75 so the misses that
  carry no remote word can be seen.

The sizing is for a round near the gates. A round far from them, as round 1, has a wider interval.

Until round 1, a round drew **200 undeclared vacancies** in the measurement's five strata, re-weighted
towards recall:

| stratum | drawn | was |
|---|---:|---:|
| the description gives remote | 30 | 40 |
| the description gives hybrid | 25 | 40 |
| the description gives onsite | 25 | 40 |
| the description gives a choice | 20 | 15 |
| silent remote: no mode read, the description names a remote word | 100 | 65 |

The silent stratum carries the recall estimate: 8 remote of 64 left an interval of 44–73%, and the
single-mode strata, which pass precision comfortably, are where rows can be spared. The choice stratum
grows because the fixes recall needs are choices, and choices are where precision is weakest (6 of 15).

## The labelling

As ADR-0029 shapes it: the labeller reads the company, title, location and field, and only the
description segments with a mode cue, under their headings; one answer a row; batches of 50; each shown
text labelled once and kept with the criterion revision it was made under, reused by any later round
that draws it again; no second labeller, the adjudicator settling where labeller and rule split. The
labeller is `.claude/agents/work-mode-labeller.md` (Sonnet, Read and Write only), never a general-purpose
agent, which cost twice as much in the measurement.

A round spends at most **650k tokens**, labelling and adjudication together, from the recalibration after
round 1. That is 11 batches of about 50k each, as round 1 spent, and about 100k of adjudication: more silent
rows give more disagreements. It is far above ADR-0029's 150k, because three recalls need silent rows where
one did. Later rounds cost less as kept labels are reused. Each round's Record reports what it spent.

Until round 1, a round spent at most **200k tokens**: about 38k a batch of 50, four batches, and about 50k
of adjudication. Round 1 spent 252k, at about 50k a batch.

**The final measurement.** When a round's adjudicated figures hold both gates, the round is widened
before the loop exits, as ADR-0029 does: more rows are drawn, mostly into the silent stratum, past the
200k cap, as many as the round comment states and sizes beforehand.

## The gold set

From the recalibration after round 1, the gold set is redrawn in the seven strata of the draw, at the
rules as they stand then, so that the agreement covers the silent hybrid and silent office strata. Its
size and its held-out share are the calibration's to set.

**The second gold set (recalibration, 2026-10-31).** 100 vacancies, `docs/measurements/work-mode-gold-set-2.tsv`,
drawn by `scripts/draw-work-mode-gold` at round 1's rules, weighted to the silent strata: 6 described remote,
6 hybrid, 6 onsite, 8 a choice, 26 silent remote, 20 silent hybrid, 28 silent office. Half of each stratum is
held out. It excludes every vacancy drawn before: the measurement's, the first gold set's and the 525 that
re-score round 1. Labelled by the work-mode labeller under revision 2, then corrected by a Sonnet review under
revision 3 (6 rows changed). The first gold set is kept as it was.

Until then, drawn fresh, as a round draws, at the rules as they stand when calibration runs: **100 vacancies** in the
same strata at half a round's size (15 remote, 12 hybrid, 13 onsite, 10 choice, 50 silent), excluding the
measurement's 200, with **half of each stratum held out**. The measurement's rows are not reused: its
scorer has read their rule answers, so none of them could be held out blind. Labelled by the work-mode
labeller with its close calls noted, and corrected by an agent review, not by Elias.

The calibration that freezes the first revision takes up the criterion questions the measurement left:
template labels listing every mode followed by a schedule; `primarily remote, on site as needed`; an
`on-site role` later `eligible for a remote work arrangement`; and a statement about the whole team
(`The teams are all home based`) under rule 1.

### Agreement (calibration, 2026-10-31)

Recorded beside the gates, not a gate (ADR-0012). Drawn by `scripts/draw-work-mode-gold`, labelled by the
work-mode labeller under revision 1 and corrected by a Sonnet review (6 rows changed, 4 of them only the
order of a choice's modes). Revision 2 is frozen. Checked by `scripts/check-work-mode-gold` with a fresh
labeller run under it:

| part | rows | labeller | adjudicator with the gold set |
|---|---:|---|---|
| open | 51 | 48 (94.1%, 84–98%) | 2 of 3, 1 neither |
| held-out | 49 | 48 (98.0%, 89–100%) | 1 of 1 |

Both are near the gates' 90% line from above, so a round's adjudicated figures can tell rules from
noise, if barely. The labeller's misses are single cases, not a clause read two ways.

### Agreement (recalibration, 2026-10-31)

On the second gold set, with revision 3 frozen. Checked by `scripts/check-work-mode-gold` with a fresh labeller
run under it:

| part | rows | labeller | adjudicator with the gold set |
|---|---:|---|---|
| open | 50 | 50 (100%, 93–100%) | none to settle |
| held-out | 50 | 49 (98.0%, 89–100%) | 0 of 1: it sided with the labeller |

The held-out figure is the same as revision 2's on the first gold set, now on rows that are mostly silent
hybrid and silent office. Both are near the gates' 90% line from above.

## Considered options

- **Amending ADR-0029** for this loop: it already shapes a loop's labelling, but it is the eligibility
  loop's decision record. Rejected to keep one ADR per loop; this one points to it for the labelling.
- **Dropping the precision gate**, since it passes: rejected, as above.
- **Gating hybrid and onsite recall**: rejected at first, as gating a miss that errs safe for #103.
  Reversed on 2026-10-31 (Elias): the work mode is a filter for the product in every form, so a missed
  hybrid or onsite vacancy is a miss there, whatever it does to eligibility.
- **Keeping the 200-row draw with the new gates**: cheaper, but it draws no silent vacancy without a remote
  word, so hybrid and onsite misses there would never be seen.
- **The measurement's draw, unchanged** (40/40/40/15/65): leaves the recall interval about 30 points
  wide, too wide to tell a round at 85% from one at 92%.
- **Reusing the measurement's 200 rows as the gold set**: cheaper, but none could be held out.

## Consequences

- #103 waits on this loop. Its count of silent cities (ADR-0029) runs once this loop passes, and the
  per-ATS shares of the measurement are not used before then.
