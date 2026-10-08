"""Shared plumbing for every script that calls Jev (ADR-0012, ADR-0013): the request, the key, and
what Jev has cost across #11 and #41, which share one account. Imported by the scripts next to it,
never run.
"""

import concurrent.futures
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

URL = "https://api.typesafe.ai/v1/systemone"

MODEL = "jev-1.13.0"

# The file each loop appends Jev's labels to, each row carrying the input_tokens it cost.
BODY_LABELS = REPO / "docs" / "measurements" / "body-labels-jev.jsonl"

SKILL_LABELS = REPO / "docs" / "measurements" / "skill-labels-jev.jsonl"

SKILL_RECALL_LABELS = REPO / "docs" / "measurements" / "skill-recall-labels-jev.jsonl"

SKILL_KEY_LABELS = REPO / "docs" / "measurements" / "skill-key-labels-jev.jsonl"

# Written by the stoplist run of #60, since removed; kept so its spend still counts.
STOPLIST_LABELS = REPO / "docs" / "measurements" / "stoplist-labels-jev.jsonl"

LEDGERS = (BODY_LABELS, SKILL_LABELS, SKILL_RECALL_LABELS, SKILL_KEY_LABELS, STOPLIST_LABELS)

# Jev answers 429 when the rate limit is hit and 529 when it is overloaded; both are retried.
RETRIED = (429, 529)

ATTEMPTS = 8

DOLLARS_PER_INPUT_TOKEN = 0.042 / 1_000_000

SPEND_LIMIT = 4.00


def api_key():
    """TYPESAFE_API_KEY from the repository's .env. Never printed."""
    for line in (REPO / ".env").read_text().splitlines():
        name, _, value = line.partition("=")
        if name.strip() == "TYPESAFE_API_KEY":
            return value.strip().strip('"').strip("'")
    sys.exit("TYPESAFE_API_KEY is not in .env")


def ask(key, state, questions):
    """One request to the pinned model. Returns Jev's response, retrying 429 and 529 with backoff."""
    body = json.dumps({"model": MODEL, "state": state, "questions": questions}).encode()
    for attempt in range(ATTEMPTS):
        request = urllib.request.Request(URL, data=body, method="POST", headers={
            "Authorization": f"Bearer {key}", "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                answer = json.load(response)
        except urllib.error.HTTPError as error:
            if error.code not in RETRIED or attempt == ATTEMPTS - 1:
                sys.exit(f"Jev answered {error.code}: {error.read().decode(errors='replace')}")
            wait = float(error.headers.get("Retry-After") or 2 ** attempt)
            print(f"  Jev answered {error.code}, retrying in {wait:.0f}s", file=sys.stderr)
            time.sleep(wait)
            continue
        if answer["model"] != MODEL:
            sys.exit(f"Jev answered as {answer['model']}, not the pinned {MODEL}")
        return answer


# Requests in flight at once, and how many are sent before the spend is checked again.
WORKERS = 8
CHUNK = 200


def ask_many(key, items, request):
    """Asks Jev for each item, `request(item)` giving its state and questions, WORKERS at a time. Yields
    each chunk of CHUNK items as [(item, response)], in the items' order, so the caller can stop
    between chunks."""
    with concurrent.futures.ThreadPoolExecutor(WORKERS) as pool:
        for start in range(0, len(items), CHUNK):
            chunk = items[start:start + CHUNK]
            yield list(zip(chunk, pool.map(lambda item: ask(key, *request(item)), chunk)))


def spend():
    """What every label stored in any ledger has cost, in dollars, from the usage Jev reported."""
    tokens = 0
    for ledger in LEDGERS:
        if ledger.exists():
            with open(ledger) as lines:
                tokens += sum(json.loads(line)["input_tokens"] for line in lines if line.strip())
    return tokens * DOLLARS_PER_INPUT_TOKEN
