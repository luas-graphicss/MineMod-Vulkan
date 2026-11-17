package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;

public final class HookOwners {
    private static final String GLFW =
            "io/github/luasgraphicss/minemod/adapters/presentation/glfw/GlfwPresentationHooks";
    private static final String DISPLAY =
            "io/github/luasgraphicss/minemod/adapters/presentation/display/DisplayPresentationHooks";

    private HookOwners() {}

    public static String of(Windowing windowing) {
        return switch (windowing) {
            case GLFW -> GLFW;
            case LWJGL2_DISPLAY -> DISPLAY;
        };
    }
}
