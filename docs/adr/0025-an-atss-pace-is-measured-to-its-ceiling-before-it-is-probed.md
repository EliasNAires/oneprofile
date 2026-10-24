# An ATS's pace is measured to its ceiling before it is probed

Status: accepted

Probes and sweeps used to ask one board at a time, at most one every 250 ms per ATS. Each ATS's
issue said "its pace measured as Ashby's was", and each measurement file repeated the method its
own way. Measured to its ceiling, no ATS refused at 32 at a time. At half of that, a probe of
every ATS took 4 to 8 minutes against 36 minutes to 2h 17m, and a sweep 6 to 17 minutes against
23 to 57 (`docs/measurements/2026-10-pace.md`). Decided with Elias in #82, 2026-10-22 and
2026-10-23. Adding an ATS (Workable #80, Workday #81 and any after them) follows this ADR.

- **The research comes before an ATS is probed at all**, and is done for the probe's request
  and the sweep's apart:
  - **What its sources say**: documented rate limits, terms of use for the board API,
    `X-RateLimit-*` and `Retry-After` headers, any per-IP or per-board limit, and what sits in
    front of it (a CDN, a WAF). Each fact is marked as coming from a source or from a test.
  - **What a refusal looks like**: its status (`429`, `403`, `503`, a challenge page), whether it
    sends `Retry-After`, and how long it lasts.
  - **A ramp to the ceiling**: bursts at 1, 4, 8, 16 and 32 requests at a time, 30 s apart, on
    slugs drawn from the current snapshot that no earlier burst asked, with the app's user agent.
    Each records the rate, the median, 95th percentile and slowest answer, and every status. The
    ramp stops at the first refusal, when the median answer doubles, or when the rate stops
    growing.
  - **The checks after it**: whether each region host has a limit of its own, whether the probe
    and the sweep share one, and whether a rate holds over a sustained burst of several thousand.
  - **What caps it first**: the machine's link and `VacancyStore.mirror`, measured against the
    ATS.

  `scripts/draw-pace-slugs`, `scripts/pace-ramp` and `scripts/pace-burst` do this. The checks are
  bursts run side by side.
- **The settings are half of the ceiling.** A run starts an ATS's requests no faster than half
  the highest rate served without a refusal. It keeps no more than half the number of requests
  open at once at which that rate was served. A departure is written down with its reason, such
  as a sweep capped by the link rather than by the ATS, or Lever's slow tail. An ATS with one API
  per region gets the settings for each region.
- **A run that is refused anyway slows down, and never speeds back up.**
  - A refusal is a `429` or a `503`, or whatever the research found the ATS answers instead.
  - The reader retries the board, five attempts in all. It waits as long as `Retry-After` says,
    and otherwise 1 s, doubling.
  - The reader tells the run of each refusal. Each one halves both settings for that ATS, or for
    that region on an ATS with one API per region, down to one at a time and one a second, for
    the rest of the run. The next run starts from the settings again.
  - A board still refused after its retries counts as unreadable, and its company keeps what it
    had.
  - A gateway error (Lever's `500`, `502` and `504`) is retried without slowing the run, since
    nothing has tied it to load.
- **Workday departs from the above** (amended 2026-10-24, #81; its reasons are in
  `docs/measurements/2026-10-workday.md` § The setting).
  - Its settings are 16 requests a second and 20 open, not half its ceiling, which was found only
    between 15 and 31 a second; 16 held over 34 minutes.
  - They pace every request rather than every board, since a sweep asks a request a page and one a
    description, and one throttle, in `WorkdayApiAdapter`, holds them for its probes and sweeps
    together, since Cloudflare limits this machine across all of them. Its runs' own paces only
    bound how many boards are read at once.
  - A refusal is Cloudflare's `429` marked `cf-mitigated: challenge`. It halves the shared throttle
    as well as the run, and is waited out from 1 minute, doubling, since a challenge cleared in one
    to five. The throttle takes its full pace back once Workday has been left alone for 10
    minutes, which is where the runs that share it end. A tenant's own `429`, and `502`, `503`,
    `504` and `520` from one tenant's server, are retried without slowing anything.
- **The settings are checked at full scale before they stand.** A probe and a sweep of every
  company of the ATS run on the dev machine with the new settings. Each run's wall clock is
  recorded against the run before, along with every refusal. If the ATS refuses during that
  check, its settings are lowered in the code to the level the run slowed to, the reason is
  written down, and it runs again. A refusal in a later, ordinary run is only logged; a pattern
  of them is a reason to measure again.
- **Where it is written.** The research, the settings and the full-scale check go in one
  measurement file in `docs/measurements/` per ATS or group of ATSs measured together, as
  `2026-10-pace.md` holds Greenhouse's, Ashby's and Lever's. The settings live in
  `BoardProbeRun.PACES` and `BoardSweepRun.PACES`, each citing that file.

## Considered options

- **One board at a time with a pause between requests**, as runs did before: a run's speed is
  then capped by the pause, or by the ATS's latency when that is slower. Lever's slow tail held
  its probe to a third of a board a second.
- **A run that speeds up again after a quiet spell**, probing for the ceiling as it goes. The
  ceiling is measured beforehand instead, so a run never has to press against it.
- **Settings at the highest rate that drew no refusal**: no ramp reached any ATS's ceiling, so
  that rate is only a floor under it, and a margin keeps a run clear of whatever limit lies
  above.
