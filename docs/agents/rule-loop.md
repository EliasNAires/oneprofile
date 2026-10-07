# Rule loops

A **rule loop** develops rules against labels until they meet a goal: rules are written, the
whole corpus is run through them, a sample is scored against a labeller, and the misses become
the next round's work. An issue is worked as a rule loop when its body has a `## Loop` section.
Why it is shaped this way: ADR-0014.

A loop has two phases:

- **Calibration** makes the criterion fit to score by. It revises the criterion and checks the
  labeller and the adjudicator against the **gold set**, then **freezes** the revision it ends
  on. It is started with `/calibrate-loop #<issue>`. It runs before round 1, and again whenever
  a reviewer calls for it.
- **Rounds** develop the rules under the frozen revision. Each round is two sessions, never one:
  - The **implementer** changes the rules from the last round's feedback. It is started with
    `/mattpocock-skills:implement #<issue>`.
  - The **reviewer** scores the round, reviews the code and writes the next round's feedback.
    It is started with `/review-round #<issue>`.

The issue thread is the handoff: each session starts cold and reads what the others left there.
Elias sets the goal and the cap, and corrects the gold set; no round waits on him.

## The loop spec

The `## Loop` section of the issue names what differs from loop to loop. The method above and
below stays the same.

```markdown
## Loop

Worked as a rule loop, as `docs/agents/rule-loop.md` describes: one session does one role for
one round, then stops. The implementer changes the rules from the latest round comment's
Feedback and never scores them.

- **Decision:** the ADR with the sample, the gates, the cap, the labeller and the gold set.
- **Criterion:** the document the labeller, the adjudicator and the reviewer read.
- **Rules:** the code the implementer changes.
- **Score:** the command that runs one round, `<round>` standing for its number.
- **Check:** the command that scores the labeller against the gold set.
- **Gold set:** where the gold set is kept, and which part of it is held out.
- **Question:** what the adjudicator decides of a disagreeing row, in one sentence.
- **Hidden from the implementer:** every path that holds labels, the gold set or scored rows.
- **Archive:** where each round's scoring output is kept.
```

## Calibration

The steps live in the `calibrate-loop` skill. What it settles:

- **The gold set**: rows drawn as a round draws them, labelled by an agent from the criterion and
  corrected by Elias. A share of it is **held out**: the calibrating session never reads its
  rows or labels, only the agreement the check command reports on it. A criterion revised
  against the rows it is checked on is fitted to them.
- **The criterion**: the criterion proposals waiting on the issue, and the disagreements on the
  gold set's open part, are decided and written in as one new revision.
- **Agreement**: the labeller's and the adjudicator's, on the held-out rows. It is recorded in the
  loop's ADR beside the gates and is not itself a gate (ADR-0012): a low figure says the
  criterion is unclear, and a gate set much tighter than it cannot tell rules from noise.
- **The frozen revision**, and the latest round re-scored under it, so the next Feedback is
  measured against the criterion the next round is scored by.

It ends with a **calibration comment** on the issue: the revision frozen, what changed and why,
the agreement figures, and the re-scored round's Feedback when there was a round to re-score.

## The implementer

1. Find round N: one more than the last implemented round in `git log --oneline` (`#<issue> … round N-1`).
2. Read the latest round or calibration comment on the issue, its **Feedback** section: that is
   the work. Before round 1 there is none, and the criterion alone is the work.
3. Change the rules test-first, in whatever shape the feedback calls for. The rules answer to
   the criterion; a feedback point the criterion does not back is raised in the commit, not
   followed.
4. Commit as `#<issue> <rules> rules, round N`, the message saying what changed and why, then
   stop. Scoring is the reviewer's.

The implementer stays **blind**: it never opens a path the spec hides, never runs the score
command, and reads the comments' Feedback sections only. A rule shaped by labels it has seen
measures nothing.

## The reviewer

The steps live in the `review-round` skill. The reviewer never edits the criterion. What it
hands on:

- **The round comment** on the issue, one per round, with up to three sections.
  - **Record**: the round, the commit, the frozen revision, each gate's adjudicated number and
    whether it held, the labeller's raw number beside it, and the labeller's spend.
  - **Feedback**: the **top three** patterns of real rule misses by what they cost against the
    gates, each tied to the criterion clause it breaks and the rule that produced it, written so
    the implementer can act without seeing a row or its label. Smaller patterns go in a short
    **Also seen** list the implementer may leave.
  - **Criterion proposals**, when the round calls for any: the clause, the pattern of rows that
    calls for the change, and a proposed wording.
- **The archive**: the full scoring output and the adjudication, disagreeing rows included,
  committed under the spec's archive path and never deleted.

**Adjudication** decides the gates. A subagent reads each disagreeing row with the criterion,
blind to the rules, and decides which of two unattributed answers, the labeller's and the
rules', the criterion supports. A row the rules got wrong is a rule miss; one the labeller got
wrong is not. The reviewer reads the adjudication's tallies and patterns, and opens a row only to
check a pattern.

**Recalibration** is called by the reviewer when a criterion proposal, applied, would move a gate
across its line, or when the adjudicator's patterns say it and the labeller read a clause two
ways. The round comment says so, and the next session is `/calibrate-loop`, not the implementer.

The loop **exits** when one round holds every gate, and **stops** at the cap whatever the
numbers. Either way the issue stays open until Elias closes it.
