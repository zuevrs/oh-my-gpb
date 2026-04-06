# RECOMMENDATION

## Summary

Spike shows that `docs/akita` contains a meaningful expansion set for `oh-my-akitagpb`, but the right strategy is **selective adoption**, not importing all modules into the pack at once.

Current product shape matters: `oh-my-akitagpb` is a **Node/TypeScript bootstrap pack** that ships reviewed, pinned capability bundles for downstream Java repos. Today it intentionally activates only `akita-gpb-core-module` and `akita-gpb-api-module`. That packaging model strongly favors modules with:
- explicit step surfaces,
- low hidden side effects,
- broad enterprise usefulness,
- clear capability contract boundaries.

Based on that, the strongest next-wave candidates are:
- `ccl-keycloak-module`
- `ccl-database-module`
- `ccl-files-module`

These three extend the current core+api baseline in the most practical way while keeping risk and conceptual sprawl under control.

---

## Inventory of discovered modules

| Module | Primary role | User-facing step value | Recommendation class |
|---|---|---|---|
| `akita-gpb-core-module` | Akita runtime/core steps | high | already active / must-have |
| `akita-gpb-api-module` | HTTP/API testing | high | already active / must-have |
| `akita-gpb-db-module` | generic DB access | medium | optional / overlaps |
| `akita-gpb-helpers-module` | SSH/Testcontainers/helpers | medium-low | optional / niche |
| `akita-gpb-hooks-module` | reporting/docker/hooks | low as explicit capability | not now |
| `akita-gpb-kafka-mq-module` | Kafka/MQ/Artemis testing | high in async systems | useful next |
| `ccl-additional-steps-module` | generic utility/polling steps | medium-high | useful next |
| `ccl-camunda-module` | Camunda process/BPMN testing | high in BPM-heavy systems | not now / targeted only |
| `ccl-database-module` | named SQL DB testing | high | must-have next |
| `ccl-email-module` | mailbox verification | medium | optional / niche |
| `ccl-files-module` | files/docs/json/xml/pdf/xlsx/docx/html | very high | must-have next |
| `ccl-helpers-module` | shared CCL helper substrate | indirect only | prerequisite, not user-facing |
| `ccl-keycloak-module` | Keycloak token acquisition | high leverage | must-have next |
| `ccl-pre-post-condition-module` | pre/post condition hooks | medium later | useful next / later |

---

## Module-by-module assessment

### `akita-gpb-core-module`
**What it gives:** core runtime, scenario variables, common steps, foundational Akita behavior.  
**Role in platform:** base capability bundle already present and correct.  
**Recommendation:** keep as foundational baseline.

### `akita-gpb-api-module`
**What it gives:** reviewed HTTP request/response extraction and schema validation capability.  
**Role in platform:** strongest broad fit after core; already active.  
**Recommendation:** keep as stable base and build outward from it.

### `ccl-keycloak-module`
**What it gives:** focused Keycloak token acquisition step plus token cache hook.  
**Typical scenarios:** obtain bearer token → call protected API → assert response.  
**Dependencies:** `ccl-helpers-module`; Keycloak host and credentials in `.conf`.  
**Role in platform:** excellent next add-on to API-first flows.  
**Recommendation:** **adopt early**.

### `ccl-database-module`
**What it gives:** named SQL execution, result assertions, DML validation, variable extraction.  
**Typical scenarios:** verify side effects after API call; read/write DB state; compare query results.  
**Dependencies:** `ccl-helpers-module`, Spring JDBC, DB access, SQL files under `resources/queries`.  
**Role in platform:** very practical for enterprise integration tests.  
**Recommendation:** **adopt early**, and prefer it over `akita-gpb-db-module` as the primary DB story.

### `ccl-files-module`
**What it gives:** broad verification surface for PDF, JSON, XML, HTML, DOCX, XLSX, PNG, archives, generic files.  
**Typical scenarios:** verify generated docs, parse structured artifacts, compare payloads/files.  
**Dependencies:** `ccl-helpers-module` plus document/image libraries.  
**Role in platform:** broadest business value after core/api/database.  
**Recommendation:** **adopt early**.

### `ccl-additional-steps-module`
**What it gives:** utility steps for strings, arrays, dates, expressions, waits, polling loops.  
**Role in platform:** good scenario authoring amplifier once core modules are in use.  
**Recommendation:** **useful next**, but not necessary for first expansion wave.

### `akita-gpb-kafka-mq-module`
**What it gives:** Kafka, IBM MQ, Artemis steps for send/read/assert flows.  
**Typical scenarios:** async integration testing, queue/topic verification, payload/header assertions.  
**Dependencies:** core + helpers + broker connectivity.  
**Role in platform:** very strong if target repos are event-driven.  
**Recommendation:** **Phase 2**, after base synchronous capabilities are stable.

### `ccl-email-module`
**What it gives:** inbox verification, mail body/attachment/link extraction.  
**Dependencies:** EWS/SMTP connectivity and permitted execution environment.  
**Role in platform:** useful for notification-heavy repos, but narrower audience.  
**Recommendation:** optional / niche.

### `ccl-pre-post-condition-module`
**What it gives:** reusable pre/post scenario orchestration for parameterized tests.  
**Role in platform:** suite optimization and orchestration, not core day-1 business coverage.  
**Recommendation:** later-stage adoption.

### `ccl-camunda-module`
**What it gives:** Camunda REST/process inspection, BPMN/DMN deployment helpers, incident and activity path assertions.  
**Dependencies:** `ccl-keycloak-module`, `akita-gpb-api-module`, Camunda infra, cleanup hooks, config wiring.  
**Role in platform:** extremely valuable in Camunda-heavy repos, but too domain-specific for early default inclusion.  
**Recommendation:** **not now** unless product strategy explicitly targets Camunda stacks.

### `akita-gpb-db-module`
**What it gives:** lower-level DB access story based on core.  
**Issue:** overlaps with `ccl-database-module`.  
**Recommendation:** do not prioritize early; revisit only if a generic non-CCL DB story is needed.

### `akita-gpb-helpers-module`
**What it gives:** SSH/Testcontainers/helper surface.  
**Role in platform:** infra-enabler, but narrower as a reviewed bundle.  
**Recommendation:** optional / niche.

### `akita-gpb-hooks-module`
**What it gives:** hook-driven lifecycle behavior (Allure, Docker, masking, cross-module orchestration).  
**Issue:** heavy dependencies, hidden side effects, and coupling to modules not yet adopted, including UI.  
**Recommendation:** **not now**.

### `ccl-helpers-module`
**What it gives:** shared utility substrate used by many CCL modules.  
**Role in platform:** foundational prerequisite, but not a user-facing capability bundle by itself.  
**Recommendation:** treat as an implementation dependency/provenance concept, not as an active capability bundle.

---

## Dependency and integration map

### Core dependency backbone
- `akita-gpb-core-module`
- `akita-gpb-api-module`
- `ccl-helpers-module` (shared technical prerequisite for CCL family)

### Downstream dependency map
- `ccl-keycloak-module` → `ccl-helpers-module`
- `ccl-database-module` → `ccl-helpers-module`
- `ccl-files-module` → `ccl-helpers-module`
- `ccl-email-module` → `ccl-helpers-module`
- `ccl-pre-post-condition-module` → `ccl-helpers-module`
- `ccl-additional-steps-module` → `ccl-helpers-module` + `akita-gpb-api-module`
- `ccl-camunda-module` → `ccl-helpers-module` + `ccl-keycloak-module` + `akita-gpb-api-module`
- `akita-gpb-kafka-mq-module` → `akita-gpb-core-module` + `akita-gpb-helpers-module`
- `akita-gpb-hooks-module` → core + api + ui + helpers + kafka/mq

### Key overlaps / conflicts
1. **DB overlap:** `akita-gpb-db-module` vs `ccl-database-module`  
   - Recommendation: pick **one** early. Prefer `ccl-database-module`.

2. **Generic utility overlap:** core vs `ccl-additional-steps-module`  
   - Recommendation: document boundaries carefully before adding utility bundle.

3. **Hook invisibility risk:** hook-centric modules are harder to expose safely than explicit step modules.  
   - Recommendation: defer hook-heavy modules.

### Infrastructure requirements
- **API baseline:** HTTP endpoints, config conventions
- **Keycloak:** auth host, users/clients, secrets
- **Database:** reachable DBs, JDBC driver match, SQL query files
- **Files:** generated artifacts / document fixtures, optional image diff setup
- **Kafka/MQ:** broker connectivity, topic/queue config, CI networking
- **Camunda:** Camunda REST, BPMN/DMN assets, auth, cleanup permissions
- **Email:** reachable mail infra (EWS/SMTP)

---

## Recommended adoption phases

## Phase 1 — Minimal working contour
### Modules to connect
- keep active:
  - `akita-gpb-core-module`
  - `akita-gpb-api-module`
- add:
  - `ccl-keycloak-module`
  - `ccl-database-module`
  - `ccl-files-module`

### Scenarios covered
- authenticated API flows;
- API + DB verification flows;
- document/file/report/content verification.

### Changes needed in the project
- create reviewed capability bundles for those modules in `assets/opencode/skills/akita-capability-*`;
- add contracts, examples, unsupported cases, provenance;
- update `assets/oma/capability-manifest.json`;
- extend tests around manifest and bundle integrity;
- enrich generated templates/examples with auth/db/files scenarios.

### Risks / unknowns to close
- confirm how `ccl-helpers-module` should appear in docs/provenance;
- verify that `ccl-files-module` can be bounded cleanly into a contract;
- verify no pack confusion from DB capability terminology.

## Phase 2 — Coverage expansion
### Modules to connect
- `ccl-additional-steps-module`
- `akita-gpb-kafka-mq-module`
- optionally `ccl-email-module`

### Scenarios covered
- richer utility/polling flows;
- event-driven integration testing;
- email notification validation.

### Changes needed
- stronger capability taxonomy;
- examples for async/event-driven flows;
- explicit unsupported-case docs for streaming/broker-specific behavior.

### Risks / unknowns to close
- whether one messaging bundle is too broad;
- whether email belongs in the default pack or optional profile;
- infra variability across downstream repos.

## Phase 3 — Advanced integrations
### Modules to connect
- `ccl-camunda-module`
- `ccl-pre-post-condition-module`
- optionally later `akita-gpb-hooks-module`

### Scenarios covered
- Camunda BPMN/DMN lifecycle and process assertions;
- advanced scenario orchestration and suite reuse;
- broader lifecycle hook integrations.

### Changes needed
- likely profile-based activation or targeted bundle sets;
- stronger prerequisites and setup guidance;
- more careful lifecycle-side-effect documentation.

### Risks / unknowns to close
- product audience narrowing;
- hook side effects;
- maintenance cost of niche bundles.

---

## Risks / unknowns

### Main risks
- over-expanding the pack beyond its reviewed-capability philosophy;
- activating modules whose infrastructure assumptions are not common;
- duplicating capability stories (especially DB and utility steps);
- exposing hook behavior that is hard to describe and trust.

### Unknowns
- whether future roadmap should support **profiles** instead of one global manifest;
- whether target downstream repos more often need DB/files or Kafka/Camunda first;
- whether `.conf` conventions used by CCL modules are already standard in expected adopters.

---

## PoC proposals

### PoC 1 — `ccl-files-module` capability-contract PoC
Goal: prove this wide module can still be represented as a bounded reviewed contract.  
Output: grouped step inventory + first contract draft.

### PoC 2 — DB story choice PoC
Goal: choose decisively between `ccl-database-module` and `akita-gpb-db-module`.  
Output: comparison of authoring ergonomics, clarity, and fit for generated artifacts.

### PoC 3 — Keycloak + API integration PoC
Goal: validate a clean reviewed flow: token acquisition → authenticated request → assertions.  
Output: sample bundle references/examples proving low overlap.

### PoC 4 — Messaging bundling PoC
Goal: determine whether Kafka/MQ/Artemis should stay one bundle or later split by domain.  
Output: contract-surface estimate and recommendation.

### PoC 5 — Profile activation PoC
Goal: test whether profile-based capability activation reduces confusion.  
Candidate profiles:
- `base-api-data`
- `event-driven`
- `camunda-stack`

---

## Final recommendation

### Recommended integration order
1. keep `akita-gpb-core-module` and `akita-gpb-api-module` as the stable foundation;
2. add `ccl-keycloak-module`;
3. add `ccl-database-module`;
4. add `ccl-files-module`;
5. add `ccl-additional-steps-module`;
6. add `akita-gpb-kafka-mq-module`;
7. decide on `ccl-email-module` only if there is demonstrated demand;
8. defer `ccl-camunda-module`, `ccl-pre-post-condition-module`, and `akita-gpb-hooks-module` until there is a targeted product strategy.

### Shortlist for first implementation wave
- `ccl-keycloak-module`
- `ccl-database-module`
- `ccl-files-module`

### Shortlist to leave untouched for now
- `ccl-camunda-module`
- `akita-gpb-hooks-module`
- `akita-gpb-db-module` (until DB story is finalized)

### Practical decision
Do **not** try to mirror all discovered Akita/CCL modules into the pack.  
Instead, evolve `oh-my-akitagpb` as a **reviewed, high-signal capability pack** with deliberate phases:
- Phase 1: auth + database + files,
- Phase 2: utility + messaging,
- Phase 3: process orchestration and advanced hooks.

That path gives the best balance of usefulness, confidence, and maintainability.
