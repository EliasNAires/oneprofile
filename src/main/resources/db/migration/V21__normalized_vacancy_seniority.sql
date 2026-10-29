-- The seniority a vacancy of the engineering subset asks for (PRD §6.1): its one seniority level, set by
-- the level its title names or else by its required years, and those required years, the highest floor
-- of the experience requirements its description states. Both null where nothing says them, and for a
-- vacancy outside the subset or before the seniority run.
alter table normalized_vacancy
	add column seniority_level text,
	add column required_years integer;

-- Every experience requirement the seniority pass accepted in a description, one row each: the segment
-- it was read in (its index, as vacancy_skill's segments are), where in that segment's text it starts,
-- its lower and upper years, and whether it set the required years. The upper years are null where it
-- names no range.
create table normalized_vacancy_experience_requirement (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	segment integer not null,
	position integer not null,
	lower_years integer not null,
	upper_years integer,
	sets_required_years boolean not null,
	primary key (normalized_vacancy_id, segment, position)
);
