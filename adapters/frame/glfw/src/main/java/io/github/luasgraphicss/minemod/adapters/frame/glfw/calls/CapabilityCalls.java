package io.github.luasgraphicss.minemod.adapters.frame.glfw.calls;

import io.github.luasgraphicss.minemod.adapters.frame.answers.BlankInstances;
import io.github.luasgraphicss.minemod.adapters.frame.answers.OfferedCapabilities;
import org.lwjgl.opengl.GLCapabilities;

public final class CapabilityCalls {
    private CapabilityCalls() {}

    public static GLCapabilities getCapabilities() {
        return Offered.CAPABILITIES;
    }

    private static final class Offered {
        static final GLCapabilities CAPABILITIES =
                BlankInstances.raising(GLCapabilities.class, OfferedCapabilities.flags());
    }
}
