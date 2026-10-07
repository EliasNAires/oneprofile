## Agent skills

### Issue tracker

Issues live as GitHub issues in `EliasNAires/oneprofile`, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage labels, used under their own names. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Code layout

Packages split into `storage/` and `workers/`, and every class is named by its role (`Run`, `Rule`, `Store`, …). See `docs/agents/code-layout.md`.

### Rule loops

An issue whose body has a `## Loop` section is calibrated (`/calibrate-loop #N`), then worked in rounds by two sessions under the frozen criterion: an implementer (`/mattpocock-skills:implement #N`) and a reviewer (`/review-round #N`). Read `docs/agents/rule-loop.md` before working such an issue in any role.
