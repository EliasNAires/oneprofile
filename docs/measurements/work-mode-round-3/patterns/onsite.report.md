# Onsite cluster report

Group: rows whose truth includes `onsite` and the rule does not give it. 124 rows read. Code
cited is `WorkModeRule.java`; line numbers are from the copy read for this task.

## Family 1 — "in person" / "in-person" is not in the rule's vocabulary at all (6 rows)

Rows: 235905, 785752, 238228, 502503, 136888, 1501388.

> "This is an in-person opportunity in London." (235905)
> "Courses are in-person and on-campus for the Spring 2027 Semester" (785752)
> "All full-time roles are in person in New York." (238228)

Why the rule misses it: `WORDS`/`DESCRIPTION_WORDS` (lines 68–77, 889–894) hold `"on site"`,
`"in office"`, `"presencial"`, but never `"in person"`. `MODE_ROLE` (290–292) and `MODE` (112–113)
list `in-office` but not `in-person`. So no step — `sentence()`, `offer()`, `REQUIRED_ONSITE`,
`EVERY_DAY` — can ever see this phrase; a sentence built entirely on it returns empty everywhere.
This is the single biggest vocabulary gap found: it recurs across unrelated templates (235905,
785752, 238228, 502503, 136888, 1501388), always the same shape (no governing word missing, no
schedule missing — just the word).

Fix shape: add `"in person"` (and `"in-person"`) to `DESCRIPTION_WORDS` → ONSITE, and to the
`MODE` string's onsite alternatives so `CHOICE`/`LIST`/`MODE_ROLE` pick it up too. Guard: keep
`DESCRIPTION_SUBJECTS` entries for `"in-person events"`/`"in-person training"` (already partly
covered by `EVENT`, line 390) so an onboarding/offsite day isn't read as the mode.

Corpus check: not checked (told to stop running queries before this family was sized).

## Family 2 — a duty/support sentence built on "onsite", with no modal or travel verb (11 rows)

Rows: 176964, 1064591, 1156878, 1156894, 1119803, 594018, 350807, 326986, 902745, 672442, 635489.

> "Provide on-site technical support to clinicians, biomedical engineers, and hospital personnel…" (1064591)
> "…a 9-person team that provides onsite support to users and customers." (350807)
> "Manage onsite SAP SuccessFactors ECP support operations for a recently go-live environment." (176964)

Why the rule misses it: `REQUIRED_ONSITE` (329–332) only fires after a governing word from a
fixed set (`required|requires|must|need to|ability|able|willing|expected|will be an`). Duty verbs
like *provide, manage, perform, deliver, carry out, support, responsible for* aren't in that set,
so a plain duty bullet with "onsite" never matches it, `EVERY_DAY`, `PRIMARILY_ONSITE`, or
`SITE_WORK` (which additionally needs the literal word "travel"). These sentences are invisible to
every step, not wrongly excluded by a guard.

Fix shape: a new pattern (or an extra alternative inside `REQUIRED_ONSITE`) recognising a duty verb
from a curated list immediately before "onsite support/service/presence/role/operations", with
onsite/on-site/on site appearing right next to the duty noun — not three steps earlier or later (to
avoid matching "provide a stipend … used at our on-site cafeteria", which should stay excluded, the
way `DESCRIPTION_SUBJECTS` already excludes `"onsite cafeteria"`/`"onsite gym"`, lines 921–923).
Keep the existing `NOW_AND_THEN`/negation guards from `onsite()` (748–762) on it.

Corpus check: a looser sizing regex (any of provide/manage/perform/support/deliver/carry
out/responsible for within 40 chars of "onsite") hit 702 undeclared IN vacancies with empty
`work_modes`. Of 8 samples read: 4 clear true positives (968799, 594018, 902745, 475120), 1 a
benefit sentence already protected by the `onsite cafeteria` subject exclusion (1301125 — would
stay safe with a tighter pattern), 1 an "after-hours support onsite or remote" case that reads as
an on-call exception rather than the role's regular mode (1077277 — risk, needs the duty noun to
be immediately adjacent, not just nearby), 2 unreadable in the snippet. A tight, adjacency-only
version of the pattern should land near the 4/6 readable rate, not the looser one.

## Family 3 — this role/team stated as based at a named office, outside the narrow scaffolding (9 rows)

Rows: 193075, 62169, 289407, 65163, 1296180, 50596, 57610, 85142, 210506.

> "As a Silicon Verification Engineer, you will be based in either our Bristol or London office…" (65163)
> "This internship role is based in either Figma's San Francisco or New York hub offices…" (193075, 62169, 289407 — same template)
> "The Algorithmic Trading Team is looking for a Site Reliability Engineer for our Tel Aviv office." (50596)

Why the rule misses it: three separate narrow spots, depending on the row —
`THIS_ROLE` (212–214) doesn't recognise "you" as the role's subject, or "program" as a role noun
(85142), so `statement()` is never entered; where it is entered, `BASED_IN_AN_OFFICE` (308–309)
requires the determiner "our/the/a(n)" right after "based in/at", not "either…'s" (65163, 193075
family) or no "based/located" verb at all, just "for our … office" (50596) or "join … Team" blocked
by the `aside` guard at line 838 because text precedes the team mention (57610).

Fix shape: loosen `BASED_IN_AN_OFFICE`'s determiner to also accept a possessive ("Figma's") and
"either", and widen `THIS_ROLE` to accept "you" as a subject when followed by "will be based/work
from". Each sub-case needs its own small guard; this is three related gaps sharing one symptom,
not one regex change — flagged as medium confidence.

Corpus check: not checked.

## Family 4 — choice between remote and an office; bare "office" is not a vocabulary word (6 rows)

Rows: 26462, 134817, 141050, 66551, 1277603, 100852.

> "You can cooperate 100% remotely or choose one of our office spaces in Warsaw or Cracow." (134817)
> "This role can be based out of our San Francisco office or remotely in USA region." (100852)
> "…can be located in our Somerville, MA office or fully remote with occasional travel to our office." (66551)

Why the rule misses it: `DESCRIPTION_WORDS`/`WORDS` never hold bare `"office"`; it's only
recognised inside the structured `LOCATION_LABEL`, `BASED_IN_AN_OFFICE`, `WORKS_IN_AN_OFFICE`,
`PLACE_MODE` patterns. In a choice sentence like these, `offered()`/`options()` (465–517) only see
the "remote(ly)" side, so `offer()`'s `offered.size() > 1` check (718–719) never passes, and the
whole sentence — including the remote finding — is thrown away. Separately, `sentence()`'s
`STATEMENT_WORDS` cutoff (295, 853) also rejects 66551 because "office" sits 20+ words from the
subject while the recognised word ("remote") sits even further.

Fix shape: this is the riskiest of the lot. `WORKS_IN_AN_OFFICE`'s own negative lookbehind for
`front/back/management/program/project/executive/post/home` office (372–373) shows the authors
deliberately kept bare "office" out of the general word bag — adding it there would also fire
inside `sentence()`/`offer()` on any sentence mentioning an office for unrelated reasons. Safer:
only add "office" as an alternative inside `CHOICE`/`options()`'s mode-detection when it sits
beside "or"/a slash next to an already-recognised remote/hybrid word, not as a general vocabulary
entry.

Corpus check: sizing regex (`remote(ly)? … or … office` either order) found 153 undeclared IN rows
with empty `work_modes`. Of 8 random hits: 3 clean true positives (191302, 1061271 — the second is
literally the criterion's own "may be remote, hybrid, or office-based depending on…" example, 895841),
1 known family row (134817), 2 already safely kept to `none` by the existing `PERKS` heading check
and the `employees` hedge word (137443, 337046), and 2 real new risks — "flexible working
arrangements that allow remote work or office attendance" under a "WE WILL OFFER" heading (345022)
and a benefits list under "Our benefits are…" (158123) — both missed by `PERKS` (243–244), which
only matches headings starting with "we offer"/"benefits"/"perks" verbatim. **Any fix here should
also widen `PERKS` to catch "we will offer" and "our benefit(s)…".**

## Family 5 — a "‹Label› Type -" heading is read as a mode-with-detail heading (5 rows)

Rows: 810761, 811238, 811351, 811003, 811120 — all Genpact India Workday postings.

> heading `"Remote Type -"`, next segment `"Office"` (810761, 811120, …)
> same heading, next segment `"Hybrid"` (not in our sample, but the common case)

Why the rule misses it: `MODE_HEADING_WITH_DETAILS` (273–275) matches a mode word followed by a
separator and then **any uppercase letter**, intended for things like `# Remote - United States`.
"Remote Type -" satisfies this too ("Remote" + space + the "T" of "Type"), so line 623-624 reads
the heading itself as stating REMOTE and never reaches the `LABEL_ALONE` branch (199, 636) that
would correctly read the *next* segment's actual value.

Fix shape: add a negative lookahead to `MODE_HEADING_WITH_DETAILS` excluding the LABEL suffix
words already enumerated in `LABEL` (157–161: type/model/arrangement/style/mode/environment/
setting/flexibility/option(s)/pattern/schedule/conditions) right after the mode word, so
"Remote Type -" falls through to `LABEL_ALONE` and reads its real value.

Corpus check (done): 674 IN vacancies carry this exact heading. Sampled the value that actually
follows it across all 674: Hybrid 477, Office 182, Remote only 15. **The rule currently answers
REMOTE for every one of them** (confirmed on a sample of 15 — all `{REMOTE}`), so ~98% are wrong
today. This is the highest-value, best-evidenced fix in the whole set, though most of the
corrected answers land on hybrid/office rather than new onsite recall specifically for undeclared
rows (gain-side query for undeclared+empty `work_modes` returned 0 — these all already have *some*
wrong answer, not a blank one).

## Family 6 — days-a-week schedule where the office/site word comes before the count (4 rows)

Rows: 315302, 315303, 315310, 1222479.

> "…are expected to report to our Pleasanton office 5 days a week." (315302, 315303, 315310)
> "Ability to work full-time, on-site five days per week, for one year." (1222479)

Why the rule misses it: `EVERY_DAY` (338–340)'s "5 days" branch uses a forward-only lookahead for
"office/on-site/in-person" (`(?=[^.;]*?...)`), so it only matches when the office word comes
*after* "5 days". Here it comes before. 1222479 additionally has a comma between "full-time," and
"on-site", breaking the other branch's `\s*` adjacency.

Fix shape: make the office/onsite/in-person token in `EVERY_DAY` a lookaround that works in either
direction around the day count, and let `\s*` in the `full-time` branch also match `,\s*`. Low
risk — it only adds a mirrored case of an already-safe pattern.

Corpus check: not checked.

## Family 7 — an office-duty sentence is excluded by an independent "remote" elsewhere in the same sentence (4 rows)

Rows: 6450, 6458, 132538, 138396.

> "Our employees work fully from the office with the possibility to work remotely occasionally." (6450, 6458)
> "Monday - Friday we work from our office … and do not offer remote work." (132538, 138396)

Why the rule misses it: `onsite()` (748–762) requires `named.containsAll(wordsNamedIn(sentence,…))`
— i.e. the sentence may not mention any mode word beyond the one being tested. Both examples
*correctly* name "remote" too, but the remote mention is itself hedged ("occasionally") or negated
("do not offer"); `RULED_OUT` (134–136) needs "not" directly before the mode word and doesn't
match "do not **offer** remote", so the negation isn't stripped before the consistency check runs.

Fix shape: broaden `RULED_OUT` to allow a verb between the negator and the mode ("do not offer
remote", "don't provide remote options"), or apply `NOW_AND_THEN` stripping before the consistency
check so a hedged secondary mention doesn't block the primary, unhedged office statement.

Corpus check: not checked.

## Rows flagged as possibly mislabelled

None found. The sampled truths read as careful, criterion-consistent calls (case-by-case
exceptions correctly kept as the base mode, team-specific statements correctly outweighing
company-wide hedges, labels under job-details headings correctly read). No clause conflict spotted.

## Notable single-row findings worth a reviewer's eye (not full families)

- **1592**: "a hybrid, remote, or client-site environment as program needs require" — excluded by
  the `DEPENDING` guard (238–240), but revision 3 of the criterion explicitly says modes "this role
  may take, depending on the organisation" **are** a choice. The guard and the criterion disagree.
- **1039307**: "requires regular onsite presence" — excluded by `NOW_AND_THEN`'s `regular(?:ly)?`
  (381–384), but the criterion's own hybrid/onsite definitions use "a regular schedule" as the
  defining feature, not a hedge. Worth checking whether `regular` should ever have been in that set.
- **1024035** / **305217** / **475088**: "Based onsite in…", "Work must be performed onsite.",
  "Ability to perform work onsite…" — each one word away from matching `REQUIRED_ONSITE`
  (missing "based"/"performed"/"perform" as governing verbs). Cheap, low-risk additions.
- **286453** / **500488** / **1023211**: location strings ending `"… (In-person)"`, `"… - Office"` —
  `LOCATION_LABEL` (87–88) only recognises "home"/"office" as a **prefix**, never a suffix.

## Residue (one line each)

40324 Ll-Onsite recruiting tag, garbled source text, likely unfixable by regex.
139634, 139633, 716 unhedged in-office company floor sitting under a `>` benefits-marked line, possibly suppressed by the `PERKS`/benefits treatment.
248326 "deploy … in customer facilities" — travel-to-sites with no literal "travel" word.
311293 "Tokyo (building) / On Site" — PLACE_MODE likely blocked by a long parenthetical place name.
340459, 66551, 283336 mode word sits >10 words into the sentence; rejected by `STATEMENT_WORDS` (line 295/853).
444765 "comfortable with an on-site presence" duty phrase, no governing verb — same cause as Family 2, kept separate because a recruiting tag wins instead.
570815 "Location: Santa Clara, CA; on-site full-time" — `DETAILS` heading vocabulary doesn't include a bare "Location" heading.
686990 "travel to and work onsite … as needed" — `NOW_AND_THEN`'s "as needed" wrongly suppresses a travel-to-sites job.
727897 Portuguese, "Office Park" — bare-office gap plus non-English text.
778233, 1282256 "opening in the … office", "rotations at our … office" — no based/located/work/join verb for `BASED_IN_AN_OFFICE`/`WORKS_IN_AN_OFFICE` to latch onto.
924336 "# LI-OFFICE" — `TAG` pattern's alternation lacks "office".
954600 "1-3 weeks onsite" — week-count schedule, not day-count; travel sentence separate from the onsite sentence.
1017143 "Hybrid Working" heading, value "Onsite" in the next segment — not resolved.
1190498 "no remote / hybrid / telework" — `RULED_OUT` list lacks "telework".
1227977 hedged "options exist for hybrid … case by case" vs. onsite base — nuanced rule-5 case.
59021 "On-site | Vancouver Office" plus repeated "not offering remote or hybrid" — multi-sentence RULED_OUT interplay, not traced.
238589, 144670, 10486 "not a remote position", "NOT a remote position … relocation" — ruling out remote (and sometimes hybrid) with no explicit onsite word; onsite isn't inferred when nothing else is offered.
292103, 337497 "on-site at client facilities; occasional travel" — semicolon split, "occasional" hedge wrongly dominant.
475969 "come into the office during the internship" — phrasing not in `WORKS_IN_AN_OFFICE`/`BASED_IN_AN_OFFICE`.
627677 "Based in Penang … Office (long parenthetical)" — likely breaks `PLACED_LABEL`'s word limit.
726732 "daily presence at the office is needed" — phrasing gap, not traced.
775880, 305217 "staff attendance is needed on site", "must be performed onsite" — duty phrase without a matching governing verb, same family as Family 2.
785752, 238228, 502503, 136888, 1501388, 85142 — see Family 1 ("in person"/"in-person" vocabulary gap); 85142 and 136888 also have their own subject/offer issues.
924982 "designed as 'Onsite'" — curly-quoted mode word, not traced against `BARE`/`MODE_FIRST`.
1065801 "work onsite in our … office" — duty phrase, governing-verb gap, same as Family 2.
1132703 "If you like being onsite … Enjoys working onsite" — `CONDITIONS` guard ("if") misreads a candidate-trait sentence as a hypothetical.
1159194 "located … at DRS State Office" — "Office" here is a department name, not a workplace; a naive office-word fix would risk this one as a new false positive.
1198418 "work from Brindabella Business Park 5 days per week" — no office/onsite word at all, only a named place.
1202227 "located at PIMCO's London Office" — possessive determiner, same cause as Family 3.
1209852 unhedged SF floor vs. a hedged Bangalore exception in the same posting — not traced.
1445482 "fielded observatory sites … additional travel to remote deployment locations" — co-occurring hedged "remote", same cause as Family 7.
1453927 "On Site- Miami" — `LOCATION_LABEL` prefix list lacks "on site" itself (only "home"/"office").
1482085 "Hybrid-Eligible Or On-Site Eligible" — near-miss of the `CHOICE` splitter; not fully traced.
180257 "Modelo de trabalho: Presencial" — `MODE_FIRST` lookahead is English-only, lacks "presencial".
209941 "Boston, MA / Remote US" — trailing "US" breaks `PLACE_MODE`'s end-of-string anchor.
215665 "is onsite position Monday-Friday" — `MODE_ROLE` only matches at the start of a sentence, not mid-sentence.
135898 "Daily onsite presence … in alignment with our Flexible Work policy" — possible hedge from the policy wording, not traced.
144069 a generic "This hybrid role blends…" intro conflicts with a later explicit "requires you to be onsite…"; the rule's disagree-then-none fallback doesn't prefer the more specific statement.
146016, 304232 "Working Policy - On-Site" — `LABEL` vocabulary lacks "policy" as a suffix (same gap as the "category" miss in 596118/164483/503504).
164483, 503504, 596118 heading/label formats `# … (Onsite)`, `# … - Client Onsite`, `Workplace Category: Onsite` — none matches `MODE_HEADING`/`LABEL`'s exact shapes.
70570 "work on customer site … 5 days a week" — "on customer site" isn't `\bon[- ]?site\b`; also a days-before-site-word ordering issue like Family 6.
104537 "lead on-site rack bring-up" duty phrase (Family 2) plus a co-occurring "Remote work reimbursement" benefit line (Family 7-style conflict).

