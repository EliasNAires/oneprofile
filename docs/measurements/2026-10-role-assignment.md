# Roles on the vacancies, and role nearness (#25, steps 4 and 5)

2026-10-26, on `classified-2026-10-25`, with the role list of 4bd1301 before step 2's agent pass. Run
with `POST /roles` (after `POST /title-breakdowns`) and then `POST /role-nearness`; the figures below
are queries over `normalized_vacancy.roles` and `role_nearness`.

## Coverage

Of the 155,767 vacancies of the engineering subset, **120,772 (77.5%) have a role**, above the 70% of
ADR-0024. 416 have two (a title naming two heads, each with its own role). 34,995 have none, 370 of
them because the breakdown left no normalized title (a bare `Manager`). This is the rule's count, not
its precision: that is step 6's blind sample.

| ATS | vacancies | with a role |
|---|---:|---:|
| Workday | 78,913 | 71.4% |
| Greenhouse | 41,727 | 82.3% |
| Ashby | 22,575 | 87.3% |
| Lever | 12,552 | 82.6% |

**Beyond the sorted titles.** 33,942 vacancies carry only normalized titles on 1 to 4 vacancies, which
`titles.tsv` does not hold; the rule gives 17,081 of them (50.3%) a role from their head and kind words.

## The roles

Share of the subset; a vacancy with two roles counts in each. None is over 25%.

| role | vacancies | share |
|---|---:|---:|
| software-engineer | 26,105 | 16.76% |
| platform-engineer | 10,797 | 6.93% |
| data-engineer | 7,341 | 4.71% |
| ai-engineer | 6,936 | 4.45% |
| security-engineer | 6,244 | 4.01% |
| full-stack-engineer | 5,946 | 3.82% |
| product-manager | 4,731 | 3.04% |
| solutions-architect | 4,210 | 2.70% |
| qa-engineer | 3,934 | 2.53% |
| data-scientist | 3,799 | 2.44% |
| systems-administrator | 3,654 | 2.35% |
| machine-learning-engineer | 3,562 | 2.29% |
| backend-engineer | 3,515 | 2.26% |
| support-engineer | 3,259 | 2.09% |
| sales-engineer | 2,799 | 1.80% |
| network-engineer | 2,662 | 1.71% |
| embedded-engineer | 2,451 | 1.57% |
| enterprise-platform-developer | 2,430 | 1.56% |
| program-manager | 2,426 | 1.56% |
| data-analyst | 2,373 | 1.52% |
| mobile-engineer | 2,311 | 1.48% |
| forward-deployed-engineer | 2,037 | 1.31% |
| frontend-engineer | 1,970 | 1.26% |
| business-analyst | 1,790 | 1.15% |
| research-scientist | 1,689 | 1.08% |
| robotics-engineer | 1,111 | 0.71% |
| security-analyst | 1,106 | 0.71% |

`robotics-engineer` and `security-analyst` are under the 1% the review estimated over all titles; the
agent pass over the unreached titles may raise them.

## Left without a role

The most common normalized titles of vacancies with no role, the input for step 2's agent pass and the
next snapshot: `systems engineer` 1,560 (no role by the criterion), `analyst` 836, `automation
engineer` 712, `consultant` 333, `android team lead` 248, `mobile expert` 207 (an IN leak, #95),
`design engineer` 185, `technical product` 178, `technical consultant` 170, `gtm engineer` 139,
`performance engineer` 138, `application developer` 122, `specialist` 121, `design verification
engineer` 119, `application engineer` 115, `verification engineer` 111, `financial analyst` 108,
`intern` 106, `field engineer` 106, `data analytics` 99, `technical writer` 97, `analytics` 90,
`scientist` 86, `stage` 86, `developer relations` 82.

## Role nearness

`workers/role/RoleNearnessRule`: for each role, the share of its vacancies naming each skill, weighted
by the logarithm of the subset's vacancies over those naming the skill; two roles' nearness is the
cosine. Unlike `scripts/role-titles`, the vectors are not cut to their 60 heaviest skills, since 27
roles make only 351 pairs. Each role's three nearest:

| role | nearest |
|---|---|
| ai-engineer | forward-deployed-engineer 0.83, machine-learning-engineer 0.69, solutions-architect 0.67 |
| backend-engineer | software-engineer 0.84, full-stack-engineer 0.76, forward-deployed-engineer 0.66 |
| business-analyst | product-manager 0.71, program-manager 0.63, data-analyst 0.60 |
| data-analyst | data-scientist 0.67, business-analyst 0.60, data-engineer 0.60 |
| data-engineer | solutions-architect 0.66, data-scientist 0.62, data-analyst 0.60 |
| data-scientist | machine-learning-engineer 0.68, data-analyst 0.67, data-engineer 0.62 |
| embedded-engineer | robotics-engineer 0.48, software-engineer 0.37, qa-engineer 0.31 |
| enterprise-platform-developer | solutions-architect 0.31, business-analyst 0.31, product-manager 0.29 |
| forward-deployed-engineer | ai-engineer 0.83, software-engineer 0.72, solutions-architect 0.72 |
| frontend-engineer | full-stack-engineer 0.79, software-engineer 0.59, forward-deployed-engineer 0.48 |
| full-stack-engineer | software-engineer 0.87, frontend-engineer 0.79, backend-engineer 0.76 |
| machine-learning-engineer | research-scientist 0.83, ai-engineer 0.69, data-scientist 0.68 |
| mobile-engineer | software-engineer 0.20, full-stack-engineer 0.19, backend-engineer 0.19 |
| network-engineer | sales-engineer 0.47, support-engineer 0.46, systems-administrator 0.35 |
| platform-engineer | security-engineer 0.67, software-engineer 0.67, backend-engineer 0.64 |
| product-manager | program-manager 0.78, business-analyst 0.71, solutions-architect 0.60 |
| program-manager | product-manager 0.78, business-analyst 0.63, solutions-architect 0.41 |
| qa-engineer | software-engineer 0.56, full-stack-engineer 0.43, sales-engineer 0.41 |
| research-scientist | machine-learning-engineer 0.83, data-scientist 0.54, ai-engineer 0.52 |
| robotics-engineer | embedded-engineer 0.48, research-scientist 0.48, software-engineer 0.37 |
| sales-engineer | solutions-architect 0.82, security-engineer 0.72, forward-deployed-engineer 0.69 |
| security-analyst | security-engineer 0.71, support-engineer 0.55, systems-administrator 0.54 |
| security-engineer | sales-engineer 0.72, security-analyst 0.71, platform-engineer 0.67 |
| software-engineer | full-stack-engineer 0.87, backend-engineer 0.84, forward-deployed-engineer 0.72 |
| solutions-architect | sales-engineer 0.82, forward-deployed-engineer 0.72, software-engineer 0.71 |
| support-engineer | systems-administrator 0.86, sales-engineer 0.69, security-engineer 0.59 |
| systems-administrator | support-engineer 0.86, sales-engineer 0.62, security-engineer 0.59 |

Two things to read with care. `mobile-engineer` is near nothing (0.20 at most): its distinctive skills
(Swift, Kotlin, the mobile platforms) are named by few other vacancies, so its vector points its own
way. Skills alone put `sales-engineer` near `security-engineer` (0.72) and `support-engineer` near
`systems-administrator` (0.86): the skills are shared and the work is not. That is why nearness never
moves a title (ADR-0024); the Finder offers near roles, and the visitor picks.
