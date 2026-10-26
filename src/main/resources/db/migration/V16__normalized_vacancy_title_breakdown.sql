-- What the title breakdown makes of a vacancy of the engineering subset (ADR-0024). Null until it has
-- run, and for a vacancy outside the subset. A title can name two heads, so the normalized titles are
-- a list in the order named; an empty list means nothing was left once the title's facts were out, as
-- of a bare "Manager". The management level is null where the title names no leadership word.
alter table normalized_vacancy
	add column normalized_titles text[],
	add column management_level text;

-- The levels the breakdown reads from the guarded seniority words (staff, mid, entry and the numbered
-- levels), kept apart from the ones cleaning reads so that each pass replaces only its own. Together
-- they are the vacancy's title seniority.
create table normalized_vacancy_guarded_title_seniority (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	title_seniority text not null,
	primary key (normalized_vacancy_id, title_seniority)
);
