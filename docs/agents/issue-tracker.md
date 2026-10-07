# Issue tracker: GitHub

Issues and specs live as GitHub issues in `EliasNAires/oneprofile`; `gh` infers the repo from
the clone. "Publish to the issue tracker" means create an issue; "fetch the ticket" means read
one.

- **Read**: `gh issue view <n> --comments`, or `gh issue view <n> --json title,body,labels,comments`
  to filter with `--jq`. `--comments` and `--json` can't be combined.
- **List**: `gh issue list --state open --limit 100 --json number,title,labels`, narrowed with `--label`;
  add `body`/`comments` to the fields only when you need them.
- **Create**: `gh issue create --title "..." --body-file <file>` (or a heredoc for the body).
- **Comment**: `gh issue comment <n> --body-file <file>`.
- **Labels**: `gh issue edit <n> --add-label "..."` / `--remove-label "..."`.
- **Close**: `gh issue close <n> --comment "..."`.

Rule-loop issues are the ones whose body has a loop spec:
`gh issue list --limit 200 --json number,title,body --jq '.[] | select(.body | test("## Loop")) | "\(.number) \(.title)"'`.

**PRs as a request surface: no.** Work is committed to `main`; `/triage` covers issues only.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: an issue labelled `wayfinder:map` (create the label on first use), holding the
  Notes / Decisions-so-far / Fog body.
- **Child ticket**: an issue linked to the map as a GitHub sub-issue (`gh api` on the sub-issues
  endpoint), labelled `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`), assigned to
  the driving dev once claimed.
- **Blocking**: native issue dependencies:
  `gh api --method POST repos/EliasNAires/oneprofile/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`,
  where `<blocker-db-id>` is `gh api repos/EliasNAires/oneprofile/issues/<n> --jq .id` (not the
  `#number` or `node_id`). `issue_dependencies_summary.blocked_by` counts open blockers.
- **Frontier**: the map's open sub-issues with no open blocker and no assignee; first in map
  order wins.
- **Claim**: `gh issue edit <n> --add-assignee @me`, the session's first write.
- **Resolve**: `gh issue comment <n> --body "<answer>"`, `gh issue close <n>`, then append a
  gist and link to the map's Decisions-so-far.
