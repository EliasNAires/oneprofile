# Workable, researched before it is added (#80)

Research for #80, run on the development machine on 2026-10-23, before Workable is probed at all
(ADR-0025). Workable is not in the `classified-2026-10-22-lever` snapshot, so its slugs were read
from Common Crawl for this research rather than drawn from the snapshot. The research stopped
early: Workable blocked the machine's IP for 24 hours (§ The block). What is left is listed at the
end. Times are -03:00.

## Workable's API

Checked by hand against `apply.workable.com`, Workable's careers-page host.

| Request | What it answers |
| --- | --- |
| `GET /api/v1/widget/accounts/<slug>` | The company's name and description, and every published vacancy without its description: title, shortcode, department, employment type, `telecommuting`, country, state, city, every location, `published_on`, `created_at`, education, experience, function, industry, the vacancy's URL. `404` for a slug with no account, and `"jobs": []` for an account with nothing published |
| `GET /api/v1/widget/accounts/<slug>?details=true` | The same, with each vacancy's description as HTML. The description holds the vacancy's description, requirements and benefits one after another, each under its heading |
| `GET /api/v1/accounts/<slug>` | The account: id, name, website, logo, languages |
| `POST /api/v3/accounts/<slug>/jobs` | The careers page's own list, 10 vacancies a page by `nextPage` token, with `workplace` (`remote`, `hybrid`, `on_site`), `language` and `isInternal`, but no description |
| `GET /api/v2/accounts/<slug>/jobs/<shortcode>` | One vacancy: description, requirements and benefits apart, `workplace`, `language` and a pay range (`salary_from`, `salary_to`, currency, frequency) |

- **One request reads a whole board**: the widget with `details=true`. Without it, the same request
  says whether a board has any vacancy, as a probe asks.
- **The widget lists a vacancy once for each of its locations.** `1000heads` answered 18 entries
  for 11 shortcodes, the 11 the v3 list holds. A sweep keeps one vacancy per shortcode.
- **Work mode, language and pay come only one vacancy a request** (v2). The widget gives only
  `telecommuting`, which is false for a hybrid vacancy.
- **Workable names the company**, unlike Ashby and Lever.
- `www.workable.com/api/accounts/<slug>`, the API's older address, redirects to the widget.

## Where boards are published

Read from the cluster index of the 12 newest crawls, CC-MAIN-2025-47 to CC-MAIN-2026-43, under
the SURT prefix `com,workable` (ADR-0001's range requests; `index.commoncrawl.org` answered `504`
that day, and the Wayback Machine was "Temporarily Offline").

- **A board is `apply.workable.com/<slug>`**, and the slug is the first path segment. `/j/<shortcode>`
  is a vacancy's short link and names no slug. No captured slug held an uppercase letter.
- **`<slug>.workable.com` is the board's old address**: each redirects with `301` to
  `apply.workable.com/<slug>/`, so its subdomain is the same slug. Workable's own subdomains
  (`www`, `resources`, `jobs`, `help`, `blog` and others) are not boards.
- **One region.** No EU host was found.
- `jobs.workable.com` is Workable's job search across every company. Its vacancy URLs name the
  company only in a slugified title, so it is not read as a board host.

| Crawl | `apply.workable.com` slugs | Subdomain slugs | … not under `apply` | New slugs |
| --- | ---: | ---: | ---: | ---: |
| CC-MAIN-2026-43 | 3818 | 263 | 42 | 3860 |
| CC-MAIN-2026-39 | 3329 | 198 | 22 | 1130 |
| CC-MAIN-2026-34 | 3155 | 214 | 24 | 889 |
| CC-MAIN-2026-30 | 3149 | 170 | 16 | 594 |
| CC-MAIN-2026-25 | 2949 | 186 | 21 | 452 |
| CC-MAIN-2026-21 | 3078 | 208 | 27 | 393 |
| CC-MAIN-2026-17 | 3261 | 206 | 25 | 291 |
| CC-MAIN-2026-12 | 2582 | 179 | 18 | 189 |
| CC-MAIN-2026-08 | 2661 | 184 | 22 | 250 |
| CC-MAIN-2026-04 | 2849 | 193 | 17 | 187 |
| CC-MAIN-2025-51 | 2681 | 187 | 19 | 174 |
| CC-MAIN-2025-47 | 2839 | 191 | 26 | 159 |
| **All twelve** | | | | **8568** |

## What the boards hold

The 1447 distinct slugs answered `200` or `404` in the bursts below:

| | Slugs | |
| --- | ---: | --- |
| Live (a vacancy or more) | 621 | 42.9% |
| Empty | 736 | 50.9% |
| Not found | 90 | 6.2% |
| Live, of those in CC-MAIN-2026-43 | 377 of 649 | 58.1%, 28.7 vacancies a live board |
| Live, of those only in older crawls | 244 of 798 | 30.6%, 14.9 vacancies a live board |

A live board holds 23.3 vacancies (distinct shortcodes), median 8, at most 976. Over the 8568
slugs that is about 3700 live boards and 85 000 vacancies, before Wayback. At #74's 15.6% of
titles `IN`, that is about 13 000 title `IN` vacancies, against the 23 633 #70 still needs
(`2026-10-lever.md`). Orders of size only: the share is from #74's 50 boards.

## What the sources say

| | Workable |
| --- | --- |
| Documented limit on the API a run reads | None. Workable's documented limits (10 requests per 10 s for an account token, 50 for OAuth and partner tokens, with `X-Rate-Limit-*` headers) are for the key-authenticated API, `workable.readme.io/reference/rate-limits` (source) |
| Terms of use for the board API | None found (source). `apply.workable.com/robots.txt` disallows nothing, and signals `search=yes, ai-input=yes, ai-train=no` (test) |
| `X-RateLimit-*` or `Retry-After` headers | None on an answer. A refusal carries `Retry-After` (test) |
| What sits in front | Cloudflare, with `cf-cache-status: DYNAMIC`: nothing is served from cache (test) |
| Region hosts | One (test) |

## What a refusal looks like

Cloudflare's rate limiting: `429` with the body `error code: 1015`, answered in about 10 ms
without reaching Workable. It counts requests over time, not requests open at once.

- **The first four refusals carried `Retry-After: 0`**, and each cleared within about 20 s.
- **The fifth carried `Retry-After: 86400`** and blocked the IP for 24 hours (§ The block).

## The bursts

`scripts/pace-ramp` first, with its probe request (the widget without `details`), then a script
that starts requests at a fixed rate, at most 4 open, and stops after 10 refusals: the limit is on
the rate, which `pace-ramp`'s levels of requests at once do not set. No slug was asked twice.
The bursts came 30 s to 3 minutes apart, so a window longer than that also counted the burst
before.

| Burst | Rate | Requests | Wall clock | Answer, median / 95th / max | First refusal | Answered before it | `Retry-After` |
| --- | ---: | ---: | ---: | --- | ---: | ---: | --- |
| Ramp, 1 at a time (08:48) | 5.2/s | 200 | 38.8 s | 0.19 / 0.23 / 0.44 s | none | 200 | |
| Ramp, 4 at a time | 19.9/s | 200 | 10.1 s | 0.18 / 0.24 / 0.98 s | none | 200 | |
| Ramp, 8 at a time | 43.1/s | 174 | | 0.19 / 0.24 / 0.55 s | 3.8 s | 151 | 0, cleared in 20 s |
| 10/s (08:52) | 10/s | 149 | | 0.19 / 0.24 / 0.62 s | 12.1 s | 121 | 0 |
| 2/s for 2 min (08:57) | 2.0/s | 240 | 119.7 s | 0.19 / 0.24 / 0.74 s | none | 240 | |
| 4/s for 2 min (09:00) | 4/s | 245 | | 0.19 / 0.26 / 0.41 s | 58.8 s | 235 | 0 |
| 3/s for 5 min (09:05) | 3/s | 395 | | 0.19 / 0.24 / 0.65 s | 128.3 s | 385 | 0 |
| 2/s for 20 min (09:13) | 2/s | 458 | | 0.19 / 0.23 / 0.80 s | 224.0 s | 448 | **86400** |
| 1/s for 25 min (09:44) | 1/s | 10 | | | 0 s | 0 | 84805 |

- **No rate held over time.** 4, 3 and 2 a second were refused after 59 s, 128 s and 224 s. So
  beside a short window there is a longer one, counting something like 450 to 500 requests over
  more than 4 minutes. If that window is 5 minutes, 1 a second holds; if it is 10, only about
  0.75 a second does.
- **Answers are fast**: a median of 0.19 s at every rate, so a run is held by the limit, never by
  Workable's latency or the link.
- The 1 a second burst was refused from its first request: the block had started 27 minutes before.

## The block

At 09:17:17 the 2 a second burst was refused with `Retry-After: 86400`. Every request to
`apply.workable.com` from the machine's IP has since been refused, the careers pages a browser
asks included, until 2026-10-24 09:17. `jobs.workable.com` still answered `200`. It came on the
fifth time the limit was reached in 27 minutes, the first four having cleared in seconds.

So reaching Workable's limit is not cheap, as ADR-0025 assumes when it lets a refused run slow
down and carry on: a few refusals can cost every Workable run for a day.

- **A Workable run must never reach the limit.** Its settings are set below anything that drew a
  refusal, not at half a measured ceiling.
- **A `Retry-After` of hours is a block, not a pause.** ADR-0025's reader waits as long as
  `Retry-After` says. Against Workable it would wait a day; the run has to give Workable's boards
  up instead, each counted unreadable, as a board still refused after its retries is.

## The setting

Decided with Elias on 2026-10-23: **Workable runs at one request a second**, one at a time, for
probes and sweeps together, as a departure from ADR-0025's half of the ceiling, which was not
found before the block. It is checked by a sustained burst at one a second once the block has
lifted (below).

| Run | Requests | At one a second |
| --- | ---: | --- |
| Probe, the 8568 Common Crawl slugs | 8568 | 2h 23m |
| Sweep, about 3700 live boards | about 3700 | about 1h 2m |

Workable's runs go beside the other ATSs', so they hold none of them up. A probe and a sweep of
Workable at once would share the one a second, if the sweep's request counts against the same
limit, which is likely: same host, same Cloudflare rule. Wayback's slugs will lengthen the probe:
for Lever it named 2.7 times Common Crawl's, most of them dead.

The per-vacancy request for work mode, language and pay would make a sweep about 23 times as long,
about a day.

## What is left

From 2026-10-24 09:17, once the block has lifted, and stopping at the first refusal of any kind:

1. **A sustained burst at one a second** of 30 minutes or more, on slugs not yet asked.
2. **Whether the sweep shares the probe's limit**: the sweep's request (`details=true`) alone, then
   with the probe's beside it, at a rate the first step held.
3. **Wayback** for `apply.workable.com/` and `*.workable.com`, once it is back: 254 CDX pages for
   `apply.workable.com/` alone, about 1.5 million captures.
4. **`jobs.workable.com/api/v1/jobs`**, Workable's search across every company, as a source of
   board URLs. Its limit is its own: it answered while `apply.workable.com` was blocked.
