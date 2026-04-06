# Scope

## Research question
How should oh-my-akitagpb evolve after the existing core/api capability bundles so an agent can write meaningful Akita component/integration tests that verify real system behavior rather than REST-only wrappers?

## Success criteria
A useful answer must:
- derive the current bundle integration pattern from this repository, not from abstract assumptions;
- evaluate candidate Akita modules by the concrete test behaviors they unlock: database state verification, broker/message verification, file generation/consumption verification, async flow waiting/polling, and supporting utility steps;
- decide the DB story explicitly between `ccl-database-module` and `akita-gpb-db-module`;
- recommend first-wave modules and a practical implementation order;
- translate the recommendation into capability-pack design implications: bundle boundaries, reviewed step categories, unsupported boundaries, examples, dependencies, and test coverage expectations.

## Constraints
- Keep the analysis centered on meaningful tests, not module catalog completeness.
- Prioritize database, brokers, files, and utility/support steps.
- Treat Keycloak, Camunda, hooks, and domain-heavy modules as non-priority for this wave.
- Use the existing core/api bundle pattern in this repo as the baseline shape for any recommendation.

## Research angles
1. **Current pack integration pattern** — how a reviewed Akita module becomes a capability bundle in this repository.
2. **Candidate module capability value** — what meaningful test surfaces the target modules unlock, and where they stop.
3. **Sequencing and DB decision** — which module order gives the biggest product gain first, and which DB path avoids duplication while improving test quality.

## Expected decision format
- Recommended first-wave module set
- Recommended DB path
- Capability design implications per module
- Ordered implementation recommendation with a concrete next implementation task
