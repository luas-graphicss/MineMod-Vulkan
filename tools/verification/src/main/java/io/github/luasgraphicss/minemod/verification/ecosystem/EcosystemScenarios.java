package io.github.luasgraphicss.minemod.verification.ecosystem;

import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.modpack.ModpackMatrixScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ownership.RendererConflictsScenario;
import io.github.luasgraphicss.minemod.verification.scenario.shaders.ShaderIntegrationScenario;
import java.util.List;

public final class EcosystemScenarios {
    private static final List<RepositoryScenario> SCENARIOS =
            List.of(new ModpackMatrixScenario(), new RendererConflictsScenario(), new ShaderIntegrationScenario());

    private EcosystemScenarios() {}

    public static List<RepositoryScenario> all() {
        return SCENARIOS;
    }
}
