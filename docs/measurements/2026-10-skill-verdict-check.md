# Jev's skill decisions against the #38 and #40 reviews

Run on 2026-10-06 for #52, before any Jev decision of skill discovery (ADR-0013) is trusted. Jev
(`jev-1.13.0`) decided keep or drop for 400 names the #38 and #40 reviews had already decided,
under revision 1 of `docs/skill-criterion.md`, and the two were compared. **The check passed**:
once the disagreements are hand-read, Jev agrees with the right decision on 343 of 400 names,
85.8%, 95% interval 82.0%–88.8%, above the 80% bar ADR-0012 sets for its labeller check.

## The sample

`scripts/skill-verdict-check 1 400`, its full output in `skill-verdict-check-1.txt`, the names it
drew in `skill-verdict-check-drawn.tsv`.

- **Drawn from** the 3,437 names of `decisions.tsv` that came from the reviews and that a reviewer
  decided under the review rules. Left out: the 334 names #40 never reviewed (not in the record)
  and #40's 5,699 `screened out` drops, decided on the name alone.
- **Stratified** half keeps (200) and half drops (200). The future candidate stream at the
  precision gate's 50% looks like that. The drops are split by the reason their note gives, mapped
  onto the criterion's drop reasons by `skillloop.drop_reason`, in proportion to how many each
  reason dropped, at least one each.
- **Jev read** the criterion, the name as the review spelled it, and three segments mentioning it
  in `classified-2026-10-06.dump`: the requirement text of the IN vacancies and the pile, English
  and Spanish, boilerplate and headings left out. It answered keep or drop, and one of the twelve
  categories.
- **Passed over** were 91 names mentioned in no segment of the snapshot, 74 of them `key noise`
  drops: names the reviews dropped because their honest form matched almost nothing. This is an
  exclusion #52 does not name. It is made because a candidate always comes with segments, so
  these are not what Jev will be asked. It leaves `key noise` drawn from the 24 of its names the
  snapshot still mentions.
- **Fewer than three segments** were read for 65 names: 36 are mentioned in fewer than three
  distinct segments, and 29 of the first 200 were given the same text more than once, one ad
  posted as several vacancies, before the draw was made to take distinct texts. Their labels are
  kept: each is a statement about what Jev read, which the cache stores with it.

The check was run at 200 first (79.5%, 73.4%–84.5%: undecided) and grown to 400 with the same
seed, so the 400 begin with the 200.

## Agreement

| | Jev keep | Jev drop |
| --- | --- | --- |
| Reviews keep | 165 | 35 |
| Reviews drop | 50 | 150 |

**Raw agreement: 315/400 = 78.8%, 95% interval 74.5%–82.5%.** That is undecided by the bar.

The 85 disagreements were hand-read against the criterion (`skill-verdict-check-1-disagreements.tsv`,
one row each with the ruling and why):

| Ruling | Names |
| --- | --- |
| Jev is right | 28 |
| The review is right | 34 |
| Unclear | 23 |

A disagreement where Jev is right is not a miss (#52), and an unclear one is counted as a miss.
**Agreement with the right decision: (315 + 28)/400 = 85.8%, 95% interval 82.0%–88.8%: passed.**
The script prints this figure from the rulings file. Counting the unclear ones as Jev's would give
91.5%.

Two limits on this figure:

- **Only the disagreements were re-read.** A name both sides got wrong is counted as agreement, so
  the correction can only raise the raw figure. The 315 agreements were not re-read.
- **The draw is half keeps**, where the decided names are 2,417 keeps to 1,020 drops. Weighted back
  to that mix, agreement with the right decision is 87.1%. Jev is right on 178 of the 200 keeps and
  on 165 of the 200 drops.

By stratum, raw:

| Stratum | Agreed |
| --- | --- |
| keep | 165/200 |
| drop: generic concept | 49/54 |
| drop: ordinary word | 31/38 |
| drop: key noise | 11/24 |
| drop: not a technology | 13/24 |
| drop: ambiguous abbreviation | 14/19 |
| drop: obscure or historical | 10/12 |
| drop: other (no note, or none the mapping reads) | 7/11 |
| drop: homonym | 6/9 |
| drop: consumer, office or browser software | 6/6 |
| drop: single letter | 2/2 |
| drop: not hired for | 1/1 |

## The disagreements by pattern

**Where Jev is right (28).** The reviews were made under rules that revision 1 changed.

- *Dropped for an abbreviation* (10: Common Lisp, Checkstyle, PowerBuilder, CoffeeScript, OrientDB,
  MUMPS, OpenVMS, Bourne shell, Proxmox Backup Server, Linux Security Modules). The reviews dropped
  the whole skill because its short form (CL, CS, pb) is noise. Under the criterion, whether a
  name is a skill and which keys it gets are separate questions, and the drafter settles the
  keys. This is most of why `key noise` agrees at 11/24.
- *Not what an engineer is hired for, or office software* (8: Creo, SAP PM, Yardi, Lucidchart,
  Klaxoon, Microsoft Copilot, Ciena, and the generic SSO). Revision 1 added these drops,
  and the reviews had kept them.
- *Homonyms read from the ads* (eCos, whose every mention is ECOs; Gemini and DeepAR, which the
  ads only ever use in the software sense; SAC), *another spelling* (Visual Studio IDE), and
  *names the criterion spells out* (make, Purview, CWE, Spark ML), and a *bare name two vendors
  share* (Recovery Manager).

**Where the review is right (34).** These are Jev's real misses, and they show what to watch in
discovery:

- *An ordinary word that also names a tool* (10: Flux, Swarm, Compose, Tempo, Graphite, Polymer,
  CLIP, Elementary, Opal, Goose). Jev keeps the word when one of the three segments uses it as the
  tool, though the others show the ordinary sense. The criterion drops ordinary words, and this
  is the costliest miss: a kept ordinary word puts a false skill on every vacancy containing it.
  It is also the miss the drafter and the reviewing session are best placed to catch, since the
  safe form (Flux CD, Docker Swarm, Grafana Tempo) is the skill.
- *A fragment of a longer name, or a feature of one* (8: Enterprise Linux, Lake Storage Gen2,
  365 GCC High, Spark Structured, Logic Apps Azure, Assured Compliance Assessment, UCS, Next.js
  App Router). Jev reads the full name in the segment and keeps the fragment.
- *A generic concept in technical words* (4: Federated GraphQL, HSM, NAT gateways, UNIX Shell),
  *ambiguous abbreviations* (SBE, R2), a *homonym* (Foundry) and *consumer software* (Home
  Assistant).
- *Drops of real skills* (8: SSL, PoE, MapReduce, BAPIs, SAP Basis, Figma, Palo Alto Cortex, Python
  2). Jev drops protocols and platforms that read like generic concepts. Python 2 is a spelling of
  an existing skill and should have been kept as that skill.

**Unclear (23).** These are features of a service (Shared VPC, Jira Automation, ArcGIS Portal, HCM
Extracts), obscure names in one ad (JetBrains MPS, Atlas CLI, Spec Kit, BMIDE), outdated versions
(Windows 7, Windows Mobile, OpenView), business software at the edge of "hired for" (Hyperion,
FactoryTalk, Freshservice, Elementor, SAP Joule), and IAM, NAT, ISIS, Argo, Wikitext, AWQ and DevNet. NAT is here rather than with SSO: it is a
networking technique, as SSL and PoE are protocols the review was right to keep.
The criterion does not say whether a named feature of a service is a skill. #55's reviewing
session will meet these, and a later revision may settle them.

## Category

The category was scored apart, on the 165 names both kept: Jev picked the reviews' category for
**113, 68.5%, 95% interval 61.0%–75.1%**. Most of the moves are between neighbouring categories:
a managed service filed under `devops` or `data` by the reviews and `cloud` by Jev (10, such as
MWAA, SNS, Google Dataflow, Azure Container Registry), a library under `ml` or `data`
by the reviews and `framework` by Jev (7), and `devops`, `tool` and `networking` traded among
themselves. The criterion files a cloud's managed services under `cloud`, so Jev's moves to
`cloud` follow it. No gate rests on the category: the reviewing session reads every kept row and
can fix it.

## Spend

400 labels, 835k input tokens, about 2.1k a name: **$0.035**. Every label is cached in
`skill-labels-jev.jsonl` by name form, criterion revision and model, with the segments Jev read,
and the git blob hash of the criterion. A label is never made again, and labelling refuses to run
if the criterion changed without its revision marker being raised. #53's scoring uses the same
cache. Jev has cost $0.82 across #11 and #41,
of the $4 stop.
