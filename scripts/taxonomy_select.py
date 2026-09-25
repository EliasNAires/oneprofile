#!/usr/bin/env python3
"""Selects the skill taxonomy from the review of the candidates: merges the verdicts into skills,
keeps those named in enough IN descriptions by the keys the review kept, and writes skills.tsv.

    scripts/taxonomy_select.py <review.tsv>... <threshold> <skills.tsv>

Each review has one verdict per line, as name, verdict (keep or drop), canonical name, category,
aliases separated by '|', and a note. The descriptions are counted as scripts/taxonomy_candidates.py
counts them, so the development database must be up. Once written, skills.tsv is edited by hand.
"""
import csv
import re
import sys

from taxonomy_candidates import corpus, document_frequency, name_key, say

CATEGORIES = {'language', 'framework', 'database', 'cloud', 'devops', 'data', 'ml', 'testing', 'os',
              'security', 'networking', 'tool'}


def skill_id(canonical):
    slug = canonical.lower().replace('++', 'pp').replace('#', '-sharp')
    return re.sub(r'[^a-z0-9]+', '-', slug).strip('-')


def merge(verdicts):
    """The kept verdicts as skills, one per canonical name, their aliases pooled."""
    skills = {}
    for v in verdicts:
        if v['verdict'] != 'keep':
            continue
        if v['category'] not in CATEGORIES:
            raise ValueError(f"{v['canonical']} has the category '{v['category']}'")
        skill = skills.setdefault(name_key(v['canonical']), {
            'id': skill_id(v['canonical']), 'canonical': v['canonical'], 'category': v['category'],
            'aliases': []})
        if skill['category'] != v['category']:
            raise ValueError(f"{v['canonical']} is both {skill['category']} and {v['category']}")
        skill['aliases'] += [a for a in v['aliases'].split('|') if a.strip()]
    named, ids = {}, {}
    for skill in skills.values():
        keys = [name_key(skill['canonical'])]
        aliases = []
        for alias in skill['aliases']:
            if name_key(alias) not in keys:
                keys.append(name_key(alias))
                aliases.append(alias)
        skill['aliases'] = aliases
        for k in keys:
            other = named.setdefault(k, skill)
            if other is not skill:
                raise ValueError(f"'{k}' names both {other['canonical']} and {skill['canonical']}")
        other = ids.setdefault(skill['id'], skill)
        if other is not skill:
            raise ValueError(f"{other['canonical']} and {skill['canonical']} share the id '{skill['id']}'")
    return list(skills.values())


class Counted:
    """A skill as document_frequency counts a candidate: by its forms."""

    def __init__(self, skill):
        self.skill = skill
        self.forms = [skill['canonical']] + skill['aliases']


def select(skills, docs, threshold):
    """The skills named in at least threshold descriptions by their canonical name or an alias."""
    counted = [Counted(s) for s in skills]
    document_frequency(counted, docs)
    return [c.skill for c in counted if c.df >= threshold]


def read(path):
    with open(path, newline='') as f:
        rows = csv.reader((line for line in f if not line.startswith('#')), delimiter='\t', quoting=csv.QUOTE_NONE)
        fields = next(rows)
        return [dict(zip(fields, row)) for row in rows]


def main():
    if len(sys.argv) < 4:
        raise SystemExit('usage: taxonomy_select.py <review.tsv>... <threshold> <skills.tsv>')
    *reviews, threshold, out = sys.argv[1:]
    skills = merge([v for review in reviews for v in read(review)])
    docs, _ = corpus()
    kept = select(skills, docs, int(threshold))
    say(f'{len(skills)} skills kept by the reviews, {len(kept)} named in at least {threshold} descriptions')
    with open(out, 'w') as f:
        for s in sorted(kept, key=lambda s: s['id']):
            f.write(f"{s['id']}\t{s['canonical']}\t{s['category']}\t{'|'.join(s['aliases'])}\n")


if __name__ == '__main__':
    main()
