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

Not measured yet: the blind sample is still to be drawn (#86).
