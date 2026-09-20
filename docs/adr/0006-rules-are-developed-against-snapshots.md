# Rules are developed against snapshots on the development machine, not against production

Status: accepted

Extraction and classification rules are built and measured against a snapshot — a frozen
copy of the corpus held on the development machine — and never against the production
server, which holds live data. Each stage produces its own snapshot, and a snapshot is
archived rather than replaced.

## Considered options

- **Measuring inside the production server**, which is where the corpus already lives and
  where the previous iteration ran. Rejected on experience: the runs were hard to drive and
  hard to repeat there, and a rule under development is a thing you re-run a dozen times a
  day. Building the corpus locally costs a day of paced requests once, and buys every later
  rule an input it can be re-run over at will.
- **Working against a live database**, local or otherwise, kept current by continuing
  sweeps. Rejected because an accuracy figure is a statement about a specific input: if the
  vacancies move underneath the rules, two measurements taken a week apart cannot be
  compared, and a regression cannot be distinguished from a change in the market.

## Consequences

The development machine must hold the corpus and its snapshots, which is hundreds of
megabytes per stage, and the development database needs a volume so that a restart does not
destroy hours of crawling. Production keeps its own role: live data, served.

Snapshots are the interface between the step that builds the corpus and every step that
derives facts from it. A later stage restores the previous stage's snapshot rather than
rebuilding from Common Crawl, so the twelve-crawl run is paid for once.
