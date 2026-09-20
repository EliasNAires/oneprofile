-- The facts derived from a vacancy by rule. One row per vacancy, holding every derived scalar, and
-- columns arrive with the step that fills them: this is the cleaning step, so the only column here
-- is the cleaned title. Derived rows go when their vacancy goes, because a vacancy that has left
-- its board is closed and nothing derived from it can be read again.
create table normalized_vacancy (
	id bigint generated always as identity primary key,
	vacancy_id bigint not null references vacancy on delete cascade,
	cleaned_title text not null,
	constraint normalized_vacancy_vacancy_id_unique unique (vacancy_id)
);

-- Every seniority level a vacancy's title names, one row each, because a title may name more than
-- one and the reports count them with a join.
create table normalized_vacancy_title_seniority (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	title_seniority text not null,
	primary key (normalized_vacancy_id, title_seniority)
);
