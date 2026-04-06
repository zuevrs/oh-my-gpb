# Research Angle 3 — Adoption strategy, dependency map, risks, and PoC shortlist

## Goal
Собрать рекомендуемую стратегию внедрения модулей в `oh-my-akitagpb` как capability bundles / pack capabilities, определить зависимости, риски и PoC-проверки.

## Dependency map

### Foundational base
- `akita-gpb-core-module`
  - conceptual/runtime root for Akita step model
- `akita-gpb-api-module`
  - builds on Akita runtime and extends into HTTP workflows
- `ccl-helpers-module`
  - shared technical substrate for most CCL modules

### Direct dependency graph (practical view)
- `ccl-keycloak-module` → `ccl-helpers-module`
- `ccl-database-module` → `ccl-helpers-module`
- `ccl-files-module` → `ccl-helpers-module`
- `ccl-email-module` → `ccl-helpers-module`
- `ccl-pre-post-condition-module` → `ccl-helpers-module`
- `ccl-additional-steps-module` → `ccl-helpers-module` + `akita-gpb-api-module`
- `ccl-camunda-module` → `ccl-helpers-module` + `ccl-keycloak-module` + `akita-gpb-api-module`
- `akita-gpb-db-module` → `akita-gpb-core-module`
- `akita-gpb-helpers-module` → `akita-gpb-core-module`
- `akita-gpb-kafka-mq-module` → `akita-gpb-core-module` + `akita-gpb-helpers-module`
- `akita-gpb-hooks-module` → `akita-gpb-core-module` + `akita-gpb-api-module` + `akita-gpb-ui-module` + `akita-gpb-helpers-module` + `akita-gpb-kafka-mq-module`

### Strategic consequence
If the pack expands incrementally, the cleanest backbone is:
1. `core`
2. `api`
3. `ccl-helpers` as internal prerequisite concept
4. user-facing add-ons: `keycloak`, `database`, `files`
5. later domain packs: `additional-steps`, `kafka-mq`, `camunda`, `email`

## Overlaps and responsibility conflicts

### 1. Database duplication
- `akita-gpb-db-module` and `ccl-database-module` both cover DB querying.
- `ccl-database-module` appears more scenario-oriented and project-friendly:
  - named SQL files,
  - richer step wording,
  - better enterprise fit.
- Recommendation:
  - **do not adopt both early**.
  - choose `ccl-database-module` first.

### 2. Generic utility step overlap
- Core already includes common variable/assert/wait capabilities.
- `ccl-additional-steps-module` expands generic manipulations.
- Risk:
  - bundle authors may accidentally duplicate supported operations across bundles.
- Recommendation:
  - enforce strict capability contract separation.

### 3. Hook-heavy modules vs explicit-step modules
- Hooks (`akita-gpb-hooks-module`, `ccl-pre-post-condition-module`) can silently alter execution behavior.
- Risk:
  - hard to communicate through pack contracts,
  - may create invisible assumptions in generated artifacts.
- Recommendation:
  - package hook-centric behavior only after explicit-step bundles are mature.

### 4. Auth boundary overlap
- Generic authenticated API scenarios can be described with core+api only in theory, but `ccl-keycloak-module` provides a concrete token acquisition path.
- Recommendation:
  - model Keycloak as a focused auth add-on, not as part of generic API bundle.

## Infrastructure requirements by module family

| Module / family | Infra prerequisites | Operational notes |
|---|---|---|
| core/api | Java/Cucumber repo, config conventions, HTTP endpoints | lowest extra infra burden |
| keycloak | reachable Keycloak host, user/client credentials | secret management and token lifecycle needed |
| database | DB host access, JDBC driver compatibility, SQL files | CI network access and test data stability matter |
| files | test artifacts/files, document fixtures, optional image diff tooling | relatively self-contained |
| email | Outlook/EWS or SMTP reachability | often blocked by network policy |
| kafka-mq | Kafka / IBM MQ / Artemis access | highest infra variability among generic modules |
| camunda | Camunda REST, process models, auth, cleanup permissions | substantial environment coupling |
| hooks | docker/ui/reporting ecosystem | difficult to standardize as pack default |

## Recommended adoption phases

## Phase 1 — Minimal viable expansion
### Modules
- keep existing: `akita-gpb-core-module`, `akita-gpb-api-module`
- add next:
  - `ccl-keycloak-module`
  - `ccl-database-module`
  - `ccl-files-module`

### Why this phase
This is the smallest expansion that materially broadens enterprise test coverage without overcommitting to niche infrastructure.

### Scenarios unlocked
- authenticated REST flows via Keycloak token acquisition;
- database read/write verification tied to API calls;
- document/file/report validation (JSON/XML/PDF/XLSX/DOCX/archive/image).

### Changes needed in `oh-my-akitagpb`
- create new capability bundles under `assets/opencode/skills/akita-capability-*` for the selected modules;
- add reviewed capability contracts, examples, unsupported cases, provenance;
- update `assets/oma/capability-manifest.json` to activate them;
- extend tests validating manifest consistency and shipped asset paths;
- likely add templates/examples that reference auth/db/files use cases.

### Risks to retire in this phase
- confirm whether `ccl-helpers-module` should remain purely implicit or needs explicit representation in provenance/docs;
- verify that step extraction and contract generation are tractable for the larger `files` module;
- verify no overlap confusion between core and additional generic steps.

## Phase 2 — Coverage expansion
### Modules
- `ccl-additional-steps-module`
- `akita-gpb-kafka-mq-module`
- optional target-dependent: `ccl-email-module`

### Scenarios unlocked
- richer variable/date/array/polling expressions;
- async/event-driven verification via Kafka/MQ/Artemis;
- notification and attachment validation via email.

### Changes needed
- stronger capability taxonomy to separate:
  - synchronous API/data flows,
  - async messaging flows,
  - utility enrichers;
- examples and planning templates for event-driven systems;
- better unsupported-case documentation to avoid overclaiming streaming/message semantics.

### Risks to retire
- infra reachability assumptions in downstream repos;
- complexity of message contracts across Kafka, MQ, Artemis in one bundle;
- whether email belongs in the default pack or in an optional profile.

## Phase 3 — Advanced ecosystem integrations
### Modules
- `ccl-camunda-module`
- `ccl-pre-post-condition-module`
- potentially later `akita-gpb-hooks-module`
- optionally revisit `akita-gpb-helpers-module`

### Scenarios unlocked
- BPMN/DMN deployment and runtime validation;
- process activity/path/history checks;
- advanced suite orchestration via pre/post conditions;
- lifecycle/reporting hooks if product strategy expands that far.

### Changes needed
- likely introduce profile- or stack-based activation rather than one global manifest;
- deeper guidance for infra prerequisites and setup;
- sharper disclaimers around hook side effects and process cleanup behavior.

### Risks to retire
- Camunda-specific capability creep narrowing the product audience;
- invisible hook side effects causing trust issues;
- maintenance cost of niche bundles with lower broad adoption.

## Categorization summary

### Must-have for base startup
- `akita-gpb-core-module` (already active)
- `akita-gpb-api-module` (already active)
- `ccl-keycloak-module`
- `ccl-database-module`
- `ccl-files-module`

### Useful next
- `ccl-additional-steps-module`
- `akita-gpb-kafka-mq-module`
- `ccl-pre-post-condition-module`

### Optional / niche
- `ccl-email-module`
- `akita-gpb-helpers-module`
- `akita-gpb-db-module`

### Not now
- `ccl-camunda-module` (unless product focus shifts to BPM-heavy repos)
- `akita-gpb-hooks-module`

## PoC proposals

### PoC 1 — Contractability PoC for `ccl-files-module`
Question:
- can we reliably convert the very wide step surface into a bounded, reviewer-friendly capability contract?

Check:
- extract all public step annotations;
- cluster them into domains: file, json, xml, pdf, excel, docx, image, html;
- identify unsupported or ambiguous behaviors.

Success signal:
- one coherent capability bundle without excessive unsupported surface.

### PoC 2 — Database story choice PoC
Question:
- should the pack standardize on `ccl-database-module` or keep a more generic `akita-gpb-db-module` story?

Check:
- compare step readability, config burden, and scenario authoring ergonomics;
- test whether capability examples feel clearer with named SQL (`ccl-database`) than low-level DB access.

Success signal:
- clear recommendation to keep only one primary DB bundle in early phases.

### PoC 3 — Auth + API integration PoC
Question:
- does `ccl-keycloak-module` extend the current API capability cleanly without confusing responsibility boundaries?

Check:
- model one reviewed flow: get token → send authenticated request → assert response.

Success signal:
- Keycloak can remain a focused auth add-on bundle with minimal overlap.

### PoC 4 — Event-driven scope PoC
Question:
- is `akita-gpb-kafka-mq-module` small enough conceptually to be one bundle, or should Kafka/MQ/Artemis be split later?

Check:
- inspect step domains and examples;
- estimate whether one contract would be too broad.

Success signal:
- phased plan for event-driven support without overloading the manifest.

### PoC 5 — Profile-based activation PoC
Question:
- should `oh-my-akitagpb` remain one universal manifest or support optional capability profiles?

Check:
- model profiles such as:
  - `base-api-data`
  - `event-driven`
  - `camunda-stack`
- estimate complexity in install/update UX.

Success signal:
- profile system only if it reduces confusion more than it increases pack complexity.

## Risk register

### Product risks
- over-activating niche capabilities and making the pack feel heavier than it is;
- exposing modules whose infrastructure assumptions are uncommon in target repos;
- duplicate capability stories (DB, generic helpers).

### Technical risks
- hidden hook behavior is hard to encode in strict bundle contracts;
- large modules like `ccl-files-module` may require substantial curation effort;
- external/internal Maven publication assumptions may complicate provenance and docs.

### Adoption risks
- downstream teams may interpret shipped bundles as “fully supported runtime out of the box”;
- repos without Keycloak/Kafka/Camunda may receive irrelevant capability suggestions unless activation is disciplined.

## Recommended integration order
1. keep `core` and `api` stable;
2. add `ccl-keycloak-module`;
3. add `ccl-database-module`;
4. add `ccl-files-module`;
5. add `ccl-additional-steps-module`;
6. add `akita-gpb-kafka-mq-module`;
7. decide on `ccl-email-module` based on demand;
8. defer `ccl-camunda-module`, `ccl-pre-post-condition-module`, `akita-gpb-hooks-module` until targeted strategy exists.

## Confidence / unknowns
### High confidence
- the safest near-term strategy is selective expansion, not full-module import.
- keycloak/database/files form the best next wave.

### Medium confidence
- ordering of files vs database could swap depending on dominant downstream use cases.

### Unknowns
- whether future product direction prefers optional profiles;
- whether there is immediate user demand for event-driven or Camunda-heavy bundles.
