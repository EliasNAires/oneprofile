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
- **Each ATS is backfilled once**, in the step that adds it (or, for Greenhouse and Ashby, the
  step that adds Wayback). Afterwards a run reads only a new crawl, and Wayback's captures since
  the last run. Scheduling those runs is PRD §12.5's, not this ADR's.
- **How Wayback is read is decided by research before the first run**, so the read takes no
  longer than it must: its paging, its limits and what it lets a query leave out.

Amends ADR-0001 by adding a second archive; ADR-0001's method for Common Crawl is unchanged.

## Considered options

- **Wayback limited to the last year**, matching Common Crawl's depth: fewer dead slugs, but
  fewer boards, and probing the rest is cheap.
- **Common Crawl alone, further back**: it misses what Wayback alone holds, and every crawl
  after the block misses Lever.
- **Reading every crawl on every run**: the backfill's result is already held, so rereading it
  finds nothing new.
