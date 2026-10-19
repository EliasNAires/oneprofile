-- The skills extraction found in a vacancy, one row per pair (ADR-0018). The skill is its id in
-- skills.tsv, with no foreign key, because the taxonomy lives in that file. Segments are the indexes
-- of the description's segments it was found in, empty when only the title names it: the body pass
-- reads where a skill sits from them rather than matching again. Rows go with the derived facts
-- they belong to.
create table vacancy_skill (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	skill_id text not null,
	segments integer[] not null,
	in_title boolean not null,
	primary key (normalized_vacancy_id, skill_id)
);

create index vacancy_skill_skill_id on vacancy_skill (skill_id);
