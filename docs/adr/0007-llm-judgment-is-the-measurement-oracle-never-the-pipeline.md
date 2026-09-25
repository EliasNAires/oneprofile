# LLM judgment labels the measurement samples, and never enters the pipeline

Status: accepted

The labelling that measures the classifier of #10 is produced by a Claude Code session
applying `docs/engineering-role-criterion.md`, not by a person reading each row. The labels it
produces are ground truth. No language model is called from the application, at any stage, for
any vacancy outside a measurement sample.

Two constraints bound this. **No API is paid for**: labelling runs inside a Claude Code
session, never through the Batch API or any other billed endpoint, and a design that needs
one is rejected rather than costed. And **the labeller is blind by ordering**: a session labels
the sample from the criterion alone, over bare shuffled titles, and writes its labels to disk
before it opens the classifier's code. A labeller that can see the prediction — or can derive
it from the rules that produced it — agrees with it, and the resulting figure measures nothing.
Since the same session goes on to change those rules, the sequence is the whole mechanism;
there is nothing else keeping it honest.

The labeller reads **the whole criterion, rulings included**, because a phrase the rulings fix
is read rather than derived, and hiding them tests recall rather than clarity.

The labeller reads **titles only**. Ground truth therefore answers "is this title readable as
an engineering role", not "is this job one" — which is the right question to score a
title-stage classifier against, and is coherent only because the third state (ADR-0008)
exists to absorb the titles that do not say. Eligibility labelling (#12) needs description
bodies and is not covered by this decision.

## Considered options

- **Hand labelling by the developer.** Rejected as the binding cost: the loop in #10 needs a
  fresh sample every iteration, and 1000 labels per iteration paid in human attention is what
  stops the loop from being run often enough to converge.
- **The Anthropic Batch API over sampled rows**, roughly a dollar an iteration and
  reproducible by re-running a committed script. Rejected outright on the no-paid-API rule.
- **A subagent per description body**, which would let the labeller read more than the title.
  Rejected on rate limits and cost in sessions: resolving the ambiguous pile that way is tens
  of thousands of sessions per full run, and it would put a language model on the path every
  vacancy travels, which is the thing ADR-0005 exists to prevent.
- **Certifying the labeller with a calibration gate**: fifty adversarial titles, hand-labelled
  by the developer, with a 95%-agreement gate in front of every iteration. Rejected: it spends
  the developer's attention on titles he cannot confidently label, to certify a reader the
  loop's own numbers already test — a labeller that reads the criterion badly produces a bad
  error rate, which is the thing being measured anyway. Its two fixtures,
  `docs/measurements/engineering-role-2026-09-20.tsv` and
  `engineering-role-boundary-2026-09-21.tsv`, are kept as history and read by nothing.

## Consequences

The labels are **not reproducible by re-running a script**. A session given the same titles
may not return identical answers, so the committed fixture is the artifact, not the process
that made it — which is why iterations accumulate and are never regenerated. Each fixture
records the criterion revision it was produced under, so a label can be read against the
criterion that was in force when it was made.
