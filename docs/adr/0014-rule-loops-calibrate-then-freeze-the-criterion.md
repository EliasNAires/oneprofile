# Rule loops calibrate the criterion, then freeze it, and gate on adjudication

Status: accepted

A rule loop scores rules against a labeller reading a criterion. Until 2026-10-07 the reviewer of
each round could revise the criterion in the same round (#57), and in #11 and #54 it did so in
nearly every one. Three things followed. Feedback was ranked from labels made under the revision
the review had just replaced, so it went stale on arrival, or the round was scored twice. Rounds
were scored under different revisions, so a change in the numbers mixed rule progress with
criterion drift. And labeller errors sat inside the gates: in #54 about 70% of Jev's recall
misses broke the criterion, and in #11 round 1 they put a floor of about 17% under the `IN`
stratum's error, so the exit could be blocked or passed by the labeller rather than the rules.
Each criterion revision also cost the reviewer context: re-reading every disagreeing row, revising,
re-scoring.

So a rule loop has two phases.

- **Calibration** revises the criterion and checks the labeller against a **gold set**: rows an
  agent drafts labels for and Elias corrects. Part of the gold set is held out; the session that
  revises the criterion never reads it, since a criterion revised against rows it was checked on
  is fitted to them. The revision that calibration ends on is **frozen**.
- **Rounds** score under the frozen revision only. A reviewer who finds the criterion wrong writes
  a **criterion proposal** into the round comment and leaves the criterion as it is. Proposals
  are taken up by the next calibration, which the reviewer calls when one would move a gate
  across its line.

And the gates are computed on **adjudication**: a blind subagent reads each disagreeing row with
the criterion, never the rules, and decides which of the two answers, unattributed, the
criterion supports. The labeller's raw figure is reported beside the adjudicated one, and the
adjudicator's agreement with the gold set says how far that can be trusted. The reviewer reads
the adjudication's tallies, not the rows.

**Agreement** with the gold set measures the criterion's objectivity more than the labeller
(ADR-0012). It is recorded beside the gates, not gated on, and a gate set much tighter than it
cannot tell rules apart from reading noise.

## Considered options

- **Re-score every round whose review revises the criterion.** Keeps Feedback fresh, but leaves
  rounds incomparable, costs labeller spend and reviewer context every round, and does nothing
  about labeller errors inside the gates.
- **Keep revising the criterion until the labeller gets it right.** #54 revision 2 spelled out,
  with examples, what is not a recall miss, and Jev still counted those very examples. Some
  errors are the question's shape, not the criterion's wording, and are fixed by asking a
  different question (#58).
- **Gate on the reviewer's own adjudication**, as #54 rounds 0 and 1 reported beside Jev's
  figure. The reviewer has read the rules, so its reading is not blind, and it reads every row in
  its own context.

## Consequences

- A loop's ADR names its gold set and its adjudication question, and records each calibration's
  agreement beside the gates.
- ADR-0012's labeller check and ADR-0013's use of #52 become the first calibration of their loops;
  their figures stand until a gold set is checked.
