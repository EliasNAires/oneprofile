# The public apps read a copy the pipeline pushes

Status: accepted

The Finder and the Explorer are public; the pipeline runs on Elias's own server, which should
serve nobody and carry no public load. Decided by Elias on 2026-10-20.

- **Two Spring Boot apps, one repository.** The build becomes two Maven modules: `pipeline`, what
  the backend is today, and `api`, the read app. The pipeline stays on Elias's server; the read
  app runs on a 4 GB Hetzner VPS in Ashburn, beside the public Postgres.
- **The pipeline pushes.** After each cycle, a publish stage rebuilds a **read schema** in the
  public Postgres, in staging tables swapped in one transaction, so a visitor never sees a half
  published day. Elias's server only makes outbound connections.
- **The read app serves the site.** Its endpoints are the contract with the site: the Finder's
  query, with filter rank, its orders and the counts of what a filter cannot see, one vacancy, the
  Explorer's aggregates, and recording an apply click, its only write. The read schema sits between
  the two apps only, so either side of it can change without the site noticing. Sessions, when they
  come, are Spring Security in the read app.
- **The site is static where it can be.** An Astro site with React components, in its own
  repository, on Cloudflare Pages. Each publish rebuilds the pages worth ranking for: the home page,
  a landing page per role and per skill, the methodology, the Explorer and the blog. A vacancy page
  is rendered on request by a Pages Function and cached. The read app sits behind Cloudflare too,
  and its answers are cached until the next publish.

## Considered options

- **PostgREST's generated API over the read schema**, the logic in Postgres functions. Less code,
  but filter rank, the orders and eligibility per country are the Finder's core, and as SQL
  functions they are harder to test and change than Java under Testcontainers. Any client could
  compose an expensive query, and free-form query strings split Cloudflare's cache where fixed
  endpoints do not.
- **A self-hosted Supabase**: about ten containers to upgrade, secure and back up, for auth,
  storage and realtime nothing uses yet. **Hosted Supabase**: the free plan's 500 MB is filled at
  the 100k launch gate (the engineering descriptions take 119 MB on disk at 34k vacancies), and Pro
  costs five times the VPS.
- **A static bundle only** (JSON, or a SQLite file read over HTTP ranges, on R2). No server at all,
  but no real queries, a heavy first load at 100k vacancies, or unproven libraries.
- **Firebase**: not SQL. Every filter combination with an order needs its own index, some
  combinations are not allowed, and reads are billed per document: the worst fit for a filtered
  search with counts.
- **Serving from Elias's server through a Cloudflare tunnel**: free, but it puts the public load
  and surface back on his machine.
- **A Hetzner region in Germany**: cheaper, but measured from Argentina its round trip is 245 ms
  against Ashburn's 145 ms, and US East is about 90 ms against 15 ms.

## Consequences

- nginx in front of the app (#18) and the tunnel (#32) are not built.
- Descriptions are shown as the sweep stores them, as marked lines rendered into paragraphs,
  headings and lists, with the boilerplate set apart. The original HTML is not kept.
