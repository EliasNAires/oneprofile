-- A company is identified by the pair of ATS and slug, so that pair is what is kept unique.
create table company (
	id bigint generated always as identity primary key,
	ats text not null,
	slug text not null,
	constraint company_ats_slug_unique unique (ats, slug)
);
