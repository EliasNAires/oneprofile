# What counts as an engineering role

The prose criterion. It is the one document a session reads before labelling, and it is the
only thing the classifier's rules answer to. When the rules and this file disagree, this file
is right and the rules are wrong.

It holds no word lists. The lists are code — `src/main/java`, grown by whichever session is
running the loop — because a list is mechanism and this is judgement.

## The three states

**IN** — the vacancy is an engineering role.
**OUT** — it is not, and the title says so.
**UNKNOWN** — the title does not carry enough to decide.

UNKNOWN is the default. IN and OUT are claims a rule has to earn, and a title no rule reaches
is a title that has not been read.

Every UNKNOWN carries a reason, because the reason is what tells the next reader why the title
landed there:

| Reason | Means | Whose problem |
| --- | --- | --- |
| `unruled` | no rule reaches this title | **ours** — a line added to a list fixes it |
| `domain_ambiguity` | the head is known, the domain is not | the corpus's |
| `scope_ambiguity` | a ruled phrase whose variants genuinely split | the corpus's |

Only `unruled` is expected to fall. The other two are what the world is.

## How a title is read

A title is a **function head** — the noun naming what the role does — with **modifiers** naming
the domain it does it in.

**Which word is the head.** The first head the title names, except where that word **yields**.
A yielding head names a rank or a department rather than the work — `lead`, `director`,
`manager`, `chief`, `intern`, `support`, `operations`, `specialist` — so where another head
stands behind it, that one is the head: `Lead Analytics Engineer` is an engineer, and so is
`Network Support Engineer`. A never-engineering word behind a yielding word does not take the
head, because there it names who the work is done for rather than what it does: in `Manager
Rider Operations` the head stays `manager`. A yielding word with no head behind it is the head
itself.

Each head carries two attributes:

- **Domain-bound or domain-free.** `engineer` and `technician` name a function embedded in a
  domain, so the modifier settles what the role is: a `civil engineer` is not a software job.
  `analyst` and `architect` name a function that operates on information *about* a domain, so
  the same modifier settles nothing: an `audit analyst` may well work on audit software.
- **Engineering-capable or not.** Whether a software domain under this head can produce an
  engineering role at all. `data scientist` is one; `data manager` is not obviously one.

A third class sits outside both: **never-engineering heads**, which decide OUT whatever
modifies them.

**Generic heads** are domain-bound heads that name no work of their own — `manager`,
`director`, `consultant`, `advisor`, `operations`, `support`. Most are yielding heads, and
they matter where nothing stands behind them. With any
modifier and no software qualifier, a generic head is OUT: `Hotel Manager` and `Director of
Operations` say what the role is for, and nothing about software. This is the rule that an
unclassed modifier is a market marker, applied where it cannot cause a miss. `engineer` and
`developer` are deliberately not generic, because a bare `C Engineer` is a software job and
the same reading would decide it OUT. One word holds a generic head open: **`product`**. A
product title with no software qualifier is `domain_ambiguity`, because product roles split
across the criterion more than any other family.

**A head's attributes are evidence-revisable.** They are an argument about language, and the
corpus is allowed to win: a session may reclass a head against the argument made here when its
samples say so, recording the sample count that moved it. `consultant` is domain-bound for this
reason — the consultants this corpus posts are leasing agents, beauty counters and recruiters,
though the audit-analyst argument would make it domain-free. Reclassing a head is a change like
any other: it is priced on the accumulated labels and it obeys the ban on new `OUT`-stratum
false accepts.

## Two kinds of modifier

An off-domain modifier is not one thing. The distinction decides what happens when a title
carries an off-domain modifier *and* a software qualifier at once, which the corpus does
constantly.

- A **discipline marker** names a body of training a person is hired on — `mechanical`,
  `civil`, `chemical`, `aerospace`, `fpga`, and equally `nurse`, `attorney`, `chef`,
  `veterinarian`. It is about the candidate, not the customer.
- A **market marker** names who the work is done *for* — `finance`, `retail`, `marketing`,
  `higher education`, `logistics`, `procurement`. Software is built for every one of these
  markets, so the marker says nothing about whether this role builds it.

The test is the credential: would a person need that training to be hired? If yes, it is a
discipline marker; if no, it is a market marker. **A modifier no one has classed is a market
marker**, because market is the class that cannot cause a miss.

The two behave differently in exactly one place, and identically everywhere else:

- A **discipline marker decides OUT under any head**, domain-bound or domain-free, and it
  beats a software qualifier. `Mechanical Software Engineer` is OUT, and so is `Nurse
  Analyst`. This is what settles the hardware-adjacent code roles — `FPGA Engineer`,
  `ASIC Verification Engineer`, `GNC Engineer` — where the artifact is code but the credential
  is not software: the discipline named in the title decides, and Q4 is why.
- A **market marker decides OUT only under a domain-bound head with no software qualifier**.
  `Marketing Manager` is OUT; `Marketing Web Developer` is not, because `web` is present and
  `marketing` names the customer. Under a domain-free head a market marker settles nothing at
  all: `Treasury Analyst` and `Audit Analyst` are UNKNOWN, by the same argument that makes
  the head domain-free in the first place.

## The procedure

```
classify(cleaned_title):

    # 0 — Q1 first, before anything is read as a head. A post that hires
    #     nobody is OUT whether or not it names a role.
    if the post is not a vacancy:
        return OUT

    # 1 — Rulings override everything. They are decisions, not shortcuts.
    if a ruling phrase occurs in the title:
        return its verdict

    # 2 — An unknown head is our backlog, not the corpus's ambiguity.
    if head is not on the head list:
        return UNKNOWN, reason = unruled

    # 3 — Some functions are never engineering, whatever they are attached to.
    if head is never-engineering:
        return OUT

    # 4 — The credential beats everything the title says about the market.
    if a discipline marker is present:
        return OUT

    # 5 — A software domain under a head that can carry engineering work.
    if a software qualifier is present and head is engineering-capable:
        return IN

    # 6a — A generic head with a modifier and nothing software about it.
    if head is generic and the title has a modifier and `product` is absent:
        return OUT

    # 6 — With no qualifier to argue against, the market decides a bound head.
    if head is domain-bound and a market marker is present:
        return OUT

    # 7 — The head is known and nothing settled the rest.
    if head is engineering-capable:
        return UNKNOWN, reason = domain_ambiguity
    return UNKNOWN, reason = unruled
```

Matching is case-insensitive and on whole words, so every list entry is spelled out in full:
a stem such as `pharmac` matches nothing.

## How a ruling is decided

Steps 0 through 7 are mechanical. This section is not: it is the reasoning a person does when
deciding a phrase, and it is never executed at classification time. A judgement a blind reader
cannot execute does not belong in the procedure.

**Q1 — it has to be a vacancy.** This one is a gate, not an alternative: it has to hold before
Q2–Q4 are worth asking, and it is read before the title is read as a head at all. A post that
names no role being hired for is OUT however engineering it sounds — talent-community sign-ups,
general applications, "join our team" banners, event and referral posts. They are marketing
surfaces that happen to sit in a job feed, and Q2–Q4 would otherwise read them as engineering
because the words around them are.

Given a post that is a vacancy, it is IN if any one of these holds:

- **Q2** — the role writes code as its primary artifact.
- **Q3** — the role reads or operates on engineer-facing artifacts — source, API definitions,
  schemas, logs, specs — as opposed to authoring surfaces built for non-engineers.
- **Q4** — a software background alone, with no new credential and no domain retraining, would
  make you a credible candidate today.

Q4 is what makes this project's sense of "engineering role" wider than the phrase usually
carries: the product exists to find work a person can actually take, so a role a software
background alone opens is in scope even when the role does not write software. Scrum Master is
IN for this reason and for no other.

Q2 and Q4 conflict on one family, and the conflict is not resolved by ranking the questions.
FPGA and ASIC verification write SystemVerilog, which is Q2, and hire on a hardware credential,
which fails Q4. The discipline marker is what decides them: the title names the training, step 4
reads it, and the answer is OUT. Where a code role's non-software credential is *not* named in
the title, nothing decides it and it stays `domain_ambiguity` — which is honest, because the
title genuinely does not say.

A phrase is UNKNOWN with `scope_ambiguity` when its variants split across Q2–Q4 in the world —
some `data analyst` roles are engineering roles and some are not, and no rule fixes that. It is
OUT when Q1 fails, and when none of Q2–Q4 holds and the title says so.

## How a title is labelled

A labeller applies this document and nothing else, over bare cleaned titles, before it has read
a rule. Three conventions it does not get to invent:

- **Q1 is read first**, as the procedure says. `Submit your resume here` and `Talent Community`
  are OUT, not UNKNOWN — they are not vacancies, and a title with no head at all is decided
  here rather than filed under a reason that does not fit it.
- **Meaning is read in any language.** `Técnico em segurança do trabalho` is a safety
  technician and is labelled as one. That the classifier's lists are mostly English is the
  rules' backlog, not the label's: a labeller calling every non-English title `unruled` scores
  the rules wrong for reading a language it refused to. A labeller that genuinely cannot read a
  script says so on that row.
- **`unruled` is available to the labeller.** It is the honest answer for a title the labeller
  cannot reach at all, and forbidding it only pushes those rows into `domain_ambiguity`, where
  they inflate the reason this document says belongs to the corpus.

A labeller does not override this document on a family it finds unsatisfying. `Treasury
Analyst` is UNKNOWN because `analyst` is domain-free and `treasury` is a market marker; calling
it OUT on judgement is a disagreement with the criterion, not a label.

## What a rule may cost

A rule the loop invents is unconstrained in shape and constrained in cost. It matches whole
words, case-insensitively, against the cleaned title and nothing else: no description bodies,
no network, no backtracking regexes, and a full pass over the corpus in under ten seconds.
The limit exists to forbid one specific escape — resolving a hard title by reading its
description, which is #11's job and would put a language model on the path every vacancy
travels.
