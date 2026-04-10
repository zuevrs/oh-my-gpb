# oh-my-gpb

Umbrella npm-workspaces repository for GPB-oriented tooling.

## Current status

This repository currently contains two workspace packages:

- `packages/oh-my-akitagpb` — published on npm as `oh-my-akitagpb`
- `packages/oh-my-pactgpb` — prepared for publication as `oh-my-pactgpb`

The root package is orchestration-only:

- it is `private`
- it uses npm workspaces
- it is not publishable
- product-specific code, tests, assets, and release metadata live with each package

## Repository shape

```text
.
├── package.json
├── package-lock.json
├── .github/workflows/
│   ├── ci.yml
│   ├── publish-akita.yml
│   └── publish-pact.yml
└── packages/
    ├── oh-my-akitagpb/
    └── oh-my-pactgpb/
```

## Working with the workspace

Install once at repo root:

```bash
npm ci
```

### Explicit package commands

Akita:

```bash
npm run build --workspace oh-my-akitagpb
npm test --workspace oh-my-akitagpb
npm run smoke:pack-install --workspace oh-my-akitagpb
npm run release:check --workspace oh-my-akitagpb
npm pack --workspace oh-my-akitagpb --json
```

Pact:

```bash
npm run build --workspace oh-my-pactgpb
npm test --workspace oh-my-pactgpb
npm run smoke:pack-install --workspace oh-my-pactgpb
npm run release:check --workspace oh-my-pactgpb
npm pack --workspace oh-my-pactgpb --json
```

### Thin root helpers

The root package exposes explicit wrappers when you want shorter commands:

```bash
npm run build:akita
npm run test:akita
npm run smoke:akita
npm run release:check:akita
npm run pack:akita

npm run build:pact
npm run test:pact
npm run smoke:pact
npm run release:check:pact
npm run pack:pact
```

Generic root scripts now mean **both packages**:

```bash
npm run build
npm test
npm run smoke:pack-install
npm run release:check
```

## Package docs

For package-specific install/runtime behavior, see:

- [`packages/oh-my-akitagpb/README.md`](packages/oh-my-akitagpb/README.md)
- [`packages/oh-my-pactgpb/README.md`](packages/oh-my-pactgpb/README.md)

## Release model

There is no repo-wide "publish all products" workflow.

Each package has its own publish workflow and tag namespace:

| Package | Workspace path | Workflow | Tag format |
|---|---|---|---|
| `oh-my-akitagpb` | `packages/oh-my-akitagpb` | `.github/workflows/publish-akita.yml` | `akita-vX.Y.Z` |
| `oh-my-pactgpb` | `packages/oh-my-pactgpb` | `.github/workflows/publish-pact.yml` | `pact-vX.Y.Z` |

Each workflow:

- triggers only for its own tag prefix
- reads metadata from its own package-local `package.json`
- runs `npm ci` at repo root
- runs package-local `release:check`
- fails if the pushed tag does not exactly match the package version
- publishes only from that package directory
- skips `npm publish` if the exact version already exists on npm
- creates a GitHub Release only for that package tag

## Manual trusted publishing follow-up

Trusted publishing must be configured separately on npm for each package/workflow pair.
Repository code cannot do this for you.

### `oh-my-akitagpb`

- npm package: `oh-my-akitagpb`
- GitHub repository: `zuevrs/oh-my-gpb`
- workflow filename to trust: `publish-akita.yml`
- expected tag prefix: `akita-v`

If npm trusted publishing for Akita still points at the old `publish.yml`, update it before the next real publish.

### `oh-my-pactgpb`

- npm package: `oh-my-pactgpb`
- GitHub repository: `zuevrs/oh-my-gpb`
- workflow filename to trust: `publish-pact.yml`
- expected tag prefix: `pact-v`

Pact still needs its first trusted-publisher binding before the first real publish.

## Manual release flow

Akita:

1. Update `packages/oh-my-akitagpb/package.json` version.
2. Merge to `main`.
3. Create tag `akita-vX.Y.Z`.
4. Push the tag.
5. GitHub Actions runs `publish-akita.yml` and publishes only `packages/oh-my-akitagpb`.

Pact:

1. Update `packages/oh-my-pactgpb/package.json` version.
2. Merge to `main`.
3. Create tag `pact-vX.Y.Z`.
4. Push the tag.
5. GitHub Actions runs `publish-pact.yml` and publishes only `packages/oh-my-pactgpb`.

## Release-path verification

A deterministic repo-side check is available:

```bash
npm run release:verify
```

It verifies that workflows and docs still reference the correct package names, workflow filenames, paths, and tag prefixes.
