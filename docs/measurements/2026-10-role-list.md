# Making the roles

Step 1 of #25, run on 2026-10-26 against the dev database restored from
`~/oneprofile-snapshots/classified-2026-10-25.dump`: the 155,767 vacancies of the engineering
subset, with the normalized titles as #94 left them. Its files are in `2026-10-role-list/`. The
result is `docs/role-criterion.md`: 27 roles.

## What was done

1. **A script narrowed.** `scripts/role-titles` exported every normalized title on at least 5
   vacancies: 2,541 titles, covering 78.0% of the subset (`titles.tsv`). The issue's 2,633 was
   counted before #94 merged duplicates. Each row has the vacancy count, the management levels, the
   most distinctive skills, the five titles nearest by skills, and three raw titles. The top 200
   (`top-200.tsv`) hold 57.8% of the subset; the 200th is on 82 vacancies.
2. **An agent drafted the roles from the top 200** (`draft.md`): 24 roles, with an estimated 70.9%
   of the subset over the titles on at least 5 vacancies.
3. **Another agent reviewed the draft** (`review.md`), under the subset as the IN criterion admits
   it (Q2 to Q4), and set it beside the Stack Overflow Developer Survey's developer types. Its 12
   required changes were all taken, and most of its optional ones. The main ones:
   - Shares are counted over every IN vacancy, titles on 1 to 4 vacancies included. Counted so,
     three kinds the draft had folded into other roles clear about 1% and became roles of their
     own: **business analyst** (1.06%, out of product manager), **security analyst** (0.96%, about
     1.3% with governance and compliance analysts, out of security engineer) and **robotics
     engineer** (0.92%, out of embedded engineer, whose description had been widened to hold it).
   - `systems engineer` (1,585) has no role: its raw titles mix IT systems work, defence systems
     engineering, presales, and software engineers whose title #92 misread.
   - `platform`, `infrastructure` and `software` before the head yield to a kind word in front of
     them; a word naming the sale sends a title to sales engineering; a career stage (`intern`,
     `graduate`) is read past; game and graphics work has no role rather than software engineer's.
   - Reasons for a title with no role are the fourth amendment's (under about 1% and covered by no
     role, or the title does not say one kind), never that the work is "not engineering".

Not taken from the review: the bare `technology` (103) follows the `engineering` precedent into
software engineer, which the review left open. Each role's share with its largest employer's share
beside it, which the review asked for, belongs to the measurement of step 6.

## Estimated shares

From the review's recount over all IN vacancies, rough to about ±10%. They are estimates for the 1%
floor, not the sorting of step 2 or the coverage of step 6.

- **No role is over 25%.** `software-engineer` is the largest, at about 16%; kept whole, since the
  areas titles name already have roles of their own.
- Every role clears about 1%. Nearest the floor: `robotics-engineer` 0.92%, `security-analyst`
  0.96%, `business-analyst` 1.06%, `frontend-engineer` about 1.3%, `forward-deployed-engineer` about
  1.3%.
- Coverage loses about a point to `systems engineer` against the draft's 70.9%, and gains the titles
  on 1 to 4 vacancies the draft did not count. Whether the 70% is reached is step 6's measurement.

## For other issues

Found while drafting and reviewing; nothing was changed for them here.

- **For #92, the breakdown:**
  - "Software Engineer, X Systems" reads as `systems engineer`, and "Software Engineer, Networking"
    as `networking engineer`, which gives software engineers another kind's head (265 of `systems
    engineer`'s 1,585).
  - "AI Training" and "AI Trainer" are dropped, so about 500 AI-training posts (0.32%) land in other
    titles: 118 of `ai developer`'s 265, 76 of `database administrator`'s 376, 64 in `ai engineer`, 53
    in `machine learning engineer`. Kept as `ai trainer`, the role list could leave them without a
    role.
  - A lone skill before the head goes, so "React Developer" reads as a bare `developer` and lands in
    software engineer, understating frontend and backend.
  - "Data & AI Engineer" reads as one title, `ai data engineer` (74% one employer's posting), where it
    may name two heads.
  - `intern`, `trainee`, `graduate` could be taken out as a level, as `senior` is.
  - `administrator`, `consultant` and `specialist` lose their platform word (Oracle, SAP, SharePoint)
    as a skill, which hides the kind of work.
- **For #11, the classification:** `mobile expert` (207, all T-Mobile retail sales), `mobile
  mortgage advisor` (62), `mobile mortgage specialist` (46) and `ai machine learning quality control
  specialist` (49, a language job) are IN and should not be.
- **For the blind sample (step 6):** report `solutions-architect` against `sales-engineer` first;
  `support-engineer`'s IT and product halves apart; the bare heads by ATS. `ingénieur` is 70% one
  employer, mostly embedded and industrial work.
