package io.github.luasgraphicss.minemod.verification.differential;

import java.util.List;

public record DifferentialResult(int scripts, List<String> differences) {
    public DifferentialResult {
        differences = List.copyOf(differences);
    }
}
