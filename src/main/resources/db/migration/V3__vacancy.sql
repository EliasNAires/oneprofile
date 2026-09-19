-- A vacancy is a mirror of one opening of a board, identified by its company and the id its ATS
-- gives it. It is deleted once the board stops publishing it, which is what closed means.
create table vacancy (
	id bigint generated always as identity primary key,
	company_id bigint not null references company,
	external_id bigint not null,
	title text,
	location text,
	department text,
	description text,
	url text,
	pay_min_cents bigint,
	pay_max_cents bigint,
	pay_currency text,
	pay_title text,
	first_published_at timestamptz,
	updated_at timestamptz,
	constraint vacancy_company_external_id_unique unique (company_id, external_id)
);
