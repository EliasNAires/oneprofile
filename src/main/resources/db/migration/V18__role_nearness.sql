-- How near each pair of roles is by the skills their vacancies ask for (ADR-0024), computed again on each
-- snapshot. Nearness is the same both ways, so a pair is held once, its roles in order of slug. The roles
-- are slugs of the role list, a classpath file the database cannot reference.
create table role_nearness (
	role text not null,
	near_role text not null,
	nearness double precision not null,
	primary key (role, near_role),
	check (role < near_role)
);
