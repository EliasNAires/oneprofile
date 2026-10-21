# The next ATSs are chosen by engineering vacancies per live board: Lever, Workable, Workday

Status: accepted

ADR-0002 put Workable next and judged Lever undiscoverable. #70 needs about 45,000 more
engineering vacancies live in one sweep, so an ATS is now chosen for how many of those it adds,
not for its board count or its fields. Decided with Elias on 2026-10-21 (#74).

- **The measure is title `IN` vacancies per live board**: vacancies per live board times the
  share of titles `TitleClassificationRule` puts `IN`. Measured on 50 boards drawn at random from
  each candidate's catalog in freehire (strelov1/freehire, `sources/*.yml` at 6eb9bc9): Workable
  5.9, Workday 3.4, SmartRecruiters 0.8, against Greenhouse's 5.3 and Ashby's 4.9 in our corpus.
  Figures and caveats: #74's comment of 2026-10-21.
- **The order is Lever, Workable, Workday.** Lever first although it was not sampled: it serves
  the same venture-backed population as Greenhouse and Ashby, its API reads a board in one call,
  and the archives already name 5,755 of its slugs. Workday comes only if #70's gap is still open
  after Lever and Workable: its boards are large but mostly not engineering, and it needs a call
  per vacancy for the description.
- **Lever is discoverable.** Common Crawl holds its board pages in every crawl up to
  CC-MAIN-2025-43; from CC-MAIN-2025-47 Cloudflare answers CCBot with 403. ADR-0002 read only
  crawls after the block. ADR-0023 says how its slugs are read.
- **Each ATS adds its EU host in its own step** where it has one: `job-boards.eu.greenhouse.io`
  and `jobs.eu.lever.co`, each with its own API host.

Supersedes ADR-0002's consequences (Workable next) and its reason for rejecting Lever.

## Considered options

- **Board count**, as freehire's catalog ranks ATSs: payroll ATSs (Paylocity, BambooHR, Paycom)
  lead it, and their vacancies are mostly hourly work.
- **SmartRecruiters**: a public API and 4,064 boards, but mostly retail and delivery, much of it
  outside English.
- **Workable before Lever**, as ADR-0002 left it: Workable measured slightly more per board, but
  Lever's slugs are already in hand and its population is the one the corpus is built on.
