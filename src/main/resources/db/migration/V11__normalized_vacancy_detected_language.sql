-- The language cleaning detects a vacancy's description to be written in, as an ISO 639-1 code, for
-- a vacancy whose board declared none: Ashby declares none at all. Null where the board declared one,
-- which is kept as it is, and where the text did not say clearly enough. What the body pass reads is
-- the declared language, or else this one.
alter table normalized_vacancy
	add column detected_language text;
