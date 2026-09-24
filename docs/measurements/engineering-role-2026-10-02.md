# Iteration 12 of the engineering-role classification loop — the close

Run on the development machine on 2026-09-23, against the raw corpus snapshot already loaded in the
development database — 179 098 vacancies, cleaned and classified by iteration 11's rules. The
criterion was labelled at revision `1154f3a`, whose only change since `29b1272` is wording.

The filename carries 2026-10-02 rather than the day it ran, for the reason iteration 3 gave: the
newest file is the loop's latest word, and the earlier iterations already hold the days before it.

This is the twelfth session, the cap ADR-0008 fixes. It labelled the 1000 rows iteration 11 drew,
scored iteration 11's classifier against them, and closes #10 with the gate unmet. **It changed no
rule and draws no sample**: a rule grown from this sample would have no later iteration to measure
it, and the unknown pile it hands to #11 would come from a classifier nobody scored.

The labels are `src/test/resources/labels/engineering-role-2026-10-01.tsv`. As in iterations 5
through 11, a subagent wrote them, given the criterion and the 1000 bare titles and denied the
classifier's source, the iteration reports and the earlier label files. They were written to disk
before this session read a rule or the prediction map.

## The numbers

These three rates score **iteration 11's** classifier on the strata it drew: 100 `IN`, 500 `OUT`
and 400 `UNKNOWN`. A mistake is any row whose state differs from the label.

| Gated number | Iteration 8 | Iteration 9 | Iteration 10 | Iteration 11 | Iteration 12 | Gate |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 3.00% | 1.80% | 1.60% | 4.80% | **3.80%** (19 of 500) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 16.00% | 18.00% | 30.00% | 31.00% | **27.00%** (27 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 77.33% | 74.25% | 73.50% | 60.25% | **65.25%** (261 of 400) | ≤ 10% |
| `unruled` — of the whole corpus | 5.61% | 3.95% | 3.93% | 3.49% | **3.49%** (6 257) | ≤ 4% |

Reported alongside, gated by nothing:

- **`OUT`-stratum rows labelled `IN`**, the error nothing downstream recovers: **3** of 500, down
  from 5. They are `Quantitative Technologist C++ Intern` (`technologist` holds no head, and `c++`
  under no head decides nothing), `Website Lead at Private Equity Insights` and `Project Manager
  ERP Implementation Contractor`, where a market marker under a generic head won.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **36**. The classifier called 20
  of them `IN` and 16 `OUT`.
- **The labeller's own unknown pile** is 175 rows: 114 `domain_ambiguity`, 56 `scope_ambiguity`
  and 5 `unruled`. The `scope_ambiguity` count sits between iteration 10's 13 and iteration 11's
  80. The families it went to are the same ones: sales and solutions engineers, technical program
  and account managers, data and business analysts, and quality, reliability and test engineers.
- **What the `UNKNOWN` stratum turned out to be**, split by the classifier's reason:

  | Classifier's reason | Labelled `IN` | Labelled `OUT` | Labelled `UNKNOWN` |
  | --- | ---: | ---: | ---: |
  | `domain_ambiguity` (240) | 20 | 106 | 114 |
  | `scope_ambiguity` (49) | 17 | 14 | 18 |
  | `unruled` (111) | 10 | 94 | 7 |

  The pattern is the same as iteration 11's. The labeller decides `unruled` rows almost every
  time, and nearly always `OUT`. It decides `domain_ambiguity` rows half the time, mostly `OUT`.

**The `IN` stratum** is mostly the families the criterion had not ruled when this sample was
labelled. Of its 27 errors, 10 are `scope_ambiguity` labels: technical program, delivery and account
managers, customer and solutions engineers, and `Finance Systems Engineer`. Eleven are
`domain_ambiguity`: product managers over a software word, solution architects over a market, and
`Intern Business Technology`. Six are `OUT`: a qualifier under a generic head that names no
software work, such as `HR Systems Manager`, `Communications Security Account Manager` and
`Construction Project Manager Facilities Infrastructure`.

**The `UNKNOWN` stratum rose 5 points back toward iteration 10's level.** Iteration 11's fall to
60.25% was not a floor. The single largest error there is still the `domain_ambiguity` title the
labeller reads as plainly `OUT`, at 106 of 400 rows.

## The corpus the loop leaves

No rule changed, so the corpus stands at iteration 11's counts, confirmed against the database on
this run:

| State | Iteration 12 |
| --- | ---: |
| `IN` | **18.40%** (32 949) |
| `OUT` | **68.66%** (122 963) |
| `UNKNOWN` | **12.95%** (23 186) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 7.88% (14 119), 60.9% of the pile |
| &nbsp;&nbsp;of which `unruled` | 3.49% (6 257), 27.0% of the pile |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.57% (2 810), 12.1% of the pile |

## The exit gate

**Not met.** The `OUT` stratum error passes at 3.80%, and `unruled` passes at 3.49%. The `IN`
stratum error is 27.00% and the `UNKNOWN` stratum error is 65.25%, both against a 10% gate. The
twelve-session cap binds, so #10 closes here and the unknown pile above becomes #11's input.

ADR-0008 says `unruled` does not go to #11, because it is the title stage's backlog rather than a
question for the body. Of the 111 `unruled` rows sampled here, the labeller decided 104, and 94 of
those were `OUT`. Those are title rules nobody wrote, not questions only the body can answer.
Whether #11 takes those 6 257 rows or a title pass finishes them first is left to #11.

## Cost

Unchanged from iteration 11: a full pass over the corpus takes 0.72s on the offline harness,
against the ten-second limit.

## What changed, and why

**No rule.** The loop has no later iteration to measure a rule grown from this sample, so any rule
added now would reach #11 unmeasured.

**The criterion**, after the labels were written and scored, took the four sentences from the #10
comment "Criterion decisions for the close" (grilling, 2026-09-23):

1. A manager or director of engineering work is `IN`.
2. Product managers are `UNKNOWN` / `scope_ambiguity`.
3. Program and project managers are `UNKNOWN` / `scope_ambiguity`.
4. `scope_ambiguity` means a role family with technical and non-technical subsets.

`CONTEXT.md` no longer says Product Manager is out of the engineering role. Under sentence 2 it is
`UNKNOWN` / `scope_ambiguity`.

**The rules predate these sentences and disagree with two of them.** A product manager with no
software word is `domain_ambiguity`, through the rule that `product` holds a generic head open.
Under sentence 2 it would be `scope_ambiguity`. Program and project managers are decided whichever
way their qualifier points, where sentence 3 would leave them undecided. Whether the `product`
rule should move is one of the questions handed to #11.

## The questions carried to #11

These come from the close comment and were deliberately not decided:

- Should the "`product` holds a generic head open" rule move from `domain_ambiguity` to
  `scope_ambiguity`?
- Are `Technical Product Manager` and `Technical Program Manager` `IN`, because the title names the
  technical subset, or do they stay `scope_ambiguity`?
- Is a manager over a software qualifier that is not engineering work — `IT Manager`, `Data
  Manager`, `Security Manager`, `Salesforce Manager` — `IN` or `scope_ambiguity`?
- Is a bare `Engineering Manager`, with no software word and no discipline marker, `IN`, or does it
  stay `domain_ambiguity`?
- Can a labeller use `scope_ambiguity` on any title whose family splits, without a rulings list,
  while the classifier produces it only through rulings?

Two questions carry over from iterations 10 and 11: the AI-training gig posts, and whether a
named enterprise package (`Salesforce`, `SAP`, `Oracle EBS`) is a software qualifier. This
iteration's labeller read the package as one.
