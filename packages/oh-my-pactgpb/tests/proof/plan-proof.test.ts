import { readFileSync } from 'node:fs';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';

import {
  derivePlanFromScanState,
  hasInstalledPlanContract,
  readPersistedScanState,
  writeProofPlanArtifacts,
  type PactProviderPlanState,
} from '../../src/proof/plan-proof.js';
import { writeProofScanArtifacts } from '../../src/proof/scan-proof.js';
import {
  createInstalledFixture,
  type FixtureRepo,
  invokeInstalledCli,
  parseJsonOutput,
} from '../helpers/fixture-repo.js';

type CliResult = {
  subcommand: 'install' | 'update' | 'doctor' | 'unknown';
  status: 'ok' | 'blocked' | 'error';
  reason: string;
};

type PlanStateContract = {
  requiredMachineState: Array<{
    requiredTopLevelFields: string[];
  }>;
};

const fixtures: FixtureRepo[] = [];

function trackFixture<T extends FixtureRepo>(fixture: T): T {
  fixtures.push(fixture);
  return fixture;
}

function readJsonFile<T>(filePath: string): T {
  return JSON.parse(readFileSync(filePath, 'utf8')) as T;
}

function readInstalledContract(projectRoot: string): PlanStateContract {
  return readJsonFile<PlanStateContract>(path.join(projectRoot, '.oma', 'packs', 'oh-my-pactgpb', 'templates', 'plan', 'state-contract.json'));
}

afterEach(() => {
  while (fixtures.length > 0) {
    fixtures.pop()?.cleanup();
  }
});

describe('plan proof', () => {
  it('requires persisted scan-state on disk instead of planning from memory', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-local' }));
    const installResult = parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));

    expect(installResult).toMatchObject({
      subcommand: 'install',
      status: 'ok',
      reason: 'install-complete',
    });
    expect(hasInstalledPlanContract(fixture.rootDir)).toBe(true);
    expect(() => readPersistedScanState(fixture.rootDir)).toThrow(/Persisted scan-state is missing/);
    expect(() => writeProofPlanArtifacts(fixture.rootDir)).toThrow(/Persisted scan-state is missing/);
  });

  it('produces a ready-to-scaffold plan from persisted local pact scan-state', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-local' }));
    parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));
    writeProofScanArtifacts(fixture.rootDir);

    const artifacts = writeProofPlanArtifacts(fixture.rootDir);
    const contract = readInstalledContract(fixture.rootDir);
    const requiredFields = contract.requiredMachineState[0]?.requiredTopLevelFields ?? [];
    const persistedState = readJsonFile<PactProviderPlanState>(artifacts.statePath);
    const summary = readFileSync(artifacts.summaryPath, 'utf8');

    for (const field of requiredFields) {
      expect(persistedState).toHaveProperty(field);
    }

    expect(persistedState.providerSelection.name).toBe('payments-provider');
    expect(persistedState.artifactSourceStrategy.verdict).toBe('local');
    expect(persistedState.verificationReadiness.verdict).toBe('ready-to-scaffold');
    expect(persistedState.verificationReadiness.existingSetup).toBe('extend-existing-provider-verification');
    expect(persistedState.providerStateWork.existingStates).toContain('payment exists');
    expect(persistedState.plannedTasks.map((task) => task.title)).toContain('Extend the existing provider verification setup');
    expect(summary).toContain('Planning verdict: ready-to-scaffold');
    expect(summary).toContain('Selected provider: payments-provider');
    expect(summary).toContain('Extend the existing provider verification setup');
  });

  it('marks Pact as irrelevant when the persisted scan-state says the repo has no Pact evidence', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'java-service' }));
    parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));

    const scanArtifacts = writeProofScanArtifacts(fixture.rootDir);
    const plan = derivePlanFromScanState(scanArtifacts.state);

    expect(plan.verificationReadiness.verdict).toBe('irrelevant');
    expect(plan.providerSelection.name).toBe('fixture-java-service');
    expect(plan.plannedTasks).toEqual([]);
    expect(plan.blockedBy).toEqual([]);
  });

  it('keeps broker-only artifact hints in clarification mode instead of claiming runnable verification', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-broker' }));
    parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));
    writeProofScanArtifacts(fixture.rootDir);

    const artifacts = writeProofPlanArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderPlanState>(artifacts.statePath);

    expect(persistedState.providerSelection.name).toBe('shipping-provider');
    expect(persistedState.artifactSourceStrategy.verdict).toBe('broker');
    expect(persistedState.verificationReadiness.verdict).toBe('needs-artifact-source-clarification');
    expect(persistedState.plannedTasks.map((task) => task.title)).toContain('Clarify broker-backed pact retrieval');
    expect(persistedState.blockedBy.join(' ')).toContain('broker access');
  });

  it('extends stale existing verification instead of recreating it from scratch', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-stale' }));
    parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));
    writeProofScanArtifacts(fixture.rootDir);

    const artifacts = writeProofPlanArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderPlanState>(artifacts.statePath);

    expect(persistedState.providerSelection.name).toBe('orders-provider');
    expect(persistedState.artifactSourceStrategy.verdict).toBe('local');
    expect(persistedState.verificationReadiness.verdict).toBe('needs-provider-state-work');
    expect(persistedState.verificationReadiness.existingSetup).toBe('extend-existing-provider-verification');
    expect(persistedState.providerStateWork.gaps.join(' ')).toContain('No provider state hooks were detected');
    expect(persistedState.plannedTasks.map((task) => task.title)).toContain('Extend the existing provider verification setup');
    expect(persistedState.plannedTasks.map((task) => task.title)).not.toContain('Scaffold a provider verification test');
  });

  it('persists provider ambiguity as a blocker instead of silently guessing', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-ambiguous' }));
    parseJsonOutput<CliResult>(invokeInstalledCli(fixture.rootDir, ['install']));
    writeProofScanArtifacts(fixture.rootDir);

    const artifacts = writeProofPlanArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderPlanState>(artifacts.statePath);
    const summary = readFileSync(artifacts.summaryPath, 'utf8');

    expect(persistedState.providerSelection.name).toBeNull();
    expect(persistedState.verificationReadiness.verdict).toBe('blocked');
    expect(persistedState.providerSelection.ambiguities.join(' ')).toContain('ambiguous');
    expect(persistedState.blockedBy.join(' ')).toContain('ambiguous');
    expect(persistedState.plannedTasks).toHaveLength(1);
    expect(persistedState.plannedTasks[0]?.title).toBe('Resolve provider binding ambiguity');
    expect(summary).toContain('blocked or unclear');
  });
});
