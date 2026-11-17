package io.github.luasgraphicss.minemod.verification.scenario;

import io.github.luasgraphicss.minemod.verification.journey.instance.GradleInstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.scenario.vanilla.ResourceReloadScenario;
import io.github.luasgraphicss.minemod.verification.scenario.vanilla.VanillaJourneyScenario;
import io.github.luasgraphicss.minemod.verification.scenario.vanilla.VanillaWithoutGlScenario;
import java.util.List;
import java.util.Optional;

public final class TargetScenarioCatalog {
    private static final InstanceLauncher LAUNCHER = new GradleInstanceLauncher();
    private static final List<TargetScenario> SCENARIOS = List.of(
            new VanillaJourneyScenario(LAUNCHER),
            new VanillaWithoutGlScenario(LAUNCHER),
            new ResourceReloadScenario(LAUNCHER));

    private TargetScenarioCatalog() {}

    public static List<TargetScenario> all() {
        return SCENARIOS;
    }

    public static Optional<TargetScenario> named(String name) {
        return SCENARIOS.stream().filter(scenario -> scenario.name().equals(name)).findFirst();
    }
}
