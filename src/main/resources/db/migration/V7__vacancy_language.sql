-- The language the board says a vacancy is written in, as an ISO 639-1 code. Only English and
-- Spanish descriptions are read by the body pass, and a vacancy with none counts as either. A
-- vacancy swept before this column existed has none until the next sweep.
alter table vacancy
	add column language text;
