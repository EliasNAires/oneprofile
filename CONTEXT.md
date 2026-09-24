# OneProfile

Finds job openings a person can actually take, and ranks them against what that person can
actually do. The domain is built around three questions: what openings exist, which of them
would hire this person, and which of those fit them best.

## Language

### Sources

**ATS**:
An applicant tracking system that hosts company job boards and exposes them publicly.
_Avoid_: job site, job board provider, platform

**Board**:
One company's set of published openings on an ATS.
_Avoid_: careers page, job page, listing page

**Slug**:
The identifier a company is known by within one ATS. Unique only within that ATS, so a
company is identified by the pair of ATS and slug.
_Avoid_: handle, token, company id, tenant

**Company**:
An employer that has a board on an ATS. Its readable name comes from the ATS, never from
the slug.
_Avoid_: employer, org, account, client

**Board Status**:
Whether a board was found and whether it had openings when last checked: active, empty, or
not found. Unknown until the board has been checked.
_Avoid_: alive, valid, health

**Probe**:
Checking whether a slug corresponds to a real board, and recording its board status.
_Avoid_: validate, ping, health check

**Sweep**:
Reading every opening from a board and reconciling them against what is already held.
_Avoid_: fetch, sync, import, scrape

**Crawl**:
One dated snapshot of the public web published by Common Crawl, searched for board URLs to
discover slugs.
_Avoid_: dataset, archive

### The Crawl Index

Common Crawl's own names for the parts of a crawl's index are kept, because they are the
names on the files themselves and in the tools that read them.

**Crawl Index**:
The record of what a crawl captured: one line per capture, giving its SURT key, when it was
taken and which archive file holds it. Sorted by SURT key, which is what makes a host's
captures contiguous and a prefix searchable.
_Avoid_: CDX API, index server, catalog

**Capture**:
One line of a crawl index: a URL as it was fetched, and where the response was stored. Only
page captures are captures of a board; robots.txt and redirect captures are not.
_Avoid_: record, entry, hit, snapshot

**Shard**:
One file of a crawl index. A capture is located by shard and by the block within it.
_Avoid_: part, partition, segment

**CDX Block**:
A compressed run of index lines inside a shard, addressed as a byte range. The smallest
piece of a crawl index that can be read on its own.
_Avoid_: chunk, page, range, slice

**Cluster Index**:
The sampled table of contents of a crawl index: one line per 3000th capture, naming the
block it was sampled from. A prefix is searched here first.
_Avoid_: summary index, manifest, secondary index

**SURT Key**:
A URL rewritten so that its host reads from least to most specific, as in
`io,greenhouse,boards)/`. The form a crawl index is sorted and searched by.
_Avoid_: canonical URL, normalized URL, sort key

### Openings

**Vacancy**:
A single open position at a company, as published on its board. Held as a mirror of the
board: a vacancy that disappears from the board is treated as closed.
_Avoid_: job, posting, listing, opening, role, ad

**Description**:
The free-text body of a vacancy. The only place where eligibility and much of the skill and
seniority evidence appears.
_Avoid_: content, body, details

**Normalized Vacancy**:
The facts derived from a vacancy by rule rather than published by the ATS: cleaned and
normalized titles, classification, work mode, declared location, seniority and skills. Kept
apart from the vacancy so rules can be re-run at any time.
_Avoid_: enriched vacancy, parsed vacancy, processed vacancy

**Cleaned Title**:
A vacancy's title with what is noise in any title taken out: decoration, gender markers, and
the seniority words that name a level wherever they appear. What classification reads.
_Avoid_: normalized title, clean title, title slug

**Cleaning**:
The pass that gives every vacancy in the corpus a cleaned title. Runs before classification.
_Avoid_: normalization, scrubbing, preprocessing, sanitizing

**Normalized Title**:
A cleaned title reduced further to its role-bearing words. Only engineering roles have one.
_Avoid_: canonical title, title slug

### The Corpus

**Corpus**:
Every company and vacancy that discovery, probing and sweeping have accumulated, taken as
one body of data. What the rules are run over, and what their measured accuracy is about.
_Avoid_: dataset, database, sample, data

**Snapshot**:
The corpus frozen at one stage, so that a rule re-run later is re-run over identical input.
Never replaced. Which snapshots exist, and why: ADR-0006.
_Avoid_: backup, dump, export, copy, fixture

**Cycle**:
One scheduled pass of the pipeline: a probe and a sweep every week, and discovery whenever a
new crawl has been published.
_Avoid_: run, job, batch, refresh

**Stability**:
The point at which the corpus is large and steady enough for its history to mean something:
one million vacancies live in a single sweep, and four consecutive cycles completed without
manual intervention. Series start being recorded here, and not before (ADR-0011).
_Avoid_: maturity, readiness, launch

### Classification

**Tech-Adjacent Role**:
A role whose hiring market is shaped by software work, whether or not the role writes
software. The product's eventual scope; engineering roles are the subset served now.
_Avoid_: tech role, IT role, technical role

**Engineering Role**:
A vacancy a software background alone qualifies someone for — wider than the phrase usually
carries, so Scrum Master belongs. Product Manager is unknown with scope ambiguity: it is plainly
software, but the expertise it needs is not clear from the title. The criterion is
`docs/engineering-role-criterion.md`.
_Avoid_: role family, category, discipline, job function, technical role

**Classification State**:
What classification answers about a vacancy: in, out, or unknown, with a reason when unknown
and the signal (title or body) that decided. See ADR-0008.
_Avoid_: verdict, flag, is_engineering, category

**Function Head**:
The noun in a title that names what the role does — engineer, analyst, technician — as
opposed to the modifiers naming the domain it does it in.
_Avoid_: role noun, job noun, base title, head word

**Domain-Bound**:
Said of a function head whose modifier settles what the role is: a civil engineer is not a
software job. The opposite is domain-free: an audit analyst may work on audit software.
_Avoid_: domain-specific, vertical, contextual

**Yielding Head**:
A head that names a rank or a department — lead, director, manager, support — and gives way
to a head standing behind it: a Lead Analytics Engineer is an engineer.
_Avoid_: rank head, seniority word, prefix head, title prefix

**Generic Head**:
A domain-bound head that names no work of its own — manager, director, consultant. With a
modifier and no software qualifier it is out.
_Avoid_: management head, empty head, weak head

**Marker**:
A modifier that names a domain outside software. A **discipline marker** names a body of
training a person is hired on (mechanical, nurse); a **market marker** names who the work is
done for (finance, retail). See ADR-0010.
_Avoid_: off-domain word, negative keyword, exclusion term, vertical

**Software Qualifier**:
A modifier that names the work as software — the thing a marker is weighed against.
_Avoid_: tech keyword, positive keyword, signal word

**Ruling**:
A verdict fixed for one phrase, overriding every other rule.
_Avoid_: exception, override, special case, hardcoded verdict

**Unruled**:
The unknown reason meaning no rule reaches this title — the rules' backlog, as against the
reasons that say the corpus itself is ambiguous.
_Avoid_: uncovered, unhandled, missing, not found

**Labeller**:
The session that produces the labels a classification state is scored against, applying the
criterion before it has read the rules. Never part of the pipeline. See ADR-0007.
_Avoid_: annotator, judge, oracle, reviewer

**Iteration**:
One session's pass around the classification loop: label the sample the last session left,
score the rules, change them, re-classify, draw the next sample.
_Avoid_: round, batch, run, evaluation, cycle

**Work Mode**:
Where the work is performed, as declared by the vacancy: remote, hybrid or onsite.
Undeclared is its own answer and never means onsite.
_Avoid_: modality, workplace type, location type, arrangement

**Declared Location**:
The place a vacancy names for the work, as it wrote it, once its work mode has been taken
out. Never on its own a reason to rule a vacancy out (ADR-0004).
_Avoid_: office, region, geo, country

**Seniority Level**:
The experience level a vacancy asks for, on one ordinal scale from entry to principal.
Where a vacancy names several, the lowest applies.
_Avoid_: level, grade, rank, band, experience

**Title Seniority**:
Every level a vacancy's title names, kept as found: evidence for the seniority level, not
the level itself.
_Avoid_: seniority, level, title level

### Eligibility

**Eligibility**:
Whether a company will pay someone located in the user's country. Not a question of work
authorization, and not implied by a vacancy being remote.
_Avoid_: work authorization, visa status, right to work, availability

**Country Set**:
The countries or regions a vacancy says it will engage someone in. Eligibility is derived
by asking whether the user's country belongs to it.
_Avoid_: allowed locations, regions, geo, markets

**Eligibility State**:
The three answers eligibility can take: eligible, excluded, or unknown. Unknown means the
vacancy said nothing, and is expected to be the most common answer.
_Avoid_: eligible flag, status, verdict

**Timezone Requirement**:
An explicit demand for working-hours overlap with a given region. Disqualifies a vacancy
independently of its country set.
_Avoid_: hours, overlap, availability window

### Matching

**Skill**:
A named capability a vacancy asks for and a profile can claim, drawn from the taxonomy so
that both sides speak the same vocabulary.
_Avoid_: technology, tool, keyword, competency, requirement

**Alias**:
An alternative spelling or name for a skill. Without aliases the two sides of a match never
meet.
_Avoid_: synonym, variant, alternate label

**Taxonomy**:
The curated set of skills and their aliases. It is the vocabulary of matching and the list a
person picks their own skills from.
_Avoid_: skill list, ontology, dictionary, catalog

**Coverage**:
The share of a vacancy's skills that a profile holds. A vacancy naming very few skills
carries less evidence and is scored as such.
_Avoid_: match percentage, overlap, fit, similarity

**Profile**:
The person being matched: the skills they claim and the seniority level they hold.
_Avoid_: user, account, candidate, résumé, CV

**Match**:
An eligible vacancy paired with its score against a profile. Vacancies that are not eligible
are not matches.
_Avoid_: recommendation, result, hit, suggestion

**Score**:
How well a match fits a profile: coverage, reduced where the profile falls short of the
vacancy's seniority level or where the vacancy carries too little evidence. Exceeding a
vacancy's seniority level costs nothing.
_Avoid_: rank, rating, weight, relevance

### The Explorer

**Explorer**:
The public, read-only view of the corpus: how engineering roles divide by role, seniority and
skill, as it stands now and, after stability, over time. Its one-line promise is "jobs you
can apply to from a software background".
_Avoid_: dashboard, analytics, insights, report

**Share**:
A count taken as a proportion of the engineering roles in scope, always shown beside the
count itself and beside the unknown share of the dimension being charted, since unknowns are
left out of the denominator and are not spread evenly across it.
_Avoid_: percentage, ratio, market share, rate

**Unknown Share**:
The proportion of the vacancies in scope whose value for the charted dimension could not be
determined — no seniority stated, no skill found, classification undecided. How much of the
corpus a chart cannot see.
_Avoid_: missing data, null rate, coverage

**Series**:
A share or count recorded at dated points, kept as a number rather than as the vacancies that
produced it. The only history the corpus has (ADR-0011).
_Avoid_: trend, time series, history table, aggregate
