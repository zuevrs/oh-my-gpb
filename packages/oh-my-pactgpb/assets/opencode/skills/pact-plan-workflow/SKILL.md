---
name: pact-plan-workflow
description: Turn persisted Pact scan state into an honest provider-verification plan for the installed /pact-plan command.
compatibility: opencode
metadata:
  audience: runtime
  phase: phase2
---

## Use this skill when

Use this skill only for the installed `/pact-plan` flow.

## Required reads

Read these before reasoning:
- `.oma/packs/oh-my-pactgpb/templates/plan/state-contract.json`
- `.oma/packs/oh-my-pactgpb/templates/scan/state-contract.json`
- `.oma/packs/oh-my-pactgpb/state/shared/scan/scan-state.json`
- `.oma/packs/oh-my-pactgpb/capability-manifest.json`
- `.oma/packs/oh-my-pactgpb/runtime/shared/data-handling-policy.json`
- `.oma/packs/oh-my-pactgpb/instructions/rules/*.md`

## Required writes

Persist exactly these shared plan outputs:
- `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-state.json`
- optional derived summary: `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-summary.md`

## Procedure

1. Read `.oma/packs/oh-my-pactgpb/templates/plan/state-contract.json` first and treat it as the canonical persistence contract.
2. Read the persisted scan state from disk, not from chat memory.
3. Keep the worldview narrow: Java, Spring Boot, HTTP provider verification, provider-first, broker-optional.
4. Build a machine-readable planning state in `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-state.json` that answers at least:
   - which provider under contract is selected and why
   - whether provider selection is ambiguous or blocked
   - which pact artifact-source strategy applies: `local`, `broker`, or `unclear`
   - whether existing verification setup should be extended, a new scaffold should be created, or planning is blocked
   - which provider-state gaps require explicit work
   - which ordered next tasks are safe to do now
   - which blockers still prevent honest runnable verification
5. Derive planning only from persisted scan evidence. Do not claim broker flows, runnable verification, or missing repo facts that the scan state does not support.
6. If an existing verification test is present, default to extend/remediate it before recommending a full rewrite.
7. If provider-state support is missing or thin, convert that into concrete planning tasks rather than generic warnings.
8. If the provider under contract is ambiguous, persist the ambiguity in `blockedBy` and stop short of a fake selection.
9. If Pact is irrelevant for this repo, persist `irrelevant` and do not invent a verification plan.
10. Persist the exact shared outputs listed above.

## Stop with `needs-review` when

Stop instead of guessing if:
- the required persisted scan-state file is missing
- the plan contract is missing or malformed
- provider selection remains ambiguous in a way that prevents an honest plan
- the scan-state does not provide enough evidence to classify the next step safely

When you stop, say `needs-review`, name the exact blocker, and record it in `.oma/packs/oh-my-pactgpb/state/shared/plan/plan-state.json`.

## Evidence and redaction

- Keep the plan grounded in persisted scan state.
- Do not use README prose as capability or repo evidence.
- Never persist secrets, credentials, tokens, raw auth headers, or machine-local values in shared plan state.

## Handoff

If planning succeeds, tell the user the plan state is persisted and summarize the verdict, safe next tasks, and blockers.
If planning stops with `needs-review`, do not suggest `/pact-write` yet.
