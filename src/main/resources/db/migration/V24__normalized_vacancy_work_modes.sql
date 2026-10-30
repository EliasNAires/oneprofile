-- A vacancy offering a choice of work modes keeps every mode it offers (#98), so a filter on any of them
-- finds it: the one work mode becomes a set, as REMOTE, HYBRID and ONSITE. Null still means none declared,
-- which never means onsite.
alter table normalized_vacancy add column work_modes text[];
update normalized_vacancy set work_modes = array[work_mode] where work_mode is not null;
alter table normalized_vacancy drop column work_mode;
