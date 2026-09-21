---
name: implement
description: Run one round of the engineering-role classification loop — classify, draw a blind stratified sample, label it, report the numbers, propose a list diff. Use when asked to run a classification round, measure the classifier, or advance issues #10 and #9.
---

# One classification round

Issues #10 and #9 are a loop: classify the corpus, label a fresh sample blind, measure, grow
the lists, re-classify. This skill runs **exactly one round** and stops for approval. It
never runs two.

Read `docs/engineering-role-criterion.md` first. It is the criterion, and this skill assumes
its six-step procedure, its four lists — function heads, software qualifiers, off-domain
markers, rulings — and the three unknown reasons of ADR-0008.

The head list is the one that grows. Coverage comes from heads, not from qualifiers: a head
generalises across every title that names it, and a ruling generalises across none.

## Before the first round

The labeller must be calibrated, per ADR-0007. If no calibration fixture exists for the
current criterion version, stop and say so.

Calibration certifies **the reader that does the labelling**, which for a round is a Claude
Code session. A run by hand is worth doing and its labels are the answer key, but it is
diagnostic, not the gate. The labeller reads the whole criterion, rulings table included.

Two numbers, not one: agreement of 95% or better on the items where the criterion commits to
`IN` or `OUT`, and no more unknowns than the criterion itself produces, within one item.

A session that has read a calibration set's answers is disqualified as that set's labeller,
and is equally disqualified from editing the criterion that set measures — it would fit the
criterion to answers it has already seen. If that has happened, say so and stop.

## The round

**1. Classify.** Bring the dev database up (`docker compose up -d postgres`) and run the
classifier over the corpus. Report the counts per state, and the unknown share **split by
reason** — `unruled`, `domain_ambiguity`, `scope_ambiguity`.

**2. Draw the sample.** 300 from `IN`, 600 from `OUT`, 100 from `UNKNOWN`, at random,
**excluding** every vacancy in a previous round's fixture and every vacancy in the
calibration set. Fresh means fresh: a vacancy labelled once is never drawn again, because
growing the lists from a round's misses and re-measuring on the same rows is training on
the test set.

**3. Blind it.** Keep the `vacancy id → prediction` map to yourself. Write the 1000 rows out
as id and cleaned title only, **shuffled together**, so the labeller cannot tell the strata
apart or feel where it is in the list. A labeller that can see the prediction agrees with it,
and the round measures nothing.

**4. Label.** Dispatch a subagent whose entire instruction is the criterion document and the
shuffled titles, returning `IN` / `OUT` / `UNKNOWN` plus a reason for each id. Titles only —
never description bodies, never a paid API (ADR-0007). Chunk if the list is large, but never
let a chunk correspond to a stratum.

**5. Score.** Rejoin by id and report:

- **miss rate** — of the `OUT` stratum, the share the labeller called `IN`.
- **false accept rate** — of the `IN` stratum, the share the labeller called `OUT`.
- **unruled share** — of the whole corpus, not of the sample. This is the one that must fall.
- **domain and scope shares** — reported alongside, expected to plateau.

Also report what the `UNKNOWN` stratum turned out to be. It does not gate the exit, but a pile
that is mostly `IN` means recall is worse than the miss rate says.

**6. Write it down.** A measurement doc under `docs/measurements/`, following the shape of the
ones already there: what was run, against which snapshot, the numbers, and what the round
changes. The labels themselves go in as a fixture under `src/test/resources/`, recording the
criterion version they were labelled under. Fixtures accumulate — never replace or regenerate
one.

**7. Propose, then stop.** From the disagreements, propose a diff: heads first, then
qualifiers and markers, and the ruling table where the labeller had to guess at a phrase the
criterion does not cover. A head proposal names its two attributes — domain-bound or
domain-free, engineering-capable or not — or proposes it as never-engineering, and gives the
share of the corpus it reaches. **Halt.** The diff is applied only once the developer approves
it: a list grows through a decision, never as a side effect of a measurement.

## Exiting

The loop is done when two consecutive rounds hold miss rate ≤ 3%, false accept rate ≤ 10%,
and the `unruled` share has stopped falling. The old fixed 10% unknown cap was replaced in
ADR-0008 when ADR-0009 made unknown the default: unknown now starts above half the corpus, so
a fixed number gates nothing, and what matters is that our own backlog is still shrinking.

Say so plainly when a round meets the thresholds, and say which consecutive round it is. Until
then, the round ends at step 7 and the next one is a separate invocation.
