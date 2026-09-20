# Title cleaning over the raw corpus

Run on the development machine on 2026-09-20, against the raw corpus snapshot
`~/oneprofile-snapshots/raw-2026-09-20.dump` — 6890 companies and 179 098 vacancies, restored
with `scripts/restore-snapshot.sh` — and triggered with one `POST /cleanings`. The rules are pure
functions over a title, so the run is repeatable by restoring that snapshot and calling the
endpoint again.

| | |
| --- | ---: |
| Vacancies cleaned | 179 098 |
| Distinct titles they were written under | 115 286 |
| Distinct titles left once all three rules have run | 104 891 |
| Distinct titles collapsed | 10 395 (9.0%) |
| Wall clock | 54s |

## What each rule collapses

Each rule was applied to the raw titles on its own, with the other two out of the way, which is
what makes the three comparable with one another. They do not add up to the 9.0% above: a rule
reaches titles the others brought together.

Every rule makes a title's spacing even as it removes, so spacing is counted on its own rather
than credited to whichever rule ran. In practice it belongs to the whitelist, which is the rule
that turns characters into separators: the whitelist and the spacing together collapse 4.6%.

| Rule | Distinct titles collapsed | Share | Earns its place |
| --- | ---: | ---: | --- |
| Even spacing | 3436 | 2.98% | — (shared by all three) |
| Character whitelist | 1897 | 1.65%, or 4.6% with the spacing | yes, on the share |
| Unguarded seniority words | 4266 | 3.70% | yes, on the share |
| Gender markers | 402 | 0.35% | yes, on the second ground only |

A rule earns its place by collapsing at least 0.5% of distinct titles **or** by removing a token
that would otherwise pollute classification. Gender-marker removal is kept on the second ground
alone, as the PRD said it would be: at 0.35% it is under the threshold, but it is what keeps
single letters out of titles that are about to be read token by token.

The figures the PRD estimated by SQL approximation before the rules were written were whitelist
4.9%, gender markers 0.3% and seniority words 5.4%. The first two land where they were expected.
The seniority words collapse less than the approximation said, 3.70% against 5.4%, which is what
an approximation is for; the rule still clears the threshold several times over, so nothing about
it was decided by the difference.

## What the rules decide that the specification left open

Two decisions were the rules' to make, and both were made against the corpus rather than by
taste.

**Which slashed letters are a gender marker.** The rule fires on two or more of
`m w d f h x v i k n gn` slashed together, bracketed or not, and only when the group names at
least one of `m`, `w`, `f` or `h`. The corpus holds 45 distinct groups of slashed single
letters. The rule fires on 29 of them and every one is a gender marker, in its German, French,
Dutch, Polish and Italian spellings; it leaves the other 16 alone, and none of those is one —
`C/C++`, `D/N` for a day or night shift, `A/V`, `I/O`, `F/T` and `P/T` for full and part time,
`D/E` for the language pair. Without the second condition, `D/N` would be read as a marker and
stripped from 7 vacancies.

**What to do with a character whose word has gone.** `Senior+ Applied Scientist` and
`Principal/ Sr.Principal Digital Design Engineer` keep a `+` and a `.` through the whitelist,
because at that point each still sits beside the word that gave it a meaning; removing the word
orphans it. What survives the seniority rule is therefore put through the whitelist a second
time, which is what the character is owed once its word is gone. Fifteen distinct titles in the
corpus need this, and it changes nothing for the rest.

## What the titles name

| Level | Vacancies whose title names it |
| --- | ---: |
| Senior | 27 810 |
| Principal | 2450 |
| Junior | 844 |
| Mid | 16 |

30 859 vacancies — 17.2% of the corpus — have a title that names a level, and 261 of them name
more than one, which is why the level is held as a list rather than as a single answer. The 16
mid come from `semi senior` and `ssr` alone, which is the count the PRD predicted for the whole
corpus and the reason those two words are read as mid rather than given a level of their own.

## What is left in the titles, deliberately

- **The trailing acronym.** `Registered Behavior Technician RBT` and `Registered Behavior
  Technician` stay two titles, as do `Psychiatric Mental Health Nurse Practitioner PMHNP` and its
  bare form. The pre-reset rule that collapsed them was not carried over: it collapses 0.1% of
  distinct titles here, under the threshold, and both of those jobs are out of scope.
- **The guarded words.** `staff`, `mid`, `entry` and the numbered levels are still in the titles.
  They need a reading of the job, which only the engineering subset makes tractable, so they
  belong to normalization.
- **Work-mode words.** Not touched here. They belong to normalization, where the location field
  that carries most of that signal is being read anyway.

## What the rules leave behind

- **3227 distinct cleaned titles carry a loose single Latin letter.** 1211 of them are a numbered
  level or a grade — `Engineer I`, `Health Information Specialist I` — which is exactly what
  normalization goes looking for. Most of the rest are French and Portuguese: the whitelist drops
  the apostrophe of `chef d'équipe` and the brackets of `Assistant(e)`, leaving `d` and `e` loose.
  The apostrophe is not one of the four characters the PRD gives a meaning to, so it is dropped
  like any other separator.
- **170 vacancies still say `all genders`.** The gender-marker rule matches the slashed forms the
  PRD names — `(m/w/d)`, `(H/F)` and their variants — and this spelled-out one is left to the
  whitelist, which takes its brackets but not its words.
- **Six titles clean to nothing.** All six are the single word `Principal`, and all six are school
  principals. `principal` names a level wherever it appears, which is what makes it unguarded, so
  the title is emptied and the level recorded. Six vacancies out of 179 098, in a job that is out
  of scope.
