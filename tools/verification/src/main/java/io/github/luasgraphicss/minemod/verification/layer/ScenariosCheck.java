package io.github.luasgraphicss.minemod.verification.layer;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioCatalog;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ScenariosCheck implements Check {
    @Override
    public String name() {
        return "compatibility-scenarios";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) {
        List<String> violations = new ArrayList<>();
        for (Scenario scenario : ScenarioCatalog.all()) {
            for (String failure : scenario.run().failures()) {
                violations.add(scenario.name() + ": " + failure);
            }
        }
        return violations;
    }
}
