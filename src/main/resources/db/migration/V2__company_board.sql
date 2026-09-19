-- What a probe found on a company's board. Null until the company has been probed, which is what an
-- unknown board status is.
alter table company
	add column name text,
	add column board_status text,
	add column probed_at timestamptz;
