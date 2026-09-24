#!/usr/bin/env bash
# Draws the sample a classification review labels: 100 IN, 500 OUT and 400 UNKNOWN rows at random
# from the corpus as the rules last classified it, excluding every id a label fixture already holds
# and every calibration title. Fresh means fresh: re-measuring on rows a rule was grown from is
# training on the test set.
#
#     scripts/draw-classification-sample.sh <out-dir>
#
# Writes three files into <out-dir>:
#
#   sample.tsv       id and cleaned title, shuffled so the strata cannot be told apart. The only
#                    file the labeller is given.
#   predictions.tsv  id, state and reason, as the rules answered. Not read until the labels are
#                    written.
#   corpus.tsv       the whole corpus counted by state and reason, and the eligible row count.

source "$(dirname "$0")/lib.sh"

out="${1:?usage: draw-classification-sample.sh <out-dir>}"
mkdir -p "$out"

held_ids() {
	grep -hv '^#' "$REPO"/src/test/resources/labels/*.tsv | cut -f1 | grep -v '^$'
}

# Lower-cased, since the calibration sets hold lower-case titles and the corpus keeps the case.
held_titles() {
	grep -hv '^#' "$REPO"/src/test/resources/calibration/*.tsv | cut -f1 | grep -v '^$' |
		tr '[:upper:]' '[:lower:]' | sed 's/\\/\\\\/g'
}

{
	echo 'create temp table held_id (id bigint); copy held_id from stdin;'
	held_ids
	echo '\.'
	echo 'create temp table held_title (title text); copy held_title from stdin;'
	held_titles
	echo '\.'
	cat <<-'SQL'
		create temp view eligible as
			select id, cleaned_title, classification_state as state, lower(classification_reason) as reason
			from normalized_vacancy
			where classification_state is not null
				and id not in (select id from held_id)
				and lower(cleaned_title) not in (select title from held_title);
		copy (
			select id, cleaned_title, state, coalesce(reason, '')
			from (select *, row_number() over (partition by state order by random()) as n from eligible) ranked
			where (state = 'IN' and n <= 100) or (state = 'OUT' and n <= 500) or (state = 'UNKNOWN' and n <= 400)
			order by random()
		) to stdout;
		\echo ---
		copy (
			select classification_state, coalesce(lower(classification_reason), ''), count(*)
			from normalized_vacancy where classification_state is not null group by 1, 2 order by 1, 2
		) to stdout;
		copy (select 'eligible', '', count(*) from eligible) to stdout;
	SQL
} | psql_dev -q -At | awk -F'\t' -v out="$out" '
	$0 == "---" { counts = 1; next }
	counts { print > (out "/corpus.tsv"); next }
	{ print $1 "\t" $2 > (out "/sample.tsv"); print $1 "\t" $3 "\t" $4 > (out "/predictions.tsv") }
'

say "drew $(wc -l <"$out/sample.tsv") rows into $out"
