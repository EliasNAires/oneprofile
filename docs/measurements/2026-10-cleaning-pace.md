# Cleaning pace with open-in-view off

#50: `spring.jpa.open-in-view` was on by default, so one persistence context spanned each `POST`,
and every batch cost more than the one before it. It is now off (`0c4adf1`), so each store
transaction opens and closes its own context. This re-times cleaning against the run in
`2026-10-resweep.md`, which is the baseline.

Run on the development machine on 2026-10-06, in dev mode, against
`~/oneprofile-snapshots/raw-2026-10-06.dump` restored with `scripts/restore-snapshot.sh`, which is
the same input the baseline cleaned. Triggered with one `POST /cleanings`. Progress was sampled every
15 s as `count(*)` of `normalized_vacancy`, and heap use with `jcmd <pid> GC.heap_info`. Only
cleaning was re-timed: the sweep reads Greenhouse at a fixed pace, so its wall clock depends on the
remote service as much as on this change.

| | Baseline, open-in-view on | Open-in-view off |
| --- | ---: | ---: |
| Wall clock | 37m 39s | about 4m 15s |
| Pace at the start | about 125 vacancies/s | about 700 vacancies/s |
| Pace at the end | about 75 vacancies/s | about 670 vacancies/s |
| Heap | grew with the run | 150 MB to 320 MB, not growing |

The report is identical to the baseline's: 177 911 vacancies, 114 483 distinct titles, 104 082 once
cleaned, and 9 319 163 segments stored. Turning the setting off changed how long the run takes, not
what it writes.

## Pace through the run

| Seconds | Cleaned | Pace over the last sample | Heap used |
| ---: | ---: | ---: | ---: |
| 16 | 10 000 | 625/s | 184 MB |
| 31 | 22 000 | 800/s | 156 MB |
| 46 | 33 000 | 733/s | 245 MB |
| 61 | 43 000 | 667/s | 217 MB |
| 76 | 54 000 | 733/s | 248 MB |
| 91 | 66 000 | 800/s | 291 MB |
| 107 | 78 000 | 750/s | 212 MB |
| 122 | 89 000 | 733/s | 223 MB |
| 137 | 103 000 | 933/s | 226 MB |
| 152 | 113 000 | 667/s | 221 MB |
| 167 | 125 000 | 800/s | 317 MB |
| 182 | 136 000 | 733/s | 312 MB |
| 198 | 147 000 | 688/s | 230 MB |
| 213 | 157 000 | 667/s | 204 MB |
| 228 | 167 000 | 667/s | 249 MB |
| 243 | 177 000 | 667/s | 224 MB |

The count moves in steps of 1000, one committed batch at a time, so a single sample's pace is
rough to about one batch. The pace drifts down by about a tenth over the run, against about 40% in
the baseline. The rest of the run is flat, and heap use goes up and down with each batch rather than
growing. That is what a persistence context closed at the end of every batch should look like.

## What keeps it this way

The fix holds only while no transaction spans a whole run. A `@Transactional` on a `*Run` or a
`*Endpoint` would hold one context for the whole run again, with or without open-in-view, and a
`*Store` that returned an entity would now fail with `LazyInitializationException` once the entity
left its transaction. Neither happens today: every store method is transactional and returns
records, ints or sets, and nothing under `workers/` touches JPA.

No test guards this. The store tests run each test in one transaction, which behaves like
open-in-view, and the endpoint and run tests mock what is below them. A re-timing like this one is
the check.
