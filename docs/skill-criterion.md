# What counts as a skill

Revision 5 (2026-10-07)

The criterion of skill discovery (#41, ADR-0013, ADR-0015). It is read by the agent that drafts a
kept candidate's row of `skills.tsv`, by the adjudicator, and by every session that reviews
either. Jev reads only its short form, the section *What Jev reads*, and the Category table. When a
decision and this file disagree, this file is right and the decision is wrong.

It is written from the verdict rules of #38 and #40, whose decisions are archived in
`docs/measurements/taxonomy-review-2026-09-24.tsv` and
`taxonomy-mined-review-2026-09-24.tsv`. Their notes are the case law: where this file is silent,
read how a like name was decided there.

## What a skill is for

A skill is matched in two places: in a vacancy's description, where an ad asks for it, and in a
profile, where a person claims it. Both sides only meet if they name a skill the same way, and
the taxonomy is also the list a person picks their own skills from. So a skill is a name that an
engineer would put on a CV.

A skill is a **technology**: one specific named language, framework or library, database, cloud
or cloud service, tool or platform. A practice, method, technique, architecture or kind of system
is no skill, however technical: CI/CD, GitOps, infrastructure as code, TDD, MVVM, microservices,
REST, RAG, LLMs, reinforcement learning, device drivers, ERP, CRM, data lake, vector databases.

"Engineer" in this document always means a software person: someone whose background is
software, as `docs/engineering-role-criterion.md` reads the word. Whether a technology is a skill
does not depend on where the ad naming it comes from: a technology no software person is hired
for is a drop even when an engineering ad names it, and a software technology is a keep even when
the ad naming it is not an engineering role.

## Keep

**Keep** a name when it names one specific technology an engineer would put on a CV: a language,
a framework or library, a database, a cloud or one of its services, a tool or a platform. Python,
Grafana, Claude Code, GNU Make, Google Cloud Platform. A platform data, ML or AI teams build on is
a platform like any other: Dataiku, Microsoft Foundry, Databricks. So is a protocol, file format
or standard that software implements: MCP, gRPC, OAuth, Parquet, Bluetooth.

A name is judged where it is written, and exactly as given: in the segments given with it, it is a
keep when they use it for the technology, and when it is the technology's whole name and nothing
more. A name with an ordinary word before or after it (Configure Nginx, Kubernetes Expertise,
Advanced AWS, Kubernetes-based) is a drop, though the name inside it is a keep; so is part of a
longer name written around it (Security Command, written in Security Command Center). A **bare form**, the name ads use for a product without its vendor or
qualifier, is that product: Go is Golang, Spring is the Spring Framework, Kafka is Apache Kafka,
Compose is Jetpack Compose or Docker Compose, Fabric is Microsoft Fabric, Tempo is Grafana Tempo.
It is a keep where the text uses it for the product, and a drop where the text uses the ordinary
word ("go above and beyond", "the fabric of our team").

A name that names an existing skill by another spelling is a keep, as that skill: GCP is Google
Cloud Platform, `#Kubernetes` is Kubernetes, and a name split by a space, an underscore or a slash
is that name (Check MK is Checkmk, Mongo DB is MongoDB). So is a version of a skill (HTTP/2 is
HTTP, Vue 3 is Vue.js), and a named feature or service of a skill that ads ask for by its own name
(GKE Workload Identity, Intune Proactive Remediations).

## Drop

**Drop** a name that is:

- **A generic concept** or a practice, not a named technology: data visualization, CI/CD,
  microservices, API, dead letter queue, WebView.
- **An ordinary word**, in English or Spanish, or a phrase of them, used in its ordinary sense:
  Clean, Cadence, Familiarity, Nice. The same word used for a product is a bare form, and kept.
- **Obscure or historical**: a name few ads use and few engineers would recognise, or a technology
  that is no longer hired for: TOPS-10, HAL/S, C--.
- **An ambiguous abbreviation**: one that names several things, none dominant where it is
  written: CAPI, SDF, BO, ZK.
- **Consumer, office or browser software**: what anyone uses, not what an engineer is hired for:
  Chrome, Microsoft Word, Microsoft Teams, Google Drive, Zoom, Box, Coda, Concur.
- **A single letter**, except C and R.
- **A homonym** of something else, where the text means the other thing: SPARK (the Ada subset)
  against Apache Spark, TS against TS/SCI.
- **A technology no software person is hired for**: AutoCAD, Epic EHR, SAP Concur, a CNC
  controller.
- **A compliance framework, regulation or certification**: what a company is audited against or a
  person is certified in, not a technology they build with: SOC 2, ISO 27001, NIST CSF, FedRAMP,
  HIPAA, PCI DSS, ITIL, CISSP, AZ-204, Security+.
- **Not a technology at all**: a company (as an employer, customer, partner or vendor), a vendor
  umbrella, a product line, a place, a person, a heading or field of study, a piece of a longer
  name that ads never use alone for the product ("Cloud Platform" of Google Cloud Platform,
  "Directory" of Active Directory), or a name glued to an ordinary word. A vendor's name is a drop
  even where ads use it for its products: Palo Alto for its firewalls, Juniper for its routers.
  The products are kept under their own names (PAN-OS, Junos).

When in doubt between keep and drop, drop. A kept name that matches ordinary text puts a false
skill on every vacancy containing it.

## Key safety

A skill is looked up by its keys: its canonical name and each of its aliases, in any case and
spacing. Aliases are the other spellings ads actually use (k8s, postgres, ReactJS, Go for
Golang). A key belongs to one skill, and a name that is already a key is the skill it names.

Each key is **plain** or **context**, set by a rule when its row is drafted, never by hand
(ADR-0015):

- A **plain** key is matched anywhere. It must mean its skill in at least 95% of its mentions.
- A **context** key is an ordinary word, or C or R: Go, Spring, Rails, Compose, Tempo. It is
  matched only where a context rule says the text means the skill.

Whether a name is a skill and how its key is matched are two questions: a bare form that is an
ordinary word is a skill with a context key, not a drop.

## What Jev reads

This section is the whole criterion Jev reads, with the Category table for the category. It says
what the sections above say, short. It changes only with them, at a calibration.

> A skill is a technology an engineer puts on a CV and an ad asks for: one specific named
> language, framework or library, database, cloud or cloud service, tool or platform (Python,
> Grafana, Google Cloud Platform, Databricks, Claude Code), or a protocol, file format or standard
> software implements (MCP, gRPC, OAuth, Parquet). Decide the name exactly as given, word for word,
> as the text given with it uses it.
>
> Keep it when the text uses it for one such technology. Keep it too when it is that technology
> under another spelling (GCP, k8s, Mongo DB), a version of it (Vue 3, HTTP/2), a named feature or
> service ads ask for, or its bare form without the vendor: Go for Golang, Spring, Kafka, Compose,
> Fabric for Microsoft Fabric, Tempo for Grafana Tempo.
>
> Drop it when it holds more or less than a technology's name: an ordinary word before or after
> the name (Configure Nginx, Kubernetes Expertise, Advanced AWS, Kubernetes-based), or only part
> of a longer name written there (Security Command, when the text says Security Command Center).
>
> Drop it when it is: a practice, method, architecture or concept (CI/CD, microservices, REST,
> TDD, RAG, LLMs, API, data lake); an ordinary English or Spanish word used in its ordinary sense
> ("go above and beyond", "the fabric of our team"); a company, vendor, customer, place, person,
> heading or field of study (Palo Alto, Juniper, Stripe as an employer); a piece of a longer name
> that is never used alone for the product ("Cloud Platform", "Directory"); a compliance
> framework, regulation or certification (SOC 2, HIPAA, ITIL, CISSP); consumer, office or browser
> software (Chrome, Microsoft Word, Zoom); software no software person is hired for (AutoCAD,
> Epic EHR); an abbreviation that names several things, none dominant there; a single letter
> other than C and R; or a name few engineers would recognise. When in doubt, drop.

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

- **Revision 5** (2026-10-07): from the open half of the gold set under revision 4. A name is
  judged exactly as given, so a name with an ordinary word around it, or part of a longer name
  written there, is a drop; protocols, file formats and standards software implements are keeps,
  as the Category table already had them. Reopens no drop.
- **Revision 4** (2026-10-07): from #58 and ADR-0015, at #54's first calibration. A skill is a
  technology, and practices stay out. A bare form is the skill it names, kept where the text uses
  it for the product; *An ordinary word* drops only the ordinary sense, and *Not a technology*
  drops only the pieces of a name ads never use alone. Key safety is rewritten around plain and
  context keys. *In a segment* goes: its exceptions are the filters of the recall check, and a
  name in a segment is judged by Keep and Drop like any other. *What Jev reads* is the short text
  Jev answers from. It reopens the decisions dropped, or kept without a key, for being a bare
  form.
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
