# Where the skill taxonomy comes from

The provenance and licences of `src/main/resources/taxonomy/skills.tsv`: which sources its
skills were drawn from, under what terms, and how the file was built from them. Why the
taxonomy is curated rather than taken whole from someone else's: ADR-0003.

## Sources

| Source | Version | Licence | Where |
| --- | --- | --- | --- |
| O\*NET Technology Skills | O\*NET 30.2 Database, `Technology Skills.txt` | CC BY 4.0 | https://www.onetcenter.org/dl_files/database/db_30_2_text/Technology%20Skills.txt |
| GitHub Linguist | v9.7.0, commit `e0c78d62c42abae6122235d8e68a7aa43eef89da`, `lib/linguist/languages.yml` | MIT | https://raw.githubusercontent.com/github-linguist/linguist/v9.7.0/lib/linguist/languages.yml |
| Wikidata | queried 2026-09-24 through https://query.wikidata.org/sparql | CC0 | seven class pulls and one label lookup, below |
| The corpus | the 26,587 IN descriptions of the development database, newest vacancy update 2026-09-20 | — | the names its ads use that no source above supplies |

O\*NET and Linguist are pinned by SHA-256 in `scripts/taxonomy_candidates.py`:

- O\*NET: `a6e7ea1fb368659a3a8ffe2895d8343cbe80b61efbb2db39cb3cb0f9bd56d3b4`
- Linguist: `7c2bc5b59662de6c5d09cd4990e82b2541d4cd2ea7c8c213537730474f24a5c7`

Wikidata has no releases. Its answers are kept as pulled, with the date, under
`~/oneprofile-taxonomy` (`wikidata-Q*.json` for the class pulls, `wikidata-labels.json` for the
label lookup), and every one of them was pulled on 2026-09-24. The classes pulled, each with
its subclasses except where noted, items with at least five sitelinks: programming language
(Q9143), software framework (Q271680), web framework (Q1330336), software library (Q188860),
DBMS (Q176165), GUI toolkit (Q1193246, no subclasses) and operating system (Q9135, no
subclasses). The label lookup resolves each O\*NET example to the Wikidata item it names.

From O\*NET, only the engineering rows are read: occupations 15-12\*, 15-2051 and 17-2061, and
the commodity categories listed in `ENGINEERING_COMMODITY` whichever occupation lists them.
From Linguist, every language's name and aliases.

## Licences

**O\*NET — CC BY 4.0** (https://creativecommons.org/licenses/by/4.0/). The attribution and
change notice O\*NET asks of anyone who edits its information:

> This page includes information from the O\*NET 30.2 Database by the U.S. Department of Labor,
> Employment and Training Administration (USDOL/ETA). Used under the CC BY 4.0 license.
> O\*NET® is a trademark of USDOL/ETA. OneProfile has modified all or some of this information.
> USDOL/ETA has not approved, endorsed, or tested these modifications.

What was changed: only the technology example names are used. Vendor prefixes and a trailing
" software" are removed, "long form ACRONYM" names are split in two, names are merged with
Wikidata and Linguist entries, renamed, recategorised under the taxonomy's own twelve
categories, and dropped when a review judged them unsafe to match in a job ad or too rare in
the corpus. The full record of every change is `docs/measurements/taxonomy-candidates-2026-09-24.tsv`
(what was read) and `docs/measurements/taxonomy-review-2026-09-24.tsv` (what was done with it).
Any page of the product that shows the taxonomy has to carry the notice above.

**GitHub Linguist — MIT**, Copyright (c) 2017 GitHub, Inc.
(https://github.com/github-linguist/linguist/blob/v9.7.0/LICENSE). Only language names and
aliases are taken, not the file itself.

**Wikidata — CC0** (https://creativecommons.org/publicdomain/zero/1.0/). No attribution is
required.

**The corpus** supplies only names — what the ads call a tool — and no text of any ad is
kept in the taxonomy.

## How it was built

Three scripts in `scripts/`, each run against the development database:

1. **Seed** (#37). `taxonomy_candidates.py` turns the three sources into 3,793 candidate
   skills and counts, for each, how many of the 26,587 IN descriptions name it. Archived in
   `docs/measurements/taxonomy-candidates-2026-09-24.tsv`, whose header repeats the versions,
   hashes and pull dates above.
2. **Review and select** (#38). The 1,221 candidates with a document frequency of at least 3
   were reviewed one by one — keep or drop, canonical name, category, aliases — by four
   subagents; dbt, which no source supplies, was added by hand. `taxonomy_select.py` merges
   the verdicts by canonical name, recounts each kept skill by the keys the review kept, and
   writes the skills named in at least 3 descriptions: 521.
3. **Mine** (#40). `taxonomy_mine.py` lists the names the corpus uses that no candidate covers:
   df ≥ 3, in the ads of ≥ 3 companies, written as a name in ≥ 40% of their mid-sentence
   occurrences, near a known skill in ≥ 20% of them — 8,290 names, archived in
   `docs/measurements/taxonomy-mined-2026-09-24.tsv`. They were screened and reviewed by
   subagents under the same rules (`docs/measurements/taxonomy-mined-review-2026-09-24.tsv`);
   334 survivors were not reviewed before the usage limit and are left to #41.
   `taxonomy_select.py` over both reviews, again at a threshold of 3, wrote the 2,009 skills of
   `skills.tsv`.

From then on `skills.tsv` is curated by hand. The scripts and archived files are the record of
how it started, not a build step: rerunning `taxonomy_select.py` over the file would throw the
hand edits away.

## Every skill, by source

Every one of the 2,009 skills in `skills.tsv` as written by #40 traces back to a kept verdict,
and every verdict to a source. A skill named by several sources counts under each.

| Source | Skills |
| --- | --- |
| The corpus (mined, #40) | 1,561 |
| O\*NET | 300 |
| Wikidata | 264 |
| Linguist | 135 |
| Added by hand (dbt) | 1 |

1,488 skills come from the corpus alone; the other 521 were supplied by at least one of O\*NET,
Wikidata, Linguist or the hand addition. A skill added to `skills.tsv` by hand after this is
the curator's, and is not one of these sources.
