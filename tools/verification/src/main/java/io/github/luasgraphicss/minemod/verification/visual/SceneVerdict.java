package io.github.luasgraphicss.minemod.verification.visual;

import java.util.List;

public record SceneVerdict(double meanError, double differingCells, List<String> differences) {
    public SceneVerdict {
        differences = List.copyOf(differences);
    }

    public boolean matches() {
        return differences.isEmpty();
    }
}
