-- Where the work is done as a Workday tenant writes it on a vacancy's page (`Onsite`, `Hybrid Working`,
-- `Remote`, ...): a field named unlike Ashby's and Lever's workplace type, so a column of its own
-- (ADR-0021). Null means Workday did not provide it, or the vacancy's page was not read.
alter table vacancy
	add column remote_type text;
