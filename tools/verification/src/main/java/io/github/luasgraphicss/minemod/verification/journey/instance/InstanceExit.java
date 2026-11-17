package io.github.luasgraphicss.minemod.verification.journey.instance;

import java.nio.file.Path;

public record InstanceExit(int exitCode, boolean timedOut, Path log) {}
