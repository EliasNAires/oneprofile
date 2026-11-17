-- Exact duplicates replace vacancy groups (#106). The kept vacancy remains visible and each duplicate
-- keeps its reference to it; the group identity and its sequence are no longer needed.
drop index normalized_vacancy_group_id_idx;
alter table normalized_vacancy drop column group_id;
drop sequence normalized_vacancy_group_id_seq;
