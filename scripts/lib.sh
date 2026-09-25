# Shared plumbing for the corpus runs: the development database. Sourced by the scripts next to it,
# never run.
#
# Everything here runs on the development machine in dev mode, never against the production
# server — see docs/adr/0006-rules-are-developed-against-snapshots.md.

set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

say() {
	echo "[$(date +%H:%M:%S)] $*"
}

compose() {
	docker compose --project-directory "$REPO" "$@"
}

psql_dev() {
	compose exec -T postgres psql -v ON_ERROR_STOP=1 -U oneprofile -d oneprofile "$@"
}

# Brings the database up empty, throwing away whatever the volume held. The protection against
# losing the corpus is the snapshot, not the volume.
reset_database() {
	say "resetting the database"
	compose down -v
	start_database
}

start_database() {
	compose up -d postgres
	# Asked over TCP rather than over the socket: a database still initialising answers on the
	# socket before it is ready to be connected to.
	until compose exec -T postgres pg_isready -h 127.0.0.1 -U oneprofile -d oneprofile >/dev/null 2>&1; do
		sleep 1
	done
	say "the database is up"
}
