#!/usr/bin/env python3
"""Seeds the skill taxonomy: turns O*NET Technology Skills, GitHub Linguist and Wikidata into
candidate skills, each with the document frequency of its surface forms over the IN descriptions of
the development database. It does not choose the taxonomy; that is done from the candidates.

    scripts/taxonomy_candidates.py <out.tsv>

The sources are downloaded once into $TAXONOMY_DIR (default ~/oneprofile-taxonomy) and read from
there afterwards. O*NET and Linguist are pinned to a release and checked against their SHA-256.
Wikidata has no releases, so its answers are kept as pulled, with the date; deleting them pulls
again. Needs Python 3 and PyYAML, and the development database up.
"""
import collections
import csv
import datetime
import hashlib
import http.client
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

import yaml

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TAXONOMY_DIR = os.environ.get('TAXONOMY_DIR', os.path.expanduser('~/oneprofile-taxonomy'))
USER_AGENT = 'oneprofile-taxonomy-build/0.1 (https://github.com/EliasNAires/oneprofile)'

ONET = {
    'file': 'onet-30.2-technology-skills.txt',
    'url': 'https://www.onetcenter.org/dl_files/database/db_30_2_text/Technology%20Skills.txt',
    'sha256': 'a6e7ea1fb368659a3a8ffe2895d8343cbe80b61efbb2db39cb3cb0f9bd56d3b4',
    'title': 'O*NET 30.2 Technology Skills (CC BY 4.0)',
}
LINGUIST = {
    'file': 'linguist-v9.7.0-languages.yml',
    'url': 'https://raw.githubusercontent.com/github-linguist/linguist/v9.7.0/lib/linguist/languages.yml',
    'sha256': '7c2bc5b59662de6c5d09cd4990e82b2541d4cd2ea7c8c213537730474f24a5c7',
    'title': 'GitHub Linguist v9.7.0 languages.yml (MIT)',
}

# The engineering occupations, and the commodity categories that are engineering whichever
# occupation lists them.
ENGINEERING_SOC = re.compile(r'^(15-12|15-2051|17-2061)')
ENGINEERING_COMMODITY = re.compile(
    r'^(Development environment|Web platform development|Object or component oriented development'
    r'|Data base management system|Object oriented data base|Program testing|Configuration management'
    r'|File versioning|Cloud-based management|Application server|Compiler and decompiler'
    r'|Operating system software|Enterprise application integration'
    r'|Graphical user interface development|Business intelligence|Data mining)')

# The Wikidata classes pulled, and whether their subclasses are pulled too. Each is its own query:
# a broad transitive one times out. Items with fewer than five sitelinks are left out, which is
# also what keeps the software library query inside the timeout.
WIKIDATA_CLASSES = [
    ('Q9143', 'programming language', True),
    ('Q271680', 'software framework', True),
    ('Q1330336', 'web framework', True),
    ('Q188860', 'software library', True),
    ('Q176165', 'DBMS', True),
    ('Q1193246', 'GUI toolkit', False),
    ('Q9135', 'operating system', False),
]

# The properties only software has: version, file format, license, source repository, website of
# its documentation. Not the developer: that resolves "Spring Boot" to the Tupolev Tu-91 aircraft.
SOFTWARE_PROPERTIES = 'wdt:P348|wdt:P277|wdt:P306|wdt:P3966|wdt:P1324'

# The vendor names O*NET prefixes a product with, longest first where one is the start of another.
VENDORS = [
    'Amazon Web Services AWS', 'Amazon Web Services', 'Amazon', 'AWS', 'IBM', 'Google', 'Microsoft',
    'Oracle', 'Apache', 'Adobe', 'SAP', 'Atlassian', 'Red Hat', 'Cisco', 'HashiCorp', 'JetBrains',
    'Meta', 'Facebook', 'Salesforce', 'Elastic', 'Apple', 'Informatica', 'Teradata', 'Talend',
    'Perforce', 'MathWorks', 'SAS', 'Hewlett Packard Enterprise', 'Hewlett-Packard', 'HP', 'Intel',
    'VMware', 'Broadcom', 'GitHub', 'GitLab', 'Mozilla', 'Eclipse', 'Progress', 'Embarcadero',
    'Quest', 'BMC', 'CA', 'Symantec', 'McAfee', 'Sun', 'Autodesk', 'ESRI', 'Cadence', 'Synopsys',
    'Xilinx', 'NVIDIA', 'Nvidia', 'Databricks', 'Snowflake', 'Confluent', 'MongoDB', 'Splunk',
    'Datadog', 'SmartBear', 'Micro Focus', 'OpenText', 'Twilio', 'Stripe', 'Shopify', 'Canonical',
    'SUSE', 'Citrix', 'Dell', 'EMC', 'NetApp', 'Juniper', 'Fortinet', 'Palo Alto Networks',
    'Check Point', 'Qlik', 'Tableau', 'MicroStrategy', 'Alteryx', 'Pivotal', 'Epic Games',
]

# O*NET examples that neither Wikidata nor the rules above name right, and the forms they are
# written as, the name first. They are not looked up in Wikidata. The AWS products lose "Elastic"
# as if it were a vendor, and "AWS" alone resolves to a MediaWiki extension.
MERGE = {
    'Red Hat Ansible Engine': ['Ansible'],
    'Amazon Web Services AWS software': ['AWS', 'Amazon Web Services'],
    'Amazon Elastic Compute Cloud EC2': ['EC2', 'Elastic Compute Cloud'],
    'Amazon Elastic Container Service ECS': ['ECS', 'Elastic Container Service'],
    'AWS Elastic MapReduce (EMR)': ['EMR', 'Elastic MapReduce'],
    'Amazon Simple Storage Service S3': ['S3', 'Simple Storage Service'],
}

TOKEN_SEPARATOR = re.compile(r'[^a-z0-9+#.]+')


def tokens(text):
    """Lower-cased words, keeping the '+', '#' and '.' technology names are spelled with. A trailing
    dot is a sentence's, and a leading one is kept only before a letter ('.net')."""
    out = []
    for t in TOKEN_SEPARATOR.split(text.lower()):
        t = t.rstrip('.')
        if t.startswith('.') and not re.match(r'\.[a-z]', t):
            t = t.lstrip('.')
        if t:
            out.append(t)
    return out


def key(name):
    return tuple(tokens(name))


def name_key(name):
    """A name compared as written, bar case and spacing. Stricter than the token key, which makes
    'C--' the same as 'C'."""
    return ' '.join(name.lower().split())


def strip_software(name):
    return re.sub(r'\s+software$', '', name).strip()


def split_acronym(name):
    """'Integrated development environment IDE' -> ('Integrated development environment', 'IDE'),
    or None when no word is the initials of the words before it."""
    words = name.split()
    for i, w in enumerate(words):
        if i >= 1 and len(w) >= 2 and re.fullmatch(r'[A-Z][A-Z0-9&/-]*s?', w):
            letters = re.sub(r'[^A-Z]', '', w.rstrip('s'))
            n = len(letters)
            if n >= 2 and i >= n and ''.join(x[0].upper() for x in words[i - n:i]) == letters:
                long_form = ' '.join(words[:i] + words[i + 1:])
                short_form = ' '.join(words[:i - n] + [w] + words[i + 1:])
                return long_form, short_form
    return None


def vendor_prefixed(name):
    """Whether the name starts with a vendor's, not counting a vendor's whole name ('Amazon Web
    Services')."""
    return name not in VENDORS and any(name.startswith(v + ' ') for v in VENDORS)


def strip_vendors(name):
    while vendor_prefixed(name):
        vendor = next(v for v in VENDORS if name.startswith(v + ' '))
        name = name[len(vendor) + 1:]
    return name


def variants(example):
    """The names an O*NET example may go by, in the order they are looked up in Wikidata: as
    written, split into long form and acronym, and without the vendor."""
    base = strip_software(example)
    out = [base] + list(split_acronym(base) or ())
    out += [strip_vendors(v) for v in out]
    return list(dict.fromkeys(out))


def surface_forms(example):
    """The variants a skill can be written as, longest first: none of them vendor-prefixed, and not
    the run-together 'long form ACRONYM' spelling."""
    base = strip_software(example)
    run_together = base if split_acronym(base) else None
    forms = [v for v in variants(example) if not vendor_prefixed(v) and v != run_together]
    return sorted(forms, key=len, reverse=True)


def clean_aliases(aliases):
    """Wikidata aliases without the noisy ones: 'Python (lang)', '.yml', 'Python programming
    language', and any that has no word in it."""
    return [a.strip() for a in aliases
            if '(' not in a and not a.strip().startswith('.')
            and 'programming language' not in a.lower() and key(a)]


def resolve(example, lookup):
    """The Wikidata item an O*NET example names: the most linked software item named by the
    earliest variant that names any."""
    for v in variants(example):
        found = [m for m in lookup.get(v, []) if m['software']]
        if found:
            return max(found, key=lambda m: (m['links'], m['item']))
    return None


class Candidate:

    def __init__(self, name, qid=None):
        self.name, self.qid, self.forms, self.sources = name, qid, [], set()
        self.df, self.form_df = 0, {}

    def keys(self):
        return {key(f) for f in self.forms}


class Candidates:
    """The candidates being built, indexed by Wikidata item and by the key of every surface form."""

    def __init__(self):
        self.all, self.by_qid = [], {}
        self.by_name, self.by_key = collections.defaultdict(list), collections.defaultdict(list)

    def new(self, name, qid=None):
        c = Candidate(name, qid)
        self.all.append(c)
        if qid:
            self.by_qid[qid] = c
        self.by_name[name_key(name)].append(c)
        self.add_forms(c, [name])
        return c

    def for_item(self, qid, label):
        return self.by_qid.get(qid) or self.new(label, qid)

    def add_forms(self, c, forms):
        """Adds the forms not yet held. One the tokenizer cannot spell is not a form: 'A♯' would be
        matched as the article 'a'."""
        for f in forms:
            k = key(f)
            if k and f.isascii() and k not in c.keys():
                c.forms.append(f)
                self.by_key[k].append(c)

    def named(self, name, forms):
        """The one candidate already named so, else the one already written as the name or one of
        the forms, or a new one when none or several are."""
        same_name = self.by_name.get(name_key(name), [])
        if len(same_name) == 1:
            return same_name[0]
        hits = list(dict.fromkeys(c for f in [name] + forms for c in self.by_key.get(key(f), [])))
        return hits[0] if len(hits) == 1 else self.new(name)

    def collides(self, c, form):
        return any(other is not c for other in self.by_key.get(key(form), []))


def build_candidates(wikidata, onet, lookup, linguist):
    """Candidates from the Wikidata class rows, the O*NET examples (example -> commodity titles),
    the Wikidata matches of each O*NET variant, and Linguist's languages. Merged by Wikidata item
    first, then by normalized name. Linguist aliases are editor-mode ids, so none merges two
    candidates: one is kept only when no other candidate is written that way."""
    cs = Candidates()
    for row in sorted(wikidata, key=lambda r: r['item']):
        c = cs.for_item(row['item'], row['label'])
        cs.add_forms(c, clean_aliases(row['aliases']))
        c.sources.add('wikidata:' + row['class'])
    unresolved = []  # (forms, the name first; commodity titles)
    for example, commodities in onet.items():
        if example in MERGE:
            unresolved.append((MERGE[example], commodities))
            continue
        forms = surface_forms(example)
        m = resolve(example, lookup)
        if m:
            c = cs.for_item(m['item'], m['label'])
            cs.add_forms(c, clean_aliases(m['aliases']) + forms)
            c.sources |= {'onet:' + t for t in commodities}
        elif forms:
            # Named by its shortest form, which drops the long form an acronym was split from.
            unresolved.append((forms[-1:] + forms[:-1], commodities))
    for forms, commodities in unresolved:
        c = cs.named(forms[0], forms)
        cs.add_forms(c, forms)
        c.sources |= {'onet:' + t for t in commodities}
    for name, language in linguist.items():
        c = cs.named(name, [])
        cs.add_forms(c, [a for a in language.get('aliases') or [] if not cs.collides(c, a)])
        c.sources.add('linguist:' + language['type'])
    return cs.all


def document_frequency(candidates, docs):
    """Sets each candidate's document frequency, a document counted once whichever of its forms it
    holds, and each form's own. Forms are matched as token n-grams."""
    by_key = collections.defaultdict(list)
    for c in candidates:
        for f in c.forms:
            by_key[key(f)].append((c, f))
    lengths = collections.defaultdict(set)
    for k in by_key:
        lengths[k[0]].add(len(k))
    df, form_df = collections.Counter(), collections.Counter()
    for doc in docs:
        found = set()
        for i, t in enumerate(doc):
            for n in lengths.get(t, ()):
                found.update(by_key.get(tuple(doc[i:i + n]), ()))
        df.update({id(c) for c, _ in found})
        form_df.update((id(c), f) for c, f in found)
    for c in candidates:
        c.df = df[id(c)]
        c.form_df = {f: form_df[(id(c), f)] for f in c.forms}


# --- Sources ----------------------------------------------------------------------------------

def say(message):
    print(f'[{datetime.datetime.now():%H:%M:%S}] {message}', file=sys.stderr)


def get(url, data=None):
    request = urllib.request.Request(url, data=data, headers={'User-Agent': USER_AGENT})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=180) as response:
                return response.read()
        except (OSError, http.client.HTTPException) as e:
            # A 4xx other than being rate limited is a bad request, which asking again won't fix.
            if isinstance(e, urllib.error.HTTPError) and 400 <= e.code < 500 and e.code != 429:
                raise SystemExit(f'{url[:80]} refused the request: {e}')
            say(f'retrying {url[:80]}: {e}')
            time.sleep(10 * (attempt + 1))
    raise SystemExit(f'giving up on {url}')


def save(path, data):
    """Writes the whole file or nothing, so an interrupted run never leaves half a source behind."""
    with open(path + '.tmp', 'wb') as f:
        f.write(data)
    os.replace(path + '.tmp', path)


def pinned(source):
    path = os.path.join(TAXONOMY_DIR, source['file'])
    if not os.path.exists(path):
        say(f'downloading {source["title"]}')
        data = get(source['url'])
        digest = hashlib.sha256(data).hexdigest()
        if digest != source['sha256']:
            raise SystemExit(f'{source["url"]} is not the pinned release: sha256 {digest}')
        save(path, data)
    return path


def sparql(query):
    body = urllib.parse.urlencode({'query': query, 'format': 'json'}).encode()
    bindings = json.loads(get('https://query.wikidata.org/sparql', body))['results']['bindings']
    time.sleep(1)
    return bindings


def kept(file, pull):
    """What a pull answered, read from where it was kept, or pulled and kept now."""
    path = os.path.join(TAXONOMY_DIR, file)
    if not os.path.exists(path):
        save(path, json.dumps({'pulled': datetime.date.today().isoformat(), 'answer': pull()}).encode())
    with open(path) as f:
        return json.load(f)


def wikidata_item(b):
    """The fields of a Wikidata answer row that every pull shares."""
    return {'item': b['item']['value'].rsplit('/', 1)[1], 'label': b['label']['value'],
            'links': int(b['links']['value']),
            'aliases': [a for a in b['aliases']['value'].split('|') if a]}


def wikidata_classes():
    rows, pulled = [], set()
    for qid, name, transitive in WIKIDATA_CLASSES:
        query = f'''SELECT ?item ?label ?links (GROUP_CONCAT(DISTINCT ?alias; separator="|") AS ?aliases) WHERE {{
          ?item wdt:P31{'/wdt:P279*' if transitive else ''} wd:{qid} .
          ?item wikibase:sitelinks ?links . FILTER(?links >= 5)
          ?item rdfs:label ?label . FILTER(lang(?label) = "en")
          OPTIONAL {{ ?item skos:altLabel ?alias . FILTER(lang(?alias) = "en") }}
        }} GROUP BY ?item ?label ?links'''
        say(f'Wikidata class {name}')
        pull = kept(f'wikidata-{qid}.json', lambda: sparql(query))
        pulled.add(pull['pulled'])
        for b in pull['answer']:
            rows.append(wikidata_item(b) | {'class': name})
    return rows, pulled


def wikidata_lookup(labels):
    """label -> the Wikidata items labelled or aliased that way in English. Each label is kept as
    pulled, so only labels never looked up are pulled."""
    path = os.path.join(TAXONOMY_DIR, 'wikidata-labels.json')
    kept_labels = {}
    if os.path.exists(path):
        with open(path) as f:
            kept_labels = json.load(f)
    todo = sorted(set(labels) - kept_labels.keys())
    for i in range(0, len(todo), 150):
        batch = todo[i:i + 150]
        values = ' '.join(json.dumps(label) + '@en' for label in batch)
        say(f'Wikidata labels {i + len(batch)}/{len(todo)}')
        answer = sparql(f'''SELECT ?l ?item ?label ?links ?software (GROUP_CONCAT(DISTINCT ?alias; separator="|") AS ?aliases) WHERE {{
          VALUES ?l {{ {values} }}
          {{ ?item rdfs:label ?l }} UNION {{ ?item skos:altLabel ?l }}
          ?item wikibase:sitelinks ?links .
          ?item rdfs:label ?label . FILTER(lang(?label) = "en")
          BIND(EXISTS {{ ?item {SOFTWARE_PROPERTIES} ?x }} AS ?software)
          OPTIONAL {{ ?item skos:altLabel ?alias . FILTER(lang(?alias) = "en") }}
        }} GROUP BY ?l ?item ?label ?links ?software''')
        today = datetime.date.today().isoformat()
        for label in batch:
            kept_labels[label] = {'pulled': today, 'matches': []}
        for b in answer:
            kept_labels[b['l']['value']]['matches'].append(
                wikidata_item(b) | {'software': b['software']['value'] == 'true'})
        save(path, json.dumps(kept_labels).encode())
    lookup = {label: sorted(kept_labels[label]['matches'], key=lambda m: m['item']) for label in labels}
    return lookup, {kept_labels[label]['pulled'] for label in labels}


def onet_examples(path):
    """Engineering example -> the commodity titles O*NET files it under."""
    out = collections.defaultdict(set)
    with open(path, newline='') as f:
        rows = csv.reader(f, delimiter='\t', quoting=csv.QUOTE_NONE)
        next(rows)
        for soc, example, _, commodity, _, _ in rows:
            if ENGINEERING_SOC.match(soc) or ENGINEERING_COMMODITY.match(commodity):
                out[example].add(commodity)
    return out


def psql(sql):
    return subprocess.run(
        ['docker', 'compose', '--project-directory', REPO, 'exec', '-T', 'postgres',
         'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'oneprofile', '-d', 'oneprofile', '-At', '-c', sql],
        check=True, capture_output=True, text=True).stdout


def corpus():
    """The IN descriptions of the development database, as tokens, and the date of the newest
    update among them."""
    counted = """from vacancy v join normalized_vacancy n on n.vacancy_id = v.id
                 where n.classification_state = 'IN' and v.description is not null"""
    out = psql(f'copy (select v.description {counted} order by n.id) to stdout with csv')
    csv.field_size_limit(sys.maxsize)
    docs = [tokens(row[0]) for row in csv.reader(out.splitlines(keepends=True))]
    newest = psql(f'select max(v.updated_at)::date {counted}').strip()
    return docs, newest


def write(path, candidates, header):
    with open(path, 'w') as f:
        for line in header:
            f.write(f'# {line}\n')
        f.write('df\tname\tqid\tsources\tforms\n')
        for c in sorted(candidates, key=lambda c: (-c.df, c.name.lower(), c.qid or '')):
            forms = '|'.join(f'{f}={c.form_df[f]}' for f in c.forms)
            f.write(f'{c.df}\t{c.name}\t{c.qid or ""}\t{"|".join(sorted(c.sources))}\t{forms}\n')


def main():
    if len(sys.argv) != 2:
        raise SystemExit('usage: taxonomy_candidates.py <out.tsv>')
    os.makedirs(TAXONOMY_DIR, exist_ok=True)
    onet = onet_examples(pinned(ONET))
    with open(pinned(LINGUIST)) as f:
        linguist = yaml.safe_load(f)
    wikidata, class_pulls = wikidata_classes()
    lookup, label_pulls = wikidata_lookup({v for example in onet for v in variants(example)})
    candidates = build_candidates(wikidata, onet, lookup, linguist)
    say(f'{len(candidates)} candidates; reading the corpus')
    docs, newest = corpus()
    say(f'counting over {len(docs)} descriptions')
    document_frequency(candidates, docs)
    write(sys.argv[1], candidates, [
        'Taxonomy candidates (issue #37), written by scripts/taxonomy_candidates.py.',
        f'Corpus: the {len(docs)} IN descriptions of the development database, newest vacancy update {newest}.',
        f'{ONET["title"]}: {ONET["url"]} sha256 {ONET["sha256"]}',
        f'{LINGUIST["title"]}: {LINGUIST["url"]} sha256 {LINGUIST["sha256"]}',
        f'Wikidata (CC0): class pulls on {", ".join(sorted(class_pulls))}, label lookups on {", ".join(sorted(label_pulls))}.',
        'df is the number of descriptions naming the candidate by any of its forms; each form=N is that form alone.',
    ])
    say(f'wrote {len(candidates)} candidates to {sys.argv[1]}')


if __name__ == '__main__':
    main()
