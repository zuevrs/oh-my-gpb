---
description: Read persisted Pact scan state and persist an honest provider-verification plan.
agent: build
subtask: false
---

Use the `pact-plan-workflow` skill from `.opencode/skills/pact-plan-workflow/SKILL.md`.

Before you start:
1. Read `.oma/packs/oh-my-pactgpb/templates/plan/state-contract.json` and treat it as the canonical plan persistence contract.
2. Read `.oma/packs/oh-my-pactgpb/templates/scan/state-contract.json`.
3. Read `.oma/packs/oh-my-pactgpb/state/shared/scan/scan-state.json` from disk as the canonical planning input.
4. Read `.oma/packs/oh-my-pactgpb/capability-manifest.json`.
5. Read `.oma/packs/oh-my-pactgpb/runtime/shared/data-handling-policy.json`.
6. Read the installed rule files under `.oma/packs/oh-my-pactgpb/instructions/rules/`.
7. Persist only the contract-defined shared plan outputs instead of relying on chat memory.

Required shared plan outputs:
- `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-state.json`
- optional derived summary: `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-summary.md`

Then:
- plan only for Pact **provider verification** within the current MVP scope: Java, Spring Boot providers, HTTP Pact provider verification, provider-first, broker-optional
- use the persisted scan-state on disk as the canonical evidence source; do not rescan the repo and do not plan "from memory"
- select the provider under contract only when persisted scan evidence supports it; if the selection is ambiguous, persist the ambiguity as a blocker instead of guessing
- classify pact artifact source strategy explicitly as `local`, `broker`, or `unclear`
- determine verification readiness honestly: existing setup to extend, new scaffold to create, or insufficient evidence to proceed safely
- turn provider-state findings into concrete planning tasks: missing `@State(...)`, weak fixtures/bootstrap, auth/setup uncertainty, likely coverage gaps, stale verification wiring
- use only evidence-supported next steps; do not pretend broker automation or runnable verification already exists when the persisted scan-state does not prove it
- if Pact provider verification is irrelevant here, persist that verdict explicitly instead of inventing a plan
- keep shared JSON and markdown redaction-first per `.oma/packs/oh-my-pactgpb/runtime/shared/data-handling-policy.json`; never persist secrets, credentials, tokens, raw auth headers, or machine-local values

If planning succeeds, say the plan state is persisted and call out the verdict, strongest evidence, and main blockers plainly.
