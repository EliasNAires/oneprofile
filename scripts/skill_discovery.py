#!/usr/bin/env python3
"""Proposes the candidates of skill discovery (ADR-0013): the name-like phrases of the segments of IN
and the pile that are not a key of skills.tsv. A name in the decision record is proposed like any other
(#62): precision grades the rules on every new name they propose, whatever an older criterion decided.

    scripts/skill_discovery.py <out.tsv>

There is no dictionary to tell a name from a word, so the corpus tells: a name is written
capitalized in the middle of a sentence, or carries a digit or a symbol, and sits near a skill the
taxonomy already has. Ordinary words fail the first test, and company, place and people names, which
are capitalized too, fail the second.

Round 0 is #40's taxonomy_mine.py, reading the segments cleaning stored (#46) instead of whole
descriptions, and their words as Segment.tokens() reads them. A segment's first word starts a
sentence, and no phrase runs from one segment into the next. Spellings are grouped by name form,
the decision record's and the keys', so a hyphen inside a word counts as a space (Spring-Boot is spring boot), and
#40's check that a phrase keeps one key token a word is gone with the key it checked.

Round 1 cuts what round 0's review found to be noise: a word hyphened or possessive to a name is
folded into it (Linux-based is Linux), and a phrase is no candidate when it is the plural of a covered
name or of another candidate, holds a number, a title or a folded suffix, is known names run together,
or holds a known name beside a generic noun or a word that is not name-like. An acronym has to sit
near a skill more often than a longer name, a name has to be in more vacancies, and none can sit
mostly inside a longer name. Office and design software are no anchors. A known key written with a
space inside it is that key's spelling, and proposed on less evidence.

Round 2 brings back the names in few ads, which round 1's ten-vacancy bar cut to fit a cap that is
gone: a name needs three companies, and so three vacancies, as in round 0. A vacancy counts for a
phrase only where it writes the words as one, so a pair from a list ("Grafana, Kibana") is not
counted as a phrase. An acronym sits near a skill as often as any other name has to, since protocols
are named in prose. A known key respelled still skips the tests a name in its own right takes, but no
longer has a lower bar of vacancies.

Round 3 cuts concepts, certifications and companies, and keeps yield: an acronym is judged by how
vacancies spell it out, where enough of them do, and is no candidate when they spell it out in lower
case, as a title or as several things. A phrase of two or more words has to be capitalized as a whole
more often than a word, and one holding no known name cannot end on a generic noun. A name vacancies
mostly write after "at" is a company. A descriptor hyphened to a word (Multi-Cloud), an organization
word (Capital, Ventures) or a count glued to a word (5+years) makes a phrase no candidate.

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
MIN_COMPANIES = 3  # one company's boilerplate and its own name are not skills; in as many vacancies at least
MIN_CAPITALIZED = 0.4  # pytest is capitalized in 45% of its mid-sentence occurrences
MIN_PHRASE_CAPITALIZED = 0.9  # a product word after a known name (MongoDB Atlas) is capitalized with it
MIN_WORDS_CAPITALIZED = 0.65  # round 2's feedback: round 1's sample held no keep among phrases capitalized less
NEAR = 6  # words either side
MIN_NEAR = 0.2  # 95% of the skills in skills.tsv sit near another more often than this
# A company is written after "at" (engineers at GoCardless); a platform whose maker posts vacancies less
# often: Dataiku 0.15, PlanetScale 0.22 in round 2's candidates, against GoCardless 0.44, Tipalti 0.31.
MAX_AFTER_AT = 0.3
MAX_EXTENDED = 0.7  # 97% of the keys of skills.tsv sit inside a longer name less often (Workspace in Google Workspace)
# An acronym is judged by how vacancies spell it out, where they do at least MIN_SPELLED times: a technology's is
# written as a name and means one thing (Model Context Protocol, Open Policy Agent). A concept's is written
# in lower case somewhere (service level agreement), a certification's holds a title (Certified Kubernetes
# Administrator), and an ambiguous abbreviation's mean several things (Azure Resource Manager, Advanced
# Revenue Management). Each spelling counts once a vacancy. Of round 2's 608 short acronyms, 244 were
# spelled out three times or more.
MIN_SPELLED = 3
# Spelled out in lower case or as a title: MCP 1%, OPA 0%, against PAM 12%, HPC 51%, API 81%.
MAX_NOT_A_NAME_SPELLED = 0.1
# The commonest meaning's share: MCP 0.95, OPA 0.85, against ARM 0.58, MDM 0.58, CD 0.37.
MIN_ONE_MEANING = 0.6

# Segment.WORD: letters and digits of any script, holding together across the apostrophes, full stops
# and hyphens inside it, and keeping the + and # that end C++, C# or 5+.
WORD = re.compile(r"[^\W_](?:(?:[^\W_]|['’.+#-])*(?:[^\W_]|[+#]))?")
SENTENCE_BREAK = re.compile(r"[.!?]\s|[:;•*|–—]|(^|\s)-(\s|$)")
MARKED = re.compile(r"[0-9+#.]")
POSSESSIVE = re.compile(r"['’]s$", re.IGNORECASE)
NUMBER = re.compile(r"[0-9][0-9.,+%]*(?:[kKmMbB]|년)?")  # a year, a count, a version: 2028, 5+, 2.0, 100M, 5년
GLUED_COUNT = re.compile(r"[0-9][0-9.,]*[+%].+")  # 5+years, not 3D or H100
ACRONYM = re.compile(r"[A-Z]{2,6}")  # one letter is no evidence: R (preferred)
# An acronym's spelling out, before it or after it: "Open Shortest Path First (OSPF)", "OSPF (Open Shortest Path
# First)". A plural s after the acronym is the acronym (SLAs).
SPELLED_BEFORE = re.compile(r"((?:[^\W_][\w'’.&/-]*,?\s+){1,8})\(\s*([A-Z]{2,6})s?\s*\)")
SPELLED_AFTER = re.compile(r"(?<![^\W_])([A-Z]{2,6})s?\s*\(\s*([^()]{3,80})\)")
SPELLED_PARTS = re.compile(r"[\s,/-]+")
# Words a spelling out passes over (Network Access Control, Extract, Transform and Load).
SMALL = {"&", "a", "an", "and", "as", "by", "de", "for", "in", "of", "on", "the", "to", "with", "y"}

# A name with one of these hyphened to it is the name: Linux-based is Linux, AI-assisted is AI.
FOLDED_SUFFIXES = {"assisted", "authorized", "aware", "based", "centric", "certified", "compatible", "compliant",
                   "driven", "enabled", "first", "focused", "friendly", "heavy", "led", "native", "oriented",
                   "powered", "ready", "related", "savvy", "specific", "accelerated", "agnostic", "backed", "style",
                   "augmented", "level"}
# Words hyphened before a name that make it a descriptor (Multi-Cloud, Cross-Platform): the phrase is no name.
DESCRIPTOR_PREFIXES = {"cross", "multi", "non"}
# Abbreviations whose full stop, and contractions whose capital, is no evidence of a name: "e.g Python" is
# Python, and I'm is no one.
ABBREVIATIONS = {"e.g", "i.e", "eg", "ie", "etc", "vs", "incl", "approx", "esp", "i'm", "i’m"}
# Words that make a phrase a job title or a certification (Senior Architect, Certified Azure Administrator).
TITLES = {"admin", "administrator", "administrators", "analyst", "analysts", "architect", "architects", "associate",
          "cert", "certificate", "certification", "certifications", "certified", "consultant", "consultants",
          "director", "engineer", "engineers", "ii", "iii", "intern", "iv", "jr", "junior", "lead", "officer",
          "principal", "professional", "scientist", "senior", "specialist", "specialists", "sr", "technician"}
# Words that make a phrase an investor or an organization (Sequoia Capital, Vista Equity Partners).
ORGANIZATIONS = {"capital", "corp", "corporation", "foundation", "inc", "llc", "ltd", "partners", "ventures"}
# Words that end a product's name as often as a title's (Google Tag Manager, Amazon Q Developer): a title
# only after a skill, a title word or a word that heads a title (Java Developer, Product Manager).
ROLES = {"developer", "developers", "manager", "managers"}
ROLE_HEADS = {"account", "delivery", "engineering", "general", "hiring", "office", "product", "program", "project"}
# Ordinary nouns that end no product's name after a skill (Docker Access, Kubernetes Pods), nor lead into
# one (Platforms Docker). Inside a name they are part of it: Azure Data Factory.
GENERIC = {"access", "ai", "api", "apis", "automation", "business", "certs", "ci", "data", "development",
           "experience", "fundamentals", "gpu", "gpus", "it", "key", "knowledge", "ml", "office", "ops", "os",
           "platform", "platforms", "pod", "pods", "proficiency", "role", "roles", "saas", "sdk", "sdks", "skills",
           "soc", "stack", "suite", "tooling", "tools"}
# Office, design and engineering software the criterion drops: its passages vouch for their own vendors,
# so its keys are no evidence that a name beside them is a skill.
NOT_ANCHORS = {"Abaqus", "Adobe After Effects", "Adobe Creative Cloud", "Adobe InDesign", "Adobe Photoshop",
               "ADP Workforce Now", "Altium Designer", "AutoCAD", "Autodesk Construction Cloud", "Autodesk Revit",
               "Basecamp", "CATIA", "ClickUp", "DocuSign", "EPLAN", "Google Sheets", "Google Workspace",
               "Klaxoon", "Lucidchart", "Microsoft 365", "Microsoft Excel", "Microsoft Project", "Microsoft Visio",
               "Microsoft Word", "Miro", "Monday.com", "Primavera P6", "Procore", "PTC Creo", "QuickBooks",
               "Siemens NX", "Smartsheet", "Trello", "UKG Pro", "Windchill", "Wrike", "Xero"}


class Word:
    __slots__ = ("text", "name", "form", "starts", "joined")

    def __init__(self, text, starts, joined):
        self.text, self.starts, self.joined = text, starts, joined
        name = POSSESSIVE.sub("", text)
        head, hyphen, tail = name.rpartition("-")
        if hyphen and head and tail.lower() in FOLDED_SUFFIXES:
            name = head
        self.name = name
        lowered = name.lower()
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
    return all((word != word.lower() or MARKED.search(word)) and word.lower() not in ABBREVIATIONS
               for word in text.split())


def phrases(ws):
    """(start, end) of every run of up to MAX_WORDS words joined by spaces within a sentence."""
    for i in range(len(ws)):
        for j in range(i + 1, min(i + MAX_WORDS, len(ws)) + 1):
            if j > i + 1 and (ws[j - 1].starts or not ws[j - 1].joined):
                break
            yield i, j


def spans(words, known):
    """(start, end) of every run of the words, short of all of them, that is a known name form."""
    return [(i, j) for i in range(len(words)) for j in range(i + 1, len(words) + 1)
            if j - i < len(words) and " ".join(words[i:j]) in known]


def names_run_together(words, known):
    """Whether the words are two or more known names side by side, as a list writes them: Docker
    Kubernetes."""
    ends = {0}
    for j in range(1, len(words) + 1):
        if any(i in ends and " ".join(words[i:j]) in known for i in range(j)):
            ends.add(j)
    return len(words) > 1 and len(words) in ends


def a_title(words, known):
    """Whether the words read as a job title or a certification."""
    for k, word in enumerate(words):
        if word in TITLES:
            return True
        if word in ROLES and k > 0 and any(words[k - 1] in heads for heads in (known, TITLES, ROLE_HEADS)):
            return True
    return False


def proposable(form, covered, known, compact):
    """Whether a phrase's name form can be a candidate at all, before the corpus is asked about it: not
    covered, not covered once a plural s is taken off, and either a known key respelled or holding no
    number, folded suffix, descriptor prefix or organization word, no title, and not names already known
    run together. A plural is no candidate where its singular cannot be one (ITIL Foundations, Chief Data
    Officers)."""
    if form in covered or (form.endswith("s") and form[:-1] in covered):
        return False
    if respelled(form, compact):
        return True
    if form.endswith("s") and not proposable(form[:-1], covered, known, compact):
        return False
    words = form.split(" ")
    return not (any(NUMBER.fullmatch(word) or GLUED_COUNT.fullmatch(word) or word in FOLDED_SUFFIXES
                    or word in DESCRIPTOR_PREFIXES or word in ORGANIZATIONS for word in words)
                or a_title(words, known) or names_run_together(words, known))


def mine(docs, covered, known, anchors=None):
    """The name-like phrases of the docs, each a company and its vacancy's segments, whose name form is
    not in covered. known holds the name forms of the keys of the skills already in the taxonomy, and
    anchors those of them whose neighbours are evidence of a skill, all of them unless given."""
    compact = {key.replace(" ", "") for key in known}
    said, forms = gather(docs, covered, known, compact)
    stats = measure(docs, forms, known if anchors is None else anchors)
    acronyms = {commonest_spelling(s) for s in stats.values() if ACRONYM.fullmatch(commonest_spelling(s))}
    spelled = spelled_out(docs, acronyms, known)
    return [candidate(s) for form, s in stats.items() if passes(form, s, said, known, compact, spelled)]


def gather(docs, covered, known, compact):
    """How often each word is said mid-sentence and how often written as a name, by word and by (word,
    True); and the name forms of the phrases that could be candidates: written as a name mid-sentence
    somewhere, proposable, and not the plural of another."""
    said = collections.Counter()
    forms = set()
    for _, segments in docs:
        ws = words(segments)
        for w in ws:
            if not w.starts:
                for part in w.form.split(" "):
                    said[part] += 1
                    said[part, True] += written_as_name(w.name)
        for i, j in phrases(ws):
            if ws[i].starts:
                continue
            form = " ".join(w.form for w in ws[i:j])
            if (form not in forms and written_as_name(" ".join(w.name for w in ws[i:j]))
                    and proposable(form, covered, known, compact)):
                forms.add(form)
    return said, {form for form in forms if not (form.endswith("s") and form[:-1] in forms)}


def respelled(form, compact):
    """Whether the form is a known key written with a space inside it (Mongo DB, Power Shell), and so that
    key's spelling. compact holds the known keys with their spaces taken out."""
    joined = form.replace(" ", "")
    return " " in form and len(joined) > 3 and joined in compact


def passes(form, s, said, known, compact, spelled):
    """Whether a phrase measured in the corpus is a candidate: in the vacancies of enough companies,
    written as a name, and either a known key respelled, or a name in its own right: near a skill, not
    mostly a company written after "at" nor a fragment of a longer name, naming a product, and, for an
    acronym, spelled out as one."""
    if not (len(s["companies"]) >= MIN_COMPANIES and s["capitalized"] / s["said"] >= MIN_CAPITALIZED):
        return False
    if respelled(form, compact):
        return True
    return (s["near"] / s["said"] >= MIN_NEAR
            and s["after_at"] / s["said"] <= MAX_AFTER_AT
            and s["extended"] / s["said"] <= MAX_EXTENDED
            and (" " not in form or s["capitalized"] / s["said"] >= MIN_WORDS_CAPITALIZED)
            and names_a_product(form, s, said, known)
            and spelled_as_a_name(spelled.get(commonest_spelling(s), collections.Counter())))


def spelled_out(docs, acronyms, known):
    """For each of the acronyms, its spellings out in the docs, each counted once a vacancy, as
    (whether it is written as a name, holding no lower-case word and no title; what it means, the first
    letters of its words)."""
    spellings = collections.defaultdict(collections.Counter)
    for _, segments in docs:
        found = set()
        for segment in segments:
            if "(" not in segment:
                continue
            for pattern, before in ((SPELLED_BEFORE, True), (SPELLED_AFTER, False)):
                for match in pattern.finditer(segment):
                    words, acronym = match.groups() if before else match.groups()[::-1]
                    parts = [part for part in SPELLED_PARTS.split(words) if part]
                    spelling = acronym in acronyms and spelling_out(parts, acronym, before)
                    if spelling:
                        found.add((acronym, tuple(spelling)))
        for acronym, spelling in found:
            words = [part for part in spelling if part.lower() not in SMALL]
            written_as_a_name = not any(word.islower() for word in words) and not a_title(
                [word.lower() for word in words], known)
            spellings[acronym][written_as_a_name, tuple(word[:4].lower() for word in words)] += 1
    return spellings


def spelling_out(parts, acronym, before):
    """The fewest of the parts, counted from the last when they come before the acronym and from the
    first when after it, whose initials spell the acronym, small words passed over and none at the far
    end."""
    if before:
        spelling = spelling_out(parts[::-1], acronym[::-1], False)
        return spelling and spelling[::-1]
    initials = ""
    for k, part in enumerate(parts):
        if part.lower() not in SMALL:
            initials += part[0].upper()
            if initials == acronym:
                return parts[:k + 1]
            if not acronym.startswith(initials):
                return None
    return None


def spelled_as_a_name(spellings):
    """Whether the vacancies spelling an acronym out write it as a name, and as one name: true when too
    few spell it out to tell."""
    total = sum(spellings.values())
    if total < MIN_SPELLED:
        return True
    meanings = collections.Counter()
    for (_, meaning), n in spellings.items():
        meanings[meaning] += n
    not_a_name = sum(n for (written_as_a_name, _), n in spellings.items() if not written_as_a_name)
    return not_a_name / total < MAX_NOT_A_NAME_SPELLED and meanings.most_common(1)[0][1] / total >= MIN_ONE_MEANING


def names_a_product(form, s, said, known):
    """Whether the words of a phrase that are not a known name are name-like: any of them, for a phrase
    holding none, whose last word is no generic noun either (Excel Experience, NVIDIA GPU);
    each, for one that does, none that ends the phrase or leads into the known name a generic noun, and
    those after the known name passing too when the phrase is capitalized as a whole (MongoDB Atlas, Redis
    Cloud). A phrase of ordinary words capitalized as a whole is a heading or a field of study as often as
    a name (Key Responsibilities, Computer Science), so a phrase holding no
    known name has no such pass."""

    def name_like(word):
        return said[word] and said[word, True] / said[word] >= MIN_CAPITALIZED

    words = form.split(" ")
    inside = spans(words, known)
    if not inside:
        return any(name_like(word) for word in words) and words[-1] not in GENERIC
    whole = s["capitalized"] / s["said"] >= MIN_PHRASE_CAPITALIZED
    first = min(i for i, _ in inside)
    outside = [k for k in range(len(words)) if not any(i <= k < j for i, j in inside)]
    return all((first < k < len(words) - 1 or words[k] not in GENERIC)
               and (name_like(words[k]) or (whole and k > first)) for k in outside)


def commonest_spelling(s):
    """A measured phrase's commonest spelling."""
    return s["forms"].most_common(1)[0][0]


def candidate(s):
    """A candidate as a run writes it, named by its commonest spelling."""
    return {"name": commonest_spelling(s), "df": s["df"], "companies": len(s["companies"]),
            "capitalized": s["capitalized"] / s["said"], "near": s["near"] / s["said"], "forms": dict(s["forms"])}


def measure(docs, forms, anchors):
    """For each of the name forms, the vacancies and companies writing it as a phrase, not as words that
    only follow each other across a comma or a sentence break, and of its mid-sentence
    occurrences: how many, how many written as a name, how many near an anchor's, how many right after
    "at", how many inside a longer run of words written as names, and each spelling's count."""
    longest = max(len(form.split(" ")) for form in anchors)
    firsts = {form.split(" ")[0] for form in anchors}
    stats = collections.defaultdict(lambda: {"df": 0, "companies": set(), "said": 0, "capitalized": 0,
                                             "near": 0, "after_at": 0, "extended": 0,
                                             "forms": collections.Counter()})
    for company, segments in docs:
        ws = words(segments)
        word_forms = [w.form for w in ws]
        present = {" ".join(word_forms[i:j]) for i, j in phrases(ws)} & forms
        if not present:
            continue
        for form in present:
            stats[form]["df"] += 1
            stats[form]["companies"].add(company)
        skills = [(i, j) for i in range(len(word_forms)) if word_forms[i].split(" ")[0] in firsts
                  for j in range(i + 1, min(i + longest, len(word_forms)) + 1) if " ".join(word_forms[i:j]) in anchors]
        for i, j in phrases(ws):
            form = " ".join(word_forms[i:j])
            if ws[i].starts or form not in present:
                continue
            s = stats[form]
            text = " ".join(w.name for w in ws[i:j])
            s["said"] += 1
            s["capitalized"] += written_as_name(text)
            s["forms"][text] += 1
            s["near"] += any((b <= i or a >= j) and a <= j + NEAR and b >= i - NEAR for a, b in skills)
            s["after_at"] += ws[i].joined and ws[i - 1].form == "at"
            s["extended"] += ((ws[i].joined and extends(ws[i - 1]))
                              or (j < len(ws) and ws[j].joined and extends(ws[j])))
    return stats


def extends(word):
    """Whether a word joined to a phrase makes it part of a longer name: written as one, and neither a
    capital starting a sentence nor a version (Grafana 11 is Grafana)."""
    return not word.starts and written_as_name(word.name) and not NUMBER.fullmatch(word.name)


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


def covered():
    """The name forms never proposed: the keys of skills.tsv."""
    return set(skillloop.read_keys())


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__.strip())
    keys = skillloop.read_keys()
    known = set(keys)
    anchors = {form for form, canonical in keys.items() if canonical not in NOT_ANCHORS}
    docs = corpus()
    print(f"Mining the segments of {len(docs)} vacancies", file=sys.stderr)
    mined = mine(docs, covered(), known, anchors)
    write_candidates(sys.argv[1], mined, f"the segments of {len(docs)} vacancies of {skillloop.SEGMENTS.name}")
    print(f"Wrote {len(mined)} candidates to {sys.argv[1]}", file=sys.stderr)


if __name__ == "__main__":
    main()
