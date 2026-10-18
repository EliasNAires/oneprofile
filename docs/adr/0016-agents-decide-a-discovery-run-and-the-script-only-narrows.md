# Agents decide a skill discovery run, and the script only narrows the corpus

Status: accepted; supersedes ADR-0013's steps 2 and 3 of a run and its precision and yield gates,
and ADR-0015's rule and Jev check setting a key's kind

ADR-0013 has Jev decide each candidate, keep or drop, a Haiku agent draft the row of each keep,
and a session review the rows. The first run (#55, `classified-2026-10-06`) showed where that
breaks. The script cut 1.7M segments (202 MB) to 2,888 candidates, about 1 MB with three segments
each, roughly a thousandth. Jev kept 1,475 of them, and the session review dropped 451 of those
keeps (31%) and changed 121 more. Jev reads one name and three segments at a time, so it cannot
see what most of those errors are about: that two candidates are one skill, that a bare form is a
skill already held, that a name is a fragment of a longer one, or that a candidate glues two skills
together. An agent reading a batch of candidates beside the skills already held can. Decided by
Elias on 2026-10-18.

## A run

1. **The script narrows.** `scripts/skill_discovery.py` reads the snapshot and proposes
   candidates, as now. Its job is recall: let every skill through, and accept the noise that comes
   with it. Noise costs agent tokens and nothing else.
2. **Agents decide and draft in one pass.** Each agent reads `docs/skill-criterion.md`, one batch
   of candidates with their spellings and segments, the skills of `skills.tsv` each candidate may be
   (`similar`), and each candidate's earlier decisions in the record. For each candidate it writes
   one of:
   - a **drop**, with the criterion's reason;
   - a **spelling** of a skill held, by its id;
   - a **new skill**: id, canonical name, category, aliases, and which of its keys are context keys;
   - a **split**: a candidate naming two skills (Spring Hibernate) is a drop, and each part not held
     is a new skill;
   - a **join**: candidates that are pieces of one name (Time-Sensitive, Time-Sensitive
     Networking) are one skill, under the whole name.

   Any key the agent writes, on a new skill or as a spelling, it marks plain or context. Once
   `skills.tsv` has a parent column (#15), the agent fills it on the rows it writes.
3. **Missing skills join the pool.** The names that recall scoring (ADR-0015) finds kept but not
   proposed are read by the agents beside the candidates, as candidates of their own.
4. **A session reviews the whole draft.** It reads it across batches, for one skill drafted
   twice, keys that collide, and decisions the criterion does not support. Then it appends the rows
   to `skills.tsv` and every decision, drops included, to the decision record.

Agents curating the taxonomy keep to ADR-0007: it is offline work done once per snapshot, and no
model is on the path a vacancy travels. Extraction stays rules (ADR-0005).

## Context keys are only stored

The agent marks a key context when ads also use it as an ordinary word or a letter (Go, Spring,
Swift, C), and the mark is stored in the context column of `skills.tsv`. That replaces ADR-0015's
rule (a word list and a lower-case share) and its Jev check, and a run no longer calls
`scripts/check-skill-keys`. Discovery only records which keys need context. How a context key is
matched, and whether plain keys misfire, are decided by a process not yet designed.

## What is measured

- **Recall stays the script's gate**, scored as ADR-0015 says, with Jev judging pieces. A name the
  script never proposes is lost for good, which is why recall is the one thing worth measuring.
- **Precision and yield are dropped as gates.** They existed to keep Jev's input from being mostly
  noise and to stop the script cutting valid names to raise precision. With agents reading the
  pool, the only limit is size: a run's pool has to fit the agent batches of one usage window. The
  first run's 2,888 candidates took 25 batches of 60.
- **The script changes only when recall fails**, or the pool outgrows that limit. The rounds of
  #54 tuned it against precision and yield, and are not reopened.

## Jev's part

Jev leaves the run: it no longer decides candidates or picks categories. It stays recall's judge
of pieces, a closed question it answers well (ADR-0015), under the same $4 stop.

## Considered

- **Keep Jev and improve the review.** The review already carried the run. It spent most of its
  effort undoing Jev, and it could not add what Jev never saw.
- **Agents read the corpus without the script.** 202 MB of segments is about 50M tokens, far
  beyond a usage window. The script's thousandfold cut is what makes agents affordable.
