# A role is read from the title, against a curated list

Status: accepted (amended three times on 2026-10-26)

The Finder filters by role and builds a landing page for each one, and the Explorer charts by
role. So a role has to be a short, stable list of values. Over the 76,854 vacancies of the
engineering subset there are 40,622 distinct cleaned titles, and 33,452 of them appear only
once. Taking a title's facts out (levels, work mode, places, skills) still leaves 35,149
(`docs/measurements/2026-10-title-breakdown-probe.md`). Decided with Elias on 2026-10-22.

- **A role is what the title says the work is.** Normalization breaks the title down:
  - It takes out the title seniority, work-mode words, places and the skills the title names.
  - It reads the function head as ADR-0009 does, with the words in front of it.
  - It drops what follows the head, unless those words name the kind of work, which then moves
    in front: `Software Engineer, Backend` reads as `backend engineer`.
  - A skill that is the only word in front of the head leaves too: `Java Developer` reads as
    `developer`.
  - What is left is the **normalized title**.
- **The role list is a curated file**, stable between runs so that a role's landing page keeps
  its URL. It is grown on each new snapshot, the way skill discovery grows the taxonomy.
  - A normalized title joins the list once enough vacancies carry it. The floor is set by
    measurement, not as a fixed number of roles.
  - The list is flat, with no parent roles. The Finder's role filter takes several roles at once.
  - Two titles are one role when a candidate reading them would expect the same day-to-day work.
    Where that is unsure, they stay apart. Agents apply the test, and another agent reviews the
    draft.
- **Leadership words stay in the role** (PRD §5.3). `lead backend engineer` and `engineering
  manager` are roles of their own, if they clear the floor.
- **A title naming two heads gets both roles**: `Data Scientist / Machine Learning Engineer`. Role
  shares in the Explorer can add up to more than 100%.
- **A normalized title the list does not hold leaves the vacancy without a role.** No role is
  guessed. The Finder's role filter says how many vacancies it cannot see, as the work-mode and
  salary filters do. The Explorer's role charts show the vacancies with no role as their unknown
  share.
- **Done when the list covers 70% of the engineering subset.** There is no labelled sample. A
  role is assigned only by exact match against a reviewed list, so the list review and a report
  of the most common unmatched normalized titles are the measurement.

## Considered options

- **Role families**, a short list chosen up front and assigned by rule. Rejected: the families
  would be invented rather than read off the data, and checking whether they fit would need a
  criterion and a rule loop.
- **Title search**, with no role derived. Rejected: the Explorer's role × skill and role ×
  seniority views need a value to group by, and so does a landing page per role.
- **The N most common cleaned titles as roles.** Rejected: the 1,000 most common cover only
  35.8%, and synonyms stay apart.
- **Profiling the rest**: giving a vacancy the role whose profile its other fields (skills,
  department, domain, practices) look most like. Moved after the MVP. It needs extractors nothing
  else in the MVP reads, and a scoring method of its own that would have to be measured. If it
  used skills, the Explorer's "best stack for a role" would partly measure the assignment rule.
  It only adds roles to vacancies that have none, so it can follow later.

## Consequences

- PRD §5.2's "role families finer than engineering role are not modelled" still holds. The role
  list is made of titles companies actually write, not of families.
- `staff` and the numbered levels are read during the title breakdown, because it has to take
  them out anyway. They cannot move to cleaning: `staff nurse` is only harmless inside the
  engineering subset.
- About a quarter of the subset is expected to have no role at launch, by the probe's estimate.

## Amended 2026-10-26

Decided with Elias in #25's triage, after the breakdown was measured again on
`classified-2026-10-25` with Workday (`docs/measurements/2026-10-title-breakdown-probe-workday.md`):
about 786 normalized titles are needed for 70% of the subset, against about 300 before Workday.

- **A role is the employer's word for the work, not a cluster of skills.** It is read from the
  title. What a role is for: it tells a visitor the kind of work, which skills do not. A data
  engineer and a web developer both ask for Python and SQL.
- **Titles and roles are kept apart.** Breaking a title down into its normalized title is one
  piece of work, and the role list, which maps normalized titles to roles, is another. Many
  normalized titles map to one role. A visitor who searches a title is shown the vacancies of its
  role, and the Explorer counts by role, not by title.
- **Skill profiles are evidence for the role list, never its rule.** Two normalized titles join
  one role by the test above, the same day-to-day work. The similarity of the skills their
  vacancies ask for is shown to the agent that drafts the merges and to the one that reviews them.
  It does not decide a merge, because it cannot tell who does what: `engineering manager` and
  `software engineer` ask for nearly the same skills (0.93) and are different work. Membership
  that followed similarity would also change between snapshots and break a role's series.
- **Role nearness is computed beside the list.** It is how near two roles are, from their skill
  profiles, computed again on each snapshot and allowed to change. It is what shows a visitor the
  roles near the one they picked. It never moves a title from one role to another.
- **Management is a fact of its own, and the role is what is left.** A leadership word that leads
  an area (`Engineering Manager, Backend`, `Director of Cybersecurity`, `Head of Security`, `VP
  Engineering`) is taken out of the title as its **management level**: lead, manager, director or
  executive. The area it leads is what the role is read from. This replaces "leadership words stay
  in the role" above. The worry behind it, recommending a manager's job to someone who did not
  apply for one, is met by the level being its own field, which a filter or a match can hold
  apart; skills cannot, since `engineering manager` and `software engineer` ask for nearly the
  same ones. Stored apart, the facts can be shown as titles, roles, levels or any mix of them as
  the Finder's presentation changes, without deriving them again.
  - A title that names a job rather than a rank keeps its word: `product manager`, `technical
    program manager`, `account manager`. The role list claims those titles whole.
  - A bare `Manager` has a management level and no role, and does not count toward the 70%.
  - A leader of a kind of engineering is read as an engineer of that kind (decided with Elias in
    #92): a `Head of Data Engineering` is a `data engineer`. A bare `Engineering Manager` keeps
    `engineering`, which the role list maps.
  - `lead` is a level of its own: a tech lead often manages no one, a team lead often does, and
    the title does not tell them apart.
  - The management level is not the seniority level: a `Senior Engineering Manager` has both.
- **Practices are not extracted from descriptions** for roles. Most skills imply their practice:
  dbt implies data modelling, Airflow implies orchestration. Grouping skills by function
  (Airflow, Dagster and Prefect are all orchestration) may serve matching later, as a mapping
  over the taxonomy rather than an extractor.


## Amended again 2026-10-26: about 25 roles

Decided with Elias in #25's grilling, and written down only later the same day.

- **There are about 25 roles.** Roles are what a visitor is shown when statistics are given by
  role, which is the point of keeping them apart from titles: a chart or a list of a thousand
  titles tells nobody anything. The normalized titles are the fine level, many and close to what
  employers write; the roles are the few that every normalized title falls into.
- **The roles are read off the data, then the titles are sorted into them.** The roles are taken
  from the most common normalized titles and the kinds of work they name, not invented up front.
  Then each normalized title above the floor is placed in the role whose day-to-day work a
  candidate reading it would expect. This replaces growing the list by pairwise merges, and with
  it "where that is unsure, they stay apart": with about 25 roles, a title goes to the nearest
  one. Skill profiles stay evidence, never the rule.
- **A normalized title that fits no role leaves its vacancies without one**, as one the list does
  not hold does. So coverage no longer follows from the floor alone, and the 70% is measured after
  the titles are sorted.
- The "role families" option above was rejected because the families would be invented. These
  roles are not: they are named from the most common titles and checked by the same review.

## Recorded later on 2026-10-26: what a role is for, and what is settled

Discussed with Elias in #25's triage and confirmed by him; figures in
`docs/measurements/2026-10-title-breakdown-probe-workday.md` §3 and §4.

**What a role is for.** It tells the kind of work, which skills cannot: a data engineer and a web
developer both ask for Python and SQL. It is used in three places, in this order of weight:

- **The Explorer's statistics.** "What do data engineers' vacancies ask for" needs one group per
  vacancy; skills counted over the whole subset mostly measure its mix of roles.
- **Matching.** A CV is a list of roles held, and experience is asked for by role ("three years as a
  data engineer"). The roles a person held say what experience they bring; the roles they pick say
  where they want to go, which a CV does not (a backend engineer moving into AI picks AI engineer).
- **The Finder's filter**, the first one a visitor reaches for.

**Settled for the role list:**

- **A normalized title falls into one role**, the one whose day-to-day work is nearest
  (`ml platform engineer` into one of ML engineer or platform engineer). A vacancy has two roles
  only where its title names two heads.
- **The roles and their names are drafted by an agent and reviewed by another**, from the most
  common normalized titles. Elias sets the goal; he does not settle each name.
- **Bare heads that are no rank fall into software engineer**: `engineer`, `developer`, and
  `engineering` with its manager's level (a bare `Engineering Manager`). Within the subset a bare
  `engineer` asks for the skills of `software engineer` (0.88). A bare rank (`Manager`) has no role.
- **Role nearness stays** with about 25 roles: roles near each other are still worth showing
  (backend and full stack), and matching needs it to say whether experience carries over.

## Considered options, added later on 2026-10-26

- **Many role tags on a vacancy**, as Himalayas shows 7 to 15 overlapping, generated ones on each
  posting. Rejected: a vacancy counted under every tag makes counts by role mean nothing, and the
  same posting turns up under every filter. What is worth taking from it is splitting a role's
  vacancies by level. Reaching the vacancies of near roles is role nearness's work, done when a
  visitor asks for it.
- **Roles as clusters of skill profiles.** Rejected: the skills tell the kinds of work apart but
  not who does what in them (`engineering manager` ~ `software engineer` 0.93), similarity has no
  natural cut and chains one title to the next, and membership that followed it would move
  between snapshots and break a role's series.
- **Leadership kept in the role** (`engineering manager` as a role of its own), as the first
  version of this ADR said. Superseded by the management level: the role can be shown with the
  level as one label, but a role holding the rank cannot say which area it leads.
- **Extracting practices from descriptions** (MVC, ETL, API design). Not done: a distinctive skill
  implies its practice (dbt implies data modelling), and the skills already separate the kinds of
  work. The one gap is the quarter of the subset that names two skills or fewer; whether those
  descriptions name practices instead is unmeasured.

