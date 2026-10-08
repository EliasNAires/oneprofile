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
- **0002**: Ashby is the second ATS. Workable is the next one if the eligible pool is too small.
- **0006**: rules are developed against **snapshots** on the dev machine, never production. Two
  are kept, raw and after classification. A re-sweep cuts a new pair and archives the old one.
- **0011**: history is kept as **series** (numbers, not vacancies), recorded from **stability**
  onwards.

**Derivation**
- **0005**: every derived fact comes from a rule. No LLM is called anywhere in the pipeline.
- **0004**: eligibility is a hard filter, so recall is the binding constraint. A declared
  location is evidence for eligibility, never against it, and the country set is stored.
- **0003**: skills come from a curated taxonomy (O\*NET, Linguist, Wikidata), not Lightcast.
  Its provenance and licences: `docs/taxonomy.md`.

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
  the labeller check, which is now the loop's first calibration.
- **0013**: skill discovery (#41, looped in #54) is a script whose candidates Jev filters,
  against the decision record `src/main/resources/taxonomy/decisions.tsv`. Its gates: recall,
  precision and yield. *Overridden by 0015:* how recall is scored, and every key being safe.
  *Overridden by #62:* the record filtering candidates and deciding drawn ones.
- **0014**: every rule loop calibrates its criterion, freezes it, and gates on adjudication.
  How a loop is worked: `docs/agents/rule-loop.md`.
- **0015**: recall is scored one name at a time: code proposes and filters a segment's name-like
  pieces, Jev judges each left in its segment with the precision question, and the ordinary list
  grows from its drops. Each key is plain or context, set by a rule Jev checks. Lands in #54's
  first calibration, with a 300-segment gold set. *Overridden by #60:* the ordinary list, which
  the stoplist (`stopwords.tsv`) replaces. *Overridden by #61:* the filter's use of the decision
  record.
