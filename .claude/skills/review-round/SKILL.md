---
name: review-round
description: Score and review one round of a rule loop, and write the next round's feedback.
disable-model-invocation: true
argument-hint: "#<issue>"
---

Review the latest round of the rule loop on the issue given. Read `docs/agents/rule-loop.md`
first: it defines the round, the two roles, the loop spec and what the round comment holds.

1. **Read the loop.** The issue body's `## Loop` section, the ADR it names, the criterion, and
   the earlier round comments. Round N is the latest `#<issue> … round N` commit. Done when you
   know round N, its commit, and every gate and the cap.
2. **Score.** Run the spec's score command for round N, with whatever it needs up (its usage
   says). Save its full output under the spec's archive path, named for the round.
3. **Review the code.** Read the round's diff and the rules as they stand, not the commit
   message: the implementer's reasoning is what the review checks, not where it starts. Trace
   every disagreeing row to the rule that decided it, or to the absence of one, and group the
   rows into patterns. Done when every disagreeing row belongs to a pattern or is named as a
   labeller error, with the criterion clause it was judged by.
4. **Settle the criterion.** A pattern where the criterion itself is unclear is yours to
   decide, judged by what the classifier is for. So is a labeller error that a clause already
   covers: the labeller is the final gate once the loop ends, so a clause it misapplies is not
   working. Sharpen it, and the answer text the labeller chooses from where that steers it. Write
   every change into the criterion now, then score the round again under the new revision, so the
   Feedback is measured against the criterion the next round is labelled under.
5. **Write the round comment** on the issue: Record and Feedback, as `rule-loop.md` describes.
   Feedback is patterns and clauses, ranked by the error they cost against the gates, with no
   vacancy ids and no labels. When the round held every gate, or N is the cap, the comment says
   the loop exited or stopped, and reports what the issue's **Done when** asks for.
6. **Commit** the archive and any criterion change as `#<issue> round N review`, then post the
   comment.
7. **Stop.** Tell Elias the next step: `/mattpocock-skills:implement #<issue>` in a fresh
   session, or, if the loop ended, that the issue is ready for him to close.
