-- The roles the role rule gives a vacancy of the engineering subset (ADR-0024): the role of each of its
-- normalized titles that has one, once each, in the order its titles name them, by the role's slug.
-- Null until the role assignment has run, and for a vacancy outside the subset; an empty list means
-- none of its normalized titles has a role.
alter table normalized_vacancy add column roles text[];
