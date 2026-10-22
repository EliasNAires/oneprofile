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
  and `jobs.eu.lever.co`. Lever's has its own API host. Greenhouse's EU boards are served by its
  one API host, `boards-api.greenhouse.io`, and their slugs share one namespace with the rest
  (amended 2026-10-21, #78: this ADR first gave Greenhouse's EU host an API host of its own, and
  `boards-api.eu.greenhouse.io` does not resolve).
- **A company keeps its region** (amended 2026-10-22, #79, decided by grilling Elias): `US` or
  `EU`, the region of the ATS host that publishes its board, read by discovery from the URL. Lever
  needs it: `api.lever.co` answers 404 for an EU board and `api.eu.lever.co` 404 for a US one, so
  a probe through the wrong host would record a live board as not found. Every company has one,
  never null: Ashby's and Greenhouse's US boards are `US`, and Greenhouse's EU boards `EU` although
  its one API host does not need it. V13 sets `EU` on the Greenhouse companies whose vacancy URLs
  are on `job-boards.eu.greenhouse.io` and `US` on the rest; a Greenhouse EU board never swept stays
  `US`, which no probe or sweep reads. A company is still its ATS and slug: a slug discovered under
  both regions (only Lever's own `lever` and `leverdemo` were found so) keeps the region first
  recorded; captured under both in one run, it is recorded in the EU, since only an EU board is
  published under an EU host. A 404 from its region's API means not found, with no try on the
  other.
  Measurements: `docs/measurements/2026-10-lever.md` § Lever's API.

Supersedes ADR-0002's consequences (Workable next) and its reason for rejecting Lever.

## Considered options

For the region (#79):

- **The slug holding its host** (`eu/kwalee`): a slug would stop being the company's identifier
  within its ATS.
- **Lever's EU host as an ATS of its own**: one ATS's figures would be split in two.
- **Probing both API hosts on every run**: Lever answers in about 3 s a request, so every run
  over Lever would take nearly twice as long, for nothing a column cannot remember.
- **Only Lever's companies holding a region**: a null would mean "this ATS has no regions", the
  two-meaning null ADR-0021 took out of the vacancy.

For the ATS:

- **Board count**, as freehire's catalog ranks ATSs: payroll ATSs (Paylocity, BambooHR, Paycom)
  lead it, and their vacancies are mostly hourly work.
- **SmartRecruiters**: a public API and 4,064 boards, but mostly retail and delivery, much of it
  outside English.
- **Workable before Lever**, as ADR-0002 left it: Workable measured slightly more per board, but
  Lever's slugs are already in hand and its population is the one the corpus is built on.
