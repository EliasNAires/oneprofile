-- The vacancy group of each vacancy of the engineering subset (#27). Its id comes from a sequence and
-- references no vacancy, so it outlives any member closing. Every vacancy of the subset has one, a lone
-- vacancy a group of its own. A duplicate has none and holds instead the vacancy it repeats; when that
-- vacancy closes the reference is set null, so the duplicate is shown again. Both are null outside the
-- subset or before the run.
alter table normalized_vacancy
	add column group_id bigint,
	add column duplicate_of_vacancy_id bigint references vacancy on delete set null;

create sequence normalized_vacancy_group_id_seq;

create index normalized_vacancy_group_id_idx on normalized_vacancy (group_id);
