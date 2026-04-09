---
description: Read persisted Pact scan/plan/write state and verify whether the recorded provider-verification outcome is actually ready, partial, blocked, irrelevant, or inconsistent.
agent: build
subtask: false
---

Use the `pact-validate-workflow` skill from `.opencode/skills/pact-validate-workflow/SKILL.md`.

Before you start:
1. Read `.oma/packs/oh-my-pactgpb/templates/validate/state-contract.json` and treat it as the canonical validate persistence contract.
2. Read `.oma/packs/oh-my-pactgpb/templates/write/state-contract.json`.
3. Read `.oma/packs/oh-my-pactgpb/templates/plan/state-contract.json`.
4. Read `.oma/packs/oh-my-pactgpb/templates/scan/state-contract.json`.
5. Read `.oma/packs/oh-my-pactgpb/state/shared/write/write-state.json` from disk as the canonical validation input.
6. Read `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-state.json` from disk as supporting evidence.
7. Read `.oma/packs/oh-my-pactgpb/state/shared/scan/scan-state.json` from disk as supporting evidence.
8. Read `.oma/packs/oh-my-pactgpb/capability-manifest.json`.
9. Read `.oma/packs/oh-my-pactgpb/runtime/shared/data-handling-policy.json`.
10. Read the installed rule files under `.oma/packs/oh-my-pactgpb/instructions/rules/`.
11. Persist only the contract-defined shared validate outputs instead of relying on chat memory.

Required shared validate outputs:
- `.oma/packs/oh-my-pactgpb/state/shared/validate/validate-state.json`
- optional derived summary: `.oma/packs/oh-my-pactgpb/state/shared/validate/validate-summary.md`

Then:
- treat persisted `scan-state.json`, `plan-state.json`, and `write-state.json` as the canonical validation input chain
- do not rescan the repo and do not re-plan implicitly inside validate
- do not silently fix repo files during validate; default to read/verify/report only
- verify that the recorded write outcome matches the prior plan verdict honestly
- verify that files claimed in write-state still exist and still contain the expected Pact scaffold substance
- distinguish structural/compile proof from runnable Pact verification proof
- if runnable verification cannot be proven honestly, say why instead of inferring success from file presence
- if plan/write were `irrelevant`, confirm the honest no-op instead of failing for missing scaffold
- if write stayed `partial` or `blocked`, preserve those unresolved blockers instead of greenwashing them
- if repo state drifted from the recorded write result, report inconsistency explicitly
- keep shared JSON and markdown redaction-first per `.oma/packs/oh-my-pactgpb/runtime/shared/data-handling-policy.json`; never persist secrets, credentials, tokens, raw auth headers, or machine-local values

If validate succeeds, say the validate state is persisted and summarize the validation outcome, strongest evidence, remaining blockers, and whether runnable verification is proven, ready, blocked, or still unproven.
