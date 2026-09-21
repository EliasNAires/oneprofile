# Unknown is the default, and a title is read as a head with modifiers

Status: accepted

Classification of a title returns `unknown` unless a rule earns `in` or `out`, and the rules
read a title as a **function head** — the noun that names what the role does — qualified by
**modifiers** that name the domain it does it in. Both halves of this replace what
`docs/engineering-role-criterion.md` said before: the old procedure defaulted to `out` for
anything it did not recognise, and it read a title as an unordered bag of tokens.

A hand check of fifty adversarial titles is what forced it. Labelled against the old criterion,
they agreed with it 78% of the time. Simulating candidate rules against those same labels is
what chose between them: flipping the default alone scored 78% again, and reading the title as
head-and-modifier scores **90%**.

The five remaining disagreements are all on phrases the rulings table fixes — `product manager`,
`implementation engineer`, `systems administrator`, `technical program manager`, `game
designer`. A labeller reads those rather than deriving them, which is why ADR-0007 has it read
the whole criterion, rulings included.

That hand check was, at the time, meant to become a recurring 95%-agreement gate in front of
every round. ADR-0007 abandoned the gate. These findings stand regardless: they rest on the
fifty labels and the simulation over them, not on the threshold they were once scored against.

## Why the default flips

The old default was not a decision about titles, it was a decision about budget. `manager`,
`designer` and `consultant` were kept off the ambiguous-word list, and therefore fell through to
`out`, because admitting `manager` alone would have parked 13.4% of the corpus in `unknown`
against the 10% cap of ADR-0008. That is the classifier's exit criterion reaching back and
editing ground truth, and it makes the threshold it protects meaningless.

`unknown` as the default says what is true: a title the rules do not reach has not been read, and
`out` is a claim that has to be earned by a marker, a never-engineering head, or a ruling. The
cost is real and is recorded in ADR-0008: the classifier's unknown share now starts above half
the corpus rather than at 10%.

## Why a head and its modifiers

The hand labels are not reproducible by any bag of tokens, and they are reproducible by a
two-slot reading. Under the head `engineer`, a non-software domain decides: `civil`, `petroleum`,
`aerospace`, `agricultural` and `mechanical engineer` were all labelled `out`. Under the head
`analyst`, the identical modifiers decide nothing: `audit analyst` and `payroll analyst` were
labelled `unknown`, though `audit` and `payroll` are both on the off-domain marker list. The same
modifier reverses across heads — `marketing manager` is `out` and `marketing analyst` is
`unknown`.

That is not inconsistency. `engineer` and `technician` name a function embedded in a domain, so
the domain settles what the role is. `analyst` and `architect` name a function that operates on
information about a domain, so the domain says nothing about the artifact. The criterion now
carries that distinction as an attribute of each head, which is what the reason code
`domain_ambiguity` was reaching for and never managed to state.

## Considered options

- **Rewriting the prose of the old procedure**, on the theory that 78% meant the document was
  unclear rather than wrong. Rejected on the simulation: the old rules reproduce 39 of the 50
  labels no matter how they are worded, and six of the eleven disagreements were caused by a step
  that cannot be executed at all — "if the title's variants split across the membership test" is
  answerable only by someone who has already read the rulings table.
- **A whitelist of ruled phrases, with everything else unknown.** Rejected on coverage. A ruling
  on an exact title generalises to nothing: 104,891 distinct cleaned titles exist, and 86,982 of
  them are needed to cover 90% of the corpus. Keyed on head phrases it is better and still
  hopeless — 1,764 phrases for 50%, 31,376 for 90%. Rulings are kept as an override layer for
  exactly this reason: they are precise and they do not scale.
- **Keeping the bag-of-tokens model and absorbing the residual as rulings.** Rejected as the same
  trap one layer down: every modifier-head interaction would need its own ruling, and there are
  more of those than there are phrases.

## Consequences

**The head list is the new unit of work, and it is tractable.** Twenty-two seeded heads already
match 62.6% of the corpus, and 77 heads cover half of it by trailing word alone. Growing that
list is what each iteration of #10 does, in place of growing the qualifier list.

**The ambiguous-function-word list is deleted.** Its members — `engineer`, `engineering`,
`analyst`, `architect`, `technician` — are all heads, and its only job was to force `unknown`
against a default of `out`. With the default flipped and heads enumerated, it has no work left.

**Head extraction becomes a cleaning rule.** The model rests on identifying the head, and the last
word of a cleaned title is not it: `Remote`, `US`, `2027` and `II` are all among the twenty most
common trailing words. Title cleaning gains a rule that strips trailing location, date and level
junk, held to the same standard as the other three rules in
`docs/measurements/2026-09-title-cleaning.md`, and the head is stored on `normalized_vacancy` so
the classifier looks it up rather than parsing it. Where a title names more than one head —
`Software Engineer / Developer`, `Manager, Data Platform` — the first is the head.

**An off-domain marker beats a software qualifier under a domain-bound head.** `mechanical
software engineer` is `out`. This is the one place the labels disagree with themselves:
`manufacturing systems engineer` has the identical shape and was labelled `unknown`, so it is
carried as a ruling rather than as a clause bent around a single case.
