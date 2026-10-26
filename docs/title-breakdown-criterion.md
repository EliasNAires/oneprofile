# What a title breaks down into

The criterion of the title breakdown (#92, ADR-0024 as amended on 2026-10-26). It is what a labeller
reads before breaking a title down, and what the breakdown's rule answers to: where the rule and this
file disagree, this file is right. It says what a vacancy's **normalized titles** and **management
level** are. The levels a title names are taken out of the normalized title, but are not measured
against this file.

Every vacancy read under it is in the engineering subset: its title is already known to name an
engineering-type job.

## The normalized title

The normalized title is what the title says the work is: its **function head**, with the words in
front of it that name the kind of work. It is written in lowercase, as words separated by single
spaces, with no punctuation except what belongs to a word (`c++`, `.net`, `r&d`). A title naming
two jobs has two normalized titles. A title naming no work at all, as a bare `Manager`, has none.

### The function head

1. **The head is the noun naming what the person does**: engineer, developer, scientist, analyst,
   architect, administrator, designer, consultant, technician, researcher, programmer, tester,
   writer, officer, advocate, coach, and the like.
   - **Several words can be one head**, where the first alone would cut the job short: `product
     manager`, `program manager`, `project manager`, `account manager`, `delivery manager`,
     `release manager`, `customer success manager`, `product owner`, `scrum master`, `chief of
     staff`, `member of technical staff`, `developer advocate`, `tech lead`, `technical lead`,
     `team lead`.
   - **A plural head is written in the singular.**
   - **Some words yield to a head behind them**: intern, specialist, expert, member, fellow,
     graduate, trainee, apprentice, student, partner, quant, support, operations, tech. They are the
     head only when no stronger head follows: `Support Engineer` has the head `engineer`, with
     `support` in front of it. Where none follows, a word naming a person (`intern`, `specialist`)
     is the head before `support` or `operations` is: `IT Support Specialist` → `it support
     specialist`.
   - **`engineering` and `development` are the head only when no function head is named**:
     `Software Engineering Intern` → `software engineering`.
   - **Two heads side by side, with nothing between them, are one compound head**, and the first is
     the head: `Programmer Analyst` → `programmer`.
2. **Two heads joined by `/`, `&`, `and` or `or` give two normalized titles**: `Data Scientist / ML
   Engineer` → `data scientist | machine learning engineer` (14). A bare second head borrows the
   first's front: `Data Scientist/Engineer` → `data scientist | data engineer`.

### What is kept around the head

3. **The words in front of the head are kept; what follows it is dropped.** What follows is usually
   a team, a product, a client, a place or a code: `Platform Engineer, Payments` → `platform
   engineer`; `Principal Engineer, DSP` → `engineer`.
4. **The kind of work moves in front.** Where what follows the head names the kind of work, and the
   front names none, it moves in front of the head.
   - It replaces a bare `software` there: `Software Engineer, Backend` → `backend engineer`;
     `Engineer, Software` → `software engineer`; `Product Manager, AI` → `ai product manager`;
     `Software Engineer in Test` → `test engineer`.
   - **The kind of work is the technical field the job works in**: backend, frontend, full stack,
     data, machine learning, ai, platform, infrastructure, cloud, devops, site reliability,
     security, network, mobile, web, embedded, firmware, systems, test, qa, automation, robotics,
     research, analytics, product, it, and other fields of the same sort. It is not the team, the
     product, the client, the industry or the company the job works for (`Payments`, `Ads`,
     `Healthcare`, `Risk`): those are dropped from behind the head, and kept where the company
     wrote them in front of it (`Equities Data Engineer`).
   - **Where the front already names a kind of work, nothing moves**: `Data Engineer, Platform` →
     `data engineer`.
   - **Only the first run of kind words moves**, and a slash or an `&` ends it: `Team Lead, Android
     Core Product` → `android team lead`; `Software Engineer, AI & Data` → `ai engineer`.
   - **The kind is looked for only next to the head**: in the rest of the head's part and the part
     right after it, parts being what commas, brackets and spaced dashes cut a title into. A kind two
     parts behind the head stays where it is: `Software Engineer - Payments, Backend` → `software
     engineer`.
5. **What a title names after `at` goes**: it is the company or the team. `AI Engineering Lead at
   AIOS` → `ai engineer`.

### What is taken out, wherever it stands

6. **Seniority**: senior, junior, principal, staff, mid (and mid level, intermediate, medior, and
   middle, except in `Middle East` and `Middle Office`), entry level, and the numbered levels I, II,
   III, IV.
7. **Work modes** (remote, hybrid, onsite) and **places**, whether or not the location field names
   them (`Remote, Latin America`, `DACH`).
8. **Codes, years and hours**: requisition and job codes, any word with a digit except those naming
   the work (`3d`, `5g`, `web3`, `b2b`), a cohort season with its year (`Summer 2027`), years of
   experience, `founding`, and shift or hours words.
9. **Corporate grades**: `associate`, and a vice president, assistant vice president or managing
   director beside a function head or a word naming a person (`VP System Analyst` → `systems
   analyst`; `Vice President - Inflation Quant` → `inflation quant`).
10. **Skills.** A language, a framework or a tool the title names goes: `Java Developer` →
    `developer`, `Senior Python Engineer` → `engineer`. A platform a whole job is built on stays as
    the kind of work: Android, iOS, React Native, Flutter, Salesforce, ServiceNow, SAP, Workday
    (`Senior Android Engineer` → `android engineer`).
11. **A leadership word**, read as the management level below.

### Leaders

12. **A leadership word that leads an area is taken out, and the area is the normalized title**:
    lead, leader, manager, director, head, vice president (VP, SVP, AVP, EVP), president, chief,
    managing director.
    - **The area is the words around the leadership word**, or, where it stands alone, the part
      after it, or else the title's first part: `Director of Cybersecurity` → `cybersecurity`;
      `Head of Security` → `security`; `Senior Manager, Data Applications` → `data applications`;
      `Digital Transformation - Senior Manager` → `digital transformation`. A kind of work from the
      part after it moves in front, as in 4: `Engineering Manager, Backend` → `backend engineer`.
    - **An area that is a kind of engineering reads as its engineer**: `Head of Data Engineering` →
      `data engineer`; `Software Engineering Manager` → `software engineer`. A bare `Engineering
      Manager` stays `engineering`.
    - **A chief officer leads the area between the two words**: `Chief Technology Officer` →
      `technology`. A chief's *office* is a department, not a chief: `Chief Data Office` names no
      leader.
    - **Where a function head is present, the head is read** as in 1 to 10, and the leadership word
      only gives the management level: `Lead Software Engineer` → `software engineer`; `Manager,
      Solutions Architect` → `solutions architect`. Where a leader stands in an earlier part with an
      area of its own before the head, the area is read: `Manager, Software Engineering - Developer
      Support` → `software engineer`.
    - **A bare leadership word has no normalized title**: `Manager` → none.
    - **A title naming a job rather than a rank keeps its word as a head**: `product manager`,
      `technical program manager`, `account manager`.

### One way to write it

13. **A word is written one way**: `fullstack`, `full-stack` and `full stack` are `full stack`;
    `back end` is `backend`, `front end` is `frontend`, `cyber security` is `cybersecurity`,
    `pre sales` is `presales`, `system` is `systems`, and `sw` is `software`.
14. **An abbreviation and its long form are one, written the way most titles write it.** `ML` is
    `machine learning`, `SRE` is `site reliability`, `BI` is `business intelligence`, `DBA` is
    `database administrator`; `quality assurance` is `qa`, `generative ai` and `gen ai` are `genai`,
    `artificial intelligence` is `ai`, `information technology` is `it`, `user experience` is `ux`.
    An abbreviation of a whole title is written out: `SWE` → `software engineer`, `SDE` → `software
    development engineer`, `FDE` → `forward deployed engineer`, `MLE` → `machine learning
    engineer`, `MTS` → `member of technical staff`, and a bare `SRE` → `site reliability engineer`.
15. **`software` adds nothing before a kind of work that is only ever software**: backend,
    frontend, full stack, web, mobile, Android, iOS, React Native, Flutter, UI. `Backend Software
    Engineer` → `backend engineer`, as `Software Engineer, Backend` reads. Before any other kind it
    stays: `Embedded Software Engineer` → `embedded software engineer`.
16. **No word is written twice running, nor a word the head already names**: `Senior Product
    Manager, Product Foundations` → `product manager`.
17. **The same words in another order, naming the same work, are one normalized title**, written in
    the order most titles use: `Automation QA Engineer` → `qa automation engineer`; `ML/AI Engineer`
    → `ai machine learning engineer`. Where the order changes the work, they stay apart: an
    `application security engineer` secures applications, a `security application engineer` builds
    them for security.

### A title not in English

18. **A title not in English is not broken down here.** How such titles are read is not decided
    (#94). Mark it as not English. A title written in English with a translation or a note in
    another language beside it is read by its English part: `【フルリモート相談可能】Senior Platform
    Engineer（シニアプラットフォームエンジニア）` → `platform engineer`.

## The management level

The highest of the leadership words the title names:

- **LEAD**: lead or leader, in front of a head or leading an area; and the heads tech lead,
  technical lead and team lead.
- **MANAGER**: manager, as a rank.
- **DIRECTOR**: director or head (of an area).
- **EXECUTIVE**: vice president, VP, SVP, EVP, AVP, president, chief, managing director, or a chief
  officer (CTO, CIO, CISO).

**None** where the title has no leadership word, or only a job title such as product manager,
account manager or technical program manager. A corporate grade (9) is not a level, and `associate`
never is. The management level is not the seniority: a `Senior Engineering Manager` is a MANAGER,
and its seniority is read elsewhere.
