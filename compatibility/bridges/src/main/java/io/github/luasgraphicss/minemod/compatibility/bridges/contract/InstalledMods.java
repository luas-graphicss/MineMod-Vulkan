package io.github.luasgraphicss.minemod.compatibility.bridges.contract;

import java.util.Optional;

public interface InstalledMods {
    Optional<String> versionOf(String modId);

    boolean declares(String modId, ContractSymbol symbol);
}
