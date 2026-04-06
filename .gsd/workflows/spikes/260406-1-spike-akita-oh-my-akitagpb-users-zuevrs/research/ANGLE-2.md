# Research Angle 2 — Project fit assessment for `oh-my-akitagpb`

## Goal
Сопоставить найденные Akita/CCL-модули с реальными целями текущего проекта `oh-my-akitagpb`.

## Current project shape

### What `oh-my-akitagpb` is today
По коду и README проект сейчас представляет собой:
- **Node/TypeScript CLI bootstrap pack**, а не Java test runtime;
- устанавливает управляемую поверхность `.oma/` и `.opencode/` в целевой Java-репозиторий;
- управляет lifecycle-командами `install`, `update`, `doctor`;
- поставляет workflow-команды `/akita-scan`, `/akita-plan`, `/akita-write`, `/akita-accept`;
- хранит capability bundles в `assets/opencode/skills/**` и активирует их через `assets/oma/capability-manifest.json`.

### What is active now
Current manifest activates only two capability bundles:
- `akita-gpb-core-module@c795936046e`
- `akita-gpb-api-module@223b2561bbc`

This is reinforced by:
- `README.md`
- `assets/oma/capability-manifest.json`
- `tests/capabilities/capability-manifest.test.ts`
- capability skill docs under `assets/opencode/skills/akita-capability-*`

### Architectural implication
Проект **не исполняет** Akita/CCL Java-модули напрямую. Он:
1. описывает/пакует их возможности для OpenCode runtime,
2. материализует инструкции и capability bundles в целевые репозитории,
3. ограничивает активный capability surface манифестом.

Следовательно, основной вопрос fit-а звучит так:
> Какие Akita/CCL-модули стоит превращать в следующие capability bundles этого pack-а, чтобы расширение было практичным, безопасным и последовательным?

## Fit criteria used
Я оценивал каждый модуль по следующим критериям:
1. **Alignment with current pack model** — легко ли выразить модуль как capability bundle / reviewed skill?
2. **Breadth of value** — насколько модуль полезен для широкого числа Java enterprise repos.
3. **Dependency weight** — сколько внешней инфраструктуры и межмодульных зависимостей он требует.
4. **Safety for early adoption** — насколько безопасно добавлять его без резкого роста ложных ожиданий.
5. **Documentation-to-capability tractability** — реально ли превратить README + step surface в строгий reviewed contract, как это уже сделано для core/api.

## Fit assessment by module

| Module | Fit to current pack | Why |
|---|---|---|
| `akita-gpb-core-module` | **already adopted** | foundational reviewed capability bundle already present |
| `akita-gpb-api-module` | **already adopted** | broad API value, good fit for contract-based bundling |
| `ccl-helpers-module` | **indirect prerequisite** | not user-facing; better as internal dependency concept than active bundle |
| `ccl-files-module` | **strong fit** | broad, reviewable, highly practical for enterprise test artifacts |
| `ccl-keycloak-module` | **strong selective fit** | small focused auth capability; easy to express as bundle |
| `ccl-database-module` | **good fit** | common enterprise need; BDD-friendly step surface |
| `ccl-additional-steps-module` | **good later fit** | useful convenience layer after core/api/database/files |
| `akita-gpb-kafka-mq-module` | **conditional fit** | high value, but infra-heavy and broad in scope |
| `ccl-camunda-module` | **conditional fit** | very valuable in Camunda shops, but niche and config-heavy |
| `ccl-email-module` | **conditional fit** | useful in some repos; requires reachable mail infra |
| `ccl-pre-post-condition-module` | **later/internal fit** | mostly suite orchestration; less value as early user-facing bundle |
| `akita-gpb-db-module` | **weak-to-medium fit** | overlaps with CCL DB; lower priority unless pursuing generic/non-CCL DB story |
| `akita-gpb-helpers-module` | **medium niche fit** | infra helpers/SSH/Testcontainers are useful but not broad phase-1 capability |
| `akita-gpb-hooks-module` | **weak early fit** | hook-heavy and depends on modules not yet packaged, including UI |

## Key observations about the project’s needs

### 1. The pack wants reviewed, bounded capability bundles
Existing skills are intentionally narrow and source-pinned:
- only reviewed step annotations count;
- unsupported cases are explicitly tracked;
- manifest activates exact bundles.

That means modules with:
- clear step boundaries,
- low hidden behavior,
- low hook complexity,
- strong standalone utility

fit best.

### 2. Broad generic value matters more than ecosystem completeness
Since this pack is a bootstrap product, the next modules should help many target repos quickly.
That favors:
- files/documents/assertions,
- DB queries/assertions,
- auth token acquisition,
- generic utility steps.

It deprioritizes:
- Camunda unless the target audience is known to be BPM-heavy,
- email unless notification testing is central,
- hooks/internal orchestration modules.

### 3. Hidden infra burden is the main risk
Several modules assume target repos can provide:
- Nexus-published jars,
- `.conf` configuration conventions,
- specific infra endpoints (Keycloak, Camunda, DB, mail, Kafka/MQ),
- network access from CI/execution nodes.

For `oh-my-akitagpb`, that creates a product risk:
- if capability bundles are activated too early, the generated pack may suggest flows the downstream repo cannot actually run.

### 4. Hooks are harder to package safely than explicit steps
Modules centered around hooks (`akita-gpb-hooks-module`, `ccl-pre-post-condition-module`) are harder to expose as clear user-facing capability bundles because:
- value is contextual rather than explicit;
- contract surface is less intuitive;
- side effects are larger;
- unsupported-case space is wider.

## Recommended categorization for this project

### Must-have for base expansion
These are the best candidates **after current core + api baseline**.

#### `ccl-files-module`
Why:
- broad real-world utility;
- many explicit steps;
- easy mapping into reviewed contracts/examples;
- supports document-heavy enterprise scenarios.

Expected effect on product:
- makes `/akita-plan` and `/akita-write` capable of proposing richer file/document assertions and fixtures.

#### `ccl-database-module`
Why:
- common need in integration testing;
- expressive, business-friendly SQL step surface;
- clearer than lower-level `akita-gpb-db-module` for most users.

Expected effect:
- enables database verification scenarios in generated artifacts and capability-aware planning.

#### `ccl-keycloak-module`
Why:
- very small and focused module;
- high leverage in secured enterprise APIs;
- natural extension of current API-first capability set.

Expected effect:
- unlocks authenticated API scenarios with explicit token acquisition patterns.

### Useful next
#### `ccl-additional-steps-module`
- excellent utility layer once scenarios become richer;
- not strictly necessary for first meaningful expansion.

#### `akita-gpb-kafka-mq-module`
- powerful for event-driven systems;
- should arrive after base synchronous/API/data capabilities stabilize.

#### `ccl-pre-post-condition-module`
- useful once scenario reuse/parameterized suites become a real pain point.

### Optional / niche
#### `ccl-email-module`
- valuable for notification workflows;
- narrower applicability.

#### `akita-gpb-helpers-module`
- SSH/Testcontainers/Docker helper domain is useful but niche as capability surface.

#### `akita-gpb-db-module`
- technically useful, but overshadowed by `ccl-database-module` for the pack’s likely audience.

### Not now
#### `akita-gpb-hooks-module`
- too much hidden lifecycle behavior and dependency sprawl for current maturity.

#### `ccl-camunda-module`
- not because it lacks value, but because it is too domain-specific and infrastructure-coupled for early default adoption.
- could move up only if product strategy explicitly targets Camunda-heavy repos.

## Tension points / overlaps

### DB overlap
- `akita-gpb-db-module` vs `ccl-database-module`
- Recommendation: prefer **one primary DB story** first, otherwise the pack will expose duplicate or confusing paths.
- Winner for first adoption: `ccl-database-module`.

### Generic utility overlap
- `akita-gpb-core-module` common steps already cover some generic variable/wait/assert usage.
- `ccl-additional-steps-module` extends this heavily.
- Recommendation: add only after documenting boundaries clearly to avoid “two ways to do the same thing” confusion.

### Auth coupling
- `ccl-camunda-module` depends materially on `ccl-keycloak-module` for practical secure flows.
- So Keycloak is a better earlier move than Camunda.

## Product-level recommendation from project fit
If the goal is to expand `oh-my-akitagpb` as a practical bootstrap pack rather than a complete mirror of all Akita modules, the next safest track is:
1. keep `core + api` as the stable base;
2. add **files**, **database**, **keycloak**;
3. then layer **additional-steps** and possibly **kafka-mq**;
4. leave **camunda/hooks/email/pre-post** for later or targeted profiles.

## Confidence / unknowns
### High confidence
- current pack scope is intentionally limited to reviewed/pinned bundles;
- core/api are the only active bundles today;
- files/db/keycloak fit the current packaging model well.

### Medium confidence
- whether target users will value `files` more than `database` depends on the dominant repo patterns.

### Unknowns
- whether the roadmap of `oh-my-akitagpb` aims to stay generic or become a full enterprise Akita meta-pack;
- whether there is demand for profile-based activation (e.g. `banking-core`, `camunda-stack`, `event-driven-stack`);
- whether downstream repos already standardize `.conf` conventions needed by CCL modules.
