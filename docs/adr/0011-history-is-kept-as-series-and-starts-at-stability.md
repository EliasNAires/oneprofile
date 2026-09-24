# History is kept as series, not as vacancies, and starts at stability

Status: accepted

The corpus keeps no history of its own: a vacancy is a mirror of its board, and one that
disappears is deleted. The explorer's trend views need history, and it is kept as **series**
— numbers computed from the corpus just before each scheduled sweep, such as the count of
vacancies per role and seniority, or a skill's share of a role's vacancies — never as the
vacancies that produced them. A vacancy stays a mirror.

Series are not recorded until **stability**: one million vacancies live in a single sweep,
and four consecutive weekly cycles of the scheduled pipeline completed without manual
intervention. Before stability the explorer shows the live corpus only.

The two series recorded from the start are the vacancy count per (role × seniority) and a
skill's share per (role × skill). Any other cut is answered over the live corpus, and is
promoted to a series once it proves worth tracking.

## Considered options

- **Keep closed vacancies, with the date they closed.** Rejected: it ends the vacancy as a
  mirror, and grows the corpus without bound for the sake of numbers that can be computed
  once and kept.
- **Archive a dated dump of the normalized facts at every aggregation point**, so a series
  could be recomputed when the rules change. Rejected for now: before stability the rules
  and the coverage both move too much for early points to mean anything, and after it the
  drift is accepted.
- **Start recording now, tagging each point with the rules version**, and hide the points
  from before stability. Rejected: points taken while discovery is still multiplying the
  corpus record the growth of discovery, not of the market.

## Consequences

**A cut that was not a series from its start can never have history.** Promoting a cut
gives it a future, not a past.

**A series is computed through the rules as they were at the time.** A later change to the
classifier, normalization or the skill extractor cannot be applied backwards, since the
vacancies are gone; a trend that crosses a rule change mixes the market with the rules. This
is the drift ADR-0006 exists to prevent for measurement, and it is accepted here for history,
on the grounds that stability is when the rules have stopped moving much.

**Reaching one million live vacancies is a discovery project**: Wayback Machine discovery
alongside Common Crawl, and five to ten applicant tracking systems rather than two.
