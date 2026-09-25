# Code Layout

Where a class goes and what it is called. A file's package says whether it holds data or does work; its name says its role, so it can be found without opening it.

## Two groups

Everything under `oneprofile.backend` sits in one of two groups:

- **`storage/`**: the data the workers read and write, one package per aggregate (`company`, `vacancy`, `normalizedvacancy`, `taxonomy`). "Storage" means data others read, not Postgres: the taxonomy is a classpath file.
- **`workers/`**: the work done on that data, one package per step (`discovery`, `probe`, `sweep`, `cleaning`, `classification`). A worker's HTTP endpoint lives in its package.

A worker depends on storage and never the other way round.

## Name by role

Every class that has a role ends in its suffix:

| Suffix | Role |
|---|---|
| `Run` | Walks a corpus or a set of boards and records what it finds. What an endpoint triggers. |
| `Rule` | A pure function over a title, a text or a URL. Knows nothing about Spring or storage. |
| `Parser` | Reads a file format through a port. |
| `Port` | An interface to the outside world, faked in tests. |
| `Adapter` | The real implementation of a port. |
| `Endpoint` | The HTTP trigger of a run. Holds no logic: it calls the run and returns its report. |
| `Store` | A storage package's entry point: reconciliation, dedup, upsert and the transaction boundary. Workers call stores, not repositories. |
| `Repository` | Spring Data. Called only by stores. |
| `Entity` | A JPA entity. Keeps its table and JPQL name with `@Entity(name = "...")`. |
| `Enum` | Every enum. |

Values carried between classes (records such as `CleanedTitle`, `PublishedVacancy`) take no suffix; they keep their name from `CONTEXT.md`.

## Wiring

Classes are `@Component`s; there are no `@Configuration` classes except where one has a job of its own (`TaxonomyConfiguration` locates the taxonomy file). A class that needs values Spring cannot supply (an ATS, a pace, a batch size, a clock, a user agent) has two constructors: a public `@Autowired` one that fixes them from private constants, and a package-private one that takes them all, for tests. The user agent every adapter sends is `workers.UserAgent.VALUE`.
