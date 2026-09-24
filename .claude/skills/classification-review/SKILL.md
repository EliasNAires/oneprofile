---
name: classification-review
description: "Review the engineering-role classifier for one round of issue #35 — draw a fresh sample, have it labelled blind, score it, and write the handoff the next implementer works from. Use when asked to review, measure or score the classifier, or to run the review half of a classification round."
---

# Reviewing one round of the classification loop

You are the **reviewer** of issue #35's loop. The implementer changed the rules and re-classified
the corpus; you measure what it left, adversarially, and tell the next implementer what is
wrong. One session is one review. Then you stop.

You are **read-only on the code**. You write exactly two files: the label fixture and the round
report. The criterion — `docs/engineering-role-criterion.md` — is Elias's, changed only in a
grilling session: where it is silent or unclear, you log a question for him.

## The context budget

The session finishes at around 100k tokens. The thousand titles, the predictions and the
accumulated fixtures stay on disk; scripts and a subagent touch them, you read results. Work in
this session's scratchpad directory, `$SCRATCH/round-<N>` below.

## Steps

**1. Find the round.** Read #35 and its comments. The newest comment is the implementer's
"Round N implemented". Its round number is yours. If the newest comment is a review, the
implementer has not run yet: stop and say so.

**2. Draw.** `scripts/draw-classification-sample.sh $SCRATCH/round-<N>`. Leave
`predictions.tsv` and `corpus.tsv` unopened until step 4.

**3. Label blind.** Dispatch one subagent with the criterion's path, `sample.tsv`'s path and the
fixture path `src/test/resources/labels/engineering-role-round-<N>.tsv`. Tell it:

- Read exactly two files: the criterion and `sample.tsv`. Everything else in the repository
  is out of reach for this task — `src/`, `docs/measurements/`, the other fixtures, the
  scratchpad's other files.
- Label every row `IN`, `OUT`, or `UNKNOWN` with one of the three reasons, from the criterion
  alone, following its section "How a title is labelled".
- Write the fixture as `id<TAB>cleaned_title<TAB>label<TAB>reason`, one row per sample row, under
  a `#` header stating: round `<N>` of issue #35, the date, the criterion's git revision
  (`git log -1 --format=%H -- docs/engineering-role-criterion.md`, plus "with uncommitted edits" if
  `git status` shows it modified), that a subagent labelled it from the criterion and the bare
  titles alone, and the column line.
- Return the counts per label and reason, never the rows.

Done when the fixture holds 1000 data rows.

**4. Score.** `scripts/score-classification-sample.sh $SCRATCH/round-<N>
src/test/resources/labels/engineering-role-round-<N>.tsv`. It prints the numbers and writes
`disagreements.tsv`. Read the disagreements — every row.

**5. Review.** Group every disagreement by cause: the family of titles and what the criterion
says about it. Be adversarial toward the rules, and toward the labels too: a label that
contradicts the criterion's text is a labelling error, and you report it as one, with the
criterion sentence it breaks. Each group ends in one of three verdicts:

- **Rules wrong**: the criterion decides it and the rules do not follow.
- **Label wrong**: the criterion decides it and the label does not follow.
- **Criterion silent**: a question for Elias.

Done when every disagreement sits in exactly one group.

**6. Write the report**, `docs/measurements/engineering-role-round-<N>.md`:

- **The numbers**: the score script's output, with a column per earlier round of #35 (and the
  closing run, `engineering-role-2026-10-03.md`, as the baseline), against the gate in #35.
- **What the rules get wrong**: each "rules wrong" group as titles and the state the criterion
  gives them, with the sentence of the criterion that decides it. Describe **behaviour** — titles
  in, states out. How to change the rules is the implementer's call.
- **Labelling errors**: each "label wrong" group, so the numbers can be read net of them.
- **Questions for Elias**: each "criterion silent" group as a question with its titles.
- **The disagreements**: `disagreements.tsv` in full, as a code block — the archive of the round.

**7. Close the round.** Comment on #35: "Round N reviewed", the gated numbers, the gate verdict,
and a link to the report. If the gate is met or this is round 15, close #35 with the final
numbers, and comment on #11 with the unknown pile as it now stands (the reason split from
`corpus.tsv`) and the open questions for Elias. Then stop.
