#!/usr/bin/env bash
# Puts a snapshot back into the development database, replacing whatever it holds.
#
#     scripts/restore-snapshot.sh ~/oneprofile-snapshots/raw-2026-09-19.dump

source "$(dirname "$0")/lib.sh"

snapshot="${1:?usage: restore-snapshot.sh <file>}"
[ -f "$snapshot" ] || { echo "No such snapshot: $snapshot" >&2; exit 1; }

reset_database
say "restoring $snapshot"
compose exec -T postgres pg_restore -U oneprofile -d oneprofile --no-owner <"$snapshot"
say "restored: $(psql_dev -At -c 'select count(*) from company') companies, $(psql_dev -At -c 'select count(*) from vacancy') vacancies"
