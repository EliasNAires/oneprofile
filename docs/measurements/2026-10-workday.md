# Workday, researched before it is added (#81)

Research for #81, run on the development machine on 2026-10-23 (ADR-0025), while Workable's block
(`2026-10-workable.md`) held #80 up. Workday is not in the `classified-2026-10-22-lever` snapshot,
so its boards were read from Common Crawl for this research rather than drawn from the snapshot.
Times are -03:00.

## Workday's API

Checked by hand against `<tenant>.<host>.myworkdayjobs.com`, Workday's careers-site host, under
`/wday/cxs/<tenant>/<site>`.

| Request | What it answers |
| --- | --- |
| `POST …/jobs` with `{"appliedFacets":{},"limit":20,"offset":0,"searchText":""}` | One page of the board's vacancies: `total`, and for each vacancy its title, `externalPath`, `locationsText`, `timeType`, `postedOn` (relative: "Posted Today") and the requisition id. No description. `limit` over 20 answers `400` |
| `GET …/job/<externalPath>` | One vacancy: its description as HTML, location, country, `startDate`, time type, requisition id, and `hiringOrganization.name`, the company's name |
| `GET …/sidebar`, `GET …/approot` | The careers site's page content and settings |
| `GET <tenant>.<host>.myworkdayjobs.com/<site>/siteMap.xml` | Vacancy URLs, but not reliably all: 1237 of 1237 for 2020 Companies, 100 of 2000 or more for NVIDIA |

- **A board is a tenant, a host and a site**: `nvidia` + `wd5` + `NVIDIAExternalCareerSite`. A
  company can publish several sites; 928 of the 3691 live tenants below have more than one live
  site, which can list the same vacancies.
- **A probe is one list request** with `limit` 1: `total` says whether the board has a vacancy.
- **A sweep pages the list 20 at a time and asks for each description apart.** Paging stops at an
  offset of 2000 (third-party reports say it then answers page one again, `hiver-data/workday-jobs-scraper`);
  `total` can be larger (up to 25 124 seen), so a larger board is read in parts by facet.
  `total` is on every page for some boards and only the first for others.
- **What a board that cannot be read answers**, each with Workday's JSON error body
  (`{"errorCode":…,"httpStatus":…}`): `422` for a tenant or site that does not exist, or the
  wrong host; `404` "not found: Job_Posting_Site_ID"; `403` `S22` "permission denied" for a
  site that is not public; `401` "Unable to verify credentials for system account". None is a
  refusal.
- **Workday names the company**, in a vacancy's `hiringOrganization`.
- `impl-wdN` and `wdN-impl` hosts are Workday's implementation tenants, test boards (#83).

## Where boards are published

Read from the cluster index of the 12 newest crawls, CC-MAIN-2025-47 to CC-MAIN-2026-43, under the
SURT prefixes `com,myworkdayjobs,` and `com,myworkdaysite,` (ADR-0001's range requests;
`index.commoncrawl.org` timed out on the prefix that day). Common Crawl's crawler is not blocked:
captures answer `200`.

- **`<tenant>.<host>.myworkdayjobs.com/[<locale>/]<site>/…`**: the tenant and host are the
  hostname's two labels, the site the first path segment after an optional locale (`en-US`).
- **`<host>.myworkdaysite.com/[<locale>/]recruiting/<tenant>/<site>/…`** is the same board's other
  address, read through the `myworkdayjobs.com` API.
- **Hosts**: `wd1` holds 3014 of the boards, `wd5` 1758, `wd3` 1737, then `wd12`, `wd103`,
  `wd108`, `wd501`, `wd503` and 12 more. One limit covers them all (§ The bursts), so they are
  not regions.

| Crawl | Captures, `myworkdayjobs` | Captures, `myworkdaysite` | Boards | New boards |
| --- | ---: | ---: | ---: | ---: |
| CC-MAIN-2026-43 | 77 200 | 2601 | 3067 | 3067 |
| CC-MAIN-2026-39 | 68 982 | 1223 | 3049 | 1156 |
| CC-MAIN-2026-34 | 62 880 | 1433 | 2628 | 724 |
| CC-MAIN-2026-30 | 60 401 | 1890 | 2567 | 555 |
| CC-MAIN-2026-25 | 56 696 | 1497 | 2410 | 373 |
| CC-MAIN-2026-21 | 65 027 | 1419 | 3102 | 598 |
| CC-MAIN-2026-17 | 65 302 | 1781 | 3226 | 485 |
| CC-MAIN-2026-12 | 63 910 | 1441 | 3317 | 431 |
| CC-MAIN-2026-08 | 50 634 | 1560 | 2360 | 205 |
| CC-MAIN-2026-04 | 49 638 | 989 | 2048 | 132 |
| CC-MAIN-2025-51 | 51 730 | 935 | 2199 | 107 |
| CC-MAIN-2025-47 | 55 664 | 2322 | 2337 | 137 |
| **All twelve** | | | | **7970**, of 4079 tenants |

A board is counted once by tenant, host and site, the site in lower case. A few sites read from
URLs are not boards (`job`, a blank one); they answer `422` or `400` like a missing board.

## What the boards hold

Every one of the 7970 boards was probed (§ The bursts); each counts by its last answer.

| | Boards | |
| --- | ---: | --- |
| Live (a vacancy or more) | 6006 | 75.4% |
| Empty | 809 | 10.2% |
| Not found (`404`, `422`) | 906 | 11.4% |
| Not public (`401`, `403`) | 237 | 3.0% |
| Other (`400`, `502`, `520`, no DNS) | 12 | 0.2% |
| Live, of those in CC-MAIN-2026-43 | 2629 of 3066 | 85.7%, 268.4 vacancies a live board |
| Live, of those only in older crawls | 3377 of 4904 | 68.9%, 96.0 vacancies a live board |

A live board holds 171.5 vacancies by `total`, median 38; 81 hold 2000 or more. That is 1 030 000
vacancies over the 6006 live boards, or 913 000 counting only each tenant's largest site, before
Wayback.

### Titles

Every title of 300 live boards drawn at random (seed 81), up to 2000 a board: 2115 list requests,
38 789 vacancies, run through `TitleCleaningRule` and `TitleClassificationRule` as they stand on
2026-10-24.

| | Titles | Share | 90% range, boards resampled |
| --- | ---: | ---: | --- |
| `IN` | 1969 | 5.1% | 3.8 to 6.7% |
| `UNKNOWN` | 6991 | 18.0% | 15.6 to 20.7% |
| `OUT` | 29 829 | 76.9% | |

- **`IN` is 5.1%, not #74's 2.1%**, which was read from 50 of freehire's boards. That is 47 000 to
  52 000 title `IN` vacancies over Workday's 913 000 to 1 030 000 (35 000 to 69 000 over the
  range), against the 23 633 #70 still needs before Workable (`2026-10-lever.md`).
- **The largest boards are the least technical**: the 10 largest of the 300 (a Wendy's franchisee,
  Michaels, Brookshire's, J.Crew, hospitals) hold 0 to 128 `IN` titles each. Without them `IN` is
  6.8% and `UNKNOWN` 19.1%.
- **`UNKNOWN` reasons**: 3326 unruled (mostly healthcare and trades shorthand: "PCT Med Surg Unit",
  "CSR"), 2332 domain ambiguity, 1333 scope ambiguity.

### What the body pass adds

What the body pass made of title `UNKNOWN` vacancies in `classified-2026-10-22-lever`, by the reason
the title gave:

| Reason | Vacancies | Body pass `IN` | Ashby / Greenhouse / Lever | Share of Workday's titles |
| --- | ---: | ---: | --- | ---: |
| Domain ambiguity | 22 459 | 44.3% | 56.5 / 39.5 / 41.6% | 6.0% |
| Scope ambiguity | 20 585 | 36.9% | 37.8 / 35.9 / 38.7% | 3.4% |
| Unruled | 14 029 | 0%: the body pass does not read them | | 8.6% |

- **At these rates the body pass adds 3.9% of Workday's titles** (3.6% at Greenhouse's), for 8.9%
  `IN` in all. Workday's ambiguous titles may be less technical than ours; at half the rate it is
  5.1% + 1.9% = 7.0%.
- **That is 81 000 to 92 000 `IN` vacancies** over 913 000 to 1 030 000, and 52 000 at the low
  end (3.8% by title, half the body pass's rate, 913 000 vacancies).
- **An unruled title's description is never read by the body pass**, so a sweep does not ask for
  it (§ The setting): 14.5% of the descriptions, not 23.1%.

### Repeated vacancies

How many of the snapshot's 75 677 `IN` vacancies a dedup would fold, by what counts as the same
vacancy:

| Same company and… | Ashby | Greenhouse | Lever | All |
| --- | ---: | ---: | ---: | ---: |
| title | 18.2% | 17.7% | 29.5% | 19.8% |
| title and description | 7.9% | 4.7% | 13.0% | 7.0% |
| title and location | 5.1% | 4.8% | 4.7% | 4.8% |

A tenant's several Workday sites can list the same vacancy too, which the 913 000 already leaves out.

## What the sources say

| | Workday |
| --- | --- |
| Documented limit on the API a run reads | None found (source) |
| Terms of use | Workday's site terms cover `workday.com`, pages that reference them, and "Workday APIs", and forbid data mining, robots and scraping, `workday.com/en-us/legal/site-terms.html` (source). Careers sites link no terms of their own, only Workday's recruiting privacy statement and a candidate account agreement (test). Elias decided on 2026-10-23 to go on |
| `robots.txt` | Allows the site's path and disallows only `/refreshFacet/myworkdaysite`; it names the site's sitemap (test) |
| `X-RateLimit-*` or `Retry-After` headers | None on any answer, refusals included (test) |
| What sits in front | Cloudflare, with `cf-cache-status: DYNAMIC` and `cache-control: no-store`: nothing is served from cache, so a board asked twice reaches Workday twice (test) |
| Region hosts | None: the `wdN` hosts share one limit (test) |

## What a refusal looks like

- **Cloudflare's challenge**: `429` with `cf-mitigated: challenge` and a 28 KB HTML challenge page,
  without `Retry-After`. It refused requests to several tenants within half a second, so it
  counts this machine's requests to every Workday host together. The first time, it had
  cleared when checked 60 s later; the second, by the next request, 5 minutes later.
- **Workday's own `429`**, with its JSON error body, came from one tenant at a time: Accenture
  three times and Werner once. It is that tenant's limit, not the run's.
- **Not refusals**: `502` (Cloudflare's page or Workday's JSON), `520` "error code: 520" and `503`
  "no healthy upstream", each from one tenant's server: AMB Group's board answered `520` once
  and `200` when asked again.

## The bursts

`scripts/pace-ramp`'s method, with its own script, since Workday's list is a `POST`: requests a
number at a time, optionally no faster than a rate, stopping at the first refusal. No board was
asked twice in the ramp; the long probe asked each board again, which nothing caches.

| Burst | At a time | Rate | Requests | Answer, median / 95th / max | First refusal |
| --- | ---: | ---: | ---: | --- | --- |
| Sample at 1/s | 1 | 0.8/s | 300 | 1.07 / 1.87 / 6.9 s | none |
| Probe ramp (11:40) | 1 | 1.0/s | 200 | 0.98 / 1.71 / 3.2 s | none |
| | 4 | 3.8/s | 200 | 1.04 / 1.59 / 2.5 s | none |
| | 8 | 7.4/s | 320 | 1.08 / 1.54 / 3.3 s | none |
| | 16 | 15.3/s | 640 | 0.98 / 1.46 / 4.6 s | none |
| | 32 | about 31/s | about 1280 | 1.00 / 1.52 / 6.7 s | **challenge after 40.9 s** |
| Probe, 4390 boards (14:41) | 20 | 16/s set, 15.3/s | 4390 | 1.01 / 1.53 / 9.1 s | none |
| Sweep with a probe at 16/s beside it (14:51) | 8 + 20 | about 25/s together | | | **challenge on both, 7.4 s and 140 s in** |
| Sweep ramp alone, one tenant a level (15:00) | 1 | 2.0 to 2.1/s | 100 list + 150 detail | 0.46 / 0.59 / 2.0 s | none |
| | 4 | 3.3 to 4.9/s | 100 + 150 | 0.75 / 1.21 / 3.2 s | none |
| | 8 | 5.9 to 7.1/s | 100 + 160 | 0.51 / 1.01 / 12.1 s | none |
| | 16 | 17.1 to 18.9/s | 100 + 320 | 0.59 / 1.18 / 12.8 s | none |
| Long probe at 16/s (17:01) | 20 | 14.2/s | 28 800 | 1.01 / 1.57 / 121.8 s | none |

- **The ceiling is between 15 and 31 a second**, for this machine across every Workday host. 32
  at a time was refused in 41 s; the probe and the sweep together, at about 25 a second, in 7 s
  and 140 s.
- **16 a second held for 34 minutes**: 28 800 requests, 15 to 16 a second in each 5 minutes, no
  refusal. Workable's longer window refused after about 4 minutes.
- **The sweep shares the probe's limit.** Each alone held; together they were refused at once.
- **No limit a tenant.** One tenant answered 17 to 19 requests a second, its list and its
  descriptions alike. Accenture's `429` is its own.
- **Answers take about 1 s for a probe and 0.5 s for a description**, at every rate, so the
  limit holds a run, not Workday's latency.
- The long probe was run three times: the first stopped on a `520` and the second on a `503` "no
  healthy upstream", which the script took for refusals until it was fixed.

## The setting

Decided with Elias on 2026-10-23: **Workday runs at 16 requests a second, at most 20 open at once,
for probes and sweeps together**, after the long probe held. This departs from ADR-0025's half of
the ceiling (7.5 a second, 8 at a time): the ceiling was not found more exactly than 15 to 31 a
second, and 16 a second held over a sustained burst, which Workable's did not. A probe and a sweep
of Workday at once share the 16.

**A sweep reads a description only for a vacancy whose title is `IN`, or `UNKNOWN` for domain or
scope ambiguity**, decided with Elias on 2026-10-23 and 2026-10-24. A description for every vacancy
would be about a million requests, 18 hours at 16 a second. The body pass never reads an unruled
title's description, so it is not asked for either; nor are title `OUT` vacancies', which the body
pass then never sees.

| Run | Requests | At 16 a second |
| --- | ---: | --- |
| Probe, the 7970 Common Crawl boards | 7970 | 8m 18s |
| Sweep's list, 6006 live boards | 54 718 pages, more for the 81 read by facet | 57 minutes |
| Sweep's descriptions, title `IN`, domain or scope ambiguity | 14.5% of 913 000 to 1 030 000: 132 000 to 150 000 | 2h 18m to 2h 36m |
| **A whole sweep** | | **about 3h 30m** |

## What is left

1. **Wayback** for `*.myworkdayjobs.com` and `*.myworkdaysite.com` (ADR-0023).
2. **What caps it first** beside Workday: the machine's link and `VacancyStore.mirror`, measured
   in the full-scale check.
3. **Facets for a board of 2000 or more**: which facet splits a board into parts under 2000.
4. **Duplicate sites**: whether a tenant's sites list the same vacancies, and which a sweep keeps.
