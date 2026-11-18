-- Deleting a vacancy sets null the duplicates that point at it, and without an index each delete scans the
-- whole table (#107). Partial: only the few duplicates hold a value, and both the delete's lookup and the
-- clearing of duplicates read non-null values alone.
create index normalized_vacancy_duplicate_of_vacancy_id_idx on normalized_vacancy (duplicate_of_vacancy_id)
	where duplicate_of_vacancy_id is not null;
