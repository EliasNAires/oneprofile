# Seed lists

**Temporary.** These are the four lists as they stood on 2026-09-21, before any of them were
code. The first iteration of the loop ports them into the classifier and **deletes this file**.
Nothing should ever read it afterwards: once the classifier exists, the code is the only place
a list lives, and a second copy here is a second answer to the same question.

The criterion these serve is `docs/engineering-role-criterion.md`.

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
