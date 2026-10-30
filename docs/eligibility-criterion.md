# Whether a vacancy is open to a country

The criterion of a vacancy's eligibility state (#12, PRD §5.4). The blind sample's labellers read it,
and the country-set and eligibility rules (`CountrySetRule`, `EligibilityRule`) answer to it. Where the
rules and this file disagree, this file is right.

Eligibility asks: **will this company pay someone located in this country for this job?** Not whether
the job is remote, and not whether the person may work there. Hired as a contractor, through an
employer of record or otherwise, counts.

Each vacancy gets two verdicts. The **rule verdict** follows the decisions below (ADR-0004, ADR-0027),
not what a reader would guess: a `Remote - US` vacancy whose description is silent is unknown for
Argentina, not excluded. The **reader verdict** is the reader's own judgement of the whole text shown.
Where the two differ, the measurement says whether the rules or the decisions should change.

## What the labeller sees

Per vacancy: an id, the company, the title, and its **options**: each entry of its declared location
with the work mode it declares, `remote`, `hybrid`, `onsite` or `undeclared`. Then its description:
either whole, or the segments that name a place, a region or a requirement of where the person is, each
with the segment before and after it. A segment starting `- ` is a list item, `# ` a heading;
`[under: X]` names the heading it sits under; `>` marks text every vacancy of the board carries; `…`
marks segments left out.

## The rule verdict

Given for Argentina and for the United States, with the person's city unknown.

### The options

The work mode is the one each option declares. Never read it from the description: a vacancy whose
options are undeclared stays undeclared, whatever the description says.

1. **An onsite or hybrid option** is eligible where one of its places lies in the country, and
   excluded otherwise. `Berlin, Germany [onsite]` is excluded for Argentina.
2. **A remote or undeclared option** is eligible where one of its places lies in the country or is a
   region that holds it (`LATAM`, `Latin America`, `South America`, `Americas`, `Worldwide`,
   `Anywhere`), and unknown otherwise. It never excludes: `Remote - US` is unknown for Argentina.
3. **An option that names no place** (`Remote`) is unknown.
4. **Several options** are a choice: the vacancy takes the best of them, eligible over unknown over
   excluded.

### The title

5. **A country or region the title names** (`Engineer - Remote, LATAM`) counts as a remote option's
   place does: eligible where it holds the country, otherwise it says nothing.
6. **A security clearance the title names** (`TS/SCI`, `Secret Clearance`) excludes every country but
   the one that grants it, as 10 says.

### The description

What the description states counts only where it is a statement about **this vacancy**, about **where
the person must be or where the company hires**, and **required**: `must`, `required`, `only`, `open
to candidates in`, `we hire in`, `you will be based in`.

7. **Where the person must be, or the company hires.** `Remote within the US`, `we can hire in Canada
   and Mexico`, `candidates must reside in the EU`, `we hire contractors in LATAM through Deel`. All such
   places in one description are one set: eligible where one of them holds the country, excluded
   otherwise.
8. **Work authorization in a country** (`must be authorized to work in the United States`) excludes
   every other country.
9. **Citizenship** (`US citizens only`, `must be a US person`) excludes every other country.
10. **A security clearance**, held or obtainable (`active Secret clearance`, `ability to obtain a TS/SCI`),
    excludes every country but the one that grants it: the one country the options name where the text
    names none (`Government Clearance Required? Yes` in Jakarta is Indonesia's), else the United States.
11. **A timezone requirement** is a window of UTC offsets the person must be located in (`candidates
    in UTC-5 to UTC+1`, `must be based in a US time zone`). Argentina is UTC-3, the United States UTC-10
    to UTC-5: eligible where every offset of the country is inside, excluded where none is, unknown where
    some are. A schedule (`work US Eastern hours`, `4 hours of overlap with PST`) is not one, and says
    nothing.
12. **Every requirement must hold.** One that excludes the country excludes the vacancy, whatever the
    options say. One that holds it makes an unknown vacancy eligible.

These **say nothing**, either way:

- What is preferred, prioritised or a plus (`EU work authorization preferred`).
- Visa sponsorship and relocation, offered or refused (`we cannot sponsor visas`, `relocation to Berlin
  provided`).
- A benefit of working from somewhere for a time (`work from anywhere up to 30 days a year`).
- Where the company or its offices or customers are (`headquartered in New York`, `offices in London and
  Tokyo`, `serving customers across Europe`), and pay ranges for some places (`the range for New York
  is`).
- A requirement for some of the company's roles only (`applicants for employment in the US must have
  work authorization`, `for US based roles only, …`): it says what holds there, not that the vacancy is
  there.
- Authorization or legal text naming no country (`must be legally authorized to work in the country
  where the role is based`), and E-Verify or equal-opportunity notices.

### The state

- **eligible**: a statement above holds the country and none excludes it.
- **excluded**: one excludes it.
- **unknown**: nothing above says either.

## The reader verdict

Reading everything shown, as a careful job seeker would: **could someone located in Argentina get paid
for this job?**

- **yes**: the text gives a reason to believe so: Argentina, a region holding it or anywhere is named
  as a place the company hires or the person may be.
- **no**: the text makes clear the person must be somewhere else: working at an office abroad, or only
  in named places that leave Argentina out, or a requirement it cannot meet. A description saying the
  work is at an office counts here even where the options declare no work mode.
- **unclear**: nothing shown says either.

## How to answer

One line per vacancy, tab-separated:

- **id**.
- **ar**: the rule verdict for Argentina: `eligible`, `excluded` or `unknown`.
- **us**: the rule verdict for the United States.
- **reader**: `yes`, `no` or `unclear`, for Argentina.
- **mode**: where the text says the work is done, read from everything shown, the description
  included: `remote`, `hybrid`, `onsite` or `unclear`.
- **reason**: a few words: the option, title part or phrase that decided `ar`, quoted, or `-` where
  nothing did.
