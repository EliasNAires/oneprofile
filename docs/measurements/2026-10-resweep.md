# The corpus re-swept with structured descriptions

Run on the development machine on 2026-10-06, in dev mode, for #46 step 3. It starts from the raw
corpus snapshot `raw-2026-09-20.dump` (6890 companies, 179 098 vacancies), restored with
`scripts/restore-snapshot.sh`. Flyway then added V4 to V8 on startup. The run sweeps the 5136
boards that snapshot holds as active, with no probe first, so boards opened since 2026-09-20 are
not in it. Then it cleans and classifies by title.

The reader keeps one marked line per HTML block (`# ` heading, `- ` item, `> ` boilerplate) and
stores each vacancy's `language`. Cleaning cuts every description into segments and stores them
in `normalized_vacancy.description_segments`. The body pass was not run: it still reads flat
text, and step 4 moves it onto segments.

Sweeping was paced at 500 ms a board, the pace the code carries. Both snapshots were written with
`pg_dump -Fc`. They supersede the 2026-09-20 pair as what dev restores (ADR-0006, amended
today):

- `~/oneprofile-snapshots/raw-2026-10-06.dump`, 146 MB: the corpus as the sweep left it, with
  the data of `normalized_vacancy` and `normalized_vacancy_title_seniority` left out. Those
  tables were empty when the sweep finished.
- `~/oneprofile-snapshots/classified-2026-10-06.dump`, 303 MB: after cleaning and title
  classification. `scripts/restore-snapshot.sh` puts it back in 1m 04s.

`raw-2026-09-20.dump` and its pile export `pile-raw-2026-09-20.jsonl` moved to
`~/oneprofile-snapshots/archive/`, next to a dump of the dev database as it stood before the run
(`dev-2026-10-06-before-resweep.dump`, round 3's state).

| Phase | Wall clock | What it reported |
| --- | --- | --- |
| Restore `raw-2026-09-20.dump` | 1m 03s | 6890 companies, 179 098 vacancies |
| One sweep over the active boards | 2h 33m 14s | 5136 boards, 177 075 published: 36 998 added, 140 077 updated, 38 185 deleted, 43 unreadable |
| `pg_dump -Fc`, raw | 55s | 146 MB |
| `POST /cleanings` | 37m 39s | 177 911 vacancies, 114 483 distinct titles, 104 082 once cleaned |
| `POST /classifications` | 20m 48s | 26 652 `IN`, 125 366 `OUT`, 25 893 `UNKNOWN` |
| `pg_dump -Fc`, classified | 47s | 303 MB |
| **The run** | **3h 34m 26s** | |

## Why the runs were slow

The sweep took 2h 33m against 59m on 2026-09-20 at the same pace. It started at about 48 boards
a minute and fell to about 22. Cleaning started at about 125 vacancies a second and fell to about
75. The cause is #50: `spring.jpa.open-in-view` is on by default, so one persistence context
spans each `POST`. Every entity a run touches stays managed, and every auto-flush dirty-checks
all of them, so each board or batch costs more than the one before. The thread dumps sat in
Hibernate's `DirtyHelper.findDirty`. Part of cleaning's time is new work: it now cleans and splits
every description, where on 2026-09-20 it cleaned titles alone in 54s. The figures above are what
the code as committed in `7f8c46a` does. With #50 in, cleaning takes about 4m 15s instead:
`2026-10-cleaning-pace.md`.

## Boards that answered 404

All 43 unreadable boards answered 404: the board is gone. The sweep treats a 404 like any other
failed read, so it keeps the board's vacancies. Those 836 vacancies (0.47%) are still in both
snapshots with the 2026-09-20 flat description and no `language`. They are the only rows the
new reader never read. #49 makes a 404 close the board instead. The snapshots keep what the
pipeline produced.

## What the new reader and splitter produced

**Language.** Every re-read vacancy has one. The 836 rows without one are those of the 404
boards.

| `language` | Vacancies | Share |
| --- | ---: | ---: |
| `en` | 169 079 | 95.04% |
| `fr` | 2373 | 1.33% |
| `pt` | 1993 | 1.12% |
| `de` | 1403 | 0.79% |
| none | 836 | 0.47% |
| `es` | 611 | 0.34% |
| 13 others | 1616 | 0.91% |

**Lines.** 176 775 of 177 911 descriptions (99.36%) have more than one line. With `.text()` every
one of them was a single line.

**Segments.** 9 319 163 segments, 52.4 a vacancy on average. 98.79% of vacancies have at least
one heading and 97.70% at least one list item. 63.65% carry Greenhouse boilerplate, which matches
the 63% step 1 measured on a sample.

| Kind | Segments | Share | Marked boilerplate | Sitting under a heading |
| --- | ---: | ---: | ---: | ---: |
| `ITEM` | 4 395 270 | 47.16% | 271 748 (6.18%) | 4 351 732 (99.0%) |
| `SENTENCE` | 3 659 668 | 39.27% | 1 469 987 (40.17%) | 2 720 108 (74.3%) |
| `HEADING` | 1 264 225 | 13.57% | 197 671 (15.64%) | — |

Boilerplate is 20.8% of all segments, mostly company prose. A few pile rows read by eye split as
expected: the company's introduction marked boilerplate sentence by sentence, then `Who we are`
or `About the role` as headings, and each item under the heading it sits under.

## The pile

The title stage is frozen, so the pile moves only because the population did.

| | 2026-09-20 | 2026-10-06 |
| --- | ---: | ---: |
| Vacancies | 179 098 | 177 911 |
| Title `IN` | 26 587 | 26 652 |
| Title `OUT` | 126 399 | 125 366 |
| `unruled` | 4656 | 4647 |
| `domain_ambiguity` | 11 268 | 11 272 |
| `scope_ambiguity` | 10 188 | 9974 |
| **Pile** | **21 456** | **21 246** |

The 2026-09-20 column is read off the archived dev dump. The body pass of round 3 had overwritten
the pile's states there, but not its reasons.

Of round 3's 21 456 pile rows, 16 527 are still open and 16 389 are still in the pile, so 4857 of
the new pile's rows are new to it. Of the 3741 vacancies Jev has labelled, 2883 are still open and
2854 are in the new pile. Their labels were made from the flat text and stay valid as statements
about that text (ADR-0012).

By language, the pile holds 20 293 `en`, 85 `es`, 141 with no language, and 727 in another
language. Those 727 (3.4%) are out of scope and skip the body pass as `unsupported_language`. Step
1 estimated 3.3%.

The pile export (`POST /pile-exports`) was not run against this snapshot. It still reads the
flat cleaned text, and step 4 moves it onto segments, as it does the body pass.
