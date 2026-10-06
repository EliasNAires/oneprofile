"""Measures how much structure Greenhouse descriptions carry, for #46 step 1.

Usage: measure-description-structure.py <out.json> <raw dir>...

Each raw dir holds one Greenhouse job response per file, named <vacancy_id>.json, as fetched from
boards-api.greenhouse.io/v1/boards/<slug>/jobs/<id>. A 404 is stored as {"_status": 404} or as a
<vacancy_id>.gone marker. It prints the counts in
docs/measurements/2026-10-description-structure.md and writes <out.json>, one row per vacancy.
"""
import glob, html, json, os, re, statistics, sys
from collections import Counter
from html.parser import HTMLParser

VOID = {"br", "img", "hr", "meta", "input", "wbr"}
BLOCK = {"p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "td", "th", "tr", "ul", "ol", "table", "section", "blockquote"}
BOILER = {"content-intro", "content-pay-transparency", "content-conclusion"}
BULLET = re.compile(r"^\s*(?:[•·▪◦●‣]|[-*–]\s)")
HEADWORDS = re.compile(r"^(about|requirements?|responsibilities|qualifications|what you|who you|nice to have|bonus|preferred|benefits|perks|the role|your role|requisitos|responsabilidades|deseable|beneficios|qué|quién|sobre|ofrecemos)", re.I)
STOP = {
    "en": "the and to of a in for with you is are we our your on as be will this that or".split(),
    "es": "el la los las de y en que para con un una por del se es su nuestro tu como".split(),
    "de": "der die das und mit für wir sie ist ein eine zu von auf den bei".split(),
    "fr": "le les des et pour avec vous nous est une dans sur du au vos".split(),
    "pt": "o os as e para com você nós é um uma em do da no na seu".split(),
}


class Node:
    def __init__(self, tag, attrs, parent):
        self.tag, self.attrs, self.parent, self.kids = tag, dict(attrs), parent, []


class Tree(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node("root", [], None)
        self.cur = self.root

    def handle_starttag(self, tag, attrs):
        n = Node(tag, attrs, self.cur)
        self.cur.kids.append(n)
        if tag not in VOID:
            self.cur = n

    def handle_endtag(self, tag):
        c = self.cur
        while c is not self.root and c.tag != tag:
            c = c.parent
        if c is not self.root:
            self.cur = c.parent

    def handle_data(self, data):
        self.cur.kids.append(data)


def text(n):
    return "".join(k if isinstance(k, str) else ("\n" if k.tag == "br" else text(k)) for k in n.kids)


def walk(n, skip_boiler):
    for k in n.kids:
        if isinstance(k, Node):
            if skip_boiler and k.tag == "div" and BOILER & set((k.attrs.get("class") or "").split()):
                continue
            yield k
            yield from walk(k, skip_boiler)


def inside(n, tags):
    p = n.parent
    while p is not None:
        if p.tag in tags:
            return True
        p = p.parent
    return False


def leaf_blocks(root, skip_boiler):
    """Blocks with no block descendants: the units a reader would emit as lines."""
    out = []
    for n in walk(root, skip_boiler):
        if n.tag in BLOCK and not any(isinstance(d, Node) and d.tag in BLOCK for d in walk(n, False)):
            out.append(n)
    return out


def all_bold(n):
    t = text(n).strip()
    if not t:
        return False
    bold = "".join(text(d) for d in walk(n, False) if d.tag in ("strong", "b") and not inside(d, ("strong", "b")))
    return len(bold.strip()) >= len(t) * 0.95


def lang(t):
    words = re.findall(r"[a-záéíóúñüàâçèêôãõäöß]+", t.lower())
    c = Counter(words)
    score = {k: sum(c[w] for w in v) for k, v in STOP.items()}
    best = max(score, key=score.get)
    return best if score[best] >= 5 else "unknown"


def measure(content, skip_boiler):
    t = Tree()
    t.feed(html.unescape(content))
    nodes = list(walk(t.root, skip_boiler))
    blocks = leaf_blocks(t.root, skip_boiler)
    lines = [(b, l.strip()) for b in blocks for l in text(b).split("\n") if l.strip()]
    total = sum(len(l) for _, l in lines) or 1
    in_li = sum(len(l) for b, l in lines if b.tag == "li" or inside(b, ("li",)))
    r = {
        "h": any(n.tag in ("h1", "h2", "h3", "h4", "h5", "h6") for n in nodes),
        "li": any(n.tag == "li" for n in nodes),
        "li_share": in_li / total,
        "bold_head": sum(1 for b in blocks if b.tag in ("p", "div") and all_bold(b) and len(text(b).strip()) <= 80),
        "word_head": sum(1 for b, l in lines if b.tag not in ("li", "h1", "h2", "h3", "h4", "h5", "h6") and len(l) <= 60 and (HEADWORDS.match(l) or l.endswith(":"))),
        "bullets": sum(1 for b, l in lines if not (b.tag == "li" or inside(b, ("li",))) and BULLET.match(l)),
        "br_lists": sum(1 for b in blocks if b.tag != "li" and len([s for s in text(b).split("\n") if s.strip()]) >= 3
                        and statistics.median(len(s.split()) for s in text(b).split("\n") if s.strip()) <= 15),
        "lines": len(lines),
    }
    r["category"] = "real" if r["h"] or r["li"] else ("fake" if r["bold_head"] or r["bullets"] >= 2 or r["br_lists"] or r["word_head"] else "none")
    r["detected"] = lang(" ".join(l for _, l in lines))
    return r


rows = []
for d in sys.argv[2:]:
    for f in sorted(glob.glob(f"{d}/*.json")):
        j = json.load(open(f))
        vid = int(os.path.basename(f)[:-5])
        if "_status" in j or "content" not in j:
            rows.append({"vid": vid, "gone": True}); continue
        rows.append({"vid": vid, "gone": False, "tag": (j.get("language") or "null").split("-")[0].lower(),
                     "company": j.get("company_name"), "full": measure(j["content"], False), "body": measure(j["content"], True)})
    for f in glob.glob(f"{d}/*.gone"):
        rows.append({"vid": int(os.path.basename(f)[:-5]), "gone": True})

json.dump(rows, open(sys.argv[1], "w"), indent=1)
live = [r for r in rows if not r["gone"]]
print("rows", len(rows), "live", len(live), "gone", len(rows) - len(live))
for view in ("full", "body"):
    m = [r[view] for r in live]
    print(f"\n== {view} ==")
    print("category", Counter(x["category"] for x in m))
    print("h+li", sum(x["h"] and x["li"] for x in m), "li only", sum(x["li"] and not x["h"] for x in m),
          "h only", sum(x["h"] and not x["li"] for x in m), "neither", sum(not x["h"] and not x["li"] for x in m))
    print("li-only rows with bold heads", sum(x["li"] and not x["h"] and x["bold_head"] > 0 for x in m),
          "| with word heads", sum(x["li"] and not x["h"] and (x["bold_head"] or x["word_head"]) > 0 for x in m))
    print("any bold heads", sum(x["bold_head"] > 0 for x in m), "typed bullets>=2", sum(x["bullets"] >= 2 for x in m),
          "br lists", sum(x["br_lists"] > 0 for x in m))
    sh = sorted(x["li_share"] for x in m)
    print("li share median %.2f  p25 %.2f  <25%%: %d  <10%%: %d" % (statistics.median(sh), sh[len(sh) // 4],
          sum(s < .25 for s in sh), sum(s < .10 for s in sh)))
    print("real rows with >=2 typed bullets outside li", sum(x["category"] == "real" and x["bullets"] >= 2 for x in m))
print("\ntags", Counter(r["tag"] for r in live))
print("detected", Counter(r["full"]["detected"] for r in live))
mis = [(r["vid"], r["company"], r["tag"], r["full"]["detected"]) for r in live if r["tag"] != r["full"]["detected"]]
print("mismatch", len(mis), mis)
