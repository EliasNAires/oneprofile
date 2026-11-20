# Workday has a long and a short sweep

Status: accepted

A Workday sweep took 8h 38m in #90's first run, against PRD §12.5's 4 hours: 252 436 requests at the 8
a second the throttle holds after Cloudflare's challenge, 62% of them description pages. 58% of the
descriptions read were the one already held. Reading only those not held (c40422d) took the second run
to 4h 5m, but a description that changes under the same id and title is then never read again: over
26 days that left a wrong fact on 1.8% of open `IN` vacancies (eligibility, work mode, required years,
skills, seniority; `docs/measurements/2026-11-description-changes.md`). Decided by Elias on
2026-11-19.

## The decision

Workday is swept in two ways, from two endpoints:

- A **long sweep** reads every description the title rules call for, held or not. It is what a sweep
  was before #90.
- A **short sweep** reads a description only for a vacancy not held with one under the same id and
  title, and keeps the held one otherwise (c40422d).

Both read every list page, so both add the vacancies a board has published since and remove the ones it
has closed. A description is as fresh as the last long sweep; the list of vacancies, as fresh as the last
sweep of either kind.

The schedule is #30's: a long sweep each night, short sweeps through the day, around US working hours.
A long and a short sweep never run at once, since every Workday request shares one throttle.

Greenhouse, Ashby and Lever give the descriptions with the list, so they have one sweep, as before.

## Making both fit

Every speed-up that changes nothing a visitor sees applies to both sweeps (#90's plan): asking again
after a network error, narrowing a large board by a facet that names every vacancy (#110), reading one
board's requests several at a time (#88), and a pace that holds a whole run. Projected from #90's runs
at 8 a second, with #110: a long sweep about 7h 25m, a short one about 2h 15m.

## Consequences

- A changed description, and the facts derived from it, can be up to a day old.
- PRD §12.5's goal of all sweeps under about 4 hours becomes: a long sweep fits a US night, and a short
  sweep is as short as #110 and #88 make it.
