-- Whether a vacancy's description offers visa sponsorship or relocation (#13): offers that move a person and add
-- no country to its country set (ADR-0027). Null for a vacancy outside the engineering subset or before the
-- country-set run.
alter table normalized_vacancy
	add column offers_visa_sponsorship boolean,
	add column offers_relocation boolean;

-- The country set of each vacancy of the engineering subset (#13), one row per stated set: where it stands among
-- the vacancy's, where it was read (a location string or one option of it, the title, the description), what it
-- says (a location, a region, work authorization, citizenship, a clearance, a timezone requirement), the work
-- mode of a location option, the description segment it was read in, the text that stated it, its places as a
-- JSON array with regions kept as named, and the window of a timezone requirement in minutes of UTC offset. The
-- eligibility state is derived from these at query time, for whichever country and city are asked.
create table normalized_vacancy_stated_set (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	position integer not null,
	source text not null,
	kind text not null,
	work_mode text,
	segment integer,
	text text not null,
	places jsonb not null,
	utc_from integer,
	utc_to integer,
	primary key (normalized_vacancy_id, position)
);
