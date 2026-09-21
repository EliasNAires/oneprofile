# Classification answers in three states, not two

Status: accepted

A vacancy is classified as **in**, **out**, or **unknown**, where unknown carries a reason:
`domain_ambiguity`, the title's function word exists identically outside software, or
`scope_ambiguity`, titles of this shape split across the criterion. The state and the signal
that produced it — title or body — are both stored.

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
checked for domain markers, a scope-ambiguous one needs the membership test re-run. Without
the reason, the body pass would re-derive from scratch what the title pass already knew.

The third state is also an **escape hatch that has to be capped**, which is the price of
having it. A classifier that answers unknown to everything has no false accepts and no misses
and would satisfy any accuracy threshold; the loop in #10 therefore exits only when the
unknown share of the corpus is at or below 10%, alongside its error thresholds. Each round
also labels a slice of the unknown pile — not to gate on it, but because a pile that is mostly
in means recall is worse than the miss rate reports, and nothing else would reveal that.

Downstream, three states means every consumer of classification chooses explicitly what to do
with unknown, rather than inheriting a default. In this iteration the engineering subset is the
in state alone; unknown vacancies reach it only if #11 resolves them.
