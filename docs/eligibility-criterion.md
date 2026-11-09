# Whether a vacancy is open to a visitor group

The criterion of a vacancy's eligibility state (#12, PRD §5.4; the rule loop of #103, ADR-0029,
ADR-0031). The
loop's labeller, adjudicator and reviewer read it, and the country-set and eligibility rules
(`CountrySetRule`, `EligibilityRule`) answer to it. Where the rules and this file disagree, this file is
right.

Eligibility asks: **will this company pay someone located in this country for this job?** Not whether
the job is remote, and not whether the person may work there. Hired as a contractor, through an
employer of record or otherwise, counts.

Every vacancy is asked two verdicts for each of three **visitor groups** (ADR-0031):

- **latam**: Argentina, Brazil, Mexico, Colombia, Chile.
- **us**: the United States.
- **english**: the United Kingdom, Canada, Australia, New Zealand, Ireland.

The **rule verdict** follows the decisions below (ADR-0004, ADR-0027), not what a reader would guess:
a `Remote - US` vacancy whose description is silent is unknown for latam, not excluded. The **reader
verdict** is the reader's own judgement of the text shown.

**A group's verdict is the best of its countries'**: the verdict is first given for each country of the
group, then the group takes the best of them, eligible over unknown over excluded, and open over unclear
over closed. `We hire in Brazil` is eligible for latam; `Remote - Canada` is eligible for english though
it leaves out the UK. A place, a region or a requirement is judged country by country: `South America`
holds Argentina, Brazil, Colombia and Chile but not Mexico, and `must be authorized to work in Canada`
excludes every country of english but Canada.

## What the labeller sees

Per vacancy: an id, the company, the title, and its
**options**: each entry of its declared location with the work mode it declares, `remote`, `hybrid`,
`onsite` or `undeclared`. Then only the description's segments holding a cue (a place or a region,
`remote`, `office`, `onsite`, `hybrid`, `based`, `located`, `relocat`, `authoriz`, `citizen`,
`clearance`, `time zone`, `UTC`, `GMT`, `anywhere`, `hire`), each under the heading it sits in, with no
segment around it. A segment starting `- ` is a list item, `# ` a heading; `[under: X]` names the
heading it sits under; `>` marks text every vacancy of the board carries. What is not shown counts as
silent.

## The rule verdict

Given for each country, with the person's city unknown, then for the group as above.

### The options

The work mode is the one each option declares. Never read it from the description: a vacancy whose
options are undeclared stays undeclared, whatever the description says.

1. **An onsite or hybrid option** is eligible where one of its places lies in the country, and
   excluded otherwise. `Berlin, Germany [onsite]` is excluded for every group; `Austin, TX [onsite]`
   is eligible for us.
2. **A remote or undeclared option** is eligible where one of its places lies in the country or is a
   region that holds it, and unknown otherwise. It never excludes: `Remote - US` is unknown for
   latam. The list of regions is of examples, and any name of a region holding a country counts:
   `Worldwide`, `Global` and `Anywhere` hold every country; `Americas` and `AMER` hold every country of
   latam and us, and Canada; `LATAM` and `Latin America` hold latam's; `North America` the US, Canada and
   Mexico; `Europe`, `EMEA` and `UK & Ireland` the UK and Ireland; `APAC` and `ANZ` Australia and New
   Zealand. A region before a narrower place (`AMER - Canada - Ontario`) gives the
   narrower place.
3. **An option that names no place** (`Remote`) is unknown.
4. **Several options** are a choice: the vacancy takes the best of them, eligible over unknown over
   excluded.

### The title

5. **A country or region the title names** (`Engineer - Remote, LATAM`) counts as a remote option's
   place does: eligible where it holds the country, otherwise it says nothing. A US state or a Canadian
   province counts as its country.
6. **A security clearance the title names** (`TS/SCI`, `Secret Clearance`) excludes every country but
   the one that grants it, as 10 says.

### The description

What the description states counts only where it is a statement about **this vacancy**, about **where
the person must be or where the company hires**, and **required**: `must`, `required`, `only`, `open
to candidates in`, `we hire in`, `you will be based in`.

7. **Where the person must be, or the company hires.** `Remote within the US`, `we can hire in Canada
   and Mexico`, `candidates must reside in the EU`, `we hire contractors in LATAM through Deel`. A
   statement of where this role is done is required by its nature, with or without `must`: `This role
   is based in our London office`, `This position is located in Vilnius`, `You'll work in Hyvinkää`,
   `Location: Bucharest`. All such places in one description are one set: eligible where one of them
   holds the country, excluded otherwise.
8. **Work authorization in a country** (`must be authorized to work in the United States`) excludes
   every other country.
9. **Citizenship** (`US citizens only`, `must be a US person`) excludes every other country. A list of
   citizenships accepted (`citizens of Australia, New Zealand or the EU`) excludes the countries it
   leaves out.
10. **A security clearance**, held or obtainable (`active Secret clearance`, `ability to obtain a TS/SCI`),
    excludes every country but the one that grants it. A clearance named by a country's own scheme is
    that country's: `TS/SCI`, `Secret`, `Top Secret`, `Public Trust` and `DoD` are the United States',
    wherever the options are; `SC`, `DV` and `BPSS` the United Kingdom's; `Baseline`, `NV1` and `NV2`
    Australia's. A clearance named by no scheme is the one country the options name (`Government
    Clearance Required? Yes` in Jakarta is Indonesia's), else the United States'.
11. **A timezone requirement** is a window of UTC offsets the person must be located in (`candidates
    in UTC-5 to UTC+1`, `must be based in a US time zone`). Each country's offsets, standard time:
    Argentina UTC-3; Brazil UTC-5 to UTC-2; Mexico UTC-8 to UTC-5; Colombia UTC-5; Chile UTC-6 to UTC-3;
    the United States UTC-10 to UTC-5; the United Kingdom and Ireland UTC+0; Canada UTC-8 to UTC-3:30;
    Australia UTC+8 to UTC+10; New Zealand UTC+12. A country is eligible where every offset of it is
    inside, excluded where none is, unknown where some are: `must be in US Eastern or Central time`
    leaves the United States unknown, Colombia excluded. Several windows given as alternatives (`Europe & Africa: UTC-1 to UTC+3` / `The Americas:
    UTC-8 to UTC-3`) are a choice: the best of them holds. A schedule (`work US Eastern hours`, `4 hours
    of overlap with PST`, colleagues across `GMT-8 to GMT+3`) is not one, and says nothing.
12. **Every requirement must hold.** One that excludes the country excludes the vacancy, whatever the
    options say. One that holds it makes an unknown vacancy eligible. Rules 8 to 10 hold the country they
    name as well as excluding the rest: `all applicants must be eligible to work in the United States`
    and `US citizens only` make a `Remote` or `Multiple Locations` vacancy eligible for us, and
    `authorized to work in the US or Canada` eligible for english.

These **say nothing**, either way:

- What is preferred, prioritised or a plus (`EU work authorization preferred`).
- Visa sponsorship and relocation, offered or refused (`we cannot sponsor visas`, `relocation to Berlin
  provided`).
- A benefit of working from somewhere for a time (`work from anywhere up to 30 days a year`).
- Working from anywhere offered as a perk or a culture: under a heading such as *Benefits*, *Perks*,
  *Why join us* or *Our culture*, or in the company's voice about how it works (`Remote-first culture –
  work from anywhere!`, `WFA (Work From Anywhere)`), unless the segment says the company hires, or the
  person may be located, anywhere (`we hire from anywhere`, `candidates may be located anywhere`).
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

Reading everything shown, as a careful job seeker would: **could someone located in this country get
paid for this job?** Given for each country of the group, then for the group as above.

- **open**: the text gives a reason to believe so: the country, a region holding it or anywhere is named
  as a place the company hires or the person may be, or the job is at an office in the country (an
  office in Austin is open to someone located in the United States, the city aside, as rule 1 has it).
- **closed**: the text makes clear the person must be somewhere else: working at an office in another
  country, or only in named places that leave the country out, or a requirement it cannot meet. A
  description saying the work is at an office counts here even where the options declare no work mode.
  So does a **silent city abroad**: an undeclared option naming only a place outside the country, with
  nothing shown of the mode, reads as an office there (ADR-0029); a silent city inside the country reads
  as open.
- **unclear**: nothing shown says either.

## How to answer

One line per vacancy, tab-separated, in this order:

- **id**.
- **latam-rule**, **latam-reader**, **us-rule**, **us-reader**, **english-rule**, **english-reader**:
  each group's rule verdict, `eligible`, `excluded` or `unknown`, and its reader verdict, `open`,
  `closed` or `unclear`.
- **reason**: the option, title part or phrase that decided the verdicts, quoted in a few words, or `-`
  where nothing did.

## Change log

- **Revision 1**, 2026-10-30: the measurement's (#12, `2026-10-eligibility.md`).
- **Revision 2**, 2026-11-08, the first calibration of the loop (#103, ADR-0029): one verdict a row,
  for Argentina, read from the cue segments alone. Rule 2: the regions are examples, `Global` and
  `AMER` among them; a region before a narrower place gives the narrower place. Rule 7: a statement of
  where the role is done is required without `must`. Rule 11: windows given as alternatives are a
  choice. *These say nothing*: working from anywhere as a perk or a culture. Reader verdict: a silent
  city abroad is closed.
- **Revision 3**, 2026-11-08, the second calibration (#103, ADR-0031): every vacancy is asked both
  verdicts for each of three visitor groups, latam, us and english, and a group's verdict is the best of
  its countries'. Rule 2 names which countries each region holds; rule 9 reads a list of citizenships;
  rule 10 gives a clearance named by a country's scheme to that country; rule 5 reads a US state or a
  Canadian province as its country; rule 11 gives every country's offsets. The reader verdict reads an
  office in the country, or a silent city in it, as open.
- **Revision 4**, 2026-11-08, the same calibration: rule 12 says rules 8 to 10 hold the country they
  name, so work authorization, citizenship or a clearance in a country makes an unknown vacancy eligible
  for it. The check under revision 3 read it two ways.
