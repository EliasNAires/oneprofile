# Domain docs

One context: `CONTEXT.md` (the glossary) and `docs/adr/` at the repo root.

- Write domain concepts (issue titles, test names, commit messages, proposals) with the
  glossary's term, never one it lists under _Avoid_. A concept the glossary lacks is a gap:
  name it in your output so it can be added.
- When your work contradicts an ADR, say so explicitly ("contradicts ADR-0009, because …").
  Never override an ADR silently.

## The ADRs by area

Read the ADRs for the area you're working in. Where a later ADR overrides part of an earlier one,
the later ADR wins; the overridden parts are marked below.

**Corpus**
- **0001**: discovery binary-searches Common Crawl's cluster index over HTTPS range requests.
  It starts reading one block before the prefix, keeps only `/warc/` captures, and builds SURT
  keys with `webarchive-commons`.
- **0002**: Ashby is the second ATS. *Overridden by 0022:* Workable being next, and Lever being
  undiscoverable.
- **0022**: the next ATSs are chosen by title `IN` vacancies per live board: Lever, then
  Workable, then Workday only if #70's gap is still open. Each adds its EU host. *Amended:*
  Greenhouse's EU boards are read through its one API host; every company keeps its **region**
  (`US`/`EU`) from discovery, which picks Lever's API host (#79).
- **0023**: discovery reads two archives, Common Crawl and the Wayback Machine. Common Crawl to
  the 12 newest crawls that hold the ATS's pages, backfilled once, then a run reads only a new
  crawl. Wayback is read whole on every run, by resume key, and a run keeps the slugs it lacks.
  *Amended:* an ATS whose boards are subdomains (Workday) is asked for by domain (#81).
- **0025**: an ATS's pace is measured to its ceiling before it is probed. The research comes
  first: its sources, what a refusal looks like, a ramp of 1 to 32 at a time, whether each region
  host has its own limit, and the link. The settings are half the ceiling, for requests open at
  once and for starts a second, per region where an ATS has one API per region. Each refusal
  halves them for the rest of the run, and gateway errors do not. The settings are checked at
  full scale. Written in one `docs/measurements/` file; the tools are `scripts/pace-*`.
  *Amended:* Workday is paced per request at 16 a second and 20 open, by one throttle its probes
  and sweeps share, and a Cloudflare challenge is waited out from 1 minute (#81).
- **0021**: each ATS's vacancy is a class of its own over the one `vacancy` table, keeping what
  that ATS publishes as it publishes it; `Vacancy` holds what any ATS would be expected to
  publish. Facts are derived in passes, never in the sweep. Overrides PRD §7's nullable columns.
  *Amended:* `Company` gains its region (0022). Workday's fields share Lever's columns where they
  mean the same, and its time type has its own; its sweep runs the title rules to choose which
  descriptions to ask for, storing nothing they decide (#81).
- **0006**: rules are developed against **snapshots** on the dev machine, never production. Two
  are kept, raw and after classification. A re-sweep cuts a new pair and archives the old one.
- **0011**: history is kept as **series** (numbers, not vacancies), recorded from **stability**
  onwards. Stability's bar, amended: 100k engineering vacancies live in one sweep (was one million
  vacancies) and 30 consecutive days without manual intervention (was four weekly cycles).

**Derivation**
- **0005**: every derived fact comes from a rule. No LLM is called anywhere in the pipeline.
- **0004**: eligibility is a hard filter, so recall is the binding constraint. A declared
  location is evidence for eligibility, never against it, and the country set is stored.
  *Overridden by 0019* for the Finder, which shows unknown eligibility; the hard filter holds for
  matches.
- **0003**: skills come from a curated taxonomy (O\*NET, Linguist, Wikidata), not Lightcast.
  Its provenance and licences: `docs/taxonomy.md`.
- **0024**: a vacancy's role is read from its title: the title is broken down into its
  normalized title and looked up in a curated, flat role list. No match means no role, never a
  guess; filling those from other fields is after the MVP (#87). Done at 70% of the engineering
  subset.

**Classification**
- **0008**: three states, and three unknown reasons (`domain_ambiguity`, `scope_ambiguity`,
  `unruled`). Also the title loop's measurement and exit. That loop closed with #35, and since
  then `unruled` vacancies are discarded.
- **0009**: unknown is the default. A title is read as a function head with modifiers, and a
  yielding head gives way to the head behind it.
- **0010**: discipline markers beat a software qualifier; market markers do not.

**Measurement and rule loops**
- **0007**: an LLM labels the measurement samples and never enters the pipeline. No paid API is
  allowed except Jev (TypeSafe), under a $5 account limit that every Jev script stops at $4 of.
  Its title-loop labeller (a Claude Code session, kept blind by the order it works in) is
  history: 0012 and 0013 name the labellers of later loops, and 0014 keeps them blind by
  splitting each round into two sessions.
- **0012**: the body pass (#11) is scored against Jev: the pile, the budget, the gates and the
  ten-round cap. *Overridden by 0014:* the reviewer revising the criterion (§ The loop), and
  the labeller check, which is now the loop's first calibration. *Overridden by 0017:* the
  rules moving onto lemmas; they read extraction's skills instead.
- **0013**: skill discovery (#41, looped in #54) is a script whose candidates Jev filters,
  against the decision record `src/main/resources/taxonomy/decisions.tsv`. Its gates: recall,
  precision and yield. *Overridden by 0015:* how recall is scored, and every key being safe.
  *Overridden by #62:* the record filtering candidates and deciding drawn ones.
  *Overridden by 0016:* Jev deciding candidates, the Haiku drafter, and the precision and yield
  gates. *Overridden by #64:* a drop being final; a run decides a held name again, the record
  being evidence.
- **0014**: every rule loop calibrates its criterion, freezes it, and gates on adjudication.
  How a loop is worked: `docs/agents/rule-loop.md`.
- **0015**: recall is scored one name at a time: code proposes and filters a segment's name-like
  pieces, Jev judges each left in its segment with the precision question, and the ordinary list
  grows from its drops. Each key is plain or context, set by a rule Jev checks. Lands in #54's
  first calibration, with a 300-segment gold set. *Overridden by #60:* the ordinary list, which
  the stoplist (`stopwords.tsv`) replaces. *Overridden by #61:* the filter's use of the decision
  record. *Overridden by 0016:* the rule and Jev check setting a key's kind.
- **0016**: agents decide a skill discovery run in one pass (drop, spelling, new skill, split,
  join), with recall's missed names in their pool, and a session reviews the whole draft. The
  script only narrows the corpus, gated on recall alone; precision and yield are gone. Jev stays
  recall's judge of pieces. The agent marks each key plain or context; how context keys are
  matched is left to a later process.
- **0017**: the pipeline's order: title classification, skill extraction over the title's in and
  the pile, the body pass reading those skills, then the other extractions over the engineering
  subset. Descriptions are not lemmatized (#47); word forms stay in the rules.
- **0018**: extraction stores each (vacancy, skill) pair with the segments it was found in and
  whether the title names it, and a run leaves no row outside its scope. It reads the title and
  the segments that are not boilerplate. The longest key wins, a child does not add its parent,
  and a context key counts only in its own casing and where a plain key confirms it. Its
  precision was measured once (#65): 0.945, judged good enough without a rule loop; only the
  easy wins listed on #65 are taken.

**The public site**
- **0019**: the Finder derives eligibility for the visitor's country from the country set, hides
  the excluded and shows the eligible and the unknown, with a toggle for the eligible alone.
- **0020**: one repository holding two Spring Boot apps, the Maven modules `pipeline` and `api`,
  and the site in `site/`.
  The pipeline pushes a read schema into Postgres on a Hetzner VPS in Ashburn; the read app beside
  it serves the site, and its endpoints are the contract. The site is an Astro site on Cloudflare
  Pages. nginx (#18) and the tunnel (#32) are not built.
