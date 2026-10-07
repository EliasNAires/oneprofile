#!/usr/bin/env python3
"""Proposes the candidates of skill discovery (ADR-0013): the name-like phrases of the segments of IN
and the pile that are neither a key of skills.tsv nor in the decision record.

    scripts/skill_discovery.py <out.tsv>

There is no dictionary to tell a name from a word, so the corpus tells: a name is written
capitalized in the middle of a sentence, or carries a digit or a symbol, and sits near a skill the
taxonomy already has. Ordinary words fail the first test, and company, place and people names, which
are capitalized too, fail the second.

Round 0 is #40's taxonomy_mine.py, reading the segments cleaning stored (#46) instead of whole
descriptions, and their words as Segment.tokens() reads them. A segment's first word starts a
sentence, and no phrase runs from one segment into the next. Spellings are grouped by name form,
the decision record's, so a hyphen inside a word counts as a space (Spring-Boot is spring boot), and
#40's check that a phrase keeps one key token a word is gone with the key it checked.

Reads the segment export skillloop makes from the development database, which has to hold
classified-2026-10-06.dump the first time.
"""

import collections
import re
import sys
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import skillloop  # noqa: E402

MAX_WORDS = 3
MIN_DF = 3
MIN_COMPANIES = 3  # one company's boilerplate and its own name are not skills
MIN_CAPITALIZED = 0.4  # pytest is capitalized in 45% of its mid-sentence occurrences
NEAR = 6  # words either side
MIN_NEAR = 0.2  # 95% of the skills in skills.tsv sit near another more often than this

# Segment.WORD: letters and digits of any script, holding together across the apostrophes, full stops
# and hyphens inside it, and keeping the + and # that end C++, C# or 5+.
WORD = re.compile(r"[^\W_](?:(?:[^\W_]|['’.+#-])*(?:[^\W_]|[+#]))?")
SENTENCE_BREAK = re.compile(r"[.!?]\s|[:;•*|–—]|(^|\s)-(\s|$)")
MARKED = re.compile(r"[0-9+#.]")


class Word:
    __slots__ = ("text", "form", "starts", "joined")

    def __init__(self, text, starts, joined):
        self.text, self.starts, self.joined = text, starts, joined
        lowered = text.lower()
        self.form = skillloop.name_form(lowered) if "-" in lowered else lowered


def words(segments):
    """The words of a vacancy's segments as written, each marked with whether it starts a sentence and
    whether only spaces part it from the word before."""
    out = []
    for segment in segments:
        end = None
        for found in WORD.finditer(segment):
            if end is None:
                out.append(Word(found.group(), True, False))
            else:
                gap = segment[end:found.start()]
                out.append(Word(found.group(), bool(SENTENCE_BREAK.search(gap)), not gap.strip()))
            end = found.end()
    return out


def written_as_name(text):
    """Every word capitalized or carrying a digit or symbol."""
    return all(word != word.lower() or MARKED.search(word) for word in text.split())


def phrases(ws):
    """(start, end) of every run of up to MAX_WORDS words joined by spaces within a sentence."""
    for i in range(len(ws)):
        for j in range(i + 1, min(i + MAX_WORDS, len(ws)) + 1):
            if j > i + 1 and (ws[j - 1].starts or not ws[j - 1].joined):
                break
            yield i, j


def mine(docs, covered, known):
    """The name-like phrases of the docs, each a company and its vacancy's segments, whose name form is
    not in covered. known holds the name forms of the keys of the skills already in the taxonomy."""
    said = collections.Counter()  # word -> mid-sentence occurrences, and capitalized ones
    candidates = set()
    for _, segments in docs:
        ws = words(segments)
        for w in ws:
            if not w.starts:
                for part in w.form.split(" "):
                    said[part] += 1
                    said[part, True] += written_as_name(w.text)
        for i, j in phrases(ws):
            if ws[i].starts:
                continue
            form = " ".join(w.form for w in ws[i:j])
            if form not in covered and written_as_name(" ".join(w.text for w in ws[i:j])):
                candidates.add(form)

    longest = max(len(form.split(" ")) for form in known)
    firsts = {form.split(" ")[0] for form in known}
    stats = collections.defaultdict(lambda: {"df": 0, "companies": set(), "said": 0, "capitalized": 0,
                                             "near": 0, "forms": collections.Counter()})
    for company, segments in docs:
        ws = words(segments)
        forms = [w.form for w in ws]
        present = {" ".join(forms[i:j]) for i in range(len(forms))
                   for j in range(i + 1, min(i + MAX_WORDS, len(forms)) + 1)} & candidates
        for form in present:
            stats[form]["df"] += 1
            stats[form]["companies"].add(company)
        skills = [(i, j) for i in range(len(forms)) if forms[i].split(" ")[0] in firsts
                  for j in range(i + 1, min(i + longest, len(forms)) + 1) if " ".join(forms[i:j]) in known]
        for i, j in phrases(ws):
            form = " ".join(forms[i:j])
            if ws[i].starts or form not in present:
                continue
            s = stats[form]
            text = " ".join(w.text for w in ws[i:j])
            s["said"] += 1
            s["capitalized"] += written_as_name(text)
            s["forms"][text] += 1
            s["near"] += any((b <= i or a >= j) and a <= j + NEAR and b >= i - NEAR for a, b in skills)

    def name_like(word):
        return said[word, True] / said[word] >= MIN_CAPITALIZED

    mined = []
    for form, s in stats.items():
        if (s["df"] >= MIN_DF and len(s["companies"]) >= MIN_COMPANIES
                and s["capitalized"] / s["said"] >= MIN_CAPITALIZED
                and s["near"] / s["said"] >= MIN_NEAR
                and any(name_like(word) for word in form.split(" "))):
            mined.append({"name": s["forms"].most_common(1)[0][0], "df": s["df"],
                          "companies": len(s["companies"]), "capitalized": s["capitalized"] / s["said"],
                          "near": s["near"] / s["said"], "forms": dict(s["forms"])})
    return mined


def write_candidates(path, candidates, corpus):
    with open(path, "w") as out:
        out.write(f"# Candidates of skill discovery, written by scripts/skill_discovery.py from {corpus}.\n"
                  "# capitalized (written as a name) and near are shares of the mid-sentence occurrences; each\n"
                  "# form=N counts a spelling mid-sentence.\n"
                  "df\tname\tcompanies\tcapitalized\tnear\tforms\n")
        for c in sorted(candidates, key=lambda c: (-c["df"], c["name"].lower())):
            forms = "|".join(f"{text}={n}" for text, n in sorted(c["forms"].items(), key=lambda x: -x[1]))
            out.write(f"{c['df']}\t{c['name']}\t{c['companies']}\t{c['capitalized']:.2f}\t{c['near']:.2f}\t{forms}\n")


def read_candidates(path):
    """The candidates a run wrote, highest document frequency first. A name never starts with #, which
    no word does, so the comment lines are told apart by it."""
    with open(path) as lines:
        header, *rows = [line.rstrip("\n").split("\t") for line in lines if not line.startswith("#")]
    candidates = []
    for row in (dict(zip(header, row)) for row in rows):
        forms = dict(form.rsplit("=", 1) for form in row["forms"].split("|"))
        candidates.append({"name": row["name"], "df": int(row["df"]), "companies": int(row["companies"]),
                           "forms": {text: int(n) for text, n in forms.items()}})
    return candidates


def corpus():
    """Each vacancy of the segment export as its company and its segments."""
    companies = skillloop.read_companies()
    docs = collections.defaultdict(list)
    for line in skillloop.read_segment_lines():
        vacancy_id, text = line.rstrip("\n").split("\t", 1)
        docs[vacancy_id].append(text)
    return [(companies[vacancy_id], segments) for vacancy_id, segments in docs.items()]


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__.strip())
    known = set(skillloop.read_keys())
    covered = known | set(skillloop.read_decisions())
    docs = corpus()
    print(f"Mining the segments of {len(docs)} vacancies", file=sys.stderr)
    mined = mine(docs, covered, known)
    write_candidates(sys.argv[1], mined, f"the segments of {len(docs)} vacancies of {skillloop.SEGMENTS.name}")
    print(f"Wrote {len(mined)} candidates to {sys.argv[1]}", file=sys.stderr)


if __name__ == "__main__":
    main()
