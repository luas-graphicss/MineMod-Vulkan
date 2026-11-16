package io.github.luasgraphicss.minemod.verification.targets.sources;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.util.Optional;

public interface LoaderDistribution {
    Optional<LoaderTarget> targetFor(RequiredRelease release);
}
