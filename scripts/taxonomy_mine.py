#!/usr/bin/env python3
"""Mines the IN descriptions for the skill names the seed (scripts/taxonomy_candidates.py) missed:
the name-like n-grams no candidate form covers, for review.

    scripts/taxonomy_mine.py <candidates.tsv> <skills.tsv> <out.tsv>

There is no dictionary to tell a name from a word, so the corpus tells: a name is written
capitalized in the middle of a sentence, or carries a digit or a symbol, and sits near a skill the
taxonomy already has. Ordinary words fail the first test, and company, place and people names, which
are capitalized too, fail the second. Needs the development database up.
"""
import collections
import csv
import re
import sys

from taxonomy_candidates import key, psql, say, trim_dots

MAX_WORDS = 3
MIN_DF = 3
MIN_COMPANIES = 3  # one company's boilerplate and its own name are not skills
MIN_CAPITALIZED = 0.4  # pytest is capitalized in 45% of its mid-sentence occurrences
NEAR = 6  # words either side
MIN_NEAR = 0.2  # 95% of the skills in skills.tsv sit near another more often than this

SEPARATOR = re.compile(r'[^A-Za-z0-9+#.]+')
SENTENCE_BREAK = re.compile(r'[.!?:;•*|–—]|(^|\s)-(\s|$)')
MARKED = re.compile(r'[0-9+#.]')


class Word:
    __slots__ = ("text", "starts", "joined")

    def __init__(self, text, starts, joined):
        self.text, self.starts, self.joined = text, starts, joined


def words(text):
    """The tokens of the seed as written, each marked with whether it starts a sentence and whether
    only spaces part it from the word before."""
    out, gap = [], ''
    parts = SEPARATOR.split(text)
    separators = SEPARATOR.findall(text) + ['']
    for part, separator in zip(parts, separators):
        t = trim_dots(part)
        if t:
            before, after = part[:part.index(t)], part[part.index(t) + len(t):]
            gap += before
            out.append(Word(t, not out or bool(SENTENCE_BREAK.search(gap)), bool(out) and not gap.strip()))
            gap = after
        else:
            gap += part
        gap += separator
    return out


def written_as_name(text):
    """Every word capitalized or carrying a digit or symbol."""
    return all(w != w.lower() or MARKED.search(w) for w in text.split())


def phrases(ws):
    """(start, end) of every run of up to MAX_WORDS words joined by spaces within a sentence."""
    for i in range(len(ws)):
        for j in range(i + 1, min(i + MAX_WORDS, len(ws)) + 1):
            if j > i + 1 and (ws[j - 1].starts or not ws[j - 1].joined):
                break
            yield i, j


def mine(docs, covered, known):
    """The name-like n-grams of the docs, as (company, text) pairs, that no key in covered names.
    known holds the keys of the skills already in the taxonomy."""
    docs = [(company, words(text)) for company, text in docs]
    said = collections.Counter()  # word -> mid-sentence occurrences, and capitalized ones
    candidates = set()
    for _, ws in docs:
        for w in ws:
            if not w.starts:
                said[w.text.lower()] += 1
                said[w.text.lower(), True] += written_as_name(w.text)
        for i, j in phrases(ws):
            if ws[i].starts:
                continue
            text = ' '.join(w.text for w in ws[i:j])
            k = key(text)
            if len(k) == j - i and written_as_name(text) and k not in covered:
                candidates.add(k)

    longest = max(len(k) for k in known)
    stats = collections.defaultdict(lambda: {'df': 0, 'companies': set(), 'said': 0, 'capitalized': 0,
                                             'near': 0, 'forms': collections.Counter()})
    for company, ws in docs:
        low = [w.text.lower() for w in ws]
        present = {tuple(low[i:j]) for i in range(len(low)) for j in range(i + 1, i + MAX_WORDS + 1)
                   if j <= len(low)} & candidates
        for k in present:
            stats[k]['df'] += 1
            stats[k]['companies'].add(company)
        skills = [(i, j) for i in range(len(low)) for j in range(i + 1, min(i + longest, len(low)) + 1)
                  if tuple(low[i:j]) in known]
        for i, j in phrases(ws):
            k = tuple(low[i:j])
            if ws[i].starts or k not in present:
                continue
            s = stats[k]
            text = ' '.join(w.text for w in ws[i:j])
            s['said'] += 1
            s['capitalized'] += written_as_name(text)
            s['forms'][text] += 1
            s['near'] += any((b <= i or a >= j) and a <= j + NEAR and b >= i - NEAR for a, b in skills)

    def name_like(word):
        return said[word, True] / said[word] >= MIN_CAPITALIZED

    mined = []
    for k, s in stats.items():
        if (s['df'] >= MIN_DF and len(s['companies']) >= MIN_COMPANIES
                and s['capitalized'] / s['said'] >= MIN_CAPITALIZED
                and s['near'] / s['said'] >= MIN_NEAR
                and any(name_like(word) for word in k)):
            mined.append({'name': s['forms'].most_common(1)[0][0], 'df': s['df'],
                          'companies': len(s['companies']), 'capitalized': s['capitalized'] / s['said'],
                          'near': s['near'] / s['said'], 'forms': dict(s['forms'])})
    return mined


def covered_keys(candidates_path):
    """The key of every form in the candidates, kept or dropped: all of them were reviewed."""
    covered = set()
    with open(candidates_path) as f:
        for line in f:
            if line.startswith('#') or line.startswith('df\t'):
                continue
            for form in line.rstrip('\n').split('\t')[4].split('|'):
                covered.add(key(form.rsplit('=', 1)[0]))
    return covered


def skill_keys(skills_path):
    known = set()
    with open(skills_path) as f:
        for line in f:
            _, canonical, _, aliases = line.rstrip('\n').split('\t')
            known |= {key(k) for k in [canonical] + aliases.split('|') if k}
    return known


def corpus():
    """The IN descriptions of the development database, each with its company's name, and the date
    of the newest update among them."""
    counted = """from vacancy v join normalized_vacancy n on n.vacancy_id = v.id join company c on c.id = v.company_id
                 where n.classification_state = 'IN' and v.description is not null"""
    out = psql(f'copy (select c.name, v.description {counted} order by n.id) to stdout with csv')
    csv.field_size_limit(sys.maxsize)
    docs = [(row[0], row[1]) for row in csv.reader(out.splitlines(keepends=True))]
    newest = psql(f'select max(v.updated_at)::date {counted}').strip()
    return docs, newest


def main():
    if len(sys.argv) != 4:
        raise SystemExit('usage: taxonomy_mine.py <candidates.tsv> <skills.tsv> <out.tsv>')
    known = skill_keys(sys.argv[2])
    covered = covered_keys(sys.argv[1]) | known
    docs, newest = corpus()
    say(f'mining {len(docs)} descriptions')
    mined = mine(docs, covered, known)
    with open(sys.argv[3], 'w') as f:
        f.write('# Skill names mined from the corpus (issue #40), written by scripts/taxonomy_mine.py.\n'
                f'# Corpus: the {len(docs)} IN descriptions of the development database, newest vacancy update {newest}.\n'
                f'# Left out: every form of {sys.argv[1]} and every key of {sys.argv[2]}.\n'
                f'# Kept: df >= {MIN_DF}, in the ads of >= {MIN_COMPANIES} companies, written as a name (every word capitalized or\n'
                f'# carrying a digit or symbol) in >= {MIN_CAPITALIZED:.0%} of its mid-sentence occurrences, with a known skill within {NEAR}\n'
                f'# words in >= {MIN_NEAR:.0%} of them, and a phrase holding a name-like word.\n'
                '# capitalized (written as a name) and near are those shares; each form=N counts a spelling mid-sentence.\n'
                'df\tname\tcompanies\tcapitalized\tnear\tforms\n')
        for m in sorted(mined, key=lambda m: (-m['df'], m['name'].lower())):
            forms = '|'.join(f'{t}={n}' for t, n in sorted(m['forms'].items(), key=lambda x: -x[1]))
            f.write(f"{m['df']}\t{m['name']}\t{m['companies']}\t{m['capitalized']:.2f}\t{m['near']:.2f}\t{forms}\n")
    say(f'wrote {len(mined)} mined names to {sys.argv[3]}')


if __name__ == '__main__':
    main()
