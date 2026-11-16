package io.github.luasgraphicss.minemod.verification.check;

import io.github.luasgraphicss.minemod.verification.targets.TargetMatrixCheck;
import java.util.List;
import java.util.Optional;

public record CheckProfile(String name, List<Check> checks) {
    private static final List<CheckProfile> PROFILES = List.of(
            new CheckProfile("foundation", List.of(new TargetMatrixCheck())));

    public static Optional<CheckProfile> named(String name) {
        return PROFILES.stream().filter(profile -> profile.name().equals(name)).findFirst();
    }
}
