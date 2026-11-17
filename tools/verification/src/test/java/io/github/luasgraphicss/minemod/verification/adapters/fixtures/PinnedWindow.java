package io.github.luasgraphicss.minemod.verification.adapters.fixtures;

import io.github.luasgraphicss.minemod.verification.adapters.names.NameTable;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import java.util.List;
import java.util.Map;

public final class PinnedWindow {
    public static final String RELEASE = "1.13";
    public static final String CLIENT_SHA1 = "c0b970952cdd279912da384cdbfc0c26e6c6090b";
    public static final String OFFICIAL_SWAP = "cfs.a(Z)V";
    public static final String MOJANG_SWAP = "com/mojang/blaze3d/platform/Window.updateDisplay(Z)V";
    public static final String INTERMEDIARY_SWAP = "net/minecraft/class_1041.method_15998(Z)V";

    private static final String SWAP_BUFFERS = "org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V";

    private PinnedWindow() {}

    public static ReleaseHooks hooks() {
        return hooksNamed(MOJANG_SWAP);
    }

    public static ReleaseHooks hooksWithoutMojangNames() {
        return hooksNamed(null);
    }

    public static NameTable intermediaryNames() {
        return new NameTable(
                Map.of("cfs", "net/minecraft/class_1041"),
                Map.of(NameTable.methodKey("cfs", "a", "(Z)V"), "method_15998"));
    }

    private static ReleaseHooks hooksNamed(String named) {
        return new ReleaseHooks(
                RELEASE,
                CLIENT_SHA1,
                Windowing.GLFW,
                List.of(new HookPoint("swap-buffers", "swap-buffers", SWAP_BUFFERS, OFFICIAL_SWAP, named, 1)));
    }
}
