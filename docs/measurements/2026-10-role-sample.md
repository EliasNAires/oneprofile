# The roles measured on a blind sample (#25, step 6)

2026-10-27, on `classified-2026-10-25`, with the role list of 690c8d3 (after step 2's agent pass).
`POST /roles` and `POST /role-nearness` were run again first, then `scripts/draw-role-sample` and
`scripts/score-role-sample` into `2026-10-role-sample/`: the draw (`sample.tsv`), what the labellers
saw (`batch-1.tsv`, `batch-2.tsv`), their labels (`labels-a-*.tsv`, `labels-b-*.tsv`), the
disagreements and the adjudication (`disagreements.tsv`, `adjudicated.tsv`).

## Coverage

Of the 155,767 vacancies of the engineering subset, **120,986 (77.7%) have a role**, above the 70%
of ADR-0024; 416 have two. Step 5's run, before the agent pass, had 120,772.

| ATS | vacancies | with a role |
|---|---:|---:|
| Workday | 78,913 | 71.5% |
| Greenhouse | 41,727 | 82.4% |
| Ashby | 22,575 | 87.5% |
| Lever | 12,552 | 82.8% |

**Beyond the sorted titles.** 33,942 vacancies carry only normalized titles on 1 to 4 vacancies; the
rule gives 17,158 of them (50.6%) a role.

## The roles

Share of the subset; a vacancy with two roles counts in each. None is over 25%.

| role | vacancies | share | precision |
|---|---:|---:|---:|
| software-engineer | 26,120 | 16.77% | 8 of 11 |
| platform-engineer | 10,774 | 6.92% | 10 of 11 |
| data-engineer | 7,333 | 4.71% | 7 of 11 |
| ai-engineer | 6,929 | 4.45% | 9 of 11 |
| security-engineer | 6,265 | 4.02% | 11 of 11 |
| full-stack-engineer | 5,946 | 3.82% | 10 of 11 |
| product-manager | 4,731 | 3.04% | 11 of 11 |
| solutions-architect | 4,196 | 2.69% | 9 of 11 |
| qa-engineer | 3,896 | 2.50% | 11 of 11 |
| data-scientist | 3,799 | 2.44% | 11 of 11 |
| systems-administrator | 3,752 | 2.41% | 11 of 11 |
| machine-learning-engineer | 3,562 | 2.29% | 10 of 11 |
| backend-engineer | 3,515 | 2.26% | 10 of 11 |
| support-engineer | 3,274 | 2.10% | 11 of 11 |
| sales-engineer | 2,818 | 1.81% | 10 of 11 |
| network-engineer | 2,675 | 1.72% | 11 of 11 |
| enterprise-platform-developer | 2,524 | 1.62% | 11 of 11 |
| embedded-engineer | 2,459 | 1.58% | 9 of 11 |
| program-manager | 2,426 | 1.56% | 11 of 11 |
| data-analyst | 2,380 | 1.53% | 9 of 11 |
| mobile-engineer | 2,310 | 1.48% | 9 of 11 |
| forward-deployed-engineer | 2,058 | 1.32% | 11 of 11 |
| frontend-engineer | 1,969 | 1.26% | 9 of 11 |
| business-analyst | 1,768 | 1.14% | 10 of 11 |
| research-scientist | 1,689 | 1.08% | 8 of 11 |
| security-analyst | 1,123 | 0.72% | 11 of 11 |
| robotics-engineer | 1,111 | 0.71% | 8 of 11 |

`robotics-engineer` and `security-analyst` stay under the 1% the review estimated; the agent pass
did not raise them.

## The blind sample

**How it was drawn.** 11 vacancies per role, spread over the ATSs in proportion to the role's
vacancies on each, and the bare heads topped up to 5 per ATS: 312 vacancies, 15 of them topped-up
bare heads, 39 of the drawn ones on titles carried by 1 to 4 vacancies. Seeded (25).

**Who labelled.** Two labellers (Sonnet, each run on both batches separately) read
`docs/role-criterion.md` and, per vacancy, only the company and the raw title, and gave each a role
slug, two, or `none`. They never saw the normalized title or the rule's answer. An adjudicator
(Sonnet) read the 8 vacancies they split on, with both answers and reasons, under the same criterion.
About 490k tokens in all.

**Agreement: 304 of 312 (97.4%).** Both labellers are the same model reading the same criterion, so
this says the criterion reads the same way twice, not that two kinds of reader agree. The pairs they
split on: `data-analyst` ~ `none` 2; `embedded-engineer` ~ `sales-engineer,solutions-architect`;
`machine-learning-engineer` ~ `none`; `none` ~ `platform-engineer`; `none` ~ `robotics-engineer`;
`none` ~ `software-engineer`; `sales-engineer` ~ `solutions-architect`. The adjudicator took a's
answer on 3 and b's on 5.

**Precision.** Of the vacancies drawn under a role, the share whose settled label holds it:

| | right | precision | 95% interval |
|---|---:|---:|---|
| every role, 11 each | 266 of 297 | 89.6% | 86% to 93% |
| weighted by each role's share of the subset | | 86.1% | |
| titles on 5 or more vacancies | 237 of 258 | 91.9% | 88% to 95% |
| titles on 1 to 4 vacancies | 29 of 39 | 74.4% | 59% to 85% |

With 11 per role, a role's own interval is wide (8 of 11 is 43% to 90%); the per-role figures say
where to look, not how good a role is. The weighted figure counts `software-engineer` (8 of 11) and
`data-engineer` (7 of 11) for their size.

**Bare heads by ATS** (rule 8, `software-engineer`): Ashby 5 of 5, Greenhouse 5 of 5, Lever 2 of 3,
Workday 2 of 4. Workday's two misses are not bare work: `Azure Engineer Senior Associate` and `Oracle
Database Developer II (PL/SQL)` became bare heads because the breakdown took the word that decides
as a skill.

## Where the rule and the labels split

The 31 misses, by the stage that made them:

- **The breakdown dropped or moved the word that decides (10).** A team after the head moved in
  front of it as a kind word: `Software Engineer, Data Acquisition` and `Software Engineer, AI Data`
  → `data engineer` (labelled software engineer), `Middle C# Developer (Data Management)` → `data
  developer`, `Software Engineer - Flutter Studios` → `flutter engineer`, `Pessoa Desenvolvedor
  Back-end Senior (AI Advocate)` → AI. A skill that decides was dropped: `Azure Engineer`, `Oracle
  Database Developer`, `Architect - Enterprise Application - SAP`. `Ingénieur Data Scientist` lost
  its head to `data ingénieur`. `Software Engineer, Stablecoin` lost the blockchain word, which the
  criterion lists without a role. These are #95's.
- **A kind word in front of a head that says no one kind of work (9).** Rule 4 gives the kind word
  the role "whatever the head", and the labellers read rule 9's heads as no role:
  `web analyst`, `data management analyst`, `data transformation analyst`, `research development
  technology management analyst`, `robotics specialist field engineer`, `robotics operations
  technician`, `robotics product specialist`, `ai coding specialist` (an AI-training post), and a bare
  `machine learning intern`. The criterion does not say which of rules 4 and 9 wins; this is the
  largest group the rule can fix, and is a criterion question first. Three of `robotics-engineer`'s
  misses are here.
- **Two kind words, or a kind word against a job read whole (10).** `Data Backend` → data, not
  backend; `Cybersecurity Systems Analyst` → security analyst, not `systems analyst` read whole;
  `Lead Security Mobile Engineer` → security; `AI Product Engineer` → AI; `Storage Systems/Software
  Engineer` → platform (storage), not embedded (systems software); `Analytics Solution Architect` →
  data engineer; `Vulnerability Research Engineer` → security; `Quantitative MBS Research Analyst` →
  data scientist; `Solutions Platform Engineer` → platform, not sales; `Field Application Engineer –
  Embedded Solutions Architect` → sales engineer and solutions architect.
- **Not engineering work, leaked past the classification (2).** `Front End Lead` at a grocery chain
  (the checkout), `Ceded Reinsurance Specialist, Platform Partner Syndicates`.

## Left without a role

The most common normalized titles of vacancies with no role, the input for the next snapshot:
`systems engineer` 1,560, `analyst` 836, `automation engineer` 712, `consultant` 333, `android team
lead` 248, `mobile expert` 207, `design engineer` 185, `technical product` 178, `technical
consultant` 170, `gtm engineer` 139, `performance engineer` 138, `application developer` 122,
`specialist` 121, `design verification engineer` 119, `application engineer` 115, `verification
engineer` 111, `financial analyst` 108, `field engineer` 106, `intern` 106, `data analytics` 99,
`technical writer` 97, `analytics` 90, `scientist` 86, `stage` 86, `developer relations` 82, `mech
and robotics tech` 81, `data` 80, `quantitative analyst` 74, `ux researcher` 74, `analista` 70.

The agent pass did not reach these: it settled only the pilot batch's disagreements. `android team
lead`, `application developer` and `technical product` are the commonest that a line of the role list
would place.

## Role nearness

Run again after the assignment. Each role's three nearest are the same roles as in
`docs/measurements/2026-10-role-assignment.md`, each within 0.01, so its table stands.
