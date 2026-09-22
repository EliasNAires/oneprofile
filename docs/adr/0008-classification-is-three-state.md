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
The cap is therefore on the unknown pile's **honesty** rather than on its size: the pile has to
be a pile the labeller could not decide either. **The loop exits when all three per-stratum
error rates are at or below 10%** — where a mistake is any row whose state differs from the
labeller's — and the `unruled` share is at or below 4% of the corpus. See #10 for the numbers
and the sample split; a twelve-session cap stops the loop whatever they say.

The reasons are still reported and are not gated, because they tell the next reader why a title
landed where it did and they tell #11 which question to ask of the body. `unruled` is the one
exception, because it is the only reason that is ours rather than the corpus's.

Each iteration labels a slice of the unknown pile — 400 of the 1000 drawn rows, now the second
largest stratum — and that slice is what the third rate is measured on.

Downstream, three states means every consumer of classification chooses explicitly what to do
with unknown, rather than inheriting a default. In this iteration the engineering subset is the
in state alone; unknown vacancies reach it only if #11 resolves them.

## Amended by ADR-0009

The third reason code and the restated cap above were added when ADR-0009 made unknown the
default. The three-state decision itself is unchanged.

## Amended after iteration 7

The exit above was originally a direction rather than a level: the unknown share had to fall by
less than a percentage point, alongside a miss rate and a false accept rate held for two
consecutive iterations. Seven iterations showed what that measured. Both quality rates passed
from iteration 4 onwards, and the direction condition restarted four times — met either by
converging or by declining to make changes the sample had already shown were right. It gated the
loop's rate of change, not the classifier.

Two further findings from the same seven iterations are behind the restatement. The unknown
pile is 17% `domain_ambiguity` against 5.82% `unruled`, and `domain_ambiguity` is what #11 exists
to resolve, so gating on the total asked the title stage to solve the next stage's problem. And
the old definition of a mistake was one-directional per stratum, which left the classifier free
to decide titles the labeller could not — a row the labeller left `UNKNOWN` and the classifier
called `OUT` was counted by nothing.
