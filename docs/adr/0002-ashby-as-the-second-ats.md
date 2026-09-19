# Ashby is the second ATS, not Workable

Status: accepted

After Greenhouse, the second ATS integration is Ashby. It was chosen for the highest
measured live-board rate (85% of sampled slugs, against 55% for Workable), a structured
`workplaceType`, per-location countries on `secondaryLocations[]`, compensation data, and no
pagination. The goal of this iteration is a working vertical slice, and Ashby has the fewest
moving parts.

## Considered options

- **Workable**, which measured roughly five times the volume (about 3,156 slugs and an
  estimated 62,000 vacancies per crawl, against Ashby's 926 and 12,700), a **100%-filled
  ISO-2 country code**, a 100%-filled remote flag, and an employer population of SMBs across
  100+ countries that barely overlaps Greenhouse's. It is the better choice on reach and on
  eligibility data, and it is the obvious thing to reconsider when volume becomes the
  constraint. It was rejected only because it has no salary field at all and adds more
  surface than a thin slice needs.
- **Lever**, whose public API is the best of any candidate. Rejected on a hard constraint:
  Common Crawl holds **zero page captures** for `jobs.lever.co` across CC-MAIN-2026-34, -30
  and -25 — only 62 `robots.txt` records. Its slugs cannot be discovered by our method at
  all, and reaching it would require a second discovery channel.
- **Workday, SmartRecruiters, BambooHR, Rippling, Recruitee, JazzHR, Teamtailor, Personio**,
  rejected variously for having no public board API, no publicly discoverable slug, two
  calls per job, or too small a discoverable population.

## Consequences

Ashby draws on the same US and EU venture-backed population as Greenhouse, so this deepens
the corpus rather than widening it. If the eligible pool proves too small, adding Workable is
the first move, not adding a third source of the same kind.
