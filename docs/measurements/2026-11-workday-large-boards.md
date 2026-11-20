# Workday's large boards

Measured on 2026-11-20 for #110, before building: what a Workday board of 2000 or more vacancies
gives, how its size can be read, and how far its counts drift. These are the 78 boards that #90's
second run narrowed by facet (`~/oneprofile-sweep-logs/facet-coverage.tsv`). Times are -03:00.

In short: 26 of the 78 boards page past 2000 and can be read whole, and 52 stop at 2000. On every
capped board, two or more facets agree on a sum within 1%, which gives its size, and a facet that
covers it. A part's counts drift from what its parent showed by at most 4 vacancies. The rule built
from this reads the 78 in about 18 200 list pages, against #90's 48 820.

## How it was asked

Before this file, a research pass (34 requests, notes outside the repo in
`../learning/workday-large-boards/`) found the two kinds of board and the facet sums below. Here, a
first page of each of the 78 boards, CVS and 7-Eleven paged whole, and the first page under 104 facet
values, at one request a second.

The first 54 requests went out from Python's `requests` with a browser's user agent. Cloudflare
challenged it twice, once after 40 boards and once after 14 at one request every 2 seconds. Java's
`HttpClient` with the app's user agent was answered at the same moment and never challenged in
about 1700 requests. So the challenge was for the client, not the rate.

## Two kinds of board

A **capped** board gives `total: 2000` on its first page and, from offset 2000 on, its first page
again (Walmart and NVIDIA in the research: the same 20 vacancies at 2000, 2020 and 4000). A page
before 2000 but not the first gives `total: 0`. An **uncapped** board gives its real `total` and pages
past 2000. Workday documents neither.

Of the 78, **52 are capped and 26 uncapped**. The uncapped include the costliest boards of #90's
runs: O'Reilly (19 283), CVS (18 601), Dollar Tree (18 514), Lowe's (13 095), 7-Eleven (12 152),
Family Dollar (10 414) and TJX (10 363). Paged whole, the 26 take 8199 list pages.

### Paged whole

| Board | `total` | Pages | Distinct vacancies | Read | Repeated | Status |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `cvshealth.wd1/cvs_health_careers` | 18 601 | 931 | 18 577 | 99.87% | 27 | 200 on all |
| `my7elevenhr.wd12/paradox_posting_store_reqs_only` | 12 152 | 608 | 12 152 | 100% | 0 | 200 on all |

Neither repeats or stops short, so step 2 of #110 stands. No page repeated another, and no page was
empty before the `total`. On CVS, 27 vacancies were listed twice, and others probably skipped, as
vacancies opened and closed while the list was paged: its `total` moved between 18 600 and 18 605 in
the 25 minutes it took. #90's runs read 472 of 7-Eleven, narrowed by
`timeType`, a facet whose two values held 470 of its 12 152.

## A capped board's size

A facet's **sum** is its values' counts added up. A facet whose sum is the part's size names every
vacancy in it. A sum below misses some (a vacancy with no value for it, or values cut off); a sum above
counts a vacancy in several values (`locations`, a state). Walmart, capped, on its first page:

| Facet | Values | Sum |
| --- | ---: | ---: |
| `locationRegionStateProvince` | 66 | 20 344 |
| `locationCountry` | 3 | 20 051 |
| `primaryLocation` | 4210 | 20 051 |
| `jobFamilyGroup` | 24 | 20 028 |
| `jobFamily` | 55 | 19 932 |
| `timeType` | 2 | 19 027 |
| `Job_Profiles` | 55 | 15 434 |
| `company` | 55 | 414 |
| `distance` | 5 | 5 |

#90's rule took `company`, of fewest pages, and read 414 of about 20 000.

Two rules were tried for the size:

- **A country facet by name** (`locationCountry`, `Location_Country`, `Country_and_Jurisdiction`, …):
  34 of the 52 have one. Where a vacancy lists two countries it counts twice (ABB: 2412 against
  2223 that two other facets agree on).
- **The largest sum two or more facets agree on.** Exactly: **25 of the 26 uncapped boards, where it
  equals the real `total`**, and 47 of the 52 capped. Within 1%: all 78. The five capped boards it
  takes 1% for, with their two closest sums: `advanceauto.wd5/advanceexternalcareers` (16 352,
  16 307), `circlek.wd3/circlekstorejobs` (10 904, 10 900), `citi.wd5/2` (4256, 4248),
  `freseniusmedicalcare.wd3/fme` (3785, 3784) and `pacs.wd108/pacs` (4695, 4658, 0.8% apart). The
  uncapped one is `micron.wd1/external` (2894, 2893).

The second is the rule. Two facets that agree on a sum are evidence that it is the size, not proof:
two facets can miss the same vacancies. The check that a board was read whole (ADR-0033) rests on it.

## The 55-value cut

33 of the 35 facets with exactly 55 values are top-level facets, on 22 boards: `jobFamily` (8),
`Location_Region_State_Province` (5), `Job_Profiles` (3), `company`, `hiringCompany`, `Skills`, …
Only two top-level facets list more, both `jobFamilyGroup` on PwC's boards (89 and 110). Facets
nested under `locationMainGroup` list up to 5265, though two of them stop at 55 too (JLL's
`locationCountry`, Minor's `locationRegionStateProvince`). So most boards seem to list at most 55
values a facet outside the location group, and a facet cut there names fewer vacancies than it
covers. That shows in its sum, so the rule needs nothing of its own for it.

## The tolerance

Under a facet value, a part's size is the count its parent showed for it. For each capped board, the
first page under the largest value of the facet the rule takes, and under its middle value, was asked
(104 parts, 10 to 50 minutes after the parent):

- The part's `total` against its parent's count, for the 100 parts under 2000: equal in 83, at most 4
  apart, the largest share 1% (Walmart, 99 against 100).
- The part's closest facet sum against its parent's count: within 0.4% in 103 of 104 (Walmart's 100
  again, at 1%).
- The four parts of 2000 or more (Airbus 2245, Circle K 9053, Fresenius 3246, Trinity 2281) all gave
  `total: 2000`: a part of a capped board is capped too.

The tolerance is **1% of the part's size**. It matters only on a capped part, 2000 or more, where 1% is
20 vacancies or more against the 4 seen.

## The rule built from this (#110)

- A part whose `total` is not exactly 2000 is paged to its `total`.
- A capped part is narrowed by a facet whose sum is within 1% of the part's size and that has two
  values or more, the one whose values take the fewest pages (each value's count over 20, rounded up,
  added up). Failing any, by the location facet of most values. A value still capped is narrowed
  again.
- On the 52 capped boards' first pages, the rule finds a covering facet on all 52, with no location
  fallback. Walmart qualifies `locationCountry` (1004 pages, its US value of 15 430 narrowed again),
  `jobFamilyGroup` (1013), `jobFamily` (1024; 55 values, 0.6% under) and `primaryLocation` (4352),
  and is narrowed by `locationCountry`. #90's preference for a facet whose every value is under 2000
  would have taken `primaryLocation`.
- Estimated from the first pages: 8199 list pages for the uncapped boards and about 9985 for the capped
  ones, about 18 200 in all, against #90's second run's 48 820. Parts under a value of 2000 or more
  add their own first pages.

## The run (#110 step 5)

Run on 2026-11-20 from 14:10:45 to 17:50:44 with 0c5553a, from what dev held after #90
(`~/oneprofile-snapshots/archive/pre-sweep-2026-11-18.dump` restored), dumped first to
`archive/pre-sweep-2026-11-20.dump` (1.2 GB, 6m 33s, 1 448 737 vacancies) and exported with
`analyze-sweep export`. A Workday short sweep and the long sweeps of the other three, called at once
as in `2026-11-workday-sweep.md`. The log is `~/oneprofile-sweep-logs/run4.log`.

| Sweep | Wall time | Boards | Published | Added | Deleted | Unreadable | Short |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Ashby | 4m 45s | 3629 | 64 141 | 18 724 | 19 211 | 0 | 0 |
| Lever | 7m 32s | 1889 | 70 672 | 3914 | 4126 | 0 | 0 |
| Greenhouse | 14m 3s | 6888 | 213 054 | 11 773 | 11 878 | 0 | 0 |
| Workday, short | **3h 39m 54s** | 6469 | 1 169 489 | 469 865 | 384 917 | 6 | 7 |

Added and deleted count two days of change on top of #110's: dev held the corpus of 2026-11-18.
Cloudflare challenged at 15:23, 72 minutes in, and the throttle held 8 a second from there. Workday
asked for 63 391 list pages and 65 052 descriptions. The board lines add up to each report.

### The 78 boards

| | #90's second run | This run |
| --- | ---: | ---: |
| List pages | 54 749 | 18 539 |
| Vacancies read | 266 522 | 353 584 |
| Their sizes, added up | | 361 051 (97.9% read) |

#110's issue gave 48 820 list pages for #90's second run; 54 749 adds up its `WORKDAY_BOARD` lines for
the 78. Walmart read 19 968 of a size of 20 093 (414 before), 7-Eleven 12 152 (472), Target 14 185 of
14 191 in 34 minutes. Under 99% of their size: Babilou and Citi at 98.9%, and Airbus and Bridgestone,
read short.

### Read short

| Board | Read | Size | Why |
| --- | ---: | ---: | --- |
| `bridgestone.wd5/external` | 901 | 2887 | narrowed twice, no facet covering the second part |
| `kohls.wd504/kohlscareers` | 1429 | 5996 | the same |
| `kindercare.wd5/externalcareersite` | 211 | 2568 | the same |
| `petco.wd504/external` | 1151 | 3881 | the same |
| `ag.wd3/airbus` | 2679 | 3007 | the same |
| `cbh.wd12/cherrybekaertearlycareers` | 5 | 7 | the `total` behind closed vacancies |
| `jpi.wd1/jpicareers` | 14 | 16 | the same |

All seven kept their held vacancies. **Under a facet applied, Workday lists at most 55 values of
every facet, location facets included**: Bridgestone's `primaryLocation` lists 1635 values on its first
page, 55 (sum 288) under full time. So a part narrowed once and still capped can be left with no
facet that covers it, and its location fallback reads a cut list. The root survey above could not
show this.

0c5553a had dropped #90's preference for a covering facet whose every value is under 2000, to spare
Walmart pages. Restored in 7ddc227, the five boards narrowed twice, the two small ones and Walmart
were swept again alone (`run5.log`):

| Board | Read | Size | List pages |
| --- | ---: | ---: | ---: |
| `bridgestone.wd5/external` | 2883 | 2884 | 1644 |
| `kohls.wd504/kohlscareers` | 5992 | 5992 | 1166 |
| `kindercare.wd5/externalcareersite` | 2564 | 2565 | 2062 |
| `petco.wd504/external` | 1150 | 3881, short | 106 |
| `ag.wd3/airbus` | 2680 | 3008, short | 173 |
| `walmart.wd504/walmartexternal` | 20 016 | 20 072 | 4351 |

The two small boards read whole too, an hour later (5 and 14). Petco and Airbus have no covering facet
whose every value is under 2000, so they are still narrowed twice: Petco's `locations` (1288 values)
would read it whole, but its sum is 2.7% over the size, a vacancy in several stores counted in each,
so it does not cover. Estimated from the first pages, the preference takes the 78 from 18 539 list
pages to about 24 300, mostly Walmart's `primaryLocation`: 4351 pages and 1h 37m alone, against 1042
pages and 38m by `locationCountry`. Read one request after another, Walmart is then a sweep's last
board by about an hour, until #88 reads a board's requests several at a time.
