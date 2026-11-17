package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractSymbol;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.InstalledMods;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class PackMods implements InstalledMods {
    private final Map<String, String> versionsByModId;
    private final Map<String, Set<ContractSymbol>> symbolsByModId = new HashMap<>();

    PackMods(Map<String, String> versionsByModId) {
        this.versionsByModId = Map.copyOf(versionsByModId);
    }

    void declare(String modId, ContractSymbol symbol) {
        symbolsByModId.computeIfAbsent(modId, absent -> new HashSet<>()).add(symbol);
    }

    @Override
    public Optional<String> versionOf(String modId) {
        return Optional.ofNullable(versionsByModId.get(modId));
    }

    @Override
    public boolean declares(String modId, ContractSymbol symbol) {
        return symbolsByModId.getOrDefault(modId, Set.of()).contains(symbol);
    }
}
