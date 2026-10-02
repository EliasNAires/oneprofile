"""Shared plumbing for the body-pass loop of #11 (ADR-0012): the pile export, the labels Jev made,
the rows earlier rounds drew, and what Jev has cost. Imported by the scripts next to it, never run.
"""

import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

CRITERION = REPO / "docs" / "engineering-role-criterion.md"

# One row per vacancy Jev labelled. Committed: a label is paid for, made once and never remade.
LABELS = REPO / "docs" / "measurements" / "body-labels-jev.jsonl"

# Every row a round drew, so a later round never draws it again.
DRAWN = REPO / "docs" / "measurements" / "body-rounds-drawn.tsv"

# The export the application writes (POST /pile-exports). It holds every description, so it lives
# outside the repository, next to the snapshot it was exported from.
PILE = Path.home() / "oneprofile-snapshots" / "pile-raw-2026-09-20.jsonl"

STATES = ("IN", "OUT", "UNKNOWN")

JEV = "https://api.typesafe.ai/v1/systemone"

MODEL = "jev-1.13.0"

QUESTION = "Applying `criterion`, is the vacancy whose `title` and `description` are given an engineering role?"

# The criterion's three states, worded for a vacancy read with its description.
OPTIONS = {
    "IN": "The vacancy is an engineering role.",
    "OUT": "It is not an engineering role.",
    "UNKNOWN": "What the vacancy says does not carry enough to decide.",
}

# Jev answers 429 when the rate limit is hit and 529 when it is overloaded; both are retried.
RETRIED = (429, 529)

ATTEMPTS = 8

DOLLARS_PER_INPUT_TOKEN = 0.042 / 1_000_000

SPEND_LIMIT = 4.00


def read_pile(path=PILE):
    """Every vacancy of the export, by vacancy id."""
    with open(path) as lines:
        return {row["vacancy_id"]: row for row in map(json.loads, lines)}


def read_labels():
    """Every label Jev has made, by vacancy id."""
    if not LABELS.exists():
        return {}
    with open(LABELS) as lines:
        return {row["vacancy_id"]: row for row in map(json.loads, lines)}


def append_label(row):
    with open(LABELS, "a") as labels:
        labels.write(json.dumps(row) + "\n")


def spend(labels):
    """What every label stored has cost, in dollars, from the usage Jev reported for it."""
    return sum(row["input_tokens"] for row in labels.values()) * DOLLARS_PER_INPUT_TOKEN


def criterion_revision():
    """The git blob hash of the criterion as it stands on disk, committed or not."""
    return subprocess.run(["git", "hash-object", str(CRITERION)], check=True, capture_output=True,
                          text=True).stdout.strip()


def read_drawn():
    """Every vacancy id an earlier round drew, with the round that drew it."""
    if not DRAWN.exists():
        return {}
    with open(DRAWN) as lines:
        rows = [line.rstrip("\n").split("\t") for line in lines if line.strip() and not line.startswith("round")]
    return {int(vacancy_id): round_name for round_name, vacancy_id in rows}


def record_drawn(round_number, vacancy_ids):
    new = not DRAWN.exists()
    with open(DRAWN, "a") as drawn:
        if new:
            drawn.write("round\tvacancy_id\n")
        for vacancy_id in sorted(vacancy_ids):
            drawn.write(f"{round_number}\t{vacancy_id}\n")


def read_ids(path):
    """A file of vacancy ids, one a line."""
    with open(path) as lines:
        return [int(line) for line in lines if line.strip()]


def api_key():
    """TYPESAFE_API_KEY from the repository's .env. Never printed."""
    for line in (REPO / ".env").read_text().splitlines():
        name, _, value = line.partition("=")
        if name.strip() == "TYPESAFE_API_KEY":
            return value.strip().strip('"').strip("'")
    sys.exit("TYPESAFE_API_KEY is not in .env")


def ask_jev(key, criterion, title, description):
    """One labelling request. Returns Jev's response, retrying 429 and 529 with backoff."""
    body = json.dumps({
        "model": MODEL,
        "state": {"title": title, "description": description},
        "questions": {
            "engineering_role": {
                "type": "choice",
                "instructions": {"criterion": criterion, "question": QUESTION},
                "criteria": OPTIONS,
            },
        },
    }).encode()
    for attempt in range(ATTEMPTS):
        request = urllib.request.Request(JEV, data=body, method="POST", headers={
            "Authorization": f"Bearer {key}", "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            if error.code not in RETRIED or attempt == ATTEMPTS - 1:
                sys.exit(f"Jev answered {error.code}: {error.read().decode(errors='replace')}")
            wait = float(error.headers.get("Retry-After") or 2 ** attempt)
            print(f"  Jev answered {error.code}, retrying in {wait:.0f}s", file=sys.stderr)
            time.sleep(wait)


def label(vacancy_ids, pile):
    """Labels every vacancy given that has no label yet, appending each label as it arrives, so a
    run that dies is resumed by running it again. Stops once every label stored has cost $4.
    Returns how many it labelled."""
    labels = read_labels()
    wanted = [vacancy_id for vacancy_id in dict.fromkeys(vacancy_ids) if vacancy_id not in labels]
    missing = [vacancy_id for vacancy_id in wanted if vacancy_id not in pile]
    if missing:
        sys.exit(f"{len(missing)} vacancy ids are not in the pile export, the first {missing[0]}")
    print(f"{len(vacancy_ids) - len(wanted)} already labelled, {len(wanted)} to label")
    if not wanted:
        return 0
    key = api_key()
    criterion = CRITERION.read_text()
    revision = criterion_revision()
    spent = spend(labels)
    made = 0
    for vacancy_id in wanted:
        if spent >= SPEND_LIMIT:
            print(f"Stopped: Jev has cost ${spent:.4f}, the limit is ${SPEND_LIMIT:.2f}")
            break
        row = pile[vacancy_id]
        response = ask_jev(key, criterion, row["cleaned_title"], row["cleaned_description"])
        if response["model"] != MODEL:
            sys.exit(f"Jev answered as {response['model']}, not the pinned {MODEL}")
        answer = response["answers"]["engineering_role"]
        tokens = response["usage"]["input_tokens"]
        append_label({
            "vacancy_id": vacancy_id,
            "label": answer["choice"],
            "probabilities": answer["probabilities"],
            "criterion_revision": revision,
            "model": response["model"],
            "input_tokens": tokens,
        })
        spent += tokens * DOLLARS_PER_INPUT_TOKEN
        made += 1
        print(f"  {vacancy_id}\t{answer['choice']}\t{tokens} tokens\t{row['cleaned_title']}")
    print(f"Labelled {made}. Jev has cost ${spent:.4f} over every run.")
    return made
