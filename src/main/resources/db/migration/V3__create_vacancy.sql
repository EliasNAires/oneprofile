create sequence vacancy_seq start with 1 increment by 50;

create table vacancy (
    id bigint not null,
    company_id bigint not null references company,
    external_id bigint not null,
    title varchar(255),
    location text,
    department varchar(255),
    description text,
    url text,
    language varchar(255),
    pay_min_cents bigint,
    pay_max_cents bigint,
    pay_currency varchar(255),
    pay_title varchar(255),
    first_published timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    primary key (id),
    unique (company_id, external_id)
);
