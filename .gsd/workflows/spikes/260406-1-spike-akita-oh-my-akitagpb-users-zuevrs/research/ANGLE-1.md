# Research Angle 1 — Module inventory and static analysis

## Goal
Разобрать все архивы из `docs/akita`, извлечь из них назначение модулей, шаги/хуки, зависимости и признаки интеграции.

## Evidence base
Исследованы:
- `README.md`
- `gpb-manifest.json`
- `build.gradle`
- структура `src/main/**`
- step-классы и hook-классы

## Inventory summary

| Module | Domain | Step surface | Key dependencies | Initial assessment |
|---|---|---:|---|---|
| `akita-gpb-core-module` | core BDD runtime | core/common steps + scenario/runtime API | Cucumber, RestAssured, Typesafe Config, Allure, ReportPortal | foundational |
| `akita-gpb-api-module` | REST API testing | API request/assertion steps | `akita-gpb`, RestAssured, schema validation | foundational for API-first flows |
| `akita-gpb-db-module` | generic DB access | DB query/update steps | `akita-gpb-core-module`, Spring JDBC, HikariCP | useful, but overlaps with CCL DB module |
| `akita-gpb-helpers-module` | infra helpers | SSH steps + helper classes | core, SSHJ, Testcontainers, BrowserUp | niche infra-enabler |
| `akita-gpb-hooks-module` | hooks/reporting/docker lifecycle | mostly hooks, little direct step value | core, api, ui, helpers, kafka/mq | heavy integration surface |
| `akita-gpb-kafka-mq-module` | Kafka / MQ / Artemis | rich messaging steps | core, helpers, Kafka, IBM MQ, Artemis JMS | strong value if async/event flows exist |
| `ccl-additional-steps-module` | utility BDD steps | many generic data/loop steps | ccl-helpers, api-module | high convenience, not foundational |
| `ccl-camunda-module` | Camunda BPMN/DMN/process runtime | rich Camunda control/validation steps | ccl-helpers, ccl-keycloak, api-module | high-value only for Camunda-based systems |
| `ccl-database-module` | DB access via named SQL | rich DB steps | ccl-helpers, Spring JDBC, Oracle driver | likely more practical than generic db module |
| `ccl-email-module` | email receive/inspect | inbox verification steps | ccl-helpers, EWS, mail libs | niche but useful for notification tests |
| `ccl-files-module` | files/json/xml/pdf/docx/xlsx/html | very rich file/assertion steps | ccl-helpers, screen-diff, ashot, pdf libs | broadly useful for enterprise test evidence |
| `ccl-helpers-module` | shared helper substrate | no steps | core, Vault, POI, docx, pdf, json-unit | foundational for many CCL modules |
| `ccl-keycloak-module` | auth token acquisition | 1 focused token step + hook | ccl-helpers | focused and practical |
| `ccl-pre-post-condition-module` | custom pre/post condition hooks | hooks only | ccl-helpers | workflow optimization, not day-1 |

## Module-by-module notes

### 1. `akita-gpb-core-module`
- Purpose: ядро Akita-BDD; управление runtime, scenario vars, reflection/scanning, common steps.
- Role: обязательная основа для большинства модулей.
- Signals in sources:
  - runtime APIs: `AkitaEnvironment`, `AkitaScenario`, `ScopedVariables`
  - common step layer: `modules/core/steps/CommonSteps.java`
- External stack:
  - Cucumber, RestAssured, Config, Allure, ReportPortal, Awaitility.
- Assessment:
  - must-have as conceptual/runtime base.
  - High coupling with Java/Cucumber ecosystem.

### 2. `akita-gpb-api-module`
- Purpose: REST API interaction and response validation.
- Role in platform: базовый API-testing capability.
- Evidence:
  - `modules/api/steps/ApiSteps.java`
  - README positions it as one of primary pack bundles already curated in this project.
- Assessment:
  - strongest fit with current `oh-my-akitagpb` positioning because pack already ships commands/capabilities for core + api.

### 3. `akita-gpb-db-module`
- Purpose: DB integration via SQL, Spring JDBC, pool management.
- Role: low-level DB verification.
- Notable dependencies:
  - Spring JDBC, HikariCP, PostgreSQL, Spring Data JDBC.
- Assessment:
  - useful, but likely secondary to `ccl-database-module`, which exposes more opinionated business-friendly BDD steps.

### 4. `akita-gpb-helpers-module`
- Purpose: infra helpers beyond core — DockerHelper, SSH helpers, `SshSteps`, Testcontainers presence.
- Role: infra/bootstrap enabler for environment control.
- Assessment:
  - useful when platform expands into environment orchestration.
  - not first adoption target for a safe minimal contour.

### 5. `akita-gpb-hooks-module`
- Purpose: reporting and lifecycle hooks (Allure, Docker compose, masking, integration hooks).
- Dependencies show it is heavy and assumes:
  - core + api + ui + helpers + kafka/mq
- Assessment:
  - integration-heavy and potentially brittle.
  - not a good early module without broader Java test runtime adoption.

### 6. `akita-gpb-kafka-mq-module`
- Purpose: Kafka, IBM MQ, Artemis messaging steps.
- Typical scenarios:
  - publish events;
  - wait for message in topic/queue;
  - inspect headers/body;
  - assert JSON/XML payload contents;
  - manage Artemis subscriptions.
- Assessment:
  - high value for async/event-driven systems.
  - infrastructure-heavy; best after API/core baseline is stable.

### 7. `ccl-additional-steps-module`
- Purpose: generic utility steps for strings, arrays, dates, expressions, polling loops.
- Typical scenarios:
  - save values into scenario vars;
  - transform strings via regex;
  - evaluate expressions;
  - polling GET/POST until JsonPath condition matches.
- Assessment:
  - convenience amplifier rather than foundational capability.
  - likely useful once real scenarios are being authored at scale.

### 8. `ccl-camunda-module`
- Purpose: Camunda process lifecycle testing via REST + BPMN mutation helpers + cleanup hooks.
- Typical scenarios:
  - wait for process token at activity;
  - inspect variables/history/incidents;
  - deploy BPMN/DMN;
  - complete tasks;
  - mutate BPMN for autotest markers.
- Dependencies / constraints:
  - requires `ccl-keycloak-module` and `akita-gpb-api-module`;
  - needs Camunda base config and RestAssured filter wiring.
- Assessment:
  - very valuable only if target systems use Camunda actively.
  - not suitable as general default capability.

### 9. `ccl-database-module`
- Purpose: named SQL query execution with BDD-friendly result assertions.
- Typical scenarios:
  - execute query by logical name from `resources/queries/*.sql`;
  - assert row sets with/without ordering;
  - persist selected columns into vars;
  - run DML and validate affected rows.
- Assessment:
  - likely better day-to-day DX than generic `akita-gpb-db-module`.
  - strong fit for enterprise integration tests.

### 10. `ccl-email-module`
- Purpose: inspect mailbox, assert received/non-received messages, body/attachments/links.
- Infra assumptions:
  - EWS / Outlook access;
  - SMTP/EWS connectivity from execution environment.
- Assessment:
  - useful for notification-driven systems; otherwise niche.

### 11. `ccl-files-module`
- Purpose: broad file/data-format verification surface.
- Typical scenarios:
  - assert PDF contents/tables;
  - compare JSON/XML;
  - inspect HTML by XPath;
  - read Excel/Word;
  - unpack archives;
  - compare PNG/screenshots.
- Assessment:
  - one of the broadest practical modules after core/api.
  - especially relevant where tests validate generated documents/reports.

### 12. `ccl-helpers-module`
- Purpose: shared helper substrate for CCL modules.
- No direct steps; provides utilities, asserts, file/date/json helpers, Vault integration.
- Assessment:
  - foundational dependency for most useful CCL modules.
  - should be treated as a platform prerequisite, not a user-facing capability.

### 13. `ccl-keycloak-module`
- Purpose: acquire and cache Keycloak bearer tokens.
- Step surface:
  - one clear step for token acquisition into scenario variable.
- Assessment:
  - focused, small, high leverage where auth is Keycloak-based.
  - good early add-on after API baseline if project ecosystem uses Keycloak.

### 14. `ccl-pre-post-condition-module`
- Purpose: optimized pre/post-condition orchestration for Cucumber scenarios.
- Surface:
  - hooks only, no user-facing steps.
- Assessment:
  - useful later for scenario suite optimization and reuse.
  - not day-1 for bootstrap pack.

## Structural patterns found
- Almost all modules are Java 17 Gradle libraries.
- They rely on internal Maven/Nexus publication rather than source-level multi-module linking.
- Most modules assume `typesafe-config` driven `.conf` project configuration.
- Many modules expose Cucumber regex steps in Russian.
- Allure/ReportPortal/log4j dependencies are pervasive.
- CCL modules commonly depend on `ccl-helpers-module`; akita modules commonly depend on core.

## Confidence / unknowns
### High confidence
- module purpose
- direct dependencies from Gradle
- step/hook presence
- likely integration domain

### Medium confidence
- exact runtime behavior of hooks without executing Java tests
- extent of overlap between generic Akita DB vs CCL DB in real project usage

### Unknowns
- which of these modules are already supported by the current pack templates beyond core/api
- whether target repositories using this pack commonly have Camunda/Kafka/Keycloak/Oracle/EWS available
- internal publication/versioning process required to consume these jars in downstream repos
