# Skills are extracted before the body pass, and descriptions are not lemmatized

Status: accepted; supersedes ADR-0012's lemmas (§ The skill taxonomy as a signal: descriptions
lemmatized against the taxonomy by #47, and the loop resuming with its rules moved onto lemmas)

ADR-0012 lets the body rules read the taxonomy, and planned for them to read descriptions
lemmatized against it (#47). #47 was closed without lemmatizing. Word forms explained 2 rows of
round 4's misses (item 12), and the rules already handle them with stems and alternations, where
the context is known. A general dictionary turns data into datum, cannot tell the noun building
from the verb, and adds a layer to account for when explaining a rule's miss. What the rules
needed from the taxonomy is skills, and finding those is extraction's work (#15), not lemmas'.
Decided by Elias on 2026-10-19.

## The order

1. **Title classification.**
2. **Skill extraction**, over the vacancies the title decided in and the pile, without those set
   aside as `unsupported_language`.
3. **The body pass**, which reads extraction's skills as evidence, in place of its own list of
   technologies.
4. **The other extractions** (normalization, seniority from the body, eligibility), over the
   engineering subset alone, which is cheaper once the body pass has decided the pile.

Skill extraction moves ahead of the body pass because a description asking for named software
skills is the plainest domain marker the body carries (ADR-0012), and round 4's misses include
skills the rules' own short list does not know (items 6, 8 and 13). Skill names are the same in
any language, so extraction also reaches requirements written in words the rules do not know.

## What follows

- **#11 waits for #15.** After round 4 the loop stays paused until skill extraction lands, and
  resumes with its rules reading skills. As ADR-0012 says, a pause is not a round, and each
  round's Record names the commit of `skills.tsv` it ran against.
- **Word forms stay in the rules.** A shared table of word families is added only when the same
  family is needed by a second rule.
- **Jev's input does not move.** Jev keeps reading the segmented description, so a change in
  extraction or the taxonomy is not a new baseline (ADR-0012).
- How extraction stores what it finds, and how context keys are matched, are #15's to decide.
