package io.github.luasgraphicss.minemod.verification.visual;

import java.util.List;

public record VisualResult(int scenes, List<String> observations, List<String> differences) {
    public VisualResult {
        observations = List.copyOf(observations);
        differences = List.copyOf(differences);
    }

    public boolean matches() {
        return differences.isEmpty();
    }

    public String observationsAsText() {
        return String.join("\n", observations) + "\n";
    }
}
