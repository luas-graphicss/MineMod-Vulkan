package io.github.luasgraphicss.minemod.verification.scenario;

import java.io.IOException;

public interface RepositoryScenario {
    String name();

    ScenarioReport run(ScenarioEnvironment environment) throws IOException, InterruptedException;
}
