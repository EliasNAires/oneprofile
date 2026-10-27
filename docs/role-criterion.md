# What a vacancy's role is

The criterion of the role list (#25, ADR-0024 and its amendments). It is what the agents that sort
normalized titles read, what the blind sample's labellers read, and what the role rule answers to.
When the rule and this file disagree, this file is right.

It was drafted from the 200 most common normalized titles of `classified-2026-10-25` and reviewed
by a second agent (`docs/measurements/2026-10-role-list/`, `draft.md` and `review.md`). Shares and
counts are kept out of it on purpose: they are measured, and they change with each snapshot.

## What a role is

A role is the kind of day-to-day work a candidate reading the title would expect. It is the
employer's word for the work, read from the normalized title, not a cluster of skills. Two titles
whose vacancies ask for the same skills can be different work: `engineering manager` and `software
engineer` ask for nearly the same ones. Skill nearness is evidence for a placement, never its
reason.

The subset is every vacancy classified IN by `docs/engineering-role-criterion.md`: a role that writes
code (Q2), works on engineer-facing artifacts (Q3), or that a software background alone opens (Q4).
A product manager, a business analyst or a sales engineer is in it on the same footing as a backend
engineer.

## How a normalized title is read

1. **One role per normalized title**, the one whose day-to-day work is nearest. A vacancy has two
   roles only where its title names two heads.
2. **A leader's role is the area they lead.** The rank is the management level, already taken out.
3. **A title that names a job rather than a rank is read whole**: `product manager`, `technical
   program manager`, `technical account manager`, `scrum master`.
4. **A kind word in front of the head decides**, whatever the head: `data architect` is data
   engineering, `security researcher` is security engineering, `salesforce administrator` is
   enterprise platform work.
5. **`platform`, `infrastructure` and `software` in front of the head yield to a kind word in front
   of them** that has a role: `data infrastructure engineer` is data engineering, `machine learning
   platform engineer` machine learning engineering, `security platform engineer` security
   engineering, `backend platform engineer` backend engineering. The one exception is `ai
   infrastructure engineer`: it runs GPU clusters, which is platform work.
6. **A word naming the sale sends an engineer or architect title to sales engineering**:
   `presales`, `pre sales`, `partner`, `sales`. A bare `solutions architect` stays with the
   solutions architects.
7. **A stage of a career is not work.** `intern`, `internship`, `trainee`, `graduate`, `apprentice`,
   `student`, `stage` and `stagiaire` are read past: `data science intern` is data science. On their
   own they have no role.
8. **Bare heads that are no rank fall into software engineer**: `engineer`, `developer`,
   `engineering` (a bare Engineering Manager), `technology` (a bare CTO or Technology Manager), and
   their forms in other languages (`ingénieur`, `ingeniero`, `engenheiro`, `développeur`,
   `desarrollador`, `desenvolvedor`).
9. **A title that does not say one kind of work has no role.** A bare rank (`manager`), and heads
   whose vacancies span several kinds: `analyst`, `analista`, `consultant`, `specialist`,
   `scientist`, `cientista`, `data`, `systems engineer`, `automation engineer`, `design engineer`,
   `technical consultant`, `field engineer`.
10. **A kind of work under about 1% of the subset joins a role only where that role's description
    below already covers it.** Otherwise it has no role. No role is guessed to raise coverage. The
    kinds left without one are listed at the end.
11. **The list is flat.** No role holds another.

## The roles

Each role has a slug that never changes, a label that may, its day-to-day work, and worked examples
at its edges with the roles nearest it.

### `software-engineer`: Software engineer

Designs, writes and maintains software, where the title names no more specific area of the stack,
platform or domain.

- `software engineer`, `member of technical staff`, `software architect`, `tech lead`, `quantitative
  developer`, `integration engineer`, `integrations engineer`, and the bare heads of rule 8.
- `backend engineer` → backend engineer: the title names the server side; a software engineer may
  do any part.
- `software architect` → here, not solutions architect: the architect of one product's code writes
  and reviews it daily.
- `research member of technical staff` → research scientist, `ai member of technical staff` → AI
  engineer: the kind word names the work.
- `cloud developer` → here, not platform engineer: develops cloud software rather than running the
  infrastructure.
- `game developer`, `gameplay engineer`, `graphics engineer` → no role: `game` and `graphics` name
  an area this role does not cover.

### `backend-engineer`: Backend engineer

Builds the server side of an application: APIs, services, databases, queues, and the integrations
between systems.

- `backend engineer`, `backend developer`, `distributed systems engineer`, `backend platform
  engineer`.
- `full stack engineer` → full stack engineer: the title includes the interface.
- `data engineer` → data engineer: the pipelines carry data for analysis, not an application's
  requests.
- `integrations engineer` → software engineer, with `integration engineer`: neither names an area
  reliably.

### `frontend-engineer`: Frontend engineer

Builds the part of a web application users see and use: interfaces, components, styling and
client-side state in the browser.

- `frontend engineer`, `frontend developer`, `web developer`, `web engineer`, `ui engineer`.
- `web developer` → here, not full stack: the day is the site; some web developers are full stack.
- `react native engineer` → mobile engineer: the product is a phone app.
- `design engineer` → no role (rule 9): UI design engineers and chip design engineers share it.

### `full-stack-engineer`: Full stack engineer

Builds product features end to end, from the user interface through the API to the database.

- `full stack engineer`, `full stack developer`, `product engineer`.
- `product engineer` → here, not software engineer: the title promises shipping product features
  across the stack.
- `full stack ai engineer` → AI engineer: the product is the AI feature.
- `product manager` → product manager: decides what is built, does not build it.

### `mobile-engineer`: Mobile engineer

Builds apps for phones and tablets, native (iOS, Android) or cross-platform.

- `mobile engineer`, `android engineer`, `ios engineer`, `android developer`, `ios developer`,
  `mobile application developer`, `react native engineer`, `flutter developer`.
- `frontend engineer` → frontend engineer: the browser, not a phone app.
- `mobile expert`, `mobile mortgage advisor`, `mobile mortgage specialist` → no role: retail and
  mortgage sales that leaked past the classification.

### `embedded-engineer`: Embedded engineer

Writes software that runs on or drives hardware: firmware, embedded Linux, drivers and kernels,
tested on the device.

- `embedded software engineer`, `embedded engineer`, `firmware engineer`, `systems software
  engineer`, `flight software engineer`.
- `robotics software engineer` → robotics engineer: perception, planning and control, not the
  firmware under them.
- `design verification engineer` → no role: chip design, not software on a device.
- `networking engineer` → network engineer, although many are software on network devices: the
  title names networks.

### `robotics-engineer`: Robotics engineer

Writes the software that lets robots and autonomous vehicles sense, plan and move: perception,
motion planning, control and simulation, tested on the machine.

- `robotics engineer`, `robotics software engineer`, `autonomy engineer`, `autonomy software
  engineer`, `perception engineer`, `simulation engineer`.
- `firmware engineer` → embedded engineer: the layer under the robot's software.
- `computer vision engineer` → machine learning engineer: trains vision models; a robot's
  perception stack is here.
- `mech and robotics tech` → no role: builds and repairs machines, a kind under 1% that no role
  covers.

### `platform-engineer`: Platform engineer

Builds and runs the infrastructure software runs on: cloud accounts, Kubernetes, CI/CD pipelines,
infrastructure as code, monitoring, on-call reliability, and the internal tooling engineers build
with.

- `platform engineer`, `devops engineer`, `site reliability engineer`, `infrastructure engineer`,
  `cloud engineer`, `cloud architect`, `devsecops engineer`, `storage engineer`, `observability
  engineer`, `database reliability engineer`, `ai infrastructure engineer`, `developer experience
  engineer`, `developer productivity engineer`, `developer platform engineer`.
- DevOps, SRE, platform, infrastructure and cloud engineers are one role: employers use the titles
  interchangeably, and a candidate expects the same pipelines, clusters and on-call.
- `devsecops engineer` → here, not security engineer: the day is pipelines with security scanning
  in them.
- `ai platform engineer` → AI engineer, `data platform engineer` → data engineer, `machine learning
  platform engineer` and `mlops engineer` → machine learning engineer (rule 5).
- `systems administrator` → systems administrator: runs an organization's own servers, not a
  product's infrastructure as code.

### `security-engineer`: Security engineer

Builds and runs the defences of systems and data: secures applications and clouds, manages
identities and access, and finds and fixes vulnerabilities.

- `security engineer`, `cybersecurity engineer`, `application security engineer`, `product security
  engineer`, `cloud security engineer`, `security architect`, `identity engineer`, `iam engineer`,
  `detection engineer`, `security operations engineer`, `penetration tester`, `security
  researcher`, `vulnerability researcher`, `information security` (a CISO).
- `security analyst` → security analyst: watches and responds to alerts rather than building the
  defences.
- `detection engineer` → here: builds the detections the analysts run.
- `network security engineer` → network engineer: the day is firewalls, VPNs and routing. This is
  an exception to rule 4.

### `security-analyst`: Security analyst

Watches systems for attacks, investigates and responds to alerts and incidents, tracks threats, and
assesses vulnerabilities, controls and compliance.

- `security analyst`, `cybersecurity analyst`, `information security analyst`, `soc analyst`,
  `security operations analyst`, `incident response analyst`, `threat intelligence analyst`,
  `exploitation analyst`, `digital network exploitation analyst`, and governance, risk and
  compliance analysts over a security word.
- `cybersecurity engineer` → security engineer: the title names building.
- `security researcher` → security engineer: finding vulnerabilities is in that role.
- bare `analyst` → no role (rule 9), although some are security analysts.

### `data-engineer`: Data engineer

Builds and runs the pipelines, warehouses and lakes that move, model and store an organization's
data for others to use.

- `data engineer`, `analytics engineer`, `data architect`, `data platform engineer`, `data
  infrastructure engineer`, `data developer`, `database developer`, `ai data engineer`.
- `analytics engineer` → here, not data analyst: builds the models in the warehouse that analysts
  query.
- `business intelligence engineer`, `business intelligence developer` → data analyst: the day is
  reports and dashboards.
- `database administrator` → systems administrator: keeps databases running rather than building
  pipelines.

### `data-analyst`: Data analyst

Turns data into answers for the business: queries, reports, dashboards and the metrics they track.

- `data analyst`, `analytics analyst`, `product analyst`, `business intelligence analyst`,
  `business intelligence developer`, `data business analyst`.
- `business analyst` → business analyst: requirements, not data.
- `data scientist` → data scientist: models rather than reports.
- `financial analyst` → no role: financial analysis is under 1%, and this role covers analysis for
  the business from its data, not budgets and forecasts.

### `data-scientist`: Data scientist

Builds statistical and machine learning models from data to explain or predict something for the
business, and reports what they find.

- `data scientist`, `data science`, `quantitative researcher`, `ai data scientist`.
- `machine learning engineer` → machine learning engineer: puts models into production as software.
- `research scientist` → research scientist: advances the methods rather than applying them.
- `quantitative researcher` → here: statistical models of markets, which this role covers.

### `machine-learning-engineer`: Machine learning engineer

Trains, evaluates and serves machine learning models in production, and builds the pipelines and
tooling around them.

- `machine learning engineer`, `ai machine learning engineer`, `mlops engineer`, `machine learning
  platform engineer`, `machine learning infrastructure engineer`, `computer vision engineer`,
  `applied scientist`.
- `ai engineer` → AI engineer: builds on models others trained.
- `applied scientist` → here, not research scientist: models for products, not published methods.
- `machine learning scientist` → research scientist: the scientist head names research.

### `ai-engineer`: AI engineer

Builds applications and agents on top of foundation models: prompting, retrieval, tool use,
evaluation, and the code around them.

- `ai engineer`, `applied ai engineer`, `ai developer`, `ai platform engineer`, `ai architect`, `ai
  software engineer`, `agentic ai engineer`, `genai engineer`, `full stack ai engineer`.
- `machine learning engineer` → machine learning engineer: trains and serves models.
- `forward deployed ai engineer` → forward deployed engineer: the day is at the customer.
- `ai solutions architect` → solutions architect: designs for a client.
- `ai infrastructure engineer` → platform engineer (rule 5).

### `research-scientist`: Research scientist

Advances methods, mostly in machine learning and AI: designs and runs experiments, trains research
models, and publishes or hands results to product teams.

- `research scientist`, `research engineer`, `machine learning scientist`, `ai scientist`, `research
  member of technical staff`.
- `research engineer` → here: the engineer of a research team.
- `applied scientist` → machine learning engineer.
- `ux researcher` → no role: design research, under 1%.

### `qa-engineer`: QA engineer

Tests software: writes test plans and automated tests, runs them in CI, and finds and reports
defects.

- `qa engineer`, `test engineer`, `qa automation engineer`, `quality engineer`, `software test
  engineer`, `test automation engineer`, `qa analyst`, `sdet`.
- `sdet` → here, not software engineer: the code it writes is for testing.
- `automation engineer` → no role (rule 9): test, industrial, RPA and building automation share it.
- `verification engineer` → no role: chip verification.

### `systems-administrator`: Systems and database administrator

Keeps an organization's own servers, databases, identities and devices running: installs,
configures, patches and backs them up.

- `systems administrator`, `it systems engineer`, `it systems administrator`, `it engineer`,
  `database administrator`, `database engineer`, `administrator`, `it` (an IT Manager).
- `systems engineer` → no role (rule 9): IT, defence systems engineering, presales and software
  engineers share it.
- `it support engineer` → support engineer: answers users' tickets rather than running the systems.
- `devops engineer` → platform engineer: a product's infrastructure, as code.

### `network-engineer`: Network engineer

Designs, configures and runs networks: routers, switches, firewalls, WAN and data-centre links.

- `network engineer`, `network security engineer`, `networking engineer`, `network administrator`,
  `network architect`.
- `digital network exploitation analyst` → security analyst: intelligence work, not running
  networks.
- `network sales engineer` → sales engineer (rule 6).

### `support-engineer`: Support engineer

Solves users' technical problems: takes tickets, troubleshoots, and escalates to engineering,
whether the users are a product's customers or an organization's own staff.

- `technical support engineer`, `support engineer`, `product support engineer`, `application
  support engineer`, `production support engineer`, `it support engineer`, `it support specialist`,
  `technical support specialist`, `it analyst`, `service desk analyst`, `help desk technician`.
- `it engineer` → systems administrator: runs the device fleet and identities.
- `customer success engineer` → sales engineer: keeps an account, not a ticket queue.

### `sales-engineer`: Sales and customer engineer

Helps sell and keep a technical product: runs demos and proofs of concept, answers customers'
technical questions, and looks after accounts technically after the sale.

- `solutions engineer`, `sales engineer`, `presales engineer`, `presales systems engineer`,
  `partner solutions architect`, `presales solutions architect`, `technical account manager`,
  `customer success engineer`, `ai solutions engineer`.
- `solutions architect` → solutions architect: the bare title names design work.
- `forward deployed engineer` → forward deployed engineer: writes production code at the customer.
- `technical support engineer` → support engineer: solves tickets, not part of a sale or an
  account.

### `solutions-architect`: Solutions architect

Designs how systems and products fit together to solve a client's or the organization's problem,
above the level of one codebase, and often presents that design to the client.

- `solutions architect`, `solution architect`, `architect`, `enterprise architect`, `technical
  architect`, `systems architect`, `ai solutions architect`, `delivery solutions architect`.
- `software architect` → software engineer, `data architect` → data engineer, `cloud architect` →
  platform engineer, `security architect` → security engineer (rule 4).
- `partner solutions architect`, `presales solutions architect` → sales engineer (rule 6).
- `salesforce architect` → enterprise platform engineer (rule 4).

### `forward-deployed-engineer`: Forward deployed engineer

Works inside a customer's organization to build and deploy the product in their systems, writing
production code and integrations for them.

- `forward deployed engineer`, `forward deployed software engineer`, `forward deployed ai
  engineer`, `implementation engineer`, `deployment engineer`.
- `solutions engineer` → sales engineer: demos before the sale, not production code after it.
- `applied ai engineer` → AI engineer: the title does not say the work is at the customer.
- `technical consultant` → no role (rule 9).

### `enterprise-platform-developer`: Enterprise platform engineer

Configures, extends and integrates a vendor's business platform (Salesforce, SAP, ServiceNow,
Workday, Dynamics) for the company that runs it.

- `salesforce developer`, `servicenow developer`, `salesforce engineer`, `salesforce administrator`,
  `salesforce architect`, `sap consultant`, `workday engineer`, `salesforce business analyst`.
- The platform word decides, whatever the head (rule 4), except a job read whole: `salesforce
  product owner` → product manager.
- `administrator` with no platform word → systems administrator.

### `product-manager`: Product manager

Decides what gets built: talks to users and stakeholders, sets priorities and the roadmap, and owns
the backlog.

- `product manager`, `product owner`, `technical product manager`, `product management`, `ai
  product manager`, `platform product manager`, `data product manager`, `product` (a Head of
  Product).
- `business analyst` → business analyst: writes requirements for decisions others make.
- `technical program manager` → program manager: coordinates delivery, does not decide what the
  product does.
- `data product manager` → here, not data engineer (rule 3).

### `business-analyst`: Business analyst

Works out what a business needs from its systems: interviews stakeholders, maps processes, writes
requirements and specifications, and checks that what is delivered meets them.

- `business analyst`, `business systems analyst`, `systems analyst`, `it business analyst`,
  `technical business analyst`, `functional analyst`, `business process analyst`, `business
  systems engineer`.
- `product owner` → product manager: decides what goes in the backlog.
- `data business analyst` → data analyst: the analysis is of data.
- `it analyst` → support engineer.

### `program-manager`: Program and project manager

Coordinates the delivery of technical work across people and teams: plans, schedules,
dependencies, risks, and the process the team runs.

- `technical program manager`, `program manager`, `project manager`, `technical project manager`,
  `it project manager`, `scrum master`, `delivery manager`.
- `scrum master` → here: runs the team's delivery process.
- `product owner` → product manager.

## Kinds of work left without a role

Under about 1% of the subset, with no role whose description covers them, on
`classified-2026-10-25`: game and graphics development, hardware and chip design and verification,
industrial, RPA and building automation, UX and design research, developer relations, technical
writing, go-to-market engineering, blockchain, financial analysis, and AI training (posts that pay
for writing or grading code to train a model). Each is reported with its count when a snapshot is
measured, and joins the list if it clears about 1%.
