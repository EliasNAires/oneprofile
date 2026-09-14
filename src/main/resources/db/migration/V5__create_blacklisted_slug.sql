create sequence blacklisted_slug_seq start with 1 increment by 50;

create table blacklisted_slug (
    id bigint not null,
    ats varchar(255),
    slug varchar(255),
    primary key (id),
    unique (ats, slug)
);
