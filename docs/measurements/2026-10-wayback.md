# Greenhouse and Ashby discovered from the Wayback Machine

For #78. ADR-0023 asks for the way Wayback's CDX index is read to be settled by research before the
first full read. This file holds that research. The run's figures are added below it once the run
has been done.

## How Wayback's CDX is read

Measured on 2026-10-21 from the development machine, in about 40 queries against
`https://web.archive.org/cdx/search/cdx`, checked against the CDX server's documentation
(`internetarchive/wayback`, `wayback-cdx-server/README.md`). The server answering today is not the
one that documentation describes: every response carries `x-be: wb_cdx_pygwb` (pywb). Where the two
disagree, what was measured wins.

### Size of the read

`showNumPages=true` (asked without `fl`, which turns the answer into `-`):

| Prefix | Pages |
| --- | ---: |
| `boards.greenhouse.io/` | 298 |
| `job-boards.greenhouse.io/` | 198 |
| `boards.eu.greenhouse.io/` | 8 |
| `job-boards.eu.greenhouse.io/` | 14 |
| `jobs.ashbyhq.com/` | 53 |

A page holds about 6,000 lines (3,800 to 6,350 measured), so Greenhouse is about 3.1 million
captures and Ashby about 320,000. September counted 450 Greenhouse pages over the two non-EU hosts;
there are 496 now.

### Paging against a resume key: the resume key, about ten times faster

| Method | Measured | Lines a second |
| --- | --- | ---: |
| `page=N`, one after another | 4 pages, 12–39 s each | ~170 |
| `page=N`, 4 at a time | 4 pages in 40 s | ~600 |
| `page=N`, 8 at a time | 7 pages in 39 s, the eighth a 504 at 60 s | ~800 |
| No `page`, `limit=50000&showResumeKey=true` | 50,000 lines in 20–23 s | ~2,300 |
| No `page`, `limit=150000` | 150,000 lines in 75 s | ~2,000 |
| No `page`, `limit=300000` | 300,000 lines in 125 s | ~2,400 |

A paged request spends most of its time on the server (`x-tr` 8.6 s for a 4,000-line page), and the
time varies from 5 s to 44 s for pages of the same size. The unpaged query streams its result. The
documented maximum of 150,000 lines a query is not enforced: 300,000 came back whole.

- **The read is one unpaged query per URL prefix, continued by resume key**, 50,000 lines a request:
  `url=<host>/&matchType=prefix&limit=50000&showResumeKey=true`. The body ends with an empty line and
  the key while more remains, and the next request adds `resumeKey=<key>`. The same key can be
  asked again after a failure and resumes at the same capture.
- Chunks of 50,000 keep each request well under the gateway's 60 s timeout. A request that runs
  past 60 s is answered `504 Gateway Time-out` by nginx.
- Estimated wall clock for the backfill: Greenhouse about 25 minutes, Ashby about 3, plus waits
  after refusals. September's paged read at a 2 s pause took about 40 minutes for 450 pages.

### Running in parallel: no

Six unpaged queries at once (`boards.greenhouse.io/a` … `/f`) got five refusals within 6 s, and two
`from=` pages asked 10 s apart were refused too. Since a single stream already outruns eight
parallel pages, the reader asks one request at a time.

### Refusals and limits

- **The server refuses by answering `503` with an HTML page titled "Internet Archive: Temporarily
  Offline"**, not 429. No `Retry-After` header is sent. It came within 6 s of the request, on the
  first query of a session as much as after a burst; `x-rl` and `x-na` were `0` on every response,
  refused or not. No 429 was seen in about 40 requests.
- Waiting 20 s and asking the refused request again succeeded, the one time it was tried. How long
  a refusal lasts under a sustained read is not known yet; the first full read will show it.
- **A key taken from a refused response is garbage**: read from the 503's HTML it gave `400` on the
  next request. The reader takes a key only from a `200`.
- The reader therefore treats anything but `200` as no answer at all, waits (30 s, doubling to
  5 minutes) and asks the same request again, and gives the run up after a number of attempts
  rather than skipping a chunk.

### What a query can leave out: only bytes, never time

| Parameter | Effect |
| --- | --- |
| `fl=original` | 37% of the bytes (319 KB against 873 KB on one page), the same lines, no faster: one page took 44 s with it and 15 s without. |
| `collapse=urlkey` | `504` at 60 s on a page of 3,865 lines. It also collapses only adjacent lines, so it cannot give one line per slug. |
| `filter=statuscode:200` | Drops 5.5% of a page (`404`, `301`, `-`), no faster. Not used: a slug whose captures are all redirects (old `boards.` URLs moved to `job-boards.`) is still a board to probe. |
| `filter=mimetype:text/html` | Not used, for the same reason: what a capture holds is irrelevant, only its URL is read. |

The server scans every capture under the prefix whatever the query leaves out, so no parameter
shortens the read. `fl` is kept to the fields the reader parses, to save transfer.

### Cut lines: the response must close

Responses carry no `Content-Length`, so a cut body cannot be told from a whole one by its headers.
In September a plain-text line cut short made a false slug.

- **The reader asks for `output=json`.** A whole response is a JSON array that closes with `]]`,
  with an empty array and the key as its last two elements while more remains
  (`…,"301"],\n[],\n["eJxF…"]]`). A body that does not parse as a closed array is a failed request
  and is asked again, so no line of it is read.
- `fl=urlkey,timestamp,original,statuscode` keeps the fields worth having in JSON; one 50,000-line
  JSON chunk was 7.4 MB against 3.1 MB for `fl=original` as plain text.

### Incremental runs: `from=` saves nothing

- `from=20261015` without `page` gave `504` at 60 s, even with `limit=2000`: the server scans from
  the start of the prefix for the first capture after the date, and the gateway gives up first.
- With `page`, `from=` does not reduce the page count (53 for Ashby with or without it). It thins
  each page's answer (267 lines instead of 3,865), but each page is still scanned whole and took
  5 s to 44 s.
- The paged index was fresh: on 2026-10-21 its newest Ashby capture was from 2026-10-17.

So an incremental read costs about what a whole read does. A later run reads the whole archive again
by resume key and keeps only the slugs it does not already hold. ADR-0023 first said a run reads
only Wayback's captures since the last run; it was amended to this on 2026-10-21.

### `http://` captures

Wayback holds `http://` captures, which Common Crawl does not, and the discovery rules accept only
`https://`. In 400,000 Greenhouse lines (three chunks, both non-EU hosts) 20,070 were `http://`
(5%), and they named 5 slugs absent from the `https://` lines, out of 2,506. September found 77 of
17,730. The rules stay `https://` only.

## Greenhouse's pace

Measured on 2026-10-21 as `2026-10-ashby.md` § Ashby's pace measured Ashby's: bursts against
discovered slugs, drawn at random from the 2026-10-20 snapshot (`setseed(0.78)` over every
Greenhouse company for probes, `setseed(0.79)` over the active ones for sweeps), asking what the
adapters ask (`/v1/boards/<slug>/jobs` for a probe, with `?content=true&pay_transparency=true` for
a sweep) and sending the app's user agent. 30 s between bursts.

| Burst | Requests | Wall clock | Rate | Answer, median / 95th / max | Answers |
| --- | ---: | ---: | ---: | --- | --- |
| Probe, one after another | 200 | 43 s | 4.7/s | 0.20 / 0.24 / 0.67 s | 164 `200`, 36 `404` |
| Probe, four at a time | 400 | 23 s | 17.3/s | 0.21 / 0.30 / 0.86 s | 341 `200`, 59 `404` |
| Sweep, one after another | 150 | 76 s | 2.0/s | 0.23 / 0.84 / 15.8 s | 150 `200`, 159 MB |
| Sweep, four at a time | 300 | 32 s | 9.3/s | 0.31 / 0.83 / 4.1 s | 300 `200`, 142 MB |

Greenhouse refused none of the 1050 requests. A sweep's answer is a whole board's descriptions, yet
it was refused no sooner than a probe's: the slow sweeps are the large boards (one of 15.8 s), not
throttling.

The code paces both at **250 ms a board**, as Ashby is: about the rate it served one after another,
a quarter of what it served four at a time for probes and under half for sweeps. That replaces 130
boards a minute for the probe (about 460 ms, carried from before the reset) and 500 ms for the
sweep. A run asks one board at a time, so a sweep is held back by Greenhouse's answers more than by
its pace. At this pace the 6890 companies of the 2026-10-20 snapshot probe in about 29 minutes,
against 55 measured at the old pace. The bursts were short; the run is the longer test, and a
refusal in it sets the pace back.

## The backfill

Run on the development machine on 2026-10-21 and 22, from the `classified-2026-10-20` snapshot
(Greenhouse 6890 companies and Ashby 4597, all discovered from Common Crawl). Wayback was read whole
through `POST /discoveries/{ats}/wayback` (4199e8d), Greenhouse's four hosts and then Ashby's one.
Then every company of both ATSs was probed, both ATSs were swept at the same time, each by its own
request, and the whole corpus went through cleaning, title classification, skill extraction and the
body pass. Times are -03:00.

### Snapshots

They supersede the 2026-10-20 pair as what dev restores (ADR-0006); that pair moved to
`~/oneprofile-snapshots/archive/`.

- `~/oneprofile-snapshots/raw-2026-10-22.dump`, 282 MB: the corpus as the sweeps left it, with the
  data of `normalized_vacancy`, `normalized_vacancy_title_seniority` and `vacancy_skill` left out.
- `~/oneprofile-snapshots/classified-2026-10-22.dump`, 600 MB: after cleaning, title
  classification, skill extraction and the body pass.

Both were cut after the body pass; the raw one leaves the derived tables' data out of the same
database.

### The run

| Phase | Wall clock | What it reported |
| --- | --- | --- |
| Wayback, Greenhouse | 1h 27m (21:50–23:18) | 3 366 600 captures, 18 706 slugs, 12 035 companies added |
| Wayback, Ashby | 9m 50s | 422 212 captures, 7669 slugs, 3323 companies added |
| Greenhouse probe | 85m 50s | 7032 active, 1447 empty, 10 446 not found, 0 unreadable |
| Ashby probe | 35m 37s | 3737 active, 403 empty, 3780 not found, 0 unreadable |
| Greenhouse sweep | 56m 43s | 7032 boards, 218 617 published: 45 701 added, 172 916 updated, 4934 deleted, 0 unreadable |
| Ashby sweep, beside it | 22m 41s | 3737 boards, 65 418 published: 5655 added, 59 763 updated, 1202 deleted, 0 unreadable |
| `POST /cleanings` | 6m 9s | 286 082 vacancies, 177 020 distinct titles, 159 256 once cleaned, 66 139 languages detected |
| `POST /classifications` | 2m 10s | 49 491 `IN`, 190 098 `OUT`, 46 493 `UNKNOWN` |
| `POST /skill-extractions` | 49s | 85 691 vacancies, 513 955 rows |
| `POST /body-classifications` | 55s | pile of 37 498: 14 811 `IN`, 21 330 `OUT`, 59 unknown, 1298 `unsupported_language` |
| `pg_dump -Fc`, classified | 1m 13s | 600 MB |
| `pg_dump -Fc`, raw | 34s | 282 MB |

### Wayback's refusals under a whole read

The Greenhouse read took 1h 27m against the 25 minutes estimated above, and the difference is the
waits. Wayback refused 35 of its requests: 22 with the `503` page and 13 with a `504`, all on
`boards.greenhouse.io/` and `job-boards.greenhouse.io/`, none on the two EU hosts. A refusal came
every two to five minutes for the whole read. 24 cleared on the first retry after 30 s; the worst
was five in a row from 22:40 to 22:49, cleared on the sixth attempt after waits of 30 s, 1, 2, 4 and
5 minutes. Ashby's read was refused five times (three `503`, two `504`), never more than twice in a
row. No request reached the tenth attempt, so no read was given up. Before the run, Wayback had
refused a three-line query five times in three minutes, so a refusal says nothing about the size of
what is asked.

So a whole read of Greenhouse is about an hour and a half while the server behaves as it did, and
the reader's ten attempts, about 32 minutes of waiting for one request, were never close to spent.

Where the Greenhouse read's 87 minutes went, from the app's log:

| | Minutes |
| --- | ---: |
| 69 requests answered whole, 50,000 captures each | 39 |
| Waits after a refusal | 33 |
| Requests refused with `504`, about 60 s each | 13 |
| Requests refused with `503`, a few seconds each | 2 |

A whole chunk took 34 s at the median, 53 s at the 90th percentile and 64 s at most, against
20–23 s in the research: the server streamed about 1,400 captures a second, not 2,300. At that
speed a chunk of 50,000 runs close to the gateway's 60 s, and a slow one is lost as a `504` and then
waited after. The reader now asks for chunks of 25,000 (about 17 s each at that speed), which should
leave most of the 13 minutes of `504`s and the waits that followed them out of the next read. The
`503`s come fast and on queries of any size, so a smaller chunk does nothing for them. Reading
alone sets the floor, about 40 minutes for Greenhouse.

### Greenhouse and Ashby at the pace of 250 ms

Neither ATS refused a request in the probes or the sweeps: 18 925 Greenhouse boards probed at about
3.7 a second and 7032 swept, 7920 Ashby boards probed and 3737 swept, with no unreadable board.
The pace stays at 250 ms a board for both.

### What each archive added

Common Crawl's slugs are the companies held before the run; Wayback's are the run's. "Live" is
`ACTIVE` at this run's probe.

| | Greenhouse | Ashby |
| --- | ---: | ---: |
| Common Crawl slugs (12 crawls) | 6890 | 4597 |
| Wayback slugs | 18 706 | 7669 |
| … also in Common Crawl | 6671 | 4346 |
| Common Crawl only | 219 | 251 |
| **Wayback only (companies added)** | **12 035** | **3323** |
| Live, Common Crawl's companies | 5006 (72.7%) | 3328 (72.4%) |
| Live, Wayback only | 2026 (16.8%) | 409 (12.3%) |
| Vacancies on Wayback-only boards | 40 448 | 4263 |
| … per live board | 20.0 | 10.4 |
| Engineering, Wayback-only boards | 7079 | 1397 |
| … per live board | 3.5 | 3.4 |
| Engineering per live board, Common Crawl's | 6.8 | 6.4 |

Wayback names nearly every slug Common Crawl does, and three to four times as many. Most of what
only it names is dead: September's sample put Wayback-only Greenhouse slugs at 23% live, and the
whole set probes at 16.8%. The boards that are live are about half the size of Common Crawl's, in
vacancies and in engineering alike: an archive that reaches back years holds the small and the
short-lived boards a recent crawl misses. Still, Wayback adds 40% to Greenhouse's live boards (5004
to 7032) and 12% to Ashby's (3332 to 3737). 33 Greenhouse and 19 Ashby companies held before changed
board status since 2026-10-20.

Greenhouse's EU boards needed nothing of their own: `boards-api.eu.greenhouse.io` does not resolve,
and `boards-api.greenhouse.io` serves the EU boards, so the probe and the sweep read them with the
rest (ADR-0022, amended). A company holds no host, so how many of the live boards are European is
not counted here.

### Per ATS

| | Greenhouse | Ashby | Both |
| --- | ---: | ---: | ---: |
| Companies | 18 925 | 7920 | 26 845 |
| Live (`ACTIVE`) | 7032 | 3737 | 10 769 |
| Vacancies | 220 645 | 65 437 | 286 082 |
| … on live boards | 218 617 | 65 418 | 284 035 |
| **Engineering subset** | **41 727** (18.9%) | **22 575** (34.5%) | **64 302** |
| … decided by the title | 32 217 | 17 274 | 49 491 |
| … decided by the body | 9510 | 5301 | 14 811 |
| **Live in one sweep** | **41 251** | **22 564** | **63 815** |

As in the Ashby run, vacancies of boards a probe found empty or gone since they were swept stay
held, since a sweep reads only active boards: 2028 Greenhouse and 19 Ashby vacancies, 487 of them
engineering. The figure **live in one sweep** that PRD §12.6 gates on leaves them out: one query
over `vacancy`, `company` and `normalized_vacancy`, counting `classification_state = 'IN'` on
companies whose board status is `ACTIVE`.

## How far #70 still is

**63 815 engineering vacancies live in one sweep, against 100 000: 36 185 to go.** The 2026-10-20
snapshot had 55 331, so the Wayback backfill added 8484 (8476 from the boards only Wayback names,
the rest from the boards held before). ADR-0022 counted about 45 000 to go when it chose Lever,
Workable and Workday; Wayback for Greenhouse and Ashby closed under a fifth of that, and #70 still
needs a new ATS. Lever's Wayback and Common Crawl reads come with it (ADR-0023).
