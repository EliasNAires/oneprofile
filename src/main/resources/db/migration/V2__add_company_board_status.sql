alter table company add column name varchar(255);
alter table company add column board_status varchar(255);
alter table company add column last_probed_at timestamp(6) with time zone;
