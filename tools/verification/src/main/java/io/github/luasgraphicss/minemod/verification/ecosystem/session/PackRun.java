package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import java.util.List;
import java.util.Map;

public record PackRun(
        boolean refusedBeforeWorld,
        List<String> failures,
        Map<String, Map<String, Integer>> journalsByMod,
        List<String> diagnostics,
        String summary) {
    public PackRun {
        failures = List.copyOf(failures);
        journalsByMod = Map.copyOf(journalsByMod);
        diagnostics = List.copyOf(diagnostics);
    }
}
