"""Shared plumbing for skill discovery (#41, ADR-0013, ADR-0015): name forms, the decision record, the
segment export, the keys and their kinds, the pieces recall asks about, and the labels Jev made of
candidates, of pieces in their segments and of key mentions, each made once under one criterion
revision and model and never remade. Imported by the scripts next to it, never run.
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

# One row per piece, the segment it was cut from, criterion revision and model Jev labelled under.
# Committed, like LABELS. Rows of revisions before 4 answered ADR-0013's open question on a segment.
RECALL_LABELS = jev.SKILL_RECALL_LABELS

# One row per mention of a plain key, and whether it means the key's skill there (ADR-0015).
KEY_LABELS = jev.SKILL_KEY_LABELS

# The criterion's categories are Jev's options for the category, so the category question reads only
# the Category table.
QUESTIONS = {
    "decision": ("Applying `criterion`, is `name`, as the job-ad `segments` given use it, a skill to keep or a "
                 "name to drop?"),
    "category": "Which one category best says what `name` is, taken as a skill?",
}

DECISION_OPTIONS = {
    "keep": "Keep: it names one specific technology an engineer would put on a CV, or an existing skill by "
            "another spelling.",
    "drop": "Drop: it is any of what the criterion drops, or the doubt between keep and drop remains.",
}


KEY_QUESTION = "In the job-ad `segment`, does `key` mean the technology `skill`?"

KEY_OPTIONS = {
    "yes": "Yes: there, the key names that technology, under its own name or a spelling of it.",
    "no": "No: there, the key is an ordinary word, part of another name, or another thing of the same name.",
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


def read_skill_rows():
    """The rows of skills.tsv, each as id, canonical name, category, keys (the canonical name first)
    and the keys that are context keys."""
    rows = []
    with open(SKILLS) as lines:
        for line in lines:
            identifier, canonical, category, aliases, *context = line.rstrip("\n").split("\t")
            keys = [canonical] + [alias for alias in aliases.split("|") if alias]
            rows.append((identifier, canonical, category, keys, set(filter(None, "".join(context).split("|")))))
    return rows


def read_keys(kind=None):
    """The canonical name of the skill each key of skills.tsv names, by the key's name form; only the
    `plain` or the `context` keys when a kind is given."""
    keys = {}
    for _, canonical, _, row_keys, context in read_skill_rows():
        for key in row_keys:
            if kind is None or (key in context) == (kind == "context"):
                keys[name_form(key)] = canonical
    return keys


# The rule that sets a key's kind (ADR-0015). A key is context when it is C or R, or an ordinary word:
# one word of the English word list that the corpus also writes in lower case in at least a tenth of
# its mentions. The word list alone holds names (Django, Jenkins, Kafka) that ads never use as words;
# the lower case share alone holds a tool's own spelling (curl). Plain keys are checked with Jev.
WORD_LIST = Path("/usr/share/dict/cracklib-small")
MIN_LOWER = 0.10


def lower_shares(words, lines):
    """For each word given, lowercased, the share of its mentions in the segment lines written in lower case."""
    pattern = re.compile(r"(?<![\w.+#-])([A-Za-z]\w*)(?![\w+#])")
    mentions, lower = {word: 0 for word in words}, {word: 0 for word in words}
    for line in lines:
        for found in pattern.finditer(line):
            word = found.group(1)
            if word.lower() in mentions:
                mentions[word.lower()] += 1
                lower[word.lower()] += word.islower()
    return {word: lower[word] / mentions[word] if mentions[word] else 0 for word in words}


def context_keys(keys, lines):
    """The keys the rule calls context, of those given."""
    ordinary = {word.strip().lower() for word in open(WORD_LIST)}
    single = {key.lower() for key in keys if " " not in key.strip() and key.lower() in ordinary}
    shares = lower_shares(single, lines)
    return {key for key in keys if key in ("C", "R") or shares.get(key.lower(), 0) >= MIN_LOWER}


class Names:
    """The skills of skills.tsv, found by the keys given and named by their canonical name, and the
    candidates of a run, found and named by their own name."""

    def __init__(self, keys, candidates):
        self.patterns = [(name_form(key).split(" ")[0], mention(key), canonical) for key, canonical in keys.items()]
        self.patterns += [(name_form(name).split(" ")[0], mention(name), name) for name in candidates]

    def named_in(self, text):
        """Every name found in the text, once, in alphabetical order."""
        lowered = text.lower()
        return sorted({name for word, pattern, name in self.patterns if word in lowered and pattern.search(text)},
                      key=str.lower)

    def spans_in(self, text):
        """Where each name is found in the text, as (start, end)."""
        lowered = text.lower()
        return sorted({(found.start(), found.end()) for word, pattern, _ in self.patterns if word in lowered
                       for found in pattern.finditer(text)})


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


def jev_text(text=None):
    """The short criterion Jev reads, the quoted text of the criterion's section What Jev reads."""
    text = CRITERION.read_text() if text is None else text
    section = text.split("\n## What Jev reads\n", 1)[1].split("\n## ", 1)[0]
    return "\n".join(line[2:] if line.startswith("> ") else "" for line in section.splitlines()
                     if line.startswith(">")).strip()


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
    text = CRITERION.read_text()
    criterion, revision, options = jev_text(text), criterion_revision(text), categories(text)
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


# Recall, in ADR-0015's four steps: generate the name-like pieces of a segment, filter them, have Jev
# judge each piece left in its segment, and score a segment missed when a kept piece was not found.
# The generator and the filter are frozen with the criterion, and never share the discovery rules' code.

# A token: a run of letters, digits and the symbols names are written with, never ending in a symbol
# other than + or #, nor starting with one other than the dot of .NET.
TOKEN = re.compile(r"\.?[^\W_](?:[\w+#./&'-]*[\w+#])?")

# A word written in lower case in at least this many vacancies is known vocabulary, and never a piece.
KNOWN_VACANCIES = 10

MAX_RUN = 3


def known_vocabulary(lines):
    """The lowercase words the corpus writes in lower case in at least KNOWN_VACANCIES vacancies."""
    vacancies = {}
    for line in lines:
        vacancy_id, text = line.rstrip("\n").split("\t", 1)
        for word in set(re.findall(r"(?<![\w.+#/-])[a-z]+(?![\w+#/])", text)):
            if len(vacancies.setdefault(word, set())) < KNOWN_VACANCIES:
                vacancies[word].add(vacancy_id)
    return {word for word, found in vacancies.items() if len(found) >= KNOWN_VACANCIES}


def pieces(segment, vocabulary):
    """The name-like pieces of a segment, as (text, start, end): capitalized words, tokens with a digit
    or a symbol, lowercase words outside the known vocabulary, and runs of up to three adjacent
    capitalized words or such tokens, never across punctuation. A token joined by slashes is cut at
    them (TypeScript/JavaScript), unless a part is a number or a single character (HTTP/2, I/O)."""
    tokens = []
    for found in TOKEN.finditer(segment):
        parts = found.group().split("/")
        if len(parts) > 1 and all(len(part) > 1 and not part.isdigit() for part in parts):
            start = found.start()
            for part in parts:
                if part:
                    tokens.append((part, start, start + len(part)))
                start += len(part) + 1
        else:
            tokens.append((found.group(), found.start(), found.end()))
    name_like = [token[0][:1].isupper() or token[0][:1] == "." or bool(re.search(r"[\d+#./&]", token[0][1:]))
                 for token in tokens]
    result = [token for token, like in zip(tokens, name_like)
              if like or (token[0].isalpha() and token[0].islower() and token[0] not in vocabulary)]
    for first in range(len(tokens)):
        for last in range(first + 1, min(first + MAX_RUN, len(tokens))):
            between = segment[tokens[last - 1][2]:tokens[last][1]]
            if not (name_like[last] and name_like[first]) or not re.fullmatch(r" +", between):
                break
            result.append((segment[tokens[first][1]:tokens[last][2]], tokens[first][1], tokens[last][2]))
    return result


class Filter:
    """ADR-0015's step 2: drops a piece the rules found, a plain key, a name in the decision record, a
    number, a single letter other than C and R, or a word on the ordinary list. A context key is never
    dropped by name. A piece inside the span of a name found is that name, and dropped with it."""

    def __init__(self, plain_forms, context_forms, decisions, ordinary):
        self.plain, self.context, self.decisions, self.ordinary = plain_forms, context_forms, decisions, ordinary

    def __call__(self, segment_pieces, found_spans):
        kept = {}
        for text, start, end in segment_pieces:
            form = name_form(text)
            if any(found_start <= start and end <= found_end for found_start, found_end in found_spans):
                continue
            if form not in self.context and (
                    form in self.plain or form in self.decisions or form in self.ordinary
                    or re.fullmatch(r"[\d\W_]+", form) or (len(form) == 1 and form not in "cr")):
                continue
            kept.setdefault(form, text)
        return kept


def read_piece_labels(revision=None):
    """Jev's verdicts on pieces under one criterion revision, the current one by default, and the pinned
    model, by name form and segment."""
    revision = revision or criterion_revision()
    if not RECALL_LABELS.exists():
        return {}
    with open(RECALL_LABELS) as lines:
        rows = [json.loads(line) for line in lines if line.strip()]
    return {(row["name_form"], row["segment"]): row for row in rows
            if row["criterion_revision"] == revision and row["model"] == jev.MODEL and "name_form" in row}


# A word joins the ordinary list once Jev has dropped it in this many different segments, and never kept it.
ORDINARY_DROPS = 5


def ordinary_list(labels):
    """The single words Jev has dropped in ORDINARY_DROPS different segments and kept in none."""
    drops, kept = {}, set()
    for (form, segment), row in labels.items():
        if " " in form:
            continue
        if row["decision"] == "keep":
            kept.add(form)
        else:
            drops.setdefault(form, set()).add(segment)
    return {form for form, segments in drops.items() if len(segments) >= ORDINARY_DROPS and form not in kept}


def label_pieces(wanted):
    """Asks Jev to keep or drop each piece given, a dict of `name` and `segment`, in its segment, unless it
    has under the current revision and model; the precision question with one segment. Appends the
    verdicts a chunk at a time and stops at the spend limit between chunks. Returns how many it made."""
    labels = read_piece_labels()
    blob = unchanged_criterion(labels.values())
    wanted = list({(name_form(row["name"]), row["segment"]): row for row in wanted
                   if (name_form(row["name"]), row["segment"]) not in labels}.values())
    print(f"{len(wanted)} pieces to judge")
    if not wanted:
        return 0
    key = jev.api_key()
    text = CRITERION.read_text()
    criterion, revision = jev_text(text), criterion_revision(text)
    spent = jev.spend()
    made = 0
    question = {"decision": {"type": "choice", "instructions": QUESTIONS["decision"], "criteria": DECISION_OPTIONS}}
    for chunk in jev.ask_many(key, wanted, lambda row: (
            {"criterion": criterion, "name": row["name"], "segments": [row["segment"]]}, question)):
        with open(RECALL_LABELS, "a") as ledger:
            for row, response in chunk:
                decision = response["answers"]["decision"]
                tokens = response["usage"]["input_tokens"]
                ledger.write(json.dumps({
                    "name_form": name_form(row["name"]),
                    "name": row["name"],
                    "segment": row["segment"],
                    "decision": decision["choice"],
                    "decision_probabilities": decision["probabilities"],
                    "criterion_revision": revision,
                    "criterion_blob": blob,
                    "model": response["model"],
                    "input_tokens": tokens,
                }, ensure_ascii=False) + "\n")
                spent += tokens * jev.DOLLARS_PER_INPUT_TOKEN
                made += 1
        print(f"  {made} judged, Jev has cost ${spent:.4f}")
        if spent >= jev.SPEND_LIMIT:
            print(f"Stopped: Jev has cost ${spent:.4f}, the limit is ${jev.SPEND_LIMIT:.2f}")
            break
    print(f"Judged {made}. Jev has cost ${spent:.4f} over every run of both issues.")
    return made


class Recall:
    """Recall over segments, each a dict of `segment` and `found`, the spans of the names the rules and
    the plain keys found in it. Run in two passes: pieces() gives what Jev has to judge, and score()
    reads its verdicts."""

    def __init__(self, lines):
        self.vocabulary = known_vocabulary(lines)
        labels = read_piece_labels()
        self.filter = Filter(set(read_keys("plain")), set(read_keys("context")), read_decisions(),
                             ordinary_list(labels))

    def pieces(self, row):
        """The pieces of a segment left after the filter, by name form."""
        return self.filter(pieces(row["segment"], self.vocabulary), row["found"])

    def score(self, rows):
        """Each segment's outcome, as (row, outcome, the pieces kept), the outcome `missed` when a piece
        left is kept, `covered` when the rules or the plain keys found a name and no piece is kept, and
        `none` when neither; or `unjudged` while a piece has no verdict."""
        labels = read_piece_labels()
        outcomes = []
        for row in rows:
            verdicts = {form: labels.get((form, row["segment"])) for form in self.pieces(row)}
            kept = sorted(text for form, text in self.pieces(row).items()
                          if verdicts[form] and verdicts[form]["decision"] == "keep")
            if any(verdict is None for verdict in verdicts.values()):
                outcome = "unjudged"
            elif kept:
                outcome = "missed"
            else:
                outcome = "covered" if row["found"] else "none"
            outcomes.append((row, outcome, kept))
        return outcomes


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
