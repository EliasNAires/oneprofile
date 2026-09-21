# What counts as an engineering role

The prose criterion. It is the one document a session reads before labelling, and it is the
only thing the classifier's rules answer to. When the rules and this file disagree, this file
is right and the rules are wrong.

It holds no word lists. The lists are code — `src/main/java`, grown by whichever session is
running the loop — because a list is mechanism and this is judgement. `docs/engineering-role-seed-lists.md`
holds the lists that exist today, until the first implementation absorbs them.

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
the domain it does it in. Where a title names more than one head, the first is the head.

Each head carries two attributes:

- **Domain-bound or domain-free.** `engineer` and `technician` name a function embedded in a
  domain, so the modifier settles what the role is: a `civil engineer` is not a software job.
  `analyst` and `architect` name a function that operates on information *about* a domain, so
  the same modifier settles nothing: an `audit analyst` may well work on audit software.
- **Engineering-capable or not.** Whether a software domain under this head can produce an
  engineering role at all. `data scientist` is one; `data manager` is not obviously one.

A third class sits outside both: **never-engineering heads**, which decide OUT whatever
modifies them.

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
that reason: an earlier version carried stems such as `pharmac` and `agricultur`, which only
work under prefix matching and silently stop working the moment the rule is stated precisely.

## How a ruling is decided

Steps 2 through 6 are mechanical. This section is not: it is the reasoning a person does when
deciding a phrase, and it is never executed at classification time. An earlier version of this
document put a judgement of exactly this kind inside the procedure, where a labeller could not
run it, and it caused six of the eleven disagreements in the first hand check.

**Q1 — it has to be a vacancy.** This one is a gate, not an alternative: it has to hold before
Q2–Q4 are worth asking. A post that names no role being hired for is OUT however engineering it
sounds — talent-community sign-ups, general applications, "join our team" banners, event and
referral posts. They are marketing surfaces that happen to sit in a job feed, and Q2–Q4 would
otherwise read them as engineering because the words around them are.

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

A phrase is UNKNOWN with `scope_ambiguity` when its variants split across Q2–Q4 in the world —
some `data analyst` roles are engineering roles and some are not, and no rule fixes that. It is
OUT when Q1 fails, and when none of Q2–Q4 holds and the title says so.

## What a rule may cost

A rule the loop invents is unconstrained in shape and constrained in cost. It matches whole
words, case-insensitively, against the cleaned title and nothing else: no description bodies,
no network, no backtracking regexes, and a full pass over the corpus in under ten seconds.
The limit exists to forbid one specific escape — resolving a hard title by reading its
description, which is #11's job and would put a language model on the path every vacancy
travels.
