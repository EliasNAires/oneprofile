# What counts as an engineering role, read from a description

The criterion of the body pass (#11). It is the one document a labeller reads before labelling a
vacancy from its title, the reason its title was left undecided, and its cleaned description,
and the only thing the body rules answer to. When the rules and this file disagree, this file is
right and the rules are wrong.

The title stage has its own criterion, `docs/engineering-role-criterion.md`, built for a few
words with no context. Every vacancy that reaches the body pass is one the title could not
decide, so the title's words have already said all they can. The body pass exists to settle that
doubt from the description, which carries what the title cannot: what the person hired will do,
in what domain, and what they must bring. Read the description for that, in the spirit of the
questions below, not for word combinations in the title.

"Engineering" in this document always means software engineering. Civil, mechanical,
electrical, chemical, manufacturing and every other engineering discipline are not engineering
roles in this sense.

## The three states

**IN** — the vacancy is an engineering role.
**OUT** — it is not.
**UNKNOWN** — the text does not say what the work is, or, for a title whose domain was the doubt,
does not say which domain the work is in.

UNKNOWN means missing information, never a hard call. If the title and description say what the
person hired will do, the questions below decide IN or OUT, however close the call. A labeller
that files a close call under UNKNOWN is not being careful; it is declining to apply the
criterion.

The text does not say what the work is when the description is empty, cut off before it reaches
the duties, or made only of what surrounds a job: the company, its mission, benefits, location,
the application process, legal notices. A title alone, under such a description, is UNKNOWN even
when it sounds like engineering: the title stage already read it and could not decide.

A body label carries no reason. The pile's rows keep the reason the title stage gave them.

## The questions

**Q1 — it has to be a vacancy.** This one is a gate, read first: a post that hires nobody is OUT
however engineering it sounds — talent-community sign-ups, general applications, "join our
team" banners, event and referral posts.

Given a vacancy, it is IN if any one of these holds:

- **Q2** — the role writes code as its primary artifact.
- **Q3** — the role reads or operates on engineer-facing artifacts — source, API definitions,
  schemas, logs, specs — as opposed to authoring surfaces built for non-engineers.
- **Q4** — a software background gives an applicant a clear edge for the role, and the role
  asks for no expertise outside software that such an applicant would lack.

If none holds, it is OUT.

Q4 is what makes this project's sense of "engineering role" wider than the phrase usually
carries: the product exists to find work a person can actually take, so a role a software
background opens is in scope even when the role does not write software. Scrum Master is IN for
this reason and for no other.

**The expertise limit on Q4.** A clear edge is not enough when the role also asks for expertise
outside software: a sales track record, finance or market-research experience, a clinical or
legal licence, an engineering degree in another discipline. A software applicant would lack it,
so the role is OUT. The same holds against Q2: a role that writes code but requires an
electrical-engineering degree for FPGA verification, a clinical licence or an actuarial
qualification is OUT. Domain knowledge the description lists as a plus, or expects to be learned
on the job, is not such a requirement.

## Reading the title's doubt

Each vacancy comes with the reason its title was left undecided. The reason says which question
the description has to answer first.

### `domain_ambiguity` — which engineering is this?

The title means different work in different domains — `Engineering Manager` could run a software
team or a plant — and the description has to say which.

- The description names software work — a stack, a codebase, APIs, cloud infrastructure,
  software products or teams — and the domain is software. Go on to the questions as for any
  vacancy: a software domain does not make every role IN, and an `Event Solutions Engineer`
  shown to be in software may still be a pre-sales role, decided as under `scope_ambiguity`.
- The description names another domain — highways, a plant, body metals, HVAC, construction,
  process equipment — and the vacancy is OUT.
- The description describes the job but names no domain at all, and the vacancy is UNKNOWN:
  the title's doubt is still open.

### `scope_ambiguity` — does this role need a software background?

The domain is clear, but the title covers roles a software background opens and roles it does
not — product managers, data and business analysts, solutions, sales and customer engineers,
technical account and programme managers. The description decides, through Q2–Q4. Ask, for
example:

- Does the role require technical knowledge, as a stated requirement or because its duties are
  technical in themselves — owning API specs, reading logs, making architecture calls, writing
  queries or code?
- Does the description prefer a technical background, or would one plainly help with the duties
  it lists? That is the clear edge of Q4.
- Is the expertise the role is built on something else — a quota, market research, financial
  analysis, account management — with technical knowledge at most a nice-to-have? Then the
  expertise limit makes it OUT.

## How a vacancy is read

- **The work decides, not the words.** Read what the person hired will do and what they must
  bring. A title that says engineer over duties that are selling, recruiting or running a plant
  is decided by the duties; so is a plain title over duties that are writing software.
- **Meaning is read in any language.**
- **This document and nothing else.** The labeller knows the title's reason, given with the
  vacancy, but not the title rules or anything else the title stage decided.
