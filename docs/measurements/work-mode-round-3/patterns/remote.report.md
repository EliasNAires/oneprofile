# Remote-group report: rows where truth includes remote and the rule doesn't give it

Input: `remote.json`, 50 rows. Rule: `WorkModeRule.java`. Criterion: `docs/work-mode-criterion.md`
revision 3.

## Family 1: a plain, unhedged mode statement not phrased as "this role" (22 rows)

Ids: 32789, 92772, 707483, 196035, 201950, 65821, 207810, 1309560, 742916, 174123, 160460,
245012, 162612, 650305, 650044, 650079, 650088, 1130660, 857950, 1451131, 1277603, 209941.

Criterion clause: rule 1's own example, "`all our roles are fully remote` plainly covers every
role"; rule 1, a statement about the team; rule 4, "`Primarily remote, on site as needed` is
remote... nor does travel now and then."

Snippets:
- "We are a fully remote global company with employees in Canada..." (32789)
- "Ability to work remotely full-time with up to 50% travel (eventually)" (207810)
- "You can effectively perform all work activities remotely, and will only be required to be
  on-site on an occasional basis." (650305, 650044, 650079, 650088 — identical CIBC template)
- "Working remotely, while collaborating with global teams for delivery across geographies." (1309560)
- "We are seeking a Senior Software Developer, based either in Montreal, Canada or working
  remotely..." (1451131)

Why the rule misses it: `sentence()` only recognises four subject shapes — `THIS_ROLE`,
`TEAM_OF_A_MODE`, `HIRING`, `MODE_ROLE` (lines 807-826). None of the snippets above use one of
these ("You can...", "Ability to...", a gerund bullet, "We are seeking..."), so `sentence()`
returns empty. It then falls to `offer()` (lines 695-720), which only returns a result when its
inner `offered()` call (lines 465-491) resolves to **more than one** mode
(`offered.size() > 1 && options(...).size() > 1`, line 718). But that inner `offered()` already
collapses any text with no explicit "or"/slash/schedule/semicolon down to **one** mode via
`first()` (line 490) — which is usually the *correct* single answer. `offer()` throws that
single-mode resolution away because it was built only to detect genuine choices, not to serve as
a fallback "plain statement" reader. For the CIBC rows the onsite word is present too, but it's
correctly hedged by `NOW_AND_THEN` ("occasional basis") when checked by `onsite()` — the gap is
only that the clean `{REMOTE}` resolution from `offered()` never reaches a tier in `stated()`.

Proposed fix shape: when `offer()`'s other guards pass (opening length, no `HEDGES`/`CONDITIONS`,
not under a perks heading, not a template) and the inner `offered()` resolves to exactly one
mode, credit that mode instead of discarding it.
Guards: (a) don't pick the "first" mode by *reading order* — check `NOW_AND_THEN`/`SCHEDULE_HEDGES`
near each candidate word first. Row 207944 shows why: "(hybrid as needed) ... exceptional remote
talent" — hybrid reads first but is hedged, so a naive first-word rule would wrongly return hybrid.
(b) keep excluding segments under a perks/benefits heading unless the statement is an unhedged,
every-role floor (rule 1). (c) still require no `NEGATIONS`/`CONDITIONS` word before the mode.

Corpus check (undeclared IN, empty `work_modes`): `description_segments::text ~* 'fully remote'`
→ 438 hits. Narrowed to exclude co-occurring `hybrid|on-?site|office-based|depending on` in the
same description → 178 hits. Sampled 8 of the raw 438: only ~1-2/8 were a clean, decisive
statement — most were 3-way templates ("fully on-site, hybrid, or fully remote"), a hedged
"depending on the requirements of their role", or a culture "ranges from fully remote to hybrid"
line, which should NOT resolve to remote alone. Sampled 8 of the narrowed 178: ~5/6 non-empty
samples read correctly as remote; one ("Flexible, fully remote schedule" under "What we can
offer") is a perk-framed risk. So the narrowing (exclude co-occurring other-mode words and
"depending on") matters a lot for precision; a bare "fully remote" substring is not safe alone.
Risk side (all IN): "fully remote" also appears in 138 vacancies whose ATS field already declares
Hybrid and 41 declaring OnSite — but these are not actually at risk, because `read()` (lines
428-443) returns at the FIELD step before the description is ever read.

## Family 2: the opening-words cap blocks a real choice stated far from the mode word (3 rows)

Ids: 137040, 137052, 207944.

Criterion clause: rule 5, "a choice of modes is a choice."

Snippets:
- "Scopely is looking for a Senior Product Analyst to join our MONOPOLY GO! team in our Culver
  City studio hub on a hybrid basis, or remotely within the US." (137040)
- "Scopely is looking for a Senior Software Engineer... to join our WWE Champions team in the
  United States or Canada on a remote basis or Mexico City on a hybrid basis." (137052)
- "Location: We prioritize candidates who reside in the Denver, CO area (hybrid as needed) but we
  are open to exceptional remote talent." (207944)

Why the rule misses it: `offer()` bails at line 709 (`opening.size() > OFFER_OPENING_WORDS`,
cap = 8, line 281) when more than 8 words precede the first mode word. In all three rows the
company name, "is looking for", the job title and the team name push past that cap before the
real choice ("...on a hybrid basis, or remotely...") is reached, so `offer()` returns empty
before it even gets to the part of the logic that would otherwise find the choice correctly.

Proposed fix shape: measure the opening from the nearest clause boundary (a comma, "to join",
"in") rather than from the start of the sentence, or raise the cap for sentences that do contain
an explicit "or"/"X basis, or Y basis" connector near the mode words. Guard: keep a cap so a mode
word mentioned only in passing, many clauses away, still doesn't count.

Corpus check: not checked.

## Family 3: a perks-heading anchor blocks a decisive policy, not a perk (2 rows, pattern likely recurs)

Ids: 39857, 134817.

Criterion clause: rule 1, "a policy that says which mode applies where... is a choice of those
modes where nothing else speaks" — the revision 3 worked example is almost verbatim what 39857 says.

Snippets:
- "[under: What We Offer] Remote work in locations without our offices" / "Hybrid work in
  locations with offices (2 days in-office, 3 days remote)" (39857)
- "[under: We Offer] You can cooperate 100% remotely or choose one of our office spaces in
  Warsaw or Cracow." (134817)

Why the rule misses it: in `stated()` (line 608), `offer()` is only called when
`segment.under() == null || !PERKS.matcher(segment.under()).find()`. `PERKS` (lines 243-244) is
anchored (`^\W*...`) to headings that themselves *start with* "we offer"/"benefits"/"perks" —
"What We Offer" and "We Offer" both match, so these segments are blocked outright, no matter what
they say. But the content here is exactly rule 1's "policy that says which mode applies where,"
not a perk listing.

Proposed fix shape: stop gating on the heading's name; gate on the content's shape instead — a
perk/flexibility mention (a `HEDGES` word, or a bare list of every mode with nothing narrowing it)
declares nothing, but a location- or schedule-conditioned statement ("remote in X; hybrid in Y, N
days in office") is read as a choice regardless of which heading it sits under.

Corpus check: not checked.

## Family 4: a schedule token inside one option of a choice swallows the whole option (2 rows)

Ids: 207490, 105621.

Criterion clause: rule 5, "a choice of modes is a choice"; rule 4, "a mode word with a schedule is
read with its schedule" — meant for one mode's own schedule, not a neighbouring option's.

Snippets:
- "Hybrid work in Prague (Karlin), Brno (Clubco) or remote in CZ/SK. Only Prague-based employees
  are required to work from the office at least 2 days/week." (207490)
- "Location: Hybrid in Seattle or New York City (2x/week in office) preferred; open to remote
  candidates based in Denver." (105621)

Why the rule misses it: the general `offered()` (lines 465-491) has, at line 480-481,
`if (SCHEDULE.matcher(text).find()) return EnumSet.of(HYBRID);` — this fires for *any* schedule
token anywhere in the option text, not just one tied to that option's own mode word. In 207490,
option 2 is "remote in CZ/SK. Only Prague-based employees are required to work from the office at
least 2 days/week" — the "2 days/week" belongs to the Prague-only exception, but it overrides the
"remote" word in the same option, turning the whole option into HYBRID. In 105621 the same thing
happens with "(2x/week in office)" swallowing the later, separate "open to remote candidates"
clause. Also in 105621, `options()` (lines 499-517) treats the "Seattle **or** New York City"
split as a mode-choice cut, because `edge()` (line 520, `EDGE_WORDS=3`) grabs "Hybrid in Seattle"
as the 3 words before "or" and finds "hybrid" there — a false cut between two *places*, which the
method's own doc comment says to avoid, but the short phrase defeats the 3-word window.

Proposed fix shape: only let a schedule override a mode word within the same clause/sentence as
that schedule (split on ";" and "." before applying the schedule-collapses-to-hybrid rule), and
require at least one mode word in the *same clause* as the schedule before converting it to
hybrid. Guard: don't let this stop a real "3 days a week, remote the rest" hybrid read.

Corpus check: not checked.

## Family 5: a specific remote statement is outvoted by a weaker, more generic signal (3 rows)

Ids: 1196694, 284263, 1196885.

Criterion clause: rule 3, "the most specific statement about this job decides."

Snippets:
- Location "Office - Bulgaria - Sofia"; description: "Willingness to work REMOTE and visit the
  office for meetings and get togethers." (1196694) — rule gives onsite.
- "This role is based out of our Mahwah, NJ office." / "You'll start remote and move to a hybrid
  schedule in the office within 12 months..." (284263) — rule gives onsite.
- "This role is remote, but distance is no barrier to impact." / "Our hybrid teams collaborate
  across geographies..." / "LI-Remote" (1196885) — rule gives none.

Why the rule misses it: in 1196694, `read()` (lines 428-443) returns as soon as `modesNamedIn`
finds a location label — "Office -" matches `LOCATION_LABEL` (lines 87-88) and returns ONSITE
immediately, before the description is read at all, even though the description names a more
specific remote statement. In 284263, `sentence()` matches "This role is based out of our
Mahwah, NJ office," and since that sentence alone names no mode word, `statement()` applies the
`BASED_IN_AN_OFFICE` substitution (lines 844-846) and reads it as onsite; this lands in the
`sentences` tier, checked first in `stated()` (lines 656-664), before the adjacent, more specific
"start remote, move to hybrid within 12 months" sentence is even considered — and that sentence
doesn't match any `sentence()` subject shape anyway, so it's dropped regardless. In 1196885, "This
role is remote" correctly resolves to REMOTE, but "Our hybrid teams collaborate..." matches
`TEAM_OF_A_MODE` and adds HYBRID to the same `sentences` list; the disagreement check at line 662
(`new HashSet<>(kind).size() == 1 ? ... : empty`) bails the whole description to NONE, and the
computation never falls through to the `tags` tier where the LI-Remote tag would otherwise have
confirmed REMOTE on its own.

Proposed fix shape: treat a direct "this role/position is X" statement as stronger than a "our/the
team(s) is/are X" statement (the regex already distinguishes them via the `team` named group in
`THIS_ROLE`, line 212-214) rather than letting them cancel each other out; make the
`BASED_IN_AN_OFFICE` substitution a last resort, applied only if no other segment states a
conflicting, more specific mode; and let a location label be overridden by an explicit, unhedged
description statement, as rule 3 asks, rather than short-circuiting before the description is read.

Corpus check: not checked.

## Residue (one line each)

- 47439: "Remote (US-only) pay range: ..." — mode word prefixes a pay line marked boilerplate;
  the `PAY`/`BARE` check (line 642-644) requires the segment to be bare, this isn't. Pay-range
  footers often don't reflect this specific posting's own mode — treat with caution, not a rule.
- 477560: "Role Designation: Remote" — `LABEL` (lines 157-161) has no "designation" alternative.
- 917756: "# Solutions Architect - U.S. REMOTE" — `MODE_HEADING` (line 265-266) only accepts a
  pipe (`|`) between the job family and the mode, not a dash; also covered by Family 1.
- 16072: "#LI-Remoto" — `TAG` (line 153-154) only recognises English spellings, not the
  Spanish/Portuguese ones ("remoto"/"remota"/"híbrido") that `WORDS` already has elsewhere.
- 181861: "a small remote-first team" — `TEAM_OF_A_MODE`'s `(?!\s+first\b)` guard (line 222-223),
  meant to exclude "remote-first company," also wrongly excludes "remote-first team," which the
  criterion says should count.
- 315375: "# This will be a remote Monday-Friday role..." — explained by Family 1 (single mode
  word, discarded); separately, `THIS_ROLE`'s `{0,2}?` gap between "this" and "role" (line 212) is
  too narrow to reach across "will be a remote Monday-Friday."
- 1094251: "This role drives scalable and integrated platform capabilities across onsite, offsite,
  social, and in-store channels" — "onsite" here is the criterion's own example of a channel
  word, not a mode, but `describedSubjects()` (lines 917-924) doesn't exclude this exact phrase, so
  it wrongly votes ONSITE and conflicts with (and cancels) a correct REMOTE vote from "This
  position is fully remote..." earlier in the same description.
- 207810, 1309560, 742916, 174123, 160460, 245012, 162612, 650305/650044/650079/650088, 1130660,
  857950, 1451131, 1277603, 209941, 32789, 92772, 707483, 196035, 201950, 65821: counted in Family 1.

## Rows I think are mislabelled

- 1366922: "Remote or Hybrid work" under a "What We Offer" heading, truth given as `remote,hybrid`
  — not adjudicated (labeller alone). This looks like rule 1's own counterexample almost verbatim
  ("`Thrive in a Hybrid or Remote Model`... do not plainly cover every role, so they declare
  nothing, wherever they sit"). I'd expect truth `none` here, not a choice.
