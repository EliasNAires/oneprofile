---
name: calibrate-loop
description: Calibrate a rule loop's criterion against its gold set, freeze the revision, and re-score the latest round under it.
disable-model-invocation: true
argument-hint: "#<issue>"
---

Calibrate the rule loop on the issue given. Read `docs/agents/rule-loop.md` first: it defines
the phases, the gold set and what the calibration comment holds. Decisions on the criterion are
yours to make, judged by what the classifier is for; a change to its goal, its states or the
gates is Elias's.

**The context budget.** Finish near 100k. Rows go to files and scripts count them; subagents
read them in batches.

1. **Read the loop.** The issue body's `## Loop` section, the ADR it names, the criterion, the
   last calibration comment and every criterion proposal in the round comments after it. Done
   when you know the frozen revision, the proposals waiting, and whether a gold set exists.
2. **Draft the gold set**, if there is none or the ADR asks for more rows. Draw the rows as a
   round draws them, write them into batches, and have subagents label them from the criterion,
   as the labeller would. Keep the labels in a file Elias can correct row by row, with the
   held-out share marked; never open the held-out rows yourself after drafting. Hand it to
   Elias and stop until he says it is corrected: no figure from an uncorrected gold set counts.
3. **Run the check** on the gold set: the labeller with the spec's Check command, and the
   `adjudicator` on the disagreements it finds. Read only the open part's disagreements; take
   only the held-out part's agreement figures.
4. **Revise the criterion.** Decide the waiting proposals and the patterns in the open part's
   disagreements, and write them in as one new revision with its change-log entry. A pattern
   that no wording fixes, because the question asked is the wrong shape, is reported, not
   written around.
5. **Check again** under the new revision, as in step 3. Repeat 4–5 until the held-out agreement
   stops improving. Then the revision is frozen.
6. **Re-score the latest round**, if there is one, under the frozen revision, and adjudicate it
   as `review-round` does. Archive it under the spec's archive path, named for the round and the
   revision.
7. **Write the calibration comment** on the issue: the frozen revision, what changed and why,
   the labeller's and the adjudicator's agreement on the held-out rows, and, when a round was
   re-scored, its Record and Feedback as a round comment would give them. Record the agreement
   figures in the loop's ADR.
8. **Commit** the criterion, the ADR, the gold set and the archive as
   `#<issue> calibration, revision R`, then post the comment.
9. **Stop.** Tell Elias the next step: `/mattpocock-skills:implement #<issue>` in a fresh
   session.
