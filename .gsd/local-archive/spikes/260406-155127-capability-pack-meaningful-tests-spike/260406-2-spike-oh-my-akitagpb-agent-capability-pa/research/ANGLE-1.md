# Angle 1 — Current pack integration pattern

## Question
How does a reviewed Akita module currently become a runtime capability bundle in `oh-my-akitagpb`?

## Findings

### 1. A capability bundle is exact-pin and runtime-curated
Current bundles are not generic module names. They are exact reviewed source pins:
- `akita-capability-akita-gpb-core-module-c795936046e`
- `akita-capability-akita-gpb-api-module-223b2561bbc`

That bundle ID is repeated consistently across:
- `assets/oma/capability-manifest.json`
- `assets/opencode/skills/<bundleId>/SKILL.md`
- `references/capability-contract.json`
- `references/unsupported-cases.json`
- `references/examples.json`
- `references/provenance.json`

This is not just packaging. It is the review boundary.

### 2. Manifest wiring is strict and local-runtime oriented
`assets/oma/capability-manifest.json` activates a bundle by listing:
- `bundleId`
- reviewed `module.id` and `pin`
- runtime `skillPath`
- runtime reference paths for contract / unsupported / examples / provenance

The runtime paths are `.opencode/...` paths even though the source files live under `assets/...`. Tests assert that every manifest path resolves to a shipped local asset and does not escape to arbitrary locations.

### 3. The SKILL file is a runtime guardrail, not a marketing readme
Each bundle `SKILL.md` contains:
- activation rule: use only when the manifest enables this bundle;
- exact reviewed pin and module id;
- evidence base (reviewed source file family);
- the core contract rule: only public Cucumber step annotations count as shipped truth;
- a short supported capability summary;
- required references;
- operating rules that explicitly block inference from helpers, docs, hooks, or cross-module mentions.

This is the main anti-hallucination layer for the agent.

### 4. Reviewed step surface lives in `capability-contract.json`
The contract JSON is the real executable capability map. Each supported item includes:
- stable capability id;
- `supported` or `partial` status;
- summary;
- exact step patterns;
- annotated method name;
- source path;
- notes where semantics are narrower than the step wording suggests.

Important pattern: current contracts are conservative and willingly mark something `partial` if the implementation is narrower than a naive reading would suggest.
Examples:
- core boolean expression support is partial, not a promise of arbitrary expression language;
- API SSE support is partial, because it captures only the first line, not a full stream-processing contract.

### 5. Unsupported boundaries are first-class assets
`unsupported-cases.json` is not optional documentation. It is part of the runtime bundle shape.
It records:
- helper-only internals are unsupported unless a public annotated step exposes them;
- hook behavior is unsupported unless step-addressable;
- generic-sounding steps may still be constrained to a narrower reviewed behavior;
- doc/readme claims do not expand runtime truth.

This is central for future modules whose README surfaces are broad and whose implementation internals are tempting to over-infer from.

### 6. Examples are curated for agent planning, not exhaustive docs
`examples.json` contains a small set of scenario snippets showing:
- a realistic flow shape;
- how variables are passed between steps;
- where to keep semantic limits explicit.

Current examples are intentionally small and composable. They demonstrate safe usage patterns the planner can reuse.

### 7. Provenance is shipped but redacted
`provenance.json` records:
- bundle id;
- module id;
- pin;
- authoring policy such as `runtimeExtraction: false`, `onlyAnnotatedStepMethodsPromoted: true`, and `unclearBehaviorMarkedUnsupported: true`.

Tests enforce redaction and assert that runtime-facing files do not expose `sourceZip`, internal hostnames, or enterprise-specific references.

### 8. Tests define the bundle authoring contract
Two test files enforce the pattern:
- `tests/capabilities/capability-bundle-assets.test.ts`
- `tests/capabilities/capability-manifest.test.ts`

They check:
- required files exist for each bundle;
- module id and pin match the reviewed contract;
- source paths point only to the reviewed step class family;
- unsupported cases are explicit;
- provenance is redacted;
- manifest paths are local, normalized, and aligned with contract/provenance.

This means adding a new module is not just “drop in a skill”. It requires a full reviewed asset set plus tests.

## Exact inferred implementation pattern for a new Akita module
A new module currently needs:
1. a new exact-pin bundle id;
2. `assets/opencode/skills/<bundleId>/SKILL.md`;
3. `references/capability-contract.json`;
4. `references/unsupported-cases.json`;
5. `references/examples.json`;
6. `references/provenance.json`;
7. a manifest entry in `assets/oma/capability-manifest.json`;
8. test updates that validate bundle presence, pin alignment, unsupported boundaries, and manifest wiring.

## Design implications from the current pattern
- Bundle shape is intentionally conservative.
- The unit of review is not “the whole module README”, but the exact annotated step surface judged safe for runtime planning.
- If a module contains several unrelated step families, it is safer to split reviewed capability bundles by practical surface than to dump the full README into one bundle.
- Unsupported cases should be designed upfront, not added as an afterthought.

## Pros of the current pattern
- Strong anti-hallucination posture.
- Exact reviewed pins make behavior auditable.
- Runtime bundle remains small and agent-usable.
- Tests make the integration pattern hard to silently erode.

## Risks / constraints
- Broad modules can become too large if imported as one bundle.
- README breadth can tempt overclaiming unless the source-backed review remains strict.
- Product usefulness depends on what examples and unsupported boundaries teach the agent, not only on raw step count.

## Confidence
High. This is derived directly from repository assets and tests.