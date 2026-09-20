#!/usr/bin/env bash
# Attributes the built corpus back to the crawls it came from: one row per crawl, giving how many
# slugs that crawl named, how many of their boards are live, and how many vacancies those boards
# published, plus a totals row over the union.
#
#     scripts/attribute-crawls.sh
#
# Run against the built corpus, with the slug lists scripts/discover-crawl.sh wrote still on disk.
# A crawl that also appears in a newer one is counted in both rows, because the question each row
# answers is what that crawl on its own would have been worth. The slug lists are transient: once
# the measurement document is written they can go, and a crawl is a minute to rediscover.

source "$(dirname "$0")/lib.sh"

for crawl in "${CRAWLS[@]}"; do
	[ -f "$CORPUS_DIR/slugs/$crawl.txt" ] || { echo "No slug list for $crawl; run scripts/discover-crawl.sh $crawl" >&2; exit 1; }
done

{
	echo "create temporary table crawl_slug (crawl text not null, slug text not null);"
	echo "\copy crawl_slug (crawl, slug) from stdin"
	for crawl in "${CRAWLS[@]}"; do
		awk -v crawl="$crawl" 'NF { print crawl "\t" $0 }' "$CORPUS_DIR/slugs/$crawl.txt"
	done
	echo "\."
	cat <<'SQL'
with board as (
         select s.crawl, c.id, c.board_status, coalesce(v.vacancies, 0) as vacancies
         from (select distinct crawl, slug from crawl_slug) s
              join company c on c.ats = 'GREENHOUSE' and c.slug = s.slug
              left join (select company_id, count(*) as vacancies from vacancy group by company_id) v
                on v.company_id = c.id
     ),
     -- The union is every company any of the twelve named, counted once, which is the corpus.
     counted as (
         select crawl, id, board_status, vacancies from board
         union all
         select 'all twelve', id, board_status, vacancies
         from (select distinct id, board_status, vacancies from board) union_of_crawls
     )
select crawl,
       count(*) as slugs,
       count(*) filter (where board_status = 'ACTIVE') as live_boards,
       round(100.0 * count(*) filter (where board_status = 'ACTIVE') / count(*), 1) as live_rate,
       sum(vacancies) as vacancies,
       round(sum(vacancies)::numeric / nullif(count(*) filter (where board_status = 'ACTIVE'), 0), 1)
         as vacancies_per_live_board
from counted
group by crawl
order by (crawl = 'all twelve'), crawl desc;
SQL
} | psql_dev
