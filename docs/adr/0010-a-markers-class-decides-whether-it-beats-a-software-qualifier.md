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

The reason is that titles which name who the work is *for* are not titles about a different
discipline: `Engineering Manager Finance`, `Marketing Web Developer`, `Staff Data Engineer
Accounting` and `Software Engineer with Java Warehouse Automation` are software roles, and a
single marker class read every one of them as the domain the work is *in*.

## Considered options

- **One marker class that always beats a software qualifier**, with a ruling for each phrase it
  gets wrong. Rejected: the family is large and the per-phrase cost does not decline — seven
  iterations each paid for it one ruling at a time. It also produces costs that read as absurd:
  `chemistry` as a marker decides `Staff Software Engineer Machine Learning and Computational
  Chemistry` out.
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

## Consequences

The unknown share rises: every market marker stops deciding titles that also carry a software
qualifier, and those rows move from `out` to `in` or to `domain_ambiguity`. That is the trade —
recall bought with unknown share.

Markers only need classing where their class has consequences, which is where they co-occur
with a software qualifier somewhere in the corpus. The rest stay market by default.

Label fixtures produced under the single-class rule are not regenerated — that is what makes
them the artifact. Each carries a header note naming which of its families are no longer
authoritative, and a session pricing a candidate rule treats those rows as evidence-free rather
than as costs.

**The hardware-adjacent code roles are decided by the discipline named in the title.** `FPGA
Engineer` is out; a code role whose non-software credential is not named stays
`domain_ambiguity`, which is honest, because the title does not say.

**A head's attributes are evidence-revisable.** The corpus is allowed to win an argument about
language — `consultant` is domain-bound on samples' evidence against the criterion's
domain-free argument — provided the reclass is priced like any other change.
