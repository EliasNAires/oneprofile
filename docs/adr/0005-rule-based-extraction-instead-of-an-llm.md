# Extraction is rule-based; no LLM is used

Status: accepted

Every derived fact — normalized title, work mode, seniority level, role family, eligibility
and skills — is produced by pure functions over text: whitelist cleaning, small hand-built
dictionaries, and a trie pass for skills. No language model is called anywhere in the
pipeline.

## Considered options

- **An LLM pass over the descriptions the rules cannot decide**, which was planned and then
  dropped. It was costed at roughly $31–$156 for the corpus depending on model, via the
  Batch API — affordable, but it adds a paid, networked, non-deterministic dependency to the
  feature the product hangs on, and results would need versioning by prompt and model to stay
  interpretable.

## Consequences

Rules are re-runnable for free, which matters: the pre-reset normalizer processed 128,953
vacancies in 24 seconds, so a rule change is a 24-second experiment rather than a billed
batch job. They are also inspectable when wrong, and their error rate is measurable against a
labelled sample. The measured precedent is good — work-mode extraction achieved a 0.19% false
positive rate (37 of 19,255) from a 16-phrase dictionary.

The cost is recall on eligibility, where the vocabulary is genuinely open-ended rather than
closed, and this compounds with the hard filter in ADR-0004. An LLM pass remains the obvious
upgrade, and should be adopted on measured evidence that the rules have plateaued below a
useful recall — not on the assumption that it would do better.
