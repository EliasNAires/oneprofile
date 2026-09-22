# Iteration 8 of the engineering-role classification loop

Run on the development machine on 2026-09-22, against the raw corpus snapshot already loaded in
the development database — 179 098 vacancies, cleaned and classified — and triggered with one
`POST /classifications`. **The criterion changed between iteration 7 and this one**: it was
rewritten on 2026-09-22 under ADR-0010, at revision `29b1272`, and this is the first iteration
whose labels and rules answer to the rewritten document.

The filename carries 2026-09-28 rather than the day it ran, for the reason iteration 3 gave: the
seven earlier iterations already hold the seven days before it, and the next session finds its
input by taking the newest file.

This iteration labelled the 1000 rows iteration 7 drew, scored iteration 7's classifier against
those labels under the skill's three-rate definition, implemented ADR-0010 in the classifier, and
grew the rules from where the labels and the answers disagreed. The labels are
`src/test/resources/labels/engineering-role-2026-09-27.tsv`; they were written to disk before any
rule of the classifier was read.

**The labelling was done by a subagent**, as in iterations 5, 6 and 7: the agent was given the
criterion and the 1000 bare titles, denied the classifier's source, the iteration reports and the
earlier label files, and told to write its labels to disk before anything else. Blinding is
ordering, and the ordering was enforced by what the agent could reach. Scoring, the rule pricing
and the draw were done by scripts over files in a scratchpad, and the rules were priced on an
offline `javac` harness whose answers were checked against the application's own counts — the
1000 rows, the 7000 accumulated labels and the 179 098 corpus titles never entered the session's
context.

## The numbers

The three rates score **iteration 7's** classifier, because those are the predictions the labelled
sample carries. They are the skill's definition — a mistake is any row whose state differs from
the label, so an `IN` labelled `UNKNOWN` is as wrong as an `IN` labelled `OUT` — and they are
scored on the stratum sizes iteration 7 drew, 600 `OUT` / 300 `UNKNOWN` / 100 `IN`. Iterations 1–7
were scored under a narrower definition and are not comparable; the one comparable figure is
iteration 7 re-scored under this definition, which the skill records.

| Gated number | Iteration 7 | Iteration 8 | Gate |
| --- | ---: | ---: | ---: |
| `OUT` stratum error — labelled `IN` or `UNKNOWN` | 7.33% | **3.00%** (18 of 600) | ≤ 10% |
| `IN` stratum error — labelled `OUT` or `UNKNOWN` | 35.00% | **16.00%** (16 of 100) | ≤ 10% |
| `UNKNOWN` stratum error — labelled `IN` or `OUT` | 62.67% | **77.33%** (232 of 300) | ≤ 10% |
| `unruled` — of the whole corpus | 5.82% | **5.61%** (10 047) | ≤ 4% |

Reported alongside, gated by nothing:

- **`OUT`-stratum rows labelled `IN`** — the error nothing downstream recovers: **5** of 600.
- **Rows the labeller left `UNKNOWN` that the classifier decided**: **23** — 10 it called `IN`,
  13 it called `OUT`.
- **The labeller's own unknown pile**: 91 rows, split 79 `domain_ambiguity`, 11 `unruled`,
  1 `scope_ambiguity`. It is a much smaller pile than iteration 7's labeller left (112), and the
  criterion's new instruction to read meaning in any language is most of the difference.

**Which of these errors the revision already answers.** The skill asks for this split so that
iteration 9 does not re-solve what ADR-0010 has settled. Each of the 266 scored mistakes was
re-decided twice: once by the revision alone — the split lists and the seven-step procedure, with
iteration 7's vocabulary — and once by the rules this iteration then grew.

| Stratum | Mistakes | Answered by the revision | Answered by iteration 8's rules | Still wrong |
| --- | ---: | ---: | ---: | ---: |
| `OUT` | 18 | 4 | 0 | 14 |
| `IN` | 16 | 0 | 0 | 16 |
| `UNKNOWN` | 232 | 1 | 80 | 151 |

The revision answers the `OUT` stratum and almost nothing else: its whole effect is to stop a
market word deciding a title that also names software, and that is where those four rows lived.
The two failing gates are the rules' problem, not the criterion's — and the `IN` stratum is
nobody's yet, since neither the revision nor 80 new words moved a single one of its 16 mistakes.

The corpus after this iteration's change:

| | Iteration 6 | Iteration 7 | Iteration 8 |
| --- | ---: | ---: | ---: |
| `IN` | 17.07% | 17.12% | **18.46%** (33 054) |
| `OUT` | 56.82% | 58.50% | **59.06%** (105 770) |
| `UNKNOWN` | 26.11% | 24.38% | **22.49%** (40 274) |
| &nbsp;&nbsp;of which `unruled` — the gated share | 7.00% | 5.82% | **5.61%** (10 047) |
| &nbsp;&nbsp;of which `domain_ambiguity` | 17.54% | 17.00% | **15.31%** (27 415) |
| &nbsp;&nbsp;of which `scope_ambiguity` | 1.57% | 1.57% | **1.57%** (2 812) |

ADR-0010 predicted the unknown share would rise for the first time in the loop's life, because
every market marker stops deciding titles that also carry a software qualifier. It fell instead,
because the rules grown on top of the revision took more out of the unknown pile than the revision
put into it. The `IN` share rose by 1.34 points, which is the revision showing through.

**What the `UNKNOWN` stratum turned out to be.** Of the 300 rows iteration 7 called `UNKNOWN`, the
labeller called 27 `IN`, 205 `OUT` and 68 `UNKNOWN`. One unknown title in eleven is an engineering
role — richer than iteration 7's one in twenty — and two thirds of the stratum are titles that are
plainly not engineering roles and that no rule reached. They paid for most of this iteration's
change, and they are why the `UNKNOWN` stratum error rose: the pile is not the corpus's ambiguity
yet, it is still our backlog.

## The exit gate

**Not met.** Two of the three rates fail and `unruled` fails: `IN` stratum error is 16.00% against
a 10% gate and `UNKNOWN` stratum error is 77.33% against the same gate, and `unruled` is 5.61%
against 4%. The `OUT` stratum error passes with room, as it has for five iterations.

The `UNKNOWN` gate is the one that decides this loop. It asks that the pile the classifier cannot
decide be a pile a labeller could not decide either, and three quarters of it is currently rows a
labeller decides on sight. Four iterations remain before the twelve-session cap — 9, 10, 11 and
12 — and closing a 67-point gap in four is not a plausible reading of the last three iterations'
rate of harvest, which has been steady rather than accelerating. The cap, not the gate, is the
likely end of this loop, and the skill says what to do then: write the report, record the reason
split, close #10, and hand the remaining unknown pile to #11.

## Cost

A full pass over the 179 098 cleaned titles takes **0.63s** against the ten-second limit, measured
warm on the offline harness, whose per-state counts match the application's to the row.
Whole-word, case-insensitive matching over the cleaned title; no description bodies, no network.

## What changed, and why

Two changes, and they are worth keeping apart: **the criterion's revision**, which this iteration
only implemented, and **the rules this iteration grew** on top of it.

### The revision, implemented

`TitleClassification` held one undivided marker list of 233 words under a rule that let every one
of them beat a software qualifier. ADR-0010 split that rule in two, so the list is now two lists
and the procedure has seven steps rather than six: a **discipline marker** decides out under any
head, domain-bound or domain-free, and beats a qualifier; a **market marker** decides out only
under a domain-bound head with no qualifier, and settles nothing under a domain-free head.

The 233 markers were sorted by the measurement ADR-0010 asked for rather than by hand: 188 of them
share a corpus title with a software qualifier somewhere, which is the only place the class has any
consequence, and those were the ones classed. Of the 233, **106 went to the discipline list** —
the credential test, "would a person need that training to be hired?" — and **127 to the market
list**, which is where everything unclassed belongs, because market is the class that cannot cause
a miss. With this iteration's own additions the two lists end at 138 and 163.

Three of those 106 were moved out of the market list only after the split was priced, and four
words were written fresh, each because it names a credential rather than a customer: `pfas`,
`water` and `physical`, and the new `gnc`, `guidance navigation`, `high pressure` and
`industrial automation`. Without them
`pfas technical manager`, `water systems specialist`, `physical security engineer` and
`guidance navigation and control engineer space systems` all decided `IN`.

**What the revision cost and bought, measured on the 1000 rows alone**: it fixed 5 of iteration
7's 266 mistakes and made 4 new ones, and it halved the misses over the 7000 accumulated labels,
from 36 to 18. The four new mistakes are the trade ADR-0010 wrote down: `Manager Environmental
Health Safety Data Centers`, `Manager Veeam Cloud Service Provider Sales ANZ`, `Account Manager
Platform Innovation` and `Technical Product Marketing Manager` each name a market next to a
qualifier and are now decided `IN`.

### The rules this iteration grew

Everything below was priced against the 7000 accumulated labels before it was kept. Together they
fix **80 further mistakes on the sample and break none**, and over the 7000 rows the whole
iteration moves 244 of them — 165 into the state its label gives it, 19 into or between states the
label left undecided, and 60 away from it, of which **56 are rows in the six superseded fixtures**
and 4 are the revision's own cost named above.

- **Twenty-four discipline markers the criterion names and no list held**: `fpga`, `asic`, `rtl`,
  `dft`, `serdes`, `signal integrity`, `pcb`, `electrical`, `airworthiness`, `cfd`, `cathode`,
  `dfm`, `energetics`, `human factors`, `formulation`, `cqv`, `aba`, `bioinformatics`,
  `biomarkers`, `ultrasound`, `neurology`, `radiology`, `bacteriology`, `autism`. The criterion's
  own worked example for the hardware-adjacent code roles is `FPGA Engineer`, and the word `fpga`
  was not on any list until now.
- **Thirty-six market markers** from the `domain_ambiguity` pile this sample handed back:
  `advertising`, `creative`, `visuals`, `giving`, `collections`, `people`, `hrbp`, `fp&a`,
  `federal affairs`, `policy`, `media relations`, `royalties`, `packaging`, `footwear`,
  `supplier`, `deal desk`, `enablement`, `alliances`, `credit risk`, `localization`, `catalog`,
  `pricing`, `fulfillment`, `warranty`, `residential`, `site acquisition`, `branch`, `poker`,
  `organizing`, `commerce`, `treatment`, `imagery`, `inspection`, `survey`, `fundraising`,
  `philanthropy`.
- **Four markers that repay a masked head.** `counsel` and `dentist` are never-engineering heads
  that `associate` and `director` are named before and therefore hide — the cost iterations 5 and 6
  pinned and iteration 7 half repaid with `chef` and `veterinarian`. As discipline markers they
  decide `Associate General Counsel Transactions` and `Associate Dentist Full Time` out without
  giving the mask back its power. `nanny` and `scribe` are there for the same reason.
- **Eleven never-engineering heads** the `unruled` pile named: `endodontist`, `internist`,
  `dietician`, `nanny`, `scribe`, `correspondent`, `assessor`, `keyholder`, `runner`, `shuttler`,
  `orthodontist`.
- **Twenty-eight software qualifiers** from the rows the labeller called `IN` and the rules left
  undecided: `rust`, `django`, `rpa`, `uipath`, `snowflake`, `power bi`, `dmz`,
  `vulnerability management`, `isso`, `agile`, `helpdesk`, `help desk`, `interoperability`, `ddi`,
  `golang`, `kotlin`, `scala`, `ruby`, `php`, `rails`, `angular`, `terraform`, `ansible`,
  `docker`, `hadoop`, `kafka`, `tableau`, `databricks`.
- **Three Q1 rulings** for the post that hires nobody, in the shapes this sample wrote it:
  `register your interest`, `general interest`, `don t see`.

### Tried and dropped, with what they cost

- **`engineering` as a software qualifier** — re-priced because the revision changed its trade, and
  still dropped: over the 7000 labels it moves 38 rows, 11 into their label and 19 out of it. It
  does remove two misses, which is the first time it has been worth saying out loud, and iteration
  9 may find it pays once more of the discipline list is in place.
- **`soc` as a software qualifier** — a security operations centre and a system on a chip are the
  same three letters and the corpus writes both. It decided `Engineer SoC Design Verification` in.
- **`content` as a market marker** — costs `Content Engineer Developer Relations`, a real miss on
  this sample's own evidence.
- **`trading` as a market marker** — costs `Product Manager DEX Institution Trading`, which is
  crypto-exchange software.
- **`engagement` as a market marker** — 3 rows into `OUT` that their labellers left undecided, and
  nothing gained.

## What this iteration's rules get wrong

Re-scored on the same 1000 rows after the change — informative, not a measurement, since these are
the rows the change was grown from. 185 of the 1000 disagree, against 266 before: one miss
(`OUT` decided where the label says `IN`), 10 false accepts, and 131 rows still left `UNKNOWN` that
the labeller called `OUT`.

The surviving miss is `Assistant Technology Solution Manager Shenzhen`, where the `assistant` head
is named before the technology the title is about — the mask again, one the marker trick cannot
repay because `assistant` is not a domain.

## What the next session should look at

**The `UNKNOWN` stratum error is the only gate that matters now**, and it is 77%. Two thirds of the
pile the classifier cannot decide is `domain_ambiguity` on a head the labeller reads straight
through. The harvest is still market words, one family at a time, and this sample's 131 surviving
rows are the next session's list.

**The unruled pile is now mostly foreign-language titles.** The criterion tells the labeller to
read meaning in any language, which this labeller did, and it is the rules that cannot follow. The
head list already holds `ingeniero`, `engenheiro`, `développeur` and `analista`; what it does not
hold is the rest of the vocabulary those titles need — `técnico`, `especialista`, `gerente`,
`responsable`, `entwickler` as heads, and no marker or qualifier in any language but English. A
head on its own only moves a row from `unruled` to `domain_ambiguity`, so the heads are worth
nothing until the markers land beside them, and that pairing was not priced this iteration for want
of session budget. It is the one remaining change with corpus-wide reach that nobody has measured.

**Q1 is a gate in the criterion and a list of rulings in the code.** The criterion's procedure
opens with step 0 — a post that hires nobody is out before the title is read as a head at all —
and `TitleClassification` has no step 0: it answers Q1 with one ruling per phrase, and this
iteration wrote three more. Twenty-six of the rulings are now that family. A gate answered one
phrase at a time is the shape the criterion wrote step 0 to avoid, and no rule the loop is allowed
to invent can read intent, so this may be a question for the criterion rather than a rule to grow.

**Watch the qualifier batch.** `agile`, `people` and `pricing` are the widest-reaching new words
and none of them is unambiguous. `pricing` in particular decides out under a bound head, and a
pricing engineer at a marketplace is a real title. `agile`, `helpdesk` and `interoperability` are
also not software *domains* in the sense the criterion's definition gives the word; they were kept
because the labels say the titles that name them are engineering roles, which is an argument the
next session may want to make properly or undo.

## Questions for the criterion, not for the rules

**The head that masks the head behind it.** `assistant`, `associate`, `director` and `head` are
named before the noun that would have decided the title, and three iterations have now paid for it
one marker at a time. The criterion's rule is that where a title names more than one head the first
is the head; the corpus keeps writing titles where the first head is the weaker one. This is the
oldest open question in the loop and the only one whose cost is still growing.

**Q2 against Q4 on hardware-adjacent code roles** — answered by ADR-0010 where the title names the
discipline, and still open where it does not. This labeller called `Bioinformatics Research
Engineer` out with low confidence and flagged it: the word is both the credential and the software
domain.

**Is a support role an engineering role?** The labeller called `Technical Support Engineer`,
`IT Support Engineer` and `Temporary Helpdesk Engineer` in on Q4 — a software background alone
makes you a credible candidate — while noting none of them writes code. Q4's own worked example is
Scrum Master, so the labels follow the document; but this family is large, and if the answer is
meant to be `scope_ambiguity` the criterion should say so.

**`scope_ambiguity` is nearly unusable by a labeller.** It is defined on a *ruled* phrase, which is
a fact about the classifier, and a labeller who has not read the rules cannot tell a ruled phrase
from an unruled one. This labeller used it once in 1000 rows and put everything else that genuinely
splits — `Delivery Manager`, `Infrastructure Engineer`, `Search Specialist` — into
`domain_ambiguity`, which inflates the reason the criterion says belongs to the corpus.

**A truncated or meaningless title.** `Adv Crema`, `MTS Exa`, `Progression Operator`: Q1 covers the
marketing surface that hires nobody, but not the string that names nothing at all. They land in
`unruled`, which says a rule could fix them, and no rule can.

## Held out of the draw

The 7000 ids in `src/test/resources/labels/` and the 100 distinct titles in
`src/test/resources/calibration/`, which is held out by title rather than by id. 168 685 of the
179 098 rows were eligible.

## The sample for iteration 9

1000 rows drawn at random from the predictions above — 100 from `IN`, 500 from `OUT`, 400 from
`UNKNOWN`, the split the skill now asks for — and **shuffled together**, so that the strata cannot
be told apart by position. Each row is a vacancy id and its cleaned title, tab separated.

Label these from the criterion alone, before opening any rule, and write the labels to
`src/test/resources/labels/` before reading the section below them.

```
141901	Waiter Waitress White City House West London
8021	Seasonal Sales Associate Part Time One Colorado
149987	Staff Market Intelligence Researcher
140912	Don t see the role you re looking for currently available Apply here
62416	Business Solutions Director Payments
41524	Production Risk Engineer MPL Architects Engineers
124557	Poland Residents Survey Participants Rybnik Poland
176418	Staff Software Engineer
177025	Head of Commercial Wealth Segment
175874	Manager Implementational Planning UK
175780	Implementation Activation Director
65129	Lead Physical Design Engineer
6038	Operations Safety Lead
158363	Financial Planning Analysis GTM Lead
3964	Task Order Project Manager TOPM
75887	Insurance Producer San Antonio North TX
159715	Influencer Marketing Manager UK gn
10817	Metrology Engineer CMM Programmer Maritime Division
63334	Associate Account Management
8022	Seasonal Sales Associate Part Time One Loudoun
36420	Account Executive Ejecutivo a de Cuentas Bogotá Hybrid
160765	Sector Analyst India Equity Research Consumer
59711	Data Analyst Tableau
156061	Nebraska Contract Therapist
18072	Cryo Nerve Block Clinical Specialist Dallas
29671	Hydrogeologist
164326	Cybersecurity Technical Mentor Independent Contractor US Canada Europe MENA India APAC Timezones
170474	Hospice Clinical Branch Director
24732	Licensed Psychological Associate Independent Practitioner Remote
164602	United Capital Financial Controller
147575	Vaga Temporária Auxiliar de Atendimento Logístico CNH A Moto São Paulo SP
130883	Full Stack Java Engineer
1456	Intervention Specialist
48875	Software Engineer Ads
164319	Wireless Switching Test Engineer
15292	Traveling Superintendent Construction
119311	Field Service Manager
40494	Manager Interconnection
76725	Valuations Analyst
52422	Mobile Recovery Case Manager Respite
1790	Government Security Personnel Analyst
4849	Copywriter
31482	Líder Técnico de Operações Híbrido Macaé
152492	Personal Trainer
158537	Chief CRNA St Louis MO
66309	Application Security Engineer
142159	Sonder Responder Western Australia
50903	Key Account Manager Great Lakes
87240	Software Engineer Embedded
177147	Account Executive Defense
155308	Fill Finish Process Engineer Future Opportunities
45687	Developer Advocate Service Management EMEA
67868	Backend Engineer Go Tenant Scale Gitaly
45208	Developer Advocate Open Source Omnigent
83606	Part Time Verkoopmedewerker JD Zoetermeer
114728	Platform Engineer II
38274	Group Product Manager Compliance Agent Experience
152862	Personal Trainer
170598	Physical Therapist PT Home Health PRN
166154	Behavioral Health Inpatient Nurse Nights
62979	Manager Life Sciences Partnerships
47655	Entry Door Technician
140414	AI Business Strategist Contract
29787	Reconciliation Analyst
161808	Server
22454	Medical Social Worker Home Health
72497	Operations Data Analyst Office Based
54012	Assistant Fitness Manager
30741	Especialista de ALM Liquidez
159383	Physical Occupational Therapist Consultant
146393	Programa de prácticas Sprinter Descrubre tu talento
155133	Personal Trainer Tomball TX
149703	Analytics Engineer Run Grow
59606	Back Office Business Analyst Calypso
111175	ABA Paraprofessional RBT Baldwin NY
161695	Bus Person
67824	Intermediate Security Engineer Security Incident Response Team SIRT
11576	PLM Developer
67297	Manager Global Benefits
140770	Mobile Threat Analyst
117027	Product Manager TwoTwenty
77505	Partner Account Manager Knoxville Chatanooga
86690	Test Automation Engineer
149084	Store Supervisor Part Time
99451	Software Quality Engineer
63434	Marketing Science Analyst
147856	Vaga Temporária Auxiliar de Atendimento Logístico CNH B Carro São Paulo SP Noturno
65125	Engineering Manager Compilers
134098	CNC Machinist Programmer
72387	Opportunistic Applications
160708	Associé e Service à la clientèle Francophone
4437	LaTeX Specialist Freelance AI Trainer Project
5657	Product Manager Builder Experiences
142649	Social Content Executive
156119	Account Manager Motor Finance
128225	Service Delivery Lead Our Future Health Mid Wales
64826	Startup Founder China
4673	Social Media Annotation Freelance AI Trainer Project
76716	Valuations Analyst
74833	MEP Energy Marshall
170378	Technical Writer Co op January to August 2027
176103	Associate Media Buying Pakistan
21780	Entry Level Personal Care Aide PCA
140310	Account Executive Air Force
154004	Personal Trainer
109	Junk Service Lead
165734	Software Engineer AI DevEx
157655	Director Accounting Operations
60338	Art Director Trading Card Game
17777	Attendant Adult Care Partner Fort Worth TX
152250	Personal Trainer
178047	in Residence 2026 2027
131216	Campaign Manager Scotland
113194	Product Security Engineer I
22379	LPN Nurse Residency Program
171100	Technicien.ne en informatique Niveaux 1 et 2
141198	Manager Clinical Review and Quality Assurance
26225	Manager Field Sales UK
20389	Commercial Account Executive
148564	Product Manager Strategic Apps
170207	Lifeguard
99535	Engineering Manager Java
67231	Regional Controller
76748	Revenue Strategy Analytics Director
153192	Personal Trainer
146777	Staff Quality Engineer
4380	Kyrgyz Language Specialist Freelance AI Trainer Project
141886	Waiter Waitress 180 House Central London
91146	Social Worker Child Protection
77099	Graduate Program Marketing at HRtechX
140704	Aesthetic Physician Assistant
108894	Staff Engineer Liquidity Platform Trading Service
77411	Entrepreneur in Residence
145529	CLM GTM Director
173694	Business Development Manager Defense
68119	AI Success Manager East
136834	Per Diem Clinical Research Nurse Home Visits
12398	Technical Writer Warfighter Systems
75884	Insurance Producer Sacramento CA
48482	Technical Sales Engineer
13216	Warehouse and Logistics Manager North America
22279	Live In Caregiver
64252	Director of Agency Management
74223	Research Engineer
57582	Heavy Equipment Shop Technician Mechanic
169495	Veterinarian
139242	South Hub Temp to Full Time Employee Application
45916	Developer Advocate Modern App Development
152499	Personal Trainer
140513	Robot Operator India
141310	Independent Sales Representative
4285	Hindi Language Specialist Roman Script Freelance AI Trainer Project
65937	Fintech Intern Summer 2027
56590	Stretch Manager
157181	Multiskilled Journalist
140854	Power Systems Modeling Engineer Real Time Simulation
110107	Family Medicine Physician Sign On Bonus Available
26347	Strategic Account Manager Bilingual Spanish
2274	Vice President
38237	Analytics Engineer Intern
38773	Account Operations
148938	Field Sales Representative Boston MA Metro
98906	Project Manager Michels Trenchless Inc
65927	Consultant Research and Analytics
7153	Retail Sales Associate
49893	Engineering Manager Discovery Experience
130921	Java Engineer Distributed Systems Serverless Elasticsearch
167566	6244 CIP Clean in Place Engineer
156418	Medical Assistant Crestview Hills KY
158094	Sales Representative
177125	Data Scientist Gen AI
87219	Product Operations Specialist
157474	Software Engineer Python
16492	Engineer Field Process Customer Support
51823	Surgical Coordinator
130017	Software Engineer News Insights
134735	VP of Sales
83688	Comunicação Supervisor a de Contas Farmacêutica e Jogos
156108	Real Time Analyst RTA
152123	Personal Trainer
148386	Full Stack Engineer Billing
50304	Manager Pricing Yield
45414	Staff Product Operations Manager
8835	Modeling Simulation Engineer Automotive
148355	Enterprise Services Manager Professional Services Sales SEA GCN
108221	Development Specialist Doernbecher Major Gifts
141668	Head Bartender Cecconi s Pizza Bar Central London
152185	Personal Trainer
166045	Provider Partnerships Associate Territory Sales
89844	Account Executive Eastern Europe Independent Hotel Sales
6731	Board Certified Behavior Analyst Hybrid Remote
41728	쿠팡 CS VOC 운영 담당자
166512	Salesforce Marketing Cloud SFMC Developer
72444	Customer Support Specialist
152328	Personal Trainer
162743	Class A CDL Driver
75471	Warehouse Nights Temporary
116308	Start Up Lead
25213	HOUSTON Land Development Internship
173047	Associate Veterinarian Fairgrounds Animal Hospital Reno NV
111365	ABA Paraprofessional RBT Providence NJ
4501	Mathematics Specialist Fluent in Spanish Latin America Freelance AI Trainer Project
54002	Assistant Fitness Manager
177902	Speech Language Pathologist SLP
112863	CONTRACTOR Solar Electrician Technician
152472	Personal Trainer
34465	Registered Behavior Technician RBT
638	Account Executive Corporate
1986	SAP Data Services Developer
140159	Manufacturing Technical Services Representative
110491	Advanced Practice Provider SunState Medical Specialists
172858	Part Time After School Caregiver Family Transportation Specialist Driving Required Oakland Piedmont CA
38990	Neuroscience Therapeutic Sales Specialist Nashville TN
67503	Localization Specialist
109771	Business Manager Station 250
4913	Auxiliar de Vendas e Atendimento Ouro Fino MG Exclusivo PCDs
130431	Forward Deployed Engineer Agentic AI
25804	Program Manager
156157	Executive Director Clinical Development
20553	2027 Graduate Engineer Mechanical Building Services
107755	Account Manager Hospitality
64896	HR Manager Director
15486	Analista de Segurança de Aplicações II
6602	Data Center Cabling Foreman
158549	CRNA FLOAT Dallas TX
91084	Social Worker Child Protection
7028	Legal Counsel Free Speech
162865	Account Manager International
41980	Data Analyst Marketing
148465	Manager Sales Development
146133	Customer Success BR MX
142338	Director Litigation EMEA
45534	Staff Software Engineer Search Quality
107946	Mental Health Therapist 1099 Tennessee Nashville Area
127432	Consultant in Adult Psychiatry Shipley
48166	Customer Service Team Leader 57th Lex
152487	Personal Trainer
36727	First Charge Mortgage Operations Manager
133309	PeopleX Insights Analytics Intern Summer 2027
146883	Director of IT
55752	Member Experience Manager
9030	Adjunct Entrepreneurship AI and Future of Work Expert
25478	Environmental Health and Safety Specialist
99753	Home Health Certified Home Health Aide CHHA CNA
158714	Buyer 12 Month FTC Cult Beauty
76586	SEO Strategist Consultant
49116	Architect Justice+Civic
168577	Emergency Veterinarian Costa Mesa CA
99945	Software Engineer AI ML
34516	Registered Behavior Technician RBT
144740	Environmental Health Safety Engineer
5872	Forward Deployed Engineer LatAm Market
141192	GTM Finance Director
141000	Digital Growth Director
5553	Product Designer
147786	Auxiliar Técnico de Logística Centro de Distribuição Jaboatão dos Guararapes PE Noturno
47889	Dialership Program
114168	Logistics Specialist Retail Operations
41067	Staff Specialist Field Engineer Robotics
92514	Material Coordinator
139996	Data Infrastructure Engineer
143149	CNC Programmer Starship Components Level 4 5
142007	Clinical Therapist
73572	In Home Dialysis Support Tehachapi CA
80386	Product Designer
25923	Staff Electro Optical Payload Engineer
66439	Financial Analyst
114295	Software Engineer
55856	Member Experience Manager
170362	Product Designer
41267	Service Manager Cortland Dublin
114178	Algorithm Scientist
142036	Psychiatric Clinician
43726	Environmental Technical Studies Manager FT Hybrid
4784	Voice Actor Freelance AI Trainer Project
116151	Manager Business Partner BP
146330	IT Support Engineer II
89947	Signal Integrity Engineer
47021	Adobe Value Advisor
51569	Medical Scribe Eyecare Training Provided Full Time Year Round
157151	Meteorologist
14762	Certification Operations Manager
36551	Territory Sales Lead Líder de Vendas Locais Recife PE Hybrid Remote
139938	Wireless Communication Systems Engineer
48066	EMEA Marketing Specialist
140859	Director Influencer Marketing
91023	Social Worker Adult Safeguarding
159433	Physical Occupational Therapist Consultant
3003	Almacenista
157660	Director Supply Chain Management
116397	Lakeshore Oakland Barista
24826	Manager Channel Partnerships
6190	Medical Assistant Pomona Valley
140510	Robotics Software Engineer
109020	Fabricator Metal
12330	Systems Integration Engineer Air Vehicles
77669	Commercialization
162990	Material Resource Planner
150805	Personal Trainer
158219	Therapist I or II
152236	Personal Trainer
32148	Systems Administrator 2 Linux CI CD Ansible Terraform DevOps
142358	Lead Analytics Engineer
51027	Customer Experience Specialist
131551	Supervisor Machine Shop Quick Turn
47415	Director of Immersive Education Programs
167347	Research Associate I
157854	Corrective Action Program and Performance Improvement Specialist
44434	Director of Professional Services
28082	Product Manager Commerce
149694	Regional Manager Field Sales
142713	Rigger
83001	Vendeur CDD temps partiel
155894	Early Careers Program Lead
130502	Backend Engineer Compliance Engineering
17438	Shop Technician
74430	Director Project Manager
147726	Agente Stone Consultor a Comercial Externo São Francisco do Sul SC
177375	Analista Sênior de Soluções de Portfólio e ETFs
71694	Pricing Specialist
2529	Behavior Technician
146690	Shareholder Advisory Director
4709	STEM Specialist Fluent in Hindi Roman Script Freelance AI Trainer Project
157593	Vice President Strategy Communications
66478	Pipe Welding Lead
156271	Accountant
38447	Live Phone Support Contractor Remote LATAM
68895	Business Operations Manager
49758	Analytics Engineer Brazil Vaga para mulheres
159395	Physical Occupational Therapist Consultant
108868	Product Manager AI Risk Strategy Buy Sell Crypto
16826	Lead Supply Planning
99541	Product Manager Platform Management
155283	Strategic Partner Marketing Manager
111161	ABA Paraprofessional Position BT Port Chester NY
126133	Band 5 or 6 Non Invasive Cardiac Physiologist Bury
37127	Manager HRBP NORAM
89781	Center Based Registered Behavior Technician RBT
73038	Logistics Analyst Outbound
24815	Incentive Compensation Manager
176935	Frontend Developer
54578	Certified Personal Trainer
65585	Plant Controller
81608	IT Support Analyst II
9476	Director of Third Party Logistics 3PL
171570	Contract Security Supervisor Georgetown TN
4601	Product Matching Specialist Freelance AI Trainer Project
166957	Scientist II Analytical Development Contract
107076	House Manager
140885	Major Account Executive
55093	Fitness Manager
73902	Female Caregiver Needed Part Time Support for Client Wailuku HI
31150	Analista de Processos Rio de Janeiro Híbrido
178039	Middle School Math Curriculum Writer Consultant
126104	Band 5 Locum Cardiac Physiologist Medway
1818	Inventory Configuration Specialist
111264	ABA Paraprofessional RBT Browns Mills NJ
57820	Service Manager
81005	Event Services Specialist
110360	Virtual Family Nurse Practitioner CA Licensed
12210	Staff Software Engineer Ghost
162506	Logistics Specialist
5018	Não encontrou a sua vaga em Eficiência de Vendas Clique aqui e avisaremos quando for divulgada
46340	Graduate Quant
100338	Machine Learning Engineer 머신러닝 엔지니어
25404	General Manager Wake Forest
56653	Wellness Recovery Specialist
61052	Manager of Customer Supply Chain
155659	Business Intelligence Analyst
49707	DashMart Team Member Calgary
26261	Partner Manager Reseller
149827	Manager Real Estate Property Management
168554	Emergency Veterinarian Ardmore PA
127352	Consultant in Adult Psychiatry Essex
140613	RN LPN Clinical Liaison
48857	Engineering Manager Safety
169083	New ER Doctor NERD Program Starts Jan 2027 Practicing Veterinarians Peabody MA
54283	Assistant General Manager
46382	LMHCA or LCSWA Spanish English Remote Supervision provided Work with students
116935	Lead Client Partner Brand Sales tvScientific
4035	Basque Trilingual Language Specialist Freelance AI Trainer Project
48615	Cloud Support Engineer Database
41032	Accountant JV VIE
11991	Signal Processing Engineer Space
141347	Independent Sales Representative
75367	Corporate Development
8800	Integration Project Chief Electrical Engineer
147707	Agente Stone Consultor a Comercial Externo Patos de Minas MG
149899	Head of Risk and Resilience
48437	General Manager
143078	Build Supervisor Starship Mechanisms
9475	Director Global Digital Content Commerce Strategy
166378	Software Engineer
51108	Client Portfolio Manager
142242	Front Desk Cosmetic Surgery Center Training Provided
144544	Sourcing Manager Capital Equipment Construction Wastewater Treatment Starlink
63505	Lokführer
140570	Physician
47437	Sales Compensation Analyst
99439	AI Product Manager
117180	D&I Program Manager Professional Services
95939	Fleet Insurance Producer
144812	Instrumentation Controls Engineer Solar Cell Factory Starlink
63509	Manager Bordtechnologie Konnektivität
159719	IT Business Analyst gn Data Finance SAP
168677	Emergency Veterinarian Part Time Omaha NE
136020	Data Governance Responsible AI Lead
144060	Onboarding Coordinator
140275	Forward Deployment Engineer Greater NYC
48063	Corporate Account Manager
4772	Video Production Specialist Freelance AI Trainer Project
52356	Behavioral Specialist II ACCS
96961	Podiatrist
173244	Medical Director Sanford Oaks Animal Clinic
70191	Creative Director Copy Health
25794	Medical Scientific Annotation Specialist
145997	Electronic Warfare EW Operations SME
55916	Member Experience Manager
48210	Entry Level Kitchen Position DIG on 4th
4579	Plan Documents Specialist Freelance Project
147613	Vendedor a Externo 6 horas Ipameri GO
108846	Manager Director Growth Operation
54887	Fitness Counselor
6237	Civil Engineer Land Development PE
158092	Sales Representative
144657	BAW Device Engineer Starlink Akoustis
101523	Manager Media Strategy B2B
157864	Director of Tax
156832	Teamleiter Logistik
66899	Sports Data Collector Football Montpellier France
29547	Trainee Operational Risk
41814	HR Management Specialist
30183	Analista de Negócios Comercial Atacado
11052	Program Financial Analyst
26187	Director Revenue Strategy Planning
61722	Revenue Ops Associate
114831	Virtual Speech Language Pathologist SLP
140709	Supervising Physician
172609	Caregiver
41653	쿠팡 이츠 프랜차이즈 파트너십 및 B2B 채널 관리 매니저 Partner Development Conversion
67408	Data Engineering Specialist DataOps Data Platform
112931	Sales Manager
1956	SAP AI Developer
51587	Office Manager
143429	Factory Engineer Starlink Production
46334	Experienced Quant
43236	LVN LPT
49155	Hospitality Architect
7152	Retail Sales Associate
173176	Future Medical Director Veterinarian Monument View Veterinary Hospital Grand Junction Colorado
160667	Associate Client Services Graduate
73048	Manager Strategic Procurement Packaging North America
176033	Pessoa Gerente de Estratégia de Influência Inglês avançado
30180	Analista de Continuidade de Negócios Pleno Processos Controles
24169	Tax Lead Crypto
171467	Manufacturing Engineer Machining Automation
127458	Consultant in CAMHS Psychiatry Greater London
124426	Pathologists Freelance Remote Oklahoma City US
29027	Software Engineer Backend
140177	Quality Analyst II Quality Batch Review Disposition
19081	Product Manager Compliance and Risk Products
50044	Associate Merchant Strategy Operations
16107	Stage Chef fe de projet relations publiques et internationales
144680	Construction Engineer
75082	Wellness Guide
62320	2027 July Special Executions Group Full Time Analyst New York
154493	Personal Trainer
76301	Real Estate Acquisition Consultant
105814	Lead PM Product Innovation
130523	Data Scientist Ads Integrity
73643	In Home HHD Dialysis Care Partner 1 1 Client Reseda CA 91335
74895	Office Engineer
156445	GPU Structured Cabling Project Manager
67992	Product Manager Expression of Interest Form
130898	Consulting Architect Security EMEA Public Sector eligible
84377	Process Assurance Engineer Hardware Electrical
137706	Apprentice Quality Inspector
47013	Account Lead Retail Search
13535	Landscape Foreman Stormwater
9372	Director of Business Development
147921	Store Manager
108093	Influence Manager
4481	Mathematics Specialist AI Trainer Project Freelance
156530	Gameplay Designer Combat AI
9415	Project Manager Multifamily Construction
164556	Associate Design Raleigh
140036	AI Engineer Graduate
130643	Leasing Specialist Full Time
174075	Talent Acquisition Specialist
167157	C# Developer Explorers Identity Resilience
145114	Technical Recruiter Starbase
137196	Manager Assembly Receiving and Warehousing
93026	Construction Manager
168431	Emergency Credentialed Veterinary Technician Relief Boynton Beach FL
26593	Bluehole Studio 3D Character Artists Lead Project V
157420	Account Executive ANZ
77028	Event Manager volunteer
97296	Manager Paid Social
144617	Aerodynamics Engineer Starship
140442	Engineer Signal Integrity and Power Integrity
153457	Personal Trainer
143091	Business Operations Analyst Supply Chain Starlink Aviation
56328	Service Associate
177473	Investor Offshore Pleno
156047	Hawaii Contract Therapist
141650	Front Office Supervisor Soho House Los Cabos
126397	Band 6 CT Radiographer South Wales
175927	Manager Manager Associate Director Social
4586	Plant Viruses Specialist Freelance AI Trainer Project
167062	DevSecOps
114716	VP Investment Infrastructure
175824	SEA Specialist
62204	Connector Interconnect Design Engineer
140367	Experte Group Accounting Reporting 80 100 Grossraum Zürich Hybrid
11866	Supply Chain Program Manager
25236	TAMPA Land Site Development Internship
111173	ABA Paraprofessional RBT Baldwin NY
36359	Director Financial Planning Analysis
70401	MEP Project Manager
174408	Sales Manager Wolt Benefits
16214	Lead Product Manager Enterprise Services Management
110801	Contractor Material Planning Specialist Merchandiser
8149	Store Manager Macau
48689	NoHo Restaurant Team
64187	Lead Client Advisor Experience Manager
148463	Manager Product Support Operations Top Startups
70238	Operator CDL Class A
75652	Insurance Agent Memphis TN
75119	Analytics Engineer II
64331	Labourer
15242	Thru Put Project Manager
48911	Research Associate Associate Scientist Molecular Analytics Analytical Product Sciences
168189	Associate Saver Operations
111257	ABA Paraprofessional RBT Browns Mills NJ
145203	Structural Engineer Starlink
29015	Partner Marketing Manager
137749	AI Learning Project Manager Contract
85909	Quantitative Developer Kalshi Trading LLC
74788	Field Office Processor
136978	Lead Product Manager
146531	Audio Visual Specialist
141623	Executive Chef Soho Farmhouse Japanese Restaurant
169232	Veterinary Sonographer St Louis MO
53973	Assistant Fitness Manager
157767	Physical Design Intern CPU AI Hardware
49742	People Director
114730	Implementation Project Manager
140058	Technology Analyst
62850	Director Drug Development Project Management Pipeline Strategy
141514	Barback Soho House Amsterdam
155284	CMMS Administrator
149509	Commercial Terrain Nouvelle Aquitaine Bordeaux
48418	Sous Chef New York City
143028	Avionics Build Associate Starship
42103	Sortation Manager
144166	Network Engineer Starlink Ground Network
152289	Personal Trainer
156871	Account Executive Growth
5940	Kitchen Porter
16736	Engineer II Mechanical Engineering
31369	Engenheiro de Segurança do Trabalho Remoto Rio de Janeiro Macaé
165699	Software Engineer I
107350	Project Manager Narvik Norway
17870	Lab Technician
113041	Care Coordinator
8020	Seasonal Sales Associate Part Time Old Town Alexandria
156051	Iowa Contract Therapist
18754	General Manager
72312	Paralegel
63471	Eisenbahnbetriebsleiter
46736	Account Manager Provider
65970	Workplace Advisory Consulting Intern Summer 2027
145385	Thermal Mechanical Test Engineer Silicon Packaging Starlink
6434	Accountant
89789	Center Based Registered Behavior Technician RBT
65555	Warehouse Inventory Control Lead
45665	Commercial Sales Engineer Spanish Speaking
62818	Summer 2027 Trading Operations Engineer Intern
140838	Field Service Engineer Central Corpus Christi TX
106305	Operations Program Manager
38644	Leasing Specialist
156135	Motor Finance Account Manager Dealer South Yorkshire
99221	Software Engineer AI Computer Vision
176351	WPP Media Expression of Interest Growth Roles
9517	Recruitment Consultant
3481	Manager Ticketing Pacific Northwest Portland
156777	Schichtleiter in automatisierte Logistik
8495	Mid Market Account Executive Corporate
111207	ABA Paraprofessional RBT Brooklyn
140321	Chief of Staff
144948	Propulsion Analyst Chamber and Nozzle Raptor
160671	Associate Client Services Italian Speaker January Start
62592	Insights Consultant at FirstMind
168499	Emergency Credentialed Veterinary Technician Relief Torrance CA
164708	Remote Immigration Attorney PR
108751	Software Engineer Mobile CEX
168466	Emergency Credentialed Veterinary Technician Relief Las Vegas NV
13356	Events and Ministry Coordinator Onsite
169088	New ER Doctor NERD Program Starts January 2027 Practicing Veterinarians
121809	AI Trainer Fluent Punjabi Speaker Jalandhar India
7491	Sales Associate FTC Regent Street
90793	Qualified Social Worker Generic Adult s Team
172236	Controls Engineer
173752	Dream Job Commercial Department
110313	Primary Care Physician BLVD Place
152133	Personal Trainer
140106	Staff Embedded ML Engineer Edge AI
75600	Insurance Agent Farmington NM
152309	Personal Trainer
1652	Contract Manager
148837	Medical Assistant
56563	Stretch Manager
46678	Director Of Space Operations FedD190
48113	Technical Support Engineer
62927	Scientist Optical Technologies
2288	Calibration Lab Manager Aldinger
99742	Weekend Forklift Operator
175	Lead Producer
140402	Web Application Penetration Tester
11584	Procurement Manager
77627	Director Product Management Identity
143715	Laser Tracker Toolmaker Starship Launch Hardware
160208	Business Development Director
170350	Weekend Trader
66553	Director GMP Quality
158679	Urology Physician Lecanto FL
141222	Account Executive
63093	Finance Manager
19711	Professional Services Manager
177730	Mid Market Account Executive
74600	Future Career Opportunities Coming Soon Savannah Region
57713	Regional CDL Driver
53587	Environmental Project Manager Geologist or Engineer
77585	Data Study Participant Columbus Ohio
5501	Market Manager Cambodia
161593	Technical Support Engineer India
46905	Kernel Engineer Compute Accelerator
140238	Forward Deployment Engineer
40275	Customer Support Associate
48059	Associate Authentication Analyst Chinese Speaker
56177	Personal Training Manager
158663	Urology Physician Bronx NY
11660	Radar Engineer Air Defense
81503	Manager Global Network Operations
130673	2027 Launch Program Associate Product Manager AI
146982	Technical Support Engineer
19110	Aircraft Maintenance Instructor A&P Mechanic
147365	Agente Stone Consultor a Comercial Externo Valinhos SP
146163	Games Data Analyst Europe Asia
49723	Associate New Verticals Canada Strategy Operations
67429	Mid level Backoffice Analyst
124856	Finance Consultant Corporate Strategy Research Remote Advisory Iași
158627	Gastroenterology Physician South Houston TX
4873	Onsite Total Case Specialist Kia Care Customer Service Rep
141353	Independent Sales Representative
10405	EMI EMC Test Engineer
144253	Project Manager Tooling Starship
168507	Emergency Credentialed Veterinary Technician Relief Woodbury MN
170061	Opportunistic Operations Teams
65372	Research Associate I II
71892	US Team Lead Legal Application Managed Services
118561	Sales Manager SMB AEs
156447	Account Executive III
115888	Community Engagement Lead East
15197	Electrical Project Manager
130585	Staff Data Scientist Marketing
4892	Analista de Produtos Sênior Open Finance Campinas SP
39352	Budtender Part Time
146415	Visual In Store Sprinter Tarragona
112903	Staff Software Engineer
131468	Propulsion Engineer Engine Systems Test
141207	Seasonal Inbound Call Center Representative
144016	Network Engineer Starlink
44617	Cybersecurity Lead ISSM
155847	Planner Beauty
127386	Consultant in Adult Psychiatry Leyland
140065	User Experience Designer
75690	Insurance Agent Salem OR
115288	Implementation Manager French speaking
65162	Verification Engineer
77358	Chief of Staff New York City
52072	Director of District and School Partnerships Eastern PA Delaware and Southern NJ
49392	Performance projects analyst Intern
145723	Software Engineer Platform Boise ID USA
140050	Full Stack Developer
119276	Paid Advertising Strategist Ecommerce
92576	Project Engineer Structural
172867	Part Time Babysitter Westfield New Providence NJ
55764	Member Experience Manager
52536	Dental Assistant
118098	District Manager
177197	Solutions Engineer CNC Sheet
44982	Designated Support Engineer
86143	Staff Software Engineer
174422	Catalog Quality Specialist
9507	Entry Level Recruitment Consultant
104946	Head of Engineering
165101	Volunteer Team Member
112799	Full Stack Engineer TypeScript Prototyping
77666	Chief of Staff Office of the CEO
45901	Sales Engineer Enterprise
65145	Logical Design Engineer
141516	Barback Soho House Nashville
119258	Looker Developer Data
44835	Field Engineering Manager Specialist Solutions Architects Nordics
177317	Supplier Development Specialist
40486	Warehouse Team Member
118371	Shift Supervisor
143292	Electrical Engineer Facilities Starlink
1000	Intermediate Project Manager
8143	Store Manager Columbus Ave UWS
68317	HR Specialist
172994	Radiology Tech Full Time Lifesigns
140454	Technical Accounting Manager
15651	Inventory Specialist I
18702	Payroll Benefits Specialist
142049	Psychiatric Clinician
8811	Lead Electrical Engineer
158528	Anesthesiologist Elgin IL
155342	Associate Practice Area Director I Cx
77860	Lead EBS Federal Financials Analyst 1
144179	Software Engineer Continuous Integration Starship
73665	In Home Staff Assist Thousand Oaks CA 91359
98204	Staff Software Engineer Reliability
26602	Bluehole Studio Lead VFX Artist
21003	Manager Commodity Energy Trading US Power Markets
55773	Member Experience Manager
150053	Lead Engineer System Safety
61185	Early Years Educator Unqualified
100405	Digital Marketplace Specialist
152156	Personal Trainer
66356	Analyst Commercial Finance
31923	Consultant Transport Distribution Rail
169200	Veterinary Sonographer Brooklyn NY
169196	Veterinary Radiologist Part Time Shrewsbury MA
63850	Field Tech HOU
64514	Data Scientist Real World Data
15599	Product Manager Brazil
65447	Customer Support Associate
98192	Security Engineer Infrastructure Network Security
98358	Mobile Device Repair Associate
174909	Surgeons
15824	Director Payment Operations
152628	Personal Trainer
98503	Alternative Delivery Manager Small Modular Nuclear Reactor Michels Preconstruction Services Inc
157114	Digital Strategist
157286	Reporter MSJ
48255	Food Service Worker NoHo
15856	Consulting Manager Daten KI Strategie
52767	Franchise Business Consultant
146640	Director Biostatistics
75611	Insurance Agent Grand Island NE
11401	Harness Engineer Omen
153570	Personal Trainer
108737	Product Manager Affiliate KOL Onshore Markets
99014	Estimator Substations Michels Pacific Energy Inc
108117	Project Manager Ogilvy Health
146061	Product Manager Servicing
28083	Product Manager Developer Ecosystem
74467	Vice President Public Relations Health
67413	Especialista de Marketing Digital y Automatizacion
104641	Product Manager Internal Billing Tools
47221	Applied Research Scientist AI Research
64295	Forbes Fellowship
174417	Analytics Engineer Multiple Domains
160641	Account Manager
47382	Office Administrator
38847	Venture Analyst ComparaJa
141339	Independent Sales Representative
14601	Production Operations Lead Technician
138085	EV Hardware Engineer
106248	AI Presales Engineer
63763	Aerodynamics Intern Summer 2027
11335	Firmware Engineer Manufacturing Test
5888	Sales Partnerships Manager Iberia Contrato en Prácticas
8144	Store Manager Dedham MA
49103	Fire Protection Engineer Job 1437
29553	Operations Settlement Analyst
156926	Assistant Store Manager
3335	Project Manager Estimator
102030	Outside Sales Consultant
77381	Chief of Staff Vancouver
42350	Coupang I HRIS Workday L6 I
169281	Veterinary Technician Student Externship Falls Church VA
14643	Product Lead Opterra Investment Portfolio Accounting UBOR
48681	Jefferson Restaurant Team
64699	FP&A Analyst
34451	Registered Behavior Technician RBT
147407	Agente Stone Executivo a de Contas Externo Osasco SP
141933	Corporate Counsel
54295	Assistant General Manager
109335	Temporary Project Manager
77016	Event Associate
89545	SKS Builder Product Trainer
2383	Admissions Advisor Dallas
52469	Residential Counselor II Social Services
49384	Inside Sales
142868	NOC Engineer
8733	Supply Chain Manager
97932	Enterprise Account Manager National Accounts
116719	Portal Product Manager
99913	Data Scientist Credit
47135	Visual Design
9618	Spontaneous Application Data Engineer
142041	Psychiatric Clinician
33412	Associate Director Project Management
140563	Physician
41871	Brand Management
72969	Produktionsmitarbeiter
121488	AI Trainer Advanced French Fluency Paris Remote
168460	Emergency Credentialed Veterinary Technician Relief Henderson NV
112088	Product Manager Booking Experience
63741	Math Test Free Shot
12458	Winter 2027 Technical Program Management Co op
42765	Field Operations Advisor Army Programs
55892	Member Experience Manager
160390	Apprentice People Services Assistant
149098	Educational Content Coordinator Contract
72946	Production Associate
72819	FSQA Manager Quality Systems Compliance
143566	Hardware Development Engineer PCBA Manufacturing Starlink
95087	Join Our Talent Network
48455	Business Operations Data Analyst
144513	Software Engineer Starlink Network
162264	Managed Services Engineer On site
148712	Solutions Architect Commercial UK I
56757	Fiber Optical Lead Technician
75156	Content Creator
988	Global Social PMO Manager draftLine
174929	Physical Therapist
152233	Personal Trainer
157464	GTM Recruiter
159405	Physical Occupational Therapist Consultant
141798	Retail Sales Assistant Full Time Soho Home Kings Road
119682	Billing Operations Specialist
141401	Staff FDE CUA
147383	Agente Stone Executivo a de Contas Externo 6 horas Guarulhos SP
28006	Algorithm Engineer Reinforcement Learning
98464	MHI Talent Solutions Business Systems Analyst Accounting Finance
146248	Director Analytics Engineering
7030	Legal Counsel Life
9806	Strategic Operational Planner Military Operations Planning
108952	Tech Governance Security Compliance Governance Engineer
69904	Manager Electrical Engineering
5626	Director of Digital Organizing
1035	Research Associate In vivo Pharmacology
32	Field Service Technician
21762	Division Director I DD
141228	Account Executive
176081	Associate Activation Digital Planning
112987	Associate Operational Forecasting
169711	CSR Danville Family Vet
147670	Agente Stone Consultor a Comercial Externo Diadema SP
115125	Implementation Manager
152852	Personal Trainer
25995	Shift Lead Boston
127787	Locum Consultant Microbiology Birmingham
4395	Language Alignment Resource Partner Italian Freelance AI Trainer Project
56189	Personal Training Manager
172594	Property Manager
116944	Lead Industry Manager Scaled Greater China Region
170673	Registered Nurse RN Home Health PRN
108467	Manager xDR Strategy Operations
149100	Freelance Social Media Video Host TikTok Contract
145823	Software Engineer Platform Lima Peru
37784	Vehicle Delivery Specialist
2671	Supply Chain and Supplier Management Lead
77863	Oracle Cloud EPM Manager
126493	Band 6 Locum Highly Specialist Speech and Language Therapist Sidcup
143735	Lead Development Test Technician Starship Structural Test 1st Shift
9503	Commercial Contracts Consultant
92575	Project Engineer Structural
140478	SSD Firmware Development Engineer Intern
9305	Digital Quality Measures Lead Part Time
176713	Recruiting Coordinator
13273	Medical Sales Representative Brooklyn NY
74319	Engineering Manager Customer Studios
158640	Gastroenterology Physician Waterbury CT
55771	Member Experience Manager
55546	Kids Club Associate
142096	KYC Analyst Managed Services Late Shift
4338	Java Coding Specialist Freelance AI Trainer Project
3299	Strategic Growth Manager
110705	Customer Success Manager Scaled Accounts
40903	Data Center Development
83303	Vendeur CDI temps partiel
46887	Paint Sprayer and Prepper
9085	Account Executive Mid Market
52897	L2 L3 Support Engineer
45959	Sales Engineer Key Accounts
54885	Fitness Counselor
160557	Part Time Math Tutor Campbell
168417	Emergency Credentialed Veterinary Technician Peabody MA
23805	Regional Convention Wellness Specialist
141325	Independent Sales Representative
7032	Litigation Paralegal
3016	Facilities Project Engineer On site
18656	Salesforce Administrator
142204	Oracle DBA
59778	Product Marketing Manager
49294	Head of Lab Testing
28691	Strategic AI Sales
2584	Registered Behavior Technician
73753	Peritoneal Dialysis Support Needed In Casa Grande AZ 85122
154655	Personal Trainer Bridgewater MA
143146	CNC Programmer Level 4 5 2nd Shift
9007	Settlements Analyst
152263	Personal Trainer
6048	Design Engineer Electrical
51763	Patient Relations Lead full benefits no weekends growth opportunities
141725	Line Cook I The Willows Inn Palm Springs Seasonal
142391	Director Patents
74512	Engagement Coordinator at Claremont Hillel
55913	Member Experience Manager
47965	Medical Biller Coder OB GYN coder
112462	Regional Facilities Coordinator
7690	Sales Service Lead Galleria Edina
55866	Member Experience Manager
174640	Aviation Planning Project Manager
175888	Manager Media Planning
64600	2026 Internship
11182	Analytics Engineer
173038	Associate Veterinarian DVM AAHA Accredited Small Animal Practice Mukilteo Veterinary Hospital
105714	Interconnection Project Manager DG Internal NLE candidates only
114196	Fullstack Engineer
82981	Superviseur de rayon
154471	Personal Trainer
69897	Engineering Group Lead
142434	Technical Designer
140171	Pharmaziepraktikant für 2027
140755	Sales Associate
55926	Member Experience Manager New Gym Opening
164964	Graduate sales specialist at United Media
26569	Partnership Co Marketing Specialist
30904	Open Source Enterprise Sales Alliances
33003	MBA Graduate Program Samlino Group
44016	Strategy Planning Associate
4390	Language Alignment Resource Partner Freelance AI Trainer Project
142130	Responder Broken Hill
31160	Analista Técnico de Operações Home Office
148931	Brand Ambassador
107631	Lead Mechanic Public Markets
53616	Walkaround Program Specialist
43239	LVN LPT
154452	Personal Trainer
19095	Patient Growth Specialist
53586	Environmental Project Manager
5569	Operations Manager Radiology and Clinical Operations
11678	Research Scientist Battlespace Awareness
111082	General Consideration Engineering
127508	Consultant in Old Age Psychiatry Aberdeen
11618	Product Quality Engineer Eagle Eye
22651	Outpatient Physical Therapy Assistant PTA Living
162704	Associate Director Credit
75670	Insurance Agent Odessa TX
147124	Litigation Associate
142505	Digital Marketing Manager
141363	Independent Sales Representative
98230	AI Engineer I 6811
145374	Thermal Control and Life Support Hardware Engineer Crew Starship
141664	Gym Receptionist
147763	Agente Stone Executivo a de Contas Externo 6 horas Pouso Alegre MG
152432	Personal Trainer
76761	Technical Recruiter Engineering
155335	Associate Practice Area Director CF
40715	HUMINT Collection Manager
48841	Associate Director Corporate Communications
17904	Regional Payroll Specialist EMEA
45661	Commercial Sales Engineer AMER West
173243	Medical Director Paws Claws Animal Hospital
43722	Environmental Project Manager FT Hybrid
13213	VC Partnerships Lead
47866	Founder s Office Support
162256	General Applicants
75479	Art Director
146942	Renewals Analyst
175678	Digital Strategy Planning Execution Associate
127472	Consultant in Child and Adolescent Psychiatry Cardiff
85746	Solar Production Technician
20500	Business Development Manager App Advertisers
138773	Director UX Hardware
110627	Locum Tenens Primary Care Physician Assistant Northern Oregon
31180	BA Power Systems
9613	Sales Development Representative
149101	Freelance Social Media Video Host TikTok Contract
148247	Art Director
62456	SecOps Expert
29402	Plumbing Designer
29697	Cult Leader
20897	Operations Manager II
162448	Customer Success Analyst
6660	Sales Internship Program
166499	ReactJS Associate Architect
16702	Engineer I Field Service
118954	Women s Mental Health Specialist LCSW LPC TX License
159285	HR Business Partner
56593	Stretch Manager
172688	Outside Sales Specialist
52223	HSPD 12 Government Badging Credentialing Specialist
157160	Morning Executive Producer
```

## Predictions — do not read until step 3

What iteration 8's classifier answered for each of the rows above. Reading this before the labels
are written to disk destroys the measurement: the labeller would agree with it and the numbers
would decorate rather than measure.

```
32	OUT
109	UNKNOWN
175	UNKNOWN
638	OUT
988	UNKNOWN
1000	UNKNOWN
1035	UNKNOWN
1456	UNKNOWN
1652	UNKNOWN
1790	IN
1818	OUT
1956	IN
1986	IN
2274	UNKNOWN
2288	UNKNOWN
2383	UNKNOWN
2529	OUT
2584	OUT
2671	OUT
3003	UNKNOWN
3016	OUT
3299	UNKNOWN
3335	UNKNOWN
3481	UNKNOWN
3964	UNKNOWN
4035	OUT
4285	OUT
4338	OUT
4380	OUT
4390	OUT
4395	OUT
4437	OUT
4481	OUT
4501	OUT
4579	UNKNOWN
4586	OUT
4601	OUT
4673	OUT
4709	OUT
4772	OUT
4784	OUT
4849	OUT
4873	UNKNOWN
4892	UNKNOWN
4913	OUT
5018	UNKNOWN
5501	UNKNOWN
5553	OUT
5569	OUT
5626	OUT
5657	UNKNOWN
5872	IN
5888	OUT
5940	OUT
6038	UNKNOWN
6048	OUT
6190	OUT
6237	OUT
6434	OUT
6602	OUT
6660	OUT
6731	OUT
7028	OUT
7030	OUT
7032	OUT
7152	OUT
7153	OUT
7491	OUT
7690	OUT
8020	OUT
8021	OUT
8022	OUT
8143	OUT
8144	OUT
8149	OUT
8495	OUT
8733	OUT
8800	OUT
8811	OUT
8835	OUT
9007	UNKNOWN
9030	IN
9085	OUT
9305	UNKNOWN
9372	OUT
9415	OUT
9475	OUT
9476	OUT
9503	OUT
9507	OUT
9517	OUT
9613	OUT
9618	OUT
9806	OUT
10405	OUT
10817	UNKNOWN
11052	UNKNOWN
11182	UNKNOWN
11335	IN
11401	UNKNOWN
11576	UNKNOWN
11584	OUT
11618	UNKNOWN
11660	UNKNOWN
11678	UNKNOWN
11866	OUT
11991	UNKNOWN
12210	IN
12330	IN
12398	UNKNOWN
12458	UNKNOWN
13213	OUT
13216	OUT
13273	OUT
13356	OUT
13535	OUT
14601	UNKNOWN
14643	OUT
14762	OUT
15197	OUT
15242	UNKNOWN
15292	OUT
15486	UNKNOWN
15599	UNKNOWN
15651	OUT
15824	UNKNOWN
15856	UNKNOWN
16107	OUT
16214	UNKNOWN
16492	UNKNOWN
16702	UNKNOWN
16736	OUT
16826	UNKNOWN
17438	OUT
17777	OUT
17870	OUT
17904	OUT
18072	OUT
18656	IN
18702	OUT
18754	OUT
19081	OUT
19095	OUT
19110	OUT
19711	OUT
20389	OUT
20500	OUT
20553	OUT
20897	OUT
21003	OUT
21762	UNKNOWN
21780	OUT
22279	OUT
22379	OUT
22454	OUT
22651	OUT
23805	OUT
24169	OUT
24732	UNKNOWN
24815	OUT
24826	OUT
25213	UNKNOWN
25236	UNKNOWN
25404	OUT
25478	OUT
25794	OUT
25804	UNKNOWN
25923	UNKNOWN
25995	UNKNOWN
26187	UNKNOWN
26225	OUT
26261	UNKNOWN
26347	OUT
26569	OUT
26593	UNKNOWN
26602	OUT
28006	IN
28082	OUT
28083	UNKNOWN
28691	UNKNOWN
29015	OUT
29027	IN
29402	OUT
29547	UNKNOWN
29553	UNKNOWN
29671	UNKNOWN
29697	UNKNOWN
29787	UNKNOWN
30180	UNKNOWN
30183	UNKNOWN
30741	UNKNOWN
30904	UNKNOWN
31150	UNKNOWN
31160	UNKNOWN
31180	UNKNOWN
31369	UNKNOWN
31482	UNKNOWN
31923	UNKNOWN
32148	IN
33003	UNKNOWN
33412	UNKNOWN
34451	OUT
34465	OUT
34516	OUT
36359	OUT
36420	OUT
36551	OUT
36727	OUT
37127	OUT
37784	OUT
38237	UNKNOWN
38274	OUT
38447	UNKNOWN
38644	OUT
38773	UNKNOWN
38847	UNKNOWN
38990	OUT
39352	UNKNOWN
40275	UNKNOWN
40486	UNKNOWN
40494	UNKNOWN
40715	UNKNOWN
40903	UNKNOWN
41032	OUT
41067	UNKNOWN
41267	UNKNOWN
41524	UNKNOWN
41653	UNKNOWN
41728	UNKNOWN
41814	OUT
41871	UNKNOWN
41980	UNKNOWN
42103	UNKNOWN
42350	UNKNOWN
42765	UNKNOWN
43236	UNKNOWN
43239	UNKNOWN
43722	OUT
43726	IN
44016	UNKNOWN
44434	UNKNOWN
44617	IN
44835	UNKNOWN
44982	UNKNOWN
45208	UNKNOWN
45414	OUT
45534	IN
45661	UNKNOWN
45665	UNKNOWN
45687	UNKNOWN
45901	UNKNOWN
45916	UNKNOWN
45959	UNKNOWN
46334	UNKNOWN
46340	UNKNOWN
46382	UNKNOWN
46678	UNKNOWN
46736	OUT
46887	UNKNOWN
46905	UNKNOWN
47013	OUT
47021	UNKNOWN
47135	UNKNOWN
47221	UNKNOWN
47382	UNKNOWN
47415	UNKNOWN
47437	UNKNOWN
47655	OUT
47866	UNKNOWN
47889	UNKNOWN
47965	UNKNOWN
48059	UNKNOWN
48063	OUT
48066	OUT
48113	UNKNOWN
48166	UNKNOWN
48210	UNKNOWN
48255	OUT
48418	OUT
48437	OUT
48455	UNKNOWN
48482	UNKNOWN
48615	UNKNOWN
48681	UNKNOWN
48689	UNKNOWN
48841	UNKNOWN
48857	UNKNOWN
48875	IN
48911	UNKNOWN
49103	UNKNOWN
49116	UNKNOWN
49155	UNKNOWN
49294	UNKNOWN
49384	UNKNOWN
49392	UNKNOWN
49707	UNKNOWN
49723	UNKNOWN
49742	OUT
49758	UNKNOWN
49893	UNKNOWN
50044	UNKNOWN
50304	OUT
50903	OUT
51027	OUT
51108	OUT
51569	OUT
51587	UNKNOWN
51763	OUT
51823	OUT
52072	OUT
52223	UNKNOWN
52356	OUT
52422	OUT
52469	OUT
52536	OUT
52767	UNKNOWN
52897	UNKNOWN
53586	OUT
53587	OUT
53616	UNKNOWN
53973	OUT
54002	OUT
54012	OUT
54283	OUT
54295	OUT
54578	OUT
54885	OUT
54887	OUT
55093	OUT
55546	OUT
55752	UNKNOWN
55764	UNKNOWN
55771	UNKNOWN
55773	UNKNOWN
55856	UNKNOWN
55866	UNKNOWN
55892	UNKNOWN
55913	UNKNOWN
55916	UNKNOWN
55926	UNKNOWN
56177	OUT
56189	OUT
56328	OUT
56563	UNKNOWN
56590	UNKNOWN
56593	UNKNOWN
56653	OUT
56757	UNKNOWN
57582	OUT
57713	OUT
57820	UNKNOWN
59606	UNKNOWN
59711	UNKNOWN
59778	OUT
60338	UNKNOWN
61052	OUT
61185	OUT
61722	UNKNOWN
62204	UNKNOWN
62320	UNKNOWN
62416	UNKNOWN
62456	UNKNOWN
62592	UNKNOWN
62818	UNKNOWN
62850	UNKNOWN
62927	UNKNOWN
62979	OUT
63093	OUT
63334	OUT
63434	UNKNOWN
63471	UNKNOWN
63505	UNKNOWN
63509	UNKNOWN
63741	UNKNOWN
63763	UNKNOWN
63850	UNKNOWN
64187	OUT
64252	UNKNOWN
64295	UNKNOWN
64331	UNKNOWN
64514	IN
64600	UNKNOWN
64699	UNKNOWN
64826	UNKNOWN
64896	OUT
65125	UNKNOWN
65129	OUT
65145	UNKNOWN
65162	UNKNOWN
65372	UNKNOWN
65447	UNKNOWN
65555	OUT
65585	UNKNOWN
65927	UNKNOWN
65937	UNKNOWN
65970	UNKNOWN
66309	IN
66356	UNKNOWN
66439	UNKNOWN
66478	OUT
66553	UNKNOWN
66899	OUT
67231	UNKNOWN
67297	OUT
67408	IN
67413	UNKNOWN
67429	UNKNOWN
67503	OUT
67824	IN
67868	IN
67992	OUT
68119	IN
68317	OUT
68895	OUT
69897	UNKNOWN
69904	OUT
70191	OUT
70238	OUT
70401	OUT
71694	OUT
71892	IN
72312	UNKNOWN
72387	UNKNOWN
72444	UNKNOWN
72497	UNKNOWN
72819	IN
72946	UNKNOWN
72969	UNKNOWN
73038	UNKNOWN
73048	OUT
73572	UNKNOWN
73643	OUT
73665	UNKNOWN
73753	UNKNOWN
73902	OUT
74223	UNKNOWN
74319	UNKNOWN
74430	UNKNOWN
74467	OUT
74512	OUT
74600	UNKNOWN
74788	UNKNOWN
74833	UNKNOWN
74895	UNKNOWN
75082	UNKNOWN
75119	UNKNOWN
75156	UNKNOWN
75367	UNKNOWN
75471	UNKNOWN
75479	UNKNOWN
75600	OUT
75611	OUT
75652	OUT
75670	OUT
75690	OUT
75884	OUT
75887	OUT
76301	OUT
76586	OUT
76716	UNKNOWN
76725	UNKNOWN
76748	UNKNOWN
76761	OUT
77016	UNKNOWN
77028	UNKNOWN
77099	UNKNOWN
77358	UNKNOWN
77381	UNKNOWN
77411	UNKNOWN
77505	OUT
77585	UNKNOWN
77627	UNKNOWN
77666	UNKNOWN
77669	UNKNOWN
77860	UNKNOWN
77863	IN
80386	OUT
81005	UNKNOWN
81503	IN
81608	IN
82981	OUT
83001	OUT
83303	OUT
83606	OUT
83688	OUT
84377	OUT
85746	OUT
85909	IN
86143	IN
86690	IN
87219	UNKNOWN
87240	IN
89545	OUT
89781	OUT
89789	OUT
89844	OUT
89947	OUT
90793	OUT
91023	OUT
91084	OUT
91146	OUT
92514	OUT
92575	OUT
92576	OUT
93026	OUT
95087	OUT
95939	OUT
96961	OUT
97296	UNKNOWN
97932	OUT
98192	IN
98204	IN
98230	IN
98358	IN
98464	IN
98503	OUT
98906	UNKNOWN
99014	OUT
99221	IN
99439	IN
99451	IN
99535	IN
99541	IN
99742	OUT
99753	OUT
99913	IN
99945	IN
100338	IN
100405	UNKNOWN
101523	UNKNOWN
102030	OUT
104641	UNKNOWN
104946	UNKNOWN
105714	UNKNOWN
105814	UNKNOWN
106248	IN
106305	UNKNOWN
107076	UNKNOWN
107350	UNKNOWN
107631	UNKNOWN
107755	OUT
107946	OUT
108093	UNKNOWN
108117	UNKNOWN
108221	UNKNOWN
108467	UNKNOWN
108737	UNKNOWN
108751	IN
108846	UNKNOWN
108868	IN
108894	IN
108952	IN
109020	OUT
109335	UNKNOWN
109771	UNKNOWN
110107	OUT
110313	OUT
110360	OUT
110491	OUT
110627	OUT
110705	OUT
110801	UNKNOWN
111082	UNKNOWN
111161	OUT
111173	OUT
111175	OUT
111207	OUT
111257	OUT
111264	OUT
111365	OUT
112088	UNKNOWN
112462	OUT
112799	IN
112863	OUT
112903	IN
112931	OUT
112987	UNKNOWN
113041	OUT
113194	IN
114168	OUT
114178	IN
114196	IN
114295	IN
114716	IN
114728	IN
114730	UNKNOWN
114831	OUT
115125	UNKNOWN
115288	UNKNOWN
115888	OUT
116151	UNKNOWN
116308	UNKNOWN
116397	OUT
116719	UNKNOWN
116935	OUT
116944	UNKNOWN
117027	UNKNOWN
117180	UNKNOWN
118098	UNKNOWN
118371	OUT
118561	OUT
118954	UNKNOWN
119258	IN
119276	OUT
119311	UNKNOWN
119682	UNKNOWN
121488	OUT
121809	OUT
124426	OUT
124557	OUT
124856	OUT
126104	OUT
126133	OUT
126397	OUT
126493	OUT
127352	OUT
127386	OUT
127432	OUT
127458	OUT
127472	OUT
127508	OUT
127787	OUT
128225	UNKNOWN
130017	IN
130431	IN
130502	IN
130523	IN
130585	IN
130643	OUT
130673	IN
130883	IN
130898	IN
130921	IN
131216	OUT
131468	OUT
131551	OUT
133309	UNKNOWN
134098	OUT
134735	OUT
136020	IN
136834	OUT
136978	UNKNOWN
137196	UNKNOWN
137706	OUT
137749	IN
138085	OUT
138773	OUT
139242	OUT
139938	IN
139996	IN
140036	IN
140050	IN
140058	IN
140065	OUT
140106	IN
140159	OUT
140171	UNKNOWN
140177	UNKNOWN
140238	UNKNOWN
140275	UNKNOWN
140310	OUT
140321	UNKNOWN
140367	UNKNOWN
140402	UNKNOWN
140414	IN
140442	OUT
140454	IN
140478	IN
140510	IN
140513	OUT
140563	OUT
140570	OUT
140613	UNKNOWN
140704	OUT
140709	OUT
140755	OUT
140770	IN
140838	UNKNOWN
140854	OUT
140859	OUT
140885	OUT
140912	OUT
141000	UNKNOWN
141192	OUT
141198	OUT
141207	OUT
141222	OUT
141228	OUT
141310	OUT
141325	OUT
141339	OUT
141347	OUT
141353	OUT
141363	OUT
141401	UNKNOWN
141514	UNKNOWN
141516	UNKNOWN
141623	OUT
141650	OUT
141664	OUT
141668	UNKNOWN
141725	OUT
141798	OUT
141886	OUT
141901	OUT
141933	OUT
142007	OUT
142036	OUT
142041	OUT
142049	OUT
142096	UNKNOWN
142130	UNKNOWN
142159	UNKNOWN
142204	UNKNOWN
142242	UNKNOWN
142338	UNKNOWN
142358	UNKNOWN
142391	UNKNOWN
142434	OUT
142505	OUT
142649	OUT
142713	UNKNOWN
142868	UNKNOWN
143028	OUT
143078	OUT
143091	UNKNOWN
143146	UNKNOWN
143149	UNKNOWN
143292	OUT
143429	UNKNOWN
143566	OUT
143715	UNKNOWN
143735	OUT
144016	IN
144060	OUT
144166	IN
144179	IN
144253	UNKNOWN
144513	IN
144544	OUT
144617	UNKNOWN
144657	UNKNOWN
144680	OUT
144740	OUT
144812	OUT
144948	OUT
145114	OUT
145203	OUT
145374	OUT
145385	OUT
145529	UNKNOWN
145723	IN
145823	IN
145997	UNKNOWN
146061	UNKNOWN
146133	UNKNOWN
146163	UNKNOWN
146248	UNKNOWN
146330	UNKNOWN
146393	UNKNOWN
146415	UNKNOWN
146531	UNKNOWN
146640	UNKNOWN
146690	UNKNOWN
146777	UNKNOWN
146883	IN
146942	UNKNOWN
146982	UNKNOWN
147124	UNKNOWN
147365	OUT
147383	OUT
147407	OUT
147575	OUT
147613	OUT
147670	OUT
147707	OUT
147726	OUT
147763	OUT
147786	OUT
147856	OUT
147921	OUT
148247	UNKNOWN
148355	OUT
148386	IN
148463	UNKNOWN
148465	OUT
148564	UNKNOWN
148712	IN
148837	OUT
148931	OUT
148938	OUT
149084	OUT
149098	OUT
149100	OUT
149101	OUT
149509	UNKNOWN
149694	OUT
149703	UNKNOWN
149827	OUT
149899	UNKNOWN
149987	UNKNOWN
150053	UNKNOWN
150805	OUT
152123	OUT
152133	OUT
152156	OUT
152185	OUT
152233	OUT
152236	OUT
152250	OUT
152263	OUT
152289	OUT
152309	OUT
152328	OUT
152432	OUT
152472	OUT
152487	OUT
152492	OUT
152499	OUT
152628	OUT
152852	OUT
152862	OUT
153192	OUT
153457	OUT
153570	OUT
154004	OUT
154452	OUT
154471	OUT
154493	OUT
154655	OUT
155133	OUT
155283	OUT
155284	UNKNOWN
155308	OUT
155335	UNKNOWN
155342	UNKNOWN
155659	UNKNOWN
155847	OUT
155894	UNKNOWN
156047	OUT
156051	OUT
156061	OUT
156108	UNKNOWN
156119	OUT
156135	OUT
156157	OUT
156271	OUT
156418	OUT
156445	UNKNOWN
156447	OUT
156530	OUT
156777	UNKNOWN
156832	UNKNOWN
156871	OUT
156926	OUT
157114	UNKNOWN
157151	UNKNOWN
157160	OUT
157181	OUT
157286	OUT
157420	OUT
157464	OUT
157474	IN
157593	UNKNOWN
157655	OUT
157660	OUT
157767	OUT
157854	UNKNOWN
157864	OUT
158092	OUT
158094	OUT
158219	OUT
158363	OUT
158528	OUT
158537	OUT
158549	OUT
158627	OUT
158640	OUT
158663	OUT
158679	OUT
158714	OUT
159285	OUT
159383	OUT
159395	OUT
159405	OUT
159433	OUT
159715	OUT
159719	UNKNOWN
160208	OUT
160390	OUT
160557	OUT
160641	OUT
160667	OUT
160671	OUT
160708	UNKNOWN
160765	UNKNOWN
161593	UNKNOWN
161695	UNKNOWN
161808	UNKNOWN
162256	UNKNOWN
162264	UNKNOWN
162448	UNKNOWN
162506	OUT
162704	UNKNOWN
162743	OUT
162865	OUT
162990	OUT
164319	IN
164326	UNKNOWN
164556	UNKNOWN
164602	UNKNOWN
164708	OUT
164964	OUT
165101	OUT
165699	IN
165734	IN
166045	OUT
166154	OUT
166378	IN
166499	UNKNOWN
166512	IN
166957	UNKNOWN
167062	UNKNOWN
167157	UNKNOWN
167347	UNKNOWN
167566	UNKNOWN
168189	UNKNOWN
168417	OUT
168431	OUT
168460	OUT
168466	OUT
168499	OUT
168507	OUT
168554	OUT
168577	OUT
168677	OUT
169083	UNKNOWN
169088	UNKNOWN
169196	UNKNOWN
169200	OUT
169232	OUT
169281	OUT
169495	OUT
169711	UNKNOWN
170061	UNKNOWN
170207	UNKNOWN
170350	OUT
170362	OUT
170378	UNKNOWN
170474	OUT
170598	OUT
170673	OUT
171100	UNKNOWN
171467	IN
171570	OUT
172236	OUT
172594	OUT
172609	OUT
172688	OUT
172858	OUT
172867	OUT
172994	UNKNOWN
173038	OUT
173047	OUT
173176	OUT
173243	OUT
173244	OUT
173694	OUT
173752	UNKNOWN
174075	OUT
174408	OUT
174417	UNKNOWN
174422	OUT
174640	OUT
174909	UNKNOWN
174929	OUT
175678	UNKNOWN
175780	UNKNOWN
175824	UNKNOWN
175874	UNKNOWN
175888	UNKNOWN
175927	UNKNOWN
176033	UNKNOWN
176081	UNKNOWN
176103	UNKNOWN
176351	OUT
176418	IN
176713	OUT
176935	IN
177025	OUT
177125	IN
177147	OUT
177197	IN
177317	OUT
177375	UNKNOWN
177473	UNKNOWN
177730	OUT
177902	OUT
178039	UNKNOWN
178047	UNKNOWN
```
