# Eligibility is a hard filter, and recall is the constraint that follows

Status: accepted

Only vacancies whose eligibility state is `ELIGIBLE` are ranked and shown. Vacancies whose
eligibility is `UNKNOWN` — expected to be the large majority — are excluded rather than
shown in a lower tier.

## Considered options

- **Tiering instead of filtering**: sort eligible above unknown above excluded, hiding
  nothing. This was recommended and rejected. The accepted plan is to invest heavily in the
  extraction rules so that the eligible pool is large enough to be useful, and to add
  sources in a later iteration if it is not.

## Consequences

This makes **eligibility recall the binding constraint on the entire product**. A vacancy the
rules fail to confirm is invisible, and its absence looks like a fact about the job market
rather than a gap in a rule. That failure mode is silent, unlike a precision error, which
shows up as a bad result you can read.

Two things follow. Recall must be measured, by hand-labelling a random sample of remote
vacancies and comparing against the rules' output; the phrasings that were missed are the
work queue. And the extractor stores the **country set** it found, not merely the verdict, so
that a wrong exclusion can be diagnosed without re-reading the description, and so the
verdict is recomputable if the user's country changes.
