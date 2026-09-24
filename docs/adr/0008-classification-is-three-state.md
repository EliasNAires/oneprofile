# Classification answers in three states, not two

Status: accepted

A vacancy is classified as **in**, **out**, or **unknown**, where unknown carries one of three
reasons: `domain_ambiguity`, the head names a function that exists identically outside software
and no modifier settles the domain; `scope_ambiguity`, a ruled phrase whose variants genuinely
split across the criterion; or `unruled`, the rules do not reach this title at all. The state and
the signal that produced it — title or body — are both stored.

This ADR is also the one place the classification loop's measurement and exit are fixed; every
other document points here.

## Considered options

- **A binary in-or-out**, which is what a token dictionary naturally produces. Rejected
  because it forces a guess on titles that genuinely do not say. `Engineer II` at an unnamed
  company, `Data Analyst`, `Support Engineer`, `Electrical Engineer`: each is an engineering
  role in some companies and not in others, and a binary classifier must be wrong about one
  group or the other.
- **Treating the undecidable as out**, the conservative binary. Rejected under ADR-0004: what
  is filtered out is invisible, and thousands of titles quietly discarded is exactly the silent
  recall loss that ADR-0004 was written to guard against.
- **Treating the undecidable as in**, letting the later passes sort it out. Rejected because
  every later pass — normalization, seniority, skills — is specified to assume the vocabulary
  of engineering work, and feeding it nurses and civil engineers is what makes its rules big.
- **Capping the size of the unknown pile**, as a fixed share of the corpus or as a required
  rate of decline. Rejected. Unknown is the default (ADR-0009), so a size cap gates nothing
  for many iterations; and a decline condition gates the loop's rate of change rather than
  the classifier — it is met by converging and equally by declining to make changes the sample
  has already shown were right.
- **Gating on the whole unknown share.** Rejected because most of the pile is
  `domain_ambiguity`, which #11 exists to resolve from the body; gating on it asks the title
  stage to solve the next stage's problem.

## Consequences

Unknown is a **queue, not an answer**. #11 resolves it from the description body by rule, and
the reason code says which question to ask there: a domain-ambiguous title needs the body
checked for domain markers, a scope-ambiguous one needs the criterion re-applied to the body.
Without the reason, the body pass would re-derive from scratch what the title pass already
knew.

`unruled` does not go to #11 at all. It says the title stage has no rule for this head, which
is a statement about our backlog rather than about the vacancy: the fix is a line added to a
list, not a description read. The `unruled` share is ours and must fall every iteration;
`domain_ambiguity` and `scope_ambiguity` are the corpus's and will plateau at whatever the world
actually is.

The third state is an **escape hatch that has to be capped**: a classifier that answers unknown
to everything has no false accepts and no misses. The cap is on the unknown pile's **honesty**
rather than its size — the pile has to be a pile the labeller could not decide either.

**Measurement.** Each iteration labels 1000 rows drawn from the classifier's predictions,
stratified by what it predicted: **100 from `IN`, 500 from `OUT`, 400 from `UNKNOWN`**,
excluding every vacancy an earlier fixture holds. A mistake is any row whose state differs
from the label, in either direction, so each stratum yields one error rate. Alongside, the
unknown share of the corpus is reported split by reason.

**Exit.** The loop exits when one iteration holds **all three per-stratum error rates at or
below 10%** and the **`unruled` share at or below 4% of the corpus**. It stops unconditionally
at **twelve sessions**, whatever the numbers say. The reasons other than `unruled` are reported
and not gated, because they are the corpus's rather than ours.

Downstream, three states means every consumer of classification chooses explicitly what to do
with unknown, rather than inheriting a default. The engineering subset is the in state alone;
unknown vacancies reach it only if #11 resolves them.
