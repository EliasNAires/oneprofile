# The Finder shows unknown eligibility; the hard filter is for matches

Status: accepted

The Finder launches before matching (PRD §12), and its country filter would hide most of the
engineering subset if it applied ADR-0004's hard filter: unknown is expected to be the most common
eligibility state. Decided by Elias on 2026-10-20.

A visitor picks a country, defaulting to a guess from their browser, changeable and remembered.
The Finder derives each vacancy's eligibility state for that country from its stored country set,
hides the vacancies **excluded**, and shows the **eligible** and the **unknown**, the unknown marked
as saying nothing. A toggle narrows it to the eligible alone.

ADR-0004 is unchanged for matches: only eligible vacancies are ranked against a profile. The Finder
is browsing, not matching, and a visitor reading an unknown vacancy can judge it themselves.

## Consequences

- Eligibility is derived per country at query time, from the country set, not stored as one state
  for Argentina. The country set ADR-0004 asked to store is what makes that possible.
- Recall stays the binding constraint, now for the toggle and for matches: a missed exclusion shows
  a visitor a vacancy that will not hire them, and a missed inclusion files an eligible vacancy as
  unknown.
