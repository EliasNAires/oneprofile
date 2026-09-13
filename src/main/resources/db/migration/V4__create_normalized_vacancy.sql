create sequence normalized_vacancy_seq start with 1 increment by 50;

create table normalized_vacancy (
    id bigint not null,
    vacancy_id bigint not null unique references vacancy on delete cascade,
    title varchar(255),
    seniority varchar(255),
    work_mode varchar(255),
    primary key (id)
);
