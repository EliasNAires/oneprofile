---
name: review-round
description: Score and review one round of a rule loop, and write the next round's feedback.
disable-model-invocation: true
argument-hint: "#<issue>"
---

Review the latest round of the rule loop on the issue given. Read `docs/agents/rule-loop.md`
first: it defines the phases, the roles, the loop spec and what the round comment holds. You
never edit the criterion: the round is scored under the frozen revision, and a change it calls
for is a criterion proposal.

**The context budget.** Finish near 100k. No scored row enters this session in bulk: rows go to
files, scripts count them, adjudicators read them, and you read tallies, patterns and code.

1. **Read the loop.** The issue body's `## Loop` section, the ADR it names, the criterion, and
   the latest calibration comment and the round comments after it. Round N is the latest
   `#<issue> … round N` commit. Done when you know round N, its commit, the frozen revision,
   every gate and the cap.
2. **Score.** Run the spec's score command for round N, with whatever it needs up (its usage
   says). Save its full output under the spec's archive path, named for the round.
3. **Adjudicate.** Write the disagreeing rows into batches of 20 in the scratchpad, each row with
   the text the labeller read and the two answers, labeller's and rules', shuffled into `a` and
   `b` with the key kept apart. Give each batch to an `adjudicator` subagent, with the criterion's
   path and the spec's Question, all batches in parallel. Map its verdicts back with the key, by
   script. Done when every disagreeing row is a rule miss, a labeller error or neither, with the
   clause and pattern the adjudicator gave, saved beside the archive.
4. **Review the code.** Read the round's diff and the rules as they stand, not the commit
   message: the implementer's reasoning is what the review checks, not where it starts. Trace
   each pattern of rule misses to the rule that produced it, or to the absence of one, opening
   rows only to check a pattern. Where the adjudicator's patterns look wrong, spot-check a few of
   its rows yourself and say so in the Record.
5. **Note the criterion.** A pattern where the criterion is unclear, or one the labeller misreads
   across many rows, is a criterion proposal: the clause, the pattern, a proposed wording. When a
   proposal, applied, would move a gate across its line, the round calls for recalibration.
6. **Write the round comment** on the issue: Record, Feedback and any Criterion proposals, as
   `rule-loop.md` describes. Gates are reported adjudicated, the labeller's raw figure beside
   each. Feedback is the top three patterns and an Also seen list, ranked by the error they cost
   against the gates, with no vacancy ids and no labels. When the round held every gate, or N is
   the cap, the comment says the loop exited or stopped, and reports what the issue's **Done
   when** asks for.
7. **Commit** the archive and the adjudication as `#<issue> round N review`, then post the
   comment.
8. **Stop.** Tell Elias the next step: `/mattpocock-skills:implement #<issue>` in a fresh
   session; `/calibrate-loop #<issue>` if the round calls for recalibration; or, if the loop
   ended, that the issue is ready for him to close.
