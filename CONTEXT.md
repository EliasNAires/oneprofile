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
Checking whether a slug corresponds to a real board, and recording its board status. A board
the ATS would say nothing about is not probed at all: the company keeps the board status it
had, so the next run asks again.
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
One line of a crawl index: a URL as it was fetched, and where the response was stored. A crawl
index mixes three kinds — the pages themselves, the robots.txt files, and the responses that
never became a page, redirects above all — and only the first kind is a capture of a board.
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
block it was sampled from. A prefix is searched here first, and the sampling is why the
blocks on either side of the matches are read too.
_Avoid_: summary index, manifest, secondary index

**SURT Key**:
A URL rewritten so that its host reads from least to most specific, as in
`io,greenhouse,boards)/`. The form a crawl index is sorted by, so it is also the form a
prefix search has to be asked in.
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
The facts derived from a vacancy by rule, rather than published by the ATS: its normalized
title, role family, work mode, seniority level, eligibility and skills. Derived data is
never mixed into the vacancy itself, so rules can be re-run at any time.
_Avoid_: enriched vacancy, parsed vacancy, processed vacancy

**Normalized Title**:
A vacancy's title reduced to its role-bearing words, with seniority, work mode, gender
markers and decoration removed.
_Avoid_: clean title, canonical title, title slug

### Classification

**Tech-Adjacent Role**:
A role whose hiring market is shaped by software work, whether or not the role writes
software. The product exists to serve these; the current iteration covers only the
engineering subset of them.
_Avoid_: tech role, IT role, technical role

**Role Family**:
The hiring market a vacancy belongs to, such as software engineering or security. A vacancy
belongs to at most one. Belonging to none is what makes a vacancy out of scope.
_Avoid_: category, discipline, job function, department, specialization

**Work Mode**:
Where the work is performed, as declared by the vacancy: remote, fully remote, hybrid or
onsite. Undeclared is its own answer and never means onsite.
_Avoid_: modality, workplace type, location type, arrangement

**Seniority Level**:
The experience level a vacancy asks for, on one ordinal scale from entry to principal.
Where a vacancy names several, the lowest applies, because a vacancy publishes the minimum
it will accept.
_Avoid_: level, grade, rank, band, experience

### Eligibility

**Eligibility**:
Whether a company will pay someone located in the user's country. Not a question of work
authorization, and not implied by a vacancy being remote — a remote vacancy is usually
remote only within one country.
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
