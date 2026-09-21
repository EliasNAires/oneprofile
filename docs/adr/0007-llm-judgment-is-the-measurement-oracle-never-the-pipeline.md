# LLM judgment labels the measurement samples, and never enters the pipeline

Status: accepted

The hand labelling that ADR-0005 and PRD §11 require is produced by a Claude Code session
applying a written criterion, not by a person reading each row. The labels it produces are
ground truth for measuring the classifier of #10. No language model is called from the
application, at any stage, for any vacancy outside a measurement sample.

Two constraints bound this. **No API is paid for**: labelling runs inside a Claude Code
session, never through the Batch API or any other billed endpoint, and a design that needs
one is rejected rather than costed. And **the labeller is blind by ordering**: a session labels
the sample from the criterion alone, over bare shuffled titles, and writes its labels to disk
before it opens the classifier's code. A labeller that can see the prediction — or can derive
it from the rules that produced it — agrees with it, and the resulting figure measures nothing.
Since the same session goes on to change those rules, the sequence is the whole mechanism;
there is nothing else keeping it honest.

## Considered options

- **Hand labelling by the developer**, which is what the labelling issue originally specified. Rejected as the
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
that made it — which is why iterations accumulate and are never regenerated. Each fixture
records the criterion version it was produced under, so a label can be read against the rules
that were in force when it was made.

The labeller is trusted rather than verified per row, and an attempt was made to certify it
instead: a **calibration set** of fifty adversarial titles, hand-labelled, with a 95%-agreement
gate in front of every round. That gate is **abandoned**. It cost the developer an evening of
labelling titles he could not confidently label, to certify a reader the loop's own numbers
already test every iteration — a labeller that reads the criterion badly produces a bad miss
rate, which is the thing being measured anyway. The two fixtures it produced,
`src/test/resources/calibration/engineering-role-2026-09-20.tsv` and
`engineering-role-boundary-2026-09-21.tsv`, are kept as history and read by nothing.

Its one run, on 2026-09-20, scored 78% and is what produced ADR-0009; that finding rests on its
own evidence and is unaffected by dropping the gate. Two lessons from it survive as standing
rules rather than as calibration procedure. **The labeller reads the whole criterion, rulings
included**, because that is what it reads in production, and hiding the table tests recall
rather than clarity. And **a judgement a blind reader cannot execute does not belong in the
procedure**, which is why the criterion now separates its mechanical steps from the reasoning
behind a ruling.

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
