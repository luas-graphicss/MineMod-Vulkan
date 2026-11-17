package io.github.luasgraphicss.minemod.verification.journey.instance;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public record InstanceRequest(
        GameInstance instance,
        Path repositoryRoot,
        Path driverBuild,
        Path planFile,
        Path directory,
        Optional<Path> candidateMod,
        Duration timeout) {}
