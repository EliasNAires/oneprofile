-- Which signal decided the classification state: the title, or the body for a vacancy the title
-- left unknown for a corpus reason. Every state recorded until now was the title's.
alter table normalized_vacancy
	add column classification_signal text;

update normalized_vacancy set classification_signal = 'TITLE' where classification_state is not null;
