# The work mode's first run (#86)

2026-10-27, on `classified-2026-10-25`, with the rule as first built (`WorkModeRule`):
`POST /work-modes` over the engineering subset, 155,767 vacancies, in 2 min 12 s. The snapshot was
swept before the reader kept Workday's `remoteType`, so no Workday vacancy has a field yet; its
values arrive with the next Workday sweep.

## Coverage

By the step of the order of evidence that decided it:

| ATS | vacancies | with a mode | share | location | field | title |
|---|---:|---:|---:|---:|---:|---:|
| Workday | 78,913 | 5,870 | 7.4% | 4,251 | — | 1,619 |
| Greenhouse | 41,727 | 8,982 | 21.5% | 8,628 | — | 354 |
| Ashby | 22,575 | 20,474 | 90.7% | 3,080 | 17,370 | 24 |
| Lever | 12,552 | 12,354 | 98.4% | 779 | 11,573 | 2 |
| all | 155,767 | 47,680 | 30.6% | 16,738 | 28,943 | 1,999 |

The rest, 108,087, have none: nothing they publish outside the description declares one. Workday's
`remoteType` would add about 28% of its subset (the probe on #86); the description (#98) is where
most of Workday and Greenhouse declare theirs.

By mode:

| ATS | remote | hybrid | onsite |
|---|---:|---:|---:|
| Workday | 4,434 | 773 | 663 |
| Greenhouse | 7,743 | 1,048 | 191 |
| Ashby | 7,005 | 6,690 | 6,779 |
| Lever | 5,104 | 4,059 | 3,191 |

**Location over field.** In 171 vacancies of Ashby and Lever the location names a mode the field
does not, and the location decided, as the probe (`2026-10-title-breakdown-probe.md`) found it
usually right.

**Two modes in one text.** 145 locations of the subset name more than one mode (`Remote/Hybrid`,
`Dallas, TX - Hybrid (3x in office/week)`, `Nashville, TN (Onsite) or Remote`); they read as hybrid
where hybrid is among them, else as remote.

**The title.** A title is read only where neither the location nor the field names a mode. Its
words are the ones the title breakdown takes out (`docs/title-breakdown-criterion.md`, rule 7),
except where they name what the work is about: `hybrid cloud` (58 titles), `remote sensing` (19),
`remote access` (11), `hybrid infrastructure` (10). A read of 15 Workday titles the step decided
found one wrong: `ONSITE 2X Week` read as onsite, which is hybrid.

## Precision

Measured on a blind sample: `scripts/draw-work-mode-sample` and `scripts/score-work-mode-sample` into
`2026-10-work-mode-sample/`, against `docs/work-mode-criterion.md`. The draw (`sample.tsv`), each cell's
size (`cells.tsv`), what the labellers saw (`batch-1.tsv`, `batch-2.tsv`), their labels
(`labels-a-*.tsv`, `labels-b-*.tsv`), the disagreements and the adjudication (`disagreements.tsv`,
`adjudicated.tsv`).

**How it was drawn.** The step that decided a vacancy is not stored, so the draw reads it again by the
rule's words; its reading gave the stored mode on all 47,680. 20 vacancies of each cell of ATS by step
(all 2 of Lever's title cell), 182 in all, plus two strata of 15: location over a disagreeing field, and
a location naming two modes. 212 vacancies, seeded (86).

**Who labelled.** Labeller a was Sonnet and labeller b Haiku, each on both batches. Per vacancy they read
the company, the title, the location, the ATS's work-mode field and the description's sentences that
name where work is done (at most six, cut by a regex), never the rule's answer. A Sonnet adjudicator
settled the 10 they split on (8 as a, 2 as b). About 370k tokens in all.

**Agreement: 202 of 212 (95.3%)**, between two models. Half the splits were a choice against one of its
modes (`hybrid` ~ `remote,hybrid`).

**Precision.** The share whose settled label holds the rule's mode; for a vacancy offered in a choice of
modes, the rule is right if its mode is among them:

| | right | precision | 95% interval |
|---|---:|---:|---|
| every cell, 20 each | 174 of 182 | 95.6% | 92% to 98% |
| weighted by cell size | | 96.0% | |
| location | 78 of 80 | 97.5% | 91% to 99% |
| field | 38 of 40 | 95.0% | 83% to 99% |
| title | 58 of 62 | 93.5% | 85% to 97% |
| location over field | 13 of 15 | 86.7% | 62% to 96% |
| two modes | 14 of 15 | 93.3% | 70% to 99% |

By ATS: Ashby 57 of 60, Greenhouse 38 of 40, Lever 41 of 42, Workday 38 of 40. No Workday field was
measured: the snapshot has none.

**Choices.** 17 vacancies are offered in more than one mode, most as a list of places
(`San Francisco, CA (Hybrid) or US-Remote`). The rule's mode is among them in all 17; it answers hybrid
in 14, where a remote seeker could apply too. Which one mode a choice should show is not decided.

## Where the rule and the labels split

The 11 misses:

- **The description says otherwise (8).** The fields name one mode and a sentence about this job
  another: field `OnSite` with `four days per week` in the Berlin office; location `Remote - Minnesota`
  with `This role is ON SITE`; `Remote (UK)` with `2 to 4 days per week on-site`; field `hybrid` with
  `Remote bevorzugt`. Only reading the description (#98) fixes these. Two are in the location-over-field
  stratum: there the field was right and the location not, against the probe's finding.
- **`Remote Assist` names the work (2).** `Tech Lead, Remote Assist - Robotaxi` reads as remote; it is
  the product, as `remote sensing` is. One more phrase for the guard.
- **`Home Office, CA` (1).** Workday's location names a place called home office, and the rule reads it
  as remote; the labellers found nothing declaring a mode.

The `ONSITE 2X Week` miss found before the sample was not drawn.

## Is it enough

Enough to close #86's "Done when": the precision of every step and ATS is at or above 90%, and most
misses are the description's, which is #98's. Two cheap rule changes would remove the rest: `remote
assist` in the guard, and a mode word beside a days-a-week phrase read as hybrid. This sample does not
measure recall: the 108,087 with no mode were not drawn.
