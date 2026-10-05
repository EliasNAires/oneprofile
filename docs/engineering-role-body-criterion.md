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
describes work that could be software or not without saying which.

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
  schemas, logs, specs — as opposed to content, reports or dashboards made in tools built for
  non-engineers.
- **Q4** — the description requires or prefers a software background, and the role asks for no
  expertise outside software that such an applicant would lack.

If none holds, it is OUT.

Q4 is what makes this project's sense of "engineering role" wider than the phrase usually
carries: the product exists to find work a person can actually take, so a role a software
background opens is in scope even when the role does not write software. A Scrum Master role
that asks for a technical background is IN for this reason.

**Low-code is software work.** A role whose main work is building applications, automations or
integrations is a software role whether it builds them in code or in a low-code builder: apps
and flows in Power Apps or Salesforce, an ERP or quality system implemented and configured, a
marketing and sales tool stack wired together and automated. A role that only reports on data
in such a tool — reports, dashboards — or administers it day to day — users, permissions, page
layouts, content — is not. When a role does some of both, its main work decides.

**What counts as a stated software background.** Q4 needs the description to say it, as a
requirement or a preference; a background that would merely help with the duties is not enough.
It counts when the description asks for any of:

- a degree in computer science or software engineering, or equivalent technical experience;
- a programming language, by name;
- SQL;
- cloud, container, infrastructure or networking skills;
- technical experience in a field, when that field is software — logs analytics, observability,
  developer tools.

A background listed only as a plus or a nice-to-have does not count.

**The expertise limit on Q4.** Experience in the job itself is never expertise outside software.
Years as a product manager, an analyst, a pre-sales or sales engineer, a project or programme
manager, an account or customer success manager do not make a role OUT: a product manager role
that asks for five years of product management and a computer-science degree is IN. This holds
when the experience names the field it was done in: years of marketing analytics, growth
analytics, fintech product management or healthcare project management are years in the job, and
the field they name is where the job was done, not expertise the role requires on its own.

Knowledge of a field the role is about is expertise outside software when the description
requires it: finance, marketing, market research, insurance, medicine, law, an engineering degree
in another discipline, a clinical or legal licence, an actuarial qualification. A software
applicant would lack it, so the role is OUT, even when it also asks for a software background.
The same holds against Q2: a role that writes code but requires an electrical-engineering degree,
a clinical licence or an actuarial qualification is OUT. It is the requirement that makes it OUT,
not the field: verification code written in SystemVerilog, Python or C++ for a role that asks for
no such degree is Q2. Field knowledge the description lists as a plus, or expects to be learned
on the job, is not such a requirement.

So the line between the two is what the description asks the applicant to have. Experience
doing this kind of role, wherever it was done, is the job itself. Knowledge of the field asked
for in its own right — a degree, licence or qualification in it, or expertise in the subject
named as a requirement apart from the role's experience, such as "a payments domain expert" or
"deep knowledge of insurance products" — is expertise outside software.

**Selling is decided by the duties.** A role whose main work is selling — carrying a quota,
owning revenue, closing deals — is OUT, whatever background it asks for. A role that supports a
sale without carrying its quota — demos, proofs of concept, architecture, technical objections —
is decided by Q4 like any other.

## Reading the title's doubt

Each vacancy comes with the reason its title was left undecided. The reason says which question
the description has to answer first.

### `domain_ambiguity` — which engineering is this?

The title means different work in different domains — `Engineering Manager` could run a software
team or a plant — and the description has to say which.

- The description names software work — a stack, a codebase, APIs, cloud infrastructure,
  software products or teams — and the domain is software. Go on to the questions as for any
  vacancy: a software domain does not make every role IN, and an `Event Solutions Engineer`
  shown to be in software may still be a selling role, decided as under `scope_ambiguity`.
- The description names another domain — highways, a plant, body metals, HVAC, construction,
  process equipment — and the vacancy is OUT.
- The description describes work that is plainly not engineering of any kind — policy,
  operations, business analysis — and the vacancy is OUT, whether or not it names a domain.
- The description describes work that could be software or not, and does not say which —
  "maintain the control systems", with nothing to show whether that is code or a plant — and the
  vacancy is UNKNOWN: the title's doubt is still open.

### `scope_ambiguity` — does this role need a software background?

The domain is clear, but the title covers roles a software background opens and roles it does
not — product managers, data and business analysts, solutions, sales and customer engineers,
technical account and programme managers. The description decides, through Q2–Q4. A
`scope_ambiguity` vacancy whose work is described is never UNKNOWN. Ask, for example:

- Are the duties technical in themselves — owning API specs, reading logs, making architecture
  calls, writing queries or code? That is Q2 or Q3.
- Does the description require or prefer a software background, in the forms listed above? That
  is the clear edge of Q4.
- Does the description require knowledge of a field in its own right — finance, marketing,
  market research, insurance — or is the role's main work selling? Then the expertise limit makes
  it OUT. Years of experience in the role, even named after a field ("five years of marketing
  analytics"), are not field knowledge.

## How a vacancy is read

- **The work decides, not the words.** Read what the person hired will do and what they must
  bring. A title that says engineer over duties that are selling, recruiting or running a plant
  is decided by the duties; so is a plain title over duties that are writing software.
- **Meaning is read in any language.**
- **This document and nothing else.** The labeller knows the title's reason, given with the
  vacancy, but not the title rules or anything else the title stage decided.
