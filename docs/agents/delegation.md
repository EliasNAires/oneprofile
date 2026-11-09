# Delegation to Codex

The main session plans and supervises; Codex models do the reading and editing it hands out, on
the OpenAI quota instead of the Claude one. Both run through `codex exec` from Bash.

## When to delegate

Delegate a step when its edits or runs take more reading than its spec takes writing: edits
across several files, test-first work of several items, long test or script runs. A small
single edit stays with the main session.

Every delegated task goes to Codex, never to a Claude subagent: labelling, drafting and reviewing a
gold set, adjudication and a run's stages as well as edits (Elias, 2026-11-08). Where a skill, an ADR
or a `.claude/agents/*.md` file names an agent for a role, that file is the role's instructions, and
Sol runs them: its prompt points at the file and names the batch, the output and the paths it must
not open.

## Roles

- **Astra** (`gpt-6-astra`) audits a plan before any step runs. Only plans of three or more
  steps are audited; a shorter plan goes straight to work. Astra gets the plan's text in its
  prompt and runs `-s read-only`, told to judge the text alone, so it checks the plan's logic
  (missing steps, wrong order, unverifiable steps), not the code. The main session weighs its
  feedback and revises the plan; Elias is not asked.
- **Sol** (`gpt-6.1-sol`) executes the steps the plan delegates, `-s workspace-write`, network
  off. Sol never commits.
- **The main session** writes the plan, writes each Sol spec, verifies every step by `git diff`
  and the tests rather than by Sol's report, and commits.

## A Sol spec

Sol starts cold, so its prompt carries everything:

- The change, item by item, each with the test that shows it.
- The paths it may edit, the test command, and the paths it must not open. In a rule loop
  these are the paths the loop spec hides; the sandbox does not hide them, so the prompt must.
- These standing rules:
  - Read the class and its test class before editing; match their idiom (naming, Javadoc on
    every constant and method, tabs, comment density).
  - Test-first: for each item, add or extend a failing test, run it, change the code, rerun
    until green. Keep every existing test passing; if one contradicts the spec, stop and
    report it rather than change it.
  - Run only the given test command and `git diff`/`grep` on the given files. Never commit,
    never run other scripts. Everything written is in English.
  - Report in under 200 words: each item, done or not and why, and the final test result. No
    diff.

Run it with `-o <scratchpad>/sol-<step>.txt` and read only that file.

## Flow

1. Write the plan: numbered steps, each with the files it touches and how it is checked.
2. Three or more steps: send it to Astra, revise.
3. Run the delegated steps with Sol, verify each, commit.
