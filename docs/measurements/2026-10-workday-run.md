# Workday added as an ATS

Run on the development machine on 2026-10-24 and 25 for #81, from the `classified-2026-10-22-lever`
snapshot (Greenhouse, Ashby and Lever, `2026-10-lever.md`). Flyway added V15 (Workday's vacancy
columns) on startup. Workday was discovered from both archives (ADR-0023), probed and swept at the
setting `2026-10-workday.md` § The setting decided. Then the whole corpus went through cleaning,
title classification, skill extraction and the body pass. Greenhouse, Ashby and Lever were not
discovered, probed or swept again. Times are -03:00.

## Snapshots

They supersede the 2026-10-22-lever pair as what dev restores (ADR-0006). That pair moved to
`~/oneprofile-snapshots/archive/`.

- `~/oneprofile-snapshots/raw-2026-10-25.dump`, 564 MB: the corpus as the sweep left it, with the
  data of `normalized_vacancy`, `normalized_vacancy_title_seniority` and `vacancy_skill` left out.
- `~/oneprofile-snapshots/classified-2026-10-25.dump`, 1180 MB: after cleaning, title
  classification, skill extraction and the body pass.

Both were cut after the body pass. The raw one leaves the derived tables' data out of the same
database.

## The run

| Phase | Wall clock | What it reported |
| --- | --- | --- |
| Twelve Common Crawl discoveries | 3m 26s (16:37–16:41) | 7404 companies |
| Wayback | 1h 48m (16:41–18:29) | 1 209 436 captures, 7225 slugs, 4580 companies added |
| Workday probe | 12m 49s (18:31–18:44) | 6533 active, 936 empty, 4500 not found, 15 unreadable |
| Workday sweep, first | 2h 36m (18:45–21:20), stopped | see § The throttle |
| Workday sweep, second | 13h 47m (21:21–11:08), stopped | 6530 of 6533 boards, see § Accenture |
| `POST /cleanings`, first | 8m (11:09–11:17), failed | see § Vacancies without a title |
| `POST /cleanings` | 17m 14s | 1 465 844 vacancies, 803 398 distinct titles, 747 875 once cleaned, 296 628 languages detected |
| `POST /classifications` | 5m 42s | 116 762 `IN`, 1 074 790 `OUT`, 274 292 `UNKNOWN` |
| `POST /skill-extractions` | 2m 51s | 252 866 vacancies, 1 263 377 rows |
| `POST /body-classifications` | 4m 16s | pile of 145 165: 39 005 `IN`, 96 652 `OUT`, 447 unknown, 9061 `unsupported_language` |
| `pg_dump -Fc`, classified | 2m 23s | 1180 MB |
| `pg_dump -Fc`, raw | 1m 11s | 564 MB |

The sweep reported nothing, since both runs of it were stopped. Its figures below are read from the
database. A board is mirrored once it is read whole, so every board a stopped sweep finished is
held.

The four passes took 30 minutes together on 1 465 844 vacancies, against 29m 43s on 360 311 in #79's
run. 86% of Workday's vacancies have no description, and cleaning a title alone is cheap. #89 is
about keeping the passes under an hour on the production server.

## Discovery per crawl

New companies is what the crawl added, read after every newer one. The crawls are the 12 newest,
CC-MAIN-2026-43 to CC-MAIN-2025-47 (`2026-10-workday.md` § Where boards are published). Each took
10 to 15 seconds.

| Crawl | Slugs | New companies |
| --- | ---: | ---: |
| CC-MAIN-2026-43 | 2873 | 2873 |
| CC-MAIN-2026-39 | 2913 | 1097 |
| CC-MAIN-2026-34 | 2529 | 701 |
| CC-MAIN-2026-30 | 2452 | 522 |
| CC-MAIN-2026-25 | 2303 | 351 |
| CC-MAIN-2026-21 | 2929 | 520 |
| CC-MAIN-2026-17 | 3069 | 440 |
| CC-MAIN-2026-12 | 3164 | 402 |
| CC-MAIN-2026-08 | 2238 | 177 |
| CC-MAIN-2026-04 | 1970 | 108 |
| CC-MAIN-2025-51 | 2102 | 104 |
| CC-MAIN-2025-47 | 2232 | 109 |
| **All twelve** | | **7404** |

The research counted 7970 boards in the same crawls. The research script and `WorkdayUrlRule` read
a site out of a URL differently. The difference was not traced.

## Wayback

`POST /discoveries/workday/wayback` read both domains whole, by domain (ADR-0023 as amended): 1 209 436
captures and 7225 slugs, 4580 of them not in Common Crawl. The research expected about a million
captures and about 4500 new boards. Wayback refused 30 requests with `502`, `503` or `504`, timed out
on 9, and reset the stream on 2. Each was asked again and answered within its ten attempts.

Discovery keeps no record of which archive named a company, so this run cannot say how many live
boards each archive added.

## Boards

| | Companies |
| --- | ---: |
| Discovered | 11 984 |
| `ACTIVE` | 6533 (54.5%) |
| `EMPTY` | 936 |
| `NOT_FOUND` | 4500 |
| Not probed (unreadable) | 15 |

**Slugs that are no board.** 3386 of the 4500 not found have a site that is no site:
`videoplayerlabels` (1416), `assets` (940), `videolabels` (416), `refreshfacet` (233), `cdn-cgi` (76),
or a two-letter locale such as `es` (268). `WorkdayUrlRule` skips a locale only when it is written
`xx-XX`, and nothing refuses Workday's asset paths. A board published only behind a two-letter locale
is missed. Written in #88 § Slugs that are no board.

**The 15 unreadable** are 12 boards on implementation hosts (`wd2-impl`, `wd3-impl`, `wd5-impl`,
`impl-wd12`, such as `paypal.wd2-impl/jobs`) and Whole Foods' three sites (`wholefoods.wd5/…`). They
have no board status.

Hosts, by live boards: `wd1` 2240, `wd5` 1327, `wd3` 1312, `wd12` 375, `wd103` 236, `wd108` 232,
`wd503` 219, `wd501` 210, and the rest fewer. Every company is `US` (`WorkdayUrlRule.regionIn`).

## The sweep

| | Workday |
| --- | ---: |
| Live boards read | 6530 of 6533 |
| Vacancies | 1 105 533 |
| … with a description | 157 431 (14.2%) |
| Vacancies per live board, mean / median | 169.3 / 36 |
| Companies with a name | 4653 of 6533 live (71.2%) |

The research expected 913 000 to 1 030 000 vacancies from the 6006 boards Common Crawl names, and
171.5 a live board. 14.2% of vacancies have a description, against the research's 14.5%. That is the
sweep's rule: a description is read only where the title is `IN`, or `UNKNOWN` for domain or scope
ambiguity (#81). A company is named only from the vacancy pages read, so a board where no title
called for its page has no name.

The largest boards are retail and health: `cvshealth.wd1/cvs_health_careers` 19 250,
`oreillyauto.wd1/oreilly` 19 017, `advanceauto.wd5/advanceexternalcareers` 16 511,
`dollartree.wd5/dollartreeus` 15 457, `lowes.wd5/lws_external_cs` 12 609. The largest in the
engineering subset are `nvidia.wd5/nvidiaexternalcareersite` 1576, `bah.wd1/bah_jobs` 1236,
`capitalone.wd12/capital_one` 1077, `pwc.wd3/global_experienced_careers` 1004 and `citi.wd5/2` 949.

**Boards not read.** Three live boards hold no vacancies:
- `accenture.wd103/accenturecareers`: the sweep was stopped on it (§ Accenture).
- `sunrise.wd3/sunrise` and `halifaxhealth.wd12/halifaxhealth`: a list page answered an error after
  its attempts.

Three more boards failed in the second sweep (`abercrombie.wd108/anf`, `aesseal.wd108/aesseal_careers`,
`amadeus.wd502/jobs`). They keep the vacancies the first sweep read.

### Structured fields

| Field | Vacancies that have it |
| --- | ---: |
| `country` | 157 130 (14.2%) |
| `all_locations`, more than one | 26 819 |
| `time_type` | 153 926: 150 855 `Full time`, 2772 `Part time`, 262 `Full Time`, 37 `Flexible Hours` |
| A pay range | 0 |

Country, every location and the time type come from a vacancy's page, so only vacancies whose page
was read have them. The sweep stores no pay range for Workday.

## The throttle

The first sweep was challenged by Cloudflare at 19:11, 26 minutes in. Within 0.7 seconds, the
throttle halved three times, from 16 a second and 20 open to 2 a second and 2 open. Only the refused
request waited out the challenge. The others kept starting into it, and each refusal of a request
started after the last halving halved again. At 2 a second the sweep would have taken more than a
day. It was stopped at 21:20, after 1007 boards.

Fixed in ec586d7. The throttle now holds every start while a challenge is waited out, from one
minute, doubling while it still refuses. A burst halves it once, as #81 decided.

The second sweep was challenged at 21:47, also 26 minutes in. It halved once, to 8 a second and 10
open, and held that for the rest of the run with no further challenge. The second sweep read again
the boards the first had read. Measured at 22:27, it asked about 8.7 requests a second.

**16 a second holds for about 26 minutes in a sweep.** The research's 34 minutes at 16 a second
were a probe. A sweep is challenged at the same rate, earlier. #88 proposes a lower setting or a
throttle that recovers, for Elias to decide.

## Accenture

By 09:38 the sweep had read every live board except Accenture's. Its facets count 42 394 vacancies,
so it is read in parts by facet. A board's description pages are read one after another, and the
other boards had finished, so the sweep was down to one request at a time. Accenture answered a list
page in 2.8 to 3.0 s, and it has a `429` of its own, asked again after waits of 1 to 8 s. Traffic
fell to about 2 KB a second. A board is mirrored only once it is read whole, so the run showed no
progress. The last line logged for another board was at 05:01.

Elias decided to stop the sweep, and it was stopped at 11:08. This corpus holds no Accenture
vacancies. Accenture is about 4% of Workday's vacancies, and its share of the engineering subset was
estimated at 3000 to 6000. #88 is about reading a board's descriptions in parallel, or leaving
Accenture out until its behaviour is understood.

## Vacancies without a title

16 Workday vacancies are listed with no title (for example `copart.wd12/copart`, `_JR107671`). The
sweep stores them so, and asks for no page, since no title calls for one. Cleaning took every title
to be there, and stopped on the first of them with a `NullPointerException` after about 410 000
vacancies. Fixed in f817f5d: a missing title cleans as an empty one, as Ashby's two empty titles do.

## Classification

### `IN`, `OUT` and `UNKNOWN` by reason

| State | Reason | Signal | Workday | All four ATSs |
| --- | --- | --- | ---: | ---: |
| `IN` | — | title | 57 754 | 116 762 |
| `IN` | `domain_ambiguity` | body | 11 102 | |
| `IN` | `scope_ambiguity` | body | 10 057 | |
| `OUT` | — | title | 833 090 | |
| `OUT` | `domain_ambiguity` | body | 45 820 | |
| `OUT` | `scope_ambiguity` | body | 25 035 | |
| `UNKNOWN` | `domain_ambiguity` | title | 240 | |
| `UNKNOWN` | `scope_ambiguity` | title | 94 | |
| `UNKNOWN` | `unruled` | title | 114 936 | |
| `UNKNOWN` | `unsupported_language` | body | 7405 | 9061 |

Greenhouse's, Ashby's and Lever's states are #79's: their vacancies were not read again, and the
frozen rules give them the same states. Workday's pile is 99 753 of the 145 165.

Workday's title `IN` is 5.2% of its vacancies, within the research's 3.8% to 6.7%. The body pass
added 21 159, 1.9% of Workday's vacancies, against the research's 3.9% at the earlier boards' rates:
Workday's pile resolves `OUT` more often (70 855 `OUT`, 21 159 `IN`).

Skill extraction found skills in 89 685 Workday vacancies (645 397 rows).

### Language

Of the Workday vacancies with a description, detected from the cleaned description: 145 987 `en`,
5167 `fr`, 1751 `de`, 1148 `es`, 812 `pt`, 764 `it`, 469 `nl`, 405 `ja`, and fewer in other languages.

## Per ATS

| | Greenhouse | Ashby | Lever | Workday | All four |
| --- | ---: | ---: | ---: | ---: | ---: |
| Companies | 18 925 | 7920 | 8895 | 11 984 | 47 724 |
| Live (`ACTIVE`) | 7032 | 3737 | 1935 | 6533 | 19 237 |
| Vacancies | 220 645 | 65 437 | 74 229 | 1 105 533 | 1 465 844 |
| Vacancies per live board | 31.1 | 17.5 | 38.4 | 169.3 | 76.2 |
| **Engineering subset** | **41 727** | **22 575** | **12 552** | **78 913** (7.1%) | **155 767** |
| … decided by the title | 32 217 | 17 274 | 9517 | 57 754 | 116 762 |
| … decided by the body | 9510 | 5301 | 3035 | 21 159 | 39 005 |
| **Live in one sweep** | **41 251** | **22 564** | **12 552** | **78 913** | **155 280** |
| Title `IN` per live board | 4.53 | 4.62 | 4.92 | 8.84 | |

Title `IN` per live board is ADR-0022's measure. Workday's 8.84 is the highest of the four, as its
boards are the largest. It is 7.1% of its vacancies, against 16.9% to 34.5% for the others: its
largest boards are retail and health employers, not technology companies.

## How far #70 is

**155 280 engineering vacancies live in one sweep, against 100 000: the bar is passed by 55 280.**
Before Workday the figure was 76 367, so Workday added 78 913, more than the research's 52 000 at
its low end, and within its 81 000 to 92 000 before Accenture. That is ADR-0011's first bar for
stability, 100 000 engineering vacancies live in one sweep. Its second, 30 consecutive days without
manual intervention, is not measured here.

The figures count every vacancy once per board. `2026-10-workday.md` § Repeated vacancies estimated
that a dedup would fold 4.8% to 19.8% of the snapshot's `IN` vacancies. If Workday's fold the same,
155 280 is about 124 500 at 19.8%.
