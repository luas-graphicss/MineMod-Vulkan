package io.github.luasgraphicss.minemod.verification.matrix.model;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.util.List;

public record AdapterBuild(
        String release,
        LoaderTarget.Kind loader,
        String artifactName,
        int javaRuntime,
        LoaderTarget.Toolchain toolchain,
        String gameAdapter,
        String loaderAdapter) {

    public List<String> modules() {
        return List.of(gameAdapter, loaderAdapter);
    }
}
