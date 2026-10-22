# Each ATS's vacancy is its own class over one table

Status: accepted

#20 gave the vacancy one shape for two ATSs: Ashby's structured fields became nullable columns of
every vacancy, which Greenhouse leaves null. With each new ATS that shape grows into the union of
every ATS's fields, and a null stops saying whether the vacancy left a field out or its ATS has no
such field. Decided by grilling Elias on 2026-10-21 (#73). Overrides PRD §7's "nullable columns".

- **One `vacancy` table, a class per ATS.** JPA `SINGLE_TABLE` inheritance: `GreenhouseVacancy`
  and `AshbyVacancy` extend `Vacancy`, each declaring only its own fields. The discriminator is
  `vacancy.ats`, always its company's ATS.
- **The test for `Vacancy` against a subclass:** a field goes on `Vacancy` if any ATS would be
  expected to publish it, and null there means the vacancy did not. Today that is `external_id`
  (text, since one column holds Greenhouse's numbers and Ashby's UUIDs), title, location,
  department, description, url, `first_published_at` and the pay range (`pay_min_cents`,
  `pay_max_cents`, `pay_currency`). A field only one ATS publishes, or publishes in a shape of its
  own, goes on its subclass.
- **Each ATS keeps what it publishes as it publishes it.** `GreenhouseVacancy`: `pay_title`,
  `updated_at`, `language` as the board declares it. `AshbyVacancy`: `workplace_type`,
  `address_country`, `secondary_locations`, `pay_interval`, as Ashby writes them. Nothing is
  dropped: ADR-0004's reason for keeping Ashby's structured country holds, and `workplace_type`
  keeps its name as Ashby's own field, not the work mode.
- **Facts are derived in passes, never in the sweep.** Work mode, declared location and the
  country set are derived for every ATS by their passes (PRD §5.3, §5.4), which may read an ATS's
  own fields as evidence. `DescriptionMarkingRule` stays in the sweep as the one exception: it
  formats, it derives no fact.
- **No common interface methods yet.** Each question a pass asks of every vacancy ("what work mode
  does it declare?") becomes a method with the pass that asks it, answered by each subclass.
- **`PublishedVacancy` is split the same way**: a sealed interface with the common fields,
  implemented by `PublishedGreenhouseVacancy` and `PublishedAshbyVacancy`; `VacancyStore` holds
  each as its entity subclass.
- **`Company` is unchanged.** A name is something any ATS could publish; null for Ashby means
  Ashby did not.
  *Amended 2026-10-22 (#79):* `Company` gains its region (ADR-0022), which every ATS's company
  has; the name is still the only field the ATS publishes about the company.
- **Two ATSs' fields of the same name and meaning share a column** (amended 2026-10-22, #79):
  Lever's `workplaceType` and its pay interval are held in `workplace_type` and `pay_interval`
  beside Ashby's, each declared by its own class and holding its own ATS's values. A field of one
  ATS that means something else, such as Ashby's address country beside Lever's country, gets a
  column of its own.
- **Passes do not load `Vacancy` polymorphically over the corpus** when they need only the common
  fields; they read it through a projection, or name the subclass they need.

## Considered options

- **Nullable columns on one class** (#20's shape): the union of every ATS's fields on every
  vacancy, and a null that means two things.
- **Normalizing in the sweep**, mapping each ATS's fields onto shared ones (Ashby's workplace type
  onto work mode): a derived fact made outside a pass, with no rule to re-run, and Ashby's own
  values lost.
- **A table per ATS**: every pass over the corpus would union them, and `normalized_vacancy` and
  `vacancy_skill` would reference two tables.
