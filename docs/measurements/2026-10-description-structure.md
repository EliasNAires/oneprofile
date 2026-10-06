# How much structure Greenhouse descriptions carry

Step 1 of #46: before the reader keeps a description's structure, count how much there is to keep,
and how far the job's `language` field can be trusted.

Run on the development machine on 2026-10-06, over the raw corpus snapshot `raw-2026-09-20.dump`.
Two samples were drawn, each of 300 vacancies at random and at most two per company:

- **The pile**, the population that matters: the 21 456 vacancies the title stage left `UNKNOWN`
  for `DOMAIN_AMBIGUITY` or `SCOPE_AMBIGUITY`, which the body pass reads. Drawn with
  `setseed(0.47)`, from 292 companies; the body pass had decided 111 of them `IN` and 189 `OUT`,
  close to the pile's own 36% and 64%.
- **The IN vacancies**, for comparison, since skill extraction (#15) reads them: the 34 368
  vacancies `classification_state = 'IN'`, decided by title or by body. Drawn with
  `setseed(0.46)`, from 289 companies. 67 of them were pile vacancies the body had decided `IN`; the
  pile sample excludes those.

Each vacancy was re-fetched from `boards-api.greenhouse.io/v1/boards/<slug>/jobs/<id>`, one request
at a time, and the raw HTML in `content` was measured with
`scripts/measure-description-structure.py`, which is pure over the responses. The responses
themselves are not kept in the repo.

| | Pile | IN |
| --- | ---: | ---: |
| Drawn | 300 | 300 |
| Gone (HTTP 404) | 73 (24.3%) | 67 (22.3%) |
| **Live, and measured** | **227** | **233** |
| Carrying Greenhouse boilerplate blocks | 143 (63.0%) | 140 (60.1%) |

Nearly a quarter gone sixteen days after the snapshot is a sampling loss here, and a preview of how
different the re-sweep #46 calls for will be from the corpus it replaces.

## Structure

A description is **real** when it carries a heading tag (`<h1>`–`<h6>`) or a list item (`<li>`);
**fake** when it has neither but marks structure some other way (a paragraph that is all bold and
80 characters or fewer, at least two lines led by a typed bullet, a block split by `<br>` into three
or more short lines, or a short heading-like line); **none** otherwise.

Greenhouse lets a board wrap every ad in an intro, a pay-transparency note and a conclusion
(`<div class="content-intro">` and its two siblings). They are the company's text, not the role's,
so every count below is over the body with those blocks removed. Brackets are 95% Wilson
intervals.

| | Pile | IN |
| --- | ---: | ---: |
| **Real** | **223 (98.2%)** [95.6–99.3] | **230 (98.7%)** |
| &nbsp;&nbsp;headings and lists | 85 | 107 |
| &nbsp;&nbsp;lists, no heading tags | 137 (60.4%) [53.9–66.5] | 122 (52.4%) |
| &nbsp;&nbsp;heading tags, no lists | 1 | 1 |
| Fake | 3 | 3 |
| None | 1 | 0 |
| At least one bold paragraph acting as a heading | 173 (76.2%) [70.3–81.3] | 161 (69.1%) |

Real structure is close to universal, but it is lists, not headings. Three in five pile ads carry
no heading tag at all, and every one of those 137 heads its sections some other way: 130 with a
bold paragraph, the rest with a short line such as "Requirements:". Across the board, three in four
pile ads use bold paragraphs as headings, beside heading tags or in place of them.

How much of an ad sits inside list items is where real and complete part ways:

| Share of the body's text inside `<li>` | Pile | IN |
| --- | ---: | ---: |
| Median | 61% | 61% |
| First quartile | 50% | 49% |
| Under 25% | 11 (4.8%) [2.7–8.5] | 15 (6.4%) |
| Under 10% | 5 (2.2%) [0.9–5.1] | 9 (3.9%) |

The pile ads with the least in lists show every way the structure is faked:

- **Typed bullets under real headings.** AST SpaceMobile has `<h3>` headings, but each item is a
  `<p>` led by `•`, some joined by `<br>`.
- **One paragraph holding a whole section.** Nova 401(k) writes
  `<p><strong>Job Responsibilities:</strong><br>• item<br>• item…`.
- **A Word paste.** SNS One leads every item with `·` and a run of `&nbsp;`.
- **Numbered paragraphs.** Hawthorne Machinery writes 46 duties as `<p>1. …</p>` under ALL-CAPS
  bold headings, and keeps its only `<ul>` for benefits. Four pile ads carry three or more
  numbered paragraphs; no IN ad does.
- **Bold headings over plain paragraphs.** WebChart, Thermal Works, Who Gives A Crap.
- **Nothing.** Meridial's only structure is a metadata block; Critical Mass's whole body is
  `<p>Project Manager profiles</p>`.

The other fake markers:

| | Pile | IN |
| --- | ---: | ---: |
| At least two lines led by a typed bullet, outside `<li>` | 4 (1.8%) [0.7–4.4] | 9 (3.9%) |
| A block split by `<br>` into three or more short lines | 12 (5.3%) [3.0–9.0] | 14 (6.0%) |

Most `<br>` blocks are not lists of requirements but metadata: "Location: …", "Clearance required:
…", "Employment type: …", or pay zones. Heading tags are not always headings either: one IN ad
(Innovid) puts paragraphs of intro prose inside `<h5><strong>`, and one pile ad (Caronsale) carries
empty `<h3>&nbsp;</h3>`.

Inside the pile, the ads the body went on to decide `IN` and those it decided `OUT` are written
alike: lists without heading tags 57.8% against 61.8%, bold headings 72.3% against 78.5%, under a
quarter of the text in lists 2.4% against 6.2%, with intervals that overlap throughout.

### Counted per company, not per ad

At most two ads a company makes these rates a share of companies. Ads are concentrated: in the pile,
the 50 largest of 2959 companies hold 24% of the vacancies. Weighting each sampled ad by its
company's pile size gives a rough per-ad rate:

| Pile | Per company | Per ad, weighted |
| --- | ---: | ---: |
| Real | 98.2% | 99.1% |
| Lists, no heading tags | 60.4% | 66.9% |
| Bold paragraphs as headings | 76.2% | 74.5% |
| Under 10% of the text in lists | 2.2% | 3.4% |
| Typed bullets | 1.8% | 3.0% |

The weighting moves the rates by a few points, and in the IN sample it moves them the other way
(lists without heading tags fall from 52.4% to 36.1%). No rate moves enough to change what follows.

## Language

The `language` tag was present on every live ad, and was compared with a stopword count over the
stripped text in English, Spanish, German, French and Portuguese.

| Tag | Pile | Written in another language | IN | Written in another language |
| --- | ---: | --- | ---: | --- |
| `en` | 222 | 0 | 226 | 1, in Portuguese (Capco) |
| `es` | 1 | 0 | 0 | |
| `de` | 2 | 0 | 2 | 0 |
| `fr` | 1 | 0 | 4 | 2, in English (Mirakl Labs, FlightHub) |
| `pt` | 1 | 0 | 1 | 0 |
| None | 0 | | 0 | |

No pile tag is wrong (0 of 227, at most 1.7%); three IN tags are, in both directions. Over both
samples that is 3 in 460 (0.7%).

The same stopword count over all 21 456 pile descriptions as stored (flattened, so the count is
rough, and Spanish and Portuguese share words):

| Detected | Pile vacancies |
| --- | ---: |
| English | 20 577 (95.9%) |
| Portuguese | 386 (1.8%) |
| German | 195 (0.9%) |
| French | 118 (0.6%) |
| Spanish | 106 (0.5%) |
| Too short to tell | 74 (0.3%) |

## What the numbers decide

The effort belongs in the reader. Almost every ad carries real tags, so emitting one line per HTML
block recovers most of the structure on its own. Three things the reader has to do beyond that:

- **Treat a bold-only short paragraph as a heading.** For three in five pile ads it is the only
  heading there is.
- **Treat `<br>` as a line break.** Bullets and short lines written inside one paragraph are
  otherwise glued back together.
- **Mark the boilerplate blocks.** Over 60% of ads carry them, and they bring headings and lists
  that belong to the company, not the role.

The splitter stays as the fallback #46 describes, for a few percent of ads: typed bullets outside
lists, and the numbered paragraphs `1.` / `1)` that the issue's bullet list does not name yet. In
the ads where lists hold under a tenth of the text, those lines are the role. Sentence splitting is
still needed inside paragraphs. A heading tag should be read as a heading only when it is short
and not empty.

The `language` tag is reliable enough to gate the body pass on, as #46 proposes. The English and
Spanish scope skips about 3.3% of the pile, and Spanish is about 0.5% of it, so the Spanish heading
words the splitter carries matter little today.
