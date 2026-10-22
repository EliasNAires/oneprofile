-- The region of the ATS host that publishes a company's board (ADR-0022): Lever answers each
-- region's boards on that region's API host only. Every company has one. Discovery recorded none
-- before, so a Greenhouse company is EU when its vacancies are published on Greenhouse's EU hosts,
-- and every other company is US; a Greenhouse EU board that was never swept stays US, which no
-- probe or sweep reads, since Greenhouse serves both regions from one API host.
alter table company
	add column region text;

update company c
	set region = case
		when c.ats = 'GREENHOUSE' and exists (select 1 from vacancy v
			where v.company_id = c.id and v.url ~ '^https?://[a-z-]+\.eu\.greenhouse\.io/') then 'EU'
		else 'US'
	end;

alter table company
	alter column region set not null;
