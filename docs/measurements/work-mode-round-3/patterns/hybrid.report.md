# Hybrid group: why the rule misses it

Input: `hybrid.json` (130 rows, truth includes hybrid, rule doesn't give it). Read against
`docs/work-mode-criterion.md` rev. 3 and `WorkModeRule.java`. No corpus queries were run for this
pass — every family below is marked "not checked" for gain/risk sizing.

## Family 1 — Rule 1: unhedged company/role floor, no day count the regex can grab (31 rows)

`181982,12724,279551,313636,12922,13191,181928,181983` (Anthropic, identical text),
`546365,546404` (BDO), `35779,35741` (Charles River), `342861` (Microsoft AI), `1079509` (Moderna),
`1444266` (UMCP), `359930,359884` (West Monroe), `1504044` (Wolters Kluwer), `1464216` (Vanguard),
`942101` (Invenergy), `803453` (Guardant), `125738,125719` (PubMatic), `1133922` (NIBC),
`1239737` (Regal Rexnord), `719347` (Early Warning), `757427` (Fidelity), `1069832` (Medtronic),
`129061` (Quantifind), `312145` (AlixPartners), `1132105` (Nelnet).

- "Location-based hybrid policy: Currently, we expect all staff to be in one of our offices at
  least 25% of the time." (Anthropic)
- "All BDO personnel are expected to spend some of their time working in the office, at the client
  site, and virtually unless accommodations... are in place." (BDO)
- "Moderna champions the significant benefits of in-office collaboration by embracing a 70/30 work
  model." (Moderna)
- "Starting January 26, 2026, MAI employees are expected to work from a designated Microsoft office
  at least four days a week..." (Microsoft AI)

Criterion: rule 1, "a company policy that sets a floor for every role plainly covers every role."
Why the rule misses it: `scheduled()` (WorkModeRule.java:728) is the only step that reads a
company-wide floor, and it only fires on `DAYS_A_WEEK`/`OFFICE_DAYS` (WorkModeRule.java:94-100),
both of which require a literal digit 1-4 (or spelled) immediately before "day(s)". A percentage
("25%", "70/30"), a fraction ("half the time"), a vague "some of their time," or a count above four
("8 office days per month") never matches, and the sentence has no "this role" subject
(`THIS_ROLE`, line 212) for `sentence()` to pick up either. Result: `none`.
Fix shape: none proposed — rule 1's floor clause is under recalibration per the brief; this family
is sized for information only. Corpus: not checked.

## Family 2 — a short standalone label/tag naming the mode isn't recognized as a label (14 rows)

`162571,167108,1477487,475023,939760,1342715,468192,1126051,1020630,164299,697575,696848,697383,1366922`

- "[under: You can expect] - Hybrid working arrangement" (697575)
- "[under: LI-YS1] Hybrid" (167108)
- "Job Schedule: Full-time Hybrid" (475023)
- "[under: WHAT WE OFFER] Experience Level: InternWorkplace Type: Hybrid" (1020630)

Criterion: rule 3, a value under a job-details heading or a heading naming only the mode is a
label. Why the rule misses it: `LABEL_ALONE`/`BARE`/`MODE_HEADING` (lines 199, 258, 265) only
accept a segment that is the mode word with nothing else (`\W*$` right after it), or a label:value
pair within `VALUE_WORDS`=6 words (line 206, 638). A bare bullet like "Hybrid working arrangement"
has trailing descriptor words and no colon, so it matches neither shape and nothing reads it. The
LinkedIn-style heading "LI-YS1" isn't the literal `#LI-Hybrid` the `TAG` pattern wants (line 153);
"Full-time Hybrid" fails `MODE_FIRST`'s narrow "full-time" prefix rule (line 164, only allows it
before `/`, `|`, or `,`); "InternWorkplace Type:" fails because "Workplace" is glued to "Intern"
with no boundary `LABELLED`/`PLACED_LABEL` anchor at the start of the segment.
Fix shape: let `LABEL_ALONE`/`BARE` accept a short, closed set of trailing descriptors
(`working`, `work`, `arrangement`, `setup`, `model`, `role`, `basis`, `environment`) directly after
a bare mode word with no colon, capped at ~3 extra words; let `LABELLED` search for the label
keyword anywhere near the start of the segment, not only at position 0. Guard: still require the
mode to be the first mode-bearing token, so a themed sentence that goes on to hedge isn't swept in.
Corpus: not checked.

## Family 3 — a "based in our office" sentence outranks a separate, more specific schedule (5 rows)

`115818,172255,277961,657610,657614`

- "This role is based in our Raleigh office." / "In-office 3 days per week unless designated
  remote." (115818, two different segments)
- "This is a full-time role based in our office in London." / "...we operate a hybrid working
  policy that combines time together in our offices... and time spent working from home" (172255)
- "This is a permanent position based in our Nottingham office." / "You'll spend three days a week
  in the office (Tuesday-Thursday)..." (657610, 657614)

Criterion: rule 3, the most specific statement decides; a day count beats a bare "based in office."
Why the rule misses it: `sentence()` (line 808) matches `THIS_ROLE` on "This role is based in our
X office," finds no mode word in it, and `BASED_IN_AN_OFFICE` (line 308) rewrites it into a
synthetic "onsite," which `sentence()` returns confidently. `stated()`'s priority list checks
`sentences` before `schedules` (line 656), so this synthetic onsite wins outright over the real,
more specific day-count sentence sitting in a different segment of the same vacancy — even though
it is the weaker statement by rule 3. All five rows carry both sentences.
Fix shape: before synthesizing onsite from `BASED_IN_AN_OFFICE`, check whether any other segment of
the vacancy carries a schedule (what `scheduled()` would find over the whole description); defer to
it if so. Guard: only defer when that schedule-bearing segment also names an office/onsite word, so
a vacancy that is genuinely remote except for incidental office mentions isn't flipped.
Corpus: not checked.

## Family 4 — explicit day-count schedule, blocked by a regex connector/format gap (16 rows)

`2632,29041,1107759,171723,215617,475549,1040473,796644,892775,1119963,18454,49438,169148,1086288,1308606,1256780`

- "Bay Area (hybrid, 3 days in Palo Alto office)" (2632 — place name breaks "in ... office")
- "We currently require a minimum of three coordinated days in the office per week" (29041 — an
  inserted adjective breaks "three days")
- "Position requires in-office work four (4) days every week." (1107759 — the redundant "(4)"
  breaks both `SCHEDULE` and `DAYS_A_WEEK`)
- "[under: Work mode] - Hybrid (3 days of work from the office)" (171723 — "of work from" between
  the count and "office")

Criterion: rule 4, a mode word with a schedule is read with its schedule. Why the rule misses it:
`OFFICE_DAYS` (line 99) only matches "N days (in|at) (the|our|a)? office" literally adjacent, and
`DAYS_A_WEEK` (line 95) needs "week" within 30 characters of the count — any inserted adjective,
place name, alternate preposition ("from our office"), or a parenthetical digit repeat breaks the
adjacency these regexes assume.
Fix shape: broaden `OFFICE_DAYS` to tolerate a short run of words between the count and "office"
(`\s+(?:\p{L}+\s+){0,2}?office`), add an explicit "in-office"/"from ... office" branch, and strip a
redundant "(N)" before re-testing the spelled-out count. Guard: keep the count capped at 1-4 and
require an office/onsite word in the same clause, so an unrelated day count (pay, PTO) isn't caught.
Corpus: not checked.

## Family 5 — a hedge word elsewhere in the sentence blocks an unconditional schedule (3 rows)

`211723,1020361,796660`

- "Position is hybrid in-office Tuesday-Thursday but could require occasional in-office support on
  other days and up to 40% travel between offices." (211723)
- "This roles follows a hybrid model, with an expected in-office presence of 2 days per week,
  depending on business needs." (1020361)
- "Remote: This role is based remotely but if you live within a 50-mile radius of [...], you are
  expected to report to that location three times per week, at minimum." (796660)

Criterion: rule 1, a floor stands even if some groups may need more. Why the rule misses it:
`scheduled()` (line 734) scans every word of the *whole* sentence for a hedge
(`SCHEDULE_HEDGES`, line 323) once it finds a day count and an office word; it doesn't scope the
hedge check to the clause that actually governs the count. "could", "depending", "if" sitting in a
later, unrelated clause (an exception, who it applies to) blocks the match even though the day
count itself is unconditional.
Fix shape: only block on a hedge word that appears *before* the day-count match in the sentence
(or in the clause introducing it), not anywhere after. Guard: keep blocking when the hedge precedes
the count ("most roles require 3 days"), since that is the genuine ambiguity rule 1 excludes.
Corpus: not checked.

## Family 6 — a hiring sentence names the mode late, past the trigger word's reach (6 rows)

`100882,100901,136986,137040,302326,137052`

- "Scopely is looking for a Senior Product Analyst to join our MONOPOLY GO! team in our Culver City
  studio hub on a hybrid basis, or remotely within the US." (137040)
- "We are looking to speak to candidates who are based in Dublin or Cork for our hybrid working
  model." (100882)
- "Scopely is looking for a Technical Art - Intern to join the Royal Lounge Casino team in Bangalore
  on a hybrid basis!" (302326)

Criterion: rule 3/5, a role-specific statement, including a choice it offers, decides. Why the rule
misses it: `HIRING` (line 227) only looks for a mode word immediately after
"looking for/seeking/hiring", and `TEAM_OF_A_MODE` (line 221) requires "team" right after the mode;
these sentences name the mode at the very end, after the team and city, past both patterns' narrow
adjacency window. No other step (schedule, label) matches a plain sentence like this, so it reads
`none`.
Fix shape: add a pattern anchored on the trailing "on a `<mode>` basis" / "for our `<mode>` (working
)?model", independent of the leading trigger verb. Guard: keep a sentence-length cap so it doesn't
fire on a long paragraph where "basis"/"model" appears for an unrelated reason.
Corpus: not checked.

## Family 7 — a "Remote Type -: Hybrid" description label loses to the ATS field (8 rows)

`810812,810939,810642,810719,810722,811148,811281,811286` (all Genpact)

- "# Remote Type -" / "[under: Remote Type -] Hybrid", repeated verbatim across all 8 rows.

Why the rule misses it: **uncertain — flagging per the brief rather than asserting it.**
`read()` (line 428) checks the ATS field before ever reading the description. The labeller's header
shows "field: -" (hidden from labellers, not necessarily empty in storage). If `normalized_vacancy`'s
actual `remote_type`/field value for these postings is "Remote" while the posting's own embedded
job-details text says "Hybrid," the FIELD evidence wins outright and the description's own, more
specific label is never reached. I did not query the database to confirm the stored field value, so
this is a hypothesis, not a finding. Fix shape: not proposed until the field value is confirmed.
Corpus: not checked.

## Residue (45 rows, one line each)

- 39857 — policy sets mode by location ("remote where no office, hybrid where one"); not read as
  rule 1+5's "choice by location" at all.
- 47419 — "3 to 5 day a week in office" range, same shape as family 4 but a range, not a count.
- 133636, 133562 — Roblox boilerplate footer: "onsite Tuesday, Wednesday, and Thursday," an
  enumerated weekday list, not a range `WEEKDAYS` recognizes.
- 155517, 155548 — STR: "hybrid... when work does not require facilities, welcome to work remotely"
  — mode defined by an exception, no pattern reads it.
- 280744 — `HIRING` fires on "unable to consider candidates who are looking for... remote roles"
  without checking the preceding negation; a real precision bug, singleton here.
- 297958 — "Linkedin_Hybrid" concatenated tag under an unrelated heading, not matched by `TAG`.
- 337306 — Dutch "Hybride werken..."; "hybride" isn't in `WORDS`/`DESCRIPTION_WORDS` at all.
- 351776 — "in our SF office on Wednesdays," a single repeated weekday, no count.
- 645146 — "Rotating on-site support and remote support..." bare pair, no schedule.
- 836548, 1275241 — candidate-ability bullet ("Ability to/Can work in a hybrid...") about this
  role, not framed as "this role."
- 1140086 — "home most of the time... office once a week" — inverse framing, "once a week" has no
  digit.
- 1173034 — "set days each week... option to work remote on others" — vague, no number.
- 1246080 — heading "Senior Data Scientist, London/Oxford hybrid working" has a job title before
  the mode, not a bare `MODE_HEADING`.
- 72028 — "several days a week," vague count word.
- 162637 — "20 days of remote working... per year," an annual count, different unit.
- 186392 — "Based in London (hybrid)," mode embedded mid-sentence, not a standalone segment.
- 310994 — "Location: Remote" label wins over a regular but %-quantified travel schedule; recal-
  sensitive (does a regular percentage of travel count as hybrid?).
- 344998 — "hybrid program... employees local to Albany" — no schedule at all.
- 1024577 — "work from home... 90% of the time" reads REMOTE confidently; the separate "must be in
  office once every two weeks" sentence is dropped for lacking a "this role" subject.
- 1190438 — "hybrid work schedule... remote and on-site as needed" — vague, "as needed" wording.
- 1333305 — "operate within a hybrid work environment" — bare sentence, candidate for family 2's
  fix extended to full sentences, not just bullets.
- 1465361 — "three (3) or more days per week" — open-ended "or more" breaks the regex differently
  from family 4's closed counts.
- 1480488 — "travel around 50%" + "training sessions" — hits both `SITE_WORK`'s %-exclusion and
  `EVENT`'s "training" exclusion; genuinely mixed signal.
- 155408 — "½ day work from home Fridays," a fractional day the vocabulary doesn't cover.
- 168203 — "part of Vestwell's hybrid in-office operation" — `TEAM_OF_A_MODE` needs "team," this
  says "operation."
- 196285 — Portuguese "modelo híbrido... 2 a 3 vezes por semana," non-English range phrasing.
- 200413 — "2 remote work days per month" — counts remote days, and "per month" isn't in the
  schedule vocabulary at all.
- 212864 — "preference for hybrid... however open to remote" (rule 5 exception); may already
  resolve by luck, worth re-checking.
- 289081 — "Partial telecommuting permitted; on-site... when not telecommuting" — no count.
- 319831 — Portuguese "híbrida (3x presencial)"; worth rechecking whether `DAYS_A_WEEK`'s "x"
  branch should already catch "3x" — unclear why it doesn't.
- 330022 — Spanish "Modalidad: Híbrida." — `MODE_FIRST` only lists the English word.
- 332257, 1234711 — a bare preference/offer sentence sits in its own segment, with the
  exception/alternative mode in a *different* segment, so they never combine (same mechanism
  as family 6's missed choices, but with no shared trigger word to extend).
- 467466, 467498 — "4 or more... onsite workdays per week" — open-ended count and "workdays"
  instead of "days."
- 508296 — "47 days/yr of remote work" — an annual count, under a Benefits heading.
- 727882 — "up to 20% flexibility to work remotely" — percentage, inverse framing.
- 1155528 — "with Mondays and Fridays remote" — names the remote days, not the office days, no
  digit at all.
- 1168474 — "hybrid and remote candidates" joined by "and," not a shape `CHOICE` cuts at.
- 1208228 — "a couple days per week if desired" — "a couple" isn't a counted word, plus a hedge.
- 1459654 — Spanish "hasta 3 días de teletrabajo por semana" — non-English, and inverse (telework
  days, not office days).
- 833192 — "regular attendance... is required (3 days per week)" — day count wrapped in
  parentheses after "required"; may belong in family 4, kept separate for lack of certainty.

## Rows I think are mislabelled

- **329234** (Elastic) — text: "a blend of... remote support and required on-site client
  engagement" + "Comfortability working remotely or on-site." This reads as the criterion's rule 5
  choice ("remote,onsite"), which is what the rule gives; truth is "hybrid," but nothing in the text
  states a regular, fixed in-office/away split as hybrid requires. Worth a second look.
- **284263** (Chefman) — "Remote to start, hybrid within 12 months." The criterion's remote/hybrid
  definitions describe a standing arrangement, not a timeline; calling this "remote,hybrid" (a
  choice) may not fit rule 5 either, since nothing is offered up front — it's sequential. This looks
  like a criterion gap (no rule for a mode that changes over time) rather than a labelling error,
  worth raising at the next calibration rather than silently coding around.
