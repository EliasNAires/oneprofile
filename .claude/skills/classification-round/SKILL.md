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
rules are wrong. And the **exit thresholds** below.

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
your input: it carries 1000 ids and cleaned titles, and the previous iteration's numbers. If
there is none, you are iteration 1 — skip to step 5.

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

**4. Check the exit.** The loop is done when one iteration holds **all three rates at or below
10%** and `unruled` **at or below 4%** of the corpus. There is no streak to hold: the rates are
levels, read once, and `unruled` is a corpus-wide census with no sampling error in it.

**The loop also stops at twelve sessions**, whatever the numbers say. Seven are spent, so
iteration 12 is the last. If the cap binds with the gate unmet, write the report, record the
reason split, close #10, and hand the remaining unknown pile to #11 as its input; the open
criterion questions carry over as notes on #11.

Iterations 1–7 were scored under a narrower definition of a mistake and are not comparable to
yours. Do not read a trend across iteration 8. Iteration 7 under the definition above is 7.33%,
35.00% and 62.67%.

**5. Change the rules.** The classifier lives in `src/main/java`, built test-first. Grow it
from where your labels and its answers disagreed. Heads are where coverage comes from: a head
generalises across every title that names it, and a ruling generalises across none, so reach
for the head list first and the ruling table last.

One change is forbidden outright: **a change that raises the count of `OUT`-stratum rows
labelled `IN` is not accepted**, whatever else it buys. A vacancy decided `OUT` reaches neither
#11's body pass nor a profile, so it is the one error nothing downstream recovers.

Every rule obeys the cost limit in the criterion — whole words, case-insensitive, cleaned
title only, no bodies, no network, full corpus pass under ten seconds. Beyond that the shape
is yours.

On iteration 1 there is nothing to grow from. Port `docs/engineering-role-seed-lists.md` into
the classifier, **delete that file**, and go on.

**6. Re-classify.** Bring the dev database up (`docker compose up -d postgres`) and run the
classifier over the corpus snapshot (`scripts/restore-snapshot.sh` if it is not loaded).
Report the counts per state.

**7. Draw the next sample and hand off.** 1000 rows at random from the new predictions — 100
from `IN`, 500 from `OUT`, 400 from `UNKNOWN` — **excluding** every vacancy any previous
fixture already holds. The `UNKNOWN` stratum carries a gate now, so it gets the rows: the
`OUT` stratum error has been passing by a wide margin and has band to spare.

## Iteration 8 carries a criterion revision

The criterion was rewritten on 2026-09-22 (ADR-0010) while iteration 8's sample sat undrawn-on.
Three things follow, for iteration 8 only:

- **The markers are unclassed.** `TitleClassification` holds about 200 off-domain markers under
  one rule the criterion has split in two. Do not sort all 200 by hand: a marker's class only
  has consequences when a software qualifier is present in the same title, so measure which
  markers ever co-occur with one, class those, price the result on the accumulated labels, and
  leave the rest market by default.
- **The rules being scored predate the criterion.** Score the three rates straight — no adjusted
  figure, no asterisk — and report alongside how many of the errors are attributable to the
  revision rather than to the rules, so iteration 9 does not re-solve what the revision already
  answered.
- **The older fixtures are partly superseded.** Each fixture in `src/test/resources/labels/`
  carries a header note naming the families it labelled under rules the criterion no longer
  holds. When pricing a candidate rule over the accumulated labels, those rows are evidence-free
  — not costs. Never rewrite a label row.

The twelve-session cap does not move for this.

**Iteration 8 is the exception.** Its sample was drawn by iteration 7 under the old split, 600
`OUT` / 300 `UNKNOWN` / 100 `IN`. Label it as handed and score the three rates on those stratum
sizes — redrawing it would mean running the classifier over the corpus before labelling, which
is exactly the ordering this skill exists to protect. The new split starts with the sample
iteration 8 hands to iteration 9. Fresh means fresh: growing the rules from an iteration's misses and
re-measuring on the same rows is training on the test set.

Write one file, `docs/measurements/engineering-role-<date>.md`, holding: this iteration's
three numbers, what you changed and why, and the 1000 drawn rows as id and cleaned title,
**shuffled together** so the next session cannot tell the strata apart. Keep the `id →
prediction` map in the same file but in a clearly separated section the next session is told
not to read until step 3.

Then stop.
