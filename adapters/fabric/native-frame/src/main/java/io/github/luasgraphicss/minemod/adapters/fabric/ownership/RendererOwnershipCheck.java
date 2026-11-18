package io.github.luasgraphicss.minemod.adapters.fabric.ownership;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.catalog.RendererCatalog;
import io.github.luasgraphicss.minemod.adapters.ownership.gate.WorldLoadGate;
import java.util.Collections;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class RendererOwnershipCheck implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        WorldLoadGate.forInstalledMods(
                        RendererCatalog.bundled(),
                        Collections.<StageBridge>emptyList(),
                        InstalledMods.versionsById(FabricLoader.getInstance().getAllMods()))
                .requireOpen();
    }
}
