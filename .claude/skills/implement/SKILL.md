---
name: implement
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

- **miss rate** — of the `OUT` stratum, the share you labelled `IN`.
- **false accept rate** — of the `IN` stratum, the share you labelled `OUT`.
- **unknown share** — `UNKNOWN` as a share of the whole corpus, not of the sample. Report the
  split by reason alongside, but the gate is the total.

Also say what the `UNKNOWN` stratum turned out to be. It does not gate anything, but a pile
that is mostly `IN` means recall is worse than the miss rate says.

**4. Check the exit.** The loop is done when **two consecutive iterations** hold a miss rate
at or below **2%**, a false accept rate at or below **10%**, and an unknown share that fell by
less than **1 percentage point** from the iteration before. If this iteration is the second
such, say so plainly and stop — the remaining work is only to close #10.

**5. Change the rules.** The classifier lives in `src/main/java`, built test-first. Grow it
from where your labels and its answers disagreed. Heads are where coverage comes from: a head
generalises across every title that names it, and a ruling generalises across none, so reach
for the head list first and the ruling table last.

Every rule obeys the cost limit in the criterion — whole words, case-insensitive, cleaned
title only, no bodies, no network, full corpus pass under ten seconds. Beyond that the shape
is yours.

On iteration 1 there is nothing to grow from. Port `docs/engineering-role-seed-lists.md` into
the classifier, **delete that file**, and go on.

**6. Re-classify.** Bring the dev database up (`docker compose up -d postgres`) and run the
classifier over the corpus snapshot (`scripts/restore-snapshot.sh` if it is not loaded).
Report the counts per state.

**7. Draw the next sample and hand off.** 1000 rows at random from the new predictions — 100
from `IN`, 600 from `OUT`, 300 from `UNKNOWN` — **excluding** every vacancy any previous
fixture already holds. Fresh means fresh: growing the rules from an iteration's misses and
re-measuring on the same rows is training on the test set.

Write one file, `docs/measurements/engineering-role-<date>.md`, holding: this iteration's
three numbers, what you changed and why, and the 1000 drawn rows as id and cleaned title,
**shuffled together** so the next session cannot tell the strata apart. Keep the `id →
prediction` map in the same file but in a clearly separated section the next session is told
not to read until step 3.

Then stop.
