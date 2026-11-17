package io.github.luasgraphicss.minemod.compatibility.bridges.support;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractSymbol;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.InstalledMods;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ListedMods implements InstalledMods {
    private final Map<String, String> versionsByModId = new HashMap<>();
    private final Map<String, Set<ContractSymbol>> symbolsByModId = new HashMap<>();

    public ListedMods install(String modId, String version, ContractSymbol... symbols) {
        versionsByModId.put(modId, version);
        Set<ContractSymbol> declared = new HashSet<>();
        for (ContractSymbol symbol : symbols) {
            declared.add(symbol);
        }
        symbolsByModId.put(modId, declared);
        return this;
    }

    @Override
    public Optional<String> versionOf(String modId) {
        return Optional.ofNullable(versionsByModId.get(modId));
    }

    @Override
    public boolean declares(String modId, ContractSymbol symbol) {
        Set<ContractSymbol> declared = symbolsByModId.get(modId);
        return declared != null && declared.contains(symbol);
    }
}
