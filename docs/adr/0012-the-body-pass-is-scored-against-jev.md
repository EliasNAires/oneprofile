# The body pass is scored against Jev's labels, made only for the rows a round draws

Status: accepted (amended 2026-10-02, 2026-10-04, 2026-10-06)

The body pass of #11 is measured against labels, as the title rules were (ADR-0008), but a
description is two orders of magnitude longer than a title, and a Claude Code session
cannot label hundreds of them per round. So the labels for #11 are made by **Jev**
(TypeSafe's decision model, pinned to `jev-1.13.0`), called on TypeSafe's API with
`TYPESAFE_API_KEY`. Jev is the one paid API this project allows (ADR-0007).

The pile is every vacancy the title stage left `UNKNOWN` with reason `domain_ambiguity` or
`scope_ambiguity`: 21 456 rows once #35 closed. `unruled` rows are discarded (ADR-0008) and
are never labelled. The pile is fixed because the title stage is frozen: the body pass never
changes a title rule, and title gaps it finds are reported, not fixed.

## The pile after the re-sweep (2026-10-06)

#46 re-swept the corpus so descriptions keep their structure, and cut a new pair of snapshots
(ADR-0006). The pile is **recomputed once**, from the new snapshot after classification, and
is fixed from then on: 21 246 rows (11 272 `domain_ambiguity`, 9 974 `scope_ambiguity`) in
`classified-2026-10-06.dump`, against 21 456 before. 727 of them are in a language other than
English or Spanish and skip the body pass as `unsupported_language`. The re-sweep reads the
market again, so the population moved; round 4's record is a **new baseline**, not comparable
to round 3, and the ten-round cap keeps counting.

From round 4 on, Jev reads the **segmented** description, one segment per line, for every row
it labels. Labels already made stay valid and are never remade: each is a statement about the
text it read. The 2-minute gate times the body pass alone, reading stored segments; the
cleaning run that cuts them is reported, not gated.

## How the labels are made

- Jev reads the whole body criterion (`docs/engineering-role-body-criterion.md`), the title,
  the reason the title stage left it undecided (`domain_ambiguity` or `scope_ambiguity`, since
  2026-10-04: the body pass exists to settle that doubt), and the **Cleaned Description**, exported from what the application's own cleaning made of it,
  since round 4 the stored segments one a line, the same the body pass reads, so a
  disagreement between rule and label is never a disagreement about the input. Boilerplate is
  exported marked `> `; the body pass does not read it, since it is the company's text, not the
  role's.
- It answers one Choice question in the three states, `IN`, `OUT` or `UNKNOWN`, and the
  whole probability distribution is kept. `UNKNOWN` is a legitimate label: the body pass's
  unknown stratum is scored against rows the labeller could not decide either.
- **Only rows that are drawn are labelled.** The labeller check and each round send Jev the
  rows they draw, and nothing else. Every label is stored once, with the criterion revision
  and the model ID it was made under, and is never remade.
- **Blindness** is kept by what Jev reads, not by ordering: it sees the criterion and the
  vacancy, never a body rule or a body prediction. The title's reason is part of the vacancy:
  the pile is defined by it, and the body pass does not produce it. Rows labelled after the reviewer changes the
  criterion are labelled under the new revision.

## Budget

Jev bills input tokens only, at $0.042 per million, against a **$5 hard limit** on the
account. The body criterion is under 1k tokens and a cleaned description about 1.5k on
average. Labelling only what is drawn caps the work at about 7 000 rows (the check plus 600
rows a round for ten rounds), about 17M tokens, or **under $1**. Every script that calls Jev
records the `usage` of each response, keeps a running total, and **stops when it reaches $4**.

## Checking the labeller

Before the loop, about **1000 rows** are drawn from the pile, stratified by reason in
proportion to it, and labelled by Jev. **Claude Code Haiku subagents** re-label the same rows
blind, in batches of 20, through the `body-labeller` agent (`model: haiku`, tools `Read` and
`Write`), from the same criterion and the same cleaned text. A single pilot batch runs
first; if its measured cost projects past about 4M tokens for 1000 rows, the check drops to
**500 rows**, which still measures 80% agreement to within about ±3.5 points.

The check is judged on **80% agreement**, by the 95% interval of the rows checked: it passes
when the lower bound reaches 80%, fails when the upper bound stays below it, and otherwise
draws more of the 1000 rows until one holds. Agreement is a check on the **criterion's
objectivity** more than on the labeller: two blind readers of one document disagreeing says
the document is ambiguous. So a failed check is reported, and the answer is a revision of the
body criterion, re-checked on rows drawn after it, not a change of labeller. The loop does not
run until a check passes.

The first check (2026-10-02) ran Jev and Haiku on the title criterion and agreed on 72.3% of
260 rows, 55 of the 72 disagreements involving `UNKNOWN`: the title criterion says when a title
carries enough to decide, not when a description does. That is why the body pass has a
criterion of its own (#45). Its check re-labels those 260 rows for comparison, and is judged on
rows that were not drawn into the first, since the body criterion was written after reading
the first check's disagreements.

## The loop

Each round is two sessions.

- **The implementer** writes body rules test-first, in whatever shape it chooses, starting
  from the previous round's feedback. It never sees a label.
- **The reviewer** runs the scoring script, reviews the implementer's code without its
  reasoning, and writes the feedback for the next round. It decides any criterion question
  itself and writes it into the criterion, judged by what the classifier is for: helping
  software people find better jobs, so a rule that lets a non-software job into the
  engineering subset, or keeps a software job out of it, costs the user directly.

The round's record is a comment on #11; the issue stays open until the loop exits or hits
the cap.

- **Sample.** Each round draws **200 rows from each state the body pass predicted**, `IN`,
  `OUT` and `UNKNOWN`, excluding every row an earlier round drew. A state that predicted
  fewer than 200 rows gives all of them. Rows the labeller check labelled may be drawn,
  since their labels were made without any rule. 200 rows put a 10% error rate within
  about ±4 points.
- **Error.** As in ADR-0008, a mistake is any row whose predicted state differs from the
  label, in either direction, so each stratum yields one error rate.
- **Exit.** The loop exits when one round holds the **`IN` error at or below 10%**, the
  **`OUT` error at or below 10%**, the **`UNKNOWN` error at or below 30%**, and the body
  pass runs over the whole pile in **2 minutes or less** on the laptop in dev mode. `IN` is
  the error the rest of the pipeline pays for, because an `IN` row enters the engineering
  subset and every later pass. `OUT` stays out of the subset for good. `UNKNOWN` is looser
  because staying unknown is the safe default (ADR-0009), and the rows that cost this gate
  are ones a model decided from wording that no rule reaches. The gate still stops a pass
  that decides nothing, since such a pass scores this stratum at about the share of the pile
  the labeller decided. The time gate is tight because the corpus is expected to grow about
  fivefold.
- **Cap.** The loop stops after **ten rounds**, whatever the numbers say, and reports where
  it stopped.

## Considered options

- **A Claude Code session labelling a fresh sample every round**, as ADR-0007 does for
  titles. Rejected on cost in context: 400 bodies is about 400k tokens per round, a dozen
  subagent runs each time.
- **Jev labelling the whole pile once**, the first version of this ADR, written while Jev was
  free. About 150M tokens, or $6.30, over the $5 limit.
- **A shortened criterion sent to Jev.** About $2.70 for the whole pile, but Jev would read a
  different text from the Haiku check and the reviewer, and the summary could lose cases. Moot
  since 2026-10-04: the body criterion is short in its own right, and every reader reads it.
- **Several vacancies packed into one call**, the criterion sent once. About $1.70 for the
  whole pile, but TypeSafe documents that Jev's accuracy shifts as the state grows, so it
  would need a check of its own.

## Consequences

The loop's labelling cost grows with the rounds actually run, not with the pile. Labels made
in different rounds can sit under different criterion revisions; each records its own, and
a round is scored only against labels in its own sample. A failed labeller check stops the
loop before more than a few cents are spent.

This amends ADR-0007 for #11 only: the labeller of the title loop remains a Claude Code
session reading titles.
