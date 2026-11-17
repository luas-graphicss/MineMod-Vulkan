package io.github.luasgraphicss.minemod.compatibility.bridges.activation;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.InstalledMods;
import io.github.luasgraphicss.minemod.compatibility.bridges.registry.BridgeRegistry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class BridgeActivation {
    private final BridgeRegistry registry;

    public BridgeActivation(BridgeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Map<String, ContractVerdict> activate(Collection<BridgeCandidate> candidates, InstalledMods mods) {
        Map<String, ContractVerdict> verdictsByBridgeId = new LinkedHashMap<>();
        for (BridgeCandidate candidate : candidates) {
            ContractVerdict verdict = candidate.contract().verdictFor(mods);
            if (verdict.activates()) {
                registry.register(candidate.bridge());
            }
            verdictsByBridgeId.put(candidate.bridge().descriptor().id(), verdict);
        }
        return verdictsByBridgeId;
    }
}
