# What counts as an engineering role, read from a description

The criterion of the body pass (#11). It is the one document a labeller reads before labelling a
vacancy from its title and cleaned description, and the only thing the body rules answer to.
When the rules and this file disagree, this file is right and the rules are wrong.

The title stage has its own criterion, `docs/engineering-role-criterion.md`, built for a few
words with no context: heads, modifiers, rulings. None of it applies here. A description says
what the job is, so this document asks the questions directly.

## The three states

**IN** — the vacancy is an engineering role.
**OUT** — it is not.
**UNKNOWN** — the text does not say what the work is.

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
- **Q4** — a software background alone, with no new credential and no domain retraining, would
  make you a credible candidate today.

If none holds, it is OUT.

Q4 is what makes this project's sense of "engineering role" wider than the phrase usually
carries: the product exists to find work a person can actually take, so a role a software
background alone opens is in scope even when the role does not write software. Scrum Master is
IN for this reason and for no other.

**When Q2 and Q4 conflict**, the requirements decide. A role that writes code but requires a
credential or training outside software — an electrical-engineering degree for FPGA
verification, a clinical licence, an actuarial qualification — is OUT, because a software
background alone does not open it. Domain knowledge the description lists as a plus, or expects
to be learned on the job, is not such a requirement.

## How a vacancy is read

- **The work decides, not the words.** Read what the person hired will do and what they must
  bring. A title that says engineer over duties that are selling, recruiting or running a plant
  is decided by the duties; so is a plain title over duties that are writing software.
- **Meaning is read in any language.**
- **This document and nothing else.** The labeller does not know the title rules or the title
  stage's verdict, and does not need them.
