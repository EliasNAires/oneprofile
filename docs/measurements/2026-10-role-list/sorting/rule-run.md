# The role rule over the exported titles (#25, step 2)

2026-10-26, on `classified-2026-10-25`. The role rule (`workers/role/RoleRule`) reads the role list
in `src/main/resources/rolelist/` (the roles, the words of a title that give them, and the titles the
rule gets wrong), each line taken from `docs/role-criterion.md`. Run with

    scripts/role-sorting docs/measurements/2026-10-role-list/titles.tsv \
        docs/measurements/2026-10-role-list/sorting/rule.tsv \
        docs/measurements/2026-10-role-list/sorting/answers-011.tsv

`rule.tsv` is its output: each title's role, the reading that gave it, and the roles other words of the
title give where they disagree (`competing`).

## Against the criterion

Every title the criterion lists under a role (199) and every worked example at a role's edge (86) gets
the criterion's role. Seven of the criterion's own titles come out contested although right (`ai data
engineer`, `full stack ai engineer`, `ai solutions engineer`, `ai solutions architect`, `ai data
scientist`, `digital network exploitation analyst`, `it business analyst`); the agent pass can take
them as settled.

## The split

Over the 2,541 titles on at least 5 vacancies (122,238 vacancies, 78.0% of the subset):

| | titles | vacancies | share of these |
|---|---:|---:|---:|
| placed | 1,572 | 104,304 | 85.3% |
| no role, by a rule | 98 | 6,708 | 5.5% |
| unreached | 871 | 11,226 | 9.2% |
| contested (within the above) | 244 | 3,532 | 2.9% |

Placed is about 66.6% of the whole subset from these titles alone; the tail (titles on 1 to 4
vacancies) and the 70% are step 6's.

- **Unreached** titles have a head or front words the criterion does not name: `android team lead`
  (248), `technical product` (178), `performance engineer` (139), `application developer` (122),
  `application engineer` (116), `data analytics` (99), `quantitative analyst` (75). The rule gives them
  no role rather than a guess.
- **Contested** titles have two kinds of work in front of the head; the nearest decides. Most are an
  `ai` in front of another kind (`ai security engineer`, `ai research engineer`).

## Against the pilot's answers

`answers-011.tsv` (97 titles, Sonnet under the frozen criterion): 70 agree.

| rule's reading | agree | disagree |
|---|---:|---:|
| placed, uncontested | 45 | 6 |
| contested | 9 | 7 |
| unreached | 16 (both no role) | 14 |

- **Unreached, label has a role (14)**: `dataops`, `dataflow`, `cyber defense`, `demo`, `deployed`,
  `desktop`, `desktop workstation design`, `device`, `edge`, `endpoint`, `erp`, `escalation`,
  `customer engagement`, `distinguished` engineer. Words the criterion does not name; each an agent
  confirms becomes a line of the list.
- **Contested, the nearest kind lost to the label (7)**: `business intelligence data engineer`,
  `storage data engineer`, `cloud database engineer`, `network deployment engineer`, `network devops
  engineer`, `ai devops engineer`, `ai developer experience engineer`. Labels to adjudicate.
- **Placed uncontested against the label (6)**: `test development engineer`, `test and evaluation
  engineer` (hardware or defence test, not QA), `ai infrastructure dc design engineer`, `mission data
  systems design engineer`, `embedded systems emulation engineer`, `software and data engineer`. The
  rule reads the kind word the criterion gives; the labels read the raw titles and skills. Labels to
  adjudicate, or exceptions.

Rule faults the comparison found were fixed before this run: `systems software development engineer`
(now embedded, with `development engineer` read as a developer), and the rule-9 titles (`design
engineer`, `automation engineer`, `systems engineer`, `verification engineer`) read as heads with no
role, so that a kind in front of them decides (`web design engineer` is frontend).

## The agent pass (2026-10-27)

For #25, step 2's agent pass adjudicated the 27 titles above where the rule and the pilot's labels
disagree. Two adjudicators (`.claude/agents/adjudicator.md`, Sonnet; 38k and 45k tokens) read each
title's raw titles and skills against the criterion, blind to which answer was the rule's. The
batches, the key to which answer was which, and the verdicts are in `adjudication/`.

- **The label stands on 25.** Each became a line of the role list: in `words.tsv`, `dataops`
  (data engineer), `cyber defense` (security engineer, and security analyst beside `analyst`),
  `desktop` and `endpoint` (systems administrator), `escalation` (support engineer), `demo` and
  `customer engagement` beside `engineer` (sales engineer), `deployed` beside `engineer` (forward
  deployed engineer), and `erp` deciding wherever it stands, as the vendor platforms do (enterprise
  platform engineer); in `titles.tsv`, the 15 titles no word generalizes.
- **The rule stands on 1**: `test and evaluation engineer` stays QA; its skills are software test
  automation.
- **Neither on 1**: `software and data engineer` names two heads, so it is two normalized titles, a
  fault of the title breakdown (#95). The rule's `data-engineer` is left.

Re-run over the 2,541 titles, the rule agrees with 95 of the 97 pilot labels (from 70). Thirty
titles changed role: the 25 adjudicated, and five the new words reach beyond them, checked by hand:
`cyber defense` and `cyber defense analyst` (security engineer and analyst), `erp solution
architect`, `erp systems administrator` and `erp analyst` (enterprise platform engineer).

| | titles | vacancies | share of these |
|---|---:|---:|---:|
| placed | 1,586 | 104,441 | 85.4% |
| no role, by a rule | 101 | 6,770 | 5.5% |
| unreached | 854 | 11,027 | 9.0% |
| contested (within the above) | 241 | 3,505 | 2.9% |

## Next

Step 6, by another session. The roles in the dev database are still the old rule's: first `POST
/roles` and `POST /role-nearness` again, then `scripts/draw-role-sample` (written, run once into a
scratch directory to check it) draws the blind sample, two labellers label `batch-1.tsv` and
`batch-2.tsv`, and `scripts/score-role-sample` (written, not yet run on labels) scores them.
