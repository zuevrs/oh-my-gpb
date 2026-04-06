# Angle 3 — Sequencing and DB decision

## Question
Which module order gives the biggest product gain after core/api, and which DB path avoids duplication while improving meaningful test quality?

## Decision criteria
1. Does the module let the agent verify real side effects rather than HTTP wrappers?
2. Does it fit the current conservative bundle pattern?
3. Can it be taught cleanly with examples and unsupported boundaries?
4. Does it risk bundle sprawl or overclaiming?
5. Does it improve the planner’s default behavior soonest?

## Comparison summary

| Candidate | Product gain for meaningful tests | Fit with current bundle pattern | Integration risk | First-wave priority |
|---|---:|---:|---:|---:|
| `ccl-database-module` | Very high | High | Medium | 1 |
| `akita-gpb-kafka-mq-module` | High | Medium | High | 2 |
| `ccl-files-module` | High | Medium-high | Medium | 3 |
| `ccl-additional-steps-module` | Medium-high as support | Medium | Medium | 4 |
| `akita-gpb-db-module` | Medium | Medium | Medium | defer |

## Why `ccl-database-module` should win the DB story

### 1. It is more behavior-verification oriented
Compared with `akita-gpb-db-module`, the CCL DB module gives the agent higher-level verification steps that map directly to meaningful testing:
- assert rows with or without order;
- wait until DB state appears;
- save results in object/text/json shapes;
- assert update row count > 0.

This better matches the target product behavior: “check what the system really did”.

### 2. It hides connection plumbing better
`akita-gpb-db-module` exposes connection creation directly in feature steps. That is workable, but it makes the agent operate closer to infrastructure plumbing instead of business verification patterns.

For a capability pack whose goal is good default planning, this is the wrong default posture.

### 3. It avoids encouraging low-value query scripting
The agent should not be nudged toward hand-rolled DB setup in every feature. The CCL DB surface is closer to “query configured DB by business expectation and assert result” than “assemble DB client manually”.

### 4. It already includes one important async primitive
The built-in “wait until non-empty DB result” step directly supports eventual-consistency verification, which is central to meaningful integration tests.

## How to avoid duplication if `ccl-database-module` is chosen
- Make `ccl-database-module` the only first-wave DB capability bundle.
- Do **not** add `akita-gpb-db-module` in parallel as another DB bundle unless a concrete missing scenario appears.
- If a future gap appears, add only the minimal reviewed slice from `akita-gpb-db-module` needed to fill that gap, and document it as a specialized supplement rather than a second “general DB bundle”.
- In the manifest and examples, teach one primary DB story to avoid planner indecision.

## Recommended first-wave sequence

### 1. `ccl-database-module`
Reason:
- broadest gain against REST-only weakness;
- strongest default improvement in test meaningfulness;
- clean bundle boundary;
- direct support for persistence assertions and async DB convergence.

### 2. Kafka-focused slice of `akita-gpb-kafka-mq-module`
Reason:
- next major side-effect class after DB is broker emission/consumption;
- high value for event-driven systems;
- but should be introduced conservatively because the module covers Kafka, MQ, and Artemis.

### 3. file-focused slice of `ccl-files-module`
Reason:
- high value for generated artifacts and downloads;
- complements API and additional-module byte utilities well;
- should start with generic file + PDF surfaces rather than every file type.

### 4. support-focused slice of `ccl-additional-steps-module`
Reason:
- it improves composition and async handling across all prior bundles;
- but by itself it does not prove business side effects;
- therefore it should support the first three, not displace them.

## What should wait
- `akita-gpb-db-module` as a parallel DB story;
- non-Kafka transports from `akita-gpb-kafka-mq-module` unless demanded by product use cases;
- broad file-format coverage beyond the first meaningful file surfaces;
- Keycloak, Camunda, hooks, and other domain-heavy modules.

## Main risk in sequencing
The main risk is importing too much surface too early and teaching the agent a noisy or contradictory capability map.

Mitigation:
- keep first-wave bundles narrow and reviewed;
- examples must show cross-surface meaningful scenarios, not isolated step demos;
- unsupported/deferred boundaries should be explicit from day one.

## Confidence
High. This recommendation follows directly from the product goal and from the observed bundle authoring pattern in the repository.