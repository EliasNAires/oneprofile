-- What Lever publishes as structured fields and no other ATS does (ADR-0021). Null means Lever did
-- not provide it. The country Lever places a vacancy in, as a two-letter code; its commitment
-- (Full-time, Contract) and team, as the board names them; and every location it names. Lever's
-- workplace type and the interval of its pay range go in the columns Ashby's take.
alter table vacancy
	add column country text,
	add column commitment text,
	add column team text,
	add column all_locations jsonb;
