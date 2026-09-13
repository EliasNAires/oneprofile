\pset pager off
\timing off

-- Aproximacion SQL de TitleCleaner: minusculas, sin tildes, todo lo no alfanumerico a espacio.
create temp table phrase (p text, mode text);
insert into phrase values
 ('work from home','REMOTE'),('home based','REMOTE'),('fully remote','REMOTE'),('100 remote','REMOTE'),
 ('remote only','REMOTE'),('remote','REMOTE'),('remoto','REMOTE'),('wfh','REMOTE'),
 ('hybrid','HYBRID'),('hibrido','HYBRID'),
 ('on site','ONSITE'),('in office','ONSITE'),('in person','ONSITE'),('field based','ONSITE'),
 ('onsite','ONSITE'),('presencial','ONSITE');

create temp table c as
select v.id,
       ' ' || trim(regexp_replace(translate(lower(coalesce(v.title,'')),'áéíóúüñàèìòùâêîôûç','aeiouunaeiouaeiouc'),'[^[:alnum:]]+',' ','g')) || ' ' as t,
       ' ' || trim(regexp_replace(translate(lower(coalesce(v.location,'')),'áéíóúüñàèìòùâêîôûç','aeiouunaeiouaeiouc'),'[^[:alnum:]]+',' ','g')) || ' ' as l,
       nv.work_mode, nv.title as norm_title
from vacancy v join normalized_vacancy nv on nv.vacancy_id = v.id;

create temp table hit as
select c.id, ph.p, ph.mode, 'title' as src from c join phrase ph on c.t like '% ' || ph.p || ' %'
union all
select c.id, ph.p, ph.mode, 'location' from c join phrase ph on c.l like '% ' || ph.p || ' %';

\echo '=== 0. Sanidad: reparto de work_mode en normalized_vacancy (esperado REMOTE 13166, FULLY_REMOTE 2928, HYBRID 2340, ONSITE 821) ==='
select work_mode, count(*) from normalized_vacancy group by 1 order by 2 desc;

\echo '=== 0b. Sanidad: vacantes con alguna frase segun la aproximacion SQL, contra las que tienen modalidad ==='
select count(distinct id) as con_frase_sql,
       (select count(*) from normalized_vacancy where work_mode is not null) as con_modalidad
from hit;

\echo '=== 1. Por frase: vacantes con la frase en titulo, en location, y en titulo sin ninguna frase en location (el titulo decide) ==='
select ph.p, ph.mode,
  count(distinct h.id) filter (where h.src='title') as en_titulo,
  count(distinct h.id) filter (where h.src='location') as en_location,
  count(distinct h.id) filter (where h.src='title' and not exists (select 1 from hit h2 where h2.id=h.id and h2.src='location')) as titulo_decide
from phrase ph left join hit h on h.p = ph.p
group by ph.p, ph.mode order by ph.mode, 3 desc;

\echo '=== 2. TITULO: palabra siguiente a cada frase (distribucion completa) ==='
select ph.p, m[1] as siguiente, count(*) from c
join phrase ph on c.t like '% ' || ph.p || ' %'
cross join lateral regexp_matches(c.t, ' ' || ph.p || ' (\S+)', 'g') m
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 3. TITULO: palabra anterior a cada frase (distribucion completa) ==='
select ph.p, m[1] as anterior, count(*) from c
join phrase ph on c.t like '% ' || ph.p || ' %'
cross join lateral regexp_matches(c.t, ' (\S+) ' || ph.p || ' ', 'g') m
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 4. LOCATION: palabra siguiente a cada frase (distribucion completa) ==='
select ph.p, m[1] as siguiente, count(*) from c
join phrase ph on c.l like '% ' || ph.p || ' %'
cross join lateral regexp_matches(c.l, ' ' || ph.p || ' (\S+)', 'g') m
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 5. LOCATION: palabra anterior a cada frase (distribucion completa) ==='
select ph.p, m[1] as anterior, count(*) from c
join phrase ph on c.l like '% ' || ph.p || ' %'
cross join lateral regexp_matches(c.l, ' (\S+) ' || ph.p || ' ', 'g') m
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 6. TITULO: titulos crudos completos por frase (todas las frases menos remote), con su location mas comun ==='
select h.p, v.title, count(*), min(v.location) as una_location
from hit h join vacancy v on v.id = h.id
where h.src='title' and h.p <> 'remote'
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 7. LOCATION: locations crudas completas por frase (todas las frases menos remote y hybrid) ==='
select h.p, v.location, count(*)
from hit h join vacancy v on v.id = h.id
where h.src='location' and h.p not in ('remote','hybrid')
group by 1,2 order by 1, 3 desc, 2;

\echo '=== 8. Conflictos: titulo y location nombran modalidades distintas (gana location) ==='
select ht.mode as modo_titulo, hl.mode as modo_location, count(distinct ht.id)
from hit ht join hit hl on hl.id = ht.id and hl.src='location'
where ht.src='title' and ht.mode <> hl.mode
group by 1,2 order by 3 desc;

\echo '=== 8b. Conflictos: ejemplos (titulo, location) ==='
select ht.mode as modo_titulo, hl.mode as modo_location, v.title, v.location, count(*)
from hit ht join hit hl on hl.id = ht.id and hl.src='location' join vacancy v on v.id = ht.id
where ht.src='title' and ht.mode <> hl.mode
group by 1,2,3,4 order by 1,2, 5 desc, 3;
