package io.github.luasgraphicss.minemod.verification.scenario;

import java.nio.file.Path;

public record ScenarioEnvironment(Path repositoryRoot, Path cacheDirectory) {}
