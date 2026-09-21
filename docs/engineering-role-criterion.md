# What counts as an engineering role

The criterion the classifier approximates and the labeller applies. It is one document
because it has two readers: the **labeller** runs the procedure below verbatim to produce
ground-truth labels, and the **classifier** (#10) is a token dictionary that tries to
reproduce those labels cheaply. When the two disagree, this file is right and the
dictionary is wrong.

The ruling table at the end is not illustration. It is the set of answers the procedure is
required to produce, and it is what the calibration set of #9 checks the procedure against.

## The three states

**IN** — the vacancy is an engineering role.
**OUT** — it is not, and the title says so.
**UNKNOWN** — the title does not carry enough to decide, with a reason recorded:
`domain_ambiguity` (the function word exists identically outside software) or
`scope_ambiguity` (variants of this title split across the membership test).

UNKNOWN is not a softer OUT. It is a queue: #11 resolves it from the description body by
rule. A vacancy parked in UNKNOWN is a vacancy the title stage declined to guess about, and
the share of the corpus sitting there is capped, because a classifier that declines
everything would otherwise score perfectly.

## The procedure

```
classify(cleaned_title):

    software  = the title carries a software qualifier
    ambiguous = the title carries an ambiguous function word
    offdomain = the title names a non-software domain

    # Q0 — domain ambiguity. A software qualifier settles the domain, so it is
    # checked first: "Embedded Software Engineer" is not ambiguous, "Engineer II" is.
    if ambiguous and not software:
        if offdomain:
            return OUT
        return UNKNOWN, reason = domain_ambiguity

    # Q1 — scope ambiguity. The domain is software, but titles of this shape
    # split: some are engineering roles and some are not.
    if the title's variants split across the membership test below:
        return UNKNOWN, reason = scope_ambiguity

    # Membership — a disjunction. Any one of these is enough.
    Q2: does this role write code as its primary artifact?
    Q3: does this role read or operate on engineer-facing artifacts — source,
        API definitions, schemas, logs, specs — as opposed to authoring
        surfaces built for non-engineers?
    Q4: would a software background alone, with no new credential and no domain
        retraining, make you a credible candidate today?

    if Q2 or Q3 or Q4:
        return IN
    return OUT
```

Q4 is deliberate and it is what makes this project's sense of "engineering role" wider than
the phrase usually carries: the product exists to find work a person can actually take, so a
role a software background alone opens is in scope even when the role does not write
software. Scrum Master is IN for this reason and for no other.

The procedure is the labeller's. The classifier cannot execute Q2, Q3 or Q4 — it is a
dictionary — so it approximates them with a list of tokens that mean IN and a default of OUT
for everything it does not recognise. Closing the gap between the two is the whole of the
loop in #10 and #9.

## The three lists

These are the classifier's mechanism and the labeller's tie-break, and they are what each
round of the loop grows. Shares are of the 179,098 cleaned titles in the raw corpus as of
2026-09-20.

**Ambiguous function words** — a function that exists identically outside software:
`engineer`, `engineering`, `analyst`, `architect`, `technician`.

`manager`, `designer` and `consultant` were considered and left out. They are ambiguous in
the abstract but not in the corpus: `manager` alone appears in 23,976 titles (13.4%), almost
all of them plainly outside software, and admitting it would park more vacancies in UNKNOWN
than the 10% cap allows for the entire corpus. They fall through to the membership test and
default to OUT.

**Software qualifiers** — presence of one settles the domain and suppresses Q0:
`software`, `backend`, `back end`, `frontend`, `front end`, `fullstack`, `full stack`,
`data`, `platform`, `devops`, `sre`, `site reliability`, `security`, `mobile`, `ios`,
`android`, `cloud`, `infrastructure`, `qa`, `quality assurance`, `test`, `automation`,
`machine learning`, `deep learning`, `ml`, `ai`, `systems`, `network`, `web`, `api`,
`embedded`, `application`, `integration`, `solution`, `solutions`, `database`, `firmware`,
`compiler`, `robotics`.

**Non-software domain markers** — with no software qualifier present, these decide OUT
rather than UNKNOWN: `civil`, `structural`, `mechanical`, `chemical`, `hvac`, `plumbing`,
`electrician`, `nurse`, `nursing`, `clinical`, `patient`, `pharmac`, `restaurant`, `retail`,
`store`, `cashier`, `driver`, `warehouse`, `forklift`, `construction`, `teacher`, `tutor`,
`attorney`, `legal`, `paralegal`, `accounting`, `payroll`, `audit`, `tax`, `janitor`,
`maintenance`, `facilities`, `manufacturing`, `welder`, `machinist`, `automotive`,
`aerospace`, `petroleum`, `mining`, `agricultur`.

`sales` and `marketing` are **not** markers. Sales Engineer is ruled UNKNOWN below, because
in some companies it names the role others call Solutions Engineer, and a marker would
decide OUT for all of them.

Measured over the raw corpus, these lists produce **10.2% UNKNOWN** from domain ambiguity
(18,325 titles), with the off-domain markers converting a further 2,875 (1.6%) from UNKNOWN
to OUT. Scope ambiguity adds to that figure, so the first round starts at or above the 10%
cap, and growing the qualifier list is the main lever for bringing it down.

## Rulings

The answers the procedure must reproduce. Counts are cleaned titles containing the phrase.

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

A ruling changes only by a decision, never as a side effect of a dictionary edit. When a
round of labelling turns up a phrase this table does not cover and the labeller had to guess,
the phrase is added here with its verdict before the next round runs.
