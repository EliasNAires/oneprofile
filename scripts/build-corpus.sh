#!/usr/bin/env bash
# Builds the raw corpus: the twelve crawls into one database, newest to oldest, then one probe over
# the union and one sweep over the boards that have openings, then the snapshot.
#
#     scripts/build-corpus.sh
#
# The whole run is hours long, so it is started detached and watched through its log:
#
#     nohup scripts/build-corpus.sh > ~/oneprofile-corpus/build.log 2>&1 &
#
# Each phase is a synchronous request made under nohup with its answer written to a file, so no
# phase is lost to a hangup even mid-request. A phase that fails is re-run from scratch, which
# means the whole script is.
#
# Because the database is only reset once, what a discovery reports here as companiesAdded is that
# crawl's marginal contribution — the slugs no newer crawl had. Its total is what
# scripts/discover-crawl.sh measures.

source "$(dirname "$0")/lib.sh"

run_dir="$CORPUS_DIR/build"
mkdir -p "$run_dir"

started="$(date +%s)"
reset_database
start_app

for crawl in "${CRAWLS[@]}"; do
	post "/discoveries/$crawl" "$run_dir/$crawl.json"
done

post "/probes/greenhouse" "$run_dir/probe.json"
post "/sweeps/greenhouse" "$run_dir/sweep.json"

stop_app

mkdir -p "$SNAPSHOT_DIR"
snapshot="$SNAPSHOT_DIR/raw-$(date +%F).dump"
say "writing $snapshot"
compose exec -T postgres pg_dump -U oneprofile -Fc oneprofile >"$snapshot"

echo "$(($(date +%s) - started))" >"$run_dir/total.seconds"
say "the corpus is built: $snapshot ($(du -h "$snapshot" | cut -f1))"
