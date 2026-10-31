# The work mode from the description is a rule loop

Status: accepted

#98 built the reading of the work mode from the description and measured it once
(`docs/measurements/2026-10-work-mode-description.md`). Precision passed at 97.7%. Remote recall on
undeclared vacancies failed at 56.6% (44–73%) against 90%: 7 of the 10 remote vacancies missed offer
remote as one option of a choice the rule does not read, or reads as one mode (`This role is on-site in
our Fort Worth, Texas (or remote in the US) location`). By #98's decision 3, a failed gate makes it a rule
loop (`docs/agents/rule-loop.md`, ADR-0014). Its gates were set by Elias on 2026-10-30. The rest he left to
the agent's judgment on 2026-10-31.

## The gates

A round holds when, adjudicated, against `docs/work-mode-criterion.md`:

1. **Precision.** At least **90%** of the modes the rule reads from the description are right, weighted
   by stratum size. A row is right where its label holds every mode the rule gave.
2. **Remote recall on undeclared vacancies.** Of the undeclared vacancies whose description says the
   role is remote, at least **90%** are read as remote, weighted by stratum size. As in the measurement,
   an undeclared vacancy whose description names no remote word is assumed not remote and is not drawn.

The loop is about recall. Precision already passes and stays a gate so that recall is not bought with
it: a template label (`Hybrid / Onsite / Remote`) read as a choice raises one and lowers the other.

**Hybrid and onsite recall are reported, not gated.** The silent stratum held 18 hybrid and 6 onsite
among 64 rows that the rule leaves without a mode. A missed onsite or hybrid vacancy stays undeclared,
so its eligibility stays unknown, which is the safe side, and ADR-0029's count of silent cities covers
it. A missed remote vacancy is the harmful miss: it makes that count look safer than it is. Each round
reports the hybrid and onsite share of the silent stratum, and the Feedback may list them under *Also
seen*.

**The rules are the whole of `WorkModeRule`**, not only its description step. A location-step miss
that counts against a gate (`Home - UK- England`, where `Home` names remote) is this loop's work.

**The cap** is **five rounds**, whatever the numbers, as ADR-0029's.

## The draw

A round draws **200 undeclared vacancies**, seeded, from the rule's current output, in the
measurement's five strata, re-weighted towards recall:

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

A round spends at most **200k tokens**, labelling and adjudication together: about 38k a batch of 50,
four batches, and about 50k of adjudication. That is above ADR-0029's 150k because the recall estimate
needs the silent stratum's rows. Later rounds cost less as kept labels are reused. Each round's Record
reports what it spent.

**The final measurement.** When a round's adjudicated figures hold both gates, the round is widened
before the loop exits, as ADR-0029 does: more rows are drawn, mostly into the silent stratum, past the
200k cap, as many as the round comment states and sizes beforehand.

## The gold set

Drawn fresh, as a round draws, at the rules as they stand when calibration runs: **100 vacancies** in the
same strata at half a round's size (15 remote, 12 hybrid, 13 onsite, 10 choice, 50 silent), excluding the
measurement's 200, with **half of each stratum held out**. The measurement's rows are not reused: its
scorer has read their rule answers, so none of them could be held out blind. Labelled by the work-mode
labeller with its close calls noted, and corrected by an agent review, not by Elias.

The calibration that freezes the first revision takes up the criterion questions the measurement left:
template labels listing every mode followed by a schedule; `primarily remote, on site as needed`; an
`on-site role` later `eligible for a remote work arrangement`; and a statement about the whole team
(`The teams are all home based`) under rule 1.

## Considered options

- **Amending ADR-0029** for this loop: it already shapes a loop's labelling, but it is the eligibility
  loop's decision record. Rejected to keep one ADR per loop; this one points to it for the labelling.
- **Dropping the precision gate**, since it passes: rejected, as above.
- **Gating hybrid and onsite recall**: rejected, as above; it would gate a miss that errs safe.
- **The measurement's draw, unchanged** (40/40/40/15/65): leaves the recall interval about 30 points
  wide, too wide to tell a round at 85% from one at 92%.
- **Reusing the measurement's 200 rows as the gold set**: cheaper, but none could be held out.

## Consequences

- #103 waits on this loop. Its count of silent cities (ADR-0029) runs once this loop passes, and the
  per-ATS shares of the measurement are not used before then.
