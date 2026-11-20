# A Workday board read short is not mirrored

Status: accepted

A sweep mirrors each board it reads: a vacancy the board no longer lists is deleted as closed. Since
#81, a large Workday board was read in parts by a facet that did not name every vacancy, and the mirror
deleted the rest: 17 boards under 90% of their size, about 57 700 vacancies, in every sweep (#110,
`docs/measurements/2026-11-workday-large-boards.md`). #110 narrows only by a facet whose sum covers the
part, but a capped board (one that stops paging at 2000) read whole can't be proven: its size comes
from its facets, not from Workday. Decided by Elias on 2026-11-20.

## The decision

**A Workday board read to under 90% of its size keeps what it holds instead of being mirrored.**

- **The size** is a board's `total` when it is under 2000 or over it (an uncapped board gives its real
  one), and for a capped board, the largest sum two or more of its facets agree on, exactly or failing
  that within 1%.
- **Read** is the distinct vacancies the list named, counted before any description is asked for.
- A board read short is handled like an unreadable one: held vacancies kept, nothing added or deleted,
  its company's name left alone. Its outcome is `short`: `SWEEP_BOARD outcome=short read=… size=…`,
  and `SWEEP_REPORT` counts `short`.
- A capped board whose facets give no size is not checked, and is logged as unchecked.

## Consequences

- A visitor sees a short board's last held list until a sweep reads it whole: closed vacancies stay up
  and new ones are missing until then.
- A board read to 90% or more is mirrored, so up to 10% of a board can still be closed by mistake in one
  sweep. Counts drift between requests and the list shifts while it is paged, so a tighter share would
  hold boards back for noise.
- A board that is short every sweep is frozen. The `short` lines are what show it.
