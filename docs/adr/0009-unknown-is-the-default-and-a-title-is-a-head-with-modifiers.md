# Unknown is the default, and a title is read as a head with modifiers

Status: accepted

Classification of a title returns `unknown` unless a rule earns `in` or `out`, and the rules
read a title as a **function head** — the noun that names what the role does — qualified by
**modifiers** that name the domain it does it in. The head is the first head the title names,
except where that word is a **yielding head** naming a rank or a department (`lead`,
`director`, `manager`, `support`) with another head behind it; then the head behind it is the
head. Some heads are **generic**: they name no work of their own, and with a modifier and no
software qualifier they decide `out`, except where `product` holds them open.
`docs/engineering-role-criterion.md` carries the rules in full.

## Why unknown is the default

A default of `out` is not a decision about titles, it is a decision about budget: words are
kept off the ambiguous list, and fall through to `out`, because admitting them would inflate
the unknown share. That is the classifier's exit criterion reaching back and editing ground
truth, which makes the threshold it protects meaningless.

`unknown` as the default says what is true: a title the rules do not reach has not been read, and
`out` is a claim that has to be earned by a marker, a never-engineering head, a generic head, or
a ruling. The cost is that the unknown share starts above half the corpus, which ADR-0008 prices.

## Why a head and its modifiers

Labelled titles are not reproducible by any bag of tokens, and they are reproducible by a
two-slot reading. Under the head `engineer`, a non-software domain decides: `civil`,
`petroleum`, `aerospace` and `mechanical engineer` are all `out`. Under the head `analyst`, the
identical modifiers decide nothing: `audit analyst` and `payroll analyst` are `unknown`. The same
modifier reverses across heads — `marketing manager` is `out` and `marketing analyst` is
`unknown`.

That is not inconsistency. `engineer` and `technician` name a function embedded in a domain, so
the domain settles what the role is. `analyst` and `architect` name a function that operates on
information about a domain, so the domain says nothing about the artifact. The criterion carries
that distinction as an attribute of each head, and it is what the reason `domain_ambiguity`
names.

## Considered options

- **A bag of tokens defaulting to `out`**, the first form of the criterion. Rejected on a hand
  check of fifty adversarial titles: it agreed with the labels 78% of the time, flipping the
  default alone scored 78% again, and reading the title as head-and-modifier scored 90%.
- **A whitelist of ruled phrases, with everything else unknown.** Rejected on coverage. A ruling
  on an exact title generalises to nothing: 104,891 distinct cleaned titles exist, and 86,982 of
  them are needed to cover 90% of the corpus. Keyed on head phrases it is better and still
  hopeless — 1,764 phrases for 50%, 31,376 for 90%. Rulings are kept as an override layer for
  exactly this reason: they are precise and they do not scale.
- **Keeping the bag-of-tokens model and absorbing the residual as rulings.** Rejected as the same
  trap one layer down: every modifier-head interaction would need its own ruling, and there are
  more of those than there are phrases.
- **The last word of the title is the head.** Rejected: `Remote`, `US`, `2027` and `II` are all
  among the twenty most common trailing words.
- **The first head in the title is always the head.** Rejected: a rank or a department named
  first masks the head behind it — `Lead Analytics Engineer` read as a `lead` — and the masking
  cost 24 titles on a single iteration's sample.
- **Deciding every domain-bound head with an unclassed modifier `out`**, rather than only the
  generic ones. Rejected: `engineer` and `developer` would decide a bare `C Engineer` out, and
  `specialist` and `intern`, priced as generic, each added a miss.

## Consequences

**The head list is the unit of work, and it is tractable.** A few dozen heads cover half the
corpus by trailing word alone. Growing that list is what each iteration of #10 does.

**The head is found by the classifier, not stored.** It scans the cleaned title for the first
head it knows, skipping yielding words while a head still follows them. Where a title names more
than one head — `Software Engineer / Developer` — the first is the head.

**A discipline marker beats a software qualifier under any head.** `mechanical software
engineer` is `out`. ADR-0010 splits markers into discipline and market classes and says why only
the discipline class wins.
