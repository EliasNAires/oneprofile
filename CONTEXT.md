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
The facts derived from a vacancy by rule, rather than published by the ATS: its cleaned and
normalized titles, whether it is an engineering role, its work mode, declared location,
seniority and skills. Derived data is never mixed into the vacancy itself, so rules can be
re-run at any time.
_Avoid_: enriched vacancy, parsed vacancy, processed vacancy

**Cleaned Title**:
A vacancy's title with what is noise in any title taken out: decoration, gender markers, and
the seniority words that name a level wherever they appear. Every vacancy in the corpus has
one, because it is what classification reads.
_Avoid_: normalized title, clean title, title slug

**Cleaning**:
The pass that gives every vacancy in the corpus a cleaned title. It runs before classification,
because a corpus whose duplicate titles have been collapsed is what makes a classifier buildable
by reading the most common ones.
_Avoid_: normalization, scrubbing, preprocessing, sanitizing

**Normalized Title**:
A cleaned title reduced further to its role-bearing words, with the seniority words that
need a reading of the job and the work mode taken out. Only engineering roles have one,
because only there is the vocabulary known well enough to tell a level from a job.
_Avoid_: canonical title, title slug

### The Corpus

**Corpus**:
Every company and vacancy that discovery, probing and sweeping have accumulated, taken as
one body of data rather than as rows. What the rules are run over, and what their measured
accuracy is a statement about.
_Avoid_: dataset, database, sample, data

**Snapshot**:
The corpus frozen at one stage, so that a rule re-run later is re-run over identical input.
Snapshots are what rules are developed against; live data is what production serves. There
are two of them — the raw corpus, and the corpus once it has been classified — and a
snapshot is never replaced, because an accuracy measured against one is only meaningful
while that input can be produced again. The passes in between are pure rules that re-run
over the raw corpus in seconds, so restoring it reproduces them for free.
_Avoid_: backup, dump, export, copy, fixture

### Classification

**Tech-Adjacent Role**:
A role whose hiring market is shaped by software work, whether or not the role writes
software. The product exists to serve these. The ones a software background alone opens are
engineering roles for this iteration's purposes; the rest wait for a later one.
_Avoid_: tech role, IT role, technical role

**Engineering Role**:
A vacancy a software background alone qualifies someone for: it writes code, or it reads and
operates on engineer-facing artifacts, or it is a role a software person could credibly be
hired into today without a new credential. Wider than the phrase usually carries, and
deliberately so — the product exists to find work a person can actually take, which is why
Scrum Master belongs and Product Manager does not. Whether a vacancy is one is the whole of what classification answers, and a vacancy that is
not one is out of scope. The criterion lives in `docs/engineering-role-criterion.md`; the
rules that approximate it are code.
_Avoid_: role family, category, discipline, job function, technical role

**Classification State**:
What classification answers about a vacancy: in, out, or unknown. Unknown is the default and
not a softer out — it says nothing earned a decision, and it carries the reason: the head is
known but the domain is not, a ruled phrase genuinely splits, or no rule reaches the title at
all. Which signal decided, title or body, is recorded alongside, so a wrong answer is
attributable to the rule that produced it.
_Avoid_: verdict, flag, is_engineering, category

**Function Head**:
The noun in a title that names what the role does — engineer, analyst, technician — as opposed
to the modifiers naming the domain it does it in. Extracted during cleaning and stored, because
it is the first thing classification reads and the list of known heads is where the loop's
coverage comes from.
_Avoid_: role noun, job noun, base title, head word

**Domain-Bound**:
Said of a function head whose modifier settles what the role is, because the function only
exists inside a domain: a civil engineer is not a software job. The opposite is domain-free,
where the function operates on information about a domain and the modifier settles nothing —
an audit analyst may work on audit software. Which one a head is, is an argument about
language and the corpus is allowed to win it: a head is reclassed on the evidence of samples,
against the criterion's own argument if they say so.
_Avoid_: domain-specific, vertical, contextual

**Yielding Head**:
A head that gives way to a head standing behind it, because it names a rank — lead, director,
manager, chief — or a department — support, operations, tech — rather than the work itself. A
Lead Analytics Engineer is an engineer and a Network Support Engineer is an engineer, so
classification reads past the first word to the head behind it. A never-engineering word behind a
yielding head does not take the head, because there it names who the work is done for rather than
what it does.
_Avoid_: rank head, seniority word, prefix head, title prefix

**Generic Head**:
A yielding head that names no work of its own even when nothing stands behind it — manager,
director, operations, support. With any
modifier and no software qualifier arguing back, a generic head is out: this is the criterion's
own default, that a modifier no one has classed is a market marker, applied where it cannot cause
a miss. Engineer and developer are deliberately not generic heads, because a bare C Engineer is a
software job and the same reading would decide it out.
_Avoid_: management head, empty head, weak head

**Marker**:
A modifier that names a domain outside software, and one of two things. A **discipline marker**
names a body of training a person is hired on — mechanical, fpga, nurse, chef — and is about
the candidate. A **market marker** names who the work is done for — finance, retail, marketing
— and is about the customer. The test between them is the credential, and it decides one thing:
a discipline marker outranks a software qualifier in the same title and a market marker does
not, because software is built for every market and for no credential.
_Avoid_: off-domain word, negative keyword, exclusion term, vertical

**Software Qualifier**:
A modifier that names the work as software — the thing a marker is weighed against. It earns
in under a head that can carry engineering work, and loses to a discipline marker in the same
title.
_Avoid_: tech keyword, positive keyword, signal word

**Ruling**:
A verdict fixed for one phrase, overriding every other rule. Rulings are what the word lists
cannot express; they are precise and they do not generalise, which is why they are an override
layer rather than the mechanism.
_Avoid_: exception, override, special case, hardcoded verdict

**Unruled**:
The unknown reason meaning no rule reaches this title, as against the reasons that say the
corpus itself is ambiguous. It measures the backlog rather than the world, so it is the one
share expected to fall every iteration, and the only unknown reason the exit is gated on: the
rest of the unknown pile is the corpus's, and the body pass is what resolves it.
_Avoid_: uncovered, unhandled, missing, not found

**Labeller**:
What produces the ground truth a classification state is scored against: a session that reads
titles and applies the criterion before it has read the rules the classifier runs on. Never
part of the pipeline — it exists only inside an iteration. It reads meaning in any language the
title is written in, and it does not override the criterion on a family it finds unsatisfying:
a disagreement with the criterion is a disagreement, not a label.
_Avoid_: annotator, judge, oracle, reviewer

**Iteration**:
One session's pass around the classification loop: label the sample the last session left,
score the rules against it, change them, re-classify, draw the next sample. Iterations
accumulate and their labels are never replaced, because an accuracy figure is a statement
about a specific sample of a specific corpus.
_Avoid_: round, batch, run, evaluation, cycle

**Work Mode**:
Where the work is performed, as declared by the vacancy: remote, hybrid or onsite.
Undeclared is its own answer and never means onsite.
_Avoid_: modality, workplace type, location type, arrangement

**Declared Location**:
The place a vacancy names for the work, as it wrote it, once its work mode has been taken
out. A remote vacancy that names a place is not thereby restricted to it, so the place it
names is never on its own a reason to rule it out.
_Avoid_: office, region, geo, country

**Seniority Level**:
The experience level a vacancy asks for, on one ordinal scale from entry to principal.
Where a vacancy names several, the lowest applies, because a vacancy publishes the minimum
it will accept. That choice belongs to the step that derives the final level, never to a
step that only gathers evidence for it.
_Avoid_: level, grade, rank, band, experience

**Title Seniority**:
Every level a vacancy's title names, kept as found. Evidence rather than an answer: the
title is one source among several, and collapsing it to a single level before the others
are read throws away what they would have been weighed against.
_Avoid_: seniority, level, title level

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
