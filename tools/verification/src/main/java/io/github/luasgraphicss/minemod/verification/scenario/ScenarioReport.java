package io.github.luasgraphicss.minemod.verification.scenario;

import java.util.List;

public record ScenarioReport(List<String> observations, List<String> failures) {
    public ScenarioReport {
        observations = List.copyOf(observations);
        failures = List.copyOf(failures);
    }

    public boolean passed() {
        return failures.isEmpty();
    }

    public String observationsAsText() {
        return String.join("\n", observations) + "\n";
    }
}
