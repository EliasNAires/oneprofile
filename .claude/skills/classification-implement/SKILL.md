---
name: classification-implement
description: "Implement one round of issue #35's engineering-role classification loop — change the classifier's rules from the last review's handoff, test-first, and re-classify the corpus. Use when asked to implement a classification round or to fix the rules a classification review reported."
---

# Implementing one round of the classification loop

You are the **implementer** of issue #35's loop. A reviewer measured the rules and wrote down
what they get wrong; you change the rules and re-classify the corpus, so the next reviewer
measures something new. One session is one round. Then you stop.

The criterion — `docs/engineering-role-criterion.md` — is what the rules answer to. Where the
rules and the criterion disagree, the rules are wrong. The criterion is Elias's, changed only in a
grilling session: a question it leaves open goes into your comment on #35.

## The context budget

The session finishes at around 100k tokens. The classifier's lists and the accumulated fixtures
stay on disk: `grep` the lists, and price a candidate rule with a script. An offline harness —
`javac` over the classifier and its immediate dependencies plus a `main` that reclassifies the
fixtures — runs in seconds where Maven takes minutes. Keep it in the scratchpad.

## Steps

**1. Find the round.** Read #35 and its comments. The newest comment is a review, "Round N-1
reviewed"; you implement round N. For round 13 there is no review yet: the handoff is
`docs/measurements/engineering-role-2026-10-03.md`, read against the criterion as it now stands —
its revision of 2026-09-24 decided several families that report's numbers predate.

**2. Read the handoff**: the report's "What the rules get wrong", then the criterion. The report
describes behaviour; the mechanism is yours.

**3. Change the rules**, test-first, in `src/main/java/oneprofile/backend/classification/`.
Every change obeys the criterion's cost limit. Heads are where coverage comes from: a head
generalises across every title that names it, and a ruling across none, so reach for the head
list first and the ruling table last.

Price each candidate over the accumulated fixtures in `src/test/resources/labels/` before keeping
it. Rows a fixture's header note marks as superseded are evidence-free, not costs.

The one change that is never accepted is one that raises the count of titles labelled `IN` that
the rules decide `OUT`: that vacancy reaches neither the body pass nor a profile, so nothing
downstream recovers it. `LabelledFixturesTest` holds the count at a ceiling; when a change lowers
the count, lower the ceiling with it.

Done when every group in "What the rules get wrong" is either handled or recorded as declined
with its reason, and `./mvnw test` is green.

**4. Re-classify.** With the corpus snapshot loaded (`scripts/restore-snapshot.sh` if it is not),
package a fresh jar — `start_app` reuses whatever jar is there — and run the classifier:

```
./mvnw -q -DskipTests package
source scripts/lib.sh && start_app && post /classifications "$CORPUS_DIR/classification-round-<N>.json"; stop_app
```

Done when the report's counts per state are on disk.

**5. Hand off.** Comment on #35: "Round N implemented", what changed and why, what was declined
and why, the corpus counts per state and reason, and any question for Elias. Then stop: the
review is another session's.
