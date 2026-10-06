-- A vacancy's cleaned description cut into segments: headings, list items and sentences, each with
-- the heading it sits under. Cut once, by cleaning, for every pass that reads descriptions. Null
-- until cleaning has run since this column arrived.
alter table normalized_vacancy
	add column description_segments jsonb;
