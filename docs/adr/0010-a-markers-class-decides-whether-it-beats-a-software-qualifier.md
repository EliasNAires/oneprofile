# A marker's class decides whether it beats a software qualifier

Status: accepted

An off-domain marker is one of two things, and the two behave differently when a title carries
a marker and a software qualifier at once. A **discipline marker** names a body of training a
person is hired on — `mechanical`, `civil`, `fpga`, and equally `nurse`, `attorney`, `chef`.
A **market marker** names who the work is done for — `finance`, `retail`, `marketing`,
`logistics`. A discipline marker decides `out` under any head and beats a software qualifier;
a market marker decides `out` only under a domain-bound head with no software qualifier, and
settles nothing at all under a domain-free head. The test between them is the credential:
would a person need that training to be hired? A marker nobody has classed is a market marker.

This replaces the single rule the criterion carried from iteration 1 — *an off-domain marker
beats a software qualifier* — which is now the discipline half of it.

## What forced it

Seven iterations of the loop (issue #10) recorded the same family of misses and paid for it one
ruling at a time. `Engineering Manager Finance`, `Marketing Web Developer`, `Pre Sales Systems
Engineer Higher Education`, `Staff Data Engineer Accounting`, `Software Engineer with Java
Warehouse Automation`: each names who the work is *for*, and the old step 4 read every one of
them as the domain the work is *in*. Iterations 3, 4 and 5 each recorded the trade explicitly
and declined to fix it, because the only fix available to the loop was a ruling per phrase —
`technical account manager`, `customer success engineer`, `embedded software engineer`,
`pre sales` — and rulings generalise across nothing. Iteration 5 named the cause and left it:
the precedence lives in the criterion's prose, which is the developer's document.

By iteration 7 this family was the shape of one of the two surviving misses, with the rules
otherwise passing both quality gates by their widest margin.

## Considered options

- **Keep one marker class and keep writing rulings.** Rejected: seven iterations of evidence
  say the family is large and the per-phrase cost does not decline. It also produces pinned
  costs that read as absurd — iteration 7 kept `chemistry` as a marker knowing it decides
  `Staff Software Engineer Machine Learning and Computational Chemistry` out.
- **Drop the market words from the marker list entirely.** Rejected: `Marketing Manager`,
  `Retail Operations Manager` and their families are a large share of the corpus's `out` pile,
  and they are decided by exactly those words. Dropping them sends tens of thousands of plainly
  commercial titles back to `domain_ambiguity`.
- **Rank Q4 above Q2 in the criterion**, to settle the hardware-adjacent code roles. Rejected
  in favour of the same mechanism: naming `fpga`, `asic` and `rtl` as discipline markers decides
  that family without turning Q2 and Q3 from alternatives into subordinates, which would narrow
  the criterion everywhere at once to fix one family.
- **A third class for business functions with no plausible software product**, so that
  `Treasury Analyst` could be decided `out`. Rejected: no test supports it, and it contradicts
  the criterion's own worked example, which says an audit analyst may work on audit software.

## What it costs

The unknown share will rise for the first time in the loop's life. Every market marker stops
deciding titles that also carry a software qualifier, and those rows move from `out` to `in` or
to `domain_ambiguity`. That is the trade being made: recall bought with unknown share, on a
classifier whose miss rate had fallen to 0.67% while its unknown stratum was getting *richer*
in `in` rows, one in twelve by iteration 6.

The ~200 markers already accumulated in `TitleClassification` are unclassed. Sorting all of
them by hand is 200 guesses about words whose class only matters when a software qualifier is
also present, so the sort is scoped by measurement instead: a session classes the markers that
actually co-occur with a software qualifier somewhere in the corpus, prices the result on the
accumulated labels, and leaves the rest market by default.

The seven existing label fixtures were produced under the superseded rule. They are not
regenerated — that is what makes them the artifact — so each carries a header note naming which
of its families are no longer authoritative, and a session pricing a candidate rule treats those
rows as evidence-free rather than as costs.

## Consequences the same mechanism carries

**The hardware-adjacent code roles are decided by the discipline named in the title.** `FPGA
Engineer` is out; a code role whose non-software credential is not named stays
`domain_ambiguity`, which is honest, because the title does not say. Three labellers had split
this family three ways before this.

**A head's attributes are evidence-revisable.** `consultant` was already domain-bound in code
against the criterion's own domain-free argument, on five samples' evidence, which left the
rules contradicting the document they answer to. The criterion now permits the corpus to win an
argument about language, provided the reclass is priced like any other change.
