# Discovery reads the Wayback Machine beside Common Crawl

Status: accepted

One archive misses boards the other holds. In September 2026, before the reset, Wayback's CDX
index named 17,730 Greenhouse slugs, 8,035 of them in none of 16 Common Crawl crawls, and 23% of
a sample of those probed live; and Common Crawl has not captured Lever's board pages since
CC-MAIN-2025-47. Decided with Elias on 2026-10-21 (#74).

- **Two archives, both read for every ATS**: Common Crawl as ADR-0001 reads it, and the Wayback
  Machine through its CDX index.
- **Common Crawl's cutoff is the 12 newest crawls that hold the ATS's board pages**, not the 12
  newest crawls. For an ATS every crawl captures, that is PRD §4.2's twelve; for Lever, it is
  CC-MAIN-2025-43 and older.
- **Wayback is read whole**, every capture back to its first. A dead slug costs one probe and is
  kept as not found; an old slug can still be a live board.
- **Each ATS's Common Crawl is backfilled once**, in the step that adds it. Afterwards a run
  reads only a new crawl.
- **Wayback is read whole on every run**, and a run keeps only the slugs not already held. Its
  CDX server cannot read only the captures since the last run: `from=` filters after the server
  has scanned the whole prefix, so it saves no time, and without paging it times out. A whole
  read is about 25 minutes for Greenhouse and 3 for Ashby. Scheduling runs is PRD §12.5's, not
  this ADR's.
- **How Wayback is read was decided by research before the first run**
  (`docs/measurements/2026-10-wayback.md`): one unpaged query per URL prefix, continued by resume
  key in chunks of 50,000 lines, one request at a time, as JSON so a cut answer is caught, and a
  `503` page taken as a refusal to wait out and ask again.

Amends ADR-0001 by adding a second archive; ADR-0001's method for Common Crawl is unchanged.

## Considered options

- **Wayback limited to the last year**, matching Common Crawl's depth: fewer dead slugs, but
  fewer boards, and probing the rest is cheap.
- **Common Crawl alone, further back**: it misses what Wayback alone holds, and every crawl
  after the block misses Lever.
- **Reading every crawl on every run**: the backfill's result is already held, so rereading it
  finds nothing new.
- **Reading Wayback's captures since the last run with `from=`**, as this ADR first said (amended
  2026-10-21, #78): measured, it reads no less of the server than a whole read, and the unpaged
  form fails.
