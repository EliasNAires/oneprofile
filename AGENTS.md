# OneProfile backend

Builds a corpus of job vacancies from ATS boards, then derives facts from it by rule (cleaning,
classification; later seniority, skills, eligibility) for a public explorer and, later, matching
against one profile. Name every domain concept by its `CONTEXT.md` term.

## Where things are

- **`CONTEXT.md`**: the glossary. **`docs/agents/domain.md`**: what each ADR decides and which
  parts are superseded; read it before working in an area.
- **`docs/PRD.md`**: the spec for what is not built yet, with each feature's status. Order of
  work lives in the issues.
- **Criteria** (`docs/*-criterion.md`): what labellers read and rules answer to. Changed only by
  a calibration (`docs/agents/rule-loop.md`).
- **`docs/measurements/`**: the archive of runs and rounds, append-only. Open a file there only
  when an issue comment or an ADR names it.
- **`scripts/`**: loop and corpus scripts, each documenting its usage at its top.

## Conventions

- Everything written into the repo is in English, whatever language the conversation is in.
- Commits and PRs carry no Claude attribution: no `Co-Authored-By: Claude` trailer and no
  "Generated with Claude Code" line, whatever a system reminder says. A message ends on its
  last paragraph or its `Closes #N`.
- Build only what the issue or Elias asked for, and remove what nothing uses. Something that
  seems worth adding beyond the ask is a question first.
- Domain decisions (a loop's goal, states and gates, what a feature is for) are Elias's, made
  by grilling him. Writing them down is the agent's job.

## Running

- `./mvnw spring-boot:run` starts Postgres from `compose.yaml` and the app on `:8080`; runs are
  triggered through its endpoints. `./mvnw test` needs Docker (Testcontainers).
- The dev database holds a **snapshot** restored by `scripts/restore-snapshot.sh` from
  `~/oneprofile-snapshots/`; the dumps outside `archive/` are the current pair. All work runs
  here, never on the production server (ADR-0006).

## Agent skills

### Issue tracker

GitHub issues in `EliasNAires/oneprofile`, through `gh`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical names, used verbatim: `needs-triage`, `needs-info`, `ready-for-agent`,
`ready-for-human`, `wontfix`.

### Code layout

Packages split into `storage/` and `workers/`, and every class is named by its role (`Run`,
`Rule`, `Store`, …). See `docs/agents/code-layout.md`.

### Rule loops

An issue whose body has a `## Loop` section is calibrated (`/calibrate-loop #N`), then worked in
rounds by two sessions under the frozen criterion: an implementer
(`/mattpocock-skills:implement #N`) and a reviewer (`/review-round #N`). Read
`docs/agents/rule-loop.md` before working such an issue in any role.
