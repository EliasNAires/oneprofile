# Whether two vacancies are one position

The criterion of the duplicate pass (#106, first written for #27). The blind sample's labellers read it,
and the pass (`DuplicateRun`, `DuplicateRule`) answers to it. Where the pass and this file disagree, this
file is right.

Two vacancies of one company with the same title are one of three things:

- **duplicate**: one position, posted twice for the same place and work mode. The same requisition on a
  second board or career site of the company, a repost, or one role posted once per headcount. Only one
  of them is shown.
- **another vacancy**: one position, posted once per place or work mode: remote once per country or
  state, onsite once per city, or the same role once onsite and once remote. Both are shown.
- **different position**: two jobs. A company with *Software Engineer* in 40 cities for 40 teams has 40
  positions.

## What the labeller sees

A pair of vacancies, A and B, each with its company, its title, its declared locations, its work modes
(empty where it declares none) and its whole description, segment by segment: `#` before a heading, `-`
before a list item, `>` before boilerplate. Not where it was posted, when, or how alike a rule found the
two.

## One position or two

Read the two descriptions side by side, setting aside what changes with the place of a posting: places,
offices and time zones, pay and currency, dates, requisition numbers and other numbers, the work mode,
and a sentence or two about the local office or the local benefits.

1. **One position** where what is left says the same job: the same responsibilities, the same
   requirements and the same team, product or client, in the same or nearly the same words. A
   description that is the other's with a section shorter, longer or reordered is still the same job, as
   is one that adds or drops company boilerplate (an *About us*, benefits, an equal-opportunity notice).
2. **Two positions** where the job itself differs: other responsibilities, other requirements, another
   team, product, project, programme or client, another technology stack or another specialty (`Backend`
   and `Mobile`). One differing sentence is enough when it says the work is other work (`You will join
   the Payments team` against `You will join the Identity team`).
3. **Another level is another position.** Where the titles or the descriptions name different levels
   (`Engineer II` and `Engineer III`, `Senior` and `Staff`, different years of experience asked for the
   same work), the two are two positions, however alike the rest.
4. **A shared template is not one position.** Many companies write every vacancy into one frame: the
   same company paragraph, benefits and notices around a short part about the job. Two vacancies whose
   shared text is only the frame, and whose job parts differ, are two positions. Judge by the job part.
5. **A pool or an evergreen posting** (`we are always hiring`, `talent community`) posted in several
   places is one position, by rule 1.
6. **Unclear** where a description is empty or too short to tell the job (a title, a location and a
   line), or where neither side of rules 1 and 2 clearly holds.

## Duplicate or another vacancy

Only for one position. Compare the two vacancies' locations and work modes:

- **duplicate** where both name the same places and the same work modes, however written
  (`New York, NY` and `New York City`, `Remote - US` and `United States (Remote)`), including both
  naming none.
- **another vacancy** where the places or the modes differ: another city, state or country, a place one
  names that the other does not, or one onsite and the other remote. A place named only in a
  description, where the locations name none, counts as the vacancy's place.

## How to answer

One line per pair, in the batch's order: its number, the answer (`duplicate`, `another vacancy`,
`different position` or `unclear`) and a reason of a few words naming what decided it (`same body,
other city`, `other team: Payments vs Identity`, `same template, other duties`).

## Change log

- **Revision 1**, 2026-11-16: the measurement's (#27, `2026-11-duplicates.md`).
- **Revision 2**, 2026-11-17: vacancy groups are dropped (#106), so *same group* becomes *another
  vacancy*, shown on its own. Which pairs are one position, and which of those are duplicates, is
  unchanged.
