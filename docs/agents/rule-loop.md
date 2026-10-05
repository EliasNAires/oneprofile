# Rule loops

A **rule loop** develops rules against labels until they meet a goal: rules are written, the
whole corpus is run through them, a sample is scored against a labeller, and the misses become
the next round's work. An issue is worked as a rule loop when its body has a `## Loop` section.

Each **round** is two sessions, never one:

- The **implementer** changes the rules from the last round's feedback. It is started with
  `/mattpocock-skills:implement #<issue>`.
- The **reviewer** scores the round, reviews the code and writes the next round's feedback. It
  is started with `/review-round #<issue>`.

The issue thread is the handoff: each session starts cold and reads what the other left there.
Elias sets the goal and the cap; neither session waits on him between rounds.

## The loop spec

The `## Loop` section of the issue names what differs from loop to loop. The method above and
below stays the same.

```markdown
## Loop

Worked as a rule loop, as `docs/agents/rule-loop.md` describes: one session does one role for
one round, then stops. The implementer changes the rules from the latest round comment's
Feedback and never scores them.

- **Decision:** the ADR with the sample, the gates, the cap and the labeller.
- **Criterion:** the document the labeller and the reviewer read.
- **Rules:** the code the implementer changes.
- **Score:** the command that runs one round, `<round>` standing for its number.
- **Hidden from the implementer:** every path that holds labels or scored rows.
- **Archive:** where each round's scoring output is kept.
```

## The implementer

1. Find round N: one more than the last implemented round in `git log --oneline` (`#<issue> … round N-1`).
2. Read the latest round comment on the issue, its **Feedback** section: that is the work.
   Before round 1 there is none, and the criterion alone is the work.
3. Change the rules test-first, in whatever shape the feedback calls for. The rules answer to
   the criterion; a feedback point the criterion does not back is raised in the commit, not
   followed.
4. Commit as `#<issue> <rules> rules, round N`, the message saying what changed and why, then
   stop. Scoring is the reviewer's.

The implementer stays **blind**: it never opens a path the spec hides, never runs the score
command, and reads the round comments' Feedback sections only. A rule shaped by labels it has
seen measures nothing.

## The reviewer

The steps live in the `review-round` skill. What it hands on:

- **The round comment** on the issue, one per round, with two sections. **Record**: the
  round, the commit, the criterion revision, each gate's number and whether it held, and the
  labeller's spend. **Feedback**: the misses as patterns, each tied to the criterion clause it
  breaks and the rule that produced it, written so the implementer can act without seeing a
  row or its label.
- **The archive**: the full scoring output, disagreeing rows included, committed under the
  spec's archive path and never deleted.
- **Criterion changes**: questions the misses raise are the reviewer's to decide, judged by
  what the classifier is for, and are written into the criterion in the same round.

The loop **exits** when one round holds every gate, and **stops** at the cap whatever the
numbers. Either way the issue stays open until Elias closes it.
