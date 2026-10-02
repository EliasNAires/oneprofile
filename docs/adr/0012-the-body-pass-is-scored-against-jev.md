# The body pile is labelled once, by Jev, and every round is scored against those labels

Status: accepted

The body pass of #11 is measured against labels, as the title rules were (ADR-0008), but a
description body is two orders of magnitude longer than a title, and a Claude Code session
cannot label hundreds of them per round. So the labels for #11 are produced **once, for the
whole pile, by Jev** (TypeSafe's decision model, `typesafe-ai/jev` on Vercel's AI Gateway),
and stored. Every build-review round afterwards draws its sample from the stored labels and
labels nothing.

The pile is every vacancy the title stage left `UNKNOWN` with reason `domain_ambiguity` or
`scope_ambiguity`: 21 456 rows once #35 closed. `unruled` rows are discarded (ADR-0008) and
are not labelled. The pile is fixed because the title loop is closed, which is what makes a
single labelling run enough for the whole loop.

## How the labels are made

- Jev reads the whole criterion (`docs/engineering-role-criterion.md`), the title, and the
  body **as the body pass will read it**: the same cleaned text, produced by the same code,
  so a disagreement between rule and label is never a disagreement about the input.
- It answers in the three states, `IN`, `OUT` or `UNKNOWN`, and its probability is kept.
  `UNKNOWN` is a legitimate label: the body pass's unknown stratum is scored against rows the
  labeller could not decide either.
- The labels are **blind by ordering**: they are made before any body rule is written, so no
  rule can have shaped them.
- The run happens inside Jev's free period on the AI Gateway, which ends on 2026-09-25, with
  **no card on file**, so it cannot be billed. The no-paid-API rule of ADR-0007 stands: a run
  that would cost money is not made.
- The labels are committed as a fixture, with the criterion revision they were made under.

## Checking the labeller

Before any round is scored against them, about **1000 of the stored labels are re-labelled
blind by Claude Code Haiku subagents** working in batches, from the same criterion and the
same cleaned text, and the agreement is reported, split by state.

Jev is accepted as the labeller when **at least 85% of the checked rows agree**. The limit is
set against the loop's error gates below: a labeller that disagrees with a second reader on
more than 15% of rows cannot score rules to within 10%, since some of every error rate
would be the labeller's own. Below 85% the loop does not run on Jev's labels, and the
developer is told, because which labeller the loop trusts is part of its goal.

## The loop

- **Sample.** Each round draws **200 rows from each state the body pass predicted**, `IN`,
  `OUT` and `UNKNOWN`, from the stored labels, excluding every row an earlier round drew. A
  state that predicted fewer than 200 rows gives all of them. 200 rows put a 10% error rate
  within about ±4 points, and the pile holds enough rows for every round the cap allows.
  Drawing costs nothing, since nothing is labelled; what a round pays for is reading its
  disagreements.
- **Error.** As in ADR-0008, a mistake is any row whose predicted state differs from the
  label, in either direction, so each stratum yields one error rate.
- **Exit.** The loop exits when one round holds the **`IN` error at or below 10%**, the
  **`OUT` error at or below 10%**, and the **`UNKNOWN` error at or below 30%**. `IN` is the
  error the rest of the pipeline pays for, because an `IN` row enters the engineering subset
  and every later pass. `OUT` stays out of the subset for good. `UNKNOWN` is looser because
  staying unknown is the safe default (ADR-0009), and the rows that cost this gate are ones a
  model decided from wording that no rule reaches. The gate still stops a pass that decides
  nothing, since such a pass scores this stratum at about the share of the pile the labeller
  decided.
- **Cap.** The loop stops after **ten rounds**, whatever the numbers say, and reports where
  it stopped.

## Considered options

- **A Claude Code session labelling a fresh sample every round**, as ADR-0007 does for
  titles. Rejected on cost in context: 400 bodies is about 400k tokens per round, a dozen
  subagent runs each time.
- **Paying for Jev, or any API, past its free period.** Rejected under ADR-0007.

## Consequences

The loop's labelling cost is paid once. Because the fixture holds labels for every row of the
pile, a round's sample can exclude every row an earlier round used without ever running out.
A criterion change after the labelling run does not relabel the pile; the fixture records the
revision it was made under, and rows whose label a later criterion would change are the
developer's call.

This amends ADR-0007 for #11 only: the labeller of the title loop remains a Claude Code
session reading titles.
