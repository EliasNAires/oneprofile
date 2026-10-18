# The deciding agent measured against #55's review (#64, 2026-10-18)

Step 2 of #64: which model decides a skill discovery run (ADR-0016), measured on #55's candidates
against the session's reviewed decisions (`skill-run-classified-2026-10-06.tsv`). Decided by
Elias: **Sonnet, in batches of 60, one headless `claude -p` process per batch**.

## Setup

- Five batches of 60 adjacent candidates (299 mentioned in a segment), sorted by compact name and
  spread across #55's pool, so pieces of one name sit together. Each was built against
  `skills.tsv` and `decisions.tsv` as they stood before #55 (`6bfc29c`), so no batch holds #55's
  own answers. Segments drawn with #55's seeds, as Jev saw them.
- The agent: `.claude/agents/skill-decider.md`. Per-candidate results of the Sonnet runs:
  `skill-decider-2026-10-18.tsv`.
- Agreement counts the agent's split and join as drops, and its spelling and new skill as keeps;
  full agreement also needs the same id on a keep.

## Agreement (Sonnet, 299 candidates)

- Keep or drop: 245/299 (82%). Same id where both keep: 99/110.
- By the review row's source: Jev's drops left by the session 106/119, Jev's keeps left by the
  session 91/110, the session's drops 29/57, the session's keeps 8/13.
- Of the 28 session drops the agent kept, 16 are "software no software person is hired for", which
  #55's session read as covering marketing, sales, finance, CAD, PLM, ERP, MES and AV software. The
  criterion's text names only AutoCAD, Epic EHR, SAP Concur and a CNC controller. Six more are
  "names several things, none dominant". Writing that reading into the criterion is a calibration.
- Some disagreements are #55's errors, not the agent's: A2A dropped by Jev though protocols are
  keeps, Databrick dropped though it is a misspelling of Databricks.

## Cost

| Run | Tokens | Thinking | Time | List cost | Keep/drop agreement |
|---|---|---|---|---|---|
| Sonnet, batch 1, as a general-purpose subagent | 116k | ~50k | 9 min | | 67% |
| Sonnet, batch 2, as `skill-decider` | 85k | 64k (the output cap) | 19 min | | 87% |
| Sonnet, batch 3, "decide clear entries on sight" | 82k | ~47k | 9 min | | 83% |
| Sonnet, batch 4, plus `effort: low` | 82k | ~47k | 8 min | | 87% |
| Sonnet, batch 5, headless, `CLAUDE_CODE_DISABLE_THINKING=1` | 37k out | 33,854 | 6 min | $0.65 | 87% |
| Sonnet, batch 5, headless, every thinking switch off | 54k out | 46,763 | 9 min | $0.92 | 85% |
| Haiku, batch 1 as one batch of 60 | 51k | | 3 min | | 81% of 47 decided; 13 left out |
| Haiku, batch 1 as two batches of 30, headless | 50k out | 46,794 | 4 min | $0.42 | 64%; 1 left out, no doubt marked |

- Reading (definition, criterion, batch) is about 34k tokens a batch; the rest is thinking, about
  800 tokens a candidate. Neither the agent's instructions, `effort`, nor
  `CLAUDE_CODE_DISABLE_THINKING`, `CLAUDE_CODE_DISABLE_ADAPTIVE_THINKING` or
  `MAX_THINKING_TOKENS=0` turns it off; the two batch 5 runs agree on 57 of 60 decisions.
- A whole run on `classified-2026-10-06` (2,054 candidates) is about 35 Sonnet batches,
  $23–32 at list price, more than one usage window.
- Haiku halves the cost but leaves entries out while reporting the batch done, marks no doubt and
  drafts more wrong ids (ADB as new beside `android-debug-bridge`), which moves work to the review.
