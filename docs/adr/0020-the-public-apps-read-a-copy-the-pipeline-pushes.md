# The public apps read a copy the pipeline pushes

Status: accepted

The Finder and the Explorer are public; the pipeline runs on Elias's own server, which should
serve nobody and carry no public load. Decided by Elias on 2026-10-20.

- **The pipeline pushes.** After each cycle, a publish stage rebuilds a **read schema** in the
  public database, in staging tables swapped in one transaction, so a visitor never sees a half
  published day. Elias's server only makes outbound connections.
- **The public database is Postgres with PostgREST** in front of it, two containers on a 4 GB
  Hetzner VPS in Ashburn. Supabase, or its auth alone, is added when sessions arrive: its API is
  PostgREST, so the site's calls do not change.
- **There is no read API of our own.** The site calls PostgREST's generated API. Filter rank, the
  Finder's orders and the counts of what a filter cannot see are Postgres functions in the read
  schema, called as RPC. The read schema and its functions are the whole contract between the two
  apps, so the pipeline's own tables can change without breaking the site. The anonymous role reads the
  read schema and inserts apply clicks, nothing else.
- **The site is static where it can be.** An Astro site with React components, in its own
  repository, on Cloudflare Pages. Each publish rebuilds the pages worth ranking for: the home page,
  a landing page per role and per skill, the methodology, the Explorer and the blog. A vacancy page
  is rendered on request by a Pages Function and cached. The API sits behind Cloudflare too, and its
  answers are cached until the next publish.

Amended the same day: the public database was a self-hosted Supabase. It is about ten
containers to upgrade, secure and back up, for auth, storage and realtime nothing uses yet, and
the database only serves what the pipeline hands it. Hosted Supabase Pro ($25 a month) would
remove that work but costs five times the VPS.

## Considered options

- **A static bundle only** (JSON, or a SQLite file read over HTTP ranges, on R2). No server at all,
  but no real queries, a heavy first load at 100k vacancies, or unproven libraries.
- **Postgres behind a Spring read API on a VPS**: the PRD's old §8. More to write and run than
  PostgREST's generated API, for no gain while the site only reads.
- **Firebase**: not SQL. Every filter combination with an order needs its own index, some
  combinations are not allowed, and reads are billed per document: the worst fit for a filtered
  search with counts.
- **A self-hosted Supabase from the start**: see the amendment above.
- **Supabase's hosted free plan**: 500 MB of database. The engineering descriptions alone take
  119 MB on disk at 34k vacancies, about 450 MB with everything else at the 100k launch gate.
- **Serving from Elias's server through a Cloudflare tunnel**: free, but it puts the public load
  and surface back on his machine.
- **A Hetzner region in Germany**: cheaper, but measured from Argentina its round trip is 245 ms
  against Ashburn's 145 ms, and US East is about 90 ms against 15 ms.

## Consequences

- nginx in front of the app (#18) and the tunnel (#32) are not built. PRD §8's endpoints are
  replaced by read-schema functions.
- Descriptions are shown as the sweep stores them, as marked lines rendered into paragraphs,
  headings and lists, with the boilerplate set apart. The original HTML is not kept.
