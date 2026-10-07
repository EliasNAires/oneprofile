"""Shared plumbing for skill discovery (#41, ADR-0013): name forms, the decision record, the segment
export, the labels Jev made of candidates and its answers on the segments recall is scored on, each
made once under one criterion revision and model and never remade. Imported by the scripts next to
it, never run.
"""

import bisect
import json
import math
import random
import re
import subprocess
import sys
from pathlib import Path

import jev

REPO = jev.REPO

SKILLS = REPO / "src" / "main" / "resources" / "taxonomy" / "skills.tsv"

CRITERION = REPO / "docs" / "skill-criterion.md"

DECISIONS = REPO / "src" / "main" / "resources" / "taxonomy" / "decisions.tsv"

# One row per name form, criterion revision and model Jev labelled under, with what it read.
# Committed: a label is paid for, made once and never remade.
LABELS = jev.SKILL_LABELS

# One row per segment, list of names found in it, criterion revision and model Jev answered under.
# Committed, like LABELS.
RECALL_LABELS = jev.SKILL_RECALL_LABELS

QUESTIONS = {
    "decision": ("Applying `criterion`, is `name`, seen in the job-ad `segments` given, a skill to keep or a "
                 "name to drop?"),
    "category": ("Applying the Category section of `criterion`, which one category best says what `name` is, "
                 "taken as a skill?"),
}

DECISION_OPTIONS = {
    "keep": "Keep: it names one specific technology an engineer would put on a CV, or an existing skill by "
            "another spelling.",
    "drop": "Drop: it is any of what the criterion drops, or the doubt between keep and drop remains.",
}


RECALL_QUESTION = ("Applying `criterion`, does the job-ad `segment` name a software skill, as the criterion reads "
                   "a skill, that is not in `found`, under its own name or another spelling?")

RECALL_OPTIONS = {
    "none": "None: the segment names no software skill at all.",
    "covered": "Covered: the segment names at least one software skill, and every one it names is in `found`.",
    "missed": "Missed: the segment names at least one software skill that is not in `found`.",
}


def name_form(name):
    """The name as discovery compares it: lowercased, each run of spaces and hyphens between two other
    characters one space, the ends trimmed, every other character kept."""
    return re.sub(r"(?<=[^\s-])[\s-]+(?=[^\s-])", " ", name.strip().lower())


def mention(name):
    """A pattern finding the name in text in any case, its spaces and hyphens interchangeable, with no
    letter or digit right before or after it, nor the + or # that would make it a longer name (C in C++)."""
    words = [re.escape(word) for word in name_form(name).split(" ")]
    return re.compile(r"(?<![^\W_])" + r"[\s-]+".join(words) + r"(?![^\W_]|[+#])", re.IGNORECASE)


class Segments:
    """The segments of a corpus, each line a vacancy id, a tab and one segment's text, searched for
    where a name is mentioned."""

    def __init__(self, lines):
        self.lines = [line.rstrip("\n").split("\t", 1) for line in lines]
        # Searched lowercased for the name's first word, which is fast, and each line it is in is then
        # matched in full. Lowercasing can lengthen a line (İ), so lines are found by their newlines.
        self.lowered = "\n".join(text for _, text in self.lines).lower()
        self.starts = [0] + [newline.end() for newline in re.finditer("\n", self.lowered)]
        self.found = {}

    def mentions(self, name):
        """The first segment mentioning the name in each vacancy that does, by vacancy id."""
        if name not in self.found:
            pattern, word = mention(name), name_form(name).split(" ")[0]
            found, position = {}, self.lowered.find(word)
            while position >= 0:
                line = bisect.bisect_right(self.starts, position) - 1
                vacancy_id, text = self.lines[line]
                if vacancy_id not in found and pattern.search(text):
                    found[vacancy_id] = text
                position = self.lowered.find(word, self.starts[line + 1]) if line + 1 < len(self.starts) else -1
            self.found[name] = found
        return self.found[name]

    def document_frequency(self, name):
        return len(self.mentions(name))

    def examples(self, name, count, seed):
        """Up to `count` segments mentioning the name, each from another vacancy and with another text,
        since one ad is often posted as several vacancies, drawn with the seed, as (vacancy id, text)."""
        found = self.mentions(name)
        first = {}
        for vacancy_id in sorted(found):
            first.setdefault(found[vacancy_id], vacancy_id)
        return [(first[text], text) for text in shuffled(first, seed)[:count]]


# The requirement text of the IN vacancies and the pile (ADR-0012), English and Spanish, boilerplate
# and headings left out, one segment a line after its vacancy id, and each vacancy's company. Exported
# once from the development database, which has to hold the snapshot, into files next to it.
SNAPSHOT = "classified-2026-10-06"

SEGMENTS = Path.home() / "oneprofile-snapshots" / f"skill-segments-{SNAPSHOT}.tsv"

SEGMENTS_EXPORT = """
select n.vacancy_id, regexp_replace(e->>'text', '\\s+', ' ', 'g')
from normalized_vacancy n
join vacancy v on v.id = n.vacancy_id,
jsonb_array_elements(n.description_segments) e
where v.language in ('en', 'es')
and ((n.classification_state = 'IN' and n.classification_reason is null)
     or n.classification_reason in ('DOMAIN_AMBIGUITY', 'SCOPE_AMBIGUITY'))
and e->>'kind' <> 'HEADING'
and not coalesce((e->>'boilerplate')::boolean, false)
order by n.vacancy_id
"""

COMPANIES = Path.home() / "oneprofile-snapshots" / f"skill-companies-{SNAPSHOT}.tsv"

COMPANIES_EXPORT = "select id, company_id from vacancy order by id"


def export(path, sql):
    """The lines of an export, made from the development database if the file is not there yet."""
    if not path.exists():
        print(f"Exporting {path}", file=sys.stderr)
        with open(path, "w") as out:
            subprocess.run(["docker", "compose", "--project-directory", str(REPO), "exec", "-T", "postgres", "psql",
                            "-v", "ON_ERROR_STOP=1", "-U", "oneprofile", "-d", "oneprofile", "-At", "-F", "\t",
                            "-c", sql], check=True, stdout=out)
    with open(path) as lines:
        return lines.readlines()


def read_segment_lines():
    return export(SEGMENTS, SEGMENTS_EXPORT)


def read_segments():
    return Segments(read_segment_lines())


def read_companies():
    """Each vacancy's company, by vacancy id."""
    return dict(line.rstrip("\n").split("\t") for line in export(COMPANIES, COMPANIES_EXPORT))


def read_keys():
    """The canonical name of the skill each key of skills.tsv names, by the key's name form."""
    keys = {}
    with open(SKILLS) as lines:
        for line in lines:
            _, canonical, _, aliases = line.rstrip("\n").split("\t")
            for key in [canonical] + aliases.split("|"):
                if key:
                    keys[name_form(key)] = canonical
    return keys


class Names:
    """The skills of skills.tsv, found by any of their keys and named by their canonical name, and the
    candidates of a run, found and named by their own name."""

    def __init__(self, keys, candidates):
        self.patterns = [(name_form(key).split(" ")[0], mention(key), canonical) for key, canonical in keys.items()]
        self.patterns += [(name_form(name).split(" ")[0], mention(name), name) for name in candidates]

    def named_in(self, text):
        """Every name found in the text, once, in alphabetical order."""
        lowered = text.lower()
        return sorted({name for word, pattern, name in self.patterns if word in lowered and pattern.search(text)},
                      key=str.lower)


def recall(answers):
    """How many of the segments answered named no skill the list missed, of how many named one."""
    bearing = [answer for answer in answers if answer != "none"]
    return sum(answer == "covered" for answer in bearing), len(bearing)


def criterion_revision(text=None):
    """The revision the criterion's own marker gives, `Revision N` near its top."""
    text = CRITERION.read_text() if text is None else text
    found = re.search(r"^Revision (\d+)\b", text, re.MULTILINE)
    if not found:
        sys.exit(f"{CRITERION.name} carries no revision marker")
    return found.group(1)


def criterion_blob():
    """The git blob hash of the criterion as it stands on disk, committed or not."""
    return subprocess.run(["git", "hash-object", str(CRITERION)], check=True, capture_output=True,
                          text=True).stdout.strip()


def categories(text=None):
    """The criterion's categories, each with what it is for, read from its Category table."""
    text = CRITERION.read_text() if text is None else text
    return dict(re.findall(r"^\| `(\w+)` \| (.+?) \|$", text, re.MULTILINE))


def read_decisions():
    """The decision record, by name form."""
    with open(DECISIONS) as lines:
        header, *rows = [line.rstrip("\n").split("\t") for line in lines if line.strip()]
    return {row[0]: dict(zip(header, row)) for row in rows}


def read_all_labels():
    """Every label Jev has made of a candidate, under any revision and model."""
    if not LABELS.exists():
        return []
    with open(LABELS) as lines:
        return [json.loads(line) for line in lines if line.strip()]


def read_labels(revision=None):
    """The labels made under one criterion revision, the current one by default, and the pinned model,
    by name form."""
    revision = revision or criterion_revision()
    return {row["name_form"]: row for row in read_all_labels()
            if row["criterion_revision"] == revision and row["model"] == jev.MODEL}


def label(candidates):
    """Labels every candidate given, each a dict of `name` and `segments`, whose name form has no label
    under the current revision and model, appending each label as it arrives, so a run that dies is
    resumed by running it again. Stops once Jev has cost the spend limit over both issues. Returns how many it made."""
    labels = read_labels()
    blob = unchanged_criterion(labels.values())
    wanted = list({name_form(candidate["name"]): candidate for candidate in candidates
                   if name_form(candidate["name"]) not in labels}.values())
    print(f"{len(candidates) - len(wanted)} already labelled, {len(wanted)} to label")
    if not wanted:
        return 0
    key = jev.api_key()
    criterion = CRITERION.read_text()
    revision = criterion_revision(criterion)
    options = categories(criterion)
    spent = jev.spend()
    made = 0
    for candidate in wanted:
        if spent >= jev.SPEND_LIMIT:
            print(f"Stopped: Jev has cost ${spent:.4f}, the limit is ${jev.SPEND_LIMIT:.2f}")
            break
        response = jev.ask(key, {"criterion": criterion, "name": candidate["name"], "segments": candidate["segments"]}, {
            "decision": {"type": "choice", "instructions": QUESTIONS["decision"], "criteria": DECISION_OPTIONS},
            "category": {"type": "choice", "instructions": QUESTIONS["category"], "criteria": options},
        })
        decision, category = response["answers"]["decision"], response["answers"]["category"]
        tokens = response["usage"]["input_tokens"]
        with open(LABELS, "a") as ledger:
            ledger.write(json.dumps({
                "name_form": name_form(candidate["name"]),
                "name": candidate["name"],
                "segments": candidate["segments"],
                "decision": decision["choice"],
                "decision_probabilities": decision["probabilities"],
                "category": category["choice"],
                "category_probabilities": category["probabilities"],
                "criterion_revision": revision,
                "criterion_blob": blob,
                "model": response["model"],
                "input_tokens": tokens,
            }, ensure_ascii=False) + "\n")
        spent += tokens * jev.DOLLARS_PER_INPUT_TOKEN
        made += 1
        print(f"  {decision['choice']:<4} {category['choice']:<10} {tokens:5} tokens  {candidate['name']}")
    print(f"Labelled {made}. Jev has cost ${spent:.4f} over every run of both issues.")
    return made


def unchanged_criterion(labels):
    """The criterion's blob, once it is the one every label of its revision was made under."""
    blob = criterion_blob()
    if {row["criterion_blob"] for row in labels} - {blob}:
        sys.exit(f"{CRITERION.name} changed since labels were made under revision {criterion_revision()}: "
                 "a changed criterion is a new revision, so raise its Revision marker")
    return blob


def recall_key(segment, found):
    return segment, tuple(found)


def read_recall_labels(revision=None):
    """Jev's answers on segments under one criterion revision, the current one by default, and the
    pinned model, by segment and the names that were found in it."""
    revision = revision or criterion_revision()
    if not RECALL_LABELS.exists():
        return {}
    with open(RECALL_LABELS) as lines:
        rows = [json.loads(line) for line in lines if line.strip()]
    return {recall_key(row["segment"], row["found"]): row for row in rows
            if row["criterion_revision"] == revision and row["model"] == jev.MODEL}


def label_recall(segments):
    """Asks Jev of every segment given, each a dict of `vacancy_id`, `segment` and `found`, whether it
    names a software skill `found` misses, unless it has answered for that segment and list under the
    current revision and model. Like label(), it appends each answer as it arrives and stops at the
    spend limit. Returns how many it made."""
    labels = read_recall_labels()
    blob = unchanged_criterion(labels.values())
    wanted = list({recall_key(row["segment"], row["found"]): row for row in segments
                   if recall_key(row["segment"], row["found"]) not in labels}.values())
    print(f"{len(segments) - len(wanted)} segments already answered, {len(wanted)} to ask")
    if not wanted:
        return 0
    key = jev.api_key()
    criterion = CRITERION.read_text()
    revision = criterion_revision(criterion)
    spent = jev.spend()
    made = 0
    for row in wanted:
        if spent >= jev.SPEND_LIMIT:
            print(f"Stopped: Jev has cost ${spent:.4f}, the limit is ${jev.SPEND_LIMIT:.2f}")
            break
        response = jev.ask(key, {"criterion": criterion, "segment": row["segment"], "found": row["found"]}, {
            "recall": {"type": "choice", "instructions": RECALL_QUESTION, "criteria": RECALL_OPTIONS},
        })
        answer = response["answers"]["recall"]
        tokens = response["usage"]["input_tokens"]
        with open(RECALL_LABELS, "a") as ledger:
            ledger.write(json.dumps({
                "vacancy_id": row["vacancy_id"],
                "segment": row["segment"],
                "found": row["found"],
                "answer": answer["choice"],
                "probabilities": answer["probabilities"],
                "criterion_revision": revision,
                "criterion_blob": blob,
                "model": response["model"],
                "input_tokens": tokens,
            }, ensure_ascii=False) + "\n")
        spent += tokens * jev.DOLLARS_PER_INPUT_TOKEN
        made += 1
        print(f"  {answer['choice']:<7} {tokens:5} tokens  {row['segment'][:80]}")
    print(f"Asked {made}. Jev has cost ${spent:.4f} over every run of both issues.")
    return made


# The check of Jev against the #38 and #40 reviews (#52).

# The criterion's drop reasons, each with the pattern that finds it in a review note. The reviewers wrote
# notes freely, and a note naming two reasons is read by the one it names first, usually why the name
# was dropped ("office software; Teams ordinary word"); a tie goes to the reason listed first.
REASONS = (
    ("single letter", r"single letter"),
    ("homonym", r"homonym|collision|collides|\bmeans\b|\bmatches (mean|are|belong)|not meant"),
    ("consumer, office or browser software", r"consumer|office|browser|productivity|video|game"),
    ("not hired for", r"not an engineering|marketing|expense"),
    ("ambiguous abbreviation", r"ambiguous|abbreviation"),
    ("key noise", r"noise"),
    ("generic concept", r"generic"),
    ("ordinary word", r"ordinary"),
    ("obscure or historical", r"obscure|historical|esoteric|low signal|rare\b"),
    ("not a technology", r"fragment|company|vendor|umbrella|product line|place|person|file format|project name"
                         r"|methodology|feature|tier|redundant|technology|not a (skill|product)|not separate"),
)


def drop_reason(note):
    """The criterion's reason a review note gives for a drop, `other` when it gives none."""
    first = None
    for reason, pattern in REASONS:
        found = re.search(pattern, note, re.IGNORECASE)
        if found and (first is None or found.start() < first[1]):
            first = (reason, found.start())
    return first[0] if first else "other"


def allocate(sizes, size):
    """Splits a sample of `size` across strata of the sizes given, in proportion to them by largest
    remainder, every stratum getting at least one row and none more than it holds."""
    size = min(size, sum(sizes.values()))
    total = sum(sizes.values())
    shares = {stratum: size * count / total for stratum, count in sizes.items()}
    counts = {stratum: min(sizes[stratum], max(1, int(share))) for stratum, share in shares.items()}
    while sum(counts.values()) > size:
        counts[max((stratum for stratum in counts if counts[stratum] > 1),
                   key=lambda stratum: counts[stratum] - shares[stratum])] -= 1
    while sum(counts.values()) < size:
        counts[max((stratum for stratum in counts if counts[stratum] < sizes[stratum]),
                   key=lambda stratum: shares[stratum] - counts[stratum])] += 1
    return counts


def shuffled(items, seed):
    """The items in an order fixed by the seed, so a larger draw begins with every smaller one."""
    items = sorted(items)
    random.Random(seed).shuffle(items)
    return items


Z = 1.96


def wilson(agreed, total):
    """The 95% Wilson interval of a proportion."""
    share = agreed / total
    centre = (share + Z * Z / (2 * total)) / (1 + Z * Z / total)
    half = Z * math.sqrt(share * (1 - share) / total + Z * Z / (4 * total * total)) / (1 + Z * Z / total)
    return centre - half, centre + half
