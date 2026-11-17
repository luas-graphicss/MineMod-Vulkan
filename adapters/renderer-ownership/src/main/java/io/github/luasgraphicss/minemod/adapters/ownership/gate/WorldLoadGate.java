package io.github.luasgraphicss.minemod.adapters.ownership.gate;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.catalog.RendererCatalog;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipConflict;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipDecision;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipNegotiator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class WorldLoadGate {
    private final OwnershipDecision decision;

    private WorldLoadGate(OwnershipDecision decision) {
        this.decision = decision;
    }

    public static WorldLoadGate forInstalledMods(
            RendererCatalog catalog, Collection<StageBridge> bridges, Map<String, String> installedModVersions) {
        OwnershipNegotiator negotiator = new OwnershipNegotiator(bridges);
        return new WorldLoadGate(negotiator.negotiate(catalog.claimsOf(installedModVersions)));
    }

    public OwnershipDecision decision() {
        return decision;
    }

    public List<String> diagnosis() {
        List<String> lines = new ArrayList<>();
        for (OwnershipConflict conflict : decision.conflicts()) {
            lines.add(conflict.toString());
        }
        return lines;
    }

    public void requireOpen() {
        if (!decision.allowsWorldLoad()) {
            throw new RendererConflictException(diagnosis());
        }
    }
}
