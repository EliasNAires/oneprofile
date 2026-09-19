# Skills use a curated taxonomy, not Lightcast Open Skills

Status: accepted

The skill vocabulary is a curated set of roughly 1,500–3,000 engineering concepts with
aliases, seeded from O*NET Technology Skills (CC BY 4.0), GitHub Linguist (MIT) and Wikidata
(CC0). It is held locally and matched offline.

## Considered options

- **Lightcast Open Skills**, the original choice. Rejected on three independent grounds, any
  one sufficient. Its Open Terms of Use grant use "excluding commercial or for-profit
  purposes" without a written contract, and this may become a product. There is no bulk
  download, and **aliases are not exposed in the public schema at all** — the schema is
  `id, name, type, infoUrl, tags, isSoftware, isLanguage, description, category,
  subcategory`, and their own documentation confirms the alias table is internal to their
  extraction model. Aliases are the single property matching depends on. Access is a sales
  process with a documented ceiling of 5 requests per second, which makes matching a
  200,000-document corpus through their `extract` endpoint infeasible.
- **ESCO**, which has the best licence of any standard and first-class alternative labels.
  Rejected on domain coverage, measured against its live API: `Kubernetes`, `Docker` and
  `Terraform` each return **zero results**. It stops at the language and paradigm level.
- **Stack Overflow tags and tag synonyms**, the best tech vocabulary and alias table in
  existence. Rejected for now because CC BY-SA share-alike on a taxonomy embedded in a
  possibly-commercial product is an unresolved question, and the dumps moved behind
  authentication in 2024.

## Consequences

The taxonomy is ours to curate and grow, which is ongoing work rather than a one-time
import. In exchange it is offline, deterministic, testable, legally unencumbered, and small
enough to double as the list a person picks their own skills from — which a 34,000-entry
general taxonomy could not.

Note that SkillNER and similar projects bundle Lightcast-derived data under an MIT licence
on the *code*. The code licence does not cover the data, and they are not a way around this
decision.
