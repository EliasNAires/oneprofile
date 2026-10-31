# Precision group report

75 rows. All families below are precision errors (rule gives a mode the truth doesn't hold).
Code refs are to `WorkModeRule.java` as it stands on `main` (round 3, commit `8ad46f7`).

## Family 1 — a heading like "Remote Type" / "Hybrid Working" is read as a complete label,
## ignoring the real value in the segment below it

**18 rows**: 2632, 1017143, 1319272, 810812, 810939, 810642, 810719, 810722, 811148, 811281,
811286, 811003, 811120, 810761, 811238, 811351, 581213, 1068344.

Snippets (heading → the value the rule ignores):
- 810812: `# Remote Type -` → `Hybrid` (truth hybrid, rule remote)
- 1017143: `# Hybrid Working` → `Onsite` (truth onsite, rule hybrid)
- 811238: `# Remote Type -` → `Office` (truth onsite, rule remote)
- 2632: `# Remote Work` → "All roles are remote unless otherwise specified..." (truth hybrid, rule remote)
- 581213: `# Remote Type` → "This position is not available for remote working" (truth none, rule remote)

**Why it fires wrongly.** `stated()`'s if/else chain checks `MODE_HEADING_WITH_DETAILS` (added
round 3) before `LABEL_ALONE`. That pattern is meant for `# Hybrid — 3 days onsite` or
`# Remote - United States` (mode + schedule/place), guarded only against theme words
`why|how|what|culture|love`. But its generic branch accepts mode + space + **any** capitalized
word, so it also matches `Remote Type`, `Remote Work`, `Hybrid Working` — headings whose word
after the mode is a label noun, not a schedule or place. When it matches, the code does
`offered(text, ...)` on the **heading text alone** and stops there (`else if` chain), never
reaching `LABEL_ALONE`, which is the branch that would correctly read the next segment's value.
Confirmed by reading `WORKS_IN_AN_OFFICE`/`MODE_HEADING_WITH_DETAILS` and tracing segment order
for 810812 directly against the DB (`select description_segments ...`): the heading segment is
`{"kind":"HEADING","text":"Remote Type -"}` immediately followed by
`{"kind":"SENTENCE","text":"Hybrid","under":"Remote Type -"}` — exactly a `LABEL_ALONE` shape.

**Guard shape.** Add a negative lookahead to `MODE_HEADING_WITH_DETAILS` excluding the LABEL
nouns right after the mode word (`type|work(?:ing)?|model|policy|options?|flexibility|
arrangement|schedule|conditions`) — the same vocabulary already in the `LABEL` constant — so
these headings fall through to `LABEL_ALONE` instead.

**Corpus.** Headings of this shape in IN vacancies (`"kind": "HEADING", "text": "(Remote|
Hybrid|Onsite|On-Site|In Office) (Type|Work|Working|Model|Policy|Options|Flexibility|
Arrangement|Schedule)`): **1390 hits**. Of a random 8, 7 were the clean "heading → single-value
segment" shape (`Remote Type -` → `Hybrid`/`Office`); 1 was a different shape (`Remote
Workplace` heading whose own body already states the mode, where swapping would give the same
answer either way). Breakdown: `Remote Type` 807, `Hybrid Working`/`working` 296, `Hybrid Work`/
`work` 200, `Remote Work`/`working` 41, `Hybrid Policy` 12, others <10 each — so the fix mostly
narrows a very common Workday field heading, low apparent risk.

## Family 2 — a mode word inside a "flexible"/benefit framing is read as an offered choice or a label

**13 rows**: 16962, 59630, 460018, 1077813, 136908, 192605, 288930, 59090, 756938, 1345996,
1345929, 278343, 340833.

Snippets:
- 460018: "Flexible work hybrid or remote work with modern workspaces available" (truth none, rule remote,hybrid)
- 59630 / 288930 / 192605: "In-office, hybrid, or remote flexibility" (Exadel boilerplate; truth none/hybrid/remote, rule always adds the extra modes)
- 756938: "Flexibility: Work your way — remote, hybrid, or in-office" (truth hybrid, rule remote,hybrid,onsite)
- 340833: "One day per week Work From Home flexibility" under `Perks and Benefits` (truth none, rule hybrid)

**Why it fires wrongly — three distinct, concrete gaps:**
1. `HEDGES` (checked in `offer()`) lists `most, some, many, certain, select, roles, employees,
   staff, eligible, benefit, benefits, stipend, allowance, perk, perks` but not `flexible` /
   `flexibility`, the single most common word in this phrasing. So "Flexible work ... hybrid or
   remote" passes every guard and is read as a genuine choice.
2. `PERKS` (`^\W*(?:(?:what\s+)?we\s+offer|benefits?|perks?)\b`) is anchored at the very start of
   the heading, so it doesn't match `Your Benefits at Exadel`, `What you get`, `Why You'll Love
   It Here` — the actual heading text on several of these rows.
3. Even where `PERKS` does match (e.g. 340833, under `Perks and Benefits`), the guard in
   `stated()` only wraps the `add(offers, offer(...))` call — `add(schedules, scheduled(text))`
   and the `labelled`/`LABEL_ALONE` branches run unconditionally, so a perk line can still win
   through `schedules` or `labels`.

**Guard shape.** Add `flexible`, `flexibility` to `HEDGES`; loosen `PERKS` to match anywhere a
benefits-style heading starts with a possessive/question (`your|our|what|why ... benefit|perk|
offer|love`); and gate `scheduled()`/labelled-branches by the same `PERKS` check, not just
`offers`.

**Corpus.** Not cleanly sized — `flexib\w*` co-occurring with a mode-choice shape gives **2075
raw hits**, but most are already caught by existing hedges (`roles`, `depending`, `may`). A
500-row random sample of "flexib + mode word" rows, checked per-segment in Python against the
current guard vocabulary, found **9/500 (~1.8%)** genuinely uncovered (e.g. "we are flexible
about hybrid or remote work", "Flexible work model: hybrid, remote, or in-office", "Flexible
hybrid or remote work arrangement") — scaled over the ~37k IN vacancies that mention `flexib`
near a mode word, this is a few hundred, not thousands; not fully resolved, treat as a rough
floor.

## Family 3 — a hedged, company-wide "transitioning to onsite" rollout statement is read as a flat requirement

**6 rows**: 769616, 769975, 769702, 770137, 770320, 770321 (all Fidelity Technology Group,
identical boilerplate).

Snippet: "Fidelity is transitioning to a full-time onsite working model through a phased
rollout across regions and roles." / next segment: "Currently, some roles and locations require
100% onsite presence, while others require less." (truth none, rule onsite.)

**Why it fires wrongly.** `EVERY_DAY` (added round 3) matches `full-time` directly followed by
`onsite` — "a **full-time onsite** working model" matches. `onsite()` only reads one segment at
a time: the hedge that would stop it ("transitioning", "phased rollout", "some ... require
less") sits in a *different* segment (a separate bullet/sentence under the same heading), so
`unqualified()` — which only looks at the words *before* the match, in the same sentence — never
sees it. `NOT_REQUIRED` (the word list `unqualified()` checks) has no entry for `transitioning`
or `phased`.

**Guard shape.** Either add `transitioning`, `phased` to `NOT_REQUIRED` so the trigger word
itself is disqualified when it shares a sentence with them, or — better, since the hedge is in
the *next* sentence here — have `onsite()` look at the following sentence/segment for an
explicit walk-back (`require less`, `does not apply to`) before committing.

**Corpus.** Not checked (this is Fidelity-specific boilerplate reused across postings; a general
"transitioning to X working model" shape would need its own query).

## Family 4 — a bare "based in our office" sentence outranks a correct hybrid schedule or label

**3 rows**: 657610, 657614 (Capital One Nottingham), 1020361 (LPL Financial).

Snippet (657610/657614): "This is a permanent position based in our Nottingham office." / "You'll
spend three days a week in the office (Tuesday-Thursday)..." / `Location: Nottingham - Hybrid`
(truth hybrid, rule onsite).

**Why it fires wrongly.** `sentence()`'s `statement()` path finds no mode word in "This ... is a
permanent position based in our Nottingham office", so it substitutes via `BASED_IN_AN_OFFICE`
and returns `ONSITE`. This goes into the `sentences` bucket, which `stated()` checks **first**
and returns from immediately if non-empty — before `schedules` (which correctly reads "three
days a week in the office" as hybrid) or `labels` (which correctly reads `Location: Nottingham -
Hybrid` via the new `PLACED_LABEL`/round-3 path) are even consulted. The existing escape hatch —
`if ((kind == requirements || kind == offices) && labelledHybrid) return HYBRID;` — only checks
`requirements`/`offices`, not `sentences`, so it doesn't rescue this case even though a hybrid
label is present.

**Guard shape.** Extend the `labelledHybrid` override to the `sentences` bucket's
`BASED_IN_AN_OFFICE` outcome specifically (it's already the weakest kind of onsite evidence —
"naming no mode" — so it should rank below a schedule or label, not above).

**Corpus.** Not checked.

## Smaller, single-row findings worth fixing even without a 3+ family

- **280744** (truth hybrid, rule remote): `HIRING` ("looking for ... remote") fires on "We are
  **unable to consider** candidates who are **looking for** fully remote roles" — an explicit
  exclusion of remote, not a hiring statement for a remote role. `HIRING` doesn't check for a
  preceding negation (`unable`, `not`, `cannot`) or that the sentence's subject is the employer
  rather than candidates. This wins the `sentences` bucket over the correct `schedules` hit
  ("at least three days per week" + "on-site", elsewhere in the same vacancy).
- **444765** (truth onsite, rule remote): `REQUIRED_ONSITE`'s trigger-word list (`required,
  must, ability, able, willing, expected, will be`) doesn't include `comfortable (with)`, so
  "Candidates should be **comfortable with** an on-site presence" never reaches the
  `requirements` bucket, leaving the low-priority `LI-Remote` tag to decide alone.
- **1107759** (truth hybrid, rule onsite): "Position requires in-office work **four (4)** days
  every week." — the parenthetical digit confirmation after the spelled-out count breaks
  `SCHEDULE`/`DAYS_A_WEEK` (both expect the count word directly adjacent to `day(s)`), so the
  schedule-exclusion in `onsite()` never fires and the statement is read as flat onsite instead
  of hybrid.
- **275400** (truth hybrid, rule hybrid,onsite): "preferably be **hybrid/in-office**" — the
  existing guard for "what a hybrid mode is made of" (`HYBRID_PARTS`) only strips the
  parenthesised form `Hybrid (onsite/remote)`, not the bare-slash form `hybrid/in-office`.

## Residue (one line each; the rest weren't traced past the labeller's reason)

- 100882: "hybrid working model" (too far from any trigger word to be read) loses to an onsite
  requirement built from "7 days per month" — the day-count regexes only recognise 1–4 days a
  *week*, not a monthly cadence.
- 172291: near-duplicate boilerplate to 172255 (see Mislabelled, below) — same mechanism likely.
- 284263: sequential "remote to start, hybrid within 12 months" — not traced.
- 100882, 566968, 567085: "role ... located in our X office" under a non-benefits heading, read
  onsite; truth none in both Barclays rows — not traced why "based in office" isn't decisive here
  but is elsewhere.
- 707327: "hybrid working environment ... onsite and/or working remotely" + `LI-Hybrid` — not traced.
- 1080634, 1080657: "balancing in-office collaboration with remote flexibility" — likely Family 2
  (flexibility gap) but not confirmed against the code.
- 1147116: "This is a hybrid role: part ... leader and part ... leader" — `hybrid` names the
  job's blended function, not its location (criterion rule 2); `DESCRIPTION_SUBJECTS` excludes
  `hybrid cloud/infrastructure/it/deployment(s)` but not this idiom.
- 1185244: "For the **majority of** employees, ... hybrid work model offers 3 days..." —
  `SCHEDULE_HEDGES` has `most` but not `majority`, so the hedge isn't recognised.
- 1196694: "Willingness to work REMOTE and visit the office for meetings" vs. a `Office -
  Bulgaria - Sofia` location label — not traced.
- 115818, 172255, 277961: "based in our office" + an unhedged company-wide hybrid-days policy —
  likely Family 4's mechanism in reverse (sentences bucket vs. a correct schedule), not confirmed.
- 193667: "Work is on-site, remote or hybrid arrangements will not be considered" — `RULED_OUT`
  should strip the ruled-out modes; rule instead returns all three, suggesting `RULED_OUT` isn't
  being applied on this path — not traced.
- 310994: "Location: Remote ... with consistent travel (approximately 25-40%) to ... sites" —
  not traced.
- 356661, 459301: "complimentary meals when working from our ... office" / "we ask our team
  members to be primarily in the office" (culture-framing) — `PRIMARILY_ONSITE` doesn't
  distinguish a company-culture rationale sentence from an operational one.
- 713321: "follows our hybrid work model; however, in-office expectations may adjust" — the
  semicolon-option-splitting logic in `offered()` treats the `however` clause as a second,
  equal option instead of a hedge on the first.
- 774726: "Support or lead customer audits (onsite or remote)" — a channel/duty, not a mode
  (criterion rule 2); not traced why the `SUBJECTS` exclusion list doesn't cover it.
- 1024577, 810642–811286 overlap already counted in Family 1 where applicable.
- 1209852: "We are in office 5 days a week for all roles" read as hybrid, not onsite — `EVERY_DAY`
  should catch "5 days" + "office"; not traced why it didn't win here.
- 1360940: "[office/remote/hybrid/field] environment" inside a "This position operates..."
  sentence — `PLACEHOLDER` is only checked in `offer()`, not in `sentence()`/`statement()`, so a
  bracket template reached via `THIS_ROLE` isn't excluded.
- 1480488: travel to customer sites + remote/office split — not traced.
- 42618, 249434, 249442: job-details/label value vs. a recruiting-tag/perk line — likely Family 2,
  not confirmed.
- 329234, 329236: "remote support ... required on-site client engagement" (consulting blend) —
  not traced.
- 489283: "Monday – Thursday meals provided in Ardian's office" read as hybrid — `EVENT`
  (training/onboarding/orientation/kick-off) doesn't include meal/food perks, so a benefit
  stated with a weekday range is read as this role's schedule.
- 650935: "This role requires working with both onsite and offshore **team members**" — the mode
  word describes colleagues, not this role's own location; `THIS_ROLE` + any mode word in the
  sentence is accepted without checking what the mode word is attached to.

## Rows that look mislabelled

- **172291 vs. 172255**: both are Wayve postings with the *same* boilerplate — "This is a
  full-time role based in our office in [city]." followed by "we operate a hybrid working
  policy that combines time together in our offices ... and time spent working from home" — yet
  172291's truth is `none` and 172255's is `hybrid`. The only difference is 172291 lists three
  candidate offices ("Sunnyvale, London or Israel") instead of one. Whatever the criterion says
  about this shape, it should give the same answer to both; worth a second look.
- **1345996 / 1345929**: identical rows (same company, title, location, and decisive lines) —
  not an error, but flagging in case it's an accidental duplicate in the sample rather than two
  distinct vacancies.
