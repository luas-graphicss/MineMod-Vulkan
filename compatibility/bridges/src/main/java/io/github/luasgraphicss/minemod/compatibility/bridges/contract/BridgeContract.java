package io.github.luasgraphicss.minemod.compatibility.bridges.contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class BridgeContract {
    private final String modId;
    private final Set<String> versions;
    private final List<ContractSymbol> symbols;

    public BridgeContract(String modId, Collection<String> versions, Collection<ContractSymbol> symbols) {
        if (modId.trim().isEmpty()) {
            throw new IllegalArgumentException("a contract names the mod it was read from");
        }
        if (versions.isEmpty() || symbols.isEmpty()) {
            throw new IllegalArgumentException(
                    "the contract with " + modId + " names exact versions and the symbols it relies on");
        }
        this.modId = modId;
        this.versions = Collections.unmodifiableSet(new TreeSet<>(versions));
        this.symbols = Collections.unmodifiableList(new ArrayList<>(symbols));
    }

    public String modId() {
        return modId;
    }

    public Set<String> versions() {
        return versions;
    }

    public List<ContractSymbol> symbols() {
        return symbols;
    }

    public ContractVerdict verdictFor(InstalledMods mods) {
        Optional<String> installed = mods.versionOf(modId);
        if (!installed.isPresent()) {
            return ContractVerdict.modAbsent(modId);
        }
        String version = installed.get();
        if (!versions.contains(version)) {
            return ContractVerdict.versionNotCovered(modId, version);
        }
        List<ContractSymbol> missing = new ArrayList<>();
        for (ContractSymbol symbol : symbols) {
            if (!mods.declares(modId, symbol)) {
                missing.add(symbol);
            }
        }
        return missing.isEmpty()
                ? ContractVerdict.satisfied(modId, version)
                : ContractVerdict.symbolsMissing(modId, version, missing);
    }
}
