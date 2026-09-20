# Shared plumbing for the corpus runs: the development database, the application that holds the
# endpoints, and the files a run leaves behind. Sourced by the scripts next to it, never run.
#
# Everything here runs on the development machine in dev mode, never against the production
# server — see docs/adr/0006-rules-are-developed-against-snapshots.md.

set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Where a run leaves its reports, slug lists and logs, and where the snapshots are archived.
CORPUS_DIR="${CORPUS_DIR:-$HOME/oneprofile-corpus}"
SNAPSHOT_DIR="${SNAPSHOT_DIR:-$HOME/oneprofile-snapshots}"

APP_URL="http://localhost:8080"
APP_LOG="$CORPUS_DIR/app.log"
APP_PID_FILE="$CORPUS_DIR/app.pid"

# The twelve crawls the raw corpus is built from, newest to oldest.
CRAWLS=(
	CC-MAIN-2026-39 CC-MAIN-2026-34 CC-MAIN-2026-30 CC-MAIN-2026-25
	CC-MAIN-2026-21 CC-MAIN-2026-17 CC-MAIN-2026-12 CC-MAIN-2026-08
	CC-MAIN-2026-04 CC-MAIN-2025-51 CC-MAIN-2025-47 CC-MAIN-2025-43
)

mkdir -p "$CORPUS_DIR"

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
	stop_app
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

# The application is run from its jar rather than from Maven, so that one process id is the run and
# stopping it stops the application. It is pointed at the database this script manages, so its own
# Docker Compose support stays out of the way.
start_app() {
	local jar="$REPO/target/backend-0.0.1-SNAPSHOT.jar"
	if [ ! -f "$jar" ]; then
		say "packaging the application"
		(cd "$REPO" && ./mvnw -q -DskipTests package)
	fi
	say "starting the application"
	SPRING_DOCKER_COMPOSE_ENABLED=false \
	SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oneprofile \
	SPRING_DATASOURCE_USERNAME=oneprofile \
	SPRING_DATASOURCE_PASSWORD=oneprofile \
		nohup java -jar "$jar" >>"$APP_LOG" 2>&1 &
	echo $! >"$APP_PID_FILE"
	wait_for_app
}

# Any HTTP answer at all means the application is serving; the root path has no handler, so what
# comes back is a 404 rather than a body.
wait_for_app() {
	local pid
	pid="$(cat "$APP_PID_FILE")"
	until curl -s -o /dev/null "$APP_URL/"; do
		if ! kill -0 "$pid" 2>/dev/null; then
			echo "The application died on startup; see $APP_LOG" >&2
			exit 1
		fi
		sleep 2
	done
	say "the application is up"
}

stop_app() {
	if [ ! -f "$APP_PID_FILE" ]; then
		return
	fi
	local pid
	pid="$(cat "$APP_PID_FILE")"
	rm -f "$APP_PID_FILE"
	if kill -0 "$pid" 2>/dev/null; then
		say "stopping the application"
		kill "$pid"
		while kill -0 "$pid" 2>/dev/null; do
			sleep 1
		done
	fi
}

# Calls one endpoint and writes its answer to a file. A phase is hours long and synchronous, so the
# request is made under nohup: the run survives the terminal it was started from. The elapsed time
# is written next to the answer, because the document reports each phase's wall clock.
post() {
	local path="$1" out="$2"
	local started ended
	started="$(date +%s)"
	say "POST $path"
	nohup curl -sS --fail-with-body --no-progress-meter -X POST "$APP_URL$path" -o "$out" &
	wait $! || {
		# Kept under another name, so that what is on disk as an answer is always an answer, and a
		# re-run is not mistaken for work already done. There may be nothing to keep: a request that
		# never reached the application wrote no body.
		mv "$out" "$out.failed" 2>/dev/null || true
		echo "POST $path failed; see $out.failed" >&2
		return 1
	}
	ended="$(date +%s)"
	echo "$((ended - started))" >"$out.seconds"
	say "POST $path took $((ended - started))s: $(cat "$out")"
}

# Every Greenhouse slug held, in slug order.
dump_slugs() {
	psql_dev -At -c "select slug from company where ats = 'GREENHOUSE' order by slug" >"$1"
}
