# Round 4 work-mode miss trace — issue #98

Frozen criterion: revision 5. Rule source: `d547f065f4f6a1535cbeb48a0f4280db7d0e03ba`; the working-tree `WorkModeRule.java` is identical to that commit. All code line references below refer to that file unless another path is named. Families are mutually exclusive and assigned by the sentence/location that should decide, not the adjudicator’s pattern name. Counts retain the supplied settled labels, including disputed ones; a choice is counted once, not once per constituent mode.

## Execution and input fidelity

Compiled the current `WorkModeRule.java` and throwaway `Trace.java` into scratchpad `classes/`, using `target/classes` and the Maven dependency classpath (`cp.txt`). Executed `read(location, field, title, segments)` for every row and reflected `sentence`, `offer`, `scheduled`, `companyModel`, `singleOffer`, `primarilyOnsite`, `modeWithVisit`, `placedMode`, each specialized `onsite` step and all private regexes. **137/137 reproduced the recorded rule answer.** Evidence is in `traces.json`; controlled phrase probes are in `probes.json` and `probe-results.json`. No repository source or test was changed.

`WorkModeRun.java:74–75` passes the published location, ATS work-mode field, original title and stored segments unchanged. `NormalizedVacancyStore.java:213–214` delegates to `NormalizedVacancyRepository.java:127–134`, whose query supplies `v.location`, `v.title`, the coalesced Ashby/Lever workplace type or Workday remote type, and `n.descriptionSegments`. There is **no upstream heading/boilerplate filter in this path**. The rule itself gates offer/company/single-offer extraction under `PERKS` headings at 813–820; it still evaluates schedules, requirements and labels there.

The shown text is a different projection: `scripts/draw-work-mode-description-sample:48,145–164` flattens each cued segment, truncates its text to 400 characters, and prefixes `> ` for boilerplate, `[under: ...]` for its heading, `# ` for HEADING or `- ` for ITEM. The harness reverses those prefixes into `Segment(kind, under, text, boilerplate)` and maps `field: -` to null; company is not an argument. It does not invent omitted heading segments. Full stored segments, uncued segments and text beyond 400 characters were unavailable in these inputs. Exact reproduction validates the observed failure paths, but cannot prove that unseen text contains no additional evidence. Missing preceding-heading effects are identified as such, not asserted to have occurred in storage.

`read()` itself (603–617) returns location, then field, then title before consulting the description, contrary to criterion rule 3’s specific-description priority. That is a separate structural issue; none of these 137 reproduced failures is caused by an early nonempty location/field/title answer. The location, ATS work-mode field and title supply no recognized mode in these rows.

Guard checks: `RULED_OUT`, `named`, `SCHEDULE`, `SENTENCE_END`, `NOW_AND_THEN`, heading gates and hedge lists explain the row-level failures below. `EVENT` (558–560; consumed at 1025,1083,1105) was inspected; it excludes incidental training/event text here but is **not a demonstrated primary blocker of a valid deciding phrase in this set**. Do not infer an EVENT regression from its mere appearance in a row.

## Family counts (supplied settlements)

| Family | Rows | Remote | Hybrid | Onsite | Choice | None |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| F1 — Offered arrangements versus mentions, exclusions and amenities | 9 | 0 | 1 | 2 | 2 | 4 |
| F2 — Negative, incidental or broader statements defeat the job’s actual arrangement | 9 | 2 | 2 | 5 | 0 | 0 |
| F3 — Recurring attendance: percentages, dispersed counts, weekdays and partial presence | 17 | 0 | 15 | 2 | 0 | 0 |
| F4 — Job-details labels hidden by syntax, headings, prefixes or value guards | 27 | 9 | 8 | 9 | 1 | 0 |
| F5 — The role’s own working arrangement expressed outside the narrow subject grammar | 19 | 10 | 7 | 2 | 0 | 0 |
| F6 — The vacancy is situated in an office without a canonical “this role is based” sentence | 11 | 0 | 0 | 11 | 0 | 0 |
| F7 — Required onsite work and own-site support with intervening words or punctuation | 14 | 0 | 0 | 14 | 0 | 0 |
| F8 — Company or joined-team arrangement with elaboration, variants or unrelated hedges | 14 | 4 | 10 | 0 | 0 | 0 |
| F9 — ATS or description location explicitly names an office/site rather than a mode token | 8 | 1 | 0 | 7 | 0 | 0 |
| F10 — Non-English declarations of work model or permitted workplace | 4 | 0 | 1 | 2 | 1 | 0 |
| F11 — Temporary arrangements, approved exceptions and an unfinished move | 3 | 0 | 0 | 3 | 0 | 0 |
| Other — Physical-setting or partial-duty inference with no clear mode declaration | 2 | 0 | 0 | 2 | 0 | 0 |
| **Total** | **137** | **26** | **44** | **59** | **4** | **4** |

## F1. Offered arrangements versus mentions, exclusions and amenities

**Count:** 9; remote 0, hybrid 1, onsite 2, choice 2, none 4.

**Criterion:** Rules 1, 2 and 5: “A choice is offered to the candidate up front”; “Ruled out is not offered”; “A word naming what the work is about is not a mode”.

**Verbatim deciding phrases**:

- 350938: “Ensuring collaboration in a remote / hybrid work environment across cross-functional teams.”
- 486953: “Our employees work in the office at least four (4) days per week”
- 312367: “in person or hybrid preferred, remote within US time zones considered”
- 732873: “The assignment must be performed almost entirely on-site”
- 64667: “Catered lunch at the office 3 days per week”

**Root cause:** `offer()` (922–953) treats a short multi-mode span as an offer without establishing its subject or positive polarity; conversely its 30-word ceiling and whole-span HEDGES reject a real office-or-remote offer mentioning equipment benefits (13952). `RULED_OUT` (186–188) handles negation immediately before a mode, not a predicate before/after a list. `options()` (674–692) requires an explicit connector, losing an additional comma-separated candidate option. `stated()` (880–889) puts these offers above real schedules/requirements. `scheduled()` (1066–1089) accepts a count associated with office catering as attendance.

**IDs:** 350938, 486953, 1531873, 312367, 732873, 1191117, 836730, 64667, 13952.

**Fix direction:** Establish that a span offers this vacancy’s arrangements before splitting options; handle predicate negation over the whole coordinated mode list, and distinguish lists describing projects or a posting template from candidate choices. Preserve explicitly considered up-front options, and isolate real alternatives before applying length or benefit-word guards. Scope attendance counts to the employee’s presence, so an amenity’s weekly availability cannot establish hybrid. Then reconcile validated offers with concrete schedules rather than allowing a spurious offer to win.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 350938 | remote,hybrid → none | Ensuring collaboration in a remote / hybrid work environment across cross-functional teams. | offer() accepts the slash pair without an own-arrangement check; collaboration environment becomes a choice. |
| 486953 | remote,onsite → hybrid | Our employees work in the office at least four (4) days per week | scheduled() correctly returns hybrid, but the instructional “In-Office Collaboration Or Remote Work” span becomes an earlier offers-tier choice. |
| 1531873 | remote,hybrid,onsite → none | The work style of each role, Hybrid, Remote, or In-Person is indicated in the job description/posting. | offer() treats an instruction to look elsewhere for the role’s style as a three-mode offer; LIST does not apply to this prose. |
| 312367 | hybrid,onsite → remote,hybrid,onsite | in person or hybrid preferred, remote within US time zones considered | options() splits “or” but not the comma before the additional remote option; offered() keeps hybrid as the second option’s first mode. |
| 732873 | remote,hybrid → onsite | The assignment must be performed almost entirely on-site | EVERY_DAY yields onsite, but offer() reads “A remote or predominantly hybrid working arrangement is not suitable” positively: RULED_OUT does not handle postposed predicate negation. |
| 1191117 | remote,hybrid → onsite | will be primarily based on site within our office spaces | statement() exceeds ten words to the mode; PRIMARILY_ONSITE does not allow “based on site”. offer() then treats “does not have remote or Hybrid work available” as a choice. |
| 836730 | remote,hybrid,onsite → none | Ability to support remote, hybrid, or on-site projects | offer() accepts a project/channel list without establishing where the candidate works. |
| 64667 | hybrid → none | Catered lunch at the office 3 days per week | scheduled() sees days plus office and ignores that catering, not attendance, is scheduled; it runs under We Offer. |
| 13952 | none → remote,onsite | Join our modern office with all the office benefits, or work remotely | offer() rejects the whole segment for length and benefits in HEDGES, despite explicit join-office versus work-remotely alternatives. WORKS_IN_AN_OFFICE expects joining a team, not an office. |

## F2. Negative, incidental or broader statements defeat the job’s actual arrangement

**Count:** 9; remote 2, hybrid 2, onsite 5, choice 0, none 0.

**Criterion:** Rules 2–4: “The most specific statement about this job decides”; “A mode word with a schedule is read with its schedule”; “the physical surroundings of the work” are not modes.

**Verbatim deciding phrases**:

- 797363: “telecommuting is not allowed and will require the employee to report full-time onsite”
- 1130403: “work from home in a hybrid capacity, but not fully remote”
- 292251: “Schedule: Hybrid 1-3 days onsite”
- 81608: “this position is fully onsite at our Manufacturing Facility”
- 1196897: “This role is remote, but distance is no barrier to impact.”

**Root cause:** `statement()` (1229–1232) returns the first mode on encountering any later negation; it does not bind negation to its target. `sentence()` (1165–1186) admits technical “hybrid role”, physical-environment language and generic “our hybrid teams”. `stated()` (880–889) puts sentences ahead of schedules and returns an empty set for any disagreement at that tier, even when one statement is broader or non-mode evidence.

**IDs:** 797363, 1130403, 292251, 81608, 144070, 144071, 1465738, 1196897, 1197072.

**Fix direction:** Bind polarity and incidental qualifiers to their own clauses before extracting modes. Retain evidence scope: an explicit vacancy arrangement must outrank company teams and environment boilerplate. Interpret technical adjectives through rule 2, and reconcile an actual recurring attendance schedule with a mode word before deciding conflict. A later “not fully remote” must remove that option rather than select the first remote synonym.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 797363 | remote → onsite | telecommuting is not allowed and will require the employee to report full-time onsite | statement() encounters “not” after telecommuting and returns first mode REMOTE. onsite(EVERY_DAY) is also blocked by the other named mode/earlier negation in that sentence; the separate onsite label/requirement loses to sentences. |
| 1130403 | remote → hybrid | work from home in a hybrid capacity, but not fully remote | statement() returns first mode REMOTE on later “not”; without that shortcut offered() would still pick the first synonym rather than the explicit hybrid arrangement. |
| 292251 | remote → hybrid | Schedule: Hybrid 1-3 days onsite | scheduled() returns hybrid, but an earlier role sentence returns remote and wins at the sentences tier. No cross-segment schedule reconciliation. |
| 81608 | none → onsite | this position is fully onsite at our Manufacturing Facility | sentence() correctly yields onsite, but “This job operates in a professional workplace or remote office environment” yields remote at the same tier. Physical-setting false evidence forces empty-set conflict. |
| 144070 | none → onsite | This role requires you to be onsite. | sentence() yields onsite here, but separately interprets “This hybrid role blends strong technical depth with strong interpersonal skills” as hybrid. Equal-tier conflict becomes none; rule 2’s meaning-of-work distinction is missing. |
| 144071 | none → onsite | This role requires you to be onsite. | Same technical-adjective hybrid false evidence and sentences-tier conflict as 144070. |
| 1465738 | none → onsite | the ability for infrequent remote work (<10%) | statement() reads this as REMOTE, while “This position will support the on-site network communications requirements” yields ONSITE; sentences-tier conflict gives none. No infrequent-remote qualifier reduces the remote mention to an exception. |
| 1196897 | none → remote | This role is remote, but distance is no barrier to impact. | sentence() yields remote; TEAM_OF_A_MODE yields hybrid for generic “Our hybrid teams ...”. Both are stored as equal-specificity role sentences, so disagreement returns none. |
| 1197072 | none → remote | This role is remote, but distance is no barrier to impact. | Same generic-team conflict as 1196897; even valid Location: US - Remote and LI-Remote cannot override a conflicting sentences tier. |

## F3. Recurring attendance: percentages, dispersed counts, weekdays and partial presence

**Count:** 17; remote 0, hybrid 15, onsite 2, choice 0, none 0.

**Criterion:** Hybrid definition: “days a week or a month in the office” and “a range or floor that leaves room for days away”; onsite definition: “on every working day”; rule 1: “A required count whose days the person picks ... is ... hybrid”.

**Verbatim deciding phrases**:

- 12911: “in one of our offices at least 25% of the time”
- 749457: “50% of their working time in the office each month”
- 206515: “the flexibility to decide which 10+ days a month they come in”
- 1464940: “working from a U.S. Bank location three (3) or more days per week”
- 344020: “work from the office on Monday, Wednesday, and Thursday, with remote work on Tuesday and Friday.”

**Root cause:** `scheduled()` (1066–1089) and constants (103–145, 196–207) miss percentage-before/after-location forms, `10+` monthly counts, hyphenated/reversed in-office counts, weekday enumerations and anaphoric presence/sites. Its pre-count `SCHEDULE_HEDGES` check suppresses a positive requirement after an unrelated negation. `EVERY_DAY` (496–499) expects a location after the count, not before it. `SENTENCE_END` (572) splits abbreviations. `offered()` (641–670) and `scheduled()` use different schedule coverage.

**IDs:** 1065764, 12911, 13185, 765427, 512388, 100998, 749457, 199268, 206515, 1042501, 1464940, 994845, 734069, 467355, 135988, 238407, 344020.

**Fix direction:** Parse a recurring attendance quantity together with the place it governs, allowing either order, parenthetical numerals, percentages, plus/range floors and enumerated weekdays. Recognize client sites and clear attendance anaphora without treating arbitrary Office tokens as confirmation. Share this interpretation between sentence/label extraction and schedule extraction. Apply hedges to the required floor, not earlier unrelated negations or the employee’s freedom to choose which required days.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 1065764 | onsite → hybrid | Candidate must be willing to perform a portion of the work in-person at the company site. | No fractional-presence path: REQUIRED_ONSITE cannot bridge “willing to perform a portion of the work”; WORKS_IN_AN_OFFICE returns onsite from the hiring sentence. Hybrid settlement needs review. |
| 12911 | none → hybrid | in one of our offices at least 25% of the time | PERCENT_ONSITE requires percentage immediately before in-office/onsite; no other count matches. COMPANY_MODEL rejects the long policy-label sentence. |
| 13185 | none → hybrid | in one of our offices at least 25% of the time | Same reversed/distant percentage gap as 12911; boilerplate is not filtered. |
| 765427 | none → hybrid | Role location: Hybrid (three days in-office, two days virtual) | LABEL lacks Role location. OFFICE_DAYS does not handle the hyphen in “in-office”; DAYS_A_WEEK has no week token; single-mode hybrid is not an offer. |
| 512388 | none → onsite | requires office presence a minimum of 5 days per week | The office precedes the five-day count; EVERY_DAY demands office/site after it. REQUIRED_ONSITE does not accept office presence; no vocabulary mode remains for statement(). |
| 100998 | none → hybrid | with 3 in-office days | OFFICE_DAYS requires days before office; DAYS_A_WEEK expects count before days, not “3 in-office days”. No resolver links temporary in-office work to the permanent schedule. |
| 749457 | none → hybrid | 50% of their working time in the office each month | PERCENT_ONSITE requires adjacent “50% in-office”; DAYS_A_MONTH requires a day count, not a share of working time. |
| 199268 | none → onsite | requires employees to be onsite five days a week | REQUIRED_ONSITE cannot bridge employees to be. EVERY_DAY cannot use five days because onsite precedes the count. statement() reaches onsite after more than ten words. |
| 206515 | none → hybrid | the flexibility to decide which 10+ days a month they come in | DAYS_A_MONTH does not allow +; no bare mode establishes the arrangement. If the count matched, pre-count flexibility is not itself a SCHEDULE_HEDGES token, so the primary failure is the count form. |
| 1042501 | none → hybrid | A presence of at least 3 working days per week is required. | DAYS_A_WEEK matches, but scheduled() office=false: presence is not a location/mode token. The Microsoft Office token is in another segment and should not supply that evidence. |
| 1464940 | none → hybrid | working from a U.S. Bank location three (3) or more days per week | SENTENCE_END splits at the second U.S. period; the count remains in “Bank location ...” without office/mode. Even without the split, bank location is not OFFICE or a mode word. |
| 994845 | none → hybrid | 50% Home Office. | PERCENT_ONSITE only covers office-attendance words after a percentage, not remote share/Home Office. A single remote token has no own-work/model/label context under What we offer. |
| 734069 | none → hybrid | requires onsite presence 4 days per week | scheduled() sees days+onsite but pre-count “not” (from “not a hybrid or remote opportunity”) vetoes it. statement() sees negation before the first mode and rejects; onsite() defers to the schedule it then fails to extract. |
| 467355 | none → hybrid | a hybrid work model that combines 4 regular onsite workdays | COMPANY_MODEL lacks offer and cannot consume the explanation. Count regexes require days/times/x rather than workdays, and normal singleOffer/statement paths have no subject/opener. |
| 135988 | none → hybrid | travel to client sites in Baltimore 3-4 days a week minimum. | DAYS_A_WEEK finds 4 days a week, but scheduled() office=false: client sites are not OFFICE and no mode token exists. SITE_WORK requires for/throughout/across immediately after sites. |
| 238407 | none → hybrid | in the office each Tuesday, Wednesday, and Thursday | scheduled() covers a weekday range or remotely-on-weekday, not an enumeration. COMPANY_MODEL lacks operates on and rejects trailing explanatory prose. |
| 344020 | none → hybrid | work from the office on Monday, Wednesday, and Thursday, with remote work on Tuesday and Friday. | Weekday enumerations do not satisfy scheduled(); WORKS_IN_AN_OFFICE matches but onsite() rejects SCHEDULE and named remote. Heading Hybrid Work Schedule is parsed as neither standalone mode nor mode-with-details. |

## F4. Job-details labels hidden by syntax, headings, prefixes or value guards

**Count:** 27; remote 9, hybrid 8, onsite 9, choice 1, none 0.

**Criterion:** Rule 3: “a value under a job-details heading ... and a heading that names only the mode, or the job family and the mode, standing alone”. Recruiting tags also count where nothing more specific speaks.

**Verbatim deciding phrases**:

- 182178: “Work Model: Onsite at Appian Headquarters”
- 1371060: “Work Setup: Onsite (Mandatory)”
- 1193720: “Work Location Assignment: Hybrid”
- 287807: “Mode: Remote”
- 1215546: “REMOTE WORKING”

**Root cause:** `LABEL`, `MODE_FIRST`, `LABELLED`, `PLACED_LABEL` (217–237) omit common label heads and anchor extraction at the segment start. The label ladder in `stated()` (829–867) only recognizes a few `under` headings, or an immediately preceding label segment; cued rendering need not retain that preceding segment. `PLACED_VALUE_WORDS` limits a whole value to eight words. `MODE_HEADING` (381–383), `LABEL_ALONE` (259–260) and the arrangement-label exclusion (849–850) treat REMOTE WORKING as a value-less label. `TAG` (213–214) only recognizes LI-prefixed tags.

**IDs:** 310501, 651599, 1020642, 40322, 177952, 182178, 182180, 1023982, 1269895, 77880, 1371060, 206303, 817049, 1193720, 1086286, 129838, 176998, 106942, 1017395, 287807, 287835, 972250, 952679, 833234, 106692, 1239737, 1215546.

**Fix direction:** Normalize job-detail fields into label/value spans before interpreting their value. Accept a field embedded after another field or a separator, and use a recognized `under` heading as context even if its heading segment is unavailable. Bound the mode-bearing clause instead of rejecting the whole value for length. Cover ordinary work setup/mode/posting labels and employment details; keep slogans, perks and arbitrary unknown abbreviations separate. Resolve whether mode-plus-working headings and employer-specific recruiting tags are valid labels before broadening them.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 310501 | none → hybrid | Schedule: Full-Time Hybrid Schedule -Monday through Friday | LABEL lacks standalone Schedule; weekday range is the full week, so scheduled() cannot establish hybrid independently. |
| 651599 | none → hybrid | Hybrid Mode | ARRANGEMENT_LABEL omits mode; Hours of Work is not DETAILS. MODE_HEADING_WITH_DETAILS matches the raw phrase, but the segment is an ITEM, so that branch does not apply. |
| 1020642 | none → hybrid | Experience Level: Mid-Senior levelWorkplace Type: Hybrid | LABELLED is start-anchored and cannot find the concatenated Workplace Type field. The separate “to be based in our Munich office” lacks THIS_ROLE/WORKS_IN_AN_OFFICE syntax. |
| 40322 | none → onsite | Ll:Onsite | No Ll label or LI recruiting tag pattern; PLACE_MODE expects a different delimiter shape and MODE_PLACE expects mode first. Meaning of Ll needs confirmation. |
| 177952 | none → onsite | Working hours and mode: Work from office / 9PM shift | LABEL lacks this combined heading; work from office is absent from DESCRIPTION_WORDS and WORKS_IN_AN_OFFICE requires a determiner before office. |
| 182178 | none → onsite | Work Model: Onsite at Appian Headquarters | PLACED_LABEL takes only the first Location value before the bar and finds no mode; LABELLED cannot restart at Work Model after that bar. |
| 182180 | none → onsite | Work Model: On-site at Appian Headquarters | LABELLED/PLACED_LABEL require the label at the segment start, not after a city and bar. |
| 1023982 | none → onsite | Job Type: Full-Time, Permanent, Onsite | LABEL lacks Job Type, and MODE_FIRST does not allow Permanent between Full-Time and the mode. Vendor activities onsite do not independently state candidate location. |
| 1269895 | none → onsite | Location(s): Welwyn office based | LABEL lacks Location(s); no mode-bearing subject or heading path is available. Vocabulary can read office based only after a label/statement qualifies it. |
| 77880 | none → onsite | Location: Washington, DC Metro, on-site at client facilities; occasional travel to other client locations. | PLACED_LABEL sees onsite but exceeds eight words for the full value. No short-clause label extraction; incidental travel is not separated. |
| 1371060 | none → onsite | Work Setup: Onsite (Mandatory) | LABEL omits setup; no other declaration path recognizes this explicit work-detail field. |
| 206303 | none → remote | 100% telecommuting role. | MODE_ROLE and ARRANGEMENT_LABEL omit telecommuting; Primary Work Location is not DETAILS. Can work remotely has no OWN_WORK opener without you. |
| 817049 | none → hybrid | Work setup: Hybrid (3x onsite, 2x WFH) | LABEL omits setup; scheduled() does not recognize bare 3x onsite without week or an OFFICE_DAYS day noun. HYBRID_PARTS would correctly keep hybrid if the label reached offered(). |
| 1193720 | none → hybrid | Work Location Assignment: Hybrid | LABEL does not include Assignment after Work Location, so the explicit field is ignored. |
| 1086286 | none → hybrid | Hybrid Working Arrangement (2 days WFH) | No declared week or OFFICE_DAYS office term; ARRANGEMENT_LABEL requires the label to end before the parenthesis. Benefits skips offer/company paths. See perk-versus-concrete-schedule note. |
| 129838 | none → hybrid | Hybrid Arlington, VA; Rome, NY: Hanscom, MA; Tacoma, WA | Work Type is absent from LABEL and DETAILS. MODE_HEADING_WITH_DETAILS matches the value but cannot run on an ITEM; place/mode helper expects a delimiter. |
| 176998 | none → remote | Location & Working Arrangement: Working Model: Remote (US) | LABEL does not support the ampersand-combined heading or nested Working Model; under Compensation only a BARE value is accepted. |
| 106942 | none → remote,hybrid | Location: Remote / Hybrid (Washington, DC metro preferred) | Location label is embedded after several other fields; anchored LABELLED/PLACED_LABEL miss it. Its first mode is too late for offer(). |
| 1017395 | none → onsite | Job location: Las Vegas, NV - Onsite within Clark County. | The job-location label follows Employer and Inc.; label extraction never scans a later sentence/span for it. |
| 287807 | none → remote | Mode: Remote | LABEL omits bare Mode; no qualifying subject, own-work opener or job-details heading. |
| 287835 | none → remote | Mode: Remote | Same missing bare Mode label as 287807. |
| 972250 | none → remote | Remote | Posting Type is not DETAILS or LABEL. A mode-only value is not HEADING; an unshown previous heading cannot trigger LABEL_ALONE adjacency. |
| 952679 | none → remote | 1 - US Remote | under: Location is not DETAILS. No label embedded in text, and PLACE_MODE does not support an opening numeric index and trailing bare mode without its expected separator. |
| 833234 | none → remote | GSK-Remote | TAG accepts LI-remote/hybrid/onsite, not employer-specific GSK-Remote; under: LI-GSK has no label interpretation. |
| 106692 | none → remote | Location: Primarily Remote. | MODE_FIRST omits Primarily; PLACED_LABEL captures the full multi-sentence value and exceeds eight words. The first label clause is not extracted alone. |
| 1239737 | none → hybrid | Work Model: You’ll work in a hybrid model | MODE_FIRST requires mode immediately after label; PLACED_LABEL rejects the long value. OWN_WORK is anchored before Work Model and THIS_ROLE has no you’ll-work alternative. |
| 1215546 | none → remote | REMOTE WORKING | HEADING is classified as LABEL_ALONE, excluded from ARRANGEMENT_LABEL; next displayed segment is training text, not VALUE. MODE_HEADING accepts mode alone but not the working suffix. |

## F5. The role’s own working arrangement expressed outside the narrow subject grammar

**Count:** 19; remote 10, hybrid 7, onsite 2, choice 0, none 0.

**Criterion:** Rules 1 and 3: “This vacancy, not the company”; “A sentence that states this role’s arrangement ... outweighs a bare label”. Rule 1 also says “The same hedges hold for this role’s own statement”.

**Verbatim deciding phrases**:

- 100897: “looking to speak to candidates who are based in Tokyo for our hybrid working model”
- 172409: “This role is a full-time role based in Sunnyvale, CA (hybrid)”
- 297208: “You’re welcome to work remotely from Canada.”
- 453204: “This Engineering Manager, Cloud Software Platform Practice position can work out remotely”
- 348062: “Telecommuting is permitted.”

**Root cause:** `THIS_ROLE` (273–276) has a small demonstrative/job/team grammar, and `statement()` (1220–1223) caps the subject-to-mode distance at ten cleaned words. `HIRING`/`HYBRID_ARRANGEMENT` (289–300) require specific adjacent forms. `OWN_WORK` (412–416) is anchored to a short opening list; `singleOffer()` (1002–1027) therefore misses invitations, permission and longer role names. `DESCRIPTION_SUBJECTS` (1292–1298) erases “hybrid environment” even when it describes this role. Some supplied settlements infer a mode from a qualification or support channel; see flags.

**IDs:** 100897, 172409, 270315, 1007217, 64049, 172289, 297208, 332184, 453204, 643460, 672442, 171756, 163856, 10927, 57089, 155516, 155519, 348062, 943953.

**Fix direction:** Extract bounded arrangement clauses with a vacancy/person subject, including explicit permissions and invitations, named positions, internships and hiring for a stated model. Avoid relying on total words from the first demonstrative when a later clause states the arrangement clearly. Treat a technology/environment stop phrase according to its grammatical role rather than deleting it globally. Preserve the distinction between being able or comfortable to work remotely and a declared arrangement; do not turn remote support channels into employee location.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 100897 | none → hybrid | looking to speak to candidates who are based in Tokyo for our hybrid working model | HIRING requires “looking for”, HYBRID_ARRANGEMENT requires a specific hiring opener; no role subject or own-work opener matches. |
| 172409 | none → hybrid | This role is a full-time role based in Sunnyvale, CA (hybrid) | statement() finds hybrid after more than ten cleaned opening words, exceeding STATEMENT_WORDS=10. |
| 270315 | none → hybrid | You’re welcome to work hybrid from Tel Aviv, Israel. | Neither THIS_ROLE nor OWN_WORK permits the invitation opener. Single-mode offer() returns nothing; singleOffer() also requires a description-only form rather than bare hybrid. |
| 1007217 | none → onsite | This is an exciting job opportunity for a PAM CyberArk Engineer onsite in Panama | THIS_ROLE finds “This is an”; statement() exceeds ten words before onsite. No independent named-job onsite predicate. |
| 64049 | none → onsite | This internship is a full-time position, in-person at our South San Francisco office. | THIS_ROLE does not admit “this internship”, and This ... position only allows two intervening letter/hyphen words, not this longer “internship is a full-time”. EVERY_DAY cannot bridge “full-time position,”. |
| 172289 | none → hybrid | This role is a full-time role based in Sunnyvale or Detroit (hybrid) | statement() exceeds ten opening words; companyModel() also rejects the long “At Wayve ... we operate ... policy that combines ...” sentence. |
| 297208 | none → remote | You’re welcome to work remotely from Canada. | OWN_WORK does not accept you’re welcome; THIS_ROLE has no matching person-arrangement form. offer() drops a single-mode offer. |
| 332184 | none → remote | As a fully remote role | THIS_ROLE lacks “a fully remote role” and MODE_ROLE is anchored at segment start; the embedded WORK LOCATION Remote field lacks a colon and is not scanned independently. |
| 453204 | none → remote | This Engineering Manager, Cloud Software Platform Practice position can work out remotely | THIS_ROLE allows at most two intervening words, so this long named position is not recognized. Secondary location/value text lacks a supported label/short place syntax. |
| 643460 | none → remote | a fully remote position | THIS_ROLE requires a this/the/this-is subject; MODE_ROLE requires mode at start; OWN_WORK does not permit the introductory Enjoy invitation. A concrete position declaration is lost with the promotional framing. |
| 672442 | none → remote | Can work remotely but need to live in the specified city, state, or region | OWN_WORK omits can work without you. Flexible Work Option is not a recognized details/label heading. Own onsite-service duties in the company paragraph need reconciliation before accepting remote alone. |
| 171756 | none → remote | we are open to remote candidates based elsewhere in the United States. | OPEN_TO_REMOTE is anchored at text start; it cannot read “Wasabi is based ... but we are open to ...”. No supported role subject or remote-team pattern follows. |
| 163856 | none → remote | You're comfortable working remotely on a distributed data science team | OWN_WORK omits comfortable working; no explicit joined remote-team pattern matches. This is a comfort/skill statement; settled remote is not securely supported. |
| 10927 | none → remote | looking to support our UAE office remotely | No declared role arrangement grammar matches; remotely may describe a support channel rather than the worker’s location. This should not become a blanket remote-support rule. |
| 57089 | none → remote | while working remotely with globally distributed teams. | OWN_WORK is anchored at working, not while working, and no other affirmative subject pattern matches. Under Required Skills the fragment’s meaning is uncertain. |
| 155516 | none → hybrid | will take advantage of STR’s flexible, hybrid environment | DESCRIPTION_SUBJECTS erases hybrid environment even for this vacancy; statement() then rejects distant remotely (>10 words before it). The role’s actual flexible arrangement versus rule 1 hedge is ambiguous. |
| 155519 | none → hybrid | will take advantage of STR’s flexible, hybrid environment | Same global subject erasure and distance guard as 155516, with a longer city list. |
| 348062 | none → remote | Telecommuting is permitted. | No permission predicate exists; OWN_WORK and MODE_ROLE do not match and offer() ignores a single mode. No stated onsite baseline means this is permitted remote, not a later exception. |
| 943953 | none → hybrid | we agree when to come to ING MAD and when to work best from home | No shared-agreement arrangement predicate; OWN_WORK requires an employee work opener and no other exact model/schedule pattern matches. No required regular count is stated; settlement uncertain. |

## F6. The vacancy is situated in an office without a canonical “this role is based” sentence

**Count:** 11; remote 0, hybrid 0, onsite 11, choice 0, none 0.

**Criterion:** Rule 3: “names where the work is done, and is onsite where nothing else speaks”; “Beside a schedule or a hybrid statement ... the mode is hybrid”.

**Verbatim deciding phrases**:

- 1236770: “Role based in Singapore office, with occasional travel”
- 259291: “Based out of our sunny office in the Mission District of San Francisco 5 days/week.”
- 290723: “Location: UK based from either our Bristol, Cambridge or London office.”
- 937056: “As an engineer in our Tuzla office”
- 748231: “this position which can be based in our Plano TX, Reston VA, or Washington DC office.”

**Root cause:** `sentence()` requires a `THIS_ROLE` match before applying `BASED_IN_AN_OFFICE` (1195–1213). The latter (446–448) requires a determiner/possessive and a short punctuation-limited office span. `WORKS_IN_AN_OFFICE` (529–536) requires specified work/join/hiring verbs and connectors. `onsite()` (1104–1107) rejects an entire sentence for unrelated incidental travel, remote support or a mode word. `NOT_STATED` (1224–1227) treats a geographic “may be based” alternative as a hedged mode.

**IDs:** 1236770, 1244008, 259291, 290723, 903533, 1308815, 547060, 937056, 1044956, 748231, 748273.

**Fix direction:** Read office-location assertions independently of a short demonstrative subject: role fragments, named jobs, departments and direct “in our office” clauses all can locate the vacancy. Allow ordinary location lists and parenthetical place names. Distinguish uncertainty over which office from uncertainty over whether office attendance applies, and scope occasional travel/weekend clauses separately. Keep the inferred office evidence below an actual schedule or hybrid arrangement.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 1236770 | none → onsite | Role based in Singapore office, with occasional travel | THIS_ROLE requires this/the before role; BASED_IN_AN_OFFICE also requires our/the/a/possessive before Singapore. No independent base-fragment reader; occasional travel would wrongly veto a whole-sentence onsite path. |
| 1244008 | none → onsite | You work at client locations and our office | WORKS_IN_AN_OFFICE expects office directly after a permitted determiner/location span; “client locations and our office” fails. The hybrid flexibility is conditional, not an offered mode. |
| 259291 | none → onsite | Based out of our sunny office in the Mission District of San Francisco 5 days/week. | BASED_IN_AN_OFFICE matches, but it is only applied after a THIS_ROLE subject, absent here. EVERY_DAY fails because office is before the count. |
| 290723 | none → onsite | Location: UK based from either our Bristol, Cambridge or London office. | PLACED_LABEL finds it but its 12-word value exceeds eight and contains no vocabulary mode. BASED_IN_AN_OFFICE omits based from and has no THIS_ROLE subject. |
| 903533 | none → onsite | join our Pricing Department at our corporate office in Warren, MI | WORKS_IN_AN_OFFICE permits joining a team, not a department; HIRING does not read an office well after searching for a named job. |
| 1308815 | none → onsite | out of our Dublin office | No WORKS_IN_AN_OFFICE verb immediately before out of; the preceding subject is a named role, not THIS_ROLE. |
| 547060 | none → onsite | this position in our New York Office | THIS_ROLE matches but BASED_IN_AN_OFFICE requires based/located; no direct role-in-office inference exists. |
| 937056 | none → onsite | As an engineer in our Tuzla office | Neither THIS_ROLE nor WORKS_IN_AN_OFFICE recognizes role-noun in-office syntax. Flexible perks elsewhere do not establish a different mode. |
| 1044956 | none → onsite | based in the office and standby occasionally on weekends | BASED_IN_AN_OFFICE matches but is only used within statement(), whose THIS_ROLE subject is absent. The whole-sentence NOW_AND_THEN guard would also confuse occasional standby with occasional office work. |
| 748231 | none → onsite | this position which can be based in our Plano TX, Reston VA, or Washington DC office. | BASED_IN_AN_OFFICE allows only six intervening punctuation-limited tokens and cannot consume this longer list; no substitution to onsite. Generic most-roles office policy is correctly hedged. |
| 748273 | none → onsite | This position has multiple openings and may be based in our Reston, VA (Reston Town Center) and Plano, TX (Granite Park) offices. | BASED_IN_AN_OFFICE rejects parentheses/long list; after a successful substitution, pre-mode may would still trigger NOT_STATED. Whether may selects offices or hedges attendance needs calibration. |

## F7. Required onsite work and own-site support with intervening words or punctuation

**Count:** 14; remote 0, hybrid 0, onsite 14, choice 0, none 0.

**Criterion:** Onsite definition/rule 2: “one whose own work is done at its users’ or customers’ site”; “one whose own work it is makes the job onsite”. Rule 3 applies to explicit requirements.

**Verbatim deciding phrases**:

- 1191207: “requires that you work on-site daily”
- 732866: “work is exclusively onsite at a client location”
- 202259: “Serve as the on-site technical owner for our Islamabad office”
- 30036: “Provide on-site cybersecurity and RMF sustainment support.”
- 1189769: “required to work full-time, on-site at our facility.”

**Root cause:** `REQUIRED_ONSITE` (467–470) requires near adjacency, excluding “requires that you”, report-in-person forms and punctuation after full-time. `EVERY_DAY` omits exclusively and punctuated modifiers. `SUPPORT_ONSITE` (487–489) admits only a fixed verb, zero/one technical adjective and a support noun, missing installations, longer disciplines and onsite ownership. `DESCRIPTION_SUBJECTS` deletes plain onsite support, so a failed specialized match has no fallback. `statement()` rejects a team mentioned as an aside (1204–1208), even for “join our team on site”. The specialized support reader has no broader duty/place fallback when that narrow regex fails.

**IDs:** 1191207, 570287, 732866, 209976, 163920, 202259, 30036, 1066550, 30007, 1427724, 1206017, 582792, 1189769, 81687.

**Fix direction:** Expand requirement syntax around clear obligation and onsite-location predicates, tolerating ordinary commas and intervening employee/role complements. Recognize own-site service duties by the relationship between duty, place and user, allowing discipline phrases and installations/ownership. Keep channels, remote support, vendor supervision and incidental visits out; an onsite word alone must not classify every responsibility. Respect actual schedules before defaulting a site requirement to onsite.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 1191207 | none → onsite | requires that you work on-site daily | REQUIRED_ONSITE does not allow “that you”; EVERY_DAY lacks daily-after-mode. statement() exceeds ten opening words before on-site. |
| 570287 | none → onsite | This role will include dedicated onsite support at a Malaysian Contract Manufacturer | DESCRIPTION_SUBJECTS erases onsite support; SUPPORT_ONSITE does not allow “include dedicated”. “Manage daily onsite execution” also has an intervening daily and unsupported execution noun. |
| 732866 | none → onsite | work is exclusively onsite at a client location | No work-is-performed-onsite predicate exists; EVERY_DAY omits exclusively. Neither THIS_ROLE nor OWN_WORK matches the direct work statement. |
| 209976 | none → onsite | Must be able to work full-time hours, in-person in our Toronto office | REQUIRED_ONSITE permits full-time immediately before onsite/in-office, not “hours, in-person”; EVERY_DAY has the same adjacency problem. WORKS_IN_AN_OFFICE cannot bridge the intervening hours clause. |
| 163920 | none → onsite | Provide customer support through the performance of on-site installation | SUPPORT_ONSITE requires the onsite phrase directly after the duty verb and a support/service noun; this places customer support before onsite installation. |
| 202259 | none → onsite | Serve as the on-site technical owner for our Islamabad office | No serve-as-onsite-owner predicate; SUPPORT_ONSITE omits serve and owner. Office work is asserted without a WORKS_IN_AN_OFFICE verb shape. |
| 30036 | none → onsite | Provide on-site cybersecurity and RMF sustainment support. | SUPPORT_ONSITE permits only one of technical/it/desktop/field, not a multiword discipline before support; generic support-at-site reading is absent. |
| 1066550 | none → onsite | This vital on-site support to USCYBERCOM | DESCRIPTION_SUBJECTS removes on-site support; specialized SUPPORT_ONSITE requires an explicit supported duty verb, absent in this nominal declaration. |
| 30007 | none → onsite | to join our team on-site at Scott AFB | THIS_ROLE matches our team but statement() rejects it as an aside after other text. WORKS_IN_AN_OFFICE expects join-team in/at an office, not on-site at a named facility. |
| 1427724 | none → onsite | to support the Commercial Product Cell onsite. | HIRING only handles a mode immediately after seeking/hiring; no duty-located-onsite predicate catches this longer named job/support clause. |
| 1206017 | none → onsite | within the on-site engineer team | THIS_ROLE finds the on-site engineer team but statement() rejects it as an aside; TEAM_OF_A_MODE supports remote/hybrid only. Whether this denotes the role’s own workplace warrants scope review. |
| 582792 | none → onsite | delivering white-glove support at the desk and remotely | No deskside/at-the-desk own-site-duty path. SUPPORT_ONSITE requires literal onsite; remote support channels alone cannot establish remote. Desk support as own work versus channel needs review. |
| 1189769 | none → onsite | required to work full-time, on-site at our facility. | The comma breaks REQUIRED_ONSITE and EVERY_DAY adjacency. Join our team on site is rejected as aside; negation of remote/hybrid/telework has no affirmative onsite fallback. |
| 81687 | none → onsite | Perform on-site inventories (physical and logical), assessments, and validation audits. | SUPPORT_ONSITE noun list does not cover inventories/audits. Remote work is not an option is not affirmative onsite evidence and is also missed as a postposed exclusion by generic label parsing. |

## F8. Company or joined-team arrangement with elaboration, variants or unrelated hedges

**Count:** 14; remote 4, hybrid 10, onsite 0, choice 0, none 0.

**Criterion:** Rule 1: “A company’s statement of how its people work, its model or its schedule, stated without a hedge, covers every role”; “A statement about the team this role joins ... is about this vacancy”.

**Verbatim deciding phrases**:

- 1400622: “We embrace a hybrid work model, creating the flexibility and freedom”
- 67926: “All of our roles are remote, however some roles may carry specific location-based eligibility requirements.”
- 1255713: “Protiviti practices a hybrid model”
- 279727: “We’re a team of 800+ employees, working remotely across 28 countries.”
- 942360: “A hybrid work model with a mix of in-office and remote days”

**Root cause:** `COMPANY_MODEL` (327–332) anchors a narrow subject/verb/mode/noun grammar to the entire sentence; “practices”, “is currently using”, working-remotely teams and explanatory trailing clauses do not match. `companyModel()` (961–969) additionally rejects any whole-sentence `some`, `may` or flexibility word, including geographic eligibility explanations. `TEAM_OF_A_MODE` (283–286) has no all-remote modifier. `stated()` (813–820) suppresses company extraction under selected perk headings. Bare model fragments do not satisfy `OWN_WORK` or `ARRANGEMENT_LABEL`.

**IDs:** 701669, 1400622, 701675, 67926, 769645, 67848, 205752, 302665, 1255713, 1257152, 937055, 279727, 335652, 942360.

**Fix direction:** Recognize the affirmative model/people-work clause and retain its scope through ordinary explanations. A hedge about eligibility or optional attendance must not hedge the stated remote arrangement; a phased future change must not cancel a currently stated arrangement. Include joined all-remote teams and equivalent model verbs without treating remote-first culture as a mode. Decide the flexibility/perk boundary noted below before changing blanket hedge behavior.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 701669 | none → hybrid | Our hybrid workplace also provides a high degree of flexibility. | COMPANY_MODEL requires a copular/model verb before the mode, not “Our hybrid workplace ... provides”. Other mentions are perks; settlement is ambiguous under rule 1’s flexibility hedge. |
| 1400622 | none → hybrid | We embrace a hybrid work model, creating the flexibility and freedom | COMPANY_MODEL cannot consume the trailing explanation; even if shortened to match its model clause, COMPANY_HEDGES rejects “flexibility” anywhere in the sentence. |
| 701675 | none → hybrid | Our hybrid workplace also provides a high degree of flexibility. | Same nominal-company-model and flexibility issue as 701669; additional mode mentions are explicitly benefits. |
| 67926 | none → remote | All of our roles are remote, however some roles may carry specific location-based eligibility requirements. | COMPANY_MODEL is whole-sentence anchored and rejects the eligibility continuation; COMPANY_HEDGES would additionally reject some/may even though they qualify location, not mode. |
| 769645 | none → hybrid | We are currently working in a hybrid model with some flexibility for remote working. | COMPANY_MODEL does not allow currently working after are or this continuation; COMPANY_HEDGES would reject some/flexibility. Its future phased rollout is correctly excluded by CHANGE_UNDERWAY. |
| 67848 | none → remote | As an all-remote team | TEAM_OF_A_MODE omits all-remote and the as-an opener; the separate company all-roles sentence fails the same continuation/hedge guards as 67926. |
| 205752 | none → hybrid | Quantifind is currently using a hybrid mix of working from home and in the office | COMPANY_MODEL cannot read is currently using, hybrid mix or its continuation; no supported own-role/team subject yields the mode. |
| 302665 | none → hybrid | while having a hybrid model. | No narrow COMPANY_MODEL clause at sentence start; the full statement is framed as flexibility/core values under Why join, which also suppresses company extraction. |
| 1255713 | none → hybrid | Protiviti practices a hybrid model | COMPANY_MODEL omits practices and cannot accept the following explanation; no other reader handles the model assertion. |
| 1257152 | none → hybrid | Protiviti practices a hybrid model | Same practices/continuation failure as 1255713. Variable ratio by client/team does not necessarily hedge the hybrid model itself. |
| 937055 | none → hybrid | a flexible hybrid work model tailored to local regulations. | COMPANY_MODEL cannot read the nominal perk/model fragment and COMPANY_HEDGES includes flexible. Settlement conflicts with rule 1’s flexibility/perk exclusions. |
| 279727 | none → remote | We’re a team of 800+ employees, working remotely across 28 countries. | COMPANY_MODEL lacks contracted people-working-remotely syntax; TEAM_OF_A_MODE requires a preceding mode adjective, not working remotely. Remote-first and stipends elsewhere are not valid replacements. |
| 335652 | none → remote | GreyNoise is a distributed team working remotely across the US and Europe. | COMPANY_MODEL cannot read this predicate; under Benefits suppresses the company path. The separate all-US-positions assertion does not cover this Germany vacancy, so it cannot justify the answer. |
| 942360 | none → hybrid | A hybrid work model with a mix of in-office and remote days | Nominal model fragment fits neither COMPANY_MODEL nor ARRANGEMENT_LABEL; no THIS_ROLE/OWN_WORK subject. A concrete mixture is declared, but scope under qualifications is less explicit. |

## F9. ATS or description location explicitly names an office/site rather than a mode token

**Count:** 8; remote 1, hybrid 0, onsite 7, choice 0, none 0.

**Criterion:** “What is read”: “its location as the ATS publishes it”; rule 3: an office the role is based in names where the work is done. Applicability to bare ATS office/site names is not explicit; see criterion notes.

**Verbatim deciding phrases**:

- 968930: “Corporate Manila Office”
- 1166916: “NLD, Client Site”
- 723573: “China - Shenzhen Office”
- 1325319: “United States - Boston Office”
- 792604: “Virtual Office (Texas)”

**Root cause:** `read()` (603–617) calls `modesNamedIn()` for the location; that method (625–637) only recognizes `Home -`/`Office -` at the beginning, `virtual location`, or vocabulary words. It has no terminal city Office, Client Site or Virtual Office interpretation; a corporate Home Office value in description details also has no appropriate location reader. A description’s `under: Location` is also absent from `DETAILS` handling (864–867).

**IDs:** 968930, 1166916, 1166918, 694426, 723573, 1325319, 1524684, 792604.

**Fix direction:** If the criterion confirms that a concrete office/site in the ATS location is itself the role’s base, recognize those location-field forms there, with city-only locations still unclassified. Handle Virtual Office as a field-specific remote label and protect corporate “Home Office” names from the remote-home-office vocabulary. Do not extend this to physical-environment sentences or company office lists.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 968930 | none → onsite | Corporate Manila Office | modesNamedIn() has no trailing Office interpretation; neither LOCATION_LABEL nor mode vocabulary matches. |
| 1166916 | none → onsite | NLD, Client Site | Location reader has no Client Site form. The only displayed description sentence is fraud boilerplate. |
| 1166918 | none → onsite | NLD, Client Site | Same location-field gap as 1166916; no description mode evidence. |
| 694426 | none → onsite | Client Site - USA - CA - Los Angeles - 800 S Hope Street | Client Site is unsupported in the location field. Description’s professional-office environment is explicitly non-mode evidence under rule 2. |
| 723573 | none → onsite | China - Shenzhen Office | LOCATION_LABEL only handles Office at the beginning followed by a dash, not Office as the named site’s suffix. |
| 1325319 | none → onsite | United States - Boston Office | No trailing Office location rule. Benefits mentioning in-office perks are not an arrangement. |
| 1524684 | none → onsite | APAC Home Office Hyderabad IN | modesNamedIn(location) sees no mode in Hyderabad; the under: Location description value has no recognized DETAILS context. Corporate Home Office must not become remote. |
| 792604 | none → remote | Virtual Office (Texas) | VIRTUAL_LOCATION only recognizes virtual location, and WORDS has no virtual office label. |

## F10. Non-English declarations of work model or permitted workplace

**Count:** 4; remote 0, hybrid 1, onsite 2, choice 1, none 0.

**Criterion:** “Where the work of this vacancy is done, as the vacancy declares it”; rules 1, 3 and 5 apply without a language exception.

**Verbatim deciding phrases**:

- 180257: “Presencial”
- 1398036: “nuestro modelo de trabajo híbrido”
- 30191: “Disponibilidade para atuar de forma presencial”
- 1169896: “dans leurs locaux, au sein de notre centre de services, ou en télétravail.”

**Root cause:** Vocabulary includes presencial and híbrido but almost all syntactic/heading patterns remain English: `LABEL`/`DETAILS` (217–221, 357–358), `COMPANY_MODEL` (327–332), `REQUIRED_ONSITE` (467–470), `OWN_WORK` (412–416). `télétravail`, French location/choice syntax and Spanish model prose have no corresponding path.

**IDs:** 180257, 1398036, 30191, 1169896.

**Fix direction:** Add language-specific declaration and job-detail-label forms using the same semantic categories and guard rules as English. Recognize workplace permission and an explicit office-versus-telework offer, not every token for office or hybrid. Keep statements about hybrid subject matter separate from the actual work-model field.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 180257 | none → onsite | Presencial | The mode value is recognized vocabulary, but “Modelo de trabalho” is neither DETAILS nor LABEL; the heading/value linkage is absent. |
| 1398036 | none → hybrid | nuestro modelo de trabajo híbrido | English COMPANY_MODEL and own-work grammar cannot read this Spanish declaration; the office/home schedule prose has no supported count form. |
| 30191 | none → onsite | Disponibilidade para atuar de forma presencial | No Portuguese availability/work predicate; bare presencial has no qualifying statement path. |
| 1169896 | none → remote,onsite | dans leurs locaux, au sein de notre centre de services, ou en télétravail. | No French employer/client-premises or télétravail vocabulary, and options() does not split ou. A declared location choice has no extraction path. |

## F11. Temporary arrangements, approved exceptions and an unfinished move

**Count:** 3; remote 0, hybrid 0, onsite 3, choice 0, none 0.

**Criterion:** Rule 4: “A mode that changes on a set plan ... is the mode it settles into”; rule 5: “may later be granted another, case by case ... is the mode it states”; rule 1 excludes “a change still underway”.

**Verbatim deciding phrases**:

- 766913: “transitioning to a full-time onsite working model through a phased rollout”
- 1365570: “Employees are required to report in person to this location for their first 30 days of employment.”
- 114656: “expect to be in a physical location within a few months.”

**Root cause:** There is no transition-plan resolver in `stated()` (782–893). `CHANGE_UNDERWAY` (565–566) simply excludes some onsite evidence; `NOT_REQUIRED` rejects possibilities, and the report-in-person requirement itself is absent from `REQUIRED_ONSITE`. The initial/final clauses are never linked. `sentence()` sees the first introductory role sentence in the move case but no later you-start/expect clause.

**IDs:** 766913, 1365570, 114656.

**Fix direction:** Distinguish an explicit committed end-state from an unfinished company rollout and an approval-dependent exception. Keep the stated initial requirement when a later change is discretionary; resolve to the final arrangement only when the vacancy actually commits to it. Add report-in-person duty-station evidence without interpreting every planned office search as a settled onsite job.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 766913 | none → onsite | transitioning to a full-time onsite working model through a phased rollout | CHANGE_UNDERWAY correctly excludes the rollout; “some roles ... require 100% onsite” is excluded by NOT_REQUIRED. Settled onsite lacks a role-specific every-day declaration; cohort setting is not sufficient by itself. |
| 1365570 | none → onsite | Employees are required to report in person to this location for their first 30 days of employment. | REQUIRED_ONSITE does not accept report in person; no duty-station-place path. Later hybrid is approval-dependent and correctly not a firm end-state; initial onsite obligation is missed. |
| 114656 | none → onsite | expect to be in a physical location within a few months. | No transition resolver or you-start-remote pattern; “physical location” is not itself an office/mode predicate. Final onsite settlement is questionable because the office search is still underway. |

## Other. Physical-setting or partial-duty inference with no clear mode declaration

**Count:** 2; remote 0, hybrid 0, onsite 2, choice 0, none 0.

**Criterion:** Rule 2 excludes “the physical surroundings of the work”; rule 6: “None where nothing read declares a mode for this job”.

**Verbatim deciding phrases** (only two rows in other):

- 968839: “General open office and campus environment.”
- 277626: “performing calibration and testing in lab and field environments.”

**Root cause:** No work-arrangement pattern matches either supplied passage. This is not an established missing extraction step: the settlements infer onsite from environment or one part of the duties.

**IDs:** 968839, 277626.

**Fix direction:** Readjudicate these before writing any rule. If the intended criterion includes a work-setting heading or any laboratory duty as a sufficient declaration, state that distinction explicitly in calibration rather than expanding office/lab keyword matching.

**Per-row trace** (quote = the deciding or disputed evidence; original rule → supplied settlement):

| ID | Rule → settled | Verbatim phrase | Why it fails / review qualification |
| --- | --- | --- | --- |
| 968839 | none → onsite | General open office and campus environment. | No declaration reader matches a physical-setting fragment. Rule 2 supports none unless the heading “Where You’ll Work” changes the meaning; flag for adjudication. |
| 277626 | none → onsite | performing calibration and testing in lab and field environments. | No explicit work-arrangement statement matches. This is partial-duty evidence, not necessarily a declaration of every working day onsite; readjudicate before adding a rule. |

## Flagged settlements

These are review flags, not revised ground truth. Keep them out of implementation feedback until readjudicated. Family counts above deliberately retain their supplied labels.

| ID | Supplied settlement | Assessment under revision 5 |
| --- | --- | --- |
| 766913 | onsite | **Likely adjudication error; none on shown evidence.** The only full-time onsite model is explicitly “transitioning ... through a phased rollout”, and current 100% presence applies to “some roles”. Rule 1 expressly excludes both. “Join ... an in-person, team-based setting” describes the cohort experience without establishing every-working-day attendance. |
| 937055 | hybrid | **Likely adjudication error; none or recalibration needed.** Evidence is “Flexible work arrangements” and “a flexible hybrid work model tailored to local regulations”, offered as well-being benefits. Rule 1 excludes a “flexibility” and says modes listed as a perk “declare nothing, wherever they sit”. No job-specific arrangement or required schedule appears. |
| 163856 | remote | **Likely adjudication error; none.** “You're comfortable working remotely” describes comfort/ability, not an arrangement. Rule 1 requires a statement about this vacancy’s work. A responsibilities heading alone does not turn a competency into its location. |
| 10927 | remote | **Likely adjudication error; none unless additional arrangement evidence exists.** “support our UAE office remotely” can be a support channel from another employer location; rule 2 expressly excludes channels the work covers. There is no independent employee-away-from-offices statement. |
| 1065764 | hybrid | **Not established by the criterion; onsite is supported by the separate office statement.** “a portion of the work in-person” supplies neither a regular schedule nor a statement that the remainder is away from the office. Hybrid requires “Part ... in an office on a regular schedule, and part away from it”. Do not add a portion→hybrid rule. |
| 277626 | onsite | **Insufficient evidence; likely none, but own-work boundary needs calibration.** Calibration/testing “in lab and field environments” is one duty, not an explicit every-working-day arrangement. The criterion includes a job whose work is at sites but does not say any laboratory task makes all work onsite. |
| 968839 | onsite | **Questionable; none on a physical-setting reading.** “General open office and campus environment” is explicitly setting language, which rule 2 excludes. Its “Where You'll Work” heading might instead identify an actual base; this needs a consistent heading-sensitive decision. |
| 114656 | onsite | **Questionable final-state inference.** “expect to be in a physical location within a few months” accompanies an active office search; no mandatory office arrangement is stated. Rule 4 requires a “set plan”, while rule 1 excludes a “change still underway”. Remote-to-office intent alone does not clearly settle onsite. |
| 943953 | hybrid | **Uncertain.** “we agree when to come ... and when to work best from home” may describe a joint recurring arrangement, or discretionary attendance. No regular floor is stated; rule 1 excludes “the flexibility to decide if and when to come in”. |
| 57089 | remote | **Uncertain.** “while working remotely with globally distributed teams” is a dangling fragment under Required Skills. It may state this vacancy’s work, or qualify experience expected of candidates. The shown fragment does not settle that scope. |
| 155516, 155519 | hybrid | **Uncertain hedge versus arrangement.** The role explicitly will use a “flexible, hybrid environment”, allowing remote when facilities are unnecessary. This can be an actual hybrid arrangement, but rule 1 applies the same flexibility hedges to the role itself. Avoid treating these as technology stoplist misses alone. |
| 701669, 701675 | hybrid | **Uncertain culture versus model.** “Our hybrid workplace ... provides a high degree of flexibility” can name an actual company workplace or advertise flexibility. Remaining hybrid mentions are benefits; no days or unhedged people-work predicate appears. |
| 302665 | hybrid | **Uncertain; leans none under the current hedge wording.** “while having a hybrid model” is embedded in a flexibility/core-values pitch under Why join. Rule 1’s actual-model inclusion and flexible-culture exclusion point in different directions. |
| 1215546 | remote | **Uncertain label boundary.** “REMOTE WORKING” is a heading, but rule 3’s examples say a heading naming “only the mode”, whereas this is also a conventional empty label head. No value is shown. The rule’s interpretation is defensible pending clarification. |
| 40322 | onsite | **Uncertain label evidence.** “Ll:Onsite” is not an established recruiting tag or named job-detail label in the input. The adjudication assumes a typo/abbreviation; confirm that interpretation before generalizing unknown colon labels. |

## Criterion notes requiring a consistent reading

1. **Model versus flexibility/perk.** Rule 1 includes “A company’s statement of how its people work, its model or its schedule, stated without a hedge” but excludes “naming a preference rather than an arrangement ... ‘flexible’” and says “Modes listed as a perk or a flexibility ... declare nothing, wherever they sit.” Decide whether explicit hybrid-model assertions with flexibility explanations count (1400622, 769645, 302665, 937055), and whether a concrete schedule listed under Benefits is stronger than that exclusion (994845: “50% Home Office”; 1086286: “Hybrid Working Arrangement (2 days WFH)”). An unrelated flexibility benefit should not automatically negate a required count or an already stated model.

2. **Location naming an office/site.** Rule 3 says “An office this role is based in” and contrasts it with “Based in <city> with no office named”. It does not explicitly state whether an ATS location such as “Client Site” or “Boston Office” counts as that base assertion. F9’s seven onsite settlements assume yes; “Virtual Office” additionally assumes a remote label. Confirm field semantics rather than interpreting every office token in a description the same way.

3. **Geographic alternatives versus a hedged mode.** Rule 1 says “The same hedges hold for this role’s own statement”; rule 3 accepts “based in either our Bristol or London office”. “can be based” / “may be based” with a list consisting only of offices (748231, 748273) plausibly selects which office, not whether the work is onsite. Make the scope distinction explicit before removing possibility guards.

4. **Own-site duties versus a channel or a partial task.** Rule 2 contrasts “onsite or remote support” with “one whose own work it is makes the job onsite”. Clarify how much of the work must be at users’ sites and whether deskside-and-remote support (582792), onsite inventory duties (81687), a technical onsite team (1206017), or one lab/testing duty (277626) is sufficient. “a portion ... in-person” (1065764) must not imply a regular hybrid schedule without further evidence.

5. **Planned end-state versus change underway.** Rule 4 requires “a mode that changes on a set plan”; rule 1 rejects “a change still underway”. An explicit dated final arrangement is different from office-search expectations (114656) or a phased company rollout (766913). The approval-dependent hybrid exception in 1365570 falls under rule 5, “may later be granted another, case by case”, so the required initial onsite state remains applicable.

6. **Specificity within the description.** Rule 3 says “The most specific statement about this job decides”; rule 1 says a team “this role joins” is vacancy evidence. Generic “Our hybrid teams” is not demonstrably the joined team when the role explicitly says remote (1196897, 1197072). A mode/schedule conflict across different segments (292251) should be resolved consistently with rule 4 rather than by collection tier alone. No blanket reorder of every company schedule above every role arrangement follows from these cases.

7. **What qualifies as a label or a work statement.** Rule 3 includes “a heading that names only the mode, or the job family and the mode, standing alone”; clarify REMOTE WORKING versus a value-less label (1215546), and unknown “Ll” versus a recruiting tag (40322). Rule 1’s vacancy-work requirement also needs consistent treatment of competency fragments (163856, 57089) and setting-only text under a where-you-work heading (968839).

## Verified probes and implementation handoff

Controlled probes confirm the mechanisms rather than merely correlating words: removing the redundant role modifiers lets the Sunnyvale sentence return hybrid; changing “three days in-office” to “three days in office” makes scheduled() return hybrid; putting office after the five-day count returns onsite; removing the monthly + makes the ten-day schedule return hybrid; changing “3 in-office days” to “3 days in office” returns hybrid; removing the comma in “full-time, on-site” returns onsite; truncating the GitLab sentence to “All of our roles are remote” returns remote; removing the earlier unrelated negation lets the four-day onsite schedule return hybrid; isolating the embedded Location label returns the remote/hybrid choice; shortening the explicit office-or-remote offer removes the size/hedge barriers; replacing enumerated office weekdays with a range returns hybrid. A clean model clause matches companyModel(), while the same clause followed by a flexibility explanation does not. Truncating the Primarily Remote label to its first sentence also restores remote without removing Primarily, confirming that the full-value length guard is decisive there. These are diagnostic substitutions, not proposed production rules.

Prioritize F3/F4/F5 for recall and F1/F2 for precision and spurious conflict. F6–F8 contain additional reusable language gaps. F9–F11 include several semantics that need the criterion notes resolved before expansion. This report contains row IDs and labels for review; any implementer feedback must use the abstract fix-direction paragraphs and supported criterion clauses, preserving the loop’s blind implementer boundary.
