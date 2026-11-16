package io.github.luasgraphicss.minemod.verification.scenario;

public interface Scenario {
    String name();

    ScenarioReport run();
}
