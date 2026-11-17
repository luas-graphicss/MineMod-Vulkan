package io.github.luasgraphicss.minemod.verification.scenario.ownership;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.catalog.RendererCatalog;
import io.github.luasgraphicss.minemod.adapters.ownership.gate.WorldLoadGate;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipConflict;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipConflict.Reason;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Outcome;
import io.github.luasgraphicss.minemod.verification.compatibility.storage.CompatibilityManifestFile;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureRenderers;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RendererConflictsScenario implements RepositoryScenario {
    public static final String NAME = "renderer-conflicts";

    private static final List<StageBridge> NO_BRIDGES = List.of();
    private static final String UNLISTED_VERSION = "unlisted";
    private static final String FIRST_FIXTURE = "minemod-fixture-chunk-renderer";
    private static final String SECOND_FIXTURE = "minemod-fixture-voxel-renderer";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException {
        ScenarioLog log = new ScenarioLog();
        refuseEveryCatalogRendererWithoutABridge(log);
        List<CompatibilityCase> cases = CompatibilityManifestFile.readFrom(environment.repositoryRoot()).cases().stream()
                .filter(entry -> entry.scenario().equals(NAME))
                .toList();
        log.expect(!cases.isEmpty(), "the compatibility manifest to carry cases of " + NAME);
        cases.forEach(entry -> compareWithTheGate(entry, log));
        giveABridgedStageOneOwner(log);
        return log.report();
    }

    private static void refuseEveryCatalogRendererWithoutABridge(ScenarioLog log) {
        RendererCatalog catalog = RendererCatalog.bundled();
        for (String renderer : catalog.rendererIds()) {
            WorldLoadGate gate = WorldLoadGate.forInstalledMods(catalog, NO_BRIDGES, Map.of(renderer, UNLISTED_VERSION));
            log.expect(
                    !gate.diagnosis().isEmpty()
                            && gate.diagnosis().stream().allMatch(line -> line.contains(renderer)),
                    renderer + " to be diagnosed by name before a world loads");
        }
        log.observe("every renderer of the catalog keeps the world closed until a bridge names its exact version: "
                + catalog.rendererIds());
    }

    private static void compareWithTheGate(CompatibilityCase entry, ScenarioLog log) {
        Map<String, String> installed = new LinkedHashMap<>();
        entry.mods().forEach(mod -> installed.put(mod.id(), mod.version()));
        WorldLoadGate gate = WorldLoadGate.forInstalledMods(RendererCatalog.bundled(), NO_BRIDGES, installed);
        List<OwnershipConflict> conflicts = gate.decision().conflicts();
        if (conflicts.isEmpty()) {
            log.fail("case " + entry.id() + " names no renderer of the catalog");
            return;
        }
        boolean bridgeable = conflicts.stream().allMatch(conflict -> conflict.reason() == Reason.NO_BRIDGE_FOR_VERSION);
        log.expectEqual(
                bridgeable ? Outcome.REQUIRES_BRIDGE : Outcome.UNSUPPORTED, entry.outcome(), "case " + entry.id());
        log.observe(entry.id() + ": " + entry.outcome() + ", " + String.join("; ", gate.diagnosis()));
    }

    private static void giveABridgedStageOneOwner(ScenarioLog log) {
        FixtureRenderers bridged = new FixtureRenderers();
        bridged.add(FIRST_FIXTURE, PipelineStage.TERRAIN_GEOMETRY);
        WorldLoadGate exact = bridged.gateFor(Map.of(FIRST_FIXTURE, FixtureRenderers.BRIDGED_VERSION));
        WorldLoadGate newer = bridged.gateFor(Map.of(FIRST_FIXTURE, "1.0.1"));
        bridged.add(SECOND_FIXTURE, PipelineStage.TERRAIN_GEOMETRY);
        WorldLoadGate contested = bridged.gateFor(Map.of(
                FIRST_FIXTURE, FixtureRenderers.BRIDGED_VERSION, SECOND_FIXTURE, FixtureRenderers.BRIDGED_VERSION));

        log.expect(exact.diagnosis().isEmpty(), "the bridged version of a fixture renderer to open the world");
        log.expectEqual(
                FIRST_FIXTURE + " " + FixtureRenderers.BRIDGED_VERSION,
                String.valueOf(exact.decision().bridgedStages().get(PipelineStage.TERRAIN_GEOMETRY)),
                "owner of the bridged stage");
        log.expectEqual(1, newer.decision().conflicts().size(), "conflicts of a version without a bridge");
        log.expect(
                contested.decision().conflicts().stream()
                        .anyMatch(conflict -> conflict.reason() == Reason.CONTESTED_BY_REPLACEMENTS),
                "two bridged renderers on one stage to be refused together");
        log.observe("a bridged stage has one owner; a newer version and a second renderer are refused: "
                + newer.diagnosis() + " " + contested.diagnosis());
    }
}
