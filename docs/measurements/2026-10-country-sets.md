# Country sets: the first run (#13)

2026-10-30, on `classified-2026-10-25`, with the rules as first built (`CountrySetRule`, `EligibilityRule`):
`POST /country-sets` over the engineering subset, 155,767 vacancies, in 7 min 47 s. It reads the work modes
of #86 and the places of #26, so it runs after both.

## What is stored

One row of `normalized_vacancy_stated_set` per stated set, with its kind, where it was read and the text
that stated it. A location string offering a choice of modes (143 strings name remote beside another
mode) is cut into options, each with its own mode.

| source | kind | sets | vacancies |
|---|---|---:|---:|
| location | location | 219,219 | 153,817 |
| location | region | 2,274 | 2,098 |
| title | location | 3,349 | 3,349 |
| title | region | 1,456 | 1,456 |
| title | clearance | 1,338 | 1,338 |
| description | location | 4,915 | 4,827 |
| description | region | 1,199 | 1,192 |
| description | authorization | 6,149 | 6,149 |
| description | citizenship | 8,768 | 8,768 |
| description | clearance | 12,336 | 12,320 |
| description | timezone | 34 | 34 |

945 descriptions offer visa sponsorship and 3,351 relocation, stored apart (ADR-0027).

Regions expand into countries through `src/main/resources/places/regions.tsv`, kept by hand: 29 regions
and `Worldwide`, every country. Time zones come from GeoNames' `timeZones.txt` (`timezones.tsv`, and a
`timezone` column on `cities.tsv`), cut by `scripts/fetch-geonames` on 2026-10-30; the cities are those of
the place run but for one row GeoNames changed.

## The states, with the person's city unknown

Every vacancy is judged by country here; the distance of ADR-0028 is checked by `EligibilityRuleTest`.

| | Argentina | | | United States | | |
|---|---:|---:|---:|---:|---:|---:|
| | eligible | excluded | unknown | eligible | excluded | unknown |
| all (155,767) | 1.9% | 29.0% | 69.2% | 49.0% | 8.6% | 42.4% |
| remote (24,286) | 8.7% | 18.6% | 72.7% | 49.3% | 6.9% | 43.9% |
| hybrid (12,570) | 0.6% | 93.2% | 6.2% | 48.7% | 48.7% | 2.6% |
| onsite (10,824) | 0.1% | 98.2% | 1.7% | 67.6% | 31.8% | 0.6% |
| undeclared (108,087) | 0.6% | 16.9% | 82.4% | 47.1% | 2.0% | 50.9% |

By the source that decided the state, as shares of the subset:

| source | Argentina eligible | excluded | United States eligible | excluded |
|---|---:|---:|---:|---:|
| location | 1.0% | 12.1% | 47.5% | 5.8% |
| title | 0.5% | 0.9% | 0.4% | 0.0% |
| description | 0.4% | 16.1% | 1.1% | 2.8% |
| none (unknown) | | 69.2% | | 42.4% |

For Argentina, the description excludes more than the location does: 25,030 vacancies, while 19,796
vacancies of the subset state US work authorization, citizenship or a clearance. The location excludes
the onsite and hybrid vacancies abroad (ADR-0027). Of the 2,885 eligible, 1,547 name Argentina or a region holding it in a remote or
undeclared option, 763 in the title (`LATAM`), and 575 in the description.

## Onsite and hybrid vacancies

Of the 23,394 onsite and hybrid vacancies, 21,797 (93.2%) name a city in an onsite or hybrid option and
are judged by distance once the person's city is known; 1,241 (5.3%) name a country, subdivision or
region and no city; 356 (1.5%) name no place.

## Choices the issue left open

These are readings the issue did not state, made while building; #12 measures whether they hold.

- **Every requirement must hold.** The description's locations and regions are one set, however many
  sentences name them; each authorization, citizenship, clearance and timezone requirement must hold
  beside it. A vacancy naming Canada as a place to hire and requiring US work authorization excludes
  Canada.
- **The title only includes.** A region or country in the title (`Developer - Remote, Latin America`)
  counts like a remote option's location: for eligibility, never against, whatever the work mode. A
  clearance in the title excludes.
- **A clearance's country** is the United States for `TS/SCI`, a polygraph or public trust; otherwise the
  one country the declared location names, and the United States where it names none or several.
- **A title's code** is a place only where titles use it for one (`US`, `UK`, `EU`, `EMEA`, `LATAM`, ...):
  `AI/ML`, `GTM` and the state in `Oakland, CA Office` were read as Anguilla, Mali, Guatemala and
  Canada in the first run.
- **An option a choice leaves unmarked** (`San Francisco, CA` in `San Francisco, CA; USA - Remote`) is
  undeclared, read as remote.
- **Contractor and employer-of-record hiring** states where the vacancy hires (`we hire contractors in`,
  `through an employer of record in`), read like any other place the description hires in.
- **Working from anywhere for some days a year** is a benefit and states nothing; nor does a preferred
  window or the team's.

## What the first runs corrected

Read off the stored sets of the first two runs, before this one:

- Clearance form labels: `Government Clearance Required?` followed by `No` (1,085 vacancies) and
  `Minimum Clearance Required to Start: None` read as a clearance. A label is now read with its answer.
- `Work from anywhere in the world for up to 60 days per year` read as a global set.
- `Hire only people who can raise the average` read as the town Hiré.
- The pass read every segment with every pattern and took 38 minutes; a segment holding none of the
  words a statement needs is now skipped.
