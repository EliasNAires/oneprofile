-- What classification made of a vacancy's cleaned title. Nullable, because cleaning writes the row
-- and classification fills these in afterwards: a vacancy swept since the last classification run
-- has a cleaned title and no state yet, which is not the same as being unknown.
--
-- The reason is set exactly when the state is unknown. It is what says whether the title is the
-- rules' backlog or the corpus's own ambiguity, and only the first of those is expected to fall.
alter table normalized_vacancy
	add column classification_state text,
	add column classification_reason text;
