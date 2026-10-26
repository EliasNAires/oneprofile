# The public side starts on Elias's server

Status: accepted; supersedes ADR-0020's hosting of the public side, as marked there

ADR-0020 put the public Postgres and the read app on a Hetzner VPS in Ashburn, so that Elias's
server would serve nobody. Nothing public exists yet, and a VPS paid for before the Finder works
buys availability nobody needs. Decided by Elias on 2026-10-26.

- **Both apps run on Elias's server first.** The pipeline as today; the read app and a public
  Postgres of its own beside it, each in its own container.
- **The split stays.** The publish stage still pushes the read schema into a separate database,
  and the read app reads only that one. The pipeline's database and the public one never share a
  container, so moving the public side is a change of host, not of design.
- **The read app is reached through a Cloudflare tunnel**, so the server opens no port and only
  makes outbound connections. Cloudflare caches its answers until the next publish, as before.
- **The public side moves to the VPS when it needs availability**: when the server's downtime or
  load starts to cost visitors, which may not be before there is traffic. The read schema is
  rebuilt every day, so the move carries only the apply clicks over.

## Considered options

- **The VPS from the start** (ADR-0020): pays for availability before there is anything to serve.
- **One Postgres for both**, the read schema as a schema in the pipeline's database: no second
  container, but the move would then mean splitting the database, and the read app's role would
  sit in the database that holds the admin data.

## Consequences

- The tunnel (#32), closed as not built under ADR-0020, is built after all, for the read app only.
- The public load runs on Elias's server until the move, beside the pipeline's sweeps.
