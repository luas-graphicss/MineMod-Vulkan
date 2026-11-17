package io.github.luasgraphicss.minemod.verification.ecosystem.fixture;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.catalog.RendererCatalog;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.gate.WorldLoadGate;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class FixtureRenderers {
    public static final String BRIDGED_VERSION = "1.0.0";

    private final List<StageBridge> bridges = new ArrayList<>();
    private RendererCatalog catalog = RendererCatalog.bundled();

    public void add(String modId, PipelineStage stage) {
        catalog = catalog.with(modId, EnumSet.of(stage));
        bridges.add(new StageBridge(new RendererIdentity(modId, BRIDGED_VERSION), stage));
    }

    public WorldLoadGate gateFor(Map<String, String> installedModVersions) {
        return WorldLoadGate.forInstalledMods(catalog, bridges, installedModVersions);
    }
}
