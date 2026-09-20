# The raw corpus, built from twelve crawls

Run on the development machine on 2026-09-19 and 2026-09-20, from an empty database, over the
twelve latest crawls, newest to oldest:

`CC-MAIN-2026-39`, `-34`, `-30`, `-25`, `-21`, `-17`, `-12`, `-08`, `-04`, `CC-MAIN-2025-51`,
`-47`, `-43`.

Probing was paced at 130 boards a minute and sweeping at 500 ms a board, the paces the code
carries. The snapshot is `~/oneprofile-snapshots/raw-2026-09-20.dump`, 132 MB, written with
`pg_dump -Fc`. `scripts/restore-snapshot.sh` resets the database and puts it back in 25 seconds.

| Phase | Wall clock | What it reported |
| --- | --- | --- |
| Twelve discoveries | 3m 41s | 6890 companies |
| One probe over the union | 1h 5m 34s | 5136 active, 752 empty, 1002 not found, 0 unreadable |
| One sweep over the active boards | 59m 27s | 5136 boards, 179 098 vacancies published, all of them new |
| `pg_dump -Fc` | 17s | 132 MB |
| **The run** | **2h 9m 6s** | |

## What each crawl was worth

Total slugs is what the crawl names on its own, measured by `scripts/discover-crawl.sh` against an
empty database. New companies is what it added in the big run, where it was read after every newer
crawl, so it is the crawl's marginal contribution. Live rate and vacancies per live board are the
crawl's own slugs looked up in the built corpus, so a slug several crawls name is counted in each
of their rows; the totals row counts the union once.

| Crawl | Total slugs | New companies | Live rate | Vacancies per live board |
| --- | ---: | ---: | ---: | ---: |
| CC-MAIN-2026-39 | 3653 | 3653 | 92.8% | 42.7 |
| CC-MAIN-2026-34 | 3350 | 672 | 91.8% | 41.5 |
| CC-MAIN-2026-30 | 3585 | 565 | 89.5% | 42.4 |
| CC-MAIN-2026-25 | 3123 | 343 | 87.6% | 42.7 |
| CC-MAIN-2026-21 | 3280 | 303 | 84.7% | 42.8 |
| CC-MAIN-2026-17 | 3260 | 271 | 83.1% | 36.8 |
| CC-MAIN-2026-12 | 3219 | 256 | 81.2% | 40.6 |
| CC-MAIN-2026-08 | 3200 | 187 | 79.3% | 38.9 |
| CC-MAIN-2026-04 | 3405 | 203 | 76.9% | 38.4 |
| CC-MAIN-2025-51 | 3251 | 189 | 76.0% | 39.7 |
| CC-MAIN-2025-47 | 3274 | 115 | 75.3% | 39.1 |
| CC-MAIN-2025-43 | 3263 | 133 | 73.9% | 39.7 |
| **All twelve** | **6890** | **6890** | **74.5%** | **34.9** |
