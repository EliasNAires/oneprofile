# Extraction stores pairs with their segments, and a plain key confirms a context key

Status: accepted

ADR-0017 leaves two things to #15: how skill extraction stores what it finds, and how a context
key is matched. ADR-0016 has discovery only mark which keys are context keys. Decided by Elias on
2026-10-19.

## What extraction reads

The vacancies of ADR-0017's scope: the title's in and the pile, without those set aside as
`unsupported_language`. In each, it reads the title as one more segment, and every segment of
the description that is not boilerplate. Boilerplate is the board's text about the company, not
about the vacancy, and skill discovery leaves it out for the same reason.

## Storage

`vacancy_skill` holds one row per (vacancy, skill), unique on the pair, as PRD §7 has it:

- the vacancy, by `normalized_vacancy`, deleted with it;
- the skill, by its id in `skills.tsv`, with no foreign key, because the taxonomy lives in the file;
- the indexes of the segments it was found in, empty when it was found only in the title;
- `in_title`, whether the title names it.

The body pass reads the segments: its rules look at where a skill appears (an item under the
requirements, or a sentence about the company), and they read that here rather than running a
second matcher.

A run replaces the rows of every vacancy it reads, and deletes the rows of every vacancy outside
its scope. After a run the table holds what that run found and nothing else, so a fix to the
taxonomy, or a vacancy the title classification moved out, leaves no stale row behind.

## Matching

- **A plain key** is found wherever it appears, as a name form (`TaxonomyStore`).
- **The longest key wins**, with no overlaps: Spring Boot over Spring, and Java is not found
  inside JavaScript.
- **A child does not add its parent.** AWS Lambda gives one row, not one for AWS too. Matching
  rolls a skill up to its parent, if it ever needs to, by reading the taxonomy.
- **A context key** is found only when both hold:
  1. it is written in the casing `skills.tsv` gives it: Go, not go, and not GO;
  2. a plain key confirms it: another skill found by a plain key in the same segment, or its
     parent or one of its children found by a plain key anywhere in the vacancy.

Only a plain key confirms, so two context keys never confirm each other ("Go and Spring" alone
finds neither). C and R go through the same rule.

Example, a vacancy titled "Senior Go Engineer":

| Text | Found | Why |
|---|---|---|
| Boilerplate: "Our platform runs on Kubernetes." | nothing | boilerplate is not read |
| Item: "Experience with Go, PostgreSQL and AWS Lambda." | Go, PostgreSQL, AWS Lambda | PostgreSQL is a plain key in the same segment, and confirms Go |
| Sentence: "You will go on call once a month." | nothing | lower case |
| Title | nothing more | no plain key in the title confirms Go |

## What is measured

#15 is done when extraction runs over its scope in minutes and reports the skills found per
vacancy and how many rows came from context keys. Its precision, which the launch gate needs
(PRD §12.5), is measured once, in #65. Only if that measurement fails does a rule loop open, in an
issue of its own, and it can reopen the casing rule with data.

#65 measured 0.945 overall (0.940 context, 0.945 plain), 0.005 short of the 0.95 mark. Elias
judged it good enough on 2026-10-19: no rule loop opens, since broad corrections risk breaking
more than they fix. Only the easy wins listed on #65 are taken (URLs matching HTTP, the bare SAP
key, C-level and R&D). The labels are in `docs/measurements/extraction-precision/`.

## Considered

- **Only the pair**, as PRD §7 had it. The body pass would then run the matcher again to know
  where a skill sits.
- **Insert-only re-extraction.** A taxonomy fix would never remove a wrong row.
- **Casing alone** for context keys. It fails at the start of a sentence and in title-case
  headings, where Go and Spring are written like the skills.
- **A rule per context key.** 332 keys today, more with every snapshot's discovery run.
- **All upper case accepted.** It finds GO in GO-TO-MARKET; headings in capitals are a small loss.
