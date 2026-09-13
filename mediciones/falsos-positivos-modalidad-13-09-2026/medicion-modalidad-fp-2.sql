\pset pager off
\timing off

create temp table c as
select v.id, v.title, v.location, nv.work_mode, nv.title as norm_title,
       ' ' || trim(regexp_replace(translate(lower(coalesce(v.title,'')),'áéíóúüñàèìòùâêîôûç','aeiouunaeiouaeiouc'),'[^[:alnum:]]+',' ','g')) || ' ' as t
from vacancy v join normalized_vacancy nv on nv.vacancy_id = v.id;

\echo '=== 9. TITULO con remote: titulos crudos completos, con modalidad final y titulo normalizado (todas, sin limite) ==='
select title, work_mode, norm_title, count(*), min(location) as una_location
from c where t like '% remote %'
group by 1,2,3 order by 4 desc, 1;

\echo '=== 11. LOCATION con remote pegado a una palabra que no es un lugar: locations crudas completas, con modalidad final ==='
select v.location, c.work_mode, count(*)
from c join vacancy v on v.id = c.id,
     lateral (select ' ' || trim(regexp_replace(translate(lower(coalesce(v.location,'')),'áéíóúüñàèìòùâêîôûç','aeiouunaeiouaeiouc'),'[^[:alnum:]]+',' ','g')) || ' ' as l) x
where x.l ~ ' (non|not|no|except|but|office|friendly|first|clinical|property|practice|services|corp|location|considered|depending|possible|optional|travel|eligible|opportunity|preferred|flexible|employee|strong|sub|black|public|full|all|any|working|the|from|with|on|within|roles|role|position|home|hybrid|hybird|onsite|virtual|only|time|zone|vary|other|and|to|term|permanent|availability|located|summit|motif|atwell|corporate|headquarters|opco|semafor|quadbridge|pansophic|accel|chq|rmt) remote '
   or x.l ~ ' remote (non|not|no|except|but|office|friendly|first|clinical|property|practice|services|corp|location|considered|depending|possible|optional|travel|eligible|opportunity|preferred|flexible|employee|strong|sub|black|public|full|all|any|working|the|from|with|on|within|roles|role|position|home|hybrid|hybird|onsite|virtual|only|time|zone|vary|other|and|to|term|permanent|availability|located|summit|motif|atwell|corporate|headquarters|opco|semafor|quadbridge|pansophic|accel|chq|rmt) '
group by 1,2 order by 3 desc, 1;

