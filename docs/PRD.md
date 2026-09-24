# OneProfile — Product Requirements, MVP iteration

Status: agreed 2026-09-19. The spec for what is not built yet. What is built is summarized
with pointers to the code, the ADRs and `docs/engineering-role-criterion.md`, which own it.
Sequencing lives in the issue tracker.

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

Roles are limited to **engineering roles** this iteration, as
`docs/engineering-role-criterion.md` defines them. The wider tech-adjacent set is the
product's eventual purpose and is deliberately deferred.

## 3. Definition of done

**The top 20 jobs the SPA shows are jobs the developer would actually apply to**, judged by
hand against one real search. Not a component-completeness bar, not a corpus-size bar.

## 4. Feature 1 — a meaningful corpus of jobs

### 4.1 Sources

Two applicant tracking systems: **Greenhouse**, built, and **Ashby**, not yet built. Why
Ashby and not Workable or Lever: ADR-0002.

### 4.2 Slug discovery — built

Slugs come from Common Crawl's cluster index over anonymous HTTPS range requests (ADR-0001).
Backfill depth is **12 crawls**, roughly one year: the newest crawl yielded 77% live boards
at 31.9 vacancies each, older ones 18–36% live at 3.8 each.

### 4.3 Board probing and vacancy ingestion

Built for Greenhouse. Companies are probed and recorded as `ACTIVE`, `EMPTY` or `NOT_FOUND`;
only `ACTIVE` companies are swept. `vacancy` is a **mirror of the board**: upsert what is
returned, delete what has disappeared.

Ashby, to build: `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true`.
Unauthenticated, whole board in one response, no pagination. Its structured fields
(`workplaceType`, `secondaryLocations[]` with per-location countries, compensation) are kept
— see §7.

All ingestion is triggered manually by an administrative endpoint. Scheduling comes after the
real runtimes are measured.

## 5. Feature 2 — classification and eligibility

This feature runs in three passes, in this order: **cleaning** over the whole corpus,
**classification** over the whole corpus, and **normalization** over the engineering subset
alone. The order is deliberate. Cleaning is what makes the corpus readable enough to build a
classifier by eye; classification is what earns the right to stop reasoning about personal
trainers and psychiatric nurse practitioners; and normalization is where rules may assume
the vocabulary of engineering, because by then nothing else is left.

### 5.1 Title cleaning — built

A character whitelist, gender-marker removal, and extraction of the unguarded seniority words
(`senior`, `sr`, `junior`, `jr`, `principal`, and `semi senior`/`ssr` as mid). The levels a
title names are kept as a **list**, since the title is one source of seniority among
several. Work-mode words are **not** removed here; they belong to §5.3. The rules and their
measured yield: `docs/measurements/2026-09-title-cleaning.md`.

### 5.2 Engineering roles — built, loop open

A rule over cleaned titles answers whether a vacancy is an engineering role, in three states
(ADR-0008, ADR-0009, ADR-0010). The criterion is `docs/engineering-role-criterion.md`; the
rules are code, and nobody signs off on them. Measurement and the loop's exit: ADR-0008.
Role families finer than "engineering role" are **not modelled** — nothing consumes them.

Still to build (#11): a second pass reads the description body of the **unknown** vacancies,
by rule, and resolves what it can. The reason code says which question to ask: a
domain-ambiguous title needs the body checked for domain markers, a scope-ambiguous one needs
the criterion re-applied. An `unruled` title does not reach this pass.

### 5.3 Title normalization

Over the engineering subset only. Everything in this pass may assume the vocabulary of
engineering work, which is what makes its rules small.

**Seniority.** The guarded words are read here: `staff`, `mid`, `entry`, and the numbered
levels `II`, `III`, `IV`. Restricting to the subset is what makes them tractable — probed
over an engineering-shaped slice of 37,309 vacancies, `staff` appears in 9.4% of titles and
almost always means the level, so the pre-reset guard against `staff nurse`, `staff
accountant` and `staff attorney` is **dropped**: those 163 titles are not in the subset. What
survives is the one guard that still fires inside it, `chief of staff`. `mid` and `entry` are
down to 0.4% and 0.2%, and `semi senior` to 5 vacancies in the subset and 16 in the whole
corpus, which is why §5.1 collapses it into mid rather than giving it a level of its own.

The numbered levels are extracted **as they are**, with no claim about where they sit on the
ladder: the titles do not say whether `Engineer II` is junior or mid, and 4.1% of the subset
carries one. Resolving them is left to §6.1, where the body's years-of-experience phrasing is
evidence rather than assumption.

Words that name a function rather than a level — `lead`, `director`, `head`, `VP`, `chief` —
stay in the title. A lead is doing a different job, not the same job one rung up, and
collapsing the two would recommend a user a role they did not apply for.

**Work mode**, one of remote, hybrid or onsite. The location field decides and the title
answers only when the location names nothing: of 16,444 remote vacancies in the pre-reset
corpus, 14,624 stated it only in the location. Null means "not declared" and never "onsite".

There is no `FULLY_REMOTE` value; work mode and declared location are separate (ADR-0004).

**Declared location**: the location field as the company wrote it, with the work-mode words
taken out, stored verbatim. Normalizing those 26,290 distinct strings into places is real
work with its own rules and its own accuracy, and it belongs to the step that derives the
country set, not here. A named location is **never on its own a reason to rule a vacancy
out** (ADR-0004).

What is left of the title once all of this is out is the **normalized title**: the role and
nothing else.

### 5.4 Eligibility

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

Extraction is rule-based only (ADR-0005). Eligibility is hard-filtered, which makes recall
the binding constraint on the product (ADR-0004); recall is measured by hand-labelling a
random sample of roughly 200 remote-flagged vacancies, and the missed phrasings are the work
queue.

### 5.5 Skills

Skills use a **curated engineering taxonomy of roughly 1,500–3,000 concepts**, stored as
`id, canonical_name, category, aliases[]` and matched against description text by a
normalized trie pass. The same taxonomy is the picker the user chooses their own skills
from, which is what makes coverage a meaningful number.

The taxonomy is seeded from **O\*NET Technology Skills**, **GitHub Linguist** and
**Wikidata**; why not Lightcast, ESCO or Stack Overflow: ADR-0003.

This iteration does **not** distinguish required skills from merely mentioned ones.
Coverage is a rough signal and is accepted as such.

## 6. Feature 3 — matching

### 6.1 Seniority

One ordinal axis, derived from the evidence the earlier steps gathered rather than extracted
afresh. The title contributes a list of levels (§5.1, §5.3); the description body
contributes level tokens wherever they appear and years-of-experience phrasing, mapped as:
0–1 entry, 2–3 junior, 3–5 mid, 5–8 senior, 8+ staff.

Body extraction matters because the pre-reset measurement showing 96,734 of 128,953
vacancies with no detectable seniority was **title-only**, and says nothing about what the
body contains.

This step is the only one that collapses several levels into one, and it does so by taking
the **lowest**, because a vacancy naming a range publishes the floor it will accept. It is
also where the numbered levels are resolved: `Engineer II` carries no place on the ladder
until the body's years-of-experience phrasing gives it one.

### 6.2 Ranking

Eligibility is a **hard filter** (ADR-0004): only `ELIGIBLE` vacancies are ranked.

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
- `normalized_vacancy` — one row per vacancy holding every derived scalar: cleaned title,
  normalized title, whether it is an engineering role, the signal that classified it, work
  mode, declared location, seniority, eligibility state and the extracted country set. A
  single wide table is correct here **because** every derivation is a cheap pure-function
  rule pass; had an LLM derivation been in scope, the cost asymmetry would have forced
  separate tables. Columns arrive with the step that fills them, not up front.
- `normalized_vacancy_title_seniority` — the levels a title names, one row each, because a
  title may name more than one and the counting these reports need is a join rather than an
  unnest.
- `vacancy_skill` — join table, one row per (vacancy, skill), unique on the pair so
  re-extraction is idempotent, indexed on both columns. At roughly 10–15 skills per vacancy
  this is 2–3 million rows, which is unremarkable for PostgreSQL. It is preferred over an
  array column because the UI needs to show *which* skills matched and which are missing,
  and because a wrong score must be inspectable.
- `profile` — the user's declared skills and seniority level.

Derived data lives in its own tables so rules can be re-run without touching the ATS APIs.

## 8. API

One read endpoint: `GET /profiles/{id}/matches`, returning a ranked page, with eligibility
state and seniority available as query parameters. Ranking is profile-relative
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

## 10. Measurement obligations

Still open:

1. **Eligibility recall**, measured on a hand-labelled sample of ~200 remote engineering
   vacancies. It is the binding constraint on the product's output.
2. **Normalizer regression**, by diffing against the 216,868 pre-reset normalized rows still
   on the production server, kept as a validation set.
3. **Real runtimes** for discovery, probing and sweeping, before anything is scheduled.

## 11. Explicitly out of scope

Authentication and multiple users; role families finer than "engineering"; tech-adjacent
roles a software background alone would not open; résumé parsing; required-versus-mentioned skill discrimination; multi-dimensional seniority;
salary-based ranking; LLM extraction of any kind; scheduled ingestion; TLS, a domain and
rollbackable deploys; ATS beyond Greenhouse and Ashby.
