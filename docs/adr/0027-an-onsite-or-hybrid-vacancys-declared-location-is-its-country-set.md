# An onsite or hybrid vacancy's declared location is its country set

Status: accepted; amended by ADR-0028

ADR-0004 lets a declared location count only for eligibility, never against it. It was written
with remote vacancies in mind, where `Remote - US` often says where the company is and not where it
hires. An onsite or hybrid vacancy is different: the work happens at the place it declares, so the
company pays someone who lives there. Decided by Elias on 2026-10-29 (#13).

For an onsite or hybrid vacancy, the countries of its declared location are its country set, and
every other country is excluded. `Onsite, Berlin, Germany` is excluded for Argentina. ADR-0004 holds
for remote vacancies, which the description alone can exclude, and for those whose work mode is
undeclared: undeclared never means onsite, so it is not read as onsite here either.

A vacancy offered in more than one work mode (`NYC (Hybrid); United States (Remote)`) is a
choice: each option is read by its own work mode, and the country takes the best state of them,
eligible over unknown over excluded. The vacancy above is unknown for Argentina; it is excluded
only when every option excludes.

Visa sponsorship and relocation do not add a country to the set. Eligibility asks whether the
company pays someone located in the country; a vacancy that moves them answers a different
question. A description that offers either is stored as a fact of its own, and a filter on it
comes after the launch.

## Considered options

- **ADR-0004 for every work mode**, so that an onsite vacancy in Berlin is unknown for Argentina.
  Rejected: a visitor in Argentina would see every onsite vacancy in the world marked as saying
  nothing, for a question the work mode already answers.
- **Excluding for onsite and not for hybrid.** Rejected: a hybrid vacancy needs the person near
  the office as much as an onsite one does.
