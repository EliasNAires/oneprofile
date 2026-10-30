# What places a location string names

The criterion of the places a declared location names (#26). The blind sample's labellers read it, and
the place rule (`PlaceRule`) answers to it. Where the rule and this file disagree, this file is right.

A **location string** is one entry of a vacancy's declared location, as its ATS publishes it: the
location field (`San Francisco, CA`), or one entry of the ATS's list of locations (`Remote (Canada)`).
Its **places** are what it names, each as precisely as the string gives it.

## What the labeller sees

Each string, and the country the ATS names for it where it names one (Ashby gives a country for every
location it lists). Nothing else: not the company, the title or the description.

## A place

A place is one of four kinds, the most precise the string gives:

- **city**: a city, town or village, a military base or a census place (`Fort Meade`, `Annapolis
  Junction`), with the state or province it lies in where the place has one, and its country.
- **subdivision**: a state, province or other first-level part of a country (`Texas`, `Ontario`,
  `Karnataka`, `England`), with its country.
- **country**.
- **region**: a set of countries named as one (`EMEA`, `LATAM`, `Europe`, `APAC`, `DACH`, `Nordics`,
  `North America`), kept as named. `Worldwide`, `Global` and `Anywhere` are the region `Worldwide`,
  where the string names no other place: `Anywhere in the U.S.` is the United States.

Every level the string gives is part of the place: `Paris, TX` is the city Paris in Texas, United
States; `Remote TX` is the subdivision Texas; `US, CA, Santa Clara` is the city Santa Clara in
California.

## Rules

1. **One place per place named.** `Chicago, IL, Cleveland, OH` names two cities; `Atlanta, GA; Tempe,
   AZ` two; `Berlin or Munich` two; `New York, New York, United States` one (the city, in its state and
   country). A city with its state or country is one place, not two.
2. **Not places**: work words (`Remote`, `Hybrid`, `Office`, `HQ`, `Home based`), a business park,
   building, campus, street, floor or postcode (`Bagmane Tech Park`, `1407 Broadway`, `Sector 135`), a
   company or site name (`ETIC`, `Corporate`), and a neighbourhood or district of a city
   (`Goregaon`, `Powai`, `La Défense`, `Canary Wharf`). Where the string names such a thing and the city
   it lies in, the place is the city (`Mumbai Goregaon` is Mumbai, `Powai, Mumbai, MH` is Mumbai). Where
   it names such a thing alone, the place is what else the string names, or nothing.
3. **A metro area is its main city**: `Greater Seattle Area` is Seattle, `SF Bay Area` and `Bay Area`
   are San Francisco, `Silicon Valley` is the city-level place Silicon Valley in California.
4. **The string's own context decides an ambiguous name**: `Paris, TX` is in Texas, `London, Ontario`
   in Canada, `Atlanta, Georgia` in the United States, `Perth, WA` in Western Australia, `Vancouver, WA`
   in Washington. A two-letter code after a town is its state where one fits (`Malvern, PA` is
   Pennsylvania, not Panama).
5. **A name alone is the place a job seeker would take it for**: the best-known place of that name
   (`Paris` is Paris, France; `London` is London, England; `Georgia` alone is the country). `Singapore`,
   `Hong Kong`, `Luxembourg` and `Monaco` alone are the city.
6. **A town the label cannot place** is still a place: write it with what the string gives (`Malvern,
   PA` is the city Malvern in Pennsylvania). Write `unclear` only where you cannot tell what place the
   string means at all.
7. **The ATS's country is taken.** Where the ATS names a country for the string, that country is the
   string's country: every place is read within it (`London` with the country United States is not
   London, England), and the country itself is the place where the string names nothing more precise in
   it (`Remote` with the country Canada is Canada). A region the string names is kept beside it.
8. **No place** for a string that names none (`Remote`, `Hybrid`, `Multiple Locations`, `N/A`).

## How to answer

Write a TSV with the header `id	places	reason` and one line per string, in the batch's order.
`places` is every place the string names, joined by ` ; `, each written as one of:

- `city: <city>, <state or province, or -> , <country code>`, such as `city: Paris, Texas, US`
- `subdivision: <state or province>, <country code>`, such as `subdivision: Ontario, CA`
- `country: <country code>`, such as `country: GB`
- `region: <region>`, such as `region: EMEA`

or `none` where it names no place, or `unclear`. A country code is the two-letter ISO code (`GB` for
the United Kingdom). Write names in English, as an atlas would. `reason` is a few words where the
answer is not plain from the string; empty otherwise.
