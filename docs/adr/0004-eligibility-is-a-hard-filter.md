# Eligibility is a hard filter, and recall is the constraint that follows

Status: accepted

Only vacancies whose eligibility state is `ELIGIBLE` are ranked and shown. Vacancies whose
eligibility is `UNKNOWN` — expected to be the large majority — are excluded rather than
shown in a lower tier.

Because the filter is hard, a false negative costs a vacancy the user will never see, while a
false positive costs one bad row in a list they are already reading. That asymmetry decides
how the location a vacancy declares may be used: it is evidence **for** eligibility, never
against it. What excludes must be stated in the description body — an explicit country or
region that leaves Argentina out, or work-authorization language that implies it.

## Considered options

- **Tiering instead of filtering**: sort eligible above unknown above excluded, hiding
  nothing. Rejected. The accepted plan is to invest heavily in the extraction rules so that
  the eligible pool is large enough to be useful, and to add sources if it is not.
- **Reading the declared location as a restriction**, so that `Remote - US` excludes.
  Rejected: a US-based company can and routinely does engage people abroad as contractors or
  through an employer of record, which is what eligibility asks about. Reading the location
  field as a geographic restriction manufactures exclusions the posting never stated, and each
  one is silent.
- **A `FULLY_REMOTE` work mode** meaning remote *and* naming no place, as the pre-reset system
  had. Rejected: it answers a question about eligibility in the vocabulary of work mode, where
  nothing can act on it. Work mode and declared location are extracted separately instead.

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
