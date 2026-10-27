# Role list: draft (#25, step 1)

Drafted on 2026-10-26 from `top-200.tsv` (the 200 most common normalized titles of the engineering
subset, 90,105 of its 155,767 vacancies, 57.8%), with `titles.tsv` (2,541 titles on at least 5
vacancies, 78.5%) read for the size of each kind of work in the tail. Under ADR-0024 and its four
amendments: a role is the employer's word for the day-to-day work, read from the title; a leader's
role is the area they lead; skill nearness is evidence, never the rule. For review (step 1.3).

## How the shares were estimated

- **Top 200**: the exact sum of the top-200 titles placed in the role.
- **With tail**: that sum plus the tail titles a keyword sketch of the head and its kind-of-work
  words sends to the role (e.g. anything with `devops`, `site reliability`, `platform`,
  `infrastructure` or `cloud` to platform engineer, after the earlier roles in the sketch have
  taken theirs). It is an estimate for the 1% floor, not the sorting of step 4: the sketch
  misplaces a few tail titles each way (it sends `model based systems engineer` to systems
  administrator, for one). It does not reach titles on fewer than 5 vacancies.
- 1% of the subset is 1,558 vacancies.

## Summary

24 roles. Together they hold 86,333 vacancies of the top 200 (55.4% of the subset) and, by the
sketch, about 110,400 of the titles on at least 5 vacancies (about 70.9%). Titles on 1 to 4
vacancies are not counted, so the coverage the rule reaches will be somewhat higher.

| Slug | Label | Top 200 | With tail |
|---|---|---|---|
| `software-engineer` | Software engineer | 23,155 (14.9%) | ~24,700 (15.9%) |
| `platform-engineer` | Platform / DevOps engineer | 7,099 (4.6%) | ~9,600 (6.2%) |
| `security-engineer` | Security engineer | 3,522 (2.3%) | ~6,700 (4.3%) |
| `data-engineer` | Data engineer | 5,313 (3.4%) | ~6,300 (4.0%) |
| `ai-engineer` | AI engineer | 4,216 (2.7%) | ~6,200 (4.0%) |
| `product-manager` | Product manager | 4,544 (2.9%) | ~5,800 (3.7%) |
| `full-stack-engineer` | Full stack engineer | 5,166 (3.3%) | ~5,500 (3.5%) |
| `systems-administrator` | Systems administrator / IT engineer | 3,671 (2.4%) | ~4,800 (3.1%) |
| `data-scientist` | Data scientist | 3,214 (2.1%) | ~3,800 (2.4%) |
| `backend-engineer` | Backend engineer | 3,246 (2.1%) | ~3,500 (2.3%) |
| `solutions-architect` | Solutions architect | 2,696 (1.7%) | ~3,400 (2.2%) |
| `machine-learning-engineer` | Machine learning engineer | 2,465 (1.6%) | ~3,300 (2.1%) |
| `qa-engineer` | QA / test engineer | 2,225 (1.4%) | ~3,100 (2.0%) |
| `embedded-engineer` | Embedded and robotics engineer | 2,040 (1.3%) | ~2,700 (1.7%) |
| `mobile-engineer` | Mobile engineer | 2,183 (1.4%) | ~2,600 (1.7%) |
| `data-analyst` | Data analyst | 1,618 (1.0%) | ~2,600 (1.6%) |
| `network-engineer` | Network engineer | 1,402 (0.9%) | ~2,400 (1.5%) |
| `sales-engineer` | Sales / solutions engineer | 1,564 (1.0%) | ~2,300 (1.5%) |
| `support-engineer` | Technical support engineer | 999 (0.6%) | ~2,300 (1.5%) |
| `program-manager` | Program / project manager | 1,336 (0.9%) | ~1,800 (1.2%) |
| `research-scientist` | Research scientist | 1,082 (0.7%) | ~1,800 (1.2%) |
| `forward-deployed-engineer` | Forward deployed engineer | 1,618 (1.0%) | ~1,750 (1.1%) |
| `frontend-engineer` | Frontend engineer | 1,290 (0.8%) | ~1,700 (1.1%) |
| `enterprise-platform-developer` | Enterprise platform developer | 669 (0.4%) | ~1,650 (1.1%) |

**No role is over 25%.** `software-engineer` is the largest at about 15.9%, of which the bare heads
the ADR sends there (`engineer`, `developer`, `engineering`) are 5,477 (3.5%). Proposed: keep it.
What is left in it is generic software work whose title names no area, so there is no cut in the
titles to split it along; the areas that titles do name (backend, frontend, full stack, mobile,
embedded, platform) are already roles of their own. It would grow if more raw titles lose their
area to normalization (see "Normalization notes" below).

## The roles

Each role lists the top-200 titles placed in it (with vacancies), then its edges: a title near
the edge, the side it falls on, and why. Tail titles in the examples are marked with their count.

### `software-engineer`: Software engineer

**Day to day:** designs, writes and maintains software, where the title names no more specific
area of the stack, platform or domain.

**Titles (23,155, 14.9%; ~15.9% with tail):** `software engineer` 12,948; `engineer` 2,260;
`developer` 1,620; `engineering` 1,597 (a bare Engineering Manager, settled); `software developer`
947; `software development engineer` 623; `software engineering` 461; `member of technical staff`
405; `technical lead` 305; `tech lead` 296; `software architect` 252; `integration engineer` 189;
`ingénieur` 172; `développeur` 153; `performance engineer` 139; `software development` 132
(Software Development Manager); `quantitative developer` 123; `application developer` 122;
`application engineer` 116; `cloud developer` 110; `software systems engineer` 99; `product
software engineer` 86.

**Edges:**
- `backend engineer` → `backend-engineer`: the title names the server side; a bare software
  engineer may do any part. Skill nearness 0.81 is not the reason.
- `software architect` → here, not `solutions-architect`: the architect of one product's code
  writes and reviews code daily; a solution or enterprise architect designs across systems for a
  client or an organization.
- `member of technical staff` → here: the frontier labs' word for an engineer. `research member of
  technical staff` (63) → `research-scientist`, `ai member of technical staff` (25) →
  `ai-engineer`, because the kind word names the work.
- `quantitative developer` → here: builds trading and pricing systems in code; the modelling is the
  quantitative researcher's (`data-scientist`). Quant work as a whole is under 1% (see below).
- `cloud developer` → here, not `platform-engineer`: the raw titles are developers of cloud
  software ("Software Developer Cloud & Distributed Systems"), not operators of cloud
  infrastructure. Unsure; nearest by skills is infrastructure engineer, 0.52.

### `platform-engineer`: Platform / DevOps engineer

**Day to day:** builds and runs the infrastructure software runs on: cloud accounts, Kubernetes,
CI/CD pipelines, infrastructure as code, monitoring and on-call reliability.

**Titles (7,099, 4.6%; ~6.2% with tail):** `platform engineer` 1,877; `devops engineer` 1,489;
`site reliability engineer` 1,247; `infrastructure engineer` 826; `cloud engineer` 731; `devsecops
engineer` 233; `cloud platform engineer` 181; `cloud infrastructure engineer` 165; `cloud
architect` 148; `storage engineer` 105; `observability engineer` 97.

DevOps, SRE, platform, infrastructure and cloud engineers are one role: a candidate reading any of
them expects the same pipelines, clusters and on-call. Splitting SRE off was considered; the titles
are used interchangeably across employers and SRE alone is 0.8%.

**Edges:**
- `devsecops engineer` → here, not `security-engineer`: the day is CI/CD pipelines with security
  scanning in them (gitlab-ci, sonarqube, helm); nearest devops engineer 0.85.
- `cloud architect` → here, not `solutions-architect`: designs the employer's or client's cloud
  estate (terraform, bicep, azure-kubernetes-service); nearest cloud engineer 0.83.
- `ai platform engineer` → `ai-engineer`, not here: its skills are bedrock, mcp, langchain; it builds
  the platform AI applications run on. But `ai infrastructure engineer` (65) → here: GPU clusters
  and their scheduling are infrastructure.
- `machine learning platform engineer` (62) and `mlops engineer` → `machine-learning-engineer`:
  the day is the lifecycle of models (training pipelines, serving, kubeflow, mlflow), not
  general infrastructure. This is the ADR's own example (`ml platform engineer`); unsure.
- `systems engineer` → `systems-administrator`, not here: its skills are vmware, windows,
  powershell, active directory, the running of an organization's own servers rather than a
  product's infrastructure.
- `database reliability engineer` (45) → here: SRE work on databases.

### `security-engineer`: Security engineer

**Day to day:** protects systems and data: builds defenses, finds and fixes vulnerabilities,
manages identities and access, and monitors for and responds to attacks.

**Titles (3,522, 2.3%; ~4.3% with tail):** `security engineer` 1,423; `cybersecurity engineer`
432; `application security engineer` 217; `security analyst` 209; `product security engineer` 199;
`cloud security engineer` 165; `cybersecurity analyst` 158; `security architect` 134; `identity
engineer` 117; `information security engineer` 104; `information security analyst` 100;
`information security` 93 (CISO, Information Security Manager); `cybersecurity` 92 (Director of
Cybersecurity); `iam engineer` 79.

Security analysts (SOC) are in this role rather than one of their own: they are about 0.5% with
the tail (`soc analyst` 68, `security operations analyst` 48, `threat intelligence analyst` 18…),
and "monitors for and responds to attacks" covers them honestly.

**Edges:**
- `network security engineer` → `network-engineer`, not here: the day is firewalls, VPNs and
  routing (ipsec, sd-wan, ospf, bgp); nearest network engineer 0.75. Unsure.
- `cloud security engineer` → here, not `platform-engineer`: guardduty, cloudtrail, wiz, kms; the
  work is the cloud's security posture.
- `identity engineer`, `iam engineer` → here: access management (okta, sailpoint, saml) is
  security work, even where the raw title is "Software Engineer, Identity".
- `security researcher` (72), `vulnerability researcher` (50) → here, not `research-scientist`:
  the research is finding vulnerabilities.
- `devsecops engineer` → `platform-engineer` (above).

### `data-engineer`: Data engineer

**Day to day:** builds and runs the pipelines, warehouses and lakes that move, model and store an
organization's data for others to use.

**Titles (5,313, 3.4%; ~4.0% with tail):** `data engineer` 3,603; `analytics engineer` 402;
`data architect` 375; `data platform engineer` 354; `ai data engineer` 333; `data consultant` 84;
`data developer` 83; `data engineering` 79.

**Edges:**
- `analytics engineer` → here, not `data-analyst`: builds and tests the models in the warehouse
  (dbt, fivetran, airflow) that analysts query; nearest by skills `data analytics engineer` 0.84
  and `data` 0.83. Unsure: some employers use it for an analyst who writes dbt.
- `data platform engineer` → here, not `platform-engineer`: its skills are iceberg, dagster,
  flink, spark; the platform is for data.
- `data architect` → here, not `solutions-architect`: designs the data platform (synapse,
  databricks, delta-lake); nearest data engineer 0.84.
- `business intelligence engineer` (49), `business intelligence developer` (64) → `data-analyst`:
  the day is reports and dashboards, nearest BI analyst 0.85.
- `ai data engineer` → here: its raw titles are mostly "Data & AI Engineer" and "AI Data
  Engineer"; the data work is the one named in every raw title. Unsure; "Data & AI Engineer"
  arguably names two heads (see "Normalization notes").

### `ai-engineer`: AI engineer

**Day to day:** builds applications and agents on top of foundation models: prompting, retrieval,
tool use, evaluation and the code around them.

**Titles (4,216, 2.7%; ~4.0% with tail):** `ai engineer` 2,572; `applied ai engineer` 439; `ai
developer` 265; `ai platform engineer` 242; `ai architect` 176; `ai software engineer` 171;
`agentic ai engineer` 140; `genai engineer` 126; `full stack ai engineer` 85.

**Edges:**
- `machine learning engineer` → `machine-learning-engineer`: trains and serves models (pytorch,
  scikit-learn, mlflow); the AI engineer builds on models others trained (langchain, langgraph,
  openai, mcp). A visitor moving from backend into AI picks this role.
- `ai machine learning engineer` ("AI/ML Engineer") → `machine-learning-engineer`: the head is ML
  engineer; nearest ML engineer 0.83 and AI engineer 0.81, so unsure.
- `forward deployed ai engineer` → `forward-deployed-engineer`: being embedded with a customer is
  the day-to-day; the AI is what is deployed.
- `full stack ai engineer` → here, not `full-stack-engineer`: the product is the AI feature
  (llamaindex, crewai, langgraph).
- `ai architect` → here, not `solutions-architect`: designs AI systems (azure-openai, bedrock,
  langgraph); nearest ai solution architect 0.80. `ai solutions architect` → `solutions-architect`,
  because "solutions architect" names the customer-facing design work.

### `product-manager`: Product manager

**Day to day:** decides and specifies what gets built: talks to users and stakeholders, writes
requirements and user stories, sets priorities and the roadmap.

**Titles (4,544, 2.9%; ~3.7% with tail):** `product manager` 1,272; `product owner` 804; `business
analyst` 630; `technical product manager` 352; `product management` 235 (Director of Product
Management); `ai technical product manager` 200; `business systems analyst` 186; `systems
analyst` 184; `technical product` 178 (Technical Product Lead); `ai product manager` 167;
`product` 131 (Head of Product); `platform product manager` 104; `data product manager` 101.

Business analysts are in this role, not one of their own: they are about 0.84% with the tail
(`business analyst`, `business systems analyst`, `systems analyst`, `it business analyst` 59,
`technical business analyst` 29, `functional analyst` 22…), under the floor. Gathering
requirements and writing them as stories is the day of a business analyst and of a product owner
alike, and "writes requirements and user stories" is in this role's description. This is the
fold most worth a second look (see "Open points").

**Edges:**
- `product owner` → here, not `program-manager`: owns the backlog and what goes in it; nearest by
  skills is technical program manager 0.90, because both name only jira and confluence. Skills say
  nothing here.
- `technical program manager` → `program-manager`: coordinates delivery across teams; does not
  decide what the product does.
- `data product manager` → here, not `data-engineer`: the job word is product manager, sorted
  whole.
- `business analyst` → here; `data business analyst` (19) and `analytics business analyst` (19)
  → `data-analyst`, where the kind word says the analysis is of data.
- `salesforce business analyst` (16) → `enterprise-platform-developer`: the platform word names
  the kind of work.

### `full-stack-engineer`: Full stack engineer

**Day to day:** builds product features end to end, from the user interface through the API to
the database.

**Titles (5,166, 3.3%; ~3.5% with tail):** `full stack engineer` 3,077; `product engineer` 1,040;
`full stack developer` 948; `flask+react full stack engineer` 101.

**Edges:**
- `product engineer` → here, not `software-engineer`: the title promises shipping product features
  across the stack, often with product judgement; nearest full stack engineer 0.79, and its skills
  (typescript, react, node-js, postgresql) are the full stack's.
- `full stack ai engineer` → `ai-engineer` (above).
- `web developer` → `frontend-engineer`: wordpress, php, html, css; the day is the site, not an
  application backend. Unsure: some web developers are full stack.
- `backend engineer` → `backend-engineer`: the title excludes the interface.

### `systems-administrator`: Systems administrator / IT engineer

**Day to day:** keeps an organization's own servers, databases, identities and devices running:
installs, configures, patches and backs them up.

**Titles (3,671, 2.4%; ~3.1% with tail):** `systems engineer` 1,585; `systems administrator` 762;
`database administrator` 376; `it` 228 (IT Manager, Director of IT); `it engineer` 183; `database
engineer` 181; `administrator` 152; `it systems engineer` 109; `it systems administrator` 95.

Database administration is in this role: it is about 0.5% with the tail (`database
administrator`, `database engineer`, `database developer` 47…) and "keeps databases running" is
in the description.

**Edges:**
- `systems engineer` → here: its skills are vmware, dns, powershell, windows, linux, and its raw
  titles include "Linux Systems Engineer" and "CommVault Systems Engineer". Unsure: in aerospace
  and defence a systems engineer does requirements and integration, which this role does not
  cover; the blind sample should report it by ATS.
- `it support engineer` → `support-engineer`: answers users' tickets rather than running the
  systems; nearest it support specialist 0.93.
- `devops engineer` → `platform-engineer`: the infrastructure of a product, as code, rather than
  the organization's own servers.
- `database engineer` → here: aurora, rds, sql server administration; nearest database
  administrator 0.80. `database reliability engineer` → `platform-engineer`, `database developer`
  → `data-engineer`.
- `administrator` → here: its raw titles are "Oracle Database Administrator", "SharePoint
  Administrator", "SAP Basis Administrator", whose platform word normalization took out as a
  skill. Unsure.

### `data-scientist`: Data scientist

**Day to day:** builds statistical and machine learning models from data to explain or predict
something for the business, and reports what they find.

**Titles (3,214, 2.1%; ~2.4% with tail):** `data scientist` 2,734; `data science` 258 (Data
Science Manager); `quantitative researcher` 222.

**Edges:**
- `machine learning engineer` → `machine-learning-engineer`: puts models into production as
  software; the data scientist's output is the model and the finding.
- `data analyst` → `data-analyst`: describes and reports what happened (looker, tableau, power-bi);
  the data scientist models (scikit-learn, pandas, pytorch).
- `quantitative researcher` → here: builds statistical models of markets to predict them, which
  the description covers. Quant work alone is about 0.5% (see below). Unsure.
- `research scientist` → `research-scientist`: advances methods and publishes; the data scientist
  applies them to a business's data.
- `ai data scientist` (41) → here, by the head.

### `backend-engineer`: Backend engineer

**Day to day:** builds the server side of an application: APIs, services, databases, queues and
the integrations between systems.

**Titles (3,246, 2.1%; ~2.3% with tail):** `backend engineer` 2,711; `backend developer` 308;
`distributed systems engineer` 130; `integrations engineer` 97.

**Edges:**
- `distributed systems engineer` → here: server-side systems (netty, grpc, flink, elasticsearch);
  nearest backend engineer 0.60.
- `integrations engineer` → here: builds API integrations with third parties (mulesoft, boomi,
  oauth); nearest backend engineer 0.58. But `integration engineer` (singular) → `software-engineer`:
  its raw titles mix aerospace "Integration and Test", CI, and middleware, and say no area.
- `full stack engineer` → `full-stack-engineer`.
- `data engineer` → `data-engineer`: the pipelines carry data for analysis, not an application's
  requests.

### `solutions-architect`: Solutions architect

**Day to day:** designs how systems and products fit together to solve a client's or the
organization's problem, above the level of one codebase, and often presents that design to the
client.

**Titles (2,696, 1.7%; ~2.2% with tail):** `solutions architect` 862; `solution architect` 710;
`architect` 402; `enterprise architect` 243; `technical architect` 176; `ai solutions architect`
125; `systems architect` 97; `ai solution architect` 81.

**Edges:**
- `solutions architect` → here, not `sales-engineer`: at a vendor (its skills are databricks,
  spark, gcp) the role is often pre-sales, but the title names design work and the candidate
  expects to design. Unsure: the pair most likely to split in the blind sample.
- `software architect` → `software-engineer`; `data architect` → `data-engineer`; `cloud
  architect` → `platform-engineer`; `security architect` → `security-engineer`; `network
  architect` (73) → `network-engineer`: an architect of one kind of engineering is read as that
  kind of engineer, as the ADR reads a leader.
- `architect` (bare) → here: its raw titles are "Principal Architect", "Architect - Enterprise
  Application - SAP", "Senior Architect"; nearest solution architect 0.74. Unlike the bare heads
  the ADR settles, this one is decided here; unsure.
- `salesforce architect` (39) → `enterprise-platform-developer`: the platform word wins.

### `machine-learning-engineer`: Machine learning engineer

**Day to day:** trains, evaluates and serves machine learning models in production, and builds
the pipelines and tooling around them.

**Titles (2,465, 1.6%; ~2.1% with tail):** `machine learning engineer` 1,846; `ai machine learning
engineer` 423; `mlops engineer` 108; `computer vision engineer` 88.

**Edges:**
- `ai engineer` → `ai-engineer` (above).
- `machine learning scientist` → `research-scientist`: the scientist head, and its nearest titles
  are research scientist 0.85 and deep learning researcher 0.84. Unsure; it is also 0.86 to ML
  engineer.
- `computer vision engineer` → here, not `embedded-engineer`: trains vision models (opencv,
  tensorrt, pytorch). `perception engineer` (57) → `embedded-engineer` with robotics: perception
  for a robot's stack.
- `mlops engineer`, `machine learning platform engineer` → here (see `platform-engineer`).

### `qa-engineer`: QA / test engineer

**Day to day:** tests software: writes test plans and automated tests, runs them in CI, finds and
reports defects.

**Titles (2,225, 1.4%; ~2.0% with tail):** `qa engineer` 626; `test engineer` 500; `qa automation
engineer` 212; `quality engineer` 188; `software test engineer` 185; `test automation engineer`
151; `software qa engineer` 100; `software quality engineer` 93; `qa analyst` 85; `sdet` 85.

**Edges:**
- `sdet` → here, not `software-engineer`: writes code whose purpose is testing; nearest qa engineer
  0.90.
- `automation engineer` → no role: its raw titles mix test automation, industrial control (DCS),
  RPA (UiPath) and IT automation (see below).
- `design verification engineer` → no role, not here: verifies chip designs (uvm, systemverilog)
  before silicon; this is hardware work (see below).
- `test engineer` → here, although a few raw titles may test hardware; its skills (selenium,
  cypress, jmeter) are software testing.
- `performance engineer` → `software-engineer`: the most common raw titles tune software
  ("Performance Tuning - Elasticsearch"); some are load testers. Unsure.

### `embedded-engineer`: Embedded and robotics engineer

**Day to day:** writes software that runs on or drives hardware: firmware, embedded Linux,
drivers and kernels, and the control and autonomy software of robots and vehicles, tested on the
device.

**Titles (2,040, 1.3%; ~1.7% with tail):** `embedded software engineer` 634; `systems software
engineer` 344; `firmware engineer` 341; `robotics engineer` 160; `embedded engineer` 155;
`robotics software engineer` 131; `flight software engineer` 95; `autonomy engineer` 91;
`simulation engineer` 89.

Robotics and autonomy are in this role, not one of their own: they are about 0.5% with the tail
(`perception engineer` 57, `robotics automation engineer` 21, `autonomy software engineer` 15…),
under the floor. Robotics software (ros, c++, can bus, on real machines) is software that drives
hardware, which the description names. Unsure; see "Open points".

**Edges:**
- `systems software engineer` → here: linux-kernel, cuda, c; drivers and the layer under
  applications. Nearest embedded engineer 0.60.
- `simulation engineer` → here: its raw titles are "Software Engineer, Simulation", building
  simulators for robots and vehicles (unreal-engine, unity); nearest robotics engineer 0.67.
  Unsure.
- `mech and robotics tech` → no role: a technician who builds and repairs machines, not software.
- `design verification engineer` → no role: chip design, not software on a device.
- `computer vision engineer` → `machine-learning-engineer` (above).

### `mobile-engineer`: Mobile engineer

**Day to day:** builds apps for phones and tablets, native (iOS, Android) or cross-platform.

**Titles (2,183, 1.4%; ~1.7% with tail):** `mobile engineer` 426; `android engineer` 371; `ios
engineer` 267; `android team lead` 248; `android tech lead` 247; `mobile expert` 207; `android
developer` 180; `ios developer` 157; `mobile application developer` 80.

**Edges:**
- `android team lead`, `android tech lead` → here: one employer's repeated posting ("Team Lead,
  Android Core Product"), whose lead level is already taken out.
- `react native engineer` (49) → here, not `frontend-engineer`: the product is a phone app.
- `frontend engineer` → `frontend-engineer`: the browser.
- `mobile expert` → here: one employer's title, no skills recorded; the word names mobile.

### `data-analyst`: Data analyst

**Day to day:** turns data into answers for the business: queries, reports, dashboards and the
metrics they track.

**Titles (1,618, 1.0%; ~1.6% with tail):** `data analyst` 1,041; `analytics analyst` 141;
`product analyst` 133; `business intelligence analyst` 114; `data analytics` 99 (Manager, Data
Analytics); `analytics` 90 (Analytics Manager).

**Edges:**
- `analyst` (bare) → no role here; see "Unsure".
- `financial analyst` → no role: budgets and forecasts in spreadsheets, not data work in the
  subset's sense; under 1% and not covered.
- `analytics engineer` → `data-engineer` (above).
- `business analyst` → `product-manager`: requirements, not data.
- `product analyst` → here: product usage metrics (amplitude, mixpanel); nearest analytics 0.84.

### `network-engineer`: Network engineer

**Day to day:** designs, configures and runs networks: routers, switches, firewalls, WAN and
data-centre links.

**Titles (1,402, 0.9%; ~1.5% with tail):** `network engineer` 1,087; `network security engineer`
133; `networking engineer` 93; `network administrator` 89.

**Edges:**
- `network security engineer` → here (see `security-engineer`).
- `networking engineer` → here, unsure: its raw titles are "Software Engineer - Networking
  Datapath", developers of network operating systems (junos, mpls, evpn). The day is software, not
  running a network; it might belong to `embedded-engineer`. See "Normalization notes".
- `network architect` (73) → here.
- `digital network exploitation analyst` (67) → `security-engineer`, not here: offensive
  intelligence work, not running networks.
- `network sales engineer` (15) → `sales-engineer`: the job is selling.

### `sales-engineer`: Sales / solutions engineer

**Day to day:** helps sell and keep a technical product: runs demos and proofs of concept,
answers customers' technical questions, and looks after accounts technically after the sale.

**Titles (1,564, 1.0%; ~1.5% with tail):** `solutions engineer` 570; `sales engineer` 397;
`technical account manager` 255; `solution engineer` 156; `customer success engineer` 105; `ai
solutions engineer` 81.

**Edges:**
- `technical account manager` → here: names a job, sorted whole; the technical care of an account
  after the sale. Nearest sales engineer 0.71.
- `forward deployed engineer` → `forward-deployed-engineer`: writes production code at the
  customer; the sales engineer demonstrates and advises.
- `solutions architect` → `solutions-architect` (above).
- `technical support engineer` → `support-engineer`: solves tickets after something breaks; not
  part of a sale or an account.
- `account manager` (not in the top 200) → no role: sales without technical work.

### `support-engineer`: Technical support engineer

**Day to day:** solves users' technical problems: takes tickets, troubleshoots, escalates to
engineering, whether the users are a product's customers or an organization's own staff.

**Titles (999, 0.6%; ~1.5% with tail):** `technical support engineer` 347; `support engineer` 182;
`it analyst` 126; `it support specialist` 124; `it support engineer` 118; `technical support
specialist` 102.

Product support and IT support (help desk) are one role: the day is the same queue of tickets,
and the tail is long (`service desk analyst` 68, `it support technician` 60, `product support
engineer` 57, `application support engineer` 45, `help desk technician` 15…).

**Edges:**
- `it analyst` → here: its nearest titles are service desk analyst 0.62 and service desk 0.61,
  and its raw titles include "Analyst, IT Client Services". Unsure.
- `it engineer` → `systems-administrator`: runs the device fleet and identities (jamf, intune,
  okta) rather than answering tickets; nearest it systems engineer 0.86. Unsure; the two roles
  meet here.
- `production support engineer` (20) → here: answers incidents in production systems.
- `customer success engineer` → `sales-engineer`: keeps an account, not a ticket queue.

### `program-manager`: Program / project manager

**Day to day:** coordinates the delivery of technical work across people and teams: plans,
schedules, dependencies, risks and the process the team runs.

**Titles (1,336, 0.9%; ~1.2% with tail):** `technical program manager` 753; `project manager` 216;
`scrum master` 180; `technical project manager` 95; `program manager` 92.

**Edges:**
- `scrum master` → here: runs the team's delivery process; nearest software delivery 0.90 and
  product owner 0.88.
- `product owner` → `product-manager` (above).
- `delivery solutions architect` (35) → `solutions-architect`: the head is architect.
- `it project manager` (70) → here: the job word, sorted whole.

### `research-scientist`: Research scientist

**Day to day:** advances methods, mostly in machine learning and AI: designs and runs experiments,
trains research models, and publishes or hands results to product teams.

**Titles (1,082, 0.7%; ~1.2% with tail):** `research engineer` 418; `research scientist` 340;
`machine learning scientist` 131; `applied scientist` 113; `ai scientist` 80.

**Edges:**
- `applied scientist` → here, unsure: Amazon's title for scientists who build models for
  products; nearest ML engineer 0.72 and ML scientist 0.69. It could go to
  `machine-learning-engineer`.
- `research engineer` → here, not `machine-learning-engineer`: the engineer of a research team
  (jax, pytorch, cuda); nearest research scientist 0.90.
- `security researcher` → `security-engineer`; `ux researcher` (74) → no role (design research);
  `quantitative researcher` → `data-scientist`.
- `scientist` (bare) → no role: its raw titles include "Associate Scientist, Protein
  Therapeutics".

### `forward-deployed-engineer`: Forward deployed engineer

**Day to day:** works inside a customer's organization to build and deploy the product in their
systems, writing production code and integrations for them.

**Titles (1,618, 1.0%; ~1.1% with tail):** `forward deployed engineer` 1,435; `forward deployed
software engineer` 95; `forward deployed ai engineer` 88.

Kept apart from software engineering and from sales engineering: it clears the floor on its own
title, and its day is neither (code like an engineer, with a customer like a sales engineer).

**Edges:**
- `forward deployed ai engineer` → here (see `ai-engineer`).
- `solutions engineer` → `sales-engineer`: demos before the sale, not production code after it.
- `applied ai engineer` → `ai-engineer`, although its nearest is forward deployed engineer 0.75:
  the title does not say the work is at the customer.
- `implementation engineer` (71) → no role in the draft; it is near this role (configures the
  product for a customer) and may join it at sorting.

### `frontend-engineer`: Frontend engineer

**Day to day:** builds the part of a web application users see and use: interfaces, components,
styling and client-side state in the browser.

**Titles (1,290, 0.8%; ~1.1% with tail):** `frontend engineer` 921; `frontend developer` 167;
`web developer` 114; `web engineer` 88.

The share is understated: normalization takes a lone skill out of the title, so "React Developer"
reads as `developer` and lands in `software-engineer`.

**Edges:**
- `ui engineer` (78) → here: nearest frontend engineer 0.86.
- `web developer` → here (see `full-stack-engineer`).
- `design engineer` → no role: its raw titles split between UI design engineers (figma, tailwind)
  and chip design engineers (uvm, verilog).
- `react native engineer` → `mobile-engineer`.

### `enterprise-platform-developer`: Enterprise platform developer

**Day to day:** configures, extends and integrates a vendor's business platform (Salesforce, SAP,
ServiceNow, Workday, Dynamics) for the company that runs it.

**Titles (669, 0.4%; ~1.1% with tail):** `salesforce developer` 253; `servicenow developer` 201;
`salesforce engineer` 119; `salesforce administrator` 96.

The tail is wide: `servicenow engineer` 54, `salesforce solution architect` 44, `salesforce
technical architect` 42, `sap solution architect` 31, `sap consultant` 28, `workday engineer` 21…
The rule for it is the platform word, whatever the head (developer, engineer, administrator,
consultant, architect, analyst), except a job word sorted whole (`salesforce product owner` →
`product-manager`).

**Edges:**
- `salesforce administrator` → here, not `systems-administrator`: configures the platform's flows,
  objects and permissions; nearest salesforce architect 0.86.
- `sap support engineer` (16) → here: the platform word names the kind of work.
- `technical consultant` → no role, although some raw titles are NetSuite or D365 consultants:
  the normalized title has lost the platform word.
- `administrator` → `systems-administrator` (above), for the same reason.

## Top-200 titles left without a role (3,772 vacancies, 2.4%)

| Title | Vacancies | Why |
|---|---|---|
| `analyst` | 844 | Bare head: raw titles split between security (Falcon Complete), BI, service desk and finance. See "Unsure". |
| `automation engineer` | 715 | Raw titles mix test automation, industrial control (DCS), RPA (UiPath) and IT automation; the title does not say which. |
| `consultant` | 340 | Bare head; raw titles span ERP, data, product and strategy consulting. |
| `design engineer` | 185 | Splits between UI design engineers and chip design engineers. |
| `technical consultant` | 175 | Implementation consulting on many platforms; names no kind of work. |
| `gtm engineer` | 140 | Builds sales and marketing automation (zapier, n8n, clay); under 1% and no role covers it. |
| `specialist` | 121 | Bare head. |
| `design verification engineer` | 119 | Chip verification; hardware under 1%. |
| `verification engineer` | 111 | Same. |
| `financial analyst` | 108 | Finance, not engineering. |
| `intern` | 106 | Names no work. |
| `field engineer` | 106 | On-site controls and telecom service. |
| `technology` | 103 | CTO, Technology Manager: see "Unsure". |
| `technical writer` | 97 | Documentation; under 1%. |
| `scientist` | 90 | Bare head; includes life sciences. |
| `stage` | 86 | French for internship; names no work. |
| `data specialist` | 83 | Raw titles span e-discovery, BI and data entry. |
| `developer relations` | 82 | Under 1% with `developer advocate` 51. |
| `mech and robotics tech` | 81 | Hands-on technician, not software. |
| `data` | 80 | Head of Data / Data Manager: leads a data team that may be engineering, analytics or science. See "Unsure". |

## Kinds of work considered and rejected for being under 1%

Each estimate is the top 200 plus what `titles.tsv` shows.

- **Business analysis** (~1,300, 0.84%): folded into `product-manager` rather than rejected; see
  "Open points".
- **Robotics and autonomy** (~800, 0.5%): folded into `embedded-engineer`.
- **Database administration** (~760, 0.5%): folded into `systems-administrator`.
- **Security operations (SOC)** (~800, 0.5%): folded into `security-engineer`.
- **Quantitative finance** (~750, 0.5%): researchers and analysts to `data-scientist`,
  developers to `software-engineer`; no role of its own.
- **Automation (industrial, RPA, IT)** (~1,000 across `automation engineer`, `process automation
  engineer`, `rpa developer`…, 0.6%, mixed): no role; the titles do not say which automation.
- **Hardware and chip design** (~450 with design and verification titles, 0.3%): no role. Not
  covered by `embedded-engineer`, which writes software for devices, not the devices' logic.
- **UX and product design** (~250, 0.2%, including `ux researcher` 74): no role.
- **Developer relations** (~190 with `developer advocate` and `developer relations engineer`): no
  role.
- **Technical writing** (~100): no role.
- **Game development** (~130): no role; `game developer` and `gameplay engineer` read as
  `software-engineer` by their head under the draft, which a reviewer may prefer to leave without
  one.
- **GTM engineering** (~200 with `go to market engineer`, `gtm systems engineer`): no role.
- **Blockchain** (~35): no role.
- **SRE apart from platform** (0.8% top 200): under the floor and the same work; merged.
- **Implementation / professional services** (`implementation engineer` 71, `technical
  consultant` 175, `deployment engineer` 33): no role in the draft; nearest `forward-deployed-engineer`.

## Unsure

- **`analyst` (844).** The largest title left without a role. Within the subset its skills are
  tableau, power-bi, sql, r (nearest data analyst 0.82), much as a bare `engineer` asks for
  software skills. But its raw titles include security analysts and service desk analysts, and
  the ADR settles only `engineer`, `developer` and `engineering` as bare heads. Proposed: no role,
  unless the reviewer or the blind sample shows most are data analysts.
- **`technology` (103) and `data` (80).** Leaders of a whole area: a CTO, a Head of Data. By the
  ADR's reading of `engineering`, `technology` could join `software-engineer`; `data` has no single
  role. Proposed: none, both.
- **`systems engineer` (1,585)** → `systems-administrator`. Right for the skills, wrong for
  aerospace and defence systems engineering. The blind sample should report it by ATS.
- **`networking engineer` (93), `simulation engineer` (89), `storage engineer` (105),
  `performance engineer` (139), `integration engineer` (189).** All come largely from "Software
  Engineer, X" raw titles. Each is placed by the area it names; a software engineer on a
  networking datapath may better be `embedded-engineer` than `network-engineer`.
- **`analytics engineer`** → `data-engineer` or `data-analyst`.
- **`applied scientist`, `machine learning scientist`** → `research-scientist` or
  `machine-learning-engineer`.
- **`solutions architect`** → `solutions-architect` or `sales-engineer`.
- **`it analyst`, `it engineer`**: the edge between `support-engineer` and `systems-administrator`.
- **`ai data engineer`**: one head or two (see below).
- **`architect` and `administrator`**: bare heads placed here by their raw titles and skills, not
  settled by the ADR.

## Open points

1. **Business analysts in `product-manager`.** The draft folds them in at about 0.84%, saying the
   description covers requirements work. The alternatives are a `business-analyst` role under the
   floor, or leaving them without a role (about 1,300 vacancies).
2. **Robotics in `embedded-engineer`.** Folded at about 0.5%; the alternative is no role for
   robotics, which loses about 800 vacancies but keeps `embedded-engineer` narrower.
3. **Three roles are near the floor with their tail and under it in the top 200:**
   `enterprise-platform-developer` (~1.1%, 0.4% top 200), `frontend-engineer` (~1.1%),
   `support-engineer` (~1.5%, 0.6% top 200). Their size rests on the tail estimate.
   `frontend-engineer` is understated by the skill rule (next section).
4. **No role is over 25%.** `software-engineer` is about 15.9%; proposed kept.

## Normalization notes for #92 and the rule

Seen while drafting; they change shares but not the roles.

- "React Developer", "Java Developer", "Laravel Developer" read as bare `developer` (the skill
  rule), so frontend and backend work with a framework in the title lands in `software-engineer`.
- "Software Engineer, X" reads as `x engineer`, which gives a software engineer a non-software
  area's head: `networking engineer`, `storage engineer`, `simulation engineer`.
- "Data & AI Engineer" reads as one title, `ai data engineer`, where it may name two heads.
- French and other-language heads (`ingénieur`, `développeur`, `stage`, `analista`, `ingeniero`,
  `desenvolvedor`) are not reduced: `ingénieur` keeps "Informatique industrielle" and "Systèmes
  Embarqués" out of the title, so embedded work lands in `software-engineer`.
- `administrator`, `consultant` and `specialist` lose their platform word (Oracle, SAP, SharePoint,
  NetSuite) as a skill, which hides what kind of work they are.
