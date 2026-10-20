-- Ashby, the second ATS, gives its openings UUIDs, so the id an ATS gives an opening is text. A
-- vacancy is still identified by its company and that id.
alter table vacancy
	alter column external_id type text using external_id::text;

-- What Ashby publishes as structured fields and Greenhouse does not. Null means the ATS did not
-- provide it, never that the vacancy said nothing. The work mode as the ATS names it (Remote,
-- Hybrid, OnSite); the country of the vacancy's own address, as the ATS writes it; its other
-- locations, each as written with its country; and the interval its pay range is quoted in (1 YEAR,
-- 1 HOUR), kept as the ATS writes it rather than converted.
alter table vacancy
	add column workplace_type text,
	add column address_country text,
	add column secondary_locations jsonb,
	add column pay_interval text;
