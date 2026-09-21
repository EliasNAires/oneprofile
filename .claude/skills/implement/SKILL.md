---
name: implement
description: Run one round of the engineering-role classification loop — classify, draw a blind stratified sample, label it, report the three numbers, propose a dictionary diff. Use when asked to run a classification round, measure the classifier, or advance issues #10 and #9.
---

# One classification round

Issues #10 and #9 are a loop: classify the corpus, label a fresh sample blind, measure, grow
the dictionary, re-classify. This skill runs **exactly one round** and stops for approval. It
never runs two.

Read `docs/engineering-role-criterion.md` first. It is the criterion, and this skill assumes
its procedure, its three word lists and its ruling table.

## Before the first round

The labeller must be calibrated, per ADR-0007. If no calibration fixture exists, stop and say
so: the developer labels 50 adversarial titles by hand, they are checked against the ruling
table, and agreement below 95% means the criterion is rewritten before any round runs. Don't
run a round against an uncalibrated labeller and don't calibrate it yourself — the whole point
of the calibration set is that a human made it.

## The round

**1. Classify.** Bring the dev database up (`docker compose up -d postgres`) and run the
classifier over the corpus. Report the counts per state and the unknown share.

**2. Draw the sample.** 300 from `IN`, 600 from `OUT`, 100 from `UNKNOWN`, at random,
**excluding** every vacancy in a previous round's fixture and every vacancy in the
calibration set. Fresh means fresh: a vacancy labelled once is never drawn again, because
growing the dictionary from a round's misses and re-measuring on the same rows is training on
the test set.

**3. Blind it.** Keep the `vacancy id → prediction` map to yourself. Write the 1000 rows out
as id and cleaned title only, **shuffled together**, so the labeller cannot tell the strata
apart or feel where it is in the list. A labeller that can see the prediction agrees with it,
and the round measures nothing.

**4. Label.** Dispatch a subagent whose entire instruction is the criterion document and the
shuffled titles, returning `IN` / `OUT` / `UNKNOWN` plus a reason for each id. Titles only —
never description bodies, never a paid API (ADR-0007). Chunk if the list is large, but never
let a chunk correspond to a stratum.

**5. Score.** Rejoin by id and report three numbers:

- **miss rate** — of the `OUT` stratum, the share the labeller called `IN`.
- **false accept rate** — of the `IN` stratum, the share the labeller called `OUT`.
- **unknown share** — of the whole corpus, not of the sample.

Also report what the `UNKNOWN` stratum turned out to be. It does not gate the exit, but a pile
that is mostly `IN` means recall is worse than the miss rate says.

**6. Write it down.** A measurement doc under `docs/measurements/`, following the shape of the
ones already there: what was run, against which snapshot, the numbers, and what the round
changes. The labels themselves go in as a fixture under `src/test/resources/`, recording the
criterion version they were labelled under. Fixtures accumulate — never replace or regenerate
one.

**7. Propose, then stop.** From the disagreements, propose a diff to the word lists and the
dictionary, and to the ruling table where the labeller had to guess at a phrase the criterion
does not cover. **Halt.** The diff is applied only once the developer approves it: the
dictionary grows through a decision, never as a side effect of a measurement.

## Exiting

The loop is done when two consecutive rounds hold miss rate ≤ 3%, false accept rate ≤ 10%,
and unknown share ≤ 10%. Say so plainly when a round meets the thresholds, and say which
consecutive round it is. Until then, the round ends at step 7 and the next one is a separate
invocation.
