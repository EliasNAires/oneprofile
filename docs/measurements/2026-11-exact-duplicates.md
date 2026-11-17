# Exact duplicates (#106)

2026-11-17, on the dev snapshot, with the duplicate pass (`POST /duplicates`, `DuplicateRun`,
`DuplicateRule`) that replaces #27's vacancy groups. The pass marks a vacancy of the engineering subset
(155,767) a **Duplicate** (`CONTEXT.md`) of another of the same slug with the same title, description,
declared locations and work modes, title and description equal once whitespace is collapsed and case folded. Nothing is
stripped and no threshold is set. As #106 decided, it is measured lightly: 40 random merged pairs labelled
blind against `docs/duplicate-criterion.md` revision 2, gate **merge precision ≥ 95%**, and recall read off
#27's labelled sample at no labelling cost.

**Result.** The gate passes: **39 of 39** clear pairs are duplicates (100%, 95% Wilson interval 91% to
100%), one pair unclear. The pass marks **1,976 duplicates** of 1,552 kept vacancies, leaving **153,791
vacancies shown**. Recall on #27's sample is **7 of 22** labelled duplicates (32%).

## How the pass was read

1. **Title** is the vacancy's title as its board publishes it (`vacancy.title`), not the cleaned title;
   **description** is the stored one (`vacancy.description`, the marked plain text every adapter writes),
   not the cleaning's segments. Compared exactly, these reproduce the issue's 7,281 vacancies repeating
   title and description (7,269 today); folding whitespace and case raises it to 7,466.
2. **Locations** are the declared location strings as a set, each with the country its ATS declares for
   it (Ashby's), whitespace collapsed and case folded; **work modes** as a set.
3. **A blank description** never merges: the criterion calls such a pair unclear, and precision comes first. 29
   vacancies of the subset have none.
4. **The kept vacancy**, within each set of equal vacancies: the board whose oldest vacancy in the set was
   published first (a board with no dated vacancy last, ties to the lowest company id), and on it the one published last
   (dated before undated, ties to the highest vacancy id).
5. **Marks are replaced whole**: each run clears every mark and writes its own in one transaction, so a
   vacancy that left the subset or stopped repeating is shown again.

## The run

`POST /duplicates` read 155,767 vacancies and marked 1,976 in 9 seconds. None is marked the duplicate of
another duplicate, none lies outside the subset, and 6 repeat a vacancy on another ATS's board of the same
slug. Comparing each location's declared country, added on review, left the count unchanged: 1,976
again, the 40 sampled pairs all still marked. The issue's 1,890, counted with exact text, rises to 1,976 by the folded whitespace and case.

## The check

`scripts/draw-duplicate-check` drew 40 marked pairs at random (seed 106) into
`2026-11-exact-duplicates-sample/`, in two batches of 20 showing each pair's company, title, locations,
work modes and description segments, the kept one not named. Sol (`codex exec`) labelled each batch blind
by `.claude/agents/duplicate-labeller.md`, reading only the criterion and its batch.
`scripts/score-duplicate-check` scores them:

| Answer | Pairs |
|---|---|
| duplicate | 39 |
| another vacancy | 0 |
| different position | 0 |
| unclear | 1 |

The unclear pair (34: 434217 of 434205) is a description that withholds its duties ("classified"); it is
left out of the share, as the criterion's *unclear* is.

## Recall

Of #27's 300 sampled pairs, 22 were labelled *duplicate* (revision 1 meant the same as revision 2), all
still in the subset. The pass merges 7. Of the 15 it leaves apart, 8 differ only in the description, 5 in
title and description and 2 only in the title: near copies, a sentence, a requisition number or a notice apart, which
#106 leaves apart on purpose. Allowing them would be a later change, measured on its own.

## Open

- Near copies (above) and duplicates on an aggregator's or a staffing firm's slug are not matched.
