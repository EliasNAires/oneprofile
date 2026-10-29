# What a vacancy's seniority is

The criterion of the seniority level and the required years (#99, PRD §6.1). The blind sample's
labellers read it, and the seniority rules answer to it. Where the rules and this file disagree, this
file is right.

A vacancy's level is right when it follows the decisions below, not when it matches what a reader
would guess. A `Senior Engineer` asking for 2 years is senior.

## The ladder

entry, junior, mid, senior, staff, principal, from lowest to highest.

## The level

1. **A level word in the title decides.** `entry` (`entry level`), `junior` (`jr`), `mid` (`mid-level`,
   `semi senior`), `senior` (`sr`), `staff`, `principal`. Where the title names several (`Senior/Staff`),
   the lowest. The word must name the job's level. It does not name a level when it is part of a
   product, team or company name (`Senior Living Platform Engineer`, `Lead Generation`), when `staff`
   means personnel (`Chief of Staff`, `Staff Augmentation`), or when it is a title such as `Associate`.
   `Lead`, `Manager`, `Director` and `Head` are not levels on this ladder.
2. **Otherwise the required years decide**: 0–1 entry, 2 junior, 3–4 mid, 5–7 senior, 8 or more staff.
   No years ever give principal. A numbered level (`Engineer II`, `III`, `IV`) is not a level word: such
   a title is placed by its years.
3. **None** where the title names no level and the description asks for no years.

## The required years

The highest floor of the description's experience requirements.

1. **An experience requirement is a number of years of experience the vacancy asks for**, for the role
   or for a skill. `3+ years of Java` asks for three years of working. Years are never added up.
2. **Lowest within one requirement.** A range (`3–5 years`) gives its floor, 3. Alternatives
   (`BS and 5 years, or MS and 3 years`) give the lowest, 3, as do items joined by a trailing `or`.
   An `or` between what the years are of (`5+ years of Java or Python`) is one requirement.
3. **Highest across distinct requirements.** They all hold at once: `3+ years of Java` and `2+ years
   with AWS` give 3.
4. **Only requirements count.** Leave out:
   - **Optional ones**: marked `preferred`, `is a plus`, `nice to have`, `bonus`, `desired`, or under a
     heading such as `Preferred qualifications` or `Nice to have`. Only the part so marked is left out:
     in `5+ years of Java; Kotlin preferred`, the Java years count.
   - **The company's own years**: its age, its history, its people's tenure (`With over 25 years of
     experience, we help…`, `our team brings 100 years of combined experience`). A requirement stays
     one when written in the company's voice (`technologies we use: 5+ years Java` is asked of the
     candidate).
   - **Numbers that are not experience**: age (`18 years of age`), dates and durations (`after 2 years`,
     `a 4-year vesting`, `a 2-year contract`), schooling (`4 years of college`).
   - **Text every vacancy of the board carries** (lines marked `>`), such as a company's standard footer.
5. **A substitution is not a requirement of its own.** `Eight years of experience may be substituted
   for the degree` is an alternative to the degree, not a requirement of eight years.

## What the labeller sees

Per vacancy: an id, the company, the title, and the description's segments that name years or a level,
in order. A segment starting `- ` is a list item, `# ` a heading; `[under: X]` names the heading it sits
under; `>` marks text every vacancy of the board carries. `…` marks segments left out between two shown.

## How to answer

One line per vacancy, tab-separated:

- **id**.
- **years**: the required years, a whole number, or `none`.
- **level**: `entry`, `junior`, `mid`, `senior`, `staff`, `principal` or `none` by the rules above, or
  `unclear` where what is shown does not let you tell.
- **body_level**: where the description states the job's level in words (`a senior-level role`, `we are
  hiring a junior engineer`), that level; else `-`. It does not change the level.
- **reason**: a few words: the title word that decided, or the phrase that set the years, quoted.
