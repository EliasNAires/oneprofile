---
name: classification-round
description: Run one iteration of the engineering-role classification loop — label the sample the last session left, score the classifier, change its rules, draw the next sample. Use when asked to run a classification round, measure the classifier, or advance issue #10.
---

# One iteration of the classification loop

Issue #10 is a loop, and **one session is one iteration**. You label what the last
session left you, you change the rules, and you leave a sample for the next session. Then you
stop. There is no second iteration in this session and nothing to ask approval for: the rules
are yours as long as they obey the cost limit in the criterion.

Two things only are not yours. The **criterion** — `docs/engineering-role-criterion.md` — is
the developer's, changed only in a grilling session, and when your rules disagree with it the
rules are wrong. And the **exit thresholds**, which ADR-0008 fixes.

## The order matters

Read the criterion and the sample **first**, label the sample, and only then open the
classifier's code. Backwards, you can work out what the classifier predicted for each title,
you will agree with it, and the numbers you report will be decorative. Write your labels to
disk before you read any rules.

## The context budget

**The session finishes at around 100k tokens.** An iteration reads a thousand titles, a
corpus of predictions and a classifier full of rule lists, and none of that belongs in this
session's context. Delegating only the blind labelling is not enough — that still lands near
180k. Hold to three habits:

- **The blind labelling goes to a subagent**, handed the criterion and the bare titles, and
  told explicitly not to read `src/`, `docs/measurements/`, or any earlier fixture. It writes
  the labels to disk and returns counts only.
- **The data never enters this session.** Scoring, pricing a candidate rule and drawing the
  next sample are done with scripts — `awk`, `grep`, `psql` — over files in the scratchpad.
  Read the rows that disagree, never the thousand rows, and never the accumulated fixtures.
- **Rules are tried on an offline harness**: `javac` over the classifier and its immediate
  dependencies plus a `main` that reclassifies the fixtures. Seconds a run against minutes of
  Maven, and it keeps the rule lists out of context — extract them to text files and `grep`
  them.

**The judgement stays here.** Which rule goes in and which is discarded is not delegated: a
subagent does the blind, mechanical work, not the deciding.

## The iteration

**1. Read the handoff.** `docs/measurements/` holds one file per iteration. The newest is
your input: it carries 1000 ids and cleaned titles, and the previous iteration's numbers.

**2. Label the sample.** From the criterion alone, over bare titles. `IN`, `OUT`, or
`UNKNOWN` with one of the three reasons. Never read a description body, never call a paid API
(ADR-0007). Write the labels to `src/test/resources/labels/` as a fixture, recording the date
and the criterion's git revision. Fixtures accumulate and are never regenerated: a session is
not reproducible, so the labels are the artifact, not the process that made them.

**3. Score.** Now open the previous implementation and the prediction map in the handoff, and
rejoin by id:

**A mistake is a row whose state differs from your label.** There are three states, so an `IN`
you labelled `UNKNOWN` is as wrong as an `IN` you labelled `OUT`. Three rates:

- **`OUT` stratum error** — the share you labelled `IN` or `UNKNOWN`.
- **`IN` stratum error** — the share you labelled `OUT` or `UNKNOWN`.
- **`UNKNOWN` stratum error** — the share you labelled `IN` or `OUT`. The unknown pile has to be
  a pile you could not decide either, or the loop is classifying its own ambiguity.

Agreement is on **state only**: a reason that differs from yours is not a mistake, because the
reason codes exist to tell #11 which question to ask. Report alongside, gated by nothing: the
corpus's unknown share split by reason, the count of `OUT`-stratum rows you labelled `IN`, and
the count of rows you left `UNKNOWN` that the classifier decided.

**4. Check the exit** against ADR-0008: the three rates and the `unruled` share, read once as
levels — there is no streak to hold. Iteration 12 is the last whatever the numbers say. If the
cap binds with the gate unmet, write the report, record the reason split, close #10, and hand
the remaining unknown pile to #11 as its input; the open criterion questions carry over as notes
on #11. Before closing, and only after the labels are written and scored, apply the criterion
decisions in the #10 comment "Criterion decisions for the close"
(https://github.com/EliasNAires/oneprofile/issues/10#issuecomment-5806582704), and carry its open
questions to #11.

Iterations 1–7 were scored under a narrower definition of a mistake, so read trends from
iteration 8 on.

**5. Change the rules.** The classifier lives in `src/main/java`, built test-first. Grow it
from where your labels and its answers disagreed. Heads are where coverage comes from: a head
generalises across every title that names it, and a ruling generalises across none, so reach
for the head list first and the ruling table last.

One change is forbidden outright: **a change that raises the count of `OUT`-stratum rows
labelled `IN` is not accepted**, whatever else it buys. A vacancy decided `OUT` reaches neither
#11's body pass nor a profile, so it is the one error nothing downstream recovers.

Every rule obeys the criterion's cost limit; beyond that the shape is yours.

When pricing a candidate rule over the accumulated fixtures, rows a fixture's header note marks
as no longer authoritative are evidence-free, not costs. Label rows are never rewritten.

**6. Re-classify.** Bring the dev database up (`docker compose up -d postgres`) and run the
classifier over the corpus snapshot (`scripts/restore-snapshot.sh` if it is not loaded).
Report the counts per state.

**7. Draw the next sample and hand off.** 1000 rows at random from the new predictions — 100
from `IN`, 500 from `OUT`, 400 from `UNKNOWN` — **excluding** every vacancy any previous
fixture already holds. Fresh means fresh: growing the rules from an iteration's misses and
re-measuring on the same rows is training on the test set.

Write one file, `docs/measurements/engineering-role-<date>.md`, holding: this iteration's
three numbers, what you changed and why, and the 1000 drawn rows as id and cleaned title,
**shuffled together** so the next session cannot tell the strata apart. Keep the `id →
prediction` map in the same file but in a clearly separated section the next session is told
not to read until step 3.

Then stop.
