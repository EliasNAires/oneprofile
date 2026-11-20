# How often a held Workday description changes, and what it changes

Run on the development machine on 2026-11-19 for #90, to weigh reading a Workday description only
for a vacancy not held with one (c40422d) against what the product would lose. Decided from it:
ADR-0032. Times are -03:00.

## Method

1. From `archive/pre-sweep-2026-11-18.dump` (the snapshot #90's first run started from), 6000
   Workday vacancies held with a description, drawn at random (`md5(id || 'desc-check-2026-11-19')`).
   Their descriptions were read by the sweep of 2026-10-24 and 25, about 26 days earlier.
2. Each vacancy's page was asked of Workday again (16:20–16:45, 4 a second), and its
   `jobDescription` marked with the sweep's own `DescriptionMarkingRule`, so that it compares with
   what is held line for line.
3. Every pass that reads a description (cleanings, classifications, body-classifications,
   skill-extractions, work-modes, country-sets, seniorities; title breakdown, roles and places once
   before) was run on dev with the held descriptions, then again with the 300 changed ones put in their
   place. The facts of the sampled vacancies were read after each.
4. Dev was restored from the dump.

The 3431 vacancies whose description did not change are the control: none of their facts moved between
the two runs. Every difference below comes from the changed descriptions.

## How many changed

| Of the 6000 | Vacancies |
| --- | ---: |
| Closed (`403` or `404`; `403` is closed on some tenants: the vacancy is gone from the board's list) | 2169 |
| Open, same description | 3431 |
| Open, description changed, same title | 300 |
| Open, title changed | 100 |

7.8% of the open vacancies changed their description under the same title, the ones c40422d does not
read again. In the subset, 184 of 1871 open `IN` vacancies (9.8%). A vacancy whose title changes is
read again, since its title no longer matches.

The changes, by size of the lines added and removed: 51 under 50 characters (a closing date moved, a
tag added), 70 under 300, 119 under 1500, 60 larger (most of the text rewritten). 18 add, remove or
change a salary figure, which no rule reads yet.

## What changed in the facts

Of the 184 changed `IN` vacancies:

| Fact | Vacancies | Of all 1871 open `IN` |
| --- | ---: | ---: |
| Eligibility (stated sets) | 16 | 0.9% |
| Required years | 12 | 0.6% |
| Work mode | 11 | 0.6% |
| Skills | 10 | 0.5% |
| Seniority level | 7 | 0.4% |
| Visa or relocation offered | 6 | 0.3% |
| `IN`/`OUT` (3 into the subset, 2 out) | 5 | 0.3% |
| Roles | 0 | 0% |
| **Any of these but roles** | **34** | **1.8%** |

146 of the 184 (80%) changed no fact. Examples of the 34: onsite to hybrid; no work mode to hybrid,
or to remote and hybrid; required years 10 to 7, 7 to 5, 6 to 10, 12 to none; relocation now offered;
a vacancy in Quezon City whose location statement went, leaving the subset.

So a sweep that never reads a held description again would, a month on, show a wrong fact on about 1 in
55 open `IN` vacancies (1.8%, about 1.3% to 2.5% at this sample's size), for as long as each stays
open. 36% of the sample had closed within the 26 days. How fast the changes arrive day by day is not
measured: this is one 26-day gap.

The raw files are outside the repo, in `~/oneprofile-sweep-logs/desc-check/`.
