# LLM judgment labels the measurement samples, and never enters the pipeline

Status: accepted

The hand labelling that ADR-0005 and PRD §11 require is produced by a Claude Code session
applying a written criterion, not by a person reading each row. The labels it produces are
ground truth for measuring the classifier of #10. No language model is called from the
application, at any stage, for any vacancy outside a measurement sample.

Two constraints bound this. **No API is paid for**: labelling runs inside a Claude Code
session, never through the Batch API or any other billed endpoint, and a design that needs
one is rejected rather than costed. And **the labeller is blind**: it is handed bare titles,
shuffled, with the classifier's prediction stripped, because a labeller that can see the
prediction agrees with it and the resulting agreement figure measures nothing.

## Considered options

- **Hand labelling by the developer**, which is what #9 originally specified. Rejected as the
  binding cost: the loop in #10 needs a fresh sample every round, several rounds are
  expected, and 300 labels per round paid in an evening of human attention is what stops the
  loop from being run often enough to converge.
- **The Anthropic Batch API over sampled rows**, roughly a dollar a round and reproducible by
  re-running a committed script. Rejected outright on the no-paid-API rule.
- **A subagent per description body**, which would let the labeller read more than the title.
  Rejected on rate limits and cost in sessions: resolving the ambiguous pile that way is tens
  of thousands of sessions per full run, and it would put a language model on the path every
  vacancy travels, which is the thing ADR-0005 exists to prevent.

## Consequences

The labels are **not reproducible by re-running a script**. A session given the same titles
may not return identical answers, so the committed fixture is the artifact, not the process
that made it — which is why rounds accumulate and are never regenerated. Each fixture
records the criterion version it was produced under, so a label can be read against the rules
that were in force when it was made.

Because the labeller is trusted rather than verified per row, it is **calibrated**: 50
adversarial titles labelled by hand, once, checked against the ruling table in
`docs/engineering-role-criterion.md`. Agreement below 95% means the criterion is not written
clearly enough to be followed, and the criterion is fixed before any round runs. The
calibration set is held out of every measurement sample. Calibration is re-run whenever the
criterion changes.

The labeller reads **titles only**. Ground truth therefore answers "is this title readable as
an engineering role", not "is this job one" — which is the right question to score a
title-stage classifier against, and is coherent only because the third state exists to absorb
the titles that do not say. Eligibility labelling (#12) needs description bodies and is not
covered by this decision.

## Relation to ADR-0005

ADR-0005 stands unchanged: every derived fact in the pipeline is produced by pure functions
over text. Its sentence "no language model is called anywhere in the pipeline" is narrowed
here to what it always meant — the pipeline, not the measurement harness that scores it. The
upgrade path it describes, adopting an LLM pass on measured evidence that the rules have
plateaued, is unaffected by this decision and still requires the paid-API rule to be revisited.
