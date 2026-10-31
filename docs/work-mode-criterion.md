# What a vacancy's work mode is

The criterion of the work mode (#86, PRD §5.3). It is what the blind sample's labellers read and
what the work-mode rule answers to. When the rule and this file disagree, this file is right.

## What a work mode is

Where the work of this vacancy is done, as the vacancy declares it: **remote**, **hybrid** or
**onsite**. A vacancy that declares none has no work mode; that never means onsite.

- **Remote.** The work is done away from the employer's offices, with no regular days in one. A
  country, region or time zone the person must live in does not change it (`Remote - USA`), nor
  does travel now and then: a quarterly offsite, a yearly meetup, a visit to a customer.
- **Hybrid.** Part of the work is done in an office on a regular schedule, and part away from it:
  days a week or a month in the office (`3 days in office`, `Hybrid`), or an office the person must
  come into regularly.
- **Onsite.** The work is done at the employer's or a client's site on every working day, or
  nearly: in an office, a lab, a plant, a customer's premises. A job whose work is travelling to
  sites is onsite.

## What is read

The vacancy's title, its location as the ATS publishes it, the work-mode field its ATS publishes
(Ashby's and Lever's workplace type, Workday's remote type), and the sentences of its description
that name where the work is done.

1. **This vacancy, not the company.** A sentence about how the company works (`We're a
   remote-first company`, `flexible working culture`, `offices in 12 cities`) or a benefit (`home
   office stipend`, `WFH equipment`) is not this vacancy's mode. It counts only where the vacancy
   says nothing else about where its own work is done, and even then only where it plainly covers
   every role (`all our roles are fully remote`). A company policy that sets a floor for every role
   (`everyone is in the office at least 3 days a week`) plainly covers every role, even where some
   groups may ask for more; a hedged one (`most roles`, `depending on the role`) does not. A
   statement about the team this role joins (`The teams are all home based`) is about this vacancy,
   not the company.
2. **A word naming what the work is about is not a mode**: `hybrid cloud`, `remote sensing`,
   `remote access`, `in-person events` the person organises.
3. **The most specific statement about this job decides.** A sentence that states this role's
   arrangement (`This role is hybrid, with 3 days a week in our Austin office`) outweighs a bare
   label in the location or the field. A label in the location (`New York (Hybrid)`) outweighs the
   field (`Remote`) where nothing else speaks. A recruiting tag in the description (`#LI-Remote`,
   `#LI-Hybrid`) is a label of this vacancy, as the location is: it counts where nothing more specific
   speaks.
4. **A mode word with a schedule is read with its schedule.** `Onsite 2x week` and `Remote with 2
   office days a week` are hybrid; `Remote, occasional travel` is remote; `Primarily remote, on site
   as needed` is remote, as it names no regular days. Several modes carrying one schedule between
   them (`Remote/hybrid, in the office three times a week`) are one mode, the schedule's.
5. **A choice of modes is a choice.** Where the vacancy is offered in more than one mode (`Remote or
   onsite in Nashville`, `NYC (Hybrid); United States (Remote)`), answer every mode offered. A
   hybrid vacancy that names its office and its remote days is one mode, hybrid, not a choice.
   - **Offered, not excepted.** A choice is offered to the candidate up front. A role stated in one
     mode that may later be granted another, case by case (`This is an on-site role … may be eligible
     for a remote work arrangement`), is the mode it states.
   - **Ruled out is not offered.** A mode named only to rule it out (`On-site position, no remote or
     hybrid option`) is not part of a choice.
   - **A template is not a choice.** A label listing every mode (`Type of work model: Hybrid / Onsite /
     Remote`) is a company's form, not this job's offer. The rest of the vacancy decides: a
     schedule or statement that follows gives the one mode (`Our Hybrid work model: 3 days in the
     office`). Where nothing follows that narrows it, the template declares nothing: `none`. A
     sentence offering this role in several modes (`Work location is variable with onsite, hybrid, or
     remote options`) is a choice, not a template.
6. **None** where nothing read declares a mode for this job. **Unclear** where what is read
   declares modes for this job that contradict each other and none of rules 1 to 5 decides.

## How to answer

One line per vacancy: its id, the mode (`remote`, `hybrid` or `onsite`; for a choice, every mode
offered, comma-separated, as `remote,onsite`; else `none` or `unclear`), and a reason of a few words
naming the rule that decided it.

## Change log

- **Revision 1**, 2026-10-30: the measurement's (#98, `2026-10-work-mode-description.md`).
- **Revision 2**, 2026-10-31, the first calibration (ADR-0030): settles the cases the measurement left
  and the gold set's review raised. Rule 1: a policy setting a floor for every role covers every role;
  a statement about the team the role joins is this vacancy's. Rule 3: a recruiting tag is a label.
  Rule 4: `primarily remote, on site as needed` is remote; several modes under one schedule are the
  schedule's mode. Rule 5: an exception granted case by case is not a choice; a mode ruled out is not
  offered; a template listing every mode is not a choice, and declares nothing where nothing narrows it.
