# OneProfile — Product Requirements, MVP iteration

Status: agreed 2026-09-19, re-sequenced 2026-09-24 (§12), section statuses brought up to date
2026-10-07, re-scoped 2026-10-20 around the Finder (§12). The spec for what is not built yet. What is built is summarized with pointers to the code, the ADRs and
`docs/engineering-role-criterion.md`, which own it. Sequencing lives in the issue tracker.

## 1. The problem

Job hunting for tech-adjacent roles is slow because the three things that matter are all
missing from the tools that exist: there is no single corpus of openings, there is no way
to tell whether a "remote" job will actually pay someone in Argentina, and there is no way
to rank what remains against a specific person's skills.

OneProfile is a backend that solves those three, plus a public site, in the same repository,
that reads what the backend publishes (ADR-0020). The first problem is answered first, by the
Finder (§12): a place to look for engineering vacancies with filters no general job site has.
Matching follows once the Finder has traction.

## 2. Users and scope

Matching serves **one user: the developer**. The Finder and the Explorer (§12) are the public
surface, anonymous and read-only. There are no accounts, no
authentication, no sessions and no `user` table. The `profile` table has its own id and the
API takes a profile identifier, so adding real users later is a `user` table plus a foreign
key — but nothing is built for that case now.

Roles are limited to **engineering roles** this iteration, as
`docs/engineering-role-criterion.md` defines them. The wider tech-adjacent set is the
product's eventual purpose and is deliberately deferred.

## 3. Definition of done

**The Finder is public with its launch gate met** (§12.6). Matching's bar comes after it: the
top 20 matches are jobs the developer would actually apply to, judged by hand against one real
search.

## 4. Feature 1 — a meaningful corpus of jobs

### 4.1 Sources

Four applicant tracking systems, built and run: **Greenhouse**, **Ashby** (#20), **Lever** (#79)
and **Workday** (#81, `docs/measurements/2026-10-workday-run.md`). Why Ashby before Workable and
Lever: ADR-0002.
Which ATSs come next, and in what order: ADR-0022.

### 4.2 Slug discovery — built

Slugs come from Common Crawl's cluster index over anonymous HTTPS range requests (ADR-0001).
Backfill depth is **12 crawls**, roughly one year: the newest crawl yielded 77% live boards
at 31.9 vacancies each, older ones 18–36% live at 3.8 each. Ashby's twelve crawls name 4597
boards, 72.5% of them live at 18.3 vacancies each (#20). Lever's twelve are CC-MAIN-2025-43 and
older, since Cloudflare refuses CCBot after it; the Wayback Machine is read whole beside them for
every ATS (ADR-0023). A Workday board is a subdomain, so both archives are asked for every
subdomain of `myworkdayjobs.com` and `myworkdaysite.com`, and its slug is `<tenant>.<host>/<site>`.
Each archive's yield: `docs/measurements/2026-10-wayback.md` and
`docs/measurements/2026-10-lever.md`.

### 4.3 Board probing and vacancy ingestion

Built for every ATS, each at its own pace. A company keeps the **region** of the host its board
was discovered on, `US` or `EU`, and is asked for in that region's API where its ATS has one per
region (ADR-0022). Companies are probed and recorded as `ACTIVE`, `EMPTY`
or `NOT_FOUND`; only `ACTIVE` companies are swept. `vacancy` is a **mirror of the board**: upsert
what is returned, delete what has disappeared.

Ashby: `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true`.
Unauthenticated, whole board in one response, no pagination, so a probe reads what a sweep does.
It names no company, so an Ashby company has no name. Its structured fields (`workplaceType`,
`secondaryLocations[]` with per-location countries, compensation with its interval) are kept as
Ashby writes them, on its own class of vacancy (§7, ADR-0021). The sweep derives nothing from
them; the passes of §5.3 and §5.4 may read them as evidence. Its yield:
`docs/measurements/2026-10-ashby.md`.

Lever: `GET https://api.lever.co/v0/postings/{slug}?mode=json`, or `api.eu.lever.co` for a
company in the EU; each answers 404 for the other region's boards. Unauthenticated, whole board in
one response; a probe adds `limit=1`. It names no company. Its description comes in parts
(description, headed lists, pay description, closing), held as its page shows them one after
another. Its structured fields (`workplaceType`, `country`, `categories` with commitment, team
and every location, `salaryRange` with its interval) are kept as Lever writes them, on its own
class of vacancy. Its API, pace and yield: `docs/measurements/2026-10-lever.md`.

Workday: `POST https://{tenant}.{host}.myworkdayjobs.com/wday/cxs/{tenant}/{site}/jobs`, 20
vacancies a page with no description; a probe asks for one. Paging stops at 2000, so a larger
board is read in parts by facet. A description is a `GET …/job/{path}` a vacancy, asked for only
where the title is `IN`, or `UNKNOWN` for domain or scope ambiguity; the rest are held from the
list alone. Its own errors (`400`, `401`, `403`, `404`, `422`) mean no public board. Workday
names the company only on a vacancy's page, so the sweep records it, the name a board's vacancies
give most often, as Workday writes it (a legal entity such as "2100 NVIDIA USA"); a board whose
pages were not read keeps none. A company takes its name wherever its ATS gives one, and has none
where it does not (decided with Elias on 2026-10-24). Every request
of its probes and sweeps shares one pace, 16 a second and 20 open (ADR-0025). Its time type,
country and every location are kept as Workday writes them, on its own class of vacancy. Its API,
pace and the boards it holds: `docs/measurements/2026-10-workday.md`.

All ingestion is triggered manually by an administrative endpoint today. In production every
stage runs on a schedule (§12.5), once the real runtimes are measured.

## 5. Feature 2 — classification and eligibility

This feature runs in three passes, in this order: **cleaning** over the whole corpus,
**classification** over the whole corpus, and **normalization** over the engineering subset
alone. The order is deliberate. Cleaning is what makes the corpus readable enough to build a
classifier by eye; classification is what earns the right to stop reasoning about personal
trainers and psychiatric nurse practitioners; and normalization is where rules may assume
the vocabulary of engineering, because by then nothing else is left.

### 5.1 Cleaning — built

A character whitelist, gender-marker removal, and extraction of the unguarded seniority words
(`senior`, `sr`, `junior`, `jr`, `principal`, and `semi senior`/`ssr` as mid). The levels a
title names are kept as a **list**, since the title is one source of seniority among
several. Work-mode words are **not** removed here; they belong to §5.3. The rules and their
measured yield: `docs/measurements/2026-09-title-cleaning.md`.

Cleaning also strips decoration from each description and cuts it into **segments** once, with
boilerplate marked, for every pass that reads descriptions (#46). Where a board declared no
language, as Ashby never does, cleaning detects it from the description, by script and by function
words, so the body pass skips a vacancy it does not read (#20). A declared language is kept.

### 5.2 Engineering roles — title pass built, body pass in its loop

A rule over cleaned titles answers whether a vacancy is an engineering role, in three states
(ADR-0008, ADR-0009, ADR-0010). The criterion is `docs/engineering-role-criterion.md`; the
rules are code, and nobody signs off on them. Measurement and the loop's exit: ADR-0008.
Role families finer than "engineering role" are **not modelled**: a vacancy's role is read
from its title against a list of titles companies write (§5.3, ADR-0024).

The title loop closed with #35, and its rules are frozen. The **body pass** (#11, ADR-0012) is
in its rule loop: it reads the description of every vacancy in the pile, by rule, and resolves
what it can. The reason code says which question to ask: a
domain-ambiguous title needs the body checked for domain markers, a scope-ambiguous one needs
the criterion re-applied. An `unruled` title does not reach this pass.

### 5.3 Normalization

Over the engineering subset only. Everything in this pass may assume the vocabulary of
engineering work, which is what makes its rules small. It derives the role (#25), the work mode
(#86) and the rest of the title's seniority. What it decides and why:
`docs/measurements/2026-10-title-breakdown-probe.md` and ADR-0024.

**Role.** The title is broken down into the **normalized title**: title seniority, work-mode
words, places and the skills the title names go, the function head is read as ADR-0009 reads it,
and what follows the head goes too unless it names the kind of work, which moves in front of the
head (`Software Engineer, Backend` → `backend engineer`). A skill that is the only word in front of
the head goes as well (`Java Developer` → `developer`). The normalized title is then looked up in
the **role list**, a curated file stable between runs and grown on each new snapshot: a
normalized title joins it once enough vacancies carry it, synonyms are merged when a candidate
would expect the same day-to-day work, and the list is flat. A title naming two heads gets both
roles; a normalized title the list does not hold leaves the vacancy without one, never with a
guess. Done when the list covers 70% of the subset (ADR-0024). Giving the rest a role from their
other fields is after the MVP (#87).

The breakdown (#92) and the role list (#25) are separate pieces of work: many normalized titles map
to one role. The breakdown is built (`POST /title-breakdowns`, after skill extraction); its first
run is `docs/measurements/2026-10-title-breakdown.md`. How alike the skills of two titles' vacancies
are is shown to the agents that draft and review a merge, as evidence, never as the rule. **Role
nearness**, how near two roles are by their skills, is computed beside the list on each snapshot
(ADR-0024, amended 2026-10-26).

**Management level.** A leadership word that leads an area — `lead`, `manager`, `director`,
`head`, `VP`, `chief` — is taken out of the title as the vacancy's management level (lead,
manager, director or executive), and the role is read from the area it leads: `Engineering
Manager, Backend` is a manager whose role is backend engineering. A title naming a job rather than
a rank keeps its word (`product manager`, `technical program manager`). The level is separate
from the seniority level, so a match or a filter can keep a manager's job from someone who did
not apply for one (ADR-0024, amended 2026-10-26). How the Finder shows the level is open.

**Seniority.** The guarded words are read in the title breakdown, which takes them out anyway:
`staff`, `mid`, `entry`, and the numbered levels `II`, `III`, `IV`. Restricting to the subset is
what makes them tractable — probed over an engineering-shaped slice of 37,309 vacancies, `staff`
appears in 9.4% of titles and almost always means the level, so the pre-reset guard against
`staff nurse`, `staff accountant` and `staff attorney` is **dropped**: those 163 titles are not in
the subset. What survives is the one guard that still fires inside it, `chief of staff`. `mid`
and `entry` are down to 0.4% and 0.2%, and `semi senior` to 5 vacancies in the subset and 16 in
the whole corpus, which is why §5.1 collapses it into mid rather than giving it a level of its
own.

The numbered levels are extracted **as they are**, with no claim about where they sit on the
ladder: the titles do not say whether `Engineer II` is junior or mid, and 4.1% of the subset
carries one. Resolving them is left to §6.1, where the body's years-of-experience phrasing is
evidence rather than assumption.

**Work mode**, one of remote, hybrid or onsite (#86). The location text decides where it names a
mode; otherwise the work-mode field Ashby and Lever publish decides; otherwise the title.
Where the location and the field disagree, about 170 vacancies of the subset, the location was
usually the right one. Null means "not declared" and never "onsite". There is no `FULLY_REMOTE`
value; work mode and declared location are separate (ADR-0004).

The **declared location** is not derived here. The step that derives the country set reads it
as the ATS publishes it, with Ashby's and Lever's lists of locations, and turning it into places
is that step's work (#26). A named location is **never on its own a reason to rule a vacancy
out** (ADR-0004).

### 5.4 Eligibility

The question the system answers is **"will this company pay someone located in this
country?"** — not "is this job remote" and not "am I authorized to work there". The country is
the one a Finder visitor picks (ADR-0019); for matching it is the developer's, Argentina. Being
hired as a contractor, through an employer of record or otherwise, counts as eligible.

The extractor reads the description body and produces **the set of countries or regions the
posting says it will engage someone in**, and stores it. The three-state signal is derived at
query time by testing the country's membership in that set:

- `ELIGIBLE` — the set is global, names a region containing the country, or names it.
- `EXCLUDED` — the set is stated and excludes the country. Work-authorization language such as
  "must be authorized to work in the United States" lands here, because it implies they pay
  only US-based people.
- `UNKNOWN` — no set is stated.

An explicit timezone requirement the country cannot meet is a separate disqualifier.

Extraction is rule-based only (ADR-0005). Eligibility is hard-filtered for matches, and the
Finder's toggle narrows to it, which makes recall the binding constraint (ADR-0004, ADR-0019);
recall is measured by hand-labelling a
random sample of roughly 200 remote-flagged vacancies, and the missed phrasings are the work
queue.

### 5.5 Skills — taxonomy and extraction built, precision measured

Skills use a **curated engineering taxonomy of roughly 1,500–3,000 concepts**, stored as
`id, canonical_name, category, aliases[]` and matched against description text by a
normalized trie pass. The same taxonomy is the picker the user chooses their own skills
from, which is what makes coverage a meaningful number.

The taxonomy is seeded from **O\*NET Technology Skills**, **GitHub Linguist** and
**Wikidata**; why not Lightcast, ESCO or Stack Overflow: ADR-0003. It is built:
`src/main/resources/taxonomy/skills.tsv`, with 2,009 skills (provenance in `docs/taxonomy.md`),
grown on each new snapshot by skill discovery (#41, ADR-0013). Extraction (#15) is built:
`POST /skill-extractions` fills `vacancy_skill`; its precision, measured in #65, is 0.945. It runs
before the body pass, over the vacancies the title decided in and the pile, and the body pass
reads its skills (ADR-0017). Descriptions are not lemmatized.
A skill is one named technology, and each of its keys is plain or context (ADR-0015): a plain key
is matched anywhere, a context key such as Go or Kafka only in its own casing and where a plain key
confirms it, in the same segment or as its parent or child (ADR-0018). Extraction reads the title
and the segments that are not boilerplate.

Whether a vacancy requires a skill or only mentions it is never a filter. The Finder's skill
filter keeps the vacancies that mention a picked skill, and orders those that require it first
(filter rank, §12.1), once the requirement reader (#66) gives that status; until then it orders
by how many picked skills a vacancy mentions. Coverage in matching is a rough signal and is
accepted as such.

## 6. Feature 3 — matching

Matching comes after the Finder, once it has traction (§12). How it is served is reopened by
ADR-0020, which leaves no read API of our own.

**Skill functions, not yet decided.** A candidate who knows Prefect is a near match for a vacancy
asking for Airflow, and skill overlap alone scores it as a miss. A relation from each skill to the
function it serves (Airflow, Dagster and Prefect are orchestration; Pinecone, Weaviate and pgvector
are vector stores) would let matching count it. The taxonomy has neither: a category is too coarse
(dbt is `data` too) and a parent is a part, not a kind (pgvector's is PostgreSQL). It would be a
new column of the taxonomy, kept by hand, and could make role nearness more robust as well. It
replaces extracting practices from descriptions, which is not done: most skills imply their
practice (ADR-0024, amended 2026-10-26).

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

- `company` — ats, slug, region, name, board_status, last_probed_at.
- `vacancy` — the board mirror, one table with a class per ATS told apart by `ats`, its
  company's (ADR-0021). Every vacancy holds what any ATS would be expected to publish: title,
  location, department, description, url, the pay range and when it was first published, null
  where the vacancy did not publish it. `external_id` is text, since Ashby's ids are UUIDs. A
  Greenhouse vacancy adds `pay_title`, `updated_at` and its declared `language`; an Ashby one
  adds `workplace_type`, `address_country`, `secondary_locations` with their countries, and
  `pay_interval`, as Ashby writes them; a Lever one adds `workplace_type`, `country`,
  `commitment`, `team`, `all_locations` and `pay_interval`, as Lever writes them; a Workday one
  adds `time_type`, `country` and `all_locations`. Discarding Ashby's structured country to match
  Greenhouse's weaker free-text shape would throw away the best available eligibility input.
- `normalized_vacancy` — one row per vacancy holding every derived scalar: cleaned title,
  normalized title, whether it is an engineering role, the signal that classified it, work
  mode, seniority, eligibility state and the extracted country set. A
  single wide table is correct here **because** every derivation is a cheap pure-function
  rule pass; had an LLM derivation been in scope, the cost asymmetry would have forced
  separate tables. Columns arrive with the step that fills them, not up front.
- `normalized_vacancy_title_seniority` — the levels a title names, one row each, because a
  title may name more than one and the counting these reports need is a join rather than an
  unnest.
- `vacancy_skill` — join table, one row per (vacancy, skill), unique on the pair so
  re-extraction is idempotent, indexed on both columns. Each row also holds the segments the
  skill was found in and whether the title names it, which the body pass reads (ADR-0018). At roughly 10–15 skills per vacancy
  this is 2–3 million rows, which is unremarkable for PostgreSQL. It is preferred over an
  array column because the UI needs to show *which* skills matched and which are missing,
  and because a wrong score must be inspectable.
- `profile` — the user's declared skills and seniority level.

Derived data lives in its own tables so rules can be re-run without touching the ATS APIs.

The public database holds none of these tables. It holds the **read schema** the publish stage
rebuilds after each cycle (ADR-0020): one row per vacancy of the engineering subset with what the
Finder filters, orders and shows, its skills and its country set. It sits between the pipeline
and the read app only; the site never sees it.

## 8. API

The build is two Spring Boot apps in one repository, as two Maven modules (ADR-0020):

- **`pipeline`**: what the backend is today. Ingestion remains a set of `/admin/**` trigger
  endpoints on Elias's server, reachable from nowhere else, and the publish stage pushes the read
  schema to the public Postgres.
- **`api`**: the read app, over the read schema. Its endpoints are the contract with
  the site, anonymous, fixed in shape so Cloudflare can cache them until the next publish:
  - the Finder's query: its filters, eligibility for the picked country (ADR-0019), its three
    orders with filter rank (§12.1), a page of vacancies, and for each filter how many vacancies
    it cannot see;
  - one vacancy, with its description, for its panel and its page;
  - the Explorer's aggregates (§12.2) and the methodology's counts (§12.3);
  - recording an apply click, its only write.

## 9. Stack and deployment

Unchanged from the pre-reset setup: **Spring Boot 4.1.1, Java 25**, Maven wrapper, Spring
Data JPA and Spring Web MVC, **PostgreSQL 18**, **Flyway** migrations with
`ddl-auto=validate`, Hibernate JDBC batch size 50, jsoup for HTML-to-text, Testcontainers
for tests, a multi-stage `eclipse-temurin:25` image, GitHub Actions running the test suite
and publishing to GHCR, and manual deployment over SSH to the ZeroTier-reachable server.

New dependencies: `webarchive-commons` for the ZipNum cluster reader and SURT
canonicalization, and commons-compress for decompressing concatenated gzip members when a
byte range spans several.

The pipeline pushes to the public side (ADR-0020):

- **The public side**: Postgres and the read app, in containers of their own on Elias's server,
  the read app reached through a Cloudflare tunnel and its answers cached until the next publish.
  They move to a 4 GB Hetzner VPS in Ashburn when they need availability, which may wait until
  there is traffic (ADR-0026, #91). Sessions, when they come, are Spring
  Security in the read app.
- **The site**: an Astro site with React components, in `site/`, on Cloudflare Pages
  under a paid domain. Each publish rebuilds the pages worth ranking for; a vacancy page is
  rendered on request by a Pages Function and cached.
- **Traction**: Cloudflare Web Analytics, which sets no cookie, and the count of apply clicks.

Two known deployment gaps carried over and not fixed this iteration: images are tagged
`latest` only, so **a deploy cannot be rolled back**, and deployment is manual.

## 10. Measurement obligations

Still open:

1. **Eligibility recall**, measured on a hand-labelled sample of ~200 remote engineering
   vacancies. It is the binding constraint on the product's output.
2. **Real runtimes** for discovery, probing, sweeping and publishing, before anything is
   scheduled.

## 11. Explicitly out of scope

Accounts, sessions and multiple users, until community feedback asks for a feature that needs
them; role families finer than "engineering"; tech-adjacent roles a software background alone
would not open; résumé parsing; a filter on whether a skill is required; multi-dimensional
seniority; salary-based ranking; LLM extraction of any kind; rollbackable deploys. For the Finder
and the Explorer: free-text questions, and structured data for Google's job search, which is a
later experiment.

## 12. The Finder and the Explorer — the public surface

Agreed 2026-09-24, re-scoped 2026-10-20. The **Finder** comes first: a place to look for
engineering vacancies with good filters, no account and nothing to set up, which is an easier
first sell than matching, whose profile is friction a site without a reputation cannot ask for.
The **Explorer** comes second: a product of its own, and the way people are brought to the Finder.
Posts about job seeking, drawn from what the Explorer shows, follow it. Matching follows on
traction, first as an order of the Finder's results, and accounts only when community feedback
asks for a feature that needs them.

The audience is job seekers, globally: most of the corpus is US-based, and the Explorer's
measurements are presented as such. Everything has to look good; the ambition is for it to be
shared widely. How it is served: ADR-0020.

### 12.1 The Finder

It lists the engineering subset, duplicates collapsed, and sends every "apply" to the vacancy's
board.

**Filters**: role, seniority, skills, country, work mode and salary.

- **Country** defaults to a guess from the visitor's browser, and can be changed and is
  remembered. It hides the vacancies excluded for that country, shows the eligible and the
  unknown, the unknown marked as saying nothing, and a toggle narrows it to the eligible
  (ADR-0019).
- **Skills** keep the vacancies that mention a picked skill. Whether one is required is never a
  filter; it orders, through filter rank.
- **Role** takes several roles at once, from the role list (§5.3). A vacancy whose title matches
  no role has none; the filter hides it and says how many it hid.
  The roles nearest to the ones picked are offered from role nearness (§5.3). They are added only
  when the visitor picks them, never on their own.
- **Work mode and salary**: an undeclared value is never guessed. A filter on either hides the
  vacancies that do not declare it, and says how many it hid.

**Orders**:

- **The default** groups vacancies by the calendar day they were first published, in the
  visitor's timezone, newest day first, and orders each day by filter rank.
- **Newest**, by first publication alone. `updated_at` is no measure of recency: 55% of the
  engineering subset was updated more than 30 days after it was first published.
- **Filter rank** alone, newest first among ties: vacancies requiring more of the picked skills
  first, then those only mentioning them. Until the requirement reader (#66) lands, it counts the
  picked skills a vacancy mentions.

**A vacancy** opens in a panel, and has its own URL for sharing and for search engines. Its
description is shown as the sweep stores it, as marked lines rendered into paragraphs, headings
and lists, with the boilerplate set apart. The company is credited, and its request to remove its
text is honoured.

**Traction** is measured by visits, from Cloudflare Web Analytics, and by apply clicks.

### 12.2 The Explorer

A **structured** explorer: a person picks a measure, a grouping and filters, and gets a
chart. Every chart is an exact query. There is no free-text question box, which would need an
LLM at request time. Clicking a bar opens the Finder with that bar's filters.

Launch dimensions: **role** (from the role list, §5.3), **seniority**
and **skills**. Eligibility comes next. Salary only ever appears with the subset it describes
stated on the chart, since it exists only where the ATS publishes pay.

The query "best stack for a role" is the share of that role's vacancies that name each skill.

How numbers are shown:

- **Shares beside counts.** The denominator is the engineering roles in scope. The corpus is
  a sample of the market, so relative numbers are the honest reading while it is small.
- **The unknown share of the charted dimension**, on every chart: no role in the title on
  role charts, no stated seniority on seniority charts, no skill found on skill charts.
  Unknowns are not spread evenly, so leaving them out of the denominator biases the shares;
  showing their size says how much the chart cannot see.
- **Seniority counts each vacancy once**, at the lowest level it names (§6.1).
- **Role counts a vacancy in every role its title names**, so role shares can add up to more than
  100% (ADR-0024).
- **Small samples.** A cell with fewer than 10 vacancies is hidden. Every other bar carries its
  n and a 95% Wilson interval, which stays well-behaved at small n and near 0% or 100%.

### 12.3 The methodology page

Launched with the Finder, and linked from every chart: the one-line criterion ("jobs you can
apply to from a software background"), which ATSs are covered, board and vacancy counts, the
as-of date, what unknown means, and the known biases — the selection of companies by ATS and
by crawl, the US weight of the corpus, and salary coming only from postings that publish it.

### 12.4 History

The Explorer shows the live corpus only until **stability**: 100k engineering vacancies live in a
single sweep, the launch gate's figure (§12.6), and 30 consecutive days of cycles without manual intervention. From then on series are
recorded just before each sweep — vacancy count per (role × seniority) and skill share per
(role × skill) — and trend views are added to the Explorer once there are enough points to show
one. History is numbers, not vacancies (ADR-0011).

Reaching 100k needs Ashby, then discovery from the Wayback Machine for Greenhouse and Ashby, then
Lever, Workable and, if still short, Workday (#74, ADR-0022, ADR-0023).

### 12.5 Scheduling

Every stage runs on a schedule in production: discovery when a new crawl is published,
probing after discovery and periodically for empty and not-found boards, and every day a sweep
followed by a publish, so a vacancy that closed is gone from the Finder by the next day.

### 12.6 Launch

The Finder is only the developer's until the launch gate is met; then it is public, anonymous
and read-only. The Explorer launches after it.

The launch gate:

1. Greenhouse and Ashby both swept.
2. The engineering-role classifier has met its exit criterion.
3. Skill extraction has a measured precision.
4. Roles are read from titles, work mode is derived, and duplicate vacancies are collapsed.
5. Seniority is read from the body.
6. Eligibility is extracted, and its recall measured.
7. **100k engineering vacancies live in one sweep.** The snapshot has 55,331 on live boards:
   34,173 from Greenhouse and 21,158 from Ashby (`docs/measurements/2026-10-ashby.md`). The figure for one
   country, eligible plus unknown, is reported beside it but not gated on. The figures for
   Argentina, Brazil and Mexico decide later whether marketing aims at developers outside the US.

The gate is frozen as of 2026-10-20: anything new goes after the launch. Each extractor it waits
on is built and measured once, as skill extraction was (#65), and becomes a rule loop only if that
first iteration falls short (`docs/agents/rule-loop.md`).

### 12.7 Order of work

Volume is done: Ashby (#20), then whatever reached 100k vacancies (#70). The rest runs in sequence
(decided 2026-10-26):

1. **Extraction**: the title breakdown (#92) → roles from titles (#25), beside work mode (#86) → seniority from the body (#8) →
   eligibility and its recall (#13, #12) → deduplication (#27).
2. **Data quality**: the corpus fixes ready for an agent (#83, #85, #49).
3. **Scheduling**: the schedule (#30), with the passes and the Workday sweep fitted to the server
   (#89, #90, #88).
4. **The public side**: the two modules (#71) and the public side on Elias's server (#68) → the
   publish stage (#28) → the read app (#72) → the Finder (#69). It waits for extraction rather than
   publishing columns left null.

The Finder launches when the gate is met. The Explorer (#29) comes after the launch, and the
profile and matching after traction. The series follow once the pipeline reaches stability.
