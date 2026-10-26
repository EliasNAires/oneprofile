# A second check of the title breakdown

The check #94 asked for before the role list (#25) is built on the breakdown (#92), run on
2026-10-26 against the dev database restored from `~/oneprofile-snapshots/classified-2026-10-25.dump`:
the 155,767 vacancies of the engineering subset. It answers to `docs/title-breakdown-criterion.md`,
written first, from #92, ADR-0024 and the choices in `2026-10-title-breakdown.md`. In this order: the
fragmentation was measured and its real duplicates merged in the rule; the titles not in English were
counted; and a blind check was run last, on the rule as it then stood. Nothing was changed after the
check. Its files are in `2026-10-title-breakdown-check/`.

## Fragmentation

`scripts/title-fragmentation` takes the top 2,000 normalized titles and groups those that share the
same set of words, and pairs those with the same last word whose vacancies' skill profiles are
nearly the same (cosine of the share of vacancies naming each skill, at least 0.95).

**The same words in another order: 54 groups.** Some name the same work (`qa automation engineer`
and `automation qa engineer`, `ai ml engineer` and `ml ai engineer`). Some do not: an `application
security engineer` secures applications, a `security application engineer` builds them for security.

**Near-identical skill profiles: 25 pairs at 0.95.** Almost all are different work that asks for the
same skills (`platform engineer` and `site reliability engineer`, `devops engineer` and `devsecops
engineer`), which is the role list's to sort and not a duplicate. The duplicates among them were
spellings and abbreviations: `sre engineer` against `site reliability engineer`, `system
administrator` against `systems administrator`, `quality assurance engineer` against `qa engineer`,
and `backend software engineer` against `backend engineer`.

Merged in the rule (criterion clauses 13 to 17):

- **Spellings**: `cyber security` → `cybersecurity`, `system` → `systems`, `pre sales` →
  `presales`, a plural yielding head to its singular (`experts` → `expert`).
- **An abbreviation and its long form**, written the way most of the subset's titles write it,
  counted over all normalized titles: `ml` → `machine learning` (2,349 against 1,535), `sre` → `site
  reliability` (1,285 against 189), `bi` → `business intelligence` (252 against 208), `dba` →
  `database administrator` (380 against 73); and the other way, `quality assurance` → `qa` (1,227
  against 345), `generative ai` and `gen ai` → `genai`, `artificial intelligence` → `ai`,
  `information technology` → `it`, `user experience` → `ux`. A bare `SRE` is a `site reliability
  engineer`. Skill keys are respelled the same way, so `Power BI` is still found as a skill.
- **`software` before a kind that is only software work** (backend, frontend, full stack, web,
  mobile, Android, iOS, React Native, Flutter, UI) goes: `Backend Software Engineer` → `backend
  engineer`, as `Software Engineer, Backend` already read. It stays before any other kind
  (`embedded software engineer`).
- **No word twice running, nor one the head names**: `Senior Product Manager, Product Foundations`
  was `product product manager`.
- **A table of 41 fronts** written in more than one order for the same work, each in the order most
  titles use (`qa automation`, `cloud infrastructure`, `agentic ai`, …). Those whose order changes
  the work are left apart.

7,896 vacancies (5.1%) changed normalized title. No management level changed, and the 370 vacancies
with no normalized title are the same.

| ATS | Distinct before | after | Top 100 before | after | Top 300 before | after | Top 1,000 before | after |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Greenhouse | 7,536 | 7,355 | 56.9% | 58.1% | 68.9% | 69.9% | 79.6% | 80.4% |
| Ashby | 3,325 | 3,238 | 68.6% | 69.5% | 80.2% | 81.1% | 89.5% | 90.0% |
| Lever | 2,722 | 2,645 | 58.5% | 59.8% | 73.3% | 74.3% | 86.5% | 87.1% |
| Workday | 19,843 | 19,438 | 42.0% | 43.2% | 52.5% | 53.8% | 63.4% | 64.5% |
| **All** | **27,947** | **27,361** | **49.1%** | **50.4%** | **60.2%** | **61.3%** | **70.1%** | **71.1%** |

Both columns are counted by the script. Its count of the run before differs from
`2026-10-title-breakdown.md`'s by up to 0.4 points (that report gives 27,948 and 60.4%), because that
count took the 370 vacancies with no normalized title as one more title, an empty one.

**Fragmentation was small**: the distinct titles fell by 2.1%, and the top 300 gained 1.1 points.
What is left in the top 2,000 is 12 groups of the same words, each kept apart because the order
changes the work (`systems software engineer` against `software systems engineer`).

## Titles not in English

The vacancy's detected language is the description's, not the title's, and most vacancies with a
description in another language have a title in English: of 235 Italian ones, almost none is in
Italian. And 41,512 vacancies, mostly Workday's, have no description and so no detected language.
So a title was counted as not English when it holds a word that is common in the titles of one
language's vacancies and rare in English ones (`ingénieur`, `desarrollador`, `praktikum`,
`estágio`), or is written in Japanese, Chinese or Korean with no English word beside it. A title
with none of the words is missed, so these figures are a floor.

**1,841 vacancies (1.2% of the subset)** have a title not in English.

| ATS | Vacancies | Share of the ATS |
|---|---:|---:|
| Greenhouse | 384 | 0.9% |
| Ashby | 24 | 0.1% |
| Lever | 179 | 1.4% |
| Workday | 1,254 | 1.6% |

| Title's language | Vacancies | With the description's detected language the same | No detected language | Description in English |
|---|---:|---:|---:|---:|
| French | 1,041 | 913 | 87 | 39 |
| Portuguese | 336 | 105 | 192 | 39 |
| Spanish | 213 | 129 | 53 | 20 |
| German | 141 | 100 | 20 | 15 |
| Spanish or Portuguese (`analista`, `especialista`, `consultor`) | 103 | 52 | 25 | 26 |
| Polish | 4 | 4 | — | — |
| Korean | 3 | — | 2 | 1 |

The few left over have a description in a third language: 10 Spanish titles with Portuguese
descriptions, 6 German ones with French, and the like.

The commonest words: `ingénieur` 394, `développeur` 311, `logiciel` 193, `développement` 142,
`analista` 121, `cdi` 95, `dados` 93, `engenheiro` 89, `confirmé` 84, `desenvolvedor` 77, `pleno` 75,
`ingeniero` 71, `especialista` 70, `desarrollador` 60, `responsable` 53, `expérimenté` 41,
`informatique` 40, `praktikum` 37, `systèmes` 37, `stagiaire` 32, `embarqué` 32, `cientista` 30,
`datos` 30, `werkstudent` 29, `alternance` 29.

The rule reads them badly: mostly a foreign head with what stood in front of it (`Engenheiro de
Software Sênior` → `software engenheiro`, `CIENTISTA DE DADOS PLENO II` → `cientista`). 430 of the
1,841 also hold an English head (`Développeur.se Fullstack / Senior Fullstack Developer`).

**The proposal**, for Elias to choose between:

- **Leave them without a normalized title.** 1.2% of the subset goes without a role, and the
  Finder's role filter counts them among those it cannot see. Nothing to maintain.
- **A small table of the commonest words**, about 60 words over nine languages, read before the
  head: heads (`ingénieur`, `ingeniero`, `engenheiro`, `ingenieur` → engineer; `développeur`,
  `desarrollador`, `desenvolvedor`, `entwickler` → developer; `analista` → analyst; `cientista` →
  scientist; `arquitecto` → architect; `consultor` → consultant; `especialista` → specialist), the
  stages (`stage`, `stagiaire`, `estágio`, `praktikum`, `werkstudent`, `beca`, `alternance` →
  intern), the kinds of work (`logiciel` → software, `données`, `datos`, `dados` → data,
  `systèmes`, `sistemas` → systems, `réseaux`, `redes` → network, `embarqué` → embedded), and the
  seniority words (`confirmé`, `expérimenté`, `pleno`, `sênior`). Since the rule already moves a kind
  from behind the head, `Engenheiro de Dados` would read as `data engineer`. The heads alone reach
  1,322 of the 1,841 titles that have no English head; 89 have neither.

The table is the recommendation: it is about 60 words, it reaches three in four of them, and 1,041
of the 1,841 are French, which a Finder serving France would show.

## The blind check

**The draw.** 210 vacancies, none of them among the 200 that #92's check drew, 70 in each band of
how many vacancies carry the vacancy's (first) normalized title, counted after the merges above:

| Band | Its share of the subset |
|---|---:|
| Titles on 12 vacancies or more | 71.1% |
| On 5 to 11 | 6.8% |
| On fewer than 5, or none | 22.1% |

Within a band the draw is uniform over vacancies, so a band's ATS mix is the subset's: 117 of the
210 are Workday's.

**The labelling.** Two Sonnet agents labelled 105 each from the criterion, seeing the title, the
location field and the title's skills, never the rule's answer. One title was marked not English
(`STAGE - Ingénieure / Ingénieur développement logiciel DATA et IA…`) and is left out of the title
figures.

**The adjudication.** Each disagreement between a labeller and the rule went to the `adjudicator`
agent, not to this session, with the two answers unattributed and in a shuffled order. It decided
for the labeller or the rule, or for neither. A rule's answer that neither won counts as a fault.

The labellers disagreed with the rule on 60 normalized titles and one management level. The
adjudicator gave 26 of the titles to the rule, 32 to the labeller, and 2 to neither; and the level to
the labeller.

### Normalized titles

Per vacancy, with Wilson 95% intervals:

| Group | Vacancies | Faults | Rate | 95% interval |
|---|---:|---:|---:|---:|
| Titles on 12 or more | 70 | 2 | 2.9% | 0.8–9.8% |
| On 5 to 11 | 69 | 18 | 26.1% | 17.2–37.5% |
| On fewer than 5 | 70 | 14 | 20.0% | 12.3–30.8% |
| **The subset**, bands weighted by their share | | | **8.2%** | 4.7–11.7% |
| **On 5 or more**, weighted | | | **4.9%** | 1.2–8.6% |

By ATS, over the drawn rows (which overweight the two smaller bands, so these are not the ATS's own
rates, only how they compare):

| ATS | Vacancies | Faults | Rate | 95% interval |
|---|---:|---:|---:|---:|
| Greenhouse | 48 | 4 | 8.3% | 3.3–19.6% |
| Ashby | 33 | 8 | 24.2% | 12.8–41.0% |
| Lever | 12 | 1 | 8.3% | 1.5–35.4% |
| Greenhouse, Ashby and Lever | 93 | 13 | 14.0% | 8.4–22.5% |
| Workday | 116 | 21 | 18.1% | 12.2–26.1% |

**Per distinct title**: each drawn vacancy weighed by one over the vacancies its title carries, so
that each distinct title of the band counts once, with a bootstrap 95% interval:

| Band | Share of its distinct titles at fault | 95% interval |
|---|---:|---:|
| Titles on 12 or more | 9.3% | 0.0–29.6% |
| On 5 to 11 | 28.3% | 17.5–39.6% |
| On fewer than 5 | 22.0% | 12.2–32.8% |

The first is weighted almost wholly by the band's rarer titles and rests on two faults, hence its
width.

### Management levels

One fault in 210: `Ld Developer Analyst IT`, where `Ld` is a lead. 0 of 70 in each of the two
commoner bands (0.0–5.2%), 1 of 70 in the tail (0.3–7.7%); 0 of 93 outside Workday (0.0–4.0%), 1 of
117 in Workday (0.2–4.7%); 0.3% over the subset. Per distinct title, 0% in the two commoner bands
and 1.9% (0.0–5.9%) in the tail.

### The faults, grouped

The 34 title faults:

- **A word from behind the head (15).** The largest group, and the move #92 added.
  - **A team, a product or an attribute read as the kind of work and moved (9)**: `Senior Software
    Engineer, Perception ML` → `perception machine learning engineer`; `Technical Support Engineer
    - Sales Cloud` → `cloud technical support engineer`; `Mission Software Engineer, Mission
    Systems, Networking` → `systems mission engineer`; `Senior Technical Program Manager, ECU
    Reliability & Durability Validation` → `reliability technical program manager`; `Member of
    Technical Staff, Performance and Scale` → `performance member of technical staff`; `Software
    Engineer, Inference Infrastructure` → `inference infrastructure engineer`; `Engineering
    Manager, Identity Platform` → `identity platform engineer`; `E-Discovery Administrator
    (Security/FOIA/CAPSTONE)` → `security discovery administrator`; `Staff Software Engineer,
    Quality & Reliability Platform` → `platform reliability engineer`.
  - **A kind of work not moved (3)**: `Software Engineer, Desktop` → `software engineer`;
    `Advisor - AI/ML Generative Foundation Models` → `advisor`; `GPU Cluster Engineer, Systems &
    Platform` → `gpu cluster engineer`.
  - **Where a moved kind lands (3)**: the rule puts it before the whole front, the adjudicator read
    it as standing next to the head: `MS Engineer L2, Network` → `network ms engineer`; `Principal
    Technical Product Manager, Networking` → `networking technical product manager`; `Tendering
    Engineer Infrastructure` → `infrastructure tendering engineer`. The criterion does not say
    which, so these are as much its gap as the rule's.
- **A bare `associate` (5).** With the grade taken out, no head is left, and the criterion gives such
  a title none; the rule keeps what is left: `Associate, Investments (Infrastructure Focus)` →
  `infrastructure investments`, `Capital Projects and Infrastructure Senior Associate`,
  `Associate - Forensic Technology, Investigations & Discovery`, and the two that neither answer won,
  `Postdoctoral Research Associate - Robotics and AI` and `Agentic Diagnostics &
  Insights-Associate-Bangalore`.
- **Abbreviations, typos and codes (6)**: `Eng` (`Sr. Lead, SW Full Stack Eng` → `full stack eng`),
  `Principle` for principal, `SNR` for senior, `Ld` for lead (which is also the management fault), a
  requisition code read as a word (`RR-0421351 Sr Analyst II Infrastructure Services` →
  `infrastructure rr analyst`), and `C4I` taken out as a code although it names the work.
- **Heads and joins (3)**: `installer` is not read as a head (`Lead Building Automation Systems
  (BAS) Installer` → `building automation systems`); a slash between two heads that is not read as
  one (`System SEM/Architect`), and a slash between two fronts (`CRM / Marketing Automation
  Specialist`).
- **Front words (3)**: `Tax - Digital Tax (Infrastructure) Off-Cycle Internship` → `off cycle
  internship`; `Public Sector, Customer Architect` → `customer architect`; `Specialist IT Product
  Support` → `it product specialist`.
- **Not in English (2)**: a bilingual title not read by its English part (`Mobilny Technik
  Serwisu / Mobile Service Engineer` → `mobilny technik serwisu mobile service engineer`), and
  `Analista de Web Analytics` → `web analytics analista`.

The labellers' 26 losses were mostly the criterion's finer points: an `&` inside a kept phrase
spelled `and`, a kind word that moves read as a team, and a moved kind placed after an existing
front.

## What it says

- **The breakdown is good on the common titles and weak below them.** Titles on 12 vacancies or more
  are at fault for 2.9% of their vacancies. Below that a fault is about one vacancy in four or five,
  and the band of 5 to 11, which #25's agents will sort, is the worst measured, at 26.1% of its
  vacancies and 28.3% of its distinct titles. Weighted by vacancies, titles on 5 or more are at
  fault for 4.9% (1.2–8.6%), and the subset for 8.2% (4.7–11.7%), against the 3.5% #92's check gave.
- **Most of the faults are one move**: reading what follows the head as the kind of work. Nine in 34
  moved a team or a product, three missed a kind, and three placed it where the criterion does not
  say.
- **Management levels hold**: one fault in 210, from an abbreviation.
