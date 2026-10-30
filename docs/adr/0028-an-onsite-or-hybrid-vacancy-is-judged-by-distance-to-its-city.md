# An onsite or hybrid vacancy is judged by distance to its city

Status: accepted

ADR-0027 made an onsite or hybrid vacancy's declared location its country set. A country is too
coarse for work done at an office: someone in Buenos Aires cannot take an onsite job in Córdoba,
700 km away, though both are in Argentina. Decided by Elias on 2026-10-29 (#26).

Declared locations are normalized into places as precise as the string gives: a country, then a
state or province where one is named, then a city where one is named (#26). For an onsite or
hybrid vacancy whose places include a city, and a person whose city is known, the vacancy is
eligible when one of its cities lies within a fixed radius of the person's, measured between the
two cities' coordinates, and excluded otherwise. The radius is one number for everyone; 50 km is
the starting value. Where either city is unknown, the vacancy is judged by country, as ADR-0027
says. Remote and undeclared vacancies ignore the city, as ADR-0004 says for the country.

The person's city is never asked. A Finder visitor's is a default guessed from the request's
metadata, shown, changeable and remembered, as the country is (ADR-0019); for matching, it is
the profile's.

## Considered options

- **Same city only.** Rejected: Vicente López, 15 km from Buenos Aires in the same metro area,
  would be excluded.
- **Same metro area, from a table of metro areas.** Rejected: no good world table of them is
  published.
- **Asking the visitor for a city.** Rejected by Elias: metadata gives the default, which the
  visitor can change.
- **Using the metadata city only when it lies in the picked country.** Rejected by Elias: a
  visitor whose city is wrong for the country they pick changes the city.
