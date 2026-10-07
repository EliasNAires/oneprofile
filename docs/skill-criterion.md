# What counts as a skill

Revision 3 (2026-10-07)

The criterion of skill discovery (#41, ADR-0013). It is the one document read by Jev when it
decides whether a candidate is kept, by the agent that drafts a kept candidate's row of
`skills.tsv`, and by every session that reviews either. When a decision and this file disagree,
this file is right and the decision is wrong.

It is written from the verdict rules of #38 and #40, whose decisions are archived in
`docs/measurements/taxonomy-review-2026-09-24.tsv` and
`taxonomy-mined-review-2026-09-24.tsv`. Their notes are the case law: where this file is silent,
read how a like name was decided there.

## What a skill is for

A skill is matched in two places: in a vacancy's description, where an ad asks for it, and in a
profile, where a person claims it. Both sides only meet if they name a skill the same way, and
the taxonomy is also the list a person picks their own skills from. So a skill is a name that an
engineer would put on a CV, and that can be found in an ad without false hits.

"Engineer" in this document always means a software person: someone whose background is
software, as `docs/engineering-role-criterion.md` reads the word. Whether a technology is a skill
does not depend on where the ad naming it comes from: a technology no software person is hired
for is a drop even when an engineering ad names it, and a software technology is a keep even when
the ad naming it is not an engineering role.

## Keep

**Keep** a candidate when it names one specific technology an engineer would put on a CV: a
language, a framework or library, a database, a cloud or one of its services, a tool or a
platform. Python, Grafana, Claude Code, GNU Make, Google Cloud Platform. A platform data, ML or AI
teams build on is a platform like any other: Dataiku, Microsoft Foundry, Databricks.

A candidate that names an existing skill by another spelling is a keep, as that skill: GCP is
Google Cloud Platform, `#Kubernetes` is Kubernetes, and a name split by a space, an underscore or a
slash is that name (Check MK is Checkmk, Mongo DB is MongoDB).

## Drop

**Drop** a candidate that is:

- **A generic concept**, not a named technology: data visualization, CI/CD, microservices, API,
  dead letter queue, WebView.
- **An ordinary word**, in English or Spanish, or a phrase of them: Clean, Cadence, Familiarity,
  Nice. This holds even where the ads use the word for a technology: Camel, Ant and Ranger are
  drops, and Apache Camel, Apache Ant and Apache Ranger are the candidates that keep them.
- **Obscure or historical**: a name few ads use and few engineers would recognise, or a technology
  that is no longer hired for: TOPS-10, HAL/S, C--.
- **An ambiguous abbreviation**: one that names several things, none dominant: CAPI, SDF, BO, ZK.
- **Consumer, office or browser software**: what anyone uses, not what an engineer is hired for:
  Chrome, Microsoft Word, Microsoft Teams, Google Drive, Concur.
- **A single letter**, except C and R.
- **A homonym** of something else, where the ad's word usually means the other thing: SPARK (the
  Ada subset) against Apache Spark, TS against TS/SCI.
- **A technology no software person is hired for**: AutoCAD, Epic EHR, SAP Concur, a CNC
  controller.
- **A compliance framework, regulation or certification**: what a company is audited against or a
  person is certified in, not a technology they build with: SOC 2, ISO 27001, NIST CSF, FedRAMP,
  HIPAA, PCI DSS, ITIL, CISSP, AZ-204, Security+.
- **Not a technology at all**: a company, a vendor umbrella, a product line, a place, a person, a
  fragment of a longer name (Fabric for Microsoft Fabric), or the name glued to an ordinary word. A
  vendor's name is a drop even where ads use it for its products: Palo Alto for its firewalls,
  Juniper for its routers. The products are kept under their own names (PAN-OS, Junos).

When in doubt between keep and drop, drop. A dropped name costs a skill that ads could have
named; a kept name that matches ordinary text puts a false skill on every vacancy containing it.

## Key safety

A skill is looked up by its keys: its canonical name and each of its aliases, in any case and
spacing. **No key is an ordinary word**, in English or Spanish, and no key is a single letter
other than C and R.

Where a name's bare form is ambiguous, the safe form is canonical and the bare form is no key at
all: Express.js, not Express; Golang, not Go; GNU Make, not make; Microsoft Access, not Access;
Apache Thrift, not Thrift. Aliases are the other spellings ads actually use (k8s, postgres,
ReactJS), each held to the same rule.

A key belongs to one skill. A candidate whose name is already a key is the skill it names.

## In a segment

Whether a job-ad segment names a skill, and whether a list of names found in it misses one, is read
by the same rules as a candidate. **A name is missed only when, offered as a candidate, it would be
a keep, and it is in neither the list found nor this section's exceptions.** Before answering that a
segment misses a skill, check the name against each of these:

- A segment names a skill only where it names a technology the Keep section keeps. A practice,
  method, technique, architecture or kind of system is no skill, however technical the segment:
  CI/CD, GitOps, infrastructure as code, TDD, MVVM, microservices, RAG, LLMs, reinforcement
  learning, device drivers, ERP, CRM, data lake, vector databases.
- A skill named by a bare form that key safety keeps from being a key is not missed: the taxonomy
  holds it under its safe form, and losing those mentions is the price key safety chose. Go for
  Golang, Julia for JuliaLang, REST for RESTful API, Compose for Jetpack Compose, Tempo for Grafana
  Tempo, OPA for Open Policy Agent.
- A feature, setting or part of a skill found in the segment is that skill, not another: Intune
  Proactive Remediations is Microsoft Intune, GKE Workload Identity is Google Kubernetes Engine, AWS
  security groups are AWS.
- A company named as an employer, customer or partner is not its products: "Databricks Customer
  Support" or "at Stripe" names no skill. Nor does a list of brands that use a product.
- A skill found under another spelling is found: the list names skills by their canonical name, so
  Google Cloud is found when Google Cloud Platform is listed, and k8s when Kubernetes is. A
  version of a skill found is that skill: HTTP/2 is HTTP, Vue 3 is Vue.js.
- A name the Drop section drops is not missed, wherever it appears: office and consumer software
  (Zoom, Box, Coda), software no software person is hired for, compliance frameworks, ordinary
  words, and vendors.

## Category

Every kept skill has exactly one category, the one that best says what it is:

| Category | For |
| --- | --- |
| `language` | programming, query, markup and shell languages |
| `framework` | frameworks and libraries |
| `database` | databases and data stores |
| `cloud` | cloud providers and their managed services |
| `devops` | build, CI/CD, containers, orchestration, infrastructure as code, observability |
| `data` | data engineering, analytics and BI |
| `ml` | machine learning and AI models, libraries and platforms |
| `testing` | test frameworks and tools |
| `os` | operating systems |
| `security` | security tools, and the protocols and technical standards software implements |
| `networking` | network protocols, hardware and tools |
| `tool` | anything an engineer uses that fits none of the above |

## Revisions

A decision records the revision it was made under. A drop is final: a name dropped under one
revision is not decided again under the next, unless the reviewer of the next names that drop's
reason as one the new revision reopens (ADR-0013).

- **Revision 3** (2026-10-07): from #54's round 1, where Jev decided like candidates two ways. A
  vendor's name used for its products is a drop. An ordinary word is a drop even where ads use it
  for a technology. Data, ML and AI platforms are keeps, and a known name split by a separator is
  that name. *In a segment* says that a missed name must be one that would be a keep, and adds
  spellings, versions and dropped kinds of software to what is not missed. It states what revision
  2 already meant, and reopens no drop.
- **Revision 2** (2026-10-07): compliance frameworks, regulations and certifications are drops, as
  #40 dropped OWASP, ITIL, CIS Benchmarks and NIST CSF; and *In a segment* says how a segment names
  a skill, after #54's round 0 counted concepts and bare forms as missed skills. It narrows what is
  kept and reopens no drop.
- **Revision 1** (2026-10-06): written from the verdict rules of #38 and #40.
- **Revision 0** stands for those rules themselves, unwritten, under which every decision of the
  two archives was made.
