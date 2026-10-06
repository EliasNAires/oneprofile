# Rules are developed against snapshots on the development machine, not against production

Status: accepted (amended 2026-10-06)

Extraction and classification rules are built and measured against a snapshot — a frozen
copy of the corpus held on the development machine — and never against the production
server, which holds live data. A snapshot is archived rather than deleted.

Two snapshots are kept: the **raw corpus**, and the corpus **after classification**. The
reason for a snapshot is that the input must be reproducible. Cleaning is a deterministic pure
function that runs over the raw corpus in seconds, so restoring raw and re-running it
reproduces its output exactly. Classification earns its own snapshot for a different reason: it
is what the labelling attaches to, and what every later step reads. A label is a statement
about a specific vacancy in a specific state, and if the population being labelled can shift
under the labels, the measured accuracy stops meaning anything.

## When a snapshot is superseded

A re-sweep that changes ingestion, such as a reader that keeps more of what the ATS sends, cuts
a new pair of snapshots, raw and after classification, and the new pair becomes what dev
restores. The old pair moves to `~/oneprofile-snapshots/archive/` and is kept, because the
measurements made against it name it. The re-sweep reads the same boards again, so the
population moves with the market: whatever was measured against the old pair stays a statement
about the old pair, and the next measurement starts a new baseline. Once production exists,
this is the routine answer to drift.

## Considered options

- **Measuring inside the production server**, which is where the corpus already lives and
  where the pre-reset system ran. Rejected on experience: the runs were hard to drive and
  hard to repeat there, and a rule under development is a thing you re-run a dozen times a
  day. Building the corpus locally costs a day of paced requests once, and buys every later
  rule an input it can be re-run over at will.
- **Working against a live database**, local or otherwise, kept current by continuing
  sweeps. Rejected because an accuracy figure is a statement about a specific input: if the
  vacancies move underneath the rules, two measurements taken a week apart cannot be
  compared, and a regression cannot be distinguished from a change in the market.
- **A snapshot per stage**, one after each derivation pass. Rejected for the deterministic
  passes: a dump of each would cost 132 MB apiece to store what a re-run already gives.

## Consequences

The development machine must hold the corpus and its snapshots, which is hundreds of
megabytes each, and the development database needs a volume so that a restart does not
destroy hours of crawling. Production keeps its own role: live data, served.

Snapshots are the interface between the step that builds the corpus and every step that
derives facts from it. A later stage restores a snapshot rather than rebuilding from Common
Crawl, so the twelve-crawl run is paid for once.
