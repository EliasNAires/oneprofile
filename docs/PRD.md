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

This feature runs in three passes, in this order: **cleaning** over the whole corpus,
**classification** over the whole corpus, and **normalization** over the engineering subset
alone. The order is deliberate. Cleaning is what makes the corpus readable enough to build a
classifier by eye; classification is what earns the right to stop reasoning about personal
trainers and psychiatric nurse practitioners; and normalization is where rules may assume
the vocabulary of engineering, because by then nothing else is left.

### 5.1 Title cleaning

The purpose is to collapse titles that say the same thing, without losing what is collapsed.
The raw corpus holds 179,098 vacancies under 115,286 distinct titles, and the classifier is
built by reading the most common ones over and over, so every duplicate removed is a title
that does not have to be read twice.

Three rules, all over the whole corpus:

- **A character whitelist**, spelled as what survives rather than what is dropped, because
  listing separators always misses one. Unicode-aware, so a title in Korean does not come
  out empty. `.NET`, `C#`, `C++` and `R&D` keep the character that carries their meaning.
- **Gender markers** removed whole — `(m/w/d)`, `(H/F)` — and never letter by letter, since
  a lone `f` can be part of anything.
- **Unguarded seniority words** extracted and taken out: `senior`, `sr`, `junior`, `jr`,
  `principal`, and `semi senior`/`ssr` read as mid. These mean a level wherever they appear,
  so removing them needs no reading of the job. The words that need one — `staff`, `mid`,
  `entry` and the numbered levels — are left in the title for §5.3.

What a vacancy's title names is kept as a **list of levels** rather than a single one, since
the title is one source of seniority among several and the step that finally derives a level
has not run yet. `Senior Engineer II` names two.

The **trailing-acronym rule of the pre-reset implementation is deliberately not carried
over**. It removed a closing acronym when its letters were the initials of the preceding
words, and it was justified by 1,393 vacancies spelled both `Registered Behavior Technician`
and `Registered Behavior Technician (RBT)`. Probed against this corpus it fires on 2,138
vacancies but collapses only 112 distinct titles — 0.1% — and the jobs it helps are not in
scope.

A rule earns its place in cleaning by collapsing at least 0.5% of distinct titles **or** by
removing a token that would otherwise pollute classification. Gender-marker removal is kept
on the second ground alone: it collapses 0.3%, but it is what leaves single letters loose
inside titles that are about to be read token by token. Work-mode words are **not** removed
here — they belong to §5.3, where the mode is actually wanted, and where the location field
that carries most of the signal is being read anyway.

Measured against the raw corpus by SQL approximation before the rules were written: the
whitelist collapses 4.9% of distinct titles, gender markers 0.3%, and the unguarded
seniority words 5.4%. The step's own report records the real figures.

### 5.2 Engineering roles

A **rule over cleaned titles** answers one question: is this an engineering role? A vacancy
that is not one is out of scope, and nothing downstream asks anything finer. The rule reads a
title as a **function head** — the noun naming what the role does — qualified by modifiers
naming the domain, per ADR-0009. It was specified here as a token dictionary; the hand check
is what replaced that, and the criterion document carries the current form.

It answers in **three states** — in, out, or unknown — for the reasons in ADR-0008. Unknown is
the **default**: in and out are claims a rule has to earn, and a title no rule reaches has not
been read. The reason says which kind of not-knowing it is — `unruled`, no rule reaches this
title; `domain_ambiguity`, the head is known and the domain is not (`Engineer II`, `Electrical
Engineer`); `scope_ambiguity`, a ruled phrase whose variants genuinely split (`Data Analyst`,
`Support Engineer`). Only `unruled` is ours and only `unruled` is expected to fall; it starts
above half the corpus. Each vacancy records **which signal classified it**, title or body, so a
wrong answer can be attributed to the right rule.

What counts as an engineering role is written down in `docs/engineering-role-criterion.md`,
as a procedure plus the rulings it must reproduce. It is wider than the phrase usually
carries: a role a software background alone would make you a credible candidate for today is
in scope even when it does not write software, because the product exists to find work a
person can actually take. Scrum Master is in on that ground; Product Manager is out.

The eight role families of the pre-reset design — `SOFTWARE_ENGINEERING`, `DATA_ENGINEERING`,
`MACHINE_LEARNING`, `SRE_DEVOPS_PLATFORM`, `QA_TEST`, `SECURITY`, `MOBILE`, `EMBEDDED` — are
**not modelled in this iteration**. Nothing consumes them: the score in §6.2 is built from
skills, seniority and eligibility, and a family appears only as a column and a response
field. Eight dictionaries to tune and eight error rates to measure is a large cost for a
label nothing reads. The qualifiers stay grouped in the source, so a later iteration that
wants facets can split them against a corpus it understands better than this one does.

A second pass reads the description body of the **unknown** vacancies, by rule, and resolves
what it can. The reason code says which question to ask: a domain-ambiguous title needs the
body checked for domain markers, a scope-ambiguous one needs the criterion re-applied. An
`unruled` title does not reach this pass at all — it says the title stage has no rule for that
head, which a line added to a list fixes more cheaply than a description read.

Separately and offline, a mining report lists the function heads the criterion does not know,
ranked by the share of the corpus each reaches, as input for growing the head list by hand.
The lists only ever grow through a decision, never at runtime.

The classifier's error rate is measured on samples **stratified by what it predicted** and
labelled by a Claude Code session applying the written criterion, before that session has read
the rules the classifier runs on (ADR-0007). An iteration is 1000 labels: 600 drawn from what
it called out, which measures how many it misses; 300 from the unknown pile, which measures how
much recall is parked rather than lost; and 100 from what it called in, which measures how many
it lets in wrongly. The rejected stratum is the largest because the miss rate is the number
that matters, and 600 labels put its noise floor near ±1pp, which is what a 2% threshold needs
to be measurable at all.

This is a **loop**, not a gate, and **one session is one iteration**: label the sample the last
session left, score the rules, change them, re-classify, draw the next sample. It exits when
two consecutive iterations hold a miss rate at or below 2%, a false accept rate at or below
10%, and an unknown share that fell by less than a percentage point. The discipline against
over-declining is not decoration — without it a classifier that answers unknown to everything
satisfies the other two thresholds on the first iteration — but it is a direction rather than a
fixed cap, because ADR-0009 made unknown the default and a fixed 10% would gate nothing for
many iterations.

The rules themselves are code and nobody signs off on them. `docs/engineering-role-criterion.md`
is the prose the loop answers to; how the classifier satisfies it is the loop's business, bounded
only by cost — whole-word matching over the cleaned title, no description reads, no network, a
full corpus pass under ten seconds.

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

The pre-reset `FULLY_REMOTE` value — remote *and* naming no place — is **dropped**. It was
not a statement about where the work is performed but about which places the posting names,
which is the country set of §5.4. Modality and place are extracted separately instead.

**Declared location**: the location field as the company wrote it, with the work-mode words
taken out, stored verbatim. Normalizing those 26,290 distinct strings into places is real
work with its own rules and its own accuracy, and it belongs to the step that derives the
country set, not here. A named location is **never on its own a reason to rule a vacancy
out** — a US-based company can and does hire from abroad, so reading `Remote - US` as an
exclusion manufactures false negatives in a product that hard-filters on eligibility.

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

### 5.5 Skills

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

## 10. Build order

A thin vertical slice first, against freshly crawled Greenhouse data rather than the
pre-reset corpus: discover slugs, probe boards, ingest vacancies, clean titles, classify,
normalize, extract eligibility, extract skills, score, and serve one endpoint to a page that renders a
top 20. Ashby, the twelve-crawl backfill and the wider role dictionary come after that path
runs end to end.

The 216,868 normalized rows still live on the production server are kept as a **validation
set**: the rewritten normalizer's output can be diffed against the old one, and any
disagreement is a bug in one of them.

## 11. Measurement obligations

These are consequences of the decisions above and belong in the plan, not in hindsight.

1. **Eligibility recall**, measured on a hand-labelled sample of ~200 remote engineering
   vacancies. It is the binding constraint on the product's output.
2. **Classifier accuracy**, measured on 1000 labelled titles per iteration, drawn
   stratified by what the classifier predicted — 100 in, 600 out, 300 unknown — and
   re-measured on a fresh sample each time the rules change. Reported as miss rate, false
   accept rate, and the unknown share of the corpus, split by reason alongside.
3. **Title cleaning**, reported as the share of distinct titles each rule collapses, which
   is what decides whether a rule stays.
4. **Normalizer regression**, by diffing against the pre-reset corpus.
5. **Real runtimes** for discovery, probing and sweeping, before anything is scheduled.

## 12. Explicitly out of scope

Authentication and multiple users; role families finer than "engineering"; tech-adjacent
roles a software background alone would not open, which §5.2's criterion rules out; résumé
parsing; required-versus-mentioned skill discrimination; multi-dimensional seniority;
salary-based ranking; LLM extraction of any kind; scheduled ingestion; TLS, a domain and
rollbackable deploys; ATS beyond Greenhouse and Ashby.
