package io.github.luasgraphicss.minemod.verification.scenario;

import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;

public interface TargetScenario {
    String name();

    ScenarioReport run(ScenarioEnvironment environment, GameTarget target) throws IOException, InterruptedException;
}
