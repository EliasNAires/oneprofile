"""Shared plumbing for the body-pass loop of #11 (ADR-0012): the pile export, the labels Jev made,
and the rows earlier rounds drew. The request and what Jev has cost are jev.py's. Imported by the
scripts next to it, never run.
"""

import json
import subprocess
import sys
from pathlib import Path

import jev
from jev import SPEND_LIMIT, api_key, spend  # noqa: F401, the names score-body-round reads here

REPO = jev.REPO

CRITERION = REPO / "docs" / "engineering-role-body-criterion.md"

# One row per vacancy and criterion revision Jev labelled under. Committed: a label is paid for, made
# once and never remade; labels under an earlier revision stay, as that revision's measurement.
LABELS = jev.BODY_LABELS

# Every row a round drew, so a later round never draws it again.
DRAWN = REPO / "docs" / "measurements" / "body-rounds-drawn.tsv"

# The export the application writes (POST /pile-exports). It holds every description, so it lives
# outside the repository, next to the snapshot it was exported from: classified-2026-10-06.dump, its
# descriptions as stored segments one a line (#46). Round 3's flat export is archived with its snapshot.
PILE = Path.home() / "oneprofile-snapshots" / "pile-classified-2026-10-06.jsonl"

STATES = ("IN", "OUT", "UNKNOWN")

MODEL = jev.MODEL

QUESTION = ("Applying `criterion`, is the vacancy whose `title`, `title_reason` and `description` are given an "
            "engineering role?")

# The criterion's three states, worded for a vacancy read with its description.
OPTIONS = {
    "IN": "The vacancy is an engineering role.",
    "OUT": "It is not an engineering role.",
    "UNKNOWN": "The text does not say what the work is, or, for domain_ambiguity, which domain it is in.",
}


def read_pile(path=PILE):
    """Every vacancy of the export, by vacancy id."""
    with open(path) as lines:
        return {row["vacancy_id"]: row for row in map(json.loads, lines)}


def read_all_labels():
    """Every label Jev has made, under any criterion revision."""
    if not LABELS.exists():
        return []
    with open(LABELS) as lines:
        return [json.loads(line) for line in lines]


def read_labels(revision=None):
    """The labels Jev made under one criterion revision, the current one by default, by vacancy id."""
    revision = revision or criterion_revision()
    return {row["vacancy_id"]: row for row in read_all_labels() if row["criterion_revision"] == revision}


def append_label(row):
    with open(LABELS, "a") as labels:
        labels.write(json.dumps(row) + "\n")


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


def ask_jev(key, criterion, title, title_reason, description):
    """One labelling request. Returns Jev's response."""
    return jev.ask(key, {"title": title, "title_reason": title_reason, "description": description}, {
        "engineering_role": {
            "type": "choice",
            "instructions": {"criterion": criterion, "question": QUESTION},
            "criteria": OPTIONS,
        },
    })


def label(vacancy_ids, pile):
    """Labels every vacancy given that has no label under the current criterion revision, appending
    each label as it arrives, so a run that dies is resumed by running it again. Stops once every
    label stored has cost $4. Returns how many it labelled."""
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
    spent = spend()
    made = 0
    for vacancy_id in wanted:
        if spent >= SPEND_LIMIT:
            print(f"Stopped: Jev has cost ${spent:.4f}, the limit is ${SPEND_LIMIT:.2f}")
            break
        row = pile[vacancy_id]
        response = ask_jev(key, criterion, row["cleaned_title"], row["title_reason"], row["cleaned_description"])
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
