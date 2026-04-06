# Recommendation

## Executive summary
`oh-my-akitagpb` should evolve around one product goal: teach the agent to verify **real system behavior** after a trigger, not just HTTP wrappers. The current core/api bundles already give variable handling, request sending, response extraction, and schema-level response checks. What is missing is the side-effect surface that turns those primitives into meaningful tests.

The first wave after core/api should therefore be: **database first**, **broker second**, **files third**, **support utilities fourth**. The DB story should use **`ccl-database-module` as the primary path**, not `akita-gpb-db-module`, because it gives a better behavior-verification surface: parameterized queries, ordered/unordered row assertions, result extraction, update assertions, and a built-in wait-for-state pattern. `akita-gpb-db-module` should be deferred to avoid duplicating the DB story and confusing the planner.

## Problem framing: why REST-only generated tests are weak
REST-only generated tests are weak because they mostly prove that:
- an endpoint exists;
- the request reaches a controller;
- serialization works;
- the response shape looks plausible.

They usually do **not** prove that the system actually did the business work behind the HTTP contract. If a request is supposed to:
- persist rows;
- emit an event;
- generate a document;
- trigger an async flow;
- read or write files;

then a useful integration/component test must verify those side effects. Otherwise the agent will generate polite but shallow controller tests.

## Current oh-my-akitagpb integration pattern
The current repository already defines a clear capability-bundle pattern:
- exact-pin bundle id per reviewed module revision;
- runtime `SKILL.md` as an anti-hallucination guardrail;
- `capability-contract.json` as the reviewed step truth;
- `unsupported-cases.json` as explicit stop boundaries;
- `examples.json` as planner-facing usage patterns;
- `provenance.json` as redacted source-review metadata;
- bundle activation through `assets/oma/capability-manifest.json`;
- tests that enforce file presence, pin alignment, unsupported boundaries, and runtime-path correctness.

This means new module integration should continue to be:
1. source-backed;
2. exact-pin;
3. conservative;
4. explicit about unsupported behavior.

## What meaningful tests require from the capability pack
To write meaningful tests, the pack must let the agent do five things well:
1. **Trigger behavior** — already largely covered by API/core.
2. **Observe persistence** — DB verification.
3. **Observe messaging** — broker/event verification.
4. **Observe artifacts** — generated/downloaded files and their contents.
5. **Handle eventual consistency and glue logic** — polling, byte/file conversion, multipart support, variable shaping.

The missing value is not “more Akita modules in general”. It is **the right post-trigger evidence surfaces**.

## Candidate module analysis

### `ccl-database-module`
**Unlocks:**
- REST + DB verification
- component/integration tests that assert persisted business state
- async DB polling until state appears
- extraction of DB values for later broker/file/assertion steps

**Why it matters:**
This is the fastest way to stop generating controller-wrapper tests and start checking what actually changed in the system.

**How it should land in the pack:**
- separate capability bundle;
- reviewed categories: query with params, query with inline table params, save result, assert empty/non-empty, ordered/unordered row assertions, extract values, save JSON string, update/delete/insert and affected-row checks;
- explicit unsupported boundaries around config internals, helper methods, vendor guarantees, and generic DB admin semantics.

**Verdict:** first-wave, highest priority.

### `akita-gpb-kafka-mq-module`
**Unlocks:**
- REST + broker verification
- message-driven and event-driven verification
- presence/absence of emitted messages
- extraction by key/header/jsonPath
- typed JSON/XML/TEXT message matching

**Why it matters:**
This is the main path from “202 Accepted” to “the business event was actually produced”.

**How it should land in the pack:**
- separate capability bundle, but not as one huge transport-agnostic blob;
- first implementation should focus on a **Kafka-reviewed slice**;
- MQ and Artemis should be explicitly deferred or split into later bundles.

**Verdict:** first-wave, second priority.

### `ccl-files-module`
**Unlocks:**
- REST + file verification
- report/export/document assertions
- file download detection
- byte[] -> file conversion support chain
- PDF content and table assertions
- archive verification

**Why it matters:**
For many systems the real user-facing side effect is a generated file, not the HTTP payload.

**How it should land in the pack:**
- separate capability bundle;
- first reviewed slice should focus on generic file handling + download capture + PDF verification;
- broader DOCX/Excel/HTML/image/XML/JSON file surfaces can come later if needed.

**Verdict:** first-wave, third priority.

### `ccl-additional-steps-module`
**Unlocks:**
- polling until async state converges
- multipart request body construction
- byte/body/base64 conversion
- regex/string/date support for scenario composition

**Why it matters:**
This is not the business proof itself, but it removes the friction that otherwise keeps the agent at shallow HTTP checks.

**How it should land in the pack:**
- support-oriented bundle, not a kitchen-sink bundle;
- first reviewed slice should emphasize polling/waiting, bytes/files, multipart, and only the string/date helpers that materially support meaningful flows.

**Verdict:** first-wave support bundle, fourth priority.

## Recommended first-wave modules
1. **`ccl-database-module`**
2. **Kafka-focused slice of `akita-gpb-kafka-mq-module`**
3. **file-focused slice of `ccl-files-module`**
4. **support-focused slice of `ccl-additional-steps-module`**

## DB-story decision
### Decision
Choose **`ccl-database-module`** as the primary DB path.

### Why
Compared with `akita-gpb-db-module`, `ccl-database-module` is more aligned with meaningful testing because it gives the agent:
- richer result assertions;
- ordered and unordered multi-row checks;
- JSON/result extraction forms;
- update-result checks;
- built-in wait-for-state support.

`akita-gpb-db-module` exposes more low-level connection plumbing and offers a narrower reviewed verification surface. That makes it less suitable as the default behavior-verification story for the pack.

### How to avoid duplicates in the pack
- Do not ship both as equal “DB bundles” in the first wave.
- Make `ccl-database-module` the only active DB capability bundle after core/api.
- If a later concrete gap appears, import only the missing reviewed slice from `akita-gpb-db-module` and document it as a specialized supplement, not a second general DB capability.

## Capability design implications

### 1. Bundle by practical test surface, not by README breadth
Some candidate modules are broad enough that a one-bundle-per-module policy would overclaim or overload the agent. The current bundle pattern supports narrower curation and should keep doing that.

Recommended bundle shapes:
- `akita-capability-ccl-database-module-<pin>`
- `akita-capability-akita-gpb-kafka-module-<pin>` or an explicitly Kafka-scoped bundle derived from the larger kafka/mq module
- `akita-capability-ccl-files-module-<pin>` with first-wave file/PDF slice
- `akita-capability-ccl-additional-meaningful-support-<pin>` or equivalent support-focused naming

### 2. Reviewed step categories must be product-shaped
For each new bundle, reviewed categories should be grouped by what the agent is trying to prove:
- persistence state changed;
- message emitted/absent;
- file generated and contents correct;
- async condition converged;
- multipart/body/bytes utilities needed to reach those checks.

### 3. Unsupported boundaries should be explicit and aggressive
Every new bundle should include explicit unsupported cases such as:
- helpers and hooks are not standalone capabilities;
- README claims do not expand runtime truth;
- deferred transport families or file formats are unsupported until reviewed;
- generic wording is narrowed to the observed implementation semantics.

### 4. Examples should show cross-surface meaningful scenarios
Examples should not be isolated step demos. They should teach patterns like:
- API -> DB -> assert rows
- API -> Kafka -> assert event payload
- API -> bytes -> file -> assert PDF
- API -> polling -> terminal state
- API -> broker -> DB/file follow-up proof

## Suggested implementation order

### 1. Start with `ccl-database-module`
Why this first:
- biggest immediate gain in meaningfulness;
- strongest replacement for shallow REST-only tests;
- cleanest fit with current bundle contract pattern.

### 2. Then add Kafka-focused broker verification
Why second:
- next strongest side-effect surface after persistence;
- high product value for event-driven systems;
- should be scoped tightly to avoid transport sprawl.

### 3. Then add file/PDF verification
Why third:
- major product gain for document/report/export systems;
- composes naturally with API and support utilities.

### 4. Then add support utilities
Why fourth:
- they improve the planner’s ability to use the earlier bundles well;
- but on their own they do not prove business behavior.

## Concrete next implementation task
The next implementation task should start with **`ccl-database-module`** and produce a fully integrated reviewed bundle in the existing repository pattern.

That task should do exactly this:
1. create a new exact-pin capability bundle for `ccl-database-module`;
2. author `SKILL.md` with the same conservative operating rules used by core/api;
3. build `capability-contract.json` from the reviewed annotated DB step surface;
4. record explicit `unsupported-cases.json` boundaries;
5. add `examples.json` that teach meaningful patterns such as:
   - API trigger -> DB row assertion
   - API trigger -> DB polling -> terminal state assertion
   - DB extraction -> downstream verification
6. add `provenance.json` with the same redacted authoring policy shape;
7. wire the bundle into `assets/oma/capability-manifest.json`;
8. extend capability tests to include the new bundle and its unsupported boundary expectations.

## Comparison matrix

| Dimension | `ccl-database-module` | `akita-gpb-kafka-mq-module` | `ccl-files-module` | `ccl-additional-steps-module` | `akita-gpb-db-module` |
|---|---|---|---|---|---|
| Verifies side effects beyond HTTP | Strong | Strong | Strong | Support-only | Medium |
| Best meaningful-test role | Persistence proof | Event proof | Artifact proof | Async/glue support | Low-level DB supplement |
| Fit as first-wave bundle | Excellent | Good if scoped | Good if scoped | Good as support slice | Weak as primary DB story |
| Main integration risk | moderate | high due to breadth | moderate | moderate due to kitchen-sink surface | duplication / lower product fit |
| Recommended status | implement first | implement second | implement third | implement fourth | defer |

## Bottom line
If the goal is to make the agent write meaningful Akita tests, the pack should stop expanding horizontally by generic module coverage and start expanding **along evidence surfaces**. The first missing evidence surface is database state. That makes `ccl-database-module` the correct next implementation target.