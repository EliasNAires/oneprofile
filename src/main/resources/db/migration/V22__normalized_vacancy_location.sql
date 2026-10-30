-- The places each location string of a vacancy of the engineering subset names (#26), one row per string: the
-- list it came from (the location field, Ashby's secondary locations, or Lever's and Workday's every
-- location), where in that list it stands, the string as the ATS publishes it, and the places it was read
-- as, a JSON array, empty where it names none. Eligibility (#13) reads a vacancy offered in several locations
-- string by string, and a wrong place is traced to its string.
create table normalized_vacancy_location (
	normalized_vacancy_id bigint not null references normalized_vacancy on delete cascade,
	list text not null,
	position integer not null,
	location text not null,
	places jsonb not null,
	primary key (normalized_vacancy_id, list, position)
);
