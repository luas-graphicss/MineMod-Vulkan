package io.github.luasgraphicss.minemod.verification.scenario;

import io.github.luasgraphicss.minemod.verification.ecosystem.EcosystemScenarios;
import io.github.luasgraphicss.minemod.verification.scenario.transformers.ProductionTransformersScenario;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class RepositoryScenarioCatalog {
    private static final List<RepositoryScenario> SCENARIOS = Stream.concat(
                    Stream.of(new ProductionTransformersScenario()), EcosystemScenarios.all().stream())
            .toList();

    private RepositoryScenarioCatalog() {}

    public static List<RepositoryScenario> all() {
        return SCENARIOS;
    }

    public static Optional<RepositoryScenario> named(String name) {
        return SCENARIOS.stream().filter(scenario -> scenario.name().equals(name)).findFirst();
    }
}
