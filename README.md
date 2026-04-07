# oh-my-akitagpb

Native-first OpenCode bootstrap pack for Akita GPB workflows.

`oh-my-akitagpb` installs a managed `.oma/` and `.opencode/` surface into a target
repository so an OpenCode agent can scan the repo as a system under test, plan
high-value meaningful Akita flows from persisted evidence, write Akita-grounded
artifacts, and validate them against shipped capability bundles.

The product worldview is system-behavior-first:

- scan discovers trigger surfaces, contract evidence, prior art, runtime shape,
  candidate flows, and assertion opportunities from repo evidence
- OpenAPI and AsyncAPI are first-class contract evidence sources when present,
  but not the center of all reasoning
- code-first contracts, DTOs, event schemas, tests, and feature files are normal
  evidence sources, not fallback accidents
- capability truth comes from manifest-listed bundles and their reviewed
  references, not from README prose

## What it ships

- CLI lifecycle commands: `install`, `update`, `doctor`
- OpenCode commands: `/akita-scan`, `/akita-plan`, `/akita-write`, `/akita-validate`, `/akita-promote`
- Curated capability bundles currently pinned in the manifest for:
  - `akita-gpb-core-module@c795936046e`
  - `akita-gpb-api-module@223b2561bbc`
  - `ccl-database-module@bb0d27eda3e`
  - `ccl-files-module@05e1cf4d5e7`
  - `akita-gpb-kafka-mq-module@ff56f175d8c`
  - `ccl-additional-steps-module@95eda279aa4`
- Pack-owned templates, rules, manifests, and runtime state scaffolding

The pack does not ship a fixed catalog of scenarios. `/akita-scan` and
`/akita-plan` are meant to derive candidate flows from repo evidence plus the
active capability bundle set.

## Requirements

- Node.js `>=20`
- An OpenCode-compatible target repository

## Install

```bash
npm install -D oh-my-akitagpb
npx oh-my-akitagpb install
```

After install, open the target repo in OpenCode and start with:

```text
/akita-scan
```

## CLI commands

### `install`

Bootstrap-only install. Materializes the managed pack surface into the current
repository and records ownership in `.oma/install-state.json`.

### `update`

Explicit refresh path. Rewrites only pack-owned artifacts recorded in
`install-state`.

### `doctor`

Diagnose-first command. Writes `.oma/state/local/doctor/doctor-report.json` and
returns one safe next step.

## Runtime surface

The package materializes these top-level surfaces in a target repository:

- `.oma/` as the source of truth for state, templates, manifests, and runtime metadata
- `.opencode/commands/akita-*.md`
- `.opencode/skills/akita-*-workflow/**`
- `.opencode/skills/akita-capability-*/**`

Ownership is strict. The pack does not silently overwrite user-owned `AGENTS.md`,
`opencode.json`, or unrelated `.opencode/*`.

## Development

```bash
npm ci
npm test
npm run smoke:pack-install
```

## Publishing

This repository includes:

- CI workflow: `.github/workflows/ci.yml`
- npm publish workflow: `.github/workflows/publish.yml`

Trusted publishing setup on npmjs.com must point to:

- Repository: `zuevrs/oh-my-akitagpb`
- Workflow filename: `publish.yml`

Release flow:

1. Update `package.json` version.
2. Merge to `main`.
3. Tag the release as `vX.Y.Z`.
4. Push the tag.
5. GitHub Actions publishes the package through npm trusted publishing.

## License

MIT
