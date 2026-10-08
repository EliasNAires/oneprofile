# Recall is asked one name at a time, and a key is plain or context

Status: accepted

ADR-0013 scores skill discovery's recall by asking Jev, for each drawn segment, an open question:
*does this segment name a skill that is not in this list?* Jev answers it badly, and criterion
revisions do not fix it (#58). In round 0 of #54, about 117 of its 140 misses under revision 1,
and 42 of 59 under revision 2, were no misses by the criterion; in round 1, 41 of 58, Go alone six
times, though revision 2 named Go as no miss, with examples. On the precision question, *is this
name a skill?*, Jev is stable across revisions and errs about 5 times in 200. The difference is
the question's shape: precision is a closed keep or drop about a name Jev is given, while recall
asks it to find names in free text, judge them and compare them with a list in one answer. Jev
answers only closed questions (a choice, a boolean, a probability), so it cannot be asked to list
names either. Decided by Elias on 2026-10-07.

## Recall is scored in four steps

For each drawn segment:

1. **Generate.** Code cuts the segment into name-like pieces: capitalized words, tokens with a
   digit or symbol (C++, k8s, HTTP/2), lowercase words outside any known vocabulary, and runs of
   up to three adjacent capitalized words, never across punctuation. Segments keep the ads' case.
   The generator is deliberately loose, frozen with the criterion, and never the discovery rules
   under test, whose blind spots it would share.
2. **Filter.** Code drops a piece that the rules found in the segment, a plain key, a name in the
   decision record, a number, a single letter other than C and R, or a word on the **ordinary
   list**. A context key is never dropped here.
3. **Judge.** Jev answers keep or drop for each piece left, in its segment: the precision
   question, asked with the same call and input, so the two gates share their verdicts. A verdict
   is cached by name and segment.
4. **Score.** A segment is missed when a piece is kept and the rules did not find it. Recall is
   still covered ÷ (covered + missed), by segment, gated at 90%.

A miss comes with its name, so the reviewer's Feedback works from names, and the number scored is
the number adjudicated: the reviewer no longer reclassifies Jev's misses by hand.

## The ordinary list grows from Jev

No dictionary and no case rule decide what is ordinary. The list starts empty or with a short seed
of obvious words, and a word joins it once Jev has dropped it in 5 different segments and never
kept it. A word Jev keeps in some segments and drops in others is a context key in the making, and
a signal for discovery.

A prototype on round 1's 2,400 segments, with no Jev calls, proposed 8,451 pieces; without a
dictionary about 7,000 reach Jev in the first round, about $0.30 with the short criterion, and
fewer each round as the list grows. A skill its author writes in lowercase is never proposed (38
of those segments have no capital at all); the gold set measures that loss.

## Jev reads a short criterion

Precision and recall now ask the same question, so Jev reads one short text for both: the
criterion's Keep, Drop and Key safety, rephrased short, and what a skill is for. The criterion's
*In a segment* section goes; its exceptions are the filters of step 2. The Category question reads
only the Category table. The full criterion stays the reference for the drafting agent, the
adjudicator and the reviewers. Both texts change only at a calibration (ADR-0014), and Jev's
agreement is measured on the short one.

## A skill is a technology

A skill is one specific named language, framework, database, cloud service, tool or platform.
Practices (CI/CD, microservices, TDD) stay out for now. Renaming Skill to Technology was
considered, since the word is in Jev's question and "skill" invites practices, spoken languages
and soft skills; the name was kept, and the criterion says what it means.

## Plain keys and context keys

ADR-0013's key safety mixed two questions: whether a name is a skill, and whether plain string
match finds it without false hits. A bare form like Go, Spring, Rails, Kafka or Compose is a skill
whose name is also an ordinary word. So each key of `skills.tsv` carries an attribute, a new
column:

- **Plain**: matched anywhere, as every key was until now.
- **Context**: matched only where a context rule says the text means the skill. In recall a
  context key is always judged by Jev in its segment.

The attribute is set by a **rule** when the row is drafted, never by hand, so new keys get it and
the app can run the same rule when it extracts skills (#15). A key is context when it is an
ordinary word, or C or R. Jev checks the rule: for each key the rule calls plain, about 20 of its
mentions are drawn and Jev answers whether the key means the skill there. A plain key must mean it
in at least **95%** of them; one that does not is a miss of the rule, and the rule is fixed. The
same verdicts are the labelled data the app's context rules are built from later.

Discovery may propose bare forms again, and the decisions dropped for being a bare form are
reopened by the criterion revision that brings this in.

## The gold set

**300 segments, 150 of them held out**: 200 drawn as a round draws them, and 100 from segments
naming at least one known skill, so the set holds enough skills to measure what the generator
never proposes. Each row lists the skills the segment names and a verdict on each piece left after
step 2. An agent drafts it, in a session other than the calibrating one, and Elias corrects it. It
measures Jev's agreement, about ±2.5 points on the held-out half, and the generator's losses.

The gold set is `docs/measurements/skill-gold-set.tsv`, its `part` column marking the held-out
half, and `scripts/check-skill-gold` checks Jev and the adjudicator against it.

### Agreement

| Calibration | Revision | Jev, held-out pieces | Jev, held-out segments | Adjudicator with the gold set, held-out disagreements | Generator losses, held-out |
| --- | --- | --- | --- | --- | --- |
| #54, 1st (2026-10-07) | 4 | 437/451 = 96.9% | 144/150 = 96.0% | not run | 1 skill in 1 segment |
| #54, 1st (2026-10-07) | 5, frozen | 364/367 = 99.2% (97.6–99.7%) | 149/150 = 99.3% (96.3–99.9%) | 1/3 (2 with Jev) | 1 skill in 1 segment |

Revision 5's pieces are fewer than revision 4's because the ordinary list grew from its own
verdicts. Recorded beside the gates, never gated on (ADR-0014). The filter has since read the
stoplist (#60) in place of the ordinary list; the gold set was not checked again under it, since
the change only drops pieces before Jev and leaves its verdicts as they were.

Round 1, re-scored under revision 5 with the stoplist: the adjudicator sided with the rules on 6 of
Jev's 90 candidate drops and on 12 of its 64 kept pieces
(`docs/measurements/skill-round-1-revision-5/`).

## Considered options

- **Keep revising the recall wording.** Revision 2 spelled out the bare forms, with examples, and
  Jev still counted them. The errors are the question's shape.
- **A dictionary of English and Spanish words in the filter.** Tried in the prototype with
  `cracklib-small`: about 97% of its drops were right, but it dropped Kafka, Spring and Rails, the
  bare forms recall most needs to see. Rejected for the ordinary list, which Jev's own verdicts
  build.
- **Filtering lowercase words by case.** Rejected with the dictionary: case says how an author
  writes, not what a word names. The generator still uses case to propose pieces.
- **Every key safe, as ADR-0013 had it.** Loses every mention of a bare form (Go for Golang), and
  forces the criterion to argue that those mentions are no misses.

## Consequences

- This supersedes ADR-0013's recall definition and its rule that every key is safe; its other
  gates, the decision record and the budget stand.
- All of it lands in #54's first calibration, together, so Jev's cached verdicts are remade once:
  the criterion revision, the short text, the four steps in `scripts/score-skill-round` and
  `scripts/skillloop.py`, the ordinary list, the key column of `skills.tsv`, the reopened
  bare-form drops and the gold set.
- Extraction (#15) matches a context key only through a context rule, so it is no longer a plain
  trie pass over every key.
