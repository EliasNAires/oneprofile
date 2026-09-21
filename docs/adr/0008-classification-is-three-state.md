# Classification answers in three states, not two

Status: accepted

A vacancy is classified as **in**, **out**, or **unknown**, where unknown carries one of three
reasons: `domain_ambiguity`, the head names a function that exists identically outside software
and no modifier settles the domain; `scope_ambiguity`, a ruled phrase whose variants genuinely
split across the criterion; or `unruled`, the rules do not reach this title at all. The state and
the signal that produced it — title or body — are both stored.

## Considered options

- **A binary in-or-out**, which is what PRD §5.2 originally specified and what a token
  dictionary naturally produces. Rejected because it forces a guess on titles that genuinely
  do not say. `Engineer II` at an unnamed company, `Data Analyst`, `Support Engineer`,
  `Electrical Engineer`: each is an engineering role in some companies and not in others, and
  a binary classifier must be wrong about one group or the other. Roughly 10% of the corpus is
  in this position, measured over the raw corpus with the word lists in
  `docs/engineering-role-criterion.md`.
- **Treating the undecidable as out**, the conservative binary. Rejected under ADR-0004: what
  is filtered out is invisible, and 18,000 titles quietly discarded is exactly the silent
  recall loss that ADR-0004 was written to guard against.
- **Treating the undecidable as in**, letting the later passes sort it out. Rejected because
  every later pass — normalization, seniority, skills — is specified to assume the vocabulary
  of engineering work, and feeding it nurses and civil engineers is what makes its rules big.

## Consequences

Unknown is a **queue, not an answer**. #11 resolves it from the description body by rule, and
the reason code says which question to ask there: a domain-ambiguous title needs the body
checked for domain markers, a scope-ambiguous one needs the criterion re-applied to the body.
Without the reason, the body pass would re-derive from scratch what the title pass already
knew.

`unruled` is a fourth thing again, and it does not go to #11 at all. It says the title stage has
no rule for this head, which is a statement about our backlog rather than about the vacancy: the
fix is a line added to a list, not a description read. Separating it from `scope_ambiguity` is
what makes the two numbers mean different things. The `unruled` share is ours and must fall every
iteration; `domain_ambiguity` and `scope_ambiguity` are the corpus's and will plateau at whatever the
world actually is.

The third state is also an **escape hatch that has to be capped**, which is the price of
having it. A classifier that answers unknown to everything has no false accepts and no misses
and would satisfy any accuracy threshold. The cap was originally a fixed 10% of the corpus.
ADR-0009 makes unknown the default, so the classifier now starts above half the corpus and a
fixed number would gate nothing for many rounds; the cap is therefore restated as a direction.
**The loop exits when the unknown share stops falling** — when it drops by less than a
percentage point from the previous iteration — alongside a miss rate at or below 2% and a
false accept rate at or below 10%, each held for two consecutive iterations.

The gate is the **total** unknown share, not the `unruled` share alone. The reasons still
exist and are still reported, because they tell the next reader why a title landed where it
did and they tell #11 which question to ask of the body; they are simply not what the exit is
measured on. One number is what a person checks between sessions, and a fall in the total can
only come from a fall in one of its parts.

Each iteration also labels a slice of the unknown pile — 300 of the 1000 drawn rows, the
largest stratum after the rejected one. Not to gate on it, but because a pile that is mostly
in means recall is worse than the miss rate reports, and nothing else would reveal that.

Downstream, three states means every consumer of classification chooses explicitly what to do
with unknown, rather than inheriting a default. In this iteration the engineering subset is the
in state alone; unknown vacancies reach it only if #11 resolves them.

## Amended by ADR-0009

The third reason code and the restated cap above were added when ADR-0009 made unknown the
default. The three-state decision itself is unchanged.
