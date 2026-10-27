-- Where a vacancy of the engineering subset says its work is done (#86): REMOTE, HYBRID or ONSITE, read
-- from its location, its ATS's work-mode field or its title. Null where none of them declares one, which
-- never means onsite, and for a vacancy outside the subset or before the work-mode run.
alter table normalized_vacancy add column work_mode text;
