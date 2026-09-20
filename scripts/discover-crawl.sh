#!/usr/bin/env bash
# Discovers one crawl against an empty database, so that what it reports is that crawl's total,
# independent of every other crawl.
#
#     scripts/discover-crawl.sh CC-MAIN-2026-39
#
# One invocation per crawl, and a crawl whose report is already on disk is left alone, so the
# twelve can be walked in a loop that is stopped and resumed at will:
#
#     for crawl in $(scripts/discover-crawl.sh --crawls); do scripts/discover-crawl.sh "$crawl"; done

source "$(dirname "$0")/lib.sh"

if [ "${1:-}" = "--crawls" ]; then
	printf '%s\n' "${CRAWLS[@]}"
	exit 0
fi

crawl="${1:?usage: discover-crawl.sh <crawl>}"

report="$CORPUS_DIR/discoveries/$crawl.json"
slugs="$CORPUS_DIR/slugs/$crawl.txt"
mkdir -p "$(dirname "$report")" "$(dirname "$slugs")"

if [ -f "$report" ] && [ -f "$slugs" ]; then
	say "$crawl is already discovered: $(cat "$report")"
	exit 0
fi

reset_database
start_app
post "/discoveries/$crawl" "$report"
dump_slugs "$slugs"
say "$crawl named $(wc -l <"$slugs") slugs"
stop_app
