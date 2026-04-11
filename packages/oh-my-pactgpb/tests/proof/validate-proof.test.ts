import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';

import { writeProofInitArtifacts } from '../../src/proof/init-proof.js';
import {
  hasInstalledValidateContract,
  writeProofValidateArtifacts,
  type PactProviderValidateState,
} from '../../src/proof/validate-proof.js';
import { writeProofPlanArtifacts } from '../../src/proof/plan-proof.js';
import { writeProofScanArtifacts } from '../../src/proof/scan-proof.js';
import { writeProofWriteArtifacts, type PactProviderWriteState } from '../../src/proof/write-proof.js';
import { createInstalledFixture, type FixtureRepo } from '../helpers/fixture-repo.js';
import { expectRequiredTopLevelFields, installFixture, readJsonFile } from './helpers.js';

const fixtures: FixtureRepo[] = [];

function trackFixture<T extends FixtureRepo>(fixture: T): T {
  fixtures.push(fixture);
  return fixture;
}

function prepareValidateFixture(fixture: FixtureRepo, options: { bootstrapInit?: boolean } = {}): void {
  installFixture(fixture);
  if (options.bootstrapInit) {
    writeProofInitArtifacts(fixture.rootDir);
  }
  writeProofScanArtifacts(fixture.rootDir);
  writeProofPlanArtifacts(fixture.rootDir);
  writeProofWriteArtifacts(fixture.rootDir);
}

function persistedWriteStatePath(rootDir: string): string {
  return path.join(rootDir, '.oma', 'packs', 'oh-my-pactgpb', 'state', 'shared', 'write', 'write-state.json');
}

function mutatePersistedWriteState(rootDir: string, mutate: (state: PactProviderWriteState) => void): void {
  const statePath = persistedWriteStatePath(rootDir);
  const state = readJsonFile<PactProviderWriteState>(statePath);
  mutate(state);
  writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`, 'utf8');
}

afterEach(() => {
  while (fixtures.length > 0) {
    fixtures.pop()?.cleanup();
  }
});

describe('validate proof', () => {
  it('records a validated enough-for-now stop point for a resolved provider-state slice while later uncovered coverage remains', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-partial' }));
    prepareValidateFixture(fixture);

    expect(hasInstalledValidateContract(fixture.rootDir)).toBe(true);

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expectRequiredTopLevelFields(fixture.rootDir, 'validate', persistedState);
    expect(persistedState.inputVerdicts.plan).toBe('needs-provider-state-work');
    expect(persistedState.inputVerdicts.write).toBe('written');
    expect(persistedState.validatedCoverageSlice.category).toBe('add-missing-provider-states');
    expect(persistedState.validatedCoverageSlice.providerStates).toContain('invoice creatable');
    expect(persistedState.technicalValidationOutcome).toBe('validated');
    expect(persistedState.iterationOutcome).toBe('validated-enough-for-now');
    expect(persistedState.stopPointDecision).toBe('stop-for-now');
    expect(persistedState.remainingCoverageGaps.uncoveredEndpoints).toContain('DELETE /invoices/{id}');
    expect(persistedState.expectedFollowUpCommand).toBeNull();
    expect(persistedState.unresolvedBlockers.join(' ')).not.toContain('Provider state names');
  });

  it('records validated-but-more-coverage-remains when the slice validates but immediate follow-on gaps are still open', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-local' }));
    prepareValidateFixture(fixture);

    mutatePersistedWriteState(fixture.rootDir, (state) => {
      state.remainingCoverageGaps.endpointsWithGaps = ['POST /payments/refunds'];
      state.validationFocus = [...new Set([...state.validationFocus, 'Keep remaining endpoints with gaps explicit: POST /payments/refunds.'])];
    });

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);
    const contractDir = path.join(fixture.rootDir, 'src', 'test', 'java', 'com', 'example', 'payments', 'contract');
    const pactTests = readdirSync(contractDir).filter((entry) => entry.endsWith('PactTest.java'));

    expect(pactTests).toEqual(['PaymentProviderPactTest.java']);
    expect(persistedState.technicalValidationOutcome).toBe('validated');
    expect(persistedState.iterationOutcome).toBe('validated-but-more-coverage-remains');
    expect(persistedState.stopPointDecision).toBe('continue-with-another-coverage-slice');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-plan');
    expect(persistedState.remainingCoverageGaps.endpointsWithGaps).toContain('POST /payments/refunds');
  });

  it('reports partial when provider-state work is still unresolved after extending stale verification', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-stale' }));
    prepareValidateFixture(fixture);

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);
    const contractDir = path.join(fixture.rootDir, 'src', 'test', 'java', 'com', 'example', 'orders', 'contract');
    const pactTests = readdirSync(contractDir).filter((entry) => entry.endsWith('PactTest.java'));

    expect(pactTests).toEqual(['OrderProviderPactTest.java']);
    expect(persistedState.inputVerdicts.write).toBe('partial');
    expect(persistedState.validatedCoverageSlice.category).toBe('prepare-uncovered-endpoint-coverage');
    expect(persistedState.technicalValidationOutcome).toBe('partial');
    expect(persistedState.iterationOutcome).toBe('partial');
    expect(persistedState.stopPointDecision).toBe('repair-before-continuing');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-plan');
    expect(persistedState.unresolvedBlockers.join(' ')).toContain('Provider state names');
    expect(persistedState.runnableVerificationCheck.verdict).toBe('unproven');
  });

  it('keeps broker-hint-only repos partial and points the next step at artifact-source grounding', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-broker' }));
    prepareValidateFixture(fixture);

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expect(persistedState.inputVerdicts.plan).toBe('needs-artifact-source-clarification');
    expect(persistedState.technicalValidationOutcome).toBe('partial');
    expect(persistedState.iterationOutcome).toBe('partial');
    expect(persistedState.stopPointDecision).toBe('repair-before-continuing');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-plan');
    expect(persistedState.unresolvedBlockers.join(' ')).toContain('artifact retrieval');
    expect(persistedState.runnableVerificationCheck.verdict).toBe('unproven');
  });

  it('keeps bootstrap-only repos out of positive stop-point claims', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-provider-init-codefirst' }));
    prepareValidateFixture(fixture, { bootstrapInit: true });

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expect(persistedState.validatedCoverageSlice.category).toBe('partial-preparation');
    expect(persistedState.technicalValidationOutcome).toBe('partial');
    expect(persistedState.iterationOutcome).toBe('partial');
    expect(persistedState.stopPointDecision).toBe('repair-before-continuing');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-plan');
    expect(persistedState.recommendedNextStep.toLowerCase()).toContain('not enough');
  });

  it('preserves an honest irrelevant stop point for repos without Pact relevance', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'java-service' }));
    prepareValidateFixture(fixture);

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expect(persistedState.inputVerdicts.scan).toBe('irrelevant');
    expect(persistedState.inputVerdicts.write).toBe('no-op');
    expect(persistedState.technicalValidationOutcome).toBe('irrelevant');
    expect(persistedState.iterationOutcome).toBe('irrelevant');
    expect(persistedState.stopPointDecision).toBe('not-applicable');
    expect(persistedState.expectedFollowUpCommand).toBeNull();
    expect(persistedState.repoRealityChecks.expectedArtifactsPresent).toEqual([]);
  });

  it('preserves a blocked outcome when provider binding stayed ambiguous', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-ambiguous' }));
    prepareValidateFixture(fixture);

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expect(persistedState.inputVerdicts.plan).toBe('blocked');
    expect(persistedState.inputVerdicts.write).toBe('blocked');
    expect(persistedState.technicalValidationOutcome).toBe('blocked');
    expect(persistedState.iterationOutcome).toBe('blocked');
    expect(persistedState.stopPointDecision).toBe('repair-before-continuing');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-scan');
    expect(persistedState.runnableVerificationCheck.verdict).toBe('blocked');
  });

  it('reports inconsistency when recorded write output drifts from repo reality', () => {
    const fixture = trackFixture(createInstalledFixture({ template: 'spring-pact-provider-local' }));
    prepareValidateFixture(fixture);

    const targetPath = path.join(
      fixture.rootDir,
      'src',
      'test',
      'java',
      'com',
      'example',
      'payments',
      'contract',
      'PaymentProviderPactTest.java',
    );
    const driftedContent = readFileSync(targetPath, 'utf8').replace(
      '    context.setTarget(new HttpTestTarget("localhost", 8080));\n',
      '',
    );
    writeFileSync(targetPath, driftedContent, 'utf8');

    const artifacts = writeProofValidateArtifacts(fixture.rootDir);
    const persistedState = readJsonFile<PactProviderValidateState>(artifacts.statePath);

    expect(persistedState.technicalValidationOutcome).toBe('inconsistent');
    expect(persistedState.iterationOutcome).toBe('inconsistent');
    expect(persistedState.stopPointDecision).toBe('repair-before-continuing');
    expect(persistedState.expectedFollowUpCommand).toBe('/pact-scan');
    expect(persistedState.compileCheck.verdict).toBe('failed');
    expect(persistedState.runnableVerificationCheck.verdict).toBe('blocked');
  });
});
