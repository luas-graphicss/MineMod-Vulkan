package io.github.luasgraphicss.minemod.verification.scenario;

import io.github.luasgraphicss.minemod.verification.scenario.legacy.LegacyGlContractScenario;
import io.github.luasgraphicss.minemod.verification.scenario.lifecycle.SharedResourceLifecycleScenario;
import io.github.luasgraphicss.minemod.verification.scenario.mixed.MixedRenderStateScenario;
import io.github.luasgraphicss.minemod.verification.scenario.synchronous.SynchronousOperationsScenario;
import java.util.List;
import java.util.Optional;

public final class ScenarioCatalog {
    private static final List<Scenario> SCENARIOS = List.of(
            new MixedRenderStateScenario(),
            new LegacyGlContractScenario(),
            new SharedResourceLifecycleScenario(),
            new SynchronousOperationsScenario());

    private ScenarioCatalog() {}

    public static List<Scenario> all() {
        return SCENARIOS;
    }

    public static Optional<Scenario> named(String name) {
        return SCENARIOS.stream().filter(scenario -> scenario.name().equals(name)).findFirst();
    }
}
