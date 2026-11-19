# The four sweeps at once

Run on the development machine on 2026-11-18 and 19 for #90, three times, from the snapshot dev held
(`classified-2026-10-25` with the sweeps and passes since), Accenture on `boards/unread.tsv` since
#83. Each time the four sweeps were called at once, each from its own `curl`, against one app. Its
log, at the lines #90 step 1 added, was read back by `scripts/analyze-sweep`. Times are -03:00.

In short: the first run took 8h 38m. Reading a Workday description only where none is held took the
second to 4h 5m, stopped there because one board held the end alone, a #88 trigger. Cloudflare
halved Workday's pace 5 to 26 minutes into every run. And the way a large Workday board is narrowed
by facet leaves out about 57 700 vacancies on 17 boards, in every sweep since #81 (§ Facets that leave
vacancies out).

Before the run, dev was dumped to `~/oneprofile-snapshots/archive/pre-sweep-2026-11-18.dump`
(1.2 GB, 7m 36s) and every held vacancy's title and description hashed (`analyze-sweep export`,
1 448 737 rows). After the last run, that dump was restored: 1 448 737 vacancies.

## The first run

| Sweep | Wall time | Boards | Published | Added | Deleted | Not found | Unreadable |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Lever | 9m 59s | 1889 | 70 032 | 436 | 1288 | 4 | 0 |
| Ashby | 13m 22s | 3629 | 63 996 | 17 637 | 18 269 | 1 | 0 |
| Greenhouse | 27m 58s | 6888 | 213 342 | 5045 | 4862 | 5 | 0 |
| Workday | **8h 37m 44s** | 6469 | 1 068 920 | 357 440 | 367 824 | 0 | 5 |

From the first call (22:23:15) to the last report (07:00:59): 8h 38m. Alone, Lever took 6m 13s,
Ashby 7m 35s and Greenhouse 16m 54s (`2026-10-pace.md` § The settings). Sharing the link and the
database with each other and with Workday made each about 1.6 to 1.8 times slower; all three were done
within 28 minutes, so Workday decides the time, as #90 expected.

The board lines add up to each report. No heartbeat was missed and the app did not stop, so nothing
was resumed.

## Workday's pace

| From | Throttle | Length | Requests started |
| --- | --- | --- | ---: |
| 22:23 | 16 a second, 20 open | 15m | 12 973 |
| 22:38 | 8 a second, 10 open | 8h 23m | 239 463 |

Cloudflare challenged at 22:37:40, **14 minutes in, with Accenture out of the sweep**: 4 requests
refused, one halving of the shared throttle and of the sweep's own pace. Nothing else refused all
night, and the throttle never recovered, since it recovers only after 10 idle minutes
(`WorkdayApiAdapter.RESTORED_AFTER_IDLE`). So Accenture did not bring on the challenge of #81's run;
the rate did, as it did 26 minutes in then.

The run averaged 887 requests a minute in its first 15 minutes and 486 over the whole run. Its
252 436 requests at the 8.1 a second it held take 8.6 hours: the time is requests over the throttle's
rate. Requests spent 41.6 hours together waiting in the throttle, against a mean answer of 764 ms
for a list page and 591 ms for a description: Workday answered faster than the throttle let requests
start.

## Workday's requests

| Kind | Requests | Share |
| --- | ---: | ---: |
| List pages | 96 432 | 38% |
| Description pages | 156 004 | 62% |

Answers: 252 366 `200`, 48 `404` (vacancies closed since their list page), 15 other Workday errors,
4 challenges, 2 gateway errors, 1 I/O error. No tenant `429`.

**List pages.** 78 boards hold 2000 vacancies or more and are read in parts, narrowed by facet. They
took 52 551 list pages for 261 672 vacancies, which would take about 13 100 pages if Workday paged
past 2000. `WorkdayVacancyReaderAdapter.narrowest` picks the facet whose values add up to the fewest
vacancies, so that the parts overlap least, and that is usually a facet of thousands of tiny values,
each a part of one page: CVS took 5269 list pages in 4950 parts for 18 728 vacancies, O'Reilly 5969
in 5955 for 19 235. About 39 000 requests, 16% of the run, are that choice.

**Descriptions.** Of the Workday vacancies held with a description after the run, joined to the
export before it by board and external id:

| | Vacancies |
| --- | ---: |
| Not held before | 54 933 |
| Held before without a description | 411 |
| Same description as held | 91 242 |
| … and the same title | 90 608 |
| Description changed | 9301 |

**58% of the descriptions read were the one already held**, a month after the snapshot's sweep. The
external id is the title and requisition id (`Retail-Sales---Part-Time_JR-02668850`), so a reposted
or retitled vacancy has a new one: Workday's 357 440 added and 367 824 deleted are vacancies, not ids
changing. 117 359 of the added share a title with one deleted from the same board, which is reposting
(retail most of all).

## The longest boards

A board's requests are asked one after another, so a board takes as long as its requests at about one
a second, whatever the throttle allows. 21 boards took more than 30 minutes; the 20 longest were 22%
of the summed board time. None held the end of the run alone: the last board open alone,
`wvumedicine.wd1/wvuh`, was open about 15 minutes, the last 3 of them alone.

| Board | Read | Vacancies | List pages | Parts | Descriptions |
| --- | --- | ---: | ---: | ---: | ---: |
| `cvshealth.wd1/cvs_health_careers` | 2h 56m | 18 728 | 5269 | 4950 | 237 |
| `oreillyauto.wd1/oreilly` | 2h 9m | 19 235 | 5969 | 5955 | 37 |
| `familydollar.wd503/familydollarus` | 1h 20m | 10 430 | 4469 | 4381 | 24 |
| `tjx.wd1/tjx_external` | 1h 4m | 10 477 | 3124 | 3113 | 132 |
| `ghr.wd1/us-emplsv` | 1h 3m | 3063 | 2886 | 2778 | 546 |
| `hitachi.wd1/hitachi` | 57m | 4178 | 1106 | 120 | 1448 |
| `thales.wd3/careers` | 55m | 2966 | 886 | 275 | 1595 |
| `globalhr.wd5/rec_rtx_ext_gateway` | 50m | 4551 | 662 | 556 | 1845 |
| `dollartree.wd5/dollartreeus` | 49m, unreadable | | 2033 | 1961 | |
| `davita.wd1/dkc_external` | 42m | 3282 | 2203 | 2199 | 41 |

The rest of the top 20 (NVIDIA, Lowe's, Northrop Grumman, Applied Materials, PwC, Leidos, Citi, Booz
Allen, Bridgestone, Capital One) took 33 to 42 minutes each, most of it descriptions.

## Unreadable boards

- `aah.wd5/external`, `aah.wd5/scotland-external`, `aah.wd5/external_private_marketplace`: their first
  list page answers `410`. The reader counts a board not found only on `400`, `401`, `403`, `404` or
  `422`, so they stay `ACTIVE` and are asked every sweep.
- `adr.wd504/externalcareers`: the TLS handshake failed once. An I/O error is not asked again.
- `dollartree.wd5/dollartreeus`: one list page answered `200` with a body that is not JSON, after 2033
  pages and 49 minutes, and the board was lost whole. A `200` that is not JSON is not asked again.

Neither #88 trigger was met: no board held the end alone for 30 minutes, and no description failed
after its 5 attempts.

## Projection

Time taken as requests over the rate held:

| | Workday requests | At 8 a second | At 16 a second |
| --- | ---: | ---: | ---: |
| This run | 252 436 | 8.6 h | 4.3 h |
| Without the 90 608 descriptions held unchanged | 161 828 | 5.5 h | 2.8 h |
| … and narrowing by the facet of fewest pages (about 39 000 fewer) | about 123 000 | 4.2 h | 2.1 h |

A daily sweep finds far fewer descriptions changed than this one, a month after the last. The last
row did not hold: the facets of fewest pages leave vacancies out (§ Facets that leave vacancies out).

## The second run

Run on 2026-11-19 from what the first run left in dev, a day old, as a daily sweep finds it, with
c40422d: a Workday description is read only for a vacancy not held with one under the same id and
title, and a board is narrowed by the facet of fewest pages among those whose every value holds fewer
than 2000. The first start, at 07:14, died at 09:32 when the machine restarted, and its log with it.
It was started again at 09:53.

| Sweep | Wall time | Boards | Published | Added | Deleted |
| --- | --- | ---: | ---: | ---: | ---: |
| Ashby | 5m 6s | 3627 | 64 039 | 283 | 247 |
| Lever | 7m 46s | 1885 | 70 620 | 842 | 154 |
| Greenhouse | 14m 42s | 6883 | 213 367 | 889 | 863 |
| Workday | stopped at 4h 5m | 6468 of 6469 | | | |

Cloudflare challenged at 09:58, **5 minutes in**, and the throttle held 8 a second for the rest. In
three runs at 16 a second a challenge came 5, 14 and 26 minutes in.

**Descriptions.** 2479 pages read, 153 601 descriptions taken as held: 98% skipped. Workday asked
100 926 requests, 98 447 of them list pages, against 252 436 in the first run.

**List pages.** The facet change did not reach the boards that cost most. The 77 boards narrowed took
48 820 list pages for 13 359 if unpaged, as before: CVS 5246 in 4928 parts, Family Dollar 4469 in 4381,
Dollar Tree 5154 in 5005. On CVS's board every facet but locations has a value of 2000 or more
(`timeType`: 10 624 and 7936), so locations, of 4948 values and 5266 pages, was the only one wholly under
the limit and was chosen, though a time type split again by a further facet takes about 930.

**The end.** At 13:28 every board but `oreillyauto.wd1/oreilly` was done. It was read one request at a
time, about 67 list pages a minute, and had not finished at 13:58, 30 minutes alone and 1h 48m open: a
#88 trigger. The run was stopped there, as #90 step 5 says, and counted as ended: **4h 5m**. O'Reilly was
not mirrored and keeps what it held.

**Unreadable.** 8: the three `aah.wd5` boards (`410`), five lost to an HTTP/2 stream reset
(`RST_STREAM: Protocol error`: `cbgoc.wd3/cb`, `cc.wd3/chanelcareers`, `corpartners.wd5/englemartin`,
`garlandtx.wd1/garlandcareers`, `gapinc.wd1/gapinc`), which is an I/O error and is not asked again.
Dollar Tree, lost in the first run, was read whole: 18 538 vacancies.

## The third run

Started at 14:00 with 86e2e43, which chose among all the facets that split a part, values of 2000 or
more included, the one of fewest pages. Cloudflare challenged at 14:07, 7 minutes in. Ashby took 3m
52s, Lever 11m 37s, Greenhouse 13m 7s. Large Workday boards took far fewer list pages (Bridgestone 62,
from 1672), and far fewer vacancies: Bridgestone 375 of 2958, Advance Auto 1149 of 16 336, Babilou
2078 of 3511, AdventHealth 3220 of 4055, about 20 000 on four boards. The run was stopped at 14:14,
1118 Workday boards in, 86e2e43 reverted (95a373f), and the dump restored.

Advance Auto's facet of fewest pages was `hiringCompany`: 14 values, 3836 vacancies of 16 361. A
vacancy no value of the facet names is in no part, and the mirror deletes it as closed.

## Facets that leave vacancies out

Every rule `narrowest` has had takes a facet that leaves vacancies out when one is on offer. The rule
before #90 took, among the facets whose every value holds fewer than 2000, the one whose values add up
to the fewest vacancies, which favours one that covers few. c40422d, the fewest pages among the same,
favours them too. On Walmart's board (`walmart.wd504/walmartexternal`, about 20 100 vacancies by its
country facet) `company` names 412 of them in 55 values; on Circle K's, `Job_Requisition` names 55 of
10 906; many boards have `distance`, five values of one vacancy each. Workday gives these boards'
`total` as 2000, so nothing in the list says how many are left out.

Each of the 78 boards narrowed in the second run was asked once more for its first page (2026-11-19,
14:20), and what it read set against its country facet's count, which names every vacancy once. 61
read 90% or more of it. 17 read less, about 57 700 vacancies short in all:

| Board | Read | By its country facet |
| --- | ---: | ---: |
| `walmart.wd504/walmartexternal` | 412 | 20 138 |
| `circlek.wd3/circlekstorejobs` | 55 | 10 906 |
| `citi.wd5/2` | 3168 | 4256 |
| `michaels.wd5/external` | 547 | 4190 |
| `freseniusmedicalcare.wd3/fme` | 497 | 3828 |
| `thales.wd3/careers` | 2960 | 3717 |
| `ag.wd3/airbus` | 368 | 3030 |
| `abb.wd3/external_career_page` | 1297 | 2492 |
| `tapestry.wd108/tapestry_careers` | 95 | 2478 |
| `walmart.wd504/non-workdayinternal` | 178 | 2462 |
| `tmobile.wd1/external` | 688 | 2411 |
| `mercy.wd1/mercycareers` | 313 | 2398 |
| `drivenbrands.wd1/drivenbrandscareersite` | 235 | 2381 |
| `abbott.wd5/abbottcareers` | 1756 | 2379 |
| `communicarehealth.wd1/chsexternalcareersite` | 1270 | 2139 |
| `hcmportal.wd5/search` | 684 | 2064 |
| `eiffage.wd3/eiffage_careers` | 1067 | 2008 |

A facet is safe to narrow by only if every vacancy of the part has one of its values. A count can say a
facet covers too few, but not that it covers all; the list gives no way to ask for vacancies a facet
leaves out. The coverage table is in `~/oneprofile-sweep-logs/facet-coverage.tsv`.

## Where the time goes now

With descriptions read only where none is held, the second run's Workday sweep asked 100 926
requests, 98% of them list pages, and ran at 8 a second from minute 5. That is 3h 30m of requests; the
rest of its 4h 5m was O'Reilly read alone. Three things set the time from here:

- **The pace.** 16 a second has drawn a Cloudflare challenge 5, 7, 14 and 26 minutes in, in every run,
  and the throttle then holds 8 for the rest of it. At 16 the second run's requests take 1h 45m.
- **One board at a time.** A board's requests are asked one after another, about one a second, so a
  board of 5000 list pages takes over an hour whatever the pace: O'Reilly, CVS and Dollar Tree took about
  1h 50m each in the second run. Reading a board's requests at once is #88 proposal 1, now for its
  list pages more than its descriptions, and the second run met #88's trigger.
- **The facet rule.** The 78 narrowed boards took 48 820 list pages where 13 400 would do, and the rule
  that chooses their facets loses vacancies (§ above). A rule that both covers every vacancy and takes
  few pages is not settled.

## Unreadable boards, all runs

- `aah.wd5/*` (three boards): `410` on the first page; not counted as not found.
- An I/O error is not asked again: a TLS handshake (`adr.wd504/externalcareers`, first run) and five
  HTTP/2 stream resets (`RST_STREAM: Protocol error`, second run: `cbgoc.wd3/cb`,
  `cc.wd3/chanelcareers`, `corpartners.wd5/englemartin`, `garlandtx.wd1/garlandcareers`,
  `gapinc.wd1/gapinc`) each cost a whole board.
- A `200` that is not JSON is not asked again: Dollar Tree, after 2033 pages, in the first run.
