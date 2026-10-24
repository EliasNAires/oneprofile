-- What Workday publishes as a structured field and no other ATS does (ADR-0021): its time type
-- (Full time, Part time), as Workday writes it. Null means Workday did not provide it, or the
-- vacancy's page was not read. Its country and every location go in the columns Lever's take.
alter table vacancy
	add column time_type text;
