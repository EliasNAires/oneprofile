# OneProfile — Product Requirements, MVP iteration

Status: agreed 2026-09-19. Supersedes the pre-reset attempt (commit `679c852`), whose
code was discarded but whose measurements are used as the specification for this build.

## 1. The problem

Job hunting for tech-adjacent roles is slow because the three things that matter are all
missing from the tools that exist: there is no single corpus of openings, there is no way
to tell whether a "remote" job will actually pay someone in Argentina, and there is no way
to rank what remains against a specific person's skills.

OneProfile is a backend that solves those three, plus a React SPA in a separate repository
that consumes it.

## 2. Users and scope

This iteration serves **one user: the developer**. There are no accounts, no
authentication, no sessions and no `user` table. The `profile` table has its own id and the
API takes a profile identifier, so adding real users later is a `user` table plus a foreign
key — but nothing is built for that case now.

Roles are limited to **engineering** this iteration: software, data, machine learning,
SRE/DevOps/platform, QA, security, mobile, embedded. The wider tech-adjacent set (product,
design, technical writing, solutions engineering, support) is the product's eventual
purpose and is deliberately deferred — the classifier is built to extend to it without a
rewrite.

## 3. Definition of done

**The top 20 jobs the SPA shows are jobs the developer would actually apply to**, judged by
hand against one real search. Not a component-completeness bar, not a corpus-size bar.

## 4. Feature 1 — a meaningful corpus of jobs

### 4.1 Sources

Two applicant tracking systems: **Greenhouse** and **Ashby**.

Greenhouse is the proven source — the pre-reset system held 216,868 vacancies across 6,883
active boards from Greenhouse alone. Ashby is the second integration. It was chosen over
Workable, which offers roughly five times the volume and a fully-populated ISO-2 country
code, because Ashby has the highest live-board rate measured (85% versus 55%), needs no
pagination, and returns `workplaceType`, `secondaryLocations[]` with per-location
countries, and compensation data. The trade accepted is reach: Ashby draws on the same
US/EU venture-backed population as Greenhouse, so it deepens the corpus more than it widens
it. Fewer moving parts wins under a thin-slice goal.

Lever was evaluated and rejected: its public API is excellent but Common Crawl holds **zero
page captures** for `jobs.lever.co` across three consecutive crawls, so its slugs cannot be
discovered by the method used here.

### 4.2 Slug discovery

Company slugs come from **Common Crawl**, read over anonymous HTTPS from
`https://data.commoncrawl.org/`. Anonymous S3 access to the `commoncrawl` bucket has been
disabled since 2022 and returns 403; the CloudFront mirror serves the same keys, supports
`Range` requests, and needs no AWS account.

The mechanism is the ZipNum cluster index:

1. Binary-search `cc-index/collections/{crawl}/indexes/cluster.idx` with `Range` requests
   to locate the SURT prefix (for example `io,greenhouse,boards)/`). Measured cost: 15
   requests, about 72 KB.
2. Read the `(shard, offset, length)` triples for the matching blocks and `Range`-GET those
   byte ranges from the `cdx-*.gz` shards. Measured cost for all Greenhouse and Ashby
   prefixes combined: about 11 MB per crawl, under a minute.

This replaces the `index.commoncrawl.org` index server used before, which failed on 7 of 10
indexes in one production run and 10 of 10 in another, and which returns truncated bodies
with HTTP 200 and no `Content-Length`, making the failure undetectable at the HTTP layer.

Two correctness rules are mandatory. `cluster.idx` samples every 3000th record, so reading
must start at the block **preceding** the first key greater than or equal to the prefix and
continue one block **past** the last match. The CDX index mixes the `warc`, `robotstxt` and
`crawldiagnostics` subsets, so records must be filtered on `/warc/` in the filename.

SURT canonicalization uses `WaybackURLKeyMaker` from `webarchive-commons`, not a hand-rolled
implementation, because a mismatch silently drops rows rather than failing.

Backfill depth is **12 crawls**, roughly one year, at about 11 MB each. Coverage never
flattens across crawls, but yield does: the newest crawl produced 77% live boards at 31.9
vacancies each, while older and archive-derived slugs fell to 18–36% live at 3.8 vacancies
each, and dead slugs cost probe time.

The columnar Parquet index was evaluated and rejected: the same query moved 190 MB and took
94 seconds because the files ship without usable column statistics, against 11 MB for the
CDX route.

Common Crawl's own guidance names range requests as the traffic class hardest for them to
serve, so the client retries with exponential backoff on 429 and 503 and sends an
identifying User-Agent.

### 4.3 Board probing and vacancy ingestion

Slugs become companies; companies are probed against the ATS board API and recorded as
`ACTIVE`, `EMPTY` or `NOT_FOUND`. Only `ACTIVE` companies are swept for vacancies.

- Greenhouse: `GET https://boards-api.greenhouse.io/v1/boards/{slug}/jobs?content=true&pay_transparency=true`.
  Whole board in one response, no pagination. Description content arrives HTML-escaped
  twice and needs two unescapes.
- Ashby: `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true`.
  Unauthenticated, whole board in one response.

`vacancy` is a **mirror of the board**: upsert what is returned, delete what has
disappeared. A vacancy vanishing from the board is the strongest available signal that the
role closed.

All ingestion is triggered manually by an administrative endpoint this iteration. Scheduling
comes after the real runtimes are measured — the previous system had a `last_probed_at`
column that nothing ever read.

## 5. Feature 2 — classification and eligibility

### 5.1 Title normalization

Carried over from the measured pre-reset implementation: whitelist-based character
cleaning, gender-marker removal, trailing-acronym removal, and work-mode and seniority
extraction. Measured performance was 128,953 vacancies in 24 seconds with a work-mode false
positive rate of 0.19% (37 of 19,255).

Work mode is decided by the location field first and the title only when the location names
nothing — of 16,444 remote vacancies, 14,624 stated it only in the location. Null means
"not declared", never "onsite".

### 5.2 Role families

A **token dictionary over normalized titles** assigns each vacancy zero or one role family
from: `SOFTWARE_ENGINEERING`, `DATA_ENGINEERING`, `MACHINE_LEARNING`, `SRE_DEVOPS_PLATFORM`,
`QA_TEST`, `SECURITY`, `MOBILE`, `EMBEDDED`. "Not an engineering role" is the absence of a
family, so the binary test falls out of the family assignment.

Frontend, backend and fullstack are deliberately **not** families. They are specializations
within software engineering, already expressed by skills, and modelling them as families
would force most roles to carry two.

A second pass reads the description body for role vocabulary the title does not carry, and
classifies from it when the title cannot. Each vacancy records **which signal classified
it**, title or body, so a wrong answer can be attributed to the right rule.

Separately and offline, a mining report lists role vocabulary found in description bodies
that the dictionary does not know, ranked by frequency, as input for growing the dictionary
by hand. The dictionary only ever grows through a decision, never at runtime.

A hand-labelled random sample of roughly 300 vacancies is built **before** the rules are
trusted, so the classifier has a measured error rate rather than an assumed one.

### 5.3 Eligibility

The question the system answers is **"will this company pay someone located in Argentina?"**
— not "is this job remote" and not "am I authorized to work there". Being hired as a
contractor, through an employer of record or otherwise, counts as eligible.

The extractor reads the description body and produces **the set of countries or regions the
posting says it will engage someone in**. The three-state signal is derived by testing
Argentina's membership in that set:

- `ELIGIBLE` — the set is global, names a region containing Argentina, or names Argentina.
- `EXCLUDED` — the set is stated and excludes Argentina. Work-authorization language such as
  "must be authorized to work in the United States" lands here, because it implies they pay
  only US-based people.
- `UNKNOWN` — no set is stated.

An explicit timezone requirement that Argentina cannot meet is a separate disqualifier.

Storing the extracted set rather than only the verdict means the verdict is recomputable if
the user's country changes, and a wrong filter can be diagnosed without re-reading the
description.

Extraction is **rule-based only**. An LLM pass over the residual was considered and
rejected for this iteration: it adds a paid, non-deterministic external dependency, and the
same effort spent on rules is re-runnable for free. An LLM pass remains available as a
later, measured upgrade.

This matters because eligibility is hard-filtered (§6.2), which makes **recall the binding
constraint on the entire product**: a job the rules fail to confirm is invisible, and its
absence looks like a fact about the job market rather than a gap in a regex. Recall is
measured by hand-labelling a random sample of roughly 200 remote-flagged vacancies and
comparing against the rules' output; the phrasings that were missed are the work queue for
improving them.

### 5.4 Skills

Skills use a **curated engineering taxonomy of roughly 1,500–3,000 concepts**, stored as
`id, canonical_name, category, aliases[]` and matched against description text by a
normalized trie pass. The same taxonomy is the picker the user chooses their own skills
from, which is what makes coverage a meaningful number.

Lightcast Open Skills was the original choice and was rejected on three independent grounds:
its Open Terms of Use exclude commercial and for-profit use without a written contract;
there is no bulk download and **aliases are not exposed in the public schema at all**,
which is the single property the matching needs; and access is a sales process with a
documented 5 requests per second ceiling, making 200,000 offline matches infeasible.

ESCO was rejected on domain coverage despite an excellent licence and native alias support:
live queries return **zero results for Kubernetes, Docker and Terraform**.

The taxonomy is seeded from **O\*NET Technology Skills** (CC BY 4.0, free bulk download,
current — Kubernetes appears in 220,241 postings in its 2025 data), **GitHub Linguist**
(MIT, languages with an explicit `aliases` key) and **Wikidata** (CC0, `also known as`
aliases requiring no attribution). Stack Overflow tag synonyms are the best available alias
table but carry CC BY-SA share-alike, which is unresolved for an embedded taxonomy in a
product that may be commercial.

Note that SkillNER and similar GitHub projects bundle Lightcast-derived data under an MIT
code licence; the code licence does not cover the data.

This iteration does **not** distinguish required skills from merely mentioned ones.
Coverage is a rough signal and is accepted as such.

## 6. Feature 3 — matching

### 6.1 Seniority

One ordinal axis. Level is extracted from both the title and the description body, from
level tokens (`senior`, `staff`, `principal`, and so on) and from years-of-experience
phrasing mapped as: 0–1 entry, 2–3 junior, 3–5 mid, 5–8 senior, 8+ staff.

Body extraction matters because the pre-reset measurement showing 96,734 of 128,953
vacancies with no detectable seniority was **title-only**, and says nothing about what the
body contains.

### 6.2 Ranking

Eligibility is a **hard filter**: only `ELIGIBLE` vacancies are ranked. This was chosen
knowing that `UNKNOWN` will be the largest bucket and that low recall therefore shrinks the
product's output directly. The accepted mitigation is to invest heavily in extraction rules
and, if the pool is too small, add sources in a later iteration.

Within the filtered set, score is:

```
score = 100 * (matched_skills / n)      where n = skills the vacancy mentions
      - thin_posting_penalty             when n < 5
      - seniority_shortfall_penalty      per level the profile falls short
      - seniority_unknown_penalty        when the vacancy's level is unknown
```

clamped at 0. Rules:

- `n = 0` scores 0 without dividing, and the vacancy still appears. "The extractor found
  nothing" must not be indistinguishable from "this job wants nothing you have".
- The unknown-seniority penalty is strictly **smaller** than one level of shortfall: an
  unclear posting should cost something without being buried under jobs the user is
  actively unqualified for.
- **Over-qualification is never penalized.** An experienced user filtering for level would
  sort by salary instead, which is out of scope.
- The shortfall penalty is set deliberately high, so that under-qualified matches sink and
  a user with little experience still gets a usable list.
- The thin-posting penalty and the seniority penalties share the 100-point scale and are
  chosen together, not independently.

## 7. Data model

Identity is unchanged from the pre-reset design, which was proven over 216,868 rows: a
company is `(ats, slug)`; a vacancy is `(company_id, external_id)`.

- `company` — ats, slug, name, board_status, last_probed_at.
- `vacancy` — the board mirror: title, location, department, description, url, language,
  pay fields, dates. Ashby's structured fields (`workplaceType`, `addressCountry`) are
  added as **nullable columns**; null means this ATS did not provide it, consistent with
  the existing convention. Discarding Ashby's structured country to match Greenhouse's
  weaker free-text shape would throw away the best available eligibility input.
- `normalized_vacancy` — one row per vacancy holding every derived scalar: normalized
  title, seniority, work mode, role family, classification source, eligibility state and
  the extracted country set. A single wide table is correct here **because** every
  derivation is a cheap pure-function rule pass; had an LLM derivation been in scope, the
  cost asymmetry would have forced separate tables.
- `vacancy_skill` — join table, one row per (vacancy, skill), unique on the pair so
  re-extraction is idempotent, indexed on both columns. At roughly 10–15 skills per vacancy
  this is 2–3 million rows, which is unremarkable for PostgreSQL. It is preferred over an
  array column because the UI needs to show *which* skills matched and which are missing,
  and because a wrong score must be inspectable.
- `profile` — the user's declared skills and seniority level.

Derived data lives in its own tables so rules can be re-run without touching the ATS APIs.

## 8. API

One read endpoint: `GET /profiles/{id}/matches`, returning a ranked page, with role family,
eligibility state and seniority available as query parameters. Ranking is profile-relative
and depends on the skill taxonomy, so it cannot be done in the client.

Ingestion remains a set of `/admin/**` trigger endpoints, which are never proxied and stay
unreachable from outside the Docker network.

## 9. Stack and deployment

Unchanged from the pre-reset setup: **Spring Boot 4.1.1, Java 25**, Maven wrapper, Spring
Data JPA and Spring Web MVC, **PostgreSQL 18**, **Flyway** migrations with
`ddl-auto=validate`, Hibernate JDBC batch size 50, jsoup for HTML-to-text, Testcontainers
for tests, a multi-stage `eclipse-temurin:25` image, GitHub Actions running the test suite
and publishing to GHCR, and manual deployment over SSH to the ZeroTier-reachable server.

New dependencies: `webarchive-commons` for the ZipNum cluster reader and SURT
canonicalization, and commons-compress for decompressing concatenated gzip members when a
byte range spans several.

**nginx** is added in front as a reverse proxy. It serves the SPA's built assets and proxies
`/api/**` to the application over the Docker network; only the proxy publishes a port. Same
origin, so CORS does not enter the design, and `/admin/**` is simply not proxied. The
vertical slice runs as plain HTTP over ZeroTier; a domain and TLS are a later decision.

Two known deployment gaps carried over and not fixed this iteration: images are tagged
`latest` only, so **a deploy cannot be rolled back**, and deployment is manual.

## 10. Build order

A thin vertical slice first, against freshly crawled Greenhouse data rather than the
pre-reset corpus: discover slugs, probe boards, ingest vacancies, normalize, classify,
extract eligibility, extract skills, score, and serve one endpoint to a page that renders a
top 20. Ashby, the twelve-crawl backfill and the wider role dictionary come after that path
runs end to end.

The 216,868 normalized rows still live on the production server are kept as a **validation
set**: the rewritten normalizer's output can be diffed against the old one, and any
disagreement is a bug in one of them.

## 11. Measurement obligations

These are consequences of the decisions above and belong in the plan, not in hindsight.

1. **Eligibility recall**, measured on a hand-labelled sample of ~200 remote-flagged
   vacancies. It is the binding constraint on the product's output.
2. **Role classifier accuracy**, measured on a hand-labelled sample of ~300 vacancies,
   before the rules are trusted.
3. **Normalizer regression**, by diffing against the pre-reset corpus.
4. **Real runtimes** for discovery, probing and sweeping, before anything is scheduled.

## 12. Explicitly out of scope

Authentication and multiple users; tech-adjacent role families beyond engineering; résumé
parsing; required-versus-mentioned skill discrimination; multi-dimensional seniority;
salary-based ranking; LLM extraction of any kind; scheduled ingestion; TLS, a domain and
rollbackable deploys; ATS beyond Greenhouse and Ashby.
