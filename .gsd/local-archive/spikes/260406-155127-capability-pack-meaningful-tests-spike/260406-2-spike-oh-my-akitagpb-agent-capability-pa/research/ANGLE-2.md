# Angle 2 — Candidate module capability value for meaningful tests

## Question
Which candidate modules unlock meaningful system-behavior tests, and how should they be represented in the pack without collapsing back into README-driven capability sprawl?

## Product framing
REST-only generated tests are weak because they mostly verify:
- routing;
- serialization;
- status codes;
- local response shape.

Meaningful component/integration tests need post-request evidence from the system under test:
- database state changed correctly;
- a broker message was emitted or not emitted;
- a file was generated, downloaded, transformed, or contained expected business content;
- an async process eventually converged to the expected result;
- supporting variable, byte, and polling utilities exist so the agent can wire the flow together.

Below, each module is evaluated against that framing.

---

## 1. `ccl-database-module`

### What meaningful tests it unlocks
- REST + DB verification: request causes persisted rows / updates / deletes.
- Component tests that assert real side effects in the database, not just HTTP output.
- Async flow verification via polling DB until records appear.
- Cross-step extraction: save DB column values into variables for later file/broker/assertion steps.

### Why it helps escape REST-only tests
This module lets the agent ask: “What changed in persistence after the API call?”
That is exactly the missing layer in shallow REST tests.

### Reviewed step categories visible in source
From `CCLDataBaseSteps.java` the reviewed step families are:
- execute query with scenario-variable parameters;
- execute query with inline table parameters;
- execute query and save result set;
- assert empty query result;
- wait/poll until non-empty query result appears;
- assert DB rows by fields (single row);
- assert DB rows with order;
- assert DB rows without order;
- save single column as text/object;
- save column values as formatted list;
- save whole result as JSON string;
- execute update/delete/insert;
- assert affected row count > 0.

### How it should be represented in `oh-my-akitagpb`
Yes — as a separate capability bundle.

Recommended bundle framing:
- focus on query/assert/extract/update/poll behavior;
- explicitly present it as the default persistence-verification surface after API/core;
- treat it as the main DB story unless hard evidence later shows a critical gap.

### Unsupported / stop boundaries to record
- No promise of schema discovery, migrations, or DB admin operations.
- No promise that arbitrary helper methods or config internals are available.
- No promise of transaction management semantics beyond the annotated steps.
- Polling support is limited to the reviewed “wait until non-empty result” pattern, not arbitrary DB-await DSL.
- The pack should not imply DB vendor abstraction guarantees beyond what the reviewed steps already do.

### Dependencies / constraints to teach the agent
- A DB alias / configured query source is assumed by the steps.
- Queries may rely on prepared templates plus scenario variables.
- Result structures are list-of-map shaped; assertions/extractions depend on that.
- The agent must plan meaningful assertions around business rows/columns, not raw “query returned something”.

### Typical feature/scenario patterns to show
- API call -> DB query with request identifier -> assert created row fields.
- API call -> DB query ordered/unordered row set -> assert fan-out records.
- Async request -> poll DB until row appears -> assert terminal state.
- DB seed/update cleanup steps used as fixture preparation.

### Product value
Very high. This is the fastest path from HTTP-wrapper tests to behavior tests.

---

## 2. `akita-gpb-db-module`

### What meaningful tests it unlocks
- Direct DB connection creation from step parameters.
- Query execution and result extraction.
- Simple restrictions/assertions on DB results.

### Why it helps less than `ccl-database-module`
It can verify persistence, but the source-backed surface is narrower and lower-level:
- explicit connection creation is exposed to the feature level;
- row assertions are simpler;
- there is no reviewed ordered/unordered row comparison surface like the CCL module;
- no reviewed built-in “wait until non-empty result” step is visible.

This makes it less aligned with agent-authored meaningful tests. The agent would need to do more low-level orchestration itself.

### How it should be represented in the pack
Do **not** make it the primary DB bundle if `ccl-database-module` is available.
At most:
- keep it out of the first wave; or
- review only if a specific gap appears that CCL DB cannot cover.

### Unsupported / stop boundaries to record if ever used
- No promise that connection creation should be preferred by agents in normal scenarios.
- No claim of richer row-order assertions or built-in DB polling.
- No claim that it supersedes the CCL DB surface.

### Product value
Medium. Useful, but weaker fit for the current product goal.

---

## 3. `akita-gpb-kafka-mq-module`

### What meaningful tests it unlocks
- REST + broker verification: request results in emitted Kafka/MQ/Artemis message.
- Message-driven verification for event-driven systems.
- Verification of absence of message, not just presence.
- Extraction of message body/header into variables for downstream checks.
- Correlation by key/header/jsonPath.
- End-to-end flow patterns where side effects leave the HTTP boundary and continue through messaging.

### Why it helps escape REST-only tests
This is the main path from “API returned 202” to “the system actually emitted the business event / command / notification it was supposed to”.
For event-driven systems this is often more valuable than response-body assertions.

### Reviewed step categories visible in source
At least in `KafkaSteps.java` there is a large reviewed surface for Kafka:
- generate UUID helper used for correlation;
- send Kafka message from table parameters;
- assert a received message matches field table;
- save header/body from latest message;
- get message by typed matcher table (JSON/XML/TEXT);
- assert absence of matching message;
- save body/header by key/header/jsonPath;
- find message by combined header + body predicates.

README shows the broader module also includes:
- IBM MQ steps;
- Artemis queue/topic steps;
- message header/body assertions for multiple transports.

### How it should be represented in `oh-my-akitagpb`
Yes — but not as one undifferentiated giant bundle in the first implementation.

Recommended representation:
- first-wave reviewed bundle should be **Kafka-focused** if Kafka is the highest-value message-verification surface;
- MQ/Artemis surfaces should be separate later bundles or explicitly deferred reviewed categories;
- examples must teach correlation and event verification patterns, not generic “send a message because we can”.

### Unsupported / stop boundaries to record
- No generic claim over all README-listed transports unless each reviewed step family is bundled.
- No promise that hooks/config classes create standalone capabilities.
- No promise of arbitrary long-running stream-processing or consumer-group coordination semantics.
- No promise that all matchers/validators/helpers visible in internals are independently step-addressable.
- If only Kafka is bundled first, MQ and Artemis must be explicit unsupported/deferred surfaces.

### Dependencies / constraints to teach the agent
- Correlation strategy matters: key, header, UUID, or payload field must usually be prepared before the trigger step.
- Polling windows and “since test start” semantics matter.
- Message assertions should be grounded in business fields, not just topic presence.
- The agent must avoid broker tests when the flow does not actually use messaging.

### Typical feature/scenario patterns to show
- API call -> save correlation id -> poll Kafka topic for matching event -> assert payload fields.
- API call -> assert no message emitted for invalid request.
- Message consumption flow -> save message body/header -> then verify DB/file side effects.
- Event-driven workflow -> wait for message then assert terminal persistence state.

### Product value
High, but second after DB for most “make generated tests meaningful” use cases. It is also riskier to bundle because the module is much broader.

---

## 4. `ccl-files-module`

### What meaningful tests it unlocks
- REST + files verification: request causes file generation/download.
- Verification of generated business artifacts: PDF, DOCX, Excel, JSON, XML, HTML, images.
- Archive unpacking and artifact-list checks.
- Conversion from byte arrays / file bodies to actual file objects for downstream assertions.

### Why it helps escape REST-only tests
It lets the agent prove the system generated the actual outward artifact the user cares about, not just returned HTTP 200.
This is especially strong for reporting/export/document flows.

### Reviewed step categories visible in source and README
From `FileSteps.java` and `PDFSteps.java` plus README, the practical families are:
- file content load/save;
- download detection and file capture;
- byte[] -> file conversion;
- random file generation (fixture utility);
- zip unpack / file-list assertions / pick file by name;
- PDF text and table assertions;
- plus broader file-format steps in README: DOCX, Excel, JSON, XML, HTML, image comparison.

### How it should be represented in `oh-my-akitagpb`
Yes — as a separate capability bundle, but likely **narrower than the full module README** on first wave.

Recommended first reviewed slice:
- generic file handling;
- download detection and byte[]/file conversions;
- PDF verification.

Reason: these are the strongest direct complements to API/core and the most obvious product wins for meaningful tests.

DOCX/Excel/HTML/image/XML/JSON file surfaces can be second-wave expansions unless there is strong demand.

### Unsupported / stop boundaries to record
- No promise of all file formats unless each reviewed step class is included.
- No generic “any file content assertion” claim from a PDF-only step family.
- Download detection depends on configured download directory semantics.
- The agent should not assume browser/UI download steps exist in this pack; only the file-side verification surface is in scope.

### Dependencies / constraints to teach the agent
- File assertions usually need either a `File` variable or a known path.
- Some file flows depend on API byte-body extraction or multipart utilities from the additional module.
- PDF table checks are layout-sensitive and should be used when the business value really lives in the rendered artifact.

### Typical feature/scenario patterns to show
- API call -> save response bytes -> convert to file -> assert PDF content.
- UI/API action -> wait for downloaded file -> assert its name/content.
- API call -> download zip -> unpack -> assert included files.
- API call -> generated report -> assert business rows in PDF table.

### Product value
High. Especially valuable right after DB for systems that generate artifacts or downloadables.

---

## 5. `ccl-additional-steps-module`

### What meaningful tests it unlocks
Not primary business verification by itself, but it unlocks **test composition quality**:
- helper data generation for correlation and fixtures;
- regex/date/string assertions and transformations;
- response body to bytes;
- base64 decode to bytes;
- variable-duration wait step;
- polling request until JSONPath reaches expected value;
- multipart body construction for file-upload API scenarios.

### Why it helps escape REST-only tests
It does not directly prove side effects. Instead it removes the friction that otherwise causes the agent to stop at shallow HTTP checks.
Examples:
- polling until async state changes;
- building multipart payloads for upload-driven flows;
- converting response bytes into a file for real artifact assertions.

### How it should be represented in `oh-my-akitagpb`
Probably yes, but **not as a kitchen-sink bundle**.

Recommended representation is a support-oriented bundle focused on meaningful-test enablers:
- polling / wait / retry request until condition;
- bytes/base64/file-body utilities;
- multipart body builder;
- string/regex/date helpers only if they materially support common scenario composition.

Avoid presenting number-to-words and formatting helpers as if they are core meaningful-test capability.

### Unsupported / stop boundaries to record
- No claim that the whole README is important to the planner.
- Avoid exposing low-value formatting helpers as central planning tools.
- Polling support is bounded to reviewed request/jsonPath or explicit wait steps, not generic orchestration semantics.
- If only support-relevant subfamilies are bundled, array/date/number helpers outside that slice must be marked deferred or unsupported.

### Dependencies / constraints to teach the agent
- This bundle is usually used together with API/files/broker bundles, not as a standalone test surface.
- Polling should be used when the domain is eventually consistent, not as a blanket replacement for deterministic checks.
- Multipart helper matters when the API module alone is awkward for file uploads with UTF-8 filenames.

### Typical feature/scenario patterns to show
- API call -> poll GET until status field becomes COMPLETED.
- Download/file API -> save response bytes -> convert to file -> assert artifact.
- Upload API -> build multipart body -> send request -> verify DB/file side effects.

### Product value
Medium-high as an accelerator. Lower than DB and broker/file verification, but important as the fourth support wave.

## Cross-module linkage value

### REST + DB
Highest-value general pattern. Converts request-wrapper tests into persistence-verification tests.

### REST + broker
Critical for event-driven or async command-processing systems. Usually the second strongest upgrade after DB.

### REST + files
Critical for export/document/reporting domains. Strong third pillar of meaningful behavior verification.

### async flow + polling/utilities
Support layer, not the business proof itself. Needed so meaningful checks can stabilize against eventual consistency.

### message-driven / event-driven verification
Needs explicit transport-aware bundles and correlation patterns. Valuable, but more complex and therefore should be bundled conservatively.

## Confidence
High on DB/files/additional. Medium-high on Kafka/MQ because the module is broad and the first source read focused on Kafka step surface, though that is already enough for first-wave recommendation.