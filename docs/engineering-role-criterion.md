# What counts as an engineering role

The criterion the classifier approximates and the labeller applies. It is one document because
it has two readers: the **labeller** runs the procedure below verbatim to produce ground-truth
labels, and the **classifier** (#10) reproduces it cheaply from stored lists. When the two
disagree, this file is right.

Everything in it is read in full by both readers, the rulings table included. Nothing here is
illustration.

## The three states

**IN** — the vacancy is an engineering role.
**OUT** — it is not, and the title says so.
**UNKNOWN** — the title does not carry enough to decide, with a reason recorded.

UNKNOWN is the default. IN and OUT are claims that a rule has to earn, and a title no rule
reaches is a title that has not been read. The reason says which kind of not-knowing it is, and
they are not interchangeable:

| Reason | Means | Whose problem |
| --- | --- | --- |
| `unruled` | no rule reaches this title | **ours** — a line added to a list fixes it |
| `domain_ambiguity` | the head is known, the domain is not | the corpus's |
| `scope_ambiguity` | a ruled phrase whose variants genuinely split | the corpus's |

Only `unruled` is expected to fall. #11 resolves the other two from the description body;
`unruled` does not go to #11 at all, because there is nothing for it to read that a ruling would
not settle more cheaply. ADR-0008 gates the loop on the `unruled` share.

## How a title is read

A title is a **function head** — the noun naming what the role does — with **modifiers** naming
the domain it does it in. The head is extracted during cleaning and stored, so the classifier
looks it up. Where a title names more than one head, the first is the head.

Each head carries two attributes:

- **Domain-bound or domain-free.** `engineer` and `technician` name a function embedded in a
  domain, so the modifier settles what the role is: a `civil engineer` is not a software job.
  `analyst` and `architect` name a function that operates on information *about* a domain, so the
  same modifier settles nothing: an `audit analyst` may well work on audit software.
- **Engineering-capable or not.** Whether a software domain under this head can produce an
  engineering role at all. `data scientist` is one; `data manager` is not obviously one, and
  needs a ruling before it can be.

A third class sits outside both: **never-engineering heads**, which decide OUT whatever modifies
them.

## The procedure

```
classify(cleaned_title):

    head = the first function head named in the title

    # 1 — Rulings override everything. They are decisions, not shortcuts.
    if a ruling phrase occurs in the title:
        return its verdict

    # 2 — An unknown head is our backlog, not the corpus's ambiguity.
    if head is not on the head list:
        return UNKNOWN, reason = unruled

    # 3 — Some functions are never engineering, whatever they are attached to.
    if head is never-engineering:
        return OUT

    # 4 — Under a domain-bound head the modifier decides, and an off-domain
    #     marker beats a software qualifier: "mechanical software engineer".
    if head is domain-bound and an off-domain marker is present:
        return OUT

    # 5 — A software domain under a head that can carry engineering work.
    if a software qualifier is present and head is engineering-capable:
        return IN

    # 6 — The head is known and nothing settled the rest.
    if head is engineering-capable:
        return UNKNOWN, reason = domain_ambiguity
    return UNKNOWN, reason = unruled
```

Matching is case-insensitive and on whole words. Every list entry is spelled out in full for
that reason: the previous version carried stems such as `pharmac` and `agricultur`, which only
work under prefix matching and silently stop working the moment the rule is stated precisely.

## How a ruling is decided

Steps 2 through 6 are mechanical. This section is not: it is the reasoning a person does when
adding a phrase to the rulings table, and it is never executed at classification time. That is
deliberate — the previous version of this document put a judgement of exactly this kind inside
the procedure, where a blind labeller could not run it, and it caused six of the eleven
disagreements in the first calibration.

A phrase is IN if any one of these holds:

- **Q2** — the role writes code as its primary artifact.
- **Q3** — the role reads or operates on engineer-facing artifacts — source, API definitions,
  schemas, logs, specs — as opposed to authoring surfaces built for non-engineers.
- **Q4** — a software background alone, with no new credential and no domain retraining, would
  make you a credible candidate today.

Q4 is what makes this project's sense of "engineering role" wider than the phrase usually
carries: the product exists to find work a person can actually take, so a role a software
background alone opens is in scope even when the role does not write software. Scrum Master is IN
for this reason and for no other.

A phrase is UNKNOWN with `scope_ambiguity` when its variants split across Q2–Q4 in the world —
some `data analyst` roles are engineering roles and some are not, and no rule fixes that. It is
OUT when none of Q2–Q4 holds and the title says so.

## The four lists

These are the classifier's mechanism and the labeller's tie-break, and they are what each round
of #10 grows. The **head list** is the one that grows first: it is where coverage comes from.
Shares are of the 179,098 cleaned titles in the raw corpus as of 2026-09-20, counting titles that
contain the word.

### Function heads

| Head | Domain | Engineering | n | share |
| --- | --- | --- | ---: | ---: |
| engineer | bound | yes | 28,640 | 16.0% |
| developer | bound | yes | 1,963 | 1.1% |
| administrator | bound | yes | 755 | 0.4% |
| programmer | bound | yes | 120 | 0.1% |
| analyst | free | yes | 5,045 | 2.8% |
| architect | free | yes | 1,857 | 1.0% |
| scientist | free | yes | 1,790 | 1.0% |
| manager | bound | no | 28,274 | 15.8% |
| technician | bound | no | 7,279 | 4.1% |
| designer | bound | no | 1,727 | 1.0% |
| writer | bound | no | 209 | 0.1% |
| master | bound | no | 150 | 0.1% |
| consultant | free | no | 3,316 | 1.9% |

**Never-engineering heads** — OUT whatever modifies them: `trainer` (3.7%), `assistant` (3.1%),
`executive` (3.1%), `nurse` (2.1%), `representative` (1.6%), `coordinator` (1.5%), `teacher`
(0.4%), `driver` (0.3%).

`specialist` is deliberately absent at 4.1%, the largest head not yet on the list. `IT
Specialist` and `Sales Specialist` are both common and the head decides neither, so it stays
`unruled` until a round says what it is rather than being guessed at now.

These twenty-two heads match **62.6%** of the corpus. By trailing word, 77 heads cover half of
it, which is the shape of the work the loop has left: the head vocabulary has a long tail but a
small usable head, and enumerating it is tractable in a way that enumerating phrases is not —
104,891 distinct cleaned titles exist, and 86,982 of them are needed to cover 90% of the corpus.

### Software qualifiers

Presence of one, under an engineering-capable head, decides IN:

`software`, `backend`, `back end`, `frontend`, `front end`, `fullstack`, `full stack`, `data`,
`platform`, `devops`, `sre`, `site reliability`, `security`, `mobile`, `ios`, `android`, `cloud`,
`infrastructure`, `qa`, `quality assurance`, `test`, `automation`, `machine learning`, `deep
learning`, `ml`, `ai`, `systems`, `network`, `web`, `api`, `embedded`, `application`,
`integration`, `solution`, `solutions`, `database`, `firmware`, `compiler`, `robotics`.

The head is what keeps this list honest. `security` is a software qualifier, and `security
analyst` is IN while `security guard` is `unruled` — `guard` is not a head that carries
engineering work. Under the previous bag-of-tokens reading there was nothing to stop the second.

### Off-domain markers

A function that exists identically outside software. Under a **domain-bound** head, one of these
decides OUT, and it beats a software qualifier. Under a domain-free head they do nothing at all,
which is why `audit analyst` and `payroll analyst` are not OUT:

`civil`, `structural`, `mechanical`, `chemical`, `hvac`, `plumbing`, `electrician`, `nurse`,
`nursing`, `clinical`, `patient`, `pharmacy`, `pharmaceutical`, `restaurant`, `retail`, `store`, `cashier`,
`driver`, `warehouse`, `forklift`, `construction`, `teacher`, `tutor`, `attorney`, `legal`,
`paralegal`, `accounting`, `payroll`, `audit`, `tax`, `janitor`, `maintenance`, `facilities`,
`manufacturing`, `welder`, `machinist`, `automotive`, `aerospace`, `petroleum`, `mining`,
`agriculture`, `agricultural`, `graphic`, `marketing`, `biomedical`.

`graphic`, `marketing` and `biomedical` were added after the first calibration, which labelled
`graphic designer`, `marketing manager` and `biomedical engineer` OUT where no marker existed to
say so.

`sales` is **not** a marker. Sales Engineer is ruled UNKNOWN below, because in some companies it
names the role others call Solutions Engineer, and a marker under the domain-bound head
`engineer` would decide OUT for all of them.

### Rulings

The answers the procedure must reproduce, and the only thing that overrides it. A ruling changes
only by a decision, never as a side effect of a list edit. When a round of labelling turns up a
phrase this table does not cover and the labeller had to guess, the phrase is added here with its
verdict before the next round runs.

| Phrase | n | Verdict | Why |
| --- | ---: | --- | --- |
| product manager | 2370 | OUT | authors surfaces for non-engineers |
| engineering manager | 974 | UNKNOWN | IN in software, and the title does not say |
| data scientist | 834 | IN | Q2 |
| mechanical engineer | 735 | OUT | off-domain marker |
| electrical engineer | 647 | UNKNOWN | may be mainly software; not a marker |
| solutions architect | 597 | IN | Q3, Q4 |
| technical program manager | 485 | IN | Q3, Q4 |
| sales engineer | 479 | UNKNOWN | sometimes names the solutions-engineer role |
| data analyst | 471 | UNKNOWN | depends how software-oriented the role is |
| business analyst | 417 | UNKNOWN | same |
| support engineer | 412 | UNKNOWN | most are IN, with exceptions |
| civil engineer | 409 | OUT | off-domain marker; too few exceptions to park |
| network engineer | 238 | IN | Q3 |
| field engineer | 160 | UNKNOWN | some are forward-deployed engineers |
| research scientist | 152 | UNKNOWN | some AI labs use the term |
| systems administrator | 141 | IN | Q3 |
| database administrator | 126 | IN | Q3 |
| technical writer | 68 | UNKNOWN | some write software documentation |
| scrum master | 37 | IN | Q4 alone |
| ux engineer | 35 | IN | Q2 |
| implementation engineer | 34 | IN | Q3, Q4 |
| game designer | 31 | OUT | the code it operates is not engineer-facing |
| game programmer | — | IN | Q2 |
| solution engineer | 108 | IN | Q3, Q4 |
| customer engineer | 98 | IN | Q3, Q4 |
| manufacturing systems engineer | — | UNKNOWN | a manufacturing system is itself software |

`manufacturing systems engineer` is the one ruling that exists to hold a rule together rather
than to record a judgement about a phrase. Step 4 sends it to OUT, and the calibration labelled
it UNKNOWN while labelling the identically shaped `mechanical software engineer` OUT. Rather than
bend step 4 around a single item, the item is carried here.
