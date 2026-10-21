-- Each ATS's vacancy is a class of its own over this one table (ADR-0021), told apart by the ATS
-- that published it. That is always its company's, so it is filled from there.
alter table vacancy
	add column ats text;

update vacancy v
	set ats = c.ats
	from company c
	where c.id = v.company_id;

alter table vacancy
	alter column ats set not null;
